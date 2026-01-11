// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.CharacterSmartTexture;
import zombie.characterTextures.ItemSmartTexture;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingDecal;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.popman.ObjectPool;
import zombie.util.Lambda;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ModelInstanceTextureCreator extends TextureDraw.GenericDrawer {
    private boolean zombie;
    public int renderRefCount;
    private final CharacterMask mask = new CharacterMask();
    private final boolean[] holeMask = new boolean[BloodBodyPartType.MAX.index()];
    private final ItemVisuals itemVisuals = new ItemVisuals();
    private final ModelInstanceTextureCreator.CharacterData chrData = new ModelInstanceTextureCreator.CharacterData();
    private final ArrayList<ModelInstanceTextureCreator.ItemData> itemData = new ArrayList<>();
    private final CharacterSmartTexture characterSmartTexture = new CharacterSmartTexture();
    private final ItemSmartTexture itemSmartTexture = new ItemSmartTexture(null);
    private final ArrayList<Texture> tempTextures = new ArrayList<>();
    private boolean rendered;
    private final ArrayList<Texture> texturesNotReady = new ArrayList<>();
    public int testNotReady = -1;
    private final ArrayList<ModelInstanceTextureCreator.ItemData> localItemData = new ArrayList<>();
    private static final ObjectPool<ModelInstanceTextureCreator> pool = new ObjectPool<>(ModelInstanceTextureCreator::new);

    public void init(IsoGameCharacter chr) {
        ModelManager.ModelSlot modelSlot = chr.legsSprite.modelSlot;
        if (chr instanceof IsoAnimal isoAnimal) {
            this.init(isoAnimal.getAnimalVisual(), modelSlot.model);
        } else {
            HumanVisual humanVisual = ((IHumanVisual)chr).getHumanVisual();
            chr.getItemVisuals(this.itemVisuals);
            this.init(humanVisual, this.itemVisuals, modelSlot.model);
            this.itemVisuals.clear();
        }
    }

    public void init(BaseVisual baseVisual, ItemVisuals itemVisuals, ModelInstance chrModelInstance) {
        if (baseVisual instanceof AnimalVisual animalVisual) {
            this.init(animalVisual, chrModelInstance);
        } else if (baseVisual instanceof HumanVisual humanVisual) {
            this.init(humanVisual, itemVisuals, chrModelInstance);
        } else {
            throw new IllegalArgumentException("unhandled BaseVisual " + baseVisual);
        }
    }

    public void init(AnimalVisual animalVisual, ModelInstance chrModelInstance) {
        this.chrData.modelInstance = chrModelInstance;
        this.rendered = false;
        this.zombie = false;
        Arrays.fill(this.holeMask, false);
        synchronized (this.itemData) {
            ModelInstanceTextureCreator.ItemData.pool.release(this.itemData);
            this.itemData.clear();
        }

        this.texturesNotReady.clear();
        this.chrData.mask.setAllVisible(true);
        this.chrData.maskFolder = "media/textures/Body/Masks";
        this.chrData.baseTexture = "media/textures/Body/" + animalVisual.getSkinTexture() + ".png";
        Arrays.fill(this.chrData.blood, 0.0F);
        Arrays.fill(this.chrData.dirt, 0.0F);
        Texture tex = Texture.getSharedTexture(this.chrData.baseTexture);
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }

        if (!this.chrData.mask.isAllVisible() && !this.chrData.mask.isNothingVisible()) {
            String maskFolder = this.chrData.maskFolder;
            Consumer<CharacterMask.Part> consumer = Lambda.consumer(maskFolder, this.texturesNotReady, (part, l_maskFolder, l_texturesNotReady) -> {
                Texture tex1 = Texture.getSharedTexture(l_maskFolder + "/" + part + ".png");
                if (tex1 != null && !tex1.isReady()) {
                    l_texturesNotReady.add(tex1);
                }
            });
            this.chrData.mask.forEachVisible(consumer);
        }
    }

    public void init(HumanVisual humanVisual, ItemVisuals itemVisuals, ModelInstance chrModelInstance) {
        boolean bDebugEnabled = DebugLog.isEnabled(DebugType.Clothing);
        this.chrData.modelInstance = chrModelInstance;
        this.rendered = false;
        this.zombie = humanVisual.isZombie();
        CharacterMask overlayerMask = this.mask;
        overlayerMask.setAllVisible(true);
        String overlayMasksFolder = "media/textures/Body/Masks";
        Arrays.fill(this.holeMask, false);
        synchronized (this.itemData) {
            ModelInstanceTextureCreator.ItemData.pool.release(this.itemData);
            this.itemData.clear();
        }

        this.texturesNotReady.clear();
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");

        for (int i = itemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = itemVisuals.get(i);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null) {
                if (bDebugEnabled) {
                    DebugLog.Clothing.warn("ClothingItem not found for ItemVisual:" + itemVisual);
                }
            } else if (!clothingItem.isReady()) {
                if (bDebugEnabled) {
                    DebugLog.Clothing.warn("ClothingItem not ready for ItemVisual:" + itemVisual);
                }
            } else if (!PopTemplateManager.instance.isItemModelHidden(bodyLocationGroup, itemVisuals, itemVisual)) {
                ModelInstance modelInstance = this.findModelInstance(chrModelInstance.sub, itemVisual);
                if (modelInstance == null) {
                    String modelName = clothingItem.getModel(humanVisual.isFemale());
                    if (!StringUtils.isNullOrWhitespace(modelName)) {
                        if (bDebugEnabled) {
                            DebugLog.Clothing.warn("ModelInstance not found for ItemVisual:" + itemVisual);
                        }
                        continue;
                    }
                }

                this.addClothingItem(modelInstance, itemVisual, clothingItem, overlayerMask, overlayMasksFolder);

                for (int j = 0; j < BloodBodyPartType.MAX.index(); j++) {
                    BloodBodyPartType bpt = BloodBodyPartType.FromIndex(j);
                    if (itemVisual.getHole(bpt) > 0.0F && overlayerMask.isBloodBodyPartVisible(bpt)) {
                        this.holeMask[j] = true;
                    }
                }

                for (int jx = 0; jx < clothingItem.masks.size(); jx++) {
                    CharacterMask.Part cmp = CharacterMask.Part.fromInt(clothingItem.masks.get(jx));

                    for (BloodBodyPartType bpt : cmp.getBloodBodyPartTypes()) {
                        if (itemVisual.getHole(bpt) <= 0.0F) {
                            this.holeMask[bpt.index()] = false;
                        }
                    }
                }

                itemVisual.getClothingItemCombinedMask(overlayerMask);
                if (!StringUtils.equalsIgnoreCase(clothingItem.underlayMasksFolder, "media/textures/Body/Masks")) {
                    overlayMasksFolder = clothingItem.underlayMasksFolder;
                }
            }
        }

        this.chrData.mask.copyFrom(overlayerMask);
        this.chrData.maskFolder = overlayMasksFolder;
        this.chrData.baseTexture = "media/textures/Body/" + humanVisual.getSkinTexture() + ".png";
        Arrays.fill(this.chrData.blood, 0.0F);

        for (int ix = 0; ix < BloodBodyPartType.MAX.index(); ix++) {
            BloodBodyPartType bptx = BloodBodyPartType.FromIndex(ix);
            this.chrData.blood[ix] = humanVisual.getBlood(bptx);
            this.chrData.dirt[ix] = humanVisual.getDirt(bptx);
        }

        Texture tex = getTextureWithFlags(this.chrData.baseTexture);
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }

        if (!this.chrData.mask.isAllVisible() && !this.chrData.mask.isNothingVisible()) {
            String maskFolder = this.chrData.maskFolder;
            Consumer<CharacterMask.Part> consumer = Lambda.consumer(maskFolder, this.texturesNotReady, (part, l_maskFolder, l_texturesNotReady) -> {
                Texture tex1 = getTextureWithFlags(l_maskFolder + "/" + part + ".png");
                if (tex1 != null && !tex1.isReady()) {
                    l_texturesNotReady.add(tex1);
                }
            });
            this.chrData.mask.forEachVisible(consumer);
        }

        tex = getTextureWithFlags("media/textures/BloodTextures/BloodOverlay.png");
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }

        tex = getTextureWithFlags("media/textures/BloodTextures/GrimeOverlay.png");
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }

        tex = getTextureWithFlags("media/textures/patches/patchesmask.png");
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }

        for (int ix = 0; ix < BloodBodyPartType.MAX.index(); ix++) {
            BloodBodyPartType bptx = BloodBodyPartType.FromIndex(ix);
            String mask = "media/textures/BloodTextures/" + CharacterSmartTexture.MaskFiles[bptx.index()] + ".png";
            tex = getTextureWithFlags(mask);
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }

            String hole = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[bptx.index()] + ".png";
            tex = getTextureWithFlags(hole);
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }

            String basicPatchMask = "media/textures/patches/" + CharacterSmartTexture.BasicPatchesMaskFiles[bptx.index()] + ".png";
            tex = getTextureWithFlags(basicPatchMask);
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }

            String denimPatchMask = "media/textures/patches/" + CharacterSmartTexture.DenimPatchesMaskFiles[bptx.index()] + ".png";
            tex = getTextureWithFlags(denimPatchMask);
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }

            String leatherPatchMask = "media/textures/patches/" + CharacterSmartTexture.LeatherPatchesMaskFiles[bptx.index()] + ".png";
            tex = getTextureWithFlags(leatherPatchMask);
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
        }

        overlayerMask.setAllVisible(true);
        overlayMasksFolder = "media/textures/Body/Masks";

        for (int ix = humanVisual.getBodyVisuals().size() - 1; ix >= 0; ix--) {
            ItemVisual itemVisual = humanVisual.getBodyVisuals().get(ix);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null) {
                if (bDebugEnabled) {
                    DebugLog.Clothing.warn("ClothingItem not found for ItemVisual:" + itemVisual);
                }
            } else if (!clothingItem.isReady()) {
                if (bDebugEnabled) {
                    DebugLog.Clothing.warn("ClothingItem not ready for ItemVisual:" + itemVisual);
                }
            } else {
                ModelInstance modelInstancex = this.findModelInstance(chrModelInstance.sub, itemVisual);
                if (modelInstancex == null) {
                    String modelName = clothingItem.getModel(humanVisual.isFemale());
                    if (!StringUtils.isNullOrWhitespace(modelName)) {
                        if (bDebugEnabled) {
                            DebugLog.Clothing.warn("ModelInstance not found for ItemVisual:" + itemVisual);
                        }
                        continue;
                    }
                }

                this.addClothingItem(modelInstancex, itemVisual, clothingItem, overlayerMask, overlayMasksFolder);
            }
        }
    }

    private ModelInstance findModelInstance(ArrayList<ModelInstance> sub, ItemVisual itemVisual) {
        for (int i = 0; i < sub.size(); i++) {
            ModelInstance modelInstance = sub.get(i);
            ItemVisual itemVisual1 = modelInstance.getItemVisual();
            if (itemVisual1 != null && itemVisual1.getClothingItem() == itemVisual.getClothingItem()) {
                return modelInstance;
            }
        }

        return null;
    }

    private void addClothingItem(
        ModelInstance modelInstance, ItemVisual itemVisual, ClothingItem clothingItem, CharacterMask overlayerMask, String overlayMasksFolder
    ) {
        String textureName = modelInstance == null ? itemVisual.getBaseTexture(clothingItem) : itemVisual.getTextureChoice(clothingItem);
        ImmutableColor tintColor = itemVisual.getTint(clothingItem);
        float hue = itemVisual.getHue(clothingItem);
        ModelInstanceTextureCreator.ItemData data = ModelInstanceTextureCreator.ItemData.pool.alloc();
        data.modelInstance = modelInstance;
        data.category = 3;
        data.mask.copyFrom(overlayerMask);
        data.maskFolder = clothingItem.masksFolder;
        if (StringUtils.equalsIgnoreCase(data.maskFolder, "media/textures/Body/Masks")) {
            data.maskFolder = overlayMasksFolder;
        }

        if (StringUtils.equalsIgnoreCase(data.maskFolder, "none")) {
            data.mask.setAllVisible(true);
        }

        if (data.maskFolder.contains("Clothes/Hat/Masks")) {
            data.mask.setAllVisible(true);
        }

        data.baseTexture = "media/textures/" + textureName + ".png";
        data.tint = tintColor;
        data.hue = hue;
        data.decalTexture = null;
        Arrays.fill(data.basicPatches, 0.0F);
        Arrays.fill(data.denimPatches, 0.0F);
        Arrays.fill(data.leatherPatches, 0.0F);
        Arrays.fill(data.blood, 0.0F);
        Arrays.fill(data.dirt, 0.0F);
        Arrays.fill(data.hole, 0.0F);
        int flags = ModelManager.instance.getTextureFlags();
        Texture tex = Texture.getSharedTexture(data.baseTexture, flags);
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }

        if (!data.mask.isAllVisible() && !data.mask.isNothingVisible()) {
            String maskFolder = data.maskFolder;
            Consumer<CharacterMask.Part> consumer = Lambda.consumer(maskFolder, this.texturesNotReady, (part, l_maskFolder, l_texturesNotReady) -> {
                Texture tex1 = getTextureWithFlags(l_maskFolder + "/" + part + ".png");
                if (tex1 != null && !tex1.isReady()) {
                    l_texturesNotReady.add(tex1);
                }
            });
            data.mask.forEachVisible(consumer);
        }

        if (Core.getInstance().isOptionSimpleClothingTextures(this.zombie)) {
            synchronized (this.itemData) {
                this.itemData.add(data);
            }
        } else {
            String decalName = itemVisual.getDecal(clothingItem);
            if (!StringUtils.isNullOrWhitespace(decalName)) {
                ClothingDecal decal = ClothingDecals.instance.getDecal(decalName);
                if (decal != null && decal.isValid()) {
                    data.decalTexture = decal.texture;
                    data.decalX = decal.x;
                    data.decalY = decal.y;
                    data.decalWidth = decal.width;
                    data.decalHeight = decal.height;
                    tex = getTextureWithFlags("media/textures/" + data.decalTexture + ".png");
                    if (tex != null && !tex.isReady()) {
                        this.texturesNotReady.add(tex);
                    }
                }
            }

            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                data.blood[i] = itemVisual.getBlood(bpt);
                data.dirt[i] = itemVisual.getDirt(bpt);
                data.basicPatches[i] = itemVisual.getBasicPatch(bpt);
                data.denimPatches[i] = itemVisual.getDenimPatch(bpt);
                data.leatherPatches[i] = itemVisual.getLeatherPatch(bpt);
                data.hole[i] = itemVisual.getHole(bpt);
                if (data.hole[i] > 0.0F) {
                    String mask = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[bpt.index()] + ".png";
                    tex = getTextureWithFlags(mask);
                    if (tex != null && !tex.isReady()) {
                        this.texturesNotReady.add(tex);
                    }
                }

                if (data.hole[i] == 0.0F && this.holeMask[i]) {
                    data.hole[i] = -1.0F;
                    if (data.mask.isBloodBodyPartVisible(bpt)) {
                    }
                }
            }

            synchronized (this.itemData) {
                this.itemData.add(data);
            }
        }
    }

    @Override
    public void render() {
        if (!this.rendered) {
            if (this.chrData.modelInstance != null) {
                for (int i = 0; i < this.texturesNotReady.size(); i++) {
                    Texture texture = this.texturesNotReady.get(i);
                    if (!texture.isReady()) {
                        return;
                    }
                }

                GL11.glPushAttrib(2048);

                try {
                    this.tempTextures.clear();
                    CharacterSmartTexture characterTexture = this.createFullCharacterTexture();

                    assert characterTexture == this.characterSmartTexture;

                    if (!(this.chrData.modelInstance.tex instanceof CharacterSmartTexture)) {
                        this.chrData.modelInstance.tex = new CharacterSmartTexture();
                    }

                    ((CharacterSmartTexture)this.chrData.modelInstance.tex).clear();
                    this.applyCharacterTexture(characterTexture.result, (CharacterSmartTexture)this.chrData.modelInstance.tex);
                    characterTexture.clear();
                    this.tempTextures.add(characterTexture.result);
                    characterTexture.result = null;
                    characterTexture = (CharacterSmartTexture)this.chrData.modelInstance.tex;
                    this.localItemData.clear();
                    synchronized (this.itemData) {
                        PZArrayUtil.addAll(this.localItemData, this.itemData);
                    }

                    for (int ix = this.localItemData.size() - 1; ix >= 0; ix--) {
                        ModelInstanceTextureCreator.ItemData data = this.localItemData.get(ix);
                        Texture itemTexture;
                        if (this.isSimpleTexture(data)) {
                            int flags = ModelManager.instance.getTextureFlags();
                            itemTexture = Texture.getSharedTexture(data.baseTexture, flags);
                            if (!this.isItemSmartTextureRequired(data)) {
                                data.modelInstance.tex = itemTexture;
                                continue;
                            }
                        } else {
                            ItemSmartTexture fullTexture = this.createFullItemTexture(data);

                            assert fullTexture == this.itemSmartTexture;

                            itemTexture = fullTexture.result;
                            this.tempTextures.add(fullTexture.result);
                            fullTexture.result = null;
                        }

                        if (data.modelInstance == null) {
                            this.applyItemTexture(data, itemTexture, characterTexture);
                        } else {
                            if (!(data.modelInstance.tex instanceof ItemSmartTexture)) {
                                data.modelInstance.tex = new ItemSmartTexture(null);
                            }

                            ((ItemSmartTexture)data.modelInstance.tex).clear();
                            this.applyItemTexture(data, itemTexture, (ItemSmartTexture)data.modelInstance.tex);
                            ((ItemSmartTexture)data.modelInstance.tex).calculate();
                            ((ItemSmartTexture)data.modelInstance.tex).clear();
                        }
                    }

                    characterTexture.calculate();
                    characterTexture.clear();
                    this.itemSmartTexture.clear();

                    for (int ix = 0; ix < this.tempTextures.size(); ix++) {
                        for (int j = 0; j < this.localItemData.size(); j++) {
                            ModelInstance modelInstance = this.localItemData.get(j).modelInstance;

                            assert modelInstance == null || this.tempTextures.get(ix) != modelInstance.tex;
                        }

                        TextureCombiner.instance.releaseTexture(this.tempTextures.get(ix));
                    }

                    this.tempTextures.clear();
                } finally {
                    GL11.glPopAttrib();
                }

                this.rendered = true;
            }
        }
    }

    private CharacterSmartTexture createFullCharacterTexture() {
        CharacterSmartTexture smartTex = this.characterSmartTexture;
        smartTex.clear();
        smartTex.addTexture(this.chrData.baseTexture, 0, ImmutableColor.white, 0.0F);

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            if (this.chrData.dirt[i] > 0.0F) {
                smartTex.addDirt(bpt, this.chrData.dirt[i], null);
            }

            if (this.chrData.blood[i] > 0.0F) {
                smartTex.addBlood(bpt, this.chrData.blood[i], null);
            }
        }

        smartTex.calculate();
        return smartTex;
    }

    private void applyCharacterTexture(Texture fullTex, CharacterSmartTexture destTex) {
        destTex.addMaskedTexture(this.chrData.mask, this.chrData.maskFolder, fullTex, 0, ImmutableColor.white, 0.0F);

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            if (this.holeMask[i]) {
                destTex.removeHole(fullTex, bpt);
            }
        }
    }

    private boolean isSimpleTexture(ModelInstanceTextureCreator.ItemData data) {
        if (data.hue != 0.0F) {
            return false;
        } else {
            ImmutableColor tint = data.tint;
            if (data.modelInstance != null) {
                tint = ImmutableColor.white;
            }

            if (!tint.equals(ImmutableColor.white)) {
                return false;
            } else if (data.decalTexture != null) {
                return false;
            } else {
                for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                    if (data.blood[i] > 0.0F) {
                        return false;
                    }

                    if (data.dirt[i] > 0.0F) {
                        return false;
                    }

                    if (data.hole[i] > 0.0F) {
                        return false;
                    }

                    if (data.basicPatches[i] > 0.0F) {
                        return false;
                    }

                    if (data.denimPatches[i] > 0.0F) {
                        return false;
                    }

                    if (data.leatherPatches[i] > 0.0F) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    private ItemSmartTexture createFullItemTexture(ModelInstanceTextureCreator.ItemData data) {
        ItemSmartTexture smartTex = this.itemSmartTexture;
        smartTex.clear();
        ImmutableColor tint = data.tint;
        if (data.modelInstance != null) {
            data.modelInstance.tintR = data.modelInstance.tintG = data.modelInstance.tintB = 1.0F;
        }

        smartTex.addTexture(data.baseTexture, data.category, tint, data.hue);
        if (data.decalTexture != null) {
            smartTex.addRect("media/textures/" + data.decalTexture + ".png", data.decalX, data.decalY, data.decalWidth, data.decalHeight);
        }

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            if (data.blood[i] > 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                smartTex.addBlood("media/textures/BloodTextures/BloodOverlay.png", bpt, data.blood[i]);
            }

            if (data.dirt[i] > 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                smartTex.addDirt("media/textures/BloodTextures/GrimeOverlay.png", bpt, data.dirt[i]);
            }

            if (data.basicPatches[i] > 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                smartTex.setBasicPatches(bpt);
            }

            if (data.denimPatches[i] > 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                smartTex.setDenimPatches(bpt);
            }

            if (data.leatherPatches[i] > 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                smartTex.setLeatherPatches(bpt);
            }
        }

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            if (data.hole[i] > 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                Texture result = smartTex.addHole(bpt);

                assert result != smartTex.result;

                this.tempTextures.add(result);
            }
        }

        smartTex.calculate();
        return smartTex;
    }

    private boolean isItemSmartTextureRequired(ModelInstanceTextureCreator.ItemData data) {
        if (data.modelInstance == null) {
            return true;
        } else if (data.modelInstance.tex instanceof ItemSmartTexture) {
            return true;
        } else {
            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                if (data.hole[i] < 0.0F) {
                    return true;
                }
            }

            return !data.mask.isAllVisible();
        }
    }

    private void applyItemTexture(ModelInstanceTextureCreator.ItemData data, Texture fullTex, SmartTexture destTex) {
        destTex.addMaskedTexture(data.mask, data.maskFolder, fullTex, data.category, ImmutableColor.white, 0.0F);

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            if (data.hole[i] < 0.0F) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
                destTex.removeHole(fullTex, bpt);
            }
        }
    }

    @Override
    public void postRender() {
        if (!this.rendered) {
            if (this.chrData.modelInstance.character == null) {
                boolean var1 = true;
            } else {
                boolean var5 = true;
            }
        }

        synchronized (this.itemData) {
            for (int i = 0; i < this.itemData.size(); i++) {
                this.itemData.get(i).modelInstance = null;
            }

            this.chrData.modelInstance = null;
            this.texturesNotReady.clear();
            ModelInstanceTextureCreator.ItemData.pool.release(this.itemData);
            this.itemData.clear();
        }

        pool.release(this);
    }

    public boolean isRendered() {
        return this.testNotReady > 0 ? false : this.rendered;
    }

    private static Texture getTextureWithFlags(String fileName) {
        return Texture.getSharedTexture(fileName, ModelManager.instance.getTextureFlags());
    }

    public static ModelInstanceTextureCreator alloc() {
        return pool.alloc();
    }

    private static final class CharacterData {
        ModelInstance modelInstance;
        final CharacterMask mask = new CharacterMask();
        String maskFolder;
        String baseTexture;
        final float[] blood = new float[BloodBodyPartType.MAX.index()];
        final float[] dirt = new float[BloodBodyPartType.MAX.index()];
    }

    private static final class ItemData {
        ModelInstance modelInstance;
        final CharacterMask mask = new CharacterMask();
        String maskFolder;
        String baseTexture;
        int category;
        ImmutableColor tint;
        float hue;
        String decalTexture;
        int decalX;
        int decalY;
        int decalWidth;
        int decalHeight;
        final float[] blood = new float[BloodBodyPartType.MAX.index()];
        final float[] dirt = new float[BloodBodyPartType.MAX.index()];
        final float[] basicPatches = new float[BloodBodyPartType.MAX.index()];
        final float[] denimPatches = new float[BloodBodyPartType.MAX.index()];
        final float[] leatherPatches = new float[BloodBodyPartType.MAX.index()];
        final float[] hole = new float[BloodBodyPartType.MAX.index()];
        static final ObjectPool<ModelInstanceTextureCreator.ItemData> pool = new ObjectPool<>(ModelInstanceTextureCreator.ItemData::new);
    }
}
