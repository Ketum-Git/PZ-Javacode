// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugType;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public final class CharacterTraitDefinitionScript extends BaseScriptObject {
    public CharacterTraitDefinitionScript() {
        super(ScriptType.CharacterTraitDefinition);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        super.LoadCommonBlock(block);
        this.loadCharacterTraitDefinition(block);
    }

    private void loadCharacterTraitDefinition(ScriptParser.Block block) throws Exception {
        CharacterTrait trait = null;
        String uiName = null;
        int cost = 0;
        String uiDescription = null;
        boolean profession = false;
        boolean disabledInMultiplayer = false;
        String textureName = null;
        List<Pair<PerkFactory.Perk, Integer>> xpBoosts = new ArrayList<>();
        List<CharacterTrait> mutuallyExclusiveTraits = new ArrayList<>();
        List<String> grantedRecipes = new ArrayList<>();
        List<CharacterTrait> grantedTraits = new ArrayList<>();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=", 2);
                if (ss.length >= 2) {
                    String k = ss[0].trim();
                    String v = ss[1].trim();
                    if (!k.isEmpty() && !v.isEmpty()) {
                        if (k.equalsIgnoreCase("CharacterTrait")) {
                            trait = Registries.CHARACTER_TRAIT.get(ResourceLocation.of(v));
                        } else if (k.equalsIgnoreCase("UIName")) {
                            uiName = v;
                        } else if (k.equalsIgnoreCase("Cost")) {
                            cost = Integer.parseInt(v);
                        } else if (k.equalsIgnoreCase("UIDescription")) {
                            uiDescription = v;
                        } else if (k.equalsIgnoreCase("IsProfessionTrait")) {
                            profession = Boolean.parseBoolean(v);
                        } else if (k.equalsIgnoreCase("DisabledInMultiplayer")) {
                            disabledInMultiplayer = Boolean.parseBoolean(v);
                        } else if (k.equalsIgnoreCase("GrantedRecipes")) {
                            Collections.addAll(grantedRecipes, v.split(";"));
                        } else if (k.equalsIgnoreCase("GrantedTraits")) {
                            for (String s : v.split(";")) {
                                grantedTraits.add(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(s.trim())));
                            }
                        } else if (k.equalsIgnoreCase("XPBoosts")) {
                            for (String s : v.split(";")) {
                                String[] xpPair = s.split("=");
                                if (xpPair.length >= 2) {
                                    PerkFactory.Perk perk = PerkFactory.Perks.FromString(xpPair[0].trim());
                                    if (perk != PerkFactory.Perks.MAX) {
                                        int boost = PZMath.tryParseInt(xpPair[1], 1);
                                        xpBoosts.add(Pair.of(perk, boost));
                                    }
                                }
                            }
                        } else if (k.equalsIgnoreCase("MutuallyExclusiveTraits")) {
                            for (String sx : v.split(";")) {
                                mutuallyExclusiveTraits.add(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(sx.trim())));
                            }
                        } else if (k.equalsIgnoreCase("Texture")) {
                            textureName = v;
                        } else {
                            DebugType.Script.error("Unknown key '%s' in CharacterTraitDefinitionScript '%s'", k, block.id);
                            if (Core.debug) {
                                throw new Exception("CharacterTraitDefinitionScript error in " + block.id);
                            }
                        }
                    }
                }
            }
        }

        CharacterTraitDefinition characterTraitDefinition = CharacterTraitDefinition.addCharacterTraitDefinition(
            trait, uiName, cost, uiDescription, profession, disabledInMultiplayer
        );

        for (Pair<PerkFactory.Perk, Integer> xpBoost : xpBoosts) {
            characterTraitDefinition.addXPBoost(xpBoost.getLeft(), xpBoost.getRight());
        }

        for (CharacterTrait mutuallyExclusiveCharacterTrait : mutuallyExclusiveTraits) {
            characterTraitDefinition.addMutuallyExclusive(mutuallyExclusiveCharacterTrait);
        }

        for (String freeRecipe : grantedRecipes) {
            characterTraitDefinition.addGrantedRecipe(freeRecipe);
        }

        for (CharacterTrait freeTrait : grantedTraits) {
            characterTraitDefinition.addGrantedTrait(freeTrait);
        }

        if (textureName != null) {
            characterTraitDefinition.setTexture(Texture.getSharedTexture(textureName));
        }
    }
}
