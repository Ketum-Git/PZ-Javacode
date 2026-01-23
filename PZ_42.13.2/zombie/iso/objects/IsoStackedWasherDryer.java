// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public class IsoStackedWasherDryer extends IsoObject {
    private static final int DefaultCapacity = 20;
    private final ClothingWasherLogic washer = new ClothingWasherLogic(this);
    private final ClothingDryerLogic dryer = new ClothingDryerLogic(this);

    public IsoStackedWasherDryer(IsoCell cell) {
        super(cell);
    }

    public IsoStackedWasherDryer(IsoCell cell, IsoGridSquare sq, IsoSprite gid) {
        super(cell, sq, gid);
    }

    @Override
    public String getObjectName() {
        return "StackedWasherDryer";
    }

    @Override
    public void createContainersFromSpriteProperties() {
        super.createContainersFromSpriteProperties();
        PropertyContainer props = this.getProperties();
        if (props != null) {
            if (this.getContainerByType("clothingwasher") == null) {
                ItemContainer washer = new ItemContainer("clothingwasher", this.getSquare(), this);
                if (props.has("ContainerCapacity")) {
                    washer.capacity = PZMath.tryParseInt(props.get("ContainerCapacity"), 20);
                }

                if (this.getContainer() == null) {
                    this.setContainer(washer);
                } else {
                    this.addSecondaryContainer(washer);
                }
            }

            if (this.getContainerByType("clothingdryer") == null) {
                ItemContainer dryer = new ItemContainer("clothingdryer", this.getSquare(), this);
                if (props.has("ContainerCapacity")) {
                    dryer.capacity = PZMath.tryParseInt(props.get("ContainerCapacity"), 20);
                }

                if (this.getContainer() == null) {
                    this.setContainer(dryer);
                } else {
                    this.addSecondaryContainer(dryer);
                }
            }
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.washer.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.dryer.load(input, WorldVersion, IS_DEBUG_SAVE);
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        this.washer.save(output, IS_DEBUG_SAVE);
        this.dryer.save(output, IS_DEBUG_SAVE);
    }

    @Override
    public void update() {
        this.washer.update();
        this.dryer.update();
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
        this.washer.saveChange(change, tbl, bb);
        this.dryer.saveChange(change, tbl, bb);
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        this.washer.loadChange(change, bb);
        this.dryer.loadChange(change, bb);
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return this.washer.isItemAllowedInContainer(container, item) || this.dryer.isItemAllowedInContainer(container, item);
    }

    @Override
    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        return this.washer.isRemoveItemAllowedFromContainer(container, item) || this.dryer.isRemoveItemAllowedFromContainer(container, item);
    }

    public boolean isWasherActivated() {
        return this.washer.isActivated();
    }

    public void setWasherActivated(boolean activated) {
        this.washer.setActivated(activated);
    }

    public boolean isDryerActivated() {
        return this.dryer.isActivated();
    }

    public void setDryerActivated(boolean activated) {
        this.dryer.setActivated(activated);
    }
}
