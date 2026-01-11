// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 1, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncItemFieldsPacket implements INetworkPacket {
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    ContainerID containerId = new ContainerID();
    @JSONField
    int itemId;
    @JSONField
    int condition;
    @JSONField
    int headCondition;
    @JSONField
    int remoteControlId;
    @JSONField
    int uses;
    @JSONField
    int currentAmmoCount;
    @JSONField
    int haveBeenRepaired;
    @JSONField
    boolean taintedWater;
    @JSONField
    float wetness;
    @JSONField
    float dirtyness;
    @JSONField
    float bloodLevel;
    @JSONField
    float hungChange;
    @JSONField
    float weight;
    @JSONField
    int alreadyReadPages;
    @JSONField
    HashMap<Integer, String> customPages;
    @JSONField
    boolean isCustomName;
    @JSONField
    String customName;
    @JSONField
    int attachedSlot;
    @JSONField
    String attachedSlotType;
    @JSONField
    String attachedToModel;
    @JSONField
    FluidContainer fluidContainer;
    @JSONField
    boolean hasFluidContainer;
    @JSONField
    float sharpness;
    @JSONField
    boolean hasSharpness;
    @JSONField
    float actualWeight;
    @JSONField
    boolean isFavorite;
    @JSONField
    KahluaTable moddata;
    @JSONField
    ItemVisual itemVisual;
    @JSONField
    List<SyncItemFieldsPacket.Patch> patches;

    @Override
    public void setData(Object... values) {
        if (values.length != 2) {
            DebugLog.Multiplayer.error(this.getClass().getSimpleName() + ".set get invalid arguments");
        } else {
            this.playerId.set((IsoPlayer)values[0]);
            InventoryItem item = (InventoryItem)values[1];
            this.containerId.set(item.getContainer());
            this.itemId = item.getID();
            this.condition = item.getCondition();
            this.headCondition = item.getHeadCondition();
            this.remoteControlId = item.getRemoteControlID();
            this.uses = item.getCurrentUses();
            this.currentAmmoCount = item.getCurrentAmmoCount();
            this.haveBeenRepaired = item.getHaveBeenRepaired();
            this.weight = item.getWeight();
            this.isFavorite = item.isFavorite();
            this.isCustomName = item.isCustomName();
            this.customName = item.getName();
            this.attachedSlot = item.getAttachedSlot();
            this.attachedSlotType = item.getAttachedSlotType();
            this.attachedToModel = item.getAttachedToModel();
            if (item instanceof Clothing clothing) {
                this.wetness = item.getWetness();
                this.dirtyness = clothing.getDirtyness();
                this.bloodLevel = item.getBloodLevel();
            }

            if (item instanceof HandWeapon) {
                this.bloodLevel = item.getBloodLevel();
            }

            if (item instanceof Food food) {
                this.hungChange = food.getHungChange();
                this.actualWeight = food.getActualWeight();
            }

            if (item instanceof Literature literature) {
                this.alreadyReadPages = literature.getAlreadyReadPages();
                this.customPages = literature.getCustomPages();
            } else {
                this.customPages = null;
            }

            this.fluidContainer = item.getComponent(ComponentType.FluidContainer);
            this.hasFluidContainer = this.fluidContainer != null;
            this.hasSharpness = item.hasSharpness();
            if (this.hasSharpness) {
                this.sharpness = item.getSharpness();
            }

            this.moddata = item.hasModData() ? item.getModData() : null;
            this.itemVisual = null;
            this.patches = null;
            if (item instanceof Clothing clothing) {
                this.itemVisual = clothing.getVisual();
                if (clothing.getPatchesNumber() > 0) {
                    this.patches = new ArrayList<>(clothing.getPatchesNumber());

                    for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                        Clothing.ClothingPatch clothingPatch = clothing.getPatchType(BloodBodyPartType.FromIndex(i));
                        if (clothingPatch != null) {
                            SyncItemFieldsPacket.Patch patch = new SyncItemFieldsPacket.Patch();
                            patch.partIndex = i;
                            patch.patch = clothingPatch;
                            this.patches.add(patch);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.containerId.write(b);
        b.putInt(this.itemId);
        b.putInt(this.condition);
        b.putInt(this.headCondition);
        b.putInt(this.remoteControlId);
        b.putInt(this.uses);
        b.putInt(this.currentAmmoCount);
        b.putInt(this.haveBeenRepaired);
        b.putByte((byte)(this.taintedWater ? 1 : 0));
        b.putFloat(this.wetness);
        b.putFloat(this.dirtyness);
        b.putFloat(this.bloodLevel);
        b.putFloat(this.hungChange);
        b.putFloat(this.actualWeight);
        b.putFloat(this.weight);
        b.putInt(this.alreadyReadPages);
        b.putByte((byte)(this.isCustomName ? 1 : 0));
        if (this.isCustomName) {
            b.putUTF(this.customName);
        }

        if (this.customPages != null) {
            b.putByte((byte)this.customPages.size());
            this.customPages.forEach((idx, pageStr) -> b.putUTF(pageStr));
        } else {
            b.putByte((byte)0);
        }

        b.putInt(this.attachedSlot);
        b.putUTF(this.attachedSlotType);
        b.putUTF(this.attachedToModel);
        b.putByte((byte)(this.hasFluidContainer ? 1 : 0));
        if (this.hasFluidContainer) {
            try {
                this.fluidContainer.save(b.bb);
            } catch (IOException var4) {
                throw new RuntimeException(var4);
            }
        }

        if (this.hasSharpness) {
            b.putFloat(this.sharpness);
        }

        if (this.moddata != null) {
            b.putByte((byte)1);

            try {
                this.moddata.save(b.bb);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        } else {
            b.putByte((byte)0);
        }

        b.putByte((byte)(this.isFavorite ? 1 : 0));
        this.writeItemVisual(b);
        this.writePatches(b);
    }

    private void writeItemVisual(ByteBufferWriter b) {
        b.putBoolean(this.itemVisual != null);
        if (this.itemVisual != null) {
            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
                int flags = 0;
                if (this.itemVisual.getBasicPatch(part) != 0.0F) {
                    flags |= 1;
                }

                if (this.itemVisual.getDenimPatch(part) != 0.0F) {
                    flags |= 2;
                }

                if (this.itemVisual.getLeatherPatch(part) != 0.0F) {
                    flags |= 4;
                }

                if (this.itemVisual.getHole(part) != 0.0F) {
                    flags |= 8;
                }

                float dirt = this.itemVisual.getDirt(part);
                if (dirt > 0.0F) {
                    flags |= 16;
                }

                float blood = this.itemVisual.getBlood(part);
                if (blood > 0.0F) {
                    flags |= 32;
                }

                b.putByte((byte)flags);
                if (dirt > 0.0F) {
                    b.putFloat(dirt);
                }

                if (blood > 0.0F) {
                    b.putFloat(blood);
                }
            }
        }
    }

    private void writePatches(ByteBufferWriter b) {
        if (this.patches == null) {
            b.putByte((byte)0);
        } else {
            b.putByte((byte)this.patches.size());

            for (SyncItemFieldsPacket.Patch patch : this.patches) {
                b.putByte((byte)patch.partIndex);
                b.putByte((byte)patch.patch.tailorLvl);
                b.putByte((byte)patch.patch.fabricType);
                b.putBoolean(patch.patch.hasHole);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.containerId.parse(b, connection);
        this.itemId = b.getInt();
        this.condition = b.getInt();
        this.headCondition = b.getInt();
        this.remoteControlId = b.getInt();
        this.uses = b.getInt();
        this.currentAmmoCount = b.getInt();
        this.haveBeenRepaired = b.getInt();
        this.taintedWater = b.get() != 0;
        this.wetness = b.getFloat();
        this.dirtyness = b.getFloat();
        this.bloodLevel = b.getFloat();
        this.hungChange = b.getFloat();
        this.actualWeight = b.getFloat();
        this.weight = b.getFloat();
        this.alreadyReadPages = b.getInt();
        this.isCustomName = b.get() != 0;
        if (this.isCustomName) {
            this.customName = GameWindow.ReadString(b);
        }

        byte pageSize = b.get();
        if (pageSize > 0 && this.customPages == null) {
            this.customPages = new HashMap<>();
        }

        if (this.customPages != null) {
            this.customPages.clear();
        }

        for (int i = 0; i < pageSize; i++) {
            String pageStr = GameWindow.ReadString(b);
            this.customPages.put(i, pageStr);
        }

        this.attachedSlot = b.getInt();
        this.attachedSlotType = GameWindow.ReadString(b);
        this.attachedToModel = GameWindow.ReadString(b);
        this.hasFluidContainer = b.get() != 0;
        if (this.containerId.getContainer() != null) {
            InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
            if (this.hasFluidContainer) {
                this.fluidContainer = item.getComponent(ComponentType.FluidContainer);
                if (this.fluidContainer != null) {
                    try {
                        this.fluidContainer.load(b, IsoWorld.getWorldVersion());
                    } catch (IOException var8) {
                        throw new RuntimeException(var8);
                    }
                }
            } else {
                this.fluidContainer = null;
            }

            if (item.hasSharpness()) {
                this.sharpness = b.getFloat();
            }

            this.moddata = null;
            byte hasModData = b.get();
            if (hasModData > 0) {
                this.moddata = LuaManager.platform.newTable();

                try {
                    this.moddata.load(b, 240);
                } catch (IOException var7) {
                    ExceptionLogger.logException(var7);
                }
            }

            this.isFavorite = b.get() != 0;
            this.readItemVisual(b);
            this.readPatches(b);
        }
    }

    private void readItemVisual(ByteBuffer b) {
        this.itemVisual = null;
        boolean hasVisual = b.get() != 0;
        if (hasVisual) {
            this.itemVisual = new ItemVisual();

            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
                int flags = b.get() & 255;
                if ((flags & 1) != 0) {
                    this.itemVisual.setBasicPatch(part);
                }

                if ((flags & 2) != 0) {
                    this.itemVisual.setDenimPatch(part);
                }

                if ((flags & 4) != 0) {
                    this.itemVisual.setLeatherPatch(part);
                }

                if ((flags & 8) != 0) {
                    this.itemVisual.setHole(part);
                }

                if ((flags & 16) != 0) {
                    this.itemVisual.setDirt(part, b.getFloat());
                }

                if ((flags & 32) != 0) {
                    this.itemVisual.setBlood(part, b.getFloat());
                }
            }
        }
    }

    private void readPatches(ByteBuffer b) {
        this.patches = null;
        byte patchesNum = b.get();
        if (patchesNum > 0) {
            this.patches = new ArrayList<>();

            for (byte i = 0; i < patchesNum; i++) {
                byte bloodBodyPartTypeIdx = b.get();
                byte tailorLvl = b.get();
                byte fabricType = b.get();
                boolean hasHole = b.get() == 1;
                SyncItemFieldsPacket.Patch patch = new SyncItemFieldsPacket.Patch();
                patch.partIndex = bloodBodyPartTypeIdx;
                patch.patch = new Clothing.ClothingPatch();
                patch.patch.fabricType = fabricType;
                patch.patch.tailorLvl = tailorLvl;
                patch.patch.hasHole = hasHole;
                this.patches.add(patch);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
        item.setCondition(this.condition);
        if (item.hasHeadCondition()) {
            item.setHeadCondition(this.headCondition);
        }

        item.setRemoteControlID(this.remoteControlId);
        item.setCurrentUses(this.uses);
        item.setCurrentAmmoCount(this.currentAmmoCount);
        item.setHaveBeenRepaired(this.haveBeenRepaired);
        item.setWeight(this.weight);
        item.setCustomName(this.isCustomName);
        item.setFavorite(this.isFavorite);
        if (this.isCustomName) {
            item.setName(this.customName);
        }

        if (item instanceof Clothing clothing) {
            clothing.setWetness(this.wetness);
            clothing.setDirtyness(this.dirtyness);
            item.setBloodLevel(this.bloodLevel);
        }

        if (item instanceof HandWeapon) {
            item.setBloodLevel(this.bloodLevel);
        }

        if (item instanceof Food food) {
            food.setHungChange(this.hungChange);
            food.setActualWeight(this.actualWeight);
        }

        if (item instanceof Literature literature) {
            literature.setAlreadyReadPages(this.alreadyReadPages);
            literature.setCustomPages(this.customPages);
            this.setAlreadyReadPagesForCharacter(item);
        }

        item.setAttachedSlot(this.attachedSlot);
        item.setAttachedSlotType(this.attachedSlotType);
        item.setAttachedToModel(this.attachedToModel);
        if (item.hasSharpness()) {
            item.setSharpness(this.sharpness);
        }

        this.processModData();
        this.processClothing(item);
        if (this.playerId.getPlayer() != null) {
            this.playerId.getPlayer().resetModelNextFrame();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
        item.setCondition(this.condition);
        if (item.hasHeadCondition()) {
            item.setHeadCondition(this.headCondition);
        }

        item.setRemoteControlID(this.remoteControlId);
        item.setCurrentAmmoCount(this.currentAmmoCount);
        item.setHaveBeenRepaired(this.haveBeenRepaired);
        item.setCustomName(this.isCustomName);
        item.setFavorite(this.isFavorite);
        if (this.isCustomName) {
            item.setName(this.customName);
        }

        if (item instanceof Literature literature) {
            literature.setAlreadyReadPages(this.alreadyReadPages);
            literature.setCustomPages(this.customPages);
            this.setAlreadyReadPagesForCharacter(item);
        }

        item.setAttachedSlot(this.attachedSlot);
        item.setAttachedSlotType(this.attachedSlotType);
        item.setAttachedToModel(this.attachedToModel);
        this.processClothing(item);
        if (item.getCondition() == item.getConditionMax() && item instanceof Clothing clothing && connection.role.hasCapability(Capability.EditItem)) {
            clothing.fullyRestore();
        }

        this.processModData();
    }

    private void processModData() {
        InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
        if (item != null) {
            if (this.moddata != null) {
                if (item.hasModData()) {
                    item.getModData().wipe();
                }

                KahluaTableIterator iterator = this.moddata.iterator();

                while (iterator.advance()) {
                    Object key = iterator.getKey();
                    item.getModData().rawset(key, this.moddata.rawget(key));
                }
            }
        }
    }

    private void processClothing(InventoryItem item) {
        if (item instanceof Clothing clothing) {
            clothing.removeAllPatches();
            if (this.patches != null) {
                for (SyncItemFieldsPacket.Patch patch : this.patches) {
                    clothing.addPatchForSync(patch.partIndex, patch.patch.tailorLvl, patch.patch.fabricType, patch.patch.hasHole);
                }
            }

            ItemVisual var5 = clothing.getVisual();
            if (var5 instanceof ItemVisual && this.itemVisual != null) {
                var5.copyBlood(this.itemVisual);
                var5.copyDirt(this.itemVisual);
                var5.copyHoles(this.itemVisual);
                var5.copyPatches(this.itemVisual);
            }
        }
    }

    private void setAlreadyReadPagesForCharacter(InventoryItem book) {
        if (this.containerId.getContainer().parent instanceof IsoGameCharacter isoGameCharacter) {
            isoGameCharacter.setAlreadyReadPages(book.getFullType(), this.alreadyReadPages);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.containerId.getContainer() != null && this.containerId.getContainer().getItemWithID(this.itemId) != null;
    }

    private static final class Patch {
        int partIndex;
        Clothing.ClothingPatch patch;
    }
}
