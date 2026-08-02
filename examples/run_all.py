#!/usr/bin/env python3
# Author: Viquar Khan
"""
Download-and-run orchestrator for Flink MCP examples.

Usage (from repo root):
  python examples/run_all.py
  python examples/run_all.py --skip-build
  python examples/run_all.py --start-flink
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXAMPLES = Path(__file__).resolve().parent


def log(msg: str) -> None:
    print(msg, flush=True)


def flink_url() -> str:
    return os.environ.get("FLINK_REST_URL", "http://localhost:8081").rstrip("/")


def flink_up() -> bool:
    try:
        with urllib.request.urlopen(flink_url() + "/overview", timeout=3) as r:
            return r.status == 200
    except Exception:
        return False


def wait_flink(timeout_sec: int = 120) -> None:
    log(f"Waiting for Flink at {flink_url()} ...")
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        if flink_up():
            body = urllib.request.urlopen(flink_url() + "/overview", timeout=5).read().decode()
            log(f"OK  Flink is up: {body[:120]}...")
            return
        time.sleep(2)
    raise SystemExit(f"FAIL: Flink not reachable at {flink_url()} after {timeout_sec}s")


def start_flink() -> None:
    compose = EXAMPLES / "docker-compose.yml"
    log("Starting Flink via docker compose ...")
    subprocess.run(
        ["docker", "compose", "-f", str(compose), "up", "-d"],
        cwd=str(ROOT),
        check=True,
    )


def ensure_jar(skip_build: bool) -> Path:
    jars = sorted((ROOT / "target").glob("flink-mcp-server-*-all.jar")) if (ROOT / "target").exists() else []
    if jars:
        log(f"OK  using jar {jars[-1].name}")
        return jars[-1]
    if skip_build:
        raise SystemExit("FAIL: no shaded jar found; run mvn package or omit --skip-build")
    log("Building shaded jar (mvn -DskipTests package) ...")
    subprocess.run(["mvn", "-q", "-DskipTests", "package"], cwd=str(ROOT), check=True)
    jars = sorted((ROOT / "target").glob("flink-mcp-server-*-all.jar"))
    if not jars:
        raise SystemExit("FAIL: build succeeded but jar missing")
    log(f"OK  built {jars[-1].name}")
    return jars[-1]


def run_script(rel: str) -> None:
    path = EXAMPLES / rel if not rel.startswith("_") else EXAMPLES / rel
    # allow "01_sre_job_triage/run.py" or "_mcp_smoke.py"
    script = EXAMPLES / rel
    log(f"\n=== Running {rel} ===")
    env = os.environ.copy()
    env.setdefault("FLINK_REST_URL", "http://localhost:8081")
    proc = subprocess.run([sys.executable, str(script)], cwd=str(ROOT), env=env)
    if proc.returncode != 0:
        raise SystemExit(f"FAIL: {rel} exited {proc.returncode}")
    log(f"PASS {rel}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run all Flink MCP examples against a real cluster")
    parser.add_argument("--start-flink", action="store_true", help="docker compose up examples/docker-compose.yml")
    parser.add_argument("--skip-build", action="store_true", help="do not mvn package if jar missing")
    parser.add_argument("--skip-mcp-smoke", action="store_true", help="skip MCP stdio tools/list smoke")
    args = parser.parse_args()

    os.environ.setdefault("FLINK_REST_URL", "http://localhost:8081")
    log("Flink MCP Enterprise — example runner")
    log(f"Repo root: {ROOT}")

    if args.start_flink or not flink_up():
        if args.start_flink or input_auto_start():
            start_flink()
        wait_flink()
    else:
        wait_flink(timeout_sec=5)

    ensure_jar(args.skip_build)

    run_script("01_sre_job_triage/run.py")
    run_script("02_sql_readonly_guard/run.py")
    run_script("03_change_window_approvals/run.py")
    run_script("04_dlp_secret_egress/run.py")
    if not args.skip_mcp_smoke:
        run_script("_mcp_smoke.py")

    log("\nALL EXAMPLES PASSED")
    log("Next: wire the jar into Cursor — see doc/getting-started.md")
    return 0


def input_auto_start() -> bool:
    # Non-interactive: auto-start when Flink is down
    log("Flink is down — starting docker compose automatically")
    return True


if __name__ == "__main__":
    raise SystemExit(main())
