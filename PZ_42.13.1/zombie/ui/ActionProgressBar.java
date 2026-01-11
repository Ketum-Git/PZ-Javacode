// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;

@UsedFromLua
public final class ActionProgressBar extends UIElement {
    Texture background;
    Texture foreground;
    float deltaValue = 1.0F;
    float animationProgress;
    public int delayHide;
    private final int offsetX;
    private final int offsetY;

    public ActionProgressBar(int x, int y) {
        this.background = Texture.getSharedTexture("BuildBar_Bkg");
        this.foreground = Texture.getSharedTexture("BuildBar_Bar");
        this.offsetX = x;
        this.offsetY = y;
        this.width = this.background.getWidth();
        this.height = this.background.getHeight();
        this.followGameWorld = true;
    }

    @Override
    public void render() {
        if (this.isVisible() && UIManager.visibleAllUi) {
            float scale = Core.getInstance().getOptionActionProgressBarSize();
            IndieGL.glBlendFuncSeparate(770, 771, 1, 771);
            this.DrawUVSliceTexture(
                this.background, 0.0, 0.0, this.background.getWidth() * scale, this.background.getHeight() * scale, Color.white, 0.0, 0.0, 1.0, 1.0
            );
            float fgY = this.foreground.offsetY * scale - this.foreground.offsetY;
            float fgH = this.foreground.getHeight() + 1;
            float fgW = this.foreground.getWidth() + 1;
            if (this.deltaValue == Float.POSITIVE_INFINITY) {
                if (this.animationProgress < 0.5F) {
                    this.DrawUVSliceTexture(
                        this.foreground, 3.0F * scale, fgY, fgW * scale, fgH * scale, Color.white, 0.0, 0.0, this.animationProgress * 2.0F, 1.0
                    );
                } else {
                    this.DrawUVSliceTexture(
                        this.foreground, 3.0F * scale, fgY, fgW * scale, fgH * scale, Color.white, (this.animationProgress - 0.5F) * 2.0F, 0.0, 1.0, 1.0
                    );
                }
            } else {
                this.DrawUVSliceTexture(this.foreground, 3.0F * scale, fgY, fgW * scale, fgH * scale, Color.white, 0.0, 0.0, this.deltaValue, 1.0);
            }
        }
    }

    public void setValue(float delta) {
        this.deltaValue = delta;
    }

    public float getValue() {
        return this.deltaValue;
    }

    public void update(int nPlayer) {
        if (this.deltaValue == Float.POSITIVE_INFINITY) {
            this.animationProgress = this.animationProgress + 0.02F * (GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 60.0F);
            if (this.animationProgress > 1.0F) {
                this.animationProgress = 0.0F;
            }

            this.setVisible(true);
            this.updateScreenPos(nPlayer);
            this.delayHide = 2;
        } else {
            if (this.getValue() > 0.0F && this.getValue() < 1.0F) {
                this.setVisible(true);
                this.delayHide = 2;
            } else if (this.isVisible() && this.delayHide > 0 && --this.delayHide == 0) {
                this.setVisible(false);
            }

            if (!UIManager.visibleAllUi) {
                this.setVisible(false);
            }

            if (this.isVisible()) {
                this.updateScreenPos(nPlayer);
            }
        }
    }

    private void updateScreenPos(int nPlayer) {
        IsoPlayer player = IsoPlayer.players[nPlayer];
        if (player != null) {
            float scale = Core.getInstance().getOptionActionProgressBarSize();
            this.width = this.background.getWidth() * scale;
            this.height = this.background.getHeight() * scale;
            float sx = IsoUtils.XToScreen(player.getX(), player.getY(), player.getZ(), 0);
            float sy = IsoUtils.YToScreen(player.getX(), player.getY(), player.getZ(), 0);
            sx = sx - IsoCamera.getOffX() - player.offsetX;
            sy = sy - IsoCamera.getOffY() - player.offsetY;
            sy -= 128 / (2 / Core.tileScale);
            sx /= Core.getInstance().getZoom(nPlayer);
            sy /= Core.getInstance().getZoom(nPlayer);
            sx -= this.width / 2.0F;
            sy -= this.height;
            if (player.getUserNameHeight() > 0) {
                sy -= player.getUserNameHeight() + 2;
            }

            this.setX(sx + this.offsetX);
            this.setY(sy + this.offsetY);
        }
    }
}
