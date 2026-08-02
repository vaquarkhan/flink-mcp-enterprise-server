# Tools & resources

Author: Viquar Khan

## Read tools (default profile)
| Tool | Flink mapping | Args |
|------|---------------|------|
| `get_cluster_info` | `GET /overview` | — |
| `get_flink_config` | `GET /config` | — |
| `list_taskmanagers` | `GET /taskmanagers` | — |
| `list_jobs` | `GET /jobs/overview` | — |
| `get_job` | `GET /jobs/{jobId}` | `jobId` |
| `get_job_status` | `GET /jobs/{jobId}/status` | `jobId` |
| `get_job_config` | `GET /jobs/{jobId}/config` | `jobId` |
| `get_job_exceptions` | `GET /jobs/{jobId}/exceptions` | `jobId` |
| `get_job_metrics` | `GET /jobs/{jobId}/metrics` | `jobId` |
| `list_checkpoints` | `GET /jobs/{jobId}/checkpoints` | `jobId` |
| `list_jars` | `GET /jars` | — |
| `run_sql_readonly` | SQL Gateway execute | `sql` (SELECT/WITH/SHOW/DESCRIBE/EXPLAIN) |

## Write tools (gated)
Require `MCP_FLINK_WRITE_ENABLED=true` + `MCP_FLINK_APPROVAL_SECRET` + `approvalToken`.

| Tool | Class | Mapping |
|------|-------|---------|
| `trigger_savepoint` | MUTATE | `POST /jobs/{jobId}/savepoints` |
| `rescale_job` | MUTATE | `PATCH …/rescaling?parallelism=N` |
| `upload_jar` | MUTATE | `POST /jars/upload` (path allow-list) |
| `run_jar` | DESTRUCTIVE | `POST /jars/{jarId}/run` |
| `stop_job` | DESTRUCTIVE | `POST /jobs/{jobId}/stop` |
| `cancel_job` | DESTRUCTIVE | `PATCH /jobs/{jobId}` `{"mode":"cancel"}` |
| `run_sql_ddl_dml` | DESTRUCTIVE | SQL Gateway execute |

## Resources
| URI | Content |
|-----|---------|
| `flink://cluster/overview` | Flink `/overview` |
| `flink://jobs` | Jobs overview |
| `flink://health` | MCP version + Flink/Gateway ping + writes flag |
| `flink://audit/recent` | Hash-chained audit lines |
| `flink://metrics` | JSON metrics |
| `flink://metrics/prometheus` | Prometheus text |
