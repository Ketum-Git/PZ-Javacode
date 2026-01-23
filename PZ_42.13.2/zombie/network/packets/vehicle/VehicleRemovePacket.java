// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehicleRemovePacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();

    @Override
    public void setData(Object... values) {
        this.vehicleId.set((BaseVehicle)values[0]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleId.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleId.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.vehicleId.isConsistent(connection)) {
            this.vehicleId.getVehicle().serverRemovedFromWorld = true;
            this.vehicleId.getVehicle().removeFromWorld();
            this.vehicleId.getVehicle().removeFromSquare();
            VehicleManager.instance.unregisterVehicle(this.vehicleId.getVehicle());
        }

        VehicleCache.remove(this.vehicleId.getID());
    }
}
