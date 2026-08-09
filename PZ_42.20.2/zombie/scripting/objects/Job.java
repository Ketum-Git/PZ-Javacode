// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Job {
    public static final Job ACCOUNTANT = registerBase("Accountant");
    public static final Job ACTOR = registerBase("Actor");
    public static final Job ALARM_INSTALLER = registerBase("AlarmInstaller");
    public static final Job ANIMAL_EXPERT = registerBase("AnimalExpert");
    public static final Job ARCHITECT = registerBase("Architect");
    public static final Job ARTIST = registerBase("Artist");
    public static final Job BABYSITTER = registerBase("Babysitter");
    public static final Job BARBER = registerBase("Barber");
    public static final Job BODYGUARD = registerBase("Bodyguard");
    public static final Job BUILDER = registerBase("Builder");
    public static final Job BUSINESS_CARD_MAKER = registerBase("BusinessCardMaker");
    public static final Job BUSINESS_CONSULTANT = registerBase("BusinessConsultant");
    public static final Job BUSINESS_OWNER = registerBase("BusinessOwner");
    public static final Job BUTCHER = registerBase("Butcher");
    public static final Job CAR_SALESPERSON = registerBase("CarSalesperson");
    public static final Job CARPENTER = registerBase("Carpenter");
    public static final Job CLEANER = registerBase("Cleaner");
    public static final Job CLOTHING_DESIGNER = registerBase("ClothingDesigner");
    public static final Job CLOWN = registerBase("Clown");
    public static final Job CODER = registerBase("Coder");
    public static final Job COOK = registerBase("Cook");
    public static final Job CULT_DEPROGRAMMER = registerBase("CultDeprogrammer");
    public static final Job DANCER = registerBase("Dancer");
    public static final Job DENTIST = registerBase("Dentist");
    public static final Job DERMATOLOGIST = registerBase("Dermatologist");
    public static final Job DIETICIAN = registerBase("Dietician");
    public static final Job DIY = registerBase("DIY");
    public static final Job DOCTOR = registerBase("Doctor");
    public static final Job DRAFTER = registerBase("Drafter");
    public static final Job DRIVER = registerBase("Driver");
    public static final Job DRY_CLEANER = registerBase("DryCleaner");
    public static final Job EFFICIENCY_EXPERT = registerBase("EfficiencyExpert");
    public static final Job ELECTRICIAN = registerBase("Electrician");
    public static final Job ENGINEER = registerBase("Engineer");
    public static final Job ESCORT = registerBase("Escort");
    public static final Job EXORCIST = registerBase("Exorcist");
    public static final Job EXOTIC_DANCER = registerBase("ExoticDancer");
    public static final Job EXTERMINATOR = registerBase("Exterminator");
    public static final Job FACTORY_MANAGER = registerBase("FactoryManager");
    public static final Job FENCER = registerBase("Fencer");
    public static final Job FILM_TV_CREW = registerBase("Film/TVCrew");
    public static final Job FINANCIAL_ADVISOR = registerBase("FinancialAdvisor");
    public static final Job FITNESS_INSTRUCTOR = registerBase("FitnessInstructor");
    public static final Job FLOORER = registerBase("Floorer");
    public static final Job FORTUNE_TELLER = registerBase("FortuneTeller");
    public static final Job FRAMER = registerBase("Framer");
    public static final Job GARDENER = registerBase("Gardener");
    public static final Job GENERAL_MANAGER = registerBase("GeneralManager");
    public static final Job GRAPHIC_DESIGNER = registerBase("GraphicDesigner");
    public static final Job HAIRDRESSER = registerBase("Hairdresser");
    public static final Job HEAD_CHEF = registerBase("HeadChef");
    public static final Job HISTORIAN = registerBase("Historian");
    public static final Job HUMOROUS_FAKE_OCCUPATION_NAME = registerBase("HumorousFakeOccupationName");
    public static final Job HUNTER = registerBase("Hunter");
    public static final Job INSURANCE_AGENT = registerBase("InsuranceAgent");
    public static final Job INTIMATE_DISEASE_SPECIALIST = registerBase("IntimateDiseaseSpecialist");
    public static final Job IT_TECHNICIAN = registerBase("ITTechnician");
    public static final Job JACK_OF_ALL_TRADES = registerBase("JackofallTrades");
    public static final Job JOURNALIST = registerBase("Journalist");
    public static final Job LABORER = registerBase("Laborer");
    public static final Job LAWYER = registerBase("Lawyer");
    public static final Job LECTURER = registerBase("Lecturer");
    public static final Job LOCAL_HISTORY_EXPERT = registerBase("LocalHistoryExpert");
    public static final Job LOCAL_POLITICIAN = registerBase("LocalPolitician");
    public static final Job LOCKSMITH = registerBase("Locksmith");
    public static final Job LOGGER = registerBase("Logger");
    public static final Job LOGISTICS_EXPERT = registerBase("LogisticsExpert");
    public static final Job MACHINE_OPERATOR = registerBase("MachineOperator");
    public static final Job MAKEUP_ARTIST = registerBase("MakeupArtist");
    public static final Job MASSEUSE = registerBase("Masseuse");
    public static final Job MECHANIC = registerBase("Mechanic");
    public static final Job METALWORKER = registerBase("Metalworker");
    public static final Job MIDWIFE = registerBase("Midwife");
    public static final Job NANNY = registerBase("Nanny");
    public static final Job NURSE = registerBase("Nurse");
    public static final Job OPTICIAN = registerBase("Optician");
    public static final Job ORTHODONTIST = registerBase("Orthodontist");
    public static final Job PAINTER = registerBase("Painter");
    public static final Job PEDIATRICIAN = registerBase("Pediatrician");
    public static final Job PERSONAL_TRAINER = registerBase("PersonalTrainer");
    public static final Job PHARMACIST = registerBase("Pharmacist");
    public static final Job PHOTOGRAPHER = registerBase("Photographer");
    public static final Job PHYSIOTHERAPIST = registerBase("Physiotherapist");
    public static final Job PILOT = registerBase("Pilot");
    public static final Job PLUMBER = registerBase("Plumber");
    public static final Job PRIVATE_INVESTIGATOR = registerBase("PrivateInvestigator");
    public static final Job PRODUCER = registerBase("Producer");
    public static final Job PSYCHIATRIST = registerBase("Psychiatrist");
    public static final Job PSYCHIC = registerBase("Psychic");
    public static final Job PUBLISHER = registerBase("Publisher");
    public static final Job REAL_ESTATE_AGENT = registerBase("RealEstateAgent");
    public static final Job REHAB = registerBase("Rehab");
    public static final Job REPAIRMAN = registerBase("Repairman");
    public static final Job SAILOR = registerBase("Sailor");
    public static final Job SALESPERSON = registerBase("Salesperson");
    public static final Job SCIENTIST = registerBase("Scientist");
    public static final Job SCRAPYARD_WORKER = registerBase("ScrapyardWorker");
    public static final Job SECRETARY = registerBase("Secretary");
    public static final Job SECURITY_GUARD = registerBase("SecurityGuard");
    public static final Job SINGER = registerBase("Singer");
    public static final Job STOCK_MARKET_EXPERT = registerBase("StockMarketExpert");
    public static final Job STONEMASON = registerBase("Stonemason");
    public static final Job TAILOR = registerBase("Tailor");
    public static final Job TAX_EXPERT = registerBase("TaxExpert");
    public static final Job TAXI_DRIVER = registerBase("TaxiDriver");
    public static final Job TEACHER = registerBase("Teacher");
    public static final Job TECHNICIAN = registerBase("Technician");
    public static final Job TOUR_GUIDE = registerBase("TourGuide");
    public static final Job TRAVEL_AGENT = registerBase("TravelAgent");
    public static final Job TUTOR = registerBase("Tutor");
    public static final Job UNDERTAKER = registerBase("Undertaker");
    public static final Job VETERINARIAN = registerBase("Veterinarian");
    public static final Job WELDER = registerBase("Welder");
    public static final Job WINDOW_FITTER = registerBase("WindowFitter");
    public static final Job WRITER = registerBase("Writer");
    private final String translationKey;

    private Job(String translationKey) {
        this.translationKey = "IGUI_" + translationKey;
    }

    public static Job get(ResourceLocation id) {
        return Registries.JOB.get(id);
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static Job register(String id) {
        return register(false, id);
    }

    private static Job registerBase(String id) {
        return register(true, id);
    }

    private static Job register(boolean allowDefaultNamespace, String id) {
        return Registries.JOB.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Job(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Job job : Registries.JOB) {
                TranslationKeyValidator.of(job.translationKey());
            }
        }
    }
}
