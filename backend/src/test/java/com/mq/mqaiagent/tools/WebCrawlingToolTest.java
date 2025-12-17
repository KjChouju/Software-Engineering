package com.mq.mqaiagent.tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class WebCrawlingToolTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html><body>Hello Tool</body></html>";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testCrawl_success_returnsHtml() {
        WebCrawlingTool tool = new WebCrawlingTool();
        String html = tool.crawl("http://localhost:" + port + "/");
        assertNotNull(html);
        assertTrue(html.contains("Hello Tool"));
    }

    @Test
    void testCrawl_error_returnsMessage() {
        WebCrawlingTool tool = new WebCrawlingTool();
        String html = tool.crawl("http://localhost:1/");
        assertNotNull(html);
        assertTrue(html.startsWith("Error Crawl web pages: "));
    }
}

