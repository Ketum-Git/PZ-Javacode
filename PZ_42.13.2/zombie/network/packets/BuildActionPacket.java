// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.BuildAction;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class BuildActionPacket extends BuildAction implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        if (values.length != 7) {
            DebugLog.Multiplayer.error(this.getClass().getSimpleName() + ".set get invalid arguments");
        } else {
            this.set((IsoPlayer)values[0], (Float)values[1], (Float)values[2], (Float)values[3], (Boolean)values[4], (String)values[5], (KahluaTable)values[6]);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ActionManager.getInstance().setStateFromPacket(this);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.state == Transaction.TransactionState.Request) {
            if (this.isConsistent(connection) && this.item != null) {
                DebugLog.Action.trace("BuildAction accepted %s", this.getDescription());
                ActionManager.start(this);
                this.state = Transaction.TransactionState.Accept;
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.BuildAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.BuildAction.send(connection);
            } else {
                DebugLog.Action.trace("BuildAction rejected %s", this.getDescription());
                this.state = Transaction.TransactionState.Reject;
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.BuildAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.BuildAction.send(connection);
            }
        } else if (Transaction.TransactionState.Reject == this.state) {
            DebugLog.Action.trace("BuildAction reject %s", this.getDescription());
            ActionManager.stop(this);
        }
    }
}
