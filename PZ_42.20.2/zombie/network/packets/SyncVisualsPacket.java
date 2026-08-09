// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.util.Type;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncVisualsPacket implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    ItemVisuals itemVisuals = new ItemVisuals();
    private int itemVisualsSize;

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values[0]);
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player) {
        this.playerId.set(player);
        this.itemVisuals.clear();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId.getPlayer() != null && this.itemVisualsSize == this.itemVisuals.size();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        if (this.playerId.isConsistent(connection)) {
            this.playerId.getPlayer().getItemVisuals(this.itemVisuals);
            this.itemVisualsSize = b.getByte();
            if (this.itemVisualsSize != this.itemVisuals.size()) {
                DebugType.General.error("Player has " + this.itemVisuals.size() + " itemVisuals but server tries to sync " + this.itemVisualsSize + " ones");
                return;
            }

            for (int i = 0; i < this.itemVisualsSize; i++) {
                ItemVisual itemVisual = this.itemVisuals.get(i);
                Clothing clothing = Type.tryCastTo(itemVisual.getInventoryItem(), Clothing.class);
                if (clothing != null) {
                    clothing.removeAllPatches();
                }

                for (int j = 0; j < BloodBodyPartType.MAX.index(); j++) {
                    BloodBodyPartType part = BloodBodyPartType.FromIndex(j);
                    byte basicPatch = b.getByte();
                    byte denimPatch = b.getByte();
                    byte leatherPatch = b.getByte();
                    byte hole = b.getByte();
                    float dirt = b.getFloat();
                    float blood = b.getFloat();
                    itemVisual.removePatch(part.index());
                    itemVisual.removeHole(part.index());
                    if (basicPatch != 0) {
                        itemVisual.setBasicPatch(part);
                    }

                    if (denimPatch != 0) {
                        itemVisual.setDenimPatch(part);
                    }

                    if (leatherPatch != 0) {
                        itemVisual.setLeatherPatch(part);
                    }

                    if (hole != 0) {
                        itemVisual.setHole(part);
                    }

                    itemVisual.setDirt(part, dirt);
                    itemVisual.setBlood(part, blood);
                }

                this.parsePatches(b, itemVisual);
                if (b.getBoolean()) {
                    float dirtiness = b.getFloat();
                    float bloodLevel = b.getFloat();
                    int condition = b.getInt();
                    if (clothing != null) {
                        clothing.setDirtiness(dirtiness);
                        clothing.setBloodLevel(bloodLevel);
                        clothing.setConditionNoSound(condition);
                    }
                }
            }

            if (b.getBoolean()) {
                int itemId = b.getInt();
                float bloodLevel = b.getFloat();
                InventoryItem item = this.playerId.getPlayer().getPrimaryHandItem();
                if (item == null || item.getID() != itemId) {
                    item = this.playerId.getPlayer().getInventory().getItemWithID(itemId);
                }

                if (item instanceof HandWeapon weapon) {
                    weapon.setBloodLevel(bloodLevel);
                }
            }
        }
    }

    private void parsePatches(ByteBufferReader b, ItemVisual itemVisual) {
        byte patchesNum = b.getByte();
        if (patchesNum > 0) {
            for (byte i = 0; i < patchesNum; i++) {
                byte bloodBodyPartTypeIdx = b.getByte();
                byte tailorLvl = b.getByte();
                byte fabricType = b.getByte();
                boolean hasHole = b.getBoolean();
                if (itemVisual.getInventoryItem() instanceof Clothing clothing) {
                    clothing.addPatchForSync(bloodBodyPartTypeIdx, tailorLvl, fabricType, hasHole);
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.playerId.getPlayer().resetModelNextFrame();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID() && c.isRelevantTo(this.playerId.getX(), this.playerId.getY())) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.SyncVisuals.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.SyncVisuals.send(c);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.playerId.getPlayer().getItemVisuals(this.itemVisuals);
        b.putByte(this.itemVisuals.size());
        ArrayList<SyncVisualsPacket.Patch> patches = new ArrayList<>(this.itemVisuals.size());

        for (int i = 0; i < this.itemVisuals.size(); i++) {
            ItemVisual itemVisual = this.itemVisuals.get(i);

            for (int j = 0; j < BloodBodyPartType.MAX.index(); j++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(j);
                b.putBoolean(itemVisual.getBasicPatch(part) != 0.0F);
                b.putBoolean(itemVisual.getDenimPatch(part) != 0.0F);
                b.putBoolean(itemVisual.getLeatherPatch(part) != 0.0F);
                b.putBoolean(itemVisual.getHole(part) != 0.0F);
                b.putFloat(itemVisual.getDirt(part));
                b.putFloat(itemVisual.getBlood(part));
            }

            Clothing clothing = Type.tryCastTo(itemVisual.getInventoryItem(), Clothing.class);
            patches.clear();
            if (clothing != null && clothing.getPatchesNumber() > 0) {
                for (int j = 0; j < BloodBodyPartType.MAX.index(); j++) {
                    Clothing.ClothingPatch clothingPatch = clothing.getPatchType(BloodBodyPartType.FromIndex(j));
                    if (clothingPatch != null) {
                        SyncVisualsPacket.Patch patch = new SyncVisualsPacket.Patch();
                        patch.partIndex = j;
                        patch.patch = clothingPatch;
                        patches.add(patch);
                    }
                }
            }

            this.writePatches(b, patches);
            if (b.putBoolean(clothing != null)) {
                b.putFloat(clothing.getDirtiness());
                b.putFloat(clothing.getBloodLevel());
                b.putInt(clothing.getCondition());
            }
        }

        InventoryItem handWeapon = Type.tryCastTo(this.playerId.getPlayer().getPrimaryHandItem(), HandWeapon.class);
        if (b.putBoolean(handWeapon != null)) {
            b.putInt(handWeapon.getID());
            b.putFloat(handWeapon.getBloodLevel());
        }
    }

    private void writePatches(ByteBufferWriter b, ArrayList<SyncVisualsPacket.Patch> patches) {
        b.putByte(patches.size());

        for (SyncVisualsPacket.Patch patch : patches) {
            b.putByte(patch.partIndex);
            b.putByte(patch.patch.tailorLvl);
            b.putByte(patch.patch.fabricType);
            b.putBoolean(patch.patch.hasHole);
        }
    }

    private static final class Patch {
        int partIndex;
        Clothing.ClothingPatch patch;
    }
}
