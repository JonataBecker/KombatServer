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
    public static final int MIN = 10;
    public static final int LIMIT = 725;

    private final WorldParser worldParser;
    private final WorldServer worldServer;
    private final Socket socket;
    private final Player player;
    private final Map<String, Boolean> commands;

    private boolean connected = true;
    private PrintWriter out;
    private BufferedReader in;
    private Thread th;

    public ClientConnection(WorldServer worldServer, Socket socket) {
        this.worldParser = new WorldParser();
        this.worldServer = worldServer;
        this.socket = socket;
        this.player = new Player();
        this.commands = new HashMap<>();
        this.commands.put(LEFT, Boolean.FALSE);
        this.commands.put(RIGHT, Boolean.FALSE);
        this.commands.put(PUNCH, Boolean.FALSE);
        this.commands.put(BULLET, Boolean.FALSE);
        this.commands.put(UP, Boolean.FALSE);
        this.commands.put(DOWN, Boolean.FALSE);
        this.commands.put(CONNECT, Boolean.FALSE);
    }

    public Player getPlayer() {
        return player;
    }

    public Map<String, Boolean> getCommands() {
        return new HashMap<>(commands);
    }

    private void playerEventIn() throws IOException {
        String command;
        try {
            while (!(command = in.readLine()).equals("exit")) {
                boolean press = command.substring(0, 1).equals("P");
                commands.put(command.substring(1), press);
            }
        } catch (Exception e) {
            desconect();
        }
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            worldServer.addEvent(() -> {
                out.println(worldParser.fromObject(worldServer.getWorld()));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        th = new Thread(() -> {
            try {
                if (!worldServer.getPlayers().isEmpty()) {
                    player.setX(LIMIT);
                    player.setPos(Player.POS_RIGHT);
                }
                worldServer.addPlayer(player);
                playerEventIn();
                desconect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public void sendAlert() {
        commands.put(CONNECT, Boolean.FALSE);
        out.println(CONNECT);
    }

    public boolean isConnected() {
        return connected;
    }

    public void desconect() {
        th.interrupt();
        worldServer.removePlayer(player);
        connected = false;
    }

}
