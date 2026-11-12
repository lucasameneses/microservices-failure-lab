package com.failurelab.service_b.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.failurelab.service_b.error.DownstreamException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

class ServiceAClientTest {

    private static final Duration CONNECT = Duration.ofMillis(500);
    private static final Duration READ = Duration.ofMillis(300);

    @Test
    void shouldReturnPeerBodyOnSuccess() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/cycle", exchange -> {
            byte[] body = "{\"service\":\"service-a\",\"depth\":2}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var client = clientFor("http://localhost:" + server.getAddress().getPort());

            assertThat(client.cycle(2).get("depth").asInt()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldMapConnectionFailureToBadGateway() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        var client = clientFor("http://localhost:" + closedPort);

        assertThatThrownBy(() -> client.cycle(1))
                .isInstanceOfSatisfying(DownstreamException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.code()).isEqualTo("DOWNSTREAM_ERROR");
                });
    }

    @Test
    void shouldMapReadTimeoutToGatewayTimeout() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) {
            Thread acceptor = new Thread(() -> {
                try (Socket ignored = silent.accept()) {
                    Thread.sleep(2_000);
                } catch (IOException | InterruptedException ignored) {
                    // the test only needs a connection that never answers
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
            var client = clientFor("http://localhost:" + silent.getLocalPort());

            assertThatThrownBy(() -> client.cycle(1))
                    .isInstanceOfSatisfying(DownstreamException.class, e -> {
                        assertThat(e.status()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                        assertThat(e.code()).isEqualTo("DOWNSTREAM_TIMEOUT");
                    });
        }
    }

    @Test
    void shouldMapPeerServerErrorToBadGateway() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/cycle", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            var client = clientFor("http://localhost:" + server.getAddress().getPort());

            assertThatThrownBy(() -> client.cycle(1))
                    .isInstanceOfSatisfying(DownstreamException.class,
                            e -> assertThat(e.code()).isEqualTo("DOWNSTREAM_ERROR"));
        } finally {
            server.stop(0);
        }
    }

    private static ServiceAClient clientFor(String url) {
        return new ServiceAClient(RestClient.builder(), new DownstreamProperties(url, CONNECT, READ));
    }
}
