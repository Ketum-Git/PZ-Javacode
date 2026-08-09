// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

import zombie.audio.BaseSoundEmitter;

abstract class VehicleSound {
    protected final VehicleSoundOwner owner;

    public VehicleSound(VehicleSoundOwner owner) {
        this.owner = owner;
    }

    public VehicleSoundOwner getOwner() {
        return this.owner;
    }

    public BaseSoundEmitter getEmitter() {
        return this.getOwner().getVehicleSoundEmitter();
    }

    public abstract void update();

    public abstract void remove();
}
