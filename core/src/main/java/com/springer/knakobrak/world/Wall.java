package com.springer.knakobrak.world;

import com.badlogic.gdx.physics.box2d.Body;

public class Wall {
    public float x, y;
    public float width, height;
    public Body body;

    @Override
    public String toString() {
        return "Wall: (" + x + "," + y + "), w: " + width + ", h: " + height + ", B: " + (body != null);
    }
}
