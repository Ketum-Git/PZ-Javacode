// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat.defaultChats;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatMessage;
import zombie.chat.ChatMode;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.network.chat.ChatType;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.ui.UIFont;

public class RadioChat extends RangeBasedChat {
    public RadioChat(ByteBuffer bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.radio, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }

        this.customTag = "radio";
    }

    public RadioChat(int id, ChatTab tab) {
        super(id, ChatType.radio, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(getDefaultSettings());
        }

        this.customTag = "radio";
    }

    public RadioChat() {
        super(ChatType.radio);
        this.setSettings(getDefaultSettings());
        this.customTag = "radio";
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(Color.lightGray);
        settings.setShowAuthor(false);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(true);
        settings.setAllowColors(true);
        settings.setAllowFonts(false);
        settings.setAllowBBcode(true);
        settings.setAllowImages(false);
        settings.setAllowChatIcons(true);
        return settings;
    }

    @Override
    public ChatMessage createMessage(String text) {
        ChatMessage msg = super.createMessage(text);
        if (this.getMode() == ChatMode.SinglePlayer) {
            msg.setOverHeadSpeech(true);
            msg.setShowInChat(false);
        }

        msg.setShouldAttractZombies(true);
        return msg;
    }

    public ChatMessage createBroadcastingMessage(String text, int channel) {
        ChatMessage chatMessage = this.createBubbleMessage(text);
        chatMessage.setAuthor("");
        chatMessage.setShouldAttractZombies(false);
        chatMessage.setRadioChannel(channel);
        return chatMessage;
    }

    public ChatMessage createStaticSoundMessage(String text) {
        ChatMessage chatMessage = this.createBubbleMessage(text);
        chatMessage.setAuthor("");
        chatMessage.setShouldAttractZombies(false);
        return chatMessage;
    }

    @Override
    protected void showInSpeechBubble(ChatMessage msg) {
        Color color = this.getColor();
        this.getSpeechBubble()
            .addChatLine(
                msg.getText(),
                color.r,
                color.g,
                color.b,
                UIFont.Dialogue,
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

    @Override
    public void showMessage(ChatMessage msg) {
        if (this.isEnabled() && msg.isShowInChat() && this.hasChatTab()) {
            LuaEventManager.triggerEvent("OnAddMessage", msg, this.getTabID());
        }
    }

    @Override
    public void sendToServer(ChatMessage msg, DeviceData deviceData) {
        if (deviceData != null) {
            int x = PZMath.fastfloor(this.getChatOwner().getX());
            int y = PZMath.fastfloor(this.getChatOwner().getY());
            int strength = deviceData.getTransmitRange();
            ZomboidRadio.getInstance().SendTransmission(x, y, msg, strength);
        }
    }

    @Override
    public ChatMessage unpackMessage(ByteBuffer bb) {
        ChatMessage msg = super.unpackMessage(bb);
        msg.setRadioChannel(bb.getInt());
        msg.setOverHeadSpeech(bb.get() == 1);
        msg.setShowInChat(bb.get() == 1);
        msg.setShouldAttractZombies(bb.get() == 1);
        return msg;
    }

    @Override
    public void packMessage(ByteBufferWriter b, ChatMessage msg) {
        super.packMessage(b, msg);
        b.putInt(msg.getRadioChannel());
        b.putBoolean(msg.isOverHeadSpeech());
        b.putBoolean(msg.isShowInChat());
        b.putBoolean(msg.isShouldAttractZombies());
    }

    @Override
    public String getMessagePrefix(ChatMessage msg) {
        StringBuilder chatLine = new StringBuilder(this.getChatSettingsTags());
        if (this.isShowTimestamp()) {
            chatLine.append("[").append(LuaManager.getHourMinuteJava()).append("]");
        }

        if (this.isShowTitle()) {
            chatLine.append("[").append(this.getTitle()).append("]");
        }

        if (this.isShowAuthor() && msg.getAuthor() != null && !msg.getAuthor().equals("")) {
            chatLine.append(" ").append(msg.getAuthor()).append(" ");
        } else {
            chatLine.append(" ").append("Radio").append(" ");
        }

        chatLine.append(" (").append(this.getRadioChannelStr(msg)).append("): ");
        return chatLine.toString();
    }

    private String getRadioChannelStr(ChatMessage msg) {
        StringBuilder result = new StringBuilder();
        int channel = msg.getRadioChannel();
        int hz = channel % 1000;

        while (hz % 10 == 0 && hz != 0) {
            hz /= 10;
        }

        int mhz = channel / 1000;
        result.append(mhz).append(".").append(hz).append(" MHz");
        return result.toString();
    }
}
