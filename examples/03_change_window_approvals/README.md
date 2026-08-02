# 03 — Change-window approvals

Author: Viquar Khan

## Persona
**Sam**, platform on-call during a Friday change window. A deprecated streaming job must be stopped — but only with a minted, single-use approval token.

## What this proves
- Approval mint/verify round-trip (via Java `ApprovalTokens` CLI when jar exists).
- Replay is rejected.
- Flink cluster is reachable so the change window is real ops context.

## Agent prompt
> Stop job `<id>` only if I provide an approval token. If the token was already used, refuse.

## Run
```bash
mvn -q -DskipTests package   # once
python examples/03_change_window_approvals/run.py
```
