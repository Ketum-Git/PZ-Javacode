// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2i;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MainThread;
import zombie.MovingObjectUpdateScheduler;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.physics.RagdollControllerDebugRenderer;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbeList;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.IntGrid;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.erosion.utils.Noise2D;
import zombie.gameStates.GameLoadingState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.inventory.InventoryItem;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.BuildingScore;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderSnow;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.popman.NetworkZombieSimulator;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;

/**
 * Loaded area 'reality bubble' around the player(s). Don't confuse this with map cells - the name is a relic from when it did actually represent these. Only one instance should ever exist. Instantiating this class during gameplay will likely immediately crash.
 */
@UsedFromLua
public final class IsoCell {
    public static final int CELL_SIZE_IN_CHUNKS = 32;
    public static final int CELL_SIZE_IN_SQUARES = 256;
    public static int maxHeight = 32;
    public static Shader floorRenderShader;
    public static Shader wallRenderShader;
    public ArrayList<IsoGridSquare> trees = new ArrayList<>();
    public int minHeight;
    static final ArrayList<IsoGridSquare> stchoices = new ArrayList<>();
    public final IsoChunkMap[] chunkMap = new IsoChunkMap[4];
    public final ArrayList<IsoBuilding> buildingList = new ArrayList<>();
    private final ArrayList<IsoWindow> windowList = new ArrayList<>();
    private final ArrayList<IsoMovingObject> objectList = new ArrayList<>();
    private final ArrayList<IsoPushableObject> pushableObjectList = new ArrayList<>();
    private final HashMap<Integer, BuildingScore> buildingScores = new HashMap<>();
    private final ArrayList<IsoRoom> roomList = new ArrayList<>();
    private final ArrayList<IsoObject> staticUpdaterObjectList = new ArrayList<>();
    private final ArrayList<IsoZombie> zombieList = new ArrayList<>();
    private final ArrayList<IsoGameCharacter> remoteSurvivorList = new ArrayList<>();
    private final ArrayList<IsoMovingObject> removeList = new ArrayList<>();
    private final ArrayList<IsoMovingObject> addList = new ArrayList<>();
    private final ArrayList<IsoObject> processIsoObject = new ArrayList<>();
    private final ArrayList<IsoObject> processIsoObjectRemove = new ArrayList<>();
    private final ArrayList<InventoryItem> processItems = new ArrayList<>();
    private final Set<InventoryItem> processItemsRemove = new HashSet<>();
    private final ArrayList<IsoWorldInventoryObject> processWorldItems = new ArrayList<>();
    public final Set<IsoWorldInventoryObject> processWorldItemsRemove = new HashSet<>();
    private final IsoGridSquare[][] gridSquares = GameServer.server
        ? null
        : new IsoGridSquare[4][IsoChunkMap.chunkWidthInTiles * IsoChunkMap.chunkWidthInTiles * 64];
    public static final boolean ENABLE_SQUARE_CACHE = false;
    private int height;
    private int width;
    private int worldX;
    private int worldY;
    public IntGrid dangerScore;
    private boolean safeToAdd = true;
    private final Stack<IsoLightSource> lamppostPositions = new Stack<>();
    public final ArrayList<IsoRoomLight> roomLights = new ArrayList<>();
    private final ArrayList<IsoHeatSource> heatSources = new ArrayList<>();
    public final ArrayList<BaseVehicle> addVehicles = new ArrayList<>();
    public final ArrayList<BaseVehicle> vehicles = new ArrayList<>();
    public static final int ISOANGLEFACTOR = 3;
    public static final int ZOMBIESCANBUDGET = 10;
    public static final float NEARESTZOMBIEDISTSQRMAX = 150.0F;
    public int zombieScanCursor;
    public final IsoZombie[] nearestVisibleZombie = new IsoZombie[4];
    public final float[] nearestVisibleZombieDistSqr = new float[4];
    private static Stack<BuildingScore> buildingscores = new Stack<>();
    public static ArrayList<IsoGridSquare> gridStack;
    public static final int RTF_SolidFloor = 1;
    public static final int RTF_VegetationCorpses = 2;
    public static final int RTF_MinusFloorCharacters = 4;
    public static final int RTF_ShadedFloor = 8;
    public static final int RTF_Shadows = 16;
    public static final ArrayList<IsoGridSquare> ShadowSquares = new ArrayList<>(1000);
    public static final ArrayList<IsoGridSquare> MinusFloorCharacters = new ArrayList<>(1000);
    public static final ArrayList<IsoGridSquare> SolidFloor = new ArrayList<>(5000);
    public static final ArrayList<IsoGridSquare> ShadedFloor = new ArrayList<>(5000);
    public static final ArrayList<IsoGridSquare> VegetationCorpses = new ArrayList<>(5000);
    public static final IsoCell.PerPlayerRender[] perPlayerRender = new IsoCell.PerPlayerRender[4];
    public int stencilX1;
    public int stencilY1;
    public int stencilX2;
    public int stencilY2;
    private Texture stencilTexture;
    private final DiamondMatrixIterator diamondMatrixIterator = new DiamondMatrixIterator(123);
    private final Vector2i diamondMatrixPos = new Vector2i();
    public int deferredCharacterTick;
    private boolean hasSetupSnowGrid;
    private IsoCell.SnowGridTiles snowGridTilesSquare;
    private IsoCell.SnowGridTiles[] snowGridTilesStrip;
    private IsoCell.SnowGridTiles[] snowGridTilesEdge;
    private IsoCell.SnowGridTiles[] snowGridTilesCove;
    private IsoCell.SnowGridTiles snowGridTilesEnclosed;
    private int snowFirstNonSquare = -1;
    private Noise2D snowNoise2d = new Noise2D();
    private IsoCell.SnowGrid snowGridCur;
    private IsoCell.SnowGrid snowGridPrev;
    private int snowFracTarget;
    private long snowFadeTime;
    private final float snowTransitionTime = 5000.0F;
    private final int raport = 0;
    private static final int SNOWSHORE_NONE = 0;
    private static final int SNOWSHORE_N = 1;
    private static final int SNOWSHORE_E = 2;
    private static final int SNOWSHORE_S = 4;
    private static final int SNOWSHORE_W = 8;
    public boolean recalcFloors;
    static int wx;
    static int wy;
    final KahluaTable[] drag = new KahluaTable[4];
    final ArrayList<IsoSurvivor> survivorList = new ArrayList<>();
    private static Texture texWhite;
    private static IsoCell instance;
    private int currentLx;
    private int currentLy;
    private int currentLz;
    public int recalcShading = 30;
    public int lastMinX = -1234567;
    public int lastMinY = -1234567;
    private float rainScroll;
    private final int[] rainX = new int[4];
    private final int[] rainY = new int[4];
    private final Texture[] rainTextures = new Texture[5];
    private final long[] rainFileTime = new long[5];
    private float rainAlphaMax = 0.6F;
    private final float[] rainAlpha = new float[4];
    protected int rainIntensity;
    protected int rainSpeed = 6;
    public int lightUpdateCount = 11;
    public boolean rendering;
    public final boolean[] hideFloors = new boolean[4];
    public final int[] unhideFloorsCounter = new int[4];
    public boolean occludedByOrphanStructureFlag;
    public long playerPeekedRoomId = -1L;
    public final ArrayList<ArrayList<IsoBuilding>> playerOccluderBuildings = new ArrayList<>(4);
    public final IsoBuilding[][] playerOccluderBuildingsArr = new IsoBuilding[4][];
    public final long[] playerWindowPeekingRoomId = new long[4];
    public final boolean[] playerHidesOrphanStructures = new boolean[4];
    public final boolean[] playerCutawaysDirty = new boolean[4];
    final Vector2 tempCutawaySqrVector = new Vector2();
    ArrayList<ArrayList<Long>> tempPrevPlayerCutawayRoomIds = new ArrayList<>(4);
    public ArrayList<ArrayList<Long>> tempPlayerCutawayRoomIds = new ArrayList<>(4);
    public final IsoGridSquare[] lastPlayerSquare = new IsoGridSquare[4];
    public final boolean[] lastPlayerSquareHalf = new boolean[4];
    public final IsoDirections[] lastPlayerDir = new IsoDirections[4];
    public final Vector2[] lastPlayerAngle = new Vector2[4];
    public int hidesOrphanStructuresAbove = maxHeight;
    final Rectangle buildingRectTemp = new Rectangle();
    public final ArrayList<ArrayList<IsoBuilding>> zombieOccluderBuildings = new ArrayList<>(4);
    public final IsoBuilding[][] zombieOccluderBuildingsArr = new IsoBuilding[4][];
    public final IsoGridSquare[] lastZombieSquare = new IsoGridSquare[4];
    public final boolean[] lastZombieSquareHalf = new boolean[4];
    public final ArrayList<ArrayList<IsoBuilding>> otherOccluderBuildings = new ArrayList<>(4);
    public final IsoBuilding[][] otherOccluderBuildingsArr = new IsoBuilding[4][];
    final int mustSeeSquaresRadius = 4;
    final int mustSeeSquaresGridSize = 10;
    public final ArrayList<IsoGridSquare> gridSquaresTempLeft = new ArrayList<>(100);
    public final ArrayList<IsoGridSquare> gridSquaresTempRight = new ArrayList<>(100);
    private IsoWeatherFX weatherFx;
    public int minX;
    public int maxX;
    public int minY;
    public int maxY;
    public int minZ;
    public int maxZ;
    private OnceEvery dangerUpdate = new OnceEvery(0.4F, false);
    private Thread lightInfoUpdate;
    long lastServerItemsUpdate = System.currentTimeMillis();
    private final Stack<IsoRoom> spottedRooms = new Stack<>();
    private IsoZombie fakeZombieForHit;

    /**
     * @return the MaxHeight
     */
    public static int getMaxHeight() {
        return maxHeight;
    }

    public static int getCellSizeInChunks() {
        return 32;
    }

    public static int getCellSizeInSquares() {
        return 256;
    }

    public LotHeader getCurrentLotHeader() {
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        IsoChunk ch = this.getChunkForGridSquare(
            PZMath.fastfloor(isoGameCharacter.getX()), PZMath.fastfloor(isoGameCharacter.getY()), PZMath.fastfloor(isoGameCharacter.getZ())
        );
        return ch.lotheader;
    }

    public IsoChunkMap getChunkMap(int pl) {
        return this.chunkMap[pl];
    }

    public IsoGridSquare getFreeTile(RoomDef def) {
        stchoices.clear();

        for (int n = 0; n < def.rects.size(); n++) {
            RoomDef.RoomRect rect = def.rects.get(n);

            for (int x = rect.x; x < rect.x + rect.w; x++) {
                for (int y = rect.y; y < rect.y + rect.h; y++) {
                    IsoGridSquare g = this.getGridSquare(x, y, def.level);
                    if (g != null) {
                        g.setCachedIsFree(false);
                        g.setCacheIsFree(false);
                        if (g.isFree(false)) {
                            stchoices.add(g);
                        }
                    }
                }
            }
        }

        if (stchoices.isEmpty()) {
            return null;
        } else {
            IsoGridSquare sq = stchoices.get(Rand.Next(stchoices.size()));
            stchoices.clear();
            return sq;
        }
    }

    /**
     * @return the getBuildings
     */
    public static Stack<BuildingScore> getBuildings() {
        return buildingscores;
    }

    public static void setBuildings(Stack<BuildingScore> scores) {
        buildingscores = scores;
    }

    public IsoZombie getNearestVisibleZombie(int playerIndex) {
        return this.nearestVisibleZombie[playerIndex];
    }

    public IsoChunk getChunkForGridSquare(int x, int y, int z) {
        if (GameServer.server) {
            return ServerMap.instance.getChunk(x / 8, y / 8);
        } else {
            int ox = x;
            int oy = y;

            for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                if (!this.chunkMap[n].ignore) {
                    x = ox - this.chunkMap[n].getWorldXMinTiles();
                    y = oy - this.chunkMap[n].getWorldYMinTiles();
                    if (x >= 0 && y >= 0) {
                        x /= 8;
                        y /= 8;
                        IsoChunk c = null;
                        c = this.chunkMap[n].getChunk(x, y);
                        if (c != null) {
                            return c;
                        }
                    }
                }
            }

            return null;
        }
    }

    public IsoChunk getChunk(int wx, int wy) {
        for (int n = 0; n < IsoPlayer.numPlayers; n++) {
            IsoChunkMap chunkMap = this.chunkMap[n];
            if (!chunkMap.ignore) {
                IsoChunk chunk = chunkMap.getChunk(wx - chunkMap.getWorldXMin(), wy - chunkMap.getWorldYMin());
                if (chunk != null) {
                    return chunk;
                }
            }
        }

        return null;
    }

    public IsoCell(int width, int height) {
        IsoWorld.instance.currentCell = this;
        instance = this;
        this.width = width;
        this.height = height;

        for (int n = 0; n < 4; n++) {
            this.chunkMap[n] = new IsoChunkMap(this);
            this.chunkMap[n].playerId = n;
            this.chunkMap[n].ignore = n > 0;
            this.tempPlayerCutawayRoomIds.add(new ArrayList<>());
            this.tempPrevPlayerCutawayRoomIds.add(new ArrayList<>());
            this.playerOccluderBuildings.add(new ArrayList<>(5));
            this.zombieOccluderBuildings.add(new ArrayList<>(5));
            this.otherOccluderBuildings.add(new ArrayList<>(5));
        }

        WorldReuserThread.instance.run();
    }

    public void CalculateVertColoursForTile(IsoGridSquare sqThis, int x, int y, int zz, int playerIndex) {
        IsoGridSquare sqUL = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 0, 0, 1) ? sqThis.getAdjacentSquare(IsoDirections.NW) : null;
        IsoGridSquare sqU = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 1, 0, 1) ? sqThis.getAdjacentSquare(IsoDirections.N) : null;
        IsoGridSquare sqUR = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 2, 0, 1) ? sqThis.getAdjacentSquare(IsoDirections.NE) : null;
        IsoGridSquare sqR = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 2, 1, 1) ? sqThis.getAdjacentSquare(IsoDirections.E) : null;
        IsoGridSquare sqDR = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 2, 2, 1) ? sqThis.getAdjacentSquare(IsoDirections.SE) : null;
        IsoGridSquare sqD = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 1, 2, 1) ? sqThis.getAdjacentSquare(IsoDirections.S) : null;
        IsoGridSquare sqDL = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 0, 2, 1) ? sqThis.getAdjacentSquare(IsoDirections.SW) : null;
        IsoGridSquare sqL = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 0, 1, 1) ? sqThis.getAdjacentSquare(IsoDirections.W) : null;
        this.CalculateColor(sqUL, sqU, sqL, sqThis, 0, playerIndex);
        this.CalculateColor(sqU, sqUR, sqR, sqThis, 1, playerIndex);
        this.CalculateColor(sqDR, sqD, sqR, sqThis, 2, playerIndex);
        this.CalculateColor(sqDL, sqD, sqL, sqThis, 3, playerIndex);
    }

    private Texture getStencilTexture() {
        if (this.stencilTexture == null) {
            this.stencilTexture = Texture.getSharedTexture("media/mask_circledithernew.png");
        }

        return this.stencilTexture;
    }

    public void DrawStencilMask() {
        Texture tex2 = this.getStencilTexture();
        if (tex2 != null) {
            IndieGL.glStencilMask(255);
            IndieGL.glDepthMask(false);
            IndieGL.glClear(1280);
            int px = IsoCamera.getOffscreenWidth(IsoPlayer.getPlayerIndex()) / 2;
            int py = IsoCamera.getOffscreenHeight(IsoPlayer.getPlayerIndex()) / 2;
            px -= tex2.getWidth() / (2 / Core.tileScale);
            py -= tex2.getHeight() / (2 / Core.tileScale);
            IndieGL.enableStencilTest();
            IndieGL.enableAlphaTest();
            IndieGL.glAlphaFunc(516, 0.1F);
            IndieGL.glStencilFunc(519, 128, 255);
            IndieGL.glStencilOp(7680, 7680, 7681);
            IndieGL.glColorMask(false, false, false, false);
            tex2.renderstrip(
                px - (int)IsoCamera.getRightClickOffX(),
                py - (int)IsoCamera.getRightClickOffY(),
                tex2.getWidth() * Core.tileScale,
                tex2.getHeight() * Core.tileScale,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                null
            );
            IndieGL.glColorMask(true, true, true, true);
            IndieGL.glStencilFunc(519, 0, 255);
            IndieGL.glStencilOp(7680, 7680, 7680);
            IndieGL.glStencilMask(127);
            IndieGL.glAlphaFunc(519, 0.0F);
            this.stencilX1 = px - (int)IsoCamera.getRightClickOffX();
            this.stencilY1 = py - (int)IsoCamera.getRightClickOffY();
            this.stencilX2 = this.stencilX1 + tex2.getWidth() * Core.tileScale;
            this.stencilY2 = this.stencilY1 + tex2.getHeight() * Core.tileScale;
        }
    }

    public void RenderTiles(int MaxHeight) {
        try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.isoCellRenderTiles.profile()) {
            this.renderTilesInternal(MaxHeight);
        }
    }

    private void renderTilesInternal(int maxHeight) {
        if (DebugOptions.instance.terrain.renderTiles.enable.getValue()) {
            if (floorRenderShader == null) {
                RenderThread.invokeOnRenderContext(this::initTileShaders);
            }

            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            player.dirtyRecalcGridStackTime = player.dirtyRecalcGridStackTime - GameTime.getInstance().getMultiplier() / 4.0F;
            IsoCell.PerPlayerRender perPlayerRender = this.getPerPlayerRenderAt(playerIndex);
            perPlayerRender.setSize(this.maxX - this.minX + 1, this.maxY - this.minY + 1);
            long currentTimeMillis = System.currentTimeMillis();
            if (this.minX != perPlayerRender.minX
                || this.minY != perPlayerRender.minY
                || this.maxX != perPlayerRender.maxX
                || this.maxY != perPlayerRender.maxY) {
                perPlayerRender.minX = this.minX;
                perPlayerRender.minY = this.minY;
                perPlayerRender.maxX = this.maxX;
                perPlayerRender.maxY = this.maxY;
                player.dirtyRecalcGridStack = true;
                WeatherFxMask.forceMaskUpdate(playerIndex);
            }

            boolean recalculatedGridStack = player.dirtyRecalcGridStack;

            try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.profile()) {
                this.recalculateAnyGridStacks(perPlayerRender, maxHeight, playerIndex, currentTimeMillis);
            }

            this.deferredCharacterTick++;

            try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.renderTiles.flattenAnyFoliage.profile()) {
                this.flattenAnyFoliage(perPlayerRender, playerIndex);
            }

            if (this.SetCutawayRoomsForPlayer() || recalculatedGridStack) {
                IsoGridStack GridStacks = perPlayerRender.gridStacks;

                for (int zza = 0; zza < maxHeight + 1; zza++) {
                    gridStack = GridStacks.squares.get(zza);

                    for (int i = 0; i < gridStack.size(); i++) {
                        IsoGridSquare square = gridStack.get(i);
                        square.setPlayerCutawayFlag(playerIndex, this.IsCutawaySquare(square, currentTimeMillis) ? 3 : 0, currentTimeMillis);
                    }
                }
            }

            try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.renderTiles.performRenderTiles.profile()) {
                this.performRenderTiles(perPlayerRender, maxHeight, playerIndex, currentTimeMillis);
            }

            this.playerCutawaysDirty[playerIndex] = false;
            ShadowSquares.clear();
            MinusFloorCharacters.clear();
            ShadedFloor.clear();
            SolidFloor.clear();
            VegetationCorpses.clear();

            try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.renderTiles.renderDebugPhysics.profile()) {
                this.renderDebugPhysics(playerIndex);
            }

            try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.renderTiles.renderDebugLighting.profile()) {
                this.renderDebugLighting(perPlayerRender, maxHeight);
            }
        }
    }

    public void initTileShaders() {
        if (DebugLog.isEnabled(DebugType.Shader)) {
            DebugLog.Shader.debugln("Loading shader: \"floorTile\"");
        }

        floorRenderShader = new Shader("floorTile");
        if (DebugLog.isEnabled(DebugType.Shader)) {
            DebugLog.Shader.debugln("Loading shader: \"wallTile\"");
        }

        wallRenderShader = new Shader("wallTile");
        IsoGridSquare.CircleStencilShader inst = IsoGridSquare.CircleStencilShader.instance;
        IsoGridSquare.CutawayNoDepthShader.getInstance();
    }

    public IsoCell.PerPlayerRender getPerPlayerRenderAt(int playerIndex) {
        if (perPlayerRender[playerIndex] == null) {
            perPlayerRender[playerIndex] = new IsoCell.PerPlayerRender();
        }

        return perPlayerRender[playerIndex];
    }

    private void recalculateAnyGridStacks(IsoCell.PerPlayerRender perPlayerRender, int MaxHeight, int playerIndex, long currentTimeMillis) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.dirtyRecalcGridStack) {
            player.dirtyRecalcGridStack = false;
            IsoGridStack gridStacks = perPlayerRender.gridStacks;
            boolean[][][] visiOccludedFlags = perPlayerRender.visiOccludedFlags;
            boolean[][] visiCulledFlags = perPlayerRender.visiCulledFlags;
            int lastChunkX = -1;
            int lastChunkY = -1;
            int currentChunkMaxHeight = -1;
            WeatherFxMask.setDiamondIterDone(playerIndex);
            int max = this.chunkMap[playerIndex].maxHeight;
            int min = this.chunkMap[playerIndex].minHeight;
            if (IsoPlayer.getInstance().getZ() < 0.0F) {
                max = Math.min(-1, PZMath.fastfloor(IsoPlayer.getInstance().getZ()));
                min = Math.max(min, PZMath.fastfloor(IsoPlayer.getInstance().getZ()));
            }

            for (int zza = 0; zza < 64; zza++) {
                gridStack = gridStacks.squares.get(zza);
                gridStack.clear();
            }

            for (int zza = max; zza >= min; zza--) {
                gridStack = gridStacks.squares.get(zza + 32);
                gridStack.clear();
                if (zza < this.maxZ) {
                    if (DebugOptions.instance.terrain.renderTiles.newRender.getValue()) {
                        DiamondMatrixIterator iter = this.diamondMatrixIterator.reset(this.maxX - this.minX);
                        IsoGridSquare square = null;
                        Vector2i v = this.diamondMatrixPos;

                        while (iter.next(v)) {
                            if (v.y < this.maxY - this.minY + 1) {
                                square = this.chunkMap[playerIndex].getGridSquare(v.x + this.minX, v.y + this.minY, zza);
                                if (zza == 0) {
                                    visiOccludedFlags[v.x][v.y][0] = false;
                                    visiOccludedFlags[v.x][v.y][1] = false;
                                    visiCulledFlags[v.x][v.y] = false;
                                }

                                if (square == null) {
                                    WeatherFxMask.addMaskLocation(null, v.x + this.minX, v.y + this.minY, zza);
                                } else {
                                    IsoChunk c = square.getChunk();
                                    if (c != null && square.IsOnScreen(true)) {
                                        WeatherFxMask.addMaskLocation(square, v.x + this.minX, v.y + this.minY, zza);
                                        boolean bDissolved = this.IsDissolvedSquare(square, playerIndex);
                                        square.setIsDissolved(playerIndex, bDissolved, currentTimeMillis);
                                        if (!square.getIsDissolved(playerIndex, currentTimeMillis)) {
                                            square.cacheLightInfo();
                                            gridStack.add(square);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (int n = this.minY; n < this.maxY; n++) {
                            int x = this.minX;
                            IsoGridSquare square = this.chunkMap[playerIndex].getGridSquare(x, n, zza);
                            IsoDirections navStepDirIdx = IsoDirections.E;

                            while (x < this.maxX) {
                                if (zza == 0) {
                                    visiOccludedFlags[x - this.minX][n - this.minY][0] = false;
                                    visiOccludedFlags[x - this.minX][n - this.minY][1] = false;
                                    visiCulledFlags[x - this.minX][n - this.minY] = false;
                                }

                                if (square != null && square.getY() != n) {
                                    square = null;
                                }

                                int chunkX = -1;
                                int chunkY = -1;
                                int c = x - (this.chunkMap[playerIndex].worldX - IsoChunkMap.chunkGridWidth / 2) * 8;
                                int bDissolved = n - (this.chunkMap[playerIndex].worldY - IsoChunkMap.chunkGridWidth / 2) * 8;
                                c /= 8;
                                bDissolved /= 8;
                                if (c != lastChunkX || bDissolved != lastChunkY) {
                                    IsoChunk test = this.chunkMap[playerIndex].getChunkForGridSquare(x, n);
                                    if (test != null) {
                                        currentChunkMaxHeight = test.maxLevel;
                                    }
                                }

                                lastChunkX = c;
                                lastChunkY = bDissolved;
                                if (currentChunkMaxHeight < zza) {
                                    x++;
                                } else {
                                    if (square == null) {
                                        square = this.getGridSquare(x, n, zza);
                                        if (square == null) {
                                            square = this.chunkMap[playerIndex].getGridSquare(x, n, zza);
                                            if (square == null) {
                                                x++;
                                                continue;
                                            }
                                        }
                                    }

                                    IsoChunk cx = square.getChunk();
                                    if (cx != null && square.IsOnScreen(true)) {
                                        WeatherFxMask.addMaskLocation(square, square.x, square.y, zza);
                                        boolean bDissolvedx = this.IsDissolvedSquare(square, playerIndex);
                                        square.setIsDissolved(playerIndex, bDissolvedx, currentTimeMillis);
                                        if (!square.getIsDissolved(playerIndex, currentTimeMillis)) {
                                            square.cacheLightInfo();
                                            gridStack.add(square);
                                        }
                                    }

                                    square = square.getAdjacentSquare(navStepDirIdx);
                                    x++;
                                }
                            }
                        }
                    }
                }
            }

            this.CullFullyOccludedSquares(gridStacks, visiOccludedFlags, visiCulledFlags);
        }
    }

    public void flattenAnyFoliage(IsoCell.PerPlayerRender perPlayerRender, int playerIndex) {
        boolean[][] flattenGrassEtc = perPlayerRender.flattenGrassEtc;

        for (int y = this.minY; y <= this.maxY; y++) {
            for (int x = this.minX; x <= this.maxX; x++) {
                flattenGrassEtc[x - this.minX][y - this.minY] = false;
            }
        }

        for (int i = 0; i < this.vehicles.size(); i++) {
            BaseVehicle vehicle = this.vehicles.get(i);
            if (!(vehicle.getAlpha(playerIndex) <= 0.0F)) {
                for (int dy = -2; dy < 5; dy++) {
                    for (int dx = -2; dx < 5; dx++) {
                        int vx = PZMath.fastfloor(vehicle.getX()) + dx;
                        int vy = PZMath.fastfloor(vehicle.getY()) + dy;
                        if (vx >= this.minX && vx <= this.maxX && vy >= this.minY && vy <= this.maxY) {
                            flattenGrassEtc[vx - this.minX][vy - this.minY] = true;
                        }
                    }
                }
            }
        }
    }

    private void performRenderTiles(IsoCell.PerPlayerRender perPlayerRender, int maxHeight, int playerIndex, long currentTimeMillis) {
        IsoGridStack gridStacks = perPlayerRender.gridStacks;
        boolean[][] flattenGrassEtc = perPlayerRender.flattenGrassEtc;
        Shader floorRenderShader;
        Shader wallRenderShader;
        if (Core.debug && !DebugOptions.instance.terrain.renderTiles.useShaders.getValue()) {
            floorRenderShader = null;
            wallRenderShader = null;
        } else {
            floorRenderShader = IsoCell.floorRenderShader;
            wallRenderShader = IsoCell.wallRenderShader;
        }

        for (int zza = -32; zza < maxHeight + 1; zza++) {
            IsoCell.s_performance.renderTiles.PerformRenderTilesLayer performanceLayerProbe = IsoCell.s_performance.renderTiles.performRenderTilesLayers
                .start(zza + 32);
            gridStack = gridStacks.squares.get(zza + 32);
            ShadowSquares.clear();
            SolidFloor.clear();
            ShadedFloor.clear();
            VegetationCorpses.clear();
            MinusFloorCharacters.clear();
            IndieGL.glClear(256);
            if (zza == 0 && DebugOptions.instance.terrain.renderTiles.water.getValue() && DebugOptions.instance.terrain.renderTiles.waterBody.getValue()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderIsoWater.profile()) {
                    IsoWater.getInstance().render(gridStack);
                }
            }

            try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderFloor.profile()) {
                for (int i = 0; i < gridStack.size(); i++) {
                    IsoGridSquare square = gridStack.get(i);
                    if (square.chunk == null || !square.chunk.lightingNeverDone[playerIndex]) {
                        square.flattenGrassEtc = zza == 0 && flattenGrassEtc[square.x - this.minX][square.y - this.minY];
                        int flags = square.renderFloor(floorRenderShader);
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
                            boolean bOnFloor = mov.onFloor;
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

                        if (!square.getDeferedCharacters().isEmpty()) {
                            flags |= 4;
                        }

                        if (square.hasFlies()) {
                            flags |= 4;
                        }

                        if ((flags & 1) != 0) {
                            SolidFloor.add(square);
                        }

                        if ((flags & 8) != 0) {
                            ShadedFloor.add(square);
                        }

                        if ((flags & 2) != 0) {
                            VegetationCorpses.add(square);
                        }

                        if ((flags & 4) != 0) {
                            MinusFloorCharacters.add(square);
                        }

                        if ((flags & 16) != 0) {
                            ShadowSquares.add(square);
                        }
                    }
                }
            }

            try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderPuddles.profile()) {
                IsoPuddles.getInstance().render(SolidFloor, zza);
            }

            if (zza == 0 && DebugOptions.instance.terrain.renderTiles.water.getValue() && DebugOptions.instance.terrain.renderTiles.waterShore.getValue()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderShore.profile()) {
                    IsoWater.getInstance().renderShore(gridStack);
                }
            }

            if (!SolidFloor.isEmpty()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderSnow.profile()) {
                    this.RenderSnow(zza);
                }
            }

            if (!gridStack.isEmpty()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderBlood.profile()) {
                    this.chunkMap[playerIndex].renderBloodForChunks(zza);
                }
            }

            if (!ShadedFloor.isEmpty()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderFloorShading.profile()) {
                    this.RenderFloorShading(zza);
                }
            }

            WorldMarkers.instance.renderGridSquareMarkers(perPlayerRender, zza, playerIndex);
            if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.renderShadows.profile()) {
                    this.renderShadows();
                }
            }

            if (DebugOptions.instance.terrain.renderTiles.lua.getValue()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.luaOnPostFloorLayerDraw.profile()) {
                    LuaEventManager.triggerEvent("OnPostFloorLayerDraw", zza);
                }
            }

            IsoMarkers.instance.renderIsoMarkers(perPlayerRender, zza, playerIndex);
            if (DebugOptions.instance.terrain.renderTiles.vegetationCorpses.getValue()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.vegetationCorpses.profile()) {
                    for (int ix = 0; ix < VegetationCorpses.size(); ix++) {
                        IsoGridSquare square = VegetationCorpses.get(ix);
                        int cutawaySelf = 0;
                        int cutawayN = 0;
                        int cutawayS = 0;
                        int cutawayW = 0;
                        int cutawayE = 0;
                        square.renderMinusFloor(this.maxZ, false, true, 0, 0, 0, 0, 0, wallRenderShader);
                        square.renderCharacters(this.maxZ, true, true);
                    }
                }
            }

            ImprovedFog.startRender(playerIndex, zza);
            if (DebugOptions.instance.terrain.renderTiles.minusFloorCharacters.getValue()) {
                try (AbstractPerformanceProfileProbe ignored = performanceLayerProbe.minusFloorCharacters.profile()) {
                    for (int ix = 0; ix < MinusFloorCharacters.size(); ix++) {
                        IsoGridSquare square = MinusFloorCharacters.get(ix);
                        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
                        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
                        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
                        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        this.currentLy = square.getY() - this.minY;
                        this.currentLz = zza;
                        ImprovedFog.renderRowsBehind(square);
                        boolean hasSE = square.renderMinusFloor(this.maxZ, false, false, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, wallRenderShader);
                        square.renderDeferredCharacters(this.maxZ);
                        square.renderCharacters(this.maxZ, false, true);
                        if (square.hasFlies()) {
                            CorpseFlies.render(square.x, square.y, square.z);
                        }

                        if (hasSE) {
                            square.renderMinusFloor(this.maxZ, true, false, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, wallRenderShader);
                        }
                    }
                }
            }

            ImprovedFog.endRender();
            performanceLayerProbe.end();
        }
    }

    public void renderShadows() {
        boolean corpseShadows = Core.getInstance().getOptionCorpseShadows();

        for (int i = 0; i < ShadowSquares.size(); i++) {
            IsoGridSquare square = ShadowSquares.get(i);

            for (int j = 0; j < square.getMovingObjects().size(); j++) {
                IsoMovingObject mov = square.getMovingObjects().get(j);
                if (mov instanceof IsoGameCharacter chr) {
                    chr.renderShadow(chr.getX(), chr.getY(), chr.getZ());
                } else if (mov instanceof BaseVehicle vehicle) {
                    vehicle.renderShadow();
                }
            }

            if (corpseShadows) {
                for (int jx = 0; jx < square.getStaticMovingObjects().size(); jx++) {
                    IsoMovingObject mov = square.getStaticMovingObjects().get(jx);
                    if (mov instanceof IsoDeadBody deadBody) {
                        deadBody.renderShadow();
                    }
                }
            }
        }
    }

    public void renderDebugPhysics(int playerIndex) {
        if (Core.debug
            && (
                DebugOptions.instance.physicsRender.getValue()
                    || RagdollControllerDebugRenderer.renderDebugPhysics()
                    || DebugOptions.instance.physicsRenderBallisticsTargets.getValue()
                    || DebugOptions.instance.physicsRenderBallisticsControllers.getValue()
            )) {
            TextureDraw.GenericDrawer drawer = WorldSimulation.getDrawer(playerIndex);
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    public void renderDebugLighting(IsoCell.PerPlayerRender perPlayerRender, int MaxHeight) {
        if (Core.debug && DebugOptions.instance.lightingRender.getValue()) {
            IsoGridStack gridStacks = perPlayerRender.gridStacks;
            int tz = 1;

            for (int zza = this.chunkMap[IsoPlayer.getPlayerIndex()].minHeight; zza < MaxHeight + 1; zza++) {
                gridStack = gridStacks.squares.get(zza + 32);

                for (int i = 0; i < gridStack.size(); i++) {
                    IsoGridSquare square = gridStack.get(i);
                    float x1 = IsoUtils.XToScreenExact(square.x + 0.3F, square.y, 0.0F, 0);
                    float y1 = IsoUtils.YToScreenExact(square.x + 0.3F, square.y, 0.0F, 0);
                    float x2 = IsoUtils.XToScreenExact(square.x + 0.6F, square.y, 0.0F, 0);
                    float y2 = IsoUtils.YToScreenExact(square.x + 0.6F, square.y, 0.0F, 0);
                    float x3 = IsoUtils.XToScreenExact(square.x + 1, square.y + 0.3F, 0.0F, 0);
                    float y3 = IsoUtils.YToScreenExact(square.x + 1, square.y + 0.3F, 0.0F, 0);
                    float x4 = IsoUtils.XToScreenExact(square.x + 1, square.y + 0.6F, 0.0F, 0);
                    float y4 = IsoUtils.YToScreenExact(square.x + 1, square.y + 0.6F, 0.0F, 0);
                    float x5 = IsoUtils.XToScreenExact(square.x + 0.6F, square.y + 1, 0.0F, 0);
                    float y5 = IsoUtils.YToScreenExact(square.x + 0.6F, square.y + 1, 0.0F, 0);
                    float x6 = IsoUtils.XToScreenExact(square.x + 0.3F, square.y + 1, 0.0F, 0);
                    float y6 = IsoUtils.YToScreenExact(square.x + 0.3F, square.y + 1, 0.0F, 0);
                    float x7 = IsoUtils.XToScreenExact(square.x, square.y + 0.6F, 0.0F, 0);
                    float y7 = IsoUtils.YToScreenExact(square.x, square.y + 0.6F, 0.0F, 0);
                    float x8 = IsoUtils.XToScreenExact(square.x, square.y + 0.3F, 0.0F, 0);
                    float y8 = IsoUtils.YToScreenExact(square.x, square.y + 0.3F, 0.0F, 0);
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 0, 1)) {
                        LineDrawer.drawLine(x1, y1, x2, y2, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 1, 1)) {
                        LineDrawer.drawLine(x2, y2, x3, y3, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 2, 1)) {
                        LineDrawer.drawLine(x3, y3, x4, y4, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 2, 1)) {
                        LineDrawer.drawLine(x4, y4, x5, y5, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 2, 2, 1)) {
                        LineDrawer.drawLine(x5, y5, x6, y6, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 2, 1, 1)) {
                        LineDrawer.drawLine(x6, y6, x7, y7, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 2, 0, 1)) {
                        LineDrawer.drawLine(x7, y7, x8, y8, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }

                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 0, 1)) {
                        LineDrawer.drawLine(x8, y8, x1, y1, 1.0F, 0.0F, 0.0F, 1.0F, 0);
                    }
                }
            }
        }
    }

    private void CullFullyOccludedSquares(IsoGridStack InGridStacks, boolean[][][] VF, boolean[][] CF) {
        int aboveGroundSquares = 0;

        for (int earlyOutCheckZ = 1; earlyOutCheckZ < maxHeight + 1; earlyOutCheckZ++) {
            aboveGroundSquares += InGridStacks.squares.get(earlyOutCheckZ + 32).size();
        }

        if (aboveGroundSquares >= 500) {
            int drawnSquares = 0;

            for (int zza = this.chunkMap[IsoPlayer.getPlayerIndex()].maxHeight; zza >= this.chunkMap[IsoPlayer.getPlayerIndex()].minHeight; zza--) {
                gridStack = InGridStacks.squares.get(zza + 32);

                for (int i = gridStack.size() - 1; i >= 0; i--) {
                    IsoGridSquare square = gridStack.get(i);
                    int projX = square.getX() - zza * 3 - this.minX;
                    int projY = square.getY() - zza * 3 - this.minY;
                    if (projX < 0 || projX >= CF.length) {
                        gridStack.remove(i);
                    } else if (projY >= 0 && projY < CF[0].length) {
                        if (zza < maxHeight) {
                            boolean bVisible = !CF[projX][projY];
                            if (bVisible) {
                                bVisible = false;
                                if (projX > 2) {
                                    if (projY > 2) {
                                        bVisible = !VF[projX - 3][projY - 3][0]
                                            || !VF[projX - 3][projY - 3][1]
                                            || !VF[projX - 3][projY - 2][0]
                                            || !VF[projX - 2][projY - 3][1]
                                            || !VF[projX - 2][projY - 2][0]
                                            || !VF[projX - 2][projY - 2][1]
                                            || !VF[projX - 2][projY - 1][0]
                                            || !VF[projX - 1][projY - 2][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else if (projY > 1) {
                                        bVisible = !VF[projX - 3][projY - 2][0]
                                            || !VF[projX - 2][projY - 2][0]
                                            || !VF[projX - 2][projY - 2][1]
                                            || !VF[projX - 2][projY - 1][0]
                                            || !VF[projX - 1][projY - 2][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else if (projY > 0) {
                                        bVisible = !VF[projX - 2][projY - 1][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else {
                                        bVisible = !VF[projX - 1][projY][0] || !VF[projX][projY][0] || !VF[projX][projY][1];
                                    }
                                } else if (projX > 1) {
                                    if (projY > 2) {
                                        bVisible = !VF[projX - 2][projY - 3][1]
                                            || !VF[projX - 2][projY - 2][0]
                                            || !VF[projX - 2][projY - 2][1]
                                            || !VF[projX - 2][projY - 1][0]
                                            || !VF[projX - 1][projY - 2][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else if (projY > 1) {
                                        bVisible = !VF[projX - 2][projY - 2][0]
                                            || !VF[projX - 2][projY - 2][1]
                                            || !VF[projX - 2][projY - 1][0]
                                            || !VF[projX - 1][projY - 2][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else if (projY > 0) {
                                        bVisible = !VF[projX - 2][projY - 1][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else {
                                        bVisible = !VF[projX - 1][projY][0] || !VF[projX][projY][0] || !VF[projX][projY][1];
                                    }
                                } else if (projX > 0) {
                                    if (projY > 2) {
                                        bVisible = !VF[projX - 1][projY - 2][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else if (projY > 1) {
                                        bVisible = !VF[projX - 1][projY - 2][0]
                                            || !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else if (projY > 0) {
                                        bVisible = !VF[projX - 1][projY - 1][1]
                                            || !VF[projX - 1][projY - 1][0]
                                            || !VF[projX - 1][projY][0]
                                            || !VF[projX][projY - 1][1]
                                            || !VF[projX][projY][0]
                                            || !VF[projX][projY][1];
                                    } else {
                                        bVisible = !VF[projX - 1][projY][0] || !VF[projX][projY][0] || !VF[projX][projY][1];
                                    }
                                } else if (projY > 2) {
                                    bVisible = !VF[projX][projY - 1][1] || !VF[projX][projY][0] || !VF[projX][projY][1];
                                } else if (projY > 1) {
                                    bVisible = !VF[projX][projY - 1][1] || !VF[projX][projY][0] || !VF[projX][projY][1];
                                } else if (projY > 0) {
                                    bVisible = !VF[projX][projY - 1][1] || !VF[projX][projY][0] || !VF[projX][projY][1];
                                } else {
                                    bVisible = !VF[projX][projY][0] || !VF[projX][projY][1];
                                }
                            }

                            if (!bVisible) {
                                gridStack.remove(i);
                                CF[projX][projY] = true;
                                continue;
                            }
                        }

                        drawnSquares++;
                        boolean cutW = IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 1, 1) && square.getProperties().has(IsoFlagType.cutW);
                        boolean cutN = IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 0, 1) && square.getProperties().has(IsoFlagType.cutN);
                        boolean stenciled = false;
                        if (cutW || cutN) {
                            stenciled = (square.x > IsoCamera.frameState.camCharacterX || square.y > IsoCamera.frameState.camCharacterY)
                                && square.z >= PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
                            if (stenciled) {
                                int sx = (int)(square.cachedScreenX - IsoCamera.frameState.offX);
                                int sy = (int)(square.cachedScreenY - IsoCamera.frameState.offY);
                                if (sx + 32 * Core.tileScale <= this.stencilX1
                                    || sx - 32 * Core.tileScale >= this.stencilX2
                                    || sy + 32 * Core.tileScale <= this.stencilY1
                                    || sy - 96 * Core.tileScale >= this.stencilY2) {
                                    stenciled = false;
                                }
                            }
                        }

                        int BlockedCount = 0;
                        if (cutW && !stenciled) {
                            BlockedCount++;
                            if (projX > 0) {
                                VF[projX - 1][projY][0] = true;
                                if (projY > 0) {
                                    VF[projX - 1][projY - 1][1] = true;
                                }
                            }

                            if (projX > 1 && projY > 0) {
                                VF[projX - 2][projY - 1][0] = true;
                                if (projY > 1) {
                                    VF[projX - 2][projY - 2][1] = true;
                                }
                            }

                            if (projX > 2 && projY > 1) {
                                VF[projX - 3][projY - 2][0] = true;
                                if (projY > 2) {
                                    VF[projX - 3][projY - 3][1] = true;
                                }
                            }
                        }

                        if (cutN && !stenciled) {
                            BlockedCount++;
                            if (projY > 0) {
                                VF[projX][projY - 1][1] = true;
                                if (projX > 0) {
                                    VF[projX - 1][projY - 1][0] = true;
                                }
                            }

                            if (projY > 1 && projX > 0) {
                                VF[projX - 1][projY - 2][1] = true;
                                if (projX > 1) {
                                    VF[projX - 2][projY - 2][0] = true;
                                }
                            }

                            if (projY > 2 && projX > 1) {
                                VF[projX - 2][projY - 3][1] = true;
                                if (projX > 2) {
                                    VF[projX - 3][projY - 3][0] = true;
                                }
                            }
                        }

                        if (IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 1, 0)) {
                            BlockedCount++;
                            VF[projX][projY][0] = true;
                            VF[projX][projY][1] = true;
                        }

                        if (BlockedCount == 3) {
                            CF[projX][projY] = true;
                        }
                    } else {
                        gridStack.remove(i);
                    }
                }
            }
        }
    }

    public void RenderFloorShading(int zza) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingOld.getValue()
            && !DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lighting.getValue()) {
            if (zza < this.maxZ) {
                if (!Core.debug || !DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                    if (texWhite == null) {
                        texWhite = Texture.getWhite();
                    }

                    Texture tex = texWhite;
                    if (tex != null) {
                        int playerIndex = IsoCamera.frameState.playerIndex;
                        int camOffX = (int)IsoCamera.frameState.offX;
                        int camOffY = (int)IsoCamera.frameState.offY;

                        for (int i = 0; i < ShadedFloor.size(); i++) {
                            IsoGridSquare square = ShadedFloor.get(i);
                            if (square.getProperties().has(IsoFlagType.solidfloor)) {
                                float offX = 0.0F;
                                float offY = 0.0F;
                                float offZ = 0.0F;
                                if (square.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                                    offY = -1.0F;
                                    offX = -1.0F;
                                } else if (square.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                                    offY = -2.0F;
                                    offX = -2.0F;
                                }

                                float sx = IsoUtils.XToScreen(square.getX() + offX, square.getY() + offY, zza + 0.0F, 0);
                                float sy = IsoUtils.YToScreen(square.getX() + offX, square.getY() + offY, zza + 0.0F, 0);
                                sx -= camOffX;
                                sy -= camOffY;
                                int col0 = square.getVertLight(0, playerIndex);
                                int col1 = square.getVertLight(1, playerIndex);
                                int col2 = square.getVertLight(2, playerIndex);
                                int col3 = square.getVertLight(3, playerIndex);
                                if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
                                    col0 = -65536;
                                    col1 = -65536;
                                    col2 = -16776961;
                                    col3 = -16776961;
                                }

                                tex.renderdiamond(
                                    sx - 32 * Core.tileScale, sy + 16 * Core.tileScale, 64 * Core.tileScale, 32 * Core.tileScale, col3, col0, col1, col2
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean IsPlayerWindowPeeking(int playerIndex) {
        return this.playerWindowPeekingRoomId[playerIndex] != -1L;
    }

    public boolean CanBuildingSquareOccludePlayer(IsoGridSquare square, int playerIndex) {
        ArrayList<IsoBuilding> oc = this.playerOccluderBuildings.get(playerIndex);

        for (int i = 0; i < oc.size(); i++) {
            IsoBuilding building = oc.get(i);
            int boundsX = building.getDef().getX();
            int boundsY = building.getDef().getY();
            int boundsWidth = building.getDef().getX2() - boundsX;
            int boundsHeight = building.getDef().getY2() - boundsY;
            this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
            if (this.buildingRectTemp.contains(square.getX(), square.getY())) {
                return true;
            }
        }

        return false;
    }

    public long GetEffectivePlayerRoomId() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        long playerRoomID = this.playerWindowPeekingRoomId[playerIndex];
        if (IsoPlayer.players[playerIndex] != null && IsoPlayer.players[playerIndex].isClimbing()) {
            playerRoomID = -1L;
        }

        if (playerRoomID != -1L) {
            return playerRoomID;
        } else {
            IsoGridSquare playerSquare = IsoPlayer.players[playerIndex].current;
            return playerSquare != null ? playerSquare.getRoomID() : -1L;
        }
    }

    public boolean SetCutawayRoomsForPlayer() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        ArrayList<Long> tempList = this.tempPrevPlayerCutawayRoomIds.get(playerIndex);
        this.tempPrevPlayerCutawayRoomIds.set(playerIndex, this.tempPlayerCutawayRoomIds.get(playerIndex));
        this.tempPlayerCutawayRoomIds.set(playerIndex, tempList);
        this.tempPlayerCutawayRoomIds.get(playerIndex).clear();
        IsoGridSquare playerSquare = player.getSquare();
        if (playerSquare == null) {
            return false;
        } else {
            IsoBuilding playerBuilding = playerSquare.getBuilding();
            long playerRoomId = playerSquare.getRoomID();
            if (playerRoomId == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(playerSquare)) {
                playerRoomId = playerSquare.associatedBuilding.getRoofRoomID(playerSquare.z);
            }

            boolean bForceCutawayUpdate = false;
            if (playerRoomId == -1L) {
                if (this.playerWindowPeekingRoomId[playerIndex] != -1L) {
                    this.tempPlayerCutawayRoomIds.get(playerIndex).add(this.playerWindowPeekingRoomId[playerIndex]);
                } else {
                    bForceCutawayUpdate = this.playerCutawaysDirty[playerIndex];
                }
            } else {
                float cutawaySeeRange = 1.5F;
                int minX = (int)(player.getX() - 1.5F);
                int minY = (int)(player.getY() - 1.5F);
                int maxX = (int)(player.getX() + 1.5F);
                int maxY = (int)(player.getY() + 1.5F);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        IsoGridSquare sqr = this.getGridSquare(x, y, playerSquare.getZ());
                        if (sqr != null) {
                            long roomID = sqr.getRoomID();
                            if (roomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(sqr)) {
                                roomID = sqr.associatedBuilding.getRoofRoomID(sqr.z);
                            }

                            if ((sqr.getCanSee(playerIndex) || playerSquare == sqr)
                                && roomID != -1L
                                && !this.tempPlayerCutawayRoomIds.get(playerIndex).contains(roomID)) {
                                this.tempCutawaySqrVector.set(sqr.getX() + 0.5F - player.getX(), sqr.getY() + 0.5F - player.getY());
                                if (playerSquare == sqr || player.getForwardDirection().dot(this.tempCutawaySqrVector) > 0.0F) {
                                    this.tempPlayerCutawayRoomIds.get(playerIndex).add(roomID);
                                }
                            }
                        }
                    }
                }

                Collections.sort(this.tempPlayerCutawayRoomIds.get(playerIndex));
            }

            return bForceCutawayUpdate || !this.tempPlayerCutawayRoomIds.get(playerIndex).equals(this.tempPrevPlayerCutawayRoomIds.get(playerIndex));
        }
    }

    public boolean IsCutawaySquare(IsoGridSquare square, long currentTimeMillis) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.current == null) {
            return false;
        } else if (square == null) {
            return false;
        } else {
            IsoGridSquare playerSquare = player.current;
            if (playerSquare.getZ() != square.getZ()) {
                return false;
            } else {
                if (this.tempPlayerCutawayRoomIds.get(playerIndex).isEmpty()) {
                    IsoGridSquare nSquare = square.getAdjacentSquare(IsoDirections.N);
                    IsoGridSquare wSquare = square.getAdjacentSquare(IsoDirections.W);
                    if (this.IsCollapsibleBuildingSquare(square)) {
                        if (player.getZ() == 0.0F) {
                            return true;
                        }

                        if (square.getBuilding() != null
                            && (playerSquare.getX() < square.getBuilding().def.x || playerSquare.getY() < square.getBuilding().def.y)) {
                            return true;
                        }

                        IsoGridSquare checkSquare = square;

                        for (int i = 0; i < 3; i++) {
                            checkSquare = checkSquare.getAdjacentSquare(IsoDirections.NW);
                            if (checkSquare == null) {
                                break;
                            }

                            if (checkSquare.isCanSee(playerIndex)) {
                                return true;
                            }
                        }
                    }

                    if (nSquare != null && nSquare.getRoomID() == -1L && wSquare != null && wSquare.getRoomID() == -1L) {
                        return this.DoesSquareHaveValidCutaways(playerSquare, square, playerIndex, currentTimeMillis);
                    }
                } else {
                    IsoGridSquare nSquarex = square.getAdjacentSquare(IsoDirections.N);
                    IsoGridSquare eSquare = square.getAdjacentSquare(IsoDirections.E);
                    IsoGridSquare sSquare = square.getAdjacentSquare(IsoDirections.S);
                    IsoGridSquare wSquarex = square.getAdjacentSquare(IsoDirections.W);
                    IsoGridSquare nPlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.N);
                    IsoGridSquare ePlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.E);
                    IsoGridSquare sPlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.S);
                    IsoGridSquare wPlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.W);
                    boolean isSquareOnEdgeOfRoom = false;
                    boolean isSquareNotInPlayerCutawayRooms = false;

                    for (int i = 0; i < 8; i++) {
                        if (square.getSurroundingSquares()[i] != null && square.getSurroundingSquares()[i].getRoomID() != square.getRoomID()) {
                            isSquareOnEdgeOfRoom = true;
                            break;
                        }
                    }

                    if (!this.tempPlayerCutawayRoomIds.get(playerIndex).contains(square.getRoomID())) {
                        isSquareNotInPlayerCutawayRooms = true;
                    }

                    if (isSquareOnEdgeOfRoom || isSquareNotInPlayerCutawayRooms || square.getWall() != null) {
                        IsoGridSquare checkSquare = square;

                        for (int ix = 0; ix < 3; ix++) {
                            checkSquare = checkSquare.getAdjacentSquare(IsoDirections.NW);
                            if (checkSquare == null) {
                                break;
                            }

                            if (checkSquare.getRoomID() != -1L && this.tempPlayerCutawayRoomIds.get(playerIndex).contains(checkSquare.getRoomID())) {
                                if ((isSquareOnEdgeOfRoom || isSquareNotInPlayerCutawayRooms) && checkSquare.getCanSee(playerIndex)) {
                                    return true;
                                }

                                if (square.getWall() != null && checkSquare.isCouldSee(playerIndex)) {
                                    return true;
                                }
                            }
                        }
                    }

                    if (nSquarex != null
                        && wSquarex != null
                        && (
                            nSquarex.getThumpableWallOrHoppable(false) != null
                                || wSquarex.getThumpableWallOrHoppable(true) != null
                                || square.getThumpableWallOrHoppable(true) != null
                                || square.getThumpableWallOrHoppable(false) != null
                        )) {
                        return this.DoesSquareHaveValidCutaways(playerSquare, square, playerIndex, currentTimeMillis);
                    }

                    if (playerSquare.getRoomID() == -1L
                        && (
                            nPlayerSquare != null && nPlayerSquare.getRoomID() != -1L
                                || ePlayerSquare != null && ePlayerSquare.getRoomID() != -1L
                                || sPlayerSquare != null && sPlayerSquare.getRoomID() != -1L
                                || wPlayerSquare != null && wPlayerSquare.getRoomID() != -1L
                        )) {
                        int diffX = playerSquare.x - square.x;
                        int diffY = playerSquare.y - square.y;
                        if (diffX < 0 && diffY < 0) {
                            if (diffX >= -3) {
                                if (diffY >= -3) {
                                    return true;
                                }

                                if (nSquarex != null
                                    && sSquare != null
                                    && square.getWall(false) != null
                                    && nSquarex.getWall(false) != null
                                    && sSquare.getWall(false) != null
                                    && sSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                                    return true;
                                }
                            } else if (eSquare != null && wSquarex != null) {
                                if (square.getWall(true) != null
                                    && wSquarex.getWall(true) != null
                                    && eSquare.getWall(true) != null
                                    && eSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                                    return true;
                                }

                                if (square.getWall(true) != null
                                    && wSquarex.getWall(true) != null
                                    && eSquare.getWall(true) != null
                                    && eSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                                    return true;
                                }
                            }
                        }
                    }
                }

                return false;
            }
        }
    }

    public boolean DoesSquareHaveValidCutaways(IsoGridSquare playerSquare, IsoGridSquare square, int playerIndex, long currentTimeMillis) {
        IsoGridSquare nSquare = square.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare eSquare = square.getAdjacentSquare(IsoDirections.E);
        IsoGridSquare sSquare = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare wSquare = square.getAdjacentSquare(IsoDirections.W);
        int seeSquareRevealDist = 6;
        IsoObject northWall = square.getWall(true);
        IsoObject westWall = square.getWall(false);
        IsoObject wNorthWall = null;
        IsoObject nWestWall = null;
        if (nSquare != null
            && nSquare.getAdjacentSquare(IsoDirections.W) != null
            && nSquare.getAdjacentSquare(IsoDirections.W).getRoomID() == nSquare.getRoomID()) {
            nWestWall = nSquare.getWall(false);
        }

        if (wSquare != null
            && wSquare.getAdjacentSquare(IsoDirections.N) != null
            && wSquare.getAdjacentSquare(IsoDirections.N).getRoomID() == wSquare.getRoomID()) {
            wNorthWall = wSquare.getWall(true);
        }

        if (westWall != null || northWall != null || nWestWall != null || wNorthWall != null) {
            IsoGridSquare checkSquare = square.getAdjacentSquare(IsoDirections.NW);

            for (int i = 0; i < 2 && checkSquare != null && checkSquare.getRoomID() == playerSquare.getRoomID(); i++) {
                IsoGridSquare sCheckSquare = checkSquare.getAdjacentSquare(IsoDirections.S);
                IsoGridSquare eCheckSquare = checkSquare.getAdjacentSquare(IsoDirections.E);
                if (sCheckSquare != null && sCheckSquare.getBuilding() != null || eCheckSquare != null && eCheckSquare.getBuilding() != null) {
                    break;
                }

                if (checkSquare.isCanSee(playerIndex) && checkSquare.isCouldSee(playerIndex) && checkSquare.DistTo(playerSquare) <= 6 - (i + 1)) {
                    return true;
                }

                if (checkSquare.getBuilding() == null) {
                    checkSquare = checkSquare.getAdjacentSquare(IsoDirections.NW);
                }
            }
        }

        int diffX = playerSquare.x - square.x;
        int diffY = playerSquare.y - square.y;
        if ((northWall == null || !northWall.sprite.name.contains("fencing")) && (westWall == null || !westWall.sprite.name.contains("fencing"))) {
            if (square.DistTo(playerSquare) <= 6.0F
                && square.getAdjacentSquare(IsoDirections.NW) != null
                && square.getAdjacentSquare(IsoDirections.NW).getRoomID() == square.getRoomID()
                && (square.getWall(true) == null || square.getWall(true) == northWall)
                && (square.getWall(false) == null || square.getWall(false) == westWall)) {
                if (sSquare != null && nSquare != null && diffY != 0) {
                    if (diffY > 0
                        && westWall != null
                        && sSquare.getWall(false) != null
                        && nSquare.getWall(false) != null
                        && sSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                        return true;
                    }

                    if (diffY < 0 && westWall != null && nSquare.getWall(false) != null && nSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                        return true;
                    }
                }

                if (eSquare != null && wSquare != null && diffX != 0) {
                    if (diffX > 0
                        && northWall != null
                        && eSquare.getWall(true) != null
                        && wSquare.getWall(true) != null
                        && eSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                        return true;
                    }

                    if (diffX < 0 && northWall != null && wSquare.getWall(true) != null && wSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                        return true;
                    }
                }
            }
        } else {
            if (northWall != null && wNorthWall != null && diffY >= -6 && diffY < 0) {
                return true;
            }

            if (westWall != null && nWestWall != null && diffX >= -6 && diffX < 0) {
                return true;
            }
        }

        if (square == playerSquare
            && square.getAdjacentSquare(IsoDirections.NW) != null
            && square.getAdjacentSquare(IsoDirections.NW).getRoomID() == square.getRoomID()) {
            if (northWall != null && nSquare != null && nSquare.getWall(false) == null && nSquare.isCanSee(playerIndex) && nSquare.isCouldSee(playerIndex)) {
                return true;
            }

            if (westWall != null && wSquare != null && wSquare.getWall(true) != null && wSquare.isCanSee(playerIndex) && wSquare.isCouldSee(playerIndex)) {
                return true;
            }
        }

        return nSquare != null
                && wSquare != null
                && diffX != 0
                && diffY != 0
                && nWestWall != null
                && wNorthWall != null
                && nSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0
                && wSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0
            ? true
            : diffX < 0
                && diffX >= -6
                && diffY < 0
                && diffY >= -6
                && (westWall != null && square.getWall(true) == null || northWall != null && square.getWall(false) == null);
    }

    public boolean IsCollapsibleBuildingSquare(IsoGridSquare square) {
        if (square.getProperties().has(IsoFlagType.forceRender)) {
            return false;
        } else {
            for (int playerIter = 0; playerIter < 4; playerIter++) {
                int size = 500;

                for (int i = 0; i < 500 && this.playerOccluderBuildingsArr[playerIter] != null; i++) {
                    IsoBuilding building = this.playerOccluderBuildingsArr[playerIter][i];
                    if (building == null) {
                        break;
                    }

                    BuildingDef def = building.getDef();
                    if (this.collapsibleBuildingSquareAlgorithm(def, square, IsoPlayer.players[playerIter].getSquare())) {
                        return true;
                    }

                    if (square.getY() - def.getY2() == 1 && square.getWall(true) != null) {
                        return true;
                    }

                    if (square.getX() - def.getX2() == 1 && square.getWall(false) != null) {
                        return true;
                    }
                }
            }

            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player.getVehicle() != null) {
                return false;
            } else {
                for (int i = 0; i < 500 && this.zombieOccluderBuildingsArr[playerIndex] != null; i++) {
                    IsoBuilding buildingx = this.zombieOccluderBuildingsArr[playerIndex][i];
                    if (buildingx == null) {
                        break;
                    }

                    BuildingDef defx = buildingx.getDef();
                    if (this.collapsibleBuildingSquareAlgorithm(defx, square, player.getSquare())) {
                        return true;
                    }
                }

                for (int i = 0; i < 500 && this.otherOccluderBuildingsArr[playerIndex] != null; i++) {
                    IsoBuilding buildingxx = this.otherOccluderBuildingsArr[playerIndex][i];
                    if (buildingxx == null) {
                        break;
                    }

                    BuildingDef defx = buildingxx.getDef();
                    if (this.collapsibleBuildingSquareAlgorithm(defx, square, player.getSquare())) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public boolean collapsibleBuildingSquareAlgorithm(BuildingDef def, IsoGridSquare sq, IsoGridSquare pl) {
        int boundsX = def.getX();
        int boundsY = def.getY();
        int boundsWidth = def.getW();
        int boundsHeight = def.getH();
        this.buildingRectTemp.setBounds(boundsX, boundsY, boundsWidth, boundsHeight);
        if (pl.getRoomID() == -1L && this.buildingRectTemp.contains(pl.getX(), pl.getY())) {
            this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
            IsoGridSquare sqN = sq.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare sqW = sq.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare sqNW = sq.getAdjacentSquare(IsoDirections.NW);
            if (sqNW != null && sqN != null && sqW != null) {
                boolean sq_out = sq.getRoomID() == -1L;
                boolean sqN_out = sqN.getRoomID() == -1L;
                boolean sqW_out = sqW.getRoomID() == -1L;
                boolean sqNW_out = sqNW.getRoomID() == -1L;
                boolean plN = pl.getY() < sq.getY();
                boolean plW = pl.getX() < sq.getX();
                return this.buildingRectTemp.contains(sq.getX(), sq.getY())
                    && (
                        pl.getZ() < sq.getZ()
                            || sq_out && (!sqN_out && plN || !sqW_out && plW)
                            || sq_out && sqN_out && sqW_out && !sqNW_out
                            || !sq_out && (sqNW_out || sqN_out == sqW_out || sqN_out && plW || sqW_out && plN)
                    );
            } else {
                return false;
            }
        } else {
            this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
            return this.buildingRectTemp.contains(sq.getX(), sq.getY());
        }
    }

    private boolean IsDissolvedSquare(IsoGridSquare square, int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.current == null) {
            return false;
        } else {
            IsoGridSquare playerSquare = player.current;
            if (playerSquare.getZ() >= square.getZ()) {
                return false;
            } else if (!PerformanceSettings.newRoofHiding) {
                return this.hideFloors[playerIndex] && square.getZ() >= this.maxZ;
            } else {
                if (square.getZ() > this.hidesOrphanStructuresAbove) {
                    IsoBuilding squareBuilding = square.getBuilding();
                    if (squareBuilding == null) {
                        squareBuilding = square.roofHideBuilding;
                    }

                    for (int dropZ = square.getZ() - 1; dropZ >= 0 && squareBuilding == null; dropZ--) {
                        IsoGridSquare testDropSquare = this.getGridSquare(square.x, square.y, dropZ);
                        if (testDropSquare != null) {
                            squareBuilding = testDropSquare.getBuilding();
                            if (squareBuilding == null) {
                                squareBuilding = testDropSquare.roofHideBuilding;
                            }
                        }
                    }

                    if (squareBuilding == null) {
                        if (square.isSolidFloor()) {
                            return true;
                        }

                        IsoGridSquare squareToNorth = square.getAdjacentSquare(IsoDirections.N);
                        if (squareToNorth != null && squareToNorth.getBuilding() == null) {
                            if (squareToNorth.getPlayerBuiltFloor() != null) {
                                return true;
                            }

                            if (squareToNorth.HasStairsBelow()) {
                                return true;
                            }
                        }

                        IsoGridSquare squareToWest = square.getAdjacentSquare(IsoDirections.W);
                        if (squareToWest != null && squareToWest.getBuilding() == null) {
                            if (squareToWest.getPlayerBuiltFloor() != null) {
                                return true;
                            }

                            if (squareToWest.HasStairsBelow()) {
                                return true;
                            }
                        }

                        if (square.has(IsoFlagType.WallSE)) {
                            IsoGridSquare squareToNorthWest = square.getAdjacentSquare(IsoDirections.NW);
                            if (squareToNorthWest != null && squareToNorthWest.getBuilding() == null) {
                                if (squareToNorthWest.getPlayerBuiltFloor() != null) {
                                    return true;
                                }

                                if (squareToNorthWest.HasStairsBelow()) {
                                    return true;
                                }
                            }
                        }
                    }
                }

                return this.IsCollapsibleBuildingSquare(square);
            }
        }
    }

    private int GetBuildingHeightAt(IsoBuilding building, int inX, int inY, int inZ) {
        for (int z = maxHeight; z > inZ; z--) {
            IsoGridSquare square = this.getGridSquare(inX, inY, z);
            if (square != null && square.getBuilding() == building) {
                return z;
            }
        }

        return inZ;
    }

    private void updateSnow(int fracTarget) {
        if (this.snowGridCur == null) {
            this.snowGridCur = new IsoCell.SnowGrid(fracTarget);
            this.snowGridPrev = new IsoCell.SnowGrid(0);
        } else {
            if (fracTarget != this.snowGridCur.frac) {
                this.snowGridPrev.init(this.snowGridCur.frac);
                this.snowGridCur.init(fracTarget);
                this.snowFadeTime = System.currentTimeMillis();
                DebugLog.log("snow from " + this.snowGridPrev.frac + " to " + this.snowGridCur.frac);
                if (PerformanceSettings.fboRenderChunk) {
                    for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player != null) {
                            player.dirtyRecalcGridStack = true;
                        }
                    }
                }
            }
        }
    }

    public void setSnowTarget(int target) {
        if (!SandboxOptions.instance.enableSnowOnGround.getValue()) {
            target = 0;
        }

        this.snowFracTarget = target;
    }

    public int getSnowTarget() {
        return this.snowFracTarget;
    }

    public boolean gridSquareIsSnow(int x, int y, int z) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderSnow.getInstance().gridSquareIsSnow(x, y, z);
        } else {
            IsoGridSquare square = this.getGridSquare(x, y, z);
            if (square != null && this.snowGridCur != null) {
                if (!square.getProperties().has(IsoFlagType.solidfloor)) {
                    return false;
                } else if (!square.getProperties().has(IsoFlagType.water) && (square.getWater() == null || !square.getWater().isValid())) {
                    if (square.getProperties().has(IsoFlagType.exterior) && square.room == null && !square.isInARoom()) {
                        int snowX = square.getX() % this.snowGridCur.w;
                        int snowY = square.getY() % this.snowGridCur.h;
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
        }
    }

    public void RenderSnow(int zza) {
        if (DebugOptions.instance.weather.snow.getValue()) {
            this.updateSnow(this.snowFracTarget);
            IsoCell.SnowGrid snowGridCur = this.snowGridCur;
            if (snowGridCur != null) {
                IsoCell.SnowGrid snowGridPrev = this.snowGridPrev;
                if (snowGridCur.frac > 0 || snowGridPrev.frac > 0) {
                    float alphaCur = 1.0F;
                    float alphaPrev = 0.0F;
                    long ms = System.currentTimeMillis();
                    long fadeDt = ms - this.snowFadeTime;
                    if ((float)fadeDt < 5000.0F) {
                        float fadeAlpha = (float)fadeDt / 5000.0F;
                        alphaCur = fadeAlpha;
                        alphaPrev = 1.0F - fadeAlpha;
                    }

                    if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                        alphaCur = 1.0F;
                        alphaPrev = 0.0F;
                    }

                    Shader floorRenderShader = null;
                    if (DebugOptions.instance.terrain.renderTiles.useShaders.getValue()) {
                        floorRenderShader = IsoCell.floorRenderShader;
                    }

                    FloorShaperAttachedSprites.instance.setShore(false);
                    FloorShaperDiamond.instance.setShore(false);
                    IndieGL.StartShader(floorRenderShader, IsoCamera.frameState.playerIndex);
                    int camOffX = (int)IsoCamera.frameState.offX;
                    int camOffY = (int)IsoCamera.frameState.offY;

                    for (int i = 0; i < SolidFloor.size(); i++) {
                        IsoGridSquare square = SolidFloor.get(i);
                        if (square.room == null && square.getProperties().has(IsoFlagType.exterior) && square.getProperties().has(IsoFlagType.solidfloor)) {
                            int shore;
                            if (square.getProperties().has(IsoFlagType.water) || square.getWater() != null && square.getWater().isValid()) {
                                shore = getShoreInt(square);
                                if (shore == 0) {
                                    continue;
                                }
                            } else {
                                shore = 0;
                            }

                            int snowX = square.getX() % snowGridCur.w;
                            int snowY = square.getY() % snowGridCur.h;
                            float sx = IsoUtils.XToScreen(square.getX(), square.getY(), zza, 0);
                            float sy = IsoUtils.YToScreen(square.getX(), square.getY(), zza, 0);
                            sx -= camOffX;
                            sy -= camOffY;
                            if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                                sx = IsoUtils.XToScreen(square.getX() % 8, square.getY() % 8, zza, 0);
                                sy = IsoUtils.YToScreen(square.getX() % 8, square.getY() % 8, zza, 0);
                                sx += FBORenderChunkManager.instance.getXOffset();
                                sy += FBORenderChunkManager.instance.getYOffset();
                            }

                            float offsetX = 32 * Core.tileScale;
                            float offsetY = 96 * Core.tileScale;
                            sx -= offsetX;
                            sy -= offsetY;
                            int playerIndex = IsoCamera.frameState.playerIndex;
                            int col0 = square.getVertLight(0, playerIndex);
                            int col1 = square.getVertLight(1, playerIndex);
                            int col2 = square.getVertLight(2, playerIndex);
                            int col3 = square.getVertLight(3, playerIndex);
                            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
                                col0 = -65536;
                                col1 = -65536;
                                col2 = -16776961;
                                col3 = -16776961;
                            }

                            FloorShaperAttachedSprites.instance.setVertColors(col0, col1, col2, col3);
                            FloorShaperDiamond.instance.setVertColors(col0, col1, col2, col3);

                            for (int s = 0; s < 2; s++) {
                                if (alphaPrev > alphaCur) {
                                    this.renderSnowTileGeneral(snowGridCur, alphaCur, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                                    this.renderSnowTileGeneral(snowGridPrev, alphaPrev, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                                } else {
                                    this.renderSnowTileGeneral(snowGridPrev, alphaPrev, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                                    this.renderSnowTileGeneral(snowGridCur, alphaCur, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                                }
                            }
                        }
                    }

                    IndieGL.StartShader(null);
                }
            }
        }
    }

    private void renderSnowTileGeneral(IsoCell.SnowGrid snowGrid, float alpha, IsoGridSquare square, int shore, int snowX, int snowY, int sx, int sy, int s) {
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

    private void renderSnowTile(IsoCell.SnowGrid sgrid, int gx, int gy, int s, IsoGridSquare sq, int shore, Texture tex, int sx, int sy, float alpha) {
        if (shore == 0) {
            byte id = sgrid.gridType[gx][gy][s];
            this.renderSnowTileBase(tex, sx, sy, alpha, id < this.snowFirstNonSquare);
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

            IsoCell.SnowGridTiles sTiles = null;
            IsoCell.SnowGridTiles sTiles2 = null;
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

    private static int getShoreInt(IsoGridSquare sq) {
        int shore = 0;
        if (isSnowShore(sq, 0, -1)) {
            shore |= 1;
        }

        if (isSnowShore(sq, 1, 0)) {
            shore |= 2;
        }

        if (isSnowShore(sq, 0, 1)) {
            shore |= 4;
        }

        if (isSnowShore(sq, -1, 0)) {
            shore |= 8;
        }

        return shore;
    }

    private static boolean isSnowShore(IsoGridSquare sq, int ox, int oy) {
        IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare(sq.getX() + ox, sq.getY() + oy, 0);
        return square != null && !square.getProperties().has(IsoFlagType.water);
    }

    public IsoBuilding getClosestBuildingExcept(IsoGameCharacter chr, IsoRoom except) {
        IsoBuilding building = null;
        float lowest = 1000000.0F;

        for (int n = 0; n < this.buildingList.size(); n++) {
            IsoBuilding b = this.buildingList.get(n);

            for (int m = 0; m < b.exits.size(); m++) {
                float dist = chr.DistTo(b.exits.get(m).x, b.exits.get(m).y);
                if (dist < lowest && (except == null || except.building != b)) {
                    building = b;
                    lowest = dist;
                }
            }
        }

        return building;
    }

    public int getDangerScore(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.height ? this.dangerScore.getValue(x, y) : 1000000;
    }

    private void ObjectDeletionAddition() {
        for (int n = 0; n < this.removeList.size(); n++) {
            IsoMovingObject r = this.removeList.get(n);
            if (r instanceof IsoZombie isoZombie) {
                VirtualZombieManager.instance.RemoveZombie(isoZombie);
            }

            if (!(r instanceof IsoPlayer isoPlayer && !isoPlayer.isDead())) {
                MovingObjectUpdateScheduler.instance.removeObject(r);
                this.objectList.remove(r);
                if (r.getCurrentSquare() != null) {
                    r.getCurrentSquare().getMovingObjects().remove(r);
                }

                if (r.getLastSquare() != null) {
                    r.getLastSquare().getMovingObjects().remove(r);
                }
            }
        }

        this.removeList.clear();

        for (int n = 0; n < this.addList.size(); n++) {
            IsoMovingObject rx = this.addList.get(n);
            this.objectList.add(rx);
        }

        this.addList.clear();

        for (int i = 0; i < this.addVehicles.size(); i++) {
            BaseVehicle vehicle = this.addVehicles.get(i);
            if (!this.objectList.contains(vehicle)) {
                this.objectList.add(vehicle);
            }

            if (!this.vehicles.contains(vehicle)) {
                this.vehicles.add(vehicle);
            }
        }

        this.addVehicles.clear();
    }

    private void ProcessItems(Iterator<InventoryItem> it2) {
        int size = this.processItems.size();

        for (int n = 0; n < size; n++) {
            InventoryItem i = this.processItems.get(n);
            i.update();
            if (i.finishupdate()) {
                this.processItemsRemove.add(i);
            }
        }

        size = this.processWorldItems.size();

        for (int nx = 0; nx < size; nx++) {
            IsoWorldInventoryObject i = this.processWorldItems.get(nx);
            i.update();
            if (i.finishupdate()) {
                this.processWorldItemsRemove.add(i);
            }
        }
    }

    private void ProcessIsoObject() {
        this.processIsoObject.removeAll(this.processIsoObjectRemove);
        this.processIsoObjectRemove.clear();
        int size = this.processIsoObject.size();

        for (int n = 0; n < size; n++) {
            IsoObject i = this.processIsoObject.get(n);
            if (i != null) {
                i.update();
                if (size > this.processIsoObject.size()) {
                    n--;
                    size--;
                }
            }
        }
    }

    private void ProcessObjects(Iterator<IsoMovingObject> it) {
        MovingObjectUpdateScheduler.instance.update();

        for (int n = 0; n < this.objectList.size(); n++) {
            if (this.objectList.get(n) instanceof IsoAnimal animal && !animal.isOnHook()) {
                animal.updateVocalProperties();
                animal.updateLoopingSounds();
            }
        }

        try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("Zombie Vocals")) {
            this.updateZombieVocals();
        }
    }

    private void updateZombieVocals() {
        for (int n = 0; n < this.zombieList.size(); n++) {
            IsoZombie zombie = this.zombieList.get(n);
            zombie.updateVocalProperties();
        }
    }

    private void ProcessRemoveItems(Iterator<InventoryItem> it2) {
        this.processItems.removeAll(this.processItemsRemove);
        this.processWorldItems.removeAll(this.processWorldItemsRemove);
        this.processItemsRemove.clear();
        this.processWorldItemsRemove.clear();
    }

    private void ProcessStaticUpdaters() {
        int size = this.staticUpdaterObjectList.size();

        for (int n = 0; n < size; n++) {
            try {
                this.staticUpdaterObjectList.get(n).update();
            } catch (Exception var4) {
                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, var4);
            }

            if (size > this.staticUpdaterObjectList.size()) {
                n--;
                size--;
            }
        }
    }

    public void addToProcessIsoObject(IsoObject object) {
        if (object != null) {
            this.processIsoObjectRemove.remove(object);
            if (!this.processIsoObject.contains(object)) {
                this.processIsoObject.add(object);
            }
        }
    }

    public void addToProcessIsoObjectRemove(IsoObject object) {
        if (object != null) {
            if (this.processIsoObject.contains(object)) {
                if (!this.processIsoObjectRemove.contains(object)) {
                    this.processIsoObjectRemove.add(object);
                }
            }
        }
    }

    public void addToStaticUpdaterObjectList(IsoObject object) {
        if (object != null) {
            if (!this.staticUpdaterObjectList.contains(object)) {
                this.staticUpdaterObjectList.add(object);
            }
        }
    }

    public void addToProcessItems(InventoryItem item) {
        if (item != null && !GameClient.client) {
            this.processItemsRemove.remove(item);
            if (!this.processItems.contains(item)) {
                this.processItems.add(item);
            }
        }
    }

    public void addToProcessItems(ArrayList<InventoryItem> items) {
        if (items != null && !GameClient.client) {
            for (int i = 0; i < items.size(); i++) {
                InventoryItem item = items.get(i);
                if (item != null) {
                    this.processItemsRemove.remove(item);
                    if (!this.processItems.contains(item)) {
                        this.processItems.add(item);
                    }
                }
            }
        }
    }

    public void addToProcessItemsRemove(InventoryItem item) {
        if (item != null) {
            if (!this.processItemsRemove.contains(item)) {
                this.processItemsRemove.add(item);
            }
        }
    }

    public void addToProcessItemsRemove(ArrayList<InventoryItem> items) {
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                InventoryItem item = items.get(i);
                if (item != null && !this.processItemsRemove.contains(item)) {
                    this.processItemsRemove.add(item);
                }
            }
        }
    }

    public void addToProcessWorldItems(IsoWorldInventoryObject worldItem) {
        if (worldItem != null) {
            this.processWorldItemsRemove.remove(worldItem);
            if (!this.processWorldItems.contains(worldItem)) {
                this.processWorldItems.add(worldItem);
            }
        }
    }

    public void addToProcessWorldItemsRemove(IsoWorldInventoryObject worldItem) {
        if (worldItem != null) {
            if (!this.processWorldItemsRemove.contains(worldItem)) {
                this.processWorldItemsRemove.add(worldItem);
            }
        }
    }

    public IsoSurvivor getNetworkPlayer(int RemoteID) {
        int size = this.remoteSurvivorList.size();

        for (int n = 0; n < size; n++) {
            if (this.remoteSurvivorList.get(n).getRemoteID() == RemoteID) {
                return (IsoSurvivor)this.remoteSurvivorList.get(n);
            }
        }

        return null;
    }

    IsoGridSquare ConnectNewSquare(IsoGridSquare newSquare, boolean bDoSurrounds, boolean specialSquare) {
        int x = newSquare.getX();
        int y = newSquare.getY();
        int z = newSquare.getZ();
        this.setCacheGridSquare(x, y, z, newSquare);
        this.DoGridNav(newSquare, IsoGridSquare.cellGetSquare);
        return newSquare;
    }

    public void DoGridNav(IsoGridSquare newSquare, IsoGridSquare.GetSquare getter) {
        int x = newSquare.getX();
        int y = newSquare.getY();
        int z = newSquare.getZ();
        newSquare.setAdjacentSquare(IsoDirections.N, getter.getGridSquare(x, y - 1, z));
        newSquare.setAdjacentSquare(IsoDirections.NW, getter.getGridSquare(x - 1, y - 1, z));
        newSquare.setAdjacentSquare(IsoDirections.W, getter.getGridSquare(x - 1, y, z));
        newSquare.setAdjacentSquare(IsoDirections.SW, getter.getGridSquare(x - 1, y + 1, z));
        newSquare.setAdjacentSquare(IsoDirections.S, getter.getGridSquare(x, y + 1, z));
        newSquare.setAdjacentSquare(IsoDirections.SE, getter.getGridSquare(x + 1, y + 1, z));
        newSquare.setAdjacentSquare(IsoDirections.E, getter.getGridSquare(x + 1, y, z));
        newSquare.setAdjacentSquare(IsoDirections.NE, getter.getGridSquare(x + 1, y - 1, z));
        if (newSquare.getAdjacentSquare(IsoDirections.N) != null) {
            newSquare.getAdjacentSquare(IsoDirections.N).setAdjacentSquare(IsoDirections.S, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.NW) != null) {
            newSquare.getAdjacentSquare(IsoDirections.NW).setAdjacentSquare(IsoDirections.SE, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.W) != null) {
            newSquare.getAdjacentSquare(IsoDirections.W).setAdjacentSquare(IsoDirections.E, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.SW) != null) {
            newSquare.getAdjacentSquare(IsoDirections.SW).setAdjacentSquare(IsoDirections.NE, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.S) != null) {
            newSquare.getAdjacentSquare(IsoDirections.S).setAdjacentSquare(IsoDirections.N, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.SE) != null) {
            newSquare.getAdjacentSquare(IsoDirections.SE).setAdjacentSquare(IsoDirections.NW, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.E) != null) {
            newSquare.getAdjacentSquare(IsoDirections.E).setAdjacentSquare(IsoDirections.W, newSquare);
        }

        if (newSquare.getAdjacentSquare(IsoDirections.NE) != null) {
            newSquare.getAdjacentSquare(IsoDirections.NE).setAdjacentSquare(IsoDirections.SW, newSquare);
        }
    }

    public IsoGridSquare ConnectNewSquare(IsoGridSquare newSquare, boolean bDoSurrounds) {
        for (int n = 0; n < IsoPlayer.numPlayers; n++) {
            if (!this.chunkMap[n].ignore) {
                this.chunkMap[n].setGridSquare(newSquare, newSquare.getX(), newSquare.getY(), newSquare.getZ());
            }
        }

        return this.ConnectNewSquare(newSquare, bDoSurrounds, false);
    }

    public void PlaceLot(String filename, int sx, int sy, int sz, boolean bClearExisting) {
    }

    public void PlaceLot(IsoLot lot, int sx, int sy, int sz, boolean bClearExisting) {
        int maxLevel = Math.min(sz + lot.info.maxLevel - lot.info.minLevel + 1, 32);

        for (int x = sx; x < sx + lot.info.width; x++) {
            for (int y = sy; y < sy + lot.info.height; y++) {
                for (int z = sz; z < maxLevel; z++) {
                    int lx = x - sx;
                    int ly = y - sy;
                    int lz = z - sz;
                    if (x < this.width && y < this.height && x >= 0 && y >= 0 && z >= 0) {
                        int squareXYZ = lx + ly * 8 + lz * 8 * 8;
                        int offsetInData = lot.offsetInData[squareXYZ];
                        if (offsetInData != -1) {
                            int numInts = lot.data.getQuick(offsetInData);
                            if (numInts > 0) {
                                boolean bClearObjects = false;

                                for (int n = 0; n < numInts; n++) {
                                    String tile = lot.info.tilesUsed.get(lot.data.getQuick(offsetInData + 1 + n));
                                    IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                                    if (spr == null) {
                                        Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                                    } else {
                                        IsoGridSquare square = this.getGridSquare(x, y, z);
                                        if (square == null) {
                                            if (IsoGridSquare.loadGridSquareCache != null) {
                                                square = IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, this, null, x, y, z);
                                            } else {
                                                square = IsoGridSquare.getNew(this, null, x, y, z);
                                            }

                                            this.chunkMap[IsoPlayer.getPlayerIndex()].setGridSquare(square, x, y, z);
                                        } else {
                                            if (bClearExisting
                                                && n == 0
                                                && spr.getProperties().has(IsoFlagType.solidfloor)
                                                && (!spr.properties.has(IsoFlagType.hidewalls) || numInts > 1)) {
                                                bClearObjects = true;
                                            }

                                            if (bClearObjects && n == 0) {
                                                square.getObjects().clear();
                                            }
                                        }

                                        CellLoader.DoTileObjectCreation(spr, spr.getType(), square, this, x, y, z, tile);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int PlaceLot(IsoLot lot, int sx, int sy, int sz, IsoChunk ch, int WX, int WY, boolean[] bDoneSquares) {
        WX *= 8;
        WY *= 8;
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int minLevel = Math.max(sz, -32);
        int maxLevel = Math.min(sz + lot.maxLevel - lot.minLevel - 1, 31);
        int nonEmptySquareCount = 0;

        try {
            for (int x = WX + sx; x < WX + sx + 8; x++) {
                for (int y = WY + sy; y < WY + sy + 8; y++) {
                    if (x >= WX && y >= WY && x < WX + 8 && y < WY + 8) {
                        if (bDoneSquares[x - WX + (y - WY) * 8]) {
                            nonEmptySquareCount++;
                        } else {
                            for (int z = minLevel; z <= maxLevel; z++) {
                                int lx = x - WX - sx;
                                int ly = y - WY - sy;
                                int lz = z - lot.info.minLevel;
                                int squareXYZ = lx + ly * 8 + lz * 8 * 8;
                                int offsetInData = lot.offsetInData[squareXYZ];
                                if (offsetInData != -1) {
                                    int numInts = lot.data.getQuick(offsetInData);
                                    if (numInts > 0) {
                                        if (!bDoneSquares[x - WX + (y - WY) * 8]) {
                                            bDoneSquares[x - WX + (y - WY) * 8] = true;
                                            nonEmptySquareCount++;
                                        }

                                        IsoGridSquare square = ch.getGridSquare(x - WX, y - WY, z);
                                        if (square == null) {
                                            if (IsoGridSquare.loadGridSquareCache != null) {
                                                square = IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, this, null, x, y, z);
                                            } else {
                                                square = IsoGridSquare.getNew(this, null, x, y, z);
                                            }

                                            square.setX(x);
                                            square.setY(y);
                                            square.setZ(z);
                                            ch.setSquare(x - WX, y - WY, z, square);
                                        }

                                        for (int xx = -1; xx <= 1; xx++) {
                                            for (int yy = -1; yy <= 1; yy++) {
                                                if ((xx != 0 || yy != 0) && xx + x - WX >= 0 && xx + x - WX < 8 && yy + y - WY >= 0 && yy + y - WY < 8) {
                                                    IsoGridSquare square2 = ch.getGridSquare(x + xx - WX, y + yy - WY, z);
                                                    if (square2 == null) {
                                                        square2 = IsoGridSquare.getNew(this, null, x + xx, y + yy, z);
                                                        ch.setSquare(x + xx - WX, y + yy - WY, z, square2);
                                                    }
                                                }
                                            }
                                        }

                                        RoomDef roomDef = metaGrid.getRoomAt(x, y, z);
                                        long roomID = roomDef != null ? roomDef.id : -1L;
                                        square.setRoomID(roomID);
                                        square.ResetIsoWorldRegion();
                                        roomDef = metaGrid.getEmptyOutsideAt(x, y, z);
                                        if (roomDef != null) {
                                            IsoRoom room = ch.getRoom(roomDef.id);
                                            square.roofHideBuilding = room == null ? null : room.building;
                                        }

                                        boolean bClearObjects = true;

                                        for (int n = 0; n < numInts; n++) {
                                            String tile = lot.info.tilesUsed.get(lot.data.get(offsetInData + 1 + n));
                                            if (!lot.info.fixed2x) {
                                                tile = IsoChunk.Fix2x(tile);
                                            }

                                            IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                                            if (spr == null) {
                                                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                                            } else {
                                                if (n == 0
                                                    && spr.getProperties().has(IsoFlagType.solidfloor)
                                                    && (!spr.properties.has(IsoFlagType.hidewalls) || numInts > 1)) {
                                                    bClearObjects = true;
                                                }

                                                if (bClearObjects && n == 0) {
                                                    square.getObjects().clear();
                                                }

                                                CellLoader.DoTileObjectCreation(spr, spr.getType(), square, this, x, y, z, tile);
                                            }
                                        }

                                        square.FixStackableObjects();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception var30) {
            DebugLog.log("Failed to load chunk, blocking out area");
            ExceptionLogger.logException(var30);

            for (int x = WX + sx; x < WX + sx + 8; x++) {
                for (int yx = WY + sy; yx < WY + sy + 8; yx++) {
                    for (int zx = minLevel; zx <= maxLevel; zx++) {
                        ch.setSquare(x - WX - sx, yx - WY - sy, zx - sz, null);
                        this.setCacheGridSquare(x, yx, zx, null);
                    }
                }
            }
        }

        return nonEmptySquareCount;
    }

    public void setDrag(KahluaTable item, int player) {
        LuaEventManager.triggerEvent("SetDragItem", item, player);
        if (player >= 0 && player < 4) {
            if (this.drag[player] != null && this.drag[player] != item) {
                Object functionObj = this.drag[player].rawget("deactivate");
                if (functionObj instanceof JavaFunction || functionObj instanceof LuaClosure) {
                    LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this.drag[player]);
                }
            }

            this.drag[player] = item;
        }
    }

    public KahluaTable getDrag(int player) {
        return player >= 0 && player < 4 ? this.drag[player] : null;
    }

    public boolean DoBuilding(int player, boolean bRender) {
        boolean var4;
        try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.isoCellDoBuilding.profile()) {
            var4 = this.doBuildingInternal(player, bRender);
        }

        return var4;
    }

    private boolean doBuildingInternal(int player, boolean bRender) {
        if (UIManager.getPickedTile() != null && this.drag[player] != null && JoypadManager.instance.getFromPlayer(player) == null) {
            int buildX = PZMath.fastfloor(UIManager.getPickedTile().x);
            int buildY = PZMath.fastfloor(UIManager.getPickedTile().y);
            int buildZ = PZMath.fastfloor(IsoCamera.getCameraCharacterZ());
            if (!IsoWorld.instance.isValidSquare(buildX, buildY, buildZ)) {
                return false;
            }

            IsoGridSquare square = this.getGridSquare(buildX, buildY, buildZ);
            if (!bRender) {
                if (square == null) {
                    square = this.createNewGridSquare(buildX, buildY, buildZ, true);
                    if (square == null) {
                        return false;
                    }
                }

                square.EnsureSurroundNotNull();
            }

            LuaEventManager.triggerEvent("OnDoTileBuilding2", this.drag[player], bRender, buildX, buildY, buildZ, square);
        }

        if (this.drag[player] != null && JoypadManager.instance.getFromPlayer(player) != null) {
            LuaEventManager.triggerEvent(
                "OnDoTileBuilding3",
                this.drag[player],
                bRender,
                PZMath.fastfloor(IsoPlayer.players[player].getX()),
                PZMath.fastfloor(IsoPlayer.players[player].getY()),
                PZMath.fastfloor(IsoCamera.getCameraCharacterZ())
            );
        }

        if (bRender) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glBlendFuncSeparate(770, 771, 770, 1);
            } else {
                IndieGL.glBlendFunc(770, 771);
            }
        }

        return false;
    }

    public float DistanceFromSupport(int x, int y, int z) {
        return 0.0F;
    }

    /**
     * @return the BuildingList
     * @deprecated
     */
    public ArrayList<IsoBuilding> getBuildingList() {
        return this.buildingList;
    }

    public ArrayList<IsoWindow> getWindowList() {
        return this.windowList;
    }

    public void addToWindowList(IsoWindow window) {
        if (!GameServer.server) {
            if (window != null) {
                if (!this.windowList.contains(window)) {
                    this.windowList.add(window);
                }
            }
        }
    }

    public void removeFromWindowList(IsoWindow window) {
        this.windowList.remove(window);
    }

    /**
     * @return the ObjectList
     */
    public ArrayList<IsoMovingObject> getObjectList() {
        return this.objectList;
    }

    public IsoRoom getRoom(int ID) {
        return this.chunkMap[IsoPlayer.getPlayerIndex()].getRoom(ID);
    }

    /**
     * @return the PushableObjectList
     */
    public ArrayList<IsoPushableObject> getPushableObjectList() {
        return this.pushableObjectList;
    }

    /**
     * @return the BuildingScores
     */
    public HashMap<Integer, BuildingScore> getBuildingScores() {
        return this.buildingScores;
    }

    /**
     * @return the RoomList
     */
    public ArrayList<IsoRoom> getRoomList() {
        return this.roomList;
    }

    /**
     * @return the StaticUpdaterObjectList
     */
    public ArrayList<IsoObject> getStaticUpdaterObjectList() {
        return this.staticUpdaterObjectList;
    }

    /**
     * List of every zombie currently in the world.
     * @return the ZombieList
     */
    public ArrayList<IsoZombie> getZombieList() {
        return this.zombieList;
    }

    /**
     * @return the RemoteSurvivorList
     * @deprecated
     */
    public ArrayList<IsoGameCharacter> getRemoteSurvivorList() {
        return this.remoteSurvivorList;
    }

    /**
     * @return the removeList
     */
    public ArrayList<IsoMovingObject> getRemoveList() {
        return this.removeList;
    }

    /**
     * @return the addList
     */
    public ArrayList<IsoMovingObject> getAddList() {
        return this.addList;
    }

    public void addMovingObject(IsoMovingObject o) {
        this.addList.add(o);
    }

    /**
     * @return the ProcessItems
     */
    public ArrayList<InventoryItem> getProcessItems() {
        return this.processItems;
    }

    public ArrayList<IsoWorldInventoryObject> getProcessWorldItems() {
        return this.processWorldItems;
    }

    public ArrayList<IsoObject> getProcessIsoObjects() {
        return this.processIsoObject;
    }

    public Set<InventoryItem> getProcessItemsRemove() {
        return this.processItemsRemove;
    }

    public ArrayList<BaseVehicle> getVehicles() {
        return this.vehicles;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * 
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * 
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the worldX
     */
    public int getWorldX() {
        return this.worldX;
    }

    /**
     * 
     * @param worldX the worldX to set
     */
    public void setWorldX(int worldX) {
        this.worldX = worldX;
    }

    /**
     * @return the worldY
     */
    public int getWorldY() {
        return this.worldY;
    }

    /**
     * 
     * @param worldY the worldY to set
     */
    public void setWorldY(int worldY) {
        this.worldY = worldY;
    }

    /**
     * @return the safeToAdd
     */
    public boolean isSafeToAdd() {
        return this.safeToAdd;
    }

    /**
     * 
     * @param safeToAdd the safeToAdd to set
     */
    public void setSafeToAdd(boolean safeToAdd) {
        this.safeToAdd = safeToAdd;
    }

    /**
     * @return the LamppostPositions
     */
    public Stack<IsoLightSource> getLamppostPositions() {
        return this.lamppostPositions;
    }

    public IsoLightSource getLightSourceAt(int x, int y, int z) {
        for (int i = 0; i < this.lamppostPositions.size(); i++) {
            IsoLightSource light = this.lamppostPositions.get(i);
            if (light.getX() == x && light.getY() == y && light.getZ() == z) {
                return light;
            }
        }

        return null;
    }

    public void addLamppost(IsoLightSource light) {
        if (light != null && !this.lamppostPositions.contains(light)) {
            this.lamppostPositions.add(light);
            IsoGridSquare.recalcLightTime = -1.0F;
            if (PerformanceSettings.fboRenderChunk) {
                Core.dirtyGlobalLightsCount++;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
        }
    }

    public IsoLightSource addLamppost(int x, int y, int z, float r, float g, float b, int rad) {
        IsoLightSource light = new IsoLightSource(x, y, z, r, g, b, rad);
        this.lamppostPositions.add(light);
        IsoGridSquare.recalcLightTime = -1.0F;
        if (PerformanceSettings.fboRenderChunk) {
            Core.dirtyGlobalLightsCount++;
        }

        GameTime.instance.lightSourceUpdate = 100.0F;
        return light;
    }

    public void removeLamppost(int x, int y, int z) {
        for (int i = 0; i < this.lamppostPositions.size(); i++) {
            IsoLightSource light = this.lamppostPositions.get(i);
            if (light.getX() == x && light.getY() == y && light.getZ() == z) {
                light.clearInfluence();
                this.lamppostPositions.remove(light);
                IsoGridSquare.recalcLightTime = -1.0F;
                if (PerformanceSettings.fboRenderChunk) {
                    Core.dirtyGlobalLightsCount++;
                }

                GameTime.instance.lightSourceUpdate = 100.0F;
                return;
            }
        }
    }

    public void removeLamppost(IsoLightSource light) {
        light.life = 0;
        IsoGridSquare.recalcLightTime = -1.0F;
        if (PerformanceSettings.fboRenderChunk) {
            Core.dirtyGlobalLightsCount++;
        }

        GameTime.instance.lightSourceUpdate = 100.0F;
    }

    /**
     * @return the currentLX
     */
    public int getCurrentLightX() {
        return this.currentLx;
    }

    /**
     * 
     * @param currentLX the currentLX to set
     */
    public void setCurrentLightX(int currentLX) {
        this.currentLx = currentLX;
    }

    /**
     * @return the currentLY
     */
    public int getCurrentLightY() {
        return this.currentLy;
    }

    /**
     * 
     * @param currentLY the currentLY to set
     */
    public void setCurrentLightY(int currentLY) {
        this.currentLy = currentLY;
    }

    /**
     * @return the currentLZ
     */
    public int getCurrentLightZ() {
        return this.currentLz;
    }

    /**
     * 
     * @param currentLZ the currentLZ to set
     */
    public void setCurrentLightZ(int currentLZ) {
        this.currentLz = currentLZ;
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return this.minX;
    }

    /**
     * 
     * @param minX the minX to set
     */
    public void setMinX(int minX) {
        this.minX = minX;
    }

    /**
     * @return the maxX
     */
    public int getMaxX() {
        return this.maxX;
    }

    /**
     * 
     * @param maxX the maxX to set
     */
    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    /**
     * @return the minY
     */
    public int getMinY() {
        return this.minY;
    }

    /**
     * 
     * @param minY the minY to set
     */
    public void setMinY(int minY) {
        this.minY = minY;
    }

    /**
     * @return the maxY
     */
    public int getMaxY() {
        return this.maxY;
    }

    /**
     * 
     * @param maxY the maxY to set
     */
    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return this.minZ;
    }

    /**
     * 
     * @param minZ the minZ to set
     */
    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    /**
     * @return the maxZ
     */
    public int getMaxZ() {
        return this.maxZ;
    }

    /**
     * 
     * @param maxZ the maxZ to set
     */
    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    /**
     * @return the dangerUpdate
     */
    public OnceEvery getDangerUpdate() {
        return this.dangerUpdate;
    }

    /**
     * 
     * @param dangerUpdate the dangerUpdate to set
     */
    public void setDangerUpdate(OnceEvery dangerUpdate) {
        this.dangerUpdate = dangerUpdate;
    }

    /**
     * @return the LightInfoUpdate
     */
    public Thread getLightInfoUpdate() {
        return this.lightInfoUpdate;
    }

    /**
     * 
     * @param LightInfoUpdate the LightInfoUpdate to set
     */
    public void setLightInfoUpdate(Thread LightInfoUpdate) {
        this.lightInfoUpdate = LightInfoUpdate;
    }

    public ArrayList<IsoSurvivor> getSurvivorList() {
        return this.survivorList;
    }

    public static int getRComponent(int col) {
        return col & 0xFF;
    }

    public static int getGComponent(int col) {
        return (col & 0xFF00) >> 8;
    }

    public static int getBComponent(int col) {
        return (col & 0xFF0000) >> 16;
    }

    public static int toIntColor(float r, float g, float b, float a) {
        return (int)(r * 255.0F) << 0 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F) << 16 | (int)(a * 255.0F) << 24;
    }

    public IsoGridSquare getRandomOutdoorTile() {
        IsoGridSquare sq = null;

        do {
            sq = this.getGridSquare(
                this.chunkMap[IsoPlayer.getPlayerIndex()].getWorldXMin() * 8 + Rand.Next(this.width),
                this.chunkMap[IsoPlayer.getPlayerIndex()].getWorldYMin() * 8 + Rand.Next(this.height),
                0
            );
            if (sq != null) {
                sq.setCachedIsFree(false);
            }
        } while (sq == null || !sq.isFree(false) || sq.getRoom() != null);

        return sq;
    }

    private static void InsertAt(int a, BuildingScore score, BuildingScore[] array) {
        for (int n = array.length - 1; n > a; n--) {
            array[n] = array[n - 1];
        }

        array[a] = score;
    }

    static void Place(BuildingScore score, BuildingScore[] array, IsoCell.BuildingSearchCriteria criteria) {
        for (int a = 0; a < array.length; a++) {
            if (array[a] != null) {
                boolean bHigher = false;
                if (array[a] == null) {
                    bHigher = true;
                } else {
                    switch (criteria) {
                        case Food:
                            if (array[a].food < score.food) {
                                bHigher = true;
                            }
                            break;
                        case Defense:
                            if (array[a].defense < score.defense) {
                                bHigher = true;
                            }
                            break;
                        case Wood:
                            if (array[a].wood < score.wood) {
                                bHigher = true;
                            }
                            break;
                        case Weapons:
                            if (array[a].weapons < score.weapons) {
                                bHigher = true;
                            }
                            break;
                        case General:
                            if (array[a].food + array[a].defense + array[a].size + array[a].weapons < score.food + score.defense + score.size + score.weapons) {
                                bHigher = true;
                            }
                    }
                }

                if (bHigher) {
                    InsertAt(a, score, array);
                    return;
                }
            }
        }
    }

    public Stack<BuildingScore> getBestBuildings(IsoCell.BuildingSearchCriteria criteria, int count) {
        BuildingScore[] b = new BuildingScore[count];
        if (this.buildingScores.isEmpty()) {
            int size = this.buildingList.size();

            for (int n = 0; n < size; n++) {
                this.buildingList.get(n).update();
            }
        }

        int size = this.buildingScores.size();

        for (int n = 0; n < size; n++) {
            BuildingScore score = this.buildingScores.get(n);
            Place(score, b, criteria);
        }

        buildingscores.clear();
        buildingscores.addAll(Arrays.asList(b));
        return buildingscores;
    }

    public boolean blocked(Mover mover, int x, int y, int z, int lx, int ly, int lz) {
        IsoGridSquare spfrom = this.getGridSquare(lx, ly, lz);
        if (spfrom == null) {
            return true;
        } else {
            if (mover instanceof IsoMovingObject isoMovingObject) {
                if (spfrom.testPathFindAdjacent(isoMovingObject, x - lx, y - ly, z - lz)) {
                    return true;
                }
            } else if (spfrom.testPathFindAdjacent(null, x - lx, y - ly, z - lz)) {
                return true;
            }

            return false;
        }
    }

    public void Dispose() {
        for (int n = 0; n < this.objectList.size(); n++) {
            IsoMovingObject o = this.objectList.get(n);
            if (o instanceof IsoZombie isoZombie) {
                o.setCurrent(null);
                o.setLast(null);
                VirtualZombieManager.instance.addToReusable(isoZombie);
            }
        }

        for (int nx = 0; nx < this.roomList.size(); nx++) {
            this.roomList.get(nx).tileList.clear();
            this.roomList.get(nx).exits.clear();
            this.roomList.get(nx).waterSources.clear();
            this.roomList.get(nx).lightSwitches.clear();
            this.roomList.get(nx).beds.clear();
        }

        for (int nx = 0; nx < this.buildingList.size(); nx++) {
            this.buildingList.get(nx).exits.clear();
            this.buildingList.get(nx).rooms.clear();
            this.buildingList.get(nx).container.clear();
            this.buildingList.get(nx).windows.clear();
        }

        LuaEventManager.clear();
        LuaHookManager.clear();
        this.lamppostPositions.clear();
        this.processItems.clear();
        this.processItemsRemove.clear();
        this.processWorldItems.clear();
        this.processWorldItemsRemove.clear();
        this.buildingScores.clear();
        this.buildingList.clear();
        this.windowList.clear();
        this.pushableObjectList.clear();
        this.roomList.clear();
        this.survivorList.clear();
        this.objectList.clear();
        this.zombieList.clear();
        WorldReuserThread.instance.stop();

        for (int i = 0; i < this.chunkMap.length; i++) {
            this.chunkMap[i].Dispose();
            this.chunkMap[i] = null;
        }

        if (!GameServer.server) {
            for (int i = 0; i < this.gridSquares.length; i++) {
                if (this.gridSquares[i] != null) {
                    Arrays.fill(this.gridSquares[i], null);
                    this.gridSquares[i] = null;
                }
            }
        }

        if (this.weatherFx != null) {
            this.weatherFx.Reset();
            this.weatherFx = null;
        }
    }

    @LuaMethod(name = "getGridSquare")
    public IsoGridSquare getGridSquare(double x, double y, double z) {
        int x1 = PZMath.fastfloor(x);
        int y1 = PZMath.fastfloor(y);
        int z1 = PZMath.fastfloor(z);
        return GameServer.server ? ServerMap.instance.getGridSquare(x1, y1, z1) : this.getGridSquare(x1, y1, z1);
    }

    @LuaMethod(name = "getOrCreateGridSquare")
    public IsoGridSquare getOrCreateGridSquare(double x, double y, double z) {
        int x1 = PZMath.fastfloor(x);
        int y1 = PZMath.fastfloor(y);
        int z1 = PZMath.fastfloor(z);
        if (GameServer.server) {
            IsoGridSquare sq = ServerMap.instance.getGridSquare(x1, y1, z1);
            if (sq == null) {
                sq = IsoGridSquare.getNew(this, null, x1, y1, z1);
                ServerMap.instance.setGridSquare(x1, y1, z1, sq);
                this.ConnectNewSquare(sq, true);
            }

            return sq;
        } else {
            IsoGridSquare sq = this.getGridSquare(x1, y1, z1);
            if (sq == null) {
                sq = IsoGridSquare.getNew(this, null, x1, y1, z1);
                this.ConnectNewSquare(sq, true);
            }

            return sq;
        }
    }

    public void setCacheGridSquare(int x, int y, int z, IsoGridSquare square) {
        assert square == null || x == square.getX() && y == square.getY() && z == square.getZ();
    }

    public void setCacheChunk(IsoChunk chunk) {
        if (!GameServer.server) {
            ;
        }
    }

    public void setCacheChunk(IsoChunk chunk, int playerIndex) {
        if (!GameServer.server) {
            ;
        }
    }

    public void clearCacheGridSquare(int playerIndex) {
    }

    public void setCacheGridSquareLocal(int x, int y, int z, IsoGridSquare square, int playerIndex) {
    }

    public IsoGridSquare getGridSquare(Double x, Double y, Double z) {
        return this.getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
    }

    public IsoGridSquare getGridSquare(int x, int y, int z) {
        if (GameServer.server) {
            return ServerMap.instance.getGridSquare(x, y, z);
        } else {
            int stride = IsoChunkMap.chunkWidthInTiles;

            for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                IsoChunkMap chunkMap = this.chunkMap[n];
                if (!chunkMap.ignore) {
                    if (z == 0) {
                        boolean chunkMapSquareX = false;
                    }

                    int chunkMapSquareX = x - chunkMap.getWorldXMinTiles();
                    int chunkMapSquareY = y - chunkMap.getWorldYMinTiles();
                    int zz = z + 32;
                    if (z <= 31 && z >= -32 && chunkMapSquareX >= 0 && chunkMapSquareX < stride && chunkMapSquareY >= 0 && chunkMapSquareY < stride) {
                        IsoGridSquare sq = chunkMap.getGridSquareDirect(chunkMapSquareX, chunkMapSquareY, z);
                        if (sq != null) {
                            return sq;
                        }
                    }
                }
            }

            return null;
        }
    }

    public void EnsureSurroundNotNull(int xx, int yy, int zz) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                this.createNewGridSquare(xx + x, yy + y, zz, false);
            }
        }
    }

    public void DeleteAllMovingObjects() {
        this.objectList.clear();
    }

    @LuaMethod(name = "getMaxFloors")
    public int getMaxFloors() {
        return 32;
    }

    public KahluaTable getLuaObjectList() {
        KahluaTable table = LuaManager.platform.newTable();
        LuaManager.env.rawset("Objects", table);

        for (int n = 0; n < this.objectList.size(); n++) {
            table.rawset(n + 1, this.objectList.get(n));
        }

        return table;
    }

    public int getHeightInTiles() {
        return this.chunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
    }

    public int getWidthInTiles() {
        return this.chunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
    }

    public boolean isNull(int x, int y, int z) {
        IsoGridSquare sq = this.getGridSquare(x, y, z);
        return sq == null || !sq.isFree(false);
    }

    public void Remove(IsoMovingObject obj) {
        if (!(obj instanceof IsoPlayer isoPlayer && !isoPlayer.isDead())) {
            this.removeList.add(obj);
        }
    }

    boolean isBlocked(IsoGridSquare from, IsoGridSquare to) {
        return from.room != to.room;
    }

    private int CalculateColor(IsoGridSquare sqUL, IsoGridSquare sqU, IsoGridSquare sqL, IsoGridSquare sqThis, int col, int playerIndex) {
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;
        float a = 1.0F;
        if (sqThis == null) {
            return 0;
        } else {
            float div = 0.0F;
            boolean bTest = true;
            if (sqUL != null && sqThis.room == sqUL.room && sqUL.getChunk() != null) {
                div++;
                ColorInfo inf = sqUL.lighting[playerIndex].lightInfo();
                r += inf.r;
                g += inf.g;
                b += inf.b;
            }

            if (sqU != null && sqThis.room == sqU.room && sqU.getChunk() != null) {
                div++;
                ColorInfo inf = sqU.lighting[playerIndex].lightInfo();
                r += inf.r;
                g += inf.g;
                b += inf.b;
            }

            if (sqL != null && sqThis.room == sqL.room && sqL.getChunk() != null) {
                div++;
                ColorInfo inf = sqL.lighting[playerIndex].lightInfo();
                r += inf.r;
                g += inf.g;
                b += inf.b;
            }

            if (sqThis != null) {
                div++;
                ColorInfo inf = sqThis.lighting[playerIndex].lightInfo();
                r += inf.r;
                g += inf.g;
                b += inf.b;
            }

            if (div != 0.0F) {
                r /= div;
                g /= div;
                b /= div;
            }

            if (r > 1.0F) {
                r = 1.0F;
            }

            if (g > 1.0F) {
                g = 1.0F;
            }

            if (b > 1.0F) {
                b = 1.0F;
            }

            if (r < 0.0F) {
                r = 0.0F;
            }

            if (g < 0.0F) {
                g = 0.0F;
            }

            if (b < 0.0F) {
                b = 0.0F;
            }

            if (sqThis != null) {
                sqThis.setVertLight(col, (int)(r * 255.0F) << 0 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F) << 16 | 0xFF000000, playerIndex);
                sqThis.setVertLight(col + 4, (int)(r * 255.0F) << 0 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F) << 16 | 0xFF000000, playerIndex);
            }

            return col;
        }
    }

    public static IsoCell getInstance() {
        return instance;
    }

    public void render() {
        if (Core.debug && IsoCamera.frameState.playerIndex == 0 && GameKeyboard.isKeyPressed(199)) {
            PerformanceSettings.fboRenderChunk = !PerformanceSettings.fboRenderChunk;
        }

        try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.isoCellRender.profile()) {
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderCell.instance.cell = this;
                FBORenderCutaways.getInstance().cell = this;
                FBORenderCell.instance.renderInternal();
            } else {
                this.renderInternal();
            }
        }
    }

    private void renderInternal() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.dirtyRecalcGridStackTime > 0.0F) {
            player.dirtyRecalcGridStack = true;
        } else {
            player.dirtyRecalcGridStack = false;
        }

        if (!PerformanceSettings.newRoofHiding) {
            if (this.hideFloors[playerIndex] && this.unhideFloorsCounter[playerIndex] > 0) {
                this.unhideFloorsCounter[playerIndex]--;
            }

            if (this.unhideFloorsCounter[playerIndex] <= 0) {
                this.hideFloors[playerIndex] = false;
                this.unhideFloorsCounter[playerIndex] = 60;
            }
        }

        int MaxHeight = 32;
        if (MaxHeight < 32) {
            MaxHeight++;
        }

        this.recalcShading--;
        int x1 = 0;
        int y1 = 0;
        int x2 = 0 + IsoCamera.getOffscreenWidth(playerIndex);
        int y2 = 0 + IsoCamera.getOffscreenHeight(playerIndex);
        float topLeftX = IsoUtils.XToIso(0.0F, 0.0F, 0.0F);
        float topRightY = IsoUtils.YToIso(x2, 0.0F, 0.0F);
        float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0F);
        float bottomLeftY = IsoUtils.YToIso(0.0F, y2, 6.0F);
        this.minY = (int)topRightY;
        this.maxY = (int)bottomLeftY;
        this.minX = (int)topLeftX;
        this.maxX = (int)bottomRightX;
        this.minX -= 2;
        this.minY -= 2;
        this.maxZ = maxHeight;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter == null) {
            this.maxZ = 1;
        }

        int lightCountMax = 0;
        int var28 = 4;
        if (GameTime.instance.fpsMultiplier > 1.5F) {
            var28 = 6;
        }

        if (this.minX != this.lastMinX || this.minY != this.lastMinY) {
            this.lightUpdateCount = 10;
        }

        if (!PerformanceSettings.newRoofHiding) {
            IsoGridSquare currentSq = isoGameCharacter == null ? null : isoGameCharacter.getCurrentSquare();
            if (currentSq != null) {
                IsoGridSquare sq = this.getGridSquare(
                    (double)Math.round(isoGameCharacter.getX()), (double)Math.round(isoGameCharacter.getY()), (double)isoGameCharacter.getZ()
                );
                if (sq != null && this.IsBehindStuff(sq)) {
                    this.hideFloors[playerIndex] = true;
                }

                if (!this.hideFloors[playerIndex] && currentSq.getProperties().has(IsoFlagType.hidewalls)
                    || !currentSq.getProperties().has(IsoFlagType.exterior)) {
                    this.hideFloors[playerIndex] = true;
                }
            }

            if (this.hideFloors[playerIndex]) {
                this.maxZ = isoGameCharacter.getZi() + 1;
            }
        }

        this.DrawStencilMask();
        GameProfiler profiler = GameProfiler.getInstance();
        if (PerformanceSettings.newRoofHiding && player.dirtyRecalcGridStack) {
            this.hidesOrphanStructuresAbove = MaxHeight;
            this.otherOccluderBuildings.get(playerIndex).clear();
            if (this.otherOccluderBuildingsArr[playerIndex] != null) {
                this.otherOccluderBuildingsArr[playerIndex][0] = null;
            } else {
                this.otherOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
            }

            if (isoGameCharacter != null && isoGameCharacter.getCurrentSquare() != null) {
                try (GameProfiler.ProfileArea ignored = profiler.profile("updateZombies")) {
                    this.updateZombiesForRender(isoGameCharacter, playerIndex);
                }
            } else {
                for (int playerIter = 0; playerIter < 4; playerIter++) {
                    this.playerOccluderBuildings.get(playerIter).clear();
                    if (this.playerOccluderBuildingsArr[playerIter] != null) {
                        this.playerOccluderBuildingsArr[playerIter][0] = null;
                    } else {
                        this.playerOccluderBuildingsArr[playerIter] = new IsoBuilding[500];
                    }

                    this.lastPlayerSquare[playerIter] = null;
                    this.playerCutawaysDirty[playerIter] = true;
                }

                this.playerWindowPeekingRoomId[playerIndex] = -1L;
                this.zombieOccluderBuildings.get(playerIndex).clear();
                if (this.zombieOccluderBuildingsArr[playerIndex] != null) {
                    this.zombieOccluderBuildingsArr[playerIndex][0] = null;
                } else {
                    this.zombieOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                }

                this.lastZombieSquare[playerIndex] = null;
            }
        }

        if (!PerformanceSettings.newRoofHiding) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                this.playerWindowPeekingRoomId[i] = -1L;
                IsoPlayer player2 = IsoPlayer.players[i];
                if (player2 != null) {
                    IsoBuilding currentBuilding = player2.getCurrentBuilding();
                    if (currentBuilding == null) {
                        IsoDirections playerDir = IsoDirections.fromAngle(player2.getForwardDirection());
                        currentBuilding = this.GetPeekedInBuilding(player2.getCurrentSquare(), playerDir);
                        if (currentBuilding != null) {
                            this.playerWindowPeekingRoomId[i] = this.playerPeekedRoomId;
                        }
                    }
                }
            }
        }

        if (isoGameCharacter != null
            && isoGameCharacter.getCurrentSquare() != null
            && isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.hidewalls)) {
            this.maxZ = isoGameCharacter.getZi() + 1;
        }

        this.rendering = true;

        try {
            this.RenderTiles(MaxHeight);
        } catch (Exception var23) {
            this.rendering = false;
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, var23);
        }

        this.rendering = false;
        if (IsoGridSquare.getRecalcLightTime() < 0.0F) {
            IsoGridSquare.setRecalcLightTime(60.0F);
        }

        if (IsoGridSquare.getLightcache() <= 0) {
            IsoGridSquare.setLightcache(90);
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("renderLast")) {
            this.renderLast();
        }

        IsoTree.renderChopTreeIndicators();
        if (Core.debug) {
        }

        this.lastMinX = this.minX;
        this.lastMinY = this.minY;

        try (GameProfiler.ProfileArea ignored = profiler.profile("DoBuilding")) {
            this.DoBuilding(IsoPlayer.getPlayerIndex(), true);
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("renderRain")) {
            this.renderRain();
        }
    }

    private void renderLast() {
        for (int n = 0; n < this.objectList.size(); n++) {
            IsoMovingObject obj = this.objectList.get(n);
            obj.renderlast();
        }

        for (int i = 0; i < this.staticUpdaterObjectList.size(); i++) {
            IsoObject obj = this.staticUpdaterObjectList.get(i);
            obj.renderlast();
        }
    }

    private void updateZombiesForRender(IsoGameCharacter isoGameCharacter, int playerIndex) {
        IsoGridSquare playerSquare = isoGameCharacter.getCurrentSquare();
        IsoGridSquare currentPlayerSquare = null;
        int scanCount = 10;
        if (this.zombieList.size() < 10) {
            scanCount = this.zombieList.size();
        }

        if (this.nearestVisibleZombie[playerIndex] != null) {
            if (this.nearestVisibleZombie[playerIndex].isDead()) {
                this.nearestVisibleZombie[playerIndex] = null;
            } else {
                float diffX = this.nearestVisibleZombie[playerIndex].getX() - isoGameCharacter.getX();
                float diffY = this.nearestVisibleZombie[playerIndex].getY() - isoGameCharacter.getY();
                this.nearestVisibleZombieDistSqr[playerIndex] = diffX * diffX + diffY * diffY;
            }
        }

        for (int scan = 0; scan < scanCount; this.zombieScanCursor++) {
            if (this.zombieScanCursor >= this.zombieList.size()) {
                this.zombieScanCursor = 0;
            }

            IsoZombie candidateZombie = this.zombieList.get(this.zombieScanCursor);
            if (candidateZombie != null) {
                IsoGridSquare candidateZombieSquare = candidateZombie.getCurrentSquare();
                if (candidateZombieSquare != null && playerSquare.z == candidateZombieSquare.z && candidateZombieSquare.getCanSee(playerIndex)) {
                    if (this.nearestVisibleZombie[playerIndex] == null) {
                        this.nearestVisibleZombie[playerIndex] = candidateZombie;
                        float diffX = this.nearestVisibleZombie[playerIndex].getX() - isoGameCharacter.getX();
                        float diffY = this.nearestVisibleZombie[playerIndex].getY() - isoGameCharacter.getY();
                        this.nearestVisibleZombieDistSqr[playerIndex] = diffX * diffX + diffY * diffY;
                    } else {
                        float diffX = candidateZombie.getX() - isoGameCharacter.getX();
                        float diffY = candidateZombie.getY() - isoGameCharacter.getY();
                        float candidateZombieDistSqr = diffX * diffX + diffY * diffY;
                        if (candidateZombieDistSqr < this.nearestVisibleZombieDistSqr[playerIndex]) {
                            this.nearestVisibleZombie[playerIndex] = candidateZombie;
                            this.nearestVisibleZombieDistSqr[playerIndex] = candidateZombieDistSqr;
                        }
                    }
                }
            }

            scan++;
        }

        for (int playerIter = 0; playerIter < 4; playerIter++) {
            IsoPlayer player2 = IsoPlayer.players[playerIter];
            if (player2 != null && player2.getCurrentSquare() != null) {
                IsoGridSquare playerIterSquare = player2.getCurrentSquare();
                if (playerIter == playerIndex) {
                    currentPlayerSquare = playerIterSquare;
                }

                double xInSquare = player2.getX() - PZMath.fastfloor(player2.getX());
                double yInSquare = player2.getY() - PZMath.fastfloor(player2.getY());
                boolean bRightOfSquare = xInSquare > yInSquare;
                if (this.lastPlayerAngle[playerIter] == null) {
                    this.lastPlayerAngle[playerIter] = new Vector2(player2.getForwardDirection());
                    this.playerCutawaysDirty[playerIter] = true;
                } else if (player2.getForwardDirection().dot(this.lastPlayerAngle[playerIter]) < 0.98F) {
                    this.lastPlayerAngle[playerIter].set(player2.getForwardDirection());
                    this.playerCutawaysDirty[playerIter] = true;
                }

                IsoDirections playerDir = IsoDirections.fromAngle(player2.getForwardDirection());
                if (this.lastPlayerSquare[playerIter] != playerIterSquare
                    || this.lastPlayerSquareHalf[playerIter] != bRightOfSquare
                    || this.lastPlayerDir[playerIter] != playerDir) {
                    this.playerCutawaysDirty[playerIter] = true;
                    this.lastPlayerSquare[playerIter] = playerIterSquare;
                    this.lastPlayerSquareHalf[playerIter] = bRightOfSquare;
                    this.lastPlayerDir[playerIter] = playerDir;
                    IsoBuilding currentBuilding = playerIterSquare.getBuilding();
                    this.playerWindowPeekingRoomId[playerIter] = -1L;
                    this.GetBuildingsInFrontOfCharacter(this.playerOccluderBuildings.get(playerIter), playerIterSquare, bRightOfSquare);
                    if (this.playerOccluderBuildingsArr[playerIndex] == null) {
                        this.playerOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                    }

                    this.playerHidesOrphanStructures[playerIter] = this.occludedByOrphanStructureFlag;
                    if (currentBuilding == null && !player2.remote) {
                        currentBuilding = this.GetPeekedInBuilding(playerIterSquare, playerDir);
                        if (currentBuilding != null) {
                            this.playerWindowPeekingRoomId[playerIter] = this.playerPeekedRoomId;
                        }
                    }

                    if (currentBuilding != null) {
                        this.AddUniqueToBuildingList(this.playerOccluderBuildings.get(playerIter), currentBuilding);
                    }

                    ArrayList<IsoBuilding> arr = this.playerOccluderBuildings.get(playerIter);

                    for (int i = 0; i < arr.size(); i++) {
                        IsoBuilding isoBuilding = arr.get(i);
                        this.playerOccluderBuildingsArr[playerIndex][i] = isoBuilding;
                    }

                    this.playerOccluderBuildingsArr[playerIndex][arr.size()] = null;
                }

                if (playerIter == playerIndex && currentPlayerSquare != null) {
                    this.gridSquaresTempLeft.clear();
                    this.gridSquaresTempRight.clear();
                    this.GetSquaresAroundPlayerSquare(player2, currentPlayerSquare, this.gridSquaresTempLeft, this.gridSquaresTempRight);

                    for (int i = 0; i < this.gridSquaresTempLeft.size(); i++) {
                        IsoGridSquare square = this.gridSquaresTempLeft.get(i);
                        if (square.getCanSee(playerIndex) && (square.getBuilding() == null || square.getBuilding() == currentPlayerSquare.getBuilding())) {
                            ArrayList<IsoBuilding> buildings = this.GetBuildingsInFrontOfMustSeeSquare(square, IsoGridOcclusionData.OcclusionFilter.Right);

                            for (int j = 0; j < buildings.size(); j++) {
                                this.AddUniqueToBuildingList(this.otherOccluderBuildings.get(playerIndex), buildings.get(j));
                            }

                            this.playerHidesOrphanStructures[playerIndex] = this.playerHidesOrphanStructures[playerIndex] | this.occludedByOrphanStructureFlag;
                        }
                    }

                    for (int ix = 0; ix < this.gridSquaresTempRight.size(); ix++) {
                        IsoGridSquare square = this.gridSquaresTempRight.get(ix);
                        if (square.getCanSee(playerIndex) && (square.getBuilding() == null || square.getBuilding() == currentPlayerSquare.getBuilding())) {
                            ArrayList<IsoBuilding> buildings = this.GetBuildingsInFrontOfMustSeeSquare(square, IsoGridOcclusionData.OcclusionFilter.Left);

                            for (int j = 0; j < buildings.size(); j++) {
                                this.AddUniqueToBuildingList(this.otherOccluderBuildings.get(playerIndex), buildings.get(j));
                            }

                            this.playerHidesOrphanStructures[playerIndex] = this.playerHidesOrphanStructures[playerIndex] | this.occludedByOrphanStructureFlag;
                        }
                    }

                    ArrayList<IsoBuilding> oocc = this.otherOccluderBuildings.get(playerIndex);
                    if (this.otherOccluderBuildingsArr[playerIndex] == null) {
                        this.otherOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                    }

                    for (int ixx = 0; ixx < oocc.size(); ixx++) {
                        IsoBuilding isoBuilding = oocc.get(ixx);
                        this.otherOccluderBuildingsArr[playerIndex][ixx] = isoBuilding;
                    }

                    this.otherOccluderBuildingsArr[playerIndex][oocc.size()] = null;
                }

                if (this.playerHidesOrphanStructures[playerIter] && this.hidesOrphanStructuresAbove > playerIterSquare.getZ()) {
                    this.hidesOrphanStructuresAbove = playerIterSquare.getZ();
                }
            }
        }

        if (currentPlayerSquare != null && this.hidesOrphanStructuresAbove < currentPlayerSquare.getZ()) {
            this.hidesOrphanStructuresAbove = currentPlayerSquare.getZ();
        }

        boolean bSetZombieBuildings = false;
        if (this.nearestVisibleZombie[playerIndex] != null && this.nearestVisibleZombieDistSqr[playerIndex] < 150.0F) {
            IsoGridSquare zombieSquare = this.nearestVisibleZombie[playerIndex].getCurrentSquare();
            if (zombieSquare != null && zombieSquare.getCanSee(playerIndex)) {
                double xInSquarex = this.nearestVisibleZombie[playerIndex].getX() - PZMath.fastfloor(this.nearestVisibleZombie[playerIndex].getX());
                double yInSquarex = this.nearestVisibleZombie[playerIndex].getY() - PZMath.fastfloor(this.nearestVisibleZombie[playerIndex].getY());
                boolean bRightOfSquarex = xInSquarex > yInSquarex;
                bSetZombieBuildings = true;
                if (this.lastZombieSquare[playerIndex] != zombieSquare || this.lastZombieSquareHalf[playerIndex] != bRightOfSquarex) {
                    this.lastZombieSquare[playerIndex] = zombieSquare;
                    this.lastZombieSquareHalf[playerIndex] = bRightOfSquarex;
                    this.GetBuildingsInFrontOfCharacter(this.zombieOccluderBuildings.get(playerIndex), zombieSquare, bRightOfSquarex);
                    ArrayList<IsoBuilding> zarr = this.zombieOccluderBuildings.get(playerIndex);
                    if (this.zombieOccluderBuildingsArr[playerIndex] == null) {
                        this.zombieOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                    }

                    for (int ixx = 0; ixx < zarr.size(); ixx++) {
                        IsoBuilding isoBuilding = zarr.get(ixx);
                        this.zombieOccluderBuildingsArr[playerIndex][ixx] = isoBuilding;
                    }

                    this.zombieOccluderBuildingsArr[playerIndex][zarr.size()] = null;
                }
            }
        }

        if (!bSetZombieBuildings) {
            this.zombieOccluderBuildings.get(playerIndex).clear();
            if (this.zombieOccluderBuildingsArr[playerIndex] != null) {
                this.zombieOccluderBuildingsArr[playerIndex][0] = null;
            } else {
                this.zombieOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
            }
        }
    }

    public void invalidatePeekedRoom(int playerIndex) {
        this.lastPlayerDir[playerIndex] = IsoDirections.Max;
    }

    protected boolean initWeatherFx() {
        if (GameServer.server) {
            return false;
        } else {
            if (this.weatherFx == null) {
                this.weatherFx = new IsoWeatherFX();
                this.weatherFx.init();
            }

            return true;
        }
    }

    private void updateWeatherFx() {
        if (this.initWeatherFx()) {
            this.weatherFx.update();
        }
    }

    private void renderWeatherFx() {
        if (this.initWeatherFx()) {
            this.weatherFx.render();
        }
    }

    public IsoWeatherFX getWeatherFX() {
        return this.weatherFx;
    }

    public void renderRain() {
    }

    public void setRainAlpha(int alpha) {
        this.rainAlphaMax = alpha / 100.0F;
    }

    public void setRainIntensity(int intensity) {
        this.rainIntensity = intensity;
    }

    public int getRainIntensity() {
        return this.rainIntensity;
    }

    public void setRainSpeed(int speed) {
        this.rainSpeed = speed;
    }

    public void reloadRainTextures() {
    }

    public void GetBuildingsInFrontOfCharacter(ArrayList<IsoBuilding> buildings, IsoGridSquare square, boolean bRightOfSquare) {
        buildings.clear();
        this.occludedByOrphanStructureFlag = false;
        if (square != null) {
            int x = square.getX();
            int y = square.getY();
            int z = square.getZ();
            this.GetBuildingsInFrontOfCharacterSquare(x, y, z, bRightOfSquare, buildings);
            if (z < maxHeight) {
                this.GetBuildingsInFrontOfCharacterSquare(x - 1 + 3, y - 1 + 3, z + 1, bRightOfSquare, buildings);
                this.GetBuildingsInFrontOfCharacterSquare(x - 2 + 3, y - 2 + 3, z + 1, bRightOfSquare, buildings);
                if (bRightOfSquare) {
                    this.GetBuildingsInFrontOfCharacterSquare(x + 3, y - 1 + 3, z + 1, !bRightOfSquare, buildings);
                    this.GetBuildingsInFrontOfCharacterSquare(x - 1 + 3, y - 2 + 3, z + 1, !bRightOfSquare, buildings);
                } else {
                    this.GetBuildingsInFrontOfCharacterSquare(x - 1 + 3, y + 3, z + 1, !bRightOfSquare, buildings);
                    this.GetBuildingsInFrontOfCharacterSquare(x - 2 + 3, y - 1 + 3, z + 1, !bRightOfSquare, buildings);
                }
            }
        }
    }

    private void GetBuildingsInFrontOfCharacterSquare(int inX, int inY, int inZ, boolean bRightOfSquare, ArrayList<IsoBuilding> outBuildings) {
        IsoGridSquare square = this.getGridSquare(inX, inY, inZ);
        if (square == null) {
            if (inZ < maxHeight) {
                this.GetBuildingsInFrontOfCharacterSquare(inX + 3, inY + 3, inZ + 1, bRightOfSquare, outBuildings);
            }
        } else {
            IsoGridOcclusionData occlusion = square.getOrCreateOcclusionData();
            IsoGridOcclusionData.OcclusionFilter occlusionFilter = bRightOfSquare
                ? IsoGridOcclusionData.OcclusionFilter.Right
                : IsoGridOcclusionData.OcclusionFilter.Left;
            this.occludedByOrphanStructureFlag = this.occludedByOrphanStructureFlag | occlusion.getCouldBeOccludedByOrphanStructures(occlusionFilter);
            ArrayList<IsoBuilding> occluders = occlusion.getBuildingsCouldBeOccluders(occlusionFilter);

            for (int i = 0; i < occluders.size(); i++) {
                this.AddUniqueToBuildingList(outBuildings, occluders.get(i));
            }
        }
    }

    public ArrayList<IsoBuilding> GetBuildingsInFrontOfMustSeeSquare(IsoGridSquare square, IsoGridOcclusionData.OcclusionFilter filter) {
        IsoGridOcclusionData occlusion = square.getOrCreateOcclusionData();
        this.occludedByOrphanStructureFlag = occlusion.getCouldBeOccludedByOrphanStructures(IsoGridOcclusionData.OcclusionFilter.All);
        return occlusion.getBuildingsCouldBeOccluders(filter);
    }

    public IsoBuilding GetPeekedInBuilding(IsoGridSquare square, IsoDirections lookDir) {
        this.playerPeekedRoomId = -1L;
        if (square == null) {
            return null;
        } else {
            if ((lookDir == IsoDirections.NW || lookDir == IsoDirections.N || lookDir == IsoDirections.NE)
                && LosUtil.lineClear(this, square.x, square.y, square.z, square.x, square.y - 1, square.z, false) != LosUtil.TestResults.Blocked) {
                IsoGridSquare targetSquare = square.getAdjacentSquare(IsoDirections.N);
                if (targetSquare != null) {
                    IsoBuilding peekedBuilding = targetSquare.getBuilding();
                    if (peekedBuilding != null) {
                        this.playerPeekedRoomId = targetSquare.getRoomID();
                        return peekedBuilding;
                    }
                }
            }

            if ((lookDir == IsoDirections.SW || lookDir == IsoDirections.W || lookDir == IsoDirections.NW)
                && LosUtil.lineClear(this, square.x, square.y, square.z, square.x - 1, square.y, square.z, false) != LosUtil.TestResults.Blocked) {
                IsoGridSquare targetSquare = square.getAdjacentSquare(IsoDirections.W);
                if (targetSquare != null) {
                    IsoBuilding peekedBuilding = targetSquare.getBuilding();
                    if (peekedBuilding != null) {
                        this.playerPeekedRoomId = targetSquare.getRoomID();
                        return peekedBuilding;
                    }
                }
            }

            if ((lookDir == IsoDirections.SE || lookDir == IsoDirections.S || lookDir == IsoDirections.SW)
                && LosUtil.lineClear(this, square.x, square.y, square.z, square.x, square.y + 1, square.z, false) != LosUtil.TestResults.Blocked) {
                IsoGridSquare targetSquare = square.getAdjacentSquare(IsoDirections.S);
                if (targetSquare != null) {
                    IsoBuilding peekedBuilding = targetSquare.getBuilding();
                    if (peekedBuilding != null) {
                        this.playerPeekedRoomId = targetSquare.getRoomID();
                        return peekedBuilding;
                    }
                }
            }

            if ((lookDir == IsoDirections.NE || lookDir == IsoDirections.E || lookDir == IsoDirections.SE)
                && LosUtil.lineClear(this, square.x, square.y, square.z, square.x + 1, square.y, square.z, false) != LosUtil.TestResults.Blocked) {
                IsoGridSquare targetSquare = square.getAdjacentSquare(IsoDirections.E);
                if (targetSquare != null) {
                    IsoBuilding peekedBuilding = targetSquare.getBuilding();
                    if (peekedBuilding != null) {
                        this.playerPeekedRoomId = targetSquare.getRoomID();
                        return peekedBuilding;
                    }
                }
            }

            return null;
        }
    }

    public void GetSquaresAroundPlayerSquare(
        IsoPlayer player, IsoGridSquare square, ArrayList<IsoGridSquare> outGridSquaresToLeft, ArrayList<IsoGridSquare> outGridSquaresToRight
    ) {
        float fMinX = player.getX() - 4.0F;
        float fMinY = player.getY() - 4.0F;
        int startX = PZMath.fastfloor(fMinX);
        int startY = PZMath.fastfloor(fMinY);
        int z = square.getZ();

        for (int y = startY; y < startY + 10; y++) {
            for (int x = startX; x < startX + 10; x++) {
                if ((x >= PZMath.fastfloor(player.getX()) || y >= PZMath.fastfloor(player.getY()))
                    && (x != PZMath.fastfloor(player.getX()) || y != PZMath.fastfloor(player.getY()))) {
                    float deltaX = x - player.getX();
                    float deltaY = y - player.getY();
                    if (deltaY < deltaX + 4.5 && deltaY > deltaX - 4.5) {
                        IsoGridSquare iterSquare = this.getGridSquare(x, y, z);
                        if (iterSquare != null) {
                            if (deltaY >= deltaX) {
                                outGridSquaresToLeft.add(iterSquare);
                            }

                            if (deltaY <= deltaX) {
                                outGridSquaresToRight.add(iterSquare);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean IsBehindStuff(IsoGridSquare sq) {
        if (!sq.getProperties().has(IsoFlagType.exterior)) {
            return true;
        } else {
            for (int z = 1; z < 64 && sq.getZ() + z < maxHeight; z++) {
                for (int y = -5; y <= 6; y++) {
                    for (int x = -5; x <= 6; x++) {
                        if (x >= y - 5 && x <= y + 5) {
                            IsoGridSquare sq2 = this.getGridSquare(sq.getX() + x + z * 3, sq.getY() + y + z * 3, sq.getZ() + z);
                            if (sq2 != null && !sq2.getObjects().isEmpty()) {
                                if (z != 1 || sq2.getObjects().size() != 1) {
                                    return true;
                                }

                                IsoObject obj = sq2.getObjects().get(0);
                                if (obj.sprite == null || obj.sprite.name == null || !obj.sprite.name.startsWith("lighting_outdoor")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    public static IsoDirections FromMouseTile() {
        IsoDirections dir = IsoDirections.N;
        float x = UIManager.getPickedTileLocal().x;
        float y = UIManager.getPickedTileLocal().y;
        float centrenessY = 0.5F - Math.abs(0.5F - y);
        float centrenessX = 0.5F - Math.abs(0.5F - x);
        if (x > 0.5F && centrenessX < centrenessY) {
            dir = IsoDirections.E;
        } else if (y > 0.5F && centrenessX > centrenessY) {
            dir = IsoDirections.S;
        } else if (x < 0.5F && centrenessX < centrenessY) {
            dir = IsoDirections.W;
        } else if (y < 0.5F && centrenessX > centrenessY) {
            dir = IsoDirections.N;
        }

        return dir;
    }

    public void update() {
        try (AbstractPerformanceProfileProbe ignored = IsoCell.s_performance.isoCellUpdate.profile()) {
            this.updateInternal();
        }
    }

    private void updateInternal() {
        MovingObjectUpdateScheduler.instance.startFrame();
        IsoSprite.alphaStep = 0.075F * GameTime.getInstance().getThirtyFPSMultiplier();
        IsoGridSquare.gridSquareCacheEmptyTimer++;
        GameProfiler profiler = GameProfiler.getInstance();

        try (GameProfiler.ProfileArea ignored = profiler.profile("SpottedRooms")) {
            this.ProcessSpottedRooms();
        }

        if (!GameServer.server) {
            for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                if (IsoPlayer.players[n] != null && (!IsoPlayer.players[n].isDead() || IsoPlayer.players[n].reanimatedCorpse != null)) {
                    IsoPlayer.setInstance(IsoPlayer.players[n]);
                    IsoCamera.setCameraCharacter(IsoPlayer.players[n]);
                    this.chunkMap[n].update();
                }
            }
        }

        CompletableFuture<Void> itemsFuture = null;
        this.ProcessRemoveItems(null);
        if (!GameClient.client && !GameServer.server || GameServer.server && System.currentTimeMillis() - this.lastServerItemsUpdate > 5000L) {
            this.lastServerItemsUpdate = System.currentTimeMillis();

            try (GameProfiler.ProfileArea ignored = profiler.profile("Items")) {
                this.ProcessItems(null);
            }
        }

        this.ProcessRemoveItems(null);

        try (GameProfiler.ProfileArea ignored = profiler.profile("IsoObject")) {
            this.ProcessIsoObject();
        }

        this.safeToAdd = false;

        try (GameProfiler.ProfileArea ignored = profiler.profile("Objects")) {
            this.ProcessObjects(null);
        }

        if (GameClient.client
            && (
                NetworkZombieSimulator.getInstance().anyUnknownZombies() && GameClient.instance.sendZombieRequestsTimer.Check()
                    || GameClient.instance.sendZombieTimer.Check()
            )) {
            NetworkZombieSimulator.getInstance().send();
            GameClient.instance.sendZombieTimer.Reset();
            GameClient.instance.sendZombieRequestsTimer.Reset();
        }

        if (itemsFuture != null) {
            try (GameProfiler.ProfileArea ignored = profiler.profile("Items")) {
                itemsFuture.join();
            }
        }

        this.safeToAdd = true;

        try (GameProfiler.ProfileArea ignored = profiler.profile("Static Updaters")) {
            this.ProcessStaticUpdaters();
        }

        this.ObjectDeletionAddition();

        try (GameProfiler.ProfileArea ignored = profiler.profile("Update Dead Bodies")) {
            IsoDeadBody.updateBodies();
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("Update Fish")) {
            FishSchoolManager.getInstance().update();
        }

        IsoGridSquare.setLightcache(IsoGridSquare.getLightcache() - 1);
        IsoGridSquare.setRecalcLightTime(IsoGridSquare.getRecalcLightTime() - GameTime.getInstance().getThirtyFPSMultiplier());
        if (GameServer.server) {
            this.lamppostPositions.clear();
            this.roomLights.clear();
        }

        if (!GameTime.isGamePaused()) {
            this.rainScroll = this.rainScroll + this.rainSpeed / 10.0F * 0.075F * (30.0F / PerformanceSettings.getLockFPS());
            if (this.rainScroll > 1.0F) {
                this.rainScroll = 0.0F;
            }
        }

        if (!GameServer.server) {
            try (GameProfiler.ProfileArea ignored = profiler.profile("Update Weather")) {
                this.updateWeatherFx();
            }
        }
    }

    IsoGridSquare getRandomFreeTile() {
        IsoGridSquare tile = null;
        boolean bDone = true;

        do {
            bDone = true;
            tile = this.getGridSquare(Rand.Next(this.width), Rand.Next(this.height), 0);
            if (tile == null) {
                bDone = false;
            } else if (!tile.isFree(false)) {
                bDone = false;
            } else if (tile.getProperties().has(IsoFlagType.solid) || tile.getProperties().has(IsoFlagType.solidtrans)) {
                bDone = false;
            } else if (!tile.getMovingObjects().isEmpty()) {
                bDone = false;
            } else if (tile.has(IsoObjectType.stairsBN) || tile.has(IsoObjectType.stairsMN) || tile.has(IsoObjectType.stairsTN)) {
                bDone = false;
            } else if (tile.has(IsoObjectType.stairsBW) || tile.has(IsoObjectType.stairsMW) || tile.has(IsoObjectType.stairsTW)) {
                bDone = false;
            }
        } while (!bDone);

        return tile;
    }

    IsoGridSquare getRandomOutdoorFreeTile() {
        IsoGridSquare tile = null;
        boolean bDone = true;

        do {
            bDone = true;
            tile = this.getGridSquare(Rand.Next(this.width), Rand.Next(this.height), 0);
            if (tile == null) {
                bDone = false;
            } else if (!tile.isFree(false)) {
                bDone = false;
            } else if (tile.getRoom() != null) {
                bDone = false;
            } else if (tile.getProperties().has(IsoFlagType.solid) || tile.getProperties().has(IsoFlagType.solidtrans)) {
                bDone = false;
            } else if (!tile.getMovingObjects().isEmpty()) {
                bDone = false;
            } else if (tile.has(IsoObjectType.stairsBN) || tile.has(IsoObjectType.stairsMN) || tile.has(IsoObjectType.stairsTN)) {
                bDone = false;
            } else if (tile.has(IsoObjectType.stairsBW) || tile.has(IsoObjectType.stairsMW) || tile.has(IsoObjectType.stairsTW)) {
                bDone = false;
            }
        } while (!bDone);

        return tile;
    }

    public IsoGridSquare getRandomFreeTileInRoom() {
        Stack<IsoRoom> rooms = new Stack<>();

        for (int n = 0; n < this.roomList.size(); n++) {
            if (this.roomList.get(n).tileList.size() > 9
                && !this.roomList.get(n).exits.isEmpty()
                && this.roomList.get(n).tileList.get(0).getProperties().has(IsoFlagType.solidfloor)) {
                rooms.add(this.roomList.get(n));
            }
        }

        if (rooms.isEmpty()) {
            return null;
        } else {
            IsoRoom room = rooms.get(Rand.Next(rooms.size()));
            return room.getFreeTile();
        }
    }

    public void roomSpotted(IsoRoom room) {
        synchronized (this.spottedRooms) {
            if (!this.spottedRooms.contains(room)) {
                this.spottedRooms.push(room);
            }
        }
    }

    public void ProcessSpottedRooms() {
        synchronized (this.spottedRooms) {
            for (int i = 0; i < this.spottedRooms.size(); i++) {
                IsoRoom room = this.spottedRooms.get(i);
                if (!room.def.doneSpawn) {
                    room.def.doneSpawn = true;
                    LuaEventManager.triggerEvent("OnSeeNewRoom", room);
                    VirtualZombieManager.instance.roomSpotted(room);
                    if (!GameClient.client
                        && !Core.lastStand
                        && (
                            "shed".equals(room.def.name)
                                || "garage".equals(room.def.name)
                                || "garagestorage".equals(room.def.name)
                                || "storageunit".equals(room.def.name)
                                || "farmstorage".equals(room.def.name)
                        )) {
                        int chance = 7;
                        if ("shed".equals(room.def.name) || "garagestorage".equals(room.def.name) || "farmstorage".equals(room.def.name)) {
                            chance = 4;
                        }

                        switch (SandboxOptions.instance.generatorSpawning.getValue()) {
                            case 1:
                                chance = 0;
                                break;
                            case 2:
                                chance += 20;
                                break;
                            case 3:
                                chance += 3;
                                break;
                            case 4:
                                chance += 2;
                            case 5:
                            default:
                                break;
                            case 6:
                                chance -= 2;
                                break;
                            case 7:
                                chance -= 3;
                        }

                        if (chance != 0 && Rand.Next(chance) == 0) {
                            room.spawnRandomWorkstation();
                        }

                        if (chance != 0 && Rand.Next(chance) == 0) {
                            IsoGridSquare square = room.getRandomDoorFreeSquare();
                            if (square != null) {
                                square.spawnRandomGenerator();
                            }
                        }
                    }
                }
            }

            this.spottedRooms.clear();
        }
    }

    public IsoObject addTileObject(IsoGridSquare sq, String spriteName) {
        if (sq == null) {
            return null;
        } else {
            IsoObject obj = IsoObject.getNew(sq, spriteName, null, false);
            sq.AddTileObject(obj);
            MapObjects.newGridSquare(sq);
            MapObjects.loadGridSquare(sq);
            return obj;
        }
    }

    public void save(DataOutputStream output, boolean bDoChars) throws IOException {
        while (ChunkSaveWorker.instance.saving) {
            try {
                MainThread.busyWait();
                Thread.sleep(30L);
            } catch (InterruptedException var5) {
                ExceptionLogger.logException(var5);
            }
        }

        for (int n = 0; n < IsoPlayer.numPlayers; n++) {
            this.chunkMap[n].Save();
        }

        output.writeInt(this.width);
        output.writeInt(this.height);
        output.writeInt(maxHeight);
        File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");
        FileOutputStream outStream = new FileOutputStream(outFile);
        output = new DataOutputStream(new BufferedOutputStream(outStream));
        GameTime.instance.save(output);
        output.flush();
        output.close();
        if (!GameClient.client) {
            IsoWorld.instance.metaGrid.save();
        }

        if (PlayerDB.isAllow()) {
            PlayerDB.getInstance().savePlayers();
        }

        ReanimatedPlayers.instance.saveReanimatedPlayers();
    }

    public boolean LoadPlayer(int WorldVersion) throws FileNotFoundException, IOException {
        if (GameClient.client) {
            return ClientPlayerDB.getInstance().loadNetworkPlayer();
        } else {
            File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_p.bin");
            if (!inFile.exists()) {
                PlayerDB.getInstance().importPlayersFromVehiclesDB();
                return PlayerDB.getInstance().loadLocalPlayer(1);
            } else {
                FileInputStream inStream = new FileInputStream(inFile);
                BufferedInputStream input = new BufferedInputStream(inStream);
                synchronized (SliceY.SliceBufferLock) {
                    SliceY.SliceBuffer.clear();
                    int numBytes = input.read(SliceY.SliceBuffer.array());
                    SliceY.SliceBuffer.limit(numBytes);
                    byte b1 = SliceY.SliceBuffer.get();
                    byte b2 = SliceY.SliceBuffer.get();
                    byte b3 = SliceY.SliceBuffer.get();
                    byte b4 = SliceY.SliceBuffer.get();
                    if (b1 == 80 && b2 == 76 && b3 == 89 && b4 == 82) {
                        WorldVersion = SliceY.SliceBuffer.getInt();
                    } else {
                        SliceY.SliceBuffer.rewind();
                    }

                    String serverPlayerID = GameWindow.ReadString(SliceY.SliceBuffer);
                    if (GameClient.client && !IsoPlayer.isServerPlayerIDValid(serverPlayerID)) {
                        GameLoadingState.gameLoadingString = Translator.getText("IGUI_MP_ServerPlayerIDMismatch");
                        GameLoadingState.playerWrongIP = true;
                        return false;
                    }

                    instance.chunkMap[IsoPlayer.getPlayerIndex()].worldX = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsetx * 32;
                    instance.chunkMap[IsoPlayer.getPlayerIndex()].worldY = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsety * 32;
                    SliceY.SliceBuffer.getInt();
                    SliceY.SliceBuffer.getInt();
                    SliceY.SliceBuffer.getInt();
                    if (IsoPlayer.getInstance() == null) {
                        IsoPlayer.setInstance(new IsoPlayer(instance));
                        IsoPlayer.players[0] = IsoPlayer.getInstance();
                    }

                    IsoPlayer.getInstance().load(SliceY.SliceBuffer, WorldVersion);
                    inStream.close();
                }

                PlayerDB.getInstance().saveLocalPlayersForce();
                inFile.delete();
                PlayerDB.getInstance().uploadLocalPlayers2DB();
                return true;
            }
        }
    }

    public IsoGridSquare getRelativeGridSquare(int x, int y, int z) {
        int wx = this.chunkMap[0].getWorldXMin() * 8;
        int wy = this.chunkMap[0].getWorldYMin() * 8;
        x += wx;
        y += wy;
        return this.getGridSquare(x, y, z);
    }

    public IsoGridSquare createNewGridSquare(int x, int y, int z, boolean recalcAll) {
        if (!IsoWorld.instance.isValidSquare(x, y, z)) {
            return null;
        } else {
            IsoGridSquare sq = this.getGridSquare(x, y, z);
            if (sq != null) {
                return sq;
            } else {
                if (GameServer.server) {
                    int wx = PZMath.coorddivision(x, 8);
                    int wy = PZMath.coorddivision(y, 8);
                    if (ServerMap.instance.getChunk(wx, wy) != null) {
                        sq = IsoGridSquare.getNew(this, null, x, y, z);
                        ServerMap.instance.setGridSquare(x, y, z, sq);
                    }
                } else if (this.getChunkForGridSquare(x, y, z) != null) {
                    sq = IsoGridSquare.getNew(this, null, x, y, z);
                    this.ConnectNewSquare(sq, true);
                }

                if (sq != null && recalcAll) {
                    sq.RecalcAllWithNeighbours(true);
                }

                return sq;
            }
        }
    }

    public IsoGridSquare getGridSquareDirect(int x, int y, int z, int playerIndex) {
        if (GameServer.server) {
            return null;
        } else {
            int length = IsoChunkMap.chunkWidthInTiles;
            return this.chunkMap[playerIndex].getGridSquareDirect(x, y, z);
        }
    }

    public boolean isInChunkMap(int x, int y) {
        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            int minX = this.chunkMap[pn].getWorldXMinTiles();
            int maxX = this.chunkMap[pn].getWorldXMaxTiles();
            int minY = this.chunkMap[pn].getWorldYMinTiles();
            int maxY = this.chunkMap[pn].getWorldYMaxTiles();
            if (x >= minX && x < maxX && y >= minY && y < maxY) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<IsoObject> getProcessIsoObjectRemove() {
        return this.processIsoObjectRemove;
    }

    public void checkHaveRoof(int x, int y) {
        boolean haveRoof = false;

        for (int z = 31; z >= 0; z--) {
            IsoGridSquare sq = this.getGridSquare(x, y, z);
            if (sq != null) {
                if (haveRoof != sq.haveRoof) {
                    sq.haveRoof = haveRoof;
                    sq.RecalcAllWithNeighbours(true);
                }

                if (sq.hasRainBlockingTile()) {
                    haveRoof = true;
                }
            }
        }
    }

    public IsoZombie getFakeZombieForHit() {
        if (this.fakeZombieForHit == null) {
            this.fakeZombieForHit = new IsoZombie(this);
        }

        return this.fakeZombieForHit;
    }

    public void addHeatSource(IsoHeatSource heatSource) {
        if (!GameServer.server) {
            if (this.heatSources.contains(heatSource)) {
                DebugLog.log("ERROR addHeatSource called again with the same HeatSource");
            } else {
                this.heatSources.add(heatSource);
            }
        }
    }

    public void removeHeatSource(IsoHeatSource heatSource) {
        if (!GameServer.server) {
            this.heatSources.remove(heatSource);
        }
    }

    public void updateHeatSources() {
        if (!GameServer.server) {
            for (int i = this.heatSources.size() - 1; i >= 0; i--) {
                IsoHeatSource heatSource = this.heatSources.get(i);
                if (!heatSource.isInBounds()) {
                    this.heatSources.remove(i);
                }
            }
        }
    }

    public int getHeatSourceTemperature(int x, int y, int z) {
        int temperature = 0;

        for (int i = 0; i < this.heatSources.size(); i++) {
            IsoHeatSource heatSource = this.heatSources.get(i);
            if (heatSource.getZ() == z) {
                float distSq = IsoUtils.DistanceToSquared(x, y, heatSource.getX(), heatSource.getY());
                if (distSq < heatSource.getRadius() * heatSource.getRadius()) {
                    LosUtil.TestResults los = LosUtil.lineClear(this, heatSource.getX(), heatSource.getY(), heatSource.getZ(), x, y, z, false);
                    if (los == LosUtil.TestResults.Clear || los == LosUtil.TestResults.ClearThroughOpenDoor) {
                        temperature = (int)(temperature + heatSource.getTemperature() * (1.0 - Math.sqrt(distSq) / heatSource.getRadius()));
                    }
                }
            }
        }

        return temperature;
    }

    public float getHeatSourceHighestTemperature(float surroundingAirTemperature, int x, int y, int z) {
        float airTemp = surroundingAirTemperature;
        float temperature = surroundingAirTemperature;
        float testTemp = 0.0F;
        IsoGridSquare gs = null;
        float mod = 0.0F;

        for (int i = 0; i < this.heatSources.size(); i++) {
            IsoHeatSource heatSource = this.heatSources.get(i);
            if (heatSource.getZ() == z) {
                float distSq = IsoUtils.DistanceToSquared(x, y, heatSource.getX(), heatSource.getY());
                gs = this.getGridSquare(heatSource.getX(), heatSource.getY(), heatSource.getZ());
                mod = 0.0F;
                if (gs != null) {
                    if (!gs.isInARoom()) {
                        mod = airTemp - 30.0F;
                        if (mod < -15.0F) {
                            mod = -15.0F;
                        } else if (mod > 5.0F) {
                            mod = 5.0F;
                        }
                    } else {
                        mod = airTemp - 30.0F;
                        if (mod < -7.0F) {
                            mod = -7.0F;
                        } else if (mod > 7.0F) {
                            mod = 7.0F;
                        }
                    }
                }

                testTemp = ClimateManager.lerp((float)(1.0 - Math.sqrt(distSq) / heatSource.getRadius()), airTemp, heatSource.getTemperature() + mod);
                if (!(testTemp <= temperature) && distSq < heatSource.getRadius() * heatSource.getRadius()) {
                    LosUtil.TestResults los = LosUtil.lineClear(this, heatSource.getX(), heatSource.getY(), heatSource.getZ(), x, y, z, false);
                    if (los == LosUtil.TestResults.Clear || los == LosUtil.TestResults.ClearThroughOpenDoor) {
                        temperature = testTemp;
                    }
                }
            }
        }

        return temperature;
    }

    public void putInVehicle(IsoGameCharacter chr) {
        if (chr != null && chr.savedVehicleSeat != -1) {
            int chunkMinX = (PZMath.fastfloor(chr.getX()) - 4) / 8;
            int chunkMinY = (PZMath.fastfloor(chr.getY()) - 4) / 8;
            int chunkMaxX = (PZMath.fastfloor(chr.getX()) + 4) / 8;
            int chunkMaxY = (PZMath.fastfloor(chr.getY()) + 4) / 8;

            for (int wy = chunkMinY; wy <= chunkMaxY; wy++) {
                for (int wx = chunkMinX; wx <= chunkMaxX; wx++) {
                    IsoChunk chunk = this.getChunkForGridSquare(wx * 8, wy * 8, PZMath.fastfloor(chr.getZ()));
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (PZMath.fastfloor(vehicle.getZ()) == PZMath.fastfloor(chr.getZ())
                                && IsoUtils.DistanceToSquared(vehicle.getX(), vehicle.getY(), chr.savedVehicleX, chr.savedVehicleY) < 0.010000001F) {
                                if (vehicle.vehicleId == -1) {
                                    return;
                                }

                                VehicleScript.Position position = vehicle.getPassengerPosition(chr.savedVehicleSeat, "inside");
                                if (position != null && !vehicle.isSeatOccupied(chr.savedVehicleSeat)) {
                                    vehicle.enter(chr.savedVehicleSeat, chr, position.offset);
                                    LuaEventManager.triggerEvent("OnEnterVehicle", chr);
                                    if (vehicle.getCharacter(chr.savedVehicleSeat) == chr && chr.savedVehicleRunning) {
                                        vehicle.resumeRunningAfterLoad();
                                    }
                                }

                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    public void resumeVehicleSounds(IsoGameCharacter chr) {
        if (chr != null && chr.savedVehicleSeat != -1) {
            int chunkMinX = (PZMath.fastfloor(chr.getX()) - 4) / 8;
            int chunkMinY = (PZMath.fastfloor(chr.getY()) - 4) / 8;
            int chunkMaxX = (PZMath.fastfloor(chr.getX()) + 4) / 8;
            int chunkMaxY = (PZMath.fastfloor(chr.getY()) + 4) / 8;

            for (int wy = chunkMinY; wy <= chunkMaxY; wy++) {
                for (int wx = chunkMinX; wx <= chunkMaxX; wx++) {
                    IsoChunk chunk = this.getChunkForGridSquare(wx * 8, wy * 8, PZMath.fastfloor(chr.getZ()));
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle.lightbarSirenMode.isEnable()) {
                                vehicle.setLightbarSirenMode(vehicle.lightbarSirenMode.get());
                            }
                        }
                    }
                }
            }
        }
    }

    public void AddUniqueToBuildingList(ArrayList<IsoBuilding> buildings, IsoBuilding inBuilding) {
        for (int i = 0; i < buildings.size(); i++) {
            if (buildings.get(i) == inBuilding) {
                return;
            }
        }

        buildings.add(inBuilding);
    }

    public IsoSpriteManager getSpriteManager() {
        return IsoSpriteManager.instance;
    }

    public static enum BuildingSearchCriteria {
        Food,
        Defense,
        Wood,
        Weapons,
        General;
    }

    public static final class PerPlayerRender {
        public final IsoGridStack gridStacks = new IsoGridStack(65);
        public boolean[][][] visiOccludedFlags;
        public boolean[][] visiCulledFlags;
        public boolean[][] flattenGrassEtc;
        public int minX;
        public int minY;
        public int maxX;
        public int maxY;

        public void setSize(int w, int h) {
            if (this.visiOccludedFlags == null || this.visiOccludedFlags.length < w || this.visiOccludedFlags[0].length < h) {
                this.visiOccludedFlags = new boolean[w][h][2];
                this.visiCulledFlags = new boolean[w][h];
                this.flattenGrassEtc = new boolean[w][h];
            }
        }
    }

    private class SnowGrid {
        public int w;
        public int h;
        public int frac;
        public static final int N = 0;
        public static final int S = 1;
        public static final int W = 2;
        public static final int E = 3;
        public static final int A = 0;
        public static final int B = 1;
        public final Texture[][][] grid;
        public final byte[][][] gridType;

        public SnowGrid(final int frac) {
            Objects.requireNonNull(IsoCell.this);
            super();
            this.w = 256;
            this.h = 256;
            this.grid = new Texture[this.w][this.h][2];
            this.gridType = new byte[this.w][this.h][2];
            this.init(frac);
        }

        public IsoCell.SnowGrid init(int frac) {
            if (!IsoCell.this.hasSetupSnowGrid) {
                IsoCell.this.snowNoise2d = new Noise2D();
                IsoCell.this.snowNoise2d.addLayer(16, 0.5F, 3.0F);
                IsoCell.this.snowNoise2d.addLayer(32, 2.0F, 5.0F);
                IsoCell.this.snowNoise2d.addLayer(64, 5.0F, 8.0F);
                byte id = 0;
                IsoCell.this.snowGridTilesSquare = IsoCell.this.new SnowGridTiles(id++);
                int base = 40;

                for (int i = 0; i < 4; i++) {
                    IsoCell.this.snowGridTilesSquare.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (base + i)));
                }

                IsoCell.this.snowGridTilesEnclosed = IsoCell.this.new SnowGridTiles(id++);
                int var15 = 0;

                for (int i = 0; i < 4; i++) {
                    IsoCell.this.snowGridTilesEnclosed.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var15 + i)));
                }

                IsoCell.this.snowGridTilesCove = new IsoCell.SnowGridTiles[4];

                for (int i = 0; i < 4; i++) {
                    IsoCell.this.snowGridTilesCove[i] = IsoCell.this.new SnowGridTiles(id++);
                    if (i == 0) {
                        var15 = 7;
                    }

                    if (i == 2) {
                        var15 = 4;
                    }

                    if (i == 1) {
                        var15 = 5;
                    }

                    if (i == 3) {
                        var15 = 6;
                    }

                    for (int j = 0; j < 3; j++) {
                        IsoCell.this.snowGridTilesCove[i].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var15 + j * 4)));
                    }
                }

                IsoCell.this.snowFirstNonSquare = id;
                IsoCell.this.snowGridTilesEdge = new IsoCell.SnowGridTiles[4];

                for (int i = 0; i < 4; i++) {
                    IsoCell.this.snowGridTilesEdge[i] = IsoCell.this.new SnowGridTiles(id++);
                    if (i == 0) {
                        var15 = 16;
                    }

                    if (i == 2) {
                        var15 = 18;
                    }

                    if (i == 1) {
                        var15 = 17;
                    }

                    if (i == 3) {
                        var15 = 19;
                    }

                    for (int j = 0; j < 3; j++) {
                        IsoCell.this.snowGridTilesEdge[i].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var15 + j * 4)));
                    }
                }

                IsoCell.this.snowGridTilesStrip = new IsoCell.SnowGridTiles[4];

                for (int i = 0; i < 4; i++) {
                    IsoCell.this.snowGridTilesStrip[i] = IsoCell.this.new SnowGridTiles(id++);
                    if (i == 0) {
                        var15 = 28;
                    }

                    if (i == 2) {
                        var15 = 29;
                    }

                    if (i == 1) {
                        var15 = 31;
                    }

                    if (i == 3) {
                        var15 = 30;
                    }

                    for (int j = 0; j < 3; j++) {
                        IsoCell.this.snowGridTilesStrip[i].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var15 + j * 4)));
                    }
                }

                IsoCell.this.hasSetupSnowGrid = true;
            }

            IsoCell.this.snowGridTilesSquare.resetCounter();
            IsoCell.this.snowGridTilesEnclosed.resetCounter();

            for (int i = 0; i < 4; i++) {
                IsoCell.this.snowGridTilesCove[i].resetCounter();
                IsoCell.this.snowGridTilesEdge[i].resetCounter();
                IsoCell.this.snowGridTilesStrip[i].resetCounter();
            }

            this.frac = frac;
            Noise2D noiseMain = IsoCell.this.snowNoise2d;

            for (int y = 0; y < this.h; y++) {
                for (int x = 0; x < this.w; x++) {
                    for (int s = 0; s < 2; s++) {
                        this.grid[x][y][s] = null;
                        this.gridType[x][y][s] = -1;
                    }

                    if (noiseMain.layeredNoise(x / 10.0F, y / 10.0F) <= frac / 100.0F) {
                        this.grid[x][y][0] = IsoCell.this.snowGridTilesSquare.getNext();
                        this.gridType[x][y][0] = IsoCell.this.snowGridTilesSquare.id;
                    }
                }
            }

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
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesStrip[0]);
                                } else if (bS) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesStrip[1]);
                                } else if (bE) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesStrip[3]);
                                } else if (bW) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesStrip[2]);
                                }
                            } else if (count == 2) {
                                if (bN && bS) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesStrip[0]);
                                    this.set(x, y, 1, IsoCell.this.snowGridTilesStrip[1]);
                                } else if (bE && bW) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesStrip[2]);
                                    this.set(x, y, 1, IsoCell.this.snowGridTilesStrip[3]);
                                } else if (bN) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesEdge[bW ? 0 : 3]);
                                } else if (bS) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesEdge[bW ? 2 : 1]);
                                } else if (bW) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesEdge[bN ? 0 : 2]);
                                } else if (bE) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesEdge[bN ? 3 : 1]);
                                }
                            } else if (count == 3) {
                                if (!bN) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesCove[1]);
                                } else if (!bS) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesCove[0]);
                                } else if (!bE) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesCove[2]);
                                } else if (!bW) {
                                    this.set(x, y, 0, IsoCell.this.snowGridTilesCove[3]);
                                }
                            } else if (count == 4) {
                                this.set(x, y, 0, IsoCell.this.snowGridTilesEnclosed);
                            }
                        }
                    }
                }
            }

            return this;
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
                return IsoCell.this.snowGridTilesSquare.contains(tex);
            } else {
                return false;
            }
        }

        public boolean checkAny(int x, int y) {
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
            } else {
                return y >= 0 && y < this.h ? this.grid[x][y][0] != null : false;
            }
        }

        public void set(int x, int y, int t, IsoCell.SnowGridTiles tiles) {
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
                    this.grid[x][y][t] = tiles.getNext();
                    this.gridType[x][y][t] = tiles.id;
                }
            }
        }

        public void subtract(IsoCell.SnowGrid other) {
            for (int y = 0; y < this.h; y++) {
                for (int x = 0; x < this.w; x++) {
                    for (int i = 0; i < 2; i++) {
                        if (other.gridType[x][y][i] == this.gridType[x][y][i]) {
                            this.grid[x][y][i] = null;
                            this.gridType[x][y][i] = -1;
                        }
                    }
                }
            }
        }
    }

    protected class SnowGridTiles {
        protected byte id;
        private int counter;
        private final ArrayList<Texture> textures;

        public SnowGridTiles(final byte id) {
            Objects.requireNonNull(IsoCell.this);
            super();
            this.id = -1;
            this.counter = -1;
            this.textures = new ArrayList<>();
            this.id = id;
        }

        protected void add(Texture tex) {
            this.textures.add(tex);
        }

        protected Texture getNext() {
            this.counter++;
            if (this.counter >= this.textures.size()) {
                this.counter = 0;
            }

            return this.textures.get(this.counter);
        }

        protected Texture get(int index) {
            return this.textures.get(index);
        }

        protected int size() {
            return this.textures.size();
        }

        protected Texture getRand() {
            return this.textures.get(Rand.Next(4));
        }

        protected boolean contains(Texture other) {
            return this.textures.contains(other);
        }

        protected void resetCounter() {
            this.counter = 0;
        }
    }

    public static class s_performance {
        static final PerformanceProfileProbe isoCellUpdate = new PerformanceProfileProbe("IsoCell.update");
        static final PerformanceProfileProbe isoCellRender = new PerformanceProfileProbe("IsoCell.render");
        public static final PerformanceProfileProbe isoCellRenderTiles = new PerformanceProfileProbe("IsoCell.renderTiles");
        static final PerformanceProfileProbe isoCellDoBuilding = new PerformanceProfileProbe("IsoCell.doBuilding");

        public static class renderTiles {
            public static final PerformanceProfileProbe performRenderTiles = new PerformanceProfileProbe("performRenderTiles");
            public static final PerformanceProfileProbe recalculateAnyGridStacks = new PerformanceProfileProbe("recalculateAnyGridStacks");
            public static final PerformanceProfileProbe flattenAnyFoliage = new PerformanceProfileProbe("flattenAnyFoliage");
            public static final PerformanceProfileProbe renderDebugPhysics = new PerformanceProfileProbe("renderDebugPhysics");
            public static final PerformanceProfileProbe renderDebugLighting = new PerformanceProfileProbe("renderDebugLighting");
            static PerformanceProfileProbeList<IsoCell.s_performance.renderTiles.PerformRenderTilesLayer> performRenderTilesLayers = PerformanceProfileProbeList.construct(
                "performRenderTiles",
                8,
                IsoCell.s_performance.renderTiles.PerformRenderTilesLayer.class,
                IsoCell.s_performance.renderTiles.PerformRenderTilesLayer::new
            );

            static class PerformRenderTilesLayer extends PerformanceProfileProbe {
                final PerformanceProfileProbe renderIsoWater = new PerformanceProfileProbe("renderIsoWater");
                final PerformanceProfileProbe renderFloor = new PerformanceProfileProbe("renderFloor");
                final PerformanceProfileProbe renderPuddles = new PerformanceProfileProbe("renderPuddles");
                final PerformanceProfileProbe renderShore = new PerformanceProfileProbe("renderShore");
                final PerformanceProfileProbe renderSnow = new PerformanceProfileProbe("renderSnow");
                final PerformanceProfileProbe renderBlood = new PerformanceProfileProbe("renderBlood");
                final PerformanceProfileProbe vegetationCorpses = new PerformanceProfileProbe("vegetationCorpses");
                final PerformanceProfileProbe renderFloorShading = new PerformanceProfileProbe("renderFloorShading");
                final PerformanceProfileProbe renderShadows = new PerformanceProfileProbe("renderShadows");
                final PerformanceProfileProbe luaOnPostFloorLayerDraw = new PerformanceProfileProbe("luaOnPostFloorLayerDraw");
                final PerformanceProfileProbe minusFloorCharacters = new PerformanceProfileProbe("minusFloorCharacters");

                PerformRenderTilesLayer(String title) {
                    super(title);
                }
            }
        }
    }
}
