package com.springer.knakobrak.world;

import com.badlogic.gdx.physics.box2d.Body;

public class Projectile {
    public int clientId;
    public int counter;
    public float x, y;
    public Body body;
    public float lifeTime = 0f;
    public int lifeTimeLimit = 3; // seconds

    public boolean isAlive = true;
}
