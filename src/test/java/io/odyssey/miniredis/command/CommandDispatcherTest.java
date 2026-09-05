package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespArray;
import io.odyssey.miniredis.protocol.RespBulkString;
import io.odyssey.miniredis.protocol.RespError;
import io.odyssey.miniredis.protocol.RespSimpleString;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandDispatcherTest {

    @Test
    void shouldDispatchPingCommand() {
        var registry = new CommandRegistry();
        registry.register("PING", new PingCommand());

        var dispatcher = new CommandDispatcher(registry, new CommandRequestParser());
        var request = new RespArray(List.of(bulk("PING")));

        var response = dispatcher.dispatch(request);

        assertThat(response).isEqualTo(new RespSimpleString("PONG"));
    }

    @Test
    void shouldReturnErrorForUnknownCommand() {
        var registry = new CommandRegistry();
        var dispatcher = new CommandDispatcher(registry, new CommandRequestParser());
        var request = new RespArray(List.of(bulk("HELLO-WORLD")));

        var response = dispatcher.dispatch(request);

        assertThat(response).isInstanceOf(RespError.class);
    }

    private RespBulkString bulk(String value) {
        return new RespBulkString(value.getBytes(StandardCharsets.UTF_8));
    }
}