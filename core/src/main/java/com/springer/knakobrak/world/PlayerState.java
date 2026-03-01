package com.springer.knakobrak.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.springer.knakobrak.util.Constants;

import java.io.Serializable;

import static com.springer.knakobrak.util.Constants.MAX_HEALTH;
import static com.springer.knakobrak.util.Constants.PIXELS_PER_METER;

public class PlayerState implements Serializable {
    public int id;
    public String name;
    public Body body;
    public int playerIcon;
    public int ballIcon;
    public int hp = MAX_HEALTH;
    public boolean isDead = false;
    public float x, y;
    public float deathTimer = 0f;
    public float invincibilityTimer = 0f;
    public boolean isInvincible = false;
    public Vector2 nextSpawnPoint = new Vector2();

    public void resurrect() {
        nextSpawnPoint.x += Constants.pxToMeters(PIXELS_PER_METER/2);
        nextSpawnPoint.y += Constants.pxToMeters(PIXELS_PER_METER/2);
        body.setTransform(nextSpawnPoint, 0f);
        System.out.println("Respawning at (" + nextSpawnPoint.x + "," + nextSpawnPoint.y + ")");
    }

    @Override
    public String toString () {
        return "PlayerState{id=" + id + ", x=" + x + ", y=" + y + "}";
    }
}
