# Publishing (Maven Central)

Author: Viquar Khan

**Preferred:** GitHub Actions — no local `settings.xml`.

Full runbook: [../docs/PUBLISHING.md](../docs/PUBLISHING.md)

## Secrets (Actions)

`CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`

(`VJRSKA` is unused.)

## Run

Actions → **publish** → Run workflow  
or `git tag v0.3.1 && git push origin v0.3.1`

## Links (after publish)

| | |
|---|---|
| **Maven Central** | https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server |
| **repo1 0.3.1** | https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/0.3.1/ |

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>flink-mcp-server</artifactId>
  <version>0.3.1</version>
</dependency>
```
