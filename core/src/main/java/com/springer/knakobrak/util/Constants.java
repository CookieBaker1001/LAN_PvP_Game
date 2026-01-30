package com.springer.knakobrak.util;

public final class Constants {

    private Constants() {}

    public static final float PIXELS_PER_METER = 25f;// pixels per meter

    public static float pxToMeters(float px) {
        return px / PIXELS_PER_METER;
    }

    public static float metersToPx(float m) {
        return m * PIXELS_PER_METER;
    }


    public static final float PLAYER_RADIUS_PX = 25f;
    public static final float BULLET_RADIUS_PX = 10f;

    public static final float PLAYER_RADIUS_M =
        PLAYER_RADIUS_PX / PIXELS_PER_METER;

    public static final float BULLET_RADIUS_M =
        BULLET_RADIUS_PX / PIXELS_PER_METER;


    public static final float PLAYER_SPEED_MPS = 6.0f;
    public static final float BULLET_SPEED_MPS = 18.0f;


    public static final float COLLISION_DISTANCE_M =
        PLAYER_RADIUS_M + BULLET_RADIUS_M;

}
