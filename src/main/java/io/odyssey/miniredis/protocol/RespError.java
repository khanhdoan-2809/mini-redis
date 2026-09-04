package io.odyssey.miniredis.protocol;

import java.util.Objects;

public record RespError(String message) implements RespValue {

    public RespError {
        Objects.requireNonNull(message);

        if (message.contains("\r") || message.contains("\n")) {

            throw new IllegalArgumentException(
                    "RESP error cannot contain CR or LF"
            );
        }
    }
}