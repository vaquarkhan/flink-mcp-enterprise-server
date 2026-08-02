---
name: flink-mcp-examples
description: >
  Create or run download-and-run Flink MCP examples against a real Flink
  cluster (docker compose + run_all). Use when the user asks for demos,
  tutorials, persona scenarios, or example runners.
---

# Flink MCP examples — agent skill

Author: Viquar Khan

## Layout

```text
examples/
  docker-compose.yml      # Flink 1.20 JM + TM
  run_all.py|.ps1|.sh     # one-command orchestrator
  0N_slug/{README.md,run.py,data/}
  _common.py              # Flink REST helpers
  _mcp_smoke.py          # stdio initialize + tools/list
```

## Always do

1. Examples must call **real** Flink (`FLINK_REST_URL`) when claiming cluster validation.
2. `run.py` exits **0** on pass, **non-zero** on fail.
3. Wire new scenarios into `examples/run_all.py`.
4. Document persona + agent prompt in each `README.md`.
5. Keep `# Author: Viquar Khan` on Python files.

## Run

```bash
docker compose -f examples/docker-compose.yml up -d
mvn -DskipTests package
python examples/run_all.py --skip-build
```
