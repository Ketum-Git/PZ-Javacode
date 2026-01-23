// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import com.sun.management.OperatingSystemMXBean;
import fmod.fmod.Audio;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjglx.LWJGLException;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.OpenGLException;
import zombie.DebugFileWatcher;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.LocaleManager;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.asset.AssetManagers;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.ProxyPrintStream;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LimitSizeFileOutputStream;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZipLogs;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAsset;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAssetManager;
import zombie.core.skinnedmodel.model.AiSceneAsset;
import zombie.core.skinnedmodel.model.AiSceneAssetManager;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.AnimationAssetManager;
import zombie.core.skinnedmodel.model.MeshAssetManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelAssetManager;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemAssetManager;
import zombie.core.textures.AnimatedTexture;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.AnimatedTextureIDAssetManager;
import zombie.core.textures.AnimatedTextures;
import zombie.core.textures.NinePatchTexture;
import zombie.core.textures.NinePatchTextureAssetManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureAssetManager;
import zombie.core.textures.TextureID;
import zombie.core.textures.TextureIDAssetManager;
import zombie.core.textures.VideoTexture;
import zombie.core.znet.ServerBrowser;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.entity.components.attributes.Attribute;
import zombie.input.JoypadManager;
import zombie.modding.ActiveMods;
import zombie.network.CustomizationManager;
import zombie.network.GameClient;
import zombie.network.statistics.StatisticManager;
import zombie.ui.ScreenFader;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapDataAssetManager;

@UsedFromLua
public final class MainScreenState extends GameState {
    public static final String VERSION = "RC 3";
    public static Audio ambient;
    public static float totalScale = 1.0F;
    public float alpha = 1.0F;
    public float alphaStep = 0.03F;
    private int restartDebounceClickTimer = 10;
    public final ArrayList<MainScreenState.ScreenElement> elements = new ArrayList<>(16);
    public float targetAlpha = 1.0F;
    int lastH;
    int lastW;
    MainScreenState.ScreenElement logo;
    private ScreenFader screenFader;
    private VideoTexture videoTex;
    private VideoTexture videoTex2;
    private static final long MIN_MEM_VIDEO_EFFECTS = 8589934592L;
    public static MainScreenState instance;
    public boolean showLogo;
    private float fadeAlpha;
    public boolean lightningTimelineMarker;
    float lightningTime;
    public UIWorldMap worldMap;
    public float lightningDelta;
    public float lightningTargetDelta;
    public float lightningFullTimer;
    public float lightningCount;
    public float lightOffCount;
    private AnimatedTexture animatedTexture;
    private ConnectToServerState connectToServerState;
    private static GLFWImage windowIcon1;
    private static GLFWImage windowIcon2;
    private static ByteBuffer windowIconBB1;
    private static ByteBuffer windowIconBB2;

    public static void main(String[] args) {
        LocaleManager.initialise();
        String consoleDotTxtSizeString = System.getProperty("zomboid.ConsoleDotTxtSizeKB");
        Core.getInstance().setConsoleDotTxtSizeKB(consoleDotTxtSizeString);

        for (int n = 0; n < args.length; n++) {
            if (args[n] != null) {
                if (args[n].startsWith("-cachedir=")) {
                    ZomboidFileSystem.instance.setCacheDir(args[n].replace("-cachedir=", "").trim());
                }

                if (args[n].startsWith("-console_dot_txt_size_kb=")) {
                    consoleDotTxtSizeString = args[n].replace("-console_dot_txt_size_kb=", "").trim();
                    Core.getInstance().setConsoleDotTxtSizeKB(consoleDotTxtSizeString);
                }
            }
        }

        ZipLogs.addZipFile(false);

        try {
            String logFileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "console.txt";
            LimitSizeFileOutputStream fout = new LimitSizeFileOutputStream(new File(logFileName), Core.getInstance().getConsoleDotTxtSizeKB());
            PrintStream fileStream = new PrintStream(fout, true);
            System.setOut(new ProxyPrintStream(System.out, fileStream));
            System.setErr(new ProxyPrintStream(System.err, fileStream));
        } catch (FileNotFoundException var12) {
            var12.printStackTrace();
        }

        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        DebugLog.init();
        LoggerManager.init();
        JAssImpImporter.Init();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        System.out.println(sdf.format(Calendar.getInstance().getTime()));
        DebugLog.DetailedInfo.trace("cachedir is \"" + ZomboidFileSystem.instance.getCacheDir() + "\"");
        DebugLog.DetailedInfo.trace("LogFileDir is \"" + LoggerManager.getLogsDir() + "\"");
        printSpecs();
        DebugLog.General.debugln("-- listing properties --");

        for (Entry<Object, Object> e : System.getProperties().entrySet()) {
            String key = (String)e.getKey();
            String val = (String)e.getValue();
            if (val.length() > 40) {
                val = val.substring(0, 37) + "...";
            }

            if (!key.contains("user") && !key.contains("path") && !key.contains("dir")) {
                DebugLog.General.println(key + "=" + val);
            } else {
                DebugLog.DetailedInfo.trace(key + "=" + val);
            }
        }

        System.out.println("-----");
        System.out.println("version=" + Core.getInstance().getVersion() + " demo=false");
        if (!"986d733f73d95810492027afa9594e1a62502a48".isEmpty()) {
            DebugLog.General.println("revision=%s date=%s time=%s (%s)", "986d733f73d95810492027afa9594e1a62502a48", "2026-01-19", "12:14:48", "ZB");
        }

        Display.setIcon(loadIcons());
        String debugcfg = null;

        for (int nx = 0; nx < args.length; nx++) {
            if (args[nx] != null) {
                if (args[nx].contains("safemode")) {
                    Core.safeMode = true;
                    Core.safeModeForced = true;
                } else if (args[nx].equals("-nosound")) {
                    Core.soundDisabled = true;
                } else if (args[nx].equals("-aitest")) {
                    IsoPlayer.isTestAIMode = true;
                } else if (args[nx].equals("-novoip")) {
                    VoiceManager.voipDisabled = true;
                } else if (args[nx].equals("-debug")) {
                    Core.debug = true;
                } else if (args[nx].equals("-imguidebugviewports")) {
                    Core.useViewports = true;
                    Core.debug = true;
                    Core.imGui = true;
                } else if (args[nx].equals("-imgui")) {
                    Core.imGui = true;
                    Core.debug = true;
                } else if (!args[nx].startsWith("-debuglog=")) {
                    if (!args[nx].startsWith("-cachedir=")) {
                        if (args[nx].equals("+connect")) {
                            if (nx + 1 < args.length) {
                                System.setProperty("args.server.connect", args[nx + 1]);
                            }

                            nx++;
                        } else if (args[nx].equals("+password")) {
                            if (nx + 1 < args.length) {
                                System.setProperty("args.server.password", args[nx + 1]);
                            }

                            nx++;
                        } else if (args[nx].contains("-debugtranslation")) {
                            Translator.debug = true;
                        } else if ("-modfolders".equals(args[nx])) {
                            if (nx + 1 < args.length) {
                                ZomboidFileSystem.instance.setModFoldersOrder(args[nx + 1]);
                            }

                            nx++;
                        } else if (args[nx].equals("-nosteam")) {
                            System.setProperty("zomboid.steam", "0");
                        } else if (args[nx].startsWith("-debugcfg=")) {
                            debugcfg = args[nx].replace("-debugcfg=", "");
                        } else {
                            DebugLog.log("unknown option \"" + args[nx] + "\"");
                        }
                    }
                } else {
                    for (String t : args[nx].replace("-debuglog=", "").split(",")) {
                        try {
                            char firstChar = t.charAt(0);
                            t = firstChar != '+' && firstChar != '-' ? t : t.substring(1);
                            DebugLog.setLogEnabled(DebugType.valueOf(t), firstChar != '-');
                        } catch (IllegalArgumentException var13) {
                        }
                    }
                }
            }
        }

        if (Core.debug || System.getProperty("debug") != null) {
            DebugLog.loadDebugConfig(debugcfg);
        }

        DebugLog.printLogLevels();
        StatisticManager.getInstance().init();
        if (Core.debug || System.getProperty("debug") != null) {
            Attribute.init();
        }

        try {
            RenderThread.init();
            AssetManagers assetManagers = GameWindow.assetManagers;
            AiSceneAssetManager.instance.create(AiSceneAsset.ASSET_TYPE, assetManagers);
            AnimatedTextureIDAssetManager.instance.create(AnimatedTextureID.ASSET_TYPE, assetManagers);
            AnimationAssetManager.instance.create(AnimationAsset.ASSET_TYPE, assetManagers);
            AnimNodeAssetManager.instance.create(AnimNodeAsset.ASSET_TYPE, assetManagers);
            ClothingItemAssetManager.instance.create(ClothingItem.ASSET_TYPE, assetManagers);
            MeshAssetManager.instance.create(ModelMesh.ASSET_TYPE, assetManagers);
            ModelAssetManager.instance.create(Model.ASSET_TYPE, assetManagers);
            NinePatchTextureAssetManager.instance.create(NinePatchTexture.ASSET_TYPE, assetManagers);
            PhysicsShapeAssetManager.instance.create(PhysicsShape.ASSET_TYPE, assetManagers);
            TextureIDAssetManager.instance.create(TextureID.ASSET_TYPE, assetManagers);
            TextureAssetManager.instance.create(Texture.ASSET_TYPE, assetManagers);
            WorldMapDataAssetManager.instance.create(WorldMapData.ASSET_TYPE, assetManagers);
            GameWindow.InitGameThread();
            RenderThread.renderLoop();
        } catch (LWJGLException | IOException | Error | OpenGLException var10) {
            onExceptionThrown_TryDeleteOptionsFile(var10);
        } catch (Exception var11) {
            DebugLog.General.printException(var11, "Exception thrown during MainScreenState.main.", LogSeverity.Error);
        }
    }

    private static void onExceptionThrown_TryDeleteOptionsFile(Throwable in_thrownException) {
        DebugLog.General.error("An error was encountered during startup. Attempting to delete options file. Please try running the game again.");
        DebugLog.General.printException(in_thrownException, "Exception thrown.", LogSeverity.Error);

        try {
            String optionsFilePath = ZomboidFileSystem.instance.getCacheDir() + File.separator + "options2.bin";
            File outFile = new File(optionsFilePath);
            boolean deleteSuccess = outFile.delete();
            if (!deleteSuccess) {
                throw new IOException("Failed to delete options file: " + optionsFilePath);
            }
        } catch (Exception var4) {
            DebugLog.General.printException(var4, "Failed to delete options file.", LogSeverity.Error);
        }
    }

    public static void DrawTexture(Texture tex, int x, int y, int width, int height, float alpha) {
        SpriteRenderer.instance.renderi(tex, x, y, width, height, 1.0F, 1.0F, 1.0F, alpha, null);
    }

    public static void DrawTexture(Texture tex, int x, int y, int width, int height, Color col) {
        SpriteRenderer.instance.renderi(tex, x, y, width, height, col.r, col.g, col.b, col.a, null);
    }

    @Override
    public void enter() {
        DebugType.ExitDebug.debugln("MainScreenState.enter 1");

        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
            long physicalMemorySize = os.getTotalMemorySize();
            if (physicalMemorySize < 8589934592L) {
                Core.getInstance().setOptionDoVideoEffects(false);
            }
        } catch (Throwable var5) {
        }

        GameClient.client = false;
        this.elements.clear();
        this.targetAlpha = 1.0F;
        TextureID.useFiltering = true;
        this.restartDebounceClickTimer = 100;
        totalScale = Core.getInstance().getOffscreenHeight(0) / 1080.0F;
        this.lastW = Core.getInstance().getOffscreenWidth(0);
        this.lastH = Core.getInstance().getOffscreenHeight(0);
        this.alpha = 1.0F;
        this.showLogo = false;
        SoundManager.instance.setMusicState("MainMenu");
        int y = (int)(Core.getInstance().getOffscreenHeight(0) * 0.7F);
        MainScreenState.ScreenElement el = new MainScreenState.ScreenElement(
            Texture.getSharedTexture("media/ui/PZ_Logo.png"),
            Core.getInstance().getOffscreenWidth(0) / 2 - (int)(Texture.getSharedTexture("media/ui/PZ_Logo.png").getWidth() * totalScale) / 2,
            y - (int)(350.0F * totalScale),
            0.0F,
            0.0F,
            1
        );
        el.targetAlpha = 1.0F;
        el.alphaStep *= 0.9F;
        this.logo = el;
        this.elements.add(el);
        TextureID.useFiltering = false;
        LuaEventManager.triggerEvent("OnMainMenuEnter");
        instance = this;
        float testK = TextureID.totalMemUsed / 1024.0F;
        float testM = testK / 1024.0F;
        if (Core.getInstance().getOptionDoVideoEffects()) {
        }

        DebugType.ExitDebug.debugln("MainScreenState.enter 2");
    }

    public static MainScreenState getInstance() {
        return instance;
    }

    public boolean ShouldShowLogo() {
        return this.showLogo;
    }

    @Override
    public void exit() {
        DebugType.ExitDebug.debugln("MainScreenState.exit 1");
        DebugLog.log("LOADED UP A TOTAL OF " + Texture.totalTextureID + " TEXTURES");
        if (SteamUtils.isSteamModeEnabled()) {
            ServerBrowser.Release();
        }

        float musicVolume = Core.getInstance().getOptionMusicVolume() / 10.0F;
        long startTime = Calendar.getInstance().getTimeInMillis();

        while (true) {
            this.fadeAlpha = Math.min(1.0F, (float)(Calendar.getInstance().getTimeInMillis() - startTime) / 250.0F);
            this.render();
            if (this.fadeAlpha >= 1.0F) {
                if (this.videoTex != null) {
                    this.videoTex.closeAndDestroy();
                    this.videoTex = null;
                }

                if (this.videoTex2 != null) {
                    this.videoTex2.closeAndDestroy();
                    this.videoTex2 = null;
                }

                SoundManager.instance.stopMusic("");
                SoundManager.instance.setMusicVolume(musicVolume);
                DebugType.ExitDebug.debugln("MainScreenState.exit 2");
                return;
            }

            try {
                Thread.sleep(33L);
            } catch (Exception var5) {
            }

            SoundManager.instance.Update();
        }
    }

    @Override
    public void render() {
        this.lightningTime = this.lightningTime + 1.0F * GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        Core.getInstance().StartFrameUI();
        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0F, 0.0F, 0.0F, 1.0F, null);
        IndieGL.glBlendFunc(770, 770);
        this.renderBackground();
        UIManager.render();
        if (GameWindow.drawReloadingLua) {
            int textWidth = TextManager.instance.MeasureStringX(UIFont.Small, "Reloading Lua") + 32;
            int fontHeight = TextManager.instance.font.getLineHeight();
            int textHeight = (int)Math.ceil(fontHeight * 1.5);
            SpriteRenderer.instance
                .renderi(null, Core.getInstance().getScreenWidth() - textWidth - 12, 12, textWidth, textHeight, 0.0F, 0.5F, 0.75F, 1.0F, null);
            TextManager.instance
                .DrawStringCentre(
                    Core.getInstance().getScreenWidth() - textWidth / 2 - 12, 12 + (textHeight - fontHeight) / 2, "Reloading Lua", 1.0, 1.0, 1.0, 1.0
                );
        }

        if (this.fadeAlpha > 0.0F) {
            UIManager.DrawTexture(UIManager.getBlack(), 0.0, 0.0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), this.fadeAlpha);
        }

        if (Core.debug) {
        }

        ActiveMods.renderUI();
        JoypadManager.instance.renderUI();
        if (this.screenFader == null) {
            this.screenFader = new ScreenFader();
            this.screenFader.startFadeFromBlack();
        }

        if (this.screenFader.isFading()) {
            this.screenFader.update();
            this.screenFader.render();
        }

        Core.getInstance().EndFrameUI();
        UIManager.useUiFbo = useUIFBO;
    }

    public static void preloadBackgroundTextures() {
        int flags = 3;
        flags |= TextureID.useCompression ? 4 : 0;
        Texture.getSharedTexture("media/ui/Title.png", flags);
        Texture.getSharedTexture("media/ui/Title2.png", flags);
        Texture.getSharedTexture("media/ui/Title3.png", flags);
        Texture.getSharedTexture("media/ui/Title4.png", flags);
        Texture.getSharedTexture("media/ui/Title_lightning.png", flags);
        Texture.getSharedTexture("media/ui/Title_lightning2.png", flags);
        Texture.getSharedTexture("media/ui/Title_lightning3.png", flags);
        Texture.getSharedTexture("media/ui/Title_lightning4.png", flags);
        AnimatedTextures.getTexture("media/ui/Progress/MaleDoor.png");
        AnimatedTextures.getTexture("media/ui/Progress/MaleSprint06.png");
        AnimatedTextures.getTexture("media/ui/Progress/MaleWalk2.png");
    }

    public void renderBackground() {
        if (this.lightningTargetDelta == 0.0F && this.lightningDelta != 0.0F && this.lightningDelta < 0.6F && this.lightningCount == 0.0F) {
            this.lightningTargetDelta = 1.0F;
            this.lightningCount = 1.0F;
        }

        if (this.lightningTimelineMarker) {
            this.lightningTimelineMarker = false;
            this.lightningTargetDelta = 1.0F;
        }

        if (this.lightningTargetDelta == 1.0F
            && this.lightningDelta == 1.0F
            && (this.lightningFullTimer > 1.0F && this.lightningCount == 0.0F || this.lightningFullTimer > 10.0F)) {
            this.lightningTargetDelta = 0.0F;
            this.lightningFullTimer = 0.0F;
        }

        if (this.lightningTargetDelta == 1.0F && this.lightningDelta == 1.0F) {
            this.lightningFullTimer = this.lightningFullTimer + GameTime.getInstance().getMultiplier();
        }

        if (this.lightningDelta != this.lightningTargetDelta) {
            if (this.lightningDelta < this.lightningTargetDelta) {
                this.lightningDelta = this.lightningDelta + 0.17F * GameTime.getInstance().getMultiplier();
                if (this.lightningDelta > this.lightningTargetDelta) {
                    this.lightningDelta = this.lightningTargetDelta;
                    if (this.lightningDelta == 1.0F) {
                        this.showLogo = true;
                    }
                }
            }

            if (this.lightningDelta > this.lightningTargetDelta) {
                this.lightningDelta = this.lightningDelta - 0.025F * GameTime.getInstance().getMultiplier();
                if (this.lightningCount == 0.0F) {
                    this.lightningDelta -= 0.1F;
                }

                if (this.lightningDelta < this.lightningTargetDelta) {
                    this.lightningDelta = this.lightningTargetDelta;
                    this.lightningCount = 0.0F;
                }
            }
        }

        if (Rand.Next(150) == 0) {
            this.lightOffCount = 10.0F;
        }

        float a = 1.0F - this.lightningDelta * 0.6F;
        if (Core.getInstance().getOptionDoVideoEffects()) {
        }

        if (this.videoTex != null) {
            this.videoTex.closeAndDestroy();
            this.videoTex = null;
        }

        if (this.videoTex2 != null) {
            this.videoTex2.closeAndDestroy();
            this.videoTex2 = null;
        }

        this.renderOriginalBackground(a);
    }

    private boolean renderVideo(float a) {
        if (this.videoTex == null) {
            this.videoTex = VideoTexture.getOrCreate("pztitletest.bk2", 2560, 1440);
            if (this.videoTex == null) {
                return false;
            }
        }

        if (this.videoTex2 == null) {
            this.videoTex2 = VideoTexture.getOrCreate("pztitletest_light.bk2", 2560, 1440);
            if (this.videoTex2 == null) {
                return false;
            }
        }

        if (this.videoTex.isValid() && this.videoTex2.isValid()) {
            this.videoTex.RenderFrame();
            this.videoTex2.RenderFrame();
            int height = Core.getInstance().getScreenHeight();
            int width = (int)(height * 16.0 / 9.0);
            int screenWidth = Core.getInstance().getScreenWidth();
            int xPos = screenWidth - width;
            DrawTexture(this.videoTex, xPos, 0, width, height, a);
            IndieGL.glBlendFunc(770, 1);
            DrawTexture(this.videoTex2, xPos, 0, width, height, this.lightningDelta);
            IndieGL.glBlendFunc(770, 771);
            return true;
        } else {
            return false;
        }
    }

    private void renderOriginalBackground(float a) {
        Texture l = Texture.getSharedTexture("media/ui/Title.png");
        Texture l2 = Texture.getSharedTexture("media/ui/Title2.png");
        Texture l3 = Texture.getSharedTexture("media/ui/Title3.png");
        Texture l4 = Texture.getSharedTexture("media/ui/Title4.png");
        Texture b = Texture.getSharedTexture("media/ui/Title_lightning.png");
        Texture b2 = Texture.getSharedTexture("media/ui/Title_lightning2.png");
        Texture b3 = Texture.getSharedTexture("media/ui/Title_lightning3.png");
        Texture b4 = Texture.getSharedTexture("media/ui/Title_lightning4.png");
        float he = Core.getInstance().getScreenHeight() / 1080.0F;
        float wi = l.getWidth() * he;
        float wi2 = l2.getWidth() * he;
        float bigger = Core.getInstance().getScreenWidth() - (wi + wi2);
        if (bigger >= 0.0F) {
            bigger = 0.0F;
        }

        float he1 = 1024.0F * he;
        float he2 = 56.0F * he;
        DrawTexture(l, (int)bigger, 0, (int)wi, (int)he1, a);
        DrawTexture(l2, (int)bigger + (int)wi, 0, (int)wi, (int)he1, a);
        DrawTexture(l3, (int)bigger, (int)he1, (int)wi, (int)(l3.getHeight() * he), a);
        DrawTexture(l4, (int)bigger + (int)wi, (int)he1, (int)wi, (int)(l3.getHeight() * he), a);
        IndieGL.glBlendFunc(770, 1);
        DrawTexture(b, (int)bigger, 0, (int)wi, (int)he1, this.lightningDelta);
        DrawTexture(b2, (int)bigger + (int)wi, 0, (int)wi, (int)he1, this.lightningDelta);
        DrawTexture(b3, (int)bigger, (int)he1, (int)wi, (int)he1, this.lightningDelta);
        DrawTexture(b4, (int)bigger + (int)wi, (int)he1, (int)wi, (int)he1, this.lightningDelta);
        IndieGL.glBlendFunc(770, 771);
    }

    private void renderNinePatchTextures() {
        float x = 10.0F;
        float y = 10.0F;
        int pad = 20;
        x = this.renderNinePatchTexture("media/ui/NinePatch1.png", x, 10.0F) + 20.0F;
        x = this.renderNinePatchTexture("media/ui/NinePatch2.png", x, 10.0F) + 20.0F;
        x = this.renderNinePatchTexture("media/ui/NinePatch3.png", x, 10.0F) + 20.0F;
        x = this.renderNinePatchTexture("media/ui/NinePatch4.png", x, 10.0F) + 20.0F;
    }

    private float renderNinePatchTexture(String path, float x, float y) {
        NinePatchTexture npt = NinePatchTexture.getSharedTexture(path);
        if (npt == null) {
            return x;
        } else if (!npt.isReady()) {
            return x;
        } else {
            int MINW = npt.getMinWidth();
            int MINH = npt.getMinHeight();
            int MAX = 200;
            long ms = System.currentTimeMillis();
            float wid = PZMath.lerp(MINW, 200.0F, (float)Math.sin(ms / 1500.0) + 1.0F);
            float hgt = PZMath.lerp(MINH, 200.0F, (float)Math.cos(ms / 1500.0) + 1.0F);
            npt.render(x, y, wid, hgt);
            return x + wid;
        }
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (this.connectToServerState != null) {
            GameStateMachine.StateAction action = this.connectToServerState.update();
            if (action == GameStateMachine.StateAction.Continue) {
                this.connectToServerState.exit();
                this.connectToServerState = null;
                return GameStateMachine.StateAction.Remain;
            }
        }

        LuaEventManager.triggerEvent("OnFETick", BoxedStaticValues.toDouble(0.0));
        if (this.restartDebounceClickTimer > 0) {
            this.restartDebounceClickTimer--;
        }

        for (int n = 0; n < this.elements.size(); n++) {
            MainScreenState.ScreenElement el = this.elements.get(n);
            el.update();
        }

        this.lastW = Core.getInstance().getOffscreenWidth(0);
        this.lastH = Core.getInstance().getOffscreenHeight(0);
        DebugFileWatcher.instance.update();
        ZomboidFileSystem.instance.update();

        try {
            Core.getInstance().CheckDelayResetLua();
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
        }

        return GameStateMachine.StateAction.Remain;
    }

    public void setConnectToServerState(ConnectToServerState state) {
        this.connectToServerState = state;
    }

    @Override
    public GameState redirectState() {
        return null;
    }

    public static Buffer loadIcons() {
        Buffer imageBuffer = null;
        String OS = System.getProperty("os.name").toUpperCase(Locale.ENGLISH);
        if (OS.contains("WIN")) {
            try {
                imageBuffer = GLFWImage.create(2);
                BufferedImage bufferedImage = ImageIO.read(new File("media" + File.separator + "ui" + File.separator + "zomboidIcon16.png").getAbsoluteFile());
                ByteBuffer byteBuffer;
                windowIconBB1 = byteBuffer = loadInstance(bufferedImage, 16);
                GLFWImage image;
                windowIcon1 = image = GLFWImage.create().set(16, 16, byteBuffer);
                imageBuffer.put(0, image);
                bufferedImage = ImageIO.read(new File("media" + File.separator + "ui" + File.separator + "zomboidIcon32.png").getAbsoluteFile());
                windowIconBB2 = byteBuffer = loadInstance(bufferedImage, 32);
                windowIcon2 = image = GLFWImage.create().set(32, 32, byteBuffer);
                imageBuffer.put(1, image);
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        } else if (OS.contains("MAC")) {
            try {
                imageBuffer = GLFWImage.create(1);
                BufferedImage bufferedImage = ImageIO.read(new File("media" + File.separator + "ui" + File.separator + "zomboidIcon128.png").getAbsoluteFile());
                ByteBuffer byteBuffer;
                windowIconBB1 = byteBuffer = loadInstance(bufferedImage, 128);
                GLFWImage image;
                windowIcon1 = image = GLFWImage.create().set(128, 128, byteBuffer);
                imageBuffer.put(0, image);
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        } else {
            try {
                imageBuffer = GLFWImage.create(1);
                BufferedImage bufferedImage = ImageIO.read(new File("media" + File.separator + "ui" + File.separator + "zomboidIcon32.png").getAbsoluteFile());
                ByteBuffer byteBuffer;
                windowIconBB1 = byteBuffer = loadInstance(bufferedImage, 32);
                GLFWImage image;
                windowIcon1 = image = GLFWImage.create().set(32, 32, byteBuffer);
                imageBuffer.put(0, image);
            } catch (IOException var6) {
                var6.printStackTrace();
            }
        }

        return imageBuffer;
    }

    private static ByteBuffer loadInstance(BufferedImage image, int dimension) {
        return CustomizationManager.loadAndResizeInstance(image, dimension, dimension);
    }

    private static void printSpecs() {
        try {
            System.out.println("===== System specs =====");
            long kilobytes = 1024L;
            long megabytes = 1048576L;
            long gigabytes = 1073741824L;
            Map<String, String> env = System.getenv();
            System.out
                .println("OS: " + System.getProperty("os.name") + ", version: " + System.getProperty("os.version") + ", arch: " + System.getProperty("os.arch"));
            if (env.containsKey("PROCESSOR_IDENTIFIER")) {
                System.out.println("Processor: " + env.get("PROCESSOR_IDENTIFIER"));
            }

            if (env.containsKey("NUMBER_OF_PROCESSORS")) {
                System.out.println("Processor cores: " + env.get("NUMBER_OF_PROCESSORS"));
            }

            System.out.println("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
            System.out.println("Memory free: " + (float)Runtime.getRuntime().freeMemory() / 1048576.0F + " MB");
            long maxMemory = Runtime.getRuntime().maxMemory();
            System.out.println("Memory max: " + (maxMemory == Long.MAX_VALUE ? "no limit" : (float)maxMemory / 1048576.0F) + " MB");
            System.out.println("Memory  total available to JVM: " + (float)Runtime.getRuntime().totalMemory() / 1048576.0F + " MB");
            if (SystemDisabler.printDetailedInfo()) {
                File[] roots = File.listRoots();

                for (File root : roots) {
                    System.out
                        .println(
                            root.getAbsolutePath()
                                + ", Total: "
                                + (float)root.getTotalSpace() / 1.0737418E9F
                                + " GB, Free: "
                                + (float)root.getFreeSpace() / 1.0737418E9F
                                + " GB"
                        );
                }
            }

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                System.out.println("Mobo = " + wmic("baseboard", new String[]{"Product"}));
                System.out.println("CPU = " + wmic("cpu", new String[]{"Manufacturer", "MaxClockSpeed", "Name"}));
                System.out.println("Graphics = " + wmic("path Win32_videocontroller", new String[]{"AdapterRAM", "DriverVersion", "Name"}));
                System.out.println("VideoMode = " + wmic("path Win32_videocontroller", new String[]{"VideoModeDescription"}));
                System.out.println("Sound = " + wmic("path Win32_sounddevice", new String[]{"Manufacturer", "Name"}));
                System.out.println("Memory RAM = " + wmic("memorychip", new String[]{"Capacity", "Manufacturer"}));
            }

            System.out.println("------------------------");
        } catch (Exception var14) {
            var14.printStackTrace();
        }
    }

    private static String wmic(String component, String[] get) {
        String str = "";

        try {
            String wmic_com = "WMIC " + component + " GET";

            for (int i = 0; i < get.length; i++) {
                wmic_com = wmic_com + " " + get[i];
                if (i < get.length - 1) {
                    wmic_com = wmic_com + ",";
                }
            }

            Process process = Runtime.getRuntime().exec(new String[]{"CMD", "/C", wmic_com});
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String data = "";

            String s;
            while ((s = reader.readLine()) != null) {
                data = data + s;
            }

            for (String g : get) {
                data = data.replaceAll(g, "");
            }

            data = data.trim().replaceAll(" ( )+", "=");
            System.out.println(data);
            String[] parts = data.split("=");
            if (parts.length > get.length) {
                str = "{ ";
                int items = parts.length / get.length;

                for (int k = 0; k < items; k++) {
                    str = str + "[";

                    for (int ix = 0; ix < get.length; ix++) {
                        int ii = k * get.length + ix;
                        str = str + get[ix] + "=" + parts[ii];
                        if (ix < get.length - 1) {
                            str = str + ",";
                        }
                    }

                    str = str + "]";
                    if (k < items - 1) {
                        str = str + ", ";
                    }
                }

                str = str + " }";
            } else {
                str = "[";

                for (int ixx = 0; ixx < parts.length; ixx++) {
                    str = str + get[ixx] + "=" + parts[ixx];
                    if (ixx < parts.length - 1) {
                        str = str + ",";
                    }
                }

                str = str + "]";
            }

            return str;
        } catch (Exception var13) {
            return "Couldnt get info...";
        }
    }

    public class Credit {
        public int disappearDelay;
        public Texture name;
        public float nameAlpha;
        public float nameAppearDelay;
        public float nameTargetAlpha;
        public Texture title;
        public float titleAlpha;
        public float titleTargetAlpha;

        public Credit(final Texture title, final Texture name) {
            Objects.requireNonNull(MainScreenState.this);
            super();
            this.disappearDelay = 200;
            this.nameAppearDelay = 40.0F;
            this.titleTargetAlpha = 1.0F;
            this.titleAlpha = 0.0F;
            this.nameTargetAlpha = 0.0F;
            this.nameAlpha = 0.0F;
            this.title = title;
            this.name = name;
        }
    }

    public static class ScreenElement {
        public float alpha;
        public float alphaStep = 0.2F;
        public boolean jumpBack = true;
        public float sx;
        public float sy;
        public float targetAlpha;
        public Texture tex;
        public int ticksTillTargetAlpha;
        public float x;
        public int xCount = 1;
        public float xVel;
        public float xVelO;
        public float y;
        public float yVel;
        public float yVelO;

        public ScreenElement(Texture tex, int x, int y, float xVel, float yVel, int xCount) {
            this.x = this.sx = x;
            this.y = this.sy = y - tex.getHeight() * MainScreenState.totalScale;
            this.xVel = xVel;
            this.yVel = yVel;
            this.tex = tex;
            this.xCount = xCount;
        }

        public void render() {
            int x = (int)this.x;
            int y = (int)this.y;

            for (int n = 0; n < this.xCount; n++) {
                MainScreenState.DrawTexture(
                    this.tex,
                    x,
                    y,
                    (int)(this.tex.getWidth() * MainScreenState.totalScale),
                    (int)(this.tex.getHeight() * MainScreenState.totalScale),
                    this.alpha
                );
                x = (int)(x + this.tex.getWidth() * MainScreenState.totalScale);
            }

            TextManager.instance
                .DrawStringRight(
                    Core.getInstance().getOffscreenWidth(0) - 5, Core.getInstance().getOffscreenHeight(0) - 15, "Version: RC 3", 1.0, 1.0, 1.0, 1.0
                );
        }

        public void setY(float y) {
            this.y = this.sy = y - this.tex.getHeight() * MainScreenState.totalScale;
        }

        public void update() {
            this.x = this.x + this.xVel * MainScreenState.totalScale;
            this.y = this.y + this.yVel * MainScreenState.totalScale;
            this.ticksTillTargetAlpha--;
            if (this.ticksTillTargetAlpha <= 0) {
                this.targetAlpha = 1.0F;
            }

            if (this.jumpBack && this.sx - this.x > this.tex.getWidth() * MainScreenState.totalScale) {
                this.x = this.x + this.tex.getWidth() * MainScreenState.totalScale;
            }

            if (this.alpha < this.targetAlpha) {
                this.alpha = this.alpha + this.alphaStep;
                if (this.alpha > this.targetAlpha) {
                    this.alpha = this.targetAlpha;
                }
            } else if (this.alpha > this.targetAlpha) {
                this.alpha = this.alpha - this.alphaStep;
                if (this.alpha < this.targetAlpha) {
                    this.alpha = this.targetAlpha;
                }
            }
        }
    }
}
