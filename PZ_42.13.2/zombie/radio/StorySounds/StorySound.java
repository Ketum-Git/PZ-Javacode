// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.StorySounds;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.iso.Vector2;

/**
 * Turbo
 */
@UsedFromLua
public final class StorySound {
    protected String name;
    protected float baseVolume = 1.0F;

    public StorySound(String name, float baseVol) {
        this.name = name;
        this.baseVolume = baseVol;
    }

    public long playSound() {
        Vector2 pos = SLSoundManager.getInstance().getRandomBorderPosition();
        return SLSoundManager.emitter.playSound(this.name, this.baseVolume, pos.x, pos.y, 0.0F, 100.0F, SLSoundManager.getInstance().getRandomBorderRange());
    }

    public long playSound(float volumeOverride) {
        return SLSoundManager.emitter
            .playSound(this.name, volumeOverride, IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), 10.0F, 50.0F);
    }

    public long playSound(float x, float y, float z, float minRange, float maxRange) {
        return this.playSound(this.baseVolume, x, y, z, minRange, maxRange);
    }

    public long playSound(float volumeMod, float x, float y, float z, float minRange, float maxRange) {
        return SLSoundManager.emitter.playSound(this.name, this.baseVolume * volumeMod, x, y, z, minRange, maxRange);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getBaseVolume() {
        return this.baseVolume;
    }

    public void setBaseVolume(float baseVolume) {
        this.baseVolume = baseVolume;
    }

    public StorySound getClone() {
        return new StorySound(this.name, this.baseVolume);
    }
}
