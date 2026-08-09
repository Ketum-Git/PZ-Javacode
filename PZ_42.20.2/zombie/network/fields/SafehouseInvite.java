// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.util.StringUtils;

public class SafehouseInvite extends SafeHousePlayer implements INetworkPacketField {
    @JSONField
    private String owner;

    public void set(SafeHouse safeHouse, String owner, String player) {
        super.set(safeHouse, player);
        this.owner = owner;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.owner = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.owner);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (StringUtils.isNullOrEmpty(this.owner)) {
            DebugType.Multiplayer.error("owner is not set");
            return false;
        } else {
            return true;
        }
    }

    public String getOwner() {
        return this.owner;
    }
}
