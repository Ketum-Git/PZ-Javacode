// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.CharacterProfessionDefinitionBuilder;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
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
        CharacterProfessionDefinitionBuilder builder = new CharacterProfessionDefinitionBuilder();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=", 2);
                if (ss.length >= 2) {
                    String k = ss[0].trim();
                    String v = ss[1].trim();
                    if (!k.isEmpty() && !v.isEmpty()) {
                        if (k.equalsIgnoreCase("CharacterProfession")) {
                            builder.characterProfession(Registries.CHARACTER_PROFESSION.get(ResourceLocation.of(v)));
                        } else if (k.equalsIgnoreCase("UIName")) {
                            builder.uiName(v);
                        } else if (k.equalsIgnoreCase("Cost")) {
                            builder.cost(Integer.parseInt(v));
                        } else if (k.equalsIgnoreCase("UIDescription")) {
                            builder.uiDescription(v);
                        } else if (k.equalsIgnoreCase("GrantedRecipes")) {
                            String[] freeRecipes = v.split(";");

                            for (String freeRecipe : freeRecipes) {
                                builder.addGrantedRecipe(freeRecipe);
                            }
                        } else if (k.equalsIgnoreCase("GrantedTraits")) {
                            for (String s : v.split(";")) {
                                builder.addGrantedTrait(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(s.trim())));
                            }
                        } else if (k.equalsIgnoreCase("XPBoosts")) {
                            for (String s : v.split(";")) {
                                String[] xpPair = s.split("=");
                                if (xpPair.length >= 2) {
                                    PerkFactory.Perk perk = PerkFactory.Perks.FromString(xpPair[0].trim());
                                    if (perk != PerkFactory.Perks.MAX) {
                                        int boost = PZMath.tryParseInt(xpPair[1], 1);
                                        builder.addXPBoost(perk, boost);
                                    }
                                }
                            }
                        } else if (k.equalsIgnoreCase("IconPathName")) {
                            builder.iconPathName(v);
                        } else {
                            DebugLog.Script.error("Unknown key '%s' in CharacterProfessionDefinitionScript '%s'", k, block.id);
                            if (Core.debug) {
                                throw new Exception("CharacterProfessionDefinitionScript error in " + block.id);
                            }
                        }
                    }
                }
            }
        }

        CharacterProfessionDefinition.addCharacterProfessionDefinition(builder);
    }
}
