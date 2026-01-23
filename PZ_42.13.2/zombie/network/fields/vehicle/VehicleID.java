// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class VehicleID extends IDShort implements IPositional, INetworkPacketField {
    protected BaseVehicle vehicle;

    public void set(BaseVehicle vehicle) {
        this.setID(vehicle.getId());
        this.vehicle = vehicle;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.vehicle = VehicleManager.instance.getVehicleByID(this.getID());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.vehicle != null;
    }

    public BaseVehicle getVehicle() {
        return this.vehicle;
    }

    @Override
    public float getX() {
        return this.vehicle.getX();
    }

    @Override
    public float getY() {
        return this.vehicle.getY();
    }

    @Override
    public float getZ() {
        return this.vehicle.getZ();
    }
}
