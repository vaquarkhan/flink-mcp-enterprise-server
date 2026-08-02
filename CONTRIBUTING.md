# Contributing

Author: Viquar Khan

Thanks for helping improve **Flink MCP Enterprise Server**.

## Quick path

1. Fork and clone the repo.
2. Use JDK 17+ and Maven 3.9+.
3. `mvn clean test`
4. For end-to-end examples (Docker + Python): see [`examples/README.md`](examples/README.md).
5. Open a PR against `main`.

## Guidelines

- Read [`AGENTS.md`](AGENTS.md) — same rules for humans and coding agents.
- Prefer small, focused PRs.
- Keep **stdout MCP-only**; logs on stderr.
- Do not weaken secure-by-default writes without a clear design note in the PR.
- New deny codes → update [`doc/error-codes.md`](doc/error-codes.md) and tests.
- Java sources must be **UTF-8 without BOM**.
- Add `@author Viquar Khan` (or keep existing author tags) on new types.

## Checks before you push

```bash
mvn clean package
python examples/run_all.py --skip-build   # if Flink is running
```

## Docs & site

- Product docs: `doc/`
- Publish guide: `docs/PUBLISHING.md`
- Static site: `site/` (GitHub Pages via `.github/workflows/pages.yml`)

## Code of conduct

See [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).

## Security reports

See [`SECURITY.md`](SECURITY.md).
