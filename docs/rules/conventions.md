# Code conventions

- Backend: Java records + Bean Validation for API DTOs and AI-signal schemas (parse AI output with Jackson, reject unknown/invalid fields); business logic in services, not controllers.
- Config via `@ConfigurationProperties` and Spring profiles; `paper` is the default profile, `live` must be enabled explicitly (and per account).
- Money/prices/quantities: use `BigDecimal` (`NUMERIC` in PostgreSQL), never `double`/`float`, in the DB and order logic. Always store the currency with money values.
- Times stored in UTC.
- Keep each AI provider adapter, the AI router, each broker adapter and the risk engine as separate modules so each can be tested/mocked independently.
- Broker-specific code, DTOs and quirks stay inside that broker's adapter package; document each adapter's quirks in a short `README.md` in its package.
