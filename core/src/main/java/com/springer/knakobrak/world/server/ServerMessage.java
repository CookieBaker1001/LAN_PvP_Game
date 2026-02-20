package com.springer.knakobrak.world.server;

import com.springer.knakobrak.net.ClientHandler;
import com.springer.knakobrak.net.messages.NetMessage;

public class ServerMessage {
    public ClientHandler sender;
    public NetMessage message;

    public ServerMessage() {
    }

    public ServerMessage(ClientHandler sender, NetMessage message) {
        this.sender = sender;
        this.message = message;
    }
}
