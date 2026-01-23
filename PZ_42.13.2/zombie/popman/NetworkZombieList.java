// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import java.util.LinkedList;
import zombie.characters.IsoZombie;
import zombie.core.raknet.UdpConnection;

public class NetworkZombieList {
    final LinkedList<NetworkZombieList.NetworkZombie> networkZombies = new LinkedList<>();
    public Object lock = new Object();

    public NetworkZombieList.NetworkZombie getNetworkZombie(UdpConnection connection) {
        if (connection == null) {
            return null;
        } else {
            for (NetworkZombieList.NetworkZombie nz : this.networkZombies) {
                if (nz.connection == connection) {
                    return nz;
                }
            }

            NetworkZombieList.NetworkZombie nzx = new NetworkZombieList.NetworkZombie(connection);
            this.networkZombies.add(nzx);
            return nzx;
        }
    }

    public static class NetworkZombie {
        public final LinkedList<IsoZombie> zombies = new LinkedList<>();
        final UdpConnection connection;

        public NetworkZombie(UdpConnection _connection) {
            this.connection = _connection;
        }
    }
}
