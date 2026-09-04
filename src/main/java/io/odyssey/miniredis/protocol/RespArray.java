package io.odyssey.miniredis.protocol;

import java.util.List;
import java.util.Objects;

public record RespArray(List<RespValue> values) implements RespValue {

    public RespArray {
        Objects.requireNonNull(values);

        values = List.copyOf(values);
    }
}