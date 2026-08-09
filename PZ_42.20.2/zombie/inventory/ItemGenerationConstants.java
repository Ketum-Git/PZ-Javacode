// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.List;
import java.util.Map;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.scripting.objects.BookSubject;
import zombie.scripting.objects.Business;
import zombie.scripting.objects.CraftRecipeKey;
import zombie.scripting.objects.Letter;
import zombie.scripting.objects.Newspaper;
import zombie.scripting.objects.OldNewspaper;
import zombie.scripting.objects.PetName;
import zombie.scripting.objects.Photo;
import zombie.scripting.objects.Postcard;
import zombie.util.list.WeightedList;

public class ItemGenerationConstants {
    public static final BookSubject[] BOOK_SUBJECTS = new BookSubject[]{
        BookSubject.GENERAL_FICTION,
        BookSubject.GENERAL_FICTION,
        BookSubject.DIET,
        BookSubject.DIET,
        BookSubject.HORROR,
        BookSubject.HORROR,
        BookSubject.ROMANCE,
        BookSubject.ROMANCE,
        BookSubject.THRILLER,
        BookSubject.THRILLER,
        BookSubject.ADVENTURE_NON_FICTION,
        BookSubject.ART,
        BookSubject.BIBLE,
        BookSubject.BIOGRAPHY,
        BookSubject.BUSINESS,
        BookSubject.CINEMA,
        BookSubject.CLASSIC,
        BookSubject.CONSPIRACY,
        BookSubject.CRIME_FICTION,
        BookSubject.FANTASY,
        BookSubject.FASHION,
        BookSubject.HISTORY,
        BookSubject.LEGAL,
        BookSubject.MUSIC,
        BookSubject.NATURE,
        BookSubject.PHILOSOPHY,
        BookSubject.POLITICS,
        BookSubject.QUACKERY,
        BookSubject.RELATIONSHIP,
        BookSubject.SAD_NON_FICTION,
        BookSubject.SCIENCE,
        BookSubject.SCIFI,
        BookSubject.SELF_HELP,
        BookSubject.SPORTS,
        BookSubject.TRAVEL,
        BookSubject.TRUE_CRIME,
        BookSubject.WESTERN
    };
    public static final BookSubject[] BOOK_SUBJECTS_NON_FICTION = new BookSubject[]{
        BookSubject.ART,
        BookSubject.BUSINESS,
        BookSubject.CINEMA,
        BookSubject.CLASSIC_NONFICTION,
        BookSubject.COMPUTER,
        BookSubject.FASHION,
        BookSubject.GENERAL_REFERENCE,
        BookSubject.HISTORY,
        BookSubject.LEGAL,
        BookSubject.NATURE,
        BookSubject.PHILOSOPHY,
        BookSubject.RELIGION,
        BookSubject.SCIENCE,
        BookSubject.SPORTS,
        BookSubject.TRAVEL
    };
    public static final BookSubject[] BOOK_SUBJECTS_FICTION = new BookSubject[]{
        BookSubject.GENERAL_FICTION,
        BookSubject.GENERAL_FICTION,
        BookSubject.GENERAL_FICTION,
        BookSubject.HORROR,
        BookSubject.HORROR,
        BookSubject.ROMANCE,
        BookSubject.ROMANCE,
        BookSubject.THRILLER,
        BookSubject.THRILLER,
        BookSubject.CLASSIC_FICTION,
        BookSubject.CRIME_FICTION,
        BookSubject.FANTASY,
        BookSubject.SCIFI,
        BookSubject.WESTERN
    };
    public static final BookSubject[] BOOK_SUBJECTS_POOR = new BookSubject[]{
        BookSubject.ADVENTURE_NON_FICTION,
        BookSubject.BIBLE,
        BookSubject.CONSPIRACY,
        BookSubject.CRIME_FICTION,
        BookSubject.HASS,
        BookSubject.QUACKERY,
        BookSubject.QUIGLEY,
        BookSubject.RELATIONSHIP,
        BookSubject.RELIGION,
        BookSubject.ROMANCE,
        BookSubject.SAD_NON_FICTION,
        BookSubject.SELF_HELP,
        BookSubject.SPORTS,
        BookSubject.TRUE_CRIME,
        BookSubject.WESTERN
    };
    public static final BookSubject[] BOOK_SUBJECTS_RICH = new BookSubject[]{
        BookSubject.ART,
        BookSubject.BIBLE,
        BookSubject.BIOGRAPHY,
        BookSubject.BUSINESS,
        BookSubject.CINEMA,
        BookSubject.CLASSIC,
        BookSubject.COMPUTER,
        BookSubject.DIET,
        BookSubject.FASHION,
        BookSubject.GENERAL_FICTION,
        BookSubject.GOLF,
        BookSubject.HISTORY,
        BookSubject.LEGAL,
        BookSubject.NATURE,
        BookSubject.NEW_AGE,
        BookSubject.SCIENCE,
        BookSubject.SEXY,
        BookSubject.THRILLER,
        BookSubject.TRAVEL
    };
    public static final BookSubject[] BOOK_SUBJECTS_SCARY = new BookSubject[]{BookSubject.HORROR, BookSubject.OCCULT, BookSubject.TRUE_CRIME};
    public static final List<Business> STOCK_CERTIFICATE_1 = List.of(
        Business.FOSSOIL,
        Business.SPIFFO_CORP,
        Business.GIGA_MART,
        Business.KIRRUS_INC,
        Business.FRANKLIN_MOTORS,
        Business.GLOBAL_COMPUTER_SOLUTIONS,
        Business.PARASOL_INC,
        Business.TISCONSTRUCTION,
        Business.PREMIUM_TECHNOLOGIES,
        Business.MMM_INC,
        Business.ALGOL_ELECTRONICS,
        Business.FIBROIL,
        Business.SEAHORSE_COFFEE_CORP,
        Business.HAWTHORN_OIL,
        Business.POP_CO,
        Business.CHRYSALIS,
        Business.NIKODA,
        Business.VALU_INSURANCE,
        Business.ZIPPEE,
        Business.PHARMAHUG,
        Business.SPECIFIC_ELECTRIC,
        Business.HALLOWAY_FRAMER,
        Business.REDMOND_REDMOND,
        Business.HAVISHAM_HOTELS,
        Business.AMERICAN_TIRE,
        Business.AMERI_GLOBE_INC,
        Business.MASS_GENFAC_CO,
        Business.FINNEGAN_GROUP,
        Business.PALM_TRAVEL,
        Business.GENERAL_BROADCAST_CORPORATION,
        Business.SCITT_WILKER_FIREARMS
    );
    public static final List<Business> STOCK_CERTIFICATE_2 = List.of(
        Business.MC_COY_LOGGING,
        Business.VALU_TECH,
        Business.EGENEREX,
        Business.UNITED_SHIPPING_LOGISTICS,
        Business.PERFICK_POTATO_CO,
        Business.HERR_FLICK_KNIVES,
        Business.COBBER_METALS,
        Business.BANSHEE_HOLLOWAY,
        Business.BERING_COMPANY,
        Business.YURI_DESIGN,
        Business.NEWCASTLE_PAPERAND_INK,
        Business.BUSAN_TELECOMMUNICATIONS,
        Business.KITTEN_KNIVES,
        Business.BUTTERFLY_MACHINERY,
        Business.WIRKLICHLANGESWORT_AG,
        Business.SANCHEZ_GOLDBERG,
        Business.BEANZ,
        Business.BRUCEY_SOUPS,
        Business.FELLOWS_INC,
        Business.INVISIBLE_SLEDGEHAMMER_CORP,
        Business.PANTHER_MOTORS,
        Business.KILLIAN_FOODSTUFFS,
        Business.GRENNADE_CHEMICALS,
        Business.REALLY_HARD_STEEL,
        Business.CHINESE_PETROLEUM,
        Business.BANKOF_KENTUCKY,
        Business.LOVEHEART_SHIPBUILDING,
        Business.DOUBLE_ENTRY_ACCOUNTING,
        Business.SWIFT_THOMPSON_AEROSPACE,
        Business.FUN_XTREME_INC,
        Business.IMEKAGI,
        Business.WOLFRAM_WAFFEN
    );
    public static final List<PetName> PET_NAMES = List.of(
        PetName.ACE,
        PetName.ACORN,
        PetName.AMERICA,
        PetName.ARCHIE,
        PetName.AVOCADO,
        PetName.BABY,
        PetName.BABY,
        PetName.BACON,
        PetName.BADGER,
        PetName.BAGEL,
        PetName.BAILEY,
        PetName.BAILEY,
        PetName.BANDIT,
        PetName.BANDIT,
        PetName.BANDIT,
        PetName.BEANIE,
        PetName.BEANS,
        PetName.BELLA,
        PetName.BELLE,
        PetName.BEN,
        PetName.BERSERKER,
        PetName.BERT,
        PetName.BESS,
        PetName.BISCUIT,
        PetName.BLONDIE,
        PetName.BLOSSOM,
        PetName.BORIS,
        PetName.BOXER,
        PetName.BRANDY,
        PetName.BRUCE,
        PetName.BRUNO,
        PetName.BUBBLE,
        PetName.BUBBLES,
        PetName.BUCK,
        PetName.BUCKSHOT,
        PetName.BUD,
        PetName.BUDDY,
        PetName.BUDDY,
        PetName.BUDDY,
        PetName.BUDDY,
        PetName.BULLET,
        PetName.BUTTERCUP,
        PetName.CALLY,
        PetName.CHAPLIN,
        PetName.CHARLIE,
        PetName.CHARLIE,
        PetName.CHARLIE,
        PetName.CHARLIE,
        PetName.CHARLIE,
        PetName.CHIEF,
        PetName.CHOCOLATE,
        PetName.CHOPPER,
        PetName.CHRONOS,
        PetName.CLAUDE,
        PetName.CLEAVER,
        PetName.CLOUD,
        PetName.CLOVER,
        PetName.COCO,
        PetName.COFFEE,
        PetName.COOKIE,
        PetName.COOPER,
        PetName.COPPER,
        PetName.CROCKETT,
        PetName.CUPCAKE,
        PetName.DAISY,
        PetName.DAISY,
        PetName.DAKOTA,
        PetName.DOCTOR,
        PetName.DOT,
        PetName.DUDE,
        PetName.DUDE,
        PetName.DUKE,
        PetName.DYLAN,
        PetName.ED,
        PetName.ELLE,
        PetName.FIFI,
        PetName.FLORA,
        PetName.FLUFFY,
        PetName.FLUFFYFOOT,
        PetName.FREDDY,
        PetName.FRAIDY,
        PetName.FREEDOM,
        PetName.FROSTY,
        PetName.FUDGE,
        PetName.FURBERT,
        PetName.GINGER,
        PetName.GOBLIN,
        PetName.GOLDIE,
        PetName.GRAVY,
        PetName.GRIFFIN,
        PetName.GUNNER,
        PetName.HARGRAVE,
        PetName.HARRY,
        PetName.HAZEL,
        PetName.HERB,
        PetName.HOLLY,
        PetName.HONEY,
        PetName.JACK,
        PetName.JACK,
        PetName.JACK,
        PetName.JACQUES,
        PetName.JAY,
        PetName.JENNY,
        PetName.JILL,
        PetName.JILLY,
        PetName.JOKER,
        PetName.JOSH,
        PetName.JOSHIE,
        PetName.JOSS,
        PetName.JULIET,
        PetName.KAI,
        PetName.KATANA,
        PetName.KATJA,
        PetName.KENTUCKY,
        PetName.LADDIE,
        PetName.LADY,
        PetName.LADY,
        PetName.LADY,
        PetName.LARRY,
        PetName.LASER,
        PetName.LAVENDER,
        PetName.LAZ,
        PetName.LESTER,
        PetName.LIBERTY,
        PetName.LILY,
        PetName.LINCOLN,
        PetName.LITTLEGRAY,
        PetName.LORD_PUDDINGTON,
        PetName.LOUIS,
        PetName.LOVELY,
        PetName.LUCY,
        PetName.LULU,
        PetName.LUNA,
        PetName.MACHETE,
        PetName.MADAME,
        PetName.MADAME,
        PetName.MAGNUM,
        PetName.MANGO,
        PetName.MANTELL,
        PetName.MARGE,
        PetName.MARIA,
        PetName.MAULER,
        PetName.MAX,
        PetName.MAX,
        PetName.MAX,
        PetName.MAYO,
        PetName.MILKSHAKE,
        PetName.MISTER,
        PetName.MISTER,
        PetName.MISTER,
        PetName.MISTY,
        PetName.MOLLY,
        PetName.MOLLY,
        PetName.MOON,
        PetName.MOSS,
        PetName.MR_CHEESE,
        PetName.MR_WAFFLES,
        PetName.MUFFIN,
        PetName.NICKI,
        PetName.NIKO,
        PetName.NUGGET,
        PetName.ODIN,
        PetName.ORCA,
        PetName.ORCHID,
        PetName.OSCAR,
        PetName.PANCAKE,
        PetName.PANCHO,
        PetName.PATRIOT,
        PetName.PAULY,
        PetName.PENNY,
        PetName.PEPPER,
        PetName.PIKE,
        PetName.PIPSQUEAK,
        PetName.PISTOL,
        PetName.PLONKIE,
        PetName.POINTER,
        PetName.POLLY,
        PetName.POPPY,
        PetName.PRIMROSE,
        PetName.PRINCE,
        PetName.PRINCE,
        PetName.PRINCESS,
        PetName.PRINCESS,
        PetName.PUDDING,
        PetName.PUMPKIN,
        PetName.PUPPERS,
        PetName.RADA,
        PetName.RAINBOW,
        PetName.RANGER,
        PetName.RASPBERRY,
        PetName.REVOLVER,
        PetName.REX,
        PetName.REX,
        PetName.REX,
        PetName.REX,
        PetName.RIVER,
        PetName.ROCKY,
        PetName.RODNEY,
        PetName.ROMAN,
        PetName.ROMEO,
        PetName.ROOSEVELT,
        PetName.ROSEMARY,
        PetName.ROSIE,
        PetName.ROSIE,
        PetName.ROVER,
        PetName.ROVER,
        PetName.ROVER,
        PetName.ROVER,
        PetName.ROY,
        PetName.RUA,
        PetName.RUBY,
        PetName.RUCA,
        PetName.SAGE,
        PetName.SALLY,
        PetName.SAM,
        PetName.SAMMY,
        PetName.SANDY,
        PetName.SANTA,
        PetName.SCOTT,
        PetName.SCOUT,
        PetName.SHADOW,
        PetName.SHEP,
        PetName.SHEP,
        PetName.SID,
        PetName.SILVER,
        PetName.SIREN,
        PetName.SNIPER,
        PetName.SNOWIE,
        PetName.SOLDIER,
        PetName.SPARKY,
        PetName.SPIFFO,
        PetName.SPIRAL,
        PetName.SPOT,
        PetName.SPOT,
        PetName.SPOT,
        PetName.SQUEAK,
        PetName.STRAWBERRY,
        PetName.SUGAR,
        PetName.SWEETIE,
        PetName.SWEETIE,
        PetName.TAMMY,
        PetName.TED,
        PetName.TEDDY,
        PetName.TERRY,
        PetName.THOR,
        PetName.TIA,
        PetName.TIGER,
        PetName.TINKLER,
        PetName.TOBY,
        PetName.TOFFEE,
        PetName.TOMMY,
        PetName.TRIXIE,
        PetName.TWINKLE,
        PetName.VIC,
        PetName.VIOLET,
        PetName.WALLY,
        PetName.WASHINGTON,
        PetName.WATERFALL,
        PetName.WHIMBLY,
        PetName.WHISKEY,
        PetName.WILLOW,
        PetName.WOODY,
        PetName.YORKIE,
        PetName.ZUKO
    );
    public static final List<Photo> PHOTOS = List.of(
        Photo.A_BABY,
        Photo.A_BABY_LEARNING_TO_WALK,
        Photo.A_BABY_PLAYING_WITH_TOYS,
        Photo.A_BABY_WITH_A_KITTEN,
        Photo.A_BABY_WITH_A_PUPPY,
        Photo.A_BABY_WITH_A_TEDDY_BEAR,
        Photo.A_BAND,
        Photo.A_BASEBALL_GAME,
        Photo.A_BASKETBALL_GAME,
        Photo.A_BEACH_PARTY,
        Photo.A_BEACHSIDE_VACATION,
        Photo.A_BEAR,
        Photo.A_BEAUTIFUL_VISTA,
        Photo.A_BEAUTIFUL_YOUNG_WOMAN,
        Photo.A_BIRD,
        Photo.A_BIRTHDAY_PARTY,
        Photo.A_BOAT,
        Photo.A_BRIDE,
        Photo.A_BRIDE_AND_GROOM_EXCHANGING_RINGS,
        Photo.A_BRIDE_GETTING_READY_FOR_HER_WEDDING,
        Photo.A_BRIDE_WALKING_DOWN_THE_AISLE,
        Photo.A_BRIDE_WITH_A_BOUQUET,
        Photo.A_BUILDING,
        Photo.A_BUSY_STREET,
        Photo.A_CAMERA_SHY_OLD_LADY,
        Photo.A_CAMERA_SHY_OLD_MAN,
        Photo.A_CAR,
        Photo.A_CAT,
        Photo.A_CELEBRATION,
        Photo.A_CEREMONY,
        Photo.A_CHILD,
        Photo.A_CHILD_PLAYING_CHESS,
        Photo.A_CHILD_PLAYING_DRESSUP,
        Photo.A_CHILD_PLAYING_WITH_A_CAT,
        Photo.A_CHILD_PLAYING_WITH_A_DOG,
        Photo.A_CHILD_PLAYING_WITH_A_DOLL,
        Photo.A_CHILD_PLAYING_WITH_A_TOY_CAR,
        Photo.A_CHILD_PLAYING_WITH_A_TOY_ROCKET,
        Photo.A_CHILD_VISITING_SANTA,
        Photo.A_CHILD_WEARING_A_UNIFORM,
        Photo.A_CHILDS_BIRTHDAY_PARTY,
        Photo.A_CHRISTMAS_DINNER,
        Photo.A_CLASSROOM,
        Photo.A_CONCERT,
        Photo.A_COUPLE_CUTTING_THEIR_WEDDING_CAKE,
        Photo.A_COUPLE_DANCING,
        Photo.A_COUPLE_HAVING_A_ROMANTIC_DINNER,
        Photo.A_COUPLE_HAVING_A_ROMANTIC_PICNIC,
        Photo.A_COUPLE_HOLDING_HANDS,
        Photo.A_COUPLE_IN_FRONT_OF_A_HOUSE,
        Photo.A_COUPLE_KISSING,
        Photo.A_COUPLE_LAUGHING,
        Photo.A_COUPLE_RELAXING_ON_A_BEACH,
        Photo.A_COUPLE_WEARING_MATCHING_OUTFITS,
        Photo.A_COUPLE_WITH_A_BABY,
        Photo.A_COW,
        Photo.A_CRAWLING_BABY,
        Photo.A_CROWD,
        Photo.A_CRYING_CHILD,
        Photo.A_CRYING_CHILD_VISITING_SANTA,
        Photo.A_CUTE_ANIMAL,
        Photo.A_DANCE,
        Photo.A_DARKLY_DRESSED_TEENAGER,
        Photo.A_DEAD_ANIMAL,
        Photo.A_DEER,
        Photo.A_DOG,
        Photo.A_DOG_AND_A_CAT,
        Photo.A_FAMILY,
        Photo.A_FAMILY_RELAXING_ON_A_BEACH,
        Photo.A_FAMILY_WEARING_MATCHING_HALLOWEEN_COSTUMES,
        Photo.A_FAMILY_WEARING_MATCHING_OUTFITS,
        Photo.A_FAMILY_WITH_A_CHILD,
        Photo.A_FAMILY_WITH_CHILDREN,
        Photo.A_FAMOUS_PERSON,
        Photo.A_FAMOUS_PLACE,
        Photo.A_FARMER,
        Photo.A_FINISHED_PRODUCT,
        Photo.A_FIRST_BIRTHDAY_PARTY,
        Photo.A_FOOTBALL_GAME,
        Photo.A_FOREIGN_VACATION,
        Photo.A_FOREST,
        Photo.A_FOX,
        Photo.A_FUSSY_CHILD,
        Photo.A_GARDEN,
        Photo.A_GET_TOGETHER,
        Photo.A_GRADUATION,
        Photo.A_GROOM,
        Photo.A_GROOM_GETTING_READY_FOR_HIS_WEDDING,
        Photo.A_GROUP_OF_CYCLISTS,
        Photo.A_GROUP_OF_HORSES,
        Photo.A_GROUP_OF_HUNTERS,
        Photo.A_GROUP_OF_PEOPLE,
        Photo.A_GROUP_OF_PEOPLE_LAUGHING,
        Photo.A_GROUP_OF_STUDENTS,
        Photo.A_GUN,
        Photo.A_HAMSTER,
        Photo.A_HANDSOME_YOUNG_MAN,
        Photo.A_HORSE,
        Photo.A_HORSEDRAWN_CARRIAGE_ARRIVING_AT_A_CHURCH,
        Photo.A_HOUSE,
        Photo.A_HUNTER_POSING_WITH_THEIR_KILL,
        Photo.A_KITTEN,
        Photo.A_LAKE,
        Photo.A_LANDMARK,
        Photo.A_LANDSCAPE,
        Photo.A_LARGE_GROUP_OF_PEOPLE,
        Photo.A_LAUGHING_CHILD,
        Photo.A_LEADER,
        Photo.A_LONELY_LOOKING_CHILD,
        Photo.A_MAN_LAUGHING,
        Photo.A_MAN_ON_A_BICYCLE,
        Photo.A_MAN_WEARING_A_UNIFORM,
        Photo.A_MAN_WITH_A_BABY,
        Photo.A_MEAL,
        Photo.A_MESSY_BABY,
        Photo.A_MOTORCYCLE,
        Photo.A_NEATLY_DRESSED_CHILD,
        Photo.A_NEWLY_MARRIED_COUPLE_DANCING_TOGETHER,
        Photo.A_NEWLY_MARRIED_COUPLE_HOLDING_HANDS,
        Photo.A_NEWLY_MARRIED_COUPLE_KISSING,
        Photo.A_PAIR_OF_SLEEPING_CHILDREN,
        Photo.A_PAIR_OF_TEENAGERS_IN_LOVE,
        Photo.A_PARADE,
        Photo.A_PARTY,
        Photo.A_PERSON,
        Photo.A_PET,
        Photo.A_PHOTO_WITH_A_PERSON_CUT_OUT,
        Photo.A_PICNIC,
        Photo.A_PLANE,
        Photo.A_POLITICAL_MEETING,
        Photo.A_POLITICAL_RALLY,
        Photo.A_POLITICIAN,
        Photo.A_PROM,
        Photo.A_PUPPY,
        Photo.A_RABBIT,
        Photo.A_RACCOON,
        Photo.A_RAINY_DAY,
        Photo.A_RECITAL,
        Photo.A_RELIGIOUS_LEADER,
        Photo.A_RELIGIOUS_SERVICE,
        Photo.A_REUNION,
        Photo.A_RIVER,
        Photo.A_ROAD,
        Photo.A_ROAD_TRIP,
        Photo.A_ROOM,
        Photo.A_SCHOOL_PLAY,
        Photo.A_SECOND_BIRTHDAY_PARTY,
        Photo.A_SHIP,
        Photo.A_SKIING_VACATION,
        Photo.A_SLEEPING_BABY,
        Photo.A_SLEEPING_CAT,
        Photo.A_SLEEPING_CHILD,
        Photo.A_SLEEPING_DOG,
        Photo.A_SLEEPING_GRANDFATHER,
        Photo.A_SLEEPING_GRANDMOTHER,
        Photo.A_SLEEPING_KITTEN,
        Photo.A_SLEEPING_PUPPY,
        Photo.A_SMALL_CHILDS_BIRTHDAY_PARTY,
        Photo.A_SMILING_BABY,
        Photo.A_SMILING_COUPLE,
        Photo.A_SMILING_FAMILY,
        Photo.A_SNAKE,
        Photo.A_SOCCER_GAME,
        Photo.A_SPECIAL_EVENT,
        Photo.A_SPORTS_CAR,
        Photo.A_STREET,
        Photo.A_SUNNY_DAY,
        Photo.A_SUNRISE,
        Photo.A_SUNSET,
        Photo.A_TEENAGE_BAND,
        Photo.A_TEENAGERS_BIRTHDAY_PARTY,
        Photo.A_THIRD_BIRTHDAY_PARTY,
        Photo.A_TRAIN,
        Photo.A_VACATION,
        Photo.A_VACATION_TO_ASIA,
        Photo.A_VACATION_TO_CALIFORNIA,
        Photo.A_VACATION_TO_EUROPE,
        Photo.A_VACATION_TO_FLORIDA,
        Photo.A_VACATION_TO_HAWAII,
        Photo.A_VACATION_TO_LAS_VEGAS,
        Photo.A_VACATION_TO_LONDON,
        Photo.A_VACATION_TO_NEW_YORK,
        Photo.A_VACATION_TO_PARIS,
        Photo.A_VACATION_TO_SOUTH_AMERICA,
        Photo.A_VACATION_TO_THE_GRAND_CANYON,
        Photo.A_VACATION_TO_WASHINGTON_DC,
        Photo.A_VEHICLE,
        Photo.A_VIEW_FROM_A_WINDOW,
        Photo.A_WEDDING,
        Photo.A_WEDDING_CAR_ARRIVING_AT_A_CHURCH,
        Photo.A_WEDDING_PHOTO_WITH_A_PERSON_CUT_OUT,
        Photo.A_WEDDING_RECEPTION,
        Photo.A_WELL_DRESSED_OLD_MAN,
        Photo.A_WILD_ANIMAL,
        Photo.A_WILD_BIRD,
        Photo.A_WOMAN_LAUGHING,
        Photo.A_WOMAN_ON_A_BICYCLE,
        Photo.A_WOMAN_WEARING_A_UNIFORM,
        Photo.A_WOMAN_WITH_A_BABY,
        Photo.A_YOUNG_COUPLE,
        Photo.A_YOUNG_COUPLE_DANCING,
        Photo.A_YOUNG_MAN_WEARING_A_UNIFORM,
        Photo.A_YOUNG_MANS_BIRTHDAY_PARTY,
        Photo.A_YOUNG_WOMAN_WEARING_A_UNIFORM,
        Photo.A_YOUNG_WOMANS_BIRTHDAY_PARTY,
        Photo.AN_AWARD_CEREMONY,
        Photo.AN_AWKWARD_YOUNG_COUPLE,
        Photo.AN_ELDERLY_COUPLE,
        Photo.AN_EMBARASSED_OLD_LADY,
        Photo.AN_EMBARASSED_OLD_MAN,
        Photo.AN_EMBARASSED_TEENAGER,
        Photo.AN_EMPTY_ROOM,
        Photo.AN_EMPTY_STREET,
        Photo.AN_ENGAGEMENT_RING,
        Photo.AN_EVERYDAY_OBJECT,
        Photo.AN_ICE_HOCKEY_GAME,
        Photo.AN_OLD_CAR,
        Photo.AN_OLD_COUPLE_DANCING,
        Photo.AN_OLD_LADY_IN_HER_BEST_HAT,
        Photo.AN_OLD_MAN,
        Photo.AN_OLD_TREE,
        Photo.AN_OLD_WOMAN,
        Photo.AN_OUTING,
        Photo.ANIMALS,
        Photo.ARTWORK,
        Photo.BRIDESMAIDS,
        Photo.CAMPERS,
        Photo.CHILDREN,
        Photo.CHILDREN_IN_A_PLAYGROUND,
        Photo.CHILDREN_PLAYING_BASEBALL,
        Photo.CHILDREN_PLAYING_BASKETBALL,
        Photo.CHILDREN_PLAYING_FOOTBALL,
        Photo.CHILDREN_PLAYING_ON_A_SLIDE,
        Photo.CHILDREN_PLAYING_ON_A_SWING,
        Photo.CHILDREN_PLAYING_SOCCER,
        Photo.CLOUDS,
        Photo.CO_WORKERS,
        Photo.COPRS,
        Photo.DOGS_PLAYING,
        Photo.FARM_ANIMALS,
        Photo.FLOWERS,
        Photo.FOUR_PEOPLE,
        Photo.GARBAGE,
        Photo.GIFTS,
        Photo.GRANDPARENTS_AND_GRANDCHILDREN,
        Photo.GRANDPARENTS_WITH_A_GRANDCHILD,
        Photo.GROOMSMEN,
        Photo.HIKERS,
        Photo.HUNTERS,
        Photo.KITTENS,
        Photo.LAMBS,
        Photo.MOUNTAINS,
        Photo.NATURE,
        Photo.NOTHING_IN_PARTICULAR,
        Photo.PEOPLE,
        Photo.PEOPLE_CELEBRATING_INDEPENDANCE_DAY,
        Photo.PEOPLE_CELEBRATING_SOMETHING,
        Photo.PEOPLE_DANCING,
        Photo.PEOPLE_DANCING_AT_A_WEDDING,
        Photo.PEOPLE_DRINKING_TOGETHER,
        Photo.PEOPLE_EAT_A_BARBECUE,
        Photo.PEOPLE_HANGING_OUT,
        Photo.PEOPLE_HAVING_A_GOOD_TIME,
        Photo.PEOPLE_HUGGING,
        Photo.PEOPLE_IN_FANCY_DRESS,
        Photo.PEOPLE_PARTYING,
        Photo.PEOPLE_PLAYING_A_GAME,
        Photo.PEOPLE_PLAYING_SPORTS,
        Photo.PEOPLE_POSING,
        Photo.PEOPLE_RELAXING_ON_A_BEACH,
        Photo.PEOPLE_RELAXING_ON_VACATION,
        Photo.PEOPLE_STANDING_TOGETHER_AWKWARDLY,
        Photo.PEOPLE_SWIMMING,
        Photo.PEOPLE_WAITING_IN_AN_AIRPORT,
        Photo.PETS,
        Photo.PUPPIES,
        Photo.SANTA_CLAUS,
        Photo.SLEEPING_CHILDREN,
        Photo.SMILING_TRAIN,
        Photo.SOLDIERS,
        Photo.SOMEONE_COOKING,
        Photo.SOMEONE_DOING_CARPENTRY,
        Photo.SOMEONE_DOING_DIY,
        Photo.SOMEONE_DRESSED_AS_A_COWBOY,
        Photo.SOMEONE_DRESSED_AS_A_COWGIRL,
        Photo.SOMEONE_DRESSED_AS_A_GHOST,
        Photo.SOMEONE_DRESSED_AS_A_MONSTER,
        Photo.SOMEONE_DRESSED_AS_A_VAMPIRE,
        Photo.SOMEONE_DRESSED_AS_AN_ELF,
        Photo.SOMEONE_DRIVING,
        Photo.SOMEONE_GIVING_A_WEDDING_SPEECH,
        Photo.SOMEONE_HOLDING_A_BIG_FISH,
        Photo.SOMEONE_HOLDING_A_NEWBORN_BABY,
        Photo.SOMEONE_IN_A_HOSPITAL,
        Photo.SOMEONE_IN_A_STRANGE_OUTFIT,
        Photo.SOMEONE_MAKING_A_RUDE_GESTURE_AT_THE_CAMERA,
        Photo.SOMEONE_MEETING_A_FAMOUS_PERSON,
        Photo.SOMEONE_PLAYING_A_GAME,
        Photo.SOMEONE_PLAYING_MUSIC,
        Photo.SOMEONE_POSING_WITH_A_BIG_TRUCK,
        Photo.SOMEONE_POSING_WITH_A_GUN,
        Photo.SOMEONE_POSING_WITH_A_MOTORCYCLE,
        Photo.SOMEONE_PROPOSING,
        Photo.SOMEONE_READING,
        Photo.SOMEONE_RECEIVING_A_LARGE_CHECK,
        Photo.SOMEONE_RECEIVING_AN_AWARD,
        Photo.SOMEONE_SHOWING_OFF,
        Photo.SOMEONE_SITTING,
        Photo.SOMEONE_SLEEPING,
        Photo.SOMEONE_SMILING,
        Photo.SOMEONE_SPEAKING_ON_A_STAGE,
        Photo.SOMEONE_STANDING,
        Photo.SOMEONE_USING_A_COMPUTER,
        Photo.SOMEONE_WATCHING_TV,
        Photo.SOMEONE_WHO_CLEARLY_HATES_PHOTOS,
        Photo.SOMEONE_WITH_THEIR_EYES_SHUT,
        Photo.SOMEONE_WORKING,
        Photo.SOMEONE_WORKING_ON_A_CAR,
        Photo.SOMETHING_BLURRY,
        Photo.SOMETHING_BORING,
        Photo.SOMETHING_HORRIBLE,
        Photo.SOMETHING_NICE,
        Photo.SOMETHING_OVEREXPOSED,
        Photo.SOMETHING_RUDE,
        Photo.SOMETHING_SPOOKY,
        Photo.SOMETHING_STRANGE,
        Photo.SOMETHING_TOO_FADED_TO_MAKE_OUT,
        Photo.SOMETHING_TOO_STAINED_TO_MAKE_OUT,
        Photo.STRANGE_BLURRY_SHAPES,
        Photo.TEENAGERS,
        Photo.THANKSGIVING,
        Photo.THE_OHIO_RIVER,
        Photo.THE_SKY,
        Photo.THREE_PEOPLE,
        Photo.TRICK_OR_TREATERS,
        Photo.TWO_CHILDREN_PLAYING,
        Photo.TWO_PEOPLE,
        Photo.TWO_PEOPLE_KISSING,
        Photo.TWO_PEOPLE_WEARING_MATCHING_HALLOWEEN_COSTUMES,
        Photo.WEDDING_GUESTS,
        Photo.WELL_DRESSED_OLD_PEOPLE,
        Photo.WELL_DRESSED_PEOPLE,
        Photo.WILD_ANIMALS,
        Photo.WILD_BIRDS
    );
    public static final List<Photo> SECRET_PHOTOS = List.of(
        Photo.SOMETHING_SAUCY,
        Photo.A_PERSON_IN_A_COMPROMISING_POSITION,
        Photo.SOMEONE_RECEIVING_A_BRIEFCASE,
        Photo.SOMEONE_CONSUMING_A_SUSPICIOUS_SUBSTANCE,
        Photo.TWO_PEOPLE_IN_BED,
        Photo.THREE_PEOPLE_IN_BED,
        Photo.SOMEONE_HANDING_OVER_AN_ENVELOPE,
        Photo.SOMEONE_COMMITTING_ILL_DEEDS,
        Photo.SOMEONE_UNCLOTHED,
        Photo.AN_UNCLOTHED_COUPLE,
        Photo.A_GROUP_OF_UNCLOTHED_PEOPLE,
        Photo.A_LICENSE_PLATE,
        Photo.SOME_DUBIOUS_DOCUMENTS,
        Photo.AN_ILLICIT_NATURE,
        Photo.A_SUSPICIOUS_PERSON,
        Photo.A_NERVOUS_PERSON,
        Photo.SOMEONE_TRYING_TO_HIDE,
        Photo.A_WANTED_FUGITIVE,
        Photo.A_PATERNITY_TEST,
        Photo.A_ROMANTIC_NATURE,
        Photo.SOMEONE_BEING_ARRESTED,
        Photo.A_MUGSHOT,
        Photo.A_MISSING_PERSON,
        Photo.CASH,
        Photo.A_PILE_OF_CASH,
        Photo.AN_ARTICLE_ABOUT_A_CRIME,
        Photo.A_GROUP_OF_PEOPLE_IN_BED,
        Photo.SECURITY_FOOTAGE,
        Photo.SOMEONE_RECEIVING_A_PACKAGE,
        Photo.A_GROUP_OF_UNUSUAL_PLANTS,
        Photo.TWO_PEOPLE_SHAKING_HANDS,
        Photo.A_SUSPICIOUS_MEETING,
        Photo.A_PERSON_WITH_CROSSHAIRS_ON_THEIR_FACE,
        Photo.A_DEAD_BODY,
        Photo.A_CAR_CRASH,
        Photo.SOMEONE_FIRING_A_GUN,
        Photo.A_BIRTH_CERTIFICATE,
        Photo.A_DEATH_CERTIFICATE,
        Photo.A_MARRIAGE_CERTIFICATE,
        Photo.A_BUILDING_ON_FIRE,
        Photo.A_PERSON_WHO_IS_TIED_UP,
        Photo.A_GUN,
        Photo.A_SUSPICIOUS_OBJECT,
        Photo.A_SUSPICIOUS_GROUP_OF_PEOPLE,
        Photo.TWO_PEOPLE_KISSING,
        Photo.A_PERSON_WITH_THEIR_FACE_CROSSED_OUT,
        Photo.TWO_MEN_KISSING,
        Photo.TWO_WOMEN_KISSING
    );
    public static final List<Photo> RACY_PHOTOS = List.of(
        Photo.SOMETHING_SAUCY,
        Photo.A_PERSON_IN_A_COMPROMISING_POSITION,
        Photo.TWO_PEOPLE_IN_BED,
        Photo.SOMEONE_UNCLOTHED,
        Photo.AN_UNCLOTHED_COUPLE,
        Photo.A_GROUP_OF_UNCLOTHED_PEOPLE,
        Photo.A_ROMANTIC_NATURE,
        Photo.A_GROUP_OF_PEOPLE_IN_BED,
        Photo.A_PERSON_WHO_IS_TIED_UP,
        Photo.TWO_PEOPLE_KISSING,
        Photo.TWO_MEN_KISSING,
        Photo.TWO_WOMEN_KISSING
    );
    public static final List<Photo> VERY_OLD_PHOTOS = List.of(
        Photo.A_STREET_OF_WOODEN_BUILDINGS,
        Photo.PEOPLE_ON_A_HORSE_DRAWN_BUGGY,
        Photo.A_HORSE_DRAWING_A_PLOW,
        Photo.A_NINETEENTH_CENTURY_FAMILY,
        Photo.A_BABY,
        Photo.A_COUPLE_WITH_A_BABY,
        Photo.A_MAN,
        Photo.A_WOMAN,
        Photo.A_CHILD,
        Photo.A_FAMILY,
        Photo.A_PET,
        Photo.LOUISVILLE,
        Photo.A_TOWN,
        Photo.A_CITY,
        Photo.A_PADDLE_STEAMER_ON_THE_OHIO,
        Photo.PEOPLE_SITTING_TOGETHER,
        Photo.PEOPLE_STANDING_TOGETHER,
        Photo.PEOPLE_FARMING,
        Photo.A_COWBOY,
        Photo.A_NATIVE_AMERICAN,
        Photo.A_GROUP_OF_COWBOYS,
        Photo.A_GROUP_OF_NATIVE_AMERICANS,
        Photo.A_STEAM_TRAIN,
        Photo.A_TRAIN_STATION_IN_THE_OLD_DAYS,
        Photo.A_FRONTIERSMAN,
        Photo.A_FRONTIER_FAMILY,
        Photo.A_CIVIL_WAR_SOLDIER,
        Photo.A_GROUP_OF_CIVIL_WAR_SOLDIERS,
        Photo.A_SAILING_SHIP,
        Photo.A_STEAMSHIP,
        Photo.A_HOMESTEADER_FAMILY,
        Photo.A_HUNTER,
        Photo.A_GROUP_OF_MEN,
        Photo.A_GROUP_OF_WOMEN,
        Photo.A_GROUP_OF_PEOPLE,
        Photo.A_WEDDING,
        Photo.PEOPLE_DANCING_AT_A_WEDDING,
        Photo.PEOPLE_WITH_AN_EARLY_MOTORCAR,
        Photo.A_CIVIL_WAR_BATTLEFIELD,
        Photo.A_FORT,
        Photo.A_MILITARY_CAMP,
        Photo.A_GROUP_OF_PROTESTORS,
        Photo.A_GROUP_OF_SUFFRAGETTES,
        Photo.A_GROUP_OF_ABOLITIONISTS,
        Photo.A_GROUP_OF_PROHIBITIONISTS,
        Photo.A_GROUP_OF_SPIRITUALISTS,
        Photo.A_GROUP_OF_PACIFISTS,
        Photo.A_FIRST_WORLD_WAR_SOLDIER,
        Photo.A_SECOND_WORLD_WAR_SOLDIER,
        Photo.A_KOREAN_WAR_SOLDIER,
        Photo.A_GROUP_OF_FIRST_WORLD_WAR_SOLDIERS,
        Photo.A_GROUP_OF_SECOND_WORLD_WAR_SOLDIERS,
        Photo.A_GROUP_OF_KOREAN_WAR_SOLDIERS,
        Photo.A_BATTLEFIELD_NURSE,
        Photo.A_BATTLEFIELD_MEDIC,
        Photo.A_POLITICAL_MEETING,
        Photo.A_BIG_HOUSE,
        Photo.A_SMALL_HOUSE,
        Photo.A_RELIGIOUS_SERVICE,
        Photo.A_VACATION,
        Photo.A_HOUSE,
        Photo.A_CABIN,
        Photo.A_WELL_BUILT_CABIN,
        Photo.A_RUGGED_CABIN,
        Photo.A_GENTLEMAN,
        Photo.A_LADY,
        Photo.A_LANDSCAPE,
        Photo.PEOPLE_WORKING_IN_A_FACTORY,
        Photo.MINERS,
        Photo.PEOPLE_WORKING,
        Photo.A_POLICE_OFFICER,
        Photo.SOMETHING_TOO_FADED_TO_MAKE_OUT,
        Photo.SOMETHING_TOO_STAINED_TO_MAKE_OUT,
        Photo.A_TYPICAL_WESTERN_SCENE,
        Photo.A_CIRCUS,
        Photo.A_CARNIVAL,
        Photo.A_OUTLAW,
        Photo.A_FAMOUS_OUTLAW,
        Photo.A_SALOON,
        Photo.SOMEONES_ANCESTORS,
        Photo.A_WAGON_TRAIN,
        Photo.A_GRUESOME_SCENE,
        Photo.A_CATTLE_DRIVE,
        Photo.A_SHERIFF,
        Photo.A_BUSINESS_MARKET,
        Photo.A_HOUSE_BEING_BUILT,
        Photo.A_BUILDING_BEING_BUILT,
        Photo.A_LANDMARK_BEING_BUILT,
        Photo.A_HORSE_RACE,
        Photo.A_LARGE_PUBLIC_EVENT,
        Photo.A_FLOOD,
        Photo.A_SPORTS_GAME,
        Photo.IMMIGRANTS,
        Photo.A_CAMP,
        Photo.A_GROUP_OF_SCHOOLCHILDREN,
        Photo.CHILDREN_PLAYING,
        Photo.A_GROUP_OF_CHILDREN,
        Photo.A_BUSY_STREET,
        Photo.PRISONERS,
        Photo.A_FAMOUS_PERSON_FROM_A_LONG_TIME_AGO,
        Photo.SOMEONE_FORGOTTEN,
        Photo.IMMIGRANTS_IN_THEIR_NATIVE_DRESS,
        Photo.A_RELIGIOUS_LEADER,
        Photo.A_PRESIDENT,
        Photo.A_POLITICIAN,
        Photo.AN_OUTDATED_PIECE_OF_TECHNOLOGY,
        Photo.PEOPLE_DRESSED_UP,
        Photo.A_LEADER,
        Photo.A_PARADE,
        Photo.A_COUPLE_HOLDING_HANDS,
        Photo.TWO_WOMEN_KISSING,
        Photo.TWO_MEN_KISSING,
        Photo.A_COUPLE_KISSING,
        Photo.A_HORSEDRAWN_CARRIAGE_ARRIVING_AT_A_CHURCH,
        Photo.A_BRIDE_GETTING_READY_FOR_HER_WEDDING,
        Photo.A_GROOM_GETTING_READY_FOR_HIS_WEDDING,
        Photo.A_SEANCE,
        Photo.A_GHOST,
        Photo.PEOPLE_PLAYING_BASEBALL,
        Photo.PEOPLE_PLAYING_FOOTBALL,
        Photo.A_TEENAGER,
        Photo.A_YOUNG_MAN,
        Photo.A_YOUNG_WOMAN,
        Photo.A_YOUNG_COUPLE,
        Photo.A_GROUP_OF_YOUNG_PEOPLE,
        Photo.A_BEAUTIFUL_YOUNG_WOMAN,
        Photo.A_HANDSOME_YOUNG_MAN,
        Photo.A_MILITARY_OFFICER,
        Photo.SOMEONE_HOLDING_A_VERY_LARGE_VEGETABLE,
        Photo.A_MAN_ON_A_BICYCLE,
        Photo.A_WOMAN_ON_A_BICYCLE,
        Photo.A_MAN_WITH_A_LARGE_MUSTACHE,
        Photo.A_MAN_WITH_A_LONG_BEARD,
        Photo.A_WOMAN_WITH_A_HUGE_HAT,
        Photo.A_GROUP_OF_SCHOOLBOYS,
        Photo.A_GROUP_OF_SCHOOLGIRLS,
        Photo.A_FAMILY_HAVING_CHRISTMAS_DINNER,
        Photo.A_FAMILY_CELEBRATING_THANKSGIVING
    );
    public static final List<OldNewspaper> OLD_NEWSPAPER = List.of(
        OldNewspaper.CHRISTIAN_BULLETIN,
        OldNewspaper.CHRISTIAN_BULLETIN,
        OldNewspaper.KNOX_KNEWS,
        OldNewspaper.KNOX_KNEWS,
        OldNewspaper.KNOX_KNEWS,
        OldNewspaper.KENTUCKY_HERALD,
        OldNewspaper.KENTUCKY_HERALD,
        OldNewspaper.KENTUCKY_HERALD,
        OldNewspaper.NATIONAL_DISPATCH,
        OldNewspaper.NATIONAL_DISPATCH,
        OldNewspaper.NATIONAL_DISPATCH,
        OldNewspaper.MULDRAUGH_MESSENGER,
        OldNewspaper.MULDRAUGH_MESSENGER,
        OldNewspaper.LOUISVILLE_SUN_TIMES,
        OldNewspaper.LOUISVILLE_SUN_TIMES,
        OldNewspaper.LOUISVILLE_SUN_TIMES,
        OldNewspaper.LOUISVILLE_STUDENT,
        OldNewspaper.KNOX_FRONTLINE,
        OldNewspaper.KNOX_FRONTLINE,
        OldNewspaper.BRANDENBURG_BUGLE,
        OldNewspaper.LOUISVILLE_SUN,
        OldNewspaper.THE_CINCINNATI_TIMES,
        OldNewspaper.BOWLING_GREEN_POST,
        OldNewspaper.OWENSBORO_OUTSIDER,
        OldNewspaper.THE_LEXINGTON_VOICE,
        OldNewspaper.PADUCAH_POST,
        OldNewspaper.KENTUCKY_OBSERVER,
        OldNewspaper.NATIONAL_FINANCE,
        OldNewspaper.WASHINGTON_HERALD,
        OldNewspaper.WALL_STREET_INSIDER,
        OldNewspaper.THE_LONDON_POST,
        OldNewspaper.THE_KENTUCKY_DEFENDER,
        OldNewspaper.EVANSVILLE_POST
    );
    public static final Map<String, List<Newspaper>> REGIONAL_PAPERS = Map.ofEntries(
        Map.entry("Louisville", List.of(Newspaper.LOUISVILLE_SUN_TIMES, Newspaper.KENTUCKY_HERALD)),
        Map.entry("MarchRidge", List.of(Newspaper.LOUISVILLE_SUN_TIMES, Newspaper.KNOX_KNEWS)),
        Map.entry("Muldraugh", List.of(Newspaper.KENTUCKY_HERALD)),
        Map.entry("General", List.of(Newspaper.KNOX_KNEWS, Newspaper.KENTUCKY_HERALD))
    );
    public static final WeightedList<Letter> GENERIC_MAIL = new WeightedList<>();
    public static final WeightedList<Letter> LETTER_HANDWRITTEN = new WeightedList<>();
    public static final WeightedList<Postcard> POSTCARD = new WeightedList<>();
    public static final List<CraftRecipeKey> FOOD_RECIPES = List.of(
        CraftRecipeKey.MAKE_BAGUETTE_DOUGH,
        CraftRecipeKey.MAKE_BISCUITS,
        CraftRecipeKey.MAKE_BREAD_DOUGH,
        CraftRecipeKey.MAKE_CABBAGE_ROLLS,
        CraftRecipeKey.MAKE_CAKE_BATTER,
        CraftRecipeKey.MAKE_CHOCOLATE_CHIP_COOKIE_DOUGH,
        CraftRecipeKey.MAKE_CHOCOLATE_COOKIE_DOUGH,
        CraftRecipeKey.MAKE_FRIED_ONION_RINGS,
        CraftRecipeKey.MAKE_FRIED_SHRIMP,
        CraftRecipeKey.MAKE_GUACAMOLE,
        CraftRecipeKey.MAKE_JAR,
        CraftRecipeKey.MAKE_MAKI,
        CraftRecipeKey.MAKE_OATMEAL_COOKIE_DOUGH,
        CraftRecipeKey.MAKE_ONIGIRI,
        CraftRecipeKey.MAKE_PIE_DOUGH,
        CraftRecipeKey.MAKE_PIZZA,
        CraftRecipeKey.MAKE_SHORTBREAD_COOKIE_DOUGH,
        CraftRecipeKey.MAKE_SUGAR_COOKIE_DOUGH,
        CraftRecipeKey.MAKE_SUSHI,
        CraftRecipeKey.PREPARE_MUFFINS
    );
    public static final List<CraftRecipeKey> EXPLOSIVE_SCHEMATICS = List.of(
        CraftRecipeKey.ADD_CRAFTED_TRIGGER_TO_BOMB,
        CraftRecipeKey.ADD_MOTION_SENSOR_V1TO_BOMB,
        CraftRecipeKey.ADD_MOTION_SENSOR_V2TO_BOMB,
        CraftRecipeKey.ADD_MOTION_SENSOR_V3TO_BOMB,
        CraftRecipeKey.ADD_TIMER_TO_BOMB,
        CraftRecipeKey.MAKE_AEROSOL_BOMB,
        CraftRecipeKey.MAKE_FLAME_BOMB,
        CraftRecipeKey.MAKE_NOISE_MAKER,
        CraftRecipeKey.MAKE_PIPE_BOMB,
        CraftRecipeKey.MAKE_REMOTE_CONTROLLER_V1,
        CraftRecipeKey.MAKE_REMOTE_CONTROLLER_V2,
        CraftRecipeKey.MAKE_REMOTE_CONTROLLER_V3,
        CraftRecipeKey.MAKE_REMOTE_TRIGGER,
        CraftRecipeKey.MAKE_SMOKE_BOMB,
        CraftRecipeKey.MAKE_TIMER
    );
    public static final List<CraftRecipeKey> COOKWARE_SCHEMATIC = List.of(
        CraftRecipeKey.FORGE_BAKING_PAN,
        CraftRecipeKey.FORGE_BAKING_TRAY,
        CraftRecipeKey.FORGE_COOKING_POT,
        CraftRecipeKey.FORGE_FRYING_PAN,
        CraftRecipeKey.FORGE_ROASTING_PAN
    );
    public static final List<CraftRecipeKey> BLACKSMITH_TOOLS_SCHEMATICS = List.of(
        CraftRecipeKey.FORGE_BALL_PEEN_HAMMER_HEAD,
        CraftRecipeKey.FORGE_BUCKET,
        CraftRecipeKey.FORGE_BUCKLE,
        CraftRecipeKey.FORGE_CARPENTRY_CHISEL,
        CraftRecipeKey.FORGE_CLAWHAMMER_HEAD,
        CraftRecipeKey.FORGE_CLUB_HAMMER_HEAD,
        CraftRecipeKey.FORGE_CORKSCREW,
        CraftRecipeKey.FORGE_CROWBAR,
        CraftRecipeKey.FORGE_DOOR_KNOB,
        CraftRecipeKey.FORGE_DRAW_PLATE,
        CraftRecipeKey.FORGE_DRILL,
        CraftRecipeKey.FORGE_FILE,
        CraftRecipeKey.FORGE_FINE_SCISSORS,
        CraftRecipeKey.FORGE_FIREPLACE_POKER,
        CraftRecipeKey.FORGE_FISHING_HOOKS,
        CraftRecipeKey.FORGE_FLESHING_TOOL,
        CraftRecipeKey.FORGE_FORCEPS,
        CraftRecipeKey.FORGE_GARDEN_HOE_HEAD,
        CraftRecipeKey.FORGE_GARDENING_TROWEL,
        CraftRecipeKey.FORGE_HAND_AXE_HEAD,
        CraftRecipeKey.FORGE_HAND_SCYTHE_HEAD,
        CraftRecipeKey.FORGE_HEADING_TOOL,
        CraftRecipeKey.FORGE_HEADING_TOOL,
        CraftRecipeKey.FORGE_HINGE,
        CraftRecipeKey.FORGE_JAR_LID,
        CraftRecipeKey.FORGE_LANTERN,
        CraftRecipeKey.FORGE_MASONS_CHISEL,
        CraftRecipeKey.FORGE_MASONS_TROWEL,
        CraftRecipeKey.FORGE_METALWORKING_CHISEL,
        CraftRecipeKey.FORGE_METALWORKING_PLIERS,
        CraftRecipeKey.FORGE_METALWORKING_PUNCH,
        CraftRecipeKey.FORGE_NAILS,
        CraftRecipeKey.FORGE_NEEDLE,
        CraftRecipeKey.FORGE_OLD_AXE_HEAD,
        CraftRecipeKey.FORGE_PICK_AXE_HEAD,
        CraftRecipeKey.FORGE_SAW,
        CraftRecipeKey.FORGE_SCISSORS,
        CraftRecipeKey.FORGE_SCYTHE_HEAD,
        CraftRecipeKey.FORGE_SHEEP_SHEARS,
        CraftRecipeKey.FORGE_SLEDGEHAMMER_HEAD,
        CraftRecipeKey.FORGE_SMITHING_HAMMER_HEAD,
        CraftRecipeKey.FORGE_SPADE_HEAD,
        CraftRecipeKey.FORGE_STRAIGHT_RAZOR,
        CraftRecipeKey.FORGE_TONGS,
        CraftRecipeKey.FORGE_TWEEZERS,
        CraftRecipeKey.FORGE_WOOD_AXE_HEAD,
        CraftRecipeKey.FORGE_WRENCH,
        CraftRecipeKey.MAKE_CRUDE_WHETSTONE
    );
    public static final List<CraftRecipeKey> SURVIVAL_SCHEMATICS = List.of(
        CraftRecipeKey.ASSEMBLE_ADVANCED_FRAMEPACK,
        CraftRecipeKey.ASSEMBLE_LARGE_FRAMEPACK,
        CraftRecipeKey.ASSEMBLE_SMALL_FRAMEPACK,
        CraftRecipeKey.BIND_SPEAR,
        CraftRecipeKey.CARVE_BUCKET,
        CraftRecipeKey.CARVE_FLESHING_TOOL,
        CraftRecipeKey.CARVE_KNITTING_NEEDLES,
        CraftRecipeKey.CARVE_WHISTLE,
        CraftRecipeKey.CARVE_WOODEN_SPADE,
        CraftRecipeKey.FIRE_HARDEN_SPEAR,
        CraftRecipeKey.MAKE_BONE_AWL,
        CraftRecipeKey.MAKE_BONE_FISHING_HOOK,
        CraftRecipeKey.MAKE_BONE_SEWING_NEEDLE,
        CraftRecipeKey.MAKE_CAGE_TRAP,
        CraftRecipeKey.MAKE_COMFREY_POULTICE,
        CraftRecipeKey.MAKE_CRAFTED_GAS_MASK_FILTER,
        CraftRecipeKey.MAKE_CRUDE_WHETSTONE,
        CraftRecipeKey.MAKE_FISHING_ROD,
        CraftRecipeKey.MAKE_IMPROVISED_FLASHLIGHT,
        CraftRecipeKey.MAKE_IMPROVISED_GAS_MASK,
        CraftRecipeKey.MAKE_IMPROVISED_LANTERN,
        CraftRecipeKey.MAKE_IMPROVISED_LIGHTER,
        CraftRecipeKey.MAKE_LARGE_STONE_AXE_HEAD,
        CraftRecipeKey.MAKE_PLANTAIN_POULTICE,
        CraftRecipeKey.MAKE_SCREWDRIVER,
        CraftRecipeKey.MAKE_SNARE_TRAP,
        CraftRecipeKey.MAKE_STICK_TRAP,
        CraftRecipeKey.MAKE_STONE_AWL,
        CraftRecipeKey.MAKE_STONE_BLADE,
        CraftRecipeKey.MAKE_STONE_BLADE_SAW,
        CraftRecipeKey.MAKE_STONE_BLADE_SCYTHE,
        CraftRecipeKey.MAKE_STONE_CHISEL,
        CraftRecipeKey.MAKE_STONE_DRILL,
        CraftRecipeKey.MAKE_STONE_MAUL_HEAD,
        CraftRecipeKey.MAKE_TARP_CHEST_RIG,
        CraftRecipeKey.MAKE_TRAP_BOX,
        CraftRecipeKey.MAKE_WILD_GARLIC_POULTICE,
        CraftRecipeKey.MAKE_WOODEN_BOX_TRAP,
        CraftRecipeKey.RECHARGE_FILTERS,
        CraftRecipeKey.SEW_BANDOLIER,
        CraftRecipeKey.SEW_BELT,
        CraftRecipeKey.SEW_CRUDE_LEATHER_BACKPACK,
        CraftRecipeKey.SEW_FURRED_HIDE_COAT,
        CraftRecipeKey.SEW_FURRED_HIDE_JACKET,
        CraftRecipeKey.SEW_HIDE_BOOTS,
        CraftRecipeKey.SEW_HIDE_COAT,
        CraftRecipeKey.SEW_HIDE_FANNY_BAG,
        CraftRecipeKey.SEW_HIDE_HOODIE,
        CraftRecipeKey.SEW_HIDE_JACKET,
        CraftRecipeKey.SEW_HIDE_PANTS,
        CraftRecipeKey.SEW_HIDE_SLEEPING_BAG,
        CraftRecipeKey.SEW_HOLSTER,
        CraftRecipeKey.SEW_HOLSTER_DOUBLE,
        CraftRecipeKey.SEW_LEATHER_GLOVES,
        CraftRecipeKey.SEW_LEATHER_TOOL_ROLL,
        CraftRecipeKey.SEW_LEATHER_WATER_BAG,
        CraftRecipeKey.SEW_SANDALS,
        CraftRecipeKey.SEW_SHEEPSKIN_PANTS,
        CraftRecipeKey.SEW_SHEEPSKIN_VEST,
        CraftRecipeKey.SHARPEN_BONE
    );
    public static final List<CraftRecipeKey> MELEE_WEAPON_SCHEMATICS = List.of(
        CraftRecipeKey.BARBED_WIRE_WEAPON,
        CraftRecipeKey.BONE_SPIKE_WEAPON,
        CraftRecipeKey.CAN_REINFORCE_WEAPON,
        CraftRecipeKey.CARVE_BAT,
        CraftRecipeKey.CARVE_SHORT_BAT,
        CraftRecipeKey.FORGE_CRUDE_BLADE,
        CraftRecipeKey.FORGE_CRUDE_SHORTSWORD_BLADE,
        CraftRecipeKey.FORGE_CRUDE_SWORD_BLADE,
        CraftRecipeKey.FORGE_HUNTING_KNIFE_BLADE,
        CraftRecipeKey.FORGE_LARGE_KNIFE_BLADE,
        CraftRecipeKey.FORGE_LONG_CRUDE_BLADE,
        CraftRecipeKey.FORGE_MACE_HEAD,
        CraftRecipeKey.FORGE_MACHETE_BLADE,
        CraftRecipeKey.FORGE_MACHETE_BLADE,
        CraftRecipeKey.FORGE_SCRAP_SHORTSWORD,
        CraftRecipeKey.FORGE_SHORT_SWORD_BLADE,
        CraftRecipeKey.FORGE_SMALL_KNIFE,
        CraftRecipeKey.FORGE_SWORD_BLADE,
        CraftRecipeKey.MAKE_BONE_CLUB,
        CraftRecipeKey.MAKE_BONE_HATCHET_HEAD,
        CraftRecipeKey.MAKE_BRAKE_WEAPON,
        CraftRecipeKey.MAKE_BUCKET_MAUL,
        CraftRecipeKey.MAKE_GARDEN_FORK_HEAD_WEAPON,
        CraftRecipeKey.MAKE_GLASS_SHIV,
        CraftRecipeKey.MAKE_JAWBONE_AXE,
        CraftRecipeKey.MAKE_JAWBONE_CLUB,
        CraftRecipeKey.MAKE_JAWBONE_MORNINGSTAR,
        CraftRecipeKey.MAKE_KETTLE_MAUL,
        CraftRecipeKey.MAKE_LARGE_STONE_AXE_HEAD,
        CraftRecipeKey.MAKE_RAKE_HEAD_WEAPON,
        CraftRecipeKey.MAKE_SAW_PLANK,
        CraftRecipeKey.MAKE_SAWBLADE_WEAPON,
        CraftRecipeKey.MAKE_SHIV,
        CraftRecipeKey.MAKE_SPADE_HEAD_CUDGEL,
        CraftRecipeKey.MAKE_SPIKED_CLUB,
        CraftRecipeKey.MAKE_STONE_MAUL_HEAD,
        CraftRecipeKey.MAKE_TOOTHBRUSH_SHIV,
        CraftRecipeKey.SHEET_METAL_WEAPON
    );
    public static final List<CraftRecipeKey> ARMOR_SCHEMATICS = List.of(
        CraftRecipeKey.ASSEMBLE_ARTICULATED_SHIN_ARMOR,
        CraftRecipeKey.ASSEMBLE_ARTICULATED_THIGH_ARMOR,
        CraftRecipeKey.ASSEMBLE_FOREARM_ARMOR,
        CraftRecipeKey.ASSEMBLE_FULL_METAL_FOREARM_ARMOR,
        CraftRecipeKey.ASSEMBLE_SHIN_ARMOR,
        CraftRecipeKey.ASSEMBLE_SIMPLE_SHOULDER_ARMOR,
        CraftRecipeKey.ASSEMBLE_THIGH_ARMOR,
        CraftRecipeKey.FORGE_ARMORED_GLOVES,
        CraftRecipeKey.FORGE_BODY_ARMOR,
        CraftRecipeKey.FORGE_COAT_OF_PLATES,
        CraftRecipeKey.FORGE_CODPIECE,
        CraftRecipeKey.FORGE_GORGET,
        CraftRecipeKey.FORGE_METAL_HELMET,
        CraftRecipeKey.FORGE_METAL_MASK,
        CraftRecipeKey.MAKE_BODY_MAGAZINE_ARMOR,
        CraftRecipeKey.MAKE_BONE_BODY_ARMOR,
        CraftRecipeKey.MAKE_BONE_CHOKER,
        CraftRecipeKey.MAKE_BONE_FOREARM_ARMOR,
        CraftRecipeKey.MAKE_BONE_SHIN_ARMOR,
        CraftRecipeKey.MAKE_BONE_SHOULDER_ARMOR,
        CraftRecipeKey.MAKE_BONE_THIGH_ARMOR,
        CraftRecipeKey.MAKE_CRAFTED_GAS_MASK_FILTER,
        CraftRecipeKey.MAKE_FOREARM_BULLETPROOF_VEST_ARMOR,
        CraftRecipeKey.MAKE_FOREARM_MAGAZINE_ARMOR,
        CraftRecipeKey.MAKE_IMPROVISED_GAS_MASK,
        CraftRecipeKey.MAKE_LARGE_BONE_BEAD,
        CraftRecipeKey.MAKE_SCRAP_METAL_BODY_ARMOR,
        CraftRecipeKey.MAKE_SCRAP_METAL_HELMET,
        CraftRecipeKey.MAKE_SCRAP_METAL_SHOULDER_ARMOR,
        CraftRecipeKey.MAKE_SCRAP_METAL_THIGH_ARMOR,
        CraftRecipeKey.MAKE_SHIN_BULLETPROOF_VEST_ARMOR,
        CraftRecipeKey.MAKE_THIGH_BULLETPROOF_VEST_ARMOR,
        CraftRecipeKey.MAKE_TIRE_BODY_ARMOR,
        CraftRecipeKey.MAKE_TIRE_FOREARM_ARMOR,
        CraftRecipeKey.MAKE_TIRE_SHIN_ARMOR,
        CraftRecipeKey.MAKE_TIRE_SHOULDER_ARMOR_LEFT,
        CraftRecipeKey.MAKE_TIRE_SHOULDER_ARMOR_RIGHT,
        CraftRecipeKey.MAKE_TIRE_THIGH_ARMOR,
        CraftRecipeKey.MAKE_WOOD_BODY_ARMOR,
        CraftRecipeKey.MAKE_WOOD_FOREARM_ARMOR,
        CraftRecipeKey.MAKE_WOOD_SHIN_ARMOR,
        CraftRecipeKey.MAKE_WOOD_SHOULDER_ARMOR,
        CraftRecipeKey.MAKE_WOOD_THIGH_ARMOR,
        CraftRecipeKey.RECHARGE_FILTERS,
        CraftRecipeKey.SEW_ELBOW_PADS,
        CraftRecipeKey.SEW_KNEE_PADS,
        CraftRecipeKey.SEW_LEATHER_CODPIECE,
        CraftRecipeKey.SEW_LEATHER_GAITER,
        CraftRecipeKey.SEW_LEATHER_GORGET,
        CraftRecipeKey.SEW_LEATHER_PANTS,
        CraftRecipeKey.SEW_LEATHER_VAMBRACE,
        CraftRecipeKey.SPIKE_ARMOR_WELDING,
        CraftRecipeKey.SPIKE_PADDING
    );
    public static final List<CraftRecipeKey> SEWING_PATTERNS = List.of(
        CraftRecipeKey.KNIT_BALACLAVA_FACE,
        CraftRecipeKey.KNIT_BALACLAVA_FULL,
        CraftRecipeKey.KNIT_BEANY,
        CraftRecipeKey.KNIT_LEGWARMERS,
        CraftRecipeKey.KNIT_SCARF,
        CraftRecipeKey.KNIT_SOCKS,
        CraftRecipeKey.KNIT_SWEATER_VEST,
        CraftRecipeKey.KNIT_WOOLY_HAT,
        CraftRecipeKey.SEW_DRESS_KNEES,
        CraftRecipeKey.SEW_DRESS_LONG,
        CraftRecipeKey.SEW_LONGJOHNS,
        CraftRecipeKey.SEW_LONGJOHNS_BOTTOM,
        CraftRecipeKey.SEW_SHIRT,
        CraftRecipeKey.SEW_SHIRT_SLEEVELESS,
        CraftRecipeKey.SEW_SKIRT_KNEES,
        CraftRecipeKey.SEW_SKIRT_LONG,
        CraftRecipeKey.SEW_TROUSERS
    );
    public static final WeightedList<Color> COLOR_FABRIC_ROLL = new WeightedList<>();
    public static final WeightedList<Color> COLOR_LIPSTICK = new WeightedList<>();
    public static final WeightedList<Color> COLOR_SPRAY_PAINT = new WeightedList<>();
    public static final WeightedList<Color> COLOR_TOY_PLANE = new WeightedList<>();

    static {
        GENERIC_MAIL.add(Letter.ACCEPTANCE_LETTER, 10);
        GENERIC_MAIL.add(Letter.APPLICATION_LETTER, 10);
        GENERIC_MAIL.add(Letter.BANK_LETTER, 10);
        GENERIC_MAIL.add(Letter.BILL, 10);
        GENERIC_MAIL.add(Letter.BUSINESS_LETTER, 10);
        GENERIC_MAIL.add(Letter.CHARITY_LETTER, 10);
        GENERIC_MAIL.add(Letter.CONDOLENCE_LETTER, 10);
        GENERIC_MAIL.add(Letter.EMPLOYMENT_LETTER, 10);
        GENERIC_MAIL.add(Letter.FRIENDLY_LETTER, 10);
        GENERIC_MAIL.add(Letter.GOVERNMENT_LETTER, 10);
        GENERIC_MAIL.add(Letter.INVITATION_LETTER, 10);
        GENERIC_MAIL.add(Letter.LEGAL_LETTER, 10);
        GENERIC_MAIL.add(Letter.OFFICIAL_LETTER, 10);
        GENERIC_MAIL.add(Letter.OVERDUE_BILL, 10);
        GENERIC_MAIL.add(Letter.REJECTION_LETTER, 10);
        GENERIC_MAIL.add(Letter.RESIGNATION_LETTER, 10);
        GENERIC_MAIL.add(Letter.RUDE_LETTER, 10);
        GENERIC_MAIL.add(Letter.SAD_LETTER, 10);
        GENERIC_MAIL.add(Letter.SCAM_LETTER, 10);
        GENERIC_MAIL.add(Letter.THANK_YOU_LETTER, 10);
        GENERIC_MAIL.add(Letter.THREATENING_LETTER, 10);
        GENERIC_MAIL.add(Letter.LETTER, 80);
        LETTER_HANDWRITTEN.add(Letter.CHILDS_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.CONDOLENCE_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.FRIENDLY_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.INVITATION_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.RESIGNATION_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.ROMANTIC_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.RUDE_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.SAD_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.SCAM_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.THANK_YOU_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.THREATENING_LETTER, 10);
        LETTER_HANDWRITTEN.add(Letter.LETTER, 100);
        POSTCARD.add(Postcard.ALASKA, 10);
        POSTCARD.add(Postcard.ALCATRAZ, 10);
        POSTCARD.add(Postcard.AMSTERDAM, 10);
        POSTCARD.add(Postcard.AN_AFRICAN_SAFARI, 30);
        POSTCARD.add(Postcard.ANTARCTICA, 10);
        POSTCARD.add(Postcard.ARGENTINA, 10);
        POSTCARD.add(Postcard.ASIA, 10);
        POSTCARD.add(Postcard.ASPEN, 10);
        POSTCARD.add(Postcard.ATHENS, 10);
        POSTCARD.add(Postcard.ATLANTA, 10);
        POSTCARD.add(Postcard.AUSTRALIA, 20);
        POSTCARD.add(Postcard.AUSTRIA, 10);
        POSTCARD.add(Postcard.AYERS_ROCK, 10);
        POSTCARD.add(Postcard.BALI, 10);
        POSTCARD.add(Postcard.BARCELONA, 10);
        POSTCARD.add(Postcard.BATON_ROUGE, 10);
        POSTCARD.add(Postcard.BEACON_HILL, 10);
        POSTCARD.add(Postcard.BEIJING, 10);
        POSTCARD.add(Postcard.BERLIN, 10);
        POSTCARD.add(Postcard.BERMUDA, 10);
        POSTCARD.add(Postcard.BIG_SUR, 10);
        POSTCARD.add(Postcard.BOSTON, 10);
        POSTCARD.add(Postcard.BRAZIL, 10);
        POSTCARD.add(Postcard.BROOKLYN, 10);
        POSTCARD.add(Postcard.BUCKINGHAM_PALACE, 10);
        POSTCARD.add(Postcard.BUENOS_AIRES, 10);
        POSTCARD.add(Postcard.CAIRO, 10);
        POSTCARD.add(Postcard.CALCUTTA, 10);
        POSTCARD.add(Postcard.CALIFORNIA, 10);
        POSTCARD.add(Postcard.CAMBODIA, 10);
        POSTCARD.add(Postcard.CANADA, 20);
        POSTCARD.add(Postcard.CAPE_CANAVERAL, 10);
        POSTCARD.add(Postcard.CAPE_COD, 10);
        POSTCARD.add(Postcard.CARNEGIE_HALL, 20);
        POSTCARD.add(Postcard.CHICAGO, 20);
        POSTCARD.add(Postcard.CHINA, 10);
        POSTCARD.add(Postcard.CLEVELAND, 10);
        POSTCARD.add(Postcard.COLONIAL_WILLIAMSBURG, 10);
        POSTCARD.add(Postcard.COLORADO, 10);
        POSTCARD.add(Postcard.CONEY_ISLAND, 10);
        POSTCARD.add(Postcard.COPENHAGEN, 10);
        POSTCARD.add(Postcard.COSTA_RICA, 10);
        POSTCARD.add(Postcard.CUMBERLAND_FALLS, 20);
        POSTCARD.add(Postcard.CYPRUS, 10);
        POSTCARD.add(Postcard.CZECH_REPUBLIC, 10);
        POSTCARD.add(Postcard.CZECHOSLOVAKIA, 10);
        POSTCARD.add(Postcard.DALLAS, 10);
        POSTCARD.add(Postcard.DEATH_VALLEY, 10);
        POSTCARD.add(Postcard.DUBLIN, 10);
        POSTCARD.add(Postcard.EASTER_ISLAND, 10);
        POSTCARD.add(Postcard.EDINBURGH, 10);
        POSTCARD.add(Postcard.EGYPT, 10);
        POSTCARD.add(Postcard.EL_CAPITAN, 10);
        POSTCARD.add(Postcard.ELLIS_ISLAND, 10);
        POSTCARD.add(Postcard.ENGLAND, 20);
        POSTCARD.add(Postcard.EUROPE, 10);
        POSTCARD.add(Postcard.FANEUIL_HALL, 10);
        POSTCARD.add(Postcard.FINLAND, 10);
        POSTCARD.add(Postcard.FLORENCE, 10);
        POSTCARD.add(Postcard.FLORIDA, 20);
        POSTCARD.add(Postcard.FORT_INDEPENDENCE, 10);
        POSTCARD.add(Postcard.FORT_SUMTER, 10);
        POSTCARD.add(Postcard.FRANCE, 10);
        POSTCARD.add(Postcard.GALLERIA_DELL_ACCADEMIA, 10);
        POSTCARD.add(Postcard.GERMANY, 10);
        POSTCARD.add(Postcard.GETTYSBURG, 10);
        POSTCARD.add(Postcard.GIBRALTAR, 10);
        POSTCARD.add(Postcard.GLEN_CANYON, 10);
        POSTCARD.add(Postcard.GOLDEN_GATE_PARK, 10);
        POSTCARD.add(Postcard.GREAT_BARRIER_REEF, 10);
        POSTCARD.add(Postcard.GREECE, 10);
        POSTCARD.add(Postcard.GRIFFITH_OBSERVATORY, 10);
        POSTCARD.add(Postcard.HA_LONG_BAY, 10);
        POSTCARD.add(Postcard.HAITI, 10);
        POSTCARD.add(Postcard.HANOI, 10);
        POSTCARD.add(Postcard.HAWAII, 30);
        POSTCARD.add(Postcard.HO_CHI_MINH_CITY, 10);
        POSTCARD.add(Postcard.HOLLYWOOD, 30);
        POSTCARD.add(Postcard.HONG_KONG, 10);
        POSTCARD.add(Postcard.HONOLULU, 10);
        POSTCARD.add(Postcard.ICELAND, 10);
        POSTCARD.add(Postcard.ILLINOIS, 20);
        POSTCARD.add(Postcard.INDIA, 10);
        POSTCARD.add(Postcard.INDIANA, 20);
        POSTCARD.add(Postcard.INDONESIA, 10);
        POSTCARD.add(Postcard.IRELAND, 20);
        POSTCARD.add(Postcard.ISTANBUL, 10);
        POSTCARD.add(Postcard.ITALY, 30);
        POSTCARD.add(Postcard.JAMAICA, 10);
        POSTCARD.add(Postcard.JAMESTOWN, 10);
        POSTCARD.add(Postcard.JAPAN, 10);
        POSTCARD.add(Postcard.JERUSALEM, 10);
        POSTCARD.add(Postcard.JOSHUA_TREE_NATIONAL_PARK, 10);
        POSTCARD.add(Postcard.KENYA, 10);
        POSTCARD.add(Postcard.KEY_WEST, 10);
        POSTCARD.add(Postcard.KILLARNEY, 10);
        POSTCARD.add(Postcard.KINGSMOUTH_ISLAND, 30);
        POSTCARD.add(Postcard.LAKE_BAIKAL, 10);
        POSTCARD.add(Postcard.LAKE_COMO, 10);
        POSTCARD.add(Postcard.LAKE_GARDA, 10);
        POSTCARD.add(Postcard.LAKE_LUCERNE, 10);
        POSTCARD.add(Postcard.LAKE_MEAD, 10);
        POSTCARD.add(Postcard.LAKE_PLACID, 10);
        POSTCARD.add(Postcard.LAKE_SUPERIOR, 10);
        POSTCARD.add(Postcard.LAKE_TAHOE, 10);
        POSTCARD.add(Postcard.LAKE_TITICACA, 10);
        POSTCARD.add(Postcard.LAND_BETWEEN_THE_LAKES, 30);
        POSTCARD.add(Postcard.LAPLAND, 10);
        POSTCARD.add(Postcard.LAS_VEGAS, 50);
        POSTCARD.add(Postcard.LISBON, 10);
        POSTCARD.add(Postcard.LOCH_NESS, 10);
        POSTCARD.add(Postcard.LONDON, 20);
        POSTCARD.add(Postcard.LOS_ANGELES, 20);
        POSTCARD.add(Postcard.LOUISIANA, 10);
        POSTCARD.add(Postcard.MACHU_PICCHU, 10);
        POSTCARD.add(Postcard.MADRID, 10);
        POSTCARD.add(Postcard.MALAYSIA, 10);
        POSTCARD.add(Postcard.MALDIVES, 10);
        POSTCARD.add(Postcard.MALIBU, 10);
        POSTCARD.add(Postcard.MALTA, 10);
        POSTCARD.add(Postcard.MANHATTAN, 10);
        POSTCARD.add(Postcard.MARENGO_CAVE, 10);
        POSTCARD.add(Postcard.MARTHAS_VINEYARD, 10);
        POSTCARD.add(Postcard.MASSACHUSETTS, 10);
        POSTCARD.add(Postcard.MAUI, 10);
        POSTCARD.add(Postcard.MEMPHIS, 20);
        POSTCARD.add(Postcard.MESA_VERDE, 10);
        POSTCARD.add(Postcard.MEXICO, 30);
        POSTCARD.add(Postcard.MIAMI, 20);
        POSTCARD.add(Postcard.MIAMI_BEACH, 20);
        POSTCARD.add(Postcard.MILWAUKEE, 10);
        POSTCARD.add(Postcard.MINNESOTA, 10);
        POSTCARD.add(Postcard.MISSOURI, 20);
        POSTCARD.add(Postcard.MONACO, 10);
        POSTCARD.add(Postcard.MONGOLIA, 10);
        POSTCARD.add(Postcard.MONROE_LAKE, 10);
        POSTCARD.add(Postcard.MONROEVILLE, 10);
        POSTCARD.add(Postcard.MONT_BLANC, 10);
        POSTCARD.add(Postcard.MONT_SANT_MICHEL, 10);
        POSTCARD.add(Postcard.MONTANA, 10);
        POSTCARD.add(Postcard.MONTICELLO, 10);
        POSTCARD.add(Postcard.MONTREAL, 10);
        POSTCARD.add(Postcard.MONTSERRAT, 10);
        POSTCARD.add(Postcard.MONUMENT_VALLEY, 20);
        POSTCARD.add(Postcard.MOROCCO, 10);
        POSTCARD.add(Postcard.MOSCOW, 10);
        POSTCARD.add(Postcard.MOUNT_EVERST, 10);
        POSTCARD.add(Postcard.MOUNT_FUJI, 10);
        POSTCARD.add(Postcard.MOUNT_KILIMANJARO, 10);
        POSTCARD.add(Postcard.MOUNT_MCKINLEY, 10);
        POSTCARD.add(Postcard.MOUNT_OLYMPUS, 10);
        POSTCARD.add(Postcard.MOUNT_RAINIER, 10);
        POSTCARD.add(Postcard.MOUNT_RUSHMORE, 10);
        POSTCARD.add(Postcard.MOUNT_VERNON, 10);
        POSTCARD.add(Postcard.MOUNT_WILLIAMSON, 10);
        POSTCARD.add(Postcard.MOUNTAIN_LION_PICTURES_STUDIO, 20);
        POSTCARD.add(Postcard.MY_OLD_KENTUCKY_HOME_PARK, 20);
        POSTCARD.add(Postcard.NASHVILLE, 20);
        POSTCARD.add(Postcard.NEUSCHWANSTEIN_CASTLE, 10);
        POSTCARD.add(Postcard.NEW_ORLEANS, 20);
        POSTCARD.add(Postcard.NEW_YORK, 30);
        POSTCARD.add(Postcard.NEW_ZEALAND, 10);
        POSTCARD.add(Postcard.NEWCASTLE, 20);
        POSTCARD.add(Postcard.NEWGRANGE, 10);
        POSTCARD.add(Postcard.NIAGARA_FALLS, 10);
        POSTCARD.add(Postcard.NORWAY, 10);
        POSTCARD.add(Postcard.OHIO, 20);
        POSTCARD.add(Postcard.OKLAHOMA, 10);
        POSTCARD.add(Postcard.PARIS, 20);
        POSTCARD.add(Postcard.PEARL_HARBOR, 10);
        POSTCARD.add(Postcard.PETRA, 10);
        POSTCARD.add(Postcard.PHILADELPHIA, 10);
        POSTCARD.add(Postcard.PISA, 10);
        POSTCARD.add(Postcard.POMPEII, 10);
        POSTCARD.add(Postcard.PORTUGAL, 10);
        POSTCARD.add(Postcard.PRAGUE, 10);
        POSTCARD.add(Postcard.PUERTO_RICO, 10);
        POSTCARD.add(Postcard.QUEBEC, 10);
        POSTCARD.add(Postcard.RALEIGH, 10);
        POSTCARD.add(Postcard.RED_ROCK_CANYON, 10);
        POSTCARD.add(Postcard.RICHMOND, 20);
        POSTCARD.add(Postcard.RIO_DE_JANEIRO, 10);
        POSTCARD.add(Postcard.ROME, 10);
        POSTCARD.add(Postcard.ROSWELL, 10);
        POSTCARD.add(Postcard.ROUTE_66, 30);
        POSTCARD.add(Postcard.RUSSIA, 10);
        POSTCARD.add(Postcard.SAINT_PETERSBURG, 10);
        POSTCARD.add(Postcard.SALEM, 10);
        POSTCARD.add(Postcard.SAN_DIEGO, 10);
        POSTCARD.add(Postcard.SAN_FRANCISCO, 20);
        POSTCARD.add(Postcard.SANTA_CATALINA_ISLAND, 10);
        POSTCARD.add(Postcard.SAUDI_ARABIA, 10);
        POSTCARD.add(Postcard.SCOTLAND, 10);
        POSTCARD.add(Postcard.SEATTLE, 10);
        POSTCARD.add(Postcard.SEDONA, 10);
        POSTCARD.add(Postcard.SEOUL, 10);
        POSTCARD.add(Postcard.SHANGHAI, 10);
        POSTCARD.add(Postcard.SIERRA_NEVADA, 20);
        POSTCARD.add(Postcard.SINGAPORE, 10);
        POSTCARD.add(Postcard.SOUTH_AFRICA, 10);
        POSTCARD.add(Postcard.SOUTH_AMERICA, 10);
        POSTCARD.add(Postcard.SOUTH_KOREA, 10);
        POSTCARD.add(Postcard.SPAIN, 20);
        POSTCARD.add(Postcard.SPIFFO_WORLD, 20);
        POSTCARD.add(Postcard.STONEHENGE, 10);
        POSTCARD.add(Postcard.SWEDEN, 10);
        POSTCARD.add(Postcard.SWITZERLAND, 10);
        POSTCARD.add(Postcard.SYDNEY, 10);
        POSTCARD.add(Postcard.TAIWAN, 10);
        POSTCARD.add(Postcard.TANZANIA, 10);
        POSTCARD.add(Postcard.TENNESSEE, 20);
        POSTCARD.add(Postcard.TEXAS, 20);
        POSTCARD.add(Postcard.THAILAND, 10);
        POSTCARD.add(Postcard.THE_ALAMO, 20);
        POSTCARD.add(Postcard.THE_ALPS, 20);
        POSTCARD.add(Postcard.THE_AMAZON, 10);
        POSTCARD.add(Postcard.THE_AMERICAN_GUN_MUSEUM, 30);
        POSTCARD.add(Postcard.THE_AMERICAN_HISTORY_MUSEUM, 20);
        POSTCARD.add(Postcard.THE_AMERICAN_MEDIA_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_AMERICAN_MUSIC_HALL_OF_FAME, 30);
        POSTCARD.add(Postcard.THE_AMERICAN_WWII_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_ANDES, 10);
        POSTCARD.add(Postcard.THE_APPALACHIAN_TRAIL, 30);
        POSTCARD.add(Postcard.THE_APPALACHIANS, 30);
        POSTCARD.add(Postcard.THE_AUSTIN_PARKER_MANSION, 30);
        POSTCARD.add(Postcard.THE_AUSTRALIAN_OUTBACK, 10);
        POSTCARD.add(Postcard.THE_BAHAMAS, 20);
        POSTCARD.add(Postcard.THE_BRITISH_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_BRONX, 10);
        POSTCARD.add(Postcard.THE_CALIFORNIA_SCIENCE_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_CARIBBEAN, 10);
        POSTCARD.add(Postcard.THE_CHEVALIER_MUSEUM, 20);
        POSTCARD.add(Postcard.THE_CINQUE_TERRE, 10);
        POSTCARD.add(Postcard.THE_COLOSSEUM, 10);
        POSTCARD.add(Postcard.THE_DEAD_SEA, 10);
        POSTCARD.add(Postcard.THE_EMPIRE_STATE_BUILDING, 10);
        POSTCARD.add(Postcard.THE_EVERGLADES, 10);
        POSTCARD.add(Postcard.THE_FORBIDDEN_CITY, 10);
        POSTCARD.add(Postcard.THE_FRANKIE_MONRO_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_GALAPAGOS_ISLANDS, 10);
        POSTCARD.add(Postcard.THE_GRAND_CANYON, 20);
        POSTCARD.add(Postcard.THE_GREAT_SMOKY_MOUNTAINS, 20);
        POSTCARD.add(Postcard.THE_GREAT_WALL_OF_CHINA, 10);
        POSTCARD.add(Postcard.THE_HAGIA_SOPHIA, 10);
        POSTCARD.add(Postcard.THE_HANK_GILMAN_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_HIMALAYAS, 10);
        POSTCARD.add(Postcard.THE_HOOVER_DAM, 10);
        POSTCARD.add(Postcard.THE_KLAMATH_MOUNTAINS, 10);
        POSTCARD.add(Postcard.THE_LINCOLN_MEMORIAL, 10);
        POSTCARD.add(Postcard.THE_LOUVRE, 10);
        POSTCARD.add(Postcard.THE_MATTERHORN, 10);
        POSTCARD.add(Postcard.THE_MODERN_ART_MUSEUM, 30);
        POSTCARD.add(Postcard.THE_MOJAVE, 10);
        POSTCARD.add(Postcard.THE_NATIONAL_AIR_AND_SPACE_MUSEUM, 20);
        POSTCARD.add(Postcard.THE_NATIONAL_ART_GALLERY, 30);
        POSTCARD.add(Postcard.THE_NATIONAL_BASEBALL_MUSEUM, 30);
        POSTCARD.add(Postcard.THE_NATIONAL_BASKETBALL_MUSEUM, 30);
        POSTCARD.add(Postcard.THE_NATIONAL_CAR_MUSEUM, 20);
        POSTCARD.add(Postcard.THE_NATIONAL_FOOTBALL_MUSEUM, 30);
        POSTCARD.add(Postcard.THE_NATIONAL_NATURAL_HISTORY_MUSEUM, 20);
        POSTCARD.add(Postcard.THE_NORTH_POLE, 10);
        POSTCARD.add(Postcard.THE_OSCC_HALL_OF_FAME, 30);
        POSTCARD.add(Postcard.THE_PACIFIC_COAST_HIGHWAY, 10);
        POSTCARD.add(Postcard.THE_PALACE_OF_VERSAILLES, 10);
        POSTCARD.add(Postcard.THE_PYRAMIDS_AT_GIZA, 10);
        POSTCARD.add(Postcard.THE_RED_SEA, 10);
        POSTCARD.add(Postcard.THE_RMS_QUEEN_MARY, 10);
        POSTCARD.add(Postcard.THE_ROCKIES, 30);
        POSTCARD.add(Postcard.THE_SOVIET_UNION, 10);
        POSTCARD.add(Postcard.THE_STATUE_OF_LIBERTY, 10);
        POSTCARD.add(Postcard.THE_TAJ_MAHAL, 10);
        POSTCARD.add(Postcard.THE_UFFIZI, 10);
        POSTCARD.add(Postcard.THE_US_CAPITOL, 10);
        POSTCARD.add(Postcard.THE_VIRGINIA_PATTERSON_MUSEUM, 10);
        POSTCARD.add(Postcard.THE_WASHINGTON_MONUMENT, 10);
        POSTCARD.add(Postcard.THE_WHITE_HOUSE, 20);
        POSTCARD.add(Postcard.THE_ZOCALO, 10);
        POSTCARD.add(Postcard.TIBET, 10);
        POSTCARD.add(Postcard.TIJUANA, 20);
        POSTCARD.add(Postcard.TIMES_SQUARE, 10);
        POSTCARD.add(Postcard.TOKYO, 10);
        POSTCARD.add(Postcard.TORONTO, 10);
        POSTCARD.add(Postcard.TURKEY, 10);
        POSTCARD.add(Postcard.UKRAINE, 10);
        POSTCARD.add(Postcard.URUGUAY, 10);
        POSTCARD.add(Postcard.VALLEY_FORGE, 10);
        POSTCARD.add(Postcard.VANCOUVER, 10);
        POSTCARD.add(Postcard.VATICAN_CITY, 10);
        POSTCARD.add(Postcard.VENICE, 10);
        POSTCARD.add(Postcard.VENICE_BEACH, 10);
        POSTCARD.add(Postcard.VICTORIA_FALLS, 10);
        POSTCARD.add(Postcard.VIETNAM, 10);
        POSTCARD.add(Postcard.VIRGINIA, 20);
        POSTCARD.add(Postcard.WASHINGTON_DC, 30);
        POSTCARD.add(Postcard.WEST_VIRGINIA, 10);
        POSTCARD.add(Postcard.WISCONSIN, 10);
        POSTCARD.add(Postcard.YELLOWSTONE, 10);
        POSTCARD.add(Postcard.YOSEMITE, 10);
        POSTCARD.add(Postcard.YUGOSLAVIA, 10);
        POSTCARD.add(Postcard.ZION_NATIONAL_PARK, 10);
        COLOR_FABRIC_ROLL.add(Colors.White, 10);
        COLOR_FABRIC_ROLL.add(Colors.Red, 10);
        COLOR_FABRIC_ROLL.add(Colors.Green, 10);
        COLOR_FABRIC_ROLL.add(Colors.Blue, 10);
        COLOR_LIPSTICK.add(Colors.Red, 30);
        COLOR_LIPSTICK.add(Colors.Purple, 30);
        COLOR_LIPSTICK.add(Colors.Green, 10);
        COLOR_LIPSTICK.add(Colors.Blue, 10);
        COLOR_LIPSTICK.add(Colors.Cyan, 10);
        COLOR_LIPSTICK.add(Colors.DarkGray, 10);
        COLOR_SPRAY_PAINT.add(Colors.DarkGray, 10);
        COLOR_SPRAY_PAINT.add(Colors.White, 10);
        COLOR_SPRAY_PAINT.add(Colors.Red, 10);
        COLOR_SPRAY_PAINT.add(Colors.Blue, 10);
        COLOR_SPRAY_PAINT.add(Colors.Green, 10);
        COLOR_TOY_PLANE.add(Colors.DarkGray, 10);
        COLOR_TOY_PLANE.add(Colors.White, 10);
        COLOR_TOY_PLANE.add(Colors.Red, 10);
        COLOR_TOY_PLANE.add(Colors.Blue, 10);
        COLOR_TOY_PLANE.add(Colors.Green, 10);
        COLOR_TOY_PLANE.add(Colors.LightYellow, 10);
        COLOR_TOY_PLANE.add(Colors.YellowGreen, 10);
    }
}
