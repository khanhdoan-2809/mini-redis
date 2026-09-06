package io.odyssey.miniredis.datastore;

import java.util.Arrays;
import java.util.Objects;

public final class RedisString implements RedisValue {

    private final byte[] value;

    public RedisString(byte[] value) {
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
    public RedisType type() {
        return RedisType.STRING;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RedisString other)) {
            return false;
        }

        return Arrays.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "RedisString[length=%d]".formatted(value.length);
    }
}