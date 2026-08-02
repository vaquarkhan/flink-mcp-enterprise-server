package io.github.vaquarkhan.flinkmcp.client;

import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

public final class FlinkRestClient {

    public static final class BackendException extends RuntimeException {
        public BackendException(String message) {
            super(message);
        }

        public BackendException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final String baseUrl;
    private final Metrics metrics;
    private final HttpClient http;

    public FlinkRestClient(String baseUrl, Metrics metrics) {
        String u = baseUrl == null ? "" : baseUrl;
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        this.baseUrl = u;
        this.metrics = metrics;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public String get(String path) {
        return send("GET", path, null, Duration.ofSeconds(15));
    }

    public String post(String path, String jsonBody) {
        return send("POST", path, jsonBody, Duration.ofSeconds(30));
    }

    public String patch(String path, String jsonBody) {
        return send("PATCH", path, jsonBody, Duration.ofSeconds(30));
    }

    public String uploadJar(Path jar) {
        try {
            String boundary = "----FlinkMcp" + UUID.randomUUID().toString().replace("-", "");
            byte[] fileBytes = Files.readAllBytes(jar);
            String filename = jar.getFileName().toString();
            byte[] preamble = (
                    "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"jarfile\"; filename=\"" + filename + "\"\r\n"
                            + "Content-Type: application/java-archive\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8);
            byte[] epilogue = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[preamble.length + fileBytes.length + epilogue.length];
            System.arraycopy(preamble, 0, body, 0, preamble.length);
            System.arraycopy(fileBytes, 0, body, preamble.length, fileBytes.length);
            System.arraycopy(epilogue, 0, body, preamble.length + fileBytes.length, epilogue.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/jars/upload"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            metrics.addBytesOut(body.length);
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            String respBody = resp.body() == null ? "" : resp.body();
            metrics.addBytesIn(respBody.getBytes(StandardCharsets.UTF_8).length);
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BackendException("HTTP " + resp.statusCode() + ": " + truncate(respBody));
            }
            return respBody;
        } catch (BackendException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BackendException("upload failed: " + e.getMessage(), e);
        }
    }

    private String send(String method, String path, String jsonBody, Duration timeout) {
        try {
            String p = path.startsWith("/") ? path : "/" + path;
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + p))
                    .timeout(timeout);
            byte[] out = jsonBody == null ? new byte[0] : jsonBody.getBytes(StandardCharsets.UTF_8);
            metrics.addBytesOut(out.length);
            if ("GET".equals(method)) {
                b.GET();
            } else if ("POST".equals(method)) {
                b.header("Content-Type", "application/json");
                b.POST(HttpRequest.BodyPublishers.ofByteArray(out));
            } else if ("PATCH".equals(method)) {
                b.header("Content-Type", "application/json");
                b.method("PATCH", HttpRequest.BodyPublishers.ofByteArray(out));
            } else {
                throw new BackendException("unsupported method " + method);
            }
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            String respBody = resp.body() == null ? "" : resp.body();
            metrics.addBytesIn(respBody.getBytes(StandardCharsets.UTF_8).length);
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BackendException("HTTP " + resp.statusCode() + ": " + truncate(respBody));
            }
            return respBody;
        } catch (BackendException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BackendException(method + " failed: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 300 ? s : s.substring(0, 300);
    }
}
