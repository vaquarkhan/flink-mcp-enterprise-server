# 02 — SQL readonly guard

Author: Viquar Khan

## Persona
**Priya**, analytics engineer. Wants the agent to explore catalogs with SQL — never `DROP` or `INSERT` through the readonly tool.

## What this proves
- `SqlReadonlyGuard` allows SELECT / SHOW / DESCRIBE / WITH / EXPLAIN.
- Rejects INSERT, CREATE, DROP, stacked statements, null.

This example runs the guard logic via a tiny inline reimplementation matching server rules, and documents the MCP tool contract. Prefer unit tests in `GuardrailsTest` for the Java source of truth; this runner is for demos + CI when Flink is up (health check) and docs stay honest.

## Agent prompt
> Run `SHOW JOBS` style discovery via read-only SQL. If I paste `DROP TABLE t`, refuse.

## Run
```bash
python examples/02_sql_readonly_guard/run.py
```
