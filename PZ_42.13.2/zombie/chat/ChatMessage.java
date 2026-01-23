// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.network.ByteBufferWriter;

@UsedFromLua
public class ChatMessage implements Cloneable {
    private ChatBase chat;
    private LocalDateTime datetime;
    private String author;
    private String text;
    private boolean scramble;
    private String customTag;
    private Color textColor;
    private boolean customColor;
    private boolean overHeadSpeech = true;
    private boolean showInChat = true;
    private boolean fromDiscord;
    private boolean serverAlert;
    private int radioChannel = -1;
    private boolean local;
    private boolean shouldAttractZombies;
    private boolean serverAuthor;

    public ChatMessage(ChatBase chat, String text) {
        this(chat, LocalDateTime.now(), text);
    }

    public ChatMessage(ChatBase chat, LocalDateTime datetime, String text) {
        this.chat = chat;
        this.datetime = datetime;
        this.text = text;
        this.textColor = chat.getColor();
        this.customColor = false;
    }

    public boolean isShouldAttractZombies() {
        return this.shouldAttractZombies;
    }

    public void setShouldAttractZombies(boolean shouldAttractZombies) {
        this.shouldAttractZombies = shouldAttractZombies;
    }

    public boolean isLocal() {
        return this.local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getTextWithReplacedParentheses() {
        return this.text != null ? this.text.replaceAll("<", "&lt;").replaceAll(">", "&gt;") : null;
    }

    public void setScrambledText(String text) {
        this.scramble = true;
        this.text = text;
    }

    public int getRadioChannel() {
        return this.radioChannel;
    }

    public void setRadioChannel(int radioChannel) {
        this.radioChannel = radioChannel;
    }

    public boolean isServerAuthor() {
        return this.serverAuthor;
    }

    public void setServerAuthor(boolean serverAuthor) {
        this.serverAuthor = serverAuthor;
    }

    public boolean isFromDiscord() {
        return this.fromDiscord;
    }

    public void makeFromDiscord() {
        this.fromDiscord = true;
    }

    public boolean isOverHeadSpeech() {
        return this.overHeadSpeech;
    }

    public void setOverHeadSpeech(boolean overHeadSpeech) {
        this.overHeadSpeech = overHeadSpeech;
    }

    public boolean isShowInChat() {
        return this.showInChat;
    }

    public void setShowInChat(boolean showInChat) {
        this.showInChat = showInChat;
    }

    public LocalDateTime getDatetime() {
        return this.datetime;
    }

    public String getDatetimeStr() {
        return this.datetime.format(DateTimeFormatter.ofPattern("h:m"));
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }

    public boolean isShowAuthor() {
        return this.getChat().isShowAuthor();
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public ChatBase getChat() {
        return this.chat;
    }

    public int getChatID() {
        return this.chat.getID();
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTextWithPrefix() {
        return this.chat.getMessageTextWithPrefix(this);
    }

    public boolean isScramble() {
        return this.scramble;
    }

    public String getCustomTag() {
        return this.customTag;
    }

    public void setCustomTag(String customTag) {
        this.customTag = customTag;
    }

    public Color getTextColor() {
        return this.textColor;
    }

    public void setTextColor(Color textColor) {
        this.customColor = true;
        this.textColor = textColor;
    }

    public boolean isCustomColor() {
        return this.customColor;
    }

    public void pack(ByteBufferWriter b) {
        this.chat.packMessage(b, this);
    }

    public ChatMessage clone() {
        ChatMessage msg;
        try {
            msg = (ChatMessage)super.clone();
        } catch (CloneNotSupportedException var3) {
            throw new RuntimeException();
        }

        msg.datetime = this.datetime;
        msg.chat = this.chat;
        msg.author = this.author;
        msg.text = this.text;
        msg.scramble = this.scramble;
        msg.customTag = this.customTag;
        msg.textColor = this.textColor;
        msg.customColor = this.customColor;
        msg.overHeadSpeech = this.overHeadSpeech;
        return msg;
    }

    public boolean isServerAlert() {
        return this.serverAlert;
    }

    public void setServerAlert(boolean serverAlert) {
        this.serverAlert = serverAlert;
    }

    @Override
    public String toString() {
        return "ChatMessage{chat=" + this.chat.getTitle() + ", author='" + this.author + "', text='" + this.text + "'}";
    }
}
