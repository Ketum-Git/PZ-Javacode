// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.animals.AnimalFalldownState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterVehicleBrake;
import zombie.audio.parameters.ParameterVehicleEngineCondition;
import zombie.audio.parameters.ParameterVehicleGear;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterVehicleLoad;
import zombie.audio.parameters.ParameterVehicleRPM;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.audio.parameters.ParameterVehicleSkid;
import zombie.audio.parameters.ParameterVehicleSpeed;
import zombie.audio.parameters.ParameterVehicleSteer;
import zombie.audio.parameters.ParameterVehicleTireMissing;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.Bullet;
import zombie.core.physics.CarController;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.physics.RagdollController;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.weather.ClimateManager;
import zombie.network.ClientServerMap;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.PassengerMap;
import zombie.network.ServerOptions;
import zombie.network.fields.IPositional;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.VehiclePoly;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.radio.ZomboidRadio;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.VehiclePartModel;
import zombie.scripting.objects.VehicleScript;
import zombie.statistics.StatisticCategory;
import zombie.statistics.StatisticType;
import zombie.statistics.StatisticsManager;
import zombie.ui.TextManager;
import zombie.ui.UIManager;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class BaseVehicle extends IsoMovingObject implements Thumpable, IFMODParameterUpdater, IPositional {
    public static final int MASK1_FRONT = 0;
    public static final int MASK1_REAR = 4;
    public static final int MASK1_DOOR_RIGHT_FRONT = 8;
    public static final int MASK1_DOOR_RIGHT_REAR = 12;
    public static final int MASK1_DOOR_LEFT_FRONT = 1;
    public static final int MASK1_DOOR_LEFT_REAR = 5;
    public static final int MASK1_WINDOW_RIGHT_FRONT = 9;
    public static final int MASK1_WINDOW_RIGHT_REAR = 13;
    public static final int MASK1_WINDOW_LEFT_FRONT = 2;
    public static final int MASK1_WINDOW_LEFT_REAR = 6;
    public static final int MASK1_WINDOW_FRONT = 10;
    public static final int MASK1_WINDOW_REAR = 14;
    public static final int MASK1_GUARD_RIGHT_FRONT = 3;
    public static final int MASK1_GUARD_RIGHT_REAR = 7;
    public static final int MASK1_GUARD_LEFT_FRONT = 11;
    public static final int MASK1_GUARD_LEFT_REAR = 15;
    public static final int MASK2_ROOF = 0;
    public static final int MASK2_LIGHT_RIGHT_FRONT = 4;
    public static final int MASK2_LIGHT_LEFT_FRONT = 8;
    public static final int MASK2_LIGHT_RIGHT_REAR = 12;
    public static final int MASK2_LIGHT_LEFT_REAR = 1;
    public static final int MASK2_BRAKE_RIGHT = 5;
    public static final int MASK2_BRAKE_LEFT = 9;
    public static final int MASK2_LIGHTBAR_RIGHT = 13;
    public static final int MASK2_LIGHTBAR_LEFT = 2;
    public static final int MASK2_HOOD = 6;
    public static final int MASK2_BOOT = 10;
    public static final float PHYSICS_Z_SCALE = 0.8164967F;
    public static final float RADIUS = 0.3F;
    public static final float PLUS_RADIUS = 0.15F;
    public static final int FADE_DISTANCE = 15;
    public static final int RANDOMIZE_CONTAINER_CHANCE = 100;
    public static final byte noAuthorization = -1;
    private static final Vector3f _UNIT_Y = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final VehiclePoly tempPoly = new VehiclePoly();
    public static final boolean YURI_FORCE_FIELD = false;
    public static boolean renderToTexture;
    public static float centerOfMassMagic = 0.7F;
    private static final float[] wheelParams = new float[24];
    private static final float[] physicsParams = new float[27];
    private final float forcedFriction = -1.0F;
    public static Texture vehicleShadow;
    private static final ColorInfo inf = new ColorInfo();
    private static final float[] lowRiderParam = new float[4];
    private final BaseVehicle.VehicleImpulse impulseFromServer = new BaseVehicle.VehicleImpulse();
    private final BaseVehicle.VehicleImpulse[] impulseFromSquishedZombie = new BaseVehicle.VehicleImpulse[4];
    private final ArrayList<BaseVehicle.VehicleImpulse> impulseFromHitZombie = new ArrayList<>();
    private final int netPlayerTimeoutMax = 30;
    public final ArrayList<BaseVehicle.ModelInfo> models = new ArrayList<>();
    public IsoChunk chunk;
    public boolean polyDirty = true;
    private boolean polyGarageCheck = true;
    private float radiusReductionInGarage;
    public short vehicleId = -1;
    public int sqlId = -1;
    public boolean serverRemovedFromWorld;
    public VehicleInterpolation interpolation;
    public boolean waitFullUpdate;
    public float throttle;
    public double engineSpeed;
    public TransmissionNumber transmissionNumber;
    public final UpdateLimit transmissionChangeTime = new UpdateLimit(1000L);
    public boolean hasExtendOffset = true;
    public boolean hasExtendOffsetExiting;
    public float savedPhysicsZ = Float.NaN;
    public final Quaternionf savedRot = new Quaternionf();
    public final Transform jniTransform = new Transform();
    private float jniSpeed;
    public boolean jniIsCollide;
    public final Vector3f jniLinearVelocity = new Vector3f();
    private final Vector3f lastLinearVelocity = new Vector3f();
    public BaseVehicle.Authorization netPlayerAuthorization = BaseVehicle.Authorization.Server;
    public short netPlayerId = -1;
    public int netPlayerTimeout;
    public int authSimulationHash;
    public long authSimulationTime;
    public int frontEndDurability = 100;
    public int rearEndDurability = 100;
    public float rust;
    public float colorHue;
    public float colorSaturation;
    public float colorValue;
    public int currentFrontEndDurability = 100;
    public int currentRearEndDurability = 100;
    public float collideX = -1.0F;
    public float collideY = -1.0F;
    public final VehiclePoly shadowCoord = new VehiclePoly();
    public BaseVehicle.engineStateTypes engineState = BaseVehicle.engineStateTypes.Idle;
    public long engineLastUpdateStateTime;
    public static final int MAX_WHEELS = 4;
    public static final int PHYSICS_PARAM_COUNT = 27;
    public final BaseVehicle.WheelInfo[] wheelInfo = new BaseVehicle.WheelInfo[4];
    public boolean skidding;
    public long skidSound;
    public long ramSound;
    public long ramSoundTime;
    private VehicleEngineRPM vehicleEngineRpm;
    public final long[] newEngineSoundId = new long[8];
    private long combinedEngineSound;
    public int engineSoundIndex;
    public BaseSoundEmitter alarmEmitter;
    public BaseSoundEmitter hornEmitter;
    public float startTime;
    public boolean headlightsOn;
    public boolean stoplightsOn;
    public boolean windowLightsOn;
    public boolean soundAlarmOn;
    public boolean soundHornOn;
    public boolean soundBackMoveOn;
    public boolean previouslyEntered;
    public boolean previouslyMoved;
    public final LightbarLightsMode lightbarLightsMode = new LightbarLightsMode();
    public final LightbarSirenMode lightbarSirenMode = new LightbarSirenMode();
    private final IsoLightSource leftLight1 = new IsoLightSource(0, 0, 0, 0.0F, 0.0F, 1.0F, 8);
    private final IsoLightSource leftLight2 = new IsoLightSource(0, 0, 0, 0.0F, 0.0F, 1.0F, 8);
    private final IsoLightSource rightLight1 = new IsoLightSource(0, 0, 0, 1.0F, 0.0F, 0.0F, 8);
    private final IsoLightSource rightLight2 = new IsoLightSource(0, 0, 0, 1.0F, 0.0F, 0.0F, 8);
    private int leftLightIndex = -1;
    private int rightLightIndex = -1;
    public final BaseVehicle.ServerVehicleState[] connectionState = new BaseVehicle.ServerVehicleState[512];
    private BaseVehicle.Passenger[] passengers = new BaseVehicle.Passenger[1];
    private String scriptName;
    protected VehicleScript script;
    protected final ArrayList<VehiclePart> parts = new ArrayList<>();
    private VehiclePart battery;
    protected int engineQuality;
    protected int engineLoudness;
    protected int enginePower;
    private long engineCheckTime;
    private final ArrayList<VehiclePart> lights = new ArrayList<>();
    private boolean createdModel;
    private int skinIndex = -1;
    protected CarController physics;
    private boolean created;
    private final VehiclePoly poly = new VehiclePoly();
    private final VehiclePoly polyPlusRadius = new VehiclePoly();
    protected boolean doDamageOverlay;
    private boolean loaded;
    public short updateFlags;
    private long updateLockTimeout;
    private final UpdateLimit limitPhysicSend = new UpdateLimit(300L);
    private Vector2 limitPhysicPositionSent;
    protected final UpdateLimit limitPhysicValid = new UpdateLimit(1000L);
    private final UpdateLimit limitCrash = new UpdateLimit(600L);
    public boolean addedToWorld;
    private boolean removedFromWorld;
    private float polyPlusRadiusMinX = -123.0F;
    private float polyPlusRadiusMinY;
    private float polyPlusRadiusMaxX;
    private float polyPlusRadiusMaxY;
    private float maxSpeed;
    private boolean keyIsOnDoor;
    private boolean hotwired;
    private boolean hotwiredBroken;
    private boolean keysInIgnition;
    public ItemContainer ignitionSwitch = new ItemContainer();
    public int keysContainerId = -1;
    private long soundAlarm = -1L;
    private long soundHorn = -1L;
    private long soundScrapePastPlant = -1L;
    private boolean hittingPlant;
    private long soundBackMoveSignal = -1L;
    public long soundSirenSignal = -1L;
    public long doorAlarmSound;
    private boolean handBrakeActive;
    private long handBrakeSound;
    private final HashMap<String, String> choosenParts = new HashMap<>();
    private String type = "";
    private String respawnZone;
    private float mass;
    private float initialMass;
    private float brakingForce;
    private float baseQuality;
    private float currentSteering;
    private boolean isBraking;
    private int mechanicalId;
    private boolean needPartsUpdate;
    private boolean alarmed;
    private double alarmStartTime;
    private float alarmAccumulator;
    private String chosenAlarmSound;
    private double sirenStartTime;
    private boolean mechanicUiOpen;
    private boolean isGoodCar;
    private InventoryItem currentKey;
    private boolean doColor = true;
    private float breakingSlowFactor;
    private final ArrayList<IsoObject> breakingObjectsList = new ArrayList<>();
    private final UpdateLimit limitUpdate = new UpdateLimit(333L);
    public byte keySpawned;
    public final Matrix4f vehicleTransform = new Matrix4f();
    public final Matrix4f renderTransform = new Matrix4f();
    private BaseSoundEmitter emitter;
    private float brakeBetweenUpdatesSpeed;
    public long physicActiveCheck = -1L;
    private long constraintChangedTime = -1L;
    private AnimationPlayer animPlayer;
    public String specificDistributionId;
    private boolean addThumpWorldSound;
    private final SurroundVehicle surroundVehicle = new SurroundVehicle(this);
    private boolean regulator;
    private float regulatorSpeed;
    private static final HashMap<String, Integer> s_PartToMaskMap = new HashMap<>();
    private static final Byte BYTE_ZERO = (byte)0;
    private final HashMap<String, Byte> bloodIntensity = new HashMap<>();
    private boolean optionBloodDecals;
    private BaseVehicle vehicleTowing;
    private BaseVehicle vehicleTowedBy;
    public int constraintTowing = -1;
    private int vehicleTowingId = -1;
    private int vehicleTowedById = -1;
    private String towAttachmentSelf;
    private String towAttachmentOther;
    private float rowConstraintZOffset;
    private final ParameterVehicleBrake parameterVehicleBrake = new ParameterVehicleBrake(this);
    private final ParameterVehicleEngineCondition parameterVehicleEngineCondition = new ParameterVehicleEngineCondition(this);
    private final ParameterVehicleGear parameterVehicleGear = new ParameterVehicleGear(this);
    private final ParameterVehicleLoad parameterVehicleLoad = new ParameterVehicleLoad(this);
    private final ParameterVehicleRoadMaterial parameterVehicleRoadMaterial = new ParameterVehicleRoadMaterial(this);
    private final ParameterVehicleRPM parameterVehicleRpm = new ParameterVehicleRPM(this);
    private final ParameterVehicleSkid parameterVehicleSkid = new ParameterVehicleSkid(this);
    private final ParameterVehicleSpeed parameterVehicleSpeed = new ParameterVehicleSpeed(this);
    private final ParameterVehicleSteer parameterVehicleSteer = new ParameterVehicleSteer(this);
    private final ParameterVehicleTireMissing parameterVehicleTireMissing = new ParameterVehicleTireMissing(this);
    private final FMODParameterList fmodParameters = new FMODParameterList();
    public boolean isActive;
    public boolean isStatic;
    private final UpdateLimit physicReliableLimit = new UpdateLimit(500L);
    public boolean isReliable;
    public ArrayList<IsoAnimal> animals = new ArrayList<>();
    private float totalAnimalSize;
    private final float keySpawnChancedD100 = (float)SandboxOptions.getInstance().keyLootNew.getValue() * 25.0F;
    public float timeSinceLastAuth = 10.0F;
    private final UpdateLimit updateAnimal = new UpdateLimit(2100L);
    private final BaseVehicle.HitVars hitVars = new BaseVehicle.HitVars();
    private int zombiesHits;
    private long zombieHitTimestamp;
    private int createPhysicsRecursion;
    public static final ThreadLocal<BaseVehicle.TransformPool> TL_transform_pool = ThreadLocal.withInitial(BaseVehicle.TransformPool::new);
    public static final ThreadLocal<BaseVehicle.Vector3ObjectPool> TL_vector3_pool = ThreadLocal.withInitial(BaseVehicle.Vector3ObjectPool::new);
    public static final ThreadLocal<BaseVehicle.Vector2fObjectPool> TL_vector2f_pool = ThreadLocal.withInitial(BaseVehicle.Vector2fObjectPool::new);
    public static final ThreadLocal<BaseVehicle.Vector3fObjectPool> TL_vector3f_pool = ThreadLocal.withInitial(BaseVehicle.Vector3fObjectPool::new);
    public static final ThreadLocal<BaseVehicle.Vector4fObjectPool> TL_vector4f_pool = ThreadLocal.withInitial(BaseVehicle.Vector4fObjectPool::new);
    public static final ThreadLocal<BaseVehicle.Matrix4fObjectPool> TL_matrix4f_pool = ThreadLocal.withInitial(BaseVehicle.Matrix4fObjectPool::new);
    public static final ThreadLocal<BaseVehicle.QuaternionfObjectPool> TL_quaternionf_pool = ThreadLocal.withInitial(BaseVehicle.QuaternionfObjectPool::new);
    private IsoGameCharacter lastDamagedBy;

    public int getSqlId() {
        return this.sqlId;
    }

    public static Matrix4f allocMatrix4f() {
        return TL_matrix4f_pool.get().alloc();
    }

    public static void releaseMatrix4f(Matrix4f v) {
        TL_matrix4f_pool.get().release(v);
    }

    public static Quaternionf allocQuaternionf() {
        return TL_quaternionf_pool.get().alloc();
    }

    public static void releaseQuaternionf(Quaternionf q) {
        TL_quaternionf_pool.get().release(q);
    }

    public static Transform allocTransform() {
        return TL_transform_pool.get().alloc();
    }

    public static void releaseTransform(Transform t) {
        TL_transform_pool.get().release(t);
    }

    public static Vector2 allocVector2() {
        return Vector2ObjectPool.get().alloc();
    }

    public static void releaseVector2(Vector2 v) {
        Vector2ObjectPool.get().release(v);
    }

    public static Vector3 allocVector3() {
        return TL_vector3_pool.get().alloc();
    }

    public static void releaseVector3(Vector3 v) {
        TL_vector3_pool.get().release(v);
    }

    public static Vector2f allocVector2f() {
        return TL_vector2f_pool.get().alloc();
    }

    public static void releaseVector2f(Vector2f vector2f) {
        TL_vector2f_pool.get().release(vector2f);
    }

    public static Vector3f allocVector3f() {
        return TL_vector3f_pool.get().alloc();
    }

    public static void releaseVector4f(Vector4f vector4f) {
        TL_vector4f_pool.get().release(vector4f);
    }

    public static Vector4f allocVector4f() {
        return TL_vector4f_pool.get().alloc();
    }

    public static void releaseVector3f(Vector3f vector3f) {
        TL_vector3f_pool.get().release(vector3f);
    }

    public BaseVehicle(IsoCell cell) {
        super(cell, false);
        this.setCollidable(false);
        this.respawnZone = "";
        this.scriptName = "Base.PickUpTruck";
        this.passengers[0] = new BaseVehicle.Passenger();
        this.waitFullUpdate = false;
        this.savedRot.w = 1.0F;

        for (int i = 0; i < this.wheelInfo.length; i++) {
            this.wheelInfo[i] = new BaseVehicle.WheelInfo();
        }

        if (GameClient.client) {
            this.interpolation = new VehicleInterpolation();
        }

        this.setKeyId(Rand.Next(100000000));
        this.engineSpeed = 0.0;
        this.transmissionNumber = TransmissionNumber.N;
        this.rust = Rand.Next(0, 2);
        this.jniIsCollide = false;

        for (int i = 0; i < 4; i++) {
            lowRiderParam[i] = 0.0F;
        }

        this.fmodParameters.add(this.parameterVehicleBrake);
        this.fmodParameters.add(this.parameterVehicleEngineCondition);
        this.fmodParameters.add(this.parameterVehicleGear);
        this.fmodParameters.add(this.parameterVehicleLoad);
        this.fmodParameters.add(this.parameterVehicleRpm);
        this.fmodParameters.add(this.parameterVehicleRoadMaterial);
        this.fmodParameters.add(this.parameterVehicleSkid);
        this.fmodParameters.add(this.parameterVehicleSpeed);
        this.fmodParameters.add(this.parameterVehicleSteer);
        this.fmodParameters.add(this.parameterVehicleTireMissing);
    }

    public static void LoadAllVehicleTextures() {
        DebugLog.Vehicle.println("BaseVehicle.LoadAllVehicleTextures...");

        for (VehicleScript script : ScriptManager.instance.getAllVehicleScripts()) {
            LoadVehicleTextures(script);
        }
    }

    public static void LoadVehicleTextures(VehicleScript script) {
        if (SystemDisabler.doVehiclesWithoutTextures) {
            VehicleScript.Skin skin = script.getSkin(0);
            skin.textureData = LoadVehicleTexture(skin.texture);
            skin.textureDataMask = LoadVehicleTexture("vehicles_placeholder_mask");
            skin.textureDataDamage1Overlay = LoadVehicleTexture("vehicles_placeholder_damage1overlay");
            skin.textureDataDamage1Shell = LoadVehicleTexture("vehicles_placeholder_damage1shell");
            skin.textureDataDamage2Overlay = LoadVehicleTexture("vehicles_placeholder_damage2overlay");
            skin.textureDataDamage2Shell = LoadVehicleTexture("vehicles_placeholder_damage2shell");
            skin.textureDataLights = LoadVehicleTexture("vehicles_placeholder_lights");
            skin.textureDataRust = LoadVehicleTexture("vehicles_placeholder_rust");
        } else {
            for (int i = 0; i < script.getSkinCount(); i++) {
                VehicleScript.Skin skin = script.getSkin(i);
                skin.copyMissingFrom(script.getTextures());
                LoadVehicleTextures(skin);
            }
        }
    }

    private static void LoadVehicleTextures(VehicleScript.Skin skin) {
        skin.textureData = LoadVehicleTexture(skin.texture);
        if (skin.textureMask != null) {
            int flags = 0;
            flags |= 256;
            skin.textureDataMask = LoadVehicleTexture(skin.textureMask, flags);
        }

        skin.textureDataDamage1Overlay = LoadVehicleTexture(skin.textureDamage1Overlay);
        skin.textureDataDamage1Shell = LoadVehicleTexture(skin.textureDamage1Shell);
        skin.textureDataDamage2Overlay = LoadVehicleTexture(skin.textureDamage2Overlay);
        skin.textureDataDamage2Shell = LoadVehicleTexture(skin.textureDamage2Shell);
        skin.textureDataLights = LoadVehicleTexture(skin.textureLights);
        skin.textureDataRust = LoadVehicleTexture(skin.textureRust);
        skin.textureDataShadow = LoadVehicleTexture(skin.textureShadow);
    }

    public static Texture LoadVehicleTexture(String name) {
        int flags = 0;
        flags |= TextureID.useCompression ? 4 : 0;
        flags |= 256;
        return LoadVehicleTexture(name, flags);
    }

    public static Texture LoadVehicleTexture(String name, int flags) {
        return StringUtils.isNullOrWhitespace(name) ? null : Texture.getSharedTexture("media/textures/" + name + ".png", flags);
    }

    public void setNetPlayerAuthorization(BaseVehicle.Authorization netPlayerAuthorization, int netPlayerId) {
        this.netPlayerAuthorization = netPlayerAuthorization;
        this.timeSinceLastAuth = 10.0F;
        this.netPlayerId = (short)netPlayerId;
        this.netPlayerTimeout = netPlayerId == -1 ? 0 : 30;
        if (GameClient.client) {
            boolean isLocal = BaseVehicle.Authorization.Local == netPlayerAuthorization || BaseVehicle.Authorization.LocalCollide == netPlayerAuthorization;
            this.setPhysicsActive(isLocal);
        }

        DebugLog.Vehicle
            .trace(
                "vid%s=%d pid=%d %s",
                this.getVehicleTowing() != null ? "-a" : (this.getVehicleTowedBy() != null ? "-b" : ""),
                this.getId(),
                netPlayerId,
                netPlayerAuthorization.name()
            );
    }

    public boolean isNetPlayerAuthorization(BaseVehicle.Authorization netPlayerAuthorization) {
        return this.netPlayerAuthorization == netPlayerAuthorization;
    }

    public boolean isNetPlayerId(short netPlayerId) {
        return this.netPlayerId == netPlayerId;
    }

    public short getNetPlayerId() {
        return this.netPlayerId;
    }

    public String getAuthorizationDescription() {
        return String.format(
            "vid:%s(%d) pid:(%d) auth=%s static=%b active=%b",
            this.scriptName,
            this.vehicleId,
            this.netPlayerId,
            this.netPlayerAuthorization.name(),
            this.isStatic,
            this.isActive
        );
    }

    public static float getFakeSpeedModifier() {
        if (!GameClient.client && !GameServer.server) {
            return 1.0F;
        } else {
            float limit = (float)ServerOptions.instance.speedLimit.getValue();
            return 120.0F / Math.min(limit, 120.0F);
        }
    }

    public boolean isLocalPhysicSim() {
        return GameServer.server
            ? this.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)
            : this.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide) || this.isNetPlayerAuthorization(BaseVehicle.Authorization.Local);
    }

    public void addImpulse(Vector3f impulse, Vector3f rel_pos) {
        if (!this.impulseFromServer.enable) {
            this.impulseFromServer.enable = true;
            this.impulseFromServer.impulse.set(impulse);
            this.impulseFromServer.relPos.set(rel_pos);
        } else if (this.impulseFromServer.impulse.length() < impulse.length()) {
            this.impulseFromServer.impulse.set(impulse);
            this.impulseFromServer.relPos.set(rel_pos);
            this.impulseFromServer.enable = false;
            this.impulseFromServer.release();
        }
    }

    public double getEngineSpeed() {
        return this.engineSpeed;
    }

    public String getTransmissionNumberLetter() {
        return this.transmissionNumber.getString();
    }

    public int getTransmissionNumber() {
        return this.transmissionNumber.getIndex();
    }

    public void setClientForce(float force) {
        this.physics.clientForce = force;
    }

    public float getClientForce() {
        return this.physics.clientForce;
    }

    public float getForce() {
        return this.physics.engineForce - this.physics.brakingForce;
    }

    private void doVehicleColor() {
        if (!this.isDoColor()) {
            this.colorSaturation = 0.1F;
            this.colorValue = 0.9F;
        } else {
            this.colorHue = Rand.Next(0.0F, 0.0F);
            this.colorSaturation = 0.5F;
            this.colorValue = Rand.Next(0.3F, 0.6F);
            int rng = Rand.Next(100);
            if (rng < 20) {
                this.colorHue = Rand.Next(0.0F, 0.03F);
                this.colorSaturation = Rand.Next(0.85F, 1.0F);
                this.colorValue = Rand.Next(0.55F, 0.85F);
            } else if (rng < 32) {
                this.colorHue = Rand.Next(0.55F, 0.61F);
                this.colorSaturation = Rand.Next(0.85F, 1.0F);
                this.colorValue = Rand.Next(0.65F, 0.75F);
            } else if (rng < 67) {
                this.colorHue = 0.15F;
                this.colorSaturation = Rand.Next(0.0F, 0.1F);
                this.colorValue = Rand.Next(0.7F, 0.8F);
            } else if (rng < 89) {
                this.colorHue = Rand.Next(0.0F, 1.0F);
                this.colorSaturation = Rand.Next(0.0F, 0.1F);
                this.colorValue = Rand.Next(0.1F, 0.25F);
            } else {
                this.colorHue = Rand.Next(0.0F, 1.0F);
                this.colorSaturation = Rand.Next(0.6F, 0.75F);
                this.colorValue = Rand.Next(0.3F, 0.7F);
            }

            if (this.getScript() != null) {
                if (this.getScript().getForcedHue() > -1.0F) {
                    this.colorHue = this.getScript().getForcedHue();
                }

                if (this.getScript().getForcedSat() > -1.0F) {
                    this.colorSaturation = this.getScript().getForcedSat();
                }

                if (this.getScript().getForcedVal() > -1.0F) {
                    this.colorValue = this.getScript().getForcedVal();
                }
            }
        }
    }

    @Override
    public String getObjectName() {
        return "Vehicle";
    }

    @Override
    public boolean Serialize() {
        return true;
    }

    public void createPhysics() {
        this.createPhysics(false);
    }

    public void createPhysics(boolean spawnSwap) {
        if (!GameClient.client && this.vehicleId == -1) {
            this.vehicleId = VehicleIDMap.instance.allocateID();
            if (GameServer.server) {
                VehicleManager.instance.registerVehicle(this);
            } else {
                VehicleIDMap.instance.put(this.vehicleId, this);
            }
        }

        if (this.script == null) {
            this.setScript(this.scriptName);
        }

        try {
            this.createPhysicsRecursion++;
            if (this.createPhysicsRecursion == 1) {
                if (!spawnSwap) {
                    LuaEventManager.triggerEvent("OnSpawnVehicleStart", this);
                }

                if (this.physics != null) {
                    return;
                }
            }
        } finally {
            this.createPhysicsRecursion--;
        }

        if (this.script != null) {
            if (this.skinIndex == -1) {
                this.setSkinIndex(Rand.Next(this.getSkinCount()));
            }

            if (!GameServer.server) {
                WorldSimulation.instance.create();
            }

            this.jniTransform
                .origin
                .set(
                    this.getX() - WorldSimulation.instance.offsetX,
                    Float.isNaN(this.savedPhysicsZ) ? this.getZ() : this.savedPhysicsZ,
                    this.getY() - WorldSimulation.instance.offsetY
                );
            this.physics = new CarController(this);
            this.savedPhysicsZ = Float.NaN;
            if (!this.created) {
                this.created = true;
                int recentlySurvivorVehiclesChance = 30;
                if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 1) {
                    recentlySurvivorVehiclesChance = 0;
                }

                if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 2) {
                    recentlySurvivorVehiclesChance = 10;
                }

                if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 3) {
                    recentlySurvivorVehiclesChance = 30;
                }

                if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 4) {
                    recentlySurvivorVehiclesChance = 50;
                }

                if (Rand.Next(100) < recentlySurvivorVehiclesChance) {
                    this.setGoodCar(true);
                }
            }

            this.createParts();
            this.initParts();
            if (!this.createdModel) {
                ModelManager.instance.addVehicle(this);
                this.createdModel = true;
            }

            this.updateTransform();
            this.lights.clear();

            for (int i = 0; i < this.parts.size(); i++) {
                VehiclePart part = this.parts.get(i);
                if (part.getLight() != null) {
                    this.lights.add(part);
                }
            }

            this.setMaxSpeed(this.getScript().maxSpeed);
            this.setInitialMass(this.getScript().getMass());
            if (!this.getCell().getVehicles().contains(this) && !this.getCell().addVehicles.contains(this)) {
                this.getCell().addVehicles.add(this);
            }

            this.square = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
            if (!this.shouldNotHaveLoot()) {
                this.randomizeContainers();
            }

            if (this.engineState == BaseVehicle.engineStateTypes.Running) {
                this.engineDoRunning();
            }

            this.updateTotalMass();
            this.doDamageOverlay = true;
            this.updatePartStats();
            this.mechanicalId = Rand.Next(100000);
            LuaEventManager.triggerEvent("OnSpawnVehicleEnd", this);
        }
    }

    public boolean isPreviouslyEntered() {
        return this.previouslyEntered;
    }

    public void setPreviouslyEntered(boolean bool) {
        this.previouslyEntered = bool;
    }

    public boolean isPreviouslyMoved() {
        return this.previouslyMoved;
    }

    public void setPreviouslyMoved(boolean bool) {
        this.previouslyMoved = bool;
    }

    @Override
    public int getKeyId() {
        return this.keyId;
    }

    public boolean getKeySpawned() {
        return this.keySpawned != 0;
    }

    private InventoryContainer tryCreateKeyRing() {
        String keyRingType = ItemKey.Container.KEY_RING.toString();
        if (this.getScript().hasSpecialKeyRing() && Rand.Next(100) < 1.0F * this.getSpecialKeyRingChance()) {
            keyRingType = this.getScript().getRandomSpecialKeyRing();
        }

        if (InventoryItemFactory.CreateItem(keyRingType) instanceof InventoryContainer keyRing && keyRing.getInventory() != null) {
            this.keyNamerVehicle(keyRing);
            return keyRing;
        } else {
            return null;
        }
    }

    private InventoryItem tryCreateBuildingKey(BuildingDef buildingDef) {
        if (buildingDef != null && buildingDef.getKeyId() != -1) {
            ItemKey keyType = ItemKey.Key.KEY_1;
            InventoryItem key = InventoryItemFactory.CreateItem(keyType);
            if (key == null) {
                return null;
            } else {
                key.setKeyId(buildingDef.getKeyId());
                IsoGridSquare square = buildingDef.getFreeSquareInRoom();
                if (square != null) {
                    ItemPickerJava.KeyNamer.nameKey(key, square);
                }

                return key;
            }
        } else {
            return null;
        }
    }

    private InventoryItem randomlyAddNearestBuildingKeyToContainer(ItemContainer container) {
        if (Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
            return null;
        } else {
            float px = this.getX();
            float py = this.getY();
            BuildingDef buildingDef = AmbientStreamManager.getNearestBuilding(px, py);
            InventoryItem houseKey = this.tryCreateBuildingKey(buildingDef);
            if (houseKey != null) {
                container.AddItem(houseKey);
                return houseKey;
            } else {
                return null;
            }
        }
    }

    public void putKeyToZombie(IsoZombie zombie) {
        if (zombie.shouldZombieHaveKey(true)) {
            if (this.checkZombieKeyForVehicle(zombie)) {
                InventoryItem key = this.createVehicleKey();
                if (key != null) {
                    this.keySpawned = 1;
                    if (Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
                        zombie.getInventory().AddItem(key);
                    } else {
                        InventoryContainer keyRing = this.tryCreateKeyRing();
                        if (keyRing == null) {
                            zombie.getInventory().AddItem(key);
                            return;
                        }

                        keyRing.getInventory().AddItem(key);
                        this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
                        zombie.getInventory().AddItem(keyRing);
                    }
                }
            }
        }
    }

    public void putKeyToContainer(ItemContainer container, IsoGridSquare sq, IsoObject obj) {
        InventoryItem key = this.createVehicleKey();
        if (key != null) {
            this.keySpawned = 1;
            if (Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
                container.AddItem(key);
                this.putKeyToContainerServer(key, sq, obj);
            } else {
                InventoryContainer keyRing = this.tryCreateKeyRing();
                if (keyRing == null) {
                    container.AddItem(key);
                    this.putKeyToContainerServer(key, sq, obj);
                    return;
                }

                keyRing.getInventory().AddItem(key);
                if (sq.getBuilding() != null && sq.getBuilding().getDef() != null && sq.getBuilding().getDef().getKeyId() != -1 && Rand.Next(10) != 0) {
                    InventoryItem houseKey = this.tryCreateBuildingKey(sq.getBuilding().getDef());
                    if (houseKey != null) {
                        keyRing.getInventory().AddItem(houseKey);
                    }
                }

                container.AddItem(keyRing);
                this.putKeyToContainerServer(keyRing, sq, obj);
            }
        }
    }

    public void putKeyToContainerServer(InventoryItem item, IsoGridSquare sq, IsoObject obj) {
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, obj.square.x, obj.square.y, this.container, item);
        }
    }

    public void putKeyToWorld(IsoGridSquare sq) {
        InventoryItem key = this.createVehicleKey();
        if (key != null) {
            this.keySpawned = 1;
            if (Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
                sq.AddWorldInventoryItem(key, 0.0F, 0.0F, 0.0F);
            } else {
                InventoryContainer keyRing = this.tryCreateKeyRing();
                if (keyRing == null) {
                    sq.AddWorldInventoryItem(key, 0.0F, 0.0F, 0.0F);
                    return;
                }

                keyRing.getInventory().AddItem(key);
                this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
                sq.AddWorldInventoryItem(keyRing, 0.0F, 0.0F, 0.0F);
            }
        }
    }

    public void addKeyToWorld() {
        this.addKeyToWorld(false);
    }

    public void addKeyToWorld(boolean crashed) {
        if (!this.isPreviouslyEntered() && !this.isPreviouslyMoved() && !this.isHotwired() && !this.isBurnt()) {
            if (this.isInTrafficJam()) {
                crashed = true;
            }

            if (this.checkIfGoodVehicleForKey()) {
                if (!this.getScriptName().contains("Burnt") && !this.getScriptName().equals("Trailer") && !this.getScriptName().equals("TrailerAdvert")) {
                    if (!this.getScriptName().contains("Smashed") && this.haveOneDoorUnlocked()) {
                        if (Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                            this.keysInIgnition = true;
                            this.currentKey = this.createVehicleKey();
                            this.ignitionSwitch.addItem(this.currentKey);
                            this.keySpawned = 1;
                            return;
                        }

                        if (Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                            this.addKeyToGloveBox();
                            return;
                        }
                    }

                    IsoGridSquare sq = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
                    if (sq != null) {
                        boolean var3 = this.addKeyToSquare(sq, crashed || this.isBurntOrSmashed());
                    }
                }
            }
        }
    }

    public void addKeyToGloveBox() {
        if (this.keySpawned == 0) {
            VehiclePart glovebox = this.getPartById("GloveBox");
            if (glovebox != null) {
                InventoryItem key = this.createVehicleKey();
                if (key != null) {
                    this.keySpawned = 1;
                    if (Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
                        glovebox.container.addItem(key);
                        this.randomlyAddNearestBuildingKeyToContainer(glovebox.container);
                    } else {
                        InventoryContainer keyRing = this.tryCreateKeyRing();
                        if (keyRing == null) {
                            glovebox.container.addItem(key);
                            this.randomlyAddNearestBuildingKeyToContainer(glovebox.container);
                            return;
                        }

                        keyRing.getInventory().AddItem(key);
                        this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
                        glovebox.container.addItem(keyRing);
                    }
                }
            }
        }
    }

    public void addBuildingKeyToGloveBox(IsoGridSquare square) {
        VehiclePart glovebox = this.getPartById("GloveBox");
        if (glovebox != null) {
            if (square.getBuilding() != null && square.getBuilding().getDef() != null) {
                BuildingDef buildingDef = square.getBuilding().getDef();
                InventoryItem houseKey = this.tryCreateBuildingKey(buildingDef);
                if (houseKey != null) {
                    if (Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
                        glovebox.container.AddItem(houseKey);
                    } else {
                        InventoryContainer keyRing = this.tryCreateKeyRing();
                        if (keyRing == null) {
                            glovebox.container.AddItem(houseKey);
                            return;
                        }

                        keyRing.getInventory().AddItem(houseKey);
                        glovebox.container.addItem(keyRing);
                    }
                }
            }
        }
    }

    public InventoryItem createVehicleKey() {
        InventoryItem item = InventoryItemFactory.CreateItem(ItemKey.Key.CAR_KEY);
        if (item == null) {
            return null;
        } else {
            item.setKeyId(this.getKeyId());
            keyNamerVehicle(item, this);
            Color newC = Color.HSBtoRGB(this.colorHue, this.colorSaturation * 0.5F, this.colorValue);
            item.setColor(newC);
            item.setCustomColor(true);
            return item;
        }
    }

    public boolean addKeyToSquare(IsoGridSquare sq) {
        return this.addKeyToSquare(sq, false);
    }

    public boolean addKeyToSquare(IsoGridSquare sq, boolean crashed) {
        boolean isKeyIssued = false;

        for (int z = 0; z < 3; z++) {
            if (Rand.Next(2) == 0) {
                for (int x2 = sq.getX() - 10; x2 < sq.getX() + 10; x2++) {
                    isKeyIssued = this.addKeyToSquare2(sq, x2, crashed);
                    if (isKeyIssued) {
                        return true;
                    }
                }
            } else {
                for (int x2x = sq.getX() + 10; x2x > sq.getX() - 10; x2x--) {
                    isKeyIssued = this.addKeyToSquare2(sq, x2x, crashed);
                    if (isKeyIssued) {
                        return true;
                    }
                }
            }
        }

        if (Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
            for (int i = 0; i < 100; i++) {
                int x = sq.getX() - 10 + Rand.Next(20);
                int y = sq.getY() - 10 + Rand.Next(20);
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x, (double)y, (double)this.getZ());
                if (square != null && !square.isSolid() && !square.isSolidTrans() && !square.HasTree()) {
                    this.putKeyToWorld(square);
                    return true;
                }
            }
        }

        return isKeyIssued;
    }

    public boolean addKeyToSquare2(IsoGridSquare sq, int x2) {
        return this.addKeyToSquare2(sq, x2, false);
    }

    public boolean addKeyToSquare2(IsoGridSquare sq, int x2, boolean crashed) {
        boolean isKeyIssued = false;
        if (Rand.Next(100) < 50) {
            for (int y = sq.getY() - 10; y < sq.getY() + 10; y++) {
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x2, (double)y, (double)this.getZ());
                if (square != null) {
                    isKeyIssued = this.checkSquareForVehicleKeySpot(square, crashed);
                    if (isKeyIssued) {
                        return true;
                    }
                }
            }
        } else {
            for (int yx = sq.getY() + 10; yx > sq.getY() - 10; yx--) {
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x2, (double)yx, (double)this.getZ());
                if (square != null) {
                    isKeyIssued = this.checkSquareForVehicleKeySpot(square, crashed);
                    if (isKeyIssued) {
                        return true;
                    }
                }
            }
        }

        return isKeyIssued;
    }

    public void toggleLockedDoor(VehiclePart part, IsoGameCharacter chr, boolean locked) {
        if (locked) {
            if (!this.canLockDoor(part, chr)) {
                return;
            }

            part.getDoor().setLocked(true);
        } else {
            if (!this.canUnlockDoor(part, chr)) {
                return;
            }

            part.getDoor().setLocked(false);
        }
    }

    public boolean canLockDoor(VehiclePart part, IsoGameCharacter chr) {
        if (part == null) {
            return false;
        } else if (chr == null) {
            return false;
        } else {
            VehicleDoor door = part.getDoor();
            if (door == null) {
                return false;
            } else if (door.lockBroken) {
                return false;
            } else if (door.locked) {
                return false;
            } else if (this.getSeat(chr) != -1) {
                return true;
            } else if (chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
                return true;
            } else {
                VehiclePart windowPart = part.getChildWindow();
                if (windowPart != null && windowPart.getInventoryItem() == null) {
                    return true;
                } else {
                    VehicleWindow window = windowPart == null ? null : windowPart.getWindow();
                    return window != null && (window.isOpen() || window.isDestroyed());
                }
            }
        }
    }

    public boolean canUnlockDoor(VehiclePart part, IsoGameCharacter chr) {
        if (part == null) {
            return false;
        } else if (chr == null) {
            return false;
        } else {
            VehicleDoor door = part.getDoor();
            if (door == null) {
                return false;
            } else if (door.lockBroken) {
                return false;
            } else if (!door.locked) {
                return false;
            } else if (this.getSeat(chr) != -1) {
                return true;
            } else if (chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
                return true;
            } else {
                VehiclePart windowPart = part.getChildWindow();
                if (windowPart != null && windowPart.getInventoryItem() == null) {
                    return true;
                } else {
                    VehicleWindow window = windowPart == null ? null : windowPart.getWindow();
                    return window != null && (window.isOpen() || window.isDestroyed());
                }
            }
        }
    }

    public boolean canOpenDoor(VehiclePart part, IsoGameCharacter chr) {
        if (part == null) {
            return false;
        } else if (chr == null) {
            return false;
        } else {
            VehicleDoor door = part.getDoor();
            if (door == null) {
                return false;
            } else if (door.lockBroken) {
                return false;
            } else if (!door.locked) {
                return true;
            } else if (this.getSeat(chr) != -1) {
                return true;
            } else if (chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
                return true;
            } else {
                VehiclePart windowPart = part.getChildWindow();
                if (windowPart != null && windowPart.getInventoryItem() == null) {
                    return true;
                } else {
                    VehicleWindow window = windowPart == null ? null : windowPart.getWindow();
                    return window != null && (window.isOpen() || window.isDestroyed());
                }
            }
        }
    }

    private void initParts() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("init");
            if (functionName != null) {
                this.callLuaVoid(functionName, this, part);
            }
        }
    }

    public void setGeneralPartCondition(float baseQuality, float chanceToSpawnDamaged) {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            part.setGeneralCondition(null, baseQuality, chanceToSpawnDamaged);
        }
    }

    private void createParts() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            ArrayList<String> itemType = part.getItemType();
            if (part.created && itemType != null && !itemType.isEmpty() && part.getInventoryItem() == null && part.getTable("install") == null) {
                part.created = false;
            } else if ((itemType == null || itemType.isEmpty()) && part.getInventoryItem() != null) {
                part.item = null;
            }

            if (!part.created) {
                part.created = true;
                String functionName = part.getLuaFunction("create");
                if (functionName == null) {
                    part.setRandomCondition(null);
                } else {
                    this.callLuaVoid(functionName, this, part);
                    if (part.getCondition() == -1) {
                        part.setRandomCondition(null);
                    }
                }
            }
        }

        if (this.hasLightbar() && this.getScript().rightSirenCol != null && this.getScript().leftSirenCol != null) {
            this.leftLight1.r = this.leftLight2.r = this.getScript().leftSirenCol.r;
            this.leftLight1.g = this.leftLight2.g = this.getScript().leftSirenCol.g;
            this.leftLight1.b = this.leftLight2.b = this.getScript().leftSirenCol.b;
            this.rightLight1.r = this.rightLight2.r = this.getScript().rightSirenCol.r;
            this.rightLight1.g = this.rightLight2.g = this.getScript().rightSirenCol.g;
            this.rightLight1.b = this.rightLight2.b = this.getScript().rightSirenCol.b;
        }
    }

    public CarController getController() {
        return this.physics;
    }

    public SurroundVehicle getSurroundVehicle() {
        return this.surroundVehicle;
    }

    public int getSkinCount() {
        return this.script.getSkinCount();
    }

    public int getSkinIndex() {
        return this.skinIndex;
    }

    public void setSkinIndex(int index) {
        if (index >= 0 && index <= this.getSkinCount()) {
            this.skinIndex = index;
        }
    }

    public void updateSkin() {
        if (this.sprite != null && this.sprite.modelSlot != null && this.sprite.modelSlot.model != null) {
            VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
            VehicleScript.Skin skin = this.script.getTextures();
            VehicleScript script = this.getScript();
            if (this.getSkinIndex() >= 0 && this.getSkinIndex() < script.getSkinCount()) {
                skin = script.getSkin(this.getSkinIndex());
            }

            inst.LoadTexture(skin.texture);
            inst.tex = skin.textureData;
            inst.textureMask = skin.textureDataMask;
            inst.textureDamage1Overlay = skin.textureDataDamage1Overlay;
            inst.textureDamage1Shell = skin.textureDataDamage1Shell;
            inst.textureDamage2Overlay = skin.textureDataDamage2Overlay;
            inst.textureDamage2Shell = skin.textureDataDamage2Shell;
            inst.textureLights = skin.textureDataLights;
            inst.textureRust = skin.textureDataRust;
            if (inst.tex != null) {
                inst.tex.bindAlways = true;
            } else {
                DebugLog.Animation.error("texture not found:", this.getSkin());
            }
        }
    }

    public Texture getShadowTexture() {
        if (this.getScript() != null) {
            VehicleScript.Skin skin = this.getScript().getTextures();
            if (this.getSkinIndex() >= 0 && this.getSkinIndex() < this.getScript().getSkinCount()) {
                skin = this.getScript().getSkin(this.getSkinIndex());
            }

            if (skin.textureDataShadow != null) {
                return skin.textureDataShadow;
            }
        }

        if (vehicleShadow == null) {
            int flags = 0;
            flags |= TextureID.useCompression ? 4 : 0;
            vehicleShadow = Texture.getSharedTexture("media/vehicleShadow.png", flags);
        }

        return vehicleShadow;
    }

    public VehicleScript getScript() {
        return this.script;
    }

    public void setScript(String name) {
        if (!StringUtils.isNullOrWhitespace(name)) {
            this.scriptName = name;
            boolean hadScript = this.script != null;
            this.script = ScriptManager.instance.getVehicle(this.scriptName);
            if (this.script == null) {
                ArrayList<VehicleScript> scripts = ScriptManager.instance.getAllVehicleScripts();
                if (!scripts.isEmpty()) {
                    ArrayList<VehicleScript> scriptsBurnt = new ArrayList<>();

                    for (int i = 0; i < scripts.size(); i++) {
                        VehicleScript script1 = scripts.get(i);
                        if (script1.getWheelCount() == 0) {
                            scriptsBurnt.add(script1);
                            scripts.remove(i--);
                        }
                    }

                    boolean isBurnt = this.loaded && this.parts.isEmpty() || this.scriptName.contains("Burnt");
                    if (isBurnt && !scriptsBurnt.isEmpty()) {
                        this.script = scriptsBurnt.get(Rand.Next(scriptsBurnt.size()));
                    } else if (!scripts.isEmpty()) {
                        this.script = scripts.get(Rand.Next(scripts.size()));
                    }

                    if (this.script != null) {
                        this.scriptName = this.script.getFullName();
                    }
                }
            }

            this.battery = null;
            this.models.clear();
            if (this.script != null) {
                this.scriptName = this.script.getFullName();
                BaseVehicle.Passenger[] oldPassengers = this.passengers;
                this.passengers = new BaseVehicle.Passenger[this.script.getPassengerCount()];

                for (int ix = 0; ix < this.passengers.length; ix++) {
                    if (ix < oldPassengers.length) {
                        this.passengers[ix] = oldPassengers[ix];
                    } else {
                        this.passengers[ix] = new BaseVehicle.Passenger();
                    }
                }

                ArrayList<VehiclePart> oldParts = new ArrayList<>();
                oldParts.addAll(this.parts);
                this.parts.clear();

                for (int ixx = 0; ixx < this.script.getPartCount(); ixx++) {
                    VehicleScript.Part scriptPart = this.script.getPart(ixx);
                    VehiclePart part = null;

                    for (int j = 0; j < oldParts.size(); j++) {
                        VehiclePart oldPart = oldParts.get(j);
                        if (oldPart.getScriptPart() != null && scriptPart.id.equals(oldPart.getScriptPart().id)) {
                            part = oldPart;
                            break;
                        }

                        if (oldPart.partId != null && scriptPart.id.equals(oldPart.partId)) {
                            part = oldPart;
                            break;
                        }
                    }

                    if (part == null) {
                        part = new VehiclePart(this);
                    }

                    part.setScriptPart(scriptPart);
                    part.category = scriptPart.category;
                    part.specificItem = scriptPart.specificItem;
                    part.setDurability(scriptPart.getDurability());
                    if (scriptPart.container != null && scriptPart.container.contentType == null) {
                        if (part.getItemContainer() == null) {
                            ItemContainer container = new ItemContainer(scriptPart.id, null, this);
                            part.setItemContainer(container);
                            container.id = 0;
                        }

                        part.getItemContainer().capacity = scriptPart.container.capacity;
                    } else {
                        part.setItemContainer(null);
                    }

                    if (scriptPart.door == null) {
                        part.door = null;
                    } else if (part.door == null) {
                        part.door = new VehicleDoor(part);
                        part.door.init(scriptPart.door);
                    }

                    if (scriptPart.window == null) {
                        part.window = null;
                    } else if (part.window == null) {
                        part.window = new VehicleWindow(part);
                        part.window.init(scriptPart.window);
                    } else {
                        part.window.openable = scriptPart.window.openable;
                    }

                    part.parent = null;
                    if (part.children != null) {
                        part.children.clear();
                    }

                    this.parts.add(part);
                    if ("Battery".equals(part.getId())) {
                        this.battery = part;
                    }
                }

                for (int ixx = 0; ixx < this.script.getPartCount(); ixx++) {
                    VehiclePart part = this.parts.get(ixx);
                    VehicleScript.Part scriptPart = part.getScriptPart();
                    if (scriptPart.parent != null) {
                        part.parent = this.getPartById(scriptPart.parent);
                        if (part.parent != null) {
                            part.parent.addChild(part);
                        }
                    }
                }

                if (!hadScript && !this.loaded) {
                    this.frontEndDurability = this.rearEndDurability = 99999;
                }

                this.frontEndDurability = Math.min(this.frontEndDurability, this.script.getFrontEndHealth());
                this.rearEndDurability = Math.min(this.rearEndDurability, this.script.getRearEndHealth());
                this.currentFrontEndDurability = this.frontEndDurability;
                this.currentRearEndDurability = this.rearEndDurability;

                for (int ixxx = 0; ixxx < this.script.getPartCount(); ixxx++) {
                    VehiclePart part = this.parts.get(ixxx);
                    part.setInventoryItem(part.item);
                }
            }

            if (!this.loaded || this.colorHue == 0.0F && this.colorSaturation == 0.0F && this.colorValue == 0.0F) {
                this.doVehicleColor();
            }

            this.surroundVehicle.reset();
        }
    }

    @Override
    public String getScriptName() {
        return this.scriptName;
    }

    public void setScriptName(String name) {
        assert name == null || name.contains(".");

        this.scriptName = name;
    }

    public void setScript() {
        this.setScript(this.scriptName);
    }

    public void scriptReloaded() {
        this.scriptReloaded(false);
    }

    public void scriptReloaded(boolean spawnSwap) {
        if (this.physics != null) {
            Transform xfrm = allocTransform();
            xfrm.setIdentity();
            this.getWorldTransform(xfrm);
            xfrm.basis.getUnnormalizedRotation(this.savedRot);
            releaseTransform(xfrm);
            this.breakConstraint(false, false);
            Bullet.removeVehicle(this.vehicleId);
            this.physics = null;
        }

        if (this.createdModel) {
            ModelManager.instance.Remove(this);
            this.createdModel = false;
        }

        this.vehicleEngineRpm = null;

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            part.setInventoryItem(null);
            part.created = false;
        }

        this.setScript(this.scriptName);
        this.createPhysics(spawnSwap);
        if (this.script != null) {
            for (int i = 0; i < this.passengers.length; i++) {
                BaseVehicle.Passenger passenger = this.passengers[i];
                if (passenger != null && passenger.character != null) {
                    VehicleScript.Position pp = this.getPassengerPosition(i, "inside");
                    if (pp != null) {
                        passenger.offset.set(pp.offset);
                    }
                }
            }
        }

        this.polyDirty = true;
        if (this.isEngineRunning()) {
            this.engineDoShuttingDown();
            this.engineState = BaseVehicle.engineStateTypes.Idle;
        }

        if (this.addedToWorld) {
            if (PathfindNative.useNativeCode) {
                PathfindNative.instance.removeVehicle(this);
                PathfindNative.instance.addVehicle(this);
            } else {
                PolygonalMap2.instance.removeVehicleFromWorld(this);
                PolygonalMap2.instance.addVehicleToWorld(this);
            }
        }
    }

    public String getSkin() {
        if (this.script != null && this.script.getSkinCount() != 0) {
            if (this.skinIndex < 0 || this.skinIndex >= this.script.getSkinCount()) {
                this.skinIndex = Rand.Next(this.script.getSkinCount());
            }

            return this.script.getSkin(this.skinIndex).texture;
        } else {
            return "BOGUS";
        }
    }

    public BaseVehicle.ModelInfo setModelVisible(VehiclePart part, VehicleScript.Model scriptModel, boolean visible) {
        for (int i = 0; i < this.models.size(); i++) {
            BaseVehicle.ModelInfo info = this.models.get(i);
            if (info.part == part && info.scriptModel == scriptModel) {
                if (visible) {
                    return info;
                }

                if (info.animPlayer != null) {
                    info.animPlayer = Pool.tryRelease(info.animPlayer);
                }

                this.models.remove(i);
                if (this.createdModel) {
                    ModelManager.instance.Remove(this);
                    ModelManager.instance.addVehicle(this);
                }

                part.updateFlags = (short)(part.updateFlags | 64);
                this.updateFlags = (short)(this.updateFlags | 64);
                return null;
            }
        }

        if (visible) {
            String modelScriptName = this.getModelScriptNameForPart(part, scriptModel);
            if (modelScriptName == null) {
                return null;
            } else {
                BaseVehicle.ModelInfo info = new BaseVehicle.ModelInfo();
                info.part = part;
                info.scriptModel = scriptModel;
                info.modelScript = ScriptManager.instance.getModelScript(modelScriptName);
                info.wheelIndex = part.getWheelIndex();
                this.models.add(info);
                if (this.createdModel) {
                    ModelManager.instance.Remove(this);
                    ModelManager.instance.addVehicle(this);
                }

                part.updateFlags = (short)(part.updateFlags | 64);
                this.updateFlags = (short)(this.updateFlags | 64);
                return info;
            }
        } else {
            return null;
        }
    }

    private String getModelScriptNameForPart(VehiclePart part, VehicleScript.Model scriptModel) {
        String modelScriptName = scriptModel.file;
        if (modelScriptName == null) {
            InventoryItem item = part.getInventoryItem();
            if (item == null) {
                return null;
            }

            ArrayList<VehiclePartModel> vehiclePartModels = item.getScriptItem().getVehiclePartModels();
            if (vehiclePartModels == null || vehiclePartModels.isEmpty()) {
                return null;
            }

            for (int i = 0; i < vehiclePartModels.size(); i++) {
                VehiclePartModel vehiclePartModel = vehiclePartModels.get(i);
                if (vehiclePartModel.partId.equalsIgnoreCase(part.getId()) && vehiclePartModel.partModelId.equalsIgnoreCase(scriptModel.getId())) {
                    modelScriptName = vehiclePartModel.modelId;
                    break;
                }
            }
        }

        return modelScriptName;
    }

    private BaseVehicle.ModelInfo getModelInfoForPart(VehiclePart part) {
        for (int i = 0; i < this.models.size(); i++) {
            BaseVehicle.ModelInfo modelInfo = this.models.get(i);
            if (modelInfo.part == part) {
                return modelInfo;
            }
        }

        return null;
    }

    private VehicleScript.Passenger getScriptPassenger(int seat) {
        if (this.getScript() == null) {
            return null;
        } else {
            return seat >= 0 && seat < this.getScript().getPassengerCount() ? this.getScript().getPassenger(seat) : null;
        }
    }

    public int getMaxPassengers() {
        return this.passengers.length;
    }

    public boolean setPassenger(int seat, IsoGameCharacter chr, Vector3f offset) {
        if (seat >= 0 && seat < this.passengers.length) {
            if (seat == 0) {
                this.setNeedPartsUpdate(true);
            }

            this.passengers[seat].character = chr;
            this.passengers[seat].offset.set(offset);
            return true;
        } else {
            return false;
        }
    }

    public boolean clearPassenger(int seat) {
        if (seat >= 0 && seat < this.passengers.length) {
            this.passengers[seat].character = null;
            this.passengers[seat].offset.set(0.0F, 0.0F, 0.0F);
            return true;
        } else {
            return false;
        }
    }

    public boolean hasPassenger() {
        for (int i = 0; i < this.getMaxPassengers(); i++) {
            BaseVehicle.Passenger passenger = this.getPassenger(i);
            if (passenger != null && passenger.character != null) {
                return true;
            }
        }

        return false;
    }

    public BaseVehicle.Passenger getPassenger(int seat) {
        return seat >= 0 && seat < this.passengers.length ? this.passengers[seat] : null;
    }

    public IsoGameCharacter getCharacter(int seat) {
        BaseVehicle.Passenger passenger = this.getPassenger(seat);
        return passenger != null ? passenger.character : null;
    }

    public int getSeat(IsoGameCharacter chr) {
        for (int i = 0; i < this.getMaxPassengers(); i++) {
            if (this.getCharacter(i) == chr) {
                return i;
            }
        }

        return -1;
    }

    public boolean isDriver(IsoGameCharacter chr) {
        return this.getSeat(chr) == 0;
    }

    public Vector3f getWorldPos(Vector3f localPos, Vector3f worldPos, VehicleScript script) {
        return this.getWorldPos(localPos.x, localPos.y, localPos.z, worldPos, script);
    }

    public Vector3f getWorldPos(float localX, float localY, float localZ, Vector3f worldPos, VehicleScript script) {
        Transform xfrm = this.getWorldTransform(allocTransform());
        xfrm.origin.set(0.0F, 0.0F, 0.0F);
        worldPos.set(localX, localY, localZ);
        xfrm.transform(worldPos);
        releaseTransform(xfrm);
        float physX = this.jniTransform.origin.x + WorldSimulation.instance.offsetX;
        float physY = this.jniTransform.origin.z + WorldSimulation.instance.offsetY;
        float physZ = this.jniTransform.origin.y / 2.44949F;
        worldPos.set(physX + worldPos.x, physY + worldPos.z, physZ + worldPos.y);
        return worldPos;
    }

    public Vector3f getWorldPos(Vector3f localPos, Vector3f worldPos) {
        return this.getWorldPos(localPos.x, localPos.y, localPos.z, worldPos, this.getScript());
    }

    public Vector3f getWorldPos(float localX, float localY, float localZ, Vector3f worldPos) {
        return this.getWorldPos(localX, localY, localZ, worldPos, this.getScript());
    }

    public Vector3f getLocalPos(Vector3f worldPos, Vector3f localPos) {
        return this.getLocalPos(worldPos.x, worldPos.y, worldPos.z, localPos);
    }

    public Vector3f getLocalPos(float worldX, float worldY, float worldZ, Vector3f localPos) {
        Transform xfrm = this.getWorldTransform(allocTransform());
        xfrm.inverse();
        localPos.set(worldX - WorldSimulation.instance.offsetX, 0.0F, worldY - WorldSimulation.instance.offsetY);
        xfrm.transform(localPos);
        releaseTransform(xfrm);
        return localPos;
    }

    public Vector3f getPassengerLocalPos(int seat, Vector3f v) {
        BaseVehicle.Passenger passenger = this.getPassenger(seat);
        return passenger == null ? null : v.set(this.script.getModel().getOffset()).add(passenger.offset);
    }

    public Vector3f getPassengerWorldPos(int seat, Vector3f out) {
        BaseVehicle.Passenger passenger = this.getPassenger(seat);
        return passenger == null ? null : this.getPassengerPositionWorldPos(passenger.offset.x, passenger.offset.y, passenger.offset.z, out);
    }

    public Vector3f getPassengerPositionWorldPos(VehicleScript.Position posn, Vector3f out) {
        return this.getPassengerPositionWorldPos(posn.offset.x, posn.offset.y, posn.offset.z, out);
    }

    public Vector3f getPassengerPositionWorldPos(float x, float y, float z, Vector3f out) {
        out.set(this.script.getModel().offset);
        out.add(x, y, z);
        this.getWorldPos(out.x, out.y, out.z, out);
        out.z = PZMath.fastfloor(this.getZ());
        return out;
    }

    public VehicleScript.Anim getPassengerAnim(int seat, String id) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        } else {
            for (int i = 0; i < pngr.anims.size(); i++) {
                VehicleScript.Anim anim = pngr.anims.get(i);
                if (id.equals(anim.id)) {
                    return anim;
                }
            }

            return null;
        }
    }

    public VehicleScript.Position getPassengerPosition(int seat, String id) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? null : pngr.getPositionById(id);
    }

    public VehiclePart getPassengerDoor(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? null : this.getPartById(pngr.door);
    }

    public VehiclePart getPassengerDoor2(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? null : this.getPartById(pngr.door2);
    }

    public boolean isPositionOnLeftOrRight(float x, float y) {
        Vector3f v = TL_vector3f_pool.get().alloc();
        this.getLocalPos(x, y, 0.0F, v);
        x = v.x;
        TL_vector3f_pool.get().release(v);
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0F;
        float xMax = com.x + ext.x / 2.0F;
        return x < xMin * 0.98F || x > xMax * 0.98F;
    }

    /**
     * Check if one of the seat door is unlocked
     */
    public boolean haveOneDoorUnlocked() {
        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getDoor() != null
                && (part.getId().contains("Left") || part.getId().contains("Right"))
                && (!part.getDoor().isLocked() || part.getDoor().isOpen())) {
                return true;
            }
        }

        return false;
    }

    public String getPassengerArea(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? null : pngr.area;
    }

    public void playPassengerAnim(int seat, String animId) {
        IsoGameCharacter chr = this.getCharacter(seat);
        this.playPassengerAnim(seat, animId, chr);
    }

    public void playPassengerAnim(int seat, String animId, IsoGameCharacter chr) {
        if (chr != null) {
            VehicleScript.Anim anim = this.getPassengerAnim(seat, animId);
            if (anim != null) {
                this.playCharacterAnim(chr, anim, true);
            }
        }
    }

    public void playPassengerSound(int seat, String animId) {
        VehicleScript.Anim anim = this.getPassengerAnim(seat, animId);
        if (anim != null && anim.sound != null) {
            this.playSound(anim.sound);
        }
    }

    public void playPartAnim(VehiclePart part, String animId) {
        if (this.parts.contains(part)) {
            VehicleScript.Anim anim = part.getAnimById(animId);
            if (anim != null && !StringUtils.isNullOrWhitespace(anim.anim)) {
                BaseVehicle.ModelInfo modelInfo = this.getModelInfoForPart(part);
                if (modelInfo != null) {
                    AnimationPlayer animPlayer = modelInfo.getAnimationPlayer();
                    if (animPlayer != null && animPlayer.isReady()) {
                        if (animPlayer.getMultiTrack().getIndexOfTrack(modelInfo.track) != -1) {
                            animPlayer.getMultiTrack().removeTrack(modelInfo.track);
                        }

                        modelInfo.track = null;
                        SkinningData skinningData = animPlayer.getSkinningData();
                        if (skinningData == null || skinningData.animationClips.containsKey(anim.anim)) {
                            AnimationTrack track = animPlayer.play(anim.anim, anim.loop);
                            modelInfo.track = track;
                            if (track != null) {
                                track.setBlendWeight(1.0F);
                                track.setSpeedDelta(anim.rate);
                                track.isPlaying = anim.animate;
                                track.reverse = anim.reverse;
                                if (!modelInfo.modelScript.boneWeights.isEmpty()) {
                                    track.setBoneWeights(modelInfo.modelScript.boneWeights);
                                    track.initBoneWeights(skinningData);
                                }

                                if (part.getWindow() != null) {
                                    track.setCurrentTimeValue(track.getDuration() * part.getWindow().getOpenDelta());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void playActorAnim(VehiclePart part, String animId, IsoGameCharacter chr) {
        if (chr != null) {
            if (this.parts.contains(part)) {
                VehicleScript.Anim anim = part.getAnimById("Actor" + animId);
                if (anim != null) {
                    this.playCharacterAnim(chr, anim, !"EngineDoor".equals(part.getId()));
                }
            }
        }
    }

    private void playCharacterAnim(IsoGameCharacter chr, VehicleScript.Anim anim, boolean snapDirection) {
        chr.PlayAnimUnlooped(anim.anim);
        chr.getSpriteDef().setFrameSpeedPerFrame(anim.rate);
        chr.getLegsSprite().animate = true;
        Vector3f angle = this.getForwardVector(TL_vector3f_pool.get().alloc());
        if (anim.angle.lengthSquared() != 0.0F) {
            Matrix4f m4 = TL_matrix4f_pool.get().alloc();
            m4.rotationXYZ((float)Math.toRadians(anim.angle.x), (float)Math.toRadians(anim.angle.y), (float)Math.toRadians(anim.angle.z));
            Quaternionf q = allocQuaternionf();
            angle.rotate(m4.getNormalizedRotation(q));
            releaseQuaternionf(q);
            TL_matrix4f_pool.get().release(m4);
        }

        Vector2 vector2 = Vector2ObjectPool.get().alloc();
        vector2.set(angle.x, angle.z);
        chr.DirectionFromVector(vector2);
        Vector2ObjectPool.get().release(vector2);
        chr.setForwardDirection(angle.x, angle.z);
        if (chr.getAnimationPlayer() != null) {
            chr.getAnimationPlayer().setTargetAngle(chr.getDirectionAngleRadians());
            if (snapDirection) {
                chr.getAnimationPlayer().setAngleToTarget();
            }
        }

        TL_vector3f_pool.get().release(angle);
    }

    public void playPartSound(VehiclePart part, IsoPlayer player, String animId) {
        if (this.parts.contains(part)) {
            VehicleScript.Anim anim = part.getAnimById(animId);
            if (anim != null && anim.sound != null) {
                this.getEmitter().playSound(anim.sound, (IsoGameCharacter)player);
            }
        }
    }

    public void setCharacterPosition(IsoGameCharacter chr, int seat, String positionId) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr != null) {
            VehicleScript.Position position = pngr.getPositionById(positionId);
            if (position != null) {
                if (this.getCharacter(seat) == chr) {
                    this.passengers[seat].offset.set(position.offset);
                } else {
                    Vector3f worldPos = TL_vector3f_pool.get().alloc();
                    if (position.area == null) {
                        this.getPassengerPositionWorldPos(position, worldPos);
                    } else {
                        VehicleScript.Area area = this.script.getAreaById(position.area);
                        Vector2 vector2 = Vector2ObjectPool.get().alloc();
                        Vector2 areaPos = this.areaPositionWorld4PlayerInteract(area, vector2);
                        worldPos.x = areaPos.x;
                        worldPos.y = areaPos.y;
                        worldPos.z = PZMath.fastfloor(this.getZ());
                        Vector2ObjectPool.get().release(vector2);
                    }

                    chr.setX(worldPos.x);
                    chr.setY(worldPos.y);
                    chr.setZ(worldPos.z);
                    TL_vector3f_pool.get().release(worldPos);
                }

                if (chr instanceof IsoPlayer isoPlayer && isoPlayer.isLocalPlayer()) {
                    isoPlayer.dirtyRecalcGridStackTime = 10.0F;
                }
            }
        }
    }

    public void transmitCharacterPosition(int seat, String positionId) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.VehiclePassengerPosition, this, seat, positionId);
        }
    }

    public void setCharacterPositionToAnim(IsoGameCharacter chr, int seat, String animId) {
        VehicleScript.Anim anim = this.getPassengerAnim(seat, animId);
        if (anim != null) {
            if (this.getCharacter(seat) == chr) {
                this.passengers[seat].offset.set(anim.offset);
            } else {
                Vector3f worldPos = this.getWorldPos(anim.offset, TL_vector3f_pool.get().alloc());
                chr.setX(worldPos.x);
                chr.setY(worldPos.y);
                chr.setZ(0.0F);
                TL_vector3f_pool.get().release(worldPos);
            }
        }
    }

    public int getPassengerSwitchSeatCount(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? -1 : pngr.switchSeats.size();
    }

    public VehicleScript.Passenger.SwitchSeat getPassengerSwitchSeat(int seat, int index) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        } else {
            return index >= 0 && index < pngr.switchSeats.size() ? pngr.switchSeats.get(index) : null;
        }
    }

    private VehicleScript.Passenger.SwitchSeat getSwitchSeat(int seatFrom, int seatTo) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seatFrom);
        if (pngr == null) {
            return null;
        } else {
            for (int i = 0; i < pngr.switchSeats.size(); i++) {
                VehicleScript.Passenger.SwitchSeat switchSeat = pngr.switchSeats.get(i);
                if (switchSeat.seat == seatTo
                    && this.getPartForSeatContainer(seatTo) != null
                    && this.getPartForSeatContainer(seatTo).getInventoryItem() != null) {
                    return switchSeat;
                }
            }

            return null;
        }
    }

    public String getSwitchSeatAnimName(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        return switchSeat == null ? null : switchSeat.anim;
    }

    public float getSwitchSeatAnimRate(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        return switchSeat == null ? 0.0F : switchSeat.rate;
    }

    public String getSwitchSeatSound(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        return switchSeat == null ? null : switchSeat.sound;
    }

    public boolean canSwitchSeat(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        return switchSeat != null;
    }

    public void switchSeat(IsoGameCharacter chr, int seatTo) {
        int seatFrom = this.getSeat(chr);
        if (seatFrom != -1) {
            this.clearPassenger(seatFrom);
            VehicleScript.Position posInside = this.getPassengerPosition(seatTo, "inside");
            if (posInside == null) {
                Vector3f v = TL_vector3f_pool.get().alloc();
                v.set(0.0F, 0.0F, 0.0F);
                this.setPassenger(seatTo, chr, v);
                TL_vector3f_pool.get().release(v);
            } else {
                this.setPassenger(seatTo, chr, posInside.offset);
            }
        }
    }

    public void playSwitchSeatAnim(int seatFrom, int seatTo) {
        IsoGameCharacter chr = this.getCharacter(seatFrom);
        if (chr != null) {
            VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
            if (switchSeat != null) {
                chr.PlayAnimUnlooped(switchSeat.anim);
                chr.getSpriteDef().setFrameSpeedPerFrame(switchSeat.rate);
                chr.getLegsSprite().animate = true;
            }
        }
    }

    public boolean isSeatOccupied(int seat) {
        return this.isSeatHoldingItems(seat) || this.getCharacter(seat) != null;
    }

    public boolean isSeatInstalled(int seat) {
        VehiclePart part = this.getPartForSeatContainer(seat);
        return part != null && part.getInventoryItem() != null;
    }

    public boolean isSeatHoldingItems(int seat) {
        return this.isSeatHoldingItems(this.getPartForSeatContainer(seat));
    }

    public boolean isSeatHoldingItems(VehiclePart seat) {
        return seat != null && seat.getItemContainer() != null && !seat.getItemContainer().isEmpty()
            ? seat.getItemContainer().getContentsWeight() * 4.0F > seat.getItemContainer().getCapacity()
            : false;
    }

    public ArrayList<VehiclePart> getAllSeatParts() {
        return this.getAllSeatParts(new ArrayList<>(this.passengers.length));
    }

    public ArrayList<VehiclePart> getAllSeatParts(ArrayList<VehiclePart> results) {
        results.clear();
        results.ensureCapacity(this.passengers.length);

        for (int i = 0; i < this.passengers.length; i++) {
            results.add(null);
        }

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            int seatNumber = part.getContainerSeatNumber();
            if (seatNumber >= 0) {
                results.set(seatNumber, part);
            }
        }

        return results;
    }

    public boolean isPointLeftOfCenter(float x, float y) {
        Vector3f forward = this.getForwardVector(allocVector3f());
        boolean bLeft = PZMath.isLeft(this.getX(), this.getY(), this.getX() + forward.x, this.getY() + forward.z, x, y) < 0.0F;
        releaseVector3f(forward);
        return bLeft;
    }

    public int getBestSeat(IsoGameCharacter chr) {
        return -1;
    }

    public float getEnterSeatDistance(int seat, float x, float y) {
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
        if (posOutside != null) {
            Vector3f pos = this.getPassengerPositionWorldPos(posOutside, allocVector3f());
            float distSq = IsoUtils.DistanceToSquared(x, y, pos.x, pos.y);
            releaseVector3f(pos);
            return distSq;
        } else {
            posOutside = this.getPassengerPosition(seat, "outside2");
            if (posOutside != null) {
                Vector3f pos = this.getPassengerPositionWorldPos(posOutside, allocVector3f());
                float distSq = IsoUtils.DistanceToSquared(x, y, pos.x, pos.y);
                releaseVector3f(pos);
                return distSq;
            } else {
                return -1.0F;
            }
        }
    }

    public void updateHasExtendOffsetForExit(IsoGameCharacter chr) {
        this.hasExtendOffsetExiting = true;
        this.updateHasExtendOffset(chr);
        this.getPoly();
    }

    public void updateHasExtendOffsetForExitEnd(IsoGameCharacter chr) {
        this.hasExtendOffsetExiting = false;
        this.updateHasExtendOffset(chr);
        this.getPoly();
    }

    public void updateHasExtendOffset(IsoGameCharacter chr) {
        this.hasExtendOffset = false;
        this.hasExtendOffsetExiting = false;
    }

    public VehiclePart getUseablePart(IsoGameCharacter chr) {
        return this.getUseablePart(chr, true);
    }

    public VehiclePart getUseablePart(IsoGameCharacter chr, boolean checkDir) {
        if (chr.getVehicle() != null) {
            return null;
        } else if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(chr.getZ())) {
            return null;
        } else if (chr.DistTo(this) > 6.0F) {
            return null;
        } else {
            VehicleScript script = this.getScript();
            if (script == null) {
                return null;
            } else {
                Vector3f ext = script.getExtents();
                Vector3f com = script.getCenterOfMassOffset();
                float minY = com.z - ext.z / 2.0F;
                float maxY = com.z + ext.z / 2.0F;
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                Vector3f vecTest = TL_vector3f_pool.get().alloc();

                for (int i = 0; i < this.parts.size(); i++) {
                    VehiclePart part = this.parts.get(i);
                    if (part.getArea() != null && this.isInArea(part.getArea(), chr)) {
                        String func = part.getLuaFunction("use");
                        if (func != null && !func.equals("")) {
                            VehicleScript.Area area = script.getAreaById(part.getArea());
                            if (area != null) {
                                Vector2 center = this.areaPositionLocal(area, vector2);
                                if (center != null) {
                                    float testX = 0.0F;
                                    float testY = 0.0F;
                                    float testZ = 0.0F;
                                    if (!(center.y >= maxY) && !(center.y <= minY)) {
                                        testZ = center.y;
                                    } else {
                                        testX = center.x;
                                    }

                                    if (!checkDir) {
                                        return part;
                                    }

                                    this.getWorldPos(testX, 0.0F, testZ, vecTest);
                                    vector2.set(vecTest.x - chr.getX(), vecTest.y - chr.getY());
                                    vector2.normalize();
                                    float dot = vector2.dot(chr.getForwardDirection());
                                    if (dot > 0.5F
                                        && !PolygonalMap2.instance
                                            .lineClearCollide(chr.getX(), chr.getY(), vecTest.x, vecTest.y, PZMath.fastfloor(chr.getZ()), this, false, true)) {
                                        Vector2ObjectPool.get().release(vector2);
                                        TL_vector3f_pool.get().release(vecTest);
                                        return part;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                Vector2ObjectPool.get().release(vector2);
                TL_vector3f_pool.get().release(vecTest);
                return null;
            }
        }
    }

    public float distanceToManhatten(float x, float y) {
        return IsoUtils.DistanceManhatten(this.getX(), this.getY(), x, y);
    }

    public VehiclePart getClosestWindow(IsoGameCharacter chr) {
        if (chr == null) {
            return null;
        } else {
            float chrX = chr.getX();
            float chrY = chr.getY();
            float chrZ = chr.getZ();
            float forwardDirectionX = chr.getForwardDirectionX();
            float forwardDirectionY = chr.getForwardDirectionY();
            return this.getClosestWindow(chrX, chrY, chrZ, forwardDirectionX, forwardDirectionY);
        }
    }

    private @Nullable VehiclePart getClosestWindow(float chrX, float chrY, float chrZ, float forwardDirectionX, float forwardDirectionY) {
        if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(chrZ)) {
            return null;
        } else if (this.distanceToManhatten(chrX, chrY) > 5.0F) {
            return null;
        } else {
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            float minY = com.z - ext.z / 2.0F;
            float maxY = com.z + ext.z / 2.0F;
            Vector2 vecTo = Vector2ObjectPool.get().alloc();
            Vector3f vecTest = TL_vector3f_pool.get().alloc();

            for (int i = 0; i < this.parts.size(); i++) {
                VehiclePart part = this.parts.get(i);
                if (part.getWindow() != null && part.getArea() != null && this.isInArea(part.getArea(), chrX, chrY)) {
                    VehicleScript.Area area = this.script.getAreaById(part.getArea());
                    if (area != null) {
                        if (!(area.y >= maxY) && !(area.y <= minY)) {
                            vecTest.set(0.0F, 0.0F, area.y);
                        } else {
                            vecTest.set(area.x, 0.0F, 0.0F);
                        }

                        this.getWorldPos(vecTest, vecTest);
                        vecTo.set(vecTest.x - chrX, vecTest.y - chrY);
                        vecTo.normalize();
                        float dot = vecTo.dot(forwardDirectionX, forwardDirectionY);
                        if (dot > 0.5F) {
                            Vector2ObjectPool.get().release(vecTo);
                            TL_vector3f_pool.get().release(vecTest);
                            return part;
                        }
                        break;
                    }
                }
            }

            Vector2ObjectPool.get().release(vecTo);
            TL_vector3f_pool.get().release(vecTest);
            return null;
        }
    }

    public Vector2 getFacingPosition(IsoGameCharacter chr, Vector2 out) {
        return this.getFacingPosition(chr.getX(), chr.getY(), chr.getZ(), out);
    }

    private Vector2 getFacingPosition(float in_worldX, float in_worldY, float in_worldZ, Vector2 out_worldFacingPos) {
        Vector3f chrPos = this.getLocalPos(in_worldX, in_worldY, in_worldZ, TL_vector3f_pool.get().alloc());
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0F;
        float xMax = com.x + ext.x / 2.0F;
        float yMin = com.z - ext.z / 2.0F;
        float yMax = com.z + ext.z / 2.0F;
        float localX = 0.0F;
        float localY = 0.0F;
        if (chrPos.x <= 0.0F && chrPos.z >= yMin && chrPos.z <= yMax) {
            localY = chrPos.z;
        } else if (chrPos.x > 0.0F && chrPos.z >= yMin && chrPos.z <= yMax) {
            localY = chrPos.z;
        } else if (chrPos.z <= 0.0F && chrPos.x >= xMin && chrPos.x <= xMax) {
            localX = chrPos.x;
        } else if (chrPos.z > 0.0F && chrPos.x >= xMin && chrPos.x <= xMax) {
            localX = chrPos.x;
        }

        this.getWorldPos(localX, 0.0F, localY, chrPos);
        out_worldFacingPos.set(chrPos.x, chrPos.y);
        TL_vector3f_pool.get().release(chrPos);
        return out_worldFacingPos;
    }

    public boolean enter(int seat, IsoGameCharacter chr, Vector3f offset) {
        if (!GameClient.client) {
            VehiclesDB2.instance.updateVehicleAndTrailer(this);
        }

        if (chr == null) {
            return false;
        } else if (chr.getVehicle() != null && !chr.getVehicle().exit(chr)) {
            return false;
        } else if (this.setPassenger(seat, chr, offset)) {
            chr.setVehicle(this);
            chr.setCollidable(false);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.VehicleEnter, this, chr, seat);
            }

            if (chr instanceof IsoPlayer isoPlayer && isoPlayer.isLocalPlayer()) {
                isoPlayer.dirtyRecalcGridStackTime = 10.0F;
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean enter(int seat, IsoGameCharacter chr) {
        if (this.getPartForSeatContainer(seat) != null && this.getPartForSeatContainer(seat).getInventoryItem() != null) {
            VehicleScript.Position position = this.getPassengerPosition(seat, "outside");
            return position != null ? this.enter(seat, chr, position.offset) : false;
        } else {
            return false;
        }
    }

    public boolean enterRSync(int seat, IsoGameCharacter chr, BaseVehicle v) {
        if (chr == null) {
            return false;
        } else {
            VehicleScript.Position position = this.getPassengerPosition(seat, "inside");
            if (position != null) {
                if (this.setPassenger(seat, chr, position.offset)) {
                    chr.setVehicle(v);
                    chr.setCollidable(false);
                    if (GameClient.client && chr instanceof IsoPlayer player && player.isLocalPlayer()) {
                        LuaEventManager.triggerEvent("OnEnterVehicle", player);
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public boolean exit(IsoGameCharacter chr) {
        if (!GameClient.client) {
            VehiclesDB2.instance.updateVehicleAndTrailer(this);
        }

        if (chr == null) {
            return false;
        } else {
            int seat = this.getSeat(chr);
            if (seat == -1) {
                return false;
            } else if (this.clearPassenger(seat)) {
                chr.setVehicle(null);
                chr.savedVehicleSeat = -1;
                chr.setCollidable(true);
                if (GameClient.client) {
                    INetworkPacket.send(PacketTypes.PacketType.VehicleExit, this, chr, seat);
                }

                if (this.getDriver() == null && this.soundHornOn) {
                    this.onHornStop();
                }

                this.polyGarageCheck = true;
                this.polyDirty = true;
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean exitRSync(IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        } else {
            int seat = this.getSeat(chr);
            if (seat == -1) {
                return false;
            } else if (this.clearPassenger(seat)) {
                chr.setVehicle(null);
                chr.setCollidable(true);
                if (GameClient.client) {
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public boolean hasRoof(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? false : pngr.hasRoof;
    }

    public boolean showPassenger(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        return pngr == null ? false : pngr.showPassenger;
    }

    public boolean showPassenger(IsoGameCharacter chr) {
        int seat = this.getSeat(chr);
        return this.showPassenger(seat);
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        float origX = this.getX();
        float origY = this.getY();
        if (this.square != null) {
            float d = 5.0E-4F;
            this.setX(PZMath.clamp(this.getX(), this.square.x + 5.0E-4F, this.square.x + 1.0F - 5.0E-4F));
            this.setY(PZMath.clamp(this.getY(), this.square.y + 5.0E-4F, this.square.y + 1.0F - 5.0E-4F));
        }

        super.save(output, IS_DEBUG_SAVE);
        this.setX(origX);
        this.setY(origY);
        Quaternionf q = this.savedRot;
        Transform xfrm = this.getWorldTransform(allocTransform());
        output.putFloat(xfrm.origin.y);
        xfrm.getRotation(q);
        releaseTransform(xfrm);
        output.putFloat(q.x);
        output.putFloat(q.y);
        output.putFloat(q.z);
        output.putFloat(q.w);
        GameWindow.WriteStringUTF(output, this.scriptName);
        output.putInt(this.skinIndex);
        output.put((byte)(this.isEngineRunning() ? 1 : 0));
        output.putInt(this.frontEndDurability);
        output.putInt(this.rearEndDurability);
        output.putInt(this.currentFrontEndDurability);
        output.putInt(this.currentRearEndDurability);
        output.putInt(this.engineLoudness);
        output.putInt(this.engineQuality);
        output.putInt(this.keyId);
        output.put(this.keySpawned);
        output.put((byte)(this.headlightsOn ? 1 : 0));
        output.put((byte)(this.created ? 1 : 0));
        output.put((byte)(this.soundHornOn ? 1 : 0));
        output.put((byte)(this.soundBackMoveOn ? 1 : 0));
        output.put((byte)this.lightbarLightsMode.get());
        output.put((byte)this.lightbarSirenMode.get());
        output.putShort((short)this.parts.size());

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            part.save(output);
        }

        output.put((byte)(this.keyIsOnDoor ? 1 : 0));
        output.put((byte)(this.hotwired ? 1 : 0));
        output.put((byte)(this.hotwiredBroken ? 1 : 0));
        output.put((byte)(this.keysInIgnition ? 1 : 0));
        output.putFloat(this.rust);
        output.putFloat(this.colorHue);
        output.putFloat(this.colorSaturation);
        output.putFloat(this.colorValue);
        output.putInt(this.enginePower);
        output.putShort(this.vehicleId);
        output.putInt(this.mechanicalId);
        output.put((byte)(this.alarmed ? 1 : 0));
        output.putDouble(this.alarmStartTime);
        GameWindow.WriteString(output, this.chosenAlarmSound);
        output.putDouble(this.sirenStartTime);
        if (this.getCurrentKey() != null) {
            output.put((byte)1);
            this.getCurrentKey().saveWithSize(output, false);
        } else {
            output.put((byte)0);
        }

        output.put((byte)this.bloodIntensity.size());

        for (Entry<String, Byte> entry : this.bloodIntensity.entrySet()) {
            GameWindow.WriteStringUTF(output, entry.getKey());
            output.put(entry.getValue());
        }

        if (this.vehicleTowingId != -1) {
            output.put((byte)1);
            output.putInt(this.vehicleTowingId);
            GameWindow.WriteStringUTF(output, this.towAttachmentSelf);
            GameWindow.WriteStringUTF(output, this.towAttachmentOther);
            output.putFloat(this.rowConstraintZOffset);
        } else {
            output.put((byte)0);
        }

        output.putFloat(this.getRegulatorSpeed());
        output.put((byte)(this.previouslyEntered ? 1 : 0));
        output.put((byte)(this.previouslyMoved ? 1 : 0));
        int pos = output.position();
        output.putInt(0);
        int posStart = output.position();
        if (this.animals.isEmpty()) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            output.putInt(this.animals.size());

            for (int i = 0; i < this.animals.size(); i++) {
                this.animals.get(i).save(output, IS_DEBUG_SAVE, false);
            }
        }

        int posEnd = output.position();
        output.position(pos);
        output.putInt(posEnd - posStart);
        output.position(posEnd);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        float physicsZ = input.getFloat();
        int level = PZMath.fastfloor(this.getZ());
        float F = 2.44949F;
        this.savedPhysicsZ = PZMath.clamp(physicsZ, level * 2.44949F, (level + 0.995F) * 2.44949F);
        float x = input.getFloat();
        float y = input.getFloat();
        float z = input.getFloat();
        float w = input.getFloat();
        this.savedRot.set(x, y, z, w);
        this.jniTransform
            .origin
            .set(
                this.getX() - WorldSimulation.instance.offsetX,
                Float.isNaN(this.savedPhysicsZ) ? this.getZ() : this.savedPhysicsZ,
                this.getY() - WorldSimulation.instance.offsetY
            );
        this.jniTransform.setRotation(this.savedRot);
        this.scriptName = GameWindow.ReadStringUTF(input);
        this.skinIndex = input.getInt();
        boolean isEngineRunning = input.get() == 1;
        if (isEngineRunning) {
            this.engineState = BaseVehicle.engineStateTypes.Running;
        }

        this.frontEndDurability = input.getInt();
        this.rearEndDurability = input.getInt();
        this.currentFrontEndDurability = input.getInt();
        this.currentRearEndDurability = input.getInt();
        this.engineLoudness = input.getInt();
        this.engineQuality = input.getInt();
        this.engineQuality = PZMath.clamp(this.engineQuality, 0, 100);
        this.keyId = input.getInt();
        this.keySpawned = input.get();
        this.headlightsOn = input.get() == 1;
        this.created = input.get() == 1;
        this.soundHornOn = input.get() == 1;
        this.soundBackMoveOn = input.get() == 1;
        this.lightbarLightsMode.set(input.get());
        this.lightbarSirenMode.set(input.get());
        short partCount = input.getShort();

        for (int i = 0; i < partCount; i++) {
            VehiclePart part = new VehiclePart(this);
            part.load(input, WorldVersion);
            this.parts.add(part);
        }

        this.keyIsOnDoor = input.get() == 1;
        this.hotwired = input.get() == 1;
        this.hotwiredBroken = input.get() == 1;
        this.keysInIgnition = input.get() == 1;
        this.rust = input.getFloat();
        this.colorHue = input.getFloat();
        this.colorSaturation = input.getFloat();
        this.colorValue = input.getFloat();
        this.enginePower = input.getInt();
        short VehicleID = input.getShort();
        if (WorldVersion < 229) {
            String var21 = GameWindow.ReadString(input);
        }

        this.mechanicalId = input.getInt();
        this.alarmed = input.get() == 1;
        if (WorldVersion >= 229) {
            this.alarmStartTime = input.getDouble();
            this.chosenAlarmSound = StringUtils.discardNullOrWhitespace(GameWindow.ReadString(input));
        }

        this.sirenStartTime = input.getDouble();
        if (input.get() == 1) {
            InventoryItem key = null;

            try {
                key = InventoryItem.loadItem(input, WorldVersion);
            } catch (Exception var19) {
                var19.printStackTrace();
            }

            if (key != null) {
                this.setCurrentKey(key);
            }
        }

        int count = input.get();

        for (int i = 0; i < count; i++) {
            String id = GameWindow.ReadStringUTF(input);
            byte intensity = input.get();
            this.bloodIntensity.put(id, intensity);
        }

        if (input.get() == 1) {
            this.vehicleTowingId = input.getInt();
            this.towAttachmentSelf = GameWindow.ReadStringUTF(input);
            this.towAttachmentOther = GameWindow.ReadStringUTF(input);
            this.rowConstraintZOffset = input.getFloat();
        }

        this.setRegulatorSpeed(input.getFloat());
        this.previouslyEntered = input.get() == 1;
        if (WorldVersion >= 196) {
            this.previouslyMoved = input.get() == 1;
        }

        if (WorldVersion >= 212) {
            int bufferSize = input.getInt();
            if (GameClient.client) {
                input.position(input.position() + bufferSize);
            } else if (input.get() == 1) {
                int size = input.getInt();

                for (int i = 0; i < size; i++) {
                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                    animal.load(input, WorldVersion, IS_DEBUG_SAVE);
                    this.addAnimalInTrailer(animal);
                }
            }
        } else if (input.get() == 1) {
            int size = input.getInt();

            for (int i = 0; i < size; i++) {
                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                animal.load(input, WorldVersion, IS_DEBUG_SAVE);
                this.addAnimalInTrailer(animal);
            }
        }

        this.loaded = true;
    }

    @Override
    public void softReset() {
        this.keySpawned = 0;
        this.keyIsOnDoor = false;
        this.keysInIgnition = false;
        this.ignitionSwitch.removeAllItems();
        this.currentKey = null;
        this.previouslyEntered = false;
        this.previouslyMoved = false;
        this.engineState = BaseVehicle.engineStateTypes.Idle;
        this.randomizeContainers();
    }

    public void trySpawnKey() {
        this.trySpawnKey(false);
    }

    public void trySpawnKey(boolean crashed) {
        if (!GameClient.client) {
            if (this.script != null && !this.script.neverSpawnKey()) {
                if (this.keySpawned != 1) {
                    if (SandboxOptions.getInstance().vehicleEasyUse.getValue()) {
                        this.addKeyToGloveBox();
                    } else if (!this.isPreviouslyEntered() && !this.isPreviouslyMoved() && !this.isHotwired() && !this.isBurnt()) {
                        VehicleType type = VehicleType.getTypeFromName(this.getVehicleType());
                        int chance = type == null ? 70 : type.getChanceToSpawnKey();
                        if (Rand.Next(100) <= chance) {
                            this.addKeyToWorld(crashed);
                        }
                    }
                }
            }
        }
    }

    public boolean shouldCollideWithCharacters() {
        if (this.vehicleTowedBy != null) {
            return this.vehicleTowedBy.shouldCollideWithCharacters();
        } else {
            float speed = this.getSpeed2D();
            return this.isEngineRunning() ? speed > 0.05F : speed > 1.0F;
        }
    }

    public boolean shouldCollideWithObjects() {
        return this.vehicleTowedBy != null ? this.vehicleTowedBy.shouldCollideWithObjects() : this.isEngineRunning();
    }

    public void breakingObjects() {
        boolean bCollideWithCharacters = this.shouldCollideWithCharacters();
        boolean bCollideWithObjects = this.shouldCollideWithObjects();
        if (bCollideWithCharacters || bCollideWithObjects) {
            Vector3f ext = this.script.getExtents();
            Vector2 vector2 = Vector2ObjectPool.get().alloc();
            float radius = Math.max(ext.x / 2.0F, ext.z / 2.0F) + 0.3F + 1.0F;
            int radius_sq = (int)Math.ceil(radius);

            for (int yy = -radius_sq; yy < radius_sq; yy++) {
                for (int xx = -radius_sq; xx < radius_sq; xx++) {
                    IsoGridSquare sq = this.getCell().getGridSquare((double)(this.getX() + xx), (double)(this.getY() + yy), (double)this.getZ());
                    if (sq != null) {
                        if (bCollideWithObjects) {
                            for (int i = 0; i < sq.getObjects().size(); i++) {
                                IsoObject object = sq.getObjects().get(i);
                                if (!(object instanceof IsoWorldInventoryObject)) {
                                    Vector2 collision = null;
                                    if (!this.breakingObjectsList.contains(object) && object != null && object.getProperties() != null) {
                                        if (object.getProperties().has("CarSlowFactor")) {
                                            collision = this.testCollisionWithObject(object, 0.3F, vector2);
                                        }

                                        if (collision != null) {
                                            this.breakingObjectsList.add(object);
                                            if (!GameClient.client) {
                                                object.Collision(collision, this);
                                            }
                                        }

                                        if (object.getProperties().has("HitByCar")) {
                                            collision = this.testCollisionWithObject(object, 0.3F, vector2);
                                        }

                                        if (collision != null && !GameClient.client) {
                                            object.Collision(collision, this);
                                        }

                                        this.checkCollisionWithPlant(sq, object, vector2);
                                    }
                                }
                            }
                        }

                        if (bCollideWithCharacters) {
                            for (int ix = 0; ix < sq.getMovingObjects().size(); ix++) {
                                IsoMovingObject object = sq.getMovingObjects().get(ix);
                                if (object instanceof IsoZombie zombie) {
                                    if (zombie.isProne()) {
                                        this.testCollisionWithProneCharacter(zombie, false);
                                    }

                                    zombie.setVehicle4TestCollision(this);
                                }

                                if (object instanceof IsoAnimal animal) {
                                    animal.setVehicle4TestCollision(this);
                                }

                                if (object instanceof IsoPlayer player && object != this.getDriver()) {
                                    player.setVehicle4TestCollision(this);
                                }
                            }
                        }

                        if (bCollideWithObjects) {
                            for (int ix = 0; ix < sq.getStaticMovingObjects().size(); ix++) {
                                IsoMovingObject objectx = sq.getStaticMovingObjects().get(ix);
                                if (objectx instanceof IsoDeadBody body) {
                                    int var13 = this.testCollisionWithCorpse(body, true);
                                }
                            }
                        }
                    }
                }
            }

            float slowFactor = -999.0F;

            for (int ixx = 0; ixx < this.breakingObjectsList.size(); ixx++) {
                IsoObject objectx = this.breakingObjectsList.get(ixx);
                Vector2 collision = this.testCollisionWithObject(objectx, 1.0F, vector2);
                if (collision == null || !objectx.getSquare().getObjects().contains(objectx)) {
                    this.breakingObjectsList.remove(objectx);
                    objectx.UnCollision(this);
                } else if (slowFactor < objectx.GetVehicleSlowFactor(this)) {
                    slowFactor = objectx.GetVehicleSlowFactor(this);
                }
            }

            if (slowFactor != -999.0F) {
                this.breakingSlowFactor = PZMath.clamp(slowFactor, 0.0F, 34.0F);
            } else {
                this.breakingSlowFactor = 0.0F;
            }

            Vector2ObjectPool.get().release(vector2);
        }
    }

    private void updateVelocityMultiplier() {
        if (this.physics != null && this.getScript() != null) {
            Vector3f velocity = this.getLinearVelocity(TL_vector3f_pool.get().alloc());
            velocity.y = 0.0F;
            float speed = velocity.length();
            float maxSpeed = 100000.0F;
            float multiplier = 1.0F;
            if (this.getScript().getWheelCount() > 0) {
                if (speed > 0.0F && speed > 34.0F - this.breakingSlowFactor) {
                    maxSpeed = 34.0F - this.breakingSlowFactor;
                    multiplier = (34.0F - this.breakingSlowFactor) / speed;
                }
            } else if (this.getVehicleTowedBy() == null) {
                maxSpeed = 0.0F;
                multiplier = 0.1F;
            }

            Bullet.setVehicleVelocityMultiplier(this.vehicleId, maxSpeed, multiplier);
            TL_vector3f_pool.get().release(velocity);
        }
    }

    private void playScrapePastPlantSound(IsoGridSquare sq) {
        if (this.emitter != null && !this.emitter.isPlaying(this.soundScrapePastPlant)) {
            this.emitter.setPos(sq.x + 0.5F, sq.y + 0.5F, sq.z);
            this.soundScrapePastPlant = this.emitter.playSoundImpl("VehicleScrapePastPlant", sq);
        }

        this.hittingPlant = true;
    }

    private void checkCollisionWithPlant(IsoGridSquare sq, IsoObject object, Vector2 vector2) {
        if (object.sprite != null) {
            IsoTree tree = Type.tryCastTo(object, IsoTree.class);
            String tilesetName = object.sprite.tilesetName;
            boolean bPlantExcludingGrass = "d_generic_1".equalsIgnoreCase(tilesetName) || "d_plants_1".equalsIgnoreCase(tilesetName);
            if (tree != null || object.isBush() || bPlantExcludingGrass) {
                float currentSpeed = this.getCurrentAbsoluteSpeedKmHour();
                if (!(currentSpeed <= 1.0F)) {
                    Vector2 collision = this.testCollisionWithObject(object, 0.3F, vector2);
                    if (collision != null) {
                        if (tree != null && tree.getSize() == 1) {
                            this.ApplyImpulse4Break(object, 0.025F);
                            this.playScrapePastPlantSound(sq);
                        } else {
                            if (object.isBush() && this.soundScrapePastPlant == -1L && this.emitter != null && !this.emitter.isPlaying("VehicleHitHedge")) {
                                this.emitter.playSoundImpl("VehicleHitHedge", (IsoObject)null);
                            }

                            if (this.isPositionOnLeftOrRight(collision.x, collision.y)) {
                                if (!bPlantExcludingGrass) {
                                    this.ApplyImpulse4Break(object, 0.025F);
                                }

                                this.playScrapePastPlantSound(sq);
                            } else if (currentSpeed < 10.0F) {
                                if (!bPlantExcludingGrass) {
                                    this.ApplyImpulse4Break(object, 0.025F);
                                }

                                this.playScrapePastPlantSound(sq);
                            } else {
                                if (!bPlantExcludingGrass) {
                                    this.ApplyImpulse4Break(object, 0.1F);
                                }

                                this.playScrapePastPlantSound(sq);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateScrapPastPlantSound() {
        if (this.soundScrapePastPlant != -1L) {
            if ((!this.isEngineRunning() || !this.hittingPlant) && this.emitter != null) {
                this.emitter.stopOrTriggerSound(this.soundScrapePastPlant);
                this.soundScrapePastPlant = -1L;
            }
        }
    }

    public void damageObjects(float damage) {
        if (this.isEngineRunning()) {
            Vector3f ext = this.script.getExtents();
            Vector2 vector2 = Vector2ObjectPool.get().alloc();
            float radius = Math.max(ext.x / 2.0F, ext.z / 2.0F) + 0.3F + 1.0F;
            int radius_sq = (int)Math.ceil(radius);

            for (int yy = -radius_sq; yy < radius_sq; yy++) {
                for (int xx = -radius_sq; xx < radius_sq; xx++) {
                    IsoGridSquare sq = this.getCell().getGridSquare((double)(this.getX() + xx), (double)(this.getY() + yy), (double)this.getZ());
                    if (sq != null) {
                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoObject object = sq.getObjects().get(i);
                            Vector2 collision = null;
                            if (object instanceof IsoTree) {
                                collision = this.testCollisionWithObject(object, 2.0F, vector2);
                                if (collision != null) {
                                    object.setRenderEffect(RenderEffectType.Hit_Tree_Shudder);
                                }
                            }

                            if (collision == null && object instanceof IsoWindow) {
                                collision = this.testCollisionWithObject(object, 1.0F, vector2);
                            }

                            if (collision == null
                                && object.sprite != null
                                && (object.sprite.getProperties().has("HitByCar") || object.sprite.getProperties().has("CarSlowFactor"))) {
                                collision = this.testCollisionWithObject(object, 1.0F, vector2);
                            }

                            if (collision == null) {
                                IsoGridSquare sq2 = this.getCell().getGridSquare((double)(this.getX() + xx), (double)(this.getY() + yy), 1.0);
                                if (sq2 != null && sq2.has(IsoObjectType.lightswitch)) {
                                    collision = this.testCollisionWithObject(object, 1.0F, vector2);
                                }
                            }

                            if (collision == null) {
                                IsoGridSquare sq2 = this.getCell().getGridSquare((double)(this.getX() + xx), (double)(this.getY() + yy), 0.0);
                                if (sq2 != null && sq2.has(IsoObjectType.lightswitch)) {
                                    collision = this.testCollisionWithObject(object, 1.0F, vector2);
                                }
                            }

                            if (collision != null) {
                                object.Hit(collision, this, damage);
                            }
                        }
                    }
                }
            }

            ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();

            for (int i = 0; i < vehicles.size(); i++) {
                BaseVehicle vehicle = vehicles.get(i);
                if (vehicle != this && this.testCollisionWithVehicle(vehicle)) {
                    vehicle.lastDamagedBy = this.getDriverRegardlessOfTow();
                }
            }

            Vector2ObjectPool.get().release(vector2);
        }
    }

    @Override
    public void update() {
        if (!this.removedFromWorld) {
            if (!this.getCell().vehicles.contains(this)) {
                this.getCell().getRemoveList().add(this);
            } else {
                if (this.chunk != null) {
                    if (!this.chunk.vehicles.contains(this)) {
                        if (GameClient.client) {
                            VehicleManager.instance.sendVehicleRequest(this.vehicleId, (short)2);
                        }
                    } else if (!GameServer.server && this.chunk.refs.isEmpty()) {
                        this.removeFromWorld();
                        return;
                    }
                }

                super.update();
                if (this.timeSinceLastAuth > 0.0F) {
                    this.timeSinceLastAuth--;
                }

                if (!GameClient.client) {
                    for (int i = this.getAnimals().size() - 1; i >= 0; i--) {
                        IsoAnimal animal = this.getAnimals().get(i);
                        animal.setX(this.getX());
                        animal.setY(this.getY());
                        animal.setZ(PZMath.fastfloor(this.getZ()));
                        animal.update();
                        if (this.getAnimals().contains(animal)) {
                            if (!animal.isDead()) {
                                animal.updateVocalProperties();
                            }

                            this.setNeedPartsUpdate(true);
                            if (GameServer.server && this.updateAnimal.Check()) {
                                animal.networkAi.setAnimalPacket(null);
                                AnimalSynchronizationManager.getInstance().setSendToClients(animal.onlineId);
                            }
                        }
                    }
                } else {
                    for (int ix = 0; ix < this.getAnimals().size(); ix++) {
                        IsoAnimal animal = this.getAnimals().get(ix);
                        animal.setX(this.getX());
                        animal.setY(this.getY());
                        animal.setZ(PZMath.fastfloor(this.getZ()));
                        AnimalInstanceManager.getInstance().update(animal);
                    }
                }

                if (GameClient.client || GameServer.server) {
                    this.isReliable = this.physicReliableLimit.Check();
                }

                if (GameClient.client && this.hasAuthorization(GameClient.connection)) {
                    this.updatePhysicsNetwork();
                }

                if (this.getVehicleTowing() != null && this.getDriver() != null) {
                    float baseMassInc = 2.5F;
                    if (this.getVehicleTowing().getPartCount() == 0) {
                        baseMassInc = 12.0F;
                    }

                    if (this.getVehicleTowing().scriptName.equals("Base.Trailer")) {
                        VehiclePart trailer = this.getVehicleTowing().getPartById("TrailerTrunk");
                        if (this.getCurrentSpeedKmHour() > 30.0F && trailer.getCondition() < 50.0F && !trailer.container.items.isEmpty()) {
                            ArrayList<InventoryItem> heavyItems = new ArrayList<>();

                            for (int ix = 0; ix < trailer.container.items.size(); ix++) {
                                if (trailer.container.items.get(ix).getWeight() >= 3.5F) {
                                    heavyItems.add(trailer.container.items.get(ix));
                                }
                            }

                            if (!heavyItems.isEmpty()) {
                                int T = trailer.getCondition();
                                int S = 0;
                                int W = 0;

                                for (int ixx = 0; ixx < this.getVehicleTowing().parts.size(); ixx++) {
                                    VehiclePart part = this.getVehicleTowing().getPartByIndex(ixx);
                                    if (part != null && part.item != null) {
                                        if (part.partId != null && part.partId.contains("Suspension")) {
                                            S += part.getCondition();
                                        } else if (part.partId != null && part.partId.contains("Tire")) {
                                            W += part.getCondition();
                                        }
                                    }
                                }

                                float R = this.parameterVehicleSteer.getCurrentValue();
                                int dropChance = (int)(
                                    Math.pow(100 - T * 2, 2.0) * 0.3 * (1.0 + (100 - S / 2) * 0.005) * (1.0 + (100 - S / 2) * 0.005) * (1.0F + R / 3.0F)
                                );
                                if (Rand.Next(0, Math.max(10000 - dropChance, 1)) == 0) {
                                    InventoryItem droppedItem = heavyItems.get(Rand.Next(0, heavyItems.size()));
                                    droppedItem.setCondition(droppedItem.getCondition() - droppedItem.getConditionMax() / 10, false);
                                    trailer.getSquare().AddWorldInventoryItem(droppedItem, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                                    trailer.container.items.remove(droppedItem);
                                    trailer.getSquare().playSound("thumpa2");
                                }
                            }
                        }
                    }
                }

                if (this.physics != null && this.vehicleTowingId != -1 && this.vehicleTowing == null) {
                    this.tryReconnectToTowedVehicle();
                }

                boolean bTowed = false;
                boolean bTowing = false;
                if (this.getVehicleTowedBy() != null && this.getVehicleTowedBy().getController() != null) {
                    bTowed = this.getVehicleTowedBy() != null && this.getVehicleTowedBy().getController().isEnable;
                    bTowing = this.getVehicleTowing() != null && this.getVehicleTowing().getDriver() != null;
                }

                if (this.physics != null) {
                    boolean bUpdatePhysics = this.getDriver() != null || bTowed || bTowing;
                    long currentTimeMS = System.currentTimeMillis();
                    if (this.constraintChangedTime != -1L) {
                        if (this.constraintChangedTime + 3500L < currentTimeMS) {
                            this.constraintChangedTime = -1L;
                            if (!bUpdatePhysics && this.physicActiveCheck < currentTimeMS) {
                                this.setPhysicsActive(false);
                            }
                        }
                    } else {
                        if (this.physicActiveCheck != -1L && (bUpdatePhysics || !this.physics.isEnable)) {
                            this.physicActiveCheck = -1L;
                        }

                        if (!bUpdatePhysics && this.physics.isEnable && this.physicActiveCheck != -1L && this.physicActiveCheck < currentTimeMS) {
                            this.physicActiveCheck = -1L;
                            this.setPhysicsActive(false);
                        }
                    }

                    if (this.getVehicleTowedBy() != null && this.getScript().getWheelCount() > 0) {
                        this.physics.updateTrailer();
                    } else if (this.getDriver() == null && !GameServer.server) {
                        this.physics.checkShouldBeActive();
                    }

                    this.doAlarm();
                    BaseVehicle.VehicleImpulse impulse = this.impulseFromServer;
                    if (!GameServer.server && impulse != null && impulse.enable) {
                        impulse.enable = false;
                        float fpsScale = 1.0F;
                        Bullet.applyCentralForceToVehicle(this.vehicleId, impulse.impulse.x * 1.0F, impulse.impulse.y * 1.0F, impulse.impulse.z * 1.0F);
                        Vector3f torque = impulse.relPos.cross(impulse.impulse, TL_vector3f_pool.get().alloc());
                        Bullet.applyTorqueToVehicle(this.vehicleId, torque.x * 1.0F, torque.y * 1.0F, torque.z * 1.0F);
                        TL_vector3f_pool.get().release(torque);
                    }

                    int baseCheckTime = 1000;
                    if (System.currentTimeMillis() - this.engineCheckTime > 1000L && !GameClient.client) {
                        this.engineCheckTime = System.currentTimeMillis();
                        if (!GameClient.client) {
                            if (this.engineState != BaseVehicle.engineStateTypes.Idle) {
                                int newEngineLoudness = (int)(this.engineLoudness * this.engineSpeed / 2500.0);
                                double maxSpeed = Math.min(this.getEngineSpeed(), 2000.0);
                                newEngineLoudness = (int)(newEngineLoudness * (1.0 + maxSpeed / 4000.0));
                                int baseRand = 120;
                                if (GameServer.server) {
                                    baseRand = (int)(baseRand * ServerOptions.getInstance().carEngineAttractionModifier.getValue());
                                    newEngineLoudness = (int)(newEngineLoudness * ServerOptions.getInstance().carEngineAttractionModifier.getValue());
                                }

                                if (Rand.Next((int)(baseRand * GameTime.instance.getInvMultiplier())) == 0) {
                                    WorldSoundManager.instance
                                        .addSoundRepeating(
                                            this,
                                            PZMath.fastfloor(this.getX()),
                                            PZMath.fastfloor(this.getY()),
                                            PZMath.fastfloor(this.getZ()),
                                            Math.max(8, newEngineLoudness),
                                            Math.max(6, newEngineLoudness / 3),
                                            false,
                                            true
                                        );
                                }

                                if (Rand.Next((int)((baseRand - 85) * GameTime.instance.getInvMultiplier())) == 0) {
                                    WorldSoundManager.instance
                                        .addSoundRepeating(
                                            this,
                                            PZMath.fastfloor(this.getX()),
                                            PZMath.fastfloor(this.getY()),
                                            PZMath.fastfloor(this.getZ()),
                                            Math.max(8, newEngineLoudness / 2),
                                            Math.max(6, newEngineLoudness / 3),
                                            false,
                                            true
                                        );
                                }

                                if (Rand.Next((int)((baseRand - 110) * GameTime.instance.getInvMultiplier())) == 0) {
                                    WorldSoundManager.instance
                                        .addSoundRepeating(
                                            this,
                                            PZMath.fastfloor(this.getX()),
                                            PZMath.fastfloor(this.getY()),
                                            PZMath.fastfloor(this.getZ()),
                                            Math.max(8, newEngineLoudness / 4),
                                            Math.max(6, newEngineLoudness / 3),
                                            false,
                                            true
                                        );
                                }

                                WorldSoundManager.instance
                                    .addSoundRepeating(
                                        this,
                                        PZMath.fastfloor(this.getX()),
                                        PZMath.fastfloor(this.getY()),
                                        PZMath.fastfloor(this.getZ()),
                                        Math.max(8, newEngineLoudness / 6),
                                        Math.max(6, newEngineLoudness / 3),
                                        false,
                                        true
                                    );
                            }

                            if (this.lightbarSirenMode.isEnable() && this.getBatteryCharge() > 0.0F && SandboxOptions.instance.sirenEffectsZombies.getValue()) {
                                WorldSoundManager.instance
                                    .addSoundRepeating(
                                        this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 100, 60, false, true
                                    );
                            }
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.Running && !this.isEngineWorking()) {
                            String sound = "VehicleEngineFailureDamage";
                            VehiclePart gasTank = this.getPartById("GasTank");
                            if (gasTank != null && gasTank.getContainerContentAmount() <= 0.0F) {
                                sound = "VehicleRunningOutOfGas";
                            }

                            this.shutOff(sound);
                            this.checkVehicleFailsToStartWithZombiesTargeting();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.Running) {
                            VehiclePart engine = this.getPartById("Engine");
                            if (engine != null && engine.getCondition() < 50 && Rand.Next(Rand.AdjustForFramerate(engine.getCondition() * 12)) == 0) {
                                this.shutOff("VehicleEngineFailureDamage");
                                this.checkVehicleFailsToStartWithZombiesTargeting();
                            }
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.Starting) {
                            this.updateEngineStarting();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.RetryingStarting
                            && System.currentTimeMillis() - this.engineLastUpdateStateTime > 10L) {
                            this.engineDoStarting();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.StartingSuccess
                            && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                            this.engineDoRunning();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.StartingFailed
                            && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                            this.engineDoIdle();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.StartingFailedNoPower
                            && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                            this.engineDoIdle();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.Stalling && System.currentTimeMillis() - this.engineLastUpdateStateTime > 3000L) {
                            this.engineDoIdle();
                        }

                        if (this.engineState == BaseVehicle.engineStateTypes.ShutingDown && System.currentTimeMillis() - this.engineLastUpdateStateTime > 2000L
                            )
                         {
                            this.engineDoIdle();
                        }
                    }

                    if (this.getDriver() == null && !bTowed) {
                        this.getController().park();
                    }

                    this.setX(this.jniTransform.origin.x + WorldSimulation.instance.offsetX);
                    this.setY(this.jniTransform.origin.z + WorldSimulation.instance.offsetY);
                    this.setZ(0.0F);
                    int zi = PZMath.fastfloor(this.jniTransform.origin.y / 2.44949F + 0.05F);
                    IsoGridSquare square = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)zi);
                    IsoGridSquare below = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)(zi - 1));
                    if (square != null && (square.getFloor() != null || below != null && below.getFloor() != null)) {
                        this.setZ(zi);
                    }

                    IsoGridSquare sq = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
                    if (sq == null && !this.chunk.refs.isEmpty()) {
                        float d = 5.0E-4F;
                        int minX = this.chunk.wx * 8;
                        int minY = this.chunk.wy * 8;
                        int maxX = minX + 8;
                        int maxY = minY + 8;
                        this.setX(Math.max(this.getX(), minX + 5.0E-4F));
                        this.setX(Math.min(this.getX(), maxX - 5.0E-4F));
                        this.setY(Math.max(this.getY(), minY + 5.0E-4F));
                        this.setY(Math.min(this.getY(), maxY - 5.0E-4F));
                        this.setZ(0.2F);
                        Transform t = allocTransform();
                        Transform t1 = allocTransform();
                        this.getWorldTransform(t);
                        t1.basis.set(t.basis);
                        t1.origin.set(this.getX() - WorldSimulation.instance.offsetX, this.getZ(), this.getY() - WorldSimulation.instance.offsetY);
                        this.setWorldTransform(t1);
                        releaseTransform(t);
                        releaseTransform(t1);
                        this.current = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)PZMath.floor(this.getZ()));
                    }

                    if (this.current != null && this.current.chunk != null) {
                        if (this.current.getChunk() != this.chunk) {
                            assert this.chunk.vehicles.contains(this);

                            this.chunk.vehicles.remove(this);
                            this.chunk = this.current.getChunk();

                            assert !this.chunk.vehicles.contains(this);

                            this.chunk.vehicles.add(this);
                            IsoChunk.addFromCheckedVehicles(this);
                        }
                    } else {
                        boolean var56 = false;
                    }

                    this.updateTransform();
                    Vector3f currentVelocity = allocVector3f().set(this.jniLinearVelocity);
                    if (this.jniIsCollide && this.limitCrash.Check()) {
                        this.jniIsCollide = false;
                        this.limitCrash.Reset();
                        Vector3f velocityChange = allocVector3f();
                        velocityChange.set(currentVelocity).sub(this.lastLinearVelocity);
                        velocityChange.y = 0.0F;
                        float delta = velocityChange.length();
                        float DELTA_LIMIT = 6.0F;
                        if (currentVelocity.lengthSquared() > this.lastLinearVelocity.lengthSquared() && delta > 6.0F) {
                            DebugLog.Vehicle.trace("Vehicle vid=%d got sharp speed increase delta=%f", this.vehicleId, delta);
                            delta = 6.0F;
                        }

                        if (delta > 1.0F) {
                            if (this.lastLinearVelocity.length() < 6.0F) {
                                delta /= 3.0F;
                            }

                            DebugLog.Vehicle.trace("Vehicle vid=%d crash delta=%f", this.vehicleId, delta);
                            Vector3f forward = this.getForwardVector(allocVector3f());
                            float dot = velocityChange.dot(forward);
                            releaseVector3f(forward);
                            this.crash(delta * 3.0F, dot < 0.0F);
                            this.damageObjects(delta * 30.0F);
                        }

                        releaseVector3f(velocityChange);
                    }

                    this.lastLinearVelocity.set(currentVelocity);
                    releaseVector3f(currentVelocity);
                }

                if (this.soundAlarmOn && this.alarmEmitter != null) {
                    this.alarmEmitter.setPos(this.getX(), this.getY(), this.getZ());
                }

                if (this.soundHornOn && this.hornEmitter != null) {
                    this.hornEmitter.setPos(this.getX(), this.getY(), this.getZ());
                }

                for (int ixxx = 0; ixxx < this.impulseFromSquishedZombie.length; ixxx++) {
                    BaseVehicle.VehicleImpulse impulsex = this.impulseFromSquishedZombie[ixxx];
                    if (impulsex != null) {
                        impulsex.enable = false;
                    }
                }

                this.updateSounds();
                this.hittingPlant = false;
                this.breakingObjects();
                this.updateScrapPastPlantSound();
                if (this.addThumpWorldSound) {
                    this.addThumpWorldSound = false;
                    WorldSoundManager.instance
                        .addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 20, 20, true);
                }

                if (this.script.getLightbar().enable && this.lightbarLightsMode.isEnable() && this.getBatteryCharge() > 0.0F) {
                    this.lightbarLightsMode.update();
                }

                this.updateWorldLights();

                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                    if (this.current == null || !this.couldSeeIntersectedSquare(playerIndex)) {
                        this.setTargetAlpha(playerIndex, 0.0F);
                    }

                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player != null && this.DistToSquared(player) < 225.0F) {
                        this.setTargetAlpha(playerIndex, 1.0F);
                    }
                }

                for (int ixxxx = 0; ixxxx < this.getScript().getPassengerCount(); ixxxx++) {
                    if (this.getCharacter(ixxxx) != null) {
                        Vector3f worldPos = this.getPassengerWorldPos(ixxxx, TL_vector3f_pool.get().alloc());
                        this.getCharacter(ixxxx).setX(worldPos.x);
                        this.getCharacter(ixxxx).setY(worldPos.y);
                        this.getCharacter(ixxxx).setZ(worldPos.z * 1.0F);
                        TL_vector3f_pool.get().release(worldPos);
                    }
                }

                VehiclePart lightbar = this.getPartById("lightbar");
                if (lightbar != null && this.lightbarLightsMode.isEnable() && lightbar.getCondition() == 0 && !GameClient.client) {
                    this.setLightbarLightsMode(0);
                }

                if (lightbar != null && this.lightbarSirenMode.isEnable() && lightbar.getCondition() == 0 && !GameClient.client) {
                    this.setLightbarSirenMode(0);
                }

                if (!this.needPartsUpdate() && !this.isMechanicUIOpen() && !(this.alarmStartTime > 0.0)) {
                    this.drainBatteryUpdateHack();
                } else {
                    this.updateParts();
                }

                if (this.engineState == BaseVehicle.engineStateTypes.Running || bTowed) {
                    this.updateBulletStats();
                }

                if (this.doDamageOverlay) {
                    this.doDamageOverlay = false;
                    this.doDamageOverlay();
                }

                if (GameClient.client) {
                    this.checkPhysicsValidWithServer();
                }

                VehiclePart gasTank = this.getPartById("GasTank");
                if (gasTank != null && gasTank.getContainerContentAmount() > gasTank.getContainerCapacity()) {
                    gasTank.setContainerContentAmount(gasTank.getContainerCapacity());
                }

                boolean bHasPassengers = false;

                for (int ixxxxx = 0; ixxxxx < this.getMaxPassengers(); ixxxxx++) {
                    BaseVehicle.Passenger pngr = this.getPassenger(ixxxxx);
                    if (pngr.character != null) {
                        bHasPassengers = true;
                        break;
                    }
                }

                if (bHasPassengers) {
                    this.surroundVehicle.update();
                }

                if (!this.notKillCrops() && this.getSquare() != null) {
                    for (int ixxxxxx = -1; ixxxxxx < 1; ixxxxxx++) {
                        for (int j = -1; j < 1; j++) {
                            IsoGridSquare sqx = IsoWorld.instance
                                .currentCell
                                .getGridSquare(this.getSquare().getX() + ixxxxxx, this.getSquare().getY() + j, this.getSquare().getZ());
                            if (sqx != null) {
                                sqx.checkForIntersectingCrops(this);
                            }
                        }
                    }
                }

                if (!GameServer.server) {
                    if (this.physics != null) {
                        Bullet.setVehicleMass(this.vehicleId, this.getFudgedMass());
                    }

                    this.updateVelocityMultiplier();
                }
            }
        }
    }

    private void updateEngineStarting() {
        if (this.getBatteryCharge() <= 0.1F) {
            this.engineDoStartingFailedNoPower();
        } else {
            VehiclePart gasTank = this.getPartById("GasTank");
            if (gasTank != null && gasTank.getContainerContentAmount() <= 0.0F) {
                this.engineDoStartingFailed("VehicleRunningOutOfGas");
            } else {
                int weatherAffect = 0;
                float airTemp = ClimateManager.getInstance().getAirTemperatureForSquare(this.getSquare());
                if (this.engineQuality < 65 && airTemp <= 2.0F) {
                    weatherAffect = Math.min((2 - (int)airTemp) * 2, 30);
                }

                if (!SandboxOptions.instance.vehicleEasyUse.getValue() && this.engineQuality < 100 && Rand.Next(this.engineQuality + 50 - weatherAffect) <= 30) {
                    this.engineDoStartingFailed("VehicleEngineFailureDamage");
                } else {
                    if (Rand.Next(this.engineQuality) != 0) {
                        this.engineDoStartingSuccess();
                    } else {
                        this.engineDoRetryingStarting();
                    }
                }
            }
        }
    }

    public void applyImpulseFromHitZombies() {
        if (!this.impulseFromHitZombie.isEmpty()) {
            if ((!GameClient.client || this.hasAuthorization(GameClient.connection)) && !GameServer.server) {
                Vector3f force = TL_vector3f_pool.get().alloc().set(0.0F, 0.0F, 0.0F);
                Vector3f torque = TL_vector3f_pool.get().alloc().set(0.0F, 0.0F, 0.0F);
                Vector3f cross = TL_vector3f_pool.get().alloc().set(0.0F, 0.0F, 0.0F);
                int count = this.impulseFromHitZombie.size();

                for (int i = 0; i < count; i++) {
                    BaseVehicle.VehicleImpulse impulse = this.impulseFromHitZombie.get(i);
                    force.add(impulse.impulse);
                    torque.add(impulse.relPos.cross(impulse.impulse, cross));
                    impulse.release();
                    impulse.enable = false;
                }

                this.impulseFromHitZombie.clear();
                float limit = 7.0F * this.getFudgedMass();
                if (force.lengthSquared() > limit * limit) {
                    force.mul(limit / force.length());
                }

                float fpsScale = 30.0F;
                Bullet.applyCentralForceToVehicle(this.vehicleId, force.x * 30.0F, force.y * 30.0F, force.z * 30.0F);
                Bullet.applyTorqueToVehicle(this.vehicleId, torque.x * 30.0F, torque.y * 30.0F, torque.z * 30.0F);
                TL_vector3f_pool.get().release(force);
                TL_vector3f_pool.get().release(torque);
                TL_vector3f_pool.get().release(cross);
            } else {
                int i = 0;

                for (int count = this.impulseFromHitZombie.size(); i < count; i++) {
                    BaseVehicle.VehicleImpulse impulse = this.impulseFromHitZombie.get(i);
                    impulse.release();
                    impulse.enable = false;
                }

                this.impulseFromHitZombie.clear();
            }
        }
    }

    public void applyImpulseFromProneCharacters() {
        if ((!GameClient.client || this.hasAuthorization(GameClient.connection)) && !GameServer.server) {
            boolean hasImpulse = PZArrayUtil.contains(this.impulseFromSquishedZombie, impulse -> impulse != null && impulse.enable);
            if (hasImpulse) {
                Vector3f force = TL_vector3f_pool.get().alloc().set(0.0F, 0.0F, 0.0F);
                Vector3f torque = TL_vector3f_pool.get().alloc().set(0.0F, 0.0F, 0.0F);
                Vector3f cross = TL_vector3f_pool.get().alloc();

                for (int i = 0; i < this.impulseFromSquishedZombie.length; i++) {
                    BaseVehicle.VehicleImpulse impulse = this.impulseFromSquishedZombie[i];
                    if (impulse != null && impulse.enable && !impulse.applied) {
                        force.add(impulse.impulse);
                        torque.add(impulse.relPos.cross(impulse.impulse, cross));
                        impulse.applied = true;
                    }
                }

                if (force.lengthSquared() > 0.0F) {
                    float limit = this.getFudgedMass() * 0.15F;
                    if (force.lengthSquared() > limit * limit) {
                        force.mul(limit / force.length());
                    }

                    float fpsScale = 30.0F;
                    Bullet.applyCentralForceToVehicle(this.vehicleId, force.x * 30.0F, force.y * 30.0F, force.z * 30.0F);
                    Bullet.applyTorqueToVehicle(this.vehicleId, torque.x * 30.0F, torque.y * 30.0F, torque.z * 30.0F);
                }

                TL_vector3f_pool.get().release(force);
                TL_vector3f_pool.get().release(torque);
                TL_vector3f_pool.get().release(cross);
            }
        }
    }

    public float getFudgedMass() {
        if (this.getScriptName().contains("Trailer")) {
            return this.getMass();
        } else {
            BaseVehicle vehicleA = this.getVehicleTowedBy();
            if (vehicleA != null && vehicleA.getDriver() != null && vehicleA.isEngineRunning()) {
                float mass = Math.max(250.0F, vehicleA.getMass() / 3.7F);
                if (this.getScript().getWheelCount() == 0) {
                    mass = Math.min(mass, 200.0F);
                }

                return mass;
            } else {
                return this.getMass();
            }
        }
    }

    private boolean isNullChunk(int wx, int wy) {
        if (!IsoWorld.instance.getMetaGrid().isValidChunk(wx, wy)) {
            return false;
        } else if (GameClient.client && !ClientServerMap.isChunkLoaded(wx, wy)) {
            return true;
        } else {
            return GameClient.client && !PassengerMap.isChunkLoaded(this, wx, wy) ? true : this.getCell().getChunk(wx, wy) == null;
        }
    }

    public boolean isInvalidChunkAround() {
        Vector3f velocity = this.getLinearVelocity(allocVector3f());
        float absX = Math.abs(velocity.x);
        float absY = Math.abs(velocity.z);
        boolean moveW = velocity.x < 0.0F && absX > absY;
        boolean moveE = velocity.x > 0.0F && absX > absY;
        boolean moveN = velocity.z < 0.0F && absY > absX;
        boolean moveS = velocity.z > 0.0F && absY > absX;
        releaseVector3f(velocity);
        return this.isInvalidChunkAround(moveW, moveE, moveN, moveS);
    }

    public boolean isInvalidChunkAhead() {
        Vector3f angle = this.getForwardVector(allocVector3f());
        boolean moveW = angle.x < -0.5F;
        boolean moveS = angle.z > 0.5F;
        boolean moveE = angle.x > 0.5F;
        boolean moveN = angle.z < -0.5F;
        releaseVector3f(angle);
        return this.isInvalidChunkAround(moveW, moveE, moveN, moveS);
    }

    public boolean isInvalidChunkBehind() {
        Vector3f angle = this.getForwardVector(allocVector3f());
        boolean moveE = angle.x < -0.5F;
        boolean moveN = angle.z > 0.5F;
        boolean moveW = angle.x > 0.5F;
        boolean moveS = angle.z < -0.5F;
        releaseVector3f(angle);
        return this.isInvalidChunkAround(moveW, moveE, moveN, moveS);
    }

    public boolean isInvalidChunkAround(boolean moveW, boolean moveE, boolean moveN, boolean moveS) {
        if (IsoChunkMap.chunkGridWidth > 7) {
            if (moveE && (this.isNullChunk(this.chunk.wx + 1, this.chunk.wy) || this.isNullChunk(this.chunk.wx + 2, this.chunk.wy))) {
                return true;
            }

            if (moveW && (this.isNullChunk(this.chunk.wx - 1, this.chunk.wy) || this.isNullChunk(this.chunk.wx - 2, this.chunk.wy))) {
                return true;
            }

            if (moveS && (this.isNullChunk(this.chunk.wx, this.chunk.wy + 1) || this.isNullChunk(this.chunk.wx, this.chunk.wy + 2))) {
                return true;
            }

            if (moveN && (this.isNullChunk(this.chunk.wx, this.chunk.wy - 1) || this.isNullChunk(this.chunk.wx, this.chunk.wy - 2))) {
                return true;
            }
        } else if (IsoChunkMap.chunkGridWidth > 4) {
            if (moveE && this.isNullChunk(this.chunk.wx + 1, this.chunk.wy)) {
                return true;
            }

            if (moveW && this.isNullChunk(this.chunk.wx - 1, this.chunk.wy)) {
                return true;
            }

            if (moveS && this.isNullChunk(this.chunk.wx, this.chunk.wy + 1)) {
                return true;
            }

            if (moveN && this.isNullChunk(this.chunk.wx, this.chunk.wy - 1)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void postupdate() {
        int zi = PZMath.fastfloor(this.getZ());
        this.current = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), zi);
        if (this.current == null) {
            for (int n = PZMath.fastfloor(this.getZ()); n >= 0; n--) {
                this.current = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor((float)n));
                if (this.current != null) {
                    break;
                }
            }
        }

        if (this.movingSq != null) {
            this.movingSq.getMovingObjects().remove(this);
            this.movingSq = null;
        }

        if (this.current != null && !this.current.getMovingObjects().contains(this)) {
            this.current.getMovingObjects().add(this);
            this.movingSq = this.current;
        }

        this.square = this.current;
        if (this.sprite.hasActiveModel()) {
            this.updateAnimationPlayer(this.getAnimationPlayer(), null);

            for (int i = 0; i < this.models.size(); i++) {
                BaseVehicle.ModelInfo modelInfo = this.models.get(i);
                this.updateAnimationPlayer(modelInfo.getAnimationPlayer(), modelInfo.part);
            }
        }
    }

    private void updateAnimationPlayer(AnimationPlayer animPlayer, VehiclePart part) {
        if (animPlayer != null && animPlayer.isReady()) {
            AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
            float del = 0.016666668F;
            del *= 0.8F;
            del *= GameTime.instance.getUnmoddedMultiplier();
            animPlayer.Update(del);

            for (int i = 0; i < multiTrack.getTrackCount(); i++) {
                AnimationTrack track = multiTrack.getTracks().get(i);
                if (track.isPlaying && track.isFinished()) {
                    multiTrack.removeTrackAt(i);
                    i--;
                }
            }

            if (part != null) {
                BaseVehicle.ModelInfo modelInfo = this.getModelInfoForPart(part);
                if (modelInfo.track != null && multiTrack.getIndexOfTrack(modelInfo.track) == -1) {
                    modelInfo.track = null;
                }

                if (modelInfo.track != null) {
                    VehicleWindow window = part.getWindow();
                    if (window != null) {
                        AnimationTrack track = modelInfo.track;
                        track.setCurrentTimeValue(track.getDuration() * window.getOpenDelta());
                    }
                } else {
                    VehicleDoor door = part.getDoor();
                    if (door != null) {
                        this.playPartAnim(part, door.isOpen() ? "Opened" : "Closed");
                    }

                    VehicleWindow window = part.getWindow();
                    if (window != null) {
                        this.playPartAnim(part, "ClosedToOpen");
                    }
                }
            }
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        super.saveChange(change, tbl, bb);
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        super.loadChange(change, bb);
    }

    public void authorizationClientCollide(IsoPlayer driver) {
        if (driver != null && this.getDriver() == null) {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, driver.getOnlineID());
            this.authSimulationTime = System.currentTimeMillis();
            this.interpolation.clear();
            if (this.getVehicleTowing() != null) {
                this.getVehicleTowing().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, driver.getOnlineID());
                this.getVehicleTowing().authSimulationTime = System.currentTimeMillis();
                this.getVehicleTowing().interpolation.clear();
            } else if (this.getVehicleTowedBy() != null) {
                this.getVehicleTowedBy().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, driver.getOnlineID());
                this.getVehicleTowedBy().authSimulationTime = System.currentTimeMillis();
                this.getVehicleTowedBy().interpolation.clear();
            }
        }
    }

    public void authorizationServerCollide(short PlayerID, boolean isCollide) {
        if (!this.isNetPlayerAuthorization(BaseVehicle.Authorization.Local)) {
            if (isCollide) {
                this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, PlayerID);
                if (this.getVehicleTowing() != null) {
                    this.getVehicleTowing().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, PlayerID);
                } else if (this.getVehicleTowedBy() != null) {
                    this.getVehicleTowedBy().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, PlayerID);
                }
            } else {
                BaseVehicle.Authorization auth = PlayerID == -1 ? BaseVehicle.Authorization.Server : BaseVehicle.Authorization.Local;
                this.setNetPlayerAuthorization(auth, PlayerID);
                if (this.getVehicleTowing() != null) {
                    this.getVehicleTowing().setNetPlayerAuthorization(auth, PlayerID);
                } else if (this.getVehicleTowedBy() != null) {
                    this.getVehicleTowedBy().setNetPlayerAuthorization(auth, PlayerID);
                }
            }
        }
    }

    public void authorizationServerOnSeat(IsoPlayer player, boolean enter) {
        BaseVehicle vehicleA = this.getVehicleTowing();
        BaseVehicle vehicleB = this.getVehicleTowedBy();
        if (this.isNetPlayerId((short)-1) && enter) {
            if (vehicleA != null && vehicleA.getDriver() == null) {
                this.addPointConstraint(null, vehicleA, this.getTowAttachmentSelf(), vehicleA.getTowAttachmentSelf());
            } else if (vehicleB != null && vehicleB.getDriver() == null) {
                this.addPointConstraint(null, vehicleB, this.getTowAttachmentSelf(), vehicleB.getTowAttachmentSelf());
            } else {
                this.setNetPlayerAuthorization(BaseVehicle.Authorization.Local, player.getOnlineID());
            }
        } else if (this.isNetPlayerId(player.getOnlineID()) && !enter) {
            if (vehicleA != null && vehicleA.getDriver() != null) {
                vehicleA.addPointConstraint(null, this, vehicleA.getTowAttachmentSelf(), this.getTowAttachmentSelf());
            } else if (vehicleB != null && vehicleB.getDriver() != null) {
                vehicleB.addPointConstraint(null, this, vehicleB.getTowAttachmentSelf(), this.getTowAttachmentSelf());
            } else {
                this.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
                if (vehicleA != null) {
                    vehicleA.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
                } else if (vehicleB != null) {
                    vehicleB.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
                }
            }
        }
    }

    public boolean hasAuthorization(UdpConnection connection) {
        if (!this.isNetPlayerId((short)-1) && connection != null) {
            if (GameServer.server) {
                for (int i = 0; i < connection.players.length; i++) {
                    if (connection.players[i] != null && this.isNetPlayerId(connection.players[i].onlineId)) {
                        return true;
                    }
                }

                return false;
            } else {
                return this.isNetPlayerId(IsoPlayer.getInstance().getOnlineID());
            }
        } else {
            return false;
        }
    }

    public void netPlayerFromServerUpdate(BaseVehicle.Authorization authorization, short authorizationPlayer) {
        if (!this.isNetPlayerAuthorization(authorization) || !this.isNetPlayerId(authorizationPlayer)) {
            if (BaseVehicle.Authorization.Local == authorization) {
                if (IsoPlayer.getLocalPlayerByOnlineID(authorizationPlayer) != null) {
                    this.setNetPlayerAuthorization(BaseVehicle.Authorization.Local, authorizationPlayer);
                } else {
                    this.setNetPlayerAuthorization(BaseVehicle.Authorization.Remote, authorizationPlayer);
                }
            } else if (BaseVehicle.Authorization.LocalCollide == authorization) {
                if (IsoPlayer.getLocalPlayerByOnlineID(authorizationPlayer) != null) {
                    this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, authorizationPlayer);
                } else {
                    this.setNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide, authorizationPlayer);
                }
            } else {
                this.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
            }
        }
    }

    public Transform getWorldTransform(Transform out) {
        out.set(this.jniTransform);
        return out;
    }

    public void setWorldTransform(Transform in) {
        this.jniTransform.set(in);
        Quaternionf rotation = allocQuaternionf();
        in.getRotation(rotation);
        if (!GameServer.server) {
            Bullet.teleportVehicle(
                this.vehicleId,
                in.origin.x + WorldSimulation.instance.offsetX,
                in.origin.z + WorldSimulation.instance.offsetY,
                in.origin.y,
                rotation.x,
                rotation.y,
                rotation.z,
                rotation.w
            );
        }

        releaseQuaternionf(rotation);
    }

    public void flipUpright() {
        Transform xfrm = allocTransform();
        xfrm.set(this.jniTransform);
        Quaternionf rotation = allocQuaternionf();
        rotation.setAngleAxis(0.0F, _UNIT_Y.x, _UNIT_Y.y, _UNIT_Y.z);
        xfrm.setRotation(rotation);
        releaseQuaternionf(rotation);
        this.setWorldTransform(xfrm);
        releaseTransform(xfrm);
    }

    public void setAngles(float degreesX, float degreesY, float degreesZ) {
        if ((int)degreesX != (int)this.getAngleX() || (int)degreesY != (int)this.getAngleY() || degreesZ != (int)this.getAngleZ()) {
            this.polyDirty = true;
            float radiansX = degreesX * (float) (Math.PI / 180.0);
            float radiansY = degreesY * (float) (Math.PI / 180.0);
            float radiansZ = degreesZ * (float) (Math.PI / 180.0);
            Quaternionf q = allocQuaternionf();
            q.rotationXYZ(radiansX, radiansY, radiansZ);
            Transform xfrm = allocTransform();
            xfrm.set(this.jniTransform);
            xfrm.setRotation(q);
            releaseQuaternionf(q);
            this.setWorldTransform(xfrm);
            releaseTransform(xfrm);
        }
    }

    public float getAngleX() {
        Vector3f v = TL_vector3f_pool.get().alloc();
        Quaternionf q = allocQuaternionf();
        this.jniTransform.getRotation(q).getEulerAnglesXYZ(v);
        releaseQuaternionf(q);
        float angle = v.x * (180.0F / (float)Math.PI);
        TL_vector3f_pool.get().release(v);
        return angle;
    }

    public float getAngleY() {
        Vector3f v = TL_vector3f_pool.get().alloc();
        Quaternionf q = allocQuaternionf();
        this.jniTransform.getRotation(q).getEulerAnglesXYZ(v);
        releaseQuaternionf(q);
        float angle = v.y * (180.0F / (float)Math.PI);
        TL_vector3f_pool.get().release(v);
        return angle;
    }

    public float getAngleZ() {
        Vector3f v = TL_vector3f_pool.get().alloc();
        Quaternionf q = allocQuaternionf();
        this.jniTransform.getRotation(q).getEulerAnglesXYZ(v);
        releaseQuaternionf(q);
        float angle = v.z * (180.0F / (float)Math.PI);
        TL_vector3f_pool.get().release(v);
        return angle;
    }

    public void setDebugZ(float z) {
        Transform xfrm = allocTransform();
        xfrm.set(this.jniTransform);
        int zi = PZMath.fastfloor(this.jniTransform.origin.y / 2.44949F);
        xfrm.origin.y = (zi + PZMath.clamp(z, 0.0F, 0.99F)) * 3.0F * 0.8164967F;
        this.setWorldTransform(xfrm);
        releaseTransform(xfrm);
    }

    public void setPhysicsActive(boolean active) {
        if (this.physics != null && active != this.physics.isEnable) {
            this.physics.isEnable = active;
            if (!GameServer.server) {
                if (this.isStatic != !active) {
                    Bullet.setVehicleStatic(this, !active);
                }

                if (this.isActive != active) {
                    Bullet.setVehicleActive(this, active);
                }
            }

            if (active) {
                this.physicActiveCheck = System.currentTimeMillis() + 3000L;
            }

            BaseVehicle towedVehicle = this.getVehicleTowing();
            if (towedVehicle != null) {
                towedVehicle.setPhysicsActive(active);
            }
        }
    }

    public float getDebugZ() {
        return this.jniTransform.origin.y / 2.44949F;
    }

    public VehiclePoly getPoly() {
        if (this.polyDirty) {
            if (this.polyGarageCheck && this.square != null) {
                if (this.square.getRoom() != null && this.square.getRoom().roomDef != null && this.square.getRoom().roomDef.contains("garagestorage")) {
                    this.radiusReductionInGarage = -0.3F;
                } else {
                    this.radiusReductionInGarage = 0.0F;
                }

                this.polyGarageCheck = false;
            }

            this.poly.init(this, 0.0F);
            this.polyPlusRadius.init(this, 0.15F + this.radiusReductionInGarage);
            this.polyDirty = false;
            this.polyPlusRadiusMinX = -123.0F;
            this.initShadowPoly();
        }

        return this.poly;
    }

    public VehiclePoly getPolyPlusRadius() {
        if (this.polyDirty) {
            if (this.polyGarageCheck && this.square != null) {
                if (this.square.getRoom() != null && this.square.getRoom().roomDef != null && this.square.getRoom().roomDef.contains("garagestorage")) {
                    this.radiusReductionInGarage = -0.3F;
                } else {
                    this.radiusReductionInGarage = 0.0F;
                }

                this.polyGarageCheck = false;
            }

            this.poly.init(this, 0.0F);
            this.polyPlusRadius.init(this, 0.15F + this.radiusReductionInGarage);
            this.polyDirty = false;
            this.polyPlusRadiusMinX = -123.0F;
            this.initShadowPoly();
        }

        return this.polyPlusRadius;
    }

    private void initShadowPoly() {
        Transform xfrm = this.getWorldTransform(allocTransform());
        Quaternionf q = xfrm.getRotation(allocQuaternionf());
        releaseTransform(xfrm);
        Vector2f ext = this.script.getShadowExtents();
        Vector2f off = this.script.getShadowOffset();
        float width = ext.x / 2.0F;
        float length = ext.y / 2.0F;
        Vector3f v = allocVector3f();
        if (q.x < 0.0F) {
            this.getWorldPos(off.x - width, 0.0F, off.y + length, v);
            this.shadowCoord.x1 = v.x;
            this.shadowCoord.y1 = v.y;
            this.getWorldPos(off.x + width, 0.0F, off.y + length, v);
            this.shadowCoord.x2 = v.x;
            this.shadowCoord.y2 = v.y;
            this.getWorldPos(off.x + width, 0.0F, off.y - length, v);
            this.shadowCoord.x3 = v.x;
            this.shadowCoord.y3 = v.y;
            this.getWorldPos(off.x - width, 0.0F, off.y - length, v);
            this.shadowCoord.x4 = v.x;
            this.shadowCoord.y4 = v.y;
        } else {
            this.getWorldPos(off.x - width, 0.0F, off.y + length, v);
            this.shadowCoord.x1 = v.x;
            this.shadowCoord.y1 = v.y;
            this.getWorldPos(off.x + width, 0.0F, off.y + length, v);
            this.shadowCoord.x2 = v.x;
            this.shadowCoord.y2 = v.y;
            this.getWorldPos(off.x + width, 0.0F, off.y - length, v);
            this.shadowCoord.x3 = v.x;
            this.shadowCoord.y3 = v.y;
            this.getWorldPos(off.x - width, 0.0F, off.y - length, v);
            this.shadowCoord.x4 = v.x;
            this.shadowCoord.y4 = v.y;
        }

        releaseVector3f(v);
        releaseQuaternionf(q);
    }

    private void initPolyPlusRadiusBounds() {
        if (this.polyPlusRadiusMinX == -123.0F) {
            VehiclePoly poly = this.getPolyPlusRadius();
            Vector3f localPos = TL_vector3f_pool.get().alloc();
            Vector3f v = this.getLocalPos(poly.x1, poly.y1, poly.z, localPos);
            float x1 = PZMath.fastfloor(v.x * 100.0F) / 100.0F;
            float y1 = PZMath.fastfloor(v.z * 100.0F) / 100.0F;
            v = this.getLocalPos(poly.x2, poly.y2, poly.z, localPos);
            float x2 = PZMath.fastfloor(v.x * 100.0F) / 100.0F;
            float y2 = PZMath.fastfloor(v.z * 100.0F) / 100.0F;
            v = this.getLocalPos(poly.x3, poly.y3, poly.z, localPos);
            float x3 = PZMath.fastfloor(v.x * 100.0F) / 100.0F;
            float y3 = PZMath.fastfloor(v.z * 100.0F) / 100.0F;
            v = this.getLocalPos(poly.x4, poly.y4, poly.z, localPos);
            float x4 = PZMath.fastfloor(v.x * 100.0F) / 100.0F;
            float y4 = PZMath.fastfloor(v.z * 100.0F) / 100.0F;
            this.polyPlusRadiusMinX = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
            this.polyPlusRadiusMaxX = Math.max(x1, Math.max(x2, Math.max(x3, x4)));
            this.polyPlusRadiusMinY = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
            this.polyPlusRadiusMaxY = Math.max(y1, Math.max(y2, Math.max(y3, y4)));
            TL_vector3f_pool.get().release(localPos);
        }
    }

    public Vector3f getForwardVector(Vector3f out) {
        int forwardAxis = 2;
        return this.jniTransform.basis.getColumn(2, out);
    }

    public Vector3f getUpVector(Vector3f out) {
        int axis = 1;
        return this.jniTransform.basis.getColumn(1, out);
    }

    public float getUpVectorDot() {
        Vector3f up = this.getUpVector(TL_vector3f_pool.get().alloc());
        float dot = up.dot(_UNIT_Y);
        TL_vector3f_pool.get().release(up);
        return dot;
    }

    public boolean isStopped() {
        return this.jniSpeed > -0.8F && this.jniSpeed < 0.8F && !this.getController().isGasPedalPressed();
    }

    public void setSpeedKmHour(float speedKmHour) {
        this.jniSpeed = speedKmHour;
    }

    public float getCurrentSpeedKmHour() {
        return this.jniSpeed;
    }

    public float getCurrentAbsoluteSpeedKmHour() {
        return PZMath.abs(this.jniSpeed);
    }

    public Vector3f getLinearVelocity(Vector3f out) {
        return out.set(this.jniLinearVelocity);
    }

    public float getSpeed2D() {
        float vx = this.jniLinearVelocity.x;
        float vy = this.jniLinearVelocity.z;
        return (float)Math.sqrt(vx * vx + vy * vy);
    }

    public boolean isAtRest() {
        if (this.physics == null) {
            return true;
        } else if (!this.impulseFromHitZombie.isEmpty()) {
            return false;
        } else if (this.impulseFromServer.enable) {
            return false;
        } else if (Math.abs(this.physics.engineForce) >= 0.01F) {
            return false;
        } else if (this.getSpeed2D() >= 0.02F) {
            return false;
        } else {
            float velocityVertical = this.jniLinearVelocity.y;
            if (Math.abs(velocityVertical) >= 0.5F) {
                return false;
            } else {
                IsoGridSquare ourSquare = this.getSquare();
                if (!ourSquare.hasFloor()) {
                    return false;
                } else {
                    float z = this.jniTransform.origin.y / 2.44949F;
                    float heightAboveGround = z - ourSquare.z;
                    return !(heightAboveGround > 0.2F);
                }
            }
        }
    }

    protected void updateTransform() {
        if (this.sprite.modelSlot != null) {
            float scale = this.getScript().getModelScale();
            float scale2 = 1.0F;
            if (this.sprite.modelSlot != null && this.sprite.modelSlot.model.scale != 1.0F) {
                scale2 = this.sprite.modelSlot.model.scale;
            }

            Quaternionf chassisRot = TL_quaternionf_pool.get().alloc();
            Quaternionf modelRotQ = TL_quaternionf_pool.get().alloc();
            Matrix4f matrix4f = TL_matrix4f_pool.get().alloc();
            int rightToLeftHand = -1;
            Transform chassisTrans = this.getWorldTransform(allocTransform());
            chassisTrans.getRotation(chassisRot);
            releaseTransform(chassisTrans);
            chassisRot.y *= -1.0F;
            chassisRot.z *= -1.0F;
            Matrix4f chassisRotM = chassisRot.get(matrix4f);
            float scaleInvertX = 1.0F;
            if (this.sprite.modelSlot.model.modelScript != null) {
                scaleInvertX = this.sprite.modelSlot.model.modelScript.invertX ? -1.0F : 1.0F;
            }

            Vector3f modelOffset = this.script.getModel().getOffset();
            Vector3f modelRotate = this.getScript().getModel().getRotate();
            modelRotQ.rotationXYZ(
                modelRotate.x * (float) (Math.PI / 180.0), modelRotate.y * (float) (Math.PI / 180.0), modelRotate.z * (float) (Math.PI / 180.0)
            );
            this.renderTransform
                .translationRotateScale(
                    modelOffset.x * -1.0F,
                    modelOffset.y,
                    modelOffset.z,
                    modelRotQ.x,
                    modelRotQ.y,
                    modelRotQ.z,
                    modelRotQ.w,
                    scale * scale2 * scaleInvertX,
                    scale * scale2,
                    scale * scale2
                );
            chassisRotM.mul(this.renderTransform, this.renderTransform);
            this.vehicleTransform.translationRotateScale(modelOffset.x * -1.0F, modelOffset.y, modelOffset.z, 0.0F, 0.0F, 0.0F, 1.0F, scale);
            chassisRotM.mul(this.vehicleTransform, this.vehicleTransform);

            for (int i = 0; i < this.models.size(); i++) {
                BaseVehicle.ModelInfo modelInfo = this.models.get(i);
                VehicleScript.Model scriptModel = modelInfo.scriptModel;
                modelOffset = scriptModel.getOffset();
                modelRotate = scriptModel.getRotate();
                float scale1 = scriptModel.scale;
                scale2 = 1.0F;
                float scaleInvertXx = 1.0F;
                if (modelInfo.modelScript != null) {
                    scale2 = modelInfo.modelScript.scale;
                    scaleInvertXx = modelInfo.modelScript.invertX ? -1.0F : 1.0F;
                }

                int rotateYZ = 1;
                if (modelInfo.wheelIndex == -1) {
                    rotateYZ = -1;
                }

                modelRotQ.rotationXYZ(
                    modelRotate.x * (float) (Math.PI / 180.0),
                    modelRotate.y * (float) (Math.PI / 180.0) * rotateYZ,
                    modelRotate.z * (float) (Math.PI / 180.0) * rotateYZ
                );
                if (modelInfo.wheelIndex == -1) {
                    if (modelInfo.part != null
                        && modelInfo.part.scriptPart != null
                        && modelInfo.part.scriptPart.parent != null
                        && scriptModel.attachmentNameParent != null) {
                        BaseVehicle.ModelInfo parentModelInfo = this.getModelInfoForPart(modelInfo.part.getParent());
                        Matrix4f attachmentXfrm = TL_matrix4f_pool.get().alloc();
                        this.initTransform(
                            parentModelInfo.modelInstance,
                            parentModelInfo.modelScript,
                            modelInfo.modelScript,
                            scriptModel.attachmentNameParent,
                            scriptModel.attachmentNameSelf,
                            attachmentXfrm
                        );
                        Model parentModel = parentModelInfo.modelInstance.model;
                        ModelInstanceRenderData.preMultiplyMeshTransform(attachmentXfrm, parentModel.mesh);
                        parentModelInfo.renderTransform.mul(attachmentXfrm, modelInfo.renderTransform);
                        boolean bIgnoreVehicleScale = scriptModel.ignoreVehicleScale;
                        float scale3 = bIgnoreVehicleScale ? 1.5F / this.getScript().getModelScale() : 1.0F;
                        modelInfo.renderTransform.scale(scale1 * scale2 * scale3);
                        TL_matrix4f_pool.get().release(attachmentXfrm);
                    } else {
                        modelInfo.renderTransform
                            .translationRotateScale(
                                modelOffset.x * -1.0F,
                                modelOffset.y,
                                modelOffset.z,
                                modelRotQ.x,
                                modelRotQ.y,
                                modelRotQ.z,
                                modelRotQ.w,
                                scale1 * scale2 * scaleInvertXx,
                                scale1 * scale2,
                                scale1 * scale2
                            );
                        this.vehicleTransform.mul(modelInfo.renderTransform, modelInfo.renderTransform);
                    }
                } else {
                    BaseVehicle.WheelInfo wheelInfo = this.wheelInfo[modelInfo.wheelIndex];
                    float steering = wheelInfo.steering;
                    float rotate = wheelInfo.rotation;
                    VehicleScript.Wheel scriptWheel = this.getScript().getWheel(modelInfo.wheelIndex);
                    BaseVehicle.VehicleImpulse impulse = modelInfo.wheelIndex < this.impulseFromSquishedZombie.length
                        ? this.impulseFromSquishedZombie[modelInfo.wheelIndex]
                        : null;
                    float corpseOffset = impulse != null && impulse.enable ? 0.05F : 0.0F;
                    if (wheelInfo.suspensionLength == 0.0F) {
                        matrix4f.translation(scriptWheel.offset.x / scale * -1.0F, scriptWheel.offset.y / scale, scriptWheel.offset.z / scale);
                    } else {
                        matrix4f.translation(
                            scriptWheel.offset.x / scale * -1.0F,
                            (scriptWheel.offset.y + this.script.getSuspensionRestLength() - wheelInfo.suspensionLength) / scale + corpseOffset * 0.5F,
                            scriptWheel.offset.z / scale
                        );
                    }

                    modelInfo.renderTransform.identity();
                    modelInfo.renderTransform.mul(matrix4f);
                    modelInfo.renderTransform.rotateY(steering * -1.0F);
                    modelInfo.renderTransform.rotateX(rotate);
                    matrix4f.translationRotateScale(
                        modelOffset.x * -1.0F,
                        modelOffset.y,
                        modelOffset.z,
                        modelRotQ.x,
                        modelRotQ.y,
                        modelRotQ.z,
                        modelRotQ.w,
                        scale1 * scale2 * scaleInvertXx,
                        scale1 * scale2,
                        scale1 * scale2
                    );
                    modelInfo.renderTransform.mul(matrix4f);
                    this.vehicleTransform.mul(modelInfo.renderTransform, modelInfo.renderTransform);
                }
            }

            TL_matrix4f_pool.get().release(matrix4f);
            TL_quaternionf_pool.get().release(chassisRot);
            TL_quaternionf_pool.get().release(modelRotQ);
        }
    }

    private void initTransform(
        ModelInstance parentModelInstance,
        ModelScript parentModelScript,
        ModelScript modelScript,
        String m_attachmentNameParent,
        String m_attachmentNameSelf,
        Matrix4f m_transform
    ) {
        m_transform.identity();
        Matrix4f attachmentXfrm = TL_matrix4f_pool.get().alloc();
        ModelAttachment parentAttachment = parentModelScript.getAttachmentById(m_attachmentNameParent);
        if (parentAttachment == null) {
            parentAttachment = this.getScript().getAttachmentById(m_attachmentNameParent);
        }

        if (parentAttachment != null) {
            ModelInstanceRenderData.makeBoneTransform(parentModelInstance.animPlayer, parentAttachment.getBone(), m_transform);
            m_transform.scale(1.0F / parentModelScript.scale);
            ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
            m_transform.mul(attachmentXfrm);
        }

        ModelAttachment selfAttachment = modelScript.getAttachmentById(m_attachmentNameSelf);
        if (selfAttachment != null) {
            ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
            if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                attachmentXfrm.invert();
            }

            m_transform.mul(attachmentXfrm);
        }

        TL_matrix4f_pool.get().release(attachmentXfrm);
    }

    public void updatePhysics() {
        this.physics.update();
    }

    public void updatePhysicsNetwork() {
        if (this.limitPhysicSend.Check()) {
            INetworkPacket.send(this.isReliable ? PacketTypes.PacketType.VehiclePhysicsReliable : PacketTypes.PacketType.VehiclePhysicsUnreliable, this);
            if (this.limitPhysicPositionSent == null) {
                this.limitPhysicPositionSent = new Vector2();
            } else if (IsoUtils.DistanceToSquared(this.limitPhysicPositionSent.x, this.limitPhysicPositionSent.y, this.getX(), this.getY()) > 0.001F) {
                this.limitPhysicSend.setUpdatePeriod(150L);
            } else {
                this.limitPhysicSend.setSmoothUpdatePeriod(300L);
            }

            this.limitPhysicPositionSent.set(this.getX(), this.getY());
        }
    }

    public void checkPhysicsValidWithServer() {
        float delta = 0.05F;
        if (this.limitPhysicValid.Check() && Bullet.getOwnVehiclePhysics(this.vehicleId, physicsParams) == 0) {
            float diffX = Math.abs(physicsParams[0] - this.getX());
            float diffY = Math.abs(physicsParams[1] - this.getY());
            if (diffX > 0.05F || diffY > 0.05F) {
                VehicleManager.instance.sendVehicleRequest(this.vehicleId, (short)2);
                DebugLog.Vehicle.trace("diff-x=%f diff-y=%f delta=%f", diffX, diffY, 0.05F);
            }
        }
    }

    public void updateControls() {
        if (this.getController() != null) {
            if (this.isOperational()) {
                IsoPlayer player = Type.tryCastTo(this.getDriver(), IsoPlayer.class);
                if (player == null || !player.isBlockMovement()) {
                    this.getController().updateControls();
                }
            }
        }
    }

    public boolean isKeyboardControlled() {
        IsoGameCharacter chr = this.getCharacter(0);
        return chr != null && chr == IsoPlayer.players[0] && this.getVehicleTowedBy() == null;
    }

    public int getJoypad() {
        IsoGameCharacter chr = this.getCharacter(0);
        return chr != null && chr instanceof IsoPlayer isoPlayer ? isoPlayer.joypadBind : -1;
    }

    @Override
    public void Damage(float amount) {
        this.crash(amount, true);
    }

    @Override
    public void HitByVehicle(BaseVehicle vehicle, float amount) {
        this.crash(amount, true);
    }

    public void crash(float delta, boolean front) {
        StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Vehicle, "Vehicle Crash Counter", 1.0F);
        if (GameClient.client) {
            SoundManager.instance.PlayWorldSound(this.getCrashSound(delta), this.square, 1.0F, 20.0F, 1.0F, true);
            GameClient.instance.sendClientCommandV(null, "vehicle", "crash", "vehicle", this.getId(), "amount", delta, "front", front);
        } else {
            float modifier = 1.3F;
            switch (SandboxOptions.instance.carDamageOnImpact.getValue()) {
                case 1:
                    modifier = 1.9F;
                    break;
                case 2:
                    modifier = 1.6F;
                case 3:
                default:
                    break;
                case 4:
                    modifier = 1.1F;
                    break;
                case 5:
                    modifier = 0.9F;
            }

            delta = Math.abs(delta) / modifier;
            if (front) {
                this.addDamageFront((int)delta);
            } else {
                this.addDamageRear((int)Math.abs(delta / modifier));
            }

            this.damagePlayers(Math.abs(delta));
            SoundManager.instance.PlayWorldSound(this.getCrashSound(delta), this.square, 1.0F, 20.0F, 1.0F, true);
            if (this.getVehicleTowing() != null) {
                this.getVehicleTowing().crash(delta, front);
            }

            if (this.getAnimals() != null && !this.getAnimals().isEmpty()) {
                for (int i = 0; i < this.getAnimals().size(); i++) {
                    this.getAnimals().get(i).carCrash(delta, front);
                }
            }

            IsoPlayer driver = Type.tryCastTo(this.getDriverRegardlessOfTow(), IsoPlayer.class);
            if (driver != null && driver.isLocalPlayer()) {
                driver.triggerMusicIntensityEvent("VehicleCrash");
            }
        }
    }

    private String getCrashSound(float dmg) {
        if (dmg < 5.0F) {
            return "VehicleCrash1";
        } else {
            return dmg < 30.0F ? "VehicleCrash2" : "VehicleCrash";
        }
    }

    /**
     * When hitting a character (zombie or player) damage aren't the same as hitting a wall.
     *  damaged will be mainly focus on windshield/hood, not on doors/windows like when hitting a wall.
     */
    public void addDamageFrontHitAChr(int dmg) {
        if (!this.isDriverGodMode()) {
            if (dmg >= 4 || !Rand.NextBool(7)) {
                VehiclePart part = this.getPartById("EngineDoor");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
                }

                if (part != null && (part.getCondition() <= 0 || part.getInventoryItem() == null) && Rand.NextBool(4)) {
                    part = this.getPartById("Engine");
                    if (part != null) {
                        part.damage(Rand.Next(2, 4));
                    }
                }

                if (dmg > 12) {
                    part = this.getPartById("Windshield");
                    if (part != null && part.getInventoryItem() != null) {
                        part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
                    }
                }

                if (Rand.Next(5) < dmg) {
                    if (Rand.NextBool(2)) {
                        part = this.getPartById("TireFrontLeft");
                    } else {
                        part = this.getPartById("TireFrontRight");
                    }

                    if (part != null && part.getInventoryItem() != null) {
                        part.damage(Rand.Next(1, 3));
                    }
                }

                if (Rand.Next(7) < dmg) {
                    this.damageHeadlight("HeadlightLeft", Rand.Next(1, 4));
                }

                if (Rand.Next(7) < dmg) {
                    this.damageHeadlight("HeadlightRight", Rand.Next(1, 4));
                }

                float intensity = this.getBloodIntensity("Front");
                this.setBloodIntensity("Front", intensity + 0.01F);
            }
        }
    }

    /**
     * When hitting a character (zombie or player) damage aren't the same as hitting a wall.
     *  damaged will be mainly focus on windshield/truckbed, not on doors/windows like when hitting a wall.
     */
    public void addDamageRearHitAChr(int dmg) {
        if (!this.isDriverGodMode()) {
            if (dmg >= 4 || !Rand.NextBool(7)) {
                VehiclePart part = this.getPartById("TruckBed");
                if (part != null && part.getInventoryItem() != null) {
                    part.setCondition(part.getCondition() - Rand.Next(Math.max(1, dmg - 10), dmg + 3));
                    part.doInventoryItemStats(part.getInventoryItem(), 0);
                    this.transmitPartCondition(part);
                }

                part = this.getPartById("DoorRear");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
                }

                part = this.getPartById("TrunkDoor");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
                }

                if (dmg > 12) {
                    part = this.getPartById("WindshieldRear");
                    if (part != null && part.getInventoryItem() != null) {
                        part.damage(dmg);
                    }
                }

                if (Rand.Next(5) < dmg) {
                    if (Rand.NextBool(2)) {
                        part = this.getPartById("TireRearLeft");
                    } else {
                        part = this.getPartById("TireRearRight");
                    }

                    if (part != null && part.getInventoryItem() != null) {
                        part.damage(Rand.Next(1, 3));
                    }
                }

                if (Rand.Next(7) < dmg) {
                    this.damageHeadlight("HeadlightRearLeft", Rand.Next(1, 4));
                }

                if (Rand.Next(7) < dmg) {
                    this.damageHeadlight("HeadlightRearRight", Rand.Next(1, 4));
                }

                if (Rand.Next(6) < dmg) {
                    part = this.getPartById("GasTank");
                    if (part != null && part.getInventoryItem() != null) {
                        part.damage(Rand.Next(1, 3));
                    }
                }

                float intensity = this.getBloodIntensity("Rear");
                this.setBloodIntensity("Rear", intensity + 0.01F);
            }
        }
    }

    private void addDamageFront(int dmg) {
        if (!this.isDriverGodMode()) {
            this.currentFrontEndDurability -= dmg;
            VehiclePart part = this.getPartById("EngineDoor");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }

            if (part == null || part.getInventoryItem() == null || part.getCondition() < 25) {
                part = this.getPartById("Engine");
                if (part != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 3), dmg + 3));
                }
            }

            part = this.getPartById("Windshield");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }

            if (Rand.Next(4) == 0) {
                part = this.getPartById("DoorFrontLeft");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }

                part = this.getPartById("WindowFrontLeft");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }
            }

            if (Rand.Next(4) == 0) {
                part = this.getPartById("DoorFrontRight");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }

                part = this.getPartById("WindowFrontRight");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }
            }

            if (Rand.Next(20) < dmg) {
                this.damageHeadlight("HeadlightLeft", dmg);
            }

            if (Rand.Next(20) < dmg) {
                this.damageHeadlight("HeadlightRight", dmg);
            }
        }
    }

    private void addDamageRear(int dmg) {
        if (!this.isDriverGodMode()) {
            this.currentRearEndDurability -= dmg;
            VehiclePart part = this.getPartById("TruckBed");
            if (part != null && part.getInventoryItem() != null) {
                part.setCondition(part.getCondition() - Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                part.doInventoryItemStats(part.getInventoryItem(), 0);
                this.transmitPartCondition(part);
            }

            part = this.getPartById("DoorRear");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }

            part = this.getPartById("TrunkDoor");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }

            part = this.getPartById("WindshieldRear");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(dmg);
            }

            if (Rand.Next(4) == 0) {
                part = this.getPartById("DoorRearLeft");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }

                part = this.getPartById("WindowRearLeft");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }
            }

            if (Rand.Next(4) == 0) {
                part = this.getPartById("DoorRearRight");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }

                part = this.getPartById("WindowRearRight");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }
            }

            if (Rand.Next(20) < dmg) {
                this.damageHeadlight("HeadlightRearLeft", dmg);
            }

            if (Rand.Next(20) < dmg) {
                this.damageHeadlight("HeadlightRearRight", dmg);
            }

            if (Rand.Next(20) < dmg) {
                part = this.getPartById("Muffler");
                if (part != null && part.getInventoryItem() != null) {
                    part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
                }
            }
        }
    }

    private void damageHeadlight(String partId, int dmg) {
        if (!this.isDriverGodMode()) {
            VehiclePart part = this.getPartById(partId);
            if (part != null && part.getInventoryItem() != null) {
                part.damage(dmg);
                if (part.getCondition() <= 0) {
                    part.setInventoryItem(null);
                    this.transmitPartItem(part);
                }
            }
        }
    }

    private float clamp(float f1, float min, float max) {
        if (f1 < min) {
            f1 = min;
        }

        if (f1 > max) {
            f1 = max;
        }

        return f1;
    }

    private double getClosestPointOnEdge(float px, float py, float x1, float y1, float x2, float y2, double closestDistSq, Vector2f out) {
        float ox = out.x;
        float oy = out.y;
        double distSq = PZMath.closestPointOnLineSegment(x1, y1, x2, y2, px, py, 0.0, out);
        if (distSq < closestDistSq) {
            return distSq;
        } else {
            out.set(ox, oy);
            return closestDistSq;
        }
    }

    public float getClosestPointOnExtents(float x, float y, Vector2f closest) {
        if (this.getScript() == null) {
            closest.set(x, y);
            return 0.0F;
        } else {
            Vector3f pos = allocVector3f();
            this.getLocalPos(x, y, 0.0F, pos);
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            float xMin = com.x - ext.x / 2.0F;
            float xMax = com.x + ext.x / 2.0F;
            float yMin = com.z - ext.z / 2.0F;
            float yMax = com.z + ext.z / 2.0F;
            float lx = pos.x;
            float ly = pos.z;
            double distSq = Float.MAX_VALUE;
            distSq = this.getClosestPointOnEdge(lx, ly, xMin, yMin, xMax, yMin, distSq, closest);
            distSq = this.getClosestPointOnEdge(lx, ly, xMax, yMin, xMax, yMax, distSq, closest);
            distSq = this.getClosestPointOnEdge(lx, ly, xMin, yMax, xMax, yMax, distSq, closest);
            distSq = this.getClosestPointOnEdge(lx, ly, xMin, yMin, xMin, yMax, distSq, closest);
            this.getWorldPos(closest.x, 0.0F, closest.y, pos);
            closest.x = pos.x;
            closest.y = pos.y;
            releaseVector3f(pos);
            return (float)distSq;
        }
    }

    public float getClosestPointOnPoly(float x, float y, Vector2f closest) {
        if (this.getScript() == null) {
            closest.set(x, y);
            return 0.0F;
        } else {
            VehiclePoly poly1 = this.getPoly();
            double distSq = Float.MAX_VALUE;
            distSq = this.getClosestPointOnEdge(x, y, poly1.x1, poly1.y1, poly1.x2, poly1.y2, distSq, closest);
            distSq = this.getClosestPointOnEdge(x, y, poly1.x2, poly1.y2, poly1.x3, poly1.y3, distSq, closest);
            distSq = this.getClosestPointOnEdge(x, y, poly1.x3, poly1.y3, poly1.x4, poly1.y4, distSq, closest);
            distSq = this.getClosestPointOnEdge(x, y, poly1.x4, poly1.y4, poly1.x1, poly1.y1, distSq, closest);
            return (float)distSq;
        }
    }

    public float getClosestPointOnPoly(BaseVehicle other, Vector2f pointSelf, Vector2f pointOther) {
        if (this.getScript() == null && other.getScript() == null) {
            pointSelf.set(this.getX(), this.getY());
            pointOther.set(other.getX(), other.getY());
            return IsoUtils.DistanceToSquared(pointSelf.x, pointSelf.y, pointOther.x, pointOther.y);
        } else if (this.getScript() == null) {
            pointSelf.set(this.getX(), this.getY());
            return other.getClosestPointOnPoly(this.getX(), this.getY(), pointOther);
        } else if (other.getScript() == null) {
            pointOther.set(other.getX(), other.getY());
            return this.getClosestPointOnPoly(other.getX(), other.getY(), pointSelf);
        } else {
            VehiclePoly poly1 = this.getPoly();
            VehiclePoly poly2 = other.getPoly();
            Vector2[] pts1 = poly1.borders;
            Vector2[] pts2 = poly2.borders;
            Vector2f p1 = allocVector2f();
            Vector2f p2 = allocVector2f();
            double closestDistSq = Double.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                Vector2 pt1 = pts1[i];
                Vector2 pt2 = pts1[(i + 1) % 4];

                for (int j = 0; j < 4; j++) {
                    Vector2 pt3 = pts2[j];
                    Vector2 pt4 = pts2[(j + 1) % 4];
                    double distSq = PZMath.closestPointsOnLineSegments(pt1.x, pt1.y, pt2.x, pt2.y, pt3.x, pt3.y, pt4.x, pt4.y, p1, p2);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        pointSelf.set(p1);
                        pointOther.set(p2);
                    }
                }
            }

            releaseVector2f(p1);
            releaseVector2f(p2);
            return (float)closestDistSq;
        }
    }

    public boolean intersectLineWithExtents(float x1, float y1, float x2, float y2, float adjust, Vector2f intersection) {
        Vector3f p1 = this.getLocalPos(x1, y1, this.getZ(), allocVector3f());
        Vector3f p2 = this.getLocalPos(x2, y2, this.getZ(), allocVector3f());
        float lx1 = p1.x;
        float ly1 = p1.z;
        float lx2 = p2.x;
        float ly2 = p2.z;
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0F - adjust;
        float xMax = com.x + ext.x / 2.0F + adjust;
        float yMin = com.z - ext.z / 2.0F - adjust;
        float yMax = com.z + ext.z / 2.0F + adjust;
        float closestDistSq = Float.MAX_VALUE;
        float closestX = 0.0F;
        float closestY = 0.0F;
        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMin, yMin, xMax, yMin, intersection)) {
            float distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y);
            if (distSq < closestDistSq) {
                closestX = intersection.x;
                closestY = intersection.y;
                closestDistSq = distSq;
            }
        }

        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMax, yMin, xMax, yMax, intersection)) {
            float distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y);
            if (distSq < closestDistSq) {
                closestX = intersection.x;
                closestY = intersection.y;
                closestDistSq = distSq;
            }
        }

        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMin, yMax, xMax, yMax, intersection)) {
            float distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y);
            if (distSq < closestDistSq) {
                closestX = intersection.x;
                closestY = intersection.y;
                closestDistSq = distSq;
            }
        }

        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMin, yMin, xMin, yMax, intersection)) {
            float distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y);
            if (distSq < closestDistSq) {
                closestX = intersection.x;
                closestY = intersection.y;
                closestDistSq = distSq;
            }
        }

        if (closestDistSq < Float.MAX_VALUE) {
            this.getWorldPos(closestX, 0.0F, closestY, p1);
            intersection.set(p1.x, p1.y);
            releaseVector3f(p1);
            releaseVector3f(p2);
            return true;
        } else {
            releaseVector3f(p1);
            releaseVector3f(p2);
            return false;
        }
    }

    public boolean intersectLineWithPoly(float x1, float y1, float x2, float y2, Vector2f intersection) {
        VehiclePoly poly1 = this.getPoly();
        float closestDistSq = Float.MAX_VALUE;
        float closestX = 0.0F;
        float closestY = 0.0F;

        for (int i = 0; i < 4; i++) {
            Vector2 p1 = poly1.borders[i];
            Vector2 p2 = poly1.borders[(i + 1) % 4];
            if (PZMath.intersectLineSegments(x1, y1, x2, y2, p1.x, p1.y, p2.x, p2.y, intersection)) {
                float distSq = IsoUtils.DistanceToSquared(x1, y1, intersection.x, intersection.y);
                if (distSq < closestDistSq) {
                    closestX = intersection.x;
                    closestY = intersection.y;
                    closestDistSq = distSq;
                }
            }
        }

        if (closestDistSq < Float.MAX_VALUE) {
            Vector3f p1 = this.getWorldPos(closestX, 0.0F, closestY, allocVector3f());
            intersection.set(p1.x, p1.y);
            releaseVector3f(p1);
            return true;
        } else {
            return false;
        }
    }

    public boolean isCharacterAdjacentTo(IsoGameCharacter chr) {
        if (PZMath.fastfloor(chr.getZ()) != PZMath.fastfloor(this.getZ())) {
            return false;
        } else {
            Transform xfrm = this.getWorldTransform(allocTransform());
            xfrm.inverse();
            Vector3f circle = TL_vector3f_pool.get().alloc();
            circle.set(chr.getX() - WorldSimulation.instance.offsetX, 0.0F, chr.getY() - WorldSimulation.instance.offsetY);
            xfrm.transform(circle);
            releaseTransform(xfrm);
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            float xMin = com.x - ext.x / 2.0F;
            float xMax = com.x + ext.x / 2.0F;
            float yMin = com.z - ext.z / 2.0F;
            float yMax = com.z + ext.z / 2.0F;
            float ADJACENT_DIST = 0.5F;
            if (circle.x >= xMin - 0.5F && circle.x < xMax + 0.5F && circle.z >= yMin - 0.5F && circle.z < yMax + 0.5F) {
                TL_vector3f_pool.get().release(circle);
                return true;
            } else {
                TL_vector3f_pool.get().release(circle);
                return false;
            }
        }
    }

    public Vector2 testCollisionWithCharacter(IsoGameCharacter chr, float circleRadius, Vector2 out) {
        if (this.physics == null) {
            return null;
        } else {
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            if (this.DistToProper(chr) > Math.max(ext.x / 2.0F, ext.z / 2.0F) + circleRadius + 1.0F) {
                return null;
            } else {
                Vector3f circle = TL_vector3f_pool.get().alloc();
                this.getLocalPos(chr.getNextX(), chr.getNextY(), 0.0F, circle);
                float xMin = com.x - ext.x / 2.0F;
                float xMax = com.x + ext.x / 2.0F;
                float yMin = com.z - ext.z / 2.0F;
                float yMax = com.z + ext.z / 2.0F;
                if (circle.x > xMin && circle.x < xMax && circle.z > yMin && circle.z < yMax) {
                    float dw = circle.x - xMin;
                    float de = xMax - circle.x;
                    float dn = circle.z - yMin;
                    float ds = yMax - circle.z;
                    Vector3f v = TL_vector3f_pool.get().alloc();
                    if (dw < de && dw < dn && dw < ds) {
                        v.set(xMin - circleRadius - 0.015F, 0.0F, circle.z);
                    } else if (de < dw && de < dn && de < ds) {
                        v.set(xMax + circleRadius + 0.015F, 0.0F, circle.z);
                    } else if (dn < dw && dn < de && dn < ds) {
                        v.set(circle.x, 0.0F, yMin - circleRadius - 0.015F);
                    } else if (ds < dw && ds < de && ds < dn) {
                        v.set(circle.x, 0.0F, yMax + circleRadius + 0.015F);
                    }

                    TL_vector3f_pool.get().release(circle);
                    Transform xfrm = this.getWorldTransform(allocTransform());
                    xfrm.origin.set(0.0F, 0.0F, 0.0F);
                    xfrm.transform(v);
                    releaseTransform(xfrm);
                    v.x = v.x + this.getX();
                    v.z = v.z + this.getY();
                    this.collideX = v.x;
                    this.collideY = v.z;
                    out.set(v.x, v.z);
                    TL_vector3f_pool.get().release(v);
                    return out;
                } else {
                    float closestX = this.clamp(circle.x, xMin, xMax);
                    float closestY = this.clamp(circle.z, yMin, yMax);
                    float distanceX = circle.x - closestX;
                    float distanceY = circle.z - closestY;
                    TL_vector3f_pool.get().release(circle);
                    float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                    if (distanceSquared < circleRadius * circleRadius) {
                        if (distanceX == 0.0F && distanceY == 0.0F) {
                            return out.set(-1.0F, -1.0F);
                        } else {
                            Vector3f v = TL_vector3f_pool.get().alloc();
                            v.set(distanceX, 0.0F, distanceY);
                            v.normalize();
                            v.mul(circleRadius + 0.015F);
                            v.x += closestX;
                            v.z += closestY;
                            Transform xfrm = this.getWorldTransform(allocTransform());
                            xfrm.origin.set(0.0F, 0.0F, 0.0F);
                            xfrm.transform(v);
                            releaseTransform(xfrm);
                            v.x = v.x + this.getX();
                            v.z = v.z + this.getY();
                            this.collideX = v.x;
                            this.collideY = v.z;
                            out.set(v.x, v.z);
                            TL_vector3f_pool.get().release(v);
                            return out;
                        }
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    public int testCollisionWithProneCharacter(IsoGameCharacter chr, boolean doSound) {
        Vector2 animVector = chr.getAnimVector(Vector2ObjectPool.get().alloc());
        int result = this.testCollisionWithProneCharacter(chr, animVector.x, animVector.y, doSound);
        Vector2ObjectPool.get().release(animVector);
        return result;
    }

    public int testCollisionWithCorpse(IsoDeadBody body, boolean doSound) {
        float angleX = (float)Math.cos(body.getAngle());
        float angleY = (float)Math.sin(body.getAngle());
        return this.testCollisionWithProneCharacter(body, angleX, angleY, doSound);
    }

    public int testCollisionWithProneCharacter(IsoMovingObject chr, float angleX, float angleY, boolean doSound) {
        if (this.physics == null) {
            return 0;
        } else if (GameServer.server) {
            return 0;
        } else {
            Vector3f ext = this.script.getExtents();
            float circleRadius = 0.3F;
            if (this.DistToProper(chr) > Math.max(ext.x / 2.0F, ext.z / 2.0F) + 0.3F + 1.0F) {
                return 0;
            } else {
                float currentAbsoluteSpeedKmHour = this.getCurrentAbsoluteSpeedKmHour();
                if (currentAbsoluteSpeedKmHour < 3.0F) {
                    return 0;
                } else {
                    float angle = 0.65F;
                    if (chr instanceof IsoDeadBody && chr.getModData() != null && chr.getModData().rawget("corpseLength") != null) {
                    }

                    float headX = chr.getX() + angleX * 0.65F;
                    float headY = chr.getY() + angleY * 0.65F;
                    float feetX = chr.getX() - angleX * 0.65F;
                    float feetY = chr.getY() - angleY * 0.65F;
                    int numWheelsHit = 0;
                    Vector3f worldPos = TL_vector3f_pool.get().alloc();
                    Vector3f wheelPos = TL_vector3f_pool.get().alloc();

                    for (int i = 0; i < this.script.getWheelCount(); i++) {
                        VehicleScript.Wheel scriptWheel = this.script.getWheel(i);
                        boolean onGround = true;

                        for (int j = 0; j < this.models.size(); j++) {
                            BaseVehicle.ModelInfo modelInfo = this.models.get(j);
                            if (modelInfo.wheelIndex == i) {
                                this.getWorldPos(
                                    scriptWheel.offset.x, scriptWheel.offset.y - this.wheelInfo[i].suspensionLength, scriptWheel.offset.z, worldPos
                                );
                                if (worldPos.z > this.script.getWheel(i).radius + 0.05F) {
                                    onGround = false;
                                }
                                break;
                            }
                        }

                        if (onGround) {
                            this.getWorldPos(scriptWheel.offset.x, scriptWheel.offset.y, scriptWheel.offset.z, wheelPos);
                            float x3 = wheelPos.x;
                            float y3 = wheelPos.y;
                            double u = ((x3 - feetX) * (headX - feetX) + (y3 - feetY) * (headY - feetY))
                                / (Math.pow(headX - feetX, 2.0) + Math.pow(headY - feetY, 2.0));
                            float closestX;
                            float closestY;
                            if (u <= 0.0) {
                                closestX = feetX;
                                closestY = feetY;
                            } else if (u >= 1.0) {
                                closestX = headX;
                                closestY = headY;
                            } else {
                                closestX = feetX + (headX - feetX) * (float)u;
                                closestY = feetY + (headY - feetY) * (float)u;
                            }

                            if (!(IsoUtils.DistanceToSquared(wheelPos.x, wheelPos.y, closestX, closestY) > scriptWheel.radius * scriptWheel.radius)) {
                                if (doSound && currentAbsoluteSpeedKmHour > 10.0F) {
                                    if (GameServer.server && chr instanceof IsoZombie isoZombie) {
                                        isoZombie.setThumpFlag(1);
                                    } else {
                                        SoundManager.instance.PlayWorldSound("VehicleRunOverBody", chr.getCurrentSquare(), 0.0F, 20.0F, 0.9F, true);
                                    }

                                    doSound = false;
                                }

                                if (i < this.impulseFromSquishedZombie.length) {
                                    if (this.impulseFromSquishedZombie[i] == null) {
                                        this.impulseFromSquishedZombie[i] = new BaseVehicle.VehicleImpulse();
                                    }

                                    this.impulseFromSquishedZombie[i].impulse.set(0.0F, 1.0F, 0.0F);
                                    float speedMult = Math.max(currentAbsoluteSpeedKmHour, 10.0F) / 10.0F;
                                    float corpseSizeMul = 1.0F;
                                    if (chr instanceof IsoDeadBody && chr.getModData() != null && chr.getModData().rawget("corpseSize") != null) {
                                        corpseSizeMul = ((KahluaTableImpl)chr.getModData()).rawgetFloat("corpseSize");
                                    }

                                    this.impulseFromSquishedZombie[i].impulse.mul(0.065F * this.getFudgedMass() * speedMult * corpseSizeMul);
                                    this.impulseFromSquishedZombie[i].relPos.set(wheelPos.x - this.getX(), 0.0F, wheelPos.y - this.getY());
                                    this.impulseFromSquishedZombie[i].enable = true;
                                    this.impulseFromSquishedZombie[i].applied = false;
                                    numWheelsHit++;
                                }
                            }
                        }
                    }

                    TL_vector3f_pool.get().release(worldPos);
                    TL_vector3f_pool.get().release(wheelPos);
                    return numWheelsHit;
                }
            }
        }
    }

    public Vector2 testCollisionWithObject(IsoObject obj, float circleRadius, Vector2 out) {
        if (this.physics == null) {
            return null;
        } else if (obj.square == null) {
            return null;
        } else {
            float objX = this.getObjectX(obj);
            float objY = this.getObjectY(obj);
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            float selfRadius = Math.max(ext.x / 2.0F, ext.z / 2.0F) + circleRadius + 1.0F;
            if (this.DistToSquared(objX, objY) > selfRadius * selfRadius) {
                return null;
            } else {
                Vector3f circle = TL_vector3f_pool.get().alloc();
                this.getLocalPos(objX, objY, 0.0F, circle);
                float xMin = com.x - ext.x / 2.0F;
                float xMax = com.x + ext.x / 2.0F;
                float yMin = com.z - ext.z / 2.0F;
                float yMax = com.z + ext.z / 2.0F;
                if (circle.x > xMin && circle.x < xMax && circle.z > yMin && circle.z < yMax) {
                    float dw = circle.x - xMin;
                    float de = xMax - circle.x;
                    float dn = circle.z - yMin;
                    float ds = yMax - circle.z;
                    Vector3f v = TL_vector3f_pool.get().alloc();
                    if (dw < de && dw < dn && dw < ds) {
                        v.set(xMin - circleRadius - 0.015F, 0.0F, circle.z);
                    } else if (de < dw && de < dn && de < ds) {
                        v.set(xMax + circleRadius + 0.015F, 0.0F, circle.z);
                    } else if (dn < dw && dn < de && dn < ds) {
                        v.set(circle.x, 0.0F, yMin - circleRadius - 0.015F);
                    } else if (ds < dw && ds < de && ds < dn) {
                        v.set(circle.x, 0.0F, yMax + circleRadius + 0.015F);
                    }

                    TL_vector3f_pool.get().release(circle);
                    Transform xfrm = this.getWorldTransform(allocTransform());
                    xfrm.origin.set(0.0F, 0.0F, 0.0F);
                    xfrm.transform(v);
                    releaseTransform(xfrm);
                    v.x = v.x + this.getX();
                    v.z = v.z + this.getY();
                    this.collideX = v.x;
                    this.collideY = v.z;
                    out.set(v.x, v.z);
                    TL_vector3f_pool.get().release(v);
                    return out;
                } else {
                    float closestX = this.clamp(circle.x, xMin, xMax);
                    float closestY = this.clamp(circle.z, yMin, yMax);
                    float distanceX = circle.x - closestX;
                    float distanceY = circle.z - closestY;
                    TL_vector3f_pool.get().release(circle);
                    float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                    if (distanceSquared < circleRadius * circleRadius) {
                        if (distanceX == 0.0F && distanceY == 0.0F) {
                            return out.set(-1.0F, -1.0F);
                        } else {
                            Vector3f v = TL_vector3f_pool.get().alloc();
                            v.set(distanceX, 0.0F, distanceY);
                            v.normalize();
                            v.mul(circleRadius + 0.015F);
                            v.x += closestX;
                            v.z += closestY;
                            Transform xfrm = this.getWorldTransform(allocTransform());
                            xfrm.origin.set(0.0F, 0.0F, 0.0F);
                            xfrm.transform(v);
                            releaseTransform(xfrm);
                            v.x = v.x + this.getX();
                            v.z = v.z + this.getY();
                            this.collideX = v.x;
                            this.collideY = v.z;
                            out.set(v.x, v.z);
                            TL_vector3f_pool.get().release(v);
                            return out;
                        }
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    public boolean testCollisionWithVehicle(BaseVehicle obj) {
        VehicleScript thisScript = this.script;
        if (thisScript == null) {
            thisScript = ScriptManager.instance.getVehicle(this.scriptName);
        }

        VehicleScript objScript = obj.script;
        if (objScript == null) {
            objScript = ScriptManager.instance.getVehicle(obj.scriptName);
        }

        if (thisScript != null && objScript != null) {
            Vector2[] testVecs1 = BaseVehicle.L_testCollisionWithVehicle.testVecs1;
            Vector2[] testVecs2 = BaseVehicle.L_testCollisionWithVehicle.testVecs2;
            if (testVecs1[0] == null) {
                for (int i = 0; i < testVecs1.length; i++) {
                    testVecs1[i] = new Vector2();
                    testVecs2[i] = new Vector2();
                }
            }

            Vector3f extThis = thisScript.getExtents();
            Vector3f comThis = thisScript.getCenterOfMassOffset();
            Vector3f extOther = objScript.getExtents();
            Vector3f comOther = objScript.getCenterOfMassOffset();
            Vector3f worldPos = BaseVehicle.L_testCollisionWithVehicle.worldPos;
            float scale = 0.5F;
            this.getWorldPos(comThis.x + extThis.x * 0.5F, 0.0F, comThis.z + extThis.z * 0.5F, worldPos, thisScript);
            testVecs1[0].set(worldPos.x, worldPos.y);
            this.getWorldPos(comThis.x - extThis.x * 0.5F, 0.0F, comThis.z + extThis.z * 0.5F, worldPos, thisScript);
            testVecs1[1].set(worldPos.x, worldPos.y);
            this.getWorldPos(comThis.x - extThis.x * 0.5F, 0.0F, comThis.z - extThis.z * 0.5F, worldPos, thisScript);
            testVecs1[2].set(worldPos.x, worldPos.y);
            this.getWorldPos(comThis.x + extThis.x * 0.5F, 0.0F, comThis.z - extThis.z * 0.5F, worldPos, thisScript);
            testVecs1[3].set(worldPos.x, worldPos.y);
            obj.getWorldPos(comOther.x + extOther.x * 0.5F, 0.0F, comOther.z + extOther.z * 0.5F, worldPos, objScript);
            testVecs2[0].set(worldPos.x, worldPos.y);
            obj.getWorldPos(comOther.x - extOther.x * 0.5F, 0.0F, comOther.z + extOther.z * 0.5F, worldPos, objScript);
            testVecs2[1].set(worldPos.x, worldPos.y);
            obj.getWorldPos(comOther.x - extOther.x * 0.5F, 0.0F, comOther.z - extOther.z * 0.5F, worldPos, objScript);
            testVecs2[2].set(worldPos.x, worldPos.y);
            obj.getWorldPos(comOther.x + extOther.x * 0.5F, 0.0F, comOther.z - extOther.z * 0.5F, worldPos, objScript);
            testVecs2[3].set(worldPos.x, worldPos.y);
            return QuadranglesIntersection.IsQuadranglesAreIntersected(testVecs1, testVecs2);
        } else {
            return false;
        }
    }

    private float getObjectX(IsoObject obj) {
        return obj instanceof IsoMovingObject ? obj.getX() : obj.getSquare().getX() + 0.5F;
    }

    private float getObjectY(IsoObject obj) {
        return obj instanceof IsoMovingObject ? obj.getY() : obj.getSquare().getY() + 0.5F;
    }

    public void ApplyImpulse(IsoObject obj, float mul) {
        float objX = this.getObjectX(obj);
        float objY = this.getObjectY(obj);
        BaseVehicle.VehicleImpulse impulse = BaseVehicle.VehicleImpulse.alloc();
        impulse.impulse.set(this.getX() - objX, 0.0F, this.getY() - objY);
        impulse.impulse.normalize();
        impulse.impulse.mul(mul);
        impulse.relPos.set(objX - this.getX(), 0.0F, objY - this.getY());
        this.impulseFromHitZombie.add(impulse);
        this.setPhysicsActive(true);
    }

    public void ApplyImpulse4Break(IsoObject obj, float mul) {
        float objX = this.getObjectX(obj);
        float objY = this.getObjectY(obj);
        BaseVehicle.VehicleImpulse impulse = BaseVehicle.VehicleImpulse.alloc();
        this.getLinearVelocity(impulse.impulse);
        impulse.impulse.mul(-mul * this.getFudgedMass());
        impulse.relPos.set(objX - this.getX(), 0.0F, objY - this.getY());
        this.impulseFromHitZombie.add(impulse);
        this.setPhysicsActive(true);
    }

    public void ApplyImpulse(float fromX, float fromY, float fromZ, float impulseDirX, float impulseDirY, float impulseDirZ, float impulseStrength) {
        BaseVehicle.VehicleImpulse impulse = BaseVehicle.VehicleImpulse.alloc();
        impulse.impulse.set(impulseDirX, impulseDirZ, impulseDirY);
        impulse.impulse.normalize();
        impulse.impulse.mul(impulseStrength);
        impulse.relPos.set(fromX - this.getX(), fromZ - this.getZ(), fromY - this.getY());
        this.impulseFromHitZombie.add(impulse);
        this.setPhysicsActive(true);
        DebugType.General
            .println(
                "ApplyImpulse! from(%f, %f, %f). relPos(%f, %f, %f). isActive: %s, isStatic: %s",
                fromX,
                fromY,
                fromZ,
                impulse.relPos.x,
                impulse.relPos.y,
                impulse.relPos.z,
                this.isActive ? "true" : "false",
                this.isStatic ? "true" : "false"
            );
    }

    public void hitCharacter(IsoZombie chr) {
        IsoPlayer chrPlayer = Type.tryCastTo(chr, IsoPlayer.class);
        IsoZombie chrZombie = Type.tryCastTo(chr, IsoZombie.class);
        if (chr.getCurrentState() != StaggerBackState.instance() && chr.getCurrentState() != ZombieFallDownState.instance()) {
            if (!(Math.abs(chr.getX() - this.getX()) < 0.01F) && !(Math.abs(chr.getY() - this.getY()) < 0.01F)) {
                float SPEED_CAP = 15.0F;
                Vector3f velocity = this.getLinearVelocity(TL_vector3f_pool.get().alloc());
                velocity.y = 0.0F;
                float speed = velocity.length();
                speed = Math.min(speed, 15.0F);
                if (speed < 0.05F) {
                    TL_vector3f_pool.get().release(velocity);
                } else {
                    Vector3f impulse = TL_vector3f_pool.get().alloc();
                    impulse.set(this.getX() - chr.getX(), 0.0F, this.getY() - chr.getY());
                    impulse.normalize();
                    velocity.normalize();
                    float dot = velocity.dot(impulse);
                    TL_vector3f_pool.get().release(velocity);
                    if (dot < 0.0F && !GameServer.server) {
                        this.ApplyImpulse(chr, this.getFudgedMass() * 7.0F * speed / 15.0F * Math.abs(dot));
                    }

                    impulse.normalize();
                    impulse.mul(3.0F * speed / 15.0F);
                    Vector2 vector2 = Vector2ObjectPool.get().alloc();
                    float hitSpeed = speed + this.physics.clientForce / this.getFudgedMass();
                    if (chrPlayer != null) {
                        chrPlayer.setVehicleHitLocation(this);
                    } else if (chrZombie != null) {
                        chrZombie.setVehicleHitLocation(this);
                    }

                    BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(chr.getX(), chr.getY(), chr.getZ());
                    long soundRef = emitter.playSound("VehicleHitCharacter");
                    emitter.setParameterValue(
                        soundRef,
                        FMODManager.instance.getParameterDescription("VehicleHitLocation"),
                        ParameterVehicleHitLocation.calculateLocation(this, chr.getX(), chr.getY(), chr.getZ()).getValue()
                    );
                    emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleSpeed"), this.getCurrentSpeedKmHour());
                    chr.Hit(this, hitSpeed, dot > 0.0F, vector2.set(-impulse.x, -impulse.z));
                    IsoPlayer driver = Type.tryCastTo(this.getDriverRegardlessOfTow(), IsoPlayer.class);
                    if (driver != null && driver.isLocalPlayer()) {
                        driver.triggerMusicIntensityEvent("VehicleHitCharacter");
                    }

                    Vector2ObjectPool.get().release(vector2);
                    TL_vector3f_pool.get().release(impulse);
                    long currentMS = System.currentTimeMillis();
                    long diff = (currentMS - this.zombieHitTimestamp) / 1000L;
                    this.zombiesHits = Math.max(this.zombiesHits - (int)diff, 0);
                    if (currentMS - this.zombieHitTimestamp > 700L) {
                        this.zombieHitTimestamp = currentMS;
                        this.zombiesHits++;
                        this.zombiesHits = Math.min(this.zombiesHits, 20);
                    }

                    if (speed >= 5.0F || this.zombiesHits > 10) {
                        Vector3f pos = TL_vector3f_pool.get().alloc();
                        this.getLocalPos(chr.getX(), chr.getY(), chr.getZ(), pos);
                        if (pos.z > 0.0F) {
                            int dmg = this.caclulateDamageWithBodies(true);
                            this.addDamageFrontHitAChr(dmg);
                        } else {
                            int dmg = this.caclulateDamageWithBodies(false);
                            this.addDamageRearHitAChr(dmg);
                        }

                        TL_vector3f_pool.get().release(pos);
                    }
                }
            }
        }
    }

    public void hitCharacter(IsoAnimal chr) {
        IsoPlayer chrPlayer = Type.tryCastTo(chr, IsoPlayer.class);
        IsoAnimal chrAnimal = Type.tryCastTo(chr, IsoAnimal.class);
        if (chr.getCurrentState() != AnimalFalldownState.instance()) {
            if (!(Math.abs(chr.getX() - this.getX()) < 0.01F) && !(Math.abs(chr.getY() - this.getY()) < 0.01F)) {
                float SPEED_CAP = 15.0F;
                Vector3f velocity = this.getLinearVelocity(TL_vector3f_pool.get().alloc());
                velocity.y = 0.0F;
                float speed = velocity.length();
                speed = Math.min(speed, 15.0F);
                if (speed < 0.05F) {
                    TL_vector3f_pool.get().release(velocity);
                } else {
                    Vector3f impulse = TL_vector3f_pool.get().alloc();
                    impulse.set(this.getX() - chr.getX(), 0.0F, this.getY() - chr.getY());
                    impulse.normalize();
                    velocity.normalize();
                    float dot = velocity.dot(impulse);
                    TL_vector3f_pool.get().release(velocity);
                    if (dot < 0.0F && !GameServer.server) {
                        this.ApplyImpulse(chr, this.getFudgedMass() * 7.0F * speed / 15.0F * Math.abs(dot));
                    }

                    impulse.normalize();
                    impulse.mul(3.0F * speed / 15.0F);
                    Vector2 vector2 = Vector2ObjectPool.get().alloc();
                    float hitSpeed = speed + this.physics.clientForce / this.getFudgedMass();
                    if (chrPlayer != null) {
                        chrPlayer.setVehicleHitLocation(this);
                    } else if (chrAnimal != null) {
                        chrAnimal.setVehicleHitLocation(this);
                    }

                    BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(chr.getX(), chr.getY(), chr.getZ());
                    long soundRef = emitter.playSound("VehicleHitCharacter");
                    emitter.setParameterValue(
                        soundRef,
                        FMODManager.instance.getParameterDescription("VehicleHitLocation"),
                        ParameterVehicleHitLocation.calculateLocation(this, chr.getX(), chr.getY(), chr.getZ()).getValue()
                    );
                    emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleSpeed"), this.getCurrentSpeedKmHour());
                    chr.Hit(this, hitSpeed, dot > 0.0F, vector2.set(-impulse.x, -impulse.z));
                    IsoPlayer driver = Type.tryCastTo(this.getDriverRegardlessOfTow(), IsoPlayer.class);
                    if (driver != null && driver.isLocalPlayer()) {
                        driver.triggerMusicIntensityEvent("VehicleHitCharacter");
                    }

                    Vector2ObjectPool.get().release(vector2);
                    TL_vector3f_pool.get().release(impulse);
                    long currentMS = System.currentTimeMillis();
                    long diff = (currentMS - this.zombieHitTimestamp) / 1000L;
                    this.zombiesHits = Math.max(this.zombiesHits - (int)diff, 0);
                    if (currentMS - this.zombieHitTimestamp > 700L) {
                        this.zombieHitTimestamp = currentMS;
                        this.zombiesHits++;
                        this.zombiesHits = Math.min(this.zombiesHits, 20);
                    }

                    if (speed >= 5.0F || this.zombiesHits > 10) {
                        speed = this.getCurrentSpeedKmHour() / 5.0F;
                        Vector3f pos = TL_vector3f_pool.get().alloc();
                        this.getLocalPos(chr.getX(), chr.getY(), chr.getZ(), pos);
                        if (pos.z > 0.0F) {
                            int dmg = this.caclulateDamageWithBodies(true);
                            this.addDamageFrontHitAChr(dmg);
                        } else {
                            int dmg = this.caclulateDamageWithBodies(false);
                            this.addDamageRearHitAChr(dmg);
                        }

                        TL_vector3f_pool.get().release(pos);
                    }
                }
            }
        }
    }

    private int caclulateDamageWithBodies(boolean isFront) {
        boolean movingForward = this.getCurrentSpeedKmHour() > 0.0F;
        float currentAbsoluteSpeedKmHour = this.getCurrentAbsoluteSpeedKmHour();
        float multi = currentAbsoluteSpeedKmHour / 160.0F;
        multi = PZMath.clamp(multi * multi, 0.0F, 1.0F);
        float dmg = 60.0F * multi;
        float hits = PZMath.max(1.0F, this.zombiesHits / 3.0F);
        if (!isFront && !movingForward) {
            hits = 1.0F;
        }

        if (this.zombiesHits > 10 && dmg < currentAbsoluteSpeedKmHour / 5.0F) {
            dmg = currentAbsoluteSpeedKmHour / 5.0F;
        }

        return (int)(hits * dmg);
    }

    public int calculateDamageWithCharacter(IsoGameCharacter chr) {
        Vector3f pos = TL_vector3f_pool.get().alloc();
        this.getLocalPos(chr.getX(), chr.getY(), chr.getZ(), pos);
        int dmg;
        if (pos.z > 0.0F) {
            dmg = this.caclulateDamageWithBodies(true);
        } else {
            dmg = -1 * this.caclulateDamageWithBodies(false);
        }

        TL_vector3f_pool.get().release(pos);
        return dmg;
    }

    public boolean blocked(int x, int y, int z) {
        if (this.removedFromWorld || this.current == null) {
            return false;
        } else if (this.getController() == null) {
            return false;
        } else if (z != PZMath.fastfloor(this.getZ())) {
            return false;
        } else if (IsoUtils.DistanceTo2D(x + 0.5F, y + 0.5F, this.getX(), this.getY()) > 5.0F) {
            return false;
        } else {
            float circleRadius = 0.3F;
            Transform xfrm = allocTransform();
            this.getWorldTransform(xfrm);
            xfrm.inverse();
            Vector3f circle = TL_vector3f_pool.get().alloc();
            circle.set(x + 0.5F - WorldSimulation.instance.offsetX, 0.0F, y + 0.5F - WorldSimulation.instance.offsetY);
            xfrm.transform(circle);
            releaseTransform(xfrm);
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            float closestX = this.clamp(circle.x, com.x - ext.x / 2.0F, com.x + ext.x / 2.0F);
            float closestY = this.clamp(circle.z, com.z - ext.z / 2.0F, com.z + ext.z / 2.0F);
            float distanceX = circle.x - closestX;
            float distanceY = circle.z - closestY;
            TL_vector3f_pool.get().release(circle);
            float distanceSquared = distanceX * distanceX + distanceY * distanceY;
            return distanceSquared < 0.09F;
        }
    }

    public boolean isIntersectingSquare(int x, int y, int z) {
        if (z != PZMath.fastfloor(this.getZ())) {
            return false;
        } else if (!this.removedFromWorld && this.current != null && this.getController() != null) {
            tempPoly.x1 = tempPoly.x4 = x;
            tempPoly.y1 = tempPoly.y2 = y;
            tempPoly.x2 = tempPoly.x3 = x + 1.0F;
            tempPoly.y3 = tempPoly.y4 = y + 1.0F;
            return PolyPolyIntersect.intersects(tempPoly, this.getPoly());
        } else {
            return false;
        }
    }

    public boolean isIntersectingSquare(IsoGridSquare sq) {
        return this.isIntersectingSquare(sq.getX(), sq.getY(), sq.getZ());
    }

    public boolean isIntersectingSquareWithShadow(int x, int y, int z) {
        if (z != PZMath.fastfloor(this.getZ())) {
            return false;
        } else if (!this.removedFromWorld && this.current != null && this.getController() != null) {
            tempPoly.x1 = tempPoly.x4 = x;
            tempPoly.y1 = tempPoly.y2 = y;
            tempPoly.x2 = tempPoly.x3 = x + 1.0F;
            tempPoly.y3 = tempPoly.y4 = y + 1.0F;
            return PolyPolyIntersect.intersects(tempPoly, this.shadowCoord);
        } else {
            return false;
        }
    }

    public boolean circleIntersects(float x, float y, float z, float radius) {
        if (this.getController() == null) {
            return false;
        } else if (PZMath.fastfloor(z) != PZMath.fastfloor(this.getZ())) {
            return false;
        } else if (IsoUtils.DistanceTo2D(x, y, this.getX(), this.getY()) > 5.0F) {
            return false;
        } else {
            Vector3f ext = this.script.getExtents();
            Vector3f com = this.script.getCenterOfMassOffset();
            Vector3f circle = TL_vector3f_pool.get().alloc();
            this.getLocalPos(x, y, z, circle);
            float xMin = com.x - ext.x / 2.0F;
            float xMax = com.x + ext.x / 2.0F;
            float yMin = com.z - ext.z / 2.0F;
            float yMax = com.z + ext.z / 2.0F;
            if (circle.x > xMin && circle.x < xMax && circle.z > yMin && circle.z < yMax) {
                return true;
            } else {
                float closestX = this.clamp(circle.x, xMin, xMax);
                float closestY = this.clamp(circle.z, yMin, yMax);
                float distanceX = circle.x - closestX;
                float distanceY = circle.z - closestY;
                TL_vector3f_pool.get().release(circle);
                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                return distanceSquared < radius * radius;
            }
        }
    }

    public void updateLights() {
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        inst.textureRustA = this.rust;
        if (this.script.getWheelCount() == 0) {
            inst.textureRustA = 0.0F;
        }

        inst.painColor.x = this.colorHue;
        inst.painColor.y = this.colorSaturation;
        inst.painColor.z = this.colorValue;
        boolean windowFront = false;
        boolean windowRear = false;
        boolean windowFrontLeft = false;
        boolean windowMiddleLeft = false;
        boolean windowRearLeft = false;
        boolean windowFrontRight = false;
        boolean windowMiddleRight = false;
        boolean windowRearRight = false;
        if (this.windowLightsOn) {
            VehiclePart part = this.getPartById("Windshield");
            windowFront = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindshieldRear");
            windowRear = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowFrontLeft");
            windowFrontLeft = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowMiddleLeft");
            windowMiddleLeft = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowRearLeft");
            windowRearLeft = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowFrontRight");
            windowFrontRight = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowMiddleRight");
            windowMiddleRight = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowRearRight");
            windowRearRight = part != null && part.getInventoryItem() != null;
        }

        inst.textureLightsEnables1[10] = windowFront ? 1.0F : 0.0F;
        inst.textureLightsEnables1[14] = windowRear ? 1.0F : 0.0F;
        inst.textureLightsEnables1[2] = windowFrontLeft ? 1.0F : 0.0F;
        inst.textureLightsEnables1[6] = windowMiddleLeft | windowRearLeft ? 1.0F : 0.0F;
        inst.textureLightsEnables1[9] = windowFrontRight ? 1.0F : 0.0F;
        inst.textureLightsEnables1[13] = windowMiddleRight | windowRearRight ? 1.0F : 0.0F;
        boolean HeadlightLeftSet = false;
        boolean HeadlightRightSet = false;
        boolean HeadlightRearRightSet = false;
        boolean HeadlightRearLeftSet = false;
        if (this.headlightsOn && this.getBatteryCharge() > 0.0F) {
            VehiclePart part = this.getPartById("HeadlightLeft");
            if (part != null && part.getInventoryItem() != null) {
                HeadlightLeftSet = true;
            }

            part = this.getPartById("HeadlightRight");
            if (part != null && part.getInventoryItem() != null) {
                HeadlightRightSet = true;
            }

            part = this.getPartById("HeadlightRearLeft");
            if (part != null && part.getInventoryItem() != null) {
                HeadlightRearLeftSet = true;
            }

            part = this.getPartById("HeadlightRearRight");
            if (part != null && part.getInventoryItem() != null) {
                HeadlightRearRightSet = true;
            }
        }

        inst.textureLightsEnables2[4] = HeadlightRightSet ? 1.0F : 0.0F;
        inst.textureLightsEnables2[8] = HeadlightLeftSet ? 1.0F : 0.0F;
        inst.textureLightsEnables2[12] = HeadlightRearRightSet ? 1.0F : 0.0F;
        inst.textureLightsEnables2[1] = HeadlightRearLeftSet ? 1.0F : 0.0F;
        boolean stoplightsOn = this.stoplightsOn && this.getBatteryCharge() > 0.0F;
        if (this.scriptName.contains("Trailer")
            && this.vehicleTowedBy != null
            && this.vehicleTowedBy.stoplightsOn
            && this.vehicleTowedBy.getBatteryCharge() > 0.0F) {
            stoplightsOn = true;
        }

        if (stoplightsOn) {
            inst.textureLightsEnables2[5] = 1.0F;
            inst.textureLightsEnables2[9] = 1.0F;
        } else {
            inst.textureLightsEnables2[5] = 0.0F;
            inst.textureLightsEnables2[9] = 0.0F;
        }

        if (this.script.getLightbar().enable) {
            if (this.lightbarLightsMode.isEnable() && this.getBatteryCharge() > 0.0F) {
                switch (this.lightbarLightsMode.getLightTexIndex()) {
                    case 0:
                        inst.textureLightsEnables2[13] = 0.0F;
                        inst.textureLightsEnables2[2] = 0.0F;
                        break;
                    case 1:
                        inst.textureLightsEnables2[13] = 0.0F;
                        inst.textureLightsEnables2[2] = 1.0F;
                        break;
                    case 2:
                        inst.textureLightsEnables2[13] = 1.0F;
                        inst.textureLightsEnables2[2] = 0.0F;
                        break;
                    default:
                        inst.textureLightsEnables2[13] = 0.0F;
                        inst.textureLightsEnables2[2] = 0.0F;
                }
            } else {
                inst.textureLightsEnables2[13] = 0.0F;
                inst.textureLightsEnables2[2] = 0.0F;
            }
        }

        if (DebugOptions.instance.vehicleCycleColor.getValue()) {
            float c = (float)(System.currentTimeMillis() % 2000L);
            float c2 = (float)(System.currentTimeMillis() % 7000L);
            float c3 = (float)(System.currentTimeMillis() % 11000L);
            inst.painColor.x = c / 2000.0F;
            inst.painColor.y = c2 / 7000.0F;
            inst.painColor.z = c3 / 11000.0F;
        }

        if (DebugOptions.instance.vehicleRenderBlood0.getValue()) {
            Arrays.fill(inst.matrixBlood1Enables1, 0.0F);
            Arrays.fill(inst.matrixBlood1Enables2, 0.0F);
            Arrays.fill(inst.matrixBlood2Enables1, 0.0F);
            Arrays.fill(inst.matrixBlood2Enables2, 0.0F);
        }

        if (DebugOptions.instance.vehicleRenderBlood50.getValue()) {
            Arrays.fill(inst.matrixBlood1Enables1, 0.5F);
            Arrays.fill(inst.matrixBlood1Enables2, 0.5F);
            Arrays.fill(inst.matrixBlood2Enables1, 1.0F);
            Arrays.fill(inst.matrixBlood2Enables2, 1.0F);
        }

        if (DebugOptions.instance.vehicleRenderBlood100.getValue()) {
            Arrays.fill(inst.matrixBlood1Enables1, 1.0F);
            Arrays.fill(inst.matrixBlood1Enables2, 1.0F);
            Arrays.fill(inst.matrixBlood2Enables1, 1.0F);
            Arrays.fill(inst.matrixBlood2Enables2, 1.0F);
        }

        if (DebugOptions.instance.vehicleRenderDamage0.getValue()) {
            Arrays.fill(inst.textureDamage1Enables1, 0.0F);
            Arrays.fill(inst.textureDamage1Enables2, 0.0F);
            Arrays.fill(inst.textureDamage2Enables1, 0.0F);
            Arrays.fill(inst.textureDamage2Enables2, 0.0F);
        }

        if (DebugOptions.instance.vehicleRenderDamage1.getValue()) {
            Arrays.fill(inst.textureDamage1Enables1, 1.0F);
            Arrays.fill(inst.textureDamage1Enables2, 1.0F);
            Arrays.fill(inst.textureDamage2Enables1, 0.0F);
            Arrays.fill(inst.textureDamage2Enables2, 0.0F);
        }

        if (DebugOptions.instance.vehicleRenderDamage2.getValue()) {
            Arrays.fill(inst.textureDamage1Enables1, 0.0F);
            Arrays.fill(inst.textureDamage1Enables2, 0.0F);
            Arrays.fill(inst.textureDamage2Enables1, 1.0F);
            Arrays.fill(inst.textureDamage2Enables2, 1.0F);
        }

        if (DebugOptions.instance.vehicleRenderRust0.getValue()) {
            inst.textureRustA = 0.0F;
        }

        if (DebugOptions.instance.vehicleRenderRust50.getValue()) {
            inst.textureRustA = 0.5F;
        }

        if (DebugOptions.instance.vehicleRenderRust100.getValue()) {
            inst.textureRustA = 1.0F;
        }

        inst.refBody = 0.3F;
        inst.refWindows = 0.4F;
        if (this.rust > 0.8F) {
            inst.refBody = 0.1F;
            inst.refWindows = 0.2F;
        }
    }

    private void updateWorldLights() {
        if (!this.script.getLightbar().enable) {
            this.removeWorldLights();
        } else if (!this.lightbarLightsMode.isEnable() || this.getBatteryCharge() <= 0.0F) {
            this.removeWorldLights();
        } else if (this.lightbarLightsMode.getLightTexIndex() == 0) {
            this.removeWorldLights();
        } else {
            this.leftLight1.radius = this.leftLight2.radius = this.rightLight1.radius = this.rightLight2.radius = 8;
            if (this.lightbarLightsMode.getLightTexIndex() == 1) {
                Vector3f pos = this.getWorldPos(0.4F, 0.0F, 0.0F, TL_vector3f_pool.get().alloc());
                int lx = PZMath.fastfloor(pos.x);
                int ly = PZMath.fastfloor(pos.y);
                int lz = PZMath.fastfloor(this.getZ());
                TL_vector3f_pool.get().release(pos);
                int oldLeftLightIndex = this.leftLightIndex;
                if (oldLeftLightIndex == 1 && this.leftLight1.x == lx && this.leftLight1.y == ly && this.leftLight1.z == lz) {
                    return;
                }

                if (oldLeftLightIndex == 2 && this.leftLight2.x == lx && this.leftLight2.y == ly && this.leftLight2.z == lz) {
                    return;
                }

                this.removeWorldLights();
                IsoLightSource light;
                if (oldLeftLightIndex == 1) {
                    light = this.leftLight2;
                    this.leftLightIndex = 2;
                } else {
                    light = this.leftLight1;
                    this.leftLightIndex = 1;
                }

                light.life = -1;
                light.x = lx;
                light.y = ly;
                light.z = lz;
                IsoWorld.instance.currentCell.addLamppost(light);
            } else {
                Vector3f posx = this.getWorldPos(-0.4F, 0.0F, 0.0F, TL_vector3f_pool.get().alloc());
                int lxx = PZMath.fastfloor(posx.x);
                int lyx = PZMath.fastfloor(posx.y);
                int lzx = PZMath.fastfloor(this.getZ());
                TL_vector3f_pool.get().release(posx);
                int oldRightLightIndex = this.rightLightIndex;
                if (oldRightLightIndex == 1 && this.rightLight1.x == lxx && this.rightLight1.y == lyx && this.rightLight1.z == lzx) {
                    return;
                }

                if (oldRightLightIndex == 2 && this.rightLight2.x == lxx && this.rightLight2.y == lyx && this.rightLight2.z == lzx) {
                    return;
                }

                this.removeWorldLights();
                IsoLightSource light;
                if (oldRightLightIndex == 1) {
                    light = this.rightLight2;
                    this.rightLightIndex = 2;
                } else {
                    light = this.rightLight1;
                    this.rightLightIndex = 1;
                }

                light.life = -1;
                light.x = lxx;
                light.y = lyx;
                light.z = lzx;
                IsoWorld.instance.currentCell.addLamppost(light);
            }
        }
    }

    public void fixLightbarModelLighting(IsoLightSource ls, Vector3f lightPos) {
        if (ls == this.leftLight1 || ls == this.leftLight2) {
            lightPos.set(1.0F, 0.0F, 0.0F);
        } else if (ls == this.rightLight1 || ls == this.rightLight2) {
            lightPos.set(-1.0F, 0.0F, 0.0F);
        }
    }

    private void removeWorldLights() {
        if (this.leftLightIndex == 1) {
            IsoWorld.instance.currentCell.removeLamppost(this.leftLight1);
            this.leftLightIndex = -1;
        }

        if (this.leftLightIndex == 2) {
            IsoWorld.instance.currentCell.removeLamppost(this.leftLight2);
            this.leftLightIndex = -1;
        }

        if (this.rightLightIndex == 1) {
            IsoWorld.instance.currentCell.removeLamppost(this.rightLight1);
            this.rightLightIndex = -1;
        }

        if (this.rightLightIndex == 2) {
            IsoWorld.instance.currentCell.removeLamppost(this.rightLight2);
            this.rightLightIndex = -1;
        }
    }

    public void doDamageOverlay() {
        if (this.sprite.modelSlot != null) {
            this.doDoorDamage();
            this.doWindowDamage();
            this.doOtherBodyWorkDamage();
            this.doBloodOverlay();
        }
    }

    private void checkDamage(VehiclePart part, int matrixName, boolean doBlack) {
        if (doBlack && part != null && part.getId().startsWith("Window") && part.getScriptModelById("Default") != null) {
            doBlack = false;
        }

        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;

        try {
            inst.textureDamage1Enables1[matrixName] = 0.0F;
            inst.textureDamage2Enables1[matrixName] = 0.0F;
            inst.textureUninstall1[matrixName] = 0.0F;
            if (part != null && part.getInventoryItem() != null) {
                if (part.<InventoryItem>getInventoryItem().getCondition() < 60 && part.<InventoryItem>getInventoryItem().getCondition() >= 40) {
                    inst.textureDamage1Enables1[matrixName] = 1.0F;
                }

                if (part.<InventoryItem>getInventoryItem().getCondition() < 40) {
                    inst.textureDamage2Enables1[matrixName] = 1.0F;
                }

                if (part.window != null && part.window.isOpen() && doBlack) {
                    inst.textureUninstall1[matrixName] = 1.0F;
                }
            } else if (part != null && doBlack) {
                inst.textureUninstall1[matrixName] = 1.0F;
            }
        } catch (Exception var6) {
            var6.printStackTrace();
        }
    }

    private void checkDamage2(VehiclePart part, int matrixName, boolean doBlack) {
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;

        try {
            inst.textureDamage1Enables2[matrixName] = 0.0F;
            inst.textureDamage2Enables2[matrixName] = 0.0F;
            inst.textureUninstall2[matrixName] = 0.0F;
            if (part != null && part.getInventoryItem() != null) {
                if (part.<InventoryItem>getInventoryItem().getCondition() < 60 && part.<InventoryItem>getInventoryItem().getCondition() >= 40) {
                    inst.textureDamage1Enables2[matrixName] = 1.0F;
                }

                if (part.<InventoryItem>getInventoryItem().getCondition() < 40) {
                    inst.textureDamage2Enables2[matrixName] = 1.0F;
                }

                if (part.window != null && part.window.isOpen() && doBlack) {
                    inst.textureUninstall2[matrixName] = 1.0F;
                }
            } else if (part != null && doBlack) {
                inst.textureUninstall2[matrixName] = 1.0F;
            }
        } catch (Exception var6) {
            var6.printStackTrace();
        }
    }

    private void checkUninstall2(VehiclePart part, int matrixName) {
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;

        try {
            inst.textureUninstall2[matrixName] = 0.0F;
            if (part != null && part.getInventoryItem() == null) {
                inst.textureUninstall2[matrixName] = 1.0F;
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    private void doOtherBodyWorkDamage() {
        this.checkDamage(this.getPartById("EngineDoor"), 0, false);
        this.checkDamage(this.getPartById("EngineDoor"), 3, false);
        this.checkDamage(this.getPartById("EngineDoor"), 11, false);
        this.checkDamage2(this.getPartById("EngineDoor"), 6, true);
        this.checkDamage(this.getPartById("TruckBed"), 4, false);
        this.checkDamage(this.getPartById("TruckBed"), 7, false);
        this.checkDamage(this.getPartById("TruckBed"), 15, false);
        VehiclePart backDoor = this.getPartById("TrunkDoor");
        if (backDoor != null) {
            this.checkDamage2(backDoor, 10, true);
            if (backDoor.scriptPart.hasLightsRear) {
                this.checkUninstall2(backDoor, 12);
                this.checkUninstall2(backDoor, 1);
                this.checkUninstall2(backDoor, 5);
                this.checkUninstall2(backDoor, 9);
            }
        } else {
            backDoor = this.getPartById("DoorRear");
            if (backDoor != null) {
                this.checkDamage2(backDoor, 10, true);
                if (backDoor.scriptPart.hasLightsRear) {
                    this.checkUninstall2(backDoor, 12);
                    this.checkUninstall2(backDoor, 1);
                    this.checkUninstall2(backDoor, 5);
                    this.checkUninstall2(backDoor, 9);
                }
            }
        }
    }

    private void doWindowDamage() {
        this.checkDamage(this.getPartById("WindowFrontLeft"), 2, true);
        this.checkDamage(this.getPartById("WindowFrontRight"), 9, true);
        VehiclePart backDoor = this.getPartById("WindowRearLeft");
        if (backDoor != null) {
            this.checkDamage(backDoor, 6, true);
        } else {
            backDoor = this.getPartById("WindowMiddleLeft");
            if (backDoor != null) {
                this.checkDamage(backDoor, 6, true);
            }
        }

        backDoor = this.getPartById("WindowRearRight");
        if (backDoor != null) {
            this.checkDamage(backDoor, 13, true);
        } else {
            backDoor = this.getPartById("WindowMiddleRight");
            if (backDoor != null) {
                this.checkDamage(backDoor, 13, true);
            }
        }

        this.checkDamage(this.getPartById("Windshield"), 10, true);
        this.checkDamage(this.getPartById("WindshieldRear"), 14, true);
    }

    private void doDoorDamage() {
        this.checkDamage(this.getPartById("DoorFrontLeft"), 1, true);
        this.checkDamage(this.getPartById("DoorFrontRight"), 8, true);
        VehiclePart backDoor = this.getPartById("DoorRearLeft");
        if (backDoor != null) {
            this.checkDamage(backDoor, 5, true);
        } else {
            backDoor = this.getPartById("DoorMiddleLeft");
            if (backDoor != null) {
                this.checkDamage(backDoor, 5, true);
            }
        }

        backDoor = this.getPartById("DoorRearRight");
        if (backDoor != null) {
            this.checkDamage(backDoor, 12, true);
        } else {
            backDoor = this.getPartById("DoorMiddleRight");
            if (backDoor != null) {
                this.checkDamage(backDoor, 12, true);
            }
        }
    }

    public float getBloodIntensity(String id) {
        return (this.bloodIntensity.getOrDefault(id, BYTE_ZERO) & 255) / 100.0F;
    }

    public void setBloodIntensity(String id, float intensity) {
        byte intensity2 = (byte)(PZMath.clamp(intensity, 0.0F, 1.0F) * 100.0F);
        if (!this.bloodIntensity.containsKey(id) || intensity2 != this.bloodIntensity.get(id)) {
            this.bloodIntensity.put(id, intensity2);
            this.doBloodOverlay();
            this.transmitBlood();
        }
    }

    public void transmitBlood() {
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 4096);
        }
    }

    public void doBloodOverlay() {
        if (this.sprite.modelSlot != null) {
            VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
            Arrays.fill(inst.matrixBlood1Enables1, 0.0F);
            Arrays.fill(inst.matrixBlood1Enables2, 0.0F);
            Arrays.fill(inst.matrixBlood2Enables1, 0.0F);
            Arrays.fill(inst.matrixBlood2Enables2, 0.0F);
            if (Core.getInstance().getOptionBloodDecals() != 0) {
                this.doBloodOverlayFront(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Front"));
                this.doBloodOverlayRear(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Rear"));
                this.doBloodOverlayLeft(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Left"));
                this.doBloodOverlayRight(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Right"));

                for (Entry<String, Byte> entry : this.bloodIntensity.entrySet()) {
                    Integer mask = s_PartToMaskMap.get(entry.getKey());
                    if (mask != null) {
                        inst.matrixBlood1Enables1[mask] = (entry.getValue() & 255) / 100.0F;
                    }
                }

                this.doBloodOverlayAux(inst.matrixBlood2Enables1, inst.matrixBlood2Enables2, 1.0F);
            }
        }
    }

    private void doBloodOverlayAux(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[0] = intensity;
        matrix2[6] = intensity;
        matrix2[4] = intensity;
        matrix2[8] = intensity;
        matrix1[4] = intensity;
        matrix1[7] = intensity;
        matrix1[15] = intensity;
        matrix2[10] = intensity;
        matrix2[12] = intensity;
        matrix2[1] = intensity;
        matrix2[5] = intensity;
        matrix2[9] = intensity;
        matrix1[3] = intensity;
        matrix1[8] = intensity;
        matrix1[12] = intensity;
        matrix1[11] = intensity;
        matrix1[1] = intensity;
        matrix1[5] = intensity;
        matrix2[0] = intensity;
        matrix1[10] = intensity;
        matrix1[14] = intensity;
        matrix1[9] = intensity;
        matrix1[13] = intensity;
        matrix1[2] = intensity;
        matrix1[6] = intensity;
    }

    private void doBloodOverlayFront(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[0] = intensity;
        matrix2[6] = intensity;
        matrix2[4] = intensity;
        matrix2[8] = intensity;
        matrix1[10] = intensity;
    }

    private void doBloodOverlayRear(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[4] = intensity;
        matrix2[10] = intensity;
        matrix2[12] = intensity;
        matrix2[1] = intensity;
        matrix2[5] = intensity;
        matrix2[9] = intensity;
        matrix1[14] = intensity;
    }

    private void doBloodOverlayLeft(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[11] = intensity;
        matrix1[1] = intensity;
        matrix1[5] = intensity;
        matrix1[15] = intensity;
        matrix1[2] = intensity;
        matrix1[6] = intensity;
    }

    private void doBloodOverlayRight(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[3] = intensity;
        matrix1[8] = intensity;
        matrix1[12] = intensity;
        matrix1[7] = intensity;
        matrix1[9] = intensity;
        matrix1[13] = intensity;
    }

    @Override
    public boolean isOnScreen() {
        if (super.isOnScreen()) {
            return true;
        } else if (this.physics == null) {
            return false;
        } else if (this.script == null) {
            return false;
        } else {
            int playerIndex = IsoCamera.frameState.playerIndex;
            if (this.polyDirty) {
                this.getPoly();
            }

            float x1 = IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0);
            float y1 = IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0);
            float x2 = IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0);
            float y2 = IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0);
            float x3 = IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0);
            float y3 = IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0);
            float x4 = IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0);
            float y4 = IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0);
            float dz = (this.script.getCenterOfMassOffset().y + this.script.getExtents().y) / 0.8164967F * 24.0F * Core.tileScale;
            float zoom = Core.getInstance().getZoom(playerIndex);
            float minX = PZMath.min(x1, x2, x3, x4) / zoom;
            float maxX = PZMath.max(x1, x2, x3, x4) / zoom;
            float minY = PZMath.min(y1, y2, y3, y4) / zoom;
            float maxY = PZMath.max(y1, y2, y3, y4) / zoom;
            if (minX < IsoCamera.getScreenLeft(playerIndex) + IsoCamera.getScreenWidth(playerIndex)
                && maxX > IsoCamera.getScreenLeft(playerIndex)
                && minY < IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex)
                && maxY > IsoCamera.getScreenTop(playerIndex)) {
                return true;
            } else {
                y1 -= dz;
                y2 -= dz;
                y3 -= dz;
                y4 -= dz;
                minX = PZMath.min(x1, x2, x3, x4) / zoom;
                maxX = PZMath.max(x1, x2, x3, x4) / zoom;
                minY = PZMath.min(y1, y2, y3, y4) / zoom;
                maxY = PZMath.max(y1, y2, y3, y4) / zoom;
                return minX < IsoCamera.getScreenLeft(playerIndex) + IsoCamera.getScreenWidth(playerIndex)
                    && maxX > IsoCamera.getScreenLeft(playerIndex)
                    && minY < IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex)
                    && maxY > IsoCamera.getScreenTop(playerIndex);
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.script != null) {
            if (this.physics != null) {
                this.physics.debug();
            }

            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
            boolean bForceSeen = isoGameCharacter != null && isoGameCharacter.getVehicle() == this;
            if (isoGameCharacter != null && isoGameCharacter.getVehicle() != null && isoGameCharacter.getVehicle().getVehicleTowing() == this) {
                bForceSeen = true;
            }

            if (bForceSeen || this.square.lighting[playerIndex].bSeen()) {
                if (!bForceSeen && !this.couldSeeIntersectedSquare(playerIndex)) {
                    this.setTargetAlpha(playerIndex, 0.0F);
                } else {
                    this.setTargetAlpha(playerIndex, 1.0F);
                }

                if (this.sprite.hasActiveModel()) {
                    this.updateLights();
                    boolean showBloodDecals = Core.getInstance().getOptionBloodDecals() != 0;
                    if (this.optionBloodDecals != showBloodDecals) {
                        this.optionBloodDecals = showBloodDecals;
                        this.doBloodOverlay();
                    }

                    if (col == null) {
                        inf.set(1.0F, 1.0F, 1.0F, 1.0F);
                    } else {
                        col.a = this.getAlpha(playerIndex);
                        inf.a = col.a;
                        inf.r = col.r;
                        inf.g = col.g;
                        inf.b = col.b;
                    }

                    this.sprite.renderVehicle(this.def, this, x, y, 0.0F, 0.0F, 0.0F, inf, true);
                }

                this.updateAlpha(playerIndex);
                if (Core.debug && DebugOptions.instance.vehicleRenderArea.getValue()) {
                    this.renderAreas();
                }

                if (Core.debug && DebugOptions.instance.vehicleRenderAttackPositions.getValue()) {
                    this.surroundVehicle.render();
                }

                if (Core.debug && DebugOptions.instance.vehicleRenderExit.getValue()) {
                    this.renderExits();
                }

                if (Core.debug && DebugOptions.instance.vehicleRenderIntersectedSquares.getValue()) {
                    this.renderIntersectedSquares();
                }

                if (Core.debug && DebugOptions.instance.vehicleRenderAuthorizations.getValue()) {
                    this.renderAuthorizations();
                }

                if (Core.debug && DebugOptions.instance.vehicleRenderInterpolateBuffer.getValue()) {
                    this.renderInterpolateBuffer();
                }

                if (DebugOptions.instance.vehicleRenderTrailerPositions.getValue()) {
                    this.renderTrailerPositions();
                }

                this.renderUsableArea();
            }
        }
    }

    @Override
    public void renderlast() {
        int playerIndex = IsoCamera.frameState.playerIndex;

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (part.chatElement != null && part.chatElement.getHasChatToDisplay()) {
                if (part.getDeviceData() != null && !part.getDeviceData().getIsTurnedOn()) {
                    part.chatElement.clear(playerIndex);
                } else {
                    float sx = IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
                    float sy = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
                    sx = sx - IsoCamera.getOffX() - this.offsetX;
                    sy = sy - IsoCamera.getOffY() - this.offsetY;
                    sx += 32 * Core.tileScale;
                    sy += 20 * Core.tileScale;
                    sx /= Core.getInstance().getZoom(playerIndex);
                    sy /= Core.getInstance().getZoom(playerIndex);
                    sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX;
                    sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY;
                    part.chatElement.renderBatched(playerIndex, (int)sx, (int)sy);
                }
            }
        }
    }

    public void renderShadow() {
        if (this.physics != null) {
            if (this.script != null) {
                if (this.square != null) {
                    int playerIndex = IsoCamera.frameState.playerIndex;
                    if (this.square.lighting[playerIndex].bSeen()) {
                        if (this.square.lighting[playerIndex].bCouldSee()) {
                            this.setTargetAlpha(playerIndex, 1.0F);
                        } else {
                            this.setTargetAlpha(playerIndex, 0.0F);
                        }

                        Texture vehicleShadow = this.getShadowTexture();
                        if (vehicleShadow != null && vehicleShadow.isReady() && this.getCurrentSquare() != null) {
                            float shadowAlpha = 0.6F * this.getAlpha(playerIndex);
                            ColorInfo lightInfo = this.getCurrentSquare().lighting[playerIndex].lightInfo();
                            shadowAlpha *= (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0F;
                            if (this.polyDirty) {
                                this.getPoly();
                            }

                            if (PerformanceSettings.fboRenderChunk) {
                                float shadowZ = PZMath.fastfloor(this.getZ());
                                if (this.current != null && this.current.hasSlopedSurface()) {
                                    shadowZ = this.current.getApparentZ(this.getX() % 1.0F, this.getY() % 1.0F);
                                }

                                FBORenderShadows.getInstance()
                                    .addShadow(
                                        this.getX(),
                                        this.getY(),
                                        shadowZ,
                                        this.shadowCoord.x2,
                                        this.shadowCoord.y2,
                                        this.shadowCoord.x1,
                                        this.shadowCoord.y1,
                                        this.shadowCoord.x4,
                                        this.shadowCoord.y4,
                                        this.shadowCoord.x3,
                                        this.shadowCoord.y3,
                                        1.0F,
                                        1.0F,
                                        1.0F,
                                        0.8F * shadowAlpha,
                                        vehicleShadow,
                                        true
                                    );
                                return;
                            }

                            SpriteRenderer.instance
                                .renderPoly(
                                    vehicleShadow,
                                    (int)IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0),
                                    (int)IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0),
                                    (int)IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0),
                                    (int)IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0),
                                    (int)IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0),
                                    (int)IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0),
                                    (int)IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0),
                                    (int)IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0),
                                    1.0F,
                                    1.0F,
                                    1.0F,
                                    0.8F * shadowAlpha
                                );
                        }
                    }
                }
            }
        }
    }

    public boolean isEnterBlocked(IsoGameCharacter chr, int seat) {
        return this.isExitBlocked(chr, seat);
    }

    public boolean isExitBlocked(int seat) {
        VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
        if (posInside != null && posOutside != null) {
            Vector3f exitPos = this.getPassengerPositionWorldPos(posOutside, TL_vector3f_pool.get().alloc());
            if (posOutside.area != null) {
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                VehicleScript.Area area = this.script.getAreaById(posOutside.area);
                Vector2 areaPos = this.areaPositionWorld4PlayerInteract(area, vector2);
                exitPos.x = areaPos.x;
                exitPos.y = areaPos.y;
                Vector2ObjectPool.get().release(vector2);
            }

            exitPos.z = 0.0F;
            Vector3f seatedPos = this.getPassengerPositionWorldPos(posInside, TL_vector3f_pool.get().alloc());
            boolean blocked = PolygonalMap2.instance
                .lineClearCollide(seatedPos.x, seatedPos.y, exitPos.x, exitPos.y, PZMath.fastfloor(this.getZ()), this, false, false);
            TL_vector3f_pool.get().release(exitPos);
            TL_vector3f_pool.get().release(seatedPos);
            return blocked;
        } else {
            return true;
        }
    }

    public boolean isExitBlocked(IsoGameCharacter chr, int seat) {
        VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
        if (posInside != null && posOutside != null) {
            Vector3f exitPos = this.getPassengerPositionWorldPos(posOutside, TL_vector3f_pool.get().alloc());
            if (posOutside.area != null) {
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                VehicleScript.Area area = this.script.getAreaById(posOutside.area);
                Vector2 areaPos = this.areaPositionWorld4PlayerInteract(area, vector2);
                exitPos.x = areaPos.x;
                exitPos.y = areaPos.y;
                Vector2ObjectPool.get().release(vector2);
            }

            exitPos.z = 0.0F;
            Vector3f seatedPos = this.getPassengerPositionWorldPos(posInside, TL_vector3f_pool.get().alloc());
            boolean blocked = PolygonalMap2.instance.lineClearCollide(seatedPos.x, seatedPos.y, exitPos.x, exitPos.y, this.getZi(), this, false, false);
            TL_vector3f_pool.get().release(exitPos);
            TL_vector3f_pool.get().release(seatedPos);
            if (!blocked && GameClient.client) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)exitPos.x, (double)exitPos.y, (double)exitPos.z);
                if (sq != null && chr instanceof IsoPlayer isoPlayer && !SafeHouse.isPlayerAllowedOnSquare(isoPlayer, sq)) {
                    blocked = true;
                }
            }

            return blocked;
        } else {
            return true;
        }
    }

    public boolean isPassengerUseDoor2(IsoGameCharacter chr, int seat) {
        VehicleScript.Position posn = this.getPassengerPosition(seat, "outside2");
        if (posn != null) {
            Vector3f worldPos = this.getPassengerPositionWorldPos(posn, TL_vector3f_pool.get().alloc());
            worldPos.sub(chr.getX(), chr.getY(), chr.getZ());
            float length = worldPos.length();
            TL_vector3f_pool.get().release(worldPos);
            if (length < 2.0F) {
                return true;
            }
        }

        return false;
    }

    public boolean isEnterBlocked2(IsoGameCharacter chr, int seat) {
        return this.isExitBlocked2(seat);
    }

    public boolean isExitBlocked2(int seat) {
        VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside2");
        if (posInside != null && posOutside != null) {
            Vector3f exitPos = this.getPassengerPositionWorldPos(posOutside, TL_vector3f_pool.get().alloc());
            exitPos.z = 0.0F;
            Vector3f seatedPos = this.getPassengerPositionWorldPos(posInside, TL_vector3f_pool.get().alloc());
            boolean blocked = PolygonalMap2.instance
                .lineClearCollide(seatedPos.x, seatedPos.y, exitPos.x, exitPos.y, PZMath.fastfloor(this.getZ()), this, false, false);
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)exitPos.x, (double)exitPos.y, (double)exitPos.z);
            if (this.getCharacter(seat) instanceof IsoPlayer isoPlayer && !SafeHouse.isPlayerAllowedOnSquare(isoPlayer, sq)) {
                blocked = true;
            }

            TL_vector3f_pool.get().release(exitPos);
            TL_vector3f_pool.get().release(seatedPos);
            return blocked;
        } else {
            return true;
        }
    }

    private void renderExits() {
        int SCL = Core.tileScale;
        Vector3f exitPos = TL_vector3f_pool.get().alloc();
        Vector3f seatedPos = TL_vector3f_pool.get().alloc();

        for (int seat = 0; seat < this.getMaxPassengers(); seat++) {
            VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
            VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
            if (posInside != null && posOutside != null) {
                float radius = 0.3F;
                this.getPassengerPositionWorldPos(posOutside, exitPos);
                this.getPassengerPositionWorldPos(posInside, seatedPos);
                int x1 = PZMath.fastfloor(exitPos.x - 0.3F);
                int x2 = PZMath.fastfloor(exitPos.x + 0.3F);
                int y1 = PZMath.fastfloor(exitPos.y - 0.3F);
                int y2 = PZMath.fastfloor(exitPos.y + 0.3F);

                for (int y = y1; y <= y2; y++) {
                    for (int x = x1; x <= x2; x++) {
                        float sx = IsoUtils.XToScreenExact(x, y + 1, PZMath.fastfloor(this.getZ()), 0);
                        float sy = IsoUtils.YToScreenExact(x, y + 1, PZMath.fastfloor(this.getZ()), 0);
                        if (PerformanceSettings.fboRenderChunk) {
                            sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX * IsoCamera.frameState.zoom;
                            sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY * IsoCamera.frameState.zoom;
                        }

                        IndieGL.glBlendFunc(770, 771);
                        SpriteRenderer.instance
                            .renderPoly(sx, sy, sx + 32 * SCL, sy - 16 * SCL, sx + 64 * SCL, sy, sx + 32 * SCL, sy + 16 * SCL, 1.0F, 1.0F, 1.0F, 0.5F);
                    }
                }

                float r = 1.0F;
                float g = 1.0F;
                float b = 1.0F;
                if (this.isExitBlocked(seat)) {
                    b = 0.0F;
                    g = 0.0F;
                }

                this.getController().drawCircle(seatedPos.x, seatedPos.y, 0.3F, 0.0F, 0.0F, 1.0F, 1.0F);
                this.getController().drawCircle(exitPos.x, exitPos.y, 0.3F, 1.0F, g, b, 1.0F);
            }
        }

        TL_vector3f_pool.get().release(exitPos);
        TL_vector3f_pool.get().release(seatedPos);
    }

    private Vector2 areaPositionLocal(VehicleScript.Area area) {
        return this.areaPositionLocal(area, new Vector2());
    }

    private Vector2 areaPositionLocal(VehicleScript.Area area, Vector2 out) {
        Vector2 center = this.areaPositionWorld(area, out);
        Vector3f vec = TL_vector3f_pool.get().alloc();
        this.getLocalPos(center.x, center.y, 0.0F, vec);
        center.set(vec.x, vec.z);
        TL_vector3f_pool.get().release(vec);
        return center;
    }

    public Vector2 areaPositionWorld(VehicleScript.Area area) {
        return this.areaPositionWorld(area, new Vector2());
    }

    public Vector2 areaPositionWorld(VehicleScript.Area area, Vector2 out) {
        if (area == null) {
            return null;
        } else {
            Vector3f worldPos = this.getWorldPos(area.x, 0.0F, area.y, TL_vector3f_pool.get().alloc());
            out.set(worldPos.x, worldPos.y);
            TL_vector3f_pool.get().release(worldPos);
            return out;
        }
    }

    public Vector2 areaPositionWorld4PlayerInteract(VehicleScript.Area area) {
        return this.areaPositionWorld4PlayerInteract(area, new Vector2());
    }

    public Vector2 areaPositionWorld4PlayerInteract(VehicleScript.Area area, Vector2 out) {
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        Vector2 p = this.areaPositionWorld(area, out);
        Vector3f vec = this.getLocalPos(p.x, p.y, 0.0F, TL_vector3f_pool.get().alloc());
        if (!(area.x > com.x + ext.x / 2.0F) && !(area.x < com.x - ext.x / 2.0F)) {
            if (area.y > 0.0F) {
                vec.z = vec.z - area.h * 0.3F;
            } else {
                vec.z = vec.z + area.h * 0.3F;
            }
        } else if (area.x > 0.0F) {
            vec.x = vec.x - area.w * 0.3F;
        } else {
            vec.x = vec.x + area.w * 0.3F;
        }

        this.getWorldPos(vec, vec);
        out.set(vec.x, vec.y);
        TL_vector3f_pool.get().release(vec);
        return out;
    }

    private void renderAreas() {
        if (this.getScript() != null) {
            Vector3f forward = this.getForwardVector(TL_vector3f_pool.get().alloc());
            Vector2 vector2 = Vector2ObjectPool.get().alloc();

            for (int i = 0; i < this.parts.size(); i++) {
                VehiclePart part = this.parts.get(i);
                if (part.getArea() != null) {
                    VehicleScript.Area area = this.getScript().getAreaById(part.getArea());
                    if (area != null) {
                        Vector2 center = this.areaPositionWorld(area, vector2);
                        if (center != null) {
                            boolean inArea = this.isInArea(area.id, IsoPlayer.getInstance());
                            this.getController()
                                .drawRect(
                                    forward,
                                    center.x - WorldSimulation.instance.offsetX,
                                    center.y - WorldSimulation.instance.offsetY,
                                    area.w,
                                    area.h / 2.0F,
                                    inArea ? 0.0F : 0.65F,
                                    inArea ? 1.0F : 0.65F,
                                    inArea ? 1.0F : 0.65F
                                );
                            center = this.areaPositionWorld4PlayerInteract(area, vector2);
                            this.getController()
                                .drawRect(
                                    forward,
                                    center.x - WorldSimulation.instance.offsetX,
                                    center.y - WorldSimulation.instance.offsetY,
                                    0.1F,
                                    0.1F,
                                    1.0F,
                                    0.0F,
                                    0.0F
                                );
                        }
                    }
                }
            }

            TL_vector3f_pool.get().release(forward);
            Vector2ObjectPool.get().release(vector2);
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
                IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
                1.0F,
                0.5F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
                IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
                1.0F,
                0.5F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
                IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
                1.0F,
                0.5F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
                IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
                IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
                1.0F,
                0.5F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0),
                IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0),
                0.5F,
                1.0F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0),
                IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0),
                0.5F,
                1.0F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0),
                IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0),
                0.5F,
                1.0F,
                0.5F,
                1.0F,
                0
            );
            LineDrawer.drawLine(
                IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0),
                IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0),
                IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0),
                0.5F,
                1.0F,
                0.5F,
                1.0F,
                0
            );
        }
    }

    private void renderInterpolateBuffer() {
        if (this.netPlayerAuthorization == BaseVehicle.Authorization.Remote) {
            float grx = IsoUtils.XToScreenExact(this.getX(), this.getY(), 0.0F, 0);
            float gry = IsoUtils.YToScreenExact(this.getX(), this.getY(), 0.0F, 0);
            float gr_x = grx - 310.0F;
            float gr_y = gry + 22.0F;
            float gr_w = 300.0F;
            float gr_h = 150.0F;
            float gr_position_scale = 4.0F;
            Color gr_color_border = Color.lightGray;
            Color gr_data_cur_time = Color.green;
            Color gr_data_cur_shifttime = Color.cyan;
            Color gr_data = Color.yellow;
            Color gr_data_x = Color.blue;
            Color gr_data_y = Color.red;
            LineDrawer.drawLine(gr_x, gr_y, gr_x + 300.0F, gr_y, gr_color_border.r, gr_color_border.g, gr_color_border.b, gr_color_border.a, 1);
            LineDrawer.drawLine(
                gr_x, gr_y + 150.0F, gr_x + 300.0F, gr_y + 150.0F, gr_color_border.r, gr_color_border.g, gr_color_border.b, gr_color_border.a, 1
            );
            long gr_t = GameTime.getServerTimeMills();
            long gr_t_start = gr_t - 150L - this.interpolation.history;
            long gr_t_end = gr_t + 150L;
            this.renderInterpolateBuffer_drawVertLine(gr_t_start, gr_color_border, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, true);
            this.renderInterpolateBuffer_drawVertLine(gr_t_end, gr_color_border, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, true);
            this.renderInterpolateBuffer_drawVertLine(gr_t, gr_data_cur_time, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, true);
            this.renderInterpolateBuffer_drawVertLine(
                gr_t - this.interpolation.delay, gr_data_cur_shifttime, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, true
            );
            this.renderInterpolateBuffer_drawPoint(
                gr_t - this.interpolation.delay,
                this.getX(),
                gr_data_x,
                5,
                gr_x,
                gr_y,
                300.0F,
                150.0F,
                gr_t_start,
                gr_t_end,
                this.getX() - 4.0F,
                this.getX() + 4.0F
            );
            this.renderInterpolateBuffer_drawPoint(
                gr_t - this.interpolation.delay,
                this.getY(),
                gr_data_y,
                5,
                gr_x,
                gr_y,
                300.0F,
                150.0F,
                gr_t_start,
                gr_t_end,
                this.getY() - 4.0F,
                this.getY() + 4.0F
            );
            long prev_t = 0L;
            float prev_x = Float.NaN;
            float prev_y = Float.NaN;
            VehicleInterpolationData temp = new VehicleInterpolationData();
            temp.time = gr_t - this.interpolation.delay;
            VehicleInterpolationData higher = this.interpolation.buffer.higher(temp);
            VehicleInterpolationData lower = this.interpolation.buffer.floor(temp);

            for (VehicleInterpolationData data : this.interpolation.buffer) {
                boolean parity = (data.hashCode() & 1) == 0;
                this.renderInterpolateBuffer_drawVertLine(data.time, gr_data, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, parity);
                if (data == higher) {
                    this.renderInterpolateBuffer_drawTextHL(data.time, "H", gr_data_cur_shifttime, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end);
                }

                if (data == lower) {
                    this.renderInterpolateBuffer_drawTextHL(data.time, "L", gr_data_cur_shifttime, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end);
                }

                this.renderInterpolateBuffer_drawPoint(
                    data.time, data.x, gr_data_x, 5, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, this.getX() - 4.0F, this.getX() + 4.0F
                );
                this.renderInterpolateBuffer_drawPoint(
                    data.time, data.y, gr_data_y, 5, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, this.getY() - 4.0F, this.getY() + 4.0F
                );
                if (!Float.isNaN(prev_x)) {
                    this.renderInterpolateBuffer_drawLine(
                        prev_t, prev_x, data.time, data.x, gr_data_x, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, this.getX() - 4.0F, this.getX() + 4.0F
                    );
                    this.renderInterpolateBuffer_drawLine(
                        prev_t, prev_y, data.time, data.y, gr_data_y, gr_x, gr_y, 300.0F, 150.0F, gr_t_start, gr_t_end, this.getY() - 4.0F, this.getY() + 4.0F
                    );
                }

                prev_t = data.time;
                prev_x = data.x;
                prev_y = data.y;
            }

            float[] physicsBuf = new float[27];
            float[] engineSoundBuf = new float[2];
            boolean ret = this.interpolation.interpolationDataGet(physicsBuf, engineSoundBuf, gr_t - this.interpolation.delay);
            TextManager.instance
                .DrawString(
                    gr_x,
                    gr_y + 150.0F + 20.0F,
                    String.format("interpolationDataGet=%s", ret ? "True" : "False"),
                    gr_data_cur_shifttime.r,
                    gr_data_cur_shifttime.g,
                    gr_data_cur_shifttime.b,
                    gr_data_cur_shifttime.a
                );
            TextManager.instance
                .DrawString(
                    gr_x,
                    gr_y + 150.0F + 30.0F,
                    String.format("buffer.size=%d buffering=%s", this.interpolation.buffer.size(), String.valueOf(this.interpolation.buffering)),
                    gr_data_cur_shifttime.r,
                    gr_data_cur_shifttime.g,
                    gr_data_cur_shifttime.b,
                    gr_data_cur_shifttime.a
                );
            TextManager.instance
                .DrawString(
                    gr_x,
                    gr_y + 150.0F + 40.0F,
                    String.format("delayTarget=%d", this.interpolation.delayTarget),
                    gr_data_cur_shifttime.r,
                    gr_data_cur_shifttime.g,
                    gr_data_cur_shifttime.b,
                    gr_data_cur_shifttime.a
                );
            if (this.interpolation.buffer.size() >= 2) {
                TextManager.instance
                    .DrawString(
                        gr_x,
                        gr_y + 150.0F + 50.0F,
                        String.format("last=%d first=%d", this.interpolation.buffer.last().time, this.interpolation.buffer.first().time),
                        gr_data_cur_shifttime.r,
                        gr_data_cur_shifttime.g,
                        gr_data_cur_shifttime.b,
                        gr_data_cur_shifttime.a
                    );
                TextManager.instance
                    .DrawString(
                        gr_x,
                        gr_y + 150.0F + 60.0F,
                        String.format(
                            "(last-first).time=%d delay=%d",
                            this.interpolation.buffer.last().time - this.interpolation.buffer.first().time,
                            this.interpolation.delay
                        ),
                        gr_data_cur_shifttime.r,
                        gr_data_cur_shifttime.g,
                        gr_data_cur_shifttime.b,
                        gr_data_cur_shifttime.a
                    );
            }
        }
    }

    private void renderInterpolateBuffer_drawTextHL(long _x, String text, Color col, float gr_x, float gr_y, float gr_w, float gr_h, long start, long end) {
        float gr_t_m = gr_w / (float)(end - start);
        float gr_temp_x = (float)(_x - start) * gr_t_m;
        TextManager.instance.DrawString(gr_temp_x + gr_x, gr_y, text, col.r, col.g, col.b, col.a);
    }

    private void renderInterpolateBuffer_drawVertLine(
        long _x, Color col, float gr_x, float gr_y, float gr_w, float gr_h, long start, long end, boolean draw_parity
    ) {
        float gr_t_m = gr_w / (float)(end - start);
        float gr_temp_x = (float)(_x - start) * gr_t_m;
        LineDrawer.drawLine(gr_temp_x + gr_x, gr_y, gr_temp_x + gr_x, gr_y + gr_h, col.r, col.g, col.b, col.a, 1);
        TextManager.instance
            .DrawString(
                gr_temp_x + gr_x,
                gr_y + gr_h + (draw_parity ? 0.0F : 10.0F),
                String.format("%.1f", (float)(_x - _x / 100000L * 100000L) / 1000.0F),
                col.r,
                col.g,
                col.b,
                col.a
            );
    }

    private void renderInterpolateBuffer_drawLine(
        long _x, float _y, long _x2, float _y2, Color col, float gr_x, float gr_y, float gr_w, float gr_h, long start, long end, float starty, float endy
    ) {
        float gr_t_m = gr_w / (float)(end - start);
        float gr_temp_x = (float)(_x - start) * gr_t_m;
        float gr_temp_x2 = (float)(_x2 - start) * gr_t_m;
        float gr_t_my = gr_h / (endy - starty);
        float gr_temp_y = (_y - starty) * gr_t_my;
        float gr_temp_y2 = (_y2 - starty) * gr_t_my;
        LineDrawer.drawLine(gr_temp_x + gr_x, gr_temp_y + gr_y, gr_temp_x2 + gr_x, gr_temp_y2 + gr_y, col.r, col.g, col.b, col.a, 1);
    }

    private void renderInterpolateBuffer_drawPoint(
        long _x, float _y, Color col, int radius, float gr_x, float gr_y, float gr_w, float gr_h, long start, long end, float starty, float endy
    ) {
        float gr_t_m = gr_w / (float)(end - start);
        float gr_temp_x = (float)(_x - start) * gr_t_m;
        float gr_t_my = gr_h / (endy - starty);
        float gr_temp_y = (_y - starty) * gr_t_my;
        LineDrawer.drawCircle(gr_temp_x + gr_x, gr_temp_y + gr_y, radius, 10, col.r, col.g, col.b);
    }

    private void renderAuthorizations() {
        float r = 0.3F;
        float g = 0.3F;
        float b = 0.3F;
        float a = 0.5F;
        switch (this.netPlayerAuthorization) {
            case Server:
                r = 1.0F;
                break;
            case LocalCollide:
                b = 1.0F;
                break;
            case RemoteCollide:
                b = 1.0F;
                r = 1.0F;
                break;
            case Local:
                g = 1.0F;
                break;
            case Remote:
                g = 1.0F;
                r = 1.0F;
        }

        LineDrawer.drawLine(
            IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
            IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
            r,
            g,
            b,
            a,
            1
        );
        LineDrawer.drawLine(
            IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0),
            IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
            r,
            g,
            b,
            a,
            1
        );
        LineDrawer.drawLine(
            IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0),
            IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
            r,
            g,
            b,
            a,
            1
        );
        LineDrawer.drawLine(
            IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0),
            IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
            IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0),
            r,
            g,
            b,
            a,
            1
        );
        float distance = 0.0F;
        if (this.getVehicleTowing() != null) {
            BaseVehicle.Vector3fObjectPool pool = TL_vector3f_pool.get();
            Vector3f v1t = pool.alloc();
            Vector3f worldA = this.getTowingWorldPos(this.getTowAttachmentSelf(), v1t);
            Vector3f v2t = pool.alloc();
            Vector3f worldB = this.getVehicleTowing().getTowingWorldPos(this.getVehicleTowing().getTowAttachmentSelf(), v2t);
            if (worldA != null && worldB != null) {
                LineDrawer.DrawIsoLine(worldA.x, worldA.y, worldA.z, worldB.x, worldB.y, worldB.z, r, g, b, a, 1);
                LineDrawer.DrawIsoCircle(worldA.x, worldA.y, worldA.z, 0.2F, 16, r, g, b, a);
                distance = IsoUtils.DistanceTo(worldA.x, worldA.y, worldA.z, worldB.x, worldB.y, worldB.z);
            }

            pool.release(v1t);
            pool.release(v2t);
        }

        r = 1.0F;
        g = 1.0F;
        b = 0.75F;
        a = 1.0F;
        float dy = 10.0F;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), 0.0F, 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), 0.0F, 0);
        IsoPlayer owner = GameClient.IDToPlayerMap.get(this.netPlayerId);
        String player = (owner == null ? "@server" : owner.getUsername()) + " ( " + this.netPlayerId + " )";
        float var16;
        TextManager.instance.DrawString(sx, sy + (var16 = dy + 12.0F), "VID: " + this.getScriptName() + " ( " + this.getId() + " )", r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy = var16 + 12.0F), "PID: " + player, r, g, b, a);
        float var18;
        TextManager.instance.DrawString(sx, sy + (var18 = dy + 12.0F), "Auth: " + this.netPlayerAuthorization.name(), r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy = var18 + 12.0F), "Static/active: " + this.isStatic + "/" + this.isActive, r, g, b, a);
        float var20;
        TextManager.instance.DrawString(sx, sy + (var20 = dy + 12.0F), "x=" + this.getX() + " / y=" + this.getY(), r, g, b, a);
        TextManager.instance
            .DrawString(
                sx,
                sy + (dy = var20 + 14.0F),
                String.format("Passengers: %d/%d", Arrays.stream(this.passengers).filter(p -> p.character != null).count(), this.passengers.length),
                r,
                g,
                b,
                a
            );
        float var22;
        TextManager.instance
            .DrawString(
                sx,
                sy + (var22 = dy + 12.0F),
                String.format("Speed: %s%.3f kmph", this.getCurrentSpeedKmHour() >= 0.0F ? "+" : "", this.getCurrentSpeedKmHour()),
                r,
                g,
                b,
                a
            );
        TextManager.instance.DrawString(sx, sy + (dy = var22 + 12.0F), String.format("Engine speed: %.3f", this.engineSpeed), r, g, b, a);
        float var24;
        TextManager.instance.DrawString(sx, sy + (var24 = dy + 12.0F), String.format("Mass: %.3f/%.3f", this.getMass(), this.getFudgedMass()), r, g, b, a);
        if (distance > 1.5F) {
            g = 0.75F;
        }

        if (this.getVehicleTowing() != null) {
            TextManager.instance.DrawString(sx, sy + (dy = var24 + 14.0F), "Towing: " + this.getVehicleTowing().getId(), r, g, b, a);
            TextManager.instance.DrawString(sx, sy + (var24 = dy + 12.0F), String.format("Distance: %.3f", distance), r, g, b, a);
        }

        if (this.getVehicleTowedBy() != null) {
            TextManager.instance.DrawString(sx, sy + (dy = var24 + 14.0F), "TowedBy: " + this.getVehicleTowedBy().getId(), r, g, b, a);
            float var27;
            TextManager.instance.DrawString(sx, sy + (var27 = dy + 12.0F), String.format("Distance: %.3f", distance), r, g, b, a);
        }
    }

    private void renderUsableArea() {
        if (this.getScript() != null && UIManager.visibleAllUi) {
            if (this.getAlpha(IsoPlayer.getPlayerIndex()) != 0.0F) {
                IsoPlayer chr = IsoPlayer.getInstance();
                VehiclePart part = this.getUseablePart(chr);
                boolean bBestSeat = false;
                if (part == null && this == chr.getUseableVehicle()) {
                    int seat = this.getBestSeat(chr);
                    if (seat != -1) {
                        part = this.getPassengerDoor(seat);
                        bBestSeat = true;
                    }
                }

                if (part != null) {
                    VehicleScript.Area area = this.getScript().getAreaById(part.getArea());
                    if (area != null) {
                        Vector2 vector2 = Vector2ObjectPool.get().alloc();
                        Vector2 center = this.areaPositionWorld(area, vector2);
                        if (center == null) {
                            Vector2ObjectPool.get().release(vector2);
                        } else {
                            Vector3f forward = this.getForwardVector(TL_vector3f_pool.get().alloc());
                            float r = Core.getInstance().getGoodHighlitedColor().getR();
                            float g = Core.getInstance().getGoodHighlitedColor().getG();
                            float b = Core.getInstance().getGoodHighlitedColor().getB();
                            if (bBestSeat) {
                                r *= 0.6666667F;
                                g *= 0.6666667F;
                                b *= 0.6666667F;
                            }

                            this.getController()
                                .drawRect(
                                    forward,
                                    center.x - WorldSimulation.instance.offsetX,
                                    center.y - WorldSimulation.instance.offsetY,
                                    area.w,
                                    area.h / 2.0F,
                                    r,
                                    g,
                                    b
                                );
                            forward.x = forward.x * (area.h / this.script.getModelScale());
                            forward.z = forward.z * (area.h / this.script.getModelScale());
                            if (part.getDoor() != null && (part.getId().contains("Left") || part.getId().contains("Right"))) {
                                if (part.getId().contains("Front")) {
                                    this.getController()
                                        .drawRect(
                                            forward,
                                            center.x - WorldSimulation.instance.offsetX + forward.x * area.h / 2.0F,
                                            center.y - WorldSimulation.instance.offsetY + forward.z * area.h / 2.0F,
                                            area.w,
                                            area.h / 8.0F,
                                            r,
                                            g,
                                            b
                                        );
                                } else if (part.getId().contains("Rear")) {
                                    this.getController()
                                        .drawRect(
                                            forward,
                                            center.x - WorldSimulation.instance.offsetX - forward.x * area.h / 2.0F,
                                            center.y - WorldSimulation.instance.offsetY - forward.z * area.h / 2.0F,
                                            area.w,
                                            area.h / 8.0F,
                                            r,
                                            g,
                                            b
                                        );
                                }
                            }

                            Vector2ObjectPool.get().release(vector2);
                            TL_vector3f_pool.get().release(forward);
                        }
                    }
                }
            }
        }
    }

    private boolean couldSeeIntersectedSquare(int playerIndex) {
        VehiclePoly poly = this.getPoly();
        float minX = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
        float minY = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
        float maxX = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
        float maxY = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
        int z = PZMath.fastfloor(this.getZ());

        for (int y = PZMath.fastfloor(minY); y < (int)Math.ceil(maxY); y++) {
            for (int x = PZMath.fastfloor(minX); x < (int)Math.ceil(maxX); x++) {
                IsoGridSquare square = this.getCell().getGridSquare(x, y, z);
                if (square != null && square.isCouldSee(playerIndex) && this.isIntersectingSquare(x, y, z)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void renderIntersectedSquares() {
        VehiclePoly poly = this.getPoly();
        float minX = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
        float minY = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
        float maxX = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
        float maxY = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));

        for (int y = PZMath.fastfloor(minY); y < (int)Math.ceil(maxY); y++) {
            for (int x = PZMath.fastfloor(minX); x < (int)Math.ceil(maxX); x++) {
                if (this.isIntersectingSquare(x, y, PZMath.fastfloor(this.getZ()))) {
                    LineDrawer.addRect(x, y, PZMath.fastfloor(this.getZ()), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    }

    private void renderTrailerPositions() {
        if (this.script != null && this.physics != null) {
            Vector3f v1 = TL_vector3f_pool.get().alloc();
            Vector3f v3 = TL_vector3f_pool.get().alloc();
            Vector3f vt = this.getTowingWorldPos("trailer", v3);
            if (vt != null) {
                this.physics.drawCircle(vt.x, vt.y, 0.3F, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            Vector3f v2 = this.getPlayerTrailerLocalPos("trailer", false, v1);
            if (v2 != null) {
                this.getWorldPos(v2, v2);
                boolean blocked = PolygonalMap2.instance.lineClearCollide(v3.x, v3.y, v2.x, v2.y, PZMath.fastfloor(this.getZ()), this, false, false);
                this.physics.drawCircle(v2.x, v2.y, 0.3F, 1.0F, blocked ? 0.0F : 1.0F, blocked ? 0.0F : 1.0F, 1.0F);
                if (blocked) {
                    LineDrawer.addLine(v2.x, v2.y, 0.0F, v3.x, v3.y, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
                }
            }

            v2 = this.getPlayerTrailerLocalPos("trailer", true, v1);
            if (v2 != null) {
                this.getWorldPos(v2, v2);
                boolean blocked = PolygonalMap2.instance.lineClearCollide(v3.x, v3.y, v2.x, v2.y, PZMath.fastfloor(this.getZ()), this, false, false);
                this.physics.drawCircle(v2.x, v2.y, 0.3F, 1.0F, blocked ? 0.0F : 1.0F, blocked ? 0.0F : 1.0F, 1.0F);
                if (blocked) {
                    LineDrawer.addLine(v2.x, v2.y, 0.0F, v3.x, v3.y, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
                }
            }

            TL_vector3f_pool.get().release(v1);
            TL_vector3f_pool.get().release(v3);
        }
    }

    public void getWheelForwardVector(int wheelIndex, Vector3f out) {
        BaseVehicle.WheelInfo wheelInfo = this.wheelInfo[wheelIndex];
        Matrix4f m = TL_matrix4f_pool.get().alloc();
        m.rotationY(wheelInfo.steering);
        Matrix4f chassisRotMatrix = this.jniTransform.getMatrix(TL_matrix4f_pool.get().alloc());
        chassisRotMatrix.setTranslation(0.0F, 0.0F, 0.0F);
        m.mul(chassisRotMatrix, m);
        TL_matrix4f_pool.get().release(chassisRotMatrix);
        TL_matrix4f_pool.get().release(m);
        Vector4f forward = allocVector4f();
        m.getColumn(2, forward);
        out.set(forward.x, 0.0F, forward.z);
        releaseVector4f(forward);
    }

    public void tryStartEngine(boolean haveKey) {
        if (this.getDriver() == null || !(this.getDriver() instanceof IsoPlayer) || !((IsoPlayer)this.getDriver()).isBlockMovement()) {
            VehiclePart part = this.getPartById("Engine");
            if (part != null && part.getCondition() > 0) {
                if (this.getEngineQuality() <= 0) {
                    this.engineDoStartingFailed("VehicleEngineFailureDamage");
                } else if (this.engineState == BaseVehicle.engineStateTypes.Idle) {
                    DrainableComboItem batteryItem = this.getBattery().getInventoryItem();
                    if (batteryItem != null) {
                        float currentCharge = batteryItem.getCurrentUsesFloat();
                        batteryItem.setCurrentUsesFloat(PZMath.clamp(currentCharge - 0.025F, 0.0F, 1.0F));
                        if (currentCharge <= 0.1F) {
                            this.engineDoStartingFailedNoPower();
                        } else {
                            if ((!Core.debug || !DebugOptions.instance.cheat.vehicle.startWithoutKey.getValue())
                                && !SandboxOptions.instance.vehicleEasyUse.getValue()
                                && !this.isKeysInIgnition()
                                && !haveKey
                                && !this.isHotwired()) {
                                if (GameServer.server) {
                                    this.getDriver().sendObjectChange("vehicleNoKey");
                                } else {
                                    this.getDriver().SayDebug(" [img=media/ui/CarKey_none.png]");
                                    this.checkVehicleFailsToStartWithZombiesTargeting();
                                }
                            } else {
                                this.engineDoStarting();
                            }
                        }
                    }
                }
            } else {
                if (part != null) {
                    this.engineDoStartingFailed("VehicleEngineFailureDamage");
                }
            }
        }
    }

    public void tryStartEngine() {
        this.tryStartEngine(false);
    }

    public void engineDoIdle() {
        this.engineState = BaseVehicle.engineStateTypes.Idle;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
    }

    public void engineDoStarting() {
        this.engineState = BaseVehicle.engineStateTypes.Starting;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.setKeysInIgnition(true);
        this.setPreviouslyMoved(true);
    }

    public boolean isStarting() {
        return this.engineState == BaseVehicle.engineStateTypes.Starting
            || this.engineState == BaseVehicle.engineStateTypes.StartingFailed
            || this.engineState == BaseVehicle.engineStateTypes.StartingSuccess
            || this.engineState == BaseVehicle.engineStateTypes.StartingFailedNoPower;
    }

    private String getEngineSound() {
        return this.getScript() != null && this.getScript().getSounds().engine != null ? this.getScript().getSounds().engine : "VehicleEngineDefault";
    }

    private String getEngineStartSound() {
        return this.getScript() != null && this.getScript().getSounds().engineStart != null ? this.getScript().getSounds().engineStart : "VehicleStarted";
    }

    private String getEngineTurnOffSound() {
        return this.getScript() != null && this.getScript().getSounds().engineTurnOff != null ? this.getScript().getSounds().engineTurnOff : "VehicleTurnedOff";
    }

    private String getHandBrakeSound() {
        return this.getScript() != null && this.getScript().getSounds().handBrake != null ? this.getScript().getSounds().handBrake : "VehicleHandBrake";
    }

    private String getIgnitionFailSound() {
        return this.getScript() != null && this.getScript().getSounds().ignitionFail != null
            ? this.getScript().getSounds().ignitionFail
            : "VehicleFailingToStart";
    }

    private String getIgnitionFailNoPowerSound() {
        return this.getScript() != null && this.getScript().getSounds().ignitionFailNoPower != null
            ? this.getScript().getSounds().ignitionFailNoPower
            : SoundKey.VEHICLE_IGNITION_FAIL_DEFAULT.id();
    }

    public void engineDoRetryingStarting() {
        this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
        this.getEmitter().playSoundImpl(this.getIgnitionFailSound(), (IsoObject)null);
        this.engineState = BaseVehicle.engineStateTypes.RetryingStarting;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
    }

    public void engineDoStartingSuccess() {
        this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
        this.engineState = BaseVehicle.engineStateTypes.StartingSuccess;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        if (this.getEngineStartSound().equals(this.getEngineSound())) {
            if (!this.getEmitter().isPlaying(this.combinedEngineSound)) {
                this.combinedEngineSound = this.emitter.playSoundImpl(this.getEngineSound(), (IsoObject)null);
            }
        } else {
            this.getEmitter().playSoundImpl(this.getEngineStartSound(), (IsoObject)null);
        }

        this.transmitEngine();
        this.setKeysInIgnition(true);
        this.checkVehicleStartsWithZombiesTargeting();
    }

    public void engineDoStartingFailed() {
        this.engineDoStartingFailed(this.getIgnitionFailSound());
    }

    public void engineDoStartingFailed(String sound) {
        this.getEmitter().stopSoundByName(sound);
        this.getEmitter().playSoundImpl(sound, (IsoObject)null);
        this.stopEngineSounds();
        this.engineState = BaseVehicle.engineStateTypes.StartingFailed;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
    }

    public void engineDoStartingFailedNoPower() {
        this.getEmitter().stopSoundByName(this.getIgnitionFailNoPowerSound());
        this.getEmitter().playSoundImpl(this.getIgnitionFailNoPowerSound(), (IsoObject)null);
        this.stopEngineSounds();
        this.engineState = BaseVehicle.engineStateTypes.StartingFailedNoPower;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
    }

    public void engineDoRunning() {
        this.setNeedPartsUpdate(true);
        this.engineState = BaseVehicle.engineStateTypes.Running;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
    }

    public void engineDoStalling() {
        this.getEmitter().playSoundImpl("VehicleRunningOutOfGas", (IsoObject)null);
        this.engineState = BaseVehicle.engineStateTypes.Stalling;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.stopEngineSounds();
        this.engineSoundIndex = 0;
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
        if (!Core.getInstance().getOptionLeaveKeyInIgnition()) {
            this.setKeysInIgnition(false);
        }
    }

    public void engineDoShuttingDown() {
        this.engineDoShuttingDown(this.getEngineTurnOffSound());
    }

    public void engineDoShuttingDown(String sound) {
        if (!StringUtils.equals(sound, this.getEngineSound())) {
            this.getEmitter().playSoundImpl(sound, (IsoObject)null);
        }

        this.stopEngineSounds();
        this.engineSoundIndex = 0;
        this.engineState = BaseVehicle.engineStateTypes.ShutingDown;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        if (!Core.getInstance().getOptionLeaveKeyInIgnition()) {
            this.setKeysInIgnition(false);
        }

        VehiclePart heater = this.getHeater();
        if (heater != null) {
            heater.getModData().rawset("active", false);
        }
    }

    public void shutOff() {
        this.shutOff(this.getEngineTurnOffSound());
    }

    public void shutOff(String sound) {
        if (this.getPartById("GasTank").getContainerContentAmount() == 0.0F) {
            this.engineDoStalling();
        } else {
            this.engineDoShuttingDown(sound);
        }
    }

    public void resumeRunningAfterLoad() {
        if (GameClient.client) {
            IsoGameCharacter driver = this.getDriver();
            if (driver != null) {
                Boolean haveKey = this.getDriver().getInventory().haveThisKeyId(this.getKeyId()) != null ? Boolean.TRUE : Boolean.FALSE;
                GameClient.instance.sendClientCommandV((IsoPlayer)this.getDriver(), "vehicle", "startEngine", "haveKey", haveKey);
            }
        } else if (this.isEngineWorking()) {
            this.getEmitter();
            this.engineDoStartingSuccess();
        }
    }

    public boolean isEngineStarted() {
        return this.engineState == BaseVehicle.engineStateTypes.Starting
            || this.engineState == BaseVehicle.engineStateTypes.StartingFailed
            || this.engineState == BaseVehicle.engineStateTypes.StartingSuccess
            || this.engineState == BaseVehicle.engineStateTypes.RetryingStarting;
    }

    public boolean isEngineRunning() {
        return this.engineState == BaseVehicle.engineStateTypes.Running;
    }

    public boolean isEngineWorking() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("checkEngine");
            if (functionName != null && !Boolean.TRUE.equals(this.callLuaBoolean(functionName, this, part))) {
                return false;
            }
        }

        return true;
    }

    public boolean isOperational() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("checkOperate");
            if (functionName != null && !Boolean.TRUE.equals(this.callLuaBoolean(functionName, this, part))) {
                return false;
            }
        }

        return true;
    }

    public boolean isDriveable() {
        return !this.isEngineWorking() ? false : this.isOperational();
    }

    public BaseSoundEmitter getEmitter() {
        if (this.emitter == null) {
            if (!Core.soundDisabled && !GameServer.server) {
                FMODSoundEmitter emitter1 = new FMODSoundEmitter();
                emitter1.parameterUpdater = this;
                this.emitter = emitter1;
            } else {
                this.emitter = new DummySoundEmitter();
            }
        }

        return this.emitter;
    }

    public long playSoundImpl(String file, IsoObject parent) {
        return this.getEmitter().playSoundImpl(file, parent);
    }

    public int stopSound(long channel) {
        return this.getEmitter().stopSound(channel);
    }

    public void playSound(String sound) {
        this.getEmitter().playSound(sound);
    }

    public void updateSounds() {
        if (!GameServer.server) {
            if (this.getBatteryCharge() > 0.0F) {
                if (this.lightbarSirenMode.isEnable() && this.soundSirenSignal == -1L) {
                    this.setLightbarSirenMode(this.lightbarSirenMode.get());
                }
            } else if (this.soundSirenSignal != -1L) {
                this.getEmitter().stopSound(this.soundSirenSignal);
                this.soundSirenSignal = -1L;
            }
        }

        IsoGameCharacter closestListener = null;
        float closestListenerDistSq = Float.MAX_VALUE;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoGameCharacter chr = IsoPlayer.players[i];
            if (chr != null && chr.getCurrentSquare() != null) {
                float px = chr.getX();
                float py = chr.getY();
                float dist = IsoUtils.DistanceToSquared(px, py, this.getX(), this.getY());
                dist *= chr.getHearDistanceModifier();
                if (chr.hasTrait(CharacterTrait.DEAF)) {
                    dist = Float.MAX_VALUE;
                }

                if (dist < closestListenerDistSq) {
                    closestListener = chr;
                    closestListenerDistSq = dist;
                }
            }
        }

        if (closestListener == null) {
            if (this.emitter != null) {
                this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                if (!this.emitter.isEmpty()) {
                    this.emitter.tick();
                }
            }
        } else {
            if (!GameServer.server) {
                float rainIntensity = ClimateManager.getInstance().isRaining() ? ClimateManager.getInstance().getPrecipitationIntensity() : 0.0F;
                if (this.getSquare() != null && this.getSquare().isInARoom()) {
                    rainIntensity = 0.0F;
                }

                if (this.getEmitter().isPlaying("VehicleAmbiance")) {
                    if (rainIntensity == 0.0F) {
                        this.getEmitter().stopOrTriggerSoundByName("VehicleAmbiance");
                    }
                } else if (rainIntensity > 0.0F && closestListenerDistSq < 100.0F) {
                    this.emitter.playAmbientLoopedImpl("VehicleAmbiance");
                }

                float distance = closestListenerDistSq;
                if (closestListenerDistSq > 1200.0F) {
                    this.stopEngineSounds();
                    if (this.emitter != null && !this.emitter.isEmpty()) {
                        this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                        this.emitter.tick();
                    }

                    return;
                }

                for (int ix = 0; ix < this.newEngineSoundId.length; ix++) {
                    if (this.newEngineSoundId[ix] != 0L) {
                        this.getEmitter().setVolume(this.newEngineSoundId[ix], 1.0F - distance / 1200.0F);
                    }
                }
            }

            if (this.getController() != null) {
                if (!GameServer.server) {
                    if (this.emitter == null) {
                        if (this.engineState != BaseVehicle.engineStateTypes.Running) {
                            return;
                        }

                        this.getEmitter();
                    }

                    boolean isAnyListenerInside = this.isAnyListenerInside();
                    if (this.startTime <= 0.0F
                        && this.engineState == BaseVehicle.engineStateTypes.Running
                        && !this.getEmitter().isPlaying(this.combinedEngineSound)) {
                        this.combinedEngineSound = this.emitter.playSoundImpl(this.getEngineSound(), (IsoObject)null);
                    }

                    boolean skidding = false;
                    if (!GameClient.client || this.isLocalPhysicSim()) {
                        for (int ixx = 0; ixx < this.script.getWheelCount(); ixx++) {
                            if (this.wheelInfo[ixx].skidInfo < 0.15F) {
                                skidding = true;
                                break;
                            }
                        }
                    }

                    if (this.getDriver() == null) {
                        skidding = false;
                    }

                    if (skidding != this.skidding) {
                        if (skidding) {
                            this.skidSound = this.getEmitter().playSoundImpl("VehicleSkid", (IsoObject)null);
                        } else if (this.skidSound != 0L) {
                            this.emitter.stopSound(this.skidSound);
                            this.skidSound = 0L;
                        }

                        this.skidding = skidding;
                    }

                    if (this.soundBackMoveSignal != -1L && this.emitter != null) {
                        this.emitter.set3D(this.soundBackMoveSignal, !isAnyListenerInside);
                    }

                    if (this.soundHorn != -1L && this.emitter != null) {
                        this.emitter.set3D(this.soundHorn, !isAnyListenerInside);
                    }

                    if (this.soundSirenSignal != -1L && this.emitter != null) {
                        this.emitter.set3D(this.soundSirenSignal, !isAnyListenerInside);
                    }

                    this.updateDoorAlarmSound();
                    this.updateHandBrakeSound();
                    if (this.emitter != null && (this.engineState != BaseVehicle.engineStateTypes.Idle || !this.emitter.isEmpty())) {
                        this.getFMODParameters().update();
                        this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                        this.emitter.tick();
                    }
                }
            }
        }
    }

    private void updateDoorAlarmSound() {
        if (this.emitter != null) {
            boolean bShouldPlay = false;
            if (this.isEngineRunning()) {
                for (int i = 0; i < this.getMaxPassengers(); i++) {
                    VehiclePart doorPart = this.getPassengerDoor(i);
                    if (doorPart != null && !doorPart.isInventoryItemUninstalled() && doorPart.getDoor().isOpen()) {
                        bShouldPlay = true;
                        break;
                    }
                }
            }

            if (bShouldPlay) {
                if (!this.emitter.isPlaying(this.doorAlarmSound)) {
                    this.doorAlarmSound = this.emitter.playSoundImpl("VehicleDoorAlarm", (IsoObject)null);
                }
            } else if (this.emitter.isPlaying(this.doorAlarmSound)) {
                this.emitter.stopSound(this.doorAlarmSound);
                this.doorAlarmSound = 0L;
            }
        }
    }

    private void updateHandBrakeSound() {
        if (this.emitter != null) {
            String soundName = this.getHandBrakeSound();
            boolean bShouldPlay = false;
            if (soundName != null
                && this.isEngineRunning()
                && this.getDriver() != null
                && this.getController() != null
                && this.getController().clientControls.brake) {
                bShouldPlay = true;
            }

            if (bShouldPlay) {
                if (!this.handBrakeActive) {
                    this.handBrakeActive = true;
                    if (!this.emitter.isPlaying(this.handBrakeSound)) {
                        this.handBrakeSound = this.emitter.playSoundImpl(soundName, (IsoObject)null);
                    }
                }
            } else if (this.handBrakeActive) {
                this.handBrakeActive = false;
                if (this.emitter.isPlaying(this.handBrakeSound)) {
                    this.emitter.stopOrTriggerSound(this.handBrakeSound);
                }

                this.handBrakeSound = 0L;
            }
        }
    }

    private boolean updatePart(VehiclePart part) {
        part.updateSignalDevice();
        VehicleLight light = part.getLight();
        if (light != null && part.getId().contains("Headlight")) {
            part.setLightActive(this.getHeadlightsOn() && part.getInventoryItem() != null && this.getBatteryCharge() > 0.0F);
        }

        String functionName = part.getLuaFunction("update");
        if (functionName == null) {
            return false;
        } else {
            float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
            if (part.getLastUpdated() < 0.0F) {
                part.setLastUpdated(worldAgeHours);
            } else if (part.getLastUpdated() > worldAgeHours) {
                part.setLastUpdated(worldAgeHours);
            }

            float elapsedHours = worldAgeHours - part.getLastUpdated();
            if ((int)(elapsedHours * 60.0F) > 0) {
                part.setLastUpdated(worldAgeHours);
                this.callLuaVoid(functionName, this, part, (double)(elapsedHours * 60.0F));
                return true;
            } else {
                return false;
            }
        }
    }

    public void updateParts() {
        if (!GameClient.client) {
            boolean didUpdate = false;

            for (int i = 0; i < this.getPartCount(); i++) {
                VehiclePart part = this.getPartByIndex(i);
                if (this.updatePart(part) && !didUpdate) {
                    didUpdate = true;
                }

                if (i == this.getPartCount() - 1 && didUpdate) {
                    this.brakeBetweenUpdatesSpeed = 0.0F;
                }
            }
        } else {
            for (int i = 0; i < this.getPartCount(); i++) {
                VehiclePart partx = this.getPartByIndex(i);
                partx.updateSignalDevice();
            }
        }
    }

    public void drainBatteryUpdateHack() {
        boolean engineRunning = this.isEngineRunning();
        if (!engineRunning) {
            for (int i = 0; i < this.parts.size(); i++) {
                VehiclePart part = this.parts.get(i);
                if (part.getDeviceData() != null && part.getDeviceData().getIsTurnedOn()) {
                    this.updatePart(part);
                } else if (part.getLight() != null && part.getLight().getActive()) {
                    this.updatePart(part);
                }
            }

            if (this.hasLightbar() && (this.lightbarLightsMode.isEnable() || this.lightbarSirenMode.isEnable()) && this.getBattery() != null) {
                this.updatePart(this.getBattery());
            }
        }
    }

    public boolean getHeadlightsOn() {
        return this.headlightsOn;
    }

    public void setHeadlightsOn(boolean on) {
        if (this.headlightsOn != on) {
            this.headlightsOn = on;
            if (GameServer.server) {
                this.updateFlags = (short)(this.updateFlags | 8);
            } else {
                this.playSound(this.headlightsOn ? "VehicleHeadlightsOn" : "VehicleHeadlightsOff");
            }
        }
    }

    public boolean getWindowLightsOn() {
        return this.windowLightsOn;
    }

    public void setWindowLightsOn(boolean on) {
        this.windowLightsOn = on;
    }

    public boolean getHeadlightCanEmmitLight() {
        if (this.getBatteryCharge() <= 0.0F) {
            return false;
        } else {
            VehiclePart part = this.getPartById("HeadlightLeft");
            if (part != null && part.getInventoryItem() != null) {
                return true;
            } else {
                part = this.getPartById("HeadlightRight");
                return part != null && part.getInventoryItem() != null;
            }
        }
    }

    public boolean getStoplightsOn() {
        return this.stoplightsOn;
    }

    public void setStoplightsOn(boolean on) {
        if (this.stoplightsOn != on) {
            this.stoplightsOn = on;
            if (GameServer.server) {
                this.updateFlags = (short)(this.updateFlags | 8);
            }
        }
    }

    public boolean hasHeadlights() {
        return this.getLightCount() > 0;
    }

    @Override
    public void addToWorld() {
        this.addToWorld(false);
    }

    public void addToWorld(boolean crashed) {
        if (this.addedToWorld) {
            DebugLog.Vehicle.error("added vehicle twice " + this + " id=" + this.vehicleId);
        } else {
            if (Core.debug) {
                this.setDebugPhysicsRender(true);
            }

            VehiclesDB2.instance.setVehicleLoaded(this);
            this.addedToWorld = true;
            this.removedFromWorld = false;
            super.addToWorld();
            this.createPhysics();

            for (int i = 0; i < this.parts.size(); i++) {
                VehiclePart part = this.parts.get(i);
                if (part.getItemContainer() != null) {
                    part.getItemContainer().addItemsToProcessItems();
                }

                if (part.getDeviceData() != null) {
                    ZomboidRadio.getInstance().RegisterDevice(part);
                }
            }

            if (this.lightbarSirenMode.isEnable()) {
                this.setLightbarSirenMode(this.lightbarSirenMode.get());
                if (this.sirenStartTime <= 0.0) {
                    this.sirenStartTime = GameTime.instance.getWorldAgeHours();
                }
            }

            if (this.chunk != null && this.chunk.jobType != IsoChunk.JobType.SoftReset) {
                if (PathfindNative.useNativeCode) {
                    PathfindNative.instance.addVehicle(this);
                } else {
                    PolygonalMap2.instance.addVehicleToWorld(this);
                }
            }

            if (this.engineState != BaseVehicle.engineStateTypes.Idle) {
                this.engineSpeed = this.getScript() == null ? 1000.0 : this.getScript().getEngineIdleSpeed();
            }

            if (this.chunk != null
                && this.chunk.jobType != IsoChunk.JobType.SoftReset
                && !this.isPreviouslyEntered()
                && !this.isPreviouslyMoved()
                && !this.isHotwired()
                && !this.isBurnt()) {
                try {
                    this.trySpawnKey(crashed);
                } catch (Exception var4) {
                    ExceptionLogger.logException(var4);
                }
            }

            if (this.emitter != null) {
                SoundManager.instance.registerEmitter(this.emitter);
            }
        }
    }

    @Override
    public void removeFromWorld() {
        this.breakConstraint(false, false);
        VehiclesDB2.instance.setVehicleUnloaded(this);

        for (int i = 0; i < this.passengers.length; i++) {
            if (this.getPassenger(i).character != null) {
                for (int k = 0; k < 4; k++) {
                    if (this.getPassenger(i).character == IsoPlayer.players[k]) {
                        return;
                    }
                }
            }
        }

        IsoChunk.removeFromCheckedVehicles(this);
        DebugLog.Vehicle.trace("BaseVehicle.removeFromWorld() %s id=%d", this, this.vehicleId);
        if (!this.removedFromWorld) {
            if (!this.addedToWorld) {
                DebugLog.Vehicle.debugln("ERROR: removing vehicle but addedToWorld=false %s id=%d", this, this.vehicleId);
            }

            if (Core.debug) {
                this.setDebugPhysicsRender(false);
            }

            this.removedFromWorld = true;
            this.addedToWorld = false;

            for (int ix = 0; ix < this.parts.size(); ix++) {
                VehiclePart part = this.parts.get(ix);
                if (part.getItemContainer() != null) {
                    part.getItemContainer().removeItemsFromProcessItems();
                }

                if (part.getDeviceData() != null) {
                    part.getDeviceData().cleanSoundsAndEmitter();
                    ZomboidRadio.getInstance().UnRegisterDevice(part);
                }
            }

            if (this.emitter != null) {
                this.emitter.stopAll();
                SoundManager.instance.unregisterEmitter(this.emitter);
                this.emitter = null;
            }

            if (this.hornEmitter != null && this.soundHorn != -1L) {
                this.hornEmitter.stopAll();
                this.hornEmitter = null;
                this.soundHorn = -1L;
            }

            if (this.alarmEmitter != null && this.soundAlarm != -1L) {
                this.alarmEmitter.stopAll();
                this.alarmEmitter = null;
                this.soundAlarm = -1L;
            }

            if (this.createdModel) {
                ModelManager.instance.Remove(this);
                this.createdModel = false;
            }

            this.releaseAnimationPlayers();
            if (this.getController() != null) {
                if (!GameServer.server) {
                    Bullet.removeVehicle(this.vehicleId);
                }

                this.physics = null;
            }

            if (GameServer.server || GameClient.client) {
                VehicleManager.instance.removeFromWorld(this);
            } else if (this.vehicleId != -1) {
                VehicleIDMap.instance.remove(this.vehicleId);
            }

            IsoWorld.instance.currentCell.addVehicles.remove(this);
            IsoWorld.instance.currentCell.vehicles.remove(this);
            if (PathfindNative.useNativeCode) {
                PathfindNative.instance.removeVehicle(this);
            } else {
                PolygonalMap2.instance.removeVehicleFromWorld(this);
            }

            if (GameClient.client) {
                this.chunk.vehicles.remove(this);
            }

            this.surroundVehicle.reset();
            this.removeWorldLights();

            for (IsoAnimal a : this.animals) {
                a.delete();
            }

            super.removeFromWorld();
        }
    }

    public void permanentlyRemove() {
        for (int i = 0; i < this.getMaxPassengers(); i++) {
            IsoGameCharacter chr = this.getCharacter(i);
            if (chr != null) {
                if (GameServer.server) {
                    chr.sendObjectChange("exitVehicle");
                }

                this.exit(chr);
            }
        }

        this.breakConstraint(true, false);
        this.removeFromWorld();
        this.removeFromSquare();
        if (this.chunk != null) {
            this.chunk.vehicles.remove(this);
        }

        VehiclesDB2.instance.removeVehicle(this);
    }

    public VehiclePart getBattery() {
        return this.battery;
    }

    public void setEngineFeature(int quality, int loudness, int engineForce) {
        this.engineQuality = PZMath.clamp(quality, 0, 100);
        this.engineLoudness = (int)(loudness / 2.7F);
        this.enginePower = engineForce;
    }

    public int getEngineQuality() {
        return this.engineQuality;
    }

    public int getEngineLoudness() {
        return this.engineLoudness;
    }

    public int getEnginePower() {
        return this.enginePower;
    }

    public float getBatteryCharge() {
        VehiclePart battery = this.getBattery();
        return battery != null && battery.getInventoryItem() instanceof DrainableComboItem
            ? battery.<InventoryItem>getInventoryItem().getCurrentUsesFloat()
            : 0.0F;
    }

    public int getPartCount() {
        return this.parts.size();
    }

    public VehiclePart getPartByIndex(int index) {
        return index >= 0 && index < this.parts.size() ? this.parts.get(index) : null;
    }

    public VehiclePart getPartByPartId(zombie.scripting.objects.VehiclePart id) {
        return id == null ? null : this.getPartById(id.toString());
    }

    public VehiclePart getPartById(String id) {
        if (id == null) {
            return null;
        } else {
            for (int i = 0; i < this.parts.size(); i++) {
                VehiclePart part = this.parts.get(i);
                VehicleScript.Part scriptPart = part.getScriptPart();
                if (scriptPart != null && id.equals(scriptPart.id)) {
                    return part;
                }
            }

            return null;
        }
    }

    public int getPartIndex(String id) {
        if (id == null) {
            return -1;
        } else {
            for (int i = 0; i < this.parts.size(); i++) {
                VehicleScript.Part scriptPart = this.parts.get(i).getScriptPart();
                if (scriptPart != null && id.equals(scriptPart.id)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public int getNumberOfPartsWithContainers() {
        if (this.getScript() == null) {
            return 0;
        } else {
            int count = 0;

            for (int i = 0; i < this.getScript().getPartCount(); i++) {
                if (this.getScript().getPart(i).container != null) {
                    count++;
                }
            }

            return count;
        }
    }

    public VehiclePart getTrunkDoorPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUNK_DOOR);
    }

    public VehiclePart getTrunkPart() {
        VehiclePart truckBedPart = this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED);
        return truckBedPart != null ? truckBedPart : this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED_OPEN);
    }

    public VehiclePart getTrailerTrunkPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRAILER_TRUNK);
    }

    public VehiclePart getPartForSeatContainer(int seat) {
        if (this.getScript() != null && seat >= 0 && seat < this.getMaxPassengers()) {
            for (int i = 0; i < this.getPartCount(); i++) {
                VehiclePart part = this.getPartByIndex(i);
                if (part.getContainerSeatNumber() == seat) {
                    return part;
                }
            }

            return null;
        } else {
            return null;
        }
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
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            ItemContainer partContainer = part.getItemContainer();
            if (partContainer != null) {
                boolean canStore = in_isValidPredicate.accept(in_paramToCompare, partContainer);
                if (canStore) {
                    inout_containerList.addUniqueReference(partContainer);
                }
            }
        }

        return inout_containerList;
    }

    public void transmitPartCondition(VehiclePart part) {
        if (GameServer.server) {
            if (this.parts.contains(part)) {
                part.updateFlags = (short)(part.updateFlags | 2048);
                this.updateFlags = (short)(this.updateFlags | 2048);
            }
        }
    }

    public void transmitPartItem(VehiclePart part) {
        if (GameServer.server) {
            if (this.parts.contains(part)) {
                part.updateFlags = (short)(part.updateFlags | 128);
                this.updateFlags = (short)(this.updateFlags | 128);
            }
        }
    }

    public void transmitPartModData(VehiclePart part) {
        if (GameServer.server) {
            if (this.parts.contains(part)) {
                part.updateFlags = (short)(part.updateFlags | 16);
                this.updateFlags = (short)(this.updateFlags | 16);
            }
        }
    }

    public void transmitPartUsedDelta(VehiclePart part) {
        if (GameServer.server) {
            if (this.parts.contains(part)) {
                if (part.getInventoryItem() instanceof DrainableComboItem) {
                    part.updateFlags = (short)(part.updateFlags | 32);
                    this.updateFlags = (short)(this.updateFlags | 32);
                }
            }
        }
    }

    public void transmitPartDoor(VehiclePart part) {
        if (GameServer.server) {
            if (this.parts.contains(part)) {
                if (part.getDoor() != null) {
                    part.updateFlags = (short)(part.updateFlags | 512);
                    this.updateFlags = (short)(this.updateFlags | 512);
                }
            }
        }
    }

    public void transmitPartWindow(VehiclePart part) {
        if (GameServer.server) {
            if (this.parts.contains(part)) {
                if (part.getWindow() != null) {
                    part.updateFlags = (short)(part.updateFlags | 256);
                    this.updateFlags = (short)(this.updateFlags | 256);
                }
            }
        }
    }

    public int getLightCount() {
        return this.lights.size();
    }

    public VehiclePart getLightByIndex(int index) {
        return index >= 0 && index < this.lights.size() ? this.lights.get(index) : null;
    }

    public String getZone() {
        return this.respawnZone;
    }

    public void setZone(String name) {
        this.respawnZone = name;
    }

    public boolean isInArea(String areaId, IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        } else {
            float chrX = chr.getX();
            float chrY = chr.getY();
            return this.isInArea(areaId, chrX, chrY);
        }
    }

    private boolean isInArea(String areaId, float chrX, float chrY) {
        if (areaId != null && this.getScript() != null) {
            VehicleScript.Area area = this.getScript().getAreaById(areaId);
            if (area == null) {
                return false;
            } else {
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                Vector2 center = this.areaPositionLocal(area, vector2);
                if (center == null) {
                    Vector2ObjectPool.get().release(vector2);
                    return false;
                } else {
                    Vector3f localPos = TL_vector3f_pool.get().alloc();
                    this.getLocalPos(chrX, chrY, this.getZ(), localPos);
                    float minX = center.x - area.w / 2.0F;
                    float minY = center.y - area.h / 2.0F;
                    float maxX = center.x + area.w / 2.0F;
                    float maxY = center.y + area.h / 2.0F;
                    Vector2ObjectPool.get().release(vector2);
                    boolean inside = localPos.x >= minX && localPos.x < maxX && localPos.z >= minY && localPos.z < maxY;
                    TL_vector3f_pool.get().release(localPos);
                    return inside;
                }
            }
        } else {
            return false;
        }
    }

    public float getAreaDist(String areaId, float x, float y, float z) {
        if (areaId != null && this.getScript() != null) {
            VehicleScript.Area area = this.getScript().getAreaById(areaId);
            if (area != null) {
                Vector3f localPos = this.getLocalPos(x, y, z, TL_vector3f_pool.get().alloc());
                float minX = Math.abs(area.x - area.w / 2.0F);
                float minY = Math.abs(area.y - area.h / 2.0F);
                float maxX = Math.abs(area.x + area.w / 2.0F);
                float maxY = Math.abs(area.y + area.h / 2.0F);
                float result = Math.abs(localPos.x + minX) + Math.abs(localPos.z + minY);
                TL_vector3f_pool.get().release(localPos);
                return result;
            } else {
                return 999.0F;
            }
        } else {
            return 999.0F;
        }
    }

    public float getAreaDist(String areaId, IsoGameCharacter chr) {
        if (areaId != null && this.getScript() != null) {
            VehicleScript.Area area = this.getScript().getAreaById(areaId);
            if (area != null) {
                Vector3f localPos = this.getLocalPos(chr.getX(), chr.getY(), this.getZ(), TL_vector3f_pool.get().alloc());
                float minX = Math.abs(area.x - area.w / 2.0F);
                float minY = Math.abs(area.y - area.h / 2.0F);
                float result = Math.abs(localPos.x + minX) + Math.abs(localPos.z + minY);
                TL_vector3f_pool.get().release(localPos);
                return result;
            } else {
                return 999.0F;
            }
        } else {
            return 999.0F;
        }
    }

    public Vector2 getAreaCenter(String areaId) {
        return this.getAreaCenter(areaId, new Vector2());
    }

    public Vector2 getAreaCenter(String areaId, Vector2 out) {
        if (areaId != null && this.getScript() != null) {
            VehicleScript.Area area = this.getScript().getAreaById(areaId);
            return area == null ? null : this.areaPositionWorld(area, out);
        } else {
            return null;
        }
    }

    public Vector2 getAreaFacingPosition(String areaId, Vector2 out) {
        Vector2 areaCenter = this.getAreaCenter(areaId, out);
        if (areaCenter == null) {
            return null;
        } else {
            float areaZ = this.getZ();
            return this.getFacingPosition(areaCenter.x, areaCenter.y, areaZ, out);
        }
    }

    public boolean isInBounds(float worldX, float worldY) {
        return this.getPoly().containsPoint(worldX, worldY);
    }

    public boolean canAccessContainer(int partIndex, IsoGameCharacter chr) {
        VehiclePart part = this.getPartByIndex(partIndex);
        if (part == null) {
            return false;
        } else {
            VehicleScript.Part scriptPart = part.getScriptPart();
            if (scriptPart == null) {
                return false;
            } else if (scriptPart.container == null) {
                return false;
            } else if (part.isInventoryItemUninstalled() && scriptPart.container.capacity == 0) {
                return false;
            } else {
                return scriptPart.container.luaTest != null && !scriptPart.container.luaTest.isEmpty()
                    ? Boolean.TRUE.equals(this.callLuaBoolean(scriptPart.container.luaTest, this, part, chr))
                    : true;
            }
        }
    }

    public boolean canInstallPart(IsoGameCharacter chr, VehiclePart part) {
        if (!this.parts.contains(part)) {
            return false;
        } else {
            KahluaTable install = part.getTable("install");
            return install != null && install.rawget("test") instanceof String
                ? Boolean.TRUE.equals(this.callLuaBoolean((String)install.rawget("test"), this, part, chr))
                : false;
        }
    }

    public boolean canUninstallPart(IsoGameCharacter chr, VehiclePart part) {
        if (!this.parts.contains(part)) {
            return false;
        } else {
            KahluaTable uninstall = part.getTable("uninstall");
            return uninstall != null && uninstall.rawget("test") instanceof String
                ? Boolean.TRUE.equals(this.callLuaBoolean((String)uninstall.rawget("test"), this, part, chr))
                : false;
        }
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2);
        }
    }

    private void callLuaVoid(String functionName, Object arg1) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1);
        }
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2, Object arg3) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2, arg3);
        }
    }

    private Boolean callLuaBoolean(String functionName, Object arg, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        return functionObj == null ? null : LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, arg, arg2);
    }

    private Boolean callLuaBoolean(String functionName, Object arg, Object arg2, Object arg3) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        return functionObj == null ? null : LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, arg, arg2, arg3);
    }

    public short getId() {
        return this.vehicleId;
    }

    public void setTireInflation(int wheelIndex, float inflation) {
        Bullet.setTireInflation(this.vehicleId, wheelIndex, inflation);
    }

    public void setTireRemoved(int wheelIndex, boolean removed) {
        if (!GameServer.server) {
            Bullet.setTireRemoved(this.vehicleId, wheelIndex, removed);
        }
    }

    public Vector3f chooseBestAttackPosition(IsoGameCharacter target, IsoGameCharacter attacker, Vector3f worldPos) {
        if (attacker instanceof IsoAnimal) {
            return null;
        } else {
            Vector2f v0 = allocVector2f();
            Vector2f v = target.getVehicle().getSurroundVehicle().getPositionForZombie((IsoZombie)attacker, v0);
            float vx = v0.x;
            float vy = v0.y;
            releaseVector2f(v0);
            return v != null ? worldPos.set(vx, vy, this.getZ()) : null;
        }
    }

    public BaseVehicle.MinMaxPosition getMinMaxPosition() {
        BaseVehicle.MinMaxPosition res = new BaseVehicle.MinMaxPosition();
        float x = this.getX();
        float y = this.getY();
        Vector3f ext = this.getScript().getExtents();
        float l = ext.x;
        float w = ext.z;
        IsoDirections dir = this.getDir();
        switch (dir) {
            case E:
            case W:
                res.minX = x - l / 2.0F;
                res.maxX = x + l / 2.0F;
                res.minY = y - w / 2.0F;
                res.maxY = y + w / 2.0F;
                break;
            case N:
            case S:
                res.minX = x - w / 2.0F;
                res.maxX = x + w / 2.0F;
                res.minY = y - l / 2.0F;
                res.maxY = y + l / 2.0F;
                break;
            default:
                return null;
        }

        return res;
    }

    public String getVehicleType() {
        return this.type;
    }

    public void setVehicleType(String type) {
        this.type = type;
    }

    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public void lockServerUpdate(long lockTimeMs) {
        this.updateLockTimeout = System.currentTimeMillis() + lockTimeMs;
    }

    /**
     * Change transmission, slow down the car if you change shift for a superior one
     */
    public void changeTransmission(TransmissionNumber newTransmission) {
        this.transmissionNumber = newTransmission;
    }

    /**
     * Try to hotwire a car Calcul is: 100-Engine quality (capped to 5) + Skill modifier: electricityLvl * 4 % of
     *  hotwiring the car Failing may cause the ignition to break
     */
    public void tryHotwire(int electricityLevel) {
        int engineQuality = Math.max(100 - this.getEngineQuality(), 5);
        engineQuality = Math.min(engineQuality, 50);
        int skillModifier = electricityLevel * 4;
        int chance = engineQuality + skillModifier;
        boolean sync = false;
        String sound = "VehicleHotwireFail";
        if (Rand.Next(100) <= 11 - electricityLevel && this.alarmed) {
            this.triggerAlarm();
        }

        if (Rand.Next(100) <= chance) {
            this.setHotwired(true);
            sync = true;
            sound = "VehicleHotwireSuccess";
        } else if (Rand.Next(100) <= 10 - electricityLevel) {
            this.setHotwiredBroken(true);
            sync = true;
        }

        if (GameServer.server) {
            LuaManager.GlobalObject.playServerSound(sound, this.square);
        } else if (this.getDriver() != null) {
            this.getDriver().getEmitter().playSound(sound);
        }

        if (sync && GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 4096);
        }
    }

    public void cheatHotwire(boolean hotwired, boolean broken) {
        if (hotwired != this.hotwired || broken != this.hotwiredBroken) {
            this.hotwired = hotwired;
            this.hotwiredBroken = broken;
            if (GameServer.server) {
                this.updateFlags = (short)(this.updateFlags | 4096);
            }
        }
    }

    public boolean isKeyIsOnDoor() {
        return this.keyIsOnDoor;
    }

    public void setKeyIsOnDoor(boolean keyIsOnDoor) {
        this.keyIsOnDoor = keyIsOnDoor;
    }

    public boolean isHotwired() {
        return this.hotwired;
    }

    public void setHotwired(boolean hotwired) {
        this.hotwired = hotwired;
    }

    public boolean isHotwiredBroken() {
        return this.hotwiredBroken;
    }

    public void setHotwiredBroken(boolean hotwiredBroken) {
        this.hotwiredBroken = hotwiredBroken;
    }

    public IsoGameCharacter getDriver() {
        BaseVehicle.Passenger passenger = this.getPassenger(0);
        return passenger == null ? null : passenger.character;
    }

    public IsoGameCharacter getDriverRegardlessOfTow() {
        IsoGameCharacter driver = this.getDriver();
        if (driver != null) {
            return driver;
        } else {
            return this.vehicleTowedBy != null ? this.vehicleTowedBy.getDriver() : null;
        }
    }

    public boolean isKeysInIgnition() {
        return !this.ignitionSwitch.getItems().isEmpty();
    }

    public void setKeysInIgnition(boolean keysOnContact) {
        IsoGameCharacter driver = this.getDriver();
        if (driver != null) {
            this.setAlarmed(false);
            if (!GameClient.client || driver instanceof IsoPlayer isoPlayer && isoPlayer.isLocalPlayer()) {
                if (!this.isHotwired()) {
                    if (!GameServer.server && keysOnContact && !this.isKeysInIgnition()) {
                        InventoryItem key = this.getDriver().getInventory().haveThisKeyId(this.getKeyId());
                        int containerID = -1;
                        if (key != null) {
                            if (key.getContainer() != null) {
                                InventoryItem keyRing = key.getContainer().getContainingItem();
                                if (!(keyRing instanceof InventoryContainer) || !keyRing.hasTag(ItemTag.KEY_RING) && !"KeyRing".equals(keyRing.getType())) {
                                    if (key.hasModData()) {
                                        key.getModData().rawset("keyRing", null);
                                    }
                                } else {
                                    key.getModData().rawset("keyRing", (double)keyRing.getID());
                                    containerID = key.getContainer().getContainingItem().getID();
                                }
                            }

                            this.keysInIgnition = keysOnContact;
                            if (GameClient.client) {
                                GameClient.instance
                                    .sendClientCommandV((IsoPlayer)this.getDriver(), "vehicle", "putKeyInIgnition", "key", key, "container", containerID);
                            }

                            if (key.getContainer() != null) {
                                key.getContainer().DoRemoveItem(key);
                            }

                            this.ignitionSwitch.addItem(key);
                            this.keysContainerId = containerID;
                        }
                    }

                    if (!keysOnContact && this.isKeysInIgnition() && !GameServer.server) {
                        if (this.currentKey == null) {
                            this.currentKey = this.createVehicleKey();
                        }

                        InventoryItem key = this.ignitionSwitch.getItems().get(0);
                        ItemContainer container = this.getDriver().getInventory();
                        if (this.keysContainerId != -1) {
                            InventoryContainer keyRingContainer = (InventoryContainer)this.getDriver().getInventory().getItemWithID(this.keysContainerId);
                            InventoryItem keyRingItem = this.getDriver().getInventory().getItemWithID(this.keysContainerId);
                            if (keyRingItem != null && keyRingContainer != null) {
                                container = keyRingContainer.getInventory();
                                key.getModData().rawset("keyRing", (double)keyRingItem.getID());
                            }
                        }

                        container.addItem(key);
                        this.ignitionSwitch.removeAllItems();
                        this.setCurrentKey(null);
                        this.keysInIgnition = keysOnContact;
                        if (GameClient.client) {
                            GameClient.instance.sendClientCommand((IsoPlayer)this.getDriver(), "vehicle", "removeKeyFromIgnition", null);
                        }
                    }
                }
            }
        }
    }

    public void putKeyInIgnition(InventoryItem key, int containerID) {
        if (GameServer.server) {
            if (key instanceof Key) {
                if (!this.isKeysInIgnition()) {
                    this.keysInIgnition = true;
                    this.keyIsOnDoor = false;
                    if (key != null) {
                        ItemContainer container = this.getDriver().getInventory();
                        if (containerID != 1) {
                            InventoryContainer keyRingContainer = (InventoryContainer)this.getDriver().getInventory().getItemWithID(containerID);
                            InventoryItem keyRingItem = this.getDriver().getInventory().getItemWithID(containerID);
                            if (keyRingItem != null && keyRingContainer != null) {
                                container = keyRingContainer.getInventory();
                                key.getModData().rawset("keyRing", (double)keyRingItem.getID());
                            }
                        } else {
                            key.getModData().rawset("keyRing", null);
                        }

                        container.DoRemoveItem(this.getDriver().getInventory().haveThisKeyId(key.getKeyId()));
                        this.ignitionSwitch.addItem(key);
                        this.keysContainerId = containerID;
                    }

                    this.currentKey = key;
                    this.updateFlags = (short)(this.updateFlags | 4096);
                    VehicleManager.instance.serverUpdate();
                }
            }
        }
    }

    public void removeKeyFromIgnition() {
        if (GameServer.server) {
            if (this.isKeysInIgnition()) {
                this.keysInIgnition = false;
                InventoryItem key = this.ignitionSwitch.getItems().get(0);
                ItemContainer container = this.getDriver().getInventory();
                if (this.keysContainerId != -1) {
                    InventoryContainer keyRingContainer = (InventoryContainer)this.getDriver().getInventory().getItemWithID(this.keysContainerId);
                    InventoryItem keyRingItem = this.getDriver().getInventory().getItemWithID(this.keysContainerId);
                    if (keyRingItem != null && keyRingContainer != null) {
                        container = keyRingContainer.getInventory();
                        key.getModData().rawset("keyRing", (double)keyRingItem.getID());
                    }
                } else {
                    key.getModData().rawset("keyRing", null);
                }

                container.addItem(key);
                this.ignitionSwitch.removeAllItems();
                this.keysContainerId = -1;
                this.currentKey = null;
                this.updateFlags = (short)(this.updateFlags | 4096);
                VehicleManager.instance.serverUpdate();
            }
        }
    }

    public void putKeyOnDoor(InventoryItem key) {
        if (GameServer.server) {
            if (key instanceof Key) {
                if (!this.keyIsOnDoor) {
                    this.keyIsOnDoor = true;
                    this.keysInIgnition = false;
                    this.currentKey = key;
                    this.updateFlags = (short)(this.updateFlags | 4096);
                }
            }
        }
    }

    public void removeKeyFromDoor() {
        if (GameServer.server) {
            if (this.keyIsOnDoor) {
                this.keyIsOnDoor = false;
                this.currentKey = null;
                this.updateFlags = (short)(this.updateFlags | 4096);
            }
        }
    }

    public void syncKeyInIgnition(boolean inIgnition, boolean onDoor, InventoryItem key) {
        if (GameClient.client) {
            this.keysInIgnition = inIgnition;
            this.keyIsOnDoor = onDoor;
            this.currentKey = key;
        }
    }

    private void randomizeContainers() {
        if (!GameClient.client) {
            boolean doSpecific = true;
            String scriptType = this.getScriptName().substring(this.getScriptName().indexOf(46) + 1);
            ItemPickerJava.VehicleDistribution distrib = ItemPickerJava.VehicleDistributions.get(scriptType + this.getSkinIndex());
            if (distrib != null) {
                doSpecific = false;
            } else {
                distrib = ItemPickerJava.VehicleDistributions.get(scriptType);
            }

            if (distrib == null) {
                for (int i = 0; i < this.parts.size(); i++) {
                    VehiclePart part = this.parts.get(i);
                    if (part.getItemContainer() != null) {
                        if (Core.debug) {
                            DebugLog.Vehicle.debugln("VEHICLE MISSING CONT DISTRIBUTION: " + scriptType);
                        }

                        return;
                    }
                }
            } else {
                int specialLootChance = 8;
                if (this.getScript() == null) {
                    this.setScript();
                }

                if (this.getScript().getSpecialLootChance() > 0) {
                    specialLootChance = this.getScript().getSpecialLootChance();
                }

                ItemPickerJava.ItemPickerRoom contDistrib;
                if (doSpecific && Rand.Next(100) <= specialLootChance && !distrib.specific.isEmpty()) {
                    contDistrib = PZArrayUtil.pickRandom(distrib.specific);
                } else {
                    contDistrib = distrib.normal;
                }

                if (!StringUtils.isNullOrWhitespace(this.specificDistributionId)) {
                    for (int ix = 0; ix < distrib.specific.size(); ix++) {
                        ItemPickerJava.ItemPickerRoom room = distrib.specific.get(ix);
                        if (this.specificDistributionId.equals(room.specificId)) {
                            contDistrib = room;
                            break;
                        }
                    }
                }

                for (int ixx = 0; ixx < this.parts.size(); ixx++) {
                    VehiclePart part = this.parts.get(ixx);
                    if (part.getItemContainer() != null) {
                        if (GameServer.server && GameServer.softReset) {
                            part.getItemContainer().setExplored(false);
                        }

                        if (!part.getItemContainer().explored) {
                            part.getItemContainer().clear();
                            if (Rand.Next(100) <= 100) {
                                this.randomizeContainer(part, contDistrib);
                            }

                            part.getItemContainer().setExplored(true);
                        }
                    }
                }
            }
        }
    }

    private void randomizeContainers(ItemPickerJava.ItemPickerRoom contDistrib) {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                if (GameServer.server && GameServer.softReset) {
                    part.getItemContainer().setExplored(false);
                }

                if (!part.getItemContainer().explored) {
                    part.getItemContainer().clear();
                    if (Rand.Next(100) <= 100) {
                        this.randomizeContainer(part, contDistrib);
                    }

                    part.getItemContainer().setExplored(true);
                }
            }
        }
    }

    private void randomizeContainer(VehiclePart part, ItemPickerJava.ItemPickerRoom contDistrib) {
        if (!GameClient.client) {
            if (contDistrib != null) {
                if (!part.getId().contains("Seat") && !contDistrib.containers.containsKey(part.getId())) {
                    DebugLog.Vehicle.debugln("NO CONT DISTRIB FOR PART: " + part.getId() + " CAR: " + this.getScriptName().replaceFirst("Base.", ""));
                }

                ItemPickerJava.fillContainerType(contDistrib, part.getItemContainer(), "", null);
                String scriptType = this.getScriptName().substring(this.getScriptName().indexOf(46) + 1);
                LuaEventManager.triggerEvent("OnFillContainer", scriptType, part.getItemContainer().getType(), part.getItemContainer());
            }
        }
    }

    public boolean hasAlarm() {
        return this.script.getSounds().alarmEnable;
    }

    public boolean hasHorn() {
        return this.script.getSounds().hornEnable;
    }

    public boolean hasLightbar() {
        return this.script.getLightbar().enable;
    }

    public void setChosenAlarmSound(String soundName) {
        this.chosenAlarmSound = StringUtils.discardNullOrWhitespace(soundName);
    }

    public void chooseAlarmSound() {
        if (this.script != null) {
            VehicleScript.Sounds sounds = this.script.getSounds();
            if (sounds.alarmEnable) {
                if (this.chosenAlarmSound == null || !sounds.alarm.contains(this.chosenAlarmSound) && !sounds.alarmLoop.contains(this.chosenAlarmSound)) {
                    ArrayList<String> choices = new ArrayList<>();
                    choices.addAll(sounds.alarm);
                    choices.addAll(sounds.alarmLoop);
                    this.chosenAlarmSound = PZArrayUtil.pickRandom(choices);
                }
            }
        }
    }

    public void onAlarmStart() {
        this.soundAlarmOn = true;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
            if (this.script.getSounds().alarmEnable) {
                WorldSoundManager.instance
                    .addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0F, 1.0F, false, true, false, false, true);
            }
        } else {
            if (this.soundAlarm != -1L) {
                this.alarmEmitter.stopSound(this.soundAlarm);
            }

            if (this.script.getSounds().alarmEnable) {
                this.alarmEmitter = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), this.getZi());
                this.chooseAlarmSound();
                this.soundAlarm = this.alarmEmitter.playSoundImpl(this.chosenAlarmSound, (IsoObject)null);
                this.alarmEmitter.set3D(this.soundAlarm, !this.isAnyListenerInside());
                this.alarmEmitter.setVolume(this.soundAlarm, 1.0F);
                this.alarmEmitter.setPitch(this.soundAlarm, 1.0F);
                if (!GameClient.client) {
                    WorldSoundManager.instance
                        .addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0F, 1.0F, false, true, false, false, true);
                }

                IsoGameCharacter driver = this.getDriver();
                IsoPlayer player = Type.tryCastTo(driver, IsoPlayer.class);
                if (player != null && player.isLocalPlayer()) {
                    player.triggerMusicIntensityEvent("VehicleHorn");
                }
            }
        }
    }

    public void onAlarmStop() {
        this.soundAlarmOn = false;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
        } else {
            if (this.script.getSounds().alarmEnable && this.soundAlarm != -1L) {
                this.alarmEmitter.stopSound(this.soundAlarm);
                this.soundAlarm = -1L;
            }
        }
    }

    public void onHornStart() {
        this.soundHornOn = true;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
            if (this.script.getSounds().hornEnable) {
                WorldSoundManager.instance
                    .addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0F, 1.0F, false, true, false, false, true);
            }
        } else {
            if (this.soundHorn != -1L) {
                this.hornEmitter.stopSound(this.soundHorn);
            }

            if (this.script.getSounds().hornEnable) {
                this.hornEmitter = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), this.getZi());
                this.soundHorn = this.hornEmitter.playSoundLoopedImpl(this.script.getSounds().horn);
                this.hornEmitter.set3D(this.soundHorn, !this.isAnyListenerInside());
                this.hornEmitter.setVolume(this.soundHorn, 1.0F);
                this.hornEmitter.setPitch(this.soundHorn, 1.0F);
                if (!GameClient.client) {
                    WorldSoundManager.instance
                        .addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0F, 1.0F, false, true, false, false, true);
                }

                IsoGameCharacter driver = this.getDriver();
                IsoPlayer player = Type.tryCastTo(driver, IsoPlayer.class);
                if (player != null && player.isLocalPlayer()) {
                    player.triggerMusicIntensityEvent("VehicleHorn");
                }
            }
        }
    }

    public void onHornStop() {
        this.soundHornOn = false;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
        } else {
            if (this.script.getSounds().hornEnable && this.soundHorn != -1L) {
                this.hornEmitter.stopSound(this.soundHorn);
                this.soundHorn = -1L;
            }
        }
    }

    public boolean hasBackSignal() {
        return this.script != null && this.script.getSounds().backSignalEnable;
    }

    public boolean isBackSignalEmitting() {
        return this.soundBackMoveSignal != -1L;
    }

    public void onBackMoveSignalStart() {
        this.soundBackMoveOn = true;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
        } else {
            if (this.soundBackMoveSignal != -1L) {
                this.emitter.stopSound(this.soundBackMoveSignal);
            }

            if (this.script.getSounds().backSignalEnable) {
                this.soundBackMoveSignal = this.emitter.playSoundLoopedImpl(this.script.getSounds().backSignal);
                this.emitter.set3D(this.soundBackMoveSignal, !this.isAnyListenerInside());
            }
        }
    }

    public void onBackMoveSignalStop() {
        this.soundBackMoveOn = false;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
        } else {
            if (this.script.getSounds().backSignalEnable && this.soundBackMoveSignal != -1L) {
                this.emitter.stopSound(this.soundBackMoveSignal);
                this.soundBackMoveSignal = -1L;
            }
        }
    }

    public int getLightbarLightsMode() {
        return this.lightbarLightsMode.get();
    }

    public void setLightbarLightsMode(int mode) {
        this.lightbarLightsMode.set(mode);
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
        }
    }

    public int getLightbarSirenMode() {
        return this.lightbarSirenMode.get();
    }

    public void setLightbarSirenMode(int mode) {
        if (this.soundSirenSignal != -1L) {
            this.getEmitter().stopSound(this.soundSirenSignal);
            this.soundSirenSignal = -1L;
        }

        this.lightbarSirenMode.set(mode);
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 1024);
        } else {
            if (this.lightbarSirenMode.isEnable() && this.getBatteryCharge() > 0.0F) {
                this.soundSirenSignal = this.getEmitter().playSoundLoopedImpl(this.lightbarSirenMode.getSoundName(this.script.getLightbar()));
                this.getEmitter().set3D(this.soundSirenSignal, !this.isAnyListenerInside());
            }
        }
    }

    public HashMap<String, String> getChoosenParts() {
        return this.choosenParts;
    }

    public float getMass() {
        float tempMass = this.mass;
        if (tempMass < 0.0F) {
            tempMass = 1.0F;
        }

        return tempMass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public float getInitialMass() {
        return this.initialMass;
    }

    public void setInitialMass(float initialMass) {
        this.initialMass = initialMass;
    }

    public void updateTotalMass() {
        float plusMass = 0.0F;

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                plusMass += part.getItemContainer().getCapacityWeight();
            }

            if (part.getInventoryItem() != null) {
                plusMass += part.<InventoryItem>getInventoryItem().getWeight();
            }
        }

        this.setMass(Math.round(this.getInitialMass() + plusMass));
        if (this.physics != null && !GameServer.server) {
            Bullet.setVehicleMass(this.vehicleId, this.getMass());
        }
    }

    public float getBrakingForce() {
        return this.brakingForce;
    }

    public void setBrakingForce(float brakingForce) {
        this.brakingForce = brakingForce;
    }

    public float getBaseQuality() {
        return this.baseQuality;
    }

    public void setBaseQuality(float baseQuality) {
        this.baseQuality = baseQuality;
    }

    public float getCurrentSteering() {
        return this.currentSteering;
    }

    public void setCurrentSteering(float currentSteering) {
        this.currentSteering = currentSteering;
    }

    public boolean isDoingOffroad() {
        if (this.getCurrentSquare() == null) {
            return false;
        } else {
            IsoObject floor = this.getCurrentSquare().getFloor();
            if (floor != null && floor.getSprite() != null) {
                String spriteName = floor.getSprite().getName();
                return spriteName == null
                    ? false
                    : !spriteName.contains("carpentry_02") && !spriteName.contains("blends_street") && !spriteName.contains("floors_exterior_street");
            } else {
                return false;
            }
        }
    }

    public boolean isBraking() {
        return this.isBraking;
    }

    public void setBraking(boolean isBraking) {
        this.isBraking = isBraking;
        if (isBraking && this.brakeBetweenUpdatesSpeed == 0.0F) {
            this.brakeBetweenUpdatesSpeed = this.getCurrentAbsoluteSpeedKmHour();
        }
    }

    /**
     * Update the stats of the part depending on condition
     */
    public void updatePartStats() {
        this.setBrakingForce(0.0F);
        this.engineLoudness = (int)(this.getScript().getEngineLoudness() * SandboxOptions.instance.zombieAttractionMultiplier.getValue() / 2.0);
        boolean foundMuffler = false;

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getInventoryItem() != null) {
                if (part.<InventoryItem>getInventoryItem().getBrakeForce() > 0.0F) {
                    float newBrakeForce = VehiclePart.getNumberByCondition(
                        part.<InventoryItem>getInventoryItem().getBrakeForce(), part.<InventoryItem>getInventoryItem().getCondition(), 5.0F
                    );
                    newBrakeForce += newBrakeForce / 50.0F * part.getMechanicSkillInstaller();
                    this.setBrakingForce(this.getBrakingForce() + newBrakeForce);
                }

                if (part.<InventoryItem>getInventoryItem().getWheelFriction() > 0.0F) {
                    part.setWheelFriction(0.0F);
                    float friction = VehiclePart.getNumberByCondition(
                        part.<InventoryItem>getInventoryItem().getWheelFriction(), part.<InventoryItem>getInventoryItem().getCondition(), 0.2F
                    );
                    friction += 0.1F * part.getMechanicSkillInstaller();
                    friction = Math.min(2.3F, friction);
                    part.setWheelFriction(friction);
                }

                if (part.<InventoryItem>getInventoryItem().getSuspensionCompression() > 0.0F) {
                    part.setSuspensionCompression(
                        VehiclePart.getNumberByCondition(
                            part.<InventoryItem>getInventoryItem().getSuspensionCompression(), part.<InventoryItem>getInventoryItem().getCondition(), 0.6F
                        )
                    );
                    part.setSuspensionDamping(
                        VehiclePart.getNumberByCondition(
                            part.<InventoryItem>getInventoryItem().getSuspensionDamping(), part.<InventoryItem>getInventoryItem().getCondition(), 0.6F
                        )
                    );
                }

                if (part.<InventoryItem>getInventoryItem().getEngineLoudness() > 0.0F) {
                    part.setEngineLoudness(
                        VehiclePart.getNumberByCondition(
                            part.<InventoryItem>getInventoryItem().getEngineLoudness(), part.<InventoryItem>getInventoryItem().getCondition(), 10.0F
                        )
                    );
                    this.engineLoudness = (int)(this.engineLoudness * (1.0F + (100.0F - part.getEngineLoudness()) / 100.0F));
                    foundMuffler = true;
                }

                if (part.<InventoryItem>getInventoryItem().getDurability() > 0.0F) {
                    part.setDurability(part.<InventoryItem>getInventoryItem().getDurability());
                }
            }
        }

        if (!foundMuffler) {
            this.engineLoudness *= 2;
        }
    }

    public void transmitEngine() {
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 4);
        }
    }

    public void setRust(float rust) {
        this.rust = PZMath.clamp(rust, 0.0F, 1.0F);
    }

    public float getRust() {
        return this.rust;
    }

    public void transmitRust() {
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 4096);
        }
    }

    public void transmitColorHSV() {
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 4096);
        }
    }

    public void transmitSkinIndex() {
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 4096);
        }
    }

    public void updateBulletStats() {
        if (!this.getScriptName().contains("Burnt") && WorldSimulation.instance.created) {
            float[] data = wheelParams;
            double susp = 0.0;
            double period = 2.4;
            int chanceofbump = 100;
            float frictionMul = 1.0F;
            float currentAbsoluteSpeedKmHour = this.getCurrentAbsoluteSpeedKmHour();
            if (this.isInForest() && this.isDoingOffroad() && currentAbsoluteSpeedKmHour > 1.0F) {
                susp = Rand.Next(0.04F, 0.13F);
                frictionMul = 0.7F;
                chanceofbump = 15;
            } else if (this.isDoingOffroad() && currentAbsoluteSpeedKmHour > 1.0F) {
                chanceofbump = 25;
                susp = Rand.Next(0.02F, 0.1F);
                frictionMul = 0.7F;
            } else if (currentAbsoluteSpeedKmHour > 1.0F && Rand.Next(100) < 10) {
                susp = Rand.Next(0.01F, 0.05F);
            } else {
                susp = 0.0;
            }

            if (RainManager.isRaining()) {
                frictionMul -= 0.3F;
            }

            Vector3f worldPos = TL_vector3f_pool.get().alloc();

            for (int i = 0; i < this.script.getWheelCount(); i++) {
                this.updateBulletStatsWheel(i, data, worldPos, frictionMul, chanceofbump, 2.4, susp);
            }

            TL_vector3f_pool.get().release(worldPos);
            if (SystemDisabler.getdoVehicleLowRider() && this.isKeyboardControlled()) {
                float lowRiderLevel = 0.25F;
                float lowRiderK = 1.0F;
                if (GameKeyboard.isKeyDown(79)) {
                    lowRiderParam[0] = lowRiderParam[0] + (0.25F - lowRiderParam[0]) * 1.0F;
                } else {
                    lowRiderParam[0] = lowRiderParam[0] + (0.0F - lowRiderParam[0]) * 0.05F;
                }

                if (GameKeyboard.isKeyDown(80)) {
                    lowRiderParam[1] = lowRiderParam[1] + (0.25F - lowRiderParam[1]) * 1.0F;
                } else {
                    lowRiderParam[1] = lowRiderParam[1] + (0.0F - lowRiderParam[1]) * 0.05F;
                }

                if (GameKeyboard.isKeyDown(75)) {
                    lowRiderParam[2] = lowRiderParam[2] + (0.25F - lowRiderParam[2]) * 1.0F;
                } else {
                    lowRiderParam[2] = lowRiderParam[2] + (0.0F - lowRiderParam[2]) * 0.05F;
                }

                if (GameKeyboard.isKeyDown(76)) {
                    lowRiderParam[3] = lowRiderParam[3] + (0.25F - lowRiderParam[3]) * 1.0F;
                } else {
                    lowRiderParam[3] = lowRiderParam[3] + (0.0F - lowRiderParam[3]) * 0.05F;
                }

                data[5] = lowRiderParam[0];
                data[11] = lowRiderParam[1];
                data[17] = lowRiderParam[2];
                data[23] = lowRiderParam[3];
            }

            Bullet.setVehicleParams(this.vehicleId, data);
        }
    }

    private void updateBulletStatsWheel(int wheelIndex, float[] data, Vector3f worldPos, float frictionMul, int chanceofbump, double period, double susp) {
        int offset = wheelIndex * 6;
        VehicleScript.Wheel scriptWheel = this.script.getWheel(wheelIndex);
        Vector3f wheelPos = this.getWorldPos(scriptWheel.offset.x, scriptWheel.offset.y, scriptWheel.offset.z, worldPos);
        VehiclePart part = this.getPartById("Tire" + scriptWheel.getId());
        VehiclePart partSuspension = this.getPartById("Suspension" + scriptWheel.getId());
        if (part != null && part.getInventoryItem() != null) {
            data[offset + 0] = 1.0F;
            data[offset + 1] = Math.min(part.getContainerContentAmount() / (part.getContainerCapacity() - 10), 1.0F);
            data[offset + 2] = frictionMul * part.getWheelFriction();
            if (partSuspension != null && partSuspension.getInventoryItem() != null) {
                data[offset + 3] = partSuspension.getSuspensionDamping();
                data[offset + 4] = partSuspension.getSuspensionCompression();
            } else {
                data[offset + 3] = 0.1F;
                data[offset + 4] = 0.1F;
            }

            if (chanceofbump > 0 && Rand.Next(chanceofbump) == 0) {
                data[offset + 5] = (float)(Math.sin(period * wheelPos.x()) * Math.sin(period * wheelPos.y()) * susp);
            } else {
                data[offset + 5] = 0.0F;
            }
        } else {
            data[offset + 0] = 0.0F;
            data[offset + 1] = 30.0F;
            data[offset + 2] = 0.0F;
            data[offset + 3] = 2.88F;
            data[offset + 4] = 3.83F;
            if (Rand.Next(chanceofbump) == 0) {
                data[offset + 5] = (float)(Math.sin(period * wheelPos.x()) * Math.sin(period * wheelPos.y()) * susp);
            } else {
                data[offset + 5] = 0.0F;
            }
        }
    }

    /**
     * Used in mechanics UI, we enable the vehicle in Bullet when starting mechanics so physic will be updated. When
     *  we close the UI, we should
     *  disable it in Bullet, expect if the engine is running.
     */
    public void setActiveInBullet(boolean active) {
    }

    public boolean areAllDoorsLocked() {
        for (int seat = 0; seat < this.getMaxPassengers(); seat++) {
            VehiclePart part = this.getPassengerDoor(seat);
            if (part != null && part.getDoor() != null && !part.getDoor().isLocked()) {
                return false;
            }
        }

        return true;
    }

    public boolean isAnyDoorLocked() {
        for (int seat = 0; seat < this.getMaxPassengers(); seat++) {
            VehiclePart part = this.getPassengerDoor(seat);
            if (part != null && part.getDoor() != null && part.getDoor().isLocked()) {
                return true;
            }
        }

        return false;
    }

    public float getRemainingFuelPercentage() {
        VehiclePart gasTank = this.getPartById("GasTank");
        return gasTank == null ? 0.0F : gasTank.getContainerContentAmount() / gasTank.getContainerCapacity() * 100.0F;
    }

    public int getMechanicalID() {
        return this.mechanicalId;
    }

    public void setMechanicalID(int mechanicalId) {
        this.mechanicalId = mechanicalId;
    }

    public boolean needPartsUpdate() {
        return this.needPartsUpdate;
    }

    public void setNeedPartsUpdate(boolean needPartsUpdate) {
        this.needPartsUpdate = needPartsUpdate;
    }

    public VehiclePart getHeater() {
        return this.getPartById("Heater");
    }

    public int windowsOpen() {
        int result = 0;

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.window != null && part.window.open) {
                result++;
            }
        }

        return result;
    }

    public boolean isAlarmed() {
        return this.alarmed;
    }

    public void setAlarmed(boolean alarmed) {
        this.alarmed = alarmed;
        if (alarmed) {
            this.setPreviouslyEntered(false);
        }
    }

    public void triggerAlarm() {
        if (this.alarmed && !this.previouslyEntered) {
            this.alarmed = false;
            this.alarmStartTime = GameTime.getInstance().getWorldAgeHours();
            this.alarmAccumulator = 0.0F;
            this.chooseAlarmSound();
            VehicleScript.Sounds sounds = this.script.getSounds();
            boolean bLooping = sounds.alarmLoop.contains(this.chosenAlarmSound);
            if (bLooping) {
                this.onAlarmStart();
            }
        } else {
            this.alarmed = false;
        }
    }

    private void doAlarm() {
        if (this.alarmStartTime > 0.0) {
            if (this.getBatteryCharge() <= 0.0F) {
                if (this.soundAlarmOn) {
                    this.onAlarmStop();
                }

                this.alarmStartTime = 0.0;
                return;
            }

            double worldAge = GameTime.getInstance().getWorldAgeHours();
            if (this.alarmStartTime > worldAge) {
                this.alarmStartTime = worldAge;
            }

            if (worldAge >= this.alarmStartTime + 0.66 * GameTime.getInstance().getDeltaMinutesPerDay()) {
                this.onAlarmStop();
                this.setHeadlightsOn(false);
                this.alarmStartTime = 0.0;
                return;
            }

            this.chooseAlarmSound();
            VehicleScript.Sounds sounds = this.script.getSounds();
            boolean bLooping = sounds.alarmLoop.contains(this.chosenAlarmSound);
            if (bLooping && (this.alarmEmitter == null || !this.alarmEmitter.isPlaying(this.soundAlarm))) {
                this.onAlarmStart();
            }

            this.alarmAccumulator = this.alarmAccumulator + GameTime.instance.getThirtyFPSMultiplier();
            int t = (int)this.alarmAccumulator / 24;
            if (!this.headlightsOn && t % 2 == 0) {
                if (!bLooping) {
                    this.onAlarmStart();
                }

                this.setHeadlightsOn(true);
            }

            if (this.headlightsOn && t % 2 == 1) {
                if (!bLooping) {
                    this.onAlarmStop();
                }

                this.setHeadlightsOn(false);
            }

            this.checkMusicIntensityEvent_AlarmNearby();
        }
    }

    private void checkMusicIntensityEvent_AlarmNearby() {
        if (!GameServer.server) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && !player.hasTrait(CharacterTrait.DEAF) && !player.isDead()) {
                    float distSq = IsoUtils.DistanceToSquared(this.getX(), this.getY(), player.getX(), player.getY());
                    if (!(distSq > 2500.0F)) {
                        player.triggerMusicIntensityEvent("AlarmNearby");
                        break;
                    }
                }
            }
        }
    }

    public boolean isMechanicUIOpen() {
        return this.mechanicUiOpen;
    }

    public void setMechanicUIOpen(boolean mechanicUiOpen) {
        this.mechanicUiOpen = mechanicUiOpen;
    }

    public void damagePlayers(float damage) {
        if (SandboxOptions.instance.playerDamageFromCrash.getValue()) {
            if (!GameClient.client) {
                for (int i = 0; i < this.passengers.length; i++) {
                    if (this.getPassenger(i).character != null) {
                        IsoGameCharacter chr = this.getPassenger(i).character;
                        if (!chr.isGodMod() && !GameClient.client) {
                            this.addRandomDamageFromCrash(chr, damage);
                            LuaEventManager.triggerEvent("OnPlayerGetDamage", chr, "CARCRASHDAMAGE", damage);
                            if (GameServer.server && chr instanceof IsoPlayer player) {
                                INetworkPacket.send(player, PacketTypes.PacketType.PlayerDamage, player);
                            }
                        }
                    }
                }
            }
        }
    }

    public void addRandomDamageFromCrash(IsoGameCharacter chr, float damage) {
        int damagedBodyPartNum = 1;
        if (damage > 40.0F) {
            damagedBodyPartNum = Rand.Next(1, 3);
        }

        if (damage > 70.0F) {
            damagedBodyPartNum = Rand.Next(2, 4);
        }

        int brokenGlass = 0;

        for (int i = 0; i < chr.getVehicle().getPartCount(); i++) {
            VehiclePart part = chr.getVehicle().getPartByIndex(i);
            if (part.window != null && part.getCondition() < 15) {
                brokenGlass++;
            }
        }

        for (int j = 0; j < damagedBodyPartNum; j++) {
            int bodyPart = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            BodyPart part = chr.getBodyDamage().getBodyPart(BodyPartType.FromIndex(bodyPart));
            float realDamage = Math.max(Rand.Next(damage - 15.0F, damage), 5.0F);
            if (chr.hasTrait(CharacterTrait.FAST_HEALER)) {
                realDamage *= 0.8F;
            } else if (chr.hasTrait(CharacterTrait.SLOW_HEALER)) {
                realDamage *= 1.2F;
            }

            switch (SandboxOptions.instance.injurySeverity.getValue()) {
                case 1:
                    realDamage *= 0.5F;
                    break;
                case 3:
                    realDamage *= 1.5F;
            }

            realDamage *= this.getScript().getPlayerDamageProtection();
            realDamage *= 0.9F;
            part.AddDamage(realDamage);
            if (realDamage > 40.0F && Rand.Next(12) == 0) {
                part.generateDeepWound();
            } else if (realDamage > 50.0F && Rand.Next(10) == 0 && SandboxOptions.instance.boneFracture.getValue()) {
                if (part.getType() != BodyPartType.Neck && part.getType() != BodyPartType.Groin) {
                    part.generateFracture(Rand.Next(Rand.Next(10.0F, realDamage + 10.0F), Rand.Next(realDamage + 20.0F, realDamage + 30.0F)));
                } else {
                    part.generateDeepWound();
                }
            }

            if (realDamage > 30.0F && Rand.Next(12 - brokenGlass) == 0) {
                part = chr.getBodyDamage().setScratchedWindow();
                if (Rand.Next(5) == 0) {
                    part.generateDeepWound();
                    part.setHaveGlass(true);
                }
            }
        }
    }

    public boolean isTrunkLocked() {
        VehiclePart trunk = this.getPartById("TrunkDoor");
        if (trunk == null) {
            trunk = this.getPartById("DoorRear");
        }

        return trunk != null && trunk.getDoor() != null && trunk.getInventoryItem() != null ? trunk.getDoor().isLocked() : false;
    }

    public void setTrunkLocked(boolean locked) {
        VehiclePart trunk = this.getPartById("TrunkDoor");
        if (trunk == null) {
            trunk = this.getPartById("DoorRear");
        }

        if (trunk != null && trunk.getDoor() != null && trunk.getInventoryItem() != null) {
            trunk.getDoor().setLocked(locked);
            if (GameServer.server) {
                this.transmitPartDoor(trunk);
            }
        }
    }

    public VehiclePart getNearestBodyworkPart(IsoGameCharacter chr) {
        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (("door".equals(part.getCategory()) || "bodywork".equals(part.getCategory())) && this.isInArea(part.getArea(), chr) && part.getCondition() > 0) {
                return part;
            }
        }

        return null;
    }

    public double getSirenStartTime() {
        return this.sirenStartTime;
    }

    public void setSirenStartTime(double worldAgeHours) {
        this.sirenStartTime = worldAgeHours;
    }

    public boolean sirenShutoffTimeExpired() {
        double shutoffHours = SandboxOptions.instance.sirenShutoffHours.getValue();
        if (shutoffHours <= 0.0) {
            return false;
        } else {
            double worldAge = GameTime.instance.getWorldAgeHours();
            if (this.sirenStartTime > worldAge) {
                this.sirenStartTime = worldAge;
            }

            return this.sirenStartTime + shutoffHours < worldAge;
        }
    }

    public void repair() {
        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            part.repair();
        }

        this.rust = 0.0F;
        this.transmitRust();
        this.bloodIntensity.clear();
        this.transmitBlood();
        this.doBloodOverlay();
    }

    public boolean isAnyListenerInside() {
        for (int seat = 0; seat < this.getMaxPassengers(); seat++) {
            IsoGameCharacter chr = this.getCharacter(seat);
            if (chr instanceof IsoPlayer isoPlayer && isoPlayer.isLocalPlayer() && !chr.hasTrait(CharacterTrait.DEAF)) {
                return true;
            }
        }

        return false;
    }

    public boolean couldCrawlerAttackPassenger(IsoGameCharacter chr) {
        int seat = this.getSeat(chr);
        return seat == -1 ? false : false;
    }

    public boolean isGoodCar() {
        return this.isGoodCar;
    }

    public void setGoodCar(boolean isGoodCar) {
        this.isGoodCar = isGoodCar;
    }

    public InventoryItem getCurrentKey() {
        return !this.ignitionSwitch.getItems().isEmpty() ? this.ignitionSwitch.getItems().get(0) : null;
    }

    public void setCurrentKey(InventoryItem currentKey) {
        this.currentKey = currentKey;
        this.ignitionSwitch.addItem(currentKey);
    }

    public boolean isInForest() {
        return this.getSquare() != null
            && this.getSquare().getZone() != null
            && (
                "Forest".equals(this.getSquare().getZone().getType())
                    || "DeepForest".equals(this.getSquare().getZone().getType())
                    || "FarmLand".equals(this.getSquare().getZone().getType())
            );
    }

    public boolean shouldNotHaveLoot() {
        return this.getSquare() != null
                && IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) != null
                && Objects.equals(
                    Objects.requireNonNull(
                            IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ())
                        )
                        .name,
                    "junkyard"
                )
            ? true
            : this.getSquare() != null
                && IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) != null
                && Objects.equals(
                    Objects.requireNonNull(
                            IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ())
                        )
                        .name,
                    "luxuryDealership"
                );
    }

    public boolean isInTrafficJam() {
        if (this.getSquare() == null) {
            return false;
        } else if (IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) == null) {
            return false;
        } else {
            String name = Objects.requireNonNull(
                    IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ())
                )
                .name;
            return name == null ? false : name.contains("trafficjam") || name.contains("burnt");
        }
    }

    /**
     * Give the offroad efficiency of the car, based on car's script + where the vehicle is (in forest you get more
     *  damage than vegitation)
     *  Currently x2 to balance things
     */
    public float getOffroadEfficiency() {
        return this.isInForest() ? this.script.getOffroadEfficiency() * 1.5F : this.script.getOffroadEfficiency() * 2.0F;
    }

    public void doChrHitImpulse(IsoObject chr) {
        float SPEED_CAP = 22.0F;
        Vector3f velocity = this.getLinearVelocity(TL_vector3f_pool.get().alloc());
        velocity.y = 0.0F;
        Vector3f v = TL_vector3f_pool.get().alloc();
        v.set(this.getX() - chr.getX(), 0.0F, this.getZ() - chr.getY());
        v.normalize();
        velocity.mul(v);
        TL_vector3f_pool.get().release(v);
        float speed = velocity.length();
        speed = Math.min(speed, 22.0F);
        if (speed < 0.05F) {
            TL_vector3f_pool.get().release(velocity);
        } else {
            if (GameServer.server) {
                if (chr instanceof IsoZombie isoZombie) {
                    isoZombie.setThumpFlag(1);
                }
            } else {
                SoundManager.instance.PlayWorldSound("ZombieThumpGeneric", chr.square, 0.0F, 20.0F, 0.9F, true);
            }

            Vector3f impulse = TL_vector3f_pool.get().alloc();
            impulse.set(this.getX() - chr.getX(), 0.0F, this.getY() - chr.getY());
            impulse.normalize();
            velocity.normalize();
            float dot = velocity.dot(impulse);
            TL_vector3f_pool.get().release(velocity);
            TL_vector3f_pool.get().release(impulse);
            this.ApplyImpulse(chr, this.getFudgedMass() * 3.0F * speed / 22.0F * Math.abs(dot));
        }
    }

    public boolean isDoColor() {
        return this.doColor;
    }

    public void setDoColor(boolean doColor) {
        this.doColor = doColor;
    }

    public float getBrakeSpeedBetweenUpdate() {
        return this.brakeBetweenUpdatesSpeed;
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
    }

    public void setColor(float value, float saturation, float hue) {
        this.colorValue = value;
        this.colorSaturation = saturation;
        this.colorHue = hue;
    }

    public void setColorHSV(float hue, float saturation, float value) {
        this.colorHue = hue;
        this.colorSaturation = saturation;
        this.colorValue = value;
    }

    public float getColorHue() {
        return this.colorHue;
    }

    public float getColorSaturation() {
        return this.colorSaturation;
    }

    public float getColorValue() {
        return this.colorValue;
    }

    public boolean isRemovedFromWorld() {
        return this.removedFromWorld;
    }

    public float getInsideTemperature() {
        VehiclePart part = this.getPartById("PassengerCompartment");
        float retval = 0.0F;
        if (part != null && part.getModData() != null) {
            if (part.getModData().rawget("temperature") != null) {
                retval += ((Double)part.getModData().rawget("temperature")).floatValue();
            }

            if (part.getModData().rawget("windowtemperature") != null) {
                retval += ((Double)part.getModData().rawget("windowtemperature")).floatValue();
            }
        }

        return retval;
    }

    public AnimationPlayer getAnimationPlayer() {
        String modelName = this.getScript().getModel().file;
        Model model = ModelManager.instance.getLoadedModel(modelName);
        if (model != null && !model.isStatic) {
            if (this.animPlayer != null && this.animPlayer.getModel() != model) {
                this.animPlayer = Pool.tryRelease(this.animPlayer);
            }

            if (this.animPlayer == null) {
                this.animPlayer = AnimationPlayer.alloc(model);
            }

            return this.animPlayer;
        } else {
            return null;
        }
    }

    public void releaseAnimationPlayers() {
        this.animPlayer = Pool.tryRelease(this.animPlayer);
        PZArrayUtil.forEach(this.models, BaseVehicle.ModelInfo::releaseAnimationPlayer);
    }

    public void setAddThumpWorldSound(boolean add) {
        this.addThumpWorldSound = add;
    }

    public void createImpulse(Vector3f vec) {
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        VehiclePart lightbar = this.getPartById("lightbar");
        if (lightbar != null) {
            if (lightbar.getCondition() <= 0) {
                thumper.setThumpTarget(null);
            }

            VehiclePart part = this.getUseablePart((IsoGameCharacter)thumper);
            if (part != null) {
                part.setCondition(part.getCondition() - Rand.Next(1, 5));
            }

            lightbar.setCondition(lightbar.getCondition() - Rand.Next(1, 5));
        }
    }

    @Override
    public void WeaponHit(IsoGameCharacter chr, HandWeapon weapon) {
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        return null;
    }

    @Override
    public float getThumpCondition() {
        return 1.0F;
    }

    public boolean isRegulator() {
        return this.regulator;
    }

    public void setRegulator(boolean regulator) {
        this.regulator = regulator;
    }

    public float getRegulatorSpeed() {
        return this.regulatorSpeed;
    }

    public void setRegulatorSpeed(float regulatorSpeed) {
        this.regulatorSpeed = regulatorSpeed;
    }

    public float getCurrentSpeedForRegulator() {
        return (float)Math.max(5.0 * Math.floor(this.jniSpeed / 5.0F), 5.0);
    }

    public void setVehicleTowing(BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        this.vehicleTowing = vehicleB;
        this.vehicleTowingId = this.vehicleTowing == null ? -1 : this.vehicleTowing.getSqlId();
        this.towAttachmentSelf = attachmentA;
        this.towAttachmentOther = attachmentB;
        this.rowConstraintZOffset = 0.0F;
    }

    public void setVehicleTowedBy(BaseVehicle vehicleA, String attachmentA, String attachmentB) {
        this.vehicleTowedBy = vehicleA;
        this.vehicleTowedById = this.vehicleTowedBy == null ? -1 : this.vehicleTowedBy.getSqlId();
        this.towAttachmentSelf = attachmentB;
        this.towAttachmentOther = attachmentA;
        this.rowConstraintZOffset = 0.0F;
    }

    public BaseVehicle getVehicleTowing() {
        return this.vehicleTowing;
    }

    public BaseVehicle getVehicleTowedBy() {
        return this.vehicleTowedBy;
    }

    public boolean attachmentExist(String attachmentName) {
        VehicleScript script = this.getScript();
        if (script == null) {
            return false;
        } else {
            ModelAttachment attach = script.getAttachmentById(attachmentName);
            return attach != null;
        }
    }

    public Vector3f getAttachmentLocalPos(String attachmentName, Vector3f v) {
        VehicleScript script = this.getScript();
        if (script == null) {
            return null;
        } else {
            ModelAttachment attach = script.getAttachmentById(attachmentName);
            if (attach == null) {
                return null;
            } else {
                v.set(attach.getOffset());
                return script.getModel() == null ? v : v.add(script.getModel().getOffset());
            }
        }
    }

    public Vector3f getAttachmentWorldPos(String attachmentName, Vector3f v) {
        v = this.getAttachmentLocalPos(attachmentName, v);
        return v == null ? null : this.getWorldPos(v, v);
    }

    public void setForceBrake() {
        this.getController().clientControls.forceBrake = System.currentTimeMillis();
    }

    public Vector3f getTowingLocalPos(String attachmentName, Vector3f v) {
        return this.getAttachmentLocalPos(attachmentName, v);
    }

    public Vector3f getTowedByLocalPos(String attachmentName, Vector3f v) {
        return this.getAttachmentLocalPos(attachmentName, v);
    }

    public Vector3f getTowingWorldPos(String attachmentName, Vector3f v) {
        v = this.getTowingLocalPos(attachmentName, v);
        return v == null ? null : this.getWorldPos(v, v);
    }

    public Vector3f getTowedByWorldPos(String attachmentName, Vector3f v) {
        v = this.getTowedByLocalPos(attachmentName, v);
        return v == null ? null : this.getWorldPos(v, v);
    }

    public Vector3f getPlayerTrailerLocalPos(String attachmentName, boolean left, Vector3f v) {
        ModelAttachment attach = this.getScript().getAttachmentById(attachmentName);
        if (attach == null) {
            return null;
        } else {
            Vector3f ext = this.getScript().getExtents();
            Vector3f com = this.getScript().getCenterOfMassOffset();
            float x = com.x + ext.x / 2.0F + 0.3F + 0.05F;
            if (!left) {
                x *= -1.0F;
            }

            return attach.getOffset().z > 0.0F ? v.set(x, 0.0F, com.z + ext.z / 2.0F + 0.3F + 0.05F) : v.set(x, 0.0F, com.z - (ext.z / 2.0F + 0.3F + 0.05F));
        }
    }

    public Vector3f getPlayerTrailerWorldPos(String attachmentName, boolean left, Vector3f v) {
        v = this.getPlayerTrailerLocalPos(attachmentName, left, v);
        if (v == null) {
            return null;
        } else {
            this.getWorldPos(v, v);
            v.z = PZMath.fastfloor(this.getZ());
            Vector3f v2 = this.getTowingWorldPos(attachmentName, TL_vector3f_pool.get().alloc());
            boolean blocked = PolygonalMap2.instance.lineClearCollide(v.x, v.y, v2.x, v2.y, PZMath.fastfloor(this.getZ()), this, false, false);
            TL_vector3f_pool.get().release(v2);
            return blocked ? null : v;
        }
    }

    private void drawTowingRope() {
        BaseVehicle vehicleB = this.getVehicleTowing();
        if (vehicleB != null) {
            BaseVehicle.Vector3fObjectPool pool = TL_vector3f_pool.get();
            Vector3f v2 = this.getAttachmentWorldPos("trailerfront", pool.alloc());
            ModelAttachment attach = this.script.getAttachmentById("trailerfront");
            if (attach != null) {
                v2.set(attach.getOffset());
            }

            Vector2 tempVector2 = new Vector2();
            tempVector2.x = vehicleB.getX();
            tempVector2.y = vehicleB.getY();
            tempVector2.x = tempVector2.x - this.getX();
            tempVector2.y = tempVector2.y - this.getY();
            tempVector2.setLength(2.0F);
            this.drawDirectionLine(tempVector2, tempVector2.getLength(), 1.0F, 0.5F, 0.5F);
        }
    }

    public void drawDirectionLine(Vector2 dir, float length, float r, float g, float b) {
        float x2 = this.getX() + dir.x * length;
        float y2 = this.getY() + dir.y * length;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, this.getZ(), 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, this.getZ(), 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5F, 1);
    }

    public void addPointConstraint(IsoPlayer player, BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        this.addPointConstraint(player, vehicleB, attachmentA, attachmentB, false);
    }

    public void addPointConstraint(IsoPlayer player, BaseVehicle vehicleB, String attachmentA, String attachmentB, Boolean remote) {
        this.setPreviouslyMoved(true);
        if (vehicleB == null
            || player != null
                && (
                    IsoUtils.DistanceToSquared(player.getX(), player.getY(), this.getX(), this.getY()) > 100.0F
                        || IsoUtils.DistanceToSquared(player.getX(), player.getY(), vehicleB.getX(), vehicleB.getY()) > 100.0F
                )) {
            DebugLog.Vehicle.warn("The " + player.getUsername() + " user attached vehicles at a long distance");
        }

        this.breakConstraint(true, remote);
        vehicleB.breakConstraint(true, remote);
        BaseVehicle.Vector3fObjectPool pool = TL_vector3f_pool.get();
        Vector3f v1 = this.getTowingLocalPos(attachmentA, pool.alloc());
        Vector3f v2 = vehicleB.getTowedByLocalPos(attachmentB, pool.alloc());
        if (v1 != null && v2 != null) {
            if (!GameServer.server) {
                if (!this.getScriptName().contains("Trailer") && !vehicleB.getScriptName().contains("Trailer")) {
                    this.constraintTowing = Bullet.addRopeConstraint(this.vehicleId, vehicleB.vehicleId, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, 1.5F);
                } else {
                    this.constraintTowing = Bullet.addPointConstraint(this.vehicleId, vehicleB.vehicleId, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z);
                }
            }

            vehicleB.constraintTowing = this.constraintTowing;
            this.setVehicleTowing(vehicleB, attachmentA, attachmentB);
            vehicleB.setVehicleTowedBy(this, attachmentA, attachmentB);
            pool.release(v1);
            pool.release(v2);
            this.constraintChanged();
            vehicleB.constraintChanged();
            if (GameServer.server
                && player != null
                && this.netPlayerAuthorization == BaseVehicle.Authorization.Server
                && vehicleB.netPlayerAuthorization == BaseVehicle.Authorization.Server) {
                this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, player.onlineId);
                this.authSimulationTime = System.currentTimeMillis();
                vehicleB.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, player.onlineId);
                vehicleB.authSimulationTime = System.currentTimeMillis();
            }

            if (GameServer.server && !remote) {
                VehicleManager.instance.attachTowing(this, vehicleB, attachmentA, attachmentB);
            }
        } else {
            if (v1 != null) {
                pool.release(v1);
            }

            if (v2 != null) {
                pool.release(v2);
            }
        }
    }

    public void authorizationChanged(IsoGameCharacter character) {
        if (character != null) {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.Local, character.getOnlineID());
        } else {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
        }
    }

    public void constraintChanged() {
        long currentTimeMS = System.currentTimeMillis();
        this.setPhysicsActive(true);
        this.constraintChangedTime = currentTimeMS;
        if (GameServer.server) {
            if (this.getVehicleTowing() != null) {
                this.authorizationChanged(this.getDriver());
                this.getVehicleTowing().authorizationChanged(this.getDriver());
            } else if (this.getVehicleTowedBy() != null) {
                this.authorizationChanged(this.getVehicleTowedBy().getDriver());
                this.getVehicleTowedBy().authorizationChanged(this.getVehicleTowedBy().getDriver());
            } else {
                this.authorizationChanged(this.getDriver());
            }
        }
    }

    public void breakConstraint(boolean forgetID, boolean remote) {
        if (GameServer.server || this.constraintTowing != -1) {
            if (!GameServer.server) {
                Bullet.removeConstraint(this.constraintTowing);
            }

            this.constraintTowing = -1;
            if (this.vehicleTowing != null) {
                if (GameServer.server && !remote) {
                    VehicleManager.instance.detachTowing(this, this.vehicleTowing);
                }

                this.vehicleTowing.vehicleTowedBy = null;
                this.vehicleTowing.constraintTowing = -1;
                if (forgetID) {
                    this.vehicleTowingId = -1;
                    this.vehicleTowing.vehicleTowedById = -1;
                }

                this.vehicleTowing.constraintChanged();
                this.vehicleTowing = null;
            }

            if (this.vehicleTowedBy != null) {
                if (GameServer.server && !remote) {
                    VehicleManager.instance.detachTowing(this.vehicleTowedBy, this);
                }

                this.vehicleTowedBy.vehicleTowing = null;
                this.vehicleTowedBy.constraintTowing = -1;
                if (forgetID) {
                    this.vehicleTowedBy.vehicleTowingId = -1;
                    this.vehicleTowedById = -1;
                }

                this.vehicleTowedBy.constraintChanged();
                this.vehicleTowedBy = null;
            }

            this.constraintChanged();
        }
    }

    public boolean canAttachTrailer(BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        return this.canAttachTrailer(vehicleB, attachmentA, attachmentB, false);
    }

    public boolean canAttachTrailer(BaseVehicle vehicleB, String attachmentA, String attachmentB, boolean reconnect) {
        if (this == vehicleB || this.physics == null || this.constraintTowing != -1) {
            return false;
        } else if (vehicleB != null && vehicleB.physics != null && vehicleB.constraintTowing == -1) {
            BaseVehicle.Vector3fObjectPool pool = TL_vector3f_pool.get();
            Vector3f v1 = this.getTowingWorldPos(attachmentA, pool.alloc());
            Vector3f v2 = vehicleB.getTowedByWorldPos(attachmentB, pool.alloc());
            if (v1 != null && v2 != null) {
                float distSq = IsoUtils.DistanceToSquared(v1.x, v1.y, 0.0F, v2.x, v2.y, 0.0F);
                pool.release(v1);
                pool.release(v2);
                ModelAttachment attachA = this.script.getAttachmentById(attachmentA);
                ModelAttachment attachB = vehicleB.script.getAttachmentById(attachmentB);
                if (attachA != null && attachA.getCanAttach() != null && !attachA.getCanAttach().contains(attachmentB)) {
                    return false;
                } else if (attachB != null && attachB.getCanAttach() != null && !attachB.getCanAttach().contains(attachmentA)) {
                    return false;
                } else {
                    boolean isTrailer = this.getScriptName().contains("Trailer") || vehicleB.getScriptName().contains("Trailer");
                    return distSq < (reconnect ? 10.0F : (isTrailer ? 1.0F : 4.0F));
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void tryReconnectToTowedVehicle() {
        if (GameClient.client) {
            short towedID = VehicleManager.instance.getTowedVehicleID(this.vehicleId);
            if (towedID != -1) {
                BaseVehicle vehicleToTow = VehicleManager.instance.getVehicleByID(towedID);
                if (vehicleToTow != null) {
                    if (this.canAttachTrailer(vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, true)) {
                        this.addPointConstraint(null, vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, true);
                    }
                }
            }
        } else if (this.vehicleTowing == null) {
            if (this.vehicleTowingId != -1) {
                BaseVehicle vehicleToTow = null;
                ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();

                for (int i = 0; i < vehicles.size(); i++) {
                    BaseVehicle vehicle = vehicles.get(i);
                    if (vehicle.getSqlId() == this.vehicleTowingId) {
                        vehicleToTow = vehicle;
                        break;
                    }
                }

                if (vehicleToTow != null) {
                    if (this.canAttachTrailer(vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, true)) {
                        this.addPointConstraint(null, vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, false);
                    }
                }
            }
        }
    }

    public void positionTrailer(BaseVehicle trailer) {
        if (trailer != null) {
            BaseVehicle.Vector3fObjectPool pool = TL_vector3f_pool.get();
            Vector3f v1 = this.getTowingWorldPos("trailer", pool.alloc());
            Vector3f v2 = trailer.getTowedByWorldPos("trailer", pool.alloc());
            if (v1 != null && v2 != null) {
                v2.sub(trailer.getX(), trailer.getY(), trailer.getZ());
                v1.sub(v2);
                Transform xfrm = trailer.getWorldTransform(allocTransform());
                xfrm.origin.set(v1.x - WorldSimulation.instance.offsetX, trailer.jniTransform.origin.y, v1.y - WorldSimulation.instance.offsetY);
                trailer.setWorldTransform(xfrm);
                releaseTransform(xfrm);
                trailer.setX(v1.x);
                trailer.setLastX(v1.x);
                trailer.setY(v1.y);
                trailer.setLastY(v1.y);
                trailer.setCurrentSquareFromPosition(v1.x, v1.y, 0.0F);
                this.addPointConstraint(null, trailer, "trailer", "trailer");
                pool.release(v1);
                pool.release(v2);
            }
        }
    }

    public String getTowAttachmentSelf() {
        return this.towAttachmentSelf;
    }

    public String getTowAttachmentOther() {
        return this.towAttachmentOther;
    }

    public VehicleEngineRPM getVehicleEngineRPM() {
        if (this.vehicleEngineRpm == null) {
            this.vehicleEngineRpm = ScriptManager.instance.getVehicleEngineRPM(this.getScript().getEngineRPMType());
            if (this.vehicleEngineRpm == null) {
                DebugLog.Vehicle.warn("unknown vehicleEngineRPM \"%s\"", this.getScript().getEngineRPMType());
                this.vehicleEngineRpm = new VehicleEngineRPM();
            }
        }

        return this.vehicleEngineRpm;
    }

    @Override
    public FMODParameterList getFMODParameters() {
        return this.fmodParameters;
    }

    @Override
    public void startEvent(long eventInstance, GameSoundClip clip, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        ArrayList<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.eventDescription.parameters;

        for (int i = 0; i < eventParameters.size(); i++) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (!parameterSet.get(eventParameter.globalIndex)) {
                FMODParameter fmodParameter = myParameters.get(eventParameter);
                if (fmodParameter != null) {
                    fmodParameter.startEventInstance(eventInstance);
                }
            }
        }
    }

    @Override
    public void updateEvent(long eventInstance, GameSoundClip clip) {
    }

    @Override
    public void stopEvent(long eventInstance, GameSoundClip clip, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        ArrayList<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.eventDescription.parameters;

        for (int i = 0; i < eventParameters.size(); i++) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (!parameterSet.get(eventParameter.globalIndex)) {
                FMODParameter fmodParameter = myParameters.get(eventParameter);
                if (fmodParameter != null) {
                    fmodParameter.stopEventInstance(eventInstance);
                }
            }
        }
    }

    private void stopEngineSounds() {
        if (this.emitter != null) {
            for (int i = 0; i < this.newEngineSoundId.length; i++) {
                if (this.newEngineSoundId[i] != 0L) {
                    this.getEmitter().stopSound(this.newEngineSoundId[i]);
                    this.newEngineSoundId[i] = 0L;
                }
            }

            if (this.combinedEngineSound != 0L) {
                if (this.getEmitter().hasSustainPoints(this.combinedEngineSound)) {
                    this.getEmitter().triggerCue(this.combinedEngineSound);
                } else {
                    this.getEmitter().stopSound(this.combinedEngineSound);
                }

                this.combinedEngineSound = 0L;
            }
        }
    }

    public BaseVehicle setSmashed(String location) {
        return this.setSmashed(location, false);
    }

    public BaseVehicle setSmashed(String location, boolean flipped) {
        String newScript = null;
        Integer newSkinIndex = null;
        KahluaTableImpl def = (KahluaTableImpl)LuaManager.env.rawget("SmashedCarDefinitions");
        if (def != null) {
            KahluaTableImpl cars = (KahluaTableImpl)def.rawget("cars");
            if (cars != null) {
                KahluaTableImpl car = (KahluaTableImpl)cars.rawget(this.getScriptName());
                if (car != null) {
                    newScript = car.rawgetStr(location.toLowerCase());
                    newSkinIndex = car.rawgetInt("skin");
                    if (newSkinIndex == -1) {
                        newSkinIndex = this.getSkinIndex();
                    }
                }
            }
        }

        int tKeyId = this.getKeyId();
        if (newScript != null) {
            this.removeFromWorld();
            this.permanentlyRemove();
            BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
            v.setScriptName(newScript);
            v.setScript();
            v.setSkinIndex(newSkinIndex);
            v.setX(this.getX());
            v.setY(this.getY());
            v.setZ(this.getZ());
            v.setDir(this.getDir());
            v.savedRot.set(this.savedRot);
            v.savedPhysicsZ = this.savedPhysicsZ;
            if (flipped) {
                float ry = this.getAngleY();
                v.savedRot.rotationXYZ(0.0F, ry * (float) (Math.PI / 180.0), (float) Math.PI);
            }

            v.jniTransform.setRotation(v.savedRot);
            if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
                v.setSquare(this.square);
                v.square.chunk.vehicles.add(v);
                v.chunk = v.square.chunk;
                v.addToWorld();
                VehiclesDB2.instance.addVehicle(v);
            }

            v.setGeneralPartCondition(0.5F, 60.0F);
            VehiclePart part = v.getPartById("Engine");
            if (part != null) {
                part.setCondition(0);
            }

            VehiclePart gloveBox = v.getPartById("GloveBox");
            if (gloveBox != null) {
                gloveBox.setInventoryItem(null);
                gloveBox.setCondition(0);
            }

            v.engineQuality = 0;
            v.setKeyId(tKeyId);
            return v;
        } else {
            return this;
        }
    }

    public boolean isCollided(IsoGameCharacter character) {
        if (GameClient.client && this.getDriver() != null && !this.getDriver().isLocal()) {
            return true;
        } else {
            Vector2 v = this.testCollisionWithCharacter(character, 0.20000002F, this.hitVars.collision);
            return v != null && v.x != -1.0F;
        }
    }

    public BaseVehicle.HitVars checkCollision(IsoGameCharacter target) {
        if (target.isProne()) {
            int numWheelsHit = this.testCollisionWithProneCharacter(target, true);
            if (numWheelsHit > 0) {
                this.hitVars.calc(target, this);
                this.hitCharacter(target, this.hitVars);
                return this.hitVars;
            } else {
                return null;
            }
        } else {
            this.hitVars.calc(target, this);
            this.hitCharacter(target, this.hitVars);
            return this.hitVars;
        }
    }

    public void onHitLandmine(IsoGridSquare square) {
        this.ApplyImpulse(square.getX(), square.getY(), square.getZ(), 0.0F, 0.0F, 1.0F, 300000.0F);
        DebugType.General
            .println(
                "Hit a land mine! %f, %f, %f. isActive: %s, isStatic: %s",
                (float)square.getX(),
                (float)square.getY(),
                (float)square.getZ(),
                this.isActive ? "true" : "false",
                this.isStatic ? "true" : "false"
            );
    }

    public void onJump() {
        this.ApplyImpulse(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F, 1.0F, 1000000.0F);
        DebugType.General.println("Jump! isActive: %s, isStatic: %s", this.isActive ? "true" : "false", this.isStatic ? "true" : "false");
    }

    public boolean updateHitByVehicle(IsoGameCharacter target) {
        if (target.isVehicleCollisionActive(this) && (this.isCollided(target) || target.isCollidedWithVehicle()) && this.physics != null) {
            BaseVehicle.HitVars hitVars = this.checkCollision(target);
            if (hitVars != null) {
                target.doHitByVehicle(this, hitVars);
                return true;
            }
        }

        return false;
    }

    public void hitCharacter(IsoGameCharacter character, BaseVehicle.HitVars vars) {
        if (vars.dot < 0.0F && !GameServer.server && !character.isAnimal()) {
            this.ApplyImpulse(character, vars.vehicleImpulse);
        }

        long currentMS = System.currentTimeMillis();
        long diff = (currentMS - this.zombieHitTimestamp) / 1000L;
        this.zombiesHits = Math.max(this.zombiesHits - (int)diff, 0);
        if (currentMS - this.zombieHitTimestamp > 700L) {
            this.zombieHitTimestamp = currentMS;
            this.zombiesHits++;
            this.zombiesHits = Math.min(this.zombiesHits, 20);
        }

        if (character instanceof IsoPlayer isoPlayer) {
            isoPlayer.setVehicleHitLocation(this);
        } else if (character instanceof IsoZombie isoZombie) {
            isoZombie.setVehicleHitLocation(this);
        }

        if (vars.vehicleSpeed >= 5.0F || this.zombiesHits > 10) {
            vars.vehicleSpeed = this.getCurrentSpeedKmHour() / 5.0F;
            Vector3f pos = TL_vector3f_pool.get().alloc();
            this.getLocalPos(character.getX(), character.getY(), character.getZ(), pos);
            if (pos.z > 0.0F) {
                int dmg = this.caclulateDamageWithBodies(true);
                if (!GameClient.client) {
                    this.addDamageFrontHitAChr(dmg);
                }

                DebugLog.Vehicle.trace("Damage car front hits=%d damage=%d", this.zombiesHits, dmg);
                vars.vehicleDamage = dmg;
                vars.isVehicleHitFromFront = true;
            } else {
                int dmg = this.caclulateDamageWithBodies(false);
                if (!GameClient.client) {
                    this.addDamageRearHitAChr(dmg);
                }

                DebugLog.Vehicle.trace("Damage car rear hits=%d damage=%d", this.zombiesHits, dmg);
                vars.vehicleDamage = dmg;
                vars.isVehicleHitFromFront = false;
            }

            TL_vector3f_pool.get().release(pos);
        }
    }

    public float getAnimalTrailerSize() {
        return this.getScript().getAnimalTrailerSize();
    }

    public ArrayList<IsoAnimal> getAnimals() {
        return this.animals;
    }

    public void addAnimalFromHandsInTrailer(IsoAnimal animal, IsoPlayer player) {
        this.animals.add(animal);
        animal.setVehicle(this);
        AnimalInventoryItem item = player.getInventory().getAnimalInventoryItem(animal);
        if (item != null) {
            player.getInventory().Remove(item);
        } else {
            DebugLog.Animal.error("Animal not found: id=%d/%d", animal.getAnimalID(), animal.getOnlineID());
        }

        player.setPrimaryHandItem(null);
        player.setSecondaryHandItem(null);
        this.recalcAnimalSize();
    }

    public void addAnimalFromHandsInTrailer(IsoDeadBody body, IsoPlayer player) {
        IsoAnimal animal = IsoAnimal.createAnimalFromCorpse(body);
        animal.setHealth(0.0F);
        InventoryItem item = player.getPrimaryHandItem();
        player.getInventory().Remove(item);
        player.setPrimaryHandItem(null);
        player.setSecondaryHandItem(null);
        this.addAnimalInTrailer(animal);
    }

    public void addAnimalInTrailer(IsoDeadBody body) {
        IsoAnimal animal = IsoAnimal.createAnimalFromCorpse(body);
        animal.setHealth(0.0F);
        this.addAnimalInTrailer(animal);
        body.getSquare().removeCorpse(body, false);
        body.invalidateCorpse();
    }

    public void addAnimalInTrailer(IsoAnimal animal) {
        this.animals.add(animal);
        if (animal.mother != null) {
            animal.attachBackToMother = animal.mother.animalId;
        }

        animal.setVehicle(this);
        if (animal.getData().getAttachedPlayer() != null) {
            animal.getData().getAttachedPlayer().removeAttachedAnimal(animal);
            animal.getData().setAttachedPlayer(null);
        }

        animal.removeFromWorld();
        animal.removeFromSquare();
        animal.setX(this.getX());
        animal.setY(this.getY());
        this.recalcAnimalSize();
    }

    private void recalcAnimalSize() {
        this.totalAnimalSize = 0.0F;

        for (int i = 0; i < this.animals.size(); i++) {
            this.totalAnimalSize = this.totalAnimalSize + this.animals.get(i).getAnimalTrailerSize();
        }
    }

    public IsoObject removeAnimalFromTrailer(IsoAnimal animal) {
        IsoObject toReturn = null;

        for (int i = 0; i < this.animals.size(); i++) {
            IsoAnimal animalTest = this.animals.get(i);
            if (animalTest == animal) {
                this.animals.remove(animalTest);
                Vector2 vec = this.getAreaCenter("AnimalEntry");
                IsoAnimal newAnimal = new IsoAnimal(
                    this.getSquare().getCell(), PZMath.fastfloor(vec.x), PZMath.fastfloor(vec.y), this.getSquare().z, animal.getAnimalType(), animal.getBreed()
                );
                newAnimal.copyFrom(animal);
                AnimalInstanceManager.getInstance().remove(animal);
                AnimalInstanceManager.getInstance().add(newAnimal, animal.getOnlineID());
                newAnimal.attachBackToMotherTimer = 10000.0F;
                toReturn = newAnimal;
                if (animal.getHealth() == 0.0F) {
                    IsoDeadBody body = new IsoDeadBody(newAnimal);
                    if (newAnimal.getSquare() != null) {
                        newAnimal.getSquare().addCorpse(body, false);
                        body.invalidateCorpse();
                    }

                    toReturn = body;
                }
            }
        }

        this.recalcAnimalSize();
        return toReturn;
    }

    public void replaceGrownAnimalInTrailer(IsoAnimal current, IsoAnimal grown) {
        if (current != null && grown != null && current != grown && !this.animals.contains(grown)) {
            for (int i = 0; i < this.animals.size(); i++) {
                IsoAnimal animalTest = this.animals.get(i);
                if (animalTest == current) {
                    this.animals.set(i, grown);
                    break;
                }
            }

            this.recalcAnimalSize();
        }
    }

    public float getCurrentTotalAnimalSize() {
        return this.totalAnimalSize;
    }

    public void setCurrentTotalAnimalSize(float totalAnimalSize) {
        this.totalAnimalSize = totalAnimalSize;
    }

    public void keyNamerVehicle(InventoryItem item) {
        keyNamerVehicle(item, this);
    }

    public static void keyNamerVehicle(InventoryItem item, BaseVehicle vehicle) {
        if (item != null && vehicle != null) {
            if (vehicle.getSquare() != null) {
                item.setOrigin(vehicle.getSquare());
            }

            if (!item.getType().equals("KeyRing") && !item.hasTag(ItemTag.KEY_RING)) {
                String carName = vehicle.getScript().getName();
                if (vehicle.getScript().getCarModelName() != null) {
                    carName = vehicle.getScript().getCarModelName();
                }

                item.setName(Translator.getText(item.getScriptItem().getDisplayName()) + " - " + Translator.getText("IGUI_VehicleName" + carName));
            }
        }
    }

    public boolean checkZombieKeyForVehicle(IsoZombie zombie) {
        return this.checkZombieKeyForVehicle(zombie, this.getScriptName());
    }

    public boolean checkZombieKeyForVehicle(IsoZombie zombie, String vehicleType) {
        if (!vehicleType.contains("Burnt") && !vehicleType.equals("Trailer") && !vehicleType.equals("TrailerAdvert")) {
            String outfitName = zombie.getOutfitName();
            if (outfitName == null) {
                return false;
            } else if (this.getZombieType() != null && this.hasZombieType(outfitName)) {
                return true;
            } else if (outfitName.contains("Survivalist")) {
                return true;
            } else if (this.getZombieType() != null) {
                return false;
            } else if (this.checkForSpecialMatchOne("Fire", vehicleType, outfitName)) {
                return this.checkForSpecialMatchTwo("Fire", vehicleType, outfitName);
            } else if (this.checkForSpecialMatchOne("Police", vehicleType, outfitName)) {
                return this.checkForSpecialMatchTwo("Police", vehicleType, outfitName);
            } else if (this.checkForSpecialMatchOne("Spiffo", vehicleType, outfitName)) {
                return this.checkForSpecialMatchTwo("Spiffo", vehicleType, outfitName);
            } else if (!vehicleType.contains("Ranger") && (!vehicleType.contains("Lights") || this.getSkinIndex() != 0)) {
                if ((!vehicleType.contains("Lights") || this.getSkinIndex() != 1)
                    && (!vehicleType.contains("VanSpecial") || this.getSkinIndex() != 0)
                    && !vehicleType.contains("Fossoil")) {
                    if (outfitName.contains("Postal")) {
                        return vehicleType.contains("Mail") || vehicleType.contains("VanSpecial") && this.getSkinIndex() == 2;
                    } else if (vehicleType.contains("Mccoy") || vehicleType.contains("VanSpecial") && this.getSkinIndex() == 1) {
                        return outfitName.contains("Foreman") || outfitName.contains("Mccoy");
                    } else if (vehicleType.contains("Taxi")) {
                        return outfitName.contains("Generic");
                    } else if (outfitName.contains("Cook") || outfitName.contains("Security") || outfitName.contains("Waiter")) {
                        return vehicleType.contains("Normal") || vehicleType.contains("Small");
                    } else if (outfitName.contains("Farmer") || outfitName.contains("Fisherman") || outfitName.contains("Hunter")) {
                        return vehicleType.contains("Pickup")
                            || vehicleType.contains("OffRoad")
                            || vehicleType.contains("SUV")
                            || vehicleType.equals("Trailer_Horsebox")
                            || vehicleType.equals("Trailer_Livestock");
                    } else if (outfitName.contains("Teacher")) {
                        return vehicleType.contains("Normal")
                            || vehicleType.contains("Small")
                            || vehicleType.contains("StationWagon")
                            || vehicleType.contains("SUV");
                    } else if (!outfitName.contains("Young") && !outfitName.contains("Student") || vehicleType.contains("Small") && Rand.Next(2) == 0) {
                        if (vehicleType.contains("Luxury") || vehicleType.contains("Modern") || vehicleType.contains("SUV")) {
                            return outfitName.contains("Classy")
                                || outfitName.contains("Doctor")
                                || outfitName.contains("Dress")
                                || outfitName.contains("Generic")
                                || outfitName.contains("Golfer")
                                || outfitName.contains("OfficeWorker")
                                || outfitName.contains("Foreman")
                                || outfitName.contains("Priest")
                                || outfitName.contains("Thug")
                                || outfitName.contains("Trader")
                                || outfitName.contains("FitnessInstructor");
                        } else if (vehicleType.contains("Sports")) {
                            return outfitName.contains("Classy")
                                || outfitName.contains("Doctor")
                                || outfitName.contains("Dress")
                                || outfitName.contains("Generic")
                                || outfitName.contains("Golfer")
                                || outfitName.contains("OfficeWorker")
                                || outfitName.contains("Trader")
                                || outfitName.contains("Bandit")
                                || outfitName.contains("Biker")
                                || outfitName.contains("Redneck")
                                || outfitName.contains("Veteran")
                                || outfitName.contains("Thug")
                                || outfitName.contains("Foreman");
                        } else if (!vehicleType.contains("Small") && !vehicleType.contains("StationWagon")
                            || !outfitName.contains("Foreman")
                                && !outfitName.contains("Classy")
                                && !outfitName.contains("Doctor")
                                && !outfitName.contains("Golfer")
                                && !outfitName.contains("Trader")
                                && !outfitName.contains("Biker")) {
                            if (!vehicleType.contains("Pickup") && !vehicleType.contains("Van")
                                || !outfitName.contains("Classy")
                                    && !outfitName.contains("Doctor")
                                    && !outfitName.contains("Golfer")
                                    && !outfitName.contains("Trader")) {
                                if (outfitName.contains("ConstructionWorker") || outfitName.contains("Fossoil")) {
                                    return vehicleType.contains("Pickup") || vehicleType.contains("Offroad");
                                } else if (vehicleType.contains("OffRoad")) {
                                    return outfitName.contains("Classy")
                                        || outfitName.contains("Doctor")
                                        || outfitName.contains("Generic")
                                        || outfitName.contains("Golfer")
                                        || outfitName.contains("Foreman")
                                        || outfitName.contains("Trader")
                                        || outfitName.contains("Biker")
                                        || outfitName.contains("Redneck");
                                } else if (outfitName.contains("Redneck") || outfitName.contains("Thug") || outfitName.contains("Veteran")) {
                                    return vehicleType.contains("Normal")
                                        || vehicleType.contains("Pickup")
                                        || vehicleType.contains("Offroad")
                                        || vehicleType.contains("Small");
                                } else {
                                    return !outfitName.contains("Biker")
                                        ? true
                                        : vehicleType.contains("Normal") || vehicleType.contains("Pickup") || vehicleType.contains("Offroad");
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
                } else {
                    return outfitName.contains("ConstructionWorker")
                        || outfitName.contains("Fossoil")
                        || outfitName.contains("Foreman")
                        || outfitName.contains("Mechanic")
                        || outfitName.contains("MetalWorker");
                }
            } else {
                return outfitName.contains("Ranger");
            }
        } else {
            return false;
        }
    }

    public boolean checkForSpecialMatchOne(String one, String two, String three) {
        return two.contains(one) || three.contains(one);
    }

    public boolean checkForSpecialMatchTwo(String one, String two, String three) {
        return two.contains(one) && three.contains(one);
    }

    public boolean checkIfGoodVehicleForKey() {
        return !this.getScriptName().contains("Burnt");
    }

    public boolean trySpawnVehicleKeyOnZombie(IsoZombie zombie) {
        if (zombie.shouldZombieHaveKey(true) && this.checkZombieKeyForVehicle(zombie)) {
            InventoryItem key = this.createVehicleKey();
            if (key == null) {
                return false;
            } else {
                this.keySpawned = 1;
                boolean randomKeyRing = this.getScript().hasSpecialKeyRing()
                    && this.getSpecialKeyRingChance() > 0.0F
                    && Rand.Next(100) < this.getSpecialKeyRingChance();
                if (!randomKeyRing && Rand.Next(2) == 0) {
                    zombie.addItemToSpawnAtDeath(key);
                    return true;
                } else {
                    InventoryContainer keyRing = this.tryCreateKeyRing();
                    if (keyRing == null) {
                        zombie.addItemToSpawnAtDeath(key);
                        return true;
                    } else {
                        keyRing.getInventory().AddItem(key);
                        this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
                        zombie.addItemToSpawnAtDeath(keyRing);
                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    public boolean trySpawnVehicleKeyInObject(IsoObject obj) {
        if (obj.container != null
            && !obj.container.isExplored()
            && !obj.container.isShop()
            && obj.container.getFirstTagRecurse(ItemTag.CAR_KEY) == null
            && (
                obj.container.type.equals("counter")
                    || obj.container.type.equals("officedrawers")
                    || obj.container.type.equals("shelves")
                    || obj.container.type.equals("desk")
                    || obj.container.type.equals("filingcabinet")
                    || obj.container.type.equals("locker")
                    || obj.container.type.equals("metal_shelves")
                    || obj.container.type.equals("tent")
                    || obj.container.type.equals("shelter")
                    || obj.container.type.equals("sidetable")
                    || obj.container.type.equals("plankstash")
                    || obj.container.type.equals("wardrobe")
                    || obj.container.type.equals("dresser")
            )) {
            this.putKeyToContainer(obj.container, this.square, obj);
            if (Rand.Next(100) < 1.0F * this.keySpawnChancedD100
                && this.square.getBuilding() != null
                && this.square.getBuilding().getDef() != null
                && this.square.getBuilding().getDef().getKeyId() != -1) {
                this.addBuildingKeyToGloveBox(this.square);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean checkSquareForVehicleKeySpot(IsoGridSquare square) {
        return this.checkSquareForVehicleKeySpot(square, false);
    }

    public boolean checkSquareForVehicleKeySpot(IsoGridSquare square, boolean crashed) {
        boolean keyInSquare = false;
        if (square == null) {
            return keyInSquare;
        } else {
            if (!this.isBurntOrSmashed() && !crashed) {
                keyInSquare = this.checkSquareForVehicleKeySpotContainer(square) || this.checkSquareForVehicleKeySpotZombie(square);
            } else {
                keyInSquare = this.checkSquareForVehicleKeySpotZombie(square);
            }

            return keyInSquare;
        }
    }

    public boolean checkSquareForVehicleKeySpotContainer(IsoGridSquare square) {
        boolean keyInSquare = false;
        if (square != null && !square.isShop() && !this.isBurntOrSmashed()) {
            for (int n = 0; n < square.getObjects().size(); n++) {
                IsoObject obj = square.getObjects().get(n);
                keyInSquare = this.trySpawnVehicleKeyInObject(obj);
                if (keyInSquare) {
                    return true;
                }
            }
        }

        return keyInSquare;
    }

    public boolean checkSquareForVehicleKeySpotZombie(IsoGridSquare square) {
        boolean keyInSquare = false;
        if (square != null) {
            for (int i = 0; i < square.getMovingObjects().size(); i++) {
                if (square.getMovingObjects().get(i) instanceof IsoZombie zombie && zombie.shouldZombieHaveKey(true)) {
                    keyInSquare = this.trySpawnVehicleKeyOnZombie(zombie);
                    if (keyInSquare) {
                        return true;
                    }
                }
            }
        }

        return keyInSquare;
    }

    private static float doKeySandboxSettings(int value) {
        return switch (value) {
            case 1 -> 0.0F;
            case 2 -> 0.05F;
            case 3 -> 0.2F;
            case 4 -> 0.6F;
            case 5 -> 1.0F;
            case 6 -> 2.0F;
            case 7 -> 2.4F;
            default -> 0.6F;
        };
    }

    public void forceVehicleDistribution(String distribution) {
        ItemPickerJava.VehicleDistribution distro = ItemPickerJava.VehicleDistributions.get(distribution);
        ItemPickerJava.ItemPickerRoom distro2 = distro.normal;

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getItemContainer() != null) {
                if (GameServer.server && GameServer.softReset) {
                    part.getItemContainer().setExplored(false);
                }

                if (!part.getItemContainer().explored) {
                    part.getItemContainer().clear();
                    this.randomizeContainer(part, distro2);
                    part.getItemContainer().setExplored(true);
                }
            }
        }
    }

    public boolean canLightSmoke(IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        } else if (!this.hasLighter()) {
            return false;
        } else {
            return this.getBatteryCharge() <= 0.0F ? false : this.getSeat(chr) <= 1;
        }
    }

    private void checkVehicleFailsToStartWithZombiesTargeting() {
        if (!GameServer.server) {
            for (int i = 0; i < this.getMaxPassengers(); i++) {
                BaseVehicle.Passenger passenger = this.getPassenger(i);
                IsoPlayer player = Type.tryCastTo(passenger.character, IsoPlayer.class);
                if (player != null && player.isLocalPlayer()) {
                    int numZombies = player.getStats().musicZombiesTargetingNearbyMoving;
                    numZombies += player.getStats().musicZombiesTargetingNearbyNotMoving;
                    if (numZombies > 0) {
                        player.triggerMusicIntensityEvent("VehicleFailsToStartWithZombiesTargeting");
                    }
                }
            }
        }
    }

    private void checkVehicleStartsWithZombiesTargeting() {
        if (!GameServer.server) {
            for (int i = 0; i < this.getMaxPassengers(); i++) {
                BaseVehicle.Passenger passenger = this.getPassenger(i);
                IsoPlayer player = Type.tryCastTo(passenger.character, IsoPlayer.class);
                if (player != null && player.isLocalPlayer()) {
                    int numZombies = player.getStats().musicZombiesTargetingNearbyMoving;
                    numZombies += player.getStats().musicZombiesTargetingNearbyNotMoving;
                    if (numZombies > 0) {
                        player.triggerMusicIntensityEvent("VehicleStartsWithZombiesTargeting");
                    }
                }
            }
        }
    }

    public ArrayList<String> getZombieType() {
        return this.script.getZombieType();
    }

    public String getRandomZombieType() {
        return this.script.getRandomZombieType();
    }

    public boolean hasZombieType(String outfit) {
        return this.script.hasZombieType(outfit);
    }

    public String getFirstZombieType() {
        return this.script.getFirstZombieType();
    }

    public boolean notKillCrops() {
        return this.script.notKillCrops();
    }

    public boolean hasLighter() {
        return this.script.hasLighter();
    }

    public boolean leftSideFuel() {
        VehicleScript.Area area = this.getScript().getAreaById("GasTank");
        return area != null && !(area.x < 0.0F);
    }

    public boolean rightSideFuel() {
        VehicleScript.Area area = this.getScript().getAreaById("GasTank");
        return area != null && !(area.x > 0.0F);
    }

    public boolean isCreated() {
        return this.created;
    }

    public float getTotalContainerItemWeight() {
        float totalContainerItemWeight = 0.0F;

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                totalContainerItemWeight += part.getItemContainer().getCapacityWeight();
            }
        }

        return totalContainerItemWeight;
    }

    public boolean isSirening() {
        return this.hasLightbar() && this.lightbarSirenMode.get() > 0;
    }

    private boolean isDriverGodMode() {
        IsoGameCharacter isoGameCharacter = this.getDriverRegardlessOfTow();
        return isoGameCharacter != null && isoGameCharacter.isGodMod();
    }

    public Vector3f getIntersectPoint(Vector3f start, Vector3f end, Vector3f result) {
        float x = this.script.getExtents().x * 0.5F + this.script.getCenterOfMassOffset().x();
        float y = this.script.getExtents().y * 0.5F + this.script.getCenterOfMassOffset().y();
        float z = this.script.getExtents().z * 0.5F + this.script.getCenterOfMassOffset().z();
        Vector3f extents = allocVector3f().set(x, y, z);
        Vector3f localStart = this.getLocalPos(start, allocVector3f());
        Vector3f localEnd = this.getLocalPos(end, allocVector3f());
        Vector3f intersect = this.getIntersectPoint(localStart, localEnd, extents, result);
        releaseVector3f(localStart);
        releaseVector3f(localEnd);
        releaseVector3f(extents);
        return intersect == null ? null : this.getWorldPos(intersect, intersect);
    }

    private Vector3f getIntersectPoint(Vector3f start, Vector3f end, Vector3f extents, Vector3f result) {
        Vector3f max = allocVector3f().set(extents);
        Vector3f min = allocVector3f().set(extents).mul(-1.0F);
        float tmin = 0.0F;
        float tmax = 1.0F;
        Vector3f direction = allocVector3f().set(end).sub(start);

        for (int i = 0; i < 3; i++) {
            float startCoord = start.get(i);
            float minCoord = min.get(i);
            float maxCoord = max.get(i);
            float directionCoord = direction.get(i);
            if (Math.abs(directionCoord) < 1.0E-6) {
                if (startCoord < minCoord || startCoord > maxCoord) {
                    releaseVector3f(min);
                    releaseVector3f(max);
                    releaseVector3f(direction);
                    return null;
                }
            } else {
                float t1 = (minCoord - startCoord) / directionCoord;
                float t2 = (maxCoord - startCoord) / directionCoord;
                if (t1 > t2) {
                    float temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) {
                    releaseVector3f(min);
                    releaseVector3f(max);
                    releaseVector3f(direction);
                    return null;
                }
            }
        }

        result.set(start).add(direction.mul(tmin));
        releaseVector3f(min);
        releaseVector3f(max);
        releaseVector3f(direction);
        return result;
    }

    public VehiclePart getNearestVehiclePart(float x, float y, float z, boolean useDestroyed) {
        Vector3f worldPosition = TL_vector3f_pool.get().alloc();
        Vector3f areaWorldPosition = TL_vector3f_pool.get().alloc();
        worldPosition.set(x, y, z);
        VehiclePart nearestVehiclePart = null;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < this.script.getAreaCount(); i++) {
            VehicleScript.Area area = this.script.getArea(i);
            String id = area.getId();
            VehiclePart vehiclePart = this.getPartById(id);
            if (vehiclePart != null && (useDestroyed || vehiclePart.condition != 0) && this.isInArea(id, worldPosition)) {
                this.getWorldPos(area.x, area.y, z, areaWorldPosition);
                float distance = worldPosition.distance(areaWorldPosition);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestVehiclePart = vehiclePart;
                }
            }
        }

        TL_vector3f_pool.get().release(areaWorldPosition);
        TL_vector3f_pool.get().release(worldPosition);
        return nearestVehiclePart;
    }

    public boolean isInArea(String areaId, Vector3f chr) {
        if (areaId != null && this.getScript() != null) {
            VehicleScript.Area area = this.getScript().getAreaById(areaId);
            if (area == null) {
                return false;
            } else {
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                Vector2 center = this.areaPositionLocal(area, vector2);
                if (center == null) {
                    Vector2ObjectPool.get().release(vector2);
                    return false;
                } else {
                    Vector3f localPos = TL_vector3f_pool.get().alloc();
                    this.getLocalPos(chr.x, chr.y, this.getZ(), localPos);
                    float minX = center.x - (area.w + 0.01F) * 0.5F;
                    float minY = center.y - (area.h + 0.01F) * 0.5F;
                    float maxX = center.x + (area.w + 0.01F) * 0.5F;
                    float maxY = center.y + (area.h + 0.01F) * 0.5F;
                    boolean inside = localPos.x >= minX && localPos.x < maxX && localPos.z >= minY && localPos.z < maxY;
                    Vector2ObjectPool.get().release(vector2);
                    TL_vector3f_pool.get().release(localPos);
                    return inside;
                }
            }
        } else {
            return false;
        }
    }

    private boolean processRangeHit(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        float range = weapon.getMaxRange(isoGameCharacter);
        Vector3f start = allocVector3f();
        Vector3f end = allocVector3f();
        float renderedAngle = isoGameCharacter.getLookAngleRadians();
        Vector3f directionVector = allocVector3f();
        directionVector.set((float)Math.cos(renderedAngle), (float)Math.sin(renderedAngle), 0.0F);
        directionVector.normalize();
        if (GameServer.server) {
            start.set(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
        } else {
            BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
            Vector3 muzzlePosition = ballisticsController.getMuzzlePosition();
            start.set(muzzlePosition.x, muzzlePosition.y, muzzlePosition.z);
        }

        end.set(start.x() + directionVector.x() * range, start.y() + directionVector.y() * range, start.z() + directionVector.z() * range);
        Vector3f intersect = allocVector3f();
        boolean bIntersected = this.getIntersectPoint(start, end, intersect) != null;
        releaseVector3f(start);
        releaseVector3f(end);
        releaseVector3f(directionVector);
        releaseVector3f(intersect);
        if (!bIntersected) {
            return false;
        } else {
            VehiclePart vehiclePart = this.getPartByDirection(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
            if (vehiclePart == null) {
                return false;
            } else {
                this.applyDamageToPart(isoGameCharacter, weapon, vehiclePart, damage);
                return true;
            }
        }
    }

    private boolean processMeleeHit(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        if (isoGameCharacter instanceof IsoPlayer isoPlayer) {
            isoPlayer.setVehicleHitLocation(this);
        }

        VehiclePart vehiclePart = this.getNearestBodyworkPart(isoGameCharacter);
        if (vehiclePart == null) {
            return false;
        } else {
            this.applyDamageToPart(isoGameCharacter, weapon, vehiclePart, damage);
            return true;
        }
    }

    private void applyDamageToPart(IsoGameCharacter isoGameCharacter, HandWeapon weapon, VehiclePart vehiclePart, float damage) {
        if (vehiclePart != null) {
            VehicleWindow window = vehiclePart.getWindow();

            for (int i = 0; i < vehiclePart.getChildCount(); i++) {
                VehiclePart child = vehiclePart.getChild(i);
                if (child != null && child.getWindow() != null) {
                    window = child.getWindow();
                    break;
                }
            }

            float calculatedDamage = 0.0F;
            if (vehiclePart.light != null) {
                calculatedDamage = CombatManager.getInstance()
                    .calculateDamageToVehicle(isoGameCharacter, vehiclePart.getDurability(), damage, weapon.getDoorDamage());
                vehiclePart.setCondition(vehiclePart.getCondition() - (int)calculatedDamage);
                if (GameServer.server) {
                    this.transmitPartItem(vehiclePart);
                    GameServer.PlayWorldSoundServer(isoGameCharacter, "HitVehicleWindowWithWeapon", false, vehiclePart.getSquare(), 0.2F, 10.0F, 1.1F, true);
                } else if (!GameClient.client) {
                    isoGameCharacter.playSound("HitVehicleWindowWithWeapon");
                }
            } else if (window != null && window.isHittable()) {
                calculatedDamage = CombatManager.getInstance()
                    .calculateDamageToVehicle(isoGameCharacter, vehiclePart.getDurability(), damage, weapon.getDoorDamage());
                window.damage((int)calculatedDamage);
                if (GameServer.server) {
                    this.transmitPartWindow(vehiclePart);
                    GameServer.PlayWorldSoundServer(isoGameCharacter, "HitVehicleWindowWithWeapon", false, vehiclePart.getSquare(), 0.2F, 10.0F, 1.1F, true);
                } else if (!GameClient.client) {
                    isoGameCharacter.playSound("HitVehicleWindowWithWeapon");
                }
            } else {
                calculatedDamage = CombatManager.getInstance()
                    .calculateDamageToVehicle(isoGameCharacter, vehiclePart.getDurability(), damage, weapon.getDoorDamage());
                vehiclePart.setCondition(vehiclePart.getCondition() - (int)calculatedDamage);
                if (GameServer.server) {
                    this.transmitPartItem(vehiclePart);
                    GameServer.PlayWorldSoundServer(isoGameCharacter, "HitVehiclePartWithWeapon", false, vehiclePart.getSquare(), 0.2F, 10.0F, 1.1F, true);
                } else if (!GameClient.client) {
                    isoGameCharacter.playSound("HitVehiclePartWithWeapon");
                }
            }

            vehiclePart.updateFlags = (short)(vehiclePart.updateFlags | 2048);
            this.updateFlags = (short)(this.updateFlags | 2048);
            DebugLog.Combat
                .debugln(
                    "VehiclePart = %s : durability = %f : damage = %f : conditionalDamage = %f",
                    vehiclePart.getId(),
                    vehiclePart.getDurability(),
                    damage,
                    calculatedDamage
                );
        }
    }

    public boolean processHit(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        return weapon.isRanged() ? this.processRangeHit(isoGameCharacter, weapon, damage) : this.processMeleeHit(isoGameCharacter, weapon, damage);
    }

    private VehiclePart getPartByDirection(float x, float y, float z) {
        Vector3f v = TL_vector3f_pool.get().alloc();
        this.getLocalPos(x, y, z, v);
        x = v.x;
        z = v.z;
        TL_vector3f_pool.get().release(v);
        Vector3f extents = this.script.getExtents();
        Vector3f centerOfMassOffset = this.script.getCenterOfMassOffset();
        float xMin = centerOfMassOffset.x - extents.x * 0.5F;
        float xMax = centerOfMassOffset.x + extents.x * 0.5F;
        float yMin = centerOfMassOffset.z - extents.z * 0.5F;
        float yMax = centerOfMassOffset.z + extents.z * 0.5F;
        if (x < xMin * 0.98F) {
            return this.getWeightedRandomSidePart("Right");
        } else if (x > xMax * 0.98F) {
            return this.getWeightedRandomSidePart("Left");
        } else if (z < yMin * 0.98F) {
            return this.getWeightedRandomRearPart();
        } else {
            return z > yMax * 0.98F ? this.getWeightedRandomFrontPart() : this.getAnyRandomPart();
        }
    }

    private void buildVehiclePartList(String partId, float weight, ArrayList<BaseVehicle.WeightedVehiclePart> weightedVehiclePartArrayList) {
        BaseVehicle.WeightedVehiclePart weightedVehiclePart = new BaseVehicle.WeightedVehiclePart();
        weightedVehiclePart.vehiclePart = this.getPartById(partId);
        weightedVehiclePart.weight = weight;
        if (weightedVehiclePart.vehiclePart != null) {
            if (weightedVehiclePart.vehiclePart.condition != 0) {
                weightedVehiclePartArrayList.add(weightedVehiclePart);
            }
        }
    }

    private VehiclePart getAnyRandomPart() {
        ArrayList<VehiclePart> activeVehiclePartList = new ArrayList<>();

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart vehiclePart = this.getPartByIndex(i);
            if (vehiclePart != null && vehiclePart.condition != 0) {
                activeVehiclePartList.add(vehiclePart);
            }
        }

        return !activeVehiclePartList.isEmpty() ? activeVehiclePartList.get(Rand.Next(0, activeVehiclePartList.size())) : null;
    }

    private boolean isGasTakeSide(String side) {
        return side.contains("Left") && this.leftSideFuel() ? true : side.contains("Right") && this.rightSideFuel();
    }

    private VehiclePart getWeightedRandomSidePart(String side) {
        ArrayList<BaseVehicle.WeightedVehiclePart> activeVehiclePartList = new ArrayList<>();

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getId().contains(side) || this.isGasTakeSide(side)) {
                this.buildVehiclePartList(part.getId(), 1.0F, activeVehiclePartList);
            }
        }

        if (side.equals("Right")) {
            this.buildVehiclePartList("WindowRearRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("WindowFrontRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("TireRearRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("TireFrontRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("DoorFrontRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("DoorRearRight", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightRearRight", 19.0F, activeVehiclePartList);
        } else {
            this.buildVehiclePartList("WindowRearLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("WindowFrontLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("TireRearLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("TireFrontLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("DoorFrontLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("DoorRearLeft", 19.0F, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightRearLeft", 19.0F, activeVehiclePartList);
        }

        this.buildVehiclePartList("GasTank", 9.0F, activeVehiclePartList);
        this.buildVehiclePartList("lightbar", 20.0F, activeVehiclePartList);
        return !activeVehiclePartList.isEmpty() ? this.getWeightedRandomPart(activeVehiclePartList) : null;
    }

    private VehiclePart getWeightedRandomFrontPart() {
        ArrayList<BaseVehicle.WeightedVehiclePart> activeVehiclePartList = new ArrayList<>();

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getId().contains("Front")) {
                this.buildVehiclePartList(part.getId(), 1.0F, activeVehiclePartList);
            }
        }

        this.buildVehiclePartList("HeadlightRight", 20.0F, activeVehiclePartList);
        this.buildVehiclePartList("HeadlightLeft", 20.0F, activeVehiclePartList);
        this.buildVehiclePartList("TireFrontRight", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("TireFrontLeft", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("Engine", 5.0F, activeVehiclePartList);
        this.buildVehiclePartList("EngineDoor", 10.0F, activeVehiclePartList);
        this.buildVehiclePartList("Battery", 5.0F, activeVehiclePartList);
        this.buildVehiclePartList("Windshield", 20.0F, activeVehiclePartList);
        this.buildVehiclePartList("Heater", 2.0F, activeVehiclePartList);
        this.buildVehiclePartList("Hood", 10.0F, activeVehiclePartList);
        this.buildVehiclePartList("Radio", 0.5F, activeVehiclePartList);
        this.buildVehiclePartList("GloveBox", 0.5F, activeVehiclePartList);
        this.buildVehiclePartList("lightbar", 20.0F, activeVehiclePartList);
        return !activeVehiclePartList.isEmpty() ? this.getWeightedRandomPart(activeVehiclePartList) : null;
    }

    private VehiclePart getWeightedRandomRearPart() {
        ArrayList<BaseVehicle.WeightedVehiclePart> activeVehiclePartList = new ArrayList<>();

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getId().contains("Rear")) {
                this.buildVehiclePartList(part.getId(), 1.0F, activeVehiclePartList);
            }
        }

        this.buildVehiclePartList("HeadlightRearRight", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("HeadlightRearLeft", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("TireRearRight", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("TireRearLeft", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("WindshieldRear", 19.0F, activeVehiclePartList);
        this.buildVehiclePartList("TruckBed", 5.0F, activeVehiclePartList);
        this.buildVehiclePartList("TrunkDoor", 10.0F, activeVehiclePartList);
        this.buildVehiclePartList("DoorRear", 9.0F, activeVehiclePartList);
        this.buildVehiclePartList("Muffler", 0.5F, activeVehiclePartList);
        this.buildVehiclePartList("lightbar", 20.0F, activeVehiclePartList);
        return !activeVehiclePartList.isEmpty() ? this.getWeightedRandomPart(activeVehiclePartList) : null;
    }

    private VehiclePart getWeightedRandomPart(ArrayList<BaseVehicle.WeightedVehiclePart> weightedVehiclePartList) {
        float totalWeight = 0.0F;

        for (BaseVehicle.WeightedVehiclePart weightedVehiclePart : weightedVehiclePartList) {
            totalWeight += weightedVehiclePart.weight;
        }

        float randomValue = Rand.Next(0.0F, 1.0F) * totalWeight;

        for (BaseVehicle.WeightedVehiclePart weightedVehiclePart : weightedVehiclePartList) {
            randomValue -= weightedVehiclePart.weight;
            if (randomValue <= 0.0F) {
                return weightedVehiclePart.vehiclePart;
            }
        }

        return null;
    }

    public boolean canAddAnimalInTrailer(IsoAnimal animal) {
        return this.getAnimalTrailerSize() >= this.getCurrentTotalAnimalSize() + animal.getAnimalTrailerSize();
    }

    public boolean canAddAnimalInTrailer(IsoDeadBody animal) {
        return this.getAnimalTrailerSize() >= this.getCurrentTotalAnimalSize() + ((KahluaTableImpl)animal.getModData()).rawgetFloat("animalTrailerSize");
    }

    public boolean isBurnt() {
        return this.getScriptName().contains("Burnt");
    }

    public boolean isSmashed() {
        return this.getScriptName().contains("Smashed");
    }

    public boolean isBurntOrSmashed() {
        return this.isBurnt() || this.isSmashed();
    }

    public float getSpecialKeyRingChance() {
        return !this.getScript().hasSpecialKeyRing()
            ? this.keySpawnChancedD100
            : Math.max(this.keySpawnChancedD100, (float)this.getScript().getSpecialKeyRingChance());
    }

    public boolean hasLiveBattery() {
        return this.getPartById("Battery") != null && this.getPartById("Battery").getInventoryItem() != null && this.getBatteryCharge() > 0.0F;
    }

    public void setDebugPhysicsRender(boolean addedToWorld) {
        if (addedToWorld) {
            PhysicsDebugRenderer.addVehicleRender(this);
        } else {
            PhysicsDebugRenderer.removeVehicleRender(this);
        }
    }

    public boolean testTouchingVehicle(IsoGameCharacter isoGameCharacter, RagdollController ragdollController) {
        if (isoGameCharacter == null) {
            return false;
        } else {
            return !(Math.abs(this.getX() - isoGameCharacter.getX()) < 0.01F) && !(Math.abs(this.getY() - isoGameCharacter.getY()) < 0.01F)
                ? !(ragdollController.getPelvisPositionZ() < this.getZ() + 0.25F)
                : false;
        }
    }

    public IsoGameCharacter getDamagedBy() {
        IsoGameCharacter isoGameCharacter = this.getDriverRegardlessOfTow();
        return isoGameCharacter != null ? isoGameCharacter : this.lastDamagedBy;
    }

    public IsoGridSquare getSquareForArea(String areaId) {
        Vector2 areaCenter = this.getAreaCenter(areaId);
        return areaCenter == null
            ? this.getSquare()
            : this.getCell().getGridSquare((double)areaCenter.getX(), (double)areaCenter.getY(), (double)PZMath.fastfloor(this.getZ()));
    }

    public void partsClear() {
        this.parts.clear();
    }

    public static enum Authorization {
        Server,
        LocalCollide,
        RemoteCollide,
        Local,
        Remote;
    }

    public static class HitVars {
        private static final float speedCap = 10.0F;
        private final Vector3f velocity = new Vector3f();
        private final Vector2 collision = new Vector2();
        private float dot;
        protected float vehicleImpulse;
        protected float vehicleSpeed;
        public final Vector3f targetImpulse = new Vector3f();
        public boolean isVehicleHitFromFront;
        public boolean isTargetHitFromBehind;
        public int vehicleDamage;
        public float hitSpeed;

        public void calc(IsoGameCharacter target, BaseVehicle vehicle) {
            vehicle.getLinearVelocity(this.velocity);
            this.velocity.y = 0.0F;
            if (target instanceof IsoZombie) {
                this.vehicleSpeed = Math.min(this.velocity.length(), 10.0F);
                this.hitSpeed = this.vehicleSpeed + vehicle.getClientForce() / vehicle.getFudgedMass();
            } else {
                this.vehicleSpeed = (float)Math.sqrt(this.velocity.x * this.velocity.x + this.velocity.z * this.velocity.z);
                if (target.isOnFloor()) {
                    this.hitSpeed = Math.max(this.vehicleSpeed * 6.0F, 5.0F);
                } else {
                    this.hitSpeed = Math.max(this.vehicleSpeed * 2.0F, 5.0F);
                }
            }

            this.targetImpulse.set(vehicle.getX() - target.getX(), 0.0F, vehicle.getY() - target.getY());
            this.targetImpulse.normalize();
            this.velocity.normalize();
            this.dot = this.velocity.dot(this.targetImpulse);
            this.targetImpulse.normalize();
            this.targetImpulse.mul(3.0F * this.vehicleSpeed / 10.0F);
            this.targetImpulse.set(this.targetImpulse.x, this.targetImpulse.y, this.targetImpulse.z);
            this.vehicleImpulse = vehicle.getFudgedMass() * 7.0F * this.vehicleSpeed / 10.0F * Math.abs(this.dot);
            this.isTargetHitFromBehind = "BEHIND".equals(target.testDotSide(vehicle));
        }
    }

    private static final class L_testCollisionWithVehicle {
        private static final Vector2[] testVecs1 = new Vector2[4];
        private static final Vector2[] testVecs2 = new Vector2[4];
        private static final Vector3f worldPos = new Vector3f();
    }

    public static final class Matrix4fObjectPool extends ObjectPool<Matrix4f> {
        private int allocated;

        private Matrix4fObjectPool() {
            super(Matrix4f::new);
        }

        protected Matrix4f makeObject() {
            this.allocated++;
            return (Matrix4f)super.makeObject();
        }
    }

    public static final class MinMaxPosition {
        public float minX;
        public float maxX;
        public float minY;
        public float maxY;
    }

    public static final class ModelInfo {
        public VehiclePart part;
        public VehicleScript.Model scriptModel;
        public ModelScript modelScript;
        public int wheelIndex;
        public final Matrix4f renderTransform = new Matrix4f();
        public VehicleSubModelInstance modelInstance;
        public AnimationPlayer animPlayer;
        public AnimationTrack track;

        public AnimationPlayer getAnimationPlayer() {
            if (this.part != null && this.part.getParent() != null) {
                BaseVehicle.ModelInfo modelInfoParent = this.part.getVehicle().getModelInfoForPart(this.part.getParent());
                if (modelInfoParent != null) {
                    return modelInfoParent.getAnimationPlayer();
                }
            }

            String modelName = this.scriptModel.file;
            Model model = ModelManager.instance.getLoadedModel(modelName);
            if (model != null && !model.isStatic) {
                if (this.animPlayer != null && this.animPlayer.getModel() != model) {
                    this.animPlayer = Pool.tryRelease(this.animPlayer);
                }

                if (this.animPlayer == null) {
                    this.animPlayer = AnimationPlayer.alloc(model);
                }

                return this.animPlayer;
            } else {
                return null;
            }
        }

        public void releaseAnimationPlayer() {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }
    }

    public static final class Passenger {
        public IsoGameCharacter character;
        private final Vector3f offset = new Vector3f();
    }

    public static final class QuaternionfObjectPool extends ObjectPool<Quaternionf> {
        private int allocated;

        private QuaternionfObjectPool() {
            super(Quaternionf::new);
        }

        protected Quaternionf makeObject() {
            this.allocated++;
            return (Quaternionf)super.makeObject();
        }
    }

    public static final class ServerVehicleState {
        public float x = -1.0F;
        public float y;
        public float z;
        public Quaternionf orient = new Quaternionf();
        public short flags;
        public BaseVehicle.Authorization netPlayerAuthorization = BaseVehicle.Authorization.Server;
        public short netPlayerId;

        public ServerVehicleState() {
            this.flags = 0;
        }

        public void setAuthorization(BaseVehicle vehicle) {
            this.netPlayerAuthorization = vehicle.netPlayerAuthorization;
            this.netPlayerId = vehicle.netPlayerId;
        }

        public boolean shouldSend(BaseVehicle vehicle) {
            if (vehicle.getController() == null) {
                return false;
            } else if (vehicle.updateLockTimeout > System.currentTimeMillis()) {
                return false;
            } else {
                this.flags = (short)(this.flags & 1);
                if (!vehicle.isNetPlayerAuthorization(this.netPlayerAuthorization) || !vehicle.isNetPlayerId(this.netPlayerId)) {
                    this.flags = (short)(this.flags | 8192);
                }

                this.flags = (short)(this.flags | vehicle.updateFlags);
                return this.flags != 0;
            }
        }
    }

    public static final class TransformPool extends ObjectPool<Transform> {
        private int allocated;

        private TransformPool() {
            super(Transform::new);
        }

        protected Transform makeObject() {
            this.allocated++;
            return (Transform)super.makeObject();
        }
    }

    public static class UpdateFlags {
        public static final short Full = 1;
        public static final short PositionOrientation = 2;
        public static final short Engine = 4;
        public static final short Lights = 8;
        public static final short PartModData = 16;
        public static final short PartUsedDelta = 32;
        public static final short PartModels = 64;
        public static final short PartItem = 128;
        public static final short PartWindow = 256;
        public static final short PartDoor = 512;
        public static final short Sounds = 1024;
        public static final short PartCondition = 2048;
        public static final short UpdateCarProperties = 4096;
        public static final short Authorization = 8192;
        public static final short Passengers = 16384;
        public static final short AllPartFlags = 3056;
    }

    public static final class Vector2fObjectPool extends ObjectPool<Vector2f> {
        private int allocated;

        private Vector2fObjectPool() {
            super(Vector2f::new);
        }

        protected Vector2f makeObject() {
            this.allocated++;
            return (Vector2f)super.makeObject();
        }
    }

    public static final class Vector3ObjectPool extends ObjectPool<Vector3> {
        private int allocated;

        private Vector3ObjectPool() {
            super(Vector3::new);
        }

        protected Vector3 makeObject() {
            this.allocated++;
            return (Vector3)super.makeObject();
        }
    }

    public static final class Vector3fObjectPool extends ObjectPool<Vector3f> {
        private int allocated;

        private Vector3fObjectPool() {
            super(Vector3f::new);
        }

        protected Vector3f makeObject() {
            this.allocated++;
            return (Vector3f)super.makeObject();
        }
    }

    public static final class Vector4fObjectPool extends ObjectPool<Vector4f> {
        private int allocated;

        private Vector4fObjectPool() {
            super(Vector4f::new);
        }

        protected Vector4f makeObject() {
            this.allocated++;
            return (Vector4f)super.makeObject();
        }
    }

    private static final class VehicleImpulse {
        private static final ArrayDeque<BaseVehicle.VehicleImpulse> pool = new ArrayDeque<>();
        private final Vector3f impulse = new Vector3f();
        private final Vector3f relPos = new Vector3f();
        private boolean enable;
        private boolean applied;

        private static BaseVehicle.VehicleImpulse alloc() {
            return pool.isEmpty() ? new BaseVehicle.VehicleImpulse() : pool.pop();
        }

        private void release() {
            pool.push(this);
        }
    }

    private class WeightedVehiclePart {
        public VehiclePart vehiclePart;
        public float weight;

        private WeightedVehiclePart() {
            Objects.requireNonNull(BaseVehicle.this);
            super();
            this.weight = 1.0F;
        }
    }

    public static final class WheelInfo {
        public float steering;
        public float rotation;
        public float skidInfo;
        public float suspensionLength;
    }

    public static enum engineStateTypes {
        Idle,
        Starting,
        RetryingStarting,
        StartingSuccess,
        StartingFailed,
        Running,
        Stalling,
        ShutingDown,
        StartingFailedNoPower;

        public static final BaseVehicle.engineStateTypes[] Values = values();
    }
}
