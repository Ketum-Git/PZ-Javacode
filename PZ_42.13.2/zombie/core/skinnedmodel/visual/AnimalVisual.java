// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.Model;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class AnimalVisual extends BaseVisual {
    private final IAnimalVisual owner;
    private String skinTextureName;
    public int animalRotStage = -1;

    public AnimalVisual(IAnimalVisual owner) {
        this.owner = owner;
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        GameWindow.WriteStringUTF(output, this.skinTextureName);
        output.put((byte)this.animalRotStage);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.skinTextureName = GameWindow.ReadStringUTF(input);
        this.animalRotStage = input.get();
    }

    @Override
    public Model getModel() {
        IsoAnimal animal = this.getIsoAnimal();
        if (animal != null) {
            return this.getModelTest(animal);
        } else {
            if (this.isSkeleton() || this.animalRotStage > 1) {
                AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
                if (this.owner instanceof IsoDeadBody corpse
                    && adef.bodyModelSkelNoHead != null
                    && corpse.getModData() != null
                    && ((KahluaTableImpl)corpse.getModData()).rawgetBool("headless")) {
                    this.skinTextureName = adef.textureSkeleton;
                    return adef.bodyModelSkelNoHead;
                }

                if (adef.bodyModelSkel != null) {
                    this.skinTextureName = adef.textureSkeleton;
                    return adef.bodyModelSkel;
                }
            }

            if (this.owner instanceof IsoDeadBody corpse) {
                AnimalDefinitions adefx = AnimalDefinitions.getDef(this.owner.getAnimalType());
                if (this.animalRotStage == 1 && !StringUtils.isNullOrEmpty(corpse.rottenTexture)) {
                    this.skinTextureName = corpse.rottenTexture;
                }

                if (adefx.bodyModelFleece != null && ((KahluaTableImpl)corpse.getModData()).rawgetBool("shouldBeBodyFleece")) {
                    return adefx.bodyModelFleece;
                }

                if (!StringUtils.isNullOrEmpty(adefx.textureSkinned) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("skinned")) {
                    this.skinTextureName = adefx.textureSkinned;
                }

                if (adefx.bodyModelHeadless != null && corpse.getModData() != null && ((KahluaTableImpl)corpse.getModData()).rawgetBool("headless")) {
                    return adefx.bodyModelHeadless;
                }
            }

            return AnimalDefinitions.getDef(this.owner.getAnimalType()).bodyModel;
        }
    }

    public Model getModelTest(IsoAnimal animal) {
        if (animal.shouldBeSkeleton()) {
            if (animal.adef.bodyModelSkelNoHead != null && animal.getModData() != null && ((KahluaTableImpl)animal.getModData()).rawgetBool("headless")) {
                this.skinTextureName = AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkeleton;
                return AnimalDefinitions.getDef(this.owner.getAnimalType()).bodyModelSkelNoHead;
            } else {
                this.skinTextureName = AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkeleton;
                return AnimalDefinitions.getDef(this.owner.getAnimalType()).bodyModelSkel;
            }
        } else {
            if (!StringUtils.isNullOrEmpty(AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkinned)
                && ((KahluaTableImpl)animal.getModData()).rawgetBool("skinned")) {
                this.skinTextureName = AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkinned;
            }

            if (animal.adef.bodyModelHeadless != null && animal.getModData() != null && ((KahluaTableImpl)animal.getModData()).rawgetBool("headless")) {
                return animal.adef.bodyModelHeadless;
            } else if (StringUtils.isNullOrEmpty(animal.getBreed().woolType)) {
                return animal.adef.bodyModel;
            } else if (animal.getData().getWoolQuantity() >= animal.getData().getMaxWool() / 2.0F && animal.adef.bodyModelFleece != null) {
                return animal.adef.bodyModelFleece;
            } else {
                return ((KahluaTableImpl)animal.getModData()).rawgetBool("shouldBeBodyFleece") && animal.adef.bodyModelFleece != null
                    ? animal.adef.bodyModelFleece
                    : animal.adef.bodyModel;
            }
        }
    }

    @Override
    public ModelScript getModelScript() {
        IsoAnimal animal = this.getIsoAnimal();
        if (animal == null) {
            AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
            if (this.isSkeleton() && adef.bodyModelSkel != null) {
                this.skinTextureName = adef.textureSkeleton;
                return ScriptManager.instance.getModelScript(adef.bodyModelSkelStr);
            } else {
                if (this.owner instanceof IsoDeadBody corpse) {
                    if (!StringUtils.isNullOrEmpty(adef.textureSkinned) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("skinned")) {
                        this.skinTextureName = adef.textureSkinned;
                    }

                    if (!StringUtils.isNullOrEmpty(adef.bodyModelHeadlessStr) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("headless")) {
                        return ScriptManager.instance.getModelScript(adef.bodyModelHeadlessStr);
                    }

                    if (!StringUtils.isNullOrEmpty(adef.bodyModelFleeceStr) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("shouldBeBodyFleece")) {
                        return ScriptManager.instance.getModelScript(adef.bodyModelFleeceStr);
                    }
                }

                return ScriptManager.instance.getModelScript(adef.bodyModelStr);
            }
        } else {
            if (!StringUtils.isNullOrEmpty(animal.adef.textureSkinned) && ((KahluaTableImpl)animal.getModData()).rawgetBool("skinned")) {
                this.skinTextureName = animal.adef.textureSkinned;
            }

            if (!StringUtils.isNullOrEmpty(animal.adef.bodyModelHeadlessStr)
                && animal.getModData() != null
                && ((KahluaTableImpl)animal.getModData()).rawgetBool("headless")) {
                return ScriptManager.instance.getModelScript(animal.adef.bodyModelHeadlessStr);
            } else if (StringUtils.isNullOrEmpty(animal.getBreed().woolType)) {
                return ScriptManager.instance.getModelScript(animal.adef.bodyModelStr);
            } else {
                return animal.getData().getWoolQuantity() >= animal.getData().getMaxWool() / 2.0F && animal.adef.bodyModelFleeceStr != null
                    ? ScriptManager.instance.getModelScript(animal.adef.bodyModelFleeceStr)
                    : ScriptManager.instance.getModelScript(animal.adef.bodyModelStr);
            }
        }
    }

    @Override
    public void dressInNamedOutfit(String outfitName, ItemVisuals itemVisuals) {
        itemVisuals.clear();
    }

    public String getAnimalType() {
        return this.owner.getAnimalType();
    }

    public float getAnimalSize() {
        return this.owner.getAnimalSize();
    }

    public IsoAnimal getIsoAnimal() {
        if (this.owner instanceof IsoAnimal animal) {
            return animal;
        } else {
            AnimatedModel animatedModel = Type.tryCastTo(this.owner, AnimatedModel.class);
            return animatedModel != null && animatedModel.getCharacter() instanceof IsoAnimal ? (IsoAnimal)animatedModel.getCharacter() : null;
        }
    }

    public String getSkinTexture() {
        AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
        if (this.animalRotStage > -1 && adef != null) {
            switch (this.animalRotStage) {
                case 1:
                    if (!StringUtils.isNullOrEmpty(adef.textureRotten)) {
                        return adef.textureRotten;
                    }

                    return this.skinTextureName;
                case 2:
                    if (!StringUtils.isNullOrEmpty(adef.textureSkeletonBloody)) {
                        return adef.textureSkeletonBloody;
                    }

                    return this.skinTextureName;
                case 3:
                    if (!StringUtils.isNullOrEmpty(adef.textureSkeleton)) {
                        return adef.textureSkeleton;
                    }

                    return this.skinTextureName;
            }
        }

        return this.skinTextureName;
    }

    public void setSkinTextureName(String textureName) {
        this.skinTextureName = textureName;
    }

    public boolean isSkeleton() {
        return this.owner != null && this.owner.isSkeleton();
    }

    @Override
    public void clear() {
        this.skinTextureName = null;
        this.animalRotStage = -1;
    }

    @Override
    public void copyFrom(BaseVisual other_) {
        if (other_ == null) {
            this.clear();
        } else if (other_ instanceof AnimalVisual other) {
            this.skinTextureName = other.skinTextureName;
            this.animalRotStage = other.animalRotStage;
        } else {
            throw new IllegalArgumentException("expected AnimalVisual, got " + other_);
        }
    }
}
