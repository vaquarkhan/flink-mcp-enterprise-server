#!/usr/bin/env python3
# Author: Viquar Khan
"""Validate SQL readonly policy (mirrors SqlReadonlyGuard) + Flink reachability."""
from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from _common import expect, require_flink  # noqa: E402

ALLOWED = ("SELECT", "WITH", "SHOW", "DESCRIBE", "DESC", "EXPLAIN")
BLOCK = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE = re.compile(r"--[^\n]*")


def is_read_only(sql: str | None) -> bool:
    if sql is None:
        return False
    cleaned = BLOCK.sub(" ", sql)
    cleaned = LINE.sub(" ", cleaned).strip()
    if not cleaned:
        return False
    last = cleaned.rfind(";")
    if last >= 0:
        before = cleaned[:last]
        if ";" in before:
            return False
        if last == len(cleaned) - 1:
            cleaned = cleaned[:last].strip()
        else:
            return False
    upper = cleaned.upper()
    for p in ALLOWED:
        if upper == p or upper.startswith(p + " ") or upper.startswith(p + "\n") or upper.startswith(p + "\t"):
            return True
    return False


def main() -> int:
    print("== 02 SQL readonly guard ==")
    require_flink()
    for ok in ["SELECT 1", "show tables", "DESCRIBE t", "WITH x AS (SELECT 1) SELECT * FROM x", "EXPLAIN SELECT 1"]:
        expect(is_read_only(ok), f"allow: {ok}")
    for bad in ["INSERT INTO t VALUES (1)", "CREATE TABLE t (id INT)", "DROP TABLE t", "SELECT 1; SELECT 2", None]:
        expect(not is_read_only(bad), f"deny: {bad}")
    print("PASS 02_sql_readonly_guard")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
