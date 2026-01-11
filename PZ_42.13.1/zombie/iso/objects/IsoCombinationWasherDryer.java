// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public class IsoCombinationWasherDryer extends IsoObject {
    private final ClothingWasherLogic washer = new ClothingWasherLogic(this);
    private final ClothingDryerLogic dryer = new ClothingDryerLogic(this);
    private IClothingWasherDryerLogic logic = this.washer;

    public IsoCombinationWasherDryer(IsoCell cell) {
        super(cell);
    }

    public IsoCombinationWasherDryer(IsoCell cell, IsoGridSquare sq, IsoSprite gid) {
        super(cell, sq, gid);
    }

    @Override
    public String getObjectName() {
        return "CombinationWasherDryer";
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.logic = (IClothingWasherDryerLogic)(input.get() == 0 ? this.washer : this.dryer);
        this.washer.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.dryer.load(input, WorldVersion, IS_DEBUG_SAVE);
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.logic == this.washer ? 0 : 1));
        this.washer.save(output, IS_DEBUG_SAVE);
        this.dryer.save(output, IS_DEBUG_SAVE);
    }

    @Override
    public void update() {
        this.logic.update();
    }

    @Override
    public void addToWorld() {
        IsoCell cell = this.getCell();
        cell.addToProcessIsoObject(this);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("mode".equals(change)) {
            bb.put((byte)(this.isModeWasher() ? 0 : 1));
        } else if ("usesExternalWaterSource".equals(change)) {
            bb.put((byte)(this.getUsesExternalWaterSource() ? 1 : 0));
        } else {
            this.logic.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("mode".equals(change)) {
            if (bb.get() == 0) {
                this.setModeWasher();
            } else {
                this.setModeDryer();
            }
        } else if ("usesExternalWaterSource".equals(change)) {
            this.setUsesExternalWaterSource(bb.get() == 1);
        } else {
            this.logic.loadChange(change, bb);
        }
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return this.logic.isItemAllowedInContainer(container, item);
    }

    @Override
    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        return this.logic.isRemoveItemAllowedFromContainer(container, item);
    }

    public boolean isActivated() {
        return this.logic.isActivated();
    }

    public void setActivated(boolean activated) {
        this.logic.setActivated(activated);
    }

    public void setModeWasher() {
        if (!this.isModeWasher()) {
            this.dryer.switchModeOff();
            this.logic = this.washer;
            this.getContainer().setType("clothingwasher");
            this.washer.switchModeOn();
            LuaEventManager.triggerEvent("OnContainerUpdate");
        }
    }

    public void setModeDryer() {
        if (!this.isModeDryer()) {
            this.washer.switchModeOff();
            this.logic = this.dryer;
            this.getContainer().setType("clothingdryer");
            this.dryer.switchModeOn();
            LuaEventManager.triggerEvent("OnContainerUpdate");
        }
    }

    public boolean isModeWasher() {
        return this.logic == this.washer;
    }

    public boolean isModeDryer() {
        return this.logic == this.dryer;
    }
}
