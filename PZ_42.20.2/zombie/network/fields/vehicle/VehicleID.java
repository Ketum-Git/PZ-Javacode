// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class VehicleID extends IDShort implements IPositional, INetworkPacketField {
    protected BaseVehicle vehicle;

    public void set(BaseVehicle vehicle) {
        this.setID(vehicle != null ? vehicle.getId() : -1);
        this.vehicle = vehicle;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.vehicle = VehicleManager.instance.getVehicleByID(this.getID());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.vehicle != null;
    }

    public BaseVehicle getVehicle() {
        return this.vehicle;
    }

    @Override
    public float getX() {
        return this.vehicle == null ? 0.0F : this.vehicle.getX();
    }

    @Override
    public float getY() {
        return this.vehicle == null ? 0.0F : this.vehicle.getY();
    }

    @Override
    public float getZ() {
        return this.vehicle == null ? 0.0F : this.vehicle.getZ();
    }
}
