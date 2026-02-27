package com.springer.knakobrak.world;

import com.badlogic.gdx.physics.box2d.Body;

public class ProjectileState {
    public int id;
    public int ownerId;
    public int localPlayerFireSequence;
    public float x, y;
    public Body body;
    public float lifeTime = 0f;
    public int lifeTimeLimit = 3; // milliseconds

    public boolean isAlive;
}
