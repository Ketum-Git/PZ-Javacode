// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.modding;

import java.util.ArrayList;
import java.util.UUID;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ModUtilsJava {
    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public static boolean sendItemListNet(IsoPlayer sender, ArrayList<InventoryItem> items, IsoPlayer receiver, String transferID, String custom) {
        if (items != null) {
            transferID = transferID != null ? transferID : "-1";
            if (GameClient.client) {
                if (items.size() > 50) {
                    return false;
                }

                for (int i = 0; i < items.size(); i++) {
                    InventoryItem item = items.get(i);
                    if (!sender.getInventory().getItems().contains(item)) {
                        return false;
                    }
                }

                return GameClient.sendItemListNet(sender, items, receiver, transferID, custom);
            }

            if (GameServer.server) {
                return GameServer.sendItemListNet(null, sender, items, receiver, transferID, custom);
            }
        }

        return false;
    }
}
