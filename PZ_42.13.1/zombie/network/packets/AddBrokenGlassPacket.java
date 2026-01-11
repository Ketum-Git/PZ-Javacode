// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class AddBrokenGlassPacket implements INetworkPacket {
    int x;
    int y;
    byte z;

    public void set(IsoGridSquare sq) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null) {
            sq.addBrokenGlass();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null) {
            sq.addBrokenGlass();
        }
    }
}
