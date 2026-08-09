// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;

@UsedFromLua
public final class VehicleGauge extends UIElement {
    protected int needleX;
    protected int needleY;
    protected float minAngle;
    protected float maxAngle;
    protected float value;
    protected Texture texture;
    protected int needleWidth = 45;

    public VehicleGauge(Texture texture, int needleX, int needleY, float minAngle, float maxAngle) {
        this.texture = texture;
        this.needleX = needleX;
        this.needleY = needleY;
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.width = texture.getWidth();
        this.height = texture.getHeight();
    }

    public void setNeedleWidth(int newSize) {
        this.needleWidth = newSize;
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            super.render();
            this.DrawTexture(this.texture, 0.0, 0.0, 1.0);
            double radians = this.minAngle < this.maxAngle
                ? Math.toRadians(this.minAngle + (this.maxAngle - this.minAngle) * this.value)
                : Math.toRadians(this.maxAngle + (this.maxAngle - this.minAngle) * (1.0F - this.value));
            double baseX = this.needleX;
            double baseY = this.needleY;
            double tipX = this.needleX + this.needleWidth * Math.cos(radians);
            double tipY = Math.ceil(this.needleY + this.needleWidth * Math.sin(radians));
            int dx = this.getAbsoluteX().intValue();
            int dy = this.getAbsoluteY().intValue();
            SpriteRenderer.instance.renderline(null, dx + (int)baseX, dy + (int)baseY, dx + (int)tipX, dy + (int)tipY, 1.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public void setValue(float value) {
        this.value = Math.min(value, 1.0F);
    }

    public void setTexture(Texture newText) {
        this.texture = newText;
    }
}
