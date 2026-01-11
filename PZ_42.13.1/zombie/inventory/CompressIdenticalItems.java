// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.InventoryContainer;

public final class CompressIdenticalItems {
    private static final int BLOCK_SIZE = 1024;
    private static final ThreadLocal<CompressIdenticalItems.PerThreadData> perThreadVars = new ThreadLocal<CompressIdenticalItems.PerThreadData>() {
        protected CompressIdenticalItems.PerThreadData initialValue() {
            return new CompressIdenticalItems.PerThreadData();
        }
    };

    private static int bufferSize(int size) {
        return (size + 1024 - 1) / 1024 * 1024;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb, int capacity) {
        if (bb == null || bb.capacity() < capacity) {
            bb = ByteBuffer.allocate(bufferSize(capacity));
        }

        return bb;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb) {
        if (bb == null) {
            return ByteBuffer.allocate(1024);
        } else if (bb.capacity() - bb.position() < 1024) {
            ByteBuffer newBB = ensureCapacity(null, bb.position() + 1024);
            return newBB.put(bb.array(), 0, bb.position());
        } else {
            ByteBuffer newBB = ensureCapacity(null, bb.capacity() + 1024);
            return newBB.put(bb.array(), 0, bb.position());
        }
    }

    private static boolean setCompareItem(CompressIdenticalItems.PerThreadData _perThreadVars, InventoryItem item1) throws IOException {
        ByteBuffer bb = _perThreadVars.itemCompareBuffer;
        bb.clear();
        int itemID1 = item1.id;
        item1.id = 0;

        try {
            while (true) {
                try {
                    bb.putInt(0);
                    item1.save(bb, false);
                    int item1End = bb.position();
                    bb.position(0);
                    bb.putInt(item1End);
                    bb.position(item1End);
                    return true;
                } catch (BufferOverflowException var8) {
                    bb = ensureCapacity(bb);
                    bb.clear();
                    _perThreadVars.itemCompareBuffer = bb;
                }
            }
        } finally {
            item1.id = itemID1;
        }
    }

    private static boolean areItemsIdentical(CompressIdenticalItems.PerThreadData _perThreadVars, InventoryItem item1, InventoryItem item2) throws IOException {
        if (item1 instanceof InventoryContainer inventoryContainer) {
            ItemContainer container1 = inventoryContainer.getInventory();
            ItemContainer container2 = ((InventoryContainer)item2).getInventory();
            if (!container1.getItems().isEmpty() || !container2.getItems().isEmpty()) {
                return false;
            }
        }

        if (item1.getAttributes() != null && item2.getAttributes() != null && !item1.getAttributes().isIdenticalTo(item2.getAttributes())) {
            return false;
        } else if ((item1.getAttributes() == null || item2.getAttributes() != null) && (item1.getAttributes() != null || item2.getAttributes() == null)) {
            ByteBuffer byteData1 = item1.getByteData();
            ByteBuffer byteData2 = item2.getByteData();
            if (byteData1 != null) {
                assert byteData1.position() == 0;

                if (!byteData1.equals(byteData2)) {
                    return false;
                }
            } else if (byteData2 != null) {
                return false;
            }

            ByteBuffer bb = null;
            int itemID2 = item2.id;
            item2.id = 0;

            while (true) {
                try {
                    bb = _perThreadVars.itemCompareBuffer;
                    bb.position(0);
                    int item1End = bb.getInt();
                    int item1Start = bb.position();
                    bb.position(item1End);
                    int item2Start = bb.position();
                    item2.save(bb, false);
                    int item2End = bb.position();
                    if (item2End - item2Start != item1End - item1Start) {
                        return false;
                    }

                    for (int offset = 0; offset < item1End - item1Start; offset++) {
                        if (bb.get(item1Start + offset) != bb.get(item2Start + offset)) {
                            return false;
                        }
                    }

                    return true;
                } catch (BufferOverflowException var16) {
                    bb = ensureCapacity(bb);
                    bb.clear();
                    _perThreadVars.itemCompareBuffer = bb;
                    setCompareItem(_perThreadVars, item1);
                } finally {
                    item2.id = itemID2;
                }
            }
        } else {
            return false;
        }
    }

    public static ArrayList<InventoryItem> save(ByteBuffer output, ArrayList<InventoryItem> items, IsoGameCharacter noCompress) throws IOException {
        CompressIdenticalItems.PerThreadData _perThreadVars = perThreadVars.get();
        CompressIdenticalItems.PerCallData _saveVars = _perThreadVars.allocSaveVars();
        HashMap<String, ArrayList<InventoryItem>> typeToItems = _saveVars.typeToItems;
        ArrayList<String> types = _saveVars.types;

        try {
            for (int i = 0; i < items.size(); i++) {
                String type = items.get(i).getFullType();
                if (!typeToItems.containsKey(type)) {
                    typeToItems.put(type, _saveVars.allocItemList());
                    types.add(type);
                }

                typeToItems.get(type).add(items.get(i));
            }

            int posSize = output.position();
            output.putShort((short)0);
            int itemCount = 0;

            for (int k = 0; k < types.size(); k++) {
                ArrayList<InventoryItem> saveItems = typeToItems.get(types.get(k));

                for (int m = 0; m < saveItems.size(); m++) {
                    InventoryItem item = saveItems.get(m);
                    _saveVars.savedItems.add(item);
                    int identical = 1;
                    int startM = m + 1;
                    if (noCompress == null || !noCompress.isEquipped(item)) {
                        setCompareItem(_perThreadVars, item);

                        while (m + 1 < saveItems.size() && areItemsIdentical(_perThreadVars, item, saveItems.get(m + 1))) {
                            _saveVars.savedItems.add(saveItems.get(m + 1));
                            m++;
                            identical++;
                        }
                    }

                    output.putInt(identical);
                    item.saveWithSize(output, false);
                    if (identical > 1) {
                        for (int i = startM; i <= m; i++) {
                            output.putInt(saveItems.get(i).id);
                        }
                    }

                    itemCount++;
                }
            }

            int posCurrent = output.position();
            output.position(posSize);
            output.putShort((short)itemCount);
            output.position(posCurrent);
        } finally {
            _saveVars.next = _perThreadVars.saveVars;
            _perThreadVars.saveVars = _saveVars;
        }

        return _saveVars.savedItems;
    }

    public static ArrayList<InventoryItem> load(
        ByteBuffer input, int WorldVersion, ArrayList<InventoryItem> Items, ArrayList<InventoryItem> IncludingObsoleteItems
    ) throws IOException {
        CompressIdenticalItems.PerThreadData _perThreadVars = perThreadVars.get();
        CompressIdenticalItems.PerCallData _saveVars = _perThreadVars.allocSaveVars();
        if (Items != null) {
            Items.clear();
        }

        if (IncludingObsoleteItems != null) {
            IncludingObsoleteItems.clear();
        }

        try {
            short count = input.getShort();

            for (int n = 0; n < count; n++) {
                int identical = input.getInt();
                int itemStart = input.position();
                InventoryItem item = InventoryItem.loadItem(input, WorldVersion);
                if (item == null) {
                    int idListBytes = identical > 1 ? (identical - 1) * 4 : 0;
                    input.position(input.position() + idListBytes);

                    for (int i = 0; i < identical; i++) {
                        if (IncludingObsoleteItems != null) {
                            IncludingObsoleteItems.add(null);
                        }

                        _saveVars.savedItems.add(null);
                    }
                } else {
                    for (int i = 0; i < identical; i++) {
                        if (i > 0) {
                            input.position(itemStart);
                            item = InventoryItem.loadItem(input, WorldVersion);
                        }

                        if (Items != null) {
                            Items.add(item);
                        }

                        if (IncludingObsoleteItems != null) {
                            IncludingObsoleteItems.add(item);
                        }

                        _saveVars.savedItems.add(item);
                    }

                    for (int i = 1; i < identical; i++) {
                        int id = input.getInt();
                        item = _saveVars.savedItems.get(_saveVars.savedItems.size() - identical + i);
                        if (item != null) {
                            item.id = id;
                        }
                    }
                }
            }
        } finally {
            _saveVars.next = _perThreadVars.saveVars;
            _perThreadVars.saveVars = _saveVars;
        }

        return _saveVars.savedItems;
    }

    public static void save(ByteBuffer output, InventoryItem item) throws IOException {
        output.putShort((short)1);
        output.putInt(1);
        item.saveWithSize(output, false);
    }

    private static class PerCallData {
        final ArrayList<String> types = new ArrayList<>();
        final HashMap<String, ArrayList<InventoryItem>> typeToItems = new HashMap<>();
        final ArrayDeque<ArrayList<InventoryItem>> itemLists = new ArrayDeque<>();
        final ArrayList<InventoryItem> savedItems = new ArrayList<>();
        CompressIdenticalItems.PerCallData next;

        void reset() {
            for (int i = 0; i < this.types.size(); i++) {
                ArrayList<InventoryItem> itemList = this.typeToItems.get(this.types.get(i));
                itemList.clear();
                this.itemLists.push(itemList);
            }

            this.types.clear();
            this.typeToItems.clear();
            this.savedItems.clear();
        }

        ArrayList<InventoryItem> allocItemList() {
            return this.itemLists.isEmpty() ? new ArrayList<>() : this.itemLists.pop();
        }
    }

    private static class PerThreadData {
        CompressIdenticalItems.PerCallData saveVars;
        ByteBuffer itemCompareBuffer = ByteBuffer.allocate(1024);

        CompressIdenticalItems.PerCallData allocSaveVars() {
            if (this.saveVars == null) {
                return new CompressIdenticalItems.PerCallData();
            } else {
                CompressIdenticalItems.PerCallData ret = this.saveVars;
                ret.reset();
                this.saveVars = this.saveVars.next;
                return ret;
            }
        }
    }
}
