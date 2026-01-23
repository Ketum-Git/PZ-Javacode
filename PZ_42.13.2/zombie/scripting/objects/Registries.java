// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.properties.TilePropertyKey;

@UsedFromLua
public class Registries {
    public static final Registry<Registry<?>> REGISTRY = new Registry<>("registry");
    private static final List<Supplier<?>> BOOTSTRAPS = new ArrayList<>();
    public static final Registry<AmmoType> AMMO_TYPE = register("ammo_type", () -> AmmoType.BULLETS_44);
    public static final Registry<Book> BOOK = register("book", () -> Book.WORLDS_UNLIKELIEST_PLANE_CRASHES);
    public static final Registry<BookSubject> BOOK_SUBJECT = register("book_subject", () -> BookSubject.ADVENTURE_NON_FICTION);
    public static final Registry<Brochure> BROCHURE = register("brochure", () -> Brochure.AIRPORT);
    public static final Registry<Business> BUSINESS = register("business", () -> Business.BEANZ);
    public static final Registry<CharacterProfession> CHARACTER_PROFESSION = register("character_profession", () -> CharacterProfession.BURGLAR);
    public static final Registry<CharacterTrait> CHARACTER_TRAIT = register("character_trait", () -> CharacterTrait.ADRENALINE_JUNKIE);
    public static final Registry<ComicBook> COMIC_BOOK = register("comic_book", () -> ComicBook.BLASTFORCE);
    public static final Registry<Flier> FLIER = register("flier", () -> Flier.A1_HAY);
    public static final Registry<TilePropertyKey> TILE_PROPERTY_KEY = register("tile_property_key", () -> TilePropertyKey.ALWAYS_DRAW);
    public static final Registry<ItemBodyLocation> ITEM_BODY_LOCATION = register("item_body_location", () -> ItemBodyLocation.HAT);
    public static final Registry<ItemTag> ITEM_TAG = register("item_tag", () -> ItemTag.IS_MEMENTO);
    public static final Registry<ItemType> ITEM_TYPE = register("item_type", () -> ItemType.FOOD);
    public static final Registry<Job> JOB = register("job", () -> Job.ACCOUNTANT);
    public static final Registry<Magazine> MAGAZINE = register("magazine", () -> Magazine.AIR_AND_SPACE_NEWS);
    public static final Registry<MagazineSubject> MAGAZINE_SUBJECT = register("magazine_subject", () -> MagazineSubject.ART);
    public static final Registry<MetaRecipe> META_RECIPE = register("meta_recipe", () -> MetaRecipe.ASSEMBLE_ADVANCED_FRAMEPACK);
    public static final Registry<MoodleType> MOODLE_TYPE = register("moodle_type", () -> MoodleType.ENDURANCE);
    public static final Registry<Newspaper> NEWSPAPER = register("newspaper", () -> Newspaper.KENTUCKY_HERALD);
    public static final Registry<OldNewspaper> OLD_NEWSPAPER = register("old_newspaper", () -> OldNewspaper.BOWLING_GREEN_POST);
    public static final Registry<PetName> PET_NAME = register("pet_name", () -> PetName.BEN);
    public static final Registry<Photo> PHOTO = register("photo", () -> Photo.A_BABY);
    public static final Registry<SeasonRecipe> SEASON_RECIPE = register("season_recipe", () -> SeasonRecipe.BARLEY_GROWING_SEASON);
    public static final Registry<WeaponCategory> WEAPON_CATEGORY = register("weapon_category", () -> WeaponCategory.BLUNT);

    public static <T> Registry<T> register(String name, Supplier<T> bootstrap) {
        Registry<T> registry = (Registry<T>)REGISTRY.register(ResourceLocation.of(name), new Registry(name));
        BOOTSTRAPS.add(bootstrap);
        return registry;
    }

    public static List<Registry<? extends RecipeKey>> getAllRecipeRegistries() {
        List<Registry<? extends RecipeKey>> recipeRegistries = new ArrayList<>();
        recipeRegistries.add(META_RECIPE);
        return recipeRegistries;
    }

    static {
        BOOTSTRAPS.forEach(Supplier::get);
        if (Core.IS_DEV) {
            REGISTRY.forEach(registry -> {
                if (registry.values().stream().anyMatch(Objects::isNull)) {
                    throw new IllegalStateException("Registry %s has null values".formatted(REGISTRY.getLocation((Registry<?>)registry)));
                }
            });
            REGISTRY.forEach(
                registry -> {
                    Optional<?> first = registry.values().stream().findFirst();
                    if (first.isEmpty()) {
                        throw new IllegalStateException("Registry %s was empty".formatted(REGISTRY.getLocation((Registry<?>)registry)));
                    } else {
                        Class<?> clazz = first.get().getClass();
                        if (Arrays.stream(clazz.getConstructors()).anyMatch(c -> Modifier.isPublic(c.getModifiers()))) {
                            throw new IllegalStateException(
                                "Registry %s is holding objects with a public constructor".formatted(REGISTRY.getLocation((Registry<?>)registry))
                            );
                        } else if (Arrays.stream(clazz.getDeclaredMethods())
                            .noneMatch(m -> m.getName().equals("registerBase") && Modifier.isPrivate(m.getModifiers()))) {
                            throw new IllegalStateException(
                                "Registry %s is missing a private registerBase, are we sure we set this up to be resettable?"
                                    .formatted(REGISTRY.getLocation((Registry<?>)registry))
                            );
                        } else {
                            try {
                                clazz.getDeclaredMethod("equals", Object.class);
                                throw new IllegalStateException(
                                    "Registry %s of %s should not override equals(Object)"
                                        .formatted(REGISTRY.getLocation((Registry<?>)registry), clazz.getName())
                                );
                            } catch (NoSuchMethodException var5) {
                                try {
                                    clazz.getDeclaredMethod("hashCode");
                                    throw new IllegalStateException(
                                        "Registry %s of %s should not override hashCode()"
                                            .formatted(REGISTRY.getLocation((Registry<?>)registry), clazz.getName())
                                    );
                                } catch (NoSuchMethodException var4) {
                                }
                            }
                        }
                    }
                }
            );
        }
    }
}
