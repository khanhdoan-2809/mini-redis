package io.odyssey.miniredis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniRedisApplication {

    private static final Logger log = LoggerFactory.getLogger(MiniRedisApplication.class);

    private MiniRedisApplication() {
    }

    public static void main(String[] args) {
        log.info("MiniRedis starting...");
    }
}
