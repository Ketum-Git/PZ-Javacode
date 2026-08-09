// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.util.StringUtils;

public class SafeHousePlayer extends SafehouseID implements INetworkPacketField {
    @JSONField
    private String username;

    public void set(SafeHouse safeHouse, String player) {
        this.set(safeHouse);
        this.username = player;
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
            DebugType.Multiplayer.error("player is not set");
            return false;
        } else {
            return true;
        }
    }

    public String getUsername() {
        return this.username;
    }
}
