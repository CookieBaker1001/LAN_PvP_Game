package com.springer.knakobrak.net.messages;

// SENT FROM CLIENT
// Sent from the client as a response to the 'PingMessage'.
public class PingResponseMessage extends NetMessage {
    public int clientId;
    public int sequenceResponse;
    public long pingTimeResponse;
    public PingResponseMessage() {}
}
