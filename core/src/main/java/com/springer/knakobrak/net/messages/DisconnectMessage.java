package com.springer.knakobrak.net.messages;

public class DisconnectMessage extends NetMessage {

    public int playerId;
    public String reason;

    public DisconnectMessage() {
    }
}
