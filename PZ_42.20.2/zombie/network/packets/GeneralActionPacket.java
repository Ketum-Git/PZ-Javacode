// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.ActionManager;
import zombie.core.GeneralAction;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class GeneralActionPacket extends GeneralAction implements INetworkPacket {
    public void setReject(byte id) {
        this.id = id;
        this.state = Transaction.TransactionState.Reject;
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (Transaction.TransactionState.Reject == this.state) {
            DebugType.Action.trace("GeneralAction client reject %s", this.getDescription());
            ActionManager.stop(this);
        }
    }
}
