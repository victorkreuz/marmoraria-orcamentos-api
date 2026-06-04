-- V1__baseline.sql
-- Baseline schema matching the current Hibernate-managed state.
-- This migration will only run on a FRESH database.
-- For an existing production database, run:
--   flyway -url=jdbc:postgresql://HOST:5432/marmoraria_orcamentos \
--          -user=postgres -password=PASSWORD \
--          baseline -baselineVersion=1 -baselineDescription="baseline"

CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255),
    senha VARCHAR(255),
    tipo_usuario VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_username ON usuario(user_name);

CREATE TABLE IF NOT EXISTS cliente (
    id BIGSERIAL PRIMARY KEY,
    cod INTEGER,
    nome VARCHAR(255) NOT NULL,
    cpf_cnpj VARCHAR(255),
    email VARCHAR(255),
    telefone VARCHAR(255),
    endereco VARCHAR(255),
    cidade VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS produto_servico (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    unidade_medida VARCHAR(50) NOT NULL,
    valor_unitario NUMERIC(19,2) NOT NULL,
    imagem_path VARCHAR(255),
    imagem_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS observacao_orcamento (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255),
    texto TEXT NOT NULL,
    ativo BOOLEAN
);

CREATE TABLE IF NOT EXISTS meio_pagamento (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN
);

CREATE TABLE IF NOT EXISTS sequencia_orcamento (
    ano INTEGER PRIMARY KEY,
    ultimo_numero INTEGER
);

CREATE TABLE IF NOT EXISTS orcamento (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(255),
    cliente_id BIGINT REFERENCES cliente(id),
    cod INTEGER,
    data_emissao DATE NOT NULL,
    validade_dias INTEGER,
    prazo_execucao_dias INTEGER,
    data_validade DATE,
    status_orcamento VARCHAR(50) NOT NULL,
    valor_total NUMERIC(19,2),
    valor_desconto NUMERIC(19,2),
    valor_frete NUMERIC(19,2),
    -- Embedded: Projeto
    nome VARCHAR(255),
    tipo_pedra_principal VARCHAR(255),
    foto_principal_url VARCHAR(255),
    observacoes VARCHAR(255),
    responsavel_tecnico VARCHAR(255),
    -- Embedded: Financeiro
    subtotal_itens NUMERIC(19,2),
    frete_extra NUMERIC(19,2),
    frete_incluso BOOLEAN,
    desconto_percentual NUMERIC(19,2),
    desconto_valor_reais NUMERIC(19,2),
    adendos NUMERIC(19,2),
    total_final NUMERIC(19,2),
    valor_a_vista NUMERIC(19,2),
    entrada50pct NUMERIC(19,2),
    restante50pct NUMERIC(19,2),
    descricao_restante VARCHAR(255),
    meio_pagamento_id BIGINT,
    meio_pagamento_titulo VARCHAR(255),
    meio_pagamento_descricao VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS item_orcamento (
    id BIGSERIAL PRIMARY KEY,
    produto_servico_id BIGINT REFERENCES produto_servico(id),
    orcamento_id BIGINT REFERENCES orcamento(id),
    cod INTEGER,
    nome VARCHAR(255),
    descricao VARCHAR(255),
    tipo_pedra VARCHAR(255),
    acabamento VARCHAR(255),
    dimensoes VARCHAR(255),
    metros_quadrados NUMERIC(19,2),
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(19,2),
    subtotal NUMERIC(19,2),
    frete_incluso BOOLEAN,
    frete_valor NUMERIC(19,2),
    imagem_url VARCHAR(255),
    valor_unitario NUMERIC(19,2),
    valor_desconto NUMERIC(19,2),
    valor_total NUMERIC(19,2)
);

CREATE TABLE IF NOT EXISTS item_orcamento_imagem (
    id BIGSERIAL PRIMARY KEY,
    item_orcamento_id BIGINT REFERENCES item_orcamento(id),
    url VARCHAR(255) NOT NULL,
    public_id VARCHAR(255),
    nome_original VARCHAR(255),
    ordem INTEGER
);

CREATE TABLE IF NOT EXISTS orcamento_observacao (
    orcamento_id BIGINT REFERENCES orcamento(id),
    observacao_id BIGINT REFERENCES observacao_orcamento(id),
    PRIMARY KEY (orcamento_id, observacao_id)
);

CREATE TABLE IF NOT EXISTS orcamento_projeto_imagem (
    orcamento_id BIGINT REFERENCES orcamento(id),
    url VARCHAR(255) NOT NULL,
    titulo VARCHAR(255),
    public_id VARCHAR(255),
    nome_original VARCHAR(255),
    ordem INTEGER,
    ordem_lista INTEGER,
    UNIQUE (orcamento_id, ordem_lista)
);
