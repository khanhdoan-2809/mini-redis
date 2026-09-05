package io.odyssey.miniredis.command;

public final class CommandExecutionException extends RuntimeException {

    public CommandExecutionException(String message) {
        super(message);
    }
}