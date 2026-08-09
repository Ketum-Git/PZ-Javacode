// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.util.ArrayList;
import zombie.chat.defaultChats.WhisperChat;

public final class WhisperChatCreation {
    String destPlayerName;
    WhisperChat.ChatStatus status = WhisperChat.ChatStatus.None;
    long createTime;
    final ArrayList<String> messages = new ArrayList<>();
}
