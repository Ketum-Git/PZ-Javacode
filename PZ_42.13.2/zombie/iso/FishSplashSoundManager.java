// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.Comparator;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;

public class FishSplashSoundManager {
    public static final FishSplashSoundManager instance = new FishSplashSoundManager();
    private final ArrayList<IsoGridSquare> squares = new ArrayList<>();
    private final long[] soundTime = new long[6];
    private final Comparator<IsoGridSquare> comp = (a, b) -> {
        float aScore = this.getClosestListener(a.x + 0.5F, a.y + 0.5F, a.z);
        float bScore = this.getClosestListener(b.x + 0.5F, b.y + 0.5F, b.z);
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
                if (!(this.getClosestListener(square.x + 0.5F, square.y + 0.5F, square.z) > 20.0F)) {
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

    private float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoGameCharacter chr = IsoPlayer.players[i];
            if (chr != null && chr.getCurrentSquare() != null) {
                float px = chr.getX();
                float py = chr.getY();
                float pz = chr.getZ();
                float dist = IsoUtils.DistanceTo(px, py, pz * 3.0F, soundX, soundY, soundZ * 3.0F);
                dist *= chr.getHearDistanceModifier();
                if (dist < minDist) {
                    minDist = dist;
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

        return ms - oldestTime < 3000L ? -1 : oldestIndex;
    }
}
