package io.odyssey.miniredis;

import io.odyssey.miniredis.server.RedisServer;
import io.odyssey.miniredis.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MiniRedisApplication {

    private static final Logger log = LoggerFactory.getLogger(MiniRedisApplication.class);

    private MiniRedisApplication() {
    }

    public static void main(String[] args) {
        var config = ServerConfig.fromEnvironment();
        try (RedisServer server = new RedisServer(config)) {
            Runtime.getRuntime()
                    .addShutdownHook(Thread.ofPlatform().name("redis-shutdown")
                            .unstarted(server::close));
            server.start();

            server.awaitTermination();
        } catch (IOException e) {
            log.error("Failed to start MiniRedis", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("MiniRedis main thread interrupted");
        }
    }
}
