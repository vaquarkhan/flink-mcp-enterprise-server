# Testing

Author: Viquar Khan

## Unit tests
```bash
mvn clean test
# Expected: Tests run: 39+, Failures: 0
```

Coverage report: `target/site/jacoco/index.html`.

## Example runners (real Flink)
```bash
export FLINK_REST_URL=http://localhost:8081
python examples/01_sre_job_triage/run.py
python examples/02_sql_readonly_guard/run.py
python examples/03_change_window_approvals/run.py
python examples/04_dlp_secret_egress/run.py
```

Each exits **0** on success, **non-zero** if Flink is unreachable or an expected control did not fire.

## MCP smoke against the jar
```bash
python examples/_mcp_smoke.py
```
