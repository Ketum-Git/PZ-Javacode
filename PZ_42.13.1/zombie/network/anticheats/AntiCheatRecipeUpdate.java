// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;

public class AntiCheatRecipeUpdate extends AbstractAntiCheat {
    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        AntiCheatRecipeUpdate.IAntiCheatUpdate field = connection.validator;
        if (field.checksumIntervalCheck()) {
            field.checksumSend(false, false);
        }

        return !this.antiCheat.isEnabled() || !field.checksumTimeoutCheck();
    }

    public interface IAntiCheatUpdate {
        void checksumSend(boolean arg0, boolean arg1);

        boolean checksumIntervalCheck();

        boolean checksumTimeoutCheck();

        void checksumTimeoutReset();
    }
}
