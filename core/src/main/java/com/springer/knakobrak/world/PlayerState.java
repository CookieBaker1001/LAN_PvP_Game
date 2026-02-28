package com.springer.knakobrak.world;

import com.badlogic.gdx.physics.box2d.Body;

import java.io.Serializable;

import static com.springer.knakobrak.util.Constants.MAX_HEALTH;

public class PlayerState implements Serializable {
    public int id;
    public String name;
    public Body body;
    public int playerIcon;
    public int ballIcon;
    public int hp = MAX_HEALTH;
    public float x, y;

    @Override
    public String toString () {
        return "PlayerState{id=" + id + ", x=" + x + ", y=" + y + "}";
    }
}
