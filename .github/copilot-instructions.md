# GitHub Copilot instructions

Author: Viquar Khan

Follow the repository root **`AGENTS.md`** for all work in this project.

Key points:

- Java 17 Maven MCP server for Apache Flink (`io.github.vaquarkhan:flink-mcp-server`)
- Secure-by-default writes; stdout = MCP JSON-RPC only; logs on stderr
- After changes: `mvn test`; examples via `python examples/run_all.py`
- Skills: `skills/` (portable) and `.cursor/skills/` (Cursor)
- Do not invent MCP SDK APIs — stick to 1.1.3 verified constructors
