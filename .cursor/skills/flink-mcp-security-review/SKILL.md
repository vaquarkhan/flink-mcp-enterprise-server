---
name: flink-mcp-security-review
description: >
  Review or change Flink MCP governance and security: approval tokens,
  policy engine, scopes, DLP, Bearer auth, rate limits, circuit breaker.
  Use when the user mentions approvals, deny codes, path injection, or writes unlock.
---

# Flink MCP security review — agent skill

Author: Viquar Khan

## Checklist

- [ ] Writes remain off without secret (`Config.validate` / `writesUnlocked`)
- [ ] HTTP refuses start without Bearer
- [ ] Governance order unchanged unless docs + tests updated
- [ ] New deny codes in `doc/error-codes.md`
- [ ] IDs validated before REST path concat
- [ ] Jar uploads require allow-listed dirs
- [ ] SQL readonly guard rejects mutations / stacked statements
- [ ] Approval scope binds jobId or jarId; nonce anti-replay
- [ ] Output DLP patterns unchanged unless tests updated
- [ ] No secrets logged; stdout stays MCP-only

## Key types

- `governance/Governance.java`
- `security/ApprovalTokens.java`, `PolicyEngine.java`, `BearerAuthFilter.java`
- `governance/OutputControls.java`
- `util/Inputs.java`
- Tests: `GuardrailsTest`, `GovernanceTest`, `ProductionHardeningTest`
