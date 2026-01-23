// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.ActionManager;
import zombie.core.GeneralAction;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class GeneralActionPacket extends GeneralAction implements INetworkPacket {
    public void setReject(byte _id) {
        this.id = _id;
        this.state = Transaction.TransactionState.Reject;
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (Transaction.TransactionState.Reject == this.state) {
            DebugLog.Action.trace("GeneralAction client reject %s", this.getDescription());
            ActionManager.stop(this);
        }
    }
}
