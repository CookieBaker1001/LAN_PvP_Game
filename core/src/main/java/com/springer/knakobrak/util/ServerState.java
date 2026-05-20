package com.springer.knakobrak.util;

public enum ServerState {
    LOBBY,
    LOADING,
    GAME;

    ServerState() {}

    public static ServerState getType(int value) {
        return (switch(value) {
            case 0 -> ServerState.LOBBY;
            case 1 -> ServerState.LOADING;
            case 2 -> ServerState.GAME;
            default -> null;
        });
    }
}
