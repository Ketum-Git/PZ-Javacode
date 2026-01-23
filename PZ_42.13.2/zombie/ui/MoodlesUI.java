// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.Moodles.Moodle;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Registries;

@UsedFromLua
public final class MoodlesUI extends UIElement {
    private final Map<MoodleType, MoodlesUI.MoodleUIData> moodleUiState = new HashMap<>();
    private static final int MaxMouseOverSlot = 1000;
    private static final float DefaultMoodleDistY = 10.0F;
    private static final int DistFromRightEdge = 10;
    private static final float OFFSCREEN_Y = 10000.0F;
    private static final float OscillatorDecelerator = 0.96F;
    private static final float OscillatorRate = 0.8F;
    private static final float OscillatorScalar = 15.6F;
    private static final float OscillatorStartLevel = 1.0F;
    public float clientH;
    public float clientW;
    private static MoodlesUI instance;
    private float alpha = 1.0F;
    private final int[] textureSizes = new int[]{32, 48, 64, 80, 96, 128};
    private final MoodleTextureSet[] textureSets = new MoodleTextureSet[this.textureSizes.length];
    private MoodleTextureSet currentTextureSet;
    private float moodleDistY = 74.0F;
    private boolean mouseOver;
    private int mouseOverSlot;
    private int numUsedSlots;
    private int debugKeyDelay;
    private float oscillatorStep;
    private IsoGameCharacter isoGameCharacter;
    private boolean alphaIncrease = true;
    private final Color backgroundColour = new Color(Color.gray);

    public MoodlesUI() {
        this.x = Core.getInstance().getScreenWidth() - 10;
        this.y = 120.0;
        this.width = this.getTextureSizeForOption();
        this.height = Core.getInstance().getScreenHeight();

        for (int i = 0; i < this.textureSizes.length; i++) {
            this.textureSets[i] = new MoodleTextureSet(this.textureSizes[i]);
        }

        int textureSetIndex = this.getTextureSetIndexForSize((int)this.width);
        this.currentTextureSet = this.textureSets[textureSetIndex];

        for (MoodleType type : Registries.MOODLE_TYPE.values()) {
            this.moodleUiState.put(type, new MoodlesUI.MoodleUIData());
        }

        this.clientW = this.width;
        this.clientH = this.height;
        instance = this;
    }

    private int getTextureSizeForOption() {
        int moodleSizeIndex = Core.getInstance().getOptionMoodleSize() - 1;
        if (moodleSizeIndex >= 0 && moodleSizeIndex < this.textureSizes.length) {
            return this.textureSizes[moodleSizeIndex];
        } else {
            if (moodleSizeIndex == 6) {
                int fontSizeIndex = Core.getInstance().getOptionFontSizeReal() - 1;
                if (fontSizeIndex >= 0 && fontSizeIndex < this.textureSizes.length) {
                    return this.textureSizes[fontSizeIndex];
                }
            }

            return 32;
        }
    }

    private int getTextureSetIndexForSize(int size) {
        return switch (size) {
            case 32 -> 0;
            case 48 -> 1;
            case 64 -> 2;
            case 80 -> 3;
            case 96 -> 4;
            case 128 -> 5;
            default -> 0;
        };
    }

    private boolean isCurrentlyAnimating() {
        for (MoodlesUI.MoodleUIData moodleUIData : this.moodleUiState.values()) {
            if (moodleUIData.slotsPos != moodleUIData.slotsDesiredPos) {
                return true;
            }
        }

        return false;
    }

    private boolean isPercentageBackground(MoodleType moodleType) {
        return false;
    }

    private float getBackgroundPercentage(MoodleType moodleType) {
        return moodleType == MoodleType.ENDURANCE ? this.isoGameCharacter.getStats().get(CharacterStat.ENDURANCE) : 1.0F;
    }

    private void drawPercentageBackground(MoodleType moodleType, float WiggleOffset) {
        if (this.isVisible()) {
            MoodleTextureSet moodleTextureSet = this.currentTextureSet;
            Texture tex = moodleTextureSet.getBackground();
            if (tex != null) {
                this.DrawTextureCol(moodleTextureSet.getBackground(), (int)WiggleOffset, this.moodleUiState.get(moodleType).slotsPos, Color.gray);
                float percent = this.getBackgroundPercentage(moodleType);
                double padY = 8 * this.getTextureSizeForOption() / 128.0;
                double dx = WiggleOffset + this.getAbsoluteX();
                double dy = this.moodleUiState.get(moodleType).slotsPos + this.getAbsoluteY();
                dx += tex.offsetX;
                dy += tex.offsetY;
                double wid = tex.getWidth();
                double hei = tex.getHeight();
                double y = dy + this.yScroll;
                double clampY = Math.ceil(y + padY + (hei - padY * 2.0) * (1.0F - percent));
                double clampH = hei - (clampY - y);
                SpriteRenderer.instance
                    .renderClamped(
                        tex,
                        (int)(dx + this.xScroll),
                        (int)(dy + this.yScroll),
                        (int)wid,
                        (int)hei,
                        (int)(dx + this.xScroll),
                        (int)clampY,
                        (int)wid,
                        (int)clampH,
                        this.backgroundColour.r,
                        this.backgroundColour.g,
                        this.backgroundColour.b,
                        this.backgroundColour.a,
                        null
                    );
                SpriteRenderer.instance
                    .renderClamped(
                        tex,
                        (int)(dx + this.xScroll),
                        (int)(dy + this.yScroll),
                        (int)wid,
                        (int)hei,
                        (int)(dx + this.xScroll),
                        (int)clampY,
                        (int)wid,
                        2,
                        this.backgroundColour.r * 0.5F,
                        this.backgroundColour.g * 0.5F,
                        this.backgroundColour.b * 0.5F,
                        this.backgroundColour.a,
                        null
                    );
            }
        }
    }

    private void drawBackgroundPulse(MoodleType moodleType, float WiggleOffset) {
        if (this.isVisible()) {
            MoodleTextureSet moodleTextureSet = this.currentTextureSet;
            Texture tex = moodleTextureSet.getBackground();
            if (tex != null) {
                MoodlesUI.MoodleUIData moodleUIData = this.moodleUiState.get(moodleType);
                float prevValue = moodleUIData.slotsPulse1;
                float percent = this.getBackgroundPercentage(moodleType);
                boolean bIncreasing = percent >= prevValue;
                if (!GameTime.isGamePaused()) {
                    float dt = (float)(UIManager.getMillisSinceLastRender() / 2500.0) * 100.0F;
                    moodleUIData.slotsPulse2 += bIncreasing ? dt : -dt;
                }

                if (bIncreasing && moodleUIData.slotsPulse2 > 100.0F) {
                    moodleUIData.slotsPulse2 = 0.0F;
                } else if (!bIncreasing && moodleUIData.slotsPulse2 < 0.0F) {
                    moodleUIData.slotsPulse2 = 100.0F;
                }

                percent = moodleUIData.slotsPulse2 / 100.0F;
                float r = PZMath.lerp(Color.gray.r, this.backgroundColour.r, percent);
                float g = PZMath.lerp(Color.gray.r, this.backgroundColour.g, percent);
                float b = PZMath.lerp(Color.gray.r, this.backgroundColour.b, percent);
                this.DrawTextureColor(moodleTextureSet.getBackground(), (int)WiggleOffset, (int)moodleUIData.slotsPos, r, g, b, 1.0);
                double padY = 8 * this.getTextureSizeForOption() / 128.0;
                double dx = WiggleOffset + this.getAbsoluteX();
                double dy = moodleUIData.slotsPos + this.getAbsoluteY();
                dx += tex.offsetX;
                dy += tex.offsetY;
                double wid = tex.getWidth();
                double hei = tex.getHeight();
                double y = dy + this.yScroll;
                double clampY = Math.ceil(y + padY + (hei - padY * 2.0) * (1.0F - percent));
                double clampH = hei - (clampY - y);
                r = PZMath.lerp(this.backgroundColour.r, 1.0F, percent);
                g = PZMath.lerp(this.backgroundColour.g, 1.0F, percent);
                b = PZMath.lerp(this.backgroundColour.b, 1.0F, percent);
                float a = 0.33F;
                SpriteRenderer.instance
                    .renderClamped(
                        tex,
                        (int)(dx + this.xScroll),
                        (int)(dy + this.yScroll),
                        (int)wid,
                        (int)hei,
                        (int)(dx + this.xScroll),
                        (int)clampY,
                        (int)wid,
                        (int)clampH,
                        r,
                        g,
                        b,
                        0.33F,
                        null
                    );
                SpriteRenderer.instance
                    .renderClamped(
                        tex,
                        (int)(dx + this.xScroll),
                        (int)(dy + this.yScroll),
                        (int)wid,
                        (int)hei,
                        (int)(dx + this.xScroll),
                        (int)clampY,
                        (int)wid,
                        2,
                        r * 0.5F,
                        g * 0.5F,
                        b * 0.5F,
                        0.33F,
                        null
                    );
            }
        }
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (!this.isVisible()) {
            return false;
        } else {
            this.mouseOver = true;
            super.onMouseMove(dx, dy);
            this.mouseOverSlot = (int)((Mouse.getYA() - this.getY()) / this.moodleDistY);
            if (this.mouseOverSlot >= this.numUsedSlots) {
                this.mouseOverSlot = 1000;
            }

            return true;
        }
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        super.onMouseMoveOutside(dx, dy);
        this.mouseOverSlot = 1000;
        this.mouseOver = false;
    }

    @Override
    public void render() {
        int widthRequired = this.getTextureSizeForOption();
        if (widthRequired != this.currentTextureSet.getSize()) {
            int textureSetIndex = this.getTextureSetIndexForSize(widthRequired);
            this.currentTextureSet = this.textureSets[textureSetIndex];
            this.width = widthRequired;
        }

        if (this.isoGameCharacter != null) {
            if (this.moodleDistY != 10.0F + this.width) {
                this.isoGameCharacter.getMoodles().setMoodlesStateChanged(true);
                this.update();
            }

            float fpsFraction = (float)(UIManager.getMillisSinceLastRender() / 33.3F);
            this.oscillatorStep += 0.8F * fpsFraction * 0.5F;
            float oscillator = (float)Math.sin(this.oscillatorStep);
            int renderedSlot = 0;
            MoodleTextureSet moodleTextureSet = this.currentTextureSet;

            for (Entry<MoodleType, MoodlesUI.MoodleUIData> entry : this.moodleUiState.entrySet()) {
                MoodleType moodleType = entry.getKey();
                MoodlesUI.MoodleUIData moodleUIData = entry.getValue();
                if (moodleUIData.slotsPos != 10000.0F) {
                    float WiggleOffset = oscillator * 15.6F * moodleUIData.oscillationLevel;
                    this.backgroundColour.set(Color.gray);
                    switch (moodleUIData.goodBadNeutral) {
                        case 0:
                        default:
                            break;
                        case 1:
                            Color.abgrToColor(
                                Color.lerpABGR(
                                    Color.colorToABGR(new Color(Color.gray)),
                                    Color.colorToABGR(Core.getInstance().getGoodHighlitedColor().toColor()),
                                    moodleUIData.level / 4.0F
                                ),
                                this.backgroundColour
                            );
                            break;
                        case 2:
                            Color.abgrToColor(
                                Color.lerpABGR(
                                    Color.colorToABGR(new Color(Color.gray)),
                                    Color.colorToABGR(Core.getInstance().getBadHighlitedColor().toColor()),
                                    moodleUIData.level / 4.0F
                                ),
                                this.backgroundColour
                            );
                    }

                    Texture moodleTex = moodleTextureSet.getTexture(moodleType);
                    if (moodleType.toString().equals(Core.getInstance().getBlinkingMoodle())) {
                        if (this.alphaIncrease) {
                            this.alpha = this.alpha + 0.1F * (30.0F / PerformanceSettings.instance.getUIRenderFPS());
                            if (this.alpha > 1.0F) {
                                this.alpha = 1.0F;
                                this.alphaIncrease = false;
                            }
                        } else {
                            this.alpha = this.alpha - 0.1F * (30.0F / PerformanceSettings.instance.getUIRenderFPS());
                            if (this.alpha < 0.0F) {
                                this.alpha = 0.0F;
                                this.alphaIncrease = true;
                            }
                        }
                    }

                    if (Core.getInstance().getBlinkingMoodle() == null) {
                        this.alpha = 1.0F;
                    }

                    int minFilter = 9985;
                    int magFilter = 9729;
                    Texture background = moodleTextureSet.getBackground();
                    Texture border = moodleTextureSet.getBorder();
                    background.getTextureId().setMinFilter(9985);
                    background.getTextureId().setMagFilter(9729);
                    border.getTextureId().setMinFilter(9985);
                    border.getTextureId().setMagFilter(9729);
                    if (this.isPercentageBackground(moodleType)) {
                        this.drawBackgroundPulse(moodleType, WiggleOffset);
                    } else {
                        moodleUIData.slotsPulse2 = 0.0F;
                        this.DrawTextureCol(background, (int)WiggleOffset, (int)moodleUIData.slotsPos, this.backgroundColour);
                    }

                    this.DrawTexture(border, (int)WiggleOffset, (int)moodleUIData.slotsPos, this.alpha);
                    float scale = this.width;
                    double offset = Math.ceil((this.width - scale) / 2.0F);
                    moodleTex.getTextureId().setMinFilter(9985);
                    moodleTex.getTextureId().setMagFilter(9729);
                    this.DrawTexture(moodleTex, (int)(WiggleOffset + offset), (int)(moodleUIData.slotsPos + offset), this.alpha);
                    if (this.mouseOver && renderedSlot == this.mouseOverSlot) {
                        String s1 = this.isoGameCharacter.getMoodles().getMoodleDisplayString(moodleType);
                        String s2 = this.isoGameCharacter.getMoodles().getMoodleDescriptionString(moodleType);
                        int width1 = TextManager.instance.font.getWidth(s1);
                        int width2 = TextManager.instance.font.getWidth(s2);
                        int width = Math.max(width1, width2);
                        int fontHgt = TextManager.instance.font.getLineHeight();
                        int y = (int)moodleUIData.slotsPos + 1;
                        int h = (2 + fontHgt) * 2;
                        if (this.width > h) {
                            y += (int)((this.width - h) / 2.0F);
                        }

                        this.DrawTextureScaledColor(null, -10.0 - width - 6.0, y - 2.0, width + 12.0, (double)h, 0.0, 0.0, 0.0, 0.6);
                        this.DrawTextRight(s1, -10.0, y, 1.0, 1.0, 1.0, 1.0);
                        this.DrawTextRight(s2, -10.0, y + fontHgt, 0.8F, 0.8F, 0.8F, 1.0);
                    }

                    renderedSlot++;
                }
            }

            super.render();
        }
    }

    public void wiggle(MoodleType moodleType) {
        this.moodleUiState.get(moodleType).oscillationLevel = 1.0F;
    }

    @Override
    public void update() {
        this.moodleDistY = 10.0F + this.width;
        super.update();
        if (this.isoGameCharacter != null) {
            if (!this.isCurrentlyAnimating()) {
                if (this.debugKeyDelay > 0) {
                    this.debugKeyDelay--;
                } else if (GameKeyboard.isKeyDown(57)) {
                    this.debugKeyDelay = 10;
                }
            }

            float fpsFraction = PerformanceSettings.getLockFPS() / 30.0F;

            for (MoodlesUI.MoodleUIData moodleUIData : this.moodleUiState.values()) {
                moodleUIData.oscillationLevel = moodleUIData.oscillationLevel - moodleUIData.oscillationLevel * 0.04000002F / fpsFraction;
                if (moodleUIData.oscillationLevel < 0.01F) {
                    moodleUIData.oscillationLevel = 0.0F;
                }
            }

            if (this.isoGameCharacter.getMoodles().UI_RefreshNeeded()) {
                int currentSlotPlace = 0;

                for (Entry<MoodleType, MoodlesUI.MoodleUIData> entry : this.moodleUiState.entrySet()) {
                    MoodleType moodleType = entry.getKey();
                    MoodlesUI.MoodleUIData moodleUIDatax = entry.getValue();
                    if (moodleType == MoodleType.FOOD_EATEN
                        && this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType) < Moodle.MoodleLevel.HighMoodleLevel.ordinal()) {
                        moodleUIDatax.slotsPos = 10000.0F;
                        moodleUIDatax.slotsDesiredPos = 10000.0F;
                        moodleUIDatax.oscillationLevel = 0.0F;
                    } else if (this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType) > 0) {
                        boolean hasChanged = false;
                        if (moodleUIDatax.level != this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType)) {
                            hasChanged = true;
                            moodleUIDatax.level = this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType);
                            moodleUIDatax.oscillationLevel = 1.0F;
                        }

                        moodleUIDatax.slotsDesiredPos = this.moodleDistY * currentSlotPlace;
                        if (hasChanged) {
                            if (moodleUIDatax.slotsPos == 10000.0F) {
                                moodleUIDatax.slotsPos = moodleUIDatax.slotsDesiredPos + 500.0F;
                                moodleUIDatax.oscillationLevel = 0.0F;
                            }

                            moodleUIDatax.goodBadNeutral = this.isoGameCharacter.getMoodles().getGoodBadNeutral(moodleType);
                        } else {
                            moodleUIDatax.oscillationLevel = 0.0F;
                        }

                        currentSlotPlace++;
                    } else {
                        moodleUIDatax.slotsPos = 10000.0F;
                        moodleUIDatax.slotsDesiredPos = 10000.0F;
                        moodleUIDatax.oscillationLevel = 0.0F;
                        moodleUIDatax.level = 0;
                    }
                }

                this.numUsedSlots = currentSlotPlace;
            }

            for (MoodlesUI.MoodleUIData moodleUIDatax : this.moodleUiState.values()) {
                if (Math.abs(moodleUIDatax.slotsPos - moodleUIDatax.slotsDesiredPos) > 0.8F) {
                    moodleUIDatax.slotsPos = moodleUIDatax.slotsPos + (moodleUIDatax.slotsDesiredPos - moodleUIDatax.slotsPos) * 0.15F;
                } else {
                    moodleUIDatax.slotsPos = moodleUIDatax.slotsDesiredPos;
                }
            }
        }
    }

    public void setCharacter(IsoGameCharacter chr) {
        if (chr != this.isoGameCharacter) {
            this.isoGameCharacter = chr;
            if (this.isoGameCharacter != null && this.isoGameCharacter.getMoodles() != null) {
                this.isoGameCharacter.getMoodles().setMoodlesStateChanged(true);
            }
        }
    }

    public static MoodlesUI getInstance() {
        return instance;
    }

    public static class MoodleUIData {
        public int goodBadNeutral;
        public int level;
        public float oscillationLevel;
        public float slotsDesiredPos = 10000.0F;
        public float slotsPos = 10000.0F;
        public float slotsPulse1;
        public float slotsPulse2;
    }
}
