package io.odyssey.miniredis;

import io.odyssey.miniredis.command.*;
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

        var registry = new CommandRegistry();
        registry.register("PING", new PingCommand());
        registry.register("ECHO", new EchoCommand());

        var parser = new CommandRequestParser();
        var dispatcher = new CommandDispatcher(registry, parser);

        try (var server = new RedisServer(config, dispatcher)) {
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
