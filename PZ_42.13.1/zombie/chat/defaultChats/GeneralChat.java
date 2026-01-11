// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.chat.ChatUtility;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameServer;
import zombie.network.chat.ChatType;

public class GeneralChat extends ChatBase {
    private boolean discordEnabled;
    private final Color discordMessageColor = new Color(114, 137, 218);

    public GeneralChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.general, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public GeneralChat(int id, ChatTab tab, boolean discordEnabled) {
        super(id, ChatType.general, tab);
        this.discordEnabled = discordEnabled;
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }
    }

    public GeneralChat() {
        super(ChatType.general);
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(new Color(255, 165, 0));
        settings.setShowAuthor(true);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(true);
        settings.setAllowColors(true);
        settings.setAllowFonts(true);
        settings.setAllowBBcode(true);
        return settings;
    }

    @Override
    public void sendMessageToChatMembers(ChatMessage msg) {
        if (this.discordEnabled) {
            IsoPlayer author = ChatUtility.findPlayer(msg.getAuthor());
            if (msg.isFromDiscord()) {
                for (short playerID : this.members) {
                    this.sendMessageToPlayer(playerID, msg);
                }
            } else {
                GameServer.discordBot.sendMessage(msg.getAuthor(), msg.getText());

                for (short playerID : this.members) {
                    if (author == null || author.getOnlineID() != playerID) {
                        this.sendMessageToPlayer(playerID, msg);
                    }
                }
            }
        } else {
            super.sendMessageToChatMembers(msg);
        }
    }

    public void sendToDiscordGeneralChatDisabled() {
        GameServer.discordBot.sendMessage("Server", Translator.getText("UI_chat_general_chat_disabled"));
    }

    @Override
    public String getMessagePrefix(ChatMessage msg) {
        StringBuilder chatLine = new StringBuilder();
        if (msg.isFromDiscord()) {
            chatLine.append(this.getColorTag(this.discordMessageColor));
        } else {
            chatLine.append(this.getColorTag());
        }

        chatLine.append(" ").append(this.getFontSizeTag()).append(" ");
        if (this.isShowTimestamp()) {
            chatLine.append("[").append(LuaManager.getHourMinuteJava()).append("]");
        }

        if (this.isShowTitle()) {
            chatLine.append("[").append(this.getTitle()).append("]");
        }

        if (this.isShowAuthor()) {
            chatLine.append("[").append(msg.getAuthor()).append("]");
        }

        chatLine.append(": ");
        return chatLine.toString();
    }

    @Override
    public void packMessage(ByteBufferWriter b, ChatMessage msg) {
        super.packMessage(b, msg);
        b.putBoolean(msg.isFromDiscord());
    }

    @Override
    public ChatMessage unpackMessage(ByteBuffer bb) {
        ChatMessage msg = super.unpackMessage(bb);
        if (bb.get() == 1) {
            msg.makeFromDiscord();
        }

        return msg;
    }
}
