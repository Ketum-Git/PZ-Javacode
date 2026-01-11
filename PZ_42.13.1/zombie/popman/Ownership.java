// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import zombie.core.raknet.UdpConnection;

public class Ownership {
    private long timestamp = -1L;
    private UdpConnection connection;

    public void setOwnership(UdpConnection connection) {
        this.connection = connection;
        this.timestamp = System.currentTimeMillis();
    }

    public UdpConnection getConnection() {
        return this.connection;
    }

    public boolean isBlocked(int interval) {
        return System.currentTimeMillis() - this.timestamp < interval && this.connection != null;
    }
}
