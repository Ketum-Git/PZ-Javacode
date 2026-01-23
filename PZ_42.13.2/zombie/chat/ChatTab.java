// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.util.HashSet;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketTypes;

public class ChatTab {
    private final short id;
    private final String titleId;
    private final String translatedTitle;
    private final HashSet<Integer> containedChats;
    private boolean enabled;

    public ChatTab(short tabID, String titleId) {
        this.id = tabID;
        this.titleId = titleId;
        this.translatedTitle = Translator.getText(titleId);
        this.containedChats = new HashSet<>();
    }

    public ChatTab(short tabID, String titleId, int chatID) {
        this(tabID, titleId);
        this.containedChats.add(chatID);
    }

    public void RemoveChat(int chatID) {
        if (!this.containedChats.contains(chatID)) {
            throw new RuntimeException("Tab '" + this.id + "' doesn't contains a chat id: " + chatID);
        } else {
            this.containedChats.remove(chatID);
        }
    }

    public String getTitleID() {
        return this.titleId;
    }

    public String getTitle() {
        return this.translatedTitle;
    }

    public short getID() {
        return this.id;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void sendAddTabPacket(UdpConnection connection) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.AddChatTab.doPacket(b);
        b.putShort(this.getID());
        PacketTypes.PacketType.AddChatTab.send(connection);
    }

    public void sendRemoveTabPacket(UdpConnection connection) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.RemoveChatTab.doPacket(b);
        b.putShort(this.getID());
        PacketTypes.PacketType.RemoveChatTab.send(connection);
    }
}
