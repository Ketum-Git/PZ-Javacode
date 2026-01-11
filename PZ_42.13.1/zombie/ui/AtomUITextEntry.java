// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.Clipboard;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;

@UsedFromLua
public class AtomUITextEntry extends AtomUI implements UITextEntryInterface {
    boolean doingTextEntry;
    int textEntryCursorPos;
    boolean isEditable = true;
    boolean isSelectable = true;
    int toSelectionIndex;
    boolean mask;
    boolean blinkState = true;
    int blinkFramesOn = 6;
    int blinkFramesOff = 4;
    float blinkFrame = this.blinkFramesOn;
    boolean selectingRange;
    int textEntryMaxLength = 2000;
    boolean onlyNumbers;
    boolean onlyText;
    int maxTextLength = -1;
    boolean forceUpperCase;
    boolean multiline;
    AngelCodeFont.CharDef cursorDef;
    AngelCodeFont.CharDef maskDef;
    AngelCodeFont fontToUse;
    String text;
    double textTracking;
    double textLeading;
    private int charNum;
    private int textWidth;
    private int textHeight;
    private static char[] data = new char[256];
    ArrayList<AtomUITextEntry.CharData> textData = new ArrayList<>();
    Object luaOnTextChange;

    public AtomUITextEntry(KahluaTable table) {
        super(table);
    }

    @Override
    public void render() {
        if (this.visible) {
            this.drawText();
            this.drawSelection();
            if (this.doingTextEntry && this.blinkState) {
                this.drawCursor();
            }

            super.render();
        }
    }

    @Override
    public void init() {
        super.init();
        this.updateInternalValues();
        int id = 124;
        this.cursorDef = this.fontToUse.chars[id];
        int var2 = 42;
        this.maskDef = this.fontToUse.chars[var2];
    }

    @Override
    public void update() {
        if (this.maxTextLength > -1 && this.text.length() > this.maxTextLength) {
            this.text = this.text.substring(0, this.maxTextLength);
        }

        if (this.forceUpperCase) {
            this.text = this.text.toUpperCase();
        }

        super.update();
        if (this.blinkFrame > 0.0F) {
            this.blinkFrame = this.blinkFrame - GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 30.0F;
        } else {
            this.blinkState = !this.blinkState;
            if (this.blinkState) {
                this.blinkFrame = this.blinkFramesOn;
            } else {
                this.blinkFrame = this.blinkFramesOff;
            }
        }
    }

    void drawSelection() {
        if (this.toSelectionIndex != this.textEntryCursorPos) {
            int startOffset = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
            int endOffset = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
            double dx = this.pivotX * this.textWidth;
            double dy = this.pivotY * this.textHeight;
            double x0 = 9999999.0;
            double y0 = 9999999.0;

            for (int i = startOffset; i <= endOffset; i++) {
                if (i < this.textData.size()) {
                    AtomUITextEntry.CharData ch = this.textData.get(i);
                    if (x0 == 9999999.0 || i > 0 && this.textData.get(i - 1).id == 10) {
                        x0 = ch.x - dx;
                        y0 = ch.y - dy;
                    }
                }

                if (i < this.textData.size() - 2 && this.textData.get(i + 1).id == 10) {
                    AtomUITextEntry.CharData ch = this.textData.get(i);
                    double advance = ch.def == null ? 0.0 : ch.def.xadvance;
                    double x1 = ch.x - dx + advance;
                    double y1 = y0 + this.fontToUse.getLineHeight();
                    double[] leftTop = this.getAbsolutePosition(x0, y0);
                    double[] rightTop = this.getAbsolutePosition(x1, y0);
                    double[] rightDown = this.getAbsolutePosition(x1, y1);
                    double[] leftDown = this.getAbsolutePosition(x0, y1);
                    SpriteRenderer.instance
                        .render(
                            null,
                            leftTop[0],
                            leftTop[1],
                            rightTop[0],
                            rightTop[1],
                            rightDown[0],
                            rightDown[1],
                            leftDown[0],
                            leftDown[1],
                            0.39215687F,
                            0.39215687F,
                            0.8627451F,
                            0.627451F,
                            null
                        );
                } else if (i == endOffset) {
                    AtomUITextEntry.CharData ch = this.textData.get(i - 1);
                    double advance = ch.def == null ? 0.0 : ch.def.xadvance;
                    double x1 = ch.x - dx + advance;
                    double y1 = y0 + this.fontToUse.getLineHeight();
                    double[] leftTop = this.getAbsolutePosition(x0, y0);
                    double[] rightTop = this.getAbsolutePosition(x1, y0);
                    double[] rightDown = this.getAbsolutePosition(x1, y1);
                    double[] leftDown = this.getAbsolutePosition(x0, y1);
                    SpriteRenderer.instance
                        .render(
                            null,
                            leftTop[0],
                            leftTop[1],
                            rightTop[0],
                            rightTop[1],
                            rightDown[0],
                            rightDown[1],
                            leftDown[0],
                            leftDown[1],
                            0.39215687F,
                            0.39215687F,
                            0.8627451F,
                            0.627451F,
                            null
                        );
                }
            }
        }
    }

    void drawCursor() {
        DebugOptions.instance.isoSprite.forceNearestMagFilter.setValue(false);
        TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
        IndieGL.StartShader(TextManager.sdfShader);
        double dx = this.pivotX * this.textWidth;
        double dy = this.pivotY * this.textHeight;
        if (this.textData.isEmpty()) {
            double x0 = -this.cursorDef.width / 2.0 - dx;
            double y0 = this.cursorDef.yoffset - dy;
            double x1 = x0 + this.cursorDef.width;
            double y1 = y0 + this.cursorDef.height;
            double[] leftTop = this.getAbsolutePosition(x0, y0);
            double[] rightTop = this.getAbsolutePosition(x1, y0);
            double[] rightDown = this.getAbsolutePosition(x1, y1);
            double[] leftDown = this.getAbsolutePosition(x0, y1);
            SpriteRenderer.instance
                .render(
                    this.cursorDef.image,
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
        } else {
            double x0;
            double y0;
            if (this.textEntryCursorPos == 0) {
                AtomUITextEntry.CharData ch = this.textData.get(0);
                x0 = ch.x - this.cursorDef.xoffset - this.cursorDef.width / 4.0 - dx;
                y0 = ch.y + this.cursorDef.yoffset - dy;
            } else if (this.textEntryCursorPos >= this.textData.size()) {
                AtomUITextEntry.CharData ch = this.textData.get(this.textData.size() - 1);
                double width = ch.def == null ? 0.0 : ch.def.xadvance;
                x0 = ch.x - this.cursorDef.xoffset + width - this.cursorDef.width / 4.0 - dx;
                y0 = ch.y + this.cursorDef.yoffset - dy;
            } else {
                AtomUITextEntry.CharData ch = this.textData.get(this.textEntryCursorPos);
                x0 = ch.x - this.cursorDef.xoffset - this.cursorDef.width / 4.0 - dx;
                y0 = ch.y + this.cursorDef.yoffset - dy;
            }

            if (this.textEntryCursorPos >= this.textData.size() && this.text.substring(this.textEntryCursorPos - 1).equals("\n")) {
                x0 = this.cursorDef.xoffset - dx;
                y0 += this.fontToUse.getLineHeight();
            }

            double x1 = x0 + this.cursorDef.width;
            double y1 = y0 + this.cursorDef.height;
            double[] leftTop = this.getAbsolutePosition(x0, y0);
            double[] rightTop = this.getAbsolutePosition(x1, y0);
            double[] rightDown = this.getAbsolutePosition(x1, y1);
            double[] leftDown = this.getAbsolutePosition(x0, y1);
            SpriteRenderer.instance
                .render(
                    this.cursorDef.image,
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

    void drawText() {
        DebugOptions.instance.isoSprite.forceNearestMagFilter.setValue(false);
        TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
        IndieGL.StartShader(TextManager.sdfShader);
        double dx = this.pivotX * this.textWidth;
        double dy = this.pivotY * this.textHeight;

        for (AtomUITextEntry.CharData ch : this.textData) {
            if (ch.id != 10) {
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
        this.onlyNumbers = this.tryGetBoolean("onlyNumbers", false);
        this.onlyText = this.tryGetBoolean("onlyText", false);
        this.forceUpperCase = this.tryGetBoolean("forceUpperCase", false);
        this.maxTextLength = (int)this.tryGetDouble("maxTextLength", -1.0);
        this.mask = this.tryGetBoolean("isMask", false);
        this.multiline = this.tryGetBoolean("isMultiline", false);
        this.textTracking = this.tryGetDouble("textTracking", 0.0);
        this.textLeading = this.tryGetDouble("textLeading", 0.0);
        this.luaOnTextChange = this.tryGetClosure("onTextChange");
    }

    void updateCharData(AngelCodeFont.CharDef def, double x, double y, int id) {
        if (this.charNum >= this.textData.size()) {
            this.textData.add(new AtomUITextEntry.CharData());
        }

        AtomUITextEntry.CharData chData = this.textData.get(this.charNum);
        chData.def = def;
        chData.x = x;
        chData.y = y;
        chData.id = id;
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
        float x = 0.0F;
        float y = 0.0F;
        AngelCodeFont.CharDef lastCharDef = null;

        for (int i = 0; i < numChars; i++) {
            int id = data[i];
            if (id == 10) {
                AngelCodeFont.CharDef charDef = this.fontToUse.chars[id];
                this.updateCharData(charDef, x, y, id);
                this.textWidth = (int)Math.max(x, (float)this.textWidth);
                x = 0.0F;
                y = (float)(y + (this.fontToUse.getLineHeight() + this.textLeading));
            } else {
                if (this.mask) {
                    id = 42;
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
                    this.updateCharData(charDef, x, y, id);
                    x = (float)(x + (charDef.xadvance + this.textTracking));
                }
            }
        }

        this.textWidth = (int)Math.max(x, (float)this.textWidth);
    }

    public void setFont(UIFont font) {
        this.fontToUse = TextManager.instance.getFontFromEnum(font);
        this.updateInternalValues();
    }

    public void setText(String text) {
        this.text = text;
        this.updateInternalValues();
        if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
        }
    }

    UIFont tryGetFont(String key, UIFont defaultValue) {
        return UIManager.tableget(this.table, key) instanceof UIFont uiFont ? uiFont : defaultValue;
    }

    public void setMask(boolean b) {
        this.mask = b;
        this.updateInternalValues();
    }

    public boolean isMask() {
        return this.mask;
    }

    public void focus() {
        this.doingTextEntry = true;
        Core.currentTextEntryBox = this;
        this.textEntryCursorPos = this.text.length();
        this.toSelectionIndex = this.text.length();
    }

    public void unfocus() {
        this.doingTextEntry = false;
        if (Core.currentTextEntryBox == this) {
            Core.currentTextEntryBox = null;
        }
    }

    @Override
    public void onOtherKey(int eventKey) {
    }

    @Override
    public void putCharacter(char eventChar) {
        if (this.textEntryCursorPos == this.toSelectionIndex) {
            int textOffset = this.textEntryCursorPos;
            if (textOffset < this.text.length()) {
                this.text = this.text.substring(0, textOffset) + eventChar + this.text.substring(textOffset);
            } else {
                this.text = this.text + eventChar;
            }

            this.textEntryCursorPos++;
            this.toSelectionIndex++;
            if (this.luaOnTextChange != null) {
                LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
            }
        } else {
            int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
            int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
            if (!this.text.isEmpty()) {
                this.text = this.text.substring(0, l) + eventChar + this.text.substring(h);
            } else {
                this.text = eventChar + "";
            }

            this.toSelectionIndex = l + 1;
            this.textEntryCursorPos = l + 1;
            if (this.luaOnTextChange != null) {
                LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
            }
        }

        this.updateInternalValues();
    }

    int getCursorPos(double x, double y) {
        double minDist = 99999.0;
        int pos = 0;
        double[] clickPos = this.getAbsolutePosition(x, y);
        double dx = this.pivotX * this.textWidth;
        double dy = this.pivotY * this.textHeight;

        for (int i = 0; i < this.textData.size(); i++) {
            AtomUITextEntry.CharData ch = this.textData.get(i);
            if (ch.id != 10) {
                double x0 = ch.x + ch.def.xoffset - dx;
                double y0 = ch.y + this.fontToUse.getLineHeight() / 2.0 - dy;
                double[] chPos = this.getAbsolutePosition(x0, y0);
                double dist = Math.hypot(clickPos[0] - chPos[0], clickPos[1] - chPos[1]);
                if (dist < minDist) {
                    pos = i;
                    minDist = dist;
                }

                if (i == this.textData.size() - 1 || this.textData.get(i + 1).id == 10) {
                    x0 += ch.def.width;
                    chPos = this.getAbsolutePosition(x0, y0);
                    dist = Math.hypot(clickPos[0] - chPos[0], clickPos[1] - chPos[1]);
                    if (dist < minDist) {
                        pos = i + 1;
                        if (i + 1 < this.textData.size() && this.textData.get(i + 1).id == 10) {
                            pos = i + 1;
                        }

                        minDist = dist;
                    }
                }
            }
        }

        return pos;
    }

    @Override
    public boolean onConsumeMouseButtonDown(int btn, double x, double y) {
        boolean consumed = super.onConsumeMouseButtonDown(btn, x, y);
        double[] local = this.toLocalCoordinates(x, y);
        if (btn != 0) {
            return consumed;
        } else if (!this.isEditable && !this.isSelectable) {
            return consumed;
        } else {
            if (Core.currentTextEntryBox != this) {
                if (Core.currentTextEntryBox != null) {
                    Core.currentTextEntryBox.setDoingTextEntry(false);
                }

                Core.currentTextEntryBox = this;
                Core.currentTextEntryBox.setSelectingRange(true);
            }

            if (!this.doingTextEntry) {
                this.focus();
            }

            this.textEntryCursorPos = this.getCursorPos(local[0], local[1]);
            this.toSelectionIndex = this.textEntryCursorPos;
            return true;
        }
    }

    @Override
    public Boolean onConsumeMouseMove(double dx, double dy, double x, double y) {
        Boolean consumed = super.onConsumeMouseMove(dx, dy, x, y);
        double[] local = this.toLocalCoordinates(x, y);
        if ((this.isEditable || this.isSelectable) && this.selectingRange) {
            this.textEntryCursorPos = this.getCursorPos(local[0], local[1]);
            return true;
        } else {
            return consumed;
        }
    }

    @Override
    public void onExtendMouseMoveOutside(double dx, double dy, double x, double y) {
        super.onExtendMouseMoveOutside(dx, dy, x, y);
        double[] local = this.toLocalCoordinates(x, y);
        if ((this.isEditable || this.isSelectable) && this.selectingRange) {
            this.textEntryCursorPos = this.getCursorPos(local[0], local[1]);
        }
    }

    @Override
    public boolean onConsumeMouseButtonUp(int btn, double x, double y) {
        boolean consumed = super.onConsumeMouseButtonUp(btn, x, y);
        this.selectingRange = false;
        return consumed;
    }

    @Override
    public void onMouseButtonUpOutside(int btn, double x, double y) {
        super.onMouseButtonUpOutside(btn, x, y);
        this.selectingRange = false;
    }

    @Override
    public boolean isDoingTextEntry() {
        return this.doingTextEntry;
    }

    @Override
    public void setDoingTextEntry(boolean value) {
        this.doingTextEntry = value;
    }

    @Override
    public boolean isEditable() {
        return this.isEditable;
    }

    @Override
    public UINineGrid getFrame() {
        return null;
    }

    @Override
    public boolean isIgnoreFirst() {
        return false;
    }

    @Override
    public void setIgnoreFirst(boolean value) {
    }

    @Override
    public void setSelectingRange(boolean value) {
        this.selectingRange = value;
    }

    @Override
    public Color getStandardFrameColour() {
        return new Color(50, 50, 50, 212);
    }

    @Override
    public void onKeyEnter() {
        if (!this.multiline) {
            this.unfocus();
        } else {
            if (this.textEntryCursorPos != this.toSelectionIndex) {
                int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
                int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
                if (!this.text.isEmpty()) {
                    this.text = this.text.substring(0, l) + "\n" + this.text.substring(h);
                } else {
                    this.text = "\n";
                }

                this.textEntryCursorPos = l + 1;
            } else {
                int textOffset = this.textEntryCursorPos;
                this.text = this.text.substring(0, textOffset) + "\n" + this.text.substring(textOffset);
                this.textEntryCursorPos = textOffset + 1;
            }

            this.toSelectionIndex = this.textEntryCursorPos;
            this.updateInternalValues();
        }
    }

    @Override
    public void onKeyHome() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        this.textEntryCursorPos = 0;
        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }

        this.resetBlink();
    }

    @Override
    public void onKeyEnd() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        this.textEntryCursorPos = this.text.length();
        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }

        this.resetBlink();
    }

    public void resetBlink() {
        this.blinkState = true;
        this.blinkFrame = this.blinkFramesOn;
    }

    @Override
    public void onKeyUp() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        int shift = 0;
        boolean checkFirstEnter = true;

        for (int i = this.textEntryCursorPos; i > 0; i--) {
            AtomUITextEntry.CharData ch = this.textData.get(i - 1);
            if (ch.id == 10 || i == 1) {
                if (!checkFirstEnter) {
                    this.textEntryCursorPos = i - 1;

                    for (int j = 1;
                        j <= shift && i + j - 1 != this.textData.size() && (i + j - 1 >= this.textData.size() || this.textData.get(i + j - 1).id != 10);
                        j++
                    ) {
                        this.textEntryCursorPos++;
                    }
                    break;
                }

                checkFirstEnter = false;
            }

            if (checkFirstEnter) {
                shift++;
            }
        }

        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
    }

    @Override
    public void onKeyDown() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        int shift = 0;

        for (int i = this.textEntryCursorPos; i > 0; i--) {
            AtomUITextEntry.CharData ch = this.textData.get(i - 1);
            if (ch.id == 10 || i == 1) {
                break;
            }

            shift++;
        }

        DebugLog.General.warn(shift);

        for (int i = this.textEntryCursorPos; i < this.textData.size(); i++) {
            AtomUITextEntry.CharData ch = this.textData.get(i);
            if (ch.id == 10) {
                this.textEntryCursorPos = i - 1;

                for (int j = 1;
                    j <= shift && i + j - 1 != this.textData.size() && (i + j + 1 >= this.textData.size() || this.textData.get(i + j + 1).id != 10);
                    j++
                ) {
                    this.textEntryCursorPos++;
                }
                break;
            }
        }

        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
    }

    @Override
    public void onKeyLeft() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        this.textEntryCursorPos--;
        if (this.textEntryCursorPos < 0) {
            this.textEntryCursorPos = 0;
        }

        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
    }

    @Override
    public void onKeyRight() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        this.textEntryCursorPos++;
        if (this.textEntryCursorPos > this.text.length()) {
            this.textEntryCursorPos = this.text.length();
        }

        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
    }

    @Override
    public void onKeyDelete() {
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            this.onTextDelete();
        }

        if (!this.text.isEmpty() && this.textEntryCursorPos < this.text.length()) {
            if (this.textEntryCursorPos > 0) {
                this.text = this.text.substring(0, this.textEntryCursorPos) + this.text.substring(this.textEntryCursorPos + 1);
            } else {
                this.text = this.text.substring(1);
            }

            if (this.luaOnTextChange != null) {
                LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
            }

            this.updateInternalValues();
        }
    }

    void onTextDelete() {
        int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
        int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
        this.text = this.text.substring(0, l) + this.text.substring(h);
        this.toSelectionIndex = l;
        this.textEntryCursorPos = l;
        if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
        }

        this.updateInternalValues();
    }

    @Override
    public void onKeyBack() {
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            this.onTextDelete();
        }

        if (!this.text.isEmpty() && this.textEntryCursorPos > 0) {
            if (this.textEntryCursorPos > this.text.length()) {
                this.text = this.text.substring(0, this.text.length() - 1);
            } else {
                int textOffset = this.textEntryCursorPos;
                this.text = this.text.substring(0, textOffset - 1) + this.text.substring(textOffset);
            }

            this.textEntryCursorPos--;
            this.toSelectionIndex = this.textEntryCursorPos;
            if (this.luaOnTextChange != null) {
                LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
            }

            this.updateInternalValues();
        }
    }

    @Override
    public void pasteFromClipboard() {
        String clip = Clipboard.getClipboard();
        if (clip != null) {
            if (this.textEntryCursorPos != this.toSelectionIndex) {
                int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
                int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
                this.text = this.text.substring(0, l) + clip + this.text.substring(h);
                this.toSelectionIndex = l + clip.length();
                this.textEntryCursorPos = l + clip.length();
            } else {
                if (this.textEntryCursorPos < this.text.length()) {
                    this.text = this.text.substring(0, this.textEntryCursorPos) + clip + this.text.substring(this.textEntryCursorPos);
                } else {
                    this.text = this.text + clip;
                }

                this.textEntryCursorPos = this.textEntryCursorPos + clip.length();
                this.toSelectionIndex = this.toSelectionIndex + clip.length();
            }

            this.updateInternalValues();
            if (this.luaOnTextChange != null) {
                LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
            }
        }
    }

    @Override
    public void copyToClipboard() {
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
            int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
            String newClip = this.text.substring(l, h);
            if (!newClip.isEmpty()) {
                Clipboard.setClipboard(newClip);
            }

            this.updateInternalValues();
        }
    }

    @Override
    public void cutToClipboard() {
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
            int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
            String newClip = this.text.substring(l, h);
            if (!newClip.isEmpty()) {
                Clipboard.setClipboard(newClip);
            }

            this.text = this.text.substring(0, l) + this.text.substring(h);
            this.toSelectionIndex = l;
            this.textEntryCursorPos = l;
            this.updateInternalValues();
        }
    }

    @Override
    public void selectAll() {
        this.textEntryCursorPos = this.text.length();
        this.toSelectionIndex = 0;
    }

    @Override
    public boolean isTextLimit() {
        return this.text.length() >= this.textEntryMaxLength;
    }

    @Override
    public boolean isOnlyNumbers() {
        return this.onlyNumbers;
    }

    @Override
    public boolean isOnlyText() {
        return this.onlyText;
    }

    public void setOnlyNumbers(boolean onlyNumbers) {
        this.onlyNumbers = onlyNumbers;
    }

    public void setOnlyText(boolean onlyText) {
        this.onlyText = onlyText;
    }

    public int getMaxTextLength() {
        return this.maxTextLength;
    }

    public void setMaxTextLength(int maxtextLength) {
        this.maxTextLength = maxtextLength;
    }

    public boolean getForceUpperCase() {
        return this.forceUpperCase;
    }

    public void setForceUpperCase(boolean forceUpperCase) {
        this.forceUpperCase = forceUpperCase;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public void setMultiline(boolean value) {
        this.multiline = value;
    }

    public String getText() {
        return this.text;
    }

    static class CharData {
        public AngelCodeFont.CharDef def;
        public double x;
        public double y;
        public int id;
    }
}
