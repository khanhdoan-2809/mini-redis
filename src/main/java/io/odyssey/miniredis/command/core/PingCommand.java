package io.odyssey.miniredis.command.core;

import io.odyssey.miniredis.command.CommandRequest;
import io.odyssey.miniredis.command.CommandRequestException;
import io.odyssey.miniredis.command.RedisCommand;
import io.odyssey.miniredis.protocol.RespSimpleString;
import io.odyssey.miniredis.protocol.RespValue;

public final class PingCommand implements RedisCommand {

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().isEmpty()) {
            return new RespSimpleString("PONG");
        }

        if (request.arguments().size() == 1) {
            return request.arguments().getFirst();
        }

        throw new CommandRequestException("PING accepts at most 1 argument");
    }
}
