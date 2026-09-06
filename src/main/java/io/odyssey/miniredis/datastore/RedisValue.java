package io.odyssey.miniredis.datastore;

public sealed interface RedisValue permits RedisString {
    RedisType type();
}
