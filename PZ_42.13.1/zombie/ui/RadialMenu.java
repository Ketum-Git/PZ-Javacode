// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.textures.Texture;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.util.StringUtils;

@UsedFromLua
public final class RadialMenu extends UIElement {
    protected int outerRadius = 200;
    protected int innerRadius = 100;
    protected ArrayList<RadialMenu.Slice> slices = new ArrayList<>();
    protected int highlight = -1;
    protected int joypad = -1;
    protected UITransition transition = new UITransition();
    protected UITransition select = new UITransition();
    protected UITransition deselect = new UITransition();
    protected int selectIndex = -1;
    protected int deselectIndex = -1;

    public RadialMenu(int x, int y, int innerRadius, int outerRadius) {
        this.setX(x);
        this.setY(y);
        this.setWidth(outerRadius * 2);
        this.setHeight(outerRadius * 2);
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    @Override
    public void update() {
        if (this.joypad != -1 && !JoypadManager.instance.isJoypadConnected(this.joypad)) {
            this.joypad = -1;
        }
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            if (this.joypad != -1 && !JoypadManager.instance.isJoypadConnected(this.joypad)) {
                this.joypad = -1;
            }

            this.transition.setIgnoreUpdateTime(true);
            this.transition.setFadeIn(true);
            this.transition.update();
            if (!this.slices.isEmpty()) {
                float scale = this.transition.fraction();
                float innerRadius = this.innerRadius * 0.85F + this.innerRadius * scale * 0.15F;
                float outerRadius = this.outerRadius * 0.85F + this.outerRadius * scale * 0.15F;

                for (int i = 0; i < 48; i++) {
                    float degreesPerSlice = 7.5F;
                    double theta2 = Math.toRadians(i * 7.5F);
                    double theta3 = Math.toRadians((i + 1) * 7.5F);
                    double x0 = this.x + this.width / 2.0F;
                    double y0 = this.y + this.height / 2.0F;
                    double x1 = this.x + this.width / 2.0F;
                    double y1 = this.y + this.height / 2.0F;
                    double x2 = this.x + this.width / 2.0F + outerRadius * (float)Math.cos(theta2);
                    double y2 = this.y + this.height / 2.0F + outerRadius * (float)Math.sin(theta2);
                    double x3 = this.x + this.width / 2.0F + outerRadius * (float)Math.cos(theta3);
                    double y3 = this.y + this.height / 2.0F + outerRadius * (float)Math.sin(theta3);
                    if (i == 47) {
                        y3 = y1;
                    }

                    float r = 0.1F;
                    float g = 0.1F;
                    float b = 0.1F;
                    float a = 0.45F + 0.45F * scale;
                    SpriteRenderer.instance
                        .renderPoly((float)x0, (float)y0, (float)x2, (float)y2, (float)x3, (float)y3, (float)x1, (float)y1, 0.1F, 0.1F, 0.1F, a);
                }

                float degreesPerSlice = 360.0F / Math.max(this.slices.size(), 2);
                float pad = this.slices.size() == 1 ? 0.0F : 1.5F;
                int highlight = this.highlight;
                if (highlight == -1) {
                    if (this.joypad != -1) {
                        highlight = this.getSliceIndexFromJoypad(this.joypad);
                    } else {
                        highlight = this.getSliceIndexFromMouse(Mouse.getXA() - this.getAbsoluteX().intValue(), Mouse.getYA() - this.getAbsoluteY().intValue());
                    }
                }

                RadialMenu.Slice slice = this.getSlice(highlight);
                if (slice != null && slice.isEmpty()) {
                    highlight = -1;
                }

                if (highlight != this.selectIndex) {
                    this.select.reset();
                    this.select.setIgnoreUpdateTime(true);
                    if (this.selectIndex != -1) {
                        this.deselectIndex = this.selectIndex;
                        this.deselect.reset();
                        this.deselect.setFadeIn(false);
                        this.deselect.init(66.666664F, true);
                    }

                    this.selectIndex = highlight;
                }

                this.select.update();
                this.deselect.update();
                float startAngle = this.getStartAngle() - 180.0F;

                for (int i = 0; i < this.slices.size(); i++) {
                    int subSlice = Math.max(6, 48 / Math.max(this.slices.size(), 2));

                    for (int j = 0; j < subSlice; j++) {
                        double theta0 = Math.toRadians(startAngle + i * degreesPerSlice + j * degreesPerSlice / subSlice + (j == 0 ? pad : 0.0F));
                        double theta1 = Math.toRadians(
                            startAngle + i * degreesPerSlice + (j + 1) * degreesPerSlice / subSlice - (j == subSlice - 1 ? pad : 0.0F)
                        );
                        double theta2 = Math.toRadians(startAngle + i * degreesPerSlice + j * degreesPerSlice / subSlice + (j == 0 ? pad / 2.0F : 0.0F));
                        double theta3 = Math.toRadians(
                            startAngle + i * degreesPerSlice + (j + 1) * degreesPerSlice / subSlice - (j == subSlice - 1 ? pad / 1.5 : 0.0)
                        );
                        double x0 = this.x + this.width / 2.0F + innerRadius * (float)Math.cos(theta0);
                        double y0 = this.y + this.height / 2.0F + innerRadius * (float)Math.sin(theta0);
                        double x1 = this.x + this.width / 2.0F + innerRadius * (float)Math.cos(theta1);
                        double y1 = this.y + this.height / 2.0F + innerRadius * (float)Math.sin(theta1);
                        double x2 = this.x + this.width / 2.0F + outerRadius * (float)Math.cos(theta2);
                        double y2 = this.y + this.height / 2.0F + outerRadius * (float)Math.sin(theta2);
                        double x3 = this.x + this.width / 2.0F + outerRadius * (float)Math.cos(theta3);
                        double y3 = this.y + this.height / 2.0F + outerRadius * (float)Math.sin(theta3);
                        float r = 1.0F;
                        float g = 1.0F;
                        float b = 1.0F;
                        float a = 0.025F;
                        if (i == highlight) {
                            a = 0.25F + 0.25F * this.select.fraction();
                        } else if (i == this.deselectIndex) {
                            a = 0.025F + 0.475F * this.deselect.fraction();
                        }

                        SpriteRenderer.instance
                            .renderPoly((float)x0, (float)y0, (float)x2, (float)y2, (float)x3, (float)y3, (float)x1, (float)y1, 1.0F, 1.0F, 1.0F, a);
                    }

                    Texture texture = this.slices.get(i).texture;
                    if (texture != null) {
                        double theta = Math.toRadians(startAngle + i * degreesPerSlice + degreesPerSlice / 2.0F);
                        float cx = 0.0F + this.width / 2.0F + (innerRadius + (outerRadius - innerRadius) / 2.0F) * (float)Math.cos(theta);
                        float cy = 0.0F + this.height / 2.0F + (innerRadius + (outerRadius - innerRadius) / 2.0F) * (float)Math.sin(theta);
                        if (texture.getWidth() > 64) {
                            this.DrawTextureScaledAspect(texture, cx - 32.0 - texture.offsetX, cy - 32.0 - texture.offsetY, 64.0, 64.0, 1.0, 1.0, 1.0, scale);
                        } else {
                            this.DrawTexture(texture, cx - texture.getWidth() / 2 - texture.offsetX, cy - texture.getHeight() / 2 - texture.offsetY, scale);
                        }
                    }
                }

                if (slice != null && !StringUtils.isNullOrWhitespace(slice.text)) {
                    this.formatTextInsideCircle(slice.text);
                }
            }
        }
    }

    private void formatTextInsideCircle(String text) {
        UIFont font = UIFont.Medium;
        AngelCodeFont fontObj = TextManager.instance.getFontFromEnum(font);
        int fontHgt = fontObj.getLineHeight();
        int nLines = 1;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                nLines++;
            }
        }

        if (nLines > 1) {
            int textHgt = nLines * fontHgt;
            int x = this.getAbsoluteX().intValue() + (int)this.width / 2;
            int y = this.getAbsoluteY().intValue() + (int)this.height / 2 - textHgt / 2;
            int offset = 0;

            for (int ix = 0; ix < text.length(); ix++) {
                if (text.charAt(ix) == '\n') {
                    this.drawTextWithBackground(fontObj, text, x, y, 1.0F, 1.0F, 1.0F, 1.0F, offset, ix - 1);
                    offset = ix + 1;
                    y += fontHgt;
                }
            }

            if (offset < text.length()) {
                this.drawTextWithBackground(fontObj, text, x, y, 1.0F, 1.0F, 1.0F, 1.0F, offset, text.length() - 1);
            }
        } else {
            int x = this.getAbsoluteX().intValue() + (int)this.width / 2;
            int y = this.getAbsoluteY().intValue() + (int)this.height / 2 - fontHgt / 2;
            this.drawTextWithBackground(fontObj, text, x, y, 1.0F, 1.0F, 1.0F, 1.0F, 0, text.length() - 1);
        }
    }

    private void drawTextWithBackground(AngelCodeFont fontObj, String text, float x, float y, float r, float g, float b, float a, int startIndex, int endIndex) {
        float textWid = fontObj.getWidth(text, startIndex, endIndex);
        int fontHgt = fontObj.getLineHeight();
        int padX = 2;
        float scale = this.transition.fraction();
        float r1 = 0.1F;
        float g1 = 0.1F;
        float b1 = 0.1F;
        float a1 = 0.45F + 0.45F * scale;
        SpriteRenderer.instance.renderi(null, (int)(x - textWid / 2.0F) - 2, (int)y, (int)textWid + 4, fontHgt, 0.1F, 0.1F, 0.1F, a1, null);
        a = a / 2.0F + a / 2.0F * scale;
        fontObj.drawString(x - textWid / 2.0F, y, text, r, g, b, a, startIndex, endIndex);
    }

    public void clear() {
        this.slices.clear();
        this.transition.reset();
        this.transition.init(66.666664F, false);
        this.selectIndex = -1;
        this.deselectIndex = -1;
    }

    public void addSlice(String text, Texture texture) {
        RadialMenu.Slice slice = new RadialMenu.Slice();
        slice.text = text;
        slice.texture = texture;
        this.slices.add(slice);
    }

    private RadialMenu.Slice getSlice(int sliceIndex) {
        return sliceIndex >= 0 && sliceIndex < this.slices.size() ? this.slices.get(sliceIndex) : null;
    }

    public void setSliceText(int sliceIndex, String text) {
        RadialMenu.Slice slice = this.getSlice(sliceIndex);
        if (slice != null) {
            slice.text = text;
        }
    }

    public void setSliceTexture(int sliceIndex, Texture texture) {
        RadialMenu.Slice slice = this.getSlice(sliceIndex);
        if (slice != null) {
            slice.texture = texture;
        }
    }

    private float getStartAngle() {
        float degreesPerSlice = 360.0F / Math.max(this.slices.size(), 2);
        return 90.0F - degreesPerSlice / 2.0F;
    }

    public int getSliceIndexFromMouse(int mx, int my) {
        float centerX = 0.0F + this.width / 2.0F;
        float centerY = 0.0F + this.height / 2.0F;
        double dist = Math.sqrt(Math.pow(mx - centerX, 2.0) + Math.pow(my - centerY, 2.0));
        if (!(dist > this.outerRadius) && !(dist < this.innerRadius)) {
            double radians = Math.atan2(my - centerY, mx - centerX) + Math.PI;
            double degrees = Math.toDegrees(radians);
            float degreesPerSlice = 360.0F / Math.max(this.slices.size(), 2);
            return degrees < this.getStartAngle()
                ? (int)((degrees + 360.0 - this.getStartAngle()) / degreesPerSlice)
                : (int)((degrees - this.getStartAngle()) / degreesPerSlice);
        } else {
            return -1;
        }
    }

    public int getSliceIndexFromJoypad(int joypad) {
        float xAxis = JoypadManager.instance.getAimingAxisX(joypad);
        float yAxis = JoypadManager.instance.getAimingAxisY(joypad);
        if (!(Math.abs(xAxis) > 0.3F) && !(Math.abs(yAxis) > 0.3F)) {
            return -1;
        } else {
            double radians = Math.atan2(-yAxis, -xAxis);
            double degrees = Math.toDegrees(radians);
            float degreesPerSlice = 360.0F / Math.max(this.slices.size(), 2);
            return degrees < this.getStartAngle()
                ? (int)((degrees + 360.0 - this.getStartAngle()) / degreesPerSlice)
                : (int)((degrees - this.getStartAngle()) / degreesPerSlice);
        }
    }

    public void setJoypad(int joypad) {
        this.joypad = joypad;
    }

    protected static class Slice {
        public String text;
        public Texture texture;

        boolean isEmpty() {
            return this.text == null && this.texture == null;
        }
    }
}
