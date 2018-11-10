package net;

import java.net.ServerSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import controller.Controller;
import common.MsgType;

public class HangmanServer {
    public static final int LINGER_TIME = 5000;
    public static final int TIME_HALF_HOUR = 1800000;
    private int portNo = 8080; // default
    private final Controller contr = new Controller();
    private final List<PlayerHandler> players = new ArrayList<>();

    public static void main(String[] args) {
        HangmanServer server = new HangmanServer ();             
        server.parseArguments(args);
        server.startGame();
        server.serve();
    }

    void broadcast (MsgType cmd, String msg) {
        synchronized (players) {
            contr.appendToHistory(cmd, msg);
            players.forEach((player)->player.sendMsg(cmd, msg));
        }
    }

    void removeHandler (PlayerHandler handler) {
        synchronized (players) {
            players.remove(handler);
        }
    }

    private void serve () {
        try {
            ServerSocket listeningSocket = new ServerSocket(portNo);
            while (true) {
                Socket clientSocket = listeningSocket.accept();
                startHandler(clientSocket);
            }
        } catch(IOException e) {
            System.err.println("Server failure.");
        }
    }

    public void startGame(){
        contr.selectedWord();
        broadcast(MsgType.NEWGAME, contr.showCurrentState() + "##" + contr.remainingGuesses());
    }

    private void startHandler(Socket playerSocket) throws SocketException{
            playerSocket.setSoLinger(true, LINGER_TIME);
            playerSocket.setSoTimeout(TIME_HALF_HOUR);
            PlayerHandler handler = new PlayerHandler(this, playerSocket, contr.getGameStatus(), contr);
            synchronized (players) {
                players.add(handler);
            } 
            Thread handlerThread = new Thread(handler);
            handlerThread.setPriority(Thread.MAX_PRIORITY);
            handlerThread.start();		
    }

    private void parseArguments(String[] arguments) {
        if (arguments.length > 0) {
            try {
                    portNo = Integer.parseInt(arguments[1]);
            } catch(NumberFormatException e) {
                    System.err.println("Invalid port number, using default");
            }
        }
    }      
}