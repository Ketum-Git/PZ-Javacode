// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.SurvivorFactory;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemGenerationConstants;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Literature;
import zombie.iso.IsoCell;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.objects.Book;
import zombie.scripting.objects.Brochure;
import zombie.scripting.objects.ComicBook;
import zombie.scripting.objects.CraftRecipeKey;
import zombie.scripting.objects.Flier;
import zombie.scripting.objects.FluidKey;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.Magazine;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.Newspaper;
import zombie.scripting.objects.OldNewspaper;
import zombie.scripting.objects.Registries;

@UsedFromLua
public class ItemCodeOnCreate extends RecipeCodeHelper {
    public static final String COLLECTIBLE_KEY = "collectibleKey";
    public static final String LITERATURE_TITLE = "literatureTitle";
    public static final String PRINT_MEDIA = "printMedia";
    public static final String PRINT_MEDIA_INFO = "info";
    public static final String PRINT_MEDIA_ID = "id";
    public static final String PRINT_MEDIA_TITLE = "title";
    public static final String PRINT_MEDIA_TEXT = "text";

    public static void scratchTicketWinner(Literature item) {
        scratchTicketWinner(null, item);
    }

    public static void onCreateStockCertificate(Literature item) {
        String displayName = item.getDisplayName();
        item.getModData().rawset("collectibleKey", displayName);
        String text;
        if (Rand.NextBool(2)) {
            text = Translator.getText(
                "IGUI_ItemWithDisplayNameNoQuote", displayName, Translator.getText(Rand.Next(ItemGenerationConstants.STOCK_CERTIFICATE_2).getTranslation())
            );
            item.setTexture(Texture.getSharedTexture("Item_StockCertificate2"));
            item.setWorldStaticModel(ModelKey.STOCK_CERTIFICATE_2);
        } else {
            text = Translator.getText(
                "IGUI_ItemWithDisplayNameNoQuote", displayName, Translator.getText(Rand.Next(ItemGenerationConstants.STOCK_CERTIFICATE_1).getTranslation())
            );
        }

        item.setName(text);
        item.setCustomName(true);
    }

    public static void onCreatePaperwork(Literature item) {
        int num = Rand.NextInclusive(1, 6);
        item.setTexture(Texture.getSharedTexture("Item_Paperwork" + num));
        switch (num) {
            case 1:
                item.setWorldStaticModel(ModelKey.PAPERWORK_1);
                break;
            case 2:
                item.setWorldStaticModel(ModelKey.PAPERWORK_2);
                break;
            case 3:
                item.setWorldStaticModel(ModelKey.PAPERWORK_3);
                break;
            case 4:
                item.setWorldStaticModel(ModelKey.PAPERWORK_4);
                break;
            case 5:
                item.setWorldStaticModel(ModelKey.PAPERWORK_5);
                break;
            default:
                item.setWorldStaticModel(ModelKey.PAPERWORK_6);
        }
    }

    public static void onCreateMonogram(Clothing item) {
        item.setName(
            Translator.getText(
                "IGUI_ItemWithDisplayName",
                item.getDisplayName(),
                SurvivorFactory.getRandomForename(Rand.NextBool(2)) + " " + SurvivorFactory.getRandomSurname()
            )
        );
    }

    public static void onCreateIDCard(InventoryItem item) {
        onCreateIDCard(item, Rand.NextBool(2));
    }

    public static void onCreateIDCardFemale(InventoryItem item) {
        onCreateIDCard(item, true);
    }

    public static void onCreateIDCardMale(InventoryItem item) {
        onCreateIDCard(item, false);
    }

    private static void onCreateIDCard(InventoryItem item, boolean female) {
        item.setName(
            Translator.getText(
                "IGUI_ItemWithDisplayName", item.getDisplayName(), SurvivorFactory.getRandomForename(female) + " " + SurvivorFactory.getRandomSurname()
            )
        );
        if (item instanceof Literature literature) {
            literature.setLockedBy(String.valueOf(Rand.Next(1000000)));
        }
    }

    public static void onCreateDogTagPet(InventoryItem item) {
        String petName = Rand.Next(ItemGenerationConstants.PET_NAMES).getTranslationKey();
        String dogTagName = Translator.getText("IGUI_ItemWithDisplayName", item.getDisplayName(), Translator.getText(petName));
        setMediaName(item, dogTagName, "collectibleKey", dogTagName);
    }

    public static void onCreatePhoto(InventoryItem item) {
        onCreatePhoto(item, "OldPhoto", Rand.Next(ItemGenerationConstants.PHOTOS).getTranslationKey());
    }

    public static void onCreateSecretPhoto(InventoryItem item) {
        onCreatePhoto(item, "SecretPhoto", Rand.Next(ItemGenerationConstants.SECRET_PHOTOS).getTranslationKey());
    }

    public static void onCreateRacyPhoto(InventoryItem item) {
        onCreatePhoto(item, "RacyPhoto", Rand.Next(ItemGenerationConstants.RACY_PHOTOS).getTranslationKey());
    }

    public static void onCreateVeryOldPhoto(InventoryItem item) {
        onCreatePhoto(item, "VeryOldPhoto", Rand.Next(ItemGenerationConstants.VERY_OLD_PHOTOS).getTranslationKey());
    }

    private static void onCreatePhoto(InventoryItem item, String type, String translationKey) {
        String title = Translator.getText("IGUI_PhotoOf", item.getDisplayName(), Translator.getText(translationKey));
        String literatureTitle = String.format("%s_%s_%d", type, translationKey, Rand.Next(1000000));
        setMediaName(item, title, "literatureTitle", literatureTitle);
        item.getModData().rawset("collectibleKey", translationKey);
    }

    public static void onCreateHottieZ(InventoryItem item) {
        boolean hunkZ = Rand.NextBool(20);
        RecipeCodeHelper.DateResult dateResult = getDate(item, hunkZ ? 1973 : 1953);
        String name = item.getDisplayName();
        if (hunkZ) {
            item.setTexture(Texture.getSharedTexture("Item_MagazineNudie2"));
            item.setWorldStaticModel(ModelKey.HOTTIE_ZGROUND_2);
            item.setStaticModel(ModelKey.HOTTIE_Z2);
            name = Translator.getText("IGUI_MagazineTitle_HunkZ");
        }

        setMagazineName(item, hunkZ ? "HunkZ" : "HottieZ", name, dateResult);
    }

    private static void setMagazineName(InventoryItem item, String type, String name, RecipeCodeHelper.DateResult dateResult) {
        String title = Translator.getText("IGUI_MagazineName", name, Translator.getText("Sandbox_StartMonth_option" + dateResult.month()), dateResult.year());
        String literatureTitle = String.format("%s_%d_%d", type, dateResult.month(), dateResult.year());
        setMediaName(item, title, "literatureTitle", literatureTitle);
    }

    private static RecipeCodeHelper.DateResult getDate(InventoryItem item, int minYear) {
        int month = Rand.NextInclusive(1, 7);
        int year = SandboxOptions.getInstance().getFirstYear() + SandboxOptions.getInstance().startYear.getValue() - 1;
        if (!item.hasTag(ItemTag.NEW)) {
            year = minYear + Rand.Next(year - minYear);
            if (year != 1993) {
                month = Rand.NextInclusive(1, 12);
            }
        }

        return new RecipeCodeHelper.DateResult(year, month);
    }

    public static void onCreateOldNewspaper(InventoryItem item) {
        OldNewspaper paper = Rand.Next(ItemGenerationConstants.OLD_NEWSPAPER);
        String title = Translator.getText("IGUI_Newspaper_Name", item.getDisplayName(), Translator.getText(paper.getTranslationKey()));
        setMediaName(item, title, "collectibleKey", title);
    }

    public static void onCreateTVMagazine(InventoryItem item) {
        RecipeCodeHelper.DateResult dateResult = getDate(item, item.hasTag(ItemTag.NEW) ? 1993 : 1953);
        setMagazineName(item, "TVMagazine", item.getDisplayName(), dateResult);
    }

    public static void onCreateBrochure(InventoryItem item) {
        Brochure brochure = Rand.Next(Registries.BROCHURE.values());
        setFlierName(brochure.toString(), brochure.getTranslationKey(), brochure.getTranslationInfoKey(), brochure.getTranslationTextKey(), item);
    }

    public static void onCreateFlier(InventoryItem item) {
        Flier flier = Rand.Next(Registries.FLIER.values());
        setFlierName(flier.toString(), flier.getTranslationKey(), flier.getTranslationInfoKey(), flier.getTranslationTextKey(), item);
    }

    public static void onCreateFlierNolan(InventoryItem item) {
        setFlierName(
            Flier.NOLANS_USED_CARS.toString(),
            Flier.NOLANS_USED_CARS.getTranslationKey(),
            Flier.NOLANS_USED_CARS.getTranslationInfoKey(),
            Flier.NOLANS_USED_CARS.getTranslationTextKey(),
            item
        );
    }

    private static void setFlierName(String mediaId, String mediaTitle, String mediaInfo, String printText, InventoryItem item) {
        String title = Translator.getText("IGUI_Newspaper_Name", item.getDisplayName(), Translator.getText(mediaTitle));
        RecipeCodeHelper.setPrintMediaInfo(item, mediaTitle, mediaInfo, printText, mediaId);
        item.setName(title);
    }

    private static void setMediaName(InventoryItem item, String mediaName, String modDataKey, Object modDataValue) {
        item.setName(mediaName);
        item.getModData().rawset(modDataKey, modDataValue);
    }

    public static void onCreateComicBook(InventoryItem item) {
        nameComicBook(item, Rand.Next(Registries.COMIC_BOOK.values()));
    }

    public static void onCreateComicBookRetail(InventoryItem item) {
        ComicBook comicBook = Rand.Next(Registries.COMIC_BOOK.values());

        while (!comicBook.isInPrint()) {
            comicBook = Rand.Next(Registries.COMIC_BOOK.values());
        }

        nameComicBook(item, comicBook);
    }

    private static void nameComicBook(InventoryItem item, ComicBook comicBook) {
        int issues = comicBook.getIssues();
        String translationKey = comicBook.getTranslationKey();
        if (issues == 0) {
            item.setName(Translator.getText("IGUI_MagazineNameNoIssue", item.getDisplayName(), Translator.getText(translationKey)));
            item.getModData().rawset("literatureTitle", translationKey);
        } else {
            int issue = Rand.Next(issues);
            item.getModData().rawset("literatureTitle", translationKey + "#" + issue);
            String issueName = String.format("#%0" + String.valueOf(issues).length() + "d", issue);
            item.setName(Translator.getText("IGUI_MagazineName", item.getDisplayName(), Translator.getText(translationKey), issueName));
        }
    }

    public static void onCreateDispatchNewNewspaper(InventoryItem item) {
        nameNewspaper(item, Newspaper.NATIONAL_DISPATCH);
    }

    public static void onCreateHeraldNewNewspaper(InventoryItem item) {
        nameNewspaper(item, Newspaper.KENTUCKY_HERALD);
    }

    public static void onCreateKnewsNewNewspaper(InventoryItem item) {
        nameNewspaper(item, Newspaper.KNOX_KNEWS);
    }

    public static void onCreateTimesNewNewspaper(InventoryItem item) {
        nameNewspaper(item, Newspaper.LOUISVILLE_SUN_TIMES);
    }

    public static void onCreateRecentNewspaper(InventoryItem item) {
        nameNewspaper(item, Rand.Next(Registries.NEWSPAPER.values()));
    }

    public static void onCreateSubjectBook(InventoryItem item) {
        Book book = Rand.Next(Book.getBooksByCoverAndSubjects(item));
        item.setName(Translator.getText("IGUI_MagazineNameNoIssue", item.getDisplayName(), Translator.getText(book.translationKey())));
        item.getModData().rawset("literatureTitle", book.translationKey());
    }

    public static void onCreateSubjectMagazine(InventoryItem item) {
        Magazine magazine = item.getMagazineSubjects().isEmpty() ? Rand.Next(Registries.MAGAZINE.values()) : Rand.Next(Magazine.getMagazineBySubject(item));
        setMagazineName(item, magazine.translationKey(), Translator.getText(magazine.translationKey()), getDate(item, item.hasTag(ItemTag.NEW) ? 1993 : 1970));
    }

    public static void onCreateBusinessCard(InventoryItem item) {
        setBusinessCardName(item, Translator.getText(Rand.Next(Registries.JOB.values()).translationKey()));
    }

    public static void onCreateBusinessCardNolan(InventoryItem item) {
        setBusinessCardName(item, Translator.getText(Flier.NOLANS_USED_CARS.getTranslationKey()));
    }

    private static void setBusinessCardName(InventoryItem item, String job) {
        item.setName(
            Translator.getText(
                "IGUI_ItemWithDisplayNameAndJob",
                item.getDisplayName(),
                SurvivorFactory.getRandomForename(Rand.NextBool(2)) + " " + SurvivorFactory.getRandomSurname(),
                job
            )
        );
    }

    public static void onCreateCatalogue(InventoryItem item) {
        setLiteratureNameBasic(item, Rand.Next(Registries.CATALOGUE.values()).getTranslationKey());
    }

    public static void onCreateRpgManual(InventoryItem item) {
        setLiteratureNameBasic(item, Rand.Next(Registries.RPG_MANUAL.values()).getTranslationKey());
    }

    private static void setLiteratureNameBasic(InventoryItem item, String translationKey) {
        item.setName(Translator.getText("IGUI_MagazineNameNoIssue", item.getDisplayName(), Translator.getText(translationKey)));
        item.getModData().rawset("literatureTitle", item.getType() + "_" + translationKey);
    }

    public static void onCreateGenericMail(InventoryItem item) {
        item.setName(Translator.getText(ItemGenerationConstants.GENERIC_MAIL.getRandom().getTranslationKey()));
    }

    public static void onCreateLetterHandwritten(InventoryItem item) {
        item.setName(Translator.getText(ItemGenerationConstants.LETTER_HANDWRITTEN.getRandom().getTranslationKey()));
    }

    public static void onCreateLocket(InventoryItem item) {
        String translationKey = Rand.Next(Registries.LOCKET.values()).getTranslationKey();
        item.setName(Translator.getText("IGUI_LocketText", item.getDisplayName(), Translator.getText(translationKey)));
        item.getModData().rawset("collectibleKey", item.getType() + "_" + translationKey);
    }

    public static void onCreateDoodle(InventoryItem item) {
        onCreatePhoto(item, "Doodle", Rand.Next(Registries.DOODLE.values()).getTranslationKey());
    }

    public static void onCreateDoodleKids(InventoryItem item) {
        onCreatePhoto(item, "Doodle", Rand.Next(Registries.DOODLE_KIDS.values()).getTranslationKey());
    }

    public static void onCreatePostcard(InventoryItem item) {
        onCreatePhoto(item, "Postcard", Rand.Next(Registries.POSTCARD.values()).getTranslationKey());
    }

    public static void onCreateSnowGlobe(InventoryItem item) {
        String translationKey = ItemGenerationConstants.POSTCARD.getRandom().getTranslationKey();
        String title = Translator.getText("IGUI_SnowGlobeOf", item.getDisplayName(), Translator.getText(translationKey));
        item.setName(title);
        item.getModData().rawset("collectibleKey", title);
    }

    public static void onCreateRecipeClipping(Literature item) {
        String recipeName = Translator.getRecipeName(Rand.Next(ItemGenerationConstants.FOOD_RECIPES).id());
        item.setLearnedRecipes(List.of(recipeName));
        String text = Translator.getText("IGUI_MagazineNameNoIssue", item.getDisplayName(), recipeName);
        item.setName(text);
        item.getModData().rawset("collectibleKey", text);
    }

    public static void onCreateExplosivesSchematic(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.EXPLOSIVE_SCHEMATICS, 40);
    }

    public static void onCreateMeleeWeaponSchematic(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.MELEE_WEAPON_SCHEMATICS, 30);
    }

    public static void onCreateBlacksmithToolsSchematic(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.BLACKSMITH_TOOLS_SCHEMATICS, 50);
    }

    public static void onCreateArmorSchematic(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.ARMOR_SCHEMATICS, 30);
    }

    public static void onCreateCookwareSchematic(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.COOKWARE_SCHEMATIC, 40);
    }

    public static void onCreateSurvivalSchematic(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.SURVIVAL_SCHEMATICS, 40);
    }

    public static void onCreateSewingPattern(Literature item) {
        setSchematicLearnedRecipes(item, ItemGenerationConstants.SEWING_PATTERNS, 0);
    }

    private static void setSchematicLearnedRecipes(Literature item, List<CraftRecipeKey> list, int multipleChance) {
        int nbOfSchematics = 1;
        if (Rand.Next(100) < multipleChance) {
            nbOfSchematics = Math.min(5, Rand.NextInclusive(2, list.size()));
        }

        item.setLearnedRecipes(new ArrayList<>());

        for (int i = 0; i < nbOfSchematics; i++) {
            item.getLearnedRecipes().add(Rand.Next(list).id());
        }

        if (item.getLearnedRecipes().size() == 1) {
            String text = Translator.getText("IGUI_MagazineNameNoIssue", item.getDisplayName(), Translator.getRecipeName(item.getLearnedRecipes().getFirst()));
            item.setName(text);
            item.getModData().rawset("collectibleKey", text);
        }
    }

    public static void onCreateRecipeMagazine(Literature item) {
        item.getModData().rawset("literatureTitle", item.getFullType());
    }

    public static void onCreateScarecrow(InventoryItem item) {
        IsoMannequin obj = new IsoMannequin(IsoCell.getInstance(), null, IsoSpriteManager.instance.getSprite("location_shop_mall_01_68"));
        obj.setMannequinScriptName("MannequinScarecrow02");

        try {
            obj.setCustomSettingsToItem(item);
        } catch (IOException var3) {
            System.out.println("Couldn't set custom settings to scarecrow");
        }
    }

    public static void onCreateSkeletonDisplay(InventoryItem item) {
        IsoMannequin obj = new IsoMannequin(IsoCell.getInstance(), null, IsoSpriteManager.instance.getSprite("location_shop_mall_01_68"));
        obj.setMannequinScriptName("MannequinSkeleton01");

        try {
            obj.setCustomSettingsToItem(item);
        } catch (IOException var3) {
            System.out.println("Couldn't set custom settings to skeleton");
        }
    }

    public static void onCreateGasMask(Clothing item) {
        if (item.hasTag(ItemTag.GAS_MASK)) {
            createFilterForMask(item, ItemKey.Drainable.GASMASK_FILTER.id(), "filterType");
        } else if (item.hasTag(ItemTag.RESPIRATOR)) {
            createFilterForMask(item, ItemKey.Drainable.RESPIRATOR_FILTERS.id(), "filterType");
        } else if (item.hasTag(ItemTag.IMPROVISED_GAS_MASK)) {
            createFilterForMask(item, ItemKey.Drainable.RAG_FILTER.id(), "filterType");
        } else if (item.hasTag(ItemTag.SCBA)) {
            createFilterForMask(item, ItemKey.Drainable.OXYGEN_TANK.id(), "tankType");
        }
    }

    private static void createFilterForMask(Clothing item, String filter, String modDataType) {
        item.getModData().rawset(modDataType, filter);
        float value = Rand.Next(1000) / 1000.0F;
        item.getModData().rawset("usedDelta", value);
        item.setUsedDelta(value);
    }

    public static void onCreateHairDyeBottle(InventoryItem item) {
        Fluid fluid = item.getFluidContainer().getPrimaryFluid();
        if (fluid != null) {
            Color color = item.getFluidContainer().getColor();
            item.setColorRed(color.getR());
            item.setColorGreen(color.getG());
            item.setColorBlue(color.getB());
        }
    }

    public static void onCreatePopBottle(InventoryItem item) {
        String fluid = item.getFluidContainer().getPrimaryFluid().getFluidTypeString();
        item.setModelIndex(FluidKey.SODA_POP.id().equals(fluid) ? 1 : 0);
        Color finalColor = getColorForFluid(fluid, item.getFluidContainer().getColor());
        setCustomColor(item, finalColor);
    }

    private static Color getColorForFluid(String fluid, Color fallback) {
        if (FluidKey.COLA.id().equals(fluid)) {
            return new Color(1.0F, 0.0F, 0.0F);
        } else if (FluidKey.GINGER_ALE.id().equals(fluid)) {
            return new Color(0.0F, 0.0F, 1.0F);
        } else if (FluidKey.SODA_LIME.id().equals(fluid)) {
            return new Color(0.0F, 1.0F, 0.0F);
        } else {
            return FluidKey.SODA_POP.id().equals(fluid) ? new Color(1.0F, 0.8F, 0.0F) : fallback;
        }
    }

    public static void onCreateLipstick(InventoryItem item) {
        setCustomColor(item, ItemGenerationConstants.COLOR_LIPSTICK.getRandom());
    }

    public static void onCreateFabricRoll(InventoryItem item) {
        setCustomColor(item, ItemGenerationConstants.COLOR_LIPSTICK.getRandom());
    }

    public static void onCreateSprayPaint(InventoryItem item) {
        setCustomColor(item, ItemGenerationConstants.COLOR_SPRAY_PAINT.getRandom());
    }

    public static void onCreateToyPlane(InventoryItem item) {
        setCustomColor(item, ItemGenerationConstants.COLOR_TOY_PLANE.getRandom());
    }

    public static void onCreateRandomColor(InventoryItem item) {
        setCustomColor(item, Color.random());
    }

    private static void setCustomColor(InventoryItem item, Color color) {
        item.setColor(color);
        item.setColorRed(color.r);
        item.setColorGreen(color.g);
        item.setColorBlue(color.b);
        item.setCustomColor(true);
    }

    public static void onCreateSodaCan(InventoryItem item) {
        setCustomColor(item, item.getFluidContainer().getPrimaryFluid().getColor());
    }
}
