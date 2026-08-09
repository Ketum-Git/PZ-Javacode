// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.globalObjects.CGlobalObjectSystem;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.GlobalObject;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.ui.TextDrawObject;
import zombie.ui.UIElementInterface;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class DebugGlobalObjectState extends GameState {
    public static DebugGlobalObjectState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private KahluaTable table;
    private int playerIndex;
    private int z;
    private int gridX = -1;
    private int gridY = -1;
    private static final UIFont FONT = UIFont.DebugConsole;

    public DebugGlobalObjectState() {
        instance = this;
    }

    @Override
    public void enter() {
        instance = this;
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            this.z = player == null ? 0 : PZMath.fastfloor(player.getZ());
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("DebugGlobalObjectState_InitUI"), this);
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
        this.restoreGameUI();

        for (int i = 0; i < IsoCamera.cameras.length; i++) {
            IsoCamera.cameras[i].deferedX = IsoCamera.cameras[i].deferedY = 0.0F;
        }
    }

    @Override
    public void render() {
        IsoPlayer.setInstance(IsoPlayer.players[this.playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[this.playerIndex]);
        boolean clear = true;

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            if (pn != this.playerIndex && IsoPlayer.players[pn] != null) {
                Core.getInstance().StartFrame(pn, clear);
                Core.getInstance().EndFrame(pn);
                clear = false;
            }
        }

        Core.getInstance().StartFrame(this.playerIndex, clear);
        this.renderScene();
        Core.getInstance().EndFrame(this.playerIndex);
        Core.getInstance().RenderOffScreenBuffer();

        for (int pnx = 0; pnx < IsoPlayer.numPlayers; pnx++) {
            TextDrawObject.NoRender(pnx);
            ChatElement.NoRender(pnx);
        }

        if (Core.getInstance().StartFrameUI()) {
            this.renderUI();
        }

        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (!this.exit && !GameKeyboard.isKeyPressed(60)) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[this.playerIndex];
            chunkMap.ProcessChunkPos(IsoPlayer.players[this.playerIndex]);
            chunkMap.update();
            return this.updateScene();
        } else {
            return GameStateMachine.StateAction.Continue;
        }
    }

    public void renderScene() {
        IsoCamera.frameState.set(this.playerIndex);
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        SpriteRenderer.instance.doCoreIntParam(0, isoGameCharacter.getX());
        SpriteRenderer.instance.doCoreIntParam(1, isoGameCharacter.getY());
        SpriteRenderer.instance.doCoreIntParam(2, IsoCamera.frameState.camCharacterZ);
        IsoSprite.globalOffsetX = -1.0F;
        IsoWorld.instance.currentCell.render();
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[this.playerIndex];
        int minX = chunkMap.getWorldXMin();
        int minY = chunkMap.getWorldYMin();
        int maxX = minX + IsoChunkMap.chunkGridWidth;
        int maxY = minY + IsoChunkMap.chunkGridWidth;
        int systemCount = CGlobalObjects.getSystemCount();

        for (int i = 0; i < systemCount; i++) {
            CGlobalObjectSystem system = CGlobalObjects.getSystemByIndex(i);

            for (int cy = minY; cy < maxY; cy++) {
                for (int cx = minX; cx < maxX; cx++) {
                    ArrayList<GlobalObject> objects = system.getObjectsInChunk(cx, cy);

                    for (int j = 0; j < objects.size(); j++) {
                        GlobalObject globalObject = objects.get(j);
                        float r = 1.0F;
                        float g = 1.0F;
                        float b = 1.0F;
                        if (globalObject.getZ() != this.z) {
                            b = 0.5F;
                            g = 0.5F;
                            r = 0.5F;
                        }

                        this.DrawIsoRect(globalObject.getX(), globalObject.getY(), globalObject.getZ(), 1.0F, 1.0F, r, g, b, 1.0F, 1);
                    }

                    system.finishedWithList(objects);
                }
            }
        }

        LineDrawer.render();
        LineDrawer.clear();
    }

    private void renderUI() {
        UIManager.render();
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public GameStateMachine.StateAction updateScene() {
        IsoPlayer.setInstance(IsoPlayer.players[this.playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[this.playerIndex]);
        UIManager.setPicked(IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA()));
        IsoObject mouseOverObject = UIManager.getPicked() == null ? null : UIManager.getPicked().tile;
        UIManager.setLastPicked(mouseOverObject);
        IsoCamera.update();
        this.updateCursor();
        return GameStateMachine.StateAction.Remain;
    }

    private void updateCursor() {
        int playerIndex = this.playerIndex;
        float x = Mouse.getXA();
        float y = Mouse.getYA();
        x -= IsoCamera.getScreenLeft(playerIndex);
        y -= IsoCamera.getScreenTop(playerIndex);
        x *= Core.getInstance().getZoom(playerIndex);
        y *= Core.getInstance().getZoom(playerIndex);
        int z = PZMath.fastfloor((float)this.z);
        this.gridX = (int)IsoUtils.XToIso(x, y, z);
        this.gridY = (int)IsoUtils.YToIso(x, y, z);
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

    private void DrawIsoLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z2, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    private void DrawIsoRect(float x, float y, float z, float width, float height, float r, float g, float b, float a, int thickness) {
        this.DrawIsoLine(x, y, z, x + width, y, z, r, g, b, a, thickness);
        this.DrawIsoLine(x + width, y, z, x + width, y + height, z, r, g, b, a, thickness);
        this.DrawIsoLine(x + width, y + height, z, x, y + height, z, r, g, b, a, thickness);
        this.DrawIsoLine(x, y + height, z, x, y, z, r, g, b, a, thickness);
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit":
                this.exit = true;
                return null;
            case "getCameraDragX":
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedX);
            case "getCameraDragY":
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedY);
            case "getPlayerIndex":
                return BoxedStaticValues.toDouble(this.playerIndex);
            case "getZ":
                return BoxedStaticValues.toDouble(this.z);
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\"", func));
        }
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "setPlayerIndex":
                this.playerIndex = PZMath.clamp(((Double)arg0).intValue(), 0, 3);
                return null;
            case "setZ":
                this.z = PZMath.clamp(((Double)arg0).intValue(), 0, 7);
                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
        }
    }

    public Object fromLua2(String func, Object arg0, Object arg1) {
        byte var5 = -1;
        switch (func.hashCode()) {
            case -1879300743:
                if (func.equals("dragCamera")) {
                    var5 = 0;
                }
            default:
                switch (var5) {
                    case 0:
                        float dx = ((Double)arg0).floatValue();
                        float dy = ((Double)arg1).floatValue();
                        IsoCamera.cameras[this.playerIndex].deferedX = -dx;
                        IsoCamera.cameras[this.playerIndex].deferedY = -dy;
                        return null;
                    default:
                        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \\\"%s\\\"", func, arg0, arg1));
                }
        }
    }
}
