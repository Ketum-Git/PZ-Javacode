// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects.interfaces;

import se.krka.kahlua.vm.KahluaTable;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;

public interface IClothingWasherDryerLogic {
    void update();

    void saveChange(IsoObjectChange var1, KahluaTable var2, ByteBufferWriter var3);

    void loadChange(IsoObjectChange var1, ByteBufferReader var2);

    ItemContainer getContainer();

    boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item);

    boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item);

    boolean isActivated();

    void setActivated(boolean activated);

    void switchModeOn();

    void switchModeOff();
}
