package com.springer.knakobrak.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.springer.knakobrak.util.CollisionBits;
import com.springer.knakobrak.util.ObjectType;
import com.springer.knakobrak.util.PhysicsData;

import static com.springer.knakobrak.util.Constants.*;

import java.util.*;

public class PhysicsSimulation {

    public String owner = "";

    private World world;
    public World getWorld() {return world;}

    private Map<Integer, PlayerState> players;
    public Map<Integer, PlayerState> getPlayers() {return players;}
    private Map<ProjectileId, ProjectileState> projectiles;
    public Map<ProjectileId, ProjectileState> getProjectiles() {return projectiles;}

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
        //this.owner = owner;
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

    private ProjectileState deadProjectile = null;
    private void age(float delta) {

        for (ProjectileState ps : projectiles.values()) {
            Body body = ps.body;
            if (body == null) continue;
            ps.lifeTime += delta;
            if (ps.lifeTime >= ps.lifeTimeLimit || !ps.isAlive || Math.abs(ps.x) > 500 || Math.abs(ps.y) > 500) {
                //System.out.println(owner + " ps.id: (" + ps.clientId + "," + ps.counter + "), ps.isAlive: " + ps.isAlive);
                world.destroyBody(body);
                deadProjectile = ps;
                //break;
            }
        }
        if (deadProjectile != null) {
            //System.out.println(owner + ": An object has been removed!");
            projectiles.remove(new ProjectileId(deadProjectile.clientId, deadProjectile.counter));
            deadProjectile = null;
        }
//        Iterator<ProjectileState> it = projectiles.values().iterator();
//        while (it.hasNext()) {
//            ProjectileState ps = it.next();
//            Body body = ps.body;
//            if (body == null) continue;
//            ps.lifeTime += delta;
//            if (ps.lifeTime >= ps.lifeTimeLimit || !ps.isAlive || Math.abs(ps.x) > 500 || Math.abs(ps.y) > 500) {
//                System.out.println("ps.id: " + ps.id + ", ps.isAlive: " + ps.isAlive);
//                world.destroyBody(body);
//                it.remove();
//            }
//        }

        for (PlayerState p : players.values()) {
            if (p.isInvincible) {
                p.invincibilityTimer += delta;
                if (p.invincibilityTimer >= INVINCIBILITY_WINDOW) {
                    p.isInvincible = false;
                    p.invincibilityTimer = 0f;
                }
            }
            if (p.isDead) {
                p.deathTimer += delta;
                if (p.deathTimer >= DEATH_DURATION) {
                    p.isDead = false;
                    p.deathTimer = 0f;
                    p.resurrect();
                    p.hp = MAX_HEALTH;
                }
            }
        }
    }

    public PlayerState getPlayer(int id) {
        //printList();
        return players.get(id);
    }

    private void printPlayerList() {
        for (PlayerState p : players.values()) {
            System.out.print("["+p.id + "]: " + p.name + " is here!");
        }
    }

    int seconds = 0;
    public void printProjectileList() {
        System.out.print("[" + (++seconds) + "]: (");
        if (projectiles.isEmpty()) System.out.print(" empty ");
        else {
            System.out.print(" ");
            for (ProjectileState ps : projectiles.values()) {
                System.out.print(ps.counter + " ");
            }
        }
        System.out.println("), size: " + projectiles.size());
    }

    public void addPlayer(PlayerState p) {
        players.put(p.id, p);
    }

    public void addProjectile(ProjectileId id, ProjectileState p) {
        projectiles.put(id, p);
    }

    public boolean containsProjectileKey(ProjectileId id) {
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
                    return;
                }

                PhysicsData dataA = (PhysicsData) a.getUserData();
                PhysicsData dataB = (PhysicsData) b.getUserData();

                if (dataA == null || dataB == null) return;

                handleCollision(dataA, dataB);
            }

            public void endContact(Contact contact) {}
            public void preSolve(Contact contact, Manifold oldManifold) {}
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    void handleCollision(PhysicsData a, PhysicsData b) {
        //if (a.type != ObjectType.PROJECTILE && b.type != ObjectType.PROJECTILE) return;
        //System.out.println(owner + " detected a bounce!");
        //System.out.println(owner + ": a:" + a.toString() + ", b:" + b.toString());
        //System.out.println(owner + ": HIT! a: " + a + ", b: " + b);
//        if (a instanceof Integer && b instanceof Integer) {
//            int idA = (int) a;
//            int idB = (int) b;
//
//            if (isProjectile(idA) && isPlayer(idB)) {
//                //System.out.println(owner + ": a: " + a.toString() + ", b: " + b.toString());
//                hitPlayer(idB, idA);
//            } else if (isProjectile(idB) && isPlayer(idA)) {
//                hitPlayer(idA, idB);
//            }
//        }

//        if (a instanceof Integer && b instanceof ProjectileId) {
//            int idA = (int) a;
//            ProjectileId idB = (ProjectileId) b;
//            if (isPlayer(idA) && isProjectile(idB)) {
//                hitPlayer(idA, idB);
//            }
//        } else if (a instanceof ProjectileId && b instanceof Integer) {
//            ProjectileId idA = (ProjectileId) a;
//            int idB = (int) b;
//            if (isPlayer(idB) && isProjectile(idA)) {
//                hitPlayer(idB, idA);
//            }
//        }

        if (a.type == ObjectType.PROJECTILE && b.type == ObjectType.PLAYER) {
            ProjectileId projectileId = new ProjectileId(a.clientId, a.counter);
            if (isPlayer(b.clientId) && isProjectile(projectileId)) {
                //System.out.println(owner + ": That was a bounce on a player!");
                hitPlayer(b.clientId, projectileId);
            }
        }

        if (b.type == ObjectType.PROJECTILE && a.type == ObjectType.PLAYER) {
            ProjectileId projectileId = new ProjectileId(b.clientId, b.counter);
            if (isPlayer(a.clientId) && isProjectile(projectileId)) {
                //System.out.println(owner + ": That was a bounce on a player!");
                hitPlayer(a.clientId, projectileId);
            }
        }
    }

    private boolean isPredicted(Fixture f) {
        return f.getFilterData().categoryBits == CollisionBits.PREDICTED;
    }

    boolean isPlayer(int id) {
        return players.containsKey(id);
    }

    boolean isProjectile(ProjectileId id) {
        return projectiles.containsKey(id);
    }

    void hitPlayer(int playerId, ProjectileId projectileId) {
        ProjectileState ps = projectiles.get(projectileId);
        System.out.println("Hit!");
        //System.out.println(owner + ": Player [" + playerId + "] got hit by bullet " + projectileId);
        players.values().forEach(player -> {
            if (player.id == playerId) {
                if (player.isDead) return;
                player.takeDamage(1);
                if (player.hp <= 0 && !player.isDead) {
                    player.isDead = true;
                    player.isInvincible = true;
                    Random r = new Random();
                    player.nextSpawnPoint = playerSpawnPoints.get(r.nextInt(0, playerSpawnPoints.size()));
                    //System.out.println("I've generated this new spawnPoint: (" + player.nextSpawnPoint.x + "," + player.nextSpawnPoint.y + ")");
                }
                //System.out.println(owner + ": The bullet hit someone! Remove the bullet now!");
                //ps.isAlive = false;
                ps.lifeTime = ps.lifeTimeLimit;
                //System.out.println(owner + ": Lifespan: " + ps.lifeTime);
                //if (owner != null) owner.onTakeDamage(player.id, projectileId);
                //System.out.println("Player " + playerId + " was damaged! Remaining HP: " + player.hp);
            }
        });
    }
}
