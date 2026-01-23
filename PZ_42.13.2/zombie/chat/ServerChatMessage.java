// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import zombie.UsedFromLua;

/**
 * Messages which sent by server to any chat stream. This applied stream setting but author always Server
 */
@UsedFromLua
public class ServerChatMessage extends ChatMessage {
    public ServerChatMessage(ChatBase chat, String text) {
        super(chat, text);
        super.setAuthor("Server");
        this.setServerAuthor(true);
    }

    @Override
    public String getAuthor() {
        return super.getAuthor();
    }

    @Override
    public void setAuthor(String author) {
        throw new UnsupportedOperationException();
    }
}
