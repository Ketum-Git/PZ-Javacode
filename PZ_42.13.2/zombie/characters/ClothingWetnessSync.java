// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class ClothingWetnessSync {
    private static final List<InventoryItem> tempItems = new ArrayList<>();
    private static final List<InventoryItem> toSend = new ArrayList<>();
    private final IsoPlayer player;
    private final Map<InventoryItem, ClothingWetnessSync.ItemWetness> itemMap = new HashMap<>();
    private final UpdateLimit updateLimit = new UpdateLimit(3000L);

    public ClothingWetnessSync(IsoPlayer player) {
        this.player = player;
    }

    public void update() {
        if (this.updateLimit.Check()) {
            List<InventoryItem> items = this.player.getInventory().getItems();
            tempItems.clear();
            tempItems.addAll(this.itemMap.keySet());
            toSend.clear();

            for (InventoryItem item : items) {
                if (item instanceof Clothing clothing) {
                    float wetness = clothing.getWetness();
                    ClothingWetnessSync.ItemWetness itemWetness = this.itemMap.computeIfAbsent(item, ClothingWetnessSync.ItemWetness::new);
                    if (PZMath.ceil(itemWetness.lastSentWetness) != PZMath.ceil(wetness)) {
                        itemWetness.lastSentWetness = wetness;
                        toSend.add(item);
                    }

                    tempItems.remove(item);
                }
            }

            tempItems.forEach(this.itemMap::remove);
            if (!toSend.isEmpty()) {
                UdpConnection connection = GameServer.getConnectionFromPlayer(this.player);
                INetworkPacket.send(connection, PacketTypes.PacketType.ClothingWetness, this.player, toSend);
            }
        }
    }

    private static final class ItemWetness {
        final InventoryItem item;
        float lastSentWetness = Float.NaN;

        ItemWetness(InventoryItem item) {
            this.item = item;
        }
    }
}
