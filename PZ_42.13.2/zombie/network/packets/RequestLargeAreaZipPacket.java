// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunkMap;
import zombie.iso.Vector3;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.popman.ZombiePopulationManager;

@PacketSetting(ordering = 4, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class RequestLargeAreaZipPacket implements INetworkPacket {
    @JSONField
    private int wx;
    @JSONField
    private int wy;
    @JSONField
    private int chunkMapWidth;

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof Integer && values[1] instanceof Integer) {
            this.set((Integer)values[0], (Integer)values[1]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(int wx, int wy) {
        this.wx = wx;
        this.wy = wy;
        this.chunkMapWidth = IsoChunkMap.chunkGridWidth;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.wx);
        b.putInt(this.wy);
        b.putInt(this.chunkMapWidth);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.wx = b.getInt();
        this.wy = b.getInt();
        this.chunkMapWidth = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.wasInLoadingQueue) {
            GameServer.kick(connection, "UI_Policy_Kick", "The server received an invalid request");
        }

        if (connection.playerDownloadServer != null) {
            connection.connectArea[0] = new Vector3(this.wx, this.wy, this.chunkMapWidth);
            connection.chunkGridWidth = this.chunkMapWidth;
            ZombiePopulationManager.instance.updateLoadedAreas();
        }
    }
}
