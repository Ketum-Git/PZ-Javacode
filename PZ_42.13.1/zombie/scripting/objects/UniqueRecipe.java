// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.scripting.ScriptType;

@UsedFromLua
public final class UniqueRecipe extends BaseScriptObject {
    private String name;
    private String baseRecipe;
    private final ArrayList<String> items = new ArrayList<>();
    private int hungerBonus;
    private int hapinessBonus;
    private int boredomBonus;

    public UniqueRecipe(String name) {
        super(ScriptType.UniqueRecipe);
        this.setName(name);
    }

    @Override
    public void Load(String name, String token) throws Exception {
        String[] waypoint = token.split("[{}]");
        String[] coords = waypoint[1].split(",");
        this.LoadCommonBlock(token);
        this.Load(name, coords);
    }

    public void Load(String name, String[] strArray) {
        for (int i = 0; i < strArray.length; i++) {
            if (!strArray[i].trim().isEmpty() && strArray[i].contains(":")) {
                String[] split = strArray[i].split(":");
                String key = split[0].trim();
                String value = split[1].trim();
                if (key.equals("BaseRecipeItem")) {
                    this.setBaseRecipe(value);
                } else if (key.equals("Item")) {
                    this.items.add(value);
                } else if (key.equals("Hunger")) {
                    this.setHungerBonus(Integer.parseInt(value));
                } else if (key.equals("Hapiness")) {
                    this.setHapinessBonus(Integer.parseInt(value));
                } else if (key.equals("Boredom")) {
                    this.setBoredomBonus(Integer.parseInt(value));
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseRecipe() {
        return this.baseRecipe;
    }

    public void setBaseRecipe(String baseRecipe) {
        this.baseRecipe = baseRecipe;
    }

    public int getHungerBonus() {
        return this.hungerBonus;
    }

    public void setHungerBonus(int hungerBonus) {
        this.hungerBonus = hungerBonus;
    }

    public int getHapinessBonus() {
        return this.hapinessBonus;
    }

    public void setHapinessBonus(int hapinessBonus) {
        this.hapinessBonus = hapinessBonus;
    }

    public ArrayList<String> getItems() {
        return this.items;
    }

    public int getBoredomBonus() {
        return this.boredomBonus;
    }

    public void setBoredomBonus(int boredomBonus) {
        this.boredomBonus = boredomBonus;
    }
}
