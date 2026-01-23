// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import generation.ItemGenerationConstants;
import java.util.List;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.SurvivorFactory;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Literature;
import zombie.scripting.objects.Book;
import zombie.scripting.objects.Brochure;
import zombie.scripting.objects.ComicBook;
import zombie.scripting.objects.Flier;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.Magazine;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.Newspaper;
import zombie.scripting.objects.OldNewspaper;
import zombie.scripting.objects.Photo;
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

    public static void onCreateOldPhoto(InventoryItem item) {
        onCreatePhoto(item, "OldPhoto", ItemGenerationConstants.OLD_PHOTOS);
    }

    public static void onCreateSecretPhoto(InventoryItem item) {
        onCreatePhoto(item, "SecretPhoto", ItemGenerationConstants.SECRET_PHOTOS);
    }

    public static void onCreateRacyPhoto(InventoryItem item) {
        onCreatePhoto(item, "RacyPhoto", ItemGenerationConstants.RACY_PHOTOS);
    }

    public static void onCreateVeryOldPhoto(InventoryItem item) {
        onCreatePhoto(item, "VeryOldPhoto", ItemGenerationConstants.VERY_OLD_PHOTOS);
    }

    private static void onCreatePhoto(InventoryItem item, String type, List<Photo> list) {
        String photo = Rand.Next(list).getTranslationKey();
        String title = Translator.getText("IGUI_PhotoOf", item.getDisplayName(), Translator.getText(photo));
        String literatureTitle = String.format("%s_%s_%d", type, photo, Rand.Next(1000000));
        setMediaName(item, title, "literatureTitle", literatureTitle);
        item.getModData().rawset("collectibleKey", photo);
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
}
