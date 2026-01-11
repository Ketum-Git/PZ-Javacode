// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class UpdateOverlaySpritePacket implements INetworkPacket {
    @JSONField
    NetObject netObject = new NetObject();
    @JSONField
    String spriteName;
    @JSONField
    float colorR;
    @JSONField
    float colorG;
    @JSONField
    float colorB;
    @JSONField
    float colorA;

    @Override
    public void setData(Object... values) {
        this.netObject.setObject((IsoObject)values[0]);
        this.spriteName = (String)values[1];
        this.colorR = (Float)values[2];
        this.colorG = (Float)values[3];
        this.colorB = (Float)values[4];
        this.colorA = (Float)values[5];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.netObject.write(b);
        GameWindow.WriteStringUTF(b.bb, this.spriteName);
        b.putFloat(this.colorR);
        b.putFloat(this.colorG);
        b.putFloat(this.colorB);
        b.putFloat(this.colorA);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.netObject.parse(b, connection);
        this.spriteName = GameWindow.ReadStringUTF(b);
        this.colorR = b.getFloat();
        this.colorG = b.getFloat();
        this.colorB = b.getFloat();
        this.colorA = b.getFloat();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.netObject.getObject() != null) {
            this.netObject.getObject().setOverlaySprite(this.spriteName, this.colorR, this.colorG, this.colorB, this.colorA, false);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.netObject.getObject() != null
            && this.netObject.getObject().setOverlaySprite(this.spriteName, this.colorR, this.colorG, this.colorB, this.colorA, false)) {
            GameServer.updateOverlayForClients(this.netObject.getObject(), this.spriteName, this.colorR, this.colorG, this.colorB, this.colorA, connection);
        }
    }
}
