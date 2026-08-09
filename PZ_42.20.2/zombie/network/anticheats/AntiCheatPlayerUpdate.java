// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

public class AntiCheatPlayerUpdate extends AbstractAntiCheat {
    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);

        for (IsoPlayer player : connection.players) {
            if (player != null && (player.isDead() || player.getVehicle() != null)) {
                connection.getValidator().playerUpdateTimeoutReset();
                return true;
            }
        }

        return !this.antiCheat.isEnabled() || !this.isPlayerUpdateTimeout(connection);
    }

    private boolean isPlayerUpdateTimeout(UdpConnection connection) {
        connection.setReady(System.currentTimeMillis() > connection.connectionTimestamp);
        return connection.isReady() && connection.getValidator().playerUpdateTimeoutCheck();
    }
}
