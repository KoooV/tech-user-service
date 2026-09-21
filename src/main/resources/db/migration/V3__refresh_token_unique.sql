-- V3: refresh-токены глобально уникальны.
-- JwtServiceImpl кладёт jti в каждый refresh-токен, этот constraint страхует
-- на уровне БД: findByToken всегда возвращает максимум одну строку
-- (иначе NonUniqueResult → 500 на POST /api/auth/refresh).

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uk_refresh_tokens_token UNIQUE (token);
