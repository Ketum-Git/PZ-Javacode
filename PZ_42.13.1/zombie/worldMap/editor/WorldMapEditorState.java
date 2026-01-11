// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.editor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class WorldMapEditorState extends GameState {
    public static WorldMapEditorState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private KahluaTable table;

    @Override
    public void enter() {
        instance = this;
        this.load();
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("WorldMapEditor_InitUI"), this);
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
    }

    @Override
    public void reenter() {
        this.saveGameUI();
    }

    @Override
    public void exit() {
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
        if (!this.exit && !GameKeyboard.isKeyPressed(66)) {
            this.updateScene();
            return GameStateMachine.StateAction.Remain;
        } else {
            return GameStateMachine.StateAction.Continue;
        }
    }

    public static WorldMapEditorState checkInstance() {
        if (instance != null) {
            if (instance.table != null && instance.table.getMetatable() != null) {
                if (instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                    instance = null;
                }
            } else {
                instance = null;
            }
        }

        return instance == null ? new WorldMapEditorState() : instance;
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

    public Object fromLua2(String func, Object arg0, Object arg1) {
        byte var5 = -1;
        switch (func.hashCode()) {
            case -51077133:
                if (func.equals("writeAnnotationsLua")) {
                    var5 = 0;
                }
            default:
                switch (var5) {
                    case 0:
                        String relativeFilePath = (String)arg0;
                        String luaCode = (String)arg1;
                        int p = relativeFilePath.lastIndexOf("/");
                        String absDirPath = ZomboidFileSystem.instance.getString(relativeFilePath.substring(0, p));
                        if (!absDirPath.endsWith(File.separator)) {
                            absDirPath = absDirPath + File.separator;
                        }

                        String absFilePath = absDirPath + relativeFilePath.substring(p + 1);
                        File file = new File(absFilePath).getAbsoluteFile();
                        DebugLog.log("writing " + relativeFilePath);

                        try {
                            try (FileWriter fw = new FileWriter(file)) {
                                fw.write(luaCode);
                            }

                            return null;
                        } catch (IOException var17) {
                            throw new RuntimeException(var17);
                        }
                    default:
                        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \\\"%s\\\"", func, arg0, arg1));
                }
        }
    }

    public void save() {
    }

    public void load() {
    }
}
