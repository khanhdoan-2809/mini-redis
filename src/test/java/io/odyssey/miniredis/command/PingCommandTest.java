package io.odyssey.miniredis.command;

import io.odyssey.miniredis.command.core.PingCommand;
import io.odyssey.miniredis.protocol.RespBulkString;
import io.odyssey.miniredis.protocol.RespSimpleString;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PingCommandTest {

    private final PingCommand command = new PingCommand();

    @Test
    void shouldReturnPongWithoutArguments() {
        var request = new CommandRequest("PING", List.of());
        var response = command.execute(request);

        assertThat(response).isEqualTo(new RespSimpleString("PONG"));
    }

    @Test
    void shouldReturnProvidedMessage() {
        var message = new RespBulkString("hello".getBytes(StandardCharsets.UTF_8));
        var request = new CommandRequest("PING", List.of(message));

        var response = command.execute(request);

        assertThat(response).isEqualTo(message);
    }
}