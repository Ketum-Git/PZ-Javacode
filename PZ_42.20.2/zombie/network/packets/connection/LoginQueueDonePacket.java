// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.GameLoadingState;
import zombie.network.LoginQueue;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.LongField;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 5)
public class LoginQueueDonePacket extends LongField {
    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LoginQueue.receiveLoginQueueDone(this.getValue(), connection);
    }

    @Override
    public void parseClientLoading(ByteBufferReader b, UdpConnection connection) {
        GameLoadingState.Done();
    }
}
