// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.SandboxOptions;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.worldMap.WorldMapVisited;
import zombie.worldMap.WorldMapVisitedServer;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 7)
public class PlayerVisitedPacket implements INetworkPacket {
    private static final int MAX_PACKET_SIZE = 1000000;
    private static final int DATA_CHUNK_SIZE = 999000;
    @JSONField
    private byte index;
    @JSONField
    private byte[] data;

    @Override
    public void setData(Object... values) {
        if (GameServer.server) {
            this.index = (Byte)values[0];
            this.data = (byte[])values[1];
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (GameClient.client) {
            this.index = b.getByte();
            int length = b.getInt();
            this.data = new byte[length];
            b.get(this.data, 0, length);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (GameServer.server) {
            b.putByte(this.index);
            b.putInt(this.data.length);
            b.put(this.data);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        WorldMapVisited instance = WorldMapVisited.getInstance(false);
        instance.processDataChunk(this.index * 999000, this.data);
        if (SandboxOptions.getInstance().map.mapAllKnown.getValue()) {
            IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
            instance.setKnownInCells(metaGrid.minX, metaGrid.minY, metaGrid.maxX, metaGrid.maxY);
        }
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        WorldMapVisitedServer.getInstance().loadUser(connection);
    }

    public static void HandleSendPacket(byte[] entity, IConnection connection) {
        byte index = 0;

        for (boolean isFilled = false; !isFilled; index++) {
            int chunkSize = Math.min(999000, entity.length - index * 999000);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(entity, index * 999000, chunk, 0, chunkSize);
            INetworkPacket.send(connection, PacketTypes.PacketType.PlayerVisitedPacket, index, chunk);
            isFilled = entity.length - index * 999000 - chunkSize == 0;
        }
    }

    public static void requestInitialData() {
        INetworkPacket.send(PacketTypes.PacketType.PlayerVisitedPacket);
    }
}
