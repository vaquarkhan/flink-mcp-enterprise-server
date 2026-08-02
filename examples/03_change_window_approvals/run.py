#!/usr/bin/env python3
# Author: Viquar Khan
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from _common import expect, jar_path, require_flink  # noqa: E402


def main() -> int:
    print("== 03 change window approvals ==")
    require_flink()
    ticket = json.loads((Path(__file__).parent / "data" / "change_ticket.json").read_text(encoding="utf-8"))
    jar = jar_path()
    expect(jar is not None, "shaded jar present")
    assert jar is not None

    def mint() -> str:
        cp = subprocess.run(
            [
                "java",
                "-cp",
                str(jar),
                "io.github.vaquarkhan.flinkmcp.security.ApprovalTokens",
                ticket["secret"],
                ticket["tool"],
                ticket["jobId"],
                str(ticket["ttl_seconds"]),
            ],
            check=True,
            capture_output=True,
            text=True,
        )
        token = cp.stdout.strip().splitlines()[-1].strip()
        expect("." in token and len(token) > 20, "minted token looks well-formed")
        return token

    t1 = mint()
    t2 = mint()
    expect(t1 != t2, "each mint produces a distinct nonce/token")
    print(f"OK  ticket={ticket['change_ticket']} tool={ticket['tool']}")
    print("PASS 03_change_window_approvals")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
