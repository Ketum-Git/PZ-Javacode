// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.ui.TextManager;
import zombie.ui.UIManager;

public final class TISLogoState extends GameState {
    public float alpha;
    public float alphaStep = 0.02F;
    public float logoDisplayTime = 20.0F;
    public int screenNumber = 1;
    public int stage = 0;
    public float targetAlpha;
    private boolean noRender;
    private final TISLogoState.LogoElement logoTis = new TISLogoState.LogoElement("media/ui/Logos/TheIndieStoneLogo_Lineart_White.png");
    private final TISLogoState.LogoElement logoFmod = new TISLogoState.LogoElement("media/ui/Logos/FMOD.png");
    private final TISLogoState.LogoElement logoGa = new TISLogoState.LogoElement("media/ui/Logos/GeneralArcade.png");
    private final TISLogoState.LogoElement logoFi = new TISLogoState.LogoElement("media/ui/Logos/FormosaInteractive.png");
    private final TISLogoState.LogoElement logoVb = new TISLogoState.LogoElement("media/ui/Logos/VertexBreak.png");
    private static final int SCREEN_TIS = 1;
    private static final int SCREEN_OTHER = 2;
    private static final int STAGE_FADING_IN_LOGO = 0;
    private static final int STAGE_HOLDING_LOGO = 1;
    private static final int STAGE_FADING_OUT_LOGO = 2;
    private static final int STAGE_EXIT = 3;

    @Override
    public void enter() {
        UIManager.suspend = true;
        this.alpha = 0.0F;
        this.targetAlpha = 1.0F;
    }

    @Override
    public void exit() {
        UIManager.suspend = false;
    }

    @Override
    public void render() {
        if (this.noRender) {
            Core.getInstance().StartFrame();
            SpriteRenderer.instance
                .renderi(null, 0, 0, Core.getInstance().getOffscreenWidth(0), Core.getInstance().getOffscreenHeight(0), 0.0F, 0.0F, 0.0F, 1.0F, null);
            Core.getInstance().EndFrame();
        } else {
            Core.getInstance().StartFrame();
            Core.getInstance().EndFrame();
            boolean useUIFBO = UIManager.useUiFbo;
            UIManager.useUiFbo = false;
            Core.getInstance().StartFrameUI();
            SpriteRenderer.instance
                .renderi(null, 0, 0, Core.getInstance().getOffscreenWidth(0), Core.getInstance().getOffscreenHeight(0), 0.0F, 0.0F, 0.0F, 1.0F, null);
            if (this.screenNumber == 1) {
                this.logoTis.centerOnScreen();
                this.logoTis.render(this.alpha, null);
            }

            if (this.screenNumber == 2) {
                this.renderAttribution();
            }

            Core.getInstance().EndFrameUI();
            UIManager.useUiFbo = useUIFBO;
        }
    }

    private void renderAttribution() {
        int screenWidth = Core.getInstance().getScreenWidth();
        int screenHeight = Core.getInstance().getScreenHeight();
        int perLogoWidth = screenWidth / 4;
        int perLogoHeight = perLogoWidth;
        if (screenHeight / 3 < perLogoWidth) {
            perLogoWidth = screenHeight / 3;
            perLogoHeight = perLogoWidth;
        }

        int centerX1 = screenWidth / 2 - perLogoWidth / 4 - perLogoWidth / 2;
        int centerX2 = screenWidth / 2 + perLogoWidth / 4 + perLogoWidth / 2;
        int centerY1 = screenHeight / 2 - perLogoHeight / 8 - perLogoHeight / 2;
        int centerY2 = screenHeight / 2 + perLogoHeight / 8 + perLogoHeight / 2;
        Texture tex = this.logoGa.texture;
        if (tex != null && tex.isReady()) {
            int x = centerX1 - perLogoWidth / 2;
            int y = centerY1 - perLogoHeight / 2;
            this.logoGa.setBounds(x, y, perLogoWidth, perLogoHeight);
            this.logoGa.render(this.alpha, null);
        }

        tex = this.logoVb.texture;
        if (tex != null && tex.isReady()) {
            int x = centerX1 - perLogoWidth / 2;
            int y = centerY2 - perLogoHeight / 2;
            this.logoVb.setBounds(x, y, perLogoWidth, perLogoHeight);
            this.logoVb.render(this.alpha, null);
        }

        tex = this.logoFi.texture;
        if (tex != null && tex.isReady()) {
            int x = centerX2 - perLogoWidth / 2;
            int y = centerY1 - perLogoHeight / 2;
            this.logoFi.setBounds(x, y, perLogoWidth, perLogoHeight);
            this.logoFi.render(this.alpha, null);
        }

        tex = this.logoFmod.texture;
        if (tex != null && tex.isReady()) {
            int x = centerX2 - perLogoWidth / 2;
            int y = centerY2 - perLogoHeight / 2;
            this.logoFmod.setBounds(x, y, perLogoWidth, perLogoHeight);
            String text = "Made with FMOD Studio by Firelight Technologies Pty Ltd.";
            this.logoFmod.render(this.alpha, "Made with FMOD Studio by Firelight Technologies Pty Ltd.");
        }
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (Mouse.isLeftDown() || GameKeyboard.isKeyDown(28) || GameKeyboard.isKeyDown(57) || GameKeyboard.isKeyDown(1)) {
            this.stage = 3;
        }

        if (this.stage == 0) {
            this.targetAlpha = 1.0F;
            if (this.alpha == 1.0F) {
                this.stage = 1;
                this.logoDisplayTime = 20.0F;
            }
        }

        if (this.stage == 1) {
            this.logoDisplayTime = this.logoDisplayTime - GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.logoDisplayTime <= 0.0F) {
                this.stage = 2;
            }
        }

        if (this.stage == 2) {
            this.targetAlpha = 0.0F;
            if (this.alpha == 0.0F) {
                if (this.screenNumber == 1) {
                    this.screenNumber = 2;
                    this.stage = 0;
                } else {
                    this.stage = 3;
                }
            }
        }

        if (this.stage == 3) {
            this.targetAlpha = 0.0F;
            if (this.alpha == 0.0F) {
                this.noRender = true;
                return GameStateMachine.StateAction.Continue;
            }
        }

        if (this.alpha < this.targetAlpha) {
            this.alpha = this.alpha + this.alphaStep * GameTime.getInstance().getMultiplier();
            if (this.alpha > this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        } else if (this.alpha > this.targetAlpha) {
            this.alpha = this.alpha - this.alphaStep * GameTime.getInstance().getMultiplier();
            if (this.stage == 3) {
                this.alpha = this.alpha - this.alphaStep * GameTime.getInstance().getMultiplier();
            }

            if (this.alpha < this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        }

        return GameStateMachine.StateAction.Remain;
    }

    private static final class LogoElement {
        Texture texture;
        int x;
        int y;
        int width;
        int height;

        LogoElement(String texture) {
            this.texture = Texture.getSharedTexture(texture);
            if (this.texture != null) {
                this.width = this.texture.getWidth();
                this.height = this.texture.getHeight();
            }
        }

        void centerOnScreen() {
            this.x = (Core.getInstance().getScreenWidth() - this.width) / 2;
            this.y = (Core.getInstance().getScreenHeight() - this.height) / 2;
        }

        void setBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        void render(float alpha, String textBelow) {
            if (this.texture != null && this.texture.isReady()) {
                float width = this.width;
                float height = this.height;
                float ratio = Math.min(width / this.texture.getWidth(), height / this.texture.getHeight());
                width = this.texture.getWidth() * ratio;
                height = this.texture.getHeight() * ratio;
                IndieGL.glEnable(3042);
                IndieGL.glBlendFunc(770, 771);
                SpriteRenderer.instance
                    .render(
                        this.texture,
                        this.x + (this.width - width) / 2.0F,
                        this.y + (this.height - height) / 2.0F,
                        width,
                        height,
                        1.0F,
                        1.0F,
                        1.0F,
                        alpha,
                        null
                    );
                if (textBelow != null) {
                    float textY = this.y + (this.height - height) / 2.0F + height + 16.0F;
                    TextManager.instance.DrawStringCentre(this.x + this.width / 2.0F, textY, textBelow, 1.0, 1.0, 1.0, alpha);
                }
            }
        }
    }
}
