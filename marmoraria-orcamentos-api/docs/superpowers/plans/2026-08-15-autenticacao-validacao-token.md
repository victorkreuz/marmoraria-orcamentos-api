# Autenticação — Validação de Token no Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the frontend from granting access to protected pages when the stored JWT is stale — validate its expiry before trusting it, instead of only checking that a token string exists.

**Architecture:** Add a pure `isTokenExpired(token)` utility that decodes the JWT payload and compares its `exp` claim to now. Wire it into `AuthContext`'s token initialization so a stale token is discarded before `isAuthenticated` is ever computed. Plant a minimal Vitest setup (this repo has none today) and cover the new utility with unit tests.

**Tech Stack:** React 18 + TypeScript, Vite 6, Vitest (new dependency)

**Repository:** `marmoraria-orcamentos-web` — this plan does not touch `marmoraria-orcamentos-api` at all. Work on a new branch in that repository (not `main`), e.g. `claude/auth-token-validation`.

**Spec:** `docs/superpowers/specs/2026-08-15-autenticacao-validacao-token-design.md` (in `marmoraria-orcamentos-api`, the repo this plan doc was authored from)

---

## File Structure

- Modify: `package.json` — add `vitest` devDependency and a `test` script.
- Create: `vitest.config.ts` — minimal test runner config.
- Create: `src/utils/jwt.ts` — `isTokenExpired(token: string): boolean`.
- Create: `src/utils/jwt.test.ts` — unit tests for the above.
- Modify: `src/contexts/AuthContext.tsx` — validate the stored token before trusting it.

---

### Task 1: Add Vitest to the project

**Files:**
- Modify: `package.json`
- Create: `vitest.config.ts`

- [ ] **Step 1: Add the `test` script**

In `package.json`, the `"scripts"` block currently reads:

```json
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
```

Change it to:

```json
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
```

- [ ] **Step 2: Install Vitest**

Run from the `marmoraria-orcamentos-web` project root:

```bash
npm install --save-dev vitest
```

Expected: `package.json`'s `devDependencies` now has a `"vitest": "^X.Y.Z"` entry added automatically, and `node_modules/.bin/vitest` exists.

- [ ] **Step 3: Create the Vitest config**

Create `vitest.config.ts`:

```ts
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
  },
});
```

`node` is enough here — the only thing under test in this plan is a pure function with no DOM dependency.

- [ ] **Step 4: Verify the install**

Run:

```bash
npx vitest --version
```

Expected: prints a version string (e.g. `vitest/3.x.x ...`) with no error.

- [ ] **Step 5: Commit**

```bash
git add package.json package-lock.json vitest.config.ts
git commit -m "chore: add vitest as the test runner"
```

---

### Task 2: `isTokenExpired` utility (TDD)

**Files:**
- Create: `src/utils/jwt.test.ts`
- Create: `src/utils/jwt.ts`

- [ ] **Step 1: Write the failing test**

Create `src/utils/jwt.test.ts`:

```ts
import { describe, it, expect } from 'vitest';
import { isTokenExpired } from './jwt';

function makeToken(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.fake-signature`;
}

describe('isTokenExpired', () => {
  it('returns false for a token whose exp is in the future', () => {
    const token = makeToken({ exp: Math.floor(Date.now() / 1000) + 3600 });
    expect(isTokenExpired(token)).toBe(false);
  });

  it('returns true for a token whose exp is in the past', () => {
    const token = makeToken({ exp: Math.floor(Date.now() / 1000) - 3600 });
    expect(isTokenExpired(token)).toBe(true);
  });

  it('returns true for a token missing the exp claim', () => {
    const token = makeToken({ sub: 'admin' });
    expect(isTokenExpired(token)).toBe(true);
  });

  it('returns true for a malformed token', () => {
    expect(isTokenExpired('not-a-jwt')).toBe(true);
  });

  it('returns true for an empty string', () => {
    expect(isTokenExpired('')).toBe(true);
  });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
npm test
```

Expected: FAIL — `src/utils/jwt.ts` does not exist yet, so the import fails to resolve (`Cannot find module './jwt'` or equivalent Vite/Rollup resolution error).

- [ ] **Step 3: Write the implementation**

Create `src/utils/jwt.ts`:

```ts
/**
 * Decodes the payload of a JWT and checks whether its `exp` claim has passed.
 * Any parsing failure (malformed token, missing claim) is treated as expired —
 * fail-safe: when in doubt, don't treat the token as valid.
 */
export function isTokenExpired(token: string): boolean {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return true;

    const base64Url = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64Url.padEnd(base64Url.length + ((4 - (base64Url.length % 4)) % 4), '=');
    const payload = JSON.parse(atob(padded)) as { exp?: unknown };

    if (typeof payload.exp !== 'number') return true;

    return payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
npm test
```

Expected: PASS — all 5 tests in `src/utils/jwt.test.ts` green.

- [ ] **Step 5: Commit**

```bash
git add src/utils/jwt.ts src/utils/jwt.test.ts
git commit -m "feat: add isTokenExpired JWT expiry check"
```

---

### Task 3: Wire the check into `AuthContext`

**Files:**
- Modify: `src/contexts/AuthContext.tsx`

- [ ] **Step 1: Replace the token initialization**

Current `src/contexts/AuthContext.tsx`:

```ts
import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { login as apiLogin } from '../api/auth.service';
import type { LoginRequest } from '../types/api.types';

interface AuthContextValue {
  token: string | null;
  isAuthenticated: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('jwtToken'));

  const login = useCallback(async (data: LoginRequest) => {
    const res = await apiLogin(data);
    localStorage.setItem('jwtToken', res.token);
    setToken(res.token);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwtToken');
    setToken(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, isAuthenticated: !!token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
```

Replace it with:

```ts
import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { login as apiLogin } from '../api/auth.service';
import { isTokenExpired } from '../utils/jwt';
import type { LoginRequest } from '../types/api.types';

interface AuthContextValue {
  token: string | null;
  isAuthenticated: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredToken(): string | null {
  const stored = localStorage.getItem('jwtToken');
  if (stored && !isTokenExpired(stored)) return stored;
  if (stored) localStorage.removeItem('jwtToken');
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(readStoredToken);

  const login = useCallback(async (data: LoginRequest) => {
    const res = await apiLogin(data);
    localStorage.setItem('jwtToken', res.token);
    setToken(res.token);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwtToken');
    setToken(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, isAuthenticated: !!token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
```

The only behavior change: `token` is now `null` from the very first render whenever the stored value is missing, malformed, or expired — `ProtectedRoute` (`src/router/ProtectedRoute.tsx`, unchanged) already redirects to `/login` whenever `isAuthenticated` is false, so this is enough to close the bug.

- [ ] **Step 2: Run the unit tests again**

```bash
npm test
```

Expected: still PASS (this step doesn't touch anything the existing tests cover, this just confirms nothing else broke).

- [ ] **Step 3: Manual verification in the browser**

There is no component-testing setup in this repo (no Testing Library) — see "Out of scope" in the spec. Verify by hand:

1. Run `npm run dev` and open the app.
2. Log in normally.
3. Open DevTools → Console and run:
   ```js
   const [h, , s] = localStorage.getItem('jwtToken').split('.');
   const expiredPayload = btoa(JSON.stringify({ ...JSON.parse(atob(localStorage.getItem('jwtToken').split('.')[1])), exp: Math.floor(Date.now() / 1000) - 3600 }));
   localStorage.setItem('jwtToken', `${h}.${expiredPayload}.${s}`);
   ```
   This rewrites the stored token's `exp` claim to one hour in the past, keeping the rest of the token shape intact.
4. Reload the page while on a protected route (e.g. `/dashboard`).
5. **Expected:** immediate redirect to `/login` — the dashboard never renders, not even briefly empty.
6. Log in again, reload `/dashboard` — **expected:** loads normally, no redirect (confirms the fix didn't break the valid-token path).

- [ ] **Step 4: Commit**

```bash
git add src/contexts/AuthContext.tsx
git commit -m "fix: validate JWT expiry before granting access to protected routes"
```

---

## Self-Review

**Spec coverage:** The spec's three concrete asks — decode `exp` client-side, wire it into `AuthContext` so `ProtectedRoute` never sees a stale token as valid, and plant a minimal Vitest base with a test for `isTokenExpired` — are each covered by Tasks 1–3. The spec's "fora de escopo" items (periodic re-validation mid-session, changing the `httpClient` interceptor's redirect mechanism, Testing Library) are intentionally not tasks here.

**Placeholder scan:** No TBD/TODO markers; every step has complete, runnable code.

**Type consistency:** `isTokenExpired` is imported and called identically in `jwt.test.ts` and `AuthContext.tsx` (`isTokenExpired(token: string): boolean`, named export from `./jwt` / `../utils/jwt`). `readStoredToken()` returns `string | null`, matching the `useState<string | null>` it initializes.
