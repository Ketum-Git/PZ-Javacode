// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.chat;

public enum ChatType {
    notDefined(-1, ""),
    general(0, "UI_chat_general_chat_title_id"),
    whisper(1, "UI_chat_private_chat_title_id"),
    say(2, "UI_chat_local_chat_title_id"),
    shout(3, "UI_chat_local_chat_title_id"),
    faction(4, "UI_chat_faction_chat_title_id"),
    safehouse(5, "UI_chat_safehouse_chat_title_id"),
    radio(6, "UI_chat_radio_chat_title_id"),
    admin(7, "UI_chat_admin_chat_title_id"),
    server(8, "UI_chat_server_chat_title_id");

    private final int value;
    private final String titleId;

    public static ChatType valueOf(Integer value) {
        if (general.value == value) {
            return general;
        } else if (whisper.value == value) {
            return whisper;
        } else if (say.value == value) {
            return say;
        } else if (shout.value == value) {
            return shout;
        } else if (faction.value == value) {
            return faction;
        } else if (safehouse.value == value) {
            return safehouse;
        } else if (radio.value == value) {
            return radio;
        } else if (admin.value == value) {
            return admin;
        } else {
            return server.value == value ? server : notDefined;
        }
    }

    private ChatType(final Integer value, final String titleId) {
        this.value = value;
        this.titleId = titleId;
    }

    public int getValue() {
        return this.value;
    }

    public String getTitleID() {
        return this.titleId;
    }
}
