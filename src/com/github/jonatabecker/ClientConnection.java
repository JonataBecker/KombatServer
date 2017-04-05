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
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    }

    private void playerEventOut() {
        Thread th2 = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(30);

                    if (!worldServer.getBullets().isEmpty()) {
                        commandBullets();
                    }
                    if (player.isDead()) {
                        continue;
                    }
                    if (commands.get(BULLET)) {
                        commandBullet();
                        continue;
                    }
                    if (commands.get(PUNCH)) {
                        commandPunch();
                        continue;
                    }
                    if (commands.get(RIGHT)) {
                        commandRight();
                        continue;
                    }
                    if (commands.get(LEFT)) {
                        commandLeft();
                        continue;
                    }
                    if (commands.get(DOWN)) {
                        commandDown();
                        continue;
                    }
                    if (commands.get(UP) || player.isUp()) {
                        commandUp();
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

    private void commandBullets() {
        worldServer.getBullets().stream().filter((b) -> {
            return player.equals(b.getPlayer());
        }).forEach((e) -> {
            int val = (e.getPos() == Player.POS_RIGHT) ? 8 * -1 : 8;
            e.setX(e.getX() + val);
            if (e.getX() > 1000) {
                worldServer.removeBullet(e);
            } else {
                Player hited = getHitedPlayer(e.getX(), e.getY());
                if (hited != null) {
                    hited.hit();
                    worldServer.removeBullet(e);
                }
            }
        });
        worldServer.fireEvent();
    }

    private void commandBullet() {
        worldServer.addBullet(new Bullet(player, player.isPosRight() ? player.getX() : player.getX() + Player.WIDTH, player.getY() + 30));
        worldServer.fireEvent();
    }

    private void commandPunch() {
        player.setState(Player.PUNCHING);
        int x = player.getX();
        int y = player.getY();
        if (player.isPosRight()) {
            x -= Player.WIDTH;
        } else {
            x += Player.WIDTH + 10;
        }
        Player hited = getHitedPlayer(x, y);
        if (hited != null) {
            hited.hitPunch();
            System.out.println(hited.getLivePercent());
        }
        worldServer.fireEvent();
    }

    private void commandRight() {
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
    }

    private void commandLeft() {
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
    }

    private void commandDown() {
        player.setState(Player.DOWN);
        worldServer.fireEvent();
    }

    private void commandUp() {
        if (player.isUp()) {
            return;
        }
        player.setState(Player.UP);
        Thread th = new Thread(() -> {
            double positionX = player.getX();
            double positionY = player.getY();
            double velocityX = 4.0;
            double velocityY = -20.5;
            double gravity = 1;
            int originalY = player.getY();

            while (true) {
                try {
                    velocityY += gravity;
                    positionY += velocityY;
                    positionX += velocityX;

//                    if (positionY < 50.0) {
//                        velocityY = 0.0;
//                    }

                    if (velocityY > 0 && positionY >= originalY) {
                        player.setX((int) positionX);
                        player.setY(originalY);
                        player.setState(Player.WAITING);
                        worldServer.fireEvent();
                        break;
                    }
                    player.setX((int) positionX);
                    player.setY((int) positionY);
                    worldServer.fireEvent();
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

        });
        th.start();
    }

    private boolean isHit(int x, int y) {
        return getHitedPlayer(x, y) != null;
    }

    private Player getHitedPlayer(int x, int y) {
        try {
            Player pl = worldServer.getPlayers().stream().filter((p) -> {
                return x >= p.getX() && x <= p.getX() + Player.WIDTH;
            }).findFirst().get();
            if (pl.equals(player)) {
                return null;
            }
            return pl;
        } catch (NoSuchElementException e) {
            return null;
        }
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
