// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

public interface IVehicleEngineListener {
    void onEngineStateChanged(BaseVehicle.engineStateTypes var1, BaseVehicle.engineStateTypes var2, VehicleEngineStateChangeReason var3);
}
