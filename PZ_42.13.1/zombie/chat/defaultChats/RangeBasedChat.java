// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import java.util.HashMap;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatElement;
import zombie.chat.ChatMessage;
import zombie.chat.ChatMode;
import zombie.chat.ChatTab;
import zombie.chat.ChatUtility;
import zombie.core.Color;
import zombie.core.fonts.AngelCodeFont;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.chat.ChatType;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public abstract class RangeBasedChat extends ChatBase {
    private static ChatElement overHeadChat;
    private static final HashMap<String, IsoPlayer> players = new HashMap<>();
    private static String currentPlayerName;
    String customTag = "default";

    RangeBasedChat(ByteBuffer bb, ChatType type, ChatTab tab, IsoPlayer owner) {
        super(bb, type, tab, owner);
    }

    RangeBasedChat(ChatType type) {
        super(type);
    }

    RangeBasedChat(int id, ChatType type, ChatTab tab) {
        super(id, type, tab);
    }

    public void Init() {
        currentPlayerName = this.getChatOwnerName();
        if (players != null) {
            players.clear();
        }

        overHeadChat = this.getChatOwner().getChatElement();
    }

    @Override
    public boolean isSendingToRadio() {
        return true;
    }

    @Override
    public ChatMessage createMessage(String text) {
        ChatMessage msg = super.createMessage(text);
        if (this.getMode() == ChatMode.SinglePlayer) {
            msg.setShowInChat(false);
        }

        msg.setOverHeadSpeech(true);
        msg.setShouldAttractZombies(true);
        return msg;
    }

    public ChatMessage createBubbleMessage(String text) {
        ChatMessage msg = super.createMessage(text);
        msg.setOverHeadSpeech(true);
        msg.setShowInChat(false);
        return msg;
    }

    @Override
    public void sendMessageToChatMembers(ChatMessage msg) {
        IsoPlayer msgAuthor = ChatUtility.findPlayer(msg.getAuthor());
        if (this.getRange() == -1.0F) {
            DebugLog.log("Range not set for '" + this.getTitle() + "' chat. Message '" + msg.getText() + "' ignored");
        } else {
            for (short playerID : this.members) {
                IsoPlayer player = ChatUtility.findPlayer(playerID);
                if (player != null && msgAuthor.getOnlineID() != playerID && ChatUtility.getDistance(msgAuthor, player) < this.getRange()) {
                    this.sendMessageToPlayer(playerID, msg);
                }
            }
        }
    }

    @Override
    public void showMessage(ChatMessage msg) {
        super.showMessage(msg);
        if (msg.isOverHeadSpeech()) {
            this.showInSpeechBubble(msg);
        }
    }

    protected ChatElement getSpeechBubble() {
        return overHeadChat;
    }

    protected UIFont selectFont(String msgText) {
        char[] chars = msgText.toCharArray();
        UIFont font = UIFont.Dialogue;
        AngelCodeFont defaultFont = TextManager.instance.getFontFromEnum(font);

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > defaultFont.chars.length) {
                font = UIFont.Medium;
                break;
            }
        }

        return font;
    }

    protected void showInSpeechBubble(ChatMessage msg) {
        Color color = this.getColor();
        String author = msg.getAuthor();
        IsoPlayer authorObj = this.getPlayer(author);
        float r = color.r;
        float g = color.g;
        float b = color.b;
        if (authorObj != null) {
            r = authorObj.getSpeakColour().r;
            g = authorObj.getSpeakColour().g;
            b = authorObj.getSpeakColour().b;
        }

        String msgText = ChatUtility.parseStringForChatBubble(msg.getText());
        if (author != null && !"".equalsIgnoreCase(author) && !author.equalsIgnoreCase(currentPlayerName)) {
            if (!players.containsKey(author)) {
                players.put(author, this.getPlayer(author));
            }

            IsoPlayer anotherPlayer = players.get(author);
            if (anotherPlayer != null) {
                if (anotherPlayer.isDead()) {
                    anotherPlayer = this.getPlayer(author);
                    players.replace(author, anotherPlayer);
                }

                anotherPlayer.getChatElement()
                    .addChatLine(
                        msgText,
                        r,
                        g,
                        b,
                        this.selectFont(msgText),
                        this.getRange(),
                        this.customTag,
                        this.isAllowBBcode(),
                        this.isAllowImages(),
                        this.isAllowChatIcons(),
                        this.isAllowColors(),
                        this.isAllowFonts(),
                        this.isEqualizeLineHeights()
                    );
            }
        } else {
            overHeadChat.addChatLine(
                msgText,
                r,
                g,
                b,
                this.selectFont(msgText),
                this.getRange(),
                this.customTag,
                this.isAllowBBcode(),
                this.isAllowImages(),
                this.isAllowChatIcons(),
                this.isAllowColors(),
                this.isAllowFonts(),
                this.isEqualizeLineHeights()
            );
        }
    }

    private IsoPlayer getPlayer(String name) {
        IsoPlayer player = GameClient.client ? GameClient.instance.getPlayerFromUsername(name) : null;
        if (player != null) {
            return player;
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                player = IsoPlayer.players[i];
                if (player != null && player.getUsername().equals(name)) {
                    return player;
                }
            }

            return null;
        }
    }
}
