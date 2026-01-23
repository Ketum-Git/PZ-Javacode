// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.input.GameKeyboard;
import zombie.iso.IsoCamera;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

public final class ServerDisconnectState extends GameState {
    private final boolean keyDown = false;
    private final int gridX = -1;
    private final int gridY = -1;

    @Override
    public void enter() {
        TutorialManager.instance.stealControl = false;
        UIManager.UI.clear();
        LuaEventManager.ResetCallbacks();
        LuaManager.call("ISServerDisconnectUI_OnServerDisconnectUI", GameWindow.kickReason);
    }

    @Override
    public void exit() {
        GameWindow.kickReason = null;
    }

    @Override
    public void render() {
        boolean clear = true;

        for (int n = 0; n < IsoPlayer.numPlayers; n++) {
            if (IsoPlayer.players[n] == null) {
                if (n == 0) {
                    SpriteRenderer.instance.prePopulating();
                }
            } else {
                IsoPlayer.setInstance(IsoPlayer.players[n]);
                IsoCamera.setCameraCharacter(IsoPlayer.players[n]);
                Core.getInstance().StartFrame(n, clear);
                IsoCamera.frameState.set(n);
                clear = false;
                IsoSprite.globalOffsetX = -1.0F;
                IsoWorld.instance.render();
                Core.getInstance().EndFrame(n);
            }
        }

        Core.getInstance().RenderOffScreenBuffer();

        for (int nx = 0; nx < IsoPlayer.numPlayers; nx++) {
            if (IsoPlayer.players[nx] != null) {
                Core.getInstance().StartFrameText(nx);
                IndieGL.disableAlphaTest();
                IndieGL.disableDepthTest();
                TextDrawObject.RenderBatch(nx);
                ChatElement.RenderBatch(nx);

                try {
                    Core.getInstance().EndFrameText(nx);
                } catch (Exception var4) {
                }
            }
        }

        if (Core.getInstance().StartFrameUI()) {
            UIManager.render();
            String reason = GameWindow.kickReason;
            if (reason == null || reason.isEmpty()) {
                reason = "Connection to server lost";
            }

            TextManager.instance
                .DrawStringCentre(UIFont.Medium, Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() / 2, reason, 1.0, 1.0, 1.0, 1.0);
        }

        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (!Core.exiting && !GameKeyboard.isKeyDown(1)) {
            UIManager.update();
            return GameStateMachine.StateAction.Remain;
        } else {
            return GameStateMachine.StateAction.Continue;
        }
    }
}
