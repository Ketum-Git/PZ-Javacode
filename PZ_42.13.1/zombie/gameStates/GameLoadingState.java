// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import zombie.AmbientStreamManager;
import zombie.ChunkMapFilenames;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatManager;
import zombie.chat.ChatUtility;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.core.textures.AnimatedTexture;
import zombie.core.textures.AnimatedTextures;
import zombie.core.textures.Texture;
import zombie.core.znet.ServerBrowser;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.SGlobalObjects;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.WorldConverter;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.ClimateManager;
import zombie.modding.ActiveMods;
import zombie.modding.ActiveModsFile;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkAIParams;
import zombie.network.ServerOptions;
import zombie.savefile.SavefileNaming;
import zombie.scripting.ScriptManager;
import zombie.ui.ScreenFader;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.world.WorldDictionary;
import zombie.worldMap.WorldMapImages;
import zombie.worldMap.WorldMapVisited;

@UsedFromLua
public final class GameLoadingState extends GameState {
    public static final int QUICK_TIP_MAX_TIMER = 720;
    public static Thread loader;
    private static boolean newGame = true;
    private static long startTime;
    public static boolean worldVersionError;
    private static boolean unexpectedError;
    public static String gameLoadingString = "";
    public static boolean playerWrongIP;
    private static boolean showedUI;
    private static boolean showedClickToSkip;
    public static boolean mapDownloadFailed;
    private static boolean playerCreated;
    private static boolean done;
    public static boolean convertingWorld;
    public static int convertingFileCount = -1;
    public static int convertingFileMax = -1;
    private volatile boolean waitForAssetLoadingToFinish1;
    private volatile boolean waitForAssetLoadingToFinish2;
    private final Object assetLock1 = "Asset Lock 1";
    private final Object assetLock2 = "Asset Lock 2";
    private float time;
    private boolean forceDone;
    private String text;
    private float width;
    private static final ScreenFader screenFader = new ScreenFader();
    private AnimatedTexture animatedTexture;
    private long progressFadeStartMs;
    private int stage;
    private final float totalTime = 33.0F;
    private float loadingDotTick;
    private String loadingDot = "";
    private float clickToSkipAlpha = 1.0F;
    private boolean clickToSkipFadeIn;
    private float quickTipsTimer = 720.0F;
    private String quickTipsText;
    private List<String> quickTipsList;
    private List<String> quickTipsListJoke;
    private static final int BOTTOM_SCREEN = 40;

    @Override
    public void enter() {
        this.loadQuickTipList();

        try {
            WorldMapImages.Reset();
            WorldMapVisited.Reset();
            LuaManager.releaseAllVideoTextures();
        } catch (Exception var8) {
            ExceptionLogger.logException(var8);
        }

        if (GameClient.client) {
            this.text = Translator.getText("UI_DirectConnectionPortWarning", ServerOptions.getInstance().udpPort.getValue());
            this.width = TextManager.instance.MeasureStringX(UIFont.NewMedium, this.text) + 8;
        }

        GameWindow.loadedAsClient = GameClient.client;
        GameWindow.okToSaveOnExit = false;
        showedUI = false;
        ChunkMapFilenames.instance.clear();
        DebugLog.DetailedInfo.trace("Savefile name is \"" + Core.gameSaveWorld + "\"");
        gameLoadingString = "";

        try {
            LuaManager.LoadDirBase("server");
            LuaManager.finishChecksum();
        } catch (Exception var7) {
            ExceptionLogger.logException(var7);
        }

        ScriptManager.instance.LoadedAfterLua();
        Core.getInstance().initFBOs();
        Core.getInstance().initShaders();
        SkyBox.getInstance();
        IsoPuddles.getInstance();
        IsoWater.getInstance();
        GameWindow.serverDisconnected = false;
        if (GameClient.client && !GameClient.instance.connected) {
            GameClient.instance.init();
            Core.getInstance().setGameMode("Multiplayer");

            for (; GameClient.instance.id == -1; GameClient.instance.update()) {
                try {
                    LuaEventManager.RunQueuedEvents();
                    Thread.sleep(10L);
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }

            Core.gameSaveWorld = "clienttest" + GameClient.instance.id;
            LuaManager.GlobalObject.deleteSave("clienttest" + GameClient.instance.id);
            LuaManager.GlobalObject.createWorld("clienttest" + GameClient.instance.id);
        }

        if (Core.gameSaveWorld.isEmpty()) {
            DebugLog.log("No savefile directory was specified.  It's a bug.");
            GameWindow.DoLoadingText("No savefile directory was specified.  The game will now close.  Sorry!");

            try {
                Thread.sleep(4000L);
            } catch (Exception var5) {
            }

            System.exit(-1);
        }

        File file = new File(ZomboidFileSystem.instance.getCurrentSaveDir());
        if (!file.exists() && !Core.getInstance().isNoSave()) {
            DebugLog.log("The savefile directory doesn't exist.  It's a bug.");
            GameWindow.DoLoadingText("The savefile directory doesn't exist.  The game will now close.  Sorry!");

            try {
                Thread.sleep(4000L);
            } catch (Exception var4) {
            }

            System.exit(-1);
        }

        if (!Core.getInstance().isNoSave()) {
            SavefileNaming.ensureSubdirectoriesExist(ZomboidFileSystem.instance.getCurrentSaveDir());
        }

        try {
            if (!GameClient.client && !GameServer.server && !Core.tutorial && !Core.isLastStand() && !"Multiplayer".equals(Core.gameMode)) {
                FileWriter fw = new FileWriter(new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini"));
                fw.write(IsoWorld.instance.getWorld() + "\r\n");
                fw.write(Core.getInstance().getGameMode() + "\r\n");
                fw.write(IsoWorld.instance.getDifficulty() + "\r\n");
                fw.flush();
                fw.close();
            }
        } catch (IOException var3) {
            ExceptionLogger.logException(var3);
        }

        done = false;
        this.forceDone = false;
        IsoChunkMap.CalcChunkWidth();
        Core.setInitialSize();
        LosUtil.init(IsoChunkMap.chunkGridWidth * 8, IsoChunkMap.chunkGridWidth * 8);
        this.time = 0.0F;
        this.stage = 0;
        this.clickToSkipAlpha = 1.0F;
        this.clickToSkipFadeIn = false;
        startTime = System.currentTimeMillis();
        SoundManager.instance.Purge();
        SoundManager.instance.setMusicState("Loading");
        LuaEventManager.triggerEvent("OnPreMapLoad");
        newGame = true;
        worldVersionError = false;
        unexpectedError = false;
        mapDownloadFailed = false;
        playerCreated = false;
        convertingWorld = false;
        convertingFileCount = 0;
        convertingFileMax = -1;
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");
        if (inFile.exists()) {
            newGame = false;
        }

        if (GameClient.client) {
            newGame = false;
        }

        if (!newGame) {
            this.stage = -1;
            screenFader.startFadeFromBlack();
            this.progressFadeStartMs = 0L;
        }

        WorldDictionary.setIsNewGame(newGame);
        GameKeyboard.noEventsWhileLoading = true;
        ServerBrowser.setSuppressLuaCallbacks(true);
        loader = new Thread(ThreadGroups.Workers, new Runnable() {
            {
                Objects.requireNonNull(GameLoadingState.this);
            }

            @Override
            public void run() {
                LuaManager.thread.debugOwnerThread = Thread.currentThread();
                LuaManager.debugthread.debugOwnerThread = Thread.currentThread();

                try {
                    this.runInner();
                } catch (Throwable var5) {
                    GameLoadingState.unexpectedError = true;
                    ExceptionLogger.logException(var5);
                } finally {
                    LuaManager.thread.debugOwnerThread = GameWindow.gameThread;
                    LuaManager.debugthread.debugOwnerThread = GameWindow.gameThread;
                    UIManager.suspend = false;
                }
            }

            private void runInner() throws Exception {
                GameLoadingState.this.waitForAssetLoadingToFinish1 = true;
                synchronized (GameLoadingState.this.assetLock1) {
                    while (GameLoadingState.this.waitForAssetLoadingToFinish1) {
                        try {
                            GameLoadingState.this.assetLock1.wait();
                        } catch (InterruptedException var9) {
                        }
                    }
                }

                boolean success = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator).mkdir();
                BaseVehicle.LoadAllVehicleTextures();
                if (GameClient.client) {
                    GameClient.instance.GameLoadingRequestData();
                }

                TutorialManager.instance = new TutorialManager();
                GameTime.setInstance(new GameTime());
                ClimateManager.setInstance(new ClimateManager());
                String spawnRegion = IsoWorld.instance.getSpawnRegion();
                IsoWorld.instance = new IsoWorld();
                IsoWorld.instance.setSpawnRegion(spawnRegion);
                DebugOptions.testThreadCrash(0);
                IsoWorld.instance.init();
                if (GameWindow.serverDisconnected) {
                    GameLoadingState.done = true;
                } else if (!GameLoadingState.playerWrongIP) {
                    if (!GameLoadingState.worldVersionError) {
                        DebugLog.General.println("triggerEvent OnGameTimeLoaded");
                        LuaEventManager.triggerEvent("OnGameTimeLoaded");
                        DebugLog.General.println("GlobalObjects.initSystems() start");
                        SGlobalObjects.initSystems();
                        CGlobalObjects.initSystems();
                        DebugLog.General.println("GlobalObjects.initSystems() end");
                        IsoObjectPicker.Instance.Init();
                        TutorialManager.instance.init();
                        TutorialManager.instance.CreateQuests();
                        File inFilex = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");
                        if (inFilex.exists()) {
                        }

                        if (!GameServer.server) {
                            inFilex = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");
                            boolean newGame = !inFilex.exists();
                            if (newGame || IsoWorld.savedWorldVersion != 240) {
                                if (!newGame) {
                                    GameLoadingState.gameLoadingString = "Saving converted world.";
                                }

                                try {
                                    DebugLog.General.println("GameWindow.save() start");
                                    GameWindow.save(true);
                                    DebugLog.General.println("GameWindow.save() end");
                                } catch (Throwable var8) {
                                    ExceptionLogger.logException(var8);
                                }
                            }
                        }

                        ChatUtility.InitAllowedChatIcons();
                        ChatManager.getInstance().init(true, IsoPlayer.getInstance());
                        Bullet.startLoadingPhysicsMeshes();
                        Texture.getSharedTexture("media/textures/NewShadow.png");
                        Texture.getSharedTexture("media/wallcutaways.png", 3);
                        DebugLog.General.println("bWaitForAssetLoadingToFinish2 start");
                        GameLoadingState.this.waitForAssetLoadingToFinish2 = true;
                        synchronized (GameLoadingState.this.assetLock2) {
                            while (GameLoadingState.this.waitForAssetLoadingToFinish2) {
                                try {
                                    GameLoadingState.this.assetLock2.wait();
                                } catch (InterruptedException var7) {
                                }
                            }
                        }

                        DebugLog.General.println("bWaitForAssetLoadingToFinish2 end");
                        if (PerformanceSettings.fboRenderChunk) {
                            DebugLog.General.println("FBORenderChunkManager.gameLoaded() start");
                            FBORenderChunkManager.instance.gameLoaded();
                            DebugLog.General.println("FBORenderChunkManager.gameLoaded() end");
                        }

                        DebugLog.General.println("Bullet.initPhysicsMeshes() start");
                        Bullet.initPhysicsMeshes();
                        DebugLog.General.println("Bullet.initPhysicsMeshes() end");
                        GameLoadingState.playerCreated = true;
                        GameLoadingState.gameLoadingString = "";
                        GameLoadingState.SendDone();
                    }
                }
            }
        });
        UIManager.suspend = true;
        loader.setName("GameLoadingThread");
        loader.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        loader.start();
    }

    public static void SendDone() {
        DebugLog.log("game loading took " + (System.currentTimeMillis() - startTime + 999L) / 1000L + " seconds");
        if (!GameClient.client) {
            done = true;
            GameKeyboard.noEventsWhileLoading = false;
        } else {
            GameClient.instance.sendLoginQueueDone(System.currentTimeMillis() - startTime);
        }
    }

    public static void Done() {
        done = true;
        GameKeyboard.noEventsWhileLoading = false;
    }

    @Override
    public GameState redirectState() {
        return new IngameState();
    }

    @Override
    public void exit() {
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        screenFader.startFadeToBlack();

        while (screenFader.isFading()) {
            screenFader.preRender();
            screenFader.postRender();
            if (screenFader.isFading()) {
                try {
                    Thread.sleep(33L);
                } catch (Exception var5) {
                }
            }
        }

        UIManager.useUiFbo = useUIFBO;
        ServerBrowser.setSuppressLuaCallbacks(false);
        if (GameClient.client) {
            NetworkAIParams.Init();
        }

        UIManager.init();
        LuaEventManager.triggerEvent("OnCreatePlayer", 0, IsoPlayer.players[0]);
        loader = null;
        done = false;
        this.stage = 0;
        IsoCamera.SetCharacterToFollow(IsoPlayer.getInstance());
        if (GameClient.client && !ServerOptions.instance.safehouseAllowTrepass.getValue()) {
            SafeHouse safe = SafeHouse.isSafeHouse(IsoPlayer.getInstance().getCurrentSquare(), GameClient.username, true);
            if (safe != null) {
                IsoPlayer.getInstance().setX(safe.getX() - 1.0F);
                IsoPlayer.getInstance().setY(safe.getY() - 1.0F);
            }
        }

        SoundManager.instance.stopMusic("");
        AmbientStreamManager.instance.init();
        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().isAsleep()) {
            UIManager.setFadeBeforeUI(IsoPlayer.getInstance().getPlayerNum(), true);
            UIManager.FadeOut(IsoPlayer.getInstance().getPlayerNum(), 2.0);
            UIManager.setFadeTime(IsoPlayer.getInstance().getPlayerNum(), 0.0);
            UIManager.getSpeedControls().SetCurrentGameSpeed(3);
        }

        if (!GameClient.client) {
            ActiveMods activeMods = ActiveMods.getById("currentGame");
            activeMods.checkMissingMods();
            activeMods.checkMissingMaps();
            ActiveMods.setLoadedMods(activeMods);
            String path = ZomboidFileSystem.instance.getFileNameInCurrentSave("mods.txt");
            ActiveModsFile activeModsFile = new ActiveModsFile();
            activeModsFile.write(path, activeMods);
        }

        DebugLog.log("Game Mode: " + Core.gameMode);
        DebugLog.log("Sandbox Options:");
        SandboxOptions options = LuaManager.GlobalObject.getSandboxOptions();

        for (int i = 0; i < options.getNumOptions(); i++) {
            SandboxOptions.SandboxOption option = options.getOptionByIndex(i);
            DebugLog.log(option.getShortName() + " " + option.asConfigOption().getValueAsString());
        }

        GameWindow.okToSaveOnExit = true;
    }

    @Override
    public void render() {
        float font_height_small = TextManager.instance.getFontHeight(UIFont.NewSmall);
        float font_height_medium = TextManager.instance.getFontHeight(UIFont.NewMedium);
        this.loadingDotTick = this.loadingDotTick + GameTime.getInstance().getMultiplier();
        if (this.loadingDotTick > 20.0F) {
            this.loadingDot = ".";
        }

        if (this.loadingDotTick > 40.0F) {
            this.loadingDot = "..";
        }

        if (this.loadingDotTick > 60.0F) {
            this.loadingDot = "...";
        }

        if (this.loadingDotTick > 80.0F) {
            this.loadingDot = "";
            this.loadingDotTick = 0.0F;
        }

        this.time = this.time + GameTime.instance.getTimeDelta();
        float alpha1 = 0.0F;
        float alpha2 = 0.0F;
        float alpha3 = 0.0F;
        if (this.stage == 0) {
            float pos = this.time;
            float textstart = 0.0F;
            float textfull = 1.0F;
            float textfullend = 5.0F;
            float textend = 7.0F;
            float del = 0.0F;
            if (pos > 0.0F && pos < 1.0F) {
                del = (pos - 0.0F) / 1.0F;
            }

            if (pos >= 1.0F && pos <= 5.0F) {
                del = 1.0F;
            }

            if (pos > 5.0F && pos < 7.0F) {
                del = 1.0F - (pos - 5.0F) / 2.0F;
            }

            if (pos >= 7.0F) {
                this.stage++;
            }

            alpha1 = del;
        }

        if (this.stage == 1) {
            float posx = this.time;
            float textstartx = 7.0F;
            float textfullx = 8.0F;
            float textfullendx = 13.0F;
            float textendx = 15.0F;
            float delx = 0.0F;
            if (posx > 7.0F && posx < 8.0F) {
                delx = (posx - 7.0F) / 1.0F;
            }

            if (posx >= 8.0F && posx <= 13.0F) {
                delx = 1.0F;
            }

            if (posx > 13.0F && posx < 15.0F) {
                delx = 1.0F - (posx - 13.0F) / 2.0F;
            }

            if (posx >= 15.0F) {
                this.stage++;
            }

            alpha2 = delx;
        }

        if (this.stage == 2) {
            float posxx = this.time;
            float textstartxx = 15.0F;
            float textfullxx = 16.0F;
            float textfullendxx = 31.0F;
            float textendxx = 33.0F;
            float delxx = 0.0F;
            if (posxx > 15.0F && posxx < 16.0F) {
                delxx = (posxx - 15.0F) / 1.0F;
            }

            if (posxx >= 16.0F && posxx <= 31.0F) {
                delxx = 1.0F;
            }

            if (posxx > 31.0F && posxx < 33.0F) {
                delxx = 1.0F - (posxx - 31.0F) / 2.0F;
            }

            if (posxx >= 33.0F) {
                this.stage++;
            }

            alpha3 = delxx;
        }

        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        Core.getInstance().StartFrameUI();
        SpriteRenderer.instance.renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0F, 0.0F, 0.0F, 1.0F, null);
        if (this.stage == -1) {
            this.renderProgressIndicator();
            screenFader.update();
            screenFader.render();
        }

        if (mapDownloadFailed) {
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int top = cy - mediumHgt / 2;
            String reason = Translator.getText("UI_GameLoad_MapDownloadFailed");
            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, reason, 0.8, 0.1, 0.1, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
        } else if (unexpectedError) {
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int smallHgt = TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight();
            int pad1 = 8;
            int pad2 = 2;
            int dy = mediumHgt + 8 + smallHgt + 2 + smallHgt;
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int top = cy - dy / 2;
            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, Translator.getText("UI_GameLoad_UnexpectedError1"), 0.8, 0.1, 0.1, 1.0);
            TextManager.instance
                .DrawStringCentre(UIFont.Small, cx, top + mediumHgt + 8, Translator.getText("UI_GameLoad_UnexpectedError2"), 1.0, 1.0, 1.0, 1.0);
            String consoleDotTxt = ZomboidFileSystem.instance.getCacheDir() + File.separator + "console.txt";
            TextManager.instance.DrawStringCentre(UIFont.Small, cx, top + mediumHgt + 8 + smallHgt + 2, consoleDotTxt, 1.0, 1.0, 1.0, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
        } else if (GameWindow.serverDisconnected) {
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int pad = 2;
            int top = cy - (mediumHgt + 2 + mediumHgt) / 2;
            String reason = GameWindow.kickReason;
            if (reason == null) {
                reason = Translator.getText("UI_OnConnectFailed_ConnectionLost");
            }

            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, reason, 0.8, 0.1, 0.1, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
        } else {
            if (worldVersionError) {
                if (WorldConverter.convertingVersion == 0) {
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Small,
                            Core.getInstance().getScreenWidth() / 2,
                            Core.getInstance().getScreenHeight() - 100,
                            Translator.getText("UI_CorruptedWorldVersion"),
                            0.8,
                            0.1,
                            0.1,
                            1.0
                        );
                } else if (WorldConverter.convertingVersion < 1) {
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Small,
                            Core.getInstance().getScreenWidth() / 2,
                            Core.getInstance().getScreenHeight() - 100,
                            Translator.getText("UI_ConvertWorldFailure"),
                            0.8,
                            0.1,
                            0.1,
                            1.0
                        );
                }
            } else if (convertingWorld) {
                TextManager.instance
                    .DrawStringCentre(
                        UIFont.Small,
                        Core.getInstance().getScreenWidth() / 2,
                        Core.getInstance().getScreenHeight() - 100,
                        Translator.getText("UI_ConvertWorld"),
                        0.5,
                        0.5,
                        0.5,
                        1.0
                    );
                if (convertingFileMax != -1) {
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Small,
                            Core.getInstance().getScreenWidth() / 2,
                            Core.getInstance().getScreenHeight() - 100 + TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight() + 8,
                            convertingFileCount + " / " + convertingFileMax,
                            0.5,
                            0.5,
                            0.5,
                            1.0
                        );
                }
            }

            if (playerWrongIP) {
                int cx = Core.getInstance().getScreenWidth() / 2;
                int cy = Core.getInstance().getScreenHeight() / 2;
                int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
                int pad = 2;
                int top = cy - (mediumHgt + 2 + mediumHgt) / 2;
                String str = gameLoadingString;
                if (gameLoadingString == null) {
                    str = "";
                }

                TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, str, 0.8, 0.1, 0.1, 1.0);
                UIManager.render();
                Core.getInstance().EndFrameUI();
            } else {
                if (GameClient.client) {
                    String str = gameLoadingString;
                    if (gameLoadingString == null) {
                        str = "";
                    }

                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Small,
                            Core.getInstance().getScreenWidth() / 2,
                            Core.getInstance().getScreenHeight() - 40 - font_height_small - 5.0F,
                            str,
                            0.5,
                            0.5,
                            0.5,
                            1.0
                        );
                    if (GameClient.connection.getConnectionType() == UdpConnection.ConnectionType.Steam) {
                        SpriteRenderer.instance
                            .render(
                                null,
                                (Core.getInstance().getScreenWidth() - this.width) / 2.0F,
                                Core.getInstance().getScreenHeight() - 40 - font_height_small * 2.0F - 5.0F,
                                this.width,
                                18.0F,
                                1.0F,
                                0.4F,
                                0.35F,
                                0.8F,
                                null
                            );
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                Core.getInstance().getScreenWidth() / 2,
                                Core.getInstance().getScreenHeight() - 40 - font_height_small * 2.0F - 5.0F,
                                this.text,
                                0.1,
                                0.1,
                                0.1,
                                1.0
                            );
                    }
                } else if (!playerCreated && newGame && !Core.isLastStand()) {
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.NewSmall,
                            Core.getInstance().getScreenWidth() / 2,
                            Core.getInstance().getScreenHeight() - 40 - font_height_small - 5.0F,
                            Translator.getText("UI_Loading").replace(".", ""),
                            0.5,
                            0.5,
                            0.5,
                            1.0
                        );
                    TextManager.instance
                        .DrawString(
                            UIFont.NewSmall,
                            Core.getInstance().getScreenWidth() / 2
                                + TextManager.instance.MeasureStringX(UIFont.Small, Translator.getText("UI_Loading").replace(".", "")) / 2
                                + 1,
                            Core.getInstance().getScreenHeight() - 40 - font_height_small - 5.0F,
                            this.loadingDot,
                            0.5,
                            0.5,
                            0.5,
                            1.0
                        );
                }

                this.doQuickTips();
                if (this.stage == 0) {
                    int x = Core.getInstance().getScreenWidth() / 2;
                    int y = Core.getInstance().getScreenHeight() / 2 - TextManager.instance.getFontFromEnum(UIFont.Intro).getLineHeight() / 2;
                    TextManager.instance.DrawStringCentre(UIFont.Intro, x, y, Translator.getText("UI_Intro1"), 1.0, 1.0, 1.0, alpha1);
                }

                if (this.stage == 1) {
                    int x = Core.getInstance().getScreenWidth() / 2;
                    int y = Core.getInstance().getScreenHeight() / 2 - TextManager.instance.getFontFromEnum(UIFont.Intro).getLineHeight() / 2;
                    TextManager.instance.DrawStringCentre(UIFont.Intro, x, y, Translator.getText("UI_Intro2"), 1.0, 1.0, 1.0, alpha2);
                }

                if (this.stage == 2) {
                    int x = Core.getInstance().getScreenWidth() / 2;
                    int y = Core.getInstance().getScreenHeight() / 2 - TextManager.instance.getFontFromEnum(UIFont.Intro).getLineHeight() / 2;
                    TextManager.instance.DrawStringCentre(UIFont.Intro, x, y, Translator.getText("UI_Intro3"), 1.0, 1.0, 1.0, alpha3);
                }

                if (Core.getInstance().getDebug()) {
                    showedClickToSkip = true;
                }

                if (done && playerCreated && (!newGame || this.time >= 33.0F || Core.isLastStand() || "Tutorial".equals(Core.gameMode))) {
                    if (this.clickToSkipFadeIn) {
                        this.clickToSkipAlpha = this.clickToSkipAlpha + GameTime.getInstance().getThirtyFPSMultiplier() / 30.0F;
                        if (this.clickToSkipAlpha > 1.0F) {
                            this.clickToSkipAlpha = 1.0F;
                            this.clickToSkipFadeIn = false;
                        }
                    } else {
                        showedClickToSkip = true;
                        this.clickToSkipAlpha = this.clickToSkipAlpha - GameTime.getInstance().getThirtyFPSMultiplier() / 30.0F;
                        if (this.clickToSkipAlpha < 0.25F) {
                            this.clickToSkipFadeIn = true;
                        }
                    }

                    int baseline = Core.getInstance().getScreenHeight();
                    if (GameWindow.activatedJoyPad != null && !JoypadManager.instance.joypadList.isEmpty()) {
                        String textureType;
                        if (Core.getInstance().getOptionControllerButtonStyle() == 1) {
                            textureType = "XBOX";
                        } else {
                            textureType = "PS4";
                        }

                        Texture tex = Texture.getSharedTexture("media/ui/controller/" + textureType + "_A.png");
                        if (tex != null) {
                            int fontHgt = TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight();
                            SpriteRenderer.instance
                                .renderi(
                                    tex,
                                    Core.getInstance().getScreenWidth() / 2
                                        - TextManager.instance.MeasureStringX(UIFont.Small, Translator.getText("UI_PressAToStart")) / 2
                                        - 8
                                        - tex.getWidth(),
                                    baseline - 60 + fontHgt / 2 - tex.getHeight() / 2,
                                    tex.getWidth(),
                                    tex.getHeight(),
                                    1.0F,
                                    1.0F,
                                    1.0F,
                                    this.clickToSkipAlpha,
                                    null
                                );
                        }

                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Small,
                                Core.getInstance().getScreenWidth() / 2,
                                baseline - 40 - font_height_small - 5.0F,
                                Translator.getText("UI_PressAToStart"),
                                1.0,
                                1.0,
                                1.0,
                                this.clickToSkipAlpha
                            );
                    } else {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.NewLarge,
                                Core.getInstance().getScreenWidth() / 2,
                                baseline - 40 - font_height_small - 5.0F,
                                Translator.getText("UI_ClickToSkip"),
                                1.0,
                                1.0,
                                1.0,
                                this.clickToSkipAlpha
                            );
                    }
                }

                ActiveMods.renderUI();
                Core.getInstance().EndFrameUI();
                UIManager.useUiFbo = useUIFBO;
            }
        }
    }

    private void doQuickTips() {
        if (!newGame) {
            if (this.quickTipsTimer > 720.0F) {
                this.quickTipsText = this.getNewQuickTip();
                this.quickTipsTimer = 0.0F;
            }

            this.quickTipsTimer = this.quickTipsTimer + GameTime.getInstance().getMultiplier();
            if (!StringUtils.isNullOrEmpty(this.quickTipsText)) {
                TextManager.instance
                    .DrawStringCentre(
                        UIFont.NewMedium,
                        Core.getInstance().getScreenWidth() / 2.0,
                        Core.getInstance().getScreenHeight() - 40,
                        this.quickTipsText,
                        0.5,
                        0.5,
                        0.5,
                        1.0
                    );
            }
        }
    }

    private String getNewQuickTip() {
        if (!this.quickTipsList.isEmpty() && !this.quickTipsListJoke.isEmpty()) {
            String quickTip = this.quickTipsList.get(Rand.Next(this.quickTipsList.size()));
            if (Rand.NextBool(13)) {
                quickTip = this.quickTipsListJoke.get(Rand.Next(this.quickTipsListJoke.size()));
            }

            return quickTip;
        } else {
            return null;
        }
    }

    private void loadQuickTipList() {
        this.quickTipsList = new ArrayList<>();
        this.quickTipsListJoke = new ArrayList<>();

        for (Entry<String, String> entry : Translator.getUI().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("UI_quick_tip_joke")) {
                this.quickTipsListJoke.add(entry.getValue());
            } else if (key.startsWith("UI_quick_tip")) {
                this.quickTipsList.add(entry.getValue());
            }
        }
    }

    private void renderProgressIndicator() {
        if (!unexpectedError) {
            if (convertingWorld) {
                this.animatedTexture = AnimatedTextures.getTexture("media/ui/Progress/MaleDoor.png");
            } else if (SandboxOptions.instance.lore.speed.getValue() == 1) {
                this.animatedTexture = AnimatedTextures.getTexture("media/ui/Progress/MaleSprint06.png");
            } else {
                this.animatedTexture = AnimatedTextures.getTexture("media/ui/Progress/MaleWalk2.png");
            }

            if (this.animatedTexture.isReady()) {
                int width = 196;
                float SCALE = 196.0F / this.animatedTexture.getWidth();
                int height = (int)(this.animatedTexture.getHeight() * SCALE);
                float alpha = 0.66F;
                if (done && showedClickToSkip) {
                    if (this.progressFadeStartMs == 0L) {
                        this.progressFadeStartMs = System.currentTimeMillis();
                    }

                    long FADE_TIME = 200L;
                    long dt = PZMath.clamp(System.currentTimeMillis() - this.progressFadeStartMs, 0L, 200L);
                    alpha *= 1.0F - (float)dt / 200.0F;
                    if (alpha == 0.0F) {
                        return;
                    }
                }

                int textY = Core.getInstance().getScreenHeight() - (convertingWorld ? 100 : 0);
                this.animatedTexture
                    .render(
                        Core.getInstance().getScreenWidth() / 2 - 98,
                        PZMath.min(Core.getInstance().getScreenHeight(), textY) - height - 30 + (convertingWorld ? 32 : 0),
                        196,
                        height,
                        1.0F,
                        1.0F,
                        1.0F,
                        alpha
                    );
            }
        }
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (this.waitForAssetLoadingToFinish1 && !OutfitManager.instance.isLoadingClothingItems()) {
            if (Core.debug) {
                OutfitManager.instance.debugOutfits();
            }

            synchronized (this.assetLock1) {
                this.waitForAssetLoadingToFinish1 = false;
                this.assetLock1.notifyAll();
            }
        }

        if (this.waitForAssetLoadingToFinish2 && !ModelManager.instance.isLoadingAnimations() && !GameWindow.fileSystem.hasWork()) {
            synchronized (this.assetLock2) {
                this.waitForAssetLoadingToFinish2 = false;
                this.assetLock2.notifyAll();

                for (RuntimeAnimationScript runtimeAnimationScript : ScriptManager.instance.getAllRuntimeAnimationScripts()) {
                    runtimeAnimationScript.exec();
                }
            }
        }

        if (!unexpectedError && !GameWindow.serverDisconnected && !playerWrongIP) {
            if (!done) {
                return GameStateMachine.StateAction.Remain;
            } else if (WorldStreamer.instance.isBusy()) {
                return GameStateMachine.StateAction.Remain;
            } else if (ModelManager.instance.isLoadingAnimations()) {
                return GameStateMachine.StateAction.Remain;
            } else if (!showedClickToSkip) {
                return GameStateMachine.StateAction.Remain;
            } else {
                if (Mouse.isButtonDown(0)) {
                    this.forceDone = true;
                }

                if (GameWindow.activatedJoyPad != null && GameWindow.activatedJoyPad.isAPressed()) {
                    this.forceDone = true;
                }

                if (this.forceDone) {
                    SoundManager.instance.playUISound("UIClickToStart");
                    this.forceDone = false;
                    return GameStateMachine.StateAction.Continue;
                } else {
                    return GameStateMachine.StateAction.Remain;
                }
            }
        } else {
            if (!showedUI) {
                showedUI = true;
                IsoPlayer.setInstance(null);
                IsoPlayer.players[0] = null;
                UIManager.UI.clear();
                LuaManager.thread.debugOwnerThread = GameWindow.gameThread;
                LuaManager.debugthread.debugOwnerThread = GameWindow.gameThread;
                LuaEventManager.Reset();
                LuaManager.call("ISGameLoadingUI_OnGameLoadingUI", "");
                UIManager.suspend = false;
            }

            if (GameKeyboard.isKeyDownRaw(1)) {
                GameClient.instance.Shutdown();
                SteamUtils.shutdown();
                System.exit(1);
            }

            return GameStateMachine.StateAction.Remain;
        }
    }
}
