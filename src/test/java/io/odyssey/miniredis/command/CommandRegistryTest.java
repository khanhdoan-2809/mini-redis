package io.odyssey.miniredis.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandRegistryTest {

    @Test
    void shouldFindRegisteredCommand() {
        var registry = new CommandRegistry();
        var command = new PingCommand();
        registry.register("PING", command);
        assertThat(registry.find("PING")).contains(command);
    }

    @Test
    void shouldBeCaseInsensitive() {
        var registry = new CommandRegistry();
        var command = new PingCommand();
        registry.register("PING", command);
        assertThat(registry.find("ping")).contains(command);
    }
}
