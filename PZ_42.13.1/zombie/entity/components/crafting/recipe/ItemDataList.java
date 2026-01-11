// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.Item;

@UsedFromLua
public class ItemDataList {
    private ItemDataList.ItemData[] dataElements;
    private int size;
    private int peak;
    private int unprocessed;

    public ItemDataList(int capacity) {
        this.dataElements = new ItemDataList.ItemData[capacity];

        for (int i = 0; i < capacity; i++) {
            this.dataElements[i] = new ItemDataList.ItemData();
        }
    }

    private void checkCapacity(int size) {
        if (size >= this.dataElements.length) {
            ItemDataList.ItemData[] old = this.dataElements;
            this.dataElements = new ItemDataList.ItemData[PZMath.max((int)(this.dataElements.length * 1.75F), size + 32)];
            System.arraycopy(old, 0, this.dataElements, 0, old.length);

            for (int i = old.length; i < this.dataElements.length; i++) {
                this.dataElements[i] = new ItemDataList.ItemData();
            }
        }
    }

    public int size() {
        return this.size;
    }

    public Item getItem(int index) {
        return this.get(index).item;
    }

    public InventoryItem getInventoryItem(int index) {
        return this.get(index).inventoryItem;
    }

    public void setProcessed(int index) {
        ItemDataList.ItemData data = this.get(index);
        if (!data.processed) {
            data.processed = true;
            this.unprocessed--;
        }
    }

    public boolean isProcessed(int index) {
        return this.get(index).processed;
    }

    public void getUnprocessed(ArrayList<InventoryItem> items) {
        this.getUnprocessed(items, false);
    }

    public void getUnprocessed(ArrayList<InventoryItem> items, boolean includeExisting) {
        for (int i = 0; i < this.size; i++) {
            ItemDataList.ItemData data = this.dataElements[i];
            if (!data.processed && (!data.existingItem || includeExisting)) {
                if (data.inventoryItem == null) {
                    DebugLog.General.warn("Cannot collect unprocessed, inventory item is null!");
                } else {
                    items.add(data.inventoryItem);
                }
            }
        }
    }

    public boolean hasUnprocessed() {
        return this.unprocessed > 0;
    }

    public void clear() {
        this.size = 0;
        this.unprocessed = 0;
    }

    public void reset() {
        for (int i = 0; i < this.peak; i++) {
            ItemDataList.ItemData data = this.dataElements[i];
            data.item = null;
            data.inventoryItem = null;
            data.processed = false;
        }

        this.size = 0;
        this.peak = 0;
        this.unprocessed = 0;
    }

    private ItemDataList.ItemData get(int index) {
        if (index >= 0 && index < this.size) {
            return this.dataElements[index];
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    private ItemDataList.ItemData getNextElement() {
        this.checkCapacity(this.size + 1);
        ItemDataList.ItemData data = this.dataElements[this.size++];
        if (this.size > this.peak) {
            this.peak = this.size;
        }

        data.processed = false;
        this.unprocessed++;
        return data;
    }

    public void addItem(InventoryItem inventoryItem) {
        this.addItem(inventoryItem, false);
    }

    public void addItem(InventoryItem inventoryItem, boolean existingItem) {
        ItemDataList.ItemData data = this.getNextElement();
        data.inventoryItem = inventoryItem;
        data.item = inventoryItem.getScriptItem();
        data.existingItem = existingItem;
    }

    public void addItem(Item item) {
        this.addItem(item, false);
    }

    public void addItem(Item item, boolean existingItem) {
        ItemDataList.ItemData data = this.getNextElement();
        data.inventoryItem = null;
        data.item = item;
        data.existingItem = existingItem;
    }

    private static class ItemData {
        private Item item;
        private InventoryItem inventoryItem;
        private boolean processed;
        private boolean existingItem;
    }
}
