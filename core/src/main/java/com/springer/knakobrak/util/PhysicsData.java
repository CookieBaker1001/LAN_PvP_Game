package com.springer.knakobrak.util;

public class PhysicsData {
    public final ObjectType type;
    public int clientId;
    public int counter;

    public PhysicsData(ObjectType type, int clientId, int counter) {
        this.type = type;
        this.clientId = clientId;
        this.counter = counter;
    }

    @Override
    public String toString() {
        return "type: " + type.ordinal() + ", clientId: " + clientId + ", counter: " + counter;
    }
}
