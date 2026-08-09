// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import generation.builders.validation.TranslationKeyValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.json.JSONParserConfiguration;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;

@UsedFromLua
public enum CreditsRole {
    HEAD_RACCOON(CreditsName.SPIFFO),
    FOUNDERS(false, CreditsName.CHRIS_SIMPSON, CreditsName.ANDY_HODGETTS, CreditsName.WILL_PORTER, CreditsName.MARINA_SIU_CHONG),
    ART_DIRECTOR(CreditsName.AYRTON_ORIO),
    TECHNICAL_DIRECTOR(CreditsName.PATRICK_BROTT),
    PRODUCTION_DIRECTOR(CreditsName.MAX_BRODE),
    DESIGN_DIRECTOR(CreditsName.CHRISTIAN_ALLEN),
    MUSIC_ORIGINALLY_COMPOSED_BY(CreditsName.ZACH_BEEVER),
    LEAD_GAMEPLAY_PROGRAMMER(CreditsName.ROMAIN_DRON),
    LEAD_TECHNICAL_PROGRAMMER(CreditsName.ERIK_BROES),
    ENGINEERS(
        CreditsName.CHRIS_SIMPSON,
        CreditsName.TIMOTHY_BAKER,
        CreditsName.ZAC_CONGO,
        CreditsName.THOMAS_GUIMBRETIERE,
        CreditsName.DEREK_BUCHANAN,
        CreditsName.ERIS_CLARK,
        CreditsName.BLAIR_FITZPATRICK,
        CreditsName.RAPHAEL_BARANDAS,
        CreditsName.NORMAN_VEZINA,
        CreditsName.KEES_BEKKEMA
    ),
    ADDITIONAL_CODE(
        CreditsName.STEVE_NORTH,
        CreditsName.FOX_CHAOTICA,
        CreditsName.CONNALL_LINDSAY,
        CreditsName.CHRIS_WOOD,
        CreditsName.STEVE_SHARP,
        CreditsName.NICK_COWEN,
        CreditsName.CASEY_JO_KENNY,
        CreditsName.AITOR_CRUZ,
        CreditsName.JESSE_BURLAND_LOKKO,
        CreditsName.SAM_SPAWTON,
        CreditsName.CASPIAN_PRINCE
    ),
    PRODUCER(CreditsName.YANA_DUNCAN),
    ASSOCIATE_PRODUCER(CreditsName.MAX_NUTTALL),
    ARTISTS(
        CreditsName.MARINA_SIU_CHONG,
        CreditsName.ANDY_HODGETTS,
        CreditsName.ILJA_GAIDENKO,
        CreditsName.DYLAN_MYERS,
        CreditsName.ERIKA_EDGREN,
        CreditsName.NIKOLA_KAPIDZIC,
        CreditsName.MARK_WRIGHT,
        CreditsName.ANDRES_CASTRO,
        CreditsName.FILIBUSTER_RHYMES,
        CreditsName.MARTIN_GREENALL
    ),
    ENVIRONMENTAL_ARTISTS(CreditsName.JAMIE_MAGNUSON, CreditsName.PAUL_RING),
    CONCEPT_ARTIST(CreditsName.AFEKAY),
    GRAPHIC_DESIGNER(CreditsName.STUART_JONES),
    MARKETING_ARTIST(CreditsName.AMY_YOUNG),
    ADDITIONAL_ARTISTS(CreditsName.ADAM_HANUS, CreditsName.DADDYDIRKIEDIRK, CreditsName.UNCONID),
    WRITER(false, CreditsName.WILL_PORTER, CreditsName.PATRICK_BRENNAN),
    RESEARCH_AND_DEVELOPMENT(CreditsName.MATT_RITCHIE, CreditsName.TIMOTHY_BAKER),
    LEAD_QA_TIS(CreditsName.CLIFF_ANDERSON),
    SENIOR_QA(CreditsName.DANIEL_HEELAN),
    QUALITY_ASSURANCE_TESTERS(CreditsName.ELIA_TASSELL, CreditsName.MATTHEW_HARBER, CreditsName.MIKE_BERRY),
    HR(CreditsName.EMMA_SMITH),
    SYSTEM_ADMINISTRATOR(CreditsName.JOHNATHON_TINSLEY),
    JUNIOR_SYSTEM_ADMINISTRATOR(CreditsName.RICHARD_CABAN),
    BACKUP_SYSADMIN(CreditsName.AVI_GREENBURY),
    TECH_SUPPORT(CreditsName.ASH_BLAKE_HOOD),
    LOCALISATION_LEAD(CreditsName.ALEKSANDRA_MIHAILOVA),
    LEAD_COMMUNITY_MANAGER(CreditsName.STEPHEN_REID),
    COMMUNITY_MANAGERS(CreditsName.MARTIN_NEHRDICH, CreditsName.MATTHEW_SULLIVAN),
    COMMUNITY_MODERATORS(CreditsName.ERJAN_READ, CreditsName.MASON_PARADA, CreditsName.JAMES_STRETTON, CreditsName.A_BRICK_FAIRY),
    OPERATIONS_MANAGER(CreditsName.JAY_MILLS),
    FINANCE(CreditsName.CARL_STONE),
    DIRECTOR(CreditsName.MARK_ROWLEY),
    SENIOR_PROGRAMMER(CreditsName.ZAC_CONGO, CreditsName.SCOTT_PURCIVAL),
    RENDERING_PROGRAMMER(CreditsName.NICHOLAS_STEVENS),
    LEAD_PROGRAMMER(CreditsName.CHRISTOPHER_SCHNITZERLING),
    UI_DESIGNER(CreditsName.JONATHAN_BURSAC),
    QUALITY_ASSURANCE(CreditsName.NATALIA_ROWLEY, CreditsName.KYLE_ROWLEY),
    SENIOR_SOFTWARE_DEVELOPER(CreditsName.IURI_IAKOVLEV, CreditsName.ANDREI_TOPILIN),
    MIDDLE_SOFTWARE_DEVELOPER(
        CreditsName.ILIA_TEPLISHCHEV,
        CreditsName.ANDREI_MELNIKOV,
        CreditsName.DANIIL_KLESHCHEVNIKOV,
        CreditsName.ALEXANDER_BLINOV,
        CreditsName.ALEKSEI_KHOKHOLOV
    ),
    LEAD_QA(CreditsName.MIKHAIL_GUDIM),
    QA(CreditsName.BAURZHAN_SITKALIEV, CreditsName.ARTEM_BORDYUGOV),
    LEAD_SOUND_DESIGNER_ARRIVAL(CreditsName.MATTEO_LUPIERI),
    TECHNICAL_SOUND_DESIGNER_ARRIVAL(CreditsName.YANNICK_WINTER, CreditsName.MICHAEL_HARTUNG),
    SOUND_SUPERVISOR(CreditsName.BYRON_BULLOCK),
    SOUND_DESIGNER(CreditsName.THOMAS_DEYN, CreditsName.LORENZO_PIANI, CreditsName.INNOKENTIY_SEDOV, CreditsName.DAVID_PHILIPP),
    ASSOCIATE_SOUND_DESIGNER(CreditsName.KASIA_DZIEKAN, CreditsName.JACK_BELL),
    INTERACTIVE_MUSIC_COMPOSER(CreditsName.ARMIN_HAAS, CreditsName.MATTHIAS_WOLF),
    SENIOR_PRODUCER(CreditsName.JEMMA_RILEY_TOLCH),
    HEAD_PRODUCER(CreditsName.FLAVIA_TACONI),
    GENERAL_ARCADE(CreditsName.GENNADII_POTAPOV, CreditsName.SERGEI_SHUBIN),
    SPECIAL_INFECTED(
        CreditsName.BENJAMIN_SCHIEDER,
        CreditsName.ALEXA_ALTEZ,
        CreditsName.ALICE_BONNEAU,
        CreditsName.AMBIGUOUSAMPHIBIAN,
        CreditsName.ANTTI_RIIKONEN,
        CreditsName.AWPENHEIMER,
        CreditsName.BEAR_GRANANDER,
        CreditsName.BENJAMIN_JAMES,
        CreditsName.BILL_REDPATH,
        CreditsName.CHARLIE_SLOAN,
        CreditsName.CHRISTIAN_JORGE,
        CreditsName.CHRISTOPHER_GREEN,
        CreditsName.CLEMENT_GIRAUDEAU,
        CreditsName.CROMULENT_ARCHER,
        CreditsName.DANIEL_SOBHI,
        CreditsName.DITOSEADIO,
        CreditsName.DMITRY_BOROVSKY,
        CreditsName.DR_COX1911,
        CreditsName.DRAKE_BARRON,
        CreditsName.EDWARD_SMALLWOOD,
        CreditsName.FRANCOIS_DESFORGES,
        CreditsName.HENRY_PATTERSON,
        CreditsName.HOODIE_GUY,
        CreditsName.JACOB_HALEY,
        CreditsName.JONES_GAMBARINE,
        CreditsName.KEVIN_KING,
        CreditsName.KIM_METZGER,
        CreditsName.KRZYSZTOF_SAGOLA,
        CreditsName.LEO_DIMILO,
        CreditsName.LEO_IVANOV,
        CreditsName.LEONKIEL,
        CreditsName.LOUIS_KEEP,
        CreditsName.MARC_TOOKEY,
        CreditsName.MARCO_PURSCHLER,
        CreditsName.MARCUS_GONCALVES,
        CreditsName.MARCUS_HILL,
        CreditsName.MARK_SANDERS,
        CreditsName.MARK_EXE,
        CreditsName.MIDDLEAGEDBOB,
        CreditsName.MR_BROLLOW,
        CreditsName.MR_L_TARTAN,
        CreditsName.MISTER_LAMPREY,
        CreditsName.PAUL_MCALOON,
        CreditsName.PAUL_WRIGHT,
        CreditsName.PETER_LOVERSO,
        CreditsName.PUPPERS,
        CreditsName.RASMUS_NIELSEN,
        CreditsName.REKKIE,
        CreditsName.ROBOMAT,
        CreditsName.ROYALEWITHCHEESE,
        CreditsName.RUAL_STORGE,
        CreditsName.SAVELIY_ZELOVSKIY,
        CreditsName.STEVE_YOUNG,
        CreditsName.THE_COMMANDER,
        CreditsName.TITOPEI,
        CreditsName.ANONYMOUS
    ),
    SPECIAL_THANKS(
        CreditsName.THE_COMMUNITY,
        CreditsName.PETER_LEWIN,
        CreditsName.ALAN_CARTER,
        CreditsName.ANGELA_BATEMAN,
        CreditsName.BOBHECKLING,
        CreditsName.BOXER,
        CreditsName.BRIAN_HICKS,
        CreditsName.CALLY,
        CreditsName.CHET_FALISZEK,
        CreditsName.CHIEF,
        CreditsName.DEAN_HALL,
        CreditsName.DEAN_TROTMAN,
        CreditsName.DUFFY_DAVIS,
        CreditsName.ECKYMAN,
        CreditsName.ELSPETH_EDMONDS,
        CreditsName.EMILY_RICHARDSON,
        CreditsName.FRANK_JORDAN,
        CreditsName.GRAEME_STRUTHERS,
        CreditsName.JAIME_BYRNE,
        CreditsName.JAS_PUREWAL,
        CreditsName.JOSH_HENNING,
        CreditsName.KIWO,
        CreditsName.KLEAN,
        CreditsName.MATHAS,
        CreditsName.MATT_SCHILLACI,
        CreditsName.MAYATUTORS,
        CreditsName.MRATOMICDUCK,
        CreditsName.NJ_APOSTOL,
        CreditsName.NOTCH,
        CreditsName.OLIVIA_WHITE,
        CreditsName.PAUL_JR,
        CreditsName.PHILL_CAMERON,
        CreditsName.PR1VATELIME,
        CreditsName.REDWIRE,
        CreditsName.ROBBAZ,
        CreditsName.SACRIEL,
        CreditsName.SARAH_NEWELL,
        CreditsName.SAUDIGAMER,
        CreditsName.SCOTT_REISMANIS,
        CreditsName.SHANNON_KILLER,
        CreditsName.THE_PUB,
        CreditsName.TIM_BUCKLEY,
        CreditsName.TOM_PORTER,
        CreditsName.TRAILER_FARM,
        CreditsName.TWIGGY
    ),
    CONTRIBUTORS(CreditsName.ADRIC_HARTIN, CreditsName.CHRISTIAN_WALBER, CreditsName.DRAKE_BARRON, CreditsName.KIERAN_RAFFERTY),
    WIKI_ADMIN(CreditsName.MICHAEL_HAZELWOOD, CreditsName.MICHAL_CHECINSKI, CreditsName.KEVIN_BANKS, CreditsName.ISAAC_SALES),
    LOCALIZATION(),
    TMG_UA(CreditsName.ANNA_SHEVCHENKO, CreditsName.YULIIA_TATSENKO, CreditsName.KONSTANTIN_KOPIN),
    TMG_PT(CreditsName.JORGE_GUEDES, CreditsName.DANIEL_MARQUES),
    TMG_CH(CreditsName.HENRY_SHU, CreditsName.MILLER_MEI, CreditsName.LIWEN_ZHANG),
    TMG_CN(CreditsName.YICHING_CHOU, CreditsName.TZU_SHENG_HSIAO),
    TMG_DA(CreditsName.THOR_PENTHIN_GRUMLØSE),
    TMG_DE(CreditsName.JAKOB_KANDEL),
    TMG_FI(CreditsName.SAMI_OLAVI, CreditsName.TANJA_SUITIALA),
    TMG_HU(CreditsName.ANDRÁS_ÁCS, CreditsName.ORSOLYA_LIPTAY),
    TMG_IT(CreditsName.ALAIN_DELLEPIANE, CreditsName.MATTEO_SCARABELLI, CreditsName.BARBARA_MONTI, CreditsName.LORENZO_BERTOLUCCI),
    TMG_KO(CreditsName.JUNWON_KONG, CreditsName.CILLIAN_JUNG, CreditsName.HYESU_CHOE, CreditsName.PETER_LEE),
    TMG_NL(CreditsName.DEMI_BARTELS, CreditsName.HELEEN_WIEMANS, CreditsName.HESTER_LEISTRA, CreditsName.IRIS_KUPPEN),
    TMG_NO(CreditsName.LINDA_MYRWOLD, CreditsName.KRISTINE_MYRENE),
    TMG_PL(
        CreditsName.KAROLINA_BARSZCZ,
        CreditsName.WOJCIECH_BRUDZIŃSKI,
        CreditsName.TOMEK_KALISZEWSKI,
        CreditsName.PIOTR_HELAK,
        CreditsName.ASIA_MLECZAK,
        CreditsName.NATALIA_PATEREK
    ),
    WIKI_EDITORS(
        CreditsName.EDWIN_TRAN,
        CreditsName.SHARPGOLDEN,
        CreditsName.SIMON_CADET,
        CreditsName.MACA,
        CreditsName.VIBRANCE,
        CreditsName.MATHILDACHAN,
        CreditsName.ALBION
    ),
    IN_LOVING_MEMORY_OF(CreditsName.ROMUALD_DRON, CreditsName.VICKI_WOODS);

    public static final String TRANSLATOR = "Translator";
    public static final Path TRANSLATION_FOLDER = Path.of("media/lua/shared/Translate");
    public static final Path CREDITS_TRANSLATOR_JSON = Path.of("Credits_Translator.json");
    private final String title = "credits_role.%s".formatted(this.name().toLowerCase());
    private final List<CreditsName> names;

    private CreditsRole(final boolean sort, final CreditsName... names) {
        if (sort) {
            this.names = Arrays.stream(names).sorted(Comparator.comparing(CreditsName::getSort)).toList();
        } else {
            this.names = List.of(names);
        }
    }

    private CreditsRole(final CreditsName... names) {
        this(true, names);
    }

    public String getTitle() {
        return this.title;
    }

    public List<CreditsName> getNames() {
        return this.names;
    }

    public static KahluaTable getTranslatorCredits() {
        KahluaTable map = LuaManager.platform.newTable();

        try {
            if (!Files.exists(TRANSLATION_FOLDER)) {
                DebugLog.log(DebugType.Translation, "Translations folder not found at " + TRANSLATION_FOLDER);
                return map;
            }

            try (Stream<Path> stream = Files.walk(TRANSLATION_FOLDER, 1)) {
                stream.filter(x$0 -> Files.isDirectory(x$0)).filter(path -> !path.equals(TRANSLATION_FOLDER)).forEach(file -> {
                    Language fileLanguage = Languages.instance.getByName(file.getFileName().toString());
                    if (fileLanguage != null && fileLanguage != Languages.instance.getDefaultLanguage()) {
                        List<String> translatorCredits = getTranslatorCreditsList(fileLanguage);
                        if (!translatorCredits.isEmpty()) {
                            map.rawset(fileLanguage.text(), translatorCredits);
                        }
                    }
                });
            }
        } catch (Exception var6) {
            DebugLog.log(DebugType.Translation, "Failed to load translator credits");
        }

        return map;
    }

    public static List<String> getTranslatorCreditsList(Language language) {
        List<String> translatorsList = new ArrayList<>();

        try {
            Path path = TRANSLATION_FOLDER.resolve(language.name()).resolve(CREDITS_TRANSLATOR_JSON);
            if (!Files.exists(path)) {
                return translatorsList;
            } else {
                JSONObject jsonObject = new JSONObject(Files.readString(path), new JSONParserConfiguration().withStrictMode(Core.IS_DEV));
                String translator = jsonObject.getString("Translator");
                if (!translator.isEmpty()) {
                    String[] split = translator.split("\n");
                    translatorsList.addAll(Arrays.asList(split));
                }

                return translatorsList;
            }
        } catch (IOException var6) {
            throw new RuntimeException(var6);
        }
    }

    static {
        if (Core.IS_DEV) {
            for (CreditsRole entry : values()) {
                TranslationKeyValidator.of(entry.title);
            }
        }
    }
}
