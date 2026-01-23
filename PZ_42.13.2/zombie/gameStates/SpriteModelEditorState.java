// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
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
import zombie.iso.SpriteModel;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;
import zombie.spriteModel.SpriteModelManager;
import zombie.spriteModel.TilesetImageCreator;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class SpriteModelEditorState extends GameState {
    public static SpriteModelEditorState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private KahluaTable table;
    private final ArrayList<String> clipNames = new ArrayList<>();
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final SpriteModelEditorState.BooleanDebugOption drawGrid = new SpriteModelEditorState.BooleanDebugOption("DrawGrid", false);
    private final SpriteModelEditorState.BooleanDebugOption drawNorthWall = new SpriteModelEditorState.BooleanDebugOption("DrawNorthWall", false);
    private final SpriteModelEditorState.BooleanDebugOption drawWestWall = new SpriteModelEditorState.BooleanDebugOption("DrawWestWall", false);

    @Override
    public void enter() {
        instance = this;
        this.load();
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("SpriteModelEditor_InitUI"));
            if (this.table != null && this.table.getMetatable() != null) {
                this.table.getMetatable().rawset("_LUA_RELOADED_CHECK", Boolean.FALSE);
            }
        } else {
            UIManager.UI.addAll(this.selfUi);
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("showUI"), this.table);
        }

        this.exit = false;
        DebugOptions.instance.isoSprite.forceNearestMagFilter.setValue(true);
    }

    @Override
    public void yield() {
        this.restoreGameUI();
    }

    @Override
    public void reenter() {
        this.saveGameUI();
    }

    @Override
    public void exit() {
        DebugOptions.instance.isoSprite.forceNearestMagFilter.setValue(false);
        this.save();
        this.restoreGameUI();
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
        if (!this.exit && !GameKeyboard.isKeyPressed(66) && !GameKeyboard.isKeyPressed(1)) {
            this.updateScene();
            return GameStateMachine.StateAction.Remain;
        } else {
            SpriteModelManager.getInstance().toScriptManager();
            return GameStateMachine.StateAction.Continue;
        }
    }

    public static SpriteModelEditorState checkInstance() {
        if (instance != null) {
            if (instance.table != null && instance.table.getMetatable() != null) {
                if (instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                    instance = null;
                }
            } else {
                instance = null;
            }
        }

        return instance == null ? new SpriteModelEditorState() : instance;
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
        byte var3 = -1;
        switch (func.hashCode()) {
            case 3127582:
                if (func.equals("exit")) {
                    var3 = 0;
                }
            default:
                switch (var3) {
                    case 0:
                        this.exit = true;
                        return null;
                    default:
                        throw new IllegalArgumentException("unhandled \"" + func + "\"");
                }
        }
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "getClipNames":
                String modelScriptName = (String)arg0;
                ModelScript modelScript = ScriptManager.instance.getModelScript(modelScriptName);
                String animationsMeshName = modelScript.animationsMesh;
                if (animationsMeshName == null) {
                    animationsMeshName = modelScript.meshName;
                    if (animationsMeshName != null && animationsMeshName.contains("/")) {
                        animationsMeshName = animationsMeshName.substring(animationsMeshName.lastIndexOf(47) + 1);
                    }
                }

                AnimationsMesh animationsMesh = ScriptManager.instance.getAnimationsMesh(animationsMeshName);
                if (animationsMesh != null && animationsMesh.modelMesh != null && animationsMesh.modelMesh.skinningData != null) {
                    HashMap<String, AnimationClip> clips = animationsMesh.modelMesh.skinningData.animationClips;
                    if (this.clipNames.isEmpty() || !clips.containsKey(this.clipNames.get(0))) {
                        this.clipNames.clear();

                        for (AnimationClip clip : clips.values()) {
                            this.clipNames.add(clip.name);
                        }

                        this.clipNames.sort(Comparator.naturalOrder());
                    }

                    return this.clipNames;
                }

                this.clipNames.clear();
                return this.clipNames;
            case "writeSpriteModelsFile":
                String modID = (String)arg0;
                SpriteModelManager.getInstance().write(modID);
                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
        }
    }

    public Object fromLua2(String func, Object arg0, Object arg1) {
        byte var5 = -1;
        func.hashCode();
        switch (var5) {
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\"", func, arg0, arg1));
        }
    }

    public Object fromLua3(String func, Object arg0, Object arg1, Object arg2) {
        byte var6 = -1;
        switch (func.hashCode()) {
            case 265285860:
                if (func.equals("saveTilesetImage")) {
                    var6 = 0;
                }
            default:
                switch (var6) {
                    case 0:
                        String modID = (String)arg0;
                        String tilesetName = (String)arg1;
                        String filePath = (String)arg2;
                        TilesetImageCreator tic = new TilesetImageCreator();
                        tic.createImage(modID, tilesetName, filePath);
                        return null;
                    default:
                        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2));
                }
        }
    }

    public Object fromLua4(String func, Object arg0, Object arg1, Object arg2, Object arg3) {
        switch (func) {
            case "clearTileProperties": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int column = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                SpriteModelManager.getInstance().clearTileProperties(modID, tilesetName, column, row);
                return null;
            }
            case "getOrCreateTileProperties": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int column = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                SpriteModel spriteModel = SpriteModelManager.getInstance().getTileProperties(modID, tilesetName, column, row);
                if (spriteModel == null) {
                    spriteModel = new SpriteModel();
                    SpriteModelManager.getInstance().setTileProperties(modID, tilesetName, column, row, spriteModel);
                    spriteModel = SpriteModelManager.getInstance().getTileProperties(modID, tilesetName, column, row);
                }

                return spriteModel;
            }
            case "getTileProperties": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int column = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                return SpriteModelManager.getInstance().getTileProperties(modID, tilesetName, column, row);
            }
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3));
        }
    }

    public Object fromLua5(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        byte var8 = -1;
        switch (func.hashCode()) {
            case -1634149116:
                if (func.equals("setTileModelScript")) {
                    var8 = 0;
                }
            default:
                switch (var8) {
                    case 0:
                        String modID = (String)arg0;
                        String tilesetName = (String)arg1;
                        int column = ((Double)arg2).intValue();
                        int row = ((Double)arg3).intValue();
                        String modelScriptName = (String)arg4;
                        SpriteModel spriteModel = SpriteModelManager.getInstance().getTileProperties(modID, tilesetName, column, row);
                        if (spriteModel == null) {
                            spriteModel = new SpriteModel();
                            SpriteModelManager.getInstance().setTileProperties(modID, tilesetName, column, row, spriteModel);
                        } else {
                            spriteModel.modelScriptName = modelScriptName;
                        }

                        return null;
                    default:
                        throw new IllegalArgumentException(
                            String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4)
                        );
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
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "SpriteModelEditorState-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "SpriteModelEditorState-options.ini";
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
            Objects.requireNonNull(SpriteModelEditorState.this);
            super(name, defaultValue);
            SpriteModelEditorState.this.options.add(this);
        }
    }
}
