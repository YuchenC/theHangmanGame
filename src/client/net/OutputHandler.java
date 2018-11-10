package client.net;

/**
 * Handles broadcast messages from server.
 */
public interface OutputHandler {
    /**
     * Called when a broadcast message from the server has been received. That message originates
     * from one of the clients.
     *
     * @param msg The message from the server.
     */
    public void handleMsg(String msg);
     public void handleMsgConnect(String msg);
}
