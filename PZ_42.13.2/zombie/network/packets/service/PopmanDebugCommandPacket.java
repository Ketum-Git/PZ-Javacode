// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.DebugCommands;

@PacketSetting(ordering = 0, priority = 1, reliability = 0, requiredCapability = Capability.ConnectWithDebug, handlingType = 3)
public class PopmanDebugCommandPacket implements INetworkPacket {
    byte packetType;
    short cellX;
    short cellY;

    @Override
    public void processClient(UdpConnection connection) {
        ByteBufferWriter bbw = GameClient.connection.startPacket();
        PacketTypes.PacketType.PopmanDebugCommand.doPacket(bbw);
        this.write(bbw);
        PacketTypes.PacketType.PopmanDebugCommand.send(GameClient.connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        DebugCommands.n_debugCommand(this.packetType, this.cellX, this.cellY);
    }

    public void setSpawnTimeToZero(short cellX, short cellY) {
        this.packetType = 3;
        this.cellX = cellX;
        this.cellY = cellY;
    }

    public void setClearZombies(short cellX, short cellY) {
        this.packetType = 4;
        this.cellX = cellX;
        this.cellY = cellY;
    }

    public void setSpawnNow(short cellX, short cellY) {
        this.packetType = 5;
        this.cellX = cellX;
        this.cellY = cellY;
    }

    public void set(byte packetType, short cellX, short cellY) {
        this.packetType = packetType;
        this.cellX = cellX;
        this.cellY = cellY;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.packetType = b.get();
        this.cellX = b.getShort();
        this.cellY = b.getShort();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.packetType);
        b.putShort(this.cellX);
        b.putShort(this.cellY);
    }
}
