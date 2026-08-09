// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoWorld;
import zombie.iso.areas.NonPvpZone;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.CanSetupNonPVPZone, handlingType = 3)
public class SyncNonPvpZonePacket implements INetworkPacket {
    @JSONField
    private NonPvpZone zone;
    @JSONField
    private boolean doRemove;

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof NonPvpZone nonPvpZone && values[1] instanceof Boolean remove) {
            this.set(nonPvpZone, remove);
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(NonPvpZone zone, boolean doRemove) {
        this.zone = zone;
        this.doRemove = doRemove;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        INetworkPacket.sendToAll(PacketTypes.PacketType.SyncNonPvpZone, connection, this.zone, this.doRemove);
        this.process();
        DebugType.Multiplayer.debugln("ReceiveSyncNonPvpZone: %s", this.getDescription());
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.zone = new NonPvpZone();
        this.zone.load(b.bb, IsoWorld.getWorldVersion());
        this.doRemove = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.zone.save(b.bb);
        b.putBoolean(this.doRemove);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return !StringUtils.isNullOrEmpty(this.zone.getTitle());
    }

    @Override
    public String getDescription() {
        return String.format(
            "\"%s\" remove=%b size=%d (%d;%d) (%d;%d)",
            this.zone.getTitle(),
            this.doRemove,
            this.zone.getSize(),
            this.zone.getX(),
            this.zone.getY(),
            this.zone.getX2(),
            this.zone.getY2()
        );
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.process();
    }

    private void process() {
        if (this.doRemove) {
            NonPvpZone.getAllZones().removeIf(z -> z.getTitle().equals(this.zone.getTitle()));
        } else if (NonPvpZone.getZoneByTitle(this.zone.getTitle()) == null) {
            NonPvpZone.getAllZones().add(this.zone);
        }
    }
}
