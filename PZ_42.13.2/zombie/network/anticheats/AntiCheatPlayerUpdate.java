// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

public class AntiCheatPlayerUpdate extends AbstractAntiCheat {
    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        AntiCheatPlayerUpdate.IAntiCheatUpdate field = connection.validator;

        for (IsoPlayer player : connection.players) {
            if (player != null && (player.isDead() || player.getVehicle() != null)) {
                connection.validator.playerUpdateTimeoutReset();
                return true;
            }
        }

        return !this.antiCheat.isEnabled() || !field.playerUpdateTimeoutCheck();
    }

    public interface IAntiCheatUpdate {
        boolean playerUpdateTimeoutCheck();

        void playerUpdateTimeoutReset();
    }
}
