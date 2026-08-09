// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;

public abstract class VehicleField implements INetworkPacketField {
    protected final VehicleID vehicleID;

    protected VehicleField(VehicleID vehicleID) {
        this.vehicleID = vehicleID;
    }

    protected BaseVehicle getVehicle() {
        return this.vehicleID.getVehicle();
    }
}
