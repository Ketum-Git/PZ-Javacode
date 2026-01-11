// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.core.Color;
import zombie.network.chat.ChatType;

public class FactionChat extends ChatBase {
    public FactionChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.faction, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public FactionChat(int id, ChatTab tab) {
        super(id, ChatType.faction, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(Color.darkGreen);
        settings.setShowAuthor(true);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(false);
        return settings;
    }
}
