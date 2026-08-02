package io.github.vaquarkhan.flinkmcp.transport;

import io.github.vaquarkhan.flinkmcp.observability.Metrics;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded Jetty for streamable HTTP MCP plus ops endpoints:
 * {@code /healthz}, {@code /readyz}, {@code /metrics} (Prometheus).
 */
/**
 * @author Viquar Khan
 */
public final class HttpTransportServer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpTransportServer.class);

    private HttpTransportServer() {}

    public static Server start(
            String host,
            int port,
            String pathSpec,
            HttpServlet mcpServlet,
            Filter authFilter,
            Metrics metrics,
            BooleanSupplier readyCheck) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);
        connector.setIdleTimeout(60_000);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        ServletHolder mcpHolder = new ServletHolder(mcpServlet);
        mcpHolder.setAsyncSupported(true);
        context.addServlet(mcpHolder, pathSpec);
        context.addFilter(new FilterHolder(authFilter), pathSpec, EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        context.addServlet(new ServletHolder(new OpsServlet(metrics, readyCheck)), "/healthz");
        context.addServlet(new ServletHolder(new OpsServlet(metrics, readyCheck)), "/readyz");
        context.addServlet(new ServletHolder(new OpsServlet(metrics, readyCheck)), "/metrics");

        server.setHandler(context);
        server.setStopAtShutdown(true);
        server.setStopTimeout(10_000);
        server.start();
        LOG.info("http transport listening on http://{}:{} (mcp={}, healthz=/healthz, metrics=/metrics)",
                host, port, pathSpec);
        return server;
    }

    public static void startAndBlock(
            int port, String pathSpec, HttpServlet mcpServlet, Filter authFilter) throws Exception {
        startAndBlock("127.0.0.1", port, pathSpec, mcpServlet, authFilter, new Metrics(), () -> true);
    }

    public static void startAndBlock(
            String host,
            int port,
            String pathSpec,
            HttpServlet mcpServlet,
            Filter authFilter,
            Metrics metrics,
            BooleanSupplier readyCheck) throws Exception {
        Server server = start(host, port, pathSpec, mcpServlet, authFilter, metrics, readyCheck);
        server.join();
    }

    public static void stopQuietly(Server server, long timeoutMs) {
        if (server == null) {
            return;
        }
        try {
            server.setStopTimeout(timeoutMs);
            server.stop();
        } catch (Exception e) {
            LOG.warn("jetty stop error: {}", e.getMessage());
        }
    }

    private static final class OpsServlet extends HttpServlet {
        private final Metrics metrics;
        private final BooleanSupplier readyCheck;
        private final AtomicBoolean live = new AtomicBoolean(true);

        OpsServlet(Metrics metrics, BooleanSupplier readyCheck) {
            this.metrics = metrics;
            this.readyCheck = readyCheck;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String path = req.getServletPath();
            if ("/healthz".equals(path)) {
                write(resp, 200, "application/json", "{\"status\":\"UP\"}");
                return;
            }
            if ("/readyz".equals(path)) {
                boolean ready = live.get() && readyCheck.getAsBoolean();
                if (ready) {
                    write(resp, 200, "application/json", "{\"status\":\"READY\"}");
                } else {
                    write(resp, 503, "application/json", "{\"status\":\"NOT_READY\"}");
                }
                return;
            }
            if ("/metrics".equals(path)) {
                write(resp, 200, "text/plain; version=0.0.4; charset=utf-8", metrics.toPrometheus());
                return;
            }
            resp.setStatus(404);
        }

        private static void write(HttpServletResponse resp, int code, String type, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            resp.setStatus(code);
            resp.setContentType(type);
            resp.setContentLength(bytes.length);
            resp.getOutputStream().write(bytes);
        }
    }
}
