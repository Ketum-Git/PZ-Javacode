// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.BodyDamageSync;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 5, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class BodyDamageUpdatePacket implements INetworkPacket {
    @JSONField
    private BodyDamageUpdatePacket.Type packetType = BodyDamageUpdatePacket.Type.START_UPDATING;
    @JSONField
    private final PlayerID currentPlayer = new PlayerID();
    @JSONField
    private final PlayerID remotePlayer = new PlayerID();
    private ByteBuffer data;

    public void setStart(IsoPlayer remotePlayer) {
        this.packetType = BodyDamageUpdatePacket.Type.START_UPDATING;
        this.currentPlayer.set(IsoPlayer.players[0]);
        this.remotePlayer.set(remotePlayer);
        DebugLog.Multiplayer.noise("start receiving updates from " + this.remotePlayer.getDescription() + " to " + this.currentPlayer.getDescription());
    }

    public void setStop(IsoPlayer remotePlayer) {
        this.packetType = BodyDamageUpdatePacket.Type.STOP_UPDATING;
        this.currentPlayer.set(IsoPlayer.players[0]);
        this.remotePlayer.set(remotePlayer);
        DebugLog.Multiplayer.noise("stop receiving updates from " + this.remotePlayer.getDescription() + " to " + this.currentPlayer.getDescription());
    }

    public void setUpdate(IsoPlayer remotePlayer, IsoPlayer requester, ByteBuffer inputData) {
        this.packetType = BodyDamageUpdatePacket.Type.UPDATE;
        this.currentPlayer.set(requester);
        this.remotePlayer.set(remotePlayer);
        this.data = ByteBuffer.allocate(inputData.position());
        this.data.put(inputData.array(), 0, inputData.position());
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.packetType.ordinal());
        this.currentPlayer.write(b);
        this.remotePlayer.write(b);
        if (this.packetType == BodyDamageUpdatePacket.Type.UPDATE) {
            this.data.position(0);
            b.bb.put(this.data);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.packetType = BodyDamageUpdatePacket.Type.values()[b.get()];
        this.currentPlayer.parse(b, connection);
        this.remotePlayer.parse(b, connection);
        if (this.packetType == BodyDamageUpdatePacket.Type.UPDATE) {
            this.data = ByteBuffer.allocate(b.limit() - b.position());
            this.data.position(0);
            this.data.put(b);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.packetType == BodyDamageUpdatePacket.Type.START_UPDATING) {
            BodyDamageSync.instance.startSendingUpdates((short)this.currentPlayer.getPlayer().getPlayerNum(), this.currentPlayer.getID());
        } else if (this.packetType == BodyDamageUpdatePacket.Type.STOP_UPDATING) {
            BodyDamageSync.instance.stopSendingUpdates((short)this.currentPlayer.getPlayer().getPlayerNum(), this.currentPlayer.getID());
        } else {
            if (this.packetType == BodyDamageUpdatePacket.Type.UPDATE) {
                this.data.position(0);
                BodyDamage bodyDamage = this.remotePlayer.getPlayer().getBodyDamageRemote();
                byte bd = this.data.get();
                if (bd == 50) {
                    bodyDamage.setOverallBodyHealth(this.data.getFloat());
                    bodyDamage.setRemotePainLevel(this.data.get());
                    bodyDamage.isFakeInfected = this.data.get() == 1;
                    this.remotePlayer.getPlayer().getStats().set(CharacterStat.ZOMBIE_INFECTION, this.data.getFloat());
                    bd = this.data.get();
                }

                while (bd == 64) {
                    int partIndex = this.data.get();
                    BodyPart part = bodyDamage.getBodyParts().get(partIndex);

                    for (byte id = this.data.get(); id != 65; id = this.data.get()) {
                        part.sync(this.data, id);
                    }

                    bd = this.data.get();
                }
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.packetType == BodyDamageUpdatePacket.Type.START_UPDATING) {
            UdpConnection connection2 = GameServer.getConnectionFromPlayer(this.remotePlayer.getPlayer());
            if (connection2 != null) {
                ByteBufferWriter b = connection2.startPacket();
                PacketTypes.PacketType.BodyDamageUpdate.doPacket(b);
                this.write(b);
                PacketTypes.PacketType.BodyDamageUpdate.send(connection2);
            }
        } else if (this.packetType == BodyDamageUpdatePacket.Type.STOP_UPDATING) {
            UdpConnection connection2 = GameServer.getConnectionFromPlayer(this.remotePlayer.getPlayer());
            if (connection2 != null) {
                ByteBufferWriter b = connection2.startPacket();
                PacketTypes.PacketType.BodyDamageUpdate.doPacket(b);
                this.write(b);
                PacketTypes.PacketType.BodyDamageUpdate.send(connection2);
            }
        } else {
            if (this.packetType == BodyDamageUpdatePacket.Type.UPDATE) {
                UdpConnection connection2 = GameServer.getConnectionFromPlayer(this.currentPlayer.getPlayer());
                if (connection2 == null) {
                    return;
                }

                ByteBufferWriter b = connection2.startPacket();
                PacketTypes.PacketType.BodyDamageUpdate.doPacket(b);
                this.write(b);
                this.data.position(0);
                b.bb.put(this.data);
                PacketTypes.PacketType.BodyDamageUpdate.send(connection2);
            }
        }
    }

    private static enum Type {
        START_UPDATING,
        STOP_UPDATING,
        UPDATE;
    }
}
