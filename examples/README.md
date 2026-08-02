# Examples

Author: Viquar Khan

Production-grade scenarios for **Flink MCP Enterprise Server**.

Each folder is self-contained: `README.md` (persona + story), `run.py`, and `data/` fixtures.
Runners talk to a **real Flink REST API** (`FLINK_REST_URL`) and, when present, the shaded jar for MCP smoke.

## Prerequisites
```bash
# Flink must answer:
curl -s http://localhost:8081/overview

# Optional Docker:
docker network create flink-net
docker run -d --name flink-jobmanager --network flink-net -p 8081:8081 \
  -e JOB_MANAGER_RPC_ADDRESS=flink-jobmanager flink:1.20-java17 jobmanager
docker run -d --name flink-taskmanager --network flink-net \
  -e JOB_MANAGER_RPC_ADDRESS=flink-jobmanager flink:1.20-java17 taskmanager
```

## Run (from repository root)
```bash
export FLINK_REST_URL=http://localhost:8081
python examples/01_sre_job_triage/run.py
python examples/02_sql_readonly_guard/run.py
python examples/03_change_window_approvals/run.py
python examples/04_dlp_secret_egress/run.py
python examples/_mcp_smoke.py
```

## Catalog
| Folder | Persona | Story |
|--------|---------|-------|
| [`01_sre_job_triage/`](01_sre_job_triage/) | Night-shift SRE | Inspect cluster, TMs, jobs — read only |
| [`02_sql_readonly_guard/`](02_sql_readonly_guard/) | Analytics engineer | SQL guard allows SELECT, blocks INSERT/DDL |
| [`03_change_window_approvals/`](03_change_window_approvals/) | Platform on-call | Mint approval + deny replay for `stop_job` |
| [`04_dlp_secret_egress/`](04_dlp_secret_egress/) | Security engineer | Output DLP redacts secrets / JWT / email |

## Layout
```text
examples/
  _common.py
  _mcp_smoke.py
  0N_name/
    README.md
    run.py
    data/
```

Exit **0** = controls behaved; **non-zero** = Flink down or assertion failed.
