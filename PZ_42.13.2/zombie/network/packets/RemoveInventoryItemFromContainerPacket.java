// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RemoveInventoryItemFromContainerPacket implements INetworkPacket {
    private static final ArrayList<Integer> alreadyRemoved = new ArrayList<>();
    @JSONField
    private final ContainerID containerId = new ContainerID();
    @JSONField
    private final ArrayList<Integer> ids = new ArrayList<>();

    protected ArrayList<Integer> getAlreadyRemoved() {
        return alreadyRemoved;
    }

    public boolean isInventory() {
        return ContainerID.ContainerType.PlayerInventory == this.containerId.containerType
            || ContainerID.ContainerType.InventoryContainer == this.containerId.containerType;
    }

    public IsoPlayer getPlayer() {
        return this.containerId.playerId.getPlayer();
    }

    @Override
    public void setData(Object... values) {
        this.containerId.set((ItemContainer)values[0]);
        this.ids.clear();
        if (values[1] instanceof InventoryItem) {
            this.ids.add(((InventoryItem)values[1]).id);
        } else {
            for (InventoryItem item : (ArrayList)values[1]) {
                this.ids.add(item.id);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putShort((short)this.ids.size());

        for (int n = 0; n < this.ids.size(); n++) {
            b.putInt(this.ids.get(n));
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.containerId.parse(b, connection);
        this.ids.clear();
        short count = b.getShort();

        for (int n = 0; n < count; n++) {
            int id = b.getInt();
            this.ids.add(id);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            for (int n = 0; n < this.ids.size(); n++) {
                int id = this.ids.get(n);
                container.removeItemWithID(id);
                container.setExplored(true);
            }

            if (this.containerId.getPart() != null) {
                this.containerId.getPart().setContainerContentAmount(container.getCapacityWeight());
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        HashSet<String> logItemType = new HashSet<>();
        boolean removed = false;
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            for (int n = 0; n < this.ids.size(); n++) {
                int id = this.ids.get(n);
                InventoryItem item = container.getItemWithID(id);
                if (item == null) {
                    this.getAlreadyRemoved().add(id);
                } else {
                    container.Remove(item);
                    removed = true;
                    logItemType.add(item.getFullType());
                }
            }

            container.setExplored(true);
            container.setHasBeenLooted(true);
        }

        for (int nx = 0; nx < GameServer.udpEngine.connections.size(); nx++) {
            UdpConnection c = GameServer.udpEngine.connections.get(nx);
            if (c.getConnectedGUID() != connection.getConnectedGUID() && c.RelevantTo(this.containerId.x, this.containerId.y)) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.RemoveInventoryItemFromContainer.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.RemoveInventoryItemFromContainer.send(c);
            }
        }

        if (!this.getAlreadyRemoved().isEmpty()) {
            INetworkPacket.send(connection, PacketTypes.PacketType.RemoveContestedItemsFromInventory, this.getAlreadyRemoved());
        }

        this.getAlreadyRemoved().clear();
        LoggerManager.getLogger("item")
            .write(
                connection.idStr
                    + " \""
                    + connection.username
                    + "\" container -"
                    + this.ids.size()
                    + " "
                    + this.containerId.x
                    + ","
                    + this.containerId.y
                    + ","
                    + this.containerId.z
                    + " "
                    + logItemType.toString()
            );
    }

    public float getX() {
        return this.containerId.x;
    }

    public float getY() {
        return this.containerId.y;
    }
}
