// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class MetaRecipe implements RecipeKey {
    public static final MetaRecipe ASSEMBLE_ADVANCED_FRAMEPACK = registerBase("AssembleAdvancedFramepack");
    public static final MetaRecipe ASSEMBLE_LARGE_FRAMEPACK = registerBase("AssembleLargeFramepack");
    public static final MetaRecipe ASSEMBLE_SHOULDER_ARMOR = registerBase("Assemble_Shoulder_Armor");
    public static final MetaRecipe BIND_SPEAR = registerBase("BindSpear");
    public static final MetaRecipe CAN_REINFORCE_WEAPON = registerBase("CanReinforceWeapon");
    public static final MetaRecipe CARVE_BAT = registerBase("CarveBat");
    public static final MetaRecipe FORGE_FILE = registerBase("Forge_File");
    public static final MetaRecipe FORGE_FINE_BUTTER_KNIVES = registerBase("Forge_Fine_Butter_Knives");
    public static final MetaRecipe FORGE_FINE_FORKS = registerBase("Forge_Fine_Forks");
    public static final MetaRecipe FORGE_FINE_SPOONS = registerBase("Forge_Fine_Spoons");
    public static final MetaRecipe FORGE_METALWORKING_CHISEL = registerBase("Forge_Metalworking_Chisel");
    public static final MetaRecipe KITCHEN_TOOLS = registerBase("KitchenTools");
    public static final MetaRecipe MAKE_BONE_ARMOR = registerBase("MakeBoneArmor");
    public static final MetaRecipe MAKE_BULLETPROOF_LIMB_ARMOR = registerBase("MakeBulletproofLimbArmor");
    public static final MetaRecipe MAKE_FLIES_CURE = registerBase("MakeFliesCure");
    public static final MetaRecipe MAKE_LARGE_BONE_BEAD = registerBase("MakeLargeBoneBead");
    public static final MetaRecipe MAKE_MAGAZINE_ARMOR = registerBase("MakeMagazineArmor");
    public static final MetaRecipe MAKE_RAILSPIKE_WEAPON = registerBase("MakeRailspikeWeapon");
    public static final MetaRecipe MAKE_SAWBLADE_WEAPON = registerBase("MakeSawbladeWeapon");
    public static final MetaRecipe MAKE_SPIKED_CLUB = registerBase("MakeSpikedClub");
    public static final MetaRecipe MAKE_STONE_BLADE = registerBase("MakeStoneBlade");
    public static final MetaRecipe MAKE_TIRE_ARMOR = registerBase("MakeTireArmor");
    public static final MetaRecipe MAKE_TIRE_SHOULDER_ARMOR_LEFT = registerBase("MakeTireShoulderArmorLeft");
    public static final MetaRecipe MAKE_WOOD_ARMOR = registerBase("MakeWoodArmor");
    public static final MetaRecipe SEW_BANDOLIER = registerBase("SewBandolier");
    public static final MetaRecipe SEW_CRUDE_LEATHER_BACKPACK = registerBase("SewCrudeLeatherBackpack");
    public static final MetaRecipe SEW_DRESS_KNEES = registerBase("SewDressKnees");
    public static final MetaRecipe SEW_HIDE_FANNY_BAG = registerBase("SewHideFannyBag");
    public static final MetaRecipe SEW_HIDE_PANTS = registerBase("SewHidePants");
    public static final MetaRecipe SEW_LONGJOHNS = registerBase("SewLongjohns");
    public static final MetaRecipe SEW_SHIRT = registerBase("SewShirt");
    public static final MetaRecipe SEW_SKIRT_KNEES = registerBase("SewSkirtKnees");
    public static final MetaRecipe SHARPEN_BONE = registerBase("SharpenBone");
    public static final MetaRecipe SPIKE_PADDING = registerBase("SpikePadding");
    private final String translationName;

    private MetaRecipe(String translationName) {
        this.translationName = translationName;
    }

    public static MetaRecipe register(String id) {
        return register(false, id);
    }

    private static MetaRecipe registerBase(String id) {
        return register(true, id);
    }

    private static MetaRecipe register(boolean allowDefaultNamespace, String id) {
        return Registries.META_RECIPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new MetaRecipe(id));
    }

    public static MetaRecipe get(ResourceLocation id) {
        return Registries.META_RECIPE.get(id);
    }

    @Override
    public String toString() {
        return Registries.META_RECIPE.getLocation(this).toString();
    }

    @Override
    public ResourceLocation getRegistryId() {
        return Registries.META_RECIPE.getLocation(this);
    }

    @Override
    public String getTranslationName() {
        return this.translationName;
    }

    static {
        if (Core.IS_DEV) {
            for (MetaRecipe metaRecipe : Registries.META_RECIPE) {
                TranslationKeyValidator.of(metaRecipe.getTranslationName());
            }
        }
    }
}
