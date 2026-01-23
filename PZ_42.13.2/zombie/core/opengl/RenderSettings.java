// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SearchMode;
import zombie.iso.weather.ClimateColorInfo;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.WorldFlares;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;

public final class RenderSettings {
    private static RenderSettings instance;
    private static Texture texture;
    private static final float AMBIENT_MIN_SHADER = 0.4F;
    private static final float AMBIENT_MAX_SHADER = 1.0F;
    private static final float AMBIENT_MIN_LEGACY = 0.4F;
    private static final float AMBIENT_MAX_LEGACY = 1.0F;
    private final RenderSettings.PlayerRenderSettings[] playerSettings = new RenderSettings.PlayerRenderSettings[4];
    private final Color defaultClear = new Color(0, 0, 0, 1);

    public static RenderSettings getInstance() {
        if (instance == null) {
            instance = new RenderSettings();
        }

        return instance;
    }

    public RenderSettings() {
        for (int i = 0; i < this.playerSettings.length; i++) {
            this.playerSettings[i] = new RenderSettings.PlayerRenderSettings();
        }

        texture = Texture.getSharedTexture("media/textures/weather/fogwhite.png");
        if (texture == null) {
            DebugLog.log("Missing texture: media/textures/weather/fogwhite.png");
        }
    }

    public RenderSettings.PlayerRenderSettings getPlayerSettings(int playerIndex) {
        return this.playerSettings[playerIndex];
    }

    public void update() {
        if (!GameServer.server) {
            for (int i = 0; i < 4; i++) {
                if (IsoPlayer.players[i] != null) {
                    this.playerSettings[i].updateRenderSettings(i, IsoPlayer.players[i]);
                }
            }
        }
    }

    public void applyRenderSettings(int playerIndex) {
        if (!GameServer.server) {
            this.getPlayerSettings(playerIndex).applyRenderSettings(playerIndex);
        }
    }

    public void legacyPostRender(int playerIndex) {
        if (!GameServer.server) {
            if (SceneShaderStore.weatherShader == null || Core.getInstance().getOffscreenBuffer() == null) {
                this.getPlayerSettings(playerIndex).legacyPostRender(playerIndex);
            }
        }
    }

    public float getAmbientForPlayer(int plrIndex) {
        RenderSettings.PlayerRenderSettings plrSettings = this.getPlayerSettings(plrIndex);
        return plrSettings != null ? plrSettings.getAmbient() : 0.0F;
    }

    public Color getMaskClearColorForPlayer(int plrIndex) {
        RenderSettings.PlayerRenderSettings plrSettings = this.getPlayerSettings(plrIndex);
        return plrSettings != null ? plrSettings.getMaskClearColor() : this.defaultClear;
    }

    public static class PlayerRenderSettings {
        public ClimateColorInfo cmGlobalLight = new ClimateColorInfo();
        public float cmNightStrength;
        public float cmDesaturation;
        public float cmGlobalLightIntensity;
        public float cmAmbient;
        public float cmViewDistance;
        public float cmDayLightStrength;
        public float cmFogIntensity;
        private final Color blendColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
        private final ColorInfo blendInfo = new ColorInfo();
        private float blendIntensity;
        private float desaturation;
        private float darkness;
        private float night;
        private float viewDistance;
        private float ambient;
        private boolean applyNightVisionGoggles;
        private float goggleMod;
        private boolean isExterior;
        private float fogMod = 1.0F;
        private float rmod;
        private float gmod;
        private float bmod;
        private float smRadius;
        private float smAlpha;
        private float drunkFactor;
        private float blurFactor;
        private final Color maskClearColor = new Color(0, 0, 0, 1);

        private void updateRenderSettings(int playerIndex, IsoPlayer player) {
            SearchMode searchMode = SearchMode.getInstance();
            this.smAlpha = 0.0F;
            this.smRadius = 0.0F;
            ClimateManager cm = ClimateManager.getInstance();
            this.cmGlobalLight = cm.getGlobalLight();
            this.cmGlobalLightIntensity = cm.getGlobalLightIntensity();
            this.cmAmbient = cm.getAmbient();
            this.cmDayLightStrength = cm.getDayLightStrength();
            this.cmNightStrength = cm.getNightStrength();
            this.cmDesaturation = cm.getDesaturation();
            this.cmViewDistance = cm.getViewDistance();
            this.cmFogIntensity = cm.getFogIntensity();
            cm.getThunderStorm().applyLightningForPlayer(this, playerIndex, player);
            WorldFlares.applyFlaresForPlayer(this, playerIndex, player);
            this.desaturation = this.cmDesaturation;
            this.viewDistance = this.cmViewDistance;
            this.applyNightVisionGoggles = player != null && player.isWearingNightVisionGoggles();
            this.isExterior = player != null && (player.isDead() || player.getCurrentSquare() != null && !player.getCurrentSquare().isInARoom());
            this.fogMod = 1.0F - this.cmFogIntensity * 0.5F;
            this.night = this.cmNightStrength;
            this.darkness = 1.0F - this.cmDayLightStrength;
            this.isExterior = true;
            if (this.isExterior) {
                this.setBlendColor(this.cmGlobalLight.getExterior());
                this.blendIntensity = this.cmGlobalLight.getExterior().a;
            } else {
                this.setBlendColor(this.cmGlobalLight.getInterior());
                this.blendIntensity = this.cmGlobalLight.getInterior().a;
            }

            this.ambient = this.cmAmbient;
            this.viewDistance = this.cmViewDistance;
            int sv = SandboxOptions.instance.nightDarkness.getValue();

            float ambientMin = switch (sv) {
                case 1 -> 0.0F;
                case 2 -> 0.07F;
                case 3 -> 0.15F;
                case 4 -> 0.25F;
                default -> 0.15F;
            };
            ambientMin += 0.075F * ClimateMoon.getInstance().getMoonFloat() * this.night;
            if (!this.isExterior) {
                ambientMin *= 0.925F - 0.075F * this.darkness;
                this.desaturation *= 0.25F;
            }

            if (this.ambient < 0.2F && player != null && player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                this.ambient = 0.2F;
            }

            this.ambient = ambientMin + (1.0F - ambientMin) * this.ambient;
            if (Core.lastStand) {
                this.ambient = 0.65F;
                this.darkness = 0.25F;
                this.night = 0.25F;
            }

            if (SceneShaderStore.weatherShader == null || Core.getInstance().getOffscreenBuffer() == null) {
                this.desaturation = this.desaturation * (1.0F - this.darkness);
                this.blendInfo.r = this.blendColor.r;
                this.blendInfo.g = this.blendColor.g;
                this.blendInfo.b = this.blendColor.b;
                this.blendInfo.desaturate(this.desaturation);
                this.rmod = GameTime.getInstance().Lerp(1.0F, this.blendInfo.r, this.blendIntensity);
                this.gmod = GameTime.getInstance().Lerp(1.0F, this.blendInfo.g, this.blendIntensity);
                this.bmod = GameTime.getInstance().Lerp(1.0F, this.blendInfo.b, this.blendIntensity);
                if (this.applyNightVisionGoggles) {
                    this.goggleMod = 1.0F - 0.9F * this.darkness;
                    this.blendIntensity = 0.0F;
                    this.night = 0.0F;
                    this.ambient = 0.8F;
                    this.rmod = 1.0F;
                    this.gmod = 1.0F;
                    this.bmod = 1.0F;
                }
            } else if (this.applyNightVisionGoggles) {
                this.ambient = 1.0F;
                this.rmod = GameTime.getInstance().Lerp(1.0F, 0.7F, this.darkness);
                this.gmod = GameTime.getInstance().Lerp(1.0F, 0.7F, this.darkness);
                this.bmod = GameTime.getInstance().Lerp(1.0F, 0.7F, this.darkness);
                this.maskClearColor.r = 0.0F;
                this.maskClearColor.g = 0.0F;
                this.maskClearColor.b = 0.0F;
                this.maskClearColor.a = 0.0F;
            } else {
                this.desaturation = this.desaturation * (1.0F - this.darkness);
                this.blendInfo.r = this.blendColor.r;
                this.blendInfo.g = this.blendColor.g;
                this.blendInfo.b = this.blendColor.b;
                this.blendInfo.desaturate(this.desaturation);
                this.rmod = GameTime.getInstance().Lerp(1.0F, this.blendInfo.r, this.blendIntensity);
                this.gmod = GameTime.getInstance().Lerp(1.0F, this.blendInfo.g, this.blendIntensity);
                this.bmod = GameTime.getInstance().Lerp(1.0F, this.blendInfo.b, this.blendIntensity);
                if (!this.isExterior) {
                    this.maskClearColor.r = 0.0F;
                    this.maskClearColor.g = 0.0F;
                    this.maskClearColor.b = 0.0F;
                    this.maskClearColor.a = 0.0F;
                } else {
                    this.maskClearColor.r = 0.0F;
                    this.maskClearColor.g = 0.0F;
                    this.maskClearColor.b = 0.0F;
                    this.maskClearColor.a = 0.0F;
                }
            }

            if (player != null && !player.isDead()) {
                this.drunkFactor = player.getStats().get(CharacterStat.INTOXICATION) / 100.0F;
                this.blurFactor = player.getBlurFactor();
            }
        }

        private void applyRenderSettings(int playerIndex) {
            IsoGridSquare.rmod = this.rmod;
            IsoGridSquare.gmod = this.gmod;
            IsoGridSquare.bmod = this.bmod;
            IsoObject.rmod = this.rmod;
            IsoObject.gmod = this.gmod;
            IsoObject.bmod = this.bmod;
        }

        private void legacyPostRender(int plr) {
            SpriteRenderer.instance.glIgnoreStyles(true);
            if (this.applyNightVisionGoggles) {
                IndieGL.glBlendFunc(770, 768);
                SpriteRenderer.instance
                    .render(
                        RenderSettings.texture,
                        0.0F,
                        0.0F,
                        Core.getInstance().getOffscreenWidth(plr),
                        Core.getInstance().getOffscreenHeight(plr),
                        0.05F,
                        0.95F,
                        0.05F,
                        this.goggleMod,
                        null
                    );
                IndieGL.glBlendFunc(770, 771);
            } else {
                IndieGL.glBlendFunc(774, 774);
                SpriteRenderer.instance
                    .render(
                        RenderSettings.texture,
                        0.0F,
                        0.0F,
                        Core.getInstance().getOffscreenWidth(plr),
                        Core.getInstance().getOffscreenHeight(plr),
                        this.blendInfo.r,
                        this.blendInfo.g,
                        this.blendInfo.b,
                        1.0F,
                        null
                    );
                IndieGL.glBlendFunc(770, 771);
            }

            SpriteRenderer.instance.glIgnoreStyles(false);
        }

        public Color getBlendColor() {
            return this.blendColor;
        }

        public float getBlendIntensity() {
            return this.blendIntensity;
        }

        public float getDesaturation() {
            return this.desaturation;
        }

        public float getDarkness() {
            return this.darkness;
        }

        public float getNight() {
            return this.night;
        }

        public float getViewDistance() {
            return this.viewDistance;
        }

        public float getAmbient() {
            return this.ambient;
        }

        public boolean isApplyNightVisionGoggles() {
            return this.applyNightVisionGoggles;
        }

        public float getRmod() {
            return this.rmod;
        }

        public float getGmod() {
            return this.gmod;
        }

        public float getBmod() {
            return this.bmod;
        }

        public boolean isExterior() {
            return this.isExterior;
        }

        public float getFogMod() {
            return this.fogMod;
        }

        private void setBlendColor(Color c) {
            this.blendColor.a = c.a;
            this.blendColor.r = c.r;
            this.blendColor.g = c.g;
            this.blendColor.b = c.b;
        }

        public Color getMaskClearColor() {
            return this.maskClearColor;
        }

        public float getSM_Radius() {
            return this.smRadius;
        }

        public float getSM_Alpha() {
            return this.smAlpha;
        }

        public float getDrunkFactor() {
            return this.drunkFactor;
        }

        public float getBlurFactor() {
            return this.blurFactor;
        }
    }
}
