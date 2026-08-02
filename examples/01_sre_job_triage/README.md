# 01 — SRE job triage (read-only)

Author: Viquar Khan

## Persona
**Alex**, night-shift SRE. PagerDuty fired for “Flink slots low.” Alex must inspect the cluster without the ability to cancel jobs by accident.

## What this proves
- Real Flink `/overview`, `/taskmanagers`, `/jobs/overview` respond.
- MCP default profile exposes inspect tools only (validated when jar is built).

## Agent prompt (Cursor / Claude)
> Check Flink cluster overview, TaskManagers, and job list. Summarize free slots. Do not cancel or stop anything.

Expected tools: `get_cluster_info`, `list_taskmanagers`, `list_jobs`.

## Run
```bash
export FLINK_REST_URL=http://localhost:8081
python examples/01_sre_job_triage/run.py
```

## Data
See `data/scenario.json` for the checklist Alex walks.
