// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import zombie.core.Core;
import zombie.inventory.InventoryItem;

public class Book {
    public static final Book A_BRIEF_HISTORY_OF_SWIMMING = registerBase("ABriefHistoryofSwimming", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book A_BUSINESS_MANIFESTO = registerBase("ABusinessManifesto", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book A_CLOSE_READING_OF_THE_CONSTITUTION = registerBase(
        "ACloseReadingoftheConstitution", CoverType.BOTH, BookSubject.LEGAL, BookSubject.POLITICS
    );
    public static final Book A_CRASH_COURSE_IN_MODERN_ART = registerBase("ACrashCourseinModernArt", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book A_DAME_A_DOZEN = registerBase("ADameaDozen", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book A_FAREWELL_TO_ARMS = registerBase("AFarewelltoArms", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book A_FINAL_DANCE_IN_LOUISVILLE = registerBase("AFinalDanceinLouisville", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book A_GRIEF_FILLED_HEART = registerBase("AGriefFilledHeart", CoverType.BOTH, BookSubject.SAD_NON_FICTION);
    public static final Book A_HISTORY_OF_HERESY = registerBase("AHistoryofHeresy", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book A_LIFE_WELL_PLAYED = registerBase("ALifeWellPlayed", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book A_MILITARY_FOR_THE_MODERN_AGE = registerBase(
        "AMilitaryFortheModernAge", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.MILITARY
    );
    public static final Book A_QUICK_LOOK_AT_TIME = registerBase("AQuickLookatTime", CoverType.BOTH, BookSubject.SCIENCE);
    public static final Book A_REAL_HISTORY_OF_AMERICAN_WITCHES = registerBase("ARealHistoryofAmericanWitches", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book A_SHORT_PUTT = registerBase("AShortPutt", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF);
    public static final Book A_SPORTING_LIFE = registerBase("ASportingLife", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book A_STUDY_IN_SCARLET = registerBase(
        "AStudyinScarlet", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CRIME_FICTION
    );
    public static final Book A_TEENAGERS_GUIDE_TO_BEING_A_SECRET_AGENT = registerBase(
        "ATeenagersGuidetoBeingaSecretAgent", CoverType.SOFTCOVER, BookSubject.TEENS
    );
    public static final Book A_TRAINCAR_CALLED_LOVE = registerBase(
        "ATraincarCalledLove", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book A_TREATISE_OF_HUMAN_NATURE = registerBase(
        "ATreatiseofHumanNature", CoverType.HARDCOVER, BookSubject.PHILOSOPHY, BookSubject.CLASSIC
    );
    public static final Book A_TRIP_THROUGH_THE_FOREST = registerBase("ATripThroughtheForest", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book A_VINDICATION_OF_THE_RIGHTS_OF_WOMAN = registerBase(
        "AVindicationoftheRightsofWoman", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book A_YEAR_IN_MARSEILLES = registerBase("AYearinMarseilles", CoverType.HARDCOVER, BookSubject.TRAVEL);
    public static final Book A_YEAR_ON_THE_LOUISVILLE_BEAT = registerBase("AYearontheLouisvilleBeat", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book ACCESSORY = registerBase("Accessory", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book ADAPTING_FOR_SUCCESS = registerBase("AdaptingForSuccess", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book ADDICTION_AND_SUBSTANCE_ABUSE = registerBase("AddictionandSubstanceAbuse", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book ADMINISTRATION_FOR_MANAGERS = registerBase("AdministrationforManagers", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book ADVANCED_BRIDGE_TACTICS = registerBase("AdvancedBridgeTactics", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book ADVENTURES_OF_HUCKLEBERRY_FINN = registerBase(
        "AdventuresofHuckleberryFinn", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book AFFAIRS = registerBase("Affairs", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book AFRICAN_INVERTEBRATES = registerBase("AfricanInvertebrates", CoverType.HARDCOVER, BookSubject.NATURE);
    public static final Book AFTER_LONDON = registerBase("AfterLondon", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book AFTER_THE_END = registerBase("AfterTheEnd", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book AGENT_UNDER_FIRE = registerBase("AgentUnderFire", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book AIM_WEST_BOYS = registerBase("AimWestBoys", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book AIRCRAFT_CARRIERS = registerBase("AircraftCarriers", CoverType.SOFTCOVER, BookSubject.MILITARY);
    public static final Book ALCHEMY_THEORY_AND_PRACTICE = registerBase("AlchemyTheoryandPractice", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book ALICES_ADVENTURES_IN_WONDERLAND = registerBase(
        "AlicesAdventuresinWonderland", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book ALIEN_ASTRONAUTS_MANKINDS_TRUE_ORIGINS = registerBase(
        "AlienAstronautsMankindsTrueOrigins", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.NEW_AGE
    );
    public static final Book ALL_ABOUT_NUMBERS = registerBase("AllAboutNumbers", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.CHILDS);
    public static final Book ALL_ABOUT_SATS = registerBase("AllAboutSATs", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book ALL_QUIET_ON_THE_WESTERN_FRONT = registerBase(
        "AllQuietontheWesternFront", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book ALVA_RICHARDS_THE_INTERVIEWER_INTERVIEWED = registerBase(
        "AlvaRichardsTheInterviewerInterviewed", CoverType.BOTH, BookSubject.BIOGRAPHY
    );
    public static final Book AMERICA_2000_OUR_NATION_IN_THE_21ST_CENTURY = registerBase(
        "America2000OurNationinthe21stCentury", CoverType.SOFTCOVER, BookSubject.POLITICS
    );
    public static final Book AMERICA_IN_STATISTICS = registerBase("AmericainStatistics", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book AMERICA_THE_COWARD = registerBase(
        "AmericatheCoward", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book AMERICA_UNDER_SIEGE = registerBase("AmericaUnderSiege", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book AMERICAN_MAGICK = registerBase("AmericanMagick", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book AMERICAN_SPORTS_COMPENDIUM_1991 = registerBase("AmericanSportsCompendium1991", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book AMERICAN_SPORTS_COMPENDIUM_1992 = registerBase("AmericanSportsCompendium1992", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book AMERICAS_CUTEST_DOGS = registerBase("AmericasCutestDogs", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book AMERICAS_NATIONAL_PARKS = registerBase("AmericasNationalParks", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book AMERICAS_NATURAL_HISTORY = registerBase("AmericasNaturalHistory", CoverType.SOFTCOVER, BookSubject.SCIENCE, BookSubject.NATURE);
    public static final Book AN_ECONOMIC_OVERVIEW = registerBase("AnEconomicOverview", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book AN_ESSAY_CONCERNING_HUMAN_UNDERSTANDING = registerBase(
        "AnEssayConcerningHumanUnderstanding", CoverType.HARDCOVER, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book AN_UNEXPECTED_LETTER = registerBase("AnUnexpectedLetter", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book ANALYSING_SUSPECT_PROFILES = registerBase("AnalysingSuspectProfiles", CoverType.HARDCOVER, BookSubject.POLICING);
    public static final Book ANALYSIS_POLICIES_A_GUIDE = registerBase("AnalysisPoliciesAGuide", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book ANARCHISM_AND_OTHER_ESSAYS = registerBase(
        "AnarchismandOtherEssays", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book AND_A_YES_FROM_ME = registerBase("AndAYesFromMe", CoverType.HARDCOVER, BookSubject.CINEMA);
    public static final Book ANIMAL_GRAVEYARD = registerBase("AnimalGraveyard", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book ANNA_KARENINA = registerBase("AnnaKarenina", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.ROMANCE, BookSubject.CLASSIC);
    public static final Book ANNE_OF_GREEN_GABLES = registerBase(
        "AnneofGreenGables", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book APES_WITH_TOOLS_HUMANITYS_STORY = registerBase("ApesWithToolsHumanitysStory", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book APOCALYPSE_COW = registerBase("ApocalypseCow", CoverType.SOFTCOVER, BookSubject.CONSPIRACY);
    public static final Book ARCHITECTURE_FOR_ANYONE = registerBase("ArchitectureforAnyone", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book ARE_YOU_MARRIED_TO_A_CHEATER = registerBase("AreYouMarriedtoaCheater", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book ARMED_CULTS_AND_MILITIAS_THE_THREAT_OF_THE_90S = registerBase(
        "ArmedCultsandMilitiasTheThreatofthe90s", CoverType.SOFTCOVER, BookSubject.POLICING, BookSubject.MILITARY
    );
    public static final Book AROUND_THE_WORLD_IN_EIGHTY_DAYS = registerBase(
        "AroundtheWorldinEightyDays", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book ART_OF_ROBBERY_THE_LOUISVILLE_GALLERY_HEIST = registerBase(
        "ArtofRobberyTheLouisvilleGalleryHeist", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME
    );
    public static final Book ART_OF_THE_NINETEENTH_CENTURY = registerBase("ArtoftheNineteenthCentury", CoverType.HARDCOVER, BookSubject.ART);
    public static final Book ARTEMISIA_GENTILESCHI_FORGOTTEN_MASTER = registerBase("ArtemisiaGentileschiForgottenMaster", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book ASPEN_MOUNTAIN = registerBase("AspenMountain", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book AT_THE_MOUNTAINS_OF_MADNESS = registerBase(
        "AttheMountainsofMadness", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book ATOMS_AND_MOLECULES = registerBase("AtomsandMolecules", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book ATTACK_OF_THE_TEN_INCH_BEES = registerBase("AttackoftheTenInchBees", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book AWESOME_ADVENTURES_FROM_AROUND_THE_WORLD = registerBase(
        "AwesomeAdventuresFromAroundtheWorld", CoverType.SOFTCOVER, BookSubject.CHILDS
    );
    public static final Book AWESOME_THINGS_TO_SEE_AND_DO_IN_KNOX = registerBase("AwesomeThingstoSeeandDoinKnox", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book BAG_THE_BABE_OF_YOUR_DREAMS = registerBase(
        "BagTheBabeOfYourDreams", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP
    );
    public static final Book BAPHOMETS_GRIMOIRE = registerBase("BaphometsGrimoire", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book BATTLES_IN_THE_JUNGLE = registerBase(
        "BattlesintheJungle", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book BE_PROUD_OF_YOU = registerBase("BeProudofYou", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book BEAT_THE_PROS_WITH_ONLY_THREE_CLUBS = registerBase(
        "BeattheProsWithOnlyThreeClubs", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book BECOME_A_MILLIONAIRE_WITH_ONLY_250000_DOLLARS = registerBase(
        "BecomeaMillionaireWithOnly250000Dollars", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book BECOME_WILLOWY_WITH_THE_CATKINS_DIET = registerBase("BecomeWillowywiththeCatkinsDiet", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book BEER_AND_COOKIES_THE_DIET_THAT_WILL_SHOCK_YOU = registerBase(
        "BeerandCookiesTheDietThatWillSHOCKYou", CoverType.SOFTCOVER, BookSubject.DIET
    );
    public static final Book BEHIND_THE_DESK = registerBase("BehindtheDesk", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book BEHIND_THE_HEADLINES = registerBase("BehindtheHeadlines", CoverType.BOTH, BookSubject.BIOGRAPHY);
    public static final Book BEING_THE_WIFE = registerBase("BeingtheWife", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book BEJEWELED_WITH_KISSES = registerBase("BejeweledwithKisses", CoverType.BOTH, BookSubject.ROMANCE);
    public static final Book BELLY_FAT_YOUR_WORST_ENEMY = registerBase("BellyFatYourWorstEnemy", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book BELOVED_HYMNS = registerBase("BelovedHymns", CoverType.SOFTCOVER, BookSubject.MUSIC, BookSubject.RELIGION);
    public static final Book BEN_HUR_A_TALE_OF_THE_CHRIST = registerBase(
        "BenHurATaleoftheChrist", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.RELIGION
    );
    public static final Book BEST_BANDS_OF_THE_80S = registerBase("BestBandsofthe80s", CoverType.SOFTCOVER, BookSubject.MUSIC);
    public static final Book BEYOND_GOOD_AND_EVIL = registerBase("BeyondGoodandEvil", CoverType.HARDCOVER, BookSubject.PHILOSOPHY, BookSubject.CLASSIC);
    public static final Book BEYOND_MAGIC = registerBase("BeyondMagic", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book BEYOND_THE_WALL_STORIES_FROM_EAST_BERLIN = registerBase("BeyondtheWallStoriesfromEastBerlin", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book BIBLE_STUDY_GUIDE = registerBase("BibleStudyGuide", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book BIBLICAL_FATHERS_FROM_ADAM_TO_JOSEPH = registerBase("BiblicalFathersFromAdamtoJoseph", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book BIBLICAL_MOTHERS_FROM_EVE_TO_MARY = registerBase("BiblicalMothersFromEvetoMary", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book BICYCLE_MY_CYCLE = registerBase("BicycleMyCycle", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book BIG_GOVERNMENT_MELTDOWN = registerBase("BigGovernmentMeltdown", CoverType.BOTH, BookSubject.POLITICS);
    public static final Book BIG_MOVIE_GUIDE_92 = registerBase("BigMovieGuide92", CoverType.HARDCOVER, BookSubject.CINEMA);
    public static final Book BIG_MOVIE_GUIDE_93 = registerBase("BigMovieGuide93", CoverType.HARDCOVER, BookSubject.CINEMA);
    public static final Book BILLS_AND_LAWS = registerBase("BillsandLaws", CoverType.HARDCOVER, BookSubject.LEGAL, BookSubject.POLITICS);
    public static final Book BLACK_BEAUTY = registerBase("BlackBeauty", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS);
    public static final Book BLACK_MAGIC_AND_HOW_TO_FIGHT_IT = registerBase("BlackMagicAndHowtoFightIt", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book BLACK_WIDOW_THE_KAITLIN_RYAN_MURDERS = registerBase("BlackWidowTheKaitlinRyanMurders", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book BLACKJACK_101 = registerBase("Blackjack101", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book BLESSINGS_FROM_THE_BIBLE = registerBase("BlessingsFromtheBible", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book BLOOD_OR_GLORY = registerBase("BloodorGlory", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book BLOODY_NOSES_BROKEN_VOWS_THE_HECTOR_LOPEZ_STORY = registerBase(
        "BloodyNosesBrokenVowsTheHectorLopezStory", CoverType.BOTH, BookSubject.SPORTS
    );
    public static final Book BLOODY_REDCOATS_THE_REVOLUTIONARY_WAR = registerBase(
        "BloodyRedcoatsTheRevolutionaryWar", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book BLOWN_AWAY_BY_BRILLIANCE_THE_Z_HURRICANES = registerBase(
        "BlownAwaybyBrillianceTheZHurricanes", CoverType.BOTH, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book BOOK_BONFIRE = registerBase("BookBonfire", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book BOOKS_STANDING_UP_TO_BULLIES = registerBase("StandingUptoBullies", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book BOTH_SIDES_OF_THE_LAW = registerBase(
        "BothSidesoftheLaw", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book BOWLED_UNDER_MY_SIDE_OF_THE_STORY = registerBase("BowledUnderMySideoftheStory", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book BRASS_INSTRUMENTS_FOR_BEGINNERS = registerBase("BrassInstrumentsforBeginners", CoverType.HARDCOVER, BookSubject.MUSIC);
    public static final Book BRAVO_ROMEO_ECHO_NOVEMBER = registerBase(
        "BravoRomeoEchoNovember", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION, BookSubject.MILITARY
    );
    public static final Book BREAKING_NEWS = registerBase("BreakingNews", CoverType.BOTH, BookSubject.BIOGRAPHY);
    public static final Book BRITISH_PREMIER_LEAGUE_SOCCER_91 = registerBase("BritishPremierLeagueSoccer91", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book BRITISH_PREMIER_LEAGUE_SOCCER_92 = registerBase("BritishPremierLeagueSoccer92", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book BROKEN_JENKINS = registerBase("BrokenJenkins", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book BUILD_YOUR_OWN_ROBOT = registerBase(
        "BuildYourOwnRobot", CoverType.SOFTCOVER, BookSubject.SCIENCE, BookSubject.TEENS, BookSubject.CHILDS
    );
    public static final Book BULK_UP_WITH_PROTEIN = registerBase("BulkUpWithProtein", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book BURNT_CAKE = registerBase("BurntCake", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book BUSINESS_TECHNOLOGY = registerBase("BusinessTechnology", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book CALLED_TO_THE_BAR = registerBase("CalledtotheBar", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book CANCER_CARE = registerBase("CancerCare", CoverType.SOFTCOVER, BookSubject.MEDICAL);
    public static final Book CANDIDE = registerBase("Candide", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book CANNIBAL_PLANE_CRASH_A_HORRIFIC_TRUE_STORY = registerBase(
        "CannibalPlaneCrashAHorrificTrueStory", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION
    );
    public static final Book CAPITAL = registerBase("Capital", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC);
    public static final Book CAPITAL_PUNISHMENT_FACTS_AND_STORIES = registerBase(
        "CapitalPunishmentFactsandStories", CoverType.SOFTCOVER, BookSubject.LEGAL, BookSubject.POLITICS
    );
    public static final Book CARING_ILLNESS_AND_LOSS = registerBase("CaringIllnessandLoss", CoverType.SOFTCOVER, BookSubject.MEDICAL);
    public static final Book CARMEN = registerBase("Carmen", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book CARRIBBEAN_CRUISING = registerBase("CarribbeanCruising", CoverType.SOFTCOVER, BookSubject.TRAVEL);
    public static final Book CATASTROPHE_HOW_TO_STOP_THE_POPULATION_EXPLOSION = registerBase(
        "CatastropheHowtoStopthePopulationExplosion", CoverType.SOFTCOVER, BookSubject.CONSPIRACY
    );
    public static final Book CELEBRITY_STYLE = registerBase("CelebrityStyle", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book CHAIR_TO_THE_FACE_LOVE_TO_THE_SOUL = registerBase("ChairtotheFaceLovetotheSoul", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book CHAOASIS_FINDING_PEACE_IN_TIMES_OF_TROUBLE = registerBase(
        "ChaoasisFindingPeaceinTimesofTrouble", CoverType.SOFTCOVER, BookSubject.SELF_HELP
    );
    public static final Book CHASING_COCHISE = registerBase("ChasingCochise", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book CHEKHOVS_SHORT_STORIES = registerBase(
        "ChekhovsShortStories", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book CHEVALIER_CROUPIER = registerBase("ChevalierCroupier", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book CHICKEN_ON_A_SUMMERS_DAY = registerBase(
        "ChickenonaSummersDay", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book CHILE_A_NARROW_HEAVEN = registerBase("ChileANarrowHeaven", CoverType.SOFTCOVER, BookSubject.TRAVEL);
    public static final Book CHINA_REVISITED = registerBase("ChinaRevisited", CoverType.HARDCOVER, BookSubject.TRAVEL);
    public static final Book CHOKED_WITH_PLASTIC_THE_STATE_OF_OUR_OCEANS = registerBase(
        "ChokedwithPlasticTheStateofOurOceans", CoverType.SOFTCOVER, BookSubject.SCIENCE
    );
    public static final Book CINEMA_A_HISTORY = registerBase("CinemaAHistory", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book CIVIL_ACTIONS = registerBase("CivilActions", CoverType.SOFTCOVER, BookSubject.LEGAL);
    public static final Book CIVIL_RIGHTS_AND_DISCRIMINATION = registerBase(
        "CivilRightsandDiscrimination", CoverType.HARDCOVER, BookSubject.POLICING, BookSubject.LEGAL
    );
    public static final Book CLAIRVOYANCE_AND_TELEKINESIS = registerBase(
        "ClairvoyanceandTelekinesis", CoverType.SOFTCOVER, BookSubject.OCCULT, BookSubject.NEW_AGE
    );
    public static final Book CLASSIC_MOVIES = registerBase("ClassicMovies", CoverType.HARDCOVER, BookSubject.CINEMA);
    public static final Book CLAWMARKS = registerBase("Clawmarks", CoverType.BOTH, BookSubject.HORROR);
    public static final Book CODING_CHEAT_GUIDE_93 = registerBase("CodingCheatGuide93", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book CODING_IN_FRENCH = registerBase("CodinginFrench", CoverType.BOTH, BookSubject.COMPUTER);
    public static final Book COLD_CASES_OF_THE_KNOX_REGION = registerBase("ColdCasesoftheKnoxRegion", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book COLLAPSE_OF_THE_SOVIET_UNION = registerBase(
        "CollapseoftheSovietUnion", CoverType.BOTH, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book COLOR_SYMBOLISM = registerBase("ColorSymbolism", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book COMMON_SENSE = registerBase(
        "CommonSense", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book COMPLETE_BOOK_OF_THE_1988_OLYMPICS = registerBase("CompleteBookofthe1988Olympics", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book COMPLETE_BOOK_OF_THE_1992_OLYMPICS = registerBase("CompleteBookofthe1992Olympics", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book COMPLEX_PROJECT_MANAGEMENT = registerBase("ComplexProjectManagement", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book COMPUTER_GAMES_LOST_OUR_CHILDREN = registerBase("ComputerGamesLostOurChildren", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book COMPUTER_SECURITY_MANUAL_THE_BASICS = registerBase("ComputerSecurityManualTheBasics", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book COMPUTERS_A_BUYERS_GUIDE = registerBase("ComputersABuyersGuide", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book CONFESSIONS_OF_TANK_COMMANDER = registerBase("ConfessionsofTankCommander", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book CONGRESS_HOW_IT_WORKS = registerBase("CongressHowItWorks", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book CONMEN_SCAM_ARTISTS_AND_GRIFFTERS_A_SHORT_HISTORY = registerBase(
        "ConmenScamArtistsandGriftersAShortHistory", CoverType.BOTH, BookSubject.TRUE_CRIME
    );
    public static final Book CONSERVATIVES_THE_REAL_LIBERALS = registerBase(
        "ConservativesTheRealLiberals", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS
    );
    public static final Book CONSTITUTION_OF_KENTUCKY = registerBase(
        "ConstitutionofKentucky", CoverType.BOTH, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book CONTACTING_THE_OTHERWORLD = registerBase("ContactingtheOtherworld", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book COOL_CHEMISTRY = registerBase("CoolChemistry", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.CHILDS);
    public static final Book COOL_KICKS_AND_FLIPS_THAT_WOW_THE_BABES = registerBase("CoolKicksandFlipsthatWowtheBabes", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book COPULATION = registerBase("Copulation", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book COPYRIGHT_LAWS = registerBase("CopyrightLaws", CoverType.HARDCOVER, BookSubject.LEGAL);
    public static final Book CORRUPTION_DETECTION_AND_LEGALITIES = registerBase("CorruptionDetectionandLegalities", CoverType.HARDCOVER, BookSubject.LEGAL);
    public static final Book CORRUPTION_ON_PENNSYLVANIA_AVENUE = registerBase("CorruptiononPennsylvaniaAvenue", CoverType.BOTH, BookSubject.POLITICS);
    public static final Book COUNTING_THE_CLOUDS = registerBase("CountingtheClouds", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book CRAZY_BUT_NOT_STUPID = registerBase("CrazybutNotStupid", CoverType.SOFTCOVER, BookSubject.BASEBALL, BookSubject.SPORTS);
    public static final Book CRAZY_SWEDES_BASEBALL_COMPANION = registerBase(
        "CrazySwedesBaseballCompanion", CoverType.BOTH, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book CREATURES_OF_THE_AMAZON = registerBase("CreaturesoftheAmazon", CoverType.HARDCOVER, BookSubject.NATURE);
    public static final Book CRIKEY_CRICKET_BOWLING_DOWN_UNDER = registerBase("CrikeyCricketBowlingDownUnder", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book CRIME_AND_PUNISHMENT = registerBase("CrimeandPunishment", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book CRIME_IN_CINCINNATI = registerBase("CrimeinCincinnati", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book CRIMES_OF_THE_RICH = registerBase("CrimesoftheRich", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book CRIMINAL_BEHAVIOR = registerBase("CriminalBehavior", CoverType.BOTH, BookSubject.POLICING);
    public static final Book CRIMINAL_PROCEDURES = registerBase("CriminalProcedures", CoverType.SOFTCOVER);
    public static final Book CRITIQUE_OF_PURE_REASON = registerBase(
        "CritiqueofPureReason", CoverType.HARDCOVER, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book CRONTON = registerBase("Cronton", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book CROWLEYS_SECRETS = registerBase("CrowleysSecrets", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book CRUSHED_HOPE_THE_STORY_OF_MY_CHILDHOOD = registerBase(
        "CrushedHopeTheStoryofMyChildhood", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION
    );
    public static final Book CRY_YOURSELF_THIN = registerBase("CryYourselfThin", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book CULTS_AND_SECTS = registerBase(
        "CultsandSects", CoverType.SOFTCOVER, BookSubject.OCCULT, BookSubject.POLICING, BookSubject.RELIGION
    );
    public static final Book CURE = registerBase("Cure", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book CUSTERS_COMEUPPANCE = registerBase("CustersComeuppance", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book CUSTODIAL_SENTENCING = registerBase("CustodialSentencing", CoverType.HARDCOVER, BookSubject.POLICING, BookSubject.LEGAL);
    public static final Book CUSTOMER_CENTRIC_CARE = registerBase("CustomerCentricCare", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book CUTE_CRITTERS = registerBase("CuteCritters", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book D_DAY_IN_THEIR_WORDS = registerBase(
        "DDayInTheirWords", CoverType.BOTH, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book DADS_SMILE = registerBase("DadsSmile", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book DANGEROUS_PROTOCOL = registerBase("DangerousProtocol", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book DARK_AGENT = registerBase("DarkAgent", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book DASTARDLYS_TRAIN_ROBBERY = registerBase("DastardlysTrainRobbery", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book DAUGHTERS_IN_LAW = registerBase("DaughtersinLaw", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book DEATH_AND_TEARS_THE_TRUE_STORY_OF_AMERICA = registerBase(
        "DeathandTearsTheTrueStoryofAmerica", CoverType.SOFTCOVER, BookSubject.POLITICS
    );
    public static final Book DEATH_ON_THE_FOOD_COURT_THE_CROSSROADS_MALL_KILLINGS = registerBase(
        "DeathontheFoodCourtTheCrossroadsMallKillings", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME
    );
    public static final Book DEEP_TIME_DEEP_SPACE = registerBase("DeepTimeDeepSpace", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book DEFEAT_YOUR_ENEMIES_WHILE_MAKING_HUGE_PROFITS = registerBase(
        "DefeatYourEnemiesWhileMakingHugeProfits", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book DELICIOUS_COCKTAILS = registerBase("DeliciousCocktails", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book DEMOCRACY_IN_AMERICA = registerBase(
        "DemocracyinAmerica", CoverType.BOTH, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book DETECTIVE_WICKLOW_INVESTIGATES = registerBase("DetectiveWicklowInvestigates", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book DETECTIVE_WICKLOW_ON_TRIAL = registerBase("DetectiveWicklowOnTrial", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book DETECTIVE_WICKLOW_RETURNS = registerBase("DetectiveWicklowReturns", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book DEVELOP_YOUR_OWN_PHOTOGRAPHS = registerBase("DevelopYourOwnPhotographs", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book DIET_AND_LIFESTYLES = registerBase(
        "DietandLifestyles", CoverType.HARDCOVER, BookSubject.DIET, BookSubject.SCHOOL_TEXTBOOK, BookSubject.MEDICAL
    );
    public static final Book DIET_SECRETS_OF_THE_CELEBS = registerBase("DietSecretsoftheCelebs", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book DISCOURSE_ON_INEQUALITY = registerBase(
        "DiscourseonInequality", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book DISEASES_OF_THE_ORGANS = registerBase("DiseasesoftheOrgans", CoverType.SOFTCOVER, BookSubject.MEDICAL);
    public static final Book DIVANI_KEBIR = registerBase("DivaniKebir", CoverType.BOTH, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION);
    public static final Book DIVORCE_HOW_TO_DO_IT_RIGHT = registerBase("DivorceHowToDoItRight", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book DNA_TESTING_A_REVOLUTION = registerBase("DNATestingARevolution", CoverType.SOFTCOVER, BookSubject.POLICING);
    public static final Book DNA_THE_FUTURE_OF_INVESTIGATION = registerBase("DNATheFutureofInvestigation", CoverType.SOFTCOVER, BookSubject.POLICING);
    public static final Book DO_ALIENS_CONTROL_US = registerBase("DoAliensControlUs", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.NEW_AGE);
    public static final Book DO_ROBOTS_PAY_FOR_THEIR_ELECTRICITY = registerBase("DoRobotsPayForTheirElectricity", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book DODGE_CITY_TALES = registerBase("DodgeCityTales", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book DOG_GOBLIN_CHRONICLES = registerBase("DogGoblinChronicles", CoverType.BOTH, BookSubject.HORROR);
    public static final Book DOG_GOBLIN_ORIGINS = registerBase("DogGoblinOrigins", CoverType.BOTH, BookSubject.HORROR);
    public static final Book DOG_GOBLIN_VENGEANCE = registerBase("DogGoblinVengeance", CoverType.BOTH, BookSubject.HORROR);
    public static final Book DOGGIE_TREATS_A_WEIGHT_LOSS_MIRACLE = registerBase("DoggieTreatsAWeightLossMiracle", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book DONT_CRASH_DONT_BURN = registerBase("DontCrashDontBurn", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book DONT_TRUST_YOUR_DOCTOR = registerBase("DontTrustYourDoctor", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.QUACKERY);
    public static final Book DOOR_TO_DOOR = registerBase("DoortoDoor", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book DOUBLE_BOGEY_LESSONS_FROM_AN_ABOVE_PAR_LIFE = registerBase(
        "DoubleBogeyLessonsfromAnAboveParLife", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book DRACULA = registerBase("Dracula", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC);
    public static final Book DRAGONS_OF_DAGGERTHORN = registerBase("DragonsofDaggerthorn", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book DRAWING_CUTE_CRITTERS = registerBase("DrawingCuteCritters", CoverType.BOTH, BookSubject.ART);
    public static final Book DRAWING_NUDES = registerBase("DrawingNudes", CoverType.SOFTCOVER, BookSubject.ART, BookSubject.SEXY);
    public static final Book DREAMARROW = registerBase("Dreamarrow", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book DRESSING_TASTEFULLY = registerBase("DressingTastefully", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book DUBLINERS = registerBase("Dubliners", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book DYING_FOR_FREEDOM = registerBase("DyingForFreedom", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY);
    public static final Book DYING_STRIKE = registerBase("DyingStrike", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book EARLY_BIRTHDAY = registerBase("EarlyBirthday", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book EARTHQUAKES_VOLCANOES_AND_OTHER_DISASTERS = registerBase(
        "EarthquakesVolcanoesandOtherDisasters", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION
    );
    public static final Book EAST_AND_WEST_THE_GREAT_SCHISM = registerBase(
        "EastandWestTheGreatSchism", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.RELIGION
    );
    public static final Book EASTERN_THOUGHT = registerBase(
        "EasternThought", CoverType.SOFTCOVER, BookSubject.PHILOSOPHY, BookSubject.HISTORY, BookSubject.NEW_AGE, BookSubject.RELIGION
    );
    public static final Book EASTERN_THOUGHTS_LONGER_LIVES = registerBase("EasternThoughtsLongerLives", CoverType.SOFTCOVER, BookSubject.SELF_HELP);
    public static final Book ECHOING_SCREAMS = registerBase("EchoingScreams", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book EINSTEIN = registerBase("Einstein", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.SCIENCE, BookSubject.HISTORY);
    public static final Book ELECTION_92 = registerBase("Election92", CoverType.HARDCOVER, BookSubject.POLITICS);
    public static final Book EMBRACING_THE_FLAG = registerBase("EmbracingtheFlag", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book EMMA = registerBase("Emma", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.ROMANCE, BookSubject.CLASSIC);
    public static final Book EMPTY_QUIVER_THE_REAL_THREAT_OF_NUCLEAR_HIJACK = registerBase(
        "EmptyQuiverTheRealThreatofNuclearHijack", CoverType.SOFTCOVER, BookSubject.MILITARY
    );
    public static final Book ENCYCLOPEDIA_HIBERNIA_AB = registerBase("EncyclopediaHiberniaAB", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_C = registerBase("EncyclopediaHiberniaC", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_DF = registerBase("EncyclopediaHiberniaDF", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_GJ = registerBase("EncyclopediaHiberniaGJ", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_KL = registerBase("EncyclopediaHiberniaKL", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_MN = registerBase("EncyclopediaHiberniaMN", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_OP = registerBase("EncyclopediaHiberniaOP", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_QS = registerBase("EncyclopediaHiberniaQS", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book ENCYCLOPEDIA_HIBERNIA_TZ = registerBase("EncyclopediaHiberniaTZ", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE);
    public static final Book END_OF_LIFE_CARE = registerBase("EndofLifeCare", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book EROTIC_TECHNIQUES_OF_THE_EAST = registerBase("EroticTechniquesoftheEast", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book ETHICS_AM_I_A_GOOD_PERSON = registerBase(
        "EthicsAmIAGoodPerson", CoverType.SOFTCOVER, BookSubject.PHILOSOPHY, BookSubject.SELF_HELP
    );
    public static final Book ETHNICITY_AND_CONFLICT = registerBase("EthnicityAndConflict", CoverType.SOFTCOVER, BookSubject.MILITARY_HISTORY);
    public static final Book EUROPEAN_RACING_CARS = registerBase("EuropeanRacingCars", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book EVERY_COUNTRY_ON_EARTH = registerBase("EveryCountryonEarth", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.CHILDS);
    public static final Book EVERYTHING_OR_NOTHING = registerBase("EverythingorNothing", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book EXOTIC_DANCING_THE_FULL_TRUTH = registerBase("ExoticDancingTheFullTruth", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book EYE_OF_THE_STORM = registerBase("EyeoftheStorm", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book EYEWITNESSES_DO_S_AND_DONTS = registerBase("EyewitnessesDosandDonts", CoverType.HARDCOVER, BookSubject.POLICING);
    public static final Book FAILURE_A_TRUE_HISTORY_OF_CONSERVATIVES = registerBase(
        "FailureATrueHistoryofConservatives", CoverType.SOFTCOVER, BookSubject.POLITICS
    );
    public static final Book FAILURES_OF_GOVERNMENT = registerBase("FailuresofGovernment", CoverType.BOTH, BookSubject.POLITICS);
    public static final Book FALLING_DOWN_EVEREST = registerBase("FallingDownEverest", CoverType.BOTH, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book FALLING_SON = registerBase("FallingSon", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book FAMILY_PLANNING = registerBase("FamilyPlanning", CoverType.SOFTCOVER, BookSubject.MEDICAL);
    public static final Book FAMINE_1985 = registerBase("Famine1985", CoverType.SOFTCOVER, BookSubject.CONSPIRACY);
    public static final Book FAR_FROM_THE_MADDING_CROWD = registerBase(
        "FarFromtheMaddingCrowd", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book FARM_PRICE_GUIDE_92 = registerBase("FarmPriceGuide92", CoverType.HARDCOVER, BookSubject.FARMING);
    public static final Book FARM_PRICE_GUIDE_93 = registerBase("FarmPriceGuide93", CoverType.HARDCOVER, BookSubject.FARMING);
    public static final Book FASHION_ICONS_OF_THE_60S = registerBase("FashionIconsofthe60s", CoverType.HARDCOVER, BookSubject.FASHION);
    public static final Book FASHION_ICONS_OF_THE_70S = registerBase("FashionIconsofthe70s", CoverType.HARDCOVER, BookSubject.FASHION);
    public static final Book FASHION_ICONS_OF_THE_80S = registerBase("FashionIconsofthe80s", CoverType.HARDCOVER, BookSubject.FASHION);
    public static final Book FASHION_THAT_NEVER_GOES_OUT_OF_STYLE = registerBase("FashionThatNeverGoesOutofStyle", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book FASHIONSOFTHE_WORLD = registerBase("FashionsoftheWorld", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book FATHETIC_THE_THINKING_THAT_KEEPS_YOU_OVERWEIGHT = registerBase(
        "FatheticTheThinkingThatKeepsYouOverweight", CoverType.SOFTCOVER, BookSubject.DIET
    );
    public static final Book FEAR_AND_TREMBLING = registerBase("FearandTrembling", CoverType.BOTH, BookSubject.PHILOSOPHY, BookSubject.CLASSIC);
    public static final Book FEMALE_LEADERS_THROUGHOUT_HISTORY = registerBase(
        "FemaleLeadersThroughoutHistory", CoverType.SOFTCOVER, BookSubject.POLITICS, BookSubject.HISTORY
    );
    public static final Book FIDELIO = registerBase("Fidelio", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book FIFTY_DAYS_WITHOUT_WATER = registerBase("FiftyDaysWithoutWater", CoverType.BOTH, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book FIFTY_SIGNS_SHES_CHEATING = registerBase("FiftySignsShesCheating", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book FINAL_BITE = registerBase("FinalBite", CoverType.SOFTCOVER, BookSubject.ROMANCE, BookSubject.HORROR);
    public static final Book FINAL_DAYS_OF_THE_THIRD_REICH = registerBase(
        "FinalDaysoftheThirdReich", CoverType.BOTH, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book FIND_FRANK = registerBase("FindFrank", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book FIND_FRANK_AT_THE_BEACH = registerBase("FindFrankAttheBeach", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book FIND_FRANK_IN_BRITAIN = registerBase("FindFrankInBritain", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book FIND_FRANK_IN_THE_WILD_WEST = registerBase("FindFrankIntheWildWest", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book FIND_ROSES_IN_THE_THORNS = registerBase("FindRosesintheThorns", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book FINDING_BRILLIANCE_LESSONS_FROM_GREAT_COMPANIES = registerBase(
        "FindingBrillianceLessonsFromGreatCompanies", CoverType.HARDCOVER, BookSubject.BUSINESS
    );
    public static final Book FINDING_THE_PERFECT_COLLEGE = registerBase("FindingthePerfectCollege", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book FINGERPRINT = registerBase("Fingerprint", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book FIRST_PAST_THE_POST = registerBase("FirstPastthePost", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book FIVE_MORE_WAYS_TO_CATCH_HIM_AND_KEEP_HIM = registerBase(
        "FiveMoreWaystoCatchHimAndKeepHim", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP
    );
    public static final Book FIVE_TO_TEN = registerBase("FivetoTen", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book FIVE_WAYS_TO_CATCH_HIS_EYE_FOR_GOOD = registerBase("FiveWaystoCatchHisEyeForGood", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book FIVEPLAY = registerBase("Fiveplay", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book FLATEARTH_QUEST_FOR_THE_CROWN = registerBase("FlatearthQuestfortheCrown", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book FLATEARTH_THE_PRINCES_REBELLION = registerBase("FlatearthThePrincesRebellion", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book FLATEARTH_THE_REDLANDS = registerBase("FlatearthTheRedlands", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book FOOTBALL_FOOTBALL_FOOTBALL = registerBase("FootballFootballFootball", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book FOOTBALL_TACTICS = registerBase("FootballTactics", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book FOR_OLD_TIMES_SAKE = registerBase(
        "ForOldTimesSake", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book FOR_THE_LAST_TIME = registerBase("FortheLastTime", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book FORBIDDEN_INTENTION = registerBase("ForbiddenIntention", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book FORBIDDEN_SECRETS_OF_LOVEMAKING = registerBase("ForbiddenSecretsofLovemaking", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book FORENSICS = registerBase("Forensics", CoverType.SOFTCOVER, BookSubject.POLICING);
    public static final Book FORGOTTEN_PIONEERS = registerBase("ForgottenPioneers", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY);
    public static final Book FORGOTTEN_WORLD = registerBase("ForgottenWorld", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book FORTY_HEIGHT_GOLFING_TIPS_THEY_DONT_WANT_YOU_TO_KNOW = registerBase(
        "48GolfingTipsTheyDontWantYoutoKnow", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book FORTY_THREE_STOCKS_THAT_WILL_EXPLODE_IN_VALUE = registerBase(
        "43StocksThatWillEXPLODEInValue", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book FOUNDATIONS_OF_A_SOUND_ECONOMY = registerBase("FoundationsofaSoundEconomy", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book FOUNDING_FATHERS_THE_VOICES_OF_REASON = registerBase(
        "FoundingFathersTheVoicesofReason", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS
    );
    public static final Book FRANK_FRINKEL_AND_THE_CARAMEL_MACHINE = registerBase("FrankFrinkelandtheCaramelMachine", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book FRANKENSTEIN = registerBase(
        "Frankenstein", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book FRIDAY_ENDS_AT_MIDNIGHT = registerBase("FridayEndsatMidnight", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book FROM_INSIDE_THE_PORTALS = registerBase("FromInsidethePortals", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book FROM_ME_TO_SHINING_ME = registerBase("FromMetoShiningMe", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book FROSTFIEND = registerBase("Frostfiend", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book FULL_COURT_PRESS = registerBase("FullCourtPress", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book FUNDING_AND_INVESTMENT_PRINCIPLES = registerBase("FundingandInvestmentPrinciples", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book FUNNIEST_RACEHORSE_NAMES = registerBase("FunniestRacehorseNames", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book FUTUREMAN_HOW_WE_MIGHT_EVOLVE = registerBase("FuturemanHowWeMightEvolve", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book GALLIC_WARS = registerBase(
        "GallicWars", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.MILITARY
    );
    public static final Book GALWAYS_GALLOWS = registerBase("GalwaysGallows", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book GATHERING_EVIDENCE = registerBase("GatheringEvidence", CoverType.SOFTCOVER, BookSubject.POLICING);
    public static final Book GENERAL_ACCOUNTANCY = registerBase("GeneralAccountancy", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book GENUINES_BOOK_OF_GLOBAL_RECORDS_88 = registerBase(
        "GenuinesBookofGlobalRecords88", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE, BookSubject.CHILDS
    );
    public static final Book GENUINES_BOOK_OF_GLOBAL_RECORDS_89 = registerBase(
        "GenuinesBookofGlobalRecords89", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE, BookSubject.CHILDS
    );
    public static final Book GENUINES_BOOK_OF_GLOBAL_RECORDS_90 = registerBase(
        "GenuinesBookofGlobalRecords90", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE, BookSubject.CHILDS
    );
    public static final Book GENUINES_BOOK_OF_GLOBAL_RECORDS_91 = registerBase(
        "GenuinesBookofGlobalRecords91", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE, BookSubject.CHILDS
    );
    public static final Book GENUINES_BOOK_OF_GLOBAL_RECORDS_92 = registerBase(
        "GenuinesBookofGlobalRecords92", CoverType.HARDCOVER, BookSubject.GENERAL_REFERENCE, BookSubject.CHILDS
    );
    public static final Book GEORGIA_O_KEEFFE_AMERICAN_MODERNIST = registerBase("GeorgiaOKeeffeAmericanModernist", CoverType.BOTH, BookSubject.ART);
    public static final Book GERMANY_BY_TRAIN = registerBase("GermanyByTrain", CoverType.SOFTCOVER, BookSubject.TRAVEL);
    public static final Book GET_RICH_BY_PAINTING = registerBase("GetRichbyPainting", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book GHOSTS = registerBase("Ghosts", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book GIFTS_OF_THE_MAGI = registerBase("GiftsoftheMagi", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book GIVING_AS_GOOD_AS_YOU_GET = registerBase("GivingasGoodasYouGet", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book GOD_SMILED_AT_ME = registerBase("GodSmiledatMe", CoverType.BOTH, BookSubject.SELF_HELP, BookSubject.RELIGION);
    public static final Book GOLD_RUSH_AT_DAWNSTRUCK_MOUNTAIN = registerBase("GoldRushatDawnstruckMountain", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book GOLF_FOR_ANYONE = registerBase("GolfForAnyone", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF);
    public static final Book GOOD_INTERVIEW_TECHNIQUES = registerBase("GoodInterviewTechniques", CoverType.HARDCOVER, BookSubject.POLICING);
    public static final Book GORESBRIDGE = registerBase("Goresbridge", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book GOSPEL_TRUTHS_HOW_THE_BIBLE_CAN_CURE_US = registerBase(
        "GospelTruthsHowTheBibleCanCureUs", CoverType.SOFTCOVER, BookSubject.QUACKERY
    );
    public static final Book GRASS_AND_WATER_RADICAL_DIETING = registerBase("GrassandWaterRadicalDieting", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book GREAT_ART_OF_THE_CLASSICAL_WORLD = registerBase("GreatArtoftheClassicalWorld", CoverType.HARDCOVER, BookSubject.ART);
    public static final Book GREAT_ART_OF_THE_MIDDLE_EAST = registerBase("GreatArtoftheMiddleEast", CoverType.HARDCOVER, BookSubject.ART);
    public static final Book GREAT_ART_OF_THE_RENAISSANCE = registerBase("GreatArtoftheRenaissance", CoverType.HARDCOVER, BookSubject.ART);
    public static final Book GREAT_EXPECTATIONS = registerBase("GreatExpectations", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book GREAT_INDUSTRIES_AND_THE_MEN_WHO_MADE_THEM = registerBase(
        "GreatIndustriesandtheMenWhoMadeThem", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.BUSINESS
    );
    public static final Book GREAT_LANDMARKS_OF_THE_WORLD = registerBase("GreatLandmarksoftheWorld", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book GREATEST_SPEECHES_OF_ALL_TIME = registerBase(
        "GreatestSpeechesofAllTime", CoverType.SOFTCOVER, BookSubject.POLITICS, BookSubject.HISTORY
    );
    public static final Book GREEN_TEA_CURED_MY_SOUL = registerBase("GreenTeaCuredMySoul", CoverType.BOTH, BookSubject.QUACKERY);
    public static final Book GUITAR_FOR_BEGINNERS = registerBase("GuitarforBeginners", CoverType.HARDCOVER, BookSubject.MUSIC);
    public static final Book HAIR_EXTENSIONS_AND_YOU = registerBase("HairExtensionsandYou", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book HAIRYFOOT = registerBase("Hairyfoot", CoverType.SOFTCOVER, BookSubject.FANTASY, BookSubject.CHILDS);
    public static final Book HAMLET = registerBase("Hamlet", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book HARD_LANDING = registerBase("HardLanding", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book HAUNTED_BY_DOFKAR = registerBase("HauntedbyDofkar", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book HAUNTED_KNOX_LOCAL_GHOST_STORIES = registerBase("HauntedKnoxLocalGhostStories", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book HEADING_FOR_THE_HEAT_FROM_ROSEWOOD_PRISON_TO_STARDOM = registerBase(
        "HeadingfortheHeatFromRosewoodPrisontoStardom", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.MUSIC
    );
    public static final Book HEALING_HANDS_MY_STORY = registerBase("HealingHandsMyStory", CoverType.BOTH, BookSubject.NEW_AGE, BookSubject.QUACKERY);
    public static final Book HEALING_THROUGH_JESUS = registerBase("HealingThroughJesus", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book HEALTHCARE_HAULS_HOW_THE_SICK_CAN_FILL_YOUR_POCKETS = registerBase(
        "HealthcareHaulsHowtheSickCanFillYourPockets", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book HEART_CONDITIONS = registerBase("HeartConditions", CoverType.SOFTCOVER, BookSubject.MEDICAL);
    public static final Book HEART_OF_DARKNESS = registerBase("HeartofDarkness", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book HEIDI = registerBase("Heidi", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS);
    public static final Book HEIGHTEN_YOUR_PLEASURE = registerBase("HeightenYourPleasure", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book HELICOPTERS_AND_ARTILLERY = registerBase("HelicoptersandArtillery", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book HELMET_HAIR = registerBase("HelmetHair", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book HIDDEN_RESEARCH_THE_FINNEGAN_INSIDER = registerBase(
        "HiddenResearchTheFinneganInsider", CoverType.SOFTCOVER, BookSubject.CONSPIRACY
    );
    public static final Book HIGH_FLOP = registerBase("HighFlop", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book HIGH_RISE_HOMICIDE = registerBase("HighRiseHomicide", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book HIM = registerBase("Him", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book HIS_STEELY_STARE = registerBase("HisSteelyStare", CoverType.SOFTCOVER, BookSubject.ROMANCE, BookSubject.HORROR);
    public static final Book HISTORY_OF_MOTOR_RACING = registerBase(
        "HistoryofMotorRacing", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY, BookSubject.SPORTS
    );
    public static final Book HISTORYS_MOST_BAFFLING_MYSTERIES = registerBase(
        "HistorysMostBafflingMysteries", CoverType.BOTH, BookSubject.HISTORY, BookSubject.TRUE_CRIME
    );
    public static final Book HISTORYS_WORSE_EPIDEMICS = registerBase("HistorysWorseEpidemics", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book HITCHHIKING_THROUGH_CANADA = registerBase("HitchhikingThroughCanada", CoverType.SOFTCOVER);
    public static final Book HOLDING = registerBase("Holding", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book HOLDING_CELLS = registerBase("HoldingCells", CoverType.SOFTCOVER, BookSubject.POLICING);
    public static final Book HOLDING_HANDS_AND_KISSING_A_SIMPLE_GUIDE = registerBase(
        "HoldingHandsandKissingASimpleGuide", CoverType.SOFTCOVER, BookSubject.TEENS
    );
    public static final Book HOLLYWOOD_HAIRSTYLE_MANUAL = registerBase("HollywoodHairstyleManual", CoverType.BOTH, BookSubject.FASHION);
    public static final Book HOLLYWOOD_MAKEUP = registerBase("HollywoodMakeup", CoverType.BOTH, BookSubject.FASHION);
    public static final Book HOLY_WATER_GODS_MEDICINE = registerBase("HolyWaterGodsMedicine", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book HOME_DECORATION = registerBase("HomeDecoration", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book HOOPING_FOR_LIFE = registerBase("HoopingforLife", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book HORSE_RACING_JOURNAL = registerBase("HorseRacingJournal", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book HORSES_TO_WATCH_IN_93 = registerBase("HorsestoWatchin93", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book HOSTILE_TAKEOVER = registerBase("HostileTakeover", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book HOT_AND_COLD_A_YOUNG_PERSONS_GUIDE_TO_LOVE = registerBase(
        "HotandColdAYoungPersonsGuidetoLove", CoverType.SOFTCOVER, BookSubject.TEENS
    );
    public static final Book HOTTIE_Z_THE_INSIDE_STORY = registerBase("HottieZTheInsideStory", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book HOW_AN_ANGEL_FIXED_MY_SMILE = registerBase("HowanAngelFixedMySmile", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book HOW_COMPUTER_GAMES_SHAPE_TOMORROWS_SOLDIERS = registerBase(
        "HowComputerGamesShapeTomorrowsSoldiers", CoverType.SOFTCOVER, BookSubject.MILITARY
    );
    public static final Book HOW_GOD_CREATED_LIFE = registerBase("HowGodCreatedLife", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book HOW_I_BAGGED_THE_PERFECT_HUSBAND = registerBase("HowIBaggedThePerfectHusband", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book HOW_PHYSICS_CONTROLS_OUR_LIVES = registerBase("HowPhysicsControlsOurLives", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book HOW_ROCK_N_ROLL_TOOK_OVER_THE_WORLD = registerBase("HowRocknRollTookOvertheWorld", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book HOW_THE_BOJANGLES_WERE_MADE = registerBase("HowTheBojanglesWereMade", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.MUSIC);
    public static final Book HOW_THE_FEMALE_BRAIN_WORKS = registerBase("HowtheFemaleBrainWorks", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book HOW_THE_RAILROADS_SHAPED_AMERICA = registerBase(
        "HowTheRailroadsShapedAmerica", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY
    );
    public static final Book HOW_TO_BE_GOOD_AND_NICE = registerBase("HowToBeGoodandNice", CoverType.SOFTCOVER, BookSubject.SELF_HELP);
    public static final Book HOW_TO_BE_SINGLE = registerBase("HowtoBeSingle", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP);
    public static final Book HOW_TO_BREAK_EMPLOYEES_SECRETS_OF_A_MANAGER = registerBase(
        "HowToBreakEmployeesSecretsofaManager", CoverType.HARDCOVER, BookSubject.BUSINESS
    );
    public static final Book HOW_TO_CRUSH_YOUR_ENEMIES = registerBase("HowToCrushYourEnemies", CoverType.SOFTCOVER, BookSubject.BUSINESS, BookSubject.SELF_HELP);
    public static final Book HOW_TO_FIX_AMERICA = registerBase("HowtoFixAmerica", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book HOW_TO_SPOT_A_CHEATER = registerBase("HowtoSpotaCheater", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF);
    public static final Book HOW_TO_STOP_A_WIFE_FROM_LEAVING = registerBase("HowToStopaWifefromLeaving", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book HOW_TO_SURVIVE_A_TIGER_ATTACK = registerBase("HowtoSurviveaTigerAttack", CoverType.BOTH, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book HOW_WE_DO_IT = registerBase("HowWeDoIt", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book HOWLING_WOMAN_HOW_SCREAMING_CURED_ME = registerBase(
        "HowlingWomanHowScreamingCuredMe", CoverType.BOTH, BookSubject.SELF_HELP, BookSubject.NEW_AGE, BookSubject.QUACKERY
    );
    public static final Book HUDDLED_MASSES = registerBase(
        "HuddledMasses", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book HUMANS_A_HISTORY_OF_SELFISHNESS = registerBase("HumansAHistoryOfSelfishness", CoverType.BOTH, BookSubject.SCIENCE);
    public static final Book HUNT_FOR_THE_LAKESIDE_STRANGLER = registerBase("HuntfortheLakesideStrangler", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book HURRY_UP_GOD_AT = registerBase(
        "HurryUpGodat", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book HYPNOTISM_FOR_ANYONE = registerBase("HypnotismForAnyone", CoverType.SOFTCOVER, BookSubject.NEW_AGE, BookSubject.QUACKERY);
    public static final Book I_CHING = registerBase("IChing", CoverType.HARDCOVER, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION);
    public static final Book I_REMEMBER = registerBase("IRemember", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book I_THINK_I_KNOW_DO_YOU = registerBase("IThinkIKnowDoYou", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book ILL_NEVER_FORGET_YOU_MOM = registerBase("IllNeverForgetYouMom", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book ILLEGAL_UNBOWED = registerBase("IllegalUnbowed", CoverType.BOTH, BookSubject.SAD_NON_FICTION);
    public static final Book IM_NOT_A_SHECRET_AGENT = registerBase("ImNotaShecretAgent", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.CINEMA);
    public static final Book IMPOSSIBLE_CHOICE = registerBase("ImpossibleChoice", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book IN_THE_ACT = registerBase("IntheAct", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book INCREDIBLE_VACATION_SPOTS = registerBase("IncredibleVacationSpots", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book INDISCRETION = registerBase(
        "Indiscretion", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book INNOVATIONS_IN_TECHNOLOGY = registerBase("InnovationsinTechnology", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book INSIDE_THE_BONDI_BOYS_OUTSIDE_THE_LAW = registerBase(
        "InsidetheBondiBoysOutsidetheLaw", CoverType.SOFTCOVER, BookSubject.BIOGRAPHY, BookSubject.TRUE_CRIME
    );
    public static final Book INSIDER_TRADING_A_SHOCKING_TALE_OF_CORRUPTION = registerBase(
        "InsiderTradingAShockingTaleofCorruption", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book INSURANCE_AND_LIABILITY = registerBase(
        "InsuranceandLiability", CoverType.BOTH, BookSubject.MEDICAL, BookSubject.LEGAL, BookSubject.BUSINESS
    );
    public static final Book INVASION_OF_THE_BLUEHEADS = registerBase("InvasionoftheBlueheads", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book IS_THAT_MY_CUE = registerBase("IsThatMyCue", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book ISOLATION = registerBase("Isolation", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book ISTANBUL_TO_CAIRO = registerBase("IstanbultoCairo", CoverType.HARDCOVER, BookSubject.TRAVEL);
    public static final Book ITALIA_90_COMPANION = registerBase("Italia90Companion", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book JACK_THE_RIPPER_A_NEW_THEORY = registerBase("JacktheRipperANewTheory", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book JACKIE_O_IN_HER_WORDS = registerBase("JackieOInHerWords", CoverType.BOTH, BookSubject.BIOGRAPHY);
    public static final Book JANE_EYRE = registerBase("JaneEyre", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book JAPAN_A_NEW_HISTORY = registerBase("JapanANewHistory", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book JEFFREY_THE_FORGOTTEN_ACORN = registerBase("JeffreyTheForgottenAcorn", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book JESSE_JAMES_AND_HIS_GANG = registerBase("JesseJamesandHisGang", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book JFK_FROM_HARVARD_TO_DALLAS = registerBase(
        "JFKFromHarvardtoDallas", CoverType.HARDCOVER, BookSubject.BIOGRAPHY, BookSubject.HISTORY
    );
    public static final Book JOURNEY_THROUGH_THE_EARTHS_CRUST = registerBase(
        "JourneyThroughtheEarthsCrust", CoverType.SOFTCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.SCIENCE
    );
    public static final Book JUNGLES_AND_TUNDRA = registerBase("JunglesandTundra", CoverType.SOFTCOVER);
    public static final Book JUST_A_GAME = registerBase("JustaGame", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book JUST_BREATHE_A_MEDITATION_GUIDE = registerBase(
        "JustBreatheAMeditationGuide", CoverType.SOFTCOVER, BookSubject.SELF_HELP, BookSubject.NEW_AGE
    );
    public static final Book JUST_ONE_LAST_THING = registerBase("JustOneLastThing", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book JUST_STOP_EATING = registerBase("JustStopEating", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book JX359 = registerBase("JX359", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book KAHLO = registerBase("Kahlo", CoverType.BOTH);
    public static final Book KENTUCKY_BY_BICYCLE = registerBase("KentuckybyBicycle", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book KENTUCKY_FOOTBALL_ANNUAL_1992 = registerBase("KentuckyFootballAnnual1992", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book KENTUCKY_IN_THE_OLD_DAYS = registerBase("KentuckyintheOldDays", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book KENTUCKY_SPORTS_BIBLE = registerBase("KentuckySportsBible", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book KENTUCKY_STATE_BASEBALL_1990 = registerBase(
        "KentuckyStateBaseball1990", CoverType.HARDCOVER, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book KENTUCKY_STATE_BASEBALL_1991 = registerBase(
        "KentuckyStateBaseball1991", CoverType.HARDCOVER, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book KENTUCKY_STATE_BASEBALL_1992 = registerBase(
        "KentuckyStateBaseball1992", CoverType.HARDCOVER, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book KENTUCKY_STATE_SENATORS = registerBase("KentuckyStateSenators", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book KIDS_WHO_HELPED_THE_WORLD = registerBase("KidsWhoHelpedTheWorld", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book KING_LEAR = registerBase("KingLear", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book KIRRUS_COMPONENT_HELPER = registerBase("KirrusComponentHelper", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book KISS_HUG_DIE = registerBase("KissHugDie", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book KNOCKED_OUT_AND_FORGOTTEN = registerBase("KnockedOutandForgotten", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book KNOW_YOUR_RIGHTS = registerBase("KnowYourRights", CoverType.BOTH, BookSubject.LEGAL, BookSubject.POLITICS);
    public static final Book KNOX_HEIGHTS_COUNTRY_CLUB_A_HISTORY = registerBase(
        "KnoxHeightsCountryClubAHistory", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book KNOX_HEIGHTS_COUNTRY_CLUB_YEARBOOK_1991 = registerBase(
        "KnoxHeightsCountryClubYearbook1991", CoverType.HARDCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book KNOX_HEIGHTS_COUNTRY_CLUB_YEARBOOK_1992 = registerBase(
        "KnoxHeightsCountryClubYearbook1992", CoverType.HARDCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book KNOXS_BIKER_GANGS_UNDERCOVER = registerBase("KnoxsBikerGangsUndercover", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book KNOXS_HIDDEN_DRUG_LABS = registerBase("KnoxsHiddenDrugLabs", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book KUBLA_KHAN_AND_OTHER_WORKS = registerBase(
        "KublaKhanandOtherWorks", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book LA_BOHEME = registerBase("Laboheme", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book LAKOTA_ROMANCE_A_TRAGIC_LOVE_STORY = registerBase("LakotaRomanceATragicLoveStory", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book LASERS_AND_LIGHT = registerBase("LasersandLight", CoverType.HARDCOVER, BookSubject.SCIENCE);
    public static final Book LAVA_AND_MAGMA_DONT_TOUCH_THEM = registerBase("LavaandMagmaDontTouchThem", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book LBJ_TEXAN_TITAN = registerBase("LBJTexanTitan", CoverType.HARDCOVER, BookSubject.BIOGRAPHY, BookSubject.HISTORY);
    public static final Book LEANNA = registerBase("Leanna", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book LEARNING_BASEBALL = registerBase(
        "LearningBaseball", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.BASEBALL, BookSubject.CHILDS
    );
    public static final Book LEATHER_AND_CHAINS = registerBase("LeatherandChains", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book LEAVES_OF_GRASS = registerBase("LeavesofGrass", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book LEGAL_WRITING_GUIDE = registerBase("LegalWritingGuide", CoverType.HARDCOVER, BookSubject.LEGAL);
    public static final Book LEGALITIES_OF_TAXATION = registerBase("LegalitiesofTaxation", CoverType.BOTH, BookSubject.LEGAL, BookSubject.BUSINESS);
    public static final Book LEGENDS_OF_FORMULA_X = registerBase("LegendsofFormulaX", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book LESTER_THOMPSONS_GUIDE_TO_LIFE = registerBase(
        "LesterThompsonsGuidetoLife", CoverType.SOFTCOVER, BookSubject.TEENS, BookSubject.CHILDS
    );
    public static final Book LETS_GET_ONE_THING_STRAIGHT = registerBase("LetsGetOneThingStraight", CoverType.SOFTCOVER, BookSubject.HASS);
    public static final Book LEVIATHAN = registerBase(
        "Leviathan", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book LIBERAL_TOTALITARIANISM = registerBase("LiberalTotalitarianism", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book LIES_OF_THE_RIGHT = registerBase("LiesoftheRight", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book LIFE_AMONG_THE_STARS = registerBase("LifeAmongtheStars", CoverType.BOTH, BookSubject.BIOGRAPHY);
    public static final Book LIFE_AND_DEATH_IN_THE_POSTBELLUM_SOUTH = registerBase("LifeandDeathinthePostbellumSouth", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book LIFE_BEFORE_MAN = registerBase(
        "LifeBeforeMan", CoverType.SOFTCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.SCIENCE, BookSubject.HISTORY, BookSubject.NATURE
    );
    public static final Book LIFE_GOALS = registerBase("LifeGoals", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book LIFE_IN_THE_ARCTIC_CIRCLE = registerBase("LifeintheArcticCircle", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book LIFE_IN_THE_LAKES = registerBase("LifeintheLakes", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book LIFE_IN_THE_MIDDLE = registerBase("LifeintheMiddle", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book LIFE_IN_THE_SCREECHING_CONDORS = registerBase("LifeintheScreechingCondors", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book LIFE_ON_THE_COURT = registerBase("LifeontheCourt", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book LIFE_ON_THE_GREEN_TABLE = registerBase("LifeOntheGreenTable", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book LIFE_UNDER_BRIGHTON_PIER = registerBase("LifeUnderBrightonPier", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book LIFE_UNDER_THE_SEA = registerBase("LifeUndertheSea", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book LINCOLN_THE_KENTUCKY_PRESIDENT = registerBase(
        "LincolnTheKentuckyPresident", CoverType.SOFTCOVER, BookSubject.POLITICS, BookSubject.HISTORY
    );
    public static final Book LINE_GO_UP_HOW_INSIDER_TRADING_DESTROYED_THE_MARKET = registerBase(
        "LineGoUpHowInsiderTradingDestroyedtheMarket", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book LIPSTICK_DONE_RIGHT = registerBase("LipstickDoneRight", CoverType.BOTH, BookSubject.FASHION);
    public static final Book LITTLE_WOMEN = registerBase("LittleWomen", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS);
    public static final Book LIVES_OF_THE_APOSTLES = registerBase("LivesoftheApostles", CoverType.SOFTCOVER, BookSubject.HISTORY, BookSubject.RELIGION);
    public static final Book LIVES_OF_THE_FOUNDING_FATHERS = registerBase(
        "LivesoftheFoundingFathers", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.HISTORY
    );
    public static final Book LIVING_WITH_FEAR_MY_STORY = registerBase("LivingWithFearMyStory", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book LOCKED_FROM_THE_INSIDE = registerBase("LockedFromtheInside", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book LOGISTICS_WHERE_HOW_AND_WHY = registerBase("LogisticsWhereHowandWhy", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book LONG_TERM_ILLNESS_MANAGEMENT = registerBase("LongTermIllnessManagement", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book LOOKING_BACKWARD_2000_1887 = registerBase(
        "LookingBackward20001887", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book LOOKING_FOR_THE_LIGHT = registerBase("LookingfortheLight", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book LOS_ANGELESS_INSANE_WORLD_OF_MUSIC = registerBase("LosAngelessInsaneWorldofMusic", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book LOSE_THE_POUNDS = registerBase("LosethePounds", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book LOST_AT_SEA_HOW_I_LIVED = registerBase("LostatSeaHowILived", CoverType.BOTH, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book LOUISVILLE_IN_THE_NINETEENTH_CENTURY = registerBase(
        "LouisvilleintheNineteenthCentury", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY
    );
    public static final Book LOUISVILLE_LIGHTS_ALIENS_OR_MASS_HYSTERIA = registerBase(
        "LouisvilleLightsAliensorMassHysteria", CoverType.BOTH, BookSubject.OCCULT, BookSubject.CONSPIRACY
    );
    public static final Book LOUISVILLE_SINGLES_DIRECTORY_JULY_93 = registerBase(
        "LouisvilleSinglesDirectoryJuly93", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP
    );
    public static final Book LOUISVILLE_SINGLES_DIRECTORY_JUNE_93 = registerBase(
        "LouisvilleSinglesDirectoryJune93", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP
    );
    public static final Book LOUISVILLE_SINGLES_DIRECTORY_MAY_93 = registerBase(
        "LouisvilleSinglesDirectoryMay93", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP
    );
    public static final Book LOVE_ALL = registerBase("LoveAll", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book LOVE_AT_LAST = registerBase("LoveatLast", CoverType.BOTH, BookSubject.ROMANCE);
    public static final Book LOVE_RHOMBUS = registerBase("LoveRhombus", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book LOVELY_DOGS = registerBase("LovelyDogs", CoverType.HARDCOVER, BookSubject.NATURE);
    public static final Book MACBETH = registerBase("Macbeth", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book MADE_MAN_REAL_LIFE_IN_THE_MAFIA = registerBase("MadeManRealLifeintheMafia", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book MAGICAL_WOODLAND_A_SHOCKING_INVESTIGATION = registerBase(
        "MagicalWoodlandAShockingInvestigation", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME
    );
    public static final Book MAGICAL_WOODLAND_BORIS_THE_BADGER = registerBase(
        "MagicalWoodlandBoristheBadger", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_FLUFFYFOOT_THE_RABBIT = registerBase(
        "MagicalWoodlandFluffyfoottheRabbit", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_FREDDY_THE_FOX = registerBase(
        "MagicalWoodlandFreddytheFox", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_FURBERT_THE_SQUIRREL = registerBase(
        "MagicalWoodlandFurberttheSquirrel", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_JACQUES_THE_BEAVER = registerBase(
        "MagicalWoodlandJacquestheBeaver", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_MOLEY_THE_MOLE = registerBase(
        "MagicalWoodlandMoleytheMole", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_PANCAKE_THE_HEDGEHOG = registerBase(
        "MagicalWoodlandPancaketheHedgehog", CoverType.HARDCOVER, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book MAGICAL_WOODLAND_SPIFFO_THE_RACCOON = registerBase("MagicalWoodlandSpiffotheRaccoon", CoverType.HARDCOVER);
    public static final Book MAGICIAN_OF_OCCIA = registerBase("MagicianofOccia", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book MAGICK_RITUALS_AND_DAEMONS = registerBase("MagickRitualsandDaemons", CoverType.HARDCOVER, BookSubject.OCCULT);
    public static final Book MAINTAINING_MORALE = registerBase("MaintainingMorale", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book MAKE_UP_GUIDE_FOR_GIRLS = registerBase("MakeUpGuideForGirls", CoverType.SOFTCOVER, BookSubject.TEENS, BookSubject.FASHION);
    public static final Book MAKING_HEADLINES = registerBase("MakingHeadlines", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book MAKING_HONEY_LESSONS_FROM_BEES = registerBase("MakingHoneyLessonsFromBees", CoverType.SOFTCOVER, BookSubject.SELF_HELP);
    public static final Book MAMMALS_OF_EUROPE = registerBase("MammalsofEurope", CoverType.HARDCOVER, BookSubject.NATURE);
    public static final Book MANAGING_EXPECTATIONS_AND_BREAKING_BAD_NEWS = registerBase(
        "ManagingExpectationsandBreakingBadNews", CoverType.HARDCOVER, BookSubject.MEDICAL
    );
    public static final Book MARISAS_DILEMMA = registerBase("MarisasDilemma", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book MARROW_OF_THE_GODS = registerBase("MarrowoftheGods", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book MATH_AND_MADNESS = registerBase("MathandMadness", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.SCIENCE);
    public static final Book MATTS_MATH_GUIDE_FOR_TEENS = registerBase("MattsMathGuideForTeens", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book MEDIEVAL_MONASTERY_MURDER_A_TRUE_STORY = registerBase(
        "MedievalMonasteryMurderATrueStory", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME
    );
    public static final Book MEDITATIONS = registerBase("Meditations", CoverType.BOTH, BookSubject.PHILOSOPHY, BookSubject.CLASSIC);
    public static final Book MENS_HAIR_DONE_RIGHT = registerBase("MensHairDoneRight", CoverType.BOTH, BookSubject.FASHION);
    public static final Book MENTAL_HEALTH_MANAGEMENT = registerBase("MentalHealthManagement", CoverType.HARDCOVER, BookSubject.MEDICAL);
    public static final Book MICAHS_ALMANAC_1990 = registerBase("MicahsAlmanac1990", CoverType.HARDCOVER, BookSubject.FARMING);
    public static final Book MICAHS_ALMANAC_1991 = registerBase("MicahsAlmanac1991", CoverType.HARDCOVER, BookSubject.FARMING);
    public static final Book MICAHS_ALMANAC_1992 = registerBase("MicahsAlmanac1992", CoverType.HARDCOVER, BookSubject.FARMING);
    public static final Book MIDDLE_CLASS_VILLAINS = registerBase(
        "MiddleClassVillains", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book MIDDLEMARCH = registerBase("Middlemarch", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book MIND_FIRE_TRUE_STORIES_FROM_A_PYROKINETIC = registerBase(
        "MindFireTrueStoriesFromaPyrokinetic", CoverType.SOFTCOVER, BookSubject.OCCULT
    );
    public static final Book MIND_FIRST_BODY_SECOND = registerBase(
        "MindFirstBodySecond", CoverType.SOFTCOVER, BookSubject.SELF_HELP, BookSubject.NEW_AGE, BookSubject.QUACKERY
    );
    public static final Book MINE_COUNTY_MARTINS = registerBase("MineCountyMartins", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book MINUTES_FROM_ARMAGEDDON_THE_CUBAN_MISSILE_CRISIS = registerBase(
        "MinutesFromArmageddonTheCubanMissileCrisis", CoverType.BOTH, BookSubject.HISTORY
    );
    public static final Book MISS_NEWSOMS_SCHOOL_FOR_BOLD_WITCHES = registerBase("MissNewsomsSchoolForBoldWitches", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book MOBY_DICK = registerBase("MobyDick", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book MONICA = registerBase("Monica", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book MONTANA = registerBase("Montana", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book MOO_QUACK_WOOF = registerBase("MooQuackWoof", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book MOON_MADNESS_THE_JERRY_JONES_STORY = registerBase(
        "MoonMadnessTheJerryJonesStory", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.MUSIC
    );
    public static final Book MOST_EVIL_PEOPLE_IN_HISTORY = registerBase("MostEvilPeopleinHistory", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book MOTHER_OF_SOLDIERS = registerBase(
        "MotherofSoldiers", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book MOTHERS_BOY = registerBase("MothersBoy", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book MRS_DALLOWAY = registerBase("MrsDalloway", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book MRS_MUSCLES = registerBase("MrsMuscles", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book MULHOLLAND_MURDER = registerBase("MulhollandMurder", CoverType.SOFTCOVER);
    public static final Book MULLETS_AND_CURTAINS_A_COOL_DUDES_HAIR_GUIDE = registerBase(
        "MulletsandCurtainsACoolDudesHairGuide", CoverType.SOFTCOVER, BookSubject.TEENS
    );
    public static final Book MUSIC_REVOLUTION = registerBase("MusicRevolution", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book MUSICAL_POETRY_SOME_TIPS_FOR_LYRICS = registerBase("MusicalPoetrySomeTipsforLyrics", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book MY_FIRST_ATLAS = registerBase("MyFirstAtlas", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book MY_FIRST_BICYCLE = registerBase("MyFirstBicycle", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book MY_FIRST_PET = registerBase("MyFirstPet", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book MY_MOTHERS_APRON = registerBase("MyMothersApron", CoverType.SOFTCOVER);
    public static final Book MY_ONLY_GOAL_IS_TO_SAVE_YOUR_SOUL = registerBase(
        "MyOnlyGoalistoSaveYourSoul", CoverType.SOFTCOVER, BookSubject.QUIGLEY, BookSubject.CONSPIRACY
    );
    public static final Book MY_OUTBACK_ADVENTURES = registerBase("MyOutbackAdventures", CoverType.SOFTCOVER, BookSubject.TRAVEL);
    public static final Book MY_SPECIAL_BABY = registerBase("MySpecialBaby", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book MY_SWEET_BOY = registerBase("MySweetBoy", CoverType.BOTH, BookSubject.SAD_NON_FICTION);
    public static final Book MY_VISION_FOR_KENTUCKY = registerBase("MyVisionforKentucky", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book MYSTERY_OF_THE_PINEFALL_MINERS = registerBase("MysteryofthePinefallMiners", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book NAIL_POLISH_DONE_RIGHT = registerBase("NailPolishDoneRight", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book NANNY_BUNNYS_BEDTIME_TALES = registerBase("NannyBunnysBedtimeTales", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book NANOMACHINES_DANGER_OF_THE_COMING_CENTURY = registerBase(
        "NanomachinesDangeroftheComingCentury", CoverType.SOFTCOVER, BookSubject.CONSPIRACY
    );
    public static final Book NAPOLEONS_ENGLAND = registerBase("NapoleonsEngland", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book NATIONAL_NEWS = registerBase("NationalNews", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book NEAR_DEATH_NEW_PERSPECTIVES = registerBase(
        "NearDeathNewPerspectives", CoverType.BOTH, BookSubject.SELF_HELP, BookSubject.NEW_AGE, BookSubject.RELIGION
    );
    public static final Book NECROMANCY_THE_TRUE_ART_OF_RAISING_THE_DEAD = registerBase(
        "NecromancyTheTrueArtofRaisingtheDead", CoverType.BOTH, BookSubject.OCCULT
    );
    public static final Book NESTING = registerBase("Nesting", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book NET_US_HOW_THE_INTERNET_WILL_REVOLUTIONISE_DEMOCRACY = registerBase(
        "NetUSHowtheInternetWillRevolutioniseDemocracy", CoverType.SOFTCOVER, BookSubject.POLITICS
    );
    public static final Book NEUROLOGY = registerBase("Neurology", CoverType.HARDCOVER, BookSubject.MEDICAL);
    public static final Book NEW_ATLANTIS = registerBase("NewAtlantis", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book NEW_VOYAGES_TO_NORTH_AMERICA = registerBase(
        "NewVoyagestoNorthAmerica", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book NICE_CATS = registerBase("NiceCats", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book NICK_DEATHKNELL_TEXAS_RANGER = registerBase("NickDeathknellTexasRanger", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book NIGHTFIRE = registerBase("Nightfire", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book NINE_NINE_NINE_DAILY_HABITS_OF_SUCCESSFUL_BUSINESSMEN = registerBase(
        "999DailyHabitsofSuccessfulBusinessmen", CoverType.HARDCOVER, BookSubject.BUSINESS
    );
    public static final Book NINETEEN_AUGHT_EIGHT = registerBase("NineteenAughtEight", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book NINETY_TWO_A_SEASON_FROM_HELL = registerBase("92ASeasonFromHell", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book NIXON_AND_WATERGATE = registerBase("NixonandWatergate", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.HISTORY);
    public static final Book NO_HURDLE_TOO_HIGH = registerBase("NoHurdleTooHigh", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book NOLANS_GUIDE_TO_PHOTOGRAPHY = registerBase("NolansGuidetoPhotography", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book NOON_SHOWDOWN = registerBase("NoonShowdown", CoverType.BOTH, BookSubject.WESTERN);
    public static final Book NORTH_V_SOUTH_THE_CIVIL_WAR = registerBase(
        "NorthvSouthTheCivilWar", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book NOSY_NATE = registerBase("NosyNate", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book NOSY_NATE_AT_THE_HOSPITAL = registerBase("NosyNateAttheHospital", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book NOSY_NATE_VISITS_THE_ZOO = registerBase("NosyNateVisitstheZoo", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book NOTHING_TO_LOSE_BUT_OUR_CHAINS = registerBase("NothingtoLoseButOurChains", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book NUCLEAR_WEAPONS_RELICS_OF_HISTORY = registerBase(
        "NuclearWeaponsRelicsofHistory", CoverType.SOFTCOVER, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book NUDITY_AN_ARTISTIC_GUIDE = registerBase("NudityAnArtisticGuide", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book NUMBER_ONE_FAN = registerBase("NumberOneFan", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book OEDIPUS_REX = registerBase("OedipusRex", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book OFFICE_EFFICIENCY = registerBase("OfficeEfficiency", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book OFFICER_MENTAL_HEALTH_GUIDE = registerBase("OfficerMentalHealthGuide", CoverType.HARDCOVER, BookSubject.POLICING);
    public static final Book OH_THE_THINGS_YOULL_DO = registerBase("OhTheThingsYoullDo", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book OIL_OF_LIES_THE_GULF_OF_ALASKA_SPILL = registerBase("OilofLiesTheGulfofAlaskaSpill", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book OLD_AGE_AND_SENILITY = registerBase("OldAgeandSenility", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book OLD_NEW_YORK = registerBase("OldNewYork", CoverType.SOFTCOVER, BookSubject.HISTORY);
    public static final Book OLIVER_TWIST = registerBase("OliverTwist", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book ON_THE_ORIGIN_OF_SPECIES = registerBase(
        "OntheOriginofSpecies", CoverType.HARDCOVER, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book ON_THE_TRAIL_OF_JUAN_SANCHEZ = registerBase("OntheTrailofJuanSanchez", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book ON_TOUR_WITH_JOE_JOHNSON = registerBase("OnTourWithJoeJohnson", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book ONE_FRAME_AT_A_TIME = registerBase("OneFrameataTime", CoverType.SOFTCOVER, BookSubject.CINEMA);
    public static final Book ONE_O_ONE_BEAUTIFUL_OUTFIT_IDEAS = registerBase("101BeautifulOutfitIdeas", CoverType.BOTH, BookSubject.FASHION);
    public static final Book ONE_O_ONE_MORE_BEAUTIFUL_OUTFIT_IDEAS = registerBase("101MOREBeautifulOutfitIdeas", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book ONE_SMALL_STEP_THE_APOLLO_STORY = registerBase(
        "OneSmallStepTheApolloStory", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY
    );
    public static final Book ONE_STREET_AT_A_TIME = registerBase("OneStreetAtaTime", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book OOH_MATRON_A_TRUE_STORY_OF_LUST = registerBase("OohMatronATrueStoryofLust", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book OPERATION_FORT_KNOX = registerBase("OperationFortKnox", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book OPTICAL_ILLUSIONS = registerBase("OpticalIllusions", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book ORATIONS_OF_CICERO = registerBase("OrationsofCicero", CoverType.SOFTCOVER, BookSubject.HISTORY, BookSubject.CLASSIC);
    public static final Book ORIGINS_OF_THE_MOON = registerBase("OriginsoftheMoon", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book OSCC_YEARBOOK_1991 = registerBase("OSCCYearbook1991", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book OSCC_YEARBOOK_1992 = registerBase("OSCCYearbook1992", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book OTHELLO = registerBase("Othello", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book OUR_FAILING_INFRASTRUCTURE = registerBase("OurFailingInfrastructure", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book OUR_FALSE_HISTORIES = registerBase("OurFalseHistories", CoverType.SOFTCOVER, BookSubject.HISTORY);
    public static final Book OUR_FRIEND_THE_MOON = registerBase("OurFriendtheMoon", CoverType.SOFTCOVER, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book OUR_GREEN_EARTH = registerBase(
        "OurGreenEarth", CoverType.SOFTCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.SCIENCE, BookSubject.NATURE
    );
    public static final Book OUR_LIBERAL_FOUNDING_FATHERS = registerBase("OurLiberalFoundingFathers", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book OUR_LORD_CELEBRITY = registerBase(
        "OurLordCelebrity", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book OUR_WAY_A_NEW_RELIGION_COMMUNITY_YOU_CAN_JOIN = registerBase(
        "OurWayANewreligionCommunityYOUCanJoin", CoverType.SOFTCOVER, BookSubject.QUIGLEY, BookSubject.CONSPIRACY
    );
    public static final Book OUT_OF_THE_AIRLOCK = registerBase("OutoftheAirlock", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book OVER_THE_ABYSS = registerBase("OvertheAbyss", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book OZYMANDIAS_AND_OTHER_POEMS = registerBase(
        "OzymandiasandOtherPoems", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book P_PROGRAMMING_FOR_BEGINNERS = registerBase(
        "PProgrammingForBeginners", CoverType.SOFTCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.COMPUTER
    );
    public static final Book PAINTING_FOR_ANYONE = registerBase("PaintingforAnyone", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book PAINTING_WITH_OILS = registerBase("PaintingwithOils", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book PAINTING_WITH_WATERCOLORS = registerBase("PaintingwithWatercolors", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book PANSY_BOLTON = registerBase("PansyBolton", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book PAPA_ALPHA_TANGO = registerBase("PapaAlphaTango", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION, BookSubject.MILITARY);
    public static final Book PARIS_RUNWAY_DIARIES = registerBase("ParisRunwayDiaries", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book PATHOLOGY_OF_COMMON_VIRUSES = registerBase("PathologyofCommonViruses", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book PATHS_TO_FREEDOM_THE_UNDERGROUND_RAILROAD = registerBase(
        "PathstoFreedomTheUndergroundRailroad", CoverType.BOTH, BookSubject.HISTORY
    );
    public static final Book PATIENT_CARE = registerBase("PatientCare", CoverType.HARDCOVER, BookSubject.MEDICAL);
    public static final Book PAULA_AND_THE_PANDA = registerBase("PaulaandthePanda", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book PEDIATRICS = registerBase("Pediatrics", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book PENCIL_DRAWING_GUIDE = registerBase("PencilDrawingGuide", CoverType.BOTH, BookSubject.ART);
    public static final Book PETER_PAN = registerBase("PeterPan", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS);
    public static final Book PHILOSOPHIES_OF_THE_OLD_WORLD = registerBase(
        "PhilosophiesoftheOldWorld", CoverType.SOFTCOVER, BookSubject.PHILOSOPHY, BookSubject.HISTORY, BookSubject.RELIGION
    );
    public static final Book PHOTO_FINISH = registerBase("PhotoFinish", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book PHOTOFIT = registerBase("Photofit", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book PHOTOGRAPHING_THE_HUMAN_BODY = registerBase("PhotographingtheHumanBody", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book PIANO_FOR_BEGINNERS = registerBase("PianoforBeginners", CoverType.HARDCOVER, BookSubject.MUSIC);
    public static final Book PICASSO_AND_CUBISM = registerBase("PicassoandCubism", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book PINNED_DOWN = registerBase("PinnedDown", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book PISTOLS_AT_MIDNIGHT = registerBase("PistolsatMidnight", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book PITCHING_OUT = registerBase("PitchingOut", CoverType.SOFTCOVER, BookSubject.BASEBALL, BookSubject.SPORTS);
    public static final Book PLANET_OF_DREAMS = registerBase("PlanetofDreams", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book PLANET_OF_THE_DAFFODILS = registerBase("PlanetoftheDaffodils", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book PLASTIC_SURGERY = registerBase("PlasticSurgery", CoverType.HARDCOVER, BookSubject.MEDICAL);
    public static final Book PLEISTOCENE_LAND = registerBase("PleistoceneLand", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book POKER_FOR_BEGINNERS = registerBase("PokerforBeginners", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book POKER_TIPS_STRAIGHT_FROM_THE_STRIP = registerBase("PokerTipsStraightfromtheStrip", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book POLICE_REFORMS = registerBase("PoliceReforms", CoverType.BOTH, BookSubject.POLICING);
    public static final Book POLICING_AND_THE_CONSTITUTION = registerBase(
        "PolicingandtheConstitution", CoverType.SOFTCOVER, BookSubject.POLICING, BookSubject.LEGAL
    );
    public static final Book POLICING_MODERN_AMERICA = registerBase("PolicingModernAmerica", CoverType.BOTH, BookSubject.POLICING);
    public static final Book POLITICS = registerBase("Politics", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION);
    public static final Book POLLS_AND_COLLEGES_HOW_ELECTIONS_WORK = registerBase("PollsandCollegesHowElectionsWork", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book POST_IMPRESSIONISM_A_PRIMER = registerBase("PostImpressionismAPrimer", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book PRAISE_THROUGH_SONG = registerBase("PraiseThroughSong", CoverType.SOFTCOVER, BookSubject.MUSIC, BookSubject.RELIGION);
    public static final Book PREGNANCY_AND_NEWBORN_BABY_GUIDE = registerBase("PregnancyandNewbornBabyGuide", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book PREGNANCY_CARE = registerBase("PregnancyCare", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book PREPARING_FOR_THE_FUTURE = registerBase("PreparingFortheFuture", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book PRIDE_AND_PREJUDICE = registerBase(
        "PrideandPrejudice", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.ROMANCE, BookSubject.CLASSIC
    );
    public static final Book PRINCIPAL_DOCTRINES = registerBase(
        "PrincipalDoctrines", CoverType.HARDCOVER, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book PROM_OUTFITS_FOR_HIM_AND_HER = registerBase("PromOutfitsForHimandHer", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book PROPERTY_LAW_A_SHORT_GUIDE = registerBase("PropertyLawAShortGuide", CoverType.HARDCOVER, BookSubject.LEGAL, BookSubject.POLITICS);
    public static final Book PUCK_OFF_THE_LIFE_OF_A_HOCKEY_REBEL = registerBase("PuckOffTheLifeofaHockeyRebel", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book PUNCHING_ABOVE_MY_WEIGHT = registerBase("PunchingAboveMyWeight", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book QPR_IN_THEIR_OWN_WORDS = registerBase("QPRInTheirOwnWords", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book QUARTERBACK_FULL_LIFE = registerBase("QuarterbackFullLife", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book QUESTIONABLE_ETHICS_THE_RIVERSIDE_RANGERS_STORY = registerBase(
        "QuestionableEthicsTheRiversideRangersStory", CoverType.BOTH, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book RAILROADED = registerBase("Railroaded", CoverType.BOTH, BookSubject.WESTERN);
    public static final Book RALLYING_HANDBOOK = registerBase("RallyingHandbook", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book RAMPANT = registerBase("Rampant", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book RAPS_N_RHYMES = registerBase("RapsnRhymes", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book RAQUEL_ROGERS_THE_BLOND_BOMBSHELL = registerBase("RaquelRogersTheBlondBombshell", CoverType.SOFTCOVER, BookSubject.CINEMA);
    public static final Book REAGAN_ACTING_PRESIDENT = registerBase("ReaganActingPresident", CoverType.HARDCOVER, BookSubject.BIOGRAPHY, BookSubject.HISTORY);
    public static final Book REAL_LIFE_SHARK_ATTACKS = registerBase("RealLifeSharkAttacks", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book RED_EUROPE_A_NOVEL_FROM_ANOTHER_HISTORY = registerBase("RedEuropeANovelFromAnotherHistory", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book RED_RING_OF_FIRE_THE_COMING_PACIFIC_RIM_DISASTER = registerBase(
        "RedRingofFireTheComingPacificRimDisaster", CoverType.SOFTCOVER, BookSubject.CONSPIRACY
    );
    public static final Book RED_WHITE_AND_BLOWN_UP = registerBase("RedWhiteandBlownUp", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book REMBRANDT = registerBase("Rembrandt", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book REPORTING_CORRUPTION = registerBase("ReportingCorruption", CoverType.BOTH, BookSubject.POLICING);
    public static final Book REVELATIONS_PASTOR_FUTURE = registerBase("RevelationsPastorFuture", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book RHINO_HORN_A_CHINESE_CURE = registerBase("RhinoHornAChineseCure", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book RHODE_ISLAND_ROMANCE = registerBase("RhodeIslandRomance", CoverType.BOTH, BookSubject.ROMANCE);
    public static final Book RIGHTS_OF_MAN = registerBase("RightsofMan", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC);
    public static final Book RITUALS_OF_LUCIFER = registerBase("RitualsofLucifer", CoverType.HARDCOVER, BookSubject.OCCULT);
    public static final Book RIVERSIDE_RANGERS_A_HISTORY_OF_HEROES = registerBase(
        "RiversideRangersAHistoryofHeroes", CoverType.SOFTCOVER, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    public static final Book ROBINSON_CRUSOE = registerBase("RobinsonCrusoe", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book ROMA_INVICTA = registerBase("RomaInvicta", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book ROMEO_AND_JULIET = registerBase(
        "RomeoandJuliet", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.ROMANCE, BookSubject.CLASSIC
    );
    public static final Book ROUNDWORLD = registerBase("Roundworld", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book ROUTE_66 = registerBase("Route66", CoverType.HARDCOVER, BookSubject.TRAVEL);
    public static final Book RUDY_GRADY_THE_LOUISVILLE_LEGEND = registerBase("RudyGradyTheLouisvilleLegend", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book RUGBY_EUROPEAN_FOOTBALL = registerBase("RugbyEuropeanFootball", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book RUNNING_A_DATABASE = registerBase("RunningADatabase", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book RUNNING_OUT_OF_AIR = registerBase("RunningOutofAir", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book RUNNING_YOUR_TROUBLES_AWAY = registerBase(
        "RunningYourTroublesAway", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.SELF_HELP
    );
    public static final Book SADDAMS_FORCES = registerBase("SaddamsForces", CoverType.SOFTCOVER, BookSubject.MILITARY);
    public static final Book SAFARI = registerBase("Safari", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book SALOME = registerBase("Salome", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book SALOON_SHOWDOWN = registerBase("SaloonShowdown", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book SAMURAI_OF_SLAFTOP_X = registerBase("SamuraiofSlaftopX", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book SAND = registerBase("Sand", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book SATANS_PUPPETS_THE_TRUTH_BEHIND_THE_LARGEST_CHURCHES = registerBase(
        "SatansPuppetsTheTruthBehindtheLargestChurches", CoverType.SOFTCOVER, BookSubject.QUIGLEY, BookSubject.CONSPIRACY
    );
    public static final Book SAY_CAN_YOU_SEE = registerBase("SayCanYouSee", CoverType.SOFTCOVER, BookSubject.MILITARY);
    public static final Book SCENIC_VISTAS = registerBase("ScenicVistas", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book SCOPE = registerBase("Scope", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book SCOTTISH_TRAILS = registerBase("ScottishTrails", CoverType.HARDCOVER, BookSubject.TRAVEL);
    public static final Book SCREAM_QUEEN = registerBase("ScreamQueen", CoverType.SOFTCOVER, BookSubject.CINEMA);
    public static final Book SCULPTURE_FOR_ANYONE = registerBase("SculptureforAnyone", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book SEARCH_FOR_THE_ZODIAC = registerBase("SearchfortheZodiac", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book SEARCHING_FOR_EVIDENCE = registerBase("SearchingforEvidence", CoverType.BOTH, BookSubject.POLICING);
    public static final Book SECOND_AMENDMENT_FIRST_RIGHT = registerBase(
        "SecondAmendmentFirstRight", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS
    );
    public static final Book SECRETS_OF_A_PERFECT_MARRIAGE = registerBase("SecretsofaPerfectMarriage", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book SECRETS_OF_EVERYDAY_CLOTHES = registerBase("SecretsofEverydayClothes", CoverType.BOTH, BookSubject.FASHION);
    public static final Book SECRETS_OF_GOLF_COURSE_DESIGN = registerBase(
        "SecretsofGolfCourseDesign", CoverType.SOFTCOVER, BookSubject.SPORTS, BookSubject.GOLF
    );
    public static final Book SECRETS_OF_THE_RALEIGH_INCIDENT = registerBase("SecretsoftheRaleighIncident", CoverType.SOFTCOVER, BookSubject.CONSPIRACY);
    public static final Book SECRETS_OF_THE_SURGEONS = registerBase("SecretsoftheSurgeons", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book SECURING_THE_EVIDENCE = registerBase("SecuringtheEvidence", CoverType.BOTH, BookSubject.POLICING);
    public static final Book SEEING_THE_HOLY_SPIRIT = registerBase("SeeingtheHolySpirit", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book SEEING_THROUGH_STEREOTYPES = registerBase("SeeingThroughStereotypes", CoverType.HARDCOVER, BookSubject.POLICING);
    public static final Book SEEING_TO_THEIR_EVERY_NEED = registerBase("SeeingtoTheirEveryNeed", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book SELECT_BIBLE_PASSAGES = registerBase("SelectBiblePassages", CoverType.HARDCOVER, BookSubject.RELIGION);
    public static final Book SELF_RELIANCE_AND_OTHER_ESSAYS = registerBase("SelfRelianceandOtherEssays", CoverType.BOTH, BookSubject.CLASSIC);
    public static final Book SENSE_AND_SENSIBILITY = registerBase(
        "SenseandSensibility", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.ROMANCE, BookSubject.CLASSIC
    );
    public static final Book SENT_OFF_THE_STORY_OF_ROY_BUNN = registerBase("SentOffTheStoryofRoyBunn", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book SERMON_ON_THE_MOUNT = registerBase("SermonontheMount", CoverType.HARDCOVER, BookSubject.RELIGION);
    public static final Book SETTLERS_HO = registerBase("SettlersHo", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book SEVEN_CORPORATE_PRINCIPLES = registerBase("SevenCorporatePrinciples", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book SEVENTEEN_TIMES_HUMANITY_ALMOST_WENT_EXTINCT = registerBase(
        "17TimesHumanityAlmostWentEXTINCT", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION
    );
    public static final Book SEVERED_WIGS_THE_FRENCH_REVOLUTION = registerBase(
        "SeveredWigsTheFrenchRevolution", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book SEXUALLY_TRANSMITTED_DISEASES = registerBase("SexuallyTransmittedDiseases", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book SEXY_SECRETS_OF_THE_STARS = registerBase("SexySecretsoftheStars", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book SHELLFISH_OF_THE_ATLANTIC = registerBase("ShellfishoftheAtlantic", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book SHIPS_AND_GUNS = registerBase("ShipsandGuns", CoverType.HARDCOVER, BookSubject.MILITARY);
    public static final Book SHIPWRECKS_AND_COMMENTARIES = registerBase("ShipwrecksandCommentaries", CoverType.HARDCOVER, BookSubject.CLASSIC);
    public static final Book SILAS_MARNER = registerBase("SilasMarner", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book SING_AWAY_THE_WAR = registerBase(
        "SingAwaytheWar", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book SING_LIKE_THE_PROS = registerBase("SingLikethePros", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book SING_ONCE_MORE = registerBase(
        "SingOnceMore", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book SINGING_THROUGH_THE_PAIN = registerBase(
        "SingingThroughthePain", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.TEENS, BookSubject.SELF_HELP, BookSubject.MUSIC
    );
    public static final Book SINSILLA_THE_GREATEST_METAL_BAND_OF_ALL_TIME = registerBase(
        "SinsillaTheGreatestMetalBandofAllTime", CoverType.SOFTCOVER, BookSubject.BIOGRAPHY, BookSubject.MUSIC
    );
    public static final Book SIX_SHOOTER_AND_LITTLE_GREEN = registerBase("SixShooterandLittleGreen", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book SKATING_AND_GRINDING = registerBase("SkatingandGrinding", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book SLAM_DUNK = registerBase("SlamDunk", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book SLAVES_OF_ARNOK_DWARF_EMPIRE = registerBase("SlavesofArnokDwarfEmpire", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book SLOW_DESCENT = registerBase("SlowDescent", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book SMALL_BLESSINGS_LIFE_IN_A_CONVENT = registerBase("SmallBlessingsLifeinaConvent", CoverType.BOTH, BookSubject.RELIGION);
    public static final Book SMILE_ACROSS_THE_AISLE = registerBase("SmileAcrossTheAisle", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book SNOW_ON_THE_MOUNTAIN = registerBase("SnowontheMountain", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book SOLAR_FLARES_AN_APOCALYPTIC_DANGER = registerBase("SolarFlaresAnApocalypticDanger", CoverType.SOFTCOVER, BookSubject.CONSPIRACY);
    public static final Book SPACE_COMMANDO_TAKEOVER = registerBase("SpaceCommandoTakeover", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book SPACE_CREW_ATTACK_OF_THE_JANSARIANS = registerBase("SpaceCrewAttackoftheJansarians", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book SPACE_CREW_TRAIL_OF_OLLARFS_PYRAMID = registerBase("SpaceCrewTrailofOllarfsPyramid", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book SPACE_CREW_WHO_STOLE_THE_SILK_ROAD = registerBase("SpaceCrewWhoStoletheSilkRoad", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book SPACE_IS_IT_TOO_BIG = registerBase("SpaceIsitTooBig", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book SPAWNLOOT_DESOLATION = registerBase("SpawnlootDesolation", CoverType.BOTH, BookSubject.HORROR);
    public static final Book SPEAK_TO_THE_ANGELS = registerBase("SpeaktotheAngels", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book SPECIAL_FORCES_THE_SHARP_EDGE_OF_FREEDOM = registerBase(
        "SpecialForcesTheSharpEdgeofFreedom", CoverType.BOTH, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book SPECTRAL_SEVEN = registerBase("SpectralSeven", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book SPIDERS_AND_SNAKES = registerBase("SpidersandSnakes", CoverType.HARDCOVER, BookSubject.NATURE);
    public static final Book SPIKE_MOUNTAIN = registerBase("SpikeMountain", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book SPORTS_2000_WHAT_THE_FUTURE_MAY_HOLD = registerBase("Sports2000WhattheFutureMayHold", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book SPORTS_OF_THE_ANCIENTS = registerBase("SportsoftheAncients", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book SPORTS_PHOTOGRAPHY = registerBase("SportsPhotography", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book SPOT_THE_CLUES = registerBase("SpottheClues", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book SPOTS_AND_SHAVING_WHAT_I_WISH_I_KNEW = registerBase("SpotsandShavingWhatIWishIKnew", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book SPRINTING_TOWARD_FREEDOM = registerBase("SprintingTowardFreedom", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book STALIN_THE_RED_CONQUEROR = registerBase(
        "StalinTheRedConqueror", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.HISTORY, BookSubject.MILITARY
    );
    public static final Book STATE_LAW_GUIDE = registerBase("StateLawGuide", CoverType.HARDCOVER, BookSubject.LEGAL, BookSubject.POLITICS);
    public static final Book STEALTH_AIRCRAFT_A_HISTORY = registerBase("StealthAircraftAHistory", CoverType.BOTH, BookSubject.MILITARY);
    public static final Book STEAMSHIP_HURRICANE_AN_INSIDE_LOOK = registerBase(
        "SteamshipHurricaneAnInsideLook", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.TEENS, BookSubject.MUSIC
    );
    public static final Book STOCK_TRADING_TIPS_FROM_A_WALL_STREET_MILLIONAIRE = registerBase(
        "StockTradingTipsFromaWallStreetMillionaire", CoverType.HARDCOVER, BookSubject.BUSINESS
    );
    public static final Book STOP_CRYING_START_LIVING = registerBase(
        "StopCryingStartLiving", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION, BookSubject.SELF_HELP
    );
    public static final Book STOP_CURSING_MOMMY = registerBase("StopCursingMommy", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book STUCK = registerBase("Stuck", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book STUDY_LESS_STUDY_BETTER = registerBase("StudyLessStudyBetter", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book STUDY_NOTES_FROM_A_STRAIGHT_A_STUDENT = registerBase("StudyNotesFromaStraightAStudent", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book STYLE_ICONS = registerBase("StyleIcons", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book SUE_AND_COUNTERSUE = registerBase("SueandCountersue", CoverType.SOFTCOVER, BookSubject.LEGAL, BookSubject.POLITICS);
    public static final Book SUFFERING_AND_SIN_WHAT_IS_GODS_PLAN = registerBase("SufferingandSinWhatisGodsPlan", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book SUFFRAGETTES_DEEDS_NOT_WORDS = registerBase("SuffragettesDeedsNotWords", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book SUITED_AND_BOOTED_SUCCEED_WITH_THE_RIGHT_CLOTHES = registerBase(
        "SuitedandBootedSucceedWiththeRightClothes", CoverType.SOFTCOVER, BookSubject.SELF_HELP, BookSubject.FASHION
    );
    public static final Book SUITS_AND_TOP_HATS = registerBase("SuitsandTopHats", CoverType.BOTH, BookSubject.FASHION);
    public static final Book SURFING_ONLINE_FUN_FOR_THE_WHOLE_FAMILY = registerBase(
        "SurfingOnlineFunForTheWholeFamily", CoverType.SOFTCOVER, BookSubject.COMPUTER
    );
    public static final Book SURGERY_THROUGH_YOUR_GUARDIAN_ANGEL = registerBase(
        "SurgeryThroughYourGuardianAngel", CoverType.SOFTCOVER, BookSubject.NEW_AGE, BookSubject.QUACKERY
    );
    public static final Book SURRENDER_OF_THE_SIOUX = registerBase("SurrenderoftheSioux", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book SURVIVING_SATS = registerBase("SurvivingSATs", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book SURVIVING_TORNADOES = registerBase("SurvivingTornadoes", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book SWAT_A_NEW_APPROACH = registerBase("SWATANewApproach", CoverType.HARDCOVER, BookSubject.POLICING);
    public static final Book TALES_FROM_THE_FRONTLINES = registerBase("TalesFromtheFrontlines", CoverType.SOFTCOVER, BookSubject.MILITARY);
    public static final Book TALES_OF_THE_EARP_BROTHERS = registerBase("TalesoftheEarpBrothers", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book TALKING_TO_OTHER_TEENS = registerBase("TalkingtoOtherTeens", CoverType.SOFTCOVER, BookSubject.TEENS);
    public static final Book TANGO_DELTA = registerBase("TangoDelta", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book TANKS_AND_PLANES = registerBase("TanksandPlanes", CoverType.HARDCOVER, BookSubject.MILITARY);
    public static final Book TAO_TE_CHING = registerBase("TaoTeChing", CoverType.BOTH, BookSubject.PHILOSOPHY, BookSubject.CLASSIC);
    public static final Book TAURUS_MEETS_LEO_AN_ASTROLOGICAL_GUIDE_TO_LOVE = registerBase(
        "TaurusMeetsLeoAnAstrologicalGuidetoLove", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP
    );
    public static final Book TEA_WITH_TEDDY = registerBase("TeaWithTeddy", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book TEAM_AIDENS = registerBase("Teamaidens", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book TECH_GENIUS_THE_KIRRUS_STORY = registerBase(
        "TechGeniusTheKirrusStory", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.COMPUTER
    );
    public static final Book TEN_ZERO_ONE_DAYS_AT_THE_CINEMA = registerBase("1001DaysattheCinema", CoverType.SOFTCOVER, BookSubject.CINEMA);
    public static final Book TENDER_BUTTONS = registerBase("TenderButtons", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book TENDING_GOALS = registerBase("TendingGoals", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book TENNESSEE_TEMPTATION = registerBase("TennesseeTemptation", CoverType.BOTH, BookSubject.ROMANCE);
    public static final Book TERRAFORM = registerBase("Terraform", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book TEXAS_TURMOIL = registerBase("TexasTurmoil", CoverType.BOTH, BookSubject.ROMANCE);
    public static final Book THANK_GOD_FOR_UNIONS = registerBase("ThankGodforUnions", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book THE_ADDIS_ABABA_INJUNCTION = registerBase("TheAddisAbabaInjunction", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_ADVENTURES_OF_PINOCCHIO = registerBase(
        "TheAdventuresofPinocchio", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book THE_ADVENTURES_OF_TOM_SAWYER = registerBase(
        "TheAdventuresofTomSawyer", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book THE_ADVENTURES_OF_TOOKS_BEAR = registerBase("TheAdventuresofTooksBear", CoverType.BOTH, BookSubject.CHILDS);
    public static final Book THE_AENEID = registerBase("TheAeneid", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_AGE_OF_INNOCENCE = registerBase("TheAgeofInnocence", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_ALACATRAZ_REVELATION = registerBase("TheAlacatrazRevelation", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book THE_ALCATRAZ_REVELATION = registerBase("TheAlcatrazRevelation", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book THE_AMERICAN_CRISIS = registerBase("TheAmericanCrisis", CoverType.HARDCOVER, BookSubject.CLASSIC);
    public static final Book THE_AMERICAN_JUSTICE_SYSTEM = registerBase("TheAmericanJusticeSystem", CoverType.SOFTCOVER, BookSubject.POLICING);
    public static final Book THE_AMERICAS_BEFORE_COLUMBUS = registerBase(
        "TheAmericasBeforeColumbus", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY
    );
    public static final Book THE_ANGELS_ARENT_THERE = registerBase("TheAngelsArentThere", CoverType.BOTH, BookSubject.SCIENCE);
    public static final Book THE_ANTI_FEDERALIST_PAPERS = registerBase(
        "TheAntiFederalistPapers", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_ART_OF_WAR = registerBase(
        "TheArtofWar", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION, BookSubject.MILITARY
    );
    public static final Book THE_AUTOBIOGRAPHY_OF_BENJAMIN_FRANKLIN = registerBase(
        "TheAutobiographyofBenjaminFranklin", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_BACTERIA_FARMERS = registerBase("TheBacteriaFarmers", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book THE_BARBER_OF_SEVILLE = registerBase(
        "TheBarberofSeville", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_BATTLE_OF_THE_SOMME = registerBase(
        "TheBattleoftheSomme", CoverType.BOTH, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book THE_BEAUTY_OF_AFRICA = registerBase("TheBeautyofAfrica", CoverType.SOFTCOVER, BookSubject.TRAVEL);
    public static final Book THE_BEST_SPORTS_TEAMS_OF_ALL_TIME = registerBase("TheBestSportsTeamsofAllTime", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book THE_BIBLE = registerBase("TheBible", CoverType.BOTH, BookSubject.RELIGION, BookSubject.BIBLE);
    public static final Book THE_BIG_BOOK_OF_SCIENCE = registerBase("TheBigBookofScience", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.CHILDS);
    public static final Book THE_BIRTH_OF_MODERN_HATE = registerBase("TheBirthofModernHate", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book THE_BIRTH_OF_NUMBERS = registerBase("TheBirthofNumbers", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book THE_BLACK_ARTS = registerBase("TheBlackArts", CoverType.BOTH, BookSubject.OCCULT);
    public static final Book THE_BLOOD = registerBase("TheBlood", CoverType.HARDCOVER, BookSubject.MEDICAL);
    public static final Book THE_BOOK_OF_HEALING = registerBase(
        "TheBookofHealing", CoverType.BOTH, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_BOOK_OF_INGENIOUS_MECHANICAL_DEVICES = registerBase(
        "TheBookofIngeniousMechanicalDevices", CoverType.BOTH, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_BOY_BAND_HANDBOOK = registerBase("TheBoyBandHandbook", CoverType.BOTH, BookSubject.TEENS, BookSubject.MUSIC);
    public static final Book THE_BRAVE_LAKOTA = registerBase("TheBraveLakota", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_BRIEFCASE = registerBase("TheBriefcase", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book THE_BRITS_ARE_BACK = registerBase("TheBritsAreBack", CoverType.BOTH, BookSubject.MUSIC);
    public static final Book THE_BROTHERS_KARAMAZOV = registerBase("TheBrothersKaramazov", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_CALL_OF_CTHULHU = registerBase(
        "TheCallofCthulhu", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book THE_CALL_OF_THE_WILD = registerBase("TheCalloftheWild", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_CAR_CRASH_THAT_CHANGED_MY_LIFE = registerBase("TheCarCrashThatChangedMyLife", CoverType.BOTH, BookSubject.SAD_NON_FICTION);
    public static final Book THE_CASK_OF_AMONTILLADO = registerBase(
        "TheCaskofAmontillado", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book THE_CHERRY_ORCHARD = registerBase("TheCherryOrchard", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_CHOCTAWS_OF_MULE_FOREST = registerBase("TheChoctawsofMuleForest", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_CLASSIFIED_SECRETS_OF_WEIGHT_LOSS = registerBase("TheClassifiedSecretsofWeightLoss", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book THE_CLUMSY_MISTER = registerBase("TheClumsyMister", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book THE_COMING_CATACLYSM_REPENT_AND_BE_SAVED = registerBase(
        "TheComingCataclysmRepentandBeSaved", CoverType.SOFTCOVER, BookSubject.QUIGLEY, BookSubject.HISTORY, BookSubject.CONSPIRACY
    );
    public static final Book THE_COMING_PANDEMIC = registerBase("TheComingPandemic", CoverType.SOFTCOVER, BookSubject.CONSPIRACY);
    public static final Book THE_COMING_POLITICAL_CRISIS = registerBase("TheComingPoliticalCrisis", CoverType.BOTH, BookSubject.POLITICS);
    public static final Book THE_COMPLETE_MELVILLE_MEDICAL_DIET = registerBase("TheCompleteMelvilleMedicalDiet", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book THE_CORN_MAZE_SLAYINGS = registerBase("TheCornMazeSlayings", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book THE_COUNT_OF_MONTE_CRISTO = registerBase("TheCountofMonteCristo", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_CRAFT_OF_TRADING = registerBase("TheCraftofTrading", CoverType.SOFTCOVER, BookSubject.BUSINESS);
    public static final Book THE_CRYING_OF_THE_FOXES = registerBase("TheCryingoftheFoxes", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book THE_CUBAN_MISSILE_WAR = registerBase("TheCubanMissileWar", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book THE_CURING_PRAYER = registerBase("TheCuringPrayer", CoverType.SOFTCOVER, BookSubject.NEW_AGE, BookSubject.QUACKERY);
    public static final Book THE_CURTAIN = registerBase("TheCurtain", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book THE_DARK_SIDE_OF_USENETS = registerBase("TheDarkSideofUsenets", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book THE_DISAPPEARANCE_OF_BRENDA_AND_COLLEEN_BUSH = registerBase(
        "TheDisappearanceofBrendaandColleenBush", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION, BookSubject.TRUE_CRIME
    );
    public static final Book THE_DIVINE_COMEDY = registerBase(
        "TheDivineComedy", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.RELIGION
    );
    public static final Book THE_DOG_GOBLIN_COMPANION = registerBase("TheDogGoblinCompanion", CoverType.SOFTCOVER, BookSubject.CINEMA);
    public static final Book THE_DOLL_HOUSE = registerBase(
        "TheDollHouse", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_DONKEY_AND_CART = registerBase("TheDonkeyandCart", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book THE_DREADED_KNOCK = registerBase("TheDreadedKnock", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book THE_DUBLIN_CONFRONTATION = registerBase("TheDublinConfrontation", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_EASTERN_FRONT = registerBase(
        "TheEasternFront", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book THE_EMERALD_ISLE = registerBase("TheEmeraldIsle", CoverType.HARDCOVER, BookSubject.TRAVEL);
    public static final Book THE_EMPTY_OCEANS = registerBase("TheEmptyOceans", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book THE_END_OF_ALL_DANGERS = registerBase("TheEndofAllDangers", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_END_OF_OTHER_LIVES = registerBase("TheEndOfOtherLives", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION);
    public static final Book THE_EYE_POPPER = registerBase("TheEyePopper", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book THE_FALL_OF_THE_ROMAN_EMPIRE = registerBase(
        "TheFalloftheRomanEmpire", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_FALLACY_OF_MODERN_LIFE = registerBase(
        "TheFallacyofModernLife", CoverType.SOFTCOVER, BookSubject.QUIGLEY, BookSubject.CONSPIRACY
    );
    public static final Book THE_FEDERALIST_PAPERS = registerBase(
        "TheFederalistPapers", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_FEELING_OF_LOVE = registerBase("TheFeelingofLove", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book THE_FILL_YOUR_TOILET_WITH_GRAVY_DIET = registerBase("TheFillYourToiletWithGravyDiet", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book THE_FIRST_OLYMPICS = registerBase("TheFirstOlympics", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book THE_FOLKLORE_OF_KNOX = registerBase("TheFolkloreofKnox", CoverType.BOTH, BookSubject.OCCULT, BookSubject.HISTORY);
    public static final Book THE_FOREST_CABIN_MASSACRE = registerBase("TheForestCabinMassacre", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book THE_FORGETFUL_MISS = registerBase("TheForgetfulMiss", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book THE_FORTY_CABBAGES_A_DAY_DIET = registerBase("TheFortyCabbagesADayDiet", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book THE_FOUR_BOOKS = registerBase(
        "TheFourBooks", CoverType.HARDCOVER, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_FRENCH_RESISTANCE = registerBase("TheFrenchResistance", CoverType.BOTH, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY);
    public static final Book THE_GETAWAY = registerBase("TheGetaway", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book THE_GHOST_DANCE = registerBase("TheGhostDance", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_GLOWING_OHIO_RADIOACTIVE_WASTE_IN_OUR_WATER = registerBase(
        "TheGlowingOhioRadioactiveWasteinOurWater", CoverType.SOFTCOVER, BookSubject.CONSPIRACY
    );
    public static final Book THE_GODS_OF_PEGANA = registerBase(
        "TheGodsofPegana", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.FANTASY, BookSubject.CLASSIC
    );
    public static final Book THE_GRAND_DAME = registerBase("TheGrandDame", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_GREAT_GATSBY = registerBase("TheGreatGatsby", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_HAPPY_MISS = registerBase("TheHappyMiss", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book THE_HAUNTING_OF_THE_DERRIN_HOTEL = registerBase("TheHauntingoftheDerrinHotel", CoverType.BOTH, BookSubject.HORROR);
    public static final Book THE_HEIR_TO_CHRIST_MY_STORY = registerBase(
        "TheHeirtoChristMyStory", CoverType.SOFTCOVER, BookSubject.QUIGLEY, BookSubject.CONSPIRACY
    );
    public static final Book THE_HERMETIC_PHILOSOPHY = registerBase("TheHermeticPhilosophy", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book THE_HIGHWAYS_OF_KNOX_COUNTRY = registerBase("TheHighwaysofKnoxCountry", CoverType.SOFTCOVER, BookSubject.ROMANCE);
    public static final Book THE_HISTORY_OF_ANIMATION = registerBase("TheHistoryofAnimation", CoverType.BOTH, BookSubject.ART);
    public static final Book THE_HOUND_OF_THE_BASKERVILLES = registerBase(
        "TheHoundoftheBaskervilles", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CRIME_FICTION
    );
    public static final Book THE_HUMAN_BODY_OUR_INSIDES = registerBase("TheHumanBodyOurInsides", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book THE_HUNGRY_MISTER = registerBase("TheHungryMister", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book THE_ILIAD = registerBase("TheIliad", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_IMMUNE_SYSTEM = registerBase("TheImmuneSystem", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book THE_IMPORTANCE_OF_BEING_EARNEST = registerBase(
        "TheImportanceofBeingEarnest", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_INCREDIBLY_NAUSEOUS_BUTTERFLY = registerBase(
        "TheIncrediblyNauseousButterfly", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL
    );
    public static final Book THE_INNOCENTS_ABROAD = registerBase("TheInnocentsAbroad", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_ISLAMIC_GOLDEN_AGE = registerBase("TheIslamicGoldenAge", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book THE_ISLAND_OF_DR_MOREAU = registerBase(
        "TheIslandofDrMoreau", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.SCIFI
    );
    public static final Book THE_ISLE_OF_PINES = registerBase("TheIsleofPines", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_ISTANBUL_FELICITATION = registerBase("TheIstanbulFelicitation", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_JUDGE = registerBase("TheJudge", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_KIDNAPPING_OF_KATHARINE_MCMULLEN = registerBase(
        "TheKidnappingofKatharineMcMullen", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME
    );
    public static final Book THE_LADY_IN_WARD_SIX = registerBase("TheLadyinWardSix", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book THE_LAND_OF_TUCKEDAWAY = registerBase("TheLandofTuckedaway", CoverType.BOTH, BookSubject.FANTASY, BookSubject.CHILDS);
    public static final Book THE_LANES_OF_KERRY = registerBase("TheLanesofKerry", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book THE_LAST_MAN = registerBase("TheLastMan", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_LAZARUS_GENE = registerBase("TheLazarusGene", CoverType.BOTH, BookSubject.SCIENCE);
    public static final Book THE_LEGEND_OF_WILD_BILL_HICKOK = registerBase("TheLegendofWildBillHickok", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_LEONARD_BROTHERS_DUEL = registerBase("TheLeonardBrothersDuel", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_LIFE_AND_DEATH_OF_THE_BRITISH_EMPIRE = registerBase("TheLifeandDeathoftheBritishEmpire", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book THE_LISBON_SWINDLE = registerBase("TheLisbonSwindle", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_LITTLE_BLACK_DRESS = registerBase("TheLittleBlackDress", CoverType.BOTH, BookSubject.FASHION);
    public static final Book THE_LORD_IN_EVERYDAY_LIFE = registerBase("TheLordinEverydayLife", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book THE_LOVELY_GIANT_MAN = registerBase("TheLovelyGiantMan", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book THE_MAGIC_HOODIE = registerBase("TheMagicHoodie", CoverType.BOTH, BookSubject.CHILDS);
    public static final Book THE_MAGIC_HOODIE_SIGNED = registerBase("TheMagicHoodieSigned", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book THE_MAGIC_OF_DOING_NOTHING = registerBase("TheMagicofDoingNothing", CoverType.SOFTCOVER, BookSubject.SELF_HELP);
    public static final Book THE_MARRIAGE_OF_FIGARO = registerBase(
        "TheMarriageofFigaro", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_MASQUE_OF_THE_RED_DEATH = registerBase(
        "TheMasqueoftheRedDeath", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book THE_MEANING_OF_DREAMS = registerBase("TheMeaningofDreams", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book THE_MECCAN_ILLUMINATIONS = registerBase(
        "TheMeccanIlluminations", CoverType.BOTH, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_MESSY_MISS = registerBase("TheMessyMiss", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book THE_METAMORPHOSIS = registerBase("TheMetamorphosis", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_MIGHTY_OAK = registerBase("TheMightyOak", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book THE_MILDEST_SALOON_IN_THE_WEST = registerBase("TheMildestSaloonintheWest", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_MILITARY_INDUSTRIAL_COMPLEX = registerBase("TheMilitaryIndustrialComplex", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book THE_MODERATORS_ARCH_DUKES_REVENGE = registerBase("TheModeratorsArchDukesRevenge", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book THE_MODERATORS_INTO_THE_CHAOS_ABYSS = registerBase("TheModeratorsIntotheChaosAbyss", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book THE_MODERATORS_TALES_FROM_THE_ICE_PALACE = registerBase(
        "TheModeratorsTalesFromtheIcePalace", CoverType.SOFTCOVER, BookSubject.CHILDS
    );
    public static final Book THE_MODERN_ARTS = registerBase("TheModernArts", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book THE_MOJAVE = registerBase("TheMojave", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book THE_MONASTIC_LIFE = registerBase("TheMonasticLife", CoverType.BOTH, BookSubject.RELIGION);
    public static final Book THE_MUQADDIMAH = registerBase(
        "TheMuqaddimah", CoverType.BOTH, BookSubject.PHILOSOPHY, BookSubject.CLASSIC, BookSubject.CLASSIC_NONFICTION
    );
    public static final Book THE_MURDER_OF_GWEN_STAHL = registerBase(
        "TheMurderofGwenStahl", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION, BookSubject.TRUE_CRIME
    );
    public static final Book THE_MURDER_OF_ROGER_ACKROYD = registerBase(
        "TheMurderofRogerAckroyd", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book THE_MYSTERIOUS_DEATH_OF_RAQUEL_ROGERS = registerBase(
        "TheMysteriousDeathofRaquelRogers", CoverType.SOFTCOVER, BookSubject.CINEMA, BookSubject.TRUE_CRIME
    );
    public static final Book THE_NAUGHTY_MISTER = registerBase("TheNaughtyMister", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book THE_NEW_WORLD_ORDER = registerBase("TheNewWorldOrder", CoverType.SOFTCOVER, BookSubject.BIOGRAPHY, BookSubject.MILITARY);
    public static final Book THE_NIGHT_IN_QUESTION = registerBase("TheNightinQuestion", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book THE_NIGHT_LONG_DAY = registerBase(
        "TheNightLongDay", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_OBJECTION = registerBase("TheObjection", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book THE_ODYSSEY = registerBase(
        "TheOdyssey", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HISTORY, BookSubject.CLASSIC
    );
    public static final Book THE_OMEGA_DEPARTMENT_GUIDE = registerBase("TheOmegaDepartmentGuide", CoverType.HARDCOVER, BookSubject.SCIFI);
    public static final Book THE_ONE_BITE_DIET = registerBase("TheOneBiteDiet", CoverType.SOFTCOVER, BookSubject.DIET);
    public static final Book THE_OPENING_ARGUMENT = registerBase("TheOpeningArgument", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book THE_ORION_VARIANT = registerBase("TheOrionVariant", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book THE_OZONE_HOLE_HOAX = registerBase("TheOzoneHoleHoax", CoverType.SOFTCOVER, BookSubject.CONSPIRACY);
    public static final Book THE_OZONE_LAYER_OUR_COSMIC_SHIELD = registerBase("TheOzoneLayerOurCosmicShield", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book THE_PAIN_ISNT_FAKE_THE_REAL_WWC = registerBase("ThePainIsntFakeTheRealWWC", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book THE_PAST_PRESENT_AND_FUTURE_OF_GENDER = registerBase("ThePastPresentandFutureofGender", CoverType.BOTH);
    public static final Book THE_PEOPLES_PRINCESS = registerBase("ThePeoplesPrincess", CoverType.SOFTCOVER, BookSubject.BIOGRAPHY);
    public static final Book THE_PICTURE_OF_DORIAN_GREY = registerBase(
        "ThePictureofDorianGrey", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book THE_PIGSKIN_LIFE = registerBase("ThePigskinLife", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book THE_PILEDRIVER_IN_HIS_WORDS = registerBase("ThePiledriverInHisWords", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book THE_PLAINS_SLAYINGS = registerBase("ThePlainsSlayings", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_PLAYBOY_OF_THE_WESTERN_WORLD = registerBase(
        "ThePlayboyoftheWesternWorld", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_POETRY_OF_WILLIAM_BUTLER_YEATS = registerBase(
        "ThePoetryofWilliamButlerYeats", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book THE_POPES_SPY = registerBase("ThePopesSpy", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_POWER_OF_COMPUTERS = registerBase("ThePowerofComputers", CoverType.HARDCOVER, BookSubject.COMPUTER);
    public static final Book THE_PRINCE = registerBase("ThePrince", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.CLASSIC, BookSubject.MILITARY);
    public static final Book THE_PRISONER_OF_CHILLON = registerBase("ThePrisonerofChillon", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_PROPHET = registerBase("TheProphet", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_PURPLE_CLOUD = registerBase("ThePurpleCloud", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_QUANTUM_ZONE = registerBase("TheQuantumZone", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book THE_RAIN_AND_THE_FOOTSTEPS = registerBase("TheRainandtheFootsteps", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book THE_RAVEN_AND_OTHER_POEMS = registerBase(
        "TheRavenandOtherPoems", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book THE_REAL_CURE_FOR_BALDNESS = registerBase("TheREALCureforBaldness", CoverType.SOFTCOVER, BookSubject.QUACKERY);
    public static final Book THE_RED_ROOM = registerBase("TheRedRoom", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_REDS_REBORN = registerBase("TheRedsReborn", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_REMEDIES_BIG_PHARMA_HIDES = registerBase(
        "TheRemediesBigPharmaHides", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.QUACKERY
    );
    public static final Book THE_RENAISSANCE = registerBase("TheRenaissance", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book THE_REPUBLIC = registerBase("TheRepublic", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.HISTORY, BookSubject.CLASSIC);
    public static final Book THE_RIGHT_HAND = registerBase("TheRightHand", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book THE_RIME_OF_THE_ANCIENT_MARINER = registerBase(
        "TheRimeoftheAncientMariner", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC
    );
    public static final Book THE_RING_CYCLE = registerBase(
        "TheRingCycle", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book THE_RIVERSIDE_RIPPER = registerBase("TheRiversideRipper", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book THE_SAD_LIFE_OF_VINCENT_VAN_GOGH = registerBase("TheSadLifeofVincentVanGogh", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book THE_SATANIST_COMMANDMENTS = registerBase("TheSatanistCommandments", CoverType.HARDCOVER, BookSubject.OCCULT);
    public static final Book THE_SCARLET_LETTER = registerBase("TheScarletLetter", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_SCIENCE_OF_SEDUCTION = registerBase(
        "TheScienceofSeduction", CoverType.SOFTCOVER, BookSubject.SELF_HELP, BookSubject.SEXY, BookSubject.RELATIONSHIP
    );
    public static final Book THE_SCREAMS = registerBase("TheScreams", CoverType.BOTH, BookSubject.HORROR);
    public static final Book THE_SEAGULL = registerBase("TheSeagull", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_SECRET_GARDEN = registerBase("TheSecretGarden", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_SECRET_LOVE_LANGUAGE = registerBase("TheSecretLoveLanguage", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP);
    public static final Book THE_SETTLEMENT = registerBase("TheSettlement", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book THE_SEVENTH_TRUMPET = registerBase("TheSeventhTrumpet", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book THE_SHADOW_OVER_INNSMOUTH = registerBase(
        "TheShadowoverInnsmouth", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.HORROR, BookSubject.CLASSIC
    );
    public static final Book THE_SHADOWED_PLANE = registerBase("TheShadowedPlane", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book THE_SHAPE_OF_FUTURE_WARS = registerBase("TheShapeofFutureWars", CoverType.SOFTCOVER, BookSubject.MILITARY);
    public static final Book THE_SHOTGUNS_OF_RUSTLERS_CREEK = registerBase("TheShotgunsofRustlersCreek", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_SHOWDOWN = registerBase("TheShowdown", CoverType.SOFTCOVER, BookSubject.HORROR);
    public static final Book THE_SIGN_OF_THE_FOUR = registerBase(
        "TheSignoftheFour", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CRIME_FICTION
    );
    public static final Book THE_SOCIAL_CONTRACT = registerBase("TheSocialContract", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC);
    public static final Book THE_SOUL_A_SCIENTIFIC_STUDY = registerBase(
        "TheSoulAScientificStudy", CoverType.SOFTCOVER, BookSubject.PHILOSOPHY, BookSubject.SCIENCE, BookSubject.NEW_AGE, BookSubject.RELIGION
    );
    public static final Book THE_STAGECOACH_MURDERS = registerBase("TheStagecoachMurders", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_STORY_OF_OIL = registerBase("TheStoryofOil", CoverType.BOTH, BookSubject.SCHOOL_TEXTBOOK, BookSubject.HISTORY);
    public static final Book THE_SUN_ALSO_RISES = registerBase("TheSunAlsoRises", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_SUNDOWN_SHERIFF = registerBase("TheSundownSheriff", CoverType.SOFTCOVER, BookSubject.WESTERN);
    public static final Book THE_SUPERMODEL_MANUAL = registerBase("TheSupermodelManual", CoverType.SOFTCOVER, BookSubject.FASHION);
    public static final Book THE_SUPREME_COURT = registerBase("TheSupremeCourt", CoverType.BOTH, BookSubject.LEGAL, BookSubject.POLITICS);
    public static final Book THE_TALE_OF_PETER_RABBIT = registerBase(
        "TheTaleofPeterRabbit", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book THE_TAMPERED_WITNESS = registerBase("TheTamperedWitness", CoverType.BOTH, BookSubject.THRILLER);
    public static final Book THE_TARFIMMEL_CHRONICLES = registerBase("TheTarfimmelChronicles", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book THE_TEETH_OF_THE_DEAD = registerBase("TheTeethoftheDead", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book THE_THING_IN_THE_VENT = registerBase("TheThingintheVent", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book THE_THOMPSONS_FAMILY_ANNUAL_92_IN_3D = registerBase(
        "TheThompsonsFamilyAnnual92In3D", CoverType.SOFTCOVER, BookSubject.TEENS, BookSubject.CHILDS
    );
    public static final Book THE_THURSDOID_COMPENDIUM = registerBase("TheThursdoidCompendium", CoverType.BOTH);
    public static final Book THE_TIME_MACHINE = registerBase(
        "TheTimeMachine", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.SCIFI
    );
    public static final Book THE_TRIAL = registerBase("TheTrial", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_TRUTH_ABOUT_THE_CDC = registerBase("TheTruthAboutTheCDC", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.QUACKERY);
    public static final Book THE_TURBULENT_SIXTIES = registerBase("TheTurbulentSixties", CoverType.BOTH, BookSubject.HISTORY);
    public static final Book THE_TURN_OF_THE_SCREW = registerBase("TheTurnoftheScrew", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_UNIVERSE = registerBase("TheUniverse", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book THE_VIROLOGIST_AND_DISEASE = registerBase("TheVirologistandDisease", CoverType.BOTH, BookSubject.MEDICAL);
    public static final Book THE_VISION_OF_TUNDALE = registerBase("TheVisionofTundale", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_VOICE_OF_REASON = registerBase("TheVoiceofReason", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book THE_WALLET_OF_LORENE_HILTON = registerBase("TheWalletofLoreneHilton", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book THE_WAR_OF_THE_WORLDS = registerBase(
        "TheWaroftheWorlds", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.SCIFI
    );
    public static final Book THE_WASTE_LAND = registerBase("TheWasteLand", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_WAVES = registerBase("TheWaves", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book THE_WEALTH_OF_NATIONS = registerBase("TheWealthofNations", CoverType.HARDCOVER, BookSubject.CLASSIC);
    public static final Book THE_WIND_IN_THE_WILLOWS = registerBase(
        "TheWindintheWillows", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.CHILDS
    );
    public static final Book THE_WONDER_OF_PRAYER = registerBase("TheWonderofPrayer", CoverType.SOFTCOVER, BookSubject.RELIGION);
    public static final Book THE_WORKERS_GUIDE_TO_COMPUTERS = registerBase("TheWorkersGuidetoComputers", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book THE_WORST_DISASTERS_OF_ALL_TIME = registerBase(
        "TheWorstDisastersofAllTime", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION
    );
    public static final Book THEIR_LOSS_YOUR_PROFIT_INSIDE_ACCOUNTING_TRICKS = registerBase(
        "TheirLossYourProfitInsideAccountingTricks", CoverType.SOFTCOVER, BookSubject.BUSINESS
    );
    public static final Book THINKING_AWAY_SEVERE_ILLNESS = registerBase("ThinkingAwaySevereIllness", CoverType.BOTH, BookSubject.NEW_AGE);
    public static final Book THINKING_SMALL_LIVING_BIG = registerBase("ThinkingSmallLivingBig", CoverType.SOFTCOVER, BookSubject.SELF_HELP);
    public static final Book THOUGHTS_OF_THE_DEAR_LEADER = registerBase("ThoughtsoftheDearLeader", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book THREE_S_A_CROWD = registerBase("ThreesaCrowd", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book THROW_LIKE_THE_PROS = registerBase("ThrowLikethePros", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book THROWING_OUT_THE_TOWEL = registerBase("ThrowingOuttheTowel", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book TIMBERGAP_MANOR = registerBase("TimbergapManor", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book TIME_FOR_HUGS = registerBase("TimeforHugs", CoverType.SOFTCOVER, BookSubject.CHILDS);
    public static final Book TIRED_OF_TROUBLE = registerBase("TiredofTrouble", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book TO_THE_LIGHTHOUSE = registerBase("TotheLighthouse", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book TOMBS_OF_THE_HUNTED = registerBase("TombsoftheHunted", CoverType.SOFTCOVER);
    public static final Book TOO_OLD_TO_BE_SORRY = registerBase("TooOldtobeSorry", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book TORT_LAW = registerBase("TortLaw", CoverType.HARDCOVER, BookSubject.LEGAL);
    public static final Book TORUS_CHRONICLES_1_GLITCHED_HORIZONS = registerBase("TorusChronicles1GlitchedHorizons", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book TORUS_CHRONICLES_2_ORIGINS_FROM_THE_RIFTE = registerBase(
        "TorusChronicles2OriginsFromtheRifte", CoverType.SOFTCOVER, BookSubject.SCIFI
    );
    public static final Book TORUS_CHRONICLES_3_CALCULATED_NATURE = registerBase("TorusChronicles3CalculatedNature", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book TORUS_CHRONICLES_4_FATE_OF_THE_SIMULATION = registerBase(
        "TorusChronicles4FateoftheSimulation", CoverType.SOFTCOVER, BookSubject.SCIFI
    );
    public static final Book TORUS_CHRONICLES_5_THE_GLICHAN_RACE_VICTORIOUS = registerBase(
        "TorusChronicles5TheGlichanRaceVictorious", CoverType.SOFTCOVER, BookSubject.SCIFI
    );
    public static final Book TOSCA = registerBase("Tosca", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book TOUR_DE_ME = registerBase("TourdeMe", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book TRADING_HER_IN_FOR_A_YOUNGER_MODEL = registerBase(
        "TradingHerInForaYoungerModel", CoverType.SOFTCOVER, BookSubject.SEXY, BookSubject.RELATIONSHIP
    );
    public static final Book TRANSFORMING_ECONOMIES = registerBase("TransformingEconomies", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book TRAPPED_IN_THE_ICE = registerBase("TrappedintheIce", CoverType.BOTH, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book TREASURE_ISLAND = registerBase("TreasureIsland", CoverType.HARDCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book TRIMBLE_STEELSKIN = registerBase("TrimbleSteelskin", CoverType.SOFTCOVER, BookSubject.FANTASY);
    public static final Book TRISTAN_AND_ISOLDE = registerBase(
        "TristanandIsolde", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book TRONTON_THE_FIRST_ANDROID = registerBase("TrontonTheFirstAndroid", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book TROUBLE_AT_THE_BRAIN_FACTORY = registerBase("TroubleattheBrainFactory", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book TRUE_FORTEAN_TALES = registerBase("TrueForteanTales", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.NEW_AGE);
    public static final Book TRUE_TALES_FROM_KENTUCKYS_PRISONS = registerBase("TrueTalesFromKentuckysPrisons", CoverType.SOFTCOVER, BookSubject.TRUE_CRIME);
    public static final Book TURING_BLETCHLEYS_GENIUS = registerBase(
        "TuringBletchleysGenius", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.SCIENCE, BookSubject.HISTORY, BookSubject.MILITARY
    );
    public static final Book TURN_ONE_DOLLAR_INTO_ONE_MILLION = registerBase("TurnOneDollarIntoOneMillion", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book TURN_YOUR_HIGH_IQ_INTO_BIG_BUCKS = registerBase("TurnYourHighIQIntoBigBucks", CoverType.SOFTCOVER, BookSubject.BUSINESS);
    public static final Book TVS_HOTTEST_CARPENTERS = registerBase("TVsHottestCarpenters", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book TWENTY_FOUR_EXPERIMENTS_TO_TRY_AT_HOME = registerBase("24ExperimentstoTryatHome", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book TWENTY_FOUR_TRUTHS_A_SECOND = registerBase("TwentyFourTruthsaSecond", CoverType.BOTH, BookSubject.CINEMA);
    public static final Book TWENTY_THIRTY_RETURN_FROM_THE_MOON = registerBase("2030ReturnFromtheMoon", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book TWENTY_THOUSAND_LEAGUES_UNDER_THE_SEAS = registerBase(
        "20000LeaguesUndertheSeas", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC, BookSubject.SCIFI
    );
    public static final Book TWENTY_TWENTY_THREE_INTERGALACTIC_VOYAGE = registerBase("2023IntergalacticVoyage", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book TWICE_A_NIGHT = registerBase("TwiceaNight", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book TWIN_COFFINS = registerBase("TwinCoffins", CoverType.SOFTCOVER, BookSubject.ROMANCE, BookSubject.HORROR);
    public static final Book TWIST = registerBase("Twist", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC);
    public static final Book UFOS_A_PSYCHIC_CONNECTION = registerBase(
        "UFOsAPsychicConnection", CoverType.SOFTCOVER, BookSubject.CONSPIRACY, BookSubject.NEW_AGE
    );
    public static final Book ULTIMATE_SPORTS_EQUIPMENT_GUIDE = registerBase("UltimateSportsEquipmentGuide", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book ULYSSES = registerBase("Ulysses", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book UNDER_THE_HELMET = registerBase("UndertheHelmet", CoverType.BOTH, BookSubject.SPORTS);
    public static final Book UNDER_THE_RUG = registerBase("UndertheRug", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book UNDER_THE_SURFACE = registerBase("UndertheSurface", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book UNDERSTANDING_THE_OZONE_LAYER = registerBase("UnderstandingtheOzoneLayer", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book UP_THE_AVENUE = registerBase("UptheAvenue", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book UPTURNED_BATHTUB = registerBase("UpturnedBathtub", CoverType.SOFTCOVER, BookSubject.GENERAL_FICTION);
    public static final Book VANITY_FAIR = registerBase("VanityFair", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book VERSAILLES_73_THE_NIGHT_THAT_CHANGED_FASHION = registerBase(
        "Versailles73TheNightthatChangedFashion", CoverType.SOFTCOVER, BookSubject.FASHION
    );
    public static final Book VINSEG_THE_ANGEL_SLAYER = registerBase("VinsegtheAngelSlayer", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book WALDEN = registerBase("Walden", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book WALKING_THROUGH_AMERICA = registerBase("WalkingThroughAmerica", CoverType.SOFTCOVER, BookSubject.NATURE);
    public static final Book WANTED_FOR_QUESTIONING = registerBase("WantedforQuestioning", CoverType.BOTH, BookSubject.CRIME_FICTION);
    public static final Book WAR_AND_DIPLOMACY = registerBase("WarandDiplomacy", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book WAR_AND_PEACE = registerBase("WarandPeace", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book WAR_FRONT = registerBase("WarFront", CoverType.SOFTCOVER, BookSubject.THRILLER);
    public static final Book WAR_IN_THE_WEST = registerBase(
        "WarintheWest", CoverType.HARDCOVER, BookSubject.HISTORY, BookSubject.MILITARY, BookSubject.MILITARY_HISTORY
    );
    public static final Book WAR_ON_THE_GRIDIRON = registerBase("WarontheGridiron", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book WAR_THE_NECESSARY_EVIL = registerBase("WarTheNecessaryEvil", CoverType.SOFTCOVER, BookSubject.MILITARY);
    public static final Book WASHINGTON_FROM_THE_INSIDE = registerBase("WashingtonFromtheInside", CoverType.SOFTCOVER, BookSubject.POLITICS);
    public static final Book WATCHING_THE_STARS = registerBase("WatchingtheStars", CoverType.SOFTCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.SCIENCE);
    public static final Book WE_RE_MADE_FROM_THE_MILKY_WAY = registerBase("WereMadeFromtheMilkyWay", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book WE_WILL_FIGHT_THEM_THE_LIFE_OF_CHURCHILL = registerBase(
        "WeWillFightThemTheLifeofChurchill", CoverType.BOTH, BookSubject.BIOGRAPHY, BookSubject.HISTORY, BookSubject.MILITARY
    );
    public static final Book WEAR_YOUR_SAFETY_BELT = registerBase("WearYourSafetyBelt", CoverType.BOTH, BookSubject.SAD_NON_FICTION);
    public static final Book WEARING_THIN = registerBase("WearingThin", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book WHAT_A_RACKET = registerBase("WhataRacket", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book WHAT_ABOUT_CIVIL_WRONGS = registerBase("WhatAboutCivilWrongs", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book WHAT_HAPPENED_TO_ARTHUR_MICHAUD = registerBase(
        "WhatHappenedToArthurMichaud", CoverType.SOFTCOVER, BookSubject.SAD_NON_FICTION, BookSubject.TRUE_CRIME
    );
    public static final Book WHAT_RAINBOWS_ARE_MADE_OF = registerBase(
        "WhatRainbowsAreMadeOf", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.CHILDS
    );
    public static final Book WHAT_SOUND_IS = registerBase("WhatSoundIs", CoverType.SOFTCOVER, BookSubject.SCIENCE);
    public static final Book WHAT_WE_DO = registerBase("WhatWeDo", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book WHAT_WOMEN_ACTUALLY_WANT = registerBase("WhatWomenACTUALLYWant", CoverType.SOFTCOVER, BookSubject.RELATIONSHIP);
    public static final Book WHATS_IN_THE_BEEF_THE_TRUTH_ABOUT_SPIFFOS = registerBase(
        "WhatsintheBeefTheTruthAboutSpiffos", CoverType.SOFTCOVER, BookSubject.CONSPIRACY
    );
    public static final Book WHEN_WE_DEAD_AWAKEN = registerBase("WhenWeDeadAwaken", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book WHERE_DO_BABIES_COME_FROM = registerBase(
        "WhereDoBabiesComeFrom", CoverType.HARDCOVER, BookSubject.SCHOOL_TEXTBOOK, BookSubject.CHILDS
    );
    public static final Book WHISKEY_AND_BULLETS = registerBase("WhiskeyandBullets", CoverType.SOFTCOVER, BookSubject.CRIME_FICTION);
    public static final Book WHISPER_ON_THE_BREEZE = registerBase("WhisperontheBreeze", CoverType.BOTH, BookSubject.GENERAL_FICTION);
    public static final Book WHITE_MAGIC_USES_AND_DANGERS = registerBase("WhiteMagicUsesandDangers", CoverType.SOFTCOVER, BookSubject.OCCULT);
    public static final Book WHO_STOLE_THE_COOKIES = registerBase("WhoStoletheCookies", CoverType.BOTH, BookSubject.CHILDS_PICTURE_SPECIAL);
    public static final Book WHY_CANT_TREES_TALK = registerBase("WhyCantTreesTalk", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book WHY_DID_GOD_LET_IT_HAPPEN = registerBase("WhydidGodletithappen", CoverType.BOTH, BookSubject.SAD_NON_FICTION, BookSubject.RELIGION);
    public static final Book WHY_DONT_THE_OCEANS_DRY_UP = registerBase("WhyDontTheOceansDryUp", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book WHY_I_OUGHTA = registerBase("WhyIOughta", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book WHY_IM_RIGHT = registerBase("WhyImRIGHT", CoverType.SOFTCOVER, BookSubject.HASS, BookSubject.POLITICS);
    public static final Book WHY_IS_THE_SKY_SO_BIG = registerBase("WhyistheSkySoBig", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book WHY_SOME_LIVE_AND_SOME_DIED = registerBase("WhySomeLiveandSomeDied", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION);
    public static final Book WIGS_AND_MORE = registerBase(
        "WigsAndMore", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.PLAY, BookSubject.CLASSIC
    );
    public static final Book WINNIE_THE_POOH = registerBase("WinniethePooh", CoverType.SOFTCOVER, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book WITH_LOVE_AND_KISSES = registerBase("WithLoveandKisses", CoverType.BOTH, BookSubject.ROMANCE);
    public static final Book WOMAN_IN_THE_NINETEENTH_CENTURY = registerBase(
        "WomanintheNineteenthCentury", CoverType.HARDCOVER, BookSubject.POLITICS, BookSubject.CLASSIC
    );
    public static final Book WOMEN_IN_ART = registerBase("WomeninArt", CoverType.SOFTCOVER, BookSubject.ART);
    public static final Book WORKER_HEALTH_MENTAL_AND_PHYSICAL = registerBase("WorkerHealthMentalandPhysical", CoverType.HARDCOVER, BookSubject.BUSINESS);
    public static final Book WORLD_CITIES = registerBase("WorldCities", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book WORLD_CUP_FEVER_A_PREVIEW_OF_USA_94 = registerBase("WorldCupFeverAPreviewofUSA94", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book WORLD_LEADERS_ATLAS = registerBase("WorldLeadersAtlas", CoverType.BOTH, BookSubject.PHOTO_SPECIAL);
    public static final Book WORLDS_BEST_SOCCER_TEAMS = registerBase("WorldsBestSoccerTeams", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book WORLDS_TIGHTEST_BEACHWARE = registerBase("WorldsTightestBeachware", CoverType.SOFTCOVER, BookSubject.SEXY);
    public static final Book WORLDS_UNLIKELIEST_PLANE_CRASHES = registerBase(
        "WorldsUnlikeliestPlaneCrashes", CoverType.SOFTCOVER, BookSubject.ADVENTURE_NON_FICTION
    );
    public static final Book WORMHOLE = registerBase("Wormhole", CoverType.BOTH, BookSubject.SCIFI);
    public static final Book WUTHERING_HEIGHTS = registerBase("WutheringHeights", CoverType.BOTH, BookSubject.CLASSIC_FICTION, BookSubject.CLASSIC);
    public static final Book WWC_GREATEST_WRESTLERS_OF_ALL_TIME = registerBase("WWCGreatestWrestlersofAllTime", CoverType.HARDCOVER, BookSubject.SPORTS);
    public static final Book XB38_THE_FAILED_EXPERIMENT = registerBase("XB38TheFailedExperiment", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book XTREME_SKATEBOARDING = registerBase("XtremeSkateboarding", CoverType.SOFTCOVER, BookSubject.SPORTS);
    public static final Book XYZS = registerBase("XYZs", CoverType.HARDCOVER, BookSubject.CHILDS);
    public static final Book YONTEL_THE_SWORDSMAN = registerBase("YonteltheSwordsman", CoverType.BOTH, BookSubject.FANTASY);
    public static final Book YORLX = registerBase("YORLX", CoverType.SOFTCOVER, BookSubject.SCIFI);
    public static final Book YOU_CAN_BE_NORMAL_AND_COOL = registerBase("YouCanBeNormalandCool", CoverType.SOFTCOVER, BookSubject.SELF_HELP);
    public static final Book YOUR_TELNET_HELPER = registerBase("YourTelnetHelper", CoverType.SOFTCOVER, BookSubject.COMPUTER);
    public static final Book Z_HURRICANES_WHAT_THEY_DONT_WANT_YOU_TO_KNOW = registerBase(
        "ZHurricanesWhatTheyDontWantYoutoKnow", CoverType.BOTH, BookSubject.BASEBALL, BookSubject.SPORTS
    );
    private static final Map<Book.CoverAndSubjects, List<Book>> BY_COVER_AND_SUBJECTS = new HashMap<>();
    private final String translationKey;
    private final CoverType cover;
    private final Set<BookSubject> bookSubjects;

    private Book(String translationKey, CoverType cover, Set<BookSubject> bookSubjects) {
        this.translationKey = "IGUI_BookTitle_" + translationKey;
        this.cover = cover;
        this.bookSubjects = bookSubjects;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public CoverType cover() {
        return this.cover;
    }

    public Set<BookSubject> bookSubjects() {
        return this.bookSubjects;
    }

    public static Book get(ResourceLocation id) {
        return Registries.BOOK.get(id);
    }

    public static List<Book> getBooksByCoverAndSubjects(InventoryItem item) {
        CoverType coverType = item.getCoverType();
        List<BookSubject> subjects = item.getBookSubjects();
        return BY_COVER_AND_SUBJECTS.computeIfAbsent(
            new Book.CoverAndSubjects(coverType, subjects),
            k -> Registries.BOOK
                .values()
                .stream()
                .filter(book -> book.cover.matches(coverType))
                .filter(book -> book.bookSubjects.stream().anyMatch(subjects::contains))
                .collect(Collectors.toList())
        );
    }

    public static Book register(String id, CoverType cover, BookSubject... bookSubjects) {
        return register(false, id, new Book(id, cover, Set.of(bookSubjects)));
    }

    private static Book registerBase(String id, CoverType cover, BookSubject... bookSubjects) {
        return register(true, id, new Book(id, cover, Set.of(bookSubjects)));
    }

    private static Book register(boolean allowDefaultNamespace, String id, Book t) {
        return Registries.BOOK.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }

    static {
        if (Core.IS_DEV) {
            for (Book book : Registries.BOOK) {
                TranslationKeyValidator.of(book.translationKey());
            }
        }
    }

    private record CoverAndSubjects(CoverType cover, List<BookSubject> subjects) {
        private CoverAndSubjects(final CoverType cover, final List<BookSubject> subjects) {
            this.cover = cover;
            this.subjects = new ArrayList<>(subjects);
            subjects.sort(Comparator.comparing(subject -> Registries.BOOK_SUBJECT.getLocation(subject).toString()));
        }
    }
}
