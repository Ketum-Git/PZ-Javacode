// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.HashMap;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.debug.DebugType;
import zombie.iso.areas.IsoBuilding;

public final class CorpseCount {
    public static final CorpseCount instance = new CorpseCount();
    public static int maxCorpseCount = 25;

    public void chunkLoaded(IsoChunk chunk) {
        if (chunk.corpseCount == null) {
            chunk.corpseCount = new CorpseCount.ChunkData(chunk);
        }

        chunk.corpseCount.reset();
    }

    public void corpseAdded(int x, int y, int z) {
        if (z >= -32 && z <= 31) {
            CorpseCount.ChunkData chunkData = this.getChunkData(x, y);
            if (chunkData != null) {
                chunkData.corpseAdded(x, y, z);
            }
        } else {
            DebugType.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
        }
    }

    public void corpseRemoved(int x, int y, int z) {
        if (z >= -32 && z <= 31) {
            CorpseCount.ChunkData chunkData = this.getChunkData(x, y);
            if (chunkData != null) {
                chunkData.corpseRemoved(x, y, z);
            }
        } else {
            DebugType.General.error("invalid z-coordinate %d,%d,%d", x, y, z);
        }
    }

    public int getCorpseCount(IsoGameCharacter chr) {
        if (chr != null && chr.getCurrentSquare() != null) {
            int DIM = 8;
            return this.getCorpseCount(PZMath.coorddivision(chr.getXi(), 8), PZMath.coorddivision(chr.getYi(), 8), chr.getZi(), chr.getBuilding());
        } else {
            return 0;
        }
    }

    public int getCorpseCount(int wx, int wy, int z, IsoBuilding building) {
        int count = 0;
        int DIM = 8;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                CorpseCount.ChunkData chunkData = this.getChunkData((wx + dx) * 8, (wy + dy) * 8);
                if (chunkData != null) {
                    CorpseCount.ChunkLevelData levelData = chunkData.getLevelData(z);
                    if (levelData != null) {
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
        }

        if (SandboxOptions.instance.zombieHealthImpact.getValue()) {
            int x = wx * 8;
            int y = wy * 8;
            int offset = 12;

            for (int dy = -12; dy <= 12; dy++) {
                for (int dxx = -12; dxx <= 12; dxx++) {
                    IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(x + dxx, y + dy, z);
                    if (sq != null) {
                        if (building == sq.getBuilding()) {
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

    private CorpseCount.ChunkData getChunkData(int x, int y) {
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, 0);
        return chunk != null ? chunk.corpseCount : null;
    }

    public boolean hasBuildingCorpseCount(IsoChunk chunk, int z, IsoBuilding building) {
        CorpseCount.ChunkLevelData var5 = chunk.corpseCount.getLevelData(z);
        return var5 instanceof CorpseCount.ChunkLevelData && var5.buildingCorpseCount != null ? var5.buildingCorpseCount.containsKey(building) : false;
    }

    public void reset() {
    }

    public static final class ChunkData {
        private final IsoChunk chunk;

        private ChunkData(IsoChunk chunk) {
            this.chunk = chunk;
        }

        private CorpseCount.ChunkLevelData getOrCreateLevelData(int z) {
            IsoChunkLevel levelData = this.chunk.getLevelData(z);
            if (levelData == null) {
                return null;
            } else {
                if (levelData.corpseCount == null) {
                    levelData.corpseCount = new CorpseCount.ChunkLevelData();
                }

                return levelData.corpseCount;
            }
        }

        private CorpseCount.ChunkLevelData getLevelData(int z) {
            IsoChunkLevel levelData = this.chunk.getLevelData(z);
            return levelData == null ? null : levelData.corpseCount;
        }

        private void corpseAdded(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            IsoBuilding building = square == null ? null : square.getBuilding();
            CorpseCount.ChunkLevelData levelData = this.getOrCreateLevelData(z);
            if (levelData != null) {
                levelData.corpseAdded(building);
            }
        }

        private void corpseRemoved(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            IsoBuilding building = square == null ? null : square.getBuilding();
            CorpseCount.ChunkLevelData levelData = this.getOrCreateLevelData(z);
            if (levelData != null) {
                levelData.corpseRemoved(building);
            }
        }

        public void removeFromWorld() {
            for (int z = this.chunk.getMinLevel(); z <= this.chunk.getMaxLevel(); z++) {
                CorpseCount.ChunkLevelData var3 = this.getLevelData(z);
                if (var3 instanceof CorpseCount.ChunkLevelData) {
                    var3.reset();
                }
            }
        }

        private void reset() {
            for (int z = this.chunk.getMinLevel(); z <= this.chunk.getMaxLevel(); z++) {
                CorpseCount.ChunkLevelData var3 = this.getLevelData(z);
                if (var3 instanceof CorpseCount.ChunkLevelData) {
                    var3.reset();
                }
            }
        }
    }

    public static final class ChunkLevelData {
        int corpseCount;
        HashMap<IsoBuilding, Integer> buildingCorpseCount;

        ChunkLevelData() {
        }

        void corpseAdded(IsoBuilding building) {
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

        void corpseRemoved(IsoBuilding building) {
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

        void reset() {
            this.corpseCount = 0;
            if (this.buildingCorpseCount != null) {
                this.buildingCorpseCount.clear();
            }
        }
    }
}
