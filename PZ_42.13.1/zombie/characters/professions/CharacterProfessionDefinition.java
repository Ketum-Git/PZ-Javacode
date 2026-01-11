// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.professions;

import generation.builders.CharacterProfessionDefinitionBuilder;
import generation.builders.CharacterProfessionDefinitionBuilder.XPBoost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.interfaces.IListBoxItem;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public class CharacterProfessionDefinition implements IListBoxItem {
    public static Map<CharacterProfession, CharacterProfessionDefinition> characterProfessionDefinitions = new LinkedHashMap<>();
    private final CharacterProfession characterProfessionType;
    private final String displayName;
    private final int cost;
    private String description = "";
    private String iconPathName;
    private Texture texture;
    private final ArrayList<CharacterTrait> grantedTraits = new ArrayList<>();
    private final ArrayList<String> grantedRecipes = new ArrayList<>();
    private final HashMap<PerkFactory.Perk, Integer> xpBoosts = new HashMap<>();

    public CharacterProfessionDefinition(CharacterProfession characterProfessionType, String name, int cost, String description, String iconPathName) {
        this.characterProfessionType = characterProfessionType;
        this.displayName = Translator.getText(name);
        this.cost = cost;
        if (description != null && !description.isEmpty()) {
            this.description = Translator.getText(description);
        }

        this.iconPathName = iconPathName;
        if (iconPathName != null && !iconPathName.isEmpty()) {
            this.texture = Texture.trygetTexture(iconPathName);
        }
    }

    public static CharacterProfessionDefinition addCharacterProfessionDefinition(
        CharacterProfession characterProfessionType, String name, int cost, String description, String iconPathName
    ) {
        CharacterProfessionDefinition t = new CharacterProfessionDefinition(characterProfessionType, name, cost, description, iconPathName);
        characterProfessionDefinitions.put(characterProfessionType, t);
        return t;
    }

    public static void addCharacterProfessionDefinition(CharacterProfessionDefinitionBuilder characterProfessionDefinitionBuilder) {
        CharacterProfessionDefinition characterProfessionDefinition = addCharacterProfessionDefinition(
            characterProfessionDefinitionBuilder.getCharacterProfession(),
            characterProfessionDefinitionBuilder.getUIName(),
            characterProfessionDefinitionBuilder.getCost(),
            characterProfessionDefinitionBuilder.getUIDescription(),
            characterProfessionDefinitionBuilder.getIconPathNam()
        );

        for (XPBoost xpBoost : characterProfessionDefinitionBuilder.getXpBoosts()) {
            characterProfessionDefinition.addXPBoost(xpBoost.perk(), xpBoost.boost());
        }

        for (String grantedRecipe : characterProfessionDefinitionBuilder.getGrantedRecipes()) {
            characterProfessionDefinition.addGrantedRecipe(grantedRecipe);
        }

        for (CharacterTrait freeTrait : characterProfessionDefinitionBuilder.getGrantedTraits()) {
            characterProfessionDefinition.addGrantedTrait(freeTrait);
        }
    }

    public static ArrayList<CharacterProfessionDefinition> getProfessions() {
        return new ArrayList<>(characterProfessionDefinitions.values());
    }

    public static CharacterProfessionDefinition getCharacterProfessionDefinition(CharacterProfession characterProfession) {
        return characterProfessionDefinitions.get(characterProfession);
    }

    public CharacterProfession getType() {
        return this.characterProfessionType;
    }

    public String getDescription() {
        return this.description;
    }

    public int getCost() {
        return this.cost;
    }

    public Texture getTexture() {
        return this.texture;
    }

    public ArrayList<CharacterTrait> getGrantedTraits() {
        return new ArrayList<>(this.grantedTraits);
    }

    public ArrayList<String> getGrantedRecipes() {
        return this.grantedRecipes;
    }

    public boolean isGrantedRecipe(String recipe) {
        return this.grantedRecipes.contains(recipe);
    }

    public boolean hasGrantedRecipes() {
        return !this.grantedRecipes.isEmpty();
    }

    @Override
    public String getLabel() {
        return this.displayName;
    }

    @Override
    public String getLeftLabel() {
        return this.displayName;
    }

    @Override
    public String getRightLabel() {
        int cost = this.cost;
        if (cost == 0) {
            return "";
        } else {
            String before = cost > 0 ? "-" : "+";
            if (cost < 0) {
                cost = -cost;
            }

            return before + cost;
        }
    }

    public String getUIName() {
        return this.displayName;
    }

    public HashMap<PerkFactory.Perk, Integer> getXpBoosts() {
        return this.xpBoosts;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addGrantedTrait(CharacterTrait characterTrait) {
        this.grantedTraits.add(characterTrait);
    }

    private void addGrantedRecipe(String recipe) {
        this.grantedRecipes.add(recipe);
    }

    public void addXPBoost(PerkFactory.Perk perk, int level) {
        this.xpBoosts.put(perk, level);
    }

    public static void reset() {
        characterProfessionDefinitions.clear();
    }
}
