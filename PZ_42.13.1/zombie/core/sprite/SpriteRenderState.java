// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.sprite;

import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Styles.TransparentStyle;
import zombie.core.opengl.GLState;
import zombie.core.opengl.RenderSettings;
import zombie.core.textures.TextureFBO;
import zombie.input.Mouse;
import zombie.iso.PlayerCamera;

public final class SpriteRenderState extends GenericSpriteRenderState {
    public TextureFBO fbo;
    public long time;
    public final SpriteRenderStateUI stateUi;
    public int playerIndex;
    public final PlayerCamera[] playerCamera = new PlayerCamera[4];
    public final float[] playerAmbient = new float[4];
    public float maxZoomLevel;
    public float minZoomLevel;
    public final float[] zoomLevel = new float[4];

    public SpriteRenderState(int index) {
        super(index);

        for (int i = 0; i < 4; i++) {
            this.playerCamera[i] = new PlayerCamera(i);
        }

        this.stateUi = new SpriteRenderStateUI(index);
    }

    @Override
    public void onRendered() {
        super.onRendered();
        this.stateUi.onRendered();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.stateUi.onReady();
    }

    @Override
    public void CheckSpriteSlots() {
        if (this.stateUi.active) {
            this.stateUi.CheckSpriteSlots();
        } else {
            super.CheckSpriteSlots();
        }
    }

    @Override
    public void clear() {
        this.stateUi.clear();
        super.clear();
    }

    /**
     * Returns either the UI state, or this state. Depends on the value of stateUI.bActive
     */
    public GenericSpriteRenderState getActiveState() {
        return (GenericSpriteRenderState)(this.stateUi.active ? this.stateUi : this);
    }

    public void prePopulating() {
        this.clear();
        this.fbo = Core.getInstance().getOffscreenBuffer();

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null) {
                this.playerCamera[i].initFromIsoCamera(i);
                this.playerAmbient[i] = RenderSettings.getInstance().getAmbientForPlayer(i);
                this.zoomLevel[i] = Core.getInstance().getZoom(i);
                this.maxZoomLevel = Core.getInstance().getMaxZoom();
                this.minZoomLevel = Core.getInstance().getMinZoom();
            }
        }

        this.defaultStyle = TransparentStyle.instance;
        this.cursorVisible = Mouse.isCursorVisible();
        GLState.startFrame();
    }
}
