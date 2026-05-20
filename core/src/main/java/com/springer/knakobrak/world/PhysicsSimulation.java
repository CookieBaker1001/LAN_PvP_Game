package com.springer.knakobrak.world;

import java.util.*;

public class PhysicsSimulation {

    public String owner = "";
    public Map<Integer, Player> players;

    public PhysicsSimulation(String owner) {
        this.owner = owner;
        players = new HashMap<>();
    }

    public void resetSimulation() {
        players.clear();
    }

    public void addPlayer(int id, Player p) {
        players.put(id, p);
        System.out.println(owner + ": New player added!");
    }

    public void playerStates() {
        StringBuilder s = new StringBuilder(owner + ": State\n");
        for (Player p : players.values()) {
            s.append("ID:").append(p.id).append(", (").append(p.x).append(",").append(p.y).append(")\n");
        }
        s.append("End of states");
        System.out.println(s);
    }

    public Player getPlayer(int id) {
        return players.get(id);
    }

    public void removePlayer(int id) {
        if (players == null || !players.containsKey(id)) return;
        players.remove(id);
    }

    public void closeSimulation() {
        resetSimulation();
        players = null;
    }
}
