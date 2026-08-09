// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.Comparator;
import zombie.audio.FMODParameterUtils;

public class FishSplashSoundManager {
    private static final int SPLASH_SOUND_RADIUS = 20;
    public static final FishSplashSoundManager instance = new FishSplashSoundManager();
    private final ArrayList<IsoGridSquare> squares = new ArrayList<>();
    private final long[] soundTime = new long[6];
    private final Comparator<IsoGridSquare> comp = (a, b) -> {
        float aScore = FMODParameterUtils.getClosestListenerDistanceSquared(a.x + 0.5F, a.y + 0.5F, a.z);
        float bScore = FMODParameterUtils.getClosestListenerDistanceSquared(b.x + 0.5F, b.y + 0.5F, b.z);
        if (aScore > bScore) {
            return 1;
        } else {
            return aScore < bScore ? -1 : 0;
        }
    };

    public void addSquare(IsoGridSquare square) {
        if (!this.squares.contains(square)) {
            this.squares.add(square);
        }
    }

    public void update() {
        if (!this.squares.isEmpty()) {
            this.squares.sort(this.comp);
            long ms = System.currentTimeMillis();

            for (int i = 0; i < this.soundTime.length && i < this.squares.size(); i++) {
                IsoGridSquare square = this.squares.get(i);
                if (!(FMODParameterUtils.getClosestListenerDistanceSquared(square.x + 0.5F, square.y + 0.5F, square.z) > 400.0F)) {
                    int slot = this.getFreeSoundSlot(ms);
                    if (slot == -1) {
                        break;
                    }

                    square.playSoundLocal("FishBreath");
                    this.soundTime[slot] = ms;
                }
            }

            this.squares.clear();
        }
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

        return ms - oldestTime < 3000L ? -1 : oldestIndex;
    }
}
