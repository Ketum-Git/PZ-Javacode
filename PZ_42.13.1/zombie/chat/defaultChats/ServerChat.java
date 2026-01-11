// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatManager;
import zombie.chat.ChatMessage;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.chat.ServerChatMessage;
import zombie.core.Color;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.chat.ChatType;

public class ServerChat extends ChatBase {
    public ServerChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.server, tab, owner);
        this.setSettings(getDefaultSettings());
    }

    public ServerChat(int id, ChatTab tab) {
        super(id, ChatType.server, tab);
        this.setSettings(getDefaultSettings());
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(new Color(0, 128, 255, 255));
        settings.setShowAuthor(false);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(false);
        settings.setAllowColors(true);
        settings.setAllowFonts(false);
        settings.setAllowBBcode(false);
        return settings;
    }

    public ChatMessage createMessage(String author, String text, boolean isAlert) {
        ChatMessage msg = this.createMessage(text);
        msg.setAuthor(author);
        if (isAlert) {
            msg.setServerAlert(true);
        }

        return msg;
    }

    public ServerChatMessage createServerMessage(String text, boolean isAlert) {
        ServerChatMessage chatMessage = this.createServerMessage(text);
        chatMessage.setServerAlert(isAlert);
        return chatMessage;
    }

    @Override
    public short getTabID() {
        return !GameClient.client ? super.getTabID() : ChatManager.getInstance().getFocusTab().getID();
    }

    @Override
    public ChatMessage unpackMessage(ByteBuffer bb) {
        ChatMessage msg = super.unpackMessage(bb);
        msg.setServerAlert(bb.get() == 1);
        msg.setServerAuthor(bb.get() == 1);
        return msg;
    }

    @Override
    public void packMessage(ByteBufferWriter b, ChatMessage msg) {
        super.packMessage(b, msg);
        b.putBoolean(msg.isServerAlert());
        b.putBoolean(msg.isServerAuthor());
    }

    @Override
    public String getMessagePrefix(ChatMessage msg) {
        StringBuilder chatLine = new StringBuilder();
        chatLine.append(this.getChatSettingsTags());
        boolean prefixAdded = false;
        if (this.isShowTitle()) {
            chatLine.append("[").append(this.getTitle()).append("]");
            prefixAdded = true;
        }

        if (!msg.isServerAuthor() && this.isShowAuthor()) {
            chatLine.append("[").append(msg.getAuthor()).append("]");
            prefixAdded = true;
        }

        if (prefixAdded) {
            chatLine.append(": ");
        }

        return chatLine.toString();
    }

    @Override
    public String getMessageTextWithPrefix(ChatMessage msg) {
        return this.getMessagePrefix(msg) + " " + msg.getText();
    }

    @Override
    public void showMessage(ChatMessage msg) {
        this.messages.add(msg);
        if (this.isEnabled()) {
            LuaEventManager.triggerEvent("OnAddMessage", msg, this.getTabID());
            if (msg.isServerAlert()) {
                LuaEventManager.triggerEvent("OnAlertMessage", msg, this.getTabID());
            }
        }
    }

    @Override
    public void sendMessageToChatMembers(ChatMessage msg) {
        for (short playerID : this.members) {
            this.sendMessageToPlayer(playerID, msg);
        }
    }
}
