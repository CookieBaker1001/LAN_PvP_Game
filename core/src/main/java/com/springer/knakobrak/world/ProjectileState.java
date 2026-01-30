package com.springer.knakobrak.world;

import com.badlogic.gdx.physics.box2d.Body;

public class ProjectileState {
    public int id;
    //public int ownerId;
    public float x, y, vx, vy;
    public Body body;
    public boolean isDead;
}
