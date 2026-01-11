// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects.interfaces;

import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;

public interface IClothingWasherDryerLogic {
    void update();

    void saveChange(String change, KahluaTable tbl, ByteBuffer bb);

    void loadChange(String change, ByteBuffer bb);

    ItemContainer getContainer();

    boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item);

    boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item);

    boolean isActivated();

    void setActivated(boolean activated);

    void switchModeOn();

    void switchModeOff();
}
