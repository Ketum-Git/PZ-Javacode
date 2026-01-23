// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fx;

import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;

public class SnowParticle extends WeatherParticle {
    private double angleRadians;
    private float lastAngle = -1.0F;
    private final float lastIntensity = -1.0F;
    protected float angleOffset;
    private float alphaMod;
    private float incarnateAlpha = 1.0F;
    private float life;
    private final float fadeTime = 80.0F;
    private float tmpAngle;

    public SnowParticle(Texture texture) {
        super(texture);
        this.recalcSizeOnZoom = true;
        this.zoomMultiW = 1.0F;
        this.zoomMultiH = 1.0F;
    }

    protected void setLife() {
        this.life = 80.0F + Rand.Next(60, 500);
    }

    @Override
    public void update(float delta) {
        if (this.lastAngle != IsoWeatherFX.instance.windAngle || -1.0F != IsoWeatherFX.instance.windPrecipIntensity.value()) {
            this.tmpAngle = IsoWeatherFX.instance.windAngle + (this.angleOffset - this.angleOffset * 0.5F * IsoWeatherFX.instance.windPrecipIntensity.value());
            if (this.tmpAngle > 360.0F) {
                this.tmpAngle -= 360.0F;
            }

            if (this.tmpAngle < 0.0F) {
                this.tmpAngle += 360.0F;
            }

            this.angleRadians = Math.toRadians(this.tmpAngle);
            this.velocity.set((float)Math.cos(this.angleRadians) * this.speed, (float)Math.sin(this.angleRadians) * this.speed);
            this.lastAngle = IsoWeatherFX.instance.windAngle;
        }

        if (this.life >= 80.0F) {
            this.position.x = this.position.x
                + this.velocity.x * IsoWeatherFX.instance.windSpeed * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
            this.position.y = this.position.y
                + this.velocity.y * IsoWeatherFX.instance.windSpeed * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
        } else {
            this.incarnateAlpha = this.life / 80.0F;
        }

        this.life--;
        if (this.life < 0.0F) {
            this.setLife();
            this.incarnateAlpha = 0.0F;
            this.position.set(Rand.Next(0, this.parent.getWidth()), Rand.Next(0, this.parent.getHeight()));
        }

        if (this.incarnateAlpha < 1.0F) {
            this.incarnateAlpha += 0.05F;
            if (this.incarnateAlpha > 1.0F) {
                this.incarnateAlpha = 1.0F;
            }
        }

        super.update(delta);
        this.updateZoomSize();
        this.alphaMod = 1.0F - 0.2F * IsoWeatherFX.instance.windIntensity.value();
        this.renderAlpha = this.alpha * this.alphaMod * this.alphaFadeMod.value() * IsoWeatherFX.instance.indoorsAlphaMod.value() * this.incarnateAlpha;
        this.renderAlpha *= 0.7F;
    }

    @Override
    public void render(float offsetx, float offsety) {
        super.render(offsetx, offsety);
    }
}
