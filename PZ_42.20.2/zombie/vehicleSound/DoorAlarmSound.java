// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

import zombie.scripting.objects.SoundKey;

final class DoorAlarmSound extends VehicleSound {
    private long instance;

    DoorAlarmSound(VehicleSoundOwner owner) {
        super(owner);
    }

    @Override
    public void update() {
        if (this.getOwner().isDoorAlarmSounding()) {
            if (!this.getEmitter().isPlaying(this.instance)) {
                String soundName = SoundKey.VEHICLE_DOOR_ALARM.getSoundName();
                this.instance = this.getEmitter().playSoundLoopedImpl(soundName);
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
