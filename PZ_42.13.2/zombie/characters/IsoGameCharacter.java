// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import gnu.trove.map.hash.THashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;
import java.util.Map.Entry;
import org.joml.GeometryUtils;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Quaternion;
import se.krka.kahlua.vm.KahluaTable;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.PersistentOutfits;
import zombie.PredicatedFileWatcher;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.ai.GameCharacterAIBrain;
import zombie.ai.MapKnowledge;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.AStarPathFinderResult;
import zombie.ai.sadisticAIDirector.SleepingEventData;
import zombie.ai.states.AttackNetworkState;
import zombie.ai.states.AttackState;
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
import zombie.ai.states.GrappledThrownIntoContainerState;
import zombie.ai.states.GrappledThrownOutWindowState;
import zombie.ai.states.GrappledThrownOverFenceState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.LungeState;
import zombie.ai.states.OpenWindowState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerEmoteState;
import zombie.ai.states.PlayerGetUpState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.SmashWindowState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.StateManager;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieFallingState;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.ai.states.animals.AnimalWalkState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartLast;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.CharacterTimedActions.LuaTimedActionNew;
import zombie.characters.Moodles.Moodles;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.ActionStateSnapshot;
import zombie.characters.action.IActionStateChanged;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.characters.traits.CharacterTraits;
import zombie.chat.ChatElement;
import zombie.chat.ChatElementOwner;
import zombie.chat.ChatManager;
import zombie.chat.ChatMessage;
import zombie.combat.CombatConfigKey;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.BallisticsTarget;
import zombie.core.physics.RagdollController;
import zombie.core.physics.RagdollStateData;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.BaseGrappleable;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.IGrappleableWrapper;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandlePool;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandles;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableRegistry;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.IClothingItemListener;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamGameServer;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.gameStates.IngameState;
import zombie.input.Mouse;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Literature;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponType;
import zombie.iso.BentFences;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoRoofFixer;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoBall;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFallingClothing;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.ShadowParams;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.PVPLogTool;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheatXPUpdate;
import zombie.network.chat.ChatServer;
import zombie.network.chat.ChatType;
import zombie.network.fields.hit.AttackVars;
import zombie.network.fields.hit.HitInfo;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.SyncPlayerStatsPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ObjectPool;
import zombie.profanity.ProfanityFilter;
import zombie.radio.ZomboidRadio;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ResourceLocation;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.WeaponCategory;
import zombie.statistics.StatisticCategory;
import zombie.statistics.StatisticType;
import zombie.statistics.StatisticsManager;
import zombie.ui.ActionProgressBar;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.AutoCloseablePool;
import zombie.util.FrameDelay;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public abstract class IsoGameCharacter
    extends IsoMovingObject
    implements Talker,
    ChatElementOwner,
    IAnimatable,
    IAnimationVariableMap,
    IAnimationVariableRegistry,
    IClothingItemListener,
    IActionStateChanged,
    IAnimEventCallback,
    IAnimEventWrappedBroadcaster,
    IFMODParameterUpdater,
    IGrappleableWrapper,
    ILuaVariableSource,
    ILuaGameCharacter {
    private static final int CorpseBodyWeight = 20;
    private static final float BaseMuscleStrainMultiplier = 2.5F;
    private static final float ZombieAttackingClimbPenalty = 25.0F;
    private static final float ZombieNearbyClimbPenalty = 7.0F;
    public static final int GlovesStrengthBonus = 1;
    public static final int AwkwardGlovesStrengthDivisor = 2;
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();
    public IsoAIModule ai = new IsoAIModule(this);
    private final float extraLungeRange = 0.2F;
    private boolean ignoreAimingInput;
    private boolean headLookAround;
    private static final float maxHeadLookAngle = 0.348F;
    private float headLookHorizontal;
    private float headLookVertical;
    private boolean doDeathSound = true;
    private boolean canShout = true;
    public boolean doDirtBloodEtc = true;
    private static int instanceId;
    public static final int RENDER_OFFSET_X = 1;
    public static final int RENDER_OFFSET_Y = -89;
    public static final float s_maxPossibleTwist = 70.0F;
    private static final IsoGameCharacter.Bandages s_bandages = new IsoGameCharacter.Bandages();
    private static final HashMap<Integer, SurvivorDesc> SurvivorMap = new HashMap<>();
    private static final int[] LevelUpLevels = new int[]{
        25,
        75,
        150,
        225,
        300,
        400,
        500,
        600,
        700,
        800,
        900,
        1000,
        1200,
        1400,
        1600,
        1800,
        2000,
        2200,
        2400,
        2600,
        2800,
        3000,
        3200,
        3400,
        3600,
        3800,
        4000,
        4400,
        4800,
        5200,
        5600,
        6000
    };
    protected static final Vector2 tempo = new Vector2();
    protected static final ColorInfo inf = new ColorInfo();
    public long vocalEvent;
    public long removedFromWorldMs;
    private boolean isAddedToModelManager;
    private boolean autoWalk;
    private final Vector2 autoWalkDirection = new Vector2();
    private boolean sneaking;
    private float sneakLimpSpeedScale = 1.0F;
    private static final float m_sneakLimpSpeed = 0.6F;
    private static final float m_sneakLowLimpSpeed = 0.45F;
    protected static final Vector2 tempo2 = new Vector2();
    private static final Vector2 tempVector2_1 = new Vector2();
    private static final Vector2 tempVector2_2 = new Vector2();
    private static String sleepText;
    protected final ArrayList<InventoryItem> savedInventoryItems = new ArrayList<>();
    private final String instancename;
    public final ArrayList<String> amputations = new ArrayList<>();
    public ModelInstance hair;
    public ModelInstance beard;
    public ModelInstance primaryHandModel;
    public ModelInstance secondaryHandModel;
    private final ActionContext actionContext = new ActionContext(this);
    public final BaseCharacterSoundEmitter emitter;
    private final FMODParameterList fmodParameters = new FMODParameterList();
    private final AnimationVariableSource gameVariables = new AnimationVariableSource();
    private AnimationVariableSource playbackGameVariables;
    private boolean running;
    private boolean sprinting;
    private boolean avoidDamage;
    public boolean callOut;
    public IsoGameCharacter reanimatedCorpse;
    public int reanimatedCorpseId = -1;
    private AnimationPlayer animPlayer;
    private boolean animPlayerRecordingExclusive;
    private boolean deferredMovementEnabled = true;
    public final AdvancedAnimator advancedAnimator;
    public final HashMap<State, HashMap<Object, Object>> stateMachineParams = new HashMap<>();
    private boolean isCrit;
    private boolean knockedDown;
    public int bumpNbr;
    private final ArrayList<IsoGameCharacter.PerkInfo> perkList = new ArrayList<>();
    protected final Vector2 forwardDirection = new Vector2();
    private float targetVerticalAimAngleDegrees;
    private float currentVerticalAimAngleDegrees;
    public boolean asleep;
    public boolean isResting;
    public boolean blockTurning;
    public float speedMod = 1.0F;
    public IsoSprite legsSprite;
    private boolean female = true;
    public float knockbackAttackMod = 1.0F;
    private boolean animal;
    public final boolean[] isVisibleToPlayer = new boolean[4];
    public float savedVehicleX;
    public float savedVehicleY;
    public short savedVehicleSeat = -1;
    public boolean savedVehicleRunning;
    private static final float RecoilDelayDecrease = 0.625F;
    protected static final float BeenMovingForIncrease = 1.25F;
    protected static final float BeenMovingForDecrease = 0.625F;
    private IsoGameCharacter followingTarget;
    private final ArrayList<IsoMovingObject> localList = new ArrayList<>();
    private final ArrayList<IsoMovingObject> localNeutralList = new ArrayList<>();
    private final ArrayList<IsoMovingObject> localGroupList = new ArrayList<>();
    private final ArrayList<IsoMovingObject> localRelevantEnemyList = new ArrayList<>();
    private float dangerLevels;
    private static final Vector2 tempVector2 = new Vector2();
    private float leaveBodyTimedown;
    protected boolean allowConversation = true;
    private float reanimateTimer;
    private int reanimAnimFrame;
    private int reanimAnimDelay;
    private boolean reanim;
    private boolean visibleToNpcs = true;
    private int dieCount;
    private float llx;
    private float lly;
    private float llz;
    protected int remoteId = -1;
    protected int numSurvivorsInVicinity;
    private float levelUpMultiplier = 2.5F;
    protected IsoGameCharacter.XP xp;
    private int lastLocalEnemies;
    private final ArrayList<IsoMovingObject> veryCloseEnemyList = new ArrayList<>();
    private final HashMap<String, IsoGameCharacter.Location> lastKnownLocation = new HashMap<>();
    protected IsoGameCharacter attackedBy;
    protected boolean damagedByVehicle;
    protected boolean ignoreStaggerBack;
    private int timeThumping;
    private int patienceMax = 150;
    private int patienceMin = 20;
    private int patience = 20;
    protected final Stack<BaseAction> characterActions = new Stack<>();
    private int zombieKills;
    private int survivorKills;
    private int lastZombieKills;
    protected float forceWakeUpTime = -1.0F;
    private float fullSpeedMod = 1.0F;
    protected float runSpeedModifier = 1.0F;
    private float walkSpeedModifier = 1.0F;
    private float combatSpeedModifier = 1.0F;
    private float clothingDiscomfortModifier;
    private boolean rangedWeaponEmpty;
    public final ArrayList<InventoryContainer> bagsWorn = new ArrayList<>();
    protected boolean forceWakeUp;
    protected final BodyDamage bodyDamage;
    private BodyDamage bodyDamageRemote;
    private State defaultState;
    protected WornItems wornItems;
    protected AttachedItems attachedItems;
    protected ClothingWetness clothingWetness;
    protected ClothingWetnessSync clothingWetnessSync;
    protected SurvivorDesc descriptor;
    private final Stack<IsoBuilding> familiarBuildings = new Stack<>();
    protected final AStarPathFinderResult finder = new AStarPathFinderResult();
    private float fireKillRate = 0.0038F;
    private int fireSpreadProbability = 6;
    protected float health = 1.0F;
    protected boolean dead;
    protected boolean kill;
    private boolean wornClothingCanRagdoll = true;
    private boolean isEditingRagdoll;
    private boolean ragdollFall;
    private boolean vehicleCollision;
    protected boolean playingDeathSound;
    private boolean deathDragDown;
    protected String hurtSound = "MaleZombieHurt";
    protected ItemContainer inventory = new ItemContainer();
    protected InventoryItem leftHandItem;
    protected boolean handItemShouldSendToClients;
    private int nextWander = 200;
    private boolean onFire;
    private int pathIndex;
    protected InventoryItem rightHandItem;
    protected Color speakColour = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    protected float slowFactor;
    protected float slowTimer;
    protected boolean useParts;
    protected boolean speaking;
    private float speakTime;
    private float staggerTimeMod = 1.0F;
    protected final StateMachine stateMachine;
    protected final Moodles moodles;
    protected final Stats stats = new Stats();
    private final Stack<String> usedItemsOn = new Stack<>();
    protected HandWeapon useHandWeapon;
    protected IsoGridSquare attackTargetSquare;
    private float bloodImpactX;
    private float bloodImpactY;
    private float bloodImpactZ;
    private IsoSprite bloodSplat;
    private boolean onBed;
    private final Vector2 moveForwardVec = new Vector2();
    protected boolean pathing;
    protected ChatElement chatElement;
    private final Stack<IsoGameCharacter> localEnemyList = new Stack<>();
    protected final Stack<IsoGameCharacter> enemyList = new Stack<>();
    protected final CharacterTraits characterTraits = new CharacterTraits();
    private int maxWeight = 8;
    private int maxWeightBase = 8;
    private float sleepingTabletEffect;
    private float sleepingTabletDelta = 1.0F;
    private float betaEffect;
    private float betaDelta;
    private float depressEffect;
    private float depressDelta;
    private float depressFirstTakeTime = -1.0F;
    private float painEffect;
    private float painDelta;
    private boolean doDefer = true;
    private float haloDispTime = 128.0F;
    protected TextDrawObject userName;
    private TextDrawObject haloNote;
    private static final String nameCarKeySuffix = " [img=media/ui/CarKey.png";
    private static final String voiceSuffix = "[img=media/ui/voiceon.png] ";
    private static final String voiceMuteSuffix = "[img=media/ui/voicemuted.png] ";
    protected IsoPlayer isoPlayer;
    private boolean hasInitTextObjects;
    private boolean canSeeCurrent;
    private boolean drawUserName;
    private final IsoGameCharacter.Location lastHeardSound = new IsoGameCharacter.Location(-1, -1, -1);
    protected boolean climbing;
    private boolean lastCollidedW;
    private boolean lastCollidedN;
    protected float fallTime;
    protected float lastFallSpeed;
    protected boolean falling;
    protected boolean isOnGround = true;
    protected BaseVehicle vehicle;
    boolean isNpc;
    private long lastBump;
    private IsoGameCharacter bumpedChr;
    private int age = 25;
    private int lastHitCount;
    private final Safety safety = new Safety(this);
    private float meleeDelay;
    private float recoilDelay;
    private float beenMovingFor;
    private float beenSprintingFor;
    private float aimingDelay;
    private String clickSound;
    private float reduceInfectionPower;
    private final List<String> knownRecipes = new ArrayList<>();
    private final HashSet<String> knownMediaLines = new HashSet<>();
    private int lastHourSleeped;
    protected float timeOfSleep;
    protected float delayToActuallySleep;
    private String bedType = "averageBed";
    private IsoObject bed;
    private boolean isReading;
    private float timeSinceLastSmoke;
    private ChatMessage lastChatMessage;
    private String lastSpokenLine;
    public PlayerCheats cheats = new PlayerCheats();
    private boolean showAdminTag = true;
    private long isAnimForecasted;
    private boolean fallOnFront;
    private boolean killedByFall;
    private boolean hitFromBehind;
    private String hitReaction = "";
    private String bumpType = "";
    private boolean isBumpDone;
    private boolean bumpFall;
    private boolean bumpStaggered;
    private String bumpFallType = "";
    private int sleepSpeechCnt;
    private Radio equipedRadio;
    private InventoryItem leftHandCache;
    private InventoryItem rightHandCache;
    private InventoryItem backCache;
    private final ArrayList<IsoGameCharacter.ReadBook> readBooks = new ArrayList<>();
    public final IsoGameCharacter.LightInfo lightInfo = new IsoGameCharacter.LightInfo();
    private final IsoGameCharacter.LightInfo lightInfo2 = new IsoGameCharacter.LightInfo();
    private Path path2;
    private final MapKnowledge mapKnowledge = new MapKnowledge();
    protected final AttackVars attackVars = new AttackVars();
    private final PZArrayList<HitInfo> hitInfoList = new PZArrayList<>(HitInfo.class, 8);
    private final PathFindBehavior2 pfb2 = new PathFindBehavior2(this);
    private final InventoryItem[] cacheEquiped = new InventoryItem[2];
    private boolean aimAtFloor;
    private float aimAtFloorTargetDistance;
    protected int persistentOutfitId = 0;
    protected boolean persistentOutfitInit;
    private boolean updateModelTextures;
    private ModelInstanceTextureCreator textureCreator;
    public boolean updateEquippedTextures;
    private final ArrayList<ModelInstance> readyModelData = new ArrayList<>();
    private boolean isSitOnFurniture;
    private IsoObject sitOnFurnitureObject;
    private IsoDirections sitOnFurnitureDirection;
    private boolean sitOnGround;
    private boolean ignoreMovement;
    private boolean hideWeaponModel = false;
    private boolean hideEquippedHandL = false;
    private boolean hideEquippedHandR = false;
    private boolean isAiming;
    private float beardGrowTiming = -1.0F;
    private float hairGrowTiming = -1.0F;
    private float moveDelta = 1.0F;
    protected float turnDeltaNormal = 1.0F;
    protected float turnDeltaRunning = 0.8F;
    protected float turnDeltaSprinting = 0.75F;
    private float maxTwist = 15.0F;
    private boolean isMoving;
    private boolean isTurning;
    private boolean isTurningAround;
    private float initialTurningAroundTarget;
    private boolean isTurning90;
    private boolean invincible;
    private float lungeFallTimer;
    private SleepingEventData sleepingEventData;
    private static final int HAIR_GROW_TIME_DAYS = 20;
    private static final int BEARD_GROW_TIME_DAYS = 5;
    public float realx;
    public float realy;
    public byte realz;
    public NetworkVariables.ZombieState realState = NetworkVariables.ZombieState.Idle;
    public String overridePrimaryHandModel;
    public String overrideSecondaryHandModel;
    public boolean forceNullOverride;
    protected final UpdateLimit ulBeatenVehicle = new UpdateLimit(200L);
    private float momentumScalar;
    private final HashMap<String, State> aiStateMap = new HashMap<>();
    private boolean isPerformingAttackAnim;
    private boolean isPerformingShoveAnim;
    private boolean isPerformingStompAnim;
    private float wornItemsVisionModifier = 1.0F;
    private float wornItemsHearingModifier = 1.0F;
    private float corpseSicknessRate;
    private float blurFactor;
    private float blurFactorTarget;
    public boolean usernameDisguised;
    private float climbRopeTime;
    @Deprecated
    public ArrayList<Integer> invRadioFreq = new ArrayList<>();
    private final PredicatedFileWatcher animStateTriggerWatcher;
    private final AnimationPlayerRecorder animationRecorder;
    private final String uid;
    private boolean debugVariablesRegistered;
    private float effectiveEdibleBuffTimer;
    private final HashMap<String, Integer> readLiterature = new HashMap<>();
    private final HashSet<String> readPrintMedia = new HashSet<>();
    private IsoGameCharacter lastHitCharacter;
    private BallisticsController ballisticsController;
    private BallisticsTarget ballisticsTarget;
    private final BaseGrappleable grappleable;
    private boolean isAnimatingBackwards;
    private float animationTimeScale = 1.0F;
    private boolean animationUpdatingThisFrame = true;
    private final FrameDelay animationInvisibleFrameDelay = new FrameDelay(4);
    public long lastAnimalPet;
    private final AnimEventBroadcaster animEventBroadcaster = new AnimEventBroadcaster();
    public IsoGameCharacter vbdebugHitTarget;
    private String hitDirEnum = "FRONT";
    private boolean isGrappleThrowOutWindow;
    private boolean isGrappleThrowOverFence;
    private boolean isGrappleThrowIntoContainer;
    private boolean shoveStompAnim;
    private static final float maxStrafeSpeed = 0.48F;
    private static final Vector3f tempVector3f00 = new Vector3f();
    private static final Vector3f tempVector3f01 = new Vector3f();
    private static final float CombatSpeedBase = 0.8F;
    private static final float HeavyTwoHandedWeaponModifier = 1.2F;
    private float idleSquareTime;
    private final ArrayList<String> concurrentActionList = new ArrayList<>(
        List.of("OpenDoor", "CloseDoor", "ClimbThroughWindow", "ClimbOverFence", "OpenHutch", "CloseCurtain", "OpenCurtain")
    );
    private float shadowFm;
    private float shadowBm;
    private long shadowTick = -1L;
    private float lastFitnessValue = CharacterStat.FITNESS.getDefaultValue();
    public final NetworkCharacter networkCharacter = new NetworkCharacter();
    private final IsoGameCharacter.Recoil recoil = new IsoGameCharacter.Recoil();
    private static final double meleeWeaponMuscleStrainAdjustment = 0.65;
    private boolean usePhysicHitReaction;
    private ClimbSheetRopeState.ClimbData climbData;
    private final FallDamage fallDamage = new FallDamage();
    private static final AnimEvent s_turn180StartedEvent = new AnimEvent();
    private static final AnimEvent s_turn180TargetChangedEvent = new AnimEvent();
    private static final ArrayList<IsoMovingObject> movingStatic = new ArrayList<>();
    final PerformanceProfileProbe postUpdateInternal = new PerformanceProfileProbe("IsoGameCharacter.postUpdate");
    final PerformanceProfileProbe updateInternal = new PerformanceProfileProbe("IsoGameCharacter.update");
    private static final Vector3 tempVectorBonePos = new Vector3();

    public IsoGameCharacter(IsoCell cell, float x, float y, float z) {
        super(cell, false);
        this.uid = String.format("%s-%s", this.getClass().getSimpleName(), UUID.randomUUID());
        this.grappleable = new BaseGrappleable(this);
        this.getWrappedGrappleable().setOnGrappledBeginCallback(this::onGrappleBegin);
        this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
        if (!GameServer.server || !(this instanceof IsoZombie)) {
            this.registerVariableCallbacks();
            this.registerAnimEventCallbacks();
        }

        this.instancename = this.getClass().getSimpleName() + instanceId;
        instanceId++;
        if (!(this instanceof IsoSurvivor)) {
            this.emitter = (BaseCharacterSoundEmitter)(!Core.soundDisabled && !GameServer.server
                ? new CharacterSoundEmitter(this)
                : new DummyCharacterSoundEmitter(this));
        } else {
            this.emitter = null;
        }

        if (x != 0.0F || y != 0.0F || z != 0.0F) {
            if (this.getCell().isSafeToAdd()) {
                this.getCell().getObjectList().add(this);
            } else {
                this.getCell().getAddList().add(this);
            }
        }

        if (this.def == null) {
            this.def = IsoSpriteInstance.get(this.sprite);
        }

        if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
            this.bodyDamage = new BodyDamage(this);
            this.moodles = new Moodles(this);
            this.xp = new IsoGameCharacter.XP(this);
        } else {
            if (this instanceof IsoAnimal) {
                this.bodyDamage = new BodyDamage(this);
            } else {
                this.bodyDamage = null;
            }

            this.moodles = null;
            this.xp = null;
        }

        this.patience = Rand.Next(this.patienceMin, this.patienceMax);
        this.setX(x + 0.5F);
        this.setY(y + 0.5F);
        this.setZ(z);
        this.setScriptNextX(this.setLastX(this.setNextX(x)));
        this.setScriptNextY(this.setLastY(this.setNextY(y)));
        if (cell != null) {
            this.current = this.getCell().getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
        }

        this.offsetY = 0.0F;
        this.offsetX = 0.0F;
        this.stateMachine = new StateMachine(this);
        this.setDefaultState(IdleState.instance());
        this.inventory.parent = this;
        this.inventory.setExplored(true);
        this.chatElement = new ChatElement(this, 1, "character");
        this.animationRecorder = new AnimationPlayerRecorder(this);
        this.advancedAnimator = new AdvancedAnimator();
        this.advancedAnimator.init(this);
        this.advancedAnimator.animCallbackHandlers.add(this);
        this.advancedAnimator.setAnimSet(AnimationSet.GetAnimationSet(this.GetAnimSetName(), false));
        this.advancedAnimator.setRecorder(this.animationRecorder);
        this.actionContext.onStateChanged.add(this);
        this.animStateTriggerWatcher = new PredicatedFileWatcher(
            ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetAnimState.xml"), AnimStateTriggerXmlFile.class, this::onTrigger_setAnimStateToTriggerFile
        );
    }

    private void registerVariableCallbacks() {
        this.setVariable(
            "isRendered",
            true,
            this::getDoRender,
            this::setDoRender,
            owner -> "If this character is allowed to be drawn to screen. Typically a corpse being dragged out of a container is not drawn until it has exited it."
        );
        this.setVariable(
            "hashitreaction", this::hasHitReaction, owner -> "If this character has been attacked, the type of reaction to the hit is stored here."
        );
        this.setVariable(
            "hitreaction",
            this::getHitReaction,
            this::setHitReaction,
            owner -> "Does this character have a hit reaction. If they have not been attacked and hit, this will return FALSE."
        );
        this.setVariable(
            "collidetype",
            this::getCollideType,
            this::setCollideType,
            owner -> "The character has collided with something. This is the type of collision. The only known value is 'wall'."
        );
        this.setVariable(
            "footInjuryType",
            this::getFootInjuryType,
            owner -> "The type of unhealed foot injury this character currently has, if any.<br />Known values:<br /> leftlight,<br /> rightlight,<br /> leftheavy,<br /> rightheavy"
        );
        this.setVariable(
            "bumptype",
            this::getBumpType,
            this::setBumpType,
            owner -> "If the character has been bumped, this is the kind of bump they've sustained.<br />Known values:<br /> stagger,<br /> left,<br /> trippingFromSprint"
        );
        this.setVariable("onbed", this::isOnBed, this::setOnBed, owner -> "Is this character lying on a bed.");
        this.setVariable(
            "sittingonfurniture", this::isSittingOnFurniture, this::setSittingOnFurniture, owner -> "Is this character sitting on furniture. Eg a chair."
        );
        this.setVariable("sitonground", this::isSitOnGround, this::setSitOnGround, owner -> "Is this character sitting on the ground.");
        this.setVariable(
            "canclimbdownrope",
            this::canClimbDownSheetRopeInCurrentSquare,
            owner -> "Can the character climb down a sheet rope, from where they are currently standing."
        );
        this.setVariable("frombehind", this::isHitFromBehind, this::setHitFromBehind, owner -> "Has this character been attacked and hit from behind.");
        this.setVariable("fallonfront", this::isFallOnFront, this::setFallOnFront, owner -> "This character is falling- or has fallen down on their belly.");
        this.setVariable(
            "killedbyfall", this::isKilledByFall, this::setKilledByFall, owner -> "Has this character been killed by a fall, caused by impact to the ground."
        );
        this.setVariable("intrees", this::isInTreesNoBush, owner -> "Is this character in trees, but not in a bush.");
        this.setVariable("bumped", this::isBumped, owner -> "This character has been bumped.");
        this.setVariable("BumpDone", false, this::isBumpDone, this::setBumpDone, owner -> "This character has finished reacting to getting bumped.");
        this.setVariable("BumpFall", false, this::isBumpFall, this::setBumpFall, owner -> "This character has been bumped, hard enough to fall.");
        this.setVariable(
            "BumpFallType",
            "",
            this::getBumpFallType,
            this::setBumpFallType,
            owner -> "If this character has been numped hard enough to fall down, this is the type of fall.<br />Known values:<br /> pushedBehind,<br /> pushedFront"
        );
        this.setVariable(
            "BumpStaggered", false, this::isBumpStaggered, this::setBumpStaggered, owner -> "This character has been bumped, hard enough to be staggered."
        );
        this.setVariable("bonfloor", this::isOnFloor, this::setOnFloor, owner -> "This character is lying on the floor.");
        this.setVariable(
            "isProne",
            this::isProne,
            owner -> "This character is currently prone, for the purposes of targeting and selection of appropriate attack. Determined by their internal logic. Usually, but not always, isProne = bOnFloor"
        );
        this.setVariable("isGettingUp", this::isGettingUp, owner -> "This character is currently getting up from a prone position.");
        this.setVariable(
            "rangedweaponempty", this::isRangedWeaponEmpty, this::setRangedWeaponEmpty, owner -> "This character's equipped weapon is out of ammo."
        );
        this.setVariable("footInjury", this::hasFootInjury, owner -> "Does this character have an unhealed injury to their foot.");
        this.setVariable("ChopTreeSpeed", 1.0F, this::getChopTreeSpeed, owner -> "The speed of chopping trees. Multiplier: 0.0 - 1.0");
        this.setVariable("MoveDelta", 1.0F, this::getMoveDelta, this::setMoveDelta, owner -> "The movement speed. Multiplier: 0.0 - 1.0");
        this.setVariable("TurnDelta", 1.0F, this::getTurnDelta, this::setTurnDelta, owner -> "The turn speed. Multiplier: 0.0 - 1.0");
        this.setVariable(
            "angle", this::getDirectionAngle, this::setDirectionAngle, owner -> "The character's current direction of travel. In degrees: -180 to 180."
        );
        this.setVariable(
            "animAngle",
            this::getAnimAngle,
            owner -> "The character's current direction of travel, as is tweened by its AnimationPlayer. In degrees: -180 to 180."
        );
        this.setVariable("twist", this::getTwist, owner -> "The character's current twist about the waist. In degrees.");
        this.setVariable(
            "targetTwist",
            this::getTargetTwist,
            owner -> "The character's target twist about the waist. In degrees. The character tries to twist this far but may or may not achieve it."
        );
        this.setVariable(
            "maxTwist",
            this.maxTwist,
            this::getMaxTwist,
            this::setMaxTwist,
            owner -> "The character's maximum twist about the waist. In degrees. The character will not twist further than this limit."
        );
        this.setVariable("shoulderTwist", this::getShoulderTwist, owner -> "The character's twist about the shoulders. In degrees.");
        this.setVariable("excessTwist", this::getExcessTwist, owner -> "The character's excess twist. This is the difference between maxTwist and twist.");
        this.setVariable("numTwistBones", this::getNumTwistBones, owner -> "The number of bones the character uses to twist about their waist.");
        this.setVariable("angleStepDelta", this::getAnimAngleStepDelta, owner -> "The character's rate of turn per frame.");
        this.setVariable("angleTwistDelta", this::getAnimAngleTwistDelta, owner -> "The character's rate of twist per frame.");
        this.setVariable("isTurning", false, this::isTurning, this::setTurning, owner -> "The character is currently turning.");
        this.setVariable("isTurning90", false, this::isTurning90, this::setTurning90, owner -> "The character is turning by a right angle.");
        this.setVariable(
            "isTurningAround", false, this::isTurningAround, this::setTurningAround, owner -> "The character is turning around, by more than 90 degrees."
        );
        this.setVariable("bMoving", false, this::isMoving, this::setMoving, owner -> "The character is moving.");
        this.setVariable("beenMovingFor", this::getBeenMovingFor, owner -> "The amount of time the character has been moving for.");
        this.setVariable("previousState", this::getPreviousActionContextStateName, owner -> "The character's previous state.");
        this.setVariable(
            "momentumScalar", this::getMomentumScalar, this::setMomentumScalar, owner -> "The amount of linear momentum the character has. Scalar: 0.0 - 1.0"
        );
        this.setVariable("hasTimedActions", this::hasTimedActions, owner -> "The character is performing a timed action. Or has a timed action to do.");
        this.setVariable("isOverEncumbered", this::isOverEncumbered, owner -> "Is the carrying too much weight.");
        if (DebugOptions.instance.character.debug.registerDebugVariables.getValue()) {
            this.registerDebugGameVariables();
        }

        this.setVariable("CriticalHit", this::isCriticalHit, this::setCriticalHit, owner -> "Has the character been attacked by a critical hit.");
        this.setVariable("bKnockedDown", this::isKnockedDown, this::setKnockedDown, owner -> "Has the character been hit hard enough to be knocked down.");
        this.setVariable("bfalling", this::getAnimVariable_bFalling, owner -> "The character is currently falling.");
        this.setVariable("bdead", this::isDead, owner -> "Is the character dead.");
        this.setVariable("fallTime", this::getFallTime, owner -> "How long has the character been falling.");
        this.setVariable(
            "fallSpeedSeverity",
            FallSeverity.class,
            this::getFallSpeedSeverity,
            owner -> "How fast are we currenly falling. How sever would be the impact, if we were to impact the ground. None, Light, Heavy, Severe, Lethal"
        );
        this.setVariable("bGetUpFromKnees", false);
        this.setVariable("bGetUpFromProne", false);
        this.setVariable("aim", this::isAiming, owner -> "Is the character aiming.");
        this.setVariable("bAimAtfloor", this::isAimAtFloor, owner -> "Is the character aiming at the floor.");
        this.setVariable(
            "aimAtFloorAmount", this::getAimAtFloorAmount, owner -> "The character is aiming at the floor by this amount. Scalar: Horizontal: 0.0 to Down: 1.0"
        );
        this.setVariable("verticalAimAngle", this::getCurrentVerticalAimAngle, owner -> "The vertical aim angle. In degrees: -90 (down) to 90 (up)");
        this.setVariable(
            "AttackAnim", this::isPerformingAttackAnimation, this::setPerformingAttackAnimation, owner -> "Is the character performing an attack animation."
        );
        this.setVariable(
            "ShoveAnim", this::isPerformingShoveAnimation, this::setPerformingShoveAnimation, owner -> "Is the character performing a shove animation."
        );
        this.setVariable(
            "StompAnim", this::isPerformingStompAnimation, this::setPerformingStompAnimation, owner -> "Is the character performing a stomp animation."
        );
        this.setVariable("isStompAnim", this::isShoveStompAnim, this::setShoveStompAnim, owner -> "Is the character's current Shove actually a stomp.");
        this.setVariable(
            "PerformingHostileAnim",
            this::isPerformingHostileAnimation,
            owner -> "Is the character performing a hostile animation - an attack, a shove, or a stomp."
        );
        this.setVariable("FireMode", this::getFireMode, owner -> "The character's equipped weapon's firing mode. Single or Auto.");
        this.setVariable(
            "ShoutType",
            this::getShoutType,
            owner -> "The character's 'shout' type. If they are making a noise with an equipped item. Can be: BlowWhistle_primary, BlowWhistle_secondary, BlowHarmonica_primary, BlowHarmonica_secondary"
        );
        this.setVariable("ShoutItemModel", this::getShoutItemModel, owner -> "The equipped item used for the 'shout'. Usually a Whistle or a Harmonica.");
        this.setVariable(
            "isAnimatingBackwards",
            this::isAnimatingBackwards,
            this::setAnimatingBackwards,
            owner -> "Is the character animating backwards. Eg. when dragging a corpse backwards."
        );
        BaseGrappleable.RegisterGrappleVariables(this.getGameVariablesInternal(), this);
        this.setVariable(
            "GrappleThrowOutWindow",
            this::isGrappleThrowOutWindow,
            this::setGrappleThrowOutWindow,
            owner -> "The character is dragging a corpse and wants to throw them through a window."
        );
        this.setVariable(
            "GrappleThrowOverFence",
            this::isGrappleThrowOverFence,
            this::setGrappleThrowOverFence,
            owner -> "The character is dragging a corpse and wants to throw them over a fence."
        );
        this.setVariable(
            "GrappleThrowIntoContainer",
            this::isGrappleThrowIntoContainer,
            this::setGrappleThrowIntoContainer,
            owner -> "The character is dragging a corpse and wants to throw them into a container."
        );
        this.setVariable("canRagdoll", this::canRagdoll, owner -> "The character can become a ragdoll.");
        this.setVariable("isEditingRagdoll", this::isEditingRagdoll, this::setEditingRagdoll, owner -> "The character is actively editing their ragdoll.");
        this.setVariable("isRagdoll", this::isRagdoll, owner -> "The character is currently ragdolling.");
        this.setVariable(
            "isSimulationActive",
            this::isRagdollSimulationActive,
            owner -> "The character is currently ragdolling and their ragdoll simulation is actively running. Once they've settled down on the ground, this becomes FALSE."
        );
        this.setVariable("isUpright", this::isUpright, owner -> "The character is currently standing upright.");
        this.setVariable("isOnBack", this::isOnBack, owner -> "The character is lying on their back.");
        this.setVariable("isRagdollFall", this::isRagdollFall, this::setRagdollFall, owner -> "The character is falling down, using their ragdoll.");
        this.setVariable("isVehicleCollision", this::isVehicleCollision, this::setVehicleCollision, owner -> "The character has been hit by a car.");
        this.setVariable(
            "usePhysicHitReaction",
            this::usePhysicHitReaction,
            this::setUsePhysicHitReaction,
            owner -> "The character has been hit, and should use their ragdoll physics to react to it."
        );
        this.setVariable(
            "useRagdollVehicleCollision",
            this::useRagdollVehicleCollision,
            owner -> "The character has been hit by a car, and should use their ragdoll physics to react to it."
        );
        this.setVariable("bHeadLookAround", this::isHeadLookAround, owner -> "The character is looking around.");
        this.setVariable("lookHorizontal", this::getHeadLookHorizontal, owner -> "The character's horizontal head look amount. -1 to 1");
        this.setVariable("lookVertical", this::getHeadLookVertical, owner -> "The character's vertical head look amount. -1 to 1");
        this.setVariable("hitforce", this::getHitForce, owner -> "The character has been hit, by this amount of force.");
        this.setVariable(
            "hitDir",
            this::getHitDirEnum,
            owner -> "The direction the character has been hit from.<br />Known values:<br /> FRONT,<br /> BEHIND,<br /> LEFT,<br /> RIGHT."
        );
        this.setVariable("hitDir.x", () -> this.getHitDir().x, owner -> "The direction the character has been hit from. Along the x-axis.");
        this.setVariable("hitDir.y", () -> this.getHitDir().y, owner -> "The direction the character has been hit from. Along the y-axis.");
        this.setVariable(
            "recoilVarX",
            this::getRecoilVarX,
            this::setRecoilVarX,
            owner -> "The character has fired a weapon, this is the amount of recoil they're currently experiencing, along the x-axis."
        );
        this.setVariable(
            "recoilVarY",
            this::getRecoilVarY,
            this::setRecoilVarY,
            owner -> "The character has fired a weapon, this is the amount of recoil they're currently experiencing, along the y-axis."
        );
        this.setVariable(
            "hideEquippedHandL",
            this::isHideEquippedHandL,
            this::setHideEquippedHandL,
            owner -> "The character will have any item in their left hand hidden from rendering."
        );
        this.setVariable(
            "hideEquippedHandR",
            this::isHideEquippedHandR,
            this::setHideEquippedHandR,
            owner -> "The character will have any item in their right hand hidden from rendering."
        );
        this.fallDamage.registerVariableCallbacks(this);
    }

    private boolean getAnimVariable_bFalling() {
        int minLevel = 0;
        IsoChunk chunk = this.getChunk();
        if (chunk != null) {
            minLevel = chunk.getMinLevel();
        }

        return this.getZ() > minLevel && (this.falling || this.lastFallSpeed > FallingConstants.isFallingThreshold);
    }

    private void registerAnimEventCallbacks() {
        this.addAnimEventListener(this::OnAnimEvent_SetVariable);
        this.addAnimEventListener("ClearVariable", this::OnAnimEvent_ClearVariable);
        this.addAnimEventListener("PlaySound", this::OnAnimEvent_PlaySound);
        this.addAnimEventListener("PlaySoundNoBlend", this::OnAnimEvent_PlaySoundNoBlend);
        this.addAnimEventListener("Footstep", this::OnAnimEvent_Footstep);
        this.addAnimEventListener("DamageWhileInTrees", this::OnAnimEvent_DamageWhileInTrees);
        this.addAnimEventListener("TurnAround", this::OnAnimEvent_TurnAround);
        this.addAnimEventListener("TurnAround_FlipSkeleton", this::OnAnimEvent_TurnAroundFlipSkeleton);
        this.addAnimEventListener("SetSharedGrappleType", this::OnAnimEvent_SetSharedGrappleType);
        this.addAnimEventListener("GrapplerLetGo", this::OnAnimEvent_GrapplerLetGo);
        this.addAnimEventListener("FallOnFront", this::OnAnimEvent_FallOnFront);
        this.addAnimEventListener("SetOnFloor", this::OnAnimEvent_SetOnFloor);
        this.addAnimEventListener("SetKnockedDown", this::OnAnimEvent_SetKnockedDown);
        this.addAnimEventListener("IsAlmostUp", this::OnAnimEvent_IsAlmostUp);
        this.addAnimEventListener("KilledByAttacker", this::OnAnimEvent_KilledByAttacker);
    }

    private void OnAnimEvent_GrapplerLetGo(IsoGameCharacter in_owner, String in_grappleResult) {
        if (GameServer.server) {
            DebugLog.Grapple.println("GrapplerLetGo.");
        }

        LuaEventManager.triggerEvent("GrapplerLetGo", in_owner, in_grappleResult);
        in_owner.LetGoOfGrappled(in_grappleResult);
    }

    private void OnAnimEvent_FallOnFront(IsoGameCharacter in_owner, boolean in_fallOnFront) {
        in_owner.setFallOnFront(in_fallOnFront);
    }

    private void OnAnimEvent_SetOnFloor(IsoGameCharacter in_owner, boolean in_onFloor) {
        in_owner.setOnFloor(in_onFloor);
    }

    private void OnAnimEvent_SetKnockedDown(IsoGameCharacter in_owner, boolean in_knockedDown) {
        in_owner.setKnockedDown(in_knockedDown);
    }

    protected void OnAnimEvent_IsAlmostUp(IsoGameCharacter in_owner) {
        in_owner.setOnFloor(false);
        in_owner.setKnockedDown(false);
        in_owner.setSitOnGround(false);
    }

    protected void OnAnimEvent_KilledByAttacker(IsoGameCharacter in_owner) {
        in_owner.Kill(this.getAttackedBy());
    }

    public boolean isShoveStompAnim() {
        return this.shoveStompAnim;
    }

    public void setShoveStompAnim(boolean in_val) {
        this.shoveStompAnim = in_val;
    }

    private void onGrappleBegin() {
        IGrappleable grappledBy = this.getGrappledBy();
        IAnimatable grappledByAnimatable = Type.tryCastTo(grappledBy, IAnimatable.class);
        if (grappledByAnimatable != null && grappledByAnimatable.isAnimationRecorderActive()) {
            this.setAnimRecorderActive(true, true);
        }
    }

    private void onGrappleEnded() {
        this.setGrappleThrowOutWindow(false);
    }

    public float getRecoilVarX() {
        return this.recoil.recoilVarX;
    }

    public void setRecoilVarX(float in_recoilVarX) {
        this.recoil.recoilVarX = in_recoilVarX;
    }

    public float getRecoilVarY() {
        return this.recoil.recoilVarY;
    }

    public void setRecoilVarY(float in_recoilVarY) {
        this.recoil.recoilVarY = in_recoilVarY;
    }

    public void setGrappleThrowOutWindow(boolean in_newValue) {
        this.isGrappleThrowOutWindow = in_newValue;
    }

    public boolean isGrappleThrowOutWindow() {
        return this.isGrappleThrowOutWindow;
    }

    public void setGrappleThrowOverFence(boolean in_newValue) {
        this.isGrappleThrowOverFence = in_newValue;
    }

    public boolean isGrappleThrowOverFence() {
        return this.isGrappleThrowOverFence;
    }

    public void setGrappleThrowIntoContainer(boolean in_newValue) {
        this.isGrappleThrowIntoContainer = in_newValue;
    }

    public boolean isGrappleThrowIntoContainer() {
        return this.isGrappleThrowIntoContainer;
    }

    public void updateRecoilVar() {
        this.setRecoilVarY(0.0F);
        this.setRecoilVarX(0.0F + this.getPerkLevel(PerkFactory.Perks.Aiming) / 10.0F);
    }

    private void registerDebugGameVariables() {
        int maxTracks = 9;
        int maxLayers = 2;

        for (int layerIdx = 0; layerIdx < 2; layerIdx++) {
            for (int trackIdx = 0; trackIdx < 9; trackIdx++) {
                this.dbgRegisterAnimTrackVariable(layerIdx, trackIdx);
            }
        }

        this.setVariable(
            "dbg.anm.dx",
            () -> this.getDeferredMovement(tempo).x / GameTime.instance.getMultiplier(),
            owner -> "The current animation's deferred motion's x value."
        );
        this.setVariable(
            "dbg.anm.dy",
            () -> this.getDeferredMovement(tempo).y / GameTime.instance.getMultiplier(),
            owner -> "The current animation's deferred motion's y value."
        );
        this.setVariable(
            "dbg.anm.da",
            () -> this.getDeferredAngleDelta() / GameTime.instance.getMultiplier(),
            owner -> "The current animation's deferred rotation's angle value."
        );
        this.setVariable("dbg.anm.daw", this::getDeferredRotationWeight, owner -> "The current animation's deferred rotation weight value.");
        this.setVariable(
            "dbg.forward", () -> this.getForwardDirectionX() + "; " + this.getForwardDirectionY(), owner -> "The current forward direction vector. (x, y)"
        );
        this.setVariable(
            "dbg.anm.blend.fbx_x",
            () -> DebugOptions.instance.animation.blendUseFbx.getValue() ? 1.0F : 0.0F,
            owner -> "The current setting of the BlendUseFbx flag."
        );
        this.setVariable("dbg.lastFallSpeed", this::getLastFallSpeed, owner -> "The speed at which the character hit the ground.");
        this.fallDamage.registerDebugGameVariables(this);
        this.debugVariablesRegistered = true;
    }

    private void dbgRegisterAnimTrackVariable(int layerIdx, int trackIdx) {
        this.setVariable(
            String.format("dbg.anm.track%d%d", layerIdx, trackIdx),
            () -> this.dbgGetAnimTrackName(layerIdx, trackIdx),
            owner -> "The current animation track at index:" + trackIdx + " layer " + layerIdx
        );
        this.setVariable(
            String.format("dbg.anm.t.track%d%d", layerIdx, trackIdx),
            () -> this.dbgGetAnimTrackTime(layerIdx, trackIdx),
            owner -> "The current animation track time at index:" + trackIdx + " layer " + layerIdx
        );
        this.setVariable(
            String.format("dbg.anm.w.track%d%d", layerIdx, trackIdx),
            () -> this.dbgGetAnimTrackWeight(layerIdx, trackIdx),
            owner -> "The current animationt rack weight at index:" + trackIdx + " layer " + layerIdx
        );
    }

    public float getMomentumScalar() {
        return this.momentumScalar;
    }

    public void setMomentumScalar(float val) {
        this.momentumScalar = val;
    }

    public Vector2 getDeferredMovement(Vector2 out_result) {
        return this.getDeferredMovement(out_result, false);
    }

    protected Vector2 getDeferredMovement(Vector2 out_result, boolean in_reset) {
        if (!this.hasAnimationPlayer()) {
            out_result.set(0.0F, 0.0F);
            return out_result;
        } else {
            this.animPlayer.getDeferredMovement(out_result, in_reset);
            return out_result;
        }
    }

    public Vector2 getDeferredMovementFromRagdoll(Vector2 out_result) {
        if (!this.hasAnimationPlayer()) {
            out_result.set(0.0F, 0.0F);
            return out_result;
        } else {
            return this.animPlayer.getDeferredMovementFromRagdoll(out_result);
        }
    }

    public float getDeferredAngleDelta() {
        return this.animPlayer == null ? 0.0F : this.animPlayer.getDeferredAngleDelta() * (180.0F / (float)Math.PI);
    }

    public float getDeferredRotationWeight() {
        return this.animPlayer == null ? 0.0F : this.animPlayer.getDeferredRotationWeight();
    }

    @Override
    public Vector3f getTargetGrapplePos(Vector3f out_result) {
        if (this.animPlayer == null) {
            out_result.set(0.0F, 0.0F, 0.0F);
            return out_result;
        } else {
            return this.animPlayer.getTargetGrapplePos(out_result);
        }
    }

    @Override
    public Vector3 getTargetGrapplePos(Vector3 out_result) {
        if (this.animPlayer == null) {
            out_result.set(0.0F, 0.0F, 0.0F);
            return out_result;
        } else {
            return this.animPlayer.getTargetGrapplePos(out_result);
        }
    }

    @Override
    public void setTargetGrapplePos(float x, float y, float z) {
        if (this.animPlayer != null) {
            this.animPlayer.setTargetGrapplePos(x, y, z);
        }
    }

    @Override
    public Vector2 getTargetGrappleRotation(Vector2 out_result) {
        if (this.animPlayer == null) {
            out_result.set(1.0F, 0.0F);
            return out_result;
        } else {
            return this.animPlayer.getTargetGrappleRotation(out_result);
        }
    }

    public boolean isStrafing() {
        return this.getPath2() != null && this.pfb2.isStrafing() ? true : this.isAiming();
    }

    public AnimationTrack dbgGetAnimTrack(int layerIdx, int trackIdx) {
        if (this.animPlayer == null) {
            return null;
        } else {
            AnimationPlayer animPlayer = this.animPlayer;
            AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
            List<AnimationTrack> tracks = multiTrack.getTracks();
            AnimationTrack foundTrack = null;
            int i = 0;
            int currentLayerTrackIdx = 0;

            for (int count = tracks.size(); i < count; i++) {
                AnimationTrack track = tracks.get(i);
                int trackLayer = track.getLayerIdx();
                if (trackLayer == layerIdx) {
                    if (currentLayerTrackIdx == trackIdx) {
                        foundTrack = track;
                        break;
                    }

                    currentLayerTrackIdx++;
                }
            }

            return foundTrack;
        }
    }

    public String dbgGetAnimTrackName(int layerIdx, int trackIdx) {
        AnimationTrack track = this.dbgGetAnimTrack(layerIdx, trackIdx);
        return track != null ? track.getName() : "";
    }

    public float dbgGetAnimTrackTime(int layerIdx, int trackIdx) {
        AnimationTrack track = this.dbgGetAnimTrack(layerIdx, trackIdx);
        return track != null ? track.getCurrentTrackTime() : 0.0F;
    }

    public float dbgGetAnimTrackWeight(int layerIdx, int trackIdx) {
        AnimationTrack track = this.dbgGetAnimTrack(layerIdx, trackIdx);
        return track != null ? track.getBlendWeight() : 0.0F;
    }

    /**
     * The character's current twist angle, in degrees.
     */
    public float getTwist() {
        return this.animPlayer != null ? (180.0F / (float)Math.PI) * this.animPlayer.getTwistAngle() : 0.0F;
    }

    /**
     * The character's current shoulder-twist angle, in degrees.
     */
    public float getShoulderTwist() {
        return this.animPlayer != null ? (180.0F / (float)Math.PI) * this.animPlayer.getShoulderTwistAngle() : 0.0F;
    }

    /**
     * The maximum twist angle, in degrees.
     */
    public float getMaxTwist() {
        return this.maxTwist;
    }

    /**
     * Specify the maximum twist angle, in degrees.
     */
    public void setMaxTwist(float degrees) {
        this.maxTwist = degrees;
    }

    /**
     * The character's excess twist, in degrees.
     *   The excess is > 0 if the character is trying to twist further than their current maximum twist.
     *   ie. The amount that the desired twist exceeds the maximum twist.
     * 
     *   eg. If the character is trying to twist by 90 degrees, but their maximum is set to 70, then excess = 20
     */
    public float getExcessTwist() {
        return this.animPlayer != null ? (180.0F / (float)Math.PI) * this.animPlayer.getExcessTwistAngle() : 0.0F;
    }

    public int getNumTwistBones() {
        return this.animPlayer != null ? this.animPlayer.getNumTwistBones() : 0;
    }

    public float getAbsoluteExcessTwist() {
        return Math.abs(this.getExcessTwist());
    }

    public float getAnimAngleTwistDelta() {
        return this.animPlayer != null ? this.animPlayer.angleTwistDelta : 0.0F;
    }

    public float getAnimAngleStepDelta() {
        return this.animPlayer != null ? this.animPlayer.angleStepDelta : 0.0F;
    }

    /**
     * The desired twist, unclamped, in degrees.
     */
    public float getTargetTwist() {
        return this.animPlayer != null ? (180.0F / (float)Math.PI) * this.animPlayer.getTargetTwistAngle() : 0.0F;
    }

    @Override
    public boolean isRangedWeaponEmpty() {
        return this.rangedWeaponEmpty;
    }

    @Override
    public void setRangedWeaponEmpty(boolean val) {
        this.rangedWeaponEmpty = val;
    }

    public boolean hasFootInjury() {
        return !StringUtils.isNullOrWhitespace(this.getFootInjuryType());
    }

    public boolean isInTrees2(boolean ignoreBush) {
        if (this.isCurrentState(BumpedState.instance())) {
            return false;
        } else {
            IsoGridSquare currentSquare = this.getCurrentSquare();
            if (currentSquare == null) {
                return false;
            } else {
                if (currentSquare.has(IsoObjectType.tree)) {
                    IsoTree tree = currentSquare.getTree();
                    if (tree == null || ignoreBush && tree.getSize() > 2 || !ignoreBush) {
                        return true;
                    }
                }

                String Movement = currentSquare.getProperties().get("Movement");
                return !"HedgeLow".equalsIgnoreCase(Movement) && !"HedgeHigh".equalsIgnoreCase(Movement) ? !ignoreBush && currentSquare.hasBush() : true;
            }
        }
    }

    public boolean isInTreesNoBush() {
        return this.isInTrees2(true);
    }

    public boolean isInTrees() {
        return this.isInTrees2(false);
    }

    /**
     * @return the SurvivorMap
     */
    public static HashMap<Integer, SurvivorDesc> getSurvivorMap() {
        return SurvivorMap;
    }

    /**
     * @return the LevelUpLevels
     */
    public static int[] getLevelUpLevels() {
        return LevelUpLevels;
    }

    /**
     * @return the tempo
     */
    public static Vector2 getTempo() {
        return tempo;
    }

    public static Vector2 getTempo2() {
        return tempo2;
    }

    /**
     * @return the inf
     */
    public static ColorInfo getInf() {
        return inf;
    }

    public boolean getIsNPC() {
        return this.isNpc;
    }

    public void setIsNPC(boolean isAI) {
        this.isNpc = isAI;
    }

    @Override
    public BaseCharacterSoundEmitter getEmitter() {
        return this.emitter;
    }

    public void updateEmitter() {
        this.getFMODParameters().update();
        if (IsoWorld.instance.emitterUpdate || this.emitter.hasSoundsToStart()) {
            if (this.isZombie() && this.isProne()) {
                CombatManager.getBoneWorldPos(this, "Bip01_Head", tempVectorBonePos);
                this.emitter.set(tempVectorBonePos.x, tempVectorBonePos.y, this.getZ());
                this.emitter.tick();
            } else {
                this.emitter.set(this.getX(), this.getY(), this.getZ());
                this.emitter.tick();
            }
        }
    }

    protected void doDeferredMovement() {
        if (this.hasAnimationPlayer()) {
            if (GameClient.client && HitReactionNetworkAI.isEnabled(this) && this.getHitReactionNetworkAI() != null) {
                if (this.getHitReactionNetworkAI().isStarted()) {
                    this.getHitReactionNetworkAI().move();
                    this.animPlayer.resetDeferredMovementAccum();
                    return;
                }

                if (this.isDead() && this.getHitReactionNetworkAI().isDoSkipMovement()) {
                    this.animPlayer.resetDeferredMovementAccum();
                    return;
                }
            }

            if (GameServer.server || this.isAnimationUpdatingThisFrame()) {
                Vector2 dMovement = tempo;
                this.getDeferredMovement(dMovement, true);
                if (this.getPath2() != null && !this.isCurrentState(ClimbOverFenceState.instance()) && !this.isCurrentState(ClimbThroughWindowState.instance())
                    )
                 {
                    if (this.isCurrentState(WalkTowardState.instance())
                        || this.isCurrentState(AnimalWalkState.instance()) && !this.getNetworkCharacterAI().usePathFind) {
                        DebugLog.General.warn("WalkTowardState but path2 != null");
                        this.setPath2(null);
                    }
                } else {
                    if (this.isCurrentState(WalkTowardState.instance())) {
                        Vector2 targetVec = BaseVehicle.allocVector2();
                        targetVec.x = this.getPathFindBehavior2().getTargetX();
                        targetVec.y = this.getPathFindBehavior2().getTargetY();
                        targetVec.x = targetVec.x - this.getX();
                        targetVec.y = targetVec.y - this.getY();
                        if (targetVec.getLengthSquared() < dMovement.getLengthSquared()) {
                            dMovement.setLength(targetVec.getLength());
                        }

                        BaseVehicle.releaseVector2(targetVec);
                    }

                    if (GameClient.client) {
                        if (this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
                            if (this.getCurrentState() != ClimbOverFenceState.instance()
                                && this.getCurrentState() != ClimbThroughWindowState.instance()
                                && this.getCurrentState() != ClimbOverWallState.instance()
                                && this.getCurrentState() != StaggerBackState.instance()
                                && this.getCurrentState() != ZombieHitReactionState.instance()
                                && this.getCurrentState() != ZombieFallDownState.instance()
                                && this.getCurrentState() != ZombieFallingState.instance()
                                && this.getCurrentState() != ZombieOnGroundState.instance()
                                && this.getCurrentState() != AttackNetworkState.instance()) {
                                this.animPlayer.resetDeferredMovementAccum();
                                return;
                            }
                        } else if (this instanceof IsoAnimal && !((IsoAnimal)this).isLocalPlayer()) {
                            if (!this.isCurrentState(AnimalIdleState.instance()) && !((IsoAnimal)this).isHappy()) {
                                return;
                            }
                        } else if (this instanceof IsoPlayer
                            && !((IsoPlayer)this).isLocalPlayer()
                            && !this.isCurrentState(CollideWithWallState.instance())
                            && !this.isCurrentState(PlayerGetUpState.instance())
                            && !this.isCurrentState(BumpedState.instance())) {
                            return;
                        }
                    }

                    if (this.isGrappling() || this.isBeingGrappled()) {
                        Vector3 grappleOffset = new Vector3();
                        this.getGrappleOffset(grappleOffset);
                        dMovement.x = dMovement.x + grappleOffset.x;
                        dMovement.y = dMovement.y + grappleOffset.y;
                    }

                    if (GameClient.client && this instanceof IsoZombie && this.isCurrentState(StaggerBackState.instance())) {
                        float len = dMovement.getLength();
                        dMovement.set(this.getHitDir());
                        dMovement.setLength(len);
                    }

                    if (this.isDeferredMovementEnabled()) {
                        if (this.isAnimationRecorderActive()) {
                            this.setVariable("deferredMovement.x", dMovement.x);
                            this.setVariable("deferredMovement.y", dMovement.y);
                        }

                        this.MoveUnmodded(dMovement);
                    } else if (this.isAnimationRecorderActive()) {
                        this.setVariable("deferredMovement.x", 0.0F);
                        this.setVariable("deferredMovement.y", 0.0F);
                    }
                }
            }
        }
    }

    public void doDeferredMovementFromRagdoll(Vector2 dMovement) {
        if (this.isRagdoll()) {
            if (this.hasAnimationPlayer()) {
                if (GameServer.server || this.isAnimationUpdatingThisFrame()) {
                    this.moveUnmoddedInternal(dMovement.x, dMovement.y);
                    this.setX(this.getNextX());
                    this.setY(this.getNextY());
                    if (this.isAnimationRecorderActive()) {
                        this.setVariable("deferredMovement_Ragdoll.x", dMovement.x);
                        this.setVariable("deferredMovement_Ragdoll.y", dMovement.y);
                    }
                }
            }
        }
    }

    @Override
    public ActionContext getActionContext() {
        return this.actionContext;
    }

    public String getPreviousActionContextStateName() {
        ActionContext context = this.getActionContext();
        return context == null ? "" : context.getPreviousStateName();
    }

    public String getCurrentActionContextStateName() {
        ActionContext context = this.getActionContext();
        return context != null && context.getCurrentState() != null ? context.getCurrentStateName() : "";
    }

    @Override
    public boolean hasAnimationPlayer() {
        return this.animPlayer != null;
    }

    @Override
    public AnimationPlayer getAnimationPlayer() {
        Model model = ModelManager.instance.getBodyModel(this);
        boolean hasTracks = false;
        if (this.animPlayer != null && this.animPlayer.getModel() != model) {
            hasTracks = this.animPlayer.getMultiTrack().getTrackCount() > 0;
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }

        if (this.animPlayer == null) {
            this.animPlayer = AnimationPlayer.alloc(model);
            this.onAnimPlayerCreated(this.animPlayer);
            if (hasTracks) {
                this.getAdvancedAnimator().OnAnimDataChanged(false);
            }
        }

        return this.animPlayer;
    }

    public void releaseAnimationPlayer() {
        this.animPlayer = Pool.tryRelease(this.animPlayer);
    }

    protected void onAnimPlayerCreated(AnimationPlayer animationPlayer) {
        animationPlayer.setIsoGameCharacter(this);
        animationPlayer.setRecorder(this.animationRecorder);
        animationPlayer.setTwistBones("Bip01_Pelvis", "Bip01_Spine", "Bip01_Spine1", "Bip01_Neck", "Bip01_Head");
        animationPlayer.setCounterRotationBone("Bip01");
    }

    protected void updateAnimationRecorderState() {
        if (this.animPlayer != null) {
            if (IsoWorld.isAnimRecorderDiscardTriggered()) {
                this.animPlayer.discardRecording();
            }

            boolean isWorldRecording = IsoWorld.isAnimRecorderActive();
            if (isWorldRecording) {
                this.animPlayerRecordingExclusive = false;
            }

            boolean isRecordingExclusive = this.animPlayerRecordingExclusive;
            boolean isRecording = (isRecordingExclusive || isWorldRecording) && !this.isSceneCulled();
            if (isRecording) {
                this.getAnimationPlayerRecorder().logCharacterPos();
            }

            this.animPlayer.setRecording(isRecording);
        }
    }

    public boolean isAnimRecorderActive() {
        return this.hasAnimationPlayer() && this.animPlayer.isRecording();
    }

    public void setAnimRecorderActive(boolean in_isActive, boolean in_isExclusive) {
        this.animPlayerRecordingExclusive = in_isExclusive && in_isActive;
        if (this.hasAnimationPlayer()) {
            this.animPlayer.setRecording(in_isActive);
        }
    }

    @Override
    public AdvancedAnimator getAdvancedAnimator() {
        return this.advancedAnimator;
    }

    @Override
    public ModelInstance getModelInstance() {
        if (this.legsSprite == null) {
            return null;
        } else {
            return this.legsSprite.modelSlot == null ? null : this.legsSprite.modelSlot.model;
        }
    }

    public String getCurrentStateName() {
        return this.stateMachine.getCurrent() == null ? null : this.stateMachine.getCurrent().getName();
    }

    public String getPreviousStateName() {
        return this.stateMachine.getPrevious() == null ? null : this.stateMachine.getPrevious().getName();
    }

    public String getAnimationDebug() {
        return this.advancedAnimator != null ? this.instancename + "\n" + this.advancedAnimator.GetDebug() : this.instancename + "\n - No Animator";
    }

    public String getStatisticsDebug() {
        return "Statistics" + StatisticsManager.getInstance().getAllStatisticsDebug();
    }

    @Override
    public String getTalkerType() {
        return this.chatElement.getTalkerType();
    }

    public void spinToZeroAllAnimNodes() {
        AdvancedAnimator aa = this.getAdvancedAnimator();
        AnimLayer rootLayer = aa.getRootLayer();
        List<LiveAnimNode> liveAnimNodes = rootLayer.getLiveAnimNodes();

        for (int i = 0; i < liveAnimNodes.size(); i++) {
            LiveAnimNode animNode = liveAnimNodes.get(i);
            animNode.stopTransitionIn();
            animNode.setWeightsToZero();
        }
    }

    public boolean isAnimForecasted() {
        return System.currentTimeMillis() < this.isAnimForecasted;
    }

    public void setAnimForecasted(int timeMs) {
        this.isAnimForecasted = System.currentTimeMillis() + timeMs;
    }

    @Override
    public void resetModel() {
        ModelManager.instance.Reset(this);
    }

    @Override
    public void resetModelNextFrame() {
        ModelManager.instance.ResetNextFrame(this);
    }

    protected void onTrigger_setClothingToXmlTriggerFile(TriggerXmlFile triggerXml) {
        OutfitManager.Reload();
        if (!StringUtils.isNullOrWhitespace(triggerXml.outfitName)) {
            String outfitName = triggerXml.outfitName;
            DebugLog.Clothing.debugln("Desired outfit name: " + outfitName);
            Outfit desiredOutfitSource;
            if (triggerXml.isMale) {
                desiredOutfitSource = OutfitManager.instance.FindMaleOutfit(outfitName);
            } else {
                desiredOutfitSource = OutfitManager.instance.FindFemaleOutfit(outfitName);
            }

            if (desiredOutfitSource == null) {
                DebugLog.Clothing.error("Could not find outfit: " + outfitName);
                return;
            }

            if (this.female == triggerXml.isMale && this instanceof IHumanVisual) {
                ((IHumanVisual)this).getHumanVisual().clear();
            }

            this.female = !triggerXml.isMale;
            if (this.descriptor != null) {
                this.descriptor.setFemale(this.female);
            }

            this.dressInNamedOutfit(desiredOutfitSource.name);
            this.advancedAnimator.OnAnimDataChanged(false);
            if (this instanceof IsoPlayer) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
        } else if (!StringUtils.isNullOrWhitespace(triggerXml.clothingItemGuid)) {
            String gameModID = "game";
            String itemGUID = "game-" + triggerXml.clothingItemGuid;
            boolean foundItem = OutfitManager.instance.getClothingItem(itemGUID) != null;
            if (!foundItem) {
                for (String modID : ZomboidFileSystem.instance.getModIDs()) {
                    itemGUID = modID + "-" + triggerXml.clothingItemGuid;
                    if (OutfitManager.instance.getClothingItem(itemGUID) != null) {
                        foundItem = true;
                        break;
                    }
                }
            }

            if (foundItem) {
                this.dressInClothingItem(itemGUID);
                if (this instanceof IsoPlayer) {
                    LuaEventManager.triggerEvent("OnClothingUpdated", this);
                }
            }
        }

        ModelManager.instance.Reset(this);
    }

    protected void onTrigger_setAnimStateToTriggerFile(AnimStateTriggerXmlFile triggerXml) {
        String animSetName = this.GetAnimSetName();
        if (!StringUtils.equalsIgnoreCase(animSetName, triggerXml.animSet)) {
            this.setVariable("dbgForceAnim", false);
            this.restoreAnimatorStateToActionContext();
        } else {
            DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.setValue(triggerXml.forceAnim);
            if (this.advancedAnimator.containsState(triggerXml.stateName)) {
                this.setVariable("dbgForceAnim", triggerXml.forceAnim);
                this.setVariable("dbgForceAnimStateName", triggerXml.stateName);
                this.setVariable("dbgForceAnimNodeName", triggerXml.nodeName);
                this.setVariable("dbgForceAnimScalars", triggerXml.setScalarValues);
                this.setVariable("dbgForceScalar", triggerXml.scalarValue);
                this.setVariable("dbgForceScalar2", triggerXml.scalarValue2);
                this.advancedAnimator.setState(triggerXml.stateName);
            } else {
                DebugLog.Animation.error("State not found: " + triggerXml.stateName);
                this.restoreAnimatorStateToActionContext();
            }
        }
    }

    private void restoreAnimatorStateToActionContext() {
        if (this.actionContext.getCurrentState() != null) {
            this.advancedAnimator
                .setState(this.actionContext.getCurrentStateName(), PZArrayUtil.listConvert(this.actionContext.getChildStates(), state -> state.getName()));
        }
    }

    /**
     * clothingItemChanged
     *  Called when a ClothingItem file has changed on disk, causing the OutfitManager to broadcast this event.
     *  Checks if this item is currently used by this player's Outfit.
     *  Reloads and re-equips if so.
     * 
     * @param itemGuid The item's Globally Unique Identifier (GUID).
     */
    @Override
    public void clothingItemChanged(String itemGuid) {
        if (this.wornItems != null) {
            for (int i = 0; i < this.wornItems.size(); i++) {
                InventoryItem item = this.wornItems.getItemByIndex(i);
                ClothingItem clothingItem = item.getClothingItem();
                if (clothingItem != null && clothingItem.isReady() && clothingItem.guid.equals(itemGuid)) {
                    ClothingItemReference itemRef = new ClothingItemReference();
                    itemRef.itemGuid = itemGuid;
                    itemRef.randomize();
                    item.getVisual().synchWithOutfit(itemRef);
                    item.synchWithVisual();
                    this.resetModelNextFrame();
                }
            }
        }
    }

    public void reloadOutfit() {
        ModelManager.instance.Reset(this);
    }

    /**
     * Specify whether this character is currently not to be drawn, as it is outside the visible area.
     *  Eg. Zombies not seen by the player. Objects outside the rendered window etc.
     */
    @Override
    public void setSceneCulled(boolean isCulled) {
        super.setSceneCulled(isCulled);

        try {
            if (this.isSceneCulled()) {
                ModelManager.instance.Remove(this);
            } else {
                ModelManager.instance.Add(this);
            }
        } catch (Exception var3) {
            System.err.println("Error in IsoGameCharacter.setSceneCulled(" + isCulled + "):");
            ExceptionLogger.logException(var3);
            ModelManager.instance.Remove(this);
            this.legsSprite.modelSlot = null;
        }
    }

    public void setAddedToModelManager(ModelManager modelManager, boolean isAdded) {
        if (this.isAddedToModelManager != isAdded) {
            this.isAddedToModelManager = isAdded;
            if (isAdded) {
                this.restoreAnimatorStateToActionContext();
                DebugFileWatcher.instance.add(this.animStateTriggerWatcher);
                OutfitManager.instance.addClothingItemListener(this);
            } else {
                DebugFileWatcher.instance.remove(this.animStateTriggerWatcher);
                OutfitManager.instance.removeClothingItemListener(this);
            }
        }
    }

    public boolean isAddedToModelManager() {
        return this.isAddedToModelManager;
    }

    /**
     * Picks a random outfit from the OutfitManager
     */
    public void dressInRandomOutfit() {
        if (DebugLog.isEnabled(DebugType.Clothing)) {
            DebugLog.Clothing.println("IsoGameCharacter.dressInRandomOutfit>");
        }

        Outfit randomOutfitSource = OutfitManager.instance.GetRandomOutfit(this.isFemale());
        if (randomOutfitSource != null) {
            this.dressInNamedOutfit(randomOutfitSource.name);
        }
    }

    public void dressInRandomNonSillyOutfit() {
        DebugLog.Clothing.println("IsoGameCharacter.dressInRandomOutfit>");
        Outfit randomOutfitSource = OutfitManager.instance.GetRandomNonSillyOutfit(this.isFemale());
        if (randomOutfitSource != null) {
            this.dressInNamedOutfit(randomOutfitSource.name);
        }
    }

    @Override
    public void dressInNamedOutfit(String outfitName) {
    }

    @Override
    public void dressInPersistentOutfit(String outfitName) {
        if (this.isZombie()) {
            this.getDescriptor().setForename(SurvivorFactory.getRandomForename(this.isFemale()));
        }

        int outfitID = PersistentOutfits.instance.pickOutfit(outfitName, this.isFemale());
        this.dressInPersistentOutfitID(outfitID);
    }

    @Override
    public void dressInPersistentOutfitID(int outfitID) {
    }

    @Override
    public String getOutfitName() {
        if (this instanceof IHumanVisual) {
            HumanVisual humanVisual = ((IHumanVisual)this).getHumanVisual();
            Outfit outfit = humanVisual.getOutfit();
            return outfit == null ? null : outfit.name;
        } else {
            return null;
        }
    }

    public void dressInClothingItem(String itemGUID) {
    }

    public Outfit getRandomDefaultOutfit() {
        IsoGridSquare square = this.getCurrentSquare();
        IsoRoom room = square == null ? null : square.getRoom();
        String roomName = room == null ? null : room.getName();
        return ZombiesZoneDefinition.getRandomDefaultOutfit(this.isFemale(), roomName);
    }

    public ModelInstance getModel() {
        return this.legsSprite != null && this.legsSprite.modelSlot != null ? this.legsSprite.modelSlot.model : null;
    }

    public boolean hasActiveModel() {
        return this.legsSprite != null && this.legsSprite.hasActiveModel();
    }

    @Override
    public boolean hasItems(String type, int count) {
        int total = this.inventory.getItemCount(type);
        return count <= total;
    }

    public int getLevelUpLevels(int level) {
        return LevelUpLevels.length <= level ? LevelUpLevels[LevelUpLevels.length - 1] : LevelUpLevels[level];
    }

    public int getLevelMaxForXp() {
        return LevelUpLevels.length;
    }

    @Override
    public int getXpForLevel(int level) {
        return level < LevelUpLevels.length
            ? (int)(LevelUpLevels[level] * this.levelUpMultiplier)
            : (int)((LevelUpLevels[LevelUpLevels.length - 1] + (level - LevelUpLevels.length + 1) * 400) * this.levelUpMultiplier);
    }

    public void DoDeath(HandWeapon weapon, IsoGameCharacter wielder) {
        this.DoDeath(weapon, wielder, true);
    }

    public void DoDeath(HandWeapon in_weapon, IsoGameCharacter in_wielder, boolean in_isGory) {
        this.OnDeath();
        if (this.getAttackedBy() instanceof IsoPlayer && GameServer.server && this instanceof IsoPlayer) {
            String steamID = "";
            String steamID2 = "";
            if (SteamUtils.isSteamModeEnabled()) {
                steamID = " (" + ((IsoPlayer)this.getAttackedBy()).getSteamID() + ") ";
                steamID2 = " (" + ((IsoPlayer)this).getSteamID() + ") ";
            }

            PVPLogTool.logKill((IsoPlayer)this.getAttackedBy(), (IsoPlayer)this);
        } else {
            if (GameServer.server && this instanceof IsoPlayer) {
                LoggerManager.getLogger("user").write("user " + ((IsoPlayer)this).username + " died at " + LoggerManager.getPlayerCoords(this) + " (non pvp)");
            }

            if (ServerOptions.instance.announceDeath.getValue() && !this.isAnimal() && this instanceof IsoPlayer && GameServer.server) {
                ChatServer.getInstance().sendMessageToServerChat(((IsoPlayer)this).username + " is dead.");
            }
        }

        this.doDeathSplatterAndSounds(in_weapon, in_wielder, in_isGory);
    }

    private void doDeathSplatterAndSounds(HandWeapon in_weapon, IsoGameCharacter in_wielder, boolean in_isGory) {
        if (this.onDeath_ShouldDoSplatterAndSounds(in_weapon, in_wielder, in_isGory)) {
            if (this.isDoDeathSound()) {
                this.playDeadSound();
            }

            this.setDoDeathSound(false);
            if (this.isDead()) {
                float dz = 0.5F;
                if (this.isZombie() && (((IsoZombie)this).crawling || this.getCurrentState() == ZombieOnGroundState.instance())) {
                    dz = 0.2F;
                }

                if (GameServer.server && in_isGory) {
                    boolean isRadial = this.isOnFloor() && in_wielder instanceof IsoPlayer && in_weapon != null && "BareHands".equals(in_weapon.getType());
                    GameServer.sendBloodSplatter(in_weapon, this.getX(), this.getY(), this.getZ() + dz, this.getHitDir(), this.isCloseKilled(), isRadial);
                }

                if (in_weapon != null && SandboxOptions.instance.bloodLevel.getValue() > 1 && in_isGory) {
                    int spn = in_weapon.getSplatNumber();
                    if (spn < 1) {
                        spn = 1;
                    }

                    if (Core.lastStand) {
                        spn *= 3;
                    }

                    switch (SandboxOptions.instance.bloodLevel.getValue()) {
                        case 2:
                            spn /= 2;
                        case 3:
                        default:
                            break;
                        case 4:
                            spn *= 2;
                            break;
                        case 5:
                            spn *= 5;
                    }

                    for (int n = 0; n < spn; n++) {
                        this.splatBlood(3, 0.3F);
                    }
                }

                if (in_weapon != null && SandboxOptions.instance.bloodLevel.getValue() > 1 && in_isGory) {
                    this.splatBloodFloorBig();
                }

                if (in_wielder != null && in_wielder.xp != null) {
                    in_wielder.xp.AddXP(in_weapon, 3);
                }

                if (SandboxOptions.instance.bloodLevel.getValue() > 1
                    && this.isOnFloor()
                    && in_wielder instanceof IsoPlayer player
                    && in_weapon == player.bareHands
                    && in_isGory) {
                    this.playBloodSplatterSound();

                    for (int sx = -1; sx <= 1; sx++) {
                        for (int sy = -1; sy <= 1; sy++) {
                            if (sx != 0 || sy != 0) {
                                new IsoZombieGiblets(
                                    IsoZombieGiblets.GibletType.A,
                                    this.getCell(),
                                    this.getX(),
                                    this.getY(),
                                    this.getZ() + dz,
                                    sx * Rand.Next(0.25F, 0.5F),
                                    sy * Rand.Next(0.25F, 0.5F)
                                );
                            }
                        }
                    }

                    new IsoZombieGiblets(
                        IsoZombieGiblets.GibletType.Eye,
                        this.getCell(),
                        this.getX(),
                        this.getY(),
                        this.getZ() + dz,
                        this.getHitDir().x * 0.8F,
                        this.getHitDir().y * 0.8F
                    );
                } else if (SandboxOptions.instance.bloodLevel.getValue() > 1 && in_isGory) {
                    this.playBloodSplatterSound();
                    new IsoZombieGiblets(
                        IsoZombieGiblets.GibletType.A,
                        this.getCell(),
                        this.getX(),
                        this.getY(),
                        this.getZ() + dz,
                        this.getHitDir().x * 1.5F,
                        this.getHitDir().y * 1.5F
                    );
                    tempo.x = this.getHitDir().x;
                    tempo.y = this.getHitDir().y;
                    int rand = 3;
                    int rand2 = 0;
                    int nbRepeat = 1;
                    switch (SandboxOptions.instance.bloodLevel.getValue()) {
                        case 1:
                            nbRepeat = 0;
                            break;
                        case 2:
                            nbRepeat = 1;
                            rand = 5;
                            rand2 = 2;
                        case 3:
                        default:
                            break;
                        case 4:
                            nbRepeat = 3;
                            rand = 2;
                            break;
                        case 5:
                            nbRepeat = 10;
                            rand = 0;
                    }

                    for (int i = 0; i < nbRepeat; i++) {
                        if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 1.5F,
                                this.getHitDir().y * 1.5F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 1.5F,
                                this.getHitDir().y * 1.5F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 1.8F,
                                this.getHitDir().y * 1.8F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 1.9F,
                                this.getHitDir().y * 1.9F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 3.5F,
                                this.getHitDir().y * 3.5F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 3.8F,
                                this.getHitDir().y * 3.8F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 3.9F,
                                this.getHitDir().y * 3.9F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 1.5F,
                                this.getHitDir().y * 1.5F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 3.8F,
                                this.getHitDir().y * 3.8F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.A,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 3.9F,
                                this.getHitDir().y * 3.9F
                            );
                        }

                        if (Rand.Next(this.isCloseKilled() ? 9 : 6) == 0) {
                            new IsoZombieGiblets(
                                IsoZombieGiblets.GibletType.Eye,
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                this.getZ() + dz,
                                this.getHitDir().x * 0.8F,
                                this.getHitDir().y * 0.8F
                            );
                        }
                    }
                }
            }
        }
    }

    public boolean onDeath_ShouldDoSplatterAndSounds(HandWeapon in_weapon, IsoGameCharacter in_wielder, boolean in_isGory) {
        return true;
    }

    protected boolean TestIfSeen(int playerIndex, IsoPlayer player) {
        if (player != null && this != player) {
            float dist = this.DistToProper(player);
            if (dist > GameTime.getInstance().getViewDist()) {
                return false;
            } else {
                boolean couldSee = GameServer.server ? ServerLOS.instance.isCouldSee(player, this.current) : this.current.isCouldSee(playerIndex);
                boolean canSee = GameServer.server ? couldSee : this.current.isCanSee(playerIndex);
                if (!canSee && this instanceof IsoZombie zombie && zombie.canSeeHeadSquare(player)) {
                    canSee = true;
                }

                if (!canSee && couldSee) {
                    canSee = dist < player.getSeeNearbyCharacterDistance();
                }

                if (!canSee) {
                    return false;
                } else {
                    ColorInfo lightInfo = this.getCurrentSquare().lighting[playerIndex].lightInfo();
                    if (lightInfo == null) {
                        return false;
                    } else {
                        float delta = (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0F;
                        if (dist < player.getSeeNearbyCharacterDistance() && player.getCurrentSquare() != null) {
                            lightInfo = player.getCurrentSquare().lighting[playerIndex].lightInfo();
                            if (lightInfo != null) {
                                delta = PZMath.max(delta, (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0F);
                            }
                        }

                        if (delta > 0.6F) {
                            delta = 1.0F;
                        }

                        float delta2 = 1.0F - dist / GameTime.getInstance().getViewDist();
                        if (delta == 1.0F && delta2 > 0.3F) {
                            delta2 = 1.0F;
                        }

                        float angle = player.getDotWithForwardDirection(this.getX(), this.getY());
                        if (angle < 0.5F) {
                            angle = 0.5F;
                        }

                        delta *= angle;
                        if (delta < 0.0F) {
                            delta = 0.0F;
                        }

                        if (dist <= 1.0F) {
                            delta2 = 1.0F;
                            delta *= 2.0F;
                        }

                        delta *= delta2;
                        delta *= 100.0F;
                        return delta > 0.025F;
                    }
                }
            }
        } else {
            return false;
        }
    }

    public void clearFallDamage() {
        this.fallDamage.reset();
    }

    public float getImpactIsoSpeed() {
        return this.fallDamage.getImpactIsoSpeed();
    }

    public void DoLand(float in_impactIsoSpeed) {
        if (!this.isClimbing() && !this.isRagdollFall()) {
            boolean isFall = FallingConstants.isFall(in_impactIsoSpeed);
            if (isFall) {
                this.fallDamage.setLandingImpact(in_impactIsoSpeed);
                DebugType.FallDamage
                    .debugln(
                        "DoLand %s speed: %s u/s %f m/s, %f km/h",
                        FallingConstants.getFallSeverity(in_impactIsoSpeed),
                        in_impactIsoSpeed,
                        in_impactIsoSpeed * 2.44949F,
                        in_impactIsoSpeed * 3.6F * 2.44949F
                    );
                this.handleLandingImpact(this.fallDamage);
            }
        } else {
            this.fallDamage.reset();
        }
    }

    protected void handleLandingImpact(FallDamage fallDamage) {
        boolean isDamagingFall = fallDamage.isDamagingFall();
        boolean isMoreThanLightFall = fallDamage.isMoreThanLightFall();
        boolean isMoreThanHardFall = fallDamage.isMoreThanHardFall();
        boolean isLethalFall = fallDamage.isLethalFall();
        boolean bWasAlive = this.isAlive();
        if (isDamagingFall && !this.isClimbing()) {
            boolean unscratch = Rand.NextBool(80 - this.getPerkLevel(PerkFactory.Perks.Nimble));
            float damageAlpha = fallDamage.getImpactIsoSpeed() / FallingConstants.hardFallThreshold;
            float healthReductionRaw = PZMath.lerpFunc_EaseOutQuad(damageAlpha);
            float healthReduction = healthReductionRaw * 115.0F;
            float randomizer = Rand.Next(0.5F, 1.0F);
            healthReduction *= randomizer;
            float weightMultiplier = this.getInventory().getCapacityWeight() / this.getInventory().getMaxWeight();
            weightMultiplier = Math.min(1.8F, weightMultiplier);
            healthReduction *= weightMultiplier;
            if (this.getCurrentSquare().getFloor() != null
                && this.getCurrentSquare().getFloor().getSprite().getName() != null
                && this.getCurrentSquare().getFloor().getSprite().getName().startsWith("blends_natural")) {
                healthReduction *= 0.8F;
                if (!unscratch) {
                    unscratch = Rand.NextBool(65 - this.getPerkLevel(PerkFactory.Perks.Nimble));
                }
            }

            if (this.characterTraits.get(CharacterTrait.OBESE) || this.characterTraits.get(CharacterTrait.EMACIATED)) {
                healthReduction *= 1.4F;
            } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT) || this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
                healthReduction *= 1.2F;
            }

            healthReduction *= Math.max(0.1F, 1.0F - this.getPerkLevel(PerkFactory.Perks.Fitness) * 0.05F);
            healthReduction *= Math.max(0.1F, 1.0F - this.getPerkLevel(PerkFactory.Perks.Nimble) * 0.05F);
            boolean fatal = isLethalFall && !unscratch;
            if (fatal) {
                healthReduction = 1000.0F;
            } else if (isLethalFall) {
                healthReduction *= 0.05F;
            } else if (unscratch) {
                healthReduction = 0.0F;
            }

            if (isMoreThanLightFall) {
                this.fallenOnKnees(true);
                this.dropHandItems();
            } else {
                this.helmetFall(false);
            }

            BodyDamage bodyDamage = this.getBodyDamage();

            for (BodyPart bodyPart : bodyDamage.getBodyParts()) {
                healthReduction *= FallingWhileInjured.getDamageMultiplier(bodyPart);
            }

            if (!(healthReduction <= 0.0F)) {
                bodyDamage.ReduceGeneralHealth(healthReduction);
                if (healthReduction > 0.0F) {
                    DebugType.FallDamage.debugln("Impact health reduction: %f. isAlive: %s", healthReduction, !this.isDead());
                    bodyDamage.Update();
                    this.setKilledByFall(bWasAlive && this.isDead());
                }

                LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FALLDOWN", healthReduction);
                if (healthReduction > 5.0F && this.isAlive()) {
                    this.playPainVoicesFromFallDamage(fallDamage);
                }

                boolean injury = isDamagingFall && Rand.Next(100) < healthReduction;
                if (injury) {
                    int rand = (int)(healthReductionRaw * 55.0F);
                    if (this.getInventory().getMaxWeight() - this.getInventory().getCapacityWeight() < 2.0F) {
                        rand = (int)(rand + this.getInventory().getCapacityWeight() / this.getInventory().getMaxWeight() * 20.0F);
                    }

                    if (this.characterTraits.get(CharacterTrait.OBESE) || this.characterTraits.get(CharacterTrait.EMACIATED)) {
                        rand += 20;
                    } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT) || this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
                        rand += 10;
                    }

                    if (this.getPerkLevel(PerkFactory.Perks.Fitness) > 4) {
                        rand = (int)(rand - (this.getPerkLevel(PerkFactory.Perks.Fitness) - 4) * 1.5);
                    }

                    rand = (int)(rand - this.getPerkLevel(PerkFactory.Perks.Nimble) * 1.5);
                    BodyPartType bodyPartType;
                    if (!isMoreThanHardFall && (!isMoreThanLightFall || !Rand.NextBool(2))) {
                        bodyPartType = BodyPartType.FromIndex(
                            Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1)
                        );
                    } else {
                        bodyPartType = BodyPartType.FromIndex(
                            Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1)
                        );
                    }

                    if (Rand.Next(100) < rand && SandboxOptions.instance.boneFracture.getValue()) {
                        DebugType.FallDamage.debugln("Impact fracture likely: %s. rand: %d", bodyPartType, rand);
                        bodyDamage.getBodyPart(bodyPartType).generateFractureNew(Rand.Next(50, 80));
                    } else if (Rand.Next(100) < rand + 10) {
                        DebugType.FallDamage.debugln("Impact deep wound likely: %s. rand: %d", bodyPartType, rand);
                        bodyDamage.getBodyPart(bodyPartType).generateDeepWound();
                    } else {
                        DebugType.FallDamage.debugln("Impact stiffness likely: %s. rand: %d", bodyPartType, rand);
                        bodyDamage.getBodyPart(bodyPartType).setStiffness(100.0F);
                    }

                    if (isMoreThanHardFall) {
                        bodyPartType = BodyPartType.FromIndex(
                            Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1)
                        );
                        if (Rand.Next(100) < rand && SandboxOptions.instance.boneFracture.getValue()) {
                            DebugType.FallDamage.debugln("Severe Impact. Fracture likely: %s. rand: %d", bodyPartType, rand);
                            bodyDamage.getBodyPart(bodyPartType).generateFractureNew(Rand.Next(50, 80));
                        } else if (Rand.Next(100) < rand + 10) {
                            DebugType.FallDamage.debugln("Severe Impact. Deep wound likely: %s. rand: %d", bodyPartType, rand);
                            bodyDamage.getBodyPart(bodyPartType).generateDeepWound();
                        } else {
                            DebugType.FallDamage.debugln("Severe Impact. Stiffness likely: %s. rand: %d", bodyPartType, rand);
                            bodyDamage.getBodyPart(bodyPartType).setStiffness(100.0F);
                        }
                    }
                }

                if (fatal) {
                    this.splatBloodFloorBig();
                    this.splatBloodFloorBig();
                    this.splatBloodFloorBig();
                    this.splatBloodFloorBig();
                }
            }
        }
    }

    protected void playPainVoicesFromFallDamage(FallDamage in_fallDamage) {
    }

    public <T> PZArrayList<ItemContainer> getContextWorldContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate
    ) {
        PZArrayList<ItemContainer> containerList = new PZArrayList<>(ItemContainer.class, 10);
        return this.getContextWorldContainers(in_paramToCompare, in_isValidPredicate, containerList);
    }

    public <T> PZArrayList<ItemContainer> getContextWorldContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate, PZArrayList<ItemContainer> inout_containerList
    ) {
        this.square.getAllContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
        IsoDirections forwardDir = this.getForwardMovementIsoDirection();
        this.square.getAllContainersFromAdjacentSquare(forwardDir, in_paramToCompare, in_isValidPredicate, inout_containerList);
        IsoDirections forwardDirL = forwardDir.RotLeft();
        this.square.getAllContainersFromAdjacentSquare(forwardDirL, in_paramToCompare, in_isValidPredicate, inout_containerList);
        IsoDirections forwardDirR = forwardDir.RotRight();
        this.square.getAllContainersFromAdjacentSquare(forwardDirR, in_paramToCompare, in_isValidPredicate, inout_containerList);
        IsoDirections forwardDirLL = forwardDirL.RotLeft();
        this.square.getAllContainersFromAdjacentSquare(forwardDirLL, in_paramToCompare, in_isValidPredicate, inout_containerList);
        IsoDirections forwardDirRR = forwardDirR.RotRight();
        this.square.getAllContainersFromAdjacentSquare(forwardDirRR, in_paramToCompare, in_isValidPredicate, inout_containerList);
        return inout_containerList;
    }

    public <T> PZArrayList<ItemContainer> getContextWorldContainersInObjects(
        IsoObject[] in_contextObjects,
        T in_paramToCompare,
        Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate,
        PZArrayList<ItemContainer> inout_containerList
    ) {
        DebugType.General.noise("Getting ContextWorld Containers");
        if (in_contextObjects == null) {
            return inout_containerList;
        } else {
            for (int i = 0; i < in_contextObjects.length; i++) {
                IsoObject obj = in_contextObjects[i];
                if (obj != null) {
                    if (obj == this) {
                        DebugType.General.noise("Found self in context list, searching surrounding area...");
                        this.getContextWorldContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
                    } else {
                        obj.getContainers(in_paramToCompare, in_isValidPredicate, inout_containerList);
                    }
                }
            }

            return inout_containerList;
        }
    }

    public PZArrayList<ItemContainer> getContextWorldSuitableContainersToDropCorpseInObjects(IsoObject[] in_contextObjects) {
        return this.getContextWorldContainersInObjects(in_contextObjects, this, IsoGameCharacter::canDropCorpseInto, new PZArrayList<>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpseInSquare(IsoGridSquare in_square) {
        return this.getSuitableContainersToDropCorpseInSquare(in_square, new PZArrayList<>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpseInSquare(IsoGridSquare in_square, PZArrayList<ItemContainer> inout_foundContainers) {
        if (in_square == null) {
            return inout_foundContainers;
        } else {
            return this.square == in_square
                ? this.getSuitableContainersToDropCorpse(inout_foundContainers)
                : in_square.getAllContainers(this, IsoGameCharacter::canDropCorpseInto, inout_foundContainers);
        }
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpse() {
        return this.getSuitableContainersToDropCorpse(new PZArrayList<>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpse(PZArrayList<ItemContainer> inout_foundContainers) {
        this.getContextWorldContainers(this, IsoGameCharacter::canDropCorpseInto, inout_foundContainers);
        return inout_foundContainers;
    }

    public PZArrayList<ItemContainer> getContextWorldContainersWithHumanCorpse(IsoObject[] in_contextObjects) {
        return this.getContextWorldContainersInObjects(in_contextObjects, this, IsoGameCharacter::canGrabCorpseFrom, new PZArrayList<>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersWithHumanCorpseInSquare(IsoGridSquare in_square) {
        return this.getSuitableContainersWithHumanCorpseInSquare(in_square, new PZArrayList<>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersWithHumanCorpseInSquare(IsoGridSquare in_square, PZArrayList<ItemContainer> inout_foundContainers) {
        return in_square.getAllContainers(this, IsoGameCharacter::canGrabCorpseFrom, inout_foundContainers);
    }

    public static boolean canDropCorpseInto(IsoGameCharacter in_chr, ItemContainer in_container) {
        if (in_chr == null) {
            return false;
        } else {
            return in_container == null ? false : in_container.canHumanCorpseFit();
        }
    }

    public static boolean canGrabCorpseFrom(IsoGameCharacter in_chr, ItemContainer in_container) {
        if (in_chr == null) {
            return false;
        } else {
            return in_container == null ? false : in_container.containsHumanCorpse();
        }
    }

    public boolean canAccessContainer(ItemContainer in_container) {
        return in_container.doesVehicleDoorNeedOpening() ? in_container.canCharacterOpenVehicleDoor(this) : true;
    }

    public String getContainerToolTip(ItemContainer in_container) {
        if (in_container.doesVehicleDoorNeedOpening()) {
            return !in_container.canCharacterOpenVehicleDoor(this) ? "IGUI_Tooltip_VehicleDoorLocked" : "IGUI_Tooltip_DoorClosed";
        } else {
            return "";
        }
    }

    /**
     * @return the FollowingTarget
     */
    public IsoGameCharacter getFollowingTarget() {
        return this.followingTarget;
    }

    /**
     * 
     * @param FollowingTarget the FollowingTarget to set
     */
    public void setFollowingTarget(IsoGameCharacter FollowingTarget) {
        this.followingTarget = FollowingTarget;
    }

    /**
     * @return the LocalList
     */
    public ArrayList<IsoMovingObject> getLocalList() {
        return this.localList;
    }

    /**
     * @return the LocalNeutralList
     */
    public ArrayList<IsoMovingObject> getLocalNeutralList() {
        return this.localNeutralList;
    }

    /**
     * @return the LocalGroupList
     */
    public ArrayList<IsoMovingObject> getLocalGroupList() {
        return this.localGroupList;
    }

    /**
     * @return the LocalRelevantEnemyList
     */
    public ArrayList<IsoMovingObject> getLocalRelevantEnemyList() {
        return this.localRelevantEnemyList;
    }

    /**
     * @return the dangerLevels
     */
    public float getDangerLevels() {
        return this.dangerLevels;
    }

    /**
     * 
     * @param dangerLevels the dangerLevels to set
     */
    public void setDangerLevels(float dangerLevels) {
        this.dangerLevels = dangerLevels;
    }

    public ArrayList<IsoGameCharacter.PerkInfo> getPerkList() {
        return this.perkList;
    }

    /**
     * @return the leaveBodyTimedown
     */
    public float getLeaveBodyTimedown() {
        return this.leaveBodyTimedown;
    }

    /**
     * 
     * @param leaveBodyTimedown the leaveBodyTimedown to set
     */
    public void setLeaveBodyTimedown(float leaveBodyTimedown) {
        this.leaveBodyTimedown = leaveBodyTimedown;
    }

    /**
     * @return the AllowConversation
     */
    public boolean isAllowConversation() {
        return this.allowConversation;
    }

    /**
     * 
     * @param AllowConversation the AllowConversation to set
     */
    public void setAllowConversation(boolean AllowConversation) {
        this.allowConversation = AllowConversation;
    }

    /**
     * @return the ReanimateTimer
     */
    public float getReanimateTimer() {
        return this.reanimateTimer;
    }

    /**
     * 
     * @param ReanimateTimer the ReanimateTimer to set
     */
    public void setReanimateTimer(float ReanimateTimer) {
        this.reanimateTimer = ReanimateTimer;
    }

    /**
     * @return the ReanimAnimFrame
     */
    public int getReanimAnimFrame() {
        return this.reanimAnimFrame;
    }

    /**
     * 
     * @param ReanimAnimFrame the ReanimAnimFrame to set
     */
    public void setReanimAnimFrame(int ReanimAnimFrame) {
        this.reanimAnimFrame = ReanimAnimFrame;
    }

    /**
     * @return the ReanimAnimDelay
     */
    public int getReanimAnimDelay() {
        return this.reanimAnimDelay;
    }

    /**
     * 
     * @param ReanimAnimDelay the ReanimAnimDelay to set
     */
    public void setReanimAnimDelay(int ReanimAnimDelay) {
        this.reanimAnimDelay = ReanimAnimDelay;
    }

    /**
     * @return the Reanim
     */
    public boolean isReanim() {
        return this.reanim;
    }

    /**
     * 
     * @param Reanim the Reanim to set
     */
    public void setReanim(boolean Reanim) {
        this.reanim = Reanim;
    }

    /**
     * @return the VisibleToNPCs
     */
    public boolean isVisibleToNPCs() {
        return this.visibleToNpcs;
    }

    /**
     * 
     * @param VisibleToNPCs the VisibleToNPCs to set
     */
    public void setVisibleToNPCs(boolean VisibleToNPCs) {
        this.visibleToNpcs = VisibleToNPCs;
    }

    /**
     * @return the DieCount
     */
    public int getDieCount() {
        return this.dieCount;
    }

    /**
     * 
     * @param DieCount the DieCount to set
     */
    public void setDieCount(int DieCount) {
        this.dieCount = DieCount;
    }

    /**
     * @return the llx
     */
    public float getLlx() {
        return this.llx;
    }

    /**
     * 
     * @param llx the llx to set
     */
    public void setLlx(float llx) {
        this.llx = llx;
    }

    /**
     * @return the lly
     */
    public float getLly() {
        return this.lly;
    }

    /**
     * 
     * @param lly the lly to set
     */
    public void setLly(float lly) {
        this.lly = lly;
    }

    /**
     * @return the llz
     */
    public float getLlz() {
        return this.llz;
    }

    /**
     * 
     * @param llz the llz to set
     */
    public void setLlz(float llz) {
        this.llz = llz;
    }

    /**
     * @return the RemoteID
     */
    public int getRemoteID() {
        return this.remoteId;
    }

    /**
     * 
     * @param RemoteID the RemoteID to set
     */
    public void setRemoteID(int RemoteID) {
        this.remoteId = RemoteID;
    }

    /**
     * @return the NumSurvivorsInVicinity
     */
    public int getNumSurvivorsInVicinity() {
        return this.numSurvivorsInVicinity;
    }

    /**
     * 
     * @param NumSurvivorsInVicinity the NumSurvivorsInVicinity to set
     */
    public void setNumSurvivorsInVicinity(int NumSurvivorsInVicinity) {
        this.numSurvivorsInVicinity = NumSurvivorsInVicinity;
    }

    /**
     * @return the LevelUpMultiplier
     */
    public float getLevelUpMultiplier() {
        return this.levelUpMultiplier;
    }

    /**
     * 
     * @param LevelUpMultiplier the LevelUpMultiplier to set
     */
    public void setLevelUpMultiplier(float LevelUpMultiplier) {
        this.levelUpMultiplier = LevelUpMultiplier;
    }

    /**
     * @return the xp
     */
    @Override
    public IsoGameCharacter.XP getXp() {
        return this.xp;
    }

    /**
     * 
     * @param xp the xp to set
     */
    @Deprecated
    public void setXp(IsoGameCharacter.XP xp) {
        this.xp = xp;
    }

    /**
     * @return the LastLocalEnemies
     */
    public int getLastLocalEnemies() {
        return this.lastLocalEnemies;
    }

    /**
     * 
     * @param LastLocalEnemies the LastLocalEnemies to set
     */
    public void setLastLocalEnemies(int LastLocalEnemies) {
        this.lastLocalEnemies = LastLocalEnemies;
    }

    /**
     * @return the VeryCloseEnemyList
     */
    public ArrayList<IsoMovingObject> getVeryCloseEnemyList() {
        return this.veryCloseEnemyList;
    }

    public HashMap<String, IsoGameCharacter.Location> getLastKnownLocation() {
        return this.lastKnownLocation;
    }

    /**
     * @return the AttackedBy
     */
    public IsoGameCharacter getAttackedBy() {
        return this.attackedBy;
    }

    /**
     * 
     * @param AttackedBy the AttackedBy to set
     */
    public void setAttackedBy(IsoGameCharacter AttackedBy) {
        this.attackedBy = AttackedBy;
    }

    /**
     * @return the IgnoreStaggerBack
     */
    public boolean isIgnoreStaggerBack() {
        return this.ignoreStaggerBack;
    }

    /**
     * 
     * @param IgnoreStaggerBack the IgnoreStaggerBack to set
     */
    public void setIgnoreStaggerBack(boolean IgnoreStaggerBack) {
        this.ignoreStaggerBack = IgnoreStaggerBack;
    }

    /**
     * @return the TimeThumping
     */
    public int getTimeThumping() {
        return this.timeThumping;
    }

    /**
     * 
     * @param TimeThumping the TimeThumping to set
     */
    public void setTimeThumping(int TimeThumping) {
        this.timeThumping = TimeThumping;
    }

    /**
     * @return the PatienceMax
     */
    public int getPatienceMax() {
        return this.patienceMax;
    }

    /**
     * 
     * @param PatienceMax the PatienceMax to set
     */
    public void setPatienceMax(int PatienceMax) {
        this.patienceMax = PatienceMax;
    }

    /**
     * @return the PatienceMin
     */
    public int getPatienceMin() {
        return this.patienceMin;
    }

    /**
     * 
     * @param PatienceMin the PatienceMin to set
     */
    public void setPatienceMin(int PatienceMin) {
        this.patienceMin = PatienceMin;
    }

    /**
     * @return the Patience
     */
    public int getPatience() {
        return this.patience;
    }

    /**
     * 
     * @param Patience the Patience to set
     */
    public void setPatience(int Patience) {
        this.patience = Patience;
    }

    /**
     * @return the CharacterActions
     */
    @Override
    public Stack<BaseAction> getCharacterActions() {
        return this.characterActions;
    }

    public boolean hasTimedActions() {
        return !this.characterActions.isEmpty() || this.getVariableBoolean("IsPerformingAnAction");
    }

    public boolean isCurrentActionPathfinding() {
        return this.checkCurrentAction(BaseAction::isPathfinding);
    }

    public boolean isCurrentActionAllowedWhileDraggingCorpses() {
        return this.checkCurrentAction(BaseAction::isAllowedWhileDraggingCorpses);
    }

    public boolean checkCurrentAction(Invokers.Params1.Boolean.ICallback<BaseAction> in_checkPredicate) {
        if (this.characterActions.isEmpty()) {
            return false;
        } else {
            BaseAction action = this.characterActions.get(0);
            return action != null && in_checkPredicate.accept(action);
        }
    }

    /**
     * @return the character's forward direction vector
     */
    @Deprecated
    public Vector2 getForwardDirection() {
        return this.forwardDirection;
    }

    public float getForwardDirectionX() {
        return this.forwardDirection.x;
    }

    public float getForwardDirectionY() {
        return this.forwardDirection.y;
    }

    public Vector2 getForwardDirection(Vector2 out_forwardDirection) {
        out_forwardDirection.x = this.forwardDirection.x;
        out_forwardDirection.y = this.forwardDirection.y;
        return out_forwardDirection;
    }

    /**
     * 
     * @param dir The character's new forward direction.
     */
    public void setForwardDirection(Vector2 dir) {
        if (dir != null) {
            this.setForwardDirection(dir.x, dir.y);
        }
    }

    @Override
    public void setTargetAndCurrentDirection(float in_directionX, float in_directionY) {
        this.setForwardDirection(in_directionX, in_directionY);
        if (this.hasAnimationPlayer()) {
            this.getAnimationPlayer().setTargetAndCurrentDirection(in_directionX, in_directionY);
        }
    }

    @Override
    public void setForwardDirection(float x, float y) {
        this.forwardDirection.x = x;
        this.forwardDirection.y = y;
        this.forwardDirection.normalize();
        this.dir = IsoDirections.fromAngle(x, y);
    }

    public void zeroForwardDirectionX() {
        this.setForwardDirection(0.0F, 1.0F);
    }

    public void zeroForwardDirectionY() {
        this.setForwardDirection(1.0F, 0.0F);
    }

    public float getDirectionAngleRadians() {
        return this.forwardDirection.getDirection();
    }

    /**
     * The forward direction angle, in degrees.
     */
    public float getDirectionAngle() {
        return (180.0F / (float)Math.PI) * this.getDirectionAngleRadians();
    }

    public void setDirectionAngle(float in_angleDegrees) {
        float angleRads = (float) (Math.PI / 180.0) * in_angleDegrees;
        float x = (float)Math.cos(angleRads);
        float y = (float)Math.sin(angleRads);
        this.setForwardDirection(x, y);
    }

    public float getAnimAngle() {
        return this.animPlayer != null && this.animPlayer.isReady() && !this.animPlayer.isBoneTransformsNeedFirstFrame()
            ? (180.0F / (float)Math.PI) * this.animPlayer.getAngle()
            : this.getDirectionAngle();
    }

    public float getAnimAngleRadians() {
        return this.animPlayer != null && this.animPlayer.isReady() && !this.animPlayer.isBoneTransformsNeedFirstFrame()
            ? this.animPlayer.getAngle()
            : this.forwardDirection.getDirection();
    }

    @Deprecated
    public Vector2 getAnimVector(Vector2 out_animForwardDirection) {
        return this.getAnimForwardDirection(out_animForwardDirection);
    }

    @Override
    public Vector2 getAnimForwardDirection(Vector2 out_animForwardDirection) {
        return out_animForwardDirection.setLengthAndDirection(this.getAnimAngleRadians(), 1.0F);
    }

    public float getLookAngleRadians() {
        if (this.animPlayer != null && this.animPlayer.isReady()) {
            float angle = this.animPlayer.getAngle() + this.animPlayer.getTwistAngle();
            if (this.isHeadLookAround()) {
                angle += this.getHeadLookHorizontal();
            }

            return angle;
        } else {
            return this.getDirectionAngleRadians();
        }
    }

    public Vector2 getLookVector(Vector2 vector2) {
        return vector2.setLengthAndDirection(this.getLookAngleRadians(), 1.0F);
    }

    public float getLookDirectionX() {
        Vector2 lookVector = BaseVehicle.allocVector2();
        float x = this.getLookVector(lookVector).x;
        BaseVehicle.releaseVector2(lookVector);
        return x;
    }

    public float getLookDirectionY() {
        Vector2 lookVector = BaseVehicle.allocVector2();
        float y = this.getLookVector(lookVector).y;
        BaseVehicle.releaseVector2(lookVector);
        return y;
    }

    public boolean isAnimatingBackwards() {
        return this.isAnimatingBackwards;
    }

    @Override
    public IsoDirections getForwardMovementIsoDirection() {
        if (this.isAnimatingBackwards()) {
            this.getForwardIsoDirection().Rot180();
        }

        return this.getForwardIsoDirection();
    }

    public void setAnimatingBackwards(boolean m_isAnimatingBackwards) {
        this.isAnimatingBackwards = m_isAnimatingBackwards;
    }

    public boolean isDraggingCorpse() {
        if (!this.isGrappling()) {
            return false;
        } else {
            return this.getGrapplingTarget() instanceof IsoZombie grappledZombie ? grappledZombie.isReanimatedForGrappleOnly() : false;
        }
    }

    public UdpConnection getOwner() {
        return null;
    }

    public void setOwner(UdpConnection connection) {
    }

    public IsoPlayer getOwnerPlayer() {
        return null;
    }

    public void setOwnerPlayer(IsoPlayer player) {
    }

    public float getDotWithForwardDirection(Vector3 bonePos) {
        return this.getDotWithForwardDirection(bonePos.x, bonePos.y);
    }

    public float getDotWithForwardDirection(float targetX, float targetY) {
        Vector2 vectorToTarget = IsoGameCharacter.L_getDotWithForwardDirection.v1.set(targetX - this.getX(), targetY - this.getY());
        vectorToTarget.normalize();
        Vector2 forward = this.getLookVector(IsoGameCharacter.L_getDotWithForwardDirection.v2);
        forward.normalize();
        return vectorToTarget.dot(forward);
    }

    /**
     * @return the Asleep
     */
    @Override
    public boolean isAsleep() {
        return this.asleep;
    }

    /**
     * 
     * @param Asleep the Asleep to set
     */
    @Override
    public void setAsleep(boolean Asleep) {
        this.asleep = Asleep;
    }

    @Override
    public boolean isResting() {
        return this.isResting;
    }

    @Override
    public void setIsResting(boolean isResting) {
        this.isResting = isResting;
    }

    /**
     * @return the ZombieKills
     */
    @Override
    public int getZombieKills() {
        return this.zombieKills;
    }

    /**
     * 
     * @param ZombieKills the ZombieKills to set
     */
    public void setZombieKills(int ZombieKills) {
        this.zombieKills = ZombieKills;
        if (GameServer.server && this instanceof IsoPlayer) {
            SteamGameServer.UpdatePlayer((IsoPlayer)this);
        }
    }

    /**
     * @return the LastZombieKills
     */
    public int getLastZombieKills() {
        return this.lastZombieKills;
    }

    /**
     * 
     * @param LastZombieKills the LastZombieKills to set
     */
    public void setLastZombieKills(int LastZombieKills) {
        this.lastZombieKills = LastZombieKills;
    }

    /**
     * @return the ForceWakeUpTime
     */
    public float getForceWakeUpTime() {
        return this.forceWakeUpTime;
    }

    /**
     * 
     * @param ForceWakeUpTime the ForceWakeUpTime to set
     */
    @Override
    public void setForceWakeUpTime(float ForceWakeUpTime) {
        this.forceWakeUpTime = ForceWakeUpTime;
    }

    public void forceAwake() {
        if (this.isAsleep()) {
            this.forceWakeUp = true;
        }
    }

    /**
     * @return the BodyDamage
     */
    @Override
    public BodyDamage getBodyDamage() {
        return this.bodyDamage;
    }

    @Override
    public BodyDamage getBodyDamageRemote() {
        if (this.bodyDamageRemote == null) {
            this.bodyDamageRemote = new BodyDamage(this);
        }

        return this.bodyDamageRemote;
    }

    public void resetBodyDamageRemote() {
        this.bodyDamageRemote = null;
    }

    /**
     * @return the defaultState
     */
    public State getDefaultState() {
        return this.defaultState;
    }

    /**
     * 
     * @param defaultState the defaultState to set
     */
    public void setDefaultState(State defaultState) {
        this.defaultState = defaultState;
    }

    /**
     * @return the descriptor
     */
    @Override
    public SurvivorDesc getDescriptor() {
        return this.descriptor;
    }

    /**
     * 
     * @param descriptor the descriptor to set
     */
    @Override
    public void setDescriptor(SurvivorDesc descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String getFullName() {
        return this.descriptor != null ? this.descriptor.getForename() + " " + this.descriptor.getSurname() : "Bob Smith";
    }

    @Override
    public BaseVisual getVisual() {
        throw new RuntimeException("subclasses must implement this");
    }

    public ItemVisuals getItemVisuals() {
        throw new RuntimeException("subclasses must implement this");
    }

    public void getItemVisuals(ItemVisuals itemVisuals) {
        this.getWornItems().getItemVisuals(itemVisuals);
    }

    public boolean isUsingWornItems() {
        return this.wornItems != null;
    }

    /**
     * @return the FamiliarBuildings
     */
    public Stack<IsoBuilding> getFamiliarBuildings() {
        return this.familiarBuildings;
    }

    /**
     * @return the finder
     */
    public AStarPathFinderResult getFinder() {
        return this.finder;
    }

    /**
     * @return the FireKillRate
     */
    public float getFireKillRate() {
        return this.fireKillRate;
    }

    /**
     * 
     * @param FireKillRate the FireKillRate to set
     */
    public void setFireKillRate(float FireKillRate) {
        this.fireKillRate = FireKillRate;
    }

    /**
     * @return the FireSpreadProbability
     */
    public int getFireSpreadProbability() {
        return this.fireSpreadProbability;
    }

    /**
     * 
     * @param FireSpreadProbability the FireSpreadProbability to set
     */
    public void setFireSpreadProbability(int FireSpreadProbability) {
        this.fireSpreadProbability = FireSpreadProbability;
    }

    /**
     * @return the Health
     */
    @Override
    public float getHealth() {
        return this.health;
    }

    /**
     * 
     * @param Health the Health to set
     */
    @Override
    public void setHealth(float Health) {
        if (Health != 0.0F || !this.isInvulnerable()) {
            this.health = Health;
        }
    }

    @Override
    public boolean isOnDeathDone() {
        return this.dead;
    }

    @Override
    public void setOnDeathDone(boolean done) {
        this.dead = done;
    }

    @Override
    public boolean isOnKillDone() {
        return this.kill;
    }

    @Override
    public void setOnKillDone(boolean done) {
        this.kill = done;
    }

    @Override
    public boolean isDeathDragDown() {
        return this.deathDragDown;
    }

    @Override
    public void setDeathDragDown(boolean dragDown) {
        this.deathDragDown = dragDown;
    }

    @Override
    public boolean isPlayingDeathSound() {
        return this.playingDeathSound;
    }

    @Override
    public void setPlayingDeathSound(boolean playing) {
        this.playingDeathSound = playing;
    }

    /**
     * @return the hurtSound
     */
    public String getHurtSound() {
        return this.hurtSound;
    }

    /**
     * 
     * @param hurtSound the hurtSound to set
     */
    public void setHurtSound(String hurtSound) {
        this.hurtSound = hurtSound;
    }

    /**
     * @return the IgnoreMovementForDirection
     */
    @Deprecated
    public boolean isIgnoreMovementForDirection() {
        return false;
    }

    /**
     * @return the inventory
     */
    @Override
    public ItemContainer getInventory() {
        return this.inventory;
    }

    /**
     * 
     * @param inventory the inventory to set
     */
    public void setInventory(ItemContainer inventory) {
        inventory.parent = this;
        this.inventory = inventory;
        this.inventory.setExplored(true);
    }

    public boolean isPrimaryEquipped(String item) {
        return this.leftHandItem == null ? false : this.leftHandItem.getFullType().equals(item) || this.leftHandItem.getType().equals(item);
    }

    /**
     * @return the leftHandItem
     */
    @Override
    public InventoryItem getPrimaryHandItem() {
        return this.leftHandItem;
    }

    /**
     * 
     * @param leftHandItem the leftHandItem to set
     */
    @Override
    public void setPrimaryHandItem(InventoryItem leftHandItem) {
        if (this.leftHandItem != leftHandItem) {
            if (leftHandItem == null && this.getPrimaryHandItem() instanceof AnimalInventoryItem) {
                ((AnimalInventoryItem)this.getPrimaryHandItem()).getAnimal().heldBy = null;
            }

            if (this instanceof IsoPlayer
                && leftHandItem == null
                && !((IsoPlayer)this).getAttachedAnimals().isEmpty()
                && this.getPrimaryHandItem() != null
                && this.getPrimaryHandItem().getType().equalsIgnoreCase("Rope")) {
                ((IsoPlayer)this).removeAllAttachedAnimals();
            }

            if (leftHandItem == this.getSecondaryHandItem()) {
                this.setEquipParent(this.leftHandItem, leftHandItem, false);
            } else {
                this.setEquipParent(this.leftHandItem, leftHandItem);
            }

            if (leftHandItem instanceof HandWeapon handWeapon) {
                this.setUseHandWeapon(handWeapon);
            } else {
                this.setUseHandWeapon(null);
            }

            if (this.leftHandItem instanceof HandWeapon || leftHandItem instanceof HandWeapon) {
                BallisticsController ballisticsController = this.getBallisticsController();
                if (ballisticsController != null) {
                    ballisticsController.clearCacheTargets();
                }
            }

            this.leftHandItem = leftHandItem;
            this.handItemShouldSendToClients = true;
            LuaEventManager.triggerEvent("OnEquipPrimary", this, leftHandItem);
            this.resetEquippedHandsModels();
            this.setVariable("Weapon", WeaponType.getWeaponType(this).getType());
            if (leftHandItem instanceof AnimalInventoryItem animalInventoryItem && this instanceof IsoPlayer) {
                animalInventoryItem.getAnimal().heldBy = (IsoPlayer)this;
            }
        }
    }

    public HandWeapon getAttackingWeapon() {
        return this.getUseHandWeapon();
    }

    protected void setEquipParent(InventoryItem handItem, InventoryItem newHandItem) {
        this.setEquipParent(handItem, newHandItem, true);
    }

    protected void setEquipParent(InventoryItem handItem, InventoryItem newHandItem, boolean register) {
        if (handItem != null) {
            handItem.setEquipParent(null, register);
        }

        if (newHandItem != null) {
            newHandItem.setEquipParent(this, register);
        }
    }

    public void initWornItems(String bodyLocationGroupName) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup(bodyLocationGroupName);
        this.wornItems = new WornItems(bodyLocationGroup);
    }

    @Override
    public WornItems getWornItems() {
        return this.wornItems;
    }

    @Override
    public void setWornItems(WornItems other) {
        this.wornItems = new WornItems(other);
    }

    @Override
    public InventoryItem getWornItem(ItemBodyLocation itemBodyLocation) {
        return this.wornItems.getItem(itemBodyLocation);
    }

    @Override
    public void setWornItem(ItemBodyLocation location, InventoryItem item) {
        this.setWornItem(location, item, true);
    }

    public void setWornItem(ItemBodyLocation location, InventoryItem item, boolean forceDropTooHeavy) {
        InventoryItem itemCur = this.wornItems.getItem(location);
        if (item != itemCur) {
            IsoCell cell = IsoWorld.instance.currentCell;
            if (itemCur != null && cell != null) {
                cell.addToProcessItemsRemove(itemCur);
            }

            this.wornItems.setItem(location, item);
            if (item != null && cell != null) {
                if (item.getContainer() != null) {
                    item.getContainer().parent = this;
                }

                cell.addToProcessItems(item);
            }

            if (forceDropTooHeavy && itemCur != null && this instanceof IsoPlayer && !this.getInventory().hasRoomFor(this, itemCur)) {
                IsoGridSquare sq = this.getCurrentSquare();
                sq = this.getSolidFloorAt(sq.x, sq.y, sq.z);
                if (sq != null) {
                    float dropX = Rand.Next(0.1F, 0.9F);
                    float dropY = Rand.Next(0.1F, 0.9F);
                    float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                    sq.AddWorldInventoryItem(itemCur, dropX, dropY, dropZ);
                    this.getInventory().Remove(itemCur);
                    if (GameServer.server) {
                        GameServer.sendRemoveItemFromContainer(this.getInventory(), itemCur);
                    }
                }
            }

            if (this.isoPlayer != null
                && this.isoPlayer.getHumanVisual().getHairModel().contains("Mohawk")
                && (Objects.equals(location, ItemBodyLocation.HAT) || Objects.equals(location, ItemBodyLocation.FULL_HAT))) {
                this.isoPlayer.getHumanVisual().setHairModel("MohawkFlat");
                this.resetModel();
            }

            this.resetModelNextFrame();
            if (this.clothingWetness != null) {
                this.clothingWetness.changed = true;
            }

            if (this instanceof IsoPlayer) {
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncClothing, this.getX(), this.getY(), this);
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncVisuals, this.getX(), this.getY(), this);
                } else if (GameClient.client && GameClient.connection.isReady()) {
                    INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                }
            }

            this.onWornItemsChanged();
        }
    }

    @Override
    public void removeWornItem(InventoryItem item) {
        this.removeWornItem(item, true);
    }

    @Override
    public void removeWornItem(InventoryItem item, boolean forceDropTooHeavy) {
        this.setWornItem(this.wornItems.getLocation(item), null, forceDropTooHeavy);
    }

    @Override
    public void clearWornItems() {
        if (this.wornItems != null) {
            this.wornItems.clear();
            if (this.clothingWetness != null) {
                this.clothingWetness.changed = true;
            }

            this.onWornItemsChanged();
        }
    }

    @Override
    public BodyLocationGroup getBodyLocationGroup() {
        return this.wornItems == null ? null : this.wornItems.getBodyLocationGroup();
    }

    public void onWornItemsChanged() {
        boolean clothingCanRagdoll = !this.hasWornTag(ItemTag.NO_RAGDOLL);
        if (this.wornClothingCanRagdoll != clothingCanRagdoll) {
            this.wornClothingCanRagdoll = clothingCanRagdoll;
            DebugLog.General
                .debugln(
                    "%s worn items changed. %s.",
                    this.getName(),
                    clothingCanRagdoll ? "Character's clothes can now ragdoll." : "Character's clothing prevent ragdolling."
                );
        }
    }

    public void initAttachedItems(String groupName) {
        AttachedLocationGroup group = AttachedLocations.getGroup(groupName);
        this.attachedItems = new AttachedItems(group);
    }

    @Override
    public AttachedItems getAttachedItems() {
        return this.attachedItems;
    }

    @Override
    public void setAttachedItems(AttachedItems other) {
        this.attachedItems = new AttachedItems(other);
    }

    @Override
    public InventoryItem getAttachedItem(String location) {
        return this.attachedItems.getItem(location);
    }

    @Override
    public void setAttachedItem(String location, InventoryItem item) {
        InventoryItem itemCur = this.attachedItems.getItem(location);
        IsoCell cell = IsoWorld.instance.currentCell;
        if (itemCur != null && cell != null) {
            cell.addToProcessItemsRemove(itemCur);
        }

        this.attachedItems.setItem(location, item);
        if (item != null && cell != null) {
            InventoryContainer invContainer = Type.tryCastTo(item, InventoryContainer.class);
            if (invContainer != null && invContainer.getInventory() != null) {
                invContainer.getInventory().parent = this;
            }

            cell.addToProcessItems(item);
        }

        this.resetEquippedHandsModels();
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (GameClient.client && player != null && player.isLocalPlayer() && !"bowtie".equals(location) && !"head_hat".equals(location)) {
            GameClient.instance.sendAttachedItem(player, location, item);
        }

        if (!GameServer.server && player != null && player.isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
        }
    }

    @Override
    public void removeAttachedItem(InventoryItem item) {
        String location = this.attachedItems.getLocation(item);
        if (location != null) {
            this.setAttachedItem(location, null);
        }
    }

    @Override
    public void clearAttachedItems() {
        if (this.attachedItems != null) {
            this.attachedItems.clear();
        }
    }

    @Override
    public AttachedLocationGroup getAttachedLocationGroup() {
        return this.attachedItems == null ? null : this.attachedItems.getGroup();
    }

    public ClothingWetness getClothingWetness() {
        return this.clothingWetness;
    }

    public ClothingWetnessSync getClothingWetnessSync() {
        return this.clothingWetnessSync;
    }

    /**
     * @return the ClothingItem_Head
     */
    public InventoryItem getClothingItem_Head() {
        return this.getWornItem(ItemBodyLocation.HAT);
    }

    /**
     * 
     * @param item the ClothingItem_Head to set
     */
    @Override
    public void setClothingItem_Head(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.HAT, item);
    }

    /**
     * @return the ClothingItem_Torso
     */
    public InventoryItem getClothingItem_Torso() {
        return this.getWornItem(ItemBodyLocation.TSHIRT);
    }

    @Override
    public void setClothingItem_Torso(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.TSHIRT, item);
    }

    public InventoryItem getClothingItem_Back() {
        return this.getWornItem(ItemBodyLocation.BACK);
    }

    @Override
    public void setClothingItem_Back(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.BACK, item);
    }

    /**
     * @return the ClothingItem_Hands
     */
    public InventoryItem getClothingItem_Hands() {
        return this.getWornItem(ItemBodyLocation.HANDS);
    }

    /**
     * 
     * @param item the ClothingItem_Hands to set
     */
    @Override
    public void setClothingItem_Hands(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.HANDS, item);
    }

    /**
     * @return the ClothingItem_Legs
     */
    public InventoryItem getClothingItem_Legs() {
        return this.getWornItem(ItemBodyLocation.PANTS);
    }

    /**
     * 
     * @param item the ClothingItem_Legs to set
     */
    @Override
    public void setClothingItem_Legs(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.PANTS, item);
    }

    /**
     * @return the ClothingItem_Feet
     */
    public InventoryItem getClothingItem_Feet() {
        return this.getWornItem(ItemBodyLocation.SHOES);
    }

    /**
     * 
     * @param item the ClothingItem_Feet to set
     */
    @Override
    public void setClothingItem_Feet(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.SHOES, item);
    }

    /**
     * @return the NextWander
     */
    public int getNextWander() {
        return this.nextWander;
    }

    /**
     * 
     * @param NextWander the NextWander to set
     */
    public void setNextWander(int NextWander) {
        this.nextWander = NextWander;
    }

    /**
     * @return the OnFire
     */
    @Override
    public boolean isOnFire() {
        return this.onFire;
    }

    /**
     * 
     * @param OnFire the OnFire to set
     */
    public void setOnFire(boolean OnFire) {
        this.onFire = OnFire;
        if (GameServer.server) {
            if (OnFire) {
                IsoFireManager.addCharacterOnFire(this);
            } else {
                IsoFireManager.deleteCharacterOnFire(this);
            }
        }
    }

    @Override
    public void removeFromWorld() {
        if (GameServer.server) {
            IsoFireManager.deleteCharacterOnFire(this);
        }

        super.removeFromWorld();
        this.releaseRagdollController();
        this.releaseBallisticsController();
        this.releaseBallisticsTarget();
        if (this.animationRecorder != null) {
            this.animationRecorder.close();
        }

        if (GameClient.client && !this.isLocal()) {
            this.getNetworkCharacterAI().resetState();
        }
    }

    /**
     * @return the pathIndex
     */
    public int getPathIndex() {
        return this.pathIndex;
    }

    /**
     * 
     * @param pathIndex the pathIndex to set
     */
    public void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }

    /**
     * @return the PathTargetX
     */
    public int getPathTargetX() {
        return PZMath.fastfloor(this.getPathFindBehavior2().getTargetX());
    }

    /**
     * @return the PathTargetY
     */
    public int getPathTargetY() {
        return PZMath.fastfloor(this.getPathFindBehavior2().getTargetY());
    }

    /**
     * @return the PathTargetZ
     */
    public int getPathTargetZ() {
        return PZMath.fastfloor(this.getPathFindBehavior2().getTargetZ());
    }

    /**
     * @return the rightHandItem
     */
    @Override
    public InventoryItem getSecondaryHandItem() {
        return this.rightHandItem;
    }

    /**
     * 
     * @param rightHandItem the rightHandItem to set
     */
    @Override
    public void setSecondaryHandItem(InventoryItem rightHandItem) {
        if (this.rightHandItem != rightHandItem) {
            if (rightHandItem == this.getPrimaryHandItem()) {
                this.setEquipParent(this.rightHandItem, rightHandItem, false);
            } else {
                this.setEquipParent(this.rightHandItem, rightHandItem);
            }

            this.rightHandItem = rightHandItem;
            this.handItemShouldSendToClients = true;
            LuaEventManager.triggerEvent("OnEquipSecondary", this, rightHandItem);
            this.resetEquippedHandsModels();
            this.setVariable("Weapon", WeaponType.getWeaponType(this).getType());
        }
    }

    @Override
    public boolean isHandItem(InventoryItem item) {
        return this.isPrimaryHandItem(item) || this.isSecondaryHandItem(item);
    }

    @Override
    public boolean isPrimaryHandItem(InventoryItem item) {
        return item != null && this.getPrimaryHandItem() == item;
    }

    @Override
    public boolean isSecondaryHandItem(InventoryItem item) {
        return item != null && this.getSecondaryHandItem() == item;
    }

    @Override
    public boolean isItemInBothHands(InventoryItem item) {
        return this.isPrimaryHandItem(item) && this.isSecondaryHandItem(item);
    }

    @Override
    public boolean removeFromHands(InventoryItem item) {
        boolean addToWorld = true;
        if (this.isPrimaryHandItem(item)) {
            this.setPrimaryHandItem(null);
        }

        if (this.isSecondaryHandItem(item)) {
            this.setSecondaryHandItem(null);
        }

        return true;
    }

    /**
     * @return the SpeakColour
     */
    public Color getSpeakColour() {
        return this.speakColour;
    }

    /**
     * 
     * @param SpeakColour the SpeakColour to set
     */
    public void setSpeakColour(Color SpeakColour) {
        this.speakColour = SpeakColour;
    }

    @Override
    public void setSpeakColourInfo(ColorInfo info) {
        this.speakColour = new Color(info.r, info.g, info.b, 1.0F);
    }

    /**
     * @return the slowFactor
     */
    public float getSlowFactor() {
        return this.slowFactor;
    }

    /**
     * 
     * @param slowFactor the slowFactor to set
     */
    public void setSlowFactor(float slowFactor) {
        this.slowFactor = slowFactor;
    }

    /**
     * @return the slowTimer
     */
    public float getSlowTimer() {
        return this.slowTimer;
    }

    /**
     * 
     * @param slowTimer the slowTimer to set
     */
    public void setSlowTimer(float slowTimer) {
        this.slowTimer = slowTimer;
    }

    /**
     * @return the bUseParts
     */
    public boolean isbUseParts() {
        return this.useParts;
    }

    /**
     * 
     * @param useParts the bUseParts to set
     */
    public void setbUseParts(boolean useParts) {
        this.useParts = useParts;
    }

    /**
     * @return the Speaking
     */
    @Override
    public boolean isSpeaking() {
        return this.IsSpeaking();
    }

    /**
     * 
     * @param Speaking the Speaking to set
     */
    public void setSpeaking(boolean Speaking) {
        this.speaking = Speaking;
    }

    /**
     * @return the SpeakTime
     */
    public float getSpeakTime() {
        return this.speakTime;
    }

    /**
     * 
     * @param SpeakTime the SpeakTime to set
     */
    public void setSpeakTime(int SpeakTime) {
        this.speakTime = SpeakTime;
    }

    /**
     * @return the speedMod
     */
    public float getSpeedMod() {
        return this.speedMod;
    }

    /**
     * 
     * @param speedMod the speedMod to set
     */
    public void setSpeedMod(float speedMod) {
        this.speedMod = speedMod;
    }

    /**
     * @return the staggerTimeMod
     */
    public float getStaggerTimeMod() {
        return this.staggerTimeMod;
    }

    /**
     * 
     * @param staggerTimeMod the staggerTimeMod to set
     */
    public void setStaggerTimeMod(float staggerTimeMod) {
        this.staggerTimeMod = staggerTimeMod;
    }

    /**
     * @return the stateMachine
     */
    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    /**
     * @return the Moodles
     */
    @Override
    public Moodles getMoodles() {
        return this.moodles;
    }

    /**
     * @return the stats
     */
    @Override
    public Stats getStats() {
        return this.stats;
    }

    /**
     * @return the UsedItemsOn
     */
    public Stack<String> getUsedItemsOn() {
        return this.usedItemsOn;
    }

    /**
     * @return the useHandWeapon
     */
    public HandWeapon getUseHandWeapon() {
        return this.useHandWeapon;
    }

    /**
     * 
     * @param useHandWeapon the useHandWeapon to set
     */
    public void setUseHandWeapon(HandWeapon useHandWeapon) {
        this.useHandWeapon = useHandWeapon;
    }

    /**
     * @return the legsSprite
     */
    public IsoSprite getLegsSprite() {
        return this.legsSprite;
    }

    /**
     * 
     * @param legsSprite the legsSprite to set
     */
    public void setLegsSprite(IsoSprite legsSprite) {
        this.legsSprite = legsSprite;
    }

    /**
     * @return the attackTargetSquare
     */
    public IsoGridSquare getAttackTargetSquare() {
        return this.attackTargetSquare;
    }

    /**
     * 
     * @param attackTargetSquare the attackTargetSquare to set
     */
    public void setAttackTargetSquare(IsoGridSquare attackTargetSquare) {
        this.attackTargetSquare = attackTargetSquare;
    }

    /**
     * @return the BloodImpactX
     */
    public float getBloodImpactX() {
        return this.bloodImpactX;
    }

    /**
     * 
     * @param BloodImpactX the BloodImpactX to set
     */
    public void setBloodImpactX(float BloodImpactX) {
        this.bloodImpactX = BloodImpactX;
    }

    /**
     * @return the BloodImpactY
     */
    public float getBloodImpactY() {
        return this.bloodImpactY;
    }

    /**
     * 
     * @param BloodImpactY the BloodImpactY to set
     */
    public void setBloodImpactY(float BloodImpactY) {
        this.bloodImpactY = BloodImpactY;
    }

    /**
     * @return the BloodImpactZ
     */
    public float getBloodImpactZ() {
        return this.bloodImpactZ;
    }

    /**
     * 
     * @param BloodImpactZ the BloodImpactZ to set
     */
    public void setBloodImpactZ(float BloodImpactZ) {
        this.bloodImpactZ = BloodImpactZ;
    }

    /**
     * @return the bloodSplat
     */
    public IsoSprite getBloodSplat() {
        return this.bloodSplat;
    }

    /**
     * 
     * @param bloodSplat the bloodSplat to set
     */
    public void setBloodSplat(IsoSprite bloodSplat) {
        this.bloodSplat = bloodSplat;
    }

    /**
     * @return the bOnBed
     */
    @Deprecated
    public boolean isbOnBed() {
        return this.onBed;
    }

    /**
     * 
     * @param onBed the bOnBed to set
     */
    @Deprecated
    public void setbOnBed(boolean onBed) {
        this.onBed = onBed;
    }

    public boolean isOnBed() {
        return this.onBed;
    }

    public void setOnBed(boolean bOnBed) {
        this.onBed = bOnBed;
    }

    /**
     * @return the moveForwardVec
     */
    public Vector2 getMoveForwardVec() {
        return this.moveForwardVec;
    }

    /**
     * 
     * @param moveForwardVec the moveForwardVec to set
     */
    public void setMoveForwardVec(Vector2 moveForwardVec) {
        this.moveForwardVec.set(moveForwardVec);
    }

    /**
     * @return the pathing
     */
    public boolean isPathing() {
        return this.pathing;
    }

    /**
     * 
     * @param pathing the pathing to set
     */
    public void setPathing(boolean pathing) {
        this.pathing = pathing;
    }

    /**
     * @return the LocalEnemyList
     */
    public Stack<IsoGameCharacter> getLocalEnemyList() {
        return this.localEnemyList;
    }

    /**
     * @return the EnemyList
     */
    public Stack<IsoGameCharacter> getEnemyList() {
        return this.enemyList;
    }

    @Override
    public CharacterTraits getCharacterTraits() {
        return this.characterTraits;
    }

    /**
     * @return the maxWeight
     */
    @Override
    public int getMaxWeight() {
        return this.maxWeight;
    }

    /**
     * 
     * @param maxWeight the maxWeight to set
     */
    public void setMaxWeight(int maxWeight) {
        this.maxWeight = maxWeight;
    }

    /**
     * @return the maxWeightBase
     */
    public int getMaxWeightBase() {
        return this.maxWeightBase;
    }

    /**
     * 
     * @param maxWeightBase the maxWeightBase to set
     */
    public void setMaxWeightBase(int maxWeightBase) {
        this.maxWeightBase = maxWeightBase;
    }

    /**
     * @return the SleepingTabletDelta
     */
    public float getSleepingTabletDelta() {
        return this.sleepingTabletDelta;
    }

    /**
     * 
     * @param SleepingTabletDelta the SleepingTabletDelta to set
     */
    public void setSleepingTabletDelta(float SleepingTabletDelta) {
        this.sleepingTabletDelta = SleepingTabletDelta;
    }

    /**
     * @return the BetaEffect
     */
    public float getBetaEffect() {
        return this.betaEffect;
    }

    /**
     * 
     * @param BetaEffect the BetaEffect to set
     */
    public void setBetaEffect(float BetaEffect) {
        this.betaEffect = BetaEffect;
    }

    /**
     * @return the DepressEffect
     */
    public float getDepressEffect() {
        return this.depressEffect;
    }

    /**
     * 
     * @param DepressEffect the DepressEffect to set
     */
    public void setDepressEffect(float DepressEffect) {
        this.depressEffect = DepressEffect;
    }

    /**
     * @return the SleepingTabletEffect
     */
    @Override
    public float getSleepingTabletEffect() {
        return this.sleepingTabletEffect;
    }

    /**
     * 
     * @param SleepingTabletEffect the SleepingTabletEffect to set
     */
    @Override
    public void setSleepingTabletEffect(float SleepingTabletEffect) {
        this.sleepingTabletEffect = SleepingTabletEffect;
    }

    /**
     * @return the BetaDelta
     */
    public float getBetaDelta() {
        return this.betaDelta;
    }

    /**
     * 
     * @param BetaDelta the BetaDelta to set
     */
    public void setBetaDelta(float BetaDelta) {
        this.betaDelta = BetaDelta;
    }

    /**
     * @return the DepressDelta
     */
    public float getDepressDelta() {
        return this.depressDelta;
    }

    /**
     * 
     * @param DepressDelta the DepressDelta to set
     */
    public void setDepressDelta(float DepressDelta) {
        this.depressDelta = DepressDelta;
    }

    /**
     * @return the PainEffect
     */
    public float getPainEffect() {
        return this.painEffect;
    }

    /**
     * 
     * @param PainEffect the PainEffect to set
     */
    public void setPainEffect(float PainEffect) {
        this.painEffect = PainEffect;
    }

    /**
     * @return the PainDelta
     */
    public float getPainDelta() {
        return this.painDelta;
    }

    /**
     * 
     * @param PainDelta the PainDelta to set
     */
    public void setPainDelta(float PainDelta) {
        this.painDelta = PainDelta;
    }

    /**
     * @return the bDoDefer
     */
    public boolean isbDoDefer() {
        return this.doDefer;
    }

    /**
     * 
     * @param doDefer the bDoDefer to set
     */
    public void setbDoDefer(boolean doDefer) {
        this.doDefer = doDefer;
    }

    /**
     * @return the LastHeardSound
     */
    public IsoGameCharacter.Location getLastHeardSound() {
        return this.lastHeardSound;
    }

    public void setLastHeardSound(int x, int y, int z) {
        this.lastHeardSound.x = x;
        this.lastHeardSound.y = y;
        this.lastHeardSound.z = z;
    }

    /**
     * @return the bClimbing
     */
    public boolean isClimbing() {
        return this.climbing;
    }

    /**
     * 
     * @param climbing the bClimbing to set
     */
    public void setbClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    /**
     * @return the lastCollidedW
     */
    public boolean isLastCollidedW() {
        return this.lastCollidedW;
    }

    /**
     * 
     * @param lastCollidedW the lastCollidedW to set
     */
    public void setLastCollidedW(boolean lastCollidedW) {
        this.lastCollidedW = lastCollidedW;
    }

    /**
     * @return the lastCollidedN
     */
    public boolean isLastCollidedN() {
        return this.lastCollidedN;
    }

    /**
     * 
     * @param lastCollidedN the lastCollidedN to set
     */
    public void setLastCollidedN(boolean lastCollidedN) {
        this.lastCollidedN = lastCollidedN;
    }

    /**
     * @return the fallTime
     */
    public float getFallTime() {
        return this.fallTime;
    }

    public FallSeverity getFallSpeedSeverity() {
        return FallingConstants.getFallSeverity(this.lastFallSpeed);
    }

    /**
     * 
     * @param fallTime the fallTime to set
     */
    public void setFallTime(float fallTime) {
        this.fallTime = fallTime;
    }

    /**
     * @return the lastFallSpeed
     */
    public float getLastFallSpeed() {
        return this.lastFallSpeed;
    }

    /**
     * 
     * @param lastFallSpeed the lastFallSpeed to set
     */
    public void setLastFallSpeed(float lastFallSpeed) {
        this.lastFallSpeed = lastFallSpeed;
    }

    /**
     * @return the bFalling
     */
    public boolean isbFalling() {
        return this.falling && !this.ragdollFall;
    }

    /**
     * 
     * @param falling the bFalling to set
     */
    public void setbFalling(boolean falling) {
        this.falling = falling;
    }

    @Override
    public IsoBuilding getCurrentBuilding() {
        if (this.current == null) {
            return null;
        } else {
            return this.current.getRoom() == null ? null : this.current.getRoom().building;
        }
    }

    public BuildingDef getCurrentBuildingDef() {
        if (this.current == null) {
            return null;
        } else if (this.current.getRoom() == null) {
            return null;
        } else {
            return this.current.getRoom().building != null ? this.current.getRoom().building.def : null;
        }
    }

    public RoomDef getCurrentRoomDef() {
        if (this.current == null) {
            return null;
        } else {
            return this.current.getRoom() != null ? this.current.getRoom().def : null;
        }
    }

    public float getTorchStrength() {
        return 0.0F;
    }

    @Override
    public AnimEventBroadcaster getAnimEventBroadcaster() {
        return this.animEventBroadcaster;
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        if (event.eventName != null) {
            this.animEvent(this, sender, track, event);
            if (Core.debug && DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.getValue()) {
                dbgOnGlobalAnimEvent(this, sender, track, event);
            }

            int layerIdx = AnimLayer.getDepth(sender);
            String stateName = AnimLayer.getCurrentStateName(sender);
            if (DebugLog.isLogEnabled(DebugType.ActionSystemEvents, LogSeverity.Trace)) {
                DebugType.ActionSystemEvents.trace("%s.animEvent: %s(%s) time=%f", stateName, event.eventName, event.parameterValue, event.timePc);
            }

            this.actionContext.reportEvent(stateName, event.eventName);
            this.stateMachine.stateAnimEvent(layerIdx, sender, track, event);
        }
    }

    private static void dbgOnGlobalAnimEvent(IsoGameCharacter in_owner, AnimLayer in_layer, AnimationTrack in_track, AnimEvent in_event) {
        if (Core.debug) {
            SwipeStatePlayer.dbgOnGlobalAnimEvent(in_owner, in_layer, in_track, in_event);
        }
    }

    private void OnAnimEvent_SetVariable(IsoGameCharacter owner, AnimationVariableReference animReference, String variableValue) {
        DebugLog.Animation.trace("SetVariable(%s, %s)", animReference, variableValue);
        animReference.setVariable(owner, variableValue);
    }

    private void OnAnimEvent_ClearVariable(IsoGameCharacter owner, String variableName) {
        AnimationVariableReference animReference = AnimationVariableReference.fromRawVariableName(variableName);
        animReference.clearVariable(owner);
    }

    private void OnAnimEvent_PlaySound(IsoGameCharacter owner, String file) {
        owner.getEmitter().playSoundImpl(file, this);
    }

    private void OnAnimEvent_PlaySoundNoBlend(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (!layer.isBlendingIn() && !layer.isBlendingOut()) {
            owner.getEmitter().playSoundImpl(event.parameterValue, this);
        }
    }

    private void OnAnimEvent_Footstep(IsoGameCharacter owner, String type) {
        owner.DoFootstepSound(type);
    }

    private void OnAnimEvent_DamageWhileInTrees(IsoGameCharacter owner) {
        owner.damageWhileInTrees();
    }

    private void OnAnimEvent_TurnAround(IsoGameCharacter owner, boolean in_instant) {
        float newDirectionX = -owner.getForwardDirectionX();
        float newDirectionY = -owner.getForwardDirectionY();
        if (in_instant) {
            owner.setTargetAndCurrentDirection(newDirectionX, newDirectionY);
        } else {
            owner.setForwardDirection(newDirectionX, newDirectionY);
        }
    }

    private void OnAnimEvent_TurnAroundFlipSkeleton(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, String boneName) {
        float newDirectionX = -owner.getForwardDirectionX();
        float newDirectionY = -owner.getForwardDirectionY();
        owner.setTargetAndCurrentDirection(newDirectionX, newDirectionY);
        AnimationPlayer animationPlayer = owner.getAnimationPlayer();
        if (animationPlayer != null) {
            int boneIndex = SkeletonBone.getBoneOrdinal(boneName);
            if (boneIndex == SkeletonBone.None.ordinal()) {
                DebugType.Animation.warn("Bone not found: %s", boneName);
            } else {
                Quaternion rotation = HelperFunctions.allocQuaternion();
                HelperFunctions.setFromAxisAngle(0.0F, 1.0F, 0.0F, (float) Math.PI, rotation);
                track.setBonePoseAdjustment(
                    boneIndex, new org.lwjgl.util.vector.Vector3f(0.0F, 0.0F, 0.0F), rotation, new org.lwjgl.util.vector.Vector3f(1.0F, 1.0F, 1.0F)
                );
                HelperFunctions.setFromAxisAngle(0.0F, 1.0F, 0.0F, (float) Math.PI, rotation);
                animationPlayer.transformRootChildBones(boneName, rotation);
                HelperFunctions.releaseQuaternion(rotation);
            }
        }
    }

    private void OnAnimEvent_SetSharedGrappleType(IsoGameCharacter owner, String in_sharedGrappleType) {
        owner.setSharedGrappleType(in_sharedGrappleType);
    }

    public void onRagdollSimulationStarted() {
        float collisionAvoidanceRadius = 0.5F;
        this.slideAwayFromWalls(0.5F);
    }

    private void damageWhileInTrees() {
        if (!this.isZombie() && !"Tutorial".equals(Core.gameMode)) {
            int rand = 50;
            int part = Rand.Next(0, BodyPartType.ToIndex(BodyPartType.MAX));
            if (this.isRunning()) {
                rand = 30;
            }

            if (this.characterTraits.get(CharacterTrait.OUTDOORSMAN)) {
                rand += 50;
            }

            rand += (int)this.getBodyPartClothingDefense(part, false, false);
            if (Rand.NextBool(rand)) {
                this.addHole(BloodBodyPartType.FromIndex(part));
                int var7 = 6;
                if (this.characterTraits.get(CharacterTrait.THICK_SKINNED)) {
                    var7 += 7;
                }

                if (this.characterTraits.get(CharacterTrait.THIN_SKINNED)) {
                    var7 -= 3;
                }

                if (Rand.NextBool((int)var7) && (int)this.getBodyPartClothingDefense(part, false, false) < 100) {
                    BodyPart bodyPart = this.getBodyDamage().getBodyParts().get(part);
                    bodyPart.setScratched(true, true);
                    if (this instanceof IsoPlayer player) {
                        player.playerVoiceSound("PainFromScratch");
                    }
                }
            }
        }
    }

    @Override
    public float getHammerSoundMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 2) {
            return 0.8F;
        } else if (level == 3) {
            return 0.6F;
        } else if (level == 4) {
            return 0.4F;
        } else {
            return level >= 5 ? 0.4F : 1.0F;
        }
    }

    @Override
    public float getWeldingSoundMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.MetalWelding);
        if (level == 2) {
            return 0.8F;
        } else if (level == 3) {
            return 0.6F;
        } else if (level == 4) {
            return 0.4F;
        } else {
            return level >= 5 ? 0.4F : 1.0F;
        }
    }

    public float getBarricadeTimeMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 1) {
            return 0.8F;
        } else if (level == 2) {
            return 0.7F;
        } else if (level == 3) {
            return 0.62F;
        } else if (level == 4) {
            return 0.56F;
        } else if (level == 5) {
            return 0.5F;
        } else if (level == 6) {
            return 0.42F;
        } else if (level == 7) {
            return 0.36F;
        } else if (level == 8) {
            return 0.3F;
        } else if (level == 9) {
            return 0.26F;
        } else {
            return level == 10 ? 0.2F : 0.7F;
        }
    }

    public float getMetalBarricadeStrengthMod() {
        switch (this.getPerkLevel(PerkFactory.Perks.MetalWelding)) {
            case 2:
                return 1.1F;
            case 3:
                return 1.14F;
            case 4:
                return 1.18F;
            case 5:
                return 1.22F;
            case 6:
                return 1.26F;
            case 7:
                return 1.3F;
            case 8:
                return 1.34F;
            case 9:
                return 1.4F;
            case 10:
                return 1.5F;
            default:
                int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
                if (level == 2) {
                    return 1.1F;
                } else if (level == 3) {
                    return 1.14F;
                } else if (level == 4) {
                    return 1.18F;
                } else if (level == 5) {
                    return 1.22F;
                } else if (level == 6) {
                    return 1.26F;
                } else if (level == 7) {
                    return 1.3F;
                } else if (level == 8) {
                    return 1.34F;
                } else if (level == 9) {
                    return 1.4F;
                } else {
                    return level == 10 ? 1.5F : 1.0F;
                }
        }
    }

    public float getBarricadeStrengthMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 2) {
            return 1.1F;
        } else if (level == 3) {
            return 1.14F;
        } else if (level == 4) {
            return 1.18F;
        } else if (level == 5) {
            return 1.22F;
        } else if (level == 6) {
            return 1.26F;
        } else if (level == 7) {
            return 1.3F;
        } else if (level == 8) {
            return 1.34F;
        } else if (level == 9) {
            return 1.4F;
        } else {
            return level == 10 ? 1.5F : 1.0F;
        }
    }

    public float getSneakSpotMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Sneak);
        float result = 0.95F;
        if (level == 1) {
            result = 0.9F;
        }

        if (level == 2) {
            result = 0.8F;
        }

        if (level == 3) {
            result = 0.75F;
        }

        if (level == 4) {
            result = 0.7F;
        }

        if (level == 5) {
            result = 0.65F;
        }

        if (level == 6) {
            result = 0.6F;
        }

        if (level == 7) {
            result = 0.55F;
        }

        if (level == 8) {
            result = 0.5F;
        }

        if (level == 9) {
            result = 0.45F;
        }

        if (level == 10) {
            result = 0.4F;
        }

        return result * 1.2F;
    }

    public float getNimbleMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Nimble);
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
            return 1.38F;
        } else if (level == 9) {
            return 1.42F;
        } else {
            return level == 10 ? 1.5F : 1.0F;
        }
    }

    @Override
    public float getFatigueMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Fitness);
        if (level == 1) {
            return 0.95F;
        } else if (level == 2) {
            return 0.92F;
        } else if (level == 3) {
            return 0.89F;
        } else if (level == 4) {
            return 0.87F;
        } else if (level == 5) {
            return 0.85F;
        } else if (level == 6) {
            return 0.83F;
        } else if (level == 7) {
            return 0.81F;
        } else if (level == 8) {
            return 0.79F;
        } else if (level == 9) {
            return 0.77F;
        } else {
            return level == 10 ? 0.75F : 1.0F;
        }
    }

    public float getLightfootMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Lightfoot);
        if (level == 1) {
            return 0.9F;
        } else if (level == 2) {
            return 0.79F;
        } else if (level == 3) {
            return 0.71F;
        } else if (level == 4) {
            return 0.65F;
        } else if (level == 5) {
            return 0.59F;
        } else if (level == 6) {
            return 0.52F;
        } else if (level == 7) {
            return 0.45F;
        } else if (level == 8) {
            return 0.37F;
        } else if (level == 9) {
            return 0.3F;
        } else {
            return level == 10 ? 0.2F : 0.99F;
        }
    }

    public float getPacingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Fitness);
        if (level == 1) {
            return 0.8F;
        } else if (level == 2) {
            return 0.75F;
        } else if (level == 3) {
            return 0.7F;
        } else if (level == 4) {
            return 0.65F;
        } else if (level == 5) {
            return 0.6F;
        } else if (level == 6) {
            return 0.57F;
        } else if (level == 7) {
            return 0.53F;
        } else if (level == 8) {
            return 0.49F;
        } else if (level == 9) {
            return 0.46F;
        } else {
            return level == 10 ? 0.43F : 0.9F;
        }
    }

    public float getHyperthermiaMod() {
        float Delta = 1.0F;
        if (this.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA) > 1 && this.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA) == 4) {
            Delta = 2.0F;
        }

        return Delta;
    }

    public float getHittingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Strength);
        if (level == 1) {
            return 0.8F;
        } else if (level == 2) {
            return 0.85F;
        } else if (level == 3) {
            return 0.9F;
        } else if (level == 4) {
            return 0.95F;
        } else if (level == 5) {
            return 1.0F;
        } else if (level == 6) {
            return 1.05F;
        } else if (level == 7) {
            return 1.1F;
        } else if (level == 8) {
            return 1.15F;
        } else if (level == 9) {
            return 1.2F;
        } else {
            return level == 10 ? 1.25F : 0.75F;
        }
    }

    public float getShovingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Strength);
        if (level == 1) {
            return 0.8F;
        } else if (level == 2) {
            return 0.85F;
        } else if (level == 3) {
            return 0.9F;
        } else if (level == 4) {
            return 0.95F;
        } else if (level == 5) {
            return 1.0F;
        } else if (level == 6) {
            return 1.05F;
        } else if (level == 7) {
            return 1.1F;
        } else if (level == 8) {
            return 1.15F;
        } else if (level == 9) {
            return 1.2F;
        } else {
            return level == 10 ? 1.25F : 0.75F;
        }
    }

    public float getRecoveryMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Fitness);
        float mod = 0.0F;
        if (level == 0) {
            mod = 0.7F;
        }

        if (level == 1) {
            mod = 0.8F;
        }

        if (level == 2) {
            mod = 0.9F;
        }

        if (level == 3) {
            mod = 1.0F;
        }

        if (level == 4) {
            mod = 1.1F;
        }

        if (level == 5) {
            mod = 1.2F;
        }

        if (level == 6) {
            mod = 1.3F;
        }

        if (level == 7) {
            mod = 1.4F;
        }

        if (level == 8) {
            mod = 1.5F;
        }

        if (level == 9) {
            mod = 1.55F;
        }

        if (level == 10) {
            mod = 1.6F;
        }

        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            mod *= 0.4F;
        }

        if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            mod *= 0.7F;
        }

        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            mod *= 0.7F;
        }

        if (this.characterTraits.get(CharacterTrait.EMACIATED)) {
            mod *= 0.3F;
        }

        if (this instanceof IsoPlayer) {
            if (((IsoPlayer)this).getNutrition().getLipids() < -1500.0F) {
                mod *= 0.2F;
            } else if (((IsoPlayer)this).getNutrition().getLipids() < -1000.0F) {
                mod *= 0.5F;
            }

            if (((IsoPlayer)this).getNutrition().getProteins() < -1500.0F) {
                mod *= 0.2F;
            } else if (((IsoPlayer)this).getNutrition().getProteins() < -1000.0F) {
                mod *= 0.5F;
            }
        }

        return mod;
    }

    public float getWeightMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Strength);
        if (level == 1) {
            return 0.9F;
        } else if (level == 2) {
            return 1.07F;
        } else if (level == 3) {
            return 1.24F;
        } else if (level == 4) {
            return 1.41F;
        } else if (level == 5) {
            return 1.58F;
        } else if (level == 6) {
            return 1.75F;
        } else if (level == 7) {
            return 1.92F;
        } else if (level == 8) {
            return 2.09F;
        } else if (level == 9) {
            return 2.26F;
        } else {
            return level == 10 ? 2.5F : 0.8F;
        }
    }

    public int getHitChancesMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Aiming);
        if (level == 1) {
            return 1;
        } else if (level == 2) {
            return 1;
        } else if (level == 3) {
            return 2;
        } else if (level == 4) {
            return 2;
        } else if (level == 5) {
            return 3;
        } else if (level == 6) {
            return 3;
        } else if (level == 7) {
            return 4;
        } else if (level == 8) {
            return 4;
        } else if (level == 9) {
            return 5;
        } else {
            return level == 10 ? 5 : 1;
        }
    }

    public float getSprintMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Sprinting);
        if (level == 1) {
            return 1.1F;
        } else if (level == 2) {
            return 1.15F;
        } else if (level == 3) {
            return 1.2F;
        } else if (level == 4) {
            return 1.25F;
        } else if (level == 5) {
            return 1.3F;
        } else if (level == 6) {
            return 1.35F;
        } else if (level == 7) {
            return 1.4F;
        } else if (level == 8) {
            return 1.45F;
        } else if (level == 9) {
            return 1.5F;
        } else {
            return level == 10 ? 1.6F : 0.9F;
        }
    }

    /**
     * Return the current lvl of a perk (skill)
     */
    @Override
    public int getPerkLevel(PerkFactory.Perk perks) {
        IsoGameCharacter.PerkInfo info = this.getPerkInfo(perks);
        return info != null ? info.level : 0;
    }

    @Override
    public void setPerkLevelDebug(PerkFactory.Perk perks, int level) {
        IsoGameCharacter.PerkInfo info = this.getPerkInfo(perks);
        if (info != null) {
            info.level = level;
        } else {
            info = new IsoGameCharacter.PerkInfo();
            info.perk = perks;
            info.level = level;
            this.perkList.add(info);
        }

        if (GameClient.client && this instanceof IsoPlayer) {
            GameClient.sendPerks((IsoPlayer)this);
        }
    }

    @Override
    public void LoseLevel(PerkFactory.Perk perk) {
        IsoGameCharacter.PerkInfo info = this.getPerkInfo(perk);
        if (info != null) {
            info.level--;
            if (info.level < 0) {
                info.level = 0;
            }

            LuaEventManager.triggerEvent("LevelPerk", this, perk, info.level, false);
            if (perk == PerkFactory.Perks.Sneak && GameClient.client && this instanceof IsoPlayer) {
                GameClient.sendPerks((IsoPlayer)this);
            }
        } else {
            LuaEventManager.triggerEvent("LevelPerk", this, perk, 0, false);
        }
    }

    /**
     * Level up a perk (max lvl 5)
     * 
     * @param perk the perk to lvl up
     * @param removePick did we remove a skill pts ? (for example passiv skill automatically lvl up, without consuming
     *                    skill pts)
     */
    @Override
    public void LevelPerk(PerkFactory.Perk perk, boolean removePick) {
        Objects.requireNonNull(perk, "perk is null");
        if (perk == PerkFactory.Perks.MAX) {
            throw new IllegalArgumentException("perk == Perks.MAX");
        } else {
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            IsoGameCharacter.PerkInfo info = this.getPerkInfo(perk);
            if (info != null) {
                info.level++;
                if (player != null && !"Tutorial".equals(Core.gameMode) && this.getHoursSurvived() > 0.016666666666666666) {
                    HaloTextHelper.addTextWithArrow(player, "+1 " + perk.getName(), "[br/]", true, HaloTextHelper.getGoodColor());
                }

                if (info.level > 10) {
                    info.level = 10;
                }

                LuaEventManager.triggerEventGarbage("LevelPerk", this, perk, info.level, true);
                if (GameClient.client && player != null) {
                    GameClient.sendPerks(player);
                }
            } else {
                info = new IsoGameCharacter.PerkInfo();
                info.perk = perk;
                info.level = 1;
                this.perkList.add(info);
                if (player != null && !"Tutorial".equals(Core.gameMode) && this.getHoursSurvived() > 0.016666666666666666) {
                    HaloTextHelper.addTextWithArrow(player, "+1 " + perk.getName(), "[br/]", true, HaloTextHelper.getGoodColor());
                }

                LuaEventManager.triggerEvent("LevelPerk", this, perk, info.level, true);
            }
        }
    }

    /**
     * Level up a perk (max lvl 5)
     * 
     * @param perk the perk to lvl up (a skill points is removed)
     */
    @Override
    public void LevelPerk(PerkFactory.Perk perk) {
        this.LevelPerk(perk, true);
    }

    public void level0(PerkFactory.Perk perk) {
        IsoGameCharacter.PerkInfo info = this.getPerkInfo(perk);
        if (info != null) {
            info.level = 0;
        }
    }

    public IsoGameCharacter.Location getLastKnownLocationOf(String character) {
        return this.lastKnownLocation.containsKey(character) ? this.lastKnownLocation.get(character) : null;
    }

    /**
     * Used when you read a book, magazine or newspaper
     * 
     * @param literature the book to read
     */
    @Override
    public void ReadLiterature(Literature literature) {
        this.stats.add(CharacterStat.STRESS, literature.getStressChange());
        this.getBodyDamage().JustReadSomething(literature);
        if (literature.getLearnedRecipes() != null) {
            for (int i = 0; i < literature.getLearnedRecipes().size(); i++) {
                if (!this.getKnownRecipes().contains(literature.getLearnedRecipes().get(i))) {
                    this.learnRecipe(literature.getLearnedRecipes().get(i));
                }
            }
        }

        if (literature.hasTag(ItemTag.CONSUME_ON_READ)) {
            literature.Use();
        }
    }

    public void OnDeath() {
        LuaEventManager.triggerEvent("OnCharacterDeath", this);
    }

    public void splatBloodFloorBig() {
        if (this.getCurrentSquare() != null && this.getCurrentSquare().getChunk() != null) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
        }
    }

    public void splatBloodFloor() {
        if (this.getCurrentSquare() != null) {
            if (this.getCurrentSquare().getChunk() != null) {
                if (this.isDead() && Rand.Next(10) == 0) {
                    this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
                }

                if (Rand.Next(14) == 0) {
                    this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(8));
                }

                if (Rand.Next(50) == 0) {
                    this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
                }
            }
        }
    }

    public int getThreatLevel() {
        int total = this.localRelevantEnemyList.size();
        total += this.veryCloseEnemyList.size() * 10;
        if (total > 20) {
            return 3;
        } else if (total > 10) {
            return 2;
        } else {
            return total > 0 ? 1 : 0;
        }
    }

    public boolean isDead() {
        return this.health <= 0.0F || this.getBodyDamage() != null && this.getBodyDamage().getHealth() <= 0.0F;
    }

    public boolean isAlive() {
        return !this.isDead();
    }

    public boolean isEditingRagdoll() {
        return this.isEditingRagdoll;
    }

    public void setEditingRagdoll(boolean value) {
        this.isEditingRagdoll = value;
    }

    public boolean isRagdoll() {
        return this.hasAnimationPlayer() && this.getAnimationPlayer().isRagdolling();
    }

    public void setRagdollFall(boolean value) {
        this.ragdollFall = value;
    }

    public boolean isRagdollFall() {
        return this.ragdollFall;
    }

    public boolean isVehicleCollision() {
        return this.vehicleCollision;
    }

    public void setVehicleCollision(boolean value) {
        this.vehicleCollision = value;
    }

    public boolean useRagdollVehicleCollision() {
        return this.canRagdoll() && this.vehicleCollision;
    }

    public boolean isUpright() {
        if (this.canRagdoll()) {
            return this.getRagdollController() == null ? false : this.getRagdollController().isUpright();
        } else {
            return false;
        }
    }

    public boolean isOnBack() {
        return this.getRagdollController() == null ? false : this.getRagdollController().isOnBack();
    }

    public boolean usePhysicHitReaction() {
        return this.usePhysicHitReaction;
    }

    public void setUsePhysicHitReaction(boolean usePhysicHitReaction) {
        this.usePhysicHitReaction = usePhysicHitReaction;
    }

    public boolean isRagdollSimulationActive() {
        if (!this.hasAnimationPlayer()) {
            return false;
        } else {
            return this.canRagdoll() && this.getRagdollController() != null ? this.getRagdollController().isSimulationActive() : false;
        }
    }

    public void Seen(Stack<IsoMovingObject> SeenList) {
        synchronized (this.localList) {
            this.localList.clear();
            this.localList.addAll(SeenList);
        }
    }

    public boolean CanSee(IsoMovingObject obj) {
        return this.CanSee((IsoObject)obj);
    }

    public boolean CanSee(IsoObject obj) {
        return LosUtil.lineClear(
                this.getCell(),
                PZMath.fastfloor(this.getX()),
                PZMath.fastfloor(this.getY()),
                PZMath.fastfloor(this.getZ()),
                PZMath.fastfloor(obj.getX()),
                PZMath.fastfloor(obj.getY()),
                PZMath.fastfloor(obj.getZ()),
                false
            )
            != LosUtil.TestResults.Blocked;
    }

    public IsoGridSquare getLowDangerInVicinity(int attempts, int range) {
        float highscore = -1000000.0F;
        IsoGridSquare chosen = null;

        for (int n = 0; n < attempts; n++) {
            float score = 0.0F;
            int randx = Rand.Next(-range, range);
            int randy = Rand.Next(-range, range);
            IsoGridSquare sq = this.getCell()
                .getGridSquare(PZMath.fastfloor(this.getX()) + randx, PZMath.fastfloor(this.getY()) + randy, PZMath.fastfloor(this.getZ()));
            if (sq != null && sq.isFree(true)) {
                float total = sq.getMovingObjects().size();
                if (sq.getE() != null) {
                    total += sq.getE().getMovingObjects().size();
                }

                if (sq.getS() != null) {
                    total += sq.getS().getMovingObjects().size();
                }

                if (sq.getW() != null) {
                    total += sq.getW().getMovingObjects().size();
                }

                if (sq.getN() != null) {
                    total += sq.getN().getMovingObjects().size();
                }

                score -= total * 1000.0F;
                if (score > highscore) {
                    highscore = score;
                    chosen = sq;
                }
            }
        }

        return chosen;
    }

    @Override
    public boolean hasEquipped(String itemType) {
        if (itemType.contains(".")) {
            itemType = itemType.split("\\.")[1];
        }

        return this.leftHandItem != null && this.leftHandItem.getType().equals(itemType)
            ? true
            : this.rightHandItem != null && this.rightHandItem.getType().equals(itemType);
    }

    @Override
    public boolean hasEquippedTag(ItemTag itemTag) {
        return this.leftHandItem != null && this.leftHandItem.hasTag(itemTag) ? true : this.rightHandItem != null && this.rightHandItem.hasTag(itemTag);
    }

    @Override
    public boolean hasWornTag(ItemTag itemTag) {
        for (int i = 0; i < this.getWornItems().size(); i++) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (item.hasTag(itemTag)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setForwardIsoDirection(IsoDirections directions) {
        this.dir = directions;
        this.setForwardDirectionFromIsoDirection();
    }

    public void setForwardDirectionFromIsoDirection() {
        this.getVectorFromDirection(tempVector2_2);
        this.setForwardDirection(tempVector2_2);
    }

    public void setForwardDirectionFromAnimAngle() {
        this.setDirectionAngle(this.getAnimAngle());
    }

    public void Callout(boolean doAnim) {
        if (this.isCanShout()) {
            this.Callout();
            if (doAnim) {
                this.playEmote("shout");
            }
        }
    }

    @Override
    public void Callout() {
        String text = "";
        InventoryItem item = this.getPrimaryHandItem();
        boolean bMegaphone = item != null && item.hasTag(ItemTag.MEGAPHONE);
        int radius = bMegaphone ? 90 : 30;
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (Core.getInstance().getGameMode().equals("Tutorial")) {
            text = Translator.getText("IGUI_PlayerText_CalloutTutorial");
            if (player != null) {
                player.transmitPlayerVoiceSound("ShoutHey");
            }
        } else if (this.isSneaking()) {
            radius = bMegaphone ? 18 : 6;
            switch (Rand.Next(3)) {
                case 0:
                    text = Translator.getText("IGUI_PlayerText_Callout1Sneak");
                    if (player != null) {
                        player.transmitPlayerVoiceSound(bMegaphone ? "WhisperMegaphonePsst" : "WhisperPsst");
                    }
                    break;
                case 1:
                    text = Translator.getText("IGUI_PlayerText_Callout2Sneak");
                    if (player != null) {
                        player.transmitPlayerVoiceSound(bMegaphone ? "WhisperMegaphonePsst" : "WhisperPsst");
                    }
                    break;
                case 2:
                    text = Translator.getText("IGUI_PlayerText_Callout3Sneak");
                    if (player != null) {
                        player.transmitPlayerVoiceSound(bMegaphone ? "WhisperMegaphoneHey" : "WhisperHey");
                    }
            }
        } else {
            InventoryItem shoutItem = null;
            if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
                shoutItem = this.getPrimaryHandItem();
            } else if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null) {
                shoutItem = this.getSecondaryHandItem();
            } else if (this.getWornItems() != null) {
                for (int i = 0; i < this.getWornItems().size(); i++) {
                    if (this.getWornItems().get(i).getItem() != null && this.getWornItems().get(i).getItem().getShoutType() != null) {
                        shoutItem = this.getWornItems().get(i).getItem();
                        break;
                    }
                }
            }

            if (shoutItem != null) {
                this.playSound(shoutItem.getShoutType());
                radius = (int)(radius * shoutItem.getShoutMultiplier());
            } else {
                text = switch (Rand.Next(3)) {
                    case 0 -> Translator.getText("IGUI_PlayerText_Callout1New");
                    case 1 -> Translator.getText("IGUI_PlayerText_Callout2New");
                    case 2 -> Translator.getText("IGUI_PlayerText_Callout3New");
                    default -> text;
                };
                if (player != null) {
                    player.transmitPlayerVoiceSound(bMegaphone ? "ShoutMegaphoneHey" : "ShoutHey");
                }
            }
        }

        WorldSoundManager.instance
            .addSound(
                this,
                PZMath.fastfloor(this.getX()),
                PZMath.fastfloor(this.getY()),
                PZMath.fastfloor(this.getZ()),
                radius,
                radius,
                false,
                0.0F,
                1.0F,
                false,
                true,
                false,
                false,
                true
            );
        this.SayShout(text);
        this.callOut = true;
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.setForwardDirectionFromIsoDirection();
        if (input.get() == 1) {
            this.descriptor = new SurvivorDesc(true);
            this.descriptor.load(input, WorldVersion, this);
            this.female = this.descriptor.isFemale();
        }

        this.getVisual().load(input, WorldVersion);
        ArrayList<InventoryItem> savedItems = this.inventory.load(input, WorldVersion);
        this.savedInventoryItems.clear();
        this.savedInventoryItems.addAll(savedItems);
        this.asleep = input.get() == 1;
        this.forceWakeUpTime = input.getFloat();
        if (!this.isZombie()) {
            this.stats.load(input, WorldVersion);
            this.getBodyDamage().load(input, WorldVersion);
            this.xp.load(input, WorldVersion);
            ArrayList<InventoryItem> items = this.inventory.includingObsoleteItems;
            int n = input.getInt();
            if (n >= 0 && n < items.size()) {
                this.leftHandItem = items.get(n);
            }

            n = input.getInt();
            if (n >= 0 && n < items.size()) {
                this.rightHandItem = items.get(n);
            }

            this.setEquipParent(null, this.leftHandItem);
            if (this.rightHandItem == this.leftHandItem) {
                this.setEquipParent(null, this.rightHandItem, false);
            } else {
                this.setEquipParent(null, this.rightHandItem);
            }
        }

        boolean onFire = input.get() == 1;
        if (onFire) {
            this.SetOnFire();
        }

        this.depressEffect = input.getFloat();
        this.depressFirstTakeTime = input.getFloat();
        this.betaEffect = input.getFloat();
        this.betaDelta = input.getFloat();
        this.painEffect = input.getFloat();
        this.painDelta = input.getFloat();
        this.sleepingTabletEffect = input.getFloat();
        this.sleepingTabletDelta = input.getFloat();
        int numBooks = input.getInt();

        for (int i = 0; i < numBooks; i++) {
            IsoGameCharacter.ReadBook read = new IsoGameCharacter.ReadBook();
            read.fullType = GameWindow.ReadString(input);
            read.alreadyReadPages = input.getInt();
            this.readBooks.add(read);
        }

        this.reduceInfectionPower = input.getFloat();
        int numrecipes = input.getInt();

        for (int i = 0; i < numrecipes; i++) {
            this.knownRecipes.add(GameWindow.ReadString(input));
        }

        this.lastHourSleeped = input.getInt();
        this.timeSinceLastSmoke = input.getFloat();
        this.beardGrowTiming = input.getFloat();
        this.hairGrowTiming = input.getFloat();
        this.setUnlimitedCarry(input.get() == 1);
        this.setBuildCheat(input.get() == 1);
        this.setHealthCheat(input.get() == 1);
        this.setMechanicsCheat(input.get() == 1);
        this.setMovablesCheat(input.get() == 1);
        this.setFarmingCheat(input.get() == 1);
        if (WorldVersion >= 202) {
            this.setFishingCheat(input.get() == 1);
        }

        if (WorldVersion >= 217) {
            this.setCanUseBrushTool(input.get() == 1);
            this.setFastMoveCheat(input.get() == 1);
        }

        this.setTimedActionInstantCheat(input.get() == 1);
        this.setUnlimitedEndurance(input.get() == 1);
        if (WorldVersion >= 230) {
            this.setUnlimitedAmmo(input.get() == 1);
            this.setKnowAllRecipes(input.get() == 1);
        }

        this.setSneaking(input.get() == 1);
        this.setDeathDragDown(input.get() == 1);
        int size = input.getInt();

        for (int i = 0; i < size; i++) {
            String title = GameWindow.ReadString(input);
            int day = input.getInt();
            this.addReadLiterature(title, day);
        }

        if (WorldVersion >= 222) {
            this.readPrintMedia.clear();
            size = input.getInt();

            for (int i = 0; i < size; i++) {
                String media_id = GameWindow.ReadString(input);
                this.readPrintMedia.add(media_id);
            }
        }

        this.lastAnimalPet = input.getLong();
        if (WorldVersion >= 231) {
            this.getCheats().load(input, WorldVersion, IS_DEBUG_SAVE);
        }
    }

    @Override
    public String getDescription(String in_separatorStr) {
        String out_result = this.getClass().getSimpleName() + " [" + in_separatorStr;
        out_result = out_result + "isDead=" + this.isDead() + " | " + in_separatorStr;
        out_result = out_result + super.getDescription(in_separatorStr + "    ") + " | " + in_separatorStr;
        out_result = out_result + "inventory=";

        for (int i = 0; i < this.inventory.items.size() - 1; i++) {
            out_result = out_result + this.inventory.items.get(i) + ", ";
        }

        return out_result + " ] ";
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        DebugLog.Saving.trace("Saving: %s", this);
        super.save(output, IS_DEBUG_SAVE);
        if (this.descriptor == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.descriptor.save(output);
        }

        this.getVisual().save(output);
        ArrayList<InventoryItem> savedItems = this.inventory.save(output, this);
        this.savedInventoryItems.clear();
        this.savedInventoryItems.addAll(savedItems);
        output.put((byte)(this.asleep ? 1 : 0));
        output.putFloat(this.forceWakeUpTime);
        if (!this.isZombie()) {
            this.stats.save(output);
            this.getBodyDamage().save(output);
            this.xp.save(output);
            if (this.leftHandItem != null) {
                output.putInt(this.inventory.getItems().indexOf(this.leftHandItem));
            } else {
                output.putInt(-1);
            }

            if (this.rightHandItem != null) {
                output.putInt(this.inventory.getItems().indexOf(this.rightHandItem));
            } else {
                output.putInt(-1);
            }
        }

        output.put((byte)(this.onFire ? 1 : 0));
        output.putFloat(this.depressEffect);
        output.putFloat(this.depressFirstTakeTime);
        output.putFloat(this.betaEffect);
        output.putFloat(this.betaDelta);
        output.putFloat(this.painEffect);
        output.putFloat(this.painDelta);
        output.putFloat(this.sleepingTabletEffect);
        output.putFloat(this.sleepingTabletDelta);
        output.putInt(this.readBooks.size());

        for (int i = 0; i < this.readBooks.size(); i++) {
            IsoGameCharacter.ReadBook read = this.readBooks.get(i);
            GameWindow.WriteString(output, read.fullType);
            output.putInt(read.alreadyReadPages);
        }

        output.putFloat(this.reduceInfectionPower);
        output.putInt(this.knownRecipes.size());

        for (int i = 0; i < this.knownRecipes.size(); i++) {
            String recipe = this.knownRecipes.get(i);
            GameWindow.WriteString(output, recipe);
        }

        output.putInt(this.lastHourSleeped);
        output.putFloat(this.timeSinceLastSmoke);
        output.putFloat(this.beardGrowTiming);
        output.putFloat(this.hairGrowTiming);
        output.put((byte)(this.isUnlimitedCarry() ? 1 : 0));
        output.put((byte)(this.isBuildCheat() ? 1 : 0));
        output.put((byte)(this.isHealthCheat() ? 1 : 0));
        output.put((byte)(this.isMechanicsCheat() ? 1 : 0));
        output.put((byte)(this.isMovablesCheat() ? 1 : 0));
        output.put((byte)(this.isFarmingCheat() ? 1 : 0));
        output.put((byte)(this.isFishingCheat() ? 1 : 0));
        output.put((byte)(this.isCanUseBrushTool() ? 1 : 0));
        output.put((byte)(this.isFastMoveCheat() ? 1 : 0));
        output.put((byte)(this.isTimedActionInstantCheat() ? 1 : 0));
        output.put((byte)(this.isUnlimitedEndurance() ? 1 : 0));
        output.put((byte)(this.isUnlimitedAmmo() ? 1 : 0));
        output.put((byte)(this.isKnowAllRecipes() ? 1 : 0));
        output.put((byte)(this.isSneaking() ? 1 : 0));
        output.put((byte)(this.isDeathDragDown() ? 1 : 0));
        output.putInt(this.readLiterature.size());

        for (Entry<String, Integer> entry : this.getReadLiterature().entrySet()) {
            GameWindow.WriteString(output, entry.getKey());
            output.putInt(entry.getValue());
        }

        output.putInt(this.readPrintMedia.size());

        for (String media_id : this.readPrintMedia) {
            GameWindow.WriteString(output, media_id);
        }

        output.putLong(this.lastAnimalPet);
        this.getCheats().save(output, IS_DEBUG_SAVE);
    }

    public ChatElement getChatElement() {
        return this.chatElement;
    }

    @Override
    public void StartAction(BaseAction act) {
        this.characterActions.clear();
        this.characterActions.push(act);
        if (act.valid()) {
            act.waitToStart();
        }
    }

    public void QueueAction(BaseAction act) {
    }

    @Override
    public void StopAllActionQueue() {
        if (!this.characterActions.isEmpty()) {
            BaseAction act = this.characterActions.get(0);
            if (act.started) {
                act.stop();
            }

            this.characterActions.clear();
            if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
            }
        }
    }

    public void StopAllActionQueueRunning() {
        if (!this.characterActions.isEmpty()) {
            BaseAction act = this.characterActions.get(0);
            if (act.stopOnRun) {
                if (act.started) {
                    act.stop();
                }

                this.characterActions.clear();
                if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                    UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
                }
            }
        }
    }

    public void StopAllActionQueueAiming() {
        if (!this.characterActions.isEmpty()) {
            BaseAction act = this.characterActions.get(0);
            if (act.stopOnAim) {
                if (act.started) {
                    act.stop();
                }

                this.characterActions.clear();
                if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                    UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
                }
            }
        }
    }

    public void StopAllActionQueueWalking() {
        if (!this.characterActions.isEmpty()) {
            BaseAction act = this.characterActions.get(0);
            if (act.stopOnWalk) {
                if (act.started) {
                    act.stop();
                }

                this.characterActions.clear();
                if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                    UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
                }
            }
        }
    }

    @Override
    public String GetAnimSetName() {
        return "Base";
    }

    public void SleepingTablet(float SleepingTabletDelta) {
        this.sleepingTabletEffect = 6600.0F;
        this.sleepingTabletDelta += SleepingTabletDelta;
    }

    public void BetaBlockers(float delta) {
        this.betaEffect = 6600.0F;
        this.betaDelta += delta;
    }

    public void BetaAntiDepress(float delta) {
        if (this.depressEffect == 0.0F) {
            this.depressFirstTakeTime = 10000.0F;
        }

        this.depressEffect = 6600.0F;
        this.depressDelta += delta;
    }

    public void PainMeds(float delta) {
        this.painEffect = 5400.0F;
        this.painDelta += delta;
    }

    @Override
    public void initSpritePartsEmpty() {
        this.InitSpriteParts(this.descriptor);
    }

    public void InitSpriteParts(SurvivorDesc desc) {
        this.sprite.disposeAnimation();
        this.legsSprite = this.sprite;
        this.legsSprite.name = desc.getTorso();
        this.useParts = true;
    }

    @Override
    public boolean hasTrait(CharacterTrait characterTrait) {
        return this.characterTraits.get(characterTrait);
    }

    public void ApplyInBedOffset(boolean apply) {
        if (apply) {
            if (!this.onBed) {
                this.offsetX -= 20.0F;
                this.offsetY += 21.0F;
                this.onBed = true;
            }
        } else if (this.onBed) {
            this.offsetX += 20.0F;
            this.offsetY -= 21.0F;
            this.onBed = false;
        }
    }

    @Override
    public void Dressup(SurvivorDesc desc) {
        if (!this.isZombie()) {
            if (this.wornItems != null) {
                ItemVisuals itemVisuals = new ItemVisuals();
                desc.getItemVisuals(itemVisuals);
                this.wornItems.setFromItemVisuals(itemVisuals);
                this.wornItems.addItemsToItemContainer(this.inventory);
                desc.getWornItems().clear();
                this.onWornItemsChanged();
            }
        }
    }

    public void setPathSpeed(float speed) {
    }

    @Override
    public void PlayAnim(String string) {
    }

    @Override
    public void PlayAnimWithSpeed(String string, float framesSpeedPerFrame) {
    }

    @Override
    public void PlayAnimUnlooped(String string) {
    }

    public void DirectionFromVector(Vector2 vecA) {
        this.dir = IsoDirections.fromAngle(vecA);
    }

    public void DoFootstepSound(String type) {
        float volume = switch (type) {
            case "sneak_walk" -> 0.2F;
            case "sneak_run", "walk" -> 0.5F;
            case "strafe" -> this.sneaking ? 0.2F : 0.3F;
            case "run" -> 1.3F;
            case "sprint" -> 1.8F;
            default -> 1.0F;
        };
        this.DoFootstepSound(volume);
    }

    public void DoFootstepSound(float volume) {
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player == null || !player.isGhostMode() || DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue()) {
            if (this.getCurrentSquare() != null) {
                if (!(volume <= 0.0F)) {
                    volume *= 1.4F;
                    if (this.characterTraits.get(CharacterTrait.GRACEFUL)) {
                        volume *= 0.6F;
                    }

                    if (this.characterTraits.get(CharacterTrait.CLUMSY)) {
                        volume *= 1.2F;
                    }

                    if (this.getWornItem(ItemBodyLocation.SHOES) == null) {
                        volume *= 0.5F;
                    }

                    volume *= this.getLightfootMod();
                    volume *= 2.0F - this.getNimbleMod();
                    if (this.sneaking) {
                        volume *= this.getSneakSpotMod();
                    }

                    if (volume > 0.0F) {
                        this.emitter.playFootsteps("HumanFootstepsCombined", volume);
                        if (player != null && player.isGhostMode()) {
                            return;
                        }

                        int rad = (int)Math.ceil(volume * 10.0F);
                        if (this.sneaking) {
                            rad = Math.max(1, rad);
                        }

                        if (this.getCurrentSquare().getRoom() != null) {
                            rad = (int)(rad * 0.5F);
                        }

                        int rand = 2;
                        if (this.sneaking) {
                            rand = Math.min(12, 4 + this.getPerkLevel(PerkFactory.Perks.Lightfoot));
                        }

                        if (Rand.Next(rand) == 0) {
                            WorldSoundManager.instance
                                .addSound(
                                    this,
                                    PZMath.fastfloor(this.getX()),
                                    PZMath.fastfloor(this.getY()),
                                    PZMath.fastfloor(this.getZ()),
                                    rad,
                                    rad,
                                    false,
                                    0.0F,
                                    1.0F,
                                    false,
                                    false,
                                    false
                                );
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean Eat(InventoryItem info, float percentage) {
        return this.Eat(info, percentage, false);
    }

    public boolean EatOnClient(InventoryItem info, float percentage) {
        if (info instanceof Food food) {
            if (food.getOnEat() != null) {
                Object functionObj = LuaManager.getFunctionObject(food.getOnEat());
                if (functionObj != null) {
                    LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, info, this, BoxedStaticValues.toDouble(percentage));
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean Eat(InventoryItem info, float percentage, boolean useUtensil) {
        if (info instanceof Food food) {
            percentage = PZMath.clamp(percentage, 0.0F, 1.0F);
            float originalPercent = percentage;
            if (food.getBaseHunger() != 0.0F && food.getHungChange() != 0.0F) {
                float hungChange = food.getBaseHunger() * percentage;
                float usedPercent = hungChange / food.getHungChange();
                usedPercent = PZMath.clamp(usedPercent, 0.0F, 1.0F);
                percentage = usedPercent;
            }

            if (food.getHungChange() < 0.0F && food.getHungChange() * (1.0F - percentage) > -0.01F) {
                percentage = 1.0F;
            }

            if (food.getHungChange() == 0.0F && food.getThirstChange() < 0.0F && food.getThirstChange() * (1.0F - percentage) > -0.01F) {
                percentage = 1.0F;
            }

            this.stats.add(CharacterStat.THIRST, food.getThirstChange() * percentage);
            this.stats.add(CharacterStat.HUNGER, food.getHungerChange() * percentage);
            this.stats.add(CharacterStat.ENDURANCE, food.getEnduranceChange() * percentage);
            this.stats.add(CharacterStat.STRESS, food.getStressChange() * percentage);
            this.stats.add(CharacterStat.FATIGUE, food.getFatigueChange() * percentage);
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            if (player != null && !food.isBurnt()) {
                Nutrition nutrition = player.getNutrition();
                nutrition.setCalories(nutrition.getCalories() + food.getCalories() * percentage);
                nutrition.setCarbohydrates(nutrition.getCarbohydrates() + food.getCarbohydrates() * percentage);
                nutrition.setProteins(nutrition.getProteins() + food.getProteins() * percentage);
                nutrition.setLipids(nutrition.getLipids() + food.getLipids() * percentage);
            } else if (player != null && food.isBurnt()) {
                Nutrition nutrition = player.getNutrition();
                nutrition.setCalories(nutrition.getCalories() + food.getCalories() * percentage / 5.0F);
                nutrition.setCarbohydrates(nutrition.getCarbohydrates() + food.getCarbohydrates() * percentage / 5.0F);
                nutrition.setProteins(nutrition.getProteins() + food.getProteins() * percentage / 5.0F);
                nutrition.setLipids(nutrition.getLipids() + food.getLipids() * percentage / 5.0F);
            }

            this.getBodyDamage().setPainReduction(this.getBodyDamage().getPainReduction() + food.getPainReduction() * percentage);
            this.getBodyDamage().setColdReduction(this.getBodyDamage().getColdReduction() + food.getFluReduction() * percentage);
            if (this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS) && food.getFoodSicknessChange() > 0 && this.effectiveEdibleBuffTimer <= 0.0F) {
                this.stats.remove(CharacterStat.FOOD_SICKNESS, food.getFoodSicknessChange() * percentage);
                this.stats.remove(CharacterStat.POISON, food.getFoodSicknessChange() * percentage);
                if (this.characterTraits.get(CharacterTrait.IRON_GUT)) {
                    this.effectiveEdibleBuffTimer = Rand.Next(80.0F, 150.0F);
                } else if (this.characterTraits.get(CharacterTrait.WEAK_STOMACH)) {
                    this.effectiveEdibleBuffTimer = Rand.Next(200.0F, 280.0F);
                } else {
                    this.effectiveEdibleBuffTimer = Rand.Next(120.0F, 230.0F);
                }
            }

            this.getBodyDamage().JustAteFood(food, percentage, useUtensil);
            if (GameServer.server && this instanceof IsoPlayer) {
                INetworkPacket.send(
                    (IsoPlayer)this,
                    PacketTypes.PacketType.SyncPlayerStats,
                    this,
                    SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.THIRST)
                        + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.HUNGER)
                        + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.ENDURANCE)
                        + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.STRESS)
                        + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.FATIGUE)
                        + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.PAIN)
                );
                GameServer.sendSyncPlayerFields((IsoPlayer)this, (byte)8);
                INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.EatFood, this, food, percentage);
            }

            if (food.getOnEat() != null) {
                Object functionObj = LuaManager.getFunctionObject(food.getOnEat());
                if (functionObj != null) {
                    LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, info, this, BoxedStaticValues.toDouble(percentage));
                }
            }

            if (percentage == 1.0F) {
                food.setHungChange(0.0F);
                food.UseAndSync();
            } else {
                float hungChange = food.getHungChange();
                float thirstChange = food.getThirstChange();
                food.multiplyFoodValues(1.0F - percentage);
                if (hungChange == 0.0F && thirstChange < 0.0F && food.getThirstChange() > -0.01F) {
                    food.setHungChange(0.0F);
                    food.UseAndSync();
                    return true;
                }

                float emptyItemWeight = 0.0F;
                if (food.isCustomWeight()) {
                    String fullType = food.getReplaceOnUseFullType();
                    Item emptyItem = fullType == null ? null : ScriptManager.instance.getItem(fullType);
                    if (emptyItem != null) {
                        emptyItemWeight = emptyItem.getActualWeight();
                    }

                    food.setWeight(food.getWeight() - emptyItemWeight - originalPercent * (food.getWeight() - emptyItemWeight) + emptyItemWeight);
                }

                food.syncItemFields();
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean Eat(InventoryItem info) {
        return this.Eat(info, 1.0F);
    }

    @Override
    public boolean DrinkFluid(InventoryItem info, float percentage) {
        return this.DrinkFluid(info, percentage, false);
    }

    @Override
    public boolean DrinkFluid(InventoryItem info, float percentage, boolean useUtensil) {
        if (!info.hasComponent(ComponentType.FluidContainer)) {
            return false;
        } else {
            FluidContainer fluidCont = info.getFluidContainer();
            return this.DrinkFluid(fluidCont, percentage, useUtensil);
        }
    }

    @Override
    public boolean DrinkFluid(FluidContainer fluidCont, float percentage) {
        return this.DrinkFluid(fluidCont, percentage, false);
    }

    @Override
    public boolean DrinkFluid(FluidContainer fluidCont, float percentage, boolean useUtensil) {
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player != null) {
            Nutrition nutrition = player.getNutrition();
            nutrition.setCalories(nutrition.getCalories() + fluidCont.getProperties().getCalories() * percentage);
            nutrition.setCarbohydrates(nutrition.getCarbohydrates() + fluidCont.getProperties().getCarbohydrates() * percentage);
            nutrition.setProteins(nutrition.getProteins() + fluidCont.getProperties().getProteins() * percentage);
            nutrition.setLipids(nutrition.getLipids() + fluidCont.getProperties().getLipids() * percentage);
        }

        boolean isTaintedWater = fluidCont.isTainted();
        boolean isBleach = fluidCont.getPrimaryFluid().getFluidType() == FluidType.Bleach;
        FluidConsume consume = fluidCont.removeFluid(fluidCont.getAmount() * percentage, true);
        this.stats.add(CharacterStat.THIRST, consume.getThirstChange());
        this.stats.add(CharacterStat.HUNGER, consume.getHungerChange());
        this.stats.add(CharacterStat.ENDURANCE, consume.getEnduranceChange());
        this.stats.add(CharacterStat.STRESS, consume.getStressChange());
        this.stats.add(CharacterStat.FATIGUE, consume.getFatigueChange());
        this.stats.add(CharacterStat.BOREDOM, consume.getUnhappyChange());
        this.stats.add(CharacterStat.UNHAPPINESS, consume.getUnhappyChange());
        float hungerChange = Math.abs(consume.getHungerChange());
        this.getBodyDamage().setHealthFromFoodTimer((int)(this.getBodyDamage().getHealthFromFoodTimer() + hungerChange * 13000.0F));
        if (consume.getAlcohol() > 0.0F) {
            this.getBodyDamage().JustDrankBoozeFluid(consume.getAlcohol());
        }

        this.getBodyDamage().setPainReduction(this.getBodyDamage().getPainReduction() + consume.getPainReduction());
        this.getBodyDamage().setColdReduction(this.getBodyDamage().getColdReduction() + consume.getFluReduction());
        float poisonModified = consume.getPoison();
        if (isTaintedWater) {
            poisonModified *= 0.75F;
        }

        if (this.characterTraits.get(CharacterTrait.IRON_GUT)) {
            if (isTaintedWater) {
                poisonModified = 0.0F;
            } else if (!isBleach) {
                poisonModified /= 2.0F;
            }
        }

        if (this.characterTraits.get(CharacterTrait.WEAK_STOMACH)) {
            if (isTaintedWater) {
                poisonModified *= 1.2F;
            } else {
                poisonModified *= 2.0F;
            }
        }

        this.stats.add(CharacterStat.POISON, poisonModified);
        if (this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS) && consume.getFoodSicknessChange() > 0.0F && this.effectiveEdibleBuffTimer <= 0.0F) {
            this.stats.remove(CharacterStat.FOOD_SICKNESS, consume.getFoodSicknessChange());
            this.stats.remove(CharacterStat.POISON, consume.getFoodSicknessChange());
            if (this.characterTraits.get(CharacterTrait.IRON_GUT)) {
                this.effectiveEdibleBuffTimer = Rand.Next(80.0F, 150.0F);
            } else if (this.characterTraits.get(CharacterTrait.WEAK_STOMACH)) {
                this.effectiveEdibleBuffTimer = Rand.Next(200.0F, 280.0F);
            } else {
                this.effectiveEdibleBuffTimer = Rand.Next(120.0F, 230.0F);
            }
        }

        if (GameServer.server && this instanceof IsoPlayer) {
            INetworkPacket.send(
                (IsoPlayer)this,
                PacketTypes.PacketType.SyncPlayerStats,
                this,
                SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.INTOXICATION)
                    + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.THIRST)
                    + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.HUNGER)
                    + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.ENDURANCE)
                    + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.STRESS)
                    + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.FATIGUE)
                    + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.BOREDOM)
            );
        }

        return true;
    }

    @Override
    public boolean DrinkFluid(InventoryItem info) {
        return this.DrinkFluid(info, 1.0F);
    }

    public void FireCheck() {
        if (!this.onFire) {
            if (!GameClient.client || !(this instanceof IsoPlayer)) {
                if (!GameClient.client || !this.isZombie() || !(this instanceof IsoZombie) || !((IsoZombie)this).isRemoteZombie()) {
                    if (this.isZombie() && VirtualZombieManager.instance.isReused((IsoZombie)this)) {
                        DebugLog.log(DebugType.Zombie, "FireCheck running on REUSABLE ZOMBIE - IGNORED " + this);
                    } else if (this.getVehicle() == null) {
                        if (this.square != null
                            && this.square.getProperties().has(IsoFlagType.burning)
                            && (!GameClient.client || this.isZombie() && this.isLocal())) {
                            if ((!(this instanceof IsoPlayer) || Rand.Next(Rand.AdjustForFramerate(70)) != 0)
                                && !this.isZombie()
                                && !(this instanceof IsoAnimal)) {
                                if (!(this instanceof IsoPlayer)) {
                                    float damage = this.fireKillRate * GameTime.instance.getMultiplier() / 2.0F;
                                    CombatManager.getInstance().applyDamage(this, damage);
                                    this.setAttackedBy(null);
                                } else {
                                    float dpm = this.fireKillRate * GameTime.instance.getThirtyFPSMultiplier() * 60.0F / 2.0F;
                                    this.getBodyDamage().ReduceGeneralHealth(dpm);
                                    LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FIRE", dpm);
                                    this.getBodyDamage().OnFire(true);
                                    this.forceAwake();
                                }

                                if (this.isDead()) {
                                    IsoFireManager.RemoveBurningCharacter(this);
                                    if (this.isZombie()) {
                                        LuaEventManager.triggerEvent("OnZombieDead", this);
                                        if (GameClient.client) {
                                            this.setAttackedBy(IsoWorld.instance.currentCell.getFakeZombieForHit());
                                        }
                                    }
                                }
                            } else {
                                this.SetOnFire();
                            }
                        }
                    }
                }
            }
        }
    }

    public String getPrimaryHandType() {
        return this.leftHandItem == null ? null : this.leftHandItem.getType();
    }

    @Override
    public float getGlobalMovementMod(boolean bDoNoises) {
        return this.getCurrentState() != ClimbOverFenceState.instance()
                && this.getCurrentState() != ClimbThroughWindowState.instance()
                && this.getCurrentState() != ClimbOverWallState.instance()
            ? super.getGlobalMovementMod(bDoNoises)
            : 1.0F;
    }

    public float getMovementSpeed() {
        tempo2.x = this.getX() - this.getLastX();
        tempo2.y = this.getY() - this.getLastY();
        return tempo2.getLength();
    }

    public String getSecondaryHandType() {
        return this.rightHandItem == null ? null : this.rightHandItem.getType();
    }

    public boolean HasItem(String string) {
        return string == null
            ? true
            : string.equals(this.getSecondaryHandType()) || string.equals(this.getPrimaryHandType()) || this.inventory.contains(string);
    }

    @Override
    public void changeState(State state) {
        this.stateMachine.changeState(state, null);
    }

    @Override
    public State getCurrentState() {
        return this.stateMachine.getCurrent();
    }

    @Override
    public boolean isCurrentState(State state) {
        return this.stateMachine.isSubstate(state) ? true : this.stateMachine.getCurrent() == state;
    }

    public boolean isCurrentGameClientState(State state) {
        if (!GameClient.client) {
            return false;
        } else {
            return !this.isLocal() ? false : this.isCurrentState(state);
        }
    }

    public HashMap<Object, Object> getStateMachineParams(State state) {
        return this.stateMachineParams.computeIfAbsent(state, k -> new HashMap<>());
    }

    public void setStateMachineLocked(boolean val) {
        this.stateMachine.setLocked(val);
    }

    @Override
    public float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta) {
        return this.Hit(weapon, wielder, damageSplit, bIgnoreDamage, modDelta, false);
    }

    @Override
    public float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta, boolean bRemote) {
        if (wielder != null && weapon != null) {
            if (weapon.isMelee() && !bIgnoreDamage && this.isZombie()) {
                IsoZombie zed = (IsoZombie)this;
                zed.setHitTime(zed.getHitTime() + 1);
                if (zed.getHitTime() >= 4 && !bRemote) {
                    damageSplit *= (zed.getHitTime() - 2.0F) * 1.5F;
                }
            }

            if (wielder instanceof IsoPlayer player && player.isDoShove() && !wielder.isAimAtFloor()) {
                bIgnoreDamage = true;
                modDelta *= 1.5F;
            }

            LuaEventManager.triggerEvent("OnWeaponHitCharacter", wielder, this, weapon, damageSplit);
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "WEAPONHIT", damageSplit);
            if (LuaHookManager.TriggerHook("WeaponHitCharacter", wielder, this, weapon, damageSplit)) {
                return 0.0F;
            } else if (this.avoidDamage) {
                this.avoidDamage = false;
                return 0.0F;
            } else {
                if (this.noDamage) {
                    bIgnoreDamage = true;
                    this.noDamage = false;
                }

                if (this instanceof IsoSurvivor && !this.enemyList.contains(wielder)) {
                    this.enemyList.add(wielder);
                }

                this.staggerTimeMod = weapon.getStaggerBackTimeMod(wielder, this);
                wielder.addWorldSoundUnlessInvisible(5, 1, false);
                this.calculateHitDirection(weapon, wielder);
                this.setAttackedBy(wielder);
                float damage = bRemote ? damageSplit : this.processHitDamage(weapon, wielder, damageSplit, bIgnoreDamage, modDelta);
                this.hitConsequences(weapon, wielder, bIgnoreDamage, damage, bRemote);
                return damage;
            }
        } else {
            return 0.0F;
        }
    }

    private void calculateHitDirection(HandWeapon handWeapon, IsoGameCharacter wielder) {
        this.hitDir.x = this.getX();
        this.hitDir.y = this.getY();
        float x = wielder.getX();
        float y = wielder.getY();
        IsoGridSquare attackTargetSquare = handWeapon.getAttackTargetSquare(null);
        if (attackTargetSquare != null) {
            x = attackTargetSquare.getX();
            y = attackTargetSquare.getY();
            if (DebugOptions.instance.character.debug.render.explosionHitDirection.getValue()) {
                LineDrawer.addAlphaDecayingIsoCircle(x, y, wielder.getZ(), 0.25F, 16, 0.0F, 0.0F, 1.0F, 1.0F);
            }
        }

        if (DebugOptions.instance.character.debug.render.explosionHitDirection.getValue()) {
            LineDrawer.addAlphaDecayingLine(x, y, wielder.getZ(), this.getX(), this.getY(), this.getZ(), 1.0F, 0.0F, 0.0F, 1.0F);
        }

        this.hitDir.x -= x;
        this.hitDir.y -= y;
        this.getHitDir().normalize();
        this.hitDir.x = this.hitDir.x * handWeapon.getPushBackMod();
        this.hitDir.y = this.hitDir.y * handWeapon.getPushBackMod();
        this.hitDir.rotate(handWeapon.hitAngleMod);
    }

    private float processInstantExplosionHitDamage(HandWeapon weapon, IsoGameCharacter wielder, float damage, boolean bIgnoreDamage, float modDelta) {
        return damage * modDelta;
    }

    public float processHitDamage(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta) {
        if (weapon.isExplosive()) {
            return this.processInstantExplosionHitDamage(weapon, wielder, damageSplit, bIgnoreDamage, modDelta);
        } else {
            float damage = damageSplit * modDelta;
            float dmgTest = damage;
            if (bIgnoreDamage) {
                dmgTest = damage / 2.7F;
            }

            float f = dmgTest * wielder.getShovingMod();
            if (f > 1.0F) {
                f = 1.0F;
            }

            this.setHitForce(f);
            if (wielder.characterTraits.get(CharacterTrait.STRONG) && !weapon.isRanged()) {
                this.setHitForce(this.getHitForce() * 1.4F);
            }

            if (wielder.characterTraits.get(CharacterTrait.WEAK) && !weapon.isRanged()) {
                this.setHitForce(this.getHitForce() * 0.6F);
            }

            float del = wielder.stats.get(CharacterStat.ENDURANCE);
            del *= wielder.knockbackAttackMod;
            if (del < 0.5F) {
                del *= 1.3F;
                if (del < 0.4F) {
                    del = 0.4F;
                }

                this.setHitForce(this.getHitForce() * del);
            }

            if (wielder instanceof IsoPlayer && !bIgnoreDamage) {
                this.setHitForce(this.getHitForce() * 2.0F);
            }

            if (wielder instanceof IsoPlayer isoPlayer && !isoPlayer.isDoShove() && !bIgnoreDamage) {
                Vector2 oPos = tempVector2_1.set(this.getX(), this.getY());
                Vector2 tPos = tempVector2_2.set(wielder.getX(), wielder.getY());
                oPos.x = oPos.x - tPos.x;
                oPos.y = oPos.y - tPos.y;
                Vector2 dir = this.getVectorFromDirection(tempVector2_2);
                oPos.normalize();
                float dot = oPos.dot(dir);
                if (dot > -0.3F) {
                    damage *= 1.5F;
                }
            }

            damage = CombatManager.getInstance().applyPlayerReceivedDamageModifier(this, damage);
            damage = CombatManager.getInstance().applyWeaponLevelDamageModifier(wielder, damage);
            if (wielder instanceof IsoPlayer player && wielder.isAimAtFloor() && !bIgnoreDamage && !player.isDoShove()) {
                damage *= Math.max(5.0F, weapon.getCriticalDamageMultiplier());
            }

            if (wielder.isCriticalHit() && !bIgnoreDamage) {
                damage *= Math.max(2.0F, weapon.getCriticalDamageMultiplier());
            }

            return CombatManager.getInstance().applyOneHandedDamagePenalty(wielder, weapon, damage);
        }
    }

    public void hitConsequences(HandWeapon weapon, IsoGameCharacter wielder, boolean bIgnoreDamage, float damage, boolean bRemote) {
        if (!bIgnoreDamage) {
            damage = CombatManager.getInstance().applyGlobalDamageReductionMultipliers(weapon, damage);
            CombatManager.getInstance().applyDamage(this, damage);
        }

        if (this.isDead()) {
            if (!this.isOnKillDone()) {
                if (this instanceof IsoZombie) {
                    wielder.setZombieKills(wielder.getZombieKills() + 1);
                }

                if (GameServer.server) {
                    this.die();
                } else if (!GameClient.client) {
                    this.Kill(weapon, wielder);
                }
            }
        } else {
            if (weapon.isSplatBloodOnNoDeath()) {
                this.splatBlood(2, 0.2F);
            }

            if (!weapon.isRanged()) {
                if (weapon.isKnockBackOnNoDeath()) {
                    if (GameServer.server) {
                        if (wielder.xp != null) {
                            GameServer.addXp((IsoPlayer)wielder, PerkFactory.Perks.Strength, 2.0F);
                        }
                    } else if (!GameClient.client && wielder.xp != null) {
                        wielder.xp.AddXP(PerkFactory.Perks.Strength, 2.0F);
                    }
                }
            }
        }
    }

    public boolean IsAttackRange(float x, float y, float z) {
        float maxrange = 1.0F;
        float minrange = 0.0F;
        if (this.leftHandItem != null && this.leftHandItem instanceof HandWeapon handWeapon) {
            maxrange = handWeapon.getMaxRange(this);
            minrange = handWeapon.getMinRange();
            maxrange *= ((HandWeapon)this.leftHandItem).getRangeMod(this);
        }

        if (Math.abs(z - this.getZ()) > 0.3F) {
            return false;
        } else {
            float dist = IsoUtils.DistanceTo(x, y, this.getX(), this.getY());
            return dist < maxrange && dist > minrange;
        }
    }

    public boolean isMeleeAttackRange(HandWeapon handWeapon, IsoMovingObject isoMovingObject, Vector3 bonePos) {
        if (handWeapon != null && !handWeapon.isRanged()) {
            float deltaZ = Math.abs(isoMovingObject.getZ() - this.getZ());
            if (deltaZ >= 0.5F) {
                return false;
            } else {
                float range = handWeapon.getMaxRange(this);
                range *= handWeapon.getRangeMod(this);
                float distSq = IsoUtils.DistanceToSquared(this.getX(), this.getY(), bonePos.x, bonePos.y);
                IsoZombie zombie = Type.tryCastTo(isoMovingObject, IsoZombie.class);
                if (zombie != null
                    && distSq < 4.0F
                    && zombie.target == this
                    && (zombie.isCurrentState(LungeState.instance()) || zombie.isCurrentState(LungeNetworkState.instance()))) {
                    range += 0.2F;
                }

                return distSq < range * range;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean IsSpeaking() {
        return this.chatElement.IsSpeaking();
    }

    public boolean IsSpeakingNPC() {
        return this.chatElement.IsSpeakingNPC();
    }

    public void MoveForward(float dist, float x, float y, float soundDelta) {
        if (!this.isCurrentState(SwipeStatePlayer.instance())) {
            this.reqMovement.x = x;
            this.reqMovement.y = y;
            this.reqMovement.normalize();
            float mult = GameTime.instance.getMultiplier();
            this.setNextX(this.getNextX() + x * dist * mult);
            this.setNextY(this.getNextY() + y * dist * mult);
            this.DoFootstepSound(dist);
        }
    }

    protected boolean CanUsePathfindState() {
        return !GameServer.server;
    }

    protected void pathToAux(float x, float y, float z) {
        boolean bLineClear = true;
        if (PZMath.fastfloor(z) == PZMath.fastfloor(this.getZ()) && IsoUtils.DistanceManhatten(x, y, this.getX(), this.getY()) <= 30.0F) {
            int chunkX = PZMath.fastfloor(x) / 8;
            int chunkY = PZMath.fastfloor(y) / 8;
            IsoChunk chunk = GameServer.server
                ? ServerMap.instance.getChunk(chunkX, chunkY)
                : IsoWorld.instance.currentCell.getChunkForGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
            if (chunk != null) {
                int flags = 1;
                if (this instanceof IsoAnimal) {
                    flags &= -2;
                }

                flags |= 2;
                if (!this.isZombie()) {
                    flags |= 4;
                }

                bLineClear = !PolygonalMap2.instance
                    .lineClearCollide(this.getX(), this.getY(), x, y, PZMath.fastfloor(z), this.getPathFindBehavior2().getTargetChar(), flags);
            }
        }

        if (bLineClear
            && this.current != null
            && this.current.HasStairs()
            && !this.current.isSameStaircase(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z))) {
            bLineClear = false;
        }

        if (bLineClear) {
            if (this.CanUsePathfindState()) {
                this.setVariable("bPathfind", false);
            }

            this.setMoving(true);
        } else {
            if (this.CanUsePathfindState()) {
                this.setVariable("bPathfind", true);
            }

            this.setMoving(false);
        }
    }

    public void pathToCharacter(IsoGameCharacter target) {
        this.getPathFindBehavior2().pathToCharacter(target);
        this.pathToAux(target.getX(), target.getY(), target.getZ());
    }

    @Override
    public void pathToLocation(int x, int y, int z) {
        this.getPathFindBehavior2().pathToLocation(x, y, z);
        this.pathToAux(x + 0.5F, y + 0.5F, z);
    }

    @Override
    public void pathToLocationF(float x, float y, float z) {
        this.getPathFindBehavior2().pathToLocationF(x, y, z);
        this.pathToAux(x, y, z);
    }

    public void pathToSound(int x, int y, int z) {
        this.getPathFindBehavior2().pathToSound(x, y, z);
        this.pathToAux(x + 0.5F, y + 0.5F, z);
    }

    public boolean CanAttack() {
        if (!this.isPerformingAttackAnimation()
            && !this.getVariableBoolean("IsRacking")
            && !this.getVariableBoolean("IsUnloading")
            && StringUtils.isNullOrEmpty(this.getVariableString("RackWeapon"))) {
            if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                if (this.isCurrentState(PlayerHitReactionState.instance())) {
                    return false;
                }

                if (this.isCurrentState(PlayerHitReactionPVPState.instance()) && !ServerOptions.instance.pvpMeleeWhileHitReaction.getValue()) {
                    return false;
                }
            }

            if (this.isSitOnGround()) {
                return false;
            } else {
                InventoryItem attackItem = this.leftHandItem;
                if (attackItem instanceof HandWeapon handWeapon && attackItem.getSwingAnim() != null) {
                    this.setUseHandWeapon(handWeapon);
                }

                if (this.useHandWeapon == null) {
                    return true;
                } else if (this.useHandWeapon.getCondition() <= 0) {
                    this.setUseHandWeapon(null);
                    if (this.rightHandItem == this.leftHandItem) {
                        this.setSecondaryHandItem(null);
                    }

                    this.setPrimaryHandItem(null);
                    if (this.getInventory() != null) {
                        this.getInventory().setDrawDirty(true);
                    }

                    return false;
                } else {
                    return !this.isWeaponReady() ? false : !this.useHandWeapon.isCantAttackWithLowestEndurance() || this.isEnduranceSufficientForAction();
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isEnduranceSufficientForAction() {
        return !this.moodles.isMaxMoodleLevel(MoodleType.ENDURANCE);
    }

    public void ReduceHealthWhenBurning() {
        if (this.onFire) {
            if (this.isGodMod()) {
                this.StopBurning();
            } else if (!GameClient.client || !this.isZombie() || !(this instanceof IsoZombie) || !((IsoZombie)this).isRemoteZombie()) {
                if (!GameClient.client || !(this instanceof IsoPlayer) || !((IsoPlayer)this).remote) {
                    if (this.isAlive()) {
                        if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
                            float dpm = this.fireKillRate * GameTime.instance.getThirtyFPSMultiplier() * 60.0F;
                            this.getBodyDamage().ReduceGeneralHealth(dpm);
                            LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FIRE", dpm);
                            this.getBodyDamage().OnFire(true);
                        } else if (this.isZombie()) {
                            float damage = this.fireKillRate / 20.0F * GameTime.instance.getMultiplier();
                            CombatManager.getInstance().applyDamage(this, damage);
                            this.setAttackedBy(null);
                        } else {
                            float damage = this.fireKillRate * GameTime.instance.getMultiplier();
                            if (this instanceof IsoAnimal) {
                                damage -= this.fireKillRate / 10.0F * GameTime.instance.getMultiplier();
                            }

                            CombatManager.getInstance().applyDamage(this, damage);
                        }

                        if (this.isDead()) {
                            IsoFireManager.RemoveBurningCharacter(this);
                            if (this.isZombie()) {
                                LuaEventManager.triggerEvent("OnZombieDead", this);
                                if (GameClient.client) {
                                    this.setAttackedBy(IsoWorld.instance.currentCell.getFakeZombieForHit());
                                }
                            }
                        }
                    }

                    if (this instanceof IsoPlayer
                        && !(this instanceof IsoAnimal)
                        && Rand.Next(Rand.AdjustForFramerate(((IsoPlayer)this).IsRunning() ? 150 : 400)) == 0) {
                        this.StopBurning();
                    }
                }
            }
        }
    }

    @Deprecated
    public void DrawSneezeText() {
        if (this.getBodyDamage().IsSneezingCoughing() > 0) {
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            String SneezeText = null;
            if (this.getBodyDamage().IsSneezingCoughing() == 1) {
                SneezeText = Translator.getText("IGUI_PlayerText_Sneeze");
                if (player != null) {
                    player.playerVoiceSound("SneezeHeavy");
                }
            }

            if (this.getBodyDamage().IsSneezingCoughing() == 2) {
                SneezeText = Translator.getText("IGUI_PlayerText_Cough");
                if (player != null) {
                    player.playerVoiceSound("Cough");
                }
            }

            if (this.getBodyDamage().IsSneezingCoughing() == 3) {
                SneezeText = Translator.getText("IGUI_PlayerText_SneezeMuffled");
                if (player != null) {
                    player.playerVoiceSound("SneezeLight");
                }
            }

            if (this.getBodyDamage().IsSneezingCoughing() == 4) {
                SneezeText = Translator.getText("IGUI_PlayerText_CoughMuffled");
                if (player != null) {
                    player.playerVoiceSound("MuffledCough");
                }
            }

            float sx = this.sx;
            float sy = this.sy;
            sx = (int)sx;
            sy = (int)sy;
            sx -= (int)IsoCamera.getOffX();
            sy -= (int)IsoCamera.getOffY();
            sy -= 48.0F;
            if (SneezeText != null) {
                TextManager.instance
                    .DrawStringCentre(
                        UIFont.Dialogue, (int)sx, (int)sy, SneezeText, this.speakColour.r, this.speakColour.g, this.speakColour.b, this.speakColour.a
                    );
            }
        }
    }

    @Override
    public IsoSpriteInstance getSpriteDef() {
        if (this.def == null) {
            this.def = new IsoSpriteInstance();
        }

        return this.def;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (this.getDoRender()) {
            if (!this.isAlphaAndTargetZero()) {
                if (!this.isSeatedInVehicle() || this.getVehicle().showPassenger(this)) {
                    if (!this.isSpriteInvisible()) {
                        if (!this.isAlphaZero()) {
                            if (!this.useParts && this.def == null) {
                                this.def = new IsoSpriteInstance(this.sprite);
                            }

                            IndieGL.glDepthMask(true);
                            if (!PerformanceSettings.fboRenderChunk && this.doDefer && z - PZMath.fastfloor(z) > 0.2F) {
                                IsoGridSquare above = this.getCell().getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z) + 1);
                                if (above != null) {
                                    above.addDeferredCharacter(this);
                                }
                            }

                            IsoGridSquare currentSquare = this.getCurrentSquare();
                            if (currentSquare != null) {
                                currentSquare.interpolateLight(inf, x - currentSquare.getX(), y - currentSquare.getY());
                            } else {
                                inf.r = col.r;
                                inf.g = col.g;
                                inf.b = col.b;
                                inf.a = col.a;
                            }

                            if (Core.debug && DebugOptions.instance.pathfindRenderWaiting.getValue() && this.hasActiveModel()) {
                                if (this.getCurrentState() == PathFindState.instance() && this.finder.progress == AStarPathFinder.PathFindProgress.notyetfound) {
                                    this.legsSprite.modelSlot.model.tintR = 1.0F;
                                    this.legsSprite.modelSlot.model.tintG = 0.0F;
                                    this.legsSprite.modelSlot.model.tintB = 0.0F;
                                } else {
                                    this.legsSprite.modelSlot.model.tintR = 1.0F;
                                    this.legsSprite.modelSlot.model.tintG = 1.0F;
                                    this.legsSprite.modelSlot.model.tintB = 1.0F;
                                }
                            }

                            if (this.dir == IsoDirections.Max) {
                                this.dir = IsoDirections.N;
                            }

                            lastRenderedRendered = lastRendered;
                            lastRendered = this;
                            this.checkUpdateModelTextures();
                            float SCL = Core.tileScale;
                            float offsetX = this.offsetX + 1.0F * SCL;
                            float offsetY = this.offsetY + -89.0F * SCL;
                            if (this.sprite != null) {
                                this.def.setScale(SCL, SCL);
                                if (!this.useParts) {
                                    this.sprite.render(this.def, this, x, y, z, this.dir, offsetX, offsetY, inf, true);
                                } else if (this.legsSprite.hasActiveModel()) {
                                    this.legsSprite.renderActiveModel();
                                } else if (!this.renderTextureInsteadOfModel(x, y)) {
                                    this.def.flip = false;
                                    inf.r = 1.0F;
                                    inf.g = 1.0F;
                                    inf.b = 1.0F;
                                    inf.a = this.def.alpha * 0.4F;
                                    this.legsSprite.renderCurrentAnim(this.def, this, x, y, z, this.dir, offsetX, offsetY, inf, false, null);
                                }
                            }

                            if (this.attachedAnimSprite != null) {
                                if (PerformanceSettings.fboRenderChunk) {
                                    FBORenderCell.instance.renderTranslucentOnly = true;
                                }

                                for (int n = 0; n < this.attachedAnimSprite.size(); n++) {
                                    IsoSpriteInstance spr = this.attachedAnimSprite.get(n);
                                    spr.update();
                                    float fa = inf.a;
                                    inf.a = spr.alpha;
                                    spr.SetTargetAlpha(this.getTargetAlpha());
                                    if (this.isOnFire()) {
                                        spr.getParentSprite().soffX = (short)(this.offsetX + 1.0F * SCL);
                                        float sprX = this.getX();
                                        float sprY = this.getY();
                                        int offY = 40;
                                        if (this.hasAnimationPlayer()) {
                                            CombatManager.getBoneWorldPos(this, "Bip01_Spine1", tempVectorBonePos);
                                            sprX = tempVectorBonePos.x;
                                            sprY = tempVectorBonePos.y;
                                            offY = (int)((tempVectorBonePos.z - this.getZ()) * 96.0F + (this.isProne() ? 0 : 10));
                                        }

                                        spr.getParentSprite().soffY = (short)(this.offsetY + -89.0F * SCL - offY * SCL);
                                        float dxy = this.isProne() ? 0.25F : 0.5F;
                                        float dz = 0.0F;
                                        inf.set(1.0F, 1.0F, 1.0F, inf.a);
                                        spr.render(this, sprX + dxy, sprY + dxy, z + 0.0F, IsoDirections.N, offsetX, offsetY, inf);
                                    } else {
                                        spr.render(this, x, y, z, this.dir, offsetX, offsetY, inf);
                                    }

                                    inf.a = fa;
                                }

                                if (PerformanceSettings.fboRenderChunk) {
                                    FBORenderCell.instance.renderTranslucentOnly = false;
                                }
                            }

                            for (int n = 0; n < this.inventory.items.size(); n++) {
                                InventoryItem item = this.inventory.items.get(n);
                                if (item instanceof IUpdater iUpdater) {
                                    iUpdater.render();
                                }
                            }

                            if (this.canRagdoll() && this.getRagdollController() != null) {
                                this.getRagdollController().debugRender();
                            }

                            if (this.ballisticsController != null) {
                                this.ballisticsController.debugRender();
                            }

                            if (this.ballisticsTarget != null) {
                                this.ballisticsTarget.debugRender();
                            }
                        }
                    }
                }
            }
        }
    }

    public void renderServerGUI() {
        if (this instanceof IsoPlayer) {
            this.setSceneCulled(false);
        }

        if (this.updateModelTextures && this.hasActiveModel()) {
            this.updateModelTextures = false;
            this.textureCreator = ModelInstanceTextureCreator.alloc();
            this.textureCreator.init(this);
        }

        float SCL = Core.tileScale;
        float offsetX = this.offsetX + 1.0F * SCL;
        float offsetY = this.offsetY + -89.0F * SCL;
        if (this.sprite != null) {
            this.def.setScale(SCL, SCL);
            inf.r = 1.0F;
            inf.g = 1.0F;
            inf.b = 1.0F;
            inf.a = this.def.alpha * 0.4F;
            if (!this.isbUseParts()) {
                this.sprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, offsetX, offsetY, inf, true);
            } else {
                this.def.flip = false;
                this.legsSprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, offsetX, offsetY, inf, true);
            }
        }

        if (Core.debug && this.hasActiveModel()) {
            if (this instanceof IsoZombie) {
                int sx = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
                int sy = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
                TextManager.instance.DrawString(sx, sy, "ID: " + this.getOnlineID());
                TextManager.instance.DrawString(sx, sy + 10, "State: " + this.getCurrentStateName());
                TextManager.instance.DrawString(sx, sy + 20, "Health: " + this.getHealth());
            }

            float maxRange = 2.0F;
            Vector2 dir = tempo;
            this.getDeferredMovement(dir);
            this.drawDirectionLine(dir, 1000.0F * dir.getLength() / GameTime.instance.getMultiplier() * 2.0F, 1.0F, 0.5F, 0.5F);
        }
    }

    @Override
    protected float getAlphaUpdateRateMul() {
        float mul = super.getAlphaUpdateRateMul();
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter.characterTraits.get(CharacterTrait.SHORT_SIGHTED)) {
            mul /= 2.0F;
        }

        if (isoGameCharacter.characterTraits.get(CharacterTrait.EAGLE_EYED)) {
            mul *= 1.5F;
        }

        return mul;
    }

    @Override
    protected boolean isUpdateAlphaDuringRender() {
        return false;
    }

    public boolean isSeatedInVehicle() {
        return this.vehicle != null && this.vehicle.getSeat(this) != -1;
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
        if (!this.useParts) {
            this.sprite.renderObjectPicker(this.def, this, this.dir);
        } else {
            this.legsSprite.renderObjectPicker(this.def, this, this.dir);
        }
    }

    private static Vector2 closestpointonline(double lx1, double ly1, double lx2, double ly2, double x0, double y0, Vector2 out) {
        double A1 = ly2 - ly1;
        double B1 = lx1 - lx2;
        double C1 = (ly2 - ly1) * lx1 + (lx1 - lx2) * ly1;
        double C2 = -B1 * x0 + A1 * y0;
        double det = A1 * A1 - -B1 * B1;
        double cx;
        double cy;
        if (det != 0.0) {
            cx = (A1 * C1 - B1 * C2) / det;
            cy = (A1 * C2 - -B1 * C1) / det;
        } else {
            cx = x0;
            cy = y0;
        }

        return out.set((float)cx, (float)cy);
    }

    public ShadowParams calculateShadowParams(ShadowParams sp) {
        if (!this.hasAnimationPlayer()) {
            return sp.set(0.45F, 1.4F, 1.125F);
        } else {
            float animalSize = this instanceof IsoAnimal animal ? animal.getAnimalSize() : 1.0F;
            return calculateShadowParams(this.getAnimationPlayer(), animalSize, false, sp);
        }
    }

    public static ShadowParams calculateShadowParams(AnimationPlayer animationPlayer, float animalSize, boolean bRagdoll, ShadowParams sp) {
        float w = 0.45F;
        float fm = 1.4F;
        float bm = 1.125F;
        if (animationPlayer != null && animationPlayer.isReady()) {
            float x = 0.0F;
            float y = 0.0F;
            float z = 0.0F;
            Vector3 v = IsoGameCharacter.L_renderShadow.vector3;
            Model.BoneToWorldCoords(animationPlayer, 0.0F, 0.0F, 0.0F, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_Head", -1), v);
            float p1x = v.x;
            float p1y = v.y;
            Model.BoneToWorldCoords(animationPlayer, 0.0F, 0.0F, 0.0F, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_L_Foot", -1), v);
            float p2x = v.x;
            float p2y = v.y;
            Model.BoneToWorldCoords(animationPlayer, 0.0F, 0.0F, 0.0F, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_R_Foot", -1), v);
            float p3x = v.x;
            float p3y = v.y;
            if (bRagdoll) {
                Model.BoneToWorldCoords(animationPlayer, 0.0F, 0.0F, 0.0F, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_Pelvis", -1), v);
                p1x -= v.x;
                p1y -= v.y;
                p2x -= v.x;
                p2y -= v.y;
                p3x -= v.x;
                p3y -= v.y;
            }

            Vector3f vClosest = IsoGameCharacter.L_renderShadow.vector3f;
            float fLen = 0.0F;
            float bLen = 0.0F;
            Vector3f forward = IsoGameCharacter.L_renderShadow.forward;
            Vector2 forward2 = IsoGameCharacter.L_renderShadow.vector2_1.setLengthAndDirection(animationPlayer.getAngle(), 1.0F);
            forward.set(forward2.x, forward2.y, 0.0F);
            Vector2 closest = closestpointonline(0.0, 0.0, 0.0F + forward.x, 0.0F + forward.y, p1x, p1y, IsoGameCharacter.L_renderShadow.vector2_2);
            float cx = closest.x;
            float cy = closest.y;
            float cLen = closest.set(cx - 0.0F, cy - 0.0F).getLength();
            if (cLen > 0.001F) {
                vClosest.set(cx - 0.0F, cy - 0.0F, 0.0F).normalize();
                if (forward.dot(vClosest) > 0.0F) {
                    fLen = Math.max(fLen, cLen);
                } else {
                    bLen = Math.max(bLen, cLen);
                }
            }

            closest = closestpointonline(0.0, 0.0, 0.0F + forward.x, 0.0F + forward.y, p2x, p2y, IsoGameCharacter.L_renderShadow.vector2_2);
            cx = closest.x;
            cy = closest.y;
            cLen = closest.set(cx - 0.0F, cy - 0.0F).getLength();
            if (cLen > 0.001F) {
                vClosest.set(cx - 0.0F, cy - 0.0F, 0.0F).normalize();
                if (forward.dot(vClosest) > 0.0F) {
                    fLen = Math.max(fLen, cLen);
                } else {
                    bLen = Math.max(bLen, cLen);
                }
            }

            closest = closestpointonline(0.0, 0.0, 0.0F + forward.x, 0.0F + forward.y, p3x, p3y, IsoGameCharacter.L_renderShadow.vector2_2);
            cx = closest.x;
            cy = closest.y;
            cLen = closest.set(cx - 0.0F, cy - 0.0F).getLength();
            if (cLen > 0.001F) {
                vClosest.set(cx - 0.0F, cy - 0.0F, 0.0F).normalize();
                if (forward.dot(vClosest) > 0.0F) {
                    fLen = Math.max(fLen, cLen);
                } else {
                    bLen = Math.max(bLen, cLen);
                }
            }

            fm = (fLen + 0.35F) * 1.35F;
            bm = (bLen + 0.35F) * 1.35F;
        }

        return sp.set(0.45F, fm, bm);
    }

    public void renderShadow(float x, float y, float z) {
        if (Core.getInstance().isDisplayPlayerModel() || this.isAnimal() || this.isZombie() || !this.isLocal()) {
            if (!this.isAlphaAndTargetZero()) {
                if (!this.isSeatedInVehicle()) {
                    IsoGridSquare currentSquare = this.getCurrentSquare();
                    if (currentSquare != null) {
                        float heightAboveFloor = this.getHeightAboveFloor();
                        if (!(heightAboveFloor > 0.5F)) {
                            int playerIndex = IsoCamera.frameState.playerIndex;
                            ShadowParams shadowParams = this.calculateShadowParams(IsoGameCharacter.L_renderShadow.shadowParams);
                            float w = shadowParams.w;
                            float fm = shadowParams.fm;
                            float bm = shadowParams.bm;
                            float alpha = this.getAlpha(playerIndex);
                            if (this instanceof IsoZombie zombie && zombie.isSkeleton()) {
                                alpha *= 0.5F;
                            }

                            if (heightAboveFloor > 0.0F) {
                                alpha *= 1.0F - heightAboveFloor / 0.5F;
                            }

                            if (this.hasActiveModel() && this.hasAnimationPlayer() && this.getAnimationPlayer().isReady()) {
                                float mult = 0.1F * GameTime.getInstance().getThirtyFPSMultiplier();
                                mult = PZMath.clamp(mult, 0.0F, 1.0F);
                                if (this.shadowTick != IngameState.instance.numberTicks - 1L) {
                                    this.shadowFm = fm;
                                    this.shadowBm = bm;
                                }

                                this.shadowTick = IngameState.instance.numberTicks;
                                this.shadowFm = PZMath.lerp(this.shadowFm, fm, mult);
                                fm = this.shadowFm;
                                this.shadowBm = PZMath.lerp(this.shadowBm, bm, mult);
                                bm = this.shadowBm;
                            } else if (this.isZombie() && this.isCurrentState(FakeDeadZombieState.instance())) {
                                alpha = 1.0F;
                            } else if (this.isSceneCulled()) {
                                return;
                            }

                            Vector2 animVector = this.getAnimVector(IsoGameCharacter.L_renderShadow.vector2_1);
                            Vector3f forward = IsoGameCharacter.L_renderShadow.forward.set(animVector.x, animVector.y, 0.0F);
                            if (this.getRagdollController() != null) {
                                RagdollStateData ragdollStateData = this.getRagdollController().getRagdollStateData();
                                if (ragdollStateData != null && ragdollStateData.isCalculated) {
                                    forward.x = ragdollStateData.simulationDirection.x;
                                    forward.y = ragdollStateData.simulationDirection.y;
                                }
                            }

                            ColorInfo lightInfo = currentSquare.lighting[playerIndex].lightInfo();
                            if (PerformanceSettings.fboRenderChunk) {
                                FBORenderShadows.getInstance()
                                    .addShadow(x, y, z - heightAboveFloor, forward, w, fm, bm, lightInfo.r, lightInfo.g, lightInfo.b, alpha, false);
                            } else {
                                IsoDeadBody.renderShadow(x, y, z - heightAboveFloor, forward, w, fm, bm, lightInfo, alpha);
                            }
                        }
                    }
                }
            }
        }
    }

    public void checkUpdateModelTextures() {
        if (this.updateModelTextures && this.hasActiveModel()) {
            this.updateModelTextures = false;
            this.textureCreator = ModelInstanceTextureCreator.alloc();
            this.textureCreator.init(this);
        }

        if (this.updateEquippedTextures && this.hasActiveModel()) {
            this.updateEquippedTextures = false;
            if (this.primaryHandModel != null && this.primaryHandModel.getTextureInitializer() != null) {
                this.primaryHandModel.getTextureInitializer().setDirty();
            }

            if (this.secondaryHandModel != null && this.secondaryHandModel.getTextureInitializer() != null) {
                this.secondaryHandModel.getTextureInitializer().setDirty();
            }
        }
    }

    @Override
    public boolean isMaskClicked(int x, int y, boolean flip) {
        if (this.sprite == null) {
            return false;
        } else {
            return !this.useParts ? super.isMaskClicked(x, y, flip) : this.legsSprite.isMaskClicked(this.dir, x, y, flip);
        }
    }

    @Override
    public void setHaloNote(String str) {
        this.setHaloNote(str, this.haloDispTime);
    }

    @Override
    public void setHaloNote(String str, float dispTime) {
        this.setHaloNote(str, 0, 255, 0, dispTime);
    }

    @Override
    public void setHaloNote(String str, int r, int g, int b, float dispTime) {
        if (this.haloNote != null && str != null) {
            this.haloDispTime = dispTime;
            this.haloNote.setDefaultColors(r, g, b);
            this.haloNote.ReadString(str);
            this.haloNote.setInternalTickClock(this.haloDispTime);
        }
    }

    public float getHaloTimerCount() {
        return this.haloNote != null ? this.haloNote.getInternalClock() : 0.0F;
    }

    public void DoSneezeText() {
        if (this.getBodyDamage() != null) {
            if (this.getBodyDamage().IsSneezingCoughing() > 0) {
                IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
                String SneezeText = null;
                int sneezeVar = 0;
                if (this.getBodyDamage().IsSneezingCoughing() == 1) {
                    SneezeText = Translator.getText("IGUI_PlayerText_Sneeze");
                    sneezeVar = Rand.Next(2) + 1;
                    this.setVariable("Ext", "Sneeze" + sneezeVar);
                    if (player != null) {
                        player.playerVoiceSound("SneezeHeavy");
                    }
                }

                if (this.getBodyDamage().IsSneezingCoughing() == 2) {
                    SneezeText = Translator.getText("IGUI_PlayerText_Cough");
                    this.setVariable("Ext", "Cough");
                    if (player != null) {
                        player.playerVoiceSound("Cough");
                    }
                }

                if (this.getBodyDamage().IsSneezingCoughing() == 3) {
                    SneezeText = Translator.getText("IGUI_PlayerText_SneezeMuffled");
                    sneezeVar = Rand.Next(2) + 1;
                    this.setVariable("Ext", "Sneeze" + sneezeVar);
                    if (player != null) {
                        player.playerVoiceSound("SneezeLight");
                    }
                }

                if (this.getBodyDamage().IsSneezingCoughing() == 4) {
                    SneezeText = Translator.getText("IGUI_PlayerText_CoughMuffled");
                    this.setVariable("Ext", "Cough");
                    if (player != null) {
                        player.playerVoiceSound("MuffledCough");
                    }
                }

                if (SneezeText != null) {
                    this.Say(SneezeText);
                    this.reportEvent("EventDoExt");
                    if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                        GameClient.sendSneezingCoughing((IsoPlayer)this, this.getBodyDamage().IsSneezingCoughing(), (byte)sneezeVar);
                    }
                }
            }
        }
    }

    @Override
    public String getSayLine() {
        return this.chatElement.getSayLine();
    }

    /**
     * 
     * @param sayLine the sayLine to set
     */
    public void setSayLine(String sayLine) {
        this.Say(sayLine);
    }

    public ChatMessage getLastChatMessage() {
        return this.lastChatMessage;
    }

    public void setLastChatMessage(ChatMessage lastChatMessage) {
        this.lastChatMessage = lastChatMessage;
    }

    public String getLastSpokenLine() {
        return this.lastSpokenLine;
    }

    public void setLastSpokenLine(String line) {
        this.lastSpokenLine = line;
    }

    protected void doSleepSpeech() {
        this.sleepSpeechCnt++;
        if (this.sleepSpeechCnt > 250 * PerformanceSettings.getLockFPS() / 30.0F) {
            this.sleepSpeechCnt = 0;
            if (sleepText == null) {
                sleepText = "ZzzZZZzzzz";
                ChatElement.addNoLogText(sleepText);
            }

            this.SayWhisper(sleepText);
        }
    }

    public void SayDebug(String text) {
        this.chatElement.SayDebug(0, text);
    }

    public void SayDebug(int n, String text) {
        this.chatElement.SayDebug(n, text);
    }

    public int getMaxChatLines() {
        return this.chatElement.getMaxChatLines();
    }

    @Override
    public void Say(String line) {
        if (!this.isZombie()) {
            this.ProcessSay(line, this.speakColour.r, this.speakColour.g, this.speakColour.b, 30.0F, 0, "default");
        }
    }

    @Override
    public void Say(String line, float r, float g, float b, UIFont font, float baseRange, String customTag) {
        this.ProcessSay(line, r, g, b, baseRange, 0, customTag);
    }

    public void SayWhisper(String line) {
        this.ProcessSay(line, this.speakColour.r, this.speakColour.g, this.speakColour.b, 10.0F, 0, "whisper");
    }

    public void SayShout(String line) {
        this.ProcessSay(line, this.speakColour.r, this.speakColour.g, this.speakColour.b, 60.0F, 0, "shout");
    }

    public void SayRadio(String line, float r, float g, float b, UIFont font, float baseRange, int channel, String customTag) {
        this.ProcessSay(line, r, g, b, baseRange, channel, customTag);
    }

    private void ProcessSay(String line, float r, float g, float b, float baseRange, int channel, String customTag) {
        if (this.allowConversation) {
            if (TutorialManager.instance.profanityFilter) {
                line = ProfanityFilter.getInstance().filterString(line);
            }

            if (customTag.equals("default")) {
                ChatManager.getInstance().showInfoMessage(((IsoPlayer)this).getUsername(), line);
                this.lastSpokenLine = line;
            } else if (customTag.equals("whisper")) {
                this.lastSpokenLine = line;
            } else if (customTag.equals("shout")) {
                ChatManager.getInstance().sendMessageToChat(((IsoPlayer)this).getUsername(), ChatType.shout, line);
                this.lastSpokenLine = line;
            } else if (customTag.equals("radio")) {
                UIFont font = UIFont.Medium;
                boolean bbcode = true;
                boolean img = true;
                boolean icons = true;
                boolean colors = false;
                boolean fonts = false;
                boolean equalizeHeights = true;
                this.chatElement.addChatLine(line, r, g, b, font, baseRange, customTag, true, true, true, false, false, true);
                if (ZomboidRadio.isStaticSound(line)) {
                    ChatManager.getInstance().showStaticRadioSound(line);
                } else {
                    ChatManager.getInstance().showRadioMessage(line, channel);
                }
            }
        }
    }

    public void addLineChatElement(String line) {
        this.addLineChatElement(line, 1.0F, 1.0F, 1.0F);
    }

    public void addLineChatElement(String line, float r, float g, float b) {
        this.addLineChatElement(line, r, g, b, UIFont.Dialogue, 30.0F, "default");
    }

    public void addLineChatElement(String line, float r, float g, float b, UIFont font, float baseRange, String customTag) {
        this.addLineChatElement(line, r, g, b, font, baseRange, customTag, false, false, false, false, false, true);
    }

    public void addLineChatElement(
        String line,
        float r,
        float g,
        float b,
        UIFont font,
        float baseRange,
        String customTag,
        boolean bbcode,
        boolean img,
        boolean icons,
        boolean colors,
        boolean fonts,
        boolean equalizeHeights
    ) {
        this.chatElement.addChatLine(line, r, g, b, font, baseRange, customTag, bbcode, img, icons, colors, fonts, equalizeHeights);
    }

    protected boolean playerIsSelf() {
        return IsoPlayer.getInstance() == this;
    }

    public int getUserNameHeight() {
        if (!GameClient.client) {
            return 0;
        } else {
            return this.userName != null ? this.userName.getHeight() : 0;
        }
    }

    protected void initTextObjects() {
        this.hasInitTextObjects = true;
        if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
            this.chatElement.setMaxChatLines(5);
            if (IsoPlayer.getInstance() != null) {
                DebugLog.DetailedInfo.trace("FirstNAME:" + IsoPlayer.getInstance().username);
            }

            this.isoPlayer = (IsoPlayer)this;
            if (this.isoPlayer.username != null) {
                this.userName = new TextDrawObject();
                this.userName.setAllowAnyImage(true);
                this.userName.setDefaultFont(UIFont.Small);
                this.userName.setDefaultColors(255, 255, 255, 255);
                this.updateUserName();
            }

            if (this.haloNote == null) {
                this.haloNote = new TextDrawObject();
                this.haloNote.setDefaultFont(UIFont.Small);
                this.haloNote.setDefaultColors(0, 255, 0);
                this.haloNote.setDrawBackground(true);
                this.haloNote.setAllowImages(true);
                this.haloNote.setAllowAnyImage(true);
                this.haloNote.setOutlineColors(0.0F, 0.0F, 0.0F, 0.33F);
            }
        }
    }

    protected void updateUserName() {
        if (this.userName != null && this.isoPlayer != null) {
            String nameStr = this.isoPlayer.getDisplayName();
            if (this != IsoPlayer.getInstance()
                && this.isInvisible()
                && IsoPlayer.getInstance() != null
                && IsoPlayer.getInstance().role != null
                && !IsoPlayer.getInstance().role.hasCapability(Capability.CanSeePlayersStats)
                && (!Core.debug || !DebugOptions.instance.cheat.player.seeEveryone.getValue())) {
                this.userName.ReadString("");
                return;
            }

            Faction fact = Faction.getPlayerFaction(this.isoPlayer);
            if (fact != null) {
                if (!this.isoPlayer.showTag && this.isoPlayer != IsoPlayer.getInstance() && Faction.getPlayerFaction(IsoPlayer.getInstance()) != fact) {
                    this.isoPlayer.tagPrefix = "";
                } else {
                    this.isoPlayer.tagPrefix = fact.getTag();
                    if (fact.getTagColor() != null) {
                        this.isoPlayer.setTagColor(fact.getTagColor());
                    }
                }
            } else {
                this.isoPlayer.tagPrefix = "";
            }

            IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
            boolean showMyUsername = this.isoPlayer != null && this.isoPlayer.remote || Core.getInstance().isShowYourUsername();
            boolean bViewerIsAdmin = GameClient.client
                && isoGameCharacter instanceof IsoPlayer player
                && player.role != null
                && player.role.hasCapability(Capability.CanSeePlayersStats);
            boolean canSeeAll = isoGameCharacter instanceof IsoPlayer playerx && playerx.canSeeAll();
            if (!ServerOptions.instance.displayUserName.getValue() && !ServerOptions.instance.showFirstAndLastName.getValue() && !canSeeAll) {
                showMyUsername = false;
            }

            if (!showMyUsername) {
                nameStr = "";
            }

            if (showMyUsername && this.isoPlayer.tagPrefix != null && !this.isoPlayer.tagPrefix.isEmpty() && (!this.isDisguised() || bViewerIsAdmin)) {
                nameStr = "[col="
                    + (int)(this.isoPlayer.getTagColor().r * 255.0F)
                    + ","
                    + (int)(this.isoPlayer.getTagColor().g * 255.0F)
                    + ","
                    + (int)(this.isoPlayer.getTagColor().b * 255.0F)
                    + "]["
                    + this.isoPlayer.tagPrefix
                    + "][/] "
                    + nameStr;
            }

            if (showMyUsername
                && this.isoPlayer.role != null
                && this.isoPlayer.role.hasCapability(Capability.CanSeePlayersStats)
                && this.isoPlayer.isShowAdminTag()) {
                nameStr = String.format(
                        "[col=%d,%d,%d]%s[/] ",
                        (int)(this.isoPlayer.role.getColor().getR() * 255.0F),
                        (int)(this.isoPlayer.role.getColor().getG() * 255.0F),
                        (int)(this.isoPlayer.role.getColor().getB() * 255.0F),
                        this.isoPlayer.role.getName()
                    )
                    + nameStr;
            }

            if (showMyUsername && this.checkPVP()) {
                String namePvpSuffix1 = " [img=media/ui/Skull1.png]";
                if (this.isoPlayer.getSafety().getToggle() == 0.0F) {
                    namePvpSuffix1 = " [img=media/ui/Skull2.png]";
                }

                nameStr = nameStr + namePvpSuffix1;
            }

            if (this.isoPlayer.isSpeek && !this.isoPlayer.isVoiceMute) {
                nameStr = "[img=media/ui/voiceon.png] " + nameStr;
            }

            if (this.isoPlayer.isVoiceMute) {
                nameStr = "[img=media/ui/voicemuted.png] " + nameStr;
            }

            BaseVehicle vehicle = isoGameCharacter == this.isoPlayer ? this.isoPlayer.getNearVehicle() : null;
            if (this.getVehicle() == null
                && vehicle != null
                && (
                    this.isoPlayer.getInventory().haveThisKeyId(vehicle.getKeyId()) != null
                        || vehicle.isHotwired()
                        || SandboxOptions.getInstance().vehicleEasyUse.getValue()
                )) {
                Color newC = Color.HSBtoRGB(vehicle.colorHue, vehicle.colorSaturation * 0.5F, vehicle.colorValue);
                nameStr = " [img=media/ui/CarKey.png," + newC.getRedByte() + "," + newC.getGreenByte() + "," + newC.getBlueByte() + "]" + nameStr;
            }

            if (!nameStr.equals(this.userName.getOriginal())) {
                this.userName.ReadString(nameStr);
            }
        }
    }

    private boolean checkPVP() {
        if (this.isoPlayer.getSafety().isEnabled()) {
            return false;
        } else if (!ServerOptions.instance.showSafety.getValue()) {
            return false;
        } else if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(this.isoPlayer.getX()), PZMath.fastfloor(this.isoPlayer.getY())) != null) {
            return false;
        } else {
            return IsoPlayer.getInstance() != this.isoPlayer && Faction.isInSameFaction(IsoPlayer.getInstance(), this.isoPlayer)
                ? this.isoPlayer.isFactionPvp()
                : true;
        }
    }

    public void updateTextObjects() {
        if (!GameServer.server) {
            if (!this.hasInitTextObjects) {
                this.initTextObjects();
            }

            if (!this.speaking) {
                this.DoSneezeText();
                if (this.isAsleep() && this.getCurrentSquare() != null && this.getCurrentSquare().getCanSee(0)) {
                    this.doSleepSpeech();
                }
            }

            if (this.isoPlayer != null) {
                this.radioEquipedCheck();
            }

            this.speaking = false;
            this.drawUserName = false;
            this.canSeeCurrent = false;
            if (this.haloNote != null && this.haloNote.getInternalClock() > 0.0F) {
                this.haloNote.updateInternalTickClock();
            }

            this.legsSprite.PlayAnim("ZombieWalk1");
            this.chatElement.update();
            this.speaking = this.chatElement.IsSpeaking();
            if (!this.speaking || this.isDead()) {
                this.speaking = false;
                this.callOut = false;
            }
        }
    }

    @Override
    public void renderlast() {
        super.renderlast();
        int playerIndex = IsoCamera.frameState.playerIndex;
        float renderX = this.getX();
        float renderY = this.getY();
        if (this.sx == 0.0F && this.def != null) {
            this.sx = IsoUtils.XToScreen(renderX + this.def.offX, renderY + this.def.offY, this.getZ() + this.def.offZ, 0);
            this.sy = IsoUtils.YToScreen(renderX + this.def.offX, renderY + this.def.offY, this.getZ() + this.def.offZ, 0);
            this.sx = this.sx - (this.offsetX - 8.0F);
            this.sy = this.sy - (this.offsetY - 60.0F);
        }

        if (this.hasInitTextObjects && this.isoPlayer != null || this.chatElement.getHasChatToDisplay()) {
            float sx = IsoUtils.XToScreen(renderX, renderY, this.getZ(), 0);
            float sy = IsoUtils.YToScreen(renderX, renderY, this.getZ(), 0);
            sx = sx - IsoCamera.getOffX() - this.offsetX;
            sy = sy - IsoCamera.getOffY() - this.offsetY;
            sy -= 128 / (2 / Core.tileScale);
            float zoom = Core.getInstance().getZoom(playerIndex);
            sx /= zoom;
            sy /= zoom;
            sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX;
            sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY;
            this.canSeeCurrent = true;
            this.drawUserName = false;
            if (this.isoPlayer != null
                    && (this == IsoCamera.frameState.camCharacter || this.getCurrentSquare() != null && this.getCurrentSquare().getCanSee(playerIndex))
                || IsoPlayer.getInstance().canSeeAll()) {
                if (this == IsoPlayer.getInstance()) {
                    this.canSeeCurrent = true;
                }

                if (GameClient.client && this.userName != null && !(this instanceof IsoAnimal)) {
                    this.drawUserName = false;
                    if (ServerOptions.getInstance().mouseOverToSeeDisplayName.getValue()
                        && this != IsoPlayer.getInstance()
                        && !IsoPlayer.getInstance().canSeeAll()) {
                        IsoObjectPicker.ClickObject object = IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA());
                        if (object != null && object.tile != null) {
                            for (int x = object.tile.square.getX() - 1; x < object.tile.square.getX() + 2; x++) {
                                for (int y = object.tile.square.getY() - 1; y < object.tile.square.getY() + 2; y++) {
                                    IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, object.tile.square.getZ());
                                    if (sq != null) {
                                        for (int i = 0; i < sq.getMovingObjects().size(); i++) {
                                            IsoMovingObject obj = sq.getMovingObjects().get(i);
                                            if (this == obj && obj instanceof IsoPlayer isoPlayer && isoPlayer.getTargetAlpha(isoPlayer.playerIndex) == 1.0F) {
                                                this.drawUserName = true;
                                                break;
                                            }
                                        }

                                        if (this.drawUserName) {
                                            break;
                                        }
                                    }

                                    if (this.drawUserName) {
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        this.drawUserName = true;
                    }

                    if (this.drawUserName) {
                        this.updateUserName();
                    }
                }

                if (!GameClient.client && this.isoPlayer != null && !this.isAnimal() && this.isoPlayer.getVehicle() == null) {
                    String nameStr = "";
                    BaseVehicle vehicle = this.isoPlayer.getNearVehicle();
                    if (this.getVehicle() == null
                        && vehicle != null
                        && vehicle.getPartById("Engine") != null
                        && (
                            this.isoPlayer.getInventory().haveThisKeyId(vehicle.getKeyId()) != null
                                || vehicle.isHotwired()
                                || SandboxOptions.getInstance().vehicleEasyUse.getValue()
                        )
                        && UIManager.visibleAllUi) {
                        Color newC = Color.HSBtoRGB(vehicle.colorHue, vehicle.colorSaturation * 0.5F, vehicle.colorValue, IsoGameCharacter.L_renderLast.color);
                        nameStr = " [img=media/ui/CarKey.png," + newC.getRedByte() + "," + newC.getGreenByte() + "," + newC.getBlueByte() + "]";
                    }

                    if (!nameStr.isEmpty()) {
                        this.userName.ReadString(nameStr);
                        this.drawUserName = true;
                    }
                }
            }

            if (this.isoPlayer != null && this.hasInitTextObjects && (this.playerIsSelf() || this.canSeeCurrent)) {
                if (this.canSeeCurrent && this.drawUserName) {
                    sy -= this.userName.getHeight();
                    this.userName.AddBatchedDraw((int)sx, (int)sy, true);
                }

                if (this.playerIsSelf()) {
                    ActionProgressBar bar = UIManager.getProgressBar(playerIndex);
                    if (bar != null && bar.isVisible()) {
                        sy -= bar.getHeight().intValue() + 2;
                    }
                }

                if (this.playerIsSelf() && this.haloNote != null && this.haloNote.getInternalClock() > 0.0F) {
                    float alp = this.haloNote.getInternalClock() / (this.haloDispTime / 4.0F);
                    alp = PZMath.min(alp, 1.0F);
                    sy -= this.haloNote.getHeight() + 2;
                    this.haloNote.AddBatchedDraw((int)sx, (int)sy, true, alp);
                }
            }

            boolean ignoreRadioLines = false;
            if (IsoPlayer.getInstance() != this
                && this.equipedRadio != null
                && this.equipedRadio.getDeviceData() != null
                && this.equipedRadio.getDeviceData().getHeadphoneType() >= 0) {
                ignoreRadioLines = true;
            }

            if (this.equipedRadio != null && this.equipedRadio.getDeviceData() != null && !this.equipedRadio.getDeviceData().getIsTurnedOn()) {
                ignoreRadioLines = true;
            }

            boolean bViewerIsAdmin = GameClient.client
                && IsoCamera.getCameraCharacter() instanceof IsoPlayer player
                && player.role.hasCapability(Capability.CanSeePlayersStats);
            if (!this.isInvisible() || this == IsoCamera.frameState.camCharacter || bViewerIsAdmin) {
                this.chatElement.renderBatched(IsoPlayer.getPlayerIndex(), (int)sx, (int)sy, ignoreRadioLines);
            }
        }

        if (this instanceof IsoPlayer) {
            IsoBulletTracerEffects.getInstance().render();
        }

        if (this.inventory != null) {
            for (int n = 0; n < this.inventory.items.size(); n++) {
                InventoryItem item = this.inventory.items.get(n);
                if (item instanceof IUpdater iUpdater) {
                    iUpdater.renderlast();
                }
            }
        }

        if (this.getIsNPC() && this.ai.brain != null) {
            this.ai.brain.renderlast();
        }

        if (Core.debug) {
            this.debugRenderLast();
        }
    }

    private void debugRenderLast() {
        float maxRange = 2.0F;
        if (DebugOptions.instance.character.debug.render.angle.getValue() && this.hasActiveModel()) {
            Vector2 dir = tempo;
            dir.set(this.dir.ToVector());
            this.drawDirectionLine(dir, 2.4F, 0.0F, 1.0F, 0.0F);
            dir.setLengthAndDirection(this.getLookAngleRadians(), 1.0F);
            this.drawDirectionLine(dir, 2.0F, 1.0F, 1.0F, 1.0F);
            dir.setLengthAndDirection(this.getAnimAngleRadians(), 1.0F);
            this.drawDirectionLine(dir, 2.0F, 1.0F, 1.0F, 0.0F);
            float angle = this.getDirectionAngleRadians();
            dir.setLengthAndDirection(angle, 1.0F);
            this.drawDirectionLine(dir, 2.0F, 0.0F, 0.0F, 1.0F);
        }

        if (DebugOptions.instance.character.debug.render.deferredMovement.getValue() && this.hasActiveModel()) {
            Vector2 dir = tempo;
            this.getDeferredMovement(dir);
            this.drawDirectionLine(dir, 1000.0F * dir.getLength() / GameTime.instance.getMultiplier() * 2.0F, 1.0F, 0.5F, 0.5F);
        }

        if (DebugOptions.instance.character.debug.render.deferredMovement.getValue() && this.hasActiveModel()) {
            Vector2 dir = tempo;
            this.getDeferredMovementFromRagdoll(dir);
            this.drawDirectionLine(dir, 1000.0F * dir.getLength() / GameTime.instance.getMultiplier() * 2.0F, 0.0F, 1.0F, 0.5F);
        }

        if (DebugOptions.instance.character.debug.render.deferredAngles.getValue() && this.hasActiveModel()) {
            Vector2 dir = tempo;
            this.getDeferredMovement(dir);
            this.drawDirectionLine(dir, 1000.0F * dir.getLength() / GameTime.instance.getMultiplier() * 2.0F, 1.0F, 0.5F, 0.5F);
        }

        if (DebugOptions.instance.character.debug.render.aimCone.getValue()) {
            this.debugAim();
        }

        if (DebugOptions.instance.character.debug.render.testDotSide.getValue()) {
            this.debugTestDotSide();
        }

        if (DebugOptions.instance.character.debug.render.vision.getValue()) {
            this.debugVision();
        }

        if (DebugOptions.instance.character.debug.render.climbRope.getValue()) {
            ClimbSheetRopeState.instance().debug(this);
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().enable.getValue() || GameServer.server && GameServer.guiCommandline) {
            this.renderDebugData();
        }

        if (DebugOptions.instance.pathfindRenderPath.getValue() && this.pfb2 != null) {
            this.pfb2.render();
        }

        if (DebugOptions.instance.collideWithObstacles.render.radius.getValue()) {
            float radius = 0.3F;
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            if (!this.isCollidable()) {
                b = 0.0F;
            }

            if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                b = 0.5F;
                g = 0.5F;
                r = 0.5F;
            }

            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3F, 16, r, g, b, 1.0F);
        }

        if (DebugOptions.instance.animation.debug.getValue() && this.hasActiveModel() && !(this instanceof IsoAnimal)) {
            IndieGL.glBlendFunc(770, 771);
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            int sx = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            int sy = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ() + 1.0F, 0);
            TextManager.instance.DrawString(UIFont.Dialogue, sx, sy, 1.0, this.getAnimationDebug(), 1.0, 1.0, 1.0, 1.0);
        }

        if (DebugOptions.instance.statistics.displayAllDebugStatistics.getValue() && this instanceof IsoPlayer) {
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            int sx = (int)IsoUtils.XToScreenExact(this.getX() + 1.0F, this.getY(), this.getZ(), 0);
            int sy = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ() + 1.0F, 0);
            TextManager.instance.DrawString(UIFont.Dialogue, sx, sy, this.getStatisticsDebug(), 0.0, 1.0, 0.0, 1.0);
        }

        if (DebugOptions.instance.character.debug.render.carStopDebug.getValue() && this.hasActiveModel()) {
            Vector2 dir = tempo;
            if (this.vehicle != null && this.vbdebugHitTarget != null) {
                float maxAngle = 0.3F;
                float maxLength = 6.0F;
                Vector2 carSpeed = this.calcCarSpeedVector();
                boolean movingBackward = this.carMovingBackward(carSpeed);
                Vector2 offset = this.calcCarPositionOffset(movingBackward);
                Vector2 vectorCarToPlayer = this.calcCarToPlayerVector(this.vbdebugHitTarget, offset);
                carSpeed = this.calcCarSpeedVector(offset);
                float lengthMultiplier = this.calcLengthMultiplier(carSpeed, movingBackward);
                float angleMultiplier = this.calcConeAngleMultiplier(this.vbdebugHitTarget, movingBackward);
                float angleOffset = this.calcConeAngleOffset(this.vbdebugHitTarget, movingBackward);
                maxLength += lengthMultiplier;
                maxAngle *= angleMultiplier;
                float angle = carSpeed.getDirection();
                angle -= angleOffset;
                dir.setLengthAndDirection(angle, maxLength);
                Vector2 startPosition = new Vector2();
                startPosition.x = this.vehicle.getX() + offset.x;
                startPosition.y = this.vehicle.getY() + offset.y;
                this.drawLine(startPosition, dir, 2.0F, 1.0F, 0.0F, 1.0F);
                float length = (float)Math.cos(maxAngle) * maxLength;
                dir.setLengthAndDirection(angle + maxAngle, length);
                this.drawLine(startPosition, dir, 2.0F, 1.0F, 0.0F, 0.0F);
                dir.setLengthAndDirection(angle - maxAngle, length);
                this.drawLine(startPosition, dir, 2.0F, 1.0F, 0.0F, 0.0F);
                angle = vectorCarToPlayer.getDirection();
                dir.setLengthAndDirection(angle, vectorCarToPlayer.getLength() / 2.0F);
                this.drawLine(startPosition, dir, 2.0F, 0.0F, 0.0F, 1.0F);
            }
        }

        if (DebugOptions.instance.character.debug.render.aimVector.getValue() && this.ballisticsController != null) {
            this.ballisticsController.renderlast();
        }
    }

    public void drawLine(Vector2 startPos, Vector2 dir, float length, float r, float g, float b) {
        float x2 = startPos.x + dir.x * length;
        float y2 = startPos.y + dir.y * length;
        float sx = IsoUtils.XToScreenExact(startPos.x, startPos.y, this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(startPos.x, startPos.y, this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, this.getZ(), 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, this.getZ(), 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5F, 1);
    }

    public Vector2 calcCarForwardVector() {
        Vector3f forward = this.vehicle.getForwardVector(BaseVehicle.allocVector3f());
        Vector2 forward2 = new Vector2().set(forward.x, forward.z);
        BaseVehicle.releaseVector3f(forward);
        return forward2;
    }

    public boolean carMovingBackward(Vector2 carSpeed) {
        float sx = carSpeed.x;
        float sy = carSpeed.y;
        carSpeed.normalize();
        boolean bBackward = this.calcCarForwardVector().dot(carSpeed) < 0.0F;
        carSpeed.set(sx, sy);
        return bBackward;
    }

    public Vector2 calcCarPositionOffset(boolean movingBackward) {
        new Vector2();
        Vector2 offset;
        if (!movingBackward) {
            offset = this.calcCarForwardVector().setLength(1.0F);
            offset.x *= -1.0F;
            offset.y *= -1.0F;
        } else {
            offset = this.calcCarForwardVector().setLength(2.0F);
        }

        return offset;
    }

    public float calcLengthMultiplier(Vector2 carSpeed, boolean movingBackward) {
        float multiplier;
        if (movingBackward) {
            multiplier = carSpeed.getLength();
        } else {
            multiplier = carSpeed.getLength();
        }

        if (multiplier < 1.0F) {
            multiplier = 1.0F;
        }

        return multiplier;
    }

    public Vector2 calcCarSpeedVector(Vector2 offset) {
        Vector2 carSpeed = this.calcCarSpeedVector();
        carSpeed.x = carSpeed.x - offset.x;
        carSpeed.y = carSpeed.y - offset.y;
        return carSpeed;
    }

    public Vector2 calcCarSpeedVector() {
        Vector2 carSpeed = new Vector2();
        Vector3f linearVelocity = this.vehicle.getLinearVelocity(BaseVehicle.allocVector3f());
        carSpeed.x = linearVelocity.x;
        carSpeed.y = linearVelocity.z;
        BaseVehicle.releaseVector3f(linearVelocity);
        return carSpeed;
    }

    public Vector2 calcCarToPlayerVector(IsoGameCharacter target, Vector2 offset) {
        Vector2 vectorCarToPlayer = new Vector2();
        vectorCarToPlayer.x = target.getX() - this.vehicle.getX();
        vectorCarToPlayer.y = target.getY() - this.vehicle.getY();
        vectorCarToPlayer.x = vectorCarToPlayer.x - offset.x;
        vectorCarToPlayer.y = vectorCarToPlayer.y - offset.y;
        return vectorCarToPlayer;
    }

    public Vector2 calcCarToPlayerVector(IsoGameCharacter target) {
        Vector2 vectorCarToPlayer = new Vector2();
        vectorCarToPlayer.x = target.getX() - this.vehicle.getX();
        vectorCarToPlayer.y = target.getY() - this.vehicle.getY();
        return vectorCarToPlayer;
    }

    public float calcConeAngleOffset(IsoGameCharacter target, boolean movingBackward) {
        float angleOffset = 0.0F;
        if (movingBackward && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
            angleOffset = this.vehicle.getCurrentSteering() * 0.3F;
        }

        if (!movingBackward && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
            angleOffset = this.vehicle.getCurrentSteering() * 0.3F;
        }

        return angleOffset;
    }

    public float calcConeAngleMultiplier(IsoGameCharacter target, boolean movingBackward) {
        float angleMultiplier = 0.0F;
        if (movingBackward && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
            angleMultiplier = this.vehicle.getCurrentSteering() * 3.0F;
        }

        if (!movingBackward && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
            angleMultiplier = this.vehicle.getCurrentSteering() * 2.0F;
        }

        if (this.vehicle.getCurrentSteering() < 0.0F) {
            angleMultiplier *= -1.0F;
        }

        if (angleMultiplier < 1.0F) {
            angleMultiplier = 1.0F;
        }

        return angleMultiplier;
    }

    protected boolean renderTextureInsteadOfModel(float x, float y) {
        return false;
    }

    public void drawDirectionLine(Vector2 dir, float length, float r, float g, float b) {
        float x2 = this.getX() + dir.x * length;
        float y2 = this.getY() + dir.y * length;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, this.getZ(), 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, this.getZ(), 0);
        SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
        IndieGL.disableDepthTest();
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5F, 1);
    }

    public void drawDebugTextBelow(String text) {
        int boxWidth = TextManager.instance.MeasureStringX(UIFont.Small, text) + 32;
        int fontHeight = TextManager.instance.MeasureStringY(UIFont.Small, text);
        int boxHeight = (int)Math.ceil(fontHeight * 1.25);
        float sx = IsoUtils.XToScreenExact(this.getX() + 0.25F, this.getY() + 0.25F, this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX() + 0.25F, this.getY() + 0.25F, this.getZ(), 0);
        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
        SpriteRenderer.instance
            .renderi(null, (int)(sx - boxWidth / 2), (int)(sy - (boxHeight - fontHeight) / 2), boxWidth, boxHeight, 0.0F, 0.0F, 0.0F, 0.5F, null);
        TextManager.instance.DrawStringCentre(UIFont.Small, sx, sy, text, 1.0, 1.0, 1.0, 1.0);
        SpriteRenderer.instance.EndShader();
    }

    public Radio getEquipedRadio() {
        return this.equipedRadio;
    }

    private void radioEquipedCheck() {
        if (this.leftHandItem != this.leftHandCache) {
            this.leftHandCache = this.leftHandItem;
            if (this.leftHandItem != null && (this.equipedRadio == null || this.equipedRadio != this.rightHandItem) && this.leftHandItem instanceof Radio radio
                )
             {
                this.equipedRadio = radio;
            } else if (this.equipedRadio != null && this.equipedRadio != this.rightHandItem && this.equipedRadio != this.getClothingItem_Back()) {
                if (this.equipedRadio.getDeviceData() != null) {
                    this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
                }

                this.equipedRadio = null;
            }
        }

        if (this.rightHandItem != this.rightHandCache) {
            this.rightHandCache = this.rightHandItem;
            if (this.rightHandItem != null && this.rightHandItem instanceof Radio radio) {
                this.equipedRadio = radio;
            } else if (this.equipedRadio != null && this.equipedRadio != this.leftHandItem && this.equipedRadio != this.getClothingItem_Back()) {
                if (this.equipedRadio.getDeviceData() != null) {
                    this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
                }

                this.equipedRadio = null;
            }
        }

        if (this.getClothingItem_Back() != this.backCache) {
            this.backCache = this.getClothingItem_Back();
            if (this.getClothingItem_Back() != null && this.getClothingItem_Back() instanceof Radio) {
                this.equipedRadio = (Radio)this.getClothingItem_Back();
            } else if (this.equipedRadio != null && this.equipedRadio != this.leftHandItem && this.equipedRadio != this.rightHandItem) {
                if (this.equipedRadio.getDeviceData() != null) {
                    this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
                }

                this.equipedRadio = null;
            }
        }
    }

    private void debugAim() {
        if (this instanceof IsoPlayer player) {
            if (player.isAiming()) {
                HandWeapon weapon = Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
                if (weapon == null) {
                    weapon = player.bareHands;
                }

                float maxRange = weapon.getMaxRange(player) * weapon.getRangeMod(player);
                float minRange = weapon.getMinRange();
                float direction = this.getLookAngleRadians();
                IndieGL.disableDepthTest();
                IndieGL.StartShader(0);
                if (this.ballisticsController != null && weapon.isRanged()) {
                    int thickness = 1;
                    float targetRectAlpha = 1.0F;
                    Color targetRectColor = Color.magenta;
                    Vector3 firingStart = new Vector3();
                    Vector3 firingDirection = new Vector3();
                    Vector3 firingEnd = new Vector3();
                    this.ballisticsController.calculateMuzzlePosition(firingStart, firingDirection);
                    firingDirection.normalize();
                    firingEnd.set(
                        firingStart.x + firingDirection.x * maxRange,
                        firingStart.y + firingDirection.y * maxRange,
                        firingStart.z + firingDirection.z * maxRange
                    );
                    float height = -0.45F;
                    GeometryUtils.computePerpendicularVectors(firingDirection.x, firingDirection.y, firingDirection.z, tempVector3f00, tempVector3f01);
                    tempVector3f00.mul(CombatManager.getInstance().getCombatConfig().get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD));
                    float zHeight = -0.45F;

                    for (int i = 0; i < 4; i++) {
                        LineDrawer.DrawIsoLine(
                            firingStart.x + tempVector3f00.x,
                            firingStart.y + tempVector3f00.y,
                            firingStart.z + tempVector3f00.z - zHeight,
                            firingEnd.x + tempVector3f00.x,
                            firingEnd.y + tempVector3f00.y,
                            firingEnd.z + tempVector3f00.z - zHeight,
                            targetRectColor.r,
                            targetRectColor.g,
                            targetRectColor.b,
                            1.0F,
                            1
                        );
                        LineDrawer.DrawIsoLine(
                            firingStart.x - tempVector3f00.x,
                            firingStart.y - tempVector3f00.y,
                            firingStart.z - tempVector3f00.z - zHeight,
                            firingEnd.x - tempVector3f00.x,
                            firingEnd.y - tempVector3f00.y,
                            firingEnd.z - tempVector3f00.z - zHeight,
                            targetRectColor.r,
                            targetRectColor.g,
                            targetRectColor.b,
                            1.0F,
                            1
                        );
                        LineDrawer.DrawIsoLine(
                            firingStart.x - tempVector3f00.x,
                            firingStart.y - tempVector3f00.y,
                            firingStart.z - tempVector3f00.z - zHeight,
                            firingStart.x + tempVector3f00.x,
                            firingStart.y + tempVector3f00.y,
                            firingStart.z + tempVector3f00.z - zHeight,
                            targetRectColor.r,
                            targetRectColor.g,
                            targetRectColor.b,
                            1.0F,
                            1
                        );
                        LineDrawer.DrawIsoLine(
                            firingEnd.x - tempVector3f00.x,
                            firingEnd.y - tempVector3f00.y,
                            firingEnd.z - tempVector3f00.z - zHeight,
                            firingEnd.x + tempVector3f00.x,
                            firingEnd.y + tempVector3f00.y,
                            firingEnd.z + tempVector3f00.z - zHeight,
                            targetRectColor.r,
                            targetRectColor.g,
                            targetRectColor.b,
                            1.0F,
                            1
                        );
                        zHeight -= -0.29999998F;
                    }
                } else {
                    float minAngle = weapon.getMinAngle();
                    minAngle -= weapon.getAimingPerkMinAngleModifier() * (this.getPerkLevel(PerkFactory.Perks.Aiming) / 2.0F);
                    LineDrawer.drawDirectionLine(this.getX(), this.getY(), this.getZ(), maxRange, direction, 1.0F, 1.0F, 1.0F, 0.5F, 1);
                    LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), maxRange, direction, minAngle, 1.0F, 1.0F, 1.0F, 0.5F, 1);
                    LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), minRange, direction, minAngle, 6, 1.0F, 1.0F, 1.0F, 0.5F);
                    if (minRange != maxRange) {
                        LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), maxRange, direction, minAngle, 6, 1.0F, 1.0F, 1.0F, 0.5F);
                    }

                    LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), maxRange + 0.2F, direction, minAngle, 6, 0.75F, 0.75F, 0.75F, 0.5F);
                    float IGNORE_PRONE_RANGE = Core.getInstance().getIgnoreProneZombieRange();
                    if (IGNORE_PRONE_RANGE > 0.0F) {
                        LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), IGNORE_PRONE_RANGE, direction, 0.0F, 12, 0.0F, 0.0F, 1.0F, 0.25F);
                        LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), IGNORE_PRONE_RANGE, direction, 0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1);
                    }

                    if (this.attackVars.targetOnGround.getObject() != null) {
                        if (!this.attackVars.targetsProne.isEmpty()) {
                            HitInfo hitInfo = this.attackVars.targetsProne.get(0);
                            if (hitInfo != null) {
                                LineDrawer.DrawIsoCircle(hitInfo.x, hitInfo.y, hitInfo.z, 0.1F, 8, 1.0F, 1.0F, 0.0F, 1.0F);
                            }
                        }
                    } else if (!this.attackVars.targetsStanding.isEmpty()) {
                        HitInfo hitInfo = this.attackVars.targetsStanding.get(0);
                        if (hitInfo != null) {
                            LineDrawer.DrawIsoCircle(hitInfo.x, hitInfo.y, hitInfo.z, 0.1F, 8, 1.0F, 1.0F, 0.0F, 1.0F);
                        }
                    }

                    for (int i = 0; i < this.hitInfoList.size(); i++) {
                        HitInfo hitInfo = this.hitInfoList.get(i);
                        IsoMovingObject obj = hitInfo.getObject();
                        if (obj != null) {
                            int chance = hitInfo.chance;
                            float r = 1.0F - chance / 100.0F;
                            float g = 1.0F - r;
                            float scale = Math.max(0.2F, chance / 100.0F) / 2.0F;
                            float sx = IsoUtils.XToScreenExact(obj.getX() - scale, obj.getY() + scale, obj.getZ(), 0);
                            float sy = IsoUtils.YToScreenExact(obj.getX() - scale, obj.getY() + scale, obj.getZ(), 0);
                            float sx2 = IsoUtils.XToScreenExact(obj.getX() - scale, obj.getY() - scale, obj.getZ(), 0);
                            float sy2 = IsoUtils.YToScreenExact(obj.getX() - scale, obj.getY() - scale, obj.getZ(), 0);
                            float sx3 = IsoUtils.XToScreenExact(obj.getX() + scale, obj.getY() - scale, obj.getZ(), 0);
                            float sy3 = IsoUtils.YToScreenExact(obj.getX() + scale, obj.getY() - scale, obj.getZ(), 0);
                            float sx4 = IsoUtils.XToScreenExact(obj.getX() + scale, obj.getY() + scale, obj.getZ(), 0);
                            float sy4 = IsoUtils.YToScreenExact(obj.getX() + scale, obj.getY() + scale, obj.getZ(), 0);
                            SpriteRenderer.instance.renderPoly(sx, sy, sx2, sy2, sx3, sy3, sx4, sy4, r, g, 0.0F, 0.5F);
                            UIFont font = UIFont.Dialogue;
                            TextManager.instance.DrawStringCentre(font, sx4, sy4, String.valueOf(hitInfo.dot), 1.0, 1.0, 1.0, 1.0);
                            TextManager.instance
                                .DrawStringCentre(font, sx4, sy4 + TextManager.instance.getFontHeight(font), hitInfo.chance + "%", 1.0, 1.0, 1.0, 1.0);
                            r = 1.0F;
                            g = 1.0F;
                            float b = 1.0F;
                            float dist = PZMath.sqrt(hitInfo.distSq);
                            if (dist < weapon.getMinRange()) {
                                b = 0.0F;
                                r = 0.0F;
                            }

                            TextManager.instance.DrawStringCentre(font, sx4, sy4 + TextManager.instance.getFontHeight(font) * 2, "DIST: " + dist, r, g, b, 1.0);
                        }

                        if (hitInfo.window.getObject() != null) {
                            hitInfo.window.getObject().setHighlighted(true);
                        }
                    }
                }
            }
        }
    }

    private void debugTestDotSide() {
        if (this == IsoPlayer.getInstance()) {
            float direction = this.getLookAngleRadians();
            float radius = 2.0F;
            float dot = 0.7F;
            LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), 2.0F, direction, dot, 1.0F, 1.0F, 1.0F, 0.5F, 1);
            dot = -0.5F;
            LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), 2.0F, direction, dot, 1.0F, 1.0F, 1.0F, 0.5F, 1);
            LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), 2.0F, direction, -1.0F, 16, 1.0F, 1.0F, 1.0F, 0.5F);
            ArrayList<IsoZombie> zombies = this.getCell().getZombieList();

            for (int i = 0; i < zombies.size(); i++) {
                IsoMovingObject obj = zombies.get(i);
                if (this.DistToSquared(obj) < 4.0F) {
                    LineDrawer.DrawIsoCircle(obj.getX(), obj.getY(), obj.getZ(), 0.3F, 1.0F, 1.0F, 1.0F, 1.0F);
                    float scale = 0.2F;
                    float sx4 = IsoUtils.XToScreenExact(obj.getX() + 0.2F, obj.getY() + 0.2F, obj.getZ(), 0);
                    float sy4 = IsoUtils.YToScreenExact(obj.getX() + 0.2F, obj.getY() + 0.2F, obj.getZ(), 0);
                    UIFont font = UIFont.DebugConsole;
                    int fontHgt = TextManager.instance.getFontHeight(font);
                    TextManager.instance.DrawStringCentre(font, sx4, sy4 + fontHgt, "SIDE: " + this.testDotSide(obj), 1.0, 1.0, 1.0, 1.0);
                    Vector2 v1 = this.getLookVector(tempo2);
                    Vector2 v2 = tempo.set(obj.getX() - this.getX(), obj.getY() - this.getY());
                    v2.normalize();
                    float radians = PZMath.wrap(v2.getDirection() - v1.getDirection(), 0.0F, (float) (Math.PI * 2));
                    TextManager.instance.DrawStringCentre(font, sx4, sy4 + fontHgt * 2, "ANGLE (0-360): " + PZMath.radToDeg(radians), 1.0, 1.0, 1.0, 1.0);
                    radians = (float)Math.acos(this.getDotWithForwardDirection(obj.getX(), obj.getY()));
                    TextManager.instance.DrawStringCentre(font, sx4, sy4 + fontHgt * 3, "ANGLE (0-180): " + PZMath.radToDeg(radians), 1.0, 1.0, 1.0, 1.0);
                }
            }
        }
    }

    private void debugVision() {
        if (this == IsoPlayer.getInstance()) {
            float cone = LightingJNI.calculateVisionCone(this);
            LineDrawer.drawDotLines(
                this.getX(), this.getY(), this.getZ(), GameTime.getInstance().getViewDist(), this.getLookAngleRadians(), -cone, 1.0F, 1.0F, 1.0F, 0.5F, 1
            );
            LineDrawer.drawArc(
                this.getX(), this.getY(), this.getZ(), GameTime.getInstance().getViewDist(), this.getLookAngleRadians(), -cone, 16, 1.0F, 1.0F, 1.0F, 0.5F
            );
            float rearZombieDistance = LightingJNI.calculateRearZombieDistance(this);
            LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), rearZombieDistance, this.getLookAngleRadians(), -1.0F, 32, 1.0F, 1.0F, 1.0F, 0.5F);
        }
    }

    public void setDefaultState() {
        this.stateMachine.changeState(this.defaultState, null);
    }

    public void SetOnFire() {
        if (!this.onFire) {
            this.setOnFire(true);
            float SCL = Core.tileScale / 2.0F;
            this.AttachAnim(
                "Fire",
                "01",
                30,
                0.5F,
                (int)(-(this.offsetX + 1.0F * SCL)) + 8 - Rand.Next(16),
                (int)(-(this.offsetY + -89.0F * SCL)) + (int)((10 + Rand.Next(20)) * SCL),
                true,
                0,
                false,
                0.7F,
                IsoFireManager.FIRE_TINT_MOD
            );
            IsoFireManager.AddBurningCharacter(this);
            int partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            if (this instanceof IsoPlayer) {
                this.getBodyDamage().getBodyParts().get(partIndex).setBurned();
            }

            if (SCL == 2.0F) {
                int n = this.attachedAnimSprite.size() - 1;
                this.attachedAnimSprite.get(n).setScale(SCL, SCL);
            }

            if (!this.getEmitter().isPlaying("BurningFlesh")) {
                this.getEmitter().playSoundImpl("BurningFlesh", this);
            }
        }
    }

    @Override
    public void StopBurning() {
        if (this.onFire) {
            IsoFireManager.RemoveBurningCharacter(this);
            this.setOnFire(false);
            if (this.attachedAnimSprite != null) {
                this.attachedAnimSprite.clear();
            }

            this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
        }
    }

    public void SpreadFireMP() {
        if (this.onFire && GameServer.server && SandboxOptions.instance.fireSpread.getValue()) {
            IsoGridSquare sq = ServerMap.instance.getGridSquare(this.getXi(), this.getYi(), this.getZi());
            if (sq != null && !sq.getProperties().has(IsoFlagType.burning) && Rand.Next(Rand.AdjustForFramerate(3000)) < this.fireSpreadProbability) {
                IsoFireManager.StartFire(this.getCell(), sq, false, 80);
            }
        }
    }

    public void SpreadFire() {
        if (this.onFire && !GameServer.server && !GameClient.client && SandboxOptions.instance.fireSpread.getValue()) {
            if (this.square != null
                && !this.square.getProperties().has(IsoFlagType.burning)
                && Rand.Next(Rand.AdjustForFramerate(3000)) < this.fireSpreadProbability) {
                IsoFireManager.StartFire(this.getCell(), this.square, false, 80);
            }
        }
    }

    public void Throw(HandWeapon weapon) {
        if (this instanceof IsoPlayer && (((IsoPlayer)this).getJoypadBind() != -1 || this.attackTargetSquare == null)) {
            Vector2 throwVec = this.getForwardDirection(tempo);
            throwVec.setLength(weapon.getMaxRange());
            this.attackTargetSquare = this.getCell()
                .getGridSquare((double)(this.getX() + throwVec.getX()), (double)(this.getY() + throwVec.getY()), (double)this.getZ());
            if (this.attackTargetSquare == null) {
                this.attackTargetSquare = this.getCell().getGridSquare((double)(this.getX() + throwVec.getX()), (double)(this.getY() + throwVec.getY()), 0.0);
            }
        }

        weapon.setAttackTargetSquare(this.attackTargetSquare);
        float x = this.attackTargetSquare.getX() - this.getX();
        if (x > 0.0F) {
            if (this.attackTargetSquare.getX() - this.getX() > weapon.getMaxRange()) {
                x = weapon.getMaxRange();
            }
        } else if (this.attackTargetSquare.getX() - this.getX() < -weapon.getMaxRange()) {
            x = -weapon.getMaxRange();
        }

        float y = this.attackTargetSquare.getY() - this.getY();
        if (y > 0.0F) {
            if (this.attackTargetSquare.getY() - this.getY() > weapon.getMaxRange()) {
                y = weapon.getMaxRange();
            }
        } else if (this.attackTargetSquare.getY() - this.getY() < -weapon.getMaxRange()) {
            y = -weapon.getMaxRange();
        }

        String physicsObject = weapon.getPhysicsObject();
        if (physicsObject != null) {
            if (physicsObject.equals(ItemKey.Normal.BALL.toString())) {
                new IsoBall(this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6F, x * 0.4F, y * 0.4F, weapon, this);
            } else {
                new IsoMolotovCocktail(this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6F, x * 0.4F, y * 0.4F, weapon, this);
            }
        }

        if (!GameClient.client || this.isLocal()) {
            IsoGridSquare sq = this.getCurrentSquare();
            INetworkPacket.send(PacketTypes.PacketType.PlayerDropHeldItems, this, sq.x, sq.y, sq.z, false, true);
        }

        if (this instanceof IsoPlayer) {
            ((IsoPlayer)this).setAttackAnimThrowTimer(0L);
        }
    }

    public boolean helmetFall(boolean hitHead) {
        if (GameClient.client) {
            return false;
        } else if (GameServer.server) {
            return GameServer.helmetFall(this, hitHead);
        } else {
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            boolean removed = false;
            IsoZombie zombie = Type.tryCastTo(this, IsoZombie.class);
            if (zombie != null && !zombie.isUsingWornItems()) {
                this.getItemVisuals(tempItemVisuals);

                for (int i = 0; i < tempItemVisuals.size(); i++) {
                    ItemVisual itemVisual = tempItemVisuals.get(i);
                    Item scriptItem = itemVisual.getScriptItem();
                    if (scriptItem != null && scriptItem.isItemType(ItemType.CLOTHING) && scriptItem.getChanceToFall() > 0) {
                        int chanceToFall = scriptItem.getChanceToFall();
                        if (hitHead) {
                            chanceToFall += 40;
                        }

                        if (Rand.Next(100) > chanceToFall) {
                            InventoryItem item = InventoryItemFactory.CreateItem(scriptItem.getFullName());
                            if (item != null) {
                                if (item.getVisual() != null) {
                                    item.getVisual().copyFrom(itemVisual);
                                    item.synchWithVisual();
                                }

                                new IsoFallingClothing(
                                    this.getCell(),
                                    this.getX(),
                                    this.getY(),
                                    PZMath.min(this.getZ() + 0.4F, PZMath.fastfloor(this.getZ()) + 0.95F),
                                    0.2F,
                                    0.2F,
                                    item
                                );
                                tempItemVisuals.remove(i--);
                                zombie.itemVisuals.clear();
                                zombie.itemVisuals.addAll(tempItemVisuals);
                                this.resetModelNextFrame();
                                this.onWornItemsChanged();
                                removed = true;
                            }
                        }
                    }
                }
            } else if (this.getWornItems() != null && !this.getWornItems().isEmpty()) {
                for (int ix = 0; ix < this.getWornItems().size(); ix++) {
                    WornItem wornItem = this.getWornItems().get(ix);
                    InventoryItem item = wornItem.getItem();
                    if (item instanceof Clothing clothing) {
                        int chanceToFallx = clothing.getChanceToFall();
                        if (hitHead) {
                            chanceToFallx += 40;
                        }

                        if (clothing.getChanceToFall() > 0 && Rand.Next(100) <= chanceToFallx) {
                            new IsoFallingClothing(
                                this.getCell(),
                                this.getX(),
                                this.getY(),
                                PZMath.min(this.getZ() + 0.4F, this.getZ() + 0.95F),
                                Rand.Next(-0.2F, 0.2F),
                                Rand.Next(-0.2F, 0.2F),
                                item
                            );
                            this.getInventory().Remove(item);
                            this.getWornItems().remove(item);
                            this.resetModelNextFrame();
                            this.onWornItemsChanged();
                            removed = true;
                            if (GameClient.client && player != null && player.isLocalPlayer()) {
                                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, player);
                            }
                        }
                    }
                }
            }

            if (removed && player != null && player.isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }

            if (removed && this.isZombie()) {
                PersistentOutfits.instance.setFallenHat(this, true);
            }

            return removed;
        }
    }

    @Override
    public void smashCarWindow(VehiclePart part) {
        HashMap<Object, Object> StateMachineParams = this.getStateMachineParams(SmashWindowState.instance());
        StateMachineParams.clear();
        StateMachineParams.put(0, part.getWindow());
        StateMachineParams.put(1, part.getVehicle());
        StateMachineParams.put(2, part);
        this.actionContext.reportEvent("EventSmashWindow");
    }

    @Override
    public void smashWindow(IsoWindow w) {
        if (!w.isInvincible()) {
            HashMap<Object, Object> StateMachineParams = this.getStateMachineParams(SmashWindowState.instance());
            StateMachineParams.clear();
            StateMachineParams.put(0, w);
            this.actionContext.reportEvent("EventSmashWindow");
        }
    }

    @Override
    public void openWindow(IsoWindow w) {
        if (!w.isInvincible()) {
            OpenWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventOpenWindow");
        }
    }

    @Override
    public void closeWindow(IsoWindow w) {
        if (!w.isInvincible()) {
            HashMap<Object, Object> StateMachineParams = this.getStateMachineParams(CloseWindowState.instance());
            StateMachineParams.clear();
            StateMachineParams.put(0, w);
            this.actionContext.reportEvent("EventCloseWindow");
        }
    }

    @Override
    public void climbThroughWindow(IsoWindow w) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            float ox = this.getX() - PZMath.fastfloor(this.getX());
            float oy = this.getY() - PZMath.fastfloor(this.getY());
            int Xmodifier = 0;
            int Ymodifier = 0;
            if (w.getX() > this.getX() && !w.isNorth()) {
                Xmodifier = -1;
            }

            if (w.getY() > this.getY() && w.isNorth()) {
                Ymodifier = -1;
            }

            this.setX(w.getX() + ox + Xmodifier);
            this.setY(w.getY() + oy + Ymodifier);
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbThroughWindow(IsoWindow w, Integer startingFrame) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    public boolean isClosingWindow(IsoWindow window) {
        if (window == null) {
            return false;
        } else {
            return !this.isCurrentState(CloseWindowState.instance()) ? false : CloseWindowState.instance().getWindow(this) == window;
        }
    }

    public boolean isClimbingThroughWindow(IsoWindow window) {
        if (window == null) {
            return false;
        } else if (!this.isCurrentState(ClimbThroughWindowState.instance())) {
            return false;
        } else {
            return !this.getVariableBoolean("BlockWindow") ? false : ClimbThroughWindowState.instance().getWindow(this) == window;
        }
    }

    @Override
    public void climbThroughWindowFrame(IsoWindowFrame windowFrame) {
        if (windowFrame.canClimbThrough(this)) {
            this.dropHeavyItems();
            ClimbThroughWindowState.instance().setParams(this, windowFrame);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbSheetRope() {
        if (this.canClimbSheetRope(this.current)) {
            this.dropHeavyItems();
            HashMap<Object, Object> StateMachineParams = this.getStateMachineParams(ClimbSheetRopeState.instance());
            StateMachineParams.clear();
            this.actionContext.reportEvent("EventClimbRope");
        }
    }

    @Override
    public void climbDownSheetRope() {
        if (this.canClimbDownSheetRope(this.current)) {
            this.dropHeavyItems();
            HashMap<Object, Object> StateMachineParams = this.getStateMachineParams(ClimbDownSheetRopeState.instance());
            StateMachineParams.clear();
            this.actionContext.reportEvent("EventClimbDownRope");
        }
    }

    @Override
    public boolean canClimbSheetRope(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        } else {
            int startZ = sq.getZ();

            while (sq != null) {
                if (!IsoWindow.isSheetRopeHere(sq)) {
                    return false;
                }

                if (!IsoWindow.canClimbHere(sq)) {
                    return false;
                }

                if (sq.TreatAsSolidFloor() && sq.getZ() > startZ) {
                    return false;
                }

                if (IsoWindow.isTopOfSheetRopeHere(sq)) {
                    return true;
                }

                sq = this.getCell().getGridSquare((double)sq.getX(), (double)sq.getY(), (double)(sq.getZ() + 1.0F));
            }

            return false;
        }
    }

    @Override
    public boolean canClimbDownSheetRopeInCurrentSquare() {
        return this.canClimbDownSheetRope(this.current);
    }

    @Override
    public boolean canClimbDownSheetRope(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        } else {
            int startZ = sq.getZ();

            while (sq != null) {
                if (!IsoWindow.isSheetRopeHere(sq)) {
                    return false;
                }

                if (!IsoWindow.canClimbHere(sq)) {
                    return false;
                }

                if (sq.TreatAsSolidFloor()) {
                    return sq.getZ() < startZ;
                }

                sq = this.getCell().getGridSquare((double)sq.getX(), (double)sq.getY(), (double)(sq.getZ() - 1.0F));
            }

            return false;
        }
    }

    @Override
    public void climbThroughWindow(IsoThumpable w) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            float ox = this.getX() - PZMath.fastfloor(this.getX());
            float oy = this.getY() - PZMath.fastfloor(this.getY());
            int Xmodifier = 0;
            int Ymodifier = 0;
            if (w.getX() > this.getX() && !w.north) {
                Xmodifier = -1;
            }

            if (w.getY() > this.getY() && w.north) {
                Ymodifier = -1;
            }

            this.setX(w.getX() + ox + Xmodifier);
            this.setY(w.getY() + oy + Ymodifier);
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbThroughWindow(IsoThumpable w, Integer startingFrame) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbOverFence(IsoDirections dir) {
        if (this.current != null) {
            IsoGridSquare oppositeSq = this.current.getAdjacentSquare(dir);
            if (IsoWindow.canClimbThroughHelper(this, this.current, oppositeSq, dir == IsoDirections.N || dir == IsoDirections.S)) {
                if (this.current.isPlayerAbleToHopWallTo(dir, oppositeSq)) {
                    BentFences.getInstance().checkDamageHoppableFence(this, this.current, oppositeSq);
                    ClimbOverFenceState.instance().setParams(this, dir);
                    this.actionContext.reportEvent("EventClimbFence");
                }
            }
        }
    }

    @Override
    public boolean isAboveTopOfStairs() {
        if (this.getZ() != 0.0F && !(this.getZ() - PZMath.fastfloor(this.getZ()) > 0.01F) && (this.current == null || !this.current.TreatAsSolidFloor())) {
            IsoGridSquare sq = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)(this.getZ() - 1.0F));
            return sq != null && (sq.has(IsoObjectType.stairsTN) || sq.has(IsoObjectType.stairsTW));
        } else {
            return false;
        }
    }

    public void throwGrappledTargetOutWindow(IsoObject windowObject) {
        DebugType.Grapple.debugln("Attempting to throw out window: %s", windowObject);
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw out the window, windowObject:%s", windowObject);
        } else if (this.getGrapplingTarget() instanceof IsoGameCharacter thrownCharacter) {
            ClimbThroughWindowPositioningParams params = ClimbThroughWindowPositioningParams.alloc();
            ClimbThroughWindowState.getClimbThroughWindowPositioningParams(this, windowObject, params);
            if (!params.canClimb) {
                DebugType.Grapple.error("Cannot climb through, cannot throw out through.");
                params.release();
            } else {
                this.setDoGrapple(false);
                this.setDoGrappleLetGo();
                this.setDir(params.climbDir.Rot180());
                ClimbThroughWindowState.slideCharacterToWindowOpening(this, params);
                GrappledThrownOutWindowState.instance().setParams(thrownCharacter, params.windowObject);
                this.actionContext.reportEvent("GrappleThrowOutWindow");
                this.setGrappleThrowOutWindow(true);
                params.release();
            }
        }
    }

    public void throwGrappledOverFence(IsoObject hoppableObject, IsoDirections dir) {
        DebugType.Grapple.debugln("Attempting to throw over fence: %s", hoppableObject);
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw over fence: %s", hoppableObject);
        } else if (this.getGrapplingTarget() instanceof IsoGameCharacter thrownCharacter) {
            if (this.current != null) {
                IsoGridSquare oppositeSq = this.current.getAdjacentSquare(dir);
                if (IsoWindow.canClimbThroughHelper(this, this.current, oppositeSq, dir == IsoDirections.N || dir == IsoDirections.S)) {
                    this.setDoGrapple(false);
                    this.setDoGrappleLetGo();
                    this.setDir(dir);
                    GrappledThrownOverFenceState.instance().setParams(thrownCharacter, dir);
                    this.actionContext.reportEvent("GrappleThrowOverFence");
                    this.setGrappleThrowOverFence(true);
                }
            }
        }
    }

    public void throwGrappledIntoInventory(ItemContainer in_targetContainer) {
        DebugType.Grapple.debugln("Attempting to throw into inventory: %s", in_targetContainer);
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw into inventory: %s", in_targetContainer);
        } else if (this.getGrapplingTarget() instanceof IsoGameCharacter thrownCharacter) {
            if (this.current != null) {
                try (AutoCloseablePool pool = AutoCloseablePool.alloc()) {
                    Vector2 containerPos = in_targetContainer.getWorldPosition(pool.allocVector2());
                    Vector2 pos = this.getPosition(pool.allocVector2());
                    Vector2 forwardDir = pool.allocVector2();
                    forwardDir.set(containerPos.x - pos.x, containerPos.y - pos.y);
                    if (this.isAnimatingBackwards()) {
                        forwardDir.scale(-1.0F);
                    }

                    this.setForwardDirection(forwardDir);
                }

                this.setDoGrapple(false);
                this.setDoGrappleLetGo();
                GrappledThrownIntoContainerState.instance().setParams(thrownCharacter, in_targetContainer);
                this.actionContext.reportEvent("GrappleThrowIntoContainer");
                this.setGrappleThrowIntoContainer(true);
            }
        }
    }

    public void pickUpCorpseItem(InventoryItem item) {
        if (this.isGrappling()) {
            DebugType.Grapple.warn("Cannot pick up a corpse when already grappling.");
        } else {
            DebugType.Grapple.debugln("Attempting to grab corpse item: %s", item);
            IsoGridSquare square = this.getCurrentSquare();
            IsoDeadBody deadBody = square.tryAddCorpseToWorld(item, this.getX() - square.x, this.getY() - square.y, false);
            if (deadBody == null) {
                DebugType.Grapple.warn("Failed to spawn IsoDeadBody from item: %s", item);
            } else {
                deadBody.setFallOnFront(false);
                this.pickUpCorpse(deadBody, "PickUpCorpseItem");
                if (!this.isGrappling()) {
                    deadBody.setDoRender(true);
                }
            }
        }
    }

    public void pickUpCorpse(IsoDeadBody in_body, String in_dragType) {
        DebugType.Grapple.debugln("Attempting to pick up corpse: %s", in_body);
        if (in_body == null) {
            DebugType.Grapple.error("Body is null.");
        } else {
            this.setDoGrapple(true);
            float grappleEffectiveness = this.calculateGrappleEffectivenessFromTraits();
            in_body.Grappled(this, this.getAttackingWeapon(), grappleEffectiveness, in_dragType);
            this.setDoGrapple(false);
        }
    }

    public float calculateGrappleEffectivenessFromTraits() {
        float grappleEffectiveness = 1.0F;
        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            grappleEffectiveness *= 0.8F;
        }

        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            grappleEffectiveness *= 0.6F;
        }

        if (this.characterTraits.get(CharacterTrait.EMACIATED)) {
            grappleEffectiveness *= 0.4F;
        }

        if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            grappleEffectiveness *= 1.1F;
        }

        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            grappleEffectiveness *= 1.05F;
        }

        if (this.characterTraits.get(CharacterTrait.STRONG)) {
            grappleEffectiveness *= 1.25F;
        }

        if (this.characterTraits.get(CharacterTrait.ATHLETIC)) {
            grappleEffectiveness *= 1.25F;
        }

        if (this.characterTraits.get(CharacterTrait.BRAVE)) {
            grappleEffectiveness *= 1.1F;
        }

        if (this.characterTraits.get(CharacterTrait.COWARDLY)) {
            grappleEffectiveness *= 0.9F;
        }

        if (this.characterTraits.get(CharacterTrait.SPEED_DEMON)) {
            grappleEffectiveness *= 1.15F;
        }

        return grappleEffectiveness;
    }

    @Override
    public void preupdate() {
        super.preupdate();
        this.updateAnimationTimeDelta();
        if (!this.debugVariablesRegistered && DebugOptions.instance.character.debug.registerDebugVariables.getValue()) {
            this.registerDebugGameVariables();
        }

        this.updateAnimationRecorderState();
        if (this.animationRecorder != null && (this.animationRecorder.isRecording() || this.animationRecorder.hasActiveLine())) {
            int frameNo = IsoWorld.instance.getFrameNo();
            this.animationRecorder.newFrame(frameNo);
        }

        if (GameServer.server && this.handItemShouldSendToClients) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.Equip, this);
            this.handItemShouldSendToClients = false;
        }
    }

    private void updateAnimationTimeDelta() {
        this.animationTimeScale = 1.0F;
        this.animationUpdatingThisFrame = true;
        if (!(GameTime.getInstance().perObjectMultiplier > 1.0F) && DebugOptions.instance.zombieAnimationDelay.getValue()) {
            float maxAlpha = 0.0F;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null) {
                    maxAlpha = PZMath.max(maxAlpha, this.getAlpha(i));
                }
            }

            if (maxAlpha < 0.03F) {
                this.animationUpdatingThisFrame = this.animationInvisibleFrameDelay.update();
                if (this.animationUpdatingThisFrame) {
                    if (GameClient.client && this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
                        this.animationTimeScale = 1.0F;
                    } else {
                        this.animationTimeScale = this.animationInvisibleFrameDelay.delay + 1;
                    }
                }
            }
        }
    }

    public void updateHandEquips() {
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.Equip, this.getX(), this.getY(), this);
        } else {
            INetworkPacket.send(PacketTypes.PacketType.Equip, this);
        }

        this.handItemShouldSendToClients = false;
    }

    @Override
    public void update() {
        try (AbstractPerformanceProfileProbe ignored = this.updateInternal.profile()) {
            this.updateInternal();
        }
    }

    @Override
    public boolean isPushedByForSeparate(IsoMovingObject other) {
        if (other instanceof IGrappleable otherGrappleable) {
            if (this.isGrapplingTarget(otherGrappleable)) {
                return false;
            }

            if (this.isBeingGrappledBy(otherGrappleable)) {
                return false;
            }
        }

        return super.isPushedByForSeparate(other);
    }

    @Override
    public void setHitDir(Vector2 hitDir) {
        super.setHitDir(hitDir);
        this.setHitDirEnum(this.determineHitDirEnum(hitDir));
    }

    private void setHitDirEnum(String in_hitDirEnum) {
        this.hitDirEnum = in_hitDirEnum;
    }

    public String getHitDirEnum() {
        return this.hitDirEnum;
    }

    private String determineHitDirEnum(Vector2 in_hitDir) {
        Vector2 lookVector = this.getLookVector(IsoGameCharacter.l_testDotSide.v1);
        Vector2 hitDirNormalized = IsoGameCharacter.l_testDotSide.v3.set(in_hitDir);
        hitDirNormalized.normalize();
        float dotHitDirToForward = Vector2.dot(hitDirNormalized.x, hitDirNormalized.y, lookVector.x, lookVector.y);
        if (dotHitDirToForward < -0.5F) {
            return "FRONT";
        } else if (dotHitDirToForward > 0.5F) {
            return "BEHIND";
        } else {
            float crossZ = hitDirNormalized.x * lookVector.y - hitDirNormalized.y * lookVector.x;
            return crossZ > 0.0F ? "RIGHT" : "LEFT";
        }
    }

    private void updateInternal() {
        if (this.current != null) {
            this.updateAlpha();
            if (!this.isAnimal()) {
                this.updateBallisticsTarget();
            }

            if (this.isNpc) {
                this.ai.update();
            }

            if (this.sprite != null) {
                this.legsSprite = this.sprite;
            }

            if (this.isGrappling() && !this.isPerformingAnyGrappleAnimation() && !this.isInGrapplerState() && !GameClient.client && !GameServer.server) {
                this.LetGoOfGrappled("Aborted");
            }

            if (!this.isDead() || this.current != null && this.current.getMovingObjects().contains(this)) {
                this.checkSCBADrain();
                if (this.getBodyDamage() != null
                    && this.getCurrentBuilding() != null
                    && this.getCurrentBuilding().isToxic()
                    && !this.isProtectedFromToxic(true)) {
                    float fatigue = this.getStats().get(CharacterStat.FATIGUE);
                    float mult = GameTime.getInstance().getThirtyFPSMultiplier();
                    if (fatigue < 1.0F) {
                        this.getStats().add(CharacterStat.FATIGUE, 1.0E-4F * mult);
                    }

                    if (fatigue > 0.8F) {
                        this.getBodyDamage().getBodyPart(BodyPartType.Head).ReduceHealth(0.1F * mult);
                    }

                    this.getBodyDamage().getBodyPart(BodyPartType.Torso_Upper).ReduceHealth(0.1F * mult);
                }

                if (this.lungeFallTimer > 0.0F) {
                    this.lungeFallTimer = this.lungeFallTimer - GameTime.getInstance().getThirtyFPSMultiplier();
                }

                if (this.getMeleeDelay() > 0.0F) {
                    this.setMeleeDelay(this.getMeleeDelay() - 0.625F * GameTime.getInstance().getMultiplier());
                }

                if (this.getRecoilDelay() > 0.0F) {
                    this.setRecoilDelay(this.getRecoilDelay() - 0.625F * GameTime.getInstance().getMultiplier());
                }

                this.sx = 0.0F;
                this.sy = 0.0F;
                if (this.current.getRoom() != null
                    && this.current.getRoom().building.def.isAlarmed()
                    && (!this.isZombie() || Core.tutorial)
                    && !GameClient.client) {
                    boolean isInvisible = false;
                    if (this instanceof IsoPlayer && (this.isInvisible() || ((IsoPlayer)this).isGhostMode())) {
                        isInvisible = true;
                    }

                    if (!isInvisible && !this.isAnimal()) {
                        AmbientStreamManager.instance.doAlarm(this.current.getRoom().def);
                    }
                }

                if (!GameServer.server) {
                    this.updateSeenVisibility();
                }

                this.llx = this.getLastX();
                this.lly = this.getLastY();
                this.updateMovementStatistics();
                if (this.getClimbRopeTime() != 0.0F && !this.isClimbingRope()) {
                    this.setClimbRopeTime(0.0F);
                }

                this.setLastX(this.getX());
                this.setLastY(this.getY());
                this.setLastZ(this.getZ());
                this.updateBeardAndHair();
                this.updateFalling();
                if (this.descriptor != null) {
                    this.descriptor.setInstance(this);
                }

                boolean inVehicle = this.vehicle != null;
                if (GameClient.client && !this.isZombie() && this.moodles != null) {
                    this.moodles.Update();
                }

                if (!GameClient.client && !this.isZombie()) {
                    if (this.characterTraits.get(CharacterTrait.MOTION_SENSITIVE) && inVehicle && this.vehicle.getCurrentAbsoluteSpeedKmHour() > 10.0F) {
                        float motionSickness = 0.01F + (this.vehicle.getCurrentAbsoluteSpeedKmHour() - 10.0F) * 0.01F;
                        if (this.vehicle.skidding) {
                            motionSickness *= 2.0F;
                        }

                        if (this.vehicle.isDoingOffroad()) {
                            motionSickness *= 1.25F;
                        }

                        if (this.vehicle.isDriver(this)) {
                            motionSickness *= 0.5F;
                        }

                        if (this.isReading) {
                            motionSickness *= 2.0F;
                        }

                        if (this.characterTraits.get(CharacterTrait.IRON_GUT)) {
                            motionSickness *= 0.7F;
                        } else if (this.characterTraits.get(CharacterTrait.WEAK_STOMACH)) {
                            motionSickness *= 1.3F;
                        }

                        this.stats.add(CharacterStat.SICKNESS, motionSickness * GameTime.getInstance().getThirtyFPSMultiplier() * 0.01F);
                    } else if (!this.isPlayerMoving()) {
                        float motionSicknessRecovery = (float)ZomboidGlobals.sicknessDecrease;
                        if (this.sitOnGround || this.isSitOnFurniture || this.onBed) {
                            motionSicknessRecovery *= 1.5F;
                        }

                        if (this.isReading) {
                            motionSicknessRecovery *= 0.25F;
                        }

                        if (this.characterTraits.get(CharacterTrait.WEAK_STOMACH)) {
                            motionSicknessRecovery *= 0.7F;
                        } else if (this.characterTraits.get(CharacterTrait.IRON_GUT)) {
                            motionSicknessRecovery *= 1.3F;
                        }

                        if (this.stats.isAtMinimum(CharacterStat.POISON) && this.stats.isAboveMinimum(CharacterStat.SICKNESS)) {
                            this.stats.remove(CharacterStat.SICKNESS, motionSicknessRecovery * GameTime.getInstance().getMultiplier() * 0.01F);
                        }
                    }

                    if (this.characterTraits.get(CharacterTrait.AGORAPHOBIC) && !this.getCurrentSquare().isInARoom()) {
                        this.stats.add(CharacterStat.PANIC, 0.5F * GameTime.getInstance().getThirtyFPSMultiplier());
                    }

                    if (this.characterTraits.get(CharacterTrait.CLAUSTROPHOBIC) && (this.getCurrentSquare().isInARoom() || inVehicle)) {
                        int n = inVehicle ? 60 : this.getCurrentSquare().getRoomSize();
                        if (n > 0 && n < 70) {
                            float del = PZMath.max(0.0F, 1.0F - n / 70.0F);
                            float panicInc = 0.6F * del * GameTime.getInstance().getThirtyFPSMultiplier();
                            this.stats.add(CharacterStat.PANIC, panicInc);
                        }
                    }

                    if (this.getBodyDamage().getNumPartsBleeding() > 0) {
                        float del = (1.0F - this.getBodyDamage().getOverallBodyHealth() / 100.0F) * this.getBodyDamage().getNumPartsBleeding();
                        this.stats
                            .add(
                                CharacterStat.PANIC,
                                (this.characterTraits.get(CharacterTrait.HEMOPHOBIC) ? 0.4F : 0.2F) * del * GameTime.getInstance().getThirtyFPSMultiplier()
                            );
                    }

                    if (this.moodles != null) {
                        this.moodles.Update();
                    }

                    if (this.asleep) {
                        this.betaEffect = 0.0F;
                        this.sleepingTabletEffect = 0.0F;
                        this.StopAllActionQueue();
                    }

                    if (this.betaEffect > 0.0F) {
                        this.betaEffect = this.betaEffect - GameTime.getInstance().getThirtyFPSMultiplier();
                        this.stats.remove(CharacterStat.PANIC, 0.6F * GameTime.getInstance().getThirtyFPSMultiplier());
                    } else {
                        this.betaDelta = 0.0F;
                    }

                    if (this.depressFirstTakeTime > 0.0F || this.depressEffect > 0.0F) {
                        this.depressFirstTakeTime = this.depressFirstTakeTime - GameTime.getInstance().getThirtyFPSMultiplier();
                        if (this.depressFirstTakeTime < 0.0F) {
                            this.depressFirstTakeTime = -1.0F;
                            this.depressEffect = this.depressEffect - GameTime.getInstance().getThirtyFPSMultiplier();
                            this.stats.remove(CharacterStat.UNHAPPINESS, 0.03F * GameTime.getInstance().getThirtyFPSMultiplier());
                        }
                    }

                    if (this.depressEffect < 0.0F) {
                        this.depressEffect = 0.0F;
                    }

                    if (this.sleepingTabletEffect > 0.0F) {
                        this.sleepingTabletEffect = this.sleepingTabletEffect - GameTime.getInstance().getThirtyFPSMultiplier();
                        this.stats.add(CharacterStat.FATIGUE, 0.0016666667F * this.sleepingTabletDelta * GameTime.getInstance().getThirtyFPSMultiplier());
                    } else {
                        this.sleepingTabletDelta = 0.0F;
                    }

                    if (this.moodles != null) {
                        int panic = this.moodles.getMoodleLevel(MoodleType.PANIC);
                        if (panic == 2) {
                            this.stats.remove(CharacterStat.SANITY, 3.2E-7F);
                        } else if (panic == 3) {
                            this.stats.remove(CharacterStat.SANITY, 4.8000004E-7F);
                        } else if (panic == 4) {
                            this.stats.remove(CharacterStat.SANITY, 8.0E-7F);
                        } else if (panic == 0) {
                            this.stats.add(CharacterStat.SANITY, 1.0E-7F);
                        }

                        int fatiguex = this.moodles.getMoodleLevel(MoodleType.TIRED);
                        if (fatiguex == 4) {
                            this.stats.remove(CharacterStat.SANITY, 2.0E-6F);
                        }
                    }
                }

                if (!this.characterActions.isEmpty()) {
                    BaseAction act = this.characterActions.get(0);
                    boolean valid = act.valid();
                    if (valid && !act.started) {
                        act.waitToStart();
                    } else if (valid && !act.finished() && !act.forceComplete && !act.forceStop) {
                        act.update();
                    }

                    if (!valid || act.finished() || act.forceComplete || act.forceStop) {
                        if (act.finished() || act.forceComplete) {
                            act.perform();
                            if (!GameClient.client) {
                                act.complete();
                            }

                            valid = true;
                        }

                        if ((act.finished() || act.forceComplete) && !act.loopAction || act.forceStop || !valid) {
                            if (act.started && (act.forceStop || !valid)) {
                                act.stop();
                            }

                            this.characterActions.removeElement(act);
                            if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                                UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
                            }
                        }

                        act.forceComplete = false;
                    }

                    for (int a = 0; a < this.enemyList.size(); a++) {
                        IsoGameCharacter b = this.enemyList.get(a);
                        if (b.isDead()) {
                            this.enemyList.remove(b);
                            a--;
                        }
                    }
                }

                if (SystemDisabler.doCharacterStats && this.getBodyDamage() != null) {
                    this.getBodyDamage().Update();
                    this.updateBandages();
                }

                if (SystemDisabler.doCharacterStats) {
                    this.calculateStats();
                }

                if (this.asleep && this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
                    ((IsoPlayer)this).processWakingUp();
                }

                this.updateIdleSquareTime();
                this.moveForwardVec.x = 0.0F;
                this.moveForwardVec.y = 0.0F;
                if (!this.asleep || !(this instanceof IsoPlayer)) {
                    this.setLastX(this.getX());
                    this.setLastY(this.getY());
                    this.setLastZ(this.getZ());
                    this.square = this.getCurrentSquare();
                    if (this.sprite != null) {
                        if (!this.useParts) {
                            this.sprite.update(this.def);
                        } else {
                            this.legsSprite.update(this.def);
                        }
                    }

                    this.setStateEventDelayTimer(this.getStateEventDelayTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
                }

                this.stateMachine.update();
                if (this.isZombie() && VirtualZombieManager.instance.isReused((IsoZombie)this)) {
                    DebugLog.log(DebugType.Zombie, "Zombie added to ReusableZombies after stateMachine.update - RETURNING " + this);
                } else {
                    if (this instanceof IsoPlayer) {
                        this.ensureOnTile();
                    }

                    if ((this instanceof IsoPlayer || this instanceof IsoSurvivor)
                        && this.remoteId == -1
                        && this instanceof IsoPlayer
                        && ((IsoPlayer)this).isLocalPlayer()) {
                        RainManager.SetPlayerLocation(((IsoPlayer)this).getPlayerNum(), this.getCurrentSquare());
                    }

                    this.FireCheck();
                    this.SpreadFire();
                    this.ReduceHealthWhenBurning();
                    this.updateTextObjects();
                    if (this.stateMachine.getCurrent() == StaggerBackState.instance()) {
                        if (this.getStateEventDelayTimer() > 20.0F) {
                            this.bloodImpactX = this.getX();
                            this.bloodImpactY = this.getY();
                            this.bloodImpactZ = this.getZ();
                        }
                    } else {
                        this.bloodImpactX = this.getX();
                        this.bloodImpactY = this.getY();
                        this.bloodImpactZ = this.getZ();
                    }

                    if (!this.isZombie()) {
                        this.recursiveItemUpdater(this.inventory);
                    }

                    this.lastZombieKills = this.zombieKills;
                    if (this.attachedAnimSprite != null) {
                        int n = this.attachedAnimSprite.size();

                        for (int i = 0; i < n; i++) {
                            IsoSpriteInstance s = this.attachedAnimSprite.get(i);
                            IsoSprite sp = s.parentSprite;
                            s.update();
                            if (sp.hasAnimation()) {
                                s.frame = s.frame + s.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F);
                                if ((int)s.frame >= sp.currentAnim.frames.size() && sp.loop && s.looped) {
                                    s.frame = 0.0F;
                                }
                            }
                        }
                    }

                    if (this.isGodMod()) {
                        this.getStats().reset(CharacterStat.FATIGUE);
                        this.getStats().reset(CharacterStat.ENDURANCE);
                        this.getStats().reset(CharacterStat.TEMPERATURE);
                        this.getStats().reset(CharacterStat.HUNGER);
                        if (this instanceof IsoPlayer) {
                            ((IsoPlayer)this).resetSleepingPillsTaken();
                        }
                    }

                    this.updateMovementMomentum();
                    if (this.effectiveEdibleBuffTimer > 0.0F) {
                        this.effectiveEdibleBuffTimer = this.effectiveEdibleBuffTimer - GameTime.getInstance().getMultiplier() * 0.015F;
                        if (this.effectiveEdibleBuffTimer < 0.0F) {
                            this.effectiveEdibleBuffTimer = 0.0F;
                        }
                    }

                    if (!GameServer.server || GameClient.client || !GameClient.client) {
                        this.updateDirt();
                    }

                    if (this.useHandWeapon != null && this.useHandWeapon.isAimedFirearm() && this.isAiming()) {
                        if (this instanceof IsoPlayer && this.isLocal()) {
                            ((IsoPlayer)this).setAngleFromAim();
                        }

                        this.updateBallistics();
                    }
                }
            } else {
                if (GameServer.server) {
                    this.die();
                }
            }
        }
    }

    private boolean isInGrapplerState() {
        if (this.actionContext == null) {
            return false;
        } else {
            ActionState currentState = this.actionContext.getCurrentState();
            return currentState == null ? false : currentState.isGrapplerState();
        }
    }

    private void updateSeenVisibility() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            this.updateSeenVisibility(playerIndex);
        }
    }

    private void updateSeenVisibility(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null) {
            this.isVisibleToPlayer[playerIndex] = this.TestIfSeen(playerIndex, player);
            if (!this.isVisibleToPlayer[playerIndex]) {
                if (!(this instanceof IsoPlayer)) {
                    if (!player.isSeeEveryone()) {
                        this.setTargetAlpha(playerIndex, 0.0F);
                    }
                }
            }
        }
    }

    private void recursiveItemUpdater(ItemContainer container) {
        for (int m = 0; m < container.items.size(); m++) {
            InventoryItem item = container.items.get(m);
            if (item instanceof InventoryContainer inventoryContainer) {
                this.recursiveItemUpdater(inventoryContainer);
            }

            if (item instanceof IUpdater) {
                item.update();
            }
        }
    }

    private void recursiveItemUpdater(InventoryContainer container) {
        for (int m = 0; m < container.getInventory().getItems().size(); m++) {
            InventoryItem item = container.getInventory().getItems().get(m);
            if (item instanceof InventoryContainer inventoryContainer) {
                this.recursiveItemUpdater(inventoryContainer);
            }

            if (item instanceof IUpdater) {
                item.update();
            }
        }
    }

    private void updateDirt() {
        if (!this.isZombie() && this.getBodyDamage() != null) {
            int dirtNbr = 0;
            if (this.isRunning() && Rand.NextBool(Rand.AdjustForFramerate(3500))) {
                dirtNbr = 1;
            }

            if (this.isSprinting() && Rand.NextBool(Rand.AdjustForFramerate(2500))) {
                dirtNbr += Rand.Next(1, 3);
            }

            float temperature = this.stats.get(CharacterStat.TEMPERATURE);
            if (temperature > 37.0F && Rand.NextBool(Rand.AdjustForFramerate(5000))) {
                dirtNbr++;
            }

            if (temperature > 38.0F && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
                dirtNbr++;
            }

            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            if (player != null && player.isPlayerMoving() || player == null && this.isMoving()) {
                float puddle = this.square == null ? 0.0F : this.square.getPuddlesInGround();
                boolean bMuddyFloor = this.square != null
                    && this.isOutside()
                    && this.square.hasNaturalFloor()
                    && IsoPuddles.getInstance().getWetGroundFinalValue() > 0.5F;
                if (puddle > 0.09F && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
                    dirtNbr++;
                } else if (bMuddyFloor && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
                    dirtNbr++;
                }

                if (dirtNbr > 0) {
                    this.addDirt(null, dirtNbr, true);
                }

                dirtNbr = 0;
                if (puddle > 0.09F && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
                    dirtNbr++;
                } else if (bMuddyFloor && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
                    dirtNbr++;
                }

                if (this.isInTrees() && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
                    dirtNbr++;
                }

                if (dirtNbr > 0) {
                    this.addDirt(null, dirtNbr, false);
                }
            }
        }
    }

    protected void updateMovementMomentum() {
        float dt = GameTime.instance.getTimeDelta();
        if (this.isPlayerMoving() && !this.isAiming()) {
            float timeToFullMomentum = 0.55F;
            float time = this.momentumScalar * 0.55F;
            if (time >= 0.55F) {
                this.momentumScalar = 1.0F;
                return;
            }

            float newTime = time + dt;
            float alpha = newTime / 0.55F;
            this.momentumScalar = PZMath.clamp(alpha, 0.0F, 1.0F);
        } else {
            float timeToZeroMomentum = 0.25F;
            float time = (1.0F - this.momentumScalar) * 0.25F;
            if (time >= 0.25F) {
                this.momentumScalar = 0.0F;
                return;
            }

            float newTime = time + dt;
            float alpha = newTime / 0.25F;
            float clampedAlpha = PZMath.clamp(alpha, 0.0F, 1.0F);
            this.momentumScalar = 1.0F - clampedAlpha;
        }
    }

    @Override
    public double getHoursSurvived() {
        return GameTime.instance.getWorldAgeHours();
    }

    private void updateBeardAndHair() {
        if (!this.isZombie() && !this.isAnimal()) {
            if (!(this instanceof IsoPlayer) || ((IsoPlayer)this).isLocalPlayer()) {
                float hoursSurvived = (float)this.getHoursSurvived();
                if (this.beardGrowTiming < 0.0F || this.beardGrowTiming > hoursSurvived) {
                    this.beardGrowTiming = hoursSurvived;
                }

                if (this.hairGrowTiming < 0.0F || this.hairGrowTiming > hoursSurvived) {
                    this.hairGrowTiming = hoursSurvived;
                }

                boolean canSleep = !GameClient.client && !GameServer.server
                    || ServerOptions.instance.sleepAllowed.getValue() && ServerOptions.instance.sleepNeeded.getValue();
                boolean updated = false;
                if ((this.isAsleep() || !canSleep) && hoursSurvived - this.beardGrowTiming > 120.0F) {
                    this.beardGrowTiming = hoursSurvived;
                    BeardStyle currentStyle = BeardStyles.instance.FindStyle(((HumanVisual)this.getVisual()).getBeardModel());
                    int level = 1;
                    if (currentStyle != null) {
                        level = currentStyle.level;
                    }

                    ArrayList<BeardStyle> allStyles = BeardStyles.instance.getAllStyles();

                    for (int i = 0; i < allStyles.size(); i++) {
                        if (allStyles.get(i).growReference && allStyles.get(i).level == level + 1) {
                            ((HumanVisual)this.getVisual()).setBeardModel(allStyles.get(i).name);
                            updated = true;
                            break;
                        }
                    }
                }

                if ((this.isAsleep() || !canSleep) && hoursSurvived - this.hairGrowTiming > 480.0F) {
                    this.hairGrowTiming = hoursSurvived;
                    HairStyle currentStyle = HairStyles.instance.FindMaleStyle(((HumanVisual)this.getVisual()).getHairModel());
                    if (this.isFemale()) {
                        currentStyle = HairStyles.instance.FindFemaleStyle(((HumanVisual)this.getVisual()).getHairModel());
                    }

                    int level = 1;
                    if (currentStyle != null) {
                        level = currentStyle.level;
                    }

                    ArrayList<HairStyle> allStyles = HairStyles.instance.maleStyles;
                    if (this.isFemale()) {
                        allStyles = HairStyles.instance.femaleStyles;
                    }

                    for (int ix = 0; ix < allStyles.size(); ix++) {
                        HairStyle style = allStyles.get(ix);
                        if (style.growReference && style.level == level + 1) {
                            ((HumanVisual)this.getVisual()).setHairModel(style.name);
                            ((HumanVisual)this.getVisual()).setNonAttachedHair(null);
                            updated = true;
                            break;
                        }
                    }
                }

                if (updated) {
                    this.resetModelNextFrame();
                    LuaEventManager.triggerEvent("OnClothingUpdated", this);
                    if (GameClient.client) {
                        GameClient.instance.sendVisual((IsoPlayer)this);
                    }
                }
            }
        }
    }

    private void updateFalling() {
        if (this instanceof IsoPlayer && !this.isClimbing()) {
            IsoRoofFixer.FixRoofsAt(this.current);
        }

        if (!this.shouldBeFalling()) {
            this.setFallTime(0.0F);
            this.lastFallSpeed = 0.0F;
            this.falling = false;
            this.isOnGround = true;
        } else {
            float dt = GameTime.getInstance().getTimeDelta();
            if (GameServer.server) {
                dt *= 0.16F;
            }

            float fallSpeedDelta = 5.0010414F * dt;
            float fallSpeed = this.lastFallSpeed + fallSpeedDelta;
            float fallDelta = this.lastFallSpeed * dt + 2.5005207F * dt * dt;
            float newZ = this.getZ() - fallDelta;
            float heightAboveFloor = this.getHeightAboveFloor();
            float floorZ = this.getZ() - heightAboveFloor;
            if (newZ < floorZ) {
                this.setZ(floorZ);
                float impactFraction = heightAboveFloor / fallDelta;
                float impactSpeed = this.lastFallSpeed + fallSpeedDelta * impactFraction;
                this.fallTime = impactSpeed / 5.0010414F;
                this.DoLand(impactSpeed);
                this.setFallTime(0.0F);
                this.lastFallSpeed = 0.0F;
                this.falling = false;
                this.isOnGround = true;
            } else {
                this.setZ(newZ);
                this.fallTime = fallSpeed / 5.0010414F;
                this.lastFallSpeed = fallSpeed;
                this.falling = FallingConstants.isFall(fallSpeed);
                this.isOnGround = false;
            }

            this.llz = this.getLastZ();
        }
    }

    @Override
    protected void snapZToCurrentSquare() {
        if (this.isOnGround) {
            super.snapZToCurrentSquare();
        }
    }

    @Override
    protected void snapZToCurrentSquareExact() {
        if (this.isOnGround) {
            super.snapZToCurrentSquareExact();
        }
    }

    public boolean shouldBeFalling() {
        if (this instanceof IsoAnimal && ((IsoAnimal)this).isOnHook()) {
            return false;
        } else if (this.isSeatedInVehicle()) {
            return false;
        } else if (this.isClimbing()) {
            return false;
        } else if (this.isRagdollFall()) {
            return false;
        } else {
            return this.isCurrentState(ClimbOverFenceState.instance()) ? false : !this.isCurrentState(ClimbThroughWindowState.instance());
        }
    }

    public float getHeightAboveFloor() {
        if (this.current == null) {
            return 1.0F;
        } else {
            if (this.current.HasStairs()) {
                float apparentZ = this.current.getApparentZ(this.getX() - PZMath.fastfloor(this.getX()), this.getY() - PZMath.fastfloor(this.getY()));
                if (this.getZ() >= apparentZ) {
                    return this.getZ() - apparentZ;
                }
            }

            if (this.current.hasSlopedSurface()) {
                float apparentZ = this.current.getApparentZ(this.getX() - PZMath.fastfloor(this.getX()), this.getY() - PZMath.fastfloor(this.getY()));
                if (this.getZ() >= apparentZ) {
                    return this.getZ() - apparentZ;
                }
            }

            if (this.current.TreatAsSolidFloor()) {
                return this.getZ() - this.current.getZ();
            } else if (this.current.chunk == null) {
                return this.getZ();
            } else if (this.current.z == this.current.chunk.minLevel) {
                return this.getZ();
            } else {
                for (int i = this.current.z; i >= this.current.chunk.minLevel; i--) {
                    IsoGridSquare below = this.getCell().getGridSquare(this.current.x, this.current.y, i);
                    if (below != null) {
                        if (below.HasStairs()) {
                            float apparentZ = below.getApparentZ(this.getX() - PZMath.fastfloor(this.getX()), this.getY() - PZMath.fastfloor(this.getY()));
                            return this.getZ() - apparentZ;
                        }

                        if (below.hasSlopedSurface()) {
                            float apparentZ = below.getApparentZ(this.getX() - PZMath.fastfloor(this.getX()), this.getY() - PZMath.fastfloor(this.getY()));
                            return this.getZ() - apparentZ;
                        }

                        if (below.TreatAsSolidFloor()) {
                            return this.getZ() - below.getZ();
                        }
                    }
                }

                return 1.0F;
            }
        }
    }

    protected void updateMovementRates() {
    }

    protected float calculateIdleSpeed() {
        float result = 0.01F;
        return result + this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 2.5F / 10.0F;
    }

    public float calculateBaseSpeed() {
        float result = 0.8F;
        float bagRunSpeedModifier = 1.0F;
        if (this.getMoodles() != null) {
            result -= this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 0.15F;
            result -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 0.15F;
        }

        if (this.getMoodles().getMoodleLevel(MoodleType.PANIC) >= 3 && this.characterTraits.get(CharacterTrait.ADRENALINE_JUNKIE)) {
            int mul = this.getMoodles().getMoodleLevel(MoodleType.PANIC) + 1;
            result += mul / 20.0F;
        }

        for (int i = BodyPartType.ToIndex(BodyPartType.Torso_Upper); i < BodyPartType.ToIndex(BodyPartType.Neck) + 1; i++) {
            BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
            if (part.HasInjury()) {
                result -= 0.1F;
            }

            if (part.bandaged()) {
                result += 0.05F;
            }
        }

        BodyPart partx = this.getBodyDamage().getBodyPart(BodyPartType.UpperLeg_L);
        if (partx.getAdditionalPain(true) > 20.0F) {
            result -= (partx.getAdditionalPain(true) - 20.0F) / 100.0F;
        }

        for (int i = 0; i < this.bagsWorn.size(); i++) {
            InventoryContainer bag = this.bagsWorn.get(i);
            bagRunSpeedModifier += this.calcRunSpeedModByBag(bag);
        }

        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof InventoryContainer) {
            bagRunSpeedModifier += this.calcRunSpeedModByBag((InventoryContainer)this.getPrimaryHandItem());
        }

        if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem() instanceof InventoryContainer) {
            bagRunSpeedModifier += this.calcRunSpeedModByBag((InventoryContainer)this.getSecondaryHandItem());
        }

        if (this.isOutside()) {
            if (this.getCurrentSquare().hasNaturalFloor()) {
                result -= IsoPuddles.getInstance().getWetGroundFinalValue() * 0.25F;
            }

            if (this.getCurrentSquare().hasSand()) {
                result -= 0.05F;
            }
        }

        this.fullSpeedMod = this.runSpeedModifier + (bagRunSpeedModifier - 1.0F);
        return result * (1.0F - Math.abs(1.0F - this.fullSpeedMod) / 2.0F);
    }

    private float calcRunSpeedModByClothing() {
        float result = 0.0F;
        int count = 0;

        for (int i = 0; i < this.wornItems.size(); i++) {
            if (this.wornItems.getItemByIndex(i) instanceof Clothing clothing && clothing.getRunSpeedModifier() != 1.0F) {
                result += clothing.getRunSpeedModifier();
                count++;
            }
        }

        if (result == 0.0F && count == 0) {
            result = 1.0F;
            count = 1;
        }

        if (this.getWornItem(ItemBodyLocation.SHOES) == null) {
            result *= 0.8F;
        }

        return result / count;
    }

    private float calcRunSpeedModByBag(InventoryContainer bag) {
        float runBagMod = bag.getScriptItem().runSpeedModifier - 1.0F;
        float deltaWeight = bag.getContentsWeight() / bag.getEffectiveCapacity(this);
        return runBagMod * (1.0F + deltaWeight / 2.0F);
    }

    public float calculateCombatSpeed() {
        float combatSpeed = 0.8F;
        HandWeapon weapon = null;
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon) {
            weapon = (HandWeapon)this.getPrimaryHandItem();
            combatSpeed *= ((HandWeapon)this.getPrimaryHandItem()).getBaseSpeed();
        }

        WeaponType weaponType = WeaponType.getWeaponType(this);
        if (weapon != null && weapon.isTwoHandWeapon() && this.getSecondaryHandItem() != weapon) {
            combatSpeed *= 0.77F;
        }

        if (weapon != null && weapon.isOfWeaponCategory(WeaponCategory.AXE)) {
            combatSpeed *= this.getChopTreeSpeed();
        }

        combatSpeed -= this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 0.07F;
        combatSpeed -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 0.07F;
        combatSpeed += this.getWeaponLevel() * 0.03F;
        combatSpeed += this.getPerkLevel(PerkFactory.Perks.Fitness) * 0.02F;
        if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem() instanceof InventoryContainer) {
            combatSpeed *= 0.95F;
        }

        combatSpeed *= Rand.Next(1.1F, 1.2F);
        combatSpeed *= this.combatSpeedModifier;
        combatSpeed *= this.getArmsInjurySpeedModifier();
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            combatSpeed *= this.getBodyDamage().getThermoregulator().getCombatModifier();
        }

        combatSpeed = Math.min(1.6F, combatSpeed);
        combatSpeed = Math.max(0.8F, combatSpeed);
        if (weapon != null && weapon.isTwoHandWeapon() && weaponType == WeaponType.HEAVY) {
            combatSpeed *= 1.2F;
        }

        return combatSpeed;
    }

    private float getArmsInjurySpeedModifier() {
        float speed = 1.0F;
        float modifier = 0.0F;
        BodyPart bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.Hand_R);
        modifier = this.calculateInjurySpeed(bodyPart, true);
        if (modifier > 0.0F) {
            speed -= modifier;
        }

        bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.ForeArm_R);
        modifier = this.calculateInjurySpeed(bodyPart, true);
        if (modifier > 0.0F) {
            speed -= modifier;
        }

        bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.UpperArm_R);
        modifier = this.calculateInjurySpeed(bodyPart, true);
        if (modifier > 0.0F) {
            speed -= modifier;
        }

        return speed;
    }

    private float getFootInjurySpeedModifier() {
        float speed = 0.0F;
        boolean left = true;
        float leftInjuries = 0.0F;
        float rightInjuries = 0.0F;

        for (int i = BodyPartType.ToIndex(BodyPartType.Groin); i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            speed = this.calculateInjurySpeed(this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i)), false);
            if (left) {
                leftInjuries += speed;
            } else {
                rightInjuries += speed;
            }

            left = !left;
        }

        return leftInjuries > rightInjuries ? -(leftInjuries + rightInjuries) : leftInjuries + rightInjuries;
    }

    private float calculateInjurySpeed(BodyPart bodyPart, boolean doPain) {
        float scratchModifier = bodyPart.getScratchSpeedModifier();
        float cutModifier = bodyPart.getCutSpeedModifier();
        float burnModifier = bodyPart.getBurnSpeedModifier();
        float deepWoundModifier = bodyPart.getDeepWoundSpeedModifier();
        float modifier = 0.0F;
        if ((bodyPart.getType() == BodyPartType.Foot_L || bodyPart.getType() == BodyPartType.Foot_R)
            && (
                bodyPart.getBurnTime() > 5.0F
                    || bodyPart.getStitchTime() > 0.0F
                    || bodyPart.getBiteTime() > 0.0F
                    || bodyPart.deepWounded()
                    || bodyPart.isSplint()
                    || bodyPart.getFractureTime() > 0.0F
                    || bodyPart.haveGlass()
            )) {
            modifier = 1.0F;
            if (bodyPart.getStitchTime() > 0.0F) {
                modifier = 0.7F;
            }

            if (bodyPart.bandaged()) {
                modifier *= 0.7F;
            }

            if (bodyPart.getFractureTime() > 0.0F) {
                modifier = this.calcFractureInjurySpeed(bodyPart);
            }
        }

        if (bodyPart.haveBullet()) {
            return 1.0F;
        } else {
            if (bodyPart.getScratchTime() > 2.0F
                || bodyPart.getCutTime() > 5.0F
                || bodyPart.getBurnTime() > 0.0F
                || bodyPart.getDeepWoundTime() > 0.0F
                || bodyPart.isSplint()
                || bodyPart.getFractureTime() > 0.0F
                || bodyPart.getBiteTime() > 0.0F) {
                modifier += bodyPart.getScratchTime() / scratchModifier
                    + bodyPart.getCutTime() / cutModifier
                    + bodyPart.getBurnTime() / burnModifier
                    + bodyPart.getDeepWoundTime() / deepWoundModifier;
                modifier += bodyPart.getBiteTime() / 20.0F;
                if (bodyPart.bandaged()) {
                    modifier /= 2.0F;
                }

                if (bodyPart.getFractureTime() > 0.0F) {
                    modifier = this.calcFractureInjurySpeed(bodyPart);
                }
            }

            if (doPain && bodyPart.getPain() > 20.0F) {
                modifier += bodyPart.getPain() / 10.0F;
            }

            return modifier;
        }
    }

    private float calcFractureInjurySpeed(BodyPart bodyPart) {
        float result = 0.4F;
        if (bodyPart.getFractureTime() > 10.0F) {
            result = 0.7F;
        }

        if (bodyPart.getFractureTime() > 20.0F) {
            result = 1.0F;
        }

        if (bodyPart.getSplintFactor() > 0.0F) {
            result -= 0.2F;
            result -= Math.min(bodyPart.getSplintFactor() / 10.0F, 0.8F);
        }

        return Math.max(0.0F, result);
    }

    protected void calculateWalkSpeed() {
        if (!(this instanceof IsoPlayer) || !GameClient.client) {
            if (this instanceof IsoPlayer && !((IsoPlayer)this).getAttachedAnimals().isEmpty()) {
                float injurySpeed = this.getFootInjurySpeedModifier();
                this.setVariable("WalkInjury", injurySpeed);
                this.setVariable("WalkSpeed", 0.0F);
            } else {
                float sneakLimpSpeedScale = 1.0F;
                float walkSpeed = 0.0F;
                float injurySpeed = this.getFootInjurySpeedModifier();
                this.setVariable("WalkInjury", injurySpeed);
                walkSpeed = this.calculateBaseSpeed();
                if (!this.running && !this.sprinting) {
                    if (Math.abs(injurySpeed) > 0.1F) {
                        boolean bIsSneaking = this.isSneaking();
                        boolean bNearWallCrouching = this.getVariable("nearWallCrouching").getValueBool();
                        if (bIsSneaking) {
                            if (bNearWallCrouching) {
                                sneakLimpSpeedScale = 0.45F;
                            } else {
                                sneakLimpSpeedScale = 0.6F;
                            }
                        }
                    } else {
                        walkSpeed *= this.walkSpeedModifier;
                    }
                } else {
                    walkSpeed -= 0.15F;
                    walkSpeed *= this.fullSpeedMod;
                    walkSpeed += this.getPerkLevel(PerkFactory.Perks.Sprinting) / 20.0F;
                    walkSpeed -= Math.abs(injurySpeed / 1.5F);
                    if ("Tutorial".equals(Core.gameMode)) {
                        walkSpeed = Math.max(1.0F, walkSpeed);
                    }
                }

                this.setSneakLimpSpeedScale(sneakLimpSpeedScale);
                if (this.getSlowFactor() > 0.0F) {
                    walkSpeed *= 0.05F;
                }

                walkSpeed = Math.min(1.0F, walkSpeed);
                if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
                    walkSpeed *= this.getBodyDamage().getThermoregulator().getMovementModifier();
                }

                if (this.isAiming()) {
                    float strafeSpeed = Math.min(0.9F + this.getPerkLevel(PerkFactory.Perks.Nimble) / 10.0F, 1.5F);
                    float walkSpeedModifier = Math.min(walkSpeed * 2.5F, 1.0F);
                    strafeSpeed *= walkSpeedModifier;
                    strafeSpeed = Math.max(strafeSpeed, 0.48F);
                    this.setVariable("StrafeSpeed", strafeSpeed * 0.8F);
                }

                if (this.isInTreesNoBush()) {
                    IsoGridSquare currentSquare = this.getCurrentSquare();
                    if (currentSquare != null && currentSquare.has(IsoObjectType.tree)) {
                        IsoTree tree = currentSquare.getTree();
                        if (tree != null) {
                            walkSpeed *= tree.getSlowFactor(this);
                        }
                    }
                }

                this.setVariable("WalkSpeed", walkSpeed);
            }
        }
    }

    public void updateSpeedModifiers() {
        this.runSpeedModifier = 1.0F;
        this.walkSpeedModifier = 1.0F;
        this.combatSpeedModifier = 1.0F;
        this.bagsWorn.clear();

        for (int i = 0; i < this.getWornItems().size(); i++) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (item instanceof Clothing clothing) {
                this.combatSpeedModifier = this.combatSpeedModifier + (clothing.getCombatSpeedModifier() - 1.0F);
            }

            if (item instanceof InventoryContainer bag) {
                this.combatSpeedModifier = this.combatSpeedModifier + (bag.getScriptItem().combatSpeedModifier - 1.0F);
                this.bagsWorn.add(bag);
            }
        }

        InventoryItem itemx = this.getWornItems().getItem(ItemBodyLocation.SHOES);
        if (itemx == null || itemx.getCondition() == 0) {
            this.runSpeedModifier *= 0.85F;
            this.walkSpeedModifier *= 0.85F;
        }
    }

    public void updateDiscomfortModifiers() {
        this.clothingDiscomfortModifier = 0.0F;

        for (int i = 0; i < this.getWornItems().size(); i++) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (item instanceof Clothing clothing) {
                this.clothingDiscomfortModifier = this.clothingDiscomfortModifier + clothing.getDiscomfortModifier();
            }

            if (item instanceof InventoryContainer bag) {
                this.clothingDiscomfortModifier = this.clothingDiscomfortModifier + bag.getScriptItem().discomfortModifier;
            }
        }

        this.clothingDiscomfortModifier = Math.max(this.clothingDiscomfortModifier, 0.0F);
    }

    public void DoFloorSplat(IsoGridSquare sq, String id, boolean bFlip, float offZ, float alpha) {
        if (sq != null) {
            sq.DirtySlice();
            IsoObject best = null;

            for (int i = 0; i < sq.getObjects().size(); i++) {
                IsoObject obj = sq.getObjects().get(i);
                if (obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.solidfloor) && best == null) {
                    best = obj;
                }
            }

            if (best != null
                && best.sprite != null
                && (best.sprite.getProperties().has(IsoFlagType.vegitation) || best.sprite.getProperties().has(IsoFlagType.solidfloor))) {
                IsoSprite spr1 = IsoSprite.getSprite(IsoSpriteManager.instance, id, 0);
                if (spr1 == null) {
                    return;
                }

                if (best.attachedAnimSprite.size() > 7) {
                    return;
                }

                IsoSpriteInstance spr = IsoSpriteInstance.get(spr1);
                best.attachedAnimSprite.add(spr);
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).flip = bFlip;
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).tintr = 0.5F + Rand.Next(100) / 2000.0F;
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).tintg = 0.7F + Rand.Next(300) / 1000.0F;
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).tintb = 0.7F + Rand.Next(300) / 1000.0F;
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).SetAlpha(0.4F * alpha * 0.6F);
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).SetTargetAlpha(0.4F * alpha * 0.6F);
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).offZ = -offZ;
                best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).offX = 0.0F;
            }
        }
    }

    void DoSplat(IsoGridSquare sq, String id, boolean bFlip, IsoFlagType prop, float offX, float offZ, float alpha) {
        if (sq != null) {
            sq.DoSplat(id, bFlip, prop, offX, offZ, alpha);
        }
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        if (IsoCamera.getCameraCharacter() != IsoPlayer.getInstance() && Core.debug) {
            IsoCamera.setCameraCharacter(this);
        }

        return super.onMouseLeftClick(x, y);
    }

    protected void calculateStats() {
        if (!this.isAnimal()) {
            if (GameServer.server && (!ServerOptions.instance.sleepAllowed.getValue() || !ServerOptions.instance.sleepNeeded.getValue())) {
                this.stats.reset(CharacterStat.FATIGUE);
            }

            if (!LuaHookManager.TriggerHook("CalculateStats", this)) {
                this.updateEndurance();
                this.updateTripping();
                this.updateThirst();
                this.updateStress();
                this.updateStats_WakeState();
                this.updateMorale();
                this.updateFitness();
            }
        }
    }

    protected void updateStats_WakeState() {
        if (!this.isAnimal()) {
            if (GameServer.server || !GameClient.client && IsoPlayer.getInstance() == this) {
                if (this.asleep) {
                    this.updateStats_Sleeping();
                } else {
                    this.updateStats_Awake();
                }
            }
        }
    }

    protected void updateStats_Sleeping() {
    }

    protected void updateStats_Awake() {
        this.stats
            .remove(
                CharacterStat.STRESS, (float)(ZomboidGlobals.stressReduction * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay())
            );
        float fatiguemod = 1.0F - this.stats.get(CharacterStat.ENDURANCE);
        if (fatiguemod < 0.3F) {
            fatiguemod = 0.3F;
        }

        float mul = 1.0F;
        if (this.characterTraits.get(CharacterTrait.NEEDS_LESS_SLEEP)) {
            mul = 0.7F;
        }

        if (this.characterTraits.get(CharacterTrait.NEEDS_MORE_SLEEP)) {
            mul = 1.3F;
        }

        float mod = 1.0F;
        if (this.isSitOnGround() || this.isSittingOnFurniture() || this.isResting()) {
            mod = 1.5F;
        }

        this.stats
            .add(
                CharacterStat.FATIGUE,
                (float)(
                    ZomboidGlobals.fatigueIncrease
                        * SandboxOptions.instance.getStatsDecreaseMultiplier()
                        * fatiguemod
                        * GameTime.instance.getMultiplier()
                        * GameTime.instance.getDeltaMinutesPerDay()
                        * mul
                        * this.getFatiqueMultiplier()
                        / mod
                )
            );
        float hungerMult = this.getAppetiteMultiplier();
        if ((!(this instanceof IsoPlayer) || !((IsoPlayer)this).IsRunning() || !this.isPlayerMoving()) && !this.isCurrentState(SwipeStatePlayer.instance())) {
            if (this.moodles.getMoodleLevel(MoodleType.FOOD_EATEN) == 0) {
                this.stats
                    .add(
                        CharacterStat.HUNGER,
                        (float)(
                            ZomboidGlobals.hungerIncrease
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
                                * GameTime.instance.getMultiplier()
                                * GameTime.instance.getDeltaMinutesPerDay()
                                * this.getHungerMultiplier()
                        )
                    );
            }
        } else if (this.moodles.getMoodleLevel(MoodleType.FOOD_EATEN) == 0) {
            this.stats
                .add(
                    CharacterStat.HUNGER,
                    (float)(
                        ZomboidGlobals.hungerIncreaseWhenExercise
                            / 3.0
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
                        ZomboidGlobals.hungerIncreaseWhenExercise
                            * SandboxOptions.instance.getStatsDecreaseMultiplier()
                            * hungerMult
                            * GameTime.instance.getMultiplier()
                            * GameTime.instance.getDeltaMinutesPerDay()
                            * this.getHungerMultiplier()
                    )
                );
        }

        this.updateIdleSquareTime();
        if (this.isInCombat()) {
            this.stats.reset(CharacterStat.IDLENESS);
        } else if (this.isCurrentlyIdle() && this.getCurrentSquare() != null) {
            if (this.getCurrentSquare() == this.getLastSquare() && this.getIdleSquareTime() >= 1800.0F) {
                this.stats
                    .set(
                        CharacterStat.IDLENESS,
                        (float)(
                            this.stats.get(CharacterStat.IDLENESS)
                                + ZomboidGlobals.idleIncreaseRate * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay()
                        )
                    );
            }

            if (this.getCurrentSquare().isInARoom()) {
                this.stats
                    .set(
                        CharacterStat.IDLENESS,
                        (float)(
                            this.stats.get(CharacterStat.IDLENESS)
                                + ZomboidGlobals.idleIncreaseRate / 3.0 * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay()
                        )
                    );
            }
        } else if (!this.isSittingOnFurniture() && !this.isSitOnGround()) {
            this.stats
                .set(
                    CharacterStat.IDLENESS,
                    (float)(
                        this.stats.get(CharacterStat.IDLENESS)
                            - ZomboidGlobals.idleDecreaseRate * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay()
                    )
                );
        }
    }

    private void updateMorale() {
        float mod = 1.0F - this.stats.getNicotineStress() - 0.5F;
        mod *= 1.0E-4F;
        if (mod > 0.0F) {
            mod += 0.5F;
        }

        this.stats.add(CharacterStat.MORALE, PZMath.clamp(mod, 0.0F, 1.0F));
    }

    private void updateFitness() {
        float expectedFitness = this.getPerkLevel(PerkFactory.Perks.Fitness) / 5.0F - 1.0F;
        if (Core.debug && !DebugOptions.instance.cheat.player.fastLooseXp.getValue()) {
            float currentFitness = this.stats.get(CharacterStat.FITNESS);
            if (PZMath.abs(currentFitness - this.lastFitnessValue) > 1.0E-4F && PZMath.abs(currentFitness - expectedFitness) > 1.0E-4F) {
                int newPerkLevel = PZMath.roundToInt((currentFitness + 1.0F) * 5.0F);
                this.setPerkLevelDebug(PerkFactory.Perks.Fitness, newPerkLevel);
            } else {
                this.stats.set(CharacterStat.FITNESS, expectedFitness);
            }

            this.lastFitnessValue = this.stats.get(CharacterStat.FITNESS);
        } else {
            this.stats.set(CharacterStat.FITNESS, expectedFitness);
        }
    }

    private void updateTripping() {
        if (this.stats.isTripping()) {
            this.stats.addTrippingRotAngle(0.06F);
        }
    }

    protected float getAppetiteMultiplier() {
        float hungerMult = 1.0F - this.stats.get(CharacterStat.HUNGER);
        if (this.characterTraits.get(CharacterTrait.HEARTY_APPETITE)) {
            hungerMult *= 1.5F;
        }

        if (this.characterTraits.get(CharacterTrait.LIGHT_EATER)) {
            hungerMult *= 0.75F;
        }

        return hungerMult;
    }

    private void updateStress() {
        if (!this.isAnimal()) {
            this.stats
                .add(
                    CharacterStat.STRESS,
                    (float)(
                        WorldSoundManager.instance
                                .getStressFromSounds(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()))
                            * ZomboidGlobals.stressFromSoundsMultiplier
                    )
                );
            if (this.getBodyDamage().getNumPartsBitten() > 0) {
                this.stats
                    .add(
                        CharacterStat.STRESS,
                        (float)(ZomboidGlobals.stressFromBiteOrScratch * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay())
                    );
            }

            if (this.getBodyDamage().getNumPartsScratched() > 0) {
                this.stats
                    .add(
                        CharacterStat.STRESS,
                        (float)(ZomboidGlobals.stressFromBiteOrScratch * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay())
                    );
            }

            if (this.getBodyDamage().IsInfected() || this.getBodyDamage().IsFakeInfected()) {
                this.stats
                    .add(
                        CharacterStat.STRESS,
                        (float)(ZomboidGlobals.stressFromBiteOrScratch * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay())
                    );
            }

            if (this.characterTraits.get(CharacterTrait.HEMOPHOBIC)) {
                this.stats
                    .add(
                        CharacterStat.STRESS,
                        (float)(
                            this.getTotalBlood()
                                * ZomboidGlobals.stressFromHemophobic
                                * (GameTime.instance.getMultiplier() / 0.8F)
                                * GameTime.instance.getDeltaMinutesPerDay()
                        )
                    );
            }

            this.stats
                .remove(
                    CharacterStat.ANGER, (float)(ZomboidGlobals.angerDecrease * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay())
                );
        }
    }

    private void updateEndurance() {
        this.stats.setLastEndurance(this.stats.get(CharacterStat.ENDURANCE));
        if (this.isUnlimitedEndurance()) {
            this.stats.reset(CharacterStat.ENDURANCE);
        }
    }

    private void updateThirst() {
        float traitMod = 1.0F;
        if (this.characterTraits.get(CharacterTrait.HIGH_THIRST)) {
            traitMod *= 2.0F;
        }

        if (this.characterTraits.get(CharacterTrait.LOW_THIRST)) {
            traitMod *= 0.5F;
        }

        if ((GameServer.server || !GameClient.client && IsoPlayer.getInstance() == this) && (this.isoPlayer == null || !this.isoPlayer.isGhostMode())) {
            if (this.asleep) {
                this.stats
                    .add(
                        CharacterStat.THIRST,
                        (float)(
                            ZomboidGlobals.thirstSleepingIncrease
                                * SandboxOptions.instance.getStatsDecreaseMultiplier()
                                * GameTime.instance.getMultiplier()
                                * GameTime.instance.getDeltaMinutesPerDay()
                                * traitMod
                        )
                    );
            } else {
                this.stats
                    .add(
                        CharacterStat.THIRST,
                        (float)(
                            ZomboidGlobals.thirstIncrease
                                * SandboxOptions.instance.getStatsDecreaseMultiplier()
                                * GameTime.instance.getMultiplier()
                                * this.getRunningThirstReduction()
                                * GameTime.instance.getDeltaMinutesPerDay()
                                * traitMod
                                * this.getThirstMultiplier()
                        )
                    );
            }
        }

        this.autoDrink();
    }

    private double getRunningThirstReduction() {
        return this == IsoPlayer.getInstance() && IsoPlayer.getInstance().IsRunning() ? 1.2 : 1.0;
    }

    public void faceDirection(IsoDirections dir) {
        this.dir = dir;
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    public void faceLocation(float x, float y) {
        tempo.x = x + 0.5F;
        tempo.y = y + 0.5F;
        tempo.x = tempo.x - this.getX();
        tempo.y = tempo.y - this.getY();
        this.DirectionFromVector(tempo);
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    public void faceLocationF(float x, float y) {
        tempo.x = x;
        tempo.y = y;
        tempo.x = tempo.x - this.getX();
        tempo.y = tempo.y - this.getY();
        if (tempo.getLengthSquared() != 0.0F) {
            this.DirectionFromVector(tempo);
            tempo.normalize();
            this.setForwardDirection(tempo.x, tempo.y);
            AnimationPlayer animationPlayer = this.getAnimationPlayer();
            if (animationPlayer != null && animationPlayer.isReady()) {
                animationPlayer.updateForwardDirection(this);
            }
        }
    }

    public boolean isFacingLocation(float x, float y, float dot) {
        Vector2 v1 = BaseVehicle.allocVector2().set(x - this.getX(), y - this.getY());
        v1.normalize();
        Vector2 v2 = this.getLookVector(BaseVehicle.allocVector2());
        float dot2 = v1.dot(v2);
        BaseVehicle.releaseVector2(v1);
        BaseVehicle.releaseVector2(v2);
        return dot2 >= dot;
    }

    public boolean isFacingObject(IsoObject object, float dot) {
        Vector2 facingPos = BaseVehicle.allocVector2();
        object.getFacingPosition(facingPos);
        boolean facing = this.isFacingLocation(facingPos.x, facingPos.y, dot);
        BaseVehicle.releaseVector2(facingPos);
        return facing;
    }

    public void splatBlood(int dist, float alpha) {
        if (this.getCurrentSquare() != null) {
            this.getCurrentSquare().splatBlood(dist, alpha);
        }
    }

    @Override
    public boolean isOutside() {
        return this.getCurrentSquare() == null ? false : this.getCurrentSquare().isOutside();
    }

    @Override
    public boolean isFemale() {
        return this.female;
    }

    @Override
    public void setFemale(boolean isFemale) {
        this.female = isFemale;
    }

    @Override
    public boolean isZombie() {
        return false;
    }

    @Override
    public int getLastHitCount() {
        return this.lastHitCount;
    }

    @Override
    public void setLastHitCount(int hitCount) {
        this.lastHitCount = hitCount;
    }

    public int getSurvivorKills() {
        return this.survivorKills;
    }

    public void setSurvivorKills(int survivorKills) {
        this.survivorKills = survivorKills;
    }

    public int getAge() {
        return this.age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void exert(float f) {
        if (this.characterTraits.get(CharacterTrait.JOGGER)) {
            f *= 0.9F;
        }

        this.stats.remove(CharacterStat.ENDURANCE, f);
    }

    @Override
    public IsoGameCharacter.PerkInfo getPerkInfo(PerkFactory.Perk perk) {
        for (int n = 0; n < this.perkList.size(); n++) {
            IsoGameCharacter.PerkInfo info = this.perkList.get(n);
            if (info.perk == perk) {
                return info;
            }
        }

        return null;
    }

    @Override
    public boolean isEquipped(InventoryItem item) {
        return this.isEquippedClothing(item) || this.isHandItem(item);
    }

    @Override
    public boolean isEquippedClothing(InventoryItem item) {
        return this.wornItems.contains(item);
    }

    @Override
    public boolean isAttachedItem(InventoryItem item) {
        return this.getAttachedItems().contains(item);
    }

    @Override
    public void faceThisObject(IsoObject object) {
        if (object != null) {
            Vector2 facingPosition = tempo;
            BaseVehicle objVehicle = Type.tryCastTo(object, BaseVehicle.class);
            BarricadeAble barricadeAble = Type.tryCastTo(object, BarricadeAble.class);
            if (objVehicle != null) {
                objVehicle.getFacingPosition(this, facingPosition);
                facingPosition.x = facingPosition.x - this.getX();
                facingPosition.y = facingPosition.y - this.getY();
                this.DirectionFromVector(facingPosition);
                facingPosition.normalize();
                this.setForwardDirection(facingPosition.x, facingPosition.y);
            } else if (barricadeAble != null && this.current == barricadeAble.getSquare()) {
                this.dir = barricadeAble.getNorth() ? IsoDirections.N : IsoDirections.W;
                this.setForwardDirectionFromIsoDirection();
            } else if (barricadeAble != null && this.current == barricadeAble.getOppositeSquare()) {
                this.dir = barricadeAble.getNorth() ? IsoDirections.S : IsoDirections.E;
                this.setForwardDirectionFromIsoDirection();
            } else {
                object.getFacingPosition(facingPosition);
                facingPosition.x = facingPosition.x - this.getX();
                facingPosition.y = facingPosition.y - this.getY();
                facingPosition.normalize();
                this.DirectionFromVector(facingPosition);
                this.setForwardDirection(facingPosition.x, facingPosition.y);
            }

            AnimationPlayer animationPlayer = this.getAnimationPlayer();
            if (animationPlayer != null && animationPlayer.isReady()) {
                animationPlayer.updateForwardDirection(this);
            }
        }
    }

    @Override
    public void facePosition(int x, int y) {
        tempo.x = x;
        tempo.y = y;
        tempo.x = tempo.x - this.getX();
        tempo.y = tempo.y - this.getY();
        this.DirectionFromVector(tempo);
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    @Override
    public void faceThisObjectAlt(IsoObject object) {
        if (object != null) {
            object.getFacingPositionAlt(tempo);
            tempo.x = tempo.x - this.getX();
            tempo.y = tempo.y - this.getY();
            this.DirectionFromVector(tempo);
            this.setForwardDirectionFromIsoDirection();
            AnimationPlayer animationPlayer = this.getAnimationPlayer();
            if (animationPlayer != null && animationPlayer.isReady()) {
                animationPlayer.updateForwardDirection(this);
            }
        }
    }

    public void setAnimated(boolean b) {
        this.legsSprite.animate = true;
    }

    public long playHurtSound() {
        return this.getEmitter().playVocals(this.getHurtSound());
    }

    public void playDeadSound() {
        if (!(this instanceof IsoAnimal)) {
            if (this.isCloseKilled()) {
                this.getEmitter().playSoundImpl("HeadStab", this);
            } else if (this.isKilledBySlicingWeapon()) {
                this.getEmitter().playSoundImpl("HeadSlice", this);
            } else if (this instanceof IsoZombie) {
                this.getEmitter().playSoundImpl("HeadSmash", this);
            } else if (this instanceof IsoPlayer player && !this.isDeathDragDown() && !this.isKilledByFall()) {
                player.playerVoiceSound("DeathAlone");
            }

            if (this.isZombie()) {
                ((IsoZombie)this).parameterZombieState.setState(ParameterZombieState.State.Death);
            }
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        super.saveChange(change, tbl, bb);
        if ("addItem".equals(change)) {
            DebugLog.General
                .warn(
                    "The addItem change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function."
                );
        } else if ("addItemOfType".equals(change)) {
            DebugLog.General
                .warn(
                    "The addItemOfType change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function."
                );
        } else if ("AddRandomDamageFromZombie".equals(change)) {
            if (tbl != null && tbl.rawget("zombie") instanceof Double) {
                bb.putShort(((Double)tbl.rawget("zombie")).shortValue());
            }
        } else if (!"AddZombieKill".equals(change)) {
            if ("removeItem".equals(change)) {
                DebugLog.General
                    .warn(
                        "The removeItem change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                    );
            } else if ("removeItemID".equals(change)) {
                DebugLog.General
                    .warn(
                        "The removeItemID change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                    );
            } else if ("removeItemType".equals(change)) {
                DebugLog.General
                    .warn(
                        "The removeItemType change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                    );
            } else if ("removeOneOf".equals(change)) {
                DebugLog.General
                    .warn(
                        "The removeOneOf change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                    );
            } else if ("reanimatedID".equals(change)) {
                if (tbl != null && tbl.rawget("ID") instanceof Double) {
                    int ID = ((Double)tbl.rawget("ID")).intValue();
                    bb.putInt(ID);
                }
            } else if ("Shove".equals(change)) {
                if (tbl != null && tbl.rawget("hitDirX") instanceof Double && tbl.rawget("hitDirY") instanceof Double && tbl.rawget("force") instanceof Double) {
                    bb.putFloat(((Double)tbl.rawget("hitDirX")).floatValue());
                    bb.putFloat(((Double)tbl.rawget("hitDirY")).floatValue());
                    bb.putFloat(((Double)tbl.rawget("force")).floatValue());
                }
            } else if (!"wakeUp".equals(change) && "mechanicActionDone".equals(change) && tbl != null) {
                bb.put((byte)(tbl.rawget("success") ? 1 : 0));
            }
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        super.loadChange(change, bb);
        if ("addItem".equals(change)) {
            DebugLog.General
                .warn(
                    "The addItem change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function."
                );
        } else if ("addItemOfType".equals(change)) {
            DebugLog.General
                .warn(
                    "The addItemOfType change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function."
                );
        } else if ("AddRandomDamageFromZombie".equals(change)) {
            short id = bb.getShort();
            IsoZombie zombie = GameClient.getZombie(id);
            if (zombie != null && !this.isDead()) {
                this.getBodyDamage().AddRandomDamageFromZombie(zombie, null);
                this.getBodyDamage().Update();
                if (this.isDead()) {
                    if (this.isFemale()) {
                        zombie.getEmitter().playSound("FemaleBeingEatenDeath");
                    } else {
                        zombie.getEmitter().playSound("MaleBeingEatenDeath");
                    }
                }
            }
        } else if ("AddZombieKill".equals(change)) {
            this.setZombieKills(this.getZombieKills() + 1);
        } else if ("exitVehicle".equals(change)) {
            BaseVehicle vehicle = this.getVehicle();
            if (vehicle != null) {
                vehicle.exit(this);
                this.setVehicle(null);
            }
        } else if ("removeItem".equals(change)) {
            DebugLog.General
                .warn(
                    "The removeItem change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                );
        } else if ("removeItemID".equals(change)) {
            DebugLog.General
                .warn(
                    "The removeItemID change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                );
        } else if ("removeItemType".equals(change)) {
            DebugLog.General
                .warn(
                    "The removeItemType change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                );
        } else if ("removeOneOf".equals(change)) {
            DebugLog.General
                .warn(
                    "The removeOneOf change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function."
                );
        } else if ("reanimatedID".equals(change)) {
            this.reanimatedCorpseId = bb.getInt();
        } else if (!"Shove".equals(change)) {
            if ("StopBurning".equals(change)) {
                this.StopBurning();
            } else if ("wakeUp".equals(change)) {
                if (this.isAsleep()) {
                    this.asleep = false;
                    this.forceWakeUpTime = -1.0F;
                    TutorialManager.instance.stealControl = false;
                    if (this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                        UIManager.setFadeBeforeUI(((IsoPlayer)this).getPlayerNum(), true);
                        UIManager.FadeIn(((IsoPlayer)this).getPlayerNum(), 2.0);
                        GameClient.instance.sendPlayer((IsoPlayer)this);
                    }
                }
            } else if ("mechanicActionDone".equals(change)) {
                boolean success = bb.get() == 1;
                LuaEventManager.triggerEvent("OnMechanicActionDone", this, success);
            } else if ("vehicleNoKey".equals(change)) {
                this.SayDebug(" [img=media/ui/CarKey_none.png]");
            }
        }
    }

    @Override
    public int getAlreadyReadPages(String fullType) {
        for (int i = 0; i < this.readBooks.size(); i++) {
            IsoGameCharacter.ReadBook read = this.readBooks.get(i);
            if (read.fullType.equals(fullType)) {
                return read.alreadyReadPages;
            }
        }

        return 0;
    }

    @Override
    public void setAlreadyReadPages(String fullType, int pages) {
        for (int i = 0; i < this.readBooks.size(); i++) {
            IsoGameCharacter.ReadBook read = this.readBooks.get(i);
            if (read.fullType.equals(fullType)) {
                read.alreadyReadPages = pages;
                return;
            }
        }

        IsoGameCharacter.ReadBook read = new IsoGameCharacter.ReadBook();
        read.fullType = fullType;
        read.alreadyReadPages = pages;
        this.readBooks.add(read);
    }

    public void updateLightInfo() {
        if (GameServer.server) {
            if (!this.isZombie()) {
                synchronized (this.lightInfo) {
                    this.lightInfo.square = this.movingSq;
                    if (this.lightInfo.square == null) {
                        this.lightInfo.square = this.getCell()
                            .getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
                    }

                    if (this.reanimatedCorpse != null) {
                        this.lightInfo.square = this.getCell()
                            .getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
                    }

                    this.lightInfo.x = this.getX();
                    this.lightInfo.y = this.getY();
                    this.lightInfo.z = this.getZ();
                    this.lightInfo.angleX = this.getForwardDirectionX();
                    this.lightInfo.angleY = this.getForwardDirectionY();
                    this.lightInfo.torches.clear();
                    this.lightInfo.night = GameTime.getInstance().getNight();
                }
            }
        }
    }

    public IsoGameCharacter.LightInfo initLightInfo2() {
        synchronized (this.lightInfo) {
            for (int i = 0; i < this.lightInfo2.torches.size(); i++) {
                IsoGameCharacter.TorchInfo.release(this.lightInfo2.torches.get(i));
            }

            this.lightInfo2.initFrom(this.lightInfo);
        }

        return this.lightInfo2;
    }

    public IsoGameCharacter.LightInfo getLightInfo2() {
        return this.lightInfo2;
    }

    @Override
    public void postupdate() {
        try (AbstractPerformanceProfileProbe ignored = this.postUpdateInternal.profile()) {
            this.postUpdateInternal();
        }
    }

    public float getAnimationTimeDelta() {
        return GameTime.instance.getTimeDelta() * this.animationTimeScale;
    }

    public void updateForServerGui() {
        AnimationPlayer animPlayer = this.getAnimationPlayer();
        float animDeltaT = this.getAnimationTimeDelta();
        this.advancedAnimator.update(animDeltaT);
        if (!this.hasActiveModel()) {
            this.updateAnimPlayer(animPlayer);
        } else {
            this.updateModelSlot();
        }

        this.updateLightInfo();
    }

    private void postUpdateInternal() {
        super.postupdate();
        this.clearHitInfo();
        this.clearAttackVars();
        if (this.ballisticsController != null) {
            this.ballisticsController.postUpdate();
        }

        if (GameServer.server || this.isAnimationUpdatingThisFrame()) {
            AnimationPlayer animPlayer = this.getAnimationPlayer();
            animPlayer.updateForwardDirection(this);
            animPlayer.updateVerticalAimAngle(this);
            boolean shouldBeTurning = this.shouldBeTurning();
            this.setTurning(shouldBeTurning);
            boolean shouldBeTurning90 = this.shouldBeTurning90();
            this.setTurning90(shouldBeTurning90);
            boolean shouldBeTurningAround = this.shouldBeTurningAround();
            this.setTurningAround(shouldBeTurningAround);
            this.actionContext.update();
            if (GameClient.client) {
                this.getNetworkCharacterAI().postUpdate();
            }

            if (this.getCurrentSquare() != null) {
                float animDeltaT = this.getAnimationTimeDelta();
                this.advancedAnimator.update(animDeltaT);
            }

            this.actionContext.clearEvent("ActiveAnimFinished");
            this.actionContext.clearEvent("ActiveAnimFinishing");
            this.actionContext.clearEvent("ActiveAnimLooped");
            GameProfiler profiler = GameProfiler.getInstance();

            try (GameProfiler.ProfileArea ignored = profiler.profile("Deltas")) {
                this.applyDeltas(animPlayer);
            }

            if (!this.hasActiveModel()) {
                try (GameProfiler.ProfileArea ignored = profiler.profile("Anim Player")) {
                    this.updateAnimPlayer(animPlayer);
                }
            } else {
                try (GameProfiler.ProfileArea ignored = profiler.profile("Model Slot")) {
                    this.updateModelSlot();
                }
            }

            this.updateLightInfo();
            if (this.isAnimationRecorderActive()) {
                for (int i = 0; i < animPlayer.getNumTwistBones(); i++) {
                    TwistableBoneTransform twistableBone = animPlayer.getTwistBoneAt(i);
                    this.setVariable("twistBone_" + i + "_Name", animPlayer.getTwistBoneNameAt(i));
                    this.setVariable("twistBone_" + i + "_Twist", (180.0F / (float)Math.PI) * twistableBone.twist);
                    this.setVariable("twistBone_" + i + "_BlendWeight", twistableBone.blendWeight);
                }

                this.animationRecorder.logVariables(this);
            }
        }
    }

    public boolean isAnimationUpdatingThisFrame() {
        return this.animationUpdatingThisFrame;
    }

    private void clearHitInfo() {
        this.hitInfoList.clear();
    }

    private void clearAttackVars() {
        this.attackVars.clear();
    }

    private void updateAnimPlayer(AnimationPlayer animPlayer) {
        animPlayer.updateBones = false;
        boolean InterpolateAnims = PerformanceSettings.interpolateAnims;
        PerformanceSettings.interpolateAnims = false;

        try {
            animPlayer.updateForwardDirection(this);
            float animDeltaT = this.getAnimationTimeDelta();
            animPlayer.Update(animDeltaT);
        } catch (Throwable var7) {
            ExceptionLogger.logException(var7);
        } finally {
            animPlayer.updateBones = true;
            PerformanceSettings.interpolateAnims = InterpolateAnims;
        }
    }

    private void updateModelSlot() {
        try {
            ModelManager.ModelSlot modelSlot = this.legsSprite.modelSlot;
            float animDeltaT = this.getAnimationTimeDelta();
            modelSlot.Update(animDeltaT);
        } catch (Throwable var3) {
            ExceptionLogger.logException(var3);
        }
    }

    private void applyDeltas(AnimationPlayer animPlayer) {
        MoveDeltaModifiers deltas = IsoGameCharacter.L_postUpdate.moveDeltas;
        deltas.moveDelta = this.getMoveDelta();
        deltas.turnDelta = this.getTurnDelta();
        boolean hasPath = this.hasPath();
        boolean isPlayer = this instanceof IsoPlayer;
        if (isPlayer && hasPath && this.isRunning()) {
            deltas.turnDelta = Math.max(deltas.turnDelta, 2.0F);
        }

        State currentState = this.getCurrentState();
        if (currentState != null) {
            currentState.getDeltaModifiers(this, deltas);
        }

        if (this.hasPath() && this.getPathFindBehavior2().isTurningToObstacle()) {
            deltas.setMaxTurnDelta(2.0F);
        }

        this.getCurrentTimedActionDeltaModifiers(deltas);
        if (deltas.twistDelta == -1.0F) {
            deltas.twistDelta = deltas.turnDelta * 1.8F;
        }

        if (!this.isTurning() && !GameServer.server) {
            deltas.turnDelta = 0.0F;
        }

        float movementTurnMultiplier = Math.max(1.0F - deltas.moveDelta / 2.0F, 0.0F);
        animPlayer.angleStepDelta = movementTurnMultiplier * deltas.turnDelta;
        animPlayer.angleTwistDelta = movementTurnMultiplier * deltas.twistDelta;
        animPlayer.setMaxTwistAngle((float) (Math.PI / 180.0) * this.getMaxTwist());
    }

    private void getCurrentTimedActionDeltaModifiers(MoveDeltaModifiers deltas) {
        if (!this.getCharacterActions().isEmpty()) {
            BaseAction action = this.getCharacterActions().get(0);
            if (action != null) {
                if (!action.finished()) {
                    action.getDeltaModifiers(deltas);
                }
            }
        }
    }

    public boolean shouldBeTurning() {
        boolean isTwisting = this.isTwisting();
        if (this.isZombie() && this.getCurrentState() == ZombieFallDownState.instance()) {
            return false;
        } else if (this.blockTurning) {
            return false;
        } else if (this.isBehaviourMoving()) {
            return isTwisting;
        } else if (this.isPlayerMoving()) {
            return isTwisting;
        } else if (this.isAttacking()) {
            return !this.aimAtFloor;
        } else {
            float absExcessTwist = this.getAbsoluteExcessTwist();
            if (absExcessTwist > 1.0F) {
                return true;
            } else {
                return this.isTurning() ? isTwisting : false;
            }
        }
    }

    public boolean shouldBeTurning90() {
        if (!this.isTurning()) {
            return false;
        } else if (this.isTurning90()) {
            return true;
        } else {
            float targetTwist = this.getTargetTwist();
            float targetTwistAbs = Math.abs(targetTwist);
            return targetTwistAbs > 65.0F;
        }
    }

    public boolean shouldBeTurningAround() {
        if (!this.isTurning()) {
            return false;
        } else {
            float targetTwist = this.getTargetTwist();
            float targetTwistAbs = Math.abs(targetTwist);
            return this.isTurningAround() ? targetTwistAbs > 45.0F : targetTwistAbs > 110.0F;
        }
    }

    public boolean isTurning() {
        return this.isTurning;
    }

    private void setTurning(boolean isTurning) {
        this.isTurning = isTurning;
    }

    public boolean isTurningAround() {
        return this.isTurningAround;
    }

    private void setTurningAround(boolean is) {
        boolean isDifferent = this.isTurningAround != is;
        boolean wasTurningAround = this.isTurningAround;
        if (isDifferent || wasTurningAround) {
            this.isTurningAround = is;
            float previousTargetAngle = this.initialTurningAroundTarget;
            float currentTargetAngle = this.getDirectionAngle();
            if (isDifferent && is) {
                this.invokeGlobalAnimEvent(s_turn180StartedEvent);
                this.initialTurningAroundTarget = currentTargetAngle;
            }

            if (!isDifferent && wasTurningAround) {
                float angleDiff = PZMath.getClosestAngleDegrees(previousTargetAngle, currentTargetAngle);
                if (PZMath.abs(angleDiff) > 90.0F) {
                    this.invokeGlobalAnimEvent(s_turn180TargetChangedEvent);
                    this.isTurningAround = false;
                }
            }
        }
    }

    private void invokeGlobalAnimEvent(AnimEvent globalEvent) {
        AdvancedAnimator advancedAnimator = this.getAdvancedAnimator();
        if (advancedAnimator != null) {
            advancedAnimator.invokeGlobalAnimEvent(globalEvent);
        }
    }

    public boolean isTurning90() {
        return this.isTurning90;
    }

    private void setTurning90(boolean is) {
        this.isTurning90 = is;
    }

    public boolean hasPath() {
        return this.getPath2() != null;
    }

    @Override
    public boolean isAnimationRecorderActive() {
        return this.animationRecorder != null && this.animationRecorder.isRecording();
    }

    @Override
    public AnimationPlayerRecorder getAnimationPlayerRecorder() {
        return this.animationRecorder;
    }

    @Override
    public float getMeleeDelay() {
        return this.meleeDelay;
    }

    @Override
    public void setMeleeDelay(float delay) {
        this.meleeDelay = Math.max(delay, 0.0F);
    }

    @Override
    public float getRecoilDelay() {
        return this.recoilDelay;
    }

    @Override
    public void setRecoilDelay(float recoilDelay) {
        this.recoilDelay = PZMath.max(0.0F, recoilDelay);
    }

    public float getAimingDelay() {
        return this.aimingDelay;
    }

    public void setAimingDelay(float aimingDelay) {
        this.aimingDelay = aimingDelay;
    }

    public void resetAimingDelay() {
        if (!(this.getPrimaryHandItem() instanceof HandWeapon)) {
            this.aimingDelay = 0.0F;
        } else {
            this.aimingDelay = ((HandWeapon)this.getPrimaryHandItem()).getAimingTime();
            this.aimingDelay = this.aimingDelay
                * (this.characterTraits.get(CharacterTrait.DEXTROUS) ? 0.8F : (this.characterTraits.get(CharacterTrait.ALL_THUMBS) ? 1.2F : 1.0F));
            this.aimingDelay = this.aimingDelay * (this.getVehicle() != null ? 1.5F : 1.0F);
        }
    }

    public void updateAimingDelay() {
        float mod = 0.0F;
        if (this.getPrimaryHandItem() instanceof HandWeapon) {
            mod = ((HandWeapon)this.getPrimaryHandItem()).getRecoilDelay(this) * (this.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0F);
        }

        if (this.isAiming() && this.getRecoilDelay() <= 0.0F + mod && !this.getVariableBoolean("isracking")) {
            this.aimingDelay = PZMath.max(
                this.aimingDelay
                    - 0.625F
                        * GameTime.getInstance().getMultiplier()
                        * (1.0F + 0.05F * this.getPerkLevel(PerkFactory.Perks.Aiming) + (this.characterTraits.get(CharacterTrait.MARKSMAN) ? 0.1F : 0.0F)),
                0.0F
            );
        } else if (!this.isAiming()) {
            this.resetAimingDelay();
        }
    }

    public float getBeenMovingFor() {
        return this.beenMovingFor;
    }

    public void setBeenMovingFor(float beenMovingFor) {
        this.beenMovingFor = PZMath.clamp(beenMovingFor, 0.0F, 70.0F);
    }

    public String getClickSound() {
        return this.clickSound;
    }

    public void setClickSound(String clickSound) {
        this.clickSound = clickSound;
    }

    public int getMeleeCombatMod() {
        int level = this.getWeaponLevel();
        if (level == 1) {
            return -2;
        } else if (level == 2) {
            return 0;
        } else if (level == 3) {
            return 1;
        } else if (level == 4) {
            return 2;
        } else if (level == 5) {
            return 3;
        } else if (level == 6) {
            return 4;
        } else if (level == 7) {
            return 5;
        } else if (level == 8) {
            return 5;
        } else if (level == 9) {
            return 6;
        } else {
            return level >= 10 ? 7 : -5;
        }
    }

    @Override
    public int getWeaponLevel() {
        return this.getWeaponLevel(null);
    }

    @Override
    public int getWeaponLevel(HandWeapon weapon) {
        WeaponType weaponType = WeaponType.getWeaponType(this);
        if (weapon != null) {
            weaponType = WeaponType.getWeaponType(this);
        }

        if (weapon == null) {
            weapon = Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
        }

        int level = -1;
        if (weaponType != null && weaponType != WeaponType.UNARMED && weapon != null) {
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.AXE)) {
                level = this.getPerkLevel(PerkFactory.Perks.Axe);
            }

            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.SPEAR)) {
                level += this.getPerkLevel(PerkFactory.Perks.Spear);
            }

            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.SMALL_BLADE)) {
                level += this.getPerkLevel(PerkFactory.Perks.SmallBlade);
            }

            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.LONG_BLADE)) {
                level += this.getPerkLevel(PerkFactory.Perks.LongBlade);
            }

            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.BLUNT)) {
                level += this.getPerkLevel(PerkFactory.Perks.Blunt);
            }

            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
                level += this.getPerkLevel(PerkFactory.Perks.SmallBlunt);
            }
        }

        if (level > 10) {
            level = 10;
        }

        return level == -1 ? 0 : level;
    }

    @Override
    public int getMaintenanceMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Maintenance);
        return level + this.getWeaponLevel() / 2;
    }

    @Override
    public BaseVehicle getVehicle() {
        return this.vehicle;
    }

    @Override
    public void setVehicle(BaseVehicle v) {
        this.vehicle = v;
    }

    public boolean isUnderVehicle() {
        return this.isUnderVehicleRadius(0.3F);
    }

    public boolean isUnderVehicleRadius(float radius) {
        int chunkMinX = (PZMath.fastfloor(this.getX()) - 4) / 8;
        int chunkMinY = (PZMath.fastfloor(this.getY()) - 4) / 8;
        int chunkMaxX = (int)Math.ceil((this.getX() + 4.0F) / 8.0F);
        int chunkMaxY = (int)Math.ceil((this.getY() + 4.0F) / 8.0F);
        Vector2 vector2 = Vector2ObjectPool.get().alloc();

        for (int y = chunkMinY; y < chunkMaxY; y++) {
            for (int x = chunkMinX; x < chunkMaxX; x++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        Vector2 v = vehicle.testCollisionWithCharacter(this, radius, vector2);
                        if (v != null && v.x != -1.0F) {
                            Vector2ObjectPool.get().release(vector2);
                            return true;
                        }
                    }
                }
            }
        }

        Vector2ObjectPool.get().release(vector2);
        return false;
    }

    public boolean isBeingSteppedOn() {
        if (!this.isOnFloor()) {
            return false;
        } else {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    IsoGridSquare square = this.getCell()
                        .getGridSquare(PZMath.fastfloor(this.getX()) + dx, PZMath.fastfloor(this.getY()) + dy, PZMath.fastfloor(this.getZ()));
                    if (square != null) {
                        ArrayList<IsoMovingObject> objects = square.getMovingObjects();

                        for (int i = 0; i < objects.size(); i++) {
                            IsoMovingObject obj = objects.get(i);
                            if (obj != this
                                && obj instanceof IsoGameCharacter chr
                                && chr.getVehicle() == null
                                && !obj.isOnFloor()
                                && ZombieOnGroundState.isCharacterStandingOnOther(chr, this)) {
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
    public float getReduceInfectionPower() {
        return this.reduceInfectionPower;
    }

    @Override
    public void setReduceInfectionPower(float reduceInfectionPower) {
        this.reduceInfectionPower = reduceInfectionPower;
    }

    @Override
    public float getInventoryWeight() {
        if (this.getInventory() == null) {
            return 0.0F;
        } else {
            float total = 0.0F;
            ArrayList<InventoryItem> items = this.getInventory().getItems();

            for (int i = 0; i < items.size(); i++) {
                InventoryItem item = items.get(i);
                if (item.getAttachedSlot() > -1 && !this.isEquipped(item)) {
                    total += item.getHotbarEquippedWeight();
                } else if (!this.isEquipped(item) && !item.isFakeEquipped(this)) {
                    total += item.getUnequippedWeight();
                } else {
                    total += item.getEquippedWeight();
                }
            }

            return total;
        }
    }

    public void dropHandItems() {
        if (!"Tutorial".equals(Core.gameMode)) {
            if (!(this instanceof IsoPlayer player && !player.isLocalPlayer())) {
                this.dropHeavyItems();
                IsoGridSquare sq = this.getCurrentSquare();
                if (sq != null) {
                    InventoryItem item1 = this.getPrimaryHandItem();
                    InventoryItem item2 = this.getSecondaryHandItem();
                    if (item1 != null || item2 != null) {
                        sq = this.getSolidFloorAt(sq.x, sq.y, sq.z);
                        if (sq != null) {
                            if (item1 != null) {
                                this.setPrimaryHandItem(null);
                                if (!GameClient.client) {
                                    this.getInventory().DoRemoveItem(item1);
                                    float dropX = Rand.Next(0.1F, 0.9F);
                                    float dropY = Rand.Next(0.1F, 0.9F);
                                    float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                                    sq.AddWorldInventoryItem(item1, dropX, dropY, dropZ);
                                    LuaEventManager.triggerEvent("OnContainerUpdate");
                                }

                                LuaEventManager.triggerEvent("onItemFall", item1);
                                this.playDropItemSound(item1);
                            }

                            if (item2 != null) {
                                this.setSecondaryHandItem(null);
                                boolean inBothHand = item2 == item1;
                                if (!inBothHand) {
                                    if (!GameClient.client) {
                                        this.getInventory().DoRemoveItem(item2);
                                        float dropX = Rand.Next(0.1F, 0.9F);
                                        float dropY = Rand.Next(0.1F, 0.9F);
                                        float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                                        sq.AddWorldInventoryItem(item2, dropX, dropY, dropZ);
                                        LuaEventManager.triggerEvent("OnContainerUpdate");
                                    }

                                    LuaEventManager.triggerEvent("onItemFall", item2);
                                    this.playDropItemSound(item2);
                                }
                            }

                            if (GameClient.client && this.isLocal()) {
                                INetworkPacket.send(PacketTypes.PacketType.PlayerDropHeldItems, this, sq.x, sq.y, sq.z, false);
                                INetworkPacket.send(PacketTypes.PacketType.Equip, this);
                            }
                        }
                    }
                }
            }
        }
    }

    public void dropHeldItems(int x, int y, int z, boolean heavy, boolean isThrow) {
        if (GameServer.server) {
            InventoryItem item1 = this.getPrimaryHandItem();
            InventoryItem item2 = this.getSecondaryHandItem();
            if (item1 != null || item2 != null) {
                IsoGridSquare sq = this.getSolidFloorAt(x, y, z);
                if (sq != null) {
                    boolean drop1 = heavy ? this.isHeavyItem(item1) : item1 != null;
                    if (drop1) {
                        this.setPrimaryHandItem(null);
                        this.getInventory().DoRemoveItem(item1);
                        GameServer.sendRemoveItemFromContainer(this.getInventory(), item1);
                        if (!isThrow) {
                            float dropX = Rand.Next(0.1F, 0.9F);
                            float dropY = Rand.Next(0.1F, 0.9F);
                            float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                            sq.AddWorldInventoryItem(item1, dropX, dropY, dropZ);
                        }
                    }

                    if (!isThrow) {
                        boolean drop2 = heavy ? this.isHeavyItem(item2) : item2 != null;
                        if (drop2) {
                            this.setSecondaryHandItem(null);
                            boolean inBothHand = item2 == item1;
                            if (!inBothHand) {
                                this.getInventory().DoRemoveItem(item2);
                                GameServer.sendRemoveItemFromContainer(this.getInventory(), item2);
                                float dropX = Rand.Next(0.1F, 0.9F);
                                float dropY = Rand.Next(0.1F, 0.9F);
                                float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                                sq.AddWorldInventoryItem(item2, dropX, dropY, dropZ);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean shouldBecomeZombieAfterDeath() {
        return switch (SandboxOptions.instance.lore.transmission.getValue()) {
            case 1 -> !this.getBodyDamage().IsFakeInfected() && this.stats.get(CharacterStat.ZOMBIE_INFECTION) >= 0.001F;
            case 2 -> !this.getBodyDamage().IsFakeInfected() && this.stats.get(CharacterStat.ZOMBIE_INFECTION) >= 0.001F;
            case 3 -> true;
            case 4 -> false;
            default -> false;
        };
    }

    @Override
    public void modifyTraitXPBoost(CharacterTrait characterTrait, boolean isRemovingTrait) {
        this.modifyTraitXPBoost(CharacterTraitDefinition.getCharacterTraitDefinition(characterTrait), isRemovingTrait);
    }

    @Override
    public void modifyTraitXPBoost(CharacterTraitDefinition trait, boolean isRemovingTrait) {
        if (trait != null) {
            HashMap<PerkFactory.Perk, Integer> playerXPBoostMap = this.getDescriptor().getXPBoostMap();
            Map<PerkFactory.Perk, Integer> xpBoosts = trait.getXpBoosts();
            if (xpBoosts != null) {
                for (Entry<PerkFactory.Perk, Integer> entry : xpBoosts.entrySet()) {
                    PerkFactory.Perk perkType = entry.getKey();
                    int currentValue = 0;
                    if (playerXPBoostMap.containsKey(perkType)) {
                        currentValue = playerXPBoostMap.get(perkType);
                    }

                    playerXPBoostMap.put(perkType, currentValue + (isRemovingTrait ? -entry.getValue() : entry.getValue()));
                }
            }
        }
    }

    public void applyTraits(List<CharacterTrait> luaTraits) {
        if (luaTraits != null) {
            HashMap<PerkFactory.Perk, Integer> levels = new HashMap<>();
            levels.put(PerkFactory.Perks.Fitness, 5);
            levels.put(PerkFactory.Perks.Strength, 5);

            for (CharacterTrait characterTrait : luaTraits) {
                if (characterTrait != null) {
                    this.characterTraits.set(characterTrait, true);
                    CharacterTraitDefinition trait = CharacterTraitDefinition.getCharacterTraitDefinition(characterTrait);
                    if (trait != null) {
                        Map<PerkFactory.Perk, Integer> xpBoostMap = trait.getXpBoosts();
                        if (xpBoostMap != null) {
                            for (Entry<PerkFactory.Perk, Integer> entry : xpBoostMap.entrySet()) {
                                PerkFactory.Perk perkType = entry.getKey();
                                int level = entry.getValue();
                                if (levels.containsKey(perkType)) {
                                    level += levels.get(perkType);
                                }

                                levels.put(perkType, level);
                            }
                        }
                    }
                }
            }

            if (this instanceof IsoPlayer) {
                ((IsoPlayer)this).getNutrition().applyWeightFromTraits();
            }

            HashMap<PerkFactory.Perk, Integer> xpBoostMap = this.getDescriptor().getXPBoostMap();

            for (Entry<PerkFactory.Perk, Integer> entry : xpBoostMap.entrySet()) {
                PerkFactory.Perk perkType = entry.getKey();
                int level = entry.getValue();
                if (levels.containsKey(perkType)) {
                    level += levels.get(perkType);
                }

                levels.put(perkType, level);
            }

            for (Entry<PerkFactory.Perk, Integer> entry : levels.entrySet()) {
                PerkFactory.Perk perkType = entry.getKey();
                int level = entry.getValue();
                level = Math.max(0, level);
                level = Math.min(10, level);
                this.getDescriptor().getXPBoostMap().put(perkType, Math.min(3, level));

                for (int i = 0; i < level; i++) {
                    this.LevelPerk(perkType);
                }

                this.getXp().setXPToLevel(perkType, this.getPerkLevel(perkType));
            }
        }
    }

    public void applyProfessionRecipes() {
        CharacterProfessionDefinition characterProfessionDefinition = CharacterProfessionDefinition.getCharacterProfessionDefinition(
            this.getDescriptor().getCharacterProfession()
        );
        this.knownRecipes.addAll(characterProfessionDefinition.getGrantedRecipes());
    }

    public void applyCharacterTraitsRecipes() {
        Map<CharacterTrait, Boolean> traits = this.characterTraits.getTraits();

        for (Entry<CharacterTrait, Boolean> entry : traits.entrySet()) {
            if (entry.getValue()) {
                CharacterTraitDefinition trait = CharacterTraitDefinition.getCharacterTraitDefinition(entry.getKey());
                if (trait != null) {
                    this.knownRecipes.addAll(trait.getGrantedRecipes());
                }
            }
        }
    }

    public InventoryItem createKeyRing() {
        return this.createKeyRing(ItemKey.Container.KEY_RING);
    }

    public InventoryItem createKeyRing(ItemKey itemKey) {
        InventoryItem keyringItem = this.getInventory().addItem(itemKey);
        InventoryContainer keyring = (InventoryContainer)keyringItem;
        keyring.setName(Translator.getText("IGUI_KeyRingName", this.getDescriptor().getForename(), this.getDescriptor().getSurname()));
        if (Rand.Next(100) < 40) {
            RoomDef roomDef = IsoWorld.instance.metaGrid.getRoomAt(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
            if (roomDef != null && roomDef.getBuilding() != null) {
                String keyType = "Base.Key1";
                InventoryItem key = keyring.getInventory().AddItem("Base.Key1");
                key.setKeyId(roomDef.getBuilding().getKeyId());
            }
        }

        return keyringItem;
    }

    public void autoDrink() {
        if (!GameClient.client) {
            if (!(GameServer.server && this instanceof IsoPlayer player) || player.getAutoDrink()) {
                if (Core.getInstance().getOptionAutoDrink()) {
                    if (!this.isAsleep()
                        && !this.isPerformingGrappleAnimation()
                        && !this.isKnockedDown()
                        && !this.isbFalling()
                        && !this.isAiming()
                        && !this.isClimbing()) {
                        if (!LuaHookManager.TriggerHook("AutoDrink", this)) {
                            if (!(this.stats.get(CharacterStat.THIRST) <= 0.1F)) {
                                InventoryItem drinkFrom = this.getWaterSource(this.getInventory().getItems());
                                if (drinkFrom != null && drinkFrom.hasComponent(ComponentType.FluidContainer)) {
                                    float amountNeeded = this.stats.get(CharacterStat.THIRST) * 2.0F;
                                    float amount = Math.min(drinkFrom.getFluidContainer().getAmount(), amountNeeded);
                                    float percentage = amount / drinkFrom.getFluidContainer().getAmount();
                                    this.DrinkFluid(drinkFrom, percentage, false);
                                    if (GameServer.server && this instanceof IsoPlayer playerx) {
                                        INetworkPacket.send(playerx, PacketTypes.PacketType.SyncItemFields, playerx, drinkFrom);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public InventoryItem getWaterSource(ArrayList<InventoryItem> items) {
        InventoryItem drinkFrom = null;

        for (int n = 0; n < items.size(); n++) {
            InventoryItem item = items.get(n);
            boolean validItem = false;
            boolean drink = true;
            if (item.isWaterSource()) {
                validItem = true;
                drink = !item.getFluidContainer().isCategory(FluidCategory.Hazardous) || !SandboxOptions.instance.enableTaintedWaterText.getValue();
            }

            if (validItem && drink) {
                if (item.hasComponent(ComponentType.FluidContainer) && item.getFluidContainer().getAmount() >= 0.12) {
                    drinkFrom = item;
                    break;
                }

                if (!(item instanceof InventoryContainer)) {
                    drinkFrom = item;
                    break;
                }
            }
        }

        return drinkFrom;
    }

    @Override
    public List<String> getKnownRecipes() {
        return this.knownRecipes;
    }

    @Override
    public boolean isRecipeKnown(Recipe recipe) {
        return !this.isKnowAllRecipes() && !SandboxOptions.instance.seeNotLearntRecipe.getValue()
            ? this.getKnownRecipes().contains(recipe.getOriginalname())
            : true;
    }

    public boolean isRecipeKnown(CraftRecipe recipe) {
        return this.isRecipeKnown(recipe, false);
    }

    public boolean isRecipeKnown(CraftRecipe recipe, boolean ignoreSandbox) {
        return (ignoreSandbox || !SandboxOptions.instance.seeNotLearntRecipe.getValue()) && !this.isKnowAllRecipes()
            ? !recipe.needToBeLearn()
                || this.getKnownRecipes().contains(recipe.getName())
                || this.getKnownRecipes().contains(recipe.getMetaRecipe())
                || this.getKnownRecipes().contains(recipe.getTranslationName())
            : true;
    }

    @Override
    public boolean isRecipeKnown(String name) {
        return this.isRecipeKnown(name, false);
    }

    public boolean isRecipeKnown(String name, boolean ignoreSandbox) {
        Recipe recipe = ScriptManager.instance.getRecipe(name);
        if (recipe != null) {
            return this.isRecipeKnown(recipe);
        } else {
            return (ignoreSandbox || !SandboxOptions.instance.seeNotLearntRecipe.getValue()) && !this.isKnowAllRecipes()
                ? this.getKnownRecipes().contains(name)
                : true;
        }
    }

    public boolean isRecipeActuallyKnown(CraftRecipe recipe) {
        return this.isRecipeKnown(recipe, true);
    }

    public boolean isRecipeActuallyKnown(String name) {
        return this.isRecipeKnown(name, true);
    }

    public boolean learnRecipe(String name) {
        return this.learnRecipe(name, true);
    }

    public boolean learnRecipe(String name, boolean checkMetaRecipe) {
        if (!this.isRecipeKnown(name, true)) {
            this.getKnownRecipes().add(name);
            if (checkMetaRecipe) {
                ScriptManager.instance.checkMetaRecipe(this, name);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void addKnownMediaLine(String guid) {
        if (!StringUtils.isNullOrWhitespace(guid)) {
            this.knownMediaLines.add(guid.trim());
        }
    }

    @Override
    public void removeKnownMediaLine(String guid) {
        if (!StringUtils.isNullOrWhitespace(guid)) {
            this.knownMediaLines.remove(guid.trim());
        }
    }

    @Override
    public void clearKnownMediaLines() {
        this.knownMediaLines.clear();
    }

    @Override
    public boolean isKnownMediaLine(String guid) {
        return StringUtils.isNullOrWhitespace(guid) ? false : this.knownMediaLines.contains(guid.trim());
    }

    protected void saveKnownMediaLines(ByteBuffer bb) {
        bb.putShort((short)this.knownMediaLines.size());

        for (String guid : this.knownMediaLines) {
            GameWindow.WriteStringUTF(bb, guid);
        }
    }

    protected void loadKnownMediaLines(ByteBuffer bb, int WorldVersion) {
        this.knownMediaLines.clear();
        int count = bb.getShort();

        for (int i = 0; i < count; i++) {
            String guid = GameWindow.ReadStringUTF(bb);
            this.knownMediaLines.add(guid);
        }
    }

    @Override
    public boolean isMoving() {
        return this instanceof IsoPlayer && !((IsoPlayer)this).isAttackAnimThrowTimeOut() ? false : this.isMoving;
    }

    public boolean isBehaviourMoving() {
        State currentState = this.getCurrentState();
        return currentState != null && currentState.isMoving(this);
    }

    public boolean isPlayerMoving() {
        return false;
    }

    public void setMoving(boolean val) {
        this.isMoving = val;
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).remote) {
            ((IsoPlayer)this).isPlayerMoving = val;
            ((IsoPlayer)this).setJustMoved(val);
        }
    }

    private boolean isFacingNorthWesterly() {
        return this.dir == IsoDirections.W || this.dir == IsoDirections.NW || this.dir == IsoDirections.N || this.dir == IsoDirections.NE;
    }

    public boolean isAttacking() {
        return false;
    }

    public boolean isZombieAttacking() {
        return false;
    }

    public boolean isZombieAttacking(IsoMovingObject other) {
        return false;
    }

    private boolean isZombieThumping() {
        return this.isZombie() ? this.getCurrentState() == ThumpState.instance() : false;
    }

    public int compareMovePriority(IsoGameCharacter other) {
        if (other == null) {
            return 1;
        } else if (this.isZombieThumping() && !other.isZombieThumping()) {
            return 1;
        } else if (!this.isZombieThumping() && other.isZombieThumping()) {
            return -1;
        } else if (other instanceof IsoPlayer) {
            return GameClient.client && this.isZombieAttacking(other) ? -1 : 0;
        } else if (this.isZombieAttacking() && !other.isZombieAttacking()) {
            return 1;
        } else if (!this.isZombieAttacking() && other.isZombieAttacking()) {
            return -1;
        } else if (this.isBehaviourMoving() && !other.isBehaviourMoving()) {
            return 1;
        } else if (!this.isBehaviourMoving() && other.isBehaviourMoving()) {
            return -1;
        } else if (this.isFacingNorthWesterly() && !other.isFacingNorthWesterly()) {
            return 1;
        } else {
            return !this.isFacingNorthWesterly() && other.isFacingNorthWesterly() ? -1 : 0;
        }
    }

    @Override
    public long playSound(String file) {
        return this.getEmitter().playSound(file);
    }

    @Override
    public long playSoundLocal(String file) {
        return this.getEmitter().playSoundImpl(file, null);
    }

    @Override
    public void stopOrTriggerSound(long eventInstance) {
        this.getEmitter().stopOrTriggerSound(eventInstance);
    }

    public long playDropItemSound(InventoryItem item) {
        if (item == null) {
            return 0L;
        } else {
            String sound = item.getDropSound();
            if (sound == null && item instanceof InventoryContainer) {
                sound = "DropBag";
            }

            return sound == null ? 0L : this.playSound(sound);
        }
    }

    public long playWeaponHitArmourSound(int partIndex, boolean bullet) {
        this.getItemVisuals(tempItemVisuals);

        for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null) {
                String sound = bullet ? scriptItem.getBulletHitArmourSound() : scriptItem.getWeaponHitArmourSound();
                if (sound != null) {
                    ArrayList<BloodClothingType> types = scriptItem.getBloodClothingType();
                    if (types != null) {
                        ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);
                        if (coveredParts != null) {
                            for (int j = 0; j < coveredParts.size(); j++) {
                                if (coveredParts.get(j).index() == partIndex) {
                                    if (GameServer.server) {
                                        INetworkPacket.sendToRelative(
                                            PacketTypes.PacketType.PlaySound, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), sound, false, this
                                        );
                                        return 0L;
                                    }

                                    return this.playSoundLocal(sound);
                                }
                            }
                        }
                    }
                }
            }
        }

        return 0L;
    }

    @Override
    public void addWorldSoundUnlessInvisible(int radius, int volume, boolean bStressHumans) {
        if (!this.isInvisible()) {
            WorldSoundManager.instance
                .addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), radius, volume, bStressHumans);
        }
    }

    @Override
    public boolean isKnownPoison(InventoryItem item) {
        if (item.hasTag(ItemTag.NO_DETECT)) {
            return false;
        } else if (item.hasTag(ItemTag.SHOW_POISON)) {
            return true;
        } else if (item instanceof Food food) {
            if (food.getPoisonPower() <= 0) {
                return false;
            } else if (food.getHerbalistType() != null && !food.getHerbalistType().isEmpty()) {
                return this.isRecipeActuallyKnown("Herbalist");
            } else {
                return food.getPoisonDetectionLevel() >= 0 && this.getPerkLevel(PerkFactory.Perks.Cooking) >= 10 - food.getPoisonDetectionLevel()
                    ? true
                    : this.getFullName().equals(food.getModData().rawget("addedPoisonBy")) && this.isRecipeActuallyKnown("Herbalist");
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isKnownPoison(Item item) {
        if (item.hasTag(ItemTag.SHOW_POISON)) {
            return true;
        } else if (item.isItemType(ItemType.FOOD)) {
            if (item.getPoisonPower() <= 0.0F) {
                return false;
            } else if (item.getHerbalistType() != null && !item.getHerbalistType().isEmpty()) {
                return this.isRecipeActuallyKnown("Herbalist");
            } else {
                return item.getPoisonDetectionLevel() >= 0 && this.getPerkLevel(PerkFactory.Perks.Cooking) >= 10 - item.getPoisonDetectionLevel()
                    ? true
                    : item.getPoisonDetectionLevel() >= 0;
            }
        } else {
            return false;
        }
    }

    @Override
    public int getLastHourSleeped() {
        return this.lastHourSleeped;
    }

    @Override
    public void setLastHourSleeped(int lastHourSleeped) {
        this.lastHourSleeped = lastHourSleeped;
    }

    @Override
    public void setTimeOfSleep(float timeOfSleep) {
        this.timeOfSleep = timeOfSleep;
    }

    public void setDelayToSleep(float delay) {
        this.delayToActuallySleep = delay;
    }

    @Override
    public String getBedType() {
        return this.bedType;
    }

    @Override
    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public void enterVehicle(BaseVehicle v, int seat, Vector3f offset) {
        if (this.vehicle != null) {
            this.vehicle.exit(this);
        }

        if (v != null) {
            v.enter(seat, this, offset);
        }
    }

    @Override
    public float Hit(BaseVehicle vehicle, float speed, boolean isHitFromBehind, float hitDirX, float hitDirY) {
        this.setHitFromBehind(isHitFromBehind);
        if (GameClient.client) {
            this.setAttackedBy(GameClient.IDToPlayerMap.get(vehicle.getNetPlayerId()));
        } else if (GameServer.server) {
            this.setAttackedBy(GameServer.IDToPlayerMap.get(vehicle.getNetPlayerId()));
        } else {
            this.setAttackedBy(vehicle.getDriverRegardlessOfTow());
        }

        this.getHitDir().set(hitDirX, hitDirY);
        if (!this.isKnockedDown()) {
            this.setHitForce(Math.max(0.5F, speed * 0.15F));
        } else {
            this.setHitForce(Math.min(2.5F, speed * 0.15F));
        }

        if (GameClient.client) {
            HitReactionNetworkAI.CalcHitReactionVehicle(this, vehicle);
        }

        return this.getHealth();
    }

    @Override
    public Path getPath2() {
        return this.path2;
    }

    @Override
    public void setPath2(Path path) {
        this.path2 = path;
    }

    @Override
    public PathFindBehavior2 getPathFindBehavior2() {
        return this.pfb2;
    }

    public MapKnowledge getMapKnowledge() {
        return this.mapKnowledge;
    }

    @Override
    public IsoObject getBed() {
        return this.bed;
    }

    @Override
    public void setBed(IsoObject bed) {
        this.bed = bed;
    }

    public boolean avoidDamage() {
        return this.avoidDamage;
    }

    public void setAvoidDamage(boolean avoid) {
        this.avoidDamage = avoid;
    }

    @Override
    public boolean isReading() {
        return this.isReading;
    }

    @Override
    public void setReading(boolean isReading) {
        this.isReading = isReading;
    }

    @Override
    public float getTimeSinceLastSmoke() {
        return this.timeSinceLastSmoke;
    }

    @Override
    public void setTimeSinceLastSmoke(float timeSinceLastSmoke) {
        this.timeSinceLastSmoke = PZMath.clamp(timeSinceLastSmoke, 0.0F, 10.0F);
    }

    @Override
    public boolean isInvisible() {
        return this.getCheats().isSet(CheatType.INVISIBLE);
    }

    @Override
    public void setInvisible(boolean b) {
        if (!Role.hasCapability(this, Capability.ToggleInvisibleHimself)) {
            this.getCheats().set(CheatType.INVISIBLE, false);
        } else {
            this.getCheats().set(CheatType.INVISIBLE, b);
        }
    }

    public void setInvisible(boolean b, boolean isForced) {
        if (!isForced) {
            this.setInvisible(b);
        } else {
            this.getCheats().set(CheatType.INVISIBLE, b);
        }
    }

    public boolean isCanUseBrushTool() {
        return this.getCheats().isSet(CheatType.BRUSH_TOOL);
    }

    public void setCanUseBrushTool(boolean b) {
        if (!Role.hasCapability(this, Capability.UseBrushToolManager)) {
            this.getCheats().set(CheatType.BRUSH_TOOL, false);
        } else {
            this.getCheats().set(CheatType.BRUSH_TOOL, b);
        }
    }

    public boolean canUseLootTool() {
        return this.getCheats().isSet(CheatType.LOOT_TOOL);
    }

    public void setCanUseLootTool(boolean b) {
        if (!Role.hasCapability(this, Capability.UseLootTool)) {
            this.getCheats().set(CheatType.LOOT_TOOL, false);
        } else {
            this.getCheats().set(CheatType.LOOT_TOOL, b);
        }
    }

    public boolean canUseDebugContextMenu() {
        return this.getCheats().isSet(CheatType.DEBUG_CONTEXT_MENU);
    }

    public void setCanUseDebugContextMenu(boolean b) {
        if (!Role.hasCapability(this, Capability.UseDebugContextMenu)) {
            this.getCheats().set(CheatType.DEBUG_CONTEXT_MENU, false);
        } else {
            this.getCheats().set(CheatType.DEBUG_CONTEXT_MENU, b);
        }
    }

    @Override
    public boolean isDriving() {
        return this.getVehicle() != null
            && this.getVehicle().getDriver() == this
            && this.getVehicle().getController() != null
            && !this.getVehicle().isStopped();
    }

    @Override
    public boolean isInARoom() {
        return this.square != null && this.square.isInARoom();
    }

    @Override
    public boolean isGodMod() {
        return this.getCheats().isSet(CheatType.GOD_MODE);
    }

    public boolean isInvulnerable() {
        return this.getCheats().isSet(CheatType.GOD_MODE);
    }

    public void setInvulnerable(boolean invulnerable) {
        this.getCheats().set(CheatType.GOD_MODE, invulnerable);
    }

    public void setZombiesDontAttack(boolean b) {
        if (!Role.hasCapability(this, Capability.UseZombieDontAttackCheat)) {
            this.getCheats().set(CheatType.ZOMBIES_DONT_ATTACK, false);
        } else {
            this.getCheats().set(CheatType.ZOMBIES_DONT_ATTACK, b);
        }
    }

    public boolean isZombiesDontAttack() {
        return this.getCheats().isSet(CheatType.ZOMBIES_DONT_ATTACK);
    }

    public void setGodMod(boolean b, boolean isForced) {
        if (!isForced) {
            this.setGodMod(b);
        } else {
            if (!this.isDead()) {
                this.getCheats().set(CheatType.GOD_MODE, b);
            }
        }
    }

    @Override
    public void setGodMod(boolean b) {
        if (!Role.hasCapability(this, Capability.ToggleGodModHimself)) {
            this.getCheats().set(CheatType.GOD_MODE, false);
        } else {
            if (!this.isDead()) {
                this.getCheats().set(CheatType.GOD_MODE, b);
            }
        }
    }

    @Override
    public boolean isUnlimitedCarry() {
        return this.getCheats().isSet(CheatType.UNLIMITED_CARRY);
    }

    @Override
    public void setUnlimitedCarry(boolean unlimitedCarry) {
        if (!Role.hasCapability(this, Capability.ToggleUnlimitedCarry)) {
            this.getCheats().set(CheatType.UNLIMITED_CARRY, false);
        } else {
            this.getCheats().set(CheatType.UNLIMITED_CARRY, unlimitedCarry);
        }
    }

    @Override
    public boolean isBuildCheat() {
        return this.getCheats().isSet(CheatType.BUILD);
    }

    @Override
    public void setBuildCheat(boolean buildCheat) {
        if (!Role.hasCapability(this, Capability.UseBuildCheat)) {
            this.getCheats().set(CheatType.BUILD, false);
        } else {
            this.getCheats().set(CheatType.BUILD, buildCheat);
        }
    }

    @Override
    public boolean isFarmingCheat() {
        return this.getCheats().isSet(CheatType.FARMING);
    }

    @Override
    public void setFarmingCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseFarmingCheat)) {
            this.getCheats().set(CheatType.FARMING, false);
        } else {
            this.getCheats().set(CheatType.FARMING, b);
        }
    }

    @Override
    public boolean isFishingCheat() {
        return this.getCheats().isSet(CheatType.FISHING);
    }

    @Override
    public void setFishingCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseFishingCheat)) {
            this.getCheats().set(CheatType.FISHING, false);
        } else {
            this.getCheats().set(CheatType.FISHING, b);
        }
    }

    @Override
    public boolean isHealthCheat() {
        return this.getCheats().isSet(CheatType.HEALTH);
    }

    @Override
    public void setHealthCheat(boolean healthCheat) {
        if (!Role.hasCapability(this, Capability.UseHealthCheat)) {
            this.getCheats().set(CheatType.HEALTH, false);
        } else {
            this.getCheats().set(CheatType.HEALTH, healthCheat);
        }
    }

    @Override
    public boolean isMechanicsCheat() {
        return this.getCheats().isSet(CheatType.MECHANICS);
    }

    @Override
    public void setMechanicsCheat(boolean mechanicsCheat) {
        if (!Role.hasCapability(this, Capability.UseMechanicsCheat)) {
            this.getCheats().set(CheatType.MECHANICS, false);
        } else {
            this.getCheats().set(CheatType.MECHANICS, mechanicsCheat);
        }
    }

    public boolean isFastMoveCheat() {
        return this.getCheats().isSet(CheatType.FAST_MOVE);
    }

    public void setFastMoveCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseFastMoveCheat)) {
            this.getCheats().set(CheatType.FAST_MOVE, false);
        } else {
            this.getCheats().set(CheatType.FAST_MOVE, b);
        }
    }

    @Override
    public boolean isMovablesCheat() {
        return this.getCheats().isSet(CheatType.MOVABLES);
    }

    @Override
    public void setMovablesCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseMovablesCheat)) {
            this.getCheats().set(CheatType.MOVABLES, false);
        } else {
            this.getCheats().set(CheatType.MOVABLES, b);
        }
    }

    @Override
    public boolean isAnimalCheat() {
        return this.getCheats().isSet(CheatType.ANIMAL);
    }

    @Override
    public void setAnimalCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.AnimalCheats)) {
            this.getCheats().set(CheatType.ANIMAL, false);
        } else {
            this.getCheats().set(CheatType.ANIMAL, b);
        }
    }

    @Override
    public boolean isTimedActionInstantCheat() {
        return this.getCheats().isSet(CheatType.TIMED_ACTION_INSTANT);
    }

    @Override
    public void setTimedActionInstantCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseTimedActionInstantCheat)) {
            this.getCheats().set(CheatType.TIMED_ACTION_INSTANT, false);
        } else {
            this.getCheats().set(CheatType.TIMED_ACTION_INSTANT, b);
        }
    }

    @Override
    public boolean isTimedActionInstant() {
        return Core.debug && DebugOptions.instance.cheat.timedAction.instant.getValue() ? true : this.isTimedActionInstantCheat();
    }

    @Override
    public boolean isShowAdminTag() {
        return this.showAdminTag;
    }

    @Override
    public void setShowAdminTag(boolean showAdminTag) {
        this.showAdminTag = showAdminTag;
    }

    /**
     * Description copied from interface: IAnimationVariableSource
     */
    @Override
    public Iterable<IAnimationVariableSlot> getGameVariables() {
        ActionContext actionContext = this.getActionContext();
        return actionContext != null && actionContext.hasStateVariables() ? () -> new Iterator<IAnimationVariableSlot>() {
            private final Iterator<AnimationVariableHandle> iterator;
            private IAnimationVariableSlot nextSlot;

            {
                Objects.requireNonNull(IsoGameCharacter.this);
                this.iterator = AnimationVariableHandlePool.all().iterator();
                this.nextSlot = this.findNextSlot();
            }

            @Override
            public boolean hasNext() {
                return this.nextSlot != null;
            }

            public IAnimationVariableSlot next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                } else {
                    IAnimationVariableSlot currentSlot = this.nextSlot;
                    this.nextSlot = this.findNextSlot();
                    return currentSlot;
                }
            }

            private IAnimationVariableSlot findNextSlot() {
                IAnimationVariableSlot nextSlot = null;

                while (this.iterator.hasNext()) {
                    AnimationVariableHandle nextHandle = this.iterator.next();
                    IAnimationVariableSlot slot = IsoGameCharacter.this.getVariable(nextHandle);
                    if (slot != null) {
                        nextSlot = slot;
                        break;
                    }
                }

                return nextSlot;
            }
        } : this.getGameVariablesInternal().getGameVariables();
    }

    /**
     * Description copied from interface: IAnimationVariableSource
     */
    @Override
    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        ActionContext actionContext = this.getActionContext();
        if (actionContext != null) {
            IAnimationVariableSlot actionSlot = actionContext.getVariable(handle);
            if (actionSlot != null) {
                return actionSlot;
            }
        }

        return this.getGameVariablesInternal().getVariable(handle);
    }

    /**
     * Description copied from interface: IAnimationVariableMap
     */
    @Override
    public void setVariable(IAnimationVariableSlot var) {
        if (!GameServer.server || !(this instanceof IsoZombie)) {
            this.getGameVariablesInternal().setVariable(var);
        }
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, String value) {
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        } else {
            if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
                INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
            }

            return this.getGameVariablesInternal().setVariable(key, value);
        }
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, boolean value) {
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        } else {
            if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
                INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
            }

            return this.getGameVariablesInternal().setVariable(key, value);
        }
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, float value) {
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        } else {
            if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
                INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
            }

            return this.getGameVariablesInternal().setVariable(key, value);
        }
    }

    @Override
    public <EnumType extends Enum<EnumType>> IAnimationVariableSlot setVariableEnum(String in_key, EnumType in_val) {
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(in_key)) {
            INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, in_key, in_val);
        }

        return this.getGameVariablesInternal().setVariableEnum(in_key, in_val);
    }

    @Override
    public IAnimationVariableSlot setVariable(AnimationVariableHandle handle, boolean value) {
        String key = handle.getVariableName();
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        } else {
            if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
                INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
            }

            return this.getGameVariablesInternal().setVariable(handle, value);
        }
    }

    @Override
    public void clearVariable(String in_key) {
        this.getGameVariablesInternal().clearVariable(in_key);
    }

    @Override
    public void clearVariables() {
        this.getGameVariablesInternal().clearVariables();
    }

    private String getFootInjuryType() {
        if (!(this instanceof IsoPlayer)) {
            return "";
        } else {
            BodyPart footL = this.getBodyDamage().getBodyPart(BodyPartType.Foot_L);
            BodyPart footR = this.getBodyDamage().getBodyPart(BodyPartType.Foot_R);
            if (!this.running) {
                if (footL.haveBullet()
                    || footL.getBurnTime() > 5.0F
                    || footL.bitten()
                    || footL.deepWounded()
                    || footL.isSplint()
                    || footL.getFractureTime() > 0.0F
                    || footL.haveGlass()) {
                    return "leftheavy";
                }

                if (footR.haveBullet()
                    || footR.getBurnTime() > 5.0F
                    || footR.bitten()
                    || footR.deepWounded()
                    || footR.isSplint()
                    || footR.getFractureTime() > 0.0F
                    || footR.haveGlass()) {
                    return "rightheavy";
                }
            }

            if (footL.getScratchTime() > 5.0F || footL.getCutTime() > 7.0F || footL.getBurnTime() > 0.0F) {
                return "leftlight";
            } else {
                return !(footR.getScratchTime() > 5.0F) && !(footR.getCutTime() > 7.0F) && !(footR.getBurnTime() > 0.0F) ? "" : "rightlight";
            }
        }
    }

    @Override
    public IAnimationVariableSource getSubVariableSource(String in_subVariableSourceName) {
        if (in_subVariableSourceName.equals("GrappledTarget")) {
            return IGrappleable.getAnimatable(this.getGrapplingTarget());
        } else {
            return in_subVariableSourceName.equals("GrappledBy") ? IGrappleable.getAnimatable(this.getGrappledBy()) : null;
        }
    }

    @Override
    public AnimationVariableSource getGameVariablesInternal() {
        return this.playbackGameVariables != null ? this.playbackGameVariables : this.gameVariables;
    }

    public AnimationVariableSource startPlaybackGameVariables() {
        if (this.playbackGameVariables != null) {
            DebugLog.General.error("Error! PlaybackGameVariables is already active.");
            return this.playbackGameVariables;
        } else {
            AnimationVariableSource playbackVars = new AnimationVariableSource();

            for (IAnimationVariableSlot var : this.getGameVariables()) {
                AnimationVariableType varType = var.getType();
                switch (varType) {
                    case String:
                        playbackVars.setVariable(var.getKey(), var.getValueString());
                        break;
                    case Float:
                        playbackVars.setVariable(var.getKey(), var.getValueFloat());
                        break;
                    case Boolean:
                        playbackVars.setVariable(var.getKey(), var.getValueBool());
                    case Void:
                        break;
                    default:
                        DebugLog.General.error("Error! Variable type not handled: %s", varType.toString());
                }
            }

            this.playbackGameVariables = playbackVars;
            return this.playbackGameVariables;
        }
    }

    public void endPlaybackGameVariables(AnimationVariableSource playbackVars) {
        if (this.playbackGameVariables != playbackVars) {
            DebugLog.General.error("Error! Playback GameVariables do not match.");
        }

        this.playbackGameVariables = null;
    }

    public void playbackSetCurrentStateSnapshot(ActionStateSnapshot snapshot) {
        if (this.actionContext != null) {
            this.actionContext.setPlaybackStateSnapshot(snapshot);
        }
    }

    public ActionStateSnapshot playbackRecordCurrentStateSnapshot() {
        return this.actionContext == null ? null : this.actionContext.getPlaybackStateSnapshot();
    }

    @Override
    public String GetVariable(String key) {
        return this.getVariableString(key);
    }

    @Override
    public void SetVariable(String key, String value) {
        this.setVariable(key, value);
    }

    @Override
    public void ClearVariable(String key) {
        this.clearVariable(key);
    }

    @Override
    public void actionStateChanged(ActionContext sender) {
        for (int ii = 0; ii < IsoGameCharacter.L_actionStateChanged.stateNames.size(); ii++) {
            DebugLog.AnimationDetailed.debugln("************* stateNames: %s", IsoGameCharacter.L_actionStateChanged.stateNames.get(ii));
        }

        ArrayList<String> stateNames = IsoGameCharacter.L_actionStateChanged.stateNames;
        PZArrayUtil.listConvert(sender.getChildStates(), stateNames, state -> state.getName());

        for (int ii = 0; ii < IsoGameCharacter.L_actionStateChanged.stateNames.size(); ii++) {
            DebugLog.AnimationDetailed.debugln("************* stateNames: %s", IsoGameCharacter.L_actionStateChanged.stateNames.get(ii));
        }

        this.advancedAnimator.setState(sender.getCurrentStateName(), stateNames);

        try {
            this.stateMachine.activeStateChanged++;
            State aiState = this.tryGetAIState(sender.getCurrentStateName());
            if (aiState == null) {
                aiState = this.defaultState;
            }

            ArrayList<State> childStates = IsoGameCharacter.L_actionStateChanged.states;
            PZArrayUtil.listConvert(sender.getChildStates(), childStates, this.aiStateMap, (state2, l_lookup) -> l_lookup.get(state2.getName().toLowerCase()));
            this.stateMachine.changeState(aiState, childStates);
        } finally {
            this.stateMachine.activeStateChanged--;
        }
    }

    @Override
    public boolean isFallOnFront() {
        return this.fallOnFront;
    }

    @Override
    public void setFallOnFront(boolean fallOnFront) {
        this.fallOnFront = fallOnFront;
    }

    public boolean isHitFromBehind() {
        return this.hitFromBehind;
    }

    public void setHitFromBehind(boolean hitFromBehind) {
        this.hitFromBehind = hitFromBehind;
    }

    public boolean isKilledBySlicingWeapon() {
        if (this.damagedByVehicle) {
            return false;
        } else {
            IsoGameCharacter killer = this.getAttackedBy();
            if (killer == null) {
                return false;
            } else {
                HandWeapon weapon = killer.getAttackingWeapon();
                return weapon == null ? false : weapon.isOfWeaponCategory(WeaponCategory.LONG_BLADE);
            }
        }
    }

    @Override
    public void reportEvent(String name) {
        this.actionContext.reportEvent(name);
    }

    @Override
    public void StartTimedActionAnim(String event) {
        this.StartTimedActionAnim(event, null);
    }

    @Override
    public void StartTimedActionAnim(String event, String type) {
        this.reportEvent(event);
        if (type != null) {
            this.setVariable("TimedActionType", type);
        }

        this.resetModelNextFrame();
    }

    @Override
    public void StopTimedActionAnim() {
        this.clearVariable("TimedActionType");
        this.reportEvent("Event_TA_Exit");
        this.resetModelNextFrame();
    }

    public boolean hasHitReaction() {
        return !StringUtils.isNullOrEmpty(this.getHitReaction());
    }

    public String getHitReaction() {
        return this.hitReaction;
    }

    public void setHitReaction(String in_hitReaction) {
        if (!StringUtils.equals(this.hitReaction, in_hitReaction)) {
            this.hitReaction = in_hitReaction;
        }
    }

    public void CacheEquipped() {
        this.cacheEquiped[0] = this.getPrimaryHandItem();
        this.cacheEquiped[1] = this.getSecondaryHandItem();
    }

    public InventoryItem GetPrimaryEquippedCache() {
        return this.cacheEquiped[0] != null && this.inventory.contains(this.cacheEquiped[0]) ? this.cacheEquiped[0] : null;
    }

    public InventoryItem GetSecondaryEquippedCache() {
        return this.cacheEquiped[1] != null && this.inventory.contains(this.cacheEquiped[1]) ? this.cacheEquiped[1] : null;
    }

    public void ClearEquippedCache() {
        this.cacheEquiped[0] = null;
        this.cacheEquiped[1] = null;
    }

    public boolean isObjectBehind(IsoObject obj) {
        Vector2 oPos = tempVector2_1.set(obj.getX(), obj.getY());
        Vector2 tPos = tempVector2_2.set(this.getX(), this.getY());
        tPos.x = tPos.x - oPos.x;
        tPos.y = tPos.y - oPos.y;
        Vector2 dir = this.getForwardDirection();
        tPos.normalize();
        dir.normalize();
        float dot = tPos.dot(dir);
        return dot > 0.6F;
    }

    public boolean isBehind(IsoGameCharacter chr) {
        Vector2 oPos = tempVector2_1.set(this.getX(), this.getY());
        Vector2 tPos = tempVector2_2.set(chr.getX(), chr.getY());
        tPos.x = tPos.x - oPos.x;
        tPos.y = tPos.y - oPos.y;
        Vector2 dir = chr.getForwardDirection();
        tPos.normalize();
        dir.normalize();
        float dot = tPos.dot(dir);
        return dot > 0.6F;
    }

    public void resetEquippedHandsModels() {
        if (!GameServer.server || ServerGUI.isCreated()) {
            if (this.hasActiveModel()) {
                ModelManager.instance.ResetEquippedNextFrame(this);
            }
        }
    }

    @Override
    public AnimatorDebugMonitor getDebugMonitor() {
        return this.advancedAnimator.getDebugMonitor();
    }

    @Override
    public void setDebugMonitor(AnimatorDebugMonitor monitor) {
        this.advancedAnimator.setDebugMonitor(monitor);
    }

    public boolean isAimAtFloor() {
        return this.aimAtFloor;
    }

    public void setAimAtFloor(boolean in_aimAtFloor) {
        this.setAimAtFloor(in_aimAtFloor, 0.0F);
    }

    public void setAimAtFloor(boolean in_aimAtFloor, float in_targetDistance) {
        this.aimAtFloor = in_aimAtFloor;
        this.aimAtFloorTargetDistance = in_targetDistance;
    }

    public float aimAtFloorTargetDistance() {
        return this.aimAtFloorTargetDistance;
    }

    public float getAimAtFloorAmount() {
        float aimAngle = this.getCurrentVerticalAimAngle();
        if (PZMath.equal(aimAngle, 0.0F, 0.1F)) {
            return 0.0F;
        } else {
            float aimAngleFrac = -aimAngle / 90.0F;
            return PZMath.clamp(aimAngleFrac, 0.0F, 1.0F);
        }
    }

    public float getCurrentVerticalAimAngle() {
        return this.currentVerticalAimAngleDegrees;
    }

    public void setCurrentVerticalAimAngle(float in_verticalAimAngleDegrees) {
        this.currentVerticalAimAngleDegrees = in_verticalAimAngleDegrees;
    }

    public void setTargetVerticalAimAngle(float in_verticalAimAngleDegrees) {
        float wrappedAngle = PZMath.wrap(in_verticalAimAngleDegrees, -180.0F, 180.0F);
        float clampedAngle;
        if (wrappedAngle < -90.0F) {
            clampedAngle = -180.0F - wrappedAngle;
        } else if (wrappedAngle > 90.0F) {
            clampedAngle = 180.0F - wrappedAngle;
        } else {
            clampedAngle = wrappedAngle;
        }

        this.targetVerticalAimAngleDegrees = clampedAngle;
    }

    public float getTargetVerticalAimAngle() {
        return this.targetVerticalAimAngleDegrees;
    }

    public boolean isDeferredMovementEnabled() {
        return this.deferredMovementEnabled;
    }

    public void setDeferredMovementEnabled(boolean deferredMovementEnabled) {
        this.deferredMovementEnabled = deferredMovementEnabled;
    }

    public String testDotSide(IsoMovingObject target) {
        Vector2 zombieLookVector = this.getLookVector(IsoGameCharacter.l_testDotSide.v1);
        Vector2 zombiePos = IsoGameCharacter.l_testDotSide.v2.set(this.getX(), this.getY());
        Vector2 zombieToPlayer = IsoGameCharacter.l_testDotSide.v3.set(target.getX() - zombiePos.x, target.getY() - zombiePos.y);
        zombieToPlayer.normalize();
        float dotZombieToPlayer = Vector2.dot(zombieToPlayer.x, zombieToPlayer.y, zombieLookVector.x, zombieLookVector.y);
        if (dotZombieToPlayer > 0.7) {
            return "FRONT";
        } else if (dotZombieToPlayer < 0.0F && dotZombieToPlayer < -0.5) {
            return "BEHIND";
        } else {
            float px = target.getX();
            float py = target.getY();
            float x1 = zombiePos.x;
            float y1 = zombiePos.y;
            float x2 = zombiePos.x + zombieLookVector.x;
            float y2 = zombiePos.y + zombieLookVector.y;
            float d = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1);
            return d > 0.0F ? "RIGHT" : "LEFT";
        }
    }

    public void addBasicPatch(BloodBodyPartType part) {
        if (this instanceof IHumanVisual) {
            if (part == null) {
                part = BloodBodyPartType.FromIndex(Rand.Next(0, BloodBodyPartType.MAX.index()));
            }

            HumanVisual humanVisual = ((IHumanVisual)this).getHumanVisual();
            this.getItemVisuals(tempItemVisuals);
            BloodClothingType.addBasicPatch(part, humanVisual, tempItemVisuals);
            this.updateModelTextures = true;
            this.updateEquippedTextures = true;
            if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
        }
    }

    @Override
    public boolean addHole(BloodBodyPartType part) {
        return this.addHole(part, false);
    }

    public boolean addHole(BloodBodyPartType part, boolean allLayers) {
        if (!(this instanceof IHumanVisual)) {
            return false;
        } else {
            if (part == null) {
                part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            }

            HumanVisual humanVisual = ((IHumanVisual)this).getHumanVisual();
            this.getItemVisuals(tempItemVisuals);
            boolean addedHole = BloodClothingType.addHole(part, humanVisual, tempItemVisuals, allLayers);
            this.updateModelTextures = true;
            if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
                if (GameClient.client) {
                    INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                    INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
                }
            }

            return addedHole;
        }
    }

    public void addDirt(BloodBodyPartType part, Integer nbr, boolean allLayers) {
        if (!(this instanceof IsoAnimal)) {
            HumanVisual humanVisual = ((IHumanVisual)this).getHumanVisual();
            if (nbr == null) {
                nbr = OutfitRNG.Next(5, 10);
            }

            boolean randomPart = false;
            if (part == null) {
                randomPart = true;
            }

            this.getItemVisuals(tempItemVisuals);

            for (int i = 0; i < nbr; i++) {
                if (randomPart) {
                    part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
                }

                BloodClothingType.addDirt(part, humanVisual, tempItemVisuals, allLayers);
            }

            this.updateModelTextures = true;
            if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
        }
    }

    public void addLotsOfDirt(BloodBodyPartType part, Integer nbr, boolean allLayers) {
        if (!(this instanceof IsoAnimal)) {
            HumanVisual humanVisual = ((IHumanVisual)this).getHumanVisual();
            if (nbr == null) {
                nbr = OutfitRNG.Next(5, 10);
            }

            boolean randomPart = false;
            if (part == null) {
                randomPart = true;
            }

            this.getItemVisuals(tempItemVisuals);

            for (int i = 0; i < nbr; i++) {
                if (randomPart) {
                    part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
                }

                BloodClothingType.addDirt(part, Rand.Next(0.01F, 1.0F), humanVisual, tempItemVisuals, allLayers);
            }

            this.updateModelTextures = true;
            if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
        }
    }

    @Override
    public void addBlood(BloodBodyPartType part, boolean scratched, boolean bitten, boolean allLayers) {
        if (!(this instanceof IsoAnimal)) {
            HumanVisual humanVisual = ((IHumanVisual)this).getHumanVisual();
            int nbr = 1;
            boolean randomPart = false;
            if (part == null) {
                randomPart = true;
            }

            if (this.getPrimaryHandItem() instanceof HandWeapon) {
                nbr = ((HandWeapon)this.getPrimaryHandItem()).getSplatNumber();
                if (OutfitRNG.Next(15) < this.getWeaponLevel()) {
                    nbr--;
                }
            }

            if (bitten) {
                nbr = 20;
            }

            if (scratched) {
                nbr = 5;
            }

            if (this.isZombie()) {
                nbr += 8;
            }

            this.getItemVisuals(tempItemVisuals);

            for (int i = 0; i < nbr; i++) {
                if (randomPart) {
                    part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
                    if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon weapon && weapon.getBloodLevel() < 1.0F) {
                        float bloodLevel = weapon.getBloodLevel() + 0.02F;
                        weapon.setBloodLevel(bloodLevel);
                        this.updateEquippedTextures = true;
                    }
                }

                BloodClothingType.addBlood(part, humanVisual, tempItemVisuals, allLayers);
            }

            this.updateModelTextures = true;
            if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
        }
    }

    private boolean bodyPartHasTag(Integer part, ItemTag itemTag) {
        this.getItemVisuals(tempItemVisuals);

        for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null) {
                ArrayList<BloodClothingType> types = scriptItem.getBloodClothingType();
                if (types != null) {
                    ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);
                    if (coveredParts != null) {
                        InventoryItem item = itemVisual.getInventoryItem();
                        if (item == null) {
                            item = InventoryItemFactory.CreateItem(itemVisual.getItemType());
                            if (item == null) {
                                continue;
                            }
                        }

                        for (int j = 0; j < coveredParts.size(); j++) {
                            if (item instanceof Clothing
                                && coveredParts.get(j).index() == part
                                && itemVisual.getHole(coveredParts.get(j)) == 0.0F
                                && item.hasTag(itemTag)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean bodyPartIsSpiked(Integer part) {
        return this.bodyPartHasTag(part, ItemTag.SPIKED);
    }

    public boolean bodyPartIsSpikedBehind(Integer part) {
        return this.bodyPartHasTag(part, ItemTag.SPIKED_BEHIND);
    }

    public float getBodyPartClothingDefense(Integer part, boolean bite, boolean bullet) {
        float result = 0.0F;
        this.getItemVisuals(tempItemVisuals);

        for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null) {
                ArrayList<BloodClothingType> types = scriptItem.getBloodClothingType();
                if (types != null) {
                    ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);
                    if (coveredParts != null) {
                        InventoryItem item = itemVisual.getInventoryItem();
                        if (item == null) {
                            item = InventoryItemFactory.CreateItem(itemVisual.getItemType());
                            if (item == null) {
                                continue;
                            }
                        }

                        for (int j = 0; j < coveredParts.size(); j++) {
                            if (item instanceof Clothing clothing && coveredParts.get(j).index() == part && itemVisual.getHole(coveredParts.get(j)) == 0.0F) {
                                result += clothing.getDefForPart(coveredParts.get(j), bite, bullet);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return Math.min(100.0F, result);
    }

    @Override
    public boolean isBumped() {
        return !StringUtils.isNullOrWhitespace(this.getBumpType());
    }

    public boolean isBumpDone() {
        return this.isBumpDone;
    }

    public void setBumpDone(boolean val) {
        this.isBumpDone = val;
    }

    public boolean isBumpFall() {
        return this.bumpFall;
    }

    public void setBumpFall(boolean val) {
        this.bumpFall = val;
    }

    public boolean isBumpStaggered() {
        return this.bumpStaggered;
    }

    public void setBumpStaggered(boolean val) {
        this.bumpStaggered = val;
    }

    @Override
    public String getBumpType() {
        return this.bumpType;
    }

    public void setBumpType(String bumpType) {
        if (StringUtils.equalsIgnoreCase(this.bumpType, bumpType)) {
            this.bumpType = bumpType;
        } else {
            boolean wasBumped = this.isBumped();
            this.bumpType = bumpType;
            boolean isBumped = this.isBumped();
            if (isBumped != wasBumped) {
                this.setBumpStaggered(isBumped);
            }
        }
    }

    public String getBumpFallType() {
        return this.bumpFallType;
    }

    public void setBumpFallType(String val) {
        this.bumpFallType = val;
    }

    public IsoGameCharacter getBumpedChr() {
        return this.bumpedChr;
    }

    public void setBumpedChr(IsoGameCharacter bumpedChr) {
        this.bumpedChr = bumpedChr;
    }

    public long getLastBump() {
        return this.lastBump;
    }

    public void setLastBump(long lastBump) {
        this.lastBump = lastBump;
    }

    public boolean isSitOnGround() {
        return this.sitOnGround;
    }

    public void setSitOnGround(boolean sitOnGround) {
        this.sitOnGround = sitOnGround;
    }

    public boolean isSittingOnFurniture() {
        return this.isSitOnFurniture;
    }

    public void setSittingOnFurniture(boolean isSittingOnFurniture) {
        this.isSitOnFurniture = isSittingOnFurniture;
    }

    public IsoObject getSitOnFurnitureObject() {
        return this.sitOnFurnitureObject;
    }

    public void setSitOnFurnitureObject(IsoObject object) {
        this.sitOnFurnitureObject = object;
    }

    public IsoDirections getSitOnFurnitureDirection() {
        return this.sitOnFurnitureDirection;
    }

    public void setSitOnFurnitureDirection(IsoDirections dir) {
        this.sitOnFurnitureDirection = dir;
    }

    public boolean isSitOnFurnitureObject(IsoObject object) {
        IsoObject sitOnObject = this.getSitOnFurnitureObject();
        if (sitOnObject == null) {
            return false;
        } else {
            return object == sitOnObject ? true : sitOnObject.isConnectedSpriteGridObject(object);
        }
    }

    @Override
    public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare square) {
        return this.getSitOnFurnitureObject() != null && this.getSitOnFurnitureObject().getSquare() == square
            ? true
            : this.hasPath() && this.getPathFindBehavior2().shouldIgnoreCollisionWithSquare(square);
    }

    public boolean canStandAt(float x, float y, float z) {
        int flags = 17;
        boolean bCloseToWalls = false;
        return PolygonalMap2.instance.canStandAt(x, y, PZMath.fastfloor(z), null, flags);
    }

    @Override
    public String getUID() {
        return this.uid;
    }

    protected void clearAIStateMap() {
        this.aiStateMap.clear();
    }

    protected void registerAIState(String in_name, State in_aiState) {
        this.aiStateMap.put(in_name.toLowerCase(Locale.ENGLISH), in_aiState);
    }

    public State tryGetAIState(String in_stateName) {
        return this.aiStateMap.get(in_stateName.toLowerCase(Locale.ENGLISH));
    }

    public boolean isRunning() {
        return this.getMoodles() != null && this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) >= 3 ? false : this.running;
    }

    public void setRunning(boolean bRunning) {
        this.running = bRunning;
    }

    public boolean isSprinting() {
        return this.sprinting && !this.canSprint() ? false : this.sprinting;
    }

    public void setSprinting(boolean bSprinting) {
        this.sprinting = bSprinting;
    }

    public boolean canSprint() {
        if (this instanceof IsoPlayer && !((IsoPlayer)this).isAllowSprint()) {
            return false;
        } else if ("Tutorial".equals(Core.gameMode)) {
            return true;
        } else {
            InventoryItem item = this.getPrimaryHandItem();
            if (item != null && item.isEquippedNoSprint()) {
                return false;
            } else {
                item = this.getSecondaryHandItem();
                return item != null && item.isEquippedNoSprint()
                    ? false
                    : this.getMoodles() == null || this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) < 2;
            }
        }
    }

    public void postUpdateModelTextures() {
        this.updateModelTextures = true;
    }

    public ModelInstanceTextureCreator getTextureCreator() {
        return this.textureCreator;
    }

    public void setTextureCreator(ModelInstanceTextureCreator textureCreator) {
        this.textureCreator = textureCreator;
    }

    public void postUpdateEquippedTextures() {
        this.updateEquippedTextures = true;
    }

    public ArrayList<ModelInstance> getReadyModelData() {
        return this.readyModelData;
    }

    public boolean getIgnoreMovement() {
        return this.ignoreMovement;
    }

    public void setIgnoreMovement(boolean ignoreMovement) {
        if (this instanceof IsoPlayer && ignoreMovement) {
            ((IsoPlayer)this).networkAi.needToUpdate();
        }

        this.ignoreMovement = ignoreMovement;
    }

    public boolean isAutoWalk() {
        return this.autoWalk;
    }

    public void setAutoWalk(boolean b) {
        this.autoWalk = b;
    }

    public void setAutoWalkDirection(Vector2 v) {
        this.autoWalkDirection.set(v);
    }

    public Vector2 getAutoWalkDirection() {
        return this.autoWalkDirection;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public void setSneaking(boolean bSneaking) {
        this.sneaking = bSneaking;
    }

    public float getSneakLimpSpeedScale() {
        return this.sneakLimpSpeedScale;
    }

    public void setSneakLimpSpeedScale(float sneakLimpSpeedScale) {
        this.sneakLimpSpeedScale = sneakLimpSpeedScale;
    }

    public GameCharacterAIBrain getGameCharacterAIBrain() {
        return this.ai.brain;
    }

    public float getMoveDelta() {
        return this.moveDelta;
    }

    public void setMoveDelta(float moveDelta) {
        this.moveDelta = moveDelta;
    }

    public float getTurnDelta() {
        if (this.isSprinting()) {
            return this.turnDeltaSprinting;
        } else {
            return this.isRunning() ? this.turnDeltaRunning : this.turnDeltaNormal;
        }
    }

    public void setTurnDelta(float m_turnDelta) {
        this.turnDeltaNormal = m_turnDelta;
    }

    public float getChopTreeSpeed() {
        return this.characterTraits.get(CharacterTrait.AXEMAN) ? 1.0F : 0.8F;
    }

    /**
     * Test if we're able to defend a zombie bite
     *  Can only happen if zombie is attacking from front
     *  Calcul include current weapon skills, fitness & strength
     */
    public boolean testDefense(IsoZombie zomb) {
        if (this.testDotSide(zomb).equals("FRONT") && !zomb.crawling && this.getSurroundingAttackingZombies() <= 3) {
            int defendChance = 0;
            if ("KnifeDeath".equals(this.getVariableString("ZombieHitReaction"))) {
                defendChance += 30;
            }

            defendChance += this.getWeaponLevel() * 3;
            defendChance += this.getPerkLevel(PerkFactory.Perks.Fitness) * 2;
            defendChance += this.getPerkLevel(PerkFactory.Perks.Strength) * 2;
            defendChance -= this.getSurroundingAttackingZombies() * 5;
            defendChance -= this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 2;
            defendChance -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 2;
            defendChance -= this.getMoodles().getMoodleLevel(MoodleType.TIRED) * 3;
            defendChance -= this.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 2;
            if (SandboxOptions.instance.lore.strength.getValue() == 1) {
                defendChance -= 7;
            }

            if (SandboxOptions.instance.lore.strength.getValue() == 3) {
                defendChance += 7;
            }

            if (Rand.Next(100) < defendChance) {
                this.setAttackedBy(zomb);
                this.setHitReaction(zomb.getVariableString("PlayerHitReaction") + "Defended");
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int getSurroundingAttackingZombies() {
        return this.getSurroundingAttackingZombies(false);
    }

    public int getSurroundingAttackingZombies(boolean includeCrawlers) {
        movingStatic.clear();
        IsoGridSquare sq = this.getCurrentSquare();
        if (sq == null) {
            return 0;
        } else {
            movingStatic.addAll(sq.getMovingObjects());
            if (sq.n != null) {
                movingStatic.addAll(sq.n.getMovingObjects());
            }

            if (sq.s != null) {
                movingStatic.addAll(sq.s.getMovingObjects());
            }

            if (sq.e != null) {
                movingStatic.addAll(sq.e.getMovingObjects());
            }

            if (sq.w != null) {
                movingStatic.addAll(sq.w.getMovingObjects());
            }

            if (sq.nw != null) {
                movingStatic.addAll(sq.nw.getMovingObjects());
            }

            if (sq.sw != null) {
                movingStatic.addAll(sq.sw.getMovingObjects());
            }

            if (sq.se != null) {
                movingStatic.addAll(sq.se.getMovingObjects());
            }

            if (sq.ne != null) {
                movingStatic.addAll(sq.ne.getMovingObjects());
            }

            int count = 0;

            for (int i = 0; i < movingStatic.size(); i++) {
                IsoZombie zombie = Type.tryCastTo(movingStatic.get(i), IsoZombie.class);
                if (zombie != null
                    && zombie.target == this
                    && !(this.DistToSquared(zombie) >= 0.80999994F)
                    && (!zombie.isCrawling() || includeCrawlers)
                    && (
                        zombie.isCurrentState(AttackState.instance())
                            || zombie.isCurrentState(AttackNetworkState.instance())
                            || zombie.isCurrentState(LungeState.instance())
                            || zombie.isCurrentState(LungeNetworkState.instance())
                    )) {
                    count++;
                }
            }

            return count;
        }
    }

    public boolean checkIsNearVehicle() {
        for (int i = 0; i < IsoWorld.instance.currentCell.getVehicles().size(); i++) {
            BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(i);
            if (vehicle.DistTo(this) < 3.5F) {
                if (this.sneaking) {
                    this.setVariable("nearWallCrouching", true);
                }

                return true;
            }
        }

        return false;
    }

    public float checkIsNearWall() {
        if (this.sneaking && this.getCurrentSquare() != null) {
            IsoGridSquare nSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.N);
            IsoGridSquare sSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
            IsoGridSquare eSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
            IsoGridSquare wSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.W);
            float result = 0.0F;
            float result2 = 0.0F;
            if (nSq != null) {
                result = nSq.getGridSneakModifier(true);
                if (result > 1.0F) {
                    this.setVariable("nearWallCrouching", true);
                    return result;
                }
            }

            if (sSq != null) {
                result = sSq.getGridSneakModifier(false);
                result2 = sSq.getGridSneakModifier(true);
                if (result > 1.0F || result2 > 1.0F) {
                    this.setVariable("nearWallCrouching", true);
                    return result > 1.0F ? result : result2;
                }
            }

            if (eSq != null) {
                result = eSq.getGridSneakModifier(false);
                result2 = eSq.getGridSneakModifier(true);
                if (result > 1.0F || result2 > 1.0F) {
                    this.setVariable("nearWallCrouching", true);
                    return result > 1.0F ? result : result2;
                }
            }

            if (wSq != null) {
                result = wSq.getGridSneakModifier(false);
                result2 = wSq.getGridSneakModifier(true);
                if (result > 1.0F || result2 > 1.0F) {
                    this.setVariable("nearWallCrouching", true);
                    return result > 1.0F ? result : result2;
                }
            }

            result = this.getCurrentSquare().getGridSneakModifier(false);
            if (result > 1.0F) {
                this.setVariable("nearWallCrouching", true);
                return result;
            } else if (this instanceof IsoPlayer && ((IsoPlayer)this).isNearVehicle()) {
                this.setVariable("nearWallCrouching", true);
                return 6.0F;
            } else {
                this.setVariable("nearWallCrouching", false);
                return 0.0F;
            }
        } else {
            this.setVariable("nearWallCrouching", false);
            return 0.0F;
        }
    }

    public float getBeenSprintingFor() {
        return this.beenSprintingFor;
    }

    public void setBeenSprintingFor(float beenSprintingFor) {
        if (beenSprintingFor < 0.0F) {
            beenSprintingFor = 0.0F;
        }

        if (beenSprintingFor > 100.0F) {
            beenSprintingFor = 100.0F;
        }

        this.beenSprintingFor = beenSprintingFor;
    }

    public boolean isHideWeaponModel() {
        return this.hideWeaponModel;
    }

    public void setHideWeaponModel(boolean hideWeaponModel) {
        if (this.hideWeaponModel != hideWeaponModel) {
            this.hideWeaponModel = hideWeaponModel;
            this.resetEquippedHandsModels();
        }
    }

    public boolean isHideEquippedHandL() {
        return this.hideEquippedHandL;
    }

    public void setHideEquippedHandL(boolean hideEquippedHandL) {
        if (this.hideEquippedHandL != hideEquippedHandL) {
            this.hideEquippedHandL = hideEquippedHandL;
            this.resetEquippedHandsModels();
        }
    }

    public boolean isHideEquippedHandR() {
        return this.hideEquippedHandR;
    }

    public void setHideEquippedHandR(boolean hideEquippedHandR) {
        if (this.hideEquippedHandR != hideEquippedHandR) {
            this.hideEquippedHandR = hideEquippedHandR;
            this.resetEquippedHandsModels();
        }
    }

    public void setIsAiming(boolean in_isAiming) {
        this.isAiming = in_isAiming;
    }

    public void setFireMode(String in_fireMode) {
    }

    public String getFireMode() {
        return this.leftHandItem instanceof HandWeapon weapon ? weapon.getFireMode() : "";
    }

    @Override
    public boolean isAiming() {
        if (this.isNpc) {
            return this.NPCGetAiming();
        } else if (this.isPerformingHostileAnimation()) {
            return true;
        } else {
            return this.isIgnoringAimingInput() ? false : this.isAiming;
        }
    }

    @Override
    public boolean isTwisting() {
        float twist = this.getTargetTwist();
        float absTwist = PZMath.abs(twist);
        return absTwist > 1.0F;
    }

    @Override
    public boolean allowsTwist() {
        return false;
    }

    public float getShoulderTwistWeight() {
        if (this.isAiming()) {
            return 1.0F;
        } else {
            return this.isSneaking() ? 0.6F : 0.75F;
        }
    }

    @Override
    public void resetBeardGrowingTime() {
        this.beardGrowTiming = (float)this.getHoursSurvived();
        if (GameClient.client && this instanceof IsoPlayer) {
            GameClient.instance.sendVisual((IsoPlayer)this);
        }
    }

    @Override
    public void resetHairGrowingTime() {
        this.hairGrowTiming = (float)this.getHoursSurvived();
        if (GameClient.client && this instanceof IsoPlayer) {
            GameClient.instance.sendVisual((IsoPlayer)this);
        }
    }

    public void fallenOnKnees() {
        this.fallenOnKnees(false);
    }

    public void fallenOnKnees(boolean hardFall) {
        if (!(this instanceof IsoPlayer) || ((IsoPlayer)this).isLocalPlayer()) {
            if (!this.isGodMod()) {
                this.helmetFall(hardFall);
                BloodBodyPartType part = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.Hand_L.index(), BloodBodyPartType.Torso_Upper.index()));
                if (Rand.NextBool(2)) {
                    part = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperLeg_L.index(), BloodBodyPartType.Back.index()));
                }

                for (int i = 0; i < 4; i++) {
                    BloodBodyPartType part2 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.Hand_L.index(), BloodBodyPartType.Torso_Upper.index()));
                    if (Rand.NextBool(2)) {
                        part2 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperLeg_L.index(), BloodBodyPartType.Back.index()));
                    }

                    this.addDirt(part2, Rand.Next(2, 6), false);
                }

                if (DebugOptions.instance.character.debug.alwaysTripOverFence.getValue()) {
                    this.dropHandItems();
                }

                if (Rand.NextBool(4 + this.getPerkLevel(PerkFactory.Perks.Nimble))) {
                    if (Rand.NextBool(4)) {
                        this.dropHandItems();
                    }

                    this.addHole(part);
                    BodyPart bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(part.index()));
                    float defense = this.getBodyPartClothingDefense(part.index(), false, false);
                    if (Rand.Next(100) >= defense) {
                        this.addBlood(part, true, false, false);
                        if (bodyPart.scratched()) {
                            bodyPart.generateDeepWound();
                            if (this instanceof IsoPlayer player) {
                                player.playerVoiceSound("PainFromLacerate");
                            }
                        } else {
                            bodyPart.setScratched(true, true);
                            if (this instanceof IsoPlayer player) {
                                player.playerVoiceSound("PainFromScratch");
                            }
                        }
                    }
                }
            }
        }
    }

    public void addVisualDamage(String itemType) {
        this.addBodyVisualFromItemType("Base." + itemType);
    }

    public ItemVisual addBodyVisualFromItemType(String itemType) {
        if (this instanceof IHumanVisual iHumanVisual) {
            Item scriptItem = ScriptManager.instance.getItem(itemType);
            if (scriptItem == null) {
                return null;
            } else {
                ClothingItem clothingItem = scriptItem.getClothingItemAsset();
                if (clothingItem == null) {
                    return null;
                } else {
                    ClothingItemReference itemRef = new ClothingItemReference();
                    itemRef.itemGuid = clothingItem.guid;
                    itemRef.randomize();
                    ItemVisual itemVisual = new ItemVisual();
                    itemVisual.setItemType(itemType);
                    itemVisual.synchWithOutfit(itemRef);
                    if (!this.isDuplicateBodyVisual(itemVisual)) {
                        ItemVisuals itemVisuals = iHumanVisual.getHumanVisual().getBodyVisuals();
                        itemVisuals.add(itemVisual);
                        return itemVisual;
                    } else {
                        return null;
                    }
                }
            }
        } else {
            return null;
        }
    }

    protected boolean isDuplicateBodyVisual(ItemVisual itemVisual) {
        if (this instanceof IHumanVisual iHumanVisual) {
            ItemVisuals itemVisuals = iHumanVisual.getHumanVisual().getBodyVisuals();

            for (int i = 0; i < itemVisuals.size(); i++) {
                ItemVisual itemVisual2 = itemVisuals.get(i);
                if (itemVisual.getClothingItemName().equals(itemVisual2.getClothingItemName())
                    && itemVisual.getTextureChoice() == itemVisual2.getTextureChoice()
                    && itemVisual.getBaseTexture() == itemVisual2.getBaseTexture()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean isCriticalHit() {
        return this.isCrit;
    }

    public void setCriticalHit(boolean isCrit) {
        this.isCrit = isCrit;
    }

    public float getRunSpeedModifier() {
        return this.runSpeedModifier;
    }

    public boolean isNPC() {
        return this.isNpc;
    }

    public void setNPC(boolean newvalue) {
        this.ai.setNPC(newvalue);
        this.isNpc = newvalue;
    }

    public void NPCSetRunning(boolean newvalue) {
        this.ai.brain.humanControlVars.running = newvalue;
    }

    public boolean NPCGetRunning() {
        return this.ai.brain.humanControlVars.running;
    }

    public void NPCSetJustMoved(boolean newvalue) {
        this.ai.brain.humanControlVars.justMoved = newvalue;
    }

    public void NPCSetAiming(boolean isAiming) {
        this.ai.brain.humanControlVars.aiming = isAiming;
    }

    public boolean NPCGetAiming() {
        return this.ai.brain.humanControlVars.aiming;
    }

    public void NPCSetAttack(boolean newvalue) {
        this.ai.brain.humanControlVars.initiateAttack = newvalue;
    }

    public void NPCSetMelee(boolean newvalue) {
        this.ai.brain.humanControlVars.melee = newvalue;
    }

    public void setMetabolicTarget(Metabolics m) {
        if (m != null) {
            this.setMetabolicTarget(m.getMet());
        }
    }

    public void setMetabolicTarget(float target) {
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            this.getBodyDamage().getThermoregulator().setMetabolicTarget(target);
        }
    }

    public double getThirstMultiplier() {
        return this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null
            ? this.getBodyDamage().getThermoregulator().getFluidsMultiplier()
            : 1.0;
    }

    public double getHungerMultiplier() {
        return 1.0;
    }

    public double getFatiqueMultiplier() {
        return this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null
            ? this.getBodyDamage().getThermoregulator().getFatigueMultiplier()
            : 1.0;
    }

    public float getTimedActionTimeModifier() {
        return 1.0F;
    }

    public boolean addHoleFromZombieAttacks(BloodBodyPartType part, boolean scratch) {
        this.getItemVisuals(tempItemVisuals);
        ItemVisual itemHit = null;

        for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null) {
                ArrayList<BloodClothingType> types = scriptItem.getBloodClothingType();
                if (types != null) {
                    ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);

                    for (int j = 0; j < coveredParts.size(); j++) {
                        BloodBodyPartType bloodClothingType = coveredParts.get(j);
                        if (part == bloodClothingType) {
                            itemHit = itemVisual;
                            break;
                        }
                    }

                    if (itemHit != null) {
                        break;
                    }
                }
            }
        }

        float baseDef = 0.0F;
        boolean result = false;
        if (itemHit != null && itemHit.getInventoryItem() != null && itemHit.getInventoryItem() instanceof Clothing clothing) {
            baseDef = Math.max(30.0F, 100.0F - clothing.getDefForPart(part, !scratch, false) / 1.5F);
        }

        if (Rand.Next(100) < baseDef) {
            boolean addedHole = this.addHole(part);
            if (addedHole) {
                this.getEmitter().playSoundImpl("ZombieRipClothing", null);
            }

            result = true;
        }

        return result;
    }

    protected void updateBandages() {
        if (!(this instanceof IsoAnimal)) {
            s_bandages.update(this);
        }
    }

    public float getTotalBlood() {
        float result = 0.0F;
        if (this.getWornItems() == null) {
            return result;
        } else {
            for (int i = 0; i < this.getWornItems().size(); i++) {
                if (this.getWornItems().get(i).getItem() instanceof Clothing clothing) {
                    result += clothing.getBloodlevel();
                }
            }

            if (this.getPrimaryHandItem() != null && !this.getWornItems().contains(this.getPrimaryHandItem())) {
                result += this.getPrimaryHandItem().getBloodLevelAdjustedHigh();
            }

            if (this.getSecondaryHandItem() != null
                && this.getPrimaryHandItem() != this.getSecondaryHandItem()
                && !this.getWornItems().contains(this.getSecondaryHandItem())) {
                result += this.getSecondaryHandItem().getBloodLevelAdjustedHigh();
            }

            return result + ((HumanVisual)this.getVisual()).getTotalBlood();
        }
    }

    public void attackFromWindowsLunge(IsoZombie zombie) {
        if (!(this.lungeFallTimer > 0.0F)
            && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(zombie.getZ())
            && !zombie.isDead()
            && this.getCurrentSquare() != null
            && !this.getCurrentSquare().isDoorBlockedTo(zombie.getCurrentSquare())
            && !this.getCurrentSquare().isWallTo(zombie.getCurrentSquare())
            && !this.getCurrentSquare().isWindowTo(zombie.getCurrentSquare())) {
            if (this.getVehicle() == null) {
                boolean hit = this.DoSwingCollisionBoneCheck(zombie, zombie.getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Hand", -1), 1.0F);
                if (hit) {
                    zombie.playSound("ZombieCrawlLungeHit");
                    this.lungeFallTimer = 200.0F;
                    this.setIsAiming(false);
                    boolean fall = false;
                    int fallChance = 30;
                    fallChance += this.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 3;
                    fallChance += this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 3;
                    fallChance += this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 5;
                    fallChance -= this.getPerkLevel(PerkFactory.Perks.Fitness) * 2;
                    fallChance -= this.getPerkLevel(PerkFactory.Perks.Nimble);
                    BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
                    if (part.getAdditionalPain(true) > 20.0F) {
                        fallChance = (int)(fallChance + (part.getAdditionalPain(true) - 20.0F) / 10.0F);
                    }

                    if (this.characterTraits.get(CharacterTrait.CLUMSY)) {
                        fallChance += 10;
                    }

                    if (this.characterTraits.get(CharacterTrait.GRACEFUL)) {
                        fallChance -= 10;
                    }

                    if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
                        fallChance += 20;
                    }

                    if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
                        fallChance += 10;
                    }

                    if (this.characterTraits.get(CharacterTrait.OBESE)) {
                        fallChance -= 10;
                    }

                    if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                        fallChance -= 5;
                    }

                    fallChance = Math.max(5, fallChance);
                    this.clearVariable("BumpFallType");
                    this.setBumpType("stagger");
                    if (Rand.Next(100) < fallChance) {
                        fall = true;
                    }

                    this.setBumpDone(false);
                    this.setBumpFall(fall);
                    if (zombie.isBehind(this)) {
                        this.setBumpFallType("pushedBehind");
                    } else {
                        this.setBumpFallType("pushedFront");
                    }

                    this.actionContext.reportEvent("wasBumped");
                }
            }
        }
    }

    public boolean DoSwingCollisionBoneCheck(IsoGameCharacter zombie, int bone, float tempoLengthTest) {
        Model.BoneToWorldCoords(zombie, bone, tempVectorBonePos);
        float distSq = IsoUtils.DistanceToSquared(tempVectorBonePos.x, tempVectorBonePos.y, this.getX(), this.getY());
        return distSq < tempoLengthTest * tempoLengthTest;
    }

    public boolean isInvincible() {
        return this.invincible;
    }

    public void setInvincible(boolean invincible) {
        if (!Role.hasCapability(this, Capability.ToggleInvincibleHimself)) {
            this.invincible = false;
        } else {
            this.invincible = invincible;
        }
    }

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
                                && (
                                    !(this instanceof IsoPlayer)
                                        || !((IsoPlayer)this).isLocalPlayer()
                                        || vehicle.getTargetAlpha(((IsoPlayer)this).playerIndex) != 0.0F
                                )
                                && !(this.DistToSquared(vehicle) >= 16.0F)) {
                                return vehicle;
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    public boolean isNearSirenVehicle() {
        if (this.getVehicle() != null) {
            return false;
        } else {
            int chunkMinX = (PZMath.fastfloor(this.getX()) - 5) / 8 - 1;
            int chunkMinY = (PZMath.fastfloor(this.getY()) - 5) / 8 - 1;
            int chunkMaxX = (int)Math.ceil((this.getX() + 5.0F) / 8.0F) + 1;
            int chunkMaxY = (int)Math.ceil((this.getY() + 5.0F) / 8.0F) + 1;

            for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle.getScript() != null
                                && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(vehicle.getZ())
                                && (
                                    !(this instanceof IsoPlayer)
                                        || !((IsoPlayer)this).isLocalPlayer()
                                        || vehicle.getTargetAlpha(((IsoPlayer)this).playerIndex) != 0.0F
                                )
                                && !(this.DistToSquared(PZMath.fastfloor(vehicle.getX()), PZMath.fastfloor(vehicle.getY())) >= 25.0F)
                                && vehicle.isSirening()) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    private IsoGridSquare getSolidFloorAt(int x, int y, int z) {
        while (z >= 0) {
            IsoGridSquare sq = this.getCell().getGridSquare(x, y, z);
            if (sq != null && sq.TreatAsSolidFloor()) {
                return sq;
            }

            z--;
        }

        return null;
    }

    public void dropHeavyItems() {
        IsoGridSquare sq = this.getCurrentSquare();
        if (sq != null) {
            InventoryItem item1 = this.getPrimaryHandItem();
            InventoryItem item2 = this.getSecondaryHandItem();
            if (item1 != null || item2 != null) {
                sq = this.getSolidFloorAt(sq.x, sq.y, sq.z);
                if (sq != null) {
                    if (this.isHeavyItem(item1)) {
                        this.setPrimaryHandItem(null);
                        if (!GameClient.client) {
                            this.getInventory().DoRemoveItem(item1);
                            float dropX = Rand.Next(0.1F, 0.9F);
                            float dropY = Rand.Next(0.1F, 0.9F);
                            float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                            sq.AddWorldInventoryItem(item1, dropX, dropY, dropZ);
                            LuaEventManager.triggerEvent("OnContainerUpdate");
                        }

                        LuaEventManager.triggerEvent("onItemFall", item1);
                        this.playDropItemSound(item1);
                    }

                    if (this.isHeavyItem(item2)) {
                        this.setSecondaryHandItem(null);
                        boolean inBothHand = item1 == item2;
                        if (!inBothHand) {
                            if (!GameClient.client) {
                                this.getInventory().DoRemoveItem(item2);
                                float dropX = Rand.Next(0.1F, 0.9F);
                                float dropY = Rand.Next(0.1F, 0.9F);
                                float dropZ = sq.getApparentZ(dropX, dropY) - sq.getZ();
                                sq.AddWorldInventoryItem(item2, dropX, dropY, dropZ);
                                LuaEventManager.triggerEvent("OnContainerUpdate");
                            }

                            LuaEventManager.triggerEvent("onItemFall", item2);
                            this.playDropItemSound(item2);
                        }
                    }

                    if (GameClient.client && this.isLocal()) {
                        INetworkPacket.send(PacketTypes.PacketType.PlayerDropHeldItems, this, sq.x, sq.y, sq.z, true);
                        INetworkPacket.send(PacketTypes.PacketType.Equip, this);
                    }
                }
            }
        }
    }

    public boolean isHeavyItem(InventoryItem item) {
        if (item == null) {
            return false;
        } else if (item instanceof InventoryContainer) {
            return true;
        } else if (item.hasTag(ItemTag.HEAVY_ITEM)) {
            return true;
        } else {
            return !item.getType().equals("CorpseMale") && !item.getType().equals("CorpseFemale") ? item.getType().equals("Generator") : true;
        }
    }

    public boolean isCanShout() {
        return this.canShout;
    }

    public void setCanShout(boolean canShout) {
        this.canShout = canShout;
    }

    public boolean isKnowAllRecipes() {
        return this.getCheats().isSet(CheatType.KNOW_ALL_RECIPES);
    }

    public void setKnowAllRecipes(boolean knowAllRecipes) {
        if (!Role.hasCapability(this, Capability.ToggleKnowAllRecipes)) {
            this.getCheats().set(CheatType.KNOW_ALL_RECIPES, false);
        } else {
            this.getCheats().set(CheatType.KNOW_ALL_RECIPES, knowAllRecipes);
        }
    }

    public boolean isUnlimitedAmmo() {
        return this.getCheats().isSet(CheatType.UNLIMITED_AMMO);
    }

    public void setUnlimitedAmmo(boolean unlimitedAmmo) {
        if (!Role.hasCapability(this, Capability.ToggleUnlimitedAmmo)) {
            this.getCheats().set(CheatType.UNLIMITED_AMMO, false);
        } else {
            this.getCheats().set(CheatType.UNLIMITED_AMMO, unlimitedAmmo);
        }
    }

    public boolean isUnlimitedEndurance() {
        return this.getCheats().isSet(CheatType.UNLIMITED_ENDURANCE);
    }

    public void setUnlimitedEndurance(boolean unlimitedEndurance) {
        if (!Role.hasCapability(this, Capability.ToggleUnlimitedEndurance)) {
            this.getCheats().set(CheatType.UNLIMITED_ENDURANCE, false);
        } else {
            this.getCheats().set(CheatType.UNLIMITED_ENDURANCE, unlimitedEndurance);
        }
    }

    private void addActiveLightItem(InventoryItem item, ArrayList<InventoryItem> items) {
        if (item != null && item.isEmittingLight() && !items.contains(item)) {
            items.add(item);
        }
    }

    public ArrayList<InventoryItem> getActiveLightItems(ArrayList<InventoryItem> items) {
        this.addActiveLightItem(this.getSecondaryHandItem(), items);
        this.addActiveLightItem(this.getPrimaryHandItem(), items);
        AttachedItems attachedItems = this.getAttachedItems();

        for (int i = 0; i < attachedItems.size(); i++) {
            InventoryItem item = attachedItems.getItemByIndex(i);
            this.addActiveLightItem(item, items);
        }

        return items;
    }

    public SleepingEventData getOrCreateSleepingEventData() {
        if (this.sleepingEventData == null) {
            this.sleepingEventData = new SleepingEventData();
        }

        return this.sleepingEventData;
    }

    public void playEmote(String emote) {
        this.setVariable("emote", emote);
        this.setVariable("EmotePlaying", true);
        this.actionContext.reportEvent("EventEmote");
        if (GameClient.client) {
            StateManager.enterState(this, PlayerEmoteState.instance());
        }
    }

    public String getAnimationStateName() {
        return this.advancedAnimator.getCurrentStateName();
    }

    public String getActionStateName() {
        return this.actionContext.getCurrentStateName();
    }

    public boolean shouldWaitToStartTimedAction() {
        if (!this.isSitOnGround()) {
            return false;
        } else if (this.getCurrentState().equals(FishingState.instance())) {
            return false;
        } else {
            AdvancedAnimator aa = this.getAdvancedAnimator();
            if (aa.getRootLayer() == null) {
                return false;
            } else if (aa.animSet != null && aa.animSet.containsState("sitonground")) {
                AnimState as = aa.animSet.GetState("sitonground");
                if (!PZArrayUtil.contains(as.nodes, node -> "sit_action".equalsIgnoreCase(node.name))) {
                    return false;
                } else {
                    LiveAnimNode lan0 = PZArrayUtil.find(
                        aa.getRootLayer().getLiveAnimNodes(), lan -> lan.isActive() && "sit_action".equalsIgnoreCase(lan.getName())
                    );
                    return lan0 == null || !lan0.isMainAnimActive();
                }
            } else {
                return false;
            }
        }
    }

    public void setPersistentOutfitID(int outfitID) {
        this.setPersistentOutfitID(outfitID, false);
    }

    public void setPersistentOutfitID(int outfitID, boolean init) {
        this.persistentOutfitId = outfitID;
        this.persistentOutfitInit = init;
    }

    public int getPersistentOutfitID() {
        return this.persistentOutfitId;
    }

    public boolean isPersistentOutfitInit() {
        return this.persistentOutfitInit;
    }

    public boolean isDoingActionThatCanBeCancelled() {
        return false;
    }

    public boolean isDoDeathSound() {
        return this.doDeathSound;
    }

    public void setDoDeathSound(boolean doDeathSound) {
        this.doDeathSound = doDeathSound;
    }

    @Override
    public boolean isKilledByFall() {
        return this.killedByFall;
    }

    @Override
    public void setKilledByFall(boolean bKilledByFall) {
        this.killedByFall = bKilledByFall;
    }

    public void updateEquippedRadioFreq() {
        this.invRadioFreq.clear();

        for (int i = 0; i < this.getInventory().getItems().size(); i++) {
            InventoryItem item = this.getInventory().getItems().get(i);
            if (item instanceof Radio radio
                && radio.getDeviceData() != null
                && radio.getDeviceData().getIsTurnedOn()
                && !radio.getDeviceData().getMicIsMuted()
                && !this.invRadioFreq.contains(radio.getDeviceData().getChannel())) {
                this.invRadioFreq.add(radio.getDeviceData().getChannel());
            }
        }

        for (int ix = 0; ix < this.invRadioFreq.size(); ix++) {
            System.out.println(this.invRadioFreq.get(ix));
        }

        if (this instanceof IsoPlayer && GameClient.client) {
            GameClient.sendEquippedRadioFreq((IsoPlayer)this);
        }
    }

    public void updateEquippedItemSounds() {
        if (this.leftHandItem != null) {
            this.leftHandItem.updateEquippedAndActivatedSound();
        }

        if (this.rightHandItem != null) {
            this.rightHandItem.updateEquippedAndActivatedSound();
        }

        WornItems wornItems1 = this.getWornItems();
        if (wornItems1 != null) {
            for (int i = 0; i < wornItems1.size(); i++) {
                InventoryItem item = wornItems1.getItemByIndex(i);
                item.updateEquippedAndActivatedSound();
            }
        }
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

    public void playBloodSplatterSound() {
        this.getEmitter().playSoundImpl("BloodSplatter", this);
    }

    public void setIgnoreAimingInput(boolean b) {
        this.ignoreAimingInput = b;
    }

    public boolean isIgnoringAimingInput() {
        return this.ignoreAimingInput;
    }

    public void setHeadLookAround(boolean b) {
        this.headLookAround = b;
    }

    public boolean isHeadLookAround() {
        return this.headLookAround;
    }

    public void setHeadLookAroundDirection(float lookHorizontal, float lookVertical) {
        this.headLookHorizontal = PZMath.clamp(lookHorizontal, -1.0F, 1.0F);
        this.headLookVertical = PZMath.clamp(lookVertical, -1.0F, 1.0F);
    }

    public float getHeadLookHorizontal() {
        return this.headLookHorizontal;
    }

    public float getHeadLookVertical() {
        return this.headLookVertical;
    }

    public float getHeadLookAngleMax() {
        return 0.348F;
    }

    public void addBlood(float speed) {
        if (!(Rand.Next(10) > speed)) {
            if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
                int spn = Rand.Next(4, 10);
                if (spn < 1) {
                    spn = 1;
                }

                if (Core.lastStand) {
                    spn *= 3;
                }

                switch (SandboxOptions.instance.bloodLevel.getValue()) {
                    case 2:
                        spn /= 2;
                    case 3:
                    default:
                        break;
                    case 4:
                        spn *= 2;
                        break;
                    case 5:
                        spn *= 5;
                }

                for (int n = 0; n < spn; n++) {
                    this.splatBlood(2, 0.3F);
                }
            }

            if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
                this.splatBloodFloorBig();
                this.playBloodSplatterSound();
            }
        }
    }

    public boolean isKnockedDown() {
        return this.knockedDown;
    }

    public void setKnockedDown(boolean in_knockedDown) {
        this.knockedDown = in_knockedDown;
    }

    public void readInventory(ByteBuffer b) {
        try {
            ArrayList<InventoryItem> savedItems = this.getInventory().load(b, IsoWorld.getWorldVersion());
            int wornItemCount = b.get();

            for (int i = 0; i < wornItemCount; i++) {
                ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadStringUTF(b)));
                int index = b.getShort();
                if (index >= 0 && index < savedItems.size() && this.getBodyLocationGroup().getLocation(itemBodyLocation) != null) {
                    this.getWornItems().setItem(itemBodyLocation, savedItems.get(index));
                    savedItems.get(index).synchWithVisual();
                }
            }

            int attachedItemsCount = b.get();

            for (int ix = 0; ix < attachedItemsCount; ix++) {
                String location = GameWindow.ReadStringUTF(b);
                int index = b.getShort();
                if (index >= 0 && index < savedItems.size() && this.getAttachedLocationGroup().getLocation(location) != null) {
                    this.getAttachedItems().setItem(location, savedItems.get(index));
                }
            }
        } catch (IOException var8) {
            DebugLog.Multiplayer.printException(var8, "ReadInventory error id=" + this.getOnlineID(), LogSeverity.Error);
        }
    }

    public void Kill(HandWeapon handWeapon, IsoGameCharacter killer) {
        this.setAttackedBy(killer);
        this.setHealth(0.0F);
        this.setOnKillDone(true);
    }

    public void Kill(IsoGameCharacter killer) {
        this.setAttackedBy(killer);
        this.setHealth(0.0F);
        if (this.getBodyDamage() != null) {
            this.getBodyDamage().setOverallBodyHealth(0.0F);
        }

        this.setOnKillDone(true);
    }

    public void die() {
        if (!this.isOnDeathDone()) {
            if (GameClient.client) {
                this.getNetworkCharacterAI().becomeCorpse();
            } else {
                this.becomeCorpse();
            }
        }
    }

    public IsoDeadBody becomeCorpse() {
        this.Kill(this.getAttackedBy());
        this.setOnDeathDone(true);
        if (this.isBeingGrappled()) {
            IGrappleable grappledBy = this.getGrappledBy();
            if (grappledBy != null) {
                grappledBy.LetGoOfGrappled("GrappledDied");
            }
        } else if (this.isGrappling()) {
            this.LetGoOfGrappled("GrapplerDied");
        }

        return null;
    }

    public InventoryItem becomeCorpseItem(ItemContainer in_placeInContainer) {
        boolean canHumanCorpseFit = in_placeInContainer.canHumanCorpseFit();
        if (!canHumanCorpseFit) {
            DebugType.General
                .warn(
                    "Character's corpse cannot fit in this container: '%s'. Remaining storage space: %f. Required: %f. Character: '%s'",
                    in_placeInContainer,
                    in_placeInContainer.getAvailableWeightCapacity(),
                    getWeightAsCorpse(),
                    this
                );
            return null;
        } else {
            IsoDeadBody deadBody = this.becomeCorpse();
            if (deadBody == null) {
                DebugType.General.warn("Failed to become an IsoDeadBody.");
                return null;
            } else {
                InventoryItem inventoryItem = deadBody.becomeCorpseItem();
                if (inventoryItem == null) {
                    DebugType.General.warn("Failed to spawn corpse item: %s", deadBody);
                    return null;
                } else if (!in_placeInContainer.canItemFit(inventoryItem)) {
                    DebugType.General
                        .error(
                            "Item cannot fit in inventory. Item: '%s', Inventory: '%s'. Remaining storage space: %f. Required: %f",
                            inventoryItem.getFullType(),
                            in_placeInContainer.getType(),
                            in_placeInContainer.getAvailableWeightCapacity(),
                            inventoryItem.getUnequippedWeight()
                        );
                    return null;
                } else {
                    in_placeInContainer.addItem(inventoryItem);
                    return inventoryItem;
                }
            }
        }
    }

    public static int getWeightAsCorpse() {
        return 20;
    }

    public HitReactionNetworkAI getHitReactionNetworkAI() {
        return null;
    }

    public NetworkCharacterAI getNetworkCharacterAI() {
        return null;
    }

    public boolean wasLocal() {
        return this.getNetworkCharacterAI() == null || this.getNetworkCharacterAI().wasLocal();
    }

    public boolean isLocal() {
        return !GameClient.client && !GameServer.server;
    }

    public boolean isVehicleCollisionActive(BaseVehicle testVehicle) {
        if (!GameClient.client) {
            return false;
        } else if (!this.isAlive()) {
            return false;
        } else if (testVehicle == null) {
            return false;
        } else if (!testVehicle.shouldCollideWithCharacters()) {
            return false;
        } else if (testVehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
            return false;
        } else if (testVehicle.isEngineRunning()
            || testVehicle.getVehicleTowing() != null && testVehicle.getVehicleTowing().isEngineRunning()
            || testVehicle.getVehicleTowedBy() != null && testVehicle.getVehicleTowedBy().isEngineRunning()) {
            if (testVehicle.getDriver() != null
                || testVehicle.getVehicleTowing() != null && testVehicle.getVehicleTowing().getDriver() != null
                || testVehicle.getVehicleTowedBy() != null && testVehicle.getVehicleTowedBy().getDriver() != null) {
                return Math.abs(testVehicle.getX() - this.getX()) < 0.01F || Math.abs(testVehicle.getY() - this.getY()) < 0.01F
                    ? false
                    : (!this.isKnockedDown() || this.isOnFloor())
                        && (this.getHitReactionNetworkAI() == null || !HitReactionNetworkAI.isEnabled(this) || !this.getHitReactionNetworkAI().isStarted());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void doHitByVehicle(BaseVehicle baseVehicle, BaseVehicle.HitVars hitVars) {
        if (GameClient.client) {
            IsoPlayer driver = GameClient.IDToPlayerMap.get(baseVehicle.getNetPlayerId());
            if (driver == null) {
                return;
            }

            if (driver.isLocal()) {
                SoundManager.instance.PlayWorldSound("VehicleHitCharacter", this.getCurrentSquare(), 0.0F, 20.0F, 0.9F, true);
                float damage = this.Hit(baseVehicle, hitVars.hitSpeed, hitVars.isTargetHitFromBehind, -hitVars.targetImpulse.x, -hitVars.targetImpulse.z);
                GameClient.sendVehicleHit(
                    driver, this, baseVehicle, damage, hitVars.isTargetHitFromBehind, hitVars.vehicleDamage, hitVars.hitSpeed, hitVars.isVehicleHitFromFront
                );
                driver.triggerMusicIntensityEvent("VehicleHitCharacter");
            } else {
                this.getNetworkCharacterAI().hitByVehicle();
            }
        } else if (!GameServer.server) {
            BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), this.getZ());
            long soundRef = emitter.playSound("VehicleHitCharacter");
            emitter.setParameterValue(
                soundRef,
                FMODManager.instance.getParameterDescription("VehicleHitLocation"),
                ParameterVehicleHitLocation.calculateLocation(baseVehicle, this.getX(), this.getY(), this.getZ()).getValue()
            );
            emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleSpeed"), baseVehicle.getCurrentSpeedKmHour());
            this.Hit(baseVehicle, hitVars.hitSpeed, hitVars.isTargetHitFromBehind, -hitVars.targetImpulse.x, -hitVars.targetImpulse.z);
            if (baseVehicle.getDriverRegardlessOfTow() instanceof IsoPlayer driverx) {
                driverx.triggerMusicIntensityEvent("VehicleHitCharacter");
            }
        }
    }

    public boolean isSkipResolveCollision() {
        return this.isBeingGrappled();
    }

    public boolean isPerformingAttackAnimation() {
        return this.isPerformingAttackAnim;
    }

    public void setPerformingAttackAnimation(boolean attackAnim) {
        this.isPerformingAttackAnim = attackAnim;
    }

    public boolean isPerformingShoveAnimation() {
        return this.isPerformingShoveAnim;
    }

    public void setPerformingShoveAnimation(boolean shoveAnim) {
        this.isPerformingShoveAnim = shoveAnim;
    }

    public boolean isPerformingStompAnimation() {
        return this.isPerformingStompAnim;
    }

    public void setPerformingStompAnimation(boolean stompAnim) {
        this.isPerformingStompAnim = stompAnim;
    }

    public boolean isPerformingHostileAnimation() {
        return this.isPerformingAttackAnimation()
            || this.isPerformingShoveAnimation()
            || this.isPerformingStompAnimation()
            || this.isPerformingGrappleGrabAnimation();
    }

    public Float getNextAnimationTranslationLength() {
        ActionContext actionContext = this.getActionContext();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        AdvancedAnimator advancedAnimator = this.getAdvancedAnimator();
        if (actionContext != null && animationPlayer != null && advancedAnimator != null) {
            ActionState actionState = actionContext.peekNextState();
            if (actionState != null && !StringUtils.isNullOrEmpty(actionState.getName())) {
                AnimationSet animSet = advancedAnimator.animSet;
                AnimState animState = animSet.GetState(actionState.getName());
                SkinningData skinningData = animationPlayer.getSkinningData();
                List<AnimNode> nodes = new ArrayList<>();
                animState.getAnimNodes(this, nodes);

                for (AnimNode node : nodes) {
                    if (!StringUtils.isNullOrEmpty(node.animName)) {
                        AnimationClip clip = skinningData.animationClips.get(node.animName);
                        if (clip != null) {
                            float length = clip.getTranslationLength(node.deferredBoneAxis);
                            float time = AdvancedAnimator.motionScale * clip.getDuration();
                            return length * time;
                        }
                    }
                }

                return null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Float calcHitDir(IsoGameCharacter wielder, HandWeapon weapon, Vector2 out) {
        Float length = this.getNextAnimationTranslationLength();
        out.set(this.getX() - wielder.getX(), this.getY() - wielder.getY()).normalize();
        if (length == null) {
            out.setLength(this.getHitForce() * 0.1F);
            out.scale(weapon.getPushBackMod());
            out.rotate(weapon.hitAngleMod);
        } else {
            out.scale(length);
        }

        return null;
    }

    public void calcHitDir(Vector2 out) {
        out.set(this.getHitDir());
        out.setLength(this.getHitForce());
    }

    @Override
    public Safety getSafety() {
        return this.safety;
    }

    @Override
    public void setSafety(Safety safety) {
        this.safety.copyFrom(safety);
    }

    public void burnCorpse(IsoDeadBody corpse) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.BurnCorpse, this, corpse.getObjectID());
        } else {
            IsoFireManager.StartFire(corpse.getCell(), corpse.getSquare(), true, 100, 700);
        }
    }

    public void setIsAnimal(boolean v) {
        this.animal = v;
    }

    public boolean isAnimal() {
        return this.animal;
    }

    public boolean isAnimalRunningToDeathPosition() {
        return false;
    }

    @Override
    public float getPerkToUnit(PerkFactory.Perk perk) {
        int perkLevel = this.getPerkLevel(perk);
        if (perkLevel == 10) {
            return 1.0F;
        } else {
            float xpNextLevel = perk.getXpForLevel(perkLevel + 1);
            float xp = this.getXp().getXP(perk);
            float subtractXp = 0.0F;
            if (perkLevel > 0) {
                subtractXp = perk.getTotalXpForLevel(perkLevel);
            }

            xp -= subtractXp;
            float perkProgress = perkLevel * 0.1F + xp / xpNextLevel * 0.1F;
            return PZMath.clamp(perkProgress, 0.0F, 1.0F);
        }
    }

    @Override
    public HashMap<String, Integer> getReadLiterature() {
        return this.readLiterature;
    }

    @Override
    public boolean isLiteratureRead(String name) {
        HashMap<String, Integer> list = this.getReadLiterature();
        if (list.containsKey(name)) {
            int dayRead = list.get(name);
            int currentDay = GameTime.getInstance().getNightsSurvived();
            return currentDay < dayRead + SandboxOptions.getInstance().literatureCooldown.getValue();
        } else {
            return false;
        }
    }

    @Override
    public void addReadLiterature(String name) {
        if (!this.isLiteratureRead(name)) {
            HashMap<String, Integer> list = this.getReadLiterature();
            int day = GameTime.getInstance().getNightsSurvived();
            list.put(name, day);
        }
    }

    @Override
    public void addReadLiterature(String name, int day) {
        if (!this.isLiteratureRead(name)) {
            HashMap<String, Integer> list = this.getReadLiterature();
            list.put(name, day);
        }
    }

    @Override
    public void addReadPrintMedia(String media_id) {
        if (!StringUtils.isNullOrWhitespace(media_id)) {
            if (!this.readPrintMedia.contains(media_id)) {
                this.readPrintMedia.add(media_id);
            }
        }
    }

    @Override
    public boolean isPrintMediaRead(String media_id) {
        return this.readPrintMedia.contains(media_id);
    }

    @Override
    public HashSet<String> getReadPrintMedia() {
        return this.readPrintMedia;
    }

    @Override
    public boolean hasReadMap(InventoryItem item) {
        return item instanceof MapItem map ? this.isPrintMediaRead(map.getMediaId()) : false;
    }

    @Override
    public void addReadMap(InventoryItem item) {
        if (item instanceof MapItem map) {
            this.addReadPrintMedia(map.getMediaId());
        }
    }

    public void setMusicIntensityEventModData(String key, Object value) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            KahluaTable table = this.hasModData() ? Type.tryCastTo(this.getModData().rawget("MusicIntensityEvent"), KahluaTable.class) : null;
            if (table != null || value != null) {
                if (table == null) {
                    table = LuaManager.platform.newTable();
                    this.getModData().rawset("MusicIntensityEvent", table);
                }

                table.rawset(key, value);
            }
        }
    }

    public Object getMusicIntensityEventModData(String key) {
        if (this.hasModData()) {
            return this.getModData().rawget("MusicIntensityEvent") instanceof KahluaTable table ? table.rawget(key) : null;
        } else {
            return null;
        }
    }

    public boolean isWearingTag(ItemTag itemTag) {
        for (int i = 0; i < this.getWornItems().size(); i++) {
            InventoryItem item = this.getWornItems().get(i).getItem();
            if (item.hasTag(itemTag)) {
                return true;
            }
        }

        return false;
    }

    public float getCorpseSicknessDefense() {
        return this.getCorpseSicknessDefense(0.0F, false);
    }

    public float getCorpseSicknessDefense(float rate) {
        return this.getCorpseSicknessDefense(rate, true);
    }

    public float getCorpseSicknessDefense(float rate, boolean drain) {
        float defense = 0.0F;

        for (int i = 0; i < this.getWornItems().size(); i++) {
            if (this.getWornItems().getItemByIndex(i) instanceof Clothing clothing) {
                if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && clothing.hasTank() && clothing.getUsedDelta() > 0.0F) {
                    return 100.0F;
                }

                if (clothing.getCorpseSicknessDefense() > defense) {
                    defense = clothing.getCorpseSicknessDefense();
                }

                if ((clothing.hasTag(ItemTag.GAS_MASK) || clothing.hasTag(ItemTag.RESPIRATOR)) && clothing.hasFilter()) {
                    defense = 25.0F;
                }

                if ((clothing.hasTag(ItemTag.GAS_MASK) || clothing.hasTag(ItemTag.RESPIRATOR)) && clothing.getUsedDelta() > 0.0F) {
                    defense = 100.0F;
                    if (drain && rate > 0.0F) {
                        clothing.drainGasMask(rate);
                    }
                }
            }

            if (defense >= 100.0F) {
                return 100.0F;
            }
        }

        return defense;
    }

    public boolean isProtectedFromToxic() {
        return this.isProtectedFromToxic(false);
    }

    public boolean isProtectedFromToxic(boolean drain) {
        for (int i = 0; i < this.getWornItems().size(); i++) {
            if (this.getWornItems().getItemByIndex(i) instanceof Clothing clothing) {
                if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && clothing.hasTank() && clothing.getUsedDelta() > 0.0F) {
                    return true;
                }

                if ((clothing.hasTag(ItemTag.GAS_MASK) || clothing.hasTag(ItemTag.RESPIRATOR)) && clothing.hasFilter() && clothing.getUsedDelta() > 0.0F) {
                    if (drain) {
                        clothing.drainGasMask(0.01F);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private void checkSCBADrain() {
        for (int i = 0; i < this.getWornItems().size(); i++) {
            if (this.getWornItems().getItemByIndex(i) instanceof Clothing clothing) {
                if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && clothing.hasTank() && clothing.getUsedDelta() > 0.0F) {
                    clothing.drainSCBA();
                    return;
                }

                if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && (!clothing.hasTank() || clothing.getUsedDelta() <= 0.0F)) {
                    clothing.setActivated(false);
                    return;
                }
            }
        }
    }

    public boolean isOverEncumbered() {
        float weight = this.getInventory().getCapacityWeight();
        float maxWeight = this.getMaxWeight();
        return weight / maxWeight > 1.0F;
    }

    public void updateWornItemsVisionModifier() {
        float mod = 1.0F;
        if (this instanceof IsoZombie) {
            this.getItemVisuals(tempItemVisuals);
            if (tempItemVisuals != null) {
                for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
                    ItemVisual itemVisual = tempItemVisuals.get(i);
                    if (itemVisual != null) {
                        Item scriptItem = itemVisual.getScriptItem();
                        if (scriptItem != null && scriptItem.getVisionModifier() != 1.0F && scriptItem.getVisionModifier() > 0.0F) {
                            mod /= scriptItem.getVisionModifier();
                        }
                    }
                }
            }
        } else if (this.getWornItems() != null) {
            for (int ix = 0; ix < this.getWornItems().size(); ix++) {
                InventoryItem item = this.getWornItems().getItemByIndex(ix);
                if (item != null && item.getVisionModifier() != 1.0F && item.getVisionModifier() > 0.0F) {
                    mod /= item.getVisionModifier();
                }
            }
        }

        this.wornItemsVisionModifier = mod;
    }

    public float getWornItemsVisionModifier() {
        return this.wornItemsVisionModifier;
    }

    public float getWornItemsVisionMultiplier() {
        return 1.0F / this.getWornItemsVisionModifier();
    }

    public void updateWornItemsHearingModifier() {
        float mod = 1.0F;
        if (this instanceof IsoZombie) {
            this.getItemVisuals(tempItemVisuals);

            for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
                ItemVisual itemVisual = tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem != null && scriptItem.getHearingModifier() != 1.0F && scriptItem.getHearingModifier() > 0.0F) {
                    mod /= scriptItem.getHearingModifier();
                }
            }
        } else {
            for (int ix = 0; ix < this.getWornItems().size(); ix++) {
                InventoryItem item = this.getWornItems().getItemByIndex(ix);
                if (item != null && item.getHearingModifier() != 1.0F && item.getHearingModifier() > 0.0F) {
                    mod /= item.getHearingModifier();
                }
            }
        }

        this.wornItemsHearingModifier = mod;
    }

    public float getWornItemsHearingModifier() {
        return this.wornItemsHearingModifier;
    }

    public float getWornItemsHearingMultiplier() {
        return 1.0F / this.getWornItemsHearingModifier();
    }

    public float getHearDistanceModifier() {
        float dist = 1.0F;
        if (this.characterTraits.get(CharacterTrait.HARD_OF_HEARING)) {
            dist *= 4.5F;
        }

        return dist * this.getWornItemsHearingModifier();
    }

    public float getWeatherHearingMultiplier() {
        float resultMod = 1.0F;
        resultMod -= ClimateManager.getInstance().getRainIntensity() * 0.33F;
        return resultMod - ClimateManager.getInstance().getFogIntensity() * 0.1F;
    }

    public float getSeeNearbyCharacterDistance() {
        return (3.5F - this.stats.get(CharacterStat.FATIGUE) - this.stats.get(CharacterStat.INTOXICATION) * 0.01F) * this.getWornItemsVisionMultiplier();
    }

    public void setLastHitCharacter(IsoGameCharacter character) {
        this.lastHitCharacter = character;
    }

    public IsoGameCharacter getLastHitCharacter() {
        return this.lastHitCharacter;
    }

    public void triggerCough() {
        this.setVariable("Ext", "Cough");
        this.Say(Translator.getText("IGUI_PlayerText_Cough"));
        this.reportEvent("EventDoExt");
        WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), 35, 40, true);
    }

    public boolean hasDirtyClothing(Integer part) {
        this.getItemVisuals(tempItemVisuals);

        for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null) {
                ArrayList<BloodClothingType> types = scriptItem.getBloodClothingType();
                if (types != null) {
                    ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);
                    if (coveredParts != null) {
                        InventoryItem item = itemVisual.getInventoryItem();
                        if (item == null) {
                            item = InventoryItemFactory.CreateItem(itemVisual.getItemType());
                            if (item == null) {
                                continue;
                            }
                        }

                        for (int j = 0; j < coveredParts.size(); j++) {
                            if (item instanceof Clothing && coveredParts.get(j).index() == part && itemVisual.getDirt(coveredParts.get(j)) > 0.15F) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean hasBloodyClothing(Integer part) {
        this.getItemVisuals(tempItemVisuals);

        for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null) {
                ArrayList<BloodClothingType> types = scriptItem.getBloodClothingType();
                if (types != null) {
                    ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);
                    if (coveredParts != null) {
                        InventoryItem item = itemVisual.getInventoryItem();
                        if (item == null) {
                            item = InventoryItemFactory.CreateItem(itemVisual.getItemType());
                            if (item == null) {
                                continue;
                            }
                        }

                        for (int j = 0; j < coveredParts.size(); j++) {
                            if (item instanceof Clothing && coveredParts.get(j).index() == part && itemVisual.getBlood(coveredParts.get(j)) > 0.25F) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public IAnimatable getAnimatable() {
        return this;
    }

    @Override
    public IGrappleable getGrappleable() {
        return this;
    }

    public BaseGrappleable getWrappedGrappleable() {
        return this.grappleable;
    }

    @Override
    public boolean canBeGrappled() {
        return this.isBeingGrappled() ? false : this.canTransitionToState("grappled");
    }

    @Override
    public boolean isPerformingGrappleAnimation() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            List<AnimationTrack> tracks = animationPlayer.getMultiTrack().getTracks();

            for (int i = 0; i < tracks.size(); i++) {
                AnimationTrack track = tracks.get(i);
                if (track.isGrappler()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public String getShoutType() {
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
            return this.getPrimaryHandItem().getShoutType() + "_primary";
        } else if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null) {
            return this.getSecondaryHandItem().getShoutType() + "_secondary";
        } else {
            if (this.getWornItems() != null) {
                for (int i = 0; i < this.getWornItems().size(); i++) {
                    if (this.getWornItems().get(i).getItem() != null && this.getWornItems().get(i).getItem().getShoutType() != null) {
                        return this.getWornItems().get(i).getItem().getShoutType() + "_secondary";
                    }
                }
            }

            return null;
        }
    }

    public String getShoutItemModel() {
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
            return this.getPrimaryHandItem().getStaticModel();
        } else {
            return this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null
                ? this.getSecondaryHandItem().getStaticModel()
                : null;
        }
    }

    public boolean isWearingGlasses() {
        InventoryItem wornItem = this.getWornItem(ItemBodyLocation.EYES);
        return this.isWearingVisualAid() || wornItem != null && wornItem.isVisualAid();
    }

    public boolean isWearingVisualAid() {
        if (this.getWornItems() != null && !this.getWornItems().isEmpty()) {
            for (int i = 0; i < this.getWornItems().size(); i++) {
                WornItem wornItem = this.getWornItems().get(i);
                InventoryItem characterItem = wornItem.getItem();
                if (characterItem.isVisualAid()) {
                    return true;
                }
            }
        }

        InventoryItem wornItem = this.getWornItem(ItemBodyLocation.EYES);
        return wornItem != null && wornItem.isVisualAid();
    }

    public float getClothingDiscomfortModifier() {
        return this.clothingDiscomfortModifier;
    }

    public void updateVisionEffectTargets() {
        this.blurFactor = PZMath.lerp(this.blurFactor, this.blurFactorTarget, this.blurFactor < this.blurFactorTarget ? 0.1F : 0.01F);
    }

    public void updateVisionEffects() {
        boolean isShortSighted = this.characterTraits.get(CharacterTrait.SHORT_SIGHTED);
        boolean isWearingGlasses = this.isWearingGlasses();
        if ((isWearingGlasses || !isShortSighted) && (!isWearingGlasses || isShortSighted)) {
            this.blurFactorTarget = 0.0F;
        } else {
            this.blurFactorTarget = 1.0F;
        }
    }

    public float getBlurFactor() {
        return this.blurFactor;
    }

    public boolean isDisguised() {
        return this.usernameDisguised;
    }

    public void updateDisguisedState() {
        if ((GameClient.client || GameServer.server) && ServerOptions.instance.usernameDisguises.getValue()) {
            this.usernameDisguised = false;
            if (this.isoPlayer == null) {
                return;
            }

            SafeHouse safe = SafeHouse.isSafeHouse(this.getCurrentSquare(), null, false);
            if (safe == null
                || !ServerOptions.instance.safehouseDisableDisguises.getValue()
                || this.isoPlayer.role.hasCapability(Capability.CanGoInsideSafehouses)) {
                HashSet<ItemTag> testItemTags = new HashSet<>();
                this.getItemVisuals(tempItemVisuals);
                if (tempItemVisuals != null) {
                    for (int i = tempItemVisuals.size() - 1; i >= 0; i--) {
                        ItemVisual itemVisual = tempItemVisuals.get(i);
                        if (itemVisual != null) {
                            Item scriptItem = itemVisual.getScriptItem();
                            if (scriptItem != null) {
                                testItemTags.addAll(scriptItem.getTags());
                            }
                        }
                    }
                } else if (this.getWornItems() != null) {
                    for (int ix = 0; ix < this.getWornItems().size(); ix++) {
                        InventoryItem item = this.getWornItems().getItemByIndex(ix);
                        if (item != null) {
                            Item scriptItem = item.getScriptItem();
                            if (scriptItem != null) {
                                testItemTags.addAll(scriptItem.getTags());
                            }
                        }
                    }
                }

                if (!testItemTags.isEmpty()
                    && (
                        testItemTags.contains(ItemTag.IS_DISGUISE)
                            || testItemTags.contains(ItemTag.IS_LOWER_DISGUISE) && testItemTags.contains(ItemTag.IS_UPPER_DISGUISE)
                    )) {
                    this.usernameDisguised = true;
                }
            }
        }
    }

    public void OnClothingUpdated() {
        this.updateSpeedModifiers();
        if (this instanceof IsoPlayer) {
            this.updateVisionEffects();
            this.updateDiscomfortModifiers();
        }

        this.updateWornItemsVisionModifier();
        this.updateWornItemsHearingModifier();
    }

    public void OnEquipmentUpdated() {
    }

    private void renderDebugData() {
        IndieGL.StartShader(0);
        IndieGL.disableDepthTest();
        IndieGL.glBlendFunc(770, 771);
        IsoZombie isoZombie = Type.tryCastTo(this, IsoZombie.class);
        IsoPlayer isoPlayer = Type.tryCastTo(this, IsoPlayer.class);
        IsoAnimal isoAnimal = Type.tryCastTo(this, IsoAnimal.class);
        TextManager.StringDrawer stringDrawer = TextManager.instance::DrawString;
        Color ownerColor = Colors.Chartreuse;
        if (this.isDead()) {
            ownerColor = Colors.Yellow;
        } else if (!this.isLocal()) {
            ownerColor = Colors.OrangeRed;
        }

        UIFont font = UIFont.Dialogue;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        Color c = Colors.White;
        float dy = 0.0F;
        if (isoPlayer != null && isoPlayer.getRole() != null && isoAnimal == null) {
            stringDrawer.draw(
                font,
                sx,
                sy + (dy = dy + 14.0F),
                String.format(
                    "%d %.03f", this.getOnlineID(), isoZombie == null && isoAnimal == null ? this.getBodyDamage().getOverallBodyHealth() : this.getHealth()
                ),
                ownerColor.r,
                ownerColor.g,
                ownerColor.b,
                ownerColor.a
            );
        } else {
            stringDrawer.draw(font, sx, sy + (dy = dy + 14.0F), String.format("%d", this.getOnlineID()), ownerColor.r, ownerColor.g, ownerColor.b, ownerColor.a);
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().position.getValue()) {
            c = Colors.RosyBrown;
            float var23 = dy + 4.0F;
            float var24;
            stringDrawer.draw(font, sx, sy + (var24 = var23 + 14.0F), String.format("x=%09.3f", this.getX()), c.r, c.g, c.b, c.a);
            float var25;
            stringDrawer.draw(font, sx, sy + (var25 = var24 + 14.0F), String.format("y=%09.3f", this.getY()), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy = var25 + 14.0F), String.format("z=%09.3f", this.getZ()), c.r, c.g, c.b, c.a);
            if (this.getHitReactionNetworkAI().isSetup()) {
                LineDrawer.DrawIsoLine(
                    this.getHitReactionNetworkAI().startPosition.x,
                    this.getHitReactionNetworkAI().startPosition.y,
                    this.getZ(),
                    this.getHitReactionNetworkAI().finalPosition.x,
                    this.getHitReactionNetworkAI().finalPosition.y,
                    this.getZ(),
                    Colors.Salmon.r,
                    Colors.Salmon.g,
                    Colors.Salmon.b,
                    Colors.Salmon.a,
                    1
                );
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.2F, 16, c.r, c.g, c.b, c.a);
                LineDrawer.DrawIsoCircle(
                    this.getHitReactionNetworkAI().finalPosition.x,
                    this.getHitReactionNetworkAI().finalPosition.y,
                    this.getZ(),
                    0.2F,
                    16,
                    Colors.Salmon.r,
                    Colors.Salmon.g,
                    Colors.Salmon.b,
                    Colors.Salmon.a
                );
            }

            if (this.getPrimaryHandItem() instanceof HandWeapon handWeapon) {
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), handWeapon.getMaxRange(this), 16, c.r, c.g, c.b, c.a);
            }
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().prediction.getValue()) {
            c = Colors.Magenta;
            dy += 4.0F;
            if (isoZombie != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0F), "Prediction: " + this.getNetworkCharacterAI().predictionType, c.r, c.g, c.b, c.a);
                LineDrawer.DrawIsoCircle(this.realx, this.realy, this.getZ(), 0.35F, 16, Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a);
                if (isoZombie.networkAi.debugInterfaceActive) {
                    LineDrawer.DrawIsoCircle(
                        this.getX(), this.getY(), this.getZ(), 0.4F, 4, Colors.NavajoWhite.r, Colors.NavajoWhite.g, Colors.NavajoWhite.b, Colors.NavajoWhite.a
                    );
                } else if (this.isLocal()) {
                    LineDrawer.DrawIsoCircle(
                        this.getX(), this.getY(), this.getZ(), 0.3F, 3, Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a
                    );
                } else {
                    LineDrawer.DrawIsoCircle(
                        this.getX(), this.getY(), this.getZ(), 0.3F, 5, Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a
                    );
                }

                if (GameClient.client) {
                    LineDrawer.DrawIsoTransform(
                        this.getNetworkCharacterAI().targetX,
                        this.getNetworkCharacterAI().targetY,
                        this.getZ(),
                        1.0F,
                        0.0F,
                        0.4F,
                        16,
                        Colors.LimeGreen.r,
                        Colors.LimeGreen.g,
                        Colors.LimeGreen.b,
                        Colors.LimeGreen.a,
                        1
                    );
                    LineDrawer.DrawIsoLine(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        this.getNetworkCharacterAI().targetX,
                        this.getNetworkCharacterAI().targetY,
                        this.getZ(),
                        Colors.LimeGreen.r,
                        Colors.LimeGreen.g,
                        Colors.LimeGreen.b,
                        Colors.LimeGreen.a,
                        1
                    );
                    if (IsoUtils.DistanceToSquared(this.getX(), this.getY(), this.realx, this.realy) > 4.5F) {
                        LineDrawer.DrawIsoLine(
                            this.realx,
                            this.realy,
                            this.getZ(),
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            Colors.Magenta.r,
                            Colors.Magenta.g,
                            Colors.Magenta.b,
                            Colors.Magenta.a,
                            1
                        );
                    } else {
                        LineDrawer.DrawIsoLine(
                            this.realx,
                            this.realy,
                            this.getZ(),
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            Colors.Blue.r,
                            Colors.Blue.g,
                            Colors.Blue.b,
                            Colors.Blue.a,
                            1
                        );
                    }
                }
            } else if (isoPlayer != null && !this.isLocal()) {
                LineDrawer.DrawIsoLine(
                    this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1
                );
                LineDrawer.DrawIsoLine(
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    tempo.x,
                    tempo.y,
                    this.getZ(),
                    Colors.LimeGreen.r,
                    Colors.LimeGreen.g,
                    Colors.LimeGreen.b,
                    Colors.LimeGreen.a,
                    1
                );
                LineDrawer.DrawIsoCircle(
                    this.getX(), this.getY(), this.getZ(), 0.3F, 16, Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a
                );
                LineDrawer.DrawIsoCircle(this.realx, this.realy, this.getZ(), 0.35F, 16, Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a);
                tempo.set(this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY);
                LineDrawer.DrawIsoCircle(
                    tempo.x, tempo.y, this.getZ(), 0.25F, 16, Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a
                );
                float tx = IsoUtils.XToScreenExact(this.getX() - 0.1F, this.getY() - 0.6F, this.getZ(), 0);
                float ty = IsoUtils.YToScreenExact(this.getX() - 0.1F, this.getY() - 0.6F, this.getZ(), 0);
                stringDrawer.draw(font, tx, ty, "local", Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a);
                tx = IsoUtils.XToScreenExact(this.realx - 1.1F, this.realy + 0.3F, this.getZ(), 0);
                ty = IsoUtils.YToScreenExact(this.realx - 1.1F, this.realy + 0.3F, this.getZ(), 0);
                stringDrawer.draw(font, tx, ty, "remote", Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a);
                tx = IsoUtils.XToScreenExact(tempo.x - 0.3F, tempo.y - 0.6F, this.getZ(), 0);
                ty = IsoUtils.YToScreenExact(tempo.x - 0.3F, tempo.y - 0.6F, this.getZ(), 0);
                stringDrawer.draw(font, tx, ty, "target", Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a);
            }
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().variables.getValue()) {
            c = Colors.MediumPurple;
            float var26 = dy + 4.0F;
            float var27;
            stringDrawer.draw(font, sx, sy + (var27 = var26 + 14.0F), String.format("FallTime: %.03f", this.getFallTime()), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy = var27 + 14.0F), String.format("Reanimate: %.03f", this.getReanimateTimer()), c.r, c.g, c.b, c.a);
            if (isoPlayer != null && isoAnimal == null) {
                float var28;
                stringDrawer.draw(
                    font, sx, sy + (var28 = dy + 14.0F), String.format("SneakLimpSpeedScale: %s", isoPlayer.getSneakLimpSpeedScale()), c.r, c.g, c.b, c.a
                );
                float var29;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var29 = var28 + 14.0F),
                    String.format("IdleSpeed: %s , targetDist: %s ", isoPlayer.getVariableString("IdleSpeed"), isoPlayer.getVariableString("targetDist")),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var30;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var30 = var29 + 14.0F),
                    String.format("WalkInjury: %s , WalkSpeed: %s", isoPlayer.getVariableString("WalkInjury"), isoPlayer.getVariableString("WalkSpeed")),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var31;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var31 = var30 + 14.0F),
                    String.format("DeltaX: %s , DeltaY: %s", isoPlayer.getVariableString("DeltaX"), isoPlayer.getVariableString("DeltaY")),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var32;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var32 = var31 + 14.0F),
                    String.format(
                        "AttackVariationX: %s , AttackVariationY: %s",
                        isoPlayer.getVariableString("AttackVariationX"),
                        isoPlayer.getVariableString("AttackVariationY")
                    ),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var33;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var33 = var32 + 14.0F),
                    String.format(
                        "autoShootVarX: %s , autoShootVarY: %s", isoPlayer.getVariableString("autoShootVarX"), isoPlayer.getVariableString("autoShootVarY")
                    ),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var34;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var34 = var33 + 14.0F),
                    String.format("recoilVarX: %s , recoilVarY: %s", isoPlayer.getVariableString("recoilVarX"), isoPlayer.getVariableString("recoilVarY")),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var35;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var35 = var34 + 14.0F),
                    String.format("ShoveAimX: %s , ShoveAimY: %s", isoPlayer.getVariableString("ShoveAimX"), isoPlayer.getVariableString("ShoveAimY")),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                float var36;
                stringDrawer.draw(
                    font, sx, sy + (var36 = var35 + 14.0F), String.format("ForwardDirection: %f", isoPlayer.getDirectionAngleRadians()), c.r, c.g, c.b, c.a
                );
                stringDrawer.draw(
                    font, sx, sy + (dy = var36 + 14.0F), String.format("FishingStage: %s", isoPlayer.getVariableString("FishingStage")), c.r, c.g, c.b, c.a
                );
            }

            if (isoAnimal != null) {
                float var37;
                stringDrawer.draw(font, sx, sy + (var37 = dy + 14.0F), String.format("Stress:%.02f", isoAnimal.getStress()), c.r, c.g, c.b, c.a);
                float var38;
                stringDrawer.draw(
                    font, sx, sy + (var38 = var37 + 14.0F), String.format("Milk: %.02f", isoAnimal.getData().getMilkQuantity()), c.r, c.g, c.b, c.a
                );
                float var39;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var39 = var38 + 14.0F),
                    String.format("Acceptance:%.02f", isoAnimal.getAcceptanceLevel(IsoPlayer.getInstance())),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                stringDrawer.draw(
                    font, sx, sy + (dy = var39 + 14.0F), String.format("AlertX:%.02f", isoAnimal.getVariableFloat("AlertX", 0.0F)), c.r, c.g, c.b, c.a
                );
            }

            if (isoZombie != null) {
                if (isoZombie.target instanceof IsoPlayer player) {
                    stringDrawer.draw(font, sx, sy + (dy += 14.0F), "Target: " + player.username, c.r, c.g, c.b, c.a);
                } else if (isoZombie.target != null) {
                    stringDrawer.draw(font, sx, sy + (dy += 14.0F), "Target: " + isoZombie.target, c.r, c.g, c.b, c.a);
                } else {
                    float var40;
                    stringDrawer.draw(font, sx, sy + (var40 = dy + 14.0F), "Target x=" + this.getPathTargetX(), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy = var40 + 14.0F), "Target y=" + this.getPathTargetY(), c.r, c.g, c.b, c.a);
                }
            }
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().state.getValue()) {
            c = Colors.LightBlue;
            dy += 4.0F;
            String subStateName = "";
            if (this.getStateMachine().getSubStateCount() > 0 && this.getStateMachine().getSubStateAt(0) != null) {
                subStateName = this.getStateMachine().getSubStateAt(0).getName();
            }

            float var42;
            stringDrawer.draw(
                font, sx, sy + (var42 = dy + 14.0F), String.format("Class state: %s ( %s )", this.getCurrentStateName(), subStateName), c.r, c.g, c.b, c.a
            );
            String childStateName = "";
            if (!this.getActionContext().getChildStates().isEmpty() && this.getActionContext().getChildStateAt(0) != null) {
                childStateName = this.getActionContext().getChildStateAt(0).getName();
            }

            stringDrawer.draw(
                font,
                sx,
                sy + (dy = var42 + 14.0F),
                String.format("Actions state: %s ( %s )", this.getCurrentActionContextStateName(), childStateName),
                c.r,
                c.g,
                c.b,
                c.a
            );
            if (this.characterActions != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0F), String.format("Actions: %d", this.characterActions.size()), c.r, c.g, c.b, c.a);

                for (BaseAction baseAction : this.characterActions) {
                    if (baseAction instanceof LuaTimedActionNew luaTimedActionNew) {
                        stringDrawer.draw(font, sx, sy + (dy += 14.0F), String.format("Action: %s", luaTimedActionNew.getMetaType()), c.r, c.g, c.b, c.a);
                    }
                }
            }

            float var44;
            stringDrawer.draw(
                font,
                sx,
                sy + (var44 = dy + 14.0F),
                String.format(
                    "Network state enter: %s ( %s )",
                    this.getNetworkCharacterAI().getState().getEnterStateName(),
                    this.getNetworkCharacterAI().getState().getEnterSubStateName()
                ),
                c.r,
                c.g,
                c.b,
                c.a
            );
            stringDrawer.draw(
                font,
                sx,
                sy + (dy = var44 + 14.0F),
                String.format(
                    "Network state exit: %s ( %s )",
                    this.getNetworkCharacterAI().getState().getExitStateName(),
                    this.getNetworkCharacterAI().getState().getExitSubStateName()
                ),
                c.r,
                c.g,
                c.b,
                c.a
            );
            if (this.getNetworkCharacterAI().getState().getEnterState() != null) {
                LineDrawer.DrawIsoCircle(
                    this.getNetworkCharacterAI().getState().getEnterState().getX(),
                    this.getNetworkCharacterAI().getState().getEnterState().getY(),
                    this.getNetworkCharacterAI().getState().getEnterState().getZ(),
                    0.2F,
                    16,
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
            }

            stringDrawer.draw(font, sx, sy + (dy = dy + 14.0F), String.format("Real state: %s", this.realState), c.r, c.g, c.b, c.a);
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().stateVariables.getValue()) {
            c = Colors.LightGreen;
            dy += 4.0F;
            float var47;
            stringDrawer.draw(
                font,
                sx,
                sy + (var47 = dy + 14.0F),
                String.format("isHitFromBehind: %b / %b", this.isHitFromBehind(), this.getVariableBoolean("frombehind")),
                c.r,
                c.g,
                c.b,
                c.a
            );
            stringDrawer.draw(
                font,
                sx,
                sy + (dy = var47 + 14.0F),
                String.format("bKnockedDown: %b / %b", this.isKnockedDown(), this.getVariableBoolean("bknockeddown")),
                c.r,
                c.g,
                c.b,
                c.a
            );
            float var49;
            stringDrawer.draw(
                font,
                sx,
                sy + (var49 = dy + 14.0F),
                String.format("isFallOnFront: %b / %b", this.isFallOnFront(), this.getVariableBoolean("fallonfront")),
                c.r,
                c.g,
                c.b,
                c.a
            );
            stringDrawer.draw(
                font,
                sx,
                sy + (dy = var49 + 14.0F),
                String.format("isOnFloor: %b / %b", this.isOnFloor(), this.getVariableBoolean("bonfloor")),
                c.r,
                c.g,
                c.b,
                c.a
            );
            float var51;
            stringDrawer.draw(
                font,
                sx,
                sy + (var51 = dy + 14.0F),
                String.format("isSitOnGround: %b / %b", this.isSitOnGround(), this.getVariableBoolean("sitonground")),
                c.r,
                c.g,
                c.b,
                c.a
            );
            stringDrawer.draw(
                font, sx, sy + (dy = var51 + 14.0F), String.format("isDead: %b / %b", this.isDead(), this.getVariableBoolean("bdead")), c.r, c.g, c.b, c.a
            );
            float var53;
            stringDrawer.draw(
                font, sx, sy + (var53 = dy + 14.0F), String.format("isAiming: %b / %b", this.isAiming(), this.getVariableBoolean("aim")), c.r, c.g, c.b, c.a
            );
            if (isoZombie != null) {
                float var54;
                stringDrawer.draw(font, sx, sy + (var54 = var53 + 14.0F), String.format("bThump: %b", this.getVariableString("bThump")), c.r, c.g, c.b, c.a);
                float var55;
                stringDrawer.draw(
                    font, sx, sy + (var55 = var54 + 14.0F), String.format("ThumpType: %s", this.getVariableString("ThumpType")), c.r, c.g, c.b, c.a
                );
                float var56;
                stringDrawer.draw(font, sx, sy + (var56 = var55 + 14.0F), String.format("onknees: %b", this.getVariableBoolean("onknees")), c.r, c.g, c.b, c.a);
                float var57;
                stringDrawer.draw(font, sx, sy + (var57 = var56 + 14.0F), String.format("isCanWalk: %b", isoZombie.isCanWalk()), c.r, c.g, c.b, c.a);
                float var58;
                stringDrawer.draw(font, sx, sy + (var58 = var57 + 14.0F), String.format("isCrawling: %b", isoZombie.isCrawling()), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy = var58 + 14.0F), String.format("isBecomeCrawler: %b", isoZombie.isBecomeCrawler()), c.r, c.g, c.b, c.a);
            } else {
                float var59;
                stringDrawer.draw(
                    font, sx, sy + (var59 = var53 + 14.0F), String.format("isBumped: %b / %s", this.isBumped(), this.getBumpType()), c.r, c.g, c.b, c.a
                );
                float var60;
                stringDrawer.draw(
                    font,
                    sx,
                    sy + (var60 = var59 + 14.0F),
                    String.format("bMoving: %b / %s", this.isMoving(), this.getVariableBoolean("bMoving")),
                    c.r,
                    c.g,
                    c.b,
                    c.a
                );
                stringDrawer.draw(font, sx, sy + (dy = var60 + 14.0F), String.format("bPathfind: %s", this.getVariableBoolean("bPathfind")), c.r, c.g, c.b, c.a);
                if (isoAnimal != null) {
                    float var61;
                    stringDrawer.draw(font, sx, sy + (var61 = dy + 14.0F), String.format("isAlerted: %b", isoAnimal.isAlerted()), c.r, c.g, c.b, c.a);
                    float var62;
                    stringDrawer.draw(
                        font,
                        sx,
                        sy + (var62 = var61 + 14.0F),
                        String.format("animalSpeed: %f", this.getVariableFloat("animalSpeed", -1.0F)),
                        c.r,
                        c.g,
                        c.b,
                        c.a
                    );
                    stringDrawer.draw(
                        font,
                        sx,
                        sy + (dy = var62 + 14.0F),
                        String.format("animalRunning: %s", this.getVariableBoolean(AnimationVariableHandles.animalRunning)),
                        c.r,
                        c.g,
                        c.b,
                        c.a
                    );
                } else if (isoPlayer != null) {
                    float var63;
                    stringDrawer.draw(font, sx, sy + (var63 = dy + 14.0F), String.format("isGrappling: %b", this.isGrappling()), c.r, c.g, c.b, c.a);
                    float var64;
                    stringDrawer.draw(font, sx, sy + (var64 = var63 + 14.0F), String.format("isDoGrapple: %b", this.isDoGrapple()), c.r, c.g, c.b, c.a);
                    float var65;
                    stringDrawer.draw(
                        font, sx, sy + (var65 = var64 + 14.0F), String.format("isDoContinueGrapple: %b", this.isDoContinueGrapple()), c.r, c.g, c.b, c.a
                    );
                    float var66;
                    stringDrawer.draw(
                        font,
                        sx,
                        sy + (var66 = var65 + 14.0F),
                        String.format("IsPerformingAnAction: %b", this.getVariableString("IsPerformingAnAction")),
                        c.r,
                        c.g,
                        c.b,
                        c.a
                    );
                    float var67;
                    stringDrawer.draw(
                        font, sx, sy + (var67 = var66 + 14.0F), String.format("initiateAttack: %b", isoPlayer.isInitiateAttack()), c.r, c.g, c.b, c.a
                    );
                    float var68;
                    stringDrawer.draw(
                        font, sx, sy + (var68 = var67 + 14.0F), String.format("rangedWeapon: %b", this.getVariableBoolean("rangedWeapon")), c.r, c.g, c.b, c.a
                    );
                    stringDrawer.draw(font, sx, sy + (dy = var68 + 14.0F), String.format("bDoShove: %b", isoPlayer.isDoShove()), c.r, c.g, c.b, c.a);
                }
            }
        }

        if (this.getNetworkCharacterAI().getBooleanDebugOptions().enable.getValue()
            && this.getNetworkCharacterAI().getBooleanDebugOptions().animation.getValue()) {
            c = Colors.YellowGreen;
            dy += 4.0F;
            if (this.advancedAnimator.getRootLayer() != null) {
                float var70;
                stringDrawer.draw(font, sx, sy + (var70 = dy + 14.0F), "State: " + this.advancedAnimator.getCurrentStateName(), c.r, c.g, c.b, c.a);
                AnimationPlayer animationPlayer = this.getAnimationPlayer();
                if (animationPlayer != null) {
                    for (AnimationTrack track : animationPlayer.getMultiTrack().getTracks()) {
                        stringDrawer.draw(font, sx, sy + (var70 += 14.0F), "Clip: " + track.currentClip.name, c.r, c.g, c.b, c.a);
                    }
                }
            }
        }
    }

    public float getCorpseSicknessRate() {
        return this.corpseSicknessRate;
    }

    public void setCorpseSicknessRate(float rate) {
        this.corpseSicknessRate = Math.max(0.0F, rate);
    }

    public void spikePartIndex(int bodyPartIndex) {
        DebugLog.Combat.debugln(this + " got spiked in " + BodyPartType.getDisplayName(BodyPartType.FromIndex(bodyPartIndex)));
        HandWeapon weapon = InventoryItemFactory.CreateItem("Base.IcePick");
        if (this.getBodyDamage() == null) {
            this.splatBloodFloorBig();
            this.getEmitter().playSoundImpl(weapon.getZombieHitSound(), null);
            this.Hit(weapon, IsoWorld.instance.currentCell.getFakeZombieForHit(), 0.0F, false, 0.0F);
        } else {
            if (this instanceof IsoAnimal) {
                this.splatBloodFloorBig();
                this.getEmitter().playSoundImpl(weapon.getZombieHitSound(), null);
                this.Hit(weapon, IsoWorld.instance.currentCell.getFakeZombieForHit(), 0.0F, false, 0.0F);
            } else {
                this.getBodyDamage().DamageFromWeapon(weapon, bodyPartIndex);
            }
        }
    }

    public void spikePart(BodyPartType partType) {
        int index = BodyPartType.ToIndex(partType);
        this.spikePartIndex(index);
    }

    public IsoGameCharacter getReanimatedCorpse() {
        return this.reanimatedCorpse;
    }

    public void applyDamage(float damageAmount) {
        this.health -= damageAmount;
        if (this.health < 0.0F) {
            this.health = 0.0F;
        }
    }

    public boolean canRagdoll() {
        if (GameClient.client || GameServer.server) {
            return false;
        } else if (DebugOptions.instance.animation.disableRagdolls.getValue()) {
            return false;
        } else if (!Core.getInstance().getOptionUsePhysicsHitReaction()) {
            return false;
        } else if (GameServer.server && !ServerOptions.getInstance().usePhysicsHitReaction.getValue()) {
            return false;
        } else if (this.getRagdollController() == null && RagdollController.getNumberOfActiveSimulations() >= Core.getInstance().getMaxActiveRagdolls()) {
            return false;
        } else if (!this.wornClothingCanRagdoll) {
            return false;
        } else {
            State currentState = this.getCurrentState();
            return currentState == null || currentState.canRagdoll(this);
        }
    }

    public RagdollController getRagdollController() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        return animationPlayer == null ? null : animationPlayer.getRagdollController();
    }

    public void releaseRagdollController() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null) {
            animationPlayer.releaseRagdollController();
        }
    }

    public BallisticsController getBallisticsController() {
        return this.ballisticsController;
    }

    public void updateBallistics() {
        if (!GameServer.server) {
            if (this.ballisticsController == null) {
                this.ballisticsController = BallisticsController.alloc();
                this.ballisticsController.setIsoGameCharacter(this);
            }

            this.ballisticsController.update();
        }
    }

    public void releaseBallisticsController() {
        if (this.ballisticsController != null) {
            this.ballisticsController.releaseController();
            this.ballisticsController = null;
        }
    }

    public BallisticsTarget getBallisticsTarget() {
        return this.ballisticsTarget;
    }

    public BallisticsTarget ensureExistsBallisticsTarget(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter == null) {
            return null;
        } else {
            if (this.ballisticsTarget == null) {
                this.ballisticsTarget = BallisticsTarget.alloc(isoGameCharacter);
            }

            this.ballisticsTarget.setIsoGameCharacter(isoGameCharacter);
            return this.ballisticsTarget;
        }
    }

    private void updateBallisticsTarget() {
        if (this.ballisticsTarget != null) {
            boolean releaseBallisticsTarget = this.ballisticsTarget.update();
            if (releaseBallisticsTarget) {
                this.releaseBallisticsTarget();
            }
        }
    }

    public void releaseBallisticsTarget() {
        if (this.ballisticsTarget != null) {
            this.ballisticsTarget.releaseTarget();
            this.ballisticsTarget = null;
        }
    }

    public boolean canReachTo(IsoGridSquare square) {
        return this.getSquare().canReachTo(square);
    }

    public boolean canUseAsGenericCraftingSurface(IsoObject object) {
        return object.isGenericCraftingSurface() && this.getSquare().canReachTo(object.getSquare());
    }

    public PZArrayList<HitInfo> getHitInfoList() {
        return this.hitInfoList;
    }

    public AttackVars getAttackVars() {
        return this.attackVars;
    }

    public void addCombatMuscleStrain(InventoryItem weapon) {
        this.addCombatMuscleStrain(weapon, 1);
    }

    public void addCombatMuscleStrain(InventoryItem weapon, int hitCount) {
        this.addCombatMuscleStrain(weapon, hitCount, 1.0F);
    }

    public void addCombatMuscleStrain(InventoryItem item, int hitCount, float multiplier) {
        if (this.isDoStomp()) {
            float val = 0.3F;
            float strengthMod = (15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
            val *= strengthMod;
            this.addRightLegMuscleStrain(val);
        } else if (this.isShoving()) {
            float val = 0.15F;
            float strengthMod = (15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
            val *= strengthMod;
            this.addBothArmMuscleStrain(val);
        } else {
            HandWeapon weapon = null;
            if (item instanceof HandWeapon handWeapon) {
                weapon = handWeapon;
            }

            if (weapon != null && weapon.isAimedFirearm()) {
                float value = weapon.getRecoilDelay(this)
                    * CombatManager.getInstance().getCombatConfig().get(CombatConfigKey.FIREARM_RECOIL_MUSCLE_STRAIN_MODIFIER)
                    * weapon.muscleStrainMod(this)
                    * ((15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F);
                if ("Auto".equalsIgnoreCase(weapon.getFireMode())) {
                    value *= 0.5F;
                }

                value *= (float)SandboxOptions.instance.muscleStrainFactor.getValue();
                if (value != 0.0F) {
                    this.addStiffness(BodyPartType.Hand_R, value);
                    this.addStiffness(BodyPartType.ForeArm_R, value);
                    if (this.getSecondaryHandItem() == weapon) {
                        this.addStiffness(BodyPartType.UpperArm_R, value);
                        this.addStiffness(BodyPartType.Hand_L, value * 0.1F);
                        this.addStiffness(BodyPartType.ForeArm_L, value * 0.1F);
                    }
                }
            } else if (this.isActuallyAttackingWithMeleeWeapon() && item != null) {
                if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0
                    && weapon != null
                    && weapon.isUseEndurance()
                    && WeaponType.getWeaponType(this) != WeaponType.UNARMED
                    && !weapon.isRanged()) {
                    if (hitCount <= 0) {
                        hitCount = 1;
                    }

                    if (multiplier <= 0.0F) {
                        multiplier = 1.0F;
                    }

                    boolean twoHandedWeapon = weapon.isTwoHandWeapon();
                    boolean twoHandedUsedOneHand = weapon.isTwoHandWeapon() && (this.getPrimaryHandItem() != weapon || this.getSecondaryHandItem() != weapon);
                    float enduranceTwoHandsWeaponModifier = 0.0F;
                    if (twoHandedUsedOneHand) {
                        enduranceTwoHandsWeaponModifier = weapon.getWeight() / 1.5F / 10.0F;
                        twoHandedWeapon = false;
                    }

                    float val = (weapon.getWeight() * 0.15F * weapon.getEnduranceMod() * 0.3F + enduranceTwoHandsWeaponModifier) * 4.0F;
                    float mod = 1.0F;
                    mod *= hitCount + 1;
                    val *= mod;
                    float strengthMod = (15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
                    val *= strengthMod;
                    val *= weapon.muscleStrainMod(this);
                    val *= multiplier;
                    if (twoHandedWeapon) {
                        val *= 0.5F;
                    }

                    val = (float)(val * 0.65);
                    this.addArmMuscleStrain(val);
                    if (twoHandedWeapon) {
                        this.addLeftArmMuscleStrain(val);
                    }
                } else if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0 && weapon == null && item != null) {
                    float valx = item.getWeight() * 0.15F * 0.3F * 4.0F;
                    float strengthModx = (15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
                    valx *= strengthModx;
                    valx = (float)(valx * 0.65);
                    this.addArmMuscleStrain(valx);
                }
            }
        }
    }

    public void addRightLegMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5F;
            painfactor = (float)(painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.UpperLeg_R, painfactor);
            this.addStiffness(BodyPartType.LowerLeg_R, painfactor);
            this.addStiffness(BodyPartType.Foot_R, painfactor);
        }
    }

    public void addBackMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5F;
            painfactor = (float)(painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Torso_Upper, painfactor);
            this.addStiffness(BodyPartType.Torso_Lower, painfactor);
        }
    }

    public void addNeckMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5F;
            painfactor = (float)(painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Neck, painfactor);
        }
    }

    public void addArmMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5F;
            painfactor = (float)(painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Hand_R, painfactor);
            this.addStiffness(BodyPartType.ForeArm_R, painfactor);
            this.addStiffness(BodyPartType.UpperArm_R, painfactor);
        }
    }

    public void addLeftArmMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5F;
            painfactor = (float)(painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Hand_L, painfactor);
            this.addStiffness(BodyPartType.ForeArm_L, painfactor);
            this.addStiffness(BodyPartType.UpperArm_L, painfactor);
        }
    }

    public void addBothArmMuscleStrain(float painfactor) {
        this.addArmMuscleStrain(painfactor);
        this.addLeftArmMuscleStrain(painfactor);
    }

    public void addStiffness(BodyPartType partType, float stiffness) {
        BodyPart part = this.getBodyDamage().getBodyPart(partType);
        part.addStiffness(stiffness);
    }

    public int getClimbingFailChanceInt() {
        return (int)this.getClimbingFailChanceFloat();
    }

    public float getClimbingFailChanceFloat() {
        float failChance = 0.0F;
        failChance += this.getPerkLevel(PerkFactory.Perks.Fitness) * 2.0F;
        failChance += this.getPerkLevel(PerkFactory.Perks.Strength) * 2.0F;
        failChance += this.getPerkLevel(PerkFactory.Perks.Nimble) * 2.0F;
        failChance += this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * -5.0F;
        failChance += this.getMoodles().getMoodleLevel(MoodleType.DRUNK) * -8.0F;
        failChance += this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * -8.0F;
        failChance += this.getMoodles().getMoodleLevel(MoodleType.PAIN) * -5.0F;
        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            failChance += -25.0F;
        } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            failChance += -15.0F;
        }

        if (this.characterTraits.get(CharacterTrait.CLUMSY)) {
            failChance /= 2.0F;
        }

        if (this.isWearingAwkwardGloves()) {
            failChance /= 2.0F;
        } else if (!this.isWearingAwkwardGloves() && this.isWearingGloves()) {
            failChance += 4.0F;
        }

        if (this.characterTraits.get(CharacterTrait.ALL_THUMBS)) {
            failChance += -4.0F;
        } else if (this.characterTraits.get(CharacterTrait.DEXTROUS)) {
            failChance += 4.0F;
        }

        if (this.characterTraits.get(CharacterTrait.BURGLAR)) {
            failChance += 4.0F;
        }

        if (this.characterTraits.get(CharacterTrait.GYMNAST)) {
            failChance += 4.0F;
        }

        failChance += this.nearbyZombieClimbPenalty();
        failChance = Math.max(0.0F, failChance);
        return (int)Math.sqrt(failChance);
    }

    public float nearbyZombieClimbPenalty() {
        float penaltyChance = 0.0F;
        IsoGridSquare current = this.getCurrentSquare();
        if (current == null) {
            return penaltyChance;
        } else {
            for (int i = 0; i < current.getMovingObjects().size(); i++) {
                IsoMovingObject mov = current.getMovingObjects().get(i);
                if (mov instanceof IsoZombie isoZombie) {
                    if (isoZombie.target == this && isoZombie.getCurrentState() == AttackState.instance()) {
                        penaltyChance += 25.0F;
                    } else {
                        penaltyChance += 7.0F;
                    }
                }
            }

            return penaltyChance;
        }
    }

    public boolean isClimbingRope() {
        return this.getCurrentState().equals(ClimbSheetRopeState.instance()) || this.getCurrentState().equals(ClimbDownSheetRopeState.instance());
    }

    public void fallFromRope() {
        if (this.isClimbingRope()) {
            this.setCollidable(true);
            this.setbClimbing(false);
            this.setbFalling(true);
            this.clearVariable("ClimbRope");
            this.setLlz(this.getZ());
        }
    }

    public boolean isWearingGloves() {
        return this.getWornItem(ItemBodyLocation.HANDS) != null;
    }

    public boolean isWearingAwkwardGloves() {
        return this.getWornItem(ItemBodyLocation.HANDS) != null && this.getWornItem(ItemBodyLocation.HANDS).hasTag(ItemTag.AWKWARD_GLOVES);
    }

    public float getClimbRopeSpeed(boolean down) {
        int effectiveStrength = Math.max(this.getPerkLevel(PerkFactory.Perks.Strength), this.getPerkLevel(PerkFactory.Perks.Fitness))
            - (
                this.getMoodles().getMoodleLevel(MoodleType.DRUNK)
                    + this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE)
                    + this.getMoodles().getMoodleLevel(MoodleType.PAIN)
            );
        if (!down) {
            effectiveStrength -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD);
            if (this.characterTraits.get(CharacterTrait.OBESE)) {
                effectiveStrength -= 2;
            } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                effectiveStrength--;
            }
        }

        if (this.characterTraits.get(CharacterTrait.ALL_THUMBS)) {
            effectiveStrength--;
        } else if (this.characterTraits.get(CharacterTrait.DEXTROUS)) {
            effectiveStrength++;
        }

        if (this.characterTraits.get(CharacterTrait.BURGLAR)) {
            effectiveStrength++;
        }

        if (this.characterTraits.get(CharacterTrait.GYMNAST)) {
            effectiveStrength++;
        }

        if (this.isWearingAwkwardGloves()) {
            effectiveStrength /= 2;
        } else if (this.isWearingGloves()) {
            effectiveStrength++;
        }

        effectiveStrength = Math.max(0, effectiveStrength);
        effectiveStrength = Math.min(10, effectiveStrength);
        float climbSpeed = 0.16F;
        switch (effectiveStrength) {
            case 0:
                climbSpeed -= 0.12F;
                break;
            case 1:
                climbSpeed -= 0.11F;
                break;
            case 2:
                climbSpeed -= 0.1F;
                break;
            case 3:
                climbSpeed -= 0.09F;
            case 4:
            case 5:
            default:
                break;
            case 6:
                climbSpeed += 0.02F;
                break;
            case 7:
                climbSpeed += 0.05F;
                break;
            case 8:
                climbSpeed += 0.07F;
                break;
            case 9:
                climbSpeed += 0.09F;
                break;
            case 10:
                climbSpeed += 0.12F;
        }

        return climbSpeed * 0.5F;
    }

    public void setClimbRopeTime(float time) {
        this.climbRopeTime = time;
    }

    public float getClimbRopeTime() {
        return this.climbRopeTime;
    }

    public boolean hasAwkwardHands() {
        return this.isWearingAwkwardGloves() || this.characterTraits.get(CharacterTrait.ALL_THUMBS);
    }

    private boolean forbidConcurrentAction(String action) {
        return this.concurrentActionList.contains(action) ? false : this.isDoingActionThatCanBeCancelled();
    }

    @Override
    public void triggerContextualAction(String action) {
        if (!this.forbidConcurrentAction(action)) {
            LuaHookManager.TriggerHook("ContextualAction", action, this);
        }
    }

    @Override
    public void triggerContextualAction(String action, Object param1) {
        if (!this.forbidConcurrentAction(action)) {
            LuaHookManager.TriggerHook("ContextualAction", action, this, param1);
        }
    }

    @Override
    public void triggerContextualAction(String action, Object param1, Object param2) {
        if (!this.forbidConcurrentAction(action)) {
            LuaHookManager.TriggerHook("ContextualAction", action, this, param1, param2);
        }
    }

    @Override
    public void triggerContextualAction(String action, Object param1, Object param2, Object param3) {
        if (!this.forbidConcurrentAction(action)) {
            LuaHookManager.TriggerHook("ContextualAction", action, this, param1, param2, param3);
        }
    }

    @Override
    public void triggerContextualAction(String action, Object param1, Object param2, Object param3, Object param4) {
        if (!this.forbidConcurrentAction(action)) {
            LuaHookManager.TriggerHook("ContextualAction", action, this, param1, param2, param3, param4);
        }
    }

    public boolean isActuallyAttackingWithMeleeWeapon() {
        if (this.getPrimaryHandItem() == null) {
            return false;
        } else if (!(this.getPrimaryHandItem() instanceof HandWeapon)) {
            return false;
        } else if (this.getUseHandWeapon() == null) {
            return false;
        } else {
            HandWeapon weapon = this.getUseHandWeapon();
            if (weapon.isBareHands()) {
                return false;
            } else if (weapon.isRanged()) {
                return false;
            } else {
                return this.isShoving() ? false : !this.isDoStomp();
            }
        }
    }

    public boolean isDoStomp() {
        return false;
    }

    public boolean isShoving() {
        return false;
    }

    public void teleportTo(int newX, int newY) {
        this.teleportTo(newX, newY, 0);
    }

    public void teleportTo(float newX, float newY) {
        this.teleportTo((int)newX, (int)newY, 0);
    }

    public void teleportTo(float newX, float newY, int newZ) {
        this.teleportTo(PZMath.fastfloor(newX), PZMath.fastfloor(newY), newZ);
    }

    public void teleportTo(int newX, int newY, int newZ) {
        this.ensureNotInVehicle();
        newZ = Math.max(-32, newZ);
        newZ = Math.min(31, newZ);
        this.setX(newX);
        this.setY(newY);
        this.setZ(newZ);
        this.setLastX(newX);
        this.setLastY(newY);
        this.ensureOnTile();
    }

    public void ensureNotInVehicle() {
        if (this.getVehicle() != null) {
            this.getVehicle().exit(this);
            LuaEventManager.triggerEvent("OnExitVehicle", this);
        }
    }

    public void forgetRecipes() {
        this.getKnownRecipes().clear();
    }

    protected boolean isHandModelOverriddenByCurrentCharacterAction() {
        BaseAction action = this.characterActions.isEmpty() ? null : this.characterActions.get(0);
        return action == null ? false : action.overrideHandModels;
    }

    protected boolean isPrimaryHandModelReady() {
        return this.primaryHandModel != null && this.primaryHandModel.model != null && this.primaryHandModel.model.isReady();
    }

    private boolean isRangedWeaponReady() {
        if (this.useHandWeapon != null && this.useHandWeapon.isAimedFirearm() && this.isPrimaryHandModelReady() && this.primaryHandModel.modelScript != null) {
            ModelAttachment attachment = this.primaryHandModel.modelScript.getAttachmentById("muzzle");
            return attachment != null;
        } else {
            return true;
        }
    }

    public boolean isWeaponReady() {
        return !this.isHandModelOverriddenByCurrentCharacterAction() && this.isPrimaryHandModelReady() && this.isRangedWeaponReady();
    }

    public void climbThroughWindow(IsoObject isoObject) {
        if (isoObject instanceof IsoWindow isoWindow) {
            this.climbThroughWindow(isoWindow);
        } else if (isoObject instanceof IsoThumpable isoThumpable) {
            this.climbThroughWindow(isoThumpable);
        }
    }

    public ClimbSheetRopeState.ClimbData getClimbData() {
        return this.climbData;
    }

    public void setClimbData(ClimbSheetRopeState.ClimbData climbData) {
        this.climbData = climbData;
    }

    public float getIdleSquareTime() {
        return this.idleSquareTime;
    }

    private void updateIdleSquareTime() {
        if (this.getCurrentSquare() == this.getLastSquare()) {
            if (this.idleSquareTime <= 3600.0F) {
                this.idleSquareTime = this.idleSquareTime + 1.0F * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay();
            }
        } else {
            this.idleSquareTime = 0.0F;
        }
    }

    public boolean isCurrentlyIdle() {
        if (!(this instanceof IsoPlayer player)) {
            return false;
        } else if (!player.isPlayerMoving() || !player.isWalking() && !player.isRunning() && !player.isSprinting()) {
            if (this.isAsleep()) {
                return false;
            } else if ((this.isSittingOnFurniture() || this.isSitOnGround()) && this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) >= 1) {
                return false;
            } else if (this.isReading()) {
                return false;
            } else if (!this.characterActions.isEmpty()) {
                return false;
            } else if (this.getMoodles().getMoodleLevel(MoodleType.PANIC) > 1) {
                return false;
            } else if (this.isInCombat()) {
                return false;
            } else {
                if (GameServer.server) {
                    if (player.networkAi.getState().getEnterState() != null
                        && !IdleState.instance().equals(player.networkAi.getState().getEnterState().getState())) {
                        return false;
                    }

                    if (player.networkAi.getState().getExitState() != null
                        && !IdleState.instance().equals(player.networkAi.getState().getExitState().getState())) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public boolean isCurrentlyBusy() {
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player != null) {
            if (this.isAsleep()) {
                return true;
            } else if (this.isReading()) {
                return true;
            } else {
                return !this.characterActions.isEmpty() && !this.characterActions.get(0).isPathfinding() ? true : this.isInCombat();
            }
        } else {
            return true;
        }
    }

    private boolean isInCombat() {
        return this.stats.getNumVeryCloseZombies() > 0 || this.stats.getNumChasingZombies() >= 3;
    }

    private void updateMovementStatistics() {
        if (this.isoPlayer != null) {
            tempVector2_1.set(this.getLastX(), this.getLastY());
            tempVector2_2.set(this.getX(), this.getY());
            float distanceTraveled = tempVector2_1.distanceTo(tempVector2_2);
            if (this.isSprinting()) {
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Sprinted", distanceTraveled);
            } else if (this.isRunning()) {
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Ran", distanceTraveled);
            } else if (this.isDriving()) {
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Driven", distanceTraveled);
            } else if (this.isoPlayer.isPlayerMoving) {
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Walked", distanceTraveled);
            }

            if (distanceTraveled != 0.0F) {
                StatisticsManager.getInstance()
                    .setStatistic(StatisticType.Player, StatisticCategory.Travel, "Travel Speed", distanceTraveled * GameWindow.averageFPS);
            }
        }
    }

    @Override
    public void flagForHotSave() {
    }

    public ArrayList<ItemContainer> getContainers() {
        ArrayList<ItemContainer> list = null;
        ArrayList<ItemContainer> list2 = null;

        for (int i = 0; i < list.size(); i++) {
            ItemContainer cont = list.get(i);
            boolean locked = cont.getParent() != null && cont.getParent() instanceof IsoThumpable && ((IsoThumpable)cont.getParent()).isLockedToCharacter(this);
            if (!locked) {
                list2.add(cont);
            }
        }

        return list2;
    }

    public boolean hasRecipeAtHand(CraftRecipe recipe) {
        return recipe.getMetaRecipe() != null && this.getInventory().hasRecipe(recipe.getMetaRecipe(), this, true)
            ? true
            : this.getInventory().hasRecipe(recipe.getName(), this, true);
    }

    public PlayerCheats getCheats() {
        return this.cheats;
    }

    public VisibilityData calculateVisibilityData() {
        float currentFatigue = this.stats.get(CharacterStat.FATIGUE);
        float fatigue = Math.max(0.0F, currentFatigue - 0.6F) * 2.5F;
        float cone = -0.2F - fatigue;
        float noiseDistance = 2.0F;
        if (fatigue >= 1.0F) {
            cone -= 0.2F;
        }

        cone -= this.stats.get(CharacterStat.INTOXICATION) * 0.002F * (this.moodles.getMoodleLevel(MoodleType.DRUNK) >= 2 ? 1 : 0);
        if (this.moodles.getMoodleLevel(MoodleType.PANIC) == 4) {
            cone -= 0.2F;
        }

        if (this.characterTraits.get(CharacterTrait.EAGLE_EYED)) {
            cone += 0.2F;
        }

        if (this instanceof IsoPlayer && this.getVehicle() != null) {
            cone = 1.0F;
        }

        if (this.characterTraits.get(CharacterTrait.HARD_OF_HEARING)) {
            noiseDistance--;
        }

        if (this.characterTraits.get(CharacterTrait.KEEN_HEARING)) {
            noiseDistance += 3.0F;
        }

        noiseDistance *= this.getWornItemsHearingMultiplier();
        float baseAmbient = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex());
        return new VisibilityData(fatigue, noiseDistance, cone, baseAmbient);
    }

    static {
        s_turn180StartedEvent.time = AnimEvent.AnimEventTime.END;
        s_turn180StartedEvent.eventName = "Turn180Started";
        s_turn180TargetChangedEvent.time = AnimEvent.AnimEventTime.END;
        s_turn180TargetChangedEvent.eventName = "Turn180TargetChanged";
    }

    private static final class Bandages {
        private final HashMap<String, String> bandageTypeMap = new HashMap<>();
        private final THashMap<String, InventoryItem> itemMap = new THashMap<>();

        private String getBloodBandageType(String type) {
            String typeBlood = this.bandageTypeMap.get(type);
            if (typeBlood == null) {
                this.bandageTypeMap.put(type, typeBlood = type + "_Blood");
            }

            return typeBlood;
        }

        private void update(IsoGameCharacter chr) {
            if (!GameServer.server) {
                BodyDamage bodyDamage = chr.getBodyDamage();
                WornItems wornItems = chr.getWornItems();
                if (bodyDamage != null && wornItems != null) {
                    assert !(chr instanceof IsoZombie);

                    this.itemMap.clear();

                    for (int i = 0; i < wornItems.size(); i++) {
                        InventoryItem item = wornItems.getItemByIndex(i);
                        if (item != null) {
                            this.itemMap.put(item.getFullType(), item);
                        }
                    }

                    for (int ix = 0; ix < BodyPartType.ToIndex(BodyPartType.MAX); ix++) {
                        BodyPart bodyPart = bodyDamage.getBodyPart(BodyPartType.FromIndex(ix));
                        BodyPartLast bodyPartLastState = bodyDamage.getBodyPartsLastState(BodyPartType.FromIndex(ix));
                        String bandageType = bodyPart.getType().getBandageModel();
                        if (!StringUtils.isNullOrWhitespace(bandageType)) {
                            String bandageTypeBlood = this.getBloodBandageType(bandageType);
                            if (bodyPart.bandaged() != bodyPartLastState.bandaged() || bodyPart.isBandageDirty() != bodyPartLastState.isBandageDirty()) {
                                if (bodyPart.bandaged()) {
                                    if (bodyPart.isBandageDirty()) {
                                        this.removeBandageModel(chr, bandageType);
                                        this.addBandageModel(chr, bandageTypeBlood);
                                    } else {
                                        this.removeBandageModel(chr, bandageTypeBlood);
                                        this.addBandageModel(chr, bandageType);
                                    }
                                } else {
                                    this.removeBandageModel(chr, bandageType);
                                    this.removeBandageModel(chr, bandageTypeBlood);
                                }
                            }

                            if (bodyPart.bitten() != bodyPartLastState.bitten()) {
                                if (bodyPart.bitten()) {
                                    String woundType = bodyPart.getType().getBiteWoundModel(chr.isFemale());
                                    if (StringUtils.isNullOrWhitespace(woundType)) {
                                        continue;
                                    }

                                    this.addBandageModel(chr, woundType);
                                } else {
                                    this.removeBandageModel(chr, bodyPart.getType().getBiteWoundModel(chr.isFemale()));
                                }
                            }

                            if (bodyPart.scratched() != bodyPartLastState.scratched()) {
                                if (bodyPart.scratched()) {
                                    String woundType = bodyPart.getType().getScratchWoundModel(chr.isFemale());
                                    if (StringUtils.isNullOrWhitespace(woundType)) {
                                        continue;
                                    }

                                    this.addBandageModel(chr, woundType);
                                } else {
                                    this.removeBandageModel(chr, bodyPart.getType().getScratchWoundModel(chr.isFemale()));
                                }
                            }

                            if (bodyPart.isCut() != bodyPartLastState.isCut()) {
                                if (bodyPart.isCut()) {
                                    String woundType = bodyPart.getType().getCutWoundModel(chr.isFemale());
                                    if (!StringUtils.isNullOrWhitespace(woundType)) {
                                        this.addBandageModel(chr, woundType);
                                    }
                                } else {
                                    this.removeBandageModel(chr, bodyPart.getType().getCutWoundModel(chr.isFemale()));
                                }
                            }
                        }
                    }
                }
            }
        }

        private void addBandageModel(IsoGameCharacter chr, String type) {
            if (!this.itemMap.containsKey(type)) {
                if (InventoryItemFactory.CreateItem(type) instanceof Clothing bandageItem) {
                    chr.getInventory().addItem(bandageItem);
                    chr.setWornItem(bandageItem.getBodyLocation(), bandageItem);
                    chr.resetModelNextFrame();
                }
            }
        }

        private void removeBandageModel(IsoGameCharacter chr, String bandageType) {
            InventoryItem item = this.itemMap.get(bandageType);
            if (item != null) {
                chr.getWornItems().remove(item);
                chr.getInventory().Remove(item);
                chr.resetModelNextFrame();
                chr.onWornItemsChanged();
            }
        }
    }

    public static enum BodyLocation {
        Head,
        Leg,
        Arm,
        Chest,
        Stomach,
        Foot,
        Hand;
    }

    private static final class L_actionStateChanged {
        private static final ArrayList<String> stateNames = new ArrayList<>();
        private static final ArrayList<State> states = new ArrayList<>();
    }

    private static final class L_getDotWithForwardDirection {
        private static final Vector2 v1 = new Vector2();
        private static final Vector2 v2 = new Vector2();
    }

    private static class L_postUpdate {
        static final MoveDeltaModifiers moveDeltas = new MoveDeltaModifiers();
    }

    private static final class L_renderLast {
        private static final Color color = new Color();
    }

    private static final class L_renderShadow {
        static final ShadowParams shadowParams = new ShadowParams(1.0F, 1.0F, 1.0F);
        static final Vector2 vector2_1 = new Vector2();
        static final Vector2 vector2_2 = new Vector2();
        static final Vector3f forward = new Vector3f();
        static final Vector3 vector3 = new Vector3();
        static final Vector3f vector3f = new Vector3f();
    }

    public static class LightInfo {
        public IsoGridSquare square;
        public float x;
        public float y;
        public float z;
        public float angleX;
        public float angleY;
        public ArrayList<IsoGameCharacter.TorchInfo> torches = new ArrayList<>();
        public long time;
        public float night;
        public float rmod;
        public float gmod;
        public float bmod;

        public void initFrom(IsoGameCharacter.LightInfo other) {
            this.square = other.square;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.angleX = other.angleX;
            this.angleY = other.angleY;
            this.torches.clear();
            this.torches.addAll(other.torches);
            this.time = (long)(System.nanoTime() / 1000000.0);
            this.night = other.night;
            this.rmod = other.rmod;
            this.gmod = other.gmod;
            this.bmod = other.bmod;
        }
    }

    @UsedFromLua
    public static class Location {
        public int x;
        public int y;
        public int z;

        public Location() {
        }

        public Location(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public IsoGameCharacter.Location set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
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

        public boolean equals(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }

        @Override
        public boolean equals(Object other) {
            return !(other instanceof IsoGameCharacter.Location location) ? false : this.x == location.x && this.y == location.y && this.z == location.z;
        }
    }

    @UsedFromLua
    public class PerkInfo {
        public int level;
        public PerkFactory.Perk perk;

        public PerkInfo() {
            Objects.requireNonNull(IsoGameCharacter.this);
            super();
        }

        public int getLevel() {
            return this.level;
        }
    }

    private static class ReadBook {
        private String fullType;
        private int alreadyReadPages;
    }

    private static class Recoil {
        private float recoilVarX;
        private float recoilVarY;
    }

    public static class TorchInfo {
        private static final ObjectPool<IsoGameCharacter.TorchInfo> TorchInfoPool = new ObjectPool<>(IsoGameCharacter.TorchInfo::new);
        private static final Vector3f tempVector3f = new Vector3f();
        public int id;
        public float x;
        public float y;
        public float z;
        public float angleX;
        public float angleY;
        public float dist;
        public float strength;
        public boolean cone;
        public float dot;
        public int focusing;

        public static IsoGameCharacter.TorchInfo alloc() {
            return TorchInfoPool.alloc();
        }

        public static void release(IsoGameCharacter.TorchInfo info) {
            TorchInfoPool.release(info);
        }

        public IsoGameCharacter.TorchInfo set(IsoPlayer p, InventoryItem item) {
            this.x = p.getX();
            this.y = p.getY();
            this.z = p.getZ();
            Vector2 lookVector = p.getLookVector(IsoGameCharacter.tempVector2);
            this.angleX = lookVector.x;
            this.angleY = lookVector.y;
            this.dist = item.getLightDistance();
            this.strength = item.getLightStrength();
            this.cone = item.isTorchCone();
            this.dot = item.getTorchDot();
            this.focusing = 0;
            return this;
        }

        public IsoGameCharacter.TorchInfo set(VehiclePart part) {
            BaseVehicle vehicle = part.getVehicle();
            VehicleLight light = part.getLight();
            VehicleScript script = vehicle.getScript();
            Vector3f vec = tempVector3f;
            vec.set(light.offset.x * script.getExtents().x / 2.0F, 0.0F, light.offset.y * script.getExtents().z / 2.0F);
            vehicle.getWorldPos(vec, vec);
            this.x = vec.x;
            this.y = vec.y;
            this.z = vec.z;
            vec = vehicle.getForwardVector(vec);
            this.angleX = vec.x;
            this.angleY = vec.z;
            this.dist = part.getLightDistance();
            this.strength = part.getLightIntensity();
            this.cone = true;
            this.dot = light.dot;
            this.focusing = (int)part.getLightFocusing();
            return this;
        }
    }

    @UsedFromLua
    public class XP implements AntiCheatXPUpdate.IAntiCheatUpdate {
        public int level;
        public int lastlevel;
        public float totalXp;
        public HashMap<PerkFactory.Perk, Float> xpMap;
        public HashMap<PerkFactory.Perk, IsoGameCharacter.XPMultiplier> xpMapMultiplier;
        private final IsoGameCharacter chr;
        private static final long XP_INTERVAL = 60000L;
        private final UpdateLimit ulInterval;
        private float sum;

        public XP(final IsoGameCharacter chr) {
            Objects.requireNonNull(IsoGameCharacter.this);
            super();
            this.xpMap = new HashMap<>();
            this.xpMapMultiplier = new HashMap<>();
            this.ulInterval = new UpdateLimit(60000L);
            this.chr = chr;
        }

        @Override
        public boolean intervalCheck() {
            return this.ulInterval.Check();
        }

        @Override
        public float getGrowthRate() {
            this.ulInterval.Reset(60000L);
            float sum = 0.0F;

            for (Float value : this.xpMap.values()) {
                sum += value;
            }

            float rate = sum - this.sum;
            this.sum = sum;
            return rate;
        }

        @Override
        public float getMultiplier() {
            double multiplier = 0.0;
            if (SandboxOptions.instance.multipliersConfig.xpMultiplierGlobalToggle.getValue()) {
                multiplier = SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue();
            } else {
                int optionCount = 0;

                for (int i = 0; i < IsoGameCharacter.this.getPerkList().size(); i++) {
                    String optionName = "MultiplierConfig." + IsoGameCharacter.this.getPerkList().get(i);
                    if (SandboxOptions.instance.getOptionByName(optionName) != null) {
                        optionCount++;
                        multiplier += Double.parseDouble(SandboxOptions.instance.getOptionByName(optionName).asConfigOption().getValueAsString());
                    }
                }

                multiplier /= optionCount;
            }

            return (float)multiplier;
        }

        public void addXpMultiplier(PerkFactory.Perk perks, float multiplier, int minLevel, int maxLevel) {
            IsoGameCharacter.XPMultiplier xpMultiplier = this.xpMapMultiplier.get(perks);
            if (xpMultiplier == null) {
                xpMultiplier = new IsoGameCharacter.XPMultiplier();
            }

            xpMultiplier.multiplier = multiplier;
            xpMultiplier.minLevel = minLevel;
            xpMultiplier.maxLevel = maxLevel;
            this.xpMapMultiplier.put(perks, xpMultiplier);
        }

        public HashMap<PerkFactory.Perk, IsoGameCharacter.XPMultiplier> getMultiplierMap() {
            return this.xpMapMultiplier;
        }

        public float getMultiplier(PerkFactory.Perk perk) {
            IsoGameCharacter.XPMultiplier xpMultiplier = this.xpMapMultiplier.get(perk);
            return xpMultiplier == null ? 0.0F : xpMultiplier.multiplier;
        }

        public int getPerkBoost(PerkFactory.Perk type) {
            return IsoGameCharacter.this.getDescriptor().getXPBoostMap().get(type) != null
                ? IsoGameCharacter.this.getDescriptor().getXPBoostMap().get(type)
                : 0;
        }

        public void setPerkBoost(PerkFactory.Perk perk, int level) {
            if (perk != null && perk != PerkFactory.Perks.None && perk != PerkFactory.Perks.MAX) {
                level = PZMath.clamp(level, 0, 10);
                if (level == 0) {
                    IsoGameCharacter.this.getDescriptor().getXPBoostMap().remove(perk);
                } else {
                    IsoGameCharacter.this.getDescriptor().getXPBoostMap().put(perk, level);
                }
            }
        }

        public int getLevel() {
            return this.level;
        }

        public void setLevel(int newlevel) {
            this.level = newlevel;
        }

        public float getTotalXp() {
            return this.totalXp;
        }

        public void AddXP(PerkFactory.Perk type, float amount) {
            if (this.chr instanceof IsoPlayer player && player.isLocalPlayer()) {
                this.AddXP(type, amount, true, true, false);
            }
        }

        public void AddXPHaloText(PerkFactory.Perk type, float amount) {
            if (this.chr instanceof IsoPlayer player && player.isLocalPlayer()) {
                this.AddXP(type, amount, true, true, false, true);
            }
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean noMultiplier) {
            if (this.chr instanceof IsoPlayer player && player.isLocalPlayer()) {
                this.AddXP(type, amount, true, !noMultiplier, false, false);
            }
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean noMultiplier, boolean haloText) {
            if (this.chr instanceof IsoPlayer player && player.isLocalPlayer()) {
                this.AddXP(type, amount, true, !noMultiplier, false, haloText);
            }
        }

        public void AddXPNoMultiplier(PerkFactory.Perk type, float amount) {
            IsoGameCharacter.XPMultiplier xpMultiplier = this.getMultiplierMap().remove(type);

            try {
                this.AddXP(type, amount);
            } finally {
                if (xpMultiplier != null) {
                    this.getMultiplierMap().put(type, xpMultiplier);
                }
            }
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean callLua, boolean doXPBoost, boolean remote) {
            this.AddXP(type, amount, callLua, doXPBoost, remote, false);
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean callLua, boolean doXPBoost, boolean remote, boolean haloText) {
            PerkFactory.Perk perk = null;

            for (int n = 0; n < PerkFactory.PerkList.size(); n++) {
                PerkFactory.Perk info = PerkFactory.PerkList.get(n);
                if (info.getType() == type) {
                    perk = info;
                    break;
                }
            }

            if (perk.getType() != PerkFactory.Perks.Fitness || !(this.chr instanceof IsoPlayer isoPlayer && !isoPlayer.getNutrition().canAddFitnessXp())) {
                if (perk.getType() == PerkFactory.Perks.Strength && this.chr instanceof IsoPlayer isoPlayerx) {
                    if (isoPlayerx.getNutrition().getProteins() > 50.0F && isoPlayerx.getNutrition().getProteins() < 300.0F) {
                        amount *= 1.5F;
                    }

                    if (isoPlayerx.getNutrition().getProteins() < -300.0F) {
                        amount *= 0.7F;
                    }
                }

                float oldXP = this.getXP(type);
                float maxXP = perk.getTotalXpForLevel(10);
                if (!(amount >= 0.0F) || !(oldXP >= maxXP)) {
                    float mod = 1.0F;
                    if (doXPBoost) {
                        boolean bDoneIt = false;

                        for (Entry<PerkFactory.Perk, Integer> entry : IsoGameCharacter.this.getDescriptor().getXPBoostMap().entrySet()) {
                            if (entry.getKey() == perk.getType()) {
                                bDoneIt = true;
                                if (entry.getValue() == 0 && !this.isSkillExcludedFromSpeedReduction(entry.getKey())) {
                                    mod *= 0.25F;
                                } else if (entry.getValue() == 1 && entry.getKey() == PerkFactory.Perks.Sprinting) {
                                    mod *= 1.25F;
                                } else if (entry.getValue() == 1) {
                                    mod *= 1.0F;
                                } else if (entry.getValue() == 2 && !this.isSkillExcludedFromSpeedIncrease(entry.getKey())) {
                                    mod *= 1.33F;
                                } else if (entry.getValue() >= 3 && !this.isSkillExcludedFromSpeedIncrease(entry.getKey())) {
                                    mod *= 1.66F;
                                }
                            }
                        }

                        if (!bDoneIt && !this.isSkillExcludedFromSpeedReduction(perk.getType())) {
                            mod = 0.25F;
                        }

                        if (IsoGameCharacter.this.characterTraits.get(CharacterTrait.FAST_LEARNER) && !this.isSkillExcludedFromSpeedIncrease(perk.getType())) {
                            mod *= 1.3F;
                        }

                        if (IsoGameCharacter.this.characterTraits.get(CharacterTrait.SLOW_LEARNER) && !this.isSkillExcludedFromSpeedReduction(perk.getType())) {
                            mod *= 0.7F;
                        }

                        if (IsoGameCharacter.this.characterTraits.get(CharacterTrait.PACIFIST)) {
                            if (perk.getType() == PerkFactory.Perks.SmallBlade
                                || perk.getType() == PerkFactory.Perks.LongBlade
                                || perk.getType() == PerkFactory.Perks.SmallBlunt
                                || perk.getType() == PerkFactory.Perks.Spear
                                || perk.getType() == PerkFactory.Perks.Blunt
                                || perk.getType() == PerkFactory.Perks.Axe) {
                                mod *= 0.75F;
                            } else if (perk.getType() == PerkFactory.Perks.Aiming) {
                                mod *= 0.75F;
                            }
                        }

                        if (IsoGameCharacter.this.characterTraits.get(CharacterTrait.CRAFTY)
                            && perk.getParent() != null
                            && perk.getParent() == PerkFactory.Perks.Crafting) {
                            mod *= 1.3F;
                        }

                        amount *= mod;
                        float multiplier = this.getMultiplier(type);
                        if (multiplier > 1.0F) {
                            amount *= multiplier;
                        }

                        if (SandboxOptions.instance.multipliersConfig.xpMultiplierGlobalToggle.getValue()) {
                            amount = (float)(amount * SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue());
                        } else {
                            amount *= Float.parseFloat(
                                SandboxOptions.instance.getOptionByName("MultiplierConfig." + perk.getType()).asConfigOption().getValueAsString()
                            );
                        }
                    }

                    float newXP = oldXP + amount;
                    if (newXP < 0.0F) {
                        newXP = 0.0F;
                        amount = -oldXP;
                    }

                    if (newXP > maxXP) {
                        newXP = maxXP;
                        amount = maxXP - oldXP;
                    }

                    this.xpMap.put(type, newXP);
                    IsoGameCharacter.XPMultiplier xpMultiplier = this.getMultiplierMap().get(perk);
                    if (xpMultiplier != null) {
                        float xpMin = perk.getTotalXpForLevel(xpMultiplier.minLevel - 1);
                        float xpMax = perk.getTotalXpForLevel(xpMultiplier.maxLevel);
                        if (oldXP >= xpMin && newXP < xpMin || oldXP < xpMax && newXP >= xpMax) {
                            this.getMultiplierMap().remove(perk);
                        }
                    }

                    for (float xpForNextLevel = perk.getTotalXpForLevel(this.chr.getPerkLevel(perk) + 1);
                        oldXP < xpForNextLevel && newXP >= xpForNextLevel;
                        xpForNextLevel = perk.getTotalXpForLevel(this.chr.getPerkLevel(perk) + 1)
                    ) {
                        IsoGameCharacter.this.LevelPerk(type);
                        if (this.chr instanceof IsoPlayer player
                            && player.isLocalPlayer()
                            && (perk != PerkFactory.Perks.Strength && perk != PerkFactory.Perks.Fitness || this.chr.getPerkLevel(perk) != 10)
                            && !this.chr.getEmitter().isPlaying("GainExperienceLevel")) {
                            this.chr.getEmitter().playSoundImpl("GainExperienceLevel", null);
                        }

                        if (this.chr.getPerkLevel(perk) >= 10) {
                            break;
                        }
                    }

                    for (float xpForThisLevel = perk.getTotalXpForLevel(this.chr.getPerkLevel(perk));
                        oldXP >= xpForThisLevel && newXP < xpForThisLevel;
                        xpForThisLevel = perk.getTotalXpForLevel(this.chr.getPerkLevel(perk))
                    ) {
                        IsoGameCharacter.this.LoseLevel(perk);
                        if (this.chr.getPerkLevel(perk) >= 10) {
                            break;
                        }
                    }

                    if (!(this.chr instanceof IsoPlayer)) {
                        haloText = false;
                    }

                    float newXPTotal = this.getXP(type);
                    if (haloText && newXPTotal > oldXP) {
                        float haloAward = newXP - oldXP;
                        haloAward = Math.round(haloAward * 10.0F) / 10.0F;
                        if (haloAward > 0.0F) {
                            HaloTextHelper.addGoodText((IsoPlayer)this.chr, Translator.getText(perk.getName()) + " XP: " + haloAward, "[br/]");
                        }
                    }

                    if (!GameClient.client) {
                        LuaEventManager.triggerEventGarbage("AddXP", this.chr, type, amount);
                    }
                }
            }
        }

        private boolean isSkillExcludedFromSpeedReduction(PerkFactory.Perk key) {
            if (key == PerkFactory.Perks.Sprinting) {
                return true;
            } else {
                return key == PerkFactory.Perks.Fitness ? true : key == PerkFactory.Perks.Strength;
            }
        }

        private boolean isSkillExcludedFromSpeedIncrease(PerkFactory.Perk key) {
            return key == PerkFactory.Perks.Fitness ? true : key == PerkFactory.Perks.Strength;
        }

        public float getXP(PerkFactory.Perk type) {
            return this.xpMap.containsKey(type) ? this.xpMap.get(type) : 0.0F;
        }

        @Deprecated
        public void AddXP(HandWeapon weapon, int amount) {
        }

        public void setTotalXP(float xp) {
            this.totalXp = xp;
        }

        private void savePerk(ByteBuffer output, PerkFactory.Perk perk) throws IOException {
            GameWindow.WriteStringUTF(output, perk == null ? "" : perk.getId());
        }

        private PerkFactory.Perk loadPerk(ByteBuffer input, int WorldVersion) throws IOException {
            String perkName = GameWindow.ReadStringUTF(input);
            PerkFactory.Perk perk = PerkFactory.Perks.FromString(perkName);
            return perk == PerkFactory.Perks.MAX ? null : perk;
        }

        public void load(ByteBuffer input, int WorldVersion) throws IOException {
            this.chr.characterTraits.load(input);
            this.totalXp = input.getFloat();
            this.level = input.getInt();
            this.lastlevel = input.getInt();
            this.xpMap.clear();
            int x = input.getInt();

            for (int n = 0; n < x; n++) {
                PerkFactory.Perk perk = this.loadPerk(input, WorldVersion);
                float xp = input.getFloat();
                if (perk != null) {
                    this.xpMap.put(perk, xp);
                }
            }

            IsoGameCharacter.this.perkList.clear();
            int nperks = input.getInt();

            for (int nx = 0; nx < nperks; nx++) {
                PerkFactory.Perk p = this.loadPerk(input, WorldVersion);
                int level = input.getInt();
                if (p != null) {
                    IsoGameCharacter.PerkInfo info = IsoGameCharacter.this.new PerkInfo();
                    info.perk = p;
                    info.level = level;
                    IsoGameCharacter.this.perkList.add(info);
                }
            }

            int x2 = input.getInt();

            for (int nxx = 0; nxx < x2; nxx++) {
                PerkFactory.Perk perks = this.loadPerk(input, WorldVersion);
                float multiplier = input.getFloat();
                int minLevel = input.get();
                int maxLevel = input.get();
                if (perks != null) {
                    this.addXpMultiplier(perks, multiplier, minLevel, maxLevel);
                }
            }

            if (this.totalXp > IsoGameCharacter.this.getXpForLevel(this.getLevel() + 1)) {
                this.setTotalXP(this.chr.getXpForLevel(this.getLevel()));
            }

            this.getGrowthRate();
        }

        public void save(ByteBuffer output) throws IOException {
            this.chr.characterTraits.save(output);
            output.putFloat(this.totalXp);
            output.putInt(this.level);
            output.putInt(this.lastlevel);
            output.putInt(this.xpMap.size());

            for (Entry<PerkFactory.Perk, Float> e : this.xpMap.entrySet()) {
                this.savePerk(output, e.getKey());
                output.putFloat(e.getValue());
            }

            output.putInt(IsoGameCharacter.this.perkList.size());

            for (int n = 0; n < IsoGameCharacter.this.perkList.size(); n++) {
                IsoGameCharacter.PerkInfo perkInfo = IsoGameCharacter.this.perkList.get(n);
                this.savePerk(output, perkInfo.perk);
                output.putInt(perkInfo.level);
            }

            output.putInt(this.xpMapMultiplier.size());

            for (Entry<PerkFactory.Perk, IsoGameCharacter.XPMultiplier> e : this.xpMapMultiplier.entrySet()) {
                this.savePerk(output, e.getKey());
                output.putFloat(e.getValue().multiplier);
                output.put((byte)e.getValue().minLevel);
                output.put((byte)e.getValue().maxLevel);
            }
        }

        public void setXPToLevel(PerkFactory.Perk key, int perkLevel) {
            PerkFactory.Perk perk = null;

            for (int n = 0; n < PerkFactory.PerkList.size(); n++) {
                PerkFactory.Perk info = PerkFactory.PerkList.get(n);
                if (info.getType() == key) {
                    perk = info;
                    break;
                }
            }

            if (perk != null) {
                this.xpMap.put(key, perk.getTotalXpForLevel(perkLevel));
            }
        }
    }

    public static class XPMultiplier {
        public float multiplier;
        public int minLevel;
        public int maxLevel;
    }

    protected static final class l_testDotSide {
        private static final Vector2 v1 = new Vector2();
        private static final Vector2 v2 = new Vector2();
        private static final Vector2 v3 = new Vector2();
    }
}
