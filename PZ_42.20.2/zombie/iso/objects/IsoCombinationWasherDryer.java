// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
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
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.logic = (IClothingWasherDryerLogic)(input.get() == 0 ? this.washer : this.dryer);
        this.washer.load(input, worldVersion, isDebugSave);
        this.dryer.load(input, worldVersion, isDebugSave);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put((byte)(this.logic == this.washer ? 0 : 1));
        this.washer.save(output, isDebugSave);
        this.dryer.save(output, isDebugSave);
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
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.MODE) {
            bb.putBoolean(this.isModeDryer());
        } else if (change == IsoObjectChange.USES_EXTERNAL_WATER_SOURCE) {
            bb.putBoolean(this.getUsesExternalWaterSource());
        } else {
            this.logic.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.MODE) {
            if (bb.getBoolean()) {
                this.setModeDryer();
            } else {
                this.setModeWasher();
            }
        } else if (change == IsoObjectChange.USES_EXTERNAL_WATER_SOURCE) {
            this.setUsesExternalWaterSource(bb.getBoolean());
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

    @Override
    public boolean couldBePoweredByGenerator() {
        return true;
    }

    @Override
    public float getGeneratorPowerConsumption() {
        return this.isActivated() ? 0.09F : 0.0F;
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
