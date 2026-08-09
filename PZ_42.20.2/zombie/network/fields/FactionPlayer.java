// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.util.StringUtils;

public class FactionPlayer extends FactionId implements INetworkPacketField {
    @JSONField
    private String username;

    public void set(Faction faction, String username) {
        this.set(faction);
        this.username = username;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.username = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.username);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (StringUtils.isNullOrEmpty(this.username)) {
            DebugType.Multiplayer.error("FactionPlayer invalid: username is null or empty, faction=" + this.getFaction());
            return false;
        } else {
            return true;
        }
    }

    public String getUsername() {
        return this.username;
    }
}
