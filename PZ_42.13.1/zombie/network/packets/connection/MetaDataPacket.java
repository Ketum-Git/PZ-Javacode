// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.None, handlingType = 6)
public class MetaDataPacket implements INetworkPacket {
    @JSONField
    String username = "";

    @Override
    public void setData(Object... values) {
        this.username = (String)values[0];
    }

    @Override
    public void write(ByteBufferWriter b) {
        LinkedList<SafeHouse> saveHousesForSend = new LinkedList<>();

        for (int n = 0; n < SafeHouse.getSafehouseList().size(); n++) {
            SafeHouse sh = SafeHouse.getSafehouseList().get(n);
            if (GameServer.getPlayerByUserName(this.username) != null) {
                saveHousesForSend.add(sh);
            }
        }

        b.bb.putInt(saveHousesForSend.size());

        for (SafeHouse sh : saveHousesForSend) {
            sh.save(b.bb);
        }

        b.bb.putInt(NonPvpZone.getAllZones().size());

        for (int nx = 0; nx < NonPvpZone.getAllZones().size(); nx++) {
            NonPvpZone.getAllZones().get(nx).save(b.bb);
        }

        b.bb.putInt(Faction.getFactions().size());

        for (int nx = 0; nx < Faction.getFactions().size(); nx++) {
            Faction.getFactions().get(nx).save(b.bb);
        }

        b.bb.putInt(DesignationZone.allZones.size());

        for (int nx = 0; nx < DesignationZone.allZones.size(); nx++) {
            DesignationZone.allZones.get(nx).save(b.bb);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        SafeHouse.clearSafehouseList();
        int nSafehouse = b.getInt();

        for (int n = 0; n < nSafehouse; n++) {
            SafeHouse.load(b, 240);
        }

        NonPvpZone.nonPvpZoneList.clear();
        int nZone = b.getInt();

        for (int n = 0; n < nZone; n++) {
            NonPvpZone zone = new NonPvpZone();
            zone.load(b, 240);
            NonPvpZone.getAllZones().add(zone);
        }

        Faction.factions = new ArrayList<>();
        int nFaction = b.getInt();

        for (int n = 0; n < nFaction; n++) {
            Faction faction = new Faction();
            faction.load(b, 240);
            Faction.getFactions().add(faction);
        }

        int nDZone = b.getInt();

        for (int n = 0; n < nDZone; n++) {
            DesignationZone.load(b, 240);
        }
    }
}
