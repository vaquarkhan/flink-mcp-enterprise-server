# 04 — DLP secret egress guard

Author: Viquar Khan

## Persona
**Jordan**, security engineer. Worried that job exception payloads or metrics labels might leak API keys, JWTs, or emails back into the agent transcript.

## What this proves
- `OutputControls` redacts configured patterns and truncates long bodies.
- Runs against Java via a tiny Maven exec-free approach: invokes unit-equivalent regexes in Python matching the server patterns, and confirms Flink is up.

For the Java source of truth, see `GuardrailsTest.outputControls_*`.

## Agent prompt
> Fetch job exceptions. If any secret-looking material appears, it must be `<redacted>` in the tool result.

## Run
```bash
python examples/04_dlp_secret_egress/run.py
```
