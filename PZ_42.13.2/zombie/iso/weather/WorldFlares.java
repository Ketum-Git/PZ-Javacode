// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.debug.LineDrawer;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.weather.fx.SteppedUpdateFloat;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class WorldFlares {
    public static final boolean ENABLED = true;
    public static boolean debugDraw;
    public static int nextId;
    private static final ArrayList<WorldFlares.Flare> flares = new ArrayList<>();

    public static void Clear() {
        flares.clear();
    }

    public static int getFlareCount() {
        return flares.size();
    }

    public static WorldFlares.Flare getFlare(int index) {
        return flares.get(index);
    }

    public static WorldFlares.Flare getFlareID(int id) {
        for (int i = 0; i < flares.size(); i++) {
            if (flares.get(i).id == id) {
                return flares.get(i);
            }
        }

        return null;
    }

    public static void launchFlare(float lifetime, int x, int y, int range, float windSpeed, float r, float g, float b, float ri, float gi, float bi) {
        if (flares.size() > 100) {
            flares.remove(0);
        }

        WorldFlares.Flare flare = new WorldFlares.Flare();
        flare.id = nextId++;
        flare.x = x;
        flare.y = y;
        flare.range = range;
        flare.windSpeed = windSpeed;
        flare.color.setExterior(r, g, b, 1.0F);
        flare.color.setInterior(ri, gi, bi, 1.0F);
        flare.hasLaunched = true;
        flare.maxLifeTime = lifetime;
        flares.add(flare);
    }

    public static void update() {
        for (int i = flares.size() - 1; i >= 0; i--) {
            flares.get(i).update();
            if (!flares.get(i).hasLaunched) {
                flares.remove(i);
            }
        }
    }

    public static void applyFlaresForPlayer(RenderSettings.PlayerRenderSettings renderSettings, int plrIndex, IsoPlayer player) {
        for (int i = flares.size() - 1; i >= 0; i--) {
            if (flares.get(i).hasLaunched) {
                flares.get(i).applyFlare(renderSettings, plrIndex, player);
            }
        }
    }

    public static void setDebugDraw(boolean b) {
        debugDraw = b;
    }

    public static boolean getDebugDraw() {
        return debugDraw;
    }

    public static void debugRender() {
        if (debugDraw) {
            double step = Math.PI / 20;
            float z = 0.0F;

            for (int i = flares.size() - 1; i >= 0; i--) {
                WorldFlares.Flare flare = flares.get(i);
                float range = 0.5F;

                for (double theta = 0.0; theta < Math.PI * 2; theta += Math.PI / 20) {
                    DrawIsoLine(
                        flare.x + flare.range * (float)Math.cos(theta),
                        flare.y + flare.range * (float)Math.sin(theta),
                        flare.x + flare.range * (float)Math.cos(theta + (Math.PI / 20)),
                        flare.y + flare.range * (float)Math.sin(theta + (Math.PI / 20)),
                        0.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.25F,
                        1
                    );
                    DrawIsoLine(
                        flare.x + 0.5F * (float)Math.cos(theta),
                        flare.y + 0.5F * (float)Math.sin(theta),
                        flare.x + 0.5F * (float)Math.cos(theta + (Math.PI / 20)),
                        flare.y + 0.5F * (float)Math.sin(theta + (Math.PI / 20)),
                        0.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.25F,
                        1
                    );
                }
            }
        }
    }

    private static void DrawIsoLine(float x, float y, float x2, float y2, float z, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z, 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    @UsedFromLua
    public static class Flare {
        private int id;
        private float x;
        private float y;
        private int range;
        private float windSpeed;
        private final ClimateColorInfo color = new ClimateColorInfo(1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        private boolean hasLaunched;
        private final SteppedUpdateFloat intensity = new SteppedUpdateFloat(0.0F, 0.01F, 0.0F, 1.0F);
        private float maxLifeTime;
        private float lifeTime;
        private int nextRandomTargetIntens = 10;
        private float perc;
        private final WorldFlares.PlayerFlareLightInfo[] infos = new WorldFlares.PlayerFlareLightInfo[4];

        public Flare() {
            for (int i = 0; i < this.infos.length; i++) {
                this.infos[i] = new WorldFlares.PlayerFlareLightInfo();
            }
        }

        public int getId() {
            return this.id;
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public int getRange() {
            return this.range;
        }

        public float getWindSpeed() {
            return this.windSpeed;
        }

        public ClimateColorInfo getColor() {
            return this.color;
        }

        public boolean isHasLaunched() {
            return this.hasLaunched;
        }

        public float getIntensity() {
            return this.intensity.value();
        }

        public float getMaxLifeTime() {
            return this.maxLifeTime;
        }

        public float getLifeTime() {
            return this.lifeTime;
        }

        public float getPercent() {
            return this.perc;
        }

        public float getIntensityPlayer(int index) {
            return this.infos[index].intensity;
        }

        public float getLerpPlayer(int index) {
            return this.infos[index].lerp;
        }

        public float getDistModPlayer(int index) {
            return this.infos[index].distMod;
        }

        public ClimateColorInfo getColorPlayer(int index) {
            return this.infos[index].flareCol;
        }

        public ClimateColorInfo getOutColorPlayer(int index) {
            return this.infos[index].outColor;
        }

        private int GetDistance(int dx, int dy, int sx, int sy) {
            return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
        }

        private void update() {
            if (this.hasLaunched) {
                if (this.lifeTime > this.maxLifeTime) {
                    this.hasLaunched = false;
                    return;
                }

                this.perc = this.lifeTime / this.maxLifeTime;
                this.nextRandomTargetIntens = (int)(this.nextRandomTargetIntens - GameTime.instance.getMultiplier());
                if (this.nextRandomTargetIntens <= 0) {
                    this.intensity.setTarget(Rand.Next(0.8F, 1.0F));
                    this.nextRandomTargetIntens = Rand.Next(5, 30);
                }

                this.intensity.update(GameTime.instance.getMultiplier());
                if (this.windSpeed > 0.0F) {
                    Vector2 add = new Vector2(
                        this.windSpeed
                            / 60.0F
                            * ClimateManager.getInstance().getWindIntensity()
                            * (float)Math.sin(ClimateManager.getInstance().getWindAngleRadians()),
                        this.windSpeed
                            / 60.0F
                            * ClimateManager.getInstance().getWindIntensity()
                            * (float)Math.cos(ClimateManager.getInstance().getWindAngleRadians())
                    );
                    this.x = this.x + add.x * GameTime.instance.getMultiplier();
                    this.y = this.y + add.y * GameTime.instance.getMultiplier();
                }

                for (int i = 0; i < 4; i++) {
                    WorldFlares.PlayerFlareLightInfo info = this.infos[i];
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null) {
                        info.intensity = 0.0F;
                    } else {
                        int dist = this.GetDistance((int)this.x, (int)this.y, (int)player.getX(), (int)player.getY());
                        if (dist > this.range) {
                            info.intensity = 0.0F;
                            info.lerp = 1.0F;
                        } else {
                            info.distMod = 1.0F - (float)dist / this.range;
                            if (this.perc < 0.75F) {
                                info.lerp = 0.0F;
                            } else {
                                info.lerp = (this.perc - 0.75F) / 0.25F;
                            }

                            info.intensity = this.intensity.value();
                        }

                        float lerp = (1.0F - info.lerp) * info.distMod * info.intensity;
                        ClimateManager.ClimateFloat var10000 = ClimateManager.getInstance().dayLightStrength;
                        var10000.finalValue = var10000.finalValue + (1.0F - ClimateManager.getInstance().dayLightStrength.finalValue) * lerp;
                        if (player != null) {
                            player.dirtyRecalcGridStackTime = 1.0F;
                        }
                    }
                }

                this.lifeTime = this.lifeTime + GameTime.instance.getMultiplier();
            }
        }

        private void applyFlare(RenderSettings.PlayerRenderSettings renderSettings, int plrIndex, IsoPlayer player) {
            WorldFlares.PlayerFlareLightInfo linfo = this.infos[plrIndex];
            if (linfo.distMod > 0.0F) {
                float DARKNESS = 1.0F - renderSettings.cmDayLightStrength;
                DARKNESS = renderSettings.cmNightStrength > DARKNESS ? renderSettings.cmNightStrength : DARKNESS;
                DARKNESS = PZMath.clamp(DARKNESS * 2.0F, 0.0F, 1.0F);
                float lerp = 1.0F - linfo.lerp;
                lerp *= linfo.distMod;
                ClimateColorInfo gl = renderSettings.cmGlobalLight;
                linfo.outColor.setTo(gl);
                Color var10000 = linfo.outColor.getExterior();
                float var10003 = DARKNESS * lerp * linfo.intensity;
                var10000.g = linfo.outColor.getExterior().g * (1.0F - var10003 * 0.5F);
                var10000 = linfo.outColor.getInterior();
                var10003 = DARKNESS * lerp * linfo.intensity;
                var10000.g = linfo.outColor.getInterior().g * (1.0F - var10003 * 0.5F);
                var10000 = linfo.outColor.getExterior();
                var10003 = DARKNESS * lerp * linfo.intensity;
                var10000.b = linfo.outColor.getExterior().b * (1.0F - var10003 * 0.8F);
                var10000 = linfo.outColor.getInterior();
                var10003 = DARKNESS * lerp * linfo.intensity;
                var10000.b = linfo.outColor.getInterior().b * (1.0F - var10003 * 0.8F);
                linfo.flareCol.setTo(this.color);
                linfo.flareCol.scale(DARKNESS);
                linfo.flareCol.getExterior().a = 1.0F;
                linfo.flareCol.getInterior().a = 1.0F;
                linfo.outColor.getExterior().r = linfo.outColor.getExterior().r > linfo.flareCol.getExterior().r
                    ? linfo.outColor.getExterior().r
                    : linfo.flareCol.getExterior().r;
                linfo.outColor.getExterior().g = linfo.outColor.getExterior().g > linfo.flareCol.getExterior().g
                    ? linfo.outColor.getExterior().g
                    : linfo.flareCol.getExterior().g;
                linfo.outColor.getExterior().b = linfo.outColor.getExterior().b > linfo.flareCol.getExterior().b
                    ? linfo.outColor.getExterior().b
                    : linfo.flareCol.getExterior().b;
                linfo.outColor.getExterior().a = linfo.outColor.getExterior().a > linfo.flareCol.getExterior().a
                    ? linfo.outColor.getExterior().a
                    : linfo.flareCol.getExterior().a;
                linfo.outColor.getInterior().r = linfo.outColor.getInterior().r > linfo.flareCol.getInterior().r
                    ? linfo.outColor.getInterior().r
                    : linfo.flareCol.getInterior().r;
                linfo.outColor.getInterior().g = linfo.outColor.getInterior().g > linfo.flareCol.getInterior().g
                    ? linfo.outColor.getInterior().g
                    : linfo.flareCol.getInterior().g;
                linfo.outColor.getInterior().b = linfo.outColor.getInterior().b > linfo.flareCol.getInterior().b
                    ? linfo.outColor.getInterior().b
                    : linfo.flareCol.getInterior().b;
                linfo.outColor.getInterior().a = linfo.outColor.getInterior().a > linfo.flareCol.getInterior().a
                    ? linfo.outColor.getInterior().a
                    : linfo.flareCol.getInterior().a;
                float useLerp = 1.0F - lerp * linfo.intensity;
                linfo.outColor.interp(gl, useLerp, gl);
                float ambient = ClimateManager.lerp(useLerp, 0.35F, renderSettings.cmAmbient);
                renderSettings.cmAmbient = renderSettings.cmAmbient > ambient ? renderSettings.cmAmbient : ambient;
                float daylight = ClimateManager.lerp(useLerp, 0.6F * linfo.intensity, renderSettings.cmDayLightStrength);
                renderSettings.cmDayLightStrength = renderSettings.cmDayLightStrength > daylight ? renderSettings.cmDayLightStrength : daylight;
                if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                    float desaturation = ClimateManager.lerp(useLerp, 1.0F * DARKNESS, renderSettings.cmDesaturation);
                    renderSettings.cmDesaturation = renderSettings.cmDesaturation > desaturation ? renderSettings.cmDesaturation : desaturation;
                }
            }
        }
    }

    private static class PlayerFlareLightInfo {
        private float intensity;
        private float lerp;
        private float distMod;
        private final ClimateColorInfo flareCol = new ClimateColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
        private final ClimateColorInfo outColor = new ClimateColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
