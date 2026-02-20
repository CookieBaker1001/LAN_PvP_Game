package com.springer.knakobrak.net.messages;

public class JoinRejectedMessage extends NetMessage {
    // Sent from the server to let the client know that it is rejected

    public String reason;

    public JoinRejectedMessage() {
    }
}
