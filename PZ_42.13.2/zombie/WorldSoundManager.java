// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.Stack;
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
import zombie.popman.ZombiePopulationManager;

@UsedFromLua
public final class WorldSoundManager {
    public static final WorldSoundManager instance = new WorldSoundManager();
    public final ArrayList<WorldSoundManager.WorldSound> soundList = new ArrayList<>();
    private final Stack<WorldSoundManager.WorldSound> freeSounds = new Stack<>();
    private static final WorldSoundManager.ResultBiggestSound resultBiggestSound = new WorldSoundManager.ResultBiggestSound();

    public void init(IsoCell cell) {
    }

    public void initFrame() {
    }

    public void KillCell() {
        for (WorldSoundManager.WorldSound sound : this.soundList) {
            sound.source = null;
        }

        this.freeSounds.addAll(this.soundList);
        this.soundList.clear();
    }

    public WorldSoundManager.WorldSound getNew() {
        return this.freeSounds.isEmpty() ? new WorldSoundManager.WorldSound() : this.freeSounds.pop();
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
        if (radius <= 0) {
            return null;
        } else {
            WorldSoundManager.WorldSound s;
            synchronized (this.soundList) {
                s = this.getNew().init(source, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod);
                s.repeating = repeating;
                s.stressAnimals = stressAnimals;
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

                    int radiusMax = (int)PZMath.ceil(radius * this.getHearingMultiplier(hearing));
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

    public WorldSoundManager.WorldSound getSoundZomb(IsoZombie zom) {
        IsoChunk chunk = null;
        if (zom.soundSourceTarget == null) {
            return null;
        } else if (zom.getCurrentSquare() == null) {
            return null;
        } else {
            chunk = zom.getCurrentSquare().chunk;
            ArrayList<WorldSoundManager.WorldSound> SoundList = null;
            if (chunk != null && !GameServer.server) {
                SoundList = chunk.soundList;
            } else {
                SoundList = this.soundList;
            }

            for (int n = 0; n < SoundList.size(); n++) {
                WorldSoundManager.WorldSound sound = SoundList.get(n);
                if (zom.soundSourceTarget == sound.source) {
                    return sound;
                }
            }

            return null;
        }
    }

    public WorldSoundManager.WorldSound getSoundAnimal(IsoAnimal animal) {
        IsoChunk chunk = null;
        if (animal.getCurrentSquare() == null) {
            return null;
        } else {
            chunk = animal.getCurrentSquare().chunk;
            ArrayList<WorldSoundManager.WorldSound> SoundList = null;
            if (chunk != null && !GameServer.server) {
                SoundList = chunk.soundList;
            } else {
                SoundList = this.soundList;
            }

            for (int n = 0; n < SoundList.size(); n++) {
                WorldSoundManager.WorldSound sound = SoundList.get(n);
                if (sound.stresshumans || sound.stressAnimals) {
                    return sound;
                }
            }

            return null;
        }
    }

    public WorldSoundManager.ResultBiggestSound getBiggestSoundZomb(int x, int y, int z, boolean ignoreBySameType, IsoZombie zom) {
        float LargestSound = -1000000.0F;
        WorldSoundManager.WorldSound largest = null;
        IsoChunk chunk = null;
        if (zom != null) {
            if (zom.getCurrentSquare() == null) {
                return resultBiggestSound.init(null, 0.0F);
            }

            chunk = zom.getCurrentSquare().chunk;
        }

        ArrayList<WorldSoundManager.WorldSound> SoundList = null;
        if (chunk != null && !GameServer.server) {
            SoundList = chunk.soundList;
        } else {
            SoundList = this.soundList;
        }

        for (int n = 0; n < SoundList.size(); n++) {
            WorldSoundManager.WorldSound sound = SoundList.get(n);
            if (sound != null && sound.radius != 0) {
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
                        if (tot > LargestSound) {
                            LargestSound = tot;
                            largest = sound;
                        }
                    }
                }
            }
        }

        return resultBiggestSound.init(largest, LargestSound);
    }

    public float getSoundAttract(WorldSoundManager.WorldSound sound, IsoZombie zom) {
        if (sound == null) {
            return 0.0F;
        } else if (sound.radius == 0) {
            return 0.0F;
        } else {
            float dist = IsoUtils.DistanceToSquared(zom.getX(), zom.getY(), zom.getZ() * 3.0F, sound.x, sound.y, sound.z * 3);
            float radius = sound.radius * this.getHearingMultiplier(zom);
            if (dist > radius * radius) {
                return 0.0F;
            } else if (dist < sound.zombieIgnoreDist * sound.zombieIgnoreDist && zom.getZ() == sound.z) {
                return 0.0F;
            } else if (sound.sourceIsZombie) {
                return 0.0F;
            } else {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
                IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare((double)zom.getX(), (double)zom.getY(), (double)zom.getZ());
                float delta = dist / (radius * radius);
                if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
                    delta *= 1.2F;
                    if (sq2.getRoom() == null || sq.getRoom() == null) {
                        delta *= 1.4F;
                    }
                }

                delta = 1.0F - delta;
                if (delta <= 0.0F) {
                    return 0.0F;
                } else {
                    if (delta > 1.0F) {
                        delta = 1.0F;
                    }

                    return sound.volume * delta;
                }
            }
        }
    }

    public float getSoundAttractAnimal(WorldSoundManager.WorldSound sound, IsoAnimal animal) {
        if (sound == null) {
            return 0.0F;
        } else if (sound.radius == 0) {
            return 0.0F;
        } else {
            float dist = IsoUtils.DistanceTo(animal.getX(), animal.getY(), animal.getZ() * 3.0F, sound.x, sound.y, sound.z * 3);
            float radiusBonus = 1.0F;
            if (animal.isWild()) {
                radiusBonus = 3.0F;
            }

            if (dist > sound.radius * radiusBonus) {
                return 0.0F;
            } else if (dist < sound.zombieIgnoreDist * sound.zombieIgnoreDist && animal.getZ() == sound.z) {
                return 0.0F;
            } else {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
                IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare((double)animal.getX(), (double)animal.getY(), (double)animal.getZ());
                float delta = dist / (sound.radius * sound.radius);
                if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
                    delta *= 1.2F;
                    if (sq2.getRoom() == null || sq.getRoom() == null) {
                        delta *= 1.4F;
                    }
                }

                delta = 1.0F - delta;
                if (delta <= 0.0F) {
                    return 0.0F;
                } else {
                    if (delta > 1.0F) {
                        delta = 1.0F;
                    }

                    return sound.volume * delta;
                }
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
                this.freeSounds.push(sound);
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
                                            int CPW = 8;
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
            this.source = source;
            this.life = 16;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.volume = volume;
            this.stresshumans = stresshumans;
            this.stressAnimals = false;
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
    }
}
