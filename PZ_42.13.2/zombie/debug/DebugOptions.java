// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.File;
import java.util.ArrayList;
import zombie.DebugFileWatcher;
import zombie.GameWindow;
import zombie.PredicatedFileWatcher;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderThread;
import zombie.debug.options.Animation;
import zombie.debug.options.Asset;
import zombie.debug.options.Character;
import zombie.debug.options.Cheat;
import zombie.debug.options.CollideWithObstacles;
import zombie.debug.options.DeadBodyAtlas;
import zombie.debug.options.IDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.debug.options.IsoSprite;
import zombie.debug.options.Model;
import zombie.debug.options.Multiplayer;
import zombie.debug.options.Network;
import zombie.debug.options.OffscreenBuffer;
import zombie.debug.options.OptionGroup;
import zombie.debug.options.Statistics;
import zombie.debug.options.Terrain;
import zombie.debug.options.Weather;
import zombie.debug.options.WorldItemAtlas;
import zombie.gameStates.GameLoadingState;
import zombie.iso.fboRenderChunk.FBORenderDebugOptions;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class DebugOptions implements IDebugOptionGroup {
    public static final int VERSION = 1;
    public static final DebugOptions instance = new DebugOptions();
    private static PredicatedFileWatcher triggerWatcher;
    private final ArrayList<BooleanDebugOption> options = new ArrayList<>();
    private final ArrayList<IDebugOption> debugOptions = new ArrayList<>();
    public final Asset asset = this.newOptionGroup(new Asset());
    public final Multiplayer multiplayer = this.newOptionGroup(new Multiplayer());
    public final Cheat cheat = this.newOptionGroup(new Cheat());
    public final CollideWithObstacles collideWithObstacles = this.newOptionGroup(new CollideWithObstacles());
    public final DeadBodyAtlas deadBodyAtlas = this.newOptionGroup(new DeadBodyAtlas());
    public final WorldItemAtlas worldItemAtlas = this.newOptionGroup(new WorldItemAtlas());
    public final BooleanDebugOption debugScenarioForceLaunch = this.newOption("DebugScenario.ForceLaunch", false);
    public final BooleanDebugOption mechanicsRenderHitbox = this.newOption("Mechanics.Render.Hitbox", false);
    public final BooleanDebugOption joypadRenderUi = this.newDebugOnlyOption("Joypad.Render.UI", false);
    public final Model model = this.newOptionGroup(new Model());
    public final BooleanDebugOption modRenderLoaded = this.newDebugOnlyOption("Mod.Render.Loaded", false);
    public final BooleanDebugOption pathfindPathToMouseAllowCrawl = this.newOption("Pathfind.PathToMouse.AllowCrawl", false);
    public final BooleanDebugOption pathfindPathToMouseAllowThump = this.newOption("Pathfind.PathToMouse.AllowThump", false);
    public final BooleanDebugOption pathfindPathToMouseEnable = this.newOption("Pathfind.PathToMouse.Enable", false);
    public final BooleanDebugOption pathfindPathToMouseIgnoreCrawlCost = this.newOption("Pathfind.PathToMouse.IgnoreCrawlCost", false);
    public final BooleanDebugOption pathfindPathToMouseRenderSuccessors = this.newOption("Pathfind.PathToMouse.RenderSuccessors", false);
    public final BooleanDebugOption pathfindRenderChunkRegions = this.newOption("Pathfind.Render.ChunkRegions", false);
    public final BooleanDebugOption pathfindRenderPath = this.newOption("Pathfind.Render.Path", false);
    public final BooleanDebugOption pathfindRenderWaiting = this.newOption("Pathfind.Render.Waiting", false);
    public final BooleanDebugOption pathfindSmoothPlayerPath = this.newOption("Pathfind.SmoothPlayerPath", true);
    public final BooleanDebugOption pathfindUseNativeCode = this.newOption("Pathfind.UseNativeCode", true);
    public final BooleanDebugOption pathfindBorderFinder = this.newOption("Pathfind.BorderFinder", false);
    public final BooleanDebugOption threadPathfinding = this.newOption("Threading.Pathfinding", false);
    public final BooleanDebugOption physicsRender = this.newOption("Physics.Render", false);
    public final BooleanDebugOption physicsRenderPlayerLevelOnly = this.newOption("Physics.Render.PlayerLevelOnly", false);
    public final BooleanDebugOption physicsRenderBallisticsControllers = this.newOption("Physics.Debug.Render.BallisticsControllers", false);
    public final BooleanDebugOption physicsRenderBallisticsTargets = this.newOption("Physics.Debug.Render.BallisticsTargets", false);
    public final BooleanDebugOption physicsRenderHighlightBallisticsTargets = this.newOption("Physics.Debug.Render.HighlightBallisticsTargets", false);
    public final BooleanDebugOption polymapRenderClusters = this.newOption("Polymap.Render.Clusters", false);
    public final BooleanDebugOption polymapRenderConnections = this.newOption("Polymap.Render.Connections", false);
    public final BooleanDebugOption polymapRenderCrawling = this.newOption("Polymap.Render.Crawling", false);
    public final BooleanDebugOption polymapRenderLineClearCollide = this.newOption("Polymap.Render.LineClearCollide", false);
    public final BooleanDebugOption polymapRenderNodes = this.newOption("Polymap.Render.Nodes", false);
    public final BooleanDebugOption tooltipInfo = this.newOption("Tooltip.Info", false);
    public final BooleanDebugOption tooltipAttributes = this.newDebugOnlyOption("Tooltip.Attributes", false);
    public final BooleanDebugOption tooltipModName = this.newDebugOnlyOption("Tooltip.ModName", false);
    public final BooleanDebugOption translationPrefix = this.newOption("Translation.Prefix", false);
    public final BooleanDebugOption uiRenderOutline = this.newOption("UI.Render.Outline", false);
    public final BooleanDebugOption uiDebugConsoleStartVisible = this.newOption("UI.DebugConsole.StartVisible", true);
    public final BooleanDebugOption uiDebugConsoleDebugLog = this.newOption("UI.DebugConsole.DebugLog", true);
    public final BooleanDebugOption uiDebugConsoleEchoCommand = this.newOption("UI.DebugConsole.EchoCommand", true);
    public final BooleanDebugOption uiDisableLogoState = this.newDebugOnlyOption("UI.DisableLogoState", true);
    public final BooleanDebugOption uiDisableWelcomeMessage = this.newOption("UI.DisableWelcomeMessage", false);
    public final BooleanDebugOption uiHideDebugContextMenuOptions = this.newOption("UI.HideDebugContextMenuOptions", false);
    public final BooleanDebugOption uiShowResearchableEtc = this.newOption("UI.UIShowResearchableEtc", true);
    public final BooleanDebugOption uiShowContextMenuReportOptions = this.newOption("UI.uiShowContextMenuReportOptions", true);
    public final BooleanDebugOption vehicleCycleColor = this.newDebugOnlyOption("Vehicle.CycleColor", false);
    public final BooleanDebugOption vehicleRenderBlood0 = this.newDebugOnlyOption("Vehicle.Render.Blood0", false);
    public final BooleanDebugOption vehicleRenderBlood50 = this.newDebugOnlyOption("Vehicle.Render.Blood50", false);
    public final BooleanDebugOption vehicleRenderBlood100 = this.newDebugOnlyOption("Vehicle.Render.Blood100", false);
    public final BooleanDebugOption vehicleRenderDamage0 = this.newDebugOnlyOption("Vehicle.Render.Damage0", false);
    public final BooleanDebugOption vehicleRenderDamage1 = this.newDebugOnlyOption("Vehicle.Render.Damage1", false);
    public final BooleanDebugOption vehicleRenderDamage2 = this.newDebugOnlyOption("Vehicle.Render.Damage2", false);
    public final BooleanDebugOption vehicleRenderRust0 = this.newDebugOnlyOption("Vehicle.Render.Rust0", false);
    public final BooleanDebugOption vehicleRenderRust50 = this.newDebugOnlyOption("Vehicle.Render.Rust50", false);
    public final BooleanDebugOption vehicleRenderRust100 = this.newDebugOnlyOption("Vehicle.Render.Rust100", false);
    public final BooleanDebugOption vehicleRenderOutline = this.newOption("Vehicle.Render.Outline", false);
    public final BooleanDebugOption vehicleRenderArea = this.newOption("Vehicle.Render.Area", false);
    public final BooleanDebugOption vehicleRenderAuthorizations = this.newOption("Vehicle.Render.Authorizations", false);
    public final BooleanDebugOption vehicleRenderInterpolateBuffer = this.newOption("Vehicle.Render.InterpolateBuffer", false);
    public final BooleanDebugOption vehicleRenderAttackPositions = this.newOption("Vehicle.Render.AttackPositions", false);
    public final BooleanDebugOption vehicleRenderExit = this.newOption("Vehicle.Render.Exit", false);
    public final BooleanDebugOption vehicleRenderIntersectedSquares = this.newOption("Vehicle.Render.IntersectedSquares", false);
    public final BooleanDebugOption vehicleRenderTrailerPositions = this.newDebugOnlyOption("Vehicle.Render.TrailerPositions", false);
    public final BooleanDebugOption vehicleSpawnEverywhere = this.newDebugOnlyOption("Vehicle.Spawn.Everywhere", false);
    public final BooleanDebugOption ambientWallEmittersRender = this.newDebugOnlyOption("Sound.AmbientWallEmitters.Render", false);
    public final BooleanDebugOption worldSoundRender = this.newOption("Sound.WorldSound.Render", false);
    public final BooleanDebugOption objectAmbientEmitterRender = this.newDebugOnlyOption("Sound.ObjectAmbientEmitter.Render", false);
    public final BooleanDebugOption parameterInsideRender = this.newDebugOnlyOption("Sound.ParameterInside.Render", false);
    public final BooleanDebugOption lightingRender = this.newOption("Lighting.Render", false);
    public final BooleanDebugOption skyboxShow = this.newOption("Skybox.Show", false);
    public final BooleanDebugOption worldStreamerSlowLoad = this.newOption("WorldStreamer.SlowLoad", false);
    public final BooleanDebugOption debugDrawSkipVboDraw = this.newOption("DebugDraw.SkipVBODraw", false);
    public final BooleanDebugOption debugDrawSkipDrawNonSkinnedModel = this.newOption("DebugDraw.SkipDrawNonSkinnedModel", false);
    public final BooleanDebugOption debugDrawSkipWorldShading = this.newOption("DebugDraw.SkipWorldShading", false);
    public final BooleanDebugOption debugDrawFishingZones = this.newOption("DebugDraw.FishingZones", false);
    public final BooleanDebugOption gameProfilerEnabled = this.newOption("GameProfiler.Enabled", false);
    public final BooleanDebugOption gameTimeSpeedHalf = this.newOption("GameTime.Speed.Half", false);
    public final BooleanDebugOption gameTimeSpeedQuarter = this.newOption("GameTime.Speed.Quarter", false);
    public final BooleanDebugOption gameTimeSpeedEighth = this.newOption("GameTime.Speed.Eighth", false);
    public final BooleanDebugOption freezeTimeOfDay = this.newOption("GameTime.TimeOfDay.Freeze", false);
    public final BooleanDebugOption threadCrashEnabled = this.newDebugOnlyOption("ThreadCrash.Enable", false);
    public final BooleanDebugOption[] threadCrashGameThread = new BooleanDebugOption[]{
        this.newDebugOnlyOption("ThreadCrash.MainThread.0", false),
        this.newDebugOnlyOption("ThreadCrash.MainThread.1", false),
        this.newDebugOnlyOption("ThreadCrash.MainThread.2", false)
    };
    public final BooleanDebugOption[] threadCrashRenderThread = new BooleanDebugOption[]{
        this.newDebugOnlyOption("ThreadCrash.RenderThread.0", false),
        this.newDebugOnlyOption("ThreadCrash.RenderThread.1", false),
        this.newDebugOnlyOption("ThreadCrash.RenderThread.2", false)
    };
    public final BooleanDebugOption[] threadCrashGameLoadingThread = new BooleanDebugOption[]{
        this.newDebugOnlyOption("ThreadCrash.GameLoadingThread.0", false)
    };
    public final BooleanDebugOption thumpableResetCurrentCellWindows = this.newDebugOnlyOption("Thumpable.IsoWindow.ResetCurrentCellWindows", false);
    public final BooleanDebugOption thumpableBarricadeCurrentCellWindowsFullPlanks = this.newDebugOnlyOption(
        "Thumpable.IsoBarricade.BarricadeCurrentCellWindows.FullPlank", false
    );
    public final BooleanDebugOption thumpableBarricadeCurrentCellWindowsHalfPlanks = this.newDebugOnlyOption(
        "Thumpable.IsoBarricade.BarricadeCurrentCellWindows.HalfPlank", false
    );
    public final BooleanDebugOption thumpableBarricadeCurrentCellWindowsFullMetalBars = this.newDebugOnlyOption(
        "Thumpable.IsoBarricade.BarricadeCurrentCellWindows.FullMetalBars", false
    );
    public final BooleanDebugOption thumpableBarricadeCurrentCellWindowsMetalPlate = this.newDebugOnlyOption(
        "Thumpable.IsoBarricade.BarBarricadeCurrentCellWindows.MetalPlate", false
    );
    public final BooleanDebugOption thumpableRemoveBarricadeCurrentCellWindows = this.newDebugOnlyOption(
        "Thumpable.IsoBarricade.RemoveBarricadeCurrentCellWindows", false
    );
    public final BooleanDebugOption worldChunkMap5x5 = this.newDebugOnlyOption("World.ChunkMap.5x5", false);
    public final BooleanDebugOption worldChunkMap7x7 = this.newDebugOnlyOption("World.ChunkMap.7x7", false);
    public final BooleanDebugOption worldChunkMap9x9 = this.newDebugOnlyOption("World.ChunkMap.9x9", false);
    public final BooleanDebugOption worldChunkMap11x11 = this.newDebugOnlyOption("World.ChunkMap.11x11", false);
    public final BooleanDebugOption worldChunkMap13x13 = this.newDebugOnlyOption("World.ChunkMap.13x13", false);
    public final BooleanDebugOption zombieRenderCanCrawlUnderVehicle = this.newDebugOnlyOption("Zombie.Render.CanCrawlUnderVehicle", false);
    public final BooleanDebugOption zombieRenderFakeDead = this.newDebugOnlyOption("Zombie.Render.FakeDead", false);
    public final BooleanDebugOption zombieRenderMemory = this.newDebugOnlyOption("Zombie.Render.Memory", false);
    public final BooleanDebugOption zombieRenderViewDistance = this.newDebugOnlyOption("Zombie.Render.ViewDistance", false);
    public final BooleanDebugOption zombieOutfitRandom = this.newDebugOnlyOption("Zombie.Outfit.Random", false);
    public final BooleanDebugOption entityDebugUi = this.newDebugOnlyOption("Entity.DebugUI", true);
    public final BooleanDebugOption zombieImposterRendering = this.newDebugOnlyOption("Zombie.Imposter.RenderImposters", false);
    public final BooleanDebugOption zombieBlendPreview = this.newDebugOnlyOption("Zombie.Imposter.PreviewBlend", false);
    public final BooleanDebugOption zombieImposterPreview = this.newDebugOnlyOption("Zombie.Imposter.PreviewCard", false);
    public final BooleanDebugOption zombieImposterBlend = this.newDebugOnlyOption("Zombie.Imposter.Blend", false);
    public final BooleanDebugOption renderTestFsQuad = this.newDebugOnlyOption("Render.Test.FSQuad", false);
    public final BooleanDebugOption zombieAnimationDelay = this.newDebugOnlyOption("Zombie.Animation.DelayInvisible", true);
    public final BooleanDebugOption zombieRenderInstanced = this.newDebugOnlyOption("Zombie.Render.Instanced", false);
    public final BooleanDebugOption newedDebugOnlyOption = this.newDebugOnlyOption("Instancing.Buffer.Copy", false);
    public final BooleanDebugOption threadLighting = this.newOption("Threading.Lighting", false);
    public final BooleanDebugOption lightingSplitUpdate = this.newDebugOnlyOption("Lighting.SplitUpdate", false);
    public final BooleanDebugOption threadAmbient = this.newOption("Threading.Ambient", false);
    public final BooleanDebugOption displayVisibilityPolygon = this.newDebugOnlyOption("Visibility.DisplayLines", false);
    public final BooleanDebugOption useNewVisibility = this.newOption("Visibility.UseNew", true);
    public final BooleanDebugOption previewTiles = this.newDebugOnlyOption("Visibility.PreviewTiles", false);
    public final BooleanDebugOption cheapOcclusionCount = this.newOption("FBORenderLevels.CheapOcclusionCount", true);
    public final BooleanDebugOption threadGridStacks = this.newOption("Threading.RecalculateGridStacks", false);
    public final BooleanDebugOption threadSound = this.newOption("Threading.Sound", false);
    public final BooleanDebugOption threadWorld = this.newOption("Threading.World", false);
    public final BooleanDebugOption threadModelSlotInit = this.newOption("Threading.ModelSlotInit", true);
    public final BooleanDebugOption threadAnimation = this.newOption("Threading.Animation", false);
    public final BooleanDebugOption delayObjectRender = this.newOption("Rendering.DelayObjects", false);
    public final DebugOptions.Checks checks = this.newOptionGroup(new DebugOptions.Checks());
    public final IsoSprite isoSprite = this.newOptionGroup(new IsoSprite());
    public final Network network = this.newOptionGroup(new Network());
    public final OffscreenBuffer offscreenBuffer = this.newOptionGroup(new OffscreenBuffer());
    public final Terrain terrain = this.newOptionGroup(new Terrain());
    public final Weather weather = this.newOptionGroup(new Weather());
    public final Animation animation = this.newOptionGroup(new Animation());
    public final Character character = this.newOptionGroup(new Character());
    public final FBORenderDebugOptions fboRenderChunk = this.newOptionGroup(new FBORenderDebugOptions());
    public final Statistics statistics = this.newOptionGroup(new Statistics());

    public static void testThreadCrash(int idx) {
        instance.testThreadCrashInternal(idx);
    }

    public void init() {
        this.load();
        this.initMessaging();
    }

    private void initMessaging() {
        if (triggerWatcher == null) {
            triggerWatcher = new PredicatedFileWatcher(
                ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetDebugOptions.xml"), this::onTrigger_SetDebugOptions
            );
            DebugFileWatcher.instance.add(triggerWatcher);
        }

        DebugOptionsXml debugXml = new DebugOptionsXml();
        debugXml.setDebugMode = true;
        debugXml.debugMode = Core.debug;

        for (BooleanDebugOption option : this.options) {
            debugXml.options.add(new DebugOptionsXml.OptionNode(option.getName(), option.getValue()));
        }

        String outFilePath = ZomboidFileSystem.instance.getMessagingDirSub("DebugOptions_list.xml");
        PZXmlUtil.tryWrite(debugXml, new File(outFilePath));
    }

    private void onTrigger_SetDebugOptions(String entryKey) {
        try {
            DebugOptionsXml optionsXml = PZXmlUtil.parse(DebugOptionsXml.class, ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetDebugOptions.xml"));

            for (DebugOptionsXml.OptionNode option : optionsXml.options) {
                this.setBoolean(option.name, option.value);
            }

            if (optionsXml.setDebugMode) {
                DebugLog.General.println("DebugMode: %s", optionsXml.debugMode ? "ON" : "OFF");
                Core.debug = optionsXml.debugMode;
            }

            this.save();
        } catch (PZXmlParserException var5) {
            ExceptionLogger.logException(var5, "Exception thrown parsing Trigger_SetDebugOptions.xml");
        }
    }

    @Override
    public Iterable<IDebugOption> getChildren() {
        return PZArrayUtil.listConvert(this.options, a -> a);
    }

    @Override
    public void addChild(IDebugOption newChild) {
        if (!this.debugOptions.contains(newChild)) {
            this.debugOptions.add(newChild);
            newChild.setParent(this);
            this.onChildAdded(newChild);
        }
    }

    @Override
    public void removeChild(IDebugOption child) {
        if (this.debugOptions.contains(child)) {
            this.debugOptions.remove(child);
            child.setParent(null);
        }
    }

    @Override
    public void onChildAdded(IDebugOption newOption) {
        this.onDescendantAdded(newOption);
    }

    @Override
    public void onDescendantAdded(IDebugOption newOption) {
        this.addOption(newOption);
    }

    private void addOption(IDebugOption newOption) {
        if (newOption instanceof BooleanDebugOption boolOption) {
            this.options.add(boolOption);
        }

        if (newOption instanceof IDebugOptionGroup group) {
            this.addDescendantOptions(group);
        }
    }

    private void addDescendantOptions(IDebugOptionGroup group) {
        for (IDebugOption child : group.getChildren()) {
            this.addOption(child);
        }
    }

    @Override
    public String getName() {
        return "DebugOptions";
    }

    @Override
    public String getCombinedName(String childName) {
        return childName;
    }

    @Override
    public IDebugOptionGroup getParent() {
        return null;
    }

    @Override
    public void setParent(IDebugOptionGroup parent) {
        throw new UnsupportedOperationException("DebugOptions is a root node. Cannot have a parent.");
    }

    @Override
    public void onFullPathChanged() {
        throw new UnsupportedOperationException("DebugOptions is a root node. Cannot have a parent.");
    }

    public BooleanDebugOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            BooleanDebugOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public BooleanDebugOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        BooleanDebugOption setting = this.getOptionByName(name);
        if (setting != null) {
            setting.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        BooleanDebugOption setting = this.getOptionByName(name);
        return setting != null ? setting.getValue() : false;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDirSub("debug-options.ini");
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDirSub("debug-options.ini");
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); i++) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption != null) {
                    myOption.parse(configOption.getValueAsString());
                }
            }
        }
    }

    private void testThreadCrashInternal(int idx) {
        if (Core.debug) {
            if (this.threadCrashEnabled.getValue()) {
                Thread currentThread = Thread.currentThread();
                BooleanDebugOption[] debugOptions;
                if (currentThread == RenderThread.renderThread) {
                    debugOptions = this.threadCrashRenderThread;
                } else if (currentThread == GameWindow.gameThread) {
                    debugOptions = this.threadCrashGameThread;
                } else {
                    if (currentThread != GameLoadingState.loader) {
                        return;
                    }

                    debugOptions = this.threadCrashGameLoadingThread;
                }

                if (debugOptions[idx].getValue()) {
                    throw new Error("ThreadCrash Test! " + currentThread.getName());
                }
            }
        }
    }

    public static final class Checks extends OptionGroup {
        public final BooleanDebugOption boundShader = this.newDebugOnlyOption("BoundShader", false);
        public final BooleanDebugOption boundTextures = this.newDebugOnlyOption("BoundTextures", false);
        public final BooleanDebugOption luaOwnerThread = this.newDebugOnlyOption("LuaOwnerThread", true);
        public final BooleanDebugOption objectPoolContains = this.newDebugOnlyOption("ObjectPool.Contains", false);
        public final BooleanDebugOption slowLuaEvents = this.newDebugOnlyOption("SlowLuaEvents", false);
    }
}
