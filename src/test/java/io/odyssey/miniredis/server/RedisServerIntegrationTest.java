package io.odyssey.miniredis.server;

import io.odyssey.miniredis.command.*;
import io.odyssey.miniredis.command.core.EchoCommand;
import io.odyssey.miniredis.command.core.PingCommand;
import io.odyssey.miniredis.command.key.DelCommand;
import io.odyssey.miniredis.command.key.ExistsCommand;
import io.odyssey.miniredis.command.string.GetCommand;
import io.odyssey.miniredis.command.string.SetCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
        var database = new RedisDatabase();
        registry.register("PING", new PingCommand());
        registry.register("ECHO", new EchoCommand());
        registry.register("SET", new SetCommand(database));
        registry.register("GET", new GetCommand(database));
        registry.register("DEL", new DelCommand(database));
        registry.register("EXISTS", new ExistsCommand(database));

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

    @Test
    void shouldSetAndGetValue() throws Exception {
        server = createServer();

        try (var client = new Socket("127.0.0.1", server.getLocalPort())) {
            var output = client.getOutputStream();
            var input = client.getInputStream();

            var set = "*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$5\r\nAlice\r\n";
            output.write(set.getBytes(StandardCharsets.US_ASCII));
            output.flush();

            assertThat(new String(input.readNBytes(5), StandardCharsets.US_ASCII)).isEqualTo("+OK\r\n");

            var get = "*2\r\n$3\r\nGET\r\n$4\r\nname\r\n";
            output.write(get.getBytes(StandardCharsets.US_ASCII));
            output.flush();

            assertThat(new String(input.readNBytes(11), StandardCharsets.US_ASCII)).isEqualTo("$5\r\nAlice\r\n");
        }
    }

    @Test
    void shouldShareDatabaseBetweenClients() throws Exception {
        server = createServer();
        try (
                var clientOne = new Socket("127.0.0.1", server.getLocalPort());
                var clientTwo = new Socket("127.0.0.1", server.getLocalPort())
        ) {
            var set = "*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$5\r\nAlice\r\n";

            clientOne.getOutputStream().write(set.getBytes(StandardCharsets.US_ASCII));
            clientOne.getOutputStream().flush();

            clientOne.getInputStream().readNBytes(5);

            var get = "*2\r\n$3\r\nGET\r\n$4\r\nname\r\n";

            clientTwo.getOutputStream().write(get.getBytes(StandardCharsets.US_ASCII));
            clientTwo.getOutputStream().flush();

            var response = clientTwo.getInputStream().readNBytes(11);

            assertThat(new String(response, StandardCharsets.US_ASCII)).isEqualTo("$5\r\nAlice\r\n");
        }
    }
}
