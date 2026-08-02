# Examples — download and run

Author: Viquar Khan

Full working examples for **Flink MCP Enterprise Server**.  
Clone the repo, start a real Flink cluster, build the jar, and run every scenario with one command.

## One-command run

### Prerequisites

- Git, JDK 17+, Maven 3.9+, Python 3.9+, Docker

### Windows (PowerShell)

```powershell
git clone https://github.com/vaquarkhan/flink-mcp-enterprise-server.git
cd flink-mcp-enterprise-server
.\examples\run_all.ps1
```

### macOS / Linux

```bash
git clone https://github.com/vaquarkhan/flink-mcp-enterprise-server.git
cd flink-mcp-enterprise-server
chmod +x examples/run_all.sh
./examples/run_all.sh
```

### Or step-by-step

```bash
git clone https://github.com/vaquarkhan/flink-mcp-enterprise-server.git
cd flink-mcp-enterprise-server

mvn -DskipTests package
docker compose -f examples/docker-compose.yml up -d
python examples/run_all.py --skip-build
```

What `run_all` does:

1. Starts Flink JobManager + TaskManager (`examples/docker-compose.yml`) if needed  
2. Builds `target/flink-mcp-server-*-all.jar` if missing  
3. Runs all persona examples against **real** Flink REST (`http://localhost:8081`)  
4. Smokes MCP `initialize` + `tools/list` over stdio against the jar  

Exit code **0** = everything passed.

---

## Example catalog

| Folder | Persona | What it validates |
|--------|---------|-------------------|
| [`01_sre_job_triage/`](01_sre_job_triage/) | Night-shift SRE | Live `/overview`, `/taskmanagers`, `/jobs/overview` |
| [`02_sql_readonly_guard/`](02_sql_readonly_guard/) | Analytics engineer | SELECT allowed; INSERT/DDL/stacked denied |
| [`03_change_window_approvals/`](03_change_window_approvals/) | Platform on-call | Mint approval tokens via Java CLI |
| [`04_dlp_secret_egress/`](04_dlp_secret_egress/) | Security engineer | DLP redaction + truncation |
| [`_mcp_smoke.py`](_mcp_smoke.py) | Integrator | MCP stdio tools/list (read profile, no `stop_job`) |

Each folder has:

```text
0N_name/
  README.md     # persona + agent prompt
  run.py        # executable — download and run
  data/         # fixtures
```

Run one scenario:

```bash
export FLINK_REST_URL=http://localhost:8081
python examples/01_sre_job_triage/run.py
```

---

## After examples: use the MCP server

```bash
FLINK_REST_URL=http://localhost:8081 \
java -jar target/flink-mcp-server-0.2.0-all.jar
```

Cursor / Claude config: [../doc/getting-started.md](../doc/getting-started.md)  
Tutorial: [../doc/tutorial.md](../doc/tutorial.md)

---

## Stop Flink

```bash
docker compose -f examples/docker-compose.yml down
```
