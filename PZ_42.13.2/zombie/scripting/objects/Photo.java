// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Photo {
    public static final Photo A_BABY = registerBase("aBaby");
    public static final Photo A_BATTLEFIELD_MEDIC = registerBase("aBattlefieldMedic");
    public static final Photo A_BATTLEFIELD_NURSE = registerBase("aBattlefieldNurse");
    public static final Photo A_BEAUTIFUL_YOUNG_WOMAN = registerBase("aBeautifulYoungWoman");
    public static final Photo A_BIG_HOUSE = registerBase("aBigHouse");
    public static final Photo A_BIRTH_CERTIFICATE = registerBase("aBirthCertificate");
    public static final Photo A_BRIDE_GETTING_READY_FOR_HER_WEDDING = registerBase("aBrideGettingReadyForHerWedding");
    public static final Photo A_BUILDING_BEING_BUILT = registerBase("aBuildingBeingBuilt");
    public static final Photo A_BUILDING_ON_FIRE = registerBase("aBuildingonFire");
    public static final Photo A_BUSINESS_MARKET = registerBase("aBusyMarket");
    public static final Photo A_BUSY_STREET = registerBase("aBusyStreet");
    public static final Photo A_CABIN = registerBase("aCabin");
    public static final Photo A_CAMP = registerBase("aCamp");
    public static final Photo A_CAR_CRASH = registerBase("aCarCrash");
    public static final Photo A_CARNIVAL = registerBase("aCarnival");
    public static final Photo A_CATTLE_DRIVE = registerBase("aCattleDrive");
    public static final Photo A_CHILD = registerBase("aChild");
    public static final Photo A_CIRCUS = registerBase("aCircus");
    public static final Photo A_CITY = registerBase("aCity");
    public static final Photo A_CIVIL_WAR_BATTLEFIELD = registerBase("aCivilWarBattlefield");
    public static final Photo A_CIVIL_WAR_SOLDIER = registerBase("aCivilWarSoldier");
    public static final Photo A_COUPLE_HOLDING_HANDS = registerBase("aCoupleHoldingHands");
    public static final Photo A_COUPLE_KISSING = registerBase("aCoupleKissing");
    public static final Photo A_COUPLE_WITH_A_BABY = registerBase("aCoupleWithaBaby");
    public static final Photo A_COWBOY = registerBase("aCowboy");
    public static final Photo A_DEAD_BODY = registerBase("aDeadBody");
    public static final Photo A_DEATH_CERTIFICATE = registerBase("aDeathCertificate");
    public static final Photo A_FAMILY = registerBase("aFamily");
    public static final Photo A_FAMILY_CELEBRATING_THANKSGIVING = registerBase("aFamilyCelebratingThanksgiving");
    public static final Photo A_FAMILY_HAVING_CHRISTMAS_DINNER = registerBase("aFamilyHavingChristmasDinner");
    public static final Photo A_FAMOUS_OUTLAW = registerBase("aFamousOutlaw");
    public static final Photo A_FAMOUS_PERSON_FROM_A_LONG_TIME_AGO = registerBase("aFamousPersonFromaLongTimeAgo");
    public static final Photo A_FIRST_WORLD_WAR_SOLDIER = registerBase("aFirstWorldWarSoldier");
    public static final Photo A_FLOOD = registerBase("aFlood");
    public static final Photo A_FORT = registerBase("aFort");
    public static final Photo A_FRONTIER_FAMILY = registerBase("aFrontierFamily");
    public static final Photo A_FRONTIERSMAN = registerBase("aFrontiersman");
    public static final Photo A_GENTLEMAN = registerBase("aGentleman");
    public static final Photo A_GHOST = registerBase("aGhost");
    public static final Photo A_GROOM_GETTING_READY_FOR_HIS_WEDDING = registerBase("aGroomGettingReadyForHisWedding");
    public static final Photo A_GROUP_OF_ABOLITIONISTS = registerBase("aGroupofAbolitionists");
    public static final Photo A_GROUP_OF_CHILDREN = registerBase("aGroupofChildren");
    public static final Photo A_GROUP_OF_CIVIL_WAR_SOLDIERS = registerBase("aGroupofCivilWarSoldiers");
    public static final Photo A_GROUP_OF_COWBOYS = registerBase("aGroupofCowboys");
    public static final Photo A_GROUP_OF_FIRST_WORLD_WAR_SOLDIERS = registerBase("aGroupofFirstWorldWarSoldiers");
    public static final Photo A_GROUP_OF_KOREAN_WAR_SOLDIERS = registerBase("aGroupofKoreanWarSoldiers");
    public static final Photo A_GROUP_OF_MEN = registerBase("aGroupofMen");
    public static final Photo A_GROUP_OF_NATIVE_AMERICANS = registerBase("aGroupofNativeAmericans");
    public static final Photo A_GROUP_OF_PACIFISTS = registerBase("aGroupofPacifists");
    public static final Photo A_GROUP_OF_PEOPLE = registerBase("aGroupofPeople");
    public static final Photo A_GROUP_OF_PEOPLE_IN_BED = registerBase("aGroupofPeopleinBed");
    public static final Photo A_GROUP_OF_PROHIBITIONISTS = registerBase("aGroupofProhibitionists");
    public static final Photo A_GROUP_OF_PROTESTORS = registerBase("aGroupofProtestors");
    public static final Photo A_GROUP_OF_SCHOOLBOYS = registerBase("aGroupofSchoolboys");
    public static final Photo A_GROUP_OF_SCHOOLCHILDREN = registerBase("aGroupofSchoolchildren");
    public static final Photo A_GROUP_OF_SCHOOLGIRLS = registerBase("aGroupofSchoolgirls");
    public static final Photo A_GROUP_OF_SECOND_WORLD_WAR_SOLDIERS = registerBase("aGroupofSecondWorldWarSoldiers");
    public static final Photo A_GROUP_OF_SPIRITUALISTS = registerBase("aGroupofSpiritualists");
    public static final Photo A_GROUP_OF_SUFFRAGETTES = registerBase("aGroupofSuffragettes");
    public static final Photo A_GROUP_OF_UNCLOTHED_PEOPLE = registerBase("aGroupofUnclothedPeople");
    public static final Photo A_GROUP_OF_UNUSUAL_PLANTS = registerBase("aGroupofUnusualPlants");
    public static final Photo A_GROUP_OF_WOMEN = registerBase("aGroupofWomen");
    public static final Photo A_GROUP_OF_YOUNG_PEOPLE = registerBase("aGroupofYoungPeople");
    public static final Photo A_GRUESOME_SCENE = registerBase("aGruesomeScene");
    public static final Photo A_GUN = registerBase("aGun");
    public static final Photo A_HANDSOME_YOUNG_MAN = registerBase("aHandsomeYoungMan");
    public static final Photo A_HOMESTEADER_FAMILY = registerBase("aHomesteaderFamily");
    public static final Photo A_HORSE_DRAWING_A_PLOW = registerBase("aHorseDrawingaPlow");
    public static final Photo A_HORSE_RACE = registerBase("aHorseRace");
    public static final Photo A_HORSEDRAWN_CARRIAGE_ARRIVING_AT_A_CHURCH = registerBase("aHorsedrawnCarriageArrivingataChurch");
    public static final Photo A_HOUSE = registerBase("aHouse");
    public static final Photo A_HOUSE_BEING_BUILT = registerBase("aHouseBeingBuilt");
    public static final Photo A_HUNTER = registerBase("aHunter");
    public static final Photo A_KOREAN_WAR_SOLDIER = registerBase("aKoreanWarSoldier");
    public static final Photo A_LADY = registerBase("aLady");
    public static final Photo A_LANDMARK_BEING_BUILT = registerBase("aLandmarkBeingBuilt");
    public static final Photo A_LANDSCAPE = registerBase("aLandscape");
    public static final Photo A_LARGE_PUBLIC_EVENT = registerBase("aLargePublicEvent");
    public static final Photo A_LEADER = registerBase("aLeader");
    public static final Photo A_LICENSE_PLATE = registerBase("aLicensePlate");
    public static final Photo A_MAN = registerBase("aMan");
    public static final Photo A_MAN_ON_A_BICYCLE = registerBase("aManonaBicycle");
    public static final Photo A_MAN_WITH_A_LARGE_MUSTACHE = registerBase("aManwithaLargeMustache");
    public static final Photo A_MAN_WITH_A_LONG_BEARD = registerBase("aManwithaLongBeard");
    public static final Photo A_MARRIAGE_CERTIFICATE = registerBase("aMarriageCertificate");
    public static final Photo A_MILITARY_CAMP = registerBase("aMilitaryCamp");
    public static final Photo A_MILITARY_OFFICER = registerBase("aMilitaryOfficer");
    public static final Photo A_MISSING_PERSON = registerBase("aMissingPerson");
    public static final Photo A_MUGSHOT = registerBase("aMugshot");
    public static final Photo A_NATIVE_AMERICAN = registerBase("aNativeAmerican");
    public static final Photo A_NERVOUS_PERSON = registerBase("aNervousPerson");
    public static final Photo A_NINETEENTH_CENTURY_FAMILY = registerBase("aNineteenthCenturyFamily");
    public static final Photo A_OUTLAW = registerBase("anOutlaw");
    public static final Photo A_PADDLE_STEAMER_ON_THE_OHIO = registerBase("aPaddleSteamerontheOhio");
    public static final Photo A_PARADE = registerBase("aParade");
    public static final Photo A_PATERNITY_TEST = registerBase("aPaternityTest");
    public static final Photo A_PERSON_IN_A_COMPROMISING_POSITION = registerBase("aPersoninaCompromisingPosition");
    public static final Photo A_PERSON_WHO_IS_TIED_UP = registerBase("aPersonWho'sTiedUp");
    public static final Photo A_PERSON_WITH_CROSSHAIRS_ON_THEIR_FACE = registerBase("aPersonwithCrosshairsonTheirFace");
    public static final Photo A_PERSON_WITH_THEIR_FACE_CROSSED_OUT = registerBase("aPersonWithTheirFaceCrossedOut");
    public static final Photo A_PET = registerBase("aPet");
    public static final Photo A_PILE_OF_CASH = registerBase("aPileofCash");
    public static final Photo A_POLICE_OFFICER = registerBase("aPoliceOfficer");
    public static final Photo A_POLITICAL_MEETING = registerBase("aPoliticalMeeting");
    public static final Photo A_POLITICIAN = registerBase("aPolitician");
    public static final Photo A_PRESIDENT = registerBase("aPresident");
    public static final Photo A_RELIGIOUS_LEADER = registerBase("aReligiousLeader");
    public static final Photo A_RELIGIOUS_SERVICE = registerBase("aReligiousService");
    public static final Photo A_ROMANTIC_NATURE = registerBase("aRomanticNature");
    public static final Photo A_RUGGED_CABIN = registerBase("aRuggedCabin");
    public static final Photo A_SAILING_SHIP = registerBase("aSailingShip");
    public static final Photo A_SALOON = registerBase("aSaloon");
    public static final Photo A_SEANCE = registerBase("aSeance");
    public static final Photo A_SECOND_WORLD_WAR_SOLDIER = registerBase("aSecondWorldWarSoldier");
    public static final Photo A_SHERIFF = registerBase("aSheriff");
    public static final Photo A_SMALL_HOUSE = registerBase("aSmallHouse");
    public static final Photo A_SPORTS_GAME = registerBase("aSportsGame");
    public static final Photo A_STEAM_TRAIN = registerBase("aSteamTrain");
    public static final Photo A_STEAMSHIP = registerBase("aSteamship");
    public static final Photo A_STREET_OF_WOODEN_BUILDINGS = registerBase("aStreetofWoodenBuildings");
    public static final Photo A_SUSPICIOUS_GROUP_OF_PEOPLE = registerBase("aSuspiciousGroupofPeople");
    public static final Photo A_SUSPICIOUS_MEETING = registerBase("aSuspiciousMeeting");
    public static final Photo A_SUSPICIOUS_OBJECT = registerBase("aSuspiciousObject");
    public static final Photo A_SUSPICIOUS_PERSON = registerBase("aSuspiciousPerson");
    public static final Photo A_TEENAGER = registerBase("aTeenager");
    public static final Photo A_TOWN = registerBase("aTown");
    public static final Photo A_TRAIN_STATION_IN_THE_OLD_DAYS = registerBase("aTrainStationintheOldDays");
    public static final Photo A_TYPICAL_WESTERN_SCENE = registerBase("aTypicalWesternScene");
    public static final Photo A_VACATION = registerBase("aVacation");
    public static final Photo A_WAGON_TRAIN = registerBase("aWagonTrain");
    public static final Photo A_WANTED_FUGITIVE = registerBase("aWantedFugitive");
    public static final Photo A_WEDDING = registerBase("aWedding");
    public static final Photo A_WELL_BUILT_CABIN = registerBase("aWellBuiltCabin");
    public static final Photo A_WOMAN = registerBase("aWoman");
    public static final Photo A_WOMAN_ON_A_BICYCLE = registerBase("aWomanonaBicycle");
    public static final Photo A_WOMAN_WITH_A_HUGE_HAT = registerBase("aWomanwithaHugeHat");
    public static final Photo A_YOUNG_COUPLE = registerBase("aYoungCouple");
    public static final Photo A_YOUNG_MAN = registerBase("aYoungMan");
    public static final Photo A_YOUNG_WOMAN = registerBase("aYoungWoman");
    public static final Photo AN_ARTICLE_ABOUT_A_CRIME = registerBase("anArticleAboutaCrime");
    public static final Photo AN_ILLICIT_NATURE = registerBase("anIllicitNature");
    public static final Photo AN_OUTDATED_PIECE_OF_TECHNOLOGY = registerBase("anOutdatedPieceofTechnology");
    public static final Photo AN_UNCLOTHED_COUPLE = registerBase("anUnclothedCouple");
    public static final Photo CASH = registerBase("Cash");
    public static final Photo CHILDREN_PLAYING = registerBase("ChildrenPlaying");
    public static final Photo IMMIGRANTS = registerBase("Immigrants");
    public static final Photo IMMIGRANTS_IN_THEIR_NATIVE_DRESS = registerBase("ImmigrantsintheirNativeDress");
    public static final Photo LOUISVILLE = registerBase("Louisville");
    public static final Photo MINERS = registerBase("Miners");
    public static final Photo PEOPLE_DANCING_AT_A_WEDDING = registerBase("PeopleDancingataWedding");
    public static final Photo PEOPLE_DRESSED_UP = registerBase("PeopleDressedUp");
    public static final Photo PEOPLE_FARMING = registerBase("PeopleFarming");
    public static final Photo PEOPLE_ON_A_HORSE_DRAWN_BUGGY = registerBase("PeopleonaHorseDrawnBuggy");
    public static final Photo PEOPLE_PLAYING_BASEBALL = registerBase("PeoplePlayingBaseball");
    public static final Photo PEOPLE_PLAYING_FOOTBALL = registerBase("PeoplePlayingFootball");
    public static final Photo PEOPLE_SITTING_TOGETHER = registerBase("PeopleSittingTogether");
    public static final Photo PEOPLE_STANDING_TOGETHER = registerBase("PeopleStandingTogether");
    public static final Photo PEOPLE_WITH_AN_EARLY_MOTORCAR = registerBase("PeopleWithanEarlyMotorcar");
    public static final Photo PEOPLE_WORKING = registerBase("PeopleWorking");
    public static final Photo PEOPLE_WORKING_IN_A_FACTORY = registerBase("PeopleWorkinginaFactory");
    public static final Photo PRISONERS = registerBase("Prisoners");
    public static final Photo SECURITY_FOOTAGE = registerBase("SecurityFootage");
    public static final Photo SOME_DUBIOUS_DOCUMENTS = registerBase("SomeDubiousDocuments");
    public static final Photo SOMEONE_BEING_ARRESTED = registerBase("SomeoneBeingArrested");
    public static final Photo SOMEONE_COMMITTING_ILL_DEEDS = registerBase("SomeoneCommittingIllDeeds");
    public static final Photo SOMEONE_CONSUMING_A_SUSPICIOUS_SUBSTANCE = registerBase("SomeoneConsumingaSuspiciousSubstance");
    public static final Photo SOMEONE_FIRING_A_GUN = registerBase("SomeoneFiringaGun");
    public static final Photo SOMEONE_FORGOTTEN = registerBase("SomeoneForgotten");
    public static final Photo SOMEONE_HANDING_OVER_AN_ENVELOPE = registerBase("SomeoneHandingOveranEnvelope");
    public static final Photo SOMEONE_HOLDING_A_VERY_LARGE_VEGETABLE = registerBase("SomeoneHoldingaVeryLargeVegetable");
    public static final Photo SOMEONE_RECEIVING_A_BRIEFCASE = registerBase("SomeoneReceivingaBriefcase");
    public static final Photo SOMEONE_RECEIVING_A_PACKAGE = registerBase("SomeoneReceivingaPackage");
    public static final Photo SOMEONE_TRYING_TO_HIDE = registerBase("SomeoneTryingtoHide");
    public static final Photo SOMEONE_UNCLOTHED = registerBase("SomeoneUnclothed");
    public static final Photo SOMEONES_ANCESTORS = registerBase("SomeonesAncestors");
    public static final Photo SOMETHING_SAUCY = registerBase("SomethingSaucy");
    public static final Photo SOMETHING_TOO_FADED_TO_MAKE_OUT = registerBase("SomethingTooFadedtoMakeOut");
    public static final Photo SOMETHING_TOO_STAINED_TO_MAKE_OUT = registerBase("SomethingTooStainedtoMakeOut");
    public static final Photo THREE_PEOPLE_IN_BED = registerBase("ThreePeopleinBed");
    public static final Photo TWO_MEN_KISSING = registerBase("TwoMenKissing");
    public static final Photo TWO_PEOPLE_IN_BED = registerBase("TwoPeopleinBed");
    public static final Photo TWO_PEOPLE_KISSING = registerBase("TwoPeopleKissing");
    public static final Photo TWO_PEOPLE_SHAKING_HANDS = registerBase("TwoPeopleShakingHands");
    public static final Photo TWO_WOMEN_KISSING = registerBase("TwoWomenKissing");
    private final String translationKey;

    private Photo(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Photo get(ResourceLocation id) {
        return Registries.PHOTO.get(id);
    }

    @Override
    public String toString() {
        return Registries.PHOTO.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Photo register(String id) {
        return register(false, id);
    }

    private static Photo registerBase(String id) {
        return register(true, id);
    }

    private static Photo register(boolean allowDefaultNamespace, String id) {
        return Registries.PHOTO.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Photo(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Photo photo : Registries.PHOTO) {
                TranslationKeyValidator.of(photo.getTranslationKey());
            }
        }
    }
}
