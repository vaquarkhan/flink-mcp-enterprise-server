# Publishing to Maven Central

## Prerequisites

1. [Sonatype Central Portal](https://central.sonatype.com/) account linked to `io.github.vaquarkhan`
2. GPG key pair for signing artifacts
3. `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN</password>
    </server>
  </servers>
</settings>
```

## Local release artifacts

```bash
mvn clean package
# produces:
#   target/flink-mcp-server-0.2.0.jar
#   target/flink-mcp-server-0.2.0-all.jar
```

With sources + javadoc (no deploy):

```bash
mvn -Prelease package -Dgpg.skip=true
```

## Deploy

```bash
mvn -Prelease clean deploy
```

The `central-publishing-maven-plugin` (server id `central`) uploads and can auto-publish when `autoPublish=true`.

## Verify

```bash
curl -s https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/0.2.0/
```
