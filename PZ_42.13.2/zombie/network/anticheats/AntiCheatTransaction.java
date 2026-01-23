// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.Transaction;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTransaction extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatTransaction.IAntiCheat field = (AntiCheatTransaction.IAntiCheat)packet;
        Transaction.TransactionState state = field.getState();
        if (Transaction.TransactionState.Accept != state && Transaction.TransactionState.Done != state) {
            return Transaction.TransactionState.Request == state && field.getConsistent() == -1 ? "invalid request" : result;
        } else {
            return "invalid state";
        }
    }

    public interface IAntiCheat {
        Transaction.TransactionState getState();

        byte getConsistent();
    }
}
