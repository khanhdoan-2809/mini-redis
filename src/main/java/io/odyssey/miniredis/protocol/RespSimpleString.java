package io.odyssey.miniredis.protocol;

import java.util.Objects;

public record RespSimpleString(String value) implements RespValue {

    public RespSimpleString {
        Objects.requireNonNull(value);

        if (value.contains("\r") || value.contains("\n")) {
            throw new IllegalArgumentException(
                    "RESP simple string cannot contain CR or LF"
            );
        }
    }
}