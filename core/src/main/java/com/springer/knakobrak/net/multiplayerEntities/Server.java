package com.springer.knakobrak.net.multiplayerEntities;

import com.springer.knakobrak.net.messages.NetMessage;
import com.springer.knakobrak.util.ServerMessage;

public interface Server extends Runnable {
    void enqueue(ServerMessage sm);
    void broadcast(NetMessage msg);
    void shutdown();
}
