// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.HairOutfitDefinitions;
import zombie.characters.SurvivorDesc;
import zombie.characters.WornItems.BodyLocation;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.DefaultClothing;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoWorld;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class HumanVisual extends BaseVisual {
    private final IHumanVisual owner;
    private ImmutableColor skinColor = ImmutableColor.white;
    private int skinTexture = -1;
    protected String skinTextureName;
    public int zombieRotStage = -1;
    private ImmutableColor hairColor;
    private ImmutableColor beardColor;
    private ImmutableColor naturalHairColor;
    private ImmutableColor naturalBeardColor;
    private String hairModel;
    private String beardModel;
    private int bodyHair = -1;
    private final byte[] blood = new byte[BloodBodyPartType.MAX.index()];
    private final byte[] dirt = new byte[BloodBodyPartType.MAX.index()];
    private final byte[] holes = new byte[BloodBodyPartType.MAX.index()];
    private final ItemVisuals bodyVisuals = new ItemVisuals();
    private Outfit outfit;
    private String nonAttachedHair;
    private Model forceModel;
    private String forceModelScript;
    private static final List<ItemBodyLocation> itemVisualLocations = new ArrayList<>();
    private static final int LASTSTAND_VERSION1 = 1;
    private static final int LASTSTAND_VERSION = 1;

    public HumanVisual(IHumanVisual owner) {
        this.owner = owner;
        Arrays.fill(this.blood, (byte)0);
        Arrays.fill(this.dirt, (byte)0);
        Arrays.fill(this.holes, (byte)0);
    }

    public boolean isFemale() {
        return this.owner.isFemale();
    }

    public boolean isZombie() {
        return this.owner.isZombie();
    }

    public boolean isSkeleton() {
        return this.owner.isSkeleton();
    }

    public void setSkinColor(ImmutableColor color) {
        this.skinColor = color;
    }

    public ImmutableColor getSkinColor() {
        if (this.skinColor == null) {
            this.skinColor = new ImmutableColor(SurvivorDesc.getRandomSkinColor());
        }

        return this.skinColor;
    }

    public void setBodyHairIndex(int index) {
        this.bodyHair = index;
    }

    public int getBodyHairIndex() {
        return this.bodyHair;
    }

    public void setSkinTextureIndex(int index) {
        this.skinTexture = index;
    }

    public int getSkinTextureIndex() {
        return this.skinTexture;
    }

    public void setSkinTextureName(String textureName) {
        this.skinTextureName = textureName;
    }

    public float lerp(float start, float end, float delta) {
        if (delta < 0.0F) {
            delta = 0.0F;
        }

        if (delta >= 1.0F) {
            delta = 1.0F;
        }

        float amount = end - start;
        float result = amount * delta;
        return start + result;
    }

    public int pickRandomZombieRotStage() {
        int daysSurvived = Math.max((int)IsoWorld.instance.getWorldAgeDays(), 0);
        float firstDayThreshold = 20.0F;
        float secondDayThreshold = 90.0F;
        float stage1ChanceFirstDay = 100.0F;
        float stage1ChanceSecondDay = 20.0F;
        float stage2ChanceFirstDay = 10.0F;
        float stage2ChanceSecondDay = 30.0F;
        if (daysSurvived >= 180) {
            stage1ChanceSecondDay = 0.0F;
            stage2ChanceSecondDay = 10.0F;
        }

        float daySinceThreshold = daysSurvived - 20.0F;
        float delta = daySinceThreshold / 70.0F;
        float chanceStage1 = this.lerp(100.0F, stage1ChanceSecondDay, delta);
        float chanceStage2 = this.lerp(10.0F, stage2ChanceSecondDay, delta);
        float roll = OutfitRNG.Next(100);
        if (roll < chanceStage1) {
            return 1;
        } else {
            return roll < chanceStage2 + chanceStage1 ? 2 : 3;
        }
    }

    public String getSkinTexture() {
        if (this.skinTextureName != null) {
            return this.skinTextureName;
        } else {
            String bodyHair = "";
            ArrayList<String> textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkins : PopTemplateManager.instance.maleSkins;
            if (this.owner.isZombie() && this.owner.isSkeleton()) {
                if (this.owner.isFemale()) {
                    textures = PopTemplateManager.instance.skeletonFemaleSkinsZombie;
                } else {
                    textures = PopTemplateManager.instance.skeletonMaleSkinsZombie;
                }
            } else if (this.owner.isZombie()) {
                if (this.zombieRotStage < 1 || this.zombieRotStage > 3) {
                    this.zombieRotStage = this.pickRandomZombieRotStage();
                }

                switch (this.zombieRotStage) {
                    case 1:
                        textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkinsZombie1 : PopTemplateManager.instance.maleSkinsZombie1;
                        break;
                    case 2:
                        textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkinsZombie2 : PopTemplateManager.instance.maleSkinsZombie2;
                        break;
                    case 3:
                        textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkinsZombie3 : PopTemplateManager.instance.maleSkinsZombie3;
                }
            } else if (!this.owner.isFemale()) {
                bodyHair = !this.owner.isZombie() && this.bodyHair >= 0 ? "a" : "";
            }

            if (this.skinTexture == textures.size()) {
                this.skinTexture--;
            } else if (this.skinTexture < 0 || this.skinTexture > textures.size()) {
                this.skinTexture = OutfitRNG.Next(textures.size());
            }

            return textures.get(this.skinTexture) + bodyHair;
        }
    }

    public void setHairColor(ImmutableColor color) {
        if (this.beardColor == null) {
            this.beardColor = new ImmutableColor(this.hairColor);
        }

        this.hairColor = color;
    }

    public ImmutableColor getHairColor() {
        if (this.hairColor == null) {
            this.hairColor = HairOutfitDefinitions.instance.getRandomHaircutColor(this.outfit != null ? this.outfit.name : null);
        }

        return this.hairColor;
    }

    public void setBeardColor(ImmutableColor color) {
        this.beardColor = color;
    }

    public ImmutableColor getBeardColor() {
        if (this.beardColor == null) {
            this.beardColor = this.getHairColor();
        }

        return this.beardColor;
    }

    public void setNaturalHairColor(ImmutableColor color) {
        this.naturalHairColor = color;
    }

    public ImmutableColor getNaturalHairColor() {
        if (this.naturalHairColor == null) {
            this.naturalHairColor = this.getHairColor();
        }

        return this.naturalHairColor;
    }

    public void setNaturalBeardColor(ImmutableColor color) {
        this.naturalBeardColor = color;
    }

    public ImmutableColor getNaturalBeardColor() {
        if (this.naturalBeardColor == null) {
            this.naturalBeardColor = this.getNaturalHairColor();
        }

        return this.naturalBeardColor;
    }

    public void setHairModel(String model) {
        this.hairModel = model;
    }

    public String getHairModel() {
        if (this.owner.isFemale()) {
            if (HairStyles.instance.FindFemaleStyle(this.hairModel) == null) {
                this.hairModel = HairStyles.instance.getRandomFemaleStyle(this.outfit != null ? this.outfit.name : null);
            }
        } else if (HairStyles.instance.FindMaleStyle(this.hairModel) == null) {
            this.hairModel = HairStyles.instance.getRandomMaleStyle(this.outfit != null ? this.outfit.name : null);
        }

        return this.hairModel;
    }

    public void setBeardModel(String model) {
        this.beardModel = model;
    }

    public String getBeardModel() {
        if (this.owner.isFemale()) {
            this.beardModel = null;
        } else if (BeardStyles.instance.FindStyle(this.beardModel) == null) {
            this.beardModel = BeardStyles.instance.getRandomStyle(this.outfit != null ? this.outfit.name : null);
        }

        return this.beardModel;
    }

    public void setBlood(BloodBodyPartType bodyPartType, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        this.blood[bodyPartType.index()] = (byte)(amount * 255.0F);
    }

    public float getBlood(BloodBodyPartType bodyPartType) {
        return (this.blood[bodyPartType.index()] & 255) / 255.0F;
    }

    public void setDirt(BloodBodyPartType bodyPartType, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        this.dirt[bodyPartType.index()] = (byte)(amount * 255.0F);
    }

    public float getDirt(BloodBodyPartType bodyPartType) {
        return (this.dirt[bodyPartType.index()] & 255) / 255.0F;
    }

    public void setHole(BloodBodyPartType bodyPartType) {
        this.holes[bodyPartType.index()] = -1;
    }

    public float getHole(BloodBodyPartType bodyPartType) {
        return (this.holes[bodyPartType.index()] & 255) / 255.0F;
    }

    public void removeBlood() {
        Arrays.fill(this.blood, (byte)0);
    }

    public void removeDirt() {
        Arrays.fill(this.dirt, (byte)0);
    }

    public void randomBlood() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            this.setBlood(BloodBodyPartType.FromIndex(i), OutfitRNG.Next(0.0F, 1.0F));
        }
    }

    public void randomDirt() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            this.setDirt(BloodBodyPartType.FromIndex(i), OutfitRNG.Next(0.0F, 1.0F));
        }
    }

    public float getTotalBlood() {
        float total = 0.0F;

        for (int i = 0; i < this.blood.length; i++) {
            total += (this.blood[i] & 255) / 255.0F;
        }

        return total;
    }

    @Override
    public void clear() {
        this.skinColor = ImmutableColor.white;
        this.skinTexture = -1;
        this.skinTextureName = null;
        this.zombieRotStage = -1;
        this.hairColor = null;
        this.beardColor = null;
        this.naturalHairColor = null;
        this.naturalBeardColor = null;
        this.hairModel = null;
        this.nonAttachedHair = null;
        this.beardModel = null;
        this.bodyHair = -1;
        Arrays.fill(this.blood, (byte)0);
        Arrays.fill(this.dirt, (byte)0);
        Arrays.fill(this.holes, (byte)0);
        this.bodyVisuals.clear();
        this.forceModel = null;
        this.forceModelScript = null;
    }

    @Override
    public void copyFrom(BaseVisual other_) {
        if (other_ == null) {
            this.clear();
        } else if (other_ instanceof HumanVisual other) {
            other.getHairColor();
            other.getNaturalHairColor();
            other.getNaturalBeardColor();
            other.getHairModel();
            other.getBeardModel();
            other.getSkinTexture();
            this.skinColor = other.skinColor;
            this.skinTexture = other.skinTexture;
            this.skinTextureName = other.skinTextureName;
            this.zombieRotStage = other.zombieRotStage;
            this.hairColor = other.hairColor;
            this.beardColor = other.beardColor;
            this.naturalHairColor = other.naturalHairColor;
            this.naturalBeardColor = other.naturalBeardColor;
            this.hairModel = other.hairModel;
            this.nonAttachedHair = other.nonAttachedHair;
            this.beardModel = other.beardModel;
            this.bodyHair = other.bodyHair;
            this.outfit = other.outfit;
            System.arraycopy(other.blood, 0, this.blood, 0, this.blood.length);
            System.arraycopy(other.dirt, 0, this.dirt, 0, this.dirt.length);
            System.arraycopy(other.holes, 0, this.holes, 0, this.holes.length);
            this.bodyVisuals.clear();
            PZArrayUtil.addAll(this.bodyVisuals, other.bodyVisuals);
            this.forceModel = other.forceModel;
            this.forceModelScript = other.forceModelScript;
        } else {
            throw new IllegalArgumentException("expected HumanVisual, got " + other_);
        }
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        byte flags1 = 0;
        if (this.hairColor != null) {
            flags1 = (byte)(flags1 | 4);
        }

        if (this.beardColor != null) {
            flags1 = (byte)(flags1 | 2);
        }

        if (this.skinColor != null) {
            flags1 = (byte)(flags1 | 8);
        }

        if (this.beardModel != null) {
            flags1 = (byte)(flags1 | 16);
        }

        if (this.hairModel != null) {
            flags1 = (byte)(flags1 | 32);
        }

        if (this.skinTextureName != null) {
            flags1 = (byte)(flags1 | 64);
        }

        output.put(flags1);
        if (this.hairColor != null) {
            output.put(this.hairColor.getRedByte());
            output.put(this.hairColor.getGreenByte());
            output.put(this.hairColor.getBlueByte());
        }

        if (this.beardColor != null) {
            output.put(this.beardColor.getRedByte());
            output.put(this.beardColor.getGreenByte());
            output.put(this.beardColor.getBlueByte());
        }

        if (this.skinColor != null) {
            output.put(this.skinColor.getRedByte());
            output.put(this.skinColor.getGreenByte());
            output.put(this.skinColor.getBlueByte());
        }

        output.put((byte)this.bodyHair);
        output.put((byte)this.skinTexture);
        output.put((byte)this.zombieRotStage);
        if (this.skinTextureName != null) {
            GameWindow.WriteString(output, this.skinTextureName);
        }

        if (this.beardModel != null) {
            GameWindow.WriteString(output, this.beardModel);
        }

        if (this.hairModel != null) {
            GameWindow.WriteString(output, this.hairModel);
        }

        output.put((byte)this.blood.length);

        for (int i = 0; i < this.blood.length; i++) {
            output.put(this.blood[i]);
        }

        output.put((byte)this.dirt.length);

        for (int i = 0; i < this.dirt.length; i++) {
            output.put(this.dirt[i]);
        }

        output.put((byte)this.holes.length);

        for (int i = 0; i < this.holes.length; i++) {
            output.put(this.holes[i]);
        }

        output.put((byte)this.bodyVisuals.size());

        for (int i = 0; i < this.bodyVisuals.size(); i++) {
            ItemVisual itemVisual = this.bodyVisuals.get(i);
            itemVisual.save(output);
        }

        GameWindow.WriteString(output, this.getNonAttachedHair());
        byte flags2 = 0;
        if (this.naturalHairColor != null) {
            flags2 = (byte)(flags2 | 4);
        }

        if (this.naturalBeardColor != null) {
            flags2 = (byte)(flags2 | 2);
        }

        output.put(flags2);
        if (this.naturalHairColor != null) {
            output.put(this.naturalHairColor.getRedByte());
            output.put(this.naturalHairColor.getGreenByte());
            output.put(this.naturalHairColor.getBlueByte());
        }

        if (this.naturalBeardColor != null) {
            output.put(this.naturalBeardColor.getRedByte());
            output.put(this.naturalBeardColor.getGreenByte());
            output.put(this.naturalBeardColor.getBlueByte());
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.clear();
        int flags1 = input.get() & 255;
        if ((flags1 & 4) != 0) {
            int r = input.get() & 255;
            int g = input.get() & 255;
            int b = input.get() & 255;
            this.hairColor = new ImmutableColor(r, g, b);
        }

        if ((flags1 & 2) != 0) {
            int r = input.get() & 255;
            int g = input.get() & 255;
            int b = input.get() & 255;
            this.beardColor = new ImmutableColor(r, g, b);
        }

        if ((flags1 & 8) != 0) {
            int r = input.get() & 255;
            int g = input.get() & 255;
            int b = input.get() & 255;
            this.skinColor = new ImmutableColor(r, g, b);
        }

        this.bodyHair = input.get();
        this.skinTexture = input.get();
        this.zombieRotStage = input.get();
        if ((flags1 & 64) != 0) {
            this.skinTextureName = GameWindow.ReadString(input);
        }

        if ((flags1 & 16) != 0) {
            this.beardModel = GameWindow.ReadString(input);
        }

        if ((flags1 & 32) != 0) {
            this.hairModel = GameWindow.ReadString(input);
        }

        int count = input.get();

        for (int i = 0; i < count; i++) {
            byte amount = input.get();
            if (i < this.blood.length) {
                this.blood[i] = amount;
            }
        }

        int var12 = input.get();

        for (int ix = 0; ix < var12; ix++) {
            byte amount = input.get();
            if (ix < this.dirt.length) {
                this.dirt[ix] = amount;
            }
        }

        var12 = input.get();

        for (int ixx = 0; ixx < var12; ixx++) {
            byte amount = input.get();
            if (ixx < this.holes.length) {
                this.holes[ixx] = amount;
            }
        }

        var12 = input.get();

        for (int ixxx = 0; ixxx < var12; ixxx++) {
            ItemVisual itemVisual = new ItemVisual();
            itemVisual.load(input, WorldVersion);
            this.bodyVisuals.add(itemVisual);
        }

        this.setNonAttachedHair(GameWindow.ReadString(input));
        int flags2 = input.get() & 255;
        if ((flags2 & 4) != 0) {
            int r = input.get() & 255;
            int g = input.get() & 255;
            int b = input.get() & 255;
            this.naturalHairColor = new ImmutableColor(r, g, b);
        }

        if ((flags2 & 2) != 0) {
            int r = input.get() & 255;
            int g = input.get() & 255;
            int b = input.get() & 255;
            this.naturalBeardColor = new ImmutableColor(r, g, b);
        }
    }

    @Override
    public Model getModel() {
        if (this.forceModel != null) {
            return this.forceModel;
        } else if (this.isSkeleton()) {
            return this.isFemale() ? ModelManager.instance.skeletonFemaleModel : ModelManager.instance.skeletonMaleModel;
        } else {
            return this.isFemale() ? ModelManager.instance.femaleModel : ModelManager.instance.maleModel;
        }
    }

    @Override
    public ModelScript getModelScript() {
        return this.forceModelScript != null
            ? ScriptManager.instance.getModelScript(this.forceModelScript)
            : ScriptManager.instance.getModelScript(this.isFemale() ? "FemaleBody" : "MaleBody");
    }

    public static CharacterMask GetMask(ItemVisuals itemVisuals) {
        CharacterMask mask = new CharacterMask();

        for (int i = itemVisuals.size() - 1; i >= 0; i--) {
            itemVisuals.get(i).getClothingItemCombinedMask(mask);
        }

        return mask;
    }

    public void synchWithOutfit(Outfit outfit) {
        if (outfit != null) {
            this.hairColor = outfit.randomData.hairColor;
            this.beardColor = this.hairColor;
            this.hairModel = this.owner.isFemale() ? outfit.randomData.femaleHairName : outfit.randomData.maleHairName;
            this.beardModel = this.owner.isFemale() ? null : outfit.randomData.beardName;
            this.getSkinTexture();
        }
    }

    @Override
    public void dressInNamedOutfit(String outfitName, ItemVisuals itemVisuals) {
        this.dressInNamedOutfit(outfitName, itemVisuals, true);
    }

    public void dressInNamedOutfit(String outfitName, ItemVisuals itemVisuals, boolean clear) {
        if (clear) {
            itemVisuals.clear();
        }

        if (!StringUtils.isNullOrWhitespace(outfitName)) {
            Outfit outfitSource = this.owner.isFemale()
                ? OutfitManager.instance.FindFemaleOutfit(outfitName)
                : OutfitManager.instance.FindMaleOutfit(outfitName);
            if (outfitSource != null) {
                Outfit outfit = outfitSource.clone();
                outfit.Randomize();
                this.dressInOutfit(outfit, itemVisuals);
            }
        }
    }

    public void dressInClothingItem(String itemGUID, ItemVisuals itemVisuals) {
        this.dressInClothingItem(itemGUID, itemVisuals, true);
    }

    public void dressInClothingItem(String itemGUID, ItemVisuals itemVisuals, boolean clearCurrentVisuals) {
        if (clearCurrentVisuals) {
            this.clear();
            itemVisuals.clear();
        }

        ClothingItem item = OutfitManager.instance.getClothingItem(itemGUID);
        if (item != null) {
            Outfit outfit = new Outfit();
            ClothingItemReference itemRef = new ClothingItemReference();
            itemRef.itemGuid = itemGUID;
            outfit.items.add(itemRef);
            outfit.pants = false;
            outfit.top = false;
            outfit.Randomize();
            this.dressInOutfit(outfit, itemVisuals);
        }
    }

    private void dressInOutfit(Outfit outfit, ItemVisuals itemVisuals) {
        this.setOutfit(outfit);
        this.getItemVisualLocations(itemVisuals, itemVisualLocations);
        if (outfit.pants) {
            String clothingName = outfit.allowPantsHue
                ? DefaultClothing.instance.pickPantsHue()
                : (outfit.allowPantsTint ? DefaultClothing.instance.pickPantsTint() : DefaultClothing.instance.pickPantsTexture());
            this.addClothingItem(itemVisuals, itemVisualLocations, clothingName, null);
        }

        if (outfit.top && outfit.randomData.hasTop) {
            String clothingName;
            if (outfit.randomData.hasTshirt) {
                if (outfit.randomData.hasTshirtDecal && outfit.GetMask().isTorsoVisible() && outfit.allowTshirtDecal) {
                    clothingName = outfit.allowTopTint ? DefaultClothing.instance.pickTShirtDecalTint() : DefaultClothing.instance.pickTShirtDecalTexture();
                } else {
                    clothingName = outfit.allowTopTint ? DefaultClothing.instance.pickTShirtTint() : DefaultClothing.instance.pickTShirtTexture();
                }
            } else {
                clothingName = outfit.allowTopTint ? DefaultClothing.instance.pickVestTint() : DefaultClothing.instance.pickVestTexture();
            }

            this.addClothingItem(itemVisuals, itemVisualLocations, clothingName, null);
        }

        for (int i = 0; i < outfit.items.size(); i++) {
            ClothingItemReference itemRef = outfit.items.get(i);
            ClothingItem clothingItem = itemRef.getClothingItem();
            if (clothingItem != null && clothingItem.isReady()) {
                ItemVisual visual = this.addClothingItem(itemVisuals, itemVisualLocations, clothingItem.mame, itemRef);
                if (visual != null && !clothingItem.getSpawnWith().isEmpty()) {
                    for (int j = 0; j < clothingItem.getSpawnWith().size(); j++) {
                        String modID = outfit.modId;
                        if (modID == null) {
                            modID = "game";
                        }

                        String spawnString = modID + "-" + clothingItem.getSpawnWith().get(j);
                        ClothingItem spawnWithItem = OutfitManager.instance.getClothingItem(spawnString);
                        if (spawnWithItem != null && spawnWithItem != null && spawnWithItem.isReady()) {
                            ItemVisual spawnWithVisual = this.addClothingItem(itemVisuals, itemVisualLocations, spawnWithItem.mame, null);
                            if (spawnWithVisual != null) {
                                spawnWithVisual.copyVisualFrom(visual);
                            }
                        }
                    }
                }
            }
        }

        outfit.pants = false;
        outfit.top = false;
        outfit.randomData.topTexture = null;
        outfit.randomData.pantsTexture = null;
    }

    public ItemVisuals getBodyVisuals() {
        return this.bodyVisuals;
    }

    public ItemVisual addBodyVisual(String clothingItemName) {
        return this.addBodyVisualFromClothingItemName(clothingItemName);
    }

    public ItemVisual addBodyVisualFromItemType(String itemType) {
        Item scriptItem = ScriptManager.instance.getItem(itemType);
        return scriptItem != null && !StringUtils.isNullOrWhitespace(scriptItem.getClothingItem())
            ? this.addBodyVisualFromClothingItemName(scriptItem.getClothingItem())
            : null;
    }

    public ItemVisual addBodyVisualFromClothingItemName(String clothingItemName) {
        if (StringUtils.isNullOrWhitespace(clothingItemName)) {
            return null;
        } else {
            Item scriptItem = ScriptManager.instance.getItemForClothingItem(clothingItemName);
            if (scriptItem == null) {
                return null;
            } else {
                ClothingItem clothingItem = scriptItem.getClothingItemAsset();
                if (clothingItem == null) {
                    return null;
                } else {
                    for (int j = 0; j < this.bodyVisuals.size(); j++) {
                        if (this.bodyVisuals.get(j).getClothingItemName().equals(clothingItemName)) {
                            return null;
                        }
                    }

                    ClothingItemReference itemRef = new ClothingItemReference();
                    itemRef.itemGuid = clothingItem.guid;
                    itemRef.randomize();
                    ItemVisual itemVisual = new ItemVisual();
                    itemVisual.setItemType(scriptItem.getFullName());
                    itemVisual.synchWithOutfit(itemRef);
                    this.bodyVisuals.add(itemVisual);
                    return itemVisual;
                }
            }
        }
    }

    public ItemVisual removeBodyVisualFromItemType(String itemType) {
        for (int i = 0; i < this.bodyVisuals.size(); i++) {
            ItemVisual itemVisual = this.bodyVisuals.get(i);
            if (itemVisual.getItemType().equals(itemType)) {
                this.bodyVisuals.remove(i);
                return itemVisual;
            }
        }

        return null;
    }

    public boolean hasBodyVisualFromItemType(String itemType) {
        for (int i = 0; i < this.bodyVisuals.size(); i++) {
            ItemVisual itemVisual = this.bodyVisuals.get(i);
            if (itemVisual.getItemType().equals(itemType)) {
                return true;
            }
        }

        return false;
    }

    private void getItemVisualLocations(ItemVisuals itemVisuals, List<ItemBodyLocation> itemVisualLocations) {
        itemVisualLocations.clear();

        for (int i = 0; i < itemVisuals.size(); i++) {
            ItemVisual itemVisual = itemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null) {
                itemVisualLocations.add(null);
            } else {
                ItemBodyLocation locationId = scriptItem.getBodyLocation();
                if (locationId == null) {
                    locationId = scriptItem.canBeEquipped;
                }

                itemVisualLocations.add(locationId);
            }
        }
    }

    public ItemVisual addClothingItem(ItemVisuals itemVisuals, Item scriptItem) {
        if (scriptItem == null) {
            return null;
        } else {
            ClothingItem clothingItem = scriptItem.getClothingItemAsset();
            if (clothingItem == null) {
                return null;
            } else if (!clothingItem.isReady()) {
                return null;
            } else {
                this.getItemVisualLocations(itemVisuals, itemVisualLocations);
                return this.addClothingItem(itemVisuals, itemVisualLocations, clothingItem.mame, null);
            }
        }
    }

    public ItemVisual addClothingItem(ItemVisuals itemVisuals, ClothingItem clothingItem) {
        if (clothingItem == null) {
            return null;
        } else if (!clothingItem.isReady()) {
            return null;
        } else {
            this.getItemVisualLocations(itemVisuals, itemVisualLocations);
            return this.addClothingItem(itemVisuals, itemVisualLocations, clothingItem.mame, null);
        }
    }

    private ItemVisual addClothingItem(ItemVisuals itemVisuals, List<ItemBodyLocation> itemVisualLocations, String clothingName, ClothingItemReference itemRef) {
        assert itemVisuals.size() == itemVisualLocations.size();

        if (itemRef != null && !itemRef.randomData.active) {
            return null;
        } else if (StringUtils.isNullOrWhitespace(clothingName)) {
            return null;
        } else {
            Item scriptItem = ScriptManager.instance.getItemForClothingItem(clothingName);
            if (scriptItem == null) {
                if (DebugLog.isEnabled(DebugType.Clothing)) {
                    DebugLog.Clothing.warn("Could not find item type for %s", clothingName);
                }

                return null;
            } else {
                ClothingItem clothingItem = scriptItem.getClothingItemAsset();
                if (clothingItem == null) {
                    return null;
                } else if (!clothingItem.isReady()) {
                    return null;
                } else {
                    ItemBodyLocation locationId = scriptItem.getBodyLocation();
                    if (locationId == null) {
                        locationId = scriptItem.canBeEquipped;
                    }

                    if (locationId == null) {
                        return null;
                    } else {
                        if (itemRef == null) {
                            itemRef = new ClothingItemReference();
                            itemRef.itemGuid = clothingItem.guid;
                            itemRef.randomize();
                        }

                        if (!itemRef.randomData.active) {
                            return null;
                        } else {
                            BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
                            BodyLocation location = bodyLocationGroup.getLocation(locationId);
                            if (location == null) {
                                DebugLog.General.error("The game can't found location '" + locationId + "' for the item '" + scriptItem.name + "'");
                                return null;
                            } else {
                                if (!location.isMultiItem()) {
                                    int index = itemVisualLocations.indexOf(locationId);
                                    if (index != -1) {
                                        itemVisuals.remove(index);
                                        itemVisualLocations.remove(index);
                                    }
                                }

                                for (int i = 0; i < itemVisuals.size(); i++) {
                                    if (bodyLocationGroup.isExclusive(locationId, itemVisualLocations.get(i))) {
                                        itemVisuals.remove(i);
                                        itemVisualLocations.remove(i);
                                        i--;
                                    }
                                }

                                assert itemVisuals.size() == itemVisualLocations.size();

                                int locationIndex = bodyLocationGroup.indexOf(locationId);
                                int insertAt = itemVisuals.size();

                                for (int ix = 0; ix < itemVisuals.size(); ix++) {
                                    if (bodyLocationGroup.indexOf(itemVisualLocations.get(ix)) > locationIndex) {
                                        insertAt = ix;
                                        break;
                                    }
                                }

                                ItemVisual itemVisual = new ItemVisual();
                                itemVisual.setItemType(scriptItem.getFullName());
                                itemVisual.synchWithOutfit(itemRef);
                                itemVisuals.add(insertAt, itemVisual);
                                itemVisualLocations.add(insertAt, locationId);
                                return itemVisual;
                            }
                        }
                    }
                }
            }
        }
    }

    public Outfit getOutfit() {
        return this.outfit;
    }

    public void setOutfit(Outfit outfit) {
        this.outfit = outfit;
    }

    public String getNonAttachedHair() {
        return this.nonAttachedHair;
    }

    public void setNonAttachedHair(String nonAttachedHair) {
        if (StringUtils.isNullOrWhitespace(nonAttachedHair)) {
            nonAttachedHair = null;
        }

        this.nonAttachedHair = nonAttachedHair;
    }

    public void setForceModel(Model model) {
        this.forceModel = model;
    }

    public void setForceModelScript(String modelScript) {
        this.forceModelScript = modelScript;
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
        StringBuilder sb = new StringBuilder();
        sb.append("version=");
        sb.append(1);
        sb.append(";");
        if (this.getHairColor() != null) {
            sb.append("hairColor=");
            toString(this.getHairColor(), sb);
            sb.append(";");
        }

        if (this.getBeardColor() != null) {
            sb.append("beardColor=");
            toString(this.getBeardColor(), sb);
            sb.append(";");
        }

        if (this.getNaturalHairColor() != null) {
            sb.append("naturalHairColor=");
            toString(this.getNaturalHairColor(), sb);
            sb.append(";");
        }

        if (this.getNaturalBeardColor() != null) {
            sb.append("naturalBeardColor=");
            toString(this.getNaturalBeardColor(), sb);
            sb.append(";");
        }

        if (this.getSkinColor() != null) {
            sb.append("skinColor=");
            toString(this.getSkinColor(), sb);
            sb.append(";");
        }

        sb.append("bodyHair=");
        sb.append(this.getBodyHairIndex());
        sb.append(";");
        sb.append("skinTexture=");
        sb.append(this.getSkinTextureIndex());
        sb.append(";");
        if (this.getSkinTexture() != null) {
            sb.append("skinTextureName=");
            sb.append(this.getSkinTexture());
            sb.append(";");
        }

        if (this.getHairModel() != null) {
            sb.append("hairModel=");
            sb.append(this.getHairModel());
            sb.append(";");
        }

        if (this.getBeardModel() != null) {
            sb.append("beardModel=");
            sb.append(this.getBeardModel());
            sb.append(";");
        }

        return sb.toString();
    }

    public boolean loadLastStandString(String saveStr) {
        saveStr = saveStr.trim();
        if (!StringUtils.isNullOrWhitespace(saveStr) && saveStr.startsWith("version=")) {
            int version = -1;
            String[] ss = saveStr.split(";");

            for (int i = 0; i < ss.length; i++) {
                int p = ss[i].indexOf(61);
                if (p != -1) {
                    String key = ss[i].substring(0, p).trim();
                    String value = ss[i].substring(p + 1).trim();
                    switch (key) {
                        case "version":
                            version = Integer.parseInt(value);
                            if (version < 1 || version > 1) {
                                return false;
                            }
                            break;
                        case "beardColor":
                            ImmutableColor colorxxxx = colorFromString(value);
                            if (colorxxxx != null) {
                                this.setBeardColor(colorxxxx);
                            }
                            break;
                        case "naturalBeardColor":
                            ImmutableColor colorxxx = colorFromString(value);
                            if (colorxxx != null) {
                                this.setNaturalBeardColor(colorxxx);
                            }
                            break;
                        case "beardModel":
                            this.setBeardModel(value);
                            break;
                        case "bodyHair":
                            try {
                                this.setBodyHairIndex(Integer.parseInt(value));
                            } catch (NumberFormatException var12) {
                            }
                            break;
                        case "hairColor":
                            ImmutableColor colorxx = colorFromString(value);
                            if (colorxx != null) {
                                this.setHairColor(colorxx);
                            }
                            break;
                        case "naturalHairColor":
                            ImmutableColor colorx = colorFromString(value);
                            if (colorx != null) {
                                this.setNaturalHairColor(colorx);
                            }
                            break;
                        case "hairModel":
                            this.setHairModel(value);
                            break;
                        case "skinColor":
                            ImmutableColor color = colorFromString(value);
                            if (color != null) {
                                this.setSkinColor(color);
                            }
                            break;
                        case "skinTexture":
                            try {
                                this.setSkinTextureIndex(Integer.parseInt(value));
                            } catch (NumberFormatException var11) {
                            }
                            break;
                        case "skinTextureName":
                            this.setSkinTextureName(value);
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
