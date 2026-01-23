// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ScoreboardUpdatePacket implements INetworkPacket {
    private final ArrayList<String> usernames = new ArrayList<>();
    private final ArrayList<String> displayNames = new ArrayList<>();
    private final ArrayList<String> steamIdsString = new ArrayList<>();
    private final ArrayList<Long> steamIdsLong = new ArrayList<>();

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.usernames.clear();
        this.displayNames.clear();
        this.steamIdsString.clear();
        this.steamIdsLong.clear();
        if (GameClient.client) {
            int count = b.getInt();
            GameClient.instance.connectedPlayers.clear();

            for (int i = 0; i < count; i++) {
                String username = GameWindow.ReadString(b);
                String displayName = GameWindow.ReadString(b);
                this.usernames.add(username);
                this.displayNames.add(displayName);
                GameClient.instance.connectedPlayers.add(GameClient.instance.getPlayerFromUsername(username));
                if (SteamUtils.isSteamModeEnabled()) {
                    long steamId = b.getLong();
                    this.steamIdsLong.add(steamId);
                    this.steamIdsString.add(SteamUtils.convertSteamIDToString(steamId));
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (GameServer.server) {
            b.putInt(this.usernames.size());

            for (int n = 0; n < this.usernames.size(); n++) {
                b.putUTF(this.usernames.get(n));
                b.putUTF(this.displayNames.get(n));
                if (SteamUtils.isSteamModeEnabled()) {
                    b.putLong(this.steamIdsLong.get(n));
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("OnScoreboardUpdate", this.usernames, this.displayNames, this.steamIdsString);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        boolean scoreboardCapability = connection.role != null && connection.role.hasCapability(Capability.SeePlayersConnected);
        boolean steamScoreboardCapability = connection.role != null && connection.role.hasCapability(Capability.GetSteamScoreboard);
        boolean isSteamScoreboardEnabled = ServerOptions.getInstance().steamScoreboard.getValue();

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.isFullyConnected()) {
                boolean doHideAdmins = ServerOptions.getInstance().hideAdminsInPlayerList.getValue() && c.role.hasCapability(Capability.HideFromSteamUserList);
                boolean doHidePlayers = ServerOptions.getInstance().disableScoreboard.getValue() && c.getConnectedGUID() != connection.getConnectedGUID();
                if (scoreboardCapability || !doHidePlayers && !doHideAdmins) {
                    for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                        if (c.usernames[playerIndex] != null) {
                            String username = c.usernames[playerIndex];
                            IsoPlayer p = GameServer.getPlayerByRealUserName(username);
                            String displayName;
                            if (p != null) {
                                displayName = p.getDisplayName();
                            } else {
                                displayName = ServerWorldDatabase.instance.getDisplayName(c.usernames[playerIndex]);
                            }

                            if (displayName == null) {
                                displayName = username;
                            } else if (!scoreboardCapability) {
                                username = displayName;
                            }

                            this.usernames.add(username);
                            this.displayNames.add(displayName);
                            if (SteamUtils.isSteamModeEnabled()) {
                                long steamId;
                                if (!isSteamScoreboardEnabled && !steamScoreboardCapability) {
                                    steamId = -1L;
                                } else {
                                    steamId = c.steamId;
                                }

                                this.steamIdsLong.add(steamId);
                            }
                        }
                    }
                }
            }
        }

        this.sendToClient(PacketTypes.PacketType.ScoreboardUpdate, connection);
    }
}
