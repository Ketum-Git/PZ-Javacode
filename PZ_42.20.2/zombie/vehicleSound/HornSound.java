// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

final class HornSound extends VehicleSound {
    private long instance;

    HornSound(VehicleSoundOwner owner) {
        super(owner);
    }

    @Override
    public void update() {
        if (this.getOwner().isHornSounding() && this.getOwner().getScript().getSounds().hornEnable) {
            if (!this.getEmitter().isPlaying(this.instance)) {
                String soundName = this.getOwner().getScript().getSounds().horn;
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
