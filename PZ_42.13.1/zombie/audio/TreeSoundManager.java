// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;

public class TreeSoundManager {
    private final ArrayList<IsoGridSquare> squares = new ArrayList<>();
    private final TreeSoundManager.Slot[] slots = new TreeSoundManager.Slot[10];
    private final Comparator<IsoGridSquare> comp = (a, b) -> {
        float aScore = this.getClosestListener(a.x + 0.5F, a.y + 0.5F, a.z);
        float bScore = this.getClosestListener(b.x + 0.5F, b.y + 0.5F, b.z);
        if (aScore > bScore) {
            return 1;
        } else {
            return aScore < bScore ? -1 : 0;
        }
    };

    public TreeSoundManager() {
        for (int i = 0; i < this.slots.length; i++) {
            this.slots[i] = new TreeSoundManager.Slot();
        }
    }

    public void addSquare(IsoGridSquare square) {
        if (!this.squares.contains(square)) {
            this.squares.add(square);
        }
    }

    public void update() {
        for (int i = 0; i < this.slots.length; i++) {
            this.slots[i].playing = false;
        }

        long ms = System.currentTimeMillis();
        if (this.squares.isEmpty()) {
            this.stopNotPlaying(ms);
        } else {
            Collections.sort(this.squares, this.comp);
            int count = Math.min(this.squares.size(), this.slots.length);

            for (int i = 0; i < count; i++) {
                IsoGridSquare square = this.squares.get(i);
                if (this.shouldPlay(square)) {
                    int j = this.getExistingSlot(square);
                    if (j != -1) {
                        this.slots[j].playSound(square);
                        this.slots[j].soundTime = ms;
                    }
                }
            }

            for (int ix = 0; ix < count; ix++) {
                IsoGridSquare square = this.squares.get(ix);
                if (this.shouldPlay(square)) {
                    int j = this.getExistingSlot(square);
                    if (j == -1) {
                        j = this.getFreeSlot();
                        this.slots[j].playSound(square);
                        this.slots[j].soundTime = ms;
                    }
                }
            }

            this.stopNotPlaying(ms);
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

    boolean shouldPlay(IsoGridSquare square) {
        return square == null ? false : !(this.getClosestListener(square.x + 0.5F, square.y + 0.5F, square.z) > 20.0F);
    }

    int getExistingSlot(IsoGridSquare square) {
        for (int i = 0; i < this.slots.length; i++) {
            if (this.slots[i].square == square) {
                return i;
            }
        }

        return -1;
    }

    private int getFreeSlot() {
        for (int i = 0; i < this.slots.length; i++) {
            if (!this.slots[i].playing) {
                return i;
            }
        }

        return -1;
    }

    private int getFreeSlot(long ms) {
        long oldestTime = Long.MAX_VALUE;
        int oldestIndex = -1;

        for (int i = 0; i < this.slots.length; i++) {
            if (this.slots[i].soundTime < oldestTime) {
                oldestTime = this.slots[i].soundTime;
                oldestIndex = i;
            }
        }

        return ms - oldestTime < 1000L ? -1 : oldestIndex;
    }

    void stopNotPlaying(long ms) {
        for (int i = 0; i < this.slots.length; i++) {
            TreeSoundManager.Slot slot = this.slots[i];
            if (!slot.playing && slot.soundTime <= ms - 1000L) {
                slot.stopPlaying();
                slot.square = null;
            }
        }
    }

    private static final class Slot {
        long soundTime;
        IsoGridSquare square;
        boolean playing;
        BaseSoundEmitter emitter;
        long instance;

        void playSound(IsoGridSquare square) {
            if (this.emitter == null) {
                this.emitter = (BaseSoundEmitter)(Core.soundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
            }

            this.emitter.setPos(square.x + 0.5F, square.y + 0.5F, square.z);
            if (!this.emitter.isPlaying("Bushes")) {
                this.instance = this.emitter.playSoundImpl("Bushes", (IsoObject)null);
                this.emitter.setParameterValueByName(this.instance, "Occlusion", 0.0F);
            }

            this.square = square;
            this.playing = true;
            this.emitter.tick();
        }

        void stopPlaying() {
            if (this.emitter != null && this.instance != 0L) {
                if (this.emitter.hasSustainPoints(this.instance)) {
                    this.emitter.triggerCue(this.instance);
                    this.instance = 0L;
                } else {
                    this.emitter.stopAll();
                    this.instance = 0L;
                }
            } else {
                if (this.emitter != null && !this.emitter.isEmpty()) {
                    this.emitter.tick();
                }
            }
        }
    }
}
