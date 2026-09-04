package io.odyssey.miniredis.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

public class RedisServerIntegrationTest {

    private RedisServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void shouldEchoBytesBackToClient() throws IOException {
        server = new RedisServer(new ServerConfig( "127.0.0.1", 0));
        server.start();

        var port = server.getLocalPort();

        try (var client = new Socket("127.0.0.1", port)) {
            var output = client.getOutputStream();
            var input = client.getInputStream();
            var request = "hello".getBytes(StandardCharsets.UTF_8);
            output.write(request);
            output.flush();

            var response = input.readNBytes(request.length);
            assertThat(response).isEqualTo(request);
        }
    }

    @Test
    void shouldServerMultipleClients() throws IOException {
        server = new RedisServer(new ServerConfig( "127.0.0.1", 0));
        server.start();

        var port = server.getLocalPort();
        try(var clientOne = new Socket("127.0.0.1", port);
            var clientTwo = new Socket("127.0.0.1", port)) {
            clientOne.getOutputStream().write("one".getBytes(StandardCharsets.UTF_8));
            clientOne.getOutputStream().flush();

            clientTwo.getOutputStream().write("two".getBytes(StandardCharsets.UTF_8));
            clientTwo.getOutputStream().flush();

            var first = clientOne.getInputStream().readNBytes(3);
            var second = clientTwo.getInputStream().readNBytes(3);
            assertThat(new String(first, StandardCharsets.UTF_8)).isEqualTo("one");
            assertThat(new String(second, StandardCharsets.UTF_8)).isEqualTo("two");
        }
    }

    @Test
    void shouldAcceptRespRequest() throws Exception {
        server = new RedisServer(new ServerConfig("127.0.0.1", 0));
        server.start();

        try (var client = new Socket("127.0.0.1", server.getLocalPort())) {
            var request = "*1\r\n$4\r\nPING\r\n"
                            .getBytes(StandardCharsets.US_ASCII);

            client.getOutputStream().write(request);

            client.getOutputStream().flush();

            var response = client.getInputStream().readNBytes(5);

            assertThat(new String(response, StandardCharsets.US_ASCII))
                    .isEqualTo("+OK\r\n");
        }
    }

    @Test
    void shouldProcessMultipleRespRequests() throws Exception {
        server = new RedisServer(new ServerConfig("127.0.0.1", 0));

        server.start();
        try (var client = new Socket("127.0.0.1", server.getLocalPort())) {
            var requests =(
                            "*1\r\n$4\r\nPING\r\n"
                                    + "*1\r\n$4\r\nPING\r\n"
                    ).getBytes(StandardCharsets.US_ASCII);

            client.getOutputStream().write(requests);

            client.getOutputStream().flush();

            byte[] response = client.getInputStream() .readNBytes(10);

            assertThat(new String(response, StandardCharsets.US_ASCII))
                    .isEqualTo("+OK\r\n+OK\r\n");
        }
    }
}
