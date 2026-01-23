// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import zombie.GameSounds;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.core.Core;
import zombie.core.IndieFileLoader;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.energy.Energy;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.ItemTags;
import zombie.inventory.RecipeManager;
import zombie.iso.IsoWorld;
import zombie.iso.MultiStageBuilding;
import zombie.iso.SpriteModel;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.CharacterProfessionDefinitionScript;
import zombie.scripting.objects.CharacterTraitDefinitionScript;
import zombie.scripting.objects.ClockScript;
import zombie.scripting.objects.EnergyDefinitionScript;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Fixing;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.scripting.objects.GameSoundScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemFilterScript;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.RagdollScript;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.scripting.objects.SoundTimelineScript;
import zombie.scripting.objects.StringListScript;
import zombie.scripting.objects.TimedActionScript;
import zombie.scripting.objects.UniqueRecipe;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.VehicleTemplate;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;
import zombie.scripting.ui.XuiManager;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.VehicleEngineRPM;
import zombie.world.WorldDictionary;

@UsedFromLua
public final class ScriptManager implements IScriptObjectStore {
    public static final ScriptManager instance = new ScriptManager();
    private static final EnumSet<ScriptType> debugTypes = EnumSet.noneOf(ScriptType.class);
    public String currentFileName;
    private final ArrayList<String> loadFileNames = new ArrayList<>();
    public final HashMap<String, ScriptModule> moduleMap = new HashMap<>();
    public final ArrayList<ScriptModule> moduleList = new ArrayList<>();
    public ScriptModule currentLoadingModule;
    private final HashMap<String, String> moduleAliases = new HashMap<>();
    private final StringBuilder buf = new StringBuilder();
    private final HashMap<String, ScriptModule> cachedModules = new HashMap<>();
    private final HashMap<ItemTag, ArrayList<Item>> tagToItemMap = new HashMap<>();
    private final HashMap<String, ArrayList<Item>> typeToItemMap = new HashMap<>();
    private final HashMap<String, String> clothingToItemMap = new HashMap<>();
    private final ArrayList<String> visualDamagesList = new ArrayList<>();
    private final ArrayList<ScriptBucketCollection<?>> bucketCollectionList = new ArrayList<>();
    private final HashMap<ScriptType, ScriptBucketCollection<?>> bucketCollectionMap = new HashMap<>();
    private boolean hasLoadErrors;
    private final ScriptBucketCollection<VehicleTemplate> vehicleTemplates = this.addBucketCollection(
        new ScriptBucketCollection<VehicleTemplate>(this, ScriptType.VehicleTemplate) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<VehicleTemplate> getBucketFromModule(ScriptModule module) {
                return module.vehicleTemplates;
            }
        }
    );
    private final ScriptBucketCollection<GameEntityTemplate> entityTemplates = this.addBucketCollection(
        new ScriptBucketCollection<GameEntityTemplate>(this, ScriptType.EntityTemplate) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<GameEntityTemplate> getBucketFromModule(ScriptModule module) {
                return module.entityTemplates;
            }
        }
    );
    private final ScriptBucketCollection<Item> items = this.addBucketCollection(new ScriptBucketCollection<Item>(this, ScriptType.Item) {
        {
            Objects.requireNonNull(ScriptManager.this);
        }

        @Override
        public ScriptBucket<Item> getBucketFromModule(ScriptModule module) {
            return module.items;
        }

        @Override
        public void LoadScripts(ScriptLoadMode loadMode) {
            super.LoadScripts(loadMode);
            ItemTags.Init(ScriptManager.this.getAllItems());
        }
    });
    private final ScriptBucketCollection<Recipe> recipes = this.addBucketCollection(new ScriptBucketCollection<Recipe>(this, ScriptType.Recipe) {
        {
            Objects.requireNonNull(ScriptManager.this);
        }

        @Override
        public ScriptBucket<Recipe> getBucketFromModule(ScriptModule module) {
            return module.recipes;
        }

        @Override
        public void OnLoadedAfterLua() throws Exception {
            super.OnLoadedAfterLua();
            RecipeManager.LoadedAfterLua();
        }
    });
    private final ScriptBucketCollection<UniqueRecipe> uniqueRecipes = this.addBucketCollection(
        new ScriptBucketCollection<UniqueRecipe>(this, ScriptType.UniqueRecipe) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<UniqueRecipe> getBucketFromModule(ScriptModule module) {
                return module.uniqueRecipes;
            }
        }
    );
    private final Stack<UniqueRecipe> uniqueRecipeTempStack = new Stack<>();
    private final ScriptBucketCollection<EvolvedRecipe> evolvedRecipes = this.addBucketCollection(
        new ScriptBucketCollection<EvolvedRecipe>(this, ScriptType.EvolvedRecipe) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<EvolvedRecipe> getBucketFromModule(ScriptModule module) {
                return module.evolvedRecipes;
            }
        }
    );
    private final Stack<EvolvedRecipe> evolvedRecipeTempStack = new Stack<>();
    private final ScriptBucketCollection<Fixing> fixings = this.addBucketCollection(new ScriptBucketCollection<Fixing>(this, ScriptType.Fixing) {
        {
            Objects.requireNonNull(ScriptManager.this);
        }

        @Override
        public ScriptBucket<Fixing> getBucketFromModule(ScriptModule module) {
            return module.fixings;
        }
    });
    private final ScriptBucketCollection<AnimationsMesh> animationMeshes = this.addBucketCollection(
        new ScriptBucketCollection<AnimationsMesh>(this, ScriptType.AnimationMesh) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<AnimationsMesh> getBucketFromModule(ScriptModule module) {
                return module.animationMeshes;
            }
        }
    );
    private final ScriptBucketCollection<ClockScript> clocks = this.addBucketCollection(new ScriptBucketCollection<ClockScript>(this, ScriptType.Clock) {
        {
            Objects.requireNonNull(ScriptManager.this);
        }

        @Override
        public ScriptBucket<ClockScript> getBucketFromModule(ScriptModule module) {
            return module.clocks;
        }
    });
    private final ScriptBucketCollection<MannequinScript> mannequins = this.addBucketCollection(
        new ScriptBucketCollection<MannequinScript>(this, ScriptType.Mannequin) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<MannequinScript> getBucketFromModule(ScriptModule module) {
                return module.mannequins;
            }

            @Override
            public void onSortAllScripts(ArrayList<MannequinScript> scripts) {
                scripts.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
            }
        }
    );
    private final ScriptBucketCollection<ModelScript> models = this.addBucketCollection(new ScriptBucketCollection<ModelScript>(this, ScriptType.Model) {
        {
            Objects.requireNonNull(ScriptManager.this);
        }

        @Override
        public ScriptBucket<ModelScript> getBucketFromModule(ScriptModule module) {
            return module.models;
        }
    });
    private final ScriptBucketCollection<PhysicsShapeScript> physicsShapes = this.addBucketCollection(
        new ScriptBucketCollection<PhysicsShapeScript>(this, ScriptType.PhysicsShape) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<PhysicsShapeScript> getBucketFromModule(ScriptModule module) {
                return module.physicsShapes;
            }
        }
    );
    private final ScriptBucketCollection<GameSoundScript> gameSounds = this.addBucketCollection(
        new ScriptBucketCollection<GameSoundScript>(this, ScriptType.Sound) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<GameSoundScript> getBucketFromModule(ScriptModule module) {
                return module.gameSounds;
            }
        }
    );
    private final ScriptBucketCollection<SoundTimelineScript> soundTimelines = this.addBucketCollection(
        new ScriptBucketCollection<SoundTimelineScript>(this, ScriptType.SoundTimeline) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<SoundTimelineScript> getBucketFromModule(ScriptModule module) {
                return module.soundTimelines;
            }
        }
    );
    private final ScriptBucketCollection<SpriteModel> spriteModels = this.addBucketCollection(
        new ScriptBucketCollection<SpriteModel>(this, ScriptType.SpriteModel) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<SpriteModel> getBucketFromModule(ScriptModule module) {
                return module.spriteModels;
            }
        }
    );
    private final ScriptBucketCollection<VehicleScript> vehicles = this.addBucketCollection(
        new ScriptBucketCollection<VehicleScript>(this, ScriptType.Vehicle) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<VehicleScript> getBucketFromModule(ScriptModule module) {
                return module.vehicles;
            }
        }
    );
    private final ScriptBucketCollection<RuntimeAnimationScript> animations = this.addBucketCollection(
        new ScriptBucketCollection<RuntimeAnimationScript>(this, ScriptType.RuntimeAnimation) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<RuntimeAnimationScript> getBucketFromModule(ScriptModule module) {
                return module.animations;
            }
        }
    );
    private final ScriptBucketCollection<VehicleEngineRPM> vehicleEngineRpms = this.addBucketCollection(
        new ScriptBucketCollection<VehicleEngineRPM>(this, ScriptType.VehicleEngineRPM) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<VehicleEngineRPM> getBucketFromModule(ScriptModule module) {
                return module.vehicleEngineRpms;
            }
        }
    );
    private final ScriptBucketCollection<ItemConfig> itemConfigs = this.addBucketCollection(
        new ScriptBucketCollection<ItemConfig>(this, ScriptType.ItemConfig) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<ItemConfig> getBucketFromModule(ScriptModule module) {
                return module.itemConfigs;
            }
        }
    );
    private final ScriptBucketCollection<GameEntityScript> entities = this.addBucketCollection(
        new ScriptBucketCollection<GameEntityScript>(this, ScriptType.Entity) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<GameEntityScript> getBucketFromModule(ScriptModule module) {
                return module.entities;
            }

            @Override
            public void OnPostTileDefinitions() throws Exception {
                super.OnPostTileDefinitions();
                SpriteConfigManager.InitScriptsPostTileDef();
            }
        }
    );
    private final ScriptBucketCollection<XuiConfigScript> xuiConfigScripts = this.addBucketCollection(
        new ScriptBucketCollection<XuiConfigScript>(this, ScriptType.XuiConfig) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<XuiConfigScript> getBucketFromModule(ScriptModule module) {
                return module.xuiConfigScripts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                XuiManager.ParseScripts();
            }
        }
    );
    private final ScriptBucketCollection<XuiLayoutScript> xuiLayouts = this.addBucketCollection(
        new ScriptBucketCollection<XuiLayoutScript>(this, ScriptType.XuiLayout) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule module) {
                return module.xuiLayouts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                XuiManager.ParseScripts();
            }
        }
    );
    private final ScriptBucketCollection<XuiLayoutScript> xuiStyles = this.addBucketCollection(
        new ScriptBucketCollection<XuiLayoutScript>(this, ScriptType.XuiStyle) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule module) {
                return module.xuiStyles;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                XuiManager.ParseScripts();
            }
        }
    );
    private final ScriptBucketCollection<XuiLayoutScript> xuiDefaultStyles = this.addBucketCollection(
        new ScriptBucketCollection<XuiLayoutScript>(this, ScriptType.XuiDefaultStyle) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<XuiLayoutScript> getBucketFromModule(ScriptModule module) {
                return module.xuiDefaultStyles;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                XuiManager.ParseScripts();
            }
        }
    );
    private final ScriptBucketCollection<XuiColorsScript> xuiGlobalColors = this.addBucketCollection(
        new ScriptBucketCollection<XuiColorsScript>(this, ScriptType.XuiColor) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<XuiColorsScript> getBucketFromModule(ScriptModule module) {
                return module.xuiGlobalColors;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                XuiManager.ParseScripts();
            }
        }
    );
    private final ScriptBucketCollection<XuiSkinScript> xuiSkinScripts = this.addBucketCollection(
        new ScriptBucketCollection<XuiSkinScript>(this, ScriptType.XuiSkin) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<XuiSkinScript> getBucketFromModule(ScriptModule module) {
                return module.xuiSkinScripts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                XuiManager.ParseScripts();
            }
        }
    );
    private final ScriptBucketCollection<ItemFilterScript> itemFilters = this.addBucketCollection(
        new ScriptBucketCollection<ItemFilterScript>(this, ScriptType.ItemFilter) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<ItemFilterScript> getBucketFromModule(ScriptModule module) {
                return module.itemFilters;
            }

            @Override
            public void OnPostWorldDictionaryInit() throws Exception {
                super.OnPostWorldDictionaryInit();
            }
        }
    );
    private final ScriptBucketCollection<FluidFilterScript> fluidFilters = this.addBucketCollection(
        new ScriptBucketCollection<FluidFilterScript>(this, ScriptType.FluidFilter) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<FluidFilterScript> getBucketFromModule(ScriptModule module) {
                return module.fluidFilters;
            }

            @Override
            public void OnPostWorldDictionaryInit() throws Exception {
                super.OnPostWorldDictionaryInit();
            }
        }
    );
    private final ScriptBucketCollection<CraftRecipe> craftRecipes = this.addBucketCollection(
        new ScriptBucketCollection<CraftRecipe>(this, ScriptType.CraftRecipe) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<CraftRecipe> getBucketFromModule(ScriptModule module) {
                return module.craftRecipes;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                CraftRecipeManager.Init();
            }

            @Override
            public void OnLoadedAfterLua() throws Exception {
                super.OnLoadedAfterLua();
            }
        }
    );
    private final ScriptBucketCollection<StringListScript> stringLists = this.addBucketCollection(
        new ScriptBucketCollection<StringListScript>(this, ScriptType.StringList) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<StringListScript> getBucketFromModule(ScriptModule module) {
                return module.stringLists;
            }
        }
    );
    private final ScriptBucketCollection<EnergyDefinitionScript> energyDefinitionScripts = this.addBucketCollection(
        new ScriptBucketCollection<EnergyDefinitionScript>(this, ScriptType.EnergyDefinition) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<EnergyDefinitionScript> getBucketFromModule(ScriptModule module) {
                return module.energyDefinitionScripts;
            }

            @Override
            public void PreReloadScripts() throws Exception {
                Energy.PreReloadScripts();
                super.PreReloadScripts();
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                Energy.Init(loadMode);
            }
        }
    );
    private final ScriptBucketCollection<FluidDefinitionScript> fluidDefinitionScripts = this.addBucketCollection(
        new ScriptBucketCollection<FluidDefinitionScript>(this, ScriptType.FluidDefinition) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<FluidDefinitionScript> getBucketFromModule(ScriptModule module) {
                return module.fluidDefinitionScripts;
            }

            @Override
            public void PreReloadScripts() throws Exception {
                Fluid.PreReloadScripts();
                super.PreReloadScripts();
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                Fluid.Init(loadMode);
            }
        }
    );
    private final ScriptBucketCollection<TimedActionScript> timedActionScripts = this.addBucketCollection(
        new ScriptBucketCollection<TimedActionScript>(this, ScriptType.TimedAction) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<TimedActionScript> getBucketFromModule(ScriptModule module) {
                return module.timedActionScripts;
            }
        }
    );
    private final ScriptBucketCollection<RagdollScript> ragdollScripts = this.addBucketCollection(
        new ScriptBucketCollection<RagdollScript>(this, ScriptType.Ragdoll) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<RagdollScript> getBucketFromModule(ScriptModule module) {
                return module.ragdollScripts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
                RagdollScript.toBullet(false);
            }
        }
    );
    private final ScriptBucketCollection<PhysicsHitReactionScript> physicsHitReactionScripts = this.addBucketCollection(
        new ScriptBucketCollection<PhysicsHitReactionScript>(this, ScriptType.PhysicsHitReaction) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<PhysicsHitReactionScript> getBucketFromModule(ScriptModule module) {
                return module.physicsHitReactionScripts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
            }
        }
    );
    private final ScriptBucketCollection<CharacterTraitDefinitionScript> characterTraitScripts = this.addBucketCollection(
        new ScriptBucketCollection<CharacterTraitDefinitionScript>(this, ScriptType.CharacterTraitDefinition) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<CharacterTraitDefinitionScript> getBucketFromModule(ScriptModule module) {
                return module.characterTraitScripts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
            }
        }
    );
    private final ScriptBucketCollection<CharacterProfessionDefinitionScript> characterProfessionScripts = this.addBucketCollection(
        new ScriptBucketCollection<CharacterProfessionDefinitionScript>(this, ScriptType.CharacterProfessionDefinition) {
            {
                Objects.requireNonNull(ScriptManager.this);
            }

            @Override
            public ScriptBucket<CharacterProfessionDefinitionScript> getBucketFromModule(ScriptModule module) {
                return module.characterProfessionScripts;
            }

            @Override
            public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
                super.PostLoadScripts(loadMode);
            }
        }
    );
    public static final String Base = "Base";
    public static final String Base_Module = "Base.";
    private String checksum = "";
    private HashMap<String, String> tempFileToModMap;
    private static String currentLoadFileMod;
    private static String currentLoadFileAbsPath;
    private static String currentLoadFileName;
    public static final String VanillaID = "pz-vanilla";

    public static void EnableDebug(ScriptType type, boolean enable) {
        if (enable) {
            debugTypes.add(type);
        } else {
            debugTypes.remove(type);
        }
    }

    public static boolean isDebugEnabled(ScriptType type) {
        return debugTypes.contains(type);
    }

    public static void println(ScriptType type, String msg) {
        if (debugTypes.contains(type)) {
            DebugLog.Script.println("[" + type.toString() + "] " + msg);
        }
    }

    public static void println(BaseScriptObject scriptObject, String msg) {
        println(scriptObject.getScriptObjectType(), msg);
    }

    private <T extends BaseScriptObject> ScriptBucketCollection<T> addBucketCollection(ScriptBucketCollection<T> collection) {
        if (this.bucketCollectionMap.containsKey(collection.getScriptType())) {
            throw new RuntimeException("ScriptType collection already added.");
        } else {
            this.bucketCollectionMap.put(collection.getScriptType(), collection);
            this.bucketCollectionList.add(collection);
            return collection;
        }
    }

    public ArrayList<?> getScriptsForType(ScriptType type) {
        if (this.bucketCollectionMap.containsKey(type)) {
            return this.bucketCollectionMap.get(type).getAllScripts();
        } else {
            DebugLog.General.warn("Type has no bucket collection: " + type);
            return new ArrayList();
        }
    }

    public VehicleTemplate getVehicleTemplate(String name) {
        return this.vehicleTemplates.getScript(name);
    }

    public ArrayList<VehicleTemplate> getAllVehicleTemplates() {
        return this.vehicleTemplates.getAllScripts();
    }

    public GameEntityTemplate getGameEntityTemplate(String name) {
        return this.entityTemplates.getScript(name);
    }

    public ArrayList<GameEntityTemplate> getAllGameEntityTemplates() {
        return this.entityTemplates.getAllScripts();
    }

    @Override
    public Item getItem(String name) {
        return this.items.getScript(name);
    }

    public ArrayList<Item> getAllItems() {
        return this.items.getAllScripts();
    }

    @Override
    public Recipe getRecipe(String name) {
        return this.recipes.getScript(name);
    }

    public ArrayList<Recipe> getAllRecipes() {
        return this.recipes.getAllScripts();
    }

    public UniqueRecipe getUniqueRecipe(String name) {
        return this.uniqueRecipes.getScript(name);
    }

    public Stack<UniqueRecipe> getAllUniqueRecipes() {
        this.uniqueRecipeTempStack.clear();
        this.uniqueRecipeTempStack.addAll(this.uniqueRecipes.getAllScripts());
        return this.uniqueRecipeTempStack;
    }

    public EvolvedRecipe getEvolvedRecipe(String name) {
        return this.evolvedRecipes.getScript(name);
    }

    public ArrayList<EvolvedRecipe> getAllEvolvedRecipesList() {
        return this.evolvedRecipes.getAllScripts();
    }

    public Stack<EvolvedRecipe> getAllEvolvedRecipes() {
        this.evolvedRecipeTempStack.clear();
        this.evolvedRecipeTempStack.addAll(this.evolvedRecipes.getAllScripts());
        return this.evolvedRecipeTempStack;
    }

    public Fixing getFixing(String name) {
        return this.fixings.getScript(name);
    }

    public ArrayList<Fixing> getAllFixing(ArrayList<Fixing> result) {
        result.addAll(this.fixings.getAllScripts());
        return result;
    }

    public AnimationsMesh getAnimationsMesh(String name) {
        return this.animationMeshes.getScript(name);
    }

    public ArrayList<AnimationsMesh> getAllAnimationsMeshes() {
        return this.animationMeshes.getAllScripts();
    }

    public ClockScript getClockScript(String name) {
        return this.clocks.getScript(name);
    }

    public ArrayList<ClockScript> getAllClockScripts() {
        return this.clocks.getAllScripts();
    }

    public MannequinScript getMannequinScript(String name) {
        return this.mannequins.getScript(name);
    }

    public ArrayList<MannequinScript> getAllMannequinScripts() {
        return this.mannequins.getAllScripts();
    }

    public ModelScript getModelScript(String name) {
        return this.models.getScript(name);
    }

    public ArrayList<ModelScript> getAllModelScripts() {
        return this.models.getAllScripts();
    }

    public void addModelScript(ModelScript modelScript) {
        ScriptModule module = modelScript.getModule();
        module.models.scriptList.add(modelScript);
        module.models.scriptMap.put(modelScript.getScriptObjectName(), modelScript);
        if (modelScript.getScriptObjectName().contains(".")) {
            module.models.dotInName.add(modelScript.getScriptObjectName());
        }

        this.models.getAllScripts().clear();
        this.models.getFullTypeToScriptMap().put(modelScript.getScriptObjectFullType(), modelScript);
    }

    public PhysicsShapeScript getPhysicsShape(String name) {
        return this.physicsShapes.getScript(name);
    }

    public ArrayList<PhysicsShapeScript> getAllPhysicsShapes() {
        return this.physicsShapes.getAllScripts();
    }

    public GameSoundScript getGameSound(String name) {
        return this.gameSounds.getScript(name);
    }

    public ArrayList<GameSoundScript> getAllGameSounds() {
        return new ArrayList<>(this.gameSounds.getAllScripts());
    }

    public SoundTimelineScript getSoundTimeline(String name) {
        return this.soundTimelines.getScript(name);
    }

    public ArrayList<SoundTimelineScript> getAllSoundTimelines() {
        return this.soundTimelines.getAllScripts();
    }

    public SpriteModel getSpriteModel(String name) {
        return this.spriteModels.getScript(name);
    }

    public ArrayList<SpriteModel> getAllSpriteModels() {
        return this.spriteModels.getAllScripts();
    }

    public void addSpriteModel(SpriteModel spriteModel) {
        ScriptModule module = spriteModel.getModule();
        module.spriteModels.getScriptList().add(spriteModel);
        module.spriteModels.getScriptMap().put(spriteModel.getScriptObjectName(), spriteModel);
        if (spriteModel.getScriptObjectName().contains(".")) {
            module.spriteModels.dotInName.add(spriteModel.getScriptObjectName());
        }

        this.spriteModels.getFullTypeToScriptMap().put(spriteModel.getScriptObjectFullType(), spriteModel);
    }

    public VehicleScript getVehicle(String name) {
        return this.vehicles.getScript(name);
    }

    public ArrayList<VehicleScript> getAllVehicleScripts() {
        return this.vehicles.getAllScripts();
    }

    public VehicleScript getRandomVehicleScript() {
        List<VehicleScript> vehiclesList = this.vehicles
            .getAllScripts()
            .stream()
            .filter(vehicleScript -> !vehicleScript.getName().toLowerCase().contains("burnt") && !vehicleScript.getName().toLowerCase().contains("smashed"))
            .toList();
        return vehiclesList.get(Rand.Next(vehiclesList.size()));
    }

    public RuntimeAnimationScript getRuntimeAnimationScript(String name) {
        return this.animations.getScript(name);
    }

    public ArrayList<RuntimeAnimationScript> getAllRuntimeAnimationScripts() {
        return new ArrayList<>(this.animations.getAllScripts());
    }

    public VehicleEngineRPM getVehicleEngineRPM(String name) {
        return this.vehicleEngineRpms.getScript(name);
    }

    public ArrayList<VehicleEngineRPM> getAllVehicleEngineRPMs() {
        return this.vehicleEngineRpms.getAllScripts();
    }

    public ItemConfig getItemConfig(String name) {
        return this.itemConfigs.getScript(name);
    }

    public ArrayList<ItemConfig> getAllItemConfigs() {
        return this.itemConfigs.getAllScripts();
    }

    public GameEntityScript getGameEntityScript(String name) {
        return this.entities.getScript(name);
    }

    public ArrayList<GameEntityScript> getAllGameEntities() {
        return this.entities.getAllScripts();
    }

    public ArrayList<CraftRecipe> getAllBuildableRecipes() {
        ArrayList<CraftRecipe> allBuildableRecipes = new ArrayList<>();
        ArrayList<GameEntityScript> entityScripts = instance.getAllGameEntities();

        for (int i = 0; i < entityScripts.size(); i++) {
            ArrayList<ComponentScript> allComponents = entityScripts.get(i).getComponentScripts();

            for (int j = 0; j < allComponents.size(); j++) {
                if (allComponents.get(j).type == ComponentType.CraftRecipe) {
                    CraftRecipeComponentScript componentScript = (CraftRecipeComponentScript)allComponents.get(j);
                    CraftRecipe craftRecipe = componentScript != null ? componentScript.getCraftRecipe() : null;
                    if (craftRecipe != null && craftRecipe.isBuildableRecipe()) {
                        allBuildableRecipes.add(craftRecipe);
                        break;
                    }
                }
            }
        }

        return allBuildableRecipes;
    }

    public CraftRecipe getBuildableRecipe(String recipe) {
        new ArrayList();
        ArrayList<GameEntityScript> entityScripts = instance.getAllGameEntities();

        for (int i = 0; i < entityScripts.size(); i++) {
            ArrayList<ComponentScript> allComponents = entityScripts.get(i).getComponentScripts();

            for (int j = 0; j < allComponents.size(); j++) {
                if (allComponents.get(j).type == ComponentType.CraftRecipe) {
                    CraftRecipeComponentScript componentScript = (CraftRecipeComponentScript)allComponents.get(j);
                    CraftRecipe craftRecipe = componentScript != null ? componentScript.getCraftRecipe() : null;
                    if (craftRecipe != null && craftRecipe.isBuildableRecipe() && Objects.equals(craftRecipe.getName(), recipe)) {
                        return craftRecipe;
                    }
                }
            }
        }

        return null;
    }

    public XuiConfigScript getXuiConfigScript(String name) {
        return this.xuiConfigScripts.getScript(name);
    }

    public ArrayList<XuiConfigScript> getAllXuiConfigScripts() {
        return this.xuiConfigScripts.getAllScripts();
    }

    public XuiLayoutScript getXuiLayout(String name) {
        return this.xuiLayouts.getScript(name);
    }

    public ArrayList<XuiLayoutScript> getAllXuiLayouts() {
        return this.xuiLayouts.getAllScripts();
    }

    public XuiLayoutScript getXuiStyle(String name) {
        return this.xuiStyles.getScript(name);
    }

    public ArrayList<XuiLayoutScript> getAllXuiStyles() {
        return this.xuiStyles.getAllScripts();
    }

    public XuiLayoutScript getXuiDefaultStyle(String name) {
        return this.xuiDefaultStyles.getScript(name);
    }

    public ArrayList<XuiLayoutScript> getAllXuiDefaultStyles() {
        return this.xuiDefaultStyles.getAllScripts();
    }

    public XuiColorsScript getXuiColor(String name) {
        return this.xuiGlobalColors.getScript(name);
    }

    public ArrayList<XuiColorsScript> getAllXuiColors() {
        return this.xuiGlobalColors.getAllScripts();
    }

    public XuiSkinScript getXuiSkinScript(String name) {
        return this.xuiSkinScripts.getScript(name);
    }

    public ArrayList<XuiSkinScript> getAllXuiSkinScripts() {
        return this.xuiSkinScripts.getAllScripts();
    }

    public ItemFilterScript getItemFilter(String name) {
        return this.itemFilters.getScript(name);
    }

    public ArrayList<ItemFilterScript> getAllItemFilters() {
        return this.itemFilters.getAllScripts();
    }

    public FluidFilterScript getFluidFilter(String name) {
        return this.fluidFilters.getScript(name);
    }

    public ArrayList<FluidFilterScript> getAllFluidFilters() {
        return this.fluidFilters.getAllScripts();
    }

    public CraftRecipe getCraftRecipe(String name) {
        CraftRecipe recipe = this.craftRecipes.getScript(name);
        if (recipe == null) {
            GameEntityScript entityScript = this.entities.getScript(name);
            if (entityScript != null) {
                CraftRecipeComponentScript craftRecipeComponent = entityScript.getComponentScriptFor(ComponentType.CraftRecipe);
                if (craftRecipeComponent != null) {
                    recipe = craftRecipeComponent.getCraftRecipe();
                }
            }
        }

        return recipe;
    }

    public ArrayList<CraftRecipe> getAllCraftRecipes() {
        return this.craftRecipes.getAllScripts();
    }

    public void VerifyAllCraftRecipesAreLearnable() {
        if (Core.debug) {
            DebugLog.log("Verifying that all Craft Recipes are Learnable");
            boolean good = true;
            ArrayList<CraftRecipe> failedVerificationRecipes = new ArrayList<>();
            ArrayList<String> allMagazineRecipes = new ArrayList<>();

            for (Item item : instance.getAllItems()) {
                List<String> taughtRecipes = item.getLearnedRecipes();
                if (taughtRecipes != null) {
                    for (String taughtRecipe : taughtRecipes) {
                        if (!allMagazineRecipes.contains(taughtRecipe)) {
                            allMagazineRecipes.add(taughtRecipe);
                        }
                    }
                }
            }

            ArrayList<CraftRecipe> allRecipes = this.craftRecipes.getAllScripts();

            for (CraftRecipe craftRecipe : allRecipes) {
                if (craftRecipe.needToBeLearn()
                    && craftRecipe.getAutoLearnAllSkillCount() <= 0
                    && craftRecipe.getAutoLearnAnySkillCount() <= 0
                    && !allMagazineRecipes.contains(craftRecipe.getName())) {
                    failedVerificationRecipes.add(craftRecipe);
                }
            }

            for (CraftRecipe craftRecipex : failedVerificationRecipes) {
                good = false;
                boolean learnable = false;
                StringBuilder warnString = new StringBuilder("CraftRecipe " + craftRecipex.getName() + " only learnable by:");
                if (craftRecipex.canBeResearched()) {
                    warnString.append(" Research.");
                    learnable = true;
                }

                List<CharacterProfessionDefinition> characterProfessionDefinitions = CharacterProfessionDefinition.getProfessions();
                ArrayList<String> recipeProfessions = new ArrayList<>();

                for (CharacterProfessionDefinition characterProfessionDefinition : characterProfessionDefinitions) {
                    if (characterProfessionDefinition.isGrantedRecipe(craftRecipex.getName())) {
                        recipeProfessions.add(characterProfessionDefinition.getType().toString());
                        learnable = true;
                    }
                }

                if (!recipeProfessions.isEmpty()) {
                    warnString.append(" Profession(");

                    for (String recipeProfession : recipeProfessions) {
                        warnString.append(recipeProfession + ";");
                    }

                    warnString.append(")");
                }

                if (learnable) {
                    DebugLog.log(String.valueOf(warnString));
                } else {
                    DebugLog.log("CraftRecipe " + craftRecipex.getName() + " is not learnable");
                }
            }

            for (String learnableRecipe : allMagazineRecipes) {
                boolean found = false;

                for (CraftRecipe craftRecipex : allRecipes) {
                    if (craftRecipex.getName().equalsIgnoreCase(learnableRecipe)) {
                        found = true;
                        break;
                    }

                    if (craftRecipex.getMetaRecipe() != null && craftRecipex.getMetaRecipe().equalsIgnoreCase(learnableRecipe)) {
                        found = true;
                        break;
                    }
                }

                if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesSeasonRecipeExist"), learnableRecipe)) {
                    found = true;
                }

                if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesMiscRecipeExist"), learnableRecipe)) {
                    found = true;
                } else if (learnableRecipe.equalsIgnoreCase("Basic Mechanics")
                    || learnableRecipe.equalsIgnoreCase("Intermediate Mechanics")
                    || learnableRecipe.equalsIgnoreCase("Advanced Mechanics")
                    || learnableRecipe.equalsIgnoreCase("Herbalist")
                    || learnableRecipe.equalsIgnoreCase("Generator")) {
                    found = true;
                }

                for (SpriteConfigManager.ObjectInfo objectInfo : SpriteConfigManager.GetObjectInfoList()) {
                    if (objectInfo.getScript() != null && objectInfo.getScript().getParent() != null) {
                        GameEntityScript entityScript = (GameEntityScript)objectInfo.getScript().getParent();
                        if (entityScript != null && entityScript.getName().equalsIgnoreCase(learnableRecipe)) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    good = false;
                    DebugLog.log("Learnable CraftRecipe " + learnableRecipe + " does not exist");
                }
            }

            if (good) {
                DebugLog.log("Verified that all Craft Recipes are Learnable without any issues.");
            } else {
                DebugLog.log("Craft Recipe Learning possible issues detected; this may be a false positive on account of intentional design however.");
            }
        }
    }

    public void checkAutoLearn(IsoGameCharacter chr) {
        ArrayList<CraftRecipe> recipes = this.craftRecipes.getAllScripts();

        for (int i = 0; i < recipes.size(); i++) {
            CraftRecipe recipe = recipes.get(i);
            if (!chr.isRecipeActuallyKnown(recipe) && recipe.getAutoLearnAnySkillCount() > 0) {
                recipe.checkAutoLearnAnySkills(chr);
            }

            if (!chr.isRecipeActuallyKnown(recipe) && recipe.getAutoLearnAllSkillCount() > 0) {
                recipe.checkAutoLearnAllSkills(chr);
            }
        }
    }

    public void checkMetaRecipes(IsoGameCharacter chr) {
        ArrayList<CraftRecipe> recipes = this.craftRecipes.getAllScripts();

        for (int i = 0; i < recipes.size(); i++) {
            CraftRecipe recipe = recipes.get(i);
            if (!chr.isRecipeActuallyKnown(recipe) && recipe.getMetaRecipe() != null) {
                recipe.checkMetaRecipe(chr);
            }
        }
    }

    public void checkMetaRecipe(IsoGameCharacter chr, String checkRecipe) {
        ArrayList<CraftRecipe> recipes = this.craftRecipes.getAllScripts();

        for (int i = 0; i < recipes.size(); i++) {
            CraftRecipe recipe = recipes.get(i);
            if (!chr.isRecipeActuallyKnown(recipe) && recipe.getMetaRecipe() != null && Objects.equals(recipe.getMetaRecipe(), checkRecipe)) {
                recipe.checkMetaRecipe(chr, checkRecipe);
            }
        }
    }

    public StringListScript getStringList(String name) {
        return this.stringLists.getScript(name);
    }

    public ArrayList<StringListScript> getAllStringLists() {
        return this.stringLists.getAllScripts();
    }

    public EnergyDefinitionScript getEnergyDefinitionScript(String name) {
        return this.energyDefinitionScripts.getScript(name);
    }

    public ArrayList<EnergyDefinitionScript> getAllEnergyDefinitionScripts() {
        return this.energyDefinitionScripts.getAllScripts();
    }

    public FluidDefinitionScript getFluidDefinitionScript(String name) {
        return this.fluidDefinitionScripts.getScript(name);
    }

    public ArrayList<FluidDefinitionScript> getAllFluidDefinitionScripts() {
        return this.fluidDefinitionScripts.getAllScripts();
    }

    public TimedActionScript getTimedActionScript(String name) {
        return this.timedActionScripts.getScript(name);
    }

    public ArrayList<TimedActionScript> getAllTimedActionScripts() {
        return this.timedActionScripts.getAllScripts();
    }

    public RagdollScript getRagdollScript(String name) {
        return this.ragdollScripts.getScript(name);
    }

    public PhysicsHitReactionScript getPhysicsHitReactionScript(String name) {
        return this.physicsHitReactionScripts.getScript(name);
    }

    public CharacterTraitDefinitionScript getCharacterTraitScript(String name) {
        return this.characterTraitScripts.getScript(name);
    }

    public CharacterProfessionDefinitionScript getCharacterProfessionScript(String name) {
        return this.characterProfessionScripts.getScript(name);
    }

    public ScriptManager() {
        Collections.sort(this.bucketCollectionList, Comparator.comparing(ScriptBucketCollection::isTemplate, Comparator.reverseOrder()));
    }

    public void update() {
    }

    public void LoadFile(ScriptLoadMode loadMode, String filename, boolean bLoadJar) throws FileNotFoundException {
        if (DebugLog.isEnabled(DebugType.Script)) {
            DebugLog.Script.debugln(filename + (bLoadJar ? " bLoadJar" : ""));
        }

        if (!GameServer.server) {
            Thread.yield();
            Core.getInstance().DoFrameReady();
        }

        if (filename.contains(".tmx")) {
            IsoWorld.mapPath = filename.substring(0, filename.lastIndexOf("/"));
            IsoWorld.mapUseJar = bLoadJar;
            DebugLog.Script.debugln("  file is a .tmx (map) file. Set mapPath to " + IsoWorld.mapPath + (IsoWorld.mapUseJar ? " mapUseJar" : ""));
        } else if (!filename.endsWith(".txt")) {
            DebugLog.Script.warn(" file is not a .txt (script) file: " + filename);
        } else {
            InputStreamReader isr = IndieFileLoader.getStreamReader(filename, !bLoadJar);
            BufferedReader br = new BufferedReader(isr);
            this.buf.setLength(0);
            String inputLine = null;
            String totalFile = "";

            label108: {
                try {
                    while (true) {
                        if ((inputLine = br.readLine()) == null) {
                            break label108;
                        }

                        this.buf.append(inputLine);
                        this.buf.append('\n');
                    }
                } catch (Exception var18) {
                    DebugLog.Script.error("Exception thrown reading file " + filename + "\n  " + var18);
                } finally {
                    try {
                        br.close();
                        isr.close();
                    } catch (Exception var17) {
                        DebugLog.Script.error("Exception thrown closing file " + filename + "\n  " + var17);
                        var17.printStackTrace(DebugLog.Script);
                    }
                }

                return;
            }

            totalFile = this.buf.toString();
            totalFile = ScriptParser.stripComments(totalFile);
            this.currentFileName = filename;
            this.registerLoadFileName(this.currentFileName);
            this.ParseScript(loadMode, totalFile);
            this.currentFileName = null;
        }
    }

    private void registerLoadFileName(String name) {
        if (!this.loadFileNames.contains(name)) {
            this.loadFileNames.add(name);
        }
    }

    public void ParseScript(ScriptLoadMode loadMode, String totalFile) {
        ArrayList<String> Tokens = ScriptParser.parseTokens(totalFile);

        for (int n = 0; n < Tokens.size(); n++) {
            String token = Tokens.get(n);
            this.CreateFromToken(loadMode, token);
        }
    }

    private void CreateFromToken(ScriptLoadMode loadMode, String token) {
        token = token.trim();
        if (token.indexOf("module") == 0) {
            int firstopen = token.indexOf("{");
            int lastClose = token.lastIndexOf("}");
            String[] waypoint = token.split("[{}]");
            String name = waypoint[0];
            name = name.replace("module", "");
            name = name.trim();
            String actual = token.substring(firstopen + 1, lastClose);
            ScriptModule way = this.moduleMap.get(name);
            if (way == null) {
                if (DebugLog.isEnabled(DebugType.Script)) {
                    DebugLog.Script.debugln("Adding new module: " + name);
                }

                way = new ScriptModule();
                this.moduleMap.put(name, way);
                this.moduleList.add(way);

                for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
                    collection.registerModule(way);
                }
            }

            way.Load(loadMode, name, actual);
        }
    }

    public void searchFolders(URI base, File fo, ArrayList<String> loadList) {
        if (fo.isDirectory()) {
            if (fo.getAbsolutePath().contains("tempNotWorking")) {
                return;
            }

            String[] internalNames = fo.list();

            for (int i = 0; i < internalNames.length; i++) {
                this.searchFolders(base, new File(fo.getAbsolutePath() + File.separator + internalNames[i]), loadList);
            }
        } else if (fo.getAbsolutePath().toLowerCase().endsWith(".txt")) {
            String relPath = ZomboidFileSystem.instance.getRelativeFile(base, fo.getAbsolutePath());
            relPath = relPath.toLowerCase(Locale.ENGLISH);
            loadList.add(relPath);
        }
    }

    public static String getItemName(String name) {
        int p = name.indexOf(46);
        return p == -1 ? name : name.substring(p + 1);
    }

    public ScriptModule getModule(String name) {
        return this.getModule(name, true);
    }

    public ScriptModule getModule(String name, boolean defaultToBase) {
        if (name.trim().equals("Base") || name.startsWith("Base.")) {
            return this.moduleMap.get("Base");
        } else if (this.cachedModules.containsKey(name)) {
            return this.cachedModules.get(name);
        } else {
            ScriptModule ret = null;
            if (this.moduleAliases.containsKey(name)) {
                name = this.moduleAliases.get(name);
            }

            if (this.cachedModules.containsKey(name)) {
                return this.cachedModules.get(name);
            } else {
                if (this.moduleMap.containsKey(name)) {
                    if (this.moduleMap.get(name).disabled) {
                        ret = null;
                    } else {
                        ret = this.moduleMap.get(name);
                    }
                }

                if (ret != null) {
                    this.cachedModules.put(name, ret);
                    return ret;
                } else {
                    int idx = name.indexOf(".");
                    if (idx != -1) {
                        ret = this.getModule(name.substring(0, idx));
                    }

                    if (ret != null) {
                        this.cachedModules.put(name, ret);
                        return ret;
                    } else {
                        return defaultToBase ? this.moduleMap.get("Base") : null;
                    }
                }
            }
        }
    }

    public ScriptModule getModuleNoDisableCheck(String name) {
        if (this.moduleAliases.containsKey(name)) {
            name = this.moduleAliases.get(name);
        }

        if (this.moduleMap.containsKey(name)) {
            return this.moduleMap.get(name);
        } else {
            return name.indexOf(".") != -1 ? this.getModule(name.split("\\.")[0]) : null;
        }
    }

    public Item FindItem(String name) {
        return this.FindItem(name, true);
    }

    public Item FindItem(String name, boolean moduleDefaultsToBase) {
        if (name.contains(".") && this.items.hasFullType(name)) {
            return this.items.getFullType(name);
        } else {
            ScriptModule module = this.getModule(name, moduleDefaultsToBase);
            if (module == null) {
                return null;
            } else {
                Item item = module.getItem(getItemName(name));
                if (item == null) {
                    for (int i = 0; i < this.moduleList.size(); i++) {
                        ScriptModule m = this.moduleList.get(i);
                        if (!m.disabled) {
                            item = module.getItem(getItemName(name));
                            if (item != null) {
                                return item;
                            }
                        }
                    }
                }

                return item;
            }
        }
    }

    public boolean isDrainableItemType(String itemType) {
        Item scriptItem = this.FindItem(itemType);
        return scriptItem != null ? scriptItem.isItemType(ItemType.DRAINABLE) : false;
    }

    public void CheckExitPoints() {
        for (int i = 0; i < this.moduleList.size(); i++) {
            ScriptModule m = this.moduleList.get(i);
            if (!m.disabled && m.CheckExitPoints()) {
                return;
            }
        }
    }

    private ArrayList<Item> getAllItemsWithTag(ItemTag itemTag) {
        ArrayList<Item> items = this.tagToItemMap.get(itemTag);
        if (items != null) {
            return items;
        } else {
            items = new ArrayList<>();
            ArrayList<Item> all = this.getAllItems();

            for (int i = 0; i < all.size(); i++) {
                Item item = all.get(i);
                if (item.hasTag(itemTag)) {
                    items.add(item);
                }
            }

            this.tagToItemMap.put(itemTag, items);
            return items;
        }
    }

    public ArrayList<Item> getItemsTag(ItemTag itemTag) {
        return this.getAllItemsWithTag(itemTag);
    }

    public ArrayList<Item> getItemsByType(String type) {
        if (StringUtils.isNullOrWhitespace(type)) {
            throw new IllegalArgumentException("invalid type \"" + type + "\"");
        } else {
            ArrayList<Item> items = this.typeToItemMap.get(type);
            if (items != null) {
                return items;
            } else {
                items = new ArrayList<>();

                for (int i = 0; i < this.moduleList.size(); i++) {
                    ScriptModule m = this.moduleList.get(i);
                    if (!m.disabled) {
                        Item item = this.items.getFullType(StringUtils.moduleDotType(m.name, type));
                        if (item != null) {
                            items.add(item);
                        }
                    }
                }

                this.typeToItemMap.put(type, items);
                return items;
            }
        }
    }

    public void Reset() {
        for (ScriptModule module : this.moduleList) {
            module.Reset();
        }

        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            collection.reset();
        }

        this.moduleMap.clear();
        this.moduleList.clear();
        this.moduleAliases.clear();
        this.cachedModules.clear();
        this.tagToItemMap.clear();
        this.typeToItemMap.clear();
        this.clothingToItemMap.clear();
        this.hasLoadErrors = false;
        CharacterProfessionDefinition.reset();
        CharacterTraitDefinition.reset();
        PhysicsHitReactionScript.Reset();
    }

    public String getChecksum() {
        return this.checksum;
    }

    public static String getCurrentLoadFileMod() {
        return currentLoadFileMod;
    }

    public static String getCurrentLoadFileAbsPath() {
        return currentLoadFileAbsPath;
    }

    public static String getCurrentLoadFileName() {
        return currentLoadFileName;
    }

    public void Load() throws IOException {
        try {
            this.loadFileNames.clear();
            WorldDictionary.StartScriptLoading();
            this.tempFileToModMap = new HashMap<>();
            ArrayList<String> gameFiles = new ArrayList<>();
            this.searchFolders(ZomboidFileSystem.instance.base.lowercaseUri, ZomboidFileSystem.instance.getMediaFile("scripts"), gameFiles);

            for (String file : gameFiles) {
                this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(file), "pz-vanilla");
            }

            ArrayList<String> modFiles = new ArrayList<>();
            ArrayList<String> root = ZomboidFileSystem.instance.getModIDs();

            for (int n = 0; n < root.size(); n++) {
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(root.get(n));
                if (mod != null) {
                    String modDir = mod.getCommonDir();
                    if (modDir != null) {
                        File modDirFile = new File(modDir);
                        URI modDirURI = modDirFile.toURI();
                        URI lowercaseURI = new File(modDirFile.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH)).toURI();
                        int firstFileInThisMod = modFiles.size();
                        File canonicalMedia = ZomboidFileSystem.instance.getCanonicalFile(modDirFile, "media");
                        File canonicalScripts = ZomboidFileSystem.instance.getCanonicalFile(canonicalMedia, "scripts");
                        this.searchFolders(lowercaseURI, canonicalScripts, modFiles);
                        if (root.get(n).equals("pz-vanilla")) {
                            throw new RuntimeException("Warning mod id is named pz-vanilla!");
                        }

                        for (int i = firstFileInThisMod; i < modFiles.size(); i++) {
                            String file = modFiles.get(i);
                            this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(file), root.get(n));
                        }
                    }

                    modDir = mod.getVersionDir();
                    if (modDir != null) {
                        File modDirFile = new File(modDir);
                        URI modDirURI = modDirFile.toURI();
                        URI lowercaseURI = new File(modDirFile.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH)).toURI();
                        int firstFileInThisMod = modFiles.size();
                        File canonicalMedia = ZomboidFileSystem.instance.getCanonicalFile(modDirFile, "media");
                        File canonicalScripts = ZomboidFileSystem.instance.getCanonicalFile(canonicalMedia, "scripts");
                        this.searchFolders(lowercaseURI, canonicalScripts, modFiles);
                        if (root.get(n).equals("pz-vanilla")) {
                            throw new RuntimeException("Warning mod id is named pz-vanilla!");
                        }

                        for (int i = firstFileInThisMod; i < modFiles.size(); i++) {
                            String file = modFiles.get(i);
                            this.tempFileToModMap.put(ZomboidFileSystem.instance.getAbsolutePath(file), root.get(n));
                        }
                    }
                }
            }

            Comparator<String> comp = new Comparator<String>() {
                {
                    Objects.requireNonNull(ScriptManager.this);
                }

                public int compare(String o1, String o2) {
                    String name1 = new File(o1).getName();
                    String name2 = new File(o2).getName();
                    if (name1.startsWith("template_") && !name2.startsWith("template_")) {
                        return -1;
                    } else {
                        return !name1.startsWith("template_") && name2.startsWith("template_") ? 1 : o1.compareTo(o2);
                    }
                }
            };
            Collections.sort(gameFiles, comp);
            Collections.sort(modFiles, comp);
            PZArrayUtil.addAll(gameFiles, modFiles);
            if (GameClient.client || GameServer.server) {
                NetChecksum.checksummer.reset(true);
                NetChecksum.GroupOfFiles.initChecksum();
            }

            MultiStageBuilding.stages.clear();
            HashSet<String> done = new HashSet<>();

            for (String s : gameFiles) {
                if (!done.contains(s)) {
                    done.add(s);
                    String absPath = ZomboidFileSystem.instance.getAbsolutePath(s);
                    currentLoadFileAbsPath = absPath;
                    currentLoadFileName = FileName.getName(absPath);
                    currentLoadFileMod = this.tempFileToModMap.get(absPath);
                    this.LoadFile(ScriptLoadMode.Init, s, false);
                    if (GameClient.client || GameServer.server) {
                        NetChecksum.checksummer.addFile(s, absPath);
                    }
                }
            }

            if (GameClient.client || GameServer.server) {
                this.checksum = NetChecksum.checksummer.checksumToString();
                if (GameServer.server) {
                    DebugLog.General.println("scriptChecksum: " + this.checksum);
                }
            }
        } catch (Exception var15) {
            ExceptionLogger.logException(var15);
        }

        this.buf.setLength(0);
        this.buf.trimToSize();
        this.loadScripts(ScriptLoadMode.Init, EnumSet.allOf(ScriptType.class));
        if (Core.debug && this.hasLoadErrors()) {
            throw new IOException("Script load errors.");
        } else {
            this.debugItems();
            this.resolveItemTypes();
            WorldDictionary.ScriptsLoaded();
            RecipeManager.ScriptsLoaded();
            GameSounds.ScriptsLoaded();
            ModelScript.ScriptsLoaded();
            if (SoundManager.instance != null) {
                SoundManager.instance.debugScriptSounds();
            }

            Translator.debugItemEvolvedRecipeNames();
            Translator.debugItemNames();
            Translator.debugMultiStageBuildNames();
            Translator.debugRecipeNames();
            Translator.debugRecipeGroupNames();
            this.createClothingItemMap();
            this.createZedDmgMap();
        }
    }

    public void ReloadScripts(ScriptType type) {
        this.ReloadScripts(EnumSet.of(type));
    }

    public void ReloadScripts(EnumSet<ScriptType> types) {
        DebugLog.General.debugln("Reloading scripts = " + types);
        this.loadScripts(ScriptLoadMode.Reload, types);
    }

    private void loadScripts(ScriptLoadMode loadMode, EnumSet<ScriptType> toLoadTypes) {
        try {
            XuiManager.setParseOnce(true);
            if (loadMode == ScriptLoadMode.Reload) {
                for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
                    if (toLoadTypes.contains(collection.getScriptType())) {
                        collection.setReloadBuckets(true);
                        collection.PreReloadScripts();
                    }
                }

                for (String loadFile : this.loadFileNames) {
                    String absPath = ZomboidFileSystem.instance.getAbsolutePath(loadFile);
                    currentLoadFileAbsPath = absPath;
                    currentLoadFileName = FileName.getName(absPath);
                    currentLoadFileMod = this.tempFileToModMap.get(absPath);
                    this.LoadFile(ScriptLoadMode.Reload, loadFile, true);
                }

                for (ScriptBucketCollection<?> collectionx : this.bucketCollectionList) {
                    collectionx.setReloadBuckets(false);
                }
            }

            for (ScriptBucketCollection<?> collectionx : this.bucketCollectionList) {
                if (toLoadTypes.contains(collectionx.getScriptType())) {
                    collectionx.LoadScripts(loadMode);
                }
            }

            for (ScriptBucketCollection<?> collectionxx : this.bucketCollectionList) {
                if (toLoadTypes.contains(collectionxx.getScriptType())) {
                    collectionxx.PostLoadScripts(loadMode);
                }
            }

            for (ScriptBucketCollection<?> collectionxxx : this.bucketCollectionList) {
                if (toLoadTypes.contains(collectionxxx.getScriptType())) {
                    collectionxxx.OnScriptsLoaded(loadMode);
                }
            }

            if (loadMode == ScriptLoadMode.Reload) {
                for (ScriptBucketCollection<?> collectionxxxx : this.bucketCollectionList) {
                    if (toLoadTypes.contains(collectionxxxx.getScriptType())) {
                        collectionxxxx.OnLoadedAfterLua();
                        collectionxxxx.OnPostTileDefinitions();
                        collectionxxxx.OnPostWorldDictionaryInit();
                    }
                }
            }
        } catch (Exception var6) {
            ExceptionLogger.logException(var6);
            this.hasLoadErrors = true;
        }

        XuiManager.setParseOnce(false);
    }

    public void LoadedAfterLua() {
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            try {
                collection.OnLoadedAfterLua();
            } catch (Exception var4) {
                var4.printStackTrace();
                this.hasLoadErrors = true;
            }
        }
    }

    public void PostTileDefinitions() {
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            try {
                collection.OnPostTileDefinitions();
            } catch (Exception var4) {
                ExceptionLogger.logException(var4);
                this.hasLoadErrors = true;
            }
        }
    }

    public void PostWorldDictionaryInit() {
        for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
            try {
                collection.OnPostWorldDictionaryInit();
            } catch (Exception var4) {
                ExceptionLogger.logException(var4);
                this.hasLoadErrors = true;
            }
        }
    }

    public boolean hasLoadErrors() {
        return this.hasLoadErrors(false);
    }

    public boolean hasLoadErrors(boolean onlyCritical) {
        if (this.hasLoadErrors) {
            return true;
        } else {
            for (ScriptBucketCollection<?> collection : this.bucketCollectionList) {
                if (collection.hasLoadErrors(onlyCritical)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void resolveGetItemTypes(ArrayList<String> sourceItems, ArrayList<Item> scriptItems) {
        for (int i = sourceItems.size() - 1; i >= 0; i--) {
            String type1 = sourceItems.get(i);
            if (type1.startsWith("[")) {
                sourceItems.remove(i);
                String functionName = type1.substring(1, type1.indexOf("]"));
                Object functionObj = LuaManager.getFunctionObject(functionName);
                if (functionObj != null) {
                    scriptItems.clear();
                    LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, scriptItems);

                    for (int j = 0; j < scriptItems.size(); j++) {
                        Item scriptItem = scriptItems.get(j);
                        sourceItems.add(i + j, scriptItem.getFullName());
                    }
                }
            }
        }
    }

    private void debugItems() {
        for (Item item : instance.getAllItems()) {
            if (item.isItemType(ItemType.DRAINABLE) && item.getReplaceOnUse() != null) {
                DebugLog.Script.warn("%s ReplaceOnUse instead of ReplaceOnDeplete", item.getFullName());
            }

            if (item.isItemType(ItemType.WEAPON) && item.hitSound != null && !item.hitSound.equals(item.hitFloorSound)) {
                boolean modelScript = true;
            }

            if (!StringUtils.isNullOrEmpty(item.worldStaticModel)) {
                if (item.isItemType(ItemType.FOOD) && item.getStaticModel() == null) {
                    boolean var6 = true;
                }

                ModelScript modelScript = this.getModelScript(item.worldStaticModel);
                if (modelScript != null && modelScript.getAttachmentById("world") != null) {
                    boolean var5 = true;
                }
            }
        }
    }

    public ArrayList<Recipe> getAllRecipesFor(String result) {
        ArrayList<Recipe> rec = this.getAllRecipes();
        ArrayList<Recipe> res = new ArrayList<>();

        for (int n = 0; n < rec.size(); n++) {
            String t = rec.get(n).result.type;
            if (t.contains(".")) {
                t = t.substring(t.indexOf(".") + 1);
            }

            if (t.equals(result)) {
                res.add(rec.get(n));
            }
        }

        return res;
    }

    public String getItemTypeForClothingItem(String clothingItem) {
        return this.clothingToItemMap.get(clothingItem);
    }

    public Item getItemForClothingItem(String clothingName) {
        String itemType = this.getItemTypeForClothingItem(clothingName);
        return itemType == null ? null : this.FindItem(itemType);
    }

    private void createZedDmgMap() {
        this.visualDamagesList.clear();
        ScriptModule module = this.getModule("Base");

        for (Item item : module.items.getScriptMap().values()) {
            if (item.isBodyLocation(ItemBodyLocation.ZED_DMG)) {
                this.visualDamagesList.add(item.getName());
            }
        }
    }

    public ArrayList<String> getZedDmgMap() {
        return this.visualDamagesList;
    }

    private void createClothingItemMap() {
        for (Item item : this.getAllItems()) {
            if (!StringUtils.isNullOrWhitespace(item.getClothingItem())) {
                if (DebugLog.isEnabled(DebugType.Script)) {
                    DebugLog.Script.noise("ClothingItem \"%s\" <---> Item \"%s\"", item.getClothingItem(), item.getFullName());
                }

                this.clothingToItemMap.put(item.getClothingItem(), item.getFullName());
            }
        }
    }

    private void resolveItemTypes() {
        for (Item item : this.getAllItems()) {
            item.resolveItemTypes();
        }
    }

    public String resolveItemType(ScriptModule module, String itemType) {
        if (StringUtils.isNullOrWhitespace(itemType)) {
            return null;
        } else if (itemType.contains(".")) {
            return itemType;
        } else {
            Item scriptItem = module.getItem(itemType);
            if (scriptItem != null) {
                return scriptItem.getFullName();
            } else {
                for (int i = 0; i < this.moduleList.size(); i++) {
                    ScriptModule module1 = this.moduleList.get(i);
                    if (!module1.disabled) {
                        scriptItem = module1.getItem(itemType);
                        if (scriptItem != null) {
                            return scriptItem.getFullName();
                        }
                    }
                }

                return "???." + itemType;
            }
        }
    }

    public String resolveModelScript(ScriptModule module, String modelScriptName) {
        if (StringUtils.isNullOrWhitespace(modelScriptName)) {
            return null;
        } else if (modelScriptName.contains(".")) {
            return modelScriptName;
        } else {
            ModelScript modelScript = module.getModelScript(modelScriptName);
            if (modelScript != null) {
                return modelScript.getFullType();
            } else {
                for (int i = 0; i < this.moduleList.size(); i++) {
                    ScriptModule module1 = this.moduleList.get(i);
                    if (module1 != module && !module1.disabled) {
                        modelScript = module1.getModelScript(modelScriptName);
                        if (modelScript != null) {
                            return modelScript.getFullType();
                        }
                    }
                }

                return "???." + modelScriptName;
            }
        }
    }

    /**
     * Attempts to get the specific item of "module.type" without defaulting to module "Base".
     */
    public Item getSpecificItem(String name) {
        if (!name.contains(".")) {
            DebugLog.log("ScriptManager.getSpecificItem requires a full type name, cannot find: " + name);
            if (Core.debug) {
                throw new RuntimeException("ScriptManager.getSpecificItem requires a full type name, cannot find: " + name);
            } else {
                return null;
            }
        } else if (this.items.hasFullType(name)) {
            return this.items.getFullType(name);
        } else {
            int idx = name.indexOf(".");
            String module = name.substring(0, idx);
            String type = name.substring(idx + 1);
            ScriptModule script = this.getModule(module, false);
            return script == null ? null : script.getItem(type);
        }
    }

    public GameEntityScript getSpecificEntity(String name) {
        if (!name.contains(".")) {
            DebugLog.log("ScriptManager.getSpecificEntity requires a full type name, cannot find: " + name);
            if (Core.debug) {
                throw new RuntimeException("ScriptManager.getSpecificEntity requires a full type name, cannot find: " + name);
            } else {
                return null;
            }
        } else if (this.entities.hasFullType(name)) {
            return this.entities.getFullType(name);
        } else {
            int idx = name.indexOf(".");
            String module = name.substring(0, idx);
            String type = name.substring(idx + 1);
            ScriptModule script = this.getModule(module, false);
            return script == null ? null : script.getGameEntityScript(type);
        }
    }
}
