// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.core.Color;
import zombie.network.chat.ChatType;

public class AdminChat extends ChatBase {
    public AdminChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.admin, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public AdminChat(int id, ChatTab tab) {
        super(id, ChatType.admin, tab);
        this.setSettings(getDefaultSettings());
    }

    public AdminChat() {
        super(ChatType.admin);
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
        settings.setAllowFonts(true);
        settings.setAllowBBcode(true);
        return settings;
    }
}
