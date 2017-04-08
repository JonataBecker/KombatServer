package com.github.jonatabecker;

import static com.github.jonatabecker.ClientConnection.LIMIT;
import static com.github.jonatabecker.ClientConnection.MIN;
import static com.github.jonatabecker.ClientConnection.SPEED;
import com.github.jonatabecker.commons.Bullet;
import com.github.jonatabecker.commons.Commands;
import com.github.jonatabecker.commons.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 *
 * @author JonataBecker
 */
public class ClientsController implements Commands {

    private final WorldServer worldServer;
    private final List<ClientConnection> connections;

    public ClientsController(WorldServer worldServer) {
        this.worldServer = worldServer;
        this.connections = new ArrayList<>();
    }

    public void addConnection(ClientConnection connection) {
        this.connections.add(connection);
    }

    private void event() {
        if (!worldServer.getBullets().isEmpty()) {
            commandBullets();
        }
        connections.stream().
                filter((c) -> {
                    return c.isConnected();
                }).
                forEach((connection) -> {
                    if (connection.getPlayer().isDead()) {
                        return;
                    }
                    if (connection.getPlayer().isUp()) {
                        return;
                    }
                    Map<String, Boolean> commands = connection.getCommands();
                    if (commands.get(BULLET)) {
                        commandBullet(connection.getPlayer());
                        return;
                    }
                    if (commands.get(PUNCH)) {
                        commandPunch(connection.getPlayer());
                        return;
                    }
                    if (commands.get(RIGHT)) {
                        commandRight(connection.getPlayer());
                        return;
                    }
                    if (commands.get(LEFT)) {
                        commandLeft(connection.getPlayer());
                        return;
                    }
                    if (commands.get(DOWN)) {
                        commandDown(connection.getPlayer());
                        return;
                    }
                    if (commands.get(UP)) {
                        commandUp(connection.getPlayer());
                        return;
                    }
                    if (!connection.getPlayer().isWaiting()) {
                        connection.getPlayer().setState(Player.WAITING);
                        worldServer.fireEvent();
                    }
                });

    }

    private void commandBullets() {
        worldServer.getBullets().stream().forEach((e) -> {
            int val = (e.getPos() == Player.POS_RIGHT) ? 8 * -1 : 8;
            e.setX(e.getX() + val);
            if (e.getX() > 1000) {
                worldServer.removeBullet(e);
            } else {
                Player hited = getHitedPlayer(e.getPlayer(), e.getX(), e.getY());
                if (hited != null) {
                    hited.hit();
                    worldServer.removeBullet(e);
                }
            }
        });
        worldServer.fireEvent();
    }

    private void commandBullet(Player player) {
        worldServer.addBullet(new Bullet(player, player.isPosRight() ? player.getX() : player.getX() + Player.WIDTH, player.getY() + 30));
        worldServer.fireEvent();
    }

    private void commandPunch(Player player) {
        player.setState(Player.PUNCHING);
        int x = player.getX();
        int y = player.getY();
        if (player.isPosRight()) {
            x -= Player.WIDTH;
        } else {
            x += Player.WIDTH + 10;
        }
        Player hited = getHitedPlayer(player, x, y);
        if (hited != null) {
            hited.hitPunch();
            System.out.println(hited.getLivePercent());
        }
        worldServer.fireEvent();
    }

    private void commandRight(Player player) {
        int pos = player.getX() + SPEED;
        if (pos > LIMIT) {
            pos = LIMIT;
        }
        if (isHit(player, pos + Player.WIDTH, player.getY())) {
            pos = player.getX();
        }
        player.setX(pos);
        player.setState(Player.WALKING);
        worldServer.fireEvent();
    }

    private void commandLeft(Player player) {
        int pos = player.getX() - SPEED;
        player.setState(Player.WALKING);
        if (pos < MIN) {
            pos = MIN;
        }
        if (isHit(player, pos, player.getY())) {
            pos = player.getX();
        }
        player.setX(pos);
        worldServer.fireEvent();
    }

    private void commandDown(Player player) {
        player.setState(Player.DOWN);
        worldServer.fireEvent();
    }

    private void commandUp(Player player) {
        if (player.isUp()) {
            return;
        }
        player.setState(Player.UP);
        Thread th = new Thread(() -> {
            double positionX = player.getX();
            double positionY = player.getY();
            double velocityX = 5.0;
            double velocityY = -15.5;
            double gravity = 1;
            int originalY = player.getY();
            if (player.isPosRight()) {
                velocityX *= -1;
            }
            while (true) {
                try {
                    velocityY += gravity;
                    positionY += velocityY;
                    positionX += velocityX;
                    if (velocityY > 0 && positionY >= originalY) {
                        player.setX((int) positionX);
                        player.setY(originalY);
                        player.setState(Player.WAITING);
                        List<Player> st = new ArrayList<>(worldServer.getPlayers()).stream().filter((p) -> {
                            return p.getX() < player.getX();
                        }).collect(Collectors.toList());
                        if (st.size() > 0) {
                            player.setPos(Player.POS_RIGHT);
                            st.forEach((p) -> {
                                p.setPos(Player.POS_LEFT);
                            });
                        } else {
                            worldServer.getPlayers().forEach((p) -> {
                                p.setPos(Player.POS_RIGHT);
                            });
                            player.setPos(Player.POS_LEFT);
                        }
                        worldServer.fireEvent();
                        break;
                    }
                    if (positionX < MIN) {
                        positionX = MIN;
                    }
                    if (positionX > LIMIT) {
                        positionX = LIMIT;
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

    private boolean isHit(Player player, int x, int y) {
        return getHitedPlayer(player, x, y) != null;
    }

    private Player getHitedPlayer(Player player, int x, int y) {
        try {
            Player pl = worldServer.getPlayers().stream().filter((p) -> {
                return !p.isDead();
            }).filter((p) -> {
                return x >= p.getX()
                        && x <= p.getX() + Player.WIDTH
                        && y >= (p.isDown() ? p.getY() + Player.HEIGHT / 2 : p.getY());
            }).findFirst().get();
            if (pl.equals(player)) {
                return null;
            }
            return pl;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void eventOut() {
        Thread th = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(30);
                    event();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public void eventDesconect() {
        Thread th = new Thread(() -> {
            try {
                while (true) {
                    List<ClientConnection> cs = new ArrayList<>(connections);
                    for (ClientConnection c : cs) {
                        if (c == null) {
                            continue;
                        }
                        c.sendAlert();
                        Thread.sleep(1000);
                        if (!c.isConnected()) {
                            connections.remove(c);
                            return;
                        }
                        if (!c.getCommands().get(CONNECT)) {
                            c.desconect();
                            connections.remove(c);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public void exec() {
        eventOut();
        eventDesconect();
    }

}
