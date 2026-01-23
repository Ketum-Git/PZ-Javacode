// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;

public class PlayerID extends IDShort implements INetworkPacketField, IPositional {
    protected IsoPlayer player;
    @JSONField
    protected byte playerIndex = -1;

    public void set(IsoPlayer player) {
        this.setID(player.onlineId);
        this.playerIndex = player.isLocal() ? (byte)player.getPlayerNum() : -1;
        this.player = player;
    }

    public void clear() {
        this.setID((short)-1);
        this.playerIndex = -1;
        this.player = null;
    }

    private void parsePlayer(UdpConnection connection) {
        if (GameServer.server) {
            if (connection != null && this.playerIndex != -1) {
                this.player = GameServer.getPlayerFromConnection(connection, this.playerIndex);
            } else {
                this.player = GameServer.IDToPlayerMap.get(this.getID());
            }
        } else if (GameClient.client) {
            this.player = GameClient.IDToPlayerMap.get(this.getID());
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.playerIndex = b.get();
        this.parsePlayer(connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putByte(this.playerIndex);
    }

    @Override
    public void write(ByteBuffer bb) {
        super.write(bb);
        bb.put(this.playerIndex);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.getPlayer() != null;
    }

    @Override
    public String toString() {
        return this.player == null ? "?" : "(" + this.player.getOnlineID() + ")";
    }

    public IsoPlayer getPlayer() {
        return this.player;
    }

    public void copy(PlayerID other) {
        this.setID(other.getID());
        this.player = other.player;
        this.playerIndex = other.playerIndex;
    }

    @Override
    public float getX() {
        return this.player != null ? this.player.getX() : 0.0F;
    }

    @Override
    public float getY() {
        return this.player != null ? this.player.getY() : 0.0F;
    }

    @Override
    public float getZ() {
        return this.player != null ? this.player.getZ() : 0.0F;
    }

    public byte getPlayerIndex() {
        return this.playerIndex;
    }
}
