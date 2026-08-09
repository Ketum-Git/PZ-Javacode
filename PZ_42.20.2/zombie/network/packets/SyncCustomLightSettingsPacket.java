// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoLightSwitch;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.util.Type;

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
        if (b.putBoolean(obj.getBulbItem() != null)) {
            b.putUTF(obj.getBulbItem());
        }

        b.putFloat(obj.getPower());
        b.putFloat(obj.getDelta());
        b.putFloat(obj.getPrimaryR());
        b.putFloat(obj.getPrimaryG());
        b.putFloat(obj.getPrimaryB());
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.netObject.parse(b, connection);
        IsoLightSwitch obj = (IsoLightSwitch)this.netObject.getObject();
        this.activated = b.getBoolean();
        if (obj != null) {
            obj.setActivated(this.activated);
            obj.setCanBeModified(b.getBoolean());
            obj.setUseBatteryDirect(b.getBoolean());
            obj.setHasBattery(b.getBoolean());
            if (b.getBoolean()) {
                obj.setBulbItemRaw(b.getUTF());
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
        IsoLightSwitch obj = Type.tryCastTo(this.netObject.getObject(), IsoLightSwitch.class);
        if (obj != null) {
            obj.switchLight(this.activated);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClients(PacketTypes.PacketType.SyncCustomLightSettings, connection);
        this.processClient(connection);
    }
}
