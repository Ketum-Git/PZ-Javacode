// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class CustomColorPacket implements INetworkPacket {
    @JSONField
    int squareX;
    @JSONField
    int squareY;
    @JSONField
    byte squareZ;
    @JSONField
    int index;
    @JSONField
    ColorInfo colorInfo;

    @Override
    public void setData(Object... values) {
        if (values.length != 1 && !(values[0] instanceof IsoObject)) {
            DebugLog.Multiplayer.error(this.getClass().getSimpleName() + ".set get invalid arguments");
        } else {
            IsoObject isoObject = (IsoObject)values[0];
            this.squareX = isoObject.getSquare().getX();
            this.squareY = isoObject.getSquare().getY();
            this.squareZ = (byte)isoObject.getSquare().getZ();
            this.index = isoObject.getSquare().getObjects().indexOf(isoObject);
            this.colorInfo = isoObject.getCustomColor();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.squareX);
        b.putInt(this.squareY);
        b.putInt(this.squareZ);
        b.putInt(this.index);
        b.putFloat(this.colorInfo.r);
        b.putFloat(this.colorInfo.g);
        b.putFloat(this.colorInfo.b);
        b.putFloat(this.colorInfo.a);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        this.squareX = bb.getInt();
        this.squareY = bb.getInt();
        this.squareZ = bb.get();
        this.index = bb.getInt();
        float r = bb.getFloat();
        float g = bb.getFloat();
        float b = bb.getFloat();
        float a = bb.getFloat();
        if (this.colorInfo == null) {
            this.colorInfo = new ColorInfo();
        }

        this.colorInfo.set(r, g, b, a);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
        if (sq != null && this.index < sq.getObjects().size()) {
            IsoObject o = sq.getObjects().get(this.index);
            if (o != null) {
                o.setCustomColor(this.colorInfo);
            }
        }
    }
}
