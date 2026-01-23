// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.PersistentOutfits;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.WornItems.WornItem;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoFallingClothing;
import zombie.network.GameClient;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.scripting.objects.Item;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ZombieHelmetFallingPacket implements INetworkPacket {
    boolean isZombie = true;
    short characterIndex = -1;
    InventoryItem item;
    float targetX;
    float targetY;
    float targetZ;
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();

    public void set(IsoGameCharacter character, InventoryItem item, float x, float y, float z) {
        this.isZombie = character instanceof IsoZombie;
        this.characterIndex = character.getOnlineID();
        this.item = item;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)(this.isZombie ? 1 : 0));
        b.putShort(this.characterIndex);
        b.putFloat(this.targetX);
        b.putFloat(this.targetY);
        b.putFloat(this.targetZ);

        try {
            this.item.saveWithSize(b.bb, false);
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.isZombie = b.get() > 0;
        this.characterIndex = b.getShort();
        this.targetX = b.getFloat();
        this.targetY = b.getFloat();
        this.targetZ = b.getFloat();

        try {
            this.item = InventoryItem.loadItem(b, 241);
        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoZombie zed = null;
        IsoPlayer player = null;
        if (this.isZombie) {
            zed = GameClient.IDToZombieMap.get(this.characterIndex);
        } else {
            player = GameClient.IDToPlayerMap.get(this.characterIndex);
        }

        if (zed != null && !zed.isUsingWornItems()) {
            IsoFallingClothing falling = new IsoFallingClothing(
                zed.getCell(), zed.getX(), zed.getY(), PZMath.min(zed.getZ() + 0.4F, zed.getZi() + 0.95F), 0.2F, 0.2F, this.item
            );
            falling.targetX = this.targetX;
            falling.targetY = this.targetY;
            falling.targetZ = this.targetZ;
            zed.getItemVisuals(tempItemVisuals);

            for (int i = 0; i < tempItemVisuals.size(); i++) {
                ItemVisual itemVisual = tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem.name.equals(this.item.getType())) {
                    tempItemVisuals.remove(i);
                    break;
                }
            }

            zed.getItemVisuals().clear();
            zed.getItemVisuals().addAll(tempItemVisuals);
            zed.resetModelNextFrame();
            zed.onWornItemsChanged();
            PersistentOutfits.instance.setFallenHat(zed, true);
        } else if (player != null && player.getWornItems() != null && !player.getWornItems().isEmpty()) {
            IsoFallingClothing falling = new IsoFallingClothing(
                player.getCell(), player.getX(), player.getY(), PZMath.min(player.getZ() + 0.4F, player.getZi() + 0.95F), 0.2F, 0.2F, this.item
            );
            falling.targetX = this.targetX;
            falling.targetY = this.targetY;
            falling.targetZ = this.targetZ;

            for (int ix = 0; ix < player.getWornItems().size(); ix++) {
                WornItem wornItem = player.getWornItems().get(ix);
                InventoryItem playerItem = wornItem.getItem();
                if (playerItem.getType().equals(this.item.getType())) {
                    player.getInventory().Remove(playerItem);
                    player.getWornItems().remove(playerItem);
                    player.resetModelNextFrame();
                    player.onWornItemsChanged();
                    if (GameClient.client && player.isLocalPlayer()) {
                        INetworkPacket.send(PacketTypes.PacketType.SyncClothing, player);
                    }

                    if (player.isLocalPlayer()) {
                        LuaEventManager.triggerEvent("OnClothingUpdated", player);
                    }
                    break;
                }
            }
        } else {
            IsoFallingClothing falling = new IsoFallingClothing(IsoWorld.instance.currentCell, this.targetX, this.targetY, this.targetZ, 0.0F, 0.0F, this.item);
            falling.targetX = this.targetX;
            falling.targetY = this.targetY;
            falling.targetZ = this.targetZ;
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.item != null;
    }
}
