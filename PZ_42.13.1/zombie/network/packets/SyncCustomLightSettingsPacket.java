// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoLightSwitch;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncCustomLightSettingsPacket implements INetworkPacket {
    @JSONField
    NetObject netObject = new NetObject();
    @JSONField
    boolean activated;

    @Override
    public void setData(Object... values) {
        IsoLightSwitch obj = (IsoLightSwitch)values[0];
        this.netObject.setObject(obj);
        this.activated = obj.isActivated();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.netObject.write(b);
        IsoLightSwitch obj = (IsoLightSwitch)this.netObject.getObject();
        b.putBoolean(this.activated);
        b.putBoolean(obj.getCanBeModified());
        b.putBoolean(obj.getUseBattery());
        b.putBoolean(obj.getHasBattery());
        b.putByte((byte)(obj.getBulbItem() != null ? 1 : 0));
        if (obj.getBulbItem() != null) {
            GameWindow.WriteString(b.bb, obj.getBulbItem());
        }

        b.putFloat(obj.getPower());
        b.putFloat(obj.getDelta());
        b.putFloat(obj.getPrimaryR());
        b.putFloat(obj.getPrimaryG());
        b.putFloat(obj.getPrimaryB());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.netObject.parse(b, connection);
        IsoLightSwitch obj = (IsoLightSwitch)this.netObject.getObject();
        this.activated = b.get() == 1;
        if (obj != null) {
            obj.setActivated(this.activated);
            obj.setCanBeModified(b.get() == 1);
            obj.setUseBatteryDirect(b.get() == 1);
            obj.setHasBattery(b.get() == 1);
            if (b.get() == 1) {
                obj.setBulbItemRaw(GameWindow.ReadString(b));
            } else {
                obj.setBulbItemRaw(null);
            }

            obj.setPower(b.getFloat());
            obj.setDelta(b.getFloat());
            obj.setPrimaryR(b.getFloat());
            obj.setPrimaryG(b.getFloat());
            obj.setPrimaryB(b.getFloat());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoLightSwitch obj = (IsoLightSwitch)this.netObject.getObject();
        if (obj != null) {
            obj.switchLight(this.activated);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        INetworkPacket.sendToAll(PacketTypes.PacketType.SyncCustomLightSettings, connection, this);
        IsoLightSwitch obj = (IsoLightSwitch)this.netObject.getObject();
        obj.switchLight(this.activated);
    }
}
