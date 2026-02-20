package com.swingmcp.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Entry point for the SwingMCP embedded HTTP server.
 *
 * <p>Starts a lightweight HTTP server using the JDK built-in
 * {@link HttpServer} that exposes the Swing component tree,
 * interaction capabilities, screenshot capture, and contrast
 * checking over a REST-like API on localhost.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // After Swing UI is initialized:
 * SwingMcpServer.start(9222);
 * }</pre>
 */
public class SwingMcpServer {

    private static HttpServer server;

    /**
     * Start the MCP HTTP server on the specified port.
     *
     * @param port the port to listen on (e.g., 9222)
     */
    public static void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            HttpApiHandler handler = new HttpApiHandler();
            server.createContext("/", handler);
            server.setExecutor(null); // default executor
            server.start();
            System.out.println("SwingMCP Server started on http://localhost:" + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start SwingMCP server on port " + port, e);
        }
    }

    /**
     * Stop the MCP HTTP server.
     */
    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("SwingMCP Server stopped");
        }
    }
}
