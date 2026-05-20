package com.springer.knakobrak.net.messages;

public class PlayerListMessage extends NetMessage {
    public int hostId;
    public int[] ids;
    public String[] names;
    public int[] pings;
    public PlayerListMessage() {}
}
