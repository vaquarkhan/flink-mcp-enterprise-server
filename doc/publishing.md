# Publishing (Maven Central)

Author: Viquar Khan

## Links
| | |
|---|---|
| **Maven Central** | https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server |
| **Version 0.2.0** | https://central.sonatype.com/artifact/io.github.vaquarkhan/flink-mcp-server/0.2.0 |
| **repo1** | https://repo1.maven.org/maven2/io/github/vaquarkhan/flink-mcp-server/ |
| **MVN Repository** | https://mvnrepository.com/artifact/io.github.vaquarkhan/flink-mcp-server |

Full publish runbook: [../docs/PUBLISHING.md](../docs/PUBLISHING.md).

## Commands
```bash
mvn clean package
# target/flink-mcp-server-0.2.0.jar
# target/flink-mcp-server-0.2.0-all.jar
mvn "-Dgpg.skip=true" -Prelease package -DskipTests
mvn -Prelease clean deploy   # needs Central credentials + GPG
```

## Coordinates
```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>flink-mcp-server</artifactId>
  <version>0.2.0</version>
</dependency>
```
