package io.odyssey.miniredis.command.expiry;

import io.odyssey.miniredis.datastore.RedisDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpirationScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ExpirationScheduler.class);
    private static final long INTERVAL_MILLIS  = 100;

    private final RedisDatabase database;
    private final ScheduledExecutorService executor;

    public ExpirationScheduler(RedisDatabase database) {
        this.database = database;
        this.executor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("redis-expiry-", 0).factory());
    }

    public void start() {
        executor.scheduleAtFixedRate(this::runCycle, INTERVAL_MILLIS, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void runCycle() {
        try {
            var deleted = database.deleteExpiredKeys();

            if (deleted > 0) {
                log.debug("Active expiration removed {} keys", deleted);
            }
        } catch (Exception e) {
            log.error("Active expiration cycle failed", e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
