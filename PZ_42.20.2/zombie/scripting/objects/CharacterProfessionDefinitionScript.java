// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugType;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public final class CharacterProfessionDefinitionScript extends BaseScriptObject {
    public CharacterProfessionDefinitionScript() {
        super(ScriptType.CharacterProfessionDefinition);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        super.LoadCommonBlock(block);
        this.loadCharacterProfessionDefinition(block);
    }

    private void loadCharacterProfessionDefinition(ScriptParser.Block block) throws Exception {
        CharacterProfession characterProfessionType = null;
        String uiName = null;
        int cost = 0;
        String uiDescription = null;
        String iconPathName = null;
        List<CharacterTrait> grantedTraits = new ArrayList<>();
        List<String> grantedRecipes = new ArrayList<>();
        List<Pair<PerkFactory.Perk, Integer>> xpBoosts = new ArrayList<>();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=", 2);
                if (ss.length >= 2) {
                    String k = ss[0].trim();
                    String v = ss[1].trim();
                    if (!k.isEmpty() && !v.isEmpty()) {
                        if (k.equalsIgnoreCase("CharacterProfession")) {
                            characterProfessionType = Registries.CHARACTER_PROFESSION.get(ResourceLocation.of(v));
                        } else if (k.equalsIgnoreCase("UIName")) {
                            uiName = v;
                        } else if (k.equalsIgnoreCase("Cost")) {
                            cost = Integer.parseInt(v);
                        } else if (k.equalsIgnoreCase("UIDescription")) {
                            uiDescription = v;
                        } else if (k.equalsIgnoreCase("GrantedRecipes")) {
                            Collections.addAll(grantedRecipes, v.split(";"));
                        } else if (k.equalsIgnoreCase("GrantedTraits")) {
                            for (String s : v.split(";")) {
                                grantedTraits.add(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(s.trim())));
                            }
                        } else if (k.equalsIgnoreCase("XPBoosts")) {
                            for (String s : v.split(";")) {
                                String[] xpPair = s.split("=", 2);
                                if (xpPair.length >= 2) {
                                    PerkFactory.Perk perk = PerkFactory.Perks.FromString(xpPair[0].trim());
                                    if (perk != PerkFactory.Perks.MAX) {
                                        int boost = PZMath.tryParseInt(xpPair[1], 1);
                                        xpBoosts.add(Pair.of(perk, boost));
                                    }
                                }
                            }
                        } else if (k.equalsIgnoreCase("IconPathName")) {
                            iconPathName = v;
                        } else {
                            DebugType.Script.error("Unknown key '%s' in CharacterProfessionDefinitionScript '%s'", k, block.id);
                            if (Core.debug) {
                                throw new Exception("CharacterProfessionDefinitionScript error in " + block.id);
                            }
                        }
                    }
                }
            }
        }

        CharacterProfessionDefinition definition = CharacterProfessionDefinition.addCharacterProfessionDefinition(
            characterProfessionType, uiName, cost, uiDescription, iconPathName
        );

        for (Pair<PerkFactory.Perk, Integer> xpBoost : xpBoosts) {
            definition.addXPBoost(xpBoost.getLeft(), xpBoost.getRight());
        }

        for (String grantedRecipe : grantedRecipes) {
            definition.addGrantedRecipe(grantedRecipe);
        }

        for (CharacterTrait freeTrait : grantedTraits) {
            definition.addGrantedTrait(freeTrait);
        }
    }
}
