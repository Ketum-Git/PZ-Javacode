// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.util.StringUtils;

public class Invite implements INetworkPacketField {
    @JSONField
    private String host;
    @JSONField
    private String username;

    public void set(String host, String username) {
        this.host = host;
        this.username = username;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.host = b.getUTF();
        this.username = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.host);
        b.putUTF(this.username);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (StringUtils.isNullOrEmpty(this.host)) {
            DebugType.Multiplayer.error("host is not set");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.username)) {
            DebugType.Multiplayer.error("username is not set");
            return false;
        } else {
            return true;
        }
    }

    public String getHost() {
        return this.host;
    }

    public String getUsername() {
        return this.username;
    }
}
