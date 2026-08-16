# Design: Autenticação — validação de token no frontend

**Data:** 2026-08-15
**Status:** Aprovado
**Repositório afetado:** `marmoraria-orcamentos-web` (frontend) — nenhuma mudança necessária no backend
**Item do backlog:** 1

---

## 1. Contexto / Problema

Bug relatado: o acesso ao app é liberado mesmo sem sessão válida — a aplicação abre vazia (sem dados) e só ao clicar em algo o usuário é redirecionado para `/login`. Não estava claro se o bug é de frontend ou backend, nem se é específico de produção ou local.

### Causa raiz (confirmada por leitura de código)

É um bug de lógica no frontend, presente em qualquer ambiente — não é específico de produção:

- `AuthContext.tsx` define `isAuthenticated = !!token`, onde `token` vem de `localStorage.getItem('jwtToken')` lido uma única vez no mount do provider, sem nenhuma checagem de validade.
- `ProtectedRoute.tsx` libera a rota baseado só nesse booleano.
- Um token expirado (o JWT dura 24h — `JwtService.EXPIRATION_MS`) ou revogado (logout em outra aba, via `TokenBlocklistService`) continua presente no `localStorage` e continua sendo tratado como "autenticado" — a página protegida renderiza normalmente antes de qualquer chamada à API confirmar que a sessão é inválida.
- O backend está correto: `JwtFilter` + `JwtService.validarToken()` rejeitam token expirado/revogado/inválido com 401 de forma consistente. Não há nada a corrigir do lado do backend.

### Sobre o sintoma "abre vazio, só redireciona ao clicar"

Toda página do app busca seus dados em `useEffect` no mount (confirmado em `DashboardPage`, `OrcamentosPage`, `OrcamentoDetailPage`, etc.), e o interceptor de resposta do `httpClient.ts` redireciona (via `window.location.href`) assim que recebe um 401 — na maioria dos casos isso deveria disparar quase imediatamente ao abrir a página, não só ao clicar.

O interceptor só falha em capturar a falha quando a requisição nunca vira uma resposta HTTP de verdade (erro de rede/CORS/timeout) — nesse caso `error.response` fica `undefined`, a condição `error.response?.status === 401` não bate, e nada redireciona até uma requisição *seguinte* (disparada por alguma interação) voltar com um 401 "limpo". Não foi possível confirmar por leitura estática qual dos dois caminhos ocorre no ambiente do usuário — mas o defeito estrutural (token não validado antes de conceder acesso) é o mesmo nos dois casos, e a correção abaixo resolve o bug relatado independente de qual seja o gatilho exato do "só ao clicar".

## 2. Decisão

Validar a expiração do JWT no cliente (decodificar o claim `exp`) antes de conceder acesso a rotas protegidas — sem chamada nova ao backend, já que a informação já está assinada no próprio token que o app tem em mãos.

**Alternativa considerada e descartada:** consultar um endpoint de validação no backend a cada carregamento do app. Mais completo (detectaria revogação por blocklist também), mas exige uma chamada de rede síncrona antes de renderizar qualquer coisa e um endpoint novo. O interceptor de 401 já existente continua cobrindo o caso de revogação em uma sessão já em andamento; o bug relatado é especificamente sobre um token já expirado/inválido presente ao abrir o app, que a checagem local resolve sem esse custo.

## 3. Design

### 3.1 Novo utilitário — `src/utils/jwt.ts`

```ts
export function isTokenExpired(token: string): boolean
```

Decodifica a segunda parte do JWT (payload, base64url), lê o claim `exp` (segundos desde epoch) e compara com `Date.now() / 1000`. Qualquer erro de parsing (token malformado, string vazia, payload sem `exp`) é tratado como **expirado** — fail-safe: na dúvida, não autentica.

### 3.2 `AuthContext.tsx`

A inicialização do estado `token` passa a validar antes de aceitar o valor do `localStorage`:

```ts
const [token, setToken] = useState<string | null>(() => {
  const stored = localStorage.getItem('jwtToken');
  if (stored && !isTokenExpired(stored)) return stored;
  localStorage.removeItem('jwtToken');
  return null;
});
```

`login()` e `logout()` continuam iguais. `isAuthenticated` continua sendo `!!token`, mas `token` nunca mais fica com um valor stale — `ProtectedRoute` passa a redirecionar imediatamente para um token vencido, sem nunca chegar a renderizar a página protegida vazia.

### 3.3 `httpClient.ts`

Sem mudança. O interceptor de 401/403 (incluindo o redirect via `window.location.href`) continua como rede de segurança para o caso de revogação em sessão ativa — fora do escopo deste bug.

## 4. Testes

O frontend não tem nenhuma infra de teste hoje (sem `vitest`/`jest` no `package.json`, nenhum arquivo `*.test.ts`). Este trabalho planta a base mínima:

- Adicionar `vitest` como devDependency.
- Script `"test": "vitest run"` no `package.json`.
- `vitest.config.ts` mínimo (ambiente `node` é suficiente — `isTokenExpired` é função pura, sem DOM).
- `src/utils/jwt.test.ts` cobrindo:
  - token válido, não-expirado → `false`
  - token expirado → `true`
  - token malformado / string vazia → `true`

A verificação do fix em si (`AuthContext`/`ProtectedRoute`) é manual, via navegador — não há Testing Library configurada, e configurar isso é escopo maior do que o pedido ("só plantar a base").

## 5. Fora de escopo

- Revalidação periódica de um token que expira *durante* a sessão (o usuário teria que deixar a aba aberta por 24h contínuas — não é o bug relatado).
- Trocar o mecanismo de redirect do `httpClient` de `window.location.href` para navegação client-side. Considerado durante o brainstorming e descartado: com o fix acima, esse caminho só é acionado no caso raro de revogação em sessão ativa, onde um reload completo é aceitável.
- Testing Library / testes de componente React — só a base (`vitest` + um teste de função pura) foi pedida.
