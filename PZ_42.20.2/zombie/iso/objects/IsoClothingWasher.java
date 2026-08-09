// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public class IsoClothingWasher extends IsoObject {
    private final ClothingWasherLogic logic = new ClothingWasherLogic(this);

    public IsoClothingWasher(IsoCell cell) {
        super(cell);
    }

    public IsoClothingWasher(IsoCell cell, IsoGridSquare sq, IsoSprite gid) {
        super(cell, sq, gid);
    }

    @Override
    public String getObjectName() {
        return "ClothingWasher";
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.logic.load(input, worldVersion, isDebugSave);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        this.logic.save(output, isDebugSave);
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
        this.logic.saveChange(change, tbl, bb);
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        this.logic.loadChange(change, bb);
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
}
