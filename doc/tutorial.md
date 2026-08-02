# Developer tutorial

Author: Viquar Khan

A human walkthrough: you are an engineer wiring Flink into an AI assistant for the first time. We go from “cluster is up” to “governed destructive stop” without skipping the scary parts.

## Persona
**You:** platform / streaming engineer  
**Goal:** let an IDE agent inspect Flink safely, then unlock one approved write during a change window  
**Constraint:** production reviewers will read every config and every deny path

---

## Step 1 — Prove Flink is alive (no MCP yet)
```bash
curl -s http://localhost:8081/overview | head
curl -s http://localhost:8081/taskmanagers | head
curl -s http://localhost:8081/jobs/overview | head
```

If these fail, fix Flink first. The MCP server is a control plane — it cannot invent a cluster.

Run the automated check:

```bash
python examples/01_sre_job_triage/run.py
```

---

## Step 2 — Build the MCP server
```bash
mvn clean package -q
ls target/flink-mcp-server-0.2.0-all.jar
```

Expect **39+** unit tests green from `mvn test`.

---

## Step 3 — Attach the agent (read-only)
Add the Cursor / Claude config from [getting-started.md](getting-started.md). Ask the agent:

> “List Flink jobs and summarize TaskManager slots. Do not change anything.”

Expected tool calls: `list_jobs`, `list_taskmanagers`, `get_cluster_info`.

Watch stderr:

```text
... INFO  ... trace=a1b2c3d4e5f60708 i.g.v.flinkmcp.governance.Governance - call tool=list_jobs ...
... INFO  ... trace=a1b2c3d4e5f60708 i.g.v.flinkmcp.governance.Governance - allowed tool=list_jobs ms=42
```

That `trace=` id is your correlation handle across logs, audit, and denials.

---

## Step 4 — Try something destructive (should fail)
Ask:

> “Stop job `abc` right now.”

With default config the tool is **not even registered**. If you force `tools/call` for `stop_job` via a custom allow-list without writes unlocked, governance returns `denied: NOT_EXPOSED` or writes stay locked.

Validate the SQL guard:

```bash
python examples/02_sql_readonly_guard/run.py
```

---

## Step 5 — Unlock writes the right way
```bash
export MCP_FLINK_WRITE_ENABLED=true
export MCP_FLINK_APPROVAL_SECRET='dev-only-secret-change-me'
export MCP_FLINK_JAR_UPLOAD_ALLOW_DIRS="/tmp/flink-jars"   # required for upload_jar
# optional deny file
export MCP_FLINK_POLICY_FILE=config/policy.sample
```

Mint a single-use token:

```bash
java -cp target/flink-mcp-server-0.2.0-all.jar \
  io.github.vaquarkhan.flinkmcp.security.ApprovalTokens \
  "$MCP_FLINK_APPROVAL_SECRET" stop_job <jobId> 300
```

Pass that string as `approvalToken` to `stop_job`. Replay the same token → deny (`APPROVAL_REQUIRED` / nonce).

Full scenario:

```bash
python examples/03_change_window_approvals/run.py
```

---

## Step 6 — Observe like an operator
HTTP mode:

```bash
curl -s http://127.0.0.1:8090/healthz
curl -s http://127.0.0.1:8090/readyz
curl -s http://127.0.0.1:8090/metrics | head
```

MCP resources (via agent): `flink://audit/recent`, `flink://metrics`, `flink://health`.

DLP demo:

```bash
python examples/04_dlp_secret_egress/run.py
```

---

## Step 7 — What good looks like in review
| Check | Pass criteria |
|-------|----------------|
| Secure default | Writes off; no `stop_job` without secret |
| Path safety | Job/jar IDs validated; jar paths allow-listed |
| Logs | stderr only; MDC `trace=` present |
| Tests | `mvn test` green; example runners exit 0 |
| Docs | Config + threat model match the code |

You are done when an agent can triage Flink without holding a god principal — and every write leaves an audit hash.
