package io.odyssey.miniredis.protocol;

public record RespInteger(long value) implements RespValue{
}
