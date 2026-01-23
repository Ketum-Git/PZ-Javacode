// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.SoundManager;
import zombie.core.textures.Texture;
import zombie.network.GameServer;
import zombie.util.StringUtils;

public class HUDButton extends UIElement {
    boolean clicked;
    UIElement display;
    Texture highlight;
    Texture overicon;
    boolean mouseOver;
    String name;
    Texture texture;
    UIEventHandler handler;
    public float notclickedAlpha = 0.85F;
    public float clickedalpha = 1.0F;
    String activateSoundName = "UIActivateButton";

    public HUDButton(String name, double x, double y, String texture, String highlight, UIElement display) {
        if (!GameServer.server) {
            this.display = display;
            this.name = name;
            if (this.texture == null) {
                this.texture = Texture.getSharedTexture(texture);
                this.highlight = Texture.getSharedTexture(highlight);
            }

            this.x = x;
            this.y = y;
            this.width = this.texture.getWidth();
            this.height = this.texture.getHeight();
        }
    }

    public HUDButton(String name, float x, float y, String texture, String highlight, UIEventHandler handler) {
        if (!GameServer.server) {
            this.texture = Texture.getSharedTexture(texture);
            this.highlight = Texture.getSharedTexture(highlight);
            this.handler = handler;
            this.name = name;
            if (this.texture == null) {
                this.texture = Texture.getSharedTexture(texture);
                this.highlight = Texture.getSharedTexture(highlight);
            }

            this.x = x;
            this.y = y;
            this.width = this.texture.getWidth();
            this.height = this.texture.getHeight();
        }
    }

    public HUDButton(String name, float x, float y, String texture, String highlight, String overicon, UIElement display) {
        if (!GameServer.server) {
            this.overicon = Texture.getSharedTexture(overicon);
            this.display = display;
            this.texture = Texture.getSharedTexture(texture);
            this.highlight = Texture.getSharedTexture(highlight);
            this.name = name;
            if (this.texture == null) {
                this.texture = Texture.getSharedTexture(texture);
                this.highlight = Texture.getSharedTexture(highlight);
            }

            this.x = x;
            this.y = y;
            this.width = this.texture.getWidth();
            this.height = this.texture.getHeight();
        }
    }

    public HUDButton(String name, float x, float y, String texture, String highlight, String overicon, UIEventHandler handler) {
        if (!GameServer.server) {
            this.texture = Texture.getSharedTexture(texture);
            this.highlight = Texture.getSharedTexture(highlight);
            this.overicon = Texture.getSharedTexture(overicon);
            this.handler = handler;
            this.name = name;
            if (this.texture == null) {
                this.texture = Texture.getSharedTexture(texture);
                this.highlight = Texture.getSharedTexture(highlight);
            }

            this.x = x;
            this.y = y;
            this.width = this.texture.getWidth();
            this.height = this.texture.getHeight();
        }
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        this.clicked = true;
        return Boolean.TRUE;
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        this.mouseOver = true;
        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.clicked = false;
        this.mouseOver = false;
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        if (this.clicked) {
            if (!StringUtils.isNullOrWhitespace(this.activateSoundName)) {
                SoundManager.instance.playUISound(this.activateSoundName);
            }

            if (this.display != null) {
                this.display.ButtonClicked(this.name);
            } else if (this.handler != null) {
                this.handler.Selected(this.name, 0, 0);
            }
        }

        this.clicked = false;
        return Boolean.TRUE;
    }

    @Override
    public void render() {
        int dy = 0;
        if (this.clicked) {
            dy++;
        }

        if (!this.mouseOver && !this.name.equals(this.display.getClickedValue())) {
            this.DrawTextureScaled(this.texture, 0.0, dy, this.getWidth(), this.getHeight(), this.notclickedAlpha);
        } else {
            this.DrawTextureScaled(this.highlight, 0.0, dy, this.getWidth(), this.getHeight(), this.clickedalpha);
        }

        if (this.overicon != null) {
            this.DrawTextureScaled(this.overicon, 0.0, dy, this.overicon.getWidth(), this.overicon.getHeight(), 1.0);
        }

        super.render();
    }

    @Override
    public void update() {
        super.update();
    }
}
