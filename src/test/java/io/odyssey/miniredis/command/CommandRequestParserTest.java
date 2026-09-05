package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespArray;
import io.odyssey.miniredis.protocol.RespBulkString;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRequestParserTest {

    private final CommandRequestParser parser = new CommandRequestParser();

    @Test
    void shouldParsePingCommand() {
        var value = new RespArray(List.of(bulk("PING")));
        var request = parser.parse(value);

        assertThat(request.name()).isEqualTo("PING");
        assertThat(request.arguments()).isEmpty();
    }

    @Test
    void shouldNormalizeCommandName() {
        var value = new RespArray(List.of(bulk("ping")));
        var request = parser.parse(value);

        assertThat(request.name()).isEqualTo("PING");
    }

    @Test
    void shouldParseArguments() {
        var value = new RespArray(List.of(bulk("SET"), bulk("name"), bulk("Alice")));
        var request = parser.parse(value);

        assertThat(request.name()).isEqualTo("SET");
        assertThat(request.arguments()).hasSize(2);
    }

    private RespBulkString bulk(String value) {
        return new RespBulkString(value.getBytes(StandardCharsets.UTF_8));
    }
}