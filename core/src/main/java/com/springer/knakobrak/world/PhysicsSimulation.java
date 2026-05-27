package com.springer.knakobrak.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.*;

public class PhysicsSimulation {

    public String owner;
    public Map<Integer, Player> players;
    public int[][] wallGrid;

    public World world;

    public PhysicsSimulation(String owner) {
        this.owner = owner;
        players = new HashMap<>();
    }

    public void resetSimulation() {
        players.clear();
    }

    public void initWorld() {
        resetSimulation();
        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();
                PhysicsData dataA = (PhysicsData) a.getUserData();
                PhysicsData dataB = (PhysicsData) b.getUserData();
                if (dataA == null || dataB == null) return;
                handleCollision(dataA, dataB);
            }
            public void endContact(Contact contact) {}
            public void preSolve(Contact contact, Manifold manifold) {}
            public void postSolve(Contact contact, ContactImpulse contactImpulse) {}
        });
    }

    private void handleCollision(PhysicsData a, PhysicsData b) {
        System.out.println("Collision detected between " + a.type + " and " + b.type);
    }

    public void addPlayer(int id, Player p) {
        players.put(id, p);
        System.out.println(owner + ": New player added!");
    }

    public void printWallGrid() {
        if (wallGrid == null) {
            System.out.println("No wall grid exists...");
            return;
        }
        System.out.println("---GRID---");
        for (int i = 0; i < wallGrid.length; i++) {
            for (int j = 0; j < wallGrid[0].length; j++) {
                System.out.print(wallGrid[i][j]);
            }
            System.out.println();
        }
        System.out.println("---GRID---");
    }

    public void playerStates() {
        StringBuilder s = new StringBuilder(owner + ": State\n");
        for (Player p : players.values()) {
            s.append("ID:").append(p.id).append(", (").append(p.realX).append(",").append(p.realY).append(")\n");
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
