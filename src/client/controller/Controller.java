package client.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import client.net.ServerConnection;
import client.net.OutputHandler;
/**
 * This controller decouples the view from the network layer. All methods, except
 * <code>disconnect</code>, submit their task to the common thread pool, provided by
 * <code>ForkJoinPool.commonPool</code>, and then return immediately.
 */
public class Controller {
    private final ServerConnection serverConnection = new ServerConnection();

    /**
     * This method connects the client to the server.
     */
    public void connect(String host, int port, OutputHandler outputHandler) {
        CompletableFuture.runAsync(() -> {
            try {
                serverConnection.connect(host, port, outputHandler);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }).thenRun(() -> outputHandler.handleMsgConnect("Connected to " + host + ":" + port));
    }

    /**
     * @see ServerConnection#disconnect() Blocks until disconnection is completed.
     */
    public void disconnect() throws IOException {
        serverConnection.disconnect();
    }

    /**
     * @see ServerConnection#sendUsername(java.lang.String)
     */
    public void sendUsername(String username, OutputHandler outputHandler) {
        CompletableFuture.runAsync(() -> 
            serverConnection.sendUsername(username));
    }

    /**
     * @see ServerConnection#sendMsg(java.lang.String)
     */
    public void sendMsg(String msg) {
        CompletableFuture.runAsync(() -> serverConnection.sendMsg(msg));
    }

    public void sendGuess(String msg) { 
        CompletableFuture.runAsync(() -> serverConnection.sendGuess(msg)); 
    }
}
