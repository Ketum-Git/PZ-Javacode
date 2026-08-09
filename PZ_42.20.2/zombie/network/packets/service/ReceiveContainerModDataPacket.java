// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.io.IOException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.ItemContainer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ReceiveContainerModDataPacket implements INetworkPacket {
    @JSONField
    private final ContainerID containerId = new ContainerID();
    @JSONField
    private KahluaTable table;

    @Override
    public void setData(Object... values) {
        this.set((ItemContainer)values[0]);
    }

    public void set(ItemContainer _container) {
        this.containerId.set(_container);
        this.table = _container.getParent().getModData();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        if (this.isConsistent(connection)) {
            this.table = this.containerId.getContainer().getParent().getModData();

            try {
                this.table.load(b.bb, 249);
            } catch (IOException var4) {
                DebugType.Network.printException(var4, "Exception thrown parsing ReceiveContainerModDataPacket.", LogSeverity.Error);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);

        try {
            this.table.save(b.bb);
        } catch (IOException var3) {
            DebugType.Network.printException(var3, "Exception thrown writing ReceiveContainerModDataPacket.", LogSeverity.Error);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.containerId.getContainer() != null;
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClients(PacketTypes.PacketType.ReceiveContainerModData, connection);
    }
}
