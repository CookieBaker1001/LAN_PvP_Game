package com.springer.knakobrak.world;

public class ProjectileId {
    public int clientId;
    public int counter;

    public ProjectileId(int clientId, int counter) {
        this.clientId = clientId;
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectileId)) return false;
        ProjectileId other = (ProjectileId) o;
        return clientId == other.clientId && counter == other.counter;
    }

    @Override
    public int hashCode() {
        return 31 * clientId + counter;
    }

    @Override
    public String toString() {
        return "(" + clientId + "," + counter + ")";
    }
}
