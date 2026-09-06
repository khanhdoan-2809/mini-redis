package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespValue;

public class EchoCommand implements RedisCommand {

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().size() != 1) {
            throw new CommandExecutionException("ERR wrong number of arguments for 'echo' command");
        }

        return request.arguments().getFirst();
    }
}
