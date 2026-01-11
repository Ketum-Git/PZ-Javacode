// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;
import zombie.scripting.objects.CharacterTrait;
import zombie.util.list.PZArrayUtil;

public final class FliesSound {
    public static int maxCorpseCount = 25;
    public static final FliesSound instance = new FliesSound();
    private static final IsoGridSquare[] tempSquares = new IsoGridSquare[64];
    private final FliesSound.PlayerData[] playerData = new FliesSound.PlayerData[4];
    private final ArrayList<FliesSound.FadeEmitter> fadeEmitters = new ArrayList<>();
    private final float fliesVolume = -1.0F;

    public FliesSound() {
        for (int i = 0; i < this.playerData.length; i++) {
            this.playerData[i] = new FliesSound.PlayerData();
        }
    }

    public void Reset() {
        for (int i = 0; i < this.playerData.length; i++) {
            this.playerData[i].Reset();
        }
    }

    public void update() {
        if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() != 1) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && player.getCurrentSquare() != null) {
                    this.playerData[i].update(player);
                }
            }

            for (int ix = 0; ix < this.fadeEmitters.size(); ix++) {
                FliesSound.FadeEmitter emitter = this.fadeEmitters.get(ix);
                if (emitter.update()) {
                    this.fadeEmitters.remove(ix--);
                }
            }
        }
    }

    public void render(int playerIndex) {
        int CPW = 8;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];

        for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; cy++) {
            for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; cx++) {
                IsoChunk chunk = chunkMap.getChunk(cx, cy);
                if (chunk != null) {
                    FliesSound.ChunkData chunkData = chunk.corpseData;
                    if (chunkData != null) {
                        int z = PZMath.fastfloor(IsoPlayer.players[playerIndex].getZ());
                        FliesSound.ChunkLevelData levelData = chunkData.levelData[z + 32];

                        for (int i = 0; i < levelData.emitters.length; i++) {
                            FliesSound.FadeEmitter emitter = levelData.emitters[i];
                            if (emitter != null && emitter.emitter != null) {
                                float alpha = 1.0F;
                                if (this.fadeEmitters.contains(emitter)) {
                                    alpha = PZMath.max(emitter.volume, 0.1F);
                                }

                                this.paintSquare(emitter.sq.x, emitter.sq.y, emitter.sq.z, 0.0F, 1.0F, 0.0F, alpha);
                            }

                            if (this.playerData[playerIndex].refs.contains(levelData)) {
                                this.paintSquare(chunk.wx * 8 + 4, chunk.wy * 8 + 4, 0, 0.0F, 0.0F, 1.0F, 1.0F);
                            }
                        }

                        IsoBuilding building = IsoPlayer.players[0].getCurrentBuilding();
                        if (building != null && levelData.buildingCorpseCount != null && levelData.buildingCorpseCount.containsKey(building)) {
                            this.paintSquare(chunk.wx * 8 + 4, chunk.wy * 8 + 4, z, 1.0F, 0.0F, 0.0F, 1.0F);
                        }
                    }
                }
            }
        }
    }

    private void paintSquare(int x, int y, int z, float r, float g, float b, float a) {
        int SCL = Core.tileScale;
        int sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
        int sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32 * SCL, sy - 16 * SCL, sx + 64 * SCL, sy, sx + 32 * SCL, sy + 16 * SCL, r, g, b, a);
    }

    public void chunkLoaded(IsoChunk chunk) {
        if (chunk.corpseData == null) {
            chunk.corpseData = new FliesSound.ChunkData(chunk.wx, chunk.wy);
        }

        chunk.corpseData.wx = chunk.wx;
        chunk.corpseData.wy = chunk.wy;
        chunk.corpseData.Reset();

        for (int i = 0; i < this.playerData.length; i++) {
            this.playerData[i].forceUpdate = true;
        }
    }

    public void corpseAdded(int x, int y, int z) {
        if (z >= -32 && z <= 31) {
            FliesSound.ChunkData chunkData = this.getChunkData(x, y);
            if (chunkData != null) {
                chunkData.corpseAdded(x, y, z);

                for (int i = 0; i < this.playerData.length; i++) {
                    FliesSound.ChunkLevelData levelData = chunkData.levelData[z + 32];
                    if (this.playerData[i].refs.contains(levelData)) {
                        this.playerData[i].forceUpdate = true;
                    }
                }
            }
        } else {
            DebugLog.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
        }
    }

    public void corpseRemoved(int x, int y, int z) {
        if (z >= -32 && z <= 31) {
            FliesSound.ChunkData chunkData = this.getChunkData(x, y);
            if (chunkData != null) {
                chunkData.corpseRemoved(x, y, z);

                for (int i = 0; i < this.playerData.length; i++) {
                    FliesSound.ChunkLevelData levelData = chunkData.levelData[z + 32];
                    if (this.playerData[i].refs.contains(levelData)) {
                        this.playerData[i].forceUpdate = true;
                    }
                }
            }
        } else {
            DebugLog.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
        }
    }

    public int getCorpseCount(IsoGameCharacter chr) {
        if (chr != null && chr.getCurrentSquare() != null) {
            int DIM = 8;
            return this.getCorpseCount(PZMath.fastfloor(chr.getX()) / 8, PZMath.fastfloor(chr.getY()) / 8, PZMath.fastfloor(chr.getZ()), chr.getBuilding());
        } else {
            return 0;
        }
    }

    private int getCorpseCount(int wx, int wy, int z, IsoBuilding building) {
        int count = 0;
        int DIM = 8;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                FliesSound.ChunkData chunkData = this.getChunkData((wx + dx) * 8, (wy + dy) * 8);
                if (chunkData != null) {
                    FliesSound.ChunkLevelData levelData = chunkData.levelData[z + 32];
                    if (building == null) {
                        count += levelData.corpseCount;
                    } else if (levelData.buildingCorpseCount != null) {
                        Integer countObj = levelData.buildingCorpseCount.get(building);
                        if (countObj != null) {
                            count += countObj;
                        }
                    }

                    if (count >= maxCorpseCount) {
                        return count;
                    }
                }
            }
        }

        if (SandboxOptions.instance.zombieHealthImpact.getValue()) {
            int x = wx * 8;
            int y = wy * 8;
            int offset = 12;

            for (int dy = -12; dy <= 12; dy++) {
                for (int dxx = -12; dxx <= 12; dxx++) {
                    IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(x + dxx, y + dy, z);
                    if (sq != null) {
                        if (building == null && sq.getBuilding() == null) {
                            count += sq.getZombieCount();
                        } else if (building == sq.getBuilding()) {
                            count += sq.getZombieCount();
                        }

                        if (count >= maxCorpseCount) {
                            return count;
                        }
                    }
                }
            }
        }

        return count;
    }

    private FliesSound.ChunkData getChunkData(int x, int y) {
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, 0);
        return chunk != null ? chunk.corpseData : null;
    }

    public final class ChunkData {
        private int wx;
        private int wy;
        private final FliesSound.ChunkLevelData[] levelData;

        private ChunkData(final int wx, final int wy) {
            Objects.requireNonNull(FliesSound.this);
            super();
            this.levelData = new FliesSound.ChunkLevelData[64];
            this.wx = wx;
            this.wy = wy;

            for (int z = 0; z < this.levelData.length; z++) {
                this.levelData[z] = FliesSound.this.new ChunkLevelData();
            }
        }

        private void corpseAdded(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            IsoBuilding building = square == null ? null : square.getBuilding();
            int col = x - this.wx * 8;
            int row = y - this.wy * 8;
            this.levelData[z + 32].corpseAdded(col, row, building);
        }

        private void corpseRemoved(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            IsoBuilding building = square == null ? null : square.getBuilding();
            int col = x - this.wx * 8;
            int row = y - this.wy * 8;
            this.levelData[z + 32].corpseRemoved(col, row, building);
        }

        public void removeFromWorld() {
            for (int z = 0; z < this.levelData.length; z++) {
                FliesSound.ChunkLevelData levelData1 = this.levelData[z];

                for (int i = 0; i < 4; i++) {
                    FliesSound.FadeEmitter fadeEmitter = levelData1.emitters[i];
                    if (fadeEmitter != null && fadeEmitter.emitter != null) {
                        fadeEmitter.emitter.stopAll();
                    }
                }

                levelData1.Reset();
            }
        }

        private void Reset() {
            for (int z = 0; z < this.levelData.length; z++) {
                this.levelData[z].Reset();
            }
        }
    }

    private final class ChunkLevelData {
        int corpseCount;
        HashMap<IsoBuilding, Integer> buildingCorpseCount;
        final FliesSound.FadeEmitter[] emitters;

        ChunkLevelData() {
            Objects.requireNonNull(FliesSound.this);
            super();
            this.emitters = new FliesSound.FadeEmitter[4];
        }

        void corpseAdded(int col, int row, IsoBuilding building) {
            if (building == null) {
                this.corpseCount++;
            } else {
                if (this.buildingCorpseCount == null) {
                    this.buildingCorpseCount = new HashMap<>();
                }

                Integer count = this.buildingCorpseCount.get(building);
                if (count == null) {
                    this.buildingCorpseCount.put(building, 1);
                } else {
                    this.buildingCorpseCount.put(building, count + 1);
                }
            }
        }

        void corpseRemoved(int col, int row, IsoBuilding building) {
            if (building == null) {
                this.corpseCount--;
            } else if (this.buildingCorpseCount != null) {
                Integer count = this.buildingCorpseCount.get(building);
                if (count != null) {
                    if (count > 1) {
                        this.buildingCorpseCount.put(building, count - 1);
                    } else {
                        this.buildingCorpseCount.remove(building);
                    }
                }
            }
        }

        IsoGridSquare calcSoundPos(int wx, int wy, int z, IsoBuilding building) {
            int CPW = 8;
            IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(wx * 8, wy * 8, z);
            if (chunk == null) {
                return null;
            } else {
                int tempSquaresCount = 0;

                for (int cy = 0; cy < 8; cy++) {
                    for (int cx = 0; cx < 8; cx++) {
                        IsoGridSquare sq = chunk.getGridSquare(cx, cy, z);
                        if (sq != null && !sq.getStaticMovingObjects().isEmpty() && sq.getBuilding() == building) {
                            FliesSound.tempSquares[tempSquaresCount++] = sq;
                        }
                    }
                }

                return tempSquaresCount > 0 ? FliesSound.tempSquares[tempSquaresCount / 2] : null;
            }
        }

        void update(int wx, int wy, int z, IsoPlayer player) {
            int _corpseCount = FliesSound.this.getCorpseCount(wx, wy, z, player.getCurrentBuilding());
            if (player.hasTrait(CharacterTrait.DEAF)) {
                _corpseCount = 0;
            }

            if (BodyDamage.getSicknessFromCorpsesRate(_corpseCount) > ZomboidGlobals.foodSicknessDecrease) {
                IsoBuilding building = player.getCurrentBuilding();
                IsoGridSquare soundSq = this.calcSoundPos(wx, wy, z, building);
                if (soundSq == null) {
                    return;
                }

                if (this.emitters[player.playerIndex] == null) {
                    this.emitters[player.playerIndex] = new FliesSound.FadeEmitter();
                }

                FliesSound.FadeEmitter fadeEmitter = this.emitters[player.playerIndex];
                float targetVolume = 1.0F;
                if (fadeEmitter.emitter == null) {
                    fadeEmitter.emitter = IsoWorld.instance.getFreeEmitter(soundSq.x, soundSq.y, z);
                    fadeEmitter.emitter.playSoundLoopedImpl("CorpseFlies");
                    fadeEmitter.emitter.setVolumeAll(0.0F);
                    fadeEmitter.volume = 0.0F;
                    FliesSound.this.fadeEmitters.add(fadeEmitter);
                } else {
                    fadeEmitter.sq.setHasFlies(false);
                    fadeEmitter.emitter.setPos(soundSq.x, soundSq.y, z);
                    if (fadeEmitter.targetVolume != 1.0F && !FliesSound.this.fadeEmitters.contains(fadeEmitter)) {
                        FliesSound.this.fadeEmitters.add(fadeEmitter);
                    }
                }

                fadeEmitter.targetVolume = 1.0F;
                fadeEmitter.sq = soundSq;
                soundSq.setHasFlies(true);
            } else {
                FliesSound.FadeEmitter fadeEmitter = this.emitters[player.playerIndex];
                if (fadeEmitter != null && fadeEmitter.emitter != null) {
                    if (!FliesSound.this.fadeEmitters.contains(fadeEmitter)) {
                        FliesSound.this.fadeEmitters.add(fadeEmitter);
                    }

                    if (player.hasTrait(CharacterTrait.DEAF)) {
                        fadeEmitter.volume = 0.0F;
                    }

                    fadeEmitter.targetVolume = 0.0F;
                    fadeEmitter.sq.setHasFlies(false);
                }
            }
        }

        void deref(IsoPlayer player) {
            int pn = player.playerIndex;
            if (this.emitters[pn] != null && this.emitters[pn].emitter != null) {
                if (!FliesSound.this.fadeEmitters.contains(this.emitters[pn])) {
                    FliesSound.this.fadeEmitters.add(this.emitters[pn]);
                }

                this.emitters[pn].targetVolume = 0.0F;
                this.emitters[pn].sq.setHasFlies(false);
            }
        }

        void Reset() {
            this.corpseCount = 0;
            if (this.buildingCorpseCount != null) {
                this.buildingCorpseCount.clear();
            }

            for (int i = 0; i < 4; i++) {
                if (this.emitters[i] != null) {
                    this.emitters[i].Reset();
                }
            }
        }
    }

    private static final class FadeEmitter {
        private static final float FADE_IN_RATE = 0.01F;
        private static final float FADE_OUT_RATE = -0.01F;
        BaseSoundEmitter emitter;
        float volume = 1.0F;
        float targetVolume = 1.0F;
        IsoGridSquare sq;

        boolean update() {
            if (this.emitter == null) {
                return true;
            } else {
                if (this.volume < this.targetVolume) {
                    this.volume = this.volume + 0.01F * GameTime.getInstance().getThirtyFPSMultiplier();
                    if (this.volume >= this.targetVolume) {
                        this.volume = this.targetVolume;
                        return true;
                    }
                } else {
                    this.volume = this.volume + -0.01F * GameTime.getInstance().getThirtyFPSMultiplier();
                    if (this.volume <= 0.0F) {
                        this.volume = 0.0F;
                        this.emitter.stopAll();
                        this.emitter = null;
                        return true;
                    }
                }

                this.emitter.setVolumeAll(this.volume);
                return false;
            }
        }

        void Reset() {
            this.emitter = null;
            this.volume = 1.0F;
            this.targetVolume = 1.0F;
            this.sq = null;
        }
    }

    private final class PlayerData {
        int wx;
        int wy;
        int z;
        IsoBuilding building;
        boolean forceUpdate;
        boolean deaf;
        final ArrayList<FliesSound.ChunkLevelData> refs;
        final ArrayList<FliesSound.ChunkLevelData> refsPrev;

        PlayerData() {
            Objects.requireNonNull(FliesSound.this);
            super();
            this.wx = Integer.MIN_VALUE;
            this.wy = Integer.MIN_VALUE;
            this.z = Integer.MIN_VALUE;
            this.refs = new ArrayList<>();
            this.refsPrev = new ArrayList<>();
        }

        boolean isSameLocation(IsoPlayer player) {
            IsoGridSquare playerSq = player.getCurrentSquare();
            if (playerSq != null && playerSq.getBuilding() != this.building) {
                return false;
            } else {
                int DIM = 8;
                return PZMath.fastfloor(player.getX()) / 8 == this.wx
                    && PZMath.fastfloor(player.getY()) / 8 == this.wy
                    && PZMath.fastfloor(player.getZ()) == this.z;
            }
        }

        void update(IsoPlayer player) {
            if (this.deaf != player.hasTrait(CharacterTrait.DEAF)) {
                this.forceUpdate = true;
                this.deaf = player.hasTrait(CharacterTrait.DEAF);
            }

            if (this.forceUpdate || !this.isSameLocation(player)) {
                this.forceUpdate = false;
                int DIM = 8;
                IsoGridSquare playerSq = player.getCurrentSquare();
                this.wx = playerSq.getX() / 8;
                this.wy = playerSq.getY() / 8;
                this.z = playerSq.getZ();
                this.building = playerSq.getBuilding();
                this.refs.clear();

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        FliesSound.ChunkData chunkData = FliesSound.this.getChunkData((this.wx + dx) * 8, (this.wy + dy) * 8);
                        if (chunkData != null) {
                            FliesSound.ChunkLevelData levelData = chunkData.levelData[this.z + 32];
                            levelData.update(this.wx + dx, this.wy + dy, this.z, player);
                            this.refs.add(levelData);
                        }
                    }
                }

                for (int i = 0; i < this.refsPrev.size(); i++) {
                    FliesSound.ChunkLevelData levelData = this.refsPrev.get(i);
                    if (!this.refs.contains(levelData)) {
                        levelData.deref(player);
                    }
                }

                this.refsPrev.clear();
                PZArrayUtil.addAll(this.refsPrev, this.refs);
            }
        }

        void Reset() {
            this.wx = this.wy = this.z = Integer.MIN_VALUE;
            this.building = null;
            this.forceUpdate = false;
            this.refs.clear();
            this.refsPrev.clear();
        }
    }
}
