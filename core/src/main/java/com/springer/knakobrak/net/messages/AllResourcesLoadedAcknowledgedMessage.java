package com.springer.knakobrak.net.messages;

// SENT FROM SERVER
// Sent from the server to acknowledge the fact that a particular client is ready for the game
// Acts as a response to the AllResourcesLoadedMessage
public class AllResourcesLoadedAcknowledgedMessage extends NetMessage {

    public long serverStartTime;
    public float x = 0, y = 0;

    public AllResourcesLoadedAcknowledgedMessage() {}
}
