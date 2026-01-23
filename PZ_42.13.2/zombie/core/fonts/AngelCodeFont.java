// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.fonts;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.procedure.TShortObjectProcedure;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import org.lwjgl.opengl.GL11;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetStateObserver;
import zombie.core.Color;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

/**
 * A font implementation that will parse BMFont format font files. The font files can be output
 *  by Hiero, which is included with Slick, and also the AngelCode font tool available at:
 * 
 *  http://www.angelcode.com/products/bmfont/
 * 
 *  This implementation copes with both the font display and kerning information
 *  allowing nicer looking paragraphs of text. Note that this utility only
 *  supports the text BMFont format definition file.
 */
@UsedFromLua
public final class AngelCodeFont implements Font, AssetStateObserver {
    private static final int DISPLAY_LIST_CACHE_SIZE = 200;
    private static final int MAX_CHAR = 255;
    private int baseDisplayListId = -1;
    /**
     * The characters building up the font
     */
    public AngelCodeFont.CharDef[] chars;
    private boolean displayListCaching;
    private AngelCodeFont.DisplayList eldestDisplayList;
    private int eldestDisplayListId;
    private final LinkedHashMap<String, AngelCodeFont.DisplayList> displayLists = new LinkedHashMap<String, AngelCodeFont.DisplayList>(200, 1.0F, true) {
        {
            Objects.requireNonNull(AngelCodeFont.this);
        }

        @Override
        protected boolean removeEldestEntry(Entry eldest) {
            AngelCodeFont.this.eldestDisplayList = (AngelCodeFont.DisplayList)eldest.getValue();
            AngelCodeFont.this.eldestDisplayListId = AngelCodeFont.this.eldestDisplayList.id;
            return false;
        }
    };
    private Texture fontImage;
    private int lineHeight;
    private final HashMap<Short, Texture> pages = new HashMap<>();
    private File fntFile;
    private boolean sdf;
    public static int xoff;
    public static int yoff;
    public static Color curCol;
    public static float curR;
    public static float curG;
    public static float curB;
    public static float curA;
    private static float scale;
    private static char[] data = new char[256];

    /**
     * Create a new font based on a font definition from AngelCode's tool and
     *  the font image generated from the tool.
     * 
     * @param this The location of the font defnition file
     * @param fntFile The image to use for the font
     */
    public AngelCodeFont(String fntFile, Texture image) throws FileNotFoundException {
        this.fontImage = image;
        String path = fntFile;
        InputStream is = new FileInputStream(new File(fntFile));
        if (fntFile.startsWith("/")) {
            path = fntFile.substring(1);
        }

        int index;
        while ((index = path.indexOf("\\")) != -1) {
            path = path.substring(0, index) + "/" + path.substring(index + 1);
        }

        this.parseFnt(is);
    }

    /**
     * Create a new font based on a font definition from AngelCode's tool and
     *  the font image generated from the tool.
     * 
     * @param this The location of the font defnition file
     * @param fntFile The location of the font image
     */
    public AngelCodeFont(String fntFile, String imgFile) throws FileNotFoundException {
        if (!StringUtils.isNullOrWhitespace(imgFile)) {
            int flags = 0;
            this.fontImage = Texture.getSharedTexture(imgFile, 0);
            if (this.fontImage != null && !this.fontImage.isReady()) {
                this.fontImage.getObserverCb().add(this);
            }
        }

        String path = fntFile;
        InputStream is = null;
        if (fntFile.startsWith("/")) {
            path = fntFile.substring(1);
        }

        int index;
        while ((index = path.indexOf("\\")) != -1) {
            path = path.substring(0, index) + "/" + path.substring(index + 1);
        }

        this.fntFile = new File(ZomboidFileSystem.instance.getString(path));
        InputStream var7 = new FileInputStream(ZomboidFileSystem.instance.getString(path));
        this.parseFnt(var7);
    }

    /**
     * Description copied from interface: Font
     * 
     * @param x The x location at which to draw the string
     * @param y The y location at which to draw the string
     * @param text The text to be displayed
     */
    @Override
    public void drawString(float x, float y, String text) {
        this.drawString(x, y, text, Color.white);
    }

    /**
     * Description copied from interface: Font
     * 
     * @param x The x location at which to draw the string
     * @param y The y location at which to draw the string
     * @param text The text to be displayed
     * @param col The colour to draw with
     */
    @Override
    public void drawString(float x, float y, String text, Color col) {
        this.drawString(x, y, text, col, 0, text.length() - 1);
    }

    public void drawString(float x, float y, String text, float r, float g, float b, float a) {
        this.drawString(x, y, text, r, g, b, a, 0, text.length() - 1);
    }

    public void drawString(float x, float y, float scale, String text, float r, float g, float b, float a) {
        this.drawString(x, y, scale, text, r, g, b, a, 0, text.length() - 1);
    }

    /**
     * Description copied from interface: Font
     * 
     * @param x The x location at which to draw the string
     * @param y The y location at which to draw the string
     * @param text The text to be displayed
     * @param col The colour to draw with
     * @param startIndex The index of the first character to draw
     * @param endIndex The index of the last character from the string to draw
     */
    @Override
    public void drawString(float x, float y, String text, Color col, int startIndex, int endIndex) {
        xoff = (int)x;
        yoff = (int)y;
        curR = col.r;
        curG = col.g;
        curB = col.b;
        curA = col.a;
        scale = 0.0F;
        Texture.lr = col.r;
        Texture.lg = col.g;
        Texture.lb = col.b;
        Texture.la = col.a;
        if (this.displayListCaching && startIndex == 0 && endIndex == text.length() - 1) {
            AngelCodeFont.DisplayList displayList = this.displayLists.get(text);
            if (displayList != null) {
                GL11.glCallList(displayList.id);
            } else {
                displayList = new AngelCodeFont.DisplayList();
                displayList.text = text;
                int displayListCount = this.displayLists.size();
                if (displayListCount < 200) {
                    displayList.id = this.baseDisplayListId + displayListCount;
                } else {
                    displayList.id = this.eldestDisplayListId;
                    this.displayLists.remove(this.eldestDisplayList.text);
                }

                this.displayLists.put(text, displayList);
                GL11.glNewList(displayList.id, 4865);
                this.render(text, startIndex, endIndex);
                GL11.glEndList();
            }
        } else {
            this.render(text, startIndex, endIndex);
        }
    }

    public void drawString(float x, float y, String text, float r, float g, float b, float a, int startIndex, int endIndex) {
        this.drawString(x, y, 0.0F, text, r, g, b, a, startIndex, endIndex);
    }

    public void drawString(float x, float y, float scale, String text, float r, float g, float b, float a, int startIndex, int endIndex) {
        xoff = (int)x;
        yoff = (int)y;
        curR = r;
        curG = g;
        curB = b;
        curA = a;
        AngelCodeFont.scale = scale;
        Texture.lr = r;
        Texture.lg = g;
        Texture.lb = b;
        Texture.la = a;
        if (this.displayListCaching && startIndex == 0 && endIndex == text.length() - 1) {
            AngelCodeFont.DisplayList displayList = this.displayLists.get(text);
            if (displayList != null) {
                GL11.glCallList(displayList.id);
            } else {
                displayList = new AngelCodeFont.DisplayList();
                displayList.text = text;
                int displayListCount = this.displayLists.size();
                if (displayListCount < 200) {
                    displayList.id = this.baseDisplayListId + displayListCount;
                } else {
                    displayList.id = this.eldestDisplayListId;
                    this.displayLists.remove(this.eldestDisplayList.text);
                }

                this.displayLists.put(text, displayList);
                GL11.glNewList(displayList.id, 4865);
                this.render(text, startIndex, endIndex);
                GL11.glEndList();
            }
        } else {
            this.render(text, startIndex, endIndex);
        }
    }

    /**
     * Description copied from interface: Font
     * 
     * @param text The string to obtain the rendered with of
     * @return The width of the given string
     */
    @Override
    public int getHeight(String text) {
        return this.getHeight(text, false, false);
    }

    public int getHeight(String text, boolean returnActualHeight, boolean returnOffset) {
        AngelCodeFont.DisplayList displayList = null;
        if (this.displayListCaching) {
            displayList = this.displayLists.get(text);
            if (displayList != null && displayList.height != null) {
                return displayList.height.intValue();
            }
        }

        int lines = 1;
        int maxHeight = 0;
        int minOffset = 1000000;

        for (int i = 0; i < text.length(); i++) {
            int id = text.charAt(i);
            if (id == 10) {
                lines++;
                maxHeight = 0;
            } else if (id != 32 && id < this.chars.length) {
                AngelCodeFont.CharDef charDef = this.chars[id];
                if (charDef != null) {
                    maxHeight = Math.max(charDef.height + charDef.yoffset, maxHeight);
                    minOffset = Math.min(charDef.yoffset, minOffset);
                }
            }
        }

        if (returnActualHeight) {
            return maxHeight - minOffset;
        } else if (returnOffset) {
            return minOffset;
        } else {
            maxHeight = lines * this.getLineHeight();
            if (displayList != null) {
                displayList.height = (short)maxHeight;
            }

            return maxHeight;
        }
    }

    /**
     * Description copied from interface: Font
     * @return The maxium height of any line drawn by this font
     */
    @Override
    public int getLineHeight() {
        return this.lineHeight;
    }

    /**
     * Description copied from interface: Font
     * 
     * @param text The string to obtain the rendered with of
     * @return The width of the given string
     */
    @Override
    public int getWidth(String text) {
        return this.getWidth(text, 0, text.length() - 1, false);
    }

    @Override
    public int getWidth(String text, boolean xAdvance) {
        return this.getWidth(text, 0, text.length() - 1, xAdvance);
    }

    @Override
    public int getWidth(String text, int start, int end) {
        return this.getWidth(text, start, end, false);
    }

    @Override
    public int getWidth(String text, int start, int end, boolean xadvance) {
        AngelCodeFont.DisplayList displayList = null;
        if (this.displayListCaching && start == 0 && end == text.length() - 1) {
            displayList = this.displayLists.get(text);
            if (displayList != null && displayList.width != null) {
                return displayList.width.intValue();
            }
        }

        int numChars = end - start + 1;
        int maxWidth = 0;
        int width = 0;
        AngelCodeFont.CharDef lastCharDef = null;

        for (int i = 0; i < numChars; i++) {
            int id = text.charAt(start + i);
            if (id == 10) {
                width = 0;
            } else if (id < this.chars.length) {
                AngelCodeFont.CharDef charDef = this.chars[id];
                if (charDef != null) {
                    if (lastCharDef != null) {
                        width += lastCharDef.getKerning(id);
                    }

                    lastCharDef = charDef;
                    if (!xadvance && i >= numChars - 1) {
                        width += charDef.width;
                    } else {
                        width += charDef.xadvance;
                    }

                    maxWidth = Math.max(maxWidth, width);
                }
            }
        }

        if (displayList != null) {
            displayList.width = (short)maxWidth;
        }

        return maxWidth;
    }

    /**
     * Returns the distance from the y drawing location to the top most pixel of the specified text.
     * 
     * @param text The text that is to be tested
     * @return The yoffset from the y draw location at which text will start
     */
    public int getYOffset(String text) {
        AngelCodeFont.DisplayList displayList = null;
        if (this.displayListCaching) {
            displayList = this.displayLists.get(text);
            if (displayList != null && displayList.yOffset != null) {
                return displayList.yOffset.intValue();
            }
        }

        int stopIndex = text.indexOf(10);
        if (stopIndex == -1) {
            stopIndex = text.length();
        }

        int minYOffset = 10000;

        for (int i = 0; i < stopIndex; i++) {
            int id = text.charAt(i);
            AngelCodeFont.CharDef charDef = this.chars[id];
            if (charDef != null) {
                minYOffset = Math.min(charDef.yoffset, minYOffset);
            }
        }

        if (displayList != null) {
            displayList.yOffset = (short)minYOffset;
        }

        return minYOffset;
    }

    private AngelCodeFont.CharDef parseChar(String line) {
        AngelCodeFont.CharDef def = new AngelCodeFont.CharDef();
        StringTokenizer tokens = new StringTokenizer(line, " =");
        tokens.nextToken();
        tokens.nextToken();
        def.id = Integer.parseInt(tokens.nextToken());
        if (def.id < 0) {
            return null;
        } else {
            if (def.id > 255) {
            }

            tokens.nextToken();
            def.x = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.y = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.width = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.height = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.xoffset = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.yoffset = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.xadvance = Short.parseShort(tokens.nextToken());
            tokens.nextToken();
            def.page = Short.parseShort(tokens.nextToken());
            Texture fontImage1 = this.fontImage;
            if (this.pages.containsKey(def.page)) {
                fontImage1 = this.pages.get(def.page);
            }

            if (fontImage1 != null && fontImage1.isReady()) {
                def.init();
            }

            if (def.id != 32) {
                this.lineHeight = Math.max(def.height + def.yoffset, this.lineHeight);
            }

            return def;
        }
    }

    private void parseFnt(InputStream fntFile) {
        if (this.displayListCaching) {
            this.baseDisplayListId = GL11.glGenLists(200);
            if (this.baseDisplayListId == 0) {
                this.displayListCaching = false;
            }
        }

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(fntFile));
            String info = in.readLine();
            String common = in.readLine();
            TShortObjectHashMap<TShortArrayList> kerning = new TShortObjectHashMap<>(64);
            List<AngelCodeFont.CharDef> charDefs = new ArrayList<>(255);
            int maxChar = 0;
            boolean done = false;

            while (!done) {
                String line = in.readLine();
                if (line == null) {
                    done = true;
                } else {
                    if (line.startsWith("page")) {
                        StringTokenizer tokens = new StringTokenizer(line, " =");
                        tokens.nextToken();
                        tokens.nextToken();
                        short id = Short.parseShort(tokens.nextToken());
                        tokens.nextToken();
                        String file = tokens.nextToken().replace("\"", "");
                        file = this.fntFile.getParent() + File.separatorChar + file;
                        file = file.replace("\\", "/");
                        int flags = 0;
                        flags |= TextureID.useCompression ? 4 : 0;
                        Texture tex = Texture.getSharedTexture(file, flags);
                        if (tex == null) {
                            DebugLog.DetailedInfo.trace("AngelCodeFont failed to load page " + id + " texture " + file);
                        } else {
                            this.pages.put(id, tex);
                            if (!tex.isReady()) {
                                tex.getObserverCb().add(this);
                            }
                        }
                    }

                    if (!line.startsWith("chars c") && line.startsWith("char")) {
                        AngelCodeFont.CharDef def = this.parseChar(line);
                        if (def != null) {
                            maxChar = Math.max(maxChar, def.id);
                            charDefs.add(def);
                        }
                    }

                    if (!line.startsWith("kernings c") && line.startsWith("kerning")) {
                        StringTokenizer tokens = new StringTokenizer(line, " =");
                        tokens.nextToken();
                        tokens.nextToken();
                        short first = Short.parseShort(tokens.nextToken());
                        tokens.nextToken();
                        int second = Integer.parseInt(tokens.nextToken());
                        tokens.nextToken();
                        int offset = Integer.parseInt(tokens.nextToken());
                        TShortArrayList values = kerning.get(first);
                        if (values == null) {
                            values = new TShortArrayList();
                            kerning.put(first, values);
                        }

                        values.add((short)second);
                        values.add((short)offset);
                    }
                }
            }

            this.chars = new AngelCodeFont.CharDef[maxChar + 1];

            for (AngelCodeFont.CharDef def : charDefs) {
                this.chars[def.id] = def;
            }

            kerning.forEachEntry(new TShortObjectProcedure<TShortArrayList>() {
                {
                    Objects.requireNonNull(AngelCodeFont.this);
                }

                public boolean execute(short key, TShortArrayList value) {
                    AngelCodeFont.CharDef charDef = AngelCodeFont.this.chars[key];
                    charDef.kerningSecond = new short[value.size() / 2];
                    charDef.kerningAmount = new short[value.size() / 2];
                    int n = 0;

                    for (int i = 0; i < value.size(); i += 2) {
                        charDef.kerningSecond[n] = value.get(i);
                        charDef.kerningAmount[n] = value.get(i + 1);
                        n++;
                    }

                    short[] sortedSecond = Arrays.copyOf(charDef.kerningSecond, charDef.kerningSecond.length);
                    short[] copyAmount = Arrays.copyOf(charDef.kerningAmount, charDef.kerningAmount.length);
                    Arrays.sort(sortedSecond);

                    for (int i = 0; i < sortedSecond.length; i++) {
                        for (int j = 0; j < charDef.kerningSecond.length; j++) {
                            if (charDef.kerningSecond[j] == sortedSecond[i]) {
                                charDef.kerningAmount[i] = copyAmount[j];
                                break;
                            }
                        }
                    }

                    charDef.kerningSecond = sortedSecond;
                    return true;
                }
            });
            in.close();
        } catch (IOException var15) {
            var15.printStackTrace();
        }
    }

    private void render(String text, int start, int end) {
        end++;
        int numChars = end - start;
        float x = 0.0F;
        float y = 0.0F;
        AngelCodeFont.CharDef lastCharDef = null;
        if (data.length < numChars) {
            data = new char[(numChars + 128 - 1) / 128 * 128];
        }

        text.getChars(start, end, data, 0);

        for (int i = 0; i < numChars; i++) {
            int id = data[i];
            if (id == 10) {
                x = 0.0F;
                y += this.getLineHeight();
            } else {
                if (id >= this.chars.length) {
                    id = 63;
                }

                AngelCodeFont.CharDef charDef = this.chars[id];
                if (charDef != null) {
                    if (lastCharDef != null) {
                        if (scale > 0.0F) {
                            x += lastCharDef.getKerning(id) * scale;
                        } else {
                            x += lastCharDef.getKerning(id);
                        }
                    }

                    lastCharDef = charDef;
                    charDef.draw(x, y);
                    if (scale > 0.0F) {
                        x += charDef.xadvance * scale;
                    } else {
                        x += charDef.xadvance;
                    }
                }
            }
        }
    }

    @Override
    public void onStateChanged(Asset.State oldState, Asset.State newState, Asset asset) {
        if (asset == this.fontImage || this.pages.containsValue(asset)) {
            if (newState == Asset.State.READY) {
                for (AngelCodeFont.CharDef charDef : this.chars) {
                    if (charDef != null && charDef.image == null) {
                        Texture fontImage1 = this.fontImage;
                        if (this.pages.containsKey(charDef.page)) {
                            fontImage1 = this.pages.get(charDef.page);
                        }

                        if (asset == fontImage1) {
                            charDef.init();
                        }
                    }
                }
            }
        }
    }

    public boolean isEmpty() {
        if (this.fontImage != null && this.fontImage.isEmpty()) {
            return true;
        } else {
            for (Texture tex : this.pages.values()) {
                if (tex.isEmpty()) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isSdf() {
        return this.sdf;
    }

    public void setSdf(boolean b) {
        this.sdf = b;
    }

    public void destroy() {
        for (AngelCodeFont.CharDef charDef : this.chars) {
            if (charDef != null) {
                charDef.destroy();
            }
        }

        Arrays.fill(this.chars, null);
        this.pages.clear();
    }

    /**
     * The definition of a single character as defined in the AngelCode file
     *  format
     */
    public class CharDef {
        /**
         * The display list index for this character
         */
        public short dlIndex;
        /**
         * The height of the character image
         */
        public short height;
        /**
         * The id of the character
         */
        public int id;
        /**
         * The image containing the character
         */
        public Texture image;
        /**
         * The kerning info for this character
         */
        public short[] kerningSecond;
        public short[] kerningAmount;
        /**
         * The width of the character image
         */
        public short width;
        /**
         * The x location on the sprite sheet
         */
        public short x;
        /**
         * The amount to move the current position after drawing the character
         */
        public short xadvance;
        /**
         * The amount the x position should be offset when drawing the image
         */
        public short xoffset;
        /**
         * The y location on the sprite sheet
         */
        public short y;
        /**
         * The amount the y position should be offset when drawing the image
         */
        public short yoffset;
        /**
         * The page number for fonts with multiple textures
         */
        public short page;

        public CharDef() {
            Objects.requireNonNull(AngelCodeFont.this);
            super();
        }

        /**
         * Draw this character embedded in a image draw
         * 
         * @param x The x position at which to draw the text
         * @param y The y position at which to draw the text
         */
        public void draw(float x, float y) {
            Texture tex = this.image;
            if (AngelCodeFont.scale > 0.0F) {
                SpriteRenderer.instance
                    .states
                    .getPopulatingActiveState()
                    .render(
                        tex,
                        x + this.xoffset * AngelCodeFont.scale + AngelCodeFont.xoff,
                        y + this.yoffset * AngelCodeFont.scale + AngelCodeFont.yoff,
                        this.width * AngelCodeFont.scale,
                        this.height * AngelCodeFont.scale,
                        AngelCodeFont.curR,
                        AngelCodeFont.curG,
                        AngelCodeFont.curB,
                        AngelCodeFont.curA,
                        null
                    );
            } else {
                SpriteRenderer.instance
                    .renderi(
                        tex,
                        (int)(x + this.xoffset + AngelCodeFont.xoff),
                        (int)(y + this.yoffset + AngelCodeFont.yoff),
                        this.width,
                        this.height,
                        AngelCodeFont.curR,
                        AngelCodeFont.curG,
                        AngelCodeFont.curB,
                        AngelCodeFont.curA,
                        null
                    );
            }
        }

        /**
         * get the kerning offset between this character and the specified character.
         * 
         * @param otherCodePoint The other code point
         * @return the kerning offset
         */
        public int getKerning(int otherCodePoint) {
            if (this.kerningSecond == null) {
                return 0;
            } else {
                int low = 0;
                int high = this.kerningSecond.length - 1;

                while (low <= high) {
                    int midIndex = low + high >>> 1;
                    if (this.kerningSecond[midIndex] < otherCodePoint) {
                        low = midIndex + 1;
                    } else {
                        if (this.kerningSecond[midIndex] <= otherCodePoint) {
                            return this.kerningAmount[midIndex];
                        }

                        high = midIndex - 1;
                    }
                }

                return 0;
            }
        }

        /**
         * Initialise the image by cutting the right section from the map
         *  produced by the AngelCode tool.
         */
        public void init() {
            Texture fontImage = AngelCodeFont.this.fontImage;
            if (AngelCodeFont.this.pages.containsKey(this.page)) {
                fontImage = AngelCodeFont.this.pages.get(this.page);
            }

            this.image = new AngelCodeFont.CharDefTexture(fontImage.getTextureId(), fontImage.getName() + "_" + this.x + "_" + this.y);
            this.image
                .setRegion(
                    this.x + (int)(fontImage.xStart * fontImage.getWidthHW()),
                    this.y + (int)(fontImage.yStart * fontImage.getHeightHW()),
                    this.width,
                    this.height
                );
        }

        public void destroy() {
            if (this.image != null && this.image.getTextureId() != null) {
                ((AngelCodeFont.CharDefTexture)this.image).releaseCharDef();
                this.image = null;
            }
        }

        @Override
        public String toString() {
            return "[CharDef id=" + this.id + " x=" + this.x + " y=" + this.y + "]";
        }
    }

    public static final class CharDefTexture extends Texture {
        public CharDefTexture(TextureID data, String name) {
            super(data, name);
        }

        public void releaseCharDef() {
            this.removeDependency(this.dataid);
        }
    }

    private static class DisplayList {
        Short height;
        int id;
        String text;
        Short width;
        Short yOffset;
    }
}
