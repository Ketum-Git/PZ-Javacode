// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatMessage;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.chat.ChatUtility;
import zombie.core.Color;
import zombie.network.chat.ChatType;

public class SayChat extends RangeBasedChat {
    public SayChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.say, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public SayChat(int id, ChatTab tab) {
        super(id, ChatType.say, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public SayChat() {
        super(ChatType.say);
        this.setSettings(getDefaultSettings());
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(Color.white);
        settings.setShowAuthor(true);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(true);
        settings.setAllowColors(true);
        settings.setAllowChatIcons(true);
        settings.setAllowImages(true);
        settings.setAllowFonts(false);
        settings.setAllowBBcode(true);
        settings.setEqualizeLineHeights(true);
        settings.setRange(30.0F);
        settings.setZombieAttractionRange(15.0F);
        return settings;
    }

    public ChatMessage createInfoMessage(String text) {
        ChatMessage msg = this.createBubbleMessage(text);
        msg.setLocal(true);
        msg.setShowInChat(false);
        return msg;
    }

    public ChatMessage createCalloutMessage(String text) {
        ChatMessage msg = this.createBubbleMessage(text);
        msg.setLocal(false);
        msg.setShouldAttractZombies(true);
        return msg;
    }

    @Override
    public String getMessageTextWithPrefix(ChatMessage msg) {
        return this.getMessagePrefix(msg) + " " + ChatUtility.parseStringForChatLog(msg.getTextWithReplacedParentheses());
    }
}
