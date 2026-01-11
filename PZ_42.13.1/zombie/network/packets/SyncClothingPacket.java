// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.WornItems.WornItem;
import zombie.characters.animals.IsoAnimal;
import zombie.core.ImmutableColor;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.fields.character.PlayerID;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.Type;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncClothingPacket implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private final ArrayList<SyncClothingPacket.ItemDescription> items = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player) {
        if (player instanceof IsoAnimal) {
            DebugLog.General.printStackTrace("SyncClothingPacket.set receives IsoAnimal");
        }

        this.playerId.set(player);
        this.items.clear();
        this.playerId.getPlayer().getWornItems().forEach(item -> {
            if (item != null && item.getItem() != null) {
                this.items.add(new SyncClothingPacket.ItemDescription(item));
            }
        });
    }

    void parseClothing(ByteBuffer b, int itemId) {
        IsoPlayer player = this.playerId.getPlayer();
        if (player != null) {
            Clothing clothing = Type.tryCastTo(player.getInventory().getItemWithID(itemId), Clothing.class);
            if (clothing != null) {
                clothing.removeAllPatches();
            }

            byte patchesNum = b.get();

            for (byte j = 0; j < patchesNum; j++) {
                byte bloodBodyPartTypeIdx = b.get();
                byte tailorLvl = b.get();
                byte fabricType = b.get();
                boolean hasHole = b.get() == 1;
                if (clothing != null) {
                    clothing.addPatchForSync(bloodBodyPartTypeIdx, tailorLvl, fabricType, hasHole);
                }
            }
        }
    }

    void writeClothing(ByteBufferWriter b, int itemId) {
        IsoPlayer player = this.playerId.getPlayer();
        if (player == null) {
            b.putByte((byte)0);
        } else {
            Clothing clothing = Type.tryCastTo(player.getInventory().getItemWithID(itemId), Clothing.class);
            if (clothing == null) {
                b.putByte((byte)0);
            } else {
                b.putByte((byte)clothing.getPatchesNumber());

                for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                    Clothing.ClothingPatch patch = clothing.getPatchType(BloodBodyPartType.FromIndex(i));
                    if (patch != null) {
                        b.putByte((byte)i);
                        b.putByte((byte)patch.tailorLvl);
                        b.putByte((byte)patch.fabricType);
                        b.putByte((byte)(patch.hasHole ? 1 : 0));
                    }
                }
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        IsoPlayer player = this.playerId.getPlayer();
        if (player != null) {
            this.items.clear();
            byte size = b.get();

            for (int i = 0; i < size; i++) {
                SyncClothingPacket.ItemDescription item = new SyncClothingPacket.ItemDescription();
                item.parse(b, connection);
                this.items.add(item);
                this.parseClothing(b, item.itemId);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte((byte)this.items.size());

        for (SyncClothingPacket.ItemDescription item : this.items) {
            item.write(b);
            this.writeClothing(b, item.itemId);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerId.getPlayer() != null;
    }

    private boolean isItemsContains(int itemId, ItemBodyLocation location) {
        for (SyncClothingPacket.ItemDescription item : this.items) {
            if (item.itemId == itemId && item.location.equals(location)) {
                return true;
            }
        }

        return false;
    }

    private void process() {
        if (this.playerId.getPlayer().remote) {
            this.playerId.getPlayer().getItemVisuals().clear();
        }

        ArrayList<InventoryItem> itemsForDelete = new ArrayList<>();
        this.playerId.getPlayer().getWornItems().forEach(itemx -> {
            if (!this.isItemsContains(itemx.getItem().getID(), itemx.getLocation())) {
                itemsForDelete.add(itemx.getItem());
            }
        });

        for (InventoryItem item : itemsForDelete) {
            this.playerId.getPlayer().getWornItems().remove(item);
        }

        for (SyncClothingPacket.ItemDescription item : this.items) {
            Clothing wornItem = Type.tryCastTo(this.playerId.getPlayer().getWornItems().getItem(item.location), Clothing.class);
            int wornItemId = wornItem == null ? -1 : wornItem.getID();
            if (wornItemId != item.itemId) {
                InventoryItem itemForAdd = this.playerId.getPlayer().getInventory().getItemWithID(item.itemId);
                if (itemForAdd == null) {
                    itemForAdd = InventoryItemFactory.CreateItem(item.itemType);
                }

                if (itemForAdd != null) {
                    this.playerId.getPlayer().getWornItems().setItem(item.location, itemForAdd);
                    if (this.playerId.getPlayer().remote) {
                        itemForAdd.getVisual().setTint(item.tint);
                        itemForAdd.getVisual().setBaseTexture(item.baseTexture);
                        itemForAdd.getVisual().setTextureChoice(item.textureChoice);
                        this.playerId.getPlayer().getItemVisuals().add(itemForAdd.getVisual());
                    }
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (GameClient.client) {
            this.process();
        }

        this.playerId.getPlayer().resetModelNextFrame();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.process();
        if (ServerGUI.isCreated()) {
            this.playerId.getPlayer().resetModelNextFrame();
        }

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            IsoPlayer p2 = GameServer.getAnyPlayerFromConnection(connection);
            if (p2 != null) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.SyncClothing.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.SyncClothing.send(c);
            }
        }
    }

    static class ItemDescription implements INetworkPacket {
        @JSONField
        int itemId;
        @JSONField
        String itemType;
        @JSONField
        ItemBodyLocation location;
        @JSONField
        ImmutableColor tint;
        @JSONField
        int textureChoice;
        @JSONField
        int baseTexture;

        public ItemDescription() {
        }

        public ItemDescription(WornItem item) {
            this.itemId = item.getItem().getID();
            this.itemType = item.getItem().getFullType();
            this.location = item.getLocation();
            this.baseTexture = item.getItem().getVisual() == null ? -1 : item.getItem().getVisual().getBaseTexture();
            this.textureChoice = item.getItem().getVisual() == null ? -1 : item.getItem().getVisual().getTextureChoice();
            this.tint = item.getItem().getVisual().getTint();
        }

        @Override
        public void write(ByteBufferWriter b) {
            b.putInt(this.itemId);
            b.putUTF(this.itemType);
            b.putUTF(this.location.toString());
            b.putInt(this.textureChoice);
            b.putInt(this.baseTexture);
            b.putFloat(this.tint.r);
            b.putFloat(this.tint.g);
            b.putFloat(this.tint.b);
            b.putFloat(this.tint.a);
        }

        @Override
        public void parse(ByteBuffer b, UdpConnection connection) {
            this.itemId = b.getInt();
            this.itemType = GameWindow.ReadString(b);
            this.location = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(b)));
            this.textureChoice = b.getInt();
            this.baseTexture = b.getInt();
            this.tint = new ImmutableColor(b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat());
        }
    }
}
