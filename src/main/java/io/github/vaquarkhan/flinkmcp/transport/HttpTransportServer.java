package io.github.vaquarkhan.flinkmcp.transport;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import java.util.EnumSet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public final class HttpTransportServer {

    private HttpTransportServer() {}

    public static void startAndBlock(int port, String pathSpec, HttpServlet mcpServlet, Filter authFilter)
            throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder holder = new ServletHolder(mcpServlet);
        holder.setAsyncSupported(true);
        context.addServlet(holder, pathSpec);
        context.addFilter(new FilterHolder(authFilter), pathSpec, EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
        server.setHandler(context);

        server.start();
        server.join();
    }
}
