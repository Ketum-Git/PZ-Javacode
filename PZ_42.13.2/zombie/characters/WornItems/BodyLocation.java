// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.WornItems;

import java.util.ArrayList;
import java.util.List;
import zombie.UsedFromLua;
import zombie.scripting.objects.ItemBodyLocation;

@UsedFromLua
public final class BodyLocation {
    private final BodyLocationGroup group;
    private final ItemBodyLocation id;
    private final List<ItemBodyLocation> exclusive = new ArrayList<>();
    private final List<ItemBodyLocation> hideModel = new ArrayList<>();
    private final List<ItemBodyLocation> altModel = new ArrayList<>();
    private boolean multiItem;

    public BodyLocation(BodyLocationGroup group, ItemBodyLocation id) {
        this.group = group;
        this.id = id;
    }

    public BodyLocation setExclusive(ItemBodyLocation itemBodyLocation) {
        if (this.exclusive.contains(itemBodyLocation)) {
            return this;
        } else {
            this.exclusive.add(itemBodyLocation);
            return this;
        }
    }

    public BodyLocation setHideModel(ItemBodyLocation itemBodyLocation) {
        if (this.hideModel.contains(itemBodyLocation)) {
            return this;
        } else {
            this.hideModel.add(itemBodyLocation);
            return this;
        }
    }

    public BodyLocation setAltModel(ItemBodyLocation itemBodyLocation) {
        if (this.altModel.contains(itemBodyLocation)) {
            return this;
        } else {
            this.altModel.add(itemBodyLocation);
            return this;
        }
    }

    public boolean isMultiItem() {
        return this.multiItem;
    }

    public BodyLocation setMultiItem(boolean bMultiItem) {
        this.multiItem = bMultiItem;
        return this;
    }

    public boolean isHideModel(ItemBodyLocation itemBodyLocation) {
        return this.hideModel.contains(itemBodyLocation);
    }

    public boolean isAltModel(ItemBodyLocation itemBodyLocation) {
        return this.altModel.contains(itemBodyLocation);
    }

    public boolean isExclusive(ItemBodyLocation itemBodyLocation) {
        return this.exclusive.contains(itemBodyLocation);
    }

    public boolean isId(ItemBodyLocation itemBodyLocation) {
        return this.id.equals(itemBodyLocation);
    }

    public ItemBodyLocation getId() {
        return this.id;
    }
}
