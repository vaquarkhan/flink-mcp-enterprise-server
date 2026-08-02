#!/usr/bin/env python3
# Author: Viquar Khan
from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from _common import expect, require_flink  # noqa: E402

PATTERNS = [
    re.compile(r'(?i)(api[_-]?key|secret|password|passwd|token)\s*[=:]\s*"?[^"\s,}]+'),
    re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),
    re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"),
    re.compile(r"(?i)Bearer\s+[A-Za-z0-9._-]+"),
]


def redact(s: str, max_bytes: int = 64) -> str:
    out = s
    for p in PATTERNS:
        out = p.sub("<redacted>", out)
    if len(out) > max_bytes:
        out = out[:max_bytes] + "...<truncated>"
    return out


def main() -> int:
    print("== 04 DLP secret egress ==")
    require_flink()
    sample = (
        'password=supersecret user@example.com '
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk "
        "Bearer abc.def-ghi"
    )
    out = redact(sample, 80)
    expect("<redacted>" in out, "secrets replaced with <redacted>")
    expect("supersecret" not in out, "raw password not present")
    expect("user@example.com" not in out, "email redacted")
    expect("...<truncated>" in out or len(out) <= 80 + len("...<truncated>"), "bounded")
    print("PASS 04_dlp_secret_egress")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
