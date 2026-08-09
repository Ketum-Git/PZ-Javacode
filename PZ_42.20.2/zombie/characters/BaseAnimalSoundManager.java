// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import zombie.audio.FMODParameterUtils;
import zombie.characters.animals.IsoAnimal;

public abstract class BaseAnimalSoundManager {
    protected final ArrayList<IsoAnimal> characters = new ArrayList<>();
    private final long[] soundTime;
    private final int staleSlotMs;
    private final Comparator<IsoAnimal> comp = new Comparator<IsoAnimal>() {
        {
            Objects.requireNonNull(BaseAnimalSoundManager.this);
        }

        public int compare(IsoAnimal a, IsoAnimal b) {
            float aScore = FMODParameterUtils.getClosestListenerDistanceSquared(a.getX(), a.getY(), a.getZ());
            float bScore = FMODParameterUtils.getClosestListenerDistanceSquared(b.getX(), b.getY(), b.getZ());
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
