// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

import zombie.scripting.objects.VehicleScript;

final class SirenSound extends VehicleSound {
    private long instance;

    SirenSound(VehicleSoundOwner owner) {
        super(owner);
    }

    @Override
    public void update() {
        if (this.getOwner().isSirenSounding()) {
            VehicleScript.LightBar lightBar = this.getOwner().getScript().getLightbar();
            String soundName = this.getOwner().getLightbarSirenModeObject().getSoundName(lightBar);
            if (!this.getEmitter().isPlaying(soundName)) {
                if (this.instance != 0L) {
                    this.getEmitter().stopSound(this.instance);
                }

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
