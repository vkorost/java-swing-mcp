package com.swingmcp.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.swingmcp.server.model.ActionRequest;
import com.swingmcp.server.model.ActionResult;
import com.swingmcp.server.model.ComponentNode;

import javax.swing.*;
import java.io.*;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HTTP request handler for the SwingMCP server API.
 *
 * <p>Routes requests to the appropriate handler method based on the
 * request path. All Swing component access is performed on the EDT
 * via {@link #runOnEDT(Callable)}.</p>
 */
public class HttpApiHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final ComponentTreeWalker treeWalker = new ComponentTreeWalker();
    private final ComponentStateExtractor stateExtractor = new ComponentStateExtractor();
    private final InteractionExecutor interactionExecutor = new InteractionExecutor();
    private final ScreenshotCapture screenshotCapture = new ScreenshotCapture();
    private final ContrastChecker contrastChecker = new ContrastChecker();
    private final UserActionRecorder actionRecorder = new UserActionRecorder();
    private final long startTime = System.currentTimeMillis();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if ("GET".equals(method) && "/tree".equals(path)) {
                handleTree(exchange);
            } else if ("GET".equals(method) && path.startsWith("/component/")) {
                handleComponent(exchange);
            } else if ("POST".equals(method) && "/action".equals(path)) {
                handleAction(exchange);
            } else if ("GET".equals(method) && "/screenshot".equals(path)) {
                handleScreenshot(exchange);
            } else if ("GET".equals(method) && "/contrast".equals(path)) {
                handleContrast(exchange);
            } else if ("GET".equals(method) && "/health".equals(path)) {
                handleHealth(exchange);
            } else if ("POST".equals(method) && "/record/start".equals(path)) {
                handleRecordStart(exchange);
            } else if ("POST".equals(method) && "/record/stop".equals(path)) {
                handleRecordStop(exchange);
            } else if ("GET".equals(method) && "/record/status".equals(path)) {
                handleRecordStatus(exchange);
            } else {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "Not found: " + path);
                sendJson(exchange, 404, err);
            }
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            String msg = e.getClass().getName() + ": " + e.getMessage();
            if (e.getCause() != null) {
                msg += " caused by " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
            }
            // Print stack trace to stderr for debugging
            e.printStackTrace();
            err.put("error", msg);
            sendJson(exchange, 500, err);
        }
    }

    private void handleTree(HttpExchange exchange) throws Exception {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());

        final Integer depth = params.containsKey("depth") ? Integer.parseInt(params.get("depth")) : null;
        final Set<String> types;
        if (params.containsKey("types")) {
            types = new HashSet<>(Arrays.asList(params.get("types").split(",")));
        } else {
            types = null;
        }
        // Default to interactable=true per spec
        final boolean interactable = !params.containsKey("interactable")
                || Boolean.parseBoolean(params.get("interactable"));

        List<ComponentNode> nodes = runOnEDT(new Callable<List<ComponentNode>>() {
            @Override
            public List<ComponentNode> call() {
                return treeWalker.walk(depth, types, interactable);
            }
        });
        int totalCount = runOnEDT(new Callable<Integer>() {
            @Override
            public Integer call() {
                return treeWalker.getTotalComponentCount();
            }
        });

        if (nodes.size() >= 200) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("components", nodes);
            response.put("truncated", true);
            response.put("totalComponents", totalCount);
            sendJson(exchange, 200, response);
        } else {
            sendJson(exchange, 200, nodes);
        }
    }

    private void handleComponent(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        final String nameOrId = URLDecoder.decode(path.substring("/component/".length()), "UTF-8");

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        final String rows = params.get("rows");

        Map<String, Object> state = runOnEDT(new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() {
                java.awt.Component comp = ComponentTreeWalker.findComponent(nameOrId);
                if (comp == null) {
                    return null;
                }
                return stateExtractor.extractState(comp, rows);
            }
        });

        if (state == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Component not found: " + nameOrId);
            sendJson(exchange, 404, err);
        } else {
            sendJson(exchange, 200, state);
        }
    }

    private void handleAction(HttpExchange exchange) throws Exception {
        final String body = readFully(exchange.getRequestBody());
        final ActionRequest request = gson.fromJson(body, ActionRequest.class);

        ActionResult result = runOnEDT(new Callable<ActionResult>() {
            @Override
            public ActionResult call() {
                return interactionExecutor.execute(request);
            }
        });
        sendJson(exchange, result.isSuccess() ? 200 : 400, result);
    }

    private void handleScreenshot(HttpExchange exchange) throws Exception {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        final String component = params.get("component");
        String format = params.containsKey("format") ? params.get("format") : "base64";

        ScreenshotCapture.CaptureResult capture = runOnEDT(new Callable<ScreenshotCapture.CaptureResult>() {
            @Override
            public ScreenshotCapture.CaptureResult call() throws Exception {
                return screenshotCapture.capture(component);
            }
        });

        // Save screenshot file to disk with timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String filename = "swing-mcp-screenshot-" + timestamp + ".png";
        File screenshotFile = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
            fos.write(capture.getPngBytes());
        }
        System.out.println("Screenshot saved: " + screenshotFile.getAbsolutePath());

        if ("raw".equals(format)) {
            byte[] pngBytes = capture.getPngBytes();
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, pngBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(pngBytes);
            }
        } else {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("image", "data:image/png;base64," + capture.toBase64());
            response.put("width", capture.getWidth());
            response.put("height", capture.getHeight());
            sendJson(exchange, 200, response);
        }
    }

    private void handleContrast(HttpExchange exchange) throws Exception {
        Map<String, Object> result = runOnEDT(new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() {
                return contrastChecker.checkAll();
            }
        });
        sendJson(exchange, 200, result);
    }

    private void handleHealth(HttpExchange exchange) throws Exception {
        int componentCount = runOnEDT(new Callable<Integer>() {
            @Override
            public Integer call() {
                return treeWalker.getTotalComponentCount();
            }
        });
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "ok");
        health.put("components", componentCount);
        health.put("uptime", uptimeSeconds);
        sendJson(exchange, 200, health);
    }

    private void handleRecordStart(HttpExchange exchange) throws Exception {
        actionRecorder.startRecording();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "recording");
        response.put("message", "Recording started");
        sendJson(exchange, 200, response);
    }

    private void handleRecordStop(HttpExchange exchange) throws Exception {
        String filename = actionRecorder.stopRecording();
        Map<String, Object> response = new LinkedHashMap<>();
        if (filename != null) {
            response.put("status", "stopped");
            response.put("file", filename);
            response.put("actions", actionRecorder.getActionCount());
        } else {
            response.put("status", "not_recording");
            response.put("message", "No active recording to stop");
        }
        sendJson(exchange, 200, response);
    }

    private void handleRecordStatus(HttpExchange exchange) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("recording", actionRecorder.isRecording());
        response.put("actions", actionRecorder.getActionCount());
        sendJson(exchange, 200, response);
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        String json = gson.toJson(body);
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        try {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(URLDecoder.decode(kv[0], "UTF-8"),
                               URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return params;
    }

    /**
     * Read the full content of an InputStream as a UTF-8 string.
     *
     * @param is the input stream
     * @return the content as a string
     * @throws IOException if reading fails
     */
    private static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toString("UTF-8");
    }

    /**
     * Execute a callable on the EDT and return the result.
     * If already on the EDT, executes directly.
     *
     * @param callable the work to execute
     * @param <T>      the return type
     * @return the result
     * @throws Exception if the callable throws or EDT invocation fails
     */
    public static <T> T runOnEDT(Callable<T> callable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return callable.call();
        }
        final AtomicReference<T> result = new AtomicReference<>();
        final AtomicReference<Exception> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    result.set(callable.call());
                } catch (Exception e) {
                    error.set(e);
                }
            }
        });
        if (error.get() != null) throw error.get();
        return result.get();
    }
}
