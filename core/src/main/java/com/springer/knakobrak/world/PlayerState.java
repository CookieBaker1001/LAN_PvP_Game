package com.springer.knakobrak.world;

import com.badlogic.gdx.graphics.Color;

public class PlayerState {
    public int id;
    public Color color = Color.WHITE;
    public float x, y;

    @Override
    public String toString () {
        return "PlayerState{id=" + id + ", color=" + color + ", x=" + x + ", y=" + y + "}";
    }
}
