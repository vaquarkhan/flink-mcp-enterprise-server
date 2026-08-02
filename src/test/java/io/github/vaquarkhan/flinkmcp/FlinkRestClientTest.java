package io.github.vaquarkhan.flinkmcp;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.vaquarkhan.flinkmcp.client.FlinkRestClient;
import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FlinkRestClientTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jobs/overview", ex -> {
            byte[] body = "{\"jobs\":[]}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/jobs/bad", ex -> {
            byte[] body = "missing".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/jars/x/run", ex -> {
            byte[] body = "{\"jobid\":\"abc\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void get_returnsBodyAndRecordsBandwidth() {
        Metrics metrics = new Metrics();
        FlinkRestClient client = new FlinkRestClient(baseUrl, metrics);
        String body = client.get("/jobs/overview");
        assertTrue(body.contains("jobs"));
        assertTrue(metrics.getBytesIn() > 0);
    }

    @Test
    void get_non2xxThrowsBackendException() {
        FlinkRestClient client = new FlinkRestClient(baseUrl, new Metrics());
        assertThrows(FlinkRestClient.BackendException.class, () -> client.get("/jobs/bad"));
    }

    @Test
    void post_returnsBody() {
        FlinkRestClient client = new FlinkRestClient(baseUrl, new Metrics());
        String body = client.post("/jars/x/run", "{}");
        assertTrue(body.contains("abc"));
    }

    @Test
    void connectionRefused_throwsBackendException() {
        FlinkRestClient client = new FlinkRestClient("http://127.0.0.1:1", new Metrics());
        assertThrows(FlinkRestClient.BackendException.class, () -> client.get("/jobs/overview"));
    }
}
