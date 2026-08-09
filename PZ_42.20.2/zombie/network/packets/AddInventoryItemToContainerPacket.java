// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoMannequin;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.fields.ContainerID;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AddInventoryItemToContainerPacket implements INetworkPacket {
    @JSONField
    private final ContainerID containerId = new ContainerID();
    @JSONField
    private final ArrayList<InventoryItem> items = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.containerId.set((ItemContainer)values[0]);
        this.items.clear();
        if (values[1] instanceof InventoryItem) {
            this.items.add((InventoryItem)values[1]);
        } else {
            this.items.addAll((ArrayList)values[1]);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);

        try {
            CompressIdenticalItems.save(b.bb, this.items, null);
        } catch (IOException var3) {
            DebugType.Multiplayer.printException(var3, "Items save fail", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.items.clear();

        try {
            this.items.addAll(CompressIdenticalItems.load(b.bb, 249, null, null));
        } catch (IOException var4) {
            DebugType.Multiplayer.printException(var4, "Items load fail", LogSeverity.Error);
        }
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

                try {
                    for (int i = 0; i < this.items.size(); i++) {
                        InventoryItem item = this.items.get(i);
                        if (item != null) {
                            if (container.containsID(item.id)) {
                                if (this.containerId.containerType != ContainerID.ContainerType.DeadBody) {
                                    System.out.println("Error: Dupe item ID. id = " + item.id);
                                }
                            } else {
                                container.addItem(item);
                                container.setExplored(true);
                                if (container.getParent() instanceof IsoMannequin) {
                                    ((IsoMannequin)container.getParent()).wearItem(item, null);
                                }

                                for (int j = 0; j < IsoPlayer.numPlayers; j++) {
                                    IsoPlayer var7 = IsoPlayer.players[j];
                                    if (var7 instanceof IsoPlayer) {
                                        ActionManager.getInstance().replaceObjectInQueuedActions(var7, null, item);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                }

                if (this.containerId.getPart() != null) {
                    this.containerId.getPart().setContainerContentAmount(container.getCapacityWeight());
                }
            }
        }
    }

    public int getX() {
        return this.containerId.x;
    }

    public int getY() {
        return this.containerId.y;
    }

    private static void logDupeItem(UdpConnection connection, String text) {
        IsoPlayer player = null;

        for (int k = 0; k < GameServer.Players.size(); k++) {
            if (connection.getUserName().equals(GameServer.Players.get(k).username)) {
                player = GameServer.Players.get(k);
                break;
            }
        }

        if (player != null) {
            String coordinate = LoggerManager.getPlayerCoords(player);
            LoggerManager.getLogger("user").write("Error: Dupe item ID for " + player.getDisplayName() + " " + coordinate);
        }

        ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.DupeItem, text, GameServer.class.getSimpleName(), 1);
    }
}
