package com.osuserverlist.bjar.irc;

import com.osuserverlist.bjar.models.essentials.Player;
import com.osuserverlist.bjar.modules.packets.ServerPacketEngine.ServerPacket;

/**
 * A {@link Player} that is connected through the IRC gateway instead of the
 * osu! client. Server packets queued for this player are translated into IRC
 * protocol lines and written directly to the underlying socket.
 */
public class IrcPlayer extends Player {

    private final IrcClient client;

    public IrcPlayer(int id, String osuToken, IrcClient client) {
        super(id, false, osuToken);
        this.client = client;
    }

    public IrcClient getClient() {
        return client;
    }

    @Override
    public void sendPacket(ServerPacket packet) {
        client.handleServerPacket(packet);
    }
}
