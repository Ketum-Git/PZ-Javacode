// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public record FluidKey(String id) {
    public static final FluidKey ACID = new FluidKey("Acid");
    public static final FluidKey ALCOHOL = new FluidKey("Alcohol");
    public static final FluidKey ANIMAL_BLOOD = new FluidKey("AnimalBlood");
    public static final FluidKey ANIMAL_GREASE = new FluidKey("AnimalGrease");
    public static final FluidKey ANIMAL_MILK = new FluidKey("AnimalMilk");
    public static final FluidKey BEER = new FluidKey("Beer");
    public static final FluidKey BLEACH = new FluidKey("Bleach");
    public static final FluidKey BLOOD = new FluidKey("Blood");
    public static final FluidKey BRANDY = new FluidKey("Brandy");
    public static final FluidKey CARBONATED_WATER = new FluidKey("CarbonatedWater");
    public static final FluidKey CHAMPAGNE = new FluidKey("Champagne");
    public static final FluidKey CIDER = new FluidKey("Cider");
    public static final FluidKey CLEANING_LIQUID = new FluidKey("CleaningLiquid");
    public static final FluidKey COFFEE = new FluidKey("Coffee");
    public static final FluidKey COFFEE_LIQUEUR = new FluidKey("CoffeeLiqueur");
    public static final FluidKey COLA = new FluidKey("Cola");
    public static final FluidKey COLA_DIET = new FluidKey("ColaDiet");
    public static final FluidKey COLOGNE = new FluidKey("Cologne");
    public static final FluidKey COW_MILK = new FluidKey("CowMilk");
    public static final FluidKey CURACAO = new FluidKey("Curacao");
    public static final FluidKey DYE = new FluidKey("Dye");
    public static final FluidKey GIN = new FluidKey("Gin");
    public static final FluidKey GINGER_ALE = new FluidKey("GingerAle");
    public static final FluidKey GRENADINE = new FluidKey("Grenadine");
    public static final FluidKey HAIR_DYE = new FluidKey("HairDye");
    public static final FluidKey HONEY = new FluidKey("Honey");
    public static final FluidKey JUICE_APPLE = new FluidKey("JuiceApple");
    public static final FluidKey JUICE_CRANBERRY = new FluidKey("JuiceCranberry");
    public static final FluidKey JUICE_FRUITPUNCH = new FluidKey("JuiceFruitpunch");
    public static final FluidKey JUICE_GRAPE = new FluidKey("JuiceGrape");
    public static final FluidKey JUICE_LEMON = new FluidKey("JuiceLemon");
    public static final FluidKey JUICE_ORANGE = new FluidKey("JuiceOrange");
    public static final FluidKey JUICE_TOMATO = new FluidKey("JuiceTomato");
    public static final FluidKey MEAD = new FluidKey("Mead");
    public static final FluidKey MILK_CHOCOLATE = new FluidKey("MilkChocolate");
    public static final FluidKey PERFUME = new FluidKey("Perfume");
    public static final FluidKey PETROL = new FluidKey("Petrol");
    public static final FluidKey POISON_POTENT = new FluidKey("PoisonPotent");
    public static final FluidKey PORT = new FluidKey("Port");
    public static final FluidKey RUM = new FluidKey("Rum");
    public static final FluidKey SCOTCH = new FluidKey("Scotch");
    public static final FluidKey SECRET_FLAVORING = new FluidKey("SecretFlavoring");
    public static final FluidKey SHEEP_MILK = new FluidKey("SheepMilk");
    public static final FluidKey SHERRY = new FluidKey("Sherry");
    public static final FluidKey SIMPLE_SYRUP = new FluidKey("SimpleSyrup");
    public static final FluidKey SODA_BLUEBERRY = new FluidKey("SodaBlueberry");
    public static final FluidKey SODA_BUBBLEGUM = new FluidKey("SodaBubblegum");
    public static final FluidKey SODA_GRAPE = new FluidKey("SodaGrape");
    public static final FluidKey SODA_LIME = new FluidKey("SodaLime");
    public static final FluidKey SODA_PINEAPPLE = new FluidKey("SodaPineapple");
    public static final FluidKey SODA_POP = new FluidKey("SodaPop");
    public static final FluidKey SODA_STREWBERRY = new FluidKey("SodaStrewberry");
    public static final FluidKey SPIFFO_JUICE = new FluidKey("SpiffoJuice");
    public static final FluidKey TAINTED_WATER = new FluidKey("TaintedWater");
    public static final FluidKey TEA = new FluidKey("Tea");
    public static final FluidKey TEQUILA = new FluidKey("Tequila");
    public static final FluidKey VERMOUTH = new FluidKey("Vermouth");
    public static final FluidKey VODKA = new FluidKey("Vodka");
    public static final FluidKey WATER = new FluidKey("Water");
    public static final FluidKey WHISKEY = new FluidKey("Whiskey");
    public static final FluidKey WINE = new FluidKey("Wine");

    @Override
    public String toString() {
        return this.id;
    }
}
