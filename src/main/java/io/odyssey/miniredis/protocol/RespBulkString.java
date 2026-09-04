package io.odyssey.miniredis.protocol;

import java.util.Arrays;
import java.util.Objects;

public final class RespBulkString implements RespValue {

    private final byte[] value;

    public RespBulkString(byte[] value) {
        Objects.requireNonNull(value);
        this.value = Arrays.copyOf(value, value.length);
    }

    public byte[] value() {
        return Arrays.copyOf(value, value.length);
    }

    public int length() {
        return value.length;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RespBulkString other)) {
            return false;
        }

        return Arrays.equals(
                value,
                other.value
        );
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "RespBulkString[length=%d]"
                .formatted(value.length);
    }
}