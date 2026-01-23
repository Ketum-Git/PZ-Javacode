// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class ComicBook {
    public static final ComicBook HUNDRED_BILLION_BC = registerBase("100BillionBC", 201, true);
    public static final ComicBook ABSOLUTE_HOOEY = registerBase("AbsoluteHooey", 237, false);
    public static final ComicBook ATOM_LIZARD_AWAKES = registerBase("AtomLizardAwakes", 3, false);
    public static final ComicBook ATOM_LIZARD_AWAKES_AGAIN = registerBase("AtomLizardAwakesAgain", 6, false);
    public static final ComicBook ATOM_LIZARD_DESTROYS_CITIES = registerBase("AtomLizardDestroysCities", 9, false);
    public static final ComicBook ATOM_LIZARD_FAR_FUTURE_STORIES = registerBase("AtomLizardFarFutureStories", 12, true);
    public static final ComicBook ATOM_LIZARD_THE_DRAGON_KING_MEDIEVAL_MAYHEM = registerBase("AtomLizardTheDragonKingMedievalMayhem", 12, true);
    public static final ComicBook ATOMMAN_MINIATURE_MAYHEM = registerBase("AtommanMiniatureMayhem", 269, true);
    public static final ComicBook BLASTFORCE = registerBase("BlastForce", 169, false);
    public static final ComicBook BLASTFORCE_UNLEASHED = registerBase("BlastForceUnleashed", 59, true);
    public static final ComicBook BLINDMANS_BLUFF = registerBase("BlindmansBluff", 53, false);
    public static final ComicBook BLOODY_AXEOF_KARNTHE_SLAYER = registerBase("BloodyAxeofKarntheSlayer", 132, true);
    public static final ComicBook BRAT_PENS_HOUSEOF_IDEAS = registerBase("BratPensHouseofIdeas", 513, false);
    public static final ComicBook BRAVELY_INTO_PERIL = registerBase("BravelyIntoPeril", 174, false);
    public static final ComicBook BRICKLAYER = registerBase("Bricklayer", 187, false);
    public static final ComicBook BRIDEOF_THE_CACTUS = registerBase("BrideOfTheCactus", 13, false);
    public static final ComicBook BRUISER = registerBase("Bruiser", 216, false);
    public static final ComicBook BRUISERS = registerBase("Bruisers", 32, true);
    public static final ComicBook CACTUS_ETERNAL = registerBase("CactusETERNAL", 0, false);
    public static final ComicBook CAPTAIN_WOOF_CANINE_PILOT = registerBase("CaptainWoofCaninePilot", 362, true);
    public static final ComicBook CHILDDETECTIVE_TALES = registerBase("ChildDetectiveTales", 317, false);
    public static final ComicBook CHUCKLE = registerBase("Chuckle", 534, true);
    public static final ComicBook COMMANDO_RAID = registerBase("CommandoRaid", 212, false);
    public static final ComicBook CORPSEBOUND = registerBase("Corpsebound", 57, true);
    public static final ComicBook CRIMESOFTHE_CENTURY = registerBase("CrimesoftheCentury", 100, false);
    public static final ComicBook CRYPTOF_INSANITY = registerBase("CryptofInsanity", 135, false);
    public static final ComicBook CURSE_RYDER = registerBase("CurseRyder", 116, true);
    public static final ComicBook DAMSELS_DANGERS = registerBase("DamselsDangers", 72, true);
    public static final ComicBook DANGER_HORSE = registerBase("DangerHorse", 11, true);
    public static final ComicBook DARKDRIVETHE_SORCERER_CAR = registerBase("DarkdrivetheSorcererCar", 120, false);
    public static final ComicBook DEATHOFTHE_NIGHTPORTER = registerBase("DeathoftheNightporter", 0, false);
    public static final ComicBook DENSE_ALLOY = registerBase("DenseAlloy", 257, true);
    public static final ComicBook DINO_TRAPPER = registerBase("DinoTrapper", 193, false);
    public static final ComicBook DINO_TRAPPER_VSATOM_LIZARD = registerBase("DinoTrapperVSAtomLizard", 3, true);
    public static final ComicBook DR_APE_JR_GANG_EXPLORES_SPACE = registerBase("DrApeJrGangExploresSpace", 35, false);
    public static final ComicBook DR_APE_SCIENCE_VIGILANTE = registerBase("DrApeScienceVigilante", 358, false);
    public static final ComicBook DR_APE_BATTLES_ATOM_LIZARD = registerBase("DrApeBattlesAtomLizard", 2, false);
    public static final ComicBook DR_WEREWOLF = registerBase("DrWerewolf", 146, false);
    public static final ComicBook DR_WEREWOLF_DR_APE_TECHNOLOGY_HOSPITAL_SHOWDOWN = registerBase("DrWerewolfDrApeTechnologyHospitalShowdown", 0, true);
    public static final ComicBook DR_WEREWOLF_FULL_MOON = registerBase("DrWerewolfFullMoon", 0, true);
    public static final ComicBook DR_WEREWOLF_RETURNS = registerBase("DrWerewolfReturns", 26, true);
    public static final ComicBook DRACULA_HUNTERS = registerBase("DraculaHunters", 42, true);
    public static final ComicBook DRACULA_HUNTERS_AGAINST_ATOM_LIZARD = registerBase("DraculaHuntersAgainstAtomLizard", 0, true);
    public static final ComicBook DRACULA_HUNTERSVS_QUEEN_VAMPIRE = registerBase("DraculaHuntersvsQueenVampire", 2, false);
    public static final ComicBook DRAKE_STEELE_ESCAPE_FROM_DRACULA = registerBase("DrakeSteeleEscapeFromDracula", 3, false);
    public static final ComicBook DRAKE_STEELE_ESCAPE_FROM_THE_DRACULA_HUNTERS = registerBase("DrakeSteeleEscapeFromTheDraculaHunters", 2, false);
    public static final ComicBook DRAKE_STEELE_VAMPIRE_DETECTIVE = registerBase("DrakeSteeleVampireDetective", 168, true);
    public static final ComicBook ENCOUNTER_CRITICAL = registerBase("EncounterCritical", 17, false);
    public static final ComicBook ESCAPE_FROM_PERIL = registerBase("EscapeFromPeril", 5, false);
    public static final ComicBook FALLOFTHE_STEELMAN = registerBase("FalloftheSteelman", 0, false);
    public static final ComicBook FANTASIES_OF_POWER = registerBase("FantasiesOfPower", 5, false);
    public static final ComicBook FARRAGO = registerBase("Farrago", 127, true);
    public static final ComicBook FASTER_THAN_LIGHTSPEED = registerBase("FasterThanLightspeed", 37, true);
    public static final ComicBook FORBIDDEN_EXPERIMENTS = registerBase("ForbiddenExperiments", 157, false);
    public static final ComicBook FRANKEN_LAD = registerBase("FrankenLad", 53, false);
    public static final ComicBook FRANKEN_LAD_FRANKEN_MUTT = registerBase("FrankenLadFrankenMutt", 11, true);
    public static final ComicBook FREEDOM_ENFORCERS = registerBase("FreedomEnforcers", 53, true);
    public static final ComicBook FROM_OUTOFTHE_SHRIEKING_MORTUARY = registerBase("FromOutoftheShriekingMortuary", 382, false);
    public static final ComicBook FURIOUS_FIVEAND_SCORPMAN = registerBase("FuriousFiveandScorpman", 50, true);
    public static final ComicBook FURIOUS_FIVE_MELTDOWN = registerBase("FuriousFiveMeltdown", 6, false);
    public static final ComicBook FUTURELORDS = registerBase("Futurelords", 23, true);
    public static final ComicBook GAMMA_RADIATION_CHILD = registerBase("GammaRadiationChild", 173, false);
    public static final ComicBook GHOST_KIDS = registerBase("GhostKids", 428, false);
    public static final ComicBook GLADIATOR_ADVENTURES = registerBase("GladiatorAdventures", 195, false);
    public static final ComicBook GORE_RIDE = registerBase("GoreRide", 32, true);
    public static final ComicBook GORILLA_TRUCKER = registerBase("GorillaTrucker", 201, true);
    public static final ComicBook GORILLA_TRUCKERS = registerBase("GorillaTruckers", 84, true);
    public static final ComicBook GREASY_BIKER_STORIES = registerBase("GreasyBikerStories", 315, true);
    public static final ComicBook GREEN_HAWK = registerBase("GreenHawk", 294, true);
    public static final ComicBook GROSS_OUT = registerBase("GrossOut", 86, true);
    public static final ComicBook HE_FRANKENSTEIN = registerBase("HeFrankenstein", 26, false);
    public static final ComicBook HEAVY_WATER = registerBase("HeavyWater", 278, true);
    public static final ComicBook HISAYOSAN_DEATHIS_LIFE = registerBase("HisayosanDeathisLife", 58, false);
    public static final ComicBook HISTORY_UNBELIEVED = registerBase("HistoryUnbelieved", 284, true);
    public static final ComicBook HOLY_WITCH = registerBase("HolyWitch", 135, true);
    public static final ComicBook HORSESHOECRAB = registerBase("Horseshoecrab", 17, true);
    public static final ComicBook HUMANMAN = registerBase("Humanman", 78, false);
    public static final ComicBook HUMANMAN_MEETS_HUMANWOMAN = registerBase("HumanmanMeetsHumanwoman", 5, true);
    public static final ComicBook HUMANWOMAN = registerBase("Humanwoman", 315, false);
    public static final ComicBook ICEBLAZE = registerBase("Iceblaze", 65, true);
    public static final ComicBook ICKY_TALES = registerBase("IckyTales", 216, false);
    public static final ComicBook IMBROGLIO = registerBase("Imbroglio", 61, true);
    public static final ComicBook JACKIE_JAYE_TRUTHSEEKER = registerBase("JackieJayeTruthseeker", 3, false);
    public static final ComicBook JOHN_SPIRAL_NUCLEAR_KERFUFFLE = registerBase("JohnSpiralNuclearKerfuffle", 3, false);
    public static final ComicBook JOHN_SPIRAL_SPYAT_LARGE = registerBase("JohnSpiralSpyatLarge", 158, true);
    public static final ComicBook JOHN_SPIRAL_TORPEDO_TUBE_TALES = registerBase("JohnSpiralTorpedoTubeTales", 3, false);
    public static final ComicBook JUNGLE_ADVENTURE = registerBase("JungleAdventure", 236, false);
    public static final ComicBook KAZUSA = registerBase("Kazusa", 32, false);
    public static final ComicBook KENTUCKY_MUTANTS = registerBase("KentuckyMutants", 56, true);
    public static final ComicBook KING_REDWOOD = registerBase("KingRedwood", 144, false);
    public static final ComicBook KING_REDWOODVS_ATOM_LIZARD = registerBase("KingRedwoodvsAtomLizard", 0, false);
    public static final ComicBook KIT_SEQUOIAS_TWO_FISTS = registerBase("KitSequoiasTwoFists", 184, false);
    public static final ComicBook KIT_SEQUOIAS_MEETS_KING_REDWOOD = registerBase("KitSequoiasMeetsKingRedwood", 4, true);
    public static final ComicBook KARNTHE_DEFEATER = registerBase("KarntheDefeater", 267, true);
    public static final ComicBook KARN_RELENTLESS = registerBase("KarnRelentless", 53, false);
    public static final ComicBook KARN_THE_SAVAGE_SORCERER_SLAYING_SPECIAL = registerBase("KarnTheSavageSorcererSlayingSpecial", 6, false);
    public static final ComicBook KARNVS_MERLIN = registerBase("KarnvsMerlin", 0, false);
    public static final ComicBook LASSO_LADY = registerBase("LassoLady", 350, true);
    public static final ComicBook LASSO_LADYAND_NIGHTPORTER = registerBase("LassoLadyandNightporter", 121, true);
    public static final ComicBook LASSO_LADYAND_STEELMAN = registerBase("LassoLadyandSteelman", 133, true);
    public static final ComicBook LASSO_LADYVS_CTHULHU = registerBase("LassoLadyvsCthulhu", 0, false);
    public static final ComicBook LASSO_LADY_FIGHTTOTHE_DEATH = registerBase("LassoLadyFighttotheDeath", 12, false);
    public static final ComicBook MAIDEN_CANADA = registerBase("MaidenCanada", 235, true);
    public static final ComicBook MAYOR_ACADEMY = registerBase("MayorAcademy", 92, true);
    public static final ComicBook METAL_SPEAR_QUEST = registerBase("MetalSpearQuest", 108, true);
    public static final ComicBook MONSTER_PRESIDENTS = registerBase("MonsterPresidents", 7, true);
    public static final ComicBook MILLIPEDE_KID = registerBase("MillipedeKid", 345, false);
    public static final ComicBook NEEDLEFACE = registerBase("Needleface", 41, true);
    public static final ComicBook NIGHTPORTER_2020 = registerBase("Nightporter2020", 6, false);
    public static final ComicBook NIGHTPORTER_JR = registerBase("NightporterJr", 16, true);
    public static final ComicBook NIGHTPORTERVS_STEELMAN = registerBase("NightportervsSteelman", 3, false);
    public static final ComicBook NIGHTPORTER_YEAR_ZERO = registerBase("NightporterYearZero", 6, false);
    public static final ComicBook NINJA_ATTACK = registerBase("NinjaAttack", 87, true);
    public static final ComicBook OILY_REVENGER = registerBase("OilyRevenger", 131, true);
    public static final ComicBook OMEGA_DEPARTMENT_HAUNTED_HARGRAVE = registerBase("OmegaDepartmentHauntedHargrave", 3, false);
    public static final ComicBook OMEGA_DEPARTMENT_MYSTERIOUS_MANTELL = registerBase("OmegaDepartmentMysteriousMantell", 3, false);
    public static final ComicBook PEACEDRIVERS = registerBase("Peacedrivers", 25, true);
    public static final ComicBook THE_PINCUSHION = registerBase("ThePincushion", 213, false);
    public static final ComicBook PTIME_HUNTER = registerBase("PTimeHunter", 33, true);
    public static final ComicBook PLASMA_TEAM = registerBase("PlasmaTeam", 16, false);
    public static final ComicBook PLASMA_WOMAN = registerBase("PlasmaWoman", 139, true);
    public static final ComicBook PLUTO_MAN = registerBase("PlutoMan", 86, false);
    public static final ComicBook PROFESSOR_IDIOT = registerBase("ProfessorIdiot", 384, false);
    public static final ComicBook PROFESSOR_IDIOT_BUNGLES_AGAIN = registerBase("ProfessorIdiotBunglesAgain", 8, true);
    public static final ComicBook PUTRID = registerBase("PUTRID", 65, true);
    public static final ComicBook QUEEN_THUNORAOF_LEMURIA = registerBase("QueenThunoraofLemuria", 99, true);
    public static final ComicBook QUEEN_VAMPIRE = registerBase("QueenVampire", 167, false);
    public static final ComicBook REANIMATED_GREEN_BERETS = registerBase("ReanimatedGreenBerets", 18, true);
    public static final ComicBook RETURNOFTHE_NIGHTPORTER = registerBase("ReturnoftheNightporter", 3, false);
    public static final ComicBook REVENGEOFTHE_STOCKBROKER = registerBase("RevengeoftheStockbroker", 0, false);
    public static final ComicBook RIKOTO = registerBase("Rikoto", 0, false);
    public static final ComicBook RIPSCORPMAN = registerBase("RIPScorpman", 6, false);
    public static final ComicBook ROBOBATTLES = registerBase("Robobattles", 38, true);
    public static final ComicBook ROBOT_UNIT = registerBase("RobotUnit", 217, true);
    public static final ComicBook SANTAS_SECRET_ADVENTURES = registerBase("SantasSecretAdventures", 75, false);
    public static final ComicBook SAUCER_SORCERY = registerBase("SaucerSorcery", 0, false);
    public static final ComicBook SCORPMANAND_BLINDMAN = registerBase("ScorpmanandBlindman", 8, false);
    public static final ComicBook SCORPMANANDTHE_ZPEOPLE = registerBase("ScorpmanandtheZPeople", 23, true);
    public static final ComicBook SCORPMAN_DANGERVILLE = registerBase("ScorpmanDangerville", 6, false);
    public static final ComicBook SCORPMAN_THE_DEATHOF_DEE_DERRY = registerBase("ScorpmanTheDeathofDeeDerry", 3, false);
    public static final ComicBook SHE_FRANKENSTEIN = registerBase("SheFrankenstein", 301, true);
    public static final ComicBook SHE_FRANKENSTEIN_SHE_NEANDERTHAL_TEAM_UP = registerBase("SheFrankensteinSheNeanderthalTeamUp", 0, false);
    public static final ComicBook SHIKOKU_MONOGATARI = registerBase("ShikokuMonogatari", 11, false);
    public static final ComicBook SHOGGOTHS_ATTACK = registerBase("ShoggothsAttack", 0, false);
    public static final ComicBook SHOGGOTHS_ATTACK_THE_FINAL_WAR = registerBase("ShoggothsAttackTheFinalWar", 6, true);
    public static final ComicBook SHOGGOTHS_ATTACK_THE_SHOGGOTH_WAR = registerBase("ShoggothsAttackTheShoggothWar", 3, false);
    public static final ComicBook SHONEN = registerBase("Shonen", 153, false);
    public static final ComicBook SHOOTING_IRONS = registerBase("ShootingIrons", 138, false);
    public static final ComicBook SIDEKICK_ADVENTURES = registerBase("SidekickAdventures", 85, false);
    public static final ComicBook SIDEKICK_AVENGERS = registerBase("SidekickAvengers", 12, false);
    public static final ComicBook SIDEKICK_CEMETERY = registerBase("SidekickCemetery", 0, false);
    public static final ComicBook SOLAR_CALCULATOR = registerBase("SolarCalculator", 46, true);
    public static final ComicBook SOLDIERMAN = registerBase("Soldierman", 350, true);
    public static final ComicBook SPACE_WARLOCK = registerBase("SpaceWarlock", 139, true);
    public static final ComicBook STAR_AVENGERS = registerBase("StarAvengers", 0, false);
    public static final ComicBook STEELGRANNY_STORIES = registerBase("SteelgrannyStories", 50, false);
    public static final ComicBook STEELLADY = registerBase("Steellady", 200, true);
    public static final ComicBook STEELLADY_GOESTO_MARS = registerBase("SteelladyGoestoMars", 20, false);
    public static final ComicBook STEELMAN_RETURNS = registerBase("SteelmanReturns", 12, false);
    public static final ComicBook STEELMANVS_SCORPMANVS_THE_NIGHTPORTER = registerBase("SteelmanvsScorpmanvsTheNightporter", 3, false);
    public static final ComicBook STEELMANVS_THE_NIGHTPORTER = registerBase("SteelmanvsTheNightporter", 3, false);
    public static final ComicBook STEELMAN_FORGEDIN_FIRE = registerBase("SteelmanForgedinFire", 0, false);
    public static final ComicBook STEELMAN_THE_IMPOSSIBLE_SUN = registerBase("SteelmanTheImpossibleSun", 12, false);
    public static final ComicBook STEELWEDDING = registerBase("Steelwedding", 3, false);
    public static final ComicBook STIPULATORVS_TERMINATRIX = registerBase("StipulatorvsTerminatrix", 3, false);
    public static final ComicBook SWORDS_FIREBALLS = registerBase("SwordsFireballs", 83, true);
    public static final ComicBook THE_SWORDFIGHTING_NURSE = registerBase("TheSwordfightingNurse", 54, false);
    public static final ComicBook TALESOF_MISFORTUNE = registerBase("TalesofMisfortune", 184, false);
    public static final ComicBook TALESOF_THE_JEWEL_THRONE = registerBase("TalesofTheJewelThrone", 4, false);
    public static final ComicBook TANKTAUR = registerBase("Tanktaur", 25, true);
    public static final ComicBook TECHNOLOGY_HOSPITAL = registerBase("TechnologyHospital", 109, false);
    public static final ComicBook TEEN_STEELMAN = registerBase("TeenSteelman", 82, true);
    public static final ComicBook TEEN_SURGEONS = registerBase("TeenSurgeons", 158, false);
    public static final ComicBook TERMINATRIX_VSATOM_LIZARD = registerBase("TerminatrixVSAtomLizard", 0, false);
    public static final ComicBook THE_BEE_PEOPLE = registerBase("TheBeePeople", 0, false);
    public static final ComicBook THE_CACTUS = registerBase("TheCactus", 174, false);
    public static final ComicBook THE_CERES_INVASION = registerBase("TheCeresInvasion", 8, false);
    public static final ComicBook THE_CRYPTOF_PAIN = registerBase("TheCryptofPain", 294, false);
    public static final ComicBook THE_CURTAIN = registerBase("TheCurtain", 32, true);
    public static final ComicBook THE_CRIMEFIGHTING_DOGS = registerBase("TheCrimefightingDogs", 175, false);
    public static final ComicBook THE_DARKEST_NIGHTPORTER = registerBase("TheDarkestNightporter", 0, false);
    public static final ComicBook THE_FLUMMOXER = registerBase("TheFlummoxer", 53, true);
    public static final ComicBook THE_FOREMAN = registerBase("TheForeman", 165, false);
    public static final ComicBook THE_FURIOUS_FIVE = registerBase("TheFuriousFive", 184, true);
    public static final ComicBook THE_GHOST_BRAWLERS = registerBase("TheGhostBrawlers", 71, true);
    public static final ComicBook THE_HYENA = registerBase("TheHyena", 46, true);
    public static final ComicBook THE_IMPOSSIBLE_STEELMAN = registerBase("TheImpossibleSteelman", 398, true);
    public static final ComicBook THE_INCREDIBLE_SCORPMAN = registerBase("TheIncredibleScorpman", 206, true);
    public static final ComicBook THE_LEOPARD_TOOTHY_TALES = registerBase("TheLeopardToothyTales", 165, true);
    public static final ComicBook THE_MAGIC_HOODIE = registerBase("TheMagicHoodie", 0, false);
    public static final ComicBook THE_MODERATORS = registerBase("TheModerators", 165, true);
    public static final ComicBook THE_MODERATORS_BEYONDTHE_ICE_PALACE = registerBase("TheModeratorsBeyondtheIcePalace", 3, false);
    public static final ComicBook THE_MODERATORS_OLDWORLDS_NEW_WORLD = registerBase("TheModeratorsOldworldsNewWorld", 12, false);
    public static final ComicBook THE_NEW_RISQUE_ADVENTURESOF_MERLIN = registerBase("TheNewRisqueAdventuresofMerlin", 21, false);
    public static final ComicBook THE_NEW_SCORPMAN = registerBase("TheNewScorpman", 15, false);
    public static final ComicBook THE_OMEGA_DEPARTMENT_ARCHIVE = registerBase("TheOmegaDepartmentArchive", 78, true);
    public static final ComicBook THE_SCAVENGER = registerBase("TheScavenger", 150, true);
    public static final ComicBook THE_SEVEN_SUNS = registerBase("TheSevenSuns", 77, false);
    public static final ComicBook THE_SHORTCHANGER = registerBase("TheShortchanger", 37, true);
    public static final ComicBook THE_SPITFIGHTIN_DAMES = registerBase("TheSpitfightinDames", 184, true);
    public static final ComicBook THE_STIPULATOR = registerBase("TheStipulator", 130, true);
    public static final ComicBook THE_STOCKBROKER_DEAD_CAT_BOUNCE = registerBase("TheStockbrokerDeadCatBounce", 3, false);
    public static final ComicBook THE_STOCKBROKER_NEW_INVESTMENTS = registerBase("TheStockbrokerNewInvestments", 12, false);
    public static final ComicBook THE_TERMINATRIX = registerBase("TheTerminatrix", 83, true);
    public static final ComicBook THE_CHURL = registerBase("TheChurl", 30, false);
    public static final ComicBook THE_THOMPSONS_HERBS_NEW_CAR = registerBase("TheThompsonsHerbsNewCar", 0, false);
    public static final ComicBook THE_THOMPSONS_LESTER_GETS_IN_TROUBLE = registerBase("TheThompsonsLesterGetsInTrouble", 0, false);
    public static final ComicBook THE_THOMPSONS_MARIE_THE_TRAIN_DRIVER = registerBase("TheThompsonsMarieTheTrainDriver", 0, false);
    public static final ComicBook THE_UNCOUTH_SQUAD = registerBase("TheUncouthSquad", 40, true);
    public static final ComicBook THE_WARLOCKHAMMER_OF_TANGLEWOOD = registerBase("TheWarlockhammerOfTanglewood", 33, false);
    public static final ComicBook THRILLING_DINOSAUR_STORIES = registerBase("ThrillingDinosaurStories", 204, false);
    public static final ComicBook THUNORATHE_SHE_NEANDERTHAL = registerBase("ThunoratheSheNeanderthal", 267, false);
    public static final ComicBook TODDLER_POLICE = registerBase("ToddlerPolice", 38, true);
    public static final ComicBook TOILET_HUMOR = registerBase("ToiletHumor", 214, true);
    public static final ComicBook TRUE_HAUNTINGS = registerBase("TrueHauntings", 157, true);
    public static final ComicBook TRUE_MERCENARY = registerBase("TrueMercenary", 193, true);
    public static final ComicBook TRUE_NINJA_HISTORY = registerBase("TrueNinjaHistory", 45, false);
    public static final ComicBook ZPEOPLE = registerBase("Zpeople", 216, true);
    public static final ComicBook ZPEOPLE_ATTHE_NORTH_POLE = registerBase("ZpeopleAttheNorthPole", 6, false);
    public static final ComicBook ZPEOPLE_OUTOF_TIME = registerBase("ZpeopleOutofTime", 6, false);
    public static final ComicBook ZPEOPLE_THE_LEOPARD = registerBase("ZpeopleTheLeopard", 3, false);
    public static final ComicBook ZOINKS = registerBase("Zoinks", 315, true);
    public static final ComicBook ZOINKS_AFTER_HOURS_UNABASHED = registerBase("ZoinksAfterHoursUnabashed", 0, false);
    public static final ComicBook UMLAUT = registerBase("UMLAUT", 138, true);
    private final String translationKey;
    private final boolean inPrint;
    private final int issues;

    private ComicBook(String id, int issues, boolean inPrint) {
        this.translationKey = "IGUI_ComicTitle_" + id;
        this.inPrint = inPrint;
        this.issues = issues;
    }

    public static ComicBook get(ResourceLocation id) {
        return Registries.COMIC_BOOK.get(id);
    }

    @Override
    public String toString() {
        return Registries.COMIC_BOOK.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public boolean isInPrint() {
        return this.inPrint;
    }

    public int getIssues() {
        return this.issues;
    }

    public static ComicBook register(String id, int issues, boolean inPrint) {
        return register(false, id, new ComicBook(id, issues, inPrint));
    }

    private static ComicBook registerBase(String id, int issues, boolean inPrint) {
        return register(true, id, new ComicBook(id, issues, inPrint));
    }

    private static ComicBook register(boolean allowDefaultNamespace, String id, ComicBook t) {
        return Registries.COMIC_BOOK.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }

    static {
        if (Core.IS_DEV) {
            for (ComicBook ComicBook : Registries.COMIC_BOOK) {
                TranslationKeyValidator.of(ComicBook.getTranslationKey());
            }
        }
    }
}
