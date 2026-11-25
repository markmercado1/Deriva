package com.deriva.bridge.http;

import com.deriva.bridge.EmBrowserStateStore;
import com.deriva.bridge.diagnostics.EmBridgeReportLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class EmLocalBridgeServerTest {
    private static final String TOKEN = "a".repeat(64);
    private static final String ORIGIN = "chrome-extension://" + "a".repeat(32);
    private EmLocalBridgeServer server;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    @BeforeEach void start() throws Exception {
        server = new EmLocalBridgeServer(0, TOKEN, "a".repeat(32),
                new EmBrowserStateStore(Clock.systemUTC(), System::nanoTime), System::nanoTime);
        server.start();
    }
    @AfterEach void stop() { server.close(); }
    private HttpResponse<String> request(String path, String body, String token, String origin) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.port()+path))
                .timeout(Duration.ofSeconds(3));
        if (token != null) builder.header("X-Deriva-Token",token);
        if (origin != null) builder.header("Origin",origin);
        if (body == null) builder.GET();
        else builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void occupiedPortFailsInsteadOfSilentlySkippingTheBridge() {
        assertThrows(IOException.class, () -> new EmLocalBridgeServer(server.port(), TOKEN, "a".repeat(32),
                new EmBrowserStateStore(Clock.systemUTC(), System::nanoTime), System::nanoTime));
    }

    @Test void pingAndSecurityBoundaries() throws Exception {
        assertEquals(200,request("/v1/ping",null,null,null).statusCode());
        assertEquals(401,request("/v1/tab","{}",null,ORIGIN).statusCode());
        assertEquals(401,request("/v1/tab","{}","wrong",ORIGIN).statusCode());
        assertEquals(403,request("/v1/tab","{}",TOKEN,"https://evil.example").statusCode());
        assertEquals(403,request("/v1/tab","{}",TOKEN,null).statusCode());
        assertEquals(403,request("/v1/tab","{}",TOKEN,"chrome-extension://"+"b".repeat(32)).statusCode());
        assertEquals(413,request("/v1/tab","x".repeat(2049),TOKEN,ORIGIN).statusCode());
        assertEquals(404,request("/v1/tab/other","{}",TOKEN,ORIGIN).statusCode());
        assertEquals(405,request("/v1/tab",null,TOKEN,ORIGIN).statusCode());
    }
    @Test void hostValidationUsesActualHttpTransport() throws Exception {
        try(var socket = new Socket("127.0.0.1",server.port())) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write("GET /v1/ping HTTP/1.1\r\nHost: evil.example\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            assertTrue(new String(socket.getInputStream().readAllBytes(),StandardCharsets.US_ASCII).startsWith("HTTP/1.1 403"));
        }
    }
    @Test void sessionReportStatusAndReplay() throws Exception {
        var response=request("/v1/session","{\"browser\":\"chrome\"}",TOKEN,ORIGIN);
        assertEquals(200,response.statusCode());
        String session=(String)EmFlatJson.parse(response.body()).get("session");
        String body="{\"browser\":\"chrome\",\"session\":\""+session+"\",\"seq\":1,\"state\":\"NORMAL\",\"host\":\"docs.google.com\"}";
        assertEquals(204,request("/v1/tab",body,TOKEN,ORIGIN).statusCode());
        var status=request("/v1/status",null,TOKEN,ORIGIN);
        assertTrue(status.body().contains("\"host\":\"docs.google.com\""));
        assertTrue(status.body().contains("\"captureEnabled\":false"));
        assertTrue(status.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertEquals(409,request("/v1/tab",body,TOKEN,ORIGIN).statusCode());
        assertEquals(200,request("/v1/session","{\"browser\":\"chrome\"}",TOKEN,ORIGIN).statusCode());
        assertEquals(409,request("/v1/tab",body.replace("\"seq\":1","\"seq\":2"),TOKEN,ORIGIN).statusCode());
    }
    @Test void diagnosticLogRecordsNoHost() throws Exception {
        Path log = Files.createTempFile("deriva-reports-", ".log");
        try (EmBridgeReportLog reportLog = new EmBridgeReportLog(log, Clock.systemUTC())) {
            server.close();
            server = new EmLocalBridgeServer(0, TOKEN, "a".repeat(32),
                    new EmBrowserStateStore(Clock.systemUTC(), System::nanoTime), System::nanoTime, reportLog);
            server.start();

            var response = request("/v1/session", "{\"browser\":\"chrome\"}", TOKEN, ORIGIN);
            String session = (String) EmFlatJson.parse(response.body()).get("session");
            String normal = "{\"browser\":\"chrome\",\"session\":\"" + session
                    + "\",\"seq\":1,\"state\":\"NORMAL\",\"host\":\"docs.google.com\"}";
            assertEquals(204, request("/v1/tab", normal, TOKEN, ORIGIN).statusCode());
            String unknown = "{\"browser\":\"chrome\",\"session\":\"" + session
                    + "\",\"seq\":2,\"state\":\"UNKNOWN\",\"host\":null}";
            assertEquals(204, request("/v1/tab", unknown, TOKEN, ORIGIN).statusCode());
        }

        String content = Files.readString(log);
        assertTrue(content.contains("NORMAL\tyes"));
        assertTrue(content.contains("UNKNOWN\tno"));
        assertFalse(content.contains("docs.google.com"));
    }

    @Test void malformedAndUnexpectedFieldsAreRejected() throws Exception {
        for(String body:new String[]{"{", "{}", "{\"browser\":\"chrome\",\"browser\":\"chrome\"}",
                "{\"browser\":\"chrome\"}junk", "{\"browser\":\"chrome\",\"host\":\"hidden.example\"}",
                "{\"browser\":[\"chrome\"]}", "{\"browser\":true}"}) {
            assertEquals(400,request("/v1/session",body,TOKEN,ORIGIN).statusCode());
        }
    }
    @Test void rateLimitAppliesWithoutUnboundedClientState() throws Exception {
        // Fixed monotonic time makes this deterministic without sleeps.
        server.close();
        server=new EmLocalBridgeServer(0,TOKEN,"a".repeat(32),new EmBrowserStateStore(Clock.systemUTC(),()->0L),()->0L);
        server.start();
        for(int i=0;i<20;i++) assertEquals(200,request("/v1/ping",null,null,null).statusCode());
        assertEquals(429,request("/v1/ping",null,null,null).statusCode());
    }
    @Test void strictJsonGrammarSupportsEscapesButRejectsAmbiguousNumbers() {
        assertEquals("chrome",EmFlatJson.parse(" {\"browser\":\"chr\\u006fme\"} ").get("browser"));
        for(String input:new String[]{"{\"n\":01}","{\"n\":1.0}","{\"n\":1e2}","{\"n\":-1}",
                "{\"n\":9223372036854775808}","{\"n\":1,}","{\"n\":null,\"n\":2}","{\"x\":\"\\uD800\"}"}) {
            assertThrows(IllegalArgumentException.class,()->EmFlatJson.parse(input));
        }
    }
}
