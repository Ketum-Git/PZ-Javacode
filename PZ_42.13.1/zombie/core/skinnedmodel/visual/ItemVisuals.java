// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.skinnedmodel.population.ClothingItem;

@UsedFromLua
public final class ItemVisuals extends ArrayList<ItemVisual> {
    public void save(ByteBuffer output) throws IOException {
        output.putShort((short)this.size());

        for (int i = 0; i < this.size(); i++) {
            this.get(i).save(output);
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.clear();
        int count = input.getShort();

        for (int i = 0; i < count; i++) {
            ItemVisual itemVisual = new ItemVisual();
            itemVisual.load(input, WorldVersion);
            this.add(itemVisual);
        }
    }

    public ItemVisual findHat() {
        for (int i = 0; i < this.size(); i++) {
            ItemVisual itemVisual = this.get(i);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem != null && clothingItem.isHat()) {
                return itemVisual;
            }
        }

        return null;
    }

    public ItemVisual findMask() {
        for (int i = 0; i < this.size(); i++) {
            ItemVisual itemVisual = this.get(i);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem != null && clothingItem.isMask()) {
                return itemVisual;
            }
        }

        return null;
    }

    private boolean contains(String itemType) {
        for (ItemVisual item : this) {
            if (item.getItemType().equals(itemType)) {
                return true;
            }
        }

        return false;
    }

    public String getDescription() {
        String s = "{ \"ItemVisuals\" : [ ";

        for (int i = 0; i < this.size(); i++) {
            s = s + this.get(i).getDescription();
            if (i < this.size() - 1) {
                s = s + ", ";
            }
        }

        return s + " ] }";
    }
}
