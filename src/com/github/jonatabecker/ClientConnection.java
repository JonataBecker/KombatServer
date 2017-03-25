package com.github.jonatabecker;

import com.github.jonatabecker.commons.Commands;
import com.github.jonatabecker.commons.Player;
import com.github.jonatabecker.commons.WorldParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author JonataBecker
 */
public class ClientConnection implements Commands {

    public static final int SPEED = 5;

    private final WorldParser worldParser;
    private final WorldServer worldServer;
    private final Socket socket;
    private final Player player;
    private final Map<String, Boolean> commands;

    public ClientConnection(WorldServer worldServer, Socket socket) {
        this.worldParser = new WorldParser();
        this.worldServer = worldServer;
        this.socket = socket;
        this.player = new Player();
        this.commands = new HashMap<>();
        this.commands.put(LEFT, Boolean.FALSE);
        this.commands.put(RIGHT, Boolean.FALSE);
    }

    private void playerEventOut() {
        Thread th2 = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(30);                    
                    if (commands.get(RIGHT)) {
                        player.setX(player.getX() + SPEED);
                        worldServer.fireEvent();
                    }
                    if (commands.get(LEFT)) {
                        player.setX(player.getX() - SPEED);
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
        String command;
        while (!(command = in.readLine()).equals("exit")) {
            boolean press = command.substring(0, 1).equals("P");
            commands.put(command.substring(1), press);
        }
    }

    public void run() {
        Thread th = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                worldServer.addEvent(() -> {
                    out.println(worldParser.fromObject(worldServer.getWorld()));
                    System.out.println(System.currentTimeMillis());
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
