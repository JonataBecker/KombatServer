package com.github.jonatabecker;

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
        world.addPlayer(player);
        fireEvent();
    }
    
    public void removePlayer(Player player) {
        world.removePlayer(player);
        fireEvent();
    }

    public World getWorld() {
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