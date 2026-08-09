// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import zombie.core.Core;
import zombie.inventory.InventoryItem;

public class Magazine {
    private static final Map<MagazineSubject, List<Magazine>> BY_SUBJECTS = new HashMap<>();
    public static final Magazine AIR_AND_SPACE_NEWS = registerBase("AirandSpaceNews", 1950, MagazineSubject.MILITARY);
    public static final Magazine ALT_F4 = registerBase("AltF4", 1988, MagazineSubject.TECH);
    public static final Magazine AMERICAN_CYCLIST = registerBase("AmericanCyclist", 1892, MagazineSubject.SPORTS);
    public static final Magazine AMERICAN_HOBBIES = registerBase("AmericanHobbies", 1902, MagazineSubject.POPULAR, MagazineSubject.HOBBY);
    public static final Magazine AMERICAN_POET = registerBase("AmericanPoet", 1905);
    public static final Magazine ANDBORG = registerBase("Andborg", 1935, MagazineSubject.SCIENCE, MagazineSubject.TECH, MagazineSubject.HOBBY);
    public static final Magazine ART_AMERICA = registerBase("ArtAmerica", 1926, MagazineSubject.ART);
    public static final Magazine AU_NATUREL = registerBase("AuNaturel", 1904, MagazineSubject.OUTDOORS);
    public static final Magazine AUTOMOBILES_MONTHLY = registerBase("AutomobilesMonthly", 1927, MagazineSubject.CARS);
    public static final Magazine BACKROAD = registerBase("Backroad", 1922, MagazineSubject.OUTDOORS);
    public static final Magazine BASSLINE = registerBase("Bassline", 1954, MagazineSubject.MUSIC);
    public static final Magazine BEAUTY = registerBase("Beauty", 1967, MagazineSubject.FASHION);
    public static final Magazine BELIEF = registerBase("Belief", 1982);
    public static final Magazine BIG_BICEPS = registerBase("BigBiceps", 1984, MagazineSubject.HEALTH);
    public static final Magazine BLOCKO_MAGAZINE = registerBase("BlockoMagazine", 1987, MagazineSubject.CHILDS);
    public static final Magazine BLUESN_JAZZ = registerBase("BluesnJazz", 1922, MagazineSubject.MUSIC);
    public static final Magazine BOINK = registerBase("Boink", 1925, MagazineSubject.HUMOR);
    public static final Magazine BRASH = registerBase("Brash", 1978, MagazineSubject.FASHION);
    public static final Magazine BRIEF = registerBase("Brief", 1928);
    public static final Magazine CABIN_FEVER = registerBase("CabinFever", 1973, MagazineSubject.OUTDOORS);
    public static final Magazine CARD_GAMES = registerBase("CardGames", 1908, MagazineSubject.HOBBY);
    public static final Magazine CHARM = registerBase("Charm", 1961, MagazineSubject.FASHION);
    public static final Magazine CHECKMATE = registerBase("Checkmate", 1966, MagazineSubject.HOBBY);
    public static final Magazine CHRISTIANS_TOGETHER = registerBase("ChristiansTogether", 1870);
    public static final Magazine CIGARS_CAVIAR = registerBase("CigarsCaviar", 1921, MagazineSubject.RICH);
    public static final Magazine CLASSIC_BATTLES = registerBase("ClassicBattles", 1944, MagazineSubject.MILITARY);
    public static final Magazine CODE_WORLD = registerBase("CodeWorld", 1979, MagazineSubject.TECH);
    public static final Magazine COLLECTING = registerBase("Collecting", 1921, MagazineSubject.HOBBY);
    public static final Magazine COMPRESSED_BOOKS = registerBase("CompressedBooks", 1922, MagazineSubject.POPULAR);
    public static final Magazine CONGRESS_WATCHER = registerBase("CongressWatcher", 1972);
    public static final Magazine CUSTODIAL_OPERATOR = registerBase("CustodialOperator", 1992, MagazineSubject.POLICE);
    public static final Magazine DARK_TALES = registerBase("DarkTales", 1923, MagazineSubject.HORROR);
    public static final Magazine DIGITAL_ADVENTURE_POWER = registerBase(
        "DigitalAdventurePower", 1983, MagazineSubject.TECH, MagazineSubject.TEENS, MagazineSubject.HOBBY
    );
    public static final Magazine DRAG_KINGS = registerBase("DragKings", 1978, MagazineSubject.CARS);
    public static final Magazine ECONOMY = registerBase("Economy", 1971, MagazineSubject.BUSINESS);
    public static final Magazine ELECTRON = registerBase("Electron", 1960, MagazineSubject.SCIENCE);
    public static final Magazine ELEGANT = registerBase("Elegant", 1954, MagazineSubject.POPULAR, MagazineSubject.FASHION);
    public static final Magazine ELEGANT_GIRL = registerBase("ElegantGirl", 1960, MagazineSubject.TEENS);
    public static final Magazine EVERYDAY_MOTORIST = registerBase("EverydayMotorist", 1949, MagazineSubject.CARS);
    public static final Magazine EXECUTIVE_LOUNGE = registerBase("ExecutiveLounge", 1957, MagazineSubject.RICH);
    public static final Magazine EXTINGUISH = registerBase("Extinguish", 1991);
    public static final Magazine FAMILY_CARING = registerBase("FamilyCaring", 1990);
    public static final Magazine FIGARY = registerBase("Figary", 1990, MagazineSubject.POPULAR, MagazineSubject.HUMOR);
    public static final Magazine FINE = registerBase("Fine", 1969);
    public static final Magazine FINE_WINE = registerBase("FineWine", 1989, MagazineSubject.RICH);
    public static final Magazine FIRE_SAFETY_NEWS = registerBase("FireSafetyNews", 1981, MagazineSubject.POLICE);
    public static final Magazine FLYING_SAUCER_JOURNAL = registerBase("FlyingSaucerJournal", 1947, MagazineSubject.HORROR);
    public static final Magazine FOOTBALL_FRENZY = registerBase("FootballFrenzy", 1983, MagazineSubject.SPORTS);
    public static final Magazine FORCES = registerBase("Forces", 1968);
    public static final Magazine FORE = registerBase("Fore", 1982, MagazineSubject.GOLF, MagazineSubject.RICH, MagazineSubject.SPORTS);
    public static final Magazine FOREVER_LIVING = registerBase("ForeverLiving", 1989, MagazineSubject.HEALTH);
    public static final Magazine FOUL_INTENTIONS = registerBase("FowlIntentions", 1958, MagazineSubject.OUTDOORS);
    public static final Magazine FREEWHEELIN = registerBase("Freewheelin", 1986, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine FUTURE_PALEONTOLOGY = registerBase("FuturePaleontology", 1987, MagazineSubject.CHILDS, MagazineSubject.SCIENCE);
    public static final Magazine GAME_Z = registerBase(
        "GameZ", 1989, MagazineSubject.CHILDS, MagazineSubject.TECH, MagazineSubject.TEENS, MagazineSubject.HOBBY, MagazineSubject.GAMING
    );
    public static final Magazine GHOSTLY_HAUNTINGS = registerBase("GhostlyHauntings", 1991, MagazineSubject.HORROR);
    public static final Magazine GREENS = registerBase("Greens", 1987, MagazineSubject.GOLF, MagazineSubject.RICH, MagazineSubject.SPORTS);
    public static final Magazine GRUESOME_CRIME_SCENES = registerBase("GruesomeCrimeScenes", 1931, MagazineSubject.HORROR, MagazineSubject.CRIME);
    public static final Magazine HEY_SIR = registerBase("HeySir", 1990);
    public static final Magazine HIGH_FLYER = registerBase("HighFlyer", 1942, MagazineSubject.RICH);
    public static final Magazine HIKING_FOOTWEAR = registerBase("HikingFootwear", 1987, MagazineSubject.OUTDOORS);
    public static final Magazine HOLLYWOOD_NEWS = registerBase("HollywoodNews", 1962, MagazineSubject.CINEMA);
    public static final Magazine HOME_RUN = registerBase("HomeRun", 1955, MagazineSubject.SPORTS);
    public static final Magazine HOSPITAL_TECHNOLOGY = registerBase("HospitalTechnology", 1982);
    public static final Magazine HOUSEHOLD_FINANCES = registerBase("HouseholdFinances", 1986);
    public static final Magazine IMPROVE_YOUR_HOUSE = registerBase("ImproveYourHouse", 1980, MagazineSubject.POPULAR);
    public static final Magazine IN_FOCUS = registerBase("InFocus", 1971, MagazineSubject.ART);
    public static final Magazine INCREDIBLE_HEROES = registerBase("IncredibleHeroes", 1911, MagazineSubject.CHILDS, MagazineSubject.MILITARY);
    public static final Magazine INQUIRE = registerBase("Inquire", 1933, MagazineSubject.POPULAR);
    public static final Magazine JOKES_JOKES = registerBase("JokesJokes", 1967, MagazineSubject.HUMOR);
    public static final Magazine JUMP = registerBase("Jump", 1979, MagazineSubject.TEENS);
    public static final Magazine KENTUCKY_DRIVER = registerBase("KentuckyDriver", 1933, MagazineSubject.CARS);
    public static final Magazine KENTUCKY_HORSE_RACING = registerBase("KentuckyHorseRacing", 1899, MagazineSubject.SPORTS);
    public static final Magazine KENTUCKY_LIBERAL = registerBase("KentuckyLiberal", 1940);
    public static final Magazine KENTUCKY_OBSERVER = registerBase("KentuckyObserver", 1959, MagazineSubject.POPULAR);
    public static final Magazine KIMS_TANKS_AND_ARMOR_MONTHLY = registerBase("KimsTanksandArmorMonthly", 1956, MagazineSubject.MILITARY);
    public static final Magazine KIRRUS_COMPUTING = registerBase("KirrusComputing", 1990, MagazineSubject.TECH);
    public static final Magazine KOOL_KIDS = registerBase("KoolKids", 1989, MagazineSubject.CHILDS);
    public static final Magazine LABYRINTHS = registerBase("Labyrinths", 1991, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine LATEST_FORENSCIS = registerBase("LatestForensics", 1992, MagazineSubject.POLICE, MagazineSubject.CRIME);
    public static final Magazine LIFESTYLE = registerBase("Lifestyle", 1990, MagazineSubject.POPULAR);
    public static final Magazine LOUISVILLE_BUSINESS_REVIEW = registerBase("LouisvilleBusinessReview", 1919, MagazineSubject.BUSINESS);
    public static final Magazine LOUISVILLE_LAUGHTER = registerBase("LouisvilleLaughter", 1939, MagazineSubject.HUMOR);
    public static final Magazine MAJESTIC_BIG_GAME = registerBase("MajesticBigGame", 1923, MagazineSubject.OUTDOORS);
    public static final Magazine MALPRACTICE_INSURANCE_MONTHLY = registerBase("MalpracticeInsuranceMonthly", 1990, MagazineSubject.RICH);
    public static final Magazine MANS_HEALTH = registerBase("MansHealth", 1986, MagazineSubject.POPULAR, MagazineSubject.HEALTH);
    public static final Magazine MARKETS_MONTHLY = registerBase("MarketsMonthly", 1958, MagazineSubject.BUSINESS);
    public static final Magazine ME = registerBase("Me", 1989);
    public static final Magazine MERC = registerBase("Merc", 1984, MagazineSubject.MILITARY);
    public static final Magazine MODERN_DANCE = registerBase("ModernDance", 1990, MagazineSubject.ART);
    public static final Magazine MODERN_TRAINS = registerBase("ModernTrains", 1925);
    public static final Magazine MONSTROUS_TRUCKS = registerBase("MonstrousTrucks", 1975, MagazineSubject.CARS, MagazineSubject.TEENS);
    public static final Magazine MOSSY_ROCK = registerBase("MossyRock", 1967, MagazineSubject.POPULAR, MagazineSubject.MUSIC);
    public static final Magazine MOTORCYCLE_ENTHUSIAST = registerBase("MotorcycleEnthusiast", 1950, MagazineSubject.CARS);
    public static final Magazine MOVIES_WEEKLY = registerBase("MoviesWeekly", 1934, MagazineSubject.CINEMA);
    public static final Magazine NATIONAL_CELEBS = registerBase("NationalCelebs", 1948, MagazineSubject.POPULAR);
    public static final Magazine NATURE_FACTS = registerBase("NatureFacts", 1930, MagazineSubject.CHILDS);
    public static final Magazine NEWSDAY = registerBase("Newsday", 1989, MagazineSubject.POPULAR);
    public static final Magazine NEWTON = registerBase("Newton", 1956, MagazineSubject.POPULAR, MagazineSubject.SCIENCE);
    public static final Magazine NUMISATICS = registerBase("Numisatics", 1967);
    public static final Magazine OLD_WEST_LIFE = registerBase("OldWestLife", 1911);
    public static final Magazine OPEN_MIND = registerBase("OpenMind", 1953, MagazineSubject.SCIENCE);
    public static final Magazine OSCC_INSIDER = registerBase("OSCCInsider", 1951, MagazineSubject.CARS, MagazineSubject.SPORTS);
    public static final Magazine PALE_GNOME = registerBase("PaleGnome", 1990, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine PARENTAGE = registerBase("Parentage", 1989);
    public static final Magazine PAWS = registerBase("Paws", 1988, MagazineSubject.CHILDS);
    public static final Magazine PHILATELTY_LATE = registerBase("PhilatelyLately", 1990);
    public static final Magazine PHOTOSPREAD = registerBase("Photospread", 1986, MagazineSubject.ART);
    public static final Magazine PILED_DRIVER = registerBase("Piledriver", 1989, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine POLICE_FILES = registerBase("PoliceFiles", 1940, MagazineSubject.POLICE, MagazineSubject.CRIME);
    public static final Magazine PROLETARIAT = registerBase("Proletariat", 1917);
    public static final Magazine READINGS_MONTHLY = registerBase("ReadingsMonthly", 1920, MagazineSubject.POPULAR);
    public static final Magazine REAL_ESTATE_INVESTMENT = registerBase("RealEstateInvestment", 1982, MagazineSubject.RICH, MagazineSubject.BUSINESS);
    public static final Magazine REELING_IN_AND_GEAR = registerBase("ReelinginandGear", 1979, MagazineSubject.OUTDOORS);
    public static final Magazine REVELATIONS = registerBase("Revelations", 1970);
    public static final Magazine ROCK_OUT = registerBase("RockOut", 1980, MagazineSubject.MUSIC, MagazineSubject.TEENS);
    public static final Magazine RUNNING_LIFE = registerBase("RunningLife", 1986, MagazineSubject.HEALTH);
    public static final Magazine RUNNING_N_GUNNING = registerBase("RunningnGunning", 1978, MagazineSubject.FIREARM);
    public static final Magazine SCOPED = registerBase("Scoped", 1977, MagazineSubject.FIREARM);
    public static final Magazine SCREEN_SHRIEK = registerBase("ScreenShriek", 1963, MagazineSubject.HORROR);
    public static final Magazine SECOND_AMENDMENT = registerBase("SecondAmendment", 1990, MagazineSubject.FIREARM);
    public static final Magazine SHORTS_ILLUSTRATED = registerBase("ShortsIllustrated", 1954, MagazineSubject.FASHION);
    public static final Magazine SILVER_SCREEN = registerBase("SilverScreen", 1937, MagazineSubject.CINEMA);
    public static final Magazine SIXTEEN = registerBase("Sixteen", 1944, MagazineSubject.TEENS);
    public static final Magazine SKEPTICAL = registerBase("Skeptical", 1976, MagazineSubject.SCIENCE);
    public static final Magazine SONG_AND_DANCE = registerBase("SongandDance", 1921, MagazineSubject.MUSIC);
    public static final Magazine SORBES = registerBase("Sorbes", 1916, MagazineSubject.BUSINESS);
    public static final Magazine SOUTHERN_LITERARY_REVIEW = registerBase("SouthernLiteraryReview", 1911);
    public static final Magazine SPORTS_STATS = registerBase("SportsStats", 1900, MagazineSubject.SPORTS);
    public static final Magazine SPRINTER = registerBase("Sprinter", 1986, MagazineSubject.HEALTH);
    public static final Magazine ST_PATRICKS_JOURNAL = registerBase("StPatricksJournal", 1903);
    public static final Magazine STAND_YOUR_GROUND = registerBase("StandYourGround", 1988, MagazineSubject.FIREARM);
    public static final Magazine STOCK_TRENDS = registerBase("StockTrends", 1983, MagazineSubject.BUSINESS);
    public static final Magazine STYLE = registerBase("Style", 1957, MagazineSubject.FASHION);
    public static final Magazine STYLE_LIFE = registerBase("StyleLife", 1963);
    public static final Magazine SUSPECTS_AND_WITNESSES = registerBase("SuspectsandWitnesses", 1929, MagazineSubject.POLICE);
    public static final Magazine T_POWER = registerBase("TPower", 1985, MagazineSubject.GOLF, MagazineSubject.RICH, MagazineSubject.SPORTS);
    public static final Magazine TAKE_A_LIFE = registerBase("TakeaLife", 1990, MagazineSubject.CRIME);
    public static final Magazine TELLTALE = registerBase("Telltale", 1989, MagazineSubject.POPULAR);
    public static final Magazine THE_BICLOPS = registerBase("TheBiclops", 1984, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine THE_BIG_APPLE = registerBase("TheBigApple", 1925);
    public static final Magazine THE_FOREIGN_REVIEW = registerBase("TheForeignReview", 1914);
    public static final Magazine THE_NATIONALIST = registerBase("TheNationalist", 1955);
    public static final Magazine THE_WALL_STREET_POST = registerBase("TheWallStreetPost", 1923, MagazineSubject.BUSINESS);
    public static final Magazine THE_WORLD_WARS = registerBase("TheWorldWars", 1948, MagazineSubject.MILITARY);
    public static final Magazine THRESHER = registerBase("Thresher", 1966, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine TODAY_S_BUILDER = registerBase("TodaysBuilder", 1979);
    public static final Magazine TRAILBEATERS = registerBase("Trailbeaters", 1972, MagazineSubject.OUTDOORS);
    public static final Magazine TRUCKING_HEAVEN = registerBase("TruckingHeaven", 1975, MagazineSubject.CARS);
    public static final Magazine TRUE_CRIME_STORIES = registerBase("TrueCrimeStories", 1954, MagazineSubject.CRIME);
    public static final Magazine TRUE_SERIAL_KILLINGS = registerBase("TrueSerialKillings", 1970, MagazineSubject.HORROR, MagazineSubject.CRIME);
    public static final Magazine TTRPG = registerBase("TTRPG", 1987, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine TWITCHING_WEEKLY = registerBase("TwitchingWeekly", 1944, MagazineSubject.HOBBY);
    public static final Magazine UNPOPULAR_ELECTRONICS = registerBase("UnpopularElectronics", 1954, MagazineSubject.TECH, MagazineSubject.HOBBY);
    public static final Magazine UNSOLD_STORIES = registerBase("UnsoldStories", 1989);
    public static final Magazine UPPER_CRUST = registerBase("UpperCrust", 1987, MagazineSubject.RICH);
    public static final Magazine UPTO_ELEVEN = registerBase("UptoEleven", 1988, MagazineSubject.MUSIC);
    public static final Magazine VETERAN = registerBase("Veteran", 1970, MagazineSubject.MILITARY);
    public static final Magazine VHS_ENTHUSIAST = registerBase("VHSEnthusiast", 1988, MagazineSubject.CINEMA);
    public static final Magazine VITIMANIA_RX = registerBase("VitimaniaRx", 1966, MagazineSubject.HEALTH);
    public static final Magazine WATERCRAFT_AND_BIKES = registerBase("WatercraftandBikes", 1984, MagazineSubject.CARS, MagazineSubject.OUTDOORS);
    public static final Magazine WORLD_GEOGRAPH = registerBase("WorldGeograph", 1888, MagazineSubject.POPULAR);
    public static final Magazine WORLD_MUSIC = registerBase("WorldMusic", 1909, MagazineSubject.MUSIC);
    public static final Magazine WORLD_TRADE = registerBase("WorldTrade", 1920, MagazineSubject.POPULAR, MagazineSubject.BUSINESS);
    public static final Magazine WWC_INSIDER = registerBase("WWCInsider", 1987, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine Y = registerBase("Y", 1990, MagazineSubject.POPULAR);
    public static final Magazine YOUR_INVESTMENT_GUIDE = registerBase("YourInvestmentGuide", 1986, MagazineSubject.BUSINESS);
    private final String translationKey;
    private final int firstYear;
    private final Set<MagazineSubject> subjects;

    private Magazine(String translationKey, int firstYear, Set<MagazineSubject> subjects) {
        this.translationKey = "IGUI_MagazineTitle_" + translationKey;
        this.firstYear = firstYear;
        this.subjects = subjects;
    }

    public static Magazine get(ResourceLocation id) {
        return Registries.MAGAZINE.get(id);
    }

    public static List<Magazine> getMagazineBySubject(InventoryItem item) {
        return item.getMagazineSubjects()
            .stream()
            .flatMap(
                subject -> BY_SUBJECTS.computeIfAbsent(
                        subject, subject2 -> Registries.MAGAZINE.values().stream().filter(m -> m.subjects().contains(subject2)).toList()
                    )
                    .stream()
            )
            .collect(Collectors.toList());
    }

    public String translationKey() {
        return this.translationKey;
    }

    public int firstYear() {
        return this.firstYear;
    }

    public Set<MagazineSubject> subjects() {
        return this.subjects;
    }

    public static Magazine register(String id, int firstYear, MagazineSubject... subjects) {
        return register(false, id, new Magazine(id, firstYear, Set.of(subjects)));
    }

    private static Magazine registerBase(String id, int firstYear, MagazineSubject... subjects) {
        return register(true, id, new Magazine(id, firstYear, Set.of(subjects)));
    }

    private static Magazine register(boolean allowDefaultNamespace, String id, Magazine t) {
        return Registries.MAGAZINE.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }

    static {
        if (Core.IS_DEV) {
            for (Magazine magazine : Registries.MAGAZINE) {
                TranslationKeyValidator.of(magazine.translationKey());
            }
        }
    }
}
