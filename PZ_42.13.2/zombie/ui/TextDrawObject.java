// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.network.GameServer;

@UsedFromLua
public final class TextDrawObject {
    private String[] validImages = new String[]{"Icon_music_notes", "media/ui/CarKey.png", "media/ui/ArrowUp.png", "media/ui/ArrowDown.png"};
    private String[] validFonts = new String[]{"Small", "Dialogue", "Medium", "Code", "Large", "Massive"};
    private final ArrayList<TextDrawObject.DrawLine> lines = new ArrayList<>();
    private int width;
    private int height;
    private int maxCharsLine = -1;
    private UIFont defaultFontEnum = UIFont.Dialogue;
    private AngelCodeFont defaultFont;
    private String original = "";
    private String unformatted = "";
    private TextDrawObject.DrawLine currentLine;
    private TextDrawObject.DrawElement currentElement;
    private boolean hasOpened;
    private boolean drawBackground;
    private boolean allowImages = true;
    private boolean allowChatIcons = true;
    private boolean allowColors = true;
    private boolean allowFonts = true;
    private boolean allowBbcode = true;
    private boolean allowAnyImage;
    private boolean allowLineBreaks = true;
    private boolean equalizeLineHeights;
    private boolean enabled = true;
    private int visibleRadius = -1;
    private float scrambleVal;
    private float outlineR;
    private float outlineG;
    private float outlineB;
    private float outlineA = 1.0F;
    private float defaultR = 1.0F;
    private float defaultG = 1.0F;
    private float defaultB = 1.0F;
    private float defaultA = 1.0F;
    private int hearRange = -1;
    private float internalClock;
    private String customTag = "default";
    private int customImageMaxDim = 18;
    private TextDrawHorizontal defaultHorz = TextDrawHorizontal.Center;
    private final int drawMode = 0;
    private static final ArrayList<TextDrawObject.RenderBatch> renderBatch = new ArrayList<>();
    private static final ArrayDeque<TextDrawObject.RenderBatch> renderBatchPool = new ArrayDeque<>();
    private String elemText;

    public TextDrawObject() {
        this(255, 255, 255, true, true, true, true, true, false);
    }

    public TextDrawObject(int r, int g, int b, boolean allowBbcode) {
        this(r, g, b, allowBbcode, true, true, true, true, false);
    }

    public TextDrawObject(
        int r,
        int g,
        int b,
        boolean allowBbcode,
        boolean allowImages,
        boolean allowChatIcons,
        boolean allowColors,
        boolean allowFonts,
        boolean equalizeLineHeights
    ) {
        this.setSettings(allowBbcode, allowImages, allowChatIcons, allowColors, allowFonts, equalizeLineHeights);
        this.setDefaultColors(r, g, b);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setVisibleRadius(int radius) {
        this.visibleRadius = radius;
    }

    public int getVisibleRadius() {
        return this.visibleRadius;
    }

    public void setDrawBackground(boolean draw) {
        this.drawBackground = draw;
    }

    public void setAllowImages(boolean allowImages) {
        this.allowImages = allowImages;
    }

    public void setAllowChatIcons(boolean allowChatIcons) {
        this.allowChatIcons = allowChatIcons;
    }

    public void setAllowColors(boolean allowColors) {
        this.allowColors = allowColors;
    }

    public void setAllowFonts(boolean allowFonts) {
        this.allowFonts = allowFonts;
    }

    public void setAllowBBcode(boolean allowBbcode) {
        this.allowBbcode = allowBbcode;
    }

    public void setAllowAnyImage(boolean allowAnyImage) {
        this.allowAnyImage = allowAnyImage;
    }

    public void setAllowLineBreaks(boolean allowLineBreaks) {
        this.allowLineBreaks = allowLineBreaks;
    }

    public void setEqualizeLineHeights(boolean equalizeLineHeights) {
        this.equalizeLineHeights = equalizeLineHeights;
        this.calculateDimensions();
    }

    public void setSettings(
        boolean allowBBcode, boolean allowImages, boolean allowChatIcons, boolean allowColors, boolean allowFonts, boolean equalizeLineHeights
    ) {
        this.allowImages = allowImages;
        this.allowChatIcons = allowChatIcons;
        this.allowColors = allowColors;
        this.allowFonts = allowFonts;
        this.allowBbcode = allowBBcode;
        this.equalizeLineHeights = equalizeLineHeights;
    }

    public void setCustomTag(String tag) {
        this.customTag = tag;
    }

    public String getCustomTag() {
        return this.customTag;
    }

    public void setValidImages(String[] list) {
        this.validImages = list;
    }

    public void setValidFonts(String[] list) {
        this.validFonts = list;
    }

    public void setMaxCharsPerLine(int charsperline) {
        if (charsperline > 0) {
            this.ReadString(this.original, charsperline);
        }
    }

    public void setCustomImageMaxDimensions(int dim) {
        if (dim >= 1) {
            this.customImageMaxDim = dim;
            this.calculateDimensions();
        }
    }

    public void setOutlineColors(int r, int g, int b) {
        this.setOutlineColors(r / 255.0F, g / 255.0F, b / 255.0F, 1.0F);
    }

    public void setOutlineColors(int r, int g, int b, int a) {
        this.setOutlineColors(r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
    }

    public void setOutlineColors(float r, float g, float b) {
        this.setOutlineColors(r, g, b, 1.0F);
    }

    public void setOutlineColors(float r, float g, float b, float a) {
        this.outlineR = r;
        this.outlineG = g;
        this.outlineB = b;
        this.outlineA = a;
    }

    public void setDefaultColors(int r, int g, int b) {
        this.setDefaultColors(r / 255.0F, g / 255.0F, b / 255.0F, 1.0F);
    }

    public void setDefaultColors(int r, int g, int b, int a) {
        this.setDefaultColors(r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
    }

    public void setDefaultColors(float r, float g, float b) {
        this.setDefaultColors(r, g, b, 1.0F);
    }

    public void setDefaultColors(float r, float g, float b, float a) {
        this.defaultR = r;
        this.defaultG = g;
        this.defaultB = b;
        this.defaultA = a;
    }

    public void setHorizontalAlign(String horz) {
        if (horz.equals("left")) {
            this.defaultHorz = TextDrawHorizontal.Left;
        } else if (horz.equals("center")) {
            this.defaultHorz = TextDrawHorizontal.Center;
        }

        if (horz.equals("right")) {
            this.defaultHorz = TextDrawHorizontal.Right;
        }
    }

    public void setHorizontalAlign(TextDrawHorizontal horz) {
        this.defaultHorz = horz;
    }

    public TextDrawHorizontal getHorizontalAlign() {
        return this.defaultHorz;
    }

    public String getOriginal() {
        return this.original;
    }

    public String getUnformatted() {
        if (!(this.scrambleVal > 0.0F)) {
            return this.unformatted;
        } else {
            String str = "";

            for (TextDrawObject.DrawLine line : this.lines) {
                for (TextDrawObject.DrawElement elem : line.elements) {
                    if (!elem.isImage) {
                        str = str + elem.scrambleText;
                    }
                }
            }

            return str;
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public UIFont getDefaultFontEnum() {
        return this.defaultFontEnum;
    }

    public boolean isNullOrZeroLength() {
        return this.original == null || this.original.isEmpty();
    }

    public float getInternalClock() {
        return this.internalClock;
    }

    public void setInternalTickClock(float ticks) {
        if (ticks > 0.0F) {
            this.internalClock = ticks;
        }
    }

    public float updateInternalTickClock() {
        return this.updateInternalTickClock(1.25F * GameTime.getInstance().getMultiplier());
    }

    public float updateInternalTickClock(float delta) {
        if (this.internalClock <= 0.0F) {
            return 0.0F;
        } else {
            this.internalClock -= delta;
            if (this.internalClock <= 0.0F) {
                this.internalClock = 0.0F;
            }

            return this.internalClock;
        }
    }

    public void setScrambleVal(float value) {
        if (this.scrambleVal != value) {
            this.scrambleVal = value;
            if (this.scrambleVal > 0.0F) {
                for (TextDrawObject.DrawLine line : this.lines) {
                    for (TextDrawObject.DrawElement elem : line.elements) {
                        if (!elem.isImage) {
                            elem.scrambleText(this.scrambleVal);
                        }
                    }
                }
            }
        }
    }

    public float getScrambleVal() {
        return this.scrambleVal;
    }

    public void setHearRange(int range) {
        if (range < 0) {
            this.hearRange = 0;
        } else {
            this.hearRange = range;
        }
    }

    public int getHearRange() {
        return this.hearRange;
    }

    private boolean isValidFont(String fnt) {
        for (String s : this.validFonts) {
            if (fnt.equals(s) && UIFont.FromString(fnt) != null) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidImage(String img) {
        for (String s : this.validImages) {
            if (img.equals(s)) {
                return true;
            }
        }

        return false;
    }

    private int tryColorInt(String str) {
        if (str.length() > 0 && str.length() <= 3) {
            try {
                int i = Integer.parseInt(str);
                return i >= 0 && i < 256 ? i : -1;
            } catch (NumberFormatException var3) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private String readTagValue(char[] chars, int pos) {
        if (chars[pos] == '=') {
            String s = "";

            for (int i = pos + 1; i < chars.length; i++) {
                char c = chars[i];
                if (c == ']') {
                    return s;
                }

                s = s + c;
            }
        }

        return null;
    }

    public void Clear() {
        this.original = "";
        this.unformatted = "";
        this.reset();
    }

    private void reset() {
        this.lines.clear();
        this.currentLine = new TextDrawObject.DrawLine();
        this.lines.add(this.currentLine);
        this.currentElement = new TextDrawObject.DrawElement();
        this.currentLine.addElement(this.currentElement);
        this.enabled = true;
        this.scrambleVal = 0.0F;
    }

    private void addNewLine() {
        this.currentLine = new TextDrawObject.DrawLine();
        this.lines.add(this.currentLine);
        this.currentElement = this.currentElement.softclone();
        this.currentLine.addElement(this.currentElement);
    }

    private void addText(String word) {
        this.currentElement.addText(word);
        this.currentLine.charW = this.currentLine.charW + word.length();
    }

    private void addWord(String word) {
        if (this.maxCharsLine > 0 && this.currentLine.charW + word.length() >= this.maxCharsLine) {
            for (int i = 0; i < word.length() / this.maxCharsLine + 1; i++) {
                int s = i * this.maxCharsLine;
                int e = s + this.maxCharsLine < word.length() ? s + this.maxCharsLine : word.length();
                if (!word.substring(s, e).isEmpty()) {
                    if (i > 0 || this.currentLine.charW != 0) {
                        this.addNewLine();
                    }

                    this.addText(word.substring(s, e));
                }
            }
        } else {
            this.addText(word);
        }
    }

    private void addNewElement() {
        if (this.currentElement.text.isEmpty()) {
            this.currentElement.reset();
        } else {
            this.currentElement = new TextDrawObject.DrawElement();
            this.currentLine.addElement(this.currentElement);
        }
    }

    private int readTag(char[] chars, int pos, String tag) {
        if (this.allowFonts && tag.equals("fnt")) {
            String val = this.readTagValue(chars, pos);
            if (val != null && this.isValidFont(val)) {
                this.addNewElement();
                this.currentElement.f = UIFont.FromString(val);
                this.currentElement.useFont = true;
                this.currentElement.font = TextManager.instance.getFontFromEnum(this.currentElement.f);
                this.hasOpened = true;
                return pos + val.length() + 1;
            }
        } else if ((this.allowImages || this.allowChatIcons) && tag.equals("img")) {
            String img = this.readTagValue(chars, pos);
            if (img != null && !img.trim().isEmpty()) {
                this.addNewElement();
                int imglen = img.length();
                String[] cols = img.split(",");
                if (cols.length > 1) {
                    img = cols[0];
                }

                this.currentElement.isImage = true;
                this.currentElement.text = img.trim();
                if (this.currentElement.text.equals("music")) {
                    this.currentElement.text = "Icon_music_notes";
                }

                if (this.allowChatIcons && this.isValidImage(this.currentElement.text)) {
                    this.currentElement.tex = Texture.getSharedTexture(this.currentElement.text);
                    this.currentElement.isTextImage = true;
                } else if (this.allowImages) {
                    this.currentElement.tex = Texture.getSharedTexture("Item_" + this.currentElement.text);
                    if (this.currentElement.tex == null) {
                        this.currentElement.tex = Texture.getSharedTexture("media/ui/Container_" + this.currentElement.text);
                    }

                    if (this.currentElement.tex != null) {
                        this.currentElement.isTextImage = false;
                        this.currentElement.text = "Item_" + this.currentElement.text;
                    }
                }

                if (this.allowAnyImage && this.currentElement.tex == null) {
                    this.currentElement.tex = Texture.getSharedTexture(this.currentElement.text);
                    if (this.currentElement.tex != null) {
                        this.currentElement.isTextImage = false;
                    }
                }

                if (cols.length == 4) {
                    int r = this.tryColorInt(cols[1]);
                    int g = this.tryColorInt(cols[2]);
                    int b = this.tryColorInt(cols[3]);
                    if (r != -1 && g != -1 && b != -1) {
                        this.currentElement.useColor = true;
                        this.currentElement.r = r / 255.0F;
                        this.currentElement.g = g / 255.0F;
                        this.currentElement.b = b / 255.0F;
                    }
                }

                this.addNewElement();
                return pos + imglen + 1;
            }
        } else if (this.allowColors && tag.equals("col")) {
            String color = this.readTagValue(chars, pos);
            if (color != null) {
                String[] colsx = color.split(",");
                if (colsx.length == 3) {
                    int r = this.tryColorInt(colsx[0]);
                    int g = this.tryColorInt(colsx[1]);
                    int b = this.tryColorInt(colsx[2]);
                    if (r != -1 && g != -1 && b != -1) {
                        this.addNewElement();
                        this.currentElement.useColor = true;
                        this.currentElement.r = r / 255.0F;
                        this.currentElement.g = g / 255.0F;
                        this.currentElement.b = b / 255.0F;
                        this.hasOpened = true;
                        return pos + color.length() + 1;
                    }
                }
            }
        } else if (tag.equals("cdt")) {
            String time = this.readTagValue(chars, pos);
            if (time != null) {
                float ftime = this.internalClock;

                try {
                    ftime = Float.parseFloat(time);
                    ftime *= 60.0F;
                } catch (NumberFormatException var10) {
                    var10.printStackTrace();
                }

                this.internalClock = ftime;
                return pos + time.length() + 1;
            }
        }

        return -1;
    }

    public void setDefaultFont(UIFont f) {
        if (f != this.defaultFontEnum) {
            this.ReadString(f, this.original, this.maxCharsLine);
        }
    }

    private void setDefaultFontInternal(UIFont f) {
        if (this.defaultFont == null || f != this.defaultFontEnum) {
            this.defaultFontEnum = f;
            this.defaultFont = TextManager.instance.getFontFromEnum(f);
        }
    }

    public void ReadString(String str) {
        this.ReadString(this.defaultFontEnum, str, this.maxCharsLine);
    }

    public void ReadString(String str, int maxLineWidth) {
        this.ReadString(this.defaultFontEnum, str, maxLineWidth);
    }

    public void ReadString(UIFont font, String str, int maxLineWidth) {
        if (str == null) {
            str = "";
        }

        this.reset();
        this.setDefaultFontInternal(font);
        if (this.defaultFont != null) {
            this.maxCharsLine = maxLineWidth;
            this.original = str;
            char[] chars = str.toCharArray();
            this.hasOpened = false;
            String word = "";

            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (this.allowBbcode && c == '[') {
                    if (!word.isEmpty()) {
                        this.addWord(word);
                        word = "";
                    }

                    if (i + 4 < chars.length) {
                        String tag = ("" + chars[i + 1] + chars[i + 2] + chars[i + 3]).toLowerCase();
                        if (this.allowLineBreaks && tag.equals("br/")) {
                            this.addNewLine();
                            i += 4;
                            continue;
                        }

                        if (!this.hasOpened) {
                            int pos = this.readTag(chars, i + 4, tag);
                            if (pos >= 0) {
                                i = pos;
                                continue;
                            }
                        }
                    }

                    if (this.hasOpened && i + 2 < chars.length && chars[i + 1] == '/' && chars[i + 2] == ']') {
                        this.hasOpened = false;
                        this.addNewElement();
                        i += 2;
                        continue;
                    }
                }

                if (Character.isWhitespace(c) && i > 0 && !Character.isWhitespace(chars[i - 1])) {
                    this.addWord(word);
                    word = "";
                }

                word = word + c;
                this.unformatted = this.unformatted + c;
            }

            if (!word.isEmpty()) {
                this.addWord(word);
            }

            this.calculateDimensions();
        }
    }

    public void calculateDimensions() {
        this.width = 0;
        this.height = 0;
        int maxHeight = 0;

        for (int j = 0; j < this.lines.size(); j++) {
            TextDrawObject.DrawLine d = this.lines.get(j);
            d.h = 0;
            d.w = 0;

            for (int i = 0; i < d.elements.size(); i++) {
                TextDrawObject.DrawElement de = d.elements.get(i);
                de.w = 0;
                de.h = 0;
                if (de.isImage && de.tex != null) {
                    if (de.isTextImage) {
                        de.w = de.tex.getWidth();
                        de.h = de.tex.getHeight();
                    } else {
                        de.w = (int)(de.tex.getWidth() * 0.75F);
                        de.h = (int)(de.tex.getHeight() * 0.75F);
                    }
                } else if (de.useFont && de.font != null) {
                    de.w = de.font.getWidth(de.text);
                    de.h = de.font.getHeight(de.text);
                } else if (this.defaultFont != null) {
                    de.w = this.defaultFont.getWidth(de.text);
                    de.h = this.defaultFont.getHeight(de.text);
                }

                d.w = d.w + de.w;
                if (de.h > d.h) {
                    d.h = de.h;
                }
            }

            if (d.w > this.width) {
                this.width = d.w;
            }

            this.height = this.height + d.h;
            if (d.h > maxHeight) {
                maxHeight = d.h;
            }
        }

        if (this.equalizeLineHeights) {
            this.height = 0;

            for (int i = 0; i < this.lines.size(); i++) {
                TextDrawObject.DrawLine line = this.lines.get(i);
                line.h = maxHeight;
                this.height += maxHeight;
            }
        }
    }

    public void Draw(double x, double y) {
        this.Draw(this.defaultHorz, x, y, this.defaultR, this.defaultG, this.defaultB, this.defaultA, false);
    }

    public void Draw(double x, double y, boolean drawOutlines) {
        this.Draw(this.defaultHorz, x, y, this.defaultR, this.defaultG, this.defaultB, this.defaultA, drawOutlines);
    }

    public void Draw(double x, double y, boolean drawOutlines, float alpha) {
        this.Draw(this.defaultHorz, x, y, this.defaultR, this.defaultG, this.defaultB, alpha, drawOutlines);
    }

    public void Draw(double x, double y, double r, double g, double b, double a, boolean drawOutlines) {
        this.Draw(this.defaultHorz, x, y, r, g, b, a, drawOutlines);
    }

    public void Draw(TextDrawHorizontal horz, double x, double y, double r, double g, double b, double a, boolean drawOutlines) {
        this.DrawRaw(horz, x, y, (float)r, (float)g, (float)b, (float)a, drawOutlines);
    }

    public void AddBatchedDraw(double x, double y) {
        this.AddBatchedDraw(this.defaultHorz, x, y, this.defaultR, this.defaultG, this.defaultB, this.defaultA, false);
    }

    public void AddBatchedDraw(double x, double y, boolean drawOutlines) {
        this.AddBatchedDraw(this.defaultHorz, x, y, this.defaultR, this.defaultG, this.defaultB, this.defaultA, drawOutlines);
    }

    public void AddBatchedDraw(double x, double y, boolean drawOutlines, float alpha) {
        this.AddBatchedDraw(this.defaultHorz, x, y, this.defaultR, this.defaultG, this.defaultB, alpha, drawOutlines);
    }

    public void AddBatchedDraw(double x, double y, double r, double g, double b, double a, boolean drawOutlines) {
        this.AddBatchedDraw(this.defaultHorz, x, y, r, g, b, a, drawOutlines);
    }

    public void AddBatchedDraw(TextDrawHorizontal horz, double x, double y, double r, double g, double b, double a, boolean drawOutlines) {
        if (!GameServer.server) {
            TextDrawObject.RenderBatch batch = renderBatchPool.isEmpty() ? new TextDrawObject.RenderBatch() : renderBatchPool.pop();
            batch.playerNum = IsoPlayer.getPlayerIndex();
            batch.element = this;
            batch.horz = horz;
            batch.x = x;
            batch.y = y;
            batch.r = (float)r;
            batch.g = (float)g;
            batch.b = (float)b;
            batch.a = (float)a;
            batch.drawOutlines = drawOutlines;
            renderBatch.add(batch);
        }
    }

    public static void RenderBatch(int playerNum) {
        if (!renderBatch.isEmpty()) {
            for (int i = 0; i < renderBatch.size(); i++) {
                TextDrawObject.RenderBatch batch = renderBatch.get(i);
                if (batch.playerNum == playerNum) {
                    batch.element.DrawRaw(batch.horz, batch.x, batch.y, batch.r, batch.g, batch.b, batch.a, batch.drawOutlines);
                    renderBatchPool.add(batch);
                    renderBatch.remove(i--);
                }
            }
        }
    }

    public static void NoRender(int playerNum) {
        for (int i = 0; i < renderBatch.size(); i++) {
            TextDrawObject.RenderBatch batch = renderBatch.get(i);
            if (batch.playerNum == playerNum) {
                renderBatchPool.add(batch);
                renderBatch.remove(i--);
            }
        }
    }

    public void DrawRaw(TextDrawHorizontal horz, double x, double y, float r, float g, float b, float a, boolean drawOutlines) {
        double drawX = x;
        double drawY = y;
        double yOffset = 0.0;
        int screenWidth = Core.getInstance().getScreenWidth();
        int screenHeight = Core.getInstance().getScreenHeight();
        int slop = 20;
        if (horz == TextDrawHorizontal.Center) {
            drawX = x - this.getWidth() / 2;
        } else if (horz == TextDrawHorizontal.Right) {
            drawX = x - this.getWidth();
        }

        if (!(drawX - 20.0 >= screenWidth) && !(drawX + this.getWidth() + 20.0 <= 0.0) && !(y - 20.0 >= screenHeight) && !(y + this.getHeight() + 20.0 <= 0.0)) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glBlendFunc(770, 771);
            }

            if (this.drawBackground && ChatElement.backdropTexture != null) {
                ChatElement.backdropTexture.renderInnerBased((int)drawX, (int)y, this.getWidth(), this.getHeight(), 0.0F, 0.0F, 0.0F, 0.4F * a);
            }

            float outlineAlpha = this.outlineA;
            if (drawOutlines && a < 1.0F) {
                outlineAlpha = this.outlineA * a;
            }

            for (int i = 0; i < this.lines.size(); i++) {
                TextDrawObject.DrawLine line = this.lines.get(i);
                drawX = x;
                if (horz == TextDrawHorizontal.Center) {
                    drawX = x - line.w / 2;
                } else if (horz == TextDrawHorizontal.Right) {
                    drawX = x - line.w;
                }

                for (int j = 0; j < line.elements.size(); j++) {
                    TextDrawObject.DrawElement elem = line.elements.get(j);
                    yOffset = line.h / 2 - elem.h / 2;
                    this.elemText = this.scrambleVal > 0.0F ? elem.scrambleText : elem.text;
                    if (elem.isImage && elem.tex != null) {
                        if (drawOutlines && elem.isTextImage) {
                            SpriteRenderer.instance
                                .renderi(
                                    elem.tex,
                                    (int)(drawX - 1.0),
                                    (int)(drawY + yOffset - 1.0),
                                    elem.w,
                                    elem.h,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha,
                                    null
                                );
                            SpriteRenderer.instance
                                .renderi(
                                    elem.tex,
                                    (int)(drawX + 1.0),
                                    (int)(drawY + yOffset + 1.0),
                                    elem.w,
                                    elem.h,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha,
                                    null
                                );
                            SpriteRenderer.instance
                                .renderi(
                                    elem.tex,
                                    (int)(drawX - 1.0),
                                    (int)(drawY + yOffset + 1.0),
                                    elem.w,
                                    elem.h,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha,
                                    null
                                );
                            SpriteRenderer.instance
                                .renderi(
                                    elem.tex,
                                    (int)(drawX + 1.0),
                                    (int)(drawY + yOffset - 1.0),
                                    elem.w,
                                    elem.h,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha,
                                    null
                                );
                        }

                        if (elem.useColor) {
                            SpriteRenderer.instance.renderi(elem.tex, (int)drawX, (int)(drawY + yOffset), elem.w, elem.h, elem.r, elem.g, elem.b, a, null);
                        } else if (elem.isTextImage) {
                            SpriteRenderer.instance.renderi(elem.tex, (int)drawX, (int)(drawY + yOffset), elem.w, elem.h, r, g, b, a, null);
                        } else {
                            SpriteRenderer.instance.renderi(elem.tex, (int)drawX, (int)(drawY + yOffset), elem.w, elem.h, 1.0F, 1.0F, 1.0F, a, null);
                        }
                    } else if (elem.useFont && elem.font != null) {
                        if (drawOutlines) {
                            elem.font
                                .drawString(
                                    (float)(drawX - 1.0),
                                    (float)(drawY + yOffset - 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                            elem.font
                                .drawString(
                                    (float)(drawX + 1.0),
                                    (float)(drawY + yOffset + 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                            elem.font
                                .drawString(
                                    (float)(drawX - 1.0),
                                    (float)(drawY + yOffset + 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                            elem.font
                                .drawString(
                                    (float)(drawX + 1.0),
                                    (float)(drawY + yOffset - 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                        }

                        elem.font.drawString((float)drawX, (float)(drawY + yOffset), this.elemText, r, g, b, a);
                    } else if (this.defaultFont != null) {
                        if (drawOutlines) {
                            this.defaultFont
                                .drawString(
                                    (float)(drawX - 1.0),
                                    (float)(drawY + yOffset - 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                            this.defaultFont
                                .drawString(
                                    (float)(drawX + 1.0),
                                    (float)(drawY + yOffset + 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                            this.defaultFont
                                .drawString(
                                    (float)(drawX - 1.0),
                                    (float)(drawY + yOffset + 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                            this.defaultFont
                                .drawString(
                                    (float)(drawX + 1.0),
                                    (float)(drawY + yOffset - 1.0),
                                    this.elemText,
                                    this.outlineR,
                                    this.outlineG,
                                    this.outlineB,
                                    outlineAlpha
                                );
                        }

                        if (elem.useColor) {
                            this.defaultFont.drawString((float)drawX, (float)(drawY + yOffset), this.elemText, elem.r, elem.g, elem.b, a);
                        } else {
                            this.defaultFont.drawString((float)drawX, (float)(drawY + yOffset), this.elemText, r, g, b, a);
                        }
                    }

                    drawX += elem.w;
                }

                drawY += line.h;
            }
        }
    }

    private static final class DrawElement {
        private String text = "";
        private String scrambleText = "";
        private float currentScrambleVal;
        private UIFont f = UIFont.AutoNormSmall;
        private AngelCodeFont font;
        private float r = 1.0F;
        private float g = 1.0F;
        private float b = 1.0F;
        private int w;
        private int h;
        private boolean isImage;
        private boolean useFont;
        private boolean useColor;
        private Texture tex;
        private boolean isTextImage;
        private int charWidth;

        private void reset() {
            this.text = "";
            this.scrambleText = "";
            this.f = UIFont.AutoNormSmall;
            this.font = null;
            this.r = 1.0F;
            this.g = 1.0F;
            this.b = 1.0F;
            this.w = 0;
            this.h = 0;
            this.isImage = false;
            this.useFont = false;
            this.useColor = false;
            this.tex = null;
            this.isTextImage = false;
            this.charWidth = 0;
        }

        private void addText(String txt) {
            this.text = this.text + txt;
            this.charWidth = this.text.length();
        }

        private void scrambleText(float scrambleVal) {
            if (scrambleVal != this.currentScrambleVal) {
                this.currentScrambleVal = scrambleVal;
                int threshold = (int)(scrambleVal * 100.0F);
                String[] words = this.text.split("\\s+");
                this.scrambleText = "";

                for (String word : words) {
                    int r = Rand.Next(100);
                    if (r > threshold) {
                        this.scrambleText = this.scrambleText + word + " ";
                    } else {
                        char[] array = new char[word.length()];
                        Arrays.fill(array, ".".charAt(0));
                        this.scrambleText = this.scrambleText + new String(array) + " ";
                    }
                }
            }
        }

        private void trim() {
            this.text = this.text.trim();
        }

        private TextDrawObject.DrawElement softclone() {
            TextDrawObject.DrawElement c = new TextDrawObject.DrawElement();
            if (this.useColor) {
                c.r = this.r;
                c.g = this.g;
                c.b = this.b;
                c.useColor = this.useColor;
            }

            if (this.useFont) {
                c.f = this.f;
                c.font = this.font;
                c.useFont = this.useFont;
            }

            return c;
        }
    }

    private static final class DrawLine {
        private final ArrayList<TextDrawObject.DrawElement> elements = new ArrayList<>();
        private int h;
        private int w;
        private int charW;

        private void addElement(TextDrawObject.DrawElement elem) {
            this.elements.add(elem);
        }
    }

    private static final class RenderBatch {
        int playerNum;
        TextDrawObject element;
        TextDrawHorizontal horz;
        double x;
        double y;
        float r;
        float g;
        float b;
        float a;
        boolean drawOutlines;
    }
}
