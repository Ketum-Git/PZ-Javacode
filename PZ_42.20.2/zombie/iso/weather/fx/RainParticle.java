// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fx;

import java.util.Objects;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;

public class RainParticle extends WeatherParticle {
    private double angleRadians;
    private float lastAngle = -1.0F;
    private float lastIntensity = -1.0F;
    protected float angleOffset;
    private float alphaMod;
    private float incarnateAlpha = 1.0F;
    private float life;
    private final RainParticle.RenderPoints rp;
    private boolean angleUpdate;
    private float tmpAngle;

    public RainParticle(Texture texture, int height) {
        super(texture);
        if (height > 6) {
            this.bounds.setSize(Rand.Next(1, 2), height);
        } else {
            this.bounds.setSize(1, height);
        }

        this.oWidth = this.bounds.getWidth();
        this.oHeight = this.bounds.getHeight();
        this.recalcSizeOnZoom = true;
        this.zoomMultiW = 0.0F;
        this.zoomMultiH = 2.0F;
        this.setLife();
        this.rp = new RainParticle.RenderPoints();
        this.rp.setDimensions(this.oWidth, this.oHeight);
    }

    protected void setLife() {
        this.life = Rand.Next(20, 60);
    }

    @Override
    public void update(float delta) {
        this.angleUpdate = false;
        if (this.updateZoomSize()) {
            this.rp.setDimensions(this.oWidth, this.oHeight);
            this.angleUpdate = true;
        }

        if (this.angleUpdate || this.lastAngle != IsoWeatherFX.instance.windAngle || this.lastIntensity != IsoWeatherFX.instance.windPrecipIntensity.value()) {
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
            this.lastIntensity = IsoWeatherFX.instance.windPrecipIntensity.value();
            this.angleUpdate = true;
        }

        this.position.x = this.position.x
            + this.velocity.x * (1.0F + IsoWeatherFX.instance.windSpeed * 0.1F) * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
        this.position.y = this.position.y
            + this.velocity.y * (1.0F + IsoWeatherFX.instance.windSpeed * 0.1F) * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
        this.life--;
        if (this.life < 0.0F) {
            this.setLife();
            this.incarnateAlpha = 0.0F;
            this.position.set(Rand.Next(0, this.parent.getWidth()), Rand.Next(0, this.parent.getHeight()));
        }

        if (this.incarnateAlpha < 1.0F) {
            this.incarnateAlpha += 0.035F;
            if (this.incarnateAlpha > 1.0F) {
                this.incarnateAlpha = 1.0F;
            }
        }

        this.update(delta, false);
        this.bounds.setLocation((int)this.position.x, (int)this.position.y);
        if (this.angleUpdate) {
            this.tmpAngle += 90.0F;
            if (this.tmpAngle > 360.0F) {
                this.tmpAngle -= 360.0F;
            }

            if (this.tmpAngle < 0.0F) {
                this.tmpAngle += 360.0F;
            }

            this.angleRadians = Math.toRadians(this.tmpAngle);
            this.rp.rotate(this.angleRadians);
        }

        this.alphaMod = 1.0F - 0.2F * IsoWeatherFX.instance.windIntensity.value();
        this.renderAlpha = this.alpha * this.alphaMod * this.alphaFadeMod.value() * IsoWeatherFX.instance.indoorsAlphaMod.value() * this.incarnateAlpha;
        this.renderAlpha *= 0.55F;
        if (IsoWeatherFX.instance.playerIndoors) {
            this.renderAlpha *= 0.5F;
        }
    }

    @Override
    public void render(float offsetx, float offsety) {
        double x = offsetx + this.bounds.getX();
        double y = offsety + this.bounds.getY();
        if (PerformanceSettings.fboRenderChunk) {
            IsoWeatherFX.instance
                .getDrawer(this.parent.id)
                .addParticle(
                    this.texture,
                    (float)(x + this.rp.getX(0)),
                    (float)(y + this.rp.getY(0)),
                    (float)(x + this.rp.getX(1)),
                    (float)(y + this.rp.getY(1)),
                    (float)(x + this.rp.getX(2)),
                    (float)(y + this.rp.getY(2)),
                    (float)(x + this.rp.getX(3)),
                    (float)(y + this.rp.getY(3)),
                    this.color.r,
                    this.color.g,
                    this.color.b,
                    this.renderAlpha
                );
        } else {
            SpriteRenderer.instance
                .render(
                    this.texture,
                    x + this.rp.getX(0),
                    y + this.rp.getY(0),
                    x + this.rp.getX(1),
                    y + this.rp.getY(1),
                    x + this.rp.getX(2),
                    y + this.rp.getY(2),
                    x + this.rp.getX(3),
                    y + this.rp.getY(3),
                    this.color.r,
                    this.color.g,
                    this.color.b,
                    this.renderAlpha,
                    null
                );
        }
    }

    private class Point {
        private double origx;
        private double origy;
        private double x;
        private double y;

        private Point() {
            Objects.requireNonNull(RainParticle.this);
            super();
        }

        public void setOrig(double x, double y) {
            this.origx = x;
            this.origy = y;
            this.x = x;
            this.y = y;
        }

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private class RenderPoints {
        RainParticle.Point[] points;
        RainParticle.Point center;
        RainParticle.Point dim;

        public RenderPoints() {
            Objects.requireNonNull(RainParticle.this);
            super();
            this.points = new RainParticle.Point[4];
            this.center = RainParticle.this.new Point();
            this.dim = RainParticle.this.new Point();

            for (int i = 0; i < this.points.length; i++) {
                this.points[i] = RainParticle.this.new Point();
            }
        }

        public double getX(int i) {
            return this.points[i].x;
        }

        public double getY(int i) {
            return this.points[i].y;
        }

        public void setCenter(float x, float y) {
            this.center.set(x, y);
        }

        public void setDimensions(float w, float h) {
            this.dim.set(w, h);
            this.points[0].setOrig(-w / 2.0F, -h / 2.0F);
            this.points[1].setOrig(w / 2.0F, -h / 2.0F);
            this.points[2].setOrig(w / 2.0F, h / 2.0F);
            this.points[3].setOrig(-w / 2.0F, h / 2.0F);
        }

        public void rotate(double angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            for (int i = 0; i < this.points.length; i++) {
                this.points[i].x = this.points[i].origx * cos - this.points[i].origy * sin;
                this.points[i].y = this.points[i].origx * sin + this.points[i].origy * cos;
            }
        }
    }
}
