// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ClothingWetnessPacket implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private final List<ClothingWetnessPacket.ItemWetness> itemWetness = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.playerId.set((IsoPlayer)values[0]);
        this.itemWetness.clear();

        for (InventoryItem item : (List)values[1]) {
            this.itemWetness.add(new ClothingWetnessPacket.ItemWetness(item.getID(), item.getWetness()));
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.itemWetness.clear();
        int count = b.get() & 255;

        for (int i = 0; i < count; i++) {
            int itemID = b.getInt();
            float wetness = b.getFloat();
            this.itemWetness.add(new ClothingWetnessPacket.ItemWetness(itemID, wetness));
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte((byte)this.itemWetness.size());

        for (ClothingWetnessPacket.ItemWetness itemWetness : this.itemWetness) {
            b.putInt(itemWetness.id);
            b.putFloat(itemWetness.wetness);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ItemContainer inventory = this.playerId.getPlayer().getInventory();

        for (ClothingWetnessPacket.ItemWetness itemWetness : this.itemWetness) {
            if (inventory.getItemWithID(itemWetness.id) instanceof Clothing clothing) {
                clothing.setWetness(itemWetness.wetness);
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerId.getPlayer() != null;
    }

    private record ItemWetness(int id, float wetness) {
    }
}
