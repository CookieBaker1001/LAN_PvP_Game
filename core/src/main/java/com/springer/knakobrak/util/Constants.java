package com.springer.knakobrak.util;

public final class Constants {

    private Constants() {}

    public static final float PIXELS_PER_METER = 25f;// pixels per meter

    public static final float WORLD_HEIGHT = 200f;
    public static final float WORLD_WIDTH = 200f;

    public static float pxToMeters(float px) {
        return px / PIXELS_PER_METER;
    }

    public static float metersToPx(float m) {
        return m * PIXELS_PER_METER;
    }


    public static final float PLAYER_RADIUS_PX = 25f;
    public static final float BULLET_RADIUS_PX = 15f;

    public static final float PLAYER_RADIUS_M =
        PLAYER_RADIUS_PX / PIXELS_PER_METER;

    public static final float BULLET_RADIUS_M =
        BULLET_RADIUS_PX / PIXELS_PER_METER;


    public static final float PLAYER_SPEED_MPS = 4.0f;
    public static final float BULLET_SPEED_MPS = 20.0f;


    public static final float COLLISION_DISTANCE_M =
        PLAYER_RADIUS_M + BULLET_RADIUS_M;

}
