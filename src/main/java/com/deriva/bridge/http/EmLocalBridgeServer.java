package com.deriva.bridge.http;

import com.deriva.bridge.EmBrowserReport;
import com.deriva.bridge.EmBrowserStateStore;
import com.deriva.bridge.diagnostics.EmBridgeReportLog;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class EmLocalBridgeServer implements AutoCloseable {
    private final HttpServer server;
    private final ThreadPoolExecutor executor;
    private final byte[] token;
    private final String origin;
    private final EmBrowserStateStore store;
    private final LongSupplier nanoTime;
    private final EmBridgeReportLog reportLog;
    private long windowStart;
    private int requests;

    public EmLocalBridgeServer(int port, String token, String extensionId,
                             EmBrowserStateStore store, LongSupplier nanoTime) throws IOException {
        this(port, token, extensionId, store, nanoTime, null);
    }

    public EmLocalBridgeServer(int port, String token, String extensionId,
                             EmBrowserStateStore store, LongSupplier nanoTime,
                             EmBridgeReportLog reportLog) throws IOException {
        if (!token.matches("[0-9a-f]{64}") || !extensionId.matches("[a-p]{32}"))
            throw new IllegalArgumentException("Invalid bridge configuration");
        this.token = token.getBytes(StandardCharsets.US_ASCII);
        this.origin = "chrome-extension://" + extensionId;
        this.store = store;
        this.nanoTime = nanoTime;
        this.reportLog = reportLog;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 16);
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());
        server.setExecutor(executor);
        server.createContext("/", this::handle);
    }

    public int port() { return server.getAddress().getPort(); }
    public void start() { server.start(); }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }

    private synchronized boolean rateAllowed() {
        long now = nanoTime.getAsLong();
        if (now - windowStart >= 1_000_000_000L || now < windowStart) {
            windowStart = now; requests = 0;
        }
        return ++requests <= 20;
    }

    private static String singleHeader(HttpExchange exchange, String name) {
        var values = exchange.getRequestHeaders().get(name);
        return values != null && values.size() == 1 ? values.getFirst() : "";
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!singleHeader(exchange, "Host").equals("127.0.0.1:" + port())) { send(exchange, 403, ""); return; }
            if (!rateAllowed()) { send(exchange, 429, ""); return; }
            String path = exchange.getRequestURI().getRawPath();
            if (exchange.getRequestURI().getRawQuery() != null) { send(exchange, 400, ""); return; }
            if (path.equals("/v1/ping") && exchange.getRequestMethod().equals("GET")) {
                send(exchange, 200, "{\"app\":\"deriva\",\"v\":1}"); return;
            }
            byte[] supplied = singleHeader(exchange, "X-Deriva-Token").getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(token, supplied)) { send(exchange, 401, ""); return; }
            if (!singleHeader(exchange, "Origin").equals(origin)) { send(exchange, 403, ""); return; }
            if (path.equals("/v1/status") && exchange.getRequestMethod().equals("GET")) {
                var snapshot = store.snapshot();
                String host = snapshot.host() == null ? "null" : "\"" + snapshot.host() + "\"";
                send(exchange, 200, "{\"captureEnabled\":false,\"state\":\"" + snapshot.state()
                        + "\",\"host\":" + host + ",\"sequence\":" + snapshot.sequence()
                        + ",\"accepted\":" + snapshot.accepted() + "}"); return;
            }
            if (!path.equals("/v1/session") && !path.equals("/v1/tab")) { send(exchange, 404, ""); return; }
            if (!exchange.getRequestMethod().equals("POST")) { send(exchange, 405, ""); return; }
            String contentType = singleHeader(exchange, "Content-Type").toLowerCase(java.util.Locale.ROOT);
            if (!contentType.equals("application/json") && !contentType.equals("application/json; charset=utf-8")) {
                send(exchange, 400, ""); return;
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(2049);
            if (bytes.length > 2048) { send(exchange, 413, ""); return; }
            try {
                String body = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
                Map<String, Object> fields = EmFlatJson.parse(body);
                if (!"chrome".equals(fields.get("browser"))) throw new IllegalArgumentException();
                if (path.equals("/v1/session")) {
                    if (!fields.keySet().equals(Set.of("browser"))) throw new IllegalArgumentException();
                    send(exchange, 200, "{\"session\":\"" + store.beginSession() + "\"}"); return;
                }
                if (!fields.keySet().equals(Set.of("browser", "state", "host", "seq", "session")))
                    throw new IllegalArgumentException();
                if (!(fields.get("seq") instanceof Long seq) || !(fields.get("session") instanceof String session)
                        || !session.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException();
                EmBrowserReport report = new EmBrowserReport(EmBrowserReport.State.valueOf((String) fields.get("state")),
                        (String) fields.get("host"), seq);
                if (!store.accept(session, report)) { send(exchange, 409, ""); return; }
                if (reportLog != null) {
                    try {
                        reportLog.append(report.sequence(), report.state().name(), report.host() != null);
                    } catch (RuntimeException logFailure) {
                        System.err.println("Bridge report log write failed: " + logFailure.getMessage());
                    }
                }
                send(exchange, 204, "");
            } catch (IllegalArgumentException | ClassCastException | NullPointerException
                     | java.nio.charset.CharacterCodingException invalid) {
                send(exchange, 400, "");
            }
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (body.isEmpty()) { exchange.sendResponseHeaders(status, -1); return; }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
