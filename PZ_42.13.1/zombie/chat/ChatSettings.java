// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.network.ByteBufferWriter;
import zombie.ui.UIFont;

public class ChatSettings {
    private boolean unique;
    private Color fontColor;
    private UIFont font;
    private ChatSettings.FontSize fontSize;
    private boolean bold;
    private boolean allowImages;
    private boolean allowChatIcons;
    private boolean allowColors;
    private boolean allowFonts;
    private boolean allowBbcode;
    private boolean equalizeLineHeights;
    private boolean showAuthor;
    private boolean showTimestamp;
    private boolean showChatTitle;
    private boolean useOnlyActiveTab;
    private float range;
    private float zombieAttractionRange;
    public static final float infinityRange = -1.0F;

    public ChatSettings() {
        this.unique = true;
        this.fontColor = Color.white;
        this.font = UIFont.Dialogue;
        this.bold = true;
        this.showAuthor = true;
        this.showTimestamp = true;
        this.showChatTitle = true;
        this.range = -1.0F;
        this.zombieAttractionRange = -1.0F;
        this.useOnlyActiveTab = false;
        this.fontSize = ChatSettings.FontSize.Medium;
    }

    public ChatSettings(ByteBuffer bb) {
        this.unique = bb.get() == 1;
        this.fontColor = new Color(bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat());
        this.font = UIFont.FromString(GameWindow.ReadString(bb));
        this.bold = bb.get() == 1;
        this.allowImages = bb.get() == 1;
        this.allowChatIcons = bb.get() == 1;
        this.allowColors = bb.get() == 1;
        this.allowFonts = bb.get() == 1;
        this.allowBbcode = bb.get() == 1;
        this.equalizeLineHeights = bb.get() == 1;
        this.showAuthor = bb.get() == 1;
        this.showTimestamp = bb.get() == 1;
        this.showChatTitle = bb.get() == 1;
        this.range = bb.getFloat();
        if (bb.get() == 1) {
            this.zombieAttractionRange = bb.getFloat();
        } else {
            this.zombieAttractionRange = this.range;
        }

        this.fontSize = ChatSettings.FontSize.Medium;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public Color getFontColor() {
        return this.fontColor;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    public void setFontColor(float r, float g, float b, float a) {
        this.fontColor = new Color(r, g, b, a);
    }

    public UIFont getFont() {
        return this.font;
    }

    public void setFont(UIFont font) {
        this.font = font;
    }

    public String getFontSize() {
        return this.fontSize.toString().toLowerCase();
    }

    public void setFontSize(String fontSize) {
        switch (fontSize) {
            case "small":
            case "Small":
                this.fontSize = ChatSettings.FontSize.Small;
                break;
            case "medium":
            case "Medium":
                this.fontSize = ChatSettings.FontSize.Medium;
                break;
            case "large":
            case "Large":
                this.fontSize = ChatSettings.FontSize.Large;
                break;
            default:
                this.fontSize = ChatSettings.FontSize.NotDefine;
        }
    }

    public boolean isBold() {
        return this.bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isShowAuthor() {
        return this.showAuthor;
    }

    public void setShowAuthor(boolean showAuthor) {
        this.showAuthor = showAuthor;
    }

    public boolean isShowTimestamp() {
        return this.showTimestamp;
    }

    public void setShowTimestamp(boolean showTimestamp) {
        this.showTimestamp = showTimestamp;
    }

    public boolean isShowChatTitle() {
        return this.showChatTitle;
    }

    public void setShowChatTitle(boolean showChatTitle) {
        this.showChatTitle = showChatTitle;
    }

    public boolean isAllowImages() {
        return this.allowImages;
    }

    public void setAllowImages(boolean allowImages) {
        this.allowImages = allowImages;
    }

    public boolean isAllowChatIcons() {
        return this.allowChatIcons;
    }

    public void setAllowChatIcons(boolean allowChatIcons) {
        this.allowChatIcons = allowChatIcons;
    }

    public boolean isAllowColors() {
        return this.allowColors;
    }

    public void setAllowColors(boolean allowColors) {
        this.allowColors = allowColors;
    }

    public boolean isAllowFonts() {
        return this.allowFonts;
    }

    public void setAllowFonts(boolean allowFonts) {
        this.allowFonts = allowFonts;
    }

    public boolean isAllowBBcode() {
        return this.allowBbcode;
    }

    public void setAllowBBcode(boolean allowBbcode) {
        this.allowBbcode = allowBbcode;
    }

    public boolean isEqualizeLineHeights() {
        return this.equalizeLineHeights;
    }

    public void setEqualizeLineHeights(boolean equalizeLineHeights) {
        this.equalizeLineHeights = equalizeLineHeights;
    }

    public float getRange() {
        return this.range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public float getZombieAttractionRange() {
        return this.zombieAttractionRange == -1.0F ? this.range : this.zombieAttractionRange;
    }

    public void setZombieAttractionRange(float zombieAttractionRange) {
        this.zombieAttractionRange = zombieAttractionRange;
    }

    public boolean isUseOnlyActiveTab() {
        return this.useOnlyActiveTab;
    }

    public void setUseOnlyActiveTab(boolean useOnlyActiveTab) {
        this.useOnlyActiveTab = useOnlyActiveTab;
    }

    public void pack(ByteBufferWriter bb) {
        bb.putBoolean(this.unique);
        bb.putFloat(this.fontColor.r);
        bb.putFloat(this.fontColor.g);
        bb.putFloat(this.fontColor.b);
        bb.putFloat(this.fontColor.a);
        bb.putUTF(this.font.toString());
        bb.putBoolean(this.bold);
        bb.putBoolean(this.allowImages);
        bb.putBoolean(this.allowChatIcons);
        bb.putBoolean(this.allowColors);
        bb.putBoolean(this.allowFonts);
        bb.putBoolean(this.allowBbcode);
        bb.putBoolean(this.equalizeLineHeights);
        bb.putBoolean(this.showAuthor);
        bb.putBoolean(this.showTimestamp);
        bb.putBoolean(this.showChatTitle);
        bb.putFloat(this.range);
        bb.putBoolean(this.range != this.zombieAttractionRange);
        if (this.range != this.zombieAttractionRange) {
            bb.putFloat(this.zombieAttractionRange);
        }
    }

    public static enum FontSize {
        NotDefine,
        Small,
        Medium,
        Large;
    }
}
