// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.CharacterTraitDefinitionBuilder;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
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
        CharacterTraitDefinitionBuilder builder = new CharacterTraitDefinitionBuilder();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=", 2);
                if (ss.length >= 2) {
                    String k = ss[0].trim();
                    String v = ss[1].trim();
                    if (!k.isEmpty() && !v.isEmpty()) {
                        if (k.equalsIgnoreCase("CharacterTrait")) {
                            builder.characterTrait(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(v)));
                        } else if (k.equalsIgnoreCase("UIName")) {
                            builder.uiName(v);
                        } else if (k.equalsIgnoreCase("Cost")) {
                            builder.cost(Integer.parseInt(v));
                        } else if (k.equalsIgnoreCase("UIDescription")) {
                            builder.uiDescription(v);
                        } else if (k.equalsIgnoreCase("IsProfessionTrait")) {
                            builder.profession(Boolean.parseBoolean(v));
                        } else if (k.equalsIgnoreCase("DisabledInMultiplayer")) {
                            builder.disabledInMultiplayer(Boolean.parseBoolean(v));
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
                        } else if (k.equalsIgnoreCase("MutuallyExclusiveTraits")) {
                            for (String sx : v.split(";")) {
                                builder.addMutuallyExclusiveTrait(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(sx.trim())));
                            }
                        } else if (k.equalsIgnoreCase("Texture")) {
                            builder.texture(Texture.getSharedTexture(v));
                        } else {
                            DebugLog.Script.error("Unknown key '%s' in CharacterTraitDefinitionScript '%s'", k, block.id);
                            if (Core.debug) {
                                throw new Exception("CharacterTraitDefinitionScript error in " + block.id);
                            }
                        }
                    }
                }
            }
        }

        CharacterTraitDefinition.addCharacterTraitDefinition(builder);
    }
}
