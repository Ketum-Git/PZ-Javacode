// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.InventoryContainer;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.savefile.ServerPlayerDB;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.InspectPlayerInventory, handlingType = 3)
public class PlayerInventoryPacket implements INetworkPacket {
    private short id;
    private String username;

    @Override
    public void setData(Object... values) {
        this.id = (Short)values[0];
        this.username = (String)values[1];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.getShort();
        this.username = GameWindow.ReadStringUTF(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.id);
        b.putUTF(this.username);
    }

    @Override
    public void parseClient(ByteBuffer b, UdpConnection connection) {
        this.parse(b, connection);
        receiveSendInventory(b);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer player = GameServer.IDToPlayerMap.get(this.id);
        if (player == null) {
            player = ServerPlayerDB.getInstance().serverLoadNetworkCharacter(0, this.username);
        }

        if (player != null) {
            ByteBufferWriter b2 = connection.startPacket();
            PacketTypes.PacketType.PlayerInventory.doPacket(b2);
            this.write(b2);
            sendInventory(b2, player);
            PacketTypes.PacketType.PlayerInventory.send(connection);
        }
    }

    private static void sendInventory(ByteBufferWriter bb, IsoPlayer player) {
        int position = bb.bb.position();
        bb.putInt(0);
        bb.putFloat(player.getInventory().getCapacityWeight());
        bb.putFloat(player.getMaxWeight());
        LinkedHashMap<String, InventoryItem> items = player.getInventory().getItems4Admin();
        int size = sendInventoryPutItems(bb, player, items, -1L);
        int positionend = bb.bb.position();
        bb.bb.position(position);
        bb.putInt(size);
        bb.bb.position(positionend);
    }

    private static int sendInventoryPutItems(ByteBufferWriter bb, IsoPlayer player, LinkedHashMap<String, InventoryItem> items, long parrentId) {
        int size = items.size();
        Iterator<String> it = items.keySet().iterator();

        while (it.hasNext()) {
            InventoryItem item = items.get(it.next());
            bb.putUTF(item.getModule());
            bb.putUTF(item.getType());
            bb.putLong(item.getID());
            bb.putLong(parrentId);
            bb.putBoolean(player.isEquipped(item));
            if (item instanceof DrainableComboItem) {
                bb.putFloat(item.getCurrentUsesFloat());
            } else {
                bb.putFloat(item.getCondition());
            }

            bb.putInt(item.getCount());
            if (item instanceof DrainableComboItem) {
                bb.putUTF(Translator.getText("IGUI_ItemCat_Drainable"));
            } else {
                bb.putUTF(item.getCategory());
            }

            bb.putUTF(item.getContainer().getType());
            bb.putBoolean(item.getWorker() != null && item.getWorker().equals("inInv"));
            if (item instanceof InventoryContainer inventoryContainer
                && inventoryContainer.getItemContainer() != null
                && !inventoryContainer.getItemContainer().getItems().isEmpty()) {
                LinkedHashMap<String, InventoryItem> items2 = inventoryContainer.getItemContainer().getItems4Admin();
                size += items2.size();
                sendInventoryPutItems(bb, player, items2, item.getID());
            }
        }

        return size;
    }

    private static void receiveSendInventory(ByteBuffer bb) {
        int size = bb.getInt();
        float capacityWeight = bb.getFloat();
        float maxWeight = bb.getFloat();
        KahluaTable result = LuaManager.platform.newTable();
        result.rawset("capacityWeight", (double)capacityWeight);
        result.rawset("maxWeight", (double)maxWeight);

        for (int i = 0; i < size; i++) {
            KahluaTable newItem = LuaManager.platform.newTable();
            String fullType = GameWindow.ReadStringUTF(bb) + "." + GameWindow.ReadStringUTF(bb);
            long itemId = bb.getLong();
            long parrentId = bb.getLong();
            boolean isEquip = bb.get() == 1;
            float var = bb.getFloat();
            int count = bb.getInt();
            String cat = GameWindow.ReadStringUTF(bb);
            String container = GameWindow.ReadStringUTF(bb);
            boolean inInv = bb.get() == 1;
            newItem.rawset("fullType", fullType);
            newItem.rawset("itemId", itemId);
            newItem.rawset("isEquip", isEquip);
            newItem.rawset("var", Math.round(var * 100.0) / 100.0);
            newItem.rawset("count", count + "");
            newItem.rawset("cat", cat);
            newItem.rawset("parrentId", parrentId);
            newItem.rawset("hasParrent", parrentId != -1L);
            newItem.rawset("container", container);
            newItem.rawset("inInv", inInv);
            result.rawset(result.size() + 1, newItem);
        }

        LuaEventManager.triggerEvent("MngInvReceiveItems", result);
    }
}
