// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import fmod.fmod.BaseSoundListener;
import fmod.fmod.DummySoundListener;
import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.SoundListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Quaternion;
import zombie.AttackType;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.PredicatedFileWatcher;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.ai.State;
import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.ai.states.BumpedState;
import zombie.ai.states.ClimbDownSheetRopeState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CloseWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.FitnessState;
import zombie.ai.states.ForecastBeatenPlayerState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeState;
import zombie.ai.states.OpenWindowState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerActionsState;
import zombie.ai.states.PlayerAimState;
import zombie.ai.states.PlayerDraggingCorpse;
import zombie.ai.states.PlayerEmoteState;
import zombie.ai.states.PlayerExtState;
import zombie.ai.states.PlayerFallDownState;
import zombie.ai.states.PlayerFallingState;
import zombie.ai.states.PlayerGetUpState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.PlayerKnockedDown;
import zombie.ai.states.PlayerOnBedState;
import zombie.ai.states.PlayerOnGroundState;
import zombie.ai.states.PlayerSitOnFurnitureState;
import zombie.ai.states.PlayerSitOnGroundState;
import zombie.ai.states.PlayerStrafeState;
import zombie.ai.states.SmashWindowState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.StateManager;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.animals.AnimalAlertedState;
import zombie.ai.states.animals.AnimalEatState;
import zombie.ai.states.player.PlayerMilkAnimalState;
import zombie.ai.states.player.PlayerMovementState;
import zombie.ai.states.player.PlayerPetAnimalState;
import zombie.ai.states.player.PlayerShearAnimalState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSound;
import zombie.audio.MusicIntensityConfig;
import zombie.audio.MusicIntensityEvents;
import zombie.audio.MusicThreatStatuses;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterCharacterMoving;
import zombie.audio.parameters.ParameterCharacterOnFire;
import zombie.audio.parameters.ParameterCharacterVoicePitch;
import zombie.audio.parameters.ParameterCharacterVoiceType;
import zombie.audio.parameters.ParameterDeaf;
import zombie.audio.parameters.ParameterDragMaterial;
import zombie.audio.parameters.ParameterElevation;
import zombie.audio.parameters.ParameterEquippedBaggageContainer;
import zombie.audio.parameters.ParameterExercising;
import zombie.audio.parameters.ParameterFirearmInside;
import zombie.audio.parameters.ParameterFirearmRoomSize;
import zombie.audio.parameters.ParameterFootstepMaterial;
import zombie.audio.parameters.ParameterFootstepMaterial2;
import zombie.audio.parameters.ParameterIsStashTile;
import zombie.audio.parameters.ParameterLocalPlayer;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.audio.parameters.ParameterMoodles;
import zombie.audio.parameters.ParameterOverlapFoliageType;
import zombie.audio.parameters.ParameterPlayerHealth;
import zombie.audio.parameters.ParameterRoomTypeEx;
import zombie.audio.parameters.ParameterShoeType;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Fitness;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.CharacterTimedActions.LuaTimedActionNew;
import zombie.characters.Moodles.Moodles;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.BentFences;
import zombie.iso.IsoButcherHook;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.weather.ClimateManager;
import zombie.iso.zones.Zone;
import zombie.network.BodyDamageSync;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.PassengerMap;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.fields.IPositional;
import zombie.network.fields.hit.HitInfo;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.ConnectionQueueStatistic;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.Point;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.WeaponCategory;
import zombie.ui.TutorialManager;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;
import zombie.vehicles.VehiclesDB2;
import zombie.world.WorldDictionary;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;

@UsedFromLua
public class IsoPlayer extends IsoLivingCharacter implements IAnimalVisual, IHumanVisual, IPositional {
    private static final float StrongTraitMaxWeightDelta = 1.5F;
    private static final float WeakTraitMaxWeightDelta = 0.75F;
    private static final float FeebleTraitMaxWeightDelta = 0.9F;
    private static final float StoutTraitMaxWeightDelta = 1.25F;
    public PhysicsDebugRenderer physicsDebugRenderer;
    private AttackType attackType = AttackType.NONE;
    public static final String DEATH_MUSIC_NAME = "PlayerDied";
    private boolean allowSprint = true;
    private boolean allowRun = true;
    public static boolean isTestAIMode;
    public static final boolean NoSound = false;
    public static int assumedPlayer;
    public static int numPlayers = 1;
    public static final short MAX = 4;
    public static final IsoPlayer[] players = new IsoPlayer[4];
    private static IsoPlayer instance;
    private static final Object instanceLock = "IsoPlayer.instance Lock";
    private static final Vector2 testHitPosition = new Vector2();
    private static int followDeadCount = 240;
    private boolean ignoreAutoVault;
    public int remoteSneakLvl;
    public int remoteStrLvl;
    public int remoteFitLvl;
    public boolean moodleCantSprint;
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempVector2 = new Vector2();
    private static final String forwardStr = "Forward";
    private static final String backwardStr = "Backward";
    private static final String leftStr = "Left";
    private static final String rightStr = "Right";
    private static boolean coopPvp;
    private boolean ignoreContextKey;
    private boolean ignoreInputsForDirection;
    private long lastRemoteUpdate = 0L;
    public ArrayList<IsoAnimal> luredAnimals = new ArrayList<>();
    public boolean isLuringAnimals;
    private boolean invPageDirty;
    private final ArrayList<IsoAnimal> attachedAnimals = new ArrayList<>();
    public boolean spottedByPlayer;
    private final HashMap<Integer, Integer> spottedPlayerTimer = new HashMap<>();
    private float extUpdateCount;
    private static final int s_randomIdleFidgetInterval = 5000;
    private boolean attackStarted;
    private static final PredicatedFileWatcher m_isoPlayerTriggerWatcher = new PredicatedFileWatcher(
        ZomboidFileSystem.instance.getMessagingDirSub("Trigger_ResetIsoPlayerModel.xml"), IsoPlayer::onTrigger_ResetIsoPlayerModel
    );
    private final PredicatedFileWatcher setClothingTriggerWatcher;
    private static final Vector2 tempVector2_1 = new Vector2();
    private static final Vector2 tempVector2_2 = new Vector2();
    protected BaseVisual baseVisual;
    protected final ItemVisuals itemVisuals = new ItemVisuals();
    public boolean targetedByZombie;
    public float lastTargeted = 1.0E8F;
    public float timeSinceOpenDoor;
    public float timeSinceCloseDoor;
    public boolean remote;
    private int timeSinceLastNetData;
    public Role role = Roles.getDefaultForNewUser();
    public String tagPrefix = "";
    public boolean showTag = true;
    public boolean factionPvp;
    public short onlineId = 1;
    public int onlineChunkGridWidth;
    public boolean joypadMovementActive = true;
    public boolean joypadIgnoreAimUntilCentered;
    public boolean joypadIgnoreChargingRt;
    protected boolean joypadBDown;
    protected boolean joypadSprint;
    public boolean mpTorchCone;
    public float mpTorchDist;
    public float mpTorchStrength;
    public int playerIndex;
    public int serverPlayerIndex = 1;
    public float useChargeDelta;
    public int joypadBind = -1;
    public float contextPanic;
    public float numNearbyBuildingsRooms;
    public boolean isCharging;
    public boolean isChargingLt;
    private boolean lookingWhileInVehicle;
    private boolean climbOverWallSuccess;
    private boolean climbOverWallStruggle;
    private boolean justMoved;
    public float maxWeightDelta = 1.0F;
    public float currentSpeed;
    public boolean deathFinished;
    public boolean isSpeek;
    public boolean isVoiceMute;
    public final Vector2 playerMoveDir = new Vector2(0.0F, 0.0F);
    public BaseSoundListener soundListener;
    public String username = "Bob";
    public boolean dirtyRecalcGridStack = true;
    public float dirtyRecalcGridStackTime = 10.0F;
    public float runningTime;
    public float timePressedContext;
    public float chargeTime;
    private float useChargeTime;
    private boolean pressContext;
    private boolean letGoAfterContextIsReleased;
    public float closestZombie = 1000000.0F;
    public final Vector2 lastAngle = new Vector2();
    public String saveFileName;
    public boolean bannedAttacking;
    public int sqlId = -1;
    protected int clearSpottedTimer = -1;
    protected float timeSinceLastStab;
    protected Stack<IsoMovingObject> lastSpotted = new Stack<>();
    protected boolean changeCharacterDebounce;
    protected int followId;
    protected final Stack<IsoGameCharacter> followCamStack = new Stack<>();
    protected boolean seenThisFrame;
    protected boolean couldBeSeenThisFrame;
    protected float asleepTime;
    protected final Stack<IsoMovingObject> spottedList = new Stack<>();
    protected int ticksSinceSeenZombie = 9999999;
    protected boolean waiting = true;
    protected IsoSurvivor dragCharacter;
    protected float heartDelay = 30.0F;
    protected float heartDelayMax = 30.0F;
    protected String attackLoopSoundName;
    protected long attackLoopInstance;
    protected long attackLoopInstancePrev;
    protected boolean aimingWeaponAnimation;
    protected long heartEventInstance;
    protected long worldAmbianceInstance;
    protected int dialogMood = 1;
    protected int ping;
    protected IsoMovingObject dragObject;
    private double lastSeenZombieTime = 2.0;
    private BaseSoundEmitter worldAmbienceEmitter;
    private int checkSafehouse = 200;
    private boolean attackFromBehind;
    private long aimKeyDownMs;
    private long runKeyDownMs;
    private long sprintKeyDownMs;
    private int hypothermiaCache = -1;
    private int hyperthermiaCache = -1;
    private float ticksSincePressedMovement;
    private boolean flickTorch;
    private float checkNearbyRooms;
    private boolean useVehicle;
    private boolean usedVehicle;
    private float useVehicleDuration;
    private static final Vector3f tempVector3f = new Vector3f();
    private static final org.lwjgl.util.vector.Vector3f templwjglVector3f = new org.lwjgl.util.vector.Vector3f();
    private final IsoPlayer.InputState inputState = new IsoPlayer.InputState();
    private boolean isWearingNightVisionGoggles;
    private float moveSpeed = 0.06F;
    private int offSetXUi;
    private int offSetYUi;
    private float combatSpeed = 1.0F;
    private double hoursSurvived;
    private boolean isAuthorizedHandToHandAction = true;
    private boolean isAuthorizedHandToHand = true;
    private boolean blockMovement;
    private Nutrition nutrition;
    private Fitness fitness;
    private boolean forceOverrideAnim;
    private boolean initiateAttack;
    private final ColorInfo tagColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    private String displayName;
    private boolean seeNonPvpZone;
    private boolean seeDesignationZone;
    private final ArrayList<Double> selectedZonesForHighlight = new ArrayList<>();
    private Double selectedZoneForHighlight = 0.0;
    private final HashMap<Long, Long> mechanicsItem = new HashMap<>();
    private int sleepingPillsTaken;
    private long lastPillsTaken;
    private long heavyBreathInstance;
    private String heavyBreathSoundName;
    private boolean allChatMuted;
    private boolean forceAim;
    private boolean forceRun;
    private boolean forceSprint;
    private final boolean multiplayer;
    private String saveFileIp;
    protected BaseVehicle vehicle4testCollision;
    private long steamId;
    private final IsoPlayer.VehicleContainerData vehicleContainerData = new IsoPlayer.VehicleContainerData();
    private boolean isWalking;
    private int footInjuryTimer;
    private boolean sneakDebounce;
    private float turnDelta;
    protected boolean isPlayerMoving;
    private float walkSpeed;
    private float walkInjury;
    private float runSpeed;
    private float idleSpeed;
    private float deltaX;
    private float deltaY;
    private float windspeed;
    private float windForce;
    private float ipX;
    private float ipY;
    private float drunkDelayCommandTimer;
    private float pressedRunTimer;
    private boolean pressedRun;
    private boolean meleePressed;
    private boolean grapplePressed;
    private boolean canLetGoOfGrappled = true;
    private boolean lastAttackWasHandToHand;
    private boolean isPerformingAnAction;
    private final ArrayList<String> alreadyReadBook = new ArrayList<>();
    public byte bleedingLevel;
    public final NetworkPlayerAI networkAi;
    private final MusicIntensityEvents musicIntensityEvents = new MusicIntensityEvents();
    private final MusicThreatStatuses musicThreatStatuses = new MusicThreatStatuses(this);
    private final boolean musicIntensityInside = true;
    private boolean isFarming;
    private float attackVariationX;
    private float attackVariationY;
    public String accessLevel;
    private boolean hasObstacleOnPath;
    private LuaTimedActionNew timedActionToRetrigger;
    private final IsoPlayer.GrapplerGruntChance grapplerGruntChance = new IsoPlayer.GrapplerGruntChance();
    private final BallisticsController.AimingVectorParameters updateAimingVectorParams = new BallisticsController.AimingVectorParameters();
    private final PlayerCraftHistory craftHistory = new PlayerCraftHistory(this);
    public boolean autoDrink = true;
    private static final ArrayList<IsoPlayer> RecentlyRemoved = new ArrayList<>();
    private boolean pathfindRun;
    private static final IsoPlayer.MoveVars s_moveVars = new IsoPlayer.MoveVars();
    private final IsoPlayer.MoveVars drunkMoveVars = new IsoPlayer.MoveVars();
    private static final PZArrayList<HitInfo> s_targetsProne = new PZArrayList<>(HitInfo.class, 8);
    private static final PZArrayList<HitInfo> s_targetsStanding = new PZArrayList<>(HitInfo.class, 8);
    private boolean reloadButtonDown;
    private boolean rackButtonDown;
    private boolean reloadKeyDown;
    private boolean rackKeyDown;
    private long attackAnimThrowTimer = System.currentTimeMillis();
    private String weaponT;
    private final ArrayList<ContextualAction> contextualActions = new ArrayList<>();
    private final ParameterCharacterMoving parameterCharacterMoving = new ParameterCharacterMoving(this);
    private final ParameterCharacterMovementSpeed parameterCharacterMovementSpeed = new ParameterCharacterMovementSpeed(this);
    private final ParameterCharacterOnFire parameterCharacterOnFire = new ParameterCharacterOnFire(this);
    private final ParameterCharacterVoiceType parameterCharacterVoiceType = new ParameterCharacterVoiceType(this);
    private final ParameterCharacterVoicePitch parameterCharacterVoicePitch = new ParameterCharacterVoicePitch(this);
    private final ParameterDeaf parameterDeaf = new ParameterDeaf(this);
    private final ParameterDragMaterial parameterDragMaterial = new ParameterDragMaterial(this);
    private final ParameterElevation parameterElevation = new ParameterElevation(this);
    private final ParameterEquippedBaggageContainer parameterEquippedBaggageContainer = new ParameterEquippedBaggageContainer(this);
    private final ParameterExercising parameterExercising = new ParameterExercising(this);
    private final ParameterFirearmInside parameterFirearmInside = new ParameterFirearmInside(this);
    private final ParameterFirearmRoomSize parameterFirearmRoomSize = new ParameterFirearmRoomSize(this);
    private final ParameterFootstepMaterial parameterFootstepMaterial = new ParameterFootstepMaterial(this);
    private final ParameterFootstepMaterial2 parameterFootstepMaterial2 = new ParameterFootstepMaterial2(this);
    private final ParameterIsStashTile parameterIsStashTile = new ParameterIsStashTile(this);
    private final ParameterLocalPlayer parameterLocalPlayer = new ParameterLocalPlayer(this);
    private final ParameterMeleeHitSurface parameterMeleeHitSurface = new ParameterMeleeHitSurface(this);
    private final ParameterOverlapFoliageType parameterOverlapFoliageType = new ParameterOverlapFoliageType(this);
    private final ParameterPlayerHealth parameterPlayerHealth = new ParameterPlayerHealth(this);
    private final ParameterVehicleHitLocation parameterVehicleHitLocation = new ParameterVehicleHitLocation();
    private final ParameterShoeType parameterShoeType = new ParameterShoeType(this);
    private ParameterMoodles parameterMoodles;

    public IsoPlayer(IsoCell cell) {
        this(cell, null, 0, 0, 0);
    }

    public IsoPlayer(IsoCell cell, SurvivorDesc desc, int x, int y, int z, boolean isAnimal) {
        super(cell, x, y, z);
        this.setIsAnimal(isAnimal);
        if (this.isAnimal()) {
            this.baseVisual = new AnimalVisual(this);
        } else {
            this.baseVisual = new HumanVisual(this);
            this.registerVariableCallbacks();
            this.registerAnimEventCallbacks();
            this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
        }

        this.dir = IsoDirections.W;
        if (!isAnimal) {
            this.nutrition = new Nutrition(this);
            this.fitness = new Fitness(this);
            this.clothingWetness = new ClothingWetness(this);
            this.initAttachedItems("Human");
        }

        this.initWornItems("Human");
        if (desc != null) {
            this.descriptor = desc;
        } else {
            this.descriptor = new SurvivorDesc();
        }

        this.setFemale(this.descriptor.isFemale());
        this.setVoiceType(this.descriptor.getVoiceType());
        this.setVoicePitch(this.descriptor.getVoicePitch());
        if (!isAnimal) {
            this.Dressup(this.descriptor);
            this.getHumanVisual().copyFrom(this.descriptor.getHumanVisual());
            this.InitSpriteParts(this.descriptor);
        }

        LuaEventManager.triggerEvent("OnCreateLivingCharacter", this, this.descriptor);
        this.descriptor.setInstance(this);
        if (!isAnimal) {
            this.speakColour = new Color(Rand.Next(135) + 120, Rand.Next(135) + 120, Rand.Next(135) + 120, 255);
        }

        if (GameClient.client) {
            if (Core.getInstance().getMpTextColor() != null) {
                this.speakColour = new Color(
                    Core.getInstance().getMpTextColor().r, Core.getInstance().getMpTextColor().g, Core.getInstance().getMpTextColor().b, 1.0F
                );
            } else {
                Core.getInstance().setMpTextColor(new ColorInfo(this.speakColour.r, this.speakColour.g, this.speakColour.b, 1.0F));

                try {
                    Core.getInstance().saveOptions();
                } catch (IOException var8) {
                    var8.printStackTrace();
                }
            }
        }

        if (Core.gameMode.equals("LastStand")) {
            this.characterTraits.set(CharacterTrait.STRONG, true);
        }

        if (this.characterTraits.get(CharacterTrait.STRONG)) {
            this.maxWeightDelta = 1.5F;
        } else if (this.characterTraits.get(CharacterTrait.WEAK)) {
            this.maxWeightDelta = 0.75F;
        } else if (this.characterTraits.get(CharacterTrait.FEEBLE)) {
            this.maxWeightDelta = 0.9F;
        } else if (this.characterTraits.get(CharacterTrait.STOUT)) {
            this.maxWeightDelta = 1.25F;
        }

        this.multiplayer = GameServer.server || GameClient.client;
        this.vehicle4testCollision = null;
        if (!isAnimal && Core.debug && DebugOptions.instance.cheat.player.startInvisible.getValue()) {
            this.setGhostMode(true);
            this.setGodMod(true);
        }

        if (!isAnimal) {
            this.getActionContext().setGroup(ActionGroup.getActionGroup("player"));
            this.initializeStates();
            DebugFileWatcher.instance.add(m_isoPlayerTriggerWatcher);
        }

        this.setClothingTriggerWatcher = new PredicatedFileWatcher(
            ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetClothing.xml"), TriggerXmlFile.class, this::onTrigger_setClothingToXmlTriggerFile
        );
        this.networkAi = new NetworkPlayerAI(this);
        this.initFMODParameters();
    }

    public IsoPlayer(IsoCell cell, SurvivorDesc desc, int x, int y, int z) {
        super(cell, x, y, z);
        this.baseVisual = new HumanVisual(this);
        this.registerVariableCallbacks();
        this.registerAnimEventCallbacks();
        this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
        this.dir = IsoDirections.W;
        this.nutrition = new Nutrition(this);
        this.fitness = new Fitness(this);
        this.initWornItems("Human");
        this.initAttachedItems("Human");
        this.clothingWetness = new ClothingWetness(this);
        if (desc != null) {
            this.descriptor = desc;
        } else {
            this.descriptor = new SurvivorDesc();
        }

        this.setFemale(this.descriptor.isFemale());
        this.Dressup(this.descriptor);
        this.getHumanVisual().copyFrom(this.descriptor.getHumanVisual());
        this.InitSpriteParts(this.descriptor);
        LuaEventManager.triggerEvent("OnCreateLivingCharacter", this, this.descriptor);
        this.descriptor.setInstance(this);
        this.speakColour = new Color(Rand.Next(135) + 120, Rand.Next(135) + 120, Rand.Next(135) + 120, 255);
        if (GameClient.client) {
            if (Core.getInstance().getMpTextColor() != null) {
                this.speakColour = new Color(
                    Core.getInstance().getMpTextColor().r, Core.getInstance().getMpTextColor().g, Core.getInstance().getMpTextColor().b, 1.0F
                );
            } else {
                Core.getInstance().setMpTextColor(new ColorInfo(this.speakColour.r, this.speakColour.g, this.speakColour.b, 1.0F));

                try {
                    Core.getInstance().saveOptions();
                } catch (IOException var7) {
                    var7.printStackTrace();
                }
            }
        }

        if (Core.gameMode.equals("LastStand")) {
            this.characterTraits.set(CharacterTrait.STRONG, true);
        }

        if (this.characterTraits.get(CharacterTrait.STRONG)) {
            this.maxWeightDelta = 1.5F;
        } else if (this.characterTraits.get(CharacterTrait.WEAK)) {
            this.maxWeightDelta = 0.75F;
        } else if (this.characterTraits.get(CharacterTrait.FEEBLE)) {
            this.maxWeightDelta = 0.9F;
        } else if (this.characterTraits.get(CharacterTrait.STOUT)) {
            this.maxWeightDelta = 1.25F;
        }

        this.multiplayer = GameServer.server || GameClient.client;
        this.vehicle4testCollision = null;
        if (Core.debug && DebugOptions.instance.cheat.player.startInvisible.getValue()) {
            this.setGhostMode(true);
            this.setGodMod(true);
        }

        this.getActionContext().setGroup(ActionGroup.getActionGroup("player"));
        this.initializeStates();
        DebugFileWatcher.instance.add(m_isoPlayerTriggerWatcher);
        this.setClothingTriggerWatcher = new PredicatedFileWatcher(
            ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetClothing.xml"), TriggerXmlFile.class, this::onTrigger_setClothingToXmlTriggerFile
        );
        this.networkAi = new NetworkPlayerAI(this);
        this.setVoiceType(this.descriptor.getVoiceType());
        this.setVoicePitch(this.descriptor.getVoicePitch());
        this.initFMODParameters();
        if (!this.isAnimal()) {
            this.ai.initPlayerAI();
        }
    }

    public void setOnlineID(short value) {
        this.onlineId = value;
    }

    private void registerVariableCallbacks() {
        this.setVariable("CombatSpeed", () -> this.combatSpeed, val -> this.combatSpeed = val, owner -> "The combat speed multiplier. Usually 0.0 - 1.0");
        this.setVariable(
            "TurnDelta",
            () -> this.turnDelta,
            val -> this.turnDelta = val,
            owner -> "The rate of turn per frame. Multiplier: 0.0 - 1.0 but often can be set much higher."
        );
        this.setVariable(
            "blockMovement",
            this::isBlockMovement,
            this::setBlockMovement,
            owner -> "Is the character blocking movement commands. Eg while busy crawling through a window, the character cannot accept other movement commands."
        );
        this.setVariable("sneaking", this::isSneaking, this::setSneaking, owner -> "Is the character sneaking.");
        this.setVariable("initiateAttack", this::isInitiateAttack, this::setInitiateAttack, owner -> "The character wishes to initiate an attack.");
        this.setVariable("isMoving", this::isPlayerMoving, owner -> "The character is moving.");
        this.setVariable("isRunning", this::isRunning, this::setRunning, owner -> "The character is running.");
        this.setVariable("run", this::isRunning, this::setRunning, owner -> "The character is running.");
        this.setVariable("isSprinting", this::isSprinting, this::setSprinting, owner -> "The character is sprinting.");
        this.setVariable("sprint", this::isSprinting, this::setSprinting, owner -> "The character is sprinting.");
        this.setVariable("isStrafing", this::isStrafing, owner -> "The character is strafing.");
        this.setVariable("WalkSpeed", () -> this.walkSpeed, val -> this.walkSpeed = val, owner -> "Walk speed multiplier. Usually 0.0 - 1.0 but can be higher.");
        this.setVariable("WalkInjury", () -> this.walkInjury, val -> this.walkInjury = val, owner -> "Walk speed while injured. Multiplier.");
        this.setVariable("RunSpeed", () -> this.runSpeed, val -> this.runSpeed = val, owner -> "Running speed. Multiplier.");
        this.setVariable("IdleSpeed", () -> this.idleSpeed, val -> this.idleSpeed = val, owner -> "Idle speed. Multiplier.");
        this.setVariable("DeltaX", () -> this.deltaX, val -> this.deltaX = val, owner -> "Movement rate per frame, along the X axis.");
        this.setVariable("DeltaY", () -> this.deltaY, val -> this.deltaY = val, owner -> "Movement rate per frame, along the Y axis.");
        this.setVariable("Windspeed", () -> this.windspeed, val -> this.windspeed = val, owner -> "The character is experiencing wind moving at this speed.");
        this.setVariable("WindForce", () -> this.windForce, val -> this.windForce = val, owner -> "The character is experiencing wind at this force.");
        this.setVariable("IPX", () -> this.ipX, val -> this.ipX = val, owner -> "The character's impulse-movement rate per frame, along the X axis.");
        this.setVariable("IPY", () -> this.ipY, val -> this.ipY = val, owner -> "The character's impulse-movement rate per frame, along the Y axis.");
        this.setVariable("attacktype", this::getAttackTypeAnimationKey, owner -> "The character is performing an attack of this type.");
        this.setVariable("attackfrombehind", () -> this.attackFromBehind, owner -> "The character is attacking a target from behind.");
        this.setVariable("bundervehicle", this::isUnderVehicle, owner -> "The character is currently underneath a vehicle.");
        this.setVariable("reanimatetimer", this::getReanimateTimer, owner -> "The character's waiting to reanimate. Timer countdown.");
        this.setVariable("isattacking", this::isAttacking, owner -> "The character is currently performing an attack.");
        this.setVariable("beensprintingfor", this::getBeenSprintingFor, owner -> "The character has been sprinting for this amount of time.");
        this.setVariable("bannedAttacking", () -> this.bannedAttacking, owner -> "Can this character perform an attack.");
        this.setVariable("meleePressed", () -> this.meleePressed, owner -> "The character detected an input to perform a melee attack.");
        this.setVariable("grapplePressed", () -> this.grapplePressed, owner -> "The character detected an input to perform a grapple.");
        this.setVariable("canLetGoOfGrappled", () -> this.canLetGoOfGrappled, owner -> "The character can let go of its grappled target.");
        this.setVariable("Weapon", this::getWeaponType, this::setWeaponType, owner -> "The character's equipped weapon type.");
        this.setVariable("BumpFall", false);
        this.setVariable("bClient", () -> GameClient.client, owner -> "Network: The character is in a multiplier game, client-side.");
        this.setVariable("bRemote", () -> this.remote, owner -> "Network: The character is in a multiplier game, and is a remote character.");
        this.setVariable(
            "IsPerformingAnAction", this::isPerformingAnAction, this::setPerformingAnAction, owner -> "The character is currently performing an action."
        );
        this.setVariable("bShoveAiming", this::isShovingWhileAiming, owner -> "The character is currently aiming, and performing a shove.");
        this.setVariable("bGrappleAiming", this::isGrapplingWhileAiming, owner -> "The character is currently aiming, and performing a grapple.");
        this.setVariable("AttackVariationX", this::getAttackVariationX, owner -> "The character's attack varation, along the x-axis.");
        this.setVariable("AttackVariationY", this::getAttackVariationY, owner -> "The character's attack variation, along the y-axis.");
        this.setVariable(
            "sneakLimpSpeedScale", this::getSneakLimpSpeedScale, this::setSneakLimpSpeedScale, owner -> "Get the character Limp speed scale while sneaking."
        );
    }

    private void registerAnimEventCallbacks() {
        this.addAnimEventListener("GrapplerRandomGrunt", this::OnAnimEvent_GrapplerPlayRandomGrunt);
    }

    private void onGrappleEnded() {
        this.setDoGrappleLetGoAfterContextKeyIsReleased(false);
    }

    private void OnAnimEvent_GrapplerPlayRandomGrunt(IsoGameCharacter in_owner, String in_gruntSound) {
        if (Rand.Next(100) < this.grapplerGruntChance.gruntChance) {
            this.grapplerGruntChance.gruntChance = 15;
            String[] gruntSounds = in_gruntSound.split(",");
            int grundSoundIdx = Rand.Next(0, gruntSounds.length);
            String gruntSound = gruntSounds[grundSoundIdx];
            DebugLog.Zombie.trace("Dragging corpse. Grunting: %s", gruntSound);
            this.stopPlayerVoiceSound(gruntSound);
            this.playerVoiceSound(gruntSound);
        } else {
            this.grapplerGruntChance.gruntChance += 5;
        }
    }

    @Override
    protected Vector2 getDeferredMovement(Vector2 out_result, boolean in_reset) {
        super.getDeferredMovement(out_result, in_reset);
        if (DebugOptions.instance.cheat.player.invisibleSprint.getValue()
            && this.isGhostMode()
            && (this.IsRunning() || this.isSprinting())
            && !this.isCurrentState(ClimbOverFenceState.instance())
            && !this.isCurrentState(ClimbThroughWindowState.instance())
            && !this.isCurrentState(PlayerGetUpState.instance())) {
            if (this.getPath2() == null && !this.pressedMovement(false) && !this.isAutoWalk()) {
                return out_result.set(0.0F, 0.0F);
            }

            if (this.getCurrentBuilding() != null) {
                out_result.scale(2.5F);
                return out_result;
            }

            out_result.scale(7.5F);
        }

        return out_result;
    }

    @Override
    public float getTurnDelta() {
        return DebugOptions.instance.cheat.player.invisibleSprint.getValue()
                && this.isGhostMode()
                && (this.isRunning() || this.isSprinting())
                && !this.isSittingOnFurniture()
            ? 10.0F
            : super.getTurnDelta();
    }

    public void setPerformingAnAction(boolean val) {
        this.isPerformingAnAction = val;
    }

    public boolean isPerformingAnAction() {
        return this.isPerformingAnAction;
    }

    @Override
    public boolean isAttacking() {
        return !this.isAttackType(AttackType.NONE);
    }

    @Override
    public boolean shouldBeTurning() {
        return this.getAnimationPlayer().getMultiTrack().getTrackCount() == 0 ? false : super.shouldBeTurning();
    }

    /**
     * The IsoPlayer.instance thread-safe invoke.
     *   Calls the supplied callback if the IsoPlayer.instance is non-null.
     *   Performs this in a thread-safe manner.
     * 
     *   It is intended that, should any thread intend to use the IsoPlayer.instance, and does not want another thread
     *   to change the ptr in the meanwhile, it should call invokeOnPlayerInstance(Runnable callback)
     * 
     *   eg.
     *   IsoPlayer.invokeOnPlayerInstance(()->
     *     {
     *         IsoPlayer.instance.doStuff();
     *     }
     */
    public static void invokeOnPlayerInstance(Runnable callback) {
        synchronized (instanceLock) {
            if (instance != null) {
                callback.run();
            }
        }
    }

    public static IsoPlayer getInstance() {
        return instance;
    }

    public static void setInstance(IsoPlayer newInstance) {
        synchronized (instanceLock) {
            instance = newInstance;
        }
    }

    public static boolean hasInstance() {
        return instance != null;
    }

    private static void onTrigger_ResetIsoPlayerModel(String entryKey) {
        if (instance != null) {
            DebugLog.log(DebugType.General, "DebugFileWatcher Hit. Resetting player model: " + entryKey);
            instance.resetModel();
        } else {
            DebugLog.log(DebugType.General, "DebugFileWatcher Hit. Player instance null : " + entryKey);
        }
    }

    public static int getFollowDeadCount() {
        return followDeadCount;
    }

    public static void setFollowDeadCount(int aFollowDeadCount) {
        followDeadCount = aFollowDeadCount;
    }

    public static ArrayList<String> getAllFileNames() {
        ArrayList<String> names = new ArrayList<>();
        String saveDir = ZomboidFileSystem.instance.getCurrentSaveDir();

        for (int n = 1; n < 100; n++) {
            File file = new File(saveDir + File.separator + "map_p" + n + ".bin");
            if (file.exists()) {
                names.add("map_p" + n + ".bin");
            }
        }

        return names;
    }

    public static String getUniqueFileName() {
        int max = 0;
        String saveDir = ZomboidFileSystem.instance.getCurrentSaveDir();

        for (int n = 1; n < 100; n++) {
            File file = new File(saveDir + File.separator + "map_p" + n + ".bin");
            if (file.exists()) {
                max = n;
            }
        }

        return ZomboidFileSystem.instance.getFileNameInCurrentSave("map_p" + ++max + ".bin");
    }

    public static ArrayList<IsoPlayer> getAllSavedPlayers() {
        ArrayList<IsoPlayer> players;
        if (GameClient.client) {
            players = ClientPlayerDB.getInstance().getAllNetworkPlayers();
        } else {
            players = PlayerDB.getInstance().getAllLocalPlayers();
        }

        for (int i = players.size() - 1; i >= 0; i--) {
            if (players.get(i).isDead()) {
                players.remove(i);
            }
        }

        return players;
    }

    public static boolean isServerPlayerIDValid(String id) {
        if (GameClient.client) {
            String ServerPlayerID = ServerOptions.instance.serverPlayerId.getValue();
            return ServerPlayerID != null && !ServerPlayerID.isEmpty() ? ServerPlayerID.equals(id) : true;
        } else {
            return true;
        }
    }

    public static int getPlayerIndex() {
        return instance == null ? assumedPlayer : instance.playerIndex;
    }

    public int getIndex() {
        return this.playerIndex;
    }

    public static boolean allPlayersDead() {
        for (int n = 0; n < numPlayers; n++) {
            if (players[n] != null && !players[n].isDead()) {
                return false;
            }
        }

        return IsoWorld.instance == null || IsoWorld.instance.addCoopPlayers.isEmpty();
    }

    public static ArrayList<IsoPlayer> getPlayers() {
        return new ArrayList<>(Arrays.asList(players));
    }

    public static boolean allPlayersAsleep() {
        int numLiving = 0;
        int numSleeping = 0;

        for (int pn = 0; pn < numPlayers; pn++) {
            if (players[pn] != null && !players[pn].isDead()) {
                numLiving++;
                if (players[pn] != null && players[pn].isAsleep()) {
                    numSleeping++;
                }
            }
        }

        return numLiving > 0 && numLiving == numSleeping;
    }

    public static boolean getCoopPVP() {
        return coopPvp;
    }

    public static void setCoopPVP(boolean enabled) {
        coopPvp = enabled;
    }

    public void TestAnimalSpotPlayer(IsoAnimal chr) {
        float dist = IsoUtils.DistanceManhatten(chr.getX(), chr.getY(), this.getX(), this.getY());
        chr.spotted(this, false, dist);
    }

    public void TestZombieSpotPlayer(IsoMovingObject chr) {
        if (GameServer.server && chr instanceof IsoZombie isoZombie && isoZombie.target != this && isoZombie.isLeadAggro(this)) {
            GameServer.updateZombieControl(isoZombie, this.onlineId);
        } else {
            chr.spotted(this, false);
            if (chr instanceof IsoZombie) {
                float dist = chr.DistTo(this);
                if (dist < this.closestZombie && !chr.isOnFloor()) {
                    this.closestZombie = dist;
                }
            }
        }
    }

    public float getPathSpeed() {
        float speed = this.getMoveSpeed() * 0.9F;
        switch (this.moodles.getMoodleLevel(MoodleType.ENDURANCE)) {
            case 1:
                speed *= 0.95F;
                break;
            case 2:
                speed *= 0.9F;
                break;
            case 3:
                speed *= 0.8F;
                break;
            case 4:
                speed *= 0.6F;
        }

        if (this.stats.isEnduranceRecharging()) {
            speed *= 0.85F;
        }

        if (this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) > 0) {
            float weight = this.getInventory().getCapacityWeight();
            float maxWeight = this.getMaxWeight();
            float ratio = Math.min(2.0F, weight / maxWeight) - 1.0F;
            speed *= 0.65F + 0.35F * (1.0F - ratio);
        }

        return speed;
    }

    public boolean isGhostMode() {
        return this.isInvisible();
    }

    public void setGhostMode(boolean aGhostMode, boolean isForced) {
        this.setInvisible(aGhostMode, isForced);
    }

    public void setGhostMode(boolean aGhostMode) {
        this.setInvisible(aGhostMode);
    }

    public boolean isSeeEveryone() {
        return Core.debug && DebugOptions.instance.cheat.player.seeEveryone.getValue();
    }

    public Vector2 getPlayerMoveDir() {
        return this.playerMoveDir;
    }

    public void setPlayerMoveDir(Vector2 aPlayerMoveDir) {
        this.playerMoveDir.set(aPlayerMoveDir);
    }

    @Override
    public void Move(Vector2 dir) {
        super.Move(dir);
        this.setCurrentSquareFromPosition();
    }

    @Override
    public void MoveUnmodded(Vector2 dir) {
        if (this.getSlowFactor() > 0.0F) {
            dir.x = dir.x * (1.0F - this.getSlowFactor());
            dir.y = dir.y * (1.0F - this.getSlowFactor());
        }

        super.MoveUnmodded(dir);
        this.setCurrentSquareFromPosition();
    }

    public void nullifyAiming() {
        if (this.isForceAim()) {
            this.toggleForceAim();
        }

        this.isCharging = false;
        this.setIsAiming(false);
    }

    public boolean isAimKeyDown() {
        if (this.playerIndex != 0) {
            return false;
        } else {
            int aimKey = GameKeyboard.whichKeyDown("Aim");
            if (aimKey == 0) {
                return false;
            } else {
                boolean bAimKeyIsCtrl = aimKey == 29 || aimKey == 157;
                return !bAimKeyIsCtrl || !UIManager.isMouseOverInventory();
            }
        }
    }

    public boolean isAimKeyDownIgnoreMouse() {
        if (this.playerIndex != 0) {
            return false;
        } else {
            int aimKey = GameKeyboard.whichKeyDownIgnoreMouse("Aim");
            if (aimKey == 0) {
                return false;
            } else {
                boolean bAimKeyIsCtrl = aimKey == 29 || aimKey == 157;
                return !bAimKeyIsCtrl || !UIManager.isMouseOverInventory();
            }
        }
    }

    private void initializeStates() {
        this.clearAIStateMap();
        if (this.getVehicle() == null) {
            this.registerAIState("actions", PlayerActionsState.instance());
            this.registerAIState("aim", PlayerAimState.instance());
            this.registerAIState("aim-sneak", PlayerAimState.instance());
            this.registerAIState("aim-strafe", PlayerStrafeState.instance());
            this.registerAIState("bumped", BumpedState.instance());
            this.registerAIState("bumped-bump", BumpedState.instance());
            this.registerAIState("climbdownrope", ClimbDownSheetRopeState.instance());
            this.registerAIState("climbfence", ClimbOverFenceState.instance());
            this.registerAIState("climbrope", ClimbSheetRopeState.instance());
            this.registerAIState("climbwall", ClimbOverWallState.instance());
            this.registerAIState("climbwindow", ClimbThroughWindowState.instance());
            this.registerAIState("closewindow", CloseWindowState.instance());
            this.registerAIState("collide", CollideWithWallState.instance());
            this.registerAIState("emote", PlayerEmoteState.instance());
            this.registerAIState("ext", PlayerExtState.instance());
            this.registerAIState("falldown", PlayerFallDownState.instance());
            this.registerAIState("falling", PlayerFallingState.instance());
            this.registerAIState("fishing", FishingState.instance());
            this.registerAIState("fitness", FitnessState.instance());
            this.registerAIState("getup", PlayerGetUpState.instance());
            this.registerAIState("hitreaction", PlayerHitReactionState.instance());
            this.registerAIState("hitreaction-hit", PlayerHitReactionPVPState.instance());
            this.registerAIState("hitreactionpvp", PlayerHitReactionPVPState.instance());
            this.registerAIState("idle", IdleState.instance());
            this.registerAIState("knockeddown", PlayerKnockedDown.instance());
            this.registerAIState("melee", SwipeStatePlayer.instance());
            this.registerAIState("milkanimal", PlayerMilkAnimalState.instance());
            this.registerAIState("movement", PlayerMovementState.instance());
            this.registerAIState("onbed", PlayerOnBedState.instance());
            this.registerAIState("onground", PlayerOnGroundState.instance());
            this.registerAIState("openwindow", OpenWindowState.instance());
            this.registerAIState("petanimal", PlayerPetAnimalState.instance());
            this.registerAIState("ranged", SwipeStatePlayer.instance());
            this.registerAIState("run", PlayerMovementState.instance());
            this.registerAIState("shearanimal", PlayerShearAnimalState.instance());
            this.registerAIState("shove", SwipeStatePlayer.instance());
            this.registerAIState("shoveWithFirearm", SwipeStatePlayer.instance());
            this.registerAIState("shoveWithHandgun", SwipeStatePlayer.instance());
            this.registerAIState("shoveAim", SwipeStatePlayer.instance());
            this.registerAIState("stomp", SwipeStatePlayer.instance());
            this.registerAIState("sitext", PlayerExtState.instance());
            this.registerAIState("sitonfurniture", PlayerSitOnFurnitureState.instance());
            this.registerAIState("sitonground", PlayerSitOnGroundState.instance());
            this.registerAIState("smashwindow", SmashWindowState.instance());
            this.registerAIState("sprint", PlayerMovementState.instance());
            this.registerAIState("strafe", PlayerStrafeState.instance());
            this.registerAIState("grappleGrab", SwipeStatePlayer.instance());
            this.registerAIState("pickUpBody", SwipeStatePlayer.instance());
            this.registerAIState("pickUpBody-HeadEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("pickUpBody-HeadEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("pickUpBody-LegsEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("pickUpBody-LegsEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("draggingBody-HeadEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("draggingBody-HeadEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("draggingBody-LegsEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("draggingBody-LegsEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("layDownBody-HeadEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("layDownBody-HeadEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("layDownBody-LegsEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("layDownBody-LegsEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOutWindow-HeadEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOutWindow-HeadEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOutWindow-LegsEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOutWindow-LegsEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOverFence-HeadEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOverFence-HeadEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOverFence-LegsEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyOverFence-LegsEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyIntoContainer-HeadEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyIntoContainer-HeadEnd-onFront", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyIntoContainer-LegsEnd-onBack", PlayerDraggingCorpse.instance());
            this.registerAIState("throwBodyIntoContainer-LegsEnd-onFront", PlayerDraggingCorpse.instance());
        } else {
            this.registerAIState("aim", PlayerAimState.instance());
            this.registerAIState("idle", IdleState.instance());
            this.registerAIState("melee", SwipeStatePlayer.instance());
            this.registerAIState("shove", SwipeStatePlayer.instance());
            this.registerAIState("shoveWithFirearm", SwipeStatePlayer.instance());
            this.registerAIState("shoveWithHandgun", SwipeStatePlayer.instance());
            this.registerAIState("shoveAim", SwipeStatePlayer.instance());
            this.registerAIState("stomp", SwipeStatePlayer.instance());
            this.registerAIState("ranged", SwipeStatePlayer.instance());
        }
    }

    @Override
    protected void onAnimPlayerCreated(AnimationPlayer animationPlayer) {
        super.onAnimPlayerCreated(animationPlayer);
        if (!this.isAnimal()) {
            animationPlayer.addBoneReparent("Bip01_L_Thigh", "Bip01");
            animationPlayer.addBoneReparent("Bip01_R_Thigh", "Bip01");
            animationPlayer.addBoneReparent("Bip01_L_Clavicle", "Bip01_Spine1");
            animationPlayer.addBoneReparent("Bip01_R_Clavicle", "Bip01_Spine1");
            animationPlayer.addBoneReparent("Bip01_Prop1", "Bip01_R_Hand");
            animationPlayer.addBoneReparent("Bip01_Prop2", "Bip01_L_Hand");
        }
    }

    @Override
    public String GetAnimSetName() {
        return this.getVehicle() == null ? "player" : "player-vehicle";
    }

    public boolean IsInMeleeAttack() {
        return this.isCurrentState(SwipeStatePlayer.instance());
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        int serialise = input.get();
        byte objectID = input.get();
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.setHoursSurvived(input.getDouble());
        SurvivorDesc desc = this.descriptor;
        this.setFemale(desc.isFemale());
        this.InitSpriteParts(desc);
        this.speakColour = new Color(Rand.Next(135) + 120, Rand.Next(135) + 120, Rand.Next(135) + 120, 255);
        if (GameClient.client) {
            if (Core.getInstance().getMpTextColor() != null) {
                this.speakColour = new Color(
                    Core.getInstance().getMpTextColor().r, Core.getInstance().getMpTextColor().g, Core.getInstance().getMpTextColor().b, 1.0F
                );
            } else {
                Core.getInstance().setMpTextColor(new ColorInfo(this.speakColour.r, this.speakColour.g, this.speakColour.b, 1.0F));

                try {
                    Core.getInstance().saveOptions();
                } catch (IOException var15) {
                    var15.printStackTrace();
                }
            }
        }

        this.setZombieKills(input.getInt());
        ArrayList<InventoryItem> items = this.savedInventoryItems;
        int wornItemCount = input.get();

        for (int i = 0; i < wornItemCount; i++) {
            ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(input)));
            short index = input.getShort();
            if (index >= 0 && index < items.size() && this.wornItems.getBodyLocationGroup().getLocation(itemBodyLocation) != null) {
                this.wornItems.setItem(itemBodyLocation, items.get(index));
            }
        }

        short index = input.getShort();
        if (index >= 0 && index < items.size()) {
            this.leftHandItem = items.get(index);
        }

        index = input.getShort();
        if (index >= 0 && index < items.size()) {
            this.rightHandItem = items.get(index);
        }

        this.setVariable("Weapon", WeaponType.getWeaponType(this).getType());
        this.setSurvivorKills(input.getInt());
        this.initSpritePartsEmpty();
        this.nutrition.load(input);
        this.setAllChatMuted(input.get() == 1);
        this.tagPrefix = GameWindow.ReadString(input);
        this.setTagColor(new ColorInfo(input.getFloat(), input.getFloat(), input.getFloat(), 1.0F));
        this.setDisplayName(GameWindow.ReadString(input));
        this.showTag = input.get() == 1;
        this.factionPvp = input.get() == 1;
        if (WorldVersion >= 239) {
            this.setAutoDrink(input.get() == 1);
        }

        if (WorldVersion >= 198) {
            input.get();
        } else {
            input.get();
        }

        if (input.get() == 1) {
            this.savedVehicleX = input.getFloat();
            this.savedVehicleY = input.getFloat();
            this.savedVehicleSeat = input.get();
            this.savedVehicleRunning = input.get() == 1;
            this.setZ(0.0F);
        }

        int size = input.getInt();

        for (int ix = 0; ix < size; ix++) {
            this.mechanicsItem.put(input.getLong(), input.getLong());
        }

        this.fitness.load(input, WorldVersion);
        int nb = input.getShort();

        for (int ix = 0; ix < nb; ix++) {
            short registryItemId = input.getShort();
            String fullType = WorldDictionary.getItemTypeFromID(registryItemId);
            if (fullType != null) {
                this.alreadyReadBook.add(fullType);
            }
        }

        this.loadKnownMediaLines(input, WorldVersion);
        if (WorldVersion >= 203) {
            this.setVoiceType(input.get());
        }

        if (WorldVersion >= 228) {
            this.craftHistory.load(input);
        }
    }

    public void setExtraInfoFlags(byte flags, boolean isForced) {
        this.setGodMod((flags & 1) != 0, isForced);
        this.setGhostMode((flags & 2) != 0, isForced);
        this.setInvisible((flags & 4) != 0, isForced);
        this.setNoClip((flags & 8) != 0, isForced);
        this.setShowAdminTag((flags & 16) != 0);
        this.setCanHearAll((flags & 32) != 0);
    }

    public byte getExtraInfoFlags() {
        boolean showAdminTag = this.isInvisible()
            || this.isGodMod()
            || this.isGhostMode()
            || this.isNoClip()
            || this.isTimedActionInstantCheat()
            || this.isUnlimitedCarry()
            || this.isUnlimitedEndurance()
            || this.isBuildCheat()
            || this.isFarmingCheat()
            || this.isFishingCheat()
            || this.isHealthCheat()
            || this.isMechanicsCheat()
            || this.isMovablesCheat()
            || this.canSeeAll()
            || this.canHearAll()
            || this.isZombiesDontAttack()
            || this.isShowMPInfos();
        return (byte)(
            (this.isGodMod() ? 1 : 0)
                | (this.isGhostMode() ? 2 : 0)
                | (this.isInvisible() ? 4 : 0)
                | (this.isNoClip() ? 8 : 0)
                | (showAdminTag ? 16 : 0)
                | (this.canHearAll() ? 32 : 0)
        );
    }

    @Override
    public String getDescription(String in_separatorStr) {
        String s = this.getClass().getSimpleName() + " [" + in_separatorStr;
        s = s + super.getDescription(in_separatorStr + "    ") + " | " + in_separatorStr;
        s = s + "hoursSurvived=" + this.getHoursSurvived() + " | " + in_separatorStr;
        s = s + "zombieKills=" + this.getZombieKills() + " | " + in_separatorStr;
        s = s + "wornItems=";

        for (int i = 0; i < this.getWornItems().size() - 1; i++) {
            s = s + this.getWornItems().get(i).getItem() + ", ";
        }

        s = s + " | " + in_separatorStr;
        s = s + "primaryHandItem=" + this.getPrimaryHandItem() + " | " + in_separatorStr;
        s = s + "secondaryHandType=" + this.getSecondaryHandType() + " | " + in_separatorStr;
        s = s + "survivorKills=" + this.getSurvivorKills() + " | " + in_separatorStr;
        if (this.isAllChatMuted()) {
            s = s + "AllChatMuted | " + in_separatorStr;
        }

        s = s
            + "tag="
            + this.tagPrefix
            + ", color=("
            + this.getTagColor().r
            + ", "
            + this.getTagColor().g
            + ", "
            + this.getTagColor().b
            + ") | "
            + in_separatorStr;
        s = s + "displayName=" + this.displayName + " | " + in_separatorStr;
        if (this.showTag) {
            s = s + "showTag | " + in_separatorStr;
        }

        if (this.factionPvp) {
            s = s + "factionPvp | " + in_separatorStr;
        }

        if (this.isNoClip()) {
            s = s + "noClip | " + in_separatorStr;
        }

        if (this.vehicle != null) {
            s = s
                + "vehicle [ pos=("
                + this.vehicle.getX()
                + ", "
                + this.vehicle.getY()
                + ") seat="
                + this.vehicle.getSeat(this)
                + " isEngineRunning="
                + this.vehicle.isEngineRunning()
                + " ] "
                + in_separatorStr;
        }

        s = s + "mechanicsItem=";

        for (int i = 0; i < this.mechanicsItem.size() - 1; i++) {
            s = s + this.mechanicsItem.get(i) + ", ";
        }

        s = s + " | " + in_separatorStr;
        s = s + "alreadyReadBook=";

        for (int i = 0; i < this.alreadyReadBook.size() - 1; i++) {
            s = s + this.alreadyReadBook.get(i) + ", ";
        }

        return s + " ] ";
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        IsoPlayer oldInstance = instance;
        instance = this;

        try {
            super.save(output, IS_DEBUG_SAVE);
        } finally {
            instance = oldInstance;
        }

        output.putDouble(this.getHoursSurvived());
        output.putInt(this.getZombieKills());
        if (this.wornItems.size() > 127) {
            throw new RuntimeException("too many worn items");
        } else {
            output.put((byte)this.wornItems.size());
            this.wornItems.forEach(wornItem -> {
                GameWindow.WriteString(output, Registries.ITEM_BODY_LOCATION.getLocation(wornItem.getLocation()).toString());
                output.putShort((short)this.savedInventoryItems.indexOf(wornItem.getItem()));
            });
            output.putShort((short)this.savedInventoryItems.indexOf(this.getPrimaryHandItem()));
            output.putShort((short)this.savedInventoryItems.indexOf(this.getSecondaryHandItem()));
            output.putInt(this.getSurvivorKills());
            this.nutrition.save(output);
            output.put((byte)(this.isAllChatMuted() ? 1 : 0));
            GameWindow.WriteString(output, this.tagPrefix);
            output.putFloat(this.getTagColor().r);
            output.putFloat(this.getTagColor().g);
            output.putFloat(this.getTagColor().b);
            GameWindow.WriteString(output, this.displayName);
            output.put((byte)(this.showTag ? 1 : 0));
            output.put((byte)(this.factionPvp ? 1 : 0));
            output.put((byte)(this.getAutoDrink() ? 1 : 0));
            output.put(this.getExtraInfoFlags());
            if (this.vehicle != null) {
                output.put((byte)1);
                output.putFloat(this.vehicle.getX());
                output.putFloat(this.vehicle.getY());
                output.put((byte)this.vehicle.getSeat(this));
                output.put((byte)(this.vehicle.isEngineRunning() ? 1 : 0));
            } else {
                output.put((byte)0);
            }

            output.putInt(this.mechanicsItem.size());

            for (Long itemid : this.mechanicsItem.keySet()) {
                output.putLong(itemid);
                output.putLong(this.mechanicsItem.get(itemid));
            }

            this.fitness.save(output);
            output.putShort((short)this.alreadyReadBook.size());

            for (int i = 0; i < this.alreadyReadBook.size(); i++) {
                output.putShort(WorldDictionary.getItemRegistryID(this.alreadyReadBook.get(i)));
            }

            this.saveKnownMediaLines(output);
            output.put((byte)this.getVoiceType());
            this.craftHistory.save(output);
        }
    }

    public void save() throws IOException {
        synchronized (SliceY.SliceBufferLock) {
            ByteBuffer output = SliceY.SliceBuffer;
            output.clear();
            output.put((byte)80);
            output.put((byte)76);
            output.put((byte)89);
            output.put((byte)82);
            output.putInt(240);
            GameWindow.WriteString(output, this.multiplayer ? ServerOptions.instance.serverPlayerId.getValue() : "");
            output.putInt(PZMath.fastfloor(this.getX() / 8.0F));
            output.putInt(PZMath.fastfloor(this.getY() / 8.0F));
            output.putInt(PZMath.fastfloor(this.getX()));
            output.putInt(PZMath.fastfloor(this.getY()));
            output.putInt(PZMath.fastfloor(this.getZ()));
            this.save(output);
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_p.bin"));
            if (!Core.getInstance().isNoSave()) {
                try (
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                ) {
                    bos.write(output.array(), 0, output.position());
                }
            }

            if (this.getVehicle() != null && !GameClient.client) {
                VehiclesDB2.instance.updateVehicleAndTrailer(this.getVehicle());
            }
        }
    }

    public void save(String fileName) throws IOException {
        this.saveFileName = fileName;
        synchronized (SliceY.SliceBufferLock) {
            SliceY.SliceBuffer.clear();
            SliceY.SliceBuffer.putInt(240);
            GameWindow.WriteString(SliceY.SliceBuffer, this.multiplayer ? ServerOptions.instance.serverPlayerId.getValue() : "");
            this.save(SliceY.SliceBuffer);
            File outFile = new File(fileName).getAbsoluteFile();

            try (
                FileOutputStream fos = new FileOutputStream(outFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
            ) {
                bos.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
            }
        }
    }

    public void load(String fileName) throws IOException {
        File inFile = new File(fileName).getAbsoluteFile();
        if (inFile.exists()) {
            this.saveFileName = fileName;

            try (
                FileInputStream inStream = new FileInputStream(inFile);
                BufferedInputStream input = new BufferedInputStream(inStream);
            ) {
                synchronized (SliceY.SliceBufferLock) {
                    SliceY.SliceBuffer.clear();
                    int numBytes = input.read(SliceY.SliceBuffer.array());
                    SliceY.SliceBuffer.limit(numBytes);
                    int worldVersion = SliceY.SliceBuffer.getInt();
                    this.saveFileIp = GameWindow.ReadStringUTF(SliceY.SliceBuffer);
                    this.load(SliceY.SliceBuffer, worldVersion);
                }
            }
        }
    }

    @Override
    public void removeFromWorld() {
        this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
        this.getEmitter().stopSoundLocal(this.vocalEvent);
        this.vocalEvent = 0L;
        if (this.worldAmbienceEmitter != null) {
            this.worldAmbienceEmitter.stopAll();
            this.worldAmbienceEmitter = null;
            this.worldAmbianceInstance = 0L;
        }

        this.removedFromWorldMs = System.currentTimeMillis();
        if (!(this instanceof IsoAnimal) && !RecentlyRemoved.contains(this)) {
            RecentlyRemoved.add(this);
        }

        super.removeFromWorld();
    }

    public static void UpdateRemovedEmitters() {
        IsoCell cell = IsoWorld.instance.currentCell;
        long currentMS = System.currentTimeMillis();

        for (int i = RecentlyRemoved.size() - 1; i >= 0; i--) {
            IsoPlayer player = RecentlyRemoved.get(i);
            if ((cell.getObjectList().contains(player) || cell.getAddList().contains(player)) && !cell.getRemoveList().contains(player)) {
                RecentlyRemoved.remove(i);
            } else {
                player.getFMODParameters().update();
                player.getEmitter().tick();
                if (currentMS - player.removedFromWorldMs > 5000L) {
                    player.getEmitter().stopAll();
                    RecentlyRemoved.remove(i);
                }
            }
        }
    }

    public static void Reset() {
        for (int i = 0; i < 4; i++) {
            IsoPlayer player = players[i];
            if (player != null) {
                player.getAdvancedAnimator().reset();
                player.releaseAnimationPlayer();
                player.getSpottedList().clear();
                player.lastSpotted.clear();
            }

            players[i] = null;
        }

        RecentlyRemoved.clear();
    }

    public void setVehicle4TestCollision(BaseVehicle vehicle) {
        this.vehicle4testCollision = vehicle;
    }

    public boolean isSaveFileInUse() {
        for (int pn = 0; pn < numPlayers; pn++) {
            IsoPlayer player2 = players[pn];
            if (player2 != null) {
                if (this.sqlId != -1 && this.sqlId == player2.sqlId) {
                    return true;
                }

                if (this.saveFileName != null && this.saveFileName.equals(player2.saveFileName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void removeSaveFile() {
        try {
            if (PlayerDB.isAvailable()) {
                PlayerDB.getInstance().saveLocalPlayersForce();
            }

            if (this.isNPC() && this.saveFileName != null) {
                File file = new File(this.saveFileName).getAbsoluteFile();
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception var2) {
            ExceptionLogger.logException(var2);
        }
    }

    public boolean isSaveFileIPValid() {
        return isServerPlayerIDValid(this.saveFileIp);
    }

    @Override
    public String getObjectName() {
        return "Player";
    }

    public int getJoypadBind() {
        return this.joypadBind;
    }

    public boolean isLBPressed() {
        return this.joypadBind == -1 ? false : JoypadManager.instance.isLBPressed(this.joypadBind);
    }

    public Vector2 getControllerAimDir(Vector2 vec) {
        if (GameWindow.activatedJoyPad != null && this.joypadBind != -1 && this.joypadMovementActive) {
            float xVal2 = JoypadManager.instance.getAimingAxisX(this.joypadBind);
            float yVal2 = JoypadManager.instance.getAimingAxisY(this.joypadBind);
            if (this.joypadIgnoreAimUntilCentered) {
                if (vec.set(xVal2, yVal2).getLengthSquared() > 0.0F) {
                    return vec.set(0.0F, 0.0F);
                }

                this.joypadIgnoreAimUntilCentered = false;
            }

            if (vec.set(xVal2, yVal2).getLength() < 0.3F) {
                yVal2 = 0.0F;
                xVal2 = 0.0F;
            }

            if (xVal2 == 0.0F && yVal2 == 0.0F) {
                return vec.set(0.0F, 0.0F);
            }

            vec.set(xVal2, yVal2);
            vec.normalize();
            vec.rotate((float) (-Math.PI / 4));
        }

        return vec;
    }

    public Vector2 getMouseAimVector(Vector2 vec) {
        int mx = Mouse.getX();
        int my = Mouse.getY();
        vec.x = IsoUtils.XToIso(mx, my + 55.0F * this.def.getScaleY(), this.getZ()) - this.getX();
        vec.y = IsoUtils.YToIso(mx, my + 55.0F * this.def.getScaleY(), this.getZ()) - this.getY();
        vec.normalize();
        return vec;
    }

    public Vector2 getAimVector(Vector2 vec) {
        return this.joypadBind == -1 ? this.getMouseAimVector(vec) : this.getControllerAimDir(vec);
    }

    @Override
    public float getGlobalMovementMod(boolean bDoNoises) {
        return !this.isGhostMode() && !this.isNoClip() ? super.getGlobalMovementMod(bDoNoises) : 1.0F;
    }

    @Override
    protected void doTreeNoises() {
        if (this instanceof IsoAnimal) {
            super.doTreeNoises();
        }
    }

    @Override
    public boolean isInTrees2(boolean ignoreBush) {
        return !this.isGhostMode() && !this.isNoClip() ? super.isInTrees2(ignoreBush) : false;
    }

    public float getMoveSpeed() {
        float minDelta = 1.0F;

        for (int i = BodyPartType.ToIndex(BodyPartType.UpperLeg_L); i <= BodyPartType.ToIndex(BodyPartType.Foot_R); i++) {
            BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
            float delta = 1.0F;
            if (part.getFractureTime() > 20.0F) {
                delta = 0.4F;
                if (part.getFractureTime() > 50.0F) {
                    delta = 0.3F;
                }

                if (part.getSplintFactor() > 0.0F) {
                    delta += part.getSplintFactor() / 10.0F;
                }
            }

            if (part.getFractureTime() < 20.0F && part.getSplintFactor() > 0.0F) {
                delta = 0.8F;
            }

            if (delta > 0.7F && part.getDeepWoundTime() > 0.0F) {
                delta = 0.7F;
                if (part.bandaged()) {
                    delta += 0.2F;
                }
            }

            if (delta < minDelta) {
                minDelta = delta;
            }
        }

        if (minDelta != 1.0F) {
            return this.moveSpeed * minDelta;
        } else if (this.getMoodles().getMoodleLevel(MoodleType.PANIC) >= 4 && this.characterTraits.get(CharacterTrait.ADRENALINE_JUNKIE)) {
            float del = 1.0F;
            int mul = this.getMoodles().getMoodleLevel(MoodleType.PANIC) + 1;
            del += mul / 50.0F;
            return this.moveSpeed * del;
        } else {
            return this.moveSpeed;
        }
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    @Override
    public float getTorchStrength() {
        if (this.isAnimal()) {
            return 0.0F;
        } else if (this.remote) {
            return this.mpTorchStrength;
        } else {
            InventoryItem item = this.getActiveLightItem();
            return item != null ? item.getLightStrength() : 0.0F;
        }
    }

    public float getInvAimingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Aiming);
        if (level == 1) {
            return 0.9F;
        } else if (level == 2) {
            return 0.86F;
        } else if (level == 3) {
            return 0.82F;
        } else if (level == 4) {
            return 0.74F;
        } else if (level == 5) {
            return 0.7F;
        } else if (level == 6) {
            return 0.66F;
        } else if (level == 7) {
            return 0.62F;
        } else if (level == 8) {
            return 0.58F;
        } else if (level == 9) {
            return 0.54F;
        } else {
            return level == 10 ? 0.5F : 0.9F;
        }
    }

    public float getAimingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Aiming);
        if (level == 1) {
            return 1.1F;
        } else if (level == 2) {
            return 1.14F;
        } else if (level == 3) {
            return 1.18F;
        } else if (level == 4) {
            return 1.22F;
        } else if (level == 5) {
            return 1.26F;
        } else if (level == 6) {
            return 1.3F;
        } else if (level == 7) {
            return 1.34F;
        } else if (level == 8) {
            return 1.36F;
        } else if (level == 9) {
            return 1.4F;
        } else {
            return level == 10 ? 1.5F : 1.0F;
        }
    }

    public float getReloadingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Reloading);
        return 3.5F - level * 0.25F;
    }

    public float getAimingRangeMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Aiming);
        if (level == 1) {
            return 1.2F;
        } else if (level == 2) {
            return 1.28F;
        } else if (level == 3) {
            return 1.36F;
        } else if (level == 4) {
            return 1.42F;
        } else if (level == 5) {
            return 1.5F;
        } else if (level == 6) {
            return 1.58F;
        } else if (level == 7) {
            return 1.66F;
        } else if (level == 8) {
            return 1.72F;
        } else if (level == 9) {
            return 1.8F;
        } else {
            return level == 10 ? 2.0F : 1.1F;
        }
    }

    public boolean isPathfindRunning() {
        return this.pathfindRun;
    }

    public void setPathfindRunning(boolean newvalue) {
        this.pathfindRun = newvalue;
    }

    public boolean isBannedAttacking() {
        return this.bannedAttacking;
    }

    public void setBannedAttacking(boolean b) {
        this.bannedAttacking = b;
    }

    public float getInvAimingRangeMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Aiming);
        if (level == 1) {
            return 0.8F;
        } else if (level == 2) {
            return 0.7F;
        } else if (level == 3) {
            return 0.62F;
        } else if (level == 4) {
            return 0.56F;
        } else if (level == 5) {
            return 0.45F;
        } else if (level == 6) {
            return 0.38F;
        } else if (level == 7) {
            return 0.31F;
        } else if (level == 8) {
            return 0.24F;
        } else if (level == 9) {
            return 0.17F;
        } else {
            return level == 10 ? 0.1F : 0.8F;
        }
    }

    private void updateCursorVisibility() {
        if (this.isAiming()) {
            if (this.playerIndex == 0 && this.joypadBind == -1 && !this.isDead()) {
                if (!Core.getInstance().getOptionShowCursorWhileAiming()) {
                    if (Core.getInstance().getIsoCursorVisibility() != 0) {
                        if (!UIManager.isForceCursorVisible()) {
                            int mouseX = Mouse.getXA();
                            int mouseY = Mouse.getYA();
                            if (mouseX >= IsoCamera.getScreenLeft(0) && mouseX <= IsoCamera.getScreenLeft(0) + IsoCamera.getScreenWidth(0)) {
                                if (mouseY >= IsoCamera.getScreenTop(0) && mouseY <= IsoCamera.getScreenTop(0) + IsoCamera.getScreenHeight(0)) {
                                    Mouse.setCursorVisible(false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (Core.getInstance().isDisplayPlayerModel() || this.isAnimal() || !this.isLocal()) {
            if (!this.attachedAnimals.isEmpty()) {
                for (int i = 0; i < this.attachedAnimals.size(); i++) {
                    this.attachedAnimals.get(i).drawRope(this);
                }
            }

            if (DebugOptions.instance.character.debug.render.displayRoomAndZombiesZone.getValue()) {
                String txt = "";
                if (this.getCurrentRoomDef() != null) {
                    txt = this.getCurrentRoomDef().name;
                }

                Zone zombieZone = ZombiesZoneDefinition.getDefinitionZoneAt(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
                if (zombieZone != null) {
                    txt = txt + " - " + zombieZone.name + " / " + zombieZone.type;
                }

                this.Say(txt);
            }

            if (!getInstance().checkCanSeeClient(this)) {
                this.setTargetAlpha(0.0F);
                getInstance().spottedPlayerTimer.remove(this.getRemoteID());
            } else {
                this.setTargetAlpha(1.0F);
            }

            super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);
        }
    }

    @Override
    public void renderlast() {
        super.renderlast();
        if (DebugOptions.instance.character.debug.render.fmodRoomType.getValue() && this.isLocalPlayer()) {
            ParameterRoomTypeEx.render(this);
        }
    }

    public float doBeatenVehicle(float vehicleSpeed) {
        if (GameClient.client && this.isLocalPlayer()) {
            this.changeState(ForecastBeatenPlayerState.instance());
            return 0.0F;
        } else if (!GameClient.client && !this.isLocalPlayer()) {
            return 0.0F;
        } else {
            float damage = this.getDamageFromHitByACar(vehicleSpeed);
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "CARHITDAMAGE", damage);
            if (this.isAlive()) {
                if (GameClient.client) {
                    if (this.isCurrentState(PlayerSitOnGroundState.instance())) {
                        this.setKnockedDown(ServerOptions.getInstance().knockedDownAllowed.getValue());
                        this.setReanimateTimer(20.0F);
                    } else if (!this.isOnFloor()
                        && !(vehicleSpeed > 15.0F)
                        && !this.isCurrentState(PlayerHitReactionState.instance())
                        && !this.isCurrentState(PlayerGetUpState.instance())
                        && !this.isCurrentState(PlayerOnGroundState.instance())) {
                        this.setHitReaction("HitReaction");
                        this.getActionContext().reportEvent("washit");
                        this.setVariable("hitpvp", false);
                    } else {
                        this.setHitReaction("HitReaction");
                        this.getActionContext().reportEvent("washit");
                        this.setVariable("hitpvp", false);
                        this.setKnockedDown(ServerOptions.getInstance().knockedDownAllowed.getValue());
                        this.setReanimateTimer(20.0F);
                    }
                } else if (this.getCurrentState() != PlayerHitReactionState.instance()
                    && this.getCurrentState() != PlayerFallDownState.instance()
                    && this.getCurrentState() != PlayerOnGroundState.instance()
                    && !this.isKnockedDown()) {
                    if (damage > 15.0F) {
                        this.setKnockedDown(ServerOptions.getInstance().knockedDownAllowed.getValue());
                        this.setReanimateTimer(20.0F + Rand.Next(60));
                    }

                    this.setHitReaction("HitReaction");
                    this.getActionContext().reportEvent("washit");
                }
            }

            return damage;
        }
    }

    @Override
    public void update() {
        try (AbstractPerformanceProfileProbe ignored = IsoPlayer.s_performance.update.profile()) {
            this.updateInternal1();
        }
    }

    private void updateInternal1() {
        if (this.isAnimal()) {
            super.update();
            AnimalInstanceManager.getInstance().update((IsoAnimal)this);
            if (GameClient.client) {
                this.remote = true;
                this.updateRemotePlayer();
                this.setVariable("bMoving", this.isPlayerMoving());
            }
        } else {
            boolean updateBaseAndSend = this.updateInternal2();
            if (updateBaseAndSend) {
                if (!this.remote) {
                    this.updateLOS();
                }

                super.update();
            }

            GameClient.instance.sendPlayer2(this);
        }
    }

    private void setBeenMovingSprinting() {
        if (this.isJustMoved()) {
            this.setBeenMovingFor(this.getBeenMovingFor() + 1.25F * GameTime.getInstance().getMultiplier());
        } else {
            this.setBeenMovingFor(this.getBeenMovingFor() - 0.625F * GameTime.getInstance().getMultiplier());
        }

        if (this.isJustMoved() && this.isSprinting()) {
            this.setBeenSprintingFor(this.getBeenSprintingFor() + 1.25F * GameTime.getInstance().getMultiplier());
        } else {
            this.setBeenSprintingFor(0.0F);
        }
    }

    private boolean updateInternal2() {
        if (Core.debug && GameKeyboard.isKeyPressed(28)) {
            FBORenderChunkManager.instance.clearCache();
        }

        if (isTestAIMode) {
            this.isNpc = true;
        }

        if (!GameServer.server && !this.isNpc) {
            if ((this.falling || this.fallTime > 0.0F || this.isOnFire()) && UIManager.speedControls.getCurrentGameSpeed() > 1) {
                UIManager.speedControls.SetCurrentGameSpeed(1);
            }

            if ((this.falling || this.fallTime > FallingConstants.isFallingThreshold) && this.isGrappling()) {
                this.setDoGrapple(false);
                this.LetGoOfGrappled("Aborted");
            }
        }

        if (!this.attackStarted) {
            this.setInitiateAttack(false);
            this.setAttackType(AttackType.NONE);
        }

        if ((this.isRunning() || this.isSprinting()) && this.getDeferredMovement(tempo).getLengthSquared() > 0.0F) {
            this.runningTime = this.runningTime + GameTime.getInstance().getThirtyFPSMultiplier();
        } else {
            this.runningTime = 0.0F;
        }

        if (this.getLastCollideTime() > 0.0F) {
            this.setLastCollideTime(this.getLastCollideTime() - GameTime.getInstance().getThirtyFPSMultiplier());
        }

        this.updateDeathDragDown();
        this.updateGodModeKey();
        this.networkAi.update();
        this.doDeferredMovement();
        if (GameServer.server) {
            this.vehicle4testCollision = null;
        } else if (GameClient.client) {
            if (this.vehicle4testCollision != null) {
                if (!this.isLocal()) {
                    this.vehicle4testCollision.updateHitByVehicle(this);
                }

                this.vehicle4testCollision = null;
            }
        } else {
            this.updateHitByVehicle();
            this.vehicle4testCollision = null;
        }

        this.updateEquippedItemSounds();
        this.updateEmitter();
        if (!this.isAnimal()) {
            this.updateMechanicsItems();
            this.updateHeavyBreathing();
            this.updateTemperatureCheck();
            if (this.isWeaponReady()) {
                this.updateAimingStance();
            }

            if (this.isLocal() && this.getPlayerNum() == 0) {
                CombatManager.targetReticleMode = this.useHandWeapon != null
                        && this.useHandWeapon.isAimedFirearm()
                        && !this.useHandWeapon.hasTag(ItemTag.FAKE_WEAPON)
                    ? 1
                    : 0;
            }

            if (SystemDisabler.doCharacterStats) {
                this.nutrition.update();
            }

            this.fitness.update();
            this.updateVisionEffectTargets();
        }

        this.updateSoundListener();
        SafetySystemManager.update(this);
        if (!GameClient.client && !GameServer.server && this.deathFinished) {
            return false;
        } else {
            if (!GameClient.client && this.getCurrentBuildingDef() != null && !this.isInvisible()) {
                this.getCurrentBuildingDef().setHasBeenVisited(true);
            }

            if (this.checkSafehouse > 0 && GameServer.server) {
                this.checkSafehouse--;
                if (this.checkSafehouse == 0) {
                    this.checkSafehouse = 200;
                    SafeHouse safe = SafeHouse.isSafeHouse(this.getCurrentSquare(), null, false);
                    if (safe != null) {
                        safe.updateSafehouse(this);
                        safe.checkTrespass(this);
                        this.updateDisguisedState();
                    }
                }
            }

            if (this.remote && this.getTimeSinceLastNetData() > 600) {
                IsoWorld.instance.currentCell.getObjectList().remove(this);
                if (this.movingSq != null) {
                    this.movingSq.getMovingObjects().remove(this);
                }
            }

            this.setTimeSinceLastNetData(this.getTimeSinceLastNetData() + (int)GameTime.instance.getMultiplier());
            this.timeSinceOpenDoor = this.timeSinceOpenDoor + GameTime.instance.getMultiplier();
            this.timeSinceCloseDoor = this.timeSinceCloseDoor + GameTime.instance.getMultiplier();
            this.lastTargeted = this.lastTargeted + GameTime.instance.getMultiplier();
            this.targetedByZombie = false;
            this.checkActionGroup();
            if (this.isJustMoved() && !this.isNpc && !this.hasPath() && !this.isCurrentActionPathfinding()) {
                if (!GameServer.server && UIManager.getSpeedControls().getCurrentGameSpeed() > 1) {
                    UIManager.getSpeedControls().SetCurrentGameSpeed(1);
                }
            } else if (!GameClient.client
                && this.stats.get(CharacterStat.ENDURANCE) < this.stats.getEnduranceDangerWarning()
                && Rand.Next((int)(300.0F * GameTime.instance.getInvMultiplier())) == 0) {
                if (GameServer.server) {
                    GameServer.addXp(this, PerkFactory.Perks.Fitness, 1.0F);
                } else {
                    this.xp.AddXP(PerkFactory.Perks.Fitness, 1.0F);
                }
            }

            float delta = 1.0F;
            float d = 0.0F;
            if (this.isJustMoved() && !this.isNpc) {
                if (!this.isRunning() && !this.isSprinting()) {
                    d = 1.0F;
                } else {
                    d = 1.5F;
                }

                if (!GameClient.client && !GameServer.server) {
                    LuaEventManager.triggerEvent("OnPlayerMove", this);
                }
            }

            if (this.updateRemotePlayer()) {
                if (this.updateWhileDead()) {
                    return true;
                } else {
                    this.updateAttackLoopSound();
                    this.updateBringToBearSound();
                    this.updateHeartSound();
                    this.updateDraggingCorpseSounds();
                    this.checkIsNearWall();
                    this.checkIsNearVehicle();
                    this.updateExt();
                    this.setBeenMovingSprinting();
                    this.updateAimingDelay();
                    if (this.getVehicle() != null) {
                        this.updateEnduranceWhileInVehicle();
                    } else {
                        this.updateEndurance();
                    }

                    return true;
                }
            } else {
                assert !GameServer.server;

                assert !this.remote;

                assert !GameClient.client || this.isLocalPlayer();

                IsoCamera.setCameraCharacter(this);
                instance = this;
                if (this.isLocalPlayer()) {
                    IsoCamera.cameras[this.playerIndex].update();
                    if (UIManager.getMoodleUI(this.playerIndex) != null) {
                        UIManager.getMoodleUI(this.playerIndex).setCharacter(this);
                    }
                }

                if (this.closestZombie > 1.2F) {
                    this.slowTimer = -1.0F;
                    this.slowFactor = 0.0F;
                }

                this.contextPanic = this.contextPanic - 1.5F * GameTime.instance.getTimeDelta();
                if (this.contextPanic < 0.0F) {
                    this.contextPanic = 0.0F;
                }

                this.lastSeenZombieTime = this.lastSeenZombieTime + GameTime.instance.getGameWorldSecondsSinceLastUpdate() / 60.0F / 60.0F;
                LuaEventManager.triggerEvent("OnPlayerUpdate", this);
                if (this.pressedMovement(false)) {
                    this.contextPanic = 0.0F;
                    this.ticksSincePressedMovement = 0.0F;
                } else {
                    this.ticksSincePressedMovement = this.ticksSincePressedMovement + GameTime.getInstance().getThirtyFPSMultiplier();
                }

                this.setVariable("pressedMovement", this.pressedMovement(true));
                if (this.updateWhileDead()) {
                    return true;
                } else {
                    this.updateAttackLoopSound();
                    this.updateBringToBearSound();
                    this.updateHeartSound();
                    this.updateDraggingCorpseSounds();
                    this.updateVocalProperties();
                    this.updateEquippedBaggageContainer();
                    this.updateWorldAmbiance();
                    this.updateSneakKey();
                    this.checkIsNearWall();
                    this.checkIsNearVehicle();
                    this.updateExt();
                    this.updateInteractKeyPanic();
                    this.updateMusicIntensityEvents();
                    this.updateMusicThreatStatuses();
                    if (this.isAsleep()) {
                        this.isPlayerMoving = false;
                    }

                    if (this.getVehicle() != null
                        && this.getVehicle().isDriver(this)
                        && this.getVehicle().hasHorn()
                        && Core.getInstance().getKey("Shout") == Core.getInstance().getKey("VehicleHorn")) {
                    }

                    if (this.getIgnoreMovement() || this.isAsleep()) {
                        this.clearUseKeyVariables();
                        return true;
                    } else if (this.checkActionsBlockingMovement()) {
                        if (this.getVehicle() != null && this.getVehicle().getDriver() == this && this.getVehicle().getController() != null) {
                            this.getVehicle().getController().clientControls.reset();
                            this.getVehicle().updatePhysics();
                        }

                        this.clearUseKeyVariables();
                        return true;
                    } else {
                        this.enterExitVehicle();
                        this.checkActionGroup();
                        this.checkReloading();
                        this.checkWalkTo();
                        if (this.checkActionsBlockingMovement()) {
                            this.clearUseKeyVariables();
                            return true;
                        } else if (this.getVehicle() != null) {
                            this.updateWhileInVehicle();
                            return true;
                        } else {
                            this.checkVehicleContainers();
                            this.setCollidable(true);
                            this.updateCursorVisibility();
                            this.seenThisFrame = false;
                            this.couldBeSeenThisFrame = false;
                            if (IsoCamera.getCameraCharacter() == null && GameClient.client) {
                                IsoCamera.setCameraCharacter(instance);
                            }

                            if (this.updateUseKey()) {
                                return true;
                            } else {
                                this.updateEnableModelsKey();
                                this.updateChangeCharacterKey();
                                boolean isAttacking = false;
                                boolean bMelee = false;
                                boolean bGrapple = false;
                                this.setRunning(false);
                                this.setSprinting(false);
                                this.useChargeTime = this.chargeTime;
                                if (!this.isBlockMovement() && !this.isNpc) {
                                    if (!this.isCharging && !this.isChargingLt) {
                                        this.chargeTime = 0.0F;
                                    } else {
                                        this.chargeTime = this.chargeTime + 1.0F * GameTime.instance.getMultiplier();
                                    }

                                    this.UpdateInputState(this.inputState);
                                    bMelee = this.inputState.melee;
                                    bGrapple = this.inputState.grapple;
                                    isAttacking = this.inputState.isAttacking;
                                    this.setRunning(this.inputState.running);
                                    this.setSprinting(this.inputState.sprinting);
                                    if (this.isSprinting() && !this.isJustMoved()) {
                                        this.setSprinting(false);
                                    }

                                    if (this.isSprinting()) {
                                        this.setRunning(false);
                                    }

                                    if (this.inputState.sprinting && !this.isSprinting()) {
                                        this.setRunning(true);
                                    }

                                    if (this.isPickingUpBody() || this.isPuttingDownBody()) {
                                        this.inputState.isAiming = false;
                                    }

                                    this.setIsAiming(this.inputState.isAiming);
                                    this.isCharging = this.inputState.isCharging;
                                    this.isChargingLt = this.inputState.isChargingLt;
                                    this.updateMovementRates();
                                    if (this.isAiming()) {
                                        this.StopAllActionQueueAiming();
                                    }

                                    if (isAttacking) {
                                        this.setIsAiming(true);
                                    }

                                    this.waiting = false;
                                    if (this.isAiming()) {
                                        this.setMoving(false);
                                        this.setRunning(false);
                                        this.setSprinting(false);
                                    }

                                    if (this.isFarming()) {
                                        this.setIsAiming(false);
                                    }

                                    this.ticksSinceSeenZombie++;
                                }

                                if (this.playerMoveDir.x == 0.0F && this.playerMoveDir.y == 0.0F) {
                                    this.setForceRun(false);
                                    this.setForceSprint(false);
                                }

                                this.movementLastFrame.x = this.playerMoveDir.x;
                                this.movementLastFrame.y = this.playerMoveDir.y;
                                if (this.stateMachine.getCurrent() != StaggerBackState.instance()
                                    && this.stateMachine.getCurrent() != FakeDeadZombieState.instance()
                                    && UIManager.speedControls != null) {
                                    if (GameKeyboard.isKeyDown(88) && Translator.debug) {
                                        Translator.loadFiles();
                                    }

                                    this.setJustMoved(false);
                                    IsoPlayer.MoveVars moveVars = s_moveVars;
                                    this.updateMovementFromInput(moveVars);
                                    if (!this.justMoved && this.hasPath() && this.getPathFindBehavior2().shouldBeMoving()) {
                                        this.setJustMoved(true);
                                    }

                                    float strafeX = moveVars.strafeX;
                                    float strafeY = moveVars.strafeY;
                                    this.updateAimingDelay();
                                    this.setBeenMovingSprinting();
                                    delta *= d;
                                    if (delta > 1.0F) {
                                        delta *= this.getSprintMod();
                                    }

                                    if (delta > 1.0F && this.characterTraits.get(CharacterTrait.ATHLETIC)) {
                                        delta *= 1.2F;
                                    }

                                    if (delta > 1.0F) {
                                        if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                                            delta *= 0.99F;
                                        }

                                        if (this.characterTraits.get(CharacterTrait.OBESE)) {
                                            delta *= 0.85F;
                                        }

                                        if (this.getNutrition().getWeight() > 120.0) {
                                            delta *= 0.97F;
                                        }

                                        if (this.characterTraits.get(CharacterTrait.OUT_OF_SHAPE)) {
                                            delta *= 0.99F;
                                        }

                                        if (this.characterTraits.get(CharacterTrait.UNFIT)) {
                                            delta *= 0.8F;
                                        }
                                    }

                                    this.updateEndurance();
                                    if (this.isAiming() && this.isJustMoved()) {
                                        delta *= 0.7F;
                                    }

                                    if (this.isAiming()) {
                                        delta *= this.getNimbleMod();
                                    }

                                    this.isWalking = delta > 0.0F && !this.isNpc;
                                    if (this.isJustMoved()) {
                                        this.sprite.animate = true;
                                    }

                                    if (this.isNpc) {
                                        bMelee = this.ai.doUpdatePlayerControls(bMelee);
                                    }

                                    this.meleePressed = bMelee;
                                    this.grapplePressed = bGrapple;
                                    if (!this.grapplePressed) {
                                        this.setDoGrapple(false);
                                        this.canLetGoOfGrappled = true;
                                    }

                                    if (!this.pressContext && this.isDoGrappleLetGoAfterContextKeyIsReleased()) {
                                        this.setDoGrapple(false);
                                        this.setDoGrappleLetGo();
                                    }

                                    if (bMelee) {
                                        if (!this.lastAttackWasHandToHand) {
                                            this.setMeleeDelay(Math.min(this.getMeleeDelay(), 2.0F));
                                        }

                                        if (!this.bannedAttacking
                                            && this.isAuthorizedHandToHand()
                                            && !this.isPerformingAttackAnimation()
                                            && this.getMeleeDelay() <= 0.0F) {
                                            this.setDoShove(true);
                                            this.setDoGrapple(false);
                                            if (!this.isCharging && !this.isChargingLt) {
                                                this.setIsAiming(false);
                                            }

                                            this.AttemptAttack(this.useChargeTime);
                                            this.useChargeTime = 0.0F;
                                            this.chargeTime = 0.0F;
                                        }
                                    } else if (bGrapple) {
                                        if (!this.lastAttackWasHandToHand) {
                                            this.setMeleeDelay(Math.min(this.getMeleeDelay(), 2.0F));
                                        }

                                        if (!this.bannedAttacking && this.isAuthorizedHandToHand() && this.CanAttack() && this.getMeleeDelay() <= 0.0F) {
                                            if (!this.isGrappling()) {
                                                this.canLetGoOfGrappled = false;
                                                this.setDoGrapple(true);
                                                if (!this.isCharging && !this.isChargingLt) {
                                                    this.setIsAiming(false);
                                                }
                                            } else if (this.canLetGoOfGrappled) {
                                                if (this.pressContext) {
                                                    this.setDoGrappleLetGoAfterContextKeyIsReleased(true);
                                                } else {
                                                    this.setDoGrapple(false);
                                                    this.setDoGrappleLetGo();
                                                }
                                            }

                                            this.useChargeTime = 0.0F;
                                            this.chargeTime = 0.0F;
                                        }
                                    } else if (this.isAiming() && this.CanAttack()) {
                                        if (this.dragCharacter != null) {
                                            this.dragObject = null;
                                            this.dragCharacter.dragging = false;
                                            this.dragCharacter = null;
                                        }

                                        if (isAttacking && !this.bannedAttacking) {
                                            this.sprite.animate = true;
                                            if (this.getRecoilDelay() <= 0.0F && this.getMeleeDelay() <= 0.0F) {
                                                this.AttemptAttack(this.useChargeTime);
                                            }

                                            this.useChargeTime = 0.0F;
                                            this.chargeTime = 0.0F;
                                        }
                                    } else if (this.isHeadLookAround()) {
                                        int headIndex = this.getAnimationPlayer().getSkinningData().boneIndices.get("Bip01_Head");
                                        TwistableBoneTransform headBone = this.getAnimationPlayer().getBone(headIndex);
                                        Vector2 desiredForward2f = tempo;
                                        desiredForward2f.set(0.0F, 0.0F);
                                        if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
                                            this.getControllerAimDir(desiredForward2f);
                                            if (desiredForward2f.getLengthSquared() < 0.001F) {
                                                this.setHeadLookAroundDirection(0.0F, 0.0F);
                                            }
                                        } else if (Mouse.isButtonDown(0)) {
                                            float sourceX = this.getX();
                                            float sourceY = this.getY();
                                            float sourceZ = this.getZ();
                                            int mx = Mouse.getX();
                                            int my = Mouse.getY();
                                            headBone.getPosition(templwjglVector3f);
                                            float posZ = templwjglVector3f.z;
                                            float raycastZ = sourceZ + posZ;
                                            float raycastX = IsoUtils.XToIso(mx, my, raycastZ);
                                            float raycastY = IsoUtils.YToIso(mx, my, raycastZ);
                                            desiredForward2f.set(raycastX - sourceX, raycastY - sourceY);
                                        } else {
                                            this.setHeadLookAroundDirection(0.0F, 0.0F);
                                        }

                                        if (desiredForward2f.getLengthSquared() > 0.0F) {
                                            desiredForward2f.normalize();
                                            Vector2 forwardDirection = this.getForwardDirection(tempVector2);
                                            float headAngle = PZMath.angleBetween(forwardDirection, desiredForward2f);
                                            headAngle = PZMath.clamp(headAngle, -this.getHeadLookAngleMax(), this.getHeadLookAngleMax());
                                            float headAngleNormalizedX = headAngle / this.getHeadLookAngleMax();
                                            Quaternion rotation = HelperFunctions.allocQuaternion();
                                            headBone.getRotation(rotation);
                                            HelperFunctions.ToEulerAngles(rotation, templwjglVector3f);
                                            float headAngleNormalizedY = -templwjglVector3f.y / this.getHeadLookAngleMax();
                                            this.setHeadLookAroundDirection(headAngleNormalizedX, 0.0F);
                                            HelperFunctions.releaseQuaternion(rotation);
                                        }
                                    }

                                    if (this.isAiming() && !this.isNpc && !this.isCurrentState(FishingState.instance()) && !this.isFarming()) {
                                        if (this.joypadBind != -1 && !this.joypadMovementActive) {
                                            if (this.getForwardDirection().getLengthSquared() > 0.0F) {
                                                this.DirectionFromVector(this.getForwardDirection());
                                            }
                                        } else {
                                            Vector2 vec = tempVector2.set(0.0F, 0.0F);
                                            if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
                                                this.getControllerAimDir(vec);
                                            } else {
                                                this.getMouseAimVector(vec);
                                            }

                                            if (vec.getLengthSquared() > 0.0F) {
                                                this.DirectionFromVector(vec);
                                                this.setForwardDirection(vec);
                                            }
                                        }

                                        moveVars.newFacing = this.dir;
                                    }

                                    if (this.getForwardDirectionX() == 0.0F && this.getForwardDirectionY() == 0.0F) {
                                        this.setForwardDirection(this.dir.ToVector());
                                    }

                                    if (this.lastAngle.x != this.getForwardDirectionX() || this.lastAngle.y != this.getForwardDirectionY()) {
                                        this.lastAngle.x = this.getForwardDirectionX();
                                        this.lastAngle.y = this.getForwardDirectionY();
                                        this.dirtyRecalcGridStackTime = 2.0F;
                                    }

                                    AnimationPlayer animPlayer = this.getAnimationPlayer();
                                    if (animPlayer != null && animPlayer.isReady()) {
                                        float angle = animPlayer.getAngle() + animPlayer.getTwistAngle();
                                        this.dir = IsoDirections.fromAngle(tempVector2.setLengthAndDirection(angle, 1.0F));
                                    } else if (!this.falling && !this.isAiming() && !isAttacking) {
                                        this.dir = moveVars.newFacing;
                                    }

                                    if (this.isAiming() && (GameWindow.activatedJoyPad == null || this.joypadBind == -1)) {
                                        this.playerMoveDir.x = moveVars.moveX;
                                        this.playerMoveDir.y = moveVars.moveY;
                                    }

                                    if (!this.isAiming() && this.isJustMoved()) {
                                        this.playerMoveDir.x = this.getForwardDirectionX();
                                        this.playerMoveDir.y = this.getForwardDirectionY();
                                    }

                                    if (this.isJustMoved()) {
                                        if (this.isSprinting()) {
                                            this.currentSpeed = 1.5F;
                                        } else if (this.isRunning()) {
                                            this.currentSpeed = 1.0F;
                                        } else {
                                            this.currentSpeed = 0.5F;
                                        }
                                    } else {
                                        this.currentSpeed = 0.0F;
                                    }

                                    boolean overrideAnimation = this.IsInMeleeAttack();
                                    if (!this.characterActions.isEmpty()) {
                                        BaseAction action = this.characterActions.get(0);
                                        if (action.overrideAnimation) {
                                            overrideAnimation = true;
                                        }
                                    }

                                    if (!overrideAnimation && !this.isForceOverrideAnim()) {
                                        if (this.getPath2() == null) {
                                            if (this.currentSpeed > 0.0F && (!this.climbing || this.lastFallSpeed > 0.0F)) {
                                                if (!this.isRunning() && !this.isSprinting()) {
                                                    this.StopAllActionQueueWalking();
                                                } else {
                                                    this.StopAllActionQueueRunning();
                                                }
                                            }
                                        } else {
                                            this.StopAllActionQueueWalking();
                                        }
                                    }

                                    if (this.slowTimer > 0.0F) {
                                        this.slowTimer = this.slowTimer - GameTime.instance.getRealworldSecondsSinceLastUpdate();
                                        this.currentSpeed = this.currentSpeed * (1.0F - this.slowFactor);
                                        this.slowFactor = this.slowFactor - GameTime.instance.getMultiplier() / 100.0F;
                                        if (this.slowFactor < 0.0F) {
                                            this.slowFactor = 0.0F;
                                        }
                                    } else {
                                        this.slowFactor = 0.0F;
                                    }

                                    this.playerMoveDir.setLength(this.currentSpeed);
                                    if (this.playerMoveDir.x != 0.0F || this.playerMoveDir.y != 0.0F) {
                                        this.dirtyRecalcGridStackTime = 10.0F;
                                    }

                                    if (this.getPath2() != null && this.current != this.last) {
                                        this.dirtyRecalcGridStackTime = 10.0F;
                                    }

                                    this.closestZombie = 1000000.0F;
                                    this.weight = 0.3F;
                                    this.separate();
                                    if (!this.isAnimal()) {
                                        this.updateSleepingPillsTaken();
                                        this.updateTorchStrength();
                                    }

                                    if (this.isNpc) {
                                        this.ai.postUpdate();
                                        strafeX = this.ai.brain.humanControlVars.strafeX;
                                        strafeY = this.ai.brain.humanControlVars.strafeY;
                                    }

                                    this.isPlayerMoving = this.isJustMoved() || this.hasPath() && this.getPathFindBehavior2().shouldBeMoving();
                                    boolean inTrees = this.isInTrees();
                                    if (inTrees) {
                                        float speed = this.getDescriptor().isCharacterProfession(CharacterProfession.PARK_RANGER) ? 1.3F : 1.0F;
                                        speed = this.getDescriptor().isCharacterProfession(CharacterProfession.LUMBERJACK) ? 1.15F : speed;
                                        if (this.isRunning()) {
                                            speed *= 1.1F;
                                        }

                                        this.setVariable("WalkSpeedTrees", speed);
                                    }

                                    IsoObject collapsedFence = BentFences.getInstance().getCollapsedFence(this.square);
                                    if ((inTrees || collapsedFence != null || this.walkSpeed < 0.4F || this.walkInjury > 0.5F)
                                        && this.isSprinting()
                                        && !this.isGhostMode()) {
                                        if (this.runSpeedModifier < 1.0F) {
                                            this.setMoodleCantSprint(true);
                                        }

                                        this.setSprinting(false);
                                        this.setRunning(true);
                                        if (this.isInTreesNoBush() || collapsedFence != null) {
                                            this.setForceSprint(false);
                                            this.setBumpType("left");
                                            this.setVariable("BumpDone", false);
                                            this.setVariable("BumpFall", true);
                                            this.setVariable("TripObstacleType", "tree");
                                            this.getActionContext().reportEvent("wasBumped");
                                        }
                                    }

                                    this.deltaX = strafeX;
                                    this.deltaY = strafeY;
                                    this.windspeed = ClimateManager.getInstance().getWindSpeedMovement();
                                    float angle = this.getForwardDirection().getDirectionNeg();
                                    this.windForce = ClimateManager.getInstance().getWindForceMovement(this, angle);
                                    return true;
                                } else {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void handleLandingImpact(FallDamage in_fallDamage) {
        boolean isDamagingFall = in_fallDamage.isDamagingFall();
        boolean isMoreThanLightFall = in_fallDamage.isMoreThanLightFall();
        if (isDamagingFall) {
            this.stopPlayerVoiceSound("DeathFall");
        }

        if (!GameClient.client) {
            if (isMoreThanLightFall) {
                this.stats.reset(CharacterStat.BOREDOM);
            }

            super.handleLandingImpact(in_fallDamage);
        }
    }

    @Override
    protected void playPainVoicesFromFallDamage(FallDamage in_fallDamage) {
        boolean isMoreThanLightFall = in_fallDamage.isMoreThanLightFall();
        this.playerVoiceSound(isMoreThanLightFall ? "PainFromFallHigh" : " PainFromFallLow");
    }

    private void setDoGrappleLetGoAfterContextKeyIsReleased(boolean in_letGoAfterContextIsReleased) {
        this.letGoAfterContextIsReleased = in_letGoAfterContextIsReleased;
    }

    private boolean isDoGrappleLetGoAfterContextKeyIsReleased() {
        return this.letGoAfterContextIsReleased;
    }

    private void updateMovementFromInput(IsoPlayer.MoveVars moveVars) {
        moveVars.moveX = 0.0F;
        moveVars.moveY = 0.0F;
        moveVars.strafeX = 0.0F;
        moveVars.strafeY = 0.0F;
        moveVars.newFacing = this.dir;
        if (!TutorialManager.instance.stealControl) {
            if (!this.isBlockMovement()) {
                if (!this.isPickingUpBody() && !this.isPuttingDownBody()) {
                    if (!this.isNpc) {
                        if (!MPDebugAI.updateMovementFromInput(this, moveVars)) {
                            if (!(this.fallTime > 2.0F)) {
                                if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
                                    this.updateMovementFromJoypad(moveVars);
                                }

                                if (this.playerIndex == 0 && this.joypadBind == -1) {
                                    this.updateMovementFromKeyboardMouse(moveVars);
                                }

                                if (this.isJustMoved()) {
                                    this.getForwardDirection().normalize();
                                    UIManager.speedControls.SetCurrentGameSpeed(1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void randomizeDrunkenMovement(IsoPlayer.MoveVars moveVars, boolean isController) {
        int mod = Rand.NextBool(20) ? 2 : 1;
        int diff = 0;
        if (this.drunkMoveVars.newFacing == null) {
            this.drunkMoveVars.newFacing = moveVars.newFacing;
        }

        diff = (moveVars.newFacing.index() - this.drunkMoveVars.newFacing.index() + 4) % 8 - 4;
        diff = diff < -4 ? diff + 4 : diff;
        boolean clockwise = false;
        if (Math.abs(diff) >= 2) {
            if (diff < 0) {
                clockwise = true;
            }
        } else if (Rand.NextBool(2)) {
            clockwise = true;
        }

        if (clockwise) {
            moveVars.newFacing = this.drunkMoveVars.newFacing.RotRight(mod);
        } else {
            moveVars.newFacing = this.drunkMoveVars.newFacing.RotLeft(mod);
        }

        if (isController) {
            switch (moveVars.newFacing) {
                case N:
                    moveVars.moveX = 1.0F;
                    moveVars.moveY = -1.0F;
                    break;
                case NW:
                    moveVars.moveX = 0.0F;
                    moveVars.moveY = -1.0F;
                    break;
                case W:
                    moveVars.moveX = -1.0F;
                    moveVars.moveY = -1.0F;
                    break;
                case SW:
                    moveVars.moveX = -1.0F;
                    moveVars.moveY = 0.0F;
                    break;
                case S:
                    moveVars.moveX = -1.0F;
                    moveVars.moveY = 1.0F;
                    break;
                case SE:
                    moveVars.moveX = 0.0F;
                    moveVars.moveY = 1.0F;
                    break;
                case E:
                    moveVars.moveX = 1.0F;
                    moveVars.moveY = 1.0F;
                    break;
                case NE:
                    moveVars.moveX = 1.0F;
                    moveVars.moveY = 0.0F;
            }
        } else {
            switch (moveVars.newFacing) {
                case N:
                    moveVars.moveX = 0.0F;
                    moveVars.moveY = -1.0F;
                    break;
                case NW:
                    moveVars.moveX = -1.0F;
                    moveVars.moveY = -1.0F;
                    break;
                case W:
                    moveVars.moveX = -1.0F;
                    moveVars.moveY = 0.0F;
                    break;
                case SW:
                    moveVars.moveX = -1.0F;
                    moveVars.moveY = 1.0F;
                    break;
                case S:
                    moveVars.moveX = 0.0F;
                    moveVars.moveY = 1.0F;
                    break;
                case SE:
                    moveVars.moveX = 1.0F;
                    moveVars.moveY = 1.0F;
                    break;
                case E:
                    moveVars.moveX = 1.0F;
                    moveVars.moveY = 0.0F;
                    break;
                case NE:
                    moveVars.moveX = 1.0F;
                    moveVars.moveY = -1.0F;
            }
        }
    }

    private void adjustMovementForDrunks(IsoPlayer.MoveVars moveVars, boolean isController) {
        if (this.getMoodles().getMoodleLevel(MoodleType.DRUNK) > 1) {
            this.drunkDelayCommandTimer = this.drunkDelayCommandTimer + GameTime.getInstance().getMultiplier();
            int chance = Rand.AdjustForFramerate(4 * this.getMoodles().getMoodleLevel(MoodleType.DRUNK));
            if (chance > this.drunkDelayCommandTimer) {
                moveVars.moveY = this.drunkMoveVars.moveY;
                moveVars.moveX = this.drunkMoveVars.moveX;
                moveVars.newFacing = this.drunkMoveVars.newFacing;
            } else {
                this.drunkDelayCommandTimer = 0.0F;
                if ((moveVars.moveX != 0.0F || moveVars.moveY != 0.0F) && Rand.Next(160) < this.getStats().get(CharacterStat.INTOXICATION)) {
                    this.randomizeDrunkenMovement(moveVars, isController);
                }

                if ((this.isSprinting() || this.isRunning())
                    && Rand.Next(2000) < this.getStats().get(CharacterStat.INTOXICATION) * this.getMoodles().getMoodleLevel(MoodleType.DRUNK)) {
                    this.setVariable("BumpDone", false);
                    this.clearVariable("BumpFallType");
                    this.setBumpType("trippingFromSprint");
                    this.setBumpFall(true);
                    this.setBumpFallType(Rand.NextBool(5) ? "pushedFront" : "pushedBehind");
                    this.getActionContext().reportEvent("wasBumped");
                }
            }
        } else {
            this.drunkDelayCommandTimer = 0.0F;
        }

        this.drunkMoveVars.moveY = moveVars.moveY;
        this.drunkMoveVars.moveX = moveVars.moveX;
        this.drunkMoveVars.newFacing = moveVars.newFacing;
    }

    private void updateMovementFromJoypad(IsoPlayer.MoveVars moveVars) {
        this.playerMoveDir.x = 0.0F;
        this.playerMoveDir.y = 0.0F;
        this.getJoypadAimVector(tempVector2);
        float aimX = tempVector2.x;
        float aimY = tempVector2.y;
        Vector2 moveVec = this.getJoypadMoveVector(tempVector2);
        if (moveVec.getLength() > 1.0F) {
            moveVec.setLength(1.0F);
        }

        if (this.isAutoWalk()) {
            if (moveVec.getLengthSquared() < 0.25F) {
                moveVec.set(this.getAutoWalkDirection());
            } else {
                this.setAutoWalkDirection(moveVec);
                this.getAutoWalkDirection().normalize();
            }
        }

        float moveX = moveVec.x;
        float moveY = moveVec.y;
        if (Math.abs(moveX) > 0.0F) {
            this.playerMoveDir.x += 0.04F * moveX;
            this.playerMoveDir.y -= 0.04F * moveX;
            this.setJustMoved(true);
        }

        if (Math.abs(moveY) > 0.0F) {
            this.playerMoveDir.y += 0.04F * moveY;
            this.playerMoveDir.x += 0.04F * moveY;
            this.setJustMoved(true);
        }

        this.playerMoveDir.setLength(0.05F * (float)Math.pow(moveVec.getLength(), 9.0));
        if (aimX != 0.0F || aimY != 0.0F) {
            Vector2 aimVec = tempVector2.set(aimX, aimY);
            aimVec.normalize();
            moveVars.newFacing = IsoDirections.fromAngle(aimVec);
        } else if ((moveX != 0.0F || moveY != 0.0F) && this.playerMoveDir.getLengthSquared() > 0.0F) {
            moveVec = tempVector2.set(this.playerMoveDir);
            moveVec.normalize();
            moveVars.newFacing = IsoDirections.fromAngle(moveVec);
        }

        moveVars.moveX = this.playerMoveDir.x;
        moveVars.moveY = this.playerMoveDir.y;
        this.playerMoveDir.x = moveVars.moveX;
        this.playerMoveDir.y = moveVars.moveY;
        PathFindBehavior2 pfb2 = this.getPathFindBehavior2();
        if (this.playerMoveDir.x == 0.0F && this.playerMoveDir.y == 0.0F && this.getPath2() != null && pfb2.isStrafing() && !pfb2.stopping) {
            this.playerMoveDir.set(pfb2.getTargetX() - this.getX(), pfb2.getTargetY() - this.getY());
            this.playerMoveDir.normalize();
        }

        if (this.playerMoveDir.x != 0.0F || this.playerMoveDir.y != 0.0F) {
            if (this.isStrafing()) {
                tempo.set(this.playerMoveDir.x, -this.playerMoveDir.y);
                tempo.normalize();
                float angle = this.legsSprite.modelSlot.model.animPlayer.getRenderedAngle();
                if (angle > Math.PI * 2) {
                    angle -= (float) (Math.PI * 2);
                }

                if (angle < 0.0F) {
                    angle += (float) (Math.PI * 2);
                }

                tempo.rotate(angle);
                moveVars.strafeX = tempo.x;
                moveVars.strafeY = tempo.y;
                this.ipX = this.playerMoveDir.x;
                this.ipY = this.playerMoveDir.y;
            } else {
                moveVars.moveX = this.playerMoveDir.x;
                moveVars.moveY = this.playerMoveDir.y;
                tempo.set(this.playerMoveDir);
                tempo.normalize();
                this.setForwardDirection(tempo);
            }
        }
    }

    private void updateMovementFromKeyboardMouse(IsoPlayer.MoveVars moveVars) {
        int keyLeft = GameKeyboard.whichKeyDown("Left");
        int keyRight = GameKeyboard.whichKeyDown("Right");
        int keyForward = GameKeyboard.whichKeyDown("Forward");
        int keyBackward = GameKeyboard.whichKeyDown("Backward");
        boolean isKeyDownLeft = keyLeft > 0;
        boolean isKeyDownRight = keyRight > 0;
        boolean isKeyDownForward = keyForward > 0;
        boolean isKeyDownBackward = keyBackward > 0;
        if (!isKeyDownLeft && !isKeyDownRight && !isKeyDownForward && !isKeyDownBackward
            || keyLeft != 30 && keyRight != 30 && keyForward != 30 && keyBackward != 30
            || !GameKeyboard.isKeyDown(29) && !GameKeyboard.isKeyDown(157)
            || !UIManager.isMouseOverInventory()
            || !Core.getInstance().isSelectingAll()) {
            if (!this.isIgnoreInputsForDirection()) {
                if (Core.altMoveMethod) {
                    if (isKeyDownLeft && !isKeyDownRight) {
                        moveVars.moveX -= 0.04F;
                        moveVars.newFacing = IsoDirections.W;
                    }

                    if (isKeyDownRight && !isKeyDownLeft) {
                        moveVars.moveX += 0.04F;
                        moveVars.newFacing = IsoDirections.E;
                    }

                    if (isKeyDownForward && !isKeyDownBackward) {
                        moveVars.moveY -= 0.04F;
                        if (moveVars.newFacing == IsoDirections.W) {
                            moveVars.newFacing = IsoDirections.NW;
                        } else if (moveVars.newFacing == IsoDirections.E) {
                            moveVars.newFacing = IsoDirections.NE;
                        } else {
                            moveVars.newFacing = IsoDirections.N;
                        }
                    }

                    if (isKeyDownBackward && !isKeyDownForward) {
                        moveVars.moveY += 0.04F;
                        if (moveVars.newFacing == IsoDirections.W) {
                            moveVars.newFacing = IsoDirections.SW;
                        } else if (moveVars.newFacing == IsoDirections.E) {
                            moveVars.newFacing = IsoDirections.SE;
                        } else {
                            moveVars.newFacing = IsoDirections.S;
                        }
                    }
                } else {
                    if (isKeyDownLeft) {
                        moveVars.moveX = -1.0F;
                    } else if (isKeyDownRight) {
                        moveVars.moveX = 1.0F;
                    }

                    if (isKeyDownForward) {
                        moveVars.moveY = 1.0F;
                    } else if (isKeyDownBackward) {
                        moveVars.moveY = -1.0F;
                    }

                    if (moveVars.moveX != 0.0F || moveVars.moveY != 0.0F) {
                        tempo.set(moveVars.moveX, moveVars.moveY);
                        tempo.normalize();
                        moveVars.newFacing = IsoDirections.fromAngle(tempo);
                    }

                    if (this.isAnimatingBackwards()) {
                        moveVars.moveX = -moveVars.moveX;
                        moveVars.moveY = -moveVars.moveY;
                    }
                }
            }

            PathFindBehavior2 pfb2 = this.getPathFindBehavior2();
            if (moveVars.moveX == 0.0F && moveVars.moveY == 0.0F && this.getPath2() != null && (pfb2.isStrafing() || this.isAiming()) && !pfb2.stopping) {
                Vector2 v = tempo.set(pfb2.getTargetX() - this.getX(), pfb2.getTargetY() - this.getY());
                Vector2 u = tempo2.set(-1.0F, 0.0F);
                float uLen = 1.0F;
                float vDotU = v.dot(u);
                float forwardComponent = vDotU / 1.0F;
                u = tempo2.set(0.0F, -1.0F);
                vDotU = v.dot(u);
                float rightComponent = vDotU / 1.0F;
                tempo.set(rightComponent, forwardComponent);
                tempo.normalize();
                tempo.rotate((float) (Math.PI / 4));
                moveVars.moveX = tempo.x;
                moveVars.moveY = tempo.y;
            }

            if (moveVars.moveX != 0.0F || moveVars.moveY != 0.0F) {
                if (this.stateMachine.getCurrent() == PathFindState.instance()) {
                    this.setDefaultState();
                }

                this.setJustMoved(true);
                this.setMoveDelta(1.0F);
                if (this.isStrafing()) {
                    tempo.set(moveVars.moveX, moveVars.moveY);
                    tempo.normalize();
                    float angle = this.legsSprite.modelSlot.model.animPlayer.getRenderedAngle();
                    angle += (float) (Math.PI / 4);
                    if (angle > Math.PI * 2) {
                        angle -= (float) (Math.PI * 2);
                    }

                    if (angle < 0.0F) {
                        angle += (float) (Math.PI * 2);
                    }

                    tempo.rotate(angle);
                    moveVars.strafeX = tempo.x;
                    moveVars.strafeY = tempo.y;
                    this.ipX = moveVars.moveX;
                    this.ipY = moveVars.moveY;
                } else {
                    tempo.set(moveVars.moveX, -moveVars.moveY);
                    tempo.normalize();
                    tempo.rotate((float) (-Math.PI / 4));
                    this.setForwardDirection(tempo);
                }
            }
        }
    }

    private void updateAimingStance() {
        if (this.isVariable("LeftHandMask", "RaiseHand")) {
            this.clearVariable("LeftHandMask");
        }

        if (this.isAiming() && !this.isCurrentState(SwipeStatePlayer.instance())) {
            HandWeapon weapon = Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
            weapon = weapon == null ? this.bareHands : weapon;
            if (weapon.hasComponent(ComponentType.FluidContainer)
                && weapon.getFluidContainer().getRainCatcher() > 0.0F
                && !weapon.getFluidContainer().isEmpty()) {
                weapon.getFluidContainer().Empty();
            }

            CombatManager.getInstance().calcValidTargets(this, weapon, s_targetsProne, s_targetsStanding);
            HitInfo bestStanding = s_targetsStanding.isEmpty() ? null : s_targetsStanding.get(0);
            HitInfo bestProne = s_targetsProne.isEmpty() ? null : s_targetsProne.get(0);
            if (CombatManager.getInstance().isProneTargetBetter(this, bestStanding, bestProne)) {
                bestStanding = null;
            }

            boolean bAttacking = this.isPerformingHostileAnimation();
            if (!bAttacking) {
                this.setAimAtFloor(false);
            }

            if (bestStanding != null) {
                if (!bAttacking) {
                    this.setAimAtFloor(false);
                }
            } else if (bestProne != null && !bAttacking) {
                this.setAimAtFloor(true);
            }

            if (bestStanding != null) {
                boolean RaiseHand = !this.isPerformingAttackAnimation()
                    && weapon.getSwingAnim() != null
                    && weapon.closeKillMove != null
                    && bestStanding.distSq < weapon.getMinRange() * weapon.getMinRange();
                if (RaiseHand && (this.getSecondaryHandItem() == null || this.getSecondaryHandItem().getItemReplacementSecondHand() == null)) {
                    this.setVariable("LeftHandMask", "RaiseHand");
                }
            }

            CombatManager.getInstance().hitInfoPool.release(s_targetsStanding);
            CombatManager.getInstance().hitInfoPool.release(s_targetsProne);
            s_targetsStanding.clear();
            s_targetsProne.clear();
        }
    }

    @Override
    protected void calculateStats() {
        if (!GameClient.client) {
            super.calculateStats();
        }
    }

    @Override
    protected void updateStats_Sleeping() {
        float endMult = 2.0F;
        if (allPlayersAsleep()) {
            endMult *= GameTime.instance.getDeltaMinutesPerDay();
        }

        this.stats
            .add(
                CharacterStat.ENDURANCE,
                (float)(
                    ZomboidGlobals.imobileEnduranceReduce
                        * SandboxOptions.instance.getEnduranceRegenMultiplier()
                        * this.getRecoveryMod()
                        * GameTime.instance.getMultiplier()
                        * endMult
                )
            );
        if (this.stats.isAboveMinimum(CharacterStat.FATIGUE)) {
            float mul = 1.0F;
            if (this.characterTraits.get(CharacterTrait.INSOMNIAC)) {
                mul *= 0.5F;
            }

            if (this.characterTraits.get(CharacterTrait.NIGHT_OWL)) {
                mul *= 1.4F;
            }

            float bedQualityMultiplier = 1.0F;
            if ("averageBedPillow".equals(this.getBedType())) {
                bedQualityMultiplier = 1.05F;
            }

            if ("goodBed".equals(this.getBedType())) {
                bedQualityMultiplier = 1.1F;
            } else if ("goodBedPillow".equals(this.getBedType())) {
                bedQualityMultiplier = 1.15F;
            } else if ("badBed".equals(this.getBedType())) {
                bedQualityMultiplier = 0.9F;
            } else if ("badBedPillow".equals(this.getBedType())) {
                bedQualityMultiplier = 0.95F;
            } else if ("floor".equals(this.getBedType())) {
                bedQualityMultiplier = 0.6F;
            } else if ("floorPillow".equals(this.getBedType())) {
                bedQualityMultiplier = 0.75F;
            }

            float elapsedHours = 1.0F / GameTime.instance.getMinutesPerDay() / 60.0F * GameTime.instance.getMultiplier() / 2.0F;
            this.timeOfSleep += elapsedHours;
            if (this.timeOfSleep > this.delayToActuallySleep) {
                float traitMul = 1.0F;
                if (this.characterTraits.get(CharacterTrait.NEEDS_LESS_SLEEP)) {
                    traitMul *= 0.75F;
                } else if (this.characterTraits.get(CharacterTrait.NEEDS_MORE_SLEEP)) {
                    traitMul *= 1.18F;
                }

                float hoursNeeded = 1.0F;
                if (this.stats.get(CharacterStat.FATIGUE) <= 0.3F) {
                    hoursNeeded = 7.0F * traitMul;
                    this.stats.remove(CharacterStat.FATIGUE, elapsedHours / hoursNeeded * 0.3F * mul * bedQualityMultiplier);
                } else {
                    hoursNeeded = 5.0F * traitMul;
                    this.stats.remove(CharacterStat.FATIGUE, elapsedHours / hoursNeeded * 0.7F * mul * bedQualityMultiplier);
                }
            }
        }

        if (this.moodles.getMoodleLevel(MoodleType.FOOD_EATEN) == 0) {
            float hungerMult = this.getAppetiteMultiplier();
            this.stats
                .add(
                    CharacterStat.HUNGER,
                    (float)(
                        ZomboidGlobals.hungerIncreaseWhileAsleep
                            * SandboxOptions.instance.getStatsDecreaseMultiplier()
                            * hungerMult
                            * GameTime.instance.getMultiplier()
                            * GameTime.instance.getDeltaMinutesPerDay()
                            * this.getHungerMultiplier()
                    )
                );
        } else {
            this.stats
                .add(
                    CharacterStat.HUNGER,
                    (float)(
                        ZomboidGlobals.hungerIncreaseWhenWellFed
                            * SandboxOptions.instance.getStatsDecreaseMultiplier()
                            * ZomboidGlobals.hungerIncreaseWhileAsleep
                            * SandboxOptions.instance.getStatsDecreaseMultiplier()
                            * GameTime.instance.getMultiplier()
                            * this.getHungerMultiplier()
                            * GameTime.instance.getDeltaMinutesPerDay()
                    )
                );
        }
    }

    public void processWakingUp() {
        if (this.forceWakeUpTime == 0.0F) {
            this.forceWakeUpTime = 9.0F;
        }

        float timeOfDay = GameTime.getInstance().getTimeOfDay();
        float lastTimeOfDay = GameTime.getInstance().getLastTimeOfDay();
        if (lastTimeOfDay > timeOfDay) {
            if (lastTimeOfDay < this.forceWakeUpTime) {
                timeOfDay += 24.0F;
            } else {
                lastTimeOfDay -= 24.0F;
            }
        }

        boolean forceWakeUp = timeOfDay >= this.forceWakeUpTime && lastTimeOfDay < this.forceWakeUpTime;
        if (this.getAsleepTime() > 16.0F) {
            forceWakeUp = true;
        }

        if (GameClient.client || numPlayers > 1) {
            forceWakeUp = forceWakeUp || this.pressedAim() || this.pressedMovement(false);
        }

        if (this.forceWakeUp) {
            forceWakeUp = true;
        }

        if (this.asleep && forceWakeUp) {
            this.forceWakeUp = false;
            SoundManager.instance.setMusicWakeState(this, "WakeNormal");
            SleepingEvent.instance.wakeUp(this);
            this.forceWakeUpTime = -1.0F;
            if (GameClient.client) {
                GameClient.instance.sendPlayer(this);
            }

            this.dirtyRecalcGridStackTime = 20.0F;
        }
    }

    public void updateEnduranceWhileSitting() {
        float mul = (float)ZomboidGlobals.sittingEnduranceMultiplier;
        mul *= 1.0F - this.stats.get(CharacterStat.FATIGUE) * 0.8F;
        mul *= GameTime.instance.getMultiplier();
        this.stats
            .add(
                CharacterStat.ENDURANCE,
                (float)(ZomboidGlobals.imobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * this.getRecoveryMod() * mul)
            );
    }

    public void updateEnduranceWhileInVehicle() {
        if (!this.isAnimal() && !GameClient.client) {
            if (!this.asleep) {
                float mul = (float)ZomboidGlobals.sittingEnduranceMultiplier;
                mul *= 1.0F - this.stats.get(CharacterStat.FATIGUE) * 0.8F;
                mul *= GameTime.instance.getMultiplier();
                this.stats
                    .add(
                        CharacterStat.ENDURANCE,
                        (float)(ZomboidGlobals.imobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * this.getRecoveryMod() * mul)
                    );
            }
        }
    }

    private void updateEndurance() {
        if (!this.isAnimal() && !GameClient.client) {
            if (!this.isSitOnGround() && !this.isSittingOnFurniture() && !this.isResting()) {
                float sneakMultiplier = 1.0F;
                if (this.isSneaking()) {
                    sneakMultiplier = 1.5F;
                }

                if (this.currentSpeed > 0.0F && (this.isRunning() || this.isSprinting() || this.isDraggingCorpse())) {
                    double enduranceReduce = ZomboidGlobals.runningEnduranceReduce;
                    if (this.isSprinting()) {
                        enduranceReduce = ZomboidGlobals.sprintingEnduranceReduce;
                    }

                    if (this.isDraggingCorpse()) {
                        enduranceReduce = ZomboidGlobals.sprintingEnduranceReduce;
                    }

                    float enddelta = 1.4F;
                    if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                        enddelta = 2.9F;
                    }

                    if (this.characterTraits.get(CharacterTrait.ATHLETIC)) {
                        enddelta = 0.8F;
                    }

                    enddelta *= 2.3F;
                    enddelta *= this.getPacingMod();
                    enddelta *= this.getHyperthermiaMod();
                    float mod = 0.7F;
                    if (this.characterTraits.get(CharacterTrait.ASTHMATIC)) {
                        mod = 1.0F;
                    }

                    if (this.moodles.getMoodleLevel(MoodleType.HEAVY_LOAD) == 0) {
                        this.stats
                            .remove(
                                CharacterStat.ENDURANCE, (float)(enduranceReduce * enddelta * 0.5 * mod * GameTime.instance.getMultiplier() * sneakMultiplier)
                            );
                    } else {
                        float weightDelta = switch (this.moodles.getMoodleLevel(MoodleType.HEAVY_LOAD)) {
                            case 1 -> 1.5F;
                            case 2 -> 1.9F;
                            case 3 -> 2.3F;
                            default -> 2.8F;
                        };
                        this.stats
                            .remove(
                                CharacterStat.ENDURANCE,
                                (float)(enduranceReduce * enddelta * 0.5 * mod * GameTime.instance.getMultiplier() * weightDelta * sneakMultiplier)
                            );
                    }
                } else if (this.currentSpeed > 0.0F && this.moodles.getMoodleLevel(MoodleType.HEAVY_LOAD) > 2) {
                    float modx = 0.7F;
                    if (this.characterTraits.get(CharacterTrait.ASTHMATIC)) {
                        modx = 1.0F;
                    }

                    float enddeltax = 1.4F;
                    if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                        enddeltax = 2.9F;
                    }

                    if (this.characterTraits.get(CharacterTrait.ATHLETIC)) {
                        enddeltax = 0.8F;
                    }

                    enddeltax *= 3.0F;
                    enddeltax *= this.getPacingMod();
                    enddeltax *= this.getHyperthermiaMod();
                    float weightDelta = 2.8F;
                    switch (this.moodles.getMoodleLevel(MoodleType.HEAVY_LOAD)) {
                        case 2:
                            weightDelta = 1.5F;
                            break;
                        case 3:
                            weightDelta = 1.9F;
                            break;
                        case 4:
                            weightDelta = 2.3F;
                    }

                    this.stats
                        .remove(
                            CharacterStat.ENDURANCE,
                            (float)(
                                ZomboidGlobals.runningEnduranceReduce
                                    * enddeltax
                                    * 0.5
                                    * modx
                                    * sneakMultiplier
                                    * GameTime.instance.getMultiplier()
                                    * weightDelta
                                    / 2.0
                            )
                        );
                }

                if (!this.isPlayerMoving()) {
                    float mul = 1.0F;
                    mul *= 1.0F - this.stats.get(CharacterStat.FATIGUE) * 0.85F;
                    mul *= GameTime.instance.getMultiplier();
                    if (this.moodles.getMoodleLevel(MoodleType.HEAVY_LOAD) <= 1) {
                        this.stats
                            .add(
                                CharacterStat.ENDURANCE,
                                (float)(
                                    ZomboidGlobals.imobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * this.getRecoveryMod() * mul
                                )
                            );
                    }
                }

                if (!this.isSprinting() && !this.isRunning() && this.currentSpeed > 0.0F) {
                    float mul = 1.0F;
                    mul *= 1.0F - this.stats.get(CharacterStat.FATIGUE);
                    mul *= GameTime.instance.getMultiplier();
                    if (this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) < 2) {
                        if (this.moodles.getMoodleLevel(MoodleType.HEAVY_LOAD) <= 1) {
                            this.stats
                                .add(
                                    CharacterStat.ENDURANCE,
                                    (float)(
                                        ZomboidGlobals.imobileEnduranceReduce
                                            / 4.0
                                            * SandboxOptions.instance.getEnduranceRegenMultiplier()
                                            * this.getRecoveryMod()
                                            * mul
                                    )
                                );
                        }
                    } else {
                        this.stats.remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce / 7.0 * sneakMultiplier));
                    }
                }
            } else {
                this.updateEnduranceWhileSitting();
            }
        }
    }

    private boolean checkActionsBlockingMovement() {
        if (this.characterActions.isEmpty()) {
            return false;
        } else {
            BaseAction action = this.characterActions.get(0);
            return action.blockMovementEtc;
        }
    }

    private void updateInteractKeyPanic() {
        if (this.playerIndex == 0) {
            if (GameKeyboard.isKeyPressed("Interact")) {
                this.contextPanic += 0.6F;
            }
        }
    }

    private void updateSneakKey() {
        if (this.playerIndex != 0) {
            this.sneakDebounce = false;
        } else {
            if (this.isBlockMovement() || !GameKeyboard.isKeyDown("Crouch")) {
                this.sneakDebounce = false;
            } else if (!this.sneakDebounce) {
                this.setSneaking(!this.isSneaking());
                this.sneakDebounce = true;
            }
        }
    }

    private void updateChangeCharacterKey() {
        if (Core.debug) {
            if (this.playerIndex == 0 && GameKeyboard.isKeyDown(22)) {
                if (!this.changeCharacterDebounce) {
                    this.followCamStack.clear();
                    this.changeCharacterDebounce = true;

                    for (int n = 0; n < this.getCell().getObjectList().size(); n++) {
                        IsoMovingObject obj = this.getCell().getObjectList().get(n);
                        if (obj instanceof IsoSurvivor isoSurvivor) {
                            this.followCamStack.add(isoSurvivor);
                        }
                    }

                    if (!this.followCamStack.isEmpty()) {
                        if (this.followId >= this.followCamStack.size()) {
                            this.followId = 0;
                        }

                        IsoCamera.SetCharacterToFollow(this.followCamStack.get(this.followId));
                        this.followId++;
                    }
                }
            } else {
                this.changeCharacterDebounce = false;
            }
        }
    }

    private void updateEnableModelsKey() {
        if (Core.debug) {
            if (this.playerIndex == 0 && GameKeyboard.isKeyPressed("ToggleModelsEnabled")) {
                ModelManager.instance.debugEnableModels = !ModelManager.instance.debugEnableModels;
            }
        }
    }

    private void updateDeathDragDown() {
        if (!this.isDead()) {
            if (this.isDeathDragDown()) {
                if (this.isGodMod()) {
                    this.setDeathDragDown(false);
                } else if (!"EndDeath".equals(this.getHitReaction())) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            IsoGridSquare square = this.getCell()
                                .getGridSquare(PZMath.fastfloor(this.getX()) + dx, PZMath.fastfloor(this.getY()) + dy, PZMath.fastfloor(this.getZ()));
                            if (square != null) {
                                for (int i = 0; i < square.getMovingObjects().size(); i++) {
                                    IsoMovingObject mo = square.getMovingObjects().get(i);
                                    IsoZombie zombie = Type.tryCastTo(mo, IsoZombie.class);
                                    if (zombie != null && zombie.isAlive() && !zombie.isOnFloor()) {
                                        this.setAttackedBy(zombie);
                                        this.setHitReaction("EndDeath");
                                        this.setBlockMovement(true);
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    this.setDeathDragDown(false);
                    if (GameClient.client) {
                        DebugLog.DetailedInfo.warn("UpdateDeathDragDown: no zombies found around player \"%s\"", this.getUsername());
                        this.setHitFromBehind(false);
                        this.Kill(null);
                    }
                }
            }
        }
    }

    private void updateGodModeKey() {
        if (Core.debug) {
            if (GameKeyboard.isKeyPressed("ToggleGodModeInvisible")) {
                IsoPlayer player = null;

                for (int n = 0; n < numPlayers; n++) {
                    if (players[n] != null && !players[n].isDead()) {
                        player = players[n];
                        break;
                    }
                }

                if (this == player) {
                    boolean godMode = !player.isGodMod();
                    DebugLog.General.println("Toggle GodMode: %s", godMode ? "ON" : "OFF");
                    player.setInvisible(godMode);
                    player.setGodMod(godMode);
                    player.setNoClip(godMode);
                    player.setFastMoveCheat(godMode);
                    if (GameServer.server) {
                        for (WorldMapRemotePlayer remotePlayer : WorldMapRemotePlayers.instance.getPlayers()) {
                            if (remotePlayer.getUsername().equals(player.getUsername())) {
                                remotePlayer.setPlayer(player);
                            }
                        }
                    }

                    for (int nx = 0; nx < numPlayers; nx++) {
                        if (players[nx] != null && players[nx] != player) {
                            players[nx].setInvisible(godMode);
                            players[nx].setGodMod(godMode);
                            players[nx].setNoClip(godMode);
                            players[nx].setFastMoveCheat(godMode);
                        }
                    }

                    if (GameClient.client) {
                        GameClient.sendPlayerExtraInfo(player);
                    }
                }
            }
        }
    }

    private void checkReloading() {
        HandWeapon weapon = Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
        if (weapon != null && weapon.isReloadable(this)) {
            boolean bReload = false;
            boolean bRack = false;
            boolean bShift = false;
            if (this.joypadBind != -1 && this.joypadMovementActive) {
                boolean bButtonDown = JoypadManager.instance.isRBPressed(this.joypadBind);
                if (bButtonDown) {
                    bReload = !this.reloadButtonDown;
                }

                this.reloadButtonDown = bButtonDown;
                bButtonDown = JoypadManager.instance.isLBPressed(this.joypadBind);
                if (bButtonDown) {
                    bRack = !this.rackButtonDown;
                }

                this.rackButtonDown = bButtonDown;
            }

            if (this.playerIndex == 0) {
                boolean bKeyDown = GameKeyboard.isKeyDown("ReloadWeapon");
                if (bKeyDown) {
                    bReload = !this.reloadKeyDown;
                }

                this.reloadKeyDown = bKeyDown;
                bKeyDown = GameKeyboard.isKeyDown("Rack Firearm");
                if (bKeyDown) {
                    bRack = !this.rackKeyDown;
                }

                this.rackKeyDown = bKeyDown;
                if (GameKeyboard.isKeyDown(42) || GameKeyboard.isKeyDown(54)) {
                    bShift = true;
                }
            }

            if (bReload) {
                this.setVariable("WeaponReloadType", weapon.getWeaponReloadType());
                LuaEventManager.triggerEvent("OnPressReloadButton", this, weapon);
            } else if (bRack) {
                this.setVariable("WeaponReloadType", weapon.getWeaponReloadType());
                LuaEventManager.triggerEvent("OnPressRackButton", this, weapon, bShift);
            }
        }
    }

    @Override
    public void postupdate() {
        try (AbstractPerformanceProfileProbe ignored = IsoPlayer.s_performance.postUpdate.profile()) {
            this.postupdateInternal();
        }
    }

    private void postupdateInternal() {
        boolean bHasHitReaction = this.hasHitReaction();
        super.postupdate();
        if (bHasHitReaction
            && this.hasHitReaction()
            && !this.isCurrentState(PlayerHitReactionState.instance())
            && !this.isCurrentState(PlayerHitReactionPVPState.instance())) {
            this.setHitReaction("");
        }

        if (this.isNpc) {
            GameTime gameTime = GameTime.getInstance();
            float time = 1.0F / gameTime.getMinutesPerDay() / 60.0F * gameTime.getMultiplier() / 2.0F;
            if (Core.lastStand) {
                time = 1.0F / gameTime.getMinutesPerDay() / 60.0F * gameTime.getUnmoddedMultiplier() / 2.0F;
            }

            this.setHoursSurvived(this.getHoursSurvived() + time);
        }

        if (this.getBodyDamage() != null) {
            this.getBodyDamage().setBodyPartsLastState();
        }
    }

    @Override
    public boolean isSolidForSeparate() {
        return this.isGhostMode() ? false : super.isSolidForSeparate();
    }

    @Override
    public boolean isPushableForSeparate() {
        if (this.isCurrentState(PlayerHitReactionState.instance())) {
            return false;
        } else {
            return this.isCurrentState(SwipeStatePlayer.instance()) ? false : super.isPushableForSeparate();
        }
    }

    @Override
    public boolean isPushedByForSeparate(IsoMovingObject other) {
        if (!this.isPlayerMoving() && other.isZombie() && ((IsoZombie)other).isAttacking()) {
            return false;
        } else {
            return !GameClient.client || this.isLocalPlayer() && this.isJustMoved() ? super.isPushedByForSeparate(other) : false;
        }
    }

    private void updateExt() {
        if (!this.isSneaking()) {
            if (!this.isGrappling() && !this.isBeingGrappled()) {
                this.extUpdateCount = this.extUpdateCount + GameTime.getInstance().getMultiplier() / 0.8F;
                if (!this.getAdvancedAnimator().containsAnyIdleNodes() && !this.isSitOnGround()) {
                    this.extUpdateCount = 0.0F;
                }

                if (!(this.extUpdateCount <= 5000.0F)) {
                    this.extUpdateCount = 0.0F;
                    if (this.stats.numVisibleZombies == 0 && this.stats.numChasingZombies == 0) {
                        if (Rand.NextBool(3)) {
                            if (this.getAdvancedAnimator().containsAnyIdleNodes() || this.isSitOnGround()) {
                                this.onIdlePerformFidgets();
                                this.reportEvent("EventDoExt");
                            }
                        }
                    }
                }
            }
        }
    }

    private void onIdlePerformFidgets() {
        int RAND_INJURY = 7;
        int RAND_DISCOMFORT = 7;
        int RAND_SICK = 7;
        int RAND_ENDURANCE = 10;
        int RAND_IDLE_EMOTE = 10;
        Moodles moodles = this.getMoodles();
        BodyDamage bodyDamage = this.getBodyDamage();
        if (moodles.getMoodleLevel(MoodleType.HYPOTHERMIA) > 0 && Rand.NextBool(7)) {
            this.setVariable("Ext", "Shiver");
        } else if (moodles.getMoodleLevel(MoodleType.HYPERTHERMIA) > 0 && Rand.NextBool(7)) {
            this.setVariable("Ext", "WipeBrow");
        } else if (moodles.getMoodleLevel(MoodleType.SICK) > 0 && Rand.NextBool(7)) {
            if (Rand.NextBool(4)) {
                this.setVariable("Ext", "Cough");
            } else {
                this.setVariable("Ext", "PainStomach" + (Rand.Next(2) + 1));
            }
        } else if (moodles.getMoodleLevel(MoodleType.ENDURANCE) > 2 && Rand.NextBool(10)) {
            if (Rand.NextBool(5) && !this.isSitOnGround()) {
                this.setVariable("Ext", "BentDouble");
            } else {
                this.setVariable("Ext", "WipeBrow");
            }
        } else if (moodles.getMoodleLevel(MoodleType.TIRED) > 2 && Rand.NextBool(10)) {
            if (Rand.NextBool(7)) {
                this.setVariable("Ext", "TiredStretch");
            } else if (Rand.NextBool(7)) {
                this.setVariable("Ext", "Sway");
            } else {
                this.setVariable("Ext", "Yawn");
            }
        } else if (bodyDamage.doBodyPartsHaveInjuries(BodyPartType.Head, BodyPartType.Neck) && Rand.NextBool(7)) {
            if (bodyDamage.areBodyPartsBleeding(BodyPartType.Head, BodyPartType.Neck) && Rand.NextBool(2)) {
                this.setVariable("Ext", "WipeHead");
            } else {
                this.setVariable("Ext", "PainHead" + (Rand.Next(2) + 1));
            }
        } else if (bodyDamage.doBodyPartsHaveInjuries(BodyPartType.UpperArm_L, BodyPartType.ForeArm_L) && Rand.NextBool(7)) {
            if (bodyDamage.areBodyPartsBleeding(BodyPartType.UpperArm_L, BodyPartType.ForeArm_L) && Rand.NextBool(2)) {
                this.setVariable("Ext", "WipeArmL");
            } else {
                this.setVariable("Ext", "PainArmL");
            }
        } else if (bodyDamage.doBodyPartsHaveInjuries(BodyPartType.UpperArm_R, BodyPartType.ForeArm_R) && Rand.NextBool(7)) {
            if (bodyDamage.areBodyPartsBleeding(BodyPartType.UpperArm_R, BodyPartType.ForeArm_R) && Rand.NextBool(2)) {
                this.setVariable("Ext", "WipeArmR");
            } else {
                this.setVariable("Ext", "PainArmR");
            }
        } else if (bodyDamage.doesBodyPartHaveInjury(BodyPartType.Hand_L) && Rand.NextBool(7)) {
            this.setVariable("Ext", "PainHandL");
        } else if (bodyDamage.doesBodyPartHaveInjury(BodyPartType.Hand_R) && Rand.NextBool(7)) {
            this.setVariable("Ext", "PainHandR");
        } else if (!this.isSitOnGround() && bodyDamage.doBodyPartsHaveInjuries(BodyPartType.UpperLeg_L, BodyPartType.LowerLeg_L) && Rand.NextBool(7)) {
            if (bodyDamage.areBodyPartsBleeding(BodyPartType.UpperLeg_L, BodyPartType.LowerLeg_L) && Rand.NextBool(2)) {
                this.setVariable("Ext", "WipeLegL");
            } else {
                this.setVariable("Ext", "PainLegL");
            }
        } else if (!this.isSitOnGround() && bodyDamage.doBodyPartsHaveInjuries(BodyPartType.UpperLeg_R, BodyPartType.LowerLeg_R) && Rand.NextBool(7)) {
            if (bodyDamage.areBodyPartsBleeding(BodyPartType.UpperLeg_R, BodyPartType.LowerLeg_R) && Rand.NextBool(2)) {
                this.setVariable("Ext", "WipeLegR");
            } else {
                this.setVariable("Ext", "PainLegR");
            }
        } else if (bodyDamage.doBodyPartsHaveInjuries(BodyPartType.Torso_Upper, BodyPartType.Torso_Lower) && Rand.NextBool(7)) {
            if (bodyDamage.areBodyPartsBleeding(BodyPartType.Torso_Upper, BodyPartType.Torso_Lower) && Rand.NextBool(2)) {
                this.setVariable("Ext", "WipeTorso" + (Rand.Next(2) + 1));
            } else {
                this.setVariable("Ext", "PainTorso");
            }
        } else if (WeaponType.getWeaponType(this) != WeaponType.UNARMED) {
            this.setVariable("Ext", Rand.Next(5) + 1 + "");
        } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "ChewNails");
        } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "ShiftWeight");
        } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "PullAtColar");
        } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "BridgeNose");
        } else {
            this.setVariable("Ext", Rand.Next(5) + 1 + "");
        }
    }

    private boolean updateUseKey() {
        if (GameServer.server) {
            return false;
        } else if (!this.isLocalPlayer()) {
            return false;
        } else if (this.playerIndex != 0) {
            return false;
        } else {
            this.timePressedContext = this.timePressedContext + GameTime.instance.getRealworldSecondsSinceLastUpdate();
            boolean bInteract = GameKeyboard.isKeyDown("Interact");
            if (bInteract && this.timePressedContext < 0.5F) {
                this.pressContext = true;
            } else {
                if (this.pressContext && (Core.currentTextEntryBox != null && Core.currentTextEntryBox.isDoingTextEntry() || !GameKeyboard.doLuaKeyPressed)) {
                    this.pressContext = false;
                }

                if (this.isGrappleThrowOutWindow()) {
                    this.pressContext = false;
                }

                if (this.pressContext && this.doContext()) {
                    this.clearUseKeyVariables();
                    return true;
                }

                if (!bInteract) {
                    this.clearUseKeyVariables();
                }
            }

            return false;
        }
    }

    private void clearUseKeyVariables() {
        this.pressContext = false;
        this.timePressedContext = 0.0F;
    }

    private void updateHitByVehicle() {
        if (!GameServer.server) {
            if (this.isLocalPlayer()) {
                if (this.vehicle4testCollision != null && this.ulBeatenVehicle.Check() && SandboxOptions.instance.damageToPlayerFromHitByACar.getValue() > 1) {
                    BaseVehicle vehicle = this.vehicle4testCollision;
                    this.vehicle4testCollision = null;
                    if (vehicle.isEngineRunning() && this.getVehicle() != vehicle) {
                        float vx = vehicle.jniLinearVelocity.x;
                        float vy = vehicle.jniLinearVelocity.z;
                        float speed = (float)Math.sqrt(vx * vx + vy * vy);
                        Vector2 vector2 = Vector2ObjectPool.get().alloc();
                        Vector2 v = vehicle.testCollisionWithCharacter(this, 0.20000002F, vector2);
                        if (v != null && v.x != -1.0F) {
                            v.x = (v.x - vehicle.getX()) * speed * 1.0F + this.getX();
                            v.y = (v.y - vehicle.getY()) * speed * 1.0F + this.getX();
                            if (this.isOnFloor()) {
                                int numWheelsHit = vehicle.testCollisionWithProneCharacter(this, false);
                                if (numWheelsHit > 0) {
                                    this.doBeatenVehicle(Math.max(speed * 6.0F, 5.0F));
                                }

                                this.doBeatenVehicle(0.0F);
                            } else if (this.getCurrentState() != PlayerFallDownState.instance() && speed > 0.1F) {
                                this.doBeatenVehicle(Math.max(speed * 2.0F, 5.0F));
                            }
                        }

                        Vector2ObjectPool.get().release(vector2);
                    }
                }
            }
        }
    }

    private void updateSoundListener() {
        if (!GameServer.server) {
            if (this.isLocalPlayer()) {
                if (this.soundListener == null) {
                    this.soundListener = (BaseSoundListener)(Core.soundDisabled
                        ? new DummySoundListener(this.playerIndex)
                        : new SoundListener(this.playerIndex));
                }

                this.soundListener.setPos(this.getX(), this.getY(), this.getZ());
                this.checkNearbyRooms = this.checkNearbyRooms - GameTime.getInstance().getThirtyFPSMultiplier();
                if (this.checkNearbyRooms <= 0.0F) {
                    this.checkNearbyRooms = 30.0F;
                    this.numNearbyBuildingsRooms = IsoWorld.instance.metaGrid.countNearbyBuildingsRooms(this);
                }

                this.soundListener.tick();
            }
        }
    }

    @Override
    public void updateMovementRates() {
        BodyPart footL = this.bodyDamage.getBodyPart(BodyPartType.Foot_L);
        BodyPart footR = this.bodyDamage.getBodyPart(BodyPartType.Foot_R);
        boolean haveGlassL = footL.haveGlass();
        boolean haveGlassR = footR.haveGlass();
        this.calculateWalkSpeed();
        this.idleSpeed = this.calculateIdleSpeed();
        this.updateFootInjuries();
        if (GameClient.client && (!haveGlassL && footL.haveGlass() || !haveGlassR && footR.haveGlass())) {
            GameClient.sendPlayerDamage(this);
        }
    }

    public void pressedAttack() {
        CombatManager.getInstance().pressedAttack(this);
    }

    public void setAttackVariationX(float in_attackVariationX) {
        this.attackVariationX = in_attackVariationX;
    }

    private float getAttackVariationX() {
        return this.attackVariationX;
    }

    public void setAttackVariationY(float in_attackVariationY) {
        this.attackVariationY = in_attackVariationY;
    }

    private float getAttackVariationY() {
        return this.attackVariationY;
    }

    public boolean canPerformHandToHandCombat() {
        return this.primaryHandModel != null
                && !StringUtils.isNullOrEmpty(this.primaryHandModel.maskVariableValue)
                && this.secondaryHandModel != null
                && !StringUtils.isNullOrEmpty(this.secondaryHandModel.maskVariableValue)
            ? false
            : this.getPrimaryHandItem() == null
                || this.getPrimaryHandItem().getItemReplacementPrimaryHand() == null
                || this.getSecondaryHandItem() == null
                || this.getSecondaryHandItem().getItemReplacementSecondHand() == null;
    }

    @Override
    public void clearHandToHandAttack() {
        super.clearHandToHandAttack();
        this.setInitiateAttack(false);
        this.attackStarted = false;
        this.setAttackType(AttackType.NONE);
    }

    public void setAttackAnimThrowTimer(long dt) {
        this.attackAnimThrowTimer = System.currentTimeMillis() + dt;
    }

    public boolean isAttackAnimThrowTimeOut() {
        return this.attackAnimThrowTimer <= System.currentTimeMillis();
    }

    @Override
    public boolean isAiming() {
        if (!this.isAttackAnimThrowTimeOut()) {
            return true;
        } else {
            return GameClient.client && this.isLocalPlayer() && DebugOptions.instance.multiplayer.debug.attackPlayer.getValue() ? false : super.isAiming();
        }
    }

    private String getWeaponType() {
        return !this.isAttackAnimThrowTimeOut() ? "throwing" : this.weaponT;
    }

    private void setWeaponType(String val) {
        this.weaponT = val;
    }

    public int calculateCritChance(IsoGameCharacter target) {
        if (this.isDoShove()) {
            float baseChance = 35.0F;
            if (target instanceof IsoPlayer plyr) {
                baseChance = 20.0F;
                if (GameClient.client && !plyr.isLocalPlayer()) {
                    double weight = plyr instanceof IsoAnimal isoAnimal ? isoAnimal.getData().getWeight() : plyr.getNutrition().getWeight();
                    baseChance -= plyr.remoteStrLvl * 1.5F;
                    if (weight < 80.0) {
                        baseChance += (float)Math.abs((weight - 80.0) / 2.0);
                    } else {
                        baseChance -= (float)((weight - 80.0) / 2.0);
                    }
                }
            }

            baseChance -= this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 5;
            baseChance -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 5;
            baseChance -= this.getMoodles().getMoodleLevel(MoodleType.PANIC) * 1.3F;
            baseChance += this.getPerkLevel(PerkFactory.Perks.Strength) * 2;
            return (int)baseChance;
        } else if (this.isDoShove() && target.getStateMachine().getCurrent() == StaggerBackState.instance() && target instanceof IsoZombie) {
            return 100;
        } else if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon handWeapon) {
            float var14 = handWeapon.getCriticalChance();
            if (handWeapon.isAlwaysKnockdown()) {
                return 100;
            } else {
                WeaponType weaponType = WeaponType.getWeaponType(this);
                if (weaponType.isRanged()) {
                    var14 += handWeapon.getAimingPerkCritModifier() * this.getPerkLevel(PerkFactory.Perks.Aiming);
                    float dist = this.DistToProper(target);
                    float max = handWeapon.getMaxSightRange(this);
                    float min = handWeapon.getMinSightRange(this);
                    var14 += PZMath.max(
                        CombatManager.getInstance().getDistanceModifierSightless(dist, false)
                            - CombatManager.getInstance().getAimDelayPenaltySightless(PZMath.max(0.0F, this.getAimingDelay()), dist),
                        CombatManager.getInstance().getDistanceModifier(dist, min, max, false)
                            - CombatManager.getInstance().getAimDelayPenalty(PZMath.max(0.0F, this.getAimingDelay()), dist, min, max)
                    );
                    var14 -= CombatManager.getInstance().getMoodlesPenalty(this, dist);
                    var14 -= CombatManager.getInstance().getWeatherPenalty(this, handWeapon, target.getCurrentSquare(), dist);
                    var14 -= CombatManager.getMovePenalty(this, dist);
                    if (this.characterTraits.get(CharacterTrait.MARKSMAN)) {
                        var14 += 10.0F;
                    }
                } else {
                    if (handWeapon.isTwoHandWeapon() && (this.getPrimaryHandItem() != handWeapon || this.getSecondaryHandItem() != handWeapon)) {
                        var14 -= var14 / 3.0F;
                    }

                    if (this.chargeTime < 2.0F) {
                        var14 -= var14 / 5.0F;
                    }

                    int skill = this.getPerkLevel(PerkFactory.Perks.Blunt);
                    if (handWeapon.isOfWeaponCategory(WeaponCategory.AXE)) {
                        skill = this.getPerkLevel(PerkFactory.Perks.Axe);
                    }

                    if (handWeapon.isOfWeaponCategory(WeaponCategory.LONG_BLADE)) {
                        skill = this.getPerkLevel(PerkFactory.Perks.LongBlade);
                    }

                    if (handWeapon.isOfWeaponCategory(WeaponCategory.SPEAR)) {
                        skill = this.getPerkLevel(PerkFactory.Perks.Spear);
                    }

                    if (handWeapon.isOfWeaponCategory(WeaponCategory.SMALL_BLADE)) {
                        skill = this.getPerkLevel(PerkFactory.Perks.SmallBlade);
                    }

                    if (handWeapon.isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
                        skill = this.getPerkLevel(PerkFactory.Perks.SmallBlunt);
                    }

                    var14 += skill * 3.0F;
                    if (target instanceof IsoPlayer plyrx && !plyrx.isAnimal() && GameClient.client && !plyrx.isLocalPlayer()) {
                        var14 -= plyrx.remoteStrLvl * 1.5F;
                        if (plyrx.getNutrition().getWeight() < 80.0) {
                            var14 += (float)Math.abs((plyrx.getNutrition().getWeight() - 80.0) / 2.0);
                        } else {
                            var14 -= (float)((plyrx.getNutrition().getWeight() - 80.0) / 2.0);
                        }
                    }

                    var14 -= this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 5;
                    var14 -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 5;
                    var14 -= this.getMoodles().getMoodleLevel(MoodleType.PANIC) * 1.3F;
                }

                if (target instanceof IsoZombie) {
                    if (SandboxOptions.instance.lore.toughness.getValue() == 1) {
                        var14 -= 6.0F;
                    }

                    if (SandboxOptions.instance.lore.toughness.getValue() == 3) {
                        var14 += 6.0F;
                    }
                }

                return (int)PZMath.clamp(var14, 10.0F, 90.0F);
            }
        } else {
            return 0;
        }
    }

    private void checkJoypadIgnoreAimUntilCentered() {
        if (this.joypadIgnoreAimUntilCentered) {
            if (GameWindow.activatedJoyPad != null && this.joypadBind != -1 && this.joypadMovementActive) {
                float x = JoypadManager.instance.getAimingAxisX(this.joypadBind);
                float y = JoypadManager.instance.getAimingAxisY(this.joypadBind);
                if (x * x + y + y <= 0.0F) {
                    this.joypadIgnoreAimUntilCentered = false;
                }
            }
        }
    }

    public boolean isAimControlActive() {
        if (this.isForceAim()) {
            return true;
        } else {
            return this.isAimKeyDown()
                ? true
                : GameWindow.activatedJoyPad != null
                    && this.joypadBind != -1
                    && this.joypadMovementActive
                    && this.getJoypadAimVector(tempo).getLengthSquared() > 0.0F;
        }
    }

    @Override
    public boolean isGettingUp() {
        return this.getCurrentState() == PlayerGetUpState.instance();
    }

    @Override
    public boolean allowsTwist() {
        if (this.isAttacking() || this.isKnockedDown() || this.isGettingUp() || this.isPerformingAnAction()) {
            return false;
        } else if (this.isAiming()) {
            return true;
        } else {
            return this.isSneaking() ? false : this.isTwisting();
        }
    }

    private Vector2 getJoypadAimVector(Vector2 out) {
        if (this.joypadIgnoreAimUntilCentered) {
            return out.set(0.0F, 0.0F);
        } else {
            float aimY = JoypadManager.instance.getAimingAxisY(this.joypadBind);
            float aimX = JoypadManager.instance.getAimingAxisX(this.joypadBind);
            float deadZone = JoypadManager.instance.getDeadZone(this.joypadBind, 0);
            if (aimX * aimX + aimY * aimY < deadZone * deadZone) {
                aimY = 0.0F;
                aimX = 0.0F;
            }

            return out.set(aimX, aimY);
        }
    }

    private Vector2 getJoypadMoveVector(Vector2 out) {
        float moveY = JoypadManager.instance.getMovementAxisY(this.joypadBind);
        float moveX = JoypadManager.instance.getMovementAxisX(this.joypadBind);
        float deadZone = JoypadManager.instance.getDeadZone(this.joypadBind, 0);
        if (moveX * moveX + moveY * moveY < deadZone * deadZone) {
            moveY = 0.0F;
            moveX = 0.0F;
        }

        out.set(moveX, moveY);
        if (this.isIgnoreInputsForDirection()) {
            out.set(0.0F, 0.0F);
        }

        return out;
    }

    private void updateToggleToAim() {
        if (this.playerIndex == 0) {
            if (!Core.getInstance().isToggleToAim()) {
                this.setForceAim(false);
            } else {
                boolean keyDown = this.isAimKeyDownIgnoreMouse();
                long now = System.currentTimeMillis();
                if (keyDown) {
                    if (this.aimKeyDownMs == 0L) {
                        this.aimKeyDownMs = now;
                    }
                } else {
                    if (this.aimKeyDownMs != 0L && now - this.aimKeyDownMs < 500L) {
                        this.toggleForceAim();
                    } else if (this.isForceAim()) {
                        if (this.aimKeyDownMs != 0L) {
                            this.toggleForceAim();
                        } else {
                            int aimKey = GameKeyboard.whichKeyDownIgnoreMouse("Aim");
                            boolean bAimKeyIsCtrl = aimKey == 29 || aimKey == 157;
                            if (bAimKeyIsCtrl && UIManager.isMouseOverInventory()) {
                                this.toggleForceAim();
                            }
                        }
                    }

                    this.aimKeyDownMs = 0L;
                }
            }
        }
    }

    private void UpdateInputState(IsoPlayer.InputState state) {
        state.melee = false;
        state.grapple = false;
        if (!MPDebugAI.updateInputState(this, state)) {
            if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
                if (this.joypadMovementActive) {
                    state.isAttacking = this.isCharging;
                    if (this.joypadIgnoreChargingRt) {
                        state.isAttacking = false;
                    }

                    if (this.joypadIgnoreAimUntilCentered) {
                        float aimX = JoypadManager.instance.getAimingAxisX(this.joypadBind);
                        float aimY = JoypadManager.instance.getAimingAxisY(this.joypadBind);
                        if (aimX == 0.0F && aimY == 0.0F) {
                            this.joypadIgnoreAimUntilCentered = false;
                        }
                    }
                }

                if (this.isChargingLt) {
                    state.melee = true;
                    state.isAttacking = false;
                }
            } else {
                state.isAttacking = this.isCharging && GameKeyboard.isKeyDown("Attack/Click");
                if (GameKeyboard.isKeyDown("Melee") && this.isAuthorizedHandToHandAction()) {
                    state.melee = true;
                    state.isAttacking = false;
                }

                if ((GameKeyboard.isKeyDown("Interact") || this.getGrapplingTarget() != null && ((IsoGameCharacter)this.getGrapplingTarget()).isOnFire())
                    && this.isAuthorizedHandToHandAction()) {
                    state.grapple = true;
                    state.isAttacking = false;
                }
            }

            if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
                if (this.joypadMovementActive) {
                    state.isCharging = JoypadManager.instance.isRTPressed(this.joypadBind);
                    state.isChargingLt = JoypadManager.instance.isLTPressed(this.joypadBind);
                    if (this.joypadIgnoreChargingRt && !state.isCharging) {
                        this.joypadIgnoreChargingRt = false;
                    }
                }

                state.isAiming = false;
                state.running = false;
                state.sprinting = false;
                Vector2 aimVec = this.getJoypadAimVector(tempVector2);
                if (aimVec.x == 0.0F && aimVec.y == 0.0F) {
                    state.isCharging = false;
                    Vector2 moveVec = this.getJoypadMoveVector(tempVector2);
                    if (this.isAutoWalk() && moveVec.getLengthSquared() == 0.0F) {
                        moveVec.set(this.getAutoWalkDirection());
                    }

                    if (moveVec.x != 0.0F || moveVec.y != 0.0F) {
                        if (this.isAllowRun()) {
                            state.running = JoypadManager.instance.isRTPressed(this.joypadBind);
                        }

                        state.isAttacking = false;
                        state.melee = false;
                        this.joypadIgnoreChargingRt = true;
                        state.isCharging = false;
                        boolean isBDown = JoypadManager.instance.isBPressed(this.joypadBind);
                        if (state.running && isBDown && !this.joypadBDown) {
                            this.joypadSprint = !this.joypadSprint;
                        }

                        this.joypadBDown = isBDown;
                        state.sprinting = this.joypadSprint;
                    }
                } else {
                    state.isAiming = true;
                }

                if (!state.running) {
                    this.joypadBDown = false;
                    this.joypadSprint = false;
                }
            } else {
                state.isAiming = this.isAimKeyDown() && this.getPlayerNum() == 0 && StringUtils.isNullOrEmpty(this.getVariableString("BumpFallType"));
                state.isCharging = this.isAimKeyDown();
                if (this.isAllowRun()) {
                    state.running = GameKeyboard.isKeyDown("Run");
                } else {
                    state.running = false;
                }

                if (this.isAllowSprint()) {
                    if (!Core.getInstance().isOptiondblTapJogToSprint()) {
                        if (GameKeyboard.isKeyDown("Sprint")) {
                            state.sprinting = true;
                            this.pressedRunTimer = 1.0F;
                        } else {
                            state.sprinting = false;
                        }
                    } else {
                        if (!GameKeyboard.wasKeyDown("Run") && GameKeyboard.isKeyDown("Run") && this.pressedRunTimer < 30.0F && this.pressedRun) {
                            state.sprinting = true;
                        }

                        if (GameKeyboard.wasKeyDown("Run") && !GameKeyboard.isKeyDown("Run")) {
                            state.sprinting = false;
                            this.pressedRun = true;
                        }

                        if (!state.running) {
                            state.sprinting = false;
                        }

                        if (this.pressedRun) {
                            this.pressedRunTimer++;
                        }

                        if (this.pressedRunTimer > 30.0F) {
                            this.pressedRunTimer = 0.0F;
                            this.pressedRun = false;
                        }
                    }
                } else {
                    state.sprinting = false;
                }

                this.updateToggleToAim();
                if (state.running || state.sprinting) {
                    this.setForceAim(false);
                }

                if (this.playerIndex == 0 && Core.getInstance().isToggleToRun()) {
                    boolean keyDown = GameKeyboard.isKeyDown("Run");
                    boolean keyWasDown = GameKeyboard.wasKeyDown("Run");
                    long now = System.currentTimeMillis();
                    if (keyDown && !keyWasDown) {
                        this.runKeyDownMs = now;
                    } else if (!keyDown && keyWasDown && now - this.runKeyDownMs < 500L) {
                        this.toggleForceRun();
                    }
                }

                if (this.playerIndex == 0 && Core.getInstance().isToggleToSprint()) {
                    boolean keyDown = GameKeyboard.isKeyDown("Sprint");
                    boolean keyWasDown = GameKeyboard.wasKeyDown("Sprint");
                    long now = System.currentTimeMillis();
                    if (keyDown && !keyWasDown) {
                        this.sprintKeyDownMs = now;
                    } else if (!keyDown && keyWasDown && now - this.sprintKeyDownMs < 500L) {
                        this.toggleForceSprint();
                    }
                }

                if (this.isForceAim()) {
                    state.isAiming = true;
                    state.isCharging = true;
                }

                if (this.isForceRun()) {
                    state.running = true;
                }

                if (this.isForceSprint()) {
                    state.sprinting = true;
                }
            }
        }
    }

    public IsoGameCharacter getClosestTo(IsoGameCharacter closestTo) {
        IsoZombie result = null;
        ArrayList<IsoZombie> zeds = new ArrayList<>();
        ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();

        for (int n = 0; n < objects.size(); n++) {
            IsoMovingObject obj = objects.get(n);
            if (obj != closestTo && obj instanceof IsoZombie isoZombie) {
                zeds.add(isoZombie);
            }
        }

        float currentDist = 0.0F;

        for (int nx = 0; nx < zeds.size(); nx++) {
            IsoZombie testZed = zeds.get(nx);
            float dist = IsoUtils.DistanceTo(testZed.getX(), testZed.getY(), closestTo.getX(), closestTo.getY());
            if (result == null || dist < currentDist) {
                result = testZed;
                currentDist = dist;
            }
        }

        return result;
    }

    @Override
    public void hitConsequences(HandWeapon weapon, IsoGameCharacter wielder, boolean bIgnoreDamage, float damage, boolean bRemote) {
        if (wielder instanceof IsoAnimal isoAnimal) {
            this.getBodyDamage().DamageFromAnimal(isoAnimal);
            this.setKnockedDown(isoAnimal.adef.knockdownAttack);
            this.setHitReaction("HitReaction");
        } else {
            String hitReaction = wielder.getVariableString("ZombieHitReaction");
            if ("Shot".equals(hitReaction)) {
                wielder.setCriticalHit(Rand.Next(100) < ((IsoPlayer)wielder).calculateCritChance(this));
            }

            if (wielder instanceof IsoPlayer && (GameServer.server || GameClient.client)) {
                if (ServerOptions.getInstance().knockedDownAllowed.getValue()) {
                    this.setKnockedDown(wielder.isCriticalHit());
                }
            } else {
                this.setKnockedDown(wielder.isCriticalHit());
            }

            if (wielder instanceof IsoPlayer) {
                if (!StringUtils.isNullOrEmpty(this.getHitReaction())) {
                    this.getActionContext().reportEvent("washitpvpagain");
                }

                this.getActionContext().reportEvent("washitpvp");
                this.setVariable("hitpvp", true);
            } else {
                this.getActionContext().reportEvent("washit");
            }

            if (bIgnoreDamage) {
                if (GameServer.server) {
                    GameServer.addXp((IsoPlayer)wielder, PerkFactory.Perks.Strength, 2.0F);
                } else {
                    if (!GameClient.client) {
                        wielder.xp.AddXP(PerkFactory.Perks.Strength, 2.0F);
                    }

                    this.setHitForce(Math.min(0.5F, this.getHitForce()));
                    this.setHitReaction("HitReaction");
                    String dotSide = this.testDotSide(wielder);
                    this.setHitFromBehind("BEHIND".equals(dotSide));
                }
            } else {
                if (GameClient.client && wielder instanceof IsoPlayer) {
                    this.getBodyDamage().splatBloodFloorBig();
                } else {
                    this.getBodyDamage().DamageFromWeapon(weapon, -1);
                }

                if ("Bite".equals(hitReaction)) {
                    String rightStr = "LEFT";
                    String leftStr = "RIGHT";
                    String dotSide = this.testDotSide(wielder);
                    if (dotSide.equals("RIGHT")) {
                        hitReaction = hitReaction + "LEFT";
                    }

                    if (dotSide.equals("LEFT")) {
                        hitReaction = hitReaction + "RIGHT";
                    }

                    if (hitReaction != null && !"".equals(hitReaction)) {
                        this.setHitReaction(hitReaction);
                    }
                } else if (!this.isKnockedDown()) {
                    this.setHitReaction("HitReaction");
                }
            }
        }
    }

    private HandWeapon getWeapon() {
        if (this.getPrimaryHandItem() instanceof HandWeapon) {
            return (HandWeapon)this.getPrimaryHandItem();
        } else {
            return this.getSecondaryHandItem() instanceof HandWeapon ? (HandWeapon)this.getSecondaryHandItem() : InventoryItemFactory.CreateItem("BareHands");
        }
    }

    private void updateMechanicsItems() {
        if (!this.mechanicsItem.isEmpty()) {
            Iterator<Long> it = this.mechanicsItem.keySet().iterator();
            ArrayList<Long> toremove = new ArrayList<>();

            while (it.hasNext()) {
                Long item = it.next();
                Long milli = this.mechanicsItem.get(item);
                if (GameTime.getInstance().getCalender().getTimeInMillis() > milli + 86400000L) {
                    toremove.add(item);
                }
            }

            for (int i = 0; i < toremove.size(); i++) {
                this.mechanicsItem.remove(toremove.get(i));
            }
        }
    }

    private void enterExitVehicle() {
        boolean interact = this.playerIndex == 0 && GameKeyboard.isKeyDown("Interact");
        if (interact) {
            this.useVehicle = true;
            this.useVehicleDuration = this.useVehicleDuration + GameTime.instance.getRealworldSecondsSinceLastUpdate();
        }

        if (!this.usedVehicle && this.useVehicle && (!interact || this.useVehicleDuration > 0.5F)) {
            if (!this.getCharacterActions().isEmpty()) {
                this.setTimedActionToRetrigger((LuaTimedActionNew)this.getCharacterActions().get(0));
            }

            this.usedVehicle = true;
            if (this.getVehicle() != null) {
                LuaEventManager.triggerEvent("OnUseVehicle", this, this.getVehicle(), this.useVehicleDuration > 0.5F);
            } else {
                BaseVehicle vehicle1 = this.getUseableVehicle();
                if (vehicle1 != null) {
                    LuaEventManager.triggerEvent("OnUseVehicle", this, vehicle1, this.useVehicleDuration > 0.5F);
                }
            }
        }

        if (!interact) {
            this.useVehicle = false;
            this.usedVehicle = false;
            this.useVehicleDuration = 0.0F;
        }
    }

    public void checkActionGroup() {
        ActionGroup actionGroup = this.getActionContext().getGroup();
        if (this.getVehicle() == null) {
            ActionGroup playerGroup = ActionGroup.getActionGroup("player");
            if (actionGroup != playerGroup) {
                this.advancedAnimator.OnAnimDataChanged(false);
                this.initializeStates();
                this.getActionContext().setGroup(playerGroup);
                this.clearVariable("bEnteringVehicle");
                this.clearVariable("EnterAnimationFinished");
                this.clearVariable("bExitingVehicle");
                this.clearVariable("ExitAnimationFinished");
                this.clearVariable("bSwitchingSeat");
                this.clearVariable("SwitchSeatAnimationFinished");
                this.setHitReaction("");
            }
        } else {
            ActionGroup vehicleGroup = ActionGroup.getActionGroup("player-vehicle");
            if (actionGroup != vehicleGroup) {
                this.advancedAnimator.OnAnimDataChanged(false);
                this.initializeStates();
                this.getActionContext().setGroup(vehicleGroup);
            }
        }
    }

    public BaseVehicle getUseableVehicle() {
        if (this.getVehicle() != null) {
            return null;
        } else {
            BaseVehicle bestSeat = null;
            int chunkMinX = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
            int chunkMinY = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
            int chunkMaxX = (int)Math.ceil((this.getX() + 4.0F) / 8.0F) + 1;
            int chunkMaxY = (int)Math.ceil((this.getY() + 4.0F) / 8.0F) + 1;

            for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle.getUseablePart(this) != null) {
                                return vehicle;
                            }

                            if (vehicle.getBestSeat(this) != -1
                                && !PolygonalMap2.instance
                                    .lineClearCollide(
                                        this.getX(), this.getY(), vehicle.getX(), vehicle.getY(), PZMath.fastfloor(this.getZ()), vehicle, false, true
                                    )
                                && (bestSeat == null || this.isBetterBestSeat(vehicle, bestSeat))) {
                                bestSeat = vehicle;
                            }
                        }
                    }
                }
            }

            return bestSeat;
        }
    }

    private boolean isBetterBestSeat(BaseVehicle vehicle1, BaseVehicle vehicle2) {
        Vector2f intersection = BaseVehicle.allocVector2f();
        if (vehicle1.intersectLineWithPoly(
            this.getX(), this.getY(), this.getX() + this.getLookDirectionX() * 4.0F, this.getY() + this.getLookDirectionY() * 4.0F, intersection
        )) {
            BaseVehicle.releaseVector2f(intersection);
            return true;
        } else if (vehicle2.intersectLineWithPoly(
            this.getX(), this.getY(), this.getX() + this.getLookDirectionX() * 4.0F, this.getY() + this.getLookDirectionY() * 4.0F, intersection
        )) {
            BaseVehicle.releaseVector2f(intersection);
            return false;
        } else {
            BaseVehicle.releaseVector2f(intersection);
            int seat1 = vehicle1.getBestSeat(this);
            float dist1 = vehicle1.getEnterSeatDistance(seat1, this.getX(), this.getY());
            int seat2 = vehicle2.getBestSeat(this);
            float dist2 = vehicle2.getEnterSeatDistance(seat2, this.getX(), this.getY());
            return dist1 < dist2;
        }
    }

    public Boolean isNearVehicle() {
        if (this.getVehicle() != null) {
            return false;
        } else {
            int chunkMinX = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
            int chunkMinY = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
            int chunkMaxX = (int)Math.ceil((this.getX() + 4.0F) / 8.0F) + 1;
            int chunkMaxY = (int)Math.ceil((this.getY() + 4.0F) / 8.0F) + 1;

            for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle.getScript() != null && vehicle.DistTo(this) < 3.5F) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    @Override
    public BaseVehicle getNearVehicle() {
        if (this.getVehicle() != null) {
            return null;
        } else {
            int chunkMinX = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
            int chunkMinY = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
            int chunkMaxX = (int)Math.ceil((this.getX() + 4.0F) / 8.0F) + 1;
            int chunkMaxY = (int)Math.ceil((this.getY() + 4.0F) / 8.0F) + 1;

            for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle.getScript() != null
                                && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(vehicle.getZ())
                                && (!this.isLocalPlayer() || vehicle.getTargetAlpha(this.playerIndex) != 0.0F)
                                && !(this.DistToSquared(vehicle) >= 16.0F)
                                && PolygonalMap2.instance
                                    .intersectLineWithVehicle(
                                        this.getX(),
                                        this.getY(),
                                        this.getX() + this.getForwardDirectionX() * 4.0F,
                                        this.getY() + this.getForwardDirectionY() * 4.0F,
                                        vehicle,
                                        tempVector2
                                    )
                                && !PolygonalMap2.instance
                                    .lineClearCollide(
                                        this.getX(), this.getY(), tempVector2.x, tempVector2.y, PZMath.fastfloor(this.getZ()), vehicle, false, true
                                    )) {
                                return vehicle;
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    private void updateWhileInVehicle() {
        this.lookingWhileInVehicle = false;
        ActionGroup actionGroup = this.getActionContext().getGroup();
        ActionGroup vehicleGroup = ActionGroup.getActionGroup("player-vehicle");
        if (actionGroup != vehicleGroup) {
            this.advancedAnimator.OnAnimDataChanged(false);
            this.initializeStates();
            this.getActionContext().setGroup(vehicleGroup);
        }

        if (GameClient.client && this.getVehicle().getSeat(this) == -1) {
            DebugLog.DetailedInfo.trace("forced " + this.getUsername() + " out of vehicle seat -1");
            this.setVehicle(null);
        } else {
            this.dirtyRecalcGridStackTime = 10.0F;
            if (this.getVehicle().isDriver(this)) {
                this.getVehicle().updatePhysics();
                boolean haveControl = true;
                if (this.isAiming()) {
                    WeaponType wpn = WeaponType.getWeaponType(this);
                    if (wpn == WeaponType.FIREARM) {
                        haveControl = false;
                    }
                }

                if (this.getVariableBoolean("isLoading")) {
                    haveControl = false;
                }

                if (haveControl) {
                    this.getVehicle().updateControls();
                }
            } else {
                BaseVehicle vehicleObject = this.getVehicle();
                if (vehicleObject.getDriver() == null && vehicleObject.engineSpeed > vehicleObject.getScript().getEngineIdleSpeed()) {
                    vehicleObject.engineSpeed = Math.max(
                        vehicleObject.engineSpeed - 50.0F * (GameTime.getInstance().getMultiplier() / 0.8F),
                        (double)vehicleObject.getScript().getEngineIdleSpeed()
                    );
                }

                if (GameClient.connection != null) {
                    PassengerMap.updatePassenger(this);
                }
            }

            this.fallTime = 0.0F;
            this.seenThisFrame = false;
            this.couldBeSeenThisFrame = false;
            this.closestZombie = 1000000.0F;
            this.updateAimingDelay();
            this.setBeenMovingFor(this.getBeenMovingFor() - 0.625F * GameTime.getInstance().getMultiplier());
            this.updateEnduranceWhileInVehicle();
            this.updateToggleToAim();
            if (this.vehicle != null) {
                Vector3f vec = this.vehicle.getForwardVector(tempVector3f);
                boolean bAimControlActive = this.isAimControlActive();
                if (!bAimControlActive && this.isCurrentState(IdleState.instance())) {
                    this.setForwardDirection(vec.x, vec.z);
                    this.getForwardDirection().normalize();
                }

                if (this.lastAngle.x != this.getForwardDirectionX() || this.lastAngle.y != this.getForwardDirectionY()) {
                    this.dirtyRecalcGridStackTime = 10.0F;
                }

                this.DirectionFromVector(this.getForwardDirection());
                AnimationPlayer animPlayer = this.getAnimationPlayer();
                if (animPlayer != null && animPlayer.isReady()) {
                    animPlayer.setTargetAndCurrentDirection(this.getForwardDirectionX(), this.getForwardDirectionY());
                    float angle = animPlayer.getAngle() + animPlayer.getTwistAngle();
                    this.dir = IsoDirections.fromAngle(tempVector2.setLengthAndDirection(angle, 1.0F));
                }

                boolean canAttack = false;
                int seatpos = this.vehicle.getSeat(this);
                VehiclePart door = this.vehicle.getPassengerDoor(seatpos);
                if (door != null) {
                    VehicleWindow window = door.findWindow();
                    if (window != null && !window.isHittable()) {
                        canAttack = true;
                    }
                }

                if (canAttack) {
                    this.attackWhileInVehicle();
                } else if (bAimControlActive) {
                    this.lookingWhileInVehicle = true;
                    this.setAngleFromAim();
                } else {
                    this.checkJoypadIgnoreAimUntilCentered();
                    this.setIsAiming(false);
                }
            }

            this.updateCursorVisibility();
        }
    }

    private void attackWhileInVehicle() {
        this.setIsAiming(false);
        boolean isAttacking = false;
        boolean bMelee = false;
        if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
            if (!this.joypadMovementActive) {
                return;
            }

            if (this.isChargingLt && !JoypadManager.instance.isLTPressed(this.joypadBind)) {
                bMelee = true;
            } else {
                isAttacking = this.isCharging && !JoypadManager.instance.isRTPressed(this.joypadBind);
            }

            float aimX = JoypadManager.instance.getAimingAxisX(this.joypadBind);
            float aimY = JoypadManager.instance.getAimingAxisY(this.joypadBind);
            if (this.joypadIgnoreAimUntilCentered) {
                if (aimX == 0.0F && aimY == 0.0F) {
                    this.joypadIgnoreAimUntilCentered = false;
                } else {
                    aimY = 0.0F;
                    aimX = 0.0F;
                }
            }

            this.setIsAiming(aimX * aimX + aimY * aimY >= 0.09F);
            this.isCharging = this.isAiming() && JoypadManager.instance.isRTPressed(this.joypadBind);
            this.isChargingLt = this.isAiming() && JoypadManager.instance.isLTPressed(this.joypadBind);
        } else {
            boolean aimKey = this.isAimKeyDown();
            this.setIsAiming(aimKey);
            this.isCharging = aimKey;
            if (this.isForceAim()) {
                this.setIsAiming(true);
                this.isCharging = true;
            }

            if (GameKeyboard.isKeyDown("Melee") && this.isAuthorizedHandToHandAction()) {
                bMelee = true;
            } else {
                isAttacking = this.isCharging && GameKeyboard.isKeyDown("Attack/Click");
                if (isAttacking) {
                    this.setIsAiming(true);
                }
            }
        }

        if (!this.isCharging && !this.isChargingLt) {
            this.chargeTime = 0.0F;
        }

        if (this.isAiming() && !this.bannedAttacking && this.CanAttack()) {
            this.chargeTime = this.chargeTime + GameTime.instance.getMultiplier();
            this.useChargeTime = this.chargeTime;
            this.meleePressed = bMelee;
            this.setAngleFromAim();
            if (bMelee) {
                this.sprite.animate = true;
                this.setDoShove(true);
                this.AttemptAttack(this.useChargeTime);
                this.useChargeTime = 0.0F;
                this.chargeTime = 0.0F;
            } else if (isAttacking) {
                this.sprite.animate = true;
                if (this.getRecoilDelay() <= 0.0F) {
                    this.AttemptAttack(this.useChargeTime);
                }

                this.useChargeTime = 0.0F;
                this.chargeTime = 0.0F;
            }
        }
    }

    public void setAngleFromAim() {
        Vector2 desiredForward2f = this.updateAimingVectorParams.desiredForward2f;
        if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
            this.getControllerAimDir(desiredForward2f);
        } else {
            float sourceX = this.getX();
            float sourceY = this.getY();
            float sourceZ = this.getZ();
            int mx = Mouse.getX();
            int my = Mouse.getY();
            float raycastZ = sourceZ + 0.47F;
            float raycastX = IsoUtils.XToIso(mx, my, raycastZ);
            float raycastY = IsoUtils.YToIso(mx, my, raycastZ);
            desiredForward2f.set(raycastX - sourceX, raycastY - sourceY);
        }

        if (!(desiredForward2f.getLengthSquared() <= 0.0F)) {
            desiredForward2f.normalize();
            BallisticsController ballisticsController = this.getBallisticsController();
            boolean ballisticsSuccess = ballisticsController != null && ballisticsController.updateAimingVector(this, this.updateAimingVectorParams);
            if (ballisticsSuccess) {
                this.setTargetVerticalAimAngle(this.updateAimingVectorParams.desiredForwardPitchRads * (180.0F / (float)Math.PI));
            } else {
                this.setTargetVerticalAimAngle(0.0F);
            }

            this.DirectionFromVector(desiredForward2f);
            this.setForwardDirection(desiredForward2f);
            if (this.lastAngle.x != desiredForward2f.x || this.lastAngle.y != desiredForward2f.y) {
                this.lastAngle.x = desiredForward2f.x;
                this.lastAngle.y = desiredForward2f.y;
                this.dirtyRecalcGridStackTime = 10.0F;
            }
        }
    }

    private void updateTorchStrength() {
        if (this.getTorchStrength() > 0.0F || this.flickTorch) {
            if (!(this.getActiveLightItem() instanceof DrainableComboItem torch)) {
                return;
            }

            if (Rand.Next(600 - (int)(0.4F / torch.getCurrentUsesFloat() * 100.0F)) == 0) {
                this.flickTorch = true;
            }

            this.flickTorch = false;
            if (this.flickTorch) {
                torch.setActivated(Rand.Next(6) != 0);
                if (Rand.Next(40) == 0) {
                    this.flickTorch = false;
                    torch.setActivated(true);
                }
            }
        }
    }

    /**
     * @return the cell
     */
    @Override
    public IsoCell getCell() {
        return IsoWorld.instance.currentCell;
    }

    public void calculateContext() {
        float cx = this.getX();
        float cy = this.getY();
        float cz = this.getX();
        IsoGridSquare[] sqs = new IsoGridSquare[4];
        if (this.dir == IsoDirections.N) {
            sqs[2] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)(cy - 1.0F), (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)cx, (double)(cy - 1.0F), (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)(cy - 1.0F), (double)cz);
        } else if (this.dir == IsoDirections.NE) {
            sqs[2] = this.getCell().getGridSquare((double)cx, (double)(cy - 1.0F), (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)(cy - 1.0F), (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)cy, (double)cz);
        } else if (this.dir == IsoDirections.E) {
            sqs[2] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)(cy - 1.0F), (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)cy, (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)(cy + 1.0F), (double)cz);
        } else if (this.dir == IsoDirections.SE) {
            sqs[2] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)cy, (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)(cy + 1.0F), (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)cx, (double)(cy + 1.0F), (double)cz);
        } else if (this.dir == IsoDirections.S) {
            sqs[2] = this.getCell().getGridSquare((double)(cx + 1.0F), (double)(cy + 1.0F), (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)cx, (double)(cy + 1.0F), (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)(cy + 1.0F), (double)cz);
        } else if (this.dir == IsoDirections.SW) {
            sqs[2] = this.getCell().getGridSquare((double)cx, (double)(cy + 1.0F), (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)(cy + 1.0F), (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)cy, (double)cz);
        } else if (this.dir == IsoDirections.W) {
            sqs[2] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)(cy + 1.0F), (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)cy, (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)(cy - 1.0F), (double)cz);
        } else if (this.dir == IsoDirections.NW) {
            sqs[2] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)cy, (double)cz);
            sqs[1] = this.getCell().getGridSquare((double)(cx - 1.0F), (double)(cy - 1.0F), (double)cz);
            sqs[3] = this.getCell().getGridSquare((double)cx, (double)(cy - 1.0F), (double)cz);
        }

        sqs[0] = this.current;

        for (int n = 0; n < 4; n++) {
            IsoGridSquare test = sqs[n];
            if (test == null) {
            }
        }
    }

    public boolean isSafeToClimbOver(IsoDirections dir) {
        IsoGridSquare sq = null;
        switch (dir) {
            case N:
                sq = this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ());
                break;
            case NW:
            case SW:
            case SE:
            default:
                return false;
            case W:
                sq = this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
                break;
            case S:
                sq = this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ());
                break;
            case E:
                sq = this.getCell().getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ());
        }

        if (sq == null) {
            return false;
        } else if (sq.has(IsoFlagType.water)) {
            return false;
        } else {
            return !sq.TreatAsSolidFloor() ? sq.HasStairsBelow() : true;
        }
    }

    private void addContextualAction(ContextualAction.Action action, IsoDirections dir, IsoGridSquare square, IsoObject object) {
        ContextualAction newAction = ContextualAction.alloc(action, dir, square, object);
        this.contextualActions.add(newAction);
    }

    private void addContextualAction(ContextualAction.Action in_action, Invokers.Params1.ICallback<ContextualAction> in_populator) {
        ContextualAction newAction = ContextualAction.alloc(in_action, in_populator);
        this.contextualActions.add(newAction);
    }

    private ContextualAction pickBestContextualAction(ArrayList<ContextualAction> contextualActions) {
        ContextualAction best = null;
        ContextualAction bestBehind = null;
        int maxPriority = -1;
        int maxPriorityBehind = -1;

        for (int i = 0; i < contextualActions.size(); i++) {
            ContextualAction ca = contextualActions.get(i);
            if (ca.behind) {
                if (ca.priority > maxPriorityBehind) {
                    bestBehind = ca;
                    maxPriorityBehind = ca.priority;
                }
            } else if (ca.priority > maxPriority) {
                best = ca;
                maxPriority = ca.priority;
            }
        }

        if (best == null) {
            return bestBehind;
        } else {
            return bestBehind != null && bestBehind.priority > best.priority ? bestBehind : best;
        }
    }

    private void performContextualAction(ContextualAction ca) {
        switch (ca.action) {
            case NONE:
            default:
                break;
            case AnimalInteraction:
                this.triggerContextualAction("AnimalsInteraction", ca.object);
                break;
            case ClimbOverFence:
                this.triggerContextualAction("ClimbOverFence", ca.object, ca.dir);
                break;
            case ClimbOverWall:
                this.climbOverWall(ca.dir);
                break;
            case ClimbSheetRope:
                this.triggerContextualAction("ClimbSheetRope", ca.square, false);
                break;
            case ClimbThroughWindow:
                this.triggerContextualAction("ClimbThroughWindow", ca.object);
                break;
            case OpenButcherHook:
                this.triggerContextualAction("OpenButcherHook", ca.object);
                break;
            case OpenHutch:
                this.triggerContextualAction("OpenHutch", ca.object);
                break;
            case RestOnFurniture:
                this.triggerContextualAction("SleepInBed", ca.object);
                break;
            case ThrowGrappledTargetOutWindow:
                this.throwGrappledTargetOutWindow(ca.object);
                break;
            case ThrowGrappledOverFence:
                this.throwGrappledOverFence(ca.object, ca.dir);
                break;
            case ThrowGrappledIntoContainer:
                this.throwGrappledIntoInventory(ca.targetContainer);
                break;
            case ToggleCurtain:
                if (ca.object instanceof IsoCurtain curtain) {
                    this.triggerContextualAction(curtain.IsOpen() ? "CloseCurtain" : "OpenCurtain", curtain);
                } else if (ca.object instanceof IsoDoor door) {
                    door.toggleCurtain();
                }
                break;
            case ToggleDoor:
                if (ca.object instanceof IsoDoor door) {
                    this.triggerContextualAction(door.IsOpen() ? "CloseDoor" : "OpenDoor", door);
                } else if (ca.object instanceof IsoThumpable door) {
                    this.triggerContextualAction(door.IsOpen() ? "CloseDoor" : "OpenDoor", door);
                }
                break;
            case ToggleWindow:
                if (ca.object instanceof IsoWindow window) {
                    this.triggerContextualAction(window.IsOpen() ? "CloseWindow" : "OpenWindow", window);
                }
        }
    }

    public boolean doContext() {
        if (this.isIgnoreContextKey()) {
            return false;
        } else if (this.isBlockMovement()) {
            return false;
        } else {
            if (!this.getCharacterActions().isEmpty()) {
                this.setTimedActionToRetrigger((LuaTimedActionNew)this.getCharacterActions().get(0));
            }

            if (!this.isSittingOnFurniture() && !this.isSitOnGround()) {
                for (int i = 0; i < this.getCell().vehicles.size(); i++) {
                    BaseVehicle vehicle = this.getCell().vehicles.get(i);
                    if (vehicle.getUseablePart(this) != null) {
                        return false;
                    }
                }

                ContextualAction.releaseAll(this.contextualActions);
                float lx = this.getX() - PZMath.fastfloor(this.getX());
                float ly = this.getY() - PZMath.fastfloor(this.getY());
                IsoDirections oppositeDir1 = IsoDirections.Max;
                IsoDirections oppositeDir2 = IsoDirections.Max;
                IsoDirections dir = this.getForwardIsoDirection();
                if (this.current.getAdjacentSquare(dir) == null) {
                    return false;
                } else {
                    if (dir == IsoDirections.NW) {
                        if (ly < lx) {
                            this.doContextNSWE(IsoDirections.N);
                            this.doContextNSWE(IsoDirections.W);
                            oppositeDir1 = IsoDirections.S;
                            oppositeDir2 = IsoDirections.E;
                        } else {
                            this.doContextNSWE(IsoDirections.W);
                            this.doContextNSWE(IsoDirections.N);
                            oppositeDir1 = IsoDirections.E;
                            oppositeDir2 = IsoDirections.S;
                        }
                    } else if (dir == IsoDirections.NE) {
                        lx = 1.0F - lx;
                        if (ly < lx) {
                            this.doContextNSWE(IsoDirections.N);
                            this.doContextNSWE(IsoDirections.E);
                            oppositeDir1 = IsoDirections.S;
                            oppositeDir2 = IsoDirections.W;
                        } else {
                            this.doContextNSWE(IsoDirections.E);
                            this.doContextNSWE(IsoDirections.N);
                            oppositeDir1 = IsoDirections.W;
                            oppositeDir2 = IsoDirections.S;
                        }
                    } else if (dir == IsoDirections.SE) {
                        lx = 1.0F - lx;
                        ly = 1.0F - ly;
                        if (ly < lx) {
                            this.doContextNSWE(IsoDirections.S);
                            this.doContextNSWE(IsoDirections.E);
                            oppositeDir1 = IsoDirections.N;
                            oppositeDir2 = IsoDirections.W;
                        } else {
                            this.doContextNSWE(IsoDirections.E);
                            this.doContextNSWE(IsoDirections.S);
                            oppositeDir1 = IsoDirections.W;
                            oppositeDir2 = IsoDirections.N;
                        }
                    } else if (dir == IsoDirections.SW) {
                        ly = 1.0F - ly;
                        if (ly < lx) {
                            this.doContextNSWE(IsoDirections.S);
                            this.doContextNSWE(IsoDirections.W);
                            oppositeDir1 = IsoDirections.N;
                            oppositeDir2 = IsoDirections.E;
                        } else {
                            this.doContextNSWE(IsoDirections.W);
                            this.doContextNSWE(IsoDirections.S);
                            oppositeDir1 = IsoDirections.E;
                            oppositeDir2 = IsoDirections.N;
                        }
                    } else {
                        this.doContextNSWE(dir);
                        oppositeDir1 = dir.Rot180();
                    }

                    if (this.contextualActions.isEmpty() && dir.dx() != 0 && dir.dy() != 0) {
                        this.doContextCorners(dir);
                    }

                    int count = this.contextualActions.size();
                    if (oppositeDir1 != IsoDirections.Max) {
                        IsoObject obj = this.getContextDoorOrWindowOrWindowFrame(oppositeDir1);
                        if (obj != null) {
                            this.doContextDoorOrWindowOrWindowFrame(oppositeDir1, obj);
                        }
                    }

                    if (oppositeDir2 != IsoDirections.Max) {
                        IsoObject obj = this.getContextDoorOrWindowOrWindowFrame(oppositeDir2);
                        if (obj != null) {
                            this.doContextDoorOrWindowOrWindowFrame(oppositeDir2, obj);
                        }
                    }

                    for (int ix = count; ix < this.contextualActions.size(); ix++) {
                        this.contextualActions.get(ix).behind = true;
                    }

                    if (this.contextualActions.isEmpty()) {
                        LuaEventManager.triggerEvent("OnContextKey", this, BoxedStaticValues.toDouble(PZMath.fastfloor(this.timePressedContext * 1000.0F)));
                        return false;
                    } else {
                        ContextualAction ca = this.pickBestContextualAction(this.contextualActions);
                        this.performContextualAction(ca);
                        return true;
                    }
                }
            } else {
                LuaEventManager.triggerEvent("OnContextKey", this, BoxedStaticValues.toDouble(PZMath.fastfloor(this.timePressedContext * 1000.0F)));
                return true;
            }
        }
    }

    private boolean doContextNSWE(IsoDirections assumedDir) {
        assert assumedDir == IsoDirections.N || assumedDir == IsoDirections.S || assumedDir == IsoDirections.W || assumedDir == IsoDirections.E;

        if (this.current == null || this.current.getAdjacentSquare(assumedDir) == null) {
            return false;
        } else if (this.doContextClimbSheetRope(assumedDir)) {
            return true;
        } else {
            IsoObject o = this.getContextDoorOrWindowOrWindowFrame(assumedDir);
            if (o != null) {
                this.doContextDoorOrWindowOrWindowFrame(assumedDir, o);
                return true;
            } else {
                if (!GameKeyboard.isKeyDown(42)) {
                    if (this.doContextHutch(assumedDir)) {
                        return true;
                    }

                    if (this.doContextButcherHook(assumedDir)) {
                        return true;
                    }

                    if (this.doContextAnimalInteraction(assumedDir)) {
                        return true;
                    }

                    if (this.doContextRestOnFurniture(assumedDir)) {
                        return true;
                    }
                }

                if (GameKeyboard.isKeyDown(42) && this.current != null && this.ticksSincePressedMovement > 15.0F) {
                    if (this.doContextHutch(assumedDir)) {
                        return true;
                    }

                    if (this.doContextToggleCurtain(assumedDir)) {
                        return true;
                    }
                }

                if (this.doContextThrowGrappledTargetOverFence(assumedDir)) {
                    return true;
                } else if (this.doContextThrowGrappledTargetIntoInventory(assumedDir)) {
                    return true;
                } else {
                    return this.doContextHopOverFence(assumedDir) ? true : this.doContextClimbOverWall(assumedDir);
                }
            }
        }
    }

    private boolean doContextRestOnFurniture(IsoDirections assumedDir) {
        if (this.isGrappling()) {
            return false;
        } else {
            IsoGridSquare adjacent = this.current.getAdjacentSquare(assumedDir);
            IsoObject bed = adjacent.getBed();
            if (bed == null) {
                bed = this.current.getBed();
            }

            if (bed != null) {
                this.addContextualAction(ContextualAction.Action.RestOnFurniture, assumedDir, bed.getSquare(), bed);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean doContextAnimalInteraction(IsoDirections assumedDir) {
        if (this.isGrappling()) {
            return false;
        } else {
            IsoGridSquare adjacent = this.current.getAdjacentSquare(assumedDir);
            ArrayList<IsoAnimal> animals = adjacent.getAnimals();
            if (animals.isEmpty()) {
                animals = this.current.getAnimals();
            }

            if (!animals.isEmpty()) {
                this.addContextualAction(ContextualAction.Action.AnimalInteraction, assumedDir, animals.get(0).getCurrentSquare(), animals.get(0));
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean doContextButcherHook(IsoDirections assumedDir) {
        if (this.isGrappling()) {
            return false;
        } else {
            IsoGridSquare adjacent = this.current.getAdjacentSquare(assumedDir);
            IsoButcherHook hook = adjacent.getButcherHook();
            if (hook == null) {
                hook = this.current.getButcherHook();
            }

            if (hook != null) {
                this.addContextualAction(ContextualAction.Action.OpenButcherHook, assumedDir, hook.getSquare(), hook);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean doContextHutch(IsoDirections assumedDir) {
        if (this.isGrappling()) {
            return false;
        } else {
            IsoGridSquare adjacent = this.current.getAdjacentSquare(assumedDir);
            IsoHutch hutch = adjacent.getHutch();
            if (hutch == null) {
                hutch = this.current.getHutch();
            }

            if (hutch != null) {
                this.addContextualAction(ContextualAction.Action.OpenHutch, assumedDir, hutch.getSquare(), hutch);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean doContextToggleCurtain(IsoDirections assumedDir) {
        if (this.isGrappling()) {
            return false;
        } else {
            IsoObject doorN = this.current.getDoor(true);
            if (doorN instanceof IsoDoor isoDoor && isoDoor.isFacingSheet(this)) {
                this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, doorN.getSquare(), doorN);
                return true;
            } else {
                IsoObject doorW = this.current.getDoor(false);
                if (doorW instanceof IsoDoor door && door.isFacingSheet(this)) {
                    this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, doorW.getSquare(), doorW);
                    return true;
                } else {
                    if (assumedDir == IsoDirections.E) {
                        IsoGridSquare checkForDoorsAndWindows1 = IsoWorld.instance
                            .currentCell
                            .getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ());
                        IsoObject doorE = checkForDoorsAndWindows1 != null ? checkForDoorsAndWindows1.getDoor(true) : null;
                        if (doorE instanceof IsoDoor isoDoor && isoDoor.isFacingSheet(this)) {
                            this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, doorE.getSquare(), doorE);
                            return true;
                        }
                    }

                    if (assumedDir == IsoDirections.S) {
                        IsoGridSquare checkForDoorsAndWindows1 = IsoWorld.instance
                            .currentCell
                            .getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ());
                        IsoObject doorS = checkForDoorsAndWindows1 != null ? checkForDoorsAndWindows1.getDoor(false) : null;
                        if (doorS instanceof IsoDoor isoDoor && isoDoor.isFacingSheet(this)) {
                            this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, doorS.getSquare(), doorS);
                            return true;
                        }
                    }

                    return false;
                }
            }
        }
    }

    private boolean doContextClimbSheetRope(IsoDirections assumedDir) {
        if (this.isGrappling()) {
            return false;
        } else if (assumedDir == IsoDirections.N && this.current.has(IsoFlagType.climbSheetN) && this.canClimbSheetRope(this.current)) {
            this.addContextualAction(ContextualAction.Action.ClimbSheetRope, assumedDir, this.current, null);
            return true;
        } else if (assumedDir == IsoDirections.S && this.current.has(IsoFlagType.climbSheetS) && this.canClimbSheetRope(this.current)) {
            this.addContextualAction(ContextualAction.Action.ClimbSheetRope, assumedDir, this.current, null);
            return true;
        } else if (assumedDir == IsoDirections.W && this.current.has(IsoFlagType.climbSheetW) && this.canClimbSheetRope(this.current)) {
            this.addContextualAction(ContextualAction.Action.ClimbSheetRope, assumedDir, this.current, null);
            return true;
        } else if (assumedDir == IsoDirections.E && this.current.has(IsoFlagType.climbSheetE) && this.canClimbSheetRope(this.current)) {
            this.addContextualAction(ContextualAction.Action.ClimbSheetRope, assumedDir, this.current, null);
            return true;
        } else {
            return false;
        }
    }

    private boolean doContextHopOverFence(IsoDirections assumedDir) {
        if (this.ignoreAutoVault) {
            return false;
        } else if (this.isGrappling()) {
            return false;
        } else {
            boolean bSafe = this.isSafeToClimbOver(assumedDir);
            if (this.timePressedContext < 0.5F && !bSafe) {
                return false;
            } else if (assumedDir == IsoDirections.N && this.getCurrentSquare().has(IsoFlagType.HoppableN)) {
                this.addContextualAction(ContextualAction.Action.ClimbOverFence, assumedDir, this.getCurrentSquare(), this.getCurrentSquare().getHoppable(true));
                return true;
            } else if (assumedDir == IsoDirections.W && this.getCurrentSquare().has(IsoFlagType.HoppableW)) {
                this.addContextualAction(
                    ContextualAction.Action.ClimbOverFence, assumedDir, this.getCurrentSquare(), this.getCurrentSquare().getHoppable(false)
                );
                return true;
            } else {
                IsoGridSquare squareS = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
                if (assumedDir == IsoDirections.S && squareS != null && squareS.has(IsoFlagType.HoppableN)) {
                    this.addContextualAction(ContextualAction.Action.ClimbOverFence, assumedDir, squareS, squareS.getHoppable(true));
                    return true;
                } else {
                    IsoGridSquare squareE = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
                    if (assumedDir == IsoDirections.E && squareE != null && squareE.has(IsoFlagType.HoppableW)) {
                        this.addContextualAction(ContextualAction.Action.ClimbOverFence, assumedDir, squareE, squareE.getHoppable(false));
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    private boolean doContextThrowGrappledTargetOverFence(IsoDirections assumedDir) {
        if (!this.isGrappling()) {
            return false;
        } else {
            boolean bSafe = this.isSafeToClimbOver(assumedDir);
            if (this.timePressedContext < 0.5F && !bSafe) {
                return false;
            } else if (assumedDir == IsoDirections.S && this.getCurrentSquare().has(IsoFlagType.HoppableN)) {
                return this.doContextThrowGrappledTargetOverFence(this.getCurrentSquare(), assumedDir, this.getCurrentSquare().getHoppable(true));
            } else if (assumedDir == IsoDirections.E && this.getCurrentSquare().has(IsoFlagType.HoppableW)) {
                return this.doContextThrowGrappledTargetOverFence(this.getCurrentSquare(), assumedDir, this.getCurrentSquare().getHoppable(false));
            } else {
                IsoGridSquare squareS = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
                if (assumedDir == IsoDirections.N && squareS != null && squareS.has(IsoFlagType.HoppableN)) {
                    return this.doContextThrowGrappledTargetOverFence(squareS, assumedDir, squareS.getHoppable(true));
                } else {
                    IsoGridSquare squareE = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
                    return assumedDir == IsoDirections.W && squareE != null && squareE.has(IsoFlagType.HoppableW)
                        ? this.doContextThrowGrappledTargetOverFence(squareE, assumedDir, squareE.getHoppable(false))
                        : false;
                }
            }
        }
    }

    private boolean doContextCorners(IsoDirections assumedDir) {
        if (!GameKeyboard.isKeyDown(42)) {
            if (this.doContextHutch(assumedDir)) {
                return true;
            }

            if (this.doContextButcherHook(assumedDir)) {
                return true;
            }

            if (this.doContextAnimalInteraction(assumedDir)) {
                return true;
            }

            if (this.doContextRestOnFurniture(assumedDir)) {
                return true;
            }
        }

        return GameKeyboard.isKeyDown(42) && this.current != null && this.ticksSincePressedMovement > 15.0F && this.doContextHutch(assumedDir);
    }

    public IsoObject getContextDoorOrWindowOrWindowFrame(IsoDirections assumedDir) {
        if (this.current != null && assumedDir != null) {
            IsoGridSquare adjacent = this.current.getAdjacentSquare(assumedDir);
            IsoObject obj = null;
            switch (assumedDir) {
                case N:
                    obj = this.current.getOpenDoor(assumedDir);
                    if (obj != null) {
                        return obj;
                    }

                    obj = this.current.getDoorOrWindowOrWindowFrame(assumedDir, true);
                    if (obj != null) {
                        return obj;
                    }

                    obj = this.current.getDoor(true);
                    if (obj != null) {
                        return obj;
                    }

                    if (adjacent != null && !this.current.isBlockedTo(adjacent)) {
                        obj = adjacent.getOpenDoor(IsoDirections.S);
                    }
                case NW:
                case SW:
                case SE:
                default:
                    break;
                case W:
                    obj = this.current.getOpenDoor(assumedDir);
                    if (obj != null) {
                        return obj;
                    }

                    obj = this.current.getDoorOrWindowOrWindowFrame(assumedDir, true);
                    if (obj != null) {
                        return obj;
                    }

                    obj = this.current.getDoor(false);
                    if (obj != null) {
                        return obj;
                    }

                    if (adjacent != null && !this.current.isBlockedTo(adjacent)) {
                        obj = adjacent.getOpenDoor(IsoDirections.E);
                    }
                    break;
                case S:
                    obj = this.current.getOpenDoor(assumedDir);
                    if (obj != null) {
                        return obj;
                    }

                    if (adjacent != null) {
                        boolean ignoreOpen = this.current.isBlockedTo(adjacent);
                        obj = adjacent.getDoorOrWindowOrWindowFrame(IsoDirections.N, ignoreOpen);
                        if (obj != null) {
                            return obj;
                        }

                        obj = adjacent.getDoor(true);
                    }
                    break;
                case E:
                    obj = this.current.getOpenDoor(assumedDir);
                    if (obj != null) {
                        return obj;
                    }

                    if (adjacent != null) {
                        boolean ignoreOpen1 = this.current.isBlockedTo(adjacent);
                        obj = adjacent.getDoorOrWindowOrWindowFrame(IsoDirections.W, ignoreOpen1);
                        if (obj != null) {
                            return obj;
                        }

                        obj = adjacent.getDoor(false);
                    }
            }

            return obj;
        } else {
            return null;
        }
    }

    private boolean doContextDoorOrWindowOrWindowFrame(IsoDirections assumedDir, IsoObject o) {
        IsoGridSquare adjacent = this.current.getAdjacentSquare(assumedDir);
        boolean bTopOfSheetRope = IsoWindow.isTopOfSheetRopeHere(adjacent) && this.canClimbDownSheetRope(adjacent);
        if (o instanceof IsoDoor isoDoor) {
            return this.doContextDoor(assumedDir, isoDoor);
        } else if (o instanceof IsoThumpable thumpable && thumpable.isDoor()) {
            return this.doContextThumpableDoor(assumedDir, thumpable);
        } else if (o instanceof IsoWindow isoWindow && !o.getSquare().getProperties().has(IsoFlagType.makeWindowInvincible)) {
            return this.doContextWindow(assumedDir, isoWindow, bTopOfSheetRope);
        } else if (o instanceof IsoThumpable isoThumpable && !o.getSquare().getProperties().has(IsoFlagType.makeWindowInvincible)) {
            return this.doContextThumpableWindow(assumedDir, isoThumpable, bTopOfSheetRope);
        } else {
            return o instanceof IsoWindowFrame windowFrame ? this.doContextWindowFrame(assumedDir, windowFrame, bTopOfSheetRope) : false;
        }
    }

    private boolean doContextWindowFrame(IsoDirections assumedDir, IsoWindowFrame o, boolean bTopOfSheetRope) {
        if (GameKeyboard.isKeyDown(42)) {
            if (!this.isGrappling()) {
                IsoCurtain curtain = o.getCurtain();
                if (curtain != null && this.current != null && !curtain.getSquare().isBlockedTo(this.current)) {
                    this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, curtain.getSquare(), curtain);
                    return true;
                }
            }
        } else if ((this.timePressedContext >= 0.5F || this.isSafeToClimbOver(assumedDir) || bTopOfSheetRope) && o.canClimbThrough(this)) {
            if (this.doContextThrowGrappledTargetOutWindow(assumedDir, o)) {
                return true;
            }

            if (!this.isGrappling()) {
                this.addContextualAction(ContextualAction.Action.ClimbThroughWindow, assumedDir, o.getSquare(), o);
                return true;
            }
        }

        return false;
    }

    private boolean doContextThumpableWindow(IsoDirections assumedDir, IsoThumpable d, boolean bTopOfSheetRope) {
        if (GameKeyboard.isKeyDown(42)) {
            if (!this.isGrappling()) {
                IsoCurtain curtain = d.HasCurtains();
                if (curtain != null && this.current != null && !curtain.getSquare().isBlockedTo(this.current)) {
                    this.triggerContextualAction(curtain.IsOpen() ? "CloseCurtain" : "OpenCurtain", curtain);
                    return true;
                }
            }
        } else if (this.timePressedContext >= 0.5F) {
            if (d.canClimbThrough(this)) {
                if (this.doContextThrowGrappledTargetOutWindow(assumedDir, d)) {
                    return true;
                }

                if (!this.isGrappling()) {
                    this.addContextualAction(ContextualAction.Action.ClimbThroughWindow, assumedDir, d.getSquare(), d);
                    return true;
                }
            }
        } else if (d.canClimbThrough(this)) {
            if (this.doContextThrowGrappledTargetOutWindow(assumedDir, d)) {
                return true;
            }

            if (!this.isGrappling() && (this.isSafeToClimbOver(assumedDir) || d.getSquare().haveSheetRope || bTopOfSheetRope)) {
                this.addContextualAction(ContextualAction.Action.ClimbThroughWindow, assumedDir, d.getSquare(), d);
                return true;
            }
        }

        return false;
    }

    private boolean doContextWindow(IsoDirections assumedDir, IsoWindow d, boolean bTopOfSheetRope) {
        if (GameKeyboard.isKeyDown(42)) {
            IsoCurtain curtain = d.HasCurtains();
            if (curtain != null && this.current != null && !curtain.getSquare().isBlockedTo(this.current) && !this.isGrappling()) {
                this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, curtain.getSquare(), curtain);
                return true;
            }
        } else if (this.timePressedContext >= 0.5F) {
            if (d.canClimbThrough(this)) {
                if (this.doContextThrowGrappledTargetOutWindow(assumedDir, d)) {
                    return true;
                }

                if (!this.isGrappling()) {
                    this.addContextualAction(ContextualAction.Action.ClimbThroughWindow, assumedDir, d.getSquare(), d);
                    return true;
                }
            } else if (!d.isPermaLocked() && !d.isBarricaded() && !d.IsOpen() && !d.isDestroyed() && !this.isGrappling()) {
                this.addContextualAction(ContextualAction.Action.ToggleWindow, assumedDir, d.getSquare(), d);
                return true;
            }
        } else if (d.getHealth() > 0 && !d.isDestroyed()) {
            IsoBarricade barricade = d.getBarricadeForCharacter(this);
            if (!d.IsOpen() && barricade == null) {
                if (!this.isGrappling()) {
                    this.addContextualAction(ContextualAction.Action.ToggleWindow, assumedDir, d.getSquare(), d);
                    return true;
                }
            } else if (barricade == null) {
                if (this.doContextThrowGrappledTargetOutWindow(assumedDir, d)) {
                    return true;
                }

                if (!this.isGrappling()) {
                    this.addContextualAction(ContextualAction.Action.ToggleWindow, assumedDir, d.getSquare(), d);
                    return true;
                }
            }
        } else if (d.isGlassRemoved() && !d.isBarricaded()) {
            if (this.doContextThrowGrappledTargetOutWindow(assumedDir, d)) {
                return true;
            }

            if (!this.isGrappling() && (this.isSafeToClimbOver(assumedDir) || d.getSquare().haveSheetRope || bTopOfSheetRope)) {
                this.addContextualAction(ContextualAction.Action.ClimbThroughWindow, assumedDir, d.getSquare(), d);
                return true;
            }
        }

        return false;
    }

    private boolean doContextThrowGrappledTargetOutWindow(IsoDirections dir, IsoObject windowObject) {
        DebugType.Grapple.trace("Attempting to throw out window: %s", windowObject);
        if (!this.isGrappling()) {
            DebugLog.Grapple.trace("Not currently grappling anything. Nothing to throw out the window, windowObject:%s", windowObject);
            return false;
        } else if (this.getGrapplingTarget() instanceof IsoGameCharacter thrownCharacter) {
            ClimbThroughWindowPositioningParams params = ClimbThroughWindowPositioningParams.alloc();
            ClimbThroughWindowState.getClimbThroughWindowPositioningParams(this, windowObject, params);
            if (!params.canClimb) {
                DebugType.Grapple.trace("Cannot climb through, cannot throw out through.");
                params.release();
                return false;
            } else {
                this.addContextualAction(ContextualAction.Action.ThrowGrappledTargetOutWindow, dir, windowObject.getSquare(), windowObject);
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean doContextThrowGrappledTargetOverFence(IsoGridSquare square, IsoDirections dir, IsoObject hoppable) {
        DebugType.Grapple.trace("Attempting to throw over fence: %s", hoppable);
        if (!this.isGrappling()) {
            DebugType.Grapple.trace("Not currently grappling anything. Nothing to throw over fence: %s", hoppable);
            return false;
        } else if (this.getGrapplingTarget() instanceof IsoGameCharacter thrownCharacter) {
            this.addContextualAction(ContextualAction.Action.ThrowGrappledOverFence, dir, square, hoppable);
            return true;
        } else {
            return false;
        }
    }

    private boolean doContextThrowGrappledTargetIntoInventory(IsoDirections dir) {
        ItemContainer targetContainer = null;
        return this.doContextThrowGrappledTargetIntoInventory(targetContainer);
    }

    private boolean doContextThrowGrappledTargetIntoInventory(ItemContainer targetContainer) {
        DebugType.Grapple.trace("Attempting to throw into inventory: %s", targetContainer);
        if (targetContainer == null) {
            DebugType.Grapple.trace("No inventory.");
            return false;
        } else if (!this.isGrappling()) {
            DebugType.Grapple.trace("Not currently grappling anything. Nothing to throw into inventory: %s", targetContainer);
            return false;
        } else if (this.getGrapplingTarget() instanceof IsoGameCharacter thrownCharacter) {
            this.addContextualAction(ContextualAction.Action.ThrowGrappledIntoContainer, newAction -> {
                newAction.square = this.square;
                newAction.targetContainer = targetContainer;
            });
            return true;
        } else {
            return false;
        }
    }

    private boolean doContextThumpableDoor(IsoDirections assumedDir, IsoThumpable d) {
        if (this.timePressedContext >= 0.5F) {
            if (d.isHoppable() && !this.isIgnoreAutoVault()) {
                if (this.doContextThrowGrappledTargetOverFence(d.getSquare(), assumedDir, d)) {
                    return true;
                } else if (!this.isGrappling()) {
                    this.addContextualAction(ContextualAction.Action.ClimbOverFence, assumedDir, d.getSquare(), d);
                    return true;
                } else {
                    return false;
                }
            } else {
                this.addContextualAction(ContextualAction.Action.ToggleDoor, assumedDir, d.getSquare(), d);
                return true;
            }
        } else {
            this.addContextualAction(ContextualAction.Action.ToggleDoor, assumedDir, d.getSquare(), d);
            return true;
        }
    }

    private boolean doContextDoor(IsoDirections assumedDir, IsoDoor d) {
        if (GameKeyboard.isKeyDown(42) && d.HasCurtains() != null && d.isFacingSheet(this) && this.ticksSincePressedMovement > 15.0F) {
            this.addContextualAction(ContextualAction.Action.ToggleCurtain, assumedDir, d.getSquare(), d);
            return true;
        } else if (this.timePressedContext >= 0.5F) {
            if (d.isHoppable() && !this.isIgnoreAutoVault()) {
                if (this.doContextThrowGrappledTargetOverFence(d.getSquare(), assumedDir, d)) {
                    return true;
                } else if (!this.isGrappling()) {
                    this.addContextualAction(ContextualAction.Action.ClimbOverFence, assumedDir, d.getSquare(), d);
                    return true;
                } else {
                    return false;
                }
            } else {
                this.addContextualAction(ContextualAction.Action.ToggleDoor, assumedDir, d.getSquare(), d);
                return true;
            }
        } else {
            this.addContextualAction(ContextualAction.Action.ToggleDoor, assumedDir, d.getSquare(), d);
            return true;
        }
    }

    public boolean hopFence(IsoDirections dir, boolean bTest) {
        float lx = this.getX() - PZMath.fastfloor(this.getX());
        float ly = this.getY() - PZMath.fastfloor(this.getY());
        if (dir == IsoDirections.NW) {
            if (ly < lx) {
                return this.hopFence(IsoDirections.N, bTest) ? true : this.hopFence(IsoDirections.W, bTest);
            } else {
                return this.hopFence(IsoDirections.W, bTest) ? true : this.hopFence(IsoDirections.N, bTest);
            }
        } else if (dir == IsoDirections.NE) {
            lx = 1.0F - lx;
            if (ly < lx) {
                return this.hopFence(IsoDirections.N, bTest) ? true : this.hopFence(IsoDirections.E, bTest);
            } else {
                return this.hopFence(IsoDirections.E, bTest) ? true : this.hopFence(IsoDirections.N, bTest);
            }
        } else if (dir == IsoDirections.SE) {
            lx = 1.0F - lx;
            ly = 1.0F - ly;
            if (ly < lx) {
                return this.hopFence(IsoDirections.S, bTest) ? true : this.hopFence(IsoDirections.E, bTest);
            } else {
                return this.hopFence(IsoDirections.E, bTest) ? true : this.hopFence(IsoDirections.S, bTest);
            }
        } else if (dir == IsoDirections.SW) {
            ly = 1.0F - ly;
            if (ly < lx) {
                return this.hopFence(IsoDirections.S, bTest) ? true : this.hopFence(IsoDirections.W, bTest);
            } else {
                return this.hopFence(IsoDirections.W, bTest) ? true : this.hopFence(IsoDirections.S, bTest);
            }
        } else if (this.current == null) {
            return false;
        } else {
            IsoGridSquare square1 = this.current.getAdjacentSquare(dir);
            if (square1 != null && !square1.has(IsoFlagType.water)) {
                if (dir == IsoDirections.N && this.getCurrentSquare().has(IsoFlagType.HoppableN)) {
                    if (bTest) {
                        return true;
                    } else {
                        this.triggerContextualAction("ClimbOverFence", this.getCurrentSquare().getHoppable(true), dir);
                        return true;
                    }
                } else if (dir != IsoDirections.W || !this.getCurrentSquare().has(IsoFlagType.HoppableW)) {
                    IsoGridSquare squareS = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
                    if (dir != IsoDirections.S || squareS == null || !squareS.has(IsoFlagType.HoppableN)) {
                        IsoGridSquare squareE = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
                        if (dir != IsoDirections.E || squareE == null || !squareE.has(IsoFlagType.HoppableW)) {
                            return false;
                        } else if (bTest) {
                            return true;
                        } else {
                            this.triggerContextualAction("ClimbOverFence", squareE.getHoppable(false), dir);
                            return true;
                        }
                    } else if (bTest) {
                        return true;
                    } else {
                        this.triggerContextualAction("ClimbOverFence", squareS.getHoppable(true), dir);
                        return true;
                    }
                } else if (bTest) {
                    return true;
                } else {
                    this.triggerContextualAction("ClimbOverFence", this.getCurrentSquare().getHoppable(false), dir);
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public boolean canClimbOverWall(IsoDirections dir) {
        if (this.isSprinting()) {
            return false;
        } else if (!this.isSafeToClimbOver(dir) || this.current == null) {
            return false;
        } else if (this.current.haveRoof) {
            return false;
        } else if (this.current.getBuilding() != null) {
            return false;
        } else {
            IsoGridSquare above = IsoWorld.instance.currentCell.getGridSquare(this.current.x, this.current.y, this.current.z + 1);
            if (above != null && above.HasSlopedRoof() && !above.HasEave()) {
                return false;
            } else {
                IsoGridSquare square = this.current.getAdjacentSquare(dir);
                if (square.haveRoof) {
                    return false;
                } else if (square.isSolid() || square.isSolidTrans()) {
                    return false;
                } else if (square.getBuilding() != null) {
                    return false;
                } else {
                    IsoGridSquare above2 = IsoWorld.instance.currentCell.getGridSquare(square.x, square.y, square.z + 1);
                    if (above2 != null && above2.HasSlopedRoof() && !above2.HasEave()) {
                        return false;
                    } else {
                        switch (dir) {
                            case N:
                                if (this.current.has(IsoFlagType.CantClimb)) {
                                    return false;
                                }

                                if (!this.current.has(IsoObjectType.wall)) {
                                    return false;
                                }

                                if (!this.current.has(IsoFlagType.collideN)) {
                                    return false;
                                }

                                if (this.current.has(IsoFlagType.HoppableN)) {
                                    return false;
                                }

                                if (above != null && above.has(IsoFlagType.collideN)) {
                                    return false;
                                }
                                break;
                            case NW:
                            case SW:
                            case SE:
                            default:
                                return false;
                            case W:
                                if (this.current.has(IsoFlagType.CantClimb)) {
                                    return false;
                                }

                                if (!this.current.has(IsoObjectType.wall)) {
                                    return false;
                                }

                                if (!this.current.has(IsoFlagType.collideW)) {
                                    return false;
                                }

                                if (this.current.has(IsoFlagType.HoppableW)) {
                                    return false;
                                }

                                if (above != null && above.has(IsoFlagType.collideW)) {
                                    return false;
                                }
                                break;
                            case S:
                                if (square.has(IsoFlagType.CantClimb)) {
                                    return false;
                                }

                                if (!square.has(IsoObjectType.wall)) {
                                    return false;
                                }

                                if (!square.has(IsoFlagType.collideN)) {
                                    return false;
                                }

                                if (square.has(IsoFlagType.HoppableN)) {
                                    return false;
                                }

                                if (above2 != null && above2.has(IsoFlagType.collideN)) {
                                    return false;
                                }
                                break;
                            case E:
                                if (square.has(IsoFlagType.CantClimb)) {
                                    return false;
                                }

                                if (!square.has(IsoObjectType.wall)) {
                                    return false;
                                }

                                if (!square.has(IsoFlagType.collideW)) {
                                    return false;
                                }

                                if (square.has(IsoFlagType.HoppableW)) {
                                    return false;
                                }

                                if (above2 != null && above2.has(IsoFlagType.collideW)) {
                                    return false;
                                }
                        }

                        return IsoWindow.canClimbThroughHelper(this, this.current, square, dir == IsoDirections.N || dir == IsoDirections.S);
                    }
                }
            }
        }
    }

    public boolean doContextClimbOverWall(IsoDirections dir) {
        if (this.ignoreAutoVault) {
            return false;
        } else if (this.isGrappling()) {
            return false;
        } else {
            boolean bSafe = this.isSafeToClimbOver(dir);
            if (this.timePressedContext < 0.5F && !bSafe) {
                return false;
            } else if (this.canClimbOverWall(dir)) {
                this.addContextualAction(ContextualAction.Action.ClimbOverWall, dir, null, null);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean climbOverWall(IsoDirections dir) {
        if (!this.canClimbOverWall(dir)) {
            return false;
        } else {
            this.dropHeavyItems();
            ClimbOverWallState.instance().setParams(this, dir);
            this.getActionContext().reportEvent("EventClimbWall");
            return true;
        }
    }

    private void updateSleepingPillsTaken() {
        if (this.getSleepingPillsTaken() > 0 && this.lastPillsTaken > 0L && GameTime.instance.calender.getTimeInMillis() - this.lastPillsTaken > 7200000L) {
            this.setSleepingPillsTaken(this.getSleepingPillsTaken() - 1);
        }
    }

    public boolean AttemptAttack() {
        return this.DoAttack(this.useChargeTime);
    }

    @Override
    public boolean DoAttack(float chargeDelta) {
        return this.DoAttack(chargeDelta, null);
    }

    public boolean DoAttack(float chargeDelta, String clickSound) {
        if (!this.isAuthorizedHandToHandAction()) {
            return false;
        } else {
            this.setClickSound(clickSound);
            this.pressedAttack();
            return false;
        }
    }

    public int getPlayerNum() {
        return this.playerIndex;
    }

    public void updateLOS() {
        this.spottedList.clear();
        this.stats.numVisibleZombies = 0;
        this.stats.setLastNumberChasingZombies(this.stats.numChasingZombies);
        this.stats.numChasingZombies = 0;
        this.stats.musicZombiesTargetingDistantNotMoving = 0;
        this.stats.musicZombiesTargetingNearbyNotMoving = 0;
        this.stats.musicZombiesTargetingDistantMoving = 0;
        this.stats.musicZombiesTargetingNearbyMoving = 0;
        this.stats.musicZombiesVisible = 0;
        this.numSurvivorsInVicinity = 0;
        if (this.getCurrentSquare() != null) {
            boolean bServer = GameServer.server;
            boolean bClient = GameClient.client;
            int playerIndex = this.playerIndex;
            IsoPlayer isoPlayer = getInstance();
            float locX = this.getX();
            float locY = this.getY();
            float locZ = this.getZ();
            int close = 0;
            int vclose = 0;
            int numObjects = this.getCell().getObjectList().size();

            for (int n = 0; n < numObjects; n++) {
                IsoMovingObject movingObject = this.getCell().getObjectList().get(n);
                if (!(movingObject instanceof IsoPhysicsObject) && !(movingObject instanceof BaseVehicle)) {
                    if (movingObject == this) {
                        this.spottedList.add(movingObject);
                    } else {
                        float movingObjectX = movingObject.getX();
                        float movingObjectY = movingObject.getY();
                        float movingObjectZ = movingObject.getZ();
                        float distanceToMovingObject = IsoUtils.DistanceTo(movingObjectX, movingObjectY, locX, locY);
                        if (distanceToMovingObject < 20.0F) {
                            close++;
                        }

                        IsoGridSquare chrCurrentSquare = movingObject.getCurrentSquare();
                        if (chrCurrentSquare != null) {
                            if (this.isSeeEveryone()) {
                                movingObject.setAlphaAndTarget(playerIndex, 1.0F);
                            }

                            IsoGameCharacter movingCharacter = Type.tryCastTo(movingObject, IsoGameCharacter.class);
                            IsoPlayer movingPlayer = Type.tryCastTo(movingCharacter, IsoPlayer.class);
                            IsoZombie movingZombie = Type.tryCastTo(movingCharacter, IsoZombie.class);
                            if (movingZombie != null && movingZombie.isReanimatedForGrappleOnly()) {
                                IsoMovingObject grappledBy = Type.tryCastTo(movingZombie.getGrappledBy(), IsoMovingObject.class);
                                movingObject.setAlphaAndTarget(grappledBy == null ? 1.0F : grappledBy.getTargetAlpha());
                            } else if (GameClient.client
                                && isoPlayer != null
                                && movingObject != isoPlayer
                                && movingCharacter != null
                                && movingCharacter.isInvisible()
                                && !isoPlayer.role.hasCapability(Capability.SeesInvisiblePlayers)) {
                                movingCharacter.setAlphaAndTarget(playerIndex, 0.0F);
                            } else {
                                boolean couldSee;
                                if (bServer) {
                                    couldSee = ServerLOS.instance.isCouldSee(this, chrCurrentSquare);
                                } else {
                                    couldSee = chrCurrentSquare.isCouldSee(playerIndex);
                                    if (!couldSee && movingZombie != null && movingZombie.couldSeeHeadSquare(this)) {
                                        couldSee = true;
                                    }
                                }

                                boolean canSee;
                                if (bClient && movingPlayer != null && chrCurrentSquare.getCanSee(playerIndex)) {
                                    canSee = true;
                                } else if (!bServer) {
                                    canSee = chrCurrentSquare.isCanSee(playerIndex);
                                    if (!canSee && movingZombie != null && movingZombie.canSeeHeadSquare(this)) {
                                        canSee = true;
                                    }
                                } else {
                                    canSee = couldSee;
                                }

                                if (!this.isAsleep() && (canSee || distanceToMovingObject < this.getSeeNearbyCharacterDistance() && couldSee)) {
                                    this.TestZombieSpotPlayer(movingObject);
                                    if (movingCharacter != null && movingCharacter.isVisibleToPlayer[playerIndex]) {
                                        if (movingCharacter instanceof IsoSurvivor) {
                                            this.numSurvivorsInVicinity++;
                                        }

                                        if (movingZombie != null) {
                                            this.lastSeenZombieTime = 0.0;
                                            if (movingObjectZ >= locZ - 1.0F
                                                && distanceToMovingObject < 7.0F
                                                && !movingZombie.ghost
                                                && !movingZombie.isFakeDead()
                                                && chrCurrentSquare.getRoom() == this.getCurrentSquare().getRoom()) {
                                                this.ticksSinceSeenZombie = 0;
                                                this.stats.numVisibleZombies++;
                                            }

                                            if (distanceToMovingObject < 3.0F) {
                                                vclose++;
                                            }

                                            if (!movingZombie.isSceneCulled()) {
                                                this.stats.musicZombiesVisible++;
                                                if (movingZombie.target == this) {
                                                    if (!movingZombie.isCurrentState(WalkTowardState.instance())
                                                        && !movingZombie.isCurrentState(LungeState.instance())
                                                        && !movingZombie.isCurrentState(PathFindState.instance())) {
                                                        if (this.DistToProper(movingZombie) >= 10.0F) {
                                                            this.stats.musicZombiesTargetingDistantNotMoving++;
                                                        } else {
                                                            this.stats.musicZombiesTargetingNearbyNotMoving++;
                                                        }
                                                    } else if (this.DistToProper(movingZombie) >= 10.0F) {
                                                        this.stats.musicZombiesTargetingDistantMoving++;
                                                    } else {
                                                        this.stats.musicZombiesTargetingNearbyMoving++;
                                                    }
                                                }
                                            }
                                        }

                                        this.spottedList.add(movingCharacter);
                                        if (!(movingPlayer instanceof IsoPlayer) && !this.remote) {
                                            if (movingPlayer != null && movingPlayer != isoPlayer) {
                                                movingPlayer.setTargetAlpha(playerIndex, 1.0F);
                                            } else {
                                                movingCharacter.setTargetAlpha(playerIndex, 1.0F);
                                            }
                                        }

                                        float maxdist = 4.0F;
                                        if (this.stats.numVisibleZombies > 4) {
                                            maxdist = 7.0F;
                                        }

                                        if (distanceToMovingObject < maxdist
                                            && movingCharacter instanceof IsoZombie
                                            && PZMath.fastfloor(movingObjectZ) == PZMath.fastfloor(locZ)
                                            && !this.isGhostMode()
                                            && !bClient) {
                                            GameTime.instance.setMultiplier(1.0F);
                                            if (!bServer) {
                                                UIManager.getSpeedControls().SetCurrentGameSpeed(1);
                                            }
                                        }

                                        if (distanceToMovingObject < maxdist
                                            && movingCharacter instanceof IsoZombie
                                            && PZMath.fastfloor(movingObjectZ) == PZMath.fastfloor(locZ)
                                            && !this.lastSpotted.contains(movingCharacter)) {
                                            this.stats.numVisibleZombies += 2;
                                        }
                                    }
                                } else {
                                    if (movingObject != instance) {
                                        movingObject.setTargetAlpha(playerIndex, 0.0F);
                                    }

                                    if (couldSee) {
                                        this.TestZombieSpotPlayer(movingObject);
                                    }
                                }

                                if (distanceToMovingObject < 2.0F && movingObject.getTargetAlpha(playerIndex) == 1.0F && !this.remote) {
                                    movingObject.setAlpha(playerIndex, 1.0F);
                                }
                            }
                        }
                    }
                }
            }

            if (this.isAlive()
                && vclose > 0
                && this.stats.lastVeryCloseZombies == 0
                && this.stats.numVisibleZombies > 0
                && this.stats.lastNumVisibleZombies == 0
                && this.timeSinceLastStab >= 600.0F) {
                this.timeSinceLastStab = 0.0F;
                long inst = this.getEmitter().playSoundImpl("ZombieSurprisedPlayer", null);
                this.getEmitter().setVolume(inst, Core.getInstance().getOptionJumpScareVolume() / 10.0F);
            }

            if (this.stats.numVisibleZombies > 0) {
                this.timeSinceLastStab = 0.0F;
            }

            if (this.timeSinceLastStab < 600.0F) {
                this.timeSinceLastStab = this.timeSinceLastStab + GameTime.getInstance().getThirtyFPSMultiplier();
            }

            float del = close / 20.0F;
            if (del > 1.0F) {
                del = 1.0F;
            }

            del *= 0.6F;
            SoundManager.instance.BlendVolume(MainScreenState.ambient, del);
            int actualSpotted = 0;

            for (int nx = 0; nx < this.spottedList.size(); nx++) {
                if (!this.lastSpotted.contains(this.spottedList.get(nx))) {
                    this.lastSpotted.add(this.spottedList.get(nx));
                }

                if (this.spottedList.get(nx) instanceof IsoZombie) {
                    actualSpotted++;
                }
            }

            if (this.clearSpottedTimer <= 0 && actualSpotted == 0) {
                this.lastSpotted.clear();
                this.clearSpottedTimer = 1000;
            } else {
                this.clearSpottedTimer--;
            }

            this.stats.lastNumVisibleZombies = this.stats.numVisibleZombies;
            this.stats.lastVeryCloseZombies = vclose;
        }
    }

    @Override
    public float getSeeNearbyCharacterDistance() {
        return (3.5F - this.stats.get(CharacterStat.FATIGUE) - this.stats.get(CharacterStat.INTOXICATION) * 0.01F) * this.getWornItemsVisionMultiplier();
    }

    private boolean checkSpottedPLayerTimer(IsoPlayer remoteChr) {
        if (!remoteChr.spottedByPlayer) {
            return false;
        } else {
            if (this.spottedPlayerTimer.containsKey(remoteChr.getRemoteID())) {
                this.spottedPlayerTimer.put(remoteChr.getRemoteID(), this.spottedPlayerTimer.get(remoteChr.getRemoteID()) + 1);
            } else {
                this.spottedPlayerTimer.put(remoteChr.getRemoteID(), 1);
            }

            if (this.spottedPlayerTimer.get(remoteChr.getRemoteID()) > 100) {
                remoteChr.spottedByPlayer = false;
                return false;
            } else {
                return true;
            }
        }
    }

    private float calculateMaxDist() {
        float minScreen = 1920.0F;
        float maxScreen = 7680.0F;
        float minDist = 80.0F;
        float maxDist = 320.0F;
        float t = (IsoCamera.getScreenWidth(0) - 1920.0F) / 5760.0F;
        return PZMath.clamp(80.0F + t * 240.0F, 80.0F, 320.0F);
    }

    public boolean checkCanSeeClient(UdpConnection remoteConnection) {
        return remoteConnection.role.hasCapability(Capability.SeesInvisiblePlayers) ? true : !this.isInvisible();
    }

    public boolean checkCanSeeClient(IsoPlayer remoteChr) {
        Vector2 oPos = tempVector2_1.set(this.getX(), this.getY());
        Vector2 tPos = tempVector2_2.set(remoteChr.getX(), remoteChr.getY());
        tPos.x = tPos.x - oPos.x;
        tPos.y = tPos.y - oPos.y;
        Vector2 dir = this.getForwardDirection();
        tPos.normalize();
        dir.normalize();
        float dot = tPos.dot(dir);
        if (!GameClient.client || remoteChr == this || !this.isLocalPlayer()) {
            return true;
        } else if (!this.isAccessLevel("None") && this.canSeeAll()) {
            remoteChr.spottedByPlayer = true;
            return true;
        } else {
            float dist = 0.0F;
            if (remoteChr.getCurrentSquare() != null) {
                dist = this.current == null ? 0.0F : remoteChr.getCurrentSquare().DistTo(this.getCurrentSquare());
                if (dist <= 2.0F) {
                    remoteChr.spottedByPlayer = true;
                    return true;
                } else if (ServerOptions.getInstance().hidePlayersBehindYou.getValue() && dot < -0.5) {
                    return this.checkSpottedPLayerTimer(remoteChr);
                } else if (remoteChr.isGhostMode() && this.isAccessLevel("None")) {
                    remoteChr.spottedByPlayer = false;
                    return false;
                } else {
                    IsoGridSquare.ILighting light = remoteChr.getCurrentSquare().lighting[this.getPlayerNum()];
                    if (!light.bCouldSee()) {
                        return this.checkSpottedPLayerTimer(remoteChr);
                    } else if (dist > this.calculateMaxDist()) {
                        remoteChr.spottedByPlayer = false;
                        return false;
                    } else if (!remoteChr.isSneaking() || !ServerOptions.getInstance().sneakModeHideFromOtherPlayers.getValue() || remoteChr.isSprinting()) {
                        remoteChr.spottedByPlayer = true;
                        return true;
                    } else if (remoteChr.spottedByPlayer) {
                        return true;
                    } else {
                        float expoRangeMod = (float)(Math.pow(Math.max(40.0F - dist, 0.0F), 3.0) / 12000.0);
                        float sneakSkillMod = 1.0F - remoteChr.remoteSneakLvl / 10.0F * 0.9F + 0.3F;
                        float chance = 1.0F;
                        if (dot < 0.8F) {
                            chance = 0.3F;
                        }

                        if (dot < 0.6F) {
                            chance = 0.05F;
                        }

                        float totalLight = (light.lightInfo().getR() + light.lightInfo().getG() + light.lightInfo().getB()) / 3.0F;
                        float tirednessMod = (1.0F - this.getMoodles().getMoodleLevel(MoodleType.TIRED) / 5.0F) * 0.7F + 0.3F;
                        float drunkennessMod = (1.0F - this.getMoodles().getMoodleLevel(MoodleType.DRUNK) / 5.0F) * 0.7F + 0.3F;
                        float movementMod = 0.1F;
                        if (remoteChr.isPlayerMoving()) {
                            movementMod = 0.35F;
                        }

                        if (remoteChr.isRunning()) {
                            movementMod = 1.0F;
                        }

                        ArrayList<Point> pts = PolygonalMap2.instance
                            .getPointInLine(remoteChr.getX(), remoteChr.getY(), this.getX(), this.getY(), PZMath.fastfloor(this.getZ()));
                        IsoGridSquare sqTestCover = null;
                        float sneakDistCover = 0.0F;
                        float observerDistCover = 0.0F;
                        boolean behindSomething = false;
                        boolean behindWindow = false;

                        for (int i = 0; i < pts.size(); i++) {
                            Point pt = pts.get(i);
                            sqTestCover = IsoCell.getInstance().getGridSquare((double)pt.x, (double)pt.y, (double)this.getZ());
                            if (sqTestCover != null) {
                                float result = sqTestCover.getGridSneakModifier(false);
                                if (result > 1.0F) {
                                    behindSomething = true;
                                    if (sqTestCover.getProperties().has(IsoFlagType.windowN) || sqTestCover.getProperties().has(IsoFlagType.windowW)) {
                                        behindWindow = true;
                                    }
                                    break;
                                }

                                int j = 0;

                                while (j < sqTestCover.getObjects().size()) {
                                    IsoObject obj = sqTestCover.getObjects().get(j);
                                    if (!obj.getSprite().getProperties().has(IsoFlagType.solidtrans) && !obj.getSprite().getProperties().has(IsoFlagType.solid)
                                        )
                                     {
                                        if (!obj.getSprite().getProperties().has(IsoFlagType.windowN)
                                            && !obj.getSprite().getProperties().has(IsoFlagType.windowW)) {
                                            j++;
                                            continue;
                                        }

                                        behindSomething = true;
                                        behindWindow = true;
                                        break;
                                    }

                                    behindSomething = true;
                                    break;
                                }

                                if (behindSomething) {
                                    break;
                                }
                            }
                        }

                        float windowMod = 1.0F;
                        if (behindWindow && remoteChr.isSneaking()) {
                            windowMod = 0.3F;
                        }

                        if (behindSomething) {
                            sneakDistCover = sqTestCover.DistTo(remoteChr.getCurrentSquare());
                            observerDistCover = sqTestCover.DistTo(this.getCurrentSquare());
                        }

                        float coverMod = observerDistCover < 2.0F ? 5.0F : Math.min(sneakDistCover, 5.0F);
                        coverMod = Math.max(0.0F, coverMod - 1.0F);
                        coverMod = coverMod / 5.0F * 0.9F + 0.1F;
                        float fogMod = Math.max(0.1F, 1.0F - ClimateManager.getInstance().getFogIntensity());
                        float finalChance = chance
                            * expoRangeMod
                            * totalLight
                            * sneakSkillMod
                            * tirednessMod
                            * drunkennessMod
                            * movementMod
                            * coverMod
                            * fogMod
                            * windowMod;
                        if (finalChance >= 1.0F) {
                            remoteChr.spottedByPlayer = true;
                            return true;
                        } else {
                            finalChance = (float)(1.0 - Math.pow(1.0F - finalChance, GameTime.getInstance().getMultiplier()));
                            finalChance *= 0.5F;
                            boolean canSee = Rand.Next(0.0F, 1.0F) < finalChance;
                            remoteChr.spottedByPlayer = canSee;
                            return canSee;
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    public String getTimeSurvived() {
        String total = "";
        int hours = (int)this.getHoursSurvived();
        int days = hours / 24;
        int hoursLeft = hours % 24;
        int months = days / 30;
        days %= 30;
        int years = months / 12;
        months %= 12;
        String dayString = Translator.getText("IGUI_Gametime_day");
        String yearString = Translator.getText("IGUI_Gametime_year");
        String hourString = Translator.getText("IGUI_Gametime_hour");
        String monthString = Translator.getText("IGUI_Gametime_month");
        if (years != 0) {
            if (years > 1) {
                yearString = Translator.getText("IGUI_Gametime_years");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + years + " " + yearString;
        }

        if (months != 0) {
            if (months > 1) {
                monthString = Translator.getText("IGUI_Gametime_months");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + months + " " + monthString;
        }

        if (days != 0) {
            if (days > 1) {
                dayString = Translator.getText("IGUI_Gametime_days");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + days + " " + dayString;
        }

        if (hoursLeft != 0) {
            if (hoursLeft > 1) {
                hourString = Translator.getText("IGUI_Gametime_hours");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + hoursLeft + " " + hourString;
        }

        if (total.isEmpty()) {
            int minutes = (int)(this.hoursSurvived * 60.0);
            total = minutes + " " + Translator.getText("IGUI_Gametime_minutes");
        }

        return total;
    }

    public boolean IsUsingAimWeapon() {
        if (this.leftHandItem == null) {
            return false;
        } else if (this.leftHandItem instanceof HandWeapon handWeapon) {
            return !this.isAiming() ? false : handWeapon.isAimedFirearm;
        } else {
            return false;
        }
    }

    private boolean IsUsingAimHandWeapon() {
        if (this.leftHandItem == null) {
            return false;
        } else if (this.leftHandItem instanceof HandWeapon handWeapon) {
            return !this.isAiming() ? false : handWeapon.isAimedHandWeapon;
        } else {
            return false;
        }
    }

    private boolean DoAimAnimOnAiming() {
        return this.IsUsingAimWeapon();
    }

    public int getSleepingPillsTaken() {
        return this.sleepingPillsTaken;
    }

    /**
     * If you've take more than 10 sleeping pills you lose some health If you're
     *  drunk, 1 pills = 2
     */
    public void setSleepingPillsTaken(int sleepingPillsTaken) {
        if (this.isGodMod()) {
            this.resetSleepingPillsTaken();
        } else {
            this.sleepingPillsTaken = sleepingPillsTaken;
            if (this.getStats().get(CharacterStat.INTOXICATION) > 10.0F) {
                this.sleepingPillsTaken++;
            }

            this.lastPillsTaken = GameTime.instance.calender.getTimeInMillis();
        }
    }

    public void resetSleepingPillsTaken() {
        this.sleepingPillsTaken = 0;
    }

    @Override
    public boolean isOutside() {
        return this.getCurrentSquare() != null && this.getCurrentSquare().getRoom() == null && !this.isInARoom();
    }

    public double getLastSeenZomboidTime() {
        return this.lastSeenZombieTime;
    }

    /**
     * Return the amount of temperature given by clothes wear
     * @return temperature
     */
    public float getPlayerClothingTemperature() {
        float result = 0.0F;
        if (this.getClothingItem_Feet() != null) {
            result += ((Clothing)this.getClothingItem_Feet()).getTemperature();
        }

        if (this.getClothingItem_Hands() != null) {
            result += ((Clothing)this.getClothingItem_Hands()).getTemperature();
        }

        if (this.getClothingItem_Head() != null) {
            result += ((Clothing)this.getClothingItem_Head()).getTemperature();
        }

        if (this.getClothingItem_Legs() != null) {
            result += ((Clothing)this.getClothingItem_Legs()).getTemperature();
        }

        if (this.getClothingItem_Torso() != null) {
            result += ((Clothing)this.getClothingItem_Torso()).getTemperature();
        }

        return result;
    }

    public float getPlayerClothingInsulation() {
        float result = 0.0F;
        if (this.getClothingItem_Feet() != null) {
            result += ((Clothing)this.getClothingItem_Feet()).getInsulation() * 0.1F;
        }

        if (this.getClothingItem_Hands() != null) {
            result += ((Clothing)this.getClothingItem_Hands()).getInsulation() * 0.0F;
        }

        if (this.getClothingItem_Head() != null) {
            result += ((Clothing)this.getClothingItem_Head()).getInsulation() * 0.0F;
        }

        if (this.getClothingItem_Legs() != null) {
            result += ((Clothing)this.getClothingItem_Legs()).getInsulation() * 0.3F;
        }

        if (this.getClothingItem_Torso() != null) {
            result += ((Clothing)this.getClothingItem_Torso()).getInsulation() * 0.6F;
        }

        return result;
    }

    public InventoryItem getActiveLightItem() {
        if (this.rightHandItem != null && this.rightHandItem.isEmittingLight()) {
            return this.rightHandItem;
        } else if (this.leftHandItem != null && this.leftHandItem.isEmittingLight()) {
            return this.leftHandItem;
        } else {
            AttachedItems attachedItems = this.getAttachedItems();

            for (int i = 0; i < attachedItems.size(); i++) {
                InventoryItem item = attachedItems.getItemByIndex(i);
                if (item.isEmittingLight()) {
                    return item;
                }
            }

            return null;
        }
    }

    public boolean isTorchCone() {
        if (this.remote) {
            return this.mpTorchCone;
        } else {
            InventoryItem item = this.getActiveLightItem();
            return item != null && item.isTorchCone();
        }
    }

    public float getTorchDot() {
        InventoryItem item = this.getActiveLightItem();
        return item != null ? item.getTorchDot() : 0.0F;
    }

    public float getLightDistance() {
        if (this.remote) {
            return this.mpTorchDist;
        } else {
            InventoryItem item = this.getActiveLightItem();
            return item != null ? item.getLightDistance() : 0.0F;
        }
    }

    public boolean pressedMovement(boolean ignoreBlock) {
        if (this.isNpc) {
            return false;
        } else if (GameClient.client && !this.isLocal()) {
            return this.networkAi.isPressedMovement();
        } else {
            boolean bPressedRun = false;
            if (this.playerIndex == 0) {
                bPressedRun = GameKeyboard.isKeyDown("Run");
            }

            if (this.joypadBind != -1) {
                bPressedRun |= JoypadManager.instance.isRTPressed(this.joypadBind);
            }

            this.setVariable("pressedRunButton", bPressedRun);
            if (ignoreBlock || !this.isBlockMovement() && !this.isIgnoreInputsForDirection()) {
                if (this.playerIndex != 0
                    || !GameKeyboard.isKeyDown("Left")
                        && !GameKeyboard.isKeyDown("Right")
                        && !GameKeyboard.isKeyDown("Forward")
                        && !GameKeyboard.isKeyDown("Backward")) {
                    if (this.joypadBind != -1) {
                        float yVal = JoypadManager.instance.getMovementAxisY(this.joypadBind);
                        float xVal = JoypadManager.instance.getMovementAxisX(this.joypadBind);
                        float deadZone = JoypadManager.instance.getDeadZone(this.joypadBind, 0);
                        if (Math.abs(yVal) > deadZone || Math.abs(xVal) > deadZone) {
                            if (GameClient.client && this.isLocal()) {
                                this.networkAi.setPressedMovement(true);
                            }

                            return true;
                        }
                    }

                    if (GameClient.client && this.isLocal()) {
                        this.networkAi.setPressedMovement(false);
                    }

                    return false;
                } else {
                    if (GameClient.client && this.isLocal()) {
                        this.networkAi.setPressedMovement(true);
                    }

                    return true;
                }
            } else {
                if (GameClient.client && this.isLocal()) {
                    this.networkAi.setPressedMovement(false);
                }

                return false;
            }
        }
    }

    public boolean pressedCancelAction() {
        if (this.isNpc) {
            return false;
        } else if (GameClient.client && !this.isLocal()) {
            return this.networkAi.isPressedCancelAction();
        } else if (this.playerIndex == 0 && GameKeyboard.isKeyDown("CancelAction")) {
            if (GameClient.client && this.isLocal()) {
                this.networkAi.setPressedCancelAction(true);
            }

            return true;
        } else if (this.joypadBind != -1) {
            boolean cancelAction = JoypadManager.instance.isBButtonStartPress(this.joypadBind);
            if (GameClient.client && this.isLocal()) {
                this.networkAi.setPressedCancelAction(cancelAction);
            }

            return cancelAction;
        } else {
            if (GameClient.client && this.isLocal()) {
                this.networkAi.setPressedCancelAction(false);
            }

            return false;
        }
    }

    public boolean checkWalkTo() {
        if (this.isNpc) {
            return false;
        } else if (this.playerIndex == 0 && GameKeyboard.isKeyDown("WalkTo")) {
            LuaEventManager.triggerEvent("OnPressWalkTo", 0, 0, 0);
            return true;
        } else {
            return false;
        }
    }

    public boolean pressedAim() {
        if (this.isNpc) {
            return false;
        } else if (this.playerIndex == 0 && this.isAimKeyDown()) {
            return true;
        } else if (this.joypadBind == -1) {
            return false;
        } else {
            float yVal = JoypadManager.instance.getAimingAxisY(this.joypadBind);
            float xVal = JoypadManager.instance.getAimingAxisX(this.joypadBind);
            return Math.abs(yVal) > 0.1F || Math.abs(xVal) > 0.1F;
        }
    }

    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        if (this.isDead()) {
            return false;
        } else if (!this.getCharacterActions().isEmpty()) {
            return true;
        } else {
            State state = this.getCurrentState();
            if (state != null && state.isDoingActionThatCanBeCancelled()) {
                return true;
            } else {
                for (int i = 0; i < this.stateMachine.getSubStateCount(); i++) {
                    state = this.stateMachine.getSubStateAt(i);
                    if (state != null && state.isDoingActionThatCanBeCancelled()) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public long getSteamID() {
        return this.steamId;
    }

    public void setSteamID(long steamId) {
        this.steamId = steamId;
    }

    public boolean isTargetedByZombie() {
        return this.targetedByZombie;
    }

    @Override
    public boolean isMaskClicked(int x, int y, boolean flip) {
        return this.sprite == null ? false : this.sprite.isMaskClicked(this.dir, x, y, flip);
    }

    public int getOffSetXUI() {
        return this.offSetXUi;
    }

    public void setOffSetXUI(int offSetXUi) {
        this.offSetXUi = offSetXUi;
    }

    public int getOffSetYUI() {
        return this.offSetYUi;
    }

    public void setOffSetYUI(int offSetYUi) {
        this.offSetYUi = offSetYUi;
    }

    public String getUsername() {
        return this.getUsername(false, false);
    }

    public String getUsername(Boolean canShowFirstname) {
        return this.getUsername(canShowFirstname, false);
    }

    public String getUsername(Boolean canShowFirstname, Boolean canShowDisguisedName) {
        String nameStr = this.username;
        if (canShowDisguisedName) {
            this.updateDisguisedState();
            boolean bViewerIsAdmin = GameClient.client
                && IsoCamera.getCameraCharacter() instanceof IsoPlayer player
                && player.role.hasCapability(Capability.CanSeePlayersStats);
            if (this.isDisguised() && !bViewerIsAdmin) {
                nameStr = ServerOptions.getInstance().hideDisguisedUserName.getValue() ? "" : Translator.getText("IGUI_Disguised_Player_Name");
            } else if (canShowFirstname && GameClient.client && ServerOptions.instance.showFirstAndLastName.getValue()) {
                nameStr = this.getDescriptor().getForename() + " " + this.getDescriptor().getSurname();
                if (ServerOptions.instance.displayUserName.getValue()) {
                    nameStr = nameStr + " (" + this.username + ")";
                }
            }
        } else if (canShowFirstname && GameClient.client && ServerOptions.instance.showFirstAndLastName.getValue()) {
            nameStr = this.getDescriptor().getForename() + " " + this.getDescriptor().getSurname();
            if (ServerOptions.instance.displayUserName.getValue()) {
                nameStr = nameStr + " (" + this.username + ")";
            }
        }

        return nameStr;
    }

    public void setUsername(String newUsername) {
        this.username = newUsername;
    }

    public void updateUsername() {
        if (!GameClient.client && !GameServer.server) {
            this.username = this.getDescriptor().getForename() + this.getDescriptor().getSurname();
        }
    }

    @Override
    public short getOnlineID() {
        return this.onlineId;
    }

    public boolean isLocalPlayer() {
        if (GameServer.server) {
            return false;
        } else {
            for (int pn = 0; pn < numPlayers; pn++) {
                if (players[pn] == this) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean isLocalPlayer(IsoGameCharacter in_character) {
        return in_character instanceof IsoPlayer player && player.isLocalPlayer();
    }

    public static boolean isLocalPlayer(Object in_characterObject) {
        return in_characterObject instanceof IsoPlayer player && player.isLocalPlayer();
    }

    public static void setLocalPlayer(int index, IsoPlayer newPlayerObj) {
        players[index] = newPlayerObj;
    }

    public static IsoPlayer getLocalPlayerByOnlineID(short ID) {
        for (int i = 0; i < numPlayers; i++) {
            IsoPlayer player = players[i];
            if (player != null && player.onlineId == ID) {
                return player;
            }
        }

        return null;
    }

    public boolean isOnlyPlayerAsleep() {
        if (!this.isAsleep()) {
            return false;
        } else {
            for (int pn = 0; pn < numPlayers; pn++) {
                if (players[pn] != null && !players[pn].isDead() && players[pn] != this && players[pn].isAsleep()) {
                    return false;
                }
            }

            return true;
        }
    }

    public void setHasObstacleOnPath(boolean value) {
        this.hasObstacleOnPath = value;
    }

    public boolean isRemoteAndHasObstacleOnPath() {
        return !this.isLocalPlayer() && this.hasObstacleOnPath;
    }

    @Override
    public void OnDeath() {
        super.OnDeath();
        if (!GameServer.server) {
            this.StopAllActionQueue();
            if (this.isAsleep()) {
                UIManager.FadeIn(this.getPlayerNum(), 0.5);
                this.setAsleep(false);
            }

            this.dropHandItems();
            if (allPlayersDead()) {
                SoundManager.instance.playMusic("PlayerDied");
            }

            if (this.isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnPlayerDeath", this);
            }

            if (this.isLocalPlayer() && this.getVehicle() != null) {
                this.getVehicle().exit(this);
            }

            this.removeSaveFile();
            if (this.shouldBecomeZombieAfterDeath()) {
                this.forceAwake();
            }

            if (this.getMoodles() != null) {
                this.getMoodles().Update();
            }

            this.getCell().setDrag(null, this.getPlayerNum());
        }
    }

    public boolean isNoClip() {
        return this.getCheats().isSet(CheatType.NO_CLIP);
    }

    public void setNoClip(boolean noClip, boolean isForced) {
        if (!isForced) {
            this.setNoClip(noClip);
        } else {
            this.getCheats().set(CheatType.NO_CLIP, noClip);
        }
    }

    public void setNoClip(boolean noClip) {
        if (!Role.hasCapability(this, Capability.ToggleNoclipHimself)) {
            this.getCheats().set(CheatType.NO_CLIP, false);
        } else {
            this.getCheats().set(CheatType.NO_CLIP, noClip);
        }
    }

    /** @deprecated */
    public void setAuthorizeMeleeAction(boolean enabled) {
        this.setAuthorizedHandToHandAction(enabled);
    }

    /** @deprecated */
    public boolean isAuthorizeMeleeAction() {
        return this.isAuthorizedHandToHandAction();
    }

    /** @deprecated */
    public void setAuthorizeShoveStomp(boolean enabled) {
        this.setAuthorizedHandToHand(enabled);
    }

    /** @deprecated */
    public boolean isAuthorizeShoveStomp() {
        return this.isAuthorizedHandToHand();
    }

    public void setAuthorizedHandToHandAction(boolean enabled) {
        this.isAuthorizedHandToHandAction = enabled;
    }

    public boolean isAuthorizedHandToHandAction() {
        return this.isAuthorizedHandToHandAction;
    }

    public void setAuthorizedHandToHand(boolean enabled) {
        this.isAuthorizedHandToHand = enabled;
    }

    public boolean isAuthorizedHandToHand() {
        return this.isAuthorizedHandToHand;
    }

    public boolean isBlockMovement() {
        return this.blockMovement;
    }

    public void setBlockMovement(boolean blockMovement) {
        this.blockMovement = blockMovement;
    }

    public void startReceivingBodyDamageUpdates(IsoPlayer other) {
        if (GameClient.client && other != null && other != this && this.isLocalPlayer() && !other.isLocalPlayer()) {
            other.resetBodyDamageRemote();
            BodyDamageSync.instance.startReceivingUpdates(other);
        }
    }

    public void stopReceivingBodyDamageUpdates(IsoPlayer other) {
        if (GameClient.client && other != null && other != this && !other.isLocalPlayer()) {
            BodyDamageSync.instance.stopReceivingUpdates(other);
        }
    }

    public Nutrition getNutrition() {
        return this.nutrition;
    }

    public Fitness getFitness() {
        return this.fitness;
    }

    public void updateRemotePlayerInVehicle() {
        float newX = this.getVehicle().getX();
        float newY = this.getVehicle().getY();
        float newZ = this.getVehicle().getZ();
        this.realx = newX;
        this.realy = newY;
        this.realz = (byte)PZMath.fastfloor(newZ);
        this.networkAi.targetX = newX;
        this.networkAi.targetY = newY;
        this.networkAi.targetZ = PZMath.fastfloor(newZ);
        this.setForceX(newX);
        this.setForceY(newY);
        this.setZ(newZ);
        this.setTimeSinceLastNetData(0);
        if (GameServer.server) {
            UdpConnection passengerConnection = GameServer.getConnectionFromPlayer(this);
            if (passengerConnection != null) {
                passengerConnection.releventPos[this.playerIndex].set(newX, newY, newZ);
            }
        }
    }

    protected float getNetworkSpeedMul() {
        float scale = IsoUtils.DistanceTo(this.getX(), this.getY(), this.networkAi.targetX, this.networkAi.targetY)
            / IsoUtils.DistanceTo(this.realx, this.realy, this.networkAi.targetX, this.networkAi.targetY);
        return 0.8F + 0.4F * IsoUtils.smoothstep(0.8F, 1.2F, scale);
    }

    protected boolean updateRemotePlayer() {
        if (!this.remote) {
            return false;
        } else {
            if (this.isSeatedInVehicle()) {
                this.updateRemotePlayerInVehicle();
            }

            if (GameServer.server) {
                ServerLOS.instance.doServerZombieLOS(this);
                ServerLOS.instance.updateLOS(this);
                if (this.isDead()) {
                    return true;
                }

                this.isPlayerMoving = this.networkAi.moving;
                if (this.isPlayerMoving) {
                    if (this.isSprinting()) {
                        this.currentSpeed = 1.5F;
                    } else if (this.isRunning()) {
                        this.currentSpeed = 1.0F;
                    } else {
                        this.currentSpeed = 0.5F;
                    }
                } else {
                    this.currentSpeed = 0.0F;
                }

                if (this.isJustMoved() && !this.isNpc) {
                    LuaEventManager.triggerEvent("OnPlayerMove", this);
                }

                this.removeFromSquare();
                this.setForceX(this.realx);
                this.setForceY(this.realy);
                this.setZ(this.realz);
                this.setLastZ(this.realz);
                this.ensureOnTile();
                this.setMovingSquareNow();
                if (this.slowTimer > 0.0F) {
                    this.slowTimer = this.slowTimer - GameTime.instance.getRealworldSecondsSinceLastUpdate();
                    this.slowFactor = this.slowFactor - GameTime.instance.getMultiplier() / 100.0F;
                    if (this.slowFactor < 0.0F) {
                        this.slowFactor = 0.0F;
                    }
                } else {
                    this.slowFactor = 0.0F;
                }
            }

            if (GameClient.client) {
                if (this.isCurrentState(BumpedState.instance())) {
                    return true;
                }

                Vector3 target = this.networkAi.tempTarget;
                if (!this.networkAi.isCollisionEnabled() && !this.networkAi.isNoCollisionTimeout()) {
                    this.setCollidable(false);
                    target.set(this.realx, this.realy, this.realz);
                } else {
                    this.setCollidable(true);
                    target.set(this.networkAi.targetX, this.networkAi.targetY, this.networkAi.targetZ);
                }

                PathFindBehavior2 pfb2 = this.getPathFindBehavior2();
                this.networkAi.getState().processExitSubState();
                this.networkAi.getState().processExitState();
                boolean moveToEvent = this.networkAi.getState().processEnterState(target);
                if (!moveToEvent) {
                    moveToEvent = this.networkAi.getState().processEnterSubState(target);
                } else if (this.networkAi.getState().getEnterState().getState() == PlayerSitOnFurnitureState.instance()) {
                    this.setX(this.realx);
                    this.setY(this.realy);
                    this.setZ(this.realz);
                    this.setLastX(this.realx);
                    this.setLastY(this.realy);
                    this.setLastZ(this.realz);
                }

                this.networkAi.targetX = target.x;
                this.networkAi.targetY = target.y;
                if (!this.networkAi.forcePathFinder && this.isRemoteAndHasObstacleOnPath()) {
                    this.networkAi.forcePathFinder = true;
                }

                if (this.networkAi.forcePathFinder
                        && !PolygonalMap2.instance
                            .lineClearCollide(this.getX(), this.getY(), target.x, target.y, PZMath.fastfloor(this.getZ()), this.vehicle, false, true)
                        && IsoUtils.DistanceManhatten(target.x, target.y, this.getX(), this.getY()) < 2.0F
                    || this.getCurrentState() == ClimbOverFenceState.instance()
                    || this.getCurrentState() == ClimbThroughWindowState.instance()
                    || this.getCurrentState() == ClimbOverWallState.instance()) {
                    this.networkAi.forcePathFinder = false;
                }

                float DIST_TO_MOVING = 0.2F;
                if (!this.networkAi.needToMovingUsingPathFinder && !this.networkAi.forcePathFinder) {
                    if (this.networkAi.usePathFind) {
                        pfb2.reset();
                        this.setPath2(null);
                        this.networkAi.usePathFind = false;
                    }

                    pfb2.walkingOnTheSpot.reset(this.getX(), this.getY());
                    this.getDeferredMovement(tempVector2_2);
                    if (this.getCurrentState() != ClimbOverWallState.instance() && this.getCurrentState() != ClimbOverFenceState.instance()) {
                        pfb2.moveToPoint(target.x, target.y, this.getNetworkSpeedMul());
                    } else {
                        this.MoveUnmodded(tempVector2_2);
                    }

                    boolean inaccurateX = PZMath.fastfloor(target.x) != PZMath.fastfloor(this.getX());
                    boolean inaccurateY = PZMath.fastfloor(target.y) != PZMath.fastfloor(this.getY());
                    boolean inaccurateZ = PZMath.fastfloor(target.z) != PZMath.fastfloor(this.getZ());
                    float distance = IsoUtils.DistanceManhatten(target.x, target.y, this.getX(), this.getY());
                    this.isPlayerMoving = moveToEvent || inaccurateX || inaccurateY || inaccurateZ || distance > 0.2F;
                    if (this.isCurrentState(PlayerSitOnGroundState.instance())
                        || this.isCurrentState(PlayerSitOnFurnitureState.instance())
                        || this.isCurrentState(AnimalAlertedState.instance())
                        || this.isCurrentState(AnimalEatState.instance())) {
                        this.isPlayerMoving = false;
                    }

                    if (!this.isPlayerMoving) {
                        this.DirectionFromVector(this.networkAi.direction);
                        this.setForwardDirection(this.networkAi.direction);
                        this.networkAi.forcePathFinder = false;
                        if (this.networkAi.usePathFind) {
                            pfb2.reset();
                            this.setPath2(null);
                            this.networkAi.usePathFind = false;
                        }
                    }

                    this.setJustMoved(this.isPlayerMoving);
                    this.deltaX = 0.0F;
                    this.deltaY = 0.0F;
                } else {
                    if ((!this.networkAi.usePathFind || target.x != pfb2.getTargetX() || target.y != pfb2.getTargetY())
                        && (
                            Math.abs(pfb2.getTargetX() - target.x) > 0.3F
                                || Math.abs(pfb2.getTargetY() - target.y) > 0.3F
                                || Math.abs(pfb2.getTargetZ() - target.z) > 0.3F
                                || !pfb2.isGoalLocation()
                                || pfb2.getIsCancelled()
                        )) {
                        pfb2.pathToLocationF(target.x, target.y, target.z);
                        pfb2.walkingOnTheSpot.reset(this.getX(), this.getY());
                        this.networkAi.usePathFind = true;
                    }

                    PathFindBehavior2.BehaviorResult result = pfb2.update();
                    if (result == PathFindBehavior2.BehaviorResult.Failed) {
                        this.setPathFindIndex(-1);
                        if (this.networkAi.forcePathFinder) {
                            this.networkAi.forcePathFinder = false;
                        } else if (this.getVehicle() == null) {
                            this.teleportTo(this.realx, this.realy, this.realz);
                            if (GameServer.server) {
                                DebugLog.Multiplayer
                                    .warn(
                                        String.format(
                                            "Player %d teleport from (%.2f, %.2f, %.2f) to (%.2f, %.2f, %.2f)",
                                            this.getOnlineID(),
                                            this.getX(),
                                            this.getY(),
                                            this.getZ(),
                                            this.realx,
                                            this.realy,
                                            this.realz
                                        )
                                    );
                            }
                        }
                    } else if (result == PathFindBehavior2.BehaviorResult.Succeeded) {
                        int tx = PZMath.fastfloor(pfb2.getTargetX());
                        int ty = PZMath.fastfloor(pfb2.getTargetY());
                        if (GameServer.server) {
                            ServerMap.instance.getChunk(tx / 8, ty / 8);
                        } else {
                            IsoWorld.instance.currentCell.getChunkForGridSquare(tx, ty, 0);
                        }

                        this.isPlayerMoving = true;
                        this.setJustMoved(true);
                    }

                    this.deltaX = 0.0F;
                    this.deltaY = 0.0F;
                }

                if (!this.isPlayerMoving
                    || this.isAiming()
                    || this.isCurrentState(SwipeStatePlayer.instance())
                    || this.isCurrentState(PlayerStrafeState.instance())
                    || this.isCurrentState(PlayerDraggingCorpse.instance())) {
                    this.DirectionFromVector(this.networkAi.direction);
                    this.setForwardDirection(this.networkAi.direction);
                    tempo.set(target.x - this.getNextX(), -(target.y - this.getNextY()));
                    tempo.normalize();
                    float angle = this.getAnimationPlayer().getRenderedAngle();
                    if (angle > Math.PI * 2) {
                        angle = (float)(angle - (Math.PI * 2));
                    }

                    if (angle < 0.0F) {
                        angle = (float)(angle + (Math.PI * 2));
                    }

                    tempo.rotate(angle);
                    tempo.setLength(Math.min(IsoUtils.DistanceTo(target.x, target.y, this.getX(), this.getY()), 1.0F));
                    this.deltaX = tempo.x;
                    this.deltaY = tempo.y;
                }

                if (this.isAnimal() && this.networkAi.animalPacket.isDead()) {
                    float distancex = IsoUtils.DistanceManhatten(target.x, target.y, this.getX(), this.getY());
                    if (distancex <= 0.2F) {
                        this.setHealth(0.0F);
                        this.setDirectionAngle(this.networkAi.animalPacket.prediction.direction);
                    }
                }
            }

            return true;
        }
    }

    private boolean updateWhileDead() {
        if (GameServer.server) {
            return false;
        } else if (!this.isLocalPlayer()) {
            return false;
        } else if (!this.isDead()) {
            return false;
        } else {
            this.setVariable("bPathfind", false);
            this.setMoving(false);
            this.isPlayerMoving = false;
            if (this.getVehicle() != null) {
                this.getVehicle().exit(this);
            }

            if (this.heartEventInstance != 0L) {
                this.getEmitter().stopSound(this.heartEventInstance);
                this.heartEventInstance = 0L;
            }

            this.stopAttackLoopSound(false);
            return true;
        }
    }

    private void initFMODParameters() {
        FMODParameterList fmodParameters = this.getFMODParameters();
        if (this instanceof IsoAnimal) {
            fmodParameters.add(this.parameterFootstepMaterial);
            fmodParameters.add(this.parameterFootstepMaterial2);
        } else {
            fmodParameters.add(this.parameterCharacterMoving);
            fmodParameters.add(this.parameterCharacterMovementSpeed);
            fmodParameters.add(this.parameterCharacterOnFire);
            fmodParameters.add(this.parameterCharacterVoiceType);
            fmodParameters.add(this.parameterCharacterVoicePitch);
            fmodParameters.add(this.parameterDeaf);
            fmodParameters.add(this.parameterDragMaterial);
            fmodParameters.add(this.parameterElevation);
            fmodParameters.add(this.parameterEquippedBaggageContainer);
            fmodParameters.add(this.parameterExercising);
            fmodParameters.add(this.parameterFirearmInside);
            fmodParameters.add(this.parameterFirearmRoomSize);
            fmodParameters.add(this.parameterFootstepMaterial);
            fmodParameters.add(this.parameterFootstepMaterial2);
            fmodParameters.add(this.parameterIsStashTile);
            fmodParameters.add(this.parameterLocalPlayer);
            fmodParameters.add(this.parameterMeleeHitSurface);
            fmodParameters.add(this.parameterOverlapFoliageType);
            fmodParameters.add(this.parameterPlayerHealth);
            fmodParameters.add(this.parameterShoeType);
            fmodParameters.add(this.parameterVehicleHitLocation);
        }
    }

    public ParameterCharacterMovementSpeed getParameterCharacterMovementSpeed() {
        return this.parameterCharacterMovementSpeed;
    }

    public void setMeleeHitSurface(ParameterMeleeHitSurface.Material material) {
        this.parameterMeleeHitSurface.setMaterial(material);
    }

    public void setMeleeHitSurface(String material) {
        try {
            if ("MetalPoleGateDouble".equalsIgnoreCase(material) || "MetalPoleGateSmall".equalsIgnoreCase(material)) {
                material = "MetalGate";
            }

            this.parameterMeleeHitSurface.setMaterial(ParameterMeleeHitSurface.Material.valueOf(material));
        } catch (IllegalArgumentException var3) {
            this.parameterMeleeHitSurface.setMaterial(ParameterMeleeHitSurface.Material.Default);
        }
    }

    public void setVehicleHitLocation(BaseVehicle vehicle) {
        ParameterVehicleHitLocation.HitLocation location = ParameterVehicleHitLocation.calculateLocation(vehicle, this.getX(), this.getY(), this.getZ());
        this.parameterVehicleHitLocation.setLocation(location);
    }

    private void updateHeartSound() {
        if (!GameServer.server) {
            if (this.isLocalPlayer()) {
                GameSound gameSound = GameSounds.getSound("HeartBeat");
                boolean playHeartbeats = gameSound != null && gameSound.getUserVolume() > 0.0F && this.stats.get(CharacterStat.PANIC) > 0.0F;
                if (!this.asleep && playHeartbeats && GameTime.getInstance().getTrueMultiplier() == 1.0F) {
                    this.heartDelay = this.heartDelay - GameTime.getInstance().getThirtyFPSMultiplier();
                    if (this.heartEventInstance == 0L || !this.getEmitter().isPlaying(this.heartEventInstance)) {
                        this.heartEventInstance = this.getEmitter().playSoundImpl("HeartBeat", null);
                        this.getEmitter().setVolume(this.heartEventInstance, 0.0F);
                    }

                    if (this.heartDelay <= 0.0F) {
                        this.heartDelayMax = (int)((1.0F - this.stats.get(CharacterStat.PANIC) / 100.0F * 0.7F) * 25.0F) * 2.0F;
                        this.heartDelay = this.heartDelayMax;
                        if (this.heartEventInstance != 0L) {
                            this.getEmitter().setVolume(this.heartEventInstance, this.stats.get(CharacterStat.PANIC) / 100.0F);
                        }
                    }
                } else if (this.heartEventInstance != 0L) {
                    this.getEmitter().setVolume(this.heartEventInstance, 0.0F);
                }
            }
        }
    }

    private void updateWorldAmbiance() {
        if (!GameServer.server) {
            if (this.isLocalPlayer()) {
                int playerIndex = -1;

                for (int i = 0; i < numPlayers; i++) {
                    IsoPlayer player = players[i];
                    if (player != null && !player.hasTrait(CharacterTrait.DEAF)) {
                        playerIndex = i;
                        break;
                    }
                }

                if (playerIndex != -1) {
                    int forcedZ = 0;
                    if (this.worldAmbienceEmitter == null) {
                        this.worldAmbienceEmitter = (BaseSoundEmitter)(Core.soundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
                        this.worldAmbienceEmitter.setPos(this.getX(), this.getY(), 0.0F);
                    }

                    if (this.worldAmbianceInstance == 0L || !this.worldAmbienceEmitter.isPlaying(this.worldAmbianceInstance)) {
                        this.worldAmbianceInstance = this.worldAmbienceEmitter.playSoundImpl("WorldAmbiance", (IsoObject)null);
                        this.worldAmbienceEmitter.setVolume(this.worldAmbianceInstance, 1.0F);
                    }

                    this.worldAmbienceEmitter.setPos(this.getX(), this.getY(), 0.0F);
                    this.worldAmbienceEmitter.tick();
                }
            }
        }
    }

    private void updateEquippedBaggageContainer() {
        if (!GameServer.server) {
            if (this.isLocalPlayer()) {
                InventoryItem item = this.getClothingItem_Back();
                if (item != null && item.IsInventoryContainer()) {
                    String paramStr = item.getSoundParameter("EquippedBaggageContainer");
                    this.parameterEquippedBaggageContainer.setContainerType(paramStr);
                } else {
                    item = this.getSecondaryHandItem();
                    if (item != null && item.IsInventoryContainer()) {
                        String paramStr = item.getSoundParameter("EquippedBaggageContainer");
                        this.parameterEquippedBaggageContainer.setContainerType(paramStr);
                    } else {
                        item = this.getPrimaryHandItem();
                        if (item != null && item.IsInventoryContainer()) {
                            String paramStr = item.getSoundParameter("EquippedBaggageContainer");
                            this.parameterEquippedBaggageContainer.setContainerType(paramStr);
                        } else {
                            this.parameterEquippedBaggageContainer.setContainerType(ParameterEquippedBaggageContainer.ContainerType.None);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void DoFootstepSound(String type) {
        ParameterCharacterMovementSpeed.MovementType movementType = ParameterCharacterMovementSpeed.MovementType.Walk;
        float speed = 0.5F;
        switch (type) {
            case "sneak_walk":
                speed = 0.25F;
                movementType = ParameterCharacterMovementSpeed.MovementType.SneakWalk;
                break;
            case "sneak_run":
                speed = 0.25F;
                movementType = ParameterCharacterMovementSpeed.MovementType.SneakRun;
                break;
            case "strafe":
                speed = 0.5F;
                movementType = ParameterCharacterMovementSpeed.MovementType.Strafe;
                break;
            case "walk":
                speed = 0.5F;
                movementType = ParameterCharacterMovementSpeed.MovementType.Walk;
                break;
            case "run":
                speed = 0.75F;
                movementType = ParameterCharacterMovementSpeed.MovementType.Run;
                break;
            case "sprint":
                speed = 1.0F;
                movementType = ParameterCharacterMovementSpeed.MovementType.Sprint;
        }

        this.parameterCharacterMovementSpeed.setMovementType(movementType);
        this.DoFootstepSound(speed);
    }

    private void updateHeavyBreathing() {
    }

    public long playerVoiceSound(String suffix) {
        String prefix = this.descriptor.getVoicePrefix();
        return this.getEmitter().isPlaying(prefix + suffix) ? 0L : this.getEmitter().playVocals(prefix + suffix);
    }

    public long transmitPlayerVoiceSound(String suffix) {
        String prefix = this.descriptor.getVoicePrefix();
        if (this.getEmitter().isPlaying(prefix + suffix)) {
            return 0L;
        } else {
            if (GameClient.client && (!this.isInvisible() || DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue())) {
                INetworkPacket.send(PacketTypes.PacketType.PlaySound, prefix + suffix, false, this);
            }

            return this.getEmitter().playVocals(prefix + suffix);
        }
    }

    public long stopPlayerVoiceSound(String suffix) {
        String prefix = this.descriptor.getVoicePrefix();
        return this.getEmitter().stopSoundByName(prefix + suffix);
    }

    public void updateVocalProperties() {
        if (!GameServer.server) {
            if (this.isLocal()) {
                if (this.vocalEvent != 0L && !this.getEmitter().isPlaying(this.vocalEvent)) {
                    this.vocalEvent = 0L;
                    if (this.parameterMoodles != null) {
                        this.parameterMoodles.reset();
                    }
                }

                if (this.vocalEvent == 0L && this.isAlive()) {
                    this.vocalEvent = this.playSoundLocal(this.descriptor.getVoicePrefix());
                    if (this.parameterMoodles != null) {
                        this.parameterMoodles.reset();
                    }
                }

                if (this.vocalEvent != 0L) {
                    if (this.parameterMoodles == null) {
                        this.parameterMoodles = new ParameterMoodles(this);
                    }

                    this.parameterMoodles.update(this.vocalEvent);
                }
            }
        }
    }

    public boolean isDraggingCorpseStateName(String stateName) {
        if (!this.isDraggingCorpse()) {
            return false;
        } else {
            AnimLayer animLayer = this.getAdvancedAnimator().getRootLayer();
            return animLayer == null ? false : animLayer.isCurrentState(stateName);
        }
    }

    public boolean isPickingUpBody() {
        return this.isDraggingCorpseStateName("pickUpBody-HeadEnd") || this.isDraggingCorpseStateName("pickUpBody-LegsEnd");
    }

    public boolean isPuttingDownBody() {
        return this.isDraggingCorpseStateName("layDownBody");
    }

    private void updateDraggingCorpseSounds() {
        if (!this.isAnimal()) {
            boolean bPickingUp = this.isPickingUpBody();
            boolean bPuttingDown = this.isPuttingDownBody();
            boolean bPlayDrag = false;
            boolean bPlayTurn = false;
            if (this.isDraggingCorpse() && !bPickingUp && !bPuttingDown) {
                if (this.isPlayerMoving() && this.getBeenMovingFor() > 20.0F) {
                    bPlayDrag = true;
                }

                if (this.isTurning()) {
                    bPlayTurn = true;
                }
            }

            if (bPlayDrag) {
                if (!this.getEmitter().isPlaying("CorpseDrag")) {
                    this.getEmitter().playSoundImpl("CorpseDrag", null);
                }
            } else if (this.getEmitter().isPlaying("CorpseDrag")) {
                this.getEmitter().stopOrTriggerSoundByName("CorpseDrag");
            }

            if (bPlayTurn) {
                if (!this.getEmitter().isPlaying("CorpseDragTurn")) {
                    this.playSoundLocal("CorpseDragTurn");
                }
            } else if (this.getEmitter().isPlaying("CorpseDragTurn")) {
                this.getEmitter().stopOrTriggerSoundByName("CorpseDragTurn");
            }
        }
    }

    private void updateAttackLoopSound() {
        if (this.getEmitter().isPlaying(this.attackLoopInstance)) {
            if (this.getPrimaryHandItem() instanceof HandWeapon weapon) {
                int ammoCount = this.isUnlimitedAmmo() ? weapon.getMaxAmmo() : weapon.getCurrentAmmoCount();
                if (weapon.isAimedFirearm() && ammoCount != 0 && !weapon.isJammed()) {
                    if (GameWindow.activatedJoyPad != null && this.joypadBind != -1) {
                        boolean isAttacking = this.isCharging && !JoypadManager.instance.isRTPressed(this.joypadBind);
                        if (!this.isAiming() || !isAttacking) {
                            this.stopAttackLoopSound(false);
                        }
                    } else if (!this.isAiming() || !this.isAimKeyDown() || !GameKeyboard.isKeyDown("Attack/Click") || this.isDoShove()) {
                        this.stopAttackLoopSound(false);
                    }
                } else {
                    this.stopAttackLoopSound(false);
                }
            } else {
                this.stopAttackLoopSound(false);
            }
        }
    }

    private void updateBringToBearSound() {
        boolean bAiming = this.isWeaponReady() && this.isAiming();
        if (bAiming && !this.aimingWeaponAnimation) {
            this.aimingWeaponAnimation = true;
            if (this.getPrimaryHandItem() instanceof HandWeapon weapon && weapon.getBringToBearSound() != null) {
                this.getEmitter().playSoundImpl(weapon.getBringToBearSound(), null);
            }
        } else if (!bAiming && this.aimingWeaponAnimation) {
            this.aimingWeaponAnimation = false;
            if (this.getPrimaryHandItem() instanceof HandWeapon weapon && weapon.getAimReleaseSound() != null) {
                this.getEmitter().playSoundImpl(weapon.getAimReleaseSound(), null);
            }
        }
    }

    public boolean isPlayingAttackLoopSound(String soundName) {
        return StringUtils.equalsIgnoreCase(soundName, this.attackLoopSoundName) && this.getEmitter().isPlaying(this.attackLoopInstance);
    }

    public void startAttackLoopSound(String soundName) {
        this.stopAttackLoopSound(true);
        if (this.getPrimaryHandItem() instanceof HandWeapon weapon) {
            int ammoCount = this.isUnlimitedAmmo() ? weapon.getMaxAmmo() : weapon.getCurrentAmmoCount();
            if (weapon.isAimedFirearm() && ammoCount != 0 && !weapon.isJammed()) {
                this.attackLoopSoundName = soundName;
                this.attackLoopInstance = this.playSound(soundName);
            }
        }
    }

    private void stopAttackLoopSound(boolean bCancelPrevious) {
        this.attackLoopSoundName = null;
        if (bCancelPrevious && this.attackLoopInstancePrev != 0L) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDesc = FMODManager.instance.getParameterDescription("FirearmCancelShot");
            this.getEmitter().setParameterValue(this.attackLoopInstancePrev, parameterDesc, 1.0F);
            this.attackLoopInstancePrev = 0L;
        }

        if (this.attackLoopInstance != 0L) {
            this.getEmitter().stopSoundDelayRelease(this.attackLoopInstance);
            this.attackLoopInstancePrev = this.attackLoopInstance;
            this.attackLoopInstance = 0L;
        }
    }

    @Override
    public void playBloodSplatterSound() {
        if (!this.isDead()) {
            super.playBloodSplatterSound();
        }
    }

    private void checkVehicleContainers() {
        ArrayList<IsoPlayer.VehicleContainer> containers = this.vehicleContainerData.tempContainers;
        containers.clear();
        int x1 = PZMath.fastfloor(this.getX()) - 4;
        int y1 = PZMath.fastfloor(this.getY()) - 4;
        int x2 = PZMath.fastfloor(this.getX()) + 4;
        int y2 = PZMath.fastfloor(this.getY()) + 4;
        int chunkX1 = x1 / 8;
        int chunkY1 = y1 / 8;
        int chunkX2 = (int)Math.ceil(x2 / 8.0F);
        int chunkY2 = (int)Math.ceil(y2 / 8.0F);

        for (int y = chunkY1; y < chunkY2; y++) {
            for (int x = chunkX1; x < chunkX2; x++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        VehicleScript script = vehicle.getScript();
                        if (script != null) {
                            for (int partIndex = 0; partIndex < script.getPartCount(); partIndex++) {
                                VehicleScript.Part scriptPart = script.getPart(partIndex);
                                if (scriptPart.container != null && scriptPart.area != null && vehicle.isInArea(scriptPart.area, this)) {
                                    IsoPlayer.VehicleContainer container = this.vehicleContainerData.freeContainers.isEmpty()
                                        ? new IsoPlayer.VehicleContainer()
                                        : this.vehicleContainerData.freeContainers.pop();
                                    containers.add(container.set(vehicle, partIndex));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (containers.size() != this.vehicleContainerData.containers.size()) {
            this.vehicleContainerData.freeContainers.addAll(this.vehicleContainerData.containers);
            this.vehicleContainerData.containers.clear();
            this.vehicleContainerData.containers.addAll(containers);
            LuaEventManager.triggerEvent("OnContainerUpdate");
        } else {
            for (int ix = 0; ix < containers.size(); ix++) {
                IsoPlayer.VehicleContainer container1 = containers.get(ix);
                IsoPlayer.VehicleContainer container2 = this.vehicleContainerData.containers.get(ix);
                if (!container1.equals(container2)) {
                    this.vehicleContainerData.freeContainers.addAll(this.vehicleContainerData.containers);
                    this.vehicleContainerData.containers.clear();
                    this.vehicleContainerData.containers.addAll(containers);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                    break;
                }
            }
        }
    }

    public void setJoypadIgnoreAimUntilCentered(boolean ignore) {
        this.joypadIgnoreAimUntilCentered = ignore;
    }

    public ByteBufferWriter createPlayerStats(ByteBufferWriter b, String adminUsername) {
        b.putShort(this.getOnlineID());
        b.putUTF(adminUsername);
        b.putUTF(this.getDisplayName());
        b.putUTF(this.getDescriptor().getForename());
        b.putUTF(this.getDescriptor().getSurname());
        b.putUTF(this.getDescriptor().getCharacterProfession().toString());
        if (!StringUtils.isNullOrEmpty(this.getTagPrefix())) {
            b.putByte((byte)1);
            b.putUTF(this.getTagPrefix());
        } else {
            b.putByte((byte)0);
        }

        b.putBoolean(this.isAllChatMuted());
        b.putFloat(this.getTagColor().r);
        b.putFloat(this.getTagColor().g);
        b.putFloat(this.getTagColor().b);
        b.putByte((byte)(this.showTag ? 1 : 0));
        b.putByte((byte)(this.factionPvp ? 1 : 0));
        return b;
    }

    public String setPlayerStats(ByteBuffer bb, String adminUsername) {
        String displayName = GameWindow.ReadString(bb);
        String forname = GameWindow.ReadString(bb);
        String surname = GameWindow.ReadString(bb);
        CharacterProfession characterProfession = CharacterProfession.get(ResourceLocation.of(GameWindow.ReadString(bb)));
        CharacterProfessionDefinition characterProfessionDefinition = CharacterProfessionDefinition.getCharacterProfessionDefinition(characterProfession);
        String tag = "";
        if (bb.get() == 1) {
            tag = GameWindow.ReadString(bb);
        }

        boolean chatMuted = bb.get() == 1;
        float r = bb.getFloat();
        float g = bb.getFloat();
        float b = bb.getFloat();
        String txt = "";
        this.setTagColor(new ColorInfo(r, g, b, 1.0F));
        this.setTagPrefix(tag);
        this.showTag = bb.get() == 1;
        this.factionPvp = bb.get() == 1;
        if (!forname.equals(this.getDescriptor().getForename())) {
            if (GameServer.server) {
                txt = adminUsername + " Changed " + displayName + " forname in " + forname;
            } else {
                txt = "Changed your forname in " + forname;
            }
        }

        this.getDescriptor().setForename(forname);
        if (!surname.equals(this.getDescriptor().getSurname())) {
            if (GameServer.server) {
                txt = adminUsername + " Changed " + displayName + " surname in " + surname;
            } else {
                txt = "Changed your surname in " + surname;
            }
        }

        this.getDescriptor().setSurname(surname);
        if (!characterProfession.equals(this.getDescriptor().getCharacterProfession())) {
            if (GameServer.server) {
                txt = adminUsername + " Changed " + displayName + " profession to " + characterProfessionDefinition.getUIName();
            } else {
                txt = "Changed your profession in " + characterProfessionDefinition.getUIName();
            }
        }

        this.getDescriptor().setCharacterProfession(characterProfession);
        if (!this.getDisplayName().equals(displayName)) {
            if (GameServer.server) {
                txt = adminUsername + " Changed display name \"" + this.getDisplayName() + "\" to \"" + displayName + "\"";
                ServerWorldDatabase.instance.updateDisplayName(this.username, displayName);
            } else {
                txt = "Changed your display name to " + displayName;
            }

            this.setDisplayName(displayName);
        }

        if (chatMuted != this.isAllChatMuted()) {
            if (chatMuted) {
                if (GameServer.server) {
                    txt = adminUsername + " Banned " + displayName + " from using /all chat";
                } else {
                    txt = "Banned you from using /all chat";
                }
            } else if (GameServer.server) {
                txt = adminUsername + " Allowed " + displayName + " to use /all chat";
            } else {
                txt = "Now allowed you to use /all chat";
            }
        }

        this.setAllChatMuted(chatMuted);
        if (GameServer.server && !txt.isEmpty()) {
            LoggerManager.getLogger("admin").write(txt);
        }

        if (GameClient.client) {
            LuaEventManager.triggerEvent("OnMiniScoreboardUpdate");
        }

        return txt;
    }

    public boolean isAllChatMuted() {
        return this.allChatMuted;
    }

    public void setAllChatMuted(boolean allChatMuted) {
        this.allChatMuted = allChatMuted;
    }

    @Deprecated
    public String getAccessLevel() {
        return this.role == null ? "none" : this.role.getName();
    }

    public Role getRole() {
        return this.role;
    }

    public boolean isAccessLevel(String level) {
        return this.getAccessLevel().equalsIgnoreCase(level);
    }

    public void setRole(String newLvl) {
        if (GameClient.client) {
            Role newRole = Roles.getRole(newLvl.trim().toLowerCase());
            GameClient.SendCommandToServer("/setaccesslevel \"" + this.username + "\" \"" + newRole.getName() + "\"");
        }
    }

    public void addMechanicsItem(String itemid, VehiclePart part, Long milli) {
        int minExp = 1;
        int maxExp = 1;
        if (this.mechanicsItem.get(Long.parseLong(itemid)) == null && !GameClient.client) {
            if (part.getTable("uninstall") != null && part.getTable("uninstall").rawget("skills") != null) {
                String[] skills = ((String)part.getTable("uninstall").rawget("skills")).split(";");

                for (String fSkill : skills) {
                    if (fSkill.contains("Mechanics")) {
                        int number = Integer.parseInt(fSkill.split(":")[1]);
                        if (number >= 6) {
                            minExp = 3;
                            maxExp = 7;
                        } else if (number >= 4) {
                            minExp = 3;
                            maxExp = 5;
                        } else if (number >= 2) {
                            minExp = 2;
                            maxExp = 4;
                        } else if (Rand.Next(3) == 0) {
                            minExp = 2;
                            maxExp = 2;
                        }

                        minExp *= 2;
                        maxExp *= 2;
                    }
                }
            }

            if (GameServer.server) {
                GameServer.addXp(this, PerkFactory.Perks.Mechanics, Rand.Next(minExp, maxExp));
            } else {
                this.getXp().AddXP(PerkFactory.Perks.Mechanics, (float)Rand.Next(minExp, maxExp));
            }
        }

        this.mechanicsItem.put(Long.parseLong(itemid), milli);
    }

    private void updateTemperatureCheck() {
        int hypo = this.moodles.getMoodleLevel(MoodleType.HYPOTHERMIA);
        if (this.hypothermiaCache == -1 || this.hypothermiaCache != hypo) {
            if (hypo >= 3 && hypo > this.hypothermiaCache && this.isAsleep() && !this.forceWakeUp) {
                this.forceAwake();
            }

            this.hypothermiaCache = hypo;
        }

        int hyper = this.moodles.getMoodleLevel(MoodleType.HYPERTHERMIA);
        if (this.hyperthermiaCache == -1 || this.hyperthermiaCache != hyper) {
            if (hyper >= 3 && hyper > this.hyperthermiaCache && this.isAsleep() && !this.forceWakeUp) {
                this.forceAwake();
            }

            this.hyperthermiaCache = hyper;
        }
    }

    public float getZombieRelevenceScore(IsoZombie z) {
        if (z.getCurrentSquare() == null) {
            return -10000.0F;
        } else {
            float score = 0.0F;
            if (z.getCurrentSquare().getCanSee(this.playerIndex)) {
                score += 100.0F;
            } else if (z.getCurrentSquare().isCouldSee(this.playerIndex)) {
                score += 10.0F;
            }

            if (z.getCurrentSquare().getRoom() != null && this.current.getRoom() == null) {
                score -= 20.0F;
            }

            if (z.getCurrentSquare().getRoom() == null && this.current.getRoom() != null) {
                score -= 20.0F;
            }

            if (z.getCurrentSquare().getRoom() != this.current.getRoom()) {
                score -= 20.0F;
            }

            float d = z.DistTo(this);
            score -= d;
            if (d < 20.0F) {
                score += 300.0F;
            }

            if (d < 15.0F) {
                score += 300.0F;
            }

            if (d < 10.0F) {
                score += 1000.0F;
            }

            if (z.getTargetAlpha() < 1.0F && score > 0.0F) {
                score *= z.getTargetAlpha();
            }

            return score;
        }
    }

    @Override
    public BaseVisual getVisual() {
        return this.baseVisual;
    }

    @Override
    public HumanVisual getHumanVisual() {
        return Type.tryCastTo(this.baseVisual, HumanVisual.class);
    }

    @Override
    public AnimalVisual getAnimalVisual() {
        return Type.tryCastTo(this.baseVisual, AnimalVisual.class);
    }

    @Override
    public String getAnimalType() {
        return null;
    }

    @Override
    public float getAnimalSize() {
        return 1.0F;
    }

    @Override
    public ItemVisuals getItemVisuals() {
        return this.itemVisuals;
    }

    @Override
    public void getItemVisuals(ItemVisuals itemVisuals) {
        if (this.remote && !GameServer.server) {
            itemVisuals.clear();
            itemVisuals.addAll(this.itemVisuals);
        } else {
            this.getWornItems().getItemVisuals(itemVisuals);
        }
    }

    @Override
    public void dressInNamedOutfit(String outfitName) {
        if (this.getHumanVisual() != null) {
            this.getHumanVisual().dressInNamedOutfit(outfitName, this.itemVisuals);
            this.onClothingOutfitPreviewChanged();
        }
    }

    @Override
    public void dressInClothingItem(String itemGUID) {
        this.getHumanVisual().dressInClothingItem(itemGUID, this.itemVisuals);
        this.onClothingOutfitPreviewChanged();
    }

    private void onClothingOutfitPreviewChanged() {
        if (this.isLocalPlayer()) {
            this.getInventory().clear();
            this.wornItems.setFromItemVisuals(this.itemVisuals);
            this.wornItems.addItemsToItemContainer(this.getInventory());
            this.itemVisuals.clear();
            this.onWornItemsChanged();
        }
    }

    @Override
    public void onWornItemsChanged() {
        this.resetModel();
        super.onWornItemsChanged();
        this.parameterShoeType.setShoeType(null);
    }

    @Override
    public void actionStateChanged(ActionContext sender) {
        super.actionStateChanged(sender);
    }

    public Vector2 getLastAngle() {
        return this.lastAngle;
    }

    public void setLastAngle(Vector2 lastAngle) {
        this.lastAngle.set(lastAngle);
    }

    public int getDialogMood() {
        return this.dialogMood;
    }

    public void setDialogMood(int DialogMood) {
        this.dialogMood = DialogMood;
    }

    public int getPing() {
        return this.ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public IsoMovingObject getDragObject() {
        return this.dragObject;
    }

    public void setDragObject(IsoMovingObject DragObject) {
        this.dragObject = DragObject;
    }

    public float getAsleepTime() {
        return this.asleepTime;
    }

    public void setAsleepTime(float AsleepTime) {
        this.asleepTime = AsleepTime;
    }

    public Stack<IsoMovingObject> getSpottedList() {
        return this.spottedList;
    }

    public int getTicksSinceSeenZombie() {
        return this.ticksSinceSeenZombie;
    }

    public void setTicksSinceSeenZombie(int TicksSinceSeenZombie) {
        this.ticksSinceSeenZombie = TicksSinceSeenZombie;
    }

    public boolean isWaiting() {
        return this.waiting;
    }

    public void setWaiting(boolean Waiting) {
        this.waiting = Waiting;
    }

    public IsoSurvivor getDragCharacter() {
        return this.dragCharacter;
    }

    public void setDragCharacter(IsoSurvivor DragCharacter) {
        this.dragCharacter = DragCharacter;
    }

    public float getHeartDelay() {
        return this.heartDelay;
    }

    public void setHeartDelay(float heartDelay) {
        this.heartDelay = heartDelay;
    }

    public float getHeartDelayMax() {
        return this.heartDelayMax;
    }

    public void setHeartDelayMax(int heartDelayMax) {
        this.heartDelayMax = heartDelayMax;
    }

    @Override
    public double getHoursSurvived() {
        return this.hoursSurvived;
    }

    public void setHoursSurvived(double hrs) {
        this.hoursSurvived = hrs;
    }

    public float getMaxWeightDelta() {
        return this.maxWeightDelta;
    }

    public void setMaxWeightDelta(float maxWeightDelta) {
        this.maxWeightDelta = maxWeightDelta;
    }

    public boolean isbChangeCharacterDebounce() {
        return this.changeCharacterDebounce;
    }

    public void setbChangeCharacterDebounce(boolean changeCharacterDebounce) {
        this.changeCharacterDebounce = changeCharacterDebounce;
    }

    public int getFollowID() {
        return this.followId;
    }

    public void setFollowID(int followId) {
        this.followId = followId;
    }

    public boolean isbSeenThisFrame() {
        return this.seenThisFrame;
    }

    public void setbSeenThisFrame(boolean seenThisFrame) {
        this.seenThisFrame = seenThisFrame;
    }

    public boolean isbCouldBeSeenThisFrame() {
        return this.couldBeSeenThisFrame;
    }

    public void setbCouldBeSeenThisFrame(boolean couldBeSeenThisFrame) {
        this.couldBeSeenThisFrame = couldBeSeenThisFrame;
    }

    public float getTimeSinceLastStab() {
        return this.timeSinceLastStab;
    }

    public void setTimeSinceLastStab(float timeSinceLastStab) {
        this.timeSinceLastStab = timeSinceLastStab;
    }

    public Stack<IsoMovingObject> getLastSpotted() {
        return this.lastSpotted;
    }

    public void setLastSpotted(Stack<IsoMovingObject> LastSpotted) {
        this.lastSpotted = LastSpotted;
    }

    public int getClearSpottedTimer() {
        return this.clearSpottedTimer;
    }

    public void setClearSpottedTimer(int ClearSpottedTimer) {
        this.clearSpottedTimer = ClearSpottedTimer;
    }

    public boolean IsRunning() {
        return this.isRunning();
    }

    public void InitSpriteParts() {
    }

    public String getTagPrefix() {
        return this.tagPrefix;
    }

    public void setTagPrefix(String newTag) {
        this.tagPrefix = newTag;
    }

    public ColorInfo getTagColor() {
        return this.tagColor;
    }

    public void setTagColor(ColorInfo tagColor) {
        this.tagColor.set(tagColor);
    }

    public String getDisplayName() {
        if (this.displayName == null || this.displayName.isEmpty()) {
            this.displayName = this.getUsername();
        }

        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isSeeNonPvpZone() {
        return this.seeNonPvpZone || DebugOptions.instance.multiplayer.debug.seeNonPvpZones.getValue();
    }

    public boolean isSeeDesignationZone() {
        return this.seeDesignationZone;
    }

    public void setSeeDesignationZone(boolean seeMetaAnimalZone) {
        this.seeDesignationZone = seeMetaAnimalZone;
    }

    public void addSelectedZoneForHighlight(Double id) {
        if (!this.selectedZonesForHighlight.contains(id)) {
            this.selectedZonesForHighlight.add(id);
        }
    }

    public void setSelectedZoneForHighlight(Double id) {
        this.selectedZoneForHighlight = id;
    }

    public Double getSelectedZoneForHighlight() {
        return this.selectedZoneForHighlight;
    }

    public ArrayList<Double> getSelectedZonesForHighlight() {
        return this.selectedZonesForHighlight;
    }

    public void resetSelectedZonesForHighlight() {
        this.selectedZonesForHighlight.clear();
    }

    public void setSeeNonPvpZone(boolean seeNonPvpZone) {
        this.seeNonPvpZone = seeNonPvpZone;
    }

    public boolean checkZonesInterception(int x1, int x2, int y1, int y2) {
        ArrayList<NonPvpZone> nonPvpZones = NonPvpZone.getAllZones();

        for (int i = 0; i < nonPvpZones.size(); i++) {
            NonPvpZone npz = nonPvpZones.get(i);
            if (x1 < npz.getX2() && x2 > npz.getX() && y1 < npz.getY2() && y2 > npz.getY()) {
                return true;
            }
        }

        return false;
    }

    public boolean isShowTag() {
        return this.showTag;
    }

    public void setShowTag(boolean show) {
        this.showTag = show;
    }

    public boolean isFactionPvp() {
        return this.factionPvp;
    }

    public void setFactionPvp(boolean pvp) {
        this.factionPvp = pvp;
    }

    public boolean isForceAim() {
        return this.forceAim;
    }

    public void setForceAim(boolean forceAim) {
        this.forceAim = forceAim;
    }

    public boolean toggleForceAim() {
        this.forceAim = !this.forceAim;
        return this.forceAim;
    }

    public boolean isForceSprint() {
        return this.forceSprint;
    }

    public void setForceSprint(boolean forceSprint) {
        this.forceSprint = forceSprint;
    }

    public boolean toggleForceSprint() {
        this.forceSprint = !this.forceSprint;
        return this.forceSprint;
    }

    public boolean isForceRun() {
        return this.forceRun;
    }

    public void setForceRun(boolean forceRun) {
        this.forceRun = forceRun;
    }

    public boolean toggleForceRun() {
        this.forceRun = !this.forceRun;
        return this.forceRun;
    }

    public boolean isForceOverrideAnim() {
        return this.forceOverrideAnim;
    }

    public void setForceOverrideAnim(boolean forceOverride) {
        this.forceOverrideAnim = forceOverride;
    }

    public Long getMechanicsItem(String itemId) {
        return this.mechanicsItem.get(Long.parseLong(itemId));
    }

    public boolean isWearingNightVisionGoggles() {
        return this.isWearingNightVisionGoggles;
    }

    public void setWearingNightVisionGoggles(boolean b) {
        this.isWearingNightVisionGoggles = b;
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        super.OnAnimEvent(sender, track, event);
        if (!this.characterActions.isEmpty()) {
            BaseAction action = this.characterActions.get(0);
            action.OnAnimEvent(event);
        }
    }

    @Override
    public void setAddedToModelManager(ModelManager modelManager, boolean isAdded) {
        super.setAddedToModelManager(modelManager, isAdded);
        if (isAdded) {
            DebugFileWatcher.instance.add(this.setClothingTriggerWatcher);
        } else {
            DebugFileWatcher.instance.remove(this.setClothingTriggerWatcher);
        }
    }

    @Override
    public boolean isTimedActionInstant() {
        return (GameClient.client || GameServer.server) && this.isAccessLevel("None") ? false : super.isTimedActionInstant();
    }

    @Override
    public boolean isSkeleton() {
        return false;
    }

    @Override
    public void addWorldSoundUnlessInvisible(int radius, int volume, boolean bStressHumans) {
        if (!this.isGhostMode()) {
            super.addWorldSoundUnlessInvisible(radius, volume, bStressHumans);
        }
    }

    private void updateFootInjuries() {
        InventoryItem shoes = this.getWornItems().getItem(ItemBodyLocation.SHOES);
        if (shoes == null || shoes.getCondition() <= 0) {
            if (this.getCurrentSquare() != null) {
                if (this.getCurrentSquare().getBrokenGlass() != null) {
                    BodyPartType partType = BodyPartType.FromIndex(
                        Rand.Next(BodyPartType.ToIndex(BodyPartType.Foot_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1)
                    );
                    BodyPart part = this.getBodyDamage().getBodyPart(partType);
                    if (!part.isDeepWounded() || !part.haveGlass()) {
                        part.generateDeepShardWound();
                        this.playerVoiceSound("PainFromGlassCut");
                    }
                }

                int modifier = 0;
                boolean inForest = this.getCurrentSquare().getZone() != null
                    && (this.getCurrentSquare().getZone().getType().equals("Forest") || this.getCurrentSquare().getZone().getType().equals("DeepForest"));
                IsoObject floor = this.getCurrentSquare().getFloor();
                if (floor != null && floor.getSprite() != null && floor.getSprite().getName() != null) {
                    String floorName = floor.getSprite().getName();
                    if (floorName.contains("blends_natural_01") && inForest) {
                        modifier = 2;
                    } else if (!floorName.contains("blends_natural_01") && this.getCurrentSquare().getBuilding() == null) {
                        modifier = 1;
                    }
                }

                if (modifier != 0) {
                    if (this.isWalking && !this.isRunning() && !this.isSprinting()) {
                        this.footInjuryTimer += modifier;
                    } else if (this.isRunning() && !this.isSprinting()) {
                        this.footInjuryTimer += modifier + 2;
                    } else {
                        if (!this.isSprinting()) {
                            if (this.footInjuryTimer > 0 && Rand.Next(3) == 0) {
                                this.footInjuryTimer--;
                            }

                            return;
                        }

                        this.footInjuryTimer += modifier + 5;
                    }

                    if (Rand.Next(Rand.AdjustForFramerate(8500 - this.footInjuryTimer)) <= 0) {
                        this.footInjuryTimer = 0;
                        BodyPartType partType = BodyPartType.FromIndex(
                            Rand.Next(BodyPartType.ToIndex(BodyPartType.Foot_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1)
                        );
                        BodyPart part = this.getBodyDamage().getBodyPart(partType);
                        if (part.getScratchTime() > 30.0F) {
                            if (!part.isCut()) {
                                part.setCut(true);
                                part.setCutTime(Rand.Next(1.0F, 3.0F));
                            } else {
                                part.setCutTime(part.getCutTime() + Rand.Next(1.0F, 3.0F));
                            }
                        } else {
                            if (!part.scratched()) {
                                part.setScratched(true, true);
                                part.setScratchTime(Rand.Next(1.0F, 3.0F));
                            } else {
                                part.setScratchTime(part.getScratchTime() + Rand.Next(1.0F, 3.0F));
                            }

                            if (part.getScratchTime() > 20.0F && part.getBleedingTime() == 0.0F) {
                                part.setBleedingTime(Rand.Next(3.0F, 10.0F));
                            }
                        }
                    }
                }
            }
        }
    }

    public int getMoodleLevel(MoodleType type) {
        return this.getMoodles().getMoodleLevel(type);
    }

    public boolean isAttackStarted() {
        return this.attackStarted;
    }

    public void setAttackStarted(boolean attackStarted) {
        this.attackStarted = attackStarted;
    }

    @Override
    public boolean isBehaviourMoving() {
        return this.hasPath() || super.isBehaviourMoving();
    }

    public boolean isJustMoved() {
        return !GameServer.server
            ? this.justMoved
            : Math.abs(this.getX() - this.networkAi.targetX) > 0.2F || Math.abs(this.getY() - this.networkAi.targetY) > 0.2F;
    }

    public void setJustMoved(boolean val) {
        this.justMoved = val;
        if (GameClient.client && !this.networkAi.moved && this.justMoved) {
            this.networkAi.moved = true;
            GameClient.connection.setReady(true);
        }
    }

    @Override
    public boolean isPlayerMoving() {
        return this.isPlayerMoving;
    }

    @Override
    public float getTimedActionTimeModifier() {
        return this.getBodyDamage().getThermoregulator() != null ? this.getBodyDamage().getThermoregulator().getTimedActionTimeModifier() : 1.0F;
    }

    public boolean isLookingWhileInVehicle() {
        return this.getVehicle() != null && this.lookingWhileInVehicle;
    }

    public void setInitiateAttack(boolean initiate) {
        this.initiateAttack = initiate;
    }

    public boolean isInitiateAttack() {
        return this.initiateAttack;
    }

    public boolean isIgnoreInputsForDirection() {
        return this.ignoreInputsForDirection;
    }

    public void setIgnoreInputsForDirection(boolean ignoreInputsForDirection) {
        this.ignoreInputsForDirection = ignoreInputsForDirection;
    }

    public boolean isIgnoreContextKey() {
        return this.ignoreContextKey;
    }

    public void setIgnoreContextKey(boolean ignoreContextKey) {
        this.ignoreContextKey = ignoreContextKey;
    }

    public boolean isIgnoreAutoVault() {
        return this.ignoreAutoVault;
    }

    public void setIgnoreAutoVault(boolean ignoreAutoVault) {
        this.ignoreAutoVault = ignoreAutoVault;
    }

    public boolean isAllowSprint() {
        return this.allowSprint;
    }

    public void setAllowSprint(boolean allowSprint) {
        this.allowSprint = allowSprint;
    }

    public boolean isAllowRun() {
        return this.allowRun;
    }

    public void setAllowRun(boolean allowRun) {
        this.allowRun = allowRun;
    }

    public boolean isAttackType(AttackType attackType) {
        return this.attackType == attackType;
    }

    public AttackType getAttackType() {
        return this.attackType;
    }

    private String getAttackTypeAnimationKey() {
        return this.attackType.toString();
    }

    public void setAttackType(AttackType attackType) {
        this.attackType = attackType;
    }

    public boolean canSeeAll() {
        return this.getCheats().isSet(CheatType.CAN_SEE_EVERYONE);
    }

    public void setCanSeeAll(boolean b) {
        if (!Role.hasCapability(this, Capability.CanSeeAll)) {
            this.getCheats().set(CheatType.CAN_SEE_EVERYONE, false);
        } else {
            this.getCheats().set(CheatType.CAN_SEE_EVERYONE, b);
        }
    }

    public boolean isCheatPlayerSeeEveryone() {
        return DebugOptions.instance.cheat.player.seeEveryone.getValue();
    }

    public float getRelevantAndDistance(float x, float y, float RelevantRange) {
        return Math.abs(this.getX() - x) <= RelevantRange * 8.0F && Math.abs(this.getY() - y) <= RelevantRange * 8.0F
            ? IsoUtils.DistanceTo(this.getX(), this.getY(), x, y)
            : Float.POSITIVE_INFINITY;
    }

    public boolean canHearAll() {
        return this.getCheats().isSet(CheatType.CAN_HEAR_EVERYONE);
    }

    public void setCanHearAll(boolean b) {
        if (!Role.hasCapability(this, Capability.CanHearAll)) {
            this.getCheats().set(CheatType.CAN_HEAR_EVERYONE, false);
        } else {
            this.getCheats().set(CheatType.CAN_HEAR_EVERYONE, b);
        }
    }

    public ArrayList<String> getAlreadyReadBook() {
        return this.alreadyReadBook;
    }

    public void setMoodleCantSprint(boolean b) {
        this.moodleCantSprint = b;
    }

    public void setAttackFromBehind(boolean attackFromBehind) {
        this.attackFromBehind = attackFromBehind;
    }

    public boolean isAttackFromBehind() {
        return this.attackFromBehind;
    }

    public float getDamageFromHitByACar(float vehicleSpeed) {
        float modifier = switch (SandboxOptions.instance.damageToPlayerFromHitByACar.getValue()) {
            case 1 -> 0.0F;
            case 2 -> 0.5F;
            default -> 1.0F;
            case 4 -> 2.0F;
            case 5 -> 5.0F;
        };
        float damage = vehicleSpeed * modifier;
        if (damage > 0.0F) {
            int damagedBodyPartNum = (int)(2.0F + damage * 0.07F);

            for (int j = 0; j < damagedBodyPartNum; j++) {
                int bodyPart = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
                BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(bodyPart));
                float realDamage = Math.max(Rand.Next(damage - 15.0F, damage), 5.0F);
                if (this.characterTraits.get(CharacterTrait.FAST_HEALER)) {
                    realDamage *= 0.8F;
                } else if (this.characterTraits.get(CharacterTrait.SLOW_HEALER)) {
                    realDamage *= 1.2F;
                }

                switch (SandboxOptions.instance.injurySeverity.getValue()) {
                    case 1:
                        realDamage *= 0.5F;
                        break;
                    case 3:
                        realDamage *= 1.5F;
                }

                realDamage *= 0.9F;
                part.AddDamage(realDamage);
                if (realDamage > 40.0F && Rand.Next(12) == 0) {
                    part.generateDeepWound();
                }

                if (realDamage > 10.0F && Rand.Next(100) <= 10 && SandboxOptions.instance.boneFracture.getValue()) {
                    part.generateFracture(Rand.Next(Rand.Next(10.0F, realDamage + 10.0F), Rand.Next(realDamage + 20.0F, realDamage + 30.0F)));
                }

                if (realDamage > 30.0F
                    && Rand.Next(100) <= 80
                    && SandboxOptions.instance.boneFracture.getValue()
                    && bodyPart == BodyPartType.ToIndex(BodyPartType.Head)) {
                    part.generateFracture(Rand.Next(Rand.Next(10.0F, realDamage + 10.0F), Rand.Next(realDamage + 20.0F, realDamage + 30.0F)));
                }

                if (realDamage > 10.0F
                    && Rand.Next(100) <= 60
                    && SandboxOptions.instance.boneFracture.getValue()
                    && bodyPart > BodyPartType.ToIndex(BodyPartType.Groin)) {
                    part.generateFracture(Rand.Next(Rand.Next(10.0F, realDamage + 20.0F), Rand.Next(realDamage + 30.0F, realDamage + 40.0F)));
                }
            }

            this.getBodyDamage().Update();
        }

        this.addBlood(vehicleSpeed);
        if (GameServer.server) {
            this.getNetworkCharacterAI().syncDamage();
        }

        return damage;
    }

    @Override
    public float Hit(BaseVehicle vehicle, float speed, boolean isHitFromBehind, float hitDirX, float hitDirY) {
        float damage = this.doBeatenVehicle(speed);
        super.Hit(vehicle, speed, isHitFromBehind, hitDirX, hitDirY);
        return damage;
    }

    @Override
    public void Kill(IsoGameCharacter killer) {
        if (!this.isOnKillDone()) {
            if (GameServer.server) {
                if (this.isOnFire()) {
                    ConnectionQueueStatistic.getInstance().playersKilledByFireToday.increase();
                }

                if (killer instanceof IsoZombie) {
                    ConnectionQueueStatistic.getInstance().playersKilledByZombieToday.increase();
                }

                if (killer instanceof IsoPlayer) {
                    ConnectionQueueStatistic.getInstance().playersKilledByPlayerToday.increase();
                }
            }

            super.Kill(killer);
            this.getBodyDamage().setOverallBodyHealth(0.0F);
            if (killer == null) {
                this.DoDeath(null, null);
            } else {
                this.DoDeath(killer.getUseHandWeapon(), killer);
            }
        }
    }

    @Override
    public IsoDeadBody becomeCorpse() {
        if (this.isOnDeathDone()) {
            return null;
        } else {
            super.becomeCorpse();
            IsoDeadBody body = new IsoDeadBody(this);
            if (!GameClient.client && this.shouldBecomeZombieAfterDeath()) {
                body.reanimateLater();
            }

            if (GameServer.server) {
                this.getNetworkCharacterAI().syncDamage();
                GameServer.sendCharacterDeath(body);
            }

            return body;
        }
    }

    @Override
    public void preupdate() {
        if (GameClient.client) {
            if (this.networkAi.isVehicleHitTimeout()) {
                this.networkAi.hitByVehicle();
            }

            if (this.networkAi.isDeadBodyTimeout()) {
                this.networkAi.becomeCorpse();
            }

            if (!this.isLocal() && this.isKnockedDown() && !this.isOnFloor()) {
                HitReactionNetworkAI hitReaction = this.getHitReactionNetworkAI();
                if (hitReaction.isSetup() && !hitReaction.isStarted()) {
                    hitReaction.start();
                }
            }
        }

        super.preupdate();
    }

    @Override
    public HitReactionNetworkAI getHitReactionNetworkAI() {
        return this.networkAi.hitReaction;
    }

    @Override
    public NetworkCharacterAI getNetworkCharacterAI() {
        return this.networkAi;
    }

    public void setFishingStage(String stage) {
        if (!this.isVariable("FishingStage", stage)) {
            this.setVariable("FishingStage", stage);
            if (GameClient.client) {
                StateManager.enterState(this, FishingState.instance());
            }
        }
    }

    public void setFitnessSpeed() {
        this.clearVariable("FitnessStruggle");
        float speed = this.getPerkLevel(PerkFactory.Perks.Fitness) / 5.0F / 1.1F - this.getMoodleLevel(MoodleType.ENDURANCE) / 20.0F;
        if (speed > 1.5F) {
            speed = 1.5F;
        }

        if (speed < 0.85F) {
            speed = 1.0F;
            this.setVariable("FitnessStruggle", true);
        }

        this.setVariable("FitnessSpeed", speed);
    }

    @Override
    public boolean isLocal() {
        return super.isLocal() || this.isLocalPlayer();
    }

    public boolean isClimbOverWallSuccess() {
        return this.climbOverWallSuccess;
    }

    public void setClimbOverWallSuccess(boolean climbOverWallSuccess) {
        this.climbOverWallSuccess = climbOverWallSuccess;
    }

    public boolean isClimbOverWallStruggle() {
        return this.climbOverWallStruggle;
    }

    public void setClimbOverWallStruggle(boolean climbOverWallStruggle) {
        this.climbOverWallStruggle = climbOverWallStruggle;
    }

    @Override
    public boolean isVehicleCollisionActive(BaseVehicle testVehicle) {
        if (!super.isVehicleCollisionActive(testVehicle)) {
            return false;
        } else if (this.isGodMod()) {
            return false;
        } else {
            IsoPlayer driver = GameClient.IDToPlayerMap.get(this.vehicle4testCollision.getNetPlayerId());
            if (!CombatManager.checkPVP(driver, this)) {
                return false;
            } else if (SandboxOptions.instance.damageToPlayerFromHitByACar.getValue() < 1) {
                return false;
            } else {
                return this.getVehicle() == testVehicle
                    ? false
                    : !this.isCurrentState(PlayerFallDownState.instance())
                        && !this.isCurrentState(PlayerFallingState.instance())
                        && !this.isCurrentState(PlayerKnockedDown.instance());
            }
        }
    }

    @Override
    public boolean isSkipResolveCollision() {
        if (super.isSkipResolveCollision()) {
            return true;
        } else {
            return this.isLocal()
                ? false
                : this.isCurrentState(PlayerFallDownState.instance())
                    || this.isCurrentState(BumpedState.instance())
                    || this.isCurrentState(PlayerKnockedDown.instance())
                    || this.isCurrentState(PlayerHitReactionState.instance())
                    || this.isCurrentState(PlayerHitReactionPVPState.instance())
                    || this.isCurrentState(PlayerOnGroundState.instance());
        }
    }

    public boolean isShowMPInfos() {
        return this.getCheats().isSet(CheatType.SHOW_MP_INFO);
    }

    public void setShowMPInfos(boolean b) {
        this.getCheats().set(CheatType.SHOW_MP_INFO, b);
    }

    public MusicIntensityEvents getMusicIntensityEvents() {
        return this.musicIntensityEvents;
    }

    private void updateMusicIntensityEvents() {
        boolean inside = this.getCurrentSquare() != null && this.getCurrentSquare().isInARoom();
        if (inside) {
            this.triggerMusicIntensityEvent("InsideBuilding");
        }

        this.musicIntensityEvents.update();
    }

    public void triggerMusicIntensityEvent(String id) {
        MusicIntensityConfig.getInstance().triggerEvent(id, this.getMusicIntensityEvents());
    }

    public MusicThreatStatuses getMusicThreatStatuses() {
        return this.musicThreatStatuses;
    }

    private void updateMusicThreatStatuses() {
        this.musicThreatStatuses.update();
    }

    public void addAttachedAnimal(IsoAnimal anim) {
        this.attachedAnimals.add(anim);
    }

    public ArrayList<IsoAnimal> getAttachedAnimals() {
        return this.attachedAnimals;
    }

    public void removeAttachedAnimal(IsoAnimal animal) {
        this.attachedAnimals.remove(animal);
    }

    public void removeAllAttachedAnimals() {
        for (int i = 0; i < this.attachedAnimals.size(); i++) {
            this.attachedAnimals.get(i).getData().attachedPlayer = null;
        }

        this.attachedAnimals.clear();
    }

    public void lureAnimal(InventoryItem item) {
        DesignationZoneAnimal dZone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ());
        if (dZone != null) {
            ArrayList<IsoAnimal> animals = dZone.getAnimals();

            for (int i = 0; i < animals.size(); i++) {
                animals.get(i).tryLure(this, item);
            }
        }

        int radius = 10;

        for (int x = this.getSquare().x - 10; x < this.getSquare().x + 10; x++) {
            for (int y = this.getSquare().y - 10; y < this.getSquare().y + 10; y++) {
                IsoGridSquare sq = this.getSquare().getCell().getGridSquare(x, y, this.getSquare().z);
                if (sq != null) {
                    ArrayList<IsoAnimal> animals = sq.getAnimals();

                    for (int i = 0; i < animals.size(); i++) {
                        animals.get(i).tryLure(this, item);
                    }
                }
            }
        }
    }

    public ArrayList<IsoAnimal> getLuredAnimals() {
        return this.luredAnimals;
    }

    public void stopLuringAnimals(boolean eatFood) {
        if (this.getPrimaryHandItem() != null) {
            this.setIsLuringAnimals(false);

            for (int i = 0; i < this.getLuredAnimals().size(); i++) {
                IsoAnimal animal = this.getLuredAnimals().get(i);
                if (eatFood) {
                    animal.eatFromLured(this, this.getPrimaryHandItem());
                    animal.getData().eatFood(this.getPrimaryHandItem());
                }

                animal.cancelLuring();
            }
        }
    }

    public void setIsLuringAnimals(boolean luring) {
        this.isLuringAnimals = luring;
    }

    public int getVoiceType() {
        return this.getDescriptor().getVoiceType();
    }

    public void setVoiceType(int voiceType) {
        this.getDescriptor().setVoiceType(PZMath.clamp(voiceType, 0, 3));
    }

    public void setVoicePitch(float voicePitch) {
        this.getDescriptor().setVoicePitch(PZMath.clamp(voicePitch, -100.0F, 100.0F));
    }

    public boolean isFarming() {
        return this.isFarming;
    }

    public void setIsFarming(boolean isFarmingBool) {
        this.isFarming = isFarmingBool;
    }

    public boolean tooDarkToRead() {
        if (!this.isLocalPlayer()) {
            return false;
        } else if (this.getSquare() == null) {
            return false;
        } else {
            int playerNum = this.getPlayerNum();
            if ((this.getPrimaryHandItem() == null || !this.getPrimaryHandItem().isEmittingLight())
                && (this.getSecondaryHandItem() == null || !this.getSecondaryHandItem().isEmittingLight())) {
                for (int i = 0; i < this.getAttachedItems().size(); i++) {
                    if (Objects.requireNonNull(this.getAttachedItems().getItemByIndex(i)).isEmittingLight()) {
                        return false;
                    }
                }

                float light = this.getSquare().getLightLevel(playerNum);
                if (this.getVehicle() != null && (light / this.getWornItemsVisionModifier() + light) / 2.0F < 0.43F) {
                    BaseVehicle vehicle = this.getVehicle();
                    return vehicle.getBatteryCharge() == 0.0F;
                } else {
                    return (light / this.getWornItemsVisionModifier() + light) / 2.0F < 0.43F;
                }
            } else {
                return false;
            }
        }
    }

    public boolean isWalking() {
        return this.isWalking;
    }

    public boolean isInvPageDirty() {
        return this.invPageDirty;
    }

    public void setInvPageDirty(boolean b) {
        this.invPageDirty = b;
    }

    public float getVoicePitch() {
        return this.getDescriptor().getVoicePitch();
    }

    public void setCombatSpeed(float combatSpeed) {
        this.combatSpeed = combatSpeed;
    }

    public float getCombatSpeed() {
        return this.combatSpeed;
    }

    public boolean isMeleePressed() {
        return this.meleePressed;
    }

    public boolean isGrapplePressed() {
        return this.grapplePressed;
    }

    public void setRole(Role newRole) {
        if (!newRole.hasCapability(Capability.ToggleWriteRoleNameAbove)) {
            this.setShowAdminTag(false);
        }

        if (!newRole.hasCapability(Capability.UseZombieDontAttackCheat)) {
            this.setZombiesDontAttack(false);
        }

        if (!newRole.hasCapability(Capability.ToggleGodModHimself)) {
            this.setGodMod(false);
        }

        if (!newRole.hasCapability(Capability.ToggleInvisibleHimself)) {
            this.setInvisible(false);
        }

        if (!newRole.hasCapability(Capability.ToggleUnlimitedEndurance)) {
            this.setUnlimitedEndurance(false);
        }

        if (!newRole.hasCapability(Capability.ToggleUnlimitedAmmo)) {
            this.setUnlimitedAmmo(false);
        }

        if (!newRole.hasCapability(Capability.ToggleKnowAllRecipes)) {
            this.setKnowAllRecipes(false);
        }

        if (!newRole.hasCapability(Capability.ToggleUnlimitedCarry)) {
            this.setUnlimitedCarry(false);
        }

        if (!newRole.hasCapability(Capability.UseMovablesCheat)) {
            this.setMovablesCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseFastMoveCheat)) {
            this.setFastMoveCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseBuildCheat)) {
            this.setBuildCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseFarmingCheat)) {
            this.setFarmingCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseFishingCheat)) {
            this.setFishingCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseHealthCheat)) {
            this.setHealthCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseMechanicsCheat)) {
            this.setMechanicsCheat(false);
        }

        if (!newRole.hasCapability(Capability.UseTimedActionInstantCheat)) {
            this.setTimedActionInstantCheat(false);
        }

        if (!newRole.hasCapability(Capability.ToggleNoclipHimself)) {
            this.setNoClip(false);
        }

        if (!newRole.hasCapability(Capability.CanSeeAll)) {
            this.setCanSeeAll(false);
        }

        if (!newRole.hasCapability(Capability.CanHearAll)) {
            this.setCanHearAll(false);
        }

        if (!newRole.hasCapability(Capability.UseBrushToolManager)) {
            this.setCanUseBrushTool(false);
        }

        if (!newRole.hasCapability(Capability.UseLootTool)) {
            this.setCanUseLootTool(false);
        }

        if (!newRole.hasCapability(Capability.UseDebugContextMenu)) {
            this.setCanUseDebugContextMenu(false);
        }

        this.role = newRole;
    }

    public boolean wasLastAttackHandToHand() {
        return this.lastAttackWasHandToHand;
    }

    public void setLastAttackWasHandToHand(boolean lastAttackWasHandToHand) {
        this.lastAttackWasHandToHand = lastAttackWasHandToHand;
    }

    public void petAnimal() {
        if (GameTime.getInstance().getCalender().getTimeInMillis() - this.lastAnimalPet > 3600000L) {
            this.lastAnimalPet = GameTime.getInstance().getCalender().getTimeInMillis();
            this.getStats().remove(CharacterStat.STRESS, Rand.Next(0.15F, 0.35F));
            this.getStats().remove(CharacterStat.UNHAPPINESS, Rand.Next(15.0F, 35.0F));
            this.getStats().remove(CharacterStat.PAIN, Rand.Next(15.0F, 35.0F));
            this.getStats().remove(CharacterStat.BOREDOM, Rand.Next(15.0F, 35.0F));
        }
    }

    public IsoAnimal getUseableAnimal() {
        if (this.getCurrentSquare() == null) {
            return null;
        } else {
            ArrayList<IsoAnimal> animals = new ArrayList<>();

            for (int x = this.getCurrentSquare().getX() - 2; x < this.getCurrentSquare().getX() + 2; x++) {
                for (int y = this.getCurrentSquare().getY() - 2; y < this.getCurrentSquare().getY() + 2; y++) {
                    IsoGridSquare sq = this.getCurrentSquare().getCell().getGridSquare(x, y, this.getCurrentSquare().getZ());
                    if (sq != null) {
                        animals.addAll(sq.getAnimals());
                    }
                }
            }

            float closestDist = 99.0F;
            IsoAnimal closestAnimal = null;

            for (int i = 0; i < animals.size(); i++) {
                IsoAnimal animal = animals.get(i);
                if (animal.getCurrentSquare() != null) {
                    IsoGridSquare lookingSq = this.getCurrentSquare().getAdjacentSquare(this.getDir());
                    if (lookingSq.DistToProper(animal.getCurrentSquare()) < closestDist) {
                        closestAnimal = animal;
                        closestDist = lookingSq.DistToProper(animal.getCurrentSquare());
                    }
                }
            }

            return closestAnimal;
        }
    }

    public LuaTimedActionNew getTimedActionToRetrigger() {
        return this.timedActionToRetrigger;
    }

    public void setTimedActionToRetrigger(LuaTimedActionNew timedActionToRetrigger) {
        this.timedActionToRetrigger = timedActionToRetrigger;
    }

    public PlayerCraftHistory getPlayerCraftHistory() {
        return this.craftHistory;
    }

    public boolean isFavouriteRecipe(String recipe) {
        String favString = BaseCraftingLogic.getFavouriteModDataString(recipe);
        Object isFavourite = this.getModData().rawget(favString);
        return isFavourite != null && (Boolean)isFavourite;
    }

    public boolean isFavouriteRecipe(CraftRecipe recipe) {
        return this.isFavouriteRecipe(recipe.getName());
    }

    public boolean isUnwanted(String item) {
        String unwantedString = getUnwantedModDataString(item);
        Object isUnwanted = this.getModData().rawget(unwantedString);
        return isUnwanted != null && (Boolean)isUnwanted;
    }

    public void setUnwanted(String item, Boolean unwanted) {
        String unwantedString = getUnwantedModDataString(item);
        this.getModData().rawset(unwantedString, unwanted);
        if (GameClient.client && this.isLocalPlayer()) {
            this.transmitModData();
        }
    }

    public static String getUnwantedModDataString(String item) {
        return item != null ? "itemUnwanted:" + item : null;
    }

    public int getTimeSinceLastNetData() {
        return this.timeSinceLastNetData;
    }

    public void setTimeSinceLastNetData(int timeSinceLastNetData) {
        this.timeSinceLastNetData = timeSinceLastNetData;
    }

    public long getLastRemoteUpdate() {
        return this.lastRemoteUpdate;
    }

    public void setLastRemoteUpdate(long lastRemoteUpdate) {
        this.lastRemoteUpdate = lastRemoteUpdate;
    }

    public boolean getAutoDrink() {
        return this.autoDrink;
    }

    public void setAutoDrink(boolean autoDrink) {
        this.autoDrink = autoDrink;
    }

    private final class GrapplerGruntChance {
        private final int baseChance;
        private final int chanceIncrement;
        private int gruntChance;

        private GrapplerGruntChance() {
            Objects.requireNonNull(IsoPlayer.this);
            super();
            this.baseChance = 15;
            this.chanceIncrement = 5;
            this.gruntChance = 15;
        }
    }

    static class InputState {
        public boolean melee;
        public boolean grapple;
        public boolean isAttacking;
        public boolean running;
        public boolean sprinting;
        public boolean isAiming;
        public boolean isCharging;
        public boolean isChargingLt;

        public void reset() {
            this.melee = false;
            this.grapple = false;
            this.isAttacking = false;
            this.running = false;
            this.sprinting = false;
            this.isAiming = false;
            this.isCharging = false;
            this.isChargingLt = false;
        }
    }

    static final class MoveVars {
        float moveX;
        float moveY;
        float strafeX;
        float strafeY;
        IsoDirections newFacing;
    }

    private static class VehicleContainer {
        private BaseVehicle vehicle;
        private int containerIndex;

        public IsoPlayer.VehicleContainer set(BaseVehicle vehicle, int containerIndex) {
            this.vehicle = vehicle;
            this.containerIndex = containerIndex;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IsoPlayer.VehicleContainer vehicleContainer
                && this.vehicle == vehicleContainer.vehicle
                && this.containerIndex == vehicleContainer.containerIndex;
        }
    }

    private static class VehicleContainerData {
        private final ArrayList<IsoPlayer.VehicleContainer> tempContainers = new ArrayList<>();
        private final ArrayList<IsoPlayer.VehicleContainer> containers = new ArrayList<>();
        private final Stack<IsoPlayer.VehicleContainer> freeContainers = new Stack<>();
    }

    private static class s_performance {
        private static final PerformanceProfileProbe postUpdate = new PerformanceProfileProbe("IsoPlayer.postUpdate");
        private static final PerformanceProfileProbe update = new PerformanceProfileProbe("IsoPlayer.update");
    }
}
