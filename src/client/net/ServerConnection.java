package client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringJoiner;
import common.MsgType;

/**
 * Manages all communication with the server.
 */
public class ServerConnection {
    private static final int TIMEOUT_HALF_HOUR = 1800000;
    private static final int TIMEOUT_HALF_MINUTE = 30000;
    private Socket socket;
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private volatile boolean connected;

    /**
     * Creates a new instance and connects to the specified server. Also starts a listener thread
     * receiving broadcast messages from server.
     *
     * @param host             Host name or IP address of server.
     * @param port             Server's port number.
     * @param broadcastHandler Called whenever a broadcast is received from server.
     * @throws IOException If failed to connect.
     */
    public void connect(String host, int port, OutputHandler broadcastHandler) throws
            IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), TIMEOUT_HALF_MINUTE);
        socket.setSoTimeout(TIMEOUT_HALF_HOUR);
        connected = true;
        boolean autoFlush = true;
        toServer = new PrintWriter(socket.getOutputStream(), autoFlush);
        fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new Thread(new Listener(broadcastHandler)).start();
    }

    /**
     * Closes the connection with the server and stops the broadcast listener thread.
     *
     * @throws IOException If failed to close socket.
     */
    public void disconnect() throws IOException {
        ServerConnection.this.sendMsg(MsgType.DISCONNECT.toString());
        socket.close();
        socket = null;
        connected = false;
    }

    /**
     * Sends the user's username to the server. That username will be prepended to all messages
     * originating from this client, until a new username is specified.
     *
     * @param username The current user's username.
     */
    public void sendUsername(String username) {
        ServerConnection.this.sendMsg(MsgType.USER.toString(), username);
    }

    /**
     * Sends a message entry to the server, which will broadcast it to all clients, including the
     * sending client.
     *
     * @param msg The message to broadcast.
     */
    public void sendMsg(String msg) {
        ServerConnection.this.sendMsg(MsgType.USER.toString(), msg);
    }

    public void sendGuess(String msg) { ServerConnection.this.sendMsg(MsgType.GUESS.toString(), msg); }

    private void sendMsg(String... parts) {
        StringJoiner joiner = new StringJoiner("##");
        for (String part : parts) {
            joiner.add(part);
        }
        toServer.println(joiner.toString());
    }

    private class Listener implements Runnable {
        private final OutputHandler outputHandler;

        private Listener(OutputHandler outputHandler) {
            this.outputHandler = outputHandler;
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    outputHandler.handleMsg(extractMsgBody(fromServer.readLine()));
                }
            } catch (Throwable connectionFailure) {
                if (connected) {
                    outputHandler.handleMsg("Lost connection.");
                }
            }
        }

        private String extractMsgBody(String entireMsg) {
            String[] msgParts = entireMsg.split("##");
            if (MsgType.valueOf(msgParts[0].toUpperCase()) != MsgType.BROADCAST) {
                System.out.println("Received corrupt message: " + entireMsg);
            }
            StringBuilder msg = new StringBuilder("");
            for(int i =0; i<msgParts.length; i++) {
                msg.append(msgParts[i]);
                msg.append("##");
            }
            return msg.toString();
        }
    }
}
