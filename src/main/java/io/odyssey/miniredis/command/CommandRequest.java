package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespBulkString;

import java.util.List;
import java.util.Objects;

public record CommandRequest(String name, List<RespBulkString> arguments) {

    public CommandRequest {
        Objects.requireNonNull(name);
        Objects.requireNonNull(arguments);
        arguments = List.copyOf(arguments);
    }
}
