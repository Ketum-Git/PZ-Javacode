// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public class SeasonRecipe {
    public static final SeasonRecipe BARLEY_GROWING_SEASON = registerBase("Barley Growing Season");
    public static final SeasonRecipe BASIL_GROWING_SEASON = registerBase("Basil Growing Season");
    public static final SeasonRecipe BELL_PEPPER_GROWING_SEASON = registerBase("Bell Pepper Growing Season");
    public static final SeasonRecipe BLACK_SAGE_GROWING_SEASON = registerBase("Black Sage Growing Season");
    public static final SeasonRecipe BROADLEAF_PLANTAIN_GROWING_SEASON = registerBase("Broadleaf Plantain Growing Season");
    public static final SeasonRecipe BROCCOLI_GROWING_SEASON = registerBase("Broccoli Growing Season");
    public static final SeasonRecipe CABBAGE_GROWING_SEASON = registerBase("Cabbage Growing Season");
    public static final SeasonRecipe CARROT_GROWING_SEASON = registerBase("Carrot Growing Season");
    public static final SeasonRecipe CAULIFLOWER_GROWING_SEASON = registerBase("Cauliflower Growing Season");
    public static final SeasonRecipe CHAMOMILE_GROWING_SEASON = registerBase("Chamomile Growing Season");
    public static final SeasonRecipe CHIVES_GROWING_SEASON = registerBase("Chives Growing Season");
    public static final SeasonRecipe CILANTRO_GROWING_SEASON = registerBase("Cilantro Growing Season");
    public static final SeasonRecipe COMFREY_GROWING_SEASON = registerBase("Comfrey Growing Season");
    public static final SeasonRecipe COMMON_MALLOW_GROWING_SEASON = registerBase("Common Mallow Growing Season");
    public static final SeasonRecipe CORN_GROWING_SEASON = registerBase("Corn Growing Season");
    public static final SeasonRecipe CUCUMBER_GROWING_SEASON = registerBase("Cucumber Growing Season");
    public static final SeasonRecipe FLAX_GROWING_SEASON = registerBase("Flax Growing Season");
    public static final SeasonRecipe GARLIC_GROWING_SEASON = registerBase("Garlic Growing Season");
    public static final SeasonRecipe GREEN_PEA_GROWING_SEASON = registerBase("Green Pea Growing Season");
    public static final SeasonRecipe HABANERO_GROWING_SEASON = registerBase("Habanero Growing Season");
    public static final SeasonRecipe HEMP_GROWING_SEASON = registerBase("Hemp Growing Season");
    public static final SeasonRecipe HOPS_GROWING_SEASON = registerBase("Hops Growing Season");
    public static final SeasonRecipe JALAPENO_GROWING_SEASON = registerBase("Jalapeno Growing Season");
    public static final SeasonRecipe KALE_GROWING_SEASON = registerBase("Kale Growing Season");
    public static final SeasonRecipe LAVENDER_GROWING_SEASON = registerBase("Lavender Growing Season");
    public static final SeasonRecipe LEEK_GROWING_SEASON = registerBase("Leek Growing Season");
    public static final SeasonRecipe LEMONGRASS_GROWING_SEASON = registerBase("Lemongrass Growing Season");
    public static final SeasonRecipe LETTUCE_GROWING_SEASON = registerBase("Lettuce Growing Season");
    public static final SeasonRecipe MARIGOLD_GROWING_SEASON = registerBase("Marigold Growing Season");
    public static final SeasonRecipe MINT_GROWING_SEASON = registerBase("Mint Growing Season");
    public static final SeasonRecipe ONION_GROWING_SEASON = registerBase("Onion Growing Season");
    public static final SeasonRecipe OREGANO_GROWING_SEASON = registerBase("Oregano Growing Season");
    public static final SeasonRecipe PARSLEY_GROWING_SEASON = registerBase("Parsley Growing Season");
    public static final SeasonRecipe POPPY_GROWING_SEASON = registerBase("Poppy Growing Season");
    public static final SeasonRecipe POTATO_GROWING_SEASON = registerBase("Potato Growing Season");
    public static final SeasonRecipe PUMPKIN_GROWING_SEASON = registerBase("Pumpkin Growing Season");
    public static final SeasonRecipe RADISH_GROWING_SEASON = registerBase("Radish Growing Season");
    public static final SeasonRecipe ROSEMARY_GROWING_SEASON = registerBase("Rosemary Growing Season");
    public static final SeasonRecipe ROSE_GROWING_SEASON = registerBase("Rose Growing Season");
    public static final SeasonRecipe RYE_GROWING_SEASON = registerBase("Rye Growing Season");
    public static final SeasonRecipe SAGE_GROWING_SEASON = registerBase("Sage Growing Season");
    public static final SeasonRecipe SOYBEAN_GROWING_SEASON = registerBase("Soybean Growing Season");
    public static final SeasonRecipe SPINACH_GROWING_SEASON = registerBase("Spinach Growing Season");
    public static final SeasonRecipe STRAWBERRY_GROWING_SEASON = registerBase("Strawberry Growing Season");
    public static final SeasonRecipe SUGAR_BEET_GROWING_SEASON = registerBase("Sugar Beet Growing Season");
    public static final SeasonRecipe SUNFLOWER_GROWING_SEASON = registerBase("Sunflower Growing Season");
    public static final SeasonRecipe SWEET_POTATO_GROWING_SEASON = registerBase("Sweet Potato Growing Season");
    public static final SeasonRecipe THYME_GROWING_SEASON = registerBase("Thyme Growing Season");
    public static final SeasonRecipe TOBACCO_GROWING_SEASON = registerBase("Tobacco Growing Season");
    public static final SeasonRecipe TOMATO_GROWING_SEASON = registerBase("Tomato Growing Season");
    public static final SeasonRecipe TURNIP_GROWING_SEASON = registerBase("Turnip Growing Season");
    public static final SeasonRecipe WATERMELON_GROWING_SEASON = registerBase("Watermelon Growing Season");
    public static final SeasonRecipe WHEAT_GROWING_SEASON = registerBase("Wheat Growing Season");
    public static final SeasonRecipe WILD_GARLIC_GROWING_SEASON = registerBase("Wild Garlic Growing Season");
    public static final SeasonRecipe ZUCCHINI_GROWING_SEASON = registerBase("Zucchini Growing Season");
    private final String translationName;

    private SeasonRecipe(String id) {
        this.translationName = id;
    }

    public static SeasonRecipe register(String id) {
        return register(false, id);
    }

    private static SeasonRecipe registerBase(String id) {
        return register(true, id);
    }

    private static SeasonRecipe register(boolean allowDefaultNamespace, String id) {
        return Registries.SEASON_RECIPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new SeasonRecipe(id));
    }

    public static SeasonRecipe get(ResourceLocation id) {
        return Registries.SEASON_RECIPE.get(id);
    }

    @Override
    public String toString() {
        return Registries.SEASON_RECIPE.getLocation(this).toString();
    }

    public ResourceLocation getRegistryId() {
        return Registries.SEASON_RECIPE.getLocation(this);
    }

    public String getTranslationName() {
        return this.translationName;
    }
}
