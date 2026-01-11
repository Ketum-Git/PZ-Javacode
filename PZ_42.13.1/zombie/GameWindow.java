// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundBank;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Controller;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;
import org.lwjglx.opengl.OpenGLException;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.asset.AssetManagers;
import zombie.audio.BaseSoundBank;
import zombie.audio.DummySoundBank;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.Languages;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.input.Input;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.ZipLogs;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.Bullet;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.textures.NinePatchTexture;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.textures.TexturePackPage;
import zombie.core.znet.ServerBrowser;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugContext;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.entity.GameEntityManager;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileSystemImpl;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.GameStateMachine;
import zombie.gameStates.IngameState;
import zombie.gameStates.MainScreenState;
import zombie.gameStates.TISLogoState;
import zombie.gameStates.TermsOfServiceState;
import zombie.globalObjects.SGlobalObjects;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.inventory.types.MapItem;
import zombie.iso.FishSchoolManager;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LightingThread;
import zombie.iso.MetaTracker;
import zombie.iso.SliceY;
import zombie.iso.WorldStreamer;
import zombie.iso.sprite.IsoCursor;
import zombie.iso.sprite.IsoReticle;
import zombie.iso.worldgen.WorldGenParams;
import zombie.network.CoopMaster;
import zombie.network.CustomizationManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.savefile.SavefileThumbnail;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModRegistries;
import zombie.seams.SeamManager;
import zombie.seating.SeatingManager;
import zombie.spnetwork.SinglePlayerClient;
import zombie.spnetwork.SinglePlayerServer;
import zombie.spriteModel.SpriteModelManager;
import zombie.statistics.StatisticsManager;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryManager;
import zombie.tileDepth.TileSeamManager;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIManager;
import zombie.util.PZSQLUtils;
import zombie.util.PublicServerUtil;
import zombie.vehicles.Clipper;
import zombie.world.moddata.GlobalModData;
import zombie.worldMap.WorldMapImages;
import zombie.worldMap.WorldMapJNI;
import zombie.worldMap.WorldMapVisited;

@UsedFromLua
public final class GameWindow {
    private static final String GAME_TITLE = "Project Zomboid";
    private static final FPSTracking s_fpsTracking = new FPSTracking();
    private static final ThreadLocal<GameWindow.StringUTF> stringUTF = ThreadLocal.withInitial(GameWindow.StringUTF::new);
    public static final Input GameInput = new Input();
    public static final boolean DEBUG_SAVE = false;
    public static boolean okToSaveOnExit;
    public static String lastP;
    public static GameStateMachine states = new GameStateMachine();
    public static boolean serverDisconnected;
    public static boolean loadedAsClient;
    public static String kickReason;
    public static boolean drawReloadingLua;
    public static JoypadManager.Joypad activatedJoyPad;
    public static String version = "RC3";
    public static volatile boolean closeRequested;
    public static float averageFPS = PerformanceSettings.getLockFPS();
    private static boolean doRenderEvent;
    public static boolean luaDebuggerKeyDown;
    public static FileSystem fileSystem = new FileSystemImpl();
    public static AssetManagers assetManagers = new AssetManagers(fileSystem);
    private static long currentTime;
    private static long accumulator;
    public static boolean gameThreadExited;
    public static Thread gameThread;
    private static long updateTime;
    public static final ArrayList<GameWindow.TexturePack> texturePacks = new ArrayList<>();
    public static final FileSystem.TexturePackTextures texturePackTextures = new FileSystem.TexturePackTextures();

    private static void initShared() throws Exception {
        String path = ZomboidFileSystem.instance.getCacheDir() + File.separator;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        TexturePackPage.ignoreWorldItemTextures = true;
        int flags = 2;
        LoadTexturePack("UI", 2);
        LoadTexturePack("UI2", 2);
        LoadTexturePack("IconsMoveables", 2);
        LoadTexturePack("RadioIcons", 2);
        LoadTexturePack("ApComUI", 2);
        LoadTexturePack("Mechanics", 2);
        LoadTexturePack("WeatherFx", 2);
        setTexturePackLookup();
        IsoCursor.getInstance();
        IsoReticle.getInstance();
        MainScreenState.preloadBackgroundTextures();
        PerkFactory.init();
        CustomPerks.instance.init();
        CustomPerks.instance.initLua();
        CustomSandboxOptions.instance.init();
        CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
        ModRegistries.init();
        DoLoadingText(Translator.getText("UI_Loading_Scripts"));
        ScriptManager.instance.Load();
        CustomizationManager.getInstance().load();
        SpriteModelManager.getInstance().init();
        DoLoadingText(Translator.getText("UI_Loading_Clothing"));
        ClothingDecals.init();
        BeardStyles.init();
        HairStyles.init();
        OutfitManager.init();
        VoiceStyles.init();
        DoLoadingText("");
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        TexturePackPage.ignoreWorldItemTextures = false;
        TextureID.useCompression = TextureID.useCompressionOption;
        Mouse.initCustomCursor();
        TileGeometryManager.getInstance().init();
        TileDepthTextureAssignmentManager.getInstance().init();
        SeamManager.getInstance().init();
        SeatingManager.getInstance().init();
        if (!Core.debug || !DebugOptions.instance.uiDisableLogoState.getValue()) {
            states.states.add(new TISLogoState());
        }

        states.states.add(new TermsOfServiceState());
        states.states.add(new MainScreenState());
        if (!Core.debug) {
            states.loopToState = 1;
        }

        GameInput.initControllers();
        if (Core.getInstance().isDefaultOptions() && SteamUtils.isSteamModeEnabled() && SteamUtils.isRunningOnSteamDeck()) {
            Core.getInstance().setOptionActiveController(0, true);
        }

        int counta = GameInput.getControllerCount();
        DebugLog.Input.println("----------------------------------------------");
        DebugLog.Input.println("--    Information about controllers     ");
        DebugLog.Input.println("----------------------------------------------");

        for (int m = 0; m < counta; m++) {
            Controller controller = GameInput.getController(m);
            if (controller != null) {
                DebugLog.Input.println("----------------------------------------------");
                DebugLog.Input.println("--  Joypad: " + controller.getGamepadName());
                DebugLog.Input.println("----------------------------------------------");
                int count = controller.getAxisCount();
                if (count > 1) {
                    DebugLog.Input.println("----------------------------------------------");
                    DebugLog.Input.println("--    Axis definitions for controller " + m);
                    DebugLog.Input.println("----------------------------------------------");

                    for (int n = 0; n < count; n++) {
                        String name = controller.getAxisName(n);
                        DebugLog.Input.println("Axis: " + name);
                    }
                }

                count = controller.getButtonCount();
                if (count > 1) {
                    DebugLog.Input.println("----------------------------------------------");
                    DebugLog.Input.println("--    Button definitions for controller " + m);
                    DebugLog.Input.println("----------------------------------------------");

                    for (int n = 0; n < count; n++) {
                        String name = controller.getButtonName(n);
                        DebugLog.Input.println("Button: " + name);
                    }
                }
            }
        }
    }

    private static void logic() {
        Display.imGuiNewFrame();
        if (Core.debug) {
            try {
                DebugContext.instance.tickFrameStart();
            } catch (Exception var17) {
                var17.printStackTrace();
            }
        }

        if (GameClient.client) {
            try {
                GameClient.instance.update();
            } catch (Exception var16) {
                ExceptionLogger.logException(var16);
            }
        }

        try {
            SinglePlayerServer.update();
            SinglePlayerClient.update();
        } catch (Throwable var15) {
            ExceptionLogger.logException(var15);
        }

        GameProfiler profiler = GameProfiler.getInstance();

        try (GameProfiler.ProfileArea ignored = profiler.profile("Steam Loop")) {
            SteamUtils.runLoop();
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("Mouse")) {
            Mouse.update();
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("Keyboard")) {
            GameKeyboard.update();
        }

        GameInput.updateGameThread();
        if (CoopMaster.instance != null) {
            CoopMaster.instance.update();
        }

        if (IsoPlayer.players[0] != null) {
            IsoPlayer.setInstance(IsoPlayer.players[0]);
            IsoCamera.setCameraCharacter(IsoPlayer.players[0]);
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("UI")) {
            UIManager.update();
        }

        CompletableFuture<Void> uiVoice = null;
        if (DebugOptions.instance.threadSound.getValue()) {
            uiVoice = CompletableFuture.runAsync(() -> {
                VoiceManager.instance.update();
                SoundManager.instance.Update();
            }, PZForkJoinPool.commonPool());
        }

        LineDrawer.clear();
        if (JoypadManager.instance.isAPressed(-1)) {
            for (int n = 0; n < JoypadManager.instance.joypadList.size(); n++) {
                JoypadManager.Joypad joypad = JoypadManager.instance.joypadList.get(n);
                if (joypad.isAPressed()) {
                    if (activatedJoyPad == null) {
                        activatedJoyPad = joypad;
                    }

                    if (IsoPlayer.getInstance() != null) {
                        LuaEventManager.triggerEvent("OnJoypadActivate", joypad.getID());
                    } else {
                        LuaEventManager.triggerEvent("OnJoypadActivateUI", joypad.getID());
                    }
                    break;
                }
            }
        }

        boolean doUpdate = !GameTime.isGamePaused();

        try (GameProfiler.ProfileArea ignored = profiler.profile("Collision Data")) {
            MapCollisionData.instance.updateGameState();
        }

        CombatManager.getInstance().update(doUpdate);
        Mouse.setCursorVisible(Core.getInstance().displayCursor);
        if (doUpdate) {
            states.update();
        } else {
            IsoCamera.updateAll();
            if (isIngameState()) {
                LuaEventManager.triggerEvent("OnTickEvenPaused", BoxedStaticValues.toDouble(0.0));
            }
        }

        if (uiVoice != null) {
            uiVoice.join();
        } else {
            try (GameProfiler.ProfileArea ignored = profiler.profile("Voice")) {
                VoiceManager.instance.update();
            }

            try (GameProfiler.ProfileArea ignored = profiler.profile("Sound")) {
                SoundManager.instance.Update();
            }
        }

        try (GameProfiler.ProfileArea ignored = profiler.profile("UI Resize")) {
            UIManager.resize();
        }

        WorldMapImages.checkLoadingQueue();
        fileSystem.updateAsyncTransactions();
        if (GameKeyboard.isKeyPressed("Take screenshot")) {
            Core.getInstance().TakeFullScreenshot(null);
        }

        if (Core.debug) {
            try {
                DebugContext.instance.tick();
            } catch (Exception var14) {
                var14.printStackTrace();
            }
        }
    }

    public static boolean isIngameState() {
        return IngameState.instance != null && (states.current == IngameState.instance || states.states.contains(IngameState.instance));
    }

    public static void render() {
        IsoCamera.frameState.frameCount++;
        IsoCamera.frameState.updateUnPausedAccumulator();
        renderInternal();
    }

    protected static void renderInternal() {
        SpriteRenderer.instance.NewFrame();
        if (!PerformanceSettings.lightingThread && LightingJNI.init && !LightingJNI.WaitingForMain()) {
            LightingJNI.DoLightingUpdateNew(System.nanoTime(), Core.dirtyGlobalLightsCount > 0);
            if (Core.dirtyGlobalLightsCount > 0) {
                Core.dirtyGlobalLightsCount--;
            }
        }

        IsoObjectPicker.Instance.StartRender();
        LightingJNI.preUpdate();

        try (AbstractPerformanceProfileProbe ignored = GameWindow.s_performance.statesRender.profile()) {
            states.render();
        }
    }

    public static void InitDisplay() throws IOException, LWJGLException {
        Display.setTitle("Project Zomboid");
        if (!Core.getInstance().loadOptions()) {
            Core.setFullScreen(true);
            Display.setFullscreen(true);
            Display.setResizable(false);
            DisplayMode displayMode = Display.getDesktopDisplayMode();
            Core.getInstance().init(displayMode.getWidth(), displayMode.getHeight());
            if (!GL.getCapabilities().GL_ATI_meminfo && !GL.getCapabilities().GL_NVX_gpu_memory_info) {
                DebugLog.General.warn("Unable to determine available GPU memory, texture compression defaults to on");
                TextureID.useCompressionOption = true;
                TextureID.useCompression = true;
            }

            DebugLog.log("Init language : " + System.getProperty("user.language"));
            Core.getInstance().setOptionLanguageName(System.getProperty("user.language").toUpperCase());
            Core.getInstance().saveOptions();
        } else {
            Core.getInstance().init(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight());
        }

        if (GL.getCapabilities().GL_ATI_meminfo) {
            int kb = GL11.glGetInteger(34812);
            DebugLog.log("ATI: available texture memory is " + kb / 1024 + " MB");
        }

        if (GL.getCapabilities().GL_NVX_gpu_memory_info) {
            int kb = GL11.glGetInteger(36937);
            DebugLog.log("NVIDIA: current available GPU memory is " + kb / 1024 + " MB");
            kb = GL11.glGetInteger(36935);
            DebugLog.log("NVIDIA: dedicated available GPU memory is " + kb / 1024 + " MB");
            kb = GL11.glGetInteger(36936);
            DebugLog.log("NVIDIA: total available GPU memory is " + kb / 1024 + " MB");
        }

        SpriteRenderer.instance.create();
    }

    public static void InitGameThread() {
        Thread.setDefaultUncaughtExceptionHandler(GameWindow::uncaughtGlobalException);
        gameThread = MainThread.init(
            GameWindow::mainThreadStart, GameWindow::mainThreadStep, GameWindow::mainThreadExit, GameWindow::uncaughtExceptionMainThread
        );
    }

    private static void uncaughtExceptionMainThread(Thread thread, Throwable e) {
        if (e instanceof ThreadDeath) {
            DebugLog.General.println("Game Thread exited: ", thread.getName());
        } else {
            try {
                uncaughtException(thread, e);
            } finally {
                onGameThreadExited();
            }
        }
    }

    private static void uncaughtGlobalException(Thread thread, Throwable e) {
        if (e instanceof ThreadDeath) {
            DebugLog.General.println("External Thread exited: ", thread.getName());
        } else {
            uncaughtException(thread, e);
        }
    }

    public static void uncaughtException(Thread thread, Throwable e) {
        if (e instanceof ThreadDeath) {
            DebugLog.General.println("Internal Thread exited: ", thread.getName());
        } else {
            String exceptionMessage = String.format("Unhandled %s thrown by thread %s.", e.getClass().getName(), thread.getName());
            DebugLog.General.error(exceptionMessage);
            ExceptionLogger.logException(e, exceptionMessage);
        }
    }

    private static void mainThreadStart() {
        mainThreadInit();
        enter();
        RenderThread.invokeOnRenderContext(() -> {
            GL20.glUseProgram(0);
            ShaderHelper.forgetCurrentlyBound();
        });
        RenderThread.setWaitForRenderState(true);
        currentTime = System.nanoTime();
    }

    private static void mainThreadStep() {
        long newTime = System.nanoTime();
        if (newTime < currentTime) {
            currentTime = newTime;
        } else {
            long timeDiffNS = newTime - currentTime;
            currentTime = newTime;
            if (PerformanceSettings.instance.isFramerateUncapped()) {
                frameStep();
            } else {
                accumulator += timeDiffNS;
                long desiredDt = PZMath.secondsToNanos / PerformanceSettings.getLockFPS();
                if (accumulator >= desiredDt) {
                    frameStep();
                    accumulator %= desiredDt;
                }
            }

            if (Core.debug && DebugOptions.instance.threadCrashEnabled.getValue()) {
                DebugOptions.testThreadCrash(0);
                RenderThread.invokeOnRenderContext(() -> DebugOptions.testThreadCrash(1));
            }
        }
    }

    private static void mainThreadExit() {
        exit();
    }

    private static void mainThreadInit() {
        String debug = System.getProperty("debug");
        String viewports = System.getProperty("imguidebugviewports");
        String imgui = System.getProperty("imgui");
        String nosave = System.getProperty("nosave");
        if (nosave != null) {
            Core.getInstance().setNoSave(true);
        }

        if (debug != null) {
            Core.debug = true;
            if (viewports != null) {
                Core.useViewports = true;
            }

            if (imgui != null) {
                Core.imGui = true;
            }
        }

        if (!Core.soundDisabled) {
            FMODManager.instance.init();
        }

        DebugOptions.instance.init();
        GameProfiler.init();
        SoundManager.instance = (BaseSoundManager)(Core.soundDisabled ? new DummySoundManager() : new SoundManager());
        AmbientStreamManager.instance = (BaseAmbientStreamManager)(Core.soundDisabled ? new DummyAmbientStreamManager() : new AmbientStreamManager());
        BaseSoundBank.instance = (BaseSoundBank)(Core.soundDisabled ? new DummySoundBank() : new FMODSoundBank());
        VoiceManager.instance.loadConfig();

        while (!RenderThread.isRunning()) {
            Thread.yield();
        }

        TextureID.useCompressionOption = Core.safeModeForced || Core.getInstance().getOptionTextureCompression();
        TextureID.useCompression = TextureID.useCompressionOption;
        SoundManager.instance.setSoundVolume(Core.getInstance().getOptionSoundVolume() / 10.0F);
        SoundManager.instance.setMusicVolume(Core.getInstance().getOptionMusicVolume() / 10.0F);
        SoundManager.instance.setAmbientVolume(Core.getInstance().getOptionAmbientVolume() / 10.0F);
        SoundManager.instance.setVehicleEngineVolume(Core.getInstance().getOptionVehicleEngineVolume() / 10.0F);

        try {
            ZomboidFileSystem.instance.init();
        } catch (Exception var9) {
            throw new RuntimeException(var9);
        }

        DebugFileWatcher.instance.init();
        String server = System.getProperty("server");
        String client = System.getProperty("client");
        String nozombies = System.getProperty("nozombies");
        if (nozombies != null) {
            IsoWorld.noZombies = true;
        }

        if (server != null && server.equals("true")) {
            GameServer.server = true;
        }

        try {
            renameSaveFolders();
            init();
        } catch (Exception var8) {
            throw new RuntimeException(var8);
        }
    }

    private static void renameSaveFolders() {
        String saveDirPath = ZomboidFileSystem.instance.getSaveDir();
        File Saves = new File(saveDirPath);
        if (Saves.exists() && Saves.isDirectory()) {
            File Fighter = new File(Saves, "Fighter");
            File Survivor = new File(Saves, "Survivor");
            if (Fighter.exists() && Fighter.isDirectory() && Survivor.exists() && Survivor.isDirectory()) {
                DebugLog.log("RENAMING Saves/Survivor to Saves/Apocalypse");
                DebugLog.log("RENAMING Saves/Fighter to Saves/Survivor");
                Survivor.renameTo(new File(Saves, "Apocalypse"));
                Fighter.renameTo(new File(Saves, "Survivor"));
                File latestSave = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini");
                if (latestSave.exists()) {
                    latestSave.delete();
                }
            }
        }
    }

    public static long readLong(DataInputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        int ch5 = in.read();
        int ch6 = in.read();
        int ch7 = in.read();
        int ch8 = in.read();
        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        } else {
            return ch1 + (ch2 << 8) + (ch3 << 16) + (ch4 << 24) + (ch5 << 32) + (ch6 << 40) + (ch7 << 48) + (ch8 << 56);
        }
    }

    public static int readInt(DataInputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        } else {
            return ch1 + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
        }
    }

    private static void enter() {
        Core.tileScale = Core.getInstance().getOptionTexture2x() ? 2 : 1;
        IsoCamera.init();
        int flags = TextureID.useCompression ? 4 : 0;
        flags |= 64;
        if (Core.tileScale == 1) {
            LoadTexturePack("Tiles1x", flags);
            LoadTexturePack("Overlays1x", flags);
            LoadTexturePack("JumboTrees1x", flags);
            LoadTexturePack("Tiles1x.floor", flags & -5);
        }

        if (Core.tileScale == 2) {
            LoadTexturePack("Tiles2x", flags);
            LoadTexturePack("Overlays2x", flags);
            LoadTexturePack("JumboTrees2x", flags);
            LoadTexturePack("Tiles2x.floor", flags & -5);
            LoadTexturePack("B42ChunkCaching2x", flags);
            LoadTexturePack("B42ChunkCaching2x.floor", flags & -5);
            LoadTexturePack("Clock2x", flags);
        }

        setTexturePackLookup();
        Texture.getSharedTexture("animated_clock_01_0");
        Texture.getSharedTexture("animated_clock_01_1");
        Texture.getSharedTexture("animated_clock_01_2");
        Texture.getSharedTexture("animated_clock_01_3");
        if (Texture.getSharedTexture("TileIndieStoneTentFrontLeft") == null) {
            throw new RuntimeException("Rebuild Tiles.pack with \"1 Include This in .pack\" as individual images not tilesheets");
        } else {
            DebugLog.log("LOADED UP A TOTAL OF " + Texture.totalTextureID + " TEXTURES");
            s_fpsTracking.init();
            DoLoadingText(Translator.getText("UI_Loading_ModelsAnimations"));
            ModelManager.instance.create();
            if (!SteamUtils.isSteamModeEnabled()) {
                DoLoadingText(Translator.getText("UI_Loading_InitPublicServers"));
                PublicServerUtil.init();
            }

            TileDepthTextureManager.getInstance().init();
            TileDepthMapManager.instance.init();
            TileSeamManager.instance.init();
            VoiceManager.instance.InitVMClient();
            DoLoadingText(Translator.getText("UI_Loading_OnGameBoot"));
            LuaEventManager.triggerEvent("OnGameBoot");
            UIManager.setShowLuaDebuggerOnError(true);
            if (Core.debug) {
                DebugContext.instance.init();
            }
        }
    }

    private static void frameStep() {
        long startTime = System.nanoTime();
        IsoCamera.frameState.frameCount++;
        IsoCamera.frameState.updateUnPausedAccumulator();

        try (AbstractPerformanceProfileProbe ignored = GameWindow.s_performance.frameStep.profile()) {
            s_fpsTracking.frameStep();

            try (AbstractPerformanceProfileProbe ignored1 = GameWindow.s_performance.logic.profile()) {
                logic();
            }

            if (!Core.isUseGameViewport()) {
                Core.getInstance().setScreenSize(RenderThread.getDisplayWidth(), RenderThread.getDisplayHeight());
            }

            IsoWorld.instance.FinishAnimation();
            GameProfiler profiler = GameProfiler.getInstance();
            if (!GameServer.server) {
                try (GameProfiler.ProfileArea ignored1 = profiler.profile("IsoObjectAnimations.update")) {
                    IsoObjectAnimations.getInstance().update();
                }
            }

            renderInternal();
            if (doRenderEvent) {
                try (GameProfiler.ProfileArea ignored1 = profiler.profile("On Render")) {
                    onRender();
                }
            }

            Core.getInstance().DoFrameReady();

            try (GameProfiler.ProfileArea ignored1 = profiler.profile("Lighting")) {
                LightingThread.instance.update();
            }

            if (states.current instanceof GameLoadingState) {
                if (GameLoadingState.loader == null || !GameLoadingState.loader.isAlive()) {
                    LuaEventManager.RunQueuedEvents();
                }
            } else {
                LuaEventManager.RunQueuedEvents();
            }

            if (Core.debug) {
                if (GameKeyboard.isKeyDown("Toggle Lua Debugger")) {
                    if (!luaDebuggerKeyDown) {
                        UIManager.setShowLuaDebuggerOnError(true);
                        LuaManager.thread.step = true;
                        LuaManager.thread.stepInto = true;
                        luaDebuggerKeyDown = true;
                    }
                } else {
                    luaDebuggerKeyDown = false;
                }

                if (GameKeyboard.isKeyPressed("ToggleLuaConsole")) {
                    UIElement console = UIManager.getDebugConsole();
                    if (console != null) {
                        console.setVisible(!console.isVisible());
                    }
                }
            }
        } catch (OpenGLException var31) {
            RenderThread.logGLException(var31);
            if (Core.isImGui()) {
                Display.imguiEndFrame();
            }
        } catch (Exception var32) {
            ExceptionLogger.logException(var32);
            if (Core.isImGui()) {
                Display.imguiEndFrame();
            }
        } finally {
            updateTime = System.nanoTime() - startTime;
        }
    }

    public static long getUpdateTime() {
        return updateTime;
    }

    private static void onRender() {
        LuaEventManager.triggerEvent("OnRenderTick");
    }

    private static void exit() {
        DebugType.ExitDebug.debugln("GameWindow.exit 1");
        if (GameClient.client) {
            WorldStreamer.instance.stop();
            GameClient.instance.doDisconnect("exit");
            VoiceManager.instance.DeinitVMClient();
        }

        if (okToSaveOnExit) {
            try {
                WorldStreamer.instance.quit();
            } catch (Exception var6) {
                var6.printStackTrace();
            }

            if (PlayerDB.isAllow()) {
                PlayerDB.getInstance().saveLocalPlayersForce();
                PlayerDB.getInstance().canSavePlayers = false;
            }

            try {
                if (GameClient.client && GameClient.connection != null) {
                    GameClient.connection.username = null;
                }

                save(true);
            } catch (Throwable var5) {
                var5.printStackTrace();
            }

            try {
                if (IsoWorld.instance.currentCell != null) {
                    LuaEventManager.triggerEvent("OnPostSave");
                }
            } catch (Exception var4) {
                var4.printStackTrace();
            }

            try {
                if (IsoWorld.instance.currentCell != null) {
                    LuaEventManager.triggerEvent("OnPostSave");
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }

            try {
                LightingThread.instance.stop();
                MapCollisionData.instance.stop();
                AnimalPopulationManager.getInstance().stop();
                ZombiePopulationManager.instance.stop();
                if (PathfindNative.useNativeCode) {
                    PathfindNative.instance.stop();
                } else {
                    PolygonalMap2.instance.stop();
                }

                ZombieSpawnRecorder.instance.quit();
            } catch (Exception var2) {
                var2.printStackTrace();
            }
        }

        DebugType.ExitDebug.debugln("GameWindow.exit 2");
        if (GameClient.client) {
            WorldStreamer.instance.stop();
            GameClient.instance.doDisconnect("exit-saving");

            try {
                Thread.sleep(500L);
            } catch (InterruptedException var1) {
                var1.printStackTrace();
            }
        }

        DebugType.ExitDebug.debugln("GameWindow.exit 3");
        if (PlayerDB.isAvailable()) {
            PlayerDB.getInstance().close();
        }

        if (ClientPlayerDB.isAvailable()) {
            ClientPlayerDB.getInstance().close();
        }

        DebugType.ExitDebug.debugln("GameWindow.exit 4");
        GameClient.instance.Shutdown();
        SteamUtils.shutdown();
        ZipLogs.addZipFile(true);
        PathfindNative.freeMemoryAtExit();
        onGameThreadExited();
        DebugType.ExitDebug.debugln("GameWindow.exit 5");
    }

    private static void onGameThreadExited() {
        gameThreadExited = true;
        RenderThread.onGameThreadExited();
    }

    public static void setTexturePackLookup() {
        texturePackTextures.clear();

        for (int i = texturePacks.size() - 1; i >= 0; i--) {
            GameWindow.TexturePack texturePack = texturePacks.get(i);
            if (texturePack.modId == null) {
                texturePackTextures.putAll(texturePack.textures);
            }
        }

        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

        for (int ix = texturePacks.size() - 1; ix >= 0; ix--) {
            GameWindow.TexturePack texturePack = texturePacks.get(ix);
            if (texturePack.modId != null && modIDs.contains(texturePack.modId)) {
                texturePackTextures.putAll(texturePack.textures);
            }
        }

        Texture.onTexturePacksChanged();
        NinePatchTexture.onTexturePacksChanged();
    }

    public static void LoadTexturePack(String pack, int flags) {
        LoadTexturePack(pack, flags, null);
    }

    public static void LoadTexturePack(String pack, int flags, String modID) {
        DebugLog.General.println("texturepack: loading " + pack);
        DoLoadingText(Translator.getText("UI_Loading_Texturepack", pack));
        String fileName = ZomboidFileSystem.instance.getString("media/texturepacks/" + pack + ".pack");
        GameWindow.TexturePack texturePack = new GameWindow.TexturePack();
        texturePack.packName = pack;
        texturePack.fileName = fileName;
        texturePack.modId = modID;
        fileSystem.mountTexturePack(pack, texturePack.textures, flags);
        texturePacks.add(texturePack);
    }

    private static void installRequiredLibrary(String exe, String name) {
        if (new File(exe).exists()) {
            DebugLog.log("Attempting to install " + name);
            DebugLog.log("Running " + exe + ".");
            ProcessBuilder pb = new ProcessBuilder(exe, "/quiet", "/norestart");

            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                DebugLog.log("Process exited with code " + exitCode);
                return;
            } catch (InterruptedException | IOException var5) {
                var5.printStackTrace();
            }
        }

        DebugLog.log("Please install " + name);
    }

    private static void checkRequiredLibraries() {
        if (System.getProperty("os.name").startsWith("Win")) {
            String suffix = "";
            if ("1".equals(System.getProperty("zomboid.debuglibs.lighting"))) {
                DebugLog.log("***** Loading debug version of Lighting");
                suffix = "d";
            }

            String dll = "Lighting64" + suffix;

            try {
                System.loadLibrary(dll);
            } catch (UnsatisfiedLinkError var3) {
                DebugLog.log("Error loading " + dll + ".dll.  Your system may be missing a required DLL.");
                installRequiredLibrary("_CommonRedist\\vcredist\\2010\\vcredist_x64.exe", "the Microsoft Visual C++ 2010 Redistributable.");
                installRequiredLibrary("_CommonRedist\\vcredist\\2012\\vcredist_x64.exe", "the Microsoft Visual C++ 2012 Redistributable.");
                installRequiredLibrary("_CommonRedist\\vcredist\\2013\\vcredist_x64.exe", "the Microsoft Visual C++ 2013 Redistributable.");
            }
        }
    }

    private static void init() throws Exception {
        Core.getInstance().initGlobalShader();
        RenderThread.invokeOnRenderContext(() -> {
            GL20.glUseProgram(SceneShaderStore.defaultShaderId);
            ShaderHelper.forgetCurrentlyBound();
        });
        initFonts();
        checkRequiredLibraries();
        SteamUtils.init();
        ServerBrowser.init();
        SteamFriends.init();
        SteamWorkshop.init();
        RakNetPeerInterface.init();
        LightingJNI.init();
        ZombiePopulationManager.init();
        PZSQLUtils.init();
        Clipper.init();
        PathfindNative.init();
        WorldMapJNI.init();
        Bullet.init();
        int cores = Runtime.getRuntime().availableProcessors();
        String path = ZomboidFileSystem.instance.getCacheDir() + File.separator;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        DoLoadingText("Loading Mods");
        ZomboidFileSystem.instance.resetDefaultModsForNewRelease("42_00");
        ZomboidFileSystem.instance.loadMods("default");
        ZomboidFileSystem.instance.loadModPackFiles();
        if (Core.getInstance().isDefaultOptions() && SteamUtils.isSteamModeEnabled() && SteamUtils.isRunningOnSteamDeck()) {
            Core.getInstance().setOptionFontSize(2);
            Core.getInstance().setOptionSingleContextMenu(0, true);
            Core.getInstance().setOptionShoulderButtonContainerSwitch(1);
            Core.getInstance().setAutoZoom(0, true);
            Core.getInstance().setOptionZoomLevels2x("75;125;150;175;200;225");
            Core.getInstance().setOptionPanCameraWhileAiming(true);
            Core.getInstance().setOptionPanCameraWhileDriving(true);
            Core.getInstance().setOptionTextureCompression(true);
            Core.getInstance().setOptionVoiceEnable(false, false);
        }

        DoLoadingText("Loading Translations");
        Languages.instance.init();
        Translator.language = null;
        initFonts();
        Translator.loadFiles();
        LuaManager.init();
        initShared();
        DoLoadingText(Translator.getText("UI_Loading_Lua"));
        LuaManager.LoadDirBase();
        ZomboidGlobals.Load();
        LuaEventManager.triggerEvent("OnLoadSoundBanks");
    }

    public static void initFonts() throws FileNotFoundException {
        TextManager.instance.Init();

        while (TextManager.instance.font.isEmpty()) {
            fileSystem.updateAsyncTransactions();

            try {
                Thread.sleep(10L);
            } catch (InterruptedException var1) {
            }
        }
    }

    public static void save(boolean bDoChars) throws IOException {
        if (!Core.getInstance().isNoSave()) {
            if (IsoWorld.instance.currentCell != null
                && !"LastStand".equals(Core.getInstance().getGameMode())
                && !"Tutorial".equals(Core.getInstance().getGameMode())) {
                if (GameClient.clientSave) {
                    GameClient.clientSave = GameClient.client;
                    MapItem.SaveWorldMap();
                    WorldMapVisited.SaveAll();
                    LuaEventManager.triggerEvent("OnSave");
                } else {
                    File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");

                    try (
                        FileOutputStream fos = new FileOutputStream(outFile);
                        DataOutputStream output = new DataOutputStream(fos);
                    ) {
                        output.writeInt(240);
                        WriteString(output, Core.gameMap);
                        WriteString(output, IsoWorld.instance.getDifficulty());
                    }

                    outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

                    try (
                        FileOutputStream var23 = new FileOutputStream(outFile);
                        BufferedOutputStream output = new BufferedOutputStream(var23);
                    ) {
                        SliceY.SliceBuffer.clear();
                        SandboxOptions.instance.save(SliceY.SliceBuffer);
                        output.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                    }

                    WorldGenParams.INSTANCE.save();
                    InstanceTracker.save();
                    MetaTracker.save();
                    StatisticsManager.getInstance().save();
                    LuaEventManager.triggerEvent("OnSave");

                    try {
                        try {
                            try {
                                if (Thread.currentThread() == gameThread) {
                                    SavefileThumbnail.create();
                                }
                            } catch (Exception var14) {
                                ExceptionLogger.logException(var14);
                            }

                            outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map.bin");

                            try (FileOutputStream outStream = new FileOutputStream(outFile)) {
                                DataOutputStream output = new DataOutputStream(outStream);
                                IsoWorld.instance.currentCell.save(output, bDoChars);
                            } catch (Exception var13) {
                                ExceptionLogger.logException(var13);
                            }

                            AnimalPopulationManager.getInstance().save();

                            try {
                                MapCollisionData.instance.save();
                                if (!loadedAsClient) {
                                    SGlobalObjects.save();
                                }
                            } catch (Exception var10) {
                                ExceptionLogger.logException(var10);
                            }

                            ZomboidRadio.getInstance().Save();
                            GlobalModData.instance.save();
                            MapItem.SaveWorldMap();
                            WorldMapVisited.SaveAll();
                            FishSchoolManager.getInstance().save();
                            GameEntityManager.Save();
                        } catch (IOException var15) {
                            throw new RuntimeException(var15);
                        }
                    } catch (RuntimeException var20) {
                        if (var20.getCause() instanceof IOException ioException) {
                            throw ioException;
                        } else {
                            throw var20;
                        }
                    }
                }
            }
        }
    }

    public static String getCoopServerHome() {
        File file = new File(ZomboidFileSystem.instance.getCacheDir());
        return file.getParent();
    }

    public static void WriteString(ByteBuffer output, String str) {
        WriteStringUTF(output, str);
    }

    public static void WriteStringUTF(ByteBuffer output, String str) {
        stringUTF.get().save(output, str);
    }

    public static void WriteString(DataOutputStream output, String str) throws IOException {
        if (str == null) {
            output.writeInt(0);
        } else {
            output.writeInt(str.length());
            if (str != null && str.length() >= 0) {
                output.writeChars(str);
            }
        }
    }

    public static String ReadStringUTF(ByteBuffer input) {
        return stringUTF.get().load(input);
    }

    public static String ReadString(ByteBuffer input) {
        return ReadStringUTF(input);
    }

    public static String ReadString(DataInputStream input) throws IOException {
        int len = input.readInt();
        if (len == 0) {
            return "";
        } else if (len > 65536) {
            throw new RuntimeException("GameWindow.ReadString: string is too long, corrupted save?");
        } else {
            StringBuilder sb = new StringBuilder(len);

            for (int n = 0; n < len; n++) {
                sb.append(input.readChar());
            }

            return sb.toString();
        }
    }

    public static ByteBuffer getEncodedBytesUTF(String str) {
        return stringUTF.get().getEncodedBytes(str);
    }

    public static void WriteUUID(ByteBuffer output, UUID uuid) {
        output.putLong(uuid.getMostSignificantBits());
        output.putLong(uuid.getLeastSignificantBits());
    }

    public static UUID ReadUUID(ByteBuffer input) {
        return new UUID(input.getLong(), input.getLong());
    }

    public static void doRenderEvent(boolean b) {
        doRenderEvent = b;
    }

    public static void DoLoadingText(String text) {
        if (SpriteRenderer.instance != null && TextManager.instance.font != null) {
            Core.getInstance().StartFrame();
            Core.getInstance().EndFrame();
            Core.getInstance().StartFrameUI();
            SpriteRenderer.instance
                .renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0F, 0.0F, 0.0F, 1.0F, null);
            TextManager.instance.DrawStringCentre(Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() / 2, text, 1.0, 1.0, 1.0, 1.0);
            Core.getInstance().EndFrameUI();
        }
    }

    public static class OSValidator {
        private static final String OS = System.getProperty("os.name").toLowerCase();

        public static boolean isWindows() {
            return OS.indexOf("win") >= 0;
        }

        public static boolean isMac() {
            return OS.indexOf("mac") >= 0;
        }

        public static boolean isUnix() {
            return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0;
        }

        public static boolean isSolaris() {
            return OS.indexOf("sunos") >= 0;
        }
    }

    private static class StringUTF {
        private char[] chars;
        private ByteBuffer byteBuffer;
        private CharBuffer charBuffer;
        private CharsetEncoder ce;
        private CharsetDecoder cd;

        private int encode(String str) {
            if (this.chars == null || this.chars.length < str.length()) {
                int capacity = (str.length() + 128 - 1) / 128 * 128;
                this.chars = new char[capacity];
                this.charBuffer = CharBuffer.wrap(this.chars);
            }

            str.getChars(0, str.length(), this.chars, 0);
            this.charBuffer.limit(str.length());
            this.charBuffer.position(0);
            if (this.ce == null) {
                this.ce = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            }

            this.ce.reset();
            int maxBytes = (int)((double)str.length() * this.ce.maxBytesPerChar());
            maxBytes = (maxBytes + 128 - 1) / 128 * 128;
            if (this.byteBuffer == null || this.byteBuffer.capacity() < maxBytes) {
                this.byteBuffer = ByteBuffer.allocate(maxBytes);
            }

            this.byteBuffer.clear();
            CoderResult result = this.ce.encode(this.charBuffer, this.byteBuffer, true);
            return this.byteBuffer.position();
        }

        private String decode(int numBytes) {
            if (this.cd == null) {
                this.cd = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            }

            this.cd.reset();
            int maxChars = (int)((double)numBytes * this.cd.maxCharsPerByte());
            if (this.chars == null || this.chars.length < maxChars) {
                int capacity = (maxChars + 128 - 1) / 128 * 128;
                this.chars = new char[capacity];
                this.charBuffer = CharBuffer.wrap(this.chars);
            }

            this.charBuffer.clear();
            CoderResult result = this.cd.decode(this.byteBuffer, this.charBuffer, true);
            return new String(this.chars, 0, this.charBuffer.position());
        }

        ByteBuffer getEncodedBytes(String str) {
            this.encode(str);
            return this.byteBuffer;
        }

        void save(ByteBuffer out, String str) {
            if (str != null && !str.isEmpty()) {
                int numBytes = this.encode(str);
                out.putShort((short)numBytes);
                this.byteBuffer.flip();
                out.put(this.byteBuffer);
            } else {
                out.putShort((short)0);
            }
        }

        String load(ByteBuffer in) {
            int numBytes = in.getShort();
            if (numBytes <= 0) {
                return "";
            } else {
                int maxBytes = (numBytes + 128 - 1) / 128 * 128;
                if (this.byteBuffer == null || this.byteBuffer.capacity() < maxBytes) {
                    this.byteBuffer = ByteBuffer.allocate(maxBytes);
                }

                this.byteBuffer.clear();
                if (in.remaining() < numBytes) {
                    DebugLog.General
                        .error("GameWindow.StringUTF.load> numBytes:" + numBytes + " is higher than the remaining bytes in the buffer:" + in.remaining());
                }

                int limit = in.limit();
                in.limit(in.position() + numBytes);
                this.byteBuffer.put(in);
                in.limit(limit);
                this.byteBuffer.flip();
                return this.decode(numBytes);
            }
        }
    }

    private static final class TexturePack {
        String packName;
        String fileName;
        String modId;
        final FileSystem.TexturePackTextures textures = new FileSystem.TexturePackTextures();
    }

    private static class s_performance {
        static final PerformanceProfileFrameProbe frameStep = new PerformanceProfileFrameProbe("GameWindow.frameStep");
        static final PerformanceProfileProbe statesRender = new PerformanceProfileProbe("GameWindow.states.render");
        static final PerformanceProfileProbe logic = new PerformanceProfileProbe("GameWindow.logic");
    }
}
