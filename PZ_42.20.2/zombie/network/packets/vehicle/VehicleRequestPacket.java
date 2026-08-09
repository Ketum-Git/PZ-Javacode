// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import gnu.trove.iterator.TShortShortIterator;
import gnu.trove.map.hash.TShortShortHashMap;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class VehicleRequestPacket implements INetworkPacket {
    private TShortShortHashMap vehicleRequests;
    private final TShortShortHashMap parsedRequests = new TShortShortHashMap();

    @Override
    public void setData(Object... values) {
        this.vehicleRequests = (TShortShortHashMap)values[0];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.parsedRequests.clear();
        int size = b.getShort();

        for (int i = 0; i < size; i++) {
            short vehicleId = b.getShort();
            this.parsedRequests.put(vehicleId, b.getShort());
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        TShortShortIterator iterator = this.parsedRequests.iterator();

        while (iterator.hasNext()) {
            iterator.advance();
            BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(iterator.key());
            if (vehicle != null) {
                short flag = iterator.value();
                BaseVehicle.ServerVehicleState state = VehicleManager.instance.getVehicleState(connection, vehicle);
                if (flag == 16384) {
                    if (!connection.isRelevantTo(vehicle.getX(), vehicle.getY())) {
                        INetworkPacket.send(connection, PacketTypes.PacketType.VehicleRemove, vehicle);
                    }
                } else {
                    state.flags |= flag;
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.vehicleRequests.size());
        TShortShortIterator iterator = this.vehicleRequests.iterator();

        while (iterator.hasNext()) {
            iterator.advance();
            b.putShort(iterator.key());
            b.putShort(iterator.value());
        }
    }
}
