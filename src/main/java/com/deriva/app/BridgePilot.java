package com.deriva.app;

import com.deriva.bridge.BrowserStateStore;
import com.deriva.bridge.diagnostics.BridgeReportLog;
import com.deriva.bridge.http.LocalBridgeServer;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;

/** Explicit opt-in diagnostic entry point; no sensor or database is connected. */
public final class BridgePilot {
    private BridgePilot() {}
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: BridgePilot <extension-id> [puerto]. Token: DERIVA_BRIDGE_TOKEN.");
            return;
        }
        String token = System.getenv("DERIVA_BRIDGE_TOKEN");
        if (token == null || !token.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Configurar DERIVA_BRIDGE_TOKEN (64 caracteres hex)");
        int port = args.length == 2 ? Integer.parseInt(args[1]) : 47821;
        if (port < 1024 || port > 65535) throw new IllegalArgumentException("Invalid port");
        var state = new BrowserStateStore(Clock.systemUTC(), System::nanoTime);
        String logPath = System.getenv("DERIVA_BRIDGE_REPORT_LOG");
        BridgeReportLog reportLog = logPath == null || logPath.isBlank()
                ? null : new BridgeReportLog(Path.of(logPath), Clock.systemUTC());
        try (reportLog; var bridge = new LocalBridgeServer(port, token, args[0], state, System::nanoTime, reportLog)) {
            Runtime.getRuntime().addShutdownHook(new Thread(bridge::close));
            bridge.start();
            System.out.println("Deriva bridge piloto en 127.0.0.1:" + port + ". Captura deshabilitada.");
            if (logPath != null && !logPath.isBlank())
                System.out.println("Registro de diagnostico en " + logPath + " (sin hosts).");
            new CountDownLatch(1).await();
        }
    }
}
