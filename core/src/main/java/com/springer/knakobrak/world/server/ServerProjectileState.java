package com.springer.knakobrak.world.server;

import com.badlogic.gdx.physics.box2d.Body;

public class ServerProjectileState {
    public int id;
    //public int ownerId;
    public float x, y;
    public Body body;
    public float lifeTime = 0f;
    public int lifeTimeLimit = 3; // milliseconds

    public boolean isDead;
}
