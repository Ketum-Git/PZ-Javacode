// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fx;

import java.util.ArrayList;
import org.joml.Matrix4f;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.iso.IsoCamera;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class IsoWeatherFX {
    private static final boolean VERBOSE = false;
    protected static boolean debugBounds;
    private static float delta;
    private ParticleRectangle cloudParticles;
    private ParticleRectangle fogParticles;
    private ParticleRectangle snowParticles;
    private ParticleRectangle rainParticles;
    public static int cloudId;
    public static int fogId = 1;
    public static int snowId = 2;
    public static int rainId = 3;
    public static float zoomMod = 1.0F;
    protected boolean playerIndoors;
    protected SteppedUpdateFloat windPrecipIntensity = new SteppedUpdateFloat(0.0F, 0.025F, 0.0F, 1.0F);
    protected SteppedUpdateFloat windIntensity = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected SteppedUpdateFloat windAngleIntensity = new SteppedUpdateFloat(0.0F, 0.005F, -1.0F, 1.0F);
    protected SteppedUpdateFloat precipitationIntensity = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected SteppedUpdateFloat precipitationIntensitySnow = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected SteppedUpdateFloat precipitationIntensityRain = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected SteppedUpdateFloat cloudIntensity = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected SteppedUpdateFloat fogIntensity = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected SteppedUpdateFloat windAngleMod = new SteppedUpdateFloat(0.0F, 0.005F, 0.0F, 1.0F);
    protected boolean precipitationIsSnow = true;
    private float fogOverlayAlpha;
    private final float windSpeedMax = 6.0F;
    protected float windSpeed;
    protected float windSpeedFog;
    protected float windAngle = 90.0F;
    protected float windAngleClouds = 90.0F;
    private Texture texFogCircle;
    private Texture texFogWhite;
    private final Color fogColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    protected SteppedUpdateFloat indoorsAlphaMod = new SteppedUpdateFloat(1.0F, 0.05F, 0.0F, 1.0F);
    private final ArrayList<ParticleRectangle> particleRectangles = new ArrayList<>(0);
    private final WeatherParticleDrawer[][][] drawers = new WeatherParticleDrawer[4][3][4];
    protected static IsoWeatherFX instance;
    private float windUpdCounter;
    static Shader shader;
    static final IsoWeatherFX.Drawer[][] s_drawer = new IsoWeatherFX.Drawer[4][3];

    public IsoWeatherFX() {
        instance = this;
    }

    public void init() {
        if (!GameServer.server) {
            int id = 0;
            Texture[] cloudTextures = new Texture[6];

            for (int i = 0; i < cloudTextures.length; i++) {
                cloudTextures[i] = Texture.getSharedTexture("media/textures/weather/clouds_" + i + ".png");
                if (cloudTextures[i] == null) {
                    DebugLog.log("Missing texture: media/textures/weather/clouds_" + i + ".png");
                }
            }

            this.cloudParticles = new ParticleRectangle(cloudId, 8192, 4096);
            WeatherParticle[] cloudparticles = new WeatherParticle[16];

            for (int ix = 0; ix < cloudparticles.length; ix++) {
                Texture tex = cloudTextures[Rand.Next(cloudTextures.length)];
                CloudParticle p = new CloudParticle(tex, tex.getWidth() * 8, tex.getHeight() * 8);
                p.position.set(Rand.Next(0, this.cloudParticles.getWidth()), Rand.Next(0, this.cloudParticles.getHeight()));
                p.speed = Rand.Next(0.01F, 0.1F);
                p.angleOffset = 180.0F - Rand.Next(0.0F, 360.0F);
                p.alpha = Rand.Next(0.25F, 0.75F);
                cloudparticles[ix] = p;
            }

            this.cloudParticles.SetParticles(cloudparticles);
            this.cloudParticles.SetParticlesStrength(1.0F);
            this.particleRectangles.add(id, this.cloudParticles);
            cloudId = id++;
            if (this.texFogCircle == null) {
                this.texFogCircle = Texture.getSharedTexture("media/textures/weather/fogcircle_tex.png", 35);
            }

            if (this.texFogWhite == null) {
                this.texFogWhite = Texture.getSharedTexture("media/textures/weather/fogwhite_tex.png", 35);
            }

            Texture[] fogTextures = new Texture[6];

            for (int ix = 0; ix < fogTextures.length; ix++) {
                fogTextures[ix] = Texture.getSharedTexture("media/textures/weather/fog_" + ix + ".png");
                if (fogTextures[ix] == null) {
                    DebugLog.log("Missing texture: media/textures/weather/fog_" + ix + ".png");
                }
            }

            this.fogParticles = new ParticleRectangle(fogId, 2048, 1024);
            WeatherParticle[] particles = new WeatherParticle[16];

            for (int ixx = 0; ixx < particles.length; ixx++) {
                Texture tex = fogTextures[Rand.Next(fogTextures.length)];
                FogParticle p = new FogParticle(tex, tex.getWidth() * 2, tex.getHeight() * 2);
                p.position.set(Rand.Next(0, this.fogParticles.getWidth()), Rand.Next(0, this.fogParticles.getHeight()));
                p.speed = Rand.Next(0.01F, 0.1F);
                p.angleOffset = 180.0F - Rand.Next(0.0F, 360.0F);
                p.alpha = Rand.Next(0.05F, 0.25F);
                particles[ixx] = p;
            }

            this.fogParticles.SetParticles(particles);
            this.fogParticles.SetParticlesStrength(1.0F);
            this.particleRectangles.add(id, this.fogParticles);
            fogId = id++;
            Texture[] snowTextures = new Texture[3];

            for (int ixx = 0; ixx < snowTextures.length; ixx++) {
                snowTextures[ixx] = Texture.getSharedTexture("media/textures/weather/snow_" + (ixx + 1) + ".png");
                if (snowTextures[ixx] == null) {
                    DebugLog.log("Missing texture: media/textures/weather/snow_" + (ixx + 1) + ".png");
                }
            }

            this.snowParticles = new ParticleRectangle(snowId, 512, 512);
            WeatherParticle[] particlesSnow = new WeatherParticle[1024];

            for (int ixxx = 0; ixxx < particlesSnow.length; ixxx++) {
                SnowParticle p = new SnowParticle(snowTextures[Rand.Next(snowTextures.length)]);
                p.position.set(Rand.Next(0, this.snowParticles.getWidth()), Rand.Next(0, this.snowParticles.getHeight()));
                p.speed = Rand.Next(1.0F, 2.0F);
                p.angleOffset = 15.0F - Rand.Next(0.0F, 30.0F);
                p.alpha = Rand.Next(0.25F, 0.6F);
                particlesSnow[ixxx] = p;
            }

            this.snowParticles.SetParticles(particlesSnow);
            this.particleRectangles.add(id, this.snowParticles);
            snowId = id++;
            this.rainParticles = new ParticleRectangle(rainId, 512, 512);
            WeatherParticle[] particlesRain = new WeatherParticle[1024];

            for (int ixxx = 0; ixxx < particlesRain.length; ixxx++) {
                RainParticle p = new RainParticle(this.texFogWhite, Rand.Next(5, 12));
                p.position.set(Rand.Next(0, this.rainParticles.getWidth()), Rand.Next(0, this.rainParticles.getHeight()));
                p.speed = Rand.Next(7, 12);
                p.angleOffset = 3.0F - Rand.Next(0.0F, 6.0F);
                p.alpha = Rand.Next(0.5F, 0.8F);
                p.color = new Color(Rand.Next(0.75F, 0.8F), Rand.Next(0.85F, 0.9F), Rand.Next(0.95F, 1.0F), 1.0F);
                particlesRain[ixxx] = p;
            }

            this.rainParticles.SetParticles(particlesRain);
            this.particleRectangles.add(id, this.rainParticles);
            rainId = id++;
        }
    }

    public void update() {
        if (!GameServer.server) {
            this.playerIndoors = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
            GameTime gt = GameTime.getInstance();
            delta = gt.getMultiplier();
            if (!WeatherFxMask.playerHasMaskToDraw(IsoCamera.frameState.playerIndex)) {
                if (this.playerIndoors && this.indoorsAlphaMod.value() > 0.0F) {
                    this.indoorsAlphaMod.setTarget(this.indoorsAlphaMod.value() - 0.05F * delta);
                } else if (!this.playerIndoors && this.indoorsAlphaMod.value() < 1.0F) {
                    this.indoorsAlphaMod.setTarget(this.indoorsAlphaMod.value() + 0.05F * delta);
                }
            } else {
                this.indoorsAlphaMod.setTarget(1.0F);
            }

            this.indoorsAlphaMod.update(delta);
            this.cloudIntensity.update(delta);
            this.windIntensity.update(delta);
            this.windPrecipIntensity.update(delta);
            this.windAngleIntensity.update(delta);
            this.precipitationIntensity.update(delta);
            this.fogIntensity.update(delta);
            if (this.precipitationIsSnow) {
                this.precipitationIntensitySnow.setTarget(this.precipitationIntensity.getTarget());
            } else {
                this.precipitationIntensitySnow.setTarget(0.0F);
            }

            if (!this.precipitationIsSnow) {
                this.precipitationIntensityRain.setTarget(this.precipitationIntensity.getTarget());
            } else {
                this.precipitationIntensityRain.setTarget(0.0F);
            }

            if (this.precipitationIsSnow) {
                this.windAngleMod.setTarget(0.3F);
            } else {
                this.windAngleMod.setTarget(0.6F);
            }

            this.precipitationIntensitySnow.update(delta);
            this.precipitationIntensityRain.update(delta);
            this.windAngleMod.update(delta);
            float f = this.fogIntensity.value() * this.indoorsAlphaMod.value();
            this.fogOverlayAlpha = 0.8F * f;
            if (++this.windUpdCounter > 15.0F) {
                this.windUpdCounter = 0.0F;
                if (this.windAngleIntensity.value() > 0.0F) {
                    this.windAngle = lerp(this.windPrecipIntensity.value(), 90.0F, 0.0F + 54.0F * this.windAngleMod.value());
                    if (this.windAngleIntensity.value() < 0.5F) {
                        this.windAngleClouds = lerp(this.windAngleIntensity.value() * 2.0F, 90.0F, 0.0F);
                    } else {
                        this.windAngleClouds = lerp((this.windAngleIntensity.value() - 0.5F) * 2.0F, 360.0F, 270.0F);
                    }
                } else if (this.windAngleIntensity.value() < 0.0F) {
                    this.windAngle = lerp(Math.abs(this.windPrecipIntensity.value()), 90.0F, 180.0F - 54.0F * this.windAngleMod.value());
                    this.windAngleClouds = lerp(Math.abs(this.windAngleIntensity.value()), 90.0F, 270.0F);
                } else {
                    this.windAngle = 90.0F;
                }

                this.windSpeed = 6.0F * this.windPrecipIntensity.value();
                this.windSpeedFog = 6.0F * this.windIntensity.value() * (4.0F + 16.0F * Math.abs(this.windAngleIntensity.value()));
                if (this.windSpeed < 1.0F) {
                    this.windSpeed = 1.0F;
                }

                if (this.windSpeedFog < 1.0F) {
                    this.windSpeedFog = 1.0F;
                }
            }

            float zoom = Core.getInstance().getZoom(IsoPlayer.getInstance().getPlayerNum());
            float mod = 1.0F - (zoom - 0.5F) * 0.5F * 0.75F;
            zoomMod = 0.0F;
            if (Core.getInstance().isZoomEnabled() && zoom > 1.0F) {
                zoomMod = ClimateManager.clamp(0.0F, 1.0F, (zoom - 1.0F) * 0.6666667F);
            }

            if (this.cloudIntensity.value() <= 0.0F) {
                this.cloudParticles.SetParticlesStrength(0.0F);
            } else {
                this.cloudParticles.SetParticlesStrength(1.0F);
            }

            if (this.fogIntensity.value() <= 0.0F) {
                this.fogParticles.SetParticlesStrength(0.0F);
            } else {
                this.fogParticles.SetParticlesStrength(1.0F);
            }

            this.snowParticles.SetParticlesStrength(this.precipitationIntensitySnow.value() * mod);
            this.rainParticles.SetParticlesStrength(this.precipitationIntensityRain.value() * mod);

            for (int i = 0; i < this.particleRectangles.size(); i++) {
                if (this.particleRectangles.get(i).requiresUpdate()) {
                    this.particleRectangles.get(i).update(delta);
                }
            }
        }
    }

    public void setDebugBounds(boolean b) {
        debugBounds = b;
    }

    public boolean isDebugBounds() {
        return debugBounds;
    }

    public void setWindAngleIntensity(float intensity) {
        this.windAngleIntensity.setTarget(intensity);
    }

    public float getWindAngleIntensity() {
        return this.windAngleIntensity.value();
    }

    public float getRenderWindAngleRain() {
        return this.windAngle;
    }

    public void setWindPrecipIntensity(float intensity) {
        this.windPrecipIntensity.setTarget(intensity);
    }

    public float getWindPrecipIntensity() {
        return this.windPrecipIntensity.value();
    }

    public void setWindIntensity(float intensity) {
        this.windIntensity.setTarget(intensity);
    }

    public float getWindIntensity() {
        return this.windIntensity.value();
    }

    public void setFogIntensity(float intensity) {
        if (SandboxOptions.instance.maxFogIntensity.getValue() == 2) {
            intensity = Math.min(intensity, 0.75F);
        } else if (SandboxOptions.instance.maxFogIntensity.getValue() == 3) {
            intensity = Math.min(intensity, 0.5F);
        } else if (SandboxOptions.instance.maxFogIntensity.getValue() == 4) {
            intensity = Math.min(intensity, 0.0F);
        }

        this.fogIntensity.setTarget(intensity);
    }

    public float getFogIntensity() {
        return this.fogIntensity.value();
    }

    public void setCloudIntensity(float intensity) {
        this.cloudIntensity.setTarget(intensity);
    }

    public float getCloudIntensity() {
        return this.cloudIntensity.value();
    }

    public void setPrecipitationIntensity(float intensity) {
        if (SandboxOptions.instance.maxRainFxIntensity.getValue() == 2) {
            intensity *= 0.75F;
        } else if (SandboxOptions.instance.maxRainFxIntensity.getValue() == 3) {
            intensity *= 0.5F;
        }

        if (intensity > 0.0F) {
            intensity = 0.05F + 0.95F * intensity;
        }

        this.precipitationIntensity.setTarget(intensity);
    }

    public float getPrecipitationIntensity() {
        return this.precipitationIntensity.value();
    }

    public void setPrecipitationIsSnow(boolean b) {
        this.precipitationIsSnow = b;
    }

    public boolean getPrecipitationIsSnow() {
        return this.precipitationIsSnow;
    }

    public boolean hasCloudsToRender() {
        return this.cloudIntensity.value() > 0.0F || this.particleRectangles.get(cloudId).requiresUpdate();
    }

    public boolean hasPrecipitationToRender() {
        return this.precipitationIntensity.value() > 0.0F
            || this.particleRectangles.get(snowId).requiresUpdate()
            || this.particleRectangles.get(rainId).requiresUpdate();
    }

    public boolean hasFogToRender() {
        return this.fogIntensity.value() > 0.0F || this.particleRectangles.get(fogId).requiresUpdate();
    }

    public void render() {
        if (!GameServer.server) {
            for (int i = 0; i < this.particleRectangles.size(); i++) {
                if (i == fogId) {
                    if (PerformanceSettings.fogQuality != 2) {
                        continue;
                    }

                    this.renderFogCircle();
                }

                if ((i != rainId && i != snowId || Core.getInstance().getOptionRenderPrecipitation() <= 2) && this.particleRectangles.get(i).requiresUpdate()) {
                    this.particleRectangles.get(i).render();
                    IsoWorld.instance.getCell().getWeatherFX().getDrawer(i).endFrame();
                }
            }
        }
    }

    public void renderLayered(boolean doClouds, boolean doFog, boolean doPrecip) {
        if (doClouds) {
            this.renderClouds();
        } else if (doFog) {
            this.renderFog();
        } else if (doPrecip) {
            this.renderPrecipitation();
        }
    }

    public void renderClouds() {
        if (!GameServer.server) {
            if (this.particleRectangles.get(cloudId).requiresUpdate()) {
                this.particleRectangles.get(cloudId).render();
                IsoWorld.instance.getCell().getWeatherFX().getDrawer(cloudId).endFrame();
            }
        }
    }

    public void renderFog() {
        if (!GameServer.server) {
            this.renderFogCircle();
            if (this.particleRectangles.get(fogId).requiresUpdate()) {
                this.particleRectangles.get(fogId).render();
                IsoWorld.instance.getCell().getWeatherFX().getDrawer(fogId).endFrame();
            }
        }
    }

    public void renderPrecipitation() {
        if (!GameServer.server) {
            if (this.particleRectangles.get(snowId).requiresUpdate()) {
                this.particleRectangles.get(snowId).render();
                IsoWorld.instance.getCell().getWeatherFX().getDrawer(snowId).endFrame();
            }

            if (this.particleRectangles.get(rainId).requiresUpdate()) {
                this.particleRectangles.get(rainId).render();
                IsoWorld.instance.getCell().getWeatherFX().getDrawer(rainId).endFrame();
            }
        }
    }

    private void renderFogCircle() {
        if (!(this.fogOverlayAlpha <= 0.0F)) {
            int player = IsoCamera.frameState.playerIndex;
            float zoom = Core.getInstance().getCurrentPlayerZoom();
            int screenW = IsoCamera.getScreenWidth(player);
            int screenH = IsoCamera.getScreenHeight(player);
            int circleWidth = 2048 - (int)(512.0F * this.fogIntensity.value());
            int circleHeight = 1024 - (int)(256.0F * this.fogIntensity.value());
            circleWidth = (int)(circleWidth / zoom);
            circleHeight = (int)(circleHeight / zoom);
            int circleX = screenW / 2 - circleWidth / 2;
            int circleY = screenH / 2 - circleHeight / 2;
            circleX = (int)(circleX - IsoCamera.getRightClickOffX() / zoom);
            circleY = (int)(circleY - IsoCamera.getRightClickOffY() / zoom);
            int circleEndX = circleX + circleWidth;
            int circleEndY = circleY + circleHeight;
            SpriteRenderer.instance.glBind(this.texFogWhite.getID());
            IndieGL.glTexParameteri(3553, 10241, 9728);
            IndieGL.glTexParameteri(3553, 10240, 9728);
            if (shader == null) {
                RenderThread.invokeOnRenderContext(() -> shader = ShaderManager.instance.getOrCreateShader("fogCircle", false, false));
            }

            if (shader.getShaderProgram().isCompiled()) {
                IndieGL.StartShader(shader.getID(), player);
                int stateIndex = SpriteRenderer.instance.getMainStateIndex();
                if (s_drawer[player][stateIndex] == null) {
                    s_drawer[player][stateIndex] = new IsoWeatherFX.Drawer();
                }

                s_drawer[player][stateIndex].init(screenW, screenH);
            }

            IndieGL.disableDepthTest();
            SpriteRenderer.instance
                .renderi(
                    this.texFogCircle,
                    circleX,
                    circleY,
                    circleWidth,
                    circleHeight,
                    this.fogColor.r,
                    this.fogColor.g,
                    this.fogColor.b,
                    this.fogOverlayAlpha,
                    null
                );
            SpriteRenderer.instance
                .renderi(this.texFogWhite, 0, 0, circleX, screenH, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
            SpriteRenderer.instance
                .renderi(this.texFogWhite, circleX, 0, circleWidth, circleY, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
            SpriteRenderer.instance
                .renderi(
                    this.texFogWhite,
                    circleEndX,
                    0,
                    screenW - circleEndX,
                    screenH,
                    this.fogColor.r,
                    this.fogColor.g,
                    this.fogColor.b,
                    this.fogOverlayAlpha,
                    null
                );
            SpriteRenderer.instance
                .renderi(
                    this.texFogWhite,
                    circleX,
                    circleEndY,
                    circleWidth,
                    screenH - circleEndY,
                    this.fogColor.r,
                    this.fogColor.g,
                    this.fogColor.b,
                    this.fogOverlayAlpha,
                    null
                );
            if (shader.getShaderProgram().isCompiled()) {
                IndieGL.EndShader();
            }

            if (Core.getInstance().getOffscreenBuffer() != null) {
                if (Core.getInstance().isZoomEnabled() && Core.getInstance().getZoom(player) > 0.5F) {
                    IndieGL.glTexParameteri(3553, 10241, 9729);
                } else {
                    IndieGL.glTexParameteri(3553, 10241, 9728);
                }

                if (Core.getInstance().getZoom(player) == 0.5F) {
                    IndieGL.glTexParameteri(3553, 10240, 9728);
                } else {
                    IndieGL.glTexParameteri(3553, 10240, 9729);
                }
            }
        }
    }

    public static float clamp(float min, float max, float val) {
        val = Math.min(max, val);
        return Math.max(min, val);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static float clerp(float t, float a, float b) {
        float t2 = (float)(1.0 - Math.cos(t * Math.PI)) / 2.0F;
        return a * (1.0F - t2) + b * t2;
    }

    public WeatherParticleDrawer getDrawer(int id) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        if (this.drawers[playerIndex][stateIndex][id] == null) {
            this.drawers[playerIndex][stateIndex][id] = new WeatherParticleDrawer();
        }

        return this.drawers[playerIndex][stateIndex][id];
    }

    public void Reset() {
        this.cloudParticles.Reset();
        this.fogParticles.Reset();
        this.snowParticles.Reset();
        this.rainParticles.Reset();
        this.cloudParticles = null;
        this.fogParticles = null;
        this.snowParticles = null;
        this.rainParticles = null;

        for (int p = 0; p < 4; p++) {
            for (int s = 0; s < 3; s++) {
                for (int id = 0; id < this.drawers[p][s].length; id++) {
                    if (this.drawers[p][s][id] != null) {
                        this.drawers[p][s][id].Reset();
                        this.drawers[p][s][id] = null;
                    }
                }
            }
        }
    }

    private static final class Drawer extends TextureDraw.GenericDrawer {
        static final Matrix4f s_matrix4f = new Matrix4f();
        final org.lwjgl.util.vector.Matrix4f mvp = new org.lwjgl.util.vector.Matrix4f();
        int width;
        int height;
        boolean set;

        void init(int w, int h) {
            if (w != this.width || h != this.height || !this.set) {
                this.width = w;
                this.height = h;
                this.set = false;
                s_matrix4f.setOrtho(0.0F, this.width, this.height, 0.0F, -1.0F, 1.0F);
                PZMath.convertMatrix(s_matrix4f, this.mvp);
                this.mvp.transpose();
                SpriteRenderer.instance.drawGeneric(this);
            }
        }

        @Override
        public void render() {
            IsoWeatherFX.shader.getShaderProgram().setValue("u_mvp", this.mvp);
            this.set = true;
        }
    }
}
