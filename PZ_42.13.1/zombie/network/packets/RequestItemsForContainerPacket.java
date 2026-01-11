// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class RequestItemsForContainerPacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    ContainerID containerId = new ContainerID();

    @Override
    public void setData(Object... values) {
        this.player.set(IsoPlayer.getInstance());
        this.containerId.set((ItemContainer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        this.containerId.write(b);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container != null && !container.isExplored()) {
            container.setExplored(true);
            int count = container.items.size();
            ItemPickerJava.fillContainer(container, this.player.getPlayer());
            if (count != container.items.size()) {
                INetworkPacket.sendToRelative(
                    PacketTypes.PacketType.AddInventoryItemToContainer, this.containerId.x, this.containerId.y, container, container.getItems()
                );
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        this.containerId.parse(b, connection);
    }
}
