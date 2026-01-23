// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.core.Color;
import zombie.network.chat.ChatType;

public class ShoutChat extends RangeBasedChat {
    public ShoutChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.shout, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public ShoutChat(int id, ChatTab tab) {
        super(id, ChatType.shout, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public ShoutChat() {
        super(ChatType.shout);
        this.setSettings(getDefaultSettings());
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(new Color(255, 51, 51, 255));
        settings.setShowAuthor(true);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(true);
        settings.setAllowColors(false);
        settings.setAllowFonts(false);
        settings.setAllowBBcode(false);
        settings.setEqualizeLineHeights(true);
        settings.setRange(60.0F);
        return settings;
    }
}
