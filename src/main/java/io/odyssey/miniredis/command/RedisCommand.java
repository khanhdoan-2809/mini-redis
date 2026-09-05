package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespValue;

public interface RedisCommand {

    RespValue execute(CommandRequest request);
}
