// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.joml.Vector2f;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.audio.FMODAmbientWalls;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.entity.util.TimSort;
import zombie.gameStates.DebugChunkState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPuddlesGeometry;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.IsoWaterGeometry;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.RoomDef;
import zombie.iso.WorldMarkers;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.tileDepth.TileSeamManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vispoly.VisibilityPolygon2;

public final class FBORenderCell {
    public static final FBORenderCell instance = new FBORenderCell();
    private static final float BLACK_OUT_DIST = 10.0F;
    public static final boolean OUTLINE_DOUBLEDOOR_FRAMES = true;
    public static IsoObject lowestCutawayObject;
    public IsoCell cell;
    public final ArrayList<IsoGridSquare> waterSquares = new ArrayList<>();
    public final ArrayList<IsoGridSquare> waterAttachSquares = new ArrayList<>();
    public final ArrayList<IsoGridSquare> fishSplashSquares = new ArrayList<>();
    public final ArrayList<IsoMannequin> mannequinList = new ArrayList<>();
    public boolean renderAnimatedAttachments;
    public boolean renderTranslucentOnly;
    public boolean renderDebugChunkState;
    private final FBORenderCell.PerPlayerData[] perPlayerData = new FBORenderCell.PerPlayerData[4];
    private long currentTimeMillis;
    private boolean windEffects;
    private boolean waterShader;
    private int puddlesQuality = -1;
    private float puddlesValue;
    private float wetGroundValue;
    private long puddlesRedrawTimeMs;
    private float snowFracTarget;
    private final TimSort timSort = new TimSort();
    private final int maxChunksPerFrame = 5;
    private final ColorInfo defColorInfo = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    private final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType = new ArrayList<>();
    private final PZArrayList<IsoWorldInventoryObject> tempWorldInventoryObjects = new PZArrayList<>(IsoWorldInventoryObject.class, 16);
    private final PZArrayList<IsoGridSquare> tempSquares = new PZArrayList<>(IsoGridSquare.class, 64);
    private final ArrayList<IsoGameCharacter.Location> tempLocations = new ArrayList<>();
    private final ObjectPool<IsoGameCharacter.Location> locationPool = new ObjectPool<>(IsoGameCharacter.Location::new);
    private long delayedLoadingTimerMs;
    private boolean invalidateDelayedLoadingLevels;
    public static final PerformanceProfileProbe calculateRenderInfo = new PerformanceProfileProbe("FBORenderCell.calculateRenderInfo");
    public static final PerformanceProfileProbe cutaways = new PerformanceProfileProbe("FBORenderCell.cutaways");
    public static final PerformanceProfileProbe fog = new PerformanceProfileProbe("FBORenderCell.fog");
    public static final PerformanceProfileProbe puddles = new PerformanceProfileProbe("FBORenderCell.puddles");
    public static final PerformanceProfileProbe renderOneChunk = new PerformanceProfileProbe("FBORenderCell.renderOneChunk");
    public static final PerformanceProfileProbe renderOneChunkLevel = new PerformanceProfileProbe("FBORenderCell.renderOneChunkLevel");
    public static final PerformanceProfileProbe renderOneChunkLevel2 = new PerformanceProfileProbe("FBORenderCell.renderOneChunkLevel2");
    public static final PerformanceProfileProbe translucentFloor = new PerformanceProfileProbe("FBORenderCell.translucentFloor");
    public static final PerformanceProfileProbe translucentNonFloor = new PerformanceProfileProbe("FBORenderCell.translucentNonFloor");
    public static final PerformanceProfileProbe updateLighting = new PerformanceProfileProbe("FBORenderCell.updateLighting");
    public static final PerformanceProfileProbe water = new PerformanceProfileProbe("FBORenderCell.water");
    public static final PerformanceProfileProbe tilesProbe = new PerformanceProfileProbe("renderTiles");
    public static final PerformanceProfileProbe itemsProbe = new PerformanceProfileProbe("renderItemsInWorld");
    public static final PerformanceProfileProbe movingObjectsProbe = new PerformanceProfileProbe("renderMovingObjects");
    public static final PerformanceProfileProbe shadowsProbe = new PerformanceProfileProbe("renderShadows");
    public static final PerformanceProfileProbe visibilityProbe = new PerformanceProfileProbe("VisibilityPolygon2");
    public static final PerformanceProfileProbe translucentFloorObjectsProbe = new PerformanceProfileProbe("renderTranslucentFloorObjects");
    public static final PerformanceProfileProbe translucentObjectsProbe = new PerformanceProfileProbe("renderTranslucentObjects");
    public static final boolean FIX_CORPSE_CLIPPING = true;
    public static final boolean FIX_ITEM_CLIPPING = true;
    public static final boolean FIX_JUMBO_CLIPPING = true;
    private final PZArrayList<IsoChunk> sortedChunks = new PZArrayList<>(IsoChunk.class, 121);
    public static float blackedOutRoomFadeBlackness;
    public static long blackedOutRoomFadeDurationMs = 800L;

    private FBORenderCell() {
        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            this.perPlayerData[playerIndex] = new FBORenderCell.PerPlayerData(playerIndex);
        }
    }

    public void renderInternal() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        if (!PerformanceSettings.newRoofHiding) {
            if (this.cell.hideFloors[playerIndex] && this.cell.unhideFloorsCounter[playerIndex] > 0) {
                this.cell.unhideFloorsCounter[playerIndex]--;
            }

            if (this.cell.unhideFloorsCounter[playerIndex] <= 0) {
                this.cell.hideFloors[playerIndex] = false;
                this.cell.unhideFloorsCounter[playerIndex] = 60;
            }
        }

        int x1 = 0;
        int y1 = 0;
        int x2 = 0 + IsoCamera.getOffscreenWidth(playerIndex);
        int y2 = 0 + IsoCamera.getOffscreenHeight(playerIndex);
        float topLeftX = IsoUtils.XToIso(0.0F, 0.0F, 0.0F);
        float topRightY = IsoUtils.YToIso(x2, 0.0F, 0.0F);
        float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0F);
        float bottomLeftY = IsoUtils.YToIso(0.0F, y2, 6.0F);
        this.cell.minY = (int)topRightY;
        this.cell.maxY = (int)bottomLeftY;
        this.cell.minX = (int)topLeftX;
        this.cell.maxX = (int)bottomRightX;
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.occludedGridX1 = this.cell.minX;
        perPlayerData1.occludedGridY1 = this.cell.minY;
        perPlayerData1.occludedGridX2 = this.cell.maxX;
        perPlayerData1.occludedGridY2 = this.cell.maxY;
        this.cell.minX -= 2;
        this.cell.minY -= 2;
        this.cell.minX = this.cell.minX - this.cell.minX % 8;
        this.cell.minY = this.cell.minY - this.cell.minY % 8;
        this.cell.maxX = this.cell.maxX + (8 - this.cell.maxX % 8);
        this.cell.maxY = this.cell.maxY + (8 - this.cell.maxY % 8);
        this.cell.maxZ = IsoCell.maxHeight;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter == null) {
            this.cell.maxZ = 1;
        }

        if (IsoPlayer.getInstance().getZ() < 0.0F) {
            this.cell.maxZ = (int)Math.ceil(IsoPlayer.getInstance().getZ()) + 1;
        }

        if (this.cell.minX != this.cell.lastMinX || this.cell.minY != this.cell.lastMinY) {
            this.cell.lightUpdateCount = 10;
        }

        if (!PerformanceSettings.newRoofHiding) {
            IsoGridSquare currentSq = isoGameCharacter == null ? null : isoGameCharacter.getCurrentSquare();
            if (currentSq != null) {
                IsoGridSquare sq = this.cell.getGridSquare(Math.round(isoGameCharacter.getX()), Math.round(isoGameCharacter.getY()), playerZ);
                if (sq != null && this.cell.IsBehindStuff(sq)) {
                    this.cell.hideFloors[playerIndex] = true;
                }

                if (!this.cell.hideFloors[playerIndex] && currentSq.getProperties().has(IsoFlagType.hidewalls)
                    || !currentSq.getProperties().has(IsoFlagType.exterior)) {
                    this.cell.hideFloors[playerIndex] = true;
                }
            }

            if (this.cell.hideFloors[playerIndex]) {
                this.cell.maxZ = playerZ + 1;
            }
        }

        this.cell.DrawStencilMask();
        long lastPlayerWindowPeekingRoomId = this.cell.playerWindowPeekingRoomId[playerIndex];

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            this.cell.playerWindowPeekingRoomId[i] = -1L;
            IsoPlayer player2 = IsoPlayer.players[i];
            if (player2 != null) {
                IsoBuilding currentBuilding = player2.getCurrentBuilding();
                if (currentBuilding == null) {
                    IsoDirections playerDir = IsoDirections.fromAngle(player2.getForwardDirection());
                    currentBuilding = this.cell.GetPeekedInBuilding(player2.getCurrentSquare(), playerDir);
                    if (currentBuilding != null) {
                        this.cell.playerWindowPeekingRoomId[i] = this.cell.playerPeekedRoomId;
                    }
                }
            }
        }

        if (lastPlayerWindowPeekingRoomId != this.cell.playerWindowPeekingRoomId[playerIndex]) {
            IsoPlayer.players[playerIndex].dirtyRecalcGridStack = true;
        }

        if (isoGameCharacter != null
            && isoGameCharacter.getCurrentSquare() != null
            && isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.hidewalls)) {
            this.cell.maxZ = playerZ + 1;
        }

        this.cell.rendering = true;

        try {
            int maxHeight = playerZ < 0 ? playerZ : IsoCell.getInstance().chunkMap[playerIndex].maxHeight;
            int min = this.cell.chunkMap[playerIndex].minHeight;
            min = Math.max(min, playerZ);
            this.RenderTiles(min, maxHeight);
        } catch (Exception var20) {
            this.cell.rendering = false;
            ExceptionLogger.logException(var20);
        }

        this.cell.rendering = false;
        if (IsoGridSquare.getRecalcLightTime() < 0.0F) {
            IsoGridSquare.setRecalcLightTime(60.0F);
        }

        if (IsoGridSquare.getLightcache() <= 0) {
            IsoGridSquare.setLightcache(90);
        }

        try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("renderLast")) {
            for (int n = 0; n < this.cell.getObjectList().size(); n++) {
                IsoMovingObject obj = this.cell.getObjectList().get(n);
                obj.renderlast();
            }

            for (int ix = 0; ix < this.cell.getStaticUpdaterObjectList().size(); ix++) {
                IsoObject obj = this.cell.getStaticUpdaterObjectList().get(ix);
                obj.renderlast();
            }
        }

        IsoTree.checkChopTreeIndicators(playerIndex);
        IsoTree.renderChopTreeIndicators();
        this.cell.lastMinX = this.cell.minX;
        this.cell.lastMinY = this.cell.minY;
        this.cell.DoBuilding(playerIndex, true);
    }

    public void RenderTiles(int MinHeight, int MaxHeight) {
        this.cell.minHeight = MinHeight;

        try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.isoCellRenderTiles.profile()) {
            this.renderTilesInternal(MaxHeight);
        }
    }

    private void renderTilesInternal(int maxHeight) {
        FBORenderChunkManager.instance.recycle();
        if (DebugOptions.instance.terrain.renderTiles.enable.getValue()) {
            if (IsoCell.floorRenderShader == null) {
                RenderThread.invokeOnRenderContext(this.cell::initTileShaders);
            }

            FBORenderLevels.clearCachedSquares = true;
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
            player.dirtyRecalcGridStackTime = player.dirtyRecalcGridStackTime - GameTime.getInstance().getMultiplier() / 4.0F;
            IsoCell.PerPlayerRender perPlayerRender = this.cell.getPerPlayerRenderAt(playerIndex);
            perPlayerRender.setSize(this.cell.maxX - this.cell.minX + 1, this.cell.maxY - this.cell.minY + 1);
            this.currentTimeMillis = System.currentTimeMillis();
            if (this.cell.minX != perPlayerRender.minX
                || this.cell.minY != perPlayerRender.minY
                || this.cell.maxX != perPlayerRender.maxX
                || this.cell.maxY != perPlayerRender.maxY) {
                perPlayerRender.minX = this.cell.minX;
                perPlayerRender.minY = this.cell.minY;
                perPlayerRender.maxX = this.cell.maxX;
                perPlayerRender.maxY = this.cell.maxY;
            }

            int currentZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);

            try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("updateWeatherMask")) {
                this.updateWeatherMask(playerIndex, currentZ);
            }

            boolean bForceCutawayUpdate = false;
            if (perPlayerData1.lastZ != currentZ) {
                if (currentZ < 0 != perPlayerData1.lastZ < 0) {
                    player.dirtyRecalcGridStack = true;
                    this.invalidateAll(playerIndex);
                } else if (player.getBuilding() != null) {
                    player.getBuilding().getDef().invalidateOverlappedChunkLevelsAbove(playerIndex, PZMath.min(currentZ, perPlayerData1.lastZ), 2048L);
                } else if (player.isClimbing()) {
                    bForceCutawayUpdate = true;
                }

                perPlayerData1.lastZ = currentZ;
                this.checkSeenRooms(player, currentZ);
            }

            int puddlesValue1 = (int)Math.ceil(IsoPuddles.getInstance().getPuddlesSizeFinalValue() * 500.0F);
            int wetGround1 = (int)Math.ceil(IsoPuddles.getInstance().getWetGroundFinalValue() * 500.0F);
            if (PerformanceSettings.puddlesQuality == 2
                && (this.puddlesValue != puddlesValue1 || this.wetGroundValue != wetGround1)
                && this.puddlesRedrawTimeMs + 1000L < this.currentTimeMillis) {
                this.puddlesValue = puddlesValue1;
                this.wetGroundValue = wetGround1;
                this.puddlesRedrawTimeMs = this.currentTimeMillis;
                this.invalidateAll(playerIndex);
            }

            if (SandboxOptions.instance.enableSnowOnGround.getValue() && this.snowFracTarget != this.cell.getSnowTarget()) {
                this.snowFracTarget = this.cell.getSnowTarget();
                this.invalidateAll(playerIndex);
            }

            CompletableFuture<Boolean> checkFuture = null;
            if (DebugOptions.instance.threadGridStacks.getValue()) {
                checkFuture = CompletableFuture.supplyAsync(() -> this.recalculateGridStacks(player, playerIndex), PZForkJoinPool.commonPool());
            }

            try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("runChecks")) {
                bForceCutawayUpdate |= this.runChecks(playerIndex);
            }

            try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.profile()) {
                if (checkFuture != null) {
                    bForceCutawayUpdate |= checkFuture.join();
                } else {
                    bForceCutawayUpdate |= this.recalculateGridStacks(player, playerIndex);
                }
            }

            for (int z = 0; z < 8; z++) {
                bForceCutawayUpdate |= this.checkDebugKeys(playerIndex, z);
            }

            bForceCutawayUpdate |= this.checkDebugKeys(playerIndex, currentZ);
            if (bForceCutawayUpdate) {
                FBORenderCutaways.getInstance().squareChanged(null);
            }

            try (AbstractPerformanceProfileProbe ignoredx = cutaways.profile()) {
                bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkPlayerRoom(playerIndex);
                bForceCutawayUpdate |= this.cell.SetCutawayRoomsForPlayer();
                bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkExteriorWalls(perPlayerData1.onScreenChunks);
                bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkSlopedSurfaces(perPlayerData1.onScreenChunks);
                if (bForceCutawayUpdate) {
                    FBORenderCutaways.getInstance().squareChanged(null);
                }

                bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkOccludedRooms(playerIndex, perPlayerData1.onScreenChunks);
                this.prepareChunksForUpdating(playerIndex);
                if (bForceCutawayUpdate) {
                    FBORenderCutaways.getInstance().doCutawayVisitSquares(playerIndex, perPlayerData1.onScreenChunks);
                }
            }

            perPlayerData1.occlusionChanged = false;
            if (FBORenderOcclusion.getInstance().enabled && this.hasAnyDirtyChunkTextures(playerIndex)) {
                perPlayerData1.occlusionChanged = true;
                int size = (perPlayerData1.occludedGridX2 - perPlayerData1.occludedGridX1 + 1)
                    * (perPlayerData1.occludedGridY2 - perPlayerData1.occludedGridY1 + 1);
                if (perPlayerData1.occludedGrid == null || perPlayerData1.occludedGrid.length < size) {
                    perPlayerData1.occludedGrid = new int[size];
                }

                Arrays.fill(perPlayerData1.occludedGrid, -32);
                this.calculateOccludingSquares(playerIndex);
                FBORenderOcclusion.getInstance().occludedGrid = perPlayerData1.occludedGrid;
                FBORenderOcclusion.getInstance().occludedGridX1 = perPlayerData1.occludedGridX1;
                FBORenderOcclusion.getInstance().occludedGridY1 = perPlayerData1.occludedGridY1;
                FBORenderOcclusion.getInstance().occludedGridX2 = perPlayerData1.occludedGridX2;
                FBORenderOcclusion.getInstance().occludedGridY2 = perPlayerData1.occludedGridY2;
            }

            try (AbstractPerformanceProfileProbe ignoredx = updateLighting.profile()) {
                this.updateChunkLighting(playerIndex);
            }

            this.checkBlackedOutBuildings(playerIndex);
            this.checkBlackedOutRooms(playerIndex);
            FBORenderLevels.clearCachedSquares = false;

            try (AbstractPerformanceProfileProbe ignoredx = IsoCell.s_performance.renderTiles.performRenderTiles.profile()) {
                this.performRenderTiles(perPlayerRender, playerIndex, this.currentTimeMillis);
            }

            FBORenderLevels.clearCachedSquares = true;
            this.cell.playerCutawaysDirty[playerIndex] = false;
            IsoCell.ShadowSquares.clear();
            IsoCell.MinusFloorCharacters.clear();
            IsoCell.ShadedFloor.clear();
            IsoCell.SolidFloor.clear();
            IsoCell.VegetationCorpses.clear();

            try (AbstractPerformanceProfileProbe ignoredx = IsoCell.s_performance.renderTiles.renderDebugPhysics.profile()) {
                this.cell.renderDebugPhysics(playerIndex);
            }

            try (AbstractPerformanceProfileProbe ignoredx = IsoCell.s_performance.renderTiles.renderDebugLighting.profile()) {
                this.cell.renderDebugLighting(perPlayerRender, maxHeight);
            }

            FMODAmbientWalls.getInstance().render();
        }
    }

    private boolean recalculateGridStacks(IsoPlayer player, int playerIndex) {
        boolean bForceCutawayUpdate = false;
        FBORenderCutaways.getInstance().CalculatePointsOfInterest();
        bForceCutawayUpdate |= FBORenderCutaways.getInstance().CalculateBuildingsToCollapse();
        bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkHiddenBuildingLevels();
        bForceCutawayUpdate |= player.dirtyRecalcGridStack;
        this.recalculateAnyGridStacks(playerIndex);
        return bForceCutawayUpdate;
    }

    private void updateWeatherMask(int playerIndex, int currentZ) {
        if (WeatherFxMask.checkVisibleSquares(playerIndex, currentZ)) {
            WeatherFxMask.forceMaskUpdate(playerIndex);
            WeatherFxMask.initMask();
        }
    }

    private boolean runChecks(int playerIndex) {
        GameProfiler profiler = GameProfiler.getInstance();
        this.checkWaterQualityOption(playerIndex);
        this.checkWindEffectsOption(playerIndex);

        boolean result;
        try (GameProfiler.ProfileArea ignored = profiler.profile("Newly")) {
            result = this.checkNewlyOnScreenChunks(playerIndex);
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("Obscuring")) {
            this.checkObjectsObscuringPlayer(playerIndex);
            this.checkFadingInObjectsObscuringPlayer(playerIndex);
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("Chunks")) {
            this.checkChunksWithTrees(playerIndex);
            this.checkSeamChunks(playerIndex);
        }

        this.checkMannequinRenderDirection(playerIndex);
        this.checkPuddlesQualityOption(playerIndex);
        return result;
    }

    private void invalidateAll(int playerIndex) {
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];

        for (int xx = 0; xx < IsoChunkMap.chunkGridWidth; xx++) {
            for (int yy = 0; yy < IsoChunkMap.chunkGridWidth; yy++) {
                IsoChunk c = chunkMap.getChunk(xx, yy);
                if (c != null && !c.lightingNeverDone[playerIndex]) {
                    FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                    renderLevels.invalidateAll(2048L);
                }
            }
        }
    }

    private void checkObjectsObscuringPlayer(int playerIndex) {
        this.calculatePlayerRenderBounds(playerIndex);
        this.calculateObjectsObscuringPlayer(playerIndex, this.tempLocations);
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        if (this.tempLocations.equals(perPlayerData1.squaresObscuringPlayer)) {
            this.locationPool.releaseAll(this.tempLocations);
            this.tempLocations.clear();
        } else {
            IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);

            for (int i = 0; i < perPlayerData1.squaresObscuringPlayer.size(); i++) {
                IsoGameCharacter.Location location = perPlayerData1.squaresObscuringPlayer.get(i);
                if (!this.listContainsLocation(this.tempLocations, location)) {
                    IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
                    if (square != null) {
                        square.invalidateRenderChunkLevel(8192L);
                        this.invalidateChunkLevelForRenderSquare(square);
                    }
                }
            }

            this.locationPool.releaseAll(perPlayerData1.squaresObscuringPlayer);
            perPlayerData1.squaresObscuringPlayer.clear();
            PZArrayUtil.addAll(perPlayerData1.squaresObscuringPlayer, this.tempLocations);

            for (int ix = 0; ix < perPlayerData1.squaresObscuringPlayer.size(); ix++) {
                IsoGameCharacter.Location location = perPlayerData1.squaresObscuringPlayer.get(ix);
                IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
                if (square != null) {
                    square.invalidateRenderChunkLevel(8192L);
                    this.invalidateChunkLevelForRenderSquare(square);
                }
            }

            this.tempLocations.clear();
        }
    }

    private void calculatePlayerRenderBounds(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        float playerX = IsoCamera.frameState.camCharacterX;
        float playerY = IsoCamera.frameState.camCharacterY;
        float playerZ = IsoCamera.frameState.camCharacterZ;
        perPlayerData1.playerBoundsX = IsoUtils.XToScreen(playerX, playerY, playerZ, 0);
        perPlayerData1.playerBoundsY = IsoUtils.YToScreen(playerX, playerY, playerZ, 0);
        perPlayerData1.playerBoundsX = perPlayerData1.playerBoundsX - 32 * Core.tileScale;
        perPlayerData1.playerBoundsY = perPlayerData1.playerBoundsY - 112 * Core.tileScale;
        perPlayerData1.playerBoundsW = 64 * Core.tileScale;
        perPlayerData1.playerBoundsH = 128 * Core.tileScale;
    }

    private boolean isPotentiallyObscuringObject(IsoObject object) {
        if (object == null) {
            return false;
        } else {
            IsoSprite sprite = object.getSprite();
            if (sprite == null) {
                return false;
            } else {
                IsoGameCharacter chr = IsoCamera.frameState.camCharacter;
                if (chr != null && chr.isSittingOnFurniture() && chr.isSitOnFurnitureObject(object)) {
                    return false;
                } else if (chr != null && chr.isOnBed() && object == chr.getBed()) {
                    return false;
                } else if (sprite.getProperties().has(IsoFlagType.water)) {
                    return false;
                } else if (!sprite.getProperties().has(IsoFlagType.attachedSurface)
                    || !object.square.has(IsoFlagType.solid) && !object.square.has(IsoFlagType.solidtrans)) {
                    if (sprite.getProperties().has(IsoFlagType.attachedE)
                        || sprite.getProperties().has(IsoFlagType.attachedS)
                        || sprite.getProperties().has(IsoFlagType.attachedCeiling)) {
                        return true;
                    } else if (object.isStairsNorth()) {
                        return IsoCamera.frameState.camCharacterSquare != null && IsoCamera.frameState.camCharacterSquare.HasStairs()
                            ? false
                            : object.getX() > PZMath.fastfloor(IsoCamera.frameState.camCharacterX);
                    } else if (!object.isStairsWest()) {
                        return sprite.solid || sprite.solidTrans;
                    } else {
                        return IsoCamera.frameState.camCharacterSquare != null && IsoCamera.frameState.camCharacterSquare.HasStairs()
                            ? false
                            : object.getY() > PZMath.fastfloor(IsoCamera.frameState.camCharacterY);
                    }
                } else {
                    return true;
                }
            }
        }
    }

    private void calculateObjectsObscuringPlayer(int playerIndex, ArrayList<IsoGameCharacter.Location> locations) {
        this.locationPool.releaseAll(locations);
        locations.clear();
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null && player.getCurrentSquare() != null) {
            IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
            int sqx = player.getCurrentSquare().getX();
            int sqy = player.getCurrentSquare().getY();
            int sqz = player.getCurrentSquare().getZ();
            int sqLeftX = sqx - 1;
            int sqLeftY = sqy;
            int sqRightX = sqx;
            int sqRightY = sqy - 1;
            this.testSquareObscuringPlayer(playerIndex, sqx, sqy, sqz, locations);

            for (int i = 1; i <= 3; i++) {
                this.testSquareObscuringPlayer(playerIndex, sqx + i, sqy + i, sqz, locations);
                this.testSquareObscuringPlayer(playerIndex, sqLeftX + i, sqLeftY + i, sqz, locations);
                this.testSquareObscuringPlayer(playerIndex, sqRightX + i, sqRightY + i, sqz, locations);
                this.testSquareObscuringPlayer(playerIndex, sqx - 1 + i, sqy + 1 + i, sqz, locations);
                this.testSquareObscuringPlayer(playerIndex, sqx + 1 + i, sqy - 1 + i, sqz, locations);
            }

            for (int i = 0; i < locations.size(); i++) {
                IsoGameCharacter.Location location = locations.get(i);
                IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
                if (square != null) {
                    for (int j = 0; j < square.getObjects().size(); j++) {
                        IsoObject object = square.getObjects().get(j);
                        if (this.isPotentiallyObscuringObject(object)) {
                            this.addObscuringStairObjects(locations, square, object);
                            IsoSprite sprite = object.getSprite();
                            if (sprite.getSpriteGrid() != null) {
                                IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
                                int spriteGridPosX = spriteGrid.getSpriteGridPosX(sprite);
                                int spriteGridPosY = spriteGrid.getSpriteGridPosY(sprite);
                                int spriteGridPosZ = spriteGrid.getSpriteGridPosZ(sprite);

                                for (int spriteGridZ = 0; spriteGridZ < spriteGrid.getLevels(); spriteGridZ++) {
                                    for (int spriteGridY = 0; spriteGridY < spriteGrid.getHeight(); spriteGridY++) {
                                        for (int spriteGridX = 0; spriteGridX < spriteGrid.getWidth(); spriteGridX++) {
                                            if (spriteGrid.getSprite(spriteGridX, spriteGridY) != null) {
                                                int squareX = square.x - spriteGridPosX + spriteGridX;
                                                int squareY = square.y - spriteGridPosY + spriteGridY;
                                                int squareZ = square.z - spriteGridPosZ + spriteGridZ;
                                                if (chunkMap.getGridSquare(squareX, squareY, squareZ) != null
                                                    && !this.listContainsLocation(locations, squareX, squareY, squareZ)) {
                                                    IsoGameCharacter.Location location1 = this.locationPool.alloc();
                                                    location1.set(squareX, squareY, squareZ);
                                                    locations.add(location1);
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

            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

            for (int ix = 0; ix < perPlayerData1.squaresObscuringPlayer.size(); ix++) {
                IsoGameCharacter.Location location = perPlayerData1.squaresObscuringPlayer.get(ix);
                if (!this.listContainsLocation(locations, location) && !this.listContainsLocation(perPlayerData1.fadingInSquares, location)) {
                    IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
                    if (this.squareHasFadingInObjects(playerIndex, square)) {
                        IsoGameCharacter.Location location1 = this.locationPool.alloc();
                        location1.set(square.x, square.y, square.z);
                        perPlayerData1.fadingInSquares.add(location1);
                    }
                }
            }
        }
    }

    private void addObscuringStairObjects(ArrayList<IsoGameCharacter.Location> locations, IsoGridSquare square, IsoObject object) {
        if (object.isStairsNorth()) {
            int dy1 = 0;
            int dy2 = 0;
            if (object.getType() == IsoObjectType.stairsTN) {
                dy2 = 2;
            }

            if (object.getType() == IsoObjectType.stairsMN) {
                dy1 = -1;
                dy2 = 1;
            }

            if (object.getType() == IsoObjectType.stairsBN) {
                dy1 = -2;
                dy2 = 0;
            }

            if (dy1 < dy2) {
                for (int dy = dy1; dy <= dy2; dy++) {
                    IsoGridSquare square1 = IsoWorld.instance.currentCell.getGridSquare(square.x, square.y + dy, square.z);
                    if (square1 != null && !this.listContainsLocation(locations, square.x, square.y + dy, square.z)) {
                        IsoGameCharacter.Location location1 = this.locationPool.alloc();
                        location1.set(square.x, square.y + dy, square.z);
                        locations.add(location1);
                    }
                }
            }
        }

        if (object.isStairsWest()) {
            int dx1 = 0;
            int dx2 = 0;
            if (object.getType() == IsoObjectType.stairsTW) {
                dx2 = 2;
            }

            if (object.getType() == IsoObjectType.stairsMW) {
                dx1 = -1;
                dx2 = 1;
            }

            if (object.getType() == IsoObjectType.stairsBW) {
                dx1 = -2;
                dx2 = 0;
            }

            if (dx1 < dx2) {
                for (int dx = dx1; dx <= dx2; dx++) {
                    IsoGridSquare square1 = IsoWorld.instance.currentCell.getGridSquare(square.x + dx, square.y, square.z);
                    if (square1 != null && !this.listContainsLocation(locations, square.x + dx, square.y, square.z)) {
                        IsoGameCharacter.Location location1 = this.locationPool.alloc();
                        location1.set(square.x + dx, square.y, square.z);
                        locations.add(location1);
                    }
                }
            }
        }
    }

    private void checkFadingInObjectsObscuringPlayer(int playerIndex) {
        IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.fadingInSquares.size(); i++) {
            IsoGameCharacter.Location location = perPlayerData1.fadingInSquares.get(i);
            IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
            if (square != null && !this.squareHasFadingInObjects(playerIndex, square)) {
                square.invalidateRenderChunkLevel(8192L);
                this.invalidateChunkLevelForRenderSquare(square);
                perPlayerData1.fadingInSquares.remove(i--);
                this.locationPool.release(location);
            }
        }
    }

    private boolean squareHasFadingInObjects(int playerIndex, IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            for (int i = 0; i < square.getObjects().size(); i++) {
                IsoObject object = square.getObjects().get(i);
                if (this.isPotentiallyObscuringObject(object) && object.getAlpha(playerIndex) < 1.0F) {
                    return true;
                }
            }

            return false;
        }
    }

    private void invalidateChunkLevelForRenderSquare(IsoGridSquare square) {
        if (!square.getWorldObjects().isEmpty()) {
            int CPW = 8;
            if (PZMath.coordmodulo(square.x, 8) == 0 && PZMath.coordmodulo(square.y, 8) == 7) {
                IsoGridSquare renderSquare = square.getAdjacentSquare(IsoDirections.S);
                if (renderSquare != null) {
                    renderSquare.invalidateRenderChunkLevel(8192L);
                }
            }

            if (PZMath.coordmodulo(square.x, 8) == 7 && PZMath.coordmodulo(square.y, 8) == 0) {
                IsoGridSquare renderSquare = square.getAdjacentSquare(IsoDirections.E);
                if (renderSquare != null) {
                    renderSquare.invalidateRenderChunkLevel(8192L);
                }
            }
        }
    }

    private boolean listContainsLocation(ArrayList<IsoGameCharacter.Location> locations, IsoGameCharacter.Location location) {
        return this.listContainsLocation(locations, location.x, location.y, location.z);
    }

    private boolean listContainsLocation(ArrayList<IsoGameCharacter.Location> locations, int x, int y, int z) {
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).equals(x, y, z)) {
                return true;
            }
        }

        return false;
    }

    private void testSquareObscuringPlayer(int playerIndex, int x, int y, int z, ArrayList<IsoGameCharacter.Location> locations) {
        IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
        IsoGridSquare square = chunkMap.getGridSquare(x, y, z);
        if (this.isSquareObscuringPlayer(playerIndex, square)) {
            if (this.listContainsLocation(locations, square.x, square.y, square.z)) {
                return;
            }

            IsoGameCharacter.Location location = this.locationPool.alloc();
            location.set(square.x, square.y, square.z);
            locations.add(location);
        }
    }

    private boolean isSquareObscuringPlayer(int playerIndex, IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
            if (!square.has(IsoFlagType.attachedE)
                && !square.has(IsoFlagType.attachedS)
                && !square.has(IsoFlagType.attachedCeiling)
                && !square.HasStairs()
                && !square.has(IsoFlagType.solid)
                && !square.has(IsoFlagType.solidtrans)) {
                return false;
            } else {
                for (int i = 0; i < square.getObjects().size(); i++) {
                    IsoObject object = square.getObjects().get(i);
                    if (this.isPotentiallyObscuringObject(object)) {
                        Texture texture = object.sprite.getTextureForCurrentFrame(object.dir);
                        if (texture != null
                            && perPlayerData1.isObjectObscuringPlayer(
                                square, texture, object.offsetX, object.offsetY + object.getRenderYOffset() * Core.tileScale
                            )) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    private void checkChunksWithTrees(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            if (0 >= c.minLevel && 0 <= c.maxLevel) {
                FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                if (renderLevels.isOnScreen(0)) {
                    boolean bInStencilRect = renderLevels.calculateInStencilRect(0);
                    if (!bInStencilRect && renderLevels.inStencilRect) {
                        renderLevels.inStencilRect = false;
                        renderLevels.invalidateLevel(0, 4096L);
                    } else {
                        renderLevels.inStencilRect = bInStencilRect;
                        if (this.checkTreeTranslucency(playerIndex, renderLevels)) {
                            renderLevels.invalidateLevel(0, 4096L);
                        }
                    }
                }
            }
        }
    }

    private void checkSeamChunks(int playerIndex) {
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
            if (renderLevels.adjacentChunkLoadedCounter != c.adjacentChunkLoadedCounter) {
                renderLevels.adjacentChunkLoadedCounter = c.adjacentChunkLoadedCounter;
                renderLevels.invalidateAll(1024L);
            }
        }
    }

    private boolean checkTreeTranslucency(int playerIndex, FBORenderLevels renderLevels) {
        if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            return false;
        } else {
            float zoom = Core.getInstance().getZoom(playerIndex);
            if (renderLevels.isDirty(0, zoom)) {
                return false;
            } else {
                ArrayList<IsoGridSquare> squares = renderLevels.treeSquares;
                boolean bChanged = false;

                for (int i = 0; i < squares.size(); i++) {
                    IsoGridSquare square = squares.get(i);
                    if (square.chunk != null) {
                        IsoTree tree = square.getTree();
                        if (tree != null) {
                            boolean bChanged2 = false;
                            if (tree.fadeAlpha < 1.0F != tree.wasFaded) {
                                tree.wasFaded = tree.fadeAlpha < 1.0F;
                                bChanged = true;
                                bChanged2 = true;
                            }

                            if (this.isTranslucentTree(tree) != tree.renderFlag) {
                                tree.renderFlag = !tree.renderFlag;
                                bChanged = true;
                                bChanged2 = true;
                            }

                            if (bChanged2) {
                                IsoGridSquare renderSquare = tree.getRenderSquare();
                                if (renderSquare != null && tree.getSquare() != renderSquare) {
                                    renderSquare.invalidateRenderChunkLevel(4096L);
                                }
                            }
                        }
                    }
                }

                return bChanged;
            }
        }
    }

    private void checkWaterQualityOption(int playerIndex) {
        if (this.waterShader != IsoWater.getInstance().getShaderEnable()) {
            this.waterShader = IsoWater.getInstance().getShaderEnable();
            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

            for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                IsoChunk c = perPlayerData1.onScreenChunks.get(i);
                if (0 >= c.minLevel && 0 <= c.maxLevel) {
                    FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                    if (renderLevels.calculateOnScreen(0)) {
                        renderLevels.invalidateLevel(0, 1024L);
                    }
                }
            }
        }
    }

    private void checkWindEffectsOption(int playerIndex) {
        if (this.windEffects != Core.getInstance().getOptionDoWindSpriteEffects()) {
            this.windEffects = Core.getInstance().getOptionDoWindSpriteEffects();
            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

            for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                IsoChunk c = perPlayerData1.onScreenChunks.get(i);
                if (0 >= c.minLevel && 0 <= c.maxLevel) {
                    FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                    if (renderLevels.calculateOnScreen(0)) {
                        renderLevels.invalidateLevel(0, 4096L);
                    }
                }
            }
        }
    }

    private void checkPuddlesQualityOption(int playerIndex) {
        if (this.puddlesQuality == 2 != (PerformanceSettings.puddlesQuality == 2)) {
            this.puddlesQuality = PerformanceSettings.puddlesQuality;
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];

            for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; cy++) {
                for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; cx++) {
                    IsoChunk chunk = chunkMap.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                            IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(z)];

                            for (int i = 0; i < squares.length; i++) {
                                IsoGridSquare square = squares[i];
                                if (square != null) {
                                    IsoPuddlesGeometry pg = square.getPuddles();
                                    if (pg != null) {
                                        pg.init(square);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.invalidateAll(playerIndex);
        }
    }

    private boolean checkNewlyOnScreenChunks(int playerIndex) {
        boolean bForceCutawaysUpdate = false;
        float cameraZoom = Core.getInstance().getZoom(playerIndex);
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.onScreenChunks.clear();
        perPlayerData1.chunksWithAnimatedAttachments.clear();
        perPlayerData1.chunksWithFlies.clear();
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];

        for (int xx = 0; xx < IsoChunkMap.chunkGridWidth; xx++) {
            for (int yy = 0; yy < IsoChunkMap.chunkGridWidth; yy++) {
                IsoChunk c = chunkMap.getChunk(xx, yy);
                if (c != null && !c.lightingNeverDone[playerIndex]) {
                    FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                    if (!c.IsOnScreen(true)) {
                        for (int z = c.minLevel; z <= c.maxLevel; z++) {
                            renderLevels.setOnScreen(z, false);
                            renderLevels.freeFBOsForLevel(z);
                        }
                    } else {
                        perPlayerData1.onScreenChunks.add(c);
                        if (renderLevels.prevMinZ != c.minLevel || renderLevels.prevMaxZ != c.maxLevel) {
                            for (int z = c.minLevel; z <= c.maxLevel; z++) {
                                renderLevels.invalidateLevel(z, 64L);
                            }
                        }

                        for (int z = c.minLevel; z <= c.maxLevel; z++) {
                            if (z == renderLevels.getMinLevel(z)) {
                                boolean bWasOnScreen = renderLevels.isOnScreen(z);
                                boolean bOnScreen = renderLevels.calculateOnScreen(z);
                                if (bOnScreen && !renderLevels.getCachedSquares_Flies(z).isEmpty()) {
                                    perPlayerData1.addChunkWith_Flies(c);
                                }

                                if (bWasOnScreen != bOnScreen) {
                                    if (bOnScreen) {
                                        renderLevels.setOnScreen(z, true);
                                        renderLevels.invalidateLevel(z, 1024L);
                                        if (renderLevels.isDirty(z, 16384L, cameraZoom)) {
                                            bForceCutawaysUpdate = true;
                                        }
                                    } else {
                                        renderLevels.setOnScreen(z, false);
                                        renderLevels.freeFBOsForLevel(z);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FBORenderChunkManager.instance.recycle();
        return bForceCutawaysUpdate;
    }

    private void performRenderTiles(IsoCell.PerPlayerRender perPlayerRender, int playerIndex, long currentTimeMillis) {
        Shader floorRenderShader = null;
        Shader wallRenderShader = null;
        this.renderAnimatedAttachments = false;
        this.renderTranslucentOnly = false;
        FBORenderChunkManager.instance.startFrame();
        IsoPuddles.getInstance().clearThreadData();
        this.invalidateDelayedLoadingLevels = false;
        if (this.delayedLoadingTimerMs != 0L && this.delayedLoadingTimerMs <= currentTimeMillis) {
            this.delayedLoadingTimerMs = 0L;
            this.invalidateDelayedLoadingLevels = true;
        }

        SpriteRenderer.instance.beginProfile(tilesProbe);
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.chunksWithTranslucentFloor.clear();
        perPlayerData1.chunksWithTranslucentNonFloor.clear();

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);

            try (AbstractPerformanceProfileProbe ignored = renderOneChunk.profile()) {
                this.renderOneChunk(c, perPlayerRender, playerIndex, currentTimeMillis, floorRenderShader, wallRenderShader);
            }
        }

        SpriteRenderer.instance.endProfile(tilesProbe);
        FBORenderCorpses.getInstance().update();
        FBORenderItems.getInstance().update();
        FBORenderChunkManager.instance.endFrame();
        FBORenderShadows.getInstance().clear();
        this.renderPlayers(playerIndex);
        this.renderCorpseShadows(playerIndex);
        this.renderMannequinShadows(playerIndex);
        if (!DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
            this.renderCorpsesInWorld(playerIndex);
        }

        if (!DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
            SpriteRenderer.instance.beginProfile(itemsProbe);
            this.renderItemsInWorld(playerIndex);
            SpriteRenderer.instance.endProfile(itemsProbe);
        }

        if (PerformanceSettings.puddlesQuality < 2) {
            try (AbstractPerformanceProfileProbe ignored = puddles.profile()) {
                this.renderPuddles(playerIndex);
            }
        }

        this.renderOpaqueObjectsEvent(playerIndex);
        SpriteRenderer.instance.beginProfile(movingObjectsProbe);
        this.renderMovingObjects();
        SpriteRenderer.instance.endProfile(movingObjectsProbe);

        try (AbstractPerformanceProfileProbe ignored = water.profile()) {
            this.renderWater(playerIndex);
        }

        this.renderAnimatedAttachments(playerIndex);
        this.renderFlies(playerIndex);
        FBORenderObjectHighlight.getInstance().render(playerIndex);
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);

        for (int z = chunkMap.minHeight; z <= chunkMap.maxHeight; z++) {
            SpriteRenderer.instance.beginProfile(translucentFloorObjectsProbe);

            try (AbstractPerformanceProfileProbe ignored = translucentFloor.profile()) {
                this.renderTranslucentFloorObjects(playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
            }

            SpriteRenderer.instance.endProfile(translucentFloorObjectsProbe);

            try (AbstractPerformanceProfileProbe ignored = puddles.profile()) {
                this.renderPuddlesTranslucentFloorsOnly(playerIndex, z);
            }

            if (z == 0) {
                this.renderWaterShore(playerIndex);
            }

            this.renderRainSplashes(playerIndex, z);
            SpriteRenderer.instance.beginProfile(shadowsProbe);
            FBORenderShadows.getInstance().renderMain(z);
            SpriteRenderer.instance.endProfile(shadowsProbe);
            if (z == PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                SpriteRenderer.instance.beginProfile(visibilityProbe);

                try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("Visibility")) {
                    VisibilityPolygon2.getInstance().renderMain(playerIndex);
                }

                SpriteRenderer.instance.endProfile(visibilityProbe);
            }

            WorldMarkers.instance.renderGridSquareMarkers(z);
            this.renderTranslucentOnly = true;
            IsoMarkers.instance.renderIsoMarkers(perPlayerRender, z, playerIndex);
            this.renderTranslucentOnly = false;
            SpriteRenderer.instance.beginProfile(translucentObjectsProbe);

            try (AbstractPerformanceProfileProbe ignored = translucentNonFloor.profile()) {
                this.renderTranslucentObjects(playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
            }

            SpriteRenderer.instance.endProfile(translucentObjectsProbe);
        }

        FBORenderShadows.getInstance().endRender();
        if (DebugOptions.instance.weather.showUsablePuddles.getValue()) {
            this.renderPuddleDebug(playerIndex);
        }

        try (AbstractPerformanceProfileProbe ignored = fog.profile()) {
            this.renderFog(playerIndex);
        }

        FBORenderObjectOutline.getInstance().render(playerIndex);
    }

    private void renderOneChunk(
        IsoChunk c, IsoCell.PerPlayerRender perPlayerRender, int playerIndex, long currentTimeMillis, Shader floorRenderShader, Shader wallRenderShader
    ) {
        if (c != null && c.IsOnScreen(true)) {
            if (!c.lightingNeverDone[playerIndex]) {
                FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                renderLevels.prevMinZ = c.minLevel;
                renderLevels.prevMaxZ = c.maxLevel;

                for (int zza = c.minLevel; zza <= c.maxLevel; zza++) {
                    try (AbstractPerformanceProfileProbe ignored = renderOneChunkLevel.profile()) {
                        this.renderOneLevel(c, zza, perPlayerRender, playerIndex, currentTimeMillis, floorRenderShader, wallRenderShader);
                    }

                    if (DebugOptions.instance.fboRenderChunk.renderWallLines.getValue() && zza == PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                        c.getCutawayData().debugRender(zza);
                    }
                }

                IndieGL.glDepthMask(false);
                IndieGL.glDepthFunc(519);
            }
        }
    }

    private void renderOneLevel(
        IsoChunk c,
        int level,
        IsoCell.PerPlayerRender perPlayerRender,
        int playerIndex,
        long currentTimeMillis,
        Shader floorRenderShader,
        Shader wallRenderShader
    ) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            renderLevels.freeFBOsForLevel(level);
        } else {
            float zoom = Core.getInstance().getZoom(playerIndex);
            if (this.invalidateDelayedLoadingLevels && renderLevels.isDelayedLoading(level) && level == renderLevels.getMinLevel(level)) {
                renderLevels.invalidateLevel(level, 1024L);
            }

            if (FBORenderOcclusion.getInstance().enabled) {
                if (level == renderLevels.getMinLevel(level) && perPlayerData1.occlusionChanged) {
                    renderLevels.setRenderedSquaresCount(level, this.calculateRenderedSquaresCount(playerIndex, c, level));
                }

                if (renderLevels.getRenderedSquaresCount(level) == 0) {
                    if (level == renderLevels.getMaxLevel(level)) {
                        renderLevels.clearDirty(level, zoom);
                        if (renderLevels.getFBOForLevel(level, zoom) != null) {
                            renderLevels.freeFBOsForLevel(level);
                            FBORenderChunkManager.instance.recycle();
                        }
                    }

                    return;
                }
            }

            int frameNo = IsoWorld.instance.getFrameNo();
            boolean canRender = true;
            boolean canClear = true;
            boolean isDirty = FBORenderChunkManager.instance.beginRenderChunkLevel(c, level, zoom, canRender, canClear);
            if (DebugOptions.instance.delayObjectRender.getValue()) {
                canRender = frameNo == c.loadedFrame || frameNo >= c.renderFrame;
                if (frameNo != c.loadedFrame && frameNo <= c.renderFrame) {
                    boolean var90 = false;
                } else {
                    boolean var10000 = true;
                }
            }

            if (isDirty && canRender) {
                boolean[][] flattenGrassEtc = perPlayerRender.flattenGrassEtc;
                IsoCell.ShadowSquares.clear();
                IsoCell.SolidFloor.clear();
                IsoCell.ShadedFloor.clear();
                IsoCell.VegetationCorpses.clear();
                IsoCell.MinusFloorCharacters.clear();
                GameProfiler profiler = GameProfiler.getInstance();

                try (AbstractPerformanceProfileProbe ignored = calculateRenderInfo.profile()) {
                    if (level == renderLevels.getMinLevel(level)) {
                        renderLevels.clearCachedSquares(level);
                    }

                    if (level == 0) {
                        renderLevels.treeSquares.clear();
                        renderLevels.waterSquares.clear();
                        renderLevels.waterShoreSquares.clear();
                        renderLevels.waterAttachSquares.clear();
                    }

                    FBORenderCutaways.ChunkLevelData levelData = c.getCutawayDataForLevel(level);
                    IsoGridSquare[] squares = c.squares[c.squaresIndexOfLevel(level)];

                    for (int i = 0; i < squares.length; i++) {
                        IsoGridSquare square = squares[i];
                        if (levelData.shouldRenderSquare(playerIndex, square) && !FBORenderOcclusion.getInstance().isOccluded(square.x, square.y, square.z)) {
                            if (!square.getObjects().isEmpty()) {
                                square.flattenGrassEtc = false;

                                try (GameProfiler.ProfileArea ignored1 = profiler.profile("Calculate")) {
                                    this.calculateObjectRenderInfo(square);
                                }

                                int flags = 0;
                                boolean bHasTranslucentFloor = false;
                                boolean bHasTranslucentNonFloor = false;
                                boolean bHasAttachedSpritesOnWater = false;
                                boolean bHasAnimatedAttachments = false;
                                boolean bHasItems = false;
                                IsoObject[] objects = square.getObjects().getElements();
                                int numObjects = square.getObjects().size();

                                for (int j = 0; j < numObjects; j++) {
                                    IsoObject object = objects[j];
                                    ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                                    switch (renderInfo.layer) {
                                        case Floor:
                                            flags |= 1;
                                            break;
                                        case Vegetation:
                                            flags |= 2;
                                            break;
                                        case MinusFloor:
                                            flags |= 4;
                                            break;
                                        case MinusFloorSE:
                                            flags |= 4;
                                            break;
                                        case WorldInventoryObject:
                                            flags |= 4;
                                            break;
                                        case Translucent:
                                            bHasTranslucentNonFloor = true;
                                            break;
                                        case TranslucentFloor:
                                            bHasTranslucentFloor = true;
                                    }

                                    if (renderInfo.layer == ObjectRenderLayer.None
                                        && object.sprite != null
                                        && object.sprite.getProperties().has(IsoFlagType.water)
                                        && object.getAttachedAnimSprite() != null
                                        && !object.getAttachedAnimSprite().isEmpty()) {
                                        bHasAttachedSpritesOnWater = true;
                                    }

                                    bHasAnimatedAttachments |= object.hasAnimatedAttachments();
                                    if (!DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                                        bHasItems |= object instanceof IsoWorldInventoryObject;
                                    }
                                }

                                if (bHasAnimatedAttachments) {
                                    renderLevels.getCachedSquares_AnimatedAttachments(level).add(square);
                                }

                                if (!square.getStaticMovingObjects().isEmpty()) {
                                    renderLevels.getCachedSquares_Corpses(level).add(square);
                                }

                                if (square.hasFlies()) {
                                    renderLevels.getCachedSquares_Flies(level).add(square);
                                }

                                if (bHasItems) {
                                    renderLevels.getCachedSquares_Items(level).add(square);
                                }

                                try (GameProfiler.ProfileArea ignored1 = profiler.profile("Puddles")) {
                                    if (square.getPuddles() != null && square.getPuddles().shouldRender()) {
                                        renderLevels.getCachedSquares_Puddles(level).add(square);
                                    }
                                }

                                if (bHasTranslucentFloor) {
                                    renderLevels.getCachedSquares_TranslucentFloor(level).add(square);
                                }

                                if (bHasTranslucentNonFloor) {
                                    renderLevels.getCachedSquares_TranslucentNonFloor(level).add(square);
                                }

                                if (!square.getStaticMovingObjects().isEmpty()) {
                                    flags |= 2;
                                    flags |= 16;
                                    if (square.HasStairs()) {
                                        flags |= 4;
                                    }
                                }

                                if (!square.getWorldObjects().isEmpty()) {
                                    flags |= 2;
                                }

                                for (int m = 0; m < square.getMovingObjects().size(); m++) {
                                    IsoMovingObject mov = square.getMovingObjects().get(m);
                                    boolean bOnFloor = mov.isOnFloor();
                                    if (bOnFloor && mov instanceof IsoZombie zombie) {
                                        bOnFloor = zombie.isProne();
                                        if (!BaseVehicle.renderToTexture) {
                                            bOnFloor = false;
                                        }
                                    }

                                    if (bOnFloor) {
                                        flags |= 2;
                                    } else {
                                        flags |= 4;
                                    }

                                    flags |= 16;
                                }

                                if (square.hasFlies()) {
                                    flags |= 4;
                                }

                                if ((flags & 1) != 0) {
                                    IsoCell.SolidFloor.add(square);
                                }

                                if ((flags & 8) != 0) {
                                    IsoCell.ShadedFloor.add(square);
                                }

                                if ((flags & 2) != 0) {
                                    IsoCell.VegetationCorpses.add(square);
                                }

                                if ((flags & 4) != 0) {
                                    IsoCell.MinusFloorCharacters.add(square);
                                }

                                if ((flags & 16) != 0) {
                                    IsoCell.ShadowSquares.add(square);
                                }

                                if (level == 0 && square.has(IsoObjectType.tree)) {
                                    renderLevels.treeSquares.add(square);
                                }

                                if (level == 0 && square.getWater() != null && square.getWater().hasWater()) {
                                    renderLevels.waterSquares.add(square);
                                }

                                if (level == 0 && square.getWater() != null && square.getWater().isbShore() && IsoWater.getInstance().getShaderEnable()) {
                                    renderLevels.waterShoreSquares.add(square);
                                }

                                if (bHasAttachedSpritesOnWater) {
                                    renderLevels.waterAttachSquares.add(square);
                                }
                            }
                        } else {
                            this.setNotRendered(square);
                        }
                    }
                }

                try (AbstractPerformanceProfileProbe ignored = renderOneChunkLevel2.profile()) {
                    if (level == renderLevels.getMinLevel(level)) {
                        renderLevels.clearDelayedLoading(level);
                    }

                    boolean renderFloor = true;
                    boolean renderObjects = true;
                    if (DebugOptions.instance.delayObjectRender.getValue()) {
                        renderFloor = frameNo == c.loadedFrame || frameNo > c.renderFrame;
                        renderObjects = frameNo >= c.renderFrame;
                    }

                    if (renderFloor) {
                        try (GameProfiler.ProfileArea ignored1x = profiler.profile("Floor")) {
                            for (int ix = 0; ix < IsoCell.SolidFloor.size(); ix++) {
                                IsoGridSquare square = IsoCell.SolidFloor.get(ix);
                                this.renderFloor(square);
                            }
                        }

                        try (GameProfiler.ProfileArea ignored1x = profiler.profile("Snow")) {
                            IndieGL.disableDepthTest();
                            FBORenderSnow.getInstance().RenderSnow(c, level);
                            IndieGL.enableDepthTest();
                        }

                        try (GameProfiler.ProfileArea ignored1x = profiler.profile("Blood")) {
                            if (IsoCamera.frameState.camCharacterZ >= 0.0F || level <= PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                                int CPW = 8;
                                this.renderOneLevel_Blood(c, level, c.wx * 8, c.wy * 8, (c.wx + 1) * 8, (c.wy + 1) * 8);
                                this.renderOneLevel_Blood(c, -1, -1, level);
                                this.renderOneLevel_Blood(c, 0, -1, level);
                                this.renderOneLevel_Blood(c, 1, -1, level);
                                this.renderOneLevel_Blood(c, -1, 0, level);
                                this.renderOneLevel_Blood(c, 1, 0, level);
                                this.renderOneLevel_Blood(c, -1, 1, level);
                                this.renderOneLevel_Blood(c, 0, 1, level);
                                this.renderOneLevel_Blood(c, 1, 1, level);
                            }
                        }
                    }

                    if (!renderObjects) {
                        FBORenderChunkManager.instance.endRenderChunkLevel(c, level, zoom, false);
                        return;
                    }

                    if (DebugOptions.instance.terrain.renderTiles.vegetationCorpses.getValue()) {
                        try (GameProfiler.ProfileArea ignored1xx = profiler.profile("Vegetation Corpses")) {
                            if (DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
                                IsoGridSquare squareNW = c.getGridSquare(0, 0, level);
                                IsoGridSquare squareN = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.N);
                                if (squareNW != null && squareN != null) {
                                    squareN.cacheLightInfo();
                                    this.renderCorpses(squareN, squareNW, true);
                                }

                                IsoGridSquare squareW = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.W);
                                if (squareNW != null && squareW != null) {
                                    squareW.cacheLightInfo();
                                    this.renderCorpses(squareW, squareNW, true);
                                }
                            }

                            for (int ix = 0; ix < IsoCell.VegetationCorpses.size(); ix++) {
                                IsoGridSquare square = IsoCell.VegetationCorpses.get(ix);
                                this.renderVegetation(square);
                                if (DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
                                    this.renderCorpses(square, square, true);
                                }
                            }
                        }
                    }

                    if (DebugOptions.instance.terrain.renderTiles.minusFloorCharacters.getValue()) {
                        try (GameProfiler.ProfileArea ignored1xx = profiler.profile("Minus Floor Chars")) {
                            if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                                IsoGridSquare squareNWx = c.getGridSquare(0, 0, level);
                                IsoGridSquare squareNx = squareNWx == null ? null : squareNWx.getAdjacentSquare(IsoDirections.N);
                                if (squareNWx != null && squareNx != null) {
                                    squareNx.cacheLightInfo();
                                    this.renderWorldInventoryObjects(squareNx, squareNWx, true);
                                }

                                IsoGridSquare squareW = squareNWx == null ? null : squareNWx.getAdjacentSquare(IsoDirections.W);
                                if (squareNWx != null && squareW != null) {
                                    squareW.cacheLightInfo();
                                    this.renderWorldInventoryObjects(squareW, squareNWx, true);
                                }
                            }

                            FBORenderTrees.current = FBORenderTrees.alloc();
                            FBORenderTrees.current.init();
                            IsoGridSquare squareNWxx = c.getGridSquare(0, 0, level);
                            IsoGridSquare squareNxx = squareNWxx == null ? null : squareNWxx.getAdjacentSquare(IsoDirections.N);
                            if (squareNWxx != null && squareNxx != null) {
                                squareNxx.cacheLightInfo();
                                this.renderMinusFloor(c, squareNxx);
                            }

                            IsoGridSquare squareW = squareNWxx == null ? null : squareNWxx.getAdjacentSquare(IsoDirections.W);
                            if (squareNWxx != null && squareW != null) {
                                squareW.cacheLightInfo();
                                this.renderMinusFloor(c, squareW);
                            }

                            for (int ixx = 0; ixx < IsoCell.MinusFloorCharacters.size(); ixx++) {
                                squareNxx = IsoCell.MinusFloorCharacters.get(ixx);
                                if (squareNxx.getLightInfo(playerIndex) != null) {
                                    this.renderMinusFloor(c, squareNxx);
                                    if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                                        this.renderWorldInventoryObjects(squareNxx, squareNxx, true);
                                    }

                                    this.renderMinusFloorSE(squareNxx);
                                }
                            }

                            if (FBORenderTrees.current.trees.isEmpty()) {
                                FBORenderTrees.s_pool.release(FBORenderTrees.current);
                            } else {
                                SpriteRenderer.instance.drawGeneric(FBORenderTrees.current);
                            }
                        }
                    }

                    if (PerformanceSettings.puddlesQuality == 2) {
                        try (GameProfiler.ProfileArea ignored1xx = profiler.profile("Low Puddles")) {
                            this.renderPuddlesToChunkTexture(playerIndex, level, c);
                        }
                    }

                    try (GameProfiler.ProfileArea ignored1xx = profiler.profile("Add Chunk")) {
                        if (!renderLevels.getCachedSquares_AnimatedAttachments(level).isEmpty()) {
                            perPlayerData1.addChunkWith_AnimatedAttachments(c);
                        }

                        if (!renderLevels.getCachedSquares_TranslucentFloor(level).isEmpty()) {
                            perPlayerData1.addChunkWith_TranslucentFloor(c);
                        }

                        if (renderLevels.getCachedSquares_Items(level).size() + renderLevels.getCachedSquares_TranslucentNonFloor(level).size() > 0) {
                            perPlayerData1.addChunkWith_TranslucentNonFloor(c);
                        }
                    }
                }

                FBORenderChunkManager.instance.endRenderChunkLevel(c, level, zoom, true);
            } else {
                FBORenderChunkManager.instance.endRenderChunkLevel(c, level, zoom, false);
                if (!renderLevels.getCachedSquares_AnimatedAttachments(level).isEmpty()) {
                    perPlayerData1.addChunkWith_AnimatedAttachments(c);
                }

                if (!renderLevels.getCachedSquares_TranslucentFloor(level).isEmpty()) {
                    perPlayerData1.addChunkWith_TranslucentFloor(c);
                }

                if (renderLevels.getCachedSquares_Items(level).size() + renderLevels.getCachedSquares_TranslucentNonFloor(level).size() > 0) {
                    perPlayerData1.addChunkWith_TranslucentNonFloor(c);
                }
            }
        }
    }

    private void calculateObjectRenderInfo(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        this.calculateObjectRenderInfo(playerIndex, square, square.getObjects());

        for (int i = 0; i < square.getStaticMovingObjects().size(); i++) {
            IsoMovingObject object = square.getStaticMovingObjects().get(i);
            if (object instanceof IsoDeadBody body) {
                ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                renderInfo.layer = ObjectRenderLayer.Corpse;
                renderInfo.renderAlpha = 1.0F;
                renderInfo.cutaway = false;
            }
        }

        for (int ix = 0; ix < square.getWorldObjects().size(); ix++) {
            IsoWorldInventoryObject object = square.getWorldObjects().get(ix);
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            renderInfo.layer = ObjectRenderLayer.WorldInventoryObject;
            renderInfo.renderAlpha = 1.0F;
            renderInfo.cutaway = false;
        }
    }

    private void calculateObjectRenderInfo(int playerIndex, IsoGridSquare square, PZArrayList<IsoObject> objectList) {
        IsoObject[] objects = objectList.getElements();
        int numObjects = objectList.size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[i];
            this.calculateObjectRenderInfo(playerIndex, square, object);
        }
    }

    private void calculateObjectRenderInfo(int playerIndex, IsoGridSquare square, IsoObject object) {
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        renderInfo.layer = this.calculateObjectRenderLayer(object);
        renderInfo.targetAlpha = this.calculateObjectTargetAlpha(object);
        renderInfo.renderAlpha = 0.0F;
        renderInfo.cutaway = false;
        if (renderInfo.targetAlpha < 1.0F) {
            if (object instanceof IsoMannequin) {
                boolean var5 = true;
            } else if (renderInfo.layer == ObjectRenderLayer.MinusFloor || renderInfo.layer == ObjectRenderLayer.MinusFloorSE) {
                renderInfo.layer = ObjectRenderLayer.Translucent;
            }
        }

        if (renderInfo.layer == ObjectRenderLayer.Translucent && square.getLightLevel(playerIndex) == 0.0F) {
            object.setAlpha(playerIndex, renderInfo.targetAlpha);
        }

        renderInfo.renderWidth = renderInfo.renderHeight = 0.0F;
    }

    private void setNotRendered(IsoGridSquare square) {
        if (square != null) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoObject[] objects = square.getObjects().getElements();
            int numObjects = square.getObjects().size();

            for (int i = 0; i < numObjects; i++) {
                IsoObject object = objects[i];
                ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                renderInfo.layer = ObjectRenderLayer.None;
                renderInfo.targetAlpha = 0.0F;
            }
        }
    }

    private ObjectRenderLayer calculateObjectRenderLayer(IsoObject object) {
        if (object instanceof IsoWorldInventoryObject) {
            return ObjectRenderLayer.WorldInventoryObject;
        } else if (this.isObjectRenderLayer_TranslucentFloor(object)) {
            return ObjectRenderLayer.TranslucentFloor;
        } else if (this.isObjectRenderLayer_Floor(object)) {
            return ObjectRenderLayer.Floor;
        } else if (this.isObjectRenderLayer_Vegetation(object)) {
            return ObjectRenderLayer.Vegetation;
        } else if (this.isObjectRenderLayer_MinusFloor(object)) {
            return ObjectRenderLayer.MinusFloor;
        } else if (this.isObjectRenderLayer_MinusFloorSE(object)) {
            return ObjectRenderLayer.MinusFloorSE;
        } else {
            return this.isObjectRenderLayer_Translucent(object) ? ObjectRenderLayer.Translucent : ObjectRenderLayer.None;
        }
    }

    private boolean isObjectRenderLayer_Floor(IsoObject object) {
        IsoGridSquare square = object.square;
        if (square == null) {
            return false;
        } else {
            boolean bDoIt = true;
            if (object.sprite != null && !object.sprite.solidfloor && object.sprite.renderLayer != 1) {
                bDoIt = false;
            }

            if (object instanceof IsoFire || object instanceof IsoCarBatteryCharger) {
                bDoIt = false;
            }

            IsoWaterGeometry water = square.z == 0 ? square.getWater() : null;
            if (IsoWater.getInstance().getShaderEnable()
                && water != null
                && water.isValid()
                && object.sprite != null
                && object.sprite.properties.has(IsoFlagType.water)) {
                bDoIt = water.isbShore();
            }

            if (bDoIt && IsoWater.getInstance().getShaderEnable() && water != null && water.isValid() && !water.isbShore()) {
                IsoObject waterObj = square.getWaterObject();
                bDoIt = waterObj != null && waterObj.getObjectIndex() < object.getObjectIndex();
            }

            if (bDoIt && object.sprite != null && object.sprite.getProperties().getSlopedSurfaceDirection() != null) {
                return false;
            } else {
                int playerIndex = IsoCamera.frameState.playerIndex;
                return FBORenderCutaways.getInstance().shouldHideElevatedFloor(playerIndex, object) ? false : bDoIt;
            }
        }
    }

    private boolean isObjectRenderLayer_Vegetation(IsoObject object) {
        IsoGridSquare square = object.square;
        boolean bGrassEtc = object.sprite != null && (object.sprite.isBush || object.sprite.canBeRemoved || object.sprite.attachedFloor);
        return bGrassEtc && square.flattenGrassEtc;
    }

    private boolean isObjectRenderLayer_MinusFloor(IsoObject object) {
        IsoSprite sprite = object.getSprite();
        if (sprite != null && (sprite.depthFlags & 2) != 0) {
            return false;
        } else if (object.isAnimating()) {
            return false;
        } else {
            if (Core.getInstance().getOptionDoWindSpriteEffects()) {
                if (object instanceof IsoTree) {
                    return false;
                }

                if (object.getWindRenderEffects() != null) {
                    return false;
                }
            } else {
                if (this.isTranslucentTree(object)) {
                    return false;
                }

                if (object instanceof IsoTree && object.getObjectRenderEffects() != null) {
                    return false;
                }
            }

            if (object.getObjectRenderEffectsToApply() != null) {
                return false;
            } else if (object instanceof IsoTree isoTree && isoTree.fadeAlpha < 1.0F) {
                return false;
            } else {
                IsoMannequin mannequin = Type.tryCastTo(object, IsoMannequin.class);
                if (mannequin != null && mannequin.shouldRenderEachFrame()) {
                    return false;
                } else {
                    IsoGridSquare square = object.square;
                    boolean bDoIt = true;
                    IsoObjectType t = IsoObjectType.MAX;
                    if (sprite != null) {
                        t = sprite.getType();
                    }

                    if (sprite != null && (sprite.solidfloor || sprite.renderLayer == 1) && sprite.getProperties().getSlopedSurfaceDirection() == null) {
                        bDoIt = false;
                    }

                    if (object instanceof IsoFire) {
                        bDoIt = false;
                    }

                    int maxZ = 1000;
                    if (square.z >= 1000 && (sprite == null || !sprite.alwaysDraw)) {
                        bDoIt = false;
                    }

                    boolean bGrassEtc = sprite != null && (sprite.isBush || sprite.canBeRemoved || sprite.attachedFloor);
                    if (bGrassEtc && square.flattenGrassEtc) {
                        return false;
                    } else {
                        if (sprite != null
                            && (t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT)
                            && square.z == 999
                            && square.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                            bDoIt = false;
                        }

                        if (sprite != null && !sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
                            bDoIt = true;
                        }

                        if (square.isSpriteOnSouthOrEastWall(object)) {
                            bDoIt = false;
                        }

                        boolean bTranslucent = object instanceof IsoWindow;
                        IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
                        bTranslucent |= door != null && door.getProperties() != null && door.getProperties().has("doorTrans");
                        int playerIndex = IsoCamera.frameState.playerIndex;
                        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
                        bTranslucent |= sprite != null
                            && (sprite.solid || sprite.solidTrans)
                            && (
                                object.getAlpha(playerIndex) < 1.0F && perPlayerData1.isFadingInSquare(square)
                                    || perPlayerData1.isSquareObscuringPlayer(square)
                            );
                        if (bTranslucent) {
                            bDoIt = false;
                        }

                        return bDoIt;
                    }
                }
            }
        }
    }

    private boolean isObjectRenderLayer_MinusFloorSE(IsoObject object) {
        IsoGridSquare square = object.square;
        boolean bDoIt = true;
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getType();
        }

        if (object.sprite != null && (object.sprite.solidfloor || object.sprite.renderLayer == 1)) {
            bDoIt = false;
        }

        if (object instanceof IsoFire) {
            bDoIt = false;
        }

        int maxZ = 1000;
        if (square.z >= 1000 && (object.sprite == null || !object.sprite.alwaysDraw)) {
            bDoIt = false;
        }

        boolean bGrassEtc = object.sprite != null && (object.sprite.isBush || object.sprite.canBeRemoved || object.sprite.attachedFloor);
        if (bGrassEtc) {
            return false;
        } else {
            if (object.sprite != null
                && (t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT)
                && square.z == 999
                && square.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                bDoIt = false;
            }

            if (object.sprite != null && !object.sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
                bDoIt = true;
            }

            if (!square.isSpriteOnSouthOrEastWall(object)) {
                bDoIt = false;
            }

            boolean bTranslucent = object instanceof IsoWindow;
            IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
            bTranslucent |= door != null && door.getProperties() != null && door.getProperties().has("doorTrans");
            if (bTranslucent) {
                bDoIt = false;
            }

            return bDoIt;
        }
    }

    private boolean isObjectRenderLayer_TranslucentFloor(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (FBORenderCutaways.getInstance().shouldHideElevatedFloor(playerIndex, object)) {
            return false;
        } else {
            boolean bTranslucent = object.getSprite() != null && object.getSprite().getProperties().has(IsoFlagType.transparentFloor);
            if (object.getSprite() != null
                && object.getSprite().solidfloor
                && IsoWater.getInstance().getShaderEnable()
                && DebugOptions.instance.terrain.renderTiles.isoGridSquare.shoreFade.getValue()) {
                IsoWaterGeometry water = object.square.z == 0 ? object.square.getWater() : null;
                boolean isShore = water != null && water.isbShore();
                if (isShore) {
                    return true;
                }
            }

            return bTranslucent;
        }
    }

    private boolean isObjectRenderLayer_Translucent(IsoObject object) {
        IsoSprite sprite = object.getSprite();
        if (sprite != null && (sprite.depthFlags & 2) != 0) {
            return true;
        } else if (object instanceof IsoFire) {
            return true;
        } else if (object.isAnimating()) {
            return true;
        } else {
            if (Core.getInstance().getOptionDoWindSpriteEffects()) {
                if (object instanceof IsoTree) {
                    return true;
                }

                if (object.getWindRenderEffects() != null) {
                    return true;
                }
            } else {
                if (this.isTranslucentTree(object)) {
                    return true;
                }

                if (object instanceof IsoTree && object.getObjectRenderEffects() != null) {
                    return true;
                }
            }

            if (object.getObjectRenderEffectsToApply() != null) {
                return true;
            } else if (object instanceof IsoTree isoTree && isoTree.fadeAlpha < 1.0F) {
                return true;
            } else {
                boolean bTranslucent = object instanceof IsoWindow;
                IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
                bTranslucent |= door != null && door.getProperties() != null && door.getProperties().has("doorTrans");
                int playerIndex = IsoCamera.frameState.playerIndex;
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
                return bTranslucent
                    | (
                        sprite != null
                            && (sprite.solid || sprite.solidTrans)
                            && (
                                object.getAlpha(playerIndex) < 1.0F && perPlayerData1.isFadingInSquare(object.square)
                                    || perPlayerData1.isSquareObscuringPlayer(object.square)
                            )
                    );
            }
        }
    }

    public boolean isTranslucentTree(IsoObject object) {
        if (object instanceof IsoTree tree) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            FBORenderLevels renderLevels = object.square.chunk.getRenderLevels(playerIndex);
            if (!renderLevels.inStencilRect) {
                return false;
            } else {
                IsoGridSquare square = object.square;
                square.IsOnScreen();
                float sx = square.cachedScreenX - IsoCamera.frameState.offX;
                float sy = square.cachedScreenY - IsoCamera.frameState.offY;
                IsoCell cell = IsoWorld.instance.currentCell;
                return sx + 32 * Core.tileScale <= cell.stencilX1
                        || sx - 32 * Core.tileScale >= cell.stencilX2
                        || sy + 32 * Core.tileScale <= cell.stencilY1
                        || sy - 96 * Core.tileScale >= cell.stencilY2
                    ? false
                    : square.x >= PZMath.fastfloor(IsoCamera.frameState.camCharacterX)
                        && square.y >= PZMath.fastfloor(IsoCamera.frameState.camCharacterY)
                        && IsoCamera.frameState.camCharacterSquare != null;
            }
        } else {
            return false;
        }
    }

    private float calculateObjectTargetAlpha(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderLayer renderLayer = object.getRenderInfo(playerIndex).layer;
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getType();
        }

        if (renderLayer != ObjectRenderLayer.MinusFloor && renderLayer != ObjectRenderLayer.MinusFloorSE && renderLayer != ObjectRenderLayer.Translucent) {
            return 1.0F;
        } else {
            boolean isOpenDoor = object instanceof IsoDoor door && door.open || object instanceof IsoThumpable isoThumpable && isoThumpable.open;
            if (isOpenDoor && object.getProperties() != null && !object.getProperties().has("GarageDoor")) {
                return 0.6F;
            } else {
                boolean isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite != null && object.sprite.cutW;
                boolean isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite != null && object.sprite.cutN;
                return !isWestDoorOrWall && !isNorthDoorOrWall
                    ? this.calculateObjectTargetAlpha_NotDoorOrWall(object)
                    : this.calculateObjectTargetAlpha_DoorOrWall(object);
            }
        }
    }

    private float calculateObjectTargetAlpha_DoorOrWall(IsoObject object) {
        if (object.sprite == null) {
            return 1.0F;
        } else if (object.sprite.cutW && object.sprite.cutN) {
            return 1.0F;
        } else {
            IsoObjectType t = object.sprite.getType();
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoGridSquare square = object.getSquare();
            IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
            if (object.isFascia() && this.shouldHideFascia(playerIndex, object)) {
                object.setAlphaAndTarget(playerIndex, 0.0F);
                return 0.0F;
            } else {
                int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
                if (squareS == null) {
                    boolean var10000 = false;
                } else {
                    squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
                }

                if (squareE == null) {
                    boolean var20 = false;
                } else {
                    squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
                }

                if (t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite.cutW) {
                    IsoObjectType doorFrType = IsoObjectType.doorFrW;
                    IsoObjectType doorType = IsoObjectType.doorW;
                    boolean isDoor = t == doorFrType || t == doorType;
                    boolean isWindow = object instanceof IsoWindow;
                    boolean isCutaway = (cutawaySelf & 2) != 0;
                    if ((isDoor || isWindow) && isCutaway) {
                        if (isDoor && !this.hasSeenDoorW(playerIndex, square)) {
                            return 0.0F;
                        }

                        if (isWindow && !this.hasSeenWindowW(playerIndex, square)) {
                            return 0.0F;
                        }

                        return 0.4F;
                    }
                } else if (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite.cutN) {
                    IsoObjectType doorFrType = IsoObjectType.doorFrN;
                    IsoObjectType doorType = IsoObjectType.doorN;
                    boolean isDoor = t == doorFrType || t == doorType;
                    boolean isWindow = object instanceof IsoWindow;
                    boolean isCutaway = (cutawaySelf & 1) != 0;
                    if ((isDoor || isWindow) && isCutaway) {
                        if (isDoor && !this.hasSeenDoorN(playerIndex, square)) {
                            return 0.0F;
                        }

                        if (isWindow && !this.hasSeenWindowN(playerIndex, square)) {
                            return 0.0F;
                        }

                        return 0.4F;
                    }
                }

                return 1.0F;
            }
        }
    }

    private boolean hasSeenDoorW(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenDoorW = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject obj = objectArray[i];
            IsoSprite sprite = obj.sprite;
            IsoObjectType type = sprite == null ? IsoObjectType.MAX : sprite.getType();
            if (type == IsoObjectType.doorFrW || type == IsoObjectType.doorW) {
                IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
                bHasSeenDoorW |= true;
            }
        }

        return bHasSeenDoorW;
    }

    private boolean hasSeenDoorN(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenDoorN = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject obj = objectArray[i];
            IsoSprite sprite = obj.sprite;
            IsoObjectType type = sprite == null ? IsoObjectType.MAX : sprite.getType();
            if (type == IsoObjectType.doorFrN || type == IsoObjectType.doorN) {
                IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
                bHasSeenDoorN |= true;
            }
        }

        return bHasSeenDoorN;
    }

    private boolean hasSeenWindowW(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenWindowW = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject obj = objectArray[i];
            if (square.isWindowOrWindowFrame(obj, false)) {
                IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
                bHasSeenWindowW |= true;
            }
        }

        return bHasSeenWindowW;
    }

    private boolean hasSeenWindowN(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenWindowN = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject obj = objectArray[i];
            if (square.isWindowOrWindowFrame(obj, true)) {
                IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
                bHasSeenWindowN |= true;
            }
        }

        return bHasSeenWindowN;
    }

    private float calculateObjectTargetAlpha_NotDoorOrWall(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGridSquare square = object.getSquare();
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getType();
        }

        if (object instanceof IsoCurtain curtain) {
            IsoObject attachedTo = curtain.getObjectAttachedTo();
            if (attachedTo != null && square.getTargetDarkMulti(playerIndex) <= attachedTo.getSquare().getTargetDarkMulti(playerIndex)) {
                return this.calculateObjectTargetAlpha_NotDoorOrWall(attachedTo);
            }
        }

        if (object instanceof IsoBarricade barricade) {
            BarricadeAble attachedTo = barricade.getBarricadedObject();
            if (attachedTo instanceof IsoObject isoObject && square.getTargetDarkMulti(playerIndex) <= attachedTo.getSquare().getTargetDarkMulti(playerIndex)) {
                return this.calculateObjectTargetAlpha_NotDoorOrWall(isoObject);
            }
        }

        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        boolean bCutawayNorth = (cutawaySelf & 1) != 0;
        boolean bCutawayWest = (cutawaySelf & 2) != 0;
        if (object instanceof IsoWindowFrame windowFrame) {
            return this.calculateWindowTargetAlpha(playerIndex, object, windowFrame.getOppositeSquare(), windowFrame.getNorth());
        } else if (object instanceof IsoWindow window) {
            return this.calculateWindowTargetAlpha(playerIndex, object, window.getOppositeSquare(), window.getNorth());
        } else {
            boolean bIsRoof = t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT;
            boolean bIsValidOverhang = bIsRoof && PZMath.fastfloor(IsoCamera.frameState.camCharacterZ) == square.getZ() && square.getBuilding() == null;
            if (bIsValidOverhang && FBORenderCutaways.getInstance().CanBuildingSquareOccludePlayer(square, playerIndex)) {
                return 0.05F;
            } else if (object.isFascia() && this.shouldHideFascia(playerIndex, object)) {
                object.setAlphaAndTarget(playerIndex, 0.0F);
                return 0.0F;
            } else {
                if (IsoCamera.frameState.camCharacterSquare == null || IsoCamera.frameState.camCharacterSquare.getRoom() != square.getRoom()) {
                    boolean bCutaway = false;
                    if (square.has(IsoFlagType.cutN) && square.has(IsoFlagType.cutW)) {
                        bCutaway = bCutawayNorth || bCutawayWest;
                    } else if (square.has(IsoFlagType.cutW)) {
                        bCutaway = bCutawayWest;
                    } else if (square.has(IsoFlagType.cutN)) {
                        bCutaway = bCutawayNorth;
                    }

                    if (bCutaway) {
                        return square.isCanSee(playerIndex) ? 0.25F : 0.0F;
                    }
                }

                if (!this.isPotentiallyObscuringObject(object) || !this.perPlayerData[playerIndex].isSquareObscuringPlayer(square)) {
                    return 1.0F;
                } else if (object.sprite != null && object.sprite.getProperties().has(IsoFlagType.attachedCeiling)) {
                    return 0.25F;
                } else {
                    return object.isStairsObject() ? 0.5F : 0.66F;
                }
            }
        }
    }

    public float calculateWindowTargetAlpha(int playerIndex, IsoObject object, IsoGridSquare oppositeSq, boolean bNorth) {
        IsoGridSquare square = object.getSquare();
        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        boolean bCutawayNorth = (cutawaySelf & 1) != 0;
        boolean bCutawayWest = (cutawaySelf & 2) != 0;
        float targetAlpha = 1.0F;
        if (object.getTargetAlpha(playerIndex) < 1.0E-4F && oppositeSq != null && oppositeSq != square && oppositeSq.lighting[playerIndex].bSeen()) {
            targetAlpha = oppositeSq.lighting[playerIndex].darkMulti() * 2.0F;
        }

        if (targetAlpha > 0.75F && (bCutawayNorth && bNorth || bCutawayWest && !bNorth)) {
            float maxOpacity = 0.75F;
            float minOpacity = 0.1F;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player != null) {
                float maxFadeDistanceSquared = 25.0F;
                float distanceSquared = PZMath.min(IsoUtils.DistanceToSquared(player.getX(), player.getY(), square.x + 0.5F, square.y + 0.5F), 25.0F);
                float fadeAmount = PZMath.lerp(0.1F, 0.75F, 1.0F - distanceSquared / 25.0F);
                targetAlpha = Math.max(fadeAmount, 0.1F);
            } else {
                targetAlpha = 0.1F;
            }
        }

        return targetAlpha;
    }

    public void renderFloor(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer == ObjectRenderLayer.Floor) {
                this.renderFloor(object);
            }
        }
    }

    public void renderFloor(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoGridSquare square = object.square;
        IndieGL.glAlphaFunc(516, 0.0F);
        object.setTargetAlpha(playerIndex, renderInfo.targetAlpha);
        object.setAlpha(playerIndex, renderInfo.targetAlpha);
        if (DebugOptions.instance.terrain.renderTiles.renderGridSquares.getValue()) {
            if (object.sprite != null) {
                IndieGL.glDepthMask(true);
                if (object.sprite.getProperties().getSlopedSurfaceDirection() != null && square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0) {
                    IsoSprite sprite = IsoSpriteManager.instance.getSprite("ramps_01_23");
                    this.defColorInfo.set(square.getLightInfo(playerIndex));
                    if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                        this.defColorInfo.set(1.0F, 1.0F, 1.0F, this.defColorInfo.a);
                    }

                    sprite.render(object, square.x, square.y, square.z, object.getDir(), object.offsetX, object.offsetY, this.defColorInfo, true);
                } else {
                    FloorShaper attachedFloorShaper = FloorShaperAttachedSprites.instance;
                    FloorShaper floorShaper;
                    if (!object.getProperties().has(IsoFlagType.diamondFloor) && !object.getProperties().has(IsoFlagType.water)) {
                        floorShaper = FloorShaperDeDiamond.instance;
                    } else {
                        floorShaper = FloorShaperDiamond.instance;
                    }

                    IsoWaterGeometry water = square.z == 0 ? square.getWater() : null;
                    boolean isShore = water != null && water.isbShore() && IsoWater.getInstance().getShaderEnable();
                    float depth0 = water == null ? 0.0F : water.depth[0];
                    float depth1 = water == null ? 0.0F : water.depth[3];
                    float depth2 = water == null ? 0.0F : water.depth[2];
                    float depth3 = water == null ? 0.0F : water.depth[1];
                    int col0 = square.getVertLight(0, playerIndex);
                    int col1 = square.getVertLight(1, playerIndex);
                    int col2 = square.getVertLight(2, playerIndex);
                    int col3 = square.getVertLight(3, playerIndex);
                    if (this.isBlackedOutBuildingSquare(square)) {
                        float fade = instance.getBlackedOutRoomFadeRatio(square);
                        col0 = Color.lerpABGR(col0, -16777216, fade);
                        col1 = Color.lerpABGR(col1, -16777216, fade);
                        col2 = Color.lerpABGR(col2, -16777216, fade);
                        col3 = Color.lerpABGR(col3, -16777216, fade);
                    }

                    if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
                        col0 = -65536;
                        col1 = -65536;
                        col2 = -16776961;
                        col3 = -16776961;
                    }

                    attachedFloorShaper.setShore(isShore);
                    attachedFloorShaper.setWaterDepth(depth0, depth1, depth2, depth3);
                    attachedFloorShaper.setVertColors(col0, col1, col2, col3);
                    floorShaper.setShore(isShore);
                    floorShaper.setWaterDepth(depth0, depth1, depth2, depth3);
                    floorShaper.setVertColors(col0, col1, col2, col3);
                    TileSeamModifier.instance.setShore(isShore);
                    TileSeamModifier.instance.setWaterDepth(depth0, depth1, depth2, depth3);
                    TileSeamModifier.instance.setVertColors(col0, col1, col2, col3);
                    IsoGridSquare.setBlendFunc();
                    Shader floorShader = null;
                    IndieGL.StartShader(floorShader, playerIndex);
                    this.defColorInfo.set(1.0F, 1.0F, 1.0F, 1.0F);
                    object.renderFloorTile(square.x, square.y, square.z, this.defColorInfo, true, false, floorShader, floorShaper, attachedFloorShaper);
                    IndieGL.EndShader();
                }
            }
        }
    }

    private void renderFishSplashes(int playerIndex, ArrayList<IsoGridSquare> squares) {
        IndieGL.glBlendFunc(770, 771);

        for (int i = 0; i < squares.size(); i++) {
            IsoGridSquare square = squares.get(i);
            ColorInfo lightInfo = square.getLightInfo(playerIndex);
            square.renderFishSplash(playerIndex, lightInfo);
        }
    }

    private void renderVegetation(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer == ObjectRenderLayer.Vegetation) {
                this.renderVegetation(object);
            }
        }
    }

    private void renderVegetation(IsoObject object) {
        this.renderMinusFloor(object);
    }

    private void renderCorpsesInWorld(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);

            for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                if (renderLevels.isOnScreen(z) && z == renderLevels.getMinLevel(z)) {
                    ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Corpses(z);

                    for (int j = 0; j < squares.size(); j++) {
                        IsoGridSquare square = squares.get(j);
                        this.renderCorpses(square, square, false);
                    }
                }
            }
        }
    }

    private void renderCorpses(IsoGridSquare square, IsoGridSquare renderSquare, boolean bInChunkTexture) {
        if (this.shouldRenderSquare(renderSquare)) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            ColorInfo lightInfo = square.getLightInfo(playerIndex);
            FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);

            for (int i = 0; i < square.getStaticMovingObjects().size(); i++) {
                IsoMovingObject mov = square.getStaticMovingObjects().get(i);
                if ((mov.sprite != null || mov instanceof IsoDeadBody) && mov instanceof IsoDeadBody isoDeadBody) {
                    if (bInChunkTexture) {
                        if (renderSquare == mov.getRenderSquare()) {
                            FBORenderChunk renderChunk = renderLevels.getFBOForLevel(square.z, Core.getInstance().getZoom(playerIndex));
                            FBORenderCorpses.getInstance().render(renderChunk.index, isoDeadBody);
                        }
                    } else {
                        mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
                    }
                }
            }

            int size = square.getMovingObjects().size();

            for (int ix = 0; ix < size; ix++) {
                IsoMovingObject mov = square.getMovingObjects().get(ix);
                if (mov != null && mov.sprite != null) {
                    boolean bOnFloor = mov.isOnFloor();
                    if (bOnFloor && mov instanceof IsoZombie zombie) {
                        bOnFloor = zombie.isProne();
                        if (!BaseVehicle.renderToTexture) {
                            bOnFloor = false;
                        }
                    }

                    if (bOnFloor) {
                        mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
                    }
                }
            }
        }
    }

    private void renderItemsInWorld(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);

            for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                if (renderLevels.isOnScreen(z) && z == renderLevels.getMinLevel(z)) {
                    ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Items(z);

                    for (int j = 0; j < squares.size(); j++) {
                        IsoGridSquare square = squares.get(j);
                        if (square.chunk == chunk) {
                            this.renderWorldInventoryObjects(square, square, false);
                        } else {
                            IsoGridSquare renderSquare = chunk.getGridSquare(0, 0, square.z);
                            this.renderWorldInventoryObjects(square, renderSquare, false);
                        }
                    }
                }
            }
        }
    }

    private void renderMinusFloor(IsoChunk chunk, IsoGridSquare square) {
        this.renderMinusFloor(chunk, square, square.getObjects());
    }

    private void renderMinusFloor(IsoChunk chunk, IsoGridSquare square, PZArrayList<IsoObject> objectList) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        boolean bForceRender = FBORenderCutaways.getInstance().isForceRenderSquare(playerIndex, square);
        IsoObject[] objects = objectList.getElements();
        int numObjects = objectList.size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer == ObjectRenderLayer.MinusFloor) {
                IsoGridSquare renderSquare = object.getRenderSquare();
                if (renderSquare != null && chunk == renderSquare.chunk) {
                    if (bForceRender) {
                        IsoSpriteGrid spriteGrid = object.getSpriteGrid();
                        if (spriteGrid == null || spriteGrid.getLevels() == 1) {
                            continue;
                        }
                    }

                    this.renderMinusFloor(object);
                }
            }
        }
    }

    private void renderMinusFloor(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getType();
        }

        if (object.getProperties() != null && object.getProperties().has(IsoFlagType.NeverCutaway)) {
            boolean var8 = true;
        } else {
            boolean var10000 = false;
        }

        boolean bNeverCutaway = false;
        boolean isWestDoorOrWall = !bNeverCutaway && (t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite != null && object.sprite.cutW);
        boolean isNorthDoorOrWall = !bNeverCutaway && (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite != null && object.sprite.cutN);
        IndieGL.glAlphaFunc(516, 0.0F);
        object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
        IsoGridSquare.setBlendFunc();
        if (object.sprite != null && (isWestDoorOrWall || isNorthDoorOrWall)) {
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.doorsAndWalls.getValue()) {
                this.renderMinusFloor_DoorOrWall(object);
            }
        } else if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.objects.getValue()) {
            this.renderMinusFloor_NotDoorOrWall(object);
        }
    }

    private void renderMinusFloor_DoorOrWall(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoGridSquare square = object.square;
        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getType();
        }

        IndieGL.glAlphaFunc(516, 0.0F);
        object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
        this.defColorInfo.set(1.0F, 1.0F, 1.0F, 1.0F);
        int stenciled = 0;
        Shader wallRenderShader = null;
        boolean bHasSeenDoorN = false;
        boolean bHasSeenDoorW = false;
        boolean bHasSeenWindowN = false;
        boolean bHasSeenWindowW = false;
        boolean bCouldSee = square.lighting[playerIndex].bCouldSee();
        lowestCutawayObject = null;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject obj = objects[i];
            IsoObjectType t2 = obj.sprite == null ? IsoObjectType.MAX : obj.sprite.getType();
            if (square.isWindowOrWindowFrame(obj, true) && (cutawaySelf & 1) != 0) {
                IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
                bHasSeenWindowN = bCouldSee || toNorth != null && toNorth.isCouldSee(playerIndex);
                lowestCutawayObject = obj;
                break;
            }

            if (square.isWindowOrWindowFrame(obj, false) && (cutawaySelf & 2) != 0) {
                IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
                bHasSeenWindowW = bCouldSee || toWest != null && toWest.isCouldSee(playerIndex);
                lowestCutawayObject = obj;
                break;
            }

            if (obj.sprite != null
                && (t2 == IsoObjectType.doorFrN || t2 == IsoObjectType.doorN || obj.sprite.getProperties().has(IsoFlagType.DoorWallN))
                && (cutawaySelf & 1) != 0) {
                IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
                bHasSeenDoorN = bCouldSee || toNorth != null && toNorth.isCouldSee(playerIndex);
                lowestCutawayObject = obj;
                break;
            }

            if (obj.sprite != null
                && (t2 == IsoObjectType.doorFrW || t2 == IsoObjectType.doorW || obj.sprite.getProperties().has(IsoFlagType.DoorWallW))
                && (cutawaySelf & 2) != 0) {
                IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
                bHasSeenDoorW = bCouldSee || toWest != null && toWest.isCouldSee(playerIndex);
                lowestCutawayObject = obj;
                break;
            }
        }

        IsoGridSquare.circleStencil = true;
        boolean bNeverCutaway = object.getProperties() != null && object.getProperties().has(IsoFlagType.NeverCutaway);
        if (bNeverCutaway) {
            IsoGridSquare.circleStencil = false;
        }

        IndieGL.glDepthMask(true);
        if (object.isWallSE()) {
            square.DoWallLightingW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorW, bHasSeenWindowW, wallRenderShader);
        } else if (object.sprite.cutW && object.sprite.cutN) {
            square.DoWallLightingNW(
                object,
                0,
                cutawaySelf,
                cutawayN,
                cutawayS,
                cutawayW,
                cutawayE,
                bHasSeenDoorN,
                bHasSeenDoorW,
                bHasSeenWindowN,
                bHasSeenWindowW,
                wallRenderShader
            );
        } else if (t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite.cutW) {
            square.DoWallLightingW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorW, bHasSeenWindowW, wallRenderShader);
        } else if (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite.cutN) {
            square.DoWallLightingN(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorN, bHasSeenWindowN, wallRenderShader);
        }
    }

    void renderMinusFloor_NotDoorOrWall(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoGridSquare square = object.square;
        IndieGL.glAlphaFunc(516, 0.0F);
        if (this.renderTranslucentOnly) {
            object.setTargetAlpha(playerIndex, renderInfo.targetAlpha);
            if (object.getType() == IsoObjectType.WestRoofT) {
                object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
            }
        } else {
            object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
        }

        ColorInfo lightInfo = this.sanitizeLightInfo(playerIndex, square);
        boolean bForceRender = FBORenderCutaways.getInstance().isForceRenderSquare(playerIndex, square);
        if (bForceRender) {
            IsoGridSquare below = this.cell.getGridSquare(square.x, square.y, square.z - 1);
            if (below != null) {
                lightInfo = this.sanitizeLightInfo(playerIndex, below);
            }
        }

        if (object instanceof IsoTree isoTree) {
            isoTree.renderFlag = this.isTranslucentTree(object);
        }

        IndieGL.glDepthMask(true);
        if (this.isRoofTileWithPossibleSeamSameLevel(square, object.sprite, IsoDirections.E)) {
            IsoGridSquare square2 = square.getAdjacentSquare(IsoDirections.E);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.E);
        }

        if (this.isRoofTileWithPossibleSeamSameLevel(square, object.sprite, IsoDirections.S)) {
            IsoGridSquare square2 = square.getAdjacentSquare(IsoDirections.S);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.S);
        }

        if (this.isRoofTileWithPossibleSeamBelow(square, object.sprite, IsoDirections.E)) {
            IsoGridSquare square2 = this.cell.getGridSquare(square.x + 1, square.y, square.z - 1);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.E);
        }

        if (this.isRoofTileWithPossibleSeamBelow(square, object.sprite, IsoDirections.S)) {
            IsoGridSquare square2 = this.cell.getGridSquare(square.x, square.y + 1, square.z - 1);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.S);
        }

        if (object instanceof IsoWindow window) {
            IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
            int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int stenciled = 0;
            Shader wallRenderShader = null;
            boolean bHasSeenDoorN = false;
            boolean bHasSeenDoorW = false;
            boolean bHasSeenWindowN = false;
            boolean bHasSeenWindowW = false;
            if (window.getNorth() && object == square.getWall(true)) {
                IsoGridSquare.circleStencil = false;
                square.DoWallLightingN(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, false, false, wallRenderShader);
                return;
            }

            if (!window.getNorth() && object == square.getWall(false)) {
                IsoGridSquare.circleStencil = false;
                square.DoWallLightingW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, false, false, wallRenderShader);
                return;
            }
        }

        object.render(square.x, square.y, square.z, lightInfo, true, false, null);
    }

    private boolean isRoofTileset(IsoSprite sprite) {
        return sprite == null ? false : sprite.getRoofProperties() != null;
    }

    private boolean isRoofTileWithPossibleSeamSameLevel(IsoGridSquare square, IsoSprite sprite, IsoDirections dir) {
        if (sprite == null) {
            return false;
        } else if (dir == IsoDirections.E && PZMath.coordmodulo(square.x, 8) != 7) {
            return false;
        } else if (dir == IsoDirections.S && PZMath.coordmodulo(square.y, 8) != 7) {
            return false;
        } else {
            return !this.isRoofTileset(sprite) ? false : sprite.getRoofProperties().hasPossibleSeamSameLevel(dir);
        }
    }

    private boolean isRoofTileWithPossibleSeamBelow(IsoGridSquare square, IsoSprite sprite, IsoDirections dir) {
        if (sprite == null) {
            return false;
        } else {
            return !this.isRoofTileset(sprite) ? false : sprite.getRoofProperties().hasPossibleSeamLevelBelow(dir);
        }
    }

    private boolean areRoofTilesJoinedSameLevel(IsoSprite sprite1, IsoSprite sprite2, IsoDirections dir) {
        if (!this.isRoofTileset(sprite1)) {
            return false;
        } else if (!this.isRoofTileset(sprite2)) {
            return false;
        } else if (dir == IsoDirections.E) {
            return sprite1.getRoofProperties().isJoinedSameLevelEast(sprite2.getRoofProperties());
        } else {
            return dir == IsoDirections.S ? sprite1.getRoofProperties().isJoinedSameLevelSouth(sprite2.getRoofProperties()) : false;
        }
    }

    private boolean areRoofTilesJoinedLevelBelow(IsoSprite sprite1, IsoSprite sprite2, IsoDirections dir) {
        if (!this.isRoofTileset(sprite1)) {
            return false;
        } else if (!this.isRoofTileset(sprite2)) {
            return false;
        } else if (dir == IsoDirections.E) {
            return sprite1.getRoofProperties().isJoinedLevelBelowEast(sprite2.getRoofProperties());
        } else {
            return dir == IsoDirections.S ? sprite1.getRoofProperties().isJoinedLevelBelowSouth(sprite2.getRoofProperties()) : false;
        }
    }

    private void renderJoinedRoofTile(int playerIndex, IsoObject object, IsoGridSquare square2, IsoDirections dir) {
        if (square2 != null) {
            IsoGridSquare square = object.getSquare();

            for (int i = 0; i < square2.getObjects().size(); i++) {
                IsoObject object2 = square2.getObjects().get(i);
                if (square.z == square2.z
                    ? this.areRoofTilesJoinedSameLevel(object.sprite, object2.sprite, dir)
                    : this.areRoofTilesJoinedLevelBelow(object.sprite, object2.sprite, dir)) {
                    ObjectRenderInfo renderInfo2 = object2.getRenderInfo(playerIndex);
                    if (renderInfo2.targetAlpha == 0.0F || square.chunk != square2.chunk) {
                        float renderWidth = renderInfo2.renderWidth;
                        float renderHeight = renderInfo2.renderHeight;
                        this.calculateObjectRenderInfo(playerIndex, object2.square, object2);
                        renderInfo2.renderWidth = renderWidth;
                        renderInfo2.renderHeight = renderHeight;
                        if (object2.getRenderInfo(playerIndex).targetAlpha == 0.0F) {
                            continue;
                        }
                    }

                    if (!(object2.getRenderInfo(playerIndex).targetAlpha < 1.0F)) {
                        object2.renderSquareOverride = square;
                        object2.renderDepthAdjust = -1.0E-5F;
                        object2.sx = 0.0F;
                        if (this.renderTranslucentOnly) {
                            object2.setTargetAlpha(playerIndex, object2.getRenderInfo(playerIndex).targetAlpha);
                        } else {
                            object2.setAlphaAndTarget(playerIndex, object2.getRenderInfo(playerIndex).targetAlpha);
                        }

                        object2.render(square2.x, square2.y, square2.z, this.sanitizeLightInfo(playerIndex, square2), true, false, null);
                        object2.sx = 0.0F;
                        object2.renderSquareOverride = null;
                        object2.renderDepthAdjust = 0.0F;
                        break;
                    }
                }
            }
        }
    }

    private void renderWorldInventoryObjects(IsoGridSquare square, IsoGridSquare renderSquare, boolean bChunkTexture) {
        if (this.shouldRenderSquare(renderSquare)) {
            this.tempWorldInventoryObjects.clear();
            PZArrayUtil.addAll(this.tempWorldInventoryObjects, square.getWorldObjects());
            this.timSort.doSort(this.tempWorldInventoryObjects.getElements(), (o1, o2) -> {
                float d1 = o1.xoff * o1.xoff + o1.yoff * o1.yoff;
                float d2 = o2.xoff * o2.xoff + o2.yoff * o2.yoff;
                if (d1 == d2) {
                    return 0;
                } else {
                    return d1 > d2 ? 1 : -1;
                }
            }, 0, this.tempWorldInventoryObjects.size());
            int playerIndex = IsoCamera.frameState.playerIndex;

            for (int i = 0; i < this.tempWorldInventoryObjects.size(); i++) {
                IsoWorldInventoryObject object = this.tempWorldInventoryObjects.get(i);
                ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                if (renderInfo.layer == ObjectRenderLayer.WorldInventoryObject && (!bChunkTexture || renderSquare == object.getRenderSquare())) {
                    this.renderWorldInventoryObject(object, bChunkTexture);
                }
            }
        }
    }

    private void renderWorldInventoryObject(IsoWorldInventoryObject worldObj, boolean bChunkTexture) {
        IsoGridSquare square = worldObj.getSquare();
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (bChunkTexture) {
            IsoGridSquare renderSquare = worldObj.getRenderSquare();
            if (!(worldObj.zoff < 0.01F)
                && (this.isTableTopObjectFadedOut(playerIndex, square) || this.isTableTopObjectSquareCutaway(playerIndex, square, worldObj.zoff))) {
                FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Items(renderSquare.z);
                if (!squares.contains(square)) {
                    squares.add(square);
                }

                return;
            }

            if (!worldObj.getItem().getScriptItem().isWorldRender()) {
                return;
            }

            if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(worldObj.getItem())) {
                FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
                FBORenderChunk renderChunk = renderLevels.getFBOForLevel(renderSquare.z, Core.getInstance().getZoom(playerIndex));
                FBORenderItems.getInstance().render(renderChunk.index, worldObj);
                return;
            }
        }

        if (this.renderTranslucentOnly) {
            if (worldObj.zoff < 0.01F) {
                return;
            }

            if (!this.isTableTopObjectFadedOut(playerIndex, square) && !this.isTableTopObjectSquareCutaway(playerIndex, square, worldObj.zoff)) {
                return;
            }
        }

        ColorInfo lightInfo = square.getLightInfo(playerIndex);
        worldObj.render(square.x, square.y, square.z, lightInfo, true, false, null);
    }

    private boolean isTableTopObjectSquareCutaway(int playerIndex, IsoGridSquare square, float zoff) {
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
        boolean cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0;
        boolean cutawayS = squareS != null && squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0;
        boolean cutawayE = squareE != null && squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0;
        if (IsoCamera.frameState.camCharacterSquare == null || IsoCamera.frameState.camCharacterSquare.getRoom() != square.getRoom()) {
            boolean bCutaway = cutawaySelf;
            if (square.has(IsoFlagType.cutN) && square.has(IsoFlagType.cutW)) {
                bCutaway = cutawaySelf | cutawayS | cutawayE;
            } else if (square.has(IsoFlagType.cutW)) {
                bCutaway = cutawaySelf | cutawayS;
            } else if (square.has(IsoFlagType.cutN)) {
                bCutaway = cutawaySelf | cutawayE;
            }

            if (bCutaway) {
                return true;
            }
        }

        return false;
    }

    private boolean isTableTopObjectFadedOut(int playerIndex, IsoGridSquare square) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        return this.listContainsLocation(perPlayerData1.squaresObscuringPlayer, square.x, square.y, square.z)
            || this.listContainsLocation(perPlayerData1.fadingInSquares, square.x, square.y, square.z);
    }

    private void renderMinusFloorSE(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = numObjects - 1; i >= 0; i--) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer == ObjectRenderLayer.MinusFloorSE) {
                this.renderMinusFloorSE(object);
            }
        }
    }

    private void renderMinusFloorSE(IsoObject object) {
        this.renderMinusFloor(object);
    }

    private void renderTranslucentFloor(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer == ObjectRenderLayer.TranslucentFloor) {
                this.renderTranslucent(object);
            }
        }
    }

    private void renderTranslucent(IsoGridSquare square, boolean bAttachedSE) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[bAttachedSE ? numObjects - 1 - i : i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer == ObjectRenderLayer.Translucent && bAttachedSE == square.isSpriteOnSouthOrEastWall(object)) {
                this.renderTranslucent(object);
            }
        }
    }

    public void renderTranslucent(IsoObject object) {
        IndieGL.glDefaultBlendFunc();
        IsoSprite sprite = object.getSprite();
        if (sprite != null && sprite.getProperties().has(IsoFlagType.transparentFloor)) {
            this.renderFloor(object);
        } else if (object instanceof IsoDoor || object instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
            object.sx = 0.0F;
            this.renderMinusFloor_DoorOrWall(object);
        } else if (object.getType() == IsoObjectType.doorFrW || object.getType() == IsoObjectType.doorFrN) {
            this.renderMinusFloor_DoorOrWall(object);
        } else if (sprite != null && sprite.solidfloor && object.square.getWater() != null && object.square.getWater().isbShore()) {
            this.renderFloor(object);
        } else {
            if (sprite == null || !sprite.cutN && !sprite.cutW) {
                object.getRenderInfo(IsoCamera.frameState.playerIndex).targetAlpha = this.calculateObjectTargetAlpha_NotDoorOrWall(object);
            } else {
                object.getRenderInfo(IsoCamera.frameState.playerIndex).targetAlpha = this.calculateObjectTargetAlpha_DoorOrWall(object);
            }

            IsoObjectType t = IsoObjectType.MAX;
            if (object.sprite != null) {
                t = object.sprite.getType();
            }

            boolean isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite != null && object.sprite.cutW;
            boolean isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite != null && object.sprite.cutN;
            if (object.sprite != null && (isWestDoorOrWall || isNorthDoorOrWall)) {
                if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.doorsAndWalls.getValue()) {
                    this.renderMinusFloor_DoorOrWall(object);
                }
            } else if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.objects.getValue()) {
                this.renderMinusFloor_NotDoorOrWall(object);
            }

            if (!(object instanceof IsoBarbecue) || !FBORenderObjectHighlight.getInstance().isRendering()) {
                if (object.hasAnimatedAttachments()) {
                    this.renderAnimatedAttachments(object);
                }
            }
        }
    }

    private void renderAnimatedAttachments(int playerIndex) {
        this.renderTranslucentOnly = true;
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.chunksWithAnimatedAttachments.size(); i++) {
            IsoChunk chunk = perPlayerData1.chunksWithAnimatedAttachments.get(i);
            this.renderOneChunk_AnimatedAttachments(playerIndex, chunk);
        }
    }

    private void renderOneChunk_AnimatedAttachments(int playerIndex, IsoChunk chunk) {
        IndieGL.enableDepthTest();
        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(false);

        for (int zza = chunk.minLevel; zza <= chunk.maxLevel; zza++) {
            this.renderOneLevel_AnimatedAttachments(playerIndex, chunk, zza);
        }

        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel_AnimatedAttachments(int playerIndex, IsoChunk chunk, int level) {
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        if (renderLevels.isOnScreen(level)) {
            FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(level);
            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_AnimatedAttachments(level);

            for (int i = 0; i < squares.size(); i++) {
                IsoGridSquare square = squares.get(i);
                if (square.z == level && levelData.shouldRenderSquare(playerIndex, square) && square.IsOnScreen()) {
                    this.renderAnimatedAttachments(square);
                }
            }
        }
    }

    private void renderAnimatedAttachments(IsoGridSquare square) {
        this.renderAnimatedAttachments = true;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();

        for (int i = 0; i < numObjects; i++) {
            IsoObject object = objects[i];
            if (object.getRenderInfo(playerIndex).layer != ObjectRenderLayer.Translucent && object.hasAnimatedAttachments()) {
                this.renderAnimatedAttachments(object);
            }
        }

        this.renderAnimatedAttachments = false;
    }

    public void renderAnimatedAttachments(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = object.square.getLightInfo(playerIndex);
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            this.defColorInfo.set(1.0F, 1.0F, 1.0F, lightInfo.a);
            lightInfo = this.defColorInfo;
        }

        IndieGL.glDefaultBlendFunc();
        object.renderAnimatedAttachments(object.getX(), object.getY(), object.getZ(), lightInfo);
    }

    private void renderFlies(int playerIndex) {
        this.renderTranslucentOnly = true;
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.chunksWithFlies.size(); i++) {
            IsoChunk chunk = perPlayerData1.chunksWithFlies.get(i);
            this.renderOneChunk_Flies(playerIndex, chunk);
        }
    }

    private void renderOneChunk_Flies(int playerIndex, IsoChunk chunk) {
        IndieGL.enableDepthTest();
        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(false);
        IndieGL.enableBlend();
        IndieGL.glBlendFunc(770, 771);

        for (int zza = chunk.minLevel; zza <= chunk.maxLevel; zza++) {
            this.renderOneLevel_Flies(playerIndex, chunk, zza);
        }

        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel_Flies(int playerIndex, IsoChunk chunk, int level) {
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        if (renderLevels.isOnScreen(level)) {
            FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(level);
            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Flies(level);

            for (int i = 0; i < squares.size(); i++) {
                IsoGridSquare square = squares.get(i);
                if (square.z == level && levelData.shouldRenderSquare(playerIndex, square) && square.IsOnScreen() && square.hasFlies()) {
                    CorpseFlies.render(square.x, square.y, square.z);
                }
            }
        }
    }

    private void updateChunkLighting(int playerIndex) {
        if (!DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            if (DebugOptions.instance.fboRenderChunk.updateSquareLightInfo.getValue()) {
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
                if (perPlayerData1.lightingUpdateCounter != LightingJNI.getUpdateCounter(playerIndex)) {
                    perPlayerData1.lightingUpdateCounter = LightingJNI.getUpdateCounter(playerIndex);
                    int chunkLevelCount = 0;
                    this.sortedChunks.clear();
                    PZArrayUtil.addAll(this.sortedChunks, perPlayerData1.onScreenChunks);
                    this.timSort.doSort(this.sortedChunks.getElements(), Comparator.comparingInt(a -> a.lightingUpdateCounter), 0, this.sortedChunks.size());

                    for (int i = 0; i < this.sortedChunks.size(); i++) {
                        IsoChunk chunk = this.sortedChunks.get(i);
                        boolean updated = false;

                        for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                            if (this.updateChunkLevelLighting(playerIndex, chunk, z)) {
                                updated = true;
                                chunk.lightingUpdateCounter = perPlayerData1.lightingUpdateCounter;
                            }
                        }

                        if (DebugOptions.instance.lightingSplitUpdate.getValue() && updated) {
                            if (++chunkLevelCount >= 5) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean updateChunkLevelLighting(int playerIndex, IsoChunk chunk, int level) {
        if (level >= chunk.minLevel && level <= chunk.maxLevel) {
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            if (!renderLevels.isOnScreen(level)) {
                return false;
            } else if (!LightingJNI.getChunkDirty(playerIndex, chunk.wx, chunk.wy, level + 32)) {
                return false;
            } else {
                FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(level);
                IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(level)];

                for (int i = 0; i < squares.length; i++) {
                    IsoGridSquare square = squares[i];
                    if (square != null && levelData.shouldRenderSquare(playerIndex, square)) {
                        square.cacheLightInfo();
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private void renderOneLevel_Blood(IsoChunk chunk, int zza, int minX, int minY, int maxX, int maxY) {
        if (DebugOptions.instance.terrain.renderTiles.bloodDecals.getValue()) {
            int OptionBloodDecals = Core.getInstance().getOptionBloodDecals();
            if (OptionBloodDecals != 0) {
                float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
                int playerIndex = IsoCamera.frameState.playerIndex;
                FBORenderCutaways.ChunkLevelData cutawayLevel = chunk.getCutawayDataForLevel(zza);
                if (this.splatByType.isEmpty()) {
                    for (int i = 0; i < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; i++) {
                        this.splatByType.add(new ArrayList<>());
                    }
                }

                for (int n = 0; n < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; n++) {
                    this.splatByType.get(n).clear();
                }

                IsoChunk ch = chunk;
                int cx = chunk.wx * 8;
                int cy = chunk.wy * 8;

                for (int n = 0; n < ch.floorBloodSplatsFade.size(); n++) {
                    IsoFloorBloodSplat b = ch.floorBloodSplatsFade.get(n);
                    if ((b.index < 1 || b.index > 10 || IsoChunk.renderByIndex[OptionBloodDecals - 1][b.index - 1] != 0)
                        && !(cx + b.x < minX)
                        && !(cx + b.x > maxX)
                        && !(cy + b.y < minY)
                        && !(cy + b.y > maxY)
                        && PZMath.fastfloor(b.z) == zza
                        && b.type >= 0
                        && b.type < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) {
                        b.chunk = ch;
                        this.splatByType.get(b.type).add(b);
                    }
                }

                for (int i = 0; i < ch.floorBloodSplats.size(); i++) {
                    IsoFloorBloodSplat b = ch.floorBloodSplats.get(i);
                    if ((b.index < 1 || b.index > 10 || IsoChunk.renderByIndex[OptionBloodDecals - 1][b.index - 1] != 0)
                        && !(cx + b.x < minX)
                        && !(cx + b.x > maxX)
                        && !(cy + b.y < minY)
                        && !(cy + b.y > maxY)
                        && PZMath.fastfloor(b.z) == zza
                        && b.type >= 0
                        && b.type < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) {
                        b.chunk = ch;
                        this.splatByType.get(b.type).add(b);
                    }
                }

                for (int nx = 0; nx < this.splatByType.size(); nx++) {
                    ArrayList<IsoFloorBloodSplat> splats = this.splatByType.get(nx);
                    if (!splats.isEmpty()) {
                        String type = IsoFloorBloodSplat.FLOOR_BLOOD_TYPES[nx];
                        IsoSprite use = null;
                        if (!IsoFloorBloodSplat.spriteMap.containsKey(type)) {
                            IsoSprite sp = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                            sp.LoadSingleTexture(type);
                            IsoFloorBloodSplat.spriteMap.put(type, sp);
                            use = sp;
                        } else {
                            use = IsoFloorBloodSplat.spriteMap.get(type);
                        }

                        for (int ix = 0; ix < splats.size(); ix++) {
                            IsoFloorBloodSplat b = splats.get(ix);
                            ColorInfo inf = this.defColorInfo;
                            inf.r = 1.0F;
                            inf.g = 1.0F;
                            inf.b = 1.0F;
                            inf.a = 0.27F;
                            float aa = (b.x + b.y / b.x) * (b.type + 1);
                            float bb = aa * b.x / b.y * (b.type + 1) / (aa + b.y);
                            float cc = bb * aa * bb * b.x / (b.y + 2.0F);
                            aa *= 42367.543F;
                            bb *= 6367.123F;
                            cc *= 23367.133F;
                            aa %= 1000.0F;
                            bb %= 1000.0F;
                            cc %= 1000.0F;
                            aa /= 1000.0F;
                            bb /= 1000.0F;
                            cc /= 1000.0F;
                            if (aa > 0.25F) {
                                aa = 0.25F;
                            }

                            inf.r -= aa * 2.0F;
                            inf.g -= aa * 2.0F;
                            inf.b -= aa * 2.0F;
                            inf.r += bb / 3.0F;
                            inf.g -= cc / 3.0F;
                            inf.b -= cc / 3.0F;
                            float deltaAge = worldAge - b.worldAge;
                            if (deltaAge >= 0.0F && deltaAge < 72.0F) {
                                float f = 1.0F - deltaAge / 72.0F;
                                inf.r *= 0.2F + f * 0.8F;
                                inf.g *= 0.2F + f * 0.8F;
                                inf.b *= 0.2F + f * 0.8F;
                                inf.a *= 0.25F + f * 0.75F;
                            } else {
                                inf.r *= 0.2F;
                                inf.g *= 0.2F;
                                inf.b *= 0.2F;
                                inf.a *= 0.25F;
                            }

                            if (b.fade > 0) {
                                inf.a = inf.a * (b.fade / (PerformanceSettings.getLockFPS() * 5.0F));
                                if (--b.fade == 0) {
                                    b.chunk.floorBloodSplatsFade.remove(b);
                                }
                            }

                            IsoGridSquare square = b.chunk.getGridSquare(PZMath.fastfloor(b.x), PZMath.fastfloor(b.y), PZMath.fastfloor(b.z));
                            if (cutawayLevel.shouldRenderSquare(playerIndex, square)) {
                                if (this.isBlackedOutBuildingSquare(square)) {
                                    inf.set(0.0F, 0.0F, 0.0F, inf.a);
                                }

                                if (square != null) {
                                    int L0 = square.getVertLight(0, playerIndex);
                                    int L1 = square.getVertLight(1, playerIndex);
                                    int L2 = square.getVertLight(2, playerIndex);
                                    int L3 = square.getVertLight(3, playerIndex);
                                    float r0 = Color.getRedChannelFromABGR(L0);
                                    float g0 = Color.getGreenChannelFromABGR(L0);
                                    float b0 = Color.getBlueChannelFromABGR(L0);
                                    float r1 = Color.getRedChannelFromABGR(L1);
                                    float g1 = Color.getGreenChannelFromABGR(L1);
                                    float b1 = Color.getBlueChannelFromABGR(L1);
                                    float r2 = Color.getRedChannelFromABGR(L2);
                                    float g2 = Color.getGreenChannelFromABGR(L2);
                                    float b2 = Color.getBlueChannelFromABGR(L2);
                                    float r3 = Color.getRedChannelFromABGR(L3);
                                    float g3 = Color.getGreenChannelFromABGR(L3);
                                    float b3 = Color.getBlueChannelFromABGR(L3);
                                    inf.r *= (r0 + r1 + r2 + r3) / 4.0F;
                                    inf.g *= (g0 + g1 + g2 + g3) / 4.0F;
                                    inf.b *= (b0 + b1 + b2 + b3) / 4.0F;
                                }

                                use.renderBloodSplat(b.chunk.wx * 8 + b.x, b.chunk.wy * 8 + b.y, b.z, inf);
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderOneLevel_Blood(IsoChunk chunk, int dwx, int dwy, int zza) {
        IsoChunk chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + dwx, chunk.wy + dwy);
        if (chunk2 != null) {
            int CPW = 8;
            int minX = chunk.wx * 8 - 1;
            int minY = chunk.wy * 8 - 1;
            int maxX = (chunk.wx + 1) * 8 + 1;
            int maxY = (chunk.wy + 1) * 8 + 1;
            this.renderOneLevel_Blood(chunk2, zza, minX, minY, maxX, maxY);
        }
    }

    private void recalculateAnyGridStacks(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.dirtyRecalcGridStack) {
            player.dirtyRecalcGridStack = false;
            WeatherFxMask.setDiamondIterDone(playerIndex);
        }
    }

    private boolean hasAnyDirtyChunkTextures(int playerIndex) {
        float zoom = Core.getInstance().getZoom(playerIndex);
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);

            for (int z = c.minLevel; z <= c.maxLevel; z++) {
                if (renderLevels.isOnScreen(z) && renderLevels.isDirty(z, zoom)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void calculateOccludingSquares(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);

            for (int z = c.minLevel; z <= c.maxLevel; z++) {
                if (renderLevels.isOnScreen(z)) {
                    FBORenderCutaways.ChunkLevelData levelData = c.getCutawayData().getDataForLevel(z);
                    boolean bChanged = levelData.calculateOccludingSquares(
                        playerIndex,
                        perPlayerData1.occludedGridX1,
                        perPlayerData1.occludedGridY1,
                        perPlayerData1.occludedGridX2,
                        perPlayerData1.occludedGridY2,
                        perPlayerData1.occludedGrid
                    );
                    if (bChanged) {
                        FBORenderOcclusion.getInstance().invalidateOverlappedChunkLevels(playerIndex, c, z);
                    }
                }
            }
        }
    }

    private int calculateRenderedSquaresCount(int playerIndex, IsoChunk chunk, int level) {
        int minLevel = chunk.getRenderLevels(playerIndex).getMinLevel(level);
        int maxLevel = chunk.getRenderLevels(playerIndex).getMaxLevel(level);
        int renderedSquaresCount = 0;

        for (int z = minLevel; z <= maxLevel; z++) {
            FBORenderCutaways.ChunkLevelData chunkLevelData = chunk.getCutawayDataForLevel(z);
            IsoGridSquare[] squares = chunk.getSquaresForLevel(z);

            for (int i = 0; i < squares.length; i++) {
                IsoGridSquare square = squares[i];
                if (chunkLevelData.shouldRenderSquare(playerIndex, square) && !FBORenderOcclusion.getInstance().isOccluded(square.x, square.y, z)) {
                    if (DebugOptions.instance.cheapOcclusionCount.getValue()) {
                        return 1;
                    }

                    renderedSquaresCount++;
                }
            }
        }

        return renderedSquaresCount;
    }

    private void prepareChunksForUpdating(int playerIndex) {
        float zoom = Core.getInstance().getZoom(playerIndex);
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);

            for (int z = c.minLevel; z <= c.maxLevel; z++) {
                if (z == renderLevels.getMinLevel(z) && renderLevels.isOnScreen(z) && renderLevels.isDirty(z, zoom)) {
                    this.prepareChunkForUpdating(playerIndex, c, z);
                }
            }
        }
    }

    private void prepareChunkForUpdating(int playerIndex, IsoChunk chunk, int z) {
        if (chunk != null) {
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            if (renderLevels.isOnScreen(z)) {
                int minLevel = renderLevels.getMinLevel(z);
                int maxLevel = renderLevels.getMaxLevel(z);

                for (int z2 = minLevel; z2 <= maxLevel; z2++) {
                    FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z2);

                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            IsoGridSquare sq = chunk.getGridSquare(x, y, z2);
                            levelData.squareFlags[playerIndex][x + y * 8] = 0;
                            if (sq != null) {
                                sq.cacheLightInfo();
                                if (sq.getLightInfo(playerIndex) != null && this.shouldRenderSquare(sq)) {
                                    levelData.squareFlags[playerIndex][x + y * 8] = (byte)(levelData.squareFlags[playerIndex][x + y * 8] | 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldRenderSquare(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            int playerIndex = IsoCamera.frameState.playerIndex;
            return square.getLightInfo(playerIndex) != null && square.lighting[playerIndex] != null
                ? FBORenderCutaways.getInstance().shouldRenderBuildingSquare(playerIndex, square)
                : false;
        }
    }

    private void renderTranslucentFloorObjects(int playerIndex, int z, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (DebugOptions.instance.fboRenderChunk.renderTranslucentFloor.getValue()) {
            this.renderTranslucentOnly = true;
            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

            for (int i = 0; i < perPlayerData1.chunksWithTranslucentFloor.size(); i++) {
                IsoChunk chunk = perPlayerData1.chunksWithTranslucentFloor.get(i);
                this.renderOneChunk_TranslucentFloor(chunk, playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
            }
        }
    }

    private void renderOneChunk_TranslucentFloor(
        IsoChunk c, int playerIndex, int zza, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis
    ) {
        if (c != null) {
            if (!c.lightingNeverDone[playerIndex]) {
                IndieGL.enableDepthTest();
                IndieGL.glDepthFunc(515);
                IndieGL.glDepthMask(false);
                this.renderOneLevel_TranslucentFloor(playerIndex, c, zza);
                IndieGL.glDepthMask(false);
                IndieGL.glDepthFunc(519);
            }
        }
    }

    private void renderOneLevel_TranslucentFloor(int playerIndex, IsoChunk c, int level) {
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        if (renderLevels.isOnScreen(level)) {
            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_TranslucentFloor(level);

            for (int i = 0; i < squares.size(); i++) {
                IsoGridSquare square = squares.get(i);
                if (square.z == level && square.IsOnScreen()) {
                    this.renderTranslucentFloor(square);
                }
            }
        }
    }

    private void renderTranslucentObjects(int playerIndex, int z, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (DebugOptions.instance.fboRenderChunk.renderTranslucentNonFloor.getValue()) {
            this.renderTranslucentOnly = true;
            FBORenderTrees.current = FBORenderTrees.alloc();
            FBORenderTrees.current.init();
            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

            for (int i = 0; i < perPlayerData1.chunksWithTranslucentNonFloor.size(); i++) {
                IsoChunk chunk = perPlayerData1.chunksWithTranslucentNonFloor.get(i);
                this.renderOneChunk_Translucent(chunk, playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
            }

            SpriteRenderer.instance.drawGeneric(FBORenderTrees.current);
        }
    }

    private void renderOneChunk_Translucent(IsoChunk c, int playerIndex, int zza, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (c != null && c.IsOnScreen(true)) {
            if (!c.lightingNeverDone[playerIndex]) {
                IndieGL.enableDepthTest();
                IndieGL.glDepthFunc(515);
                IndieGL.glDepthMask(false);
                this.renderOneLevel_Translucent(playerIndex, c, zza);
                IndieGL.glDepthMask(false);
                IndieGL.glDepthFunc(519);
            }
        }
    }

    private void renderOneLevel_Translucent(int playerIndex, IsoChunk c, int level) {
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        if (renderLevels.isOnScreen(level)) {
            FBORenderCutaways.ChunkLevelData levelData = c.getCutawayDataForLevel(level);
            ArrayList<IsoGridSquare> squaresObjects = renderLevels.getCachedSquares_TranslucentNonFloor(level);
            ArrayList<IsoGridSquare> squaresItems = renderLevels.getCachedSquares_Items(level);
            if (squaresObjects.size() + squaresItems.size() != 0) {
                PZArrayList<IsoGridSquare> sorted = this.tempSquares;
                sorted.clear();

                try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("Sort")) {
                    PZArrayUtil.addAll(sorted, squaresItems);

                    for (int i = 0; i < squaresObjects.size(); i++) {
                        IsoGridSquare square = squaresObjects.get(i);
                        if (!sorted.contains(square)) {
                            sorted.add(square);
                        }
                    }
                }

                this.timSort.doSort(sorted.getElements(), (o1, o2) -> {
                    int worldRight = IsoWorld.instance.getMetaGrid().getMaxX() * 256;
                    int i1 = o1.x + o1.y * worldRight;
                    int i2 = o2.x + o2.y * worldRight;
                    return i1 - i2;
                }, 0, sorted.size());

                for (int ix = 0; ix < sorted.size(); ix++) {
                    IsoGridSquare square = sorted.get(ix);
                    if (square.z == level && levelData.shouldRenderSquare(playerIndex, square) && square.IsOnScreen()) {
                        this.renderTranslucent(square, false);
                        if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue() && !square.getWorldObjects().isEmpty()) {
                            if (square.chunk == c) {
                                this.renderWorldInventoryObjects(square, square, false);
                            } else {
                                IsoGridSquare renderSquare = c.getGridSquare(0, 0, square.z);
                                this.renderWorldInventoryObjects(square, renderSquare, false);
                            }
                        }

                        this.renderTranslucent(square, true);
                    }
                }
            }
        }
    }

    private void renderCorpseShadows(int playerIndex) {
        if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
            if (Core.getInstance().getOptionCorpseShadows()) {
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

                for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                    IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                    FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);

                    for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                        if (renderLevels.isOnScreen(z) && z == renderLevels.getMinLevel(z)) {
                            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Corpses(z);

                            for (int j = 0; j < squares.size(); j++) {
                                IsoGridSquare square = squares.get(j);

                                for (int k = 0; k < square.getStaticMovingObjects().size(); k++) {
                                    if (square.getStaticMovingObjects().get(k) instanceof IsoDeadBody deadBody && !square.HasStairs()) {
                                        deadBody.renderShadow();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkMannequinRenderDirection(int playerIndex) {
        for (int i = 0; i < this.mannequinList.size(); i++) {
            IsoMannequin mannequin = this.mannequinList.get(i);
            if (mannequin.getObjectIndex() == -1) {
                this.mannequinList.remove(i--);
            } else {
                mannequin.checkRenderDirection(playerIndex);
            }
        }
    }

    private void renderMannequinShadows(int playerIndex) {
        for (int i = 0; i < this.mannequinList.size(); i++) {
            IsoMannequin mannequin = this.mannequinList.get(i);
            if (mannequin.getObjectIndex() == -1) {
                this.mannequinList.remove(i--);
            } else if (this.shouldRenderSquare(mannequin.getSquare())) {
                mannequin.renderShadow(mannequin.getX() + 0.5F, mannequin.getY() + 0.5F, mannequin.getZ());
                if (mannequin.shouldRenderEachFrame()) {
                    ColorInfo lightInfo = mannequin.getSquare().getLightInfo(playerIndex);
                    mannequin.render(mannequin.getX(), mannequin.getY(), mannequin.getZ(), lightInfo, true, false, null);
                }
            }
        }
    }

    private void renderOpaqueObjectsEvent(int playerIndex) {
        int buildX;
        int buildY;
        int buildZ;
        if (JoypadManager.instance.getFromPlayer(playerIndex) == null) {
            if (UIManager.getPickedTile() == null) {
                return;
            }

            buildX = PZMath.fastfloor(UIManager.getPickedTile().x);
            buildY = PZMath.fastfloor(UIManager.getPickedTile().y);
            buildZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        } else {
            buildX = PZMath.fastfloor(IsoCamera.frameState.camCharacterX);
            buildY = PZMath.fastfloor(IsoCamera.frameState.camCharacterY);
            buildZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        }

        if (IsoWorld.instance.isValidSquare(buildX, buildY, buildZ)) {
            IsoGridSquare square = this.cell.getGridSquare(buildX, buildY, buildZ);
            LuaEventManager.triggerEvent("RenderOpaqueObjectsInWorld", playerIndex, buildX, buildY, buildZ, square);
        }
    }

    private void renderPlayers(int playerIndex) {
        if (!GameClient.client) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null) {
                    this.renderPlayer(playerIndex, player);
                }
            }
        } else {
            for (IsoPlayer player : GameClient.IDToPlayerMap.values()) {
                this.renderPlayer(playerIndex, player);
            }
        }
    }

    private void renderPlayer(int playerIndex, IsoPlayer player) {
        if (this.cell.getObjectList().contains(player)) {
            if (player.getCurrentSquare() != null) {
                if (player.isOnScreen()) {
                    if (player.getCurrentSquare().getLightInfo(playerIndex) != null) {
                        if (FBORenderCutaways.getInstance().shouldRenderBuildingSquare(playerIndex, player.getCurrentSquare())) {
                            if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
                                player.renderShadow(player.getX(), player.getY(), player.getZ());
                            }

                            player.render(
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                player.getCurrentSquare().getLightInfo(IsoPlayer.getPlayerIndex()),
                                true,
                                false,
                                null
                            );
                            this.debugChunkStateRenderPlayer(player);
                        }
                    }
                }
            }
        }
    }

    private void renderMovingObjects() {
        this.renderTranslucentOnly = true;
        ArrayList<IsoMovingObject> objList = IsoWorld.instance.getCell().getObjectList();

        for (int i = 0; i < objList.size(); i++) {
            IsoMovingObject isoMovingObject = objList.get(i);
            this.renderMovingObject(isoMovingObject);
        }

        this.renderTranslucentOnly = false;
        SpriteRenderer.instance.renderQueued();
    }

    private void renderMovingObject(IsoMovingObject isoMovingObject) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (isoMovingObject.getClass() != IsoPlayer.class) {
            if (isoMovingObject.getCurrentSquare() != null) {
                if (isoMovingObject.isOnScreen()) {
                    if (isoMovingObject.getCurrentSquare().getLightInfo(playerIndex) != null) {
                        if (this.shouldRenderSquare(isoMovingObject.getCurrentSquare())) {
                            if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
                                IsoGameCharacter chr = Type.tryCastTo(isoMovingObject, IsoGameCharacter.class);
                                if (chr != null && chr.getCurrentSquare() != null && chr.getCurrentSquare().HasStairs() && chr.isRagdoll()) {
                                    boolean vehicle = true;
                                } else if (chr != null) {
                                    chr.renderShadow(isoMovingObject.getX(), isoMovingObject.getY(), isoMovingObject.getZ());
                                }

                                if (isoMovingObject instanceof BaseVehicle vehicle) {
                                    vehicle.renderShadow();
                                }
                            }

                            isoMovingObject.render(
                                isoMovingObject.getX(),
                                isoMovingObject.getY(),
                                isoMovingObject.getZ(),
                                isoMovingObject.getCurrentSquare().getLightInfo(playerIndex),
                                true,
                                false,
                                null
                            );
                        }
                    }
                }
            }
        }
    }

    private void renderWater(int playerIndex) {
        if (DebugOptions.instance.weather.waterPuddles.getValue()
            && DebugOptions.instance.terrain.renderTiles.water.getValue()
            && DebugOptions.instance.terrain.renderTiles.waterBody.getValue()) {
            if (!(IsoCamera.frameState.camCharacterZ < 0.0F)) {
                this.waterSquares.clear();
                this.waterAttachSquares.clear();
                this.fishSplashSquares.clear();
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

                for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                    IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                    if (0 >= chunk.minLevel && 0 <= chunk.maxLevel && chunk.getRenderLevels(playerIndex).isOnScreen(0)) {
                        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);

                        for (int j = 0; j < renderLevels.waterSquares.size(); j++) {
                            IsoGridSquare square = renderLevels.waterSquares.get(j);
                            if (square.IsOnScreen()) {
                                IsoObject floor = square.getFloor();
                                if (floor == null || floor.getRenderInfo(playerIndex).layer != ObjectRenderLayer.TranslucentFloor) {
                                    if (IsoWater.getInstance().getShaderEnable() && square.getWater() != null && square.getWater().isValid()) {
                                        this.waterSquares.add(square);
                                    }

                                    if (square.shouldRenderFishSplash(playerIndex)) {
                                        this.fishSplashSquares.add(square);
                                    }
                                }
                            }
                        }

                        for (int jx = 0; jx < renderLevels.waterAttachSquares.size(); jx++) {
                            IsoGridSquare square = renderLevels.waterAttachSquares.get(jx);
                            if (square.IsOnScreen() && IsoWater.getInstance().getShaderEnable() && square.getWater() != null && square.getWater().isValid()) {
                                this.waterAttachSquares.add(square);
                            }
                        }
                    }
                }

                if (!this.waterSquares.isEmpty()) {
                    IsoWater.getInstance().render(this.waterSquares);
                }

                for (int ix = 0; ix < this.waterAttachSquares.size(); ix++) {
                    IsoGridSquare square = this.waterAttachSquares.get(ix);
                    IsoObject[] objects = square.getObjects().getElements();
                    int numObjects = square.getObjects().size();

                    for (int jxx = 0; jxx < numObjects; jxx++) {
                        IsoObject object = objects[jxx];
                        if (object != null
                            && object.getRenderInfo(playerIndex).layer == ObjectRenderLayer.None
                            && object.getAttachedAnimSprite() != null
                            && !object.getAttachedAnimSprite().isEmpty()) {
                            this.renderTranslucentOnly = true;
                            object.renderAttachedAndOverlaySprites(
                                object.dir, square.x, square.y, square.z, square.getLightInfo(playerIndex), true, false, null, null
                            );
                            this.renderTranslucentOnly = false;
                        }
                    }
                }

                if (!this.fishSplashSquares.isEmpty()) {
                    this.renderFishSplashes(playerIndex, this.fishSplashSquares);
                }
            }
        }
    }

    private void renderWaterShore(int playerIndex) {
        if (DebugOptions.instance.weather.waterPuddles.getValue()
            && DebugOptions.instance.terrain.renderTiles.water.getValue()
            && DebugOptions.instance.terrain.renderTiles.waterShore.getValue()) {
            if (IsoWater.getInstance().getShaderEnable()) {
                if (!(IsoCamera.frameState.camCharacterZ < 0.0F)) {
                    this.waterSquares.clear();
                    this.waterAttachSquares.clear();
                    FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

                    for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                        IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                        if (0 >= chunk.minLevel && 0 <= chunk.maxLevel && chunk.getRenderLevels(playerIndex).isOnScreen(0)) {
                            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);

                            for (int j = 0; j < renderLevels.waterShoreSquares.size(); j++) {
                                IsoGridSquare square = renderLevels.waterShoreSquares.get(j);
                                if (square.IsOnScreen() && square.getWater() != null && square.getWater().isbShore()) {
                                    this.waterSquares.add(square);
                                    this.waterAttachSquares.add(square);
                                }
                            }
                        }
                    }

                    if (!this.waterSquares.isEmpty()) {
                        IsoWater.getInstance().renderShore(this.waterSquares);
                    }

                    for (int ix = 0; ix < this.waterAttachSquares.size(); ix++) {
                        IsoGridSquare square = this.waterAttachSquares.get(ix);
                        IsoObject[] objects = square.getObjects().getElements();
                        int numObjects = square.getObjects().size();

                        for (int jx = 0; jx < numObjects; jx++) {
                            IsoObject object = objects[jx];
                            if (object != null
                                && object.getRenderInfo(playerIndex).layer == ObjectRenderLayer.None
                                && object.getAttachedAnimSprite() != null
                                && !object.getAttachedAnimSprite().isEmpty()) {
                                this.renderTranslucentOnly = true;
                                object.renderAttachedAndOverlaySprites(
                                    object.dir, square.x, square.y, square.z, square.getLightInfo(playerIndex), true, false, null, null
                                );
                                this.renderTranslucentOnly = false;
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderPuddles(int playerIndex) {
        if (IsoPuddles.getInstance().shouldRenderPuddles()) {
            IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
            int maxZ = chunkMap.maxHeight;
            if (Core.getInstance().getPerfPuddles() > 0) {
                maxZ = 0;
            }

            FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

            for (int z = 0; z <= maxZ; z++) {
                this.waterSquares.clear();

                for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                    IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                    if (z >= chunk.minLevel && z <= chunk.maxLevel) {
                        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                        if (renderLevels.isOnScreen(z)) {
                            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Puddles(z);
                            if (!squares.isEmpty()) {
                                FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);

                                for (int j = 0; j < squares.size(); j++) {
                                    IsoGridSquare square = squares.get(j);
                                    if (square.getZ() == z && levelData.shouldRenderSquare(playerIndex, square) && square.IsOnScreen()) {
                                        IsoObject floor = square.getFloor();
                                        if (floor != null
                                            && (
                                                PerformanceSettings.puddlesQuality >= 2
                                                    || floor.getRenderInfo(playerIndex).layer != ObjectRenderLayer.TranslucentFloor
                                            )) {
                                            IsoPuddlesGeometry puddlesGeometry = square.getPuddles();
                                            if (puddlesGeometry != null && puddlesGeometry.shouldRender()) {
                                                this.waterSquares.add(square);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                IsoPuddles.getInstance().render(this.waterSquares, z);
            }
        }
    }

    private void renderPuddleDebug(int playerIndex) {
        if (IsoPuddles.getInstance().shouldRenderPuddles()) {
            Texture tex = Texture.getSharedTexture("media/textures/Item_Waterdrop_Grey.png");
            if (tex != null) {
                IndieGL.disableDepthTest();
                IndieGL.StartShader(0);
                int maxZ = 0;
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

                for (int z = 0; z <= 0; z++) {
                    for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                        IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);

                        for (int x = 0; x < 8; x++) {
                            for (int y = 0; y < 8; y++) {
                                IsoGridSquare square = chunk.getGridSquare(x, y, z);
                                if (square != null) {
                                    float level = square.getPuddlesInGround();
                                    if (!(level <= 0.09F)) {
                                        int sqx = square.getX();
                                        int sqy = square.getY();
                                        float sx = IsoUtils.XToScreen(sqx, sqy, z, 0) - IsoCamera.frameState.offX;
                                        float sy = IsoUtils.YToScreen(sqx, sqy, z, 0) - IsoCamera.frameState.offY;
                                        sx -= tex.getWidth() / 2.0F * Core.tileScale;
                                        sy -= tex.getHeight() / 2.0F * Core.tileScale;
                                        float opacity = PZMath.clamp(0.1F + level, 0.2F, 1.0F);
                                        SpriteRenderer.instance
                                            .render(
                                                tex,
                                                sx,
                                                sy,
                                                tex.getWidth() * Core.tileScale,
                                                tex.getHeight() * Core.tileScale,
                                                opacity,
                                                opacity,
                                                opacity,
                                                opacity,
                                                null
                                            );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderPuddlesTranslucentFloorsOnly(int playerIndex, int z) {
        if (IsoPuddles.getInstance().shouldRenderPuddles()) {
            if (Core.getInstance().getPerfPuddles() <= 0 || z == 0) {
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
                this.waterSquares.clear();

                for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                    IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                    if (z >= chunk.minLevel && z <= chunk.maxLevel) {
                        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                        if (renderLevels.isOnScreen(z) && !renderLevels.getCachedSquares_Puddles(z).isEmpty()) {
                            FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
                            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_TranslucentFloor(z);

                            for (int j = 0; j < squares.size(); j++) {
                                IsoGridSquare square = squares.get(j);
                                if (levelData.shouldRenderSquare(playerIndex, square) && square.IsOnScreen()) {
                                    IsoObject floor = square.getFloor();
                                    if (floor != null && floor.getRenderInfo(playerIndex).layer == ObjectRenderLayer.TranslucentFloor) {
                                        IsoPuddlesGeometry puddlesGeometry = square.getPuddles();
                                        if (puddlesGeometry != null && puddlesGeometry.shouldRender()) {
                                            this.waterSquares.add(square);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                IsoPuddles.getInstance().render(this.waterSquares, z);
            }
        }
    }

    private void renderPuddlesToChunkTexture(int playerIndex, int z, IsoChunk chunk) {
        if (IsoPuddles.getInstance().shouldRenderPuddles()) {
            if (z >= chunk.minLevel && z <= chunk.maxLevel) {
                if (chunk.getRenderLevels(playerIndex).isOnScreen(z)) {
                    this.waterSquares.clear();
                    FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
                    IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(z)];

                    for (int j = 0; j < squares.length; j++) {
                        IsoGridSquare square = squares[j];
                        if (levelData.shouldRenderSquare(playerIndex, square)) {
                            IsoObject floor = square.getFloor();
                            if (floor != null && floor.getRenderInfo(playerIndex).layer != ObjectRenderLayer.TranslucentFloor) {
                                IsoPuddlesGeometry puddlesGeometry = square.getPuddles();
                                if (puddlesGeometry != null && puddlesGeometry.shouldRender()) {
                                    this.waterSquares.add(square);
                                }
                            }
                        }
                    }

                    IsoPuddles.getInstance().renderToChunkTexture(this.waterSquares, z);
                }
            }
        }
    }

    private void renderRainSplashes(int playerIndex, int z) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        this.waterSquares.clear();

        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            if (z >= chunk.minLevel && z <= chunk.maxLevel && chunk.getRenderLevels(playerIndex).isOnScreen(z)) {
                IsoChunkLevel levelData = chunk.getLevelData(z);
                levelData.updateRainSplashes();
                levelData.renderRainSplashes(playerIndex);
            }
        }
    }

    private void renderFog(int playerIndex) {
        if (!(IsoCamera.frameState.camCharacterZ < 0.0F)) {
            if (PerformanceSettings.fogQuality != 2) {
                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
                ImprovedFog.getDrawer().startFrame();
                boolean bFirst = true;

                for (int z = 0; z <= 1; z++) {
                    if (ImprovedFog.startRender(playerIndex, z)) {
                        if (bFirst) {
                            bFirst = false;
                            ImprovedFog.startFrame(ImprovedFog.getDrawer());
                        }

                        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); i++) {
                            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                            if (z >= chunk.minLevel && z <= chunk.maxLevel) {
                                FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                                if (renderLevels.isOnScreen(z)) {
                                    FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
                                    IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(z)];

                                    for (int j = 0; j < squares.length; j++) {
                                        IsoGridSquare square = squares[j];
                                        if (levelData.shouldRenderSquare(playerIndex, square)) {
                                            IsoObject[] objects = square.getObjects().getElements();
                                            int numObjects = square.getObjects().size();

                                            for (int k = 0; k < numObjects; k++) {
                                                IsoObject object = objects[k];
                                                ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                                                if (renderInfo.layer == ObjectRenderLayer.MinusFloor) {
                                                    try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("ImprovedFog")) {
                                                        ImprovedFog.renderRowsBehind(square);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ImprovedFog.endRender();
                    }
                }

                ImprovedFog.getDrawer().endFrame();
            }
        }
    }

    public void handleDelayedLoading(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        object.getChunk().getRenderLevels(playerIndex).handleDelayedLoading(object);
        if (this.delayedLoadingTimerMs == 0L) {
            this.delayedLoadingTimerMs = System.currentTimeMillis() + 250L;
        }
    }

    private ColorInfo sanitizeLightInfo(int playerIndex, IsoGridSquare square) {
        ColorInfo lightInfo = square.getLightInfo(playerIndex);
        if (lightInfo == null) {
            lightInfo = this.defColorInfo;
        }

        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            this.defColorInfo.set(1.0F, 1.0F, 1.0F, lightInfo.a);
            lightInfo = this.defColorInfo;
        }

        return lightInfo;
    }

    private void debugChunkStateRenderPlayer(IsoPlayer player) {
        if (GameWindow.states.current == DebugChunkState.instance) {
            DebugChunkState.instance.drawObjectAtCursor();
            if (DebugChunkState.instance.getBoolean("ObjectAtCursor")) {
                if ("player".equals(DebugChunkState.instance.fromLua1("getObjectAtCursor", "id"))) {
                    float gridXf = DebugChunkState.instance.gridXf;
                    float gridYf = DebugChunkState.instance.gridYf;
                    int m_z = DebugChunkState.instance.z;
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)gridXf, (double)gridYf, (double)m_z);
                    if (square != null) {
                        float x = player.getX();
                        float y = player.getY();
                        float z = player.getZ();
                        IsoGridSquare psquare = player.getCurrentSquare();
                        float apparentZ = square.getApparentZ(gridXf % 1.0F, gridYf % 1.0F);
                        player.setX(gridXf);
                        player.setY(gridYf);
                        player.setZ(apparentZ);
                        player.setCurrent(square);
                        this.renderDebugChunkState = true;
                        player.render(gridXf, gridYf, apparentZ, new ColorInfo(), true, false, null);
                        this.renderDebugChunkState = false;
                        player.setX(x);
                        player.setY(y);
                        player.setZ(z);
                        player.setCurrent(psquare);
                    }
                }
            }
        }
    }

    private boolean checkDebugKeys(int playerIndex, int currentZ) {
        boolean bForceCutawayUpdate = false;
        if (Core.debug && GameKeyboard.isKeyPressed(28)) {
            IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);

            for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                    IsoChunk chunk = chunkMap.getChunk(x, y);
                    if (chunk != null && currentZ >= chunk.minLevel && currentZ <= chunk.maxLevel && chunk.IsOnScreen(true)) {
                        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                        if (renderLevels.isOnScreen(currentZ)) {
                            renderLevels.invalidateLevel(currentZ, 64L);
                            this.prepareChunkForUpdating(playerIndex, chunk, currentZ);
                            IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(currentZ)];

                            for (int i = 0; i < squares.length; i++) {
                                if (squares[i] != null) {
                                    squares[i].setPlayerCutawayFlag(playerIndex, 0, 0L);
                                }
                            }

                            chunk.getCutawayData().invalidateOccludedSquaresMaskForSeenRooms(playerIndex, currentZ);
                        }
                    }
                }
            }

            bForceCutawayUpdate = true;
        }

        return bForceCutawayUpdate;
    }

    public void renderSeamFix1_Floor(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix1.getValue()) {
            IsoGridSquare square = object.getSquare();
            IsoSprite sprite = object.getSprite();
            if (PZMath.coordmodulo(square.y, 8) == 7) {
                IsoGridSquare s = square.getAdjacentSquare(IsoDirections.S);
                if (s != null && s.getFloor() != null) {
                    sprite.render(
                        object,
                        x,
                        y,
                        z,
                        object.dir,
                        object.offsetX + 5.0F,
                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 5.0F,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                }
            }

            if (PZMath.coordmodulo(square.x, 8) == 7) {
                IsoGridSquare e = square.getAdjacentSquare(IsoDirections.E);
                if (e != null && e.getFloor() != null) {
                    sprite.render(
                        object,
                        x,
                        y,
                        z,
                        object.dir,
                        object.offsetX - 5.0F,
                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 5.0F,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                }
            }
        }
    }

    public void renderSeamFix2_Floor(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        if (!this.renderTranslucentOnly) {
            if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix2.getValue()) {
                IsoGridSquare square = object.getSquare();
                IsoSprite sprite = object.getSprite();
                IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                boolean bShoreS = squareS != null && squareS.getWater() != null && squareS.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
                if (PZMath.coordmodulo(square.y, 8) == 7 || bShoreS) {
                    IsoGridSquare s = square.getAdjacentSquare(IsoDirections.S);
                    if (s != null && s.getFloor() != null && (bShoreS || !s.has(IsoFlagType.water) || PerformanceSettings.waterQuality == 2)) {
                        IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouth;
                        if (sprite.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouthOneThird;
                        }

                        if (sprite.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouthTwoThirds;
                        }

                        object.sx = 0.0F;
                        if (bShoreS) {
                            object.renderDepthAdjust = -0.001F;
                        }

                        sprite.render(
                            object,
                            x,
                            y,
                            z,
                            object.dir,
                            object.offsetX + 6.0F,
                            object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                            stCol,
                            !object.isBlink(),
                            texdModifier
                        );
                        object.sx = 0.0F;
                        object.renderDepthAdjust = 0.0F;
                        IsoSprite.seamFix2 = null;
                    }
                }

                if (PZMath.coordmodulo(square.x, 8) == 7) {
                    IsoGridSquare e = square.getAdjacentSquare(IsoDirections.E);
                    if (e != null && e.getFloor() != null && (!e.has(IsoFlagType.water) || PerformanceSettings.waterQuality == 2)) {
                        IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEast;
                        if (sprite.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEastOneThird;
                        }

                        if (sprite.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEastTwoThirds;
                        }

                        object.sx = 0.0F;
                        sprite.render(
                            object,
                            x,
                            y,
                            z,
                            object.dir,
                            object.offsetX - 6.0F,
                            object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                            stCol,
                            !object.isBlink(),
                            texdModifier
                        );
                        object.sx = 0.0F;
                        IsoSprite.seamFix2 = null;
                    }
                }

                IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
                boolean bShoreN = squareN != null && squareN.getWater() != null && squareN.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
                if (bShoreN) {
                    IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouth;
                    object.sx = 0.0F;
                    object.renderSquareOverride2 = squareN;
                    object.renderDepthAdjust = -0.001F;
                    sprite.render(
                        object,
                        x,
                        y - 1.0F,
                        z,
                        object.dir,
                        object.offsetX,
                        object.offsetY + object.getRenderYOffset() * Core.tileScale,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                    object.sx = 0.0F;
                    object.renderSquareOverride2 = null;
                    object.renderDepthAdjust = 0.0F;
                    IsoSprite.seamFix2 = null;
                }

                IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
                boolean bShoreW = squareW != null && squareW.getWater() != null && squareW.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
                if (bShoreW) {
                    IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEast;
                    object.sx = 0.0F;
                    object.renderSquareOverride2 = squareW;
                    object.renderDepthAdjust = -0.001F;
                    sprite.render(
                        object,
                        x - 1.0F,
                        y,
                        z,
                        object.dir,
                        object.offsetX - 2.0F,
                        object.offsetY - 1.0F + object.getRenderYOffset() * Core.tileScale,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                    object.sx = 0.0F;
                    object.renderSquareOverride2 = null;
                    object.renderDepthAdjust = 0.0F;
                    IsoSprite.seamFix2 = null;
                }

                IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
                boolean bShoreE = squareE != null && squareE.getWater() != null && squareE.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
                if (bShoreE) {
                    IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEast;
                    object.sx = 0.0F;
                    sprite.render(
                        object,
                        x,
                        y,
                        z,
                        object.dir,
                        object.offsetX - 6.0F,
                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                    object.sx = 0.0F;
                    IsoSprite.seamFix2 = null;
                }
            }
        }
    }

    public void renderSeamFix1_Wall(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix1.getValue()) {
            IsoGridSquare square = object.getSquare();
            IsoSprite sprite = object.getSprite();
            if (sprite.getProperties().has(IsoFlagType.WallW) && PZMath.coordmodulo(square.y, 8) == 7) {
                IsoGridSquare s = square.getAdjacentSquare(IsoDirections.S);
                if (s != null && ((s.getWallType() & 4) != 0 || s.getWindowFrame(false) != null || s.has(IsoFlagType.DoorWallW))) {
                    sprite.renderWallSliceW(
                        object,
                        x,
                        y,
                        z,
                        object.dir,
                        object.offsetX,
                        object.offsetY + object.getRenderYOffset() * Core.tileScale,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                }
            }

            if (sprite.getProperties().has(IsoFlagType.WallN) && PZMath.coordmodulo(square.x, 8) == 7) {
                IsoGridSquare e = square.getAdjacentSquare(IsoDirections.E);
                if (e != null && ((e.getWallType() & 1) != 0 || e.getWindowFrame(true) != null || e.has(IsoFlagType.DoorWallN))) {
                    sprite.renderWallSliceN(
                        object,
                        x,
                        y,
                        z,
                        object.dir,
                        object.offsetX,
                        object.offsetY + object.getRenderYOffset() * Core.tileScale,
                        stCol,
                        !object.isBlink(),
                        texdModifier
                    );
                }
            }
        }
    }

    public void renderSeamFix2_Wall(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix2.getValue()) {
            IsoGridSquare square = object.getSquare();
            IsoSprite sprite = object.getSprite();
            if (!sprite.getProperties().has(IsoFlagType.HoppableN) && !sprite.getProperties().has(IsoFlagType.HoppableW)) {
                if (sprite.tileSheetIndex < 80 || sprite.tileSheetIndex > 82 || sprite.tilesetName == null || !sprite.tilesetName.equals("carpentry_02")) {
                    if (sprite.tileSheetIndex < 48 || sprite.tileSheetIndex > 55 || sprite.tilesetName == null || !sprite.tilesetName.equals("walls_logs")) {
                        if (sprite.tilesetName == null || !sprite.tilesetName.equals("walls_logs")) {
                            if (sprite.getProperties().has(IsoFlagType.WallNW) && texdModifier == WallShaperW.instance && PZMath.coordmodulo(square.y, 8) == 7) {
                                IsoGridSquare s = square.getAdjacentSquare(IsoDirections.S);
                                if (s != null && ((s.getWallType() & 4) != 0 || s.getWindowFrame(false) != null || s.has(IsoFlagType.DoorWallW))) {
                                    IsoSprite.seamFix2 = TileSeamManager.Tiles.WallSouth;
                                    object.sx = 0.0F;
                                    sprite.render(
                                        object,
                                        x,
                                        y,
                                        z,
                                        IsoDirections.NW,
                                        object.offsetX + 6.0F,
                                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                                        stCol,
                                        !object.isBlink(),
                                        texdModifier
                                    );
                                    object.sx = 0.0F;
                                    IsoSprite.seamFix2 = null;
                                }
                            }

                            if (sprite.getProperties().has(IsoFlagType.WallNW) && texdModifier == WallShaperN.instance && PZMath.coordmodulo(square.x, 8) == 7) {
                                IsoGridSquare e = square.getAdjacentSquare(IsoDirections.E);
                                if (e != null && ((e.getWallType() & 1) != 0 || e.getWindowFrame(true) != null || e.has(IsoFlagType.DoorWallN))) {
                                    IsoSprite.seamFix2 = TileSeamManager.Tiles.WallEast;
                                    object.sx = 0.0F;
                                    sprite.render(
                                        object,
                                        x,
                                        y,
                                        z,
                                        IsoDirections.NW,
                                        object.offsetX - 6.0F,
                                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                                        stCol,
                                        !object.isBlink(),
                                        texdModifier
                                    );
                                    object.sx = 0.0F;
                                    IsoSprite.seamFix2 = null;
                                }
                            }

                            if ((sprite.getProperties().has(IsoFlagType.WallW) || sprite.getProperties().has(IsoFlagType.WindowW))
                                && PZMath.coordmodulo(square.y, 8) == 7) {
                                IsoGridSquare s = square.getAdjacentSquare(IsoDirections.S);
                                if (s != null && ((s.getWallType() & 4) != 0 || s.getWindowFrame(false) != null || s.has(IsoFlagType.DoorWallW))) {
                                    IsoSprite.seamFix2 = TileSeamManager.Tiles.WallSouth;
                                    object.sx = 0.0F;
                                    sprite.render(
                                        object,
                                        x,
                                        y,
                                        z,
                                        IsoDirections.W,
                                        object.offsetX + 6.0F,
                                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                                        stCol,
                                        !object.isBlink(),
                                        texdModifier
                                    );
                                    object.sx = 0.0F;
                                    IsoSprite.seamFix2 = null;
                                }
                            }

                            if ((sprite.getProperties().has(IsoFlagType.WallN) || sprite.getProperties().has(IsoFlagType.WindowN))
                                && PZMath.coordmodulo(square.x, 8) == 7) {
                                IsoGridSquare e = square.getAdjacentSquare(IsoDirections.E);
                                if (e != null && ((e.getWallType() & 1) != 0 || e.getWindowFrame(true) != null || e.has(IsoFlagType.DoorWallN))) {
                                    IsoSprite.seamFix2 = TileSeamManager.Tiles.WallEast;
                                    object.sx = 0.0F;
                                    sprite.render(
                                        object,
                                        x,
                                        y,
                                        z,
                                        IsoDirections.N,
                                        object.offsetX - 6.0F,
                                        object.offsetY + object.getRenderYOffset() * Core.tileScale - 3.0F,
                                        stCol,
                                        !object.isBlink(),
                                        texdModifier
                                    );
                                    object.sx = 0.0F;
                                    IsoSprite.seamFix2 = null;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkSeenRooms(IsoPlayer player, int level) {
        if (!GameClient.client) {
            IsoBuilding building = player.getBuilding();
            if (building != null) {
                for (IsoRoom room : building.rooms) {
                    if (!room.def.explored && PZMath.abs(room.def.level - level) <= 1.0F) {
                        room.def.explored = true;
                        IsoWorld.instance.getCell().roomSpotted(room);
                    }
                }
            }
        }
    }

    private boolean shouldHideFascia(int playerIndex, IsoObject object) {
        IsoGridSquare square = object.getFasciaAttachedSquare();
        return square == null ? false : !FBORenderCutaways.getInstance().shouldRenderBuildingSquare(playerIndex, square);
    }

    private boolean checkBlackedOutBuildings(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        float playerX = IsoCamera.frameState.camCharacterX;
        float playerY = IsoCamera.frameState.camCharacterY;
        Vector2f closestPoint = BaseVehicle.allocVector2f();
        ArrayList<BuildingDef> collapsedBuildings = FBORenderCutaways.getInstance().getCollapsedBuildings();
        boolean bChanged = false;

        for (int i = 0; i < collapsedBuildings.size(); i++) {
            BuildingDef buildingDef = collapsedBuildings.get(i);
            float distSq = buildingDef.getClosestPoint(playerX, playerY, closestPoint);
            int index = perPlayerData1.blackedOutBuildings.indexOf(buildingDef);
            if (index == -1) {
                if (distSq > 100.0F) {
                    perPlayerData1.blackedOutBuildings.add(buildingDef);
                    buildingDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
                    bChanged = true;
                }
            } else if (distSq <= 100.0F) {
                perPlayerData1.blackedOutBuildings.remove(index);
                buildingDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
                bChanged = true;
            }
        }

        BaseVehicle.releaseVector2f(closestPoint);

        for (int ix = 0; ix < perPlayerData1.blackedOutBuildings.size(); ix++) {
            BuildingDef buildingDef = perPlayerData1.blackedOutBuildings.get(ix);
            int index = collapsedBuildings.indexOf(buildingDef);
            if (index == -1) {
                perPlayerData1.blackedOutBuildings.remove(ix--);
                buildingDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
                bChanged = true;
            }
        }

        return bChanged;
    }

    private void checkBlackedOutRooms(int playerIndex) {
        FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        ArrayList<LightingJNI.VisibleRoom> visibleRooms = LightingJNI.getVisibleRooms(playerIndex);
        if (visibleRooms != null) {
            visibleRooms.forEach(visibleRoom -> {
                if (!perPlayerData1.visibleRooms.contains(visibleRoom)) {
                    IsoMetaCell metaCellx = IsoWorld.instance.getMetaGrid().getCellData(visibleRoom.cellX, visibleRoom.cellY);
                    RoomDef roomDefx = metaCellx == null ? null : metaCellx.roomByMetaId.get(visibleRoom.metaId);
                    if (roomDefx != null) {
                        roomDefx.setInvalidateCacheForAllChunks(playerIndex, 32L);
                    }

                    if (this.shouldDarkenIndividualRooms()) {
                        for (int ix = perPlayerData1.fadingRooms.size() - 1; ix >= 0; ix--) {
                            FBORenderCell.FadingRoom fadingRoomx = perPlayerData1.fadingRooms.get(ix);
                            if (fadingRoomx.equals(visibleRoom.cellX, visibleRoom.cellY, visibleRoom.metaId)) {
                                fadingRoomx.release();
                                perPlayerData1.fadingRooms.remove(ix);
                            }
                        }
                    }
                }
            });
            perPlayerData1.visibleRooms.forEach(visibleRoom -> {
                if (!visibleRooms.contains(visibleRoom)) {
                    if (this.shouldDarkenIndividualRooms()) {
                        FBORenderCell.FadingRoom fadingRoomx = FBORenderCell.FadingRoom.alloc().set(visibleRoom.cellX, visibleRoom.cellY, visibleRoom.metaId);
                        fadingRoomx.startTimeMs = System.currentTimeMillis();
                        fadingRoomx.blackness = 0.0F;
                        perPlayerData1.fadingRooms.add(fadingRoomx);
                    }

                    IsoMetaCell metaCellx = IsoWorld.instance.getMetaGrid().getCellData(visibleRoom.cellX, visibleRoom.cellY);
                    RoomDef roomDefx = metaCellx == null ? null : metaCellx.roomByMetaId.get(visibleRoom.metaId);
                    if (roomDefx != null) {
                        roomDefx.setInvalidateCacheForAllChunks(playerIndex, 32L);
                    }
                }
            });
            LightingJNI.VisibleRoom.releaseAll(perPlayerData1.visibleRooms);
            perPlayerData1.visibleRooms.clear();

            for (int i = 0; i < visibleRooms.size(); i++) {
                LightingJNI.VisibleRoom visibleRoom1 = visibleRooms.get(i);
                LightingJNI.VisibleRoom visibleRoom2 = LightingJNI.VisibleRoom.alloc().set(visibleRoom1);
                perPlayerData1.visibleRooms.add(visibleRoom2);
            }

            if (this.shouldDarkenIndividualRooms()) {
                for (int i = perPlayerData1.fadingRooms.size() - 1; i >= 0; i--) {
                    FBORenderCell.FadingRoom fadingRoom = perPlayerData1.fadingRooms.get(i);
                    if (fadingRoom.startTimeMs + blackedOutRoomFadeDurationMs <= System.currentTimeMillis()) {
                        fadingRoom.release();
                        perPlayerData1.fadingRooms.remove(i);
                    } else {
                        float ratio = (float)(System.currentTimeMillis() - fadingRoom.startTimeMs) / (float)blackedOutRoomFadeDurationMs;
                        float fade = (int)PZMath.ceil(ratio * 100.0F) / 10 * 0.1F;
                        fade *= blackedOutRoomFadeBlackness;
                        if (fade != fadingRoom.blackness) {
                            fadingRoom.blackness = fade;
                            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(fadingRoom.cellX, fadingRoom.cellY);
                            RoomDef roomDef = metaCell == null ? null : metaCell.roomByMetaId.get(fadingRoom.metaId);
                            if (roomDef != null) {
                                roomDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean shouldDarkenIndividualRooms() {
        return blackedOutRoomFadeBlackness > 0.0F;
    }

    public boolean isBlackedOutBuildingSquare(IsoGridSquare square) {
        if (!PerformanceSettings.fboRenderChunk) {
            return false;
        } else if (!FBORenderCutaways.getInstance().isAnyBuildingCollapsed()) {
            return false;
        } else if (square == null) {
            return false;
        } else {
            BuildingDef buildingDef = square.getBuilding() == null ? null : square.getBuilding().getDef();
            if (buildingDef == null) {
                return false;
            } else {
                int playerIndex = IsoCamera.frameState.playerIndex;
                if (this.shouldDarkenIndividualRooms()) {
                    IsoRoom room = square.getRoom();
                    if (room == null) {
                        return false;
                    }

                    int cellX = buildingDef.getCellX();
                    int cellY = buildingDef.getCellY();
                    long metaID = room.getRoomDef().metaId;
                    if (!LightingJNI.isRoomVisible(playerIndex, cellX, cellY, metaID)) {
                        return true;
                    }
                }

                FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
                return perPlayerData1.blackedOutBuildings.contains(buildingDef);
            }
        }
    }

    public float getBlackedOutRoomFadeRatio(IsoGridSquare square) {
        if (!this.shouldDarkenIndividualRooms()) {
            return 1.0F;
        } else if (square == null) {
            return blackedOutRoomFadeBlackness;
        } else {
            BuildingDef buildingDef = square.getBuilding() == null ? null : square.getBuilding().getDef();
            if (buildingDef == null) {
                return blackedOutRoomFadeBlackness;
            } else {
                IsoRoom room = square.getRoom();
                if (room == null) {
                    return blackedOutRoomFadeBlackness;
                } else {
                    int cellX = buildingDef.getCellX();
                    int cellY = buildingDef.getCellY();
                    long metaID = room.getRoomDef().metaId;
                    int playerIndex = IsoCamera.frameState.playerIndex;
                    FBORenderCell.PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];

                    for (int i = perPlayerData1.fadingRooms.size() - 1; i >= 0; i--) {
                        FBORenderCell.FadingRoom fadingRoom = perPlayerData1.fadingRooms.get(i);
                        if (fadingRoom.equals(cellX, cellY, metaID)) {
                            return fadingRoom.blackness;
                        }
                    }

                    return blackedOutRoomFadeBlackness;
                }
            }
        }
    }

    public void Reset() {
        for (int i = 0; i < 4; i++) {
            this.perPlayerData[i].reset();
        }
    }

    static final class FadingRoom {
        public int cellX;
        public int cellY;
        public long metaId;
        public long startTimeMs;
        public float blackness;
        private static final ObjectPool<FBORenderCell.FadingRoom> pool = new ObjectPool<>(FBORenderCell.FadingRoom::new);

        FBORenderCell.FadingRoom set(int cellX, int cellY, long metaID) {
            this.cellX = cellX;
            this.cellY = cellY;
            this.metaId = metaID;
            return this;
        }

        public FBORenderCell.FadingRoom set(LightingJNI.VisibleRoom other) {
            return this.set(other.cellX, other.cellY, other.metaId);
        }

        @Override
        public boolean equals(Object rhs) {
            return rhs instanceof LightingJNI.VisibleRoom other ? this.equals(other.cellX, other.cellY, other.metaId) : false;
        }

        boolean equals(int cellX, int cellY, long metaID) {
            return this.cellX == cellX && this.cellY == cellY && this.metaId == metaID;
        }

        public static FBORenderCell.FadingRoom alloc() {
            return pool.alloc();
        }

        public void release() {
            pool.release(this);
        }

        public static void releaseAll(List<FBORenderCell.FadingRoom> objs) {
            pool.releaseAll(objs);
        }
    }

    private static final class PerPlayerData {
        private final int playerIndex;
        private int lastZ = Integer.MAX_VALUE;
        private final ArrayList<IsoChunk> onScreenChunks = new ArrayList<>();
        private final ArrayList<IsoChunk> chunksWithAnimatedAttachments = new ArrayList<>();
        private final ArrayList<IsoChunk> chunksWithFlies = new ArrayList<>();
        private final ArrayList<IsoChunk> chunksWithTranslucentFloor = new ArrayList<>();
        private final ArrayList<IsoChunk> chunksWithTranslucentNonFloor = new ArrayList<>();
        private float playerBoundsX;
        private float playerBoundsY;
        private float playerBoundsW;
        private float playerBoundsH;
        private final ArrayList<IsoGameCharacter.Location> squaresObscuringPlayer = new ArrayList<>();
        private final ArrayList<IsoGameCharacter.Location> fadingInSquares = new ArrayList<>();
        private int lightingUpdateCounter;
        private int occludedGridX1;
        private int occludedGridY1;
        private int occludedGridX2;
        private int occludedGridY2;
        private int[] occludedGrid;
        private boolean occlusionChanged;
        private final ArrayList<BuildingDef> blackedOutBuildings = new ArrayList<>();
        final ArrayList<LightingJNI.VisibleRoom> visibleRooms = new ArrayList<>();
        final ArrayList<FBORenderCell.FadingRoom> fadingRooms = new ArrayList<>();

        private PerPlayerData(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        private void addChunkWith_AnimatedAttachments(IsoChunk chunk) {
            if (!this.chunksWithAnimatedAttachments.contains(chunk)) {
                this.chunksWithAnimatedAttachments.add(chunk);
            }
        }

        private void addChunkWith_Flies(IsoChunk chunk) {
            if (!this.chunksWithFlies.contains(chunk)) {
                this.chunksWithFlies.add(chunk);
            }
        }

        private void addChunkWith_TranslucentFloor(IsoChunk chunk) {
            if (!this.chunksWithTranslucentFloor.contains(chunk)) {
                this.chunksWithTranslucentFloor.add(chunk);
            }
        }

        private void addChunkWith_TranslucentNonFloor(IsoChunk chunk) {
            if (!this.chunksWithTranslucentNonFloor.contains(chunk)) {
                this.chunksWithTranslucentNonFloor.add(chunk);
            }
        }

        private boolean isSquareObscuringPlayer(IsoGridSquare square) {
            for (int i = 0; i < this.squaresObscuringPlayer.size(); i++) {
                IsoGameCharacter.Location location = this.squaresObscuringPlayer.get(i);
                if (location.equals(square.x, square.y, square.z)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isFadingInSquare(IsoGridSquare square) {
            for (int i = 0; i < this.fadingInSquares.size(); i++) {
                IsoGameCharacter.Location location = this.fadingInSquares.get(i);
                if (location.equals(square.x, square.y, square.z)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isObjectObscuringPlayer(IsoGridSquare square, Texture texture, float offsetX, float offsetY) {
            square.cachedScreenX = IsoUtils.XToScreen(square.x, square.y, square.z, 0);
            square.cachedScreenY = IsoUtils.YToScreen(square.x, square.y, square.z, 0);
            float textureX = square.cachedScreenX - offsetX + texture.getOffsetX();
            float textureY = square.cachedScreenY - offsetY + texture.getOffsetY();
            return textureX < this.playerBoundsX + this.playerBoundsW
                && textureX + texture.getWidth() > this.playerBoundsX
                && textureY < this.playerBoundsY + this.playerBoundsH
                && textureY + texture.getHeight() > this.playerBoundsY;
        }

        private void reset() {
            this.blackedOutBuildings.clear();
            LightingJNI.VisibleRoom.releaseAll(this.visibleRooms);
            this.visibleRooms.clear();
            this.chunksWithAnimatedAttachments.clear();
            this.chunksWithFlies.clear();
            this.chunksWithTranslucentFloor.clear();
            this.chunksWithTranslucentNonFloor.clear();
            this.fadingInSquares.clear();
            this.lastZ = Integer.MAX_VALUE;
            this.lightingUpdateCounter = 0;
            this.onScreenChunks.clear();
        }
    }
}
