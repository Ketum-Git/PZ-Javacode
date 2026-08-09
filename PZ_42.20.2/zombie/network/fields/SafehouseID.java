// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;

public class SafehouseID extends IDInteger implements INetworkPacketField {
    private SafeHouse safeHouse;

    public void set(SafeHouse safeHouse) {
        this.setID(safeHouse.getOnlineID());
        this.safeHouse = safeHouse;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.safeHouse = SafeHouse.getSafeHouse(this.getID());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugType.Multiplayer.error("safehouse is not set");
            return false;
        } else if (this.getSafehouse() == null) {
            DebugType.Multiplayer.error("safehouse not found");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return String.valueOf(this.getID());
    }

    public SafeHouse getSafehouse() {
        return this.safeHouse;
    }
}
