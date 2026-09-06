package io.odyssey.miniredis.datastore;

import io.odyssey.miniredis.protocol.RespBulkString;

import java.util.Arrays;
import java.util.Objects;

public final class RedisKey {

    private final byte[] value;

    public RedisKey(byte[] value) {
        Objects.requireNonNull(value);
        this.value = Arrays.copyOf(value, value.length);
    }

    public static RedisKey from(RespBulkString value) {
        return new RedisKey(value.value());
    }

    public byte[] value() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RedisKey other)) {
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
        return "RedisKey[length=%d]".formatted(value.length);
    }
}