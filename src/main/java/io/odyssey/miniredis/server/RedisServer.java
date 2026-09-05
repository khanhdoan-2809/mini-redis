package io.odyssey.miniredis.server;

import io.odyssey.miniredis.command.CommandDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisServer implements AutoCloseable{

    private static final Logger log = LoggerFactory.getLogger(RedisServer.class);

    private final ServerConfig config;

    private final AtomicBoolean running = new AtomicBoolean();

    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();

    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(
            Thread.ofPlatform().name("redis-client", 0).factory() // create platform threads, not virtual threads
    );

    private volatile ServerSocket serverSocket;

    private volatile Thread acceptorThread;

    private final CommandDispatcher commandDispatcher;

    public RedisServer(ServerConfig config, CommandDispatcher commandDispatcher) {
        this.config = Objects.requireNonNull(config);
        this.commandDispatcher = Objects.requireNonNull(commandDispatcher);
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running");
        }

        var socket = new ServerSocket();
        try {
            socket.bind(new InetSocketAddress(config.bindAddress(), config.port()));

            serverSocket = socket;

            acceptorThread = Thread.ofPlatform().name("redis-acceptor").start(this::acceptLoop);

            log.info("Redis server listening on {}:{}", config.bindAddress(), config.port());
        } catch (IOException e) {
            running.set(false);

            try {
                socket.close();
            } catch (IOException e1) {
                log.error("Failed to close server socket", e1);
            }

            throw e;
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                var client = serverSocket.accept();
                clients.add(client);
                clientExecutor.execute(
                        () -> handleClient(client)
                );
            } catch (SocketException e) {
                if (running.get()) {
                    log.error("Socket failure while accepting client", e);
                }
            }
            catch (IOException e) {
                if (running.get()) {
                    log.error("Failed to accept client connection", e);
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            new ClientHandler(client, commandDispatcher).run();
        }
        finally {
            clients.remove(client);
        }
    }

    public int getLocalPort() {
        var socket = serverSocket;
        if (socket == null) {
            throw new IllegalStateException("Server is not started");
        }

        return socket.getLocalPort();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void awaitTermination() throws InterruptedException {
        var thread = acceptorThread;

        if (thread != null) {
            thread.join();
        }
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        log.info("Shutting down Redis server...");

        closeServerSocket();
        closeClients();
        clientExecutor.shutdownNow();

        log.info("Redis server shutdown complete");
    }

    private void closeServerSocket() {
        var socket = serverSocket;

        if (socket == null) {
            return;
        }

        try {
            socket.close();
        } catch (IOException e) {
            log.warn("Failed to close server socket", e);
        }
    }

    private void closeClients() {
        clients.forEach(client -> {
            try {
                client.close();
            } catch (IOException e) {
                log.warn("Failed to close client socket", e);
            }
        });

        clients.clear();
    }
}
