#!/usr/bin/env python3
# Author: Viquar Khan
"""MCP stdio smoke: initialize + tools/list against the shaded jar (if built)."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _common import expect, jar_path, require_flink  # noqa: E402


def main() -> int:
    require_flink()
    jar = jar_path()
    expect(jar is not None, "shaded jar present (run mvn package)")
    assert jar is not None

    messages = [
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "example-smoke", "version": "0.1"},
            },
        },
        {"jsonrpc": "2.0", "method": "notifications/initialized"},
        {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
    ]
    payload = ("\n".join(json.dumps(m) for m in messages) + "\n").encode("utf-8")

    env = os.environ.copy()
    env.setdefault("FLINK_REST_URL", "http://localhost:8081")
    env["MCP_FLINK_LOG_LEVEL"] = "WARN"

    proc = subprocess.Popen(
        ["java", "-jar", str(jar)],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
        bufsize=0,
    )
    assert proc.stdin and proc.stdout and proc.stderr

    out_chunks: list[bytes] = []
    err_chunks: list[bytes] = []

    def pump(stream, buf: list[bytes]) -> None:
        while True:
            b = stream.read(1)
            if not b:
                break
            buf.append(b)

    threading.Thread(target=pump, args=(proc.stdout, out_chunks), daemon=True).start()
    threading.Thread(target=pump, args=(proc.stderr, err_chunks), daemon=True).start()

    # Give the JVM a moment to boot before writing (stdio is ready after "stdio transport ready")
    time.sleep(5)
    proc.stdin.write(payload)
    proc.stdin.flush()

    deadline = time.time() + 40
    text = ""
    while time.time() < deadline:
        text = b"".join(out_chunks).decode("utf-8", errors="replace")
        json_lines = [ln for ln in text.splitlines() if ln.strip().startswith("{")]
        if len(json_lines) >= 2:
            break
        time.sleep(0.2)
    else:
        proc.kill()
        err = b"".join(err_chunks).decode("utf-8", errors="replace")
        raise SystemExit(f"FAIL: MCP smoke timed out; out={text[:300]!r} err={err[-500:]!r}")

    try:
        proc.stdin.close()
    except Exception:
        pass
    proc.kill()
    try:
        proc.wait(timeout=5)
    except Exception:
        pass

    json_lines = [ln for ln in text.splitlines() if ln.strip().startswith("{")]
    expect(len(json_lines) >= 2, f"MCP JSON responses on stdout (got {len(json_lines)})")
    tools_msg = json.loads(json_lines[-1])
    tools = (tools_msg.get("result") or {}).get("tools") or []
    names = {t.get("name") for t in tools if isinstance(t, dict)}
    expect("list_jobs" in names, "list_jobs exposed")
    expect("get_cluster_info" in names, "get_cluster_info exposed")
    expect("stop_job" not in names, "stop_job hidden in default read profile")
    print(f"OK  tools/list count={len(names)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
