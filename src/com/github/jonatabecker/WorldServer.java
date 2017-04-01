package com.github.jonatabecker;

import com.github.jonatabecker.commons.Bullet;
import com.github.jonatabecker.commons.Player;
import com.github.jonatabecker.commons.World;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JonataBecker
 */
public class WorldServer {

    private final World world;

    private final List<Event> events;
    
    public WorldServer() {
        this.world = new World();
        this.events = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        getWorld().addPlayer(player);
        fireEvent();
    }
    
    public void removePlayer(Player player) {
        getWorld().removePlayer(player);
        fireEvent();
    }
    
    public List<Player> getPlayers() {
        return new ArrayList<>(getWorld().getPlayers());
    }

    public void addBullet(Bullet bullet) {
        getWorld().addBullet(bullet);
        fireEvent();
    }
    
    public List<Bullet> getBullets() {
        return getWorld().getBullets();
    }
    
    public void removeBullet(Bullet bullet) {
        getWorld().removeBullet(bullet);
        fireEvent();
    }

    public synchronized World getWorld() {
        return world;
    }

    public void addEvent(Event e) {
        this.events.add(e);
    }
    
    public void fireEvent() {
        this.events.forEach((e) -> {
            e.run();
        });
    }
}
