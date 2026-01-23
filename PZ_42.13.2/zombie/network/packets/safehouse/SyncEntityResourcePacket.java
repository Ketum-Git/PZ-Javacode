// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.entity.components.resources.Resource;
import zombie.network.PacketSetting;
import zombie.network.fields.ResourceID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2, anticheats = {})
public class SyncEntityResourcePacket implements INetworkPacket {
    private final ResourceID resourceID = new ResourceID();

    @Override
    public void setData(Object... values) {
        this.resourceID.set((Resource)values[0]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.resourceID.parse(b, connection);

        try {
            this.resourceID.getResource().load(b, 241);
        } catch (IOException var4) {
            ExceptionLogger.logException(var4);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.resourceID.write(b);

        try {
            this.resourceID.getResource().save(b.bb);
        } catch (IOException var3) {
            ExceptionLogger.logException(var3);
        }
    }
}
