package io.odyssey.miniredis.datastore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class RedisDatabase {

    private final Map<RedisKey, RedisValue> keyspace = new HashMap<>();
    private final Map<RedisKey, Long> expirations = new HashMap<>();
    private final LongSupplier currentTimeMillis;

    public RedisDatabase() {
        this(System::currentTimeMillis);
    }

    RedisDatabase(LongSupplier currentTimeMillis) {
        this.currentTimeMillis = Objects.requireNonNull(currentTimeMillis);
    }

    public synchronized void set(RedisKey key, RedisValue value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        keyspace.put(key, value);
        expirations.remove(key);
    }

    public synchronized Optional<RedisValue> get(RedisKey key) {
        Objects.requireNonNull(key);

        expireIfNeeded(key);

        return Optional.ofNullable(keyspace.get(key));
    }

    public synchronized long delete(List<RedisKey> keys) {
        var deleted = 0L;

        for (var key : keys) {
            expireIfNeeded(key);

            if (deleteKey(key)) {
                deleted++;
            }
        }

        return deleted;
    }

    public synchronized long exists(List<RedisKey> keys) {
        var count = 0L;

        for (var key : keys) {
            expireIfNeeded(key);

            if (keyspace.containsKey(key)) {
                count++;
            }
        }

        return count;
    }

    public synchronized boolean expire(RedisKey key, long seconds) {
        expireIfNeeded(key);
        if (!keyspace.containsKey(key)) {
            return false;
        }

        if (seconds <= 0) {
            deleteKey(key);
            return true;
        }

        var timeoutMillis = Math.multiplyExact(seconds, 1_000L);
        var expiresAt = Math.addExact(currentTimeMillis.getAsLong(), timeoutMillis);

        expirations.put(key, expiresAt);

        return true;
    }

    public synchronized long deleteExpiredKeys() {
        var now = currentTimeMillis.getAsLong();
        var deleted = 0L;
        var iterator = expirations.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() <= now) {
                expirations.remove(entry.getKey());
                iterator.remove();
                deleted++;
            }
        }

        return deleted;
    }

    public synchronized int size() {
        return keyspace.size();
    }

    private boolean expireIfNeeded(RedisKey key) {
        var expiresAt = expirations.get(key);

        if (expiresAt == null || expiresAt > currentTimeMillis.getAsLong()) {
            return false;
        }

        deleteKey(key);

        return true;
    }

    private boolean deleteKey(RedisKey key) {
        expirations.remove(key);
        return keyspace.remove(key) != null;
    }

    public synchronized long ttl(RedisKey key) {
        expireIfNeeded(key);

        if (!keyspace.containsKey(key)) {
            return -2;
        }

        var expiresAt = expirations.get(key);
        if (expiresAt == null) {
            return -1;
        }

        var remainingMillis = expiresAt - currentTimeMillis.getAsLong();
        return Math.max(0, (remainingMillis + 500) / 1_000);
    }
}