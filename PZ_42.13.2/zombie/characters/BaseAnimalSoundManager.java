// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;

public abstract class BaseAnimalSoundManager {
    protected final ArrayList<IsoAnimal> characters = new ArrayList<>();
    private final long[] soundTime;
    private final int staleSlotMs;
    private final Comparator<IsoAnimal> comp = new Comparator<IsoAnimal>() {
        {
            Objects.requireNonNull(BaseAnimalSoundManager.this);
        }

        public int compare(IsoAnimal a, IsoAnimal b) {
            float aScore = BaseAnimalSoundManager.this.getClosestListener(a.getX(), a.getY(), a.getZ());
            float bScore = BaseAnimalSoundManager.this.getClosestListener(b.getX(), b.getY(), b.getZ());
            if (aScore > bScore) {
                return 1;
            } else {
                return aScore < bScore ? -1 : 0;
            }
        }
    };

    public BaseAnimalSoundManager(int numSlots, int staleSlotMs) {
        this.soundTime = new long[numSlots];
        this.staleSlotMs = staleSlotMs;
    }

    public void addCharacter(IsoAnimal chr) {
        if (!this.characters.contains(chr)) {
            this.characters.add(chr);
        }
    }

    public void update() {
        if (!this.characters.isEmpty()) {
            this.characters.sort(this.comp);
            long ms = System.currentTimeMillis();

            for (int i = 0; i < this.soundTime.length && i < this.characters.size(); i++) {
                IsoAnimal chr = this.characters.get(i);
                if (chr.getCurrentSquare() != null) {
                    int slot = this.getFreeSoundSlot(ms);
                    if (slot == -1) {
                        break;
                    }

                    this.playSound(chr);
                    this.soundTime[slot] = ms;
                }
            }

            this.postUpdate();
            this.characters.clear();
        }
    }

    public abstract void playSound(IsoAnimal var1);

    public abstract void postUpdate();

    private float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr != null && chr.getCurrentSquare() != null) {
                float px = chr.getX();
                float py = chr.getY();
                float pz = chr.getZ();
                float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0F, soundX, soundY, soundZ * 3.0F);
                distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0F);
                if (distSq < minDist) {
                    minDist = distSq;
                }
            }
        }

        return minDist;
    }

    private int getFreeSoundSlot(long ms) {
        long oldestTime = Long.MAX_VALUE;
        int oldestIndex = -1;

        for (int i = 0; i < this.soundTime.length; i++) {
            if (this.soundTime[i] < oldestTime) {
                oldestTime = this.soundTime[i];
                oldestIndex = i;
            }
        }

        return ms - oldestTime < this.staleSlotMs ? -1 : oldestIndex;
    }
}
