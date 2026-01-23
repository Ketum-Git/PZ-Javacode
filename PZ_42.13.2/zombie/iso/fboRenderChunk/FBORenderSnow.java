// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameProfiler;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.erosion.utils.Noise2D;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.weather.ClimateManager;

public final class FBORenderSnow {
    private static FBORenderSnow instance;
    private static final int NoiseGridSize = 256;
    private static final int ChunkSnowGridSize = 10;
    private boolean hasSetupSnowGrid;
    private FBORenderSnow.SnowGridTiles snowGridTilesSquare;
    private FBORenderSnow.SnowGridTiles[] snowGridTilesStrip;
    private FBORenderSnow.SnowGridTiles[] snowGridTilesEdge;
    private FBORenderSnow.SnowGridTiles[] snowGridTilesCove;
    private FBORenderSnow.SnowGridTiles snowGridTilesEnclosed;
    private int snowFirstNonSquare = -1;
    private final boolean[] snowTileHasSeamE = new boolean[48];
    private final boolean[] snowTileHasSeamS = new boolean[48];
    private final HashMap<String, Integer> snowTileNameToTilesetIndex = new HashMap<>();
    private final Noise2D snowNoise2d = new Noise2D();
    private FBORenderSnow.SnowGrid snowGridCur;
    private int snowFracTarget;
    private static final int SNOWSHORE_NONE = 0;
    private static final int SNOWSHORE_N = 1;
    private static final int SNOWSHORE_E = 2;
    private static final int SNOWSHORE_S = 4;
    private static final int SNOWSHORE_W = 8;

    public static FBORenderSnow getInstance() {
        if (instance == null) {
            instance = new FBORenderSnow();
        }

        return instance;
    }

    private void initSnowGrid() {
        this.snowNoise2d.reset();
        this.snowNoise2d.addLayer(16, 0.5F, 3.0F);
        this.snowNoise2d.addLayer(32, 2.0F, 5.0F);
        this.snowNoise2d.addLayer(64, 5.0F, 8.0F);
        byte id = 0;
        this.snowGridTilesSquare = new FBORenderSnow.SnowGridTiles(id++);
        int base = 40;

        for (int i = 0; i < 4; i++) {
            this.snowGridTilesSquare.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (base + i)));
        }

        this.snowGridTilesEnclosed = new FBORenderSnow.SnowGridTiles(id++);
        int var7 = 0;

        for (int i = 0; i < 4; i++) {
            this.snowGridTilesEnclosed.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var7 + i)));
        }

        this.snowGridTilesCove = new FBORenderSnow.SnowGridTiles[4];

        for (int i = 0; i < 4; i++) {
            this.snowGridTilesCove[i] = new FBORenderSnow.SnowGridTiles(id++);
            if (i == 0) {
                var7 = 7;
            }

            if (i == 2) {
                var7 = 4;
            }

            if (i == 1) {
                var7 = 5;
            }

            if (i == 3) {
                var7 = 6;
            }

            for (int j = 0; j < 3; j++) {
                this.snowGridTilesCove[i].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var7 + j * 4)));
            }
        }

        this.snowFirstNonSquare = id;
        this.snowGridTilesEdge = new FBORenderSnow.SnowGridTiles[4];

        for (int i = 0; i < 4; i++) {
            this.snowGridTilesEdge[i] = new FBORenderSnow.SnowGridTiles(id++);
            if (i == 0) {
                var7 = 16;
            }

            if (i == 2) {
                var7 = 18;
            }

            if (i == 1) {
                var7 = 17;
            }

            if (i == 3) {
                var7 = 19;
            }

            for (int j = 0; j < 3; j++) {
                this.snowGridTilesEdge[i].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var7 + j * 4)));
            }
        }

        this.snowGridTilesStrip = new FBORenderSnow.SnowGridTiles[4];

        for (int i = 0; i < 4; i++) {
            this.snowGridTilesStrip[i] = new FBORenderSnow.SnowGridTiles(id++);
            if (i == 0) {
                var7 = 28;
            }

            if (i == 2) {
                var7 = 29;
            }

            if (i == 1) {
                var7 = 31;
            }

            if (i == 3) {
                var7 = 30;
            }

            for (int j = 0; j < 3; j++) {
                this.snowGridTilesStrip[i].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var7 + j * 4)));
            }
        }

        this.snowTileHasSeamE[0] = this.snowTileHasSeamE[1] = this.snowTileHasSeamE[2] = this.snowTileHasSeamE[3] = this.snowTileHasSeamE[5] = this.snowTileHasSeamE[6] = this.snowTileHasSeamE[7] = true;
        this.snowTileHasSeamE[9] = this.snowTileHasSeamE[10] = this.snowTileHasSeamE[11] = this.snowTileHasSeamE[13] = this.snowTileHasSeamE[14] = this.snowTileHasSeamE[15] = true;
        this.snowTileHasSeamE[17] = this.snowTileHasSeamE[19] = this.snowTileHasSeamE[21] = this.snowTileHasSeamE[23] = true;
        this.snowTileHasSeamE[25] = this.snowTileHasSeamE[27] = this.snowTileHasSeamE[30] = true;
        this.snowTileHasSeamE[34] = this.snowTileHasSeamE[38] = true;
        this.snowTileHasSeamE[40] = this.snowTileHasSeamE[41] = this.snowTileHasSeamE[42] = this.snowTileHasSeamE[43] = this.snowTileHasSeamE[44] = this.snowTileHasSeamE[45] = this.snowTileHasSeamE[46] = this.snowTileHasSeamE[47] = true;
        this.snowTileHasSeamS[0] = this.snowTileHasSeamS[1] = this.snowTileHasSeamS[2] = this.snowTileHasSeamS[3] = this.snowTileHasSeamS[4] = this.snowTileHasSeamS[5] = this.snowTileHasSeamS[6] = true;
        this.snowTileHasSeamS[8] = this.snowTileHasSeamS[9] = this.snowTileHasSeamS[10] = this.snowTileHasSeamS[12] = this.snowTileHasSeamS[13] = this.snowTileHasSeamS[14] = true;
        this.snowTileHasSeamS[17] = this.snowTileHasSeamS[18] = this.snowTileHasSeamS[21] = this.snowTileHasSeamS[22] = true;
        this.snowTileHasSeamS[25] = this.snowTileHasSeamS[26] = this.snowTileHasSeamS[31] = true;
        this.snowTileHasSeamS[35] = this.snowTileHasSeamS[39] = true;
        this.snowTileHasSeamS[40] = this.snowTileHasSeamS[41] = this.snowTileHasSeamS[42] = this.snowTileHasSeamS[43] = this.snowTileHasSeamS[44] = this.snowTileHasSeamS[45] = this.snowTileHasSeamS[46] = this.snowTileHasSeamS[47] = true;

        for (int i = 0; i < 48; i++) {
            this.snowTileNameToTilesetIndex.put("e_newsnow_ground_1_" + i, i);
        }
    }

    private FBORenderSnow.ChunkLevel getChunkLevel(int playerIndex, IsoChunk chunk, int level) {
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        return level == 0 ? renderLevels.snowLevelZero : renderLevels.snowLevelNotZero;
    }

    private FBORenderSnow.SnowGrid setChunkSnowGrid(int playerIndex, IsoChunk chunk, int level, FBORenderSnow.SnowGrid snowGrid) {
        FBORenderSnow.ChunkLevel chunkLevel = this.getChunkLevel(playerIndex, chunk, level);
        chunkLevel.snowGrid = snowGrid;
        return snowGrid;
    }

    private FBORenderSnow.SnowGrid getChunkSnowGrid(int playerIndex, IsoChunk chunk, int level) {
        FBORenderSnow.ChunkLevel chunkLevel = this.getChunkLevel(playerIndex, chunk, level);
        return chunkLevel.snowGrid;
    }

    private void updateSnow(IsoChunk chunk, int level, int fracTarget) {
        int worldX = chunk.wx * 8 - 1;
        int worldY = chunk.wy * 8 - 1;
        int playerIndex = IsoCamera.frameState.playerIndex;
        FBORenderSnow.ChunkLevel chunkLevel = this.getChunkLevel(playerIndex, chunk, level);
        if (chunkLevel.snowGrid == null) {
            chunkLevel.snowGrid = new FBORenderSnow.SnowGrid(worldX, worldY, level, fracTarget);
            chunkLevel.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
            this.snowGridCur = chunkLevel.snowGrid;
        } else {
            this.snowGridCur = chunkLevel.snowGrid;
            if (chunk.adjacentChunkLoadedCounter != chunkLevel.adjacentChunkLoadedCounter) {
                chunkLevel.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
                this.snowGridCur.frac = -1;
            }

            if (worldX != this.snowGridCur.worldX || worldY != this.snowGridCur.worldY || fracTarget != this.snowGridCur.frac) {
                this.snowGridCur.init(worldX, worldY, level, fracTarget);
            }
        }
    }

    public boolean gridSquareIsSnow(int x, int y, int z) {
        if (IsoWorld.instance.currentCell.getSnowTarget() <= 0) {
            return false;
        } else {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (square != null && square.chunk != null) {
                int playerIndex = IsoPlayer.getPlayerIndex();
                this.snowGridCur = this.getChunkSnowGrid(playerIndex, square.chunk, z);
                if (this.snowGridCur != null) {
                    if (!square.getProperties().has(IsoFlagType.solidfloor)) {
                        return false;
                    } else if (!square.getProperties().has(IsoFlagType.water) && (square.getWater() == null || !square.getWater().isValid())) {
                        if (square.getProperties().has(IsoFlagType.exterior) && square.room == null && !square.isInARoom()) {
                            int snowX = this.snowGridCur.worldToSelfX(square.getX());
                            int snowY = this.snowGridCur.worldToSelfY(square.getY());
                            return this.snowGridCur.check(snowX, snowY);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public void RenderSnow(IsoChunk chunk, int zza) {
        if (DebugOptions.instance.weather.snow.getValue()) {
            this.snowFracTarget = IsoWorld.instance.currentCell.getSnowTarget();
            this.updateSnow(chunk, zza, this.snowFracTarget);
            FBORenderSnow.SnowGrid snowGridCur = this.snowGridCur;
            if (snowGridCur != null) {
                if (snowGridCur.frac > 0) {
                    float alphaCur = 1.0F;
                    Shader floorRenderShader = null;
                    if (DebugOptions.instance.terrain.renderTiles.useShaders.getValue()) {
                        floorRenderShader = IsoCell.floorRenderShader;
                    }

                    FloorShaperAttachedSprites.instance.setShore(false);
                    FloorShaperDiamond.instance.setShore(false);
                    IndieGL.StartShader(floorRenderShader, IsoCamera.frameState.playerIndex);
                    int camOffX = (int)IsoCamera.frameState.offX;
                    int camOffY = (int)IsoCamera.frameState.offY;

                    for (int i = 0; i < IsoCell.SolidFloor.size(); i++) {
                        IsoGridSquare square = IsoCell.SolidFloor.get(i);
                        if (square.room == null && square.getProperties().has(IsoFlagType.exterior) && square.getProperties().has(IsoFlagType.solidfloor)) {
                            int shore;
                            if (square.getProperties().has(IsoFlagType.water) || square.getWater() != null && square.getWater().isValid()) {
                                shore = this.getShoreInt(square);
                                if (shore == 0) {
                                    continue;
                                }
                            } else {
                                shore = 0;
                            }

                            int snowX = snowGridCur.worldToSelfX(square.getX());
                            int snowY = snowGridCur.worldToSelfY(square.getY());
                            float sx = IsoUtils.XToScreen(square.getX(), square.getY(), zza, 0);
                            float sy = IsoUtils.YToScreen(square.getX(), square.getY(), zza, 0);
                            sx -= camOffX;
                            sy -= camOffY;
                            if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                                sx = IsoUtils.XToScreen(PZMath.coordmodulo(square.getX(), 8), PZMath.coordmodulo(square.getY(), 8), zza, 0);
                                sy = IsoUtils.YToScreen(PZMath.coordmodulo(square.getX(), 8), PZMath.coordmodulo(square.getY(), 8), zza, 0);
                                sx += FBORenderChunkManager.instance.getXOffset();
                                sy += FBORenderChunkManager.instance.getYOffset();
                            }

                            float offsetX = 32 * Core.tileScale;
                            float offsetY = 96 * Core.tileScale;
                            sx -= offsetX;
                            sy -= offsetY;
                            if (square.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                                sy -= 32 * Core.tileScale;
                            }

                            if (square.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                                sy -= 64 * Core.tileScale;
                            }

                            this.setVertColors(square);

                            for (int s = 0; s < 2; s++) {
                                this.renderSnowTileGeneral(snowGridCur, 1.0F, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                            }
                        }
                    }

                    IndieGL.StartShader(null);
                }
            }
        }
    }

    private void setVertColors(IsoGridSquare square) {
        this.setVertColors(square, 0, 1, 2, 3);
    }

    private void setVertColors(IsoGridSquare square, int v0, int v1, int v2, int v3) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int col0 = square.getVertLight(v0, playerIndex);
        int col1 = square.getVertLight(v1, playerIndex);
        int col2 = square.getVertLight(v2, playerIndex);
        int col3 = square.getVertLight(v3, playerIndex);
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
            col0 = -65536;
            col1 = -65536;
            col2 = -16776961;
            col3 = -16776961;
        }

        FloorShaperAttachedSprites.instance.setVertColors(col0, col1, col2, col3);
        FloorShaperDiamond.instance.setVertColors(col0, col1, col2, col3);
    }

    private void renderSnowTileGeneral(
        FBORenderSnow.SnowGrid snowGrid, float alpha, IsoGridSquare square, int shore, int snowX, int snowY, int sx, int sy, int s
    ) {
        if (!(alpha <= 0.0F)) {
            Texture tex = snowGrid.grid[snowX][snowY][s];
            if (tex != null) {
                if (s == 0) {
                    this.renderSnowTile(snowGrid, snowX, snowY, s, square, shore, tex, sx, sy, alpha);
                } else if (shore == 0) {
                    byte id = snowGrid.gridType[snowX][snowY][s];
                    this.renderSnowTileBase(tex, sx, sy, alpha, id < this.snowFirstNonSquare);
                }
            }
        }
    }

    private void renderSnowTileBase(Texture tex, int sx, int sy, float alpha, boolean square) {
        FloorShaper shaper = (FloorShaper)(square ? FloorShaperDiamond.instance : FloorShaperAttachedSprites.instance);
        shaper.setAlpha4(alpha);
        tex.render(sx, sy, tex.getWidth(), tex.getHeight(), 1.0F, 1.0F, 1.0F, alpha, shaper);
    }

    private void renderSnowTile(FBORenderSnow.SnowGrid sgrid, int gx, int gy, int s, IsoGridSquare sq, int shore, Texture tex, int sx, int sy, float alpha) {
        if (shore == 0) {
            byte id = sgrid.gridType[gx][gy][s];
            this.renderSnowTileBase(tex, sx, sy, alpha, id < this.snowFirstNonSquare);
            if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix2.getValue()) {
                Integer tilesheetIndex = this.snowTileNameToTilesetIndex.getOrDefault(tex.getName(), null);
                if (PZMath.coordmodulo(sq.y, 8) == 7 && tilesheetIndex != null && this.snowTileHasSeamS[tilesheetIndex]) {
                    IsoGridSquare squareS = sq.getAdjacentSquare(IsoDirections.S);
                    if (squareS != null && squareS.getFloor() != null) {
                        this.setVertColors(sq, 3, 2, 2, 3);
                        this.renderSnowTileBase(Texture.getSharedTexture("e_newsnow_ground_1_28"), sx - 64, sy + 32, alpha, false);
                    }
                }

                if (PZMath.coordmodulo(sq.x, 8) == 7 && tilesheetIndex != null && this.snowTileHasSeamE[tilesheetIndex]) {
                    IsoGridSquare squareE = sq.getAdjacentSquare(IsoDirections.E);
                    if (squareE != null && squareE.getFloor() != null) {
                        this.setVertColors(sq, 1, 1, 2, 2);
                        this.renderSnowTileBase(Texture.getSharedTexture("e_newsnow_ground_1_29"), sx + 64, sy + 32, alpha, false);
                    }
                }
            }
        } else {
            int count = 0;
            boolean selfIsSnowFull = sgrid.check(gx, gy);
            boolean bN = (shore & 1) == 1 && (selfIsSnowFull || sgrid.check(gx, gy - 1));
            boolean bE = (shore & 2) == 2 && (selfIsSnowFull || sgrid.check(gx + 1, gy));
            boolean bS = (shore & 4) == 4 && (selfIsSnowFull || sgrid.check(gx, gy + 1));
            boolean bW = (shore & 8) == 8 && (selfIsSnowFull || sgrid.check(gx - 1, gy));
            if (bN) {
                count++;
            }

            if (bS) {
                count++;
            }

            if (bE) {
                count++;
            }

            if (bW) {
                count++;
            }

            FBORenderSnow.SnowGridTiles sTiles = null;
            FBORenderSnow.SnowGridTiles sTiles2 = null;
            boolean square = false;
            if (count != 0) {
                if (count == 1) {
                    if (bN) {
                        sTiles = this.snowGridTilesStrip[0];
                    } else if (bS) {
                        sTiles = this.snowGridTilesStrip[1];
                    } else if (bE) {
                        sTiles = this.snowGridTilesStrip[3];
                    } else if (bW) {
                        sTiles = this.snowGridTilesStrip[2];
                    }
                } else if (count == 2) {
                    if (bN && bS) {
                        sTiles = this.snowGridTilesStrip[0];
                        sTiles2 = this.snowGridTilesStrip[1];
                    } else if (bE && bW) {
                        sTiles = this.snowGridTilesStrip[2];
                        sTiles2 = this.snowGridTilesStrip[3];
                    } else if (bN) {
                        sTiles = this.snowGridTilesEdge[bW ? 0 : 3];
                    } else if (bS) {
                        sTiles = this.snowGridTilesEdge[bW ? 2 : 1];
                    } else if (bW) {
                        sTiles = this.snowGridTilesEdge[bN ? 0 : 2];
                    } else if (bE) {
                        sTiles = this.snowGridTilesEdge[bN ? 3 : 1];
                    }
                } else if (count == 3) {
                    if (!bN) {
                        sTiles = this.snowGridTilesCove[1];
                    } else if (!bS) {
                        sTiles = this.snowGridTilesCove[0];
                    } else if (!bE) {
                        sTiles = this.snowGridTilesCove[2];
                    } else if (!bW) {
                        sTiles = this.snowGridTilesCove[3];
                    }

                    square = true;
                } else if (count == 4) {
                    sTiles = this.snowGridTilesEnclosed;
                    square = true;
                }

                if (sTiles != null) {
                    int var = (sq.getX() + sq.getY()) % sTiles.size();
                    tex = sTiles.get(var);
                    if (tex != null) {
                        this.renderSnowTileBase(tex, sx, sy, alpha, square);
                    }

                    if (sTiles2 != null) {
                        tex = sTiles2.get(var);
                        if (tex != null) {
                            this.renderSnowTileBase(tex, sx, sy, alpha, false);
                        }
                    }
                }
            }
        }
    }

    private int getShoreInt(IsoGridSquare sq) {
        int shore = 0;
        if (this.isSnowShore(sq, 0, -1)) {
            shore |= 1;
        }

        if (this.isSnowShore(sq, 1, 0)) {
            shore |= 2;
        }

        if (this.isSnowShore(sq, 0, 1)) {
            shore |= 4;
        }

        if (this.isSnowShore(sq, -1, 0)) {
            shore |= 8;
        }

        return shore;
    }

    private boolean isSnowShore(IsoGridSquare sq, int ox, int oy) {
        IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare(sq.getX() + ox, sq.getY() + oy, 0);
        boolean bShore = square != null && square.getWater() != null && square.getWater().isValid();
        return square != null && !square.getProperties().has(IsoFlagType.water) && !bShore;
    }

    public static final class ChunkLevel {
        public final IsoChunk chunk;
        private FBORenderSnow.SnowGrid snowGrid;
        public int adjacentChunkLoadedCounter = -1;

        public ChunkLevel(IsoChunk chunk) {
            this.chunk = chunk;
        }
    }

    private static final class SnowGrid {
        public int worldX;
        public int worldY;
        public int w = 10;
        public int h = 10;
        public int frac;
        public static final int N = 0;
        public static final int S = 1;
        public static final int W = 2;
        public static final int E = 3;
        public static final int A = 0;
        public static final int B = 1;
        public final Texture[][][] grid = new Texture[this.w][this.h][2];
        public final byte[][][] gridType = new byte[this.w][this.h][2];

        public SnowGrid(int worldX, int worldY, int level, int frac) {
            this.init(worldX, worldY, level, frac);
        }

        public FBORenderSnow.SnowGrid init(int worldX, int worldY, int level, int frac) {
            if (!FBORenderSnow.instance.hasSetupSnowGrid) {
                FBORenderSnow.instance.initSnowGrid();
                FBORenderSnow.instance.hasSetupSnowGrid = true;
            }

            FBORenderSnow.instance.snowGridTilesSquare.resetCounter();
            FBORenderSnow.instance.snowGridTilesEnclosed.resetCounter();

            for (int i = 0; i < 4; i++) {
                FBORenderSnow.instance.snowGridTilesCove[i].resetCounter();
                FBORenderSnow.instance.snowGridTilesEdge[i].resetCounter();
                FBORenderSnow.instance.snowGridTilesStrip[i].resetCounter();
            }

            this.worldX = worldX;
            this.worldY = worldY;
            this.frac = frac;
            boolean isWinter = ClimateManager.getInstance().getSeason().isSeason(5);
            if (Core.getInstance().isForceSnow()) {
                isWinter = true;
            }

            if (!isWinter) {
                for (int y = 0; y < this.h; y++) {
                    for (int x = 0; x < this.w; x++) {
                        for (int s = 0; s < 2; s++) {
                            this.grid[x][y][s] = null;
                            this.gridType[x][y][s] = -1;
                        }
                    }
                }

                return this;
            } else {
                Noise2D noiseMain = FBORenderSnow.instance.snowNoise2d;
                GameProfiler profiler = GameProfiler.getInstance();

                try (GameProfiler.ProfileArea ignored = profiler.profile("Noise")) {
                    for (int y = 0; y < this.h; y++) {
                        for (int x = 0; x < this.w; x++) {
                            for (int s = 0; s < 2; s++) {
                                this.grid[x][y][s] = null;
                                this.gridType[x][y][s] = -1;
                            }

                            if (level == 0) {
                                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.worldX + x, this.worldY + y, level);
                                if (square == null) {
                                    continue;
                                }

                                boolean bAdjacentToWater = false;

                                for (int d = 0; d < 8; d++) {
                                    IsoDirections dir = IsoDirections.fromIndex(d);
                                    IsoGridSquare square1 = square.getAdjacentSquare(dir);
                                    if (square1 != null && square1.getWater() != null && square1.getWater().isValid()) {
                                        bAdjacentToWater = true;
                                        break;
                                    }
                                }

                                if (bAdjacentToWater) {
                                    continue;
                                }
                            }

                            int noiseX = this.worldToNoiseX(this.worldX + x);
                            int noiseY = this.worldToNoiseY(this.worldY + y);
                            if (noiseMain.layeredNoise(noiseX / 10.0F, noiseY / 10.0F) <= frac / 100.0F) {
                                this.grid[x][y][0] = FBORenderSnow.instance.snowGridTilesSquare.getAt(this.worldX + x, this.worldY + y);
                                this.gridType[x][y][0] = FBORenderSnow.instance.snowGridTilesSquare.id;
                            }
                        }
                    }
                }

                try (GameProfiler.ProfileArea ignored = profiler.profile("Check Set")) {
                    for (int y = 0; y < this.h; y++) {
                        for (int x = 0; x < this.w; x++) {
                            Texture tex = this.grid[x][y][0];
                            if (tex == null) {
                                boolean bN = this.check(x, y - 1);
                                boolean bS = this.check(x, y + 1);
                                boolean bW = this.check(x - 1, y);
                                boolean bE = this.check(x + 1, y);
                                int count = 0;
                                if (bN) {
                                    count++;
                                }

                                if (bS) {
                                    count++;
                                }

                                if (bE) {
                                    count++;
                                }

                                if (bW) {
                                    count++;
                                }

                                if (count != 0) {
                                    if (count == 1) {
                                        if (bN) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesStrip[0]);
                                        } else if (bS) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesStrip[1]);
                                        } else if (bE) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesStrip[3]);
                                        } else if (bW) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesStrip[2]);
                                        }
                                    } else if (count == 2) {
                                        if (bN && bS) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesStrip[0]);
                                            this.set(x, y, 1, FBORenderSnow.instance.snowGridTilesStrip[1]);
                                        } else if (bE && bW) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesStrip[2]);
                                            this.set(x, y, 1, FBORenderSnow.instance.snowGridTilesStrip[3]);
                                        } else if (bN) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesEdge[bW ? 0 : 3]);
                                        } else if (bS) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesEdge[bW ? 2 : 1]);
                                        } else if (bW) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesEdge[bN ? 0 : 2]);
                                        } else if (bE) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesEdge[bN ? 3 : 1]);
                                        }
                                    } else if (count == 3) {
                                        if (!bN) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesCove[1]);
                                        } else if (!bS) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesCove[0]);
                                        } else if (!bE) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesCove[2]);
                                        } else if (!bW) {
                                            this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesCove[3]);
                                        }
                                    } else if (count == 4) {
                                        this.set(x, y, 0, FBORenderSnow.instance.snowGridTilesEnclosed);
                                    }
                                }
                            }
                        }
                    }
                }

                return this;
            }
        }

        int worldToSelfX(int worldX) {
            return worldX - this.worldX;
        }

        int worldToSelfY(int worldY) {
            return worldY - this.worldY;
        }

        int worldToNoiseX(int worldX) {
            return PZMath.coordmodulo(worldX, 256);
        }

        int worldToNoiseY(int worldY) {
            return PZMath.coordmodulo(worldY, 256);
        }

        public boolean check(int x, int y) {
            if (x == this.w) {
                x = 0;
            }

            if (x == -1) {
                x = this.w - 1;
            }

            if (y == this.h) {
                y = 0;
            }

            if (y == -1) {
                y = this.h - 1;
            }

            if (x < 0 || x >= this.w) {
                return false;
            } else if (y >= 0 && y < this.h) {
                Texture tex = this.grid[x][y][0];
                return FBORenderSnow.instance.snowGridTilesSquare.contains(tex);
            } else {
                return false;
            }
        }

        public void set(int x, int y, int t, FBORenderSnow.SnowGridTiles tiles) {
            if (x == this.w) {
                x = 0;
            }

            if (x == -1) {
                x = this.w - 1;
            }

            if (y == this.h) {
                y = 0;
            }

            if (y == -1) {
                y = this.h - 1;
            }

            if (x >= 0 && x < this.w) {
                if (y >= 0 && y < this.h) {
                    this.grid[x][y][t] = tiles.getAt(this.worldX + x, this.worldY + y);
                    this.gridType[x][y][t] = tiles.id;
                }
            }
        }
    }

    private static final class SnowGridTiles {
        byte id = -1;
        int counter = -1;
        final ArrayList<Texture> textures = new ArrayList<>();

        public SnowGridTiles(byte id) {
            this.id = id;
        }

        void add(Texture tex) {
            this.textures.add(tex);
        }

        Texture getAt(int worldX, int worldY) {
            worldX -= IsoWorld.instance.metaGrid.getMinX() * 256;
            worldY -= IsoWorld.instance.metaGrid.getMinX() * 256;
            int index = (worldX + worldY) % this.textures.size();
            return this.textures.get(index);
        }

        Texture getNext() {
            this.counter++;
            if (this.counter >= this.textures.size()) {
                this.counter = 0;
            }

            return this.textures.get(this.counter);
        }

        Texture get(int index) {
            return this.textures.get(index);
        }

        int size() {
            return this.textures.size();
        }

        Texture getRand() {
            return this.textures.get(Rand.Next(4));
        }

        boolean contains(Texture other) {
            return this.textures.contains(other);
        }

        void resetCounter() {
            this.counter = 0;
        }
    }
}
