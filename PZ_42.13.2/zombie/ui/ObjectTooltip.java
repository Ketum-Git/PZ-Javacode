// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;

@UsedFromLua
public final class ObjectTooltip extends UIElement {
    public static float alphaStep = 0.1F;
    public boolean isItem;
    public InventoryItem item;
    public IsoObject object;
    float alpha;
    int showDelay;
    float targetAlpha;
    Texture texture;
    public int padLeft = 5;
    public int padTop = 5;
    public int padRight = 5;
    public int padBottom = 5;
    private IsoGameCharacter character;
    private boolean measureOnly;
    private float weightOfStack;
    private static int lineSpacing = 14;
    private static int staticPadLeft = -1;
    private static int staticPadRight;
    private static int staticPadTop;
    private static int staticPadBottom;
    private static String fontSize = "Small";
    private static UIFont font = UIFont.Small;
    private static final Stack<ObjectTooltip.Layout> freeLayouts = new Stack<>();

    public ObjectTooltip() {
        this.texture = Texture.getSharedTexture("black");
        this.width = 130.0F;
        this.height = 130.0F;
        this.defaultDraw = false;
        lineSpacing = TextManager.instance.getFontFromEnum(font).getLineHeight();
        checkFont();
    }

    public static void checkFont() {
        if (!fontSize.equals(Core.getInstance().getOptionTooltipFont())) {
            fontSize = Core.getInstance().getOptionTooltipFont();
            if ("Large".equals(fontSize)) {
                font = UIFont.Large;
            } else if ("Medium".equals(fontSize)) {
                font = UIFont.Medium;
            } else {
                font = UIFont.Small;
            }

            lineSpacing = TextManager.instance.getFontFromEnum(font).getLineHeight();
        }

        int charWidth = TextManager.instance.MeasureStringX(font, "0");
        staticPadRight = charWidth;
        staticPadLeft = charWidth;
        staticPadTop = staticPadBottom = charWidth / 2;
    }

    public UIFont getFont() {
        return font;
    }

    public int getLineSpacing() {
        return lineSpacing;
    }

    @Override
    public void DrawText(UIFont font, String text, double x, double y, double r, double g, double b, double alpha) {
        if (!this.measureOnly) {
            super.DrawText(font, text, x, y, r, g, b, alpha);
        }
    }

    @Override
    public void DrawTextCentre(UIFont font, String text, double x, double y, double r, double g, double b, double alpha) {
        if (!this.measureOnly) {
            super.DrawTextCentre(font, text, x, y, r, g, b, alpha);
        }
    }

    @Override
    public void DrawTextRight(UIFont font, String text, double x, double y, double r, double g, double b, double alpha) {
        if (!this.measureOnly) {
            super.DrawTextRight(font, text, x, y, r, g, b, alpha);
        }
    }

    public void DrawValueRight(int value, int x, int y, boolean highGood) {
        Integer val = value;
        String str = val.toString();
        float r = 0.3F;
        float g = 1.0F;
        float b = 0.2F;
        float a = 1.0F;
        if (value > 0) {
            str = "+" + str;
        }

        if (value < 0 && highGood || value > 0 && !highGood) {
            r = 0.8F;
            g = 0.3F;
            b = 0.2F;
        }

        this.DrawTextRight(font, str, x, y, r, g, b, 1.0);
    }

    public void DrawValueRightNoPlus(int value, int x, int y) {
        Integer val = value;
        String str = val.toString();
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = 1.0F;
        this.DrawTextRight(font, str, x, y, 1.0, 1.0, 1.0, 1.0);
    }

    public void DrawValueRightNoPlus(float value, int x, int y) {
        Float val = value;
        val = (int)((val.floatValue() + 0.01) * 10.0) / 10.0F;
        String str = val.toString();
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = 1.0F;
        this.DrawTextRight(font, str, x, y, 1.0, 1.0, 1.0, 1.0);
    }

    @Override
    public void DrawTextureScaled(Texture tex, double x, double y, double width, double height, double alpha) {
        if (!this.measureOnly) {
            super.DrawTextureScaled(tex, x, y, width, height, alpha);
        }
    }

    @Override
    public void DrawTextureScaledAspect(Texture tex, double x, double y, double width, double height, double r, double g, double b, double alpha) {
        if (!this.measureOnly) {
            super.DrawTextureScaledAspect(tex, x, y, width, height, r, g, b, alpha);
        }
    }

    public void DrawProgressBar(int x, int y, int w, int h, float f, double r, double g, double b, double a) {
        if (!this.measureOnly) {
            if (f < 0.0F) {
                f = 0.0F;
            }

            if (f > 1.0F) {
                f = 1.0F;
            }

            int done = (int)Math.floor(w * f);
            this.DrawTextureScaledColor(null, x - 1.0, y - 1.0, w + 2.0, (double)h, 0.25, 0.25, 0.25, 1.0);
            if (f != 0.0F && f != 1.0F) {
                this.DrawTextureScaledColor(null, (double)x + done, (double)y, (double)w - done, h - 2.0, 0.5, 0.5, 0.5, 1.0);
            }

            this.DrawTextureScaledColor(null, (double)x, (double)y, (double)done, h - 2.0, r, g, b, a);
        }
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
        return Boolean.FALSE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            if (!(this.alpha <= 0.0F)) {
                if (!this.isItem && this.object != null && this.object.haveSpecialTooltip()) {
                    this.object.DoSpecialTooltip(this, this.object.square);
                }

                super.render();
            }
        }
    }

    public void show(IsoObject obj, double x, double y) {
        this.isItem = false;
        this.object = obj;
        this.setX(x);
        this.setY(y);
        this.targetAlpha = 0.5F;
        this.showDelay = 15;
        this.alpha = 0.0F;
    }

    public void hide() {
        this.object = null;
        this.showDelay = 0;
        this.setVisible(false);
    }

    @Override
    public void update() {
        if (!(this.alpha <= 0.0F) || this.targetAlpha != 0.0F) {
            if (this.showDelay > 0) {
                if (--this.showDelay == 0) {
                    this.setVisible(true);
                }
            } else {
                if (this.alpha < this.targetAlpha) {
                    this.alpha = this.alpha + alphaStep;
                    if (this.alpha > 0.5F) {
                        this.alpha = 0.5F;
                    }
                } else if (this.alpha > this.targetAlpha) {
                    this.alpha = this.alpha - alphaStep;
                    if (this.alpha < this.targetAlpha) {
                        this.alpha = this.targetAlpha;
                    }
                }
            }
        }
    }

    void show(InventoryItem info, int i, int i0) {
        this.object = null;
        this.item = info;
        this.isItem = true;
        this.setX(this.getX());
        this.setY(this.getY());
        this.targetAlpha = 0.5F;
        this.showDelay = 15;
        this.alpha = 0.0F;
        this.setVisible(true);
    }

    public void adjustWidth(int textX, String text) {
        int textWidth = TextManager.instance.MeasureStringX(font, text);
        if (textX + textWidth + this.padRight > this.width) {
            this.setWidth(textX + textWidth + this.padRight);
        }
    }

    public ObjectTooltip.Layout beginLayout() {
        this.padLeft = staticPadLeft;
        this.padRight = staticPadRight;
        this.padTop = staticPadTop;
        this.padBottom = staticPadBottom;
        ObjectTooltip.Layout layout = null;
        if (freeLayouts.isEmpty()) {
            layout = new ObjectTooltip.Layout();
        } else {
            layout = freeLayouts.pop();
        }

        return layout;
    }

    public void endLayout(ObjectTooltip.Layout layout) {
        while (layout != null) {
            ObjectTooltip.Layout next = layout.next;
            layout.free();
            freeLayouts.push(layout);
            layout = next;
        }
    }

    public Texture getTexture() {
        return this.texture;
    }

    public void setCharacter(IsoGameCharacter chr) {
        this.character = chr;
    }

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public void setMeasureOnly(boolean b) {
        this.measureOnly = b;
    }

    public boolean isMeasureOnly() {
        return this.measureOnly;
    }

    public float getWeightOfStack() {
        return this.weightOfStack;
    }

    public void setWeightOfStack(float weight) {
        this.weightOfStack = weight;
    }

    @UsedFromLua
    public static class Layout {
        public ArrayList<ObjectTooltip.LayoutItem> items = new ArrayList<>();
        public int minLabelWidth;
        public int minValueWidth;
        public ObjectTooltip.Layout next;
        public int nextPadY;
        public int offsetY;
        private static final Stack<ObjectTooltip.LayoutItem> freeItems = new Stack<>();

        public ObjectTooltip.LayoutItem addItem() {
            ObjectTooltip.LayoutItem item = null;
            if (freeItems.isEmpty()) {
                item = new ObjectTooltip.LayoutItem();
            } else {
                item = freeItems.pop();
            }

            item.reset();
            this.items.add(item);
            return item;
        }

        public void setMinLabelWidth(int minWidth) {
            this.minLabelWidth = minWidth;
        }

        public void setMinValueWidth(int minWidth) {
            this.minValueWidth = minWidth;
        }

        public int render(int left, int top, ObjectTooltip ui) {
            int widthLabel = this.minLabelWidth;
            int widthValue = this.minValueWidth;
            int widthValueRight = this.minValueWidth;
            int widthProgress = 0;
            int widthTotal = 0;
            int padX = Math.max(TextManager.instance.MeasureStringX(ObjectTooltip.font, "W"), 8);
            int mid = 0;

            for (int i = 0; i < this.items.size(); i++) {
                ObjectTooltip.LayoutItem item = this.items.get(i);
                item.calcSizes();
                if (item.hasValue) {
                    widthLabel = Math.max(widthLabel, item.labelWidth);
                    widthValue = Math.max(widthValue, item.valueWidth);
                    widthValueRight = Math.max(widthValueRight, item.valueWidthRight);
                    widthProgress = Math.max(widthProgress, item.progressWidth);
                    mid = Math.max(mid, Math.max(item.labelWidth, this.minLabelWidth) + padX);
                    widthTotal = Math.max(widthTotal, widthLabel + padX + Math.max(Math.max(widthValue, widthValueRight), widthProgress));
                } else {
                    if (item.couldHaveValue) {
                        widthLabel = Math.max(widthLabel, item.labelWidth);
                    }

                    widthTotal = Math.max(widthTotal, item.labelWidth);
                }
            }

            if (left + widthTotal + ui.padRight > ui.width) {
                ui.setWidth(left + widthTotal + ui.padRight);
            }

            for (int ix = 0; ix < this.items.size(); ix++) {
                ObjectTooltip.LayoutItem item = this.items.get(ix);
                item.render(left, top, mid, widthValueRight, ui);
                top += item.height;
            }

            return this.next != null ? this.next.render(left, top + this.next.nextPadY, ui) : top;
        }

        public void free() {
            freeItems.addAll(this.items);
            this.items.clear();
            this.minLabelWidth = 0;
            this.minValueWidth = 0;
            this.next = null;
            this.nextPadY = 0;
            this.offsetY = 0;
        }
    }

    @UsedFromLua
    public static class LayoutItem {
        public String label;
        public float r0;
        public float g0;
        public float b0;
        public float a0;
        public boolean hasValue;
        public boolean couldHaveValue;
        public String value;
        public boolean rightJustify;
        public float r1;
        public float g1;
        public float b1;
        public float a1;
        public float progressFraction = -1.0F;
        public int labelWidth;
        public int valueWidth;
        public int valueWidthRight;
        public int progressWidth;
        public int height;

        public void reset() {
            this.label = null;
            this.value = null;
            this.hasValue = false;
            this.couldHaveValue = false;
            this.rightJustify = false;
            this.progressFraction = -1.0F;
        }

        public void setLabel(String label, float r, float g, float b, float a) {
            this.label = label;
            this.r0 = r;
            this.b0 = b;
            this.g0 = g;
            this.a0 = a;
        }

        public void setValue(String label, float r, float g, float b, float a) {
            this.value = label;
            this.r1 = r;
            this.b1 = b;
            this.g1 = g;
            this.a1 = a;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setValueRight(int value, boolean highGood) {
            this.value = Integer.toString(value);
            if (value > 0) {
                this.value = "+" + this.value;
            }

            if ((value >= 0 || !highGood) && (value <= 0 || highGood)) {
                this.r1 = Core.getInstance().getGoodHighlitedColor().getR();
                this.g1 = Core.getInstance().getGoodHighlitedColor().getG();
                this.b1 = Core.getInstance().getGoodHighlitedColor().getB();
            } else {
                this.r1 = Core.getInstance().getBadHighlitedColor().getR();
                this.g1 = Core.getInstance().getBadHighlitedColor().getG();
                this.b1 = Core.getInstance().getBadHighlitedColor().getB();
            }

            this.a1 = 1.0F;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setValueRightNoPlus(float value) {
            value = (int)((value + 0.005F) * 100.0F) / 100.0F;
            this.value = Float.toString(value);
            this.r1 = 1.0F;
            this.g1 = 1.0F;
            this.b1 = 1.0F;
            this.a1 = 1.0F;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setValueRightNoPlus(int value) {
            this.value = Integer.toString(value);
            this.r1 = 1.0F;
            this.g1 = 1.0F;
            this.b1 = 1.0F;
            this.a1 = 1.0F;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setProgress(float fraction, float r, float g, float b, float a) {
            this.progressFraction = fraction;
            this.r1 = r;
            this.b1 = b;
            this.g1 = g;
            this.a1 = a;
            this.hasValue = true;
        }

        public void calcSizes() {
            this.labelWidth = this.valueWidth = this.valueWidthRight = this.progressWidth = 0;
            if (this.label != null) {
                this.labelWidth = TextManager.instance.MeasureStringX(ObjectTooltip.font, this.label);
            }

            if (this.hasValue) {
                if (this.value != null) {
                    int textWidth = TextManager.instance.MeasureStringX(ObjectTooltip.font, this.value);
                    this.valueWidth = this.rightJustify ? 0 : textWidth;
                    this.valueWidthRight = this.rightJustify ? textWidth : 0;
                } else if (this.progressFraction != -1.0F) {
                    this.progressWidth = 80;
                }
            }

            int lines = 1;
            if (this.label != null) {
                int lines1 = 1;

                for (int i = 0; i < this.label.length(); i++) {
                    if (this.label.charAt(i) == '\n') {
                        lines1++;
                    }
                }

                lines = Math.max(lines, lines1);
            }

            if (this.hasValue && this.value != null) {
                int lines1 = 1;

                for (int ix = 0; ix < this.value.length(); ix++) {
                    if (this.value.charAt(ix) == '\n') {
                        lines1++;
                    }
                }

                lines = Math.max(lines, lines1);
            }

            this.height = lines * ObjectTooltip.lineSpacing;
        }

        public void render(int x, int y, int mid, int right, ObjectTooltip ui) {
            if (this.label != null) {
                ui.DrawText(ObjectTooltip.font, this.label, x, y, this.r0, this.g0, this.b0, this.a0);
            }

            if (this.value != null) {
                if (this.rightJustify) {
                    ui.DrawTextRight(ObjectTooltip.font, this.value, x + mid + right, y, this.r1, this.g1, this.b1, this.a1);
                } else {
                    ui.DrawText(ObjectTooltip.font, this.value, x + mid, y, this.r1, this.g1, this.b1, this.a1);
                }
            }

            if (this.progressFraction != -1.0F) {
                int h = 5;
                if ("Medium".equals(ObjectTooltip.fontSize)) {
                    h = 6;
                }

                if ("Large".equals(ObjectTooltip.fontSize)) {
                    h = 7;
                }

                ui.DrawProgressBar(x + mid, y + ObjectTooltip.lineSpacing / 2 - 1, right, h, this.progressFraction, this.r1, this.g1, this.b1, this.a1);
            }
        }
    }
}
