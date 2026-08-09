// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.AttachedItems;

import java.util.ArrayList;
import java.util.function.Consumer;
import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;

@UsedFromLua
public final class AttachedItems {
    protected final AttachedLocationGroup group;
    protected final ArrayList<AttachedItem> items = new ArrayList<>();

    public AttachedItems(AttachedLocationGroup group) {
        this.group = group;
    }

    public AttachedItems(AttachedItems other) {
        this.group = other.group;
        this.copyFrom(other);
    }

    public void copyFrom(AttachedItems other) {
        if (this.group != other.group) {
            throw new RuntimeException("group=" + this.group.id + " other.group=" + other.group.id);
        } else {
            this.items.clear();
            this.items.addAll(other.items);
        }
    }

    public AttachedLocationGroup getGroup() {
        return this.group;
    }

    public AttachedItem get(int index) {
        return this.items.get(index);
    }

    public void setItem(String location, InventoryItem item) {
        this.group.checkValid(location);
        int index = this.indexOf(location);
        if (index != -1) {
            this.items.remove(index);
        }

        if (item != null) {
            this.remove(item);
            int insertAt = this.items.size();

            for (int i = 0; i < this.items.size(); i++) {
                AttachedItem wornItem1 = this.items.get(i);
                if (this.group.indexOf(wornItem1.getLocation()) > this.group.indexOf(location)) {
                    insertAt = i;
                    break;
                }
            }

            AttachedItem wornItem = new AttachedItem(location, item);
            this.items.add(insertAt, wornItem);
        }
    }

    public InventoryItem getItem(String location) {
        this.group.checkValid(location);
        int index = this.indexOf(location);
        return index == -1 ? null : this.items.get(index).item;
    }

    public InventoryItem getItemByIndex(int index) {
        return index >= 0 && index < this.items.size() ? this.items.get(index).getItem() : null;
    }

    public void remove(InventoryItem item) {
        int index = this.indexOf(item);
        if (index != -1) {
            this.items.remove(index);
        }
    }

    public void clear() {
        this.items.clear();
    }

    public String getLocation(InventoryItem item) {
        int index = this.indexOf(item);
        return index == -1 ? null : this.items.get(index).getLocation();
    }

    public boolean contains(InventoryItem item) {
        return this.indexOf(item) != -1;
    }

    public int size() {
        return this.items.size();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public void forEach(Consumer<AttachedItem> c) {
        for (int i = 0; i < this.items.size(); i++) {
            c.accept(this.items.get(i));
        }
    }

    private int indexOf(String location) {
        for (int i = 0; i < this.items.size(); i++) {
            AttachedItem item = this.items.get(i);
            if (item.location.equals(location)) {
                return i;
            }
        }

        return -1;
    }

    private int indexOf(InventoryItem item) {
        for (int i = 0; i < this.items.size(); i++) {
            AttachedItem wornItem = this.items.get(i);
            if (wornItem.getItem() == item) {
                return i;
            }
        }

        return -1;
    }
}
