package io.odyssey.miniredis.command;

import java.util.*;

public class CommandRegistry {

    private final Map<String, RedisCommand> commands = new HashMap<>();

    public void register(String name, RedisCommand command) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(command);

        var normalizedName = normalize(name);

        if (commands.containsKey(normalizedName)) {
            throw new IllegalStateException("Command already registered: " + normalizedName);
        }

        commands.put(normalizedName, command);
    }

    public Optional<RedisCommand> find(String name) {
        return Optional.ofNullable(commands.get(normalize(name)));
    }

    private String normalize(String name) {
        return name.toUpperCase(Locale.ROOT);
    }
}
