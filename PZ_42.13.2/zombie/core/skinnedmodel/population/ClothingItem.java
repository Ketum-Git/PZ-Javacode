// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.util.StringUtils;

@UsedFromLua
public final class ClothingItem extends Asset {
    public String guid;
    public String maleModel;
    public String femaleModel;
    public String altMaleModel;
    public String altFemaleModel;
    public boolean isStatic;
    public ArrayList<String> baseTextures = new ArrayList<>();
    public String attachBone;
    public ArrayList<Integer> masks = new ArrayList<>();
    public String masksFolder = "media/textures/Body/Masks";
    public String underlayMasksFolder = "media/textures/Body/Masks";
    public ArrayList<String> textureChoices = new ArrayList<>();
    public boolean allowRandomHue;
    public boolean allowRandomTint;
    public String decalGroup;
    public String shader;
    public String hatCategory;
    public ArrayList<String> spawnWith = new ArrayList<>();
    public static final String s_masksFolderDefault = "media/textures/Body/Masks";
    public String mame;
    public static final AssetType ASSET_TYPE = new AssetType("ClothingItem");

    public ClothingItem(AssetPath path, AssetManager assetManager) {
        super(path, assetManager);
    }

    public ArrayList<String> getBaseTextures() {
        return this.baseTextures;
    }

    public ArrayList<String> getTextureChoices() {
        return this.textureChoices;
    }

    public String GetATexture() {
        return this.textureChoices.isEmpty() ? null : OutfitRNG.pickRandom(this.textureChoices);
    }

    public ArrayList<String> getSpawnWith() {
        return this.spawnWith;
    }

    public boolean getAllowRandomHue() {
        return this.allowRandomHue;
    }

    public boolean getAllowRandomTint() {
        return this.allowRandomTint;
    }

    public String getDecalGroup() {
        return this.decalGroup;
    }

    public boolean isHat() {
        return !StringUtils.isNullOrWhitespace(this.hatCategory) && !"nobeard".equals(this.hatCategory);
    }

    public boolean isMask() {
        return !StringUtils.isNullOrWhitespace(this.hatCategory) && ("nohairnobeard".equals(this.hatCategory) || !this.hatCategory.contains("hair"));
    }

    public void getCombinedMask(CharacterMask in_out_mask) {
        in_out_mask.setPartsVisible(this.masks, false);
    }

    public boolean hasModel() {
        return !StringUtils.isNullOrWhitespace(this.maleModel) && !StringUtils.isNullOrWhitespace(this.femaleModel);
    }

    public String getModel(boolean female) {
        return female ? this.femaleModel : this.maleModel;
    }

    public String getAltModel(boolean female) {
        return female ? this.altFemaleModel : this.altMaleModel;
    }

    public String getFemaleModel() {
        return this.femaleModel;
    }

    public String getMaleModel() {
        return this.maleModel;
    }

    public String getAltFemaleModel() {
        return this.altFemaleModel;
    }

    public String getAltMaleModel() {
        return this.altMaleModel;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{ Name:" + this.mame + ", GUID:" + this.guid + "}";
    }

    public static void tryGetCombinedMask(ClothingItemReference itemRef, CharacterMask in_out_mask) {
        tryGetCombinedMask(itemRef.getClothingItem(), in_out_mask);
    }

    public static void tryGetCombinedMask(ClothingItem item, CharacterMask in_out_mask) {
        if (item != null) {
            item.getCombinedMask(in_out_mask);
        }
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }
}
