package io.odyssey.miniredis;

import io.odyssey.miniredis.command.*;
import io.odyssey.miniredis.command.core.EchoCommand;
import io.odyssey.miniredis.command.core.PingCommand;
import io.odyssey.miniredis.command.expiry.ExpirationScheduler;
import io.odyssey.miniredis.command.key.DelCommand;
import io.odyssey.miniredis.command.key.ExistsCommand;
import io.odyssey.miniredis.command.key.ExpireCommand;
import io.odyssey.miniredis.command.key.TtlCommand;
import io.odyssey.miniredis.command.string.GetCommand;
import io.odyssey.miniredis.command.string.SetCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
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
        var database = new RedisDatabase();

        var registry = new CommandRegistry();
        registry.register("PING", new PingCommand());
        registry.register("ECHO", new EchoCommand());

        registry.register("SET", new SetCommand(database));
        registry.register("GET", new GetCommand(database));
        registry.register("DEL", new DelCommand(database));
        registry.register("EXISTS", new ExistsCommand(database));
        registry.register("EXPIRE", new ExpireCommand(database));
        registry.register("TTL", new TtlCommand(database));

        var parser = new CommandRequestParser();
        var dispatcher = new CommandDispatcher(registry, parser);

        try (var server = new RedisServer(config, dispatcher);
            var expirationScheduler = new ExpirationScheduler(database)) {
            Runtime.getRuntime()
                    .addShutdownHook(Thread.ofPlatform().name("redis-shutdown")
                            .unstarted(server::close));

            expirationScheduler.start();
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
