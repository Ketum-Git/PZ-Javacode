// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class AnimationViewerState extends GameState {
    public static AnimationViewerState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private KahluaTable table;
    private final ArrayList<String> clipNames = new ArrayList<>();
    private float ambientVolume;
    private float musicVolume;
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final AnimationViewerState.BooleanDebugOption drawGrid = new AnimationViewerState.BooleanDebugOption("DrawGrid", false);
    private final AnimationViewerState.BooleanDebugOption isometric = new AnimationViewerState.BooleanDebugOption("Isometric", false);
    private final AnimationViewerState.BooleanDebugOption showBones = new AnimationViewerState.BooleanDebugOption("ShowBones", false);
    private final AnimationViewerState.BooleanDebugOption useDeferredMovement = new AnimationViewerState.BooleanDebugOption("UseDeferredMovement", false);

    @Override
    public void enter() {
        instance = this;
        this.load();
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        this.saveSoundState();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("AnimationViewerState_InitUI"));
            if (this.table != null && this.table.getMetatable() != null) {
                this.table.getMetatable().rawset("_LUA_RELOADED_CHECK", Boolean.FALSE);
            }
        } else {
            UIManager.UI.addAll(this.selfUi);
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("showUI"), this.table);
        }

        this.exit = false;
    }

    @Override
    public void yield() {
        this.restoreGameUI();
        this.restoreSoundState();
    }

    @Override
    public void reenter() {
        this.saveGameUI();
        this.saveSoundState();
    }

    @Override
    public void exit() {
        this.save();
        this.restoreGameUI();
        this.restoreSoundState();
    }

    @Override
    public void render() {
        int playerIndex = 0;
        Core.getInstance().StartFrame(0, true);
        this.renderScene();
        Core.getInstance().EndFrame(0);
        Core.getInstance().RenderOffScreenBuffer();
        UIManager.useUiFbo = Core.getInstance().supportsFBO() && Core.getInstance().getOptionUIFBO();
        if (Core.getInstance().StartFrameUI()) {
            this.renderUI();
        }

        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (!this.exit && !GameKeyboard.isKeyPressed(65)) {
            this.updateScene();
            return GameStateMachine.StateAction.Remain;
        } else {
            return GameStateMachine.StateAction.Continue;
        }
    }

    public static AnimationViewerState checkInstance() {
        if (instance != null) {
            if (instance.table != null && instance.table.getMetatable() != null) {
                if (instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                    instance = null;
                }
            } else {
                instance = null;
            }
        }

        return instance == null ? new AnimationViewerState() : instance;
    }

    private void saveGameUI() {
        this.gameUi.clear();
        this.gameUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        this.suspendUi = UIManager.suspend;
        UIManager.suspend = false;
        UIManager.setShowPausedMessage(false);
        UIManager.defaultthread = this.luaEnv.thread;
    }

    private void restoreGameUI() {
        this.selfUi.clear();
        this.selfUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        UIManager.UI.addAll(this.gameUi);
        UIManager.suspend = this.suspendUi;
        UIManager.setShowPausedMessage(true);
        UIManager.defaultthread = LuaManager.thread;
    }

    private void saveSoundState() {
        this.ambientVolume = SoundManager.instance.getAmbientVolume();
        SoundManager.instance.setAmbientVolume(1.0F);
        this.musicVolume = SoundManager.instance.getMusicVolume();
        SoundManager.instance.setMusicVolume(0.0F);
        SoundManager.instance.setMusicState("InGame");
    }

    private void restoreSoundState() {
        SoundManager.instance.setAmbientVolume(this.ambientVolume);
        SoundManager.instance.setMusicVolume(this.musicVolume);
        SoundManager.instance.setMusicState("PauseMenu");
    }

    private void updateScene() {
        ModelManager.instance.update();
        if (GameKeyboard.isKeyPressed(17)) {
            DebugOptions.instance.model.render.wireframe.setValue(!DebugOptions.instance.model.render.wireframe.getValue());
        }
    }

    private void renderScene() {
    }

    private void renderUI() {
        UIManager.render();
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit":
                this.exit = true;
                return null;
            case "getClipNames":
                if (this.clipNames.isEmpty()) {
                    for (AnimationClip clip : ModelManager.instance.getAllAnimationClips()) {
                        this.clipNames.add(clip.name);
                    }

                    this.clipNames.sort(Comparator.naturalOrder());
                }

                return this.clipNames;
            default:
                throw new IllegalArgumentException("unhandled \"" + func + "\"");
        }
    }

    public Object fromLua1(String func, Object arg0) {
        byte var4 = -1;
        switch (func.hashCode()) {
            case -1628879070:
                if (func.equals("getClipNames")) {
                    var4 = 0;
                }
            default:
                switch (var4) {
                    case 0:
                        String modelScriptName = (String)arg0;
                        ModelScript modelScript = ScriptManager.instance.getModelScript(modelScriptName);
                        AnimationsMesh animationsMesh = ScriptManager.instance.getAnimationsMesh(modelScript.animationsMesh);
                        HashMap<String, AnimationClip> clips = animationsMesh.modelMesh.skinningData.animationClips;
                        if (this.clipNames.isEmpty() || !clips.containsKey(this.clipNames.get(0))) {
                            this.clipNames.clear();

                            for (AnimationClip clip : clips.values()) {
                                this.clipNames.add(clip.name);
                            }

                            this.clipNames.sort(Comparator.naturalOrder());
                        }

                        return this.clipNames;
                    default:
                        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
                }
        }
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        if (this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption) {
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        return this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption ? booleanConfigOption.getValue() : false;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "animationViewerState-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "animationViewerState-options.ini";
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

    @UsedFromLua
    public class BooleanDebugOption extends BooleanConfigOption {
        public BooleanDebugOption(final String name, final boolean defaultValue) {
            Objects.requireNonNull(AnimationViewerState.this);
            super(name, defaultValue);
            AnimationViewerState.this.options.add(this);
        }
    }
}
