// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.core.Color;
import zombie.network.chat.ChatType;

public class SafehouseChat extends ChatBase {
    public SafehouseChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.safehouse, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public SafehouseChat(int id, ChatTab tab) {
        super(id, ChatType.safehouse, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(Color.lightGreen);
        settings.setShowAuthor(true);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(true);
        return settings;
    }
}
