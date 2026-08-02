#!/usr/bin/env python3
# Author: Viquar Khan
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from _common import expect, http_get, require_flink  # noqa: E402


def main() -> int:
    print("== 01 SRE job triage ==")
    overview = require_flink()
    expect("taskmanagers" in overview or "slots-total" in overview, "overview has capacity fields")

    code, body = http_get("/taskmanagers")
    expect(code == 200, f"/taskmanagers HTTP 200 (got {code})")
    tms = json.loads(body)
    expect("taskmanagers" in tms, "taskmanagers key present")
    print(f"OK  taskmanagers count={len(tms.get('taskmanagers') or [])}")

    code, body = http_get("/jobs/overview")
    expect(code == 200, f"/jobs/overview HTTP 200 (got {code})")
    jobs = json.loads(body)
    expect("jobs" in jobs, "jobs key present")
    print(f"OK  jobs count={len(jobs.get('jobs') or [])}")

    scenario = json.loads((Path(__file__).parent / "data" / "scenario.json").read_text(encoding="utf-8"))
    print(f"OK  persona={scenario['persona']}")
    print("PASS 01_sre_job_triage")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
