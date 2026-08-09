// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.MPDebugInfo;
import zombie.popman.ObjectPool;
import zombie.popman.ZombiePopulationManager;
import zombie.vehicles.VehiclePartOwner;

@UsedFromLua
public final class WorldSoundManager {
    public static final WorldSoundManager instance = new WorldSoundManager();
    private static final float MUFFLE_SOUND_DIFFERENT_ROOMS = 1.2F;
    private static final float MUFFLE_SOUND_INSIDE_OUTSIDE = 1.4F;
    public final List<WorldSoundManager.WorldSound> soundList = new ArrayList<>();
    private final ObjectPool<WorldSoundManager.WorldSound> freeSounds = new ObjectPool<>(WorldSoundManager.WorldSound::new, "WorldSoundManager.freeSounds");
    private static final WorldSoundManager.ResultBiggestSound resultBiggestSound = new WorldSoundManager.ResultBiggestSound();

    public void init(IsoCell cell) {
    }

    public void initFrame() {
    }

    public void KillCell() {
        for (WorldSoundManager.WorldSound sound : this.soundList) {
            sound.source = null;
        }

        this.freeSounds.releaseAll(this.soundList);
        this.soundList.clear();
    }

    public WorldSoundManager.WorldSound getNew() {
        return this.freeSounds.alloc();
    }

    public WorldSoundManager.WorldSound release(WorldSoundManager.WorldSound worldSound) {
        this.freeSounds.release(worldSound);
        return null;
    }

    public WorldSoundManager.WorldSound addSound(Object source, int x, int y, int z, int radius, int volume) {
        return this.addSound(source, x, y, z, radius, volume, false, 0.0F, 1.0F);
    }

    public WorldSoundManager.WorldSound addSound(Object source, int x, int y, int z, int radius, int volume, boolean stressHumans) {
        return this.addSound(source, x, y, z, radius, volume, stressHumans, 0.0F, 1.0F);
    }

    public WorldSoundManager.WorldSound addSound(
        Object source, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod
    ) {
        return this.addSound(source, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod, false, true, false, false, false);
    }

    public WorldSoundManager.WorldSound addSoundRepeating(
        Object source, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod
    ) {
        return this.addSound(source, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod, false, true, false, true, false);
    }

    public WorldSoundManager.WorldSound addSound(
        Object source,
        int x,
        int y,
        int z,
        int radius,
        int volume,
        boolean stressHumans,
        float zombieIgnoreDist,
        float stressMod,
        boolean sourceIsZombie,
        boolean doSend,
        boolean remote
    ) {
        return this.addSound(source, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod, sourceIsZombie, doSend, remote, false, false);
    }

    public WorldSoundManager.WorldSound addSound(
        Object source,
        int x,
        int y,
        int z,
        int radius,
        int volume,
        boolean stressHumans,
        float zombieIgnoreDist,
        float stressMod,
        boolean sourceIsZombie,
        boolean doSend,
        boolean remote,
        boolean repeating,
        boolean stressAnimals
    ) {
        short flags = 4;
        if (stressAnimals) {
            flags = (short)(flags | 1);
        }

        if (stressHumans) {
            flags = (short)(flags | 2);
        }

        return this.addSound(source, x, y, z, radius, volume, zombieIgnoreDist, stressMod, sourceIsZombie, doSend, remote, repeating, flags);
    }

    public WorldSoundManager.WorldSound addSound(
        Object source,
        int x,
        int y,
        int z,
        int radius,
        int volume,
        float zombieIgnoreDist,
        float stressMod,
        boolean sourceIsZombie,
        boolean doSend,
        boolean remote,
        boolean repeating,
        short flags
    ) {
        if (radius <= 0) {
            return null;
        } else {
            WorldSoundManager.WorldSound s;
            synchronized (this.soundList) {
                s = this.getNew().init(source, x, y, z, radius, volume, zombieIgnoreDist, stressMod, flags);
                s.repeating = repeating;
                if (source == null) {
                    s.sourceIsZombie = sourceIsZombie;
                }

                if (!GameServer.server) {
                    int hearing = SandboxOptions.instance.lore.hearing.getValue();
                    if (hearing == 4) {
                        hearing = 1;
                    }

                    if (hearing == 5) {
                        hearing = 2;
                    }

                    float animalHearingMultiplier = !s.stresshumans && !s.stressAnimals ? 1.0F : 3.0F;
                    float zombieHearingMultiplier = this.getHearingMultiplier(hearing);
                    float radiusMultiplier = PZMath.max(animalHearingMultiplier, zombieHearingMultiplier);
                    int radiusMax = (int)PZMath.ceil(radius * radiusMultiplier);
                    int chunkMinX = (x - radiusMax) / 8;
                    int chunkMinY = (y - radiusMax) / 8;
                    int chunkMaxX = (int)Math.ceil(((float)x + radiusMax) / 8.0F);
                    int chunkMaxY = (int)Math.ceil(((float)y + radiusMax) / 8.0F);

                    for (int xx = chunkMinX; xx < chunkMaxX; xx++) {
                        for (int yy = chunkMinY; yy < chunkMaxY; yy++) {
                            IsoChunk c = IsoWorld.instance.currentCell.getChunk(xx, yy);
                            if (c != null) {
                                c.soundList.add(s);
                            }
                        }
                    }
                }

                this.soundList.add(s);
                ZombiePopulationManager.instance.addWorldSound(s, doSend);
            }

            if (doSend) {
                if (GameClient.client) {
                    GameClient.instance.sendWorldSound(s);
                } else if (GameServer.server) {
                    GameServer.sendWorldSound(s, null);
                }
            }

            if (Core.debug && GameClient.client) {
                MPDebugInfo.AddDebugSound(s);
            }

            return s;
        }
    }

    public WorldSoundManager.WorldSound addSoundRepeating(
        Object source, int x, int y, int z, int radius, int volume, boolean stressHumans, boolean stressAnimals
    ) {
        return this.addSound(source, x, y, z, radius, volume, stressHumans, 0.0F, 1.0F, false, true, false, true, stressAnimals);
    }

    public WorldSoundManager.WorldSound addSoundRepeating(Object source, int x, int y, int z, int radius, int volume, boolean stressHumans) {
        return this.addSoundRepeating(source, x, y, z, radius, volume, stressHumans, 0.0F, 1.0F);
    }

    public WorldSoundManager.WorldSound addSoundRepeating(Object source, int x, int y, int z, int radius, int volume, short flags) {
        boolean sourceIsZombie = false;
        boolean doSend = true;
        boolean remote = false;
        boolean repeating = true;
        return this.addSound(source, x, y, z, radius, volume, 0.0F, 1.0F, false, true, false, true, flags);
    }

    public WorldSoundManager.WorldSound getSoundZomb(IsoZombie zom) {
        if (zom.soundSourceTarget == null) {
            return null;
        } else if (zom.getCurrentSquare() == null) {
            return null;
        } else {
            IsoChunk chunk = zom.getCurrentSquare().chunk;
            List<WorldSoundManager.WorldSound> soundList;
            if (chunk != null && !GameServer.server) {
                soundList = chunk.soundList;
            } else {
                soundList = this.soundList;
            }

            for (int n = 0; n < soundList.size(); n++) {
                WorldSoundManager.WorldSound sound = soundList.get(n);
                if (zom.soundSourceTarget == sound.source && sound.stressZombies) {
                    return sound;
                }
            }

            return null;
        }
    }

    public WorldSoundManager.WorldSound getSoundAnimal(IsoAnimal animal) {
        if (animal.getCurrentSquare() == null) {
            return null;
        } else {
            IsoChunk chunk = animal.getCurrentSquare().chunk;
            List<WorldSoundManager.WorldSound> soundList;
            if (chunk != null && !GameServer.server) {
                soundList = chunk.soundList;
            } else {
                soundList = this.soundList;
            }

            WorldSoundManager.WorldSound loudest = null;
            float loudestVolume = 0.0F;

            for (int n = 0; n < soundList.size(); n++) {
                WorldSoundManager.WorldSound sound = soundList.get(n);
                if (sound.stresshumans || sound.stressAnimals) {
                    float distSq = IsoUtils.DistanceToSquared(animal.getX(), animal.getY(), animal.getZ() * 3.0F, sound.x, sound.y, sound.z * 3.0F);
                    float radiusBonus = animal.isWild() ? 3.0F : 1.0F;
                    float radius = sound.radius * radiusBonus;
                    if (!(distSq > radius * radius)) {
                        float delta = 1.0F - distSq / (radius * radius);
                        float volume = sound.volume * delta;
                        if (volume > loudestVolume) {
                            loudestVolume = volume;
                            loudest = sound;
                        }
                    }
                }
            }

            return loudest;
        }
    }

    public WorldSoundManager.ResultBiggestSound getBiggestSoundZomb(int x, int y, int z, boolean ignoreBySameType, IsoZombie zom) {
        float largestSound = -1000000.0F;
        WorldSoundManager.WorldSound largest = null;
        IsoChunk chunk = null;
        if (zom != null) {
            if (zom.getCurrentSquare() == null) {
                return resultBiggestSound.init(null, 0.0F);
            }

            chunk = zom.getCurrentSquare().chunk;
        }

        List<WorldSoundManager.WorldSound> soundList;
        if (chunk != null && !GameServer.server) {
            soundList = chunk.soundList;
        } else {
            soundList = this.soundList;
        }

        for (int n = 0; n < soundList.size(); n++) {
            WorldSoundManager.WorldSound sound = soundList.get(n);
            if (sound != null && sound.stressZombies && sound.radius != 0) {
                float dist = IsoUtils.DistanceToSquared(x, y, z * 3, sound.x, sound.y, sound.z * 3);
                float radius = sound.radius * this.getHearingMultiplier(zom);
                if (!(dist > radius * radius)
                    && (!(dist < sound.zombieIgnoreDist * sound.zombieIgnoreDist) || z != sound.z)
                    && (!ignoreBySameType || !sound.sourceIsZombie)) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
                    IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    float delta = dist / (radius * radius);
                    if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
                        delta *= 1.2F;
                        if (sq2.getRoom() == null || sq.getRoom() == null) {
                            delta *= 1.4F;
                        }
                    }

                    delta = 1.0F - delta;
                    if (!(delta <= 0.0F)) {
                        if (delta > 1.0F) {
                            delta = 1.0F;
                        }

                        float tot = sound.volume * delta;
                        if (tot > largestSound) {
                            largestSound = tot;
                            largest = sound;
                        }
                    }
                }
            }
        }

        return resultBiggestSound.init(largest, largestSound);
    }

    public float getSoundAttract(WorldSoundManager.WorldSound sound, IsoZombie zom) {
        if (sound == null) {
            return 0.0F;
        } else if (sound.radius == 0) {
            return 0.0F;
        } else if (sound.sourceIsZombie) {
            return 0.0F;
        } else {
            float distSq = IsoUtils.DistanceToSquared(zom.getX(), zom.getY(), zom.getZ() * 3.0F, sound.x, sound.y, sound.z * 3);
            float radius = sound.radius * this.getHearingMultiplier(zom);
            if (distSq > radius * radius) {
                return 0.0F;
            } else if (distSq < sound.zombieIgnoreDist * sound.zombieIgnoreDist && zom.getZ() == sound.z) {
                return 0.0F;
            } else {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
                IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare((double)zom.getX(), (double)zom.getY(), (double)zom.getZ());
                float delta = distSq / (radius * radius);
                if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
                    delta *= 1.2F;
                    if (sq2.getRoom() == null || sq.getRoom() == null) {
                        delta *= 1.4F;
                    }
                }

                delta = PZMath.clamp_01(1.0F - delta);
                return sound.volume * delta;
            }
        }
    }

    public float getSoundAttractAnimal(WorldSoundManager.WorldSound sound, IsoAnimal animal) {
        if (sound == null) {
            return 0.0F;
        } else if (sound.radius == 0) {
            return 0.0F;
        } else {
            float distSq = IsoUtils.DistanceToSquared(animal.getX(), animal.getY(), animal.getZ() * 3.0F, sound.x, sound.y, sound.z * 3);
            float radiusBonus = animal.isWild() ? 3.0F : 1.0F;
            float radius = sound.radius * radiusBonus;
            if (distSq > radius * radius) {
                return 0.0F;
            } else if (distSq < sound.zombieIgnoreDist * sound.zombieIgnoreDist && animal.getZ() == sound.z) {
                return 0.0F;
            } else {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
                IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare((double)animal.getX(), (double)animal.getY(), (double)animal.getZ());
                float delta = distSq / (radius * radius);
                if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
                    delta *= 1.2F;
                    if (sq2.getRoom() == null || sq.getRoom() == null) {
                        delta *= 1.4F;
                    }
                }

                delta = PZMath.clamp_01(1.0F - delta);
                return sound.volume * delta;
            }
        }
    }

    public float getStressFromSounds(int x, int y, int z) {
        float ret = 0.0F;

        for (int i = 0; i < this.soundList.size(); i++) {
            WorldSoundManager.WorldSound sound = this.soundList.get(i);
            if (sound.stresshumans && sound.radius != 0) {
                float dist = IsoUtils.DistanceManhatten(x, y, sound.x, sound.y);
                float delta = dist / sound.radius;
                delta = 1.0F - delta;
                if (!(delta <= 0.0F)) {
                    if (delta > 1.0F) {
                        delta = 1.0F;
                    }

                    float tot = delta * sound.stressMod;
                    ret += tot;
                }
            }
        }

        return ret;
    }

    public void update() {
        if (!GameServer.server) {
            for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[n];
                if (!chunkMap.ignore) {
                    for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                        for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                            IsoChunk chunk = chunkMap.getChunk(x, y);
                            if (chunk != null) {
                                chunk.updateSounds();
                            }
                        }
                    }
                }
            }
        }

        int s = this.soundList.size();

        for (int nx = 0; nx < s; nx++) {
            WorldSoundManager.WorldSound sound = this.soundList.get(nx);
            if (sound != null && sound.life > 0) {
                sound.life--;
            } else {
                this.soundList.remove(nx);
                this.release(sound);
                nx--;
                s--;
            }
        }
    }

    public void render() {
        if (Core.debug && DebugOptions.instance.worldSoundRender.getValue()) {
            if (!GameClient.client) {
                if (!GameServer.server || ServerGUI.isCreated()) {
                    int hearing = SandboxOptions.instance.lore.hearing.getValue();
                    if (hearing == 4) {
                        hearing = 2;
                    }

                    if (hearing == 5) {
                        hearing = 2;
                    }

                    float radiusMultiplier = this.getHearingMultiplier(hearing);

                    for (int i = 0; i < this.soundList.size(); i++) {
                        WorldSoundManager.WorldSound sound = this.soundList.get(i);
                        float radius = sound.radius * radiusMultiplier;
                        int segments = 32;
                        LineDrawer.DrawIsoCircle(sound.x, sound.y, sound.z, radius, 32, 1.0F, 1.0F, 1.0F, 1.0F);
                    }

                    if (!GameServer.server) {
                        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(0);
                        if (chunkMap != null && !chunkMap.ignore) {
                            for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                                for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                                    IsoChunk chunk = chunkMap.getChunk(x, y);
                                    if (chunk != null) {
                                        for (int i = 0; i < chunk.soundList.size(); i++) {
                                            WorldSoundManager.WorldSound sound = chunk.soundList.get(i);
                                            float radius = sound.radius * radiusMultiplier;
                                            int segments = 32;
                                            LineDrawer.DrawIsoCircle(sound.x, sound.y, sound.z, radius, 32, 0.0F, 1.0F, 1.0F, 1.0F);
                                            int chunksPerWidth = 8;
                                            float left = chunk.wx * 8 + 0.1F;
                                            float top = chunk.wy * 8 + 0.1F;
                                            float right = (chunk.wx + 1) * 8 - 0.1F;
                                            float bottom = (chunk.wy + 1) * 8 - 0.1F;
                                            LineDrawer.DrawIsoRect(left, top, right - left, bottom - top, sound.z, 0.0F, 1.0F, 1.0F);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public float getHearingMultiplier(IsoZombie zombie) {
        return zombie == null
            ? this.getHearingMultiplier(2)
            : this.getHearingMultiplier(zombie.hearing) * zombie.getWornItemsHearingMultiplier() * zombie.getWeatherHearingMultiplier();
    }

    public float getHearingMultiplier(int hearing) {
        if (hearing == 1) {
            return 3.0F;
        } else {
            return hearing == 3 ? 0.45F : 1.0F;
        }
    }

    public static final class ResultBiggestSound {
        public WorldSoundManager.WorldSound sound;
        public float attract;

        public WorldSoundManager.ResultBiggestSound init(WorldSoundManager.WorldSound sound, float attract) {
            this.sound = sound;
            this.attract = attract;
            return this;
        }
    }

    @UsedFromLua
    public static final class WorldSound {
        public Object source;
        public int life = 1;
        public int radius;
        public boolean stresshumans;
        public boolean stressZombies;
        public boolean stressAnimals;
        public int volume;
        public int x;
        public int y;
        public int z;
        public float zombieIgnoreDist;
        public boolean sourceIsZombie;
        public boolean sourceIsPlayer;
        public boolean sourceIsPlayerBase;
        public float stressMod = 1.0F;
        public boolean repeating;

        private boolean isSourceIsPlayerBase(Object source) {
            return source instanceof IsoGenerator
                || source instanceof IsoJukebox
                || source instanceof IsoTelevision
                || source instanceof IsoRadio
                || source instanceof IsoStove
                || source instanceof IsoClothingWasher
                || source instanceof IsoClothingDryer
                || source instanceof IsoCombinationWasherDryer;
        }

        public WorldSoundManager.WorldSound init(Object source, int x, int y, int z, int radius, int volume) {
            return this.init(source, x, y, z, radius, volume, false, 0.0F, 1.0F);
        }

        public WorldSoundManager.WorldSound init(Object source, int x, int y, int z, int radius, int volume, boolean stresshumans) {
            return this.init(source, x, y, z, radius, volume, stresshumans, 0.0F, 1.0F);
        }

        public WorldSoundManager.WorldSound init(
            Object source, int x, int y, int z, int radius, int volume, boolean stresshumans, float zombieIgnoreDist, float stressMod
        ) {
            short flags = 4;
            if (stresshumans) {
                flags = (short)(flags | 2);
            }

            return this.init(source, x, y, z, radius, volume, zombieIgnoreDist, stressMod, flags);
        }

        public WorldSoundManager.WorldSound init(
            Object source, int x, int y, int z, int radius, int volume, float zombieIgnoreDist, float stressMod, short flags
        ) {
            this.source = source;
            this.life = 16;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.volume = volume;
            this.stresshumans = (flags & 2) != 0;
            this.stressAnimals = (flags & 1) != 0;
            this.stressZombies = (flags & 4) != 0;
            this.zombieIgnoreDist = zombieIgnoreDist;
            this.stressMod = stressMod;
            this.sourceIsPlayer = source instanceof IsoPlayer;
            this.sourceIsPlayerBase = this.isSourceIsPlayerBase(source);
            this.sourceIsZombie = source instanceof IsoZombie;
            this.repeating = false;
            LuaEventManager.triggerEvent("OnWorldSound", x, y, z, radius, volume, source);
            if (!GameClient.client) {
                FishSchoolManager.getInstance().addSoundNoise(x, y, radius / 6);
            }

            return this;
        }

        public WorldSoundManager.WorldSound init(
            boolean sourceIsZombie, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod
        ) {
            WorldSoundManager.WorldSound sound = this.init(null, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod);
            sound.sourceIsZombie = sourceIsZombie;
            return sound;
        }

        public WorldSoundManager.WorldSound init(WorldSoundManager.WorldSound other) {
            this.source = other.source;
            this.life = other.life;
            this.radius = other.radius;
            this.stresshumans = other.stresshumans;
            this.stressZombies = other.stressZombies;
            this.stressAnimals = other.stressAnimals;
            this.volume = other.volume;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.zombieIgnoreDist = other.zombieIgnoreDist;
            this.sourceIsZombie = other.sourceIsZombie;
            this.sourceIsPlayer = other.sourceIsPlayer;
            this.sourceIsPlayerBase = other.sourceIsPlayerBase;
            this.stressMod = other.stressMod;
            this.repeating = other.repeating;
            return this;
        }

        public boolean sourceIsVehicle() {
            return this.source instanceof VehiclePartOwner;
        }
    }
}
