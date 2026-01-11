// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.chat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;
import zombie.chat.ChatTab;
import zombie.chat.ChatUtility;
import zombie.chat.ServerChatMessage;
import zombie.chat.defaultChats.AdminChat;
import zombie.chat.defaultChats.FactionChat;
import zombie.chat.defaultChats.GeneralChat;
import zombie.chat.defaultChats.RadioChat;
import zombie.chat.defaultChats.SafehouseChat;
import zombie.chat.defaultChats.SayChat;
import zombie.chat.defaultChats.ServerChat;
import zombie.chat.defaultChats.ShoutChat;
import zombie.chat.defaultChats.WhisperChat;
import zombie.core.Core;
import zombie.core.WordsFilter;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheat;

public class ChatServer {
    private static ChatServer instance;
    private static final Stack<Integer> availableChatsID = new Stack<>();
    private static int lastChatId = -1;
    private static final HashMap<ChatType, ChatBase> defaultChats = new HashMap<>();
    private static final ConcurrentHashMap<Integer, ChatBase> chats = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FactionChat> factionChats = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SafehouseChat> safehouseChats = new ConcurrentHashMap<>();
    private static AdminChat adminChat;
    private static GeneralChat generalChat;
    private static ServerChat serverChat;
    private static RadioChat radioChat;
    private static boolean inited;
    private static final HashSet<Short> players = new HashSet<>();
    private static final String logName = "chat";
    private static ZLogger logger;
    private static final HashMap<String, ChatTab> tabs = new HashMap<>();
    private static final String mainTabID = "main";
    private static final String adminTabID = "admin";

    public static ChatServer getInstance() {
        if (instance == null) {
            instance = new ChatServer();
        }

        return instance;
    }

    public static boolean isInited() {
        return inited;
    }

    private ChatServer() {
    }

    public void init() {
        if (!inited) {
            LoggerManager.createLogger("chat", Core.debug);
            logger = LoggerManager.getLogger("chat");
            logger.write("Start chat server initialization...", "info");
            ChatTab defaultTab = new ChatTab((short)0, "UI_chat_main_tab_title_id");
            ChatTab adminChatsTab = new ChatTab((short)1, "UI_chat_admin_tab_title_id");
            boolean discordEnabled = ServerOptions.getInstance().discordEnable.getValue();
            GeneralChat general = new GeneralChat(this.getNextChatID(), defaultTab, discordEnabled);
            SayChat say = new SayChat(this.getNextChatID(), defaultTab);
            ShoutChat shout = new ShoutChat(this.getNextChatID(), defaultTab);
            RadioChat radio = new RadioChat(this.getNextChatID(), defaultTab);
            AdminChat admin = new AdminChat(this.getNextChatID(), adminChatsTab);
            ServerChat server = new ServerChat(this.getNextChatID(), defaultTab);
            chats.put(general.getID(), general);
            chats.put(say.getID(), say);
            chats.put(shout.getID(), shout);
            chats.put(radio.getID(), radio);
            chats.put(admin.getID(), admin);
            chats.put(server.getID(), server);
            defaultChats.put(general.getType(), general);
            defaultChats.put(say.getType(), say);
            defaultChats.put(shout.getType(), shout);
            defaultChats.put(server.getType(), server);
            defaultChats.put(radio.getType(), radio);
            tabs.put("main", defaultTab);
            tabs.put("admin", adminChatsTab);
            generalChat = general;
            adminChat = admin;
            serverChat = server;
            radioChat = radio;
            inited = true;
            logger.write("General chat has id = " + general.getID(), "info");
            logger.write("Say chat has id = " + say.getID(), "info");
            logger.write("Shout chat has id = " + shout.getID(), "info");
            logger.write("Radio chat has id = " + radio.getID(), "info");
            logger.write("Admin chat has id = " + admin.getID(), "info");
            logger.write("Server chat has id = " + serverChat.getID(), "info");
            logger.write("Chat server successfully initialized", "info");
        }
    }

    public void initPlayer(short playerID) {
        logger.write("Player with id = '" + playerID + "' tries to connect", "info");
        synchronized (players) {
            if (players.contains(playerID)) {
                logger.write("Player already connected!", "warning");
                return;
            }
        }

        logger.write("Adding player '" + playerID + "' to chat server", "info");
        IsoPlayer player = ChatUtility.findPlayer(playerID);
        UdpConnection connection = ChatUtility.findConnection(playerID);
        if (connection != null && player != null) {
            this.sendInitPlayerChatPacket(connection);
            this.addDefaultChats(playerID);
            logger.write("Player joined to default chats", "info");
            if (connection.role.hasCapability(Capability.AdminChat)) {
                this.joinAdminChat(playerID);
            }

            Faction faction = Faction.getPlayerFaction(player);
            if (faction != null) {
                this.addMemberToFactionChat(faction.getName(), playerID);
            }

            SafeHouse safeHouse = SafeHouse.hasSafehouse(player);
            if (safeHouse != null) {
                this.addMemberToSafehouseChat(safeHouse.getId(), playerID);
            }

            ByteBufferWriter bb = connection.startPacket();
            PacketTypes.PacketType.PlayerConnectedToChat.doPacket(bb);
            PacketTypes.PacketType.PlayerConnectedToChat.send(connection);
            synchronized (players) {
                players.add(playerID);
            }

            DebugLog.DetailedInfo.trace("Player " + player.getUsername() + "(" + playerID + ") joined to chat server successfully", "info");
            logger.write("Player " + player.getOnlineID() + "(" + playerID + ") joined to chat server successfully", "info");
        } else {
            logger.write("Player or connection is not found on server!", "error");
            logger.write((connection == null ? "connection = null " : "") + (player == null ? "player = null" : ""), "error");
        }
    }

    public void processMessageFromPlayerPacket(ByteBuffer bb, UdpConnection connection) {
        int id = bb.getInt();
        synchronized (chats) {
            ChatBase chat = chats.get(id);
            ChatMessage message = chat.unpackMessage(bb);
            logger.write("Got message:" + message, "info");
            if (!ChatUtility.chatStreamEnabled(chat.getType())) {
                logger.write("Message ignored by server because the chat disabled by server settings", "warning");
            } else {
                List<WordsFilter.SearchResult> result = WordsFilter.getInstance().searchText(message.getText());
                if (!result.isEmpty()) {
                    switch (ServerOptions.getInstance().badWordPolicy.getValue()) {
                        case 1:
                            AntiCheat.doBanUser(connection, ServerOptions.getInstance().badWordPolicy.getName(), message.getText());
                            break;
                        case 2:
                            AntiCheat.doKickUser(connection, ServerOptions.getInstance().badWordPolicy.getName(), message.getText());
                            break;
                        case 3:
                            AntiCheat.doLogUser(connection, ServerOptions.getInstance().badWordPolicy.getName(), message.getText());
                            break;
                        case 4:
                            IsoPlayer msgAuthor = ChatUtility.findPlayer(message.getAuthor());
                            if (msgAuthor == null) {
                                msgAuthor.setAllChatMuted(true);
                                return;
                            }
                    }
                }

                message.setText(WordsFilter.getInstance().hideBadWords(message.getText(), result, ServerOptions.getInstance().badWordReplacement.getValue()));
                this.sendMessage(message);
                logger.write("Message " + message + " sent to chat (id = " + chat.getID() + ") members", "info");
            }
        }
    }

    public void processPlayerStartWhisperChatPacket(ByteBuffer bb) {
        logger.write("Whisper chat starting...", "info");
        if (!ChatUtility.chatStreamEnabled(ChatType.whisper)) {
            logger.write("Message for whisper chat is ignored because whisper chat is disabled by server settings", "info");
        } else {
            String authorName = GameWindow.ReadString(bb);
            String destPlayerName = GameWindow.ReadString(bb);
            logger.write("Player '" + authorName + "' attempt to start whispering with '" + destPlayerName + "'", "info");
            IsoPlayer player1 = ChatUtility.findPlayer(authorName);
            IsoPlayer player2 = ChatUtility.findPlayer(destPlayerName);
            if (player1 == null) {
                logger.write("Player '" + authorName + "' is not found!", "error");
                throw new RuntimeException("Player not found");
            } else if (player2 == null) {
                logger.write("Player '" + authorName + "' attempt to start whisper dialog with '" + destPlayerName + "' but this player not found!", "info");
                UdpConnection connection = ChatUtility.findConnection(player1.getOnlineID());
                this.sendPlayerNotFoundMessage(connection, destPlayerName);
            } else {
                logger.write("Both players found", "info");
                WhisperChat newPMChat = new WhisperChat(this.getNextChatID(), tabs.get("main"), authorName, destPlayerName);
                newPMChat.addMember(player1.getOnlineID());
                newPMChat.addMember(player2.getOnlineID());
                chats.put(newPMChat.getID(), newPMChat);
                DebugLog.DetailedInfo
                    .trace(
                        "Whisper chat (id = " + newPMChat.getID() + ") between '" + player1.getUsername() + "' and '" + player2.getUsername() + "' started",
                        "info"
                    );
                logger.write(
                    "Whisper chat (id = " + newPMChat.getID() + ") between '" + player1.getOnlineID() + "' and '" + player2.getOnlineID() + "' started", "info"
                );
            }
        }
    }

    private void sendPlayerNotFoundMessage(UdpConnection connection, String destPlayerName) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.PlayerNotFound.doPacket(b);
        b.putUTF(destPlayerName);
        PacketTypes.PacketType.PlayerNotFound.send(connection);
        logger.write("'Player not found' packet was sent", "info");
    }

    public ChatMessage unpackChatMessage(ByteBuffer bb) {
        int id = bb.getInt();
        return chats.get(id).unpackMessage(bb);
    }

    public void disconnectPlayer(short playerID) {
        logger.write("Player " + playerID + " disconnecting...", "info");
        synchronized (chats) {
            for (ChatBase chat : chats.values()) {
                chat.removeMember(playerID);
                if (chat.getType() == ChatType.whisper) {
                    this.closeChat(chat.getID());
                }
            }
        }

        synchronized (players) {
            players.remove(playerID);
        }

        logger.write("Disconnecting player " + playerID + " finished", "info");
    }

    private void closeChat(int chatID) {
        synchronized (chats) {
            if (!chats.containsKey(chatID)) {
                throw new RuntimeException("Chat '" + chatID + "' requested to close but it's not exists.");
            }

            ChatBase chat = chats.get(chatID);
            chat.close();
            chats.remove(chatID);
        }

        synchronized (availableChatsID) {
            availableChatsID.push(chatID);
        }
    }

    public void joinAdminChat(short playerID) {
        if (adminChat == null) {
            logger.write("Admin chat is null! Can't add player to it", "warning");
        } else {
            adminChat.addMember(playerID);
            logger.write("Player joined admin chat", "info");
        }
    }

    public void leaveAdminChat(short playerID) {
        logger.write("Player " + playerID + " are leaving admin chat...", "info");
        UdpConnection connection = ChatUtility.findConnection(playerID);
        if (adminChat == null) {
            logger.write("Admin chat is null. Can't leave it! ChatServer", "warning");
        } else if (connection == null) {
            logger.write("Connection to player is null. Can't leave admin chat! ChatServer.leaveAdminChat", "warning");
        } else {
            adminChat.leaveMember(playerID);
            tabs.get("admin").sendRemoveTabPacket(connection);
            logger.write("Player " + playerID + " leaved admin chat", "info");
        }
    }

    public FactionChat createFactionChat(String name) {
        logger.write("Creating faction chat '" + name + "'", "info");
        if (factionChats.containsKey(name)) {
            logger.write("Faction chat '" + name + "' already exists!", "warning");
            return factionChats.get(name);
        } else {
            FactionChat chat = new FactionChat(this.getNextChatID(), tabs.get("main"));
            chats.put(chat.getID(), chat);
            factionChats.put(name, chat);
            logger.write("Faction chat '" + name + "' created", "info");
            return chat;
        }
    }

    public SafehouseChat createSafehouseChat(String safehouseID) {
        logger.write("Creating safehouse chat '" + safehouseID + "'", "info");
        if (safehouseChats.containsKey(safehouseID)) {
            logger.write("Safehouse chat already has chat with name '" + safehouseID + "'", "warning");
            return safehouseChats.get(safehouseID);
        } else {
            SafehouseChat chat = new SafehouseChat(this.getNextChatID(), tabs.get("main"));
            chats.put(chat.getID(), chat);
            safehouseChats.put(safehouseID, chat);
            logger.write("Safehouse chat '" + safehouseID + "' created", "info");
            return chat;
        }
    }

    public void removeFactionChat(String factionName) {
        logger.write("Removing faction chat '" + factionName + "'...", "info");
        int id;
        synchronized (factionChats) {
            if (!factionChats.containsKey(factionName)) {
                String msg = "Faction chat '" + factionName + "' tried to delete but it's not exists.";
                logger.write(msg, "error");
                RuntimeException ex = new RuntimeException(msg);
                logger.write(ex);
                throw ex;
            }

            FactionChat chat = factionChats.get(factionName);
            id = chat.getID();
            factionChats.remove(factionName);
        }

        this.closeChat(id);
        logger.write("Faction chat '" + factionName + "' removed", "info");
    }

    public void removeSafehouseChat(String safehouseName) {
        logger.write("Removing safehouse chat '" + safehouseName + "'...", "info");
        int id;
        synchronized (safehouseChats) {
            if (!safehouseChats.containsKey(safehouseName)) {
                String msg = "Safehouse chat '" + safehouseName + "' tried to delete but it's not exists.";
                logger.write(msg, "error");
                RuntimeException ex = new RuntimeException(msg);
                logger.write(ex);
                throw ex;
            }

            SafehouseChat chat = safehouseChats.get(safehouseName);
            id = chat.getID();
            safehouseChats.remove(safehouseName);
        }

        this.closeChat(id);
        logger.write("Safehouse chat '" + safehouseName + "' removed", "info");
    }

    public void syncFactionChatMembers(String factionName, String factionOwner, ArrayList<String> players) {
        logger.write("Start syncing faction chat '" + factionName + "'...", "info");
        if (factionName != null && factionOwner != null && players != null) {
            synchronized (factionChats) {
                if (!factionChats.containsKey(factionName)) {
                    logger.write("Faction chat '" + factionName + "' is not exist", "warning");
                    return;
                }

                ArrayList<String> actualMembers = new ArrayList<>(players);
                actualMembers.add(factionOwner);
                FactionChat chat = factionChats.get(factionName);
                chat.syncMembersByUsernames(actualMembers);
                StringBuilder sb = new StringBuilder("These members were added: ");

                for (int playerID : chat.getJustAddedMembers()) {
                    sb.append("'").append(ChatUtility.findPlayerName(playerID)).append("', ");
                }

                sb.append(". These members were removed: ");

                for (int playerID : chat.getJustRemovedMembers()) {
                    sb.append("'").append(ChatUtility.findPlayerName(playerID)).append("', ");
                }

                logger.write(sb.toString(), "info");
            }

            logger.write("Syncing faction chat '" + factionName + "' finished", "info");
        } else {
            logger.write("Faction name or faction owner or players is null", "warning");
        }
    }

    public void syncSafehouseChatMembers(String safehouseID, String safehouseOwner, ArrayList<String> players) {
        logger.write("Start syncing safehouse chat '" + safehouseID + "'...", "info");
        if (safehouseID != null && safehouseOwner != null && players != null) {
            synchronized (safehouseChats) {
                if (!safehouseChats.containsKey(safehouseID)) {
                    logger.write("Safehouse chat '" + safehouseID + "' is not exist", "warning");
                    return;
                }

                ArrayList<String> actualMembers = new ArrayList<>(players);
                actualMembers.add(safehouseOwner);
                SafehouseChat chat = safehouseChats.get(safehouseID);
                chat.syncMembersByUsernames(actualMembers);
                StringBuilder sb = new StringBuilder("These members were added: ");

                for (int playerID : chat.getJustAddedMembers()) {
                    sb.append("'").append(ChatUtility.findPlayerName(playerID)).append("', ");
                }

                sb.append("These members were removed: ");

                for (int playerID : chat.getJustRemovedMembers()) {
                    sb.append("'").append(ChatUtility.findPlayerName(playerID)).append("', ");
                }

                logger.write(sb.toString(), "info");
            }

            logger.write("Syncing safehouse chat '" + safehouseID + "' finished", "info");
        } else {
            logger.write("Safehouse name or Safehouse owner or players is null", "warning");
        }
    }

    private void addMemberToSafehouseChat(String safehouseID, short playerID) {
        if (!safehouseChats.containsKey(safehouseID)) {
            logger.write("Safehouse chat is not initialized!", "warning");
        } else {
            synchronized (safehouseChats) {
                SafehouseChat chat = safehouseChats.get(safehouseID);
                chat.addMember(playerID);
            }

            logger.write("Player joined to chat of safehouse '" + safehouseID + "'", "info");
        }
    }

    private void addMemberToFactionChat(String factionName, short playerID) {
        if (!factionChats.containsKey(factionName)) {
            logger.write("Faction chat is not initialized!", "warning");
        } else {
            synchronized (factionChats) {
                FactionChat chat = factionChats.get(factionName);
                chat.addMember(playerID);
            }

            logger.write("Player joined to chat of faction '" + factionName + "'", "info");
        }
    }

    public void sendServerAlertMessageToServerChat(String author, String msg) {
        serverChat.sendMessageToChatMembers(serverChat.createMessage(author, msg, true));
        logger.write("Server alert message: '" + msg + "' by '" + author + "' sent.");
    }

    public void sendServerAlertMessageToServerChat(String msg) {
        serverChat.sendMessageToChatMembers(serverChat.createServerMessage(msg, true));
        logger.write("Server alert message: '" + msg + "' sent.");
    }

    public ChatMessage createRadiostationMessage(String text, int radioChannel) {
        return radioChat.createBroadcastingMessage(text, radioChannel);
    }

    public void sendMessageToServerChat(UdpConnection connection, String msg) {
        ServerChatMessage chatMessage = serverChat.createServerMessage(msg, false);
        serverChat.sendMessageToPlayer(connection, chatMessage);
    }

    public void sendMessageToServerChat(String msg) {
        ServerChatMessage chatMessage = serverChat.createServerMessage(msg, false);
        serverChat.sendMessageToChatMembers(chatMessage);
    }

    public void sendMessageFromDiscordToGeneralChat(String author, String msg) {
        if (author != null && msg != null) {
            logger.write("Got message '" + msg + "' by author '" + author + "' from discord");
        }

        ChatMessage chatMessage = generalChat.createMessage(msg);
        chatMessage.makeFromDiscord();
        chatMessage.setAuthor(author);
        if (ChatUtility.chatStreamEnabled(ChatType.general)) {
            this.sendMessage(chatMessage);
            logger.write("Message '" + msg + "' send from discord to general chat members");
        } else {
            generalChat.sendToDiscordGeneralChatDisabled();
            logger.write("General chat disabled so error message sent to discord", "warning");
        }
    }

    private int getNextChatID() {
        synchronized (availableChatsID) {
            if (availableChatsID.isEmpty()) {
                lastChatId++;
                availableChatsID.push(lastChatId);
            }

            return availableChatsID.pop();
        }
    }

    private void sendMessage(ChatMessage msg) {
        synchronized (chats) {
            if (chats.containsKey(msg.getChatID())) {
                ChatBase chat = chats.get(msg.getChatID());
                chat.sendMessageToChatMembers(msg);
            }
        }
    }

    private void sendInitPlayerChatPacket(UdpConnection connection) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.InitPlayerChat.doPacket(b);
        b.putShort((short)tabs.size());

        for (ChatTab tab : tabs.values()) {
            b.putShort(tab.getID());
            b.putUTF(tab.getTitleID());
        }

        PacketTypes.PacketType.InitPlayerChat.send(connection);
    }

    private void addDefaultChats(short playerID) {
        for (Entry<ChatType, ChatBase> entry : defaultChats.entrySet()) {
            ChatBase chat = entry.getValue();
            chat.addMember(playerID);
        }
    }

    public void sendMessageToAdminChat(String msg) {
        ServerChatMessage chatMessage = adminChat.createServerMessage(msg);
        adminChat.sendMessageToChatMembers(chatMessage);
    }
}
