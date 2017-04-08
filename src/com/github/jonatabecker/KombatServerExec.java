package com.github.jonatabecker;

import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author JonataBecker
 */
public class KombatServerExec {

    private final WorldServer worldServer;
    private final ClientsController sendConnection;
    
    public KombatServerExec() {
        this.worldServer = new WorldServer();
        this.sendConnection = new ClientsController(worldServer);
    }

    public void waitForPlayer() {
        try {
            sendConnection.exec();
            ServerSocket ss = new ServerSocket(8880);
            while (true) {
                Socket s = ss.accept();
                ClientConnection connection = new ClientConnection(worldServer, s);
                connection.run();
                sendConnection.addConnection(connection);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting...");
        KombatServerExec kse = new KombatServerExec();
        kse.waitForPlayer();
    }

}
