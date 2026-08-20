-- AI 运行时配置：全站一行。密钥只给管理端读写，不出现在公开接口。
CREATE TABLE ai_settings (
    id                  SMALLINT PRIMARY KEY,
    enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    assistant_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    chat_base_url       VARCHAR(500),
    chat_api_key        VARCHAR(500),
    chat_model          VARCHAR(200),
    embed_base_url      VARCHAR(500),
    embed_api_key       VARCHAR(500),
    embed_model         VARCHAR(200),
    embed_dimensions    INTEGER      NOT NULL DEFAULT 1536,
    assistant_persona   VARCHAR(2000),
    rate_per_minute     INTEGER      NOT NULL DEFAULT 10,
    rate_per_day        INTEGER      NOT NULL DEFAULT 50,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_ai_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_ai_settings_embed_dim CHECK (embed_dimensions BETWEEN 8 AND 4096),
    CONSTRAINT chk_ai_settings_rate_minute CHECK (rate_per_minute BETWEEN 1 AND 120),
    CONSTRAINT chk_ai_settings_rate_day CHECK (rate_per_day BETWEEN 1 AND 2000)
);

INSERT INTO ai_settings (
    id, enabled, assistant_enabled, chat_base_url, chat_api_key, chat_model,
    embed_base_url, embed_api_key, embed_model, embed_dimensions,
    assistant_persona, rate_per_minute, rate_per_day, updated_at
) VALUES (
    1, FALSE, FALSE, '', '', '',
    '', '', '', 1536,
    '', 10, 50, TIMESTAMPTZ '2026-01-01 00:00:00+00'
);
