// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.EditItem, handlingType = 3)
public class SyncItemDeletePacket extends RemoveInventoryItemFromContainerPacket implements INetworkPacket {
    private static final ArrayList<Integer> alreadyRemoved = new ArrayList<>();

    @Override
    protected ArrayList<Integer> getAlreadyRemoved() {
        return alreadyRemoved;
    }
}
