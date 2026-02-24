package com.springer.knakobrak.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PhysicsSimulation {

    public World world;

    public Map<Integer, PlayerState> players;
    public Map<Integer, ProjectileState> projectiles;

    public ArrayList<Wall> walls;

    public ArrayList<Vector2> playerSpawnPoints;

    public PhysicsSimulation() {
        players = new HashMap<>();
        projectiles = new HashMap<>();
        walls = new ArrayList<>();
        playerSpawnPoints = new ArrayList<>();
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

//            Vector2 pos = body.getPosition();
//            ps.x = pos.x;
//            ps.y = pos.y;

//            Vector2 desiredVelocity = new Vector2(ps.vx, ps.vy).scl(BULLET_SPEED);
//            body.setLinearVelocity(desiredVelocity);

            if (ps.lifeTime >= ps.lifeTimeLimit || Math.abs(ps.x) > 500 || Math.abs(ps.y) > 500) {
                world.destroyBody(body);
                it.remove();
            }
        }
    }

    public PlayerState getPlayer(int id) {
        return players.get(id);
    }

    public void clearWalls() {
        walls.clear();
    }

    private void clearPlayerSpawnPoints() {
        playerSpawnPoints.clear();
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
        clearWalls();
        clearPlayerSpawnPoints();

        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Object a = contact.getFixtureA().getUserData();
                Object b = contact.getFixtureB().getUserData();
                handleCollision(a, b);
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

    boolean isPlayer(int id) {
        return players.containsKey(id);
    }

    boolean isProjectile(int id) {
        return projectiles.containsKey(id);
    }

    void hitPlayer(int playerId, int projectileId) {
        //removeProjectile(projectileId);
        //projectiles.get(projectileId).isDead = true;
        //damagePlayer(playerId);
        players.values().forEach(player -> {
            if (player.id == playerId) {
                player.hp--;
                //broadcast("DAMAGE " + playerId + " _now_has " + player.hp + " HP.");
                System.out.println("Player " + playerId + " was damaged! Remaining HP: " + player.hp);
            }
        });
    }
}
