package com.github.jonatabecker;

import com.github.jonatabecker.commons.Player;
import com.github.jonatabecker.commons.WorldParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author JonataBecker
 */
public class ClientConnection {

    public static final int SPEED = 5;

    private final WorldParser worldParser;
    private final WorldServer worldServer;
    private final Socket socket;
    private final Player player;

    boolean btR = false, btL = false, btU = false, btD = false;

    public ClientConnection(WorldServer worldServer, Socket socket) {
        this.worldParser = new WorldParser();
        this.worldServer = worldServer;
        this.socket = socket;
        this.player = new Player();
    }

    private void playerEventOut() {
        Thread th2 = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(30);
                    if (btR) {
                        player.setX(player.getX() + SPEED);
                        worldServer.fireEvent();
                    }
                }
            } catch (Exception e) {
            }
        });
        th2.setDaemon(true);
        th2.start();
    }

    private void playerEventIn(BufferedReader in) throws IOException {
        String command = "";
        while (!(command = in.readLine()).equals("exit")) {
            if (command.equals("PR_R")) {
                btR = true;
            }

            if (command.equals("RE_R")) {
                btR = false;
            }
        }
    }

    public void run() {
        Thread th = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                worldServer.addEvent(() -> {
                    out.println(worldParser.fromObject(worldServer.getWorld()));
                });
                worldServer.addPlayer(player);
                playerEventOut();
                playerEventIn(in);
                worldServer.removePlayer(player);
            } catch (Exception e) {
            }
        });
        th.setDaemon(true);
        th.start();
    }

}