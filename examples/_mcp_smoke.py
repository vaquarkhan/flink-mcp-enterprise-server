# Author: Viquar Khan
"""MCP stdio smoke: initialize + tools/list against the shaded jar (if built)."""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import expect, jar_path, require_flink  # noqa: E402


def main() -> int:
    require_flink()
    jar = jar_path()
    expect(jar is not None, "shaded jar present (run mvn package)")
    assert jar is not None

    payload = "\n".join(
        [
            json.dumps(
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "initialize",
                    "params": {
                        "protocolVersion": "2024-11-05",
                        "capabilities": {},
                        "clientInfo": {"name": "example-smoke", "version": "0.1"},
                    },
                }
            ),
            json.dumps({"jsonrpc": "2.0", "method": "notifications/initialized"}),
            json.dumps({"jsonrpc": "2.0", "id": 2, "method": "tools/list"}),
        ]
    ) + "\n"

    env = os.environ.copy()
    env.setdefault("FLINK_REST_URL", "http://localhost:8081")
    env["MCP_FLINK_LOG_LEVEL"] = "WARN"

    proc = subprocess.run(
        ["java", "-jar", str(jar)],
        input=payload.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=45,
        env=env,
        check=False,
    )
    out = proc.stdout.decode("utf-8", errors="replace")
    lines = [ln for ln in out.splitlines() if ln.strip().startswith("{")]
    expect(len(lines) >= 2, f"MCP JSON responses on stdout (got {len(lines)} lines)")
    tools_msg = json.loads(lines[-1])
    tools = tools_msg.get("result", {}).get("tools") or tools_msg.get("tools") or []
    names = {t.get("name") for t in tools if isinstance(t, dict)}
    expect("list_jobs" in names, "list_jobs exposed")
    expect("get_cluster_info" in names, "get_cluster_info exposed")
    expect("stop_job" not in names, "stop_job hidden in default read profile")
    print(f"OK  tools/list count={len(names)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
