// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import zombie.GameSounds;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.debug.DebugLog;
import zombie.iso.MultiStageBuilding;
import zombie.iso.SpriteModel;
import zombie.scripting.IScriptObjectStore;
import zombie.scripting.ScriptBucket;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.GameEntityTemplate;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.ui.XuiScriptType;
import zombie.vehicles.VehicleEngineRPM;

@UsedFromLua
public final class ScriptModule implements IScriptObjectStore {
    public String name;
    public String value;
    public final ArrayList<String> imports = new ArrayList<>();
    public boolean disabled;
    private final ArrayList<ScriptBucket<?>> scriptBucketList = new ArrayList<>();
    private final HashMap<ScriptType, ScriptBucket<?>> scriptBucketMap = new HashMap<>();
    public final ScriptBucket.Template<VehicleTemplate> vehicleTemplates = this.addBucket(
        new ScriptBucket.Template<VehicleTemplate>(this, ScriptType.VehicleTemplate) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public VehicleTemplate createInstance(ScriptModule module, String name, String token) {
                return new VehicleTemplate(module, name, token);
            }

            protected VehicleTemplate getFromManager(String name) {
                return ScriptManager.instance.getVehicleTemplate(name);
            }

            protected VehicleTemplate getFromModule(String name, ScriptModule module) {
                return module != null ? module.getVehicleTemplate(name) : null;
            }
        }
    );
    public final ScriptBucket.Template<GameEntityTemplate> entityTemplates = this.addBucket(
        new ScriptBucket.Template<GameEntityTemplate>(this, ScriptType.EntityTemplate) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public GameEntityTemplate createInstance(ScriptModule module, String name, String token) {
                return new GameEntityTemplate(module, name, token);
            }

            protected GameEntityTemplate getFromManager(String name) {
                return ScriptManager.instance.getGameEntityTemplate(name);
            }

            protected GameEntityTemplate getFromModule(String name, ScriptModule module) {
                return module != null ? module.getGameEntityTemplate(name) : null;
            }
        }
    );
    public final ScriptBucket<Item> items = this.addBucket(new ScriptBucket<Item>(this, ScriptType.Item) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public Item createInstance(ScriptModule module, String name, String token) {
            return new Item();
        }

        protected Item getFromManager(String name) {
            return ScriptManager.instance.getItem(name);
        }

        protected Item getFromModule(String name, ScriptModule module) {
            return module != null ? module.getItem(name) : null;
        }
    });
    public final ScriptBucket<Recipe> recipes = this.addBucket(new ScriptBucket<Recipe>(this, ScriptType.Recipe) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public Recipe createInstance(ScriptModule module, String name, String token) {
            return new Recipe();
        }

        protected Recipe getFromManager(String name) {
            return ScriptManager.instance.getRecipe(name);
        }

        protected Recipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getRecipe(name) : null;
        }
    });
    public final ScriptBucket<UniqueRecipe> uniqueRecipes = this.addBucket(new ScriptBucket<UniqueRecipe>(this, ScriptType.UniqueRecipe) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public UniqueRecipe createInstance(ScriptModule module, String name, String token) {
            return new UniqueRecipe(name);
        }

        protected UniqueRecipe getFromManager(String name) {
            return ScriptManager.instance.getUniqueRecipe(name);
        }

        protected UniqueRecipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getUniqueRecipe(name) : null;
        }
    });
    public final ScriptBucket<EvolvedRecipe> evolvedRecipes = this.addBucket(new ScriptBucket<EvolvedRecipe>(this, ScriptType.EvolvedRecipe) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public EvolvedRecipe createInstance(ScriptModule module, String name, String token) {
            return new EvolvedRecipe(name);
        }

        protected EvolvedRecipe getFromManager(String name) {
            return ScriptManager.instance.getEvolvedRecipe(name);
        }

        protected EvolvedRecipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getEvolvedRecipe(name) : null;
        }
    });
    public final ScriptBucket<Fixing> fixings = this.addBucket(new ScriptBucket<Fixing>(this, ScriptType.Fixing) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public Fixing createInstance(ScriptModule module, String name, String token) {
            return new Fixing();
        }

        protected Fixing getFromManager(String name) {
            return ScriptManager.instance.getFixing(name);
        }

        protected Fixing getFromModule(String name, ScriptModule module) {
            return module != null ? module.getFixing(name) : null;
        }
    });
    public final ScriptBucket<AnimationsMesh> animationMeshes = this.addBucket(new ScriptBucket<AnimationsMesh>(this, ScriptType.AnimationMesh) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public AnimationsMesh createInstance(ScriptModule module, String name, String token) {
            return new AnimationsMesh();
        }

        protected AnimationsMesh getFromManager(String name) {
            return ScriptManager.instance.getAnimationsMesh(name);
        }

        protected AnimationsMesh getFromModule(String name, ScriptModule module) {
            return module != null ? module.getAnimationsMesh(name) : null;
        }
    });
    public final ScriptBucket<ClockScript> clocks = this.addBucket(new ScriptBucket<ClockScript>(this, ScriptType.Clock) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public ClockScript createInstance(ScriptModule module, String name, String token) {
            return new ClockScript();
        }

        protected ClockScript getFromManager(String name) {
            return ScriptManager.instance.getClockScript(name);
        }

        protected ClockScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getClockScript(name) : null;
        }
    });
    public final ScriptBucket<MannequinScript> mannequins = this.addBucket(new ScriptBucket<MannequinScript>(this, ScriptType.Mannequin) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public MannequinScript createInstance(ScriptModule module, String name, String token) {
            return new MannequinScript();
        }

        protected MannequinScript getFromManager(String name) {
            return ScriptManager.instance.getMannequinScript(name);
        }

        protected MannequinScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getMannequinScript(name) : null;
        }
    });
    public final ScriptBucket<ModelScript> models = this.addBucket(new ScriptBucket<ModelScript>(this, ScriptType.Model) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public ModelScript createInstance(ScriptModule module, String name, String token) {
            return new ModelScript();
        }

        protected ModelScript getFromManager(String name) {
            return ScriptManager.instance.getModelScript(name);
        }

        protected ModelScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getModelScript(name) : null;
        }
    });
    public final ScriptBucket<PhysicsShapeScript> physicsShapes = this.addBucket(new ScriptBucket<PhysicsShapeScript>(this, ScriptType.PhysicsShape) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public PhysicsShapeScript createInstance(ScriptModule module, String name, String token) {
            return new PhysicsShapeScript();
        }

        protected PhysicsShapeScript getFromManager(String name) {
            return ScriptManager.instance.getPhysicsShape(name);
        }

        protected PhysicsShapeScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getPhysicsShape(name) : null;
        }
    });
    public final ScriptBucket<SpriteModel> spriteModels = this.addBucket(new ScriptBucket<SpriteModel>(this, ScriptType.SpriteModel) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public SpriteModel createInstance(ScriptModule module, String name, String token) {
            return new SpriteModel();
        }

        protected SpriteModel getFromManager(String name) {
            return ScriptManager.instance.getSpriteModel(name);
        }

        protected SpriteModel getFromModule(String name, ScriptModule module) {
            return module != null ? module.getSpriteModel(name) : null;
        }
    });
    public final ScriptBucket<GameSoundScript> gameSounds = this.addBucket(new ScriptBucket<GameSoundScript>(this, ScriptType.Sound) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public GameSoundScript createInstance(ScriptModule module, String name, String token) {
            return new GameSoundScript();
        }

        protected GameSoundScript getFromManager(String name) {
            return ScriptManager.instance.getGameSound(name);
        }

        protected GameSoundScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getGameSound(name) : null;
        }

        protected void onScriptLoad(ScriptLoadMode loadMode, GameSoundScript script) {
            if (loadMode == ScriptLoadMode.Reload) {
                GameSounds.OnReloadSound(script);
            }
        }
    });
    public final ScriptBucket<SoundTimelineScript> soundTimelines = this.addBucket(new ScriptBucket<SoundTimelineScript>(this, ScriptType.SoundTimeline) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public SoundTimelineScript createInstance(ScriptModule module, String name, String token) {
            return new SoundTimelineScript();
        }

        protected SoundTimelineScript getFromManager(String name) {
            return ScriptManager.instance.getSoundTimeline(name);
        }

        protected SoundTimelineScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getSoundTimeline(name) : null;
        }
    });
    public final ScriptBucket<VehicleScript> vehicles = this.addBucket(new ScriptBucket<VehicleScript>(this, ScriptType.Vehicle) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public VehicleScript createInstance(ScriptModule module, String name, String token) {
            return new VehicleScript();
        }

        protected VehicleScript getFromManager(String name) {
            return ScriptManager.instance.getVehicle(name);
        }

        protected VehicleScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getVehicle(name) : null;
        }

        protected void onScriptLoad(ScriptLoadMode loadMode, VehicleScript script) {
            script.Loaded();
        }
    });
    public final ScriptBucket<RuntimeAnimationScript> animations = this.addBucket(
        new ScriptBucket<RuntimeAnimationScript>(this, ScriptType.RuntimeAnimation, new TreeMap<>(String.CASE_INSENSITIVE_ORDER)) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public RuntimeAnimationScript createInstance(ScriptModule module, String name, String token) {
                return new RuntimeAnimationScript();
            }

            protected RuntimeAnimationScript getFromManager(String name) {
                return ScriptManager.instance.getRuntimeAnimationScript(name);
            }

            protected RuntimeAnimationScript getFromModule(String name, ScriptModule module) {
                return module != null ? module.getAnimation(name) : null;
            }
        }
    );
    public final ScriptBucket<VehicleEngineRPM> vehicleEngineRpms = this.addBucket(new ScriptBucket<VehicleEngineRPM>(this, ScriptType.VehicleEngineRPM) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public VehicleEngineRPM createInstance(ScriptModule module, String name, String token) {
            return new VehicleEngineRPM();
        }

        protected VehicleEngineRPM getFromManager(String name) {
            return ScriptManager.instance.getVehicleEngineRPM(name);
        }

        protected VehicleEngineRPM getFromModule(String name, ScriptModule module) {
            return module != null ? module.getVehicleEngineRPM(name) : null;
        }
    });
    public final ScriptBucket<ItemConfig> itemConfigs = this.addBucket(new ScriptBucket<ItemConfig>(this, ScriptType.ItemConfig) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public ItemConfig createInstance(ScriptModule module, String name, String token) {
            return new ItemConfig();
        }

        protected ItemConfig getFromManager(String name) {
            return ScriptManager.instance.getItemConfig(name);
        }

        protected ItemConfig getFromModule(String name, ScriptModule module) {
            return module != null ? module.getItemConfig(name) : null;
        }
    });
    public final ScriptBucket<GameEntityScript> entities = this.addBucket(new ScriptBucket<GameEntityScript>(this, ScriptType.Entity) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public GameEntityScript createInstance(ScriptModule module, String name, String token) {
            return new GameEntityScript();
        }

        protected GameEntityScript getFromManager(String name) {
            return ScriptManager.instance.getGameEntityScript(name);
        }

        protected GameEntityScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getGameEntityScript(name) : null;
        }
    });
    public final ScriptBucket<XuiConfigScript> xuiConfigScripts = this.addBucket(new ScriptBucket<XuiConfigScript>(this, ScriptType.XuiConfig) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public XuiConfigScript createInstance(ScriptModule module, String name, String token) {
            return new XuiConfigScript();
        }

        protected XuiConfigScript getFromManager(String name) {
            return ScriptManager.instance.getXuiConfigScript(name);
        }

        protected XuiConfigScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiConfigScript(name) : null;
        }
    });
    public final ScriptBucket<XuiLayoutScript> xuiLayouts = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, ScriptType.XuiLayout) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public XuiLayoutScript createInstance(ScriptModule module, String name, String token) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.Layout);
        }

        protected XuiLayoutScript getFromManager(String name) {
            return ScriptManager.instance.getXuiLayout(name);
        }

        protected XuiLayoutScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiLayout(name) : null;
        }
    });
    public final ScriptBucket<XuiLayoutScript> xuiStyles = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, ScriptType.XuiStyle) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public XuiLayoutScript createInstance(ScriptModule module, String name, String token) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.Style);
        }

        protected XuiLayoutScript getFromManager(String name) {
            return ScriptManager.instance.getXuiStyle(name);
        }

        protected XuiLayoutScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiStyle(name) : null;
        }
    });
    public final ScriptBucket<XuiLayoutScript> xuiDefaultStyles = this.addBucket(new ScriptBucket<XuiLayoutScript>(this, ScriptType.XuiDefaultStyle) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public XuiLayoutScript createInstance(ScriptModule module, String name, String token) {
            return new XuiLayoutScript(this.getScriptType(), XuiScriptType.DefaultStyle);
        }

        protected XuiLayoutScript getFromManager(String name) {
            return ScriptManager.instance.getXuiDefaultStyle(name);
        }

        protected XuiLayoutScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiDefaultStyle(name) : null;
        }
    });
    public final ScriptBucket<XuiColorsScript> xuiGlobalColors = this.addBucket(new ScriptBucket<XuiColorsScript>(this, ScriptType.XuiColor) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public XuiColorsScript createInstance(ScriptModule module, String name, String token) {
            return new XuiColorsScript();
        }

        protected XuiColorsScript getFromManager(String name) {
            return ScriptManager.instance.getXuiColor(name);
        }

        protected XuiColorsScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiGlobalColors(name) : null;
        }
    });
    public final ScriptBucket<XuiSkinScript> xuiSkinScripts = this.addBucket(new ScriptBucket<XuiSkinScript>(this, ScriptType.XuiSkin) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public XuiSkinScript createInstance(ScriptModule module, String name, String token) {
            return new XuiSkinScript();
        }

        protected XuiSkinScript getFromManager(String name) {
            return ScriptManager.instance.getXuiSkinScript(name);
        }

        protected XuiSkinScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getXuiSkinScript(name) : null;
        }
    });
    public final ScriptBucket<ItemFilterScript> itemFilters = this.addBucket(new ScriptBucket<ItemFilterScript>(this, ScriptType.ItemFilter) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public ItemFilterScript createInstance(ScriptModule module, String name, String token) {
            return new ItemFilterScript();
        }

        protected ItemFilterScript getFromManager(String name) {
            return ScriptManager.instance.getItemFilter(name);
        }

        protected ItemFilterScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getItemFilter(name) : null;
        }
    });
    public final ScriptBucket<FluidFilterScript> fluidFilters = this.addBucket(new ScriptBucket<FluidFilterScript>(this, ScriptType.FluidFilter) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public FluidFilterScript createInstance(ScriptModule module, String name, String token) {
            return new FluidFilterScript();
        }

        protected FluidFilterScript getFromManager(String name) {
            return ScriptManager.instance.getFluidFilter(name);
        }

        protected FluidFilterScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getFluidFilter(name) : null;
        }
    });
    public final ScriptBucket<CraftRecipe> craftRecipes = this.addBucket(new ScriptBucket<CraftRecipe>(this, ScriptType.CraftRecipe) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public CraftRecipe createInstance(ScriptModule module, String name, String token) {
            return new CraftRecipe();
        }

        protected CraftRecipe getFromManager(String name) {
            return ScriptManager.instance.getCraftRecipe(name);
        }

        protected CraftRecipe getFromModule(String name, ScriptModule module) {
            return module != null ? module.getCraftRecipe(name) : null;
        }
    });
    public final ScriptBucket<StringListScript> stringLists = this.addBucket(new ScriptBucket<StringListScript>(this, ScriptType.StringList) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public StringListScript createInstance(ScriptModule module, String name, String token) {
            return new StringListScript();
        }

        protected StringListScript getFromManager(String name) {
            return ScriptManager.instance.getStringList(name);
        }

        protected StringListScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getStringList(name) : null;
        }
    });
    public final ScriptBucket<EnergyDefinitionScript> energyDefinitionScripts = this.addBucket(
        new ScriptBucket<EnergyDefinitionScript>(this, ScriptType.EnergyDefinition) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public EnergyDefinitionScript createInstance(ScriptModule module, String name, String token) {
                return new EnergyDefinitionScript();
            }

            protected EnergyDefinitionScript getFromManager(String name) {
                return ScriptManager.instance.getEnergyDefinitionScript(name);
            }

            protected EnergyDefinitionScript getFromModule(String name, ScriptModule module) {
                return module != null ? module.getEnergyDefinitionScript(name) : null;
            }
        }
    );
    public final ScriptBucket<FluidDefinitionScript> fluidDefinitionScripts = this.addBucket(
        new ScriptBucket<FluidDefinitionScript>(this, ScriptType.FluidDefinition) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public FluidDefinitionScript createInstance(ScriptModule module, String name, String token) {
                return new FluidDefinitionScript();
            }

            protected FluidDefinitionScript getFromManager(String name) {
                return ScriptManager.instance.getFluidDefinitionScript(name);
            }

            protected FluidDefinitionScript getFromModule(String name, ScriptModule module) {
                return module != null ? module.getFluidDefinitionScript(name) : null;
            }
        }
    );
    public final ScriptBucket<TimedActionScript> timedActionScripts = this.addBucket(new ScriptBucket<TimedActionScript>(this, ScriptType.TimedAction) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public TimedActionScript createInstance(ScriptModule module, String name, String token) {
            return new TimedActionScript();
        }

        protected TimedActionScript getFromManager(String name) {
            return ScriptManager.instance.getTimedActionScript(name);
        }

        protected TimedActionScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getTimedActionScript(name) : null;
        }
    });
    public final ScriptBucket<RagdollScript> ragdollScripts = this.addBucket(new ScriptBucket<RagdollScript>(this, ScriptType.Ragdoll) {
        {
            Objects.requireNonNull(ScriptModule.this);
        }

        public RagdollScript createInstance(ScriptModule module, String name, String token) {
            return new RagdollScript();
        }

        protected RagdollScript getFromManager(String name) {
            return ScriptManager.instance.getRagdollScript(name);
        }

        protected RagdollScript getFromModule(String name, ScriptModule module) {
            return module != null ? module.getRagdollScript(name) : null;
        }
    });
    public final ScriptBucket<CharacterTraitDefinitionScript> characterTraitScripts = this.addBucket(
        new ScriptBucket<CharacterTraitDefinitionScript>(this, ScriptType.CharacterTraitDefinition) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public CharacterTraitDefinitionScript createInstance(ScriptModule module, String name, String token) {
                return new CharacterTraitDefinitionScript();
            }

            protected CharacterTraitDefinitionScript getFromManager(String name) {
                return ScriptManager.instance.getCharacterTraitScript(name);
            }

            protected CharacterTraitDefinitionScript getFromModule(String name, ScriptModule module) {
                return module != null ? module.getCharacterTraitScript(name) : null;
            }
        }
    );
    public final ScriptBucket<CharacterProfessionDefinitionScript> characterProfessionScripts = this.addBucket(
        new ScriptBucket<CharacterProfessionDefinitionScript>(this, ScriptType.CharacterProfessionDefinition) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public CharacterProfessionDefinitionScript createInstance(ScriptModule module, String name, String token) {
                return new CharacterProfessionDefinitionScript();
            }

            protected CharacterProfessionDefinitionScript getFromManager(String name) {
                return ScriptManager.instance.getCharacterProfessionScript(name);
            }

            protected CharacterProfessionDefinitionScript getFromModule(String name, ScriptModule module) {
                return module != null ? module.getCharacterProfessionScript(name) : null;
            }
        }
    );
    public final ScriptBucket<PhysicsHitReactionScript> physicsHitReactionScripts = this.addBucket(
        new ScriptBucket<PhysicsHitReactionScript>(this, ScriptType.PhysicsHitReaction) {
            {
                Objects.requireNonNull(ScriptModule.this);
            }

            public PhysicsHitReactionScript createInstance(ScriptModule module, String name, String token) {
                return new PhysicsHitReactionScript();
            }

            protected PhysicsHitReactionScript getFromManager(String name) {
                return ScriptManager.instance.getPhysicsHitReactionScript(name);
            }

            protected PhysicsHitReactionScript getFromModule(String name, ScriptModule module) {
                return module != null ? module.getPhysicsHitReactionScript(name) : null;
            }
        }
    );

    private <T extends BaseScriptObject> ScriptBucket<T> addBucket(ScriptBucket<T> bucket) {
        if (this.scriptBucketMap.containsKey(bucket.getScriptType())) {
            throw new RuntimeException("ScriptType bucket already added.");
        } else {
            this.scriptBucketMap.put(bucket.getScriptType(), bucket);
            this.scriptBucketList.add(bucket);
            return bucket;
        }
    }

    private <T extends BaseScriptObject> ScriptBucket.Template<T> addBucket(ScriptBucket.Template<T> bucket) {
        if (this.scriptBucketMap.containsKey(bucket.getScriptType())) {
            throw new RuntimeException("ScriptType bucket already added.");
        } else {
            this.scriptBucketMap.put(bucket.getScriptType(), bucket);
            this.scriptBucketList.add(bucket);
            return bucket;
        }
    }

    public VehicleTemplate getVehicleTemplate(String name) {
        return this.vehicleTemplates.get(name);
    }

    public GameEntityTemplate getGameEntityTemplate(String name) {
        return this.entityTemplates.get(name);
    }

    @Override
    public Item getItem(String name) {
        return this.items.get(name);
    }

    @Override
    public Recipe getRecipe(String name) {
        return this.recipes.get(name);
    }

    public UniqueRecipe getUniqueRecipe(String name) {
        return this.uniqueRecipes.get(name);
    }

    public EvolvedRecipe getEvolvedRecipe(String name) {
        return this.evolvedRecipes.get(name);
    }

    public Fixing getFixing(String name) {
        return this.fixings.get(name);
    }

    public AnimationsMesh getAnimationsMesh(String name) {
        return this.animationMeshes.get(name);
    }

    public ClockScript getClockScript(String name) {
        return this.clocks.get(name);
    }

    public MannequinScript getMannequinScript(String name) {
        return this.mannequins.get(name);
    }

    public ModelScript getModelScript(String name) {
        return this.models.get(name);
    }

    public PhysicsShapeScript getPhysicsShape(String name) {
        return this.physicsShapes.get(name);
    }

    public SpriteModel getSpriteModel(String name) {
        return this.spriteModels.get(name);
    }

    public GameSoundScript getGameSound(String name) {
        return this.gameSounds.get(name);
    }

    public SoundTimelineScript getSoundTimeline(String name) {
        return this.soundTimelines.get(name);
    }

    public VehicleScript getVehicle(String name) {
        return this.vehicles.get(name);
    }

    public RuntimeAnimationScript getAnimation(String name) {
        return this.animations.get(name);
    }

    public VehicleEngineRPM getVehicleEngineRPM(String name) {
        return this.vehicleEngineRpms.get(name);
    }

    public ItemConfig getItemConfig(String name) {
        return this.itemConfigs.get(name);
    }

    public GameEntityScript getGameEntityScript(String name) {
        return this.entities.get(name);
    }

    public XuiConfigScript getXuiConfigScript(String name) {
        return this.xuiConfigScripts.get(name);
    }

    public XuiLayoutScript getXuiLayout(String name) {
        return this.xuiLayouts.get(name);
    }

    public XuiLayoutScript getXuiStyle(String name) {
        return this.xuiStyles.get(name);
    }

    public XuiLayoutScript getXuiDefaultStyle(String name) {
        return this.xuiDefaultStyles.get(name);
    }

    public XuiColorsScript getXuiGlobalColors(String name) {
        return this.xuiGlobalColors.get(name);
    }

    public XuiSkinScript getXuiSkinScript(String name) {
        return this.xuiSkinScripts.get(name);
    }

    public ItemFilterScript getItemFilter(String name) {
        return this.itemFilters.get(name);
    }

    public FluidFilterScript getFluidFilter(String name) {
        return this.fluidFilters.get(name);
    }

    public CraftRecipe getCraftRecipe(String name) {
        return this.craftRecipes.get(name);
    }

    public StringListScript getStringList(String name) {
        return this.stringLists.get(name);
    }

    public EnergyDefinitionScript getEnergyDefinitionScript(String name) {
        return this.energyDefinitionScripts.get(name);
    }

    public FluidDefinitionScript getFluidDefinitionScript(String name) {
        return this.fluidDefinitionScripts.get(name);
    }

    public TimedActionScript getTimedActionScript(String name) {
        return this.timedActionScripts.get(name);
    }

    public RagdollScript getRagdollScript(String name) {
        return this.ragdollScripts.get(name);
    }

    public CharacterTraitDefinitionScript getCharacterTraitScript(String name) {
        return this.characterTraitScripts.get(name);
    }

    public CharacterProfessionDefinitionScript getCharacterProfessionScript(String name) {
        return this.characterProfessionScripts.get(name);
    }

    public PhysicsHitReactionScript getPhysicsHitReactionScript(String name) {
        return this.physicsHitReactionScripts.get(name);
    }

    public ScriptModule() {
        this.xuiLayouts.setVerbose(false);
    }

    public void Load(ScriptLoadMode loadMode, String name, String strArray) {
        this.name = name;
        this.value = strArray.trim();
        ScriptManager.instance.currentLoadingModule = this;
        this.ParseScriptPP(loadMode, this.value);
        this.value = "";
    }

    public void ParseScriptPP(ScriptLoadMode loadMode, String totalFile) {
        ArrayList<String> Tokens = ScriptParser.parseTokens(totalFile);

        for (int n = 0; n < Tokens.size(); n++) {
            String token = Tokens.get(n);
            this.CreateFromTokenPP(loadMode, token);
        }
    }

    private String GetTokenType(String token) {
        int p = token.indexOf(123);
        if (p == -1) {
            return null;
        } else {
            String header = token.substring(0, p).trim();
            int p1 = header.indexOf(32);
            int p2 = header.indexOf(9);
            if (p1 != -1 && p2 != -1) {
                return header.substring(0, PZMath.min(p1, p2));
            } else if (p1 != -1) {
                return header.substring(0, p1);
            } else {
                return p2 != -1 ? header.substring(0, p2) : header;
            }
        }
    }

    private void CreateFromTokenPP(ScriptLoadMode loadMode, String token) {
        token = token.trim();
        String type = this.GetTokenType(token);
        if (type != null) {
            if ("imports".equals(type)) {
                String[] waypoint = token.split("[{}]");
                String[] coords = waypoint[1].split(",");

                for (int n = 0; n < coords.length; n++) {
                    if (!coords[n].trim().isEmpty()) {
                        String module = coords[n].trim();
                        if (module.equals(this.getName())) {
                            DebugLog.Script.error("ERROR: module \"" + this.getName() + "\" imports itself");
                        } else {
                            this.imports.add(module);
                        }
                    }
                }
            } else if ("multistagebuild".equals(type)) {
                String[] waypoint = token.split("[{}]");
                String name = waypoint[0];
                name = name.replace("multistagebuild", "");
                name = name.trim();
                String[] coords = waypoint[1].split(",");
                MultiStageBuilding.Stage newStage = new MultiStageBuilding().new Stage();
                newStage.Load(name, coords);
                MultiStageBuilding.addStage(newStage);
            } else {
                boolean found = false;

                for (ScriptBucket<?> scriptBucket : this.scriptBucketList) {
                    if (scriptBucket.CreateFromTokenPP(loadMode, type, token)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    DebugLog.Script.warn("unknown script object \"%s\" in '%s'", type, ScriptManager.instance.currentFileName);
                }
            }
        }
    }

    public boolean CheckExitPoints() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    public void Reset() {
        this.imports.clear();

        for (ScriptBucket<?> scriptBucket : this.scriptBucketList) {
            scriptBucket.reset();
        }
    }
}
