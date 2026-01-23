// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RegisterZonePacket implements INetworkPacket {
    @JSONField
    String name;
    @JSONField
    String type;
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    int z;
    @JSONField
    int w;
    @JSONField
    int h;
    @JSONField
    int lastActionTimestamp;

    @Override
    public void setData(Object... values) {
        this.set((Zone)values[0]);
    }

    public void set(Zone zone) {
        this.name = zone.name;
        this.type = zone.type;
        this.x = zone.x;
        this.y = zone.y;
        this.z = zone.z;
        this.w = zone.w;
        this.h = zone.h;
        this.lastActionTimestamp = zone.getLastActionTimestamp();
    }

    @Override
    public void processClient(UdpConnection connection) {
        ArrayList<Zone> zones = IsoWorld.instance.getMetaGrid().getZonesAt(this.x, this.y, this.z);
        boolean found = false;

        for (Zone zone : zones) {
            if (this.type.equals(zone.getType())) {
                found = true;
                zone.setName(this.name);
                zone.setLastActionTimestamp(this.lastActionTimestamp);
            }
        }

        if (!found) {
            IsoWorld.instance.getMetaGrid().registerZone(this.name, this.type, this.x, this.y, this.z, this.w, this.h);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ArrayList<Zone> zones = IsoWorld.instance.getMetaGrid().getZonesAt(this.x, this.y, this.z);
        boolean found = false;

        for (Zone zone : zones) {
            if (this.type.equals(zone.getType())) {
                found = true;
                zone.setName(this.name);
                zone.setLastActionTimestamp(this.lastActionTimestamp);
            }
        }

        if (!found) {
            IsoWorld.instance.getMetaGrid().registerZone(this.name, this.type, this.x, this.y, this.z, this.w, this.h);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.name = GameWindow.ReadString(b);
        this.type = GameWindow.ReadString(b);
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getInt();
        this.w = b.getInt();
        this.h = b.getInt();
        this.lastActionTimestamp = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.name);
        b.putUTF(this.type);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.z);
        b.putInt(this.w);
        b.putInt(this.h);
        b.putInt(this.lastActionTimestamp);
    }
}
