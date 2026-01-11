// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.ContainerID;
import zombie.network.fields.PlayerItem;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ReplaceInventoryItemInContainerPacket implements INetworkPacket {
    @JSONField
    ContainerID containerId = new ContainerID();
    @JSONField
    int oldItemId;
    @JSONField
    PlayerItem item = new PlayerItem();

    @Override
    public void setData(Object... values) {
        this.containerId.set((ItemContainer)values[0]);
        this.oldItemId = ((InventoryItem)values[1]).id;
        this.item.set((InventoryItem)values[2]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.oldItemId);
        this.item.write(b);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.containerId.parse(b, connection);
        this.oldItemId = b.getInt();
        this.item.parse(b, connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (IsoWorld.instance.currentCell != null) {
            ItemContainer container = this.containerId.getContainer();
            if (container != null) {
                if (container.getType().equals("floor") && !container.getItems().isEmpty()) {
                    InventoryItem item = container.getItems().get(0);
                    if (item instanceof InventoryContainer inventoryContainer) {
                        container = inventoryContainer.getItemContainer();
                    }
                }

                InventoryItem oldItem = container.getItemWithID(this.oldItemId);
                if (this.containerId.getPart() != null) {
                    this.containerId.getPart().setContainerContentAmount(container.getCapacityWeight());
                }

                if (this.containerId.getContainer().getCharacter() instanceof IsoPlayer player) {
                    ActionManager.getInstance().replaceObjectInQueuedActions(player, oldItem, this.item.getItem());
                }

                container.removeItemWithID(this.oldItemId);
                container.addItem(this.item.getItem());
                container.setExplored(true);
            }
        }
    }

    public int getX() {
        return this.containerId.x;
    }

    public int getY() {
        return this.containerId.y;
    }
}
