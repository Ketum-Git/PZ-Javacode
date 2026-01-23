// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import java.util.Locale;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.util.StringUtils;

public class PopTemplateManager {
    public static final PopTemplateManager instance = new PopTemplateManager();
    public final ArrayList<String> maleSkins = new ArrayList<>();
    public final ArrayList<String> femaleSkins = new ArrayList<>();
    public final ArrayList<String> maleSkinsZombie1 = new ArrayList<>();
    public final ArrayList<String> femaleSkinsZombie1 = new ArrayList<>();
    public final ArrayList<String> maleSkinsZombie2 = new ArrayList<>();
    public final ArrayList<String> femaleSkinsZombie2 = new ArrayList<>();
    public final ArrayList<String> maleSkinsZombie3 = new ArrayList<>();
    public final ArrayList<String> femaleSkinsZombie3 = new ArrayList<>();
    public ArrayList<String> cowSkins = new ArrayList<>();
    public ArrayList<String> ratSkins = new ArrayList<>();
    public final ArrayList<String> skeletonMaleSkinsZombie = new ArrayList<>();
    public final ArrayList<String> skeletonFemaleSkinsZombie = new ArrayList<>();
    public static final int SKELETON_BURNED_SKIN_INDEX = 0;
    public static final int SKELETON_NORMAL_SKIN_INDEX = 1;
    public static final int SKELETON_MUSCLE_SKIN_INDEX = 2;

    public void init() {
        for (int i = 1; i <= 5; i++) {
            this.maleSkins.add("MaleBody0" + i);
        }

        for (int i = 1; i <= 5; i++) {
            this.femaleSkins.add("FemaleBody0" + i);
        }

        for (int i = 1; i <= 4; i++) {
            this.maleSkinsZombie1.add("M_ZedBody0" + i + "_level1");
            this.femaleSkinsZombie1.add("F_ZedBody0" + i + "_level1");
            this.maleSkinsZombie2.add("M_ZedBody0" + i + "_level2");
            this.femaleSkinsZombie2.add("F_ZedBody0" + i + "_level2");
            this.maleSkinsZombie3.add("M_ZedBody0" + i + "_level3");
            this.femaleSkinsZombie3.add("F_ZedBody0" + i + "_level3");
        }

        this.skeletonMaleSkinsZombie.add("SkeletonBurned");
        this.skeletonMaleSkinsZombie.add("Skeleton");
        this.skeletonMaleSkinsZombie.add("SkeletonMuscle");
        this.skeletonFemaleSkinsZombie.add("SkeletonBurned");
        this.skeletonFemaleSkinsZombie.add("Skeleton");
        this.skeletonFemaleSkinsZombie.add("SkeletonMuscle");
        this.cowSkins.add("Cow_Black");
        this.cowSkins.add("Cow_Purple");
        this.ratSkins.add("Rat");
    }

    public ModelInstance addClothingItem(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, ItemVisual itemVisual, ClothingItem clothingItem) {
        return this.addClothingItem(chr, modelSlot, itemVisual, clothingItem, false);
    }

    public ModelInstance addClothingItem(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, ItemVisual itemVisual, ClothingItem clothingItem, boolean alt) {
        String modelFileName = clothingItem.getModel(chr.isFemale());
        if (alt && clothingItem.getAltModel(chr.isFemale()) != null) {
            modelFileName = clothingItem.getAltModel(chr.isFemale());
        }

        if (StringUtils.isNullOrWhitespace(modelFileName)) {
            if (DebugLog.isEnabled(DebugType.Clothing)) {
                DebugLog.Clothing.debugln("No model specified by item: " + clothingItem.mame);
            }

            return null;
        } else {
            modelFileName = this.processModelFileName(modelFileName);
            String textureName = itemVisual.getTextureChoice(clothingItem);
            ImmutableColor tintColor = itemVisual.getTint(clothingItem);
            String attachBone = clothingItem.attachBone;
            String shaderName = clothingItem.shader;
            ModelInstance inst;
            if (attachBone != null && !attachBone.isEmpty()) {
                inst = ModelManager.instance.newStaticInstance(modelSlot, modelFileName, textureName, attachBone, shaderName);
            } else {
                inst = ModelManager.instance.newAdditionalModelInstance(modelFileName, textureName, chr, modelSlot.model.animPlayer, shaderName);
            }

            if (inst == null) {
                return null;
            } else {
                this.postProcessNewItemInstance(inst, modelSlot, tintColor);
                inst.setItemVisual(itemVisual);
                return inst;
            }
        }
    }

    private void addHeadHairItem(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, String modelFileName, String textureName, ImmutableColor tintColor) {
        if (StringUtils.isNullOrWhitespace(modelFileName)) {
            if (DebugLog.isEnabled(DebugType.Clothing)) {
                DebugLog.Clothing.warn("No model specified.");
            }
        } else {
            modelFileName = this.processModelFileName(modelFileName);
            ModelInstance inst = ModelManager.instance.newAdditionalModelInstance(modelFileName, textureName, chr, modelSlot.model.animPlayer, null);
            if (inst != null) {
                this.postProcessNewItemInstance(inst, modelSlot, tintColor);
            }
        }
    }

    private void addHeadHair(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, HumanVisual humanVisual, ItemVisual hatVisual, boolean beardOnly) {
        ImmutableColor col = humanVisual.getHairColor();
        if (beardOnly) {
            col = humanVisual.getBeardColor();
        }

        if (chr.isFemale()) {
            if (!beardOnly) {
                HairStyle hairStyle = HairStyles.instance.FindFemaleStyle(humanVisual.getHairModel());
                if (hairStyle != null && hatVisual != null && hatVisual.getClothingItem() != null) {
                    hairStyle = HairStyles.instance.getAlternateForHat(hairStyle, hatVisual.getClothingItem().hatCategory);
                }

                if (hairStyle != null && hairStyle.isValid()) {
                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                        DebugLog.Clothing.debugln("  Adding female hair: " + hairStyle.name);
                    }

                    this.addHeadHairItem(chr, modelSlot, hairStyle.model, hairStyle.texture, col);
                }
            }
        } else if (!beardOnly) {
            HairStyle hairStylex = HairStyles.instance.FindMaleStyle(humanVisual.getHairModel());
            if (hairStylex != null && hatVisual != null && hatVisual.getClothingItem() != null) {
                hairStylex = HairStyles.instance.getAlternateForHat(hairStylex, hatVisual.getClothingItem().hatCategory);
            }

            if (hairStylex != null && hairStylex.isValid()) {
                if (DebugLog.isEnabled(DebugType.Clothing)) {
                    DebugLog.Clothing.debugln("  Adding male hair: " + hairStylex.name);
                }

                this.addHeadHairItem(chr, modelSlot, hairStylex.model, hairStylex.texture, col);
            }
        } else {
            BeardStyle beardStyle = BeardStyles.instance.FindStyle(humanVisual.getBeardModel());
            if (beardStyle != null && beardStyle.isValid()) {
                if (hatVisual != null
                    && hatVisual.getClothingItem() != null
                    && !StringUtils.isNullOrEmpty(hatVisual.getClothingItem().hatCategory)
                    && hatVisual.getClothingItem().hatCategory.contains("nobeard")) {
                    return;
                }

                if (DebugLog.isEnabled(DebugType.Clothing)) {
                    DebugLog.Clothing.debugln("  Adding beard: " + beardStyle.name);
                }

                this.addHeadHairItem(chr, modelSlot, beardStyle.model, beardStyle.texture, col);
            }
        }
    }

    public void populateCharacterModelSlot(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot) {
        if (chr instanceof IHumanVisual iHumanVisual) {
            HumanVisual humanVisual = iHumanVisual.getHumanVisual();
            ItemVisuals itemVisuals = new ItemVisuals();
            chr.getItemVisuals(itemVisuals);
            CharacterMask mask = HumanVisual.GetMask(itemVisuals);
            if (DebugLog.isEnabled(DebugType.Clothing)) {
                DebugLog.Clothing.debugln("characterType:" + chr.getClass().getName() + ", name:" + chr.getName());
            }

            if (mask.isPartVisible(CharacterMask.Part.Head)) {
                this.addHeadHair(chr, modelSlot, humanVisual, itemVisuals.findHat(), false);
                this.addHeadHair(chr, modelSlot, humanVisual, itemVisuals.findMask(), true);
            }

            for (int i = itemVisuals.size() - 1; i >= 0; i--) {
                ItemVisual itemVisual = itemVisuals.get(i);
                ClothingItem clothingItem = itemVisual.getClothingItem();
                if (clothingItem == null) {
                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                        DebugLog.Clothing.warn("ClothingItem not found for ItemVisual:" + itemVisual);
                    }
                } else if (!this.isItemModelHidden(chr.getBodyLocationGroup(), itemVisuals, itemVisual)) {
                    if (this.isItemModelAlt(chr.getBodyLocationGroup(), itemVisuals, itemVisual)) {
                        this.addClothingItem(chr, modelSlot, itemVisual, clothingItem, true);
                    } else {
                        this.addClothingItem(chr, modelSlot, itemVisual, clothingItem);
                    }
                }
            }

            for (int ix = humanVisual.getBodyVisuals().size() - 1; ix >= 0; ix--) {
                ItemVisual itemVisual = humanVisual.getBodyVisuals().get(ix);
                ClothingItem clothingItem = itemVisual.getClothingItem();
                if (clothingItem == null) {
                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                        DebugLog.Clothing.warn("ClothingItem not found for ItemVisual:" + itemVisual);
                    }
                } else {
                    this.addClothingItem(chr, modelSlot, itemVisual, clothingItem);
                }
            }

            chr.postUpdateModelTextures();
        } else {
            DebugLog.Clothing.warn("Supplied character is not an IHumanVisual. Ignored. " + chr);
        }
    }

    public boolean isItemModelHidden(BodyLocationGroup bodyLocationGroup, ItemVisuals visuals, ItemVisual visual) {
        Item item1 = visual.getScriptItem();
        if (item1 != null && bodyLocationGroup.getLocation(item1.getBodyLocation()) != null) {
            for (int i = 0; i < visuals.size(); i++) {
                if (visuals.get(i) != visual) {
                    Item item2 = visuals.get(i).getScriptItem();
                    if (item2 != null
                        && bodyLocationGroup.getLocation(item2.getBodyLocation()) != null
                        && bodyLocationGroup.isHideModel(item2.getBodyLocation(), item1.getBodyLocation())) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean isItemModelHidden(ItemVisuals visuals, ItemBodyLocation bodyLocation) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");

        for (int i = 0; i < visuals.size(); i++) {
            Item item2 = visuals.get(i).getScriptItem();
            if (item2 != null
                && bodyLocationGroup.getLocation(item2.getBodyLocation()) != null
                && bodyLocationGroup.isHideModel(item2.getBodyLocation(), bodyLocation)) {
                return true;
            }
        }

        return false;
    }

    public boolean isItemModelAlt(BodyLocationGroup bodyLocationGroup, ItemVisuals visuals, ItemVisual visual) {
        Item item1 = visual.getScriptItem();
        if (item1 != null && bodyLocationGroup.getLocation(item1.getBodyLocation()) != null) {
            for (int i = 0; i < visuals.size(); i++) {
                if (visuals.get(i) != visual) {
                    Item item2 = visuals.get(i).getScriptItem();
                    if (item2 != null
                        && bodyLocationGroup.getLocation(item2.getBodyLocation()) != null
                        && bodyLocationGroup.isAltModel(item2.getBodyLocation(), item1.getBodyLocation())) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean isItemModelAlt(ItemVisuals visuals, ItemBodyLocation bodyLocation) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");

        for (int i = 0; i < visuals.size(); i++) {
            Item item2 = visuals.get(i).getScriptItem();
            if (item2 != null
                && bodyLocationGroup.getLocation(item2.getBodyLocation()) != null
                && bodyLocationGroup.isAltModel(item2.getBodyLocation(), bodyLocation)) {
                return true;
            }
        }

        return false;
    }

    private String processModelFileName(String modelFileName) {
        modelFileName = modelFileName.replaceAll("\\\\", "/");
        return modelFileName.toLowerCase(Locale.ENGLISH);
    }

    private void postProcessNewItemInstance(ModelInstance modelInstance, ModelManager.ModelSlot parentSlot, ImmutableColor tintColor) {
        modelInstance.depthBias = 0.0F;
        modelInstance.matrixModel = parentSlot.model;
        modelInstance.tintR = tintColor.r;
        modelInstance.tintG = tintColor.g;
        modelInstance.tintB = tintColor.b;
        modelInstance.parent = parentSlot.model;
        modelInstance.animPlayer = parentSlot.model.animPlayer;
        if (parentSlot.model == modelInstance) {
            DebugLog.General.printStackTrace("ERROR: parentSlot.model and modelInstance are equal");
            DebugLog.General.warn("Model=" + modelInstance.model.name);
        }

        parentSlot.model.sub.add(0, modelInstance);
        parentSlot.sub.add(0, modelInstance);
        modelInstance.setOwner(parentSlot);
    }
}
