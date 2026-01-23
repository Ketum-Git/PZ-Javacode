// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;

public class SafehouseID extends IDInteger implements INetworkPacketField {
    private SafeHouse safeHouse;

    public void set(SafeHouse safeHouse) {
        this.setID(safeHouse.getOnlineID());
        this.safeHouse = safeHouse;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.safeHouse = SafeHouse.getSafeHouse(this.getID());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.getSafehouse() != null;
    }

    @Override
    public String toString() {
        return String.valueOf(this.getID());
    }

    public SafeHouse getSafehouse() {
        return this.safeHouse;
    }
}
