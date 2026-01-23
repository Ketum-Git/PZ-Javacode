// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import fmod.FMOD_DriverInfo;
import fmod.javafmod;
import gnu.trove.map.hash.TIntObjectHashMap;
import imgui.ImDrawData;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Vector3f;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Controller;
import org.lwjglx.input.Keyboard;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.PixelFormat;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MovingObjectUpdateScheduler;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorFactory;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.config.ArrayConfigOption;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.MatrixStack;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.MultiTextureFBO2;
import zombie.core.textures.NinePatchTexture;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugContext;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.DevMainScreenState;
import zombie.gameStates.IngameState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.iso.BentFences;
import zombie.iso.BrokenFences;
import zombie.iso.ContainerOverlays;
import zombie.iso.IsoCamera;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.PlayerCamera;
import zombie.iso.TileOverlays;
import zombie.modding.ActiveMods;
import zombie.network.Account;
import zombie.network.CustomizationManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.AccountDBHelper;
import zombie.savefile.SavefileThumbnail;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ModRegistries;
import zombie.scripting.objects.RegistryReset;
import zombie.seams.SeamManager;
import zombie.seating.SeatingManager;
import zombie.spriteModel.SpriteModelManager;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryManager;
import zombie.ui.FPSGraph;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.ui.UITextEntryInterface;
import zombie.util.StringUtils;
import zombie.vehicles.VehicleType;
import zombie.worldMap.WorldMap;

@UsedFromLua
public final class Core {
    public static boolean IS_DEV;
    public static final float PZWorldToBulletZScale = 2.44949F;
    public static final float characterHeight = 0.6F;
    public static final boolean bDemo = false;
    public static boolean tutorial;
    public static int dirtyGlobalLightsCount;
    private static final boolean fakefullscreen = false;
    private static final GameVersion gameVersion = new GameVersion(42, 13, "");
    private static final int buildVersion = 2;
    private String gitRevisionString;
    public String steamServerVersion = "1.0.0.0";
    public static boolean altMoveMethod;
    private int consoleDotTxtSizeKb = 5120;
    private final ColorInfo objectHighlitedColor = new ColorInfo(0.98F, 0.56F, 0.11F, 1.0F);
    private final ColorInfo worldItemHighlightColor = new ColorInfo(0.5F, 1.0F, 1.0F, 1.0F);
    private final ColorInfo workstationHighlitedColor = new ColorInfo(0.56F, 0.98F, 0.11F, 1.0F);
    private final ColorInfo goodHighlitedColor = new ColorInfo(0.0F, 1.0F, 0.0F, 1.0F);
    private final ColorInfo badHighlitedColor = new ColorInfo(1.0F, 0.0F, 0.0F, 1.0F);
    private boolean flashIsoCursor;
    private final ColorInfo targetColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    private final ColorInfo noTargetColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    private Account accountUsed;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final HashMap<String, ConfigOption> optionByName = new HashMap<>();
    private final ArrayList<ConfigOption> fakeOptions = new ArrayList<>();
    private final HashMap<String, ConfigOption> fakeOptionByName = new HashMap<>();
    private String selectedMap;
    private String gitSha;
    private final IntegerConfigOption optionReticleMode = this.newOption("reticleMode", 0, 1, 0);
    private final BooleanConfigOption optionShowAimTexture = this.newOption("showAimTexture", false);
    private final BooleanConfigOption optionShowReticleTexture = this.newOption("showReticleTexture", true);
    private final BooleanConfigOption optionShowValidTargetReticleTexture = this.newOption("showValidTargetReticleTexture", true);
    private final IntegerConfigOption optionAimTextureIndex = this.newOption("aimTextureIndex", 0, 17, 0);
    private final IntegerConfigOption optionReticleTextureIndex = this.newOption("reticleTextureIndex", 0, 6, 0);
    private final IntegerConfigOption optionValidTargetReticleTextureIndex = this.newOption("validTargetReticleTextureIndex", 0, 6, 3);
    private final IntegerConfigOption optionCrosshairTextureIndex = this.newOption("crosshairTextureIndex", 0, 2, 0);
    private final IntegerConfigOption optionMaxCrosshairOffset = this.newOption("maxCrosshairOffset", 0, 11, 5);
    private final BooleanConfigOption optionReticleCameraZoom = this.newOption("reticleCameraZoom", false);
    private final ArrayConfigOption optionTargetColor = this.newOption("targetColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "1.0,1.0,1.0")
        .setFixedSize(3);
    private final ArrayConfigOption optionNoTargetColor = this.newOption("noTargetColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "1.0,1.0,1.0")
        .setFixedSize(3);
    private final ArrayConfigOption optionObjectHighlightColor = this.newOption(
            "objHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.98,0.56,0.11"
        )
        .setFixedSize(3);
    private final ArrayConfigOption optionWorldItemHighlightColor = this.newOption(
            "worldItemHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.5,1.0, 1.0"
        )
        .setFixedSize(3);
    private final ArrayConfigOption optionWorkstationHighlightColor = this.newOption(
            "workstationHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.56,0.98,0.11"
        )
        .setFixedSize(3);
    private final ArrayConfigOption optionGoodHighlightColor = this.newOption(
            "goodHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "0.0,1.0,0.0"
        )
        .setFixedSize(3);
    private final ArrayConfigOption optionBadHighlightColor = this.newOption(
            "badHighlightColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "1.0,0.0,0.0"
        )
        .setFixedSize(3);
    private final IntegerConfigOption isoCursorVisibility = this.newOption("iso_cursor", 0, 6, 5);
    private final BooleanConfigOption optionShowCursorWhileAiming = this.newOption("showCursorWhileAiming", false);
    private boolean collideZombies = true;
    public final MultiTextureFBO2 offscreenBuffer = new MultiTextureFBO2();
    private String saveFolder;
    private final BooleanConfigOption optionZoom = this.newOption("zoom", true);
    public static boolean optionModsEnabled = true;
    private final IntegerConfigOption optionFontSize = this.newOption("fontSize", 1, 6, 6);
    private final IntegerConfigOption optionMoodleSize = this.newOption("moodleSize", 1, 7, 7);
    private final IntegerConfigOption optionSidebarSize = this.newOption("sidebarSize", 1, 6, 6);
    private final IntegerConfigOption optionActionProgressBarSize = this.newOption("actionProgressBarSize", 1, 4, 1);
    private final StringConfigOption optionContextMenuFont = this.newOption("contextMenuFont", "Medium", new String[]{"Small", "Medium", "Large"});
    private final StringConfigOption optionCodeFontSize = this.newOption("codeFontSize", "Medium", new String[]{"Small", "Medium", "Large"});
    private final StringConfigOption optionInventoryFont = this.newOption("inventoryFont", "Medium", new String[]{"Small", "Medium", "Large"});
    private final IntegerConfigOption optionInventoryContainerSize = this.newOption("inventoryContainerSize", 1, 3, 1);
    private final StringConfigOption optionTooltipFont = this.newOption("tooltipFont", "Small", new String[]{"Small", "Medium", "Large"});
    private final BooleanConfigOption optionColorblindPatterns = this.newOption("colorblindPatterns", false);
    private final BooleanConfigOption optionEnableDyslexicFont = this.newOption("enableDyslexicFont", false);
    private final BooleanConfigOption optionLightSensitivity = this.newOption("enableLightSensitivity", false);
    private final StringConfigOption optionMeasurementFormat = this.newOption("measurementsFormat", "Metric", new String[]{"Imperial", "Metric"});
    private final IntegerConfigOption optionClockFormat = this.newOption("clockFormat", 1, 2, 1);
    private final IntegerConfigOption optionClockSize = this.newOption("clockSize", 1, 2, 2);
    private final BooleanConfigOption optionClock24Hour = this.newOption("clock24Hour", true);
    private final BooleanConfigOption optionVsync = this.newOption("vsync", false);
    private final IntegerConfigOption optionSoundVolume = this.newOption("soundVolume", 0, 10, 10);
    private final IntegerConfigOption optionMusicVolume = this.newOption("musicVolume", 0, 10, 10);
    private final IntegerConfigOption optionAmbientVolume = this.newOption("ambientVolume", 0, 10, 5);
    private final IntegerConfigOption optionJumpScareVolume = this.newOption("jumpScareVolume", 0, 10, 10);
    private final IntegerConfigOption optionMusicActionStyle = this.newOption("musicActionStyle", 1, 2, 1);
    private final IntegerConfigOption optionMusicLibrary = this.newOption("musicLibrary", 1, 3, 1);
    private final BooleanConfigOption optionVoiceEnable = this.newOption("voiceEnable", true);
    private final IntegerConfigOption optionVoiceMode = this.newOption("voiceMode", 1, 3, 3);
    private final IntegerConfigOption optionVoiceVadMode = this.newOption("voiceVADMode", 1, 4, 3);
    private final IntegerConfigOption optionVoiceAgcMode = this.newOption("voiceAGCMode", 1, 3, 2);
    private final StringConfigOption optionVoiceRecordDeviceName = this.newOption("voiceRecordDeviceName", "", 256);
    private final IntegerConfigOption optionVoiceVolumeMic = this.newOption("voiceVolumeMic", 0, 10, 10);
    private final IntegerConfigOption optionVoiceVolumePlayers = this.newOption("voiceVolumePlayers", 0, 10, 5);
    private final IntegerConfigOption optionVehicleEngineVolume = this.newOption("vehicleEngineVolume", 0, 10, 10);
    private final BooleanConfigOption optionStreamerMode = this.newOption("vehicleStreamerMode", false);
    private final IntegerConfigOption optionReloadDifficulty = this.newOption("reloadDifficulty", 1, 3, 2);
    private final BooleanConfigOption optionRackProgress = this.newOption("rackProgress", true);
    private final IntegerConfigOption optionBloodDecals = this.newOption("bloodDecals", 0, 10, 10);
    private final BooleanConfigOption optionFocusloss = this.newOption("focusloss", false);
    private final BooleanConfigOption optionBorderlessWindow = this.newOption("borderless", false);
    private final BooleanConfigOption optionLockCursorToWindow = this.newOption("lockCursorToWindow", false);
    private final BooleanConfigOption optionTextureCompression = this.newOption("textureCompression", true);
    private final BooleanConfigOption optionModelTextureMipmaps = this.newOption("modelTextureMipmaps", false);
    private final BooleanConfigOption optionTexture2x = this.newOption("texture2x", true);
    private final BooleanConfigOption optionHighResPlacedItems = this.newOption("highResPlacedItems", true);
    private final IntegerConfigOption optionMaxTextureSize = this.newOption("maxTextureSize", 1, 4, 1);
    private final IntegerConfigOption optionMaxVehicleTextureSize = this.newOption("maxVehicleTextureSize", 1, 4, 2);
    private final StringConfigOption optionScreenFilter = this.newOption("screenFilter", "nearest", new String[]{"nearest", "linear"});
    private final ArrayConfigOption optionZoomLevels1x = this.newOption("zoomLevels1x", new IntegerConfigOption("element", 25, 250, 100), ";", "");
    private final ArrayConfigOption optionZoomLevels2x = this.newOption("zoomLevels2x", new IntegerConfigOption("element", 25, 250, 100), ";", "");
    private final BooleanConfigOption optionEnableContentTranslations = this.newOption("contentTranslationsEnabled", true);
    private final BooleanConfigOption optionUiFbo = this.newOption("uiRenderOffscreen", true);
    private final IntegerConfigOption optionUiRenderFps = this.newOption("uiRenderFPS", 10, 120, 60);
    private final BooleanConfigOption optionRadialMenuKeyToggle = this.newOption("radialMenuKeyToggle", true);
    private final BooleanConfigOption optionReloadRadialInstant = this.newOption("reloadRadialInstant", false);
    private final BooleanConfigOption optionPanCameraWhileAiming = this.newOption("panCameraWhileAiming", true);
    private final BooleanConfigOption optionPanCameraWhileDriving = this.newOption("panCameraWhileDriving", false);
    private final BooleanConfigOption optionShowChatTimestamp = this.newOption("showChatTimestamp", false);
    private final BooleanConfigOption optionShowChatTitle = this.newOption("showChatTitle", false);
    private final StringConfigOption optionChatFontSize = this.newOption("chatFontSize", "medium", new String[]{"small", "medium", "large"});
    private final DoubleConfigOption optionMinChatOpaque = this.newOption("minChatOpaque", 0.0, 1.0, 1.0);
    private final DoubleConfigOption optionMaxChatOpaque = this.newOption("maxChatOpaque", 0.0, 1.0, 1.0);
    private final DoubleConfigOption optionChatFadeTime = this.newOption("chatFadeTime", 0.0, 10.0, 0.0);
    private final BooleanConfigOption optionChatOpaqueOnFocus = this.newOption("chatOpaqueOnFocus", true);
    private final BooleanConfigOption optionTemperatureDisplayCelsius = this.newOption("temperatureDisplayCelsius", false);
    private final BooleanConfigOption optionDoVideoEffects = this.newOption("doVideoEffects", true);
    private final BooleanConfigOption optionDoWindSpriteEffects = this.newOption("doWindSpriteEffects", true);
    private final BooleanConfigOption optionDoDoorSpriteEffects = this.newOption("doDoorSpriteEffects", true);
    private final BooleanConfigOption optionDoContainerOutline = this.newOption("doContainerOutline", true);
    private final BooleanConfigOption optionRenderPrecipIndoors = this.newOption("renderPrecipIndoors", true);
    private final DoubleConfigOption optionPrecipitationSpeedMultiplier = this.newOption("precipitationSpeedMultiplier", 0.01, 1.0, 1.0);
    private final BooleanConfigOption optionAutoProneAtk = this.newOption("autoProneAtk", true);
    private final BooleanConfigOption option3dGroundItem = this.newOption("3DGroundItem", true);
    private final IntegerConfigOption optionRenderPrecipitation = this.newOption("renderPrecipitation", 1, 3, 1);
    private final BooleanConfigOption optionDblTapJogToSprint = this.newOption("dblTapJogToSprint", false);
    private final BooleanConfigOption optionMeleeOutline = this.newOption("meleeOutline", false);
    private final StringConfigOption optionCycleContainerKey = this.newOption(
        "cycleContainerKey", "shift", new String[]{"control", "shift", "control+shift", "command", "command+shift"}
    );
    private final BooleanConfigOption optionDropItemsOnSquareCenter = this.newOption("dropItemsOnSquareCenter", false);
    private final BooleanConfigOption optionTimedActionGameSpeedReset = this.newOption("timedActionGameSpeedReset", false);
    private final IntegerConfigOption optionShoulderButtonContainerSwitch = this.newOption("shoulderButtonContainerSwitch", 1, 3, 1);
    private final IntegerConfigOption optionControllerButtonStyle = this.newOption("controllerButtonStyle", 1, 2, 1);
    private final BooleanConfigOption optionProgressBar = this.newOption("progressBar", false);
    private final StringConfigOption optionLanguageName = this.newOption("language", "", 64);
    private final ArrayConfigOption optionSingleContextMenu = this.newOption("singleContextMenu", new BooleanConfigOption("element", false), ",", "")
        .setFixedSize(4);
    private final BooleanConfigOption optionCorpseShadows = this.newOption("corpseShadows", true);
    private final IntegerConfigOption optionSimpleClothingTextures = this.newOption("simpleClothingTextures", 1, 3, 1);
    private final BooleanConfigOption optionSimpleWeaponTextures = this.newOption("simpleWeaponTextures", false);
    private final BooleanConfigOption optionAutoDrink = this.newOption("autoDrink", true);
    private final BooleanConfigOption optionAutoRevealPrintMediaMapLocations = this.newOption("autoRevealPrintMediaMapLocations", false);
    private final BooleanConfigOption optionLeaveKeyInIgnition = this.newOption("leaveKeyInIgnition", false);
    private final BooleanConfigOption optionAutoWalkContainer = this.newOption("autoWalkContainer", false);
    private final IntegerConfigOption optionSearchModeOverlayEffect = this.newOption("searchModeOverlayEffect", 1, 4, 1);
    private final IntegerConfigOption optionIgnoreProneZombieRange = this.newOption("ignoreProneZombieRange", 1, 5, 2);
    private final BooleanConfigOption optionShowItemModInfo = this.newOption("showItemModInfo", true);
    private final BooleanConfigOption optionShowCraftingXp = this.newOption("showCraftingXP", true);
    private final BooleanConfigOption optionShowSurvivalGuide = this.newOption("showSurvivalGuide", true);
    private final BooleanConfigOption optionShowFirstAnimalZoneInfo = this.newOption("showFirstAnimalZoneInfo", true);
    private final BooleanConfigOption optionEnableLeftJoystickRadialMenu = this.newOption("enableLeftJoystickRadialMenu", true);
    private final BooleanConfigOption optionMacosIgnoreMouseWheelAcceleration = this.newOption("macosIgnoreMouseWheelAcceleration", true);
    private final BooleanConfigOption optionMacosMapHorizontalMouseWheelToVertical = this.newOption("macosMapHorizontalMouseWheelToVertical", true);
    private final BooleanConfigOption optionUsePhysicsHitReaction = this.newOption("usePhysicsHitReaction", true);
    private final IntegerConfigOption optionMaxActiveRagdolls = this.newOption("maxActiveRagdolls", 5, 60, 20);
    private final DoubleConfigOption optionWorldMapBrightness = this.newOption("worldMapBrightness", 0.0, 1.0, 1.0);
    private final BooleanConfigOption optionShowWelcomeMessage = this.newOption("showWelcomeMessage2", true);
    private boolean showPing = true;
    private boolean forceSnow;
    private boolean zombieGroupSound = true;
    private String blinkingMoodle;
    private String poisonousBerry;
    private String poisonousMushroom;
    private static String difficulty = "Hardcore";
    public static int tileScale = 2;
    private boolean isSelectingAll;
    private final BooleanConfigOption showYourUsername = this.newOption("showYourUsername", true);
    private ColorInfo mpTextColor;
    private final ArrayConfigOption optionMpTextColor = this.newOption("mpTextColor", new DoubleConfigOption("element", 0.0, 1.0, 1.0), ",", "")
        .setFixedSize(3);
    private boolean isAzerty;
    private final StringConfigOption seenUpdateText = this.newOption("seenNews", "", 64);
    private final BooleanConfigOption toggleToAim = this.newOption("toggleToAim", false);
    private final BooleanConfigOption toggleToRun = this.newOption("toggleToRun", false);
    private final BooleanConfigOption toggleToSprint = this.newOption("toggleToSprint", true);
    private final BooleanConfigOption celsius = this.newOption("celsius", false);
    private boolean noSave;
    private boolean showFirstTimeVehicleTutorial;
    private boolean showFirstTimeWeatherTutorial;
    private boolean animPopupDone;
    private boolean modsPopupDone;
    public static float blinkAlpha = 1.0F;
    public static boolean blinkAlphaIncrease;
    private boolean loadedOptions;
    private static final HashMap<String, Object> optionsOnStartup = new HashMap<>();
    public boolean animalCheat;
    public boolean displayPlayerModel = true;
    public boolean displayCursor = true;
    public final MatrixStack projectionMatrixStack = new MatrixStack(5889);
    public final MatrixStack modelViewMatrixStack = new MatrixStack(5888);
    private int screenFilter = 9728;
    public static final Vector3f UnitVector3f = new Vector3f(1.0F, 1.0F, 1.0F);
    public static final Vector3f _UNIT_Z = new Vector3f(0.0F, 0.0F, 1.0F);
    private boolean challenge;
    public static int width = 1280;
    public static int height = 720;
    public static float initialWidth = 1280.0F;
    public static float initialHeight = 720.0F;
    public static int maxJukeBoxesActive = 10;
    public static int numJukeBoxesActive;
    public static String gameMode = "Sandbox";
    public static boolean addZombieOnCellLoad = true;
    public static String preset = "Apocalypse";
    private static String glVersion;
    private static int glMajorVersion = -1;
    private static final Core core = new Core();
    public static boolean debug;
    public static boolean useViewports;
    public static boolean useGameViewport = true;
    public static boolean imGui;
    public static UITextEntryInterface currentTextEntryBox;
    public static final Core.KeyBinding KEYBINDING_EMPTY = new Core.KeyBinding("empty", 0, 0, false, false, false);
    private Map<String, Core.KeyBinding> keyMaps = new HashMap<>();
    private final TIntObjectHashMap<Core.KeyBindingList> keyBindingByKeyValue = new TIntObjectHashMap<>();
    public final boolean useShaders = true;
    private int iPerfSkybox = 1;
    private final IntegerConfigOption perfSkyboxNew = this.newOption("perfSkybox", 0, 2, 1);
    public static final int iPerfSkybox_High = 0;
    public static final int iPerfSkybox_Medium = 1;
    public static final int iPerfSkybox_Static = 2;
    private int iPerfPuddles = 0;
    private final IntegerConfigOption perfPuddlesNew = this.newOption("perfPuddles", 0, 3, 0);
    public static final int iPerfPuddles_None = 3;
    public static final int iPerfPuddles_GroundOnly = 2;
    public static final int iPerfPuddles_GroundWithRuts = 1;
    public static final int iPerfPuddles_All = 0;
    private boolean perfReflections = true;
    private final BooleanConfigOption perfReflectionsNew = this.newOption("bPerfReflections", true);
    public int vidMem = 3;
    private boolean supportsFbo = true;
    public float uiRenderAccumulator;
    public boolean uiRenderThisFrame = true;
    public int version = 1;
    public int fileversion = 7;
    private final ArrayConfigOption optionActiveControllerGuids = this.newFakeOption("controller", new StringConfigOption("element", "", 256), ",", "")
        .setMultiLine(true);
    private final BooleanConfigOption optionDoneNewSaveFolder = this.newFakeOption("doneNewSaveFolder", false);
    private final IntegerConfigOption optionFogQuality = this.newFakeOption("fogQuality", 0, 2, 0);
    private final IntegerConfigOption optionViewConeOpacity = this.newFakeOption("viewConeOpacity", 0, 5, 3);
    private final BooleanConfigOption optionGotNewBelt = this.newFakeOption("gotNewBelt", false);
    private final IntegerConfigOption optionLightingFps = this.newFakeOption("lightFPS", 5, 60, 15);
    private final IntegerConfigOption optionLockFps = this.newFakeOption("frameRate", 24, 244, 60);
    private final IntegerConfigOption optionPuddlesQuality = this.newFakeOption("puddles", 0, 2, 0);
    private final BooleanConfigOption optionRiversideDone = this.newFakeOption("riversideDone", false);
    private final BooleanConfigOption optionRosewoodSpawnDone = this.newFakeOption("rosewoodSpawnDone", false);
    private final IntegerConfigOption optionScreenHeight = this.newFakeOption("height", 0, 16384, 720);
    private final IntegerConfigOption optionScreenWidth = this.newFakeOption("width", 0, 16384, 1280);
    private final BooleanConfigOption optionShowFirstTimeSearchTutorial = this.newFakeOption("showFirstTimeSearchTutorial", true);
    private final BooleanConfigOption optionShowFirstTimeSneakTutorial = this.newFakeOption("showFirstTimeSneakTutorial", true);
    private final DoubleConfigOption optionShownWelcomeMessageVersion = this.newFakeOption("shownWelcomeMessageVersion", 0.0, 1000.0, 0.0);
    private final IntegerConfigOption optionTermsOfServiceVersion = this.newFakeOption("termsOfServiceVersion", -1, 1000, -1);
    private final BooleanConfigOption optionTieredZombieUpdates = this.newFakeOption("tieredZombieUpdates", true);
    private final BooleanConfigOption optionTutorialDone = this.newFakeOption("tutorialDone", false);
    private final BooleanConfigOption optionUpdateSneakButton = this.newFakeOption("updateSneakButton", true);
    private final BooleanConfigOption optionUncappedFps = this.newFakeOption("uncappedFPS", true);
    private final BooleanConfigOption optionVehiclesWarningShow = this.newFakeOption("vehiclesWarningShow", false);
    private final IntegerConfigOption optionWaterQuality = this.newFakeOption("water", 0, 2, 0);
    private final BooleanConfigOption fullScreen = this.newOption("fullScreen", false);
    private final ArrayConfigOption autoZoom = this.newOption("autozoom", new BooleanConfigOption("element", false), ",", "").setFixedSize(4);
    public static String gameMap = "DEFAULT";
    public static String gameSaveWorld = "";
    public static boolean safeMode;
    public static boolean safeModeForced;
    public static boolean soundDisabled;
    public int frameStage;
    private int stack;
    public static int xx;
    public static int yy;
    public static int zz;
    public final HashMap<Integer, Float> floatParamMap = new HashMap<>();
    private final Matrix4f tempMatrix4f = new Matrix4f();
    private static final float isoAngle = 62.65607F;
    public static final float ModelScale = 1.5F;
    public static final float scale = (float)(Math.sqrt(2.0) / 2.0 / 10.0) / 1.5F;
    public static boolean lastStand;
    public static String challengeId;
    public static boolean exiting;
    private String delayResetLuaActiveMods;
    private String delayResetLuaReason;
    private final String rn = "\r\n";

    private ArrayConfigOption newOption(String name, ConfigOption elementHandler, String separator, String defaultValue) {
        ArrayConfigOption option = new ArrayConfigOption(name, elementHandler, separator, defaultValue);
        this.options.add(option);
        this.optionByName.put(name, option);
        return option;
    }

    private BooleanConfigOption newOption(String name, boolean defaultValue) {
        BooleanConfigOption option = new BooleanConfigOption(name, defaultValue);
        this.options.add(option);
        this.optionByName.put(name, option);
        return option;
    }

    private DoubleConfigOption newOption(String name, double minValue, double maxValue, double defaultValue) {
        DoubleConfigOption option = new DoubleConfigOption(name, minValue, maxValue, defaultValue);
        this.options.add(option);
        this.optionByName.put(name, option);
        return option;
    }

    private IntegerConfigOption newOption(String name, int minValue, int maxValue, int defaultValue) {
        IntegerConfigOption option = new IntegerConfigOption(name, minValue, maxValue, defaultValue);
        this.options.add(option);
        this.optionByName.put(name, option);
        return option;
    }

    private StringConfigOption newOption(String name, String defaultValue, String[] values) {
        StringConfigOption option = new StringConfigOption(name, defaultValue, values);
        this.options.add(option);
        this.optionByName.put(name, option);
        return option;
    }

    private StringConfigOption newOption(String name, String defaultValue, int maxLength) {
        StringConfigOption option = new StringConfigOption(name, defaultValue, maxLength);
        this.options.add(option);
        this.optionByName.put(name, option);
        return option;
    }

    private ArrayConfigOption newFakeOption(String name, ConfigOption elementHandler, String separator, String defaultValue) {
        ArrayConfigOption option = new ArrayConfigOption(name, elementHandler, separator, defaultValue);
        this.fakeOptions.add(option);
        this.fakeOptionByName.put(name, option);
        return option;
    }

    private BooleanConfigOption newFakeOption(String name, boolean defaultValue) {
        BooleanConfigOption option = new BooleanConfigOption(name, defaultValue);
        this.fakeOptions.add(option);
        this.fakeOptionByName.put(name, option);
        return option;
    }

    private DoubleConfigOption newFakeOption(String name, double minValue, double maxValue, double defaultValue) {
        DoubleConfigOption option = new DoubleConfigOption(name, minValue, maxValue, defaultValue);
        this.fakeOptions.add(option);
        this.fakeOptionByName.put(name, option);
        return option;
    }

    private IntegerConfigOption newFakeOption(String name, int minValue, int maxValue, int defaultValue) {
        IntegerConfigOption option = new IntegerConfigOption(name, minValue, maxValue, defaultValue);
        this.fakeOptions.add(option);
        this.fakeOptionByName.put(name, option);
        return option;
    }

    private StringConfigOption newFakeOption(String name, String defaultValue, String[] values) {
        StringConfigOption option = new StringConfigOption(name, defaultValue, values);
        this.fakeOptions.add(option);
        this.fakeOptionByName.put(name, option);
        return option;
    }

    private StringConfigOption newFakeOption(String name, String defaultValue, int maxLength) {
        StringConfigOption option = new StringConfigOption(name, defaultValue, maxLength);
        this.fakeOptions.add(option);
        this.fakeOptionByName.put(name, option);
        return option;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public boolean isMultiThread() {
        return true;
    }

    public void setChallenge(boolean bChallenge) {
        this.challenge = bChallenge;
    }

    public boolean isChallenge() {
        return this.challenge;
    }

    public String getChallengeID() {
        return challengeId;
    }

    public boolean getOptionTieredZombieUpdates() {
        return this.optionTieredZombieUpdates.getValue();
    }

    public void setOptionTieredZombieUpdates(boolean val) {
        this.optionTieredZombieUpdates.setValue(val);
        MovingObjectUpdateScheduler.instance.setEnabled(val);
    }

    public void setFramerate(int index) {
        PerformanceSettings.instance.setFramerateUncapped(index == 1);
        switch (index) {
            case 1:
                PerformanceSettings.setLockFPS(60);
                break;
            case 2:
                PerformanceSettings.setLockFPS(244);
                break;
            case 3:
                PerformanceSettings.setLockFPS(240);
                break;
            case 4:
                PerformanceSettings.setLockFPS(165);
                break;
            case 5:
                PerformanceSettings.setLockFPS(144);
                break;
            case 6:
                PerformanceSettings.setLockFPS(120);
                break;
            case 7:
                PerformanceSettings.setLockFPS(95);
                break;
            case 8:
                PerformanceSettings.setLockFPS(90);
                break;
            case 9:
                PerformanceSettings.setLockFPS(75);
                break;
            case 10:
                PerformanceSettings.setLockFPS(60);
                break;
            case 11:
                PerformanceSettings.setLockFPS(55);
                break;
            case 12:
                PerformanceSettings.setLockFPS(45);
                break;
            case 13:
                PerformanceSettings.setLockFPS(30);
                break;
            case 14:
                PerformanceSettings.setLockFPS(24);
        }
    }

    public void setMultiThread(boolean val) {
    }

    public static boolean isUseGameViewport() {
        return useGameViewport && debug && imGui;
    }

    public static boolean isImGui() {
        return debug && imGui;
    }

    public static boolean isUseViewports() {
        return debug && imGui && useViewports;
    }

    public boolean loadedShader() {
        return SceneShaderStore.weatherShader != null;
    }

    public static int getGLMajorVersion() {
        if (glMajorVersion == -1) {
            getOpenGLVersions();
        }

        return glMajorVersion;
    }

    public boolean getUseShaders() {
        return true;
    }

    public int getPerfSkybox() {
        return this.perfSkyboxNew.getValue();
    }

    public int getPerfSkyboxOnLoad() {
        return this.iPerfSkybox;
    }

    public void setPerfSkybox(int val) {
        this.perfSkyboxNew.setValue(val);
    }

    public boolean getPerfReflections() {
        return this.perfReflectionsNew.getValue();
    }

    public boolean getPerfReflectionsOnLoad() {
        return this.perfReflections;
    }

    public boolean getUseOpenGL21() {
        return !Display.capabilities.OpenGL33;
    }

    public void setPerfReflections(boolean val) {
        this.perfReflectionsNew.setValue(val);
    }

    public int getPerfPuddles() {
        return this.perfPuddlesNew.getValue();
    }

    public int getPerfPuddlesOnLoad() {
        return this.iPerfPuddles;
    }

    public void setPerfPuddles(int val) {
        this.perfPuddlesNew.setValue(val);
    }

    public int getVidMem() {
        return safeMode ? 5 : this.vidMem;
    }

    public void setVidMem(int mem) {
        if (safeMode) {
            this.vidMem = 5;
        }

        this.vidMem = mem;

        try {
            this.saveOptions();
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }

    public void setUseShaders(boolean bUse) {
    }

    public void shadersOptionChanged() {
        RenderThread.invokeOnRenderContext(() -> {
            if (!safeModeForced) {
                SceneShaderStore.shaderOptionsChanged();
            }
        });
    }

    public void initGlobalShader() {
        SceneShaderStore.initGlobalShader();
    }

    public void initShaders() {
        SceneShaderStore.initShaders();
        IsoPuddles.getInstance();
        IsoWater.getInstance();
    }

    public static String getGLVersion() {
        if (glVersion == null) {
            getOpenGLVersions();
        }

        return glVersion;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        Core.gameMode = gameMode;
        addZombieOnCellLoad = !Core.gameMode.equals("Tutorial") && !Core.gameMode.equals("LastStand");
    }

    public static Core getInstance() {
        return core;
    }

    public static void getOpenGLVersions() {
        glVersion = GL11.glGetString(7938);
        glMajorVersion = glVersion.charAt(0) - '0';
    }

    public boolean getDebug() {
        return debug;
    }

    public static void setFullScreen(boolean bool) {
        getInstance().fullScreen.setValue(bool);
    }

    public static int[] flipPixels(int[] imgPixels, int imgw, int imgh) {
        int[] flippedPixels = null;
        if (imgPixels != null) {
            flippedPixels = new int[imgw * imgh];

            for (int y = 0; y < imgh; y++) {
                for (int x = 0; x < imgw; x++) {
                    flippedPixels[(imgh - y - 1) * imgw + x] = imgPixels[y * imgw + x];
                }
            }
        }

        return flippedPixels;
    }

    public void TakeScreenshot() {
        this.TakeScreenshot(256, 256, 1028);
    }

    public void TakeScreenshot(int width, int height, int readBuffer) {
        int playerIndex = 0;
        int screenWidth = IsoCamera.getScreenWidth(0);
        int screenHeight = IsoCamera.getScreenHeight(0);
        width = PZMath.min(width, screenWidth);
        height = PZMath.min(height, screenHeight);
        int x = IsoCamera.getScreenLeft(0) + screenWidth / 2 - width / 2;
        int y = IsoCamera.getScreenTop(0) + screenHeight / 2 - height / 2;
        this.TakeScreenshot(x, y, width, height, readBuffer);
    }

    public void TakeScreenshot(int x, int y, int width, int height, int readBuffer) {
        GL11.glPixelStorei(3333, 1);
        GL11.glReadBuffer(readBuffer);
        int bpp = 3;
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 3);
        GL11.glReadPixels(x, y, width, height, 6407, 5121, buffer);
        int[] pixels = new int[width * height];
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("thumb.png");
        String format = "png";

        for (int i = 0; i < pixels.length; i++) {
            int bindex = i * 3;
            pixels[i] = 0xFF000000 | (buffer.get(bindex) & 255) << 16 | (buffer.get(bindex + 1) & 255) << 8 | (buffer.get(bindex + 2) & 255) << 0;
        }

        MemoryUtil.memFree(buffer);
        pixels = flipPixels(pixels, width, height);
        BufferedImage image = new BufferedImage(width, height, 2);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        if (SavefileThumbnail.serverAddress != null) {
            ByteBuffer buffer1 = CustomizationManager.compressToByteBuffer(image, "png");
            AccountDBHelper.getInstance()
                .updateAccountIconAndData(SavefileThumbnail.serverAddress, SavefileThumbnail.serverPort, SavefileThumbnail.accountUsername, buffer1);
            buffer1.clear();
            SavefileThumbnail.serverAddress = null;
        }

        try {
            ImageIO.write(image, "png", file);
        } catch (IOException var14) {
            var14.printStackTrace();
        }

        Texture.reload(ZomboidFileSystem.instance.getFileNameInCurrentSave("thumb.png"));
    }

    public void TakeFullScreenshot(String filename) {
        RenderThread.invokeOnRenderContext(filename, l_filename -> {
            GL11.glPixelStorei(3333, 1);
            GL11.glReadBuffer(1028);
            int width = Display.getDisplayMode().getWidth();
            int height = Display.getDisplayMode().getHeight();
            int x = 0;
            int y = 0;
            int bpp = 3;
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 3);
            GL11.glReadPixels(0, 0, width, height, 6407, 5121, buffer);
            int[] pixels = new int[width * height];
            if (l_filename == null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
                l_filename = "screenshot_" + sdf.format(Calendar.getInstance().getTime()) + ".png";
            }

            File file = new File(ZomboidFileSystem.instance.getScreenshotDir() + File.separator + l_filename);

            for (int i = 0; i < pixels.length; i++) {
                int bindex = i * 3;
                pixels[i] = 0xFF000000 | (buffer.get(bindex) & 255) << 16 | (buffer.get(bindex + 1) & 255) << 8 | (buffer.get(bindex + 2) & 255) << 0;
            }

            MemoryUtil.memFree(buffer);
            pixels = flipPixels(pixels, width, height);
            BufferedImage image = new BufferedImage(width, height, 2);
            image.setRGB(0, 0, width, height, pixels, 0, width);

            try {
                ImageIO.write(image, "png", file);
            } catch (IOException var12) {
                var12.printStackTrace();
            }
        });
    }

    public static boolean supportNPTTexture() {
        return false;
    }

    public boolean supportsFBO() {
        if (safeMode) {
            this.offscreenBuffer.zoomEnabled = false;
            return false;
        } else if (!this.supportsFbo) {
            return false;
        } else if (this.offscreenBuffer.current != null) {
            return true;
        } else {
            try {
                if (TextureFBO.checkFBOSupport() && this.setupMultiFBO()) {
                    return true;
                } else {
                    this.supportsFbo = false;
                    safeMode = true;
                    this.offscreenBuffer.zoomEnabled = false;
                    return false;
                }
            } catch (Exception var2) {
                var2.printStackTrace();
                this.supportsFbo = false;
                safeMode = true;
                this.offscreenBuffer.zoomEnabled = false;
                return false;
            }
        }
    }

    private void sharedInit() {
        this.supportsFBO();
    }

    public void MoveMethodToggle() {
        altMoveMethod = !altMoveMethod;
    }

    public void EndFrameText(int nPlayer) {
        if (!LuaManager.thread.step) {
            if (this.offscreenBuffer.current != null) {
            }

            IndieGL.glDoEndFrame();
            this.frameStage = 2;
        }
    }

    public void EndFrame(int nPlayer) {
        if (!LuaManager.thread.step) {
            if (this.offscreenBuffer.current != null) {
                SpriteRenderer.instance.glBuffer(0, nPlayer);
            }

            IndieGL.glDoEndFrame();
            this.frameStage = 2;
        }
    }

    public void EndFrame() {
        IndieGL.glDoEndFrame();
        if (this.offscreenBuffer.current != null) {
            SpriteRenderer.instance.glBuffer(0, 0);
        }
    }

    public void EndFrameUI() {
        if (!blinkAlphaIncrease) {
            blinkAlpha = blinkAlpha - 0.07F * GameTime.getInstance().getThirtyFPSMultiplier();
            if (blinkAlpha < 0.15F) {
                blinkAlpha = 0.15F;
                blinkAlphaIncrease = true;
            }
        } else {
            blinkAlpha = blinkAlpha + 0.07F * GameTime.getInstance().getThirtyFPSMultiplier();
            if (blinkAlpha > 1.0F) {
                blinkAlpha = 1.0F;
                blinkAlphaIncrease = false;
            }
        }

        if (UIManager.useUiFbo && UIManager.uiFbo == null) {
            UIManager.CreateFBO(width, height);
        }

        if (LuaManager.thread != null && LuaManager.thread.step) {
            SpriteRenderer.instance.clearSprites();
        } else {
            ExceptionLogger.render();
            if (UIManager.useUiFbo) {
                if (this.uiRenderThisFrame) {
                    UIManager.uiTextureContentsValid = true;
                    SpriteRenderer.instance.glBuffer(3, 0);
                    IndieGL.glDoEndFrame();
                    SpriteRenderer.instance.stopOffscreenUI();
                    IndieGL.glDoStartFrame(width, height, 1.0F, -1);
                    this.uiRenderAccumulator = this.uiRenderAccumulator % (30.0F / this.getOptionUIRenderFPS());
                    if (FPSGraph.instance != null) {
                        FPSGraph.instance.addUI(System.currentTimeMillis());
                    }
                }
            } else {
                UIManager.uiTextureContentsValid = false;
            }

            if (UIManager.useUiFbo && UIManager.uiTextureContentsValid) {
                SpriteRenderer.instance.setDoAdditive(true);
                SpriteRenderer.instance.renderi((Texture)UIManager.uiFbo.getTexture(), 0, height, width, -height, 1.0F, 1.0F, 1.0F, 1.0F, null);
                SpriteRenderer.instance.setDoAdditive(false);
            } else if (UIManager.useUiFbo) {
                UIManager.renderFadeOverlay();
            }

            if (getInstance().getOptionLockCursorToWindow()) {
                Mouse.renderCursorTexture();
            }

            ImDrawData drawData = Display.imguiEndFrame();
            if (debug && drawData != null) {
                SpriteRenderer.instance.render(drawData);
            }

            IndieGL.glDoEndFrame();
            RenderThread.Ready();
            this.frameStage = 0;
        }
    }

    public static void UnfocusActiveTextEntryBox() {
        if (currentTextEntryBox != null && !currentTextEntryBox.getUIName().contains("chat text entry")) {
            currentTextEntryBox.setDoingTextEntry(false);
            if (currentTextEntryBox.getFrame() != null) {
                currentTextEntryBox.getFrame().color = currentTextEntryBox.getStandardFrameColour();
            }

            currentTextEntryBox = null;
        }
    }

    public int getOffscreenWidth(int playerIndex) {
        if (this.offscreenBuffer == null) {
            return IsoPlayer.numPlayers > 1 ? this.getScreenWidth() / 2 : this.getScreenWidth();
        } else {
            return this.offscreenBuffer.getWidth(playerIndex);
        }
    }

    public int getOffscreenHeight(int playerIndex) {
        if (this.offscreenBuffer == null) {
            return IsoPlayer.numPlayers > 2 ? this.getScreenHeight() / 2 : this.getScreenHeight();
        } else {
            return this.offscreenBuffer.getHeight(playerIndex);
        }
    }

    public int getOffscreenTrueWidth() {
        return this.offscreenBuffer != null && this.offscreenBuffer.current != null ? this.offscreenBuffer.getTexture(0).getWidth() : this.getScreenWidth();
    }

    public int getOffscreenTrueHeight() {
        return this.offscreenBuffer != null && this.offscreenBuffer.current != null ? this.offscreenBuffer.getTexture(0).getHeight() : this.getScreenHeight();
    }

    public int getScreenHeight() {
        return height;
    }

    public int getScreenWidth() {
        return width;
    }

    public void setResolutionAndFullScreen(int w, int h, boolean fullScreen) {
        if (!isUseGameViewport()) {
            setDisplayMode(w, h, fullScreen);
            this.setScreenSize(Display.getWidth(), Display.getHeight());
        } else {
            this.setScreenSize(w, h);
        }
    }

    public void setResolution(String res) {
        String[] bits = res.split("x");
        int w = Integer.parseInt(bits[0].trim());
        int h = Integer.parseInt(bits[1].trim());
        if (!isUseGameViewport()) {
            if (this.fullScreen.getValue()) {
                setDisplayMode(w, h, true);
            } else {
                setDisplayMode(w, h, false);
            }

            this.setScreenSize(Display.getWidth(), Display.getHeight());
        } else {
            this.setScreenSize(w, h);
        }

        try {
            this.saveOptions();
        } catch (IOException var6) {
            var6.printStackTrace();
        }
    }

    public boolean loadOptions_OLD() throws IOException {
        this.loadedOptions = false;
        File newFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "options.ini");
        if (!newFile.exists()) {
            this.initOptionsINI();
            return false;
        } else {
            this.loadedOptions = true;

            for (int n = 0; n < 4; n++) {
                this.setAutoZoom(n, false);
            }

            this.optionLanguageName.setValue("");
            BufferedReader br = new BufferedReader(new FileReader(newFile));

            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("version=")) {
                        this.version = Integer.parseInt(line.replaceFirst("version=", ""));
                    } else if (line.startsWith("width=")) {
                        width = Integer.parseInt(line.replaceFirst("width=", ""));
                    } else if (line.startsWith("height=")) {
                        height = Integer.parseInt(line.replaceFirst("height=", ""));
                    } else if (line.startsWith("fullScreen=")) {
                        this.fullScreen.parse(line.replaceFirst("fullScreen=", ""));
                    } else if (line.startsWith("frameRate=")) {
                        PerformanceSettings.setLockFPS(Integer.parseInt(line.replaceFirst("frameRate=", "")));
                    } else if (line.startsWith("uncappedFPS=")) {
                        PerformanceSettings.instance.setFramerateUncapped(Boolean.parseBoolean(line.replaceFirst("uncappedFPS=", "")));
                    } else if (line.startsWith("iso_cursor=")) {
                        this.isoCursorVisibility.parse(line.replaceFirst("iso_cursor=", ""));
                    } else if (line.startsWith("showCursorWhileAiming=")) {
                        this.optionShowCursorWhileAiming.parse(line.replaceFirst("showCursorWhileAiming=", ""));
                    } else if (line.startsWith("water=")) {
                        PerformanceSettings.waterQuality = Integer.parseInt(line.replaceFirst("water=", ""));
                    } else if (line.startsWith("puddles=")) {
                        PerformanceSettings.puddlesQuality = Integer.parseInt(line.replaceFirst("puddles=", ""));
                    } else if (line.startsWith("lightFPS=")) {
                        PerformanceSettings.instance.setLightingFPS(Integer.parseInt(line.replaceFirst("lightFPS=", "")));
                    } else if (line.startsWith("perfSkybox=")) {
                        this.perfSkyboxNew.parse(line.replaceFirst("perfSkybox=", ""));
                        this.iPerfSkybox = this.perfSkyboxNew.getValue();
                    } else if (line.startsWith("perfPuddles=")) {
                        this.perfPuddlesNew.parse(line.replaceFirst("perfPuddles=", ""));
                        this.iPerfPuddles = this.perfPuddlesNew.getValue();
                    } else if (line.startsWith("bPerfReflections=")) {
                        this.perfReflectionsNew.parse(line.replaceFirst("bPerfReflections=", ""));
                        this.perfReflections = this.perfReflectionsNew.getValue();
                    } else if (line.startsWith("language=")) {
                        this.optionLanguageName.parse(line.replaceFirst("language=", "").trim());
                    } else if (line.startsWith("zoom=")) {
                        this.optionZoom.parse(line.replaceFirst("zoom=", ""));
                    } else if (line.startsWith("autozoom=")) {
                        this.autoZoom.parse(line.replaceFirst("autozoom=", "").trim());

                        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                            this.setAutoZoom(playerIndex, this.getAutoZoom(playerIndex));
                        }
                    } else if (line.startsWith("fontSize=")) {
                        this.setOptionFontSize(Integer.parseInt(line.replaceFirst("fontSize=", "").trim()));
                    } else if (line.startsWith("moodleSize=")) {
                        this.setOptionMoodleSize(Integer.parseInt(line.replaceFirst("moodleSize=", "").trim()));
                    } else if (line.startsWith("sidebarSize=")) {
                        this.setOptionSidebarSize(Integer.parseInt(line.replaceFirst("sidebarSize=", "").trim()));
                    } else if (line.startsWith("contextMenuFont=")) {
                        this.optionContextMenuFont.parse(line.replaceFirst("contextMenuFont=", "").trim());
                    } else if (line.startsWith("inventoryFont=")) {
                        this.optionInventoryFont.parse(line.replaceFirst("inventoryFont=", "").trim());
                    } else if (line.startsWith("inventoryContainerSize=")) {
                        this.optionInventoryContainerSize.parse(line.replaceFirst("inventoryContainerSize=", ""));
                    } else if (line.startsWith("tooltipFont=")) {
                        this.optionTooltipFont.parse(line.replaceFirst("tooltipFont=", "").trim());
                    } else if (line.startsWith("measurementsFormat=")) {
                        this.optionMeasurementFormat.parse(line.replaceFirst("measurementsFormat=", "").trim());
                    } else if (line.startsWith("clockFormat=")) {
                        this.optionClockFormat.parse(line.replaceFirst("clockFormat=", ""));
                    } else if (line.startsWith("clockSize=")) {
                        this.optionClockSize.parse(line.replaceFirst("clockSize=", ""));
                    } else if (line.startsWith("clock24Hour=")) {
                        this.optionClock24Hour.parse(line.replaceFirst("clock24Hour=", ""));
                    } else if (line.startsWith("vsync=")) {
                        this.optionVsync.parse(line.replaceFirst("vsync=", ""));
                    } else if (line.startsWith("voiceEnable=")) {
                        this.optionVoiceEnable.parse(line.replaceFirst("voiceEnable=", ""));
                    } else if (line.startsWith("voiceMode=")) {
                        this.optionVoiceMode.parse(line.replaceFirst("voiceMode=", ""));
                    } else if (line.startsWith("voiceVADMode=")) {
                        this.optionVoiceVadMode.parse(line.replaceFirst("voiceVADMode=", ""));
                    } else if (line.startsWith("voiceAGCMode=")) {
                        this.optionVoiceAgcMode.parse(line.replaceFirst("voiceAGCMode=", ""));
                    } else if (line.startsWith("voiceVolumeMic=")) {
                        this.optionVoiceVolumeMic.parse(line.replaceFirst("voiceVolumeMic=", ""));
                    } else if (line.startsWith("voiceVolumePlayers=")) {
                        this.optionVoiceVolumePlayers.parse(line.replaceFirst("voiceVolumePlayers=", ""));
                    } else if (line.startsWith("voiceRecordDeviceName=")) {
                        this.optionVoiceRecordDeviceName.parse(line.replaceFirst("voiceRecordDeviceName=", "").trim());
                    } else if (line.startsWith("soundVolume=")) {
                        this.optionSoundVolume.parse(line.replaceFirst("soundVolume=", ""));
                    } else if (line.startsWith("musicVolume=")) {
                        this.optionMusicVolume.parse(line.replaceFirst("musicVolume=", ""));
                    } else if (line.startsWith("ambientVolume=")) {
                        this.optionAmbientVolume.parse(line.replaceFirst("ambientVolume=", ""));
                    } else if (line.startsWith("jumpScareVolume=")) {
                        this.optionJumpScareVolume.parse(line.replaceFirst("jumpScareVolume=", ""));
                    } else if (line.startsWith("musicActionStyle=")) {
                        this.optionMusicActionStyle.parse(line.replaceFirst("musicActionStyle=", ""));
                    } else if (line.startsWith("musicLibrary=")) {
                        this.optionMusicLibrary.parse(line.replaceFirst("musicLibrary=", ""));
                    } else if (line.startsWith("vehicleEngineVolume=")) {
                        this.optionVehicleEngineVolume.parse(line.replaceFirst("vehicleEngineVolume=", ""));
                    } else if (line.startsWith("streamerMode=")) {
                        this.optionStreamerMode.parse(line.replaceFirst("streamerMode=", ""));
                    } else if (line.startsWith("reloadDifficulty=")) {
                        this.optionReloadDifficulty.parse(line.replaceFirst("reloadDifficulty=", ""));
                    } else if (line.startsWith("rackProgress=")) {
                        this.optionRackProgress.parse(line.replaceFirst("rackProgress=", ""));
                    } else if (line.startsWith("controller=")) {
                        String guid = line.replaceFirst("controller=", "");
                        if (!guid.isEmpty()) {
                            JoypadManager.instance.setControllerActive(guid, true);
                        }
                    } else if (line.startsWith("tutorialDone=")) {
                        this.optionTutorialDone.parse(line.replaceFirst("tutorialDone=", ""));
                    } else if (line.startsWith("vehiclesWarningShow=")) {
                        this.optionVehiclesWarningShow.parse(line.replaceFirst("vehiclesWarningShow=", ""));
                    } else if (line.startsWith("bloodDecals=")) {
                        this.setOptionBloodDecals(Integer.parseInt(line.replaceFirst("bloodDecals=", "")));
                    } else if (line.startsWith("focusloss=")) {
                        this.optionFocusloss.parse(line.replaceFirst("focusloss=", ""));
                    } else if (line.startsWith("borderless=")) {
                        this.optionBorderlessWindow.parse(line.replaceFirst("borderless=", ""));
                    } else if (line.startsWith("lockCursorToWindow=")) {
                        this.optionLockCursorToWindow.parse(line.replaceFirst("lockCursorToWindow=", ""));
                    } else if (line.startsWith("textureCompression=")) {
                        this.optionTextureCompression.parse(line.replaceFirst("textureCompression=", ""));
                    } else if (line.startsWith("modelTextureMipmaps=")) {
                        this.optionModelTextureMipmaps.parse(line.replaceFirst("modelTextureMipmaps=", ""));
                    } else if (line.startsWith("texture2x=")) {
                        this.optionTexture2x.parse(line.replaceFirst("texture2x=", ""));
                    } else if (line.startsWith("maxTextureSize=")) {
                        this.optionMaxTextureSize.parse(line.replaceFirst("maxTextureSize=", ""));
                    } else if (line.startsWith("maxVehicleTextureSize=")) {
                        this.optionMaxVehicleTextureSize.parse(line.replaceFirst("maxVehicleTextureSize=", ""));
                    } else if (line.startsWith("zoomLevels1x=")) {
                        this.optionZoomLevels1x.parse(line.replaceFirst("zoomLevels1x=", ""));
                    } else if (line.startsWith("zoomLevels2x=")) {
                        this.optionZoomLevels2x.parse(line.replaceFirst("zoomLevels2x=", ""));
                    } else if (line.startsWith("showChatTimestamp=")) {
                        this.optionShowChatTimestamp.parse(line.replaceFirst("showChatTimestamp=", ""));
                    } else if (line.startsWith("showChatTitle=")) {
                        this.optionShowChatTitle.parse(line.replaceFirst("showChatTitle=", ""));
                    } else if (line.startsWith("chatFontSize=")) {
                        this.optionChatFontSize.parse(line.replaceFirst("chatFontSize=", "").trim());
                    } else if (line.startsWith("minChatOpaque=")) {
                        this.optionMinChatOpaque.parse(line.replaceFirst("minChatOpaque=", ""));
                    } else if (line.startsWith("maxChatOpaque=")) {
                        this.optionMaxChatOpaque.parse(line.replaceFirst("maxChatOpaque=", ""));
                    } else if (line.startsWith("chatFadeTime=")) {
                        this.optionChatFadeTime.parse(line.replaceFirst("chatFadeTime=", ""));
                    } else if (line.startsWith("chatOpaqueOnFocus=")) {
                        this.optionChatOpaqueOnFocus.parse(line.replaceFirst("chatOpaqueOnFocus=", ""));
                    } else if (line.startsWith("doneNewSaveFolder=")) {
                        this.optionDoneNewSaveFolder.parse(line.replaceFirst("doneNewSaveFolder=", ""));
                    } else if (line.startsWith("contentTranslationsEnabled=")) {
                        this.optionEnableContentTranslations.parse(line.replaceFirst("contentTranslationsEnabled=", ""));
                    } else if (line.startsWith("showYourUsername=")) {
                        this.showYourUsername.parse(line.replaceFirst("showYourUsername=", ""));
                    } else if (line.startsWith("riversideDone=")) {
                        this.optionRiversideDone.parse(line.replaceFirst("riversideDone=", ""));
                    } else if (line.startsWith("rosewoodSpawnDone=")) {
                        this.optionRosewoodSpawnDone.parse(line.replaceFirst("rosewoodSpawnDone=", ""));
                    } else if (line.startsWith("gotNewBelt=")) {
                        this.optionGotNewBelt.parse(line.replaceFirst("gotNewBelt=", ""));
                    } else if (line.startsWith("mpTextColor=")) {
                        String colors = line.replaceFirst("mpTextColor=", "").trim();
                        this.optionMpTextColor.parse(colors);
                        float r = (float)((DoubleConfigOption)this.optionMpTextColor.getElement(0)).getValue();
                        float g = (float)((DoubleConfigOption)this.optionMpTextColor.getElement(1)).getValue();
                        float b = (float)((DoubleConfigOption)this.optionMpTextColor.getElement(2)).getValue();
                        this.mpTextColor = new ColorInfo(r, g, b, 1.0F);
                    } else if (line.startsWith("objHighlightColor=")) {
                        String colors = line.replaceFirst("objHighlightColor=", "").trim();
                        this.optionObjectHighlightColor.parse(colors);
                        float r = (float)((DoubleConfigOption)this.optionObjectHighlightColor.getElement(0)).getValue();
                        float g = (float)((DoubleConfigOption)this.optionObjectHighlightColor.getElement(1)).getValue();
                        float b = (float)((DoubleConfigOption)this.optionObjectHighlightColor.getElement(2)).getValue();
                        this.objectHighlitedColor.set(r, g, b, 1.0F);
                    } else if (line.startsWith("workstationHighlightColor=")) {
                        String colors = line.replaceFirst("workstationHighlightColor=", "").trim();
                        this.optionWorkstationHighlightColor.parse(colors);
                        float r = (float)((DoubleConfigOption)this.optionWorkstationHighlightColor.getElement(0)).getValue();
                        float g = (float)((DoubleConfigOption)this.optionWorkstationHighlightColor.getElement(1)).getValue();
                        float b = (float)((DoubleConfigOption)this.optionWorkstationHighlightColor.getElement(2)).getValue();
                        this.workstationHighlitedColor.set(r, g, b, 1.0F);
                    } else if (line.startsWith("goodHighlightColor=")) {
                        String colors = line.replaceFirst("goodHighlightColor=", "").trim();
                        this.optionGoodHighlightColor.parse(colors);
                        float r = (float)((DoubleConfigOption)this.optionGoodHighlightColor.getElement(0)).getValue();
                        float g = (float)((DoubleConfigOption)this.optionGoodHighlightColor.getElement(1)).getValue();
                        float b = (float)((DoubleConfigOption)this.optionGoodHighlightColor.getElement(2)).getValue();
                        this.goodHighlitedColor.set(r, g, b, 1.0F);
                    } else if (line.startsWith("badHighlightColor=")) {
                        String colors = line.replaceFirst("badHighlightColor=", "").trim();
                        this.optionBadHighlightColor.parse(colors);
                        float r = (float)((DoubleConfigOption)this.optionBadHighlightColor.getElement(0)).getValue();
                        float g = (float)((DoubleConfigOption)this.optionBadHighlightColor.getElement(1)).getValue();
                        float b = (float)((DoubleConfigOption)this.optionBadHighlightColor.getElement(2)).getValue();
                        this.badHighlitedColor.set(r, g, b, 1.0F);
                    } else if (line.startsWith("seenNews=")) {
                        this.setSeenUpdateText(line.replaceFirst("seenNews=", ""));
                    } else if (line.startsWith("toggleToAim=")) {
                        this.setToggleToAim(Boolean.parseBoolean(line.replaceFirst("toggleToAim=", "")));
                    } else if (line.startsWith("toggleToRun=")) {
                        this.setToggleToRun(Boolean.parseBoolean(line.replaceFirst("toggleToRun=", "")));
                    } else if (line.startsWith("toggleToSprint=")) {
                        this.setToggleToSprint(Boolean.parseBoolean(line.replaceFirst("toggleToSprint=", "")));
                    } else if (line.startsWith("celsius=")) {
                        this.setCelsius(Boolean.parseBoolean(line.replaceFirst("celsius=", "")));
                    } else if (!line.startsWith("mapOrder=")) {
                        if (line.startsWith("showFirstTimeSneakTutorial=")) {
                            this.setShowFirstTimeSneakTutorial(Boolean.parseBoolean(line.replaceFirst("showFirstTimeSneakTutorial=", "")));
                        } else if (line.startsWith("showFirstTimeSearchTutorial=")) {
                            this.setShowFirstTimeSearchTutorial(Boolean.parseBoolean(line.replaceFirst("showFirstTimeSearchTutorial=", "")));
                        } else if (line.startsWith("termsOfServiceVersion=")) {
                            this.optionTermsOfServiceVersion.parse(line.replaceFirst("termsOfServiceVersion=", ""));
                        } else if (line.startsWith("uiRenderOffscreen=")) {
                            this.optionUiFbo.parse(line.replaceFirst("uiRenderOffscreen=", ""));
                        } else if (line.startsWith("uiRenderFPS=")) {
                            this.optionUiRenderFps.parse(line.replaceFirst("uiRenderFPS=", ""));
                        } else if (line.startsWith("radialMenuKeyToggle=")) {
                            this.optionRadialMenuKeyToggle.parse(line.replaceFirst("radialMenuKeyToggle=", ""));
                        } else if (line.startsWith("reloadRadialInstant=")) {
                            this.optionReloadRadialInstant.parse(line.replaceFirst("reloadRadialInstant=", ""));
                        } else if (line.startsWith("panCameraWhileAiming=")) {
                            this.optionPanCameraWhileAiming.parse(line.replaceFirst("panCameraWhileAiming=", ""));
                        } else if (line.startsWith("panCameraWhileDriving=")) {
                            this.optionPanCameraWhileDriving.parse(line.replaceFirst("panCameraWhileDriving=", ""));
                        } else if (line.startsWith("temperatureDisplayCelsius=")) {
                            this.optionTemperatureDisplayCelsius.parse(line.replaceFirst("temperatureDisplayCelsius=", ""));
                        } else if (line.startsWith("doVideoEffects=")) {
                            this.optionDoVideoEffects.parse(line.replaceFirst("doVideoEffects=", ""));
                        } else if (line.startsWith("doWindSpriteEffects=")) {
                            this.optionDoWindSpriteEffects.parse(line.replaceFirst("doWindSpriteEffects=", ""));
                        } else if (line.startsWith("doDoorSpriteEffects=")) {
                            this.optionDoDoorSpriteEffects.parse(line.replaceFirst("doDoorSpriteEffects=", ""));
                        } else if (line.startsWith("doContainerOutline=")) {
                            this.optionDoContainerOutline.parse(line.replaceFirst("doContainerOutline=", ""));
                        } else if (line.startsWith("updateSneakButton2=")) {
                            this.optionUpdateSneakButton.setValue(true);
                        } else if (line.startsWith("updateSneakButton=")) {
                            this.optionUpdateSneakButton.parse(line.replaceFirst("updateSneakButton=", ""));
                        } else if (line.startsWith("dblTapJogToSprint=")) {
                            this.optionDblTapJogToSprint.parse(line.replaceFirst("dblTapJogToSprint=", ""));
                        } else if (line.startsWith("meleeOutline=")) {
                            this.optionMeleeOutline.parse(line.replaceFirst("meleeOutline=", ""));
                        } else if (line.startsWith("cycleContainerKey=")) {
                            this.optionCycleContainerKey.parse(line.replaceFirst("cycleContainerKey=", "").trim());
                        } else if (line.startsWith("dropItemsOnSquareCenter=")) {
                            this.optionDropItemsOnSquareCenter.parse(line.replaceFirst("dropItemsOnSquareCenter=", ""));
                        } else if (line.startsWith("timedActionGameSpeedReset=")) {
                            this.optionTimedActionGameSpeedReset.parse(line.replaceFirst("timedActionGameSpeedReset=", ""));
                        } else if (line.startsWith("shoulderButtonContainerSwitch=")) {
                            this.optionShoulderButtonContainerSwitch.parse(line.replaceFirst("shoulderButtonContainerSwitch=", ""));
                        } else if (line.startsWith("controllerButtonStyle=")) {
                            this.optionControllerButtonStyle.parse(line.replaceFirst("controllerButtonStyle=", ""));
                        } else if (line.startsWith("singleContextMenu=")) {
                            this.optionSingleContextMenu.parse(line.replaceFirst("singleContextMenu=", ""));
                        } else if (line.startsWith("renderPrecipIndoors=")) {
                            this.optionRenderPrecipIndoors.parse(line.replaceFirst("renderPrecipIndoors=", ""));
                        } else if (line.startsWith("precipitationSpeedMultiplier=")) {
                            this.setOptionPrecipitationSpeedMultiplier(Float.parseFloat(line.replaceFirst("precipitationSpeedMultiplier=", "")));
                        } else if (line.startsWith("autoProneAtk=")) {
                            this.optionAutoProneAtk.parse(line.replaceFirst("autoProneAtk=", ""));
                        } else if (line.startsWith("3DGroundItem=")) {
                            this.option3dGroundItem.parse(line.replaceFirst("3DGroundItem=", ""));
                        } else if (line.startsWith("tieredZombieUpdates=")) {
                            this.setOptionTieredZombieUpdates(Boolean.parseBoolean(line.replaceFirst("tieredZombieUpdates=", "")));
                        } else if (line.startsWith("progressBar=")) {
                            this.setOptionProgressBar(Boolean.parseBoolean(line.replaceFirst("progressBar=", "")));
                        } else if (line.startsWith("corpseShadows=")) {
                            this.optionCorpseShadows.parse(line.replaceFirst("corpseShadows=", ""));
                        } else if (line.startsWith("simpleClothingTextures=")) {
                            this.optionSimpleClothingTextures.parse(line.replaceFirst("simpleClothingTextures=", ""));
                        } else if (line.startsWith("simpleWeaponTextures=")) {
                            this.optionSimpleWeaponTextures.parse(line.replaceFirst("simpleWeaponTextures=", ""));
                        } else if (line.startsWith("autoDrink=")) {
                            this.optionAutoDrink.parse(line.replaceFirst("autoDrink=", ""));
                        } else if (line.startsWith("leaveKeyInIgnition=")) {
                            this.optionLeaveKeyInIgnition.parse(line.replaceFirst("leaveKeyInIgnition=", ""));
                        } else if (line.startsWith("autoWalkContainer=")) {
                            this.optionAutoWalkContainer.parse(line.replaceFirst("autoWalkContainer=", ""));
                        } else if (line.startsWith("searchModeOverlayEffect=")) {
                            this.optionSearchModeOverlayEffect.parse(line.replaceFirst("searchModeOverlayEffect=", ""));
                        } else if (line.startsWith("ignoreProneZombieRange=")) {
                            this.optionIgnoreProneZombieRange.parse(line.replaceFirst("ignoreProneZombieRange=", ""));
                        } else if (line.startsWith("fogQuality=")) {
                            PerformanceSettings.fogQuality = Integer.parseInt(line.replaceFirst("fogQuality=", ""));
                        } else if (line.startsWith("viewConeOpacity=")) {
                            PerformanceSettings.viewConeOpacity = Integer.parseInt(line.replaceFirst("viewConeOpacity=", ""));
                        } else if (line.startsWith("renderPrecipitation=")) {
                            this.optionRenderPrecipitation.parse(line.replaceFirst("renderPrecipitation=", ""));
                        } else if (line.startsWith("showItemModInfo=")) {
                            this.optionShowItemModInfo.parse(line.replaceFirst("showItemModInfo=", ""));
                        } else if (line.startsWith("showCraftingXP=")) {
                            this.optionShowCraftingXp.parse(line.replaceFirst("showCraftingXP=", ""));
                        } else if (line.startsWith("showSurvivalGuide=")) {
                            this.optionShowSurvivalGuide.parse(line.replaceFirst("showSurvivalGuide=", ""));
                        } else if (line.startsWith("showFirstAnimalZoneInfo=")) {
                            this.optionShowFirstAnimalZoneInfo.parse(line.replaceFirst("showFirstAnimalZoneInfo=", ""));
                        } else if (line.startsWith("enableLeftJoystickRadialMenu=")) {
                            this.optionEnableLeftJoystickRadialMenu.parse(line.replaceFirst("enableLeftJoystickRadialMenu=", ""));
                        } else if (line.startsWith("enableDyslexicFont=")) {
                            this.optionEnableDyslexicFont.parse(line.replaceFirst("enableDyslexicFont=", ""));
                        }
                    } else {
                        if (this.version < 7) {
                            line = "mapOrder=";
                        }

                        String[] order = line.replaceFirst("mapOrder=", "").split(";");

                        for (String map : order) {
                            map = map.trim();
                            if (!map.isEmpty()) {
                                ActiveMods.getById("default").getMapOrder().add(map);
                            }
                        }

                        ZomboidFileSystem.instance.saveModsFile();
                    }
                }

                if (this.optionLanguageName.getValue().isEmpty()) {
                    this.optionLanguageName.setValue(System.getProperty("user.language").toUpperCase());
                }

                if (!this.optionDoneNewSaveFolder.getValue()) {
                    this.handleNewSaveFolderFormat();
                    this.optionDoneNewSaveFolder.setValue(true);
                }
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
            } finally {
                br.close();
            }

            this.saveOptions();
            return true;
        }
    }

    public boolean loadOptions() throws IOException {
        this.loadedOptions = false;
        File newFile = new File(ZomboidFileSystem.instance.getCacheDirSub("options.ini"));
        if (!newFile.exists()) {
            this.initOptionsINI();
            return false;
        } else {
            this.loadedOptions = true;

            for (int n = 0; n < 4; n++) {
                this.setAutoZoom(n, false);
            }

            this.optionLanguageName.setValue("");

            try {
                for (ConfigOption configOption : this.options) {
                    if (configOption instanceof ArrayConfigOption arrayConfigOption && arrayConfigOption.isMultiLine()) {
                        arrayConfigOption.clear();
                    }
                }

                ConfigFile configFile = new ConfigFile();
                configFile.setVersionString("version");
                if (configFile.read(newFile.getAbsolutePath())) {
                    int VERSION = configFile.getVersion();
                    this.version = VERSION;

                    for (int i = 0; i < configFile.getOptions().size(); i++) {
                        ConfigOption configOptionx = configFile.getOptions().get(i);
                        String optionName = configOptionx.getName();
                        String optionValue = configOptionx.getValueAsString();
                        optionName = this.upgradeOptionName(optionName, VERSION);
                        optionValue = this.upgradeOptionValue(optionName, optionValue, VERSION);
                        ConfigOption coreOption = this.optionByName.get(optionName);
                        if (coreOption == null) {
                            coreOption = this.fakeOptionByName.get(optionName);
                        }

                        if (coreOption != null) {
                            coreOption.parse(optionValue);
                        }
                    }

                    width = this.optionScreenWidth.getValue();
                    height = this.optionScreenHeight.getValue();
                    PerformanceSettings.fogQuality = this.optionFogQuality.getValue();
                    PerformanceSettings.viewConeOpacity = this.optionViewConeOpacity.getValue();
                    PerformanceSettings.setLockFPS(this.optionLockFps.getValue());
                    PerformanceSettings.instance.setFramerateUncapped(this.optionUncappedFps.getValue());
                    PerformanceSettings.waterQuality = this.optionWaterQuality.getValue();
                    PerformanceSettings.puddlesQuality = this.optionPuddlesQuality.getValue();
                    PerformanceSettings.instance.setLightingFPS(this.optionLightingFps.getValue());
                    if (this.optionUncappedFps.getValue()) {
                        this.optionUncappedFps.setValue(false);
                        this.optionLockFps.setValue(60);
                        PerformanceSettings.setLockFPS(this.optionLockFps.getValue());
                        PerformanceSettings.instance.setFramerateUncapped(this.optionUncappedFps.getValue());
                    }

                    this.iPerfSkybox = this.perfSkyboxNew.getValue();
                    this.iPerfPuddles = this.perfPuddlesNew.getValue();
                    this.perfReflections = this.perfReflectionsNew.getValue();

                    for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                        this.offscreenBuffer.autoZoom[playerIndex] = this.getAutoZoom(playerIndex);
                    }

                    for (int i = 0; i < this.optionActiveControllerGuids.size(); i++) {
                        ConfigOption element = this.optionActiveControllerGuids.getElement(i);
                        String guid = element.getValueAsString().trim();
                        if (!guid.isEmpty()) {
                            JoypadManager.instance.setControllerActive(guid, true);
                        }
                    }

                    MovingObjectUpdateScheduler.instance.setEnabled(this.optionTieredZombieUpdates.getValue());
                    this.setOptionCodeFontSize(this.optionCodeFontSize.getValue());
                }

                if (this.optionLanguageName.getValue().isEmpty()) {
                    this.optionLanguageName.setValue(System.getProperty("user.language").toUpperCase());
                }

                if (!this.optionDoneNewSaveFolder.getValue()) {
                    this.handleNewSaveFolderFormat();
                    this.optionDoneNewSaveFolder.setValue(true);
                }
            } catch (Exception var9) {
                ExceptionLogger.logException(var9);
            }

            this.saveOptions();
            return true;
        }
    }

    private String upgradeOptionName(String optionName, int version) {
        return optionName;
    }

    private String upgradeOptionValue(String optionName, String optionValue, int version) {
        return optionValue;
    }

    private void initOptionsINI() throws IOException {
        this.saveFolder = getMyDocumentFolder();
        File newFile2 = new File(this.saveFolder);
        newFile2.mkdir();
        this.copyPasteFolders("mods");
        this.setOptionLanguageName(System.getProperty("user.language").toUpperCase());
        if (Translator.getAzertyMap().contains(Translator.getLanguage().name())) {
            this.setAzerty(true);
        }

        if (!GameServer.server) {
            try {
                int w = 0;
                int h = 0;
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int[] monitorX = new int[1];
                int[] monitorY = new int[1];
                int[] monitorW = new int[1];
                int[] monitorH = new int[1];
                GLFW.glfwGetMonitorWorkarea(GLFW.glfwGetPrimaryMonitor(), monitorX, monitorY, monitorW, monitorH);

                for (int n = 0; n < modes.length; n++) {
                    if (modes[n].getWidth() > w && modes[n].getWidth() < monitorW[0] && modes[n].getHeight() < monitorH[0]) {
                        w = modes[n].getWidth();
                        h = modes[n].getHeight();
                    }
                }

                width = w;
                height = h;
            } catch (LWJGLException var10) {
                ExceptionLogger.logException(var10);
            }
        }

        this.setOptionZoomLevels2x("50;75;125;150;175;200");
        this.setOptionZoomLevels1x("50;75;125;150;175;200");
        this.saveOptions();
    }

    private void handleNewSaveFolderFormat() {
        File newSaveFolder = new File(ZomboidFileSystem.instance.getSaveDir());
        newSaveFolder.mkdir();
        ArrayList<String> gameModes = new ArrayList<>();
        gameModes.add("Beginner");
        gameModes.add("Survival");
        gameModes.add("A Really CD DA");
        gameModes.add("LastStand");
        gameModes.add("Opening Hours");
        gameModes.add("Sandbox");
        gameModes.add("Tutorial");
        gameModes.add("Winter is Coming");
        gameModes.add("You Have One Day");
        File previousSave = null;
        File newSave = null;

        try {
            for (String path : gameModes) {
                previousSave = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + path);
                newSave = new File(ZomboidFileSystem.instance.getSaveDir() + File.separator + path);
                if (previousSave.exists()) {
                    newSave.mkdir();
                    Files.move(previousSave.toPath(), newSave.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception var7) {
        }
    }

    public boolean isDefaultOptions() {
        return !this.loadedOptions;
    }

    public boolean isDedicated() {
        return GameServer.server;
    }

    private void copyPasteFolders(String dir) {
        File srcFolder = new File(dir).getAbsoluteFile();
        if (srcFolder.exists()) {
            this.searchFolders(srcFolder, dir);
        }
    }

    private void searchFolders(File file, String relative) {
        if (file.isDirectory()) {
            File newFile = new File(this.saveFolder + File.separator + relative);
            newFile.mkdir();
            String[] internalNames = file.list();

            for (int i = 0; i < internalNames.length; i++) {
                this.searchFolders(new File(file.getAbsolutePath() + File.separator + internalNames[i]), relative + File.separator + internalNames[i]);
            }
        } else {
            this.copyPasteFile(file, relative);
        }
    }

    private void copyPasteFile(File file, String relative) {
        FileOutputStream outStream = null;
        FileInputStream inStream = null;

        try {
            File newFile = new File(this.saveFolder + File.separator + relative);
            newFile.createNewFile();
            outStream = new FileOutputStream(newFile);
            inStream = new FileInputStream(file);
            outStream.getChannel().transferFrom(inStream.getChannel(), 0L, file.length());
        } catch (Exception var14) {
            var14.printStackTrace();
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }

                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException var13) {
                var13.printStackTrace();
            }
        }
    }

    public static String getMyDocumentFolder() {
        return ZomboidFileSystem.instance.getCacheDir();
    }

    public void saveOptions_OLD() throws IOException {
        File newFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "options.ini");
        if (!newFile.exists()) {
            newFile.createNewFile();
        }

        FileWriter fw = new FileWriter(newFile);

        try {
            fw.write("version=" + this.fileversion + "\r\n");
            fw.write("width=" + this.getScreenWidth() + "\r\n");
            fw.write("height=" + this.getScreenHeight() + "\r\n");
            fw.write("fullScreen=" + this.fullScreen.getValueAsString() + "\r\n");
            fw.write("frameRate=" + PerformanceSettings.getLockFPS() + "\r\n");
            fw.write("uncappedFPS=" + PerformanceSettings.instance.isFramerateUncapped() + "\r\n");
            fw.write("iso_cursor=" + this.isoCursorVisibility.getValueAsString() + "\r\n");
            fw.write("showCursorWhileAiming=" + this.optionShowCursorWhileAiming.getValueAsString() + "\r\n");
            fw.write("water=" + PerformanceSettings.waterQuality + "\r\n");
            fw.write("puddles=" + PerformanceSettings.puddlesQuality + "\r\n");
            fw.write("lightFPS=" + PerformanceSettings.lightingFps + "\r\n");
            fw.write("perfSkybox=" + this.perfSkyboxNew.getValueAsString() + "\r\n");
            fw.write("perfPuddles=" + this.perfPuddlesNew.getValueAsString() + "\r\n");
            fw.write("bPerfReflections=" + this.perfReflectionsNew.getValueAsString() + "\r\n");
            fw.write("vidMem=" + this.vidMem + "\r\n");
            fw.write("language=" + this.optionLanguageName.getValueAsString() + "\r\n");
            fw.write("zoom=" + this.optionZoom.getValueAsString() + "\r\n");
            fw.write("fontSize=" + this.optionFontSize.getValueAsString() + "\r\n");
            fw.write("moodleSize=" + this.optionMoodleSize.getValueAsString() + "\r\n");
            fw.write("sidebarSize=" + this.optionSidebarSize.getValueAsString() + "\r\n");
            fw.write("contextMenuFont=" + this.optionContextMenuFont.getValueAsString() + "\r\n");
            fw.write("inventoryFont=" + this.optionInventoryFont.getValueAsString() + "\r\n");
            fw.write("inventoryContainerSize=" + this.optionInventoryContainerSize.getValueAsString() + "\r\n");
            fw.write("tooltipFont=" + this.optionTooltipFont.getValueAsString() + "\r\n");
            fw.write("clockFormat=" + this.optionClockFormat.getValueAsString() + "\r\n");
            fw.write("clockSize=" + this.optionClockSize.getValueAsString() + "\r\n");
            fw.write("clock24Hour=" + this.optionClock24Hour.getValueAsString() + "\r\n");
            fw.write("measurementsFormat=" + this.optionMeasurementFormat.getValueAsString() + "\r\n");
            fw.write("autozoom=" + this.autoZoom.getValueAsString() + "\r\n");
            fw.write("vsync=" + this.optionVsync.getValueAsString() + "\r\n");
            fw.write("soundVolume=" + this.optionSoundVolume.getValue() + "\r\n");
            fw.write("ambientVolume=" + this.optionAmbientVolume.getValueAsString() + "\r\n");
            fw.write("musicVolume=" + this.optionMusicVolume.getValueAsString() + "\r\n");
            fw.write("jumpScareVolume=" + this.optionJumpScareVolume.getValueAsString() + "\r\n");
            fw.write("musicActionStyle=" + this.optionMusicActionStyle.getValueAsString() + "\r\n");
            fw.write("musicLibrary=" + this.optionMusicLibrary.getValueAsString() + "\r\n");
            fw.write("vehicleEngineVolume=" + this.optionVehicleEngineVolume.getValueAsString() + "\r\n");
            fw.write("vehicleStreamerMode=" + this.optionStreamerMode.getValueAsString() + "\r\n");
            fw.write("voiceEnable=" + this.optionVoiceEnable.getValueAsString() + "\r\n");
            fw.write("voiceMode=" + this.optionVoiceMode.getValueAsString() + "\r\n");
            fw.write("voiceVADMode=" + this.optionVoiceVadMode.getValueAsString() + "\r\n");
            fw.write("voiceAGCMode=" + this.optionVoiceAgcMode.getValueAsString() + "\r\n");
            fw.write("voiceVolumeMic=" + this.optionVoiceVolumeMic.getValueAsString() + "\r\n");
            fw.write("voiceVolumePlayers=" + this.optionVoiceVolumePlayers.getValueAsString() + "\r\n");
            fw.write("voiceRecordDeviceName=" + this.optionVoiceRecordDeviceName.getValueAsString() + "\r\n");
            fw.write("reloadDifficulty=" + this.optionReloadDifficulty.getValueAsString() + "\r\n");
            fw.write("rackProgress=" + this.optionRackProgress.getValueAsString() + "\r\n");

            for (String name : JoypadManager.instance.activeControllerGuids) {
                fw.write("controller=" + name + "\r\n");
            }

            fw.write("tutorialDone=" + this.isTutorialDone() + "\r\n");
            fw.write("vehiclesWarningShow=" + this.isVehiclesWarningShow() + "\r\n");
            fw.write("bloodDecals=" + this.optionBloodDecals.getValueAsString() + "\r\n");
            fw.write("focusloss=" + this.optionFocusloss.getValueAsString() + "\r\n");
            fw.write("borderless=" + this.optionBorderlessWindow.getValueAsString() + "\r\n");
            fw.write("lockCursorToWindow=" + this.optionLockCursorToWindow.getValueAsString() + "\r\n");
            fw.write("textureCompression=" + this.optionTextureCompression.getValueAsString() + "\r\n");
            fw.write("modelTextureMipmaps=" + this.optionModelTextureMipmaps.getValueAsString() + "\r\n");
            fw.write("texture2x=" + this.optionTexture2x.getValueAsString() + "\r\n");
            fw.write("maxTextureSize=" + this.optionMaxTextureSize.getValueAsString() + "\r\n");
            fw.write("maxVehicleTextureSize=" + this.optionMaxVehicleTextureSize.getValueAsString() + "\r\n");
            fw.write("zoomLevels1x=" + this.optionZoomLevels1x.getValueAsString() + "\r\n");
            fw.write("zoomLevels2x=" + this.optionZoomLevels2x.getValueAsString() + "\r\n");
            fw.write("showChatTimestamp=" + this.optionShowChatTimestamp.getValueAsString() + "\r\n");
            fw.write("showChatTitle=" + this.optionShowChatTitle.getValueAsString() + "\r\n");
            fw.write("chatFontSize=" + this.optionChatFontSize.getValueAsString() + "\r\n");
            fw.write("minChatOpaque=" + this.optionMinChatOpaque.getValueAsString() + "\r\n");
            fw.write("maxChatOpaque=" + this.optionMaxChatOpaque.getValueAsString() + "\r\n");
            fw.write("chatFadeTime=" + this.optionChatFadeTime.getValueAsString() + "\r\n");
            fw.write("chatOpaqueOnFocus=" + this.optionChatOpaqueOnFocus.getValueAsString() + "\r\n");
            fw.write("doneNewSaveFolder=" + this.optionDoneNewSaveFolder.getValueAsString() + "\r\n");
            fw.write("contentTranslationsEnabled=" + this.optionEnableContentTranslations.getValueAsString() + "\r\n");
            fw.write("showYourUsername=" + this.showYourUsername.getValueAsString() + "\r\n");
            fw.write("rosewoodSpawnDone=" + this.optionRosewoodSpawnDone.getValueAsString() + "\r\n");
            if (this.mpTextColor != null) {
                fw.write("mpTextColor=" + this.optionMpTextColor.getValueAsString() + "\r\n");
            }

            fw.write("objHighlightColor=" + this.optionObjectHighlightColor.getValueAsString() + "\r\n");
            fw.write("workstationHighlightColor=" + this.optionWorkstationHighlightColor.getValueAsString() + "\r\n");
            fw.write("seenNews=" + this.getSeenUpdateText() + "\r\n");
            fw.write("toggleToAim=" + this.toggleToAim.getValueAsString() + "\r\n");
            fw.write("toggleToRun=" + this.toggleToRun.getValueAsString() + "\r\n");
            fw.write("toggleToSprint=" + this.toggleToSprint.getValueAsString() + "\r\n");
            fw.write("celsius=" + this.celsius.getValueAsString() + "\r\n");
            fw.write("riversideDone=" + this.isRiversideDone() + "\r\n");
            fw.write("showFirstTimeSneakTutorial=" + this.isShowFirstTimeSneakTutorial() + "\r\n");
            fw.write("showFirstTimeSearchTutorial=" + this.isShowFirstTimeSearchTutorial() + "\r\n");
            fw.write("termsOfServiceVersion=" + this.optionTermsOfServiceVersion.getValueAsString() + "\r\n");
            fw.write("uiRenderOffscreen=" + this.optionUiFbo.getValueAsString() + "\r\n");
            fw.write("uiRenderFPS=" + this.optionUiRenderFps.getValueAsString() + "\r\n");
            fw.write("radialMenuKeyToggle=" + this.optionRadialMenuKeyToggle.getValueAsString() + "\r\n");
            fw.write("reloadRadialInstant=" + this.optionReloadRadialInstant.getValueAsString() + "\r\n");
            fw.write("panCameraWhileAiming=" + this.optionPanCameraWhileAiming.getValueAsString() + "\r\n");
            fw.write("panCameraWhileDriving=" + this.optionPanCameraWhileDriving.getValueAsString() + "\r\n");
            fw.write("temperatureDisplayCelsius=" + this.optionTemperatureDisplayCelsius.getValueAsString() + "\r\n");
            fw.write("doVideoEffects=" + this.optionDoVideoEffects.getValueAsString() + "\r\n");
            fw.write("doWindSpriteEffects=" + this.optionDoWindSpriteEffects.getValueAsString() + "\r\n");
            fw.write("doDoorSpriteEffects=" + this.optionDoDoorSpriteEffects.getValueAsString() + "\r\n");
            fw.write("updateSneakButton=" + this.optionUpdateSneakButton.getValueAsString() + "\r\n");
            fw.write("dblTapJogToSprint=" + this.optionDblTapJogToSprint.getValueAsString() + "\r\n");
            fw.write("gotNewBelt=" + this.optionGotNewBelt.getValueAsString() + "\r\n");
            fw.write("meleeOutline=" + this.optionMeleeOutline.getValueAsString() + "\r\n");
            fw.write("cycleContainerKey=" + this.optionCycleContainerKey.getValueAsString() + "\r\n");
            fw.write("dropItemsOnSquareCenter=" + this.optionDropItemsOnSquareCenter.getValueAsString() + "\r\n");
            fw.write("timedActionGameSpeedReset=" + this.optionTimedActionGameSpeedReset.getValueAsString() + "\r\n");
            fw.write("shoulderButtonContainerSwitch=" + this.optionShoulderButtonContainerSwitch.getValueAsString() + "\r\n");
            fw.write("controllerButtonStyle=" + this.optionControllerButtonStyle.getValueAsString() + "\r\n");
            fw.write("singleContextMenu=" + this.optionSingleContextMenu.getValueAsString() + "\r\n");
            fw.write("renderPrecipIndoors=" + this.optionRenderPrecipIndoors.getValueAsString() + "\r\n");
            fw.write("precipitationSpeedMultiplier=" + this.optionPrecipitationSpeedMultiplier.getValueAsString() + "\r\n");
            fw.write("autoProneAtk=" + this.optionAutoProneAtk.getValueAsString() + "\r\n");
            fw.write("3DGroundItem=" + this.option3dGroundItem.getValueAsString() + "\r\n");
            fw.write("tieredZombieUpdates=" + this.getOptionTieredZombieUpdates() + "\r\n");
            fw.write("progressBar=" + this.isOptionProgressBar() + "\r\n");
            fw.write("corpseShadows=" + this.getOptionCorpseShadows() + "\r\n");
            fw.write("simpleClothingTextures=" + this.getOptionSimpleClothingTextures() + "\r\n");
            fw.write("simpleWeaponTextures=" + this.getOptionSimpleWeaponTextures() + "\r\n");
            fw.write("autoDrink=" + this.getOptionAutoDrink() + "\r\n");
            fw.write("leaveKeyInIgnition=" + this.getOptionLeaveKeyInIgnition() + "\r\n");
            fw.write("autoWalkContainer=" + this.getOptionAutoWalkContainer() + "\r\n");
            fw.write("searchModeOverlayEffect=" + this.getOptionSearchModeOverlayEffect() + "\r\n");
            fw.write("ignoreProneZombieRange=" + this.getOptionIgnoreProneZombieRange() + "\r\n");
            fw.write("fogQuality=" + PerformanceSettings.fogQuality + "\r\n");
            fw.write("viewConeOpacity=" + PerformanceSettings.viewConeOpacity + "\r\n");
            fw.write("renderPrecipitation=" + this.optionRenderPrecipitation.getValueAsString() + "\r\n");
            fw.write("showItemModInfo=" + this.optionShowItemModInfo.getValueAsString() + "\r\n");
            fw.write("showItemModInfo=" + this.optionShowCraftingXp.getValueAsString() + "\r\n");
            fw.write("showSurvivalGuide=" + this.optionShowSurvivalGuide.getValueAsString() + "\r\n");
            fw.write("showFirstAnimalZoneInfo=" + this.optionShowFirstAnimalZoneInfo.getValueAsString() + "\r\n");
            fw.write("enableLeftJoystickRadialMenu=" + this.optionEnableLeftJoystickRadialMenu.getValueAsString() + "\r\n");
            fw.write("doContainerOutline=" + this.optionDoContainerOutline.getValueAsString() + "\r\n");
            fw.write("goodHighlightColor=" + this.optionGoodHighlightColor.getValueAsString() + "\r\n");
            fw.write("badHighlightColor=" + this.optionBadHighlightColor.getValueAsString() + "\r\n");
            fw.write("enableDyslexicFont=" + this.optionEnableDyslexicFont.getValueAsString() + "\r\n");
            fw.write("showItemModInfo=" + this.optionShowCraftingXp.getValueAsString() + "\r\n");
        } catch (Exception var8) {
            var8.printStackTrace();
        } finally {
            fw.close();
        }
    }

    public void saveOptions() throws IOException {
        ConfigFile configFile = new ConfigFile();
        configFile.setVersionString("version");
        configFile.setWriteTooltips(false);
        ArrayList<ConfigOption> options = new ArrayList<>(this.options);
        if (!System.getProperty("os.name").contains("OS X")) {
            options.remove(this.optionMacosIgnoreMouseWheelAcceleration);
            options.remove(this.optionMacosMapHorizontalMouseWheelToVertical);
        }

        this.addFakeOptionsForWriting(options);
        options.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
        configFile.write(ZomboidFileSystem.instance.getCacheDirSub("options.ini"), this.fileversion, options);
    }

    private void addFakeOptionsForWriting(ArrayList<ConfigOption> options) {
        this.optionFogQuality.setValue(PerformanceSettings.fogQuality);
        this.optionViewConeOpacity.setValue(PerformanceSettings.viewConeOpacity);
        this.optionLightingFps.setValue(PerformanceSettings.lightingFps);
        this.optionLockFps.setValue(PerformanceSettings.getLockFPS());
        this.optionPuddlesQuality.setValue(PerformanceSettings.puddlesQuality);
        this.optionScreenHeight.setValue(this.getScreenHeight());
        this.optionScreenWidth.setValue(this.getScreenWidth());
        this.optionTieredZombieUpdates.setValue(this.getOptionTieredZombieUpdates());
        this.optionUncappedFps.setValue(PerformanceSettings.instance.isFramerateUncapped());
        this.optionWaterQuality.setValue(PerformanceSettings.waterQuality);
        this.optionActiveControllerGuids.clear();

        for (String guid : JoypadManager.instance.activeControllerGuids) {
            this.optionActiveControllerGuids.parse(guid);
        }

        options.addAll(this.fakeOptions);
    }

    public void setWindowed(boolean b) {
        if (!isUseGameViewport()) {
            RenderThread.invokeOnRenderContext(() -> {
                if (b != this.fullScreen.getValue()) {
                    setDisplayMode(this.getScreenWidth(), this.getScreenHeight(), b);
                }

                this.fullScreen.setValue(b);
                Display.setResizable(!b);

                try {
                    this.saveOptions();
                } catch (IOException var3) {
                    var3.printStackTrace();
                }
            });
        }
    }

    public boolean isFullScreen() {
        return this.fullScreen.getValue();
    }

    public KahluaTable getScreenModes() {
        ArrayList<String> test = new ArrayList<>();
        KahluaTable t = LuaManager.platform.newTable();
        File newFile = new File(LuaManager.getLuaCacheDir() + File.separator + "screenresolution.ini");
        int c = 1;

        try {
            if (!newFile.exists()) {
                newFile.createNewFile();
                FileWriter fw = new FileWriter(newFile);
                Integer w = 0;
                Integer h = 0;
                DisplayMode[] modes = Display.getAvailableDisplayModes();

                for (int n = 0; n < modes.length; n++) {
                    w = modes[n].getWidth();
                    h = modes[n].getHeight();
                    if (!test.contains(w + " x " + h)) {
                        t.rawset(c, w + " x " + h);
                        fw.write(w + " x " + h + " \r\n");
                        test.add(w + " x " + h);
                        c++;
                    }
                }

                fw.close();
            } else {
                BufferedReader br = new BufferedReader(new FileReader(newFile));

                String line;
                for (line = null; (line = br.readLine()) != null; c++) {
                    t.rawset(c, line.trim());
                }

                br.close();
            }
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return t;
    }

    public static void setDisplayMode(int width, int height, boolean fullscreen) {
        RenderThread.invokeOnRenderContext(() -> setDisplayModeInternal(width, height, fullscreen));
    }

    private static void setDisplayModeInternal(int width, int height, boolean fullscreen) {
        boolean OptionBorderlessWindow = getInstance().getOptionBorderlessWindow();
        if (Display.getWidth() != width
            || Display.getHeight() != height
            || Display.isFullscreen() != fullscreen
            || Display.isBorderlessWindow() != OptionBorderlessWindow) {
            getInstance().fullScreen.setValue(fullscreen);

            try {
                DisplayMode targetDisplayMode = null;
                if (!fullscreen) {
                    if (OptionBorderlessWindow) {
                        if (Display.getWindow() != 0L && Display.isFullscreen()) {
                            Display.setFullscreen(false);
                        }

                        long monitor = GLFW.glfwGetPrimaryMonitor();
                        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitor);
                        targetDisplayMode = new DisplayMode(vidmode.width(), vidmode.height());
                    } else {
                        targetDisplayMode = new DisplayMode(width, height);
                    }
                } else {
                    DisplayMode[] modes = Display.getAvailableDisplayModes();
                    int freq = 0;
                    DisplayMode closest = null;

                    for (DisplayMode current : modes) {
                        if (current.getWidth() == width && current.getHeight() == height && current.isFullscreenCapable()) {
                            if ((targetDisplayMode == null || current.getFrequency() >= freq)
                                && (targetDisplayMode == null || current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
                                targetDisplayMode = current;
                                freq = current.getFrequency();
                            }

                            if (current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel()
                                && current.getFrequency() == Display.getDesktopDisplayMode().getFrequency()) {
                                targetDisplayMode = current;
                                break;
                            }
                        }

                        if (current.isFullscreenCapable()
                            && (
                                closest == null
                                    || Math.abs(current.getWidth() - width) < Math.abs(closest.getWidth() - width)
                                    || current.getWidth() == closest.getWidth() && current.getFrequency() > freq
                            )) {
                            closest = current;
                            freq = current.getFrequency();
                            System.out.println("closest width=" + current.getWidth() + " freq=" + current.getFrequency());
                        }
                    }

                    if (targetDisplayMode == null && closest != null) {
                        targetDisplayMode = closest;
                    }
                }

                if (targetDisplayMode == null) {
                    DebugLog.log("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
                    return;
                }

                Display.setBorderlessWindow(OptionBorderlessWindow);
                if (fullscreen) {
                    Display.setDisplayModeAndFullscreen(targetDisplayMode);
                } else {
                    Display.setDisplayMode(targetDisplayMode);
                    Display.setFullscreen(false);
                }

                if (!fullscreen && OptionBorderlessWindow) {
                    Display.setResizable(false);
                } else if (!fullscreen) {
                    Display.setResizable(false);
                    Display.setResizable(true);
                }

                if (Display.isCreated()) {
                    DebugLog.log(
                        "Display mode changed to "
                            + Display.getWidth()
                            + "x"
                            + Display.getHeight()
                            + " freq="
                            + Display.getDisplayMode().getFrequency()
                            + " fullScreen="
                            + Display.isFullscreen()
                    );
                }
            } catch (LWJGLException var12) {
                DebugLog.log("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + var12);
            }
        }
    }

    private boolean isFunctionKey(int key) {
        return key >= 59 && key <= 68 || key >= 87 && key <= 105 || key == 113;
    }

    public boolean isDoingTextEntry() {
        if (currentTextEntryBox == null) {
            return false;
        } else {
            return !currentTextEntryBox.isEditable() ? false : currentTextEntryBox.isDoingTextEntry();
        }
    }

    private void updateKeyboardAux(UITextEntryInterface entry, int eventKey) {
        boolean isCtrlKeyDown = GameKeyboard.isKeyDownRaw(29) || GameKeyboard.isKeyDownRaw(157);
        if (eventKey == 28 || eventKey == 156) {
            entry.onKeyEnter();
        } else if (eventKey == 1) {
            entry.onOtherKey(1);
            GameKeyboard.eatKeyPress(1);
        } else if (eventKey == 15) {
            entry.onOtherKey(15);
            LuaEventManager.triggerEvent("SwitchChatStream");
        } else if (eventKey != 58) {
            if (eventKey == 199) {
                entry.onKeyHome();
            } else if (eventKey == 207) {
                entry.onKeyEnd();
            } else if (eventKey == 200) {
                entry.onKeyUp();
            } else if (eventKey == 208) {
                entry.onKeyDown();
            } else if (eventKey != 29) {
                if (eventKey != 157) {
                    if (eventKey != 42) {
                        if (eventKey != 54) {
                            if (eventKey != 56) {
                                if (eventKey != 184) {
                                    if (eventKey == 203) {
                                        entry.onKeyLeft();
                                    } else if (eventKey == 205) {
                                        entry.onKeyRight();
                                    } else if (!this.isFunctionKey(eventKey)) {
                                        if (eventKey == 211) {
                                            entry.onKeyDelete();
                                        } else if (eventKey == 14) {
                                            entry.onKeyBack();
                                        } else if (isCtrlKeyDown && eventKey == 47) {
                                            entry.pasteFromClipboard();
                                        } else if (isCtrlKeyDown && eventKey == 46) {
                                            entry.copyToClipboard();
                                        } else if (isCtrlKeyDown && eventKey == 45) {
                                            entry.cutToClipboard();
                                        } else if (isCtrlKeyDown && eventKey == 30) {
                                            entry.selectAll();
                                        } else if (!entry.isIgnoreFirst()) {
                                            if (!entry.isTextLimit()) {
                                                char eventChar = Keyboard.getEventCharacter();
                                                if (eventChar != 0) {
                                                    if (entry.isOnlyNumbers() && eventChar != '.' && eventChar != '-') {
                                                        try {
                                                            Double.parseDouble(String.valueOf(eventChar));
                                                        } catch (Exception var6) {
                                                            return;
                                                        }
                                                    }

                                                    if (!entry.isOnlyText() || eventChar >= 'A' && eventChar <= 'Z' || eventChar >= 'a' && eventChar <= 'z') {
                                                        entry.putCharacter(eventChar);
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
            }
        }
    }

    public void updateKeyboard() {
        if (this.isDoingTextEntry()) {
            while (Keyboard.next()) {
                if (this.isDoingTextEntry() && Keyboard.getEventKeyState()) {
                    int eventKey = Keyboard.getEventKey();
                    this.updateKeyboardAux(currentTextEntryBox, eventKey);
                }
            }

            if (currentTextEntryBox != null && currentTextEntryBox.isIgnoreFirst()) {
                currentTextEntryBox.setIgnoreFirst(false);
            }
        }
    }

    public void quit() {
        DebugType.ExitDebug.debugln("Core.quit 1");
        if (IsoPlayer.getInstance() != null) {
            DebugType.ExitDebug.debugln("Core.quit 2");
            exiting = true;
        } else {
            DebugType.ExitDebug.debugln("Core.quit 3");

            try {
                this.saveOptions();
            } catch (IOException var2) {
                var2.printStackTrace();
            }

            GameClient.instance.Shutdown();
            SteamUtils.shutdown();
            DebugType.ExitDebug.debugln("Core.quit 4");
            System.exit(0);
        }
    }

    public void exitToMenu() {
        DebugType.ExitDebug.debugln("Core.exitToMenu");
        exiting = true;
    }

    public void quitToDesktop() {
        DebugType.ExitDebug.debugln("Core.quitToDesktop");
        GameWindow.closeRequested = true;
    }

    public boolean supportRes(int width, int height) throws LWJGLException {
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        boolean bFound = false;

        for (int n = 0; n < modes.length; n++) {
            if (modes[n].getWidth() == width && modes[n].getHeight() == height && modes[n].isFullscreenCapable()) {
                return true;
            }
        }

        return false;
    }

    public void init(int width, int height) throws LWJGLException {
        System.setProperty("org.lwjgl.opengl.Window.undecorated", this.getOptionBorderlessWindow() ? "true" : "false");
        if (!System.getProperty("os.name").contains("OS X") && !System.getProperty("os.name").startsWith("Win")) {
            DebugLog.log("Creating display. If this fails, you may need to install xrandr.");
        }

        try {
            setDisplayModeInternal(width, height, this.fullScreen.getValue());
            Display.create(new PixelFormat(32, 0, 24, 8, 0));
        } catch (LWJGLException var4) {
            Display.destroy();
            Display.setDisplayModeAndFullscreen(Display.getDesktopDisplayMode());
            Display.create(new PixelFormat(32, 0, 24, 8, 0));
        }

        if (debug && "1".equalsIgnoreCase(System.getProperty("zomboid.opengl.debugcontext"))) {
            PZGLUtil.InitGLDebugging();
        }

        this.fullScreen.setValue(Display.isFullscreen());
        DebugLog.log("GraphicsCard: " + GL11.glGetString(7936) + " " + GL11.glGetString(7937));
        DebugLog.log("OpenGL version: " + GL11.glGetString(7938));
        DebugLog.log("Desktop resolution " + Display.getDesktopDisplayMode().getWidth() + "x" + Display.getDesktopDisplayMode().getHeight());
        DebugLog.log("Initial resolution " + Core.width + "x" + Core.height + " fullScreen=" + this.fullScreen.getValueAsString());
        GLVertexBufferObject.init();
        DebugLog.General.println("VSync: %s", this.getOptionVSync() ? "ON" : "OFF");
        Display.setVSyncEnabled(this.getOptionVSync());
        GL11.glEnable(3553);
        IndieGL.glBlendFuncA(770, 771);
        GL32.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
    }

    private boolean setupMultiFBO() {
        try {
            if (!this.offscreenBuffer.test()) {
                return false;
            } else {
                this.offscreenBuffer
                    .setZoomLevelsFromOption(tileScale == 2 ? this.optionZoomLevels2x.getValueAsString() : this.optionZoomLevels1x.getValueAsString());
                this.offscreenBuffer.create(Display.getWidth(), Display.getHeight());
                return true;
            }
        } catch (Exception var2) {
            var2.printStackTrace();
            return false;
        }
    }

    public static void setInitialSize() {
        initialHeight = height;
        initialWidth = width;
    }

    public void setScreenSize(int width, int height) {
        if (Core.width != width || height != Core.height) {
            int oldWidth = Core.width;
            int oldHeight = Core.height;
            DebugLog.log(
                "Screen resolution changed from "
                    + oldWidth
                    + "x"
                    + oldHeight
                    + " to "
                    + width
                    + "x"
                    + height
                    + " fullScreen="
                    + this.fullScreen.getValueAsString()
            );
            Core.width = width;
            Core.height = height;
            if (debug) {
                DebugContext.instance.destroy();
                DebugContext.instance.initRenderTarget();
            }

            if (this.offscreenBuffer != null && this.offscreenBuffer.current != null) {
                this.offscreenBuffer.destroy();

                try {
                    this.offscreenBuffer
                        .setZoomLevelsFromOption(tileScale == 2 ? this.optionZoomLevels2x.getValueAsString() : this.optionZoomLevels1x.getValueAsString());
                    this.offscreenBuffer.create(width, height);
                } catch (Exception var8) {
                    var8.printStackTrace();
                }
            }

            try {
                LuaEventManager.triggerEvent("OnResolutionChange", oldWidth, oldHeight, width, height);
            } catch (Exception var7) {
                var7.printStackTrace();
            }

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null) {
                    player.dirtyRecalcGridStackTime = 2.0F;
                }
            }
        }
    }

    public static boolean supportCompressedTextures() {
        return GL.getCapabilities().GL_EXT_texture_compression_latc;
    }

    public void StartFrame() {
        if (LuaManager.thread == null || !LuaManager.thread.step) {
            if (SceneShaderStore.weatherShader != null && this.offscreenBuffer.current != null) {
                SceneShaderStore.weatherShader.setTexture(this.offscreenBuffer.getTexture(0));
            }

            SpriteRenderer.instance.prePopulating();
            IndieGL.glAlphaFunc(516, 0.0F);
            IndieGL.enableBlend();
            UIManager.resize();
            boolean PlayerTripping = false;
            Texture.bindCount = 0;
            SpriteRenderer.instance.glClearDepth(0.0F);
            IndieGL.glClear(18176);
            if (DebugOptions.instance.terrain.renderTiles.highContrastBg.getValue()) {
                SpriteRenderer.instance.glClearColor(255, 0, 255, 255);
                SpriteRenderer.instance.glClear(16384);
            }

            SpriteRenderer.instance.glClearDepth(1.0F);
            if (this.offscreenBuffer.current != null) {
                SpriteRenderer.instance.glBuffer(1, 0);
            }

            IndieGL.glDoStartFrame(this.getScreenWidth(), this.getScreenWidth(), this.getCurrentPlayerZoom(), 0);
            IndieGL.StartShader(SceneShaderStore.defaultShaderId);
            this.frameStage = 1;
        }
    }

    public void StartFrame(int nPlayer, boolean clear) {
        if (!LuaManager.thread.step) {
            this.offscreenBuffer.update();
            IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
            if (isoGameCharacter != null) {
                PlayerCamera camera = IsoCamera.cameras[nPlayer];
                camera.calculateModelViewProjection(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
                camera.calculateFixForJigglyModels(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
            }

            if (SceneShaderStore.weatherShader != null && this.offscreenBuffer.current != null) {
                SceneShaderStore.weatherShader.setTexture(this.offscreenBuffer.getTexture(nPlayer));
            }

            if (clear) {
                SpriteRenderer.instance.prePopulating();
            }

            if (!clear) {
                SpriteRenderer.instance.initFromIsoCamera(nPlayer);
            }

            Texture.bindCount = 0;
            if (this.offscreenBuffer.current != null) {
                SpriteRenderer.instance.glBuffer(1, nPlayer);
            }

            IndieGL.glDepthMask(true);
            IndieGL.glDoStartFrame(this.getScreenWidth(), this.getScreenHeight(), this.getZoom(nPlayer), nPlayer);
            IndieGL.glClear(17664);
            if (DebugOptions.instance.terrain.renderTiles.highContrastBg.getValue()) {
                SpriteRenderer.instance.glClearColor(255, 0, 255, 255);
                SpriteRenderer.instance.glClear(16384);
            }

            IndieGL.enableBlend();
            this.frameStage = 1;
        }
    }

    public TextureFBO getOffscreenBuffer() {
        return this.offscreenBuffer.getCurrent(0);
    }

    public TextureFBO getOffscreenBuffer(int nPlayer) {
        return this.offscreenBuffer.getCurrent(nPlayer);
    }

    public void setLastRenderedFBO(TextureFBO fbo) {
        this.offscreenBuffer.fboRendered = fbo;
    }

    public void DoStartFrameStuff(int w, int h, float zoom, int player) {
        this.DoStartFrameStuff(w, h, zoom, player, false);
    }

    public void DoStartFrameStuff(int w, int h, float zoom, int player, boolean isTextFrame) {
        this.DoStartFrameStuffInternal(w, h, zoom, player, isTextFrame, false, false);
    }

    public void DoEndFrameStuffFx(int w, int h, int player) {
        GL11.glPopAttrib();
        this.stack--;
        this.projectionMatrixStack.pop();
        this.modelViewMatrixStack.pop();
        this.stack--;
    }

    public void DoStartFrameStuffSmartTextureFx(int w, int h, int player) {
        this.DoStartFrameStuffInternal(w, h, 1.0F, player, false, true, true);
    }

    private void DoStartFrameStuffInternal(int w, int h, float zoom, int player, boolean isTextFrame, boolean isFx, boolean isSmartTexture) {
        GL32.glEnable(3042);
        GL32.glDepthFunc(519);
        int screenW = this.getScreenWidth();
        int screenH = this.getScreenHeight();
        if (!isSmartTexture && !isFx) {
            w = screenW;
        }

        if (!isSmartTexture && !isFx) {
            h = screenH;
        }

        if (!isSmartTexture && player != -1) {
            w /= IsoPlayer.numPlayers > 1 ? 2 : 1;
            h /= IsoPlayer.numPlayers > 2 ? 2 : 1;
        }

        if (!isFx) {
            while (this.stack > 0) {
                try {
                    GL11.glPopAttrib();
                    this.stack -= 2;
                } catch (Throwable var18) {
                    int depth1 = GL11.glGetInteger(2992);

                    while (depth1-- > 0) {
                        GL11.glPopAttrib();
                    }

                    this.stack = 0;
                }
            }
        }

        GL11.glAlphaFunc(516, 0.0F);
        GL11.glPushAttrib(2048);
        this.stack++;
        this.stack++;
        Matrix4f PROJECTION = this.projectionMatrixStack.alloc();
        if (!isSmartTexture && !isTextFrame) {
            PROJECTION.setOrtho2D(0.0F, w * zoom, h * zoom, 0.0F);
        } else {
            PROJECTION.setOrtho2D(0.0F, w, h, 0.0F);
        }

        this.projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = this.modelViewMatrixStack.alloc();
        MODELVIEW.identity();
        this.modelViewMatrixStack.push(MODELVIEW);
        if (player != -1) {
            int tw = w;
            int th = h;
            int ow;
            int oh;
            if (isTextFrame) {
                ow = w;
                oh = h;
            } else {
                ow = screenW;
                oh = screenH;
                if (IsoPlayer.numPlayers > 1) {
                    ow = screenW / 2;
                }

                if (IsoPlayer.numPlayers > 2) {
                    oh = screenH / 2;
                }
            }

            if (isFx) {
                tw = ow;
                th = oh;
            }

            float y = 0.0F;
            float x = ow * (player % 2);
            if (player >= 2) {
                y += oh;
            }

            if (isTextFrame) {
                y = getInstance().getScreenHeight() - th - y;
            }

            GL11.glViewport((int)x, (int)y, tw, th);
            GL11.glEnable(3089);
            GL11.glScissor((int)x, (int)y, tw, th);
            SpriteRenderer.instance.setRenderingPlayerIndex(player);
        } else {
            GL11.glViewport(0, 0, w, h);
        }
    }

    public void ChangeWorldViewport(int w, int h, int player) {
        this.DoStartFrameNoZoom(w, h, 1.0F, player, false, false, false);
    }

    public void StartFrameFlipY(int w, int h, float zoom, int player) {
        this.DoStartFrameFlipY(w, h, zoom, player, false, false, false);
    }

    private void DoStartFrameFlipY(int w, int h, float zoom, int player, boolean isTextFrame, boolean isFx, boolean isSmartTexture) {
        GL32.glEnable(3042);
        GL32.glDepthFunc(519);
        GL11.glAlphaFunc(516, 0.0F);
        GL14.glBlendFuncSeparate(1, 771, 773, 1);
        GL11.glPushAttrib(2048);
        this.stack++;
        this.stack++;
        Matrix4f PROJECTION = this.projectionMatrixStack.alloc();
        if (isSmartTexture || isTextFrame) {
            PROJECTION.setOrtho2D(0.0F, w, h, 0.0F);
        } else if (zoom == 1.0F) {
            PROJECTION.setOrtho2D(w / 4.0F, w * 3 / 4.0F, 0.0F, h / 2.0F);
        } else {
            PROJECTION.setOrtho2D(0.0F, w, 0.0F, h);
        }

        this.projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = this.modelViewMatrixStack.alloc();
        MODELVIEW.identity();
        this.modelViewMatrixStack.push(MODELVIEW);
        GL11.glViewport(0, 0, w, h);
    }

    public void DoStartFrameNoZoom(int w, int h, float zoom, int player, boolean isTextFrame, boolean isFx, boolean isSmartTexture) {
        GL32.glEnable(3042);
        GL32.glDepthFunc(519);
        int screenW = this.getScreenWidth();
        int screenH = this.getScreenHeight();
        if (!DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
            if (!isSmartTexture && !isFx) {
                w = screenW;
            }

            if (!isSmartTexture && !isFx) {
                h = screenH;
            }

            if (!isSmartTexture && player != -1) {
                w /= IsoPlayer.numPlayers > 1 ? 2 : 1;
                h /= IsoPlayer.numPlayers > 2 ? 2 : 1;
            }
        }

        if (!isFx) {
            while (this.stack > 0) {
                try {
                    GL11.glPopAttrib();
                    this.stack -= 2;
                } catch (OpenGLException var18) {
                    int depth1 = GL11.glGetInteger(2992);

                    while (depth1-- > 0) {
                        GL11.glPopAttrib();
                    }

                    this.stack = 0;
                }
            }
        }

        GL11.glAlphaFunc(516, 0.0F);
        GL11.glPushAttrib(2048);
        this.stack++;
        this.stack++;
        Matrix4f PROJECTION = this.projectionMatrixStack.alloc();
        if (!isSmartTexture && !isTextFrame) {
            PROJECTION.setOrtho2D(0.0F, w, h, 0.0F);
        } else {
            PROJECTION.setOrtho2D(0.0F, w, h, 0.0F);
        }

        this.projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = this.modelViewMatrixStack.alloc();
        MODELVIEW.identity();
        this.modelViewMatrixStack.push(MODELVIEW);
        if (player != -1) {
            float x = IsoCamera.getScreenLeft(player);
            float y = IsoCamera.getScreenTop(player);
            GL11.glViewport((int)x, (int)y, w, h);
            GL11.glEnable(3089);
            GL11.glScissor((int)x, (int)y, w, h);
        } else {
            GL11.glViewport(0, 0, w, h);
        }
    }

    public void DoPushIsoStuff(float ox, float oy, float oz, float useangle, boolean vehicle) {
        float cx = getInstance().floatParamMap.get(0);
        float cy = getInstance().floatParamMap.get(1);
        float cz = getInstance().floatParamMap.get(2);
        double x = cx;
        double y = cy;
        double z = cz;
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        int playerIndex = renderState.playerIndex;
        PlayerCamera cam = renderState.playerCamera[playerIndex];
        float rcx = cam.rightClickX;
        float rcy = cam.rightClickY;
        float tox = cam.getTOffX();
        float toy = cam.getTOffY();
        float defx = cam.deferedX;
        float defy = cam.deferedY;
        x -= cam.XToIso(-tox - rcx, -toy - rcy, 0.0F);
        y -= cam.YToIso(-tox - rcx, -toy - rcy, 0.0F);
        x += defx;
        y += defy;
        double screenWidth = cam.offscreenWidth / 1920.0F;
        double screenHeight = cam.offscreenHeight / 1920.0F;
        Matrix4f PROJECTION = this.projectionMatrixStack.alloc();
        PROJECTION.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -10.0F, 10.0F);
        this.projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = this.modelViewMatrixStack.alloc();
        float SCALE = (float)(2.0 / Math.sqrt(2048.0));
        MODELVIEW.scaling(scale);
        MODELVIEW.scale(tileScale / 2.0F);
        MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = ox - x;
        double difY = oy - y;
        MODELVIEW.translate(-((float)difX), (float)(oz - z) * 2.44949F, -((float)difY));
        if (vehicle) {
            MODELVIEW.scale(-1.0F, 1.0F, 1.0F);
        } else {
            MODELVIEW.scale(-1.5F, 1.5F, 1.5F);
        }

        MODELVIEW.rotate(useangle + (float) Math.PI, 0.0F, 1.0F, 0.0F);
        if (!vehicle) {
            MODELVIEW.translate(0.0F, -0.48F, 0.0F);
        }

        this.modelViewMatrixStack.push(MODELVIEW);
        GL11.glDepthRange(-10.0, 10.0);
    }

    public void DoPushIsoStuff2D(float ox, float oy, float oz, float useangle, boolean vehicle) {
        float cx = getInstance().floatParamMap.get(0);
        float cy = getInstance().floatParamMap.get(1);
        float cz = getInstance().floatParamMap.get(2);
        double x = cx;
        double y = cy;
        double z = cz;
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        int playerIndex = renderState.playerIndex;
        PlayerCamera cam = renderState.playerCamera[playerIndex];
        float rcx = cam.rightClickX;
        float rcy = cam.rightClickY;
        float tox = cam.getTOffX();
        float toy = cam.getTOffY();
        float defx = cam.deferedX;
        float defy = cam.deferedY;
        x -= cam.XToIso(-tox - rcx, -toy - rcy, 0.0F);
        y -= cam.YToIso(-tox - rcx, -toy - rcy, 0.0F);
        x += defx;
        y += defy;
        double screenWidth = cam.offscreenWidth / 1920.0F;
        double screenHeight = cam.offscreenHeight / 1920.0F;
        Matrix4f PROJECTION = this.projectionMatrixStack.alloc();
        PROJECTION.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -10.0F, 10.0F);
        this.projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = this.modelViewMatrixStack.alloc();
        float SCALE = (float)(2.0 / Math.sqrt(2048.0));
        MODELVIEW.scaling(scale);
        MODELVIEW.scale(tileScale / 2.0F);
        MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = ox - x;
        double difY = oy - y;
        MODELVIEW.translate(-((float)difX), (float)(oz - z) * 2.5F, -((float)difY));
        if (vehicle) {
            MODELVIEW.scale(-1.0F, 1.0F, 1.0F);
        } else {
            MODELVIEW.scale(-1.5F, 1.5F, 1.5F);
        }

        MODELVIEW.rotate(useangle + (float) Math.PI, 0.0F, 1.0F, 0.0F);
        if (!vehicle) {
            MODELVIEW.translate(0.0F, -0.48F, 0.0F);
        }

        MODELVIEW.rotate((float) (-Math.PI / 2), 1.0F, 0.0F, 0.0F);
        this.modelViewMatrixStack.push(MODELVIEW);
        GL11.glDepthRange(0.0, 1.0);
    }

    public void DoPushIsoParticleStuff(float ox, float oy, float oz) {
        Matrix4f PROJECTION = this.projectionMatrixStack.alloc();
        float cx = getInstance().floatParamMap.get(0);
        float cy = getInstance().floatParamMap.get(1);
        float cz = getInstance().floatParamMap.get(2);
        float screenWidth = Math.abs(getInstance().getOffscreenWidth(0)) / 1920.0F;
        float screenHeight = Math.abs(getInstance().getOffscreenHeight(0)) / 1080.0F;
        PROJECTION.setOrtho(-screenWidth / 2.0F, screenWidth / 2.0F, -screenHeight / 2.0F, screenHeight / 2.0F, -10.0F, 10.0F);
        this.projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = this.modelViewMatrixStack.alloc();
        MODELVIEW.scaling(scale, scale, scale);
        MODELVIEW.rotate(62.65607F, 1.0F, 0.0F, 0.0F);
        MODELVIEW.translate(0.0F, -2.72F, 0.0F);
        MODELVIEW.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        MODELVIEW.scale(1.7099999F, 14.193F, 1.7099999F);
        MODELVIEW.scale(0.59F, 0.59F, 0.59F);
        MODELVIEW.translate(-(ox - cx), oz - cz, -(oy - cy));
        this.modelViewMatrixStack.push(MODELVIEW);
        GL11.glDepthRange(0.0, 1.0);
    }

    public void DoPopIsoStuff() {
        GL11.glDepthRange(0.0, 1.0);
        GL11.glEnable(3008);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(false);
        GLStateRenderThread.AlphaTest.restore();
        GLStateRenderThread.DepthFunc.restore();
        GLStateRenderThread.DepthMask.restore();
        this.projectionMatrixStack.pop();
        this.modelViewMatrixStack.pop();
    }

    public void DoEndFrameStuff(int w, int h) {
        try {
            GL11.glPopAttrib();
            this.stack--;
            this.projectionMatrixStack.pop();
            this.modelViewMatrixStack.pop();
            this.stack--;
        } catch (Throwable var5) {
            int depth1 = GL11.glGetInteger(2992);

            while (depth1-- > 0) {
                GL11.glPopAttrib();
            }

            this.stack = 0;
        }

        GL11.glDisable(3089);
    }

    public void RenderOffScreenBuffer() {
        if (LuaManager.thread == null || !LuaManager.thread.step) {
            if (this.offscreenBuffer.current != null) {
                IndieGL.disableStencilTest();
                IndieGL.glDoStartFrame(width, height, 1.0F, -1);
                IndieGL.disableBlend();
                this.offscreenBuffer.render();
                IndieGL.glDoEndFrame();
                IndieGL.enableBlend();
            }
        }
    }

    public void StartFrameText(int nPlayer) {
        if (LuaManager.thread == null || !LuaManager.thread.step) {
            IndieGL.glDoStartFrame(IsoCamera.getScreenWidth(nPlayer), IsoCamera.getScreenHeight(nPlayer), 1.0F, nPlayer, true);
            this.frameStage = 2;
        }
    }

    public boolean StartFrameUI() {
        if (LuaManager.thread != null && LuaManager.thread.step) {
            return false;
        } else {
            boolean renderThisFrame = true;
            if (UIManager.useUiFbo) {
                if (UIManager.defaultthread == LuaManager.debugthread) {
                    this.uiRenderThisFrame = true;
                } else {
                    this.uiRenderAccumulator = this.uiRenderAccumulator + GameTime.getInstance().getThirtyFPSMultiplier();
                    this.uiRenderThisFrame = this.uiRenderAccumulator >= 30.0F / getInstance().getOptionUIRenderFPS();
                }

                if (this.uiRenderThisFrame) {
                    SpriteRenderer.instance.startOffscreenUI();
                    SpriteRenderer.instance.glBuffer(2, 0);
                } else {
                    renderThisFrame = false;
                }
            } else {
                UIManager.uiTextureContentsValid = false;
            }

            IndieGL.glDoStartFrame(width, height, 1.0F, -1);
            IndieGL.glStencilMask(255);
            IndieGL.glClear(1024);
            UIManager.resize();
            this.frameStage = 3;
            return renderThisFrame;
        }
    }

    public void reinitKeyMaps() {
        this.keyMaps = new HashMap<>();
        this.keyBindingByKeyValue.clear();
    }

    public boolean invalidBindingShiftCtrl(Core.KeyBinding keyB) {
        boolean shiftDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        boolean ctrlDown = GameKeyboard.isKeyDownRaw(29) || GameKeyboard.isKeyDownRaw(157);
        boolean altDown = GameKeyboard.isKeyDownRaw(56) || GameKeyboard.isKeyDownRaw(184);
        if (keyB.keyValue() == 42 || keyB.keyValue() == 54) {
            return !shiftDown;
        } else if (keyB.keyValue() != 29 && keyB.keyValue() != 157) {
            if (keyB.keyValue() != 56 && keyB.keyValue() != 184) {
                if (keyB.shift() && !shiftDown) {
                    return true;
                } else if (keyB.ctrl() && !ctrlDown) {
                    return true;
                } else if (keyB.alt() && !altDown) {
                    return true;
                } else {
                    Core.KeyBindingList bindingList = this.keyBindingByKeyValue.get(keyB.keyValue());
                    if (bindingList == null) {
                        return false;
                    } else {
                        for (int i = 0; i < bindingList.size(); i++) {
                            Core.KeyBinding other = bindingList.get(i);
                            if (other != keyB && (other.shift() != keyB.shift() || other.ctrl() != keyB.ctrl() || other.alt() != keyB.alt())) {
                                boolean matchesInput = keyB.shift() == shiftDown && keyB.ctrl() == ctrlDown && keyB.alt() == altDown;
                                boolean otherMatchesInput = other.shift() == shiftDown && other.ctrl() == ctrlDown && other.alt() == altDown;
                                if (!matchesInput && otherMatchesInput) {
                                    return true;
                                }
                            }
                        }

                        return false;
                    }
                }
            } else {
                return !altDown;
            }
        } else {
            return !ctrlDown;
        }
    }

    public boolean isKey(String keyName, Integer key) {
        Core.KeyBinding keyB = getInstance().getKeyBinding(keyName);
        return this.invalidBindingShiftCtrl(keyB) ? false : keyB.keyValue() == key || keyB.altKey() == key;
    }

    public int getKey(String keyName) {
        return this.getKeyBinding(keyName).keyValue();
    }

    public Core.KeyBinding getKeyBinding(String keyName) {
        return this.keyMaps.getOrDefault(keyName, KEYBINDING_EMPTY);
    }

    public Core.KeyBinding getKeyBinding(int keyId) {
        Core.KeyBindingList bindingList = this.keyBindingByKeyValue.get(keyId);
        return bindingList != null && !bindingList.isEmpty() ? bindingList.get(0) : KEYBINDING_EMPTY;
    }

    public void addKeyBinding(String keyName, int key, int altKey, boolean shift, boolean ctrl, boolean alt) {
        Core.KeyBinding binding = new Core.KeyBinding(keyName, key, altKey, shift, ctrl, alt);
        Core.KeyBinding previous = this.keyMaps.put(keyName, binding);
        if (previous != null) {
            Core.KeyBindingList bindingList = this.keyBindingByKeyValue.get(previous.keyValue);
            bindingList.remove(previous);
        }

        Core.KeyBindingList bindingList = this.keyBindingByKeyValue.get(key);
        if (bindingList == null) {
            this.keyBindingByKeyValue.put(key, bindingList = new Core.KeyBindingList());
        }

        if (!bindingList.contains(binding)) {
            bindingList.add(binding);
        }
    }

    public int getAltKey(String keyName) {
        return this.getKeyBinding(keyName).altKey();
    }

    public static boolean isLastStand() {
        return lastStand;
    }

    public String getVersion() {
        return gameVersion + ".2 " + getGitRevisionString();
    }

    @UsedFromLua
    public String getGitSha() {
        if (this.gitSha == null) {
            this.gitSha = IS_DEV ? DevMainScreenState.getDevSha() : "986d733f73d95810492027afa9594e1a62502a48";
        }

        return this.gitSha;
    }

    public String getGitRevision() {
        if (this.gitRevisionString == null) {
            if (IS_DEV) {
                this.gitRevisionString = DevMainScreenState.getDevVersion();
            } else {
                this.gitRevisionString = "986d733f73d95810492027afa9594e1a62502a48"
                    .substring(0, Math.min("986d733f73d95810492027afa9594e1a62502a48".length(), 20));
            }
        }

        return this.gitRevisionString;
    }

    public static String getGitRevisionString() {
        return IS_DEV ? core.getGitRevision() : "986d733f73d95810492027afa9594e1a62502a48 2026-01-19 12:14:48 (ZB)";
    }

    public GameVersion getGameVersion() {
        return gameVersion;
    }

    public GameVersion getBreakModGameVersion() {
        return new GameVersion(42, 0, "");
    }

    public String getSteamServerVersion() {
        return this.steamServerVersion;
    }

    public void DoFrameReady() {
        this.updateKeyboard();
    }

    public float getCurrentPlayerZoom() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        return this.getZoom(playerIndex);
    }

    public float getZoom(int playerIndex) {
        return this.offscreenBuffer != null ? this.offscreenBuffer.getDisplayZoom(playerIndex) * (tileScale / 2.0F) : 1.0F;
    }

    public float getNextZoom(int playerIndex, int del) {
        return this.offscreenBuffer != null ? this.offscreenBuffer.getNextZoom(playerIndex, del) : 1.0F;
    }

    public float getMinZoom() {
        return this.offscreenBuffer != null ? this.offscreenBuffer.getMinZoom() * (tileScale / 2.0F) : 1.0F;
    }

    public float getMaxZoom() {
        return this.offscreenBuffer != null ? this.offscreenBuffer.getMaxZoom() * (tileScale / 2.0F) : 1.0F;
    }

    public void doZoomScroll(int playerIndex, int del) {
        if (this.offscreenBuffer != null) {
            this.offscreenBuffer.doZoomScroll(playerIndex, del);
        }
    }

    public String getSaveFolder() {
        return this.saveFolder;
    }

    public boolean getOptionZoom() {
        return this.optionZoom.getValue();
    }

    public void setOptionZoom(boolean zoom) {
        this.optionZoom.setValue(zoom);
    }

    public void zoomOptionChanged(boolean inGame) {
        if (inGame) {
            RenderThread.invokeOnRenderContext(() -> {
                if (this.getOptionZoom() && !safeModeForced) {
                    safeMode = false;
                    this.supportsFbo = true;
                    this.offscreenBuffer.zoomEnabled = true;
                    this.supportsFBO();
                } else {
                    this.offscreenBuffer.destroy();
                    safeMode = true;
                    this.supportsFbo = false;
                    this.offscreenBuffer.zoomEnabled = false;
                }
            });
            DebugLog.log("SafeMode is " + (safeMode ? "on" : "off"));
        } else {
            safeMode = safeModeForced;
            this.offscreenBuffer.zoomEnabled = this.getOptionZoom() && !safeModeForced;
        }
    }

    public void zoomLevelsChanged() {
        if (this.offscreenBuffer.current != null) {
            RenderThread.invokeOnRenderContext(() -> {
                this.offscreenBuffer.destroy();
                this.zoomOptionChanged(true);
            });
        }
    }

    public boolean isZoomEnabled() {
        return this.offscreenBuffer.zoomEnabled;
    }

    public void setZoomEnalbed(boolean val) {
        this.offscreenBuffer.zoomEnabled = val;
    }

    public void initFBOs() {
        if (this.getOptionZoom() && !safeModeForced) {
            RenderThread.invokeQueryOnRenderContext(this::supportsFBO);
        } else {
            safeMode = true;
            this.offscreenBuffer.zoomEnabled = false;
        }

        DebugLog.log("SafeMode is " + (safeMode ? "on" : "off"));
    }

    public boolean getAutoZoom(int playerIndex) {
        return ((BooleanConfigOption)this.autoZoom.getElement(playerIndex)).getValue();
    }

    public void setAutoZoom(int playerIndex, boolean auto) {
        ((BooleanConfigOption)this.autoZoom.getElement(playerIndex)).setValue(auto);
        this.offscreenBuffer.autoZoom[playerIndex] = auto;
    }

    public boolean getOptionVSync() {
        return this.optionVsync.getValue();
    }

    public void setOptionVSync(boolean sync) {
        this.optionVsync.setValue(sync);
        RenderThread.invokeOnRenderContext(() -> Display.setVSyncEnabled(sync));
    }

    public int getOptionSoundVolume() {
        return this.optionSoundVolume.getValue();
    }

    public float getRealOptionSoundVolume() {
        return this.optionSoundVolume.getValue() / 10.0F;
    }

    public void setOptionSoundVolume(int volume) {
        this.optionSoundVolume.setValue(volume);
        if (SoundManager.instance != null) {
            SoundManager.instance.setSoundVolume(this.getOptionSoundVolume() / 10.0F);
        }
    }

    public int getOptionMusicVolume() {
        return this.optionMusicVolume.getValue();
    }

    public void setOptionMusicVolume(int volume) {
        this.optionMusicVolume.setValue(volume);
        if (SoundManager.instance != null) {
            SoundManager.instance.setMusicVolume(this.getOptionMusicVolume() / 10.0F);
        }
    }

    public int getOptionAmbientVolume() {
        return this.optionAmbientVolume.getValue();
    }

    public void setOptionAmbientVolume(int volume) {
        this.optionAmbientVolume.setValue(volume);
        if (SoundManager.instance != null) {
            SoundManager.instance.setAmbientVolume(this.getOptionAmbientVolume() / 10.0F);
        }
    }

    public int getOptionJumpScareVolume() {
        return this.optionJumpScareVolume.getValue();
    }

    public void setOptionJumpScareVolume(int volume) {
        this.optionJumpScareVolume.setValue(volume);
    }

    public int getOptionMusicActionStyle() {
        return this.optionMusicActionStyle.getValue();
    }

    public void setOptionMusicActionStyle(int v) {
        this.optionMusicActionStyle.setValue(v);
    }

    public int getOptionMusicLibrary() {
        return this.optionMusicLibrary.getValue();
    }

    public void setOptionMusicLibrary(int m) {
        this.optionMusicLibrary.setValue(m);
    }

    public int getOptionVehicleEngineVolume() {
        return this.optionVehicleEngineVolume.getValue();
    }

    public void setOptionVehicleEngineVolume(int volume) {
        this.optionVehicleEngineVolume.setValue(volume);
        if (SoundManager.instance != null) {
            SoundManager.instance.setVehicleEngineVolume(this.getOptionVehicleEngineVolume() / 10.0F);
        }
    }

    public boolean getOptionStreamerMode() {
        return this.optionStreamerMode.getValue();
    }

    public void setOptionStreamerMode(boolean b) {
        this.optionStreamerMode.setValue(b);
    }

    public boolean getOptionVoiceEnable() {
        return this.optionVoiceEnable.getValue();
    }

    public void setOptionVoiceEnable(boolean option) {
        this.setOptionVoiceEnable(option, true);
    }

    public void setOptionVoiceEnable(boolean option, boolean bRestartClient) {
        if (this.optionVoiceEnable.getValue() != option) {
            this.optionVoiceEnable.setValue(option);
            if (bRestartClient) {
                VoiceManager.instance.VoiceRestartClient(option);
            }
        }
    }

    public int getOptionVoiceMode() {
        return this.optionVoiceMode.getValue();
    }

    public void setOptionVoiceMode(int option) {
        this.optionVoiceMode.setValue(option);
        VoiceManager.instance.setMode(this.getOptionVoiceMode());
    }

    public int getOptionVoiceVADMode() {
        return this.optionVoiceVadMode.getValue();
    }

    public void setOptionVoiceVADMode(int option) {
        this.optionVoiceVadMode.setValue(option);
        VoiceManager.instance.setVADMode(this.getOptionVoiceVADMode());
    }

    public int getOptionVoiceAGCMode() {
        return this.optionVoiceAgcMode.getValue();
    }

    public void setOptionVoiceAGCMode(int option) {
        this.optionVoiceAgcMode.setValue(option);
        VoiceManager.instance.setAGCMode(this.getOptionVoiceAGCMode());
    }

    public int getOptionVoiceVolumeMic() {
        return this.optionVoiceVolumeMic.getValue();
    }

    public void setOptionVoiceVolumeMic(int option) {
        this.optionVoiceVolumeMic.setValue(option);
        VoiceManager.instance.setVolumeMic(this.getOptionVoiceVolumeMic());
    }

    public int getOptionVoiceVolumePlayers() {
        return this.optionVoiceVolumePlayers.getValue();
    }

    public void setOptionVoiceVolumePlayers(int option) {
        this.optionVoiceVolumePlayers.setValue(option);
        VoiceManager.instance.setVolumePlayers(option);
    }

    public String getOptionVoiceRecordDeviceName() {
        return this.optionVoiceRecordDeviceName.getValue();
    }

    public void setOptionVoiceRecordDeviceName(String option) {
        this.optionVoiceRecordDeviceName.setValue(option);
        VoiceManager.instance.UpdateRecordDevice();
    }

    public int getOptionVoiceRecordDevice() {
        if (!soundDisabled && !VoiceManager.voipDisabled) {
            int num_devices = javafmod.FMOD_System_GetRecordNumDrivers();

            for (int i = 0; i < num_devices; i++) {
                FMOD_DriverInfo info = new FMOD_DriverInfo();
                javafmod.FMOD_System_GetRecordDriverInfo(i, info);
                if (info.name.equals(this.getOptionVoiceRecordDeviceName())) {
                    return i + 1;
                }
            }

            return 0;
        } else {
            return 0;
        }
    }

    public void setOptionVoiceRecordDevice(int option) {
        if (!soundDisabled && !VoiceManager.voipDisabled) {
            if (option >= 1) {
                FMOD_DriverInfo info = new FMOD_DriverInfo();
                javafmod.FMOD_System_GetRecordDriverInfo(option - 1, info);
                this.optionVoiceRecordDeviceName.setValue(info.name);
                VoiceManager.instance.UpdateRecordDevice();
            }
        }
    }

    public int getMicVolumeIndicator() {
        return VoiceManager.instance.getMicVolumeIndicator();
    }

    public boolean getMicVolumeError() {
        return VoiceManager.instance.getMicVolumeError();
    }

    public boolean getServerVOIPEnable() {
        return VoiceManager.instance.getServerVOIPEnable();
    }

    public void setTestingMicrophone(boolean testing) {
        VoiceManager.instance.setTestingMicrophone(testing);
    }

    public int getOptionReloadDifficulty() {
        return 2;
    }

    public void setOptionReloadDifficulty(int d) {
        this.optionReloadDifficulty.setValue(d);
    }

    public boolean getOptionRackProgress() {
        return this.optionRackProgress.getValue();
    }

    public void setOptionRackProgress(boolean b) {
        this.optionRackProgress.setValue(b);
    }

    public int getOptionFontSize() {
        return this.optionFontSize.getValue();
    }

    public void setOptionFontSize(int size) {
        this.optionFontSize.setValue(size);
    }

    public int getOptionFontSizeReal() {
        int size = this.getOptionFontSize();
        if (size == 6) {
            int minHeight = 1080;
            int maxHeight = 2160;
            int divisions = 108;
            int scale = (int)PZMath.floor((getInstance().getScreenHeight() - 1080) / 108.0F) + 1;
            int scaleClamp = PZMath.clamp(scale, 0, 10);
            switch (scaleClamp) {
                case 0:
                    size = 1;
                    break;
                case 1:
                    size = 2;
                    break;
                case 2:
                case 3:
                    size = 3;
                    break;
                case 4:
                case 5:
                case 6:
                    size = 4;
                    break;
                case 7:
                case 8:
                case 9:
                case 10:
                    size = 5;
            }
        }

        return size;
    }

    public int getOptionMoodleSize() {
        return this.optionMoodleSize.getValue();
    }

    public void setOptionMoodleSize(int size) {
        this.optionMoodleSize.setValue(size);
    }

    public int getOptionSidebarSize() {
        return this.optionSidebarSize.getValue();
    }

    public void setOptionSidebarSize(int size) {
        this.optionSidebarSize.setValue(size);
    }

    public int getOptionActionProgressBarSize() {
        return this.optionActionProgressBarSize.getValue();
    }

    public void setOptionActionProgressBarSize(int size) {
        this.optionActionProgressBarSize.setValue(size);
    }

    public String getOptionContextMenuFont() {
        return this.optionContextMenuFont.getValue();
    }

    public void setOptionContextMenuFont(String font) {
        this.optionContextMenuFont.setValue(font);
    }

    public String getOptionCodeFontSize() {
        return this.optionCodeFontSize.getValue();
    }

    public void setOptionCodeFontSize(String font) {
        this.optionCodeFontSize.setValue(font);
        TextManager var10000 = TextManager.instance;

        var10000.currentCodeFont = switch (font) {
            case "Small" -> UIFont.CodeSmall;
            case "Medium" -> UIFont.CodeMedium;
            case "Large" -> UIFont.CodeLarge;
            default -> UIFont.CodeSmall;
        };
    }

    public String getOptionInventoryFont() {
        return this.optionInventoryFont.getValue();
    }

    public void setOptionInventoryFont(String font) {
        this.optionInventoryFont.setValue(font);
    }

    public int getOptionInventoryContainerSize() {
        return this.optionInventoryContainerSize.getValue();
    }

    public void setOptionInventoryContainerSize(int size) {
        this.optionInventoryContainerSize.setValue(size);
    }

    public String getOptionTooltipFont() {
        return this.optionTooltipFont.getValue();
    }

    public void setOptionTooltipFont(String font) {
        this.optionTooltipFont.setValue(font);
        ObjectTooltip.checkFont();
    }

    public String getOptionMeasurementFormat() {
        return this.optionMeasurementFormat.getValue();
    }

    public void setOptionMeasurementFormat(String format) {
        this.optionMeasurementFormat.setValue(format);
    }

    public int getOptionClockFormat() {
        return this.optionClockFormat.getValue();
    }

    public int getOptionClockSize() {
        return this.optionClockSize.getValue();
    }

    public void setOptionClockFormat(int fmt) {
        this.optionClockFormat.setValue(fmt);
    }

    public void setOptionClockSize(int size) {
        this.optionClockSize.setValue(size);
    }

    public boolean getOptionClock24Hour() {
        return this.optionClock24Hour.getValue();
    }

    public void setOptionClock24Hour(boolean b24Hour) {
        this.optionClock24Hour.setValue(b24Hour);
    }

    public boolean getOptionModsEnabled() {
        return optionModsEnabled;
    }

    public void setOptionModsEnabled(boolean enabled) {
        optionModsEnabled = enabled;
    }

    public int getOptionBloodDecals() {
        return this.optionBloodDecals.getValue();
    }

    public void setOptionBloodDecals(int n) {
        this.optionBloodDecals.setValue(n);
    }

    public boolean getOptionFocusloss() {
        return this.optionFocusloss.getValue();
    }

    public void setOptionFocusloss(boolean pause) {
        this.optionFocusloss.setValue(pause);
    }

    public boolean getOptionBorderlessWindow() {
        return this.optionBorderlessWindow.getValue();
    }

    public void setOptionBorderlessWindow(boolean b) {
        this.optionBorderlessWindow.setValue(b);
    }

    public boolean getOptionLockCursorToWindow() {
        return this.optionLockCursorToWindow.getValue();
    }

    public void setOptionLockCursorToWindow(boolean b) {
        this.optionLockCursorToWindow.setValue(b);
    }

    public boolean allowOptionTextureCompression() {
        return RenderThread.invokeQueryOnRenderContext(() -> Display.capabilities.OpenGL33 || !System.getProperty("os.name").contains("OS X"));
    }

    public boolean getOptionTextureCompression() {
        return this.allowOptionTextureCompression() && this.optionTextureCompression.getValue();
    }

    public void setOptionTextureCompression(boolean b) {
        this.optionTextureCompression.setValue(b);
    }

    public boolean getOptionTexture2x() {
        return true;
    }

    public void setOptionTexture2x(boolean b) {
        this.optionTexture2x.setValue(b);
        DebugLog.General.warn("1x textures are disabled.");
    }

    public boolean getOptionHighResPlacedItems() {
        return this.optionHighResPlacedItems.getValue();
    }

    public void setOptionHighResPlacedItems(boolean b) {
        this.optionHighResPlacedItems.setValue(b);
    }

    public int getOptionMaxTextureSize() {
        return this.optionMaxTextureSize.getValue();
    }

    public void setOptionMaxTextureSize(int v) {
        this.optionMaxTextureSize.setValue(v);
    }

    public int getOptionMaxVehicleTextureSize() {
        return this.optionMaxVehicleTextureSize.getValue();
    }

    public void setOptionMaxVehicleTextureSize(int v) {
        this.optionMaxVehicleTextureSize.setValue(v);
    }

    public int getMaxTextureSizeFromFlags(int flags) {
        if ((flags & 128) != 0) {
            return this.getMaxTextureSize();
        } else {
            return (flags & 256) != 0 ? this.getMaxVehicleTextureSize() : 32768;
        }
    }

    public int getMaxTextureSizeFromOption(int option) {
        return switch (option) {
            case 1 -> 256;
            case 2 -> 512;
            case 3 -> 1024;
            case 4 -> 2048;
            default -> throw new IllegalStateException("Unexpected value: " + option);
        };
    }

    public int getMaxTextureSize() {
        return this.getMaxTextureSizeFromOption(this.optionMaxTextureSize.getValue());
    }

    public int getMaxVehicleTextureSize() {
        return this.getMaxTextureSizeFromOption(this.optionMaxVehicleTextureSize.getValue());
    }

    public boolean getOptionModelTextureMipmaps() {
        return this.optionModelTextureMipmaps.getValue();
    }

    public void setOptionModelTextureMipmaps(boolean b) {
        this.optionModelTextureMipmaps.setValue(b);
    }

    public String getOptionZoomLevels1x() {
        return this.optionZoomLevels1x.getValueAsString();
    }

    public void setOptionZoomLevels1x(String levels) {
        this.optionZoomLevels1x.parse(levels == null ? "" : levels);
    }

    public String getOptionZoomLevels2x() {
        return this.optionZoomLevels2x.getValueAsString();
    }

    public void setOptionZoomLevels2x(String levels) {
        this.optionZoomLevels2x.parse(levels == null ? "" : levels);
    }

    public ArrayList<Integer> getDefaultZoomLevels() {
        return this.offscreenBuffer.getDefaultZoomLevels();
    }

    public String getOptionScreenFilter() {
        return this.optionScreenFilter.getValue();
    }

    public void setOptionScreenFilter(String value) {
        this.optionScreenFilter.parse(value == null ? "" : value);
        this.screenFilter = "linear".equalsIgnoreCase(this.optionScreenFilter.getValue()) ? 9729 : 9728;
    }

    public int getScreenFilter() {
        return this.screenFilter;
    }

    public void setOptionActiveController(int controllerIndex, boolean active) {
        if (controllerIndex >= 0 && controllerIndex < GameWindow.GameInput.getControllerCount()) {
            Controller controller = GameWindow.GameInput.getController(controllerIndex);
            if (controller != null) {
                JoypadManager.instance.setControllerActive(controller.getGUID(), active);
            }
        }
    }

    public boolean getOptionActiveController(String guid) {
        return JoypadManager.instance.activeControllerGuids.contains(guid);
    }

    public boolean isOptionShowChatTimestamp() {
        return this.optionShowChatTimestamp.getValue();
    }

    public void setOptionShowChatTimestamp(boolean optionShowChatTimestamp) {
        this.optionShowChatTimestamp.setValue(optionShowChatTimestamp);
    }

    public boolean isOptionShowChatTitle() {
        return this.optionShowChatTitle.getValue();
    }

    public String getOptionChatFontSize() {
        return this.optionChatFontSize.getValue();
    }

    public void setOptionChatFontSize(String optionChatFontSize) {
        this.optionChatFontSize.setValue(optionChatFontSize);
    }

    public void setOptionShowChatTitle(boolean optionShowChatTitle) {
        this.optionShowChatTitle.setValue(optionShowChatTitle);
    }

    public float getOptionMinChatOpaque() {
        return (float)this.optionMinChatOpaque.getValue();
    }

    public void setOptionMinChatOpaque(float optionMinChatOpaque) {
        this.optionMinChatOpaque.setValue(optionMinChatOpaque);
    }

    public float getOptionMaxChatOpaque() {
        return (float)this.optionMaxChatOpaque.getValue();
    }

    public void setOptionMaxChatOpaque(float optionMaxChatOpaque) {
        this.optionMaxChatOpaque.setValue(optionMaxChatOpaque);
    }

    public float getOptionChatFadeTime() {
        return (float)this.optionChatFadeTime.getValue();
    }

    public void setOptionChatFadeTime(float optionChatFadeTime) {
        this.optionChatFadeTime.setValue(optionChatFadeTime);
    }

    public boolean getOptionChatOpaqueOnFocus() {
        return this.optionChatOpaqueOnFocus.getValue();
    }

    public void setOptionChatOpaqueOnFocus(boolean optionChatOpaqueOnFocus) {
        this.optionChatOpaqueOnFocus.setValue(optionChatOpaqueOnFocus);
    }

    public boolean getOptionTemperatureDisplayCelsius() {
        return this.optionTemperatureDisplayCelsius.getValue();
    }

    public boolean getOptionUIFBO() {
        return this.optionUiFbo.getValue();
    }

    public void setOptionUIFBO(boolean use) {
        this.optionUiFbo.setValue(use);
        if (GameWindow.states.current == IngameState.instance) {
            UIManager.useUiFbo = getInstance().supportsFBO() && this.getOptionUIFBO();
        }
    }

    public boolean getOptionMeleeOutline() {
        return this.optionMeleeOutline.getValue();
    }

    public void setOptionMeleeOutline(boolean toggle) {
        this.optionMeleeOutline.setValue(toggle);
    }

    public int getOptionUIRenderFPS() {
        return this.optionUiRenderFps.getValue();
    }

    public void setOptionUIRenderFPS(int fps) {
        this.optionUiRenderFps.setValue(fps);
    }

    public void setOptionRadialMenuKeyToggle(boolean toggle) {
        this.optionRadialMenuKeyToggle.setValue(toggle);
    }

    public boolean getOptionRadialMenuKeyToggle() {
        return this.optionRadialMenuKeyToggle.getValue();
    }

    public void setOptionReloadRadialInstant(boolean enable) {
        this.optionReloadRadialInstant.setValue(enable);
    }

    public boolean getOptionReloadRadialInstant() {
        return this.optionReloadRadialInstant.getValue();
    }

    public void setOptionPanCameraWhileAiming(boolean enable) {
        this.optionPanCameraWhileAiming.setValue(enable);
    }

    public boolean getOptionPanCameraWhileAiming() {
        return this.optionPanCameraWhileAiming.getValue();
    }

    public void setOptionPanCameraWhileDriving(boolean enable) {
        this.optionPanCameraWhileDriving.setValue(enable);
    }

    public boolean getOptionPanCameraWhileDriving() {
        return this.optionPanCameraWhileDriving.getValue();
    }

    public String getOptionCycleContainerKey() {
        return this.optionCycleContainerKey.getValue();
    }

    public void setOptionCycleContainerKey(String s) {
        this.optionCycleContainerKey.setValue(s);
    }

    public boolean getOptionDropItemsOnSquareCenter() {
        return this.optionDropItemsOnSquareCenter.getValue();
    }

    public void setOptionDropItemsOnSquareCenter(boolean b) {
        this.optionDropItemsOnSquareCenter.setValue(b);
    }

    public boolean getOptionTimedActionGameSpeedReset() {
        return this.optionTimedActionGameSpeedReset.getValue();
    }

    public void setOptionTimedActionGameSpeedReset(boolean b) {
        this.optionTimedActionGameSpeedReset.setValue(b);
    }

    public int getOptionShoulderButtonContainerSwitch() {
        return this.optionShoulderButtonContainerSwitch.getValue();
    }

    public void setOptionShoulderButtonContainerSwitch(int v) {
        this.optionShoulderButtonContainerSwitch.setValue(v);
    }

    public int getOptionControllerButtonStyle() {
        return this.optionControllerButtonStyle.getValue();
    }

    public void setOptionControllerButtonStyle(int v) {
        this.optionControllerButtonStyle.setValue(v);
    }

    public boolean getOptionSingleContextMenu(int playerIndex) {
        return ((BooleanConfigOption)this.optionSingleContextMenu.getElement(playerIndex)).getValue();
    }

    public void setOptionSingleContextMenu(int playerIndex, boolean b) {
        ((BooleanConfigOption)this.optionSingleContextMenu.getElement(playerIndex)).setValue(b);
    }

    public boolean getOptionAutoDrink() {
        return this.optionAutoDrink.getValue();
    }

    public void setOptionAutoDrink(boolean enable) {
        this.optionAutoDrink.setValue(enable);
    }

    public boolean getOptionAutoRevealPrintMediaMapLocations() {
        return this.optionAutoRevealPrintMediaMapLocations.getValue();
    }

    public void setOptionAutoRevealPrintMediaMapLocations(boolean enable) {
        this.optionAutoRevealPrintMediaMapLocations.setValue(enable);
    }

    public boolean getOptionAutoWalkContainer() {
        return this.optionAutoWalkContainer.getValue();
    }

    public void setOptionAutoWalkContainer(boolean enable) {
        this.optionAutoWalkContainer.setValue(enable);
    }

    public boolean getOptionCorpseShadows() {
        return this.optionCorpseShadows.getValue();
    }

    public void setOptionCorpseShadows(boolean enable) {
        this.optionCorpseShadows.setValue(enable);
    }

    public boolean getOptionLeaveKeyInIgnition() {
        return this.optionLeaveKeyInIgnition.getValue();
    }

    public void setOptionLeaveKeyInIgnition(boolean enable) {
        this.optionLeaveKeyInIgnition.setValue(enable);
    }

    public int getOptionSearchModeOverlayEffect() {
        return this.optionSearchModeOverlayEffect.getValue();
    }

    public void setOptionSearchModeOverlayEffect(int v) {
        this.optionSearchModeOverlayEffect.setValue(v);
    }

    public int getOptionSimpleClothingTextures() {
        return this.optionSimpleClothingTextures.getValue();
    }

    public void setOptionSimpleClothingTextures(int v) {
        this.optionSimpleClothingTextures.setValue(v);
    }

    public boolean isOptionSimpleClothingTextures(boolean bZombie) {
        switch (this.getOptionSimpleClothingTextures()) {
            case 1:
                return false;
            case 2:
                return bZombie;
            default:
                return true;
        }
    }

    public boolean getOptionSimpleWeaponTextures() {
        return this.optionSimpleWeaponTextures.getValue();
    }

    public void setOptionSimpleWeaponTextures(boolean enable) {
        this.optionSimpleWeaponTextures.setValue(enable);
    }

    public int getOptionIgnoreProneZombieRange() {
        return this.optionIgnoreProneZombieRange.getValue();
    }

    public void setOptionIgnoreProneZombieRange(int i) {
        this.optionIgnoreProneZombieRange.setValue(i);
    }

    public float getIgnoreProneZombieRange() {
        switch (this.optionIgnoreProneZombieRange.getValue()) {
            case 1:
                return -1.0F;
            case 2:
                return 1.5F;
            case 3:
                return 2.0F;
            case 4:
                return 2.5F;
            case 5:
                return 3.0F;
            default:
                return -1.0F;
        }
    }

    private void readPerPlayerBoolean(String str, boolean[] flags) {
        Arrays.fill(flags, false);
        String[] ss = str.split(",");

        for (int i = 0; i < ss.length && i != 4; i++) {
            flags[i] = StringUtils.tryParseBoolean(ss[i]);
        }
    }

    private String getPerPlayerBooleanString(boolean[] flags) {
        return String.format("%b,%b,%b,%b", flags[0], flags[1], flags[2], flags[3]);
    }

    @Deprecated
    public void ResetLua(boolean sp, String reason) throws IOException {
        this.ResetLua("default", reason);
    }

    public void ResetLua(String activeMods, String reason) throws IOException {
        if (SpriteRenderer.instance != null) {
            GameWindow.drawReloadingLua = true;
            GameWindow.render();
            GameWindow.drawReloadingLua = false;
        }

        RenderThread.setWaitForRenderState(false);
        SpriteRenderer.instance.notifyRenderStateQueue();
        RegistryReset.resetAll();
        ScriptManager.instance.Reset();
        ClothingDecals.Reset();
        BeardStyles.Reset();
        HairStyles.Reset();
        OutfitManager.Reset();
        AnimationSet.Reset();
        GameSounds.Reset();
        VehicleType.Reset();
        LuaEventManager.Reset();
        MapObjects.Reset();
        UIManager.init();
        SurvivorFactory.Reset();
        ChooseGameInfo.Reset();
        AttachedLocations.Reset();
        BodyLocations.reset();
        ContainerOverlays.instance.Reset();
        BentFences.getInstance().Reset();
        BrokenFences.getInstance().Reset();
        TileOverlays.instance.Reset();
        LuaHookManager.Reset();
        CustomPerks.Reset();
        PerkFactory.Reset();
        CustomSandboxOptions.Reset();
        SandboxOptions.Reset();
        WorldMap.Reset();
        AnimalDefinitions.Reset();
        AnimalZones.Reset();
        MigrationGroupDefinitions.Reset();
        LuaManager.init();
        JoypadManager.instance.Reset();
        GameKeyboard.doLuaKeyPressed = true;
        Texture.nullTextures.clear();
        NinePatchTexture.Reset();
        SpriteModelManager.getInstance().Reset();
        TileGeometryManager.getInstance().Reset();
        TileDepthTextureManager.getInstance().Reset();
        SeamManager.getInstance().Reset();
        SeatingManager.getInstance().Reset();
        ZomboidFileSystem.instance.Reset();
        ZomboidFileSystem.instance.init();
        ZomboidFileSystem.instance.loadMods(activeMods);
        ZomboidFileSystem.instance.loadModPackFiles();
        Languages.instance.init();
        Translator.loadFiles();
        CustomPerks.instance.init();
        CustomPerks.instance.initLua();
        CustomSandboxOptions.instance.init();
        CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
        ModRegistries.init();
        ScriptManager.instance.Load();
        SpriteModelManager.getInstance().init();
        ModelManager.instance.initAnimationMeshes(true);
        ModelManager.instance.loadModAnimations();
        ClothingDecals.init();
        BeardStyles.init();
        HairStyles.init();
        OutfitManager.init();
        VoiceStyles.init();
        TileGeometryManager.getInstance().init();
        TileDepthTextureAssignmentManager.getInstance().init();
        TileDepthTextureManager.getInstance().init();
        SeamManager.getInstance().init();
        SeatingManager.getInstance().init();

        try {
            TextManager.instance.Init();
            LuaManager.LoadDirBase();
        } catch (Exception var6) {
            ExceptionLogger.logException(var6);
            GameWindow.DoLoadingText("Reloading Lua - ERRORS!");

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException var5) {
            }
        }

        ZomboidGlobals.Load();
        RenderThread.setWaitForRenderState(true);
        LuaEventManager.triggerEvent("OnGameBoot");
        LuaEventManager.triggerEvent("OnMainMenuEnter");
        LuaEventManager.triggerEvent("OnResetLua", reason);
    }

    public void DelayResetLua(String activeMods, String reason) {
        this.delayResetLuaActiveMods = activeMods;
        this.delayResetLuaReason = reason;
    }

    public void CheckDelayResetLua() throws IOException {
        if (this.delayResetLuaActiveMods != null) {
            String activeMods = this.delayResetLuaActiveMods;
            String reason = this.delayResetLuaReason;
            this.delayResetLuaActiveMods = null;
            this.delayResetLuaReason = null;
            this.ResetLua(activeMods, reason);
        }
    }

    public boolean isShowPing() {
        return this.showPing;
    }

    public void setShowPing(boolean showPing) {
        this.showPing = showPing;
    }

    public boolean isForceSnow() {
        return this.forceSnow;
    }

    public void setForceSnow(boolean forceSnow) {
        this.forceSnow = forceSnow;
    }

    public boolean isZombieGroupSound() {
        return this.zombieGroupSound;
    }

    public void setZombieGroupSound(boolean zombieGroupSound) {
        this.zombieGroupSound = zombieGroupSound;
    }

    public String getBlinkingMoodle() {
        return this.blinkingMoodle;
    }

    public void setBlinkingMoodle(String blinkingMoodle) {
        this.blinkingMoodle = blinkingMoodle;
    }

    public boolean isTutorialDone() {
        return this.optionTutorialDone.getValue();
    }

    public void setTutorialDone(boolean done) {
        this.optionTutorialDone.setValue(done);
    }

    public boolean isVehiclesWarningShow() {
        return this.optionVehiclesWarningShow.getValue();
    }

    public void setVehiclesWarningShow(boolean done) {
        this.optionVehiclesWarningShow.setValue(done);
    }

    public void initPoisonousBerry() {
        ArrayList<String> berriesList = new ArrayList<>();
        berriesList.add("Base.BerryGeneric1");
        berriesList.add("Base.BerryGeneric2");
        berriesList.add("Base.BerryGeneric3");
        berriesList.add("Base.BerryGeneric4");
        berriesList.add("Base.BerryGeneric5");
        berriesList.add("Base.BerryPoisonIvy");
        this.setPoisonousBerry(berriesList.get(Rand.Next(0, berriesList.size() - 1)));
    }

    public void initPoisonousMushroom() {
        ArrayList<String> mushroomList = new ArrayList<>();
        mushroomList.add("Base.MushroomGeneric1");
        mushroomList.add("Base.MushroomGeneric2");
        mushroomList.add("Base.MushroomGeneric3");
        mushroomList.add("Base.MushroomGeneric4");
        mushroomList.add("Base.MushroomGeneric5");
        mushroomList.add("Base.MushroomGeneric6");
        mushroomList.add("Base.MushroomGeneric7");
        this.setPoisonousMushroom(mushroomList.get(Rand.Next(0, mushroomList.size() - 1)));
    }

    public String getPoisonousBerry() {
        return this.poisonousBerry;
    }

    public void setPoisonousBerry(String poisonousBerry) {
        this.poisonousBerry = poisonousBerry;
    }

    public String getPoisonousMushroom() {
        return this.poisonousMushroom;
    }

    public void setPoisonousMushroom(String poisonousMushroom) {
        this.poisonousMushroom = poisonousMushroom;
    }

    public static String getDifficulty() {
        return difficulty;
    }

    public static void setDifficulty(String vdifficulty) {
        difficulty = vdifficulty;
    }

    public boolean isDoneNewSaveFolder() {
        return this.optionDoneNewSaveFolder.getValue();
    }

    public void setDoneNewSaveFolder(boolean doneNewSaveFolder) {
        this.optionDoneNewSaveFolder.setValue(doneNewSaveFolder);
    }

    public static int getTileScale() {
        return tileScale;
    }

    public boolean isSelectingAll() {
        return this.isSelectingAll;
    }

    public void setIsSelectingAll(boolean isSelectingAll) {
        this.isSelectingAll = isSelectingAll;
    }

    public boolean getContentTranslationsEnabled() {
        return this.optionEnableContentTranslations.getValue();
    }

    public void setContentTranslationsEnabled(boolean b) {
        this.optionEnableContentTranslations.setValue(b);
    }

    public boolean isShowYourUsername() {
        return this.showYourUsername.getValue();
    }

    public void setShowYourUsername(boolean showYourUsername) {
        this.showYourUsername.setValue(showYourUsername);
    }

    public ColorInfo getMpTextColor() {
        if (this.mpTextColor == null) {
            this.mpTextColor = new ColorInfo((Rand.Next(135) + 120) / 255.0F, (Rand.Next(135) + 120) / 255.0F, (Rand.Next(135) + 120) / 255.0F, 1.0F);
            this.optionMpTextColor.setValueVarArgs((double)this.mpTextColor.r, (double)this.mpTextColor.g, (double)this.mpTextColor.b);
        }

        return this.optionMpTextColor.getValueAsColorInfo(this.mpTextColor);
    }

    public void setMpTextColor(ColorInfo mpTextColor) {
        this.mpTextColor = mpTextColor;
        this.optionMpTextColor.setValueVarArgs((double)mpTextColor.r, (double)mpTextColor.g, (double)mpTextColor.b);
    }

    public boolean isAzerty() {
        return this.isAzerty;
    }

    public void setAzerty(boolean isAzerty) {
        this.isAzerty = isAzerty;
    }

    public ColorInfo getObjectHighlitedColor() {
        return this.optionObjectHighlightColor.getValueAsColorInfo(this.objectHighlitedColor);
    }

    public void setObjectHighlitedColor(ColorInfo objectHighlitedColor) {
        this.optionObjectHighlightColor.setValueVarArgs((double)objectHighlitedColor.r, (double)objectHighlitedColor.g, (double)objectHighlitedColor.b);
        this.objectHighlitedColor.set(objectHighlitedColor);
    }

    public ColorInfo getWorldItemHighlightColor() {
        return this.optionWorldItemHighlightColor.getValueAsColorInfo(this.worldItemHighlightColor);
    }

    public void setWorldItemHighlightColor(ColorInfo colorInfo) {
        this.optionWorldItemHighlightColor.setValueVarArgs((double)colorInfo.r, (double)colorInfo.g, (double)colorInfo.b);
        this.worldItemHighlightColor.set(colorInfo);
    }

    public ColorInfo getGoodHighlitedColor() {
        return this.optionGoodHighlightColor.getValueAsColorInfo(this.goodHighlitedColor);
    }

    public void setGoodHighlitedColor(ColorInfo GoodHighlitedColor) {
        this.optionGoodHighlightColor.setValueVarArgs((double)GoodHighlitedColor.r, (double)GoodHighlitedColor.g, (double)GoodHighlitedColor.b);
        this.goodHighlitedColor.set(GoodHighlitedColor);
    }

    public ColorInfo getBadHighlitedColor() {
        return this.optionBadHighlightColor.getValueAsColorInfo(this.badHighlitedColor);
    }

    public void setBadHighlitedColor(ColorInfo BadHighlitedColor) {
        this.optionBadHighlightColor.setValueVarArgs((double)BadHighlitedColor.r, (double)BadHighlitedColor.g, (double)BadHighlitedColor.b);
        this.badHighlitedColor.set(BadHighlitedColor);
    }

    public boolean getOptionColorblindPatterns() {
        return this.optionColorblindPatterns.getValue();
    }

    public void setOptionColorblindPatterns(boolean enable) {
        this.optionColorblindPatterns.setValue(enable);
    }

    public boolean getOptionEnableDyslexicFont() {
        return this.optionEnableDyslexicFont.getValue();
    }

    public void setOptionEnableDyslexicFont(boolean enable) {
        this.optionEnableDyslexicFont.setValue(enable);
    }

    public boolean getOptionLightSensitivity() {
        return this.optionLightSensitivity.getValue();
    }

    public void setOptionLightSensitivity(boolean enable) {
        this.optionLightSensitivity.setValue(enable);
    }

    public String getSeenUpdateText() {
        return this.seenUpdateText.getValue();
    }

    public void setSeenUpdateText(String seenUpdateText) {
        this.seenUpdateText.setValue(seenUpdateText);
    }

    public boolean isToggleToAim() {
        return this.toggleToAim.getValue();
    }

    public void setToggleToAim(boolean enable) {
        this.toggleToAim.setValue(enable);
    }

    public boolean isToggleToRun() {
        return this.toggleToRun.getValue();
    }

    public void setToggleToRun(boolean toggleToRun) {
        this.toggleToRun.setValue(toggleToRun);
    }

    public int getXAngle(int width, float angle) {
        double radian = Math.toRadians(225.0F + angle);
        return Long.valueOf(Math.round((Math.sqrt(2.0) * Math.cos(radian) + 1.0) * (width / 2))).intValue();
    }

    public int getYAngle(int width, float angle) {
        double radian = Math.toRadians(225.0F + angle);
        return Long.valueOf(Math.round((Math.sqrt(2.0) * Math.sin(radian) + 1.0) * (width / 2))).intValue();
    }

    public boolean isCelsius() {
        return this.celsius.getValue();
    }

    public void setCelsius(boolean celsius) {
        this.celsius.setValue(celsius);
    }

    public boolean isInDebug() {
        return debug;
    }

    public boolean isRiversideDone() {
        return this.optionRiversideDone.getValue();
    }

    public void setRiversideDone(boolean riversideDone) {
        this.optionRiversideDone.setValue(riversideDone);
    }

    public boolean isNoSave() {
        return this.noSave;
    }

    public void setNoSave(boolean noSave) {
        this.noSave = noSave;
    }

    public boolean isShowFirstTimeVehicleTutorial() {
        return this.showFirstTimeVehicleTutorial;
    }

    public void setShowFirstTimeVehicleTutorial(boolean showFirstTimeVehicleTutorial) {
        this.showFirstTimeVehicleTutorial = showFirstTimeVehicleTutorial;
    }

    public boolean getOptionDisplayAsCelsius() {
        return this.optionTemperatureDisplayCelsius.getValue();
    }

    public void setOptionDisplayAsCelsius(boolean b) {
        this.optionTemperatureDisplayCelsius.setValue(b);
    }

    public boolean isShowFirstTimeWeatherTutorial() {
        return this.showFirstTimeWeatherTutorial;
    }

    public void setShowFirstTimeWeatherTutorial(boolean showFirstTimeWeatherTutorial) {
        this.showFirstTimeWeatherTutorial = showFirstTimeWeatherTutorial;
    }

    public boolean getOptionDoVideoEffects() {
        return this.optionDoVideoEffects.getValue();
    }

    public void setOptionDoVideoEffects(boolean b) {
        this.optionDoVideoEffects.setValue(b);
    }

    public boolean getOptionDoWindSpriteEffects() {
        return this.optionDoWindSpriteEffects.getValue();
    }

    public void setOptionDoWindSpriteEffects(boolean b) {
        this.optionDoWindSpriteEffects.setValue(b);
    }

    public boolean getOptionDoDoorSpriteEffects() {
        return this.optionDoDoorSpriteEffects.getValue();
    }

    public void setOptionDoDoorSpriteEffects(boolean b) {
        this.optionDoDoorSpriteEffects.setValue(b);
    }

    public boolean getOptionDoContainerOutline() {
        return this.optionDoContainerOutline.getValue();
    }

    public void setOptionDoContainerOutline(boolean b) {
        this.optionDoContainerOutline.setValue(b);
    }

    public void setOptionUpdateSneakButton(boolean b) {
        this.optionUpdateSneakButton.setValue(b);
    }

    public boolean getOptionUpdateSneakButton() {
        return this.optionUpdateSneakButton.getValue();
    }

    public boolean isShowFirstTimeSneakTutorial() {
        return this.optionShowFirstTimeSneakTutorial.getValue();
    }

    public void setShowFirstTimeSneakTutorial(boolean showFirstTimeSneakTutorial) {
        this.optionShowFirstTimeSneakTutorial.setValue(showFirstTimeSneakTutorial);
    }

    public double getShownWelcomeMessageVersion() {
        return this.optionShownWelcomeMessageVersion.getValue();
    }

    public void setShownWelcomeMessageVersion(double value) {
        this.optionShownWelcomeMessageVersion.setValue(value);
    }

    public boolean isShowFirstTimeSearchTutorial() {
        return this.optionShowFirstTimeSearchTutorial.getValue();
    }

    public void setShowFirstTimeSearchTutorial(boolean showFirstTimeSearchTutorial) {
        this.optionShowFirstTimeSearchTutorial.setValue(showFirstTimeSearchTutorial);
    }

    public int getTermsOfServiceVersion() {
        return this.optionTermsOfServiceVersion.getValue();
    }

    public void setTermsOfServiceVersion(int v) {
        this.optionTermsOfServiceVersion.setValue(v);
    }

    public void setOptiondblTapJogToSprint(boolean dbltap) {
        this.optionDblTapJogToSprint.setValue(dbltap);
    }

    public boolean isOptiondblTapJogToSprint() {
        return this.optionDblTapJogToSprint.getValue();
    }

    public boolean isToggleToSprint() {
        return this.toggleToSprint.getValue();
    }

    public void setToggleToSprint(boolean toggleToSprint) {
        this.toggleToSprint.setValue(toggleToSprint);
    }

    public int getIsoCursorVisibility() {
        return this.isoCursorVisibility.getValue();
    }

    public void setIsoCursorVisibility(int isoCursorVisibility) {
        this.isoCursorVisibility.setValue(isoCursorVisibility);
    }

    public boolean getOptionShowCursorWhileAiming() {
        return this.optionShowCursorWhileAiming.getValue();
    }

    public void setOptionShowCursorWhileAiming(boolean show) {
        this.optionShowCursorWhileAiming.setValue(show);
    }

    public boolean gotNewBelt() {
        return this.optionGotNewBelt.getValue();
    }

    public void setGotNewBelt(boolean gotit) {
        this.optionGotNewBelt.setValue(gotit);
    }

    public void setAnimPopupDone(boolean done) {
        this.animPopupDone = done;
    }

    public boolean isAnimPopupDone() {
        return this.animPopupDone;
    }

    public void setModsPopupDone(boolean done) {
        this.modsPopupDone = done;
    }

    public boolean isModsPopupDone() {
        return this.modsPopupDone;
    }

    public boolean isRenderPrecipIndoors() {
        return this.optionRenderPrecipIndoors.getValue();
    }

    public void setRenderPrecipIndoors(boolean optionRenderPrecipIndoors) {
        this.optionRenderPrecipIndoors.setValue(optionRenderPrecipIndoors);
    }

    public float getOptionPrecipitationSpeedMultiplier() {
        return (float)this.optionPrecipitationSpeedMultiplier.getValue();
    }

    public void setOptionPrecipitationSpeedMultiplier(float f) {
        this.optionPrecipitationSpeedMultiplier.setValue(f);
    }

    public boolean isCollideZombies() {
        return this.collideZombies;
    }

    public void setCollideZombies(boolean collideZombies) {
        this.collideZombies = collideZombies;
    }

    public boolean isFlashIsoCursor() {
        return this.flashIsoCursor;
    }

    public void setFlashIsoCursor(boolean flashIsoCursor) {
        this.flashIsoCursor = flashIsoCursor;
    }

    public boolean isOptionProgressBar() {
        return true;
    }

    public void setOptionProgressBar(boolean optionProgressBar) {
        this.optionProgressBar.setValue(optionProgressBar);
    }

    public void setOptionLanguageName(String name) {
        this.optionLanguageName.setValue(name);
    }

    public String getOptionLanguageName() {
        return this.optionLanguageName.getValue();
    }

    public int getOptionRenderPrecipitation() {
        return this.optionRenderPrecipitation.getValue();
    }

    public void setOptionRenderPrecipitation(int optionRenderPrecipitation) {
        this.optionRenderPrecipitation.setValue(optionRenderPrecipitation);
    }

    public void setOptionAutoProneAtk(boolean optionAutoProneAtk) {
        this.optionAutoProneAtk.setValue(optionAutoProneAtk);
    }

    public boolean isOptionAutoProneAtk() {
        return this.optionAutoProneAtk.getValue();
    }

    public void setOption3DGroundItem(boolean option3Dgrounditem) {
        this.option3dGroundItem.setValue(option3Dgrounditem);
    }

    public boolean isOption3DGroundItem() {
        return this.option3dGroundItem.getValue();
    }

    public Object getOptionOnStartup(String name) {
        return optionsOnStartup.get(name);
    }

    public void setOptionOnStartup(String name, Object value) {
        optionsOnStartup.put(name, value);
    }

    public void countMissing3DItems() {
        ArrayList<Item> itemList = ScriptManager.instance.getAllItems();
        int missing = 0;

        for (Item item : itemList) {
            if (!item.isItemType(ItemType.WEAPON)
                && !item.isItemType(ItemType.MOVEABLE)
                && !item.name.contains("ZedDmg")
                && !item.name.contains("Wound")
                && !item.name.contains("MakeUp")
                && !item.name.contains("Bandage")
                && !item.name.contains("Hat")
                && !item.getObsolete()
                && StringUtils.isNullOrEmpty(item.worldObjectSprite)
                && StringUtils.isNullOrEmpty(item.worldStaticModel)) {
                System.out.println("Missing: " + item.name);
                missing++;
            }
        }

        System.out.println("total missing: " + missing + "/" + itemList.size());
    }

    public boolean getOptionShowItemModInfo() {
        return this.optionShowItemModInfo.getValue();
    }

    public void setOptionShowItemModInfo(boolean b) {
        this.optionShowItemModInfo.setValue(b);
    }

    public boolean getOptionShowCraftingXP() {
        return this.optionShowCraftingXp.getValue();
    }

    public void setOptionShowCraftingXP(boolean b) {
        this.optionShowCraftingXp.setValue(b);
    }

    public boolean getOptionShowSurvivalGuide() {
        return this.optionShowSurvivalGuide.getValue();
    }

    public void setOptionShowSurvivalGuide(boolean b) {
        this.optionShowSurvivalGuide.setValue(b);
    }

    public boolean getOptionShowFirstAnimalZoneInfo() {
        return this.optionShowFirstAnimalZoneInfo.getValue();
    }

    public void setOptionShowFirstAnimalZoneInfo(boolean b) {
        this.optionShowFirstAnimalZoneInfo.setValue(b);
    }

    public boolean getOptionEnableLeftJoystickRadialMenu() {
        return this.optionEnableLeftJoystickRadialMenu.getValue();
    }

    public void setOptionEnableLeftJoystickRadialMenu(boolean b) {
        this.optionEnableLeftJoystickRadialMenu.setValue(b);
    }

    public boolean getOptionMacOSIgnoreMouseWheelAcceleration() {
        return this.optionMacosIgnoreMouseWheelAcceleration.getValue();
    }

    public void setOptionMacOSIgnoreMouseWheelAcceleration(boolean b) {
        this.optionMacosIgnoreMouseWheelAcceleration.setValue(b);
    }

    public boolean getOptionMacOSMapHorizontalMouseWheelToVertical() {
        return this.optionMacosMapHorizontalMouseWheelToVertical.getValue();
    }

    public void setOptionMacOSMapHorizontalMouseWheelToVertical(boolean b) {
        this.optionMacosMapHorizontalMouseWheelToVertical.setValue(b);
    }

    public String getVersionNumber() {
        return gameVersion.toString();
    }

    public void setAnimalCheat(boolean cheat) {
        this.animalCheat = cheat;
    }

    public void setDisplayPlayerModel(boolean display) {
        this.displayPlayerModel = display;
    }

    public boolean isDisplayPlayerModel() {
        return this.displayPlayerModel;
    }

    public void setDisplayCursor(boolean display) {
        this.displayCursor = display;
    }

    public boolean isDisplayCursor() {
        return this.displayCursor;
    }

    public boolean getOptionShowAimTexture() {
        return this.optionShowAimTexture.getValue();
    }

    public void setOptionShowAimTexture(boolean show) {
        this.optionShowAimTexture.setValue(show);
    }

    public boolean getOptionShowReticleTexture() {
        return this.optionShowReticleTexture.getValue();
    }

    public void setOptionShowReticleTexture(boolean show) {
        this.optionShowReticleTexture.setValue(show);
    }

    public boolean getOptionShowValidTargetReticleTexture() {
        return this.optionShowValidTargetReticleTexture.getValue();
    }

    public void setOptionShowValidTargetReticleTexture(boolean show) {
        this.optionShowValidTargetReticleTexture.setValue(show);
    }

    public int getOptionReticleMode() {
        return this.optionReticleMode.getValue();
    }

    public void setOptionReticleMode(int mode) {
        this.optionReticleMode.setValue(mode);
    }

    public void setOptionAimTextureIndex(int index) {
        this.optionAimTextureIndex.setValue(index);
    }

    public int getOptionAimTextureIndex() {
        return this.optionAimTextureIndex.getValue();
    }

    public void setOptionReticleTextureIndex(int index) {
        this.optionReticleTextureIndex.setValue(index);
    }

    public int getOptionReticleTextureIndex() {
        return this.optionReticleTextureIndex.getValue();
    }

    public void setOptionValidTargetReticleTextureIndex(int index) {
        this.optionValidTargetReticleTextureIndex.setValue(index);
    }

    public int getOptionValidTargetReticleTextureIndex() {
        return this.optionValidTargetReticleTextureIndex.getValue();
    }

    public void setOptionCrosshairTextureIndex(int index) {
        this.optionCrosshairTextureIndex.setValue(index);
    }

    public int getOptionCrosshairTextureIndex() {
        return this.optionCrosshairTextureIndex.getValue();
    }

    public ColorInfo getTargetColor() {
        return this.optionTargetColor.getValueAsColorInfo(this.targetColor);
    }

    public void setTargetColor(ColorInfo colorInfo) {
        this.optionTargetColor.setValueVarArgs((double)colorInfo.r, (double)colorInfo.g, (double)colorInfo.b);
        this.targetColor.set(colorInfo);
    }

    public ColorInfo getNoTargetColor() {
        return this.optionNoTargetColor.getValueAsColorInfo(this.noTargetColor);
    }

    public void setNoTargetColor(ColorInfo colorInfo) {
        this.optionNoTargetColor.setValueVarArgs((double)colorInfo.r, (double)colorInfo.g, (double)colorInfo.b);
        this.noTargetColor.set(colorInfo);
    }

    public int getOptionMaxCrosshairOffset() {
        return this.optionMaxCrosshairOffset.getValue();
    }

    public void setOptionMaxCrosshairOffset(int maxCrosshairOffset) {
        this.optionMaxCrosshairOffset.setValue(maxCrosshairOffset);
    }

    public boolean getOptionReticleCameraZoom() {
        return this.optionReticleCameraZoom.getValue();
    }

    public void setOptionReticleCameraZoom(boolean optionReticleCameraZoom) {
        this.optionReticleCameraZoom.setValue(optionReticleCameraZoom);
    }

    public float getIsoCursorAlpha() {
        float alpha = 0.05F;
        switch (this.isoCursorVisibility.getValue()) {
            case 0:
                alpha = 0.0F;
                break;
            case 1:
                alpha = 0.05F;
                break;
            case 2:
                alpha = 0.1F;
                break;
            case 3:
                alpha = 0.15F;
                break;
            case 4:
                alpha = 0.3F;
                break;
            case 5:
                alpha = 0.5F;
                break;
            case 6:
                alpha = 0.75F;
        }

        return alpha;
    }

    public String debugOutputMissingItemSpawn() throws Exception {
        return this.debugOutputMissingSpawn("../workdir/media/scripts/items/", "item");
    }

    public String debugOutputMissingCLothingSpawn() throws Exception {
        return this.debugOutputMissingSpawn("../workdir/media/scripts/clothing/", "clothing");
    }

    public String debugOutputMissingSpawn(String directory, String category) throws Exception {
        StringBuilder sb = new StringBuilder(category + " items that don't spawn:\r\n");
        File scriptFolder = new File(directory);
        if (!scriptFolder.isDirectory()) {
            sb.append("Couldn't find " + category + " dir.");
            return sb.toString();
        } else if (Objects.requireNonNull(scriptFolder.listFiles()).length == 0) {
            sb.append("No " + category + " script found.");
            return sb.toString();
        } else {
            ArrayList<String> clothings = getClothingStrings(scriptFolder);
            sb.append("Found ").append(clothings.size()).append(" " + category + "s ").append("\r\n");
            File spawnFolder = new File("../workdir/media/lua/server/Items/");
            if (!spawnFolder.isDirectory()) {
                sb.append("Couldn't find Items dir.");
                return sb.toString();
            } else if (Objects.requireNonNull(spawnFolder.listFiles()).length == 0) {
                sb.append("No spawn script found.");
                return sb.toString();
            } else {
                ArrayList<String> clothingsSpawn = getClothingSpawnString(spawnFolder);
                ArrayList<String> missingClothing = new ArrayList<>();

                for (int i = 0; i < clothings.size(); i++) {
                    String clothingName = clothings.get(i);
                    if (!clothingsSpawn.contains(clothingName)) {
                        missingClothing.add(clothingName);
                    }
                }

                File vehicleFolder = new File("../workdir/media/lua/server/Vehicles");
                if (!vehicleFolder.isDirectory()) {
                    sb.append("Couldn't find vehicle distribution dir.");
                    return sb.toString();
                } else if (Objects.requireNonNull(vehicleFolder.listFiles()).length == 0) {
                    sb.append("No vehicle distribution script found.");
                    return sb.toString();
                } else {
                    ArrayList<String> vehicleSpawn = getClothingSpawnString(vehicleFolder);

                    for (int ix = 0; ix < missingClothing.size(); ix++) {
                        String itemName = missingClothing.get(ix);
                        if (vehicleSpawn.contains(itemName)) {
                            sb.append(itemName).append(" only spawn in vehicle ").append("\r\n");
                        } else {
                            sb.append(itemName).append(" dont spawn ").append("\r\n");
                        }
                    }

                    System.out.println(sb);
                    File file = new File(category + "ItemsNotSpawning.txt");
                    file.createNewFile();
                    FileWriter fw = new FileWriter(file);
                    fw.write(sb.toString());
                    fw.flush();
                    fw.close();
                    return sb.toString();
                }
            }
        }
    }

    private static ArrayList<String> getClothingSpawnString(File scriptFolder) throws IOException {
        ArrayList<String> clothings = new ArrayList<>();
        int meh = 500;

        for (File file : Objects.requireNonNull(scriptFolder.listFiles())) {
            boolean isDistributionFile = false;
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {
                if (!isDistributionFile) {
                    if (--meh == 0) {
                        break;
                    }
                }

                if (line.trim().startsWith("items") || isDistributionFile || file.getName().contains("Junk")) {
                    isDistributionFile = true;
                    meh = 500;
                    if (line.trim().startsWith("\"") && line.trim().contains(",")) {
                        clothings.add(line.split("\"")[1]);
                    }
                }
            }
        }

        return clothings;
    }

    private static ArrayList<String> getClothingStrings(File scriptFolder) throws IOException {
        ArrayList<String> clothings = new ArrayList<>();

        for (File file : Objects.requireNonNull(scriptFolder.listFiles())) {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("item")) {
                    String itemName = line.trim().split("item ")[1];
                    if (!itemName.startsWith("Wound")
                        && !itemName.startsWith("Bandage")
                        && !itemName.startsWith("ZedDmg")
                        && !itemName.startsWith("MakeUp")
                        && !itemName.startsWith("Ring_Right")
                        && !itemName.startsWith("Ring_Left_MiddleFinger_")
                        && !itemName.startsWith("Animal_")
                        && !itemName.startsWith("WristWatch_Right_")
                        && !itemName.startsWith("Bracelet_Right")
                        && !itemName.startsWith("Berry")
                        && !itemName.startsWith("MushroomGeneric")
                        && !itemName.startsWith("Umbrella")
                        && !itemName.endsWith("DOWN")
                        && !itemName.endsWith("Reverse")
                        && !itemName.endsWith("Back")
                        && !itemName.endsWith("Right")
                        && !itemName.endsWith("_R")
                        && !itemName.endsWith("_nofilter")
                        && !itemName.endsWith("_Stubble")
                        && !itemName.endsWith("Open")
                        && !itemName.endsWith("Wet")
                        && !itemName.endsWith("Lit")
                        && !itemName.endsWith("Set")
                        && !itemName.endsWith("BulletsMold")
                        && !itemName.endsWith("ShellsMold")
                        && !itemName.contains("_R_")
                        && !itemName.contains("Debug")
                        && !itemName.contains("DEBUG")
                        && !itemName.contains("Dev")
                        && !itemName.contains("GloveBox")
                        && !itemName.contains("Trunk")
                        && (!itemName.contains("Tent") && !itemName.contains("SleepingBag") || itemName.contains("Packed"))) {
                        clothings.add(itemName);
                    }
                }
            }
        }

        return clothings;
    }

    public String getSelectedMap() {
        return this.selectedMap;
    }

    public void setSelectedMap(String selectedMap) {
        this.selectedMap = selectedMap;
    }

    public int getConsoleDotTxtSizeKB() {
        return this.consoleDotTxtSizeKb;
    }

    public void setConsoleDotTxtSizeKB(int kilobytes) {
        this.consoleDotTxtSizeKb = PZMath.clamp(kilobytes, 512, 102400);
    }

    public void setConsoleDotTxtSizeKB(String kilobytesString) {
        int kilobytes = PZMath.tryParseInt(kilobytesString, -1);
        if (kilobytes > 0) {
            this.setConsoleDotTxtSizeKB(kilobytes);
        }
    }

    public boolean getOptionUsePhysicsHitReaction() {
        return !GameClient.client && !GameServer.server ? this.optionUsePhysicsHitReaction.getValue() : false;
    }

    public void setOptionUsePhysicsHitReaction(boolean usePhysicsHitReaction) {
        this.optionUsePhysicsHitReaction.setValue(usePhysicsHitReaction);
    }

    public int getMaxActiveRagdolls() {
        return this.optionMaxActiveRagdolls.getValue();
    }

    public void setMaxActiveRagdolls(int maxActiveRagdolls) {
        this.optionMaxActiveRagdolls.setValue(maxActiveRagdolls);
    }

    public double getOptionWorldMapBrightness() {
        return this.optionWorldMapBrightness.getValue();
    }

    public void setOptionWorldMapBrightness(double d) {
        this.optionWorldMapBrightness.setValue(d);
    }

    public boolean getOptionShowWelcomeMessage() {
        return this.optionShowWelcomeMessage.getValue();
    }

    public void setOptionShowWelcomeMessage(boolean showWelcomeMessage) {
        this.optionShowWelcomeMessage.setValue(showWelcomeMessage);
    }

    public Account getAccountUsed() {
        return this.accountUsed;
    }

    public void setAccountUsed(Account accountUsed) {
        this.accountUsed = accountUsed;
    }

    public static boolean isDevMode() {
        return IS_DEV;
    }

    public record KeyBinding(String name, int keyValue, int altKey, boolean shift, boolean ctrl, boolean alt) {
    }

    public static final class KeyBindingList extends ArrayList<Core.KeyBinding> {
    }
}
