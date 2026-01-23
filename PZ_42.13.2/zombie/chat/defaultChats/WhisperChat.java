// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.network.chat.ChatType;

public class WhisperChat extends ChatBase {
    private String myName;
    private String companionName;
    private final String player1;
    private final String player2;
    private boolean isInited;

    public WhisperChat(int id, ChatTab tab, String firstMember, String secondMember) {
        super(id, ChatType.whisper, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }

        this.player1 = firstMember;
        this.player2 = secondMember;
    }

    public WhisperChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.whisper, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }

        this.player1 = GameWindow.ReadString(bb);
        this.player2 = GameWindow.ReadString(bb);
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(new Color(85, 26, 139));
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
    public String getMessagePrefix(ChatMessage msg) {
        if (!this.isInited) {
            this.init();
        }

        StringBuilder chatLine = new StringBuilder(this.getChatSettingsTags());
        if (this.isShowTimestamp()) {
            chatLine.append("[").append(LuaManager.getHourMinuteJava()).append("]");
        }

        if (this.isShowTitle()) {
            chatLine.append("[").append(this.getTitle()).append("]");
        }

        if (!this.myName.equalsIgnoreCase(msg.getAuthor())) {
            chatLine.append("[").append(this.companionName).append("]");
        } else {
            chatLine.append("[to ").append(this.companionName).append("]");
        }

        chatLine.append(": ");
        return chatLine.toString();
    }

    @Override
    protected void packChat(ByteBufferWriter b) {
        super.packChat(b);
        b.putUTF(this.player1);
        b.putUTF(this.player2);
    }

    public String getCompanionName() {
        return this.companionName;
    }

    public void init() {
        if (this.player1.equals(IsoPlayer.getInstance().getUsername())) {
            this.myName = IsoPlayer.getInstance().getUsername();
            this.companionName = this.player2;
        } else {
            if (!this.player2.equals(IsoPlayer.getInstance().getUsername())) {
                if (Core.debug) {
                    throw new RuntimeException("Wrong id");
                }

                DebugLog.DetailedInfo.trace("Wrong id in whisper chat. Whisper chat not inited for players: " + this.player1 + " " + this.player2);
                return;
            }

            this.myName = IsoPlayer.getInstance().getUsername();
            this.companionName = this.player1;
        }

        this.isInited = true;
    }

    public static enum ChatStatus {
        None,
        Creating,
        PlayerNotFound;
    }
}
