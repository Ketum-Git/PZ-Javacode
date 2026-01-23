// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.HandcraftLogic;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.crafting.recipe.OverlayMapper;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.util.BitSet;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.CraftRecipeGroup;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.TimedActionScript;
import zombie.util.StringUtils;
import zombie.util.TaggedObjectManager;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
@UsedFromLua
public class CraftRecipe extends BaseScriptObject implements TaggedObjectManager.TaggedObject {
    private String name;
    private String translationName;
    private String iconName;
    private Texture iconTexture;
    private CraftRecipeGroup recipeGroup;
    private boolean needToBeLearn;
    public ArrayList<CraftRecipe.RequiredSkill> skillRequired;
    public ArrayList<CraftRecipe.RequiredSkill> autoLearnAny;
    public ArrayList<CraftRecipe.RequiredSkill> autoLearnAll;
    public ArrayList<CraftRecipe.xp_Award> xpAward;
    public String metaRecipe;
    private boolean hasOnTickInputs;
    private boolean hasOnTickOutputs;
    private int time = 50;
    private String loadedTimedActionScript;
    private TimedActionScript timedActionScript;
    private final HashMap<CraftRecipe.LuaCall, String> luaCalls = new HashMap<>();
    private final ArrayList<InputScript> inputs = new ArrayList<>();
    private final ArrayList<OutputScript> outputs = new ArrayList<>();
    private final ArrayList<CraftRecipe.IOScript> ioLines = new ArrayList<>();
    private String category;
    private final ArrayList<String> categoryTags = new ArrayList<>();
    private final List<String> unmodifiableCategoryTags = PZUnmodifiableList.wrap(this.categoryTags);
    private final BitSet categoryBits = new BitSet();
    private InputScript prop1;
    private InputScript prop2;
    private String animation;
    private InputScript toolLeft;
    private InputScript toolRight;
    private boolean existsAsVanilla;
    private String modId;
    private ChooseGameInfo.Mod modInfo;
    private final HashMap<String, OutputMapper> outputMappers = new HashMap<>();
    private final OverlayMapper overlayMapper = new OverlayMapper();
    private boolean usesTools;
    private String tooltip;
    private boolean allowBatchCraft = true;
    private boolean canWalk;
    private static String luaOnTestCacheString;
    private static Object luaOnTestCacheObject;
    private String onAddToMenu;
    public int researchSkillLevel = -1;
    private final ArrayList<PerkFactory.Perk> researchAll = new ArrayList<>();
    private final ArrayList<PerkFactory.Perk> researchAny = new ArrayList<>();

    public CraftRecipe() {
        super(ScriptType.CraftRecipe);
    }

    protected OutputMapper getOutputMapper(String name) {
        return this.outputMappers.get(name);
    }

    protected OutputMapper getOrCreateOutputMapper(String name) {
        if (this.outputMappers.containsKey(name)) {
            return this.outputMappers.get(name);
        } else {
            OutputMapper outputMapper = new OutputMapper(name);
            this.outputMappers.put(name, outputMapper);
            return outputMapper;
        }
    }

    public OverlayMapper getOverlayMapper() {
        return this.overlayMapper;
    }

    public boolean getExistsAsVanilla() {
        return this.existsAsVanilla;
    }

    public boolean isVanilla() {
        return this.modId != null && this.modId.equals("pz-vanilla");
    }

    public String getModID() {
        return this.modId;
    }

    public String getModName() {
        if (this.modId != null && this.modId.equals("pz-vanilla")) {
            return "Project Zomboid";
        } else {
            return this.modInfo != null && this.modInfo.getName() != null ? this.modInfo.getName() : "<unknown_mod>";
        }
    }

    public String getName() {
        return this.name;
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public void overrideTranslationName(String name) {
        this.translationName = name;
    }

    public String getIconName() {
        return this.iconName;
    }

    public Texture getIconTexture() {
        return this.iconTexture;
    }

    public void overrideIconTexture(Texture icon) {
        this.iconTexture = icon;
    }

    @Deprecated
    public boolean isShapeless() {
        return true;
    }

    @Deprecated
    public boolean isConsumeOnFinish() {
        return false;
    }

    @Deprecated
    public boolean isRequiresPlayer() {
        return false;
    }

    public boolean needToBeLearn() {
        return this.needToBeLearn;
    }

    public boolean canBeResearched() {
        return this.cannotBeResearched() ? false : this.canAlwaysBeResearched() || this.needToBeLearn() && !this.cannotBeResearched();
    }

    public boolean canAlwaysBeResearched() {
        return this.cannotBeResearched()
            ? false
            : this.needToBeLearn() && (this.hasTag(CraftRecipeTag.CAN_ALWAYS_BE_RESEARCHED) || this.getResearchSkillLevel() == 0);
    }

    public boolean cannotBeResearched() {
        return this.hasTag(CraftRecipeTag.CANNOT_BE_RESEARCHED);
    }

    public int getTime() {
        return this.time;
    }

    public int getTime(IsoGameCharacter character) {
        if (this.getHighestRelevantSkillLevel(character) <= this.getHighestSkillRequirement()) {
            return this.time;
        } else {
            int modifier = this.getHighestRelevantSkillLevel(character) - this.getHighestSkillRequirement();
            int timeIncrement = this.time / 20;
            return this.time - modifier * timeIncrement;
        }
    }

    public TimedActionScript getTimedActionScript() {
        return this.timedActionScript;
    }

    public CraftRecipeGroup getRecipeGroup() {
        return this.recipeGroup;
    }

    public String getCategory() {
        return StringUtils.isNullOrWhitespace(this.category) ? "Miscellaneous" : this.category;
    }

    @Override
    public List<String> getTags() {
        return this.unmodifiableCategoryTags;
    }

    @Override
    public BitSet getTagBits() {
        return this.categoryBits;
    }

    public int getInputCount() {
        return this.inputs.size();
    }

    public int getOutputCount() {
        return this.outputs.size();
    }

    public ArrayList<InputScript> getInputs() {
        return this.inputs;
    }

    public ArrayList<OutputScript> getOutputs() {
        return this.outputs;
    }

    public ArrayList<CraftRecipe.IOScript> getIoLines() {
        return this.ioLines;
    }

    public int getIndexForIO(CraftRecipe.IOScript script) {
        return this.ioLines.indexOf(script);
    }

    public CraftRecipe.IOScript getIOForIndex(int index) {
        return index >= 0 && index < this.ioLines.size() ? this.ioLines.get(index) : null;
    }

    public boolean containsIO(CraftRecipe.IOScript script) {
        return this.ioLines.contains(script);
    }

    public boolean isUsesTools() {
        return this.usesTools;
    }

    public InputScript getToolLeft() {
        return this.toolLeft;
    }

    public InputScript getToolRight() {
        return this.toolRight;
    }

    public InputScript getToolBoth() {
        return null;
    }

    public InputScript getProp1() {
        return this.prop1;
    }

    public void setProp1(InputScript prop) {
        this.prop1 = prop;
    }

    public InputScript getProp2() {
        return this.prop2;
    }

    public void setProp2(InputScript prop) {
        this.prop2 = prop;
    }

    public String getAnimation() {
        return this.animation;
    }

    public void setAnimation(String animationString) {
        this.animation = animationString;
    }

    public boolean hasOnTickInputs() {
        return this.hasOnTickInputs;
    }

    public boolean hasOnTickOutputs() {
        return this.hasOnTickOutputs;
    }

    public boolean hasLuaCall(CraftRecipe.LuaCall luaCall) {
        return this.getLuaCallString(luaCall) != null;
    }

    public String getLuaCallString(CraftRecipe.LuaCall luaCall) {
        return this.luaCalls.get(luaCall);
    }

    private void setLuaCall(CraftRecipe.LuaCall luaCall, String luaFunction) {
        this.luaCalls.put(luaCall, luaFunction);
    }

    public String getTooltip() {
        return this.tooltip;
    }

    public boolean isAllowBatchCraft() {
        return this.allowBatchCraft;
    }

    public boolean isCanWalk() {
        return this.canWalk;
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        this.modId = ScriptManager.getCurrentLoadFileMod();
        if (this.modId.equals("pz-vanilla")) {
            this.existsAsVanilla = true;
        } else {
            this.modInfo = ChooseGameInfo.getModDetails(this.modId);
        }
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.Load(name, block);
    }

    public void Load(String name, ScriptParser.Block block) throws Exception {
        this.name = name;
        this.translationName = Translator.getRecipeName(name);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim().intern();
            if (!key.isEmpty() && !val.isEmpty() && !key.equalsIgnoreCase("consumeOnFinish")) {
                if (key.equalsIgnoreCase("recipeGroup")) {
                    this.recipeGroup = CraftRecipeGroup.fromString(val);
                } else if (key.equalsIgnoreCase("icon")) {
                    this.iconName = val;
                } else if (key.equalsIgnoreCase("time")) {
                    this.time = Integer.parseInt(val);
                } else if (key.equalsIgnoreCase("ResearchSkillLevel")) {
                    this.researchSkillLevel = Integer.parseInt(val);
                } else if (key.equalsIgnoreCase("OnTest")) {
                    this.setLuaCall(CraftRecipe.LuaCall.OnTest, val);
                } else if (key.equalsIgnoreCase("OnAddToMenu")) {
                    this.onAddToMenu = val;
                } else if (key.equalsIgnoreCase("OnStart")) {
                    this.setLuaCall(CraftRecipe.LuaCall.OnStart, val);
                } else if (key.equalsIgnoreCase("OnUpdate")) {
                    this.setLuaCall(CraftRecipe.LuaCall.OnUpdate, val);
                } else if (key.equalsIgnoreCase("OnCreate")) {
                    this.setLuaCall(CraftRecipe.LuaCall.OnCreate, val);
                } else if (key.equalsIgnoreCase("OnFailed")) {
                    this.setLuaCall(CraftRecipe.LuaCall.OnFailed, val);
                } else if (!key.equalsIgnoreCase("requiresPlayer")) {
                    if (key.equalsIgnoreCase("needToBeLearn")) {
                        this.needToBeLearn = val.equalsIgnoreCase("true");
                    } else if (key.equalsIgnoreCase("SkillRequired")) {
                        this.skillRequired = new ArrayList<>();
                        String[] split2 = val.split(";");

                        for (int j = 0; j < split2.length; j++) {
                            String[] split3 = split2[j].split(":");
                            PerkFactory.Perk perks = PerkFactory.Perks.FromString(split3[0]);
                            if (perks == PerkFactory.Perks.MAX) {
                                DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", split3[0], this.name);
                            } else {
                                int level = PZMath.tryParseInt(split3[1], 1);
                                CraftRecipe.RequiredSkill skill = new CraftRecipe.RequiredSkill(perks, level);
                                if (level > 0) {
                                    this.skillRequired.add(skill);
                                }
                            }
                        }
                    } else if (key.equalsIgnoreCase("AutoLearnAny")) {
                        this.autoLearnAny = new ArrayList<>();
                        String[] split2 = val.split(";");

                        for (int jx = 0; jx < split2.length; jx++) {
                            String[] split3 = split2[jx].split(":");
                            PerkFactory.Perk perks = PerkFactory.Perks.FromString(split3[0]);
                            if (perks == PerkFactory.Perks.MAX) {
                                DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", split3[0], this.name);
                            } else {
                                int level = PZMath.tryParseInt(split3[1], 1);
                                CraftRecipe.RequiredSkill skill = new CraftRecipe.RequiredSkill(perks, level);
                                this.autoLearnAny.add(skill);
                            }
                        }
                    } else if (key.equalsIgnoreCase("AutoLearnAll")) {
                        this.autoLearnAll = new ArrayList<>();
                        String[] split2 = val.split(";");

                        for (int jxx = 0; jxx < split2.length; jxx++) {
                            String[] split3 = split2[jxx].split(":");
                            PerkFactory.Perk perks = PerkFactory.Perks.FromString(split3[0]);
                            if (perks == PerkFactory.Perks.MAX) {
                                DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", split3[0], this.name);
                            } else {
                                int level = PZMath.tryParseInt(split3[1], 1);
                                CraftRecipe.RequiredSkill skill = new CraftRecipe.RequiredSkill(perks, level);
                                this.autoLearnAll.add(skill);
                            }
                        }
                    } else if (key.equalsIgnoreCase("ResearchAll")) {
                        String[] split2 = val.split(";");

                        for (int jxxx = 0; jxxx < split2.length; jxxx++) {
                            PerkFactory.Perk perk = PerkFactory.Perks.FromString(split2[jxxx]);
                            if (perk == PerkFactory.Perks.MAX) {
                                DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", split2[jxxx], this.name);
                            } else {
                                this.researchAll.add(perk);
                            }
                        }
                    } else if (key.equalsIgnoreCase("ResearchAny")) {
                        String[] split2 = val.split(";");

                        for (int jxxxx = 0; jxxxx < split2.length; jxxxx++) {
                            PerkFactory.Perk perk = PerkFactory.Perks.FromString(split2[jxxxx]);
                            if (perk == PerkFactory.Perks.MAX) {
                                DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", split2[jxxxx], this.name);
                            } else {
                                this.researchAny.add(perk);
                            }
                        }
                    } else if (key.equalsIgnoreCase("xpAward")) {
                        this.xpAward = new ArrayList<>();
                        String[] split2 = val.split(";");

                        for (int jxxxxx = 0; jxxxxx < split2.length; jxxxxx++) {
                            String[] split3 = split2[jxxxxx].split(":");
                            PerkFactory.Perk perks = PerkFactory.Perks.FromString(split3[0]);
                            if (perks == PerkFactory.Perks.MAX) {
                                DebugLog.Recipe.warn("Unknown skill \"%s\" in recipe \"%s\"", split3[0], this.name);
                            } else {
                                int level = PZMath.tryParseInt(split3[1], 1);
                                CraftRecipe.xp_Award skill = new CraftRecipe.xp_Award(perks, level);
                                this.xpAward.add(skill);
                            }
                        }
                    } else if (key.equalsIgnoreCase("MetaRecipe")) {
                        this.metaRecipe = val.trim();
                    } else if (key.equalsIgnoreCase("Animation")) {
                        this.animation = val.trim();
                    } else if (key.equalsIgnoreCase("timedAction")) {
                        this.loadedTimedActionScript = val;
                    } else if (key.equalsIgnoreCase("category")) {
                        this.category = val;
                    } else if (key.equalsIgnoreCase("Tags")) {
                        String[] split = val.split(";");

                        for (int i = 0; i < split.length; i++) {
                            this.categoryTags.add(split[i].trim());
                        }
                    } else if (key.equalsIgnoreCase("Tooltip")) {
                        this.tooltip = StringUtils.discardNullOrWhitespace(val);
                    } else if (key.equalsIgnoreCase("AllowBatchCraft")) {
                        this.allowBatchCraft = val.equalsIgnoreCase("true");
                    } else if (key.equalsIgnoreCase("CanWalk")) {
                        this.canWalk = val.equalsIgnoreCase("true");
                    } else if (key.equalsIgnoreCase("overlayStyle")) {
                        this.getOverlayMapper().setDefaultOverlayStyle(val);
                    } else {
                        DebugLog.Recipe.error("Unknown key '" + key + "' val(" + val + ") in craft recipe: " + name);
                        if (Core.debug) {
                            throw new Exception("CraftRecipe error in " + name);
                        }
                    }
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("inputs".equalsIgnoreCase(child.type) || "outputs".equalsIgnoreCase(child.type)) {
                this.LoadIO(child, "inputs".equalsIgnoreCase(child.type));
            } else if ("itemMapper".equalsIgnoreCase(child.type)) {
                this.LoadOutputMapper(child);
            } else {
                if ("fluidMapper".equalsIgnoreCase(child.type)) {
                    throw new Exception("Not yet implemented.");
                }

                if ("energyMapper".equalsIgnoreCase(child.type)) {
                    throw new Exception("Not yet implemented.");
                }

                if ("overlayMapper".equalsIgnoreCase(child.type)) {
                    this.LoadOverlayMapper(child);
                } else {
                    DebugLog.Recipe.error("Unknown block '" + child.type + "' in craft recipe: " + name);
                    if (Core.debug) {
                        throw new Exception("CraftRecipe error in " + name + ", Unknown block '" + child.type + "' in craft recipe: " + name);
                    }
                }
            }
        }
    }

    private void LoadIO(ScriptParser.Block block, boolean isInput) throws Exception {
        InputScript lastInput = null;
        OutputScript lastOutput = null;

        for (ScriptParser.Value value : block.values) {
            if (value.string != null) {
                String s = value.string.trim();
                if (!StringUtils.isNullOrWhitespace(s)) {
                    if (!StringUtils.containsWhitespace(s)) {
                        DebugLog.Recipe.warn("Cannot load: " + value.string);
                    }

                    if (isInput) {
                        if (s.toLowerCase().startsWith("replace")) {
                            DebugLog.Recipe.error("Replace inputs have been deprecated in CraftRecipes.");
                        } else if (s.startsWith("+")) {
                            if (lastInput == null) {
                                throw new IOException("Previous input is null [" + this.name + "] line: " + s);
                            }

                            if (lastInput.createToItemScript != null) {
                                throw new IOException("Previous input already has '+' output [" + this.name + "] line: " + s);
                            }

                            s = s.substring(1);
                            OutputScript script = OutputScript.Load(this, s, true);
                            lastInput.createToItemScript = script;
                            this.ioLines.add(script);
                        } else if (s.startsWith("-")) {
                            if (lastInput == null) {
                                throw new IOException("Previous input is null [" + this.name + "] line: " + s);
                            }

                            if (lastInput.consumeFromItemScript != null) {
                                throw new IOException("Previous input already has '-' input [" + this.name + "] line: " + s);
                            }

                            s = s.substring(1);
                            InputScript script = InputScript.Load(this, s, true);
                            lastInput.consumeFromItemScript = script;
                            script.parentScript = lastInput;
                            this.ioLines.add(script);
                        } else {
                            InputScript input = InputScript.Load(this, value.string);
                            this.inputs.add(input);
                            this.ioLines.add(input);
                            lastInput = input;
                        }
                    } else if (s.startsWith("+")) {
                        if (lastOutput == null) {
                            throw new IOException("Previous input is null [" + this.name + "] line: " + s);
                        }

                        s = s.substring(1);
                        OutputScript script = OutputScript.Load(this, s, true);
                        lastOutput.createToItemScript = script;
                        this.ioLines.add(script);
                        lastOutput = null;
                    } else {
                        if (s.startsWith("-")) {
                            throw new IOException("Cannot add '-' line to output, [" + this.name + "] line: " + s);
                        }

                        OutputScript output = OutputScript.Load(this, value.string);
                        this.outputs.add(output);
                        this.ioLines.add(output);
                        lastOutput = output;
                    }
                }
            }
        }
    }

    private void LoadOutputMapper(ScriptParser.Block block) throws Exception {
        OutputMapper outputMapper = this.getOrCreateOutputMapper(block.id.trim());

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("default")) {
                    outputMapper.setDefaultOutputEntree(val);
                } else {
                    String[] split = val.split(";");

                    for (int i = 0; i < split.length; i++) {
                        String s = split[i];
                        if (s.contains("$")) {
                            String var10 = s.substring(0, s.indexOf("$")).toLowerCase();
                            switch (var10) {
                                case "fluid":
                                case "energy":
                                    throw new Exception("Not yet implemented");
                                default:
                                    split[i] = s.substring(s.indexOf("$") + 1);
                            }
                        }
                    }

                    outputMapper.addOutputEntree(key, split);
                }
            }
        }

        if (outputMapper.isEmpty()) {
            throw new Exception("Failed to load contents for output mapper: " + block.id);
        }
    }

    private void LoadOverlayMapper(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("default")) {
                    this.overlayMapper.setDefaultOverlayStyle(val);
                } else {
                    String[] split = key.split(";");
                    this.overlayMapper.addOverlayStyleEntry(val, split);
                }
            }
        }

        if (this.overlayMapper.isEmpty()) {
            throw new Exception("Failed to load contents for overlay mapper.");
        }
    }

    @Override
    public void PreReload() {
        this.name = null;
        this.translationName = null;
        this.enabled = true;
        this.debugOnly = false;
        this.iconName = null;
        this.iconTexture = null;
        this.recipeGroup = null;
        this.needToBeLearn = false;
        this.skillRequired = null;
        this.autoLearnAny = null;
        this.autoLearnAll = null;
        this.hasOnTickInputs = false;
        this.hasOnTickOutputs = false;
        this.time = 50;
        this.loadedTimedActionScript = null;
        this.timedActionScript = null;
        this.luaCalls.clear();
        this.inputs.clear();
        this.outputs.clear();
        this.ioLines.clear();
        this.category = null;
        this.categoryTags.clear();
        this.categoryBits.clear();
        this.prop1 = null;
        this.prop2 = null;
        this.animation = null;
        this.overlayMapper.clear();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        String recipeName = this.name;
        if (this.iconName != null) {
            if (!this.iconName.startsWith("Item_")) {
                this.iconName = "Item_" + this.iconName;
            }

            this.iconTexture = Texture.trygetTexture(this.iconName);
            if (Core.debug && this.iconName != null && this.iconTexture == null) {
                DebugLog.Recipe.error("Icon not found: " + this.iconName);
                DebugLog.Recipe.printStackTrace();
            }
        }

        for (InputScript input : this.inputs) {
            input.OnScriptsLoaded(loadMode);
        }

        for (OutputScript output : this.outputs) {
            output.OnScriptsLoaded(loadMode);
        }

        for (InputScript input : this.inputs) {
            if (input.isApplyOnTick()) {
                this.hasOnTickInputs = true;
            }

            if (input.isTool()) {
                this.usesTools = true;
                if (input.hasFlag(InputFlag.ToolLeft)) {
                    if (this.toolLeft != null) {
                        throw new Exception("Duplicate tool left in recipe " + this.name);
                    }

                    this.toolLeft = input;
                }

                if (input.hasFlag(InputFlag.ToolRight)) {
                    if (this.toolRight != null) {
                        throw new Exception("Duplicate tool left in recipe " + this.name);
                    }

                    this.toolRight = input;
                }
            }

            if (input.isProp1()) {
                if (this.getProp1() != null) {
                    throw new IOException("Duplicate Prop1 in recipe " + this.name);
                }

                this.setProp1(input);
            }

            if (input.isProp2()) {
                if (this.getProp2() != null) {
                    throw new IOException("Duplicate Prop2 in recipe " + this.name);
                }

                this.setProp2(input);
            }
        }

        for (OutputScript output : this.outputs) {
            if (output.isApplyOnTick()) {
                this.hasOnTickOutputs = true;
            }
        }

        if (this.loadedTimedActionScript != null) {
            this.timedActionScript = ScriptManager.instance.getTimedActionScript(this.loadedTimedActionScript);
            if (this.timedActionScript == null) {
                throw new Exception("TimedActionScript '" + this.loadedTimedActionScript + "' could not be found in recipe " + this.name);
            }
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
        Texture backupIcon = null;

        for (InputScript input : this.inputs) {
            input.OnPostWorldDictionaryInit();
            if (backupIcon == null && input.getResourceType() == ResourceType.Item && !input.getPossibleInputItems().isEmpty()) {
                backupIcon = input.getPossibleInputItems().get(0).getNormalTexture();
            }
        }

        for (OutputScript output : this.outputs) {
            output.OnPostWorldDictionaryInit();
            if (this.iconTexture == null && output.getResourceType() == ResourceType.Item && !output.getPossibleResultItems().isEmpty()) {
                this.iconTexture = output.getPossibleResultItems().get(0).getNormalTexture();
            }
        }

        if (this.iconTexture == null) {
            this.iconTexture = backupIcon;
        }
    }

    public ArrayList<String> getRequiredSkills() {
        ArrayList<String> result = new ArrayList<>();
        if (this.skillRequired != null) {
            for (int i = 0; i < this.skillRequired.size(); i++) {
                CraftRecipe.RequiredSkill skill = this.skillRequired.get(i);
                PerkFactory.Perk perk = PerkFactory.getPerk(skill.getPerk());
                if (perk == null) {
                    if (skill.getPerk() != null) {
                        result.add(skill.getPerk().name + " " + skill.getLevel());
                    }
                } else {
                    String newEntry = perk.name + " " + skill.getLevel();
                    result.add(newEntry);
                }
            }
        }

        return result;
    }

    public ArrayList<String> getAutoLearnAnySkills() {
        ArrayList<String> result = null;
        if (this.autoLearnAny != null) {
            result = new ArrayList<>();

            for (int i = 0; i < this.autoLearnAny.size(); i++) {
                CraftRecipe.RequiredSkill skill = this.autoLearnAny.get(i);
                PerkFactory.Perk perk = PerkFactory.getPerk(skill.getPerk());
                if (perk == null) {
                    if (skill.getPerk() != null) {
                        result.add(skill.getPerk().name + " " + skill.getLevel());
                    }
                } else {
                    String newEntry = perk.name + " " + skill.getLevel();
                    result.add(newEntry);
                }
            }
        }

        return result;
    }

    public ArrayList<String> getAutoLearnAllSkills() {
        ArrayList<String> result = null;
        if (this.autoLearnAll != null) {
            result = new ArrayList<>();

            for (int i = 0; i < this.autoLearnAll.size(); i++) {
                CraftRecipe.RequiredSkill skill = this.autoLearnAll.get(i);
                PerkFactory.Perk perk = PerkFactory.getPerk(skill.getPerk());
                if (perk == null) {
                    if (skill.getPerk() != null) {
                        result.add(skill.getPerk().name + " " + skill.getLevel());
                    }
                } else {
                    String newEntry = perk.name + " " + skill.getLevel();
                    result.add(newEntry);
                }
            }
        }

        return result;
    }

    public void checkAutoLearnAnySkills(IsoGameCharacter chr) {
        this.checkAutoLearnAnySkills(chr, false);
    }

    public void checkAutoLearnAnySkills(IsoGameCharacter chr, boolean textSpam) {
        if (!chr.isRecipeActuallyKnown(this) && this.getAutoLearnAnySkillCount() > 0 && this.validateHasAutoLearnAnySkill(chr)) {
            chr.learnRecipe(this.getName(), false);
            DebugLog.log("Recipe AutoLearned AnySkills - " + this.getName());
            if (textSpam && chr != null) {
                HaloTextHelper.addGoodText(
                    (IsoPlayer)chr, Translator.getText("IGUI_HaloNote_LearnedRecipe", LuaManager.GlobalObject.getRecipeDisplayName(this.getName())), "[br/]"
                );
            }
        }
    }

    public void checkAutoLearnAllSkills(IsoGameCharacter chr) {
        this.checkAutoLearnAllSkills(chr, false);
    }

    public void checkAutoLearnAllSkills(IsoGameCharacter chr, boolean textSpam) {
        if (!chr.isRecipeActuallyKnown(this) && this.getAutoLearnAllSkillCount() > 0 && this.validateHasAutoLearnAllSkill(chr)) {
            chr.learnRecipe(this.getName(), false);
            DebugLog.log("Recipe AutoLearned AllSkills - " + this.getName());
            if (textSpam && chr != null) {
                HaloTextHelper.addGoodText(
                    (IsoPlayer)chr, Translator.getText("IGUI_HaloNote_LearnedRecipe", LuaManager.GlobalObject.getRecipeDisplayName(this.getName())), "[br/]"
                );
            }
        }
    }

    private boolean validateHasAutoLearnAnySkill(IsoGameCharacter chr) {
        if (this.getAutoLearnAnySkills() != null && this.getAutoLearnAnySkillCount() > 0) {
            for (int i = 0; i < this.getAutoLearnAnySkillCount(); i++) {
                CraftRecipe.RequiredSkill skill = this.getAutoLearnAnySkill(i);
                int level = skill.getLevel();
                if (chr.hasTrait(CharacterTrait.INVENTIVE)) {
                    level--;
                }

                level = Math.max(1, level);
                if (chr.getPerkLevel(skill.getPerk()) >= level) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean validateHasAutoLearnAllSkill(IsoGameCharacter chr) {
        if (this.getAutoLearnAllSkillCount() > 0) {
            for (int i = 0; i < this.getAutoLearnAllSkillCount(); i++) {
                CraftRecipe.RequiredSkill skill = this.getAutoLearnAllSkill(i);
                int level = skill.getLevel();
                if (chr.hasTrait(CharacterTrait.INVENTIVE)) {
                    level--;
                }

                level = Math.max(1, level);
                if (chr.getPerkLevel(skill.getPerk()) < level) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void checkMetaRecipe(IsoGameCharacter chr, String checkedRecipe) {
        if (!chr.isRecipeActuallyKnown(this) && this.getMetaRecipe() != null && chr.isRecipeActuallyKnown(this.getMetaRecipe())) {
            chr.learnRecipe(this.getName(), false);
            DebugLog.log("Recipe MetaRecipe-Learned - " + this.getName() + " from " + checkedRecipe);
        }
    }

    public void checkMetaRecipe(IsoGameCharacter chr) {
        if (!chr.isRecipeActuallyKnown(this) && this.getMetaRecipe() != null && chr.isRecipeActuallyKnown(this.getMetaRecipe())) {
            chr.learnRecipe(this.getName(), false);
            DebugLog.log("Recipe MetaRecipe-Learned - " + this.getName() + " from " + this.getMetaRecipe());
        }
    }

    public int getRequiredSkillCount() {
        return this.skillRequired == null ? 0 : this.skillRequired.size();
    }

    public CraftRecipe.RequiredSkill getRequiredSkill(int index) {
        return this.skillRequired != null && index >= 0 && index < this.skillRequired.size() ? this.skillRequired.get(index) : null;
    }

    public int getAutoLearnAnySkillCount() {
        return this.autoLearnAny == null ? 0 : this.autoLearnAny.size();
    }

    public int getAutoLearnAllSkillCount() {
        return this.autoLearnAll == null ? 0 : this.autoLearnAll.size();
    }

    public CraftRecipe.RequiredSkill getAutoLearnAnySkill(int index) {
        return this.autoLearnAny != null && index >= 0 && index < this.autoLearnAny.size() ? this.autoLearnAny.get(index) : null;
    }

    public CraftRecipe.RequiredSkill getAutoLearnAllSkill(int index) {
        return this.autoLearnAll != null && index >= 0 && index < this.autoLearnAll.size() ? this.autoLearnAll.get(index) : null;
    }

    public String getMetaRecipe() {
        return this.metaRecipe;
    }

    public int getXPAwardCount() {
        return this.xpAward == null ? 0 : this.xpAward.size();
    }

    public CraftRecipe.xp_Award getXPAward(int index) {
        return this.xpAward != null && index >= 0 && index < this.xpAward.size() ? this.xpAward.get(index) : null;
    }

    public void clearRequiredSkills() {
        if (this.skillRequired != null) {
            this.skillRequired.clear();
        }
    }

    public void addRequiredSkill(PerkFactory.Perk perk, int level) {
        if (this.skillRequired == null) {
            this.skillRequired = new ArrayList<>();
        }

        this.skillRequired.add(new CraftRecipe.RequiredSkill(perk, level));
    }

    public boolean canUseItem(InventoryItem item, IsoGameCharacter character) {
        return CraftRecipeManager.getValidInputScriptForItem(this, item, character) != null;
    }

    public boolean canUseItem(String item) {
        ArrayList<InputScript> inputs = this.getInputs();

        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).canUseItem(item)) {
                return true;
            }
        }

        return false;
    }

    public boolean OnTestItem(InventoryItem inventoryItem, IsoGameCharacter character) {
        if (inventoryItem == null) {
            return false;
        } else {
            if (this.hasLuaCall(CraftRecipe.LuaCall.OnTest)) {
                String luaCallString = this.getLuaCallString(CraftRecipe.LuaCall.OnTest);
                Object functionObject;
                if (luaOnTestCacheObject != null && luaOnTestCacheString != null && luaOnTestCacheString.equals(luaCallString)) {
                    functionObject = luaOnTestCacheObject;
                } else {
                    functionObject = LuaManager.getFunctionObject(this.getLuaCallString(CraftRecipe.LuaCall.OnTest), null);
                    luaOnTestCacheString = luaCallString;
                    luaOnTestCacheObject = functionObject;
                }

                if (functionObject != null) {
                    Boolean aBoolean = LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObject, inventoryItem, character);
                    return aBoolean != null && aBoolean;
                }
            }

            return true;
        }
    }

    public boolean hasTag(CraftRecipeTag craftRecipeTag) {
        return this.unmodifiableCategoryTags.contains(craftRecipeTag.toString());
    }

    public boolean isCanBeDoneFromFloor() {
        return this.hasTag(CraftRecipeTag.CAN_BE_DONE_FROM_FLOOR);
    }

    public boolean canBeDoneInDark() {
        return this.hasTag(CraftRecipeTag.CAN_BE_DONE_IN_DARK);
    }

    public boolean isAnySurfaceCraft() {
        return this.hasTag(CraftRecipeTag.ANY_SURFACE_CRAFT);
    }

    public boolean isInHandCraftCraft() {
        return this.hasTag(CraftRecipeTag.IN_HAND_CRAFT);
    }

    public boolean isAutoRotate() {
        return this.hasTag(CraftRecipeTag.AUTO_ROTATE);
    }

    public int getHighestRelevantSkillLevel(IsoGameCharacter character) {
        return this.getHighestRelevantSkillLevel(character, true);
    }

    public int getHighestRelevantSkillLevel(IsoGameCharacter character, boolean includeAutoLearn) {
        int highest = 0;
        if (this.getRequiredSkills() != null) {
            for (int i = 0; i < this.getRequiredSkills().size(); i++) {
                PerkFactory.Perk perk = this.getRequiredSkill(i).getPerk();
                if (perk != null && character.getPerkLevel(perk) > highest) {
                    highest = character.getPerkLevel(perk);
                }
            }
        }

        if (this.getXPAwardCount() > 0) {
            for (int ix = 0; ix < this.getXPAwardCount(); ix++) {
                PerkFactory.Perk perk = this.getXPAward(ix).getPerk();
                if (perk != null && character.getPerkLevel(perk) > highest) {
                    highest = character.getPerkLevel(perk);
                }
            }
        }

        if (includeAutoLearn && this.getAutoLearnAnySkills() != null && this.getAutoLearnAnySkillCount() > 0) {
            for (int ixx = 0; ixx < this.getAutoLearnAnySkillCount(); ixx++) {
                PerkFactory.Perk perk = this.getAutoLearnAnySkill(ixx).getPerk();
                if (perk != null && character.getPerkLevel(perk) > highest) {
                    highest = character.getPerkLevel(perk);
                }
            }
        }

        if (includeAutoLearn && this.getAutoLearnAllSkills() != null && this.getAutoLearnAllSkillCount() > 0) {
            for (int ixxx = 0; ixxx < this.getAutoLearnAllSkillCount(); ixxx++) {
                PerkFactory.Perk perk = this.getAutoLearnAllSkill(ixxx).getPerk();
                if (perk != null && character.getPerkLevel(perk) > highest) {
                    highest = character.getPerkLevel(perk);
                }
            }
        }

        return highest;
    }

    public PerkFactory.Perk getHighestRelevantSkill(IsoGameCharacter character) {
        PerkFactory.Perk perk = null;
        int highest = -1;

        for (int i = 0; i < this.getRequiredSkillCount(); i++) {
            PerkFactory.Perk perk2 = this.getRequiredSkill(i).getPerk();
            if (perk2 != null && character.getPerkLevel(perk2) > highest) {
                perk = perk2;
                highest = character.getPerkLevel(perk2);
            }
        }

        return perk;
    }

    public PerkFactory.Perk getHighestRelevantSkillFromXpAward(IsoGameCharacter character) {
        PerkFactory.Perk perk = null;
        int highest = -1;

        for (int i = 0; i < this.getXPAwardCount(); i++) {
            PerkFactory.Perk perk2 = this.getXPAward(i).getPerk();
            if (perk2 != null && character.getPerkLevel(perk2) > highest) {
                perk = perk2;
                highest = character.getPerkLevel(perk2);
            }
        }

        return perk;
    }

    public static void onLuaFileReloaded() {
        luaOnTestCacheObject = null;
        luaOnTestCacheString = null;
    }

    public String getOnAddToMenu() {
        return this.onAddToMenu;
    }

    public boolean involvesSkill(PerkFactory.Perk skill) {
        return this.involvesSkill(skill, false);
    }

    public boolean involvesSkill(PerkFactory.Perk skill, boolean includeAutoLearn) {
        int highest = 0;
        if (this.getRequiredSkills() != null) {
            for (int i = 0; i < this.getRequiredSkills().size(); i++) {
                PerkFactory.Perk perk = this.getRequiredSkill(i).getPerk();
                if (perk != null && skill == perk) {
                    return true;
                }
            }
        }

        if (this.getXPAwardCount() > 0) {
            for (int ix = 0; ix < this.getXPAwardCount(); ix++) {
                PerkFactory.Perk perk = this.getXPAward(ix).getPerk();
                if (perk != null && skill == perk) {
                    return true;
                }
            }
        }

        if (includeAutoLearn && this.getAutoLearnAnySkills() != null && this.getAutoLearnAnySkillCount() > 0) {
            for (int ixx = 0; ixx < this.getAutoLearnAnySkillCount(); ixx++) {
                PerkFactory.Perk perk = this.getAutoLearnAnySkill(ixx).getPerk();
                if (perk != null && skill == perk) {
                    return true;
                }
            }
        }

        if (includeAutoLearn && this.getAutoLearnAnySkills() != null && this.getAutoLearnAllSkillCount() > 0) {
            for (int ixxx = 0; ixxx < this.getAutoLearnAllSkillCount(); ixxx++) {
                PerkFactory.Perk perk = this.getAutoLearnAllSkill(ixxx).getPerk();
                if (perk != null && skill == perk) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSmithing() {
        return this.involvesSkill(PerkFactory.Perks.Blacksmith) || Objects.equals(this.getCategory(), "Blacksmithing");
    }

    public boolean canOutputItem(InventoryItem item) {
        return item.getScriptItem() == null ? false : this.canOutputItem(item.getScriptItem());
    }

    public boolean canOutputItem(Item item) {
        if (this.getOutputs() == null) {
            return false;
        } else {
            ArrayList<OutputScript> outputs = this.getOutputs();

            for (int i = 0; i < this.getOutputs().size(); i++) {
                if (this.getOutputs().get(i) != null && this.getOutputs().get(i).canOutputItem(item)) {
                    return true;
                }
            }

            return false;
        }
    }

    public void setResearchSkillLevel(int level) {
        level = this.normalizeSkillLevel(level);
        this.researchSkillLevel = level;
    }

    public int getResearchSkillLevel() {
        return this.getResearchSkillLevel(null);
    }

    public int getResearchSkillLevel(IsoGameCharacter chr) {
        if (this.researchSkillLevel == -1 && this.getHighestSkillRequirement() > 0) {
            float researchLevel = this.getHighestSkillRequirement() * 2.0F / 3.0F;
            researchLevel = Math.round(researchLevel);
            int level = (int)Math.max(1.0F, researchLevel);
            this.setResearchSkillLevel(level);
        } else if (this.researchSkillLevel == -1 && this.getHighestSkillRequirement(true) > 0) {
            float researchLevel = this.getHighestSkillRequirement(true) * 2.0F / 3.0F;
            researchLevel = Math.round(researchLevel);
            int level = (int)Math.max(1.0F, researchLevel);
            this.setResearchSkillLevel(level);
        }

        this.researchSkillLevel = this.normalizeSkillLevel(this.researchSkillLevel);
        if (chr == null) {
            return this.researchSkillLevel;
        } else {
            int level = this.researchSkillLevel;
            if (chr.hasTrait(CharacterTrait.INVENTIVE)) {
                level -= 2;
            }

            return this.normalizeSkillLevel(level);
        }
    }

    public int normalizeSkillLevel(int level) {
        level = Math.max(0, level);
        return Math.min(10, level);
    }

    public int getHighestSkillRequirement() {
        return this.getHighestSkillRequirement(false);
    }

    public int getHighestSkillRequirement(boolean includeAutoLearn) {
        int highest = 0;
        if (this.getRequiredSkills() != null) {
            for (int i = 0; i < this.getRequiredSkills().size(); i++) {
                if (this.getRequiredSkill(i).getLevel() > highest) {
                    highest = this.getRequiredSkill(i).getLevel();
                }
            }
        }

        if (includeAutoLearn && this.getAutoLearnAnySkills() != null && this.getAutoLearnAnySkillCount() > 0) {
            for (int ix = 0; ix < this.getAutoLearnAnySkillCount(); ix++) {
                if (this.getAutoLearnAnySkill(ix).getLevel() > highest) {
                    highest = this.getAutoLearnAnySkill(ix).getLevel();
                }
            }
        }

        if (includeAutoLearn && this.getAutoLearnAnySkills() != null && this.getAutoLearnAllSkillCount() > 0) {
            for (int ixx = 0; ixx < this.getAutoLearnAllSkillCount(); ixx++) {
                if (this.getAutoLearnAllSkill(ixx).getLevel() > highest) {
                    highest = this.getAutoLearnAllSkill(ixx).getLevel();
                }
            }
        }

        return highest;
    }

    public PerkFactory.Perk getHighestPerkRequirement() {
        int highest = 0;
        PerkFactory.Perk perk = null;
        if (this.getRequiredSkills() != null) {
            for (int i = 0; i < this.getRequiredSkills().size(); i++) {
                if (this.getRequiredSkill(i).getLevel() > highest) {
                    highest = this.getRequiredSkill(i).getLevel();
                    perk = this.getRequiredSkill(i).getPerk();
                }
            }
        }

        return perk;
    }

    public boolean canResearch(IsoGameCharacter chr) {
        return this.canResearch(chr, true);
    }

    public boolean canResearch(IsoGameCharacter chr, boolean blacklistKnown) {
        if (chr == null) {
            return false;
        } else if (chr.isRecipeActuallyKnown(this) && blacklistKnown) {
            return false;
        } else if (!this.canBeResearched()) {
            return false;
        } else if (this.canAlwaysBeResearched()) {
            return true;
        } else {
            int researchSkillLevel = this.getResearchSkillLevel(chr);
            if (researchSkillLevel < 1) {
                return true;
            } else if (!this.researchAll.isEmpty()) {
                boolean canDo = true;

                for (int i = 0; i < this.researchAll.size(); i++) {
                    if (chr.getPerkLevel(this.researchAll.get(i)) < researchSkillLevel) {
                        canDo = false;
                    }
                }

                return canDo;
            } else {
                if (!this.researchAny.isEmpty()) {
                    for (int ix = 0; ix < this.researchAny.size(); ix++) {
                        if (chr.getPerkLevel(this.researchAny.get(ix)) >= researchSkillLevel) {
                            return true;
                        }
                    }
                }

                if (this.getRequiredSkills() != null && !this.getRequiredSkills().isEmpty()) {
                    for (int ixx = 0; ixx < this.getRequiredSkills().size(); ixx++) {
                        if (chr.getPerkLevel(this.getRequiredSkill(ixx).getPerk()) >= researchSkillLevel) {
                            return true;
                        }
                    }
                }

                if (this.researchAny.isEmpty() && (this.getRequiredSkills() == null || this.getRequiredSkills().isEmpty())) {
                    if (this.getAutoLearnAnySkillCount() > 0) {
                        for (int ixxx = 0; ixxx < this.getAutoLearnAnySkillCount(); ixxx++) {
                            if (chr.getPerkLevel(this.getAutoLearnAnySkill(ixxx).getPerk()) >= researchSkillLevel) {
                                return true;
                            }
                        }
                    }

                    if (this.getAutoLearnAllSkillCount() > 0) {
                        boolean canDo = true;

                        for (int ixxxx = 0; ixxxx < this.getAutoLearnAllSkillCount(); ixxxx++) {
                            if (chr.getPerkLevel(this.getAutoLearnAnySkill(ixxxx).getPerk()) < researchSkillLevel) {
                                canDo = false;
                            }
                        }

                        return canDo;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    public boolean isResearchAll() {
        return !this.researchAll.isEmpty();
    }

    public String generateDebugText() {
        return this.generateDebugText(null);
    }

    public String generateDebugText(IsoGameCharacter chr) {
        new StringBuilder();
        int researchSkillLvel = this.getResearchSkillLevel();
        if (chr != null) {
            researchSkillLvel = this.getResearchSkillLevel(chr);
        }

        if (researchSkillLvel != 0 && !this.canAlwaysBeResearched()) {
            if (this.isResearchAll()) {
                StringBuilder var6 = new StringBuilder(", requires having " + researchSkillLvel + " in all of ");

                for (int i = 0; i < this.researchAll.size(); i++) {
                    var6.append(this.researchAll.get(i).getName());
                    if (i < this.researchAny.size()) {
                        var6.append(", ");
                    }
                }

                return var6.toString();
            } else {
                StringBuilder text = new StringBuilder(", requires having " + researchSkillLvel + " in any of ");
                ArrayList<String> skills = new ArrayList<>();
                if (!this.researchAny.isEmpty()) {
                    for (int ix = 0; ix < this.researchAny.size(); ix++) {
                        skills.add(this.researchAny.get(ix).getName());
                    }
                }

                if (this.getRequiredSkills() != null && !this.getRequiredSkills().isEmpty()) {
                    for (int ix = 0; ix < this.getRequiredSkills().size(); ix++) {
                        skills.add(this.getRequiredSkill(ix).getPerk().getName());
                    }
                }

                if (!skills.isEmpty()) {
                    for (int ix = 0; ix < skills.size(); ix++) {
                        text.append(skills.get(ix));
                        if (ix < skills.size() - 1) {
                            text.append(", ");
                        }
                    }

                    return text.toString();
                } else {
                    if (this.getAutoLearnAnySkills() != null && this.getAutoLearnAnySkillCount() > 0) {
                        for (int ixx = 0; ixx < this.getAutoLearnAnySkills().size(); ixx++) {
                            skills.add(this.getAutoLearnAnySkill(ixx).getPerk().getName());
                        }
                    }

                    if (this.getAutoLearnAnySkills() != null && this.getAutoLearnAllSkillCount() > 0) {
                        for (int ixx = 0; ixx < this.getAutoLearnAllSkills().size(); ixx++) {
                            skills.add(this.getAutoLearnAllSkill(ixx).getPerk().getName());
                        }
                    }

                    if (!skills.isEmpty()) {
                        for (int ixx = 0; ixx < skills.size(); ixx++) {
                            text.append(skills.get(ixx));
                            if (ixx < skills.size() - 1) {
                                text.append(", ");
                            }
                        }
                    }

                    return text.toString();
                }
            }
        } else {
            return "";
        }
    }

    public void addXP(IsoGameCharacter character) {
        this.addXP(character, true);
    }

    public void addXP(IsoGameCharacter character, boolean showXP) {
        if (!LuaManager.GlobalObject.getCore().getOptionShowCraftingXP()) {
            showXP = false;
        }

        if (this.xpAward == null) {
            DebugLog.CraftLogic.println("XP ATTEMPTING: No XP Award for " + this.getName());
        } else {
            DebugLog.CraftLogic.println("XP ATTEMPTING: Trying to Award XP for " + this.getName());
            DebugLog.CraftLogic.println("XP ATTEMPTING: Recipe has xpAward " + this.xpAward);
            if (this.getXPAwardCount() > 0) {
                for (int i = 0; i < this.getXPAwardCount(); i++) {
                    CraftRecipe.xp_Award award = this.getXPAward(i);
                    PerkFactory.Perk perk = award.getPerk();
                    int amount = award.getAmount();
                    DebugLog.CraftLogic.println("XP AWARD BEFORE DIMINISHING RETURN ADJUSTMENT: " + amount);
                    DebugLog.CraftLogic.println("XP ATTEMPTING: Trying to Award XP to " + perk.getName() + " for an XP amount of " + amount);
                    if (GameServer.server) {
                        GameServer.addXp((IsoPlayer)character, perk, amount);
                    } else if (!GameClient.client) {
                        character.getXp().AddXP(perk, amount, true, true, false, showXP);
                    }
                }
            }
        }
    }

    public boolean canBenefitFromRecipeAtHand(IsoGameCharacter chr) {
        if (chr instanceof IsoPlayer isoPlayer) {
            return isoPlayer.tooDarkToRead() ? false : this.couldBenefitFromRecipeAtHand(chr);
        } else {
            return false;
        }
    }

    public boolean couldBenefitFromRecipeAtHand(IsoGameCharacter chr) {
        if (!this.needToBeLearn()) {
            return false;
        } else if (!chr.isRecipeActuallyKnown(this)) {
            return false;
        } else if (!(chr instanceof IsoPlayer)) {
            return false;
        } else if (this.getRequiredSkillCount() > 0) {
            for (int i = 0; i < this.getRequiredSkillCount(); i++) {
                CraftRecipe.RequiredSkill skill = this.getRequiredSkill(i);
                if (chr.getPerkLevel(skill.getPerk()) < skill.getLevel()) {
                    boolean canBenefit = false;
                    if (skill.getLevel() - chr.getPerkLevel(skill.getPerk()) == 1) {
                        canBenefit = true;
                    }

                    if (!canBenefit) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean validateBenefitFromRecipeAtHand(IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        return this.canBenefitFromRecipeAtHand(chr) && this.hasRecipeAtHand(chr, containers);
    }

    public boolean validateBenefitFromRecipeAtHand(HandcraftLogic logic) {
        return this.canBenefitFromRecipeAtHand(logic.getPlayer()) && this.hasRecipeAtHand(logic.getPlayer(), logic.getContainers());
    }

    public boolean hasRecipeAtHand(IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        if (this.getMetaRecipe() != null && chr.getInventory().hasRecipe(this.getMetaRecipe(), chr, true)) {
            return true;
        } else if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                ItemContainer cont = containers.get(i);
                if (cont.hasRecipe(this.getName(), chr, false)) {
                    return true;
                }

                if (this.getMetaRecipe() != null && cont.hasRecipe(this.getMetaRecipe(), chr, false)) {
                    return true;
                }
            }

            return false;
        } else {
            return chr.getInventory().hasRecipe(this.getName(), chr, true);
        }
    }

    public boolean hasRecipeAtHand(HandcraftLogic logic) {
        return this.hasRecipeAtHand(logic.getPlayer(), logic.getContainers());
    }

    public String getFavouriteModDataString(CraftRecipe recipe) {
        return BaseCraftingLogic.getFavouriteModDataString(recipe);
    }

    public boolean isFavourite(IsoGameCharacter character) {
        String favString = this.getFavouriteModDataString(this);
        Object isFavourite = character.getModData().rawget(favString);
        return isFavourite != null && (Boolean)isFavourite;
    }

    public boolean hasPlayerLearned(IsoGameCharacter character) {
        return character == null ? false : !this.needToBeLearn() || character.isRecipeKnown(this);
    }

    public boolean characterHasRequiredSkills(IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        } else {
            if (this.getRequiredSkills() != null && !this.getRequiredSkills().isEmpty()) {
                for (int i = 0; i < this.getRequiredSkills().size(); i++) {
                    if (chr.getPerkLevel(this.getRequiredSkill(i).getPerk()) < this.getRequiredSkill(i).getLevel()) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public boolean requiresSpecificWorkstation() {
        return !this.getTags().isEmpty()
            && !this.hasTag(CraftRecipeTag.IN_HAND_CRAFT)
            && !this.hasTag(CraftRecipeTag.ANY_SURFACE_CRAFT)
            && !this.hasTag(CraftRecipeTag.ENTITY_RECIPE)
            && !this.hasTag(CraftRecipeTag.OUTDOORS);
    }

    public boolean isBuildableRecipe() {
        return this.hasTag(CraftRecipeTag.ENTITY_RECIPE);
    }

    public abstract static class IOScript {
        private final CraftRecipe parentRecipe;

        protected IOScript(CraftRecipe parentRecipe) {
            this.parentRecipe = parentRecipe;
        }

        public CraftRecipe getParentRecipe() {
            return this.parentRecipe;
        }

        public int getRecipeLineIndex() {
            if (this.parentRecipe != null && this.parentRecipe.containsIO(this)) {
                return this.parentRecipe.getIndexForIO(this);
            } else if (Core.debug) {
                throw new IllegalStateException("Script has not been added to parent recipe (yet)");
            } else {
                return -1;
            }
        }
    }

    public static enum LuaCall {
        OnTest,
        OnStart,
        OnUpdate,
        OnCreate,
        OnFailed;
    }

    @UsedFromLua
    public static final class RequiredSkill {
        private final PerkFactory.Perk perk;
        private final int level;

        public RequiredSkill(PerkFactory.Perk perk, int level) {
            this.perk = perk;
            this.level = level;
        }

        public PerkFactory.Perk getPerk() {
            return this.perk;
        }

        public int getLevel() {
            return this.level;
        }
    }

    public static final class xp_Award {
        private final PerkFactory.Perk perk;
        private final int amount;

        public xp_Award(PerkFactory.Perk perk, int amount) {
            this.perk = perk;
            this.amount = amount;
        }

        public PerkFactory.Perk getPerk() {
            return this.perk;
        }

        public int getAmount() {
            return this.amount;
        }
    }
}
