package net;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.StringJoiner;

import common.MsgType;
import controller.Controller;


/**
 *
 * @author yuchen
 */
public class PlayerHandler implements Runnable{
    private final HangmanServer server;
    private final Socket playerSocket;
    private final String[] gameWhenStarting;
    private final Controller contr;
    private boolean connected;
    private BufferedReader fromPlayer;
    private PrintWriter toPlayer;
    
    private static final String JOIN_MSG = " joined the game, welcome!";
    private String username = "anonymous";
    private String guess;
    private static final String LEAVE_MSG = " left the game :( .";
    private static final String USER_DELIMETER = ": ";
    private static final String WORD_MSG = "The word is ";
    
    
    PlayerHandler(HangmanServer server, Socket playerSocket, String[] game, Controller contr) {
        this.server = server;
        this.playerSocket = playerSocket;
        this.gameWhenStarting = game;
        this.contr = contr;
        connected = true;
    }
    
    @Override
    public void run() {
        try {
            boolean autoFlush = true;
            fromPlayer = new BufferedReader (new InputStreamReader
                            (playerSocket.getInputStream()));
            toPlayer = new PrintWriter(playerSocket.getOutputStream(), autoFlush);
            
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        
        for (String entry : gameWhenStarting) {
            sendHistory(entry);
        }
        while (connected) {
            try {
                Message msg = new Message(fromPlayer.readLine());
                switch(msg.msgType) {
                    case USER:
                        username = msg.msgBody;
                        server.broadcast(msg.msgType, username);
                        break;
                        
                    case GUESS:
                        guess = msg.msgBody;
                        contr.playGame(guess);
                        boolean right = contr.correctWord();
                        int remainingGuessses = contr.remainingGuesses();
                        
                        StringJoiner joiner = new StringJoiner("##");
                        joiner.add(username);
                        joiner.add(guess);
                        joiner.add(contr.showCurrentState());
                        joiner.add(Integer.toString(remainingGuessses));
                        
                        server.broadcast(msg.msgType, joiner.toString());
                        if(remainingGuessses == 0) {
                            joiner.add(Integer.toString(contr.score()));
                            joiner.add("lose");
                            joiner.add(contr.getWord());
                            server.broadcast(MsgType.ENDGAME, joiner.toString());
                            server.startGame();
                        } else if(right) {
                            joiner.add(Integer.toString(contr.score()));
                            joiner.add("win");
                            server.broadcast(MsgType.ENDGAME, joiner.toString());
                            server.startGame();
                            System.out.println("no of threads");
                        System.out.println(Thread.activeCount());
                        }
                        System.out.println(guess);
                        break;
                        
                    case DISCONNECT:
                        disconnect();
                        server.broadcast(msg.msgType, username);
                        break;
                        
                    default:
                        System.out.println("Command:" + msg.receivedString + "is not known.");
                }
            } catch (IOException ioe) {
                disconnect();
                ioe.printStackTrace();
            }
        }
    }
    
    // package-private method
    void sendHistory (String msg) {
        StringJoiner joiner = new StringJoiner("##");
        joiner.add(MsgType.BROADCAST.toString());
        joiner.add(msg);
        toPlayer.println(joiner.toString());
    }
    
    void sendMsg(MsgType cmd, String msg) {
        StringJoiner joiner = new StringJoiner("##");
        joiner.add(MsgType.BROADCAST.toString());
        joiner.add(cmd.toString());
        joiner.add(msg);
        toPlayer.println(joiner.toString());
    }
    
    private void disconnect() {
        try {
            playerSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        connected = false;
        server.removeHandler(this);
    }
    
    private static class Message {
        private String receivedString;
        private MsgType msgType;
        private String msgBody;
        
        private Message (String receivedString) {
            parse(receivedString);
            this.receivedString = receivedString;
        }
        
        private void parse (String strToParse) {
            try {
                String[] msgTokens = strToParse.split("##");
                msgType = MsgType.valueOf(msgTokens[0].toUpperCase());
                if (hasBody(msgTokens)) {
                    msgBody = msgTokens[1];
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        
        private boolean hasBody(String[] msgTokens) {
            return msgTokens.length > 1;
        }
    }
}
