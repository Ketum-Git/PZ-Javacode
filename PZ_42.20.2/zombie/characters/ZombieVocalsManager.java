// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import zombie.audio.FMODParameterUtils;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class ZombieVocalsManager {
    public static final int SOUND_RANGE = 70;
    private static final long UPDATE_INTERVAL = 500L;
    public static final ZombieVocalsManager instance = new ZombieVocalsManager();
    private final Set<IsoZombie> added = new HashSet<>();
    private final ObjectPool<ZombieVocalsManager.ObjectWithDistance> objectPool = new ObjectPool<>(
        ZombieVocalsManager.ObjectWithDistance::new, "ZombieVocalsManager.objectPool"
    );
    private final List<ZombieVocalsManager.ObjectWithDistance> objects = new ArrayList<>();
    private final ZombieVocalsManager.Slot[] slots;
    private long updateMs;
    private final Comparator<ZombieVocalsManager.ObjectWithDistance> comp = new Comparator<ZombieVocalsManager.ObjectWithDistance>() {
        {
            Objects.requireNonNull(ZombieVocalsManager.this);
        }

        public int compare(ZombieVocalsManager.ObjectWithDistance a, ZombieVocalsManager.ObjectWithDistance b) {
            return Float.compare(a.distSq, b.distSq);
        }
    };

    public ZombieVocalsManager() {
        int numSlots = 20;
        this.slots = PZArrayUtil.newInstance(ZombieVocalsManager.Slot.class, 20, ZombieVocalsManager.Slot::new);
    }

    public void addCharacter(IsoZombie chr) {
        if (!this.added.contains(chr)) {
            this.added.add(chr);
            ZombieVocalsManager.ObjectWithDistance owd = this.objectPool.alloc();
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
                        ZombieVocalsManager.ObjectWithDistance owd = this.objects.get(i);
                        IsoZombie chr = owd.character;
                        owd.distSq = FMODParameterUtils.getClosestListenerDistanceSquared(chr.getX(), chr.getY(), chr.getZ());
                    }

                    this.objects.sort(this.comp);
                    int count = PZMath.min(this.slots.length, this.objects.size());

                    for (int i = 0; i < count; i++) {
                        IsoZombie object = this.objects.get(i).character;
                        if (this.shouldPlay(object)) {
                            int j = this.getExistingSlot(object);
                            if (j != -1) {
                                this.slots[j].playSound(object);
                            }
                        }
                    }

                    for (int ix = 0; ix < count; ix++) {
                        IsoZombie object = this.objects.get(ix).character;
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

                    for (int ixx = 0; ixx < this.objects.size(); ixx++) {
                        ZombieVocalsManager.ObjectWithDistance owd = this.objects.get(ixx);
                        owd.character = null;
                    }

                    this.objectPool.release(this.objects);
                    this.objects.clear();
                }
            }
        }
    }

    boolean shouldPlay(IsoZombie chr) {
        return chr.getCurrentSquare() != null;
    }

    int getExistingSlot(IsoZombie chr) {
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
            ZombieVocalsManager.Slot slot = this.slots[i];
            if (!slot.playing) {
                slot.stopPlaying();
                slot.character = null;
            }
        }
    }

    public void postUpdate() {
    }

    public void render() {
        if (DebugOptions.instance.zombieVocalsRender.getValue()) {
            for (int i = 0; i < this.slots.length; i++) {
                ZombieVocalsManager.Slot slot = this.slots[i];
                if (slot.playing) {
                    IsoZombie chr = slot.character;
                    LineDrawer.addRect(chr.getX() - 0.45F, chr.getY() - 0.45F, chr.getZ(), 0.9F, 0.9F, 0.0F, 0.0F, 1.0F);
                }
            }
        }
    }

    public static void Reset() {
        for (int i = 0; i < instance.slots.length; i++) {
            instance.slots[i].stopPlaying();
            instance.slots[i].character = null;
            instance.slots[i].playing = false;
        }

        for (int i = 0; i < instance.objects.size(); i++) {
            instance.objects.get(i).character = null;
        }

        instance.objectPool.releaseAll(instance.objects);
        instance.objects.clear();
        instance.added.clear();
    }

    static final class ObjectWithDistance {
        IsoZombie character;
        float distSq;
    }

    static final class Slot {
        IsoZombie character;
        boolean playing;

        void playSound(IsoZombie chr) {
            if (this.character != null && this.character != chr && this.character.vocalEvent != 0L) {
                this.character.getEmitter().stopSoundLocal(this.character.vocalEvent);
                this.character.vocalEvent = 0L;
            }

            this.character = chr;
            this.playing = true;
            if (this.character.vocalEvent == 0L) {
                String soundName = chr.getVoiceSoundName();
                if (!chr.getFMODParameters().parameterList.contains(chr.parameterZombieState)) {
                    chr.getFMODParameters().add(chr.parameterCharacterInside);
                    chr.getFMODParameters().add(chr.parameterCharacterOnFire);
                    chr.getFMODParameters().add(chr.parameterPlayerDistance);
                    chr.getFMODParameters().add(chr.parameterZombieState);
                    chr.parameterCharacterInside.update();
                    chr.parameterCharacterOnFire.update();
                    chr.parameterPlayerDistance.update();
                    chr.parameterZombieState.update();
                }

                chr.vocalEvent = chr.getEmitter().playVocals(soundName);
            }
        }

        void stopPlaying() {
            if (this.character != null && this.character.vocalEvent != 0L) {
                this.character.getEmitter().stopSoundLocal(this.character.vocalEvent);
                this.character.vocalEvent = 0L;
            }
        }
    }
}
