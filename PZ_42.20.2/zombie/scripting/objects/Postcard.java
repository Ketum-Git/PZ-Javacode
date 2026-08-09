// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Postcard {
    public static final Postcard ALASKA = registerBase("Alaska");
    public static final Postcard ALCATRAZ = registerBase("Alcatraz");
    public static final Postcard AMSTERDAM = registerBase("Amsterdam");
    public static final Postcard AN_AFRICAN_SAFARI = registerBase("anAfricanSafari");
    public static final Postcard ANTARCTICA = registerBase("Antarctica");
    public static final Postcard ARGENTINA = registerBase("Argentina");
    public static final Postcard ASIA = registerBase("Asia");
    public static final Postcard ASPEN = registerBase("Aspen");
    public static final Postcard ATHENS = registerBase("Athens");
    public static final Postcard ATLANTA = registerBase("Atlanta");
    public static final Postcard AUSTRALIA = registerBase("Australia");
    public static final Postcard AUSTRIA = registerBase("Austria");
    public static final Postcard AYERS_ROCK = registerBase("AyersRock");
    public static final Postcard BALI = registerBase("Bali");
    public static final Postcard BARCELONA = registerBase("Barcelona");
    public static final Postcard BATON_ROUGE = registerBase("BatonRouge");
    public static final Postcard BEACON_HILL = registerBase("BeaconHill");
    public static final Postcard BEIJING = registerBase("Beijing");
    public static final Postcard BERLIN = registerBase("Berlin");
    public static final Postcard BERMUDA = registerBase("Bermuda");
    public static final Postcard BIG_SUR = registerBase("BigSur");
    public static final Postcard BOSTON = registerBase("Boston");
    public static final Postcard BRAZIL = registerBase("Brazil");
    public static final Postcard BROOKLYN = registerBase("Brooklyn");
    public static final Postcard BUCKINGHAM_PALACE = registerBase("BuckinghamPalace");
    public static final Postcard BUENOS_AIRES = registerBase("BuenosAires");
    public static final Postcard CAIRO = registerBase("Cairo");
    public static final Postcard CALCUTTA = registerBase("Calcutta");
    public static final Postcard CALIFORNIA = registerBase("California");
    public static final Postcard CAMBODIA = registerBase("Cambodia");
    public static final Postcard CANADA = registerBase("Canada");
    public static final Postcard CAPE_CANAVERAL = registerBase("CapeCanaveral");
    public static final Postcard CAPE_COD = registerBase("CapeCod");
    public static final Postcard CARNEGIE_HALL = registerBase("CarnegieHall");
    public static final Postcard CHICAGO = registerBase("Chicago");
    public static final Postcard CHINA = registerBase("China");
    public static final Postcard CLEVELAND = registerBase("Cleveland");
    public static final Postcard COLONIAL_WILLIAMSBURG = registerBase("ColonialWilliamsburg");
    public static final Postcard COLORADO = registerBase("Colorado");
    public static final Postcard CONEY_ISLAND = registerBase("ConeyIsland");
    public static final Postcard COPENHAGEN = registerBase("Copenhagen");
    public static final Postcard COSTA_RICA = registerBase("CostaRica");
    public static final Postcard CUMBERLAND_FALLS = registerBase("CumberlandFalls");
    public static final Postcard CYPRUS = registerBase("Cyprus");
    public static final Postcard CZECH_REPUBLIC = registerBase("CzechRepublic");
    public static final Postcard CZECHOSLOVAKIA = registerBase("Czechoslovakia");
    public static final Postcard DALLAS = registerBase("Dallas");
    public static final Postcard DEATH_VALLEY = registerBase("DeathValley");
    public static final Postcard DUBLIN = registerBase("Dublin");
    public static final Postcard EASTER_ISLAND = registerBase("EasterIsland");
    public static final Postcard EDINBURGH = registerBase("Edinburgh");
    public static final Postcard EGYPT = registerBase("Egypt");
    public static final Postcard EL_CAPITAN = registerBase("ElCapitan");
    public static final Postcard ELLIS_ISLAND = registerBase("EllisIsland");
    public static final Postcard ENGLAND = registerBase("England");
    public static final Postcard EUROPE = registerBase("Europe");
    public static final Postcard FANEUIL_HALL = registerBase("FaneuilHall");
    public static final Postcard FINLAND = registerBase("Finland");
    public static final Postcard FLORENCE = registerBase("Florence");
    public static final Postcard FLORIDA = registerBase("Florida");
    public static final Postcard FORT_INDEPENDENCE = registerBase("FortIndependence");
    public static final Postcard FORT_SUMTER = registerBase("FortSumter");
    public static final Postcard FRANCE = registerBase("France");
    public static final Postcard GALLERIA_DELL_ACCADEMIA = registerBase("GalleriadellAccademia");
    public static final Postcard GERMANY = registerBase("Germany");
    public static final Postcard GETTYSBURG = registerBase("Gettysburg");
    public static final Postcard GIBRALTAR = registerBase("Gibraltar");
    public static final Postcard GLEN_CANYON = registerBase("GlenCanyon");
    public static final Postcard GOLDEN_GATE_PARK = registerBase("GoldenGatePark");
    public static final Postcard GREAT_BARRIER_REEF = registerBase("GreatBarrierReef");
    public static final Postcard GREECE = registerBase("Greece");
    public static final Postcard GRIFFITH_OBSERVATORY = registerBase("GriffithObservatory");
    public static final Postcard HA_LONG_BAY = registerBase("HaLongBay");
    public static final Postcard HAITI = registerBase("Haiti");
    public static final Postcard HANOI = registerBase("Hanoi");
    public static final Postcard HAWAII = registerBase("Hawaii");
    public static final Postcard HO_CHI_MINH_CITY = registerBase("HoChiMinhCity");
    public static final Postcard HOLLYWOOD = registerBase("Hollywood");
    public static final Postcard HONG_KONG = registerBase("HongKong");
    public static final Postcard HONOLULU = registerBase("Honolulu");
    public static final Postcard ICELAND = registerBase("Iceland");
    public static final Postcard ILLINOIS = registerBase("Illinois");
    public static final Postcard INDIA = registerBase("India");
    public static final Postcard INDIANA = registerBase("Indiana");
    public static final Postcard INDONESIA = registerBase("Indonesia");
    public static final Postcard IRELAND = registerBase("Ireland");
    public static final Postcard ISTANBUL = registerBase("Istanbul");
    public static final Postcard ITALY = registerBase("Italy");
    public static final Postcard JAMAICA = registerBase("Jamaica");
    public static final Postcard JAMESTOWN = registerBase("Jamestown");
    public static final Postcard JAPAN = registerBase("Japan");
    public static final Postcard JERUSALEM = registerBase("Jerusalem");
    public static final Postcard JOSHUA_TREE_NATIONAL_PARK = registerBase("JoshuaTreeNationalPark");
    public static final Postcard KENYA = registerBase("Kenya");
    public static final Postcard KEY_WEST = registerBase("KeyWest");
    public static final Postcard KILLARNEY = registerBase("Killarney");
    public static final Postcard KINGSMOUTH_ISLAND = registerBase("KingsmouthIsland");
    public static final Postcard LAKE_BAIKAL = registerBase("LakeBaikal");
    public static final Postcard LAKE_COMO = registerBase("LakeComo");
    public static final Postcard LAKE_GARDA = registerBase("LakeGarda");
    public static final Postcard LAKE_LUCERNE = registerBase("LakeLucerne");
    public static final Postcard LAKE_MEAD = registerBase("LakeMead");
    public static final Postcard LAKE_PLACID = registerBase("LakePlacid");
    public static final Postcard LAKE_SUPERIOR = registerBase("LakeSuperior");
    public static final Postcard LAKE_TAHOE = registerBase("LakeTahoe");
    public static final Postcard LAKE_TITICACA = registerBase("LakeTiticaca");
    public static final Postcard LAND_BETWEEN_THE_LAKES = registerBase("LandBetweentheLakes");
    public static final Postcard LAPLAND = registerBase("Lapland");
    public static final Postcard LAS_VEGAS = registerBase("LasVegas");
    public static final Postcard LISBON = registerBase("Lisbon");
    public static final Postcard LOCH_NESS = registerBase("LochNess");
    public static final Postcard LONDON = registerBase("London");
    public static final Postcard LOS_ANGELES = registerBase("LosAngeles");
    public static final Postcard LOUISIANA = registerBase("Louisiana");
    public static final Postcard MACHU_PICCHU = registerBase("MachuPicchu");
    public static final Postcard MADRID = registerBase("Madrid");
    public static final Postcard MALAYSIA = registerBase("Malaysia");
    public static final Postcard MALDIVES = registerBase("Maldives");
    public static final Postcard MALIBU = registerBase("Malibu");
    public static final Postcard MALTA = registerBase("Malta");
    public static final Postcard MANHATTAN = registerBase("Manhattan");
    public static final Postcard MARENGO_CAVE = registerBase("MarengoCave");
    public static final Postcard MARTHAS_VINEYARD = registerBase("MarthasVineyard");
    public static final Postcard MASSACHUSETTS = registerBase("Massachusetts");
    public static final Postcard MAUI = registerBase("Maui");
    public static final Postcard MEMPHIS = registerBase("Memphis");
    public static final Postcard MESA_VERDE = registerBase("MesaVerde");
    public static final Postcard MEXICO = registerBase("Mexico");
    public static final Postcard MIAMI = registerBase("Miami");
    public static final Postcard MIAMI_BEACH = registerBase("MiamiBeach");
    public static final Postcard MILWAUKEE = registerBase("Milwaukee");
    public static final Postcard MINNESOTA = registerBase("Minnesota");
    public static final Postcard MISSOURI = registerBase("Missouri");
    public static final Postcard MONACO = registerBase("Monaco");
    public static final Postcard MONGOLIA = registerBase("Mongolia");
    public static final Postcard MONROE_LAKE = registerBase("MonroeLake");
    public static final Postcard MONROEVILLE = registerBase("Monroeville");
    public static final Postcard MONT_BLANC = registerBase("MontBlanc");
    public static final Postcard MONT_SANT_MICHEL = registerBase("MontSaintMichel");
    public static final Postcard MONTANA = registerBase("Montana");
    public static final Postcard MONTICELLO = registerBase("Monticello");
    public static final Postcard MONTREAL = registerBase("Montreal");
    public static final Postcard MONTSERRAT = registerBase("Montserrat");
    public static final Postcard MONUMENT_VALLEY = registerBase("MonumentValley");
    public static final Postcard MOROCCO = registerBase("Morocco");
    public static final Postcard MOSCOW = registerBase("Moscow");
    public static final Postcard MOUNT_EVERST = registerBase("MountEverest");
    public static final Postcard MOUNT_FUJI = registerBase("MountFuji");
    public static final Postcard MOUNT_KILIMANJARO = registerBase("MountKilimanjaro");
    public static final Postcard MOUNT_MCKINLEY = registerBase("MountMcKinley");
    public static final Postcard MOUNT_OLYMPUS = registerBase("MountOlympus");
    public static final Postcard MOUNT_RAINIER = registerBase("MountRainier");
    public static final Postcard MOUNT_RUSHMORE = registerBase("MountRushmore");
    public static final Postcard MOUNT_VERNON = registerBase("MountVernon");
    public static final Postcard MOUNT_WILLIAMSON = registerBase("MountWilliamson");
    public static final Postcard MOUNTAIN_LION_PICTURES_STUDIO = registerBase("MountainLionPicturesStudio");
    public static final Postcard MY_OLD_KENTUCKY_HOME_PARK = registerBase("MyOldKentuckyHomePark");
    public static final Postcard NASHVILLE = registerBase("Nashville");
    public static final Postcard NEUSCHWANSTEIN_CASTLE = registerBase("NeuschwansteinCastle");
    public static final Postcard NEW_ORLEANS = registerBase("NewOrleans");
    public static final Postcard NEW_YORK = registerBase("NewYork");
    public static final Postcard NEW_ZEALAND = registerBase("NewZealand");
    public static final Postcard NEWCASTLE = registerBase("Newcastle");
    public static final Postcard NEWGRANGE = registerBase("Newgrange");
    public static final Postcard NIAGARA_FALLS = registerBase("NiagaraFalls");
    public static final Postcard NORWAY = registerBase("Norway");
    public static final Postcard OHIO = registerBase("Ohio");
    public static final Postcard OKLAHOMA = registerBase("Oklahoma");
    public static final Postcard PARIS = registerBase("Paris");
    public static final Postcard PEARL_HARBOR = registerBase("PearlHarbor");
    public static final Postcard PETRA = registerBase("Petra");
    public static final Postcard PHILADELPHIA = registerBase("Philadelphia");
    public static final Postcard PISA = registerBase("Pisa");
    public static final Postcard POMPEII = registerBase("Pompeii");
    public static final Postcard PORTUGAL = registerBase("Portugal");
    public static final Postcard PRAGUE = registerBase("Prague");
    public static final Postcard PUERTO_RICO = registerBase("PuertoRico");
    public static final Postcard QUEBEC = registerBase("Quebec");
    public static final Postcard RALEIGH = registerBase("Raleigh");
    public static final Postcard RED_ROCK_CANYON = registerBase("RedRockCanyon");
    public static final Postcard RICHMOND = registerBase("Richmond");
    public static final Postcard RIO_DE_JANEIRO = registerBase("RiodeJaneiro");
    public static final Postcard ROME = registerBase("Rome");
    public static final Postcard ROSWELL = registerBase("Roswell");
    public static final Postcard ROUTE_66 = registerBase("Route66");
    public static final Postcard RUSSIA = registerBase("Russia");
    public static final Postcard SAINT_PETERSBURG = registerBase("SaintPetersburg");
    public static final Postcard SALEM = registerBase("Salem");
    public static final Postcard SAN_DIEGO = registerBase("SanDiego");
    public static final Postcard SAN_FRANCISCO = registerBase("SanFrancisco");
    public static final Postcard SANTA_CATALINA_ISLAND = registerBase("SantaCatalinaIsland");
    public static final Postcard SAUDI_ARABIA = registerBase("SaudiArabia");
    public static final Postcard SCOTLAND = registerBase("Scotland");
    public static final Postcard SEATTLE = registerBase("Seattle");
    public static final Postcard SEDONA = registerBase("Sedona");
    public static final Postcard SEOUL = registerBase("Seoul");
    public static final Postcard SHANGHAI = registerBase("Shanghai");
    public static final Postcard SIERRA_NEVADA = registerBase("SierraNevada");
    public static final Postcard SINGAPORE = registerBase("Singapore");
    public static final Postcard SOUTH_AFRICA = registerBase("SouthAfrica");
    public static final Postcard SOUTH_AMERICA = registerBase("SouthAmerica");
    public static final Postcard SOUTH_KOREA = registerBase("SouthKorea");
    public static final Postcard SPAIN = registerBase("Spain");
    public static final Postcard SPIFFO_WORLD = registerBase("SpiffoWorld");
    public static final Postcard STONEHENGE = registerBase("Stonehenge");
    public static final Postcard SWEDEN = registerBase("Sweden");
    public static final Postcard SWITZERLAND = registerBase("Switzerland");
    public static final Postcard SYDNEY = registerBase("Sydney");
    public static final Postcard TAIWAN = registerBase("Taiwan");
    public static final Postcard TANZANIA = registerBase("Tanzania");
    public static final Postcard TENNESSEE = registerBase("Tennessee");
    public static final Postcard TEXAS = registerBase("Texas");
    public static final Postcard THAILAND = registerBase("Thailand");
    public static final Postcard THE_ALAMO = registerBase("theAlamo");
    public static final Postcard THE_ALPS = registerBase("theAlps");
    public static final Postcard THE_AMAZON = registerBase("theAmazon");
    public static final Postcard THE_AMERICAN_GUN_MUSEUM = registerBase("theAmericanGunMuseum");
    public static final Postcard THE_AMERICAN_HISTORY_MUSEUM = registerBase("theAmericanHistoryMuseum");
    public static final Postcard THE_AMERICAN_MEDIA_MUSEUM = registerBase("theAmericanMediaMuseum");
    public static final Postcard THE_AMERICAN_MUSIC_HALL_OF_FAME = registerBase("theAmericanMusicHallofFame");
    public static final Postcard THE_AMERICAN_WWII_MUSEUM = registerBase("theAmericanWWIIMuseum");
    public static final Postcard THE_ANDES = registerBase("theAndes");
    public static final Postcard THE_APPALACHIAN_TRAIL = registerBase("theAppalachianTrail");
    public static final Postcard THE_APPALACHIANS = registerBase("theAppalachians");
    public static final Postcard THE_AUSTIN_PARKER_MANSION = registerBase("theAustinParkerMansion");
    public static final Postcard THE_AUSTRALIAN_OUTBACK = registerBase("theAustralianOutback");
    public static final Postcard THE_BAHAMAS = registerBase("theBahamas");
    public static final Postcard THE_BRITISH_MUSEUM = registerBase("theBritishMuseum");
    public static final Postcard THE_BRONX = registerBase("theBronx");
    public static final Postcard THE_CALIFORNIA_SCIENCE_MUSEUM = registerBase("theCaliforniaScienceMuseum");
    public static final Postcard THE_CARIBBEAN = registerBase("theCaribbean");
    public static final Postcard THE_CHEVALIER_MUSEUM = registerBase("theChevalierMuseum");
    public static final Postcard THE_CINQUE_TERRE = registerBase("theCinqueTerre");
    public static final Postcard THE_COLOSSEUM = registerBase("theColosseum");
    public static final Postcard THE_DEAD_SEA = registerBase("theDeadSea");
    public static final Postcard THE_EMPIRE_STATE_BUILDING = registerBase("theEmpireStateBuilding");
    public static final Postcard THE_EVERGLADES = registerBase("theEverglades");
    public static final Postcard THE_FORBIDDEN_CITY = registerBase("theForbiddenCity");
    public static final Postcard THE_FRANKIE_MONRO_MUSEUM = registerBase("theFrankieMonroMuseum");
    public static final Postcard THE_GALAPAGOS_ISLANDS = registerBase("theGalapagosIslands");
    public static final Postcard THE_GRAND_CANYON = registerBase("theGrandCanyon");
    public static final Postcard THE_GREAT_SMOKY_MOUNTAINS = registerBase("theGreatSmokyMountains");
    public static final Postcard THE_GREAT_WALL_OF_CHINA = registerBase("theGreatWallofChina");
    public static final Postcard THE_HAGIA_SOPHIA = registerBase("theHagiaSophia");
    public static final Postcard THE_HANK_GILMAN_MUSEUM = registerBase("theHankGilmanMuseum");
    public static final Postcard THE_HIMALAYAS = registerBase("theHimalayas");
    public static final Postcard THE_HOOVER_DAM = registerBase("theHooverDam");
    public static final Postcard THE_KLAMATH_MOUNTAINS = registerBase("theKlamathMountains");
    public static final Postcard THE_LINCOLN_MEMORIAL = registerBase("theLincolnMemorial");
    public static final Postcard THE_LOUVRE = registerBase("theLouvre");
    public static final Postcard THE_MATTERHORN = registerBase("theMatterhorn");
    public static final Postcard THE_MODERN_ART_MUSEUM = registerBase("theModernArtMuseum");
    public static final Postcard THE_MOJAVE = registerBase("theMojave");
    public static final Postcard THE_NATIONAL_AIR_AND_SPACE_MUSEUM = registerBase("theNationalAirandSpaceMuseum");
    public static final Postcard THE_NATIONAL_ART_GALLERY = registerBase("theNationalArtGallery");
    public static final Postcard THE_NATIONAL_BASEBALL_MUSEUM = registerBase("theNationalBaseballMuseum");
    public static final Postcard THE_NATIONAL_BASKETBALL_MUSEUM = registerBase("theNationalBasketballMuseum");
    public static final Postcard THE_NATIONAL_CAR_MUSEUM = registerBase("theNationalCarMuseum");
    public static final Postcard THE_NATIONAL_FOOTBALL_MUSEUM = registerBase("theNationalFootballMuseum");
    public static final Postcard THE_NATIONAL_NATURAL_HISTORY_MUSEUM = registerBase("theNationalNaturalHistoryMuseum");
    public static final Postcard THE_NORTH_POLE = registerBase("theNorthPole");
    public static final Postcard THE_OSCC_HALL_OF_FAME = registerBase("theOSCCHallofFame");
    public static final Postcard THE_PACIFIC_COAST_HIGHWAY = registerBase("thePacificCoastHighway");
    public static final Postcard THE_PALACE_OF_VERSAILLES = registerBase("thePalaceofVersailles");
    public static final Postcard THE_PYRAMIDS_AT_GIZA = registerBase("thePyramidsatGiza");
    public static final Postcard THE_RED_SEA = registerBase("theRedSea");
    public static final Postcard THE_RMS_QUEEN_MARY = registerBase("theRMSQueenMary");
    public static final Postcard THE_ROCKIES = registerBase("theRockies");
    public static final Postcard THE_SOVIET_UNION = registerBase("theSovietUnion");
    public static final Postcard THE_STATUE_OF_LIBERTY = registerBase("theStatueofLiberty");
    public static final Postcard THE_TAJ_MAHAL = registerBase("theTajMahal");
    public static final Postcard THE_UFFIZI = registerBase("theUffizi");
    public static final Postcard THE_US_CAPITOL = registerBase("theUSCapitol");
    public static final Postcard THE_VIRGINIA_PATTERSON_MUSEUM = registerBase("theVirginiaPattersonMuseum");
    public static final Postcard THE_WASHINGTON_MONUMENT = registerBase("theWashingtonMonument");
    public static final Postcard THE_WHITE_HOUSE = registerBase("theWhiteHouse");
    public static final Postcard THE_ZOCALO = registerBase("theZocalo");
    public static final Postcard TIBET = registerBase("Tibet");
    public static final Postcard TIJUANA = registerBase("Tijuana");
    public static final Postcard TIMES_SQUARE = registerBase("TimesSquare");
    public static final Postcard TOKYO = registerBase("Tokyo");
    public static final Postcard TORONTO = registerBase("Toronto");
    public static final Postcard TURKEY = registerBase("Turkey");
    public static final Postcard UKRAINE = registerBase("Ukraine");
    public static final Postcard URUGUAY = registerBase("Uruguay");
    public static final Postcard VALLEY_FORGE = registerBase("ValleyForge");
    public static final Postcard VANCOUVER = registerBase("Vancouver");
    public static final Postcard VATICAN_CITY = registerBase("VaticanCity");
    public static final Postcard VENICE = registerBase("Venice");
    public static final Postcard VENICE_BEACH = registerBase("VeniceBeach");
    public static final Postcard VICTORIA_FALLS = registerBase("VictoriaFalls");
    public static final Postcard VIETNAM = registerBase("Vietnam");
    public static final Postcard VIRGINIA = registerBase("Virginia");
    public static final Postcard WASHINGTON_DC = registerBase("WashingtonDC");
    public static final Postcard WEST_VIRGINIA = registerBase("WestVirginia");
    public static final Postcard WISCONSIN = registerBase("Wisconsin");
    public static final Postcard YELLOWSTONE = registerBase("Yellowstone");
    public static final Postcard YOSEMITE = registerBase("Yosemite");
    public static final Postcard YUGOSLAVIA = registerBase("Yugoslavia");
    public static final Postcard ZION_NATIONAL_PARK = registerBase("ZionNationalPark");
    private final String translationKey;

    private Postcard(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Postcard get(ResourceLocation id) {
        return Registries.POSTCARD.get(id);
    }

    @Override
    public String toString() {
        return Registries.POSTCARD.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Postcard register(String id) {
        return register(false, id);
    }

    private static Postcard registerBase(String id) {
        return register(true, id);
    }

    private static Postcard register(boolean allowDefaultNamespace, String id) {
        return Registries.POSTCARD.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Postcard(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Postcard postcard : Registries.POSTCARD) {
                TranslationKeyValidator.of(postcard.translationKey);
            }
        }
    }
}
