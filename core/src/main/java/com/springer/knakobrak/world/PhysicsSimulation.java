package com.springer.knakobrak.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.springer.knakobrak.util.CollisionBits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PhysicsSimulation {

    private World world;
    public World getWorld() {return world;}

    private Map<Integer, PlayerState> players;
    public Map<Integer, PlayerState> getPlayers() {return players;}
    private Map<Integer, ProjectileState> projectiles;
    public Map<Integer, ProjectileState> getProjectiles() {return projectiles;}

    private ArrayList<Wall> walls;
    public ArrayList<Wall> getWalls() {return walls;}
    public void setWalls(ArrayList<Wall> walls) {this.walls = walls;}
    private int[][] wallGrid;
    public int[][] getWallGrid() {return wallGrid;}
    public void setWallGrid(int[][] grid) {this.wallGrid = grid;}

    private ArrayList<Vector2> playerSpawnPoints;
    public ArrayList<Vector2> getPlayerSpawnPoints() {return playerSpawnPoints;}
    public void setPlayerSpawnPoints(ArrayList<Vector2> points) {this.playerSpawnPoints = points;}

    public PhysicsSimulation() {
        players = new HashMap<>();
        projectiles = new HashMap<>();
        walls = new ArrayList<>();
        playerSpawnPoints = new ArrayList<>();
    }

    public void resetSimulation() {
        walls.clear();
        wallGrid = null;
        playerSpawnPoints.clear();
        players.clear();
        projectiles.clear();
    }

    public void step(float delta, int a, int b) {
        world.step(delta, a, b);
        age(delta);
    }

    public void step(float delta) {
        world.step(delta, 6, 2);
        age(delta);
    }

    private void age(float delta) {
        Iterator<ProjectileState> it = projectiles.values().iterator();
        while (it.hasNext()) {
            ProjectileState ps = it.next();
            Body body = ps.body;
            if (body == null) continue;
            ps.lifeTime += delta;
            if (ps.lifeTime >= ps.lifeTimeLimit || Math.abs(ps.x) > 500 || Math.abs(ps.y) > 500) {
                world.destroyBody(body);
                it.remove();
            }
        }
    }

    public PlayerState getPlayer(int id) {
        System.out.println("HELLOOO!!");
        printList();
        return players.get(id);
    }

    private void printList() {
        for (PlayerState p : players.values()) {
            System.out.print("["+p.id + "]: " + p.name + " is here!");
        }
    }

    public void addPlayer(PlayerState p) {
        System.out.println("One player ADDEDD!!!");
        players.put(p.id, p);
    }

    public void addProjectile(ProjectileState p) {
        projectiles.put(p.id, p);
    }

    public void addProjectile(int id, ProjectileState p) {
        projectiles.put(id, p);
    }

    public boolean containsProjectileKey(int id) {
        return projectiles.containsKey(id);
    }

    public void destroyBody(Body body) {
        world.destroyBody(body);
    }

    public void removePlayer(int id) {
        if (players == null || !players.containsKey(id)) return;
        players.remove(id);
    }

    public void addPlayerSpawnPoint(Vector2 v) {
        playerSpawnPoints.add(v);
    }

    public void addWall(Wall wall) {
        walls.add(wall);
    }

    public void initPhysics() {
        resetSimulation();
        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();
                if (isPredicted(a) || isPredicted(b)) {
                    System.out.println("NOPE!!");
                    return;
                }

                Object a2 = a.getUserData();
                Object b2 = b.getUserData();
                handleCollision(a2, b2);
            }

            public void endContact(Contact contact) {}
            public void preSolve(Contact contact, Manifold oldManifold) {}
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    void handleCollision(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) {
            int idA = (int) a;
            int idB = (int) b;

            if (isProjectile(idA) && isPlayer(idB)) {
                hitPlayer(idB, idA);
            } else if (isProjectile(idB) && isPlayer(idA)) {
                hitPlayer(idA, idB);
            }
        }
    }

    private boolean isPredicted(Fixture f) {
        return f.getFilterData().categoryBits == CollisionBits.PREDICTED;
    }

    boolean isPlayer(int id) {
        return players.containsKey(id);
    }

    boolean isProjectile(int id) {
        return projectiles.containsKey(id);
    }

    void hitPlayer(int playerId, int projectileId) {
        players.values().forEach(player -> {
            if (player.id == playerId) {
                player.hp--;
                //System.out.println("Player " + playerId + " was damaged! Remaining HP: " + player.hp);
            }
        });
    }
}
