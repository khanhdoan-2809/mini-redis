package io.odyssey.miniredis.datastore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RedisDatabase {

    private final Map<RedisKey, RedisValue> keyspace = new HashMap<>();

    public void set(RedisKey key, RedisValue value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        keyspace.put(key, value);
    }

    public Optional<RedisValue> get(RedisKey key) {
        Objects.requireNonNull(key);
        return Optional.ofNullable(keyspace.get(key));
    }

    public long delete(List<RedisKey> keys) {
        var deleted = 0L;

        for (var key : keys) {
            if (keyspace.remove(key) != null) {
                deleted++;
            }
        }

        return deleted;
    }

    public long exists(List<RedisKey> keys) {
        var count = 0L;

        for (var key : keys) {
            if (keyspace.containsKey(key)) {
                count++;
            }
        }

        return count;
    }

    public int size() {
        return keyspace.size();
    }
}