package com.osuserverlist.bjar.packets.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.osuserverlist.bjar.App;
import com.osuserverlist.bjar.Server;
import com.osuserverlist.bjar.models.essentials.Channel;
import com.osuserverlist.bjar.models.essentials.Match;
import com.osuserverlist.bjar.models.essentials.Player;
import com.osuserverlist.bjar.modules.packets.BanchoPacketReader;
import com.osuserverlist.bjar.modules.packets.ClientPacketEngine.ClientPacket;
import com.osuserverlist.bjar.modules.packets.ClientPacketEngine.ClientPackets;
import com.osuserverlist.bjar.packets.BanchoPacket;
import com.osuserverlist.bjar.packets.server.ChatServerPackets.ChannelInfoPacket;
import com.osuserverlist.bjar.packets.server.ChatServerPackets.ChannelJoinSuccessPacket;
import com.osuserverlist.bjar.packets.server.ChatServerPackets.ChannelRevokedPacket;
import com.osuserverlist.bjar.packets.server.MultiplayerServerPackets.MatchUpdatePacket;

/**
 * Handlers for the osu! tournament (osu!tourney) manager client.
 *
 * The tournament manager joins a match's chat channel as a referee (without
 * occupying a slot) and polls live match state; its spectator sub-clients use
 * the regular spectating flow. These three packets cover the manager side.
 */
public class TournamentPackets {

    private static final Logger logger = LoggerFactory.getLogger(TournamentPackets.class);

    @ClientPacket(ClientPackets.TOURNAMENT_JOIN_MATCH_CHANNEL)
    public boolean joinMatchChannel(BanchoPacket packet, BanchoPacketReader reader, Player player) throws IOException {
        int matchId = reader.readInt();
        Server server = App.server;

        if (!player.isTourneyClient()) {
            return true;
        }

        Match match = server.matchManager.getById((short) matchId);
        if (match == null) {
            return true;
        }

        if (match.getReferees().contains(player)) {
            player.sendPacket(new MatchUpdatePacket(match));
            return true;
        }

        String channelName = "#multi_" + match.getMatchId();
        Channel channel = server.channelManager.get(channelName);
        if (channel == null) {
            logger.warn("Tournament client {} requested match {} with no channel", player.getUsername(), matchId);
            return true;
        }

        match.getReferees().add(player);
        server.channelManager.forceJoinChannel(channelName, player);

        player.sendPacket(new ChannelJoinSuccessPacket(channel.getAlias()));
        player.sendPacket(new ChannelInfoPacket(channel.getAlias(), channel.getDescription(),
                channel.getPlayerCount()));
        player.sendPacket(new MatchUpdatePacket(match));

        logger.info("Tournament client {} joined match channel {}", player.getUsername(), channelName);
        return true;
    }

    @ClientPacket(ClientPackets.TOURNAMENT_LEAVE_MATCH_CHANNEL)
    public boolean leaveMatchChannel(BanchoPacket packet, BanchoPacketReader reader, Player player) throws IOException {
        int matchId = reader.readInt();
        Server server = App.server;

        Match match = server.matchManager.getById((short) matchId);
        if (match == null) {
            return true;
        }

        if (!match.getReferees().remove(player)) {
            return true;
        }

        String channelName = "#multi_" + match.getMatchId();
        Channel channel = server.channelManager.get(channelName);
        if (channel != null) {
            server.channelManager.forceLeaveChannel(channelName, player);
            player.sendPacket(new ChannelRevokedPacket(channel.getAlias()));
        }

        logger.info("Tournament client {} left match channel {}", player.getUsername(), channelName);
        return true;
    }

    @ClientPacket(ClientPackets.TOURNAMENT_MATCH_INFO_REQUEST)
    public boolean matchInfoRequest(BanchoPacket packet, BanchoPacketReader reader, Player player) throws IOException {
        int matchId = reader.readInt();
        Server server = App.server;

        Match match = server.matchManager.getById((short) matchId);
        if (match == null) {
            return true;
        }

        player.sendPacket(new MatchUpdatePacket(match));
        return true;
    }
}
