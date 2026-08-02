# Author: Viquar Khan
"""Shared helpers for Flink MCP examples — talk to a real Flink REST API."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def flink_url() -> str:
    return os.environ.get("FLINK_REST_URL", "http://localhost:8081").rstrip("/")


def http_get(path: str, timeout: float = 10.0) -> tuple[int, str]:
    url = flink_url() + (path if path.startswith("/") else "/" + path)
    req = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            return resp.status, body
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        return e.code, body
    except Exception as e:
        raise SystemExit(f"FAIL: cannot reach Flink at {url}: {e}") from e


def require_flink() -> dict:
    code, body = http_get("/overview")
    if code != 200:
        raise SystemExit(f"FAIL: Flink /overview returned HTTP {code}: {body[:200]}")
    try:
        data = json.loads(body)
    except json.JSONDecodeError as e:
        raise SystemExit(f"FAIL: /overview not JSON: {e}") from e
    print(f"OK  Flink overview slots={data.get('slots-total')} tasks={data.get('taskmanagers')}")
    return data


def expect(cond: bool, msg: str) -> None:
    if not cond:
        raise SystemExit(f"FAIL: {msg}")
    print(f"OK  {msg}")


def jar_path() -> Path | None:
    target = ROOT / "target"
    if not target.exists():
        return None
    cands = sorted(target.glob("flink-mcp-server-*-all.jar"))
    return cands[-1] if cands else None
