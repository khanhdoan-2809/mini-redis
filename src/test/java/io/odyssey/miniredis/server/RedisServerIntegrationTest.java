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
