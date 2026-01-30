package com.springer.knakobrak.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.*;

public class ServerPlayerState {
    public int id;
    public Body body;
    public int hp = 3;

    public Color color = Color.WHITE;
    public float x, y;

    @Override
    public String toString () {
        return "ServerPlayerState={id=" + id + ", body=" + body + ", hp=" + hp + ", color=" + color + ", x=" + x + ", y=" + y + "}";
    }
}
