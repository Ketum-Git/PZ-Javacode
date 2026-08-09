// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

final class BackupBeeperSound extends VehicleSound {
    private long instance;

    BackupBeeperSound(VehicleSoundOwner owner) {
        super(owner);
    }

    @Override
    public void update() {
        if (this.getOwner().isBackupBeeperSounding() && this.getOwner().getScript().getSounds().backSignalEnable) {
            if (!this.getEmitter().isPlaying(this.instance)) {
                String soundName = this.getOwner().getScript().getSounds().backSignal;
                this.instance = this.getEmitter().playSoundLoopedImpl(soundName);
            }

            this.getEmitter().setVolume(this.instance, VehicleSounds.SOUND_VOLUME);
            this.getEmitter().set3D(this.instance, false);
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
