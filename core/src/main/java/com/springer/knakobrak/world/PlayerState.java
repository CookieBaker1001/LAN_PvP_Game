package com.springer.knakobrak.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Body;

import java.io.Serializable;

public class PlayerState implements Serializable {
    public int id;
    public String name;
    public Body body;
    public int playerIcon;
    public int ballIcon;
    public int hp = 3;
    public Color color = Color.WHITE;
    public float x, y;

    @Override
    public String toString () {
        return "PlayerState{id=" + id + ", color=" + color + ", x=" + x + ", y=" + y + "}";
    }
}
