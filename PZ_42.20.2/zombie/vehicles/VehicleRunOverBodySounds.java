// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import zombie.audio.BaseSoundEmitter;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.SoundKey;

public final class VehicleRunOverBodySounds {
    private static final long INTERVAL = 500L;
    private final VehicleRunOverBodySounds.Slot[] slots = new VehicleRunOverBodySounds.Slot[4];

    public void possiblyPlay(int wheel, float x, float y, float z) {
        if (this.slots[wheel] == null) {
            this.slots[wheel] = new VehicleRunOverBodySounds.Slot();
        }

        VehicleRunOverBodySounds.Slot slot = this.slots[wheel];
        BaseSoundEmitter emitter = slot.checkEmitter(x, y, z);
        long millis = System.currentTimeMillis();
        if (emitter.isEmpty() || slot.time + 500L < millis) {
            emitter.stopAll();
            emitter.playSoundImpl(SoundKey.VEHICLE_RUN_OVER_BODY.getSoundName(), (IsoObject)null);
            slot.time = millis;
        }
    }

    public void update() {
        for (int i = 0; i < this.slots.length; i++) {
            VehicleRunOverBodySounds.Slot slot = this.slots[i];
            if (slot != null && slot.emitter != null) {
                slot.emitter.tick();
            }
        }
    }

    public void release() {
        for (int i = 0; i < this.slots.length; i++) {
            VehicleRunOverBodySounds.Slot slot = this.slots[i];
            if (slot != null) {
                slot.release();
            }
        }
    }

    private static final class Slot {
        BaseSoundEmitter emitter;
        long time;

        BaseSoundEmitter checkEmitter(float x, float y, float z) {
            if (this.emitter == null) {
                this.emitter = IsoWorld.instance.getFreeEmitter(x, y, z);
                IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            }

            this.emitter.setPos(x, y, z);
            return this.emitter;
        }

        void release() {
            if (this.emitter != null) {
                this.emitter.stopAll();
                IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
                this.emitter = null;
                this.time = 0L;
            }
        }
    }
}
