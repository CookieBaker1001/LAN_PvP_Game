package com.springer.knakobrak.util;

import com.springer.knakobrak.net.multiplayerEntities.ClientHandler;
import com.springer.knakobrak.net.messages.PlayerListMessage;

import java.util.Map;

public class PlayerListItem {
    public int id;
    public String name;
    public boolean isHost;

    public PlayerListItem(int id, String name, boolean isHost) {
        this.id = id;
        this.name = name;
        this.isHost = isHost;
    }

    public static PlayerListMessage constructPlayerList(Map<Integer, ClientHandler> map) {
        PlayerListMessage plm = new PlayerListMessage();
        plm.ids = new int[map.size()];
        plm.names = new String[map.size()];
        plm.pings = new int[map.size()];
        int i = 0;
        for (ClientHandler ch : map.values()) {
            plm.ids[i] = ch.id;
            if (ch.isHost) plm.hostId = ch.id;
            plm.names[i] = ch.username;
            plm.pings[i] = (int) ch.ping;
            i++;
        }
        return plm;
    }
}
