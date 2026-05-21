package com.springer.knakobrak.net.messages;

// SENT FROM SERVER
// Sent from the server to let the client know that it is rejected
public class JoinRejectedMessage extends NetMessage {
    public String reason;
    public JoinRejectedMessage() {}
}
