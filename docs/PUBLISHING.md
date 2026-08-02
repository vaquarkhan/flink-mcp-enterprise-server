# Publishing to Maven Central

## Artifact links (after publish)

| | URL |
|---|-----|
| **Maven Central (browse)** | https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server |
| **Maven Central (version 0.2.0)** | https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server/0.2.0 |
| **repo1 directory** | https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/ |
| **repo1 0.2.0** | https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/0.2.0/ |
| **MVN Repository** | https://mvnrepository.com/artifact/io.github.vaquarkhan/flink-mcp-server |
| **GitHub repo** | https://github.com/vaquarkhan/flink-mcp-enterprise-server |

Coordinates: `io.github.vaquarkhan:flink-mcp-server:0.2.0`

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>flink-mcp-server</artifactId>
  <version>0.2.0</version>
</dependency>
```

Shaded runnable classifier `all`:

```text
https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/0.2.0/flink-mcp-server-0.2.0-all.jar
```

(Only available if the `-all` jar is attached and published; otherwise use GitHub Releases or build locally.)

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
mvn "-Dgpg.skip=true" -Prelease package
```

## Deploy

```bash
mvn -Prelease clean deploy
```

The `central-publishing-maven-plugin` (server id `central`) uploads and can auto-publish when `autoPublish=true`.

## Verify after publish

Open: https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server/0.2.0

Or:

```bash
curl -sI https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/0.2.0/flink-mcp-server-0.2.0.pom
```
