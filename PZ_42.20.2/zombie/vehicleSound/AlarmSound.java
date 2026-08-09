// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

import zombie.iso.IsoObject;

final class AlarmSound extends VehicleSound {
    private long instance;

    AlarmSound(VehicleSoundOwner owner) {
        super(owner);
    }

    @Override
    public void update() {
        if (this.getOwner().isAlarmSounding() && this.getOwner().getChosenAlarmSound() != null) {
            if (!this.getEmitter().isPlaying(this.getOwner().getChosenAlarmSound())) {
                if (this.instance != 0L) {
                    this.getEmitter().stopSound(this.instance);
                }

                this.instance = this.getEmitter().playSoundImpl(this.getOwner().getChosenAlarmSound(), (IsoObject)null);
            }

            this.getEmitter().setVolume(this.instance, VehicleSounds.SOUND_VOLUME);
            this.getEmitter().set3D(this.instance, !this.getOwner().isAnyListenerInside());
        } else if (this.instance != 0L) {
            this.getEmitter().stopSound(this.instance);
            this.instance = 0L;
        }
    }

    @Override
    public void remove() {
        if (this.instance != 0L) {
            this.getEmitter().stopSound(this.instance);
            this.instance = 0L;
        }
    }
}
