// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@UsedFromLua
public final class ItemVisual {
    private String fullType;
    private String clothingItemName;
    private String alternateModelName;
    public static final float NULL_HUE = Float.POSITIVE_INFINITY;
    public float hue = Float.POSITIVE_INFINITY;
    public ImmutableColor tint;
    public int baseTexture = -1;
    public int textureChoice = -1;
    public String decal;
    private byte[] blood;
    private byte[] dirt;
    private byte[] holes;
    private byte[] basicPatches;
    private byte[] denimPatches;
    private byte[] leatherPatches;
    private InventoryItem inventoryItem;
    private static final int LASTSTAND_VERSION1 = 1;
    private static final int LASTSTAND_VERSION = 1;

    public ItemVisual() {
    }

    public ItemVisual(ItemVisual other) {
        this.copyFrom(other);
    }

    public void setItemType(String fullType) {
        Objects.requireNonNull(fullType);

        assert fullType.contains(".");

        this.fullType = fullType;
    }

    public String getItemType() {
        return this.fullType;
    }

    public void setAlternateModelName(String name) {
        this.alternateModelName = name;
    }

    public String getAlternateModelName() {
        return this.alternateModelName;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{ m_clothingItemName:\"" + this.clothingItemName + "\"}";
    }

    public String getClothingItemName() {
        return this.clothingItemName;
    }

    public void setClothingItemName(String name) {
        this.clothingItemName = name;
    }

    public Item getScriptItem() {
        return StringUtils.isNullOrWhitespace(this.fullType) ? null : ScriptManager.instance.getItem(this.fullType);
    }

    public ClothingItem getClothingItem() {
        Item scriptItem = this.getScriptItem();
        if (scriptItem == null) {
            return null;
        } else {
            if (!StringUtils.isNullOrWhitespace(this.alternateModelName)) {
                if ("LeftHand".equalsIgnoreCase(this.alternateModelName)) {
                    return scriptItem.replaceSecondHand.clothingItem;
                }

                if ("RightHand".equalsIgnoreCase(this.alternateModelName)) {
                    return scriptItem.replacePrimaryHand.clothingItem;
                }
            }

            return scriptItem.getClothingItemAsset();
        }
    }

    public void getClothingItemCombinedMask(CharacterMask in_out_mask) {
        ClothingItem.tryGetCombinedMask(this.getClothingItem(), in_out_mask);
    }

    public void copyVisualFrom(ItemVisual visual) {
        this.setHue(visual.getHue());
        this.setTint(visual.getTint());
        this.setBaseTexture(visual.getBaseTexture());
        this.setTextureChoice(visual.getTextureChoice());
        if (visual.decal != null) {
            this.setDecal(visual.decal);
        }
    }

    public void setHue(float hue) {
        hue = Math.max(hue, -1.0F);
        hue = Math.min(hue, 1.0F);
        this.hue = hue;
    }

    public float getHue() {
        return this.hue;
    }

    public float getHue(ClothingItem clothingItem) {
        if (clothingItem.allowRandomHue) {
            if (this.hue == Float.POSITIVE_INFINITY) {
                this.hue = OutfitRNG.Next(200) / 100.0F - 1.0F;
            }

            return this.hue;
        } else {
            return this.hue = 0.0F;
        }
    }

    public void setTint(ImmutableColor tint) {
        this.tint = tint;
    }

    public ImmutableColor getTint(ClothingItem clothingItem) {
        if (this.inventoryItem != null && this.inventoryItem.isCustomColor()) {
            InventoryItem item = this.inventoryItem;
            return new ImmutableColor(item.getR(), item.getG(), item.getB());
        } else if (clothingItem.allowRandomTint) {
            if (this.tint == null) {
                this.tint = OutfitRNG.randomImmutableColor();
            }

            return this.tint;
        } else {
            return this.tint = ImmutableColor.white;
        }
    }

    public ImmutableColor getTint() {
        return this.tint;
    }

    public String getBaseTexture(ClothingItem clothingItem) {
        if (clothingItem.baseTextures.isEmpty()) {
            this.baseTexture = -1;
            return null;
        } else {
            if (this.baseTexture < 0 || this.baseTexture >= clothingItem.baseTextures.size()) {
                this.baseTexture = OutfitRNG.Next(clothingItem.baseTextures.size());
            }

            return clothingItem.baseTextures.get(this.baseTexture);
        }
    }

    public String getTextureChoice(ClothingItem clothingItem) {
        if (clothingItem.textureChoices.isEmpty()) {
            this.textureChoice = -1;
            return null;
        } else {
            if (this.textureChoice < 0 || this.textureChoice >= clothingItem.textureChoices.size()) {
                this.textureChoice = OutfitRNG.Next(clothingItem.textureChoices.size());
            }

            return clothingItem.textureChoices.get(this.textureChoice);
        }
    }

    public void setDecal(String decalName) {
        this.decal = decalName;
    }

    public String getDecal(ClothingItem clothingItem) {
        if (StringUtils.isNullOrWhitespace(clothingItem.decalGroup)) {
            return this.decal = null;
        } else {
            if (this.decal == null) {
                this.decal = ClothingDecals.instance.getRandomDecal(clothingItem.decalGroup);
            }

            return this.decal;
        }
    }

    public void pickUninitializedValues(ClothingItem clothingItem) {
        if (clothingItem != null && clothingItem.isReady()) {
            this.getHue(clothingItem);
            this.getTint(clothingItem);
            this.getBaseTexture(clothingItem);
            this.getTextureChoice(clothingItem);
            this.getDecal(clothingItem);
        }
    }

    public void synchWithOutfit(ClothingItemReference itemRef) {
        ClothingItem clothingItem = itemRef.getClothingItem();
        this.clothingItemName = clothingItem.mame;
        this.hue = itemRef.randomData.hue;
        this.tint = itemRef.randomData.tint;
        this.baseTexture = clothingItem.baseTextures.indexOf(itemRef.randomData.baseTexture);
        this.textureChoice = clothingItem.textureChoices.indexOf(itemRef.randomData.textureChoice);
        this.decal = itemRef.randomData.decal;
    }

    public void clear() {
        this.fullType = null;
        this.clothingItemName = null;
        this.alternateModelName = null;
        this.hue = Float.POSITIVE_INFINITY;
        this.tint = null;
        this.baseTexture = -1;
        this.textureChoice = -1;
        this.decal = null;
        if (this.blood != null) {
            Arrays.fill(this.blood, (byte)0);
        }

        if (this.dirt != null) {
            Arrays.fill(this.dirt, (byte)0);
        }

        if (this.holes != null) {
            Arrays.fill(this.holes, (byte)0);
        }

        if (this.basicPatches != null) {
            Arrays.fill(this.basicPatches, (byte)0);
        }

        if (this.denimPatches != null) {
            Arrays.fill(this.denimPatches, (byte)0);
        }

        if (this.leatherPatches != null) {
            Arrays.fill(this.leatherPatches, (byte)0);
        }
    }

    public void copyFrom(ItemVisual other) {
        if (other == null) {
            this.clear();
        } else {
            ClothingItem clothingItem = other.getClothingItem();
            if (clothingItem != null) {
                other.pickUninitializedValues(clothingItem);
            }

            this.fullType = other.fullType;
            this.clothingItemName = other.clothingItemName;
            this.alternateModelName = other.alternateModelName;
            this.hue = other.hue;
            this.tint = other.tint;
            this.baseTexture = other.baseTexture;
            this.textureChoice = other.textureChoice;
            this.decal = other.decal;
            this.copyBlood(other);
            this.copyDirt(other);
            this.copyHoles(other);
            this.copyPatches(other);
        }
    }

    public void save(ByteBuffer output) throws IOException {
        byte flags1 = 0;
        if (this.tint != null) {
            flags1 = (byte)(flags1 | 1);
        }

        if (this.baseTexture != -1) {
            flags1 = (byte)(flags1 | 2);
        }

        if (this.textureChoice != -1) {
            flags1 = (byte)(flags1 | 4);
        }

        if (this.hue != Float.POSITIVE_INFINITY) {
            flags1 = (byte)(flags1 | 8);
        }

        if (!StringUtils.isNullOrWhitespace(this.decal)) {
            flags1 = (byte)(flags1 | 16);
        }

        output.put(flags1);
        GameWindow.WriteString(output, this.fullType);
        GameWindow.WriteString(output, this.alternateModelName);
        GameWindow.WriteString(output, this.clothingItemName);
        if (this.tint != null) {
            output.put(this.tint.getRedByte());
            output.put(this.tint.getGreenByte());
            output.put(this.tint.getBlueByte());
        }

        if (this.baseTexture != -1) {
            output.put((byte)this.baseTexture);
        }

        if (this.textureChoice != -1) {
            output.put((byte)this.textureChoice);
        }

        if (this.hue != Float.POSITIVE_INFINITY) {
            output.putFloat(this.hue);
        }

        if (!StringUtils.isNullOrWhitespace(this.decal)) {
            GameWindow.WriteString(output, this.decal);
        }

        if (this.blood != null) {
            output.put((byte)this.blood.length);

            for (int i = 0; i < this.blood.length; i++) {
                output.put(this.blood[i]);
            }
        } else {
            output.put((byte)0);
        }

        if (this.dirt != null) {
            output.put((byte)this.dirt.length);

            for (int i = 0; i < this.dirt.length; i++) {
                output.put(this.dirt[i]);
            }
        } else {
            output.put((byte)0);
        }

        if (this.holes != null) {
            output.put((byte)this.holes.length);

            for (int i = 0; i < this.holes.length; i++) {
                output.put(this.holes[i]);
            }
        } else {
            output.put((byte)0);
        }

        if (this.basicPatches != null) {
            output.put((byte)this.basicPatches.length);

            for (int i = 0; i < this.basicPatches.length; i++) {
                output.put(this.basicPatches[i]);
            }
        } else {
            output.put((byte)0);
        }

        if (this.denimPatches != null) {
            output.put((byte)this.denimPatches.length);

            for (int i = 0; i < this.denimPatches.length; i++) {
                output.put(this.denimPatches[i]);
            }
        } else {
            output.put((byte)0);
        }

        if (this.leatherPatches != null) {
            output.put((byte)this.leatherPatches.length);

            for (int i = 0; i < this.leatherPatches.length; i++) {
                output.put(this.leatherPatches[i]);
            }
        } else {
            output.put((byte)0);
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        int flags1 = input.get() & 255;
        this.fullType = GameWindow.ReadString(input);
        this.alternateModelName = GameWindow.ReadString(input);
        this.clothingItemName = GameWindow.ReadString(input);
        if ((flags1 & 1) != 0) {
            int r = input.get() & 255;
            int g = input.get() & 255;
            int b = input.get() & 255;
            this.tint = new ImmutableColor(r, g, b);
        }

        if ((flags1 & 2) != 0) {
            this.baseTexture = input.get();
        }

        if ((flags1 & 4) != 0) {
            this.textureChoice = input.get();
        }

        if ((flags1 & 8) != 0) {
            this.hue = input.getFloat();
        }

        if ((flags1 & 16) != 0) {
            this.decal = GameWindow.ReadString(input);
        }

        int count = input.get();
        if (count > 0 && this.blood == null) {
            this.blood = new byte[BloodBodyPartType.MAX.index()];
        }

        for (int i = 0; i < count; i++) {
            byte amount = input.get();
            if (i < this.blood.length) {
                this.blood[i] = amount;
            }
        }

        int var8 = input.get();
        if (var8 > 0 && this.dirt == null) {
            this.dirt = new byte[BloodBodyPartType.MAX.index()];
        }

        for (int ix = 0; ix < var8; ix++) {
            byte amount = input.get();
            if (ix < this.dirt.length) {
                this.dirt[ix] = amount;
            }
        }

        var8 = input.get();
        if (var8 > 0 && this.holes == null) {
            this.holes = new byte[BloodBodyPartType.MAX.index()];
        }

        for (int ixx = 0; ixx < var8; ixx++) {
            byte amount = input.get();
            if (ixx < this.holes.length) {
                this.holes[ixx] = amount;
            }
        }

        var8 = input.get();
        if (var8 > 0 && this.basicPatches == null) {
            this.basicPatches = new byte[BloodBodyPartType.MAX.index()];
        }

        for (int ixxx = 0; ixxx < var8; ixxx++) {
            byte amount = input.get();
            if (ixxx < this.basicPatches.length) {
                this.basicPatches[ixxx] = amount;
            }
        }

        var8 = input.get();
        if (var8 > 0 && this.denimPatches == null) {
            this.denimPatches = new byte[BloodBodyPartType.MAX.index()];
        }

        for (int ixxxx = 0; ixxxx < var8; ixxxx++) {
            byte amount = input.get();
            if (ixxxx < this.denimPatches.length) {
                this.denimPatches[ixxxx] = amount;
            }
        }

        var8 = input.get();
        if (var8 > 0 && this.leatherPatches == null) {
            this.leatherPatches = new byte[BloodBodyPartType.MAX.index()];
        }

        for (int ixxxxx = 0; ixxxxx < var8; ixxxxx++) {
            byte amount = input.get();
            if (ixxxxx < this.leatherPatches.length) {
                this.leatherPatches[ixxxxx] = amount;
            }
        }
    }

    public void setDenimPatch(BloodBodyPartType bodyPartType) {
        if (this.denimPatches == null) {
            this.denimPatches = new byte[BloodBodyPartType.MAX.index()];
        }

        this.denimPatches[bodyPartType.index()] = -1;
    }

    public float getDenimPatch(BloodBodyPartType bodyPartType) {
        return this.denimPatches == null ? 0.0F : (this.denimPatches[bodyPartType.index()] & 255) / 255.0F;
    }

    public void setLeatherPatch(BloodBodyPartType bodyPartType) {
        if (this.leatherPatches == null) {
            this.leatherPatches = new byte[BloodBodyPartType.MAX.index()];
        }

        this.leatherPatches[bodyPartType.index()] = -1;
    }

    public float getLeatherPatch(BloodBodyPartType bodyPartType) {
        return this.leatherPatches == null ? 0.0F : (this.leatherPatches[bodyPartType.index()] & 255) / 255.0F;
    }

    public void setBasicPatch(BloodBodyPartType bodyPartType) {
        if (this.basicPatches == null) {
            this.basicPatches = new byte[BloodBodyPartType.MAX.index()];
        }

        this.basicPatches[bodyPartType.index()] = -1;
    }

    public float getBasicPatch(BloodBodyPartType bodyPartType) {
        return this.basicPatches == null ? 0.0F : (this.basicPatches[bodyPartType.index()] & 255) / 255.0F;
    }

    public int getBasicPatchesNumber() {
        if (this.basicPatches == null) {
            return 0;
        } else {
            int totalbasicPatches = 0;

            for (int i = 0; i < this.basicPatches.length; i++) {
                if (this.basicPatches[i] != 0) {
                    totalbasicPatches++;
                }
            }

            return totalbasicPatches;
        }
    }

    public void setHole(BloodBodyPartType bodyPartType) {
        if (this.holes == null) {
            this.holes = new byte[BloodBodyPartType.MAX.index()];
        }

        this.holes[bodyPartType.index()] = -1;
    }

    public float getHole(BloodBodyPartType bodyPartType) {
        return this.holes == null ? 0.0F : (this.holes[bodyPartType.index()] & 255) / 255.0F;
    }

    public int getHolesNumber() {
        if (this.holes == null) {
            return 0;
        } else {
            int totalHoles = 0;

            for (int i = 0; i < this.holes.length; i++) {
                if (this.holes[i] != 0) {
                    totalHoles++;
                }
            }

            return totalHoles;
        }
    }

    public void setBlood(BloodBodyPartType bodyPartType, float amount) {
        if (this.blood == null) {
            this.blood = new byte[BloodBodyPartType.MAX.index()];
        }

        amount = Math.max(0.0F, Math.min(1.0F, amount));
        this.blood[bodyPartType.index()] = (byte)(amount * 255.0F);
    }

    public float getBlood(BloodBodyPartType bodyPartType) {
        return this.blood == null ? 0.0F : (this.blood[bodyPartType.index()] & 255) / 255.0F;
    }

    public float getDirt(BloodBodyPartType bodyPartType) {
        return this.dirt == null ? 0.0F : (this.dirt[bodyPartType.index()] & 255) / 255.0F;
    }

    public void setDirt(BloodBodyPartType bodyPartType, float amount) {
        if (this.dirt == null) {
            this.dirt = new byte[BloodBodyPartType.MAX.index()];
        }

        amount = Math.max(0.0F, Math.min(1.0F, amount));
        this.dirt[bodyPartType.index()] = (byte)(amount * 255.0F);
    }

    public void copyBlood(ItemVisual other) {
        if (other.blood != null) {
            if (this.blood == null) {
                this.blood = new byte[BloodBodyPartType.MAX.index()];
            }

            System.arraycopy(other.blood, 0, this.blood, 0, this.blood.length);
        } else if (this.blood != null) {
            Arrays.fill(this.blood, (byte)0);
        }
    }

    public void copyDirt(ItemVisual other) {
        if (other.dirt != null) {
            if (this.dirt == null) {
                this.dirt = new byte[BloodBodyPartType.MAX.index()];
            }

            System.arraycopy(other.dirt, 0, this.dirt, 0, this.dirt.length);
        } else if (this.dirt != null) {
            Arrays.fill(this.dirt, (byte)0);
        }
    }

    public void copyHoles(ItemVisual other) {
        if (other.holes != null) {
            if (this.holes == null) {
                this.holes = new byte[BloodBodyPartType.MAX.index()];
            }

            System.arraycopy(other.holes, 0, this.holes, 0, this.holes.length);
        } else if (this.holes != null) {
            Arrays.fill(this.holes, (byte)0);
        }
    }

    public void copyPatches(ItemVisual other) {
        if (other.basicPatches != null) {
            if (this.basicPatches == null) {
                this.basicPatches = new byte[BloodBodyPartType.MAX.index()];
            }

            System.arraycopy(other.basicPatches, 0, this.basicPatches, 0, this.basicPatches.length);
        } else if (this.basicPatches != null) {
            Arrays.fill(this.basicPatches, (byte)0);
        }

        if (other.denimPatches != null) {
            if (this.denimPatches == null) {
                this.denimPatches = new byte[BloodBodyPartType.MAX.index()];
            }

            System.arraycopy(other.denimPatches, 0, this.denimPatches, 0, this.denimPatches.length);
        } else if (this.denimPatches != null) {
            Arrays.fill(this.denimPatches, (byte)0);
        }

        if (other.leatherPatches != null) {
            if (this.leatherPatches == null) {
                this.leatherPatches = new byte[BloodBodyPartType.MAX.index()];
            }

            System.arraycopy(other.leatherPatches, 0, this.leatherPatches, 0, this.leatherPatches.length);
        } else if (this.leatherPatches != null) {
            Arrays.fill(this.leatherPatches, (byte)0);
        }
    }

    public void removeHole(int bodyPartIndex) {
        if (this.holes != null) {
            this.holes[bodyPartIndex] = 0;
        }
    }

    public void removePatch(int bodyPartIndex) {
        if (this.basicPatches != null) {
            this.basicPatches[bodyPartIndex] = 0;
        }

        if (this.denimPatches != null) {
            this.denimPatches[bodyPartIndex] = 0;
        }

        if (this.leatherPatches != null) {
            this.leatherPatches[bodyPartIndex] = 0;
        }
    }

    public void removeBlood() {
        if (this.blood != null) {
            Arrays.fill(this.blood, (byte)0);
        }
    }

    public void removeDirt() {
        if (this.dirt != null) {
            Arrays.fill(this.dirt, (byte)0);
        }
    }

    public float getTotalBlood() {
        float total = 0.0F;
        if (this.blood != null) {
            for (int i = 0; i < this.blood.length; i++) {
                total += (this.blood[i] & 255) / 255.0F;
            }
        }

        return total;
    }

    public InventoryItem getInventoryItem() {
        return this.inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public void setBaseTexture(int baseTexture) {
        this.baseTexture = baseTexture;
    }

    public int getBaseTexture() {
        return this.baseTexture;
    }

    public void setTextureChoice(int TextureChoice) {
        this.textureChoice = TextureChoice;
    }

    public int getTextureChoice() {
        return this.textureChoice;
    }

    private static StringBuilder toString(ImmutableColor color, StringBuilder sb) {
        sb.append(color.getRedByte() & 255);
        sb.append(",");
        sb.append(color.getGreenByte() & 255);
        sb.append(",");
        sb.append(color.getBlueByte() & 255);
        return sb;
    }

    private static ImmutableColor colorFromString(String str) {
        String[] ss = str.split(",");
        if (ss.length == 3) {
            try {
                int r = Integer.parseInt(ss[0]);
                int g = Integer.parseInt(ss[1]);
                int b = Integer.parseInt(ss[2]);
                return new ImmutableColor(r / 255.0F, g / 255.0F, b / 255.0F);
            } catch (NumberFormatException var5) {
            }
        }

        return null;
    }

    public String getLastStandString() {
        Item scriptItem = this.getScriptItem();
        if (scriptItem == null) {
            return null;
        } else {
            ClothingItem clothingItem = this.getClothingItem();
            if (clothingItem == null) {
                return null;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("version=");
                sb.append(1);
                sb.append(";");
                sb.append("type=");
                sb.append(this.inventoryItem.getFullType());
                sb.append(";");
                ImmutableColor tint = this.getTint(clothingItem);
                sb.append("tint=");
                toString(tint, sb);
                sb.append(";");
                int baseTexture = this.getBaseTexture();
                if (baseTexture != -1) {
                    sb.append("baseTexture=");
                    sb.append(baseTexture);
                    sb.append(";");
                }

                int textureChoice = this.getTextureChoice();
                if (textureChoice != -1) {
                    sb.append("textureChoice=");
                    sb.append(textureChoice);
                    sb.append(";");
                }

                float hue = this.getHue(clothingItem);
                if (hue != 0.0F) {
                    sb.append("hue=");
                    sb.append(hue);
                    sb.append(";");
                }

                String decal = this.getDecal(clothingItem);
                if (!StringUtils.isNullOrWhitespace(decal)) {
                    sb.append("decal=");
                    sb.append(decal);
                    sb.append(";");
                }

                return sb.toString();
            }
        }
    }

    public static InventoryItem createLastStandItem(String saveStr) {
        saveStr = saveStr.trim();
        if (!StringUtils.isNullOrWhitespace(saveStr) && saveStr.startsWith("version=")) {
            InventoryItem item = null;
            ItemVisual itemVisual = null;
            int version = -1;
            String[] ss = saveStr.split(";");
            if (ss.length >= 2 && ss[1].trim().startsWith("type=")) {
                for (int i = 0; i < ss.length; i++) {
                    int p = ss[i].indexOf(61);
                    if (p != -1) {
                        String key = ss[i].substring(0, p).trim();
                        String value = ss[i].substring(p + 1).trim();
                        switch (key) {
                            case "version":
                                version = Integer.parseInt(value);
                                if (version < 1 || version > 1) {
                                    return null;
                                }
                                break;
                            case "baseTexture":
                                try {
                                    itemVisual.setBaseTexture(Integer.parseInt(value));
                                } catch (NumberFormatException var14) {
                                }
                                break;
                            case "decal":
                                if (!StringUtils.isNullOrWhitespace(value)) {
                                    itemVisual.setDecal(value);
                                }
                                break;
                            case "hue":
                                try {
                                    itemVisual.setHue(Float.parseFloat(value));
                                } catch (NumberFormatException var13) {
                                }
                                break;
                            case "textureChoice":
                                try {
                                    itemVisual.setTextureChoice(Integer.parseInt(value));
                                } catch (NumberFormatException var12) {
                                }
                                break;
                            case "tint":
                                ImmutableColor tint = colorFromString(value);
                                if (tint != null) {
                                    itemVisual.setTint(tint);
                                }
                                break;
                            case "type":
                                item = InventoryItemFactory.CreateItem(value);
                                if (item == null) {
                                    return null;
                                }

                                itemVisual = item.getVisual();
                                if (itemVisual == null) {
                                    return null;
                                }
                        }
                    }
                }

                return item;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getDescription() {
        return "{ \"ItemVisual\" : { \"ItemType\" : " + this.getItemType() + " , \"m_clothingItemName\" : \"" + this.clothingItemName + "\" } }";
    }
}
