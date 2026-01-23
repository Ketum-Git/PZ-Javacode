// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import zombie.characters.animals.AnimalSoundState;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class AnimalVocalsManager {
    public static final AnimalVocalsManager instance = new AnimalVocalsManager();
    private final HashSet<IsoAnimal> added = new HashSet<>();
    private final ObjectPool<AnimalVocalsManager.ObjectWithDistance> objectPool = new ObjectPool<>(AnimalVocalsManager.ObjectWithDistance::new);
    private final ArrayList<AnimalVocalsManager.ObjectWithDistance> objects = new ArrayList<>();
    private final AnimalVocalsManager.Slot[] slots;
    private long updateMs;
    private final Comparator<AnimalVocalsManager.ObjectWithDistance> comp = new Comparator<AnimalVocalsManager.ObjectWithDistance>() {
        {
            Objects.requireNonNull(AnimalVocalsManager.this);
        }

        public int compare(AnimalVocalsManager.ObjectWithDistance a, AnimalVocalsManager.ObjectWithDistance b) {
            return Float.compare(a.distSq, b.distSq);
        }
    };

    public AnimalVocalsManager() {
        int NUM_SLOTS = 20;
        this.slots = PZArrayUtil.newInstance(AnimalVocalsManager.Slot.class, 20, AnimalVocalsManager.Slot::new);
    }

    public void addCharacter(IsoAnimal chr) {
        if (!this.added.contains(chr)) {
            this.added.add(chr);
            AnimalVocalsManager.ObjectWithDistance owd = this.objectPool.alloc();
            owd.character = chr;
            this.objects.add(owd);
        }
    }

    public void update() {
        if (!GameServer.server) {
            long ms = System.currentTimeMillis();
            if (ms - this.updateMs >= 500L) {
                this.updateMs = ms;

                for (int i = 0; i < this.slots.length; i++) {
                    this.slots[i].playing = false;
                }

                if (this.objects.isEmpty()) {
                    this.stopNotPlaying();
                } else {
                    for (int i = 0; i < this.objects.size(); i++) {
                        AnimalVocalsManager.ObjectWithDistance owd = this.objects.get(i);
                        IsoAnimal chr = owd.character;
                        owd.distSq = this.getClosestListener(chr.getX(), chr.getY(), chr.getZ());
                    }

                    this.objects.sort(this.comp);
                    int count = PZMath.min(this.slots.length, this.objects.size());

                    for (int i = 0; i < count; i++) {
                        IsoAnimal object = this.objects.get(i).character;
                        if (this.shouldPlay(object)) {
                            int j = this.getExistingSlot(object);
                            if (j != -1) {
                                this.slots[j].playSound(object);
                            }
                        }
                    }

                    for (int ix = 0; ix < count; ix++) {
                        IsoAnimal object = this.objects.get(ix).character;
                        if (this.shouldPlay(object)) {
                            int j = this.getExistingSlot(object);
                            if (j == -1) {
                                j = this.getFreeSlot();
                                this.slots[j].playSound(object);
                            }
                        }
                    }

                    this.stopNotPlaying();
                    this.postUpdate();
                    this.added.clear();
                    this.objectPool.release(this.objects);
                    this.objects.clear();
                }
            }
        }
    }

    boolean shouldPlay(IsoAnimal chr) {
        if (chr.isDead()) {
            return false;
        } else {
            if (chr.getVehicle() != null) {
                if (chr.getVehicle().getMovingObjectIndex() == -1) {
                    return false;
                }
            } else if (chr.getCurrentSquare() == null) {
                return false;
            }

            return chr.getAnimalSoundState("voice").shouldPlay();
        }
    }

    int getExistingSlot(IsoAnimal chr) {
        for (int i = 0; i < this.slots.length; i++) {
            if (this.slots[i].character == chr) {
                return i;
            }
        }

        return -1;
    }

    int getFreeSlot() {
        for (int i = 0; i < this.slots.length; i++) {
            if (!this.slots[i].playing) {
                return i;
            }
        }

        return -1;
    }

    void stopNotPlaying() {
        for (int i = 0; i < this.slots.length; i++) {
            AnimalVocalsManager.Slot slot = this.slots[i];
            if (!slot.playing) {
                slot.stopPlaying();
                slot.character = null;
            }
        }
    }

    public void postUpdate() {
    }

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

    public void render() {
    }

    public static void Reset() {
        for (int i = 0; i < instance.slots.length; i++) {
            instance.slots[i].stopPlaying();
            instance.slots[i].character = null;
            instance.slots[i].playing = false;
        }
    }

    static final class ObjectWithDistance {
        IsoAnimal character;
        float distSq;
    }

    static final class Slot {
        IsoAnimal character;
        boolean playing;

        void playSound(IsoAnimal chr) {
            if (this.character != null && this.character != chr && this.character.getAnimalSoundState("voice").getEventInstance() != 0L) {
                this.character.getAnimalSoundState("voice").stop();
            }

            this.character = chr;
            this.playing = true;
            AnimalSoundState ass = this.character.getAnimalSoundState("voice");
            if (!ass.isPlayingDesiredSound()) {
                String soundName = ass.getDesiredSoundName();
                int priority = ass.getDesiredSoundPriority();
                chr.getAnimalSoundState("voice").start(soundName, priority);
            }
        }

        void stopPlaying() {
            if (this.character != null && this.character.getAnimalSoundState("voice").getEventInstance() != 0L) {
                if (!this.character.isDead()) {
                    this.character.getAnimalSoundState("voice").stop();
                }
            }
        }
    }
}
