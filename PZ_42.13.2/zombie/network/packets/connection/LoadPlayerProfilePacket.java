// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.ServerPlayerDB;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class LoadPlayerProfilePacket implements INetworkPacket {
    @JSONField
    boolean isExist;
    @JSONField
    byte playerIndex;
    @JSONField
    float x;
    @JSONField
    float y;
    @JSONField
    float z;
    @JSONField
    int worldVersion;
    @JSONField
    boolean isDead;
    byte[] buffer;

    @Override
    public void write(ByteBufferWriter b) {
        if (GameClient.client) {
            b.putByte(this.playerIndex);
        } else if (this.isExist) {
            b.putByte((byte)1);
            b.putByte(this.playerIndex);
            b.putFloat(this.x);
            b.putFloat(this.y);
            b.putFloat(this.z);
            b.putInt(this.worldVersion);
            b.putByte((byte)(this.isDead ? 1 : 0));
            b.putInt(this.buffer.length);
            b.bb.put(this.buffer);
        } else {
            b.putByte((byte)0);
            b.putByte(this.playerIndex);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (GameServer.server) {
            this.playerIndex = b.get();
        } else {
            this.isExist = b.get() == 1;
            this.playerIndex = b.get();
            if (this.isExist) {
                this.x = b.getFloat();
                this.y = b.getFloat();
                this.z = b.getFloat();
                this.worldVersion = b.getInt();
                this.isDead = b.get() == 1;
                int size = b.getInt();
                this.buffer = new byte[size];
                b.get(this.buffer);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ClientPlayerDB.NetworkCharacterProfile networkProfile = ClientPlayerDB.getInstance().networkProfile;
        if (this.isExist) {
            if (networkProfile != null) {
                networkProfile.playerCount++;
                switch (networkProfile.playerCount) {
                    case 2:
                        networkProfile.worldVersion[1] = this.worldVersion;
                        networkProfile.character[1] = this.buffer;
                        networkProfile.x[1] = this.x;
                        networkProfile.y[1] = this.y;
                        networkProfile.z[1] = this.z;
                        networkProfile.isDead[1] = this.isDead;
                        break;
                    case 3:
                        networkProfile.worldVersion[2] = this.worldVersion;
                        networkProfile.character[2] = this.buffer;
                        networkProfile.x[2] = this.x;
                        networkProfile.y[2] = this.y;
                        networkProfile.z[2] = this.z;
                        networkProfile.isDead[2] = this.isDead;
                        break;
                    case 4:
                        networkProfile.worldVersion[3] = this.worldVersion;
                        networkProfile.character[3] = this.buffer;
                        networkProfile.x[3] = this.x;
                        networkProfile.y[3] = this.y;
                        networkProfile.z[3] = this.z;
                        networkProfile.isDead[3] = this.isDead;
                }
            } else {
                networkProfile = new ClientPlayerDB.NetworkCharacterProfile();
                ClientPlayerDB.getInstance().networkProfile = networkProfile;
                networkProfile.playerCount = 1;
                networkProfile.username = GameClient.username;
                networkProfile.server = GameClient.ip;
                networkProfile.character[0] = this.buffer;
                networkProfile.worldVersion[0] = this.worldVersion;
                networkProfile.x[0] = this.x;
                networkProfile.y[0] = this.y;
                networkProfile.z[0] = this.z;
                networkProfile.isDead[0] = this.isDead;
            }

            this.playerIndex++;
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.LoadPlayerProfile.doPacket(b);
            this.write(b);
            PacketTypes.PacketType.LoadPlayerProfile.send(GameClient.connection);
        } else if (networkProfile != null) {
            networkProfile.isLoaded = true;
        } else {
            networkProfile = new ClientPlayerDB.NetworkCharacterProfile();
            ClientPlayerDB.getInstance().networkProfile = networkProfile;
            networkProfile.isLoaded = true;
            networkProfile.playerCount = 0;
            networkProfile.username = GameClient.username;
            networkProfile.server = GameClient.ip;
            networkProfile.character[0] = null;
            networkProfile.worldVersion[0] = IsoWorld.getWorldVersion();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.playerIndex < 0 || this.playerIndex >= 4) {
            this.isExist = false;
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.LoadPlayerProfile.doPacket(b);
            this.write(b);
            PacketTypes.PacketType.LoadPlayerProfile.send(connection);
        } else if (ServerPlayerDB.getInstance().conn != null) {
            if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                ServerPlayerDB.getInstance().serverConvertNetworkCharacter(connection.username, connection.idStr);
            }

            String sqlSelect;
            if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                sqlSelect = "SELECT id, x, y, z, data, worldversion, isDead FROM networkPlayers WHERE steamid=? AND world=? AND playerIndex=?";
            } else {
                sqlSelect = "SELECT id, x, y, z, data, worldversion, isDead FROM networkPlayers WHERE username=? AND world=? AND playerIndex=?";
            }

            try (PreparedStatement pstmt = ServerPlayerDB.getInstance().conn.prepareStatement(sqlSelect)) {
                if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                    pstmt.setString(1, connection.idStr);
                } else {
                    pstmt.setString(1, connection.username);
                }

                pstmt.setString(2, Core.gameSaveWorld);
                pstmt.setInt(3, this.playerIndex);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    this.isExist = true;
                    this.x = rs.getFloat(2);
                    this.y = rs.getFloat(3);
                    this.z = rs.getFloat(4);
                    this.buffer = rs.getBytes(5);
                    this.worldVersion = rs.getInt(6);
                    this.isDead = rs.getBoolean(7);
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.LoadPlayerProfile.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.LoadPlayerProfile.send(connection);
                } else {
                    this.isExist = false;
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.LoadPlayerProfile.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.LoadPlayerProfile.send(connection);
                }
            } catch (SQLException var9) {
                ExceptionLogger.logException(var9);
            }
        }
    }
}
