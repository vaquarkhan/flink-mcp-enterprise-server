# Agents & skills

Author: Viquar Khan

Instructions so coding agents (Cursor, Copilot, ChatGPT, Gemini, Claude Code, …) work this repo safely.

| Asset | Path |
|-------|------|
| Canonical agent brief | [`../AGENTS.md`](../AGENTS.md) |
| Cursor skills | [`.cursor/skills/`](../.cursor/skills/) |
| Portable skills | [`../skills/`](../skills/) |

## Load in Cursor
Skills under `.cursor/skills/*/SKILL.md` are discovered automatically when this repo is open.

## Load elsewhere
Copy `skills/flink-mcp-enterprise/SKILL.md` into your agent’s skill folder, or paste `AGENTS.md` into the system prompt for the session.

## Skills shipped
| Skill | Use when |
|-------|----------|
| `flink-mcp-enterprise` | Building, testing, configuring the server |
| `flink-mcp-examples` | Adding / running persona examples |
| `flink-mcp-security-review` | Changing governance, approvals, DLP, policy |
