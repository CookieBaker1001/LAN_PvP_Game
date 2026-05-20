package com.springer.knakobrak.util;

import java.util.PriorityQueue;

public class IdPool {
    private final PriorityQueue<Integer> freeIds;
    private int connectedPlayers;

    public IdPool() {
        freeIds = new PriorityQueue<>();
        connectedPlayers = 0;
        for (int i = 0; i < 4; i++) {
            freeIds.offer(i);
        }
    }

    public int acquire() {
        if (freeIds.isEmpty()) return -1;
        connectedPlayers++;
        return freeIds.poll();
    }

    public void release(int id) {
        freeIds.offer(id);
        connectedPlayers--;
    }

    public int getConnectedPlayers() {
        return connectedPlayers;
    }
}
