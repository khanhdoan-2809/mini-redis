package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespError;
import io.odyssey.miniredis.protocol.RespValue;

public final class CommandDispatcher {

    private final CommandRegistry registry;

    private final CommandRequestParser parser;

    public CommandDispatcher(CommandRegistry registry, CommandRequestParser parser) {
        this.registry = registry;
        this.parser = parser;
    }

    public RespValue dispatch(RespValue requestValue) {
        try {
            var request = parser.parse(requestValue);
            var command = registry.find(request.name());

            if (command.isPresent()) {
                return new RespError("ERR unknown command '" + request.name().toLowerCase() + "'");
            }

            return command.get().execute(request);
        } catch (CommandRequestException | CommandExecutionException e) {
            return new RespError(e.getMessage());
        }
    }
}
