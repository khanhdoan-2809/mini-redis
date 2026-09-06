package io.odyssey.miniredis.server;

import io.odyssey.miniredis.command.*;
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

    private RedisServer createServer() throws Exception {
        var registry = new CommandRegistry();
        registry.register("PING", new PingCommand());
        registry.register("ECHO", new EchoCommand());

        var dispatcher = new CommandDispatcher(registry, new CommandRequestParser());
        var server = new RedisServer(new ServerConfig("127.0.0.1", 0), dispatcher);

        server.start();

        return server;
    }

    @Test
    void shouldExecutePing() throws Exception {
        server = createServer();

        try (var client = new Socket("127.0.0.1", server.getLocalPort())) {
            var request = "*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.US_ASCII);

            client.getOutputStream().write(request);
            client.getOutputStream().flush();

            var response = client.getInputStream().readNBytes(7);

            assertThat(new String(response, StandardCharsets.US_ASCII))
                    .isEqualTo("+PONG\r\n");
        }
    }
}
