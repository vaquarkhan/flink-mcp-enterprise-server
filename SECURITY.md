# Security Policy

Author: Viquar Khan

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.2.x   | Yes |
| 0.1.x   | Best effort |

## Reporting a vulnerability

Please **do not** open a public issue for security vulnerabilities.

1. Use [GitHub Security Advisories](https://github.com/vaquarkhan/flink-mcp-enterprise-server/security/advisories/new) for this repository, **or**
2. Contact the maintainer privately via GitHub (@vaquarkhan).

Include:

- Affected version / commit
- Reproduction steps
- Impact (e.g. auth bypass, path traversal, secret leakage on stdout)

You should receive an acknowledgement within a few days.

## Security model (summary)

- Writes are off by default; destructive tools require approval tokens.
- HTTP mode requires Bearer auth (fail-closed).
- Tool outputs may be DLP-redacted; treat agent transcripts as sensitive.
- Flink / Gateway credentials (if set via auth headers) never belong in git.

See [`doc/security-controls.md`](doc/security-controls.md).
