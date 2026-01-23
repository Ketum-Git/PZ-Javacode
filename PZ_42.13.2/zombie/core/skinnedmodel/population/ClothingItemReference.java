// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import zombie.core.ImmutableColor;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@XmlType(name = "ClothingItemReference")
public class ClothingItemReference implements Cloneable {
    @XmlElement(name = "probability")
    public float probability = 1.0F;
    @XmlElement(name = "itemGUID")
    public String itemGuid;
    @XmlElement(name = "subItems")
    public ArrayList<ClothingItemReference> subItems = new ArrayList<>();
    @XmlElement(name = "bRandomized")
    public boolean randomized;
    @XmlTransient
    public boolean immutable;
    @XmlTransient
    public final ClothingItemReference.RandomData randomData = new ClothingItemReference.RandomData();

    public void setModID(String modID) {
        this.itemGuid = modID + "-" + this.itemGuid;

        for (ClothingItemReference clothingItemReference : this.subItems) {
            clothingItemReference.setModID(modID);
        }
    }

    public ClothingItem getClothingItem() {
        String pickedItemGUID = this.itemGuid;
        if (!this.randomized) {
            throw new RuntimeException("not randomized yet");
        } else {
            if (this.randomData.pickedItemRef != null) {
                pickedItemGUID = this.randomData.pickedItemRef.itemGuid;
            }

            return OutfitManager.instance.getClothingItem(pickedItemGUID);
        }
    }

    public void randomize() {
        if (this.immutable) {
            throw new RuntimeException("trying to randomize an immutable ClothingItemReference");
        } else {
            this.randomData.reset();

            for (int i = 0; i < this.subItems.size(); i++) {
                ClothingItemReference itemRef = this.subItems.get(i);
                itemRef.randomize();
            }

            this.randomData.pickedItemRef = this.pickRandomItemInternal();
            this.randomized = true;
            ClothingItem item = this.getClothingItem();
            if (item == null) {
                this.randomData.active = false;
            } else {
                this.randomData.active = OutfitRNG.Next(0.0F, 1.0F) <= this.probability;
                if (item.allowRandomHue) {
                    this.randomData.hue = OutfitRNG.Next(200) / 100.0F - 1.0F;
                }

                if (item.allowRandomTint) {
                    this.randomData.tint = OutfitRNG.randomImmutableColor();
                } else {
                    this.randomData.tint = ImmutableColor.white;
                }

                this.randomData.baseTexture = OutfitRNG.pickRandom(item.baseTextures);
                this.randomData.textureChoice = OutfitRNG.pickRandom(item.textureChoices);
                if (!StringUtils.isNullOrWhitespace(item.decalGroup)) {
                    this.randomData.decal = ClothingDecals.instance.getRandomDecal(item.decalGroup);
                }
            }
        }
    }

    private ClothingItemReference pickRandomItemInternal() {
        if (this.subItems.isEmpty()) {
            return this;
        } else {
            int randomIndex = OutfitRNG.Next(this.subItems.size() + 1);
            if (randomIndex == 0) {
                return this;
            } else {
                ClothingItemReference randomItemRef = this.subItems.get(randomIndex - 1);
                return randomItemRef.randomData.pickedItemRef;
            }
        }
    }

    public ClothingItemReference clone() {
        try {
            ClothingItemReference clone = new ClothingItemReference();
            clone.probability = this.probability;
            clone.itemGuid = this.itemGuid;
            PZArrayUtil.copy(clone.subItems, this.subItems, ClothingItemReference::clone);
            return clone;
        } catch (CloneNotSupportedException var2) {
            throw new RuntimeException("ClothingItemReference clone failed.", var2);
        }
    }

    public static class RandomData {
        public boolean active = true;
        public float hue;
        public ImmutableColor tint = ImmutableColor.white;
        public String baseTexture;
        public ClothingItemReference pickedItemRef;
        public String textureChoice;
        public String decal;

        public void reset() {
            this.active = true;
            this.hue = 0.0F;
            this.tint = ImmutableColor.white;
            this.baseTexture = null;
            this.pickedItemRef = null;
            this.textureChoice = null;
            this.decal = null;
        }
    }
}
