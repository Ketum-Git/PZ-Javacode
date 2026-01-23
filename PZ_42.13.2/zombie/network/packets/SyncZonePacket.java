// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.zones.Zone;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncZonePacket implements INetworkPacket {
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    int z;
    @JSONField
    String type;
    @JSONField
    int lastActionTimestamp;
    @JSONField
    DesignationZone designationZone;
    @JSONField
    boolean zoneAdded = false;

    @Override
    public void write(ByteBufferWriter b) {
        if (this.designationZone != null) {
            b.putByte((byte)1);
            b.putByte((byte)(this.zoneAdded ? 1 : 0));
            if (this.zoneAdded) {
                this.designationZone.save(b.bb);
            } else {
                b.putDouble(this.designationZone.getId());
            }
        } else {
            b.putByte((byte)0);
            b.putInt(this.x);
            b.putInt(this.y);
            b.putInt(this.z);
            b.putUTF(this.type);
            b.putInt(this.lastActionTimestamp);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        boolean isDesignationZone = b.get() != 0;
        if (isDesignationZone) {
            this.zoneAdded = b.get() != 0;
            if (this.zoneAdded) {
                this.designationZone = DesignationZone.load(b, IsoWorld.getWorldVersion());
            } else {
                double zoneID = b.getDouble();
                this.designationZone = DesignationZone.getZoneById(zoneID);
            }
        } else {
            this.x = b.getInt();
            this.y = b.getInt();
            this.z = b.getInt();
            this.type = GameWindow.ReadString(b);
            this.lastActionTimestamp = b.getInt();
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.designationZone != null) {
            if (!this.zoneAdded) {
                this.removeDesignationZone();
            }

            LuaEventManager.triggerEvent("OnDesignationZoneUpdatedNetwork");
        } else {
            Zone zone = null;

            for (Zone z : IsoWorld.instance.getMetaGrid().getZonesAt(this.x, this.y, this.z)) {
                if (this.type.equals(z.getType())) {
                    zone = z;
                }
            }

            if (zone != null) {
                zone.setLastActionTimestamp(this.lastActionTimestamp);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.designationZone != null) {
            this.sendToClients(PacketTypes.PacketType.SyncZone, connection);
            if (!this.zoneAdded) {
                this.removeDesignationZone();
            }
        }
    }

    @Override
    public void setData(Object... values) {
        if (values[0] instanceof Zone zone) {
            this.x = zone.getX();
            this.y = zone.getY();
            this.z = zone.getZ();
            this.type = zone.getType();
            this.lastActionTimestamp = zone.getLastActionTimestamp();
        } else if (values[0] instanceof DesignationZone zone) {
            this.designationZone = zone;
            this.zoneAdded = (Boolean)values[1];
        }
    }

    private void removeDesignationZone() {
        if (this.designationZone != null) {
            if (this.designationZone.type.equals("AnimalZone")) {
                DesignationZoneAnimal.removeZone((DesignationZoneAnimal)this.designationZone, false);
            } else {
                DesignationZone.removeZone(this.designationZone, false);
            }
        }
    }
}
