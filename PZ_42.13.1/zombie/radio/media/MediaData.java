// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.media;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class MediaData {
    private final String id;
    private final String itemDisplayName;
    private String title;
    private String subtitle;
    private String author;
    private String extra;
    private short index;
    private String category;
    private final int spawning;
    private final ArrayList<MediaData.MediaLineData> lines = new ArrayList<>();

    public MediaData(String id, String itemDisplayName, int spawning) {
        this.itemDisplayName = itemDisplayName;
        this.id = id;
        this.spawning = spawning;
        if (Core.debug) {
            if (itemDisplayName == null) {
                throw new RuntimeException("ItemDisplayName may not be null.");
            }

            if (id == null) {
                throw new RuntimeException("Id may not be null.");
            }
        }
    }

    public void addLine(String text, float r, float g, float b, String codes) {
        MediaData.MediaLineData line = new MediaData.MediaLineData(text, r, g, b, codes);
        this.lines.add(line);
    }

    public int getLineCount() {
        return this.lines.size();
    }

    public String getTranslatedItemDisplayName() {
        return Translator.getText(this.itemDisplayName);
    }

    public boolean hasTitle() {
        return this.title != null;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleEN() {
        return this.title != null ? Translator.getTextMediaEN(this.title) : null;
    }

    public String getTranslatedTitle() {
        return this.title != null ? Translator.getText(this.title) : null;
    }

    public boolean hasSubTitle() {
        return this.subtitle != null;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSubtitleEN() {
        return this.subtitle != null ? Translator.getTextMediaEN(this.subtitle) : null;
    }

    public String getTranslatedSubTitle() {
        return this.subtitle != null ? Translator.getText(this.subtitle) : null;
    }

    public boolean hasAuthor() {
        return this.author != null;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEN() {
        return this.author != null ? Translator.getTextMediaEN(this.author) : null;
    }

    public String getTranslatedAuthor() {
        return this.author != null ? Translator.getText(this.author) : null;
    }

    public boolean hasExtra() {
        return this.extra != null;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getExtraEN() {
        return this.extra != null ? Translator.getTextMediaEN(this.extra) : null;
    }

    public String getTranslatedExtra() {
        return this.extra != null ? Translator.getText(this.extra) : null;
    }

    public String getId() {
        return this.id;
    }

    public short getIndex() {
        return this.index;
    }

    protected void setIndex(short index) {
        this.index = index;
    }

    public String getCategory() {
        return this.category;
    }

    protected void setCategory(String category) {
        this.category = category;
    }

    public int getSpawning() {
        return this.spawning;
    }

    public byte getMediaType() {
        if (this.category == null) {
            DebugLog.log("Warning MediaData has no category set, mediadata = " + (this.itemDisplayName != null ? this.itemDisplayName : "unknown"));
        }

        return RecordedMedia.getMediaTypeForCategory(this.category);
    }

    public MediaData.MediaLineData getLine(int index) {
        return index >= 0 && index < this.lines.size() ? this.lines.get(index) : null;
    }

    @UsedFromLua
    public static final class MediaLineData {
        private final String text;
        private final Color color;
        private final String codes;

        public MediaLineData(String text, float r, float g, float b, String codes) {
            this.text = text;
            this.codes = codes;
            if (r == 0.0F && g == 0.0F && b == 0.0F) {
                r = 1.0F;
                g = 1.0F;
                b = 1.0F;
            }

            this.color = new Color(r, g, b);
        }

        public String getTranslatedText() {
            return Translator.getText(this.text);
        }

        public Color getColor() {
            return this.color;
        }

        public float getR() {
            return this.color.r;
        }

        public float getG() {
            return this.color.g;
        }

        public float getB() {
            return this.color.b;
        }

        public String getCodes() {
            return this.codes;
        }

        public String getTextGuid() {
            return this.text;
        }
    }
}
