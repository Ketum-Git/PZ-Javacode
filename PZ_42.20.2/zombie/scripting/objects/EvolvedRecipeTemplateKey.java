// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public record EvolvedRecipeTemplateKey(String id) {
    public static final EvolvedRecipeTemplateKey ADD_BAIT_TO_CHUM = new EvolvedRecipeTemplateKey("AddBaitToChum");
    public static final EvolvedRecipeTemplateKey BREAD = new EvolvedRecipeTemplateKey("Bread");
    public static final EvolvedRecipeTemplateKey BURGER = new EvolvedRecipeTemplateKey("Burger");
    public static final EvolvedRecipeTemplateKey BURRITO = new EvolvedRecipeTemplateKey("Burrito");
    public static final EvolvedRecipeTemplateKey CAKE = new EvolvedRecipeTemplateKey("Cake");
    public static final EvolvedRecipeTemplateKey CONE_ICECREAM = new EvolvedRecipeTemplateKey("ConeIcecream");
    public static final EvolvedRecipeTemplateKey CONE_ICE_CREAM = new EvolvedRecipeTemplateKey("ConeIceCream");
    public static final EvolvedRecipeTemplateKey FRUIT_SALAD = new EvolvedRecipeTemplateKey("FruitSalad");
    public static final EvolvedRecipeTemplateKey HOTDOG = new EvolvedRecipeTemplateKey("Hotdog");
    public static final EvolvedRecipeTemplateKey HOT_DRINK = new EvolvedRecipeTemplateKey("HotDrink");
    public static final EvolvedRecipeTemplateKey MUFFIN = new EvolvedRecipeTemplateKey("Muffin");
    public static final EvolvedRecipeTemplateKey OATMEAL = new EvolvedRecipeTemplateKey("Oatmeal");
    public static final EvolvedRecipeTemplateKey OMELETTE = new EvolvedRecipeTemplateKey("Omelette");
    public static final EvolvedRecipeTemplateKey PANCAKES = new EvolvedRecipeTemplateKey("Pancakes");
    public static final EvolvedRecipeTemplateKey PASTA = new EvolvedRecipeTemplateKey("Pasta");
    public static final EvolvedRecipeTemplateKey PIE = new EvolvedRecipeTemplateKey("Pie");
    public static final EvolvedRecipeTemplateKey PIE_SWEET = new EvolvedRecipeTemplateKey("PieSweet");
    public static final EvolvedRecipeTemplateKey PIZZA = new EvolvedRecipeTemplateKey("Pizza");
    public static final EvolvedRecipeTemplateKey RICE = new EvolvedRecipeTemplateKey("Rice");
    public static final EvolvedRecipeTemplateKey RICE_PAN = new EvolvedRecipeTemplateKey("RicePan");
    public static final EvolvedRecipeTemplateKey RICE_POT = new EvolvedRecipeTemplateKey("RicePot");
    public static final EvolvedRecipeTemplateKey ROASTED_VEGETABLES = new EvolvedRecipeTemplateKey("Roasted Vegetables");
    public static final EvolvedRecipeTemplateKey SALAD = new EvolvedRecipeTemplateKey("Salad");
    public static final EvolvedRecipeTemplateKey SANDWICH = new EvolvedRecipeTemplateKey("Sandwich");
    public static final EvolvedRecipeTemplateKey SOUP = new EvolvedRecipeTemplateKey("Soup");
    public static final EvolvedRecipeTemplateKey STEW = new EvolvedRecipeTemplateKey("Stew");
    public static final EvolvedRecipeTemplateKey STIR_FRY = new EvolvedRecipeTemplateKey("Stir fry");
    public static final EvolvedRecipeTemplateKey STIR_FRY_GRIDDLE_PAN = new EvolvedRecipeTemplateKey("Stir fry Griddle Pan");
    public static final EvolvedRecipeTemplateKey TACO = new EvolvedRecipeTemplateKey("Taco");
    public static final EvolvedRecipeTemplateKey TOAST = new EvolvedRecipeTemplateKey("Toast");
    public static final EvolvedRecipeTemplateKey WAFFLES = new EvolvedRecipeTemplateKey("Waffles");

    @Override
    public String toString() {
        return this.id;
    }
}
