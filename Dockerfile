# Author: Viquar Khan
# Multi-stage build for flink-mcp-server
FROM maven:3.9.15-eclipse-temurin-26 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
COPY LICENSE README.md ./
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/flink-mcp-server-*-all.jar /app/flink-mcp-server-all.jar
ENV MCP_FLINK_TRANSPORT=http \
    MCP_FLINK_HTTP_HOST=0.0.0.0 \
    MCP_FLINK_HTTP_PORT=8090 \
    MCP_FLINK_LOG_LEVEL=INFO
EXPOSE 8090
USER nobody
ENTRYPOINT ["java","-jar","/app/flink-mcp-server-all.jar"]
