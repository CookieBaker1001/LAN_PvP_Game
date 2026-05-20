package com.springer.knakobrak.util;

public enum ServerType {
    UNDEFINED,
    OPEN,
    LOBBIED;

    ServerType() {}

    public static ServerType getType(int value) {
        return (switch(value) {
            case 0 -> ServerType.UNDEFINED;
            case 1 -> ServerType.OPEN;
            case 2 -> ServerType.LOBBIED;
            default -> null;
        });
    }
}
