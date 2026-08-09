// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class SearchMode {
    private static SearchMode instance;
    private float fadeTime = 1.0F;
    private final SearchMode.PlayerSearchMode[] plrModes = new SearchMode.PlayerSearchMode[4];

    public static SearchMode getInstance() {
        if (instance == null) {
            instance = new SearchMode();
        }

        return instance;
    }

    private SearchMode() {
        for (int i = 0; i < this.plrModes.length; i++) {
            this.plrModes[i] = new SearchMode.PlayerSearchMode(i, this);
            this.plrModes[i].blur.setTargets(1.0F, 1.0F);
            this.plrModes[i].desat.setTargets(0.85F, 0.85F);
            this.plrModes[i].radius.setTargets(4.0F, 4.0F);
            this.plrModes[i].darkness.setTargets(0.0F, 0.0F);
            this.plrModes[i].gradientWidth.setTargets(4.0F, 4.0F);
        }
    }

    public SearchMode.PlayerSearchMode getSearchModeForPlayer(int index) {
        return this.plrModes[index];
    }

    public float getFadeTime() {
        return this.fadeTime;
    }

    public void setFadeTime(float fadeTime) {
        this.fadeTime = fadeTime;
    }

    public boolean isOverride(int plrIdx) {
        return this.plrModes[plrIdx].override;
    }

    public void setOverride(int plrIdx, boolean enabled) {
        this.plrModes[plrIdx].override = enabled;
    }

    public boolean isOverrideSearchManager(int plrIdx) {
        return this.plrModes[plrIdx].overrideSearchManager;
    }

    public void setOverrideSearchManager(int plrIdx, boolean enabled) {
        this.plrModes[plrIdx].overrideSearchManager = enabled;
    }

    public SearchMode.SearchModeFloat getRadius(int plrIdx) {
        return this.plrModes[plrIdx].radius;
    }

    public SearchMode.SearchModeFloat getGradientWidth(int plrIdx) {
        return this.plrModes[plrIdx].gradientWidth;
    }

    public SearchMode.SearchModeFloat getBlur(int plrIdx) {
        return this.plrModes[plrIdx].blur;
    }

    public SearchMode.SearchModeFloat getDesat(int plrIdx) {
        return this.plrModes[plrIdx].desat;
    }

    public SearchMode.SearchModeFloat getDarkness(int plrIdx) {
        return this.plrModes[plrIdx].darkness;
    }

    public boolean isEnabled(int plrIdx) {
        return this.plrModes[plrIdx].enabled;
    }

    public void setEnabled(int plrIdx, boolean b) {
        SearchMode.PlayerSearchMode mode = this.plrModes[plrIdx];
        if (b && !mode.enabled) {
            mode.enabled = true;
            this.FadeIn(plrIdx);
        } else if (!b && mode.enabled) {
            mode.enabled = false;
            this.FadeOut(plrIdx);
        }
    }

    private void FadeIn(int plrIdx) {
        SearchMode.PlayerSearchMode playerSearchMode = this.plrModes[plrIdx];
        playerSearchMode.timer = Math.max(playerSearchMode.timer, 0.0F);
        playerSearchMode.doFadeIn = true;
        playerSearchMode.doFadeOut = false;
    }

    private void FadeOut(int plrIdx) {
        SearchMode.PlayerSearchMode playerSearchMode = this.plrModes[plrIdx];
        playerSearchMode.timer = Math.min(playerSearchMode.timer, this.fadeTime);
        playerSearchMode.doFadeIn = false;
        playerSearchMode.doFadeOut = true;
    }

    public void update() {
        for (int i = 0; i < this.plrModes.length; i++) {
            SearchMode.PlayerSearchMode plrSm = this.plrModes[i];
            plrSm.update();
        }
    }

    public static void reset() {
        instance = null;
    }

    @UsedFromLua
    public static class PlayerSearchMode {
        private final int plrIndex;
        private final SearchMode parent;
        private boolean override;
        private boolean overrideSearchManager;
        private boolean enabled;
        private final SearchMode.SearchModeFloat radius = new SearchMode.SearchModeFloat(0.0F, 50.0F, 1.0F);
        private final SearchMode.SearchModeFloat gradientWidth = new SearchMode.SearchModeFloat(0.0F, 20.0F, 1.0F);
        private final SearchMode.SearchModeFloat blur = new SearchMode.SearchModeFloat(0.0F, 1.0F, 0.01F);
        private final SearchMode.SearchModeFloat desat = new SearchMode.SearchModeFloat(0.0F, 1.0F, 0.01F);
        private final SearchMode.SearchModeFloat darkness = new SearchMode.SearchModeFloat(0.0F, 1.0F, 0.01F);
        private float timer;
        private boolean doFadeOut;
        private boolean doFadeIn;

        public PlayerSearchMode(int index, SearchMode sm) {
            this.plrIndex = index;
            this.parent = sm;
        }

        public boolean isShaderEnabled() {
            return this.enabled || this.doFadeIn || this.doFadeOut;
        }

        private boolean isPlayerExterior() {
            IsoPlayer player = IsoPlayer.players[this.plrIndex];
            return player != null && player.getCurrentSquare() != null && !player.getCurrentSquare().isInARoom();
        }

        public float getShaderBlur() {
            return this.isPlayerExterior() ? this.blur.getExterior() : this.blur.getInterior();
        }

        public float getShaderDesat() {
            return this.isPlayerExterior() ? this.desat.getExterior() : this.desat.getInterior();
        }

        public float getShaderRadius() {
            return this.isPlayerExterior() ? this.radius.getExterior() : this.radius.getInterior();
        }

        public float getShaderGradientWidth() {
            return this.isPlayerExterior() ? this.gradientWidth.getExterior() : this.gradientWidth.getInterior();
        }

        public float getShaderDarkness() {
            return this.isPlayerExterior() ? this.darkness.getExterior() : this.darkness.getInterior();
        }

        public SearchMode.SearchModeFloat getBlur() {
            return this.blur;
        }

        public SearchMode.SearchModeFloat getDesat() {
            return this.desat;
        }

        public SearchMode.SearchModeFloat getRadius() {
            return this.radius;
        }

        public SearchMode.SearchModeFloat getGradientWidth() {
            return this.gradientWidth;
        }

        public SearchMode.SearchModeFloat getDarkness() {
            return this.darkness;
        }

        private void update() {
            if (!this.override) {
                if (this.doFadeIn) {
                    this.timer = this.timer + GameTime.getInstance().getTimeDelta();
                    this.timer = PZMath.clamp(this.timer, 0.0F, this.parent.fadeTime);
                    float delta = PZMath.clamp(this.timer / this.parent.fadeTime, 0.0F, 1.0F);
                    this.blur.update(delta);
                    this.desat.update(delta);
                    this.radius.update(delta);
                    this.darkness.update(delta);
                    this.gradientWidth.equalise();
                    if (this.timer >= this.parent.fadeTime) {
                        this.doFadeIn = false;
                    }
                } else if (this.doFadeOut) {
                    this.timer = this.timer - GameTime.getInstance().getTimeDelta();
                    this.timer = PZMath.clamp(this.timer, 0.0F, this.parent.fadeTime);
                    float delta = PZMath.clamp(this.timer / this.parent.fadeTime, 0.0F, 1.0F);
                    this.blur.update(delta);
                    this.desat.update(delta);
                    this.radius.update(delta);
                    this.darkness.update(delta);
                    this.gradientWidth.equalise();
                    if (this.timer <= 0.0F) {
                        this.doFadeOut = false;
                    }
                } else {
                    if (this.enabled) {
                        this.blur.equalise();
                        this.desat.equalise();
                        this.radius.equalise();
                        this.darkness.equalise();
                        this.gradientWidth.equalise();
                    } else {
                        this.blur.reset();
                        this.desat.reset();
                        this.radius.reset();
                        this.darkness.reset();
                        this.gradientWidth.equalise();
                    }
                }
            }
        }
    }

    @UsedFromLua
    public static class SearchModeFloat {
        private final float min;
        private final float max;
        private final float stepsize;
        private float exterior;
        private float targetExterior;
        private float interior;
        private float targetInterior;

        private SearchModeFloat(float min, float max, float stepsize) {
            this.min = min;
            this.max = max;
            this.stepsize = stepsize;
        }

        public void set(float exterior, float targetExterior, float interior, float targetInterior) {
            this.setExterior(exterior);
            this.setTargetExterior(targetExterior);
            this.setInterior(interior);
            this.setTargetInterior(targetInterior);
        }

        public void setAll(float value) {
            this.setExterior(value);
            this.setTargetExterior(value);
            this.setInterior(value);
            this.setTargetInterior(value);
        }

        public void setTargets(float targetExterior, float targetInterior) {
            this.setTargetExterior(targetExterior);
            this.setTargetInterior(targetInterior);
        }

        public float getExterior() {
            return this.exterior;
        }

        public void setExterior(float exterior) {
            this.exterior = exterior;
        }

        public float getTargetExterior() {
            return this.targetExterior;
        }

        public void setTargetExterior(float targetExterior) {
            this.targetExterior = targetExterior;
        }

        public float getInterior() {
            return this.interior;
        }

        public void setInterior(float interior) {
            this.interior = interior;
        }

        public float getTargetInterior() {
            return this.targetInterior;
        }

        public void setTargetInterior(float targetInterior) {
            this.targetInterior = targetInterior;
        }

        public void update(float delta) {
            this.exterior = delta * this.targetExterior;
            this.interior = delta * this.targetInterior;
        }

        public void equalise() {
            if (!PZMath.equal(this.exterior, this.targetExterior, 0.001F)) {
                this.exterior = PZMath.lerp(this.exterior, this.targetExterior, 0.01F);
            } else {
                this.exterior = this.targetExterior;
            }

            if (!PZMath.equal(this.interior, this.targetInterior, 0.001F)) {
                this.interior = PZMath.lerp(this.interior, this.targetInterior, 0.01F);
            } else {
                this.interior = this.targetInterior;
            }
        }

        public void reset() {
            this.exterior = 0.0F;
            this.interior = 0.0F;
        }

        public void resetAll() {
            this.exterior = 0.0F;
            this.interior = 0.0F;
            this.targetInterior = 0.0F;
            this.targetExterior = 0.0F;
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }

        public float getStepsize() {
            return this.stepsize;
        }
    }
}
