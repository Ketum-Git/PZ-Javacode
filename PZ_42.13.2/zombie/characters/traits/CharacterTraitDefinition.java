// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.traits;

import generation.builders.CharacterTraitDefinitionBuilder;
import generation.builders.CharacterTraitDefinitionBuilder.XPBoost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.interfaces.IListBoxItem;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public class CharacterTraitDefinition implements IListBoxItem {
    public static Map<CharacterTrait, CharacterTraitDefinition> characterTraitDefinitions = new LinkedHashMap<>();
    private final CharacterTrait characterTraitType;
    private final String displayName;
    private final int cost;
    private String description;
    private final boolean isProfessionTrait;
    private Texture texture;
    private boolean disabledInMultiplayer;
    private final ArrayList<CharacterTrait> grantedTraits = new ArrayList<>();
    private final ArrayList<String> grantedRecipes = new ArrayList<>();
    private final ArrayList<CharacterTrait> mutuallyExclusiveTraits = new ArrayList<>();
    private final HashMap<PerkFactory.Perk, Integer> xpBoosts = new HashMap<>();

    public CharacterTraitDefinition(
        CharacterTrait characterTraitType, String name, int cost, String description, boolean isProfessionTrait, boolean disabledInMultiplayer
    ) {
        this.characterTraitType = characterTraitType;
        this.displayName = Translator.getText(name);
        this.cost = cost;
        this.description = Translator.getText(description);
        this.isProfessionTrait = isProfessionTrait;
        this.texture = Texture.getSharedTexture("media/ui/Traits/trait_" + this.characterTraitType.getName().toLowerCase(Locale.ENGLISH) + ".png");
        if (this.texture == null) {
            this.texture = Texture.getSharedTexture("media/ui/Traits/trait_generic.png");
        }

        this.disabledInMultiplayer = disabledInMultiplayer;
    }

    public static CharacterTraitDefinition addCharacterTraitDefinition(
        CharacterTrait characterTraitType, String name, int cost, String description, boolean profession
    ) {
        return addCharacterTraitDefinition(characterTraitType, name, cost, description, profession, false);
    }

    public static CharacterTraitDefinition addCharacterTraitDefinition(
        CharacterTrait characterTraitType, String name, int cost, String description, boolean profession, boolean disabledInMultiplayer
    ) {
        CharacterTraitDefinition t = new CharacterTraitDefinition(characterTraitType, name, cost, description, profession, disabledInMultiplayer);
        characterTraitDefinitions.put(characterTraitType, t);
        return t;
    }

    public static void addCharacterTraitDefinition(CharacterTraitDefinitionBuilder characterTraitDefinitionBuilder) {
        CharacterTraitDefinition characterTraitDefinition = addCharacterTraitDefinition(
            characterTraitDefinitionBuilder.getCharacterTrait(),
            characterTraitDefinitionBuilder.getUIName(),
            characterTraitDefinitionBuilder.getCost(),
            characterTraitDefinitionBuilder.getUIDescription(),
            characterTraitDefinitionBuilder.getProfession(),
            characterTraitDefinitionBuilder.getDisabledInMultiplayer()
        );
        List<CharacterTrait> mutuallyExclusive = characterTraitDefinitionBuilder.getMutuallyExclusiveTraits();

        for (XPBoost xpBoost : characterTraitDefinitionBuilder.getXpBoosts()) {
            characterTraitDefinition.addXPBoost(xpBoost.perk(), xpBoost.boost());
        }

        for (CharacterTrait mutuallyExclusiveCharacterTrait : mutuallyExclusive) {
            characterTraitDefinition.addMutuallyExclusive(mutuallyExclusiveCharacterTrait);
        }

        for (String freeRecipe : characterTraitDefinitionBuilder.getGrantedRecipes()) {
            characterTraitDefinition.addGrantedRecipe(freeRecipe);
        }

        for (CharacterTrait freeTrait : characterTraitDefinitionBuilder.getGrantedTraits()) {
            characterTraitDefinition.addGrantedTrait(freeTrait);
        }
    }

    public static void reset() {
        characterTraitDefinitions.clear();
    }

    public static ArrayList<CharacterTraitDefinition> getTraits() {
        return new ArrayList<>(characterTraitDefinitions.values());
    }

    public static CharacterTraitDefinition getCharacterTraitDefinition(CharacterTrait characterTrait) {
        return characterTraitDefinitions.get(characterTrait);
    }

    public static void setMutualExclusive(CharacterTrait a, CharacterTrait b) {
        if (!characterTraitDefinitions.get(a).mutuallyExclusiveTraits.contains(b)) {
            characterTraitDefinitions.get(a).mutuallyExclusiveTraits.add(b);
        }

        if (!characterTraitDefinitions.get(b).mutuallyExclusiveTraits.contains(a)) {
            characterTraitDefinitions.get(b).mutuallyExclusiveTraits.add(a);
        }

        characterTraitDefinitions.get(a).updateMutualExclusiveTraits();
        characterTraitDefinitions.get(b).updateMutualExclusiveTraits();
    }

    public CharacterTrait getType() {
        return this.characterTraitType;
    }

    public String getUIName() {
        return this.displayName;
    }

    public Texture getTexture() {
        return this.texture;
    }

    public int getCost() {
        return this.cost;
    }

    public boolean isFree() {
        return this.isProfessionTrait;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isDisabledInMultiplayer() {
        return this.disabledInMultiplayer;
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

    public ArrayList<CharacterTrait> getMutuallyExclusiveTraits() {
        return this.mutuallyExclusiveTraits;
    }

    public HashMap<PerkFactory.Perk, Integer> getXpBoosts() {
        return this.xpBoosts;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDisabledInMultiplayer(boolean disabledInMultiplayer) {
        this.disabledInMultiplayer = disabledInMultiplayer;
    }

    public void addGrantedTrait(CharacterTrait characterTrait) {
        this.grantedTraits.add(characterTrait);
        this.updateMutualExclusiveTraits();
    }

    private void addGrantedRecipe(String recipe) {
        this.grantedRecipes.add(recipe);
    }

    public void addXPBoost(PerkFactory.Perk perk, int level) {
        this.xpBoosts.put(perk, level);
    }

    public void addMutuallyExclusive(CharacterTrait characterTrait) {
        this.mutuallyExclusiveTraits.add(characterTrait);
    }

    public boolean hasMutuallyExclusiveTraits() {
        return !this.mutuallyExclusiveTraits.isEmpty();
    }

    public boolean isMutuallyExclusive(CharacterTraitDefinition characterTraitDefinition) {
        if (this.getMutuallyExclusiveTraits().contains(characterTraitDefinition.getType())) {
            return true;
        } else {
            for (int i = 0; i < this.getGrantedTraits().size(); i++) {
                CharacterTraitDefinition grantedCharacterTrait = characterTraitDefinitions.get(this.getGrantedTraits().get(i));
                if (grantedCharacterTrait != null && grantedCharacterTrait.getMutuallyExclusiveTraits().contains(characterTraitDefinition.getType())) {
                    return true;
                }
            }

            return false;
        }
    }

    private void updateMutualExclusiveTraits() {
        for (CharacterTraitDefinition traitToTest : characterTraitDefinitions.values()) {
            if (this != traitToTest && (this.isMutuallyExclusive(traitToTest) || traitToTest.isMutuallyExclusive(this))) {
                if (!this.mutuallyExclusiveTraits.contains(traitToTest.getType())) {
                    this.mutuallyExclusiveTraits.add(traitToTest.getType());
                }

                if (!traitToTest.mutuallyExclusiveTraits.contains(this.getType())) {
                    traitToTest.mutuallyExclusiveTraits.add(this.getType());
                }
            }
        }
    }
}
