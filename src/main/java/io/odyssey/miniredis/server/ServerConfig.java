package io.odyssey.miniredis.server;

public record ServerConfig(String bindAddress, int port) {

    public ServerConfig {
        if (bindAddress == null || bindAddress.isBlank()) {
            throw new IllegalArgumentException("bindAddress must not be blank");
        }

        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
    }

    public static ServerConfig defaults() {
        return new ServerConfig("0.0.0.0", 6379);
    }

    public static ServerConfig fromEnvironment() {
        var host = System.getenv()
                .getOrDefault("MINI_REDIS_HOST", "0.0.0.0");

        var port = Integer.parseInt(
                System.getenv().getOrDefault("MINI_REDIS_PORT", "6379")
        );

        return new ServerConfig(host, port);
    }
}