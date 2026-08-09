// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.debug.DebugOptions;

@UsedFromLua
public class AtomUIText extends AtomUI {
    AngelCodeFont fontToUse;
    String text;
    double textTracking;
    double textLeading;
    int autoWidth = -1;
    float outlineThick;
    float outlineColorR;
    float outlineColorG;
    float outlineColorB;
    float outlineColorA;
    boolean shadow;
    float shadowValue;
    private int charNum;
    private int textWidth;
    private int textHeight;
    private int realTextHeight;
    private static char[] data = new char[256];
    ArrayList<AtomUIText.CharData> textData = new ArrayList<>();

    public AtomUIText(KahluaTable table) {
        super(table);
    }

    @Override
    public void render() {
        if (this.visible) {
            this.drawText();
            super.render();
        }
    }

    @Override
    public void init() {
        super.init();
        this.updateInternalValues();
    }

    void drawText() {
        DebugOptions.instance.isoSprite.forceNearestMagFilter.setValue(false);
        TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
        TextManager.sdfShader.updateShadow(this.shadowValue);
        TextManager.sdfShader.updateOutline(this.outlineThick, this.outlineColorR, this.outlineColorG, this.outlineColorB, this.outlineColorA);
        IndieGL.StartShader(TextManager.sdfShader);
        double dx = this.pivotX * this.textWidth;
        double dy = this.pivotY * this.textHeight;

        for (AtomUIText.CharData ch : this.textData) {
            double x0 = ch.x + ch.def.xoffset - dx;
            double y0 = ch.y + ch.def.yoffset - dy;
            double x1 = x0 + ch.def.width;
            double y1 = y0 + ch.def.height;
            double[] leftTop = this.getAbsolutePosition(x0, y0);
            double[] rightTop = this.getAbsolutePosition(x1, y0);
            double[] rightDown = this.getAbsolutePosition(x1, y1);
            double[] leftDown = this.getAbsolutePosition(x0, y1);
            SpriteRenderer.instance
                .render(
                    ch.def.image,
                    leftTop[0],
                    leftTop[1],
                    rightTop[0],
                    rightTop[1],
                    rightDown[0],
                    rightDown[1],
                    leftDown[0],
                    leftDown[1],
                    this.colorR,
                    this.colorG,
                    this.colorB,
                    this.colorA,
                    null
                );
        }

        IndieGL.EndShader();
    }

    float getSdfThreshold() {
        double[] p0 = this.getAbsolutePosition(-5.0, 0.0);
        double[] p1 = this.getAbsolutePosition(5.0, 0.0);
        double distance = Math.hypot(p0[0] - p1[0], p0[1] - p1[1]);
        return (float)(0.125 / (distance / 10.0));
    }

    @Override
    void loadFromTable() {
        super.loadFromTable();
        this.fontToUse = TextManager.instance.getFontFromEnum(this.tryGetFont("font", UIFont.SdfRegular));
        this.text = this.tryGetString("text", "");
        this.textTracking = this.tryGetDouble("textTracking", 0.0);
        this.textLeading = this.tryGetDouble("textLeading", 0.0);
        this.autoWidth = (int)this.tryGetDouble("autoWidth", -1.0);
        this.outlineThick = (float)this.tryGetDouble("outlineThick", 0.0);
        this.outlineColorR = (float)this.tryGetDouble("outlineColorR", 0.0);
        this.outlineColorG = (float)this.tryGetDouble("outlineColorG", 0.0);
        this.outlineColorB = (float)this.tryGetDouble("outlineColorB", 0.0);
        this.outlineColorA = (float)this.tryGetDouble("outlineColorA", 0.0);
        this.shadow = this.tryGetBoolean("shadow", false);
        this.shadowValue = this.shadow ? 1.0F : 0.0F;
    }

    void updateCharData(AngelCodeFont.CharDef def, double x, double y) {
        if (this.charNum >= this.textData.size()) {
            this.textData.add(new AtomUIText.CharData());
        }

        AtomUIText.CharData chData = this.textData.get(this.charNum);
        chData.def = def;
        chData.x = x;
        chData.y = y;
        this.charNum++;
    }

    @Override
    void updateInternalValues() {
        super.updateInternalValues();
        this.textWidth = 0;
        this.textHeight = this.fontToUse.getHeight(this.text);
        this.textData.clear();
        this.charNum = 0;
        int numChars = this.text.length();
        if (data.length < numChars) {
            data = new char[(numChars + 128 - 1) / 128 * 128];
        }

        this.text.getChars(0, numChars, data, 0);
        ArrayList<Double> addTracking = new ArrayList<>();
        int lastSpace = -1;
        int startIndex = 0;
        double diff = 0.0;
        float x = 0.0F;
        float y = 0.0F;
        float diffX = 0.0F;
        AngelCodeFont.CharDef lastCharDef = null;
        int spaceNum = 0;
        if (this.autoWidth != -1) {
            for (int i = 0; i < numChars; i++) {
                int id = data[i];
                if (id == 10) {
                    x = 0.0F;

                    for (int j = startIndex; j <= i; j++) {
                        addTracking.add(0.0);
                    }

                    startIndex = i + 1;
                    lastSpace = -1;
                    spaceNum = 0;
                    y++;
                } else {
                    if (id == 32) {
                        diff = this.autoWidth - x;
                        if (y == 0.0F && lastCharDef != null) {
                            diff -= lastCharDef.xadvance / 2.0;
                        }

                        lastSpace = i;
                        diffX = x;
                        spaceNum++;
                    }

                    if (x >= this.autoWidth && lastSpace != -1) {
                        data[lastSpace] = '\n';
                        boolean isStartLineSpace = true;

                        for (int j = startIndex; j <= lastSpace; j++) {
                            if (data[j] == ' ') {
                                if (!isStartLineSpace) {
                                    addTracking.add(diff / (spaceNum - 1));
                                } else {
                                    spaceNum--;
                                    addTracking.add(0.0);
                                }
                            } else {
                                isStartLineSpace = false;
                                addTracking.add(0.0);
                            }
                        }

                        startIndex = lastSpace + 1;
                        lastSpace = -1;
                        spaceNum = 0;
                        y++;
                        x -= diffX;
                    }

                    if (id >= this.fontToUse.chars.length) {
                        id = 63;
                    }

                    AngelCodeFont.CharDef charDef = this.fontToUse.chars[id];
                    if (charDef != null) {
                        if (lastCharDef != null) {
                            x += lastCharDef.getKerning(id);
                        }

                        lastCharDef = charDef;
                        x = (float)(x + (charDef.xadvance + this.textTracking));
                    }
                }
            }
        }

        x = 0.0F;
        y = 0.0F;
        lastCharDef = null;

        for (int ix = 0; ix < numChars; ix++) {
            int id = data[ix];
            if (id == 10) {
                this.textWidth = (int)Math.max(x, (float)this.textWidth);
                x = 0.0F;
                y = (float)(y + (this.fontToUse.getLineHeight() + this.textLeading));
            } else {
                if (id >= this.fontToUse.chars.length) {
                    id = 63;
                }

                AngelCodeFont.CharDef charDef = this.fontToUse.chars[id];
                if (charDef != null) {
                    if (lastCharDef != null) {
                        x += lastCharDef.getKerning(id);
                    }

                    lastCharDef = charDef;
                    this.updateCharData(charDef, x, y);
                    x = (float)(x + (charDef.xadvance + this.textTracking));
                    if (ix < addTracking.size()) {
                        x = (float)(x + addTracking.get(ix));
                    }
                }
            }
        }

        this.textWidth = (int)Math.max(x, (float)this.textWidth);
        this.realTextHeight = (int)Math.max(y, (float)this.textHeight);
    }

    public void setFont(UIFont font) {
        this.fontToUse = TextManager.instance.getFontFromEnum(font);
        this.updateInternalValues();
    }

    public void setText(String text) {
        this.text = text;
        this.updateInternalValues();
    }

    public void setAutoWidth(Double width) {
        this.autoWidth = width.intValue();
        this.updateInternalValues();
    }

    public Double getTextHeight() {
        return (double)this.realTextHeight;
    }

    public Double getTextWidth() {
        return (double)this.textWidth;
    }

    UIFont tryGetFont(String key, UIFont defaultValue) {
        return UIManager.tableget(this.table, key) instanceof UIFont uiFont ? uiFont : defaultValue;
    }

    static class CharData {
        public AngelCodeFont.CharDef def;
        public double x;
        public double y;
    }
}
