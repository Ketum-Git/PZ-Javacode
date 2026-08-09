// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import gnu.trove.iterator.TShortShortIterator;
import gnu.trove.map.hash.TShortShortHashMap;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehicleTowingStatePacket implements INetworkPacket {
    @JSONField
    protected final TShortShortHashMap towedVehicleMap = new TShortShortHashMap();

    @Override
    public void setData(Object... values) {
        this.towedVehicleMap.putAll((TShortShortHashMap)values[0]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.towedVehicleMap.clear();
        int size = b.getInt();

        for (int i = 0; i < size; i++) {
            this.towedVehicleMap.put(b.getShort(), b.getShort());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.towedVehicleMap.size());
        TShortShortIterator iterator = this.towedVehicleMap.iterator();

        while (iterator.hasNext()) {
            iterator.advance();
            b.putShort(iterator.key());
            b.putShort(iterator.value());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        VehicleManager.instance.towedVehicleMap.putAll(this.towedVehicleMap);
    }
}
