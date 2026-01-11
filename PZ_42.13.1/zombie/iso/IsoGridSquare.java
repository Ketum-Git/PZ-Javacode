// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL20;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.VisibilityData;
import zombie.characters.animals.AnimalSoundState;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.entity.GameEntityFactory;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.SGlobalObjectSystem;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRainSplash;
import zombie.iso.objects.IsoRaindrop;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.utils.SquareCoord;
import zombie.iso.zones.Zone;
import zombie.meta.Meta;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.AddExplosiveTrapPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.RemoveItemFromSquarePacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.character.AnimalCommandPacket;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class IsoGridSquare {
    public static final boolean USE_WALL_SHADER = true;
    private static final int cutawayY = 0;
    private static final int cutawayNWWidth = 66;
    private static final int cutawayNWHeight = 226;
    private static final int cutawaySEXCut = 1084;
    private static final int cutawaySEXUncut = 1212;
    private static final int cutawaySEWidth = 6;
    private static final int cutawaySEHeight = 196;
    private static final int cutawayNXFullyCut = 700;
    private static final int cutawayNXCutW = 444;
    private static final int cutawayNXUncut = 828;
    private static final int cutawayNXCutE = 956;
    private static final int cutawayWXFullyCut = 512;
    private static final int cutawayWXCutS = 768;
    private static final int cutawayWXUncut = 896;
    private static final int cutawayWXCutN = 256;
    private static final int cutawayFenceXOffset = 1;
    private static final int cutawayLogWallXOffset = 1;
    private static final int cutawayMedicalCurtainWXOffset = -3;
    private static final int cutawayTentWallXOffset = -3;
    private static final int cutawaySpiffoWindowXOffset = -24;
    private static final int cutawayRoof4XOffset = -60;
    private static final int cutawayRoof17XOffset = -46;
    private static final int cutawayRoof28XOffset = -60;
    private static final int cutawayRoof41XOffset = -46;
    public static final int WALL_TYPE_N = 1;
    public static final int WALL_TYPE_S = 2;
    public static final int WALL_TYPE_W = 4;
    public static final int WALL_TYPE_E = 8;
    private static final int[] SURFACE_OFFSETS = new int[8];
    private static final long VisiFlagTimerPeriod_ms = 750L;
    public static final byte PCF_NONE = 0;
    public static final byte PCF_NORTH = 1;
    public static final byte PCF_WEST = 2;
    private static final ThreadLocal<ArrayList<Zone>> threadLocalZones = ThreadLocal.withInitial(ArrayList::new);
    public final IsoGridSquare.ILighting[] lighting = new IsoGridSquare.ILighting[4];
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempo2 = new Vector2();
    public static float rmod;
    public static float gmod;
    public static float bmod;
    public static int idMax = -1;
    private static int col = -1;
    private static int path = -1;
    private static int pathdoor = -1;
    private static int vision = -1;
    private static final String[] rainsplashCache = new String[50];
    public static boolean useSlowCollision;
    public BuildingDef associatedBuilding;
    private boolean hasTree;
    private ArrayList<Float> lightInfluenceB;
    private ArrayList<Float> lightInfluenceG;
    private ArrayList<Float> lightInfluenceR;
    private final IsoGridSquare[] nav = new IsoGridSquare[8];
    public int lightLevel;
    public int collideMatrix;
    public int pathMatrix;
    public int visionMatrix;
    public IsoRoom room;
    public IsoGridSquare w;
    public IsoGridSquare nw;
    public IsoGridSquare sw;
    public IsoGridSquare s;
    public IsoGridSquare n;
    public IsoGridSquare ne;
    public IsoGridSquare se;
    public IsoGridSquare e;
    public IsoGridSquare u;
    public IsoGridSquare d;
    public boolean haveSheetRope;
    private IWorldRegion isoWorldRegion;
    private boolean hasSetIsoWorldRegion;
    public int objectsSyncCount;
    public IsoBuilding roofHideBuilding;
    public boolean flattenGrassEtc;
    private final byte[] playerCutawayFlags = new byte[4];
    private final long[] playerCutawayFlagLockUntilTimes = new long[4];
    private final byte[] targetPlayerCutawayFlags = new byte[4];
    private final boolean[] playerIsDissolvedFlags = new boolean[4];
    private final long[] playerIsDissolvedFlagLockUntilTimes = new long[4];
    private final boolean[] targetPlayerIsDissolvedFlags = new boolean[4];
    private IsoWaterGeometry water;
    private IsoPuddlesGeometry puddles;
    private float puddlesCacheSize = -1.0F;
    private float puddlesCacheLevel = -1.0F;
    private final IsoGridSquare.WaterSplashData waterSplashData = new IsoGridSquare.WaterSplashData();
    private final ColorInfo[] lightInfo = new ColorInfo[4];
    private IsoRaindrop rainDrop;
    private IsoRainSplash rainSplash;
    private float splashX;
    private float splashY;
    private float splashFrame = -1.0F;
    private int splashFrameNum;
    private static final Texture[] waterSplashCache = new Texture[80];
    private static boolean isWaterSplashCacheInitialised;
    public static int gridSquareCacheEmptyTimer;
    private static float darkStep = 0.06F;
    public static float recalcLightTime;
    private static int lightcache;
    public boolean propertiesDirty = true;
    private static final ColorInfo defColorInfo = new ColorInfo();
    private static final ColorInfo blackColorInfo = new ColorInfo();
    private static int colu;
    private static int coll;
    private static int colr;
    private static int colu2;
    private static int coll2;
    private static int colr2;
    private static boolean doSlowPathfinding;
    public static boolean circleStencil;
    public long hashCodeObjects;
    public int x;
    public int y;
    public int z;
    private int cachedScreenValue = -1;
    public float cachedScreenX;
    public float cachedScreenY;
    private static long torchTimer;
    public boolean solidFloorCached;
    public boolean solidFloor;
    private boolean cacheIsFree;
    private boolean cachedIsFree;
    public IsoChunk chunk;
    public long roomId = -1L;
    public Integer id = -999;
    public Zone zone;
    private final ArrayList<IsoGameCharacter> deferedCharacters = new ArrayList<>();
    private int deferredCharacterTick = -1;
    private final ArrayList<IsoMovingObject> staticMovingObjects = new ArrayList<>(0);
    private final ArrayList<IsoMovingObject> movingObjects = new ArrayList<>(0);
    protected final PZArrayList<IsoObject> objects = new PZArrayList<>(IsoObject.class, 2);
    private final ArrayList<IsoWorldInventoryObject> worldObjects = new ArrayList<>();
    public long hasTypes;
    private final PropertyContainer properties = new PropertyContainer();
    private final ArrayList<IsoObject> specialObjects = new ArrayList<>(0);
    public boolean haveRoof;
    private boolean burntOut;
    private boolean hasFlies;
    private IBiome biome;
    private IsoGridOcclusionData occlusionDataCache;
    private static final PZArrayList<IsoWorldInventoryObject> tempWorldInventoryObjects = new PZArrayList<>(IsoWorldInventoryObject.class, 16);
    public static final ConcurrentLinkedQueue<IsoGridSquare> isoGridSquareCache = new ConcurrentLinkedQueue<>();
    public static ArrayDeque<IsoGridSquare> loadGridSquareCache;
    private boolean overlayDone;
    private KahluaTable table;
    private int trapPositionX = -1;
    private int trapPositionY = -1;
    private int trapPositionZ = -1;
    public static final ArrayList<String> ignoreBlockingSprites = new ArrayList<>();
    public static final ArrayList<IsoGridSquare> choices = new ArrayList<>();
    private static final ColorInfo lightInfoTemp = new ColorInfo();
    private static final float doorWindowCutawayLightMin = 0.3F;
    private static boolean wallCutawayW;
    private static boolean wallCutawayN;
    public boolean isSolidFloorCache;
    public boolean isExteriorCache;
    public boolean isVegitationCache;
    public int hourLastSeen = Integer.MIN_VALUE;
    private static IsoGridSquare lastLoaded;
    private static final Color tr = new Color(1, 1, 1, 1);
    private static final Color tl = new Color(1, 1, 1, 1);
    private static final Color br = new Color(1, 1, 1, 1);
    private static final Color bl = new Color(1, 1, 1, 1);
    private static final Color interp1 = new Color(1, 1, 1, 1);
    private static final Color interp2 = new Color(1, 1, 1, 1);
    private static final Color finalCol = new Color(1, 1, 1, 1);
    public static final IsoGridSquare.CellGetSquare cellGetSquare = new IsoGridSquare.CellGetSquare();
    private static final Comparator<IsoMovingObject> comp = (a, b) -> a.compareToY(b);
    public static boolean isOnScreenLast;
    private ErosionData.Square erosion;

    public SquareCoord getCoords() {
        return new SquareCoord(this.x, this.y, this.z);
    }

    public static boolean getMatrixBit(int matrix, int x, int y, int z) {
        return getMatrixBit(matrix, (byte)x, (byte)y, (byte)z);
    }

    public static boolean getMatrixBit(int matrix, byte x, byte y, byte z) {
        return (matrix >> x + y * 3 + z * 9 & 1) != 0;
    }

    public static int setMatrixBit(int matrix, int x, int y, int z, boolean val) {
        return setMatrixBit(matrix, (byte)x, (byte)y, (byte)z, val);
    }

    public static int setMatrixBit(int matrix, byte x, byte y, byte z, boolean val) {
        return val ? matrix | 1 << x + y * 3 + z * 9 : matrix & ~(1 << x + y * 3 + z * 9);
    }

    public int GetRLightLevel() {
        return (this.lightLevel & 0xFF0000) >> 16;
    }

    public int GetGLightLevel() {
        return (this.lightLevel & 0xFF00) >> 8;
    }

    public int GetBLightLevel() {
        return this.lightLevel & 0xFF;
    }

    public void SetRLightLevel(int val) {
        this.lightLevel = this.lightLevel & -16711681 | val << 16;
    }

    public void SetGLightLevel(int val) {
        this.lightLevel = this.lightLevel & -65281 | val << 8;
    }

    public void SetBLightLevel(int val) {
        this.lightLevel = this.lightLevel & -256 | val;
    }

    public void setPlayerCutawayFlag(int playerIndex, int flags, long currentTimeMillis) {
        this.targetPlayerCutawayFlags[playerIndex] = (byte)(flags & 3);
        if (currentTimeMillis > this.playerCutawayFlagLockUntilTimes[playerIndex]
            && this.playerCutawayFlags[playerIndex] != this.targetPlayerCutawayFlags[playerIndex]) {
            this.playerCutawayFlags[playerIndex] = this.targetPlayerCutawayFlags[playerIndex];
            this.playerCutawayFlagLockUntilTimes[playerIndex] = currentTimeMillis + 750L;
        }
    }

    public void addPlayerCutawayFlag(int playerIndex, int flag, long currentTimeMillis) {
        int flags = this.targetPlayerCutawayFlags[playerIndex] | flag;
        this.setPlayerCutawayFlag(playerIndex, flags, currentTimeMillis);
    }

    public void clearPlayerCutawayFlag(int playerIndex, int flag, long currentTimeMillis) {
        int flags = this.targetPlayerCutawayFlags[playerIndex] & ~flag;
        this.setPlayerCutawayFlag(playerIndex, flags, currentTimeMillis);
    }

    public int getPlayerCutawayFlag(int playerIndex, long currentTimeMillis) {
        if (PerformanceSettings.fboRenderChunk) {
            return this.targetPlayerCutawayFlags[playerIndex];
        } else {
            return currentTimeMillis > this.playerCutawayFlagLockUntilTimes[playerIndex]
                ? this.targetPlayerCutawayFlags[playerIndex]
                : this.playerCutawayFlags[playerIndex];
        }
    }

    public void setIsDissolved(int playerIndex, boolean bDissolved, long currentTimeMillis) {
        this.targetPlayerIsDissolvedFlags[playerIndex] = bDissolved;
        if (currentTimeMillis > this.playerIsDissolvedFlagLockUntilTimes[playerIndex]
            && this.playerIsDissolvedFlags[playerIndex] != this.targetPlayerIsDissolvedFlags[playerIndex]) {
            this.playerIsDissolvedFlags[playerIndex] = this.targetPlayerIsDissolvedFlags[playerIndex];
            this.playerIsDissolvedFlagLockUntilTimes[playerIndex] = currentTimeMillis + 750L;
        }
    }

    public boolean getIsDissolved(int playerIndex, long currentTimeMillis) {
        return currentTimeMillis > this.playerIsDissolvedFlagLockUntilTimes[playerIndex]
            ? this.targetPlayerIsDissolvedFlags[playerIndex]
            : this.playerIsDissolvedFlags[playerIndex];
    }

    public boolean hasWater() {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i).hasWater()) {
                return true;
            }
        }

        return false;
    }

    public IsoWaterGeometry getWater() {
        if (this.water != null && this.water.adjacentChunkLoadedCounter != this.chunk.adjacentChunkLoadedCounter) {
            this.water.adjacentChunkLoadedCounter = this.chunk.adjacentChunkLoadedCounter;
            if (this.water.hasWater || this.water.shore) {
                this.clearWater();
            }
        }

        if (this.water == null) {
            try {
                this.water = IsoWaterGeometry.pool.alloc();
                this.water.adjacentChunkLoadedCounter = this.chunk.adjacentChunkLoadedCounter;
                if (this.water.init(this) == null) {
                    this.clearWater();
                }
            } catch (Exception var2) {
                this.clearWater();
            }
        }

        return this.water;
    }

    public void clearWater() {
        if (this.water != null) {
            IsoWaterGeometry.pool.release(this.water);
            this.water = null;
        }
    }

    public IsoPuddlesGeometry getPuddles() {
        if (this.puddles == null) {
            try {
                synchronized (IsoPuddlesGeometry.pool) {
                    this.puddles = IsoPuddlesGeometry.pool.alloc();
                }

                this.puddles.square = this;
                this.puddles.recalc = true;
            } catch (Exception var4) {
                this.clearPuddles();
            }
        }

        return this.puddles;
    }

    public void clearPuddles() {
        if (this.puddles != null) {
            this.puddles.square = null;
            synchronized (IsoPuddlesGeometry.pool) {
                IsoPuddlesGeometry.pool.release(this.puddles);
            }

            this.puddles = null;
        }
    }

    public float getPuddlesInGround() {
        if (this.isInARoom()) {
            return -1.0F;
        } else {
            if (Math.abs(
                    IsoPuddles.getInstance().getPuddlesSize()
                        + Core.getInstance().getPerfPuddles()
                        + IsoCamera.frameState.offscreenWidth
                        - this.puddlesCacheSize
                )
                >= 0.01) {
                this.puddlesCacheSize = IsoPuddles.getInstance().getPuddlesSize() + Core.getInstance().getPerfPuddles() + IsoCamera.frameState.offscreenWidth;
                this.puddlesCacheLevel = IsoPuddlesCompute.computePuddle(this);
            }

            return this.puddlesCacheLevel;
        }
    }

    public void removeUnderground() {
        IsoObject[] elements = this.objects.getElements();

        for (int i = 0; i < elements.length; i++) {
            IsoObject element = elements[i];
            if (element != null && element.getTile() != null && element.getTile().startsWith("underground")) {
                this.getObjects().remove(element);
                return;
            }
        }
    }

    public boolean isInsideRectangle(int x, int y, int w, int h) {
        return this.x >= x && this.y >= y && this.x < x + w && this.y < y + h;
    }

    public IsoGridOcclusionData getOcclusionData() {
        return this.occlusionDataCache;
    }

    public IsoGridOcclusionData getOrCreateOcclusionData() {
        assert !GameServer.server;

        if (this.occlusionDataCache == null) {
            this.occlusionDataCache = new IsoGridOcclusionData(this);
        }

        return this.occlusionDataCache;
    }

    public void softClear() {
        this.zone = null;
        this.room = null;
        this.w = null;
        this.nw = null;
        this.sw = null;
        this.s = null;
        this.n = null;
        this.ne = null;
        this.se = null;
        this.e = null;
        this.u = null;
        this.d = null;
        this.isoWorldRegion = null;
        this.hasSetIsoWorldRegion = false;
        this.biome = null;

        for (int n = 0; n < 8; n++) {
            this.nav[n] = null;
        }
    }

    /**
     * Check if there's any object on this grid that has a sneak modifier, we use this to check if we reduce the chance of being spotted while crouching
     */
    public float getGridSneakModifier(boolean onlySolidTrans) {
        if (!onlySolidTrans) {
            if (this.properties.has("CloseSneakBonus")) {
                return Integer.parseInt(this.properties.get("CloseSneakBonus")) / 100.0F;
            }

            if (this.properties.has(IsoFlagType.collideN)
                || this.properties.has(IsoFlagType.collideW)
                || this.properties.has(IsoFlagType.WindowN)
                || this.properties.has(IsoFlagType.WindowW)
                || this.properties.has(IsoFlagType.doorN)
                || this.properties.has(IsoFlagType.doorW)) {
                return 8.0F;
            }
        } else if (this.properties.has(IsoFlagType.solidtrans)) {
            return 4.0F;
        }

        return 1.0F;
    }

    public boolean isSomethingTo(IsoGridSquare other) {
        return this.isWallTo(other) || this.isWindowTo(other) || this.isDoorTo(other);
    }

    public IsoObject getTransparentWallTo(IsoGridSquare other) {
        if (other == null || other == this || !this.isWallTo(other)) {
            return null;
        } else if (other.x > this.x && other.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !other.properties.has(IsoFlagType.WindowW)) {
            return other.getWall();
        } else if (this.x > other.x && this.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !this.properties.has(IsoFlagType.WindowW)) {
            return this.getWall();
        } else if (other.y > this.y && other.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !other.properties.has(IsoFlagType.WindowN)) {
            return other.getWall();
        } else if (this.y > other.y && this.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !this.properties.has(IsoFlagType.WindowN)) {
            return this.getWall();
        } else {
            if (other.x != this.x && other.y != this.y) {
                IsoObject wall1 = this.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z));
                IsoObject wall2 = this.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z));
                if (wall1 != null) {
                    return wall1;
                }

                if (wall2 != null) {
                    return wall2;
                }

                wall1 = other.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z));
                wall2 = other.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z));
                if (wall1 != null) {
                    return wall1;
                }

                if (wall2 != null) {
                    return wall2;
                }
            }

            return null;
        }
    }

    public boolean isWallTo(IsoGridSquare other) {
        if (other == null || other == this) {
            return false;
        } else if (other.x > this.x && other.properties.has(IsoFlagType.collideW) && !other.properties.has(IsoFlagType.WindowW)) {
            return true;
        } else if (this.x > other.x && this.properties.has(IsoFlagType.collideW) && !this.properties.has(IsoFlagType.WindowW)) {
            return true;
        } else if (other.y > this.y && other.properties.has(IsoFlagType.collideN) && !other.properties.has(IsoFlagType.WindowN)) {
            return true;
        } else if (this.y > other.y && this.properties.has(IsoFlagType.collideN) && !this.properties.has(IsoFlagType.WindowN)) {
            return true;
        } else {
            if (other.x != this.x && other.y != this.y) {
                if (this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), 1)
                    || this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), 1)) {
                    return true;
                }

                if (other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), 1)
                    || other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), 1)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isWallTo(IsoGridSquare other, int depth) {
        if (depth > 100) {
            boolean var3 = false;
        }

        if (other == null || other == this) {
            return false;
        } else if (other.x > this.x && other.properties.has(IsoFlagType.collideW) && !other.properties.has(IsoFlagType.WindowW)) {
            return true;
        } else if (this.x > other.x && this.properties.has(IsoFlagType.collideW) && !this.properties.has(IsoFlagType.WindowW)) {
            return true;
        } else if (other.y > this.y && other.properties.has(IsoFlagType.collideN) && !other.properties.has(IsoFlagType.WindowN)) {
            return true;
        } else if (this.y > other.y && this.properties.has(IsoFlagType.collideN) && !this.properties.has(IsoFlagType.WindowN)) {
            return true;
        } else {
            if (other.x != this.x && other.y != this.y) {
                if (this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), depth + 1)
                    || this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), depth + 1)) {
                    return true;
                }

                if (other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), depth + 1)
                    || other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), depth + 1)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isWindowTo(IsoGridSquare other) {
        if (other == null || other == this) {
            return false;
        } else if (other.x > this.x && other.properties.has(IsoFlagType.windowW)) {
            return true;
        } else if (this.x > other.x && this.properties.has(IsoFlagType.windowW)) {
            return true;
        } else if (other.y > this.y && other.properties.has(IsoFlagType.windowN)) {
            return true;
        } else if (this.y > other.y && this.properties.has(IsoFlagType.windowN)) {
            return true;
        } else {
            if (other.x != this.x && other.y != this.y) {
                if (this.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || this.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }

                if (other.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || other.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean haveDoor() {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i) instanceof IsoDoor) {
                return true;
            }
        }

        return false;
    }

    public boolean hasDoorOnEdge(IsoDirections edge, boolean ignoreOpen) {
        for (int i = 0; i < this.specialObjects.size(); i++) {
            IsoDoor door = Type.tryCastTo(this.specialObjects.get(i), IsoDoor.class);
            if (door != null && door.getSpriteEdge(ignoreOpen) == edge) {
                return true;
            }

            IsoThumpable thump = Type.tryCastTo(this.specialObjects.get(i), IsoThumpable.class);
            if (thump != null && thump.getSpriteEdge(ignoreOpen) == edge) {
                return true;
            }
        }

        return false;
    }

    public boolean hasClosedDoorOnEdge(IsoDirections edge) {
        boolean ignoreOpen = false;

        for (int i = 0; i < this.specialObjects.size(); i++) {
            IsoDoor door = Type.tryCastTo(this.specialObjects.get(i), IsoDoor.class);
            if (door != null && !door.IsOpen() && door.getSpriteEdge(false) == edge) {
                return true;
            }

            IsoThumpable thump = Type.tryCastTo(this.specialObjects.get(i), IsoThumpable.class);
            if (thump != null && !thump.IsOpen() && thump.getSpriteEdge(false) == edge) {
                return true;
            }
        }

        return false;
    }

    public boolean hasOpenDoorOnEdge(IsoDirections edge) {
        boolean ignoreOpen = false;

        for (int i = 0; i < this.specialObjects.size(); i++) {
            IsoDoor door = Type.tryCastTo(this.specialObjects.get(i), IsoDoor.class);
            if (door != null && door.IsOpen() && door.getSpriteEdge(false) == edge) {
                return true;
            }

            IsoThumpable thump = Type.tryCastTo(this.specialObjects.get(i), IsoThumpable.class);
            if (thump != null && thump.IsOpen() && thump.getSpriteEdge(false) == edge) {
                return true;
            }
        }

        return false;
    }

    public boolean isDoorTo(IsoGridSquare other) {
        if (other == null || other == this) {
            return false;
        } else if (other.x > this.x && other.properties.has(IsoFlagType.doorW)) {
            return true;
        } else if (this.x > other.x && this.properties.has(IsoFlagType.doorW)) {
            return true;
        } else if (other.y > this.y && other.properties.has(IsoFlagType.doorN)) {
            return true;
        } else if (this.y > other.y && this.properties.has(IsoFlagType.doorN)) {
            return true;
        } else {
            if (other.x != this.x && other.y != this.y) {
                if (this.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || this.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }

                if (other.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || other.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isBlockedTo(IsoGridSquare other) {
        return this.isWallTo(other) || this.isWindowBlockedTo(other) || this.isDoorBlockedTo(other) || this.isStairBlockedTo(other);
    }

    public boolean canReachTo(IsoGridSquare other) {
        if (other == this) {
            return true;
        } else if (Math.abs(other.x - this.x) > 1
            || Math.abs(other.y - this.y) > 1
            || other.z != this.z
            || this.isWindowBlockedTo(other)
            || this.isDoorBlockedTo(other)) {
            return false;
        } else if (other.y < this.y && this.getWallExcludingList(true, ignoreBlockingSprites) != null) {
            return false;
        } else if (this.y < other.y && other.getWallExcludingList(true, ignoreBlockingSprites) != null) {
            return false;
        } else if (other.x < this.x && this.getWallExcludingList(false, ignoreBlockingSprites) != null) {
            return false;
        } else if (this.x < other.x && other.getWallExcludingList(false, ignoreBlockingSprites) != null) {
            return false;
        } else if (this.x > other.x && this.HasStairTopWest()) {
            return false;
        } else {
            return this.y > other.y && this.HasStairTopNorth() ? false : !this.isWallTo(other);
        }
    }

    public boolean isWindowBlockedTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        } else if (other.x > this.x && other.hasBlockedWindow(false)) {
            return true;
        } else if (this.x > other.x && this.hasBlockedWindow(false)) {
            return true;
        } else if (other.y > this.y && other.hasBlockedWindow(true)) {
            return true;
        } else if (this.y > other.y && this.hasBlockedWindow(true)) {
            return true;
        } else {
            if (other.x != this.x && other.y != this.y) {
                if (this.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || this.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }

                if (other.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || other.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean hasBlockedWindow(boolean north) {
        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject o = this.objects.get(i);
            if (o instanceof IsoWindow w && w.getNorth() == north) {
                return !w.isDestroyed() && !w.IsOpen() || w.isBarricaded();
            }
        }

        return false;
    }

    public boolean isDoorBlockedTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        } else if (other.x > this.x && other.hasBlockedDoor(false)) {
            return true;
        } else if (this.x > other.x && this.hasBlockedDoor(false)) {
            return true;
        } else if (other.y > this.y && other.hasBlockedDoor(true)) {
            return true;
        } else if (this.y > other.y && this.hasBlockedDoor(true)) {
            return true;
        } else {
            if (other.x != this.x && other.y != this.y) {
                if (this.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || this.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }

                if (other.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z))
                    || other.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean hasBlockedDoor(boolean north) {
        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject o = this.objects.get(i);
            if (o instanceof IsoDoor d && d.getNorth() == north) {
                return !d.open || d.isBarricaded();
            }

            if (o instanceof IsoThumpable d && d.isDoor() && d.getNorth() == north) {
                return !d.open || d.isBarricaded();
            }
        }

        return false;
    }

    public IsoCurtain getCurtain(IsoObjectType curtainType) {
        for (int i = 0; i < this.getSpecialObjects().size(); i++) {
            IsoCurtain curtain = Type.tryCastTo(this.getSpecialObjects().get(i), IsoCurtain.class);
            if (curtain != null && curtain.getType() == curtainType) {
                return curtain;
            }
        }

        return null;
    }

    public IsoObject getHoppable(boolean north) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            PropertyContainer props = obj.getProperties();
            if (props != null && props.has(north ? IsoFlagType.HoppableN : IsoFlagType.HoppableW)) {
                return obj;
            }

            if (props != null && props.has(north ? IsoFlagType.WindowN : IsoFlagType.WindowW)) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getHoppableTo(IsoGridSquare next) {
        if (next != null && next != this) {
            if (next.x < this.x && next.y == this.y) {
                IsoObject obj = this.getHoppable(false);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x == this.x && next.y < this.y) {
                IsoObject obj = this.getHoppable(true);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x > this.x && next.y == this.y) {
                IsoObject obj = next.getHoppable(false);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x == this.x && next.y > this.y) {
                IsoObject obj = next.getHoppable(true);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                IsoObject obj = this.getHoppableTo(betweenA);
                if (obj != null) {
                    return obj;
                }

                obj = this.getHoppableTo(betweenB);
                if (obj != null) {
                    return obj;
                }

                obj = next.getHoppableTo(betweenA);
                if (obj != null) {
                    return obj;
                }

                obj = next.getHoppableTo(betweenB);
                if (obj != null) {
                    return obj;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean isHoppableTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        } else if (other.x != this.x && other.y != this.y) {
            return false;
        } else if (other.x > this.x && other.properties.has(IsoFlagType.HoppableW)) {
            return true;
        } else if (this.x > other.x && this.properties.has(IsoFlagType.HoppableW)) {
            return true;
        } else {
            return other.y > this.y && other.properties.has(IsoFlagType.HoppableN) ? true : this.y > other.y && this.properties.has(IsoFlagType.HoppableN);
        }
    }

    public IsoObject getBendable(boolean north) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (BentFences.getInstance().isUnbentObject(obj, north ? IsoDirections.N : IsoDirections.W)) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getBendableTo(IsoGridSquare next) {
        if (next != null && next != this) {
            if (next.x < this.x && next.y == this.y) {
                IsoObject obj = this.getBendable(false);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x == this.x && next.y < this.y) {
                IsoObject obj = this.getBendable(true);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x > this.x && next.y == this.y) {
                IsoObject obj = next.getBendable(false);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x == this.x && next.y > this.y) {
                IsoObject obj = next.getBendable(true);
                if (obj != null) {
                    return obj;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                IsoObject obj = this.getBendableTo(betweenA);
                if (obj != null) {
                    return obj;
                }

                obj = this.getBendableTo(betweenB);
                if (obj != null) {
                    return obj;
                }

                obj = next.getBendableTo(betweenA);
                if (obj != null) {
                    return obj;
                }

                obj = next.getBendableTo(betweenB);
                if (obj != null) {
                    return obj;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public void discard() {
        this.hourLastSeen = -32768;
        this.chunk = null;
        this.zone = null;
        this.lightInfluenceB = null;
        this.lightInfluenceG = null;
        this.lightInfluenceR = null;
        this.room = null;
        this.w = null;
        this.nw = null;
        this.sw = null;
        this.s = null;
        this.n = null;
        this.ne = null;
        this.se = null;
        this.e = null;
        this.u = null;
        this.d = null;
        this.isoWorldRegion = null;
        this.hasSetIsoWorldRegion = false;
        this.nav[0] = null;
        this.nav[1] = null;
        this.nav[2] = null;
        this.nav[3] = null;
        this.nav[4] = null;
        this.nav[5] = null;
        this.nav[6] = null;
        this.nav[7] = null;

        for (int n = 0; n < 4; n++) {
            if (this.lighting[n] != null) {
                this.lighting[n].reset();
            }

            this.lightInfo[n] = null;
        }

        this.solidFloorCached = false;
        this.solidFloor = false;
        this.cacheIsFree = false;
        this.cachedIsFree = false;
        this.chunk = null;
        this.roomId = -1L;
        this.deferedCharacters.clear();
        this.deferredCharacterTick = -1;
        this.staticMovingObjects.clear();
        this.movingObjects.clear();
        this.objects.clear();
        this.worldObjects.clear();
        this.hasTypes = 0L;
        this.table = null;
        this.properties.Clear();
        this.specialObjects.clear();
        this.rainDrop = null;
        this.rainSplash = null;
        this.overlayDone = false;
        this.haveRoof = false;
        this.burntOut = false;
        this.trapPositionX = this.trapPositionY = this.trapPositionZ = -1;
        this.haveSheetRope = false;
        if (this.erosion != null) {
            this.erosion.reset();
        }

        if (this.occlusionDataCache != null) {
            this.occlusionDataCache.Reset();
        }

        this.roofHideBuilding = null;
        this.hasFlies = false;
        Arrays.fill(this.playerCutawayFlags, (byte)0);
        Arrays.fill(this.playerCutawayFlagLockUntilTimes, 0L);
        Arrays.fill(this.targetPlayerCutawayFlags, (byte)0);
        isoGridSquareCache.add(this);
    }

    public float DistTo(int x, int y) {
        return IsoUtils.DistanceManhatten(x + 0.5F, y + 0.5F, this.x, this.y);
    }

    public float DistTo(IsoGridSquare sq) {
        return IsoUtils.DistanceManhatten(this.x + 0.5F, this.y + 0.5F, sq.x + 0.5F, sq.y + 0.5F);
    }

    public float DistToProper(int x, int y) {
        return IsoUtils.DistanceManhatten(x + 0.5F, y + 0.5F, this.x + 0.5F, this.y + 0.5F);
    }

    public float DistToProper(IsoGridSquare sq) {
        return IsoUtils.DistanceTo(this.x + 0.5F, this.y + 0.5F, sq.x + 0.5F, sq.y + 0.5F);
    }

    public float DistTo(IsoMovingObject other) {
        return IsoUtils.DistanceManhatten(this.x + 0.5F, this.y + 0.5F, other.getX(), other.getY());
    }

    public float DistToProper(IsoMovingObject other) {
        return IsoUtils.DistanceTo(this.x + 0.5F, this.y + 0.5F, other.getX(), other.getY());
    }

    public boolean isSafeToSpawn() {
        choices.clear();
        this.isSafeToSpawn(this, 0);
        if (choices.size() > 7) {
            choices.clear();
            return true;
        } else {
            choices.clear();
            return false;
        }
    }

    public void isSafeToSpawn(IsoGridSquare sq, int depth) {
        if (depth <= 5) {
            choices.add(sq);
            if (sq.n != null && !choices.contains(sq.n)) {
                this.isSafeToSpawn(sq.n, depth + 1);
            }

            if (sq.s != null && !choices.contains(sq.s)) {
                this.isSafeToSpawn(sq.s, depth + 1);
            }

            if (sq.e != null && !choices.contains(sq.e)) {
                this.isSafeToSpawn(sq.e, depth + 1);
            }

            if (sq.w != null && !choices.contains(sq.w)) {
                this.isSafeToSpawn(sq.w, depth + 1);
            }
        }
    }

    private void renderAttachedSpritesWithNoWallLighting(IsoObject obj, ColorInfo lightInfo, Consumer<TextureDraw> texdModifier) {
        if (obj.attachedAnimSprite != null && !obj.attachedAnimSprite.isEmpty()) {
            boolean needed = false;

            for (int i = 0; i < obj.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance s = obj.attachedAnimSprite.get(i);
                if (s.parentSprite != null && s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) {
                    needed = true;
                    break;
                }
            }

            if (needed) {
                defColorInfo.r = lightInfo.r;
                defColorInfo.g = lightInfo.g;
                defColorInfo.b = lightInfo.b;
                float fa = defColorInfo.a;
                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                    float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                    defColorInfo.set(defColorInfo.r * fade, defColorInfo.g * fade, defColorInfo.b * fade, defColorInfo.a);
                }

                if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                    defColorInfo.set(1.0F, 1.0F, 1.0F, defColorInfo.a);
                }

                if (circleStencil) {
                    IndieGL.enableStencilTest();
                    IndieGL.enableAlphaTest();
                    IndieGL.glAlphaFunc(516, 0.02F);
                    IndieGL.glStencilFunc(517, 128, 128);

                    for (int ix = 0; ix < obj.attachedAnimSprite.size(); ix++) {
                        IsoSpriteInstance s = obj.attachedAnimSprite.get(ix);
                        if (s.parentSprite != null && s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) {
                            defColorInfo.a = s.alpha;
                            s.render(
                                obj,
                                this.x,
                                this.y,
                                this.z,
                                obj.dir,
                                obj.offsetX,
                                obj.offsetY + obj.getRenderYOffset() * Core.tileScale,
                                defColorInfo,
                                true,
                                texdModifier
                            );
                        }
                    }

                    IndieGL.glStencilFunc(519, 255, 255);
                } else {
                    for (int ixx = 0; ixx < obj.attachedAnimSprite.size(); ixx++) {
                        IsoSpriteInstance s = obj.attachedAnimSprite.get(ixx);
                        if (s.parentSprite != null && s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) {
                            defColorInfo.a = s.alpha;
                            s.render(obj, this.x, this.y, this.z, obj.dir, obj.offsetX, obj.offsetY + obj.getRenderYOffset() * Core.tileScale, defColorInfo);
                            s.update();
                        }
                    }
                }

                defColorInfo.r = 1.0F;
                defColorInfo.g = 1.0F;
                defColorInfo.b = 1.0F;
                defColorInfo.a = fa;
            }
        }
    }

    public void DoCutawayShader(
        IsoObject obj,
        IsoDirections dir,
        int cutawaySelf,
        int cutawayN,
        int cutawayS,
        int cutawayW,
        int cutawayE,
        boolean bHasDoorN,
        boolean bHasDoorW,
        boolean bHasWindowN,
        boolean bHasWindowW,
        WallShaper texdModifier
    ) {
        Texture tex2 = Texture.getSharedTexture("media/wallcutaways.png", 3);
        if (tex2 != null && tex2.getID() != -1) {
            boolean NoWallLighting = obj.sprite.getProperties().has(IsoFlagType.NoWallLighting);
            int playerIndex = IsoCamera.frameState.playerIndex;
            obj.getRenderInfo(playerIndex).cutaway = true;
            IsoGridSquare squareN = this.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = this.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = this.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = this.getAdjacentSquare(IsoDirections.E);
            ColorInfo lightInfo = this.lightInfo[playerIndex];
            if (obj.getProperties().has("GarageDoor")) {
                obj.renderWallTileOnly(dir, this.x, this.y, this.z, NoWallLighting ? lightInfo : defColorInfo, null, texdModifier);
            }

            String CutawayHint = obj.getProperties().get("CutawayHint");

            try {
                String spriteName = obj.getSprite().getName();
                String tilesetName = obj.getSprite().tilesetName;
                int tileSheetIndex = obj.getSprite().tileSheetIndex;
                if (tilesetName == null) {
                    if (spriteName != null) {
                        int p = spriteName.lastIndexOf(95);
                        if (p != -1) {
                            tilesetName = spriteName.substring(0, p);
                            tileSheetIndex = PZMath.tryParseInt(spriteName.substring(p + 1), -1);
                        }
                    } else {
                        tilesetName = "";
                    }
                }

                IsoGridSquare.CircleStencilShader shader = IsoGridSquare.CircleStencilShader.instance;
                SpriteRenderer.WallShaderTexRender wallShaderTexRender = null;
                if (dir == IsoDirections.N || dir == IsoDirections.NW) {
                    int cutawayNX = 700;
                    if ((cutawaySelf & 1) != 0) {
                        if ((cutawayE & 1) == 0 && hasCutawayCapableWallNorth(squareE)) {
                            cutawayNX = 444;
                        }

                        if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                            cutawayNX = 956;
                        } else if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                            cutawayNX = 956;
                        }
                    } else if ((cutawayE & 1) == 0) {
                        cutawayNX = 828;
                    } else {
                        cutawayNX = 956;
                    }

                    int cutawayYAdjusted = 0;
                    if (!PerformanceSettings.fboRenderChunk || FBORenderCell.lowestCutawayObject == obj) {
                        if (bHasDoorN) {
                            cutawayYAdjusted = 904;
                            if (CutawayHint != null) {
                                if ("DoubleDoorLeft".equals(CutawayHint)) {
                                    cutawayYAdjusted = 1130;
                                } else if ("DoubleDoorRight".equals(CutawayHint)) {
                                    cutawayYAdjusted = 1356;
                                } else if ("GarageDoorLeft".equals(CutawayHint)) {
                                    cutawayNX = 444;
                                    cutawayYAdjusted = 1808;
                                } else if ("GarageDoorMiddle".equals(CutawayHint)) {
                                    cutawayNX = 572;
                                    cutawayYAdjusted = 1808;
                                } else if ("GarageDoorRight".equals(CutawayHint)) {
                                    cutawayNX = 700;
                                    cutawayYAdjusted = 1808;
                                }
                            }
                        } else if (bHasWindowN) {
                            cutawayYAdjusted = 226;
                            if (CutawayHint != null) {
                                if ("DoubleWindowLeft".equals(CutawayHint)) {
                                    cutawayYAdjusted = 678;
                                } else if ("DoubleWindowRight".equals(CutawayHint)) {
                                    cutawayYAdjusted = 452;
                                }
                            }
                        }
                    }

                    colu = this.getVertLight(0, playerIndex);
                    coll = this.getVertLight(1, playerIndex);
                    colu2 = this.getVertLight(4, playerIndex);
                    coll2 = this.getVertLight(5, playerIndex);
                    if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                        float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                        colu = Color.lerpABGR(colu, -16777216, fade);
                        coll = Color.lerpABGR(coll, -16777216, fade);
                        colu2 = Color.lerpABGR(colu2, -16777216, fade);
                        coll2 = Color.lerpABGR(coll2, -16777216, fade);
                    }

                    if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                        coll2 = -1;
                        colu2 = -1;
                        coll = -1;
                        colu = -1;
                        lightInfo = defColorInfo;
                    }

                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawayNX, cutawayYAdjusted, 66, 226);
                    if (dir == IsoDirections.N) {
                        Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWall);
                        SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                    }

                    if (dir == IsoDirections.NW) {
                        Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                        SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                    }

                    if (dir == IsoDirections.N) {
                        wallShaderTexRender = SpriteRenderer.WallShaderTexRender.All;
                    } else {
                        wallShaderTexRender = SpriteRenderer.WallShaderTexRender.RightOnly;
                    }

                    SpriteRenderer.instance.setExtraWallShaderParams(wallShaderTexRender);
                    texdModifier.col[0] = colu2;
                    texdModifier.col[1] = coll2;
                    texdModifier.col[2] = coll;
                    texdModifier.col[3] = colu;
                    if (!tex2.getTextureId().hasMipMaps()) {
                        IndieGL.glBlendFunc(770, 771);
                    }

                    obj.renderWallTileOnly(IsoDirections.Max, this.x, this.y, this.z, NoWallLighting ? lightInfo : defColorInfo, shader, texdModifier);
                    if (PerformanceSettings.fboRenderChunk) {
                        this.DoCutawayShaderAttached(
                            obj, dir, tex2, NoWallLighting, lightInfo, cutawayNX, cutawayYAdjusted, 66, 226, colu2, coll2, coll, colu, wallShaderTexRender
                        );
                    }

                    if (!tex2.getTextureId().hasMipMaps()) {
                        setBlendFunc();
                    }
                }

                if (dir == IsoDirections.W || dir == IsoDirections.NW) {
                    int cutawayWX = 512;
                    if ((cutawayS & 2) != 0) {
                        if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                            cutawayWX = 768;
                        } else if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                            cutawayWX = 768;
                        } else if ((cutawaySelf & 1) == 0 && hasCutawayCapableWallNorth(this)) {
                            cutawayWX = 768;
                        }
                    } else if ((cutawaySelf & 2) == 0) {
                        cutawayWX = 896;
                    } else if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                        cutawayWX = 768;
                    } else if (hasCutawayCapableWallWest(squareS)) {
                        cutawayWX = 256;
                    } else if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                        cutawayWX = 768;
                    }

                    int cutawayYAdjustedx = 0;
                    if (!PerformanceSettings.fboRenderChunk || FBORenderCell.lowestCutawayObject == obj) {
                        if (bHasDoorW) {
                            cutawayYAdjustedx = 904;
                            if (CutawayHint != null) {
                                if ("GarageDoorLeft".equals(CutawayHint)) {
                                    cutawayWX = 0;
                                    cutawayYAdjustedx = 1808;
                                } else if ("GarageDoorMiddle".equals(CutawayHint)) {
                                    cutawayWX = 128;
                                    cutawayYAdjustedx = 1808;
                                } else if ("GarageDoorRight".equals(CutawayHint)) {
                                    cutawayWX = 256;
                                    cutawayYAdjustedx = 1808;
                                } else if ("DoubleDoorLeft".equals(CutawayHint)) {
                                    cutawayYAdjustedx = 1356;
                                } else if ("DoubleDoorRight".equals(CutawayHint)) {
                                    cutawayYAdjustedx = 1130;
                                }
                            }
                        } else if (bHasWindowW) {
                            cutawayYAdjustedx = 226;
                            if (CutawayHint != null) {
                                if ("DoubleWindowLeft".equals(CutawayHint)) {
                                    cutawayYAdjustedx = 452;
                                } else if ("DoubleWindowRight".equals(CutawayHint)) {
                                    cutawayYAdjustedx = 678;
                                }
                            }
                        }
                    }

                    colu = this.getVertLight(0, playerIndex);
                    coll = this.getVertLight(3, playerIndex);
                    colu2 = this.getVertLight(4, playerIndex);
                    coll2 = this.getVertLight(7, playerIndex);
                    if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                        float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                        colu = Color.lerpABGR(colu, -16777216, fade);
                        coll = Color.lerpABGR(coll, -16777216, fade);
                        colu2 = Color.lerpABGR(colu2, -16777216, fade);
                        coll2 = Color.lerpABGR(coll2, -16777216, fade);
                    }

                    if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                        coll2 = -1;
                        colu2 = -1;
                        coll = -1;
                        colu = -1;
                        lightInfo = defColorInfo;
                    }

                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawayWX, cutawayYAdjustedx, 66, 226);
                    if (dir == IsoDirections.W) {
                        Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.WWall);
                        SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                    }

                    if (dir == IsoDirections.NW) {
                        Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                        SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                    }

                    if (dir == IsoDirections.W) {
                        wallShaderTexRender = SpriteRenderer.WallShaderTexRender.All;
                    } else {
                        wallShaderTexRender = SpriteRenderer.WallShaderTexRender.LeftOnly;
                    }

                    SpriteRenderer.instance.setExtraWallShaderParams(wallShaderTexRender);
                    texdModifier.col[0] = coll2;
                    texdModifier.col[1] = colu2;
                    texdModifier.col[2] = colu;
                    texdModifier.col[3] = coll;
                    if (!tex2.getTextureId().hasMipMaps()) {
                        IndieGL.glBlendFunc(770, 771);
                    }

                    obj.renderWallTileOnly(IsoDirections.Max, this.x, this.y, this.z, NoWallLighting ? lightInfo : defColorInfo, shader, texdModifier);
                    if (PerformanceSettings.fboRenderChunk) {
                        this.DoCutawayShaderAttached(
                            obj, dir, tex2, NoWallLighting, lightInfo, cutawayWX, cutawayYAdjustedx, 66, 226, coll2, colu2, colu, coll, wallShaderTexRender
                        );
                    }

                    if (!tex2.getTextureId().hasMipMaps()) {
                        setBlendFunc();
                    }
                }

                if (dir == IsoDirections.SE) {
                    int cutawaySEX = 1084;
                    if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                        cutawaySEX = 1212;
                    } else if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                        cutawaySEX = 1212;
                    }

                    int cutawayYAdjustedxx = 0;
                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEX, 0, 6, 196);
                    Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                    SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                    colu = this.getVertLight(0, playerIndex);
                    coll = this.getVertLight(3, playerIndex);
                    colu2 = this.getVertLight(4, playerIndex);
                    coll2 = this.getVertLight(7, playerIndex);
                    if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                        float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                        colu = Color.lerpABGR(colu, -16777216, fade);
                        coll = Color.lerpABGR(coll, -16777216, fade);
                        colu2 = Color.lerpABGR(colu2, -16777216, fade);
                        coll2 = Color.lerpABGR(coll2, -16777216, fade);
                    }

                    if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                        coll2 = -1;
                        colu2 = -1;
                        coll = -1;
                        colu = -1;
                        lightInfo = defColorInfo;
                    }

                    texdModifier.col[0] = coll2;
                    texdModifier.col[1] = colu2;
                    texdModifier.col[2] = colu;
                    texdModifier.col[3] = coll;
                    if (!tex2.getTextureId().hasMipMaps()) {
                        IndieGL.glBlendFunc(770, 771);
                    }

                    obj.renderWallTileOnly(IsoDirections.Max, this.x, this.y, this.z, NoWallLighting ? lightInfo : defColorInfo, shader, texdModifier);
                    if (PerformanceSettings.fboRenderChunk) {
                        this.DoCutawayShaderAttached(
                            obj, dir, tex2, NoWallLighting, lightInfo, cutawaySEX, 0, 66, 226, coll2, colu2, colu, coll, SpriteRenderer.WallShaderTexRender.All
                        );
                    }

                    if (!tex2.getTextureId().hasMipMaps()) {
                        setBlendFunc();
                    }
                }
            } finally {
                SpriteRenderer.instance.setExtraWallShaderParams(null);
                SpriteRenderer.instance.clearCutawayTexture();
                SpriteRenderer.instance.clearUseVertColorsArray();
            }

            if (!PerformanceSettings.fboRenderChunk) {
                obj.renderAttachedAndOverlaySprites(
                    obj.dir, this.x, this.y, this.z, NoWallLighting ? lightInfo : defColorInfo, false, !NoWallLighting, null, texdModifier
                );
            }
        }
    }

    private void DoCutawayShaderAttached(
        IsoObject obj,
        IsoDirections dir,
        Texture tex2,
        boolean NoWallLighting,
        ColorInfo lightInfo,
        int cutawayX,
        int cutawayY,
        int cutawayW,
        int cutawayH,
        int col0,
        int col1,
        int col2,
        int col3,
        SpriteRenderer.WallShaderTexRender wallShaderTexRender
    ) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.attachedSprites.getValue()) {
            if (SceneShaderStore.cutawayAttachedShader != null) {
                SpriteRenderer.instance.setExtraWallShaderParams(null);
                SpriteRenderer.instance.clearCutawayTexture();
                SpriteRenderer.instance.clearUseVertColorsArray();
                float farDepthZ = IsoDepthHelper.getSquareDepthData(
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), this.x, this.y, this.z
                    )
                    .depthStart;
                float zPlusOne = this.z + 1.0F;
                float frontDepthZ = IsoDepthHelper.getSquareDepthData(
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                        this.x + 1,
                        this.y + 1,
                        zPlusOne
                    )
                    .depthStart;
                if (!FBORenderCell.instance.renderTranslucentOnly) {
                    int CPW = 8;
                    IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                        PZMath.fastfloor(this.x / 8.0F),
                        PZMath.fastfloor(this.y / 8.0F),
                        PZMath.fastfloor((float)this.z)
                    );
                    float chunkDepth = result.depthStart;
                    farDepthZ -= chunkDepth;
                    frontDepthZ -= chunkDepth;
                }

                ShaderUniformSetter uniforms = ShaderUniformSetter.uniform1f(SceneShaderStore.cutawayAttachedShader, "zDepth", frontDepthZ);
                uniforms.setNext(ShaderUniformSetter.uniform1f(SceneShaderStore.cutawayAttachedShader, "zDepthBlendZ", frontDepthZ))
                    .setNext(ShaderUniformSetter.uniform1f(SceneShaderStore.cutawayAttachedShader, "zDepthBlendToZ", farDepthZ));
                IndieGL.pushShader(SceneShaderStore.cutawayAttachedShader, uniforms);
                CutawayAttachedModifier.instance.setVertColors(col0, col1, col2, col3);
                CutawayAttachedModifier.instance.setupWallDepth(obj.getSprite(), dir, tex2, cutawayX, cutawayY, cutawayW, cutawayH, wallShaderTexRender);
                IndieGL.glDefaultBlendFunc();
                obj.renderAttachedAndOverlaySprites(
                    IsoDirections.Max,
                    this.x,
                    this.y,
                    this.z,
                    NoWallLighting ? lightInfo : defColorInfo,
                    true,
                    !NoWallLighting,
                    null,
                    CutawayAttachedModifier.instance
                );
                this.renderAttachedSpritesWithNoWallLighting(obj, lightInfo, CutawayAttachedModifier.instance);
                IndieGL.popShader(SceneShaderStore.cutawayAttachedShader);
            }
        }
    }

    public void DoCutawayShaderSprite(IsoSprite sprite, IsoDirections dir, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE) {
        IsoGridSquare.CutawayNoDepthShader shader = IsoGridSquare.CutawayNoDepthShader.getInstance();
        WallShaperWhole texdModifier = WallShaperWhole.instance;
        int playerIndex = IsoCamera.frameState.playerIndex;
        Texture tex2 = Texture.getSharedTexture("media/wallcutaways.png", 3);
        if (tex2 != null && tex2.getID() != -1) {
            IsoGridSquare squareN = this.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = this.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = this.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = this.getAdjacentSquare(IsoDirections.E);
            int tileScale = 2 / Core.tileScale;

            try {
                Texture texture = sprite.getTextureForCurrentFrame(dir);
                if (texture != null) {
                    float XOffset = 0.0F;
                    float YOffset = texture.getOffsetY();
                    int WOffset = 0;
                    int HOffset = 226 - texture.getHeight() * tileScale;
                    if (dir != IsoDirections.NW) {
                        WOffset = 66 - texture.getWidth() * tileScale;
                    }

                    if (sprite.isWallSE()) {
                        WOffset = 6 - texture.getWidth() * tileScale;
                        HOffset = 196 - texture.getHeight() * tileScale;
                    }

                    if (sprite.name.contains("fencing_01_11")) {
                        XOffset = 1.0F;
                    } else if (sprite.name.contains("carpentry_02_80")) {
                        XOffset = 1.0F;
                    } else if (sprite.name.contains("spiffos_01_71")) {
                        XOffset = -24.0F;
                    } else if (sprite.name.contains("location_community_medical")) {
                        String spriteName = sprite.name.replaceAll("(.*)_", "");
                        int spriteID = Integer.parseInt(spriteName);
                        switch (spriteID) {
                            case 45:
                            case 46:
                            case 47:
                            case 147:
                            case 148:
                            case 149:
                                XOffset = -3.0F;
                        }
                    } else if (sprite.name.contains("walls_exterior_roofs")) {
                        String spriteName = sprite.name.replaceAll("(.*)_", "");
                        int spriteID = Integer.parseInt(spriteName);
                        if (spriteID == 4) {
                            XOffset = -60.0F;
                        } else if (spriteID == 17) {
                            XOffset = -46.0F;
                        } else if (spriteID == 28 && !sprite.name.contains("03")) {
                            XOffset = -60.0F;
                        } else if (spriteID == 41) {
                            XOffset = -46.0F;
                        }
                    }

                    if (dir == IsoDirections.N || dir == IsoDirections.NW) {
                        int cutawayNX = 700;
                        int cutawaySEX = 1084;
                        if ((cutawaySelf & 1) == 0) {
                            cutawayNX = 828;
                            cutawaySEX = 1212;
                        } else if ((cutawaySelf & 1) != 0) {
                            cutawaySEX = 1212;
                            if ((cutawayE & 1) == 0 && hasCutawayCapableWallNorth(squareE)) {
                                cutawayNX = 444;
                            }

                            if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                                cutawayNX = 956;
                            } else if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                                cutawayNX = 956;
                            }
                        } else if ((cutawayE & 1) == 0) {
                            cutawayNX = 828;
                        } else {
                            cutawayNX = 956;
                        }

                        colu = this.getVertLight(0, playerIndex);
                        coll = this.getVertLight(1, playerIndex);
                        colu2 = this.getVertLight(4, playerIndex);
                        coll2 = this.getVertLight(5, playerIndex);
                        if (sprite.isWallSE()) {
                            SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEX + (int)XOffset, 0 + (int)YOffset, 6 - WOffset, 196 - HOffset);
                        } else {
                            SpriteRenderer.instance.setCutawayTexture(tex2, cutawayNX + (int)XOffset, 0 + (int)YOffset, 66 - WOffset, 226 - HOffset);
                        }

                        if (dir == IsoDirections.N) {
                            SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                        } else {
                            SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.RightOnly);
                        }

                        texdModifier.col[0] = colu2;
                        texdModifier.col[1] = coll2;
                        texdModifier.col[2] = coll;
                        texdModifier.col[3] = colu;
                        IndieGL.bindShader(
                            shader,
                            sprite,
                            dir,
                            texdModifier,
                            (l_sprite, l_dir, l_texdModifier) -> l_sprite.render(
                                null, this.x, this.y, this.z, l_dir, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, l_texdModifier
                            )
                        );
                    }

                    if (dir == IsoDirections.W || dir == IsoDirections.NW) {
                        int cutawayWX = 512;
                        int cutawaySEXx = 1084;
                        if ((cutawaySelf & 2) == 0) {
                            cutawayWX = 896;
                            cutawaySEXx = 1212;
                        } else if ((cutawayS & 2) != 0) {
                            if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                                cutawayWX = 768;
                                cutawaySEXx = 1212;
                            } else if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                                cutawayWX = 768;
                                cutawaySEXx = 1212;
                            }
                        } else if ((cutawaySelf & 2) == 0) {
                            cutawayWX = 896;
                            cutawaySEXx = 1212;
                        } else if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                            cutawayWX = 768;
                        } else if (hasCutawayCapableWallWest(squareS)) {
                            cutawayWX = 256;
                        }

                        colu = this.getVertLight(0, playerIndex);
                        coll = this.getVertLight(3, playerIndex);
                        colu2 = this.getVertLight(4, playerIndex);
                        coll2 = this.getVertLight(7, playerIndex);
                        if (sprite.isWallSE()) {
                            SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEXx + (int)XOffset, 0 + (int)YOffset, 6 - WOffset, 196 - HOffset);
                        } else {
                            SpriteRenderer.instance.setCutawayTexture(tex2, cutawayWX + (int)XOffset, 0 + (int)YOffset, 66 - WOffset, 226 - HOffset);
                        }

                        if (dir == IsoDirections.W) {
                            Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.WWall);
                            SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                        }

                        if (dir == IsoDirections.NW) {
                            Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                            SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                        }

                        if (sprite.isWallSE()) {
                            Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                            SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                        }

                        if (dir == IsoDirections.W) {
                            SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                        } else {
                            SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.LeftOnly);
                        }

                        texdModifier.col[0] = coll2;
                        texdModifier.col[1] = colu2;
                        texdModifier.col[2] = colu;
                        texdModifier.col[3] = coll;
                        IndieGL.bindShader(
                            shader,
                            sprite,
                            dir,
                            texdModifier,
                            (l_sprite, l_dir, l_texdModifier) -> l_sprite.render(
                                null, this.x, this.y, this.z, l_dir, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, l_texdModifier
                            )
                        );
                    }

                    if (dir != IsoDirections.SE) {
                        return;
                    }

                    int cutawaySEXxx = 1084;
                    if ((cutawayW & 1) == 0 && hasCutawayCapableWallNorth(squareW)) {
                        cutawaySEXxx = 1212;
                    } else if ((cutawayN & 2) == 0 && hasCutawayCapableWallWest(squareN)) {
                        cutawaySEXxx = 1212;
                    }

                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEXxx + (int)XOffset, 0 + (int)YOffset, 6 - WOffset, 196 - HOffset);
                    Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                    SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                    colu = this.getVertLight(0, playerIndex);
                    coll = this.getVertLight(3, playerIndex);
                    colu2 = this.getVertLight(4, playerIndex);
                    coll2 = this.getVertLight(7, playerIndex);
                    texdModifier.col[0] = coll2;
                    texdModifier.col[1] = colu2;
                    texdModifier.col[2] = colu;
                    texdModifier.col[3] = coll;
                    IndieGL.bindShader(
                        shader,
                        sprite,
                        dir,
                        texdModifier,
                        (l_sprite, l_dir, l_texdModifier) -> l_sprite.render(
                            null, this.x, this.y, this.z, l_dir, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, l_texdModifier
                        )
                    );
                    return;
                }
            } finally {
                SpriteRenderer.instance.setExtraWallShaderParams(null);
                SpriteRenderer.instance.clearCutawayTexture();
                SpriteRenderer.instance.clearUseVertColorsArray();
            }
        }
    }

    public int DoWallLightingNW(
        IsoObject obj,
        int stenciled,
        int cutawaySelf,
        int cutawayN,
        int cutawayS,
        int cutawayW,
        int cutawayE,
        boolean bHasDoorN,
        boolean bHasDoorW,
        boolean bHasWindowN,
        boolean bHasWindowW,
        Shader wallRenderShader
    ) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.nw.getValue()) {
            return stenciled;
        } else {
            boolean isCutaway = cutawaySelf != 0 && DebugOptions.instance.terrain.renderTiles.cutaway.getValue();
            IsoDirections cutawayDirection = IsoDirections.NW;
            int playerIndex = IsoCamera.frameState.playerIndex;
            colu = this.getVertLight(0, playerIndex);
            coll = this.getVertLight(3, playerIndex);
            colr = this.getVertLight(1, playerIndex);
            colu2 = this.getVertLight(4, playerIndex);
            coll2 = this.getVertLight(7, playerIndex);
            colr2 = this.getVertLight(5, playerIndex);
            if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                colu = Color.lerpABGR(colu, -16777216, fade);
                coll = Color.lerpABGR(coll, -16777216, fade);
                colr = Color.lerpABGR(colr, -16777216, fade);
                colu2 = Color.lerpABGR(colu2, -16777216, fade);
                coll2 = Color.lerpABGR(coll2, -16777216, fade);
                colr2 = Color.lerpABGR(colr2, -16777216, fade);
            }

            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingDebug.getValue()) {
                colu = -65536;
                coll = -16711936;
                colr = -16711681;
                colu2 = -16776961;
                coll2 = -65281;
                colr2 = -256;
            }

            boolean circleStencil = IsoGridSquare.circleStencil;
            if (this.z != PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                circleStencil = false;
            }

            boolean isDoorN = obj.sprite.getType() == IsoObjectType.doorFrN || obj.sprite.getType() == IsoObjectType.doorN;
            boolean isDoorW = obj.sprite.getType() == IsoObjectType.doorFrW || obj.sprite.getType() == IsoObjectType.doorW;
            boolean isWindowN = false;
            boolean isWindowW = false;
            boolean noWallLighting = (isDoorN || isDoorW) && isCutaway || obj.sprite.getProperties().has(IsoFlagType.NoWallLighting);
            circleStencil = this.calculateWallAlphaAndCircleStencilCorner(
                obj, cutawaySelf, bHasDoorN, bHasDoorW, bHasWindowN, bHasWindowW, circleStencil, playerIndex, isDoorN, isDoorW, false, false
            );
            if (circleStencil && isCutaway) {
                this.DoCutawayShader(
                    obj,
                    cutawayDirection,
                    cutawaySelf,
                    cutawayN,
                    cutawayS,
                    cutawayW,
                    cutawayE,
                    bHasDoorN,
                    bHasDoorW,
                    bHasWindowN,
                    bHasWindowW,
                    WallShaperWhole.instance
                );
                wallCutawayN = true;
                wallCutawayW = true;
                return stenciled;
            } else {
                WallShaperWhole.instance.col[0] = colu2;
                WallShaperWhole.instance.col[1] = colr2;
                WallShaperWhole.instance.col[2] = colr;
                WallShaperWhole.instance.col[3] = colu;
                WallShaperN l_wallShaperN = WallShaperN.instance;
                l_wallShaperN.col[0] = colu2;
                l_wallShaperN.col[1] = colr2;
                l_wallShaperN.col[2] = colr;
                l_wallShaperN.col[3] = colu;
                TileSeamModifier.instance.setVertColors(colu2, colr2, colr, colu);
                stenciled = this.performDrawWall(obj, cutawayDirection, stenciled, playerIndex, noWallLighting, l_wallShaperN, wallRenderShader);
                WallShaperWhole.instance.col[0] = coll2;
                WallShaperWhole.instance.col[1] = colu2;
                WallShaperWhole.instance.col[2] = colu;
                WallShaperWhole.instance.col[3] = coll;
                WallShaperW l_wallShaperW = WallShaperW.instance;
                l_wallShaperW.col[0] = coll2;
                l_wallShaperW.col[1] = colu2;
                l_wallShaperW.col[2] = colu;
                l_wallShaperW.col[3] = coll;
                TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
                return this.performDrawWall(obj, cutawayDirection, stenciled, playerIndex, noWallLighting, l_wallShaperW, wallRenderShader);
            }
        }
    }

    public int DoWallLightingN(
        IsoObject obj,
        int stenciled,
        int cutawaySelf,
        int cutawayN,
        int cutawayS,
        int cutawayW,
        int cutawayE,
        boolean bHasDoorN,
        boolean bHasWindowN,
        Shader wallRenderShader
    ) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.n.getValue()) {
            return stenciled;
        } else {
            boolean bHasDoorW = false;
            boolean bHasWindowW = false;
            boolean hasNoDoor = !bHasDoorN;
            boolean hasNoWindow = !bHasWindowN;
            IsoObjectType doorFrType = IsoObjectType.doorFrN;
            IsoObjectType doorType = IsoObjectType.doorN;
            boolean isCutaway = (cutawaySelf & 1) != 0 && DebugOptions.instance.terrain.renderTiles.cutaway.getValue();
            IsoFlagType transparentFlag = IsoFlagType.transparentN;
            IsoFlagType transparentWindowFlag = IsoFlagType.WindowN;
            IsoFlagType hoppableType = IsoFlagType.HoppableN;
            IsoDirections cutawayDirection = IsoDirections.N;
            boolean circleStencil = IsoGridSquare.circleStencil;
            int playerIndex = IsoCamera.frameState.playerIndex;
            colu = this.getVertLight(0, playerIndex);
            coll = this.getVertLight(1, playerIndex);
            colu2 = this.getVertLight(4, playerIndex);
            coll2 = this.getVertLight(5, playerIndex);
            float r1 = Color.getRedChannelFromABGR(colu2);
            float g1 = Color.getGreenChannelFromABGR(colu2);
            float b1 = Color.getBlueChannelFromABGR(colu2);
            float r2 = Color.getRedChannelFromABGR(colu);
            float g2 = Color.getGreenChannelFromABGR(colu);
            float b2 = Color.getBlueChannelFromABGR(colu);
            float F = 0.045F;
            float r = PZMath.clamp(r2 * (r2 >= r1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            float g = PZMath.clamp(g2 * (g2 >= g1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            float b = PZMath.clamp(b2 * (b2 >= b1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            colu = Color.colorToABGR(r, g, b, 1.0F);
            r1 = Color.getRedChannelFromABGR(coll);
            g1 = Color.getGreenChannelFromABGR(coll);
            b1 = Color.getBlueChannelFromABGR(coll);
            r2 = Color.getRedChannelFromABGR(coll2);
            g2 = Color.getGreenChannelFromABGR(coll2);
            b2 = Color.getBlueChannelFromABGR(coll2);
            r = PZMath.clamp(r2 * (r2 >= r1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            g = PZMath.clamp(g2 * (g2 >= g1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            b = PZMath.clamp(b2 * (b2 >= b1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            coll2 = Color.colorToABGR(r, g, b, 1.0F);
            if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                colu = Color.lerpABGR(colu, -16777216, fade);
                coll = Color.lerpABGR(coll, -16777216, fade);
                colu2 = Color.lerpABGR(colu2, -16777216, fade);
                coll2 = Color.lerpABGR(coll2, -16777216, fade);
            }

            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingDebug.getValue()) {
                colu = -65536;
                coll = -16711936;
                colu2 = -16776961;
                coll2 = -65281;
            }

            WallShaperWhole l_wallShaperWhole = WallShaperWhole.instance;
            l_wallShaperWhole.col[0] = colu2;
            l_wallShaperWhole.col[1] = coll2;
            l_wallShaperWhole.col[2] = coll;
            l_wallShaperWhole.col[3] = colu;
            TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
            return this.performDrawWallSegmentSingle(
                obj,
                stenciled,
                cutawaySelf,
                cutawayN,
                cutawayS,
                cutawayW,
                cutawayE,
                false,
                false,
                bHasDoorN,
                bHasWindowN,
                hasNoDoor,
                hasNoWindow,
                doorFrType,
                doorType,
                isCutaway,
                transparentFlag,
                transparentWindowFlag,
                hoppableType,
                cutawayDirection,
                circleStencil,
                l_wallShaperWhole,
                wallRenderShader
            );
        }
    }

    public int DoWallLightingW(
        IsoObject obj,
        int stenciled,
        int cutawaySelf,
        int cutawayN,
        int cutawayS,
        int cutawayW,
        int cutawayE,
        boolean bHasDoorW,
        boolean bHasWindowW,
        Shader wallRenderShader
    ) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.w.getValue()) {
            return stenciled;
        } else {
            boolean bHasDoorN = false;
            boolean bHasWindowN = false;
            boolean hasNoDoor = !bHasDoorW;
            boolean hasNoWindow = !bHasWindowW;
            IsoObjectType doorFrType = IsoObjectType.doorFrW;
            IsoObjectType doorType = IsoObjectType.doorW;
            boolean isWallSE = obj.isWallSE();
            boolean isCutawaySE = false;
            if (isWallSE) {
                if ((cutawayW & 1) != 0) {
                    isCutawaySE = true;
                }

                if ((cutawayN & 2) != 0) {
                    isCutawaySE = true;
                }
            }

            boolean isCutaway = ((cutawaySelf & 2) != 0 || isCutawaySE) && DebugOptions.instance.terrain.renderTiles.cutaway.getValue();
            IsoFlagType transparentFlag = IsoFlagType.transparentW;
            IsoFlagType transparentWindowFlag = IsoFlagType.WindowW;
            IsoFlagType hoppableType = IsoFlagType.HoppableW;
            IsoDirections cutawayDirection = isWallSE ? IsoDirections.SE : IsoDirections.W;
            boolean circleStencil = IsoGridSquare.circleStencil;
            int playerIndex = IsoCamera.frameState.playerIndex;
            colu = this.getVertLight(0, playerIndex);
            coll = this.getVertLight(3, playerIndex);
            colu2 = this.getVertLight(4, playerIndex);
            coll2 = this.getVertLight(7, playerIndex);
            float r1 = Color.getRedChannelFromABGR(colu2);
            float g1 = Color.getGreenChannelFromABGR(colu2);
            float b1 = Color.getBlueChannelFromABGR(colu2);
            float r2 = Color.getRedChannelFromABGR(colu);
            float g2 = Color.getGreenChannelFromABGR(colu);
            float b2 = Color.getBlueChannelFromABGR(colu);
            float F = 0.045F;
            float r = PZMath.clamp(r2 * (r2 >= r1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            float g = PZMath.clamp(g2 * (g2 >= g1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            float b = PZMath.clamp(b2 * (b2 >= b1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            colu = Color.colorToABGR(r, g, b, 1.0F);
            r1 = Color.getRedChannelFromABGR(coll);
            g1 = Color.getGreenChannelFromABGR(coll);
            b1 = Color.getBlueChannelFromABGR(coll);
            r2 = Color.getRedChannelFromABGR(coll2);
            g2 = Color.getGreenChannelFromABGR(coll2);
            b2 = Color.getBlueChannelFromABGR(coll2);
            r = PZMath.clamp(r2 * (r2 >= r1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            g = PZMath.clamp(g2 * (g2 >= g1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            b = PZMath.clamp(b2 * (b2 >= b1 ? 1.045F : 0.955F), 0.0F, 1.0F);
            coll2 = Color.colorToABGR(r, g, b, 1.0F);
            if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                colu = Color.lerpABGR(colu, -16777216, fade);
                coll = Color.lerpABGR(coll, -16777216, fade);
                colu2 = Color.lerpABGR(colu2, -16777216, fade);
                coll2 = Color.lerpABGR(coll2, -16777216, fade);
            }

            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingDebug.getValue()) {
                colu = -65536;
                coll = -16711936;
                colu2 = -16776961;
                coll2 = -65281;
            }

            WallShaperWhole l_wallShaperWhole = WallShaperWhole.instance;
            l_wallShaperWhole.col[0] = coll2;
            l_wallShaperWhole.col[1] = colu2;
            l_wallShaperWhole.col[2] = colu;
            l_wallShaperWhole.col[3] = coll;
            TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
            return this.performDrawWallSegmentSingle(
                obj,
                stenciled,
                cutawaySelf,
                cutawayN,
                cutawayS,
                cutawayW,
                cutawayE,
                bHasDoorW,
                bHasWindowW,
                false,
                false,
                hasNoDoor,
                hasNoWindow,
                doorFrType,
                doorType,
                isCutaway,
                transparentFlag,
                transparentWindowFlag,
                hoppableType,
                cutawayDirection,
                circleStencil,
                l_wallShaperWhole,
                wallRenderShader
            );
        }
    }

    private int performDrawWallSegmentSingle(
        IsoObject obj,
        int stenciled,
        int cutawaySelf,
        int cutawayN,
        int cutawayS,
        int cutawayW,
        int cutawayE,
        boolean bHasDoorW,
        boolean bHasWindowW,
        boolean bHasDoorN,
        boolean bHasWindowN,
        boolean hasNoDoor,
        boolean hasNoWindow,
        IsoObjectType doorFrType,
        IsoObjectType doorType,
        boolean isCutaway,
        IsoFlagType transparentFlag,
        IsoFlagType transparentWindowFlag,
        IsoFlagType hoppableType,
        IsoDirections cutawayDirection,
        boolean circleStencil,
        WallShaperWhole texdModifier,
        Shader wallRenderShader
    ) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.z != PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
            circleStencil = false;
        }

        boolean isDoor = obj.sprite.getType() == doorFrType || obj.sprite.getType() == doorType;
        boolean isWindow = obj instanceof IsoWindow;
        boolean noWallLighting = (isDoor || isWindow) && isCutaway || obj.sprite.getProperties().has(IsoFlagType.NoWallLighting);
        circleStencil = this.calculateWallAlphaAndCircleStencilEdge(
            obj, hasNoDoor, hasNoWindow, isCutaway, transparentFlag, transparentWindowFlag, hoppableType, circleStencil, playerIndex, isDoor, isWindow
        );
        if (circleStencil && isCutaway) {
            this.DoCutawayShader(
                obj, cutawayDirection, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasDoorN, bHasDoorW, bHasWindowN, bHasWindowW, texdModifier
            );
            wallCutawayN = wallCutawayN | cutawayDirection == IsoDirections.N;
            wallCutawayW = wallCutawayW | cutawayDirection == IsoDirections.W;
            return stenciled;
        } else {
            return this.performDrawWall(obj, cutawayDirection, stenciled, playerIndex, noWallLighting, texdModifier, wallRenderShader);
        }
    }

    private int performDrawWallOnly(
        IsoObject obj, IsoDirections dir, int stenciled, int playerIndex, boolean noWallLighting, Consumer<TextureDraw> texdModifier, Shader wallRenderShader
    ) {
        IndieGL.enableAlphaTest();
        if (!noWallLighting) {
        }

        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.render.getValue()) {
            obj.renderWallTile(
                dir, this.x, this.y, this.z, noWallLighting ? lightInfoTemp : defColorInfo, true, !noWallLighting, wallRenderShader, texdModifier
            );
        }

        if (PerformanceSettings.fboRenderChunk && obj instanceof IsoWindow) {
            if (!obj.alphaForced && obj.isUpdateAlphaDuringRender()) {
                obj.updateAlpha(playerIndex);
            }

            return stenciled;
        } else {
            obj.setAlpha(playerIndex, 1.0F);
            return noWallLighting ? stenciled : stenciled + 1;
        }
    }

    private int performDrawWall(
        IsoObject obj, IsoDirections dir, int stenciled, int playerIndex, boolean noWallLighting, Consumer<TextureDraw> texdModifier, Shader wallRenderShader
    ) {
        lightInfoTemp.set(this.lightInfo[playerIndex]);
        if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
            obj.render(this.x, this.y, this.z, defColorInfo, true, !noWallLighting, null);
            return stenciled;
        } else {
            int stenciledResult = this.performDrawWallOnly(obj, dir, stenciled, playerIndex, noWallLighting, texdModifier, wallRenderShader);
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.attachedSprites.getValue()) {
                this.renderAttachedSpritesWithNoWallLighting(obj, lightInfoTemp, null);
            }

            return stenciledResult;
        }
    }

    private void calculateWallAlphaCommon(
        IsoObject obj, boolean isCutaway, boolean bHasDoor, boolean bHasWindow, int playerIndex, boolean isDoor, boolean isWindow
    ) {
        if (isDoor || isWindow) {
            if (!isWindow || !PerformanceSettings.fboRenderChunk) {
                if (isCutaway) {
                    obj.setAlpha(playerIndex, 0.4F);
                    obj.setTargetAlpha(playerIndex, 0.4F);
                    lightInfoTemp.r = Math.max(0.3F, lightInfoTemp.r);
                    lightInfoTemp.g = Math.max(0.3F, lightInfoTemp.g);
                    lightInfoTemp.b = Math.max(0.3F, lightInfoTemp.b);
                    if (isDoor && !bHasDoor) {
                        obj.setAlpha(playerIndex, 0.0F);
                        obj.setTargetAlpha(playerIndex, 0.0F);
                    }

                    if (isWindow && !bHasWindow) {
                        obj.setAlpha(playerIndex, 0.0F);
                        obj.setTargetAlpha(playerIndex, 0.0F);
                    }
                }
            }
        }
    }

    private boolean calculateWallAlphaAndCircleStencilEdge(
        IsoObject obj,
        boolean hasNoDoor,
        boolean hasNoWindow,
        boolean isCutaway,
        IsoFlagType transparentFlag,
        IsoFlagType transparentWindowFlag,
        IsoFlagType hoppableType,
        boolean circleStencil,
        int playerIndex,
        boolean isDoor,
        boolean isWindow
    ) {
        if (isDoor || isWindow) {
            if (!obj.sprite.getProperties().has("GarageDoor")) {
                circleStencil = false;
            }

            this.calculateWallAlphaCommon(obj, isCutaway, !hasNoDoor, !hasNoWindow, playerIndex, isDoor, isWindow);
        }

        if (circleStencil
            && (obj.sprite.getType() != IsoObjectType.wall || !obj.getProperties().has(transparentFlag) || !"walls_burnt_01".equals(obj.sprite.tilesetName))
            && obj.sprite.getType() == IsoObjectType.wall
            && obj.sprite.getProperties().has(transparentFlag)
            && !obj.getSprite().getProperties().has(IsoFlagType.exterior)
            && !obj.sprite.getProperties().has(transparentWindowFlag)) {
            circleStencil = false;
        }

        return circleStencil;
    }

    private boolean calculateWallAlphaAndCircleStencilCorner(
        IsoObject obj,
        int cutawaySelf,
        boolean bHasDoorN,
        boolean bHasDoorW,
        boolean bHasWindowN,
        boolean bHasWindowW,
        boolean circleStencil,
        int playerIndex,
        boolean isDoorN,
        boolean isDoorW,
        boolean isWindowN,
        boolean isWindowW
    ) {
        this.calculateWallAlphaCommon(obj, (cutawaySelf & 1) != 0, bHasDoorN, bHasWindowN, playerIndex, isDoorN, isWindowN);
        this.calculateWallAlphaCommon(obj, (cutawaySelf & 2) != 0, bHasDoorW, bHasWindowW, playerIndex, isDoorW, isWindowW);
        circleStencil = circleStencil && !isDoorN && !isWindowN;
        if (circleStencil
            && obj.sprite.getType() == IsoObjectType.wall
            && (obj.sprite.getProperties().has(IsoFlagType.transparentN) || obj.sprite.getProperties().has(IsoFlagType.transparentW))
            && !obj.getSprite().getProperties().has(IsoFlagType.exterior)
            && !obj.sprite.getProperties().has(IsoFlagType.WindowN)
            && !obj.sprite.getProperties().has(IsoFlagType.WindowW)) {
            circleStencil = false;
        }

        return circleStencil;
    }

    public KahluaTable getLuaMovingObjectList() {
        KahluaTable table = LuaManager.platform.newTable();
        LuaManager.env.rawset("Objects", table);

        for (int n = 0; n < this.movingObjects.size(); n++) {
            table.rawset(n + 1, this.movingObjects.get(n));
        }

        return table;
    }

    public boolean has(IsoFlagType flag) {
        return this.properties.has(flag);
    }

    public boolean has(String flag) {
        return this.properties.has(flag);
    }

    public boolean has(IsoObjectType type) {
        return this.has(type.index());
    }

    public boolean has(int type) {
        return (this.hasTypes & 1L << type) != 0L;
    }

    public void DeleteTileObject(IsoObject obj) {
        int index = this.objects.indexOf(obj);
        if (index != -1) {
            this.objects.remove(index);
            if (obj instanceof IsoTree tree) {
                obj.reset();
                CellLoader.isoTreeCache.push(tree);
            } else if (obj.getObjectName().equals("IsoObject")) {
                obj.reset();
                CellLoader.isoObjectCache.push(obj);
            }
        }
    }

    public KahluaTable getLuaTileObjectList() {
        KahluaTable table = LuaManager.platform.newTable();
        LuaManager.env.rawset("Objects", table);

        for (int n = 0; n < this.objects.size(); n++) {
            table.rawset(n + 1, this.objects.get(n));
        }

        return table;
    }

    boolean HasDoor(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            if (this.specialObjects.get(n) instanceof IsoDoor && ((IsoDoor)this.specialObjects.get(n)).north == north) {
                return true;
            }

            if (this.specialObjects.get(n) instanceof IsoThumpable
                && ((IsoThumpable)this.specialObjects.get(n)).isDoor()
                && ((IsoThumpable)this.specialObjects.get(n)).north == north) {
                return true;
            }
        }

        return false;
    }

    public boolean HasStairs() {
        return this.HasStairsNorth() || this.HasStairsWest();
    }

    public boolean HasStairsNorth() {
        return this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsBN);
    }

    public boolean HasStairsWest() {
        return this.has(IsoObjectType.stairsTW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsBW);
    }

    public boolean isStairBlockedTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        } else {
            return this.x > other.x && this.HasStairTopWest() ? true : this.y > other.y && this.HasStairTopNorth();
        }
    }

    public boolean HasStairTop() {
        return this.HasStairTopNorth() || this.HasStairTopWest();
    }

    public boolean HasStairTopNorth() {
        return this.has(IsoObjectType.stairsTN);
    }

    public boolean HasStairTopWest() {
        return this.has(IsoObjectType.stairsTW);
    }

    public boolean HasStairsBelow() {
        IsoGridSquare below = this.getCell().getGridSquare(this.x, this.y, this.z - 1);
        return below != null && below.HasStairs();
    }

    public boolean HasElevatedFloor() {
        return this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTW) || this.has(IsoObjectType.stairsMW);
    }

    public boolean isSameStaircase(int x, int y, int z) {
        if (z != this.getZ()) {
            return false;
        } else {
            int minX = this.getX();
            int minY = this.getY();
            int maxX = minX;
            int maxY = minY;
            if (this.has(IsoObjectType.stairsTN)) {
                maxY = minY + 2;
            } else if (this.has(IsoObjectType.stairsMN)) {
                minY--;
                maxY++;
            } else if (this.has(IsoObjectType.stairsBN)) {
                minY -= 2;
            } else if (this.has(IsoObjectType.stairsTW)) {
                maxX = minX + 2;
            } else if (this.has(IsoObjectType.stairsMW)) {
                minX--;
                maxX++;
            } else {
                if (!this.has(IsoObjectType.stairsBW)) {
                    return false;
                }

                minX -= 2;
            }

            if (x >= minX && y >= minY && x <= maxX && y <= maxY) {
                IsoGridSquare square = this.getCell().getGridSquare(x, y, z);
                return square != null && square.HasStairs();
            } else {
                return false;
            }
        }
    }

    public boolean hasRainBlockingTile() {
        return this.has(IsoFlagType.solidfloor) || this.has(IsoFlagType.BlockRain);
    }

    public boolean haveRoofFull() {
        if (this.haveRoof) {
            return true;
        } else if (this.chunk == null) {
            return false;
        } else {
            int zTest = 1;
            int CPW = 8;

            for (IsoGridSquare sq = this.chunk.getGridSquare(this.x % 8, this.y % 8, zTest);
                zTest <= this.chunk.getMaxLevel();
                sq = this.chunk.getGridSquare(this.x % 8, this.y % 8, ++zTest)
            ) {
                if (sq != null && sq.haveRoof) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean HasSlopedRoof() {
        return this.HasSlopedRoofWest() || this.HasSlopedRoofNorth();
    }

    public boolean HasSlopedRoofWest() {
        return this.has(IsoObjectType.WestRoofB) || this.has(IsoObjectType.WestRoofM) || this.has(IsoObjectType.WestRoofT);
    }

    public boolean HasSlopedRoofNorth() {
        return this.has(IsoObjectType.WestRoofB) || this.has(IsoObjectType.WestRoofM) || this.has(IsoObjectType.WestRoofT);
    }

    public boolean HasEave() {
        return this.getProperties().has(IsoFlagType.isEave);
    }

    public boolean HasTree() {
        return this.hasTree;
    }

    public IsoTree getTree() {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i) instanceof IsoTree tree) {
                return tree;
            }
        }

        return null;
    }

    public IsoObject getStump() {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i) != null && this.objects.get(i).isStump()) {
                return this.objects.get(i);
            }
        }

        return null;
    }

    public IsoObject getOre() {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i) != null && this.objects.get(i).isOre()) {
                return this.objects.get(i);
            }
        }

        return null;
    }

    public boolean hasBush() {
        return this.getBush() != null;
    }

    public IsoObject getBush() {
        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject object = this.objects.get(i);
            if (object.isBush()) {
                return object;
            }
        }

        return null;
    }

    public List<IsoObject> getBushes() {
        List<IsoObject> objects = new ArrayList<>();

        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject object = this.objects.get(i);
            if (object.isBush()) {
                objects.add(object);
            }
        }

        return objects;
    }

    public IsoObject getGrass() {
        for (int i = 0; i < this.objects.size(); i++) {
            String name = this.objects.get(i).getSprite().getName();
            if (name != null && (name.startsWith("e_newgrass_") || name.startsWith("blends_grassoverlays_"))) {
                return this.objects.get(i);
            }
        }

        return null;
    }

    public boolean hasGrassLike() {
        return !this.getGrassLike().isEmpty();
    }

    public List<IsoObject> getGrassLike() {
        List<IsoObject> objects = new ArrayList<>();

        for (int i = 0; i < this.objects.size(); i++) {
            String name = this.objects.get(i).getSprite().getName();
            if (name.startsWith("e_newgrass_")
                || name.startsWith("blends_grassoverlays_")
                || name.startsWith("d_plants_")
                || name.startsWith("d_generic_1_")
                || name.startsWith("d_floorleaves_")) {
                objects.add(this.objects.get(i));
            }
        }

        return objects;
    }

    private void fudgeShadowsToAlpha(IsoObject obj, Color colu2) {
        float invAlpha = 1.0F - obj.getAlpha();
        if (colu2.r < invAlpha) {
            colu2.r = invAlpha;
        }

        if (colu2.g < invAlpha) {
            colu2.g = invAlpha;
        }

        if (colu2.b < invAlpha) {
            colu2.b = invAlpha;
        }
    }

    public boolean shouldSave() {
        return !this.objects.isEmpty() || this.z == 0;
    }

    public void save(ByteBuffer output, ObjectOutputStream outputObj) throws IOException {
        this.save(output, outputObj, false);
    }

    public void save(ByteBuffer output, ObjectOutputStream outputObj, boolean IS_DEBUG_SAVE) throws IOException {
        this.getErosionData().save(output);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        int objSize = this.objects.size();
        if (!this.objects.isEmpty()) {
            header.addFlags(1);
            if (objSize == 2) {
                header.addFlags(2);
            } else if (objSize == 3) {
                header.addFlags(4);
            } else if (objSize >= 4) {
                header.addFlags(8);
            }

            if (IS_DEBUG_SAVE) {
                GameWindow.WriteString(output, "Number of objects (" + objSize + ")");
            }

            if (objSize >= 4) {
                output.putShort((short)this.objects.size());
            }

            for (int n = 0; n < this.objects.size(); n++) {
                int position1 = output.position();
                if (IS_DEBUG_SAVE) {
                    output.putInt(0);
                }

                byte flagsObj = 0;
                if (this.specialObjects.contains(this.objects.get(n))) {
                    flagsObj = (byte)(flagsObj | 2);
                }

                if (this.worldObjects.contains(this.objects.get(n))) {
                    flagsObj = (byte)(flagsObj | 4);
                }

                output.put(flagsObj);
                if (IS_DEBUG_SAVE) {
                    GameWindow.WriteStringUTF(output, this.objects.get(n).getClass().getName());
                }

                this.objects.get(n).save(output, IS_DEBUG_SAVE);
                if (IS_DEBUG_SAVE) {
                    int position2 = output.position();
                    output.position(position1);
                    output.putInt(position2 - position1);
                    output.position(position2);
                }
            }

            if (IS_DEBUG_SAVE) {
                output.put((byte)67);
                output.put((byte)82);
                output.put((byte)80);
                output.put((byte)83);
            }
        }

        if (this.isOverlayDone()) {
            header.addFlags(16);
        }

        if (this.haveRoof) {
            header.addFlags(32);
        }

        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        int bodyCount = 0;

        for (int n = 0; n < this.staticMovingObjects.size(); n++) {
            if (this.staticMovingObjects.get(n) instanceof IsoDeadBody) {
                bodyCount++;
            }
        }

        if (bodyCount > 0) {
            bits.addFlags(1);
            if (IS_DEBUG_SAVE) {
                GameWindow.WriteString(output, "Number of bodies");
            }

            output.putShort((short)bodyCount);

            for (int nx = 0; nx < this.staticMovingObjects.size(); nx++) {
                IsoMovingObject body = this.staticMovingObjects.get(nx);
                if (body instanceof IsoDeadBody) {
                    if (IS_DEBUG_SAVE) {
                        GameWindow.WriteStringUTF(output, body.getClass().getName());
                    }

                    body.save(output, IS_DEBUG_SAVE);
                }
            }
        }

        if (this.table != null && !this.table.isEmpty()) {
            bits.addFlags(2);
            this.table.save(output);
        }

        if (this.burntOut) {
            bits.addFlags(4);
        }

        if (this.getTrapPositionX() > 0) {
            bits.addFlags(8);
            output.putInt(this.getTrapPositionX());
            output.putInt(this.getTrapPositionY());
            output.putInt(this.getTrapPositionZ());
        }

        if (this.haveSheetRope) {
            bits.addFlags(16);
        }

        if (!bits.equals(0)) {
            header.addFlags(64);
            bits.write();
        } else {
            output.position(bits.getStartPosition());
        }

        int vis = 0;
        if (!GameClient.client && !GameServer.server) {
            for (int i = 0; i < 4; i++) {
                if (this.isSeen(i)) {
                    vis |= 1 << i;
                }
            }
        }

        output.put((byte)vis);
        header.write();
        header.release();
        bits.release();
    }

    static void loadmatrix(boolean[][][] matrix, DataInputStream input) throws IOException {
    }

    static void savematrix(boolean[][][] matrix, DataOutputStream output) throws IOException {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    output.writeBoolean(matrix[x][y][z]);
                }
            }
        }
    }

    public boolean isCommonGrass() {
        if (this.objects.isEmpty()) {
            return false;
        } else {
            IsoObject o = this.objects.get(0);
            return o.sprite.getProperties().has(IsoFlagType.solidfloor) && ("TileFloorExt_3".equals(o.getTile()) || "TileFloorExt_4".equals(o.getTile()));
        }
    }

    public static boolean toBoolean(byte[] data) {
        return data != null && data.length != 0 ? data[0] != 0 : false;
    }

    public void removeCorpse(IsoDeadBody body, boolean bRemote) {
        if (GameClient.client && !bRemote) {
            try {
                GameClient.instance.checkAddedRemovedItems(body);
            } catch (Exception var4) {
                GameClient.connection.cancelPacket();
                ExceptionLogger.logException(var4);
            }

            INetworkPacket.send(PacketTypes.PacketType.RemoveCorpseFromMap, body);
        }

        if (GameServer.server && !bRemote) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveCorpseFromMap, body.getX(), body.getY(), body);
        }

        body.invalidateRenderChunkLevel(130L);
        body.removeFromWorld();
        body.removeFromSquare();
        if (!GameServer.server) {
            LuaEventManager.triggerEvent("OnContainerUpdate", this);
        }
    }

    public IsoDeadBody getDeadBody() {
        for (int i = 0; i < this.staticMovingObjects.size(); i++) {
            if (this.staticMovingObjects.get(i) instanceof IsoDeadBody) {
                return (IsoDeadBody)this.staticMovingObjects.get(i);
            }
        }

        return null;
    }

    public List<IsoDeadBody> getDeadBodys() {
        List<IsoDeadBody> result = new ArrayList<>();

        for (int i = 0; i < this.staticMovingObjects.size(); i++) {
            if (this.staticMovingObjects.get(i) instanceof IsoDeadBody) {
                result.add((IsoDeadBody)this.staticMovingObjects.get(i));
            }
        }

        return result;
    }

    public void addCorpse(IsoDeadBody body, boolean bRemote) {
        if (GameClient.client && !bRemote) {
            AddCorpseToMapPacket packet = new AddCorpseToMapPacket();
            packet.set(this, body);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.AddCorpseToMap.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.AddCorpseToMap.send(GameClient.connection);
        }

        if (!this.staticMovingObjects.contains(body)) {
            this.staticMovingObjects.add(body);
        }

        body.addToWorld();
        this.burntOut = false;
        this.properties.unset(IsoFlagType.burntOut);
        body.invalidateRenderChunkLevel(66L);
    }

    public IsoBrokenGlass getBrokenGlass() {
        for (int i = 0; i < this.specialObjects.size(); i++) {
            IsoObject obj = this.specialObjects.get(i);
            if (obj instanceof IsoBrokenGlass isoBrokenGlass) {
                return isoBrokenGlass;
            }
        }

        return null;
    }

    public IsoBrokenGlass addBrokenGlass() {
        if (!this.isFree(false)) {
            return this.getBrokenGlass();
        } else {
            IsoBrokenGlass brokenGlass = this.getBrokenGlass();
            if (brokenGlass == null) {
                brokenGlass = new IsoBrokenGlass(this.getCell());
                brokenGlass.setSquare(this);
                this.AddSpecialObject(brokenGlass);
                if (GameServer.server) {
                    GameServer.transmitBrokenGlass(this);
                }

                if (GameClient.client) {
                    GameClient.sendBrokenGlass(this);
                }
            }

            return brokenGlass;
        }
    }

    public IsoFire getFire() {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i) instanceof IsoFire fire) {
                return fire;
            }
        }

        return null;
    }

    public IsoObject getHiddenStash() {
        for (int i = 0; i < this.specialObjects.size(); i++) {
            IsoObject obj = this.specialObjects.get(i);
            IsoSprite sprite = obj.getSprite();
            if (sprite != null && StringUtils.equalsIgnoreCase("floors_interior_tilesandwood_01_62", sprite.getName())) {
                return obj;
            }
        }

        return null;
    }

    public void load(ByteBuffer b, int WorldVersion) throws IOException {
        this.load(b, WorldVersion, false);
    }

    public void load(ByteBuffer b, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        this.getErosionData().load(b, WorldVersion);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, b);
        if (!header.equals(0)) {
            if (header.hasFlags(1)) {
                if (IS_DEBUG_SAVE) {
                    String str = GameWindow.ReadStringUTF(b);
                    DebugLog.log(str);
                }

                int objs = 1;
                if (header.hasFlags(2)) {
                    objs = 2;
                } else if (header.hasFlags(4)) {
                    objs = 3;
                } else if (header.hasFlags(8)) {
                    objs = b.getShort();
                }

                for (int n = 0; n < objs; n++) {
                    int position1 = b.position();
                    int size = 0;
                    if (IS_DEBUG_SAVE) {
                        size = b.getInt();
                    }

                    byte flagsObj = b.get();
                    boolean bSpecial = (flagsObj & 2) != 0;
                    boolean bWorld = (flagsObj & 4) != 0;
                    IsoObject obj = null;
                    if (IS_DEBUG_SAVE) {
                        String str = GameWindow.ReadStringUTF(b);
                        DebugLog.log(str);
                    }

                    obj = IsoObject.factoryFromFileInput(this.getCell(), b);
                    if (obj == null) {
                        if (IS_DEBUG_SAVE) {
                            int position2 = b.position();
                            if (position2 - position1 != size) {
                                DebugLog.log(
                                    "***** Object loaded size "
                                        + (position2 - position1)
                                        + " != saved size "
                                        + size
                                        + ", reading obj size: "
                                        + objs
                                        + ", Object == null"
                                );
                                if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                                    DebugLog.log("Obj sprite = " + obj.getSprite().getName());
                                }
                            }
                        }
                    } else {
                        obj.square = this;

                        try {
                            obj.load(b, WorldVersion, IS_DEBUG_SAVE);
                        } catch (Exception var19) {
                            this.debugPrintGridSquare();
                            if (lastLoaded != null) {
                                lastLoaded.debugPrintGridSquare();
                            }

                            throw new RuntimeException(var19);
                        }

                        if (IS_DEBUG_SAVE) {
                            int position2 = b.position();
                            if (position2 - position1 != size) {
                                DebugLog.log("***** Object loaded size " + (position2 - position1) + " != saved size " + size + ", reading obj size: " + objs);
                                if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                                    DebugLog.log("Obj sprite = " + obj.getSprite().getName());
                                }
                            }
                        }

                        if (obj instanceof IsoWorldInventoryObject worldItem) {
                            if (worldItem.getItem() == null) {
                                continue;
                            }

                            String type = worldItem.getItem().getFullType();
                            Item scriptItem = ScriptManager.instance.FindItem(type);
                            if (scriptItem != null && scriptItem.getObsolete()) {
                                continue;
                            }

                            String[] SplitString = type.split("_");
                            if ((
                                    worldItem.dropTime > -1.0
                                            && SandboxOptions.instance.hoursForWorldItemRemoval.getValue() > 0.0
                                            && (
                                                !SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue()
                                                        && SandboxOptions.instance.worldItemRemovalListContains(type)
                                                    || SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue()
                                                        && !SandboxOptions.instance.worldItemRemovalListContains(type)
                                            )
                                        || !SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue()
                                            && SandboxOptions.instance.worldItemRemovalListContains(SplitString[0])
                                        || SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue()
                                            && !SandboxOptions.instance.worldItemRemovalListContains(SplitString[0])
                                )
                                && !worldItem.isIgnoreRemoveSandbox()
                                && GameTime.instance.getWorldAgeHours() > worldItem.dropTime + SandboxOptions.instance.hoursForWorldItemRemoval.getValue()) {
                                continue;
                            }
                        }

                        if (!(obj instanceof IsoWindow)
                            || obj.getSprite() == null
                            || !"walls_special_01_8".equals(obj.getSprite().getName()) && !"walls_special_01_9".equals(obj.getSprite().getName())) {
                            this.objects.add(obj);
                            if (bSpecial) {
                                this.specialObjects.add(obj);
                            }

                            if (bWorld) {
                                if (Core.debug && !(obj instanceof IsoWorldInventoryObject)) {
                                    DebugLog.log(
                                        "Bitflags = "
                                            + flagsObj
                                            + ", obj name = "
                                            + obj.getObjectName()
                                            + ", sprite = "
                                            + (obj.getSprite() != null ? obj.getSprite().getName() : "unknown")
                                    );
                                }

                                this.worldObjects.add((IsoWorldInventoryObject)obj);
                            }
                        }
                    }
                }

                if (IS_DEBUG_SAVE) {
                    byte b1 = b.get();
                    byte b2 = b.get();
                    byte b3 = b.get();
                    byte b4 = b.get();
                    if (b1 != 67 || b2 != 82 || b3 != 80 || b4 != 83) {
                        DebugLog.log("***** Expected CRPS here");
                    }
                }
            }

            this.setOverlayDone(header.hasFlags(16));
            this.haveRoof = header.hasFlags(32);
            if (header.hasFlags(64)) {
                BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, b);
                if (bits.hasFlags(1)) {
                    if (IS_DEBUG_SAVE) {
                        String str = GameWindow.ReadStringUTF(b);
                        DebugLog.log(str);
                    }

                    int objsx = b.getShort();

                    for (int n = 0; n < objsx; n++) {
                        IsoMovingObject objx = null;
                        if (IS_DEBUG_SAVE) {
                            String str = GameWindow.ReadStringUTF(b);
                            DebugLog.log(str);
                        }

                        try {
                            objx = (IsoMovingObject)IsoObject.factoryFromFileInput(this.getCell(), b);
                        } catch (Exception var18) {
                            this.debugPrintGridSquare();
                            if (lastLoaded != null) {
                                lastLoaded.debugPrintGridSquare();
                            }

                            throw new RuntimeException(var18);
                        }

                        if (objx != null) {
                            objx.square = this;
                            objx.current = this;

                            try {
                                objx.load(b, WorldVersion, IS_DEBUG_SAVE);
                            } catch (Exception var17) {
                                this.debugPrintGridSquare();
                                if (lastLoaded != null) {
                                    lastLoaded.debugPrintGridSquare();
                                }

                                throw new RuntimeException(var17);
                            }

                            this.staticMovingObjects.add(objx);
                            this.recalcHashCodeObjects();
                        }
                    }
                }

                if (bits.hasFlags(2)) {
                    if (this.table == null) {
                        this.table = LuaManager.platform.newTable();
                    }

                    this.table.load(b, WorldVersion);
                }

                this.burntOut = bits.hasFlags(4);
                if (bits.hasFlags(8)) {
                    this.setTrapPositionX(b.getInt());
                    this.setTrapPositionY(b.getInt());
                    this.setTrapPositionZ(b.getInt());
                }

                this.haveSheetRope = bits.hasFlags(16);
                bits.release();
            }
        }

        header.release();
        byte vis = b.get();
        if (!GameClient.client && !GameServer.server) {
            for (int i = 0; i < 4; i++) {
                this.setIsSeen(i, (vis & 1 << i) != 0);
            }
        }

        lastLoaded = this;
    }

    private void debugPrintGridSquare() {
        System.out.println("x=" + this.x + " y=" + this.y + " z=" + this.z);
        System.out.println("objects");

        for (int n = 0; n < this.objects.size(); n++) {
            this.objects.get(n).debugPrintout();
        }

        System.out.println("staticmovingobjects");

        for (int n = 0; n < this.staticMovingObjects.size(); n++) {
            this.objects.get(n).debugPrintout();
        }
    }

    public float scoreAsWaypoint(int x, int y) {
        float score = 2.0F;
        return score - IsoUtils.DistanceManhatten(x, y, this.getX(), this.getY()) * 5.0F;
    }

    public void InvalidateSpecialObjectPaths() {
    }

    public boolean isSolid() {
        return this.properties.has(IsoFlagType.solid);
    }

    public boolean isSolidTrans() {
        return this.properties.has(IsoFlagType.solidtrans);
    }

    public boolean isFree(boolean bCountOtherCharacters) {
        if (bCountOtherCharacters && !this.movingObjects.isEmpty()) {
            return false;
        } else if (this.cachedIsFree) {
            return this.cacheIsFree;
        } else {
            this.cachedIsFree = true;
            this.cacheIsFree = true;
            if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans) || this.has(IsoObjectType.tree)) {
                this.cacheIsFree = false;
            }

            if (!this.properties.has(IsoFlagType.solidfloor)) {
                this.cacheIsFree = false;
            }

            if (this.has(IsoObjectType.stairsBN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTN)) {
                this.cacheIsFree = true;
            } else if (this.has(IsoObjectType.stairsBW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsTW)) {
                this.cacheIsFree = true;
            }

            return this.cacheIsFree;
        }
    }

    public boolean isFreeOrMidair(boolean bCountOtherCharacters) {
        if (bCountOtherCharacters && !this.movingObjects.isEmpty()) {
            return false;
        } else {
            boolean CacheIsFree = true;
            if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans) || this.has(IsoObjectType.tree)) {
                CacheIsFree = false;
            }

            if (this.has(IsoObjectType.stairsBN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTN)) {
                CacheIsFree = true;
            } else if (this.has(IsoObjectType.stairsBW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsTW)) {
                CacheIsFree = true;
            }

            return CacheIsFree;
        }
    }

    public boolean isFreeOrMidair(boolean bCountOtherCharacters, boolean bDoZombie) {
        if (bCountOtherCharacters && !this.movingObjects.isEmpty()) {
            if (!bDoZombie) {
                return false;
            }

            for (int i = 0; i < this.movingObjects.size(); i++) {
                IsoMovingObject object = this.movingObjects.get(i);
                if (!(object instanceof IsoDeadBody)) {
                    return false;
                }
            }
        }

        boolean CacheIsFree = true;
        if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans) || this.has(IsoObjectType.tree)) {
            CacheIsFree = false;
        }

        if (this.has(IsoObjectType.stairsBN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTN)) {
            CacheIsFree = true;
        } else if (this.has(IsoObjectType.stairsBW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsTW)) {
            CacheIsFree = true;
        }

        return CacheIsFree;
    }

    /**
     * Check if there's at least one solid floor around this tile, used to build wooden floor
     */
    public boolean connectedWithFloor() {
        if (this.getZ() == 0) {
            return true;
        } else {
            IsoGridSquare sq = null;
            sq = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ());
            if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
                return true;
            } else {
                sq = this.getCell().getGridSquare(this.getX() + 1, this.getY(), this.getZ());
                if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
                    return true;
                } else {
                    sq = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ());
                    if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
                        return true;
                    } else {
                        sq = this.getCell().getGridSquare(this.getX(), this.getY() + 1, this.getZ());
                        if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
                            return true;
                        } else {
                            sq = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ() - 1);
                            if (sq != null && sq.getSlopedSurfaceHeight(IsoDirections.S) == 1.0F) {
                                return true;
                            } else {
                                sq = this.getCell().getGridSquare(this.getX(), this.getY() + 1, this.getZ() - 1);
                                if (sq != null && sq.getSlopedSurfaceHeight(IsoDirections.N) == 1.0F) {
                                    return true;
                                } else {
                                    sq = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ() - 1);
                                    if (sq != null && sq.getSlopedSurfaceHeight(IsoDirections.E) == 1.0F) {
                                        return true;
                                    } else {
                                        sq = this.getCell().getGridSquare(this.getX() + 1, this.getY(), this.getZ() - 1);
                                        return sq != null && sq.getSlopedSurfaceHeight(IsoDirections.W) == 1.0F;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a tile has a solid floor, used to build stuff at z level > 0
     *  Also gonna check the tile "behind" the one w<e're trying to build something has a floor (only one is required)
     * 
     * @param north is the item we're trying to place facing north or not
     */
    public boolean hasFloor(boolean north) {
        if (this.properties.has(IsoFlagType.solidfloor)) {
            return true;
        } else {
            IsoGridSquare sq = null;
            if (north) {
                sq = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ());
            } else {
                sq = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ());
            }

            return sq != null && sq.properties.has(IsoFlagType.solidfloor);
        }
    }

    public boolean hasFloor() {
        return this.properties.has(IsoFlagType.solidfloor);
    }

    public boolean isNotBlocked(boolean bCountOtherCharacters) {
        if (!this.cachedIsFree) {
            this.cacheIsFree = true;
            this.cachedIsFree = true;
            if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans)) {
                this.cacheIsFree = false;
            }

            if (!this.properties.has(IsoFlagType.solidfloor)) {
                this.cacheIsFree = false;
            }
        } else if (!this.cacheIsFree) {
            return false;
        }

        return !bCountOtherCharacters || this.movingObjects.isEmpty();
    }

    public IsoObject getDoor(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable thump && thump.isDoor() && north == thump.north) {
                return thump;
            }

            if (special instanceof IsoDoor door && north == door.north) {
                return door;
            }
        }

        return null;
    }

    public IsoDoor getIsoDoor() {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoDoor isoDoor) {
                return isoDoor;
            }
        }

        return null;
    }

    /**
     * Get the door between this grid and the next in parameter
     */
    public IsoObject getDoorTo(IsoGridSquare next) {
        if (next != null && next != this) {
            IsoObject o = null;
            if (next.x < this.x) {
                o = this.getDoor(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y < this.y) {
                o = this.getDoor(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x > this.x) {
                o = next.getDoor(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y > this.y) {
                o = next.getDoor(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                o = this.getDoorTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = this.getDoorTo(betweenB);
                if (o != null) {
                    return o;
                }

                o = next.getDoorTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = next.getDoorTo(betweenB);
                if (o != null) {
                    return o;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public IsoWindow getWindow(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoWindow window && north == window.isNorth()) {
                return window;
            }
        }

        return null;
    }

    public IsoWindow getWindow() {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoWindow isoWindow) {
                return isoWindow;
            }
        }

        return null;
    }

    /**
     * Get the IsoWindow window between this grid and the next in parameter
     */
    public IsoWindow getWindowTo(IsoGridSquare next) {
        if (next != null && next != this) {
            IsoWindow o = null;
            if (next.x < this.x) {
                o = this.getWindow(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y < this.y) {
                o = this.getWindow(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x > this.x) {
                o = next.getWindow(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y > this.y) {
                o = next.getWindow(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                o = this.getWindowTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = this.getWindowTo(betweenB);
                if (o != null) {
                    return o;
                }

                o = next.getWindowTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = next.getWindowTo(betweenB);
                if (o != null) {
                    return o;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean isAdjacentToWindow() {
        if (this.getWindow() != null) {
            return true;
        } else if (this.hasWindowFrame()) {
            return true;
        } else if (this.getThumpableWindow(false) == null && this.getThumpableWindow(true) == null) {
            IsoGridSquare s = this.nav[IsoDirections.S.index()];
            if (s == null || s.getWindow(true) == null && s.getWindowFrame(true) == null && s.getThumpableWindow(true) == null) {
                IsoGridSquare e = this.nav[IsoDirections.E.index()];
                return e != null && (e.getWindow(false) != null || e.getWindowFrame(false) != null || e.getThumpableWindow(false) != null);
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public boolean isAdjacentToHoppable() {
        if (this.getHoppable(true) != null || this.getHoppable(false) != null) {
            return true;
        } else if (this.getHoppableThumpable(true) == null && this.getHoppableThumpable(false) == null) {
            IsoGridSquare s = this.nav[IsoDirections.S.index()];
            if (s == null || s.getHoppable(true) == null && s.getHoppableThumpable(true) == null) {
                IsoGridSquare e = this.nav[IsoDirections.E.index()];
                return e != null && (e.getHoppable(false) != null || e.getHoppableThumpable(false) != null);
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public IsoThumpable getThumpableWindow(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable thump && thump.isWindow() && north == thump.north) {
                return thump;
            }
        }

        return null;
    }

    /**
     * Get the IsoThumpable window between this grid and the next in parameter
     */
    public IsoThumpable getWindowThumpableTo(IsoGridSquare next) {
        if (next != null && next != this) {
            IsoThumpable o = null;
            if (next.x < this.x) {
                o = this.getThumpableWindow(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y < this.y) {
                o = this.getThumpableWindow(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x > this.x) {
                o = next.getThumpableWindow(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y > this.y) {
                o = next.getThumpableWindow(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                o = this.getWindowThumpableTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = this.getWindowThumpableTo(betweenB);
                if (o != null) {
                    return o;
                }

                o = next.getWindowThumpableTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = next.getWindowThumpableTo(betweenB);
                if (o != null) {
                    return o;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public IsoThumpable getThumpable(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable thump && north == thump.north) {
                return thump;
            }
        }

        return null;
    }

    public IsoThumpable getHoppableThumpable(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable thump && thump.isHoppable() && north == thump.north) {
                return thump;
            }
        }

        return null;
    }

    public IsoThumpable getHoppableThumpableTo(IsoGridSquare next) {
        if (next != null && next != this) {
            IsoThumpable o = null;
            if (next.x < this.x) {
                o = this.getHoppableThumpable(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y < this.y) {
                o = this.getHoppableThumpable(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x > this.x) {
                o = next.getHoppableThumpable(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y > this.y) {
                o = next.getHoppableThumpable(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                o = this.getHoppableThumpableTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = this.getHoppableThumpableTo(betweenB);
                if (o != null) {
                    return o;
                }

                o = next.getHoppableThumpableTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = next.getHoppableThumpableTo(betweenB);
                if (o != null) {
                    return o;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public IsoObject getWallHoppable(boolean north) {
        for (int i = 0; i < this.objects.size(); i++) {
            if (this.objects.get(i).isHoppable() && north == this.objects.get(i).isNorthHoppable()) {
                return this.objects.get(i);
            }
        }

        return null;
    }

    public IsoObject getWallHoppableTo(IsoGridSquare next) {
        if (next != null && next != this) {
            IsoObject o = null;
            if (next.x < this.x) {
                o = this.getWallHoppable(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y < this.y) {
                o = this.getWallHoppable(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x > this.x) {
                o = next.getWallHoppable(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y > this.y) {
                o = next.getWallHoppable(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                o = this.getWallHoppableTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = this.getWallHoppableTo(betweenB);
                if (o != null) {
                    return o;
                }

                o = next.getWallHoppableTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = next.getWallHoppableTo(betweenB);
                if (o != null) {
                    return o;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public IsoObject getBedTo(IsoGridSquare next) {
        ArrayList<IsoObject> special = null;
        if (next.y >= this.y && next.x >= this.x) {
            special = next.specialObjects;
        } else {
            special = this.specialObjects;
        }

        for (int n = 0; n < special.size(); n++) {
            IsoObject bed = special.get(n);
            if (bed.getProperties().has(IsoFlagType.bed)) {
                return bed;
            }
        }

        return null;
    }

    public IsoWindowFrame getWindowFrame(boolean north) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj instanceof IsoWindowFrame windowFrame && windowFrame.getNorth() == north) {
                return windowFrame;
            }
        }

        return null;
    }

    public IsoWindowFrame getWindowFrameTo(IsoGridSquare next) {
        if (next != null && next != this) {
            IsoWindowFrame o = null;
            if (next.x < this.x) {
                o = this.getWindowFrame(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y < this.y) {
                o = this.getWindowFrame(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x > this.x) {
                o = next.getWindowFrame(false);
                if (o != null) {
                    return o;
                }
            }

            if (next.y > this.y) {
                o = next.getWindowFrame(true);
                if (o != null) {
                    return o;
                }
            }

            if (next.x != this.x && next.y != this.y) {
                IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
                IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
                o = this.getWindowFrameTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = this.getWindowFrameTo(betweenB);
                if (o != null) {
                    return o;
                }

                o = next.getWindowFrameTo(betweenA);
                if (o != null) {
                    return o;
                }

                o = next.getWindowFrameTo(betweenB);
                if (o != null) {
                    return o;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean hasWindowFrame() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj instanceof IsoWindowFrame) {
                return true;
            }
        }

        return false;
    }

    public boolean hasWindowOrWindowFrame() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (!(obj instanceof IsoWorldInventoryObject) && (this.isWindowOrWindowFrame(obj, true) || this.isWindowOrWindowFrame(obj, false))) {
                return true;
            }
        }

        return false;
    }

    private IsoObject getSpecialWall(boolean north) {
        for (int n = this.specialObjects.size() - 1; n >= 0; n--) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable thump) {
                if (thump.isStairs()
                    || !thump.isThumpable() && !thump.isWindow() && !thump.isDoor()
                    || thump.isDoor() && thump.open
                    || thump.isBlockAllTheSquare()) {
                    continue;
                }

                if (north == thump.north && !thump.isCorner()) {
                    return thump;
                }
            }

            if (special instanceof IsoWindow window && north == window.isNorth()) {
                return window;
            }

            if (special instanceof IsoDoor door && north == door.north && !door.open) {
                return door;
            }
        }

        if ((!north || this.has(IsoFlagType.WindowN)) && (north || this.has(IsoFlagType.WindowW))) {
            IsoObject obj = this.getWindowFrame(north);
            return obj != null ? obj : null;
        } else {
            return null;
        }
    }

    public IsoObject getSheetRope() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj.sheetRope) {
                return obj;
            }
        }

        return null;
    }

    public boolean damageSpriteSheetRopeFromBottom(IsoPlayer player, boolean north) {
        IsoGridSquare sq = this;
        IsoFlagType type2;
        if (north) {
            if (this.has(IsoFlagType.climbSheetN)) {
                type2 = IsoFlagType.climbSheetN;
            } else {
                if (!this.has(IsoFlagType.climbSheetS)) {
                    return false;
                }

                type2 = IsoFlagType.climbSheetS;
            }
        } else if (this.has(IsoFlagType.climbSheetW)) {
            type2 = IsoFlagType.climbSheetW;
        } else {
            if (!this.has(IsoFlagType.climbSheetE)) {
                return false;
            }

            type2 = IsoFlagType.climbSheetE;
        }

        while (sq != null) {
            for (int i = 0; i < sq.getObjects().size(); i++) {
                IsoObject o = sq.getObjects().get(i);
                if (o.getProperties() != null && o.getProperties().has(type2)) {
                    int index = Integer.parseInt(o.getSprite().getName().split("_")[2]);
                    if (index > 14) {
                        return false;
                    }

                    String spriteName = o.getSprite().getName().split("_")[0] + "_" + o.getSprite().getName().split("_")[1];
                    index += 40;
                    o.setSprite(IsoSpriteManager.instance.getSprite(spriteName + "_" + index));
                    o.transmitUpdatedSprite();
                    break;
                }
            }

            if (sq.getZ() == 7) {
                break;
            }

            sq = sq.getCell().getGridSquare(sq.getX(), sq.getY(), sq.getZ() + 1);
        }

        return true;
    }

    public boolean removeSheetRopeFromBottom(IsoPlayer player, boolean north) {
        IsoGridSquare sq = this;
        IsoFlagType type1;
        IsoFlagType type2;
        if (north) {
            if (this.has(IsoFlagType.climbSheetN)) {
                type1 = IsoFlagType.climbSheetTopN;
                type2 = IsoFlagType.climbSheetN;
            } else {
                if (!this.has(IsoFlagType.climbSheetS)) {
                    return false;
                }

                type1 = IsoFlagType.climbSheetTopS;
                type2 = IsoFlagType.climbSheetS;
                String tile = "crafted_01_4";

                for (int i = 0; i < sq.getObjects().size(); i++) {
                    IsoObject o = sq.getObjects().get(i);
                    if (o.sprite != null && o.sprite.getName() != null && o.sprite.getName().equals(tile)) {
                        sq.transmitRemoveItemFromSquare(o);
                        break;
                    }
                }
            }
        } else if (this.has(IsoFlagType.climbSheetW)) {
            type1 = IsoFlagType.climbSheetTopW;
            type2 = IsoFlagType.climbSheetW;
        } else {
            if (!this.has(IsoFlagType.climbSheetE)) {
                return false;
            }

            type1 = IsoFlagType.climbSheetTopE;
            type2 = IsoFlagType.climbSheetE;
            String tile = "crafted_01_3";

            for (int ix = 0; ix < sq.getObjects().size(); ix++) {
                IsoObject o = sq.getObjects().get(ix);
                if (o.sprite != null && o.sprite.getName() != null && o.sprite.getName().equals(tile)) {
                    sq.transmitRemoveItemFromSquare(o);
                    break;
                }
            }
        }

        boolean find = false;

        IsoGridSquare previousSq;
        for (previousSq = null; sq != null; find = false) {
            for (int ixx = 0; ixx < sq.getObjects().size(); ixx++) {
                IsoObject o = sq.getObjects().get(ixx);
                if (o.getProperties() != null && (o.getProperties().has(type1) || o.getProperties().has(type2))) {
                    previousSq = sq;
                    find = true;
                    sq.transmitRemoveItemFromSquare(o);
                    if (GameServer.server) {
                        if (player != null) {
                            player.sendObjectChange("addItemOfType", "type", o.getName());
                        }
                    } else if (player != null) {
                        player.getInventory().AddItem(o.getName());
                    }
                    break;
                }
            }

            if (sq.getZ() == 7) {
                break;
            }

            sq = sq.getCell().getGridSquare(sq.getX(), sq.getY(), sq.getZ() + 1);
        }

        if (!find) {
            sq = previousSq.getCell().getGridSquare(previousSq.getX(), previousSq.getY(), previousSq.getZ());
            IsoGridSquare topSq = north ? sq.nav[IsoDirections.S.index()] : sq.nav[IsoDirections.E.index()];
            if (topSq == null) {
                return true;
            }

            for (int ixxx = 0; ixxx < topSq.getObjects().size(); ixxx++) {
                IsoObject o = topSq.getObjects().get(ixxx);
                if (o.getProperties() != null && (o.getProperties().has(type1) || o.getProperties().has(type2))) {
                    topSq.transmitRemoveItemFromSquare(o);
                    break;
                }
            }
        }

        return true;
    }

    private IsoObject getSpecialSolid() {
        for (int n = 0; n < this.specialObjects.size(); n++) {
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable thump && !thump.isStairs() && thump.isThumpable() && thump.isBlockAllTheSquare()) {
                if (!thump.getProperties().has(IsoFlagType.solidtrans) || !this.isAdjacentToWindow() && !this.isAdjacentToHoppable()) {
                    return thump;
                }

                return null;
            }
        }

        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject obj = this.objects.get(i);
            if (obj.isMovedThumpable()) {
                if (!this.isAdjacentToWindow() && !this.isAdjacentToHoppable()) {
                    return obj;
                }

                return null;
            }
        }

        return null;
    }

    public IsoObject testCollideSpecialObjects(IsoGridSquare next) {
        if (next == null || next == this) {
            return null;
        } else if (next.x < this.x && next.y == this.y) {
            if (next.z == this.z && this.has(IsoObjectType.stairsTW)) {
                return null;
            } else if (next.z == this.z && this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                return null;
            } else {
                IsoObject o = this.getSpecialWall(false);
                if (o != null) {
                    return o;
                } else if (this.isBlockedTo(next)) {
                    return null;
                } else {
                    o = next.getSpecialSolid();
                    return o != null ? o : null;
                }
            }
        } else if (next.x == this.x && next.y < this.y) {
            if (next.z == this.z && this.has(IsoObjectType.stairsTN)) {
                return null;
            } else if (next.z == this.z && this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                return null;
            } else {
                IsoObject o = this.getSpecialWall(true);
                if (o != null) {
                    return o;
                } else if (this.isBlockedTo(next)) {
                    return null;
                } else {
                    o = next.getSpecialSolid();
                    return o != null ? o : null;
                }
            }
        } else if (next.x > this.x && next.y == this.y) {
            IsoObject o = next.getSpecialWall(false);
            if (o != null) {
                return o;
            } else if (this.isBlockedTo(next)) {
                return null;
            } else {
                o = next.getSpecialSolid();
                return o != null ? o : null;
            }
        } else if (next.x == this.x && next.y > this.y) {
            IsoObject o = next.getSpecialWall(true);
            if (o != null) {
                return o;
            } else if (this.isBlockedTo(next)) {
                return null;
            } else {
                o = next.getSpecialSolid();
                return o != null ? o : null;
            }
        } else if (next.x < this.x && next.y < this.y) {
            IsoObject o = this.getSpecialWall(true);
            if (o != null) {
                return o;
            } else {
                o = this.getSpecialWall(false);
                if (o != null) {
                    return o;
                } else {
                    IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, this.y - 1, this.z);
                    if (betweenA != null && !this.isBlockedTo(betweenA)) {
                        o = betweenA.getSpecialSolid();
                        if (o != null) {
                            return o;
                        }

                        o = betweenA.getSpecialWall(false);
                        if (o != null) {
                            return o;
                        }
                    }

                    IsoGridSquare betweenB = this.getCell().getGridSquare(this.x - 1, this.y, this.z);
                    if (betweenB != null && !this.isBlockedTo(betweenB)) {
                        o = betweenB.getSpecialSolid();
                        if (o != null) {
                            return o;
                        }

                        o = betweenB.getSpecialWall(true);
                        if (o != null) {
                            return o;
                        }
                    }

                    if (betweenA == null || this.isBlockedTo(betweenA) || betweenB == null || this.isBlockedTo(betweenB)) {
                        return null;
                    } else if (!betweenA.isBlockedTo(next) && !betweenB.isBlockedTo(next)) {
                        o = next.getSpecialSolid();
                        return o != null ? o : null;
                    } else {
                        return null;
                    }
                }
            }
        } else if (next.x > this.x && next.y < this.y) {
            IsoObject o = this.getSpecialWall(true);
            if (o != null) {
                return o;
            } else {
                IsoGridSquare betweenAx = this.getCell().getGridSquare(this.x, this.y - 1, this.z);
                if (betweenAx != null && !this.isBlockedTo(betweenAx)) {
                    o = betweenAx.getSpecialSolid();
                    if (o != null) {
                        return o;
                    }
                }

                IsoGridSquare betweenBx = this.getCell().getGridSquare(this.x + 1, this.y, this.z);
                if (betweenBx != null) {
                    o = betweenBx.getSpecialWall(false);
                    if (o != null) {
                        return o;
                    }

                    if (!this.isBlockedTo(betweenBx)) {
                        o = betweenBx.getSpecialSolid();
                        if (o != null) {
                            return o;
                        }

                        o = betweenBx.getSpecialWall(true);
                        if (o != null) {
                            return o;
                        }
                    }
                }

                if (betweenAx != null && !this.isBlockedTo(betweenAx) && betweenBx != null && !this.isBlockedTo(betweenBx)) {
                    o = next.getSpecialWall(false);
                    if (o != null) {
                        return o;
                    } else if (!betweenAx.isBlockedTo(next) && !betweenBx.isBlockedTo(next)) {
                        o = next.getSpecialSolid();
                        return o != null ? o : null;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } else if (next.x > this.x && next.y > this.y) {
            IsoGridSquare betweenAxx = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
            if (betweenAxx != null) {
                IsoObject o = betweenAxx.getSpecialWall(true);
                if (o != null) {
                    return o;
                }

                if (!this.isBlockedTo(betweenAxx)) {
                    o = betweenAxx.getSpecialSolid();
                    if (o != null) {
                        return o;
                    }
                }
            }

            IsoGridSquare betweenBxx = this.getCell().getGridSquare(this.x + 1, this.y, this.z);
            if (betweenBxx != null) {
                IsoObject ox = betweenBxx.getSpecialWall(false);
                if (ox != null) {
                    return ox;
                }

                if (!this.isBlockedTo(betweenBxx)) {
                    ox = betweenBxx.getSpecialSolid();
                    if (ox != null) {
                        return ox;
                    }
                }
            }

            if (betweenAxx != null && !this.isBlockedTo(betweenAxx) && betweenBxx != null && !this.isBlockedTo(betweenBxx)) {
                IsoObject oxx = next.getSpecialWall(false);
                if (oxx != null) {
                    return oxx;
                } else {
                    oxx = next.getSpecialWall(true);
                    if (oxx != null) {
                        return oxx;
                    } else if (!betweenAxx.isBlockedTo(next) && !betweenBxx.isBlockedTo(next)) {
                        oxx = next.getSpecialSolid();
                        return oxx != null ? oxx : null;
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        } else if (next.x < this.x && next.y > this.y) {
            IsoObject oxx = this.getSpecialWall(false);
            if (oxx != null) {
                return oxx;
            } else {
                IsoGridSquare betweenAxxx = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
                if (betweenAxxx != null) {
                    oxx = betweenAxxx.getSpecialWall(true);
                    if (oxx != null) {
                        return oxx;
                    }

                    if (!this.isBlockedTo(betweenAxxx)) {
                        oxx = betweenAxxx.getSpecialSolid();
                        if (oxx != null) {
                            return oxx;
                        }
                    }
                }

                IsoGridSquare betweenBxxx = this.getCell().getGridSquare(this.x - 1, this.y, this.z);
                if (betweenBxxx != null && !this.isBlockedTo(betweenBxxx)) {
                    oxx = betweenBxxx.getSpecialSolid();
                    if (oxx != null) {
                        return oxx;
                    }
                }

                if (betweenAxxx != null && !this.isBlockedTo(betweenAxxx) && betweenBxxx != null && !this.isBlockedTo(betweenBxxx)) {
                    oxx = next.getSpecialWall(true);
                    if (oxx != null) {
                        return oxx;
                    } else if (!betweenAxxx.isBlockedTo(next) && !betweenBxxx.isBlockedTo(next)) {
                        oxx = next.getSpecialSolid();
                        return oxx != null ? oxx : null;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public IsoObject getDoorFrameTo(IsoGridSquare next) {
        ArrayList<IsoObject> special = null;
        if (next.y >= this.y && next.x >= this.x) {
            special = next.specialObjects;
        } else {
            special = this.specialObjects;
        }

        for (int n = 0; n < special.size(); n++) {
            if (special.get(n) instanceof IsoDoor door) {
                boolean no = door.north;
                if (no && next.y != this.y) {
                    return door;
                }

                if (!no && next.x != this.x) {
                    return door;
                }
            } else if (special.get(n) instanceof IsoThumpable door && ((IsoThumpable)special.get(n)).isDoor()) {
                boolean nox = door.north;
                if (nox && next.y != this.y) {
                    return door;
                }

                if (!nox && next.x != this.x) {
                    return door;
                }
            }
        }

        return null;
    }

    public static void getSquaresForThread(ArrayDeque<IsoGridSquare> isoGridSquareCacheDest, int count) {
        for (int xx = 0; xx < count; xx++) {
            IsoGridSquare sq = isoGridSquareCache.poll();
            if (sq == null) {
                isoGridSquareCacheDest.add(new IsoGridSquare(null, null, 0, 0, 0));
            } else {
                isoGridSquareCacheDest.add(sq);
            }
        }
    }

    public static IsoGridSquare getNew(IsoCell cell, SliceY slice, int x, int y, int z) {
        IsoGridSquare sq = isoGridSquareCache.poll();
        if (sq == null) {
            return new IsoGridSquare(cell, slice, x, y, z);
        } else {
            sq.x = x;
            sq.y = y;
            sq.z = z;
            sq.cachedScreenValue = -1;
            col = 0;
            path = 0;
            pathdoor = 0;
            vision = 0;
            sq.collideMatrix = 134217727;
            sq.pathMatrix = 134217727;
            sq.visionMatrix = 0;
            return sq;
        }
    }

    public static IsoGridSquare getNew(ArrayDeque<IsoGridSquare> isoGridSquareCache, IsoCell cell, SliceY slice, int x, int y, int z) {
        IsoGridSquare sq = null;
        if (isoGridSquareCache.isEmpty()) {
            return new IsoGridSquare(cell, slice, x, y, z);
        } else {
            sq = isoGridSquareCache.pop();
            sq.x = x;
            sq.y = y;
            sq.z = z;
            sq.cachedScreenValue = -1;
            col = 0;
            path = 0;
            pathdoor = 0;
            vision = 0;
            sq.collideMatrix = 134217727;
            sq.pathMatrix = 134217727;
            sq.visionMatrix = 0;
            return sq;
        }
    }

    @Deprecated
    public long getHashCodeObjects() {
        this.recalcHashCodeObjects();
        return this.hashCodeObjects;
    }

    @Deprecated
    public int getHashCodeObjectsInt() {
        this.recalcHashCodeObjects();
        return (int)this.hashCodeObjects;
    }

    @Deprecated
    public void recalcHashCodeObjects() {
        long h = 0L;
        this.hashCodeObjects = h;
    }

    @Deprecated
    public int hashCodeNoOverride() {
        int h = 0;
        this.recalcHashCodeObjects();
        h = h * 2 + this.objects.size();
        h = (int)(h + this.getHashCodeObjects());

        for (int n = 0; n < this.objects.size(); n++) {
            h = h * 2 + this.objects.get(n).hashCode();
        }

        int bodyCount = 0;

        for (int n = 0; n < this.staticMovingObjects.size(); n++) {
            if (this.staticMovingObjects.get(n) instanceof IsoDeadBody) {
                bodyCount++;
            }
        }

        h = h * 2 + bodyCount;

        for (int nx = 0; nx < this.staticMovingObjects.size(); nx++) {
            IsoMovingObject body = this.staticMovingObjects.get(nx);
            if (body instanceof IsoDeadBody) {
                h = h * 2 + body.hashCode();
            }
        }

        if (this.table != null && !this.table.isEmpty()) {
            h = h * 2 + this.table.hashCode();
        }

        byte flags = 0;
        if (this.isOverlayDone()) {
            flags = (byte)(flags | 1);
        }

        if (this.haveRoof) {
            flags = (byte)(flags | 2);
        }

        if (this.burntOut) {
            flags = (byte)(flags | 4);
        }

        h = h * 2 + flags;
        h = h * 2 + this.getErosionData().hashCode();
        if (this.getTrapPositionX() > 0) {
            h = h * 2 + this.getTrapPositionX();
            h = h * 2 + this.getTrapPositionY();
            h = h * 2 + this.getTrapPositionZ();
        }

        h = h * 2 + (this.haveElectricity() ? 1 : 0);
        return h * 2 + (this.haveSheetRope ? 1 : 0);
    }

    public IsoGridSquare(IsoCell cell, SliceY slice, int x, int y, int z) {
        this.id = ++idMax;
        this.x = x;
        this.y = y;
        this.z = z;
        this.cachedScreenValue = -1;
        col = 0;
        path = 0;
        pathdoor = 0;
        vision = 0;
        this.collideMatrix = 134217727;
        this.pathMatrix = 134217727;
        this.visionMatrix = 0;

        for (int i = 0; i < 4; i++) {
            if (GameServer.server) {
                if (i == 0) {
                    this.lighting[i] = new ServerLOS.ServerLighting();
                }
            } else if (LightingJNI.init) {
                this.lighting[i] = new LightingJNI.JNILighting(i, this);
            } else {
                this.lighting[i] = new IsoGridSquare.Lighting();
            }
        }
    }

    public IsoGridSquare getTileInDirection(IsoDirections directions) {
        if (directions == IsoDirections.N) {
            return this.getCell().getGridSquare(this.x, this.y - 1, this.z);
        } else if (directions == IsoDirections.NE) {
            return this.getCell().getGridSquare(this.x + 1, this.y - 1, this.z);
        } else if (directions == IsoDirections.NW) {
            return this.getCell().getGridSquare(this.x - 1, this.y - 1, this.z);
        } else if (directions == IsoDirections.E) {
            return this.getCell().getGridSquare(this.x + 1, this.y, this.z);
        } else if (directions == IsoDirections.W) {
            return this.getCell().getGridSquare(this.x - 1, this.y, this.z);
        } else if (directions == IsoDirections.SE) {
            return this.getCell().getGridSquare(this.x + 1, this.y + 1, this.z);
        } else if (directions == IsoDirections.SW) {
            return this.getCell().getGridSquare(this.x - 1, this.y + 1, this.z);
        } else {
            return directions == IsoDirections.S ? this.getCell().getGridSquare(this.x, this.y + 1, this.z) : null;
        }
    }

    public IsoObject getWall() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null && obj.sprite != null && (obj.sprite.cutW || obj.sprite.cutN)) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getThumpableWall(boolean bNorth) {
        IsoObject obj = this.getWall(bNorth);
        return obj != null && obj instanceof IsoThumpable ? obj : null;
    }

    public IsoObject getHoppableWall(boolean bNorth) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null && obj.sprite != null) {
                PropertyContainer properties = obj.getProperties();
                boolean bTallHoppableW = properties.has(IsoFlagType.TallHoppableW) && !properties.has(IsoFlagType.WallWTrans);
                boolean bTallHoppableN = properties.has(IsoFlagType.TallHoppableN) && !properties.has(IsoFlagType.WallNTrans);
                if (bTallHoppableW && !bNorth || bTallHoppableN && bNorth) {
                    return obj;
                }
            }
        }

        return null;
    }

    public IsoObject getThumpableWallOrHoppable(boolean bNorth) {
        IsoObject thumpableWall = this.getThumpableWall(bNorth);
        IsoObject hoppable = this.getHoppableWall(bNorth);
        if (thumpableWall != null && hoppable != null && thumpableWall == hoppable) {
            return thumpableWall;
        } else if (thumpableWall == null && hoppable != null) {
            return hoppable;
        } else {
            return thumpableWall != null && hoppable == null ? thumpableWall : null;
        }
    }

    public Boolean getWallFull() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null
                && obj.sprite != null
                && (
                    obj.sprite.cutN
                        || obj.sprite.cutW
                        || obj.sprite.getProperties().has(IsoFlagType.WallN)
                        || obj.sprite.getProperties().has(IsoFlagType.WallW)
                )) {
                return true;
            }
        }

        return false;
    }

    public boolean hasNonHoppableWall(boolean isNorth) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null && obj.sprite != null && !obj.isWallSE()) {
                if (isNorth) {
                    if ((obj.sprite.cutN || obj.sprite.getProperties().has(IsoFlagType.WallN) || obj.sprite.getProperties().has(IsoFlagType.WallNTrans))
                        && !obj.sprite.getProperties().has(IsoFlagType.HoppableN)
                        && !obj.sprite.getProperties().has(IsoFlagType.TallHoppableN)) {
                        return true;
                    }
                } else if ((obj.sprite.cutW || obj.sprite.getProperties().has(IsoFlagType.WallW) || obj.sprite.getProperties().has(IsoFlagType.WallWTrans))
                    && !obj.sprite.getProperties().has(IsoFlagType.HoppableW)
                    && !obj.sprite.getProperties().has(IsoFlagType.TallHoppableW)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isPlayerAbleToHopWallTo(IsoDirections dir, IsoGridSquare oppositeSq) {
        if (!this.isAdjacentTo(oppositeSq)) {
            return false;
        } else {
            if ((!this.HasStairs() || !this.has(IsoObjectType.stairsTN)) && !this.has(IsoObjectType.stairsTW)) {
                switch (dir) {
                    case N:
                        if (this.hasNonHoppableWall(true)) {
                            return false;
                        }
                        break;
                    case S:
                        if (oppositeSq.hasNonHoppableWall(true)) {
                            return false;
                        }
                        break;
                    case W:
                        if (this.hasNonHoppableWall(false)) {
                            return false;
                        }
                        break;
                    case E:
                        if (oppositeSq.hasNonHoppableWall(false)) {
                            return false;
                        }
                }
            } else if (this.getSquareAbove() != null && oppositeSq.getSquareAbove() != null) {
                switch (dir) {
                    case N:
                        if (this.getSquareAbove().hasNonHoppableWall(true)) {
                            return false;
                        }
                        break;
                    case S:
                        if (oppositeSq.getSquareAbove().hasNonHoppableWall(true)) {
                            return false;
                        }
                        break;
                    case W:
                        if (this.getSquareAbove().hasNonHoppableWall(false)) {
                            return false;
                        }
                        break;
                    case E:
                        if (oppositeSq.getSquareAbove().hasNonHoppableWall(false)) {
                            return false;
                        }
                }
            }

            return true;
        }
    }

    IsoObject getWallExcludingList(boolean bNorth, ArrayList<String> excluded) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null
                && obj.sprite != null
                && !excluded.contains(obj.sprite.name)
                && !obj.isWallSE()
                && (
                    bNorth && (obj.sprite.cutN || obj.sprite.getProperties().has(IsoFlagType.WallN))
                        || !bNorth && (obj.sprite.cutW || obj.sprite.getProperties().has(IsoFlagType.WallW))
                )) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getWallExcludingObject(boolean bNorth, IsoObject exclude) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null
                && obj.sprite != null
                && obj != exclude
                && !obj.isWallSE()
                && (
                    bNorth && (obj.sprite.cutN || obj.sprite.getProperties().has(IsoFlagType.WallN))
                        || !bNorth && (obj.sprite.cutW || obj.sprite.getProperties().has(IsoFlagType.WallW))
                )) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getWall(boolean bNorth) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null && obj.sprite != null && (obj.sprite.cutN && bNorth || obj.sprite.cutW && !bNorth)) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getWallSE() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null && obj.sprite != null && obj.isWallSE()) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getWallNW() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj != null && obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.WallNW)) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getGarageDoor(boolean bNorth) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (IsoDoor.getGarageDoorIndex(obj) != -1) {
                boolean bNorth2 = obj instanceof IsoDoor isoDoor ? isoDoor.getNorth() : ((IsoThumpable)obj).getNorth();
                if (bNorth == bNorth2) {
                    return obj;
                }
            }
        }

        return null;
    }

    public IsoObject getFloor() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.solidfloor)) {
                return obj;
            }
        }

        return null;
    }

    public IsoObject getPlayerBuiltFloor() {
        return this.getBuilding() == null && (this.roofHideBuilding == null || this.roofHideBuilding.isEntirelyEmptyOutside()) ? this.getFloor() : null;
    }

    public IsoObject getWaterObject() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.water)) {
                return obj;
            }
        }

        return null;
    }

    public void interpolateLight(ColorInfo inf, float x, float y) {
        IsoCell cell = this.getCell();
        if (x < 0.0F) {
            x = 0.0F;
        }

        if (x > 1.0F) {
            x = 1.0F;
        }

        if (y < 0.0F) {
            y = 0.0F;
        }

        if (y > 1.0F) {
            y = 1.0F;
        }

        int playerIndex = IsoCamera.frameState.playerIndex;
        int coltl = this.getVertLight(0, playerIndex);
        int coltr = this.getVertLight(1, playerIndex);
        int colbr = this.getVertLight(2, playerIndex);
        int colbl = this.getVertLight(3, playerIndex);
        Color.abgrToColor(coltl, tl);
        Color.abgrToColor(colbl, bl);
        Color.abgrToColor(coltr, tr);
        Color.abgrToColor(colbr, br);
        tl.interp(tr, x, interp1);
        bl.interp(br, x, interp2);
        interp1.interp(interp2, y, finalCol);
        inf.r = finalCol.r;
        inf.g = finalCol.g;
        inf.b = finalCol.b;
        inf.a = finalCol.a;
    }

    public void EnsureSurroundNotNull() {
        assert !GameServer.server;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if ((x != 0 || y != 0)
                    && IsoWorld.instance.isValidSquare(this.x + x, this.y + y, this.z)
                    && this.getCell().getChunkForGridSquare(this.x + x, this.y + y, this.z) != null) {
                    boolean created = false;
                    IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z);
                    if (sq == null) {
                        sq = getNew(this.getCell(), null, this.x + x, this.y + y, this.z);
                        IsoGridSquare newSq = this.getCell().ConnectNewSquare(sq, false);
                        created = true;
                    }

                    if (created && sq.z < 0) {
                        sq.addUndergroundBlock("underground_01_0");
                    }
                }
            }
        }
    }

    public void setSquareChanged() {
        this.setCachedIsFree(false);
        PolygonalMap2.instance.squareChanged(this);
        IsoGridOcclusionData.SquareChanged();
        IsoRegions.squareChanged(this);
    }

    public IsoObject addFloor(String sprite) {
        IsoRegions.setPreviousFlags(this);
        IsoObject obj = new IsoObject(this.getCell(), this, sprite);
        boolean hasRug = false;

        for (int nn = 0; nn < this.getObjects().size(); nn++) {
            IsoObject o = this.getObjects().get(nn);
            IsoSprite ss = o.sprite;
            if (ss != null
                && (
                    ss.getProperties().has(IsoFlagType.solidfloor)
                        || ss.getProperties().has(IsoFlagType.noStart)
                        || ss.getProperties().has(IsoFlagType.vegitation) && o.getType() != IsoObjectType.tree
                        || ss.getProperties().has(IsoFlagType.taintedWater)
                        || ss.getName() != null && ss.getName().startsWith("blends_grassoverlays")
                )) {
                if (ss.getName() != null && ss.getName().startsWith("floors_rugs")) {
                    hasRug = true;
                } else {
                    this.transmitRemoveItemFromSquare(o);
                    nn--;
                }
            }
        }

        obj.sprite.getProperties().set(IsoFlagType.solidfloor);
        if (hasRug) {
            this.getObjects().add(0, obj);
        } else {
            this.getObjects().add(obj);
        }

        this.EnsureSurroundNotNull();
        this.RecalcProperties();
        DesignationZoneAnimal.addNewRoof(this.x, this.y, this.z);
        this.getCell().checkHaveRoof(this.x, this.y);

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            LosUtil.cachecleared[pn] = true;
        }

        setRecalcLightTime(-1.0F);
        if (PerformanceSettings.fboRenderChunk) {
            Core.dirtyGlobalLightsCount++;
        }

        GameTime.getInstance().lightSourceUpdate = 100.0F;
        obj.transmitCompleteItemToServer();
        obj.transmitCompleteItemToClients();
        this.RecalcAllWithNeighbours(true);

        for (int z1 = this.z - 1; z1 > 0; z1--) {
            IsoGridSquare below = this.getCell().getGridSquare(this.x, this.y, z1);
            if (below == null) {
                below = getNew(this.getCell(), null, this.x, this.y, z1);
                this.getCell().ConnectNewSquare(below, false);
            }

            below.EnsureSurroundNotNull();
            below.RecalcAllWithNeighbours(true);
        }

        this.setCachedIsFree(false);
        PolygonalMap2.instance.squareChanged(this);
        IsoGridOcclusionData.SquareChanged();
        IsoRegions.squareChanged(this);
        this.clearWater();
        obj.invalidateRenderChunkLevel(64L);
        return obj;
    }

    public IsoObject addUndergroundBlock(String sprite) {
        IsoRegions.setPreviousFlags(this);
        IsoObject obj = new IsoObject(this.getCell(), this, sprite);
        obj.sprite.getProperties().set(IsoFlagType.solid);
        this.getObjects().add(obj);
        this.RecalcProperties();

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            LosUtil.cachecleared[pn] = true;
        }

        Core.dirtyGlobalLightsCount++;
        setRecalcLightTime(-1.0F);
        GameTime.getInstance().lightSourceUpdate = 100.0F;
        obj.transmitCompleteItemToServer();
        this.RecalcAllWithNeighbours(true);
        this.setCachedIsFree(false);
        PolygonalMap2.instance.squareChanged(this);
        IsoGridOcclusionData.SquareChanged();
        IsoRegions.squareChanged(this);
        this.clearWater();
        return obj;
    }

    public boolean isUndergroundBlock() {
        if (this.getObjects().size() != 1) {
            return false;
        } else {
            IsoObject object = this.getObjects().get(0);
            return object != null
                && object.getSprite() != null
                && object.getSprite().getName() != null
                && object.getSprite().getName().startsWith("underground_01");
        }
    }

    public IsoThumpable AddStairs(boolean north, int level, String sprite, String pillarSprite, KahluaTable table) {
        IsoRegions.setPreviousFlags(this);
        this.EnsureSurroundNotNull();
        boolean floating = !this.TreatAsSolidFloor() && !this.HasStairsBelow();
        this.cachedIsFree = false;
        IsoThumpable obj = new IsoThumpable(this.getCell(), this, sprite, north, table);
        if (north) {
            if (level == 0) {
                obj.setType(IsoObjectType.stairsBN);
            }

            if (level == 1) {
                obj.setType(IsoObjectType.stairsMN);
            }

            if (level == 2) {
                obj.setType(IsoObjectType.stairsTN);
                obj.sprite.getProperties().set(north ? IsoFlagType.cutN : IsoFlagType.cutW);
            }
        }

        if (!north) {
            if (level == 0) {
                obj.setType(IsoObjectType.stairsBW);
            }

            if (level == 1) {
                obj.setType(IsoObjectType.stairsMW);
            }

            if (level == 2) {
                obj.setType(IsoObjectType.stairsTW);
                obj.sprite.getProperties().set(north ? IsoFlagType.cutN : IsoFlagType.cutW);
            }
        }

        this.AddSpecialObject(obj);
        if (floating && level == 2) {
            int zI = this.z - 1;
            IsoGridSquare sq = this.getCell().getGridSquare(this.x, this.y, zI);
            if (sq == null) {
                sq = new IsoGridSquare(this.getCell(), null, this.x, this.y, zI);
                this.getCell().ConnectNewSquare(sq, true);
            }

            while (zI >= 0) {
                IsoThumpable obj2 = new IsoThumpable(this.getCell(), sq, pillarSprite, north, table);
                sq.AddSpecialObject(obj2);
                obj2.transmitCompleteItemToServer();
                if (sq.TreatAsSolidFloor()) {
                    break;
                }

                if (this.getCell().getGridSquare(sq.x, sq.y, --zI) == null) {
                    sq = new IsoGridSquare(this.getCell(), null, sq.x, sq.y, zI);
                    this.getCell().ConnectNewSquare(sq, true);
                } else {
                    sq = this.getCell().getGridSquare(sq.x, sq.y, zI);
                }
            }
        }

        if (level == 2) {
            IsoGridSquare above = null;
            if (north) {
                if (IsoWorld.instance.isValidSquare(this.x, this.y - 1, this.z + 1)) {
                    above = this.getCell().getGridSquare(this.x, this.y - 1, this.z + 1);
                    if (above == null) {
                        above = new IsoGridSquare(this.getCell(), null, this.x, this.y - 1, this.z + 1);
                        this.getCell().ConnectNewSquare(above, false);
                    }

                    if (!above.properties.has(IsoFlagType.solidfloor)) {
                        above.addFloor("carpentry_02_57");
                    }
                }
            } else if (IsoWorld.instance.isValidSquare(this.x - 1, this.y, this.z + 1)) {
                above = this.getCell().getGridSquare(this.x - 1, this.y, this.z + 1);
                if (above == null) {
                    above = new IsoGridSquare(this.getCell(), null, this.x - 1, this.y, this.z + 1);
                    this.getCell().ConnectNewSquare(above, false);
                }

                if (!above.properties.has(IsoFlagType.solidfloor)) {
                    above.addFloor("carpentry_02_57");
                }
            }

            above.getModData().rawset("ConnectedToStairs" + north, true);
            above = this.getCell().getGridSquare(this.x, this.y, this.z + 1);
            if (above == null) {
                above = new IsoGridSquare(this.getCell(), null, this.x, this.y, this.z + 1);
                this.getCell().ConnectNewSquare(above, false);
            }
        }

        for (int x = this.getX() - 1; x <= this.getX() + 1; x++) {
            for (int y = this.getY() - 1; y <= this.getY() + 1; y++) {
                for (int z = this.getZ() - 1; z <= this.getZ() + 1; z++) {
                    if (IsoWorld.instance.isValidSquare(x, y, z)) {
                        IsoGridSquare sq = this.getCell().getGridSquare(x, y, z);
                        if (sq != this) {
                            if (sq == null) {
                                sq = new IsoGridSquare(this.getCell(), null, x, y, z);
                                this.getCell().ConnectNewSquare(sq, false);
                            }

                            sq.RecalcAllWithNeighbours(true);
                        }
                    }
                }
            }
        }

        return obj;
    }

    void ReCalculateAll(IsoGridSquare a) {
        this.ReCalculateAll(a, cellGetSquare);
    }

    void ReCalculateAll(IsoGridSquare a, IsoGridSquare.GetSquare getter) {
        if (a != null && a != this) {
            this.solidFloorCached = false;
            a.solidFloorCached = false;
            this.RecalcPropertiesIfNeeded();
            a.RecalcPropertiesIfNeeded();
            this.ReCalculateCollide(a, getter);
            a.ReCalculateCollide(this, getter);
            this.ReCalculatePathFind(a, getter);
            a.ReCalculatePathFind(this, getter);
            this.ReCalculateVisionBlocked(a, getter);
            a.ReCalculateVisionBlocked(this, getter);
            this.setBlockedGridPointers(getter);
            a.setBlockedGridPointers(getter);
        }
    }

    void ReCalculateAll(boolean bDoReverse, IsoGridSquare a, IsoGridSquare.GetSquare getter) {
        if (a != null && a != this) {
            this.solidFloorCached = false;
            a.solidFloorCached = false;
            this.RecalcPropertiesIfNeeded();
            if (bDoReverse) {
                a.RecalcPropertiesIfNeeded();
            }

            this.ReCalculateCollide(a, getter);
            if (bDoReverse) {
                a.ReCalculateCollide(this, getter);
            }

            this.ReCalculatePathFind(a, getter);
            if (bDoReverse) {
                a.ReCalculatePathFind(this, getter);
            }

            this.ReCalculateVisionBlocked(a, getter);
            if (bDoReverse) {
                a.ReCalculateVisionBlocked(this, getter);
            }

            this.setBlockedGridPointers(getter);
            if (bDoReverse) {
                a.setBlockedGridPointers(getter);
            }
        }
    }

    void ReCalculateMineOnly(IsoGridSquare a) {
        this.solidFloorCached = false;
        this.RecalcProperties();
        this.ReCalculateCollide(a);
        this.ReCalculatePathFind(a);
        this.ReCalculateVisionBlocked(a);
        this.setBlockedGridPointers(cellGetSquare);
    }

    public boolean getOpenAir() {
        if (!this.getProperties().has(IsoFlagType.exterior)) {
            return false;
        } else {
            IsoGridSquare u = this.u;
            int zzz = this.z;
            return u != null ? u.getOpenAir() : IsoCell.getInstance().getGridSquare(this.x, this.y, this.z + 1) == null && this.z >= 0;
        }
    }

    public void RecalcAllWithNeighbours(boolean bDoReverse) {
        this.RecalcAllWithNeighbours(bDoReverse, cellGetSquare);
    }

    public void RecalcAllWithNeighbours(boolean bDoReverse, IsoGridSquare.GetSquare getter) {
        this.solidFloorCached = false;
        this.RecalcPropertiesIfNeeded();

        for (int x = this.getX() - 1; x <= this.getX() + 1; x++) {
            for (int y = this.getY() - 1; y <= this.getY() + 1; y++) {
                for (int z = this.getZ() - 1; z <= this.getZ() + 1; z++) {
                    if (IsoWorld.instance.isValidSquare(x, y, z)) {
                        int lx = x - this.getX();
                        int ly = y - this.getY();
                        int lz = z - this.getZ();
                        if (lx != 0 || ly != 0 || lz != 0) {
                            IsoGridSquare sq = getter.getGridSquare(x, y, z);
                            if (sq != null) {
                                sq.DirtySlice();
                                this.ReCalculateAll(bDoReverse, sq, getter);
                            }
                        }
                    }
                }
            }
        }

        IsoWorld.instance.currentCell.DoGridNav(this, getter);
        IsoGridSquare n = this.nav[IsoDirections.N.index()];
        IsoGridSquare s = this.nav[IsoDirections.S.index()];
        IsoGridSquare w = this.nav[IsoDirections.W.index()];
        IsoGridSquare e = this.nav[IsoDirections.E.index()];
        if (n != null && w != null) {
            n.ReCalculateAll(w, getter);
        }

        if (n != null && e != null) {
            n.ReCalculateAll(e, getter);
        }

        if (s != null && w != null) {
            s.ReCalculateAll(w, getter);
        }

        if (s != null && e != null) {
            s.ReCalculateAll(e, getter);
        }
    }

    public void RecalcAllWithNeighboursMineOnly() {
        this.solidFloorCached = false;
        this.RecalcProperties();

        for (int x = this.getX() - 1; x <= this.getX() + 1; x++) {
            for (int y = this.getY() - 1; y <= this.getY() + 1; y++) {
                for (int z = this.getZ() - 1; z <= this.getZ() + 1; z++) {
                    if (z >= 0) {
                        int lx = x - this.getX();
                        int ly = y - this.getY();
                        int lz = z - this.getZ();
                        if (lx != 0 || ly != 0 || lz != 0) {
                            IsoGridSquare sq = this.getCell().getGridSquare(x, y, z);
                            if (sq != null) {
                                sq.DirtySlice();
                                this.ReCalculateMineOnly(sq);
                            }
                        }
                    }
                }
            }
        }
    }

    boolean IsWindow(int sx, int sy, int sz) {
        IsoGridSquare sq = this.getCell().getGridSquare(this.x + sx, this.y + sy, this.z + sz);
        return this.getWindowTo(sq) != null || this.getWindowThumpableTo(sq) != null;
    }

    void RemoveAllWith(IsoFlagType propertyType) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject o = this.objects.get(n);
            if (o.sprite != null && o.sprite.getProperties().has(propertyType)) {
                this.objects.remove(o);
                this.specialObjects.remove(o);
                n--;
            }
        }

        this.RecalcAllWithNeighbours(true);
    }

    public boolean hasSupport() {
        IsoGridSquare s = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
        IsoGridSquare e = this.getCell().getGridSquare(this.x + 1, this.y, this.z);

        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject o = this.objects.get(n);
            if (o.sprite != null
                && (
                    o.sprite.getProperties().has(IsoFlagType.solid)
                        || (o.sprite.getProperties().has(IsoFlagType.cutW) || o.sprite.getProperties().has(IsoFlagType.cutN))
                            && !o.sprite.properties.has(IsoFlagType.halfheight)
                )) {
                return true;
            }
        }

        return s != null && s.properties.has(IsoFlagType.cutN) && !s.properties.has(IsoFlagType.halfheight)
            ? true
            : e != null && e.properties.has(IsoFlagType.cutW) && !s.properties.has(IsoFlagType.halfheight);
    }

    /**
     * @return the ID
     */
    public Integer getID() {
        return this.id;
    }

    /**
     * 
     * @param id the ID to set
     */
    public void setID(int id) {
        this.id = id;
    }

    private int savematrix(boolean[][][] pathMatrix, byte[] databytes, int index) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    databytes[index] = (byte)(pathMatrix[x][y][z] ? 1 : 0);
                    index++;
                }
            }
        }

        return index;
    }

    private int loadmatrix(boolean[][][] pathMatrix, byte[] databytes, int index) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    pathMatrix[x][y][z] = databytes[index] != 0;
                    index++;
                }
            }
        }

        return index;
    }

    private void savematrix(boolean[][][] pathMatrix, ByteBuffer databytes) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    databytes.put((byte)(pathMatrix[x][y][z] ? 1 : 0));
                }
            }
        }
    }

    private void loadmatrix(boolean[][][] pathMatrix, ByteBuffer databytes) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    pathMatrix[x][y][z] = databytes.get() != 0;
                }
            }
        }
    }

    public void DirtySlice() {
    }

    public void setHourSeenToCurrent() {
        this.hourLastSeen = (int)GameTime.instance.getWorldAgeHours();
    }

    public void splatBlood(int dist, float alpha) {
        alpha *= 2.0F;
        alpha *= 3.0F;
        if (alpha > 1.0F) {
            alpha = 1.0F;
        }

        IsoGridSquare n = this;
        IsoGridSquare w = this;

        for (int dUse = 0; dUse < dist; dUse++) {
            if (n != null) {
                n = this.getCell()
                    .getGridSquare(PZMath.fastfloor((float)this.getX()), PZMath.fastfloor((float)this.getY()) - dUse, PZMath.fastfloor((float)this.getZ()));
            }

            if (w != null) {
                w = this.getCell()
                    .getGridSquare(PZMath.fastfloor((float)this.getX()) - dUse, PZMath.fastfloor((float)this.getY()), PZMath.fastfloor((float)this.getZ()));
            }

            float offX = 0.0F;
            if (w != null && w.testCollideAdjacent(null, -1, 0, 0)) {
                boolean bLeft = false;
                boolean bRight = false;
                int min = 0;
                int max = 0;
                if (w.getS() != null && w.getS().testCollideAdjacent(null, -1, 0, 0)) {
                    bLeft = true;
                }

                if (w.getN() != null && w.getN().testCollideAdjacent(null, -1, 0, 0)) {
                    bRight = true;
                }

                if (bLeft) {
                    min = -1;
                }

                if (bRight) {
                    max = 1;
                }

                int range = max - min;
                boolean bDoTwo = false;
                int startUse = 0;
                int endUse = 0;
                if (range > 0 && Rand.Next(2) == 0) {
                    bDoTwo = true;
                    if (range > 1) {
                        if (Rand.Next(2) == 0) {
                            startUse = -1;
                            endUse = 0;
                        } else {
                            startUse = 0;
                            endUse = 1;
                        }
                    } else {
                        startUse = min;
                        endUse = max;
                    }
                }

                float offZ = Rand.Next(100) / 300.0F;
                IsoGridSquare a = this.getCell().getGridSquare(w.getX(), w.getY() + startUse, w.getZ());
                IsoGridSquare b = this.getCell().getGridSquare(w.getX(), w.getY() + endUse, w.getZ());
                if (a == null
                    || b == null
                    || !a.has(IsoFlagType.cutW)
                    || !b.has(IsoFlagType.cutW)
                    || a.getProperties().has(IsoFlagType.WallSE)
                    || b.getProperties().has(IsoFlagType.WallSE)
                    || a.has(IsoFlagType.HoppableW)
                    || b.has(IsoFlagType.HoppableW)) {
                    bDoTwo = false;
                }

                if (bDoTwo) {
                    int id = 24 + Rand.Next(2) * 2;
                    if (Rand.Next(2) == 0) {
                        id += 8;
                    }

                    a.DoSplat("overlay_blood_wall_01_" + (id + 1), false, IsoFlagType.cutW, 0.0F, offZ, alpha);
                    b.DoSplat("overlay_blood_wall_01_" + (id + 0), false, IsoFlagType.cutW, 0.0F, offZ, alpha);
                } else {
                    int id = 0;
                    switch (Rand.Next(3)) {
                        case 0:
                            id = 0 + Rand.Next(4);
                            break;
                        case 1:
                            id = 8 + Rand.Next(4);
                            break;
                        case 2:
                            id = 16 + Rand.Next(4);
                    }

                    if (id == 17 || id == 19) {
                        offZ = 0.0F;
                    }

                    if (w.has(IsoFlagType.HoppableW)) {
                        w.DoSplat("overlay_blood_fence_01_" + id, false, IsoFlagType.HoppableW, 0.0F, 0.0F, alpha);
                    } else {
                        w.DoSplat("overlay_blood_wall_01_" + id, false, IsoFlagType.cutW, 0.0F, offZ, alpha);
                    }
                }

                w = null;
            }

            if (n != null && n.testCollideAdjacent(null, 0, -1, 0)) {
                boolean bLeftx = false;
                boolean bRightx = false;
                int minx = 0;
                int maxx = 0;
                if (n.getW() != null && n.getW().testCollideAdjacent(null, 0, -1, 0)) {
                    bLeftx = true;
                }

                if (n.getE() != null && n.getE().testCollideAdjacent(null, 0, -1, 0)) {
                    bRightx = true;
                }

                if (bLeftx) {
                    minx = -1;
                }

                if (bRightx) {
                    maxx = 1;
                }

                int rangex = maxx - minx;
                boolean bDoTwox = false;
                int startUsex = 0;
                int endUsex = 0;
                if (rangex > 0 && Rand.Next(2) == 0) {
                    bDoTwox = true;
                    if (rangex > 1) {
                        if (Rand.Next(2) == 0) {
                            startUsex = -1;
                            endUsex = 0;
                        } else {
                            startUsex = 0;
                            endUsex = 1;
                        }
                    } else {
                        startUsex = minx;
                        endUsex = maxx;
                    }
                }

                float offZx = Rand.Next(100) / 300.0F;
                IsoGridSquare ax = this.getCell().getGridSquare(n.getX() + startUsex, n.getY(), n.getZ());
                IsoGridSquare bx = this.getCell().getGridSquare(n.getX() + endUsex, n.getY(), n.getZ());
                if (ax == null
                    || bx == null
                    || !ax.has(IsoFlagType.cutN)
                    || !bx.has(IsoFlagType.cutN)
                    || ax.getProperties().has(IsoFlagType.WallSE)
                    || bx.getProperties().has(IsoFlagType.WallSE)
                    || ax.has(IsoFlagType.HoppableN)
                    || bx.has(IsoFlagType.HoppableN)) {
                    bDoTwox = false;
                }

                if (bDoTwox) {
                    int id = 28 + Rand.Next(2) * 2;
                    if (Rand.Next(2) == 0) {
                        id += 8;
                    }

                    ax.DoSplat("overlay_blood_wall_01_" + (id + 0), false, IsoFlagType.cutN, 0.0F, offZx, alpha);
                    bx.DoSplat("overlay_blood_wall_01_" + (id + 1), false, IsoFlagType.cutN, 0.0F, offZx, alpha);
                } else {
                    int id = 0;
                    switch (Rand.Next(3)) {
                        case 0:
                            id = 4 + Rand.Next(4);
                            break;
                        case 1:
                            id = 12 + Rand.Next(4);
                            break;
                        case 2:
                            id = 20 + Rand.Next(4);
                    }

                    if (id == 20 || id == 22) {
                        offZx = 0.0F;
                    }

                    if (n.has(IsoFlagType.HoppableN)) {
                        n.DoSplat("overlay_blood_fence_01_" + id, false, IsoFlagType.HoppableN, 0.0F, offZx, alpha);
                    } else {
                        n.DoSplat("overlay_blood_wall_01_" + id, false, IsoFlagType.cutN, 0.0F, offZx, alpha);
                    }
                }

                n = null;
            }
        }
    }

    public boolean haveBlood() {
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return false;
        } else {
            for (int i = 0; i < this.getObjects().size(); i++) {
                IsoObject obj = this.getObjects().get(i);
                if (obj.wallBloodSplats != null && !obj.wallBloodSplats.isEmpty()) {
                    return true;
                }
            }

            for (int ix = 0; ix < this.getChunk().floorBloodSplats.size(); ix++) {
                IsoFloorBloodSplat splat = this.getChunk().floorBloodSplats.get(ix);
                float splatX = splat.x + this.getChunk().wx * 8;
                float splatY = splat.y + this.getChunk().wy * 8;
                if (PZMath.fastfloor(splatX) - 1 <= this.x
                    && PZMath.fastfloor(splatX) + 1 >= this.x
                    && PZMath.fastfloor(splatY) - 1 <= this.y
                    && PZMath.fastfloor(splatY) + 1 >= this.y) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean haveBloodWall() {
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return false;
        } else {
            for (int i = 0; i < this.getObjects().size(); i++) {
                IsoObject obj = this.getObjects().get(i);
                if (obj.wallBloodSplats != null && !obj.wallBloodSplats.isEmpty()) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean haveBloodFloor() {
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return false;
        } else {
            for (int i = 0; i < this.getChunk().floorBloodSplats.size(); i++) {
                IsoFloorBloodSplat splat = this.getChunk().floorBloodSplats.get(i);
                float splatX = splat.x + this.getChunk().wx * 8;
                float splatY = splat.y + this.getChunk().wy * 8;
                if ((int)splatX - 1 <= this.x && (int)splatX + 1 >= this.x && (int)splatY - 1 <= this.y && (int)splatY + 1 >= this.y) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean haveGrime() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.getAttachedAnimSprite() != null) {
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                for (int j = 0; j < sprites.size(); j++) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null
                        && sprite.getParentSprite() != null
                        && sprite.getParentSprite().getName() != null
                        && sprite.getParentSprite().getName().contains("overlay_grime")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean haveGrimeWall() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && !obj.isFloor() && obj.getAttachedAnimSprite() != null) {
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                for (int j = 0; j < sprites.size(); j++) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null
                        && sprite.getParentSprite() != null
                        && sprite.getParentSprite().getName() != null
                        && sprite.getParentSprite().getName().contains("overlay_grime")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean haveGrimeFloor() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.isFloor() && obj.getAttachedAnimSprite() != null) {
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                for (int j = 0; j < sprites.size(); j++) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null
                        && sprite.getParentSprite() != null
                        && sprite.getParentSprite().getName() != null
                        && sprite.getParentSprite().getName().contains("overlay_grime")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean haveGraffiti() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.getAttachedAnimSprite() != null) {
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                for (int j = 0; j < sprites.size(); j++) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null
                        && sprite.getParentSprite() != null
                        && sprite.getParentSprite().getName() != null
                        && (
                            sprite.getParentSprite().getName().contains("overlay_graffiti")
                                || sprite.getParentSprite().getName().contains("overlay_messages")
                                || sprite.getParentSprite().getName().contains("constructedobjects_signs_01_4")
                        )) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public IsoObject getGraffitiObject() {
        IsoObject graff = null;

        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.getAttachedAnimSprite() != null) {
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                for (int j = 0; j < sprites.size(); j++) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null
                        && sprite.getParentSprite() != null
                        && sprite.getParentSprite().getName() != null
                        && (
                            sprite.getParentSprite().getName().contains("overlay_graffiti")
                                || sprite.getParentSprite().getName().contains("overlay_messages")
                                || sprite.getParentSprite().getName().contains("constructedobjects_signs_01_4")
                        )) {
                        return obj;
                    }
                }
            }
        }

        return graff;
    }

    public boolean haveStains() {
        return this.haveBlood() || this.haveGrime();
    }

    public void removeGrime() {
        while (this.haveGrime()) {
            for (int i = 0; i < this.getObjects().size(); i++) {
                IsoObject obj = this.getObjects().get(i);
                if (obj != null && obj.getAttachedAnimSprite() != null) {
                    boolean clean = false;
                    ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                    for (int j = 0; j < sprites.size(); j++) {
                        IsoSpriteInstance sprite = sprites.get(j);
                        if (sprite != null
                            && sprite.getParentSprite() != null
                            && sprite.getParentSprite().getName() != null
                            && sprite.getParentSprite().getName().contains("overlay_grime")) {
                            obj.RemoveAttachedAnim(j);
                        }

                        clean = true;
                    }

                    if (clean) {
                        obj.transmitUpdatedSpriteToClients();
                    }
                }
            }
        }
    }

    public void removeGraffiti() {
        while (this.haveGraffiti()) {
            for (int i = 0; i < this.getObjects().size(); i++) {
                IsoObject obj = this.getObjects().get(i);
                if (obj != null && obj.getAttachedAnimSprite() != null) {
                    boolean clean = false;
                    ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();

                    for (int j = 0; j < sprites.size(); j++) {
                        IsoSpriteInstance sprite = sprites.get(j);
                        if (sprite != null
                            && sprite.getParentSprite() != null
                            && sprite.getParentSprite().getName() != null
                            && (
                                sprite.getParentSprite().getName().contains("overlay_graffiti")
                                    || sprite.getParentSprite().getName().contains("overlay_messages")
                                    || sprite.getParentSprite().getName().contains("constructedobjects_signs_01_4")
                            )) {
                            obj.RemoveAttachedAnim(j);
                        }

                        clean = true;
                    }

                    if (clean) {
                        obj.transmitUpdatedSpriteToClients();
                    }
                }
            }
        }
    }

    public void removeBlood(boolean remote, boolean onlyWall) {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj.wallBloodSplats != null) {
                obj.wallBloodSplats.clear();
            }
        }

        if (!onlyWall) {
            for (int ix = 0; ix < this.getChunk().floorBloodSplats.size(); ix++) {
                IsoFloorBloodSplat splat = this.getChunk().floorBloodSplats.get(ix);
                int splatX = (int)(this.getChunk().wx * 8 + splat.x);
                int splatY = (int)(this.getChunk().wy * 8 + splat.y);
                if (splatX >= this.getX() - 1 && splatX <= this.getX() + 1 && splatY >= this.getY() - 1 && splatY <= this.getY() + 1) {
                    this.getChunk().floorBloodSplats.remove(ix);
                    ix--;
                }
            }
        }

        if (GameServer.server && !remote) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveBlood, this.x, this.y, this, onlyWall);
        }

        if (PerformanceSettings.fboRenderChunk && Thread.currentThread() == GameWindow.gameThread) {
            this.invalidateRenderChunkLevel(1L);
        }
    }

    public void DoSplat(String id, boolean bFlip, IsoFlagType prop, float offX, float offZ, float alpha) {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj.sprite != null && obj.sprite.getProperties().has(prop) && (!(obj instanceof IsoWindow) || !obj.isDestroyed())) {
                IsoSprite spr = IsoSprite.getSprite(IsoSpriteManager.instance, id, 0);
                if (spr == null) {
                    return;
                }

                if (obj.wallBloodSplats == null) {
                    obj.wallBloodSplats = new ArrayList<>();
                }

                IsoWallBloodSplat splat = new IsoWallBloodSplat((float)GameTime.getInstance().getWorldAgeHours(), spr);
                obj.wallBloodSplats.add(splat);
                if (PerformanceSettings.fboRenderChunk && Thread.currentThread() == GameWindow.gameThread) {
                    this.invalidateRenderChunkLevel(1L);
                }
                break;
            }
        }
    }

    public void ClearTileObjects() {
        this.objects.clear();
        this.RecalcProperties();
    }

    public void ClearTileObjectsExceptFloor() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject o = this.objects.get(n);
            if (o.sprite == null || !o.sprite.getProperties().has(IsoFlagType.solidfloor)) {
                this.objects.remove(o);
                n--;
            }
        }

        this.RecalcProperties();
    }

    public int RemoveTileObject(IsoObject obj) {
        boolean chunkIsLoading = obj.getSquare() == null || !obj.getSquare().getChunk().loaded || obj.getSquare().getChunk().preventHotSave;
        return this.RemoveTileObject(obj, !chunkIsLoading);
    }

    public int RemoveTileObject(IsoObject obj, boolean safelyRemove) {
        if (safelyRemove) {
            return IsoObjectUtils.safelyRemoveTileObjectFromSquare(obj);
        } else {
            IsoRegions.setPreviousFlags(this);
            int index = this.objects.indexOf(obj);
            if (!this.objects.contains(obj)) {
                index = this.specialObjects.indexOf(obj);
            }

            if (obj != null && this.objects.contains(obj)) {
                if (obj.isTableSurface()) {
                    for (int i = this.objects.indexOf(obj) + 1; i < this.objects.size(); i++) {
                        IsoObject object = this.objects.get(i);
                        if (object.isTableTopObject() || object.isTableSurface()) {
                            object.setRenderYOffset(object.getRenderYOffset() - obj.getSurfaceOffset());
                            object.sx = 0.0F;
                            object.sy = 0.0F;
                        }
                    }
                }

                IsoObject playerBuiltFloor = this.getPlayerBuiltFloor();
                if (obj == playerBuiltFloor) {
                    IsoGridOcclusionData.SquareChanged();
                }

                LuaEventManager.triggerEvent("OnObjectAboutToBeRemoved", obj);
                if (!this.objects.contains(obj)) {
                    throw new IllegalArgumentException("OnObjectAboutToBeRemoved not allowed to remove the object");
                }

                index = this.objects.indexOf(obj);
                if (obj instanceof IsoWorldInventoryObject) {
                    obj.invalidateRenderChunkLevel(136L);
                } else {
                    obj.invalidateRenderChunkLevel(128L);
                }

                obj.removeFromWorld();
                obj.removeFromSquare();

                assert !this.objects.contains(obj);

                assert !this.specialObjects.contains(obj);

                if (!(obj instanceof IsoWorldInventoryObject)) {
                    this.RecalcAllWithNeighbours(true);
                    this.getCell().checkHaveRoof(this.getX(), this.getY());

                    for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                        LosUtil.cachecleared[pn] = true;
                    }

                    setRecalcLightTime(-1.0F);
                    if (PerformanceSettings.fboRenderChunk) {
                        Core.dirtyGlobalLightsCount++;
                    }

                    GameTime.instance.lightSourceUpdate = 100.0F;
                    this.fixPlacedItemRenderOffsets();
                }
            }

            MapCollisionData.instance.squareChanged(this);
            LuaEventManager.triggerEvent("OnTileRemoved", obj);
            PolygonalMap2.instance.squareChanged(this);
            IsoRegions.squareChanged(this, true);
            return index;
        }
    }

    public int RemoveTileObjectErosionNoRecalc(IsoObject obj) {
        int index = this.objects.indexOf(obj);
        IsoGridSquare sq = obj.square;
        obj.removeFromWorld();
        obj.removeFromSquare();
        sq.RecalcPropertiesIfNeeded();

        assert !this.objects.contains(obj);

        assert !this.specialObjects.contains(obj);

        return index;
    }

    public void AddSpecialObject(IsoObject obj) {
        this.AddSpecialObject(obj, -1);
    }

    public void AddSpecialObject(IsoObject obj, int index) {
        if (obj != null) {
            IsoRegions.setPreviousFlags(this);
            index = this.placeWallAndDoorCheck(obj, index);
            if (index != -1 && index >= 0 && index <= this.objects.size()) {
                this.objects.add(index, obj);
            } else {
                this.objects.add(obj);
            }

            this.specialObjects.add(obj);
            this.burntOut = false;
            obj.addToWorld();
            if (!GameServer.server && !GameClient.client) {
                this.restackSheetRope();
            }

            this.RecalcAllWithNeighbours(true);
            if (!(obj instanceof IsoWorldInventoryObject)) {
                this.fixPlacedItemRenderOffsets();

                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                setRecalcLightTime(-1.0F);
                if (PerformanceSettings.fboRenderChunk) {
                    Core.dirtyGlobalLightsCount++;
                }

                GameTime.instance.lightSourceUpdate = 100.0F;
                if (obj == this.getPlayerBuiltFloor()) {
                    IsoGridOcclusionData.SquareChanged();
                }
            }

            MapCollisionData.instance.squareChanged(this);
            PolygonalMap2.instance.squareChanged(this);
            IsoRegions.squareChanged(this);
            this.invalidateRenderChunkLevel(64L);
        }
    }

    public void AddTileObject(IsoObject obj) {
        this.AddTileObject(obj, -1);
    }

    public void AddTileObject(IsoObject obj, int index) {
        if (obj != null) {
            IsoRegions.setPreviousFlags(this);
            index = this.placeWallAndDoorCheck(obj, index);
            if (index != -1 && index >= 0 && index <= this.objects.size()) {
                this.objects.add(index, obj);
            } else {
                this.objects.add(obj);
            }

            this.burntOut = false;
            obj.addToWorld();
            this.RecalcAllWithNeighbours(true);
            if (!(obj instanceof IsoWorldInventoryObject)) {
                this.fixPlacedItemRenderOffsets();

                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                setRecalcLightTime(-1.0F);
                if (PerformanceSettings.fboRenderChunk) {
                    Core.dirtyGlobalLightsCount++;
                }

                GameTime.instance.lightSourceUpdate = 100.0F;
                if (obj == this.getPlayerBuiltFloor()) {
                    IsoGridOcclusionData.SquareChanged();
                }
            }

            MapCollisionData.instance.squareChanged(this);
            PolygonalMap2.instance.squareChanged(this);
            IsoRegions.squareChanged(this);
            this.invalidateRenderChunkLevel(64L);
        }
    }

    public int placeWallAndDoorCheck(IsoObject obj, int index) {
        int needleIndex = -1;
        if (obj.sprite != null) {
            IsoObjectType t = obj.sprite.getType();
            boolean findWalls = t == IsoObjectType.doorN || t == IsoObjectType.doorW;
            boolean findDoors = !findWalls
                && (obj.sprite.cutW || obj.sprite.cutN || t == IsoObjectType.doorFrN || t == IsoObjectType.doorFrW || obj.sprite.treatAsWallOrder);
            if (findDoors || findWalls) {
                for (int i = 0; i < this.objects.size(); i++) {
                    IsoObject other = this.objects.get(i);
                    t = IsoObjectType.MAX;
                    if (other.sprite != null) {
                        t = other.sprite.getType();
                        if (findDoors && (t == IsoObjectType.doorN || t == IsoObjectType.doorW)) {
                            needleIndex = i;
                        }

                        if (findWalls
                            && (
                                t == IsoObjectType.doorFrN
                                    || t == IsoObjectType.doorFrW
                                    || other.sprite.cutW
                                    || other.sprite.cutN
                                    || other.sprite.treatAsWallOrder
                            )) {
                            needleIndex = i;
                        }
                    }
                }

                if (findWalls && needleIndex > index) {
                    return needleIndex + 1;
                }

                if (findDoors && needleIndex >= 0 && (needleIndex < index || index < 0)) {
                    return needleIndex;
                }
            }
        }

        return index;
    }

    public void transmitAddObjectToSquare(IsoObject obj, int index) {
        if (obj != null && !this.objects.contains(obj)) {
            this.AddTileObject(obj, index);
            if (GameClient.client) {
                obj.transmitCompleteItemToServer();
            }

            if (GameServer.server) {
                obj.transmitCompleteItemToClients();
            }
        }
    }

    public int transmitRemoveItemFromSquare(IsoObject obj) {
        return this.transmitRemoveItemFromSquare(obj, true);
    }

    public int transmitRemoveItemFromSquare(IsoObject obj, boolean safelyRemove) {
        if (obj != null && this.objects.contains(obj)) {
            if (GameClient.client) {
                try {
                    GameClient.instance.checkAddedRemovedItems(obj);
                } catch (Exception var9) {
                    GameClient.connection.cancelPacket();
                    ExceptionLogger.logException(var9);
                }

                RemoveItemFromSquarePacket packet = new RemoveItemFromSquarePacket();
                packet.set(obj);
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.RemoveItemFromSquare.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.RemoveItemFromSquare.send(GameClient.connection);
            }

            if (!GameServer.server) {
                return this.RemoveTileObject(obj, safelyRemove);
            } else if (safelyRemove && IsoObjectUtils.isObjectMultiSquare(obj)) {
                ArrayList<IsoObject> objects = new ArrayList<>();
                if (IsoObjectUtils.getAllMultiTileObjects(obj, objects)) {
                    int objectIndex = -1;

                    for (IsoObject object2 : objects) {
                        IsoGridSquare sq = object2.square;
                        if (sq != null) {
                            int idx = GameServer.RemoveItemFromMap(object2);
                            if (obj == object2) {
                                objectIndex = idx;
                            }
                        }
                    }

                    return objectIndex;
                } else {
                    return -1;
                }
            } else {
                return GameServer.RemoveItemFromMap(obj);
            }
        } else {
            return -1;
        }
    }

    public void transmitRemoveItemFromSquareOnClients(IsoObject obj) {
        if (obj != null && this.objects.contains(obj)) {
            if (GameServer.server) {
                GameServer.RemoveItemFromMap(obj);
            }
        }
    }

    public void transmitModdata() {
        if (GameClient.client) {
            ReceiveModDataPacket packet = new ReceiveModDataPacket();
            packet.set(this);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.ReceiveModData.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.ReceiveModData.send(GameClient.connection);
        } else if (GameServer.server) {
            GameServer.loadModData(this);
        }
    }

    public void SpawnWorldInventoryItem(String itemType, float x, float y, float height, int nbr) {
        for (int i = 0; i < nbr; i++) {
            InventoryItem var7 = this.SpawnWorldInventoryItem(itemType, x, y, height);
        }
    }

    public InventoryItem SpawnWorldInventoryItem(String itemType, float x, float y, float height) {
        return this.SpawnWorldInventoryItem(itemType, x, y, height, true);
    }

    public InventoryItem SpawnWorldInventoryItem(String itemType, float x, float y, float height, boolean autoAge) {
        return this.AddWorldInventoryItem(itemType, x, y, height, autoAge, true);
    }

    public void AddWorldInventoryItem(String itemType, float x, float y, float height, int nbr) {
        for (int i = 0; i < nbr; i++) {
            this.AddWorldInventoryItem(itemType, x, y, height);
        }
    }

    public InventoryItem AddWorldInventoryItem(String itemType, float x, float y, float height) {
        return this.AddWorldInventoryItem(itemType, x, y, height, true);
    }

    public InventoryItem AddWorldInventoryItem(String itemType, float x, float y, float height, boolean autoAge) {
        return this.AddWorldInventoryItem(itemType, x, y, height, autoAge, false);
    }

    public InventoryItem AddWorldInventoryItem(String itemType, float x, float y, float height, boolean autoAge, boolean synchSpawn) {
        InventoryItem item = InventoryItemFactory.CreateItem(itemType);
        if (item == null) {
            return null;
        } else {
            IsoWorldInventoryObject obj = new IsoWorldInventoryObject(item, this, x, y, height);
            if (autoAge) {
                item.setAutoAge();
            }

            item.setWorldItem(obj);
            obj.setKeyId(item.getKeyId());
            obj.setName(item.getName());
            this.objects.add(obj);
            this.worldObjects.add(obj);
            if (obj.getRenderSquare() != null) {
                obj.getRenderSquare().invalidateRenderChunkLevel(68L);
            }

            if (GameClient.client) {
                obj.transmitCompleteItemToServer();
            }

            if (GameServer.server && !synchSpawn) {
                obj.transmitCompleteItemToClients();
            }

            if (synchSpawn) {
                item.SynchSpawn();
            }

            return item;
        }
    }

    public InventoryItem AddWorldInventoryItem(InventoryItem item, float x, float y, float height) {
        return this.AddWorldInventoryItem(item, x, y, height, true);
    }

    public IsoDeadBody createAnimalCorpseFromItem(InventoryItem item) {
        return item.getFullType().equals("Base.CorpseAnimal") ? item.loadCorpseFromByteData(null) : null;
    }

    public InventoryItem SpawnWorldInventoryItem(InventoryItem item, float x, float y, float height, boolean transmit) {
        return this.AddWorldInventoryItem(item, x, y, height, transmit, true);
    }

    public InventoryItem AddWorldInventoryItem(InventoryItem item, float x, float y, float height, boolean transmit) {
        return this.AddWorldInventoryItem(item, x, y, height, transmit, false);
    }

    public InventoryItem AddWorldInventoryItem(InventoryItem item, float x, float y, float height, boolean transmit, boolean synchSpawn) {
        IsoDeadBody corpse = this.tryAddCorpseToWorld(item, x, y);
        if (corpse != null) {
            return item;
        } else {
            this.invalidateRenderChunkLevel(68L);
            if (!item.getFullType().contains(".Generator") && (!item.hasTag(ItemTag.GENERATOR) || item.getWorldObjectSprite() == null)) {
                if (item instanceof AnimalInventoryItem animalItem) {
                    IsoAnimal animal = new IsoAnimal(
                        IsoWorld.instance.getCell(), this.x, this.y, this.z, animalItem.getAnimal().getAnimalType(), animalItem.getAnimal().getBreed()
                    );
                    animal.copyFrom(animalItem.getAnimal());
                    AnimalInstanceManager.getInstance().add(animal, animalItem.getAnimal().getOnlineID());
                    animal.addToWorld();
                    animal.attachBackToMotherTimer = 10000.0F;
                    animal.setSquare(this);
                    animal.playBreedSound("put_down");
                    AnimalSoundState ass = animal.getAnimalSoundState("voice");
                    if (ass != null && animal.getBreed() != null) {
                        AnimalBreed.Sound abs = animal.getBreed().getSound("idle");
                        if (abs != null) {
                            ass.setIntervalExpireTime(abs.soundName, System.currentTimeMillis() + Rand.Next(abs.intervalMin, abs.intervalMax) * 1000L);
                        }

                        abs = animal.getBreed().getSound("stressed");
                        if (abs != null) {
                            ass.setIntervalExpireTime(abs.soundName, System.currentTimeMillis() + Rand.Next(abs.intervalMin, abs.intervalMax) * 1000L);
                        }
                    }

                    if (transmit && GameServer.server) {
                        INetworkPacket.sendToRelative(
                            PacketTypes.PacketType.AnimalCommand, this.getX(), this.getY(), AnimalCommandPacket.Type.DropAnimal, animal, this
                        );
                    }

                    if (synchSpawn) {
                        item.SynchSpawn();
                    }

                    this.getCell().addToProcessItemsRemove(animalItem);
                    return animalItem;
                } else {
                    IsoWorldInventoryObject obj = new IsoWorldInventoryObject(item, this, x, y, height);
                    obj.setName(item.getName());
                    obj.setKeyId(item.getKeyId());
                    this.objects.add(obj);
                    this.worldObjects.add(obj);
                    item.setWorldItem(obj);
                    obj.addToWorld();
                    DesignationZoneAnimal.addItemOnGround(obj, this);
                    if (obj.getRenderSquare() != null) {
                        obj.getRenderSquare().invalidateRenderChunkLevel(68L);
                    }

                    if (transmit) {
                        if (GameClient.client) {
                            obj.transmitCompleteItemToServer();
                        }

                        if (GameServer.server) {
                            obj.transmitCompleteItemToClients();
                        }
                    }

                    return item;
                }
            } else {
                new IsoGenerator(item, IsoWorld.instance.currentCell, this);
                IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
                return item;
            }
        }
    }

    public IsoDeadBody tryAddCorpseToWorld(InventoryItem item, float x, float y) {
        return this.tryAddCorpseToWorld(item, x, y, true);
    }

    public @Nullable IsoDeadBody tryAddCorpseToWorld(InventoryItem item, float x, float y, boolean isVisible) {
        if (!item.isHumanCorpse() && !item.isAnimalCorpse()) {
            return null;
        } else {
            IsoDeadBody dead = item.loadCorpseFromByteData(null);
            dead.setX(this.x + x);
            dead.setY(this.y + y);
            dead.setZ(this.getApparentZ(x, y));
            dead.setSquare(this);
            dead.setCurrent(this);
            dead.setDoRender(isVisible);
            this.addCorpse(dead, false);
            if (GameServer.server) {
                GameServer.sendCorpse(dead);
            }

            IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
            return dead;
        }
    }

    public void restackSheetRope() {
        if (this.has(IsoFlagType.climbSheetW) || this.has(IsoFlagType.climbSheetN) || this.has(IsoFlagType.climbSheetE) || this.has(IsoFlagType.climbSheetS)) {
            for (int i = 0; i < this.getObjects().size() - 1; i++) {
                IsoObject sheetRope = this.getObjects().get(i);
                if (sheetRope.getProperties() != null
                    && (
                        sheetRope.getProperties().has(IsoFlagType.climbSheetW)
                            || sheetRope.getProperties().has(IsoFlagType.climbSheetN)
                            || sheetRope.getProperties().has(IsoFlagType.climbSheetE)
                            || sheetRope.getProperties().has(IsoFlagType.climbSheetS)
                    )) {
                    if (GameServer.server) {
                        this.transmitRemoveItemFromSquare(sheetRope);
                        this.objects.add(sheetRope);
                        sheetRope.transmitCompleteItemToClients();
                    } else if (!GameClient.client) {
                        this.objects.remove(sheetRope);
                        this.objects.add(sheetRope);
                    }
                    break;
                }
            }
        }
    }

    public void Burn() {
        if (!GameServer.server && !GameClient.client || !ServerOptions.instance.noFire.getValue()) {
            if (this.getCell() != null) {
                this.BurnWalls(true);
                LuaEventManager.triggerEvent("OnGridBurnt", this);
            }
        }
    }

    public void Burn(boolean explode) {
        if (!GameServer.server && !GameClient.client || !ServerOptions.instance.noFire.getValue()) {
            if (this.getCell() != null) {
                this.BurnWalls(explode);
            }
        }
    }

    public void BurnWalls(boolean explode) {
        if (!GameClient.client) {
            if (GameServer.server && SafeHouse.isSafeHouse(this, null, false) != null) {
                if (ServerOptions.instance.noFire.getValue()) {
                    return;
                }

                if (!ServerOptions.instance.safehouseAllowFire.getValue()) {
                    return;
                }
            }

            for (int i = 0; i < this.specialObjects.size(); i++) {
                IsoObject obj = this.specialObjects.get(i);
                if (obj instanceof IsoThumpable && obj.haveSheetRope()) {
                    obj.removeSheetRope(null);
                }

                if (obj instanceof IsoWindow isoWindow) {
                    if (obj.haveSheetRope()) {
                        obj.removeSheetRope(null);
                    }

                    isoWindow.removeSheet(null);
                }

                if (obj instanceof IsoWindowFrame windowFrame && windowFrame.haveSheetRope()) {
                    windowFrame.removeSheetRope(null);
                }

                if (obj instanceof BarricadeAble barricadeAble) {
                    IsoBarricade barricade1 = barricadeAble.getBarricadeOnSameSquare();
                    IsoBarricade barricade2 = barricadeAble.getBarricadeOnOppositeSquare();
                    if (barricade1 != null) {
                        if (GameServer.server) {
                            GameServer.RemoveItemFromMap(barricade1);
                        } else {
                            this.RemoveTileObject(barricade1);
                        }
                    }

                    if (barricade2 != null) {
                        if (GameServer.server) {
                            GameServer.RemoveItemFromMap(barricade2);
                        } else {
                            barricade2.getSquare().RemoveTileObject(barricade2);
                        }
                    }
                }
            }

            boolean removedTileObject = false;
            if (!this.getProperties().has(IsoFlagType.burntOut)) {
                int power = 0;

                for (int n = 0; n < this.objects.size(); n++) {
                    IsoObject objx = this.objects.get(n);
                    boolean replaceIt = false;
                    if (objx.getSprite() != null
                        && objx.getSprite().getName() != null
                        && !objx.getSprite().getProperties().has(IsoFlagType.water)
                        && !objx.getSprite().getName().contains("_burnt_")) {
                        if (objx instanceof IsoThumpable && objx.getSprite().burntTile != null) {
                            IsoObject replace = IsoObject.getNew();
                            replace.setSprite(IsoSpriteManager.instance.getSprite(objx.getSprite().burntTile));
                            replace.setSquare(this);
                            if (GameServer.server) {
                                objx.sendObjectChange("replaceWith", "object", replace);
                            }

                            objx.removeFromWorld();
                            this.objects.set(n, replace);
                        } else if (objx.getSprite().burntTile != null) {
                            objx.sprite = IsoSpriteManager.instance.getSprite(objx.getSprite().burntTile);
                            objx.RemoveAttachedAnims();
                            if (objx.children != null) {
                                objx.children.clear();
                            }

                            objx.transmitUpdatedSpriteToClients();
                            objx.setOverlaySprite(null);
                        } else if (objx.getType() == IsoObjectType.tree) {
                            objx.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_" + (Rand.Next(15, 19) + 1));
                            objx.RemoveAttachedAnims();
                            if (objx.children != null) {
                                objx.children.clear();
                            }

                            objx.transmitUpdatedSpriteToClients();
                            objx.setOverlaySprite(null);
                        } else if (!(objx instanceof IsoTrap)) {
                            if (!(objx instanceof IsoBarricade) && !(objx instanceof IsoMannequin)) {
                                if (objx instanceof IsoGenerator generator) {
                                    if (generator.getFuel() > 0.0F) {
                                        power += 20;
                                    }

                                    if (generator.isActivated()) {
                                        generator.activated = false;
                                        generator.setSurroundingElectricity();
                                        if (GameServer.server) {
                                            generator.syncIsoObject(false, (byte)0, null, null);
                                        }
                                    }

                                    if (GameServer.server) {
                                        GameServer.RemoveItemFromMap(objx);
                                    } else {
                                        this.RemoveTileObject(objx);
                                    }

                                    n--;
                                } else if (!"Campfire".equalsIgnoreCase(objx.getName())) {
                                    if (objx.getType() == IsoObjectType.wall
                                        && !objx.getProperties().has(IsoFlagType.DoorWallW)
                                        && !objx.getProperties().has(IsoFlagType.DoorWallN)
                                        && !objx.getProperties().has("WindowN")
                                        && !objx.getProperties().has(IsoFlagType.WindowW)
                                        && !objx.getSprite().getName().startsWith("walls_exterior_roofs_")
                                        && !objx.getSprite().getName().startsWith("fencing_")
                                        && !objx.getSprite().getName().startsWith("fixtures_railings_")) {
                                        if (objx.getSprite().getProperties().has(IsoFlagType.collideW)
                                            && !objx.getSprite().getProperties().has(IsoFlagType.collideN)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "0" : "4"));
                                        } else if (objx.getSprite().getProperties().has(IsoFlagType.collideN)
                                            && !objx.getSprite().getProperties().has(IsoFlagType.collideW)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "1" : "5"));
                                        } else if (objx.getSprite().getProperties().has(IsoFlagType.collideW)
                                            && objx.getSprite().getProperties().has(IsoFlagType.collideN)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "2" : "6"));
                                        } else if (objx.isWallSE()) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "3" : "7"));
                                        }
                                    } else {
                                        if (objx instanceof IsoDoor || objx instanceof IsoWindow || objx instanceof IsoCurtain) {
                                            if (GameServer.server) {
                                                GameServer.RemoveItemFromMap(objx);
                                            } else {
                                                this.RemoveTileObject(objx);
                                                removedTileObject = true;
                                            }

                                            n--;
                                            continue;
                                        }

                                        if (objx.getProperties().has(IsoFlagType.WindowW)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "8" : "12"));
                                        } else if (objx.getProperties().has("WindowN")) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "9" : "13"));
                                        } else if (objx.getProperties().has(IsoFlagType.DoorWallW)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "10" : "14"));
                                        } else if (objx.getProperties().has(IsoFlagType.DoorWallN)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "11" : "15"));
                                        } else if (objx.getSprite().getProperties().has(IsoFlagType.solidfloor)
                                            && !objx.getSprite().getProperties().has(IsoFlagType.exterior)) {
                                            objx.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_0");
                                        } else {
                                            if (objx instanceof IsoWaveSignal) {
                                                if (GameServer.server) {
                                                    GameServer.RemoveItemFromMap(objx);
                                                } else {
                                                    this.RemoveTileObject(objx);
                                                    removedTileObject = true;
                                                }

                                                n--;
                                                continue;
                                            }

                                            if (objx.getContainer() != null && objx.getContainer().getItems() != null) {
                                                InventoryItem item = null;

                                                for (int i = 0; i < objx.getContainer().getItems().size(); i++) {
                                                    item = objx.getContainer().getItems().get(i);
                                                    if (item instanceof Food && item.isAlcoholic()
                                                        || item.getType().equals("PetrolCan")
                                                        || item.getType().equals("Bleach")) {
                                                        power += 20;
                                                        if (power > 100) {
                                                            power = 100;
                                                            break;
                                                        }
                                                    }
                                                }

                                                objx.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next(1, 2));

                                                for (int ix = 0; ix < objx.getContainerCount(); ix++) {
                                                    ItemContainer container = objx.getContainerByIndex(ix);
                                                    container.removeItemsFromProcessItems();
                                                    container.removeAllItems();
                                                }

                                                objx.removeAllContainers();
                                                if (objx.getOverlaySprite() != null) {
                                                    objx.setOverlaySprite(null);
                                                }

                                                replaceIt = true;
                                            } else if (objx.getSprite().getProperties().has(IsoFlagType.solidtrans)
                                                || objx.getSprite().getProperties().has(IsoFlagType.bed)
                                                || objx.getSprite().getProperties().has(IsoFlagType.waterPiped)) {
                                                objx.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next(1, 2));
                                                if (objx.getOverlaySprite() != null) {
                                                    objx.setOverlaySprite(null);
                                                }
                                            } else if (objx.getSprite().getName().startsWith("walls_exterior_roofs_")) {
                                                objx.sprite = IsoSpriteManager.instance
                                                    .getSprite(
                                                        "walls_burnt_roofs_01_"
                                                            + objx.getSprite().getName().substring(objx.getSprite().getName().lastIndexOf("_") + 1)
                                                    );
                                            } else if (!objx.getSprite().getName().startsWith("roofs_accents")) {
                                                if (objx.getSprite().getName().startsWith("roofs_")) {
                                                    objx.sprite = IsoSpriteManager.instance
                                                        .getSprite(
                                                            "roofs_burnt_01_"
                                                                + objx.getSprite().getName().substring(objx.getSprite().getName().lastIndexOf("_") + 1)
                                                        );
                                                } else if ((
                                                        objx.getSprite().getName().startsWith("fencing_")
                                                            || objx.getSprite().getName().startsWith("fixtures_railings_")
                                                    )
                                                    && (
                                                        objx.getSprite().getProperties().has(IsoFlagType.HoppableN)
                                                            || objx.getSprite().getProperties().has(IsoFlagType.HoppableW)
                                                    )) {
                                                    if (objx.getSprite().getProperties().has(IsoFlagType.transparentW)
                                                        && !objx.getSprite().getProperties().has(IsoFlagType.transparentN)) {
                                                        objx.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_0");
                                                    } else if (objx.getSprite().getProperties().has(IsoFlagType.transparentN)
                                                        && !objx.getSprite().getProperties().has(IsoFlagType.transparentW)) {
                                                        objx.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_1");
                                                    } else {
                                                        objx.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_2");
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!replaceIt && !(objx instanceof IsoThumpable)) {
                                        objx.RemoveAttachedAnims();
                                        objx.transmitUpdatedSpriteToClients();
                                        objx.setOverlaySprite(null);
                                    } else {
                                        IsoObject replace = IsoObject.getNew();
                                        replace.setSprite(objx.getSprite());
                                        replace.setSquare(this);
                                        if (GameServer.server) {
                                            objx.sendObjectChange("replaceWith", "object", replace);
                                        }

                                        this.objects.set(n, replace);
                                    }

                                    if (objx.emitter != null) {
                                        objx.emitter.stopAll();
                                        objx.emitter = null;
                                    }
                                }
                            } else {
                                if (GameServer.server) {
                                    GameServer.RemoveItemFromMap(objx);
                                } else {
                                    this.objects.remove(objx);
                                }

                                n--;
                            }
                        }
                    }
                }

                if (power > 0 && explode) {
                    if (GameServer.server) {
                        GameServer.PlayWorldSoundServer("BurnedObjectExploded", false, this, 0.0F, 50.0F, 1.0F, false);
                    } else {
                        SoundManager.instance.PlayWorldSound("BurnedObjectExploded", this, 0.0F, 50.0F, 1.0F, false);
                    }

                    IsoFireManager.explode(this.getCell(), this, power);
                }
            }

            for (int ix = 0; ix < this.specialObjects.size(); ix++) {
                IsoObject object = this.specialObjects.get(ix);
                if (!this.objects.contains(object)) {
                    this.specialObjects.remove(ix);
                    ix--;
                }
            }

            if (!removedTileObject) {
                this.RecalcProperties();
            }

            this.getProperties().set(IsoFlagType.burntOut);
            this.burntOut = true;
            MapCollisionData.instance.squareChanged(this);
            PolygonalMap2.instance.squareChanged(this);
            this.invalidateRenderChunkLevel(384L);
        }
    }

    public void BurnWallsTCOnly() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject obj = this.objects.get(n);
            if (obj.sprite == null) {
            }
        }
    }

    public void BurnTick() {
        if (!GameClient.client) {
            for (int i = 0; i < this.staticMovingObjects.size(); i++) {
                IsoMovingObject mov = this.staticMovingObjects.get(i);
                if (mov instanceof IsoDeadBody isoDeadBody) {
                    isoDeadBody.Burn();
                    if (!this.staticMovingObjects.contains(mov)) {
                        i--;
                    }
                }
            }
        }
    }

    public boolean CalculateCollide(IsoGridSquare gridSquare, boolean bVision, boolean bPathfind, boolean bIgnoreSolidTrans) {
        return this.CalculateCollide(gridSquare, bVision, bPathfind, bIgnoreSolidTrans, false);
    }

    public boolean CalculateCollide(IsoGridSquare gridSquare, boolean bVision, boolean bPathfind, boolean bIgnoreSolidTrans, boolean bIgnoreSolid) {
        return this.CalculateCollide(gridSquare, bVision, bPathfind, bIgnoreSolidTrans, bIgnoreSolid, cellGetSquare);
    }

    public boolean CalculateCollide(
        IsoGridSquare gridSquare, boolean bVision, boolean bPathfind, boolean bIgnoreSolidTrans, boolean bIgnoreSolid, IsoGridSquare.GetSquare getter
    ) {
        if (gridSquare == null && bPathfind) {
            return true;
        } else if (gridSquare == null) {
            return false;
        } else if ((!this.properties.has(IsoFlagType.water) || this.hasFloorOverWater())
            && (!gridSquare.properties.has(IsoFlagType.water) || gridSquare.hasFloorOverWater())) {
            if (bVision && gridSquare.properties.has(IsoFlagType.trans)) {
            }

            boolean testW = false;
            boolean testE = false;
            boolean testN = false;
            boolean testS = false;
            if (gridSquare.x < this.x) {
                testW = true;
            }

            if (gridSquare.y < this.y) {
                testN = true;
            }

            if (gridSquare.x > this.x) {
                testE = true;
            }

            if (gridSquare.y > this.y) {
                testS = true;
            }

            if (!bIgnoreSolid && gridSquare.properties.has(IsoFlagType.solid)) {
                return this.has(IsoObjectType.stairsTW) && !bPathfind && gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.z == this.z
                    ? false
                    : !this.has(IsoObjectType.stairsTN) || bPathfind || gridSquare.x != this.x || gridSquare.y >= this.y || gridSquare.z != this.z;
            } else {
                if (!bIgnoreSolidTrans && gridSquare.properties.has(IsoFlagType.solidtrans)) {
                    if (this.has(IsoObjectType.stairsTW) && !bPathfind && gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.z == this.z) {
                        return false;
                    }

                    if (this.has(IsoObjectType.stairsTN) && !bPathfind && gridSquare.x == this.x && gridSquare.y < this.y && gridSquare.z == this.z) {
                        return false;
                    }

                    boolean hasHoppable = false;
                    if (gridSquare.properties.has(IsoFlagType.HoppableN) || gridSquare.properties.has(IsoFlagType.HoppableW)) {
                        hasHoppable = true;
                    }

                    if (!hasHoppable) {
                        IsoGridSquare s = getter.getGridSquare(gridSquare.x, gridSquare.y + 1, this.z);
                        if (s != null && (s.has(IsoFlagType.HoppableN) || s.has(IsoFlagType.HoppableW))) {
                            hasHoppable = true;
                        }
                    }

                    if (!hasHoppable) {
                        IsoGridSquare e = getter.getGridSquare(gridSquare.x + 1, gridSquare.y, this.z);
                        if (e != null && (e.has(IsoFlagType.HoppableN) || e.has(IsoFlagType.HoppableW))) {
                            hasHoppable = true;
                        }
                    }

                    boolean hasWindow = false;
                    if (gridSquare.properties.has(IsoFlagType.windowW) || gridSquare.properties.has(IsoFlagType.windowN)) {
                        hasWindow = true;
                    }

                    if (!hasWindow && (gridSquare.properties.has(IsoFlagType.WindowW) || gridSquare.properties.has(IsoFlagType.WindowN))) {
                        hasWindow = true;
                    }

                    if (!hasWindow) {
                        IsoGridSquare s = getter.getGridSquare(gridSquare.x, gridSquare.y + 1, this.z);
                        if (s != null && (s.has(IsoFlagType.windowN) || s.has(IsoFlagType.WindowN))) {
                            hasWindow = true;
                        }
                    }

                    if (!hasWindow) {
                        IsoGridSquare e = getter.getGridSquare(gridSquare.x + 1, gridSquare.y, this.z);
                        if (e != null && (e.has(IsoFlagType.windowW) || e.has(IsoFlagType.WindowW))) {
                            hasWindow = true;
                        }
                    }

                    if (!hasWindow && !hasHoppable) {
                        return true;
                    }
                }

                if (gridSquare.x != this.x && gridSquare.y != this.y && this.z != gridSquare.z && bPathfind) {
                    return true;
                } else if (!bPathfind || gridSquare.z >= this.z || (this.solidFloorCached ? this.solidFloor : this.TreatAsSolidFloor())) {
                    if (bPathfind && gridSquare.z == this.z) {
                        if (gridSquare.x > this.x && gridSquare.y == this.y && gridSquare.properties.has(IsoFlagType.windowW)) {
                            return false;
                        }

                        if (gridSquare.y > this.y && gridSquare.x == this.x && gridSquare.properties.has(IsoFlagType.windowN)) {
                            return false;
                        }

                        if (gridSquare.x < this.x && gridSquare.y == this.y && this.properties.has(IsoFlagType.windowW)) {
                            return false;
                        }

                        if (gridSquare.y < this.y && gridSquare.x == this.x && this.properties.has(IsoFlagType.windowN)) {
                            return false;
                        }
                    }

                    if (gridSquare.x > this.x && gridSquare.z < this.z && gridSquare.has(IsoObjectType.stairsTW)) {
                        return false;
                    } else if (gridSquare.y > this.y && gridSquare.z < this.z && gridSquare.has(IsoObjectType.stairsTN)) {
                        return false;
                    } else {
                        IsoGridSquare belowTarg = getter.getGridSquare(gridSquare.x, gridSquare.y, gridSquare.z - 1);
                        if (gridSquare.x == this.x
                            || gridSquare.z != this.z
                            || !gridSquare.has(IsoObjectType.stairsTN)
                            || belowTarg != null && belowTarg.has(IsoObjectType.stairsTN) && !bPathfind) {
                            if (gridSquare.y <= this.y
                                || gridSquare.x != this.x
                                || gridSquare.z != this.z
                                || !gridSquare.has(IsoObjectType.stairsTN)
                                || belowTarg != null && belowTarg.has(IsoObjectType.stairsTN) && !bPathfind) {
                                if (gridSquare.x <= this.x
                                    || gridSquare.y != this.y
                                    || gridSquare.z != this.z
                                    || !gridSquare.has(IsoObjectType.stairsTW)
                                    || belowTarg != null && belowTarg.has(IsoObjectType.stairsTW) && !bPathfind) {
                                    if (gridSquare.y == this.y
                                        || gridSquare.z != this.z
                                        || !gridSquare.has(IsoObjectType.stairsTW)
                                        || belowTarg != null && belowTarg.has(IsoObjectType.stairsTW) && !bPathfind) {
                                        if (gridSquare.x != this.x && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsMN)) {
                                            return true;
                                        } else if (gridSquare.y != this.y && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsMW)) {
                                            return true;
                                        } else if (gridSquare.x != this.x && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsBN)) {
                                            return true;
                                        } else if (gridSquare.y != this.y && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsBW)) {
                                            return true;
                                        } else if (gridSquare.x != this.x && gridSquare.z == this.z && this.has(IsoObjectType.stairsTN)) {
                                            return true;
                                        } else if (gridSquare.y != this.y && gridSquare.z == this.z && this.has(IsoObjectType.stairsTW)) {
                                            return true;
                                        } else if (gridSquare.x != this.x && gridSquare.z == this.z && this.has(IsoObjectType.stairsMN)) {
                                            return true;
                                        } else if (gridSquare.y != this.y && gridSquare.z == this.z && this.has(IsoObjectType.stairsMW)) {
                                            return true;
                                        } else if (gridSquare.x != this.x && gridSquare.z == this.z && this.has(IsoObjectType.stairsBN)) {
                                            return true;
                                        } else if (gridSquare.y != this.y && gridSquare.z == this.z && this.has(IsoObjectType.stairsBW)) {
                                            return true;
                                        } else if (gridSquare.y < this.y && gridSquare.x == this.x && gridSquare.z > this.z && this.has(IsoObjectType.stairsTN)
                                            )
                                         {
                                            return false;
                                        } else if (gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.z > this.z && this.has(IsoObjectType.stairsTW)
                                            )
                                         {
                                            return false;
                                        } else if (gridSquare.y > this.y
                                            && gridSquare.x == this.x
                                            && gridSquare.z < this.z
                                            && gridSquare.has(IsoObjectType.stairsTN)) {
                                            return false;
                                        } else if (gridSquare.x > this.x
                                            && gridSquare.y == this.y
                                            && gridSquare.z < this.z
                                            && gridSquare.has(IsoObjectType.stairsTW)) {
                                            return false;
                                        } else {
                                            if (gridSquare.z == this.z) {
                                                if (gridSquare.x == this.x
                                                    && gridSquare.y == this.y - 1
                                                    && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)
                                                    && (
                                                        this.isSlopedSurfaceEdgeBlocked(IsoDirections.N)
                                                            || gridSquare.isSlopedSurfaceEdgeBlocked(IsoDirections.S)
                                                    )) {
                                                    return true;
                                                }

                                                if (gridSquare.x == this.x
                                                    && gridSquare.y == this.y + 1
                                                    && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.S)
                                                    && (
                                                        this.isSlopedSurfaceEdgeBlocked(IsoDirections.S)
                                                            || gridSquare.isSlopedSurfaceEdgeBlocked(IsoDirections.N)
                                                    )) {
                                                    return true;
                                                }

                                                if (gridSquare.x == this.x - 1
                                                    && gridSquare.y == this.y
                                                    && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)
                                                    && (
                                                        this.isSlopedSurfaceEdgeBlocked(IsoDirections.W)
                                                            || gridSquare.isSlopedSurfaceEdgeBlocked(IsoDirections.E)
                                                    )) {
                                                    return true;
                                                }

                                                if (gridSquare.x == this.x + 1
                                                    && gridSquare.y == this.y
                                                    && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.E)
                                                    && (
                                                        this.isSlopedSurfaceEdgeBlocked(IsoDirections.E)
                                                            || gridSquare.isSlopedSurfaceEdgeBlocked(IsoDirections.W)
                                                    )) {
                                                    return true;
                                                }
                                            }

                                            if (gridSquare.z > this.z) {
                                                if (gridSquare.y < this.y && gridSquare.x == this.x && this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                                                    return false;
                                                }

                                                if (gridSquare.y > this.y && gridSquare.x == this.x && this.hasSlopedSurfaceToLevelAbove(IsoDirections.S)) {
                                                    return false;
                                                }

                                                if (gridSquare.x < this.x && gridSquare.y == this.y && this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                                                    return false;
                                                }

                                                if (gridSquare.x > this.x && gridSquare.y == this.y && this.hasSlopedSurfaceToLevelAbove(IsoDirections.E)) {
                                                    return false;
                                                }
                                            }

                                            if (gridSquare.z < this.z) {
                                                if (gridSquare.y > this.y && gridSquare.x == this.x && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.N)
                                                    )
                                                 {
                                                    return false;
                                                }

                                                if (gridSquare.y < this.y && gridSquare.x == this.x && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.S)
                                                    )
                                                 {
                                                    return false;
                                                }

                                                if (gridSquare.x > this.x && gridSquare.y == this.y && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.W)
                                                    )
                                                 {
                                                    return false;
                                                }

                                                if (gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.E)
                                                    )
                                                 {
                                                    return false;
                                                }
                                            }

                                            if (gridSquare.z == this.z
                                                && (gridSquare.solidFloorCached ? !gridSquare.solidFloor : !gridSquare.TreatAsSolidFloor())
                                                && bPathfind) {
                                                return true;
                                            } else {
                                                if (gridSquare.z == this.z
                                                    && (gridSquare.solidFloorCached ? !gridSquare.solidFloor : !gridSquare.TreatAsSolidFloor())
                                                    && gridSquare.z > 0) {
                                                    belowTarg = getter.getGridSquare(gridSquare.x, gridSquare.y, gridSquare.z - 1);
                                                    if (belowTarg == null) {
                                                        return true;
                                                    }
                                                }

                                                if (this.z == gridSquare.z) {
                                                    boolean colN = testN && this.properties.has(IsoFlagType.collideN);
                                                    boolean colW = testW && this.properties.has(IsoFlagType.collideW);
                                                    boolean colS = testS && gridSquare.properties.has(IsoFlagType.collideN);
                                                    boolean colE = testE && gridSquare.properties.has(IsoFlagType.collideW);
                                                    if (colN && bPathfind && this.properties.has(IsoFlagType.canPathN)) {
                                                        colN = false;
                                                    }

                                                    if (colW && bPathfind && this.properties.has(IsoFlagType.canPathW)) {
                                                        colW = false;
                                                    }

                                                    if (colS && bPathfind && gridSquare.properties.has(IsoFlagType.canPathN)) {
                                                        colS = false;
                                                    }

                                                    if (colE && bPathfind && gridSquare.properties.has(IsoFlagType.canPathW)) {
                                                        colE = false;
                                                    }

                                                    if (colW && this.has(IsoObjectType.stairsTW) && !bPathfind) {
                                                        colW = false;
                                                    }

                                                    if (colN && this.has(IsoObjectType.stairsTN) && !bPathfind) {
                                                        colN = false;
                                                    }

                                                    if (!colN && !colW && !colS && !colE) {
                                                        boolean diag = gridSquare.x != this.x && gridSquare.y != this.y;
                                                        if (diag) {
                                                            IsoGridSquare betweenA = getter.getGridSquare(this.x, gridSquare.y, this.z);
                                                            IsoGridSquare betweenB = getter.getGridSquare(gridSquare.x, this.y, this.z);
                                                            if (betweenA != null && betweenA != this && betweenA != gridSquare) {
                                                                betweenA.RecalcPropertiesIfNeeded();
                                                            }

                                                            if (betweenB != null && betweenB != this && betweenB != gridSquare) {
                                                                betweenB.RecalcPropertiesIfNeeded();
                                                            }

                                                            if (gridSquare == this
                                                                || betweenA == betweenB
                                                                || betweenA == this
                                                                || betweenB == this
                                                                || betweenA == gridSquare
                                                                || betweenB == gridSquare) {
                                                                return true;
                                                            }

                                                            if (gridSquare.x == this.x + 1
                                                                && gridSquare.y == this.y + 1
                                                                && betweenA != null
                                                                && betweenB != null
                                                                && betweenA.has(IsoFlagType.windowN)
                                                                && betweenB.has(IsoFlagType.windowW)) {
                                                                return true;
                                                            }

                                                            if (gridSquare.x == this.x - 1
                                                                && gridSquare.y == this.y - 1
                                                                && betweenA != null
                                                                && betweenB != null
                                                                && betweenA.has(IsoFlagType.windowW)
                                                                && betweenB.has(IsoFlagType.windowN)) {
                                                                return true;
                                                            }

                                                            if (this.CalculateCollide(betweenA, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                                                                return true;
                                                            }

                                                            if (this.CalculateCollide(betweenB, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                                                                return true;
                                                            }

                                                            if (gridSquare.CalculateCollide(betweenA, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                                                                return true;
                                                            }

                                                            if (gridSquare.CalculateCollide(betweenB, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                                                                return true;
                                                            }
                                                        }

                                                        return false;
                                                    } else {
                                                        return true;
                                                    }
                                                } else {
                                                    return gridSquare.z >= this.z
                                                        || gridSquare.x != this.x
                                                        || gridSquare.y != this.y
                                                        || (this.solidFloorCached ? this.solidFloor : this.TreatAsSolidFloor());
                                                }
                                            }
                                        }
                                    } else {
                                        return true;
                                    }
                                } else {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                } else {
                    return gridSquare.has(IsoObjectType.stairsTN) || gridSquare.has(IsoObjectType.stairsTW);
                }
            }
        } else {
            return true;
        }
    }

    public boolean CalculateVisionBlocked(IsoGridSquare gridSquare) {
        return this.CalculateVisionBlocked(gridSquare, cellGetSquare);
    }

    public boolean CalculateVisionBlocked(IsoGridSquare gridSquare, IsoGridSquare.GetSquare getter) {
        if (gridSquare == null) {
            return false;
        } else if (Math.abs(gridSquare.getX() - this.getX()) <= 1 && Math.abs(gridSquare.getY() - this.getY()) <= 1) {
            boolean testW = false;
            boolean testE = false;
            boolean testN = false;
            boolean testS = false;
            if (gridSquare.x < this.x) {
                testW = true;
            }

            if (gridSquare.y < this.y) {
                testN = true;
            }

            if (gridSquare.x > this.x) {
                testE = true;
            }

            if (gridSquare.y > this.y) {
                testS = true;
            }

            if (!gridSquare.properties.has(IsoFlagType.trans) && !this.properties.has(IsoFlagType.trans)) {
                if (this.z != gridSquare.z) {
                    if (gridSquare.z > this.z) {
                        if (gridSquare.properties.has(IsoFlagType.solidfloor) && !gridSquare.getProperties().has(IsoFlagType.transparentFloor)) {
                            return true;
                        }

                        if (this.properties.has(IsoFlagType.noStart)) {
                            return true;
                        }

                        IsoGridSquare sq = getter.getGridSquare(this.x, this.y, gridSquare.z);
                        if (sq == null) {
                            return false;
                        }

                        if (sq.properties.has(IsoFlagType.solidfloor) && !sq.getProperties().has(IsoFlagType.transparentFloor)) {
                            return true;
                        }
                    } else {
                        if (this.properties.has(IsoFlagType.solidfloor) && !this.getProperties().has(IsoFlagType.transparentFloor)) {
                            return true;
                        }

                        if (this.properties.has(IsoFlagType.noStart)) {
                            return true;
                        }

                        IsoGridSquare sqx = getter.getGridSquare(gridSquare.x, gridSquare.y, this.z);
                        if (sqx == null) {
                            return false;
                        }

                        if (sqx.properties.has(IsoFlagType.solidfloor) && !sqx.getProperties().has(IsoFlagType.transparentFloor)) {
                            return true;
                        }
                    }
                }

                boolean colN = testN
                    && this.properties.has(IsoFlagType.collideN)
                    && !this.properties.has(IsoFlagType.transparentN)
                    && !this.properties.has(IsoFlagType.doorN);
                boolean colW = testW
                    && this.properties.has(IsoFlagType.collideW)
                    && !this.properties.has(IsoFlagType.transparentW)
                    && !this.properties.has(IsoFlagType.doorW);
                boolean colS = testS
                    && gridSquare.properties.has(IsoFlagType.collideN)
                    && !gridSquare.properties.has(IsoFlagType.transparentN)
                    && !gridSquare.properties.has(IsoFlagType.doorN);
                boolean colE = testE
                    && gridSquare.properties.has(IsoFlagType.collideW)
                    && !gridSquare.properties.has(IsoFlagType.transparentW)
                    && !gridSquare.properties.has(IsoFlagType.doorW);
                if (!colN && !colW && !colS && !colE) {
                    boolean diag = gridSquare.x != this.x && gridSquare.y != this.y;
                    if (!gridSquare.properties.has(IsoFlagType.solid) && !gridSquare.properties.has(IsoFlagType.blocksight)) {
                        if (diag) {
                            IsoGridSquare betweenA = getter.getGridSquare(this.x, gridSquare.y, this.z);
                            IsoGridSquare betweenB = getter.getGridSquare(gridSquare.x, this.y, this.z);
                            if (betweenA != null && betweenA != this && betweenA != gridSquare) {
                                betweenA.RecalcPropertiesIfNeeded();
                            }

                            if (betweenB != null && betweenB != this && betweenB != gridSquare) {
                                betweenB.RecalcPropertiesIfNeeded();
                            }

                            if (this.CalculateVisionBlocked(betweenA, getter)) {
                                return true;
                            }

                            if (this.CalculateVisionBlocked(betweenB, getter)) {
                                return true;
                            }

                            if (gridSquare.CalculateVisionBlocked(betweenA, getter)) {
                                return true;
                            }

                            if (gridSquare.CalculateVisionBlocked(betweenB, getter)) {
                                return true;
                            }
                        }

                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public IsoGameCharacter FindFriend(IsoGameCharacter g, int range, Stack<IsoGameCharacter> EnemyList) {
        Stack<IsoGameCharacter> zombieList = new Stack<>();

        for (int n = 0; n < g.getLocalList().size(); n++) {
            IsoMovingObject obj = g.getLocalList().get(n);
            if (obj != g
                && obj != g.getFollowingTarget()
                && obj instanceof IsoGameCharacter isoGameCharacter
                && !(obj instanceof IsoZombie)
                && !EnemyList.contains(obj)) {
                zombieList.add(isoGameCharacter);
            }
        }

        float lowestDist = 1000000.0F;
        IsoGameCharacter lowest = null;

        for (IsoGameCharacter z : zombieList) {
            float Dist = 0.0F;
            Dist += Math.abs(this.getX() - z.getX());
            Dist += Math.abs(this.getY() - z.getY());
            Dist += Math.abs(this.getZ() - z.getZ());
            if (Dist < lowestDist) {
                lowest = z;
                lowestDist = Dist;
            }

            if (z == IsoPlayer.getInstance()) {
                lowest = z;
                Dist = 0.0F;
            }
        }

        return lowestDist > range ? null : lowest;
    }

    public IsoGameCharacter FindEnemy(IsoGameCharacter g, int range, ArrayList<IsoMovingObject> EnemyList, IsoGameCharacter RangeTest, int TestRangeMax) {
        float lowestDist = 1000000.0F;
        IsoGameCharacter lowest = null;

        for (int n = 0; n < EnemyList.size(); n++) {
            IsoGameCharacter z = (IsoGameCharacter)EnemyList.get(n);
            float Dist = 0.0F;
            Dist += Math.abs(this.getX() - z.getX());
            Dist += Math.abs(this.getY() - z.getY());
            Dist += Math.abs(this.getZ() - z.getZ());
            if (Dist < range && Dist < lowestDist && z.DistTo(RangeTest) < TestRangeMax) {
                lowest = z;
                lowestDist = Dist;
            }
        }

        return lowestDist > range ? null : lowest;
    }

    public IsoGameCharacter FindEnemy(IsoGameCharacter g, int range, ArrayList<IsoMovingObject> EnemyList) {
        float lowestDist = 1000000.0F;
        IsoGameCharacter lowest = null;

        for (int n = 0; n < EnemyList.size(); n++) {
            IsoGameCharacter z = (IsoGameCharacter)EnemyList.get(n);
            float Dist = 0.0F;
            Dist += Math.abs(this.getX() - z.getX());
            Dist += Math.abs(this.getY() - z.getY());
            Dist += Math.abs(this.getZ() - z.getZ());
            if (Dist < lowestDist) {
                lowest = z;
                lowestDist = Dist;
            }
        }

        return lowestDist > range ? null : lowest;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public void RecalcProperties() {
        this.cachedIsFree = false;
        String fuelAmount = null;
        if (this.properties.has("fuelAmount")) {
            fuelAmount = this.properties.get("fuelAmount");
        }

        if (this.zone == null) {
            this.zone = IsoWorld.instance.metaGrid.getZoneAt(this.x, this.y, this.z);
        }

        this.properties.Clear();
        this.hasTypes = 0L;
        this.hasTree = false;
        boolean nonWaterSolidTrans = false;
        boolean nonWaterSolidFloor = false;
        boolean nonTransparentSolidFloor = false;
        boolean nonHoppableCollideN = false;
        boolean nonHoppableCollideW = false;
        boolean nonTransparentCutN = false;
        boolean nonTransparentCutW = false;
        boolean forceRender = false;
        int numObjects = this.objects.size();
        IsoObject[] objectArray = this.objects.getElements();

        for (int n = 0; n < numObjects; n++) {
            IsoObject obj = objectArray[n];
            if (obj != null) {
                PropertyContainer spriteProps = obj.getProperties();
                if (spriteProps != null && !spriteProps.has(IsoFlagType.blueprint)) {
                    if (obj.sprite.forceRender) {
                        forceRender = true;
                    }

                    if (obj.getType() == IsoObjectType.tree) {
                        this.hasTree = true;
                    }

                    this.hasTypes = this.hasTypes | 1L << obj.getType().index();
                    this.properties.AddProperties(spriteProps);
                    if (spriteProps.has(IsoFlagType.water)) {
                        nonWaterSolidFloor = false;
                    } else {
                        if (!nonWaterSolidFloor && spriteProps.has(IsoFlagType.solidfloor)) {
                            nonWaterSolidFloor = true;
                        }

                        if (!nonWaterSolidTrans && spriteProps.has(IsoFlagType.solidtrans)) {
                            nonWaterSolidTrans = true;
                        }

                        if (!nonTransparentSolidFloor && spriteProps.has(IsoFlagType.solidfloor) && !spriteProps.has(IsoFlagType.transparentFloor)) {
                            nonTransparentSolidFloor = true;
                        }
                    }

                    if (!nonHoppableCollideN && spriteProps.has(IsoFlagType.collideN) && !spriteProps.has(IsoFlagType.HoppableN)) {
                        nonHoppableCollideN = true;
                    }

                    if (!nonHoppableCollideW && spriteProps.has(IsoFlagType.collideW) && !spriteProps.has(IsoFlagType.HoppableW)) {
                        nonHoppableCollideW = true;
                    }

                    if (!nonTransparentCutN
                        && spriteProps.has(IsoFlagType.cutN)
                        && !spriteProps.has(IsoFlagType.transparentN)
                        && !spriteProps.has(IsoFlagType.WallSE)) {
                        nonTransparentCutN = true;
                    }

                    if (!nonTransparentCutW
                        && spriteProps.has(IsoFlagType.cutW)
                        && !spriteProps.has(IsoFlagType.transparentW)
                        && !spriteProps.has(IsoFlagType.WallSE)) {
                        nonTransparentCutW = true;
                    }
                }
            }
        }

        if (this.roomId == -1L && !this.haveRoof) {
            this.getProperties().set(IsoFlagType.exterior);

            try {
                this.getPuddles().recalc = true;
            } catch (Exception var15) {
                var15.printStackTrace();
            }
        } else {
            this.getProperties().unset(IsoFlagType.exterior);

            try {
                this.getPuddles().recalc = true;
            } catch (Exception var16) {
                var16.printStackTrace();
            }
        }

        if (fuelAmount != null) {
            this.getProperties().set("fuelAmount", fuelAmount, false);
        }

        if (this.rainDrop != null) {
            this.properties.set(IsoFlagType.HasRaindrop);
        }

        if (forceRender) {
            this.properties.set(IsoFlagType.forceRender);
        }

        if (this.rainSplash != null) {
            this.properties.set(IsoFlagType.HasRainSplashes);
        }

        if (this.burntOut) {
            this.properties.set(IsoFlagType.burntOut);
        }

        if (!nonWaterSolidTrans && nonWaterSolidFloor && this.properties.has(IsoFlagType.water)) {
            this.properties.unset(IsoFlagType.solidtrans);
        }

        if (nonTransparentSolidFloor && this.properties.has(IsoFlagType.transparentFloor)) {
            this.properties.unset(IsoFlagType.transparentFloor);
        }

        if (nonHoppableCollideN && this.properties.has(IsoFlagType.HoppableN)) {
            this.properties.unset(IsoFlagType.canPathN);
            this.properties.unset(IsoFlagType.HoppableN);
        }

        if (nonHoppableCollideW && this.properties.has(IsoFlagType.HoppableW)) {
            this.properties.unset(IsoFlagType.canPathW);
            this.properties.unset(IsoFlagType.HoppableW);
        }

        if (nonTransparentCutN && this.properties.has(IsoFlagType.transparentN)) {
            this.properties.unset(IsoFlagType.transparentN);
        }

        if (nonTransparentCutW && this.properties.has(IsoFlagType.transparentW)) {
            this.properties.unset(IsoFlagType.transparentW);
        }

        this.propertiesDirty = this.chunk == null || this.chunk.loaded;
        if (this.chunk != null) {
            this.chunk.checkLightingLater_AllPlayers_OneLevel(this.z);
        }

        if (this.chunk != null) {
            this.chunk.checkPhysicsLater(this.z);
            this.chunk.collision.clear();
        }

        this.isExteriorCache = this.has(IsoFlagType.exterior);
        this.isSolidFloorCache = this.has(IsoFlagType.solidfloor);
        this.isVegitationCache = this.has(IsoFlagType.vegitation);
    }

    public void RecalcPropertiesIfNeeded() {
        if (this.propertiesDirty) {
            this.RecalcProperties();
        }
    }

    public void ReCalculateCollide(IsoGridSquare square) {
        this.ReCalculateCollide(square, cellGetSquare);
    }

    public void ReCalculateCollide(IsoGridSquare square, IsoGridSquare.GetSquare getter) {
        if (1 + square.x - this.x < 0 || 1 + square.y - this.y < 0 || 1 + square.z - this.z < 0) {
            DebugLog.log("ERROR");
        }

        boolean b = this.CalculateCollide(square, false, false, false, false, getter);
        this.collideMatrix = setMatrixBit(this.collideMatrix, 1 + square.x - this.x, 1 + square.y - this.y, 1 + square.z - this.z, b);
    }

    public void ReCalculatePathFind(IsoGridSquare square) {
        this.ReCalculatePathFind(square, cellGetSquare);
    }

    public void ReCalculatePathFind(IsoGridSquare square, IsoGridSquare.GetSquare getter) {
        boolean b = this.CalculateCollide(square, false, true, false, false, getter);
        this.pathMatrix = setMatrixBit(this.pathMatrix, 1 + square.x - this.x, 1 + square.y - this.y, 1 + square.z - this.z, b);
    }

    public void ReCalculateVisionBlocked(IsoGridSquare square) {
        this.ReCalculateVisionBlocked(square, cellGetSquare);
    }

    public void ReCalculateVisionBlocked(IsoGridSquare square, IsoGridSquare.GetSquare getter) {
        boolean b = this.CalculateVisionBlocked(square, getter);
        this.visionMatrix = setMatrixBit(this.visionMatrix, 1 + square.x - this.x, 1 + square.y - this.y, 1 + square.z - this.z, b);
    }

    private static boolean testCollideSpecialObjects(IsoMovingObject collideObject, IsoGridSquare sqFrom, IsoGridSquare sqTo) {
        for (int n = 0; n < sqTo.specialObjects.size(); n++) {
            IsoObject obj = sqTo.specialObjects.get(n);
            if (obj.TestCollide(collideObject, sqFrom, sqTo)) {
                if (obj instanceof IsoDoor) {
                    collideObject.setCollidedWithDoor(true);
                } else if (obj instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                    collideObject.setCollidedWithDoor(true);
                }

                collideObject.setCollidedObject(obj);
                return true;
            }
        }

        return false;
    }

    public boolean testCollideAdjacent(IsoMovingObject collideObject, int x, int y, int z) {
        if (collideObject instanceof IsoPlayer isoPlayer && isoPlayer.isNoClip()) {
            return false;
        } else if (this.collideMatrix == -1) {
            return true;
        } else if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= -1 && z <= 1) {
            if (!IsoWorld.instance.metaGrid.isValidChunk((this.x + x) / 8, (this.y + y) / 8)) {
                return true;
            } else {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                if (collideObject != null && collideObject.shouldIgnoreCollisionWithSquare(sq)) {
                    return false;
                } else {
                    if ((GameServer.server || GameClient.client) && collideObject instanceof IsoPlayer isoPlayer && !(collideObject instanceof IsoAnimal)) {
                        IsoGridSquare sqFloor = this.getCell().getGridSquare(this.x + x, this.y + y, 0);
                        boolean allowTrepass = SafeHouse.isSafehouseAllowTrepass(sqFloor, isoPlayer);
                        if (!allowTrepass && (GameServer.server || isoPlayer.isLocalPlayer())) {
                            return true;
                        }
                    }

                    if (sq != null && collideObject != null) {
                        IsoObject obj = this.testCollideSpecialObjects(sq);
                        if (obj != null) {
                            collideObject.collideWith(obj);
                            if (obj instanceof IsoDoor) {
                                collideObject.setCollidedWithDoor(true);
                            } else if (obj instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                                collideObject.setCollidedWithDoor(true);
                            }

                            collideObject.setCollidedObject(obj);
                            return true;
                        }
                    }

                    if (useSlowCollision) {
                        return this.CalculateCollide(sq, false, false, false);
                    } else {
                        if (collideObject instanceof IsoPlayer player && !player.isAnimal() && getMatrixBit(this.collideMatrix, x + 1, y + 1, z + 1)) {
                            this.RecalcAllWithNeighbours(true);
                        }

                        return getMatrixBit(this.collideMatrix, x + 1, y + 1, z + 1);
                    }
                }
            }
        } else {
            return true;
        }
    }

    public boolean testCollideAdjacentAdvanced(int x, int y, int z, boolean ignoreDoors) {
        if (this.collideMatrix == -1) {
            return true;
        } else if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= -1 && z <= 1) {
            IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
            if (sq != null) {
                if (!sq.specialObjects.isEmpty()) {
                    for (int n = 0; n < sq.specialObjects.size(); n++) {
                        IsoObject obj = sq.specialObjects.get(n);
                        if (obj.TestCollide(null, this, sq)) {
                            return true;
                        }
                    }
                }

                if (!this.specialObjects.isEmpty()) {
                    for (int nx = 0; nx < this.specialObjects.size(); nx++) {
                        IsoObject obj = this.specialObjects.get(nx);
                        if (obj.TestCollide(null, this, sq)) {
                            return true;
                        }
                    }
                }
            }

            return useSlowCollision ? this.CalculateCollide(sq, false, false, false) : getMatrixBit(this.collideMatrix, x + 1, y + 1, z + 1);
        } else {
            return true;
        }
    }

    public static void setCollisionMode() {
        useSlowCollision = !useSlowCollision;
    }

    public boolean testPathFindAdjacent(IsoMovingObject mover, int x, int y, int z) {
        return this.testPathFindAdjacent(mover, x, y, z, cellGetSquare);
    }

    public boolean testPathFindAdjacent(IsoMovingObject mover, int x, int y, int z, IsoGridSquare.GetSquare getter) {
        if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= -1 && z <= 1) {
            if (this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
                IsoGridSquare gridSquare = getter.getGridSquare(x + this.x, y + this.y, z + this.z);
                if (gridSquare == null) {
                    return true;
                }

                if (this.has(IsoObjectType.stairsTN) && gridSquare.y < this.y && gridSquare.z == this.z) {
                    return true;
                }

                if (this.has(IsoObjectType.stairsTW) && gridSquare.x < this.x && gridSquare.z == this.z) {
                    return true;
                }
            }

            if (doSlowPathfinding) {
                IsoGridSquare gridSquarex = getter.getGridSquare(x + this.x, y + this.y, z + this.z);
                return this.CalculateCollide(gridSquarex, false, true, false, false, getter);
            } else {
                return getMatrixBit(this.pathMatrix, x + 1, y + 1, z + 1);
            }
        } else {
            return true;
        }
    }

    public LosUtil.TestResults testVisionAdjacent(int x, int y, int z, boolean specialDiag, boolean bIgnoreDoors) {
        if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= -1 && z <= 1) {
            if (z == 1 && (x != 0 || y != 0) && this.HasElevatedFloor()) {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x, this.y, this.z + z);
                if (sq != null) {
                    return sq.testVisionAdjacent(x, y, 0, specialDiag, bIgnoreDoors);
                }
            }

            if (z == -1 && (x != 0 || y != 0)) {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                if (sq != null && sq.HasElevatedFloor()) {
                    return this.testVisionAdjacent(x, y, 0, specialDiag, bIgnoreDoors);
                }
            }

            LosUtil.TestResults test = LosUtil.TestResults.Clear;
            if (x != 0 && y != 0 && specialDiag) {
                test = this.DoDiagnalCheck(x, y, z, bIgnoreDoors);
                if (!GameServer.server
                    && (
                        test == LosUtil.TestResults.Clear
                            || test == LosUtil.TestResults.ClearThroughWindow
                            || test == LosUtil.TestResults.ClearThroughOpenDoor
                            || test == LosUtil.TestResults.ClearThroughClosedDoor
                    )) {
                    IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                    if (sq != null) {
                        test = sq.DoDiagnalCheck(-x, -y, -z, bIgnoreDoors);
                    }
                }

                return test;
            } else {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                LosUtil.TestResults ret = LosUtil.TestResults.Clear;
                if (sq != null && sq.z == this.z) {
                    if (!this.specialObjects.isEmpty()) {
                        for (int n = 0; n < this.specialObjects.size(); n++) {
                            IsoObject obj = this.specialObjects.get(n);
                            if (obj == null) {
                                return LosUtil.TestResults.Clear;
                            }

                            IsoObject.VisionResult vis = obj.TestVision(this, sq);
                            if (vis != IsoObject.VisionResult.NoEffect) {
                                if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoDoor isoDoor) {
                                    ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                                    ret = LosUtil.TestResults.ClearThroughOpenDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoWindow) {
                                    ret = LosUtil.TestResults.ClearThroughWindow;
                                } else {
                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoDoor && !bIgnoreDoors) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked
                                        && obj instanceof IsoThumpable isoThumpable
                                        && isoThumpable.isDoor()
                                        && !bIgnoreDoors) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable isoThumpable && isoThumpable.isWindow()) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoCurtain) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoWindow) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoBarricade) {
                                        return LosUtil.TestResults.Blocked;
                                    }
                                }
                            }
                        }
                    }

                    if (!sq.specialObjects.isEmpty()) {
                        for (int n = 0; n < sq.specialObjects.size(); n++) {
                            IsoObject objx = sq.specialObjects.get(n);
                            if (objx == null) {
                                return LosUtil.TestResults.Clear;
                            }

                            IsoObject.VisionResult vis = objx.TestVision(this, sq);
                            if (vis != IsoObject.VisionResult.NoEffect) {
                                if (vis == IsoObject.VisionResult.Unblocked && objx instanceof IsoDoor isoDoor) {
                                    ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && objx instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                                    ret = LosUtil.TestResults.ClearThroughOpenDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && objx instanceof IsoWindow) {
                                    ret = LosUtil.TestResults.ClearThroughWindow;
                                } else {
                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoDoor && !bIgnoreDoors) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked
                                        && objx instanceof IsoThumpable isoThumpable
                                        && isoThumpable.isDoor()
                                        && !bIgnoreDoors) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoThumpable isoThumpable && isoThumpable.isWindow()) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoCurtain) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoWindow) {
                                        return LosUtil.TestResults.Blocked;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoBarricade) {
                                        return LosUtil.TestResults.Blocked;
                                    }
                                }
                            }
                        }
                    }
                } else if (z > 0
                    && sq != null
                    && (this.z != -1 || sq.z != 0)
                    && sq.getProperties().has(IsoFlagType.exterior)
                    && !this.getProperties().has(IsoFlagType.exterior)) {
                    ret = LosUtil.TestResults.Blocked;
                }

                return !getMatrixBit(this.visionMatrix, x + 1, y + 1, z + 1) ? ret : LosUtil.TestResults.Blocked;
            }
        } else {
            return LosUtil.TestResults.Blocked;
        }
    }

    public boolean TreatAsSolidFloor() {
        if (this.solidFloorCached) {
            return this.solidFloor;
        } else {
            if (!this.properties.has(IsoFlagType.solidfloor) && !this.HasStairs()) {
                this.solidFloor = false;
            } else {
                this.solidFloor = true;
            }

            this.solidFloorCached = true;
            return this.solidFloor;
        }
    }

    public void AddSpecialTileObject(IsoObject obj) {
        this.AddSpecialObject(obj);
    }

    public void renderCharacters(int maxZ, boolean deadRender, boolean doBlendFunc) {
        if (this.z < maxZ) {
            if (!isOnScreenLast) {
            }

            if (doBlendFunc) {
                setBlendFunc();
            }

            if (this.movingObjects.size() > 1) {
                Collections.sort(this.movingObjects, comp);
            }

            int playerIndex = IsoCamera.frameState.playerIndex;
            ColorInfo lightInfo = this.lightInfo[playerIndex];
            int size = this.staticMovingObjects.size();

            for (int n = 0; n < size; n++) {
                IsoMovingObject mov = this.staticMovingObjects.get(n);
                if ((mov.sprite != null || mov instanceof IsoDeadBody)
                    && (!deadRender || mov instanceof IsoDeadBody && !this.HasStairs())
                    && (deadRender || !(mov instanceof IsoDeadBody) || this.HasStairs())) {
                    mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
                }
            }

            size = this.movingObjects.size();

            for (int nx = 0; nx < size; nx++) {
                IsoMovingObject mov = this.movingObjects.get(nx);
                if (mov != null && mov.sprite != null) {
                    boolean bOnFloor = mov.onFloor;
                    if (bOnFloor && mov instanceof IsoZombie zombie) {
                        bOnFloor = zombie.isProne();
                        if (!BaseVehicle.renderToTexture) {
                            bOnFloor = false;
                        }
                    }

                    if ((!deadRender || bOnFloor) && (deadRender || !bOnFloor)) {
                        mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
                    }
                }
            }
        }
    }

    public void renderDeferredCharacters(int maxZ) {
        if (!this.deferedCharacters.isEmpty()) {
            if (this.deferredCharacterTick != this.getCell().deferredCharacterTick) {
                this.deferedCharacters.clear();
            } else if (this.z >= maxZ) {
                this.deferedCharacters.clear();
            } else {
                IndieGL.enableAlphaTest();
                IndieGL.glAlphaFunc(516, 0.0F);
                float sx = IsoUtils.XToScreen(this.x, this.y, this.z, 0);
                float sy = IsoUtils.YToScreen(this.x, this.y, this.z, 0);
                sx -= IsoCamera.frameState.offX;
                sy -= IsoCamera.frameState.offY;
                IndieGL.glColorMask(false, false, false, false);
                Texture.getWhite().renderwallnw(sx, sy, 64 * Core.tileScale, 32 * Core.tileScale, -1, -1, -1, -1, -1, -1);
                IndieGL.glColorMask(true, true, true, true);
                IndieGL.enableAlphaTest();
                IndieGL.glAlphaFunc(516, 0.0F);
                ColorInfo lightInfo = this.lightInfo[IsoCamera.frameState.playerIndex];
                Collections.sort(this.deferedCharacters, comp);

                for (int n = 0; n < this.deferedCharacters.size(); n++) {
                    IsoGameCharacter chr = this.deferedCharacters.get(n);
                    if (chr.sprite != null) {
                        chr.setbDoDefer(false);
                        chr.render(chr.getX(), chr.getY(), chr.getZ(), lightInfo, true, false, null);
                        chr.renderObjectPicker(chr.getX(), chr.getY(), chr.getZ(), lightInfo);
                        chr.setbDoDefer(true);
                    }
                }

                this.deferedCharacters.clear();
                IndieGL.glAlphaFunc(516, 0.0F);
            }
        }
    }

    public void switchLight(boolean active) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject o = this.objects.get(n);
            if (o instanceof IsoLightSwitch isoLightSwitch) {
                isoLightSwitch.setActive(active);
            }
        }
    }

    public void removeLightSwitch() {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject o = this.objects.get(n);
            if (o instanceof IsoLightSwitch) {
                this.RemoveTileObject(o);
                n--;
            }
        }
    }

    public boolean IsOnScreen() {
        return this.IsOnScreen(false);
    }

    public boolean IsOnScreen(boolean halfTileBorder) {
        if (this.cachedScreenValue != Core.tileScale) {
            this.cachedScreenX = IsoUtils.XToScreen(this.x, this.y, this.z, 0);
            this.cachedScreenY = IsoUtils.YToScreen(this.x, this.y, this.z, 0);
            this.cachedScreenValue = Core.tileScale;
        }

        float sx = this.cachedScreenX;
        float sy = this.cachedScreenY;
        sx -= IsoCamera.frameState.offX;
        sy -= IsoCamera.frameState.offY;
        int border = halfTileBorder ? 32 * Core.tileScale : 0;
        if (this.hasTree) {
            int offsetX = 384 * Core.tileScale / 2 - 96 * Core.tileScale;
            int offsetY = 256 * Core.tileScale - 32 * Core.tileScale;
            if (sx + offsetX <= 0 - border) {
                return false;
            } else if (sy + 32 * Core.tileScale <= 0 - border) {
                return false;
            } else {
                return sx - offsetX >= IsoCamera.frameState.offscreenWidth + border ? false : !(sy - offsetY >= IsoCamera.frameState.offscreenHeight + border);
            }
        } else if (sx + 32 * Core.tileScale <= 0 - border) {
            return false;
        } else if (sy + 32 * Core.tileScale <= 0 - border) {
            return false;
        } else {
            return sx - 32 * Core.tileScale >= IsoCamera.frameState.offscreenWidth + border
                ? false
                : !(sy - 96 * Core.tileScale >= IsoCamera.frameState.offscreenHeight + border);
        }
    }

    private static void initWaterSplashCache() {
        for (int i = 0; i < 16; i++) {
            waterSplashCache[i] = Texture.getSharedTexture("media/textures/waterSplashes/WaterSplashSmall0_" + i + ".png");
        }

        for (int i = 16; i < 48; i++) {
            waterSplashCache[i] = Texture.getSharedTexture("media/textures/waterSplashes/WaterSplashBig0_" + i + ".png");
        }

        for (int i = 48; i < 80; i++) {
            waterSplashCache[i] = Texture.getSharedTexture("media/textures/waterSplashes/WaterSplashBig1_" + i + ".png");
        }

        isWaterSplashCacheInitialised = true;
    }

    public void startWaterSplash(boolean isBigSplash, float dx, float dy) {
        if (this.isSeen(IsoCamera.frameState.playerIndex) && !this.waterSplashData.isSplashNow()) {
            if (isBigSplash) {
                this.waterSplashData.initBigSplash(dx, dy);
            } else {
                this.waterSplashData.initSmallSplash(dx, dy);
            }

            FishSplashSoundManager.instance.addSquare(this);
        }
    }

    public void startWaterSplash(boolean isBigSplash) {
        this.startWaterSplash(isBigSplash, Rand.Next(0.0F, 0.5F) - 0.25F, Rand.Next(0.0F, 0.5F) - 0.25F);
    }

    public boolean shouldRenderFishSplash(int playerIndex) {
        if (this.objects.size() != 1) {
            return false;
        } else {
            IsoObject object = this.objects.get(0);
            return object.attachedAnimSprite != null && !object.attachedAnimSprite.isEmpty()
                ? false
                : this.chunk != null && this.isCouldSee(playerIndex) && this.waterSplashData.isSplashNow();
        }
    }

    public ColorInfo getLightInfo(int playerNumber) {
        return this.lightInfo[playerNumber];
    }

    public void cacheLightInfo() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        this.lightInfo[playerIndex] = this.lighting[playerIndex].lightInfo();
    }

    public void setLightInfoServerGUIOnly(ColorInfo c) {
        this.lightInfo[0] = c;
    }

    public int renderFloor(Shader floorShader) {
        int var3;
        try (AbstractPerformanceProfileProbe ignored = IsoGridSquare.s_performance.renderFloor.profile()) {
            var3 = this.renderFloorInternal(floorShader);
        }

        return var3;
    }

    private int renderFloorInternal(Shader floorShader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = this.lightInfo[playerIndex];
        IsoGridSquare camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        boolean bCouldSee = this.lighting[playerIndex].bCouldSee();
        float darkMulti = this.lighting[playerIndex].darkMulti();
        boolean showHighlightColors = GameClient.client && IsoPlayer.players[playerIndex] != null && IsoPlayer.players[playerIndex].isSeeNonPvpZone();
        boolean showSafehouseHighlighting = Core.debug && GameClient.client && SafeHouse.isSafeHouse(this, null, true) != null;
        boolean showMetaAnimalZoneHighlightColors = IsoPlayer.players[playerIndex] != null && IsoPlayer.players[playerIndex].isSeeDesignationZone();
        Double selectedMetaAnimalZone = IsoPlayer.players[playerIndex] != null ? IsoPlayer.players[playerIndex].getSelectedZoneForHighlight() : 0.0;
        boolean objSetAlpha = true;
        float objAlpha = 1.0F;
        float objTargetAlpha = 1.0F;
        if (camCharacterSquare != null) {
            long roomID = this.getRoomID();
            if (roomID != -1L) {
                long playerRoomID = IsoWorld.instance.currentCell.GetEffectivePlayerRoomId();
                if (playerRoomID == -1L && IsoWorld.instance.currentCell.CanBuildingSquareOccludePlayer(this, playerIndex)) {
                    objSetAlpha = false;
                    objAlpha = 1.0F;
                    objTargetAlpha = 1.0F;
                } else if (!bCouldSee && roomID != playerRoomID && darkMulti < 0.5F) {
                    objSetAlpha = false;
                    objAlpha = 0.0F;
                    objTargetAlpha = darkMulti * 2.0F;
                }
            }
        }

        IsoWaterGeometry water = this.z == 0 ? this.getWater() : null;
        boolean isShore = water != null && water.shore;
        float depth0 = water == null ? 0.0F : water.depth[0];
        float depth1 = water == null ? 0.0F : water.depth[3];
        float depth2 = water == null ? 0.0F : water.depth[2];
        float depth3 = water == null ? 0.0F : water.depth[1];
        setBlendFunc();
        int flags = 0;
        int size = this.objects.size();
        IsoObject[] objectArray = this.objects.getElements();

        for (int n = 0; n < size; n++) {
            IsoObject obj = objectArray[n];
            if (showHighlightColors && !obj.isHighlighted(playerIndex)) {
                obj.setHighlighted(playerIndex, true);
                if (NonPvpZone.getNonPvpZone(this.x, this.y) != null) {
                    obj.setHighlightColor(0.6F, 0.6F, 1.0F, 0.5F);
                } else {
                    obj.setHighlightColor(1.0F, 0.6F, 0.6F, 0.5F);
                }
            }

            if (showMetaAnimalZoneHighlightColors) {
                DesignationZone zone = DesignationZone.getZone(this.x, this.y, this.z);
                if (zone != null) {
                    obj.setHighlighted(true);
                    if (selectedMetaAnimalZone > 0.0 && zone.getId().intValue() == selectedMetaAnimalZone.intValue()) {
                        obj.setHighlightColor(0.2F, 0.8F, 0.9F, 0.8F);
                    } else {
                        obj.setHighlightColor(0.2F, 0.2F, 0.9F, 0.8F);
                    }
                }
            }

            if (showSafehouseHighlighting) {
                obj.setHighlighted(true);
                obj.setHighlightColor(1.0F, 0.0F, 0.0F, 1.0F);
            }

            boolean bDoIt = true;
            if (obj.sprite != null && !obj.sprite.solidfloor && obj.sprite.renderLayer != 1) {
                bDoIt = false;
                flags |= 4;
            }

            if (obj instanceof IsoFire || obj instanceof IsoCarBatteryCharger) {
                bDoIt = false;
                flags |= 4;
            }

            if (PerformanceSettings.fboRenderChunk
                && IsoWater.getInstance().getShaderEnable()
                && water != null
                && water.isValid()
                && obj.sprite != null
                && obj.sprite.properties.has(IsoFlagType.water)) {
                bDoIt = false;
            }

            if (!bDoIt) {
                boolean bGrassEtc = obj.sprite != null && (obj.sprite.isBush || obj.sprite.canBeRemoved || obj.sprite.attachedFloor);
                if (this.flattenGrassEtc && bGrassEtc) {
                    flags |= 2;
                }
            } else {
                IndieGL.glAlphaFunc(516, 0.0F);
                obj.setTargetAlpha(playerIndex, objTargetAlpha);
                if (objSetAlpha) {
                    obj.setAlpha(playerIndex, objAlpha);
                }

                if (DebugOptions.instance.terrain.renderTiles.renderGridSquares.getValue() && obj.sprite != null) {
                    IndieGL.StartShader(floorShader, playerIndex);
                    FloorShaper attachedFloorShaper = FloorShaperAttachedSprites.instance;
                    FloorShaper floorShaper;
                    if (!obj.getProperties().has(IsoFlagType.diamondFloor) && !obj.getProperties().has(IsoFlagType.water)) {
                        floorShaper = FloorShaperDeDiamond.instance;
                    } else {
                        floorShaper = FloorShaperDiamond.instance;
                    }

                    int col0 = this.getVertLight(0, playerIndex);
                    int col1 = this.getVertLight(1, playerIndex);
                    int col2 = this.getVertLight(2, playerIndex);
                    int col3 = this.getVertLight(3, playerIndex);
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
                    obj.renderFloorTile(this.x, this.y, this.z, defColorInfo, true, false, floorShader, floorShaper, attachedFloorShaper);
                    IndieGL.StartShader(null);
                }

                flags |= 1;
                if (!obj.isHighlighted(playerIndex)) {
                    flags |= 8;
                }

                if (!PerformanceSettings.fboRenderChunk && obj.isHighlightRenderOnce(playerIndex)) {
                    obj.setHighlighted(playerIndex, false, false);
                }
            }
        }

        if (!FBORenderChunkManager.instance.isCaching() && this.IsOnScreen(true)) {
            IndieGL.glBlendFunc(770, 771);
            this.renderRainSplash(playerIndex, lightInfo);
            this.renderFishSplash(playerIndex, lightInfo);
        }

        return flags;
    }

    public void renderRainSplash(int playerIndex, ColorInfo lightInfo) {
        if ((this.getCell().rainIntensity > 0 || RainManager.isRaining() && RainManager.rainIntensity > 0.0F)
            && this.isExteriorCache
            && !this.isVegitationCache
            && this.isSolidFloorCache
            && this.isCouldSee(playerIndex)) {
            if (!IsoCamera.frameState.paused) {
                int intensity = this.getCell().rainIntensity == 0
                    ? Math.min(PZMath.fastfloor(RainManager.rainIntensity / 0.2F) + 1, 5)
                    : this.getCell().rainIntensity;
                if (this.splashFrame < 0.0F && Rand.Next(Rand.AdjustForFramerate((int)(5.0F / intensity) * 100)) == 0) {
                    this.splashFrame = 0.0F;
                }
            }

            if (this.splashFrame >= 0.0F) {
                int frame = (int)(this.splashFrame * 4.0F);
                if (rainsplashCache[frame] == null) {
                    rainsplashCache[frame] = "RainSplash_00_" + frame;
                }

                Texture tex = Texture.getSharedTexture(rainsplashCache[frame]);
                if (tex != null) {
                    float sx = IsoUtils.XToScreen(this.x + this.splashX, this.y + this.splashY, this.z, 0) - IsoCamera.frameState.offX;
                    float sy = IsoUtils.YToScreen(this.x + this.splashX, this.y + this.splashY, this.z, 0) - IsoCamera.frameState.offY;
                    sx -= tex.getWidth() / 2 * Core.tileScale;
                    sy -= tex.getHeight() / 2 * Core.tileScale;
                    float alpha = 0.6F * (this.getCell().rainIntensity > 0 ? 1.0F : RainManager.rainIntensity);
                    float shaderMod = SceneShaderStore.weatherShader != null ? 0.6F : 1.0F;
                    SpriteRenderer.instance
                        .render(
                            tex,
                            sx,
                            sy,
                            tex.getWidth() * Core.tileScale,
                            tex.getHeight() * Core.tileScale,
                            0.8F * lightInfo.r,
                            0.9F * lightInfo.g,
                            1.0F * lightInfo.b,
                            alpha * shaderMod,
                            null
                        );
                }

                if (!IsoCamera.frameState.paused && this.splashFrameNum != IsoCamera.frameState.frameCount) {
                    this.splashFrame = this.splashFrame + 0.08F * (30.0F / PerformanceSettings.getLockFPS());
                    if (this.splashFrame >= 1.0F) {
                        this.splashX = Rand.Next(0.1F, 0.9F);
                        this.splashY = Rand.Next(0.1F, 0.9F);
                        this.splashFrame = -1.0F;
                    }

                    this.splashFrameNum = IsoCamera.frameState.frameCount;
                }
            }
        } else {
            this.splashFrame = -1.0F;
        }
    }

    public void renderRainSplash(int playerIndex, ColorInfo lightInfo, float splashFrame, boolean bRandomXY) {
        if (!(splashFrame < 0.0F) && (int)(splashFrame * 4.0F) < rainsplashCache.length) {
            if (this.isCouldSee(playerIndex)) {
                if (this.isExteriorCache && !this.isVegitationCache && this.isSolidFloorCache) {
                    int frame = (int)(splashFrame * 4.0F);
                    if (rainsplashCache[frame] == null) {
                        rainsplashCache[frame] = "RainSplash_00_" + frame;
                    }

                    Texture tex = Texture.getSharedTexture(rainsplashCache[frame]);
                    if (tex != null) {
                        if (bRandomXY) {
                            this.splashX = Rand.Next(0.1F, 0.9F);
                            this.splashY = Rand.Next(0.1F, 0.9F);
                        }

                        float sx = IsoUtils.XToScreen(this.x + this.splashX, this.y + this.splashY, this.z, 0) - IsoCamera.frameState.offX;
                        float sy = IsoUtils.YToScreen(this.x + this.splashX, this.y + this.splashY, this.z, 0) - IsoCamera.frameState.offY;
                        sx -= tex.getWidth() / 2 * Core.tileScale;
                        sy -= tex.getHeight() / 2 * Core.tileScale;
                        if (PerformanceSettings.fboRenderChunk) {
                            TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(
                                            PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                                            PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                                            this.x + this.splashX + 0.1F,
                                            this.y + this.splashY + 0.1F,
                                            this.z
                                        )
                                        .depthStart
                                    * 2.0F
                                - 1.0F;
                            SpriteRenderer.instance.StartShader(0, playerIndex);
                            IndieGL.enableDepthTest();
                            IndieGL.glDepthFunc(515);
                            IndieGL.glDepthMask(false);
                        }

                        IndieGL.glBlendFunc(770, 771);
                        float alpha = 0.6F * (this.getCell().rainIntensity > 0 ? 1.0F : RainManager.rainIntensity);
                        float shaderMod = 1.0F;
                        SpriteRenderer.instance
                            .render(
                                tex,
                                sx,
                                sy,
                                tex.getWidth() * Core.tileScale,
                                tex.getHeight() * Core.tileScale,
                                0.8F * lightInfo.r,
                                0.9F * lightInfo.g,
                                1.0F * lightInfo.b,
                                alpha * 1.0F,
                                null
                            );
                    }
                }
            }
        }
    }

    public void renderFishSplash(int playerIndex, ColorInfo lightInfo) {
        if (this.isCouldSee(playerIndex) && this.waterSplashData.isSplashNow()) {
            Texture tex = this.waterSplashData.getTexture();
            if (tex != null) {
                float texWidth = tex.getWidth() * this.waterSplashData.size;
                float texHeight = tex.getHeight() * this.waterSplashData.size;
                float sx = IsoUtils.XToScreen(this.x + this.waterSplashData.dx, this.y + this.waterSplashData.dy, 0.0F, 0) - IsoCamera.frameState.offX;
                float sy = IsoUtils.YToScreen(this.x + this.waterSplashData.dx, this.y + this.waterSplashData.dy, 0.0F, 0) - IsoCamera.frameState.offY;
                if (PerformanceSettings.fboRenderChunk) {
                    sx = IsoUtils.XToScreen(this.x + 0.5F, this.y + 0.5F, this.z, 0) - IsoCamera.frameState.offX - texWidth / 2.0F;
                    sy = IsoUtils.YToScreen(this.x + 0.5F, this.y + 0.5F, this.z, 0) - IsoCamera.frameState.offY - texHeight / 2.0F;
                    TextureDraw.nextZ = (
                                IsoDepthHelper.getSquareDepthData(
                                            PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                                            PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                                            this.x + 0.99F,
                                            this.y + 0.99F,
                                            this.z
                                        )
                                        .depthStart
                                    + 0.001F
                            )
                            * 2.0F
                        - 1.0F;
                    SpriteRenderer.instance.StartShader(0, playerIndex);
                    IndieGL.enableDepthTest();
                    IndieGL.glDepthFunc(515);
                    IndieGL.glDepthMask(false);
                }

                sx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * IsoCamera.frameState.zoom;
                sy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * IsoCamera.frameState.zoom;
                float alpha = 1.0F;
                float shaderMod = 1.0F;
                SpriteRenderer.instance.render(tex, sx, sy, texWidth, texHeight, 0.8F * lightInfo.r, 0.9F * lightInfo.g, lightInfo.b, 1.0F, null);
                this.waterSplashData.update();
            }
        }
    }

    public boolean isSpriteOnSouthOrEastWall(IsoObject obj) {
        if (obj instanceof IsoBarricade) {
            return obj.getDir() == IsoDirections.S || obj.getDir() == IsoDirections.E;
        } else if (obj instanceof IsoCurtain curtain) {
            return curtain.getType() == IsoObjectType.curtainS || curtain.getType() == IsoObjectType.curtainE;
        } else {
            PropertyContainer properties = obj.getProperties();
            return properties != null && (properties.has(IsoFlagType.attachedE) || properties.has(IsoFlagType.attachedS));
        }
    }

    public void RenderOpenDoorOnly() {
        int numObjects = this.objects.size();
        IsoObject[] objectArray = this.objects.getElements();

        try {
            int start = 0;
            int end = numObjects - 1;

            for (int n = 0; n <= end; n++) {
                IsoObject obj = objectArray[n];
                if (obj.sprite != null && (obj.sprite.getProperties().has(IsoFlagType.attachedN) || obj.sprite.getProperties().has(IsoFlagType.attachedW))) {
                    obj.renderFxMask(this.x, this.y, this.z, false);
                }
            }
        } catch (Exception var7) {
            ExceptionLogger.logException(var7);
        }
    }

    public boolean RenderMinusFloorFxMask(int maxZ, boolean doSE, boolean vegitationRender) {
        boolean hasSE = false;
        int numObjects = this.objects.size();
        IsoObject[] objectArray = this.objects.getElements();
        long currentTimeMillis = System.currentTimeMillis();

        try {
            int start = doSE ? numObjects - 1 : 0;
            int end = doSE ? 0 : numObjects - 1;

            for (int n = start; doSE ? n >= end : n <= end; n += doSE ? -1 : 1) {
                IsoObject obj = objectArray[n];
                if (obj.sprite != null) {
                    boolean bDoIt = true;
                    IsoObjectType t = obj.sprite.getType();
                    if (obj.sprite.solidfloor || obj.sprite.renderLayer == 1) {
                        bDoIt = false;
                    }

                    if (this.z >= maxZ && !obj.sprite.alwaysDraw) {
                        bDoIt = false;
                    }

                    boolean bGrassEtc = obj.sprite.isBush || obj.sprite.canBeRemoved || obj.sprite.attachedFloor;
                    if ((!vegitationRender || bGrassEtc && this.flattenGrassEtc) && (vegitationRender || !bGrassEtc || !this.flattenGrassEtc)) {
                        if ((t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT)
                            && this.z == maxZ - 1
                            && this.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                            bDoIt = false;
                        }

                        if (this.isSpriteOnSouthOrEastWall(obj)) {
                            if (!doSE) {
                                bDoIt = false;
                            }

                            hasSE = true;
                        } else if (doSE) {
                            bDoIt = false;
                        }

                        if (bDoIt) {
                            if (!obj.sprite.cutW && !obj.sprite.cutN) {
                                obj.renderFxMask(this.x, this.y, this.z, false);
                            } else {
                                int playerIndex = IsoCamera.frameState.playerIndex;
                                boolean N = obj.sprite.cutN;
                                boolean W = obj.sprite.cutW;
                                IsoGridSquare squareN = this.nav[IsoDirections.N.index()];
                                IsoGridSquare squareS = this.nav[IsoDirections.S.index()];
                                IsoGridSquare squareW = this.nav[IsoDirections.W.index()];
                                IsoGridSquare squareE = this.nav[IsoDirections.E.index()];
                                int cutawaySelf = this.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                IsoDirections dir;
                                if (N && W) {
                                    dir = IsoDirections.NW;
                                } else if (N) {
                                    dir = IsoDirections.N;
                                } else if (W) {
                                    dir = IsoDirections.W;
                                } else {
                                    dir = IsoDirections.W;
                                }

                                this.DoCutawayShaderSprite(obj.sprite, dir, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE);
                            }
                        }
                    }
                }
            }
        } catch (Exception var29) {
            ExceptionLogger.logException(var29);
        }

        return hasSE;
    }

    public boolean isWindowOrWindowFrame(IsoObject obj, boolean north) {
        if (obj != null && obj.sprite != null) {
            if (north && obj.sprite.getProperties().has(IsoFlagType.windowN)) {
                return true;
            } else if (!north && obj.sprite.getProperties().has(IsoFlagType.windowW)) {
                return true;
            } else {
                IsoThumpable thumpable = Type.tryCastTo(obj, IsoThumpable.class);
                if (thumpable != null && thumpable.isWindow()) {
                    return north == thumpable.getNorth();
                } else {
                    return obj instanceof IsoWindowFrame windowFrame ? windowFrame.getNorth() == north : false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean renderMinusFloor(
        int maxZ, boolean doSE, boolean vegitationRender, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, Shader wallRenderShader
    ) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.renderMinusFloor.getValue()) {
            return false;
        } else {
            setBlendFunc();
            int stenciled = 0;
            isOnScreenLast = this.IsOnScreen();
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoGridSquare CamCharacterSquare = IsoCamera.frameState.camCharacterSquare;
            ColorInfo lightInfo = this.lightInfo[playerIndex];
            boolean bCouldSee = this.lighting[playerIndex].bCouldSee();
            float darkMulti = this.lighting[playerIndex].darkMulti();
            boolean playerOccluderSqr = IsoWorld.instance.currentCell.CanBuildingSquareOccludePlayer(this, playerIndex);
            lightInfo.a = 1.0F;
            defColorInfo.r = 1.0F;
            defColorInfo.g = 1.0F;
            defColorInfo.b = 1.0F;
            defColorInfo.a = 1.0F;
            if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                lightInfo = defColorInfo;
            }

            float sx = this.cachedScreenX - IsoCamera.frameState.offX;
            float sy = this.cachedScreenY - IsoCamera.frameState.offY;
            boolean bInStencilRect = true;
            IsoCell cell = this.getCell();
            if (sx + 32 * Core.tileScale <= cell.stencilX1
                || sx - 32 * Core.tileScale >= cell.stencilX2
                || sy + 32 * Core.tileScale <= cell.stencilY1
                || sy - 96 * Core.tileScale >= cell.stencilY2) {
                bInStencilRect = false;
            }

            boolean hasSE = false;
            int numObjects = this.objects.size();
            IsoObject[] objectArray = this.objects.getElements();
            tempWorldInventoryObjects.clear();
            int start = doSE ? numObjects - 1 : 0;
            int end = doSE ? 0 : numObjects - 1;
            boolean bHasSeenDoorN = false;
            boolean bHasSeenDoorW = false;
            boolean bHasSeenWindowN = false;
            boolean bHasSeenWindowW = false;
            if (!doSE) {
                for (int n = start; n <= end; n++) {
                    IsoObject obj = objectArray[n];
                    if (this.isWindowOrWindowFrame(obj, true) && (cutawaySelf & 1) != 0) {
                        IsoGridSquare toNorth = this.nav[IsoDirections.N.index()];
                        bHasSeenWindowN = bCouldSee || toNorth != null && toNorth.isCouldSee(playerIndex);
                    }

                    if (this.isWindowOrWindowFrame(obj, false) && (cutawaySelf & 2) != 0) {
                        IsoGridSquare toWest = this.nav[IsoDirections.W.index()];
                        bHasSeenWindowW = bCouldSee || toWest != null && toWest.isCouldSee(playerIndex);
                    }

                    if (obj.sprite != null
                        && (obj.sprite.getType() == IsoObjectType.doorFrN || obj.sprite.getType() == IsoObjectType.doorN)
                        && (cutawaySelf & 1) != 0) {
                        IsoGridSquare toNorth = this.nav[IsoDirections.N.index()];
                        bHasSeenDoorN = bCouldSee || toNorth != null && toNorth.isCouldSee(playerIndex);
                    }

                    if (obj.sprite != null
                        && (obj.sprite.getType() == IsoObjectType.doorFrW || obj.sprite.getType() == IsoObjectType.doorW)
                        && (cutawaySelf & 2) != 0) {
                        IsoGridSquare toWest = this.nav[IsoDirections.W.index()];
                        bHasSeenDoorW = bCouldSee || toWest != null && toWest.isCouldSee(playerIndex);
                    }
                }
            }

            long playerRoomID = IsoWorld.instance.currentCell.GetEffectivePlayerRoomId();
            wallCutawayN = false;
            wallCutawayW = false;

            for (int n = start; doSE ? n >= end : n <= end; n += doSE ? -1 : 1) {
                IsoObject objx = objectArray[n];
                boolean bDoIt = true;
                IsoObjectType t = IsoObjectType.MAX;
                if (objx.sprite != null) {
                    t = objx.sprite.getType();
                }

                circleStencil = false;
                if (objx.sprite != null && (objx.sprite.solidfloor || objx.sprite.renderLayer == 1)) {
                    bDoIt = false;
                }

                if (objx instanceof IsoFire) {
                    bDoIt = !vegitationRender;
                }

                if (this.z >= maxZ && (objx.sprite == null || !objx.sprite.alwaysDraw)) {
                    bDoIt = false;
                }

                boolean bGrassEtc = objx.sprite != null && (objx.sprite.isBush || objx.sprite.canBeRemoved || objx.sprite.attachedFloor);
                if ((!vegitationRender || bGrassEtc && this.flattenGrassEtc) && (vegitationRender || !bGrassEtc || !this.flattenGrassEtc)) {
                    if (objx.sprite != null
                        && (t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT)
                        && this.z == maxZ - 1
                        && this.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                        bDoIt = false;
                    }

                    boolean isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || objx.sprite != null && objx.sprite.cutW;
                    boolean isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || objx.sprite != null && objx.sprite.cutN;
                    boolean isOpenDoor = objx instanceof IsoDoor isoDoor && isoDoor.open || objx instanceof IsoThumpable isoThumpable && isoThumpable.open;
                    boolean isContainer = objx.container != null;
                    boolean isPlumbed = objx.sprite != null && objx.sprite.getProperties().has(IsoFlagType.waterPiped);
                    if (objx.sprite != null
                        && t == IsoObjectType.MAX
                        && !(objx instanceof IsoDoor)
                        && !(objx instanceof IsoWindow)
                        && !isContainer
                        && !isPlumbed) {
                        if (isWestDoorOrWall || !objx.sprite.getProperties().has(IsoFlagType.attachedW) || !playerOccluderSqr && (cutawaySelf & 2) == 0) {
                            if (!isNorthDoorOrWall && objx.sprite.getProperties().has(IsoFlagType.attachedN) && (playerOccluderSqr || (cutawaySelf & 1) != 0)) {
                                bDoIt = !wallCutawayN;
                            }
                        } else {
                            bDoIt = !wallCutawayW;
                        }
                    }

                    if (objx.sprite != null && !objx.sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
                        bDoIt = true;
                    }

                    if (this.isSpriteOnSouthOrEastWall(objx)) {
                        if (!doSE) {
                            bDoIt = false;
                        }

                        hasSE = true;
                    } else if (doSE) {
                        bDoIt = false;
                    }

                    if (PerformanceSettings.fboRenderChunk) {
                        boolean bTranslucent = objx.getRenderInfo(playerIndex).layer == ObjectRenderLayer.Translucent;
                        if (FBORenderCell.instance.renderTranslucentOnly != bTranslucent) {
                            bDoIt = false;
                        }
                    }

                    if (bDoIt) {
                        IndieGL.glAlphaFunc(516, 0.0F);
                        objx.alphaForced = false;
                        if (isOpenDoor) {
                            objx.setTargetAlpha(playerIndex, 0.6F);
                            objx.setAlpha(playerIndex, 0.6F);
                        }

                        if (objx.sprite == null || !isWestDoorOrWall && !isNorthDoorOrWall) {
                            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.objects.getValue()) {
                                if (this.getRoomID() != -1L
                                    && this.getRoomID() != playerRoomID
                                    && IsoPlayer.players[playerIndex].isSeatedInVehicle()
                                    && IsoPlayer.players[playerIndex].getVehicle().getCurrentSpeedKmHour() >= 50.0F) {
                                    break;
                                }

                                IsoPlayer pl = IsoPlayer.players[playerIndex];
                                boolean withinRoofFadeDist = IsoUtils.DistanceToSquared(
                                        pl.getX(), pl.getY(), this.x - 1.5F - (this.z - pl.getZ() * 3.0F), this.y - 1.5F - (this.z - pl.getZ() * 3.0F)
                                    )
                                    <= 30.0F;
                                IsoGridSquare sqAtZero = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
                                IsoGridSquare sqZeroN = null;
                                IsoGridSquare sqZeroW = null;
                                boolean sqZeroHasWallN = false;
                                boolean sqZeroHasWallW = false;
                                if (sqAtZero != null) {
                                    sqZeroN = sqAtZero.nav[IsoDirections.N.index()];
                                    sqZeroW = sqAtZero.nav[IsoDirections.W.index()];
                                    sqZeroHasWallN = sqAtZero.getWall(true) != null || sqAtZero.getDoor(true) != null || sqAtZero.getWindow(true) != null;
                                    sqZeroHasWallW = sqAtZero.getWall(false) != null || sqAtZero.getDoor(false) != null || sqAtZero.getWindow(false) != null;
                                }

                                boolean roofFadeDist = IsoUtils.DistanceToSquared(
                                        pl.getX(), pl.getY(), this.x - 1.5F - (this.z - pl.getZ() * 3.0F), this.y - 1.5F - (this.z - pl.getZ() * 3.0F)
                                    )
                                    <= 30.0F;
                                if ((t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT)
                                    && (
                                        this.getRoomID() == -1L
                                                && (
                                                    (cutawayE != 0 || cutawayS != 0) && (pl.getX() < this.x && pl.getY() < this.y || pl.getZ() < this.z)
                                                        || sqAtZero != null && sqAtZero.getRoomID() == -1L && roofFadeDist && pl.getZ() < this.z + 1
                                                )
                                            || (this.getRoomID() != -1L || playerRoomID != -1L) && roofFadeDist && pl.getZ() < this.z + 1
                                    )) {
                                    sqZeroN = sqAtZero.nav[IsoDirections.N.index()];
                                    sqZeroW = sqAtZero.nav[IsoDirections.W.index()];
                                    sqZeroHasWallN = sqAtZero.getWall(true) != null || sqAtZero.getDoor(true) != null || sqAtZero.getWindow(true) != null;
                                    sqZeroHasWallW = sqAtZero.getWall(false) != null || sqAtZero.getDoor(false) != null || sqAtZero.getWindow(false) != null;
                                }

                                if (t != IsoObjectType.WestRoofB && t != IsoObjectType.WestRoofM && t != IsoObjectType.WestRoofT
                                    || (
                                            this.getRoomID() != -1L
                                                || (cutawayE == 0 && cutawayS == 0 || (!(pl.getX() < this.x) || !(pl.getY() < this.y)) && !(pl.getZ() < this.z))
                                                    && (
                                                        sqAtZero == null
                                                            || sqAtZero.getRoomID() != -1L
                                                            || !withinRoofFadeDist && playerRoomID == -1L
                                                            || !(pl.getZ() < this.z + 1)
                                                            || (!sqZeroHasWallN || sqZeroN == null || sqZeroN.getRoomID() != -1L)
                                                                && (!sqZeroHasWallW || sqZeroW == null || sqZeroW.getRoomID() != -1L)
                                                                && (sqZeroHasWallN || sqZeroHasWallW)
                                                    )
                                        )
                                        && (this.getRoomID() == -1L || playerRoomID == -1L || !withinRoofFadeDist || !(pl.getZ() < this.z + 1))) {
                                    if (CamCharacterSquare != null && !bCouldSee && this.getRoomID() != playerRoomID && darkMulti < 0.5F) {
                                        if (objx.getProperties() != null && objx.getProperties().has("forceFade")) {
                                            objx.setTargetAlpha(playerIndex, 0.0F);
                                        } else {
                                            objx.setTargetAlpha(playerIndex, darkMulti * 2.0F);
                                        }
                                    } else {
                                        if (!isOpenDoor) {
                                            objx.setTargetAlpha(playerIndex, 1.0F);
                                        }

                                        if (IsoPlayer.getInstance() != null
                                                && objx.getProperties() != null
                                                && (
                                                    objx.getProperties().has(IsoFlagType.solid)
                                                        || objx.getProperties().has(IsoFlagType.solidtrans)
                                                        || objx.getProperties().has(IsoFlagType.attachedCeiling)
                                                        || objx.getSprite().getProperties().has(IsoFlagType.attachedE)
                                                        || objx.getSprite().getProperties().has(IsoFlagType.attachedS)
                                                )
                                            || t.index() > 2 && t.index() < 9 && IsoCamera.frameState.camCharacterZ <= objx.getZ()) {
                                            int transRange = 3;
                                            float transAlpha = 0.75F;
                                            if (t.index() > 2 && t.index() < 9
                                                || objx.getSprite().getProperties().has(IsoFlagType.attachedE)
                                                || objx.getSprite().getProperties().has(IsoFlagType.attachedS)
                                                || objx.getProperties().has(IsoFlagType.attachedCeiling)) {
                                                transRange = 4;
                                                if (t.index() > 2 && t.index() < 9) {
                                                    transAlpha = 0.5F;
                                                }
                                            }

                                            if (objx.sprite.solid || objx.sprite.solidTrans) {
                                                transRange = 5;
                                                transAlpha = 0.25F;
                                            }

                                            int dx = this.getX() - PZMath.fastfloor(IsoPlayer.getInstance().getX());
                                            int dy = this.getY() - PZMath.fastfloor(IsoPlayer.getInstance().getY());
                                            if (dx >= 0 && dx < transRange && dy >= 0 && dy < transRange
                                                || dy >= 0 && dy < transRange && dx >= 0 && dx < transRange) {
                                                objx.setTargetAlpha(playerIndex, transAlpha);
                                            }

                                            IsoZombie nearestVisibleZombie = IsoCell.getInstance().getNearestVisibleZombie(playerIndex);
                                            if (nearestVisibleZombie != null
                                                && nearestVisibleZombie.getCurrentSquare() != null
                                                && nearestVisibleZombie.getCurrentSquare().isCanSee(playerIndex)) {
                                                int zx = this.getX() - PZMath.fastfloor(nearestVisibleZombie.getX());
                                                int zy = this.getY() - PZMath.fastfloor(nearestVisibleZombie.getY());
                                                if (zx > 0 && zx < transRange && zy >= 0 && zy < transRange
                                                    || zy > 0 && zy < transRange && zx >= 0 && zx < transRange) {
                                                    objx.setTargetAlpha(playerIndex, transAlpha);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    objx.setTargetAlpha(playerIndex, 0.0F);
                                }

                                if (objx instanceof IsoWindow w) {
                                    if (objx.getTargetAlpha(playerIndex) < 1.0E-4F) {
                                        IsoGridSquare oppositeSq = w.getOppositeSquare();
                                        if (oppositeSq != null && oppositeSq != this && oppositeSq.lighting[playerIndex].bSeen()) {
                                            objx.setTargetAlpha(playerIndex, oppositeSq.lighting[playerIndex].darkMulti() * 2.0F);
                                        }
                                    }

                                    if (objx.getTargetAlpha(playerIndex) > 0.4F
                                        && cutawaySelf != 0
                                        && (
                                            cutawayE != 0 && objx.sprite.getProperties().has(IsoFlagType.windowN)
                                                || cutawayS != 0 && objx.sprite.getProperties().has(IsoFlagType.windowW)
                                        )) {
                                        float maxOpacity = 0.4F;
                                        float minOpacity = 0.1F;
                                        IsoPlayer player = IsoPlayer.players[playerIndex];
                                        if (player != null) {
                                            float maxFadeDistance = 5.0F;
                                            float distanceSquared = Math.abs(player.getX() - this.x) * Math.abs(player.getX() - this.x)
                                                + Math.abs(player.getY() - this.y) * Math.abs(player.getY() - this.y);
                                            float fadeAmount = 0.4F * (float)(1.0 - Math.sqrt(distanceSquared / 5.0F));
                                            objx.setTargetAlpha(playerIndex, Math.max(fadeAmount, 0.1F));
                                        } else {
                                            objx.setTargetAlpha(playerIndex, 0.1F);
                                        }

                                        if (cutawayE != 0) {
                                            wallCutawayN = true;
                                        } else {
                                            wallCutawayW = true;
                                        }
                                    }
                                }

                                if (objx instanceof IsoTree isoTree) {
                                    if (bInStencilRect
                                        && this.x >= PZMath.fastfloor(IsoCamera.frameState.camCharacterX)
                                        && this.y >= PZMath.fastfloor(IsoCamera.frameState.camCharacterY)
                                        && CamCharacterSquare != null
                                        && CamCharacterSquare.has(IsoFlagType.exterior)) {
                                        isoTree.renderFlag = true;
                                        objx.setTargetAlpha(playerIndex, Math.min(0.99F, objx.getTargetAlpha(playerIndex)));
                                    } else {
                                        isoTree.renderFlag = false;
                                    }
                                }

                                if (objx instanceof IsoWorldInventoryObject worldObj) {
                                    tempWorldInventoryObjects.add(worldObj);
                                } else {
                                    if (!PerformanceSettings.fboRenderChunk && objx.getAlpha(playerIndex) < 1.0F) {
                                        IndieGL.glBlendFunc(770, 771);
                                    }

                                    objx.render(this.x, this.y, this.z, lightInfo, true, false, null);
                                }
                            }
                        } else if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.doorsAndWalls.getValue()) {
                            circleStencil = true;
                            if (CamCharacterSquare != null && this.getRoomID() != -1L && playerRoomID == -1L && playerOccluderSqr) {
                                objx.setTargetAlpha(playerIndex, 0.5F);
                                objx.setAlpha(playerIndex, 0.5F);
                            } else if (this.getRoomID() != playerRoomID
                                && !bCouldSee
                                && (objx.getProperties().has(IsoFlagType.transparentN) || objx.getProperties().has(IsoFlagType.transparentW))
                                && objx.getSpriteName() != null
                                && objx.getSpriteName().contains("police")) {
                                objx.setTargetAlpha(playerIndex, 0.0F);
                                objx.setAlpha(playerIndex, 0.0F);
                            } else if (!isOpenDoor) {
                                objx.setTargetAlpha(playerIndex, 1.0F);
                                objx.setAlpha(playerIndex, 1.0F);
                            }

                            objx.alphaForced = true;
                            if (objx.sprite.cutW && objx.sprite.cutN) {
                                stenciled = this.DoWallLightingNW(
                                    objx,
                                    stenciled,
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
                            } else if (objx.sprite.getType() == IsoObjectType.doorFrW || t == IsoObjectType.doorW || objx.sprite.cutW) {
                                stenciled = this.DoWallLightingW(
                                    objx, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorW, bHasSeenWindowW, wallRenderShader
                                );
                            } else if (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || objx.sprite.cutN) {
                                stenciled = this.DoWallLightingN(
                                    objx, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorN, bHasSeenWindowN, wallRenderShader
                                );
                            }

                            if (objx instanceof IsoWindow && objx.getTargetAlpha(playerIndex) < 1.0F) {
                                wallCutawayN = wallCutawayN | objx.sprite.cutN;
                                wallCutawayW = wallCutawayW | objx.sprite.cutW;
                            }
                        }

                        if (!PerformanceSettings.fboRenderChunk && objx.isHighlightRenderOnce(playerIndex)) {
                            objx.setHighlighted(playerIndex, false, false);
                        }
                    }
                }
            }

            Arrays.sort(tempWorldInventoryObjects.getElements(), 0, tempWorldInventoryObjects.size(), (o1, o2) -> {
                float d1 = o1.xoff * o1.xoff + o1.yoff * o1.yoff;
                float d2 = o2.xoff * o2.xoff + o2.yoff * o2.yoff;
                if (d1 == d2) {
                    return 0;
                } else {
                    return d1 > d2 ? 1 : -1;
                }
            });

            for (int i = 0; i < tempWorldInventoryObjects.size(); i++) {
                IsoWorldInventoryObject worldObj = tempWorldInventoryObjects.get(i);
                worldObj.render(this.x, this.y, this.z, lightInfo, true, false, null);
            }

            return hasSE;
        }
    }

    void RereouteWallMaskTo(IsoObject obj) {
        for (int n = 0; n < this.objects.size(); n++) {
            IsoObject objTest = this.objects.get(n);
            if (objTest.sprite.getProperties().has(IsoFlagType.collideW) || objTest.sprite.getProperties().has(IsoFlagType.collideN)) {
                objTest.rerouteMask = obj;
            }
        }
    }

    void setBlockedGridPointers(IsoGridSquare.GetSquare getter) {
        this.w = getter.getGridSquare(this.x - 1, this.y, this.z);
        this.e = getter.getGridSquare(this.x + 1, this.y, this.z);
        this.s = getter.getGridSquare(this.x, this.y + 1, this.z);
        this.n = getter.getGridSquare(this.x, this.y - 1, this.z);
        this.ne = getter.getGridSquare(this.x + 1, this.y - 1, this.z);
        this.nw = getter.getGridSquare(this.x - 1, this.y - 1, this.z);
        this.se = getter.getGridSquare(this.x + 1, this.y + 1, this.z);
        this.sw = getter.getGridSquare(this.x - 1, this.y + 1, this.z);
        this.u = getter.getGridSquare(this.x, this.y, this.z + 1);
        this.d = getter.getGridSquare(this.x, this.y, this.z - 1);
        if (this.u != null && (this.u.properties.has(IsoFlagType.solidfloor) || this.u.properties.has(IsoFlagType.solid))) {
            this.u = null;
        }

        if (this.d != null && (this.properties.has(IsoFlagType.solidfloor) || this.properties.has(IsoFlagType.solid))) {
            this.d = null;
        }

        if (this.s != null && this.testPathFindAdjacent(null, this.s.x - this.x, this.s.y - this.y, this.s.z - this.z, getter)) {
            this.s = null;
        }

        if (this.w != null && this.testPathFindAdjacent(null, this.w.x - this.x, this.w.y - this.y, this.w.z - this.z, getter)) {
            this.w = null;
        }

        if (this.n != null && this.testPathFindAdjacent(null, this.n.x - this.x, this.n.y - this.y, this.n.z - this.z, getter)) {
            this.n = null;
        }

        if (this.e != null && this.testPathFindAdjacent(null, this.e.x - this.x, this.e.y - this.y, this.e.z - this.z, getter)) {
            this.e = null;
        }

        if (this.sw != null && this.testPathFindAdjacent(null, this.sw.x - this.x, this.sw.y - this.y, this.sw.z - this.z, getter)) {
            this.sw = null;
        }

        if (this.se != null && this.testPathFindAdjacent(null, this.se.x - this.x, this.se.y - this.y, this.se.z - this.z, getter)) {
            this.se = null;
        }

        if (this.nw != null && this.testPathFindAdjacent(null, this.nw.x - this.x, this.nw.y - this.y, this.nw.z - this.z, getter)) {
            this.nw = null;
        }

        if (this.ne != null && this.testPathFindAdjacent(null, this.ne.x - this.x, this.ne.y - this.y, this.ne.z - this.z, getter)) {
            this.ne = null;
        }
    }

    public IsoObject getContainerItem(String type) {
        int numObjects = this.getObjects().size();
        IsoObject[] objectArray = this.getObjects().getElements();

        for (int i = 0; i < numObjects; i++) {
            IsoObject o = objectArray[i];
            if (o.getContainer() != null && type.equals(o.getContainer().getType())) {
                return o;
            }
        }

        return null;
    }

    @Deprecated
    public void StartFire() {
    }

    public int getHourLastSeen() {
        return this.hourLastSeen;
    }

    public float getHoursSinceLastSeen() {
        return (float)GameTime.instance.getWorldAgeHours() - this.hourLastSeen;
    }

    public void CalcVisibility(int playerIndex, IsoGameCharacter isoGameCharacter, VisibilityData visibilityData) {
        IsoGridSquare.ILighting lighting = this.lighting[playerIndex];
        lighting.bCanSee(false);
        lighting.bCouldSee(false);
        if (!GameServer.server && isoGameCharacter.isDead() && isoGameCharacter.reanimatedCorpse == null) {
            lighting.bSeen(true);
            lighting.bCanSee(true);
            lighting.bCouldSee(true);
        } else {
            IsoGameCharacter.LightInfo lightInfo = isoGameCharacter.getLightInfo2();
            IsoGridSquare currentSquare = lightInfo.square;
            if (currentSquare != null) {
                IsoChunk chk = this.getChunk();
                if (chk != null) {
                    tempo.x = this.x + 0.5F;
                    tempo.y = this.y + 0.5F;
                    tempo2.x = lightInfo.x;
                    tempo2.y = lightInfo.y;
                    tempo2.x = tempo2.x - tempo.x;
                    tempo2.y = tempo2.y - tempo.y;
                    Vector2 dir = tempo;
                    float dist = tempo2.getLength();
                    tempo2.normalize();
                    if (isoGameCharacter instanceof IsoSurvivor) {
                        isoGameCharacter.setForwardDirection(dir);
                        lightInfo.angleX = dir.x;
                        lightInfo.angleY = dir.y;
                    }

                    dir.x = lightInfo.angleX;
                    dir.y = lightInfo.angleY;
                    dir.normalize();
                    float dot = tempo2.dot(dir);
                    if (currentSquare == this) {
                        dot = -1.0F;
                    }

                    if (!GameServer.server) {
                        float fatigue = visibilityData.getFatigue();
                        float noiseDistance = visibilityData.getNoiseDistance();
                        if (dist < noiseDistance * (1.0F - fatigue) && !isoGameCharacter.hasTrait(CharacterTrait.DEAF)) {
                            dot = -1.0F;
                        }
                    }

                    LosUtil.TestResults test = LosUtil.lineClearCached(
                        this.getCell(),
                        this.x,
                        this.y,
                        this.z,
                        PZMath.fastfloor(lightInfo.x),
                        PZMath.fastfloor(lightInfo.y),
                        PZMath.fastfloor(lightInfo.z),
                        false,
                        playerIndex
                    );
                    float cone = visibilityData.getCone();
                    if (!(dot > cone) && test != LosUtil.TestResults.Blocked) {
                        lighting.bCouldSee(true);
                        if (this.room != null && this.room.def != null) {
                            if (!this.room.def.explored) {
                                int dist1 = 10;
                                if (lightInfo.square != null && lightInfo.square.getBuilding() == this.room.building) {
                                    dist1 = 50;
                                }

                                if ((!(GameServer.server && isoGameCharacter instanceof IsoPlayer isoPlayer) || !isoPlayer.isGhostMode())
                                    && IsoUtils.DistanceManhatten(lightInfo.x, lightInfo.y, this.x, this.y) < dist1
                                    && this.z == PZMath.fastfloor(lightInfo.z)) {
                                    if (GameServer.server) {
                                        DebugLog.log(DebugType.Zombie, "bExplored room=" + this.room.def.id);
                                    }

                                    this.room.def.explored = true;
                                    this.room.onSee();
                                    this.room.seen = 0;
                                }
                            }

                            if (!GameClient.client) {
                                Meta.instance.dealWithSquareSeen(this);
                            }

                            lighting.bCanSee(true);
                            lighting.bSeen(true);
                            lighting.targetDarkMulti(1.0F);
                        }
                    } else {
                        lighting.bCouldSee(test != LosUtil.TestResults.Blocked);
                        if (!GameServer.server) {
                            if (lighting.bSeen()) {
                                float amb = visibilityData.getBaseAmbient();
                                if (!lighting.bCouldSee()) {
                                    amb *= 0.5F;
                                } else {
                                    amb *= 0.94F;
                                }

                                if (this.room == null && currentSquare.getRoom() == null) {
                                    lighting.targetDarkMulti(amb);
                                } else if (this.room != null && currentSquare.getRoom() != null && this.room.building == currentSquare.getRoom().building) {
                                    if (this.room != currentSquare.getRoom() && !lighting.bCouldSee()) {
                                        lighting.targetDarkMulti(0.0F);
                                    } else {
                                        lighting.targetDarkMulti(amb);
                                    }
                                } else if (this.room == null) {
                                    lighting.targetDarkMulti(amb / 2.0F);
                                } else if (lighting.lampostTotalR() + lighting.lampostTotalG() + lighting.lampostTotalB() == 0.0F) {
                                    lighting.targetDarkMulti(0.0F);
                                }

                                if (this.room != null) {
                                    lighting.targetDarkMulti(lighting.targetDarkMulti() * 0.7F);
                                }
                            } else {
                                lighting.targetDarkMulti(0.0F);
                                lighting.darkMulti(0.0F);
                            }
                        }
                    }

                    if (dot > cone) {
                        lighting.targetDarkMulti(lighting.targetDarkMulti() * 0.85F);
                    }

                    if (!GameServer.server) {
                        for (int i = 0; i < lightInfo.torches.size(); i++) {
                            IsoGameCharacter.TorchInfo torch = lightInfo.torches.get(i);
                            tempo2.x = torch.x;
                            tempo2.y = torch.y;
                            tempo2.x = tempo2.x - (this.x + 0.5F);
                            tempo2.y = tempo2.y - (this.y + 0.5F);
                            dist = tempo2.getLength();
                            tempo2.normalize();
                            dir.x = torch.angleX;
                            dir.y = torch.angleY;
                            dir.normalize();
                            dot = tempo2.dot(dir);
                            if (PZMath.fastfloor(torch.x) == this.getX()
                                && PZMath.fastfloor(torch.y) == this.getY()
                                && PZMath.fastfloor(torch.z) == this.getZ()) {
                                dot = -1.0F;
                            }

                            boolean isTorchDot = IsoUtils.DistanceManhatten(this.getX(), this.getY(), torch.x, torch.y) < torch.dist
                                && (torch.cone && dot < -torch.dot || dot == -1.0F || !torch.cone && dot < 0.8F);
                            if ((torch.cone && dist < torch.dist || !torch.cone && dist < torch.dist)
                                && lighting.bCanSee()
                                && isTorchDot
                                && this.z == PZMath.fastfloor(isoGameCharacter.getZ())) {
                                float del = dist / torch.dist;
                                if (del > 1.0F) {
                                    del = 1.0F;
                                }

                                if (del < 0.0F) {
                                    del = 0.0F;
                                }

                                lighting.targetDarkMulti(lighting.targetDarkMulti() + torch.strength * (1.0F - del) * 3.0F);
                                if (lighting.targetDarkMulti() > 2.5F) {
                                    lighting.targetDarkMulti(2.5F);
                                }

                                torchTimer = lightInfo.time;
                            }
                        }
                    }
                }
            }
        }
    }

    private LosUtil.TestResults DoDiagnalCheck(int x, int y, int z, boolean bIgnoreDoors) {
        LosUtil.TestResults res = this.testVisionAdjacent(x, 0, z, false, bIgnoreDoors);
        if (res == LosUtil.TestResults.Blocked) {
            return LosUtil.TestResults.Blocked;
        } else {
            LosUtil.TestResults res2 = this.testVisionAdjacent(0, y, z, false, bIgnoreDoors);
            if (res2 == LosUtil.TestResults.Blocked) {
                return LosUtil.TestResults.Blocked;
            } else {
                return res != LosUtil.TestResults.ClearThroughWindow && res2 != LosUtil.TestResults.ClearThroughWindow
                    ? this.testVisionAdjacent(x, y, z, false, bIgnoreDoors)
                    : LosUtil.TestResults.ClearThroughWindow;
            }
        }
    }

    boolean HasNoCharacters() {
        for (int n = 0; n < this.movingObjects.size(); n++) {
            if (this.movingObjects.get(n) instanceof IsoGameCharacter) {
                return false;
            }
        }

        for (int nx = 0; nx < this.specialObjects.size(); nx++) {
            if (this.specialObjects.get(nx) instanceof IsoBarricade) {
                return false;
            }
        }

        return true;
    }

    public IsoZombie getZombie() {
        for (int n = 0; n < this.movingObjects.size(); n++) {
            if (this.movingObjects.get(n) instanceof IsoZombie) {
                return (IsoZombie)this.movingObjects.get(n);
            }
        }

        return null;
    }

    public IsoPlayer getPlayer() {
        for (int n = 0; n < this.movingObjects.size(); n++) {
            if (this.movingObjects.get(n) instanceof IsoPlayer) {
                return (IsoPlayer)this.movingObjects.get(n);
            }
        }

        return null;
    }

    /**
     * @return the darkStep
     */
    public static float getDarkStep() {
        return darkStep;
    }

    /**
     * 
     * @param aDarkStep the darkStep to set
     */
    public static void setDarkStep(float aDarkStep) {
        darkStep = aDarkStep;
    }

    public static float getRecalcLightTime() {
        return recalcLightTime;
    }

    public static void setRecalcLightTime(float aRecalcLightTime) {
        recalcLightTime = aRecalcLightTime;
        if (PerformanceSettings.fboRenderChunk && aRecalcLightTime < 0.0F) {
            Core.dirtyGlobalLightsCount++;
        }
    }

    /**
     * @return the lightcache
     */
    public static int getLightcache() {
        return lightcache;
    }

    /**
     * 
     * @param aLightcache the lightcache to set
     */
    public static void setLightcache(int aLightcache) {
        lightcache = aLightcache;
    }

    /**
     * @return the bCouldSee
     */
    public boolean isCouldSee(int playerIndex) {
        return this.lighting[playerIndex].bCouldSee();
    }

    /**
     * 
     * @param playerIndex
     * @param bCouldSee the bCouldSee to set
     */
    public void setCouldSee(int playerIndex, boolean bCouldSee) {
        this.lighting[playerIndex].bCouldSee(bCouldSee);
    }

    /**
     * @return the canSee
     */
    public boolean isCanSee(int playerIndex) {
        return this.lighting[playerIndex].bCanSee();
    }

    /**
     * 
     * @param playerIndex
     * @param canSee the canSee to set
     */
    public void setCanSee(int playerIndex, boolean canSee) {
        this.lighting[playerIndex].bCanSee(canSee);
    }

    /**
     * @return the getCell()
     */
    public IsoCell getCell() {
        return IsoWorld.instance.currentCell;
    }

    /**
     * @return the e
     */
    public IsoGridSquare getE() {
        return this.e;
    }

    /**
     * 
     * @param e the e to set
     */
    public void setE(IsoGridSquare e) {
        this.e = e;
    }

    /**
     * @return the LightInfluenceB
     */
    public ArrayList<Float> getLightInfluenceB() {
        return this.lightInfluenceB;
    }

    /**
     * 
     * @param LightInfluenceB the LightInfluenceB to set
     */
    public void setLightInfluenceB(ArrayList<Float> LightInfluenceB) {
        this.lightInfluenceB = LightInfluenceB;
    }

    /**
     * @return the LightInfluenceG
     */
    public ArrayList<Float> getLightInfluenceG() {
        return this.lightInfluenceG;
    }

    /**
     * 
     * @param LightInfluenceG the LightInfluenceG to set
     */
    public void setLightInfluenceG(ArrayList<Float> LightInfluenceG) {
        this.lightInfluenceG = LightInfluenceG;
    }

    /**
     * @return the LightInfluenceR
     */
    public ArrayList<Float> getLightInfluenceR() {
        return this.lightInfluenceR;
    }

    /**
     * 
     * @param LightInfluenceR the LightInfluenceR to set
     */
    public void setLightInfluenceR(ArrayList<Float> LightInfluenceR) {
        this.lightInfluenceR = LightInfluenceR;
    }

    /**
     * @return the StaticMovingObjects
     */
    public ArrayList<IsoMovingObject> getStaticMovingObjects() {
        return this.staticMovingObjects;
    }

    /**
     * @return the MovingObjects
     */
    public ArrayList<IsoMovingObject> getMovingObjects() {
        return this.movingObjects;
    }

    /**
     * @return the n
     */
    public IsoGridSquare getN() {
        return this.n;
    }

    /**
     * 
     * @param n the n to set
     */
    public void setN(IsoGridSquare n) {
        this.n = n;
    }

    /**
     * @return the Objects
     */
    public PZArrayList<IsoObject> getObjects() {
        return this.objects;
    }

    /**
     * @return the Properties
     */
    public PropertyContainer getProperties() {
        return this.properties;
    }

    /**
     * @return the room
     */
    public IsoRoom getRoom() {
        return this.roomId == -1L ? null : this.room;
    }

    /**
     * 
     * @param room the room to set
     */
    public void setRoom(IsoRoom room) {
        this.room = room;
    }

    public IsoBuilding getBuilding() {
        IsoRoom room = this.getRoom();
        return room != null ? room.getBuilding() : null;
    }

    /**
     * @return the s
     */
    public IsoGridSquare getS() {
        return this.s;
    }

    /**
     * 
     * @param s the s to set
     */
    public void setS(IsoGridSquare s) {
        this.s = s;
    }

    /**
     * @return the SpecialObjects
     */
    public ArrayList<IsoObject> getSpecialObjects() {
        return this.specialObjects;
    }

    /**
     * @return the w
     */
    public IsoGridSquare getW() {
        return this.w;
    }

    /**
     * 
     * @param w the w to set
     */
    public void setW(IsoGridSquare w) {
        this.w = w;
    }

    /**
     * @return the lampostTotalR
     */
    public float getLampostTotalR() {
        return this.lighting[0].lampostTotalR();
    }

    /**
     * 
     * @param lampostTotalR the lampostTotalR to set
     */
    public void setLampostTotalR(float lampostTotalR) {
        this.lighting[0].lampostTotalR(lampostTotalR);
    }

    /**
     * @return the lampostTotalG
     */
    public float getLampostTotalG() {
        return this.lighting[0].lampostTotalG();
    }

    /**
     * 
     * @param lampostTotalG the lampostTotalG to set
     */
    public void setLampostTotalG(float lampostTotalG) {
        this.lighting[0].lampostTotalG(lampostTotalG);
    }

    /**
     * @return the lampostTotalB
     */
    public float getLampostTotalB() {
        return this.lighting[0].lampostTotalB();
    }

    /**
     * 
     * @param lampostTotalB the lampostTotalB to set
     */
    public void setLampostTotalB(float lampostTotalB) {
        this.lighting[0].lampostTotalB(lampostTotalB);
    }

    /**
     * @return the bSeen
     */
    public boolean isSeen(int playerIndex) {
        return this.lighting[playerIndex].bSeen();
    }

    /**
     * 
     * @param playerIndex
     * @param bSeen the bSeen to set
     */
    public void setIsSeen(int playerIndex, boolean bSeen) {
        this.lighting[playerIndex].bSeen(bSeen);
    }

    /**
     * @return the darkMulti
     */
    public float getDarkMulti(int playerIndex) {
        return this.lighting[playerIndex].darkMulti();
    }

    /**
     * 
     * @param playerIndex
     * @param darkMulti the darkMulti to set
     */
    public void setDarkMulti(int playerIndex, float darkMulti) {
        this.lighting[playerIndex].darkMulti(darkMulti);
    }

    /**
     * @return the targetDarkMulti
     */
    public float getTargetDarkMulti(int playerIndex) {
        return this.lighting[playerIndex].targetDarkMulti();
    }

    /**
     * 
     * @param playerIndex
     * @param targetDarkMulti the targetDarkMulti to set
     */
    public void setTargetDarkMulti(int playerIndex, float targetDarkMulti) {
        this.lighting[playerIndex].targetDarkMulti(targetDarkMulti);
    }

    /**
     * 
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
        this.cachedScreenValue = -1;
    }

    /**
     * 
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
        this.cachedScreenValue = -1;
    }

    /**
     * 
     * @param z the z to set
     */
    public void setZ(int z) {
        z = Math.max(-32, z);
        z = Math.min(31, z);
        this.z = z;
        this.cachedScreenValue = -1;
    }

    /**
     * @return the DeferedCharacters
     */
    public ArrayList<IsoGameCharacter> getDeferedCharacters() {
        return this.deferedCharacters;
    }

    public void addDeferredCharacter(IsoGameCharacter chr) {
        if (this.deferredCharacterTick != this.getCell().deferredCharacterTick) {
            if (!this.deferedCharacters.isEmpty()) {
                this.deferedCharacters.clear();
            }

            this.deferredCharacterTick = this.getCell().deferredCharacterTick;
        }

        this.deferedCharacters.add(chr);
    }

    /**
     * @return the CacheIsFree
     */
    public boolean isCacheIsFree() {
        return this.cacheIsFree;
    }

    /**
     * 
     * @param CacheIsFree the CacheIsFree to set
     */
    public void setCacheIsFree(boolean CacheIsFree) {
        this.cacheIsFree = CacheIsFree;
    }

    /**
     * @return the CachedIsFree
     */
    public boolean isCachedIsFree() {
        return this.cachedIsFree;
    }

    /**
     * 
     * @param CachedIsFree the CachedIsFree to set
     */
    public void setCachedIsFree(boolean CachedIsFree) {
        this.cachedIsFree = CachedIsFree;
    }

    /**
     * @return the bDoSlowPathfinding
     */
    public static boolean isbDoSlowPathfinding() {
        return doSlowPathfinding;
    }

    /**
     * 
     * @param abDoSlowPathfinding the bDoSlowPathfinding to set
     */
    public static void setbDoSlowPathfinding(boolean abDoSlowPathfinding) {
        doSlowPathfinding = abDoSlowPathfinding;
    }

    /**
     * @return the SolidFloorCached
     */
    public boolean isSolidFloorCached() {
        return this.solidFloorCached;
    }

    /**
     * 
     * @param SolidFloorCached the SolidFloorCached to set
     */
    public void setSolidFloorCached(boolean SolidFloorCached) {
        this.solidFloorCached = SolidFloorCached;
    }

    /**
     * @return the SolidFloor
     */
    public boolean isSolidFloor() {
        return this.solidFloor;
    }

    /**
     * 
     * @param SolidFloor the SolidFloor to set
     */
    public void setSolidFloor(boolean SolidFloor) {
        this.solidFloor = SolidFloor;
    }

    /**
     * @return the defColorInfo
     */
    public static ColorInfo getDefColorInfo() {
        return defColorInfo;
    }

    public boolean isOutside() {
        return this.properties.has(IsoFlagType.exterior);
    }

    public boolean HasPushable() {
        int size = this.movingObjects.size();

        for (int n = 0; n < size; n++) {
            if (this.movingObjects.get(n) instanceof IsoPushableObject) {
                return true;
            }
        }

        return false;
    }

    public void setRoomID(long roomId) {
        this.roomId = roomId;
        if (roomId != -1L) {
            this.getProperties().unset(IsoFlagType.exterior);
            this.room = this.chunk.getRoom(roomId);
        }
    }

    public long getRoomID() {
        return this.roomId;
    }

    public String getRoomIDString() {
        return String.valueOf(this.getRoomID());
    }

    public boolean getCanSee(int playerIndex) {
        return this.lighting[playerIndex].bCanSee();
    }

    public boolean getSeen(int playerIndex) {
        return this.lighting[playerIndex].bSeen();
    }

    public IsoChunk getChunk() {
        return this.chunk;
    }

    public IsoObject getDoorOrWindow(boolean north) {
        for (int n = this.specialObjects.size() - 1; n >= 0; n--) {
            IsoObject s = this.specialObjects.get(n);
            if (s instanceof IsoDoor isoDoor && isoDoor.north == north) {
                return s;
            }

            if (s instanceof IsoThumpable isoThumpable && isoThumpable.north == north && (isoThumpable.isDoor() || isoThumpable.isWindow())) {
                return s;
            }

            if (s instanceof IsoWindow isoWindow && isoWindow.isNorth() == north) {
                return s;
            }
        }

        return null;
    }

    public IsoObject getDoorOrWindowOrWindowFrame(IsoDirections dir, boolean ignoreOpen) {
        for (int i = this.objects.size() - 1; i >= 0; i--) {
            IsoObject obj = this.objects.get(i);
            IsoDoor door = Type.tryCastTo(obj, IsoDoor.class);
            IsoThumpable thumpable = Type.tryCastTo(obj, IsoThumpable.class);
            IsoWindow window = Type.tryCastTo(obj, IsoWindow.class);
            if (door != null && door.getSpriteEdge(ignoreOpen) == dir) {
                return obj;
            }

            if (thumpable != null && thumpable.getSpriteEdge(ignoreOpen) == dir) {
                return obj;
            }

            if (window != null) {
                if (window.isNorth() && dir == IsoDirections.N) {
                    return obj;
                }

                if (!window.isNorth() && dir == IsoDirections.W) {
                    return obj;
                }
            }

            if (obj instanceof IsoWindowFrame windowFrame) {
                if (windowFrame.getNorth() && dir == IsoDirections.N) {
                    return obj;
                }

                if (!windowFrame.getNorth() && dir == IsoDirections.W) {
                    return obj;
                }
            }
        }

        return null;
    }

    public IsoObject getOpenDoor(IsoDirections dir) {
        for (int i = 0; i < this.specialObjects.size(); i++) {
            IsoObject obj = this.specialObjects.get(i);
            IsoDoor door = Type.tryCastTo(obj, IsoDoor.class);
            IsoThumpable thumpable = Type.tryCastTo(obj, IsoThumpable.class);
            if (door != null && door.open && door.getSpriteEdge(false) == dir) {
                return door;
            }

            if (thumpable != null && thumpable.open && thumpable.getSpriteEdge(false) == dir) {
                return thumpable;
            }
        }

        return null;
    }

    public void removeWorldObject(IsoWorldInventoryObject object) {
        if (object != null) {
            object.invalidateRenderChunkLevel(136L);
            object.removeFromWorld();
            object.removeFromSquare();
        }
    }

    public void removeAllWorldObjects() {
        for (int i = 0; i < this.getWorldObjects().size(); i++) {
            IsoObject object = this.getWorldObjects().get(i);
            object.invalidateRenderChunkLevel(136L);
            object.removeFromWorld();
            object.removeFromSquare();
            i--;
        }
    }

    public ArrayList<IsoWorldInventoryObject> getWorldObjects() {
        return this.worldObjects;
    }

    public int getNextNonItemObjectIndex(int index) {
        for (int i = index; i < this.getObjects().size(); i++) {
            IsoObject object = this.getObjects().get(i);
            if (!(object instanceof IsoWorldInventoryObject)) {
                return i;
            }
        }

        return -1;
    }

    public KahluaTable getModData() {
        if (this.table == null) {
            this.table = LuaManager.platform.newTable();
        }

        return this.table;
    }

    public boolean hasModData() {
        return this.table != null && !this.table.isEmpty();
    }

    public void setVertLight(int i, int col, int playerIndex) {
        this.lighting[playerIndex].lightverts(i, col);
    }

    public int getVertLight(int i, int playerIndex) {
        return this.lighting[playerIndex].lightverts(i);
    }

    public void setRainDrop(IsoRaindrop drop) {
        this.rainDrop = drop;
    }

    public IsoRaindrop getRainDrop() {
        return this.rainDrop;
    }

    public void setRainSplash(IsoRainSplash splash) {
        this.rainSplash = splash;
    }

    public IsoRainSplash getRainSplash() {
        return this.rainSplash;
    }

    public Zone getZone() {
        return this.zone;
    }

    public String getZoneType() {
        return this.zone != null ? this.zone.getType() : null;
    }

    public boolean isOverlayDone() {
        return this.overlayDone;
    }

    public void setOverlayDone(boolean overlayDone) {
        this.overlayDone = overlayDone;
    }

    public ErosionData.Square getErosionData() {
        if (this.erosion == null) {
            this.erosion = new ErosionData.Square();
        }

        return this.erosion;
    }

    public void disableErosion() {
        ErosionData.Square erosionModData = this.getErosionData();
        if (erosionModData != null && !erosionModData.doNothing) {
            erosionModData.doNothing = true;
        }
    }

    public void removeErosionObject(String type) {
        if (this.erosion != null) {
            if ("WallVines".equals(type)) {
                for (int i = 0; i < this.erosion.regions.size(); i++) {
                    ErosionCategory.Data sqCategoryData = this.erosion.regions.get(i);
                    if (sqCategoryData.regionId == 2 && sqCategoryData.categoryId == 0) {
                        this.erosion.regions.remove(i);
                        break;
                    }
                }
            }
        }
    }

    public void syncIsoTrap(HandWeapon weapon) {
        AddExplosiveTrapPacket packet = new AddExplosiveTrapPacket();
        packet.set(weapon, this);
        ByteBufferWriter b = GameClient.connection.startPacket();
        PacketTypes.PacketType.AddExplosiveTrap.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.AddExplosiveTrap.send(GameClient.connection);
    }

    public int getTrapPositionX() {
        return this.trapPositionX;
    }

    public void setTrapPositionX(int trapPositionX) {
        this.trapPositionX = trapPositionX;
    }

    public int getTrapPositionY() {
        return this.trapPositionY;
    }

    public void setTrapPositionY(int trapPositionY) {
        this.trapPositionY = trapPositionY;
    }

    public int getTrapPositionZ() {
        return this.trapPositionZ;
    }

    public void setTrapPositionZ(int trapPositionZ) {
        this.trapPositionZ = trapPositionZ;
    }

    public boolean haveElectricity() {
        return !SandboxOptions.getInstance().allowExteriorGenerator.getValue() && this.has(IsoFlagType.exterior)
            ? false
            : this.chunk != null && this.chunk.isGeneratorPoweringSquare(this.x, this.y, this.z);
    }

    @Deprecated
    public void setHaveElectricity(boolean haveElectricity) {
        if (this.getObjects() != null) {
            for (int i = 0; i < this.getObjects().size(); i++) {
                if (this.getObjects().get(i) instanceof IsoLightSwitch) {
                    this.getObjects().get(i).update();
                }
            }
        }
    }

    public IsoGenerator getGenerator() {
        if (this.getSpecialObjects() != null) {
            for (int i = 0; i < this.getSpecialObjects().size(); i++) {
                if (this.getSpecialObjects().get(i) instanceof IsoGenerator) {
                    return (IsoGenerator)this.getSpecialObjects().get(i);
                }
            }
        }

        return null;
    }

    public void stopFire() {
        IsoFireManager.RemoveAllOn(this);
        this.getProperties().set(IsoFlagType.burntOut);
        this.getProperties().unset(IsoFlagType.burning);
        this.burntOut = true;
    }

    public void transmitStopFire() {
        if (GameClient.client) {
            GameClient.sendStopFire(this);
        }
    }

    public long playSound(String file) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.x + 0.5F, this.y + 0.5F, this.z);
        return emitter.playSound(file);
    }

    public long playSoundLocal(String file) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.x + 0.5F, this.y + 0.5F, this.z);
        return emitter.playSoundImpl(file, this);
    }

    @Deprecated
    public long playSound(String file, boolean doWorldSound) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.x + 0.5F, this.y + 0.5F, this.z);
        return emitter.playSound(file, doWorldSound);
    }

    public void FixStackableObjects() {
        IsoObject table = null;

        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject obj = this.objects.get(i);
            if (!(obj instanceof IsoWorldInventoryObject) && obj.sprite != null) {
                PropertyContainer props = obj.sprite.getProperties();
                if (props.getStackReplaceTileOffset() != 0) {
                    obj.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, obj.sprite.id + props.getStackReplaceTileOffset());
                    if (obj.sprite == null) {
                        continue;
                    }

                    props = obj.sprite.getProperties();
                }

                if (props.isTable() || props.isTableTop()) {
                    float offset = props.isSurfaceOffset() ? props.getSurface() : 0.0F;
                    if (table != null) {
                        obj.setRenderYOffset(table.getRenderYOffset() + table.getSurfaceOffset() - offset);
                    } else {
                        obj.setRenderYOffset(0.0F - offset);
                    }
                }

                if (props.isTable()) {
                    table = obj;
                }

                if (obj instanceof IsoLightSwitch && props.isTableTop() && table != null && !props.has("IgnoreSurfaceSnap")) {
                    int NOffset = PZMath.tryParseInt(props.get("Noffset"), 0);
                    int SOffset = PZMath.tryParseInt(props.get("Soffset"), 0);
                    int WOffset = PZMath.tryParseInt(props.get("Woffset"), 0);
                    int EOffset = PZMath.tryParseInt(props.get("Eoffset"), 0);
                    String ownFacing = props.get("Facing");
                    PropertyContainer tableProps = table.getProperties();
                    String tableFacing = tableProps.get("Facing");
                    if (!StringUtils.isNullOrWhitespace(tableFacing) && !tableFacing.equals(ownFacing)) {
                        int offset = 0;
                        if ("N".equals(tableFacing)) {
                            if (NOffset != 0) {
                                offset = NOffset;
                            } else if (SOffset != 0) {
                                offset = SOffset;
                            }
                        } else if ("S".equals(tableFacing)) {
                            if (SOffset != 0) {
                                offset = SOffset;
                            } else if (NOffset != 0) {
                                offset = NOffset;
                            }
                        } else if ("W".equals(tableFacing)) {
                            if (WOffset != 0) {
                                offset = WOffset;
                            } else if (EOffset != 0) {
                                offset = EOffset;
                            }
                        } else if ("E".equals(tableFacing)) {
                            if (EOffset != 0) {
                                offset = EOffset;
                            } else if (WOffset != 0) {
                                offset = WOffset;
                            }
                        }

                        if (offset != 0) {
                            IsoSprite newSprite = IsoSpriteManager.instance.getSprite(obj.sprite.id + offset);
                            if (newSprite != null) {
                                obj.setSprite(newSprite);
                            }
                        }
                    }
                }
            }
        }
    }

    public void fixPlacedItemRenderOffsets() {
        IsoObject[] objects = this.objects.getElements();
        int nObjects = this.objects.size();
        int nOffsets = 0;

        for (int i = 0; i < nObjects; i++) {
            IsoObject obj = objects[i];
            int surfaceOffset = PZMath.roundToInt(obj.getSurfaceOffsetNoTable());
            if (!(surfaceOffset <= 0.0F) && !PZArrayUtil.contains(SURFACE_OFFSETS, nOffsets, surfaceOffset)) {
                SURFACE_OFFSETS[nOffsets++] = surfaceOffset;
            }
        }

        if (nOffsets == 0) {
            SURFACE_OFFSETS[nOffsets++] = 0;
        }

        for (int ix = 0; ix < nObjects; ix++) {
            if (objects[ix] instanceof IsoWorldInventoryObject worldObj && !worldObj.isExtendedPlacement()) {
                int renderOffset = PZMath.roundToInt(worldObj.zoff * 96.0F);
                int newOffset = 0;

                for (int j = 0; j < nOffsets; j++) {
                    if (renderOffset <= SURFACE_OFFSETS[j]) {
                        newOffset = SURFACE_OFFSETS[j];
                        break;
                    }

                    newOffset = SURFACE_OFFSETS[j];
                    if (j < nOffsets - 1 && renderOffset < SURFACE_OFFSETS[j + 1]) {
                        break;
                    }
                }

                worldObj.zoff = newOffset / 96.0F;
            }
        }
    }

    public BaseVehicle getVehicleContainer() {
        int chunkMinX = PZMath.fastfloor((this.x - 4.0F) / 8.0F);
        int chunkMinY = PZMath.fastfloor((this.y - 4.0F) / 8.0F);
        int chunkMaxX = (int)Math.ceil((this.x + 4.0F) / 8.0F);
        int chunkMaxY = (int)Math.ceil((this.y + 4.0F) / 8.0F);

        for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
            for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        if (vehicle.isIntersectingSquare(this.x, this.y, this.z)) {
                            return vehicle;
                        }
                    }
                }
            }
        }

        return null;
    }

    public boolean isVehicleIntersecting() {
        int chunkMinX = PZMath.fastfloor((this.x - 4.0F) / 8.0F);
        int chunkMinY = PZMath.fastfloor((this.y - 4.0F) / 8.0F);
        int chunkMaxX = (int)Math.ceil((this.x + 4.0F) / 8.0F);
        int chunkMaxY = (int)Math.ceil((this.y + 4.0F) / 8.0F);

        for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
            for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        if (vehicle.isIntersectingSquare(this.x, this.y, this.z)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean isVehicleIntersectingCrops() {
        if (!this.hasFarmingPlant()) {
            return false;
        } else {
            int chunkMinX = PZMath.fastfloor((this.x - 4.0F) / 8.0F);
            int chunkMinY = PZMath.fastfloor((this.y - 4.0F) / 8.0F);
            int chunkMaxX = (int)Math.ceil((this.x + 4.0F) / 8.0F);
            int chunkMaxY = (int)Math.ceil((this.y + 4.0F) / 8.0F);

            for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle.isIntersectingSquare(this) && !vehicle.notKillCrops()) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    public void checkForIntersectingCrops(BaseVehicle vehicle) {
        if (this.hasFarmingPlant() && !vehicle.notKillCrops() && vehicle.isIntersectingSquare(this)) {
            this.destroyFarmingPlant();
        }
    }

    public IsoCompost getCompost() {
        if (this.getSpecialObjects() != null) {
            for (int i = 0; i < this.getSpecialObjects().size(); i++) {
                if (this.getSpecialObjects().get(i) instanceof IsoCompost) {
                    return (IsoCompost)this.getSpecialObjects().get(i);
                }
            }
        }

        return null;
    }

    public <T> PZArrayList<ItemContainer> getAllContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate, PZArrayList<ItemContainer> inout_containerList
    ) {
        this.getObjectContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
        this.getVehicleItemContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
        return inout_containerList;
    }

    public <T> PZArrayList<ItemContainer> getAllContainersFromAdjacentSquare(
        IsoDirections in_dir,
        T in_paramToCompare,
        Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate,
        PZArrayList<ItemContainer> inout_containerList
    ) {
        IsoGridSquare adjSquare = this.getAdjacentSquare(in_dir);
        return adjSquare.getAllContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
    }

    public <T> PZArrayList<ItemContainer> getObjectContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate, PZArrayList<ItemContainer> inout_containerList
    ) {
        PZArrayList<IsoObject> adjObj = this.getObjects();

        for (int i = 0; i < adjObj.size(); i++) {
            IsoObject obj = adjObj.get(i);
            obj.getContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
        }

        return inout_containerList;
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate
    ) {
        PZArrayList<ItemContainer> containerList = new PZArrayList<>(ItemContainer.class, 10);
        return this.getVehicleItemContainers(in_paramToCompare, in_isValidPredicate, containerList);
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate, PZArrayList<ItemContainer> inout_containerList
    ) {
        BaseVehicle vehicle = this.getVehicleContainer();
        boolean hasVehicle = vehicle != null;
        return !hasVehicle ? inout_containerList : vehicle.getVehicleItemContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
    }

    public void setIsoWorldRegion(IsoWorldRegion mr) {
        this.hasSetIsoWorldRegion = mr != null;
        this.isoWorldRegion = mr;
    }

    public IWorldRegion getIsoWorldRegion() {
        if (this.z < 0) {
            return null;
        } else if (GameServer.server) {
            return IsoRegions.getIsoWorldRegion(this.x, this.y, this.z);
        } else {
            if (!this.hasSetIsoWorldRegion) {
                this.isoWorldRegion = IsoRegions.getIsoWorldRegion(this.x, this.y, this.z);
                this.hasSetIsoWorldRegion = true;
            }

            return this.isoWorldRegion;
        }
    }

    public void ResetIsoWorldRegion() {
        this.isoWorldRegion = null;
        this.hasSetIsoWorldRegion = false;
    }

    public boolean isInARoom() {
        return this.getRoom() != null || this.getIsoWorldRegion() != null && this.getIsoWorldRegion().isPlayerRoom();
    }

    public int getRoomSize() {
        if (this.getRoom() != null) {
            return this.getRoom().getSquares().size();
        } else {
            return this.getIsoWorldRegion() != null && this.getIsoWorldRegion().isPlayerRoom() ? this.getIsoWorldRegion().getSquareSize() : -1;
        }
    }

    public int getWallType() {
        int type = 0;
        if (this.getProperties().has(IsoFlagType.WallN)) {
            type |= 1;
        }

        if (this.getProperties().has(IsoFlagType.WallW)) {
            type |= 4;
        }

        if (this.getProperties().has(IsoFlagType.WallNW)) {
            type |= 5;
        }

        IsoGridSquare sqE = this.nav[IsoDirections.E.index()];
        if (sqE != null && (sqE.getProperties().has(IsoFlagType.WallW) || sqE.getProperties().has(IsoFlagType.WallNW))) {
            type |= 8;
        }

        IsoGridSquare sqS = this.nav[IsoDirections.S.index()];
        if (sqS != null && (sqS.getProperties().has(IsoFlagType.WallN) || sqS.getProperties().has(IsoFlagType.WallNW))) {
            type |= 2;
        }

        return type;
    }

    public int getPuddlesDir() {
        byte dir = 8;
        if (this.isInARoom()) {
            return 1;
        } else {
            for (int i = 0; i < this.getObjects().size(); i++) {
                IsoObject obj = this.getObjects().get(i);
                if (obj.attachedAnimSprite != null) {
                    for (int j = 0; j < obj.attachedAnimSprite.size(); j++) {
                        IsoSprite attached = obj.attachedAnimSprite.get(j).parentSprite;
                        if (attached.name != null) {
                            if (attached.name.equals("street_trafficlines_01_2")
                                || attached.name.equals("street_trafficlines_01_6")
                                || attached.name.equals("street_trafficlines_01_22")
                                || attached.name.equals("street_trafficlines_01_32")) {
                                dir = 4;
                            }

                            if (attached.name.equals("street_trafficlines_01_4")
                                || attached.name.equals("street_trafficlines_01_0")
                                || attached.name.equals("street_trafficlines_01_16")) {
                                dir = 2;
                            }
                        }
                    }
                }
            }

            return dir;
        }
    }

    public boolean haveFire() {
        int size = this.objects.size();
        IsoObject[] objectArray = this.objects.getElements();

        for (int n = 0; n < size; n++) {
            IsoObject obj = objectArray[n];
            if (obj instanceof IsoFire) {
                return true;
            }
        }

        return false;
    }

    public IsoBuilding getRoofHideBuilding() {
        return this.roofHideBuilding;
    }

    public IsoGridSquare getAdjacentSquare(IsoDirections in_dir) {
        return this.nav[in_dir.index()];
    }

    public void setAdjacentSquare(IsoDirections in_dir, IsoGridSquare in_square) {
        this.nav[in_dir.index()] = in_square;
    }

    public IsoGridSquare[] getSurroundingSquares() {
        return this.nav;
    }

    public IsoGridSquare getSquareAbove() {
        return cellGetSquare.getGridSquare(this.x, this.y, this.z + 1);
    }

    public IsoGridSquare getAdjacentPathSquare(IsoDirections dir) {
        switch (dir) {
            case N:
                return this.n;
            case S:
                return this.s;
            case W:
                return this.w;
            case E:
                return this.e;
            case NW:
                return this.nw;
            case NE:
                return this.ne;
            case SW:
                return this.sw;
            case SE:
                return this.se;
            default:
                return null;
        }
    }

    public float getApparentZ(float dx, float dy) {
        dx = PZMath.clamp(dx, 0.0F, 1.0F);
        dy = PZMath.clamp(dy, 0.0F, 1.0F);
        float FUDGE = PerformanceSettings.fboRenderChunk ? 0.1F : 0.0F;
        if (this.has(IsoObjectType.stairsTN)) {
            return this.getZ() + PZMath.lerp(0.6666F + FUDGE, 1.0F, 1.0F - dy);
        } else if (this.has(IsoObjectType.stairsTW)) {
            return this.getZ() + PZMath.lerp(0.6666F + FUDGE, 1.0F, 1.0F - dx);
        } else if (this.has(IsoObjectType.stairsMN)) {
            return this.getZ() + PZMath.lerp(0.3333F + FUDGE, 0.6666F + FUDGE, 1.0F - dy);
        } else if (this.has(IsoObjectType.stairsMW)) {
            return this.getZ() + PZMath.lerp(0.3333F + FUDGE, 0.6666F + FUDGE, 1.0F - dx);
        } else if (this.has(IsoObjectType.stairsBN)) {
            return this.getZ() + PZMath.lerp(0.01F, 0.3333F + FUDGE, 1.0F - dy);
        } else {
            return this.has(IsoObjectType.stairsBW)
                ? this.getZ() + PZMath.lerp(0.01F, 0.3333F + FUDGE, 1.0F - dx)
                : this.getZ() + this.getSlopedSurfaceHeight(dx, dy);
        }
    }

    public IsoDirections getStairsDirection() {
        if (this.HasStairsNorth()) {
            return IsoDirections.N;
        } else {
            return this.HasStairsWest() ? IsoDirections.W : null;
        }
    }

    public float getStairsHeightMax() {
        if (this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
            return 1.0F;
        } else if (this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsMW)) {
            return 0.66F;
        } else {
            return !this.has(IsoObjectType.stairsMN) && !this.has(IsoObjectType.stairsMW) ? 0.0F : 0.33F;
        }
    }

    public float getStairsHeightMin() {
        if (this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
            return 0.66F;
        } else {
            return !this.has(IsoObjectType.stairsMN) && !this.has(IsoObjectType.stairsMW) ? 0.0F : 0.33F;
        }
    }

    public float getStairsHeight(IsoDirections edge) {
        IsoDirections slopeDir = this.getStairsDirection();
        if (slopeDir == null) {
            return 0.0F;
        } else if (slopeDir == edge) {
            return this.getStairsHeightMax();
        } else {
            return slopeDir.Rot180() == edge ? this.getStairsHeightMin() : -1.0F;
        }
    }

    public boolean isStairsEdgeBlocked(IsoDirections edge) {
        IsoDirections dir = this.getStairsDirection();
        if (dir == null) {
            return false;
        } else {
            IsoGridSquare square2 = this.getAdjacentSquare(edge);
            return square2 == null ? true : this.getStairsHeight(edge) != square2.getStairsHeight(edge.Rot180());
        }
    }

    public boolean hasSlopedSurface() {
        return this.getSlopedSurfaceDirection() != null;
    }

    public IsoDirections getSlopedSurfaceDirection() {
        return this.getProperties().getSlopedSurfaceDirection();
    }

    public boolean hasIdenticalSlopedSurface(IsoGridSquare other) {
        return this.getSlopedSurfaceDirection() == other.getSlopedSurfaceDirection()
            && this.getSlopedSurfaceHeightMin() == other.getSlopedSurfaceHeightMin()
            && this.getSlopedSurfaceHeightMax() == other.getSlopedSurfaceHeightMax();
    }

    public float getSlopedSurfaceHeightMin() {
        return this.getProperties().getSlopedSurfaceHeightMin() / 100.0F;
    }

    public float getSlopedSurfaceHeightMax() {
        return this.getProperties().getSlopedSurfaceHeightMax() / 100.0F;
    }

    public float getSlopedSurfaceHeight(float dx, float dy) {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return 0.0F;
        } else {
            dx = PZMath.clamp(dx, 0.0F, 1.0F);
            dy = PZMath.clamp(dy, 0.0F, 1.0F);
            int slopeHeightMin = this.getProperties().getSlopedSurfaceHeightMin();
            int slopeHeightMax = this.getProperties().getSlopedSurfaceHeightMax();

            float z = switch (dir) {
                case N -> PZMath.lerp(slopeHeightMin, slopeHeightMax, 1.0F - dy);
                case S -> PZMath.lerp(slopeHeightMin, slopeHeightMax, dy);
                case W -> PZMath.lerp(slopeHeightMin, slopeHeightMax, 1.0F - dx);
                case E -> PZMath.lerp(slopeHeightMin, slopeHeightMax, dx);
                default -> -1.0F;
            };
            return z < 0.0F ? 0.0F : z / 100.0F;
        }
    }

    public float getSlopedSurfaceHeight(IsoDirections edge) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        if (slopeDir == null) {
            return 0.0F;
        } else if (slopeDir == edge) {
            return this.getSlopedSurfaceHeightMax();
        } else {
            return slopeDir.Rot180() == edge ? this.getSlopedSurfaceHeightMin() : -1.0F;
        }
    }

    public boolean isSlopedSurfaceEdgeBlocked(IsoDirections edge) {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return false;
        } else {
            IsoGridSquare square2 = this.getAdjacentSquare(edge);
            return square2 == null ? true : this.getSlopedSurfaceHeight(edge) != square2.getSlopedSurfaceHeight(edge.Rot180());
        }
    }

    public boolean hasSlopedSurfaceToLevelAbove(IsoDirections dir) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        return slopeDir == null ? false : this.getSlopedSurfaceHeight(dir) == 1.0F;
    }

    public float getTotalWeightOfItemsOnFloor() {
        float total = 0.0F;

        for (int i = 0; i < this.worldObjects.size(); i++) {
            InventoryItem item = this.worldObjects.get(i).getItem();
            if (item != null) {
                total += item.getUnequippedWeight();
            }
        }

        return total;
    }

    public boolean getCollideMatrix(int dx, int dy, int dz) {
        return getMatrixBit(this.collideMatrix, dx + 1, dy + 1, dz + 1);
    }

    public boolean getPathMatrix(int dx, int dy, int dz) {
        return getMatrixBit(this.pathMatrix, dx + 1, dy + 1, dz + 1);
    }

    public boolean getVisionMatrix(int dx, int dy, int dz) {
        return getMatrixBit(this.visionMatrix, dx + 1, dy + 1, dz + 1);
    }

    public void checkRoomSeen(int playerIndex) {
        IsoRoom room = this.getRoom();
        if (room != null && room.def != null && !room.def.explored) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player != null) {
                if (this.z == PZMath.fastfloor(player.getZ())) {
                    int dist = 10;
                    if (player.getBuilding() == room.building) {
                        dist = 50;
                    }

                    if (IsoUtils.DistanceToSquared(player.getX(), player.getY(), this.x + 0.5F, this.y + 0.5F) < dist * dist) {
                        room.def.explored = true;
                        room.onSee();
                        room.seen = 0;
                        if (player.isLocalPlayer()) {
                            player.triggerMusicIntensityEvent("SeeUnexploredRoom");
                        }
                    }
                }
            }
        }
    }

    public boolean hasFlies() {
        return this.hasFlies;
    }

    public void setHasFlies(boolean hasFlies) {
        if (hasFlies != this.hasFlies) {
            this.invalidateRenderChunkLevel(hasFlies ? 64L : 128L);
        }

        this.hasFlies = hasFlies;
    }

    public float getLightLevel(int playerIndex) {
        if (playerIndex == -1) {
            return this.getLightLevel2();
        } else {
            ColorInfo lightInfo = this.lighting[playerIndex].lightInfo();
            return PZMath.max(lightInfo.r, lightInfo.g, lightInfo.b);
        }
    }

    public float getLightLevel2() {
        IsoGridSquare.ILighting lightingTemp = new LightingJNI.JNILighting(-1, this);
        ColorInfo lightInfo = lightingTemp.lightInfo();
        return PZMath.max(lightInfo.r, lightInfo.g, lightInfo.b);
    }

    public ArrayList<IsoAnimal> getAnimals(ArrayList<IsoAnimal> result) {
        result.clear();

        for (int i = 0; i < this.getMovingObjects().size(); i++) {
            IsoMovingObject movingObject = this.getMovingObjects().get(i);
            if (movingObject instanceof IsoAnimal animal && !animal.isOnHook()) {
                result.add(animal);
            }
        }

        return result;
    }

    public ArrayList<IsoAnimal> getAnimals() {
        return this.getAnimals(new ArrayList<>());
    }

    public boolean checkHaveGrass() {
        if (this.getFloor() != null && this.getFloor().getAttachedAnimSprite() != null) {
            for (int i = 0; i < this.getFloor().getAttachedAnimSprite().size(); i++) {
                IsoSprite sprite = this.getFloor().getAttachedAnimSprite().get(i).parentSprite;
                if ("blends_natural_01_87".equals(sprite.getName())) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean checkHaveDung() {
        for (int i = 0; i < this.getWorldObjects().size(); i++) {
            InventoryItem item = this.getWorldObjects().get(i).getItem();
            if (item.getScriptItem().isDung) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<InventoryItem> removeAllDung() {
        ArrayList<InventoryItem> result = new ArrayList<>();

        for (int i = 0; i < this.getWorldObjects().size(); i++) {
            InventoryItem item = this.getWorldObjects().get(i).getItem();
            if (item.getScriptItem().isDung) {
                result.add(item);
                this.removeWorldObject(this.getWorldObjects().get(i));
                i--;
            }
        }

        return result;
    }

    public boolean removeGrass() {
        boolean removed = false;
        if (this.getFloor() != null && this.getFloor().getSprite().getProperties().has("grassFloor") && this.checkHaveGrass()) {
            this.getFloor().addAttachedAnimSpriteByName("blends_natural_01_87");
            removed = true;

            for (int i = 0; i < this.getObjects().size(); i++) {
                IsoObject obj = this.getObjects().get(i);
                if (obj.getSprite().getProperties().has(IsoFlagType.canBeRemoved)) {
                    if (GameServer.server) {
                        this.transmitRemoveItemFromSquare(obj);
                    }

                    this.getObjects().remove(obj);
                    i--;
                }
            }

            this.RecalcProperties();
            this.RecalcAllWithNeighbours(true);
            if (!this.isOutside()) {
                return false;
            }

            Zone zone = this.getGrassRegrowthZone();
            if (zone == null) {
                zone = IsoWorld.instance.getMetaGrid().registerZone("", "GrassRegrowth", this.x - 20, this.y - 20, this.z, 40, 40);
                zone.setLastActionTimestamp(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue());
                INetworkPacket.sendToRelative(PacketTypes.PacketType.RegisterZone, this.x, this.y, zone);
            } else {
                zone.setLastActionTimestamp(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue());
                INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncZone, this.x, this.y, zone);
            }
        }

        return removed;
    }

    private Zone getGrassRegrowthZone() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, this.z, zones);

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (StringUtils.equals(zone.getType(), "GrassRegrowth")) {
                return zone;
            }
        }

        return null;
    }

    public int getZombieCount() {
        int count = 0;

        for (int n = 0; n < this.movingObjects.size(); n++) {
            IsoMovingObject mov = this.movingObjects.get(n);
            if (mov != null && mov instanceof IsoZombie) {
                count++;
            }
        }

        return count;
    }

    public String getSquareRegion() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (StringUtils.equals(zone.type, "Region")) {
                return zone.name;
            }
        }

        return "General";
    }

    public boolean containsVegetation() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.getSprite() != null && obj.getSprite().getName() != null && obj.getSprite().getName().contains("vegetation")) {
                return true;
            }
        }

        return false;
    }

    public IsoAnimalTrack getAnimalTrack() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj instanceof IsoAnimalTrack isoAnimalTrack) {
                return isoAnimalTrack;
            }
        }

        return null;
    }

    public boolean hasTrashReceptacle() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.getSprite() != null && obj.getSprite().getProperties() != null && obj.getSprite().getProperties().has("IsTrashCan")) {
                return true;
            }
        }

        return false;
    }

    public boolean hasTrash() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null
                && obj.getSprite() != null
                && (
                    obj.getSprite().getName() != null && obj.getSprite().getName().contains("trash")
                        || obj.getSprite().getProperties() != null && obj.getSprite().getProperties().has("IsTrashCan")
                )) {
                return true;
            }
        }

        return false;
    }

    public IsoObject getTrashReceptacle() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null
                && obj.getSprite() != null
                && obj.getSprite().getProperties() != null
                && obj.getSprite().getProperties().has("IsTrashCan")
                && obj.getContainer() != null) {
                return obj;
            }
        }

        return null;
    }

    public boolean isExtraFreeSquare() {
        return this.isFree(false) && this.getObjects().size() < 2 && !this.HasStairs() && this.hasFloor();
    }

    public IsoGridSquare getRandomAdjacentFreeSameRoom() {
        if (this.getRoom() == null) {
            return null;
        } else {
            ArrayList<IsoGridSquare> squares = new ArrayList<>();

            for (int i = 0; i < 7; i++) {
                IsoGridSquare testSq = this.getAdjacentSquare(IsoDirections.fromIndex(i));
                if (testSq != null && testSq.isExtraFreeSquare() && testSq.getRoom() != null && testSq.getRoom() == this.getRoom()) {
                    squares.add(testSq);
                }
            }

            if (squares.isEmpty()) {
                return null;
            } else {
                IsoGridSquare sq2 = squares.get(Rand.Next(squares.size()));
                return sq2 != null && sq2.isExtraFreeSquare() && sq2.getRoom() != null && sq2.getRoom() == this.getRoom() ? sq2 : sq2;
            }
        }
    }

    public String getZombiesType() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (StringUtils.equalsIgnoreCase(zone.type, "ZombiesType")) {
                return zone.name;
            }
        }

        return null;
    }

    public String getLootZone() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (StringUtils.equals(zone.type, "LootZone")) {
                return zone.name;
            }
        }

        return null;
    }

    public IsoObject addTileObject(String spriteName) {
        IsoObject obj = IsoObject.getNew(this, spriteName, null, false);
        this.AddTileObject(obj);
        MapObjects.newGridSquare(this);
        MapObjects.loadGridSquare(this);
        return obj;
    }

    public boolean hasSand() {
        if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
            String name = this.getFloor().getSprite().getName();
            if (!name.contains("blends_natural_01") && !name.contains("floors_exterior_natural_01")) {
                return false;
            } else {
                return name.equals("blends_natural_01_0")
                        || name.equals("blends_natural_01_5")
                        || name.equals("blends_natural_01_6")
                        || name.equals("blends_natural_01_7")
                    ? true
                    : name.contains("floors_exterior_natural_24");
            }
        } else {
            return false;
        }
    }

    public boolean hasDirt() {
        if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
            String name = this.getFloor().getSprite().getName();
            if (!name.contains("blends_natural_01") && !name.contains("floors_exterior_natural_01")) {
                return false;
            } else if (name.equals("blends_natural_01_64")
                || name.equals("blends_natural_01_69")
                || name.equals("blends_natural_01_70")
                || name.equals("blends_natural_01_71")) {
                return true;
            } else {
                return name.equals("blends_natural_01_80")
                        || name.equals("blends_natural_01_85")
                        || name.equals("blends_natural_01_86")
                        || name.equals("blends_natural_01_87")
                    ? true
                    : name.equals("floors_exterior_natural_16")
                        || name.equals("floors_exterior_natural_17")
                        || name.equals("floors_exterior_natural_18")
                        || name.equals("floors_exterior_natural_19");
            }
        } else {
            return false;
        }
    }

    public boolean hasNaturalFloor() {
        if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
            String name = this.getFloor().getSprite().getName();
            return name.startsWith("blends_natural_01") || name.startsWith("floors_exterior_natural");
        } else {
            return false;
        }
    }

    public void dirtStamp() {
        if (!this.hasSand() && !this.hasDirt() && this.getFloor() != null) {
            this.getFloor().setAttachedAnimSprite(null);
            this.getFloor().setOverlaySprite(null);
            String tile = "blends_natural_01_64";
            if (!Rand.NextBool(4)) {
                tile = "blends_natural_01_" + (69 + Rand.Next(3));
            }

            this.getFloor().setSpriteFromName(tile);
            IsoGridSquare sq = this.getAdjacentSquare(IsoDirections.N);
            if (sq != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
                RandomizedZoneStoryBase.cleanSquareForStory(sq);
                tile = "blends_natural_01_75";
                if (!Rand.NextBool(4)) {
                    tile = "blends_natural_01_79";
                }

                sq.getFloor().setOverlaySprite(tile);
                sq.RecalcAllWithNeighbours(true);
                IsoObject obj2 = sq.getFloor();
                if (obj2 != null) {
                    if (obj2.attachedAnimSprite == null) {
                        obj2.attachedAnimSprite = new ArrayList<>(4);
                    }

                    obj2.attachedAnimSprite.add(IsoSpriteInstance.get(sq.getFloor().getOverlaySprite()));
                }
            }

            sq = this.getAdjacentSquare(IsoDirections.S);
            if (sq != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
                RandomizedZoneStoryBase.cleanSquareForStory(sq);
                tile = "blends_natural_01_72";
                if (!Rand.NextBool(4)) {
                    tile = "blends_natural_01_76";
                }

                sq.getFloor().setOverlaySprite(tile);
                sq.RecalcAllWithNeighbours(true);
                IsoObject obj2 = sq.getFloor();
                if (obj2 != null) {
                    if (obj2.attachedAnimSprite == null) {
                        obj2.attachedAnimSprite = new ArrayList<>(4);
                    }

                    obj2.attachedAnimSprite.add(IsoSpriteInstance.get(sq.getFloor().getOverlaySprite()));
                }
            }

            sq = this.getAdjacentSquare(IsoDirections.E);
            if (sq != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
                RandomizedZoneStoryBase.cleanSquareForStory(sq);
                tile = "blends_natural_01_73";
                if (!Rand.NextBool(4)) {
                    tile = "blends_natural_01_77";
                }

                sq.getFloor().setOverlaySprite(tile);
                sq.RecalcAllWithNeighbours(true);
                IsoObject obj2 = sq.getFloor();
                if (obj2 != null) {
                    if (obj2.attachedAnimSprite == null) {
                        obj2.attachedAnimSprite = new ArrayList<>(4);
                    }

                    obj2.attachedAnimSprite.add(IsoSpriteInstance.get(sq.getFloor().getOverlaySprite()));
                }
            }

            sq = this.getAdjacentSquare(IsoDirections.W);
            if (sq != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
                RandomizedZoneStoryBase.cleanSquareForStory(sq);
                tile = "blends_natural_01_74";
                if (!Rand.NextBool(4)) {
                    tile = "blends_natural_01_78";
                }

                sq.getFloor().setOverlaySprite(tile);
                sq.RecalcAllWithNeighbours(true);
                IsoObject obj2 = sq.getFloor();
                if (obj2 != null) {
                    if (obj2.attachedAnimSprite == null) {
                        obj2.attachedAnimSprite = new ArrayList<>(4);
                    }

                    obj2.attachedAnimSprite.add(IsoSpriteInstance.get(sq.getFloor().getOverlaySprite()));
                }
            }
        }
    }

    public IsoGridSquare getRandomAdjacent() {
        ArrayList<IsoGridSquare> squares = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            IsoGridSquare testSq = this.getAdjacentSquare(IsoDirections.fromIndex(i));
            if (testSq != null && testSq.isExtraFreeSquare()) {
                squares.add(testSq);
            }
        }

        return squares.isEmpty() ? null : squares.get(Rand.Next(squares.size()));
    }

    public boolean isAdjacentTo(IsoGridSquare sq) {
        if (this == sq) {
            return true;
        } else {
            for (int i = 0; i < 7; i++) {
                IsoGridSquare testSq = this.getAdjacentSquare(IsoDirections.fromIndex(i));
                if (testSq != null && testSq == sq) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean hasFireObject() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null) {
                if (obj instanceof IsoFireplace && obj.isLit()) {
                    return true;
                }

                if (obj instanceof IsoBarbecue && obj.isLit()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasAdjacentFireObject() {
        for (int i = 0; i < 7; i++) {
            IsoGridSquare testSq = this.getAdjacentSquare(IsoDirections.fromIndex(i));
            if (testSq != null && !this.isBlockedTo(testSq) && testSq.hasFireObject()) {
                return true;
            }
        }

        return false;
    }

    public void addGrindstone() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Grindstone");
        if (script != null) {
            String sprite = "crafted_01_" + (120 + Rand.Next(4));
            if (this.getProperties().has(IsoFlagType.WallNW)) {
                sprite = "crafted_01_" + (120 + Rand.Next(2));
            } else if (this.getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_01_120";
            } else if (this.getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_01_121";
            } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_01_122";
            } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_01_123";
            }

            this.addWorkstationEntity(script, sprite);
        }
    }

    public void addMetalBandsaw() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.MetalBandsaw");
        if (script != null) {
            String sprite = "industry_02_" + (264 + Rand.Next(4));
            if (this.getProperties().has(IsoFlagType.WallNW)) {
                sprite = "industry_02_" + (264 + Rand.Next(2));
            } else if (this.getProperties().has(IsoFlagType.WallN)) {
                sprite = "industry_02_265";
            } else if (this.getProperties().has(IsoFlagType.WallW)) {
                sprite = "industry_02_264";
            } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
                sprite = "industry_02_267";
            } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
                sprite = "industry_02_266";
            }

            this.addWorkstationEntity(script, sprite);
        }
    }

    public void addStandingDrillPress() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.StandingDrillPress");
        if (script != null) {
            String sprite = "industry_02_" + (268 + Rand.Next(4));
            if (this.getProperties().has(IsoFlagType.WallNW)) {
                sprite = "industry_02_" + (268 + Rand.Next(2));
            } else if (this.getProperties().has(IsoFlagType.WallN)) {
                sprite = "industry_02_269";
            } else if (this.getProperties().has(IsoFlagType.WallW)) {
                sprite = "industry_02_268";
            } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
                sprite = "industry_02_270";
            } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
                sprite = "industry_02_271";
            }

            this.addWorkstationEntity(script, sprite);
        }
    }

    public void addFreezer() {
        String sprite = "appliances_refrigeration_01_" + (48 + Rand.Next(4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "appliances_refrigeration_01_" + (48 + Rand.Next(2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "appliances_refrigeration_01_48";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "appliances_refrigeration_01_49";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "appliances_refrigeration_01_50";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "appliances_refrigeration_01_51";
        }

        this.addTileObject(sprite);
    }

    public void addFloodLights() {
        String sprite = "lighting_outdoor_01_" + (48 + Rand.Next(4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "lighting_outdoor_01_" + (48 + Rand.Next(2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "lighting_outdoor_01_01_48";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "lighting_outdoor_01_01_49";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "lighting_outdoor_01_01_51";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "lighting_outdoor_01_01_50";
        }

        this.addTileObject(sprite);
    }

    public void addSpinningWheel() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Spinning_Wheel");
        if (script != null) {
            String sprite = "crafted_04_" + (36 + Rand.Next(4));
            if (this.getProperties().has(IsoFlagType.WallNW)) {
                sprite = "crafted_04_" + (36 + Rand.Next(2));
            } else if (this.getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_04_37";
            } else if (this.getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_04_36";
            } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_04_38";
            } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_04_39";
            }

            this.addWorkstationEntity(script, sprite);
        }
    }

    public void addLoom() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Loom");
        if (script != null) {
            String sprite = "crafted_04_" + (72 + Rand.Next(4));
            if (this.getProperties().has(IsoFlagType.WallNW)) {
                sprite = "crafted_04_" + (72 + Rand.Next(2));
            } else if (this.getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_04_72";
            } else if (this.getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_04_73";
            } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_04_74";
            } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_04_75";
            }

            this.addWorkstationEntity(script, sprite);
        }
    }

    public void addHandPress() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Hand_Press");
        if (script != null) {
            String sprite = "crafted_01_" + (72 + Rand.Next(2));
            if (this.getProperties().has(IsoFlagType.WallN)) {
                sprite = "crafted_01_72";
            } else if (this.getProperties().has(IsoFlagType.WallW)) {
                sprite = "crafted_01_73";
            }

            this.addWorkstationEntity(script, sprite);
        }
    }

    public IsoThumpable addWorkstationEntity(String scriptString, String sprite) {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript(scriptString);
        return this.addWorkstationEntity(script, sprite);
    }

    public IsoThumpable addWorkstationEntity(GameEntityScript script, String sprite) {
        if (script == null) {
            return null;
        } else {
            IsoThumpable thumpable = new IsoThumpable(IsoWorld.instance.getCell(), this, sprite, false, null);
            this.addWorkstationEntity(thumpable, script);
            return thumpable;
        }
    }

    public void addWorkstationEntity(IsoThumpable thumpable, GameEntityScript script) {
        thumpable.setHealth(thumpable.getMaxHealth());
        thumpable.setBreakSound("BreakObject");
        GameEntityFactory.CreateIsoObjectEntity(thumpable, script, true);
        this.AddSpecialObject(thumpable);
        thumpable.transmitCompleteItemToClients();
    }

    public boolean isDoorSquare() {
        if (!this.getProperties().has(IsoFlagType.DoorWallN)
            && !this.getProperties().has(IsoFlagType.DoorWallW)
            && !this.has(IsoObjectType.doorN)
            && !this.has(IsoObjectType.doorW)) {
            return this.getAdjacentSquare(IsoDirections.S) == null
                    || !this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.DoorWallN)
                        && !this.getAdjacentSquare(IsoDirections.S).has(IsoObjectType.doorN)
                ? this.getAdjacentSquare(IsoDirections.E) != null
                    && (
                        this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.DoorWallW)
                            || this.getAdjacentSquare(IsoDirections.E).has(IsoObjectType.doorW)
                    )
                : true;
        } else {
            return true;
        }
    }

    public boolean isWallSquare() {
        if (this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW)) {
            return true;
        } else {
            return this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)
                ? true
                : this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW);
        }
    }

    public boolean isWallSquareNW() {
        return this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW);
    }

    public boolean isFreeWallSquare() {
        if ((this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW))
            && this.getObjects().size() < 3) {
            return true;
        } else {
            return this.getAdjacentSquare(IsoDirections.S) != null
                    && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)
                    && this.getObjects().size() < 2
                ? true
                : this.getAdjacentSquare(IsoDirections.E) != null
                    && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)
                    && this.getObjects().size() < 2;
        }
    }

    public boolean isDoorOrWallSquare() {
        return this.isDoorSquare() || this.isWallSquare();
    }

    public void spawnRandomRuralWorkstation() {
        String thing = null;
        int tool = Rand.Next(9);
        switch (tool) {
            case 0:
                thing = "Freezer";
                this.addFreezer();
                break;
            case 1:
                thing = "FloodLights";
                this.addFloodLights();
                break;
            case 2:
                thing = "MetalBandsaw";
                this.addMetalBandsaw();
                break;
            case 3:
                thing = "DrillPress";
                this.addStandingDrillPress();
                break;
            case 4:
                thing = "Electric Blower Forge Moveable";
                IsoBarbecue bbq = new IsoBarbecue(IsoWorld.instance.getCell(), this, IsoSpriteManager.instance.namedMap.get("crafted_02_52"));
                this.getObjects().add(bbq);
                this.addTileObject("crafted_02_52");
                break;
            case 5:
                thing = "Hand_Press";
                this.addHandPress();
                break;
            case 6:
                thing = "Grindstone";
                this.addGrindstone();
                break;
            case 7:
                thing = "SpinningWheel";
                this.addSpinningWheel();
                break;
            case 8:
                thing = "Loom";
                this.addLoom();
        }

        DebugLog.log("Special resource tile spawns: " + thing + ", at " + this.x + ", " + this.y);
    }

    public void spawnRandomWorkstation() {
        if (this.isRural()) {
            this.spawnRandomRuralWorkstation();
        } else {
            String thing = null;
            int tool = Rand.Next(4);
            switch (tool) {
                case 0:
                    thing = "Freezer";
                    this.addFreezer();
                    break;
                case 1:
                    thing = "FloodLights";
                    this.addFloodLights();
                    break;
                case 2:
                    thing = "MetalBandsaw";
                    this.addMetalBandsaw();
                    break;
                case 3:
                    thing = "DrillPress";
                    this.addStandingDrillPress();
            }

            DebugLog.log("Special resource tile spawns: " + thing + ", at " + this.x + ", " + this.y);
        }
    }

    public boolean isRural() {
        return Objects.equals(this.getSquareRegion(), "General") || this.getSquareRegion() == null;
    }

    public boolean isRuralExtraFussy() {
        Zone zone = this.getZone();
        return zone == null || !"TownZone".equals(zone.getType()) && !"TownZones".equals(zone.getType()) && !"TrailerPark".equals(zone.getType())
            ? Objects.equals(this.getSquareRegion(), "General") || this.getSquareRegion() == null
            : false;
    }

    public boolean isFreeWallPair(IsoDirections dir, boolean both) {
        IsoGridSquare sq = this.getAdjacentSquare(dir);
        if (this.isAdjacentTo(sq)
            && this.getRoom() != null
            && sq != null
            && sq.getRoom() != null
            && sq.getRoom() == this.getRoom()
            && this.canReachTo(sq)
            && !this.isDoorSquare()
            && this.getObjects().size() <= 2
            && !sq.isDoorSquare()
            && sq.getObjects().size() <= 2) {
            boolean good = false;
            int dir2 = dir.index();
            switch (dir2) {
                case 0:
                    if (this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallNW)) {
                        return false;
                    }
                case 1:
                case 3:
                case 5:
                default:
                    break;
                case 2:
                    if (this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW)) {
                        return false;
                    }
                    break;
                case 4:
                    if (sq.getProperties().has(IsoFlagType.WallN) || sq.getProperties().has(IsoFlagType.WallNW)) {
                        return false;
                    }
                    break;
                case 6:
                    if (sq.getProperties().has(IsoFlagType.WallW) || sq.getProperties().has(IsoFlagType.WallNW)) {
                        return false;
                    }
            }

            if (dir2 > 4) {
                dir2 -= 4;
            }

            switch (dir2) {
                case 0:
                    if (this.getProperties().has(IsoFlagType.WallW) && sq.getProperties().has(IsoFlagType.WallW)) {
                        return true;
                    }

                    if (both) {
                        IsoGridSquare sq2 = this.getAdjacentSquare(IsoDirections.E);
                        if (sq2.getProperties().has(IsoFlagType.WallW)
                            && this.getObjects().size() < 2
                            && sq2.getProperties().has(IsoFlagType.WallW)
                            && sq2.getObjects().size() < 2) {
                            return true;
                        }
                    }
                    break;
                case 2:
                    if (this.getProperties().has(IsoFlagType.WallN) && sq.getProperties().has(IsoFlagType.WallN)) {
                        return true;
                    }

                    if (both) {
                        IsoGridSquare sq2 = this.getAdjacentSquare(IsoDirections.S);
                        if (sq2.getProperties().has(IsoFlagType.WallN)
                            && this.getObjects().size() < 2
                            && sq2.getProperties().has(IsoFlagType.WallN)
                            && sq2.getObjects().size() < 2) {
                            return true;
                        }
                    }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean isGoodSquare() {
        if (this.getFloor() == null) {
            return false;
        } else if (this.isWaterSquare()) {
            return false;
        } else {
            return this.isDoorSquare() || this.HasStairs() || this.getObjects().size() > 2 ? false : this.isWallSquareNW() || this.getObjects().size() <= 1;
        }
    }

    public boolean isWaterSquare() {
        if (this.getFloor() == null) {
            return false;
        } else {
            PropertyContainer props = this.getFloor().getProperties();
            return Objects.equals(props.get("FloorMaterial"), "Water");
        }
    }

    public boolean isGoodOutsideSquare() {
        return this.isOutside() && this.isGoodSquare();
    }

    public void addStump() {
    }

    public static void setBlendFunc() {
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
        } else {
            IndieGL.glDefaultBlendFunc();
        }
    }

    public void invalidateRenderChunkLevel(long dirtyFlags) {
        if (this.chunk != null) {
            this.chunk.invalidateRenderChunkLevel(this.z, dirtyFlags);
        }
    }

    public void invalidateVispolyChunkLevel() {
        if (this.chunk != null) {
            this.chunk.invalidateVispolyChunkLevel(this.z);
        }
    }

    public ArrayList<IsoHutch> getHutchTiles() {
        ArrayList<IsoHutch> result = new ArrayList<>();

        for (int i = 0; i < this.getSpecialObjects().size(); i++) {
            if (this.getSpecialObjects().get(i) instanceof IsoHutch hutch) {
                result.add(hutch);
            }
        }

        return result;
    }

    public IsoHutch getHutch() {
        for (int i = 0; i < this.getSpecialObjects().size(); i++) {
            if (this.getSpecialObjects().get(i) instanceof IsoHutch hutch) {
                return hutch;
            }
        }

        return null;
    }

    public String getSquareZombiesType() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (StringUtils.equals(zone.type, "ZombiesType")) {
                return zone.name;
            }
        }

        return null;
    }

    public boolean hasRoomDef() {
        return this.getRoom() != null && this.getRoom().getRoomDef() != null;
    }

    public void spawnRandomGenerator() {
        if (!this.isRural()) {
            this.spawnRandomNewGenerator();
        } else {
            String thing = "Base.Generator";
            int tool = Rand.Next(4);
            switch (tool) {
                case 0:
                    thing = "Base.Generator";
                    break;
                case 1:
                    thing = "Base.Generator_Blue";
                    break;
                case 2:
                    thing = "Base.Generator_Yellow";
                    break;
                case 3:
                    thing = "Base.Generator_Old";
            }

            new IsoGenerator(InventoryItemFactory.CreateItem(thing), this.getCell(), this);
            DebugLog.log("Special resource tile spawns: " + thing + ", at " + this.x + ", " + this.y);
        }
    }

    public void spawnRandomNewGenerator() {
        String thing = "Base.Generator";
        int tool = Rand.Next(3);
        switch (tool) {
            case 0:
                thing = "Base.Generator";
                break;
            case 1:
                thing = "Base.Generator_Blue";
                break;
            case 2:
                thing = "Base.Generator_Yellow";
        }

        new IsoGenerator(InventoryItemFactory.CreateItem(thing), this.getCell(), this);
        DebugLog.log("Special resource tile spawns: " + thing + ", at " + this.x + ", " + this.y);
    }

    public boolean hasGrave() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj != null && obj.isGrave()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasFarmingPlant() {
        return this.getFarmingPlant() != null;
    }

    public GlobalObject getFarmingPlant() {
        SGlobalObjectSystem plantSystem = SGlobalObjects.getSystemByName("farming");
        return plantSystem == null ? null : plantSystem.getObjectAt(this);
    }

    public void destroyFarmingPlant() {
        SGlobalObjectSystem plantSystem = SGlobalObjects.getSystemByName("farming");
        if (plantSystem != null) {
            GlobalObject farmingPlant = plantSystem.getObjectAt(this);
            if (farmingPlant != null) {
                farmingPlant.destroyThisObject();
            }
        }
    }

    public boolean hasLitCampfire() {
        SGlobalObjectSystem campfireSystem = SGlobalObjects.getSystemByName("campfire");
        if (campfireSystem == null) {
            return false;
        } else {
            GlobalObject campfire = campfireSystem.getObjectAt(this);
            if (campfire == null) {
                return false;
            } else {
                Object isLit = campfire.getModData().rawget("isLit");
                if (isLit instanceof Boolean) {
                    return (Boolean)isLit;
                } else {
                    return isLit instanceof String ? Objects.equals(isLit, "true") : false;
                }
            }
        }
    }

    public GlobalObject getCampfire() {
        SGlobalObjectSystem campfireSystem = SGlobalObjects.getSystemByName("campfire");
        return campfireSystem == null ? null : campfireSystem.getObjectAt(this);
    }

    public void putOutCampfire() {
        SGlobalObjectSystem campfireSystem = SGlobalObjects.getSystemByName("campfire");
        if (campfireSystem != null) {
            GlobalObject campfire = campfireSystem.getObjectAt(this);
            if (campfire != null) {
                campfire.destroyThisObject();
            }
        }
    }

    private IsoGridSquareCollisionData DoDiagnalCheck(IsoGridSquareCollisionData isoGridSquareCollisionData, int x, int y, int z, boolean bIgnoreDoors) {
        LosUtil.TestResults res = this.testVisionAdjacent(x, 0, z, false, bIgnoreDoors);
        if (res == LosUtil.TestResults.Blocked) {
            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
            return isoGridSquareCollisionData;
        } else {
            LosUtil.TestResults res2 = this.testVisionAdjacent(0, y, z, false, bIgnoreDoors);
            if (res2 == LosUtil.TestResults.Blocked) {
                isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                return isoGridSquareCollisionData;
            } else if (res != LosUtil.TestResults.ClearThroughWindow && res2 != LosUtil.TestResults.ClearThroughWindow) {
                return this.getFirstBlocking(isoGridSquareCollisionData, x, y, z, false, bIgnoreDoors);
            } else {
                isoGridSquareCollisionData.testResults = LosUtil.TestResults.ClearThroughWindow;
                return isoGridSquareCollisionData;
            }
        }
    }

    public IsoGridSquareCollisionData getFirstBlocking(
        IsoGridSquareCollisionData isoGridSquareCollisionData, int x, int y, int z, boolean specialDiag, boolean bIgnoreDoors
    ) {
        if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= -1 && z <= 1) {
            if (z == 1 && (x != 0 || y != 0) && this.HasElevatedFloor()) {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x, this.y, this.z + z);
                if (sq != null) {
                    return sq.getFirstBlocking(isoGridSquareCollisionData, x, y, 0, specialDiag, bIgnoreDoors);
                }
            }

            if (z == -1 && (x != 0 || y != 0)) {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                if (sq != null && sq.HasElevatedFloor()) {
                    return this.getFirstBlocking(isoGridSquareCollisionData, x, y, 0, specialDiag, bIgnoreDoors);
                }
            }

            LosUtil.TestResults test = LosUtil.TestResults.Clear;
            if (x != 0 && y != 0 && specialDiag) {
                test = this.DoDiagnalCheck(x, y, z, bIgnoreDoors);
                if (test == LosUtil.TestResults.Clear
                    || test == LosUtil.TestResults.ClearThroughWindow
                    || test == LosUtil.TestResults.ClearThroughOpenDoor
                    || test == LosUtil.TestResults.ClearThroughClosedDoor) {
                    IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                    if (sq != null) {
                        isoGridSquareCollisionData = sq.DoDiagnalCheck(isoGridSquareCollisionData, -x, -y, -z, bIgnoreDoors);
                    }
                }

                isoGridSquareCollisionData.testResults = test;
                return isoGridSquareCollisionData;
            } else {
                IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
                LosUtil.TestResults ret = LosUtil.TestResults.Clear;
                if (sq != null && sq.z == this.z) {
                    if (!this.specialObjects.isEmpty()) {
                        for (int n = 0; n < this.specialObjects.size(); n++) {
                            IsoObject obj = this.specialObjects.get(n);
                            if (obj == null) {
                                isoGridSquareCollisionData.testResults = LosUtil.TestResults.Clear;
                                return isoGridSquareCollisionData;
                            }

                            IsoObject.VisionResult vis = obj.TestVision(this, sq);
                            if (vis != IsoObject.VisionResult.NoEffect) {
                                if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoDoor isoDoor) {
                                    ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                                    ret = LosUtil.TestResults.ClearThroughOpenDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoWindow) {
                                    ret = LosUtil.TestResults.ClearThroughWindow;
                                } else {
                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoDoor && !bIgnoreDoors) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked
                                        && obj instanceof IsoThumpable isoThumpable
                                        && isoThumpable.isDoor()
                                        && !bIgnoreDoors) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable isoThumpable && isoThumpable.isWindow()) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoCurtain) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoWindow) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoBarricade) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }
                                }
                            }
                        }
                    }

                    if (!sq.specialObjects.isEmpty()) {
                        for (int n = 0; n < sq.specialObjects.size(); n++) {
                            IsoObject objx = sq.specialObjects.get(n);
                            if (objx == null) {
                                isoGridSquareCollisionData.testResults = LosUtil.TestResults.Clear;
                                return isoGridSquareCollisionData;
                            }

                            IsoObject.VisionResult vis = objx.TestVision(this, sq);
                            if (vis != IsoObject.VisionResult.NoEffect) {
                                if (vis == IsoObject.VisionResult.Unblocked && objx instanceof IsoDoor isoDoor) {
                                    ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && objx instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                                    ret = LosUtil.TestResults.ClearThroughOpenDoor;
                                } else if (vis == IsoObject.VisionResult.Unblocked && objx instanceof IsoWindow) {
                                    ret = LosUtil.TestResults.ClearThroughWindow;
                                } else {
                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoDoor && !bIgnoreDoors) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked
                                        && objx instanceof IsoThumpable isoThumpable
                                        && isoThumpable.isDoor()
                                        && !bIgnoreDoors) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoThumpable isoThumpable && isoThumpable.isWindow()) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoCurtain) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoWindow) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }

                                    if (vis == IsoObject.VisionResult.Blocked && objx instanceof IsoBarricade) {
                                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                                        return isoGridSquareCollisionData;
                                    }
                                }
                            }
                        }
                    }
                } else if (z > 0
                    && sq != null
                    && (this.z != -1 || sq.z != 0)
                    && sq.getProperties().has(IsoFlagType.exterior)
                    && !this.getProperties().has(IsoFlagType.exterior)) {
                    ret = LosUtil.TestResults.Blocked;
                }

                test = !getMatrixBit(this.visionMatrix, x + 1, y + 1, z + 1) ? ret : LosUtil.TestResults.Blocked;
                isoGridSquareCollisionData.testResults = test;
                return isoGridSquareCollisionData;
            }
        } else {
            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
            return isoGridSquareCollisionData;
        }
    }

    private static boolean hasCutawayCapableWallNorth(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else if (square.has(IsoFlagType.WallSE)) {
            return false;
        } else {
            boolean bWallLike = (square.getWall(true) != null || square.has(IsoFlagType.WindowN))
                && (square.has(IsoFlagType.WallN) || square.has(IsoFlagType.WallNW) || square.has(IsoFlagType.DoorWallN) || square.has(IsoFlagType.WindowN));
            if (!bWallLike) {
                bWallLike = square.getGarageDoor(true) != null;
            }

            return bWallLike;
        }
    }

    private static boolean hasCutawayCapableWallWest(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else if (square.has(IsoFlagType.WallSE)) {
            return false;
        } else {
            boolean bWallLike = (square.getWall(false) != null || square.has(IsoFlagType.WindowW))
                && (square.has(IsoFlagType.WallW) || square.has(IsoFlagType.WallNW) || square.has(IsoFlagType.DoorWallW) || square.has(IsoFlagType.WindowW));
            if (!bWallLike) {
                bWallLike = square.getGarageDoor(false) != null;
            }

            return bWallLike;
        }
    }

    public boolean canSpawnVermin() {
        if (SandboxOptions.instance.getCurrentRatIndex() <= 0) {
            return false;
        } else if (this.isVehicleIntersecting() || this.isWaterSquare() || !this.isSolidFloor()) {
            return false;
        } else if (this.isOutside() && this.z != 0) {
            return false;
        } else if (this.z < 0) {
            return true;
        } else {
            return this.zone == null
                    || !"TownZone".equals(this.zone.getType())
                        && !"TownZones".equals(this.zone.getType())
                        && !"TrailerPark".equals(this.zone.getType())
                        && !"Farm".equals(this.zone.getType())
                ? this.getSquareRegion() != null
                    || Objects.equals(this.getSquareZombiesType(), "StreetPoor")
                    || Objects.equals(this.getSquareZombiesType(), "TrailerPark")
                    || Objects.equals(this.getLootZone(), "Poor")
                : true;
        }
    }

    public boolean isNoGas() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

        for (int i = 0; i < zones.size(); i++) {
            String zoneType = zones.get(i).type;
            if (StringUtils.equals(zoneType, "NoGas")) {
                return true;
            }
        }

        return false;
    }

    public boolean isNoPower() {
        if (this.isDerelict()) {
            return true;
        } else {
            ArrayList<Zone> zones = threadLocalZones.get();
            zones.clear();
            IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

            for (int i = 0; i < zones.size(); i++) {
                String zoneType = zones.get(i).type;
                if (StringUtils.equals(zoneType, "NoPower") || StringUtils.equals(zoneType, "NoPowerOrWater")) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isNoWater() {
        if (this.isDerelict()) {
            return true;
        } else {
            ArrayList<Zone> zones = threadLocalZones.get();
            zones.clear();
            IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);

            for (int i = 0; i < zones.size(); i++) {
                String zoneType = zones.get(i).type;
                if (StringUtils.equals(zoneType, "NoWater") || StringUtils.equals(zoneType, "NoPowerOrWater")) {
                    return true;
                }
            }

            return false;
        }
    }

    public IsoButcherHook getButcherHook() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj instanceof IsoButcherHook isoButcherHook) {
                return isoButcherHook;
            }
        }

        return null;
    }

    public boolean isShop() {
        return this.getRoom() != null ? this.getRoom().isShop() : false;
    }

    public boolean hasFireplace() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj.getContainer() != null && obj.getContainer().getType() != null && obj.getContainer().getType().equals("fireplace")) {
                return true;
            }
        }

        return false;
    }

    public IsoDeadBody addCorpse() {
        boolean isSkeleton = Rand.Next(15 - SandboxOptions.instance.timeSinceApo.getValue()) == 0;
        return this.addCorpse(isSkeleton);
    }

    public IsoDeadBody addCorpse(boolean isSkeleton) {
        IsoDeadBody body = null;
        int amount = 14;
        if (Rand.Next(10) == 0) {
            amount = 50;
        }

        if (Rand.Next(40) == 0) {
            amount = 100;
        }

        for (int m = 0; m < amount; m++) {
            float rx = Rand.Next(3000) / 1000.0F;
            float ry = Rand.Next(3000) / 1000.0F;
            this.getChunk().addBloodSplat(this.getX() + --rx, this.getY() + --ry, this.getZ(), Rand.Next(20));
        }

        VirtualZombieManager.instance.choices.clear();
        VirtualZombieManager.instance.choices.add(this);
        IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
        if (zombie != null) {
            zombie.setX(this.x);
            zombie.setY(this.y);
            zombie.setFakeDead(false);
            zombie.setHealth(0.0F);
            if (!isSkeleton) {
                zombie.dressInRandomOutfit();

                for (int i = 0; i < 10; i++) {
                    zombie.addHole(null);
                    zombie.addBlood(null, false, true, false);
                    zombie.addDirt(null, null, false);
                }

                zombie.DoCorpseInventory();
            }

            zombie.setSkeleton(isSkeleton);
            if (isSkeleton) {
                zombie.getHumanVisual().setSkinTextureIndex(1);
            }

            body = new IsoDeadBody(zombie, true);
            if (!isSkeleton && Rand.Next(10) == 0) {
                body.setFakeDead(true);
                if (Rand.Next(5) == 0) {
                    body.setCrawling(true);
                }
            }
        }

        return body;
    }

    public IsoDeadBody createCorpse(boolean skeleton) {
        IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
        return this.createCorpse(zombie, skeleton);
    }

    public IsoDeadBody createCorpse(IsoZombie zombie) {
        return this.createCorpse(zombie, false);
    }

    public IsoDeadBody createCorpse(IsoZombie zombie, boolean skeleton) {
        if (zombie == null) {
            return null;
        } else {
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(this);
            ZombieSpawnRecorder.instance.record(zombie, this.getClass().getSimpleName());
            RandomizedWorldBase.alignCorpseToSquare(zombie, this);
            zombie.setFakeDead(false);
            zombie.setHealth(0.0F);
            if (skeleton) {
                zombie.setSkeleton(true);
                zombie.getHumanVisual().setSkinTextureIndex(Rand.Next(1, 3));
            }

            return new IsoDeadBody(zombie, true);
        }
    }

    public IsoObject getBed() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject test = this.getObjects().get(i);
            if (test.getSprite() != null && test.getSprite().getProperties() != null && test.getSprite().getProperties().has(IsoFlagType.bed)) {
                return test;
            }
        }

        return null;
    }

    public IsoObject getPuddleFloor() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj.hasWater()
                && obj.getSprite() != null
                && obj.getSprite().getProperties() != null
                && obj.getSprite().getProperties().has(IsoFlagType.solidfloor)) {
                return obj;
            }
        }

        return null;
    }

    public void flagForHotSave() {
        if (this.getChunk() != null) {
            this.getChunk().flagForHotSave();
        }
    }

    public boolean hasGridPower() {
        return !this.isNoPower() && SandboxOptions.instance.doesPowerGridExist();
    }

    public boolean hasGridPower(int offset) {
        return !this.isNoPower() && SandboxOptions.instance.doesPowerGridExist(offset);
    }

    public boolean isDerelict() {
        return this.getRoom() != null && this.getRoom().isDerelict();
    }

    public boolean shouldNotSpawnActivatedRadiosOrTvs() {
        return this.getRoom() != null && this.getRoom().getName() != null && this.getRoom().getName().toLowerCase().contains("radiofactory");
    }

    public boolean hasFence() {
        for (int i = 0; i < this.getObjects().size(); i++) {
            IsoObject obj = this.getObjects().get(i);
            if (obj.isHoppable()) {
                return true;
            }

            if (obj.getSpriteName() != null && obj.getSpriteName().toLowerCase().contains("fence")) {
                return true;
            }

            if (obj.getSpriteName() != null && obj.getSpriteName().toLowerCase().contains("fencing")) {
                return true;
            }
        }

        return false;
    }

    public boolean hasFenceInVicinity() {
        for (int x = -1; x < 1; x++) {
            for (int y = -1; y < 1; y++) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(this.getX() + x * 8, this.getY() + y * 8, this.getZ());
                if (chunk != null && chunk.hasFence()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasFloorOverWater() {
        return this.getProperties().has(IsoFlagType.water) && !this.getProperties().has(IsoFlagType.solidtrans);
    }

    public List<IsoGridSquare> getRadius(int radius) {
        List<IsoGridSquare> result = new ArrayList<>();
        radius /= 2;

        for (int x = this.getX() - radius; x <= this.getX() + radius; x++) {
            for (int y = this.getY() - radius; y <= this.getY() + radius; y++) {
                IsoGridSquare sq = this.getCell().getGridSquare(x, y, this.getZ());
                if (sq != null) {
                    result.add(sq);
                }
            }
        }

        return result;
    }

    public IsoGridSquare getSquareBelow() {
        return this.getCell().getGridSquare(this.x, this.y, this.z - 1);
    }

    public boolean canStand() {
        if (this.has(IsoFlagType.solid)) {
            return false;
        } else {
            return !this.has(IsoFlagType.solidtrans) ? this.TreatAsSolidFloor() : this.isAdjacentToWindow() || this.isAdjacentToHoppable();
        }
    }

    public boolean hasAdjacentCanStandSquare() {
        return this.getAdjacentSquare(IsoDirections.NW) != null && this.getAdjacentSquare(IsoDirections.NW).canStand()
            || this.getAdjacentSquare(IsoDirections.W) != null && this.getAdjacentSquare(IsoDirections.W).canStand()
            || this.getAdjacentSquare(IsoDirections.SW) != null && this.getAdjacentSquare(IsoDirections.SW).canStand()
            || this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).canStand()
            || this.getAdjacentSquare(IsoDirections.SE) != null && this.getAdjacentSquare(IsoDirections.SE).canStand()
            || this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).canStand()
            || this.getAdjacentSquare(IsoDirections.NE) != null && this.getAdjacentSquare(IsoDirections.NE).canStand()
            || this.getAdjacentSquare(IsoDirections.N) != null && this.getAdjacentSquare(IsoDirections.N).canStand();
    }

    public static class CellGetSquare implements IsoGridSquare.GetSquare {
        @Override
        public IsoGridSquare getGridSquare(int x, int y, int z) {
            return IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        }
    }

    public static final class CircleStencilShader extends Shader {
        public static final IsoGridSquare.CircleStencilShader instance = new IsoGridSquare.CircleStencilShader();
        public int wallShadeColor = -1;

        public CircleStencilShader() {
            super("CircleStencil");
        }

        @Override
        public void startRenderThread(TextureDraw tex) {
            super.startRenderThread(tex);
            VertexBufferObject.setModelViewProjection(this.getProgram());
        }

        @Override
        protected void onCompileSuccess(ShaderProgram shaderProgram) {
            this.Start();
            this.wallShadeColor = GL20.glGetAttribLocation(this.getID(), "a_wallShadeColor");
            shaderProgram.setSamplerUnit("texture", 0);
            shaderProgram.setSamplerUnit("CutawayStencil", 1);
            shaderProgram.setSamplerUnit("DEPTH", 2);
            this.End();
        }
    }

    public static final class CutawayNoDepthShader extends Shader {
        private static IsoGridSquare.CutawayNoDepthShader instance;
        public int wallShadeColor = -1;

        public static IsoGridSquare.CutawayNoDepthShader getInstance() {
            if (instance == null) {
                instance = new IsoGridSquare.CutawayNoDepthShader();
            }

            return instance;
        }

        private CutawayNoDepthShader() {
            super("CutawayNoDepth");
        }

        @Override
        public void startRenderThread(TextureDraw tex) {
            super.startRenderThread(tex);
            VertexBufferObject.setModelViewProjection(this.getProgram());
        }

        @Override
        protected void onCompileSuccess(ShaderProgram shaderProgram) {
            this.Start();
            this.wallShadeColor = GL20.glGetAttribLocation(this.getID(), "a_wallShadeColor");
            shaderProgram.setSamplerUnit("texture", 0);
            shaderProgram.setSamplerUnit("CutawayStencil", 1);
            this.End();
        }
    }

    public interface GetSquare {
        IsoGridSquare getGridSquare(int x, int y, int z);
    }

    public interface ILighting {
        int lightverts(int i);

        float lampostTotalR();

        float lampostTotalG();

        float lampostTotalB();

        boolean bSeen();

        boolean bCanSee();

        boolean bCouldSee();

        float darkMulti();

        float targetDarkMulti();

        ColorInfo lightInfo();

        void lightverts(int i, int value);

        void lampostTotalR(float r);

        void lampostTotalG(float g);

        void lampostTotalB(float b);

        void bSeen(boolean seen);

        void bCanSee(boolean canSee);

        void bCouldSee(boolean couldSee);

        void darkMulti(float f);

        void targetDarkMulti(float f);

        int resultLightCount();

        IsoGridSquare.ResultLight getResultLight(int index);

        void reset();
    }

    public static final class Lighting implements IsoGridSquare.ILighting {
        private final int[] lightverts = new int[8];
        private float lampostTotalR;
        private float lampostTotalG;
        private float lampostTotalB;
        private boolean seen;
        private boolean canSee;
        private boolean couldSee;
        private float darkMulti;
        private float targetDarkMulti;
        private final ColorInfo lightInfo = new ColorInfo();

        @Override
        public int lightverts(int i) {
            return this.lightverts[i];
        }

        @Override
        public float lampostTotalR() {
            return this.lampostTotalR;
        }

        @Override
        public float lampostTotalG() {
            return this.lampostTotalG;
        }

        @Override
        public float lampostTotalB() {
            return this.lampostTotalB;
        }

        @Override
        public boolean bSeen() {
            return this.seen;
        }

        @Override
        public boolean bCanSee() {
            return this.canSee;
        }

        @Override
        public boolean bCouldSee() {
            return this.couldSee;
        }

        @Override
        public float darkMulti() {
            return this.darkMulti;
        }

        @Override
        public float targetDarkMulti() {
            return this.targetDarkMulti;
        }

        @Override
        public ColorInfo lightInfo() {
            return this.lightInfo;
        }

        @Override
        public void lightverts(int i, int value) {
            this.lightverts[i] = value;
        }

        @Override
        public void lampostTotalR(float r) {
            this.lampostTotalR = r;
        }

        @Override
        public void lampostTotalG(float g) {
            this.lampostTotalG = g;
        }

        @Override
        public void lampostTotalB(float b) {
            this.lampostTotalB = b;
        }

        @Override
        public void bSeen(boolean seen) {
            this.seen = seen;
        }

        @Override
        public void bCanSee(boolean canSee) {
            this.canSee = canSee;
        }

        @Override
        public void bCouldSee(boolean couldSee) {
            this.couldSee = couldSee;
        }

        @Override
        public void darkMulti(float f) {
            this.darkMulti = f;
        }

        @Override
        public void targetDarkMulti(float f) {
            this.targetDarkMulti = f;
        }

        @Override
        public int resultLightCount() {
            return 0;
        }

        @Override
        public IsoGridSquare.ResultLight getResultLight(int index) {
            return null;
        }

        @Override
        public void reset() {
            this.lampostTotalR = 0.0F;
            this.lampostTotalG = 0.0F;
            this.lampostTotalB = 0.0F;
            this.seen = false;
            this.couldSee = false;
            this.canSee = false;
            this.targetDarkMulti = 0.0F;
            this.darkMulti = 0.0F;
            this.lightInfo.r = 0.0F;
            this.lightInfo.g = 0.0F;
            this.lightInfo.b = 0.0F;
            this.lightInfo.a = 1.0F;
        }
    }

    public static final class NoCircleStencilShader {
        public static final IsoGridSquare.NoCircleStencilShader instance = new IsoGridSquare.NoCircleStencilShader();
        private ShaderProgram shaderProgram;
        public int shaderId = -1;
        public int wallShadeColor = -1;

        private void initShader() {
            this.shaderProgram = ShaderProgram.createShaderProgram("NoCircleStencil", false, false, true);
            if (this.shaderProgram.isCompiled()) {
                this.shaderId = this.shaderProgram.getShaderID();
                this.wallShadeColor = GL20.glGetAttribLocation(this.shaderId, "a_wallShadeColor");
            }
        }
    }

    public static class PuddlesDirection {
        public static final byte PUDDLES_DIR_NONE = 1;
        public static final byte PUDDLES_DIR_NE = 2;
        public static final byte PUDDLES_DIR_NW = 4;
        public static final byte PUDDLES_DIR_ALL = 8;
    }

    private interface RenderWallCallback {
        void invoke(Texture var1, float var2, float var3);
    }

    public static final class ResultLight {
        public int id;
        public int x;
        public int y;
        public int z;
        public int radius;
        public float r;
        public float g;
        public float b;
        public static final int RLF_NONE = 0;
        public static final int RLF_ROOMLIGHT = 1;
        public static final int RLF_TORCH = 2;
        public int flags;

        public IsoGridSquare.ResultLight copyFrom(IsoGridSquare.ResultLight other) {
            this.id = other.id;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.radius = other.radius;
            this.r = other.r;
            this.g = other.g;
            this.b = other.b;
            this.flags = other.flags;
            return this;
        }
    }

    private static final class WaterSplashData {
        public float dx;
        public float dy;
        public float frame = -1.0F;
        public float size;
        public boolean isBigSplash;
        private int frameCount;
        private int frameCacheShift;
        private float unPausedAccumulator;

        public Texture getTexture() {
            if (!IsoGridSquare.isWaterSplashCacheInitialised) {
                IsoGridSquare.initWaterSplashCache();
            }

            return IsoGridSquare.waterSplashCache[(int)(this.frame * (this.frameCount - 1)) + this.frameCacheShift];
        }

        public void init(int frameCount, int frameCacheShift, boolean isRandomSize, float dx, float dy) {
            this.frame = 0.0F;
            this.frameCount = frameCount;
            this.frameCacheShift = frameCacheShift;
            this.unPausedAccumulator = IsoCamera.frameState.unPausedAccumulator;
            this.dx = dx;
            this.dy = dy;
            this.size = 0.5F;
            if (isRandomSize) {
                this.size = Rand.Next(0.25F, 0.75F);
            }
        }

        public void initSmallSplash(float dx, float dy) {
            this.init(16, 0, true, dx, dy);
            this.isBigSplash = false;
        }

        public void initBigSplash(float dx, float dy) {
            int splashIndex = Rand.Next(2);
            this.init(32, 16 + 32 * splashIndex, false, dx, dy);
            this.isBigSplash = true;
        }

        public void update() {
            if (IsoCamera.frameState.unPausedAccumulator < this.unPausedAccumulator) {
                this.unPausedAccumulator = 0.0F;
            }

            if (!IsoCamera.frameState.paused && IsoCamera.frameState.unPausedAccumulator > this.unPausedAccumulator) {
                this.frame = this.frame + 0.0166F * (IsoCamera.frameState.unPausedAccumulator - this.unPausedAccumulator);
                if (this.frame > 1.0F) {
                    this.frame = -1.0F;
                    this.unPausedAccumulator = 0.0F;
                } else {
                    this.unPausedAccumulator = IsoCamera.frameState.unPausedAccumulator;
                }
            }
        }

        public boolean isSplashNow() {
            if (!IsoCamera.frameState.paused && this.frame >= 0.0F && this.frame <= 1.0F) {
                if (IsoCamera.frameState.unPausedAccumulator < this.unPausedAccumulator) {
                    this.unPausedAccumulator = 0.0F;
                }

                if (this.frame + 0.0166F * (IsoCamera.frameState.unPausedAccumulator - this.unPausedAccumulator) > 1.5F) {
                    this.frame = -1.0F;
                }
            }

            return this.frame >= 0.0F && this.frame <= 1.0F;
        }
    }

    private static final class s_performance {
        static final PerformanceProfileProbe renderFloor = new PerformanceProfileProbe("IsoGridSquare.renderFloor", false);
    }
}
