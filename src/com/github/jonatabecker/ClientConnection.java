package com.github.jonatabecker;

import com.github.jonatabecker.commons.Bullet;
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

    public static final int SPEED = 3;
    public static final int MIN = 10;
    public static final int LIMIT = 725;

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
        this.commands.put(PUNCH, Boolean.FALSE);
        this.commands.put(BULLET, Boolean.FALSE);
    }

    private void playerEventOut() {
        Thread th2 = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(30);

                    if (!worldServer.getBullets().isEmpty()) {
                        worldServer.getBullets().forEach((e) -> {
                            int val = (e.getPos() == Player.POS_RIGHT) ? SPEED * -2 : SPEED * 2;
                            e.setX(e.getX() + val);
                            if (e.getX() > 1000 || isHit(e.getX(), e.getY())) {
                                worldServer.removeBullet(e);
                            }
                        });
                        worldServer.fireEvent();
                    }

                    if (commands.get(BULLET)) {
                        worldServer.addBullet(new Bullet(player.getPos(), player.isPosRight() ? player.getX() : player.getX() + Player.WIDTH, player.getY() + 30));
                        worldServer.fireEvent();
                        continue;
                    }
                    if (commands.get(PUNCH)) {
                        player.setState(Player.PUNCHING);
                        worldServer.fireEvent();
                        continue;
                    }
                    if (commands.get(RIGHT)) {
                        int pos = player.getX() + SPEED;
                        if (pos > LIMIT) {
                            pos = LIMIT;
                        }
                        if (isHit(pos + Player.WIDTH, player.getY())) {
                            pos = player.getX();
                        }
                        player.setX(pos);
                        player.setState(Player.WALKING);
                        worldServer.fireEvent();
                        continue;
                    }
                    if (commands.get(LEFT)) {
                        int pos = player.getX() - SPEED;
                        player.setState(Player.WALKING);
                        if (pos < MIN) {
                            pos = MIN;
                        }
                        if (isHit(pos, player.getX())) {
                            pos = player.getX();
                        }
                        player.setX(pos);
                        worldServer.fireEvent();
                        continue;
                    }
                    if (!player.isWaiting()) {
                        player.setState(Player.WAITING);
                        worldServer.fireEvent();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        th2.setDaemon(true);
        th2.start();
    }

    private boolean isHit(int x, int y) {
        return worldServer.getPlayers().stream().anyMatch((p) -> {
            return x >= p.getX() && x <= p.getX() + Player.WIDTH;
        });
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
                });
                if (!worldServer.getPlayers().isEmpty()) {
                    player.setX(LIMIT);
                    player.setPos(Player.POS_RIGHT);
                }
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
