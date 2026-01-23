// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.ui.UIManager;

@UsedFromLua
public class TermsOfServiceState extends GameState {
    private boolean exit;
    private boolean created;

    @Override
    public void enter() {
        LuaEventManager.triggerEvent("OnGameStateEnter", this);
        if (!this.created) {
            this.exit = true;
        }
    }

    @Override
    public void exit() {
        UIManager.clearArrays();
    }

    @Override
    public GameStateMachine.StateAction update() {
        return this.exit ? GameStateMachine.StateAction.Continue : GameStateMachine.StateAction.Remain;
    }

    @Override
    public void render() {
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        if (Core.getInstance().StartFrameUI()) {
            UIManager.render();
        }

        Core.getInstance().EndFrameUI();
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "created":
                this.created = true;
                return null;
            case "exit":
                this.exit = true;
                return null;
            default:
                throw new IllegalArgumentException("unhandled \"" + func + "\"");
        }
    }
}
