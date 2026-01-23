// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;

@UsedFromLua
public class Userlog {
    private final String username;
    private final String type;
    private final String text;
    private final String issuedBy;
    private final String lastUpdate;
    private int amount;

    public Userlog(String username, String type, String text, String issuedBy, int amount, String lastUpdate) {
        this.username = username;
        this.type = type;
        this.text = text;
        this.issuedBy = issuedBy;
        this.amount = amount;
        this.lastUpdate = lastUpdate;
    }

    public String getUsername() {
        return this.username;
    }

    public String getType() {
        return this.type;
    }

    public String getText() {
        return this.text;
    }

    public String getIssuedBy() {
        return this.issuedBy;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getLastUpdate() {
        return this.lastUpdate;
    }

    public void write(ByteBuffer output) {
        GameWindow.WriteStringUTF(output, this.username);
        GameWindow.WriteStringUTF(output, this.type);
        GameWindow.WriteStringUTF(output, this.text);
        GameWindow.WriteStringUTF(output, this.issuedBy);
        GameWindow.WriteStringUTF(output, this.lastUpdate);
        output.putInt(this.amount);
    }

    public Userlog(ByteBuffer input) {
        this.username = GameWindow.ReadString(input);
        this.type = GameWindow.ReadString(input);
        this.text = GameWindow.ReadString(input);
        this.issuedBy = GameWindow.ReadString(input);
        this.lastUpdate = GameWindow.ReadString(input);
        this.amount = input.getInt();
    }

    @UsedFromLua
    public static enum UserlogType {
        AdminLog(0),
        Kicked(1),
        Banned(2),
        DupeItem(3),
        LuaChecksum(4),
        WarningPoint(5),
        UnauthorizedPacket(6),
        SuspiciousActivity(7);

        private final int index;

        private UserlogType(final int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }

        public static Userlog.UserlogType fromIndex(int value) {
            return Userlog.UserlogType.class.getEnumConstants()[value];
        }

        public static Userlog.UserlogType FromString(String str) {
            return valueOf(str);
        }
    }
}
