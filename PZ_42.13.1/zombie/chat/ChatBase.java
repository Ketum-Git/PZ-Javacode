// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.chat.ChatType;
import zombie.radio.devices.DeviceData;

@UsedFromLua
public abstract class ChatBase {
    private static final int ID_NOT_SET = -29048394;
    private int id;
    private final String titleId;
    private final ChatType type;
    private ChatSettings settings;
    private boolean customSettings;
    private ChatTab chatTab;
    private String translatedTitle;
    protected final ArrayList<Short> members;
    private final ArrayList<Short> justAddedMembers = new ArrayList<>();
    private final ArrayList<Short> justRemovedMembers = new ArrayList<>();
    protected final ArrayList<ChatMessage> messages;
    private UdpConnection serverConnection;
    private ChatMode mode;
    private IsoPlayer chatOwner;
    private final Lock memberLock = new ReentrantLock();

    protected ChatBase(ChatType type) {
        this.settings = new ChatSettings();
        this.customSettings = false;
        this.messages = new ArrayList<>();
        this.id = -29048394;
        this.titleId = type.getTitleID();
        this.type = type;
        this.members = new ArrayList<>();
        this.mode = ChatMode.SinglePlayer;
        this.serverConnection = null;
        this.chatOwner = IsoPlayer.getInstance();
    }

    /**
     * Should called only on client side of chat system
     * 
     * @param this package from server that describe how chat should look and work
     * @param bb meta information about chat. Many parameters depends on that
     * @param type tab where chat should show their info
     * @param tab actual player instance
     */
    public ChatBase(ByteBuffer bb, ChatType type, ChatTab tab, IsoPlayer owner) {
        this(type);
        this.id = bb.getInt();
        this.customSettings = bb.get() == 1;
        if (this.customSettings) {
            this.settings = new ChatSettings(bb);
        }

        this.chatTab = tab;
        this.mode = ChatMode.ClientMultiPlayer;
        this.serverConnection = GameClient.connection;
        this.chatOwner = owner;
    }

    /**
     * Should be called only on server side of chat system
     * 
     * @param this unique id of chat. It will be used to identify chat in client-server communication
     * @param id meta information about chat. Many parameters depends on that
     * @param type this tab will transferred to clients when it will connecting
     */
    public ChatBase(int id, ChatType type, ChatTab tab) {
        this(type);
        this.id = id;
        this.chatTab = tab;
        this.mode = ChatMode.ServerMultiPlayer;
    }

    public boolean isEnabled() {
        return ChatUtility.chatStreamEnabled(this.type);
    }

    protected String getChatOwnerName() {
        if (this.chatOwner == null) {
            if (this.mode != ChatMode.ServerMultiPlayer) {
                if (Core.debug) {
                    throw new NullPointerException("chat owner is null but name quired");
                }

                DebugLog.log("chat owner is null but name quired. Chat: " + this.getType());
            }

            return "";
        } else {
            return this.chatOwner.username;
        }
    }

    protected IsoPlayer getChatOwner() {
        if (this.chatOwner != null || this.mode == ChatMode.ServerMultiPlayer) {
            return this.chatOwner;
        } else if (Core.debug) {
            throw new NullPointerException("chat owner is null");
        } else {
            DebugLog.log("chat owner is null. Chat: " + this.getType());
            return null;
        }
    }

    public ChatMode getMode() {
        return this.mode;
    }

    public ChatType getType() {
        return this.type;
    }

    public int getID() {
        return this.id;
    }

    public String getTitleID() {
        return this.titleId;
    }

    public Color getColor() {
        return this.settings.getFontColor();
    }

    public short getTabID() {
        return this.chatTab.getID();
    }

    public float getRange() {
        return this.settings.getRange();
    }

    public boolean isSendingToRadio() {
        return false;
    }

    public float getZombieAttractionRange() {
        return this.settings.getZombieAttractionRange();
    }

    public void setSettings(ChatSettings settings) {
        this.settings = settings;
        this.customSettings = true;
    }

    public void setFontSize(String fontSize) {
        this.settings.setFontSize(fontSize.toLowerCase());
    }

    public void setShowTimestamp(boolean showTimestamp) {
        this.settings.setShowTimestamp(showTimestamp);
    }

    public void setShowTitle(boolean showTitle) {
        this.settings.setShowChatTitle(showTitle);
    }

    protected boolean isCustomSettings() {
        return this.customSettings;
    }

    protected boolean isAllowImages() {
        return this.settings.isAllowImages();
    }

    protected boolean isAllowChatIcons() {
        return this.settings.isAllowChatIcons();
    }

    protected boolean isAllowColors() {
        return this.settings.isAllowColors();
    }

    protected boolean isAllowFonts() {
        return this.settings.isAllowFonts();
    }

    protected boolean isAllowBBcode() {
        return this.settings.isAllowBBcode();
    }

    protected boolean isEqualizeLineHeights() {
        return this.settings.isEqualizeLineHeights();
    }

    protected boolean isShowAuthor() {
        return this.settings.isShowAuthor();
    }

    protected boolean isShowTimestamp() {
        return this.settings.isShowTimestamp();
    }

    protected boolean isShowTitle() {
        return this.settings.isShowChatTitle();
    }

    protected String getFontSize() {
        return this.settings.getFontSize();
    }

    protected String getTitle() {
        if (this.translatedTitle == null) {
            this.translatedTitle = Translator.getText(this.titleId);
        }

        return this.translatedTitle;
    }

    public void close() {
        synchronized (this.memberLock) {
            for (Short playerID : new ArrayList<>(this.members)) {
                this.leaveMember(playerID);
            }

            this.members.clear();
        }
    }

    protected void packChat(ByteBufferWriter b) {
        b.putInt(this.type.getValue());
        b.putShort(this.getTabID());
        b.putInt(this.id);
        b.putBoolean(this.customSettings);
        if (this.customSettings) {
            this.settings.pack(b);
        }
    }

    public ChatMessage unpackMessage(ByteBuffer bb) {
        String authorNickname = GameWindow.ReadString(bb);
        String text = GameWindow.ReadString(bb);
        ChatMessage message = this.createMessage(text);
        message.setAuthor(authorNickname);
        return message;
    }

    public void packMessage(ByteBufferWriter b, ChatMessage msg) {
        b.putInt(this.id);
        b.putUTF(msg.getAuthor());
        b.putUTF(msg.getText());
    }

    /**
     * Message creator. Every chat know how to create its own message
     * 
     * @param text text of the message
     * @return corresponding object to message
     */
    public ChatMessage createMessage(String text) {
        return this.createMessage(this.getChatOwnerName(), text);
    }

    private ChatMessage createMessage(String author, String text) {
        ChatMessage msg = new ChatMessage(this, text);
        msg.setAuthor(author);
        msg.setServerAuthor(false);
        return msg;
    }

    public ServerChatMessage createServerMessage(String text) {
        ServerChatMessage chatMessage = new ServerChatMessage(this, text);
        chatMessage.setServerAuthor(true);
        return chatMessage;
    }

    public void showMessage(String text, String author) {
        ChatMessage msg = new ChatMessage(this, LocalDateTime.now(), text);
        msg.setAuthor(author);
        this.showMessage(msg);
    }

    public void showMessage(ChatMessage msg) {
        this.messages.add(msg);
        if (this.isEnabled() && msg.isShowInChat() && this.chatTab != null) {
            LuaEventManager.triggerEvent("OnAddMessage", msg, this.getTabID());
        }
    }

    public String getMessageTextWithPrefix(ChatMessage msg) {
        return this.getMessagePrefix(msg) + " " + msg.getTextWithReplacedParentheses();
    }

    public void sendMessageToChatMembers(ChatMessage msg) {
        IsoPlayer msgAuthor = ChatUtility.findPlayer(msg.getAuthor());
        if (msgAuthor == null) {
            DebugLog.log("Author '" + msg.getAuthor() + "' not found");
        } else {
            synchronized (this.memberLock) {
                for (short playerID : this.members) {
                    IsoPlayer player = ChatUtility.findPlayer(playerID);
                    if (player != null && msgAuthor.getOnlineID() != playerID) {
                        this.sendMessageToPlayer(playerID, msg);
                    }
                }
            }
        }
    }

    public void sendMessageToChatMembers(ServerChatMessage msg) {
        synchronized (this.memberLock) {
            for (short playerID : this.members) {
                IsoPlayer player = ChatUtility.findPlayer(playerID);
                if (player != null) {
                    this.sendMessageToPlayer(playerID, msg);
                }
            }
        }
    }

    public void sendMessageToPlayer(UdpConnection connection, ChatMessage msg) {
        synchronized (this.memberLock) {
            boolean memberFound = false;
            short[] var5 = connection.playerIds;
            int var6 = var5.length;

            for (int var7 = 0; var7 < var6; var7++) {
                Short playerIDOnConnection = var5[var7];
                if (memberFound) {
                    break;
                }

                memberFound = this.members.contains(playerIDOnConnection);
            }

            if (!memberFound) {
                throw new RuntimeException("Passed connection didn't contained member of chat");
            } else {
                this.sendChatMessageToPlayer(connection, msg);
            }
        }
    }

    public void sendMessageToPlayer(short playerID, ChatMessage msg) {
        UdpConnection connection = ChatUtility.findConnection(playerID);
        if (connection != null) {
            this.sendChatMessageToPlayer(connection, msg);
        }
    }

    public String getMessagePrefix(ChatMessage msg) {
        StringBuilder chatLine = new StringBuilder(this.getChatSettingsTags());
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

    protected String getColorTag() {
        Color color = this.getColor();
        return this.getColorTag(color);
    }

    protected String getColorTag(Color color) {
        return "<RGB:" + color.r + "," + color.g + "," + color.b + ">";
    }

    protected String getFontSizeTag() {
        return "<SIZE:" + this.settings.getFontSize() + ">";
    }

    protected String getChatSettingsTags() {
        return this.getColorTag() + " " + this.getFontSizeTag() + " ";
    }

    public void addMember(short playerID) {
        synchronized (this.memberLock) {
            if (!this.hasMember(playerID)) {
                this.members.add(playerID);
                this.justAddedMembers.add(playerID);
                UdpConnection connection = ChatUtility.findConnection(playerID);
                if (connection != null) {
                    this.sendPlayerJoinChatPacket(connection);
                    this.chatTab.sendAddTabPacket(connection);
                } else if (Core.debug) {
                    throw new RuntimeException("Connection should exist!");
                }
            }
        }
    }

    public void leaveMember(Short playerID) {
        synchronized (this.memberLock) {
            if (this.hasMember(playerID)) {
                this.justRemovedMembers.add(playerID);
                UdpConnection connection = ChatUtility.findConnection(playerID);
                if (connection != null) {
                    this.sendPlayerLeaveChatPacket(connection);
                }

                this.members.remove(playerID);
            }
        }
    }

    private boolean hasMember(Short playerID) {
        return this.members.contains(playerID);
    }

    public void removeMember(Short playerID) {
        synchronized (this.memberLock) {
            if (this.hasMember(playerID)) {
                this.members.remove(playerID);
            }
        }
    }

    public void syncMembersByUsernames(ArrayList<String> players) {
        synchronized (this.memberLock) {
            this.justAddedMembers.clear();
            this.justRemovedMembers.clear();
            ArrayList<Short> actualMembers = new ArrayList<>(players.size());
            IsoPlayer currentPlayer = null;

            for (String player : players) {
                currentPlayer = ChatUtility.findPlayer(player);
                if (currentPlayer != null) {
                    actualMembers.add(currentPlayer.getOnlineID());
                }
            }

            this.syncMembers(actualMembers);
        }
    }

    public ArrayList<Short> getJustAddedMembers() {
        synchronized (this.memberLock) {
            return this.justAddedMembers;
        }
    }

    public ArrayList<Short> getJustRemovedMembers() {
        synchronized (this.memberLock) {
            return this.justRemovedMembers;
        }
    }

    private void syncMembers(ArrayList<Short> actualMembers) {
        for (Short id : actualMembers) {
            this.addMember(id);
        }

        ArrayList<Short> toDelete = new ArrayList<>();
        synchronized (this.memberLock) {
            for (Short memberID : this.members) {
                if (!actualMembers.contains(memberID)) {
                    toDelete.add(memberID);
                }
            }

            for (Short id : toDelete) {
                this.leaveMember(id);
            }
        }
    }

    public void sendPlayerJoinChatPacket(UdpConnection playerConnection) {
        ByteBufferWriter b = playerConnection.startPacket();
        PacketTypes.PacketType.PlayerJoinChat.doPacket(b);
        this.packChat(b);
        PacketTypes.PacketType.PlayerJoinChat.send(playerConnection);
    }

    public void sendPlayerLeaveChatPacket(short playerID) {
        UdpConnection connection = ChatUtility.findConnection(playerID);
        this.sendPlayerLeaveChatPacket(connection);
    }

    public void sendPlayerLeaveChatPacket(UdpConnection connection) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.PlayerLeaveChat.doPacket(b);
        b.putInt(this.getID());
        b.putInt(this.getType().getValue());
        PacketTypes.PacketType.PlayerLeaveChat.send(connection);
    }

    public void sendToServer(ChatMessage msg, DeviceData deviceData) {
        if (this.serverConnection == null) {
            DebugLog.log("Connection to server is null in client chat");
        }

        this.sendChatMessageFromPlayer(this.serverConnection, msg);
    }

    private void sendChatMessageToPlayer(UdpConnection connection, ChatMessage msg) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ChatMessageToPlayer.doPacket(b);
        this.packMessage(b, msg);
        PacketTypes.PacketType.ChatMessageToPlayer.send(connection);
    }

    private void sendChatMessageFromPlayer(UdpConnection connection, ChatMessage msg) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ChatMessageFromPlayer.doPacket(b);
        this.packMessage(b, msg);
        PacketTypes.PacketType.ChatMessageFromPlayer.send(connection);
    }

    protected boolean hasChatTab() {
        return this.chatTab != null;
    }
}
