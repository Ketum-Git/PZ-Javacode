// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import fmod.fmod.FMODManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.ThumpState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.ObjectAmbientEmitters;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityFactory;
import zombie.entity.GameEntityManager;
import zombie.entity.GameEntityType;
import zombie.entity.components.combat.Durability;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidInstance;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.gameStates.IngameState;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.fboRenderChunk.FBORenderObjectPicker;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStackedWasherDryer;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWheelieBin;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.ClockHands;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.AddItemToMapPacket;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ClockScript;
import zombie.scripting.objects.ItemTag;
import zombie.spnetwork.SinglePlayerServer;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.ui.ObjectTooltip;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.vehicles.BaseVehicle;
import zombie.world.WorldDictionary;

@UsedFromLua
public class IsoObject extends GameEntity implements Serializable, ILuaIsoObject, Thumpable, IsoRenderable {
    public static final int MAX_WALL_SPLATS = 32;
    public static IsoObject lastRendered;
    public static IsoObject lastRenderedRendered;
    private static final ColorInfo stCol = new ColorInfo();
    public static float rmod;
    public static float gmod;
    public static float bmod;
    private boolean doRender = true;
    private boolean isSceneCulled;
    public static boolean lowLightingQualityHack;
    private static final ColorInfo stCol2 = new ColorInfo();
    private static final ColorInfo colFxMask = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    public static final ColorInfo fireColor = new ColorInfo(0.51F, 0.27F, 0.18F, 1.0F);
    public byte ppfHighlighted;
    public byte ppfHighlightRenderOnce;
    public byte ppfBlink;
    public boolean satChair;
    public int keyId = -1;
    public BaseSoundEmitter emitter;
    public float sheetRopeHealth = 100.0F;
    public boolean sheetRope;
    public boolean neverDoneAlpha = true;
    public boolean alphaForced;
    public ArrayList<IsoSpriteInstance> attachedAnimSprite;
    public ArrayList<IsoWallBloodSplat> wallBloodSplats;
    public ItemContainer container;
    public IsoDirections dir = IsoDirections.N;
    public short damage = 100;
    public float partialThumpDmg;
    public boolean noPicking;
    public float offsetX = (float)(32 * Core.tileScale);
    public float offsetY = (float)(96 * Core.tileScale);
    public boolean outlineOnMouseover;
    public IsoObject rerouteMask;
    public IsoSprite sprite;
    public IsoSprite overlaySprite;
    public ColorInfo overlaySpriteColor;
    public IsoGridSquare square;
    public float[] alpha;
    protected float[] targetAlpha;
    protected ObjectRenderInfo[] renderInfo;
    public IsoObject rerouteCollide;
    public KahluaTable table;
    public String name;
    public float tintr = 1.0F;
    public float tintg = 1.0F;
    public float tintb = 1.0F;
    public String spriteName;
    public float sx;
    public float sy;
    public boolean doNotSync;
    protected ObjectRenderEffects windRenderEffects;
    protected ObjectRenderEffects objectRenderEffects;
    protected IsoObject externalWaterSource;
    protected boolean usesExternalWaterSource;
    protected ArrayList<IsoObject> children;
    private String tile;
    private boolean specialTooltip;
    private ColorInfo[] highlightColor;
    private ArrayList<ItemContainer> secondaryContainers;
    private ColorInfo customColor;
    private float renderYOffset;
    protected byte isOutlineHighlight;
    protected byte isOutlineHlAttached;
    protected byte isOutlineHlBlink;
    protected int[] outlineHighlightCol;
    private float outlineThickness = 1.0F;
    protected boolean movedThumpable;
    protected String spriteModelName;
    protected SpriteModel spriteModel;
    protected IsoSprite spriteModelInit;
    protected boolean animating;
    private IsoLightSource lightSource;
    private IsoSpriteInstance onOverlay;
    public IsoGridSquare renderSquareOverride;
    public IsoGridSquare renderSquareOverride2;
    public float renderDepthAdjust;
    private ClockScript clockScript;
    private IsoSprite clockScriptInit;
    private long hasPowerTick = -1L;
    private boolean hasPower;
    private static final Map<Byte, IsoObject.IsoObjectFactory> byteToObjectMap = new HashMap<>();
    private static final Map<Integer, IsoObject.IsoObjectFactory> hashCodeToObjectMap = new HashMap<>();
    private static final Map<String, IsoObject.IsoObjectFactory> nameToObjectMap = new HashMap<>();
    private boolean removeFromWorldToMeta;
    private long isoEntityNetId = -1L;
    private int lastObjectIndex = -1;
    private static IsoObject.IsoObjectFactory factoryIsoObject;
    private static IsoObject.IsoObjectFactory factoryVehicle;
    private static final Set<IsoFlagType> NORTH_FLAGS = Set.of(
        IsoFlagType.collideN,
        IsoFlagType.windowN,
        IsoFlagType.doorN,
        IsoFlagType.transparentN,
        IsoFlagType.cutN,
        IsoFlagType.tableN,
        IsoFlagType.climbSheetN,
        IsoFlagType.climbSheetTopN,
        IsoFlagType.HoppableN,
        IsoFlagType.attachedN,
        IsoFlagType.WindowN,
        IsoFlagType.TallHoppableN,
        IsoFlagType.DoorWallN,
        IsoFlagType.WallN,
        IsoFlagType.FloorAttachmentN
    );

    public IsoObject(IsoCell cell) {
        this();
    }

    public IsoObject() {
        if (!GameServer.server) {
            this.highlightColor = new ColorInfo[4];
            this.outlineHighlightCol = new int[4];
            this.renderInfo = new ObjectRenderInfo[4];
            this.alpha = new float[4];
            this.targetAlpha = new float[4];

            for (int n = 0; n < 4; n++) {
                this.setAlphaAndTarget(n, 1.0F);
                this.highlightColor[n] = new ColorInfo(0.9F, 1.0F, 0.0F, 1.0F);
                this.outlineHighlightCol[n] = -1;
                this.renderInfo[n] = new ObjectRenderInfo(this);
            }
        }

        this.isSceneCulled = false;
    }

    public IsoObject(IsoCell cell, IsoGridSquare square, IsoSprite spr) {
        this();
        this.sprite = spr;
        this.square = square;
    }

    public IsoObject(IsoCell cell, IsoGridSquare square, String gid) {
        this();
        this.sprite = IsoSpriteManager.instance.getSprite(gid);
        this.square = square;
        this.tile = gid;
    }

    public IsoObject(IsoGridSquare square, String tile, String name) {
        this();
        this.sprite = IsoSpriteManager.instance.getSprite(tile);
        this.square = square;
        this.tile = tile;
        this.spriteName = tile;
        this.name = name;
    }

    public IsoObject(IsoGridSquare square, String tile, String name, boolean bShareTilesWithMap) {
        this();
        if (bShareTilesWithMap) {
            this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
            this.sprite.LoadFramesNoDirPageSimple(tile);
        } else {
            this.sprite = IsoSpriteManager.instance.namedMap.get(tile);
        }

        this.tile = tile;
        this.square = square;
        this.name = name;
    }

    public boolean isFloor() {
        return this.getProperties() != null ? this.getProperties().has(IsoFlagType.solidfloor) : false;
    }

    public IsoObject(IsoGridSquare square, String tile, boolean bShareTilesWithMap) {
        this();
        if (bShareTilesWithMap) {
            this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
            this.sprite.LoadFramesNoDirPageSimple(tile);
        } else {
            this.sprite = IsoSpriteManager.instance.namedMap.get(tile);
        }

        this.tile = tile;
        this.square = square;
    }

    public IsoObject(IsoGridSquare square, String tile) {
        this();
        this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        this.sprite.LoadFramesNoDirPageSimple(tile);
        this.square = square;
    }

    public static IsoObject getNew(IsoGridSquare sq, String spriteName, String name, boolean bShareTilesWithMap) {
        IsoObject obj = CellLoader.isoObjectCache.pop();
        if (obj == null) {
            obj = new IsoObject(sq, spriteName, name, bShareTilesWithMap);
        } else {
            obj.reset();
            obj.tile = spriteName;
        }

        if (bShareTilesWithMap) {
            obj.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
            obj.sprite.LoadFramesNoDirPageSimple(obj.tile);
        } else {
            obj.sprite = IsoSpriteManager.instance.namedMap.get(obj.tile);
        }

        obj.square = sq;
        obj.name = name;
        return obj;
    }

    /**
     * @return the lastRendered
     */
    public static IsoObject getLastRendered() {
        return lastRendered;
    }

    /**
     * 
     * @param aLastRendered the lastRendered to set
     */
    public static void setLastRendered(IsoObject aLastRendered) {
        lastRendered = aLastRendered;
    }

    /**
     * @return the lastRenderedRendered
     */
    public static IsoObject getLastRenderedRendered() {
        return lastRenderedRendered;
    }

    /**
     * 
     * @param aLastRenderedRendered the lastRenderedRendered to set
     */
    public static void setLastRenderedRendered(IsoObject aLastRenderedRendered) {
        lastRenderedRendered = aLastRenderedRendered;
    }

    public static IsoObject getNew() {
        IsoObject obj = CellLoader.isoObjectCache.pop();
        return obj == null ? new IsoObject() : obj;
    }

    private static IsoObject.IsoObjectFactory addIsoObjectFactory(IsoObject.IsoObjectFactory f) {
        if (byteToObjectMap.containsKey(f.classId)) {
            throw new RuntimeException("Class id already exists, " + f.objectName);
        } else {
            byteToObjectMap.put(f.classId, f);
            if (hashCodeToObjectMap.containsKey(f.hashCode)) {
                throw new RuntimeException("Hashcode already exists, " + f.objectName);
            } else {
                hashCodeToObjectMap.put(f.hashCode, f);
                if (nameToObjectMap.containsKey(f.objectName)) {
                    throw new RuntimeException("ObjectName already exists, " + f.objectName);
                } else {
                    nameToObjectMap.put(f.objectName, f);
                    return f;
                }
            }
        }
    }

    public static IsoObject.IsoObjectFactory getFactoryVehicle() {
        return factoryVehicle;
    }

    private static void initFactory() {
        factoryIsoObject = addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)0, "IsoObject") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                IsoObject o = IsoObject.getNew();
                o.sx = 0.0F;
                return o;
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)1, "Player") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoPlayer(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)2, "Survivor") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoSurvivor(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)3, "Zombie") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoZombie(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)4, "Pushable") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoPushableObject(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)5, "WheelieBin") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoWheelieBin(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)6, "WorldInventoryItem") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoWorldInventoryObject(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)7, "Jukebox") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoJukebox(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)8, "Curtain") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoCurtain(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)9, "Radio") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoRadio(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)10, "Television") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoTelevision(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)11, "DeadBody") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoDeadBody(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)12, "Barbecue") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoBarbecue(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)13, "ClothingDryer") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoClothingDryer(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)14, "ClothingWasher") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoClothingWasher(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)15, "Fireplace") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoFireplace(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)16, "Stove") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoStove(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)17, "Door") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoDoor(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)18, "Thumpable") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoThumpable(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)19, "IsoTrap") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoTrap(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)20, "IsoBrokenGlass") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoBrokenGlass(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)21, "IsoCarBatteryCharger") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoCarBatteryCharger(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)22, "IsoGenerator") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoGenerator(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)23, "IsoCompost") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoCompost(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)24, "Mannequin") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoMannequin(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)26, "Window") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoWindow(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)27, "Barricade") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoBarricade(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)28, "Tree") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return IsoTree.getNew();
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)29, "LightSwitch") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoLightSwitch(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)30, "ZombieGiblets") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoZombieGiblets(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)31, "MolotovCocktail") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoMolotovCocktail(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)32, "Fire") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoFire(cell);
            }
        });
        factoryVehicle = addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)33, "Vehicle") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new BaseVehicle(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)34, "CombinationWasherDryer") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoCombinationWasherDryer(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)35, "StackedWasherDryer") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoStackedWasherDryer(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)36, "Animal") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoAnimal(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)37, "FeedingTrough") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoFeedingTrough(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)38, "IsoHutch") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoHutch(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)39, "IsoAnimalTrack") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoAnimalTrack(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)40, "ButcherHook") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoButcherHook(cell);
            }
        });
        addIsoObjectFactory(new IsoObject.IsoObjectFactory((byte)41, "IsoWindowFrame") {
            @Override
            protected IsoObject InstantiateObject(IsoCell cell) {
                return new IsoWindowFrame(cell);
            }
        });
    }

    public static byte factoryGetClassID(String name) {
        IsoObject.IsoObjectFactory f = hashCodeToObjectMap.get(name.hashCode());
        return f != null ? f.classId : factoryIsoObject.classId;
    }

    public static IsoObject factoryFromFileInput(IsoCell cell, byte classID) {
        IsoObject.IsoObjectFactory f = byteToObjectMap.get(classID);
        if (f == null || f.objectName.equals("Vehicle") && GameClient.client) {
            if (f == null && Core.debug) {
                throw new RuntimeException("Cannot get IsoObject from classID: " + classID);
            } else {
                return new IsoObject(cell);
            }
        } else {
            return f.InstantiateObject(cell);
        }
    }

    @Deprecated
    public static IsoObject factoryFromFileInput_OLD(IsoCell cell, int classID) {
        if (classID == "IsoObject".hashCode()) {
            IsoObject o = getNew();
            o.sx = 0.0F;
            return o;
        } else if (classID == "Player".hashCode()) {
            return new IsoPlayer(cell);
        } else if (classID == "Survivor".hashCode()) {
            return new IsoSurvivor(cell);
        } else if (classID == "Zombie".hashCode()) {
            return new IsoZombie(cell);
        } else if (classID == "Pushable".hashCode()) {
            return new IsoPushableObject(cell);
        } else if (classID == "WheelieBin".hashCode()) {
            return new IsoWheelieBin(cell);
        } else if (classID == "WorldInventoryItem".hashCode()) {
            return new IsoWorldInventoryObject(cell);
        } else if (classID == "Jukebox".hashCode()) {
            return new IsoJukebox(cell);
        } else if (classID == "Curtain".hashCode()) {
            return new IsoCurtain(cell);
        } else if (classID == "Radio".hashCode()) {
            return new IsoRadio(cell);
        } else if (classID == "Television".hashCode()) {
            return new IsoTelevision(cell);
        } else if (classID == "DeadBody".hashCode()) {
            return new IsoDeadBody(cell);
        } else if (classID == "Barbecue".hashCode()) {
            return new IsoBarbecue(cell);
        } else if (classID == "ClothingDryer".hashCode()) {
            return new IsoClothingDryer(cell);
        } else if (classID == "ClothingWasher".hashCode()) {
            return new IsoClothingWasher(cell);
        } else if (classID == "Fireplace".hashCode()) {
            return new IsoFireplace(cell);
        } else if (classID == "Stove".hashCode()) {
            return new IsoStove(cell);
        } else if (classID == "Door".hashCode()) {
            return new IsoDoor(cell);
        } else if (classID == "Thumpable".hashCode()) {
            return new IsoThumpable(cell);
        } else if (classID == "IsoTrap".hashCode()) {
            return new IsoTrap(cell);
        } else if (classID == "IsoBrokenGlass".hashCode()) {
            return new IsoBrokenGlass(cell);
        } else if (classID == "IsoCarBatteryCharger".hashCode()) {
            return new IsoCarBatteryCharger(cell);
        } else if (classID == "IsoGenerator".hashCode()) {
            return new IsoGenerator(cell);
        } else if (classID == "IsoCompost".hashCode()) {
            return new IsoCompost(cell);
        } else if (classID == "Mannequin".hashCode()) {
            return new IsoMannequin(cell);
        } else if (classID == "Window".hashCode()) {
            return new IsoWindow(cell);
        } else if (classID == "Barricade".hashCode()) {
            return new IsoBarricade(cell);
        } else if (classID == "Tree".hashCode()) {
            return IsoTree.getNew();
        } else if (classID == "LightSwitch".hashCode()) {
            return new IsoLightSwitch(cell);
        } else if (classID == "ZombieGiblets".hashCode()) {
            return new IsoZombieGiblets(cell);
        } else if (classID == "MolotovCocktail".hashCode()) {
            return new IsoMolotovCocktail(cell);
        } else if (classID == "Fire".hashCode()) {
            return new IsoFire(cell);
        } else {
            return (IsoObject)(classID == "Vehicle".hashCode() && !GameClient.client ? new BaseVehicle(cell) : new IsoObject(cell));
        }
    }

    @Deprecated
    public static Class<?> factoryClassFromFileInput(IsoCell cell, int classID) {
        if (classID == "IsoObject".hashCode()) {
            return IsoObject.class;
        } else if (classID == "Player".hashCode()) {
            return IsoPlayer.class;
        } else if (classID == "Survivor".hashCode()) {
            return IsoSurvivor.class;
        } else if (classID == "Zombie".hashCode()) {
            return IsoZombie.class;
        } else if (classID == "Pushable".hashCode()) {
            return IsoPushableObject.class;
        } else if (classID == "WheelieBin".hashCode()) {
            return IsoWheelieBin.class;
        } else if (classID == "WorldInventoryItem".hashCode()) {
            return IsoWorldInventoryObject.class;
        } else if (classID == "Jukebox".hashCode()) {
            return IsoJukebox.class;
        } else if (classID == "Curtain".hashCode()) {
            return IsoCurtain.class;
        } else if (classID == "Radio".hashCode()) {
            return IsoRadio.class;
        } else if (classID == "Television".hashCode()) {
            return IsoTelevision.class;
        } else if (classID == "DeadBody".hashCode()) {
            return IsoDeadBody.class;
        } else if (classID == "Barbecue".hashCode()) {
            return IsoBarbecue.class;
        } else if (classID == "ClothingDryer".hashCode()) {
            return IsoClothingDryer.class;
        } else if (classID == "ClothingWasher".hashCode()) {
            return IsoClothingWasher.class;
        } else if (classID == "Fireplace".hashCode()) {
            return IsoFireplace.class;
        } else if (classID == "Stove".hashCode()) {
            return IsoStove.class;
        } else if (classID == "Mannequin".hashCode()) {
            return IsoMannequin.class;
        } else if (classID == "Door".hashCode()) {
            return IsoDoor.class;
        } else if (classID == "Thumpable".hashCode()) {
            return IsoThumpable.class;
        } else if (classID == "Window".hashCode()) {
            return IsoWindow.class;
        } else if (classID == "Barricade".hashCode()) {
            return IsoBarricade.class;
        } else if (classID == "Tree".hashCode()) {
            return IsoTree.class;
        } else if (classID == "LightSwitch".hashCode()) {
            return IsoLightSwitch.class;
        } else if (classID == "ZombieGiblets".hashCode()) {
            return IsoZombieGiblets.class;
        } else if (classID == "MolotovCocktail".hashCode()) {
            return IsoMolotovCocktail.class;
        } else {
            return classID == "Vehicle".hashCode() ? BaseVehicle.class : IsoObject.class;
        }
    }

    @Deprecated
    private static IsoObject factoryFromFileInput(IsoCell cell, DataInputStream input) throws IOException {
        boolean serialise = input.readBoolean();
        if (!serialise) {
            return null;
        } else {
            byte classID = input.readByte();
            return factoryFromFileInput(cell, classID);
        }
    }

    public static IsoObject factoryFromFileInput(IsoCell cell, ByteBuffer b) {
        boolean serialise = b.get() != 0;
        if (!serialise) {
            return null;
        } else {
            byte classID = b.get();
            return factoryFromFileInput(cell, classID);
        }
    }

    public void sync() {
        this.syncIsoObject(false, (byte)0, null, null);
    }

    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBuffer bb) {
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
        } else if (this.getObjectIndex() == -1) {
            System.out
                .println(
                    "ERROR: "
                        + this.getClass().getSimpleName()
                        + " not found on square "
                        + this.square.getX()
                        + ","
                        + this.square.getY()
                        + ","
                        + this.square.getZ()
                );
        } else {
            if (GameClient.client && !bRemote) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                this.syncIsoObjectSend(b);
                PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
            } else if (GameServer.server && !bRemote) {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                }
            } else if (bRemote) {
                this.syncIsoObjectReceive(bb);
                if (GameServer.server) {
                    for (UdpConnection connection : GameServer.udpEngine.connections) {
                        if (source != null && connection.getConnectedGUID() != source.getConnectedGUID()) {
                            ByteBufferWriter b = connection.startPacket();
                            PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                            this.syncIsoObjectSend(b);
                            PacketTypes.PacketType.SyncIsoObject.send(connection);
                        }
                    }
                }
            }

            this.flagForHotSave();
        }
    }

    public void syncIsoObjectSend(ByteBufferWriter bb) {
        bb.putInt(this.square.getX());
        bb.putInt(this.square.getY());
        bb.putInt(this.square.getZ());
        bb.putByte((byte)this.square.getObjects().indexOf(this));
        bb.putByte((byte)1);
        bb.putByte((byte)0);
        bb.putFloat(this.getReserveWaterAmount());
        this.syncFluidContainerSend(bb);
        Durability durability = this.getComponent(ComponentType.Durability);
        if (durability != null) {
            try {
                durability.save(bb.bb);
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }
        }

        if (this instanceof IHasHealth iHasHealth) {
            bb.putInt(iHasHealth.getHealth());
        }
    }

    public void syncIsoObjectReceive(ByteBuffer bb) {
        this.setReserveWaterAmount(bb.getFloat());
        this.syncFluidContainerReceive(bb);
        Durability durability = this.getComponent(ComponentType.Durability);
        if (durability != null) {
            try {
                durability.load(bb, IsoWorld.getWorldVersion());
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }
        }

        if (this instanceof IHasHealth iHasHealth) {
            iHasHealth.setHealth(bb.getInt());
        }
    }

    public void syncFluidContainerReceive(ByteBuffer bb) {
        boolean isExist = bb.get() != 0;
        if (isExist) {
            FluidContainer fluidContainer = this.getComponent(ComponentType.FluidContainer);
            if (fluidContainer != null) {
                try {
                    fluidContainer.load(bb, IsoWorld.getWorldVersion());
                } catch (IOException var7) {
                    throw new RuntimeException(var7);
                }
            } else {
                Component component = ComponentType.FluidContainer.CreateComponent();
                fluidContainer = (FluidContainer)component;

                try {
                    fluidContainer.load(bb, IsoWorld.getWorldVersion());
                } catch (IOException var6) {
                    throw new RuntimeException(var6);
                }

                GameEntityFactory.AddComponent(this, fluidContainer);
            }
        }
    }

    public void syncFluidContainerSend(ByteBufferWriter bb) {
        FluidContainer fluidContainer = this.getComponent(ComponentType.FluidContainer);
        if (fluidContainer != null) {
            bb.putByte((byte)1);

            try {
                fluidContainer.save(bb.bb);
            } catch (IOException var4) {
                throw new RuntimeException(var4);
            }
        } else {
            bb.putByte((byte)0);
        }
    }

    public String getTextureName() {
        return this.sprite == null ? null : this.sprite.name;
    }

    public boolean Serialize() {
        return true;
    }

    public KahluaTable getModData() {
        if (this.table == null) {
            this.table = LuaManager.platform.newTable();
        }

        return this.table;
    }

    public void setModData(KahluaTable newDatas) {
        this.table = newDatas;
    }

    public boolean hasModData() {
        return this.table != null && !this.table.isEmpty();
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.square;
    }

    /**
     * 
     * @param square the square to set
     */
    public void setSquare(IsoGridSquare square) {
        this.square = square;
    }

    public IsoChunk getChunk() {
        IsoGridSquare square = this.getSquare();
        return square == null ? null : square.getChunk();
    }

    public void update() {
    }

    @Override
    public void renderlast() {
    }

    public void DirtySlice() {
    }

    public String getObjectName() {
        if (this.name != null) {
            return this.name;
        } else {
            return this.sprite != null && this.sprite.getParentObjectName() != null ? this.sprite.getParentObjectName() : "IsoObject";
        }
    }

    public final void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.load(input, WorldVersion, false);
    }

    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        int id = input.getInt();
        id = IsoChunk.Fix2x(this.square, id);
        this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, id);
        if (id == -1) {
            this.sprite = IsoSpriteManager.instance.getSprite("");

            assert this.sprite != null;

            assert this.sprite.id == -1;
        }

        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        if (!header.equals(0)) {
            if (header.hasFlags(1)) {
                int child;
                if (header.hasFlags(2)) {
                    child = 1;
                } else {
                    child = input.get() & 255;
                }

                if (IS_DEBUG_SAVE) {
                    String str = GameWindow.ReadStringUTF(input);
                    DebugLog.log(str + ", read = " + child);
                }

                for (int n = 0; n < child; n++) {
                    if (this.attachedAnimSprite == null) {
                        this.attachedAnimSprite = new ArrayList<>();
                    }

                    int wtf = input.getInt();
                    IsoSprite spr = IsoSprite.getSprite(IsoSpriteManager.instance, wtf);
                    IsoSpriteInstance a = null;
                    if (spr != null) {
                        a = spr.newInstance();
                    } else if (Core.debug) {
                        DebugLog.General.warn("discarding attached sprite because it has no tile properties");
                    }

                    byte flags = input.get();
                    boolean readData = false;
                    boolean readAlpha = false;
                    if ((flags & 2) != 0) {
                        readData = true;
                    }

                    if ((flags & 4) != 0 && a != null) {
                        a.flip = true;
                    }

                    if ((flags & 8) != 0 && a != null) {
                        a.copyTargetAlpha = true;
                    }

                    if ((flags & 16) != 0) {
                        readAlpha = true;
                        if (a != null) {
                            a.multiplyObjectAlpha = true;
                        }
                    }

                    if (readData) {
                        float x = input.getFloat();
                        float y = input.getFloat();
                        float z = input.getFloat();
                        float r = Bits.unpackByteToFloatUnit(input.get());
                        float g = Bits.unpackByteToFloatUnit(input.get());
                        float b = Bits.unpackByteToFloatUnit(input.get());
                        if (a != null) {
                            a.offX = x;
                            a.offY = y;
                            a.offZ = z;
                            a.tintr = r;
                            a.tintg = g;
                            a.tintb = b;
                        }
                    } else if (a != null) {
                        a.offX = 0.0F;
                        a.offY = 0.0F;
                        a.offZ = 0.0F;
                        a.tintr = 1.0F;
                        a.tintg = 1.0F;
                        a.tintb = 1.0F;
                        a.alpha = 1.0F;
                        a.targetAlpha = 1.0F;
                    }

                    if (readAlpha) {
                        float alpha = input.getFloat();
                        if (a != null) {
                            a.alpha = alpha;
                        }
                    }

                    if (spr != null) {
                        if (spr.name != null && spr.name.startsWith("overlay_blood_")) {
                            float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
                            IsoWallBloodSplat splat = new IsoWallBloodSplat(worldAge, spr);
                            if (this.wallBloodSplats == null) {
                                this.wallBloodSplats = new ArrayList<>();
                            }

                            this.wallBloodSplats.add(splat);
                        } else {
                            this.attachedAnimSprite.add(a);
                        }
                    }
                }
            }

            if (header.hasFlags(4)) {
                if (IS_DEBUG_SAVE) {
                    String str = GameWindow.ReadStringUTF(input);
                    DebugLog.log(str);
                }

                byte flagsx = input.get();
                if ((flagsx & 2) != 0) {
                    this.name = "Grass";
                } else if ((flagsx & 4) != 0) {
                    this.name = WorldDictionary.getObjectNameFromID(input.get());
                } else if ((flagsx & 8) != 0) {
                    this.name = GameWindow.ReadString(input);
                }

                if ((flagsx & 16) != 0) {
                    this.spriteName = WorldDictionary.getSpriteNameFromID(input.getInt());
                } else if ((flagsx & 32) != 0) {
                    this.spriteName = GameWindow.ReadString(input);
                }
            }

            if (header.hasFlags(8)) {
                float r = Bits.unpackByteToFloatUnit(input.get());
                float g = Bits.unpackByteToFloatUnit(input.get());
                float b = Bits.unpackByteToFloatUnit(input.get());
                this.customColor = new ColorInfo(r, g, b, 1.0F);
            }

            this.doNotSync = header.hasFlags(16);
            this.setOutlineOnMouseover(header.hasFlags(32));
            if (header.hasFlags(64)) {
                BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Short, input);
                if (bits.hasFlags(1)) {
                    int count = input.get();
                    if (count > 0) {
                        if (this.wallBloodSplats == null) {
                            this.wallBloodSplats = new ArrayList<>();
                        }

                        int BloodSplatLifespanDays = SandboxOptions.getInstance().bloodSplatLifespanDays.getValue();
                        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();

                        for (int i = 0; i < count; i++) {
                            IsoWallBloodSplat splat = new IsoWallBloodSplat();
                            splat.load(input, WorldVersion);
                            if (splat.worldAge > worldAgeHours) {
                                splat.worldAge = worldAgeHours;
                            }

                            if (BloodSplatLifespanDays <= 0 || !(worldAgeHours - splat.worldAge >= BloodSplatLifespanDays * 24)) {
                                this.wallBloodSplats.add(splat);
                            }
                        }
                    }
                }

                if (bits.hasFlags(2)) {
                    if (IS_DEBUG_SAVE) {
                        String str = GameWindow.ReadStringUTF(input);
                        DebugLog.log(str);
                    }

                    int numContainers = input.get();

                    for (int i = 0; i < numContainers; i++) {
                        try {
                            ItemContainer container = new ItemContainer();
                            container.id = 0;
                            container.parent = this;
                            container.parent.square = this.square;
                            container.sourceGrid = this.square;
                            container.load(input, WorldVersion);
                            if (i == 0) {
                                if (this instanceof IsoDeadBody) {
                                    container.capacity = 8;
                                }

                                this.container = container;
                            } else {
                                this.addSecondaryContainer(container);
                            }
                        } catch (Exception var20) {
                            if (this.container != null) {
                                DebugLog.log("Failed to stream in container ID: " + this.container.id);
                            }

                            throw new RuntimeException(var20);
                        }
                    }
                }

                if (bits.hasFlags(4)) {
                    if (this.table == null) {
                        this.table = LuaManager.platform.newTable();
                    }

                    this.table.load(input, WorldVersion);
                }

                this.setSpecialTooltip(bits.hasFlags(8));
                if (bits.hasFlags(16)) {
                    this.keyId = input.getInt();
                }

                this.usesExternalWaterSource = bits.hasFlags(32);
                if (bits.hasFlags(64)) {
                    this.sheetRope = true;
                    this.sheetRopeHealth = input.getFloat();
                } else {
                    this.sheetRope = false;
                }

                if (bits.hasFlags(128)) {
                    this.renderYOffset = input.getFloat();
                }

                if (bits.hasFlags(256)) {
                    String spriteName;
                    if (bits.hasFlags(512)) {
                        spriteName = GameWindow.ReadString(input);
                    } else {
                        spriteName = WorldDictionary.getSpriteNameFromID(input.getInt());
                    }

                    if (spriteName != null && !spriteName.isEmpty()) {
                        this.overlaySprite = IsoSpriteManager.instance.getSprite(spriteName);
                        this.overlaySprite.name = spriteName;
                    }
                }

                if (bits.hasFlags(1024)) {
                    float r = Bits.unpackByteToFloatUnit(input.get());
                    float g = Bits.unpackByteToFloatUnit(input.get());
                    float b = Bits.unpackByteToFloatUnit(input.get());
                    float ax = Bits.unpackByteToFloatUnit(input.get());
                    if (this.overlaySprite != null) {
                        this.setOverlaySpriteColor(r, g, b, ax);
                    }
                }

                this.setMovedThumpable(bits.hasFlags(2048));
                if (bits.hasFlags(4096)) {
                    this.loadEntity(input, WorldVersion);
                }

                if (bits.hasFlags(8192)) {
                    this.spriteModelName = GameWindow.ReadStringUTF(input);
                }

                bits.release();
            }
        }

        header.release();
        if (this.sprite == null) {
            this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
            this.sprite.LoadSingleTexture(this.spriteName);
        }
    }

    public final void save(ByteBuffer output) throws IOException {
        this.save(output, false);
    }

    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        output.put((byte)(this.Serialize() ? 1 : 0));
        if (this.Serialize()) {
            output.put(factoryGetClassID(this.getObjectName()));
            output.putInt(this.sprite == null ? -1 : this.sprite.id);
            BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
            if (this.attachedAnimSprite != null) {
                header.addFlags(1);
                if (this.attachedAnimSprite.size() == 1) {
                    header.addFlags(2);
                }

                int s = this.attachedAnimSprite.size() > 255 ? 255 : this.attachedAnimSprite.size();
                if (s != 1) {
                    output.put((byte)s);
                }

                if (IS_DEBUG_SAVE) {
                    GameWindow.WriteString(output, "Writing attached sprites (" + s + ")");
                }

                for (int n = 0; n < s; n++) {
                    IsoSpriteInstance a = this.attachedAnimSprite.get(n);
                    output.putInt(a.getID());
                    byte flags = 0;
                    boolean writeData = false;
                    if (a.offX != 0.0F || a.offY != 0.0F || a.offZ != 0.0F || a.tintr != 1.0F || a.tintg != 1.0F || a.tintb != 1.0F) {
                        flags = (byte)(flags | 2);
                        writeData = true;
                    }

                    if (a.flip) {
                        flags = (byte)(flags | 4);
                    }

                    if (a.copyTargetAlpha) {
                        flags = (byte)(flags | 8);
                    }

                    if (a.multiplyObjectAlpha) {
                        flags = (byte)(flags | 16);
                    }

                    output.put(flags);
                    if (writeData) {
                        output.putFloat(a.offX);
                        output.putFloat(a.offY);
                        output.putFloat(a.offZ);
                        output.put(Bits.packFloatUnitToByte(a.tintr));
                        output.put(Bits.packFloatUnitToByte(a.tintg));
                        output.put(Bits.packFloatUnitToByte(a.tintb));
                    }

                    if (a.multiplyObjectAlpha) {
                        output.putFloat(a.alpha);
                    }
                }
            }

            if (this.name != null || this.spriteName != null) {
                header.addFlags(4);
                if (IS_DEBUG_SAVE) {
                    GameWindow.WriteString(output, "Writing name");
                }

                byte flagsx = 0;
                byte nameID = -1;
                int spriteNameID = -1;
                if (this.name != null) {
                    if (this.name.equals("Grass")) {
                        flagsx = (byte)(flagsx | 2);
                    } else {
                        nameID = WorldDictionary.getIdForObjectName(this.name);
                        if (nameID >= 0) {
                            flagsx = (byte)(flagsx | 4);
                        } else {
                            flagsx = (byte)(flagsx | 8);
                        }
                    }
                }

                if (this.spriteName != null) {
                    spriteNameID = WorldDictionary.getIdForSpriteName(this.spriteName);
                    if (spriteNameID >= 0) {
                        flagsx = (byte)(flagsx | 16);
                    } else {
                        flagsx = (byte)(flagsx | 32);
                    }
                }

                output.put(flagsx);
                if (this.name != null && !this.name.equals("Grass")) {
                    if (nameID >= 0) {
                        output.put(nameID);
                    } else {
                        GameWindow.WriteString(output, this.name);
                    }
                }

                if (this.spriteName != null) {
                    if (spriteNameID >= 0) {
                        output.putInt(spriteNameID);
                    } else {
                        GameWindow.WriteString(output, this.spriteName);
                    }
                }
            }

            if (this.customColor != null) {
                header.addFlags(8);
                output.put(Bits.packFloatUnitToByte(this.customColor.r));
                output.put(Bits.packFloatUnitToByte(this.customColor.g));
                output.put(Bits.packFloatUnitToByte(this.customColor.b));
            }

            if (this.doNotSync) {
                header.addFlags(16);
            }

            if (this.isOutlineOnMouseover()) {
                header.addFlags(32);
            }

            BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Short, output);
            if (this.wallBloodSplats != null) {
                bits.addFlags(1);
                int count = Math.min(this.wallBloodSplats.size(), 32);
                int start = this.wallBloodSplats.size() - count;
                output.put((byte)count);

                for (int i = start; i < this.wallBloodSplats.size(); i++) {
                    this.wallBloodSplats.get(i).save(output);
                }
            }

            if (this.getContainerCount() > 0) {
                bits.addFlags(2);
                if (IS_DEBUG_SAVE) {
                    GameWindow.WriteString(output, "Writing container");
                }

                output.put((byte)this.getContainerCount());

                for (int i = 0; i < this.getContainerCount(); i++) {
                    this.getContainerByIndex(i).save(output);
                }
            }

            if (this.table != null && !this.table.isEmpty()) {
                bits.addFlags(4);
                this.table.save(output);
            }

            if (this.haveSpecialTooltip()) {
                bits.addFlags(8);
            }

            if (this.getKeyId() != -1) {
                bits.addFlags(16);
                output.putInt(this.getKeyId());
            }

            if (this.usesExternalWaterSource) {
                bits.addFlags(32);
            }

            if (this.sheetRope) {
                bits.addFlags(64);
                output.putFloat(this.sheetRopeHealth);
            }

            if (this.renderYOffset != 0.0F) {
                bits.addFlags(128);
                output.putFloat(this.renderYOffset);
            }

            if (this.getOverlaySprite() != null) {
                bits.addFlags(256);
                int spriteNameIDx = WorldDictionary.getIdForSpriteName(this.getOverlaySprite().name);
                if (spriteNameIDx < 0) {
                    bits.addFlags(512);
                    GameWindow.WriteString(output, this.getOverlaySprite().name);
                } else {
                    output.putInt(spriteNameIDx);
                }

                if (this.getOverlaySpriteColor() != null) {
                    bits.addFlags(1024);
                    output.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().r));
                    output.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().g));
                    output.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().b));
                    output.put(Bits.packFloatUnitToByte(this.getOverlaySpriteColor().a));
                }
            }

            if (this.isMovedThumpable()) {
                bits.addFlags(2048);
            }

            if (this.requiresEntitySave()) {
                bits.addFlags(4096);
                this.saveEntity(output);
            }

            if (this.spriteModelName != null) {
                bits.addFlags(8192);
                GameWindow.WriteStringUTF(output, this.spriteModelName);
            }

            if (!bits.equals(0)) {
                header.addFlags(64);
                bits.write();
            } else {
                output.position(bits.getStartPosition());
            }

            header.write();
            header.release();
            bits.release();
        }
    }

    public void saveState(ByteBuffer bb) throws IOException {
    }

    public void loadState(ByteBuffer bb) throws IOException {
    }

    public void softReset() {
        if (this.container != null) {
            this.container.items.clear();
            this.container.explored = false;
            this.setOverlaySprite(null, -1.0F, -1.0F, -1.0F, -1.0F, false);
        }

        if (this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
            for (int j = 0; j < this.attachedAnimSprite.size(); j++) {
                IsoSprite i = this.attachedAnimSprite.get(j).parentSprite;
                if (i.name != null && i.name.contains("blood")) {
                    this.attachedAnimSprite.remove(j);
                    j--;
                }
            }
        }
    }

    public void AttackObject(IsoGameCharacter owner) {
        this.damage = (short)(this.damage - 10);
        HandWeapon weapon = (HandWeapon)owner.getPrimaryHandItem();
        SoundManager.instance.PlaySound(weapon.getDoorHitSound(), false, 2.0F);
        WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
        if (this.damage <= 0) {
            this.square.getObjects().remove(this);
            this.square.RecalcAllWithNeighbours(true);
            if (this.getType() == IsoObjectType.stairsBN
                || this.getType() == IsoObjectType.stairsMN
                || this.getType() == IsoObjectType.stairsTN
                || this.getType() == IsoObjectType.stairsBW
                || this.getType() == IsoObjectType.stairsMW
                || this.getType() == IsoObjectType.stairsTW) {
                this.square.RemoveAllWith(IsoFlagType.attachtostairs);
            }

            int NumPlanks = 1;

            for (int i = 0; i < 1; i++) {
                InventoryItem var5 = this.square.AddWorldInventoryItem("Base.Plank", Rand.Next(-1.0F, 1.0F), Rand.Next(-1.0F, 1.0F), 0.0F);
            }
        }
    }

    public void onMouseRightClick(int lx, int ly) {
    }

    public void onMouseRightReleased() {
    }

    public void Hit(Vector2 collision, IsoObject obj, float damage) {
        if (obj instanceof BaseVehicle baseVehicle) {
            this.HitByVehicle(baseVehicle, damage);
            if (this.damage <= 0 && (BrokenFences.getInstance().isBreakableObject(this) || BentFences.getInstance().isUnbentObject(this))) {
                PropertyContainer props = this.getProperties();
                IsoDirections dirBreak;
                if (props.has(IsoFlagType.collideN) && props.has(IsoFlagType.collideW)) {
                    dirBreak = obj.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                } else if (props.has(IsoFlagType.collideN)) {
                    dirBreak = obj.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                } else {
                    dirBreak = obj.getX() >= this.getX() ? IsoDirections.W : IsoDirections.E;
                }

                if (BentFences.getInstance().isUnbentObject(this)) {
                    if (BentFences.getInstance().getThumpData(this).bendStage == 0) {
                        this.damage = 50;
                        BentFences.getInstance().bendFence(this, dirBreak);
                    } else {
                        BentFences.getInstance().smashFence(this, dirBreak);
                    }
                } else {
                    BrokenFences.getInstance().destroyFence(this, dirBreak);
                }
            }
        }
    }

    public void Damage(float amount) {
        this.damage -= (short)(amount * 0.1);
    }

    public void HitByVehicle(BaseVehicle vehicle, float amount) {
        short previousDmg = this.damage;
        this.damage -= (short)(amount * 0.1);
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.square.x + 0.5F, this.square.y + 0.5F, this.square.z);
        long soundRef = emitter.playSound("VehicleHitObject");
        emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleSpeed"), vehicle.getCurrentSpeedKmHour());
        WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
        if (this.getProperties().has("HitByCar")
            && this.getSprite().getProperties().get("DamagedSprite") != null
            && !this.getSprite().getProperties().get("DamagedSprite").equals("")
            && this.damage <= 90
            && previousDmg > 90) {
            this.setSprite(IsoSpriteManager.instance.getSprite(this.getSprite().getProperties().get("DamagedSprite")));
            if (this.getSprite().getProperties().has("StopCar")) {
                this.getSprite().setType(IsoObjectType.isMoveAbleObject);
            } else {
                this.getSprite().setType(IsoObjectType.MAX);
            }

            if (this instanceof IsoThumpable) {
                ((IsoThumpable)this).setBlockAllTheSquare(false);
            }

            if (GameServer.server) {
                this.transmitUpdatedSpriteToClients();
            }

            this.getSquare().RecalcProperties();
            this.damage = 50;
        }

        if (this.damage <= 40
            && this.getProperties().has("HitByCar")
            && !BrokenFences.getInstance().isBreakableObject(this)
            && !BentFences.getInstance().isBendableFence(this)) {
            this.getSquare().transmitRemoveItemFromSquare(this);
        }

        IsoPlayer driver = Type.tryCastTo(vehicle.getDriverRegardlessOfTow(), IsoPlayer.class);
        if (driver != null && driver.isLocalPlayer()) {
            driver.triggerMusicIntensityEvent("VehicleHitObject");
        }
    }

    public void Collision(Vector2 collision, IsoObject object) {
        if (object instanceof BaseVehicle) {
            if (this.getProperties().has("CarSlowFactor")) {
                int carSlowFactor = Integer.parseInt(this.getProperties().get("CarSlowFactor"));
                BaseVehicle vehicle = (BaseVehicle)object;
                vehicle.ApplyImpulse(this, Math.abs(vehicle.getFudgedMass() * vehicle.getCurrentSpeedKmHour() * carSlowFactor / 100.0F));
            }

            if (this.getProperties().has("HitByCar")) {
                BaseVehicle vehicle = (BaseVehicle)object;
                String min = this.getSprite().getProperties().get("MinimumCarSpeedDmg");
                if (min == null) {
                    min = "150";
                }

                if (Math.abs(vehicle.getCurrentSpeedKmHour()) > Integer.parseInt(min)) {
                    this.HitByVehicle(vehicle, Math.abs(vehicle.getFudgedMass() * vehicle.getCurrentSpeedKmHour()) / 300.0F);
                    if (this.damage <= 0 && BrokenFences.getInstance().isBreakableObject(this)) {
                        boolean bBreakable = BrokenFences.getInstance().isBreakableObject(this);
                        boolean bBendable = BentFences.getInstance().isBendableFence(this);
                        if (bBendable || bBreakable) {
                            PropertyContainer props = this.getProperties();
                            IsoDirections dirBreak;
                            if (props.has(IsoFlagType.collideN) && props.has(IsoFlagType.collideW)) {
                                dirBreak = vehicle.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                            } else if (props.has(IsoFlagType.collideN)) {
                                dirBreak = vehicle.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                            } else {
                                dirBreak = vehicle.getX() >= this.getX() ? IsoDirections.W : IsoDirections.E;
                            }

                            if (bBendable) {
                                if (BentFences.getInstance().getThumpData(this).bendStage == 0) {
                                    this.damage = 50;
                                    BentFences.getInstance().bendFence(this, dirBreak);
                                } else {
                                    BentFences.getInstance().smashFence(this, dirBreak);
                                }
                            } else {
                                BrokenFences.getInstance().destroyFence(this, dirBreak);
                            }
                        }
                    }
                } else if (!this.square.getProperties().has(IsoFlagType.collideN) && !this.square.getProperties().has(IsoFlagType.collideW)) {
                    vehicle.ApplyImpulse(this, Math.abs(vehicle.getFudgedMass() * vehicle.getCurrentSpeedKmHour() * 10.0F / 200.0F));
                    if (vehicle.getCurrentSpeedKmHour() > 3.0F) {
                        vehicle.ApplyImpulse(this, Math.abs(vehicle.getFudgedMass() * vehicle.getCurrentSpeedKmHour() * 10.0F / 150.0F));
                    }

                    vehicle.setSpeedKmHour(0.0F);
                }
            }
        }
    }

    public void UnCollision(IsoObject object) {
    }

    public float GetVehicleSlowFactor(BaseVehicle vehicle) {
        if (this.getProperties().has("CarSlowFactor")) {
            int carSlowFactor = Integer.parseInt(this.getProperties().get("CarSlowFactor"));
            return 33.0F - (10 - carSlowFactor);
        } else {
            return 0.0F;
        }
    }

    /**
     * @return the rerouteCollide
     */
    public IsoObject getRerouteCollide() {
        return this.rerouteCollide;
    }

    /**
     * 
     * @param rerouteCollide the rerouteCollide to set
     */
    public void setRerouteCollide(IsoObject rerouteCollide) {
        this.rerouteCollide = rerouteCollide;
    }

    /**
     * @return the table
     */
    public KahluaTable getTable() {
        return this.table;
    }

    /**
     * 
     * @param table the table to set
     */
    public void setTable(KahluaTable table) {
        this.table = table;
    }

    /**
     * 
     * @param alpha the alpha to set
     */
    public void setAlpha(float alpha) {
        this.setAlpha(IsoPlayer.getPlayerIndex(), alpha);
    }

    /**
     * 
     * @param playerIndex
     * @param alpha the alpha to set
     */
    public void setAlpha(int playerIndex, float alpha) {
        if (!GameServer.server) {
            if (DebugOptions.instance.terrain.renderTiles.forceFullAlpha.getValue()) {
                alpha = 1.0F;
            }

            this.alpha[playerIndex] = PZMath.clamp(alpha, 0.0F, 1.0F);
        }
    }

    /**
     * 
     * @param playerIndex The playerIndex to use
     */
    public void setAlphaToTarget(int playerIndex) {
        this.setAlpha(playerIndex, this.getTargetAlpha(playerIndex));
    }

    /**
     * 
     * @param alpha the alpha to set
     */
    public void setAlphaAndTarget(float alpha) {
        int playerIndex = IsoPlayer.getPlayerIndex();
        this.setAlphaAndTarget(playerIndex, alpha);
    }

    /**
     * 
     * @param playerIndex The playerIndex to use
     * @param alpha
     */
    public void setAlphaAndTarget(int playerIndex, float alpha) {
        this.setAlpha(playerIndex, alpha);
        this.setTargetAlpha(playerIndex, alpha);
    }

    /**
     * @return the alpha
     */
    public float getAlpha() {
        return this.getAlpha(IsoPlayer.getPlayerIndex());
    }

    public float getAlpha(int playerIndex) {
        return GameServer.server ? 1.0F : this.alpha[playerIndex];
    }

    /**
     * @return the AttachedAnimSprite
     */
    public ArrayList<IsoSpriteInstance> getAttachedAnimSprite() {
        return this.attachedAnimSprite;
    }

    /**
     * 
     * @param AttachedAnimSprite the AttachedAnimSprite to set
     */
    public void setAttachedAnimSprite(ArrayList<IsoSpriteInstance> AttachedAnimSprite) {
        this.attachedAnimSprite = AttachedAnimSprite;
    }

    public int getAttachedAnimSpriteCount() {
        return this.attachedAnimSprite == null ? 0 : this.attachedAnimSprite.size();
    }

    public boolean hasAttachedAnimSprites() {
        return this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty();
    }

    public void addAttachedAnimSpriteInstance(IsoSpriteInstance inst) {
        if (inst != null) {
            if (this.getAttachedAnimSprite() == null) {
                this.setAttachedAnimSprite(new ArrayList<>());
            }

            this.getAttachedAnimSprite().add(inst);
            if (PerformanceSettings.fboRenderChunk && Thread.currentThread() == GameWindow.gameThread) {
                this.invalidateRenderChunkLevel(256L);
            }
        }
    }

    public void addAttachedAnimSprite(IsoSprite sprite) {
        if (sprite != null) {
            IsoSpriteInstance inst = IsoSpriteInstance.get(sprite);
            this.addAttachedAnimSpriteInstance(inst);
        }
    }

    public void addAttachedAnimSpriteByName(String spriteName) {
        if (!StringUtils.isNullOrWhitespace(spriteName)) {
            IsoSprite sprite = IsoSprite.getSprite(IsoSpriteManager.instance, spriteName, 0);
            this.addAttachedAnimSprite(sprite);
        }
    }

    /**
     * @return the cell
     */
    public IsoCell getCell() {
        return IsoWorld.instance.currentCell;
    }

    /**
     * @return the AttachedAnimSprite
     */
    public ArrayList<IsoSpriteInstance> getChildSprites() {
        return this.attachedAnimSprite;
    }

    /**
     * 
     * @param AttachedAnimSprite the AttachedAnimSprite to set
     */
    public void setChildSprites(ArrayList<IsoSpriteInstance> AttachedAnimSprite) {
        this.attachedAnimSprite = AttachedAnimSprite;
    }

    public void clearAttachedAnimSprite() {
        this.RemoveAttachedAnims();
    }

    /**
     * @return the container
     */
    public ItemContainer getContainer() {
        return this.container;
    }

    /**
     * 
     * @param container the container to set
     */
    public void setContainer(ItemContainer container) {
        container.parent = this;
        this.container = container;
    }

    public <T> PZArrayList<ItemContainer> getContainers(
        T in_paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> in_isValidPredicate, PZArrayList<ItemContainer> inout_containerList
    ) {
        ItemContainer container = this.getContainer();
        if (container == null) {
            return inout_containerList;
        } else {
            DebugType.General.noise("Container Detected!");
            boolean isValid = in_isValidPredicate == null || in_isValidPredicate.accept(in_paramToCompare, container);
            if (isValid) {
                inout_containerList.addUniqueReference(container);
            }

            return inout_containerList;
        }
    }

    public ItemContainer getContainerClickedOn(int screenX, int screenY) {
        ItemContainer container1 = this.getContainer();
        if (this.getContainerCount() < 2) {
            return container1;
        } else {
            Vector2f v = BaseVehicle.allocVector2f();
            Vector2f v1 = FBORenderObjectPicker.getInstance().getPointRelativeToTopLeftOfTexture(this, screenX, screenY, v);
            float localY = v.y;
            BaseVehicle.releaseVector2f(v);
            if (v1 == null) {
                return container1;
            } else {
                Texture texture = this.getSprite().getTextureForCurrentFrame(this.getDir());
                return localY < texture.getHeight() / 2.0F ? this.getContainerByIndex(1) : container1;
            }
        }
    }

    /**
     * @return the dir
     */
    public IsoDirections getDir() {
        return this.getForwardIsoDirection();
    }

    /**
     * 
     * @param dir the dir to set
     */
    public void setDir(int dir) {
        this.setForwardIsoDirection(dir);
    }

    @Override
    public void setForwardIsoDirection(IsoDirections dir) {
        this.dir = dir;
    }

    public void setForwardIsoDirection(int dir) {
        this.setForwardIsoDirection(IsoDirections.fromIndex(dir));
    }

    public IsoDirections getForwardIsoDirection() {
        return this.dir;
    }

    public IsoDirections getForwardMovementIsoDirection() {
        return this.getForwardIsoDirection();
    }

    /**
     * @return the Damage
     */
    public short getDamage() {
        return this.damage;
    }

    /**
     * 
     * @param Damage the Damage to set
     */
    public void setDamage(short Damage) {
        this.damage = Damage;
    }

    /**
     * @return the NoPicking
     */
    public boolean isNoPicking() {
        return this.noPicking;
    }

    /**
     * 
     * @param NoPicking the NoPicking to set
     */
    public void setNoPicking(boolean NoPicking) {
        this.noPicking = NoPicking;
    }

    /**
     * @return the OutlineOnMouseover
     */
    public boolean isOutlineOnMouseover() {
        return this.outlineOnMouseover;
    }

    /**
     * 
     * @param OutlineOnMouseover the OutlineOnMouseover to set
     */
    public void setOutlineOnMouseover(boolean OutlineOnMouseover) {
        this.outlineOnMouseover = OutlineOnMouseover;
    }

    /**
     * @return the rerouteMask
     */
    public IsoObject getRerouteMask() {
        return this.rerouteMask;
    }

    /**
     * 
     * @param rerouteMask the rerouteMask to set
     */
    public void setRerouteMask(IsoObject rerouteMask) {
        this.rerouteMask = rerouteMask;
    }

    /**
     * @return the sprite
     */
    public IsoSprite getSprite() {
        return this.sprite;
    }

    /**
     * 
     * @param sprite the sprite to set
     */
    public void setSprite(IsoSprite sprite) {
        this.sprite = sprite;
        this.windRenderEffects = null;
        this.checkMoveWithWind();
        if (Thread.currentThread() == GameWindow.gameThread) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    public void setSprite(String name) {
        IsoSprite sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        sprite.LoadSingleTexture(name);
        this.setSprite(sprite);
        this.tile = name;
        this.spriteName = name;
    }

    public void setSpriteFromName(String name) {
        IsoSprite sprite = IsoSpriteManager.instance.getSprite(name);
        this.setSprite(sprite);
    }

    public IsoSpriteGrid getSpriteGrid() {
        IsoSprite sprite = this.getSprite();
        return sprite == null ? null : sprite.getSpriteGrid();
    }

    public boolean hasSpriteGrid() {
        return this.getSpriteGrid() != null;
    }

    /**
     * @return the targetAlpha
     */
    public float getTargetAlpha() {
        return this.getTargetAlpha(IsoPlayer.getPlayerIndex());
    }

    /**
     * 
     * @param targetAlpha the targetAlpha to set
     */
    public void setTargetAlpha(float targetAlpha) {
        this.setTargetAlpha(IsoPlayer.getPlayerIndex(), targetAlpha);
    }

    /**
     * 
     * @param playerIndex
     * @param targetAlpha the targetAlpha to set
     */
    public void setTargetAlpha(int playerIndex, float targetAlpha) {
        if (!GameServer.server) {
            this.targetAlpha[playerIndex] = PZMath.clamp(targetAlpha, 0.0F, 1.0F);
        }
    }

    public float getTargetAlpha(int playerIndex) {
        return GameServer.server ? 1.0F : this.targetAlpha[playerIndex];
    }

    /**
     * Returns TRUE if both Alpha nad TargetAlpha are transparent, or near-zero.
     */
    public boolean isAlphaAndTargetZero() {
        int playerIndex = IsoPlayer.getPlayerIndex();
        return this.isAlphaAndTargetZero(playerIndex);
    }

    public boolean isAlphaAndTargetZero(int playerIndex) {
        return this.isAlphaZero(playerIndex) && this.isTargetAlphaZero(playerIndex);
    }

    /**
     * Returns TRUE if Alpha is transparent, or near-zero.
     */
    public boolean isAlphaZero() {
        int playerIndex = IsoPlayer.getPlayerIndex();
        return this.isAlphaZero(playerIndex);
    }

    public boolean isAlphaZero(int playerIndex) {
        return GameServer.server ? false : this.alpha[playerIndex] <= 0.001F;
    }

    public boolean isTargetAlphaZero(int playerIndex) {
        return GameServer.server ? false : this.targetAlpha[playerIndex] <= 0.001F;
    }

    /**
     * @return the type
     */
    public IsoObjectType getType() {
        return this.sprite == null ? IsoObjectType.MAX : this.sprite.getType();
    }

    public void setType(IsoObjectType type) {
        if (this.sprite != null) {
            this.sprite.setType(type);
        }
    }

    public void addChild(IsoObject child) {
        if (this.children == null) {
            this.children = new ArrayList<>(4);
        }

        this.children.add(child);
    }

    public void debugPrintout() {
        System.out.println(this.getClass().toString());
        System.out.println(this.getObjectName());
    }

    protected void checkMoveWithWind() {
        this.checkMoveWithWind(this.sprite != null && this.sprite.isBush);
    }

    protected void checkMoveWithWind(boolean isTreeLike) {
        if (!GameServer.server) {
            if (this.sprite != null && this.windRenderEffects == null && this.sprite.moveWithWind) {
                if (this.getSquare() != null) {
                    IsoGridSquare west = this.getCell().getGridSquare(this.getSquare().x - 1, this.getSquare().y, this.getSquare().z);
                    if (west != null) {
                        IsoGridSquare south = this.getCell().getGridSquare(west.x, west.y + 1, west.z);
                        if (south != null && !south.isExteriorCache && south.getWall(true) != null) {
                            this.windRenderEffects = null;
                            return;
                        }
                    }

                    IsoGridSquare north = this.getCell().getGridSquare(this.getSquare().x, this.getSquare().y - 1, this.getSquare().z);
                    if (north != null) {
                        IsoGridSquare east = this.getCell().getGridSquare(north.x + 1, north.y, north.z);
                        if (east != null && !east.isExteriorCache && east.getWall(false) != null) {
                            this.windRenderEffects = null;
                            return;
                        }
                    }
                }

                this.windRenderEffects = ObjectRenderEffects.getNextWindEffect(this.sprite.windType, isTreeLike);
            } else {
                if (this.windRenderEffects != null && (this.sprite == null || !this.sprite.moveWithWind)) {
                    this.windRenderEffects = null;
                }
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.tintr = 1.0F;
        this.tintg = 1.0F;
        this.tintb = 1.0F;
        this.name = null;
        this.doRender = true;
        this.isSceneCulled = false;
        this.table = null;
        this.rerouteCollide = null;
        if (this.attachedAnimSprite != null) {
            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance isoSpriteInstance = this.attachedAnimSprite.get(i);
                IsoSpriteInstance.add(isoSpriteInstance);
            }

            this.attachedAnimSprite.clear();
        }

        if (this.wallBloodSplats != null) {
            this.wallBloodSplats.clear();
        }

        this.overlaySprite = null;
        this.overlaySpriteColor = null;
        this.customColor = null;
        if (this.container != null) {
            this.container.items.clear();
            this.container.includingObsoleteItems.clear();
            this.container.setParent(null);
            this.container.setSourceGrid(null);
            this.container.vehiclePart = null;
        }

        this.container = null;
        this.dir = IsoDirections.N;
        this.damage = 100;
        this.partialThumpDmg = 0.0F;
        this.noPicking = false;
        this.offsetX = 32 * Core.tileScale;
        this.offsetY = 96 * Core.tileScale;
        this.outlineOnMouseover = false;
        this.rerouteMask = null;
        this.sprite = null;
        this.square = null;
        if (!GameServer.server) {
            for (int n = 0; n < 4; n++) {
                this.setAlphaAndTarget(n, 1.0F);
                this.highlightColor[n].set(0.9F, 1.0F, 0.0F, 1.0F);
                this.outlineHighlightCol[n] = -1;
                this.renderInfo[n].layer = ObjectRenderLayer.None;
                this.renderInfo[n].targetAlpha = 0.0F;
            }
        }

        this.neverDoneAlpha = true;
        this.alphaForced = false;
        this.ppfHighlighted = 0;
        this.ppfHighlightRenderOnce = 0;
        this.ppfBlink = 0;
        this.satChair = false;
        this.tile = null;
        this.spriteName = null;
        this.specialTooltip = false;
        this.usesExternalWaterSource = false;
        this.externalWaterSource = null;
        if (this.secondaryContainers != null) {
            for (int i = 0; i < this.secondaryContainers.size(); i++) {
                ItemContainer container = this.secondaryContainers.get(i);
                container.items.clear();
                container.includingObsoleteItems.clear();
                container.setParent(null);
                container.setSourceGrid(null);
                container.vehiclePart = null;
            }

            this.secondaryContainers.clear();
        }

        this.renderYOffset = 0.0F;
        this.sx = 0.0F;
        this.windRenderEffects = null;
        this.objectRenderEffects = null;
        this.sheetRope = false;
        this.sheetRopeHealth = 100.0F;
        this.movedThumpable = false;
        this.isoEntityNetId = -1L;
        this.spriteModelName = null;
        this.spriteModel = null;
        this.spriteModelInit = null;
        this.clockScript = null;
        this.clockScriptInit = null;
        this.lightSource = null;
        this.onOverlay = null;
        this.hasPowerTick = -1L;
        this.hasPower = false;
    }

    public long customHashCode() {
        if (this.doNotSync) {
            return 0L;
        } else {
            try {
                long h = 1L;
                if (this.getObjectName() != null) {
                    h = h * 3L + this.getObjectName().hashCode();
                }

                if (this.name != null) {
                    h = h * 2L + this.name.hashCode();
                }

                if (this.container != null) {
                    h = ++h + this.container.items.size();

                    for (int i = 0; i < this.container.items.size(); i++) {
                        h += this.container.items.get(i).getModule().hashCode()
                            + this.container.items.get(i).getType().hashCode()
                            + this.container.items.get(i).id;
                    }
                }

                return h + this.square.getObjects().indexOf(this);
            } catch (Throwable var4) {
                DebugLog.log("ERROR: " + var4.getMessage());
                return 0L;
            }
        }
    }

    public void SetName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getSpriteName() {
        return this.spriteName == null && this.getSprite() != null && this.getSprite().getName() != null ? this.getSprite().getName() : this.spriteName;
    }

    public String getTile() {
        return this.tile;
    }

    public void setTile(String tile) {
        this.tile = tile;
    }

    public boolean isCharacter() {
        return this instanceof IsoLivingCharacter;
    }

    public boolean isZombie() {
        return false;
    }

    public String getScriptName() {
        return "none";
    }

    public IsoSpriteInstance AttachAnim(
        String ObjectName,
        String AnimName,
        int NumFrames,
        float frameIncrease,
        int OffsetX,
        int OffsetY,
        boolean Looping,
        int FinishHoldFrameIndex,
        boolean DeleteWhenFinished,
        float zBias,
        ColorInfo TintMod
    ) {
        return this.AttachAnim(
            ObjectName, AnimName, NumFrames, frameIncrease, OffsetX, OffsetY, Looping, FinishHoldFrameIndex, DeleteWhenFinished, zBias, TintMod, false
        );
    }

    public IsoSpriteInstance AttachAnim(
        String ObjectName,
        String AnimName,
        int NumFrames,
        float frameIncrease,
        int OffsetX,
        int OffsetY,
        boolean Looping,
        int FinishHoldFrameIndex,
        boolean DeleteWhenFinished,
        float zBias,
        ColorInfo TintMod,
        boolean bRandomFrame
    ) {
        IsoSprite NewAnimSprite = IsoSprite.CreateSpriteUsingCache(ObjectName, AnimName, NumFrames);
        NewAnimSprite.tintMod.set(TintMod);
        NewAnimSprite.soffX = (short)(-OffsetX);
        NewAnimSprite.soffY = (short)(-OffsetY);
        NewAnimSprite.animate = NumFrames > 1;
        NewAnimSprite.loop = Looping;
        NewAnimSprite.deleteWhenFinished = DeleteWhenFinished;
        NewAnimSprite.PlayAnim(AnimName);
        IsoSpriteInstance inst = NewAnimSprite.def;
        inst.animFrameIncrease = frameIncrease;
        inst.frame = 0.0F;
        if (bRandomFrame && NewAnimSprite.getFrameCount() > 1) {
            inst.frame = Rand.Next(0.0F, (float)NewAnimSprite.getFrameCount());
        }

        this.addAttachedAnimSpriteInstance(inst);
        return inst;
    }

    public void AttachExistingAnim(
        IsoSprite spr, int OffsetX, int OffsetY, boolean Looping, int FinishHoldFrameIndex, boolean DeleteWhenFinished, float zBias, ColorInfo TintMod
    ) {
        spr.tintMod.r = TintMod.r;
        spr.tintMod.g = TintMod.g;
        spr.tintMod.b = TintMod.b;
        spr.tintMod.a = TintMod.a;
        spr.soffX = (short)(-OffsetX);
        spr.soffY = (short)(-OffsetY);
        spr.animate = spr.hasAnimation() && spr.currentAnim.frames.size() > 1;
        spr.loop = Looping;
        spr.deleteWhenFinished = DeleteWhenFinished;
        this.addAttachedAnimSprite(spr);
    }

    public void AttachExistingAnim(IsoSprite spr, int OffsetX, int OffsetY, boolean Looping, int FinishHoldFrameIndex, boolean DeleteWhenFinished, float zBias) {
        this.AttachExistingAnim(spr, OffsetX, OffsetY, Looping, FinishHoldFrameIndex, DeleteWhenFinished, zBias, new ColorInfo());
    }

    public void DoTooltip(ObjectTooltip tooltipUI) {
    }

    public void DoSpecialTooltip(ObjectTooltip tooltipUI, IsoGridSquare square) {
        if (this.haveSpecialTooltip()) {
            tooltipUI.setHeight(0.0);
            LuaEventManager.triggerEvent("DoSpecialTooltip", tooltipUI, square);
            if (tooltipUI.getHeight() == 0.0) {
                tooltipUI.hide();
            }
        }
    }

    public ItemContainer getItemContainer() {
        return this.container;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    /**
     * 
     * @param offsetX the offsetX to set
     */
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    /**
     * 
     * @param offsetY the offsetY to set
     */
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public IsoObject getRerouteMaskObject() {
        return this.rerouteMask;
    }

    public boolean HasTooltip() {
        return false;
    }

    public boolean getUsesExternalWaterSource() {
        return this.usesExternalWaterSource;
    }

    public void setUsesExternalWaterSource(boolean b) {
        this.usesExternalWaterSource = b;
    }

    public boolean hasExternalWaterSource() {
        return this.externalWaterSource != null && this.externalWaterSource.isExistInTheWorld();
    }

    public void doFindExternalWaterSource() {
        this.externalWaterSource = this.FindExternalWaterSource();
    }

    public IsoObject FindExternalWaterSource() {
        IsoSpriteGrid grid = this.getSpriteGrid();
        if (grid == null) {
            return FindExternalWaterSource(this.square);
        } else {
            if (this.square != null && this.sprite != null) {
                int spritePosX = grid.getSpriteGridPosX(this.sprite);
                int spritePosY = grid.getSpriteGridPosY(this.sprite);
                IsoObject source = null;

                for (int x = 0; x < grid.getWidth(); x++) {
                    for (int y = 0; y < grid.getHeight(); y++) {
                        int cellX = this.square.x - spritePosX + x;
                        int cellY = this.square.y - spritePosY + y;
                        int cellZ = this.square.z;
                        source = FindExternalWaterSource(cellX, cellY, cellZ);
                        if (source != null) {
                            return source;
                        }
                    }
                }
            }

            return null;
        }
    }

    public static IsoObject FindExternalWaterSource(IsoGridSquare square) {
        return square == null ? null : FindExternalWaterSource(square.getX(), square.getY(), square.getZ());
    }

    public static IsoObject FindExternalWaterSource(int x, int y, int z) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z + 1);
        IsoObject empty = null;
        IsoObject object = FindWaterSourceOnSquare(square);
        if (object != null) {
            if (object.hasFluid()) {
                return object;
            }

            empty = object;
        }

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dy != 0) {
                    square = IsoWorld.instance.currentCell.getGridSquare(x + dx, y + dy, z + 1);
                    object = FindWaterSourceOnSquare(square);
                    if (object != null) {
                        if (object.hasFluid()) {
                            return object;
                        }

                        if (empty == null) {
                            empty = object;
                        }
                    }
                }
            }
        }

        return empty;
    }

    public static IsoObject FindWaterSourceOnSquare(IsoGridSquare square) {
        if (square == null) {
            return null;
        } else {
            PZArrayList<IsoObject> objects = square.getObjects();

            for (int i = 0; i < objects.size(); i++) {
                IsoObject object = objects.get(i);
                if (object instanceof IsoThumpable
                    && (object.getSprite() == null || !object.getSprite().solidfloor)
                    && !object.getUsesExternalWaterSource()
                    && object.getFluidCapacity() > 0.0F) {
                    return object;
                }
            }

            return null;
        }
    }

    public int getPipedFuelAmount() {
        if (this.sprite == null) {
            return 0;
        } else {
            double amount = -1.0;
            boolean noSign = false;
            if (this.hasModData() && !this.getModData().isEmpty()) {
                Object v = this.getModData().rawget("fuelAmount");
                if (v != null) {
                    amount = (Double)v;
                }
            }

            if (this.sprite.getProperties().has("fuelAmount")) {
                if (SandboxOptions.getInstance().fuelStationGasInfinite.getValue()) {
                    return 1000;
                }

                if (amount == -1.0
                    && (SandboxOptions.getInstance().allowExteriorGenerator.getValue() && this.getSquare().haveElectricity() || this.hasGridPower())) {
                    float minMod = (float)SandboxOptions.getInstance().fuelStationGasMin.getValue();
                    float maxMod = (float)SandboxOptions.getInstance().fuelStationGasMax.getValue();
                    if (minMod > maxMod) {
                        minMod = maxMod;
                    }

                    amount = (int)Rand.Next(
                        Integer.parseInt(this.sprite.getProperties().get("fuelAmount")) * minMod,
                        Integer.parseInt(this.sprite.getProperties().get("fuelAmount")) * maxMod
                    );
                    if (this.getSquare().isNoGas() || Rand.Next(100) < SandboxOptions.getInstance().fuelStationGasEmptyChance.getValue()) {
                        amount = 0.0;
                    }

                    if (amount == 0.0 && Rand.NextBool(2)) {
                        IsoDirections facing = this.getFacing();
                        if (facing == null) {
                            facing = IsoDirections.E;
                        }

                        if (Rand.NextBool(2)) {
                            if (facing == IsoDirections.E) {
                                facing = IsoDirections.W;
                            }

                            if (facing == IsoDirections.S) {
                                facing = IsoDirections.N;
                            }
                        }

                        IsoGridSquare sq = this.square.getAdjacentSquare(facing);
                        boolean doSign = sq != null && !sq.isNoGas() && sq.getObjects().size() < 2;
                        if (doSign && (facing == IsoDirections.E || facing == IsoDirections.W)) {
                            doSign = sq.getAdjacentSquare(IsoDirections.N) != null
                                && sq.getAdjacentSquare(IsoDirections.N).getObjects().size() < 2
                                && sq.getAdjacentSquare(IsoDirections.S) != null
                                && sq.getAdjacentSquare(IsoDirections.S).getObjects().size() < 2;
                        }

                        if (doSign && (facing == IsoDirections.S || facing == IsoDirections.N)) {
                            doSign = sq.getAdjacentSquare(IsoDirections.E) != null
                                && sq.getAdjacentSquare(IsoDirections.E).getObjects().size() < 2
                                && sq.getAdjacentSquare(IsoDirections.W) != null
                                && sq.getAdjacentSquare(IsoDirections.W).getObjects().size() < 2;
                        }

                        if (doSign) {
                            String sign = "signs_one-off_07_8";
                            int last = 5;
                            boolean g2g = Objects.equals(this.square.getZombiesType(), "Gas2Go");
                            boolean fo = Objects.equals(this.square.getZombiesType(), "Fossoil");
                            if (Objects.equals(this.square.getZombiesType(), "Fossoil")) {
                                sign = "location_shop_fossoil_01_6";
                                last = 8;
                            } else if (Objects.equals(this.square.getZombiesType(), "Gas2Go")) {
                                sign = "location_shop_gas2go_01_3";
                                last = 8;
                            }

                            if (facing == IsoDirections.E || facing == IsoDirections.W) {
                                last++;
                            }

                            sign = sign + last;
                            sq.addTileObject(sign);
                        }
                    }

                    this.getModData().rawset("fuelAmount", amount);
                    this.transmitModData();
                    return (int)amount;
                }
            }

            return (int)amount;
        }
    }

    public void setPipedFuelAmount(int units) {
        units = Math.max(0, units);
        int old = this.getPipedFuelAmount();
        if (units != old) {
            this.getModData().rawset("fuelAmount", (double)units);
            this.transmitModData();
        }
    }

    private boolean isWaterInfinite() {
        if (this.sprite == null) {
            return false;
        } else if (this.sprite.getProperties().has("waterAmount") && Integer.parseInt(this.sprite.getProperties().get("waterAmount")) >= 9999) {
            return true;
        } else if (this.square != null && this.square.getRoom() != null) {
            if (!this.sprite.getProperties().has(IsoFlagType.waterPiped)) {
                return false;
            } else if (this.getSquare().isNoWater()) {
                return false;
            } else {
                return (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30)
                        >= SandboxOptions.instance.getWaterShutModifier()
                    ? false
                    : !this.hasModData()
                        || !(this.getModData().rawget("canBeWaterPiped") instanceof Boolean)
                        || !(Boolean)this.getModData().rawget("canBeWaterPiped");
            }
        } else {
            return false;
        }
    }

    private FluidType getInfiniteWaterType() {
        if (this.isUnmovedPipedWaterSource()) {
            return FluidType.Water;
        } else {
            String var1 = this.sprite.getName();
            switch (var1) {
                case "camping_01_16":
                case "camping_01_64":
                case "camping_01_65":
                case "camping_01_66":
                case "camping_01_67":
                    return FluidType.Water;
                default:
                    return FluidType.TaintedWater;
            }
        }
    }

    private boolean isUnmovedPipedWaterSource() {
        return this.sprite.getProperties().has(IsoFlagType.waterPiped)
            && (
                !this.hasModData()
                    || !(this.getModData().rawget("canBeWaterPiped") instanceof Boolean)
                    || !(Boolean)this.getModData().rawget("canBeWaterPiped")
            );
    }

    private IsoObject checkExternalFluidSource() {
        if (!this.usesExternalWaterSource) {
            return null;
        } else {
            if (this.externalWaterSource == null || !this.externalWaterSource.isExistInTheWorld() || !this.externalWaterSource.hasFluid()) {
                this.doFindExternalWaterSource();
            }

            return this.externalWaterSource;
        }
    }

    public float getFluidAmount() {
        if (this.sprite == null) {
            return 0.0F;
        } else if (this.usesExternalWaterSource) {
            if (this.isWaterInfinite()) {
                return 10000.0F;
            } else {
                IsoObject object = this.checkExternalFluidSource();
                return object == null ? 0.0F : object.getFluidAmount();
            }
        } else if (this.isWaterInfinite()) {
            return 10000.0F;
        } else if (this.getFluidContainer() != null && this.getFluidContainer().getAmount() > 0.0F) {
            return this.getFluidContainer().getAmount();
        } else if (this.isUnmovedPipedWaterSource() && this.hasReserveWater()) {
            return this.getReserveWaterAmount();
        } else if (this.getFluidContainer() != null && this.getFluidContainer().getAmount() > 0.0F) {
            return (int)this.getFluidContainer().getAmount();
        } else {
            return this.square != null
                    && !this.square.getProperties().has(IsoFlagType.water)
                    && this.sprite != null
                    && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                    && this.square.getPuddlesInGround() > 0.09F
                ? this.square.getPuddlesInGround() * 10.0F
                : 0.0F;
        }
    }

    public void emptyFluid() {
        if (this.usesExternalWaterSource) {
            if (!this.isWaterInfinite()) {
                IsoObject object = this.checkExternalFluidSource();
                if (object != null) {
                    object.emptyFluid();
                }
            }
        } else {
            if (GameServer.server || !GameClient.client) {
                float old = this.getFluidAmount();
                FluidContainer fc = this.getFluidContainer();
                if (fc != null) {
                    fc.Empty();
                    this.sync();
                    LuaEventManager.triggerEvent("OnWaterAmountChange", this, old);
                }
            }
        }
    }

    public float getFluidCapacity() {
        if (this.sprite == null) {
            return 0.0F;
        } else if (this.usesExternalWaterSource) {
            if (this.isWaterInfinite()) {
                return 10000.0F;
            } else {
                IsoObject object = this.checkExternalFluidSource();
                return object != null ? object.getFluidCapacity() : 0.0F;
            }
        } else if (this.isWaterInfinite()) {
            return 10000.0F;
        } else if (this.getFluidContainer() != null && this.getFluidContainer().getCapacity() > 0.0F) {
            return this.getFluidContainer().getCapacity();
        } else if (this.isUnmovedPipedWaterSource() && this.hasReserveWater()) {
            return this.getReserveWaterMax();
        } else if (this.square != null
            && !this.square.getProperties().has(IsoFlagType.water)
            && this.sprite != null
            && this.sprite.getProperties().has(IsoFlagType.solidfloor)
            && this.square.getPuddlesInGround() > 0.09F) {
            return this.square.getPuddlesInGround() * 10.0F;
        } else {
            return this.getFluidContainer() != null && this.getFluidContainer().getAmount() > 0.0F ? (int)this.getFluidContainer().getCapacity() : 0.0F;
        }
    }

    public float useFluid(float amount) {
        if (this.sprite == null) {
            return 0.0F;
        } else if (this.usesExternalWaterSource) {
            if (this.isWaterInfinite()) {
                return amount;
            } else {
                IsoObject object = this.checkExternalFluidSource();
                return object != null ? object.useFluid(amount) : 0.0F;
            }
        } else {
            if (!this.usesExternalWaterSource) {
                if (this.sprite.getProperties().has(IsoFlagType.water)) {
                    return amount;
                }

                if (this.isWaterInfinite()) {
                    return amount;
                }
            }

            float oldAvail = this.getFluidAmount();
            float used = PZMath.clamp(amount, 0.0F, oldAvail);
            if ((GameServer.server || !GameClient.client) && used > 0.0F) {
                FluidContainer fc = this.getFluidContainer();
                if (fc != null && fc.getAmount() > 0.0F) {
                    fc.removeFluid(used);
                    this.sync();
                    LuaEventManager.triggerEvent("OnWaterAmountChange", this, oldAvail);
                } else if (this.isUnmovedPipedWaterSource() && this.hasReserveWater()) {
                    float remaining = this.getReserveWaterAmount() - used;
                    this.setReserveWaterAmount(remaining);
                }
            }

            return this.square != null
                    && this.sprite != null
                    && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                    && this.square.getPuddlesInGround() > 0.09F
                ? used
                : used;
        }
    }

    public void addFluid(FluidType fluidType, float amount) {
        if (this.usesExternalWaterSource) {
            if (!this.isWaterInfinite()) {
                IsoObject object = this.checkExternalFluidSource();
                if (object != null) {
                    object.addFluid(fluidType, amount);
                }
            }
        } else {
            if (GameServer.server || !GameClient.client) {
                float old = this.getFluidAmount();
                float freeCapacity = this.getFluidCapacity() - this.getFluidAmount();
                amount = PZMath.clamp(amount, 0.0F, freeCapacity);
                if (amount > 0.0F) {
                    FluidContainer fc = this.getFluidContainer();
                    if (fc != null) {
                        fc.addFluid(fluidType, amount);
                        this.sync();
                        LuaEventManager.triggerEvent("OnWaterAmountChange", this, old);
                    }
                }
            }
        }
    }

    public boolean canTransferFluidFrom(FluidContainer other) {
        if (this.usesExternalWaterSource) {
            IsoObject object = this.checkExternalFluidSource();
            if (object != null) {
                return object.canTransferFluidFrom(other);
            }
        }

        FluidContainer fc = this.getFluidContainer();
        return fc != null ? FluidContainer.CanTransfer(other, fc) : false;
    }

    public boolean canTransferFluidTo(FluidContainer other) {
        if (this.usesExternalWaterSource) {
            IsoObject object = this.checkExternalFluidSource();
            if (object != null && object.canTransferFluidTo(other)) {
                return true;
            }
        }

        FluidContainer fc = this.getFluidContainer();
        if (fc != null) {
            return FluidContainer.CanTransfer(fc, other);
        } else if (this.hasWater()) {
            return this.isTaintedWater() ? other.canAddFluid(Fluid.TaintedWater) : other.canAddFluid(Fluid.Water);
        } else {
            return false;
        }
    }

    private FluidContainer createSampleAndPurifyWater(FluidContainer source, float amount) {
        FluidContainer tempFc = FluidContainer.CreateContainer();
        if (source != null) {
            amount = PZMath.clamp(amount, 0.0F, source.getAmount());
            FluidSample sample = source.createFluidSample(1.0F);

            for (int i = 0; i < sample.size(); i++) {
                FluidInstance fluidInstance = sample.getFluidInstance(i);
                FluidType fluidType = fluidInstance.getFluid().getFluidType() == FluidType.TaintedWater
                    ? FluidType.Water
                    : fluidInstance.getFluid().getFluidType();
                tempFc.addFluid(fluidType, amount * fluidInstance.getAmount());
            }
        }

        return tempFc;
    }

    public float transferFluidTo(FluidContainer target, float amount) {
        if (target == null) {
            return 0.0F;
        } else {
            float sourceAvail = this.getFluidAmount();
            float targetCapacity = target.getFreeCapacity();
            float transferMax = Math.min(targetCapacity, sourceAvail);
            float used = PZMath.clamp(amount, 0.0F, transferMax);
            if (GameServer.server || !GameClient.client) {
                if (this.usesExternalWaterSource) {
                    if (this.isWaterInfinite()) {
                        target.addFluid(FluidType.Water, used);
                        return used;
                    }

                    IsoObject object = this.checkExternalFluidSource();
                    if (object != null) {
                        FluidContainer fc = object.getFluidContainer();
                        if (fc != null && fc.getAmount() > 0.0F && FluidContainer.CanTransfer(fc, target)) {
                            FluidContainer tempFc = this.createSampleAndPurifyWater(fc, used);
                            FluidContainer.Transfer(tempFc, target);
                            float consumed = used - tempFc.getAmount();
                            fc.removeFluid(consumed);
                            this.sync();
                            FluidContainer.DisposeContainer(tempFc);
                            LuaEventManager.triggerEvent("OnWaterAmountChange", this, sourceAvail);
                            return consumed;
                        }

                        return object.transferFluidTo(target, used);
                    }

                    return 0.0F;
                }

                if (this.square != null
                    && this.sprite != null
                    && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                    && this.square.getPuddlesInGround() > 0.09F) {
                    target.addFluid(FluidType.TaintedWater, used);
                    return used;
                }

                if (!this.usesExternalWaterSource) {
                    if (this.sprite.getProperties().has(IsoFlagType.water)) {
                        target.addFluid(FluidType.TaintedWater, used);
                        return used;
                    }

                    if (this.isWaterInfinite()) {
                        FluidType fluidType = this.getInfiniteWaterType();
                        target.addFluid(fluidType, used);
                        return used;
                    }
                }

                if (used > 0.0F) {
                    FluidContainer fc = this.getFluidContainer();
                    if (fc != null) {
                        fc.transferTo(target, used);
                        this.sync();
                        LuaEventManager.triggerEvent("OnWaterAmountChange", this, sourceAvail);
                        return used;
                    }

                    if (this.isUnmovedPipedWaterSource() && this.hasReserveWater()) {
                        target.addFluid(FluidType.Water, used);
                        float remaining = this.getReserveWaterAmount() - used;
                        this.setReserveWaterAmount(remaining);
                        this.sync();
                        return used;
                    }
                }
            }

            return used;
        }
    }

    public float transferFluidFrom(FluidContainer source, float amount) {
        if (source == null) {
            return 0.0F;
        } else {
            float sourceAvail = source.getAmount();
            float targetCapacity = this.getFluidCapacity() - this.getFluidAmount();
            float transferMax = Math.min(targetCapacity, sourceAvail);
            float used = PZMath.clamp(amount, 0.0F, transferMax);
            if (GameServer.server || !GameClient.client) {
                if (this.usesExternalWaterSource) {
                    if (this.isWaterInfinite()) {
                        source.removeFluid(used);
                        return used;
                    }

                    IsoObject object = this.checkExternalFluidSource();
                    if (object != null) {
                        object.transferFluidFrom(source, used);
                    }

                    return 0.0F;
                }

                float old = this.getFluidAmount();
                if (used > 0.0F) {
                    FluidContainer fc = this.getFluidContainer();
                    if (fc != null) {
                        fc.transferFrom(source, used);
                        this.sync();
                        LuaEventManager.triggerEvent("OnWaterAmountChange", this, old);
                    }
                }
            }

            return used;
        }
    }

    public FluidContainer moveFluidToTemporaryContainer(float amount) {
        float transferAmount = PZMath.clamp(amount, 0.0F, this.getFluidAmount());
        FluidContainer fluidCont = FluidContainer.CreateContainer();
        fluidCont.setCapacity(transferAmount);
        this.transferFluidTo(fluidCont, transferAmount);
        return fluidCont;
    }

    public String getFluidUiName() {
        if (this.usesExternalWaterSource) {
            IsoObject object = this.checkExternalFluidSource();
            if (object != null) {
                FluidContainer fc = object.getFluidContainer();
                if (fc != null && fc.getAmount() > 0.0F) {
                    FluidContainer tempFc = this.createSampleAndPurifyWater(fc, fc.getAmount());
                    String result = tempFc.getUiName();
                    FluidContainer.DisposeContainer(tempFc);
                    return result;
                } else {
                    return object.getFluidUiName();
                }
            } else {
                return Translator.getText("Fluid_Empty");
            }
        } else if (this.isWaterInfinite()) {
            return Fluid.Get(this.getInfiniteWaterType()).getTranslatedName();
        } else if (this.getFluidContainer() != null && this.getFluidContainer().getCapacity() > 0.0F) {
            return this.getFluidContainer().getUiName();
        } else if (this.isUnmovedPipedWaterSource() && this.hasReserveWater()) {
            return Fluid.Water.getTranslatedName();
        } else {
            return this.square != null
                    && !this.square.getProperties().has(IsoFlagType.water)
                    && this.sprite != null
                    && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                    && this.square.getPuddlesInGround() > 0.09F
                ? Fluid.TaintedWater.getTranslatedName()
                : Translator.getText("Fluid_Empty");
        }
    }

    public boolean hasFluid() {
        return this.square != null
                && this.sprite != null
                && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                && this.square.getPuddlesInGround() > 0.09F
            ? true
            : this.getFluidAmount() > 0.0F;
    }

    public boolean hasWater() {
        if (!this.hasFluid()) {
            return false;
        } else if (this.usesExternalWaterSource) {
            if (this.isWaterInfinite()) {
                return true;
            } else {
                IsoObject object = this.checkExternalFluidSource();
                return object != null ? object.hasWater() : false;
            }
        } else if (this.isWaterInfinite()) {
            return true;
        } else {
            FluidContainer fc = this.getFluidContainer();
            if (fc != null && fc.getAmount() > 0.0F) {
                return fc.isAllCategory(FluidCategory.Water);
            } else if (this.isUnmovedPipedWaterSource() && this.hasReserveWater()) {
                return this.getReserveWaterAmount() > 0.0F;
            } else {
                return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.taintedWater)
                    ? true
                    : this.square != null
                        && this.sprite != null
                        && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                        && this.square.getPuddlesInGround() > 0.09F;
            }
        }
    }

    public boolean isFluidInputLocked() {
        if (this.usesExternalWaterSource) {
            IsoObject object = this.checkExternalFluidSource();
            if (object != null) {
                return object.isFluidInputLocked();
            }
        }

        FluidContainer fc = this.getFluidContainer();
        return fc != null ? fc.isInputLocked() : true;
    }

    public boolean isTaintedWater() {
        if (this.usesExternalWaterSource) {
            if (this.isWaterInfinite()) {
                return false;
            } else {
                IsoObject object = this.checkExternalFluidSource();
                if (object != null) {
                    FluidContainer fc = object.getFluidContainer();
                    return fc != null && fc.getAmount() > 0.0F ? false : object.isTaintedWater();
                } else {
                    return false;
                }
            }
        } else if (this.getFluidContainer() != null && this.getFluidContainer().getAmount() > 0.0F && this.getFluidContainer().isTainted()) {
            return true;
        } else {
            return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.taintedWater)
                ? true
                : this.square != null
                    && this.sprite != null
                    && this.sprite.getProperties().has(IsoFlagType.solidfloor)
                    && this.square.getPuddlesInGround() > 0.09F;
        }
    }

    private boolean hasReserveWater() {
        return this.getReserveWaterAmount() > 0.0F;
    }

    private float getReserveWaterAmount() {
        if (this.getSquare() != null && this.getSquare().isNoWater()) {
            return 0.0F;
        } else if (this.hasModData() && this.getModData().rawget("waterAmount") != null) {
            return ((Double)this.getModData().rawget("waterAmount")).floatValue();
        } else {
            return this.sprite.properties.has("waterAmount") ? Float.parseFloat(this.sprite.getProperties().get("waterAmount")) : 0.0F;
        }
    }

    private float getReserveWaterMax() {
        if (this.hasModData() && this.getModData().rawget("waterMaxAmount") != null) {
            return ((Double)this.getModData().rawget("waterMaxAmount")).floatValue();
        } else if (this.sprite.properties.has("waterMaxAmount")) {
            return Float.parseFloat(this.sprite.getProperties().get("waterMaxAmount"));
        } else if (this.hasModData() && this.getModData().rawget("waterAmount") != null) {
            return ((Double)this.getModData().rawget("waterAmount")).floatValue();
        } else {
            return this.sprite.properties.has("waterAmount") ? Float.parseFloat(this.sprite.getProperties().get("waterAmount")) : 0.0F;
        }
    }

    private void setReserveWaterAmount(float amount) {
        if (this.getModData().rawget("waterMaxAmount") == null) {
            float currentAmount = this.getReserveWaterAmount();
            this.getModData().rawset("waterMaxAmount", (double)currentAmount);
        }

        amount = PZMath.clamp(amount, 0.0F, this.getReserveWaterMax());
        this.getModData().rawset("waterAmount", (double)amount);
        if (GameServer.server) {
            this.transmitModData();
        }
    }

    public InventoryItem replaceItem(InventoryItem item) {
        String replaceWith = null;
        InventoryItem itemCreated = null;
        if (item != null) {
            if (item.hasReplaceType(this.getObjectName())) {
                replaceWith = item.getReplaceType(this.getObjectName());
            } else if (item.hasReplaceType("WaterSource")) {
                replaceWith = item.getReplaceType("WaterSource");
            }
        }

        if (replaceWith != null) {
            itemCreated = item.getContainer().AddItem(InventoryItemFactory.CreateItem(replaceWith));
            if (item.getContainer().getParent() instanceof IsoGameCharacter isoGameCharacter) {
                if (isoGameCharacter.getPrimaryHandItem() == item) {
                    isoGameCharacter.setPrimaryHandItem(itemCreated);
                }

                if (isoGameCharacter.getSecondaryHandItem() == item) {
                    isoGameCharacter.setSecondaryHandItem(itemCreated);
                }
            }

            item.getContainer().Remove(item);
        }

        return itemCreated;
    }

    @Deprecated
    public void useItemOn(InventoryItem item) {
        String replaceWith = null;
        if (item != null) {
            if (item.hasReplaceType(this.getObjectName())) {
                replaceWith = item.getReplaceType(this.getObjectName());
            } else if (item.hasReplaceType("WaterSource")) {
                replaceWith = item.getReplaceType("WaterSource");
                this.useFluid(10.0F);
            }
        }

        if (replaceWith != null) {
            InventoryItem item2 = item.getContainer().AddItem(InventoryItemFactory.CreateItem(replaceWith));
            item.setUses(item.getUses() - 1);
            if (item.getUses() <= 0 && item.getContainer() != null) {
                item.getContainer().items.remove(item);
            }
        }
    }

    public boolean isCanPath() {
        return this.square != null
            && this.sprite != null
            && (this.sprite.getProperties().has(IsoFlagType.canPathW) || this.sprite.getProperties().has(IsoFlagType.canPathN));
    }

    @Override
    public float getX() {
        return this.square.getX();
    }

    @Override
    public float getY() {
        return this.square.getY();
    }

    @Override
    public float getZ() {
        return this.square.getZ();
    }

    public Vector3 getPosition(Vector3 out) {
        out.set(this.getX(), this.getY(), this.getZ());
        return out;
    }

    public Vector3f getPosition(Vector3f out) {
        out.set(this.getX(), this.getY(), this.getZ());
        return out;
    }

    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    public PropertyContainer getProperties() {
        return this.sprite == null ? null : this.sprite.getProperties();
    }

    public boolean hasProperty(IsoFlagType flag) {
        PropertyContainer properties = this.getProperties();
        return properties != null && properties.has(flag);
    }

    public boolean hasProperty(String p) {
        PropertyContainer properties = this.getProperties();
        return properties != null && properties.has(p);
    }

    public String getProperty(String p) {
        PropertyContainer properties = this.getProperties();
        return properties == null ? null : properties.get(p);
    }

    public boolean propertyEquals(String key, String value) {
        PropertyContainer properties = this.getProperties();
        return properties.propertyEquals(key, value);
    }

    public boolean propertyEqualsIgnoreCase(String key, String value) {
        PropertyContainer properties = this.getProperties();
        return properties.propertyEquals(key, value);
    }

    public void RemoveAttachedAnims() {
        if (this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance inst = this.attachedAnimSprite.get(i);
                inst.Dispose();
                IsoSpriteInstance.add(inst);
            }

            this.attachedAnimSprite.clear();
            if (PerformanceSettings.fboRenderChunk && Thread.currentThread() == GameWindow.gameThread) {
                this.invalidateRenderChunkLevel(256L);
            }
        }
    }

    public void RemoveAttachedAnim(int index) {
        if (this.attachedAnimSprite != null) {
            if (index >= 0 && index < this.attachedAnimSprite.size()) {
                this.attachedAnimSprite.get(index).Dispose();
                IsoSpriteInstance inst = this.attachedAnimSprite.remove(index);
                IsoSpriteInstance.add(inst);
                if (PerformanceSettings.fboRenderChunk && Thread.currentThread() == GameWindow.gameThread) {
                    this.invalidateRenderChunkLevel(256L);
                }
            }
        }
    }

    public void afterRotated() {
    }

    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else {
            PropertyContainer props = this.getProperties();
            if (props != null) {
                if (this.getType() == IsoObjectType.wall) {
                    if (props.has(IsoFlagType.collideN) && props.has(IsoFlagType.collideW)) {
                        return pos.set(this.getX(), this.getY());
                    }

                    if (props.has(IsoFlagType.collideN)) {
                        return pos.set(this.getX() + 0.5F, this.getY());
                    }

                    if (props.has(IsoFlagType.collideW)) {
                        return pos.set(this.getX(), this.getY() + 0.5F);
                    }

                    if (props.has(IsoFlagType.DoorWallN)) {
                        return pos.set(this.getX() + 0.5F, this.getY());
                    }

                    if (props.has(IsoFlagType.DoorWallW)) {
                        return pos.set(this.getX(), this.getY() + 0.5F);
                    }
                } else {
                    if (props.has(IsoFlagType.attachedN)) {
                        return pos.set(this.getX() + 0.5F, this.getY());
                    }

                    if (props.has(IsoFlagType.attachedS)) {
                        return pos.set(this.getX() + 0.5F, this.getY() + 1.0F);
                    }

                    if (props.has(IsoFlagType.attachedW)) {
                        return pos.set(this.getX(), this.getY() + 0.5F);
                    }

                    if (props.has(IsoFlagType.attachedE)) {
                        return pos.set(this.getX() + 1.0F, this.getY() + 0.5F);
                    }
                }
            }

            return pos.set(this.getX() + 0.5F, this.getY() + 0.5F);
        }
    }

    public Vector2 getFacingPositionAlt(Vector2 pos) {
        return this.getFacingPosition(pos);
    }

    public float getRenderYOffset() {
        return this.renderYOffset;
    }

    public void setRenderYOffset(float f) {
        this.renderYOffset = f;
        this.sx = 0.0F;
    }

    public boolean isTableSurface() {
        PropertyContainer props = this.getProperties();
        return props != null ? props.isTable() : false;
    }

    public boolean isTableTopObject() {
        PropertyContainer props = this.getProperties();
        return props != null ? props.isTableTop() : false;
    }

    public boolean getIsSurfaceNormalOffset() {
        PropertyContainer props = this.getProperties();
        return props != null ? props.isSurfaceOffset() : false;
    }

    public float getSurfaceNormalOffset() {
        float retval = 0.0F;
        PropertyContainer props = this.getProperties();
        if (props.isSurfaceOffset()) {
            retval = props.getSurface();
        }

        return retval;
    }

    public float getSurfaceOffsetNoTable() {
        float retval = 0.0F;
        int itemHeight = 0;
        PropertyContainer props = this.getProperties();
        if (props != null) {
            retval = props.getSurface();
            itemHeight = props.getItemHeight();
        }

        return retval + this.getRenderYOffset() + itemHeight;
    }

    public float getSurfaceOffset() {
        float retval = 0.0F;
        if (this.isTableSurface()) {
            PropertyContainer props = this.getProperties();
            if (props != null) {
                retval = props.getSurface();
            }
        }

        return retval;
    }

    public boolean isStairsNorth() {
        return this.getType() == IsoObjectType.stairsTN || this.getType() == IsoObjectType.stairsMN || this.getType() == IsoObjectType.stairsBN;
    }

    public boolean isStairsWest() {
        return this.getType() == IsoObjectType.stairsTW || this.getType() == IsoObjectType.stairsMW || this.getType() == IsoObjectType.stairsBW;
    }

    public boolean isStairsObject() {
        return this.isStairsNorth() || this.isStairsWest();
    }

    public boolean isHoppable() {
        return this.sprite != null && (this.sprite.getProperties().has(IsoFlagType.HoppableN) || this.sprite.getProperties().has(IsoFlagType.HoppableW));
    }

    public boolean isTallHoppable() {
        return this.sprite != null
            && (this.sprite.getProperties().has(IsoFlagType.TallHoppableN) || this.sprite.getProperties().has(IsoFlagType.TallHoppableW));
    }

    public boolean isNorthHoppable() {
        return this.sprite != null && this.isHoppable() && this.sprite.getProperties().has(IsoFlagType.HoppableN);
    }

    public boolean isWall() {
        return this.sprite != null && this.isWallN() || this.isWallW();
    }

    public boolean isWallN() {
        return this.sprite != null && (this.sprite.getProperties().has(IsoFlagType.WallN) || this.sprite.getProperties().has(IsoFlagType.WallNTrans));
    }

    public boolean isWallW() {
        return this.sprite != null && (this.sprite.getProperties().has(IsoFlagType.WallW) || this.sprite.getProperties().has(IsoFlagType.WallWTrans));
    }

    public boolean isWallSE() {
        return this.sprite != null && this.sprite.isWallSE();
    }

    public boolean haveSheetRope() {
        return IsoWindow.isTopOfSheetRopeHere(this.square, this.isNorthHoppable());
    }

    public int countAddSheetRope() {
        return IsoWindow.countAddSheetRope(this.square, this.isNorthHoppable());
    }

    public boolean canAddSheetRope() {
        return IsoWindow.canAddSheetRope(this.square, this.isNorthHoppable());
    }

    public boolean addSheetRope(IsoPlayer player, String itemType) {
        return !this.canAddSheetRope() ? false : IsoWindow.addSheetRope(player, this.square, this.isNorthHoppable(), itemType);
    }

    public boolean removeSheetRope(IsoPlayer player) {
        return this.haveSheetRope() ? IsoWindow.removeSheetRope(player, this.square, this.isNorthHoppable()) : false;
    }

    @Override
    public void setDoRender(boolean doRender) {
        this.doRender = doRender;
    }

    @Override
    public boolean getDoRender() {
        return this.doRender;
    }

    @Override
    public boolean isSceneCulled() {
        return this.isSceneCulled;
    }

    @Override
    public void setSceneCulled(boolean isCulled) {
        this.isSceneCulled = isCulled;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.getDoRender()) {
            if (!this.isSceneCulled()) {
                if (this.renderModel(x + 0.5F, y + 0.5F, z, col)) {
                    this.updateRenderInfoForObjectPicker(x, y, z, col);
                } else if (!this.isSpriteInvisible()) {
                    this.prepareToRender(col);
                    int playerIndex = IsoCamera.frameState.playerIndex;
                    if (this.shouldDrawMainSprite()) {
                        Texture oldTexture = this.checkClockTexture();
                        this.sprite.render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, stCol, !this.isBlink());
                        this.sprite.texture = oldTexture;
                        if (!PerformanceSettings.fboRenderChunk
                            && this.isOutlineHighlight(playerIndex)
                            && !this.isOutlineHlAttached(playerIndex)
                            && IsoObject.OutlineShader.instance.StartShader()) {
                            int rgba = GameServer.server ? -1 : this.outlineHighlightCol[playerIndex];
                            float r = Color.getRedChannelFromABGR(rgba);
                            float g = Color.getGreenChannelFromABGR(rgba);
                            float b = Color.getBlueChannelFromABGR(rgba);
                            IsoObject.OutlineShader.instance.setOutlineColor(r, g, b, this.isOutlineHlBlink(playerIndex) ? Core.blinkAlpha : 1.0F);
                            Texture texture = this.sprite.getTextureForCurrentFrame(this.dir);
                            if (texture != null) {
                                IsoObject.OutlineShader.instance.setStepSize(this.outlineThickness, texture.getWidthHW(), texture.getHeightHW());
                            }

                            this.sprite
                                .render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, stCol, !this.isBlink());
                            IndieGL.EndShader();
                        }
                    }

                    if (this.isSatChair()) {
                        bDoAttached = false;
                    }

                    this.renderAttachedAndOverlaySpritesInternal(this.dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, null);
                    if (!PerformanceSettings.fboRenderChunk
                        && this.isOutlineHighlight(playerIndex)
                        && this.isOutlineHlAttached(playerIndex)
                        && IsoObject.OutlineShader.instance.StartShader()) {
                        int rgba = GameServer.server ? -1 : this.outlineHighlightCol[playerIndex];
                        float r = Color.getRedChannelFromABGR(rgba);
                        float g = Color.getGreenChannelFromABGR(rgba);
                        float b = Color.getBlueChannelFromABGR(rgba);
                        IsoObject.OutlineShader.instance.setOutlineColor(r, g, b, this.isOutlineHlBlink(playerIndex) ? Core.blinkAlpha : 1.0F);
                        Texture texture = this.sprite.getTextureForCurrentFrame(this.dir);
                        if (texture != null) {
                            IsoObject.OutlineShader.instance.setStepSize(this.outlineThickness, texture.getWidthHW(), texture.getHeightHW());
                        }

                        if (this.getProperties().has(IsoFlagType.unlit)) {
                            stCol.r = 1.0F;
                            stCol.g = 1.0F;
                            stCol.b = 1.0F;
                        }

                        if (this.shouldDrawMainSprite()) {
                            this.sprite
                                .render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, stCol, !this.isBlink());
                        }

                        this.renderAttachedAndOverlaySpritesInternal(this.dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, null);
                        IndieGL.EndShader();
                    }

                    if (!this.alphaForced && this.isUpdateAlphaDuringRender()) {
                        this.updateAlpha(playerIndex);
                    }

                    this.debugRenderItemHeight(x, y, z);
                    this.debugRenderSurface(x, y, z);
                }
            }
        }
    }

    private void debugRenderItemHeight(float x, float y, float z) {
        if (DebugOptions.instance.isoSprite.itemHeight.getValue()) {
            if (this.square != null && IsoCamera.frameState.camCharacterSquare != null && this.square.z == IsoCamera.frameState.camCharacterSquare.z) {
                int ItemHeight = this.sprite.getProperties().getItemHeight();
                if (ItemHeight > 0) {
                    int Surface = 0;
                    if (this.sprite != null && this.sprite.getProperties().getSurface() > 0 && this.sprite.getProperties().isSurfaceOffset()) {
                        Surface = this.sprite.getProperties().getSurface();
                    }

                    LineDrawer.addRectYOffset(x, y, z, 1.0F, 1.0F, (int)this.getRenderYOffset() + Surface + ItemHeight, 0.66F, 0.66F, 0.66F);
                }
            }
        }
    }

    private void debugRenderSurface(float x, float y, float z) {
        if (DebugOptions.instance.isoSprite.surface.getValue()) {
            if (this.square != null && IsoCamera.frameState.camCharacterSquare != null && this.square.z == IsoCamera.frameState.camCharacterSquare.z) {
                int Surface = 0;
                if (this.sprite != null && this.sprite.getProperties().getSurface() > 0 && !this.sprite.getProperties().isSurfaceOffset()) {
                    Surface = this.sprite.getProperties().getSurface();
                }

                if (Surface > 0) {
                    LineDrawer.addRectYOffset(x, y, z, 1.0F, 1.0F, (int)this.getRenderYOffset() + Surface, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    }

    public void renderFloorTile(
        float x,
        float y,
        float z,
        ColorInfo col,
        boolean bDoAttached,
        boolean bWallLightingPass,
        Shader shader,
        Consumer<TextureDraw> texdModifier,
        Consumer<TextureDraw> attachedAndOverlayModifier
    ) {
        if (this.renderModel(x + 0.5F, y + 0.5F, z, col)) {
            this.updateRenderInfoForObjectPicker(x, y, z, col);
        } else if (!this.isSpriteInvisible()) {
            this.prepareToRender(col);
            boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
            FloorShaper floorShaper = Type.tryCastTo(texdModifier, FloorShaper.class);
            FloorShaper floorShaper2 = Type.tryCastTo(attachedAndOverlayModifier, FloorShaper.class);
            int playerIndex = IsoCamera.frameState.playerIndex;
            if ((floorShaper != null || floorShaper2 != null) && bHighlighted && this.getHighlightColor(playerIndex) != null) {
                ColorInfo ci = this.getHighlightColor(playerIndex);
                float a = ci.a * (this.isBlink() ? Core.blinkAlpha : 1.0F);
                int tintCol = Color.colorToABGR(ci.r, ci.g, ci.b, a);
                if (floorShaper != null) {
                    floorShaper.setTintColor(tintCol);
                }

                if (floorShaper2 != null) {
                    floorShaper2.setTintColor(tintCol);
                }
            }

            if (this.shouldDrawMainSprite()) {
                if (this == this.square.getFloor()) {
                    FBORenderCell.instance.renderSeamFix1_Floor(this, x, y, z, stCol, texdModifier);
                    FBORenderCell.instance.renderSeamFix2_Floor(this, x, y, z, stCol, texdModifier);
                }

                if (!PerformanceSettings.fboRenderChunk && this.square.getWater() != null && this.square.getWater().isbShore()) {
                    IndieGL.glBlendFunc(770, 771);
                }

                this.sprite
                    .render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, stCol, !this.isBlink(), texdModifier);
            }

            this.renderAttachedAndOverlaySpritesInternal(this.dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, attachedAndOverlayModifier);
            if (floorShaper != null) {
                floorShaper.setTintColor(0);
            }

            if (floorShaper2 != null) {
                floorShaper2.setTintColor(0);
            }
        }
    }

    public void renderWallTile(
        IsoDirections dir,
        float x,
        float y,
        float z,
        ColorInfo col,
        boolean bDoAttached,
        boolean bWallLightingPass,
        Shader shader,
        Consumer<TextureDraw> texdModifier
    ) {
        if (this.renderModel(x + 0.5F, y + 0.5F, z, col)) {
            this.updateRenderInfoForObjectPicker(x, y, z, col);
            this.sx = 0.0F;
        } else if (!this.isSpriteInvisible()) {
            this.renderWallTileOnly(dir, x, y, z, col, shader, texdModifier);
            this.renderAttachedAndOverlaySpritesInternal(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
            int playerIndex = IsoCamera.frameState.playerIndex;
            if (!PerformanceSettings.fboRenderChunk
                && this.isOutlineHighlight(playerIndex)
                && !this.isOutlineHlAttached(playerIndex)
                && IsoObject.OutlineShader.instance.StartShader()) {
                int rgba = GameServer.server ? -1 : this.outlineHighlightCol[playerIndex];
                float r = Color.getRedChannelFromABGR(rgba);
                float g = Color.getGreenChannelFromABGR(rgba);
                float b = Color.getBlueChannelFromABGR(rgba);
                IsoObject.OutlineShader.instance.setOutlineColor(r, g, b, this.isOutlineHlBlink(playerIndex) ? Core.blinkAlpha : 1.0F);
                Texture texture = this.sprite.getTextureForCurrentFrame(this.dir);
                if (texture != null) {
                    IsoObject.OutlineShader.instance.setStepSize(this.outlineThickness, texture.getWidthHW(), texture.getHeightHW());
                }

                this.sprite.render(this, x, y, z, dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, stCol, !this.isBlink());
                IndieGL.EndShader();
            }
        }
    }

    public void renderWallTileDepth(
        IsoDirections dir,
        boolean cutawaySelf,
        boolean cutawayE,
        boolean cutawayS,
        int cutawaySEX,
        float x,
        float y,
        float z,
        ColorInfo col,
        Shader shader,
        Consumer<TextureDraw> texdModifier
    ) {
        if (!this.isSpriteInvisible()) {
            this.prepareToRender(col);
            boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
            WallShaper wallShaper = Type.tryCastTo(texdModifier, WallShaper.class);
            int playerIndex = IsoCamera.frameState.playerIndex;
            if (wallShaper != null && bHighlighted && this.getHighlightColor(playerIndex) != null) {
                ColorInfo ci = this.getHighlightColor(playerIndex);
                float a = ci.a * (this.isBlink() ? Core.blinkAlpha : 1.0F);
                int tintCol = Color.colorToABGR(ci.r, ci.g, ci.b, a);
                wallShaper.setTintColor(tintCol);
            }

            if (this.shouldDrawMainSprite()) {
                this.sprite
                    .renderDepth(
                        this,
                        dir,
                        cutawaySelf,
                        cutawayE,
                        cutawayS,
                        cutawaySEX,
                        x,
                        y,
                        z,
                        this.offsetX,
                        this.offsetY + this.renderYOffset * Core.tileScale,
                        stCol,
                        !this.isBlink(),
                        texdModifier
                    );
            }

            if (wallShaper != null) {
                wallShaper.setTintColor(0);
            }
        }
    }

    public void renderWallTileOnly(IsoDirections dir, float x, float y, float z, ColorInfo col, Shader shader, Consumer<TextureDraw> texdModifier) {
        if (!this.isSpriteInvisible()) {
            this.prepareToRender(col);
            boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
            int playerIndex = IsoCamera.frameState.playerIndex;
            WallShaper wallShaper = Type.tryCastTo(texdModifier, WallShaper.class);
            if (wallShaper != null && bHighlighted && this.getHighlightColor(playerIndex) != null) {
                ColorInfo ci = this.getHighlightColor(playerIndex);
                float a = ci.a * (this.isBlink() ? Core.blinkAlpha : 1.0F);
                int tintCol = Color.colorToABGR(ci.r, ci.g, ci.b, a);
                wallShaper.setTintColor(tintCol);
            }

            if (this.shouldDrawMainSprite()) {
                if (shader != null) {
                    if (PerformanceSettings.fboRenderChunk && shader == IsoGridSquare.CircleStencilShader.instance) {
                        float farDepthZ = IsoDepthHelper.getSquareDepthData(
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x, y, z
                            )
                            .depthStart;
                        float zPlusOne = z + 1.0F;
                        float frontDepthZ = IsoDepthHelper.getSquareDepthData(
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                                x + 1.0F,
                                y + 1.0F,
                                zPlusOne
                            )
                            .depthStart;
                        if (!FBORenderCell.instance.renderTranslucentOnly) {
                            int CPW = 8;
                            IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                                PZMath.fastfloor(x / 8.0F),
                                PZMath.fastfloor(y / 8.0F),
                                PZMath.fastfloor(z)
                            );
                            float chunkDepth = result.depthStart;
                            farDepthZ -= chunkDepth;
                            frontDepthZ -= chunkDepth;
                        }

                        ShaderUniformSetter uniforms = ShaderUniformSetter.uniform1f(shader, "zDepthBlendZ", frontDepthZ);
                        uniforms.setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendToZ", farDepthZ));
                        IndieGL.pushShader(shader, uniforms);
                    } else {
                        IndieGL.pushShader(shader);
                    }
                }

                if (PerformanceSettings.fboRenderChunk && !FBORenderCell.instance.renderTranslucentOnly && shader != IsoGridSquare.CircleStencilShader.instance
                    )
                 {
                    FBORenderCell.instance.renderSeamFix1_Wall(this, x, y, z, stCol, texdModifier);
                    FBORenderCell.instance.renderSeamFix2_Wall(this, x, y, z, stCol, texdModifier);
                }

                this.sprite.render(this, x, y, z, dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, stCol, !this.isBlink(), texdModifier);
                if (shader != null) {
                    IndieGL.popShader(shader);
                }
            }

            if (wallShaper != null) {
                wallShaper.setTintColor(0);
            }
        }
    }

    private boolean shouldDrawMainSprite() {
        return this.sprite == null ? false : DebugOptions.instance.terrain.renderTiles.renderSprites.getValue();
    }

    public void renderAttachedAndOverlaySprites(
        IsoDirections dir,
        float x,
        float y,
        float z,
        ColorInfo col,
        boolean bDoAttached,
        boolean bWallLightingPass,
        Shader shader,
        Consumer<TextureDraw> texdModifier
    ) {
        if (!this.isSpriteInvisible()) {
            this.renderAttachedAndOverlaySpritesInternal(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
        }
    }

    private void renderAttachedAndOverlaySpritesInternal(
        IsoDirections dir,
        float x,
        float y,
        float z,
        ColorInfo col,
        boolean bDoAttached,
        boolean bWallLightingPass,
        Shader shader,
        Consumer<TextureDraw> texdModifier
    ) {
        boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
        if (bHighlighted) {
            col = stCol;
        }

        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
            float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
            col = stCol.set(col.r * fade, col.g * fade, col.b * fade, stCol.a);
        }

        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue() && !bHighlighted) {
            col.set(1.0F, 1.0F, 1.0F, col.a);
        }

        this.renderOverlaySprites(x, y, z, col, shader, texdModifier);
        if (bDoAttached) {
            this.renderAttachedSprites(dir, x, y, z, col, bWallLightingPass, shader, texdModifier);
        }
    }

    private void prepareToRender(ColorInfo col) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        stCol.set(col);
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
            float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
            stCol.set(col.r * fade, col.g * fade, col.b * fade, stCol.a);
        }

        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            stCol.set(1.0F, 1.0F, 1.0F, stCol.a);
        }

        boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
        if (bHighlighted) {
            ColorInfo highlightColor = this.getHighlightColor(playerIndex);
            stCol.a = highlightColor.a;
            if (this.isBlink()) {
                stCol.a = stCol.a * Core.blinkAlpha;
            }

            float normalColorR = col.r * (this.customColor == null ? 1.0F : this.customColor.r);
            float normalColorG = col.g * (this.customColor == null ? 1.0F : this.customColor.g);
            float normalColorB = col.b * (this.customColor == null ? 1.0F : this.customColor.b);
            stCol.r = normalColorR * (1.0F - stCol.a) + highlightColor.r * stCol.a;
            stCol.g = normalColorG * (1.0F - stCol.a) + highlightColor.g * stCol.a;
            stCol.b = normalColorB * (1.0F - stCol.a) + highlightColor.b * stCol.a;
            stCol.a = col.a;
        } else if (this.customColor != null) {
            stCol.r = stCol.r * this.customColor.r;
            stCol.g = stCol.g * this.customColor.g;
            stCol.b = stCol.b * this.customColor.b;
        }

        if (this.sprite != null && this.sprite.forceAmbient && !DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            float rmod = IsoObject.rmod * this.tintr;
            float gmod = IsoObject.gmod * this.tintg;
            float bmod = IsoObject.bmod * this.tintb;
            if (!bHighlighted) {
                stCol.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * rmod;
                stCol.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * gmod;
                stCol.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * bmod;
            }
        }

        float CamCharacterX = IsoCamera.frameState.camCharacterX;
        float CamCharacterY = IsoCamera.frameState.camCharacterY;
        float CamCharacterZ = IsoCamera.frameState.camCharacterZ;
        if (IsoWorld.instance.currentCell.IsPlayerWindowPeeking(playerIndex)) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            IsoDirections playerDir = IsoDirections.fromAngle(player.getForwardDirection());
            if (playerDir == IsoDirections.N || playerDir == IsoDirections.NW) {
                CamCharacterY--;
            }

            if (playerDir == IsoDirections.W || playerDir == IsoDirections.NW) {
                CamCharacterX--;
            }
        }

        if (this == IsoCamera.getCameraCharacter()) {
            this.setAlphaAndTarget(playerIndex, 1.0F);
        }

        lastRenderedRendered = lastRendered;
        lastRendered = this;
        if (this.sprite != null && !(this instanceof IsoPhysicsObject) && IsoCamera.getCameraCharacter() != null) {
            boolean forceOpaque = this instanceof IsoWindow || this.sprite.getType() == IsoObjectType.doorW || this.sprite.getType() == IsoObjectType.doorN;
            if (this.sprite.getProperties().has("GarageDoor")) {
                forceOpaque = false;
            }

            if (!forceOpaque
                && (this.square.getX() > CamCharacterX || this.square.getY() > CamCharacterY)
                && PZMath.fastfloor(CamCharacterZ) <= this.square.getZ()) {
                boolean bCut = false;
                float reducedAlpha = 0.2F;
                boolean bIsCutWest = (this.sprite.cutW || this.sprite.getProperties().has(IsoFlagType.doorW)) && this.square.getX() > CamCharacterX;
                boolean bIsCutNorth = (this.sprite.cutN || this.sprite.getProperties().has(IsoFlagType.doorN)) && this.square.getY() > CamCharacterY;
                if (bIsCutWest && this.square.getProperties().has(IsoFlagType.WallSE) && this.square.getY() <= CamCharacterY) {
                    bIsCutWest = false;
                }

                if (!bIsCutWest && !bIsCutNorth) {
                    boolean bIsRoof = this.getType() == IsoObjectType.WestRoofB
                        || this.getType() == IsoObjectType.WestRoofM
                        || this.getType() == IsoObjectType.WestRoofT;
                    boolean bIsValidOverhang = bIsRoof && PZMath.fastfloor(CamCharacterZ) == this.square.getZ() && this.square.getBuilding() == null;
                    if (bIsValidOverhang && IsoWorld.instance.currentCell.CanBuildingSquareOccludePlayer(this.square, playerIndex)) {
                        bCut = true;
                        reducedAlpha = 0.05F;
                    }
                } else {
                    bCut = true;
                }

                if (this.sprite.getProperties().has(IsoFlagType.halfheight)) {
                    bCut = false;
                }

                if (bCut) {
                    if (bIsCutNorth && this.sprite.getProperties().has(IsoFlagType.HoppableN)) {
                        reducedAlpha = 0.25F;
                    }

                    if (bIsCutWest && this.sprite.getProperties().has(IsoFlagType.HoppableW)) {
                        reducedAlpha = 0.25F;
                    }

                    if (!PerformanceSettings.fboRenderChunk) {
                        if (this.alphaForced) {
                            if (this.getTargetAlpha(playerIndex) == 1.0F) {
                                this.setAlphaAndTarget(playerIndex, 0.99F);
                            }
                        } else {
                            this.setTargetAlpha(playerIndex, reducedAlpha);
                        }
                    }

                    lowLightingQualityHack = true;
                    this.noPicking = this.rerouteMask == null
                        && !(this instanceof IsoThumpable)
                        && !(this instanceof IsoWindowFrame)
                        && !this.sprite.getProperties().has(IsoFlagType.doorN)
                        && !this.sprite.getProperties().has(IsoFlagType.doorW)
                        && !this.sprite.getProperties().has(IsoFlagType.HoppableN)
                        && !this.sprite.getProperties().has(IsoFlagType.HoppableW);
                } else {
                    this.noPicking = false;
                }
            } else {
                this.noPicking = false;
            }
        }

        if (this == IsoCamera.getCameraCharacter()) {
            this.setTargetAlpha(playerIndex, 1.0F);
        }
    }

    protected float getAlphaUpdateRateDiv() {
        return 14.0F;
    }

    protected float getAlphaUpdateRateMul() {
        float mul = 0.25F;
        if (this.square != null && this.square.room != null) {
            mul *= 2.0F;
        }

        return mul;
    }

    protected boolean isUpdateAlphaEnabled() {
        return true;
    }

    protected boolean isUpdateAlphaDuringRender() {
        return true;
    }

    protected final void updateAlpha() {
        if (!(this instanceof IsoAnimal)) {
            if (!GameServer.server) {
                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    if (IsoPlayer.players[i] != null) {
                        this.updateAlpha(i);
                    }
                }
            }
        }
    }

    protected final void updateAlpha(int playerIndex) {
        if (!GameServer.server) {
            float mul = this.getAlphaUpdateRateMul();
            float div = this.getAlphaUpdateRateDiv();
            this.updateAlpha(playerIndex, mul, div);
        }
    }

    protected void updateAlpha(int playerIndex, float mul, float div) {
        if (this.isUpdateAlphaEnabled()) {
            if (!DebugOptions.instance.character.debug.updateAlpha.getValue()) {
                this.setAlphaToTarget(playerIndex);
            } else {
                if (this.neverDoneAlpha) {
                    this.setAlpha(0.0F);
                    this.neverDoneAlpha = false;
                }

                if (DebugOptions.instance.character.debug.updateAlphaEighthSpeed.getValue()) {
                    mul /= 8.0F;
                    div *= 8.0F;
                }

                float timeBasedMultiplier = GameTime.getInstance().getMultiplier();
                float alphaStep = timeBasedMultiplier * 0.28F;
                float alpha = this.getAlpha(playerIndex);
                float targetAlpha = GameServer.server ? 1.0F : this.targetAlpha[playerIndex];
                if (alpha < targetAlpha) {
                    alpha += alphaStep * mul;
                    if (alpha > targetAlpha) {
                        alpha = targetAlpha;
                    }
                } else if (alpha > targetAlpha) {
                    alpha -= alphaStep / div;
                    if (alpha < targetAlpha) {
                        alpha = targetAlpha;
                    }
                }

                this.setAlpha(playerIndex, alpha);
            }
        }
    }

    private void renderOverlaySprites(float x, float y, float z, ColorInfo col, Shader shader, Consumer<TextureDraw> texdModifier) {
        if (this.getOverlaySprite() != null && DebugOptions.instance.terrain.renderTiles.overlaySprites.getValue()) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glDepthMask(true);
            }

            ColorInfo newColor = stCol2;
            newColor.set(col);
            if (this.overlaySpriteColor != null) {
                newColor.set(this.overlaySpriteColor);
                if (!DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                    int playerIndex = IsoPlayer.getPlayerIndex();
                    ColorInfo lighting = this.getSquare().getLightInfo(playerIndex);
                    if (lighting != null) {
                        newColor.r = newColor.r * lighting.r;
                        newColor.g = newColor.g * lighting.g;
                        newColor.b = newColor.b * lighting.b;
                    }
                }
            }

            if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
                float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
                newColor.set(newColor.r * fade, newColor.g * fade, newColor.b * fade, newColor.a);
            }

            if (texdModifier != CutawayAttachedModifier.instance) {
                texdModifier = null;
            }

            if (!GameServer.server && newColor.a != 1.0F && this.overlaySprite.def != null && this.overlaySprite.def.copyTargetAlpha) {
                int playerIndex = IsoPlayer.getPlayerIndex();
                float alpha1 = this.alpha[playerIndex];
                this.alpha[playerIndex] = this.alpha[playerIndex] * newColor.a;
                this.getOverlaySprite()
                    .render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, newColor, true, texdModifier);
                this.alpha[playerIndex] = alpha1;
            } else {
                this.getOverlaySprite()
                    .render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, newColor, true, texdModifier);
            }

            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glDepthMask(true);
            }
        }
    }

    private void renderAttachedSprites(
        IsoDirections dir, float x, float y, float z, ColorInfo col, boolean bWallLightingPass, Shader shader, Consumer<TextureDraw> texdModifier
    ) {
        if (PerformanceSettings.fboRenderChunk) {
            boolean bFloor = this.sprite != null && this.sprite.solidfloor;
            IndieGL.glDepthMask(!bFloor);
        }

        if (this.attachedAnimSprite != null && DebugOptions.instance.terrain.renderTiles.attachedAnimSprites.getValue()) {
            int n = this.attachedAnimSprite.size();

            for (int i = 0; i < n; i++) {
                IsoSpriteInstance s = this.attachedAnimSprite.get(i);
                if (!bWallLightingPass || !s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) {
                    float fa = col.a;
                    IndieGL.shaderSetValue(shader, "floorLayer", 1);
                    col.a = s.alpha;
                    Consumer<TextureDraw> texdModifier1 = texdModifier;
                    if (texdModifier == WallShaperW.instance) {
                        if (s.parentSprite.getProperties().has(IsoFlagType.attachedN)) {
                            Texture tex = s.parentSprite.getTextureForCurrentFrame(dir);
                            if (tex != null && tex.getWidth() < 32 * Core.tileScale) {
                                continue;
                            }
                        }

                        if (s.parentSprite.getProperties().has(IsoFlagType.attachedW)) {
                            texdModifier1 = WallShaperWhole.instance;
                        }
                    } else if (texdModifier == WallShaperN.instance) {
                        if (s.parentSprite.getProperties().has(IsoFlagType.attachedW)) {
                            continue;
                        }

                        if (s.parentSprite.getProperties().has(IsoFlagType.attachedN)) {
                            texdModifier1 = WallShaperWhole.instance;
                        }
                    }

                    s.parentSprite.render(s, this, x, y, z, dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, col, true, texdModifier1);
                    col.a = fa;
                    s.update();
                }
            }
        }

        if (this.children != null && DebugOptions.instance.terrain.renderTiles.attachedChildren.getValue()) {
            int n = this.children.size();

            for (int ix = 0; ix < n; ix++) {
                IsoObject obj = this.children.get(ix);
                if (obj instanceof IsoMovingObject) {
                    IndieGL.shaderSetValue(shader, "floorLayer", 1);
                    obj.render(obj.getX(), obj.getY(), obj.getZ(), col, true, false, null);
                }
            }
        }

        if (this.wallBloodSplats != null && DebugOptions.instance.terrain.renderTiles.attachedWallBloodSplats.getValue()) {
            if (Core.getInstance().getOptionBloodDecals() == 0) {
                if (PerformanceSettings.fboRenderChunk) {
                    IndieGL.glDepthMask(true);
                }

                return;
            }

            if (!FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare()) && !DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                col = this.getSquare().getLightInfo(IsoCamera.frameState.playerIndex);
            }

            IndieGL.shaderSetValue(shader, "floorLayer", 0);

            for (int ixx = 0; ixx < this.wallBloodSplats.size(); ixx++) {
                this.wallBloodSplats.get(ixx).render(x, y, z, col, texdModifier);
            }
        }

        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.glDepthMask(true);
        }
    }

    public boolean isSpriteInvisible() {
        return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.invisible);
    }

    public void renderFxMask(float x, float y, float z, boolean bDoAttached) {
        if (this.sprite != null) {
            this.sprite.render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, colFxMask, false);
        }

        if (this.getOverlaySprite() != null) {
            this.getOverlaySprite().render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, colFxMask, false);
        }

        if (bDoAttached) {
            if (this.attachedAnimSprite != null) {
                int n = this.attachedAnimSprite.size();

                for (int i = 0; i < n; i++) {
                    IsoSpriteInstance s = this.attachedAnimSprite.get(i);
                    s.render(this, x, y, z, this.dir, this.offsetX, this.offsetY + this.renderYOffset * Core.tileScale, colFxMask);
                }
            }

            if (this.children != null) {
                int n = this.children.size();

                for (int i = 0; i < n; i++) {
                    IsoObject obj = this.children.get(i);
                    if (obj instanceof IsoMovingObject) {
                        obj.render(obj.getX(), obj.getY(), obj.getZ(), colFxMask, bDoAttached, false, null);
                    }
                }
            }

            if (this.wallBloodSplats != null) {
                if (Core.getInstance().getOptionBloodDecals() == 0) {
                    return;
                }

                for (int ix = 0; ix < this.wallBloodSplats.size(); ix++) {
                    this.wallBloodSplats.get(ix).render(x, y, z, colFxMask, null);
                }
            }
        }
    }

    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
        if (this.sprite != null) {
            if (!this.sprite.getProperties().has(IsoFlagType.invisible)) {
                this.sprite.renderObjectPicker(this.sprite.def, this, this.dir);
            }
        }
    }

    public boolean TestPathfindCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        return false;
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        return false;
    }

    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.Unblocked;
    }

    public Texture getCurrentFrameTex() {
        return this.sprite == null ? null : this.sprite.getTextureForCurrentFrame(this.dir);
    }

    public boolean isMaskClicked(int x, int y) {
        return this.sprite == null ? false : this.sprite.isMaskClicked(this.dir, x, y);
    }

    public boolean isMaskClicked(int x, int y, boolean flip) {
        if (this.sprite == null) {
            return false;
        } else {
            return this.overlaySprite != null && this.overlaySprite.isMaskClicked(this.dir, x, y, flip)
                ? true
                : this.sprite.isMaskClicked(this.dir, x, y, flip);
        }
    }

    public float getMaskClickedY(int x, int y, boolean flip) {
        return this.sprite == null ? 10000.0F : this.sprite.getMaskClickedY(this.dir, x, y, flip);
    }

    public ColorInfo getCustomColor() {
        return this.customColor;
    }

    public void setCustomColor(ColorInfo col) {
        this.customColor = col;
        if (Thread.currentThread() == GameWindow.gameThread) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    public void setCustomColor(float r, float g, float b, float a) {
        ColorInfo col = new ColorInfo(r, g, b, a);
        this.setCustomColor(col);
    }

    public void loadFromRemoteBuffer(ByteBuffer b) {
        this.loadFromRemoteBuffer(b, true);
    }

    public void loadFromRemoteBuffer(ByteBuffer b, boolean addToObjects) {
        try {
            this.load(b, 241);
        } catch (IOException var12) {
            var12.printStackTrace();
            return;
        }

        if (this instanceof IsoWorldInventoryObject && ((IsoWorldInventoryObject)this).getItem() == null) {
            DebugLog.log("loadFromRemoteBuffer() failed due to an unknown item type");
        } else {
            int x = b.getInt();
            int y = b.getInt();
            int z = b.getInt();
            int objIndex = b.getInt();
            boolean spec = b.get() != 0;
            boolean world = b.get() != 0;
            IsoWorld.instance.currentCell.EnsureSurroundNotNull(x, y, z);
            this.square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (this.square != null) {
                if (GameServer.server && !(this instanceof IsoWorldInventoryObject)) {
                    IsoRegions.setPreviousFlags(this.square);
                }

                if (spec) {
                    this.square.getSpecialObjects().add(this);
                }

                if (world && this instanceof IsoWorldInventoryObject worldObj) {
                    this.square.getWorldObjects().add(worldObj);
                }

                if (addToObjects) {
                    if (objIndex >= 0 && objIndex <= this.square.getObjects().size()) {
                        this.square.getObjects().add(objIndex, this);
                    } else {
                        this.square.getObjects().add(this);
                    }
                }

                for (int i = 0; i < this.getContainerCount(); i++) {
                    ItemContainer container = this.getContainerByIndex(i);
                    container.parent = this;
                    container.parent.square = this.square;
                    container.sourceGrid = this.square;
                }

                for (int xx = -1; xx <= 1; xx++) {
                    for (int yy = -1; yy <= 1; yy++) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(xx + x, yy + y, z);
                        if (sq != null) {
                            sq.RecalcAllWithNeighbours(true);
                        }
                    }
                }
            }
        }
    }

    protected boolean hasObjectAmbientEmitter() {
        IsoChunk chunk = this.getChunk();
        return chunk == null ? false : chunk.hasObjectAmbientEmitter(this);
    }

    protected void addObjectAmbientEmitter(ObjectAmbientEmitters.PerObjectLogic logic) {
        IsoChunk chunk = this.getChunk();
        if (chunk != null) {
            chunk.addObjectAmbientEmitter(this, logic);
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.createContainersFromSpriteProperties();

        for (int i = 0; i < this.getContainerCount(); i++) {
            ItemContainer container = this.getContainerByIndex(i);
            container.addItemsToProcessItems();
        }

        if (!GameServer.server) {
            ItemContainer container = this.getContainerByEitherType("fridge", "freezer");
            if (container != null && container.isPowered()) {
                this.addObjectAmbientEmitter(new ObjectAmbientEmitters.FridgeHumLogic().init(this));
                IsoWorld.instance.getCell().addToProcessIsoObject(this);
            } else if (this.sprite != null && this.sprite.getProperties().has(IsoFlagType.waterPiped) && this.getFluidAmount() > 0.0F && Rand.Next(15) == 0) {
                this.addObjectAmbientEmitter(new ObjectAmbientEmitters.WaterDripLogic().init(this));
            } else if (this.sprite == null
                || this.sprite.getName() == null
                || !this.sprite.getName().startsWith("camping_01")
                || this.sprite.tileSheetIndex != 0 && this.sprite.tileSheetIndex != 3) {
                if (!(this instanceof IsoDoor) && !(this instanceof IsoWindow) && this instanceof IsoTree && Rand.Next(40) == 0) {
                    this.addObjectAmbientEmitter(new ObjectAmbientEmitters.TreeAmbianceLogic().init(this));
                }
            } else {
                this.addObjectAmbientEmitter(new ObjectAmbientEmitters.TentAmbianceLogic().init(this));
            }

            PropertyContainer props = this.getProperties();
            if (props != null && props.has("AmbientSound")) {
                this.addObjectAmbientEmitter(new ObjectAmbientEmitters.AmbientSoundLogic().init(this));
            }

            this.checkMoveWithWind();
            this.addLightSourceToWorld();
            this.flagForHotSave();
        }
    }

    @Override
    public void removeFromWorld() {
        this.removeFromWorld(this.removeFromWorldToMeta);
        IsoCell cell = this.getCell();
        cell.addToProcessIsoObjectRemove(this);
        cell.getStaticUpdaterObjectList().remove(this);

        for (int i = 0; i < this.getContainerCount(); i++) {
            ItemContainer container = this.getContainerByIndex(i);
            container.removeItemsFromProcessItems();
        }

        if (this.emitter != null) {
            this.emitter.stopAll();
            this.emitter = null;
        }

        if (this.getChunk() != null) {
            this.getChunk().removeObjectAmbientEmitter(this);
        }

        this.removeLightSourceFromWorld();
        this.clearOnOverlay();
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderObjectHighlight.getInstance().unregisterObject(this);
            FBORenderObjectOutline.getInstance().unregisterObject(this);
        }

        this.flagForHotSave();
    }

    public final void removeFromWorldToMeta() {
        try {
            this.removeFromWorldToMeta = true;
            this.removeFromWorld();
        } finally {
            this.removeFromWorldToMeta = false;
        }
    }

    public void reuseGridSquare() {
    }

    public void removeFromSquare() {
        if (this.square != null) {
            this.square.getObjects().remove(this);
            this.square.getSpecialObjects().remove(this);
        }
    }

    public void transmitCustomColorToClients() {
        if (GameServer.server && this.getCustomColor() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SendCustomColor, this.getSquare().x, this.getSquare().y, this);
        }
    }

    public void transmitCompleteItemToClients() {
        if (GameServer.server) {
            if (GameServer.udpEngine == null) {
                return;
            }

            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddItemToMap, this.square.x, this.square.y, this);
        }
    }

    public void transmitUpdatedSpriteToClients(UdpConnection connection) {
        if (GameServer.server) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c != null
                    && this.square != null
                    && (connection == null || c.getConnectedGUID() != connection.getConnectedGUID())
                    && c.RelevantTo(this.square.x, this.square.y)) {
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.UpdateItemSprite.doPacket(b);
                    b.putInt(this.getSprite().id);
                    GameWindow.WriteStringUTF(b.bb, this.spriteName);
                    b.putInt(this.getSquare().getX());
                    b.putInt(this.getSquare().getY());
                    b.putInt(this.getSquare().getZ());
                    b.putInt(this.getSquare().getObjects().indexOf(this));
                    if (this.attachedAnimSprite != null) {
                        b.putByte((byte)this.attachedAnimSprite.size());

                        for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                            IsoSpriteInstance aas = this.attachedAnimSprite.get(i);
                            b.putInt(aas.parentSprite.id);
                        }
                    } else {
                        b.putByte((byte)0);
                    }

                    PacketTypes.PacketType.UpdateItemSprite.send(c);
                }
            }
        }
    }

    public void transmitUpdatedSpriteToClients() {
        this.transmitUpdatedSpriteToClients(null);
    }

    public void transmitUpdatedSprite() {
        if (GameClient.client) {
            this.transmitUpdatedSpriteToServer();
        }

        if (GameServer.server) {
            this.transmitUpdatedSpriteToClients();
        }
    }

    public void sendObjectChange(String change) {
        if (GameServer.server) {
            GameServer.sendObjectChange(this, change, (KahluaTable)null);
        } else if (GameClient.client) {
            DebugLog.log("sendObjectChange() can only be called on the server");
        } else {
            SinglePlayerServer.sendObjectChange(this, change, (KahluaTable)null);
        }
    }

    public void sendObjectChange(String change, KahluaTable tbl) {
        if (GameServer.server) {
            GameServer.sendObjectChange(this, change, tbl);
        } else if (GameClient.client) {
            DebugLog.log("sendObjectChange() can only be called on the server");
        } else {
            SinglePlayerServer.sendObjectChange(this, change, tbl);
        }
    }

    public void sendObjectChange(String change, Object... args) {
        if (GameServer.server) {
            GameServer.sendObjectChange(this, change, args);
        } else if (GameClient.client) {
            DebugLog.log("sendObjectChange() can only be called on the server");
        } else {
            SinglePlayerServer.sendObjectChange(this, change, args);
        }
    }

    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("containers".equals(change)) {
            bb.put((byte)this.getContainerCount());

            for (int i = 0; i < this.getContainerCount(); i++) {
                ItemContainer container = this.getContainerByIndex(i);

                try {
                    container.save(bb);
                } catch (Throwable var8) {
                    ExceptionLogger.logException(var8);
                }
            }
        } else if ("container.customTemperature".equals(change)) {
            if (this.getContainer() != null) {
                bb.putFloat(this.getContainer().getCustomTemperature());
            } else {
                bb.putFloat(0.0F);
            }
        } else if ("name".equals(change)) {
            GameWindow.WriteStringUTF(bb, this.getName());
        } else if ("replaceWith".equals(change)) {
            if (tbl != null && tbl.rawget("object") instanceof IsoObject object) {
                try {
                    object.save(bb);
                } catch (IOException var7) {
                    var7.printStackTrace();
                }
            }
        } else if ("usesExternalWaterSource".equals(change)) {
            boolean value = tbl != null && Boolean.TRUE.equals(tbl.rawget("value"));
            bb.put((byte)(value ? 1 : 0));
        } else if ("sprite".equals(change)) {
            if (this.sprite == null) {
                bb.putInt(0);
            } else {
                bb.putInt(this.sprite.id);
                GameWindow.WriteStringUTF(bb, this.spriteName);
            }
        }
    }

    public void loadChange(String change, ByteBuffer bb) {
        if ("containers".equals(change)) {
            for (int i = 0; i < this.getContainerCount(); i++) {
                ItemContainer container = this.getContainerByIndex(i);
                container.removeItemsFromProcessItems();
                container.removeAllItems();
            }

            this.removeAllContainers();
            int count = bb.get();

            for (int i = 0; i < count; i++) {
                ItemContainer container = new ItemContainer();
                container.id = 0;
                container.parent = this;
                container.sourceGrid = this.square;

                try {
                    container.load(bb, 241);
                    if (i == 0) {
                        if (this instanceof IsoDeadBody) {
                            container.capacity = 8;
                        }

                        this.container = container;
                    } else {
                        this.addSecondaryContainer(container);
                    }
                } catch (Throwable var7) {
                    ExceptionLogger.logException(var7);
                }
            }
        } else if ("container.customTemperature".equals(change)) {
            float t = bb.getFloat();
            if (this.getContainer() != null) {
                this.getContainer().setCustomTemperature(t);
            }
        } else if ("name".equals(change)) {
            String str = GameWindow.ReadStringUTF(bb);
            this.setName(str);
        } else if ("replaceWith".equals(change)) {
            try {
                int index = this.getObjectIndex();
                if (index >= 0) {
                    IsoObject replace = factoryFromFileInput(this.getCell(), bb);
                    replace.load(bb, 241);
                    replace.setSquare(this.square);
                    this.square.getObjects().set(index, replace);
                    this.square.getSpecialObjects().remove(this);
                    this.square.RecalcAllWithNeighbours(true);
                    if (this.getContainerCount() > 0) {
                        for (int i = 0; i < this.getContainerCount(); i++) {
                            ItemContainer container = this.getContainerByIndex(i);
                            container.removeItemsFromProcessItems();
                        }

                        LuaEventManager.triggerEvent("OnContainerUpdate");
                    }
                }
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        } else if ("usesExternalWaterSource".equals(change)) {
            this.usesExternalWaterSource = bb.get() == 1;
        } else if ("sprite".equals(change)) {
            int id = bb.getInt();
            if (id == 0) {
                this.sprite = null;
                this.spriteName = null;
                this.tile = null;
            } else {
                this.spriteName = GameWindow.ReadString(bb);
                this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, id);
                if (this.sprite == null) {
                    this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                    this.sprite.LoadFramesNoDirPageSimple(this.spriteName);
                }
            }

            this.invalidateRenderChunkLevel(256L);
        } else if ("emptyTrash".equals(change)) {
            ItemContainer cont = this.getContainer();

            for (int i = 0; i < cont.getItems().size(); i++) {
                InventoryItem item = cont.getItems().get(i);
                cont.DoRemoveItem(item);
            }

            this.getContainer().clear();
            if (this.getOverlaySprite() != null) {
                ItemPickerJava.updateOverlaySprite(this);
            }
        }

        this.checkMoveWithWind();
    }

    @Deprecated
    public void transmitUpdatedSpriteToServer() {
        if (GameClient.client) {
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.UpdateItemSprite.doPacket(b);
            b.putInt(this.getSprite().id);
            GameWindow.WriteStringUTF(b.bb, this.spriteName);
            b.putInt(this.getSquare().getX());
            b.putInt(this.getSquare().getY());
            b.putInt(this.getSquare().getZ());
            b.putInt(this.getSquare().getObjects().indexOf(this));
            if (this.attachedAnimSprite != null) {
                b.putByte((byte)this.attachedAnimSprite.size());

                for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                    IsoSpriteInstance aas = this.attachedAnimSprite.get(i);
                    b.putInt(aas.parentSprite.id);
                }
            } else {
                b.putByte((byte)0);
            }

            PacketTypes.PacketType.UpdateItemSprite.send(GameClient.connection);
            DebugLog.General.warn("Special for the MP branch: The deprecated function was called: transmitUpdatedSpriteToServer");
        }
    }

    @Deprecated
    public void transmitCompleteItemToServer() {
        if (GameClient.client) {
            AddItemToMapPacket packet = new AddItemToMapPacket();
            packet.set(this);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.AddItemToMap.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.AddItemToMap.send(GameClient.connection);
            DebugLog.General.warn("Special for the MP branch: The deprecated function was called: transmitCompleteItemToServer");
        }
    }

    public void transmitModData() {
        if (this.square != null) {
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.ObjectModData, this);
            } else if (GameServer.server) {
                GameServer.sendObjectModData(this);
            }

            this.flagForHotSave();
        }
    }

    public void writeToRemoteBuffer(ByteBufferWriter b) {
        try {
            this.save(b.bb);
        } catch (IOException var3) {
            var3.printStackTrace();
        }

        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putInt(this.getObjectIndex());
        b.putBoolean(this.square.getSpecialObjects().contains(this));
        b.putBoolean(this.square.getWorldObjects().contains(this));
    }

    public int getObjectIndex() {
        return this.square == null ? -1 : this.square.getObjects().indexOf(this);
    }

    public int getMovingObjectIndex() {
        return this.square != null && this instanceof IsoMovingObject object ? this.square.getMovingObjects().indexOf(object) : -1;
    }

    public int getSpecialObjectIndex() {
        return this.square == null ? -1 : this.square.getSpecialObjects().indexOf(this);
    }

    public int getStaticMovingObjectIndex() {
        return this.square != null && this instanceof IsoMovingObject object ? this.square.getStaticMovingObjects().indexOf(object) : -1;
    }

    public int getWorldObjectIndex() {
        return this.square != null && this instanceof IsoWorldInventoryObject object ? this.square.getWorldObjects().indexOf(object) : -1;
    }

    public IsoSprite getOverlaySprite() {
        return this.overlaySprite;
    }

    public void setOverlaySprite(String spriteName) {
        this.setOverlaySprite(spriteName, -1.0F, -1.0F, -1.0F, -1.0F, true);
    }

    public void setOverlaySprite(String spriteName, boolean bTransmit) {
        this.setOverlaySprite(spriteName, -1.0F, -1.0F, -1.0F, -1.0F, bTransmit);
    }

    public void setOverlaySpriteColor(float r, float g, float b, float a) {
        this.overlaySpriteColor = new ColorInfo(r, g, b, a);
    }

    public ColorInfo getOverlaySpriteColor() {
        return this.overlaySpriteColor;
    }

    public void setOverlaySprite(String spriteName, float r, float g, float b, float a) {
        this.setOverlaySprite(spriteName, r, g, b, a, true);
    }

    public boolean setOverlaySprite(String spriteName, float r, float g, float b, float a, boolean bTransmit) {
        if (StringUtils.isNullOrWhitespace(spriteName)) {
            if (this.overlaySprite == null) {
                return false;
            }

            this.overlaySprite = null;
            spriteName = "";
        } else {
            boolean sameColor;
            if (!(r > -1.0F)) {
                sameColor = this.overlaySpriteColor == null;
            } else {
                sameColor = this.overlaySpriteColor != null
                    && this.overlaySpriteColor.r == r
                    && this.overlaySpriteColor.g == g
                    && this.overlaySpriteColor.b == b
                    && this.overlaySpriteColor.a == a;
            }

            if (this.overlaySprite != null && spriteName.equals(this.overlaySprite.name) && sameColor) {
                return false;
            }

            this.overlaySprite = IsoSpriteManager.instance.getSprite(spriteName);
            this.overlaySprite.name = spriteName;
        }

        if (r > -1.0F) {
            this.overlaySpriteColor = new ColorInfo(r, g, b, a);
        } else {
            this.overlaySpriteColor = null;
        }

        if (PerformanceSettings.fboRenderChunk && !GameServer.server && Thread.currentThread() == GameWindow.gameThread) {
            this.invalidateRenderChunkLevel(256L);
        }

        if (!bTransmit) {
            return true;
        } else {
            if (GameServer.server) {
                GameServer.updateOverlayForClients(this, spriteName, r, g, b, a, null);
            } else if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.UpdateOverlaySprite, this, spriteName, r, g, b, a);
            }

            return true;
        }
    }

    public boolean hasOverlaySprite() {
        return this.getOverlaySprite() != null;
    }

    public boolean haveSpecialTooltip() {
        return this.specialTooltip;
    }

    public void setSpecialTooltip(boolean specialTooltip) {
        this.specialTooltip = specialTooltip;
    }

    public int getKeyId() {
        return this.keyId;
    }

    public void setKeyId(int keyId) {
        this.keyId = keyId;
    }

    public boolean isHighlighted() {
        return (this.ppfHighlighted & 15) != 0;
    }

    public void setHighlighted(boolean highlight) {
        this.setHighlighted(highlight, true);
    }

    public void setHighlighted(boolean highlight, boolean renderOnce) {
        if (highlight) {
            this.ppfHighlighted = 15;
        } else {
            this.ppfHighlighted = 0;
        }

        if (renderOnce) {
            this.ppfHighlightRenderOnce = 15;
        } else {
            this.ppfHighlightRenderOnce = 0;
        }

        if (PerformanceSettings.fboRenderChunk) {
            if ((this.ppfHighlighted & 15) != 0) {
                FBORenderObjectHighlight.getInstance().registerObject(this);
            } else {
                FBORenderObjectHighlight.getInstance().unregisterObject(this);
            }
        }
    }

    public boolean isHighlightRenderOnce() {
        return (this.ppfHighlightRenderOnce & 15) != 0;
    }

    public void setHighlightRenderOnce(boolean highlight) {
        if (highlight) {
            this.ppfHighlightRenderOnce = 15;
        } else {
            this.ppfHighlightRenderOnce = 0;
        }
    }

    public boolean isHighlighted(int playerIndex) {
        return (this.ppfHighlighted & 1 << playerIndex) != 0;
    }

    public void setHighlighted(int playerIndex, boolean highlight) {
        this.setHighlighted(playerIndex, highlight, true);
    }

    public void setHighlighted(int playerIndex, boolean highlight, boolean renderOnce) {
        if (highlight && DebugOptions.instance.terrain.renderTiles.renderContainerHighlight.getValue()) {
            this.ppfHighlighted |= (byte)(1 << playerIndex);
        } else {
            this.ppfHighlighted &= (byte)(~(1 << playerIndex));
        }

        if (PerformanceSettings.fboRenderChunk) {
            if ((this.ppfHighlighted & 15) != 0) {
                FBORenderObjectHighlight.getInstance().registerObject(this);
            } else {
                FBORenderObjectHighlight.getInstance().unregisterObject(this);
            }
        }

        this.setHighlightRenderOnce(playerIndex, renderOnce);
    }

    public boolean isHighlightRenderOnce(int playerIndex) {
        return (this.ppfHighlightRenderOnce & 1 << playerIndex) != 0;
    }

    public void setHighlightRenderOnce(int playerIndex, boolean highlight) {
        if (highlight) {
            this.ppfHighlightRenderOnce |= (byte)(1 << playerIndex);
        } else {
            this.ppfHighlightRenderOnce &= (byte)(~(1 << playerIndex));
        }
    }

    public ColorInfo getHighlightColor() {
        return this.getHighlightColor(0);
    }

    public void setHighlightColor(ColorInfo highlightColor) {
        for (int playerNum = 0; playerNum < 4; playerNum++) {
            this.setHighlightColor(playerNum, highlightColor);
        }
    }

    public void setHighlightColor(float r, float g, float b, float a) {
        for (int playerNum = 0; playerNum < 4; playerNum++) {
            this.setHighlightColor(playerNum, r, g, b, a);
        }
    }

    public ColorInfo getHighlightColor(int playerIndex) {
        return !GameServer.server ? this.highlightColor[playerIndex] : new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void setHighlightColor(int playerIndex, ColorInfo highlightColor) {
        if (!GameServer.server) {
            this.highlightColor[playerIndex].set(highlightColor);
        }
    }

    public void setHighlightColor(int playerIndex, float r, float g, float b, float a) {
        if (!GameServer.server) {
            this.highlightColor[playerIndex].set(r, g, b, a);
        }
    }

    public boolean isBlink() {
        return (this.ppfBlink & 15) != 0;
    }

    public void setBlink(boolean blink) {
        if (blink) {
            this.ppfBlink = 15;
        } else {
            this.ppfBlink = 0;
        }
    }

    public boolean isBlink(int playerIndex) {
        return (this.ppfBlink & 1 << playerIndex) != 0;
    }

    public void setBlink(int playerIndex, boolean blink) {
        if (blink) {
            this.ppfBlink = (byte)(this.ppfBlink | 1 << playerIndex);
        } else {
            this.ppfBlink = (byte)(this.ppfBlink & ~(1 << playerIndex));
        }
    }

    public boolean isSatChair() {
        return this.satChair;
    }

    public void setSatChair(boolean satChair) {
        this.satChair = satChair;
        this.invalidateRenderChunkLevel(256L);
    }

    public void checkHaveElectricity() {
        if (!GameServer.server) {
            ItemContainer container = this.getContainerByEitherType("fridge", "freezer");
            if (container != null && container.isPowered()) {
                IsoWorld.instance.getCell().addToProcessIsoObject(this);
                if (this.getChunk() != null && !this.hasObjectAmbientEmitter()) {
                    this.getChunk().addObjectAmbientEmitter(this, new ObjectAmbientEmitters.FridgeHumLogic().init(this));
                }
            }

            this.checkAmbientSound();
            this.checkLightSourceActive();
        }
    }

    public void checkAmbientSound() {
        PropertyContainer props = this.getProperties();
        if (props != null && props.has("AmbientSound") && this.getChunk() != null && !this.hasObjectAmbientEmitter()) {
            this.getChunk().addObjectAmbientEmitter(this, new ObjectAmbientEmitters.AmbientSoundLogic().init(this));
        }
    }

    public int getContainerCount() {
        int count = this.container == null ? 0 : 1;
        int count2 = this.secondaryContainers == null ? 0 : this.secondaryContainers.size();
        return count + count2;
    }

    public ItemContainer getContainerByIndex(int index) {
        if (this.container != null) {
            if (index == 0) {
                return this.container;
            } else if (this.secondaryContainers == null) {
                return null;
            } else {
                return index >= 1 && index <= this.secondaryContainers.size() ? this.secondaryContainers.get(index - 1) : null;
            }
        } else if (this.secondaryContainers == null) {
            return null;
        } else {
            return index >= 0 && index < this.secondaryContainers.size() ? this.secondaryContainers.get(index) : null;
        }
    }

    public ItemContainer getContainerByType(String type) {
        for (int i = 0; i < this.getContainerCount(); i++) {
            ItemContainer container = this.getContainerByIndex(i);
            if (container.getType().equals(type)) {
                return container;
            }
        }

        return null;
    }

    public ItemContainer getContainerByEitherType(String type1, String type2) {
        for (int i = 0; i < this.getContainerCount(); i++) {
            ItemContainer container = this.getContainerByIndex(i);
            if (container.getType().equals(type1) || container.getType().equals(type2)) {
                return container;
            }
        }

        return null;
    }

    public void addSecondaryContainer(ItemContainer container) {
        if (this.secondaryContainers == null) {
            this.secondaryContainers = new ArrayList<>();
        }

        this.secondaryContainers.add(container);
        container.parent = this;
    }

    public int getContainerIndex(ItemContainer container) {
        if (container == this.container) {
            return 0;
        } else if (this.secondaryContainers == null) {
            return -1;
        } else {
            for (int i = 0; i < this.secondaryContainers.size(); i++) {
                if (this.secondaryContainers.get(i) == container) {
                    return (this.container == null ? 0 : 1) + i;
                }
            }

            return -1;
        }
    }

    public void removeAllContainers() {
        this.container = null;
        if (this.secondaryContainers != null) {
            this.secondaryContainers.clear();
        }
    }

    public void createFluidContainersFromSpriteProperties() {
    }

    public void createContainersFromSpriteProperties() {
        if (this.sprite != null) {
            if (this.container == null) {
                if (!(this instanceof IsoFeedingTrough trough && trough != trough.getMasterTrough())) {
                    if (!Objects.equals(this.sprite.getProperties().get("container"), "fruitbusha")) {
                        if (!Objects.equals(this.sprite.getProperties().get("container"), "fruitbushb")) {
                            if (!Objects.equals(this.sprite.getProperties().get("container"), "fruitbushc")) {
                                if (!Objects.equals(this.sprite.getProperties().get("container"), "fruitbushd")) {
                                    if (!Objects.equals(this.sprite.getProperties().get("container"), "fruitbushe")) {
                                        if (!Objects.equals(this.sprite.getProperties().get("container"), "corn")) {
                                            if (this.sprite.getProperties().has(IsoFlagType.container) && this.container == null) {
                                                this.container = new ItemContainer(this.sprite.getProperties().get("container"), this.square, this);
                                                this.container.parent = this;
                                                this.outlineOnMouseover = true;
                                                if (this.sprite.getProperties().has("ContainerCapacity")) {
                                                    this.container.capacity = Integer.parseInt(this.sprite.getProperties().get("ContainerCapacity"));
                                                }

                                                if (this.sprite.getProperties().has("ContainerPosition")) {
                                                    this.container.setContainerPosition(this.sprite.getProperties().get("ContainerPosition"));
                                                }
                                            }

                                            if (this.getSprite().getProperties().has("Freezer")) {
                                                ItemContainer freezer = new ItemContainer("freezer", this.square, this);
                                                if (this.getSprite().getProperties().has("FreezerCapacity")) {
                                                    freezer.capacity = Integer.parseInt(this.sprite.getProperties().get("FreezerCapacity"));
                                                } else {
                                                    freezer.capacity = 15;
                                                }

                                                if (this.container == null) {
                                                    this.container = freezer;
                                                    this.container.parent = this;
                                                } else {
                                                    this.addSecondaryContainer(freezer);
                                                }

                                                if (this.sprite.getProperties().has("FreezerPosition")) {
                                                    freezer.setFreezerPosition(this.sprite.getProperties().get("FreezerPosition"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return true;
    }

    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        return true;
    }

    public void cleanWallBlood() {
        this.square.removeBlood(false, true);
    }

    public ObjectRenderEffects getWindRenderEffects() {
        return this.windRenderEffects;
    }

    public ObjectRenderEffects getObjectRenderEffects() {
        return this.objectRenderEffects;
    }

    public void setRenderEffect(RenderEffectType type) {
        this.setRenderEffect(type, false);
    }

    public IsoObject getRenderEffectMaster() {
        return this;
    }

    public int getRenderEffectObjectCount() {
        return 1;
    }

    public IsoObject getRenderEffectObjectByIndex(int index) {
        return this;
    }

    public void setRenderEffect(RenderEffectType type, boolean reuseEqualType) {
        if (!GameServer.server) {
            IsoObject master = this.getRenderEffectMaster();
            ObjectRenderEffects oldEffect = master.objectRenderEffects;
            if (master.objectRenderEffects == null || reuseEqualType) {
                master.objectRenderEffects = ObjectRenderEffects.getNew(this, type, reuseEqualType);
            }

            if (PerformanceSettings.fboRenderChunk && master.objectRenderEffects != oldEffect) {
                for (int i = 0; i < this.getRenderEffectObjectCount(); i++) {
                    IsoObject object = this.getRenderEffectObjectByIndex(i);
                    if (object != null) {
                        object.invalidateRenderChunkLevel(256L);
                    }
                }
            }
        }
    }

    public void removeRenderEffect(ObjectRenderEffects o) {
        IsoObject master = this.getRenderEffectMaster();
        if (master.objectRenderEffects != null && master.objectRenderEffects == o) {
            master.objectRenderEffects = null;
            if (PerformanceSettings.fboRenderChunk) {
                for (int i = 0; i < this.getRenderEffectObjectCount(); i++) {
                    IsoObject object = this.getRenderEffectObjectByIndex(i);
                    if (object != null) {
                        object.invalidateRenderChunkLevel(256L);
                    }
                }
            }
        }
    }

    public ObjectRenderEffects getObjectRenderEffectsToApply() {
        IsoObject master = this.getRenderEffectMaster();
        if (master.objectRenderEffects != null) {
            return master.objectRenderEffects;
        } else {
            return Core.getInstance().getOptionDoWindSpriteEffects() && master.windRenderEffects != null ? master.windRenderEffects : null;
        }
    }

    public void destroyFence(IsoDirections dir) {
        BrokenFences.getInstance().destroyFence(this, dir);
    }

    public ArrayList<IsoObject> getSpriteGridObjects(ArrayList<IsoObject> result) {
        return this.getSpriteGridObjects(result, false);
    }

    public ArrayList<IsoObject> getSpriteGridObjectsExcludingSelf(ArrayList<IsoObject> result) {
        return this.getSpriteGridObjects(result, false);
    }

    public ArrayList<IsoObject> getSpriteGridObjectsIncludingSelf(ArrayList<IsoObject> result) {
        return this.getSpriteGridObjects(result, true);
    }

    public ArrayList<IsoObject> getSpriteGridObjects(ArrayList<IsoObject> result, boolean bAddSelf) {
        result.clear();
        IsoSpriteGrid spriteGrid = this.getSpriteGrid();
        if (spriteGrid == null) {
            if (bAddSelf) {
                result.add(this);
            }

            return result;
        } else {
            int gridX = spriteGrid.getSpriteGridPosX(this.sprite);
            int gridY = spriteGrid.getSpriteGridPosY(this.sprite);
            int gridZ = spriteGrid.getSpriteGridPosZ(this.sprite);
            int curX = this.getSquare().getX();
            int curY = this.getSquare().getY();
            int curZ = this.getSquare().getZ();

            for (int z = curZ - gridZ; z < curZ - gridZ + spriteGrid.getLevels(); z++) {
                for (int y = curY - gridY; y < curY - gridY + spriteGrid.getHeight(); y++) {
                    for (int x = curX - gridX; x < curX - gridX + spriteGrid.getWidth(); x++) {
                        IsoGridSquare testSq = this.getCell().getGridSquare(x, y, z);
                        if (testSq != null) {
                            for (int i = 0; i < testSq.getObjects().size(); i++) {
                                IsoObject object = testSq.getObjects().get(i);
                                if ((object != this || bAddSelf) && object.getSpriteGrid() == spriteGrid) {
                                    result.add(object);
                                }
                            }
                        }
                    }
                }
            }

            return result;
        }
    }

    public boolean isConnectedSpriteGridObject(IsoObject object) {
        if (object == null) {
            return false;
        } else {
            IsoSprite sprite1 = this.getSprite();
            IsoSprite sprite2 = object.getSprite();
            if (sprite1 != null && sprite2 != null) {
                IsoSpriteGrid spriteGrid1 = sprite1.getSpriteGrid();
                IsoSpriteGrid spriteGrid2 = sprite2.getSpriteGrid();
                if (spriteGrid1 != null && spriteGrid1 == spriteGrid2) {
                    int gridX1 = this.getSquare().getX() - spriteGrid1.getSpriteGridPosX(sprite1);
                    int gridY1 = this.getSquare().getY() - spriteGrid1.getSpriteGridPosY(sprite1);
                    int gridZ1 = this.getSquare().getZ() - spriteGrid1.getSpriteGridPosZ(sprite1);
                    int gridX2 = object.getSquare().getX() - spriteGrid2.getSpriteGridPosX(sprite2);
                    int gridY2 = object.getSquare().getY() - spriteGrid2.getSpriteGridPosY(sprite2);
                    int gridZ2 = object.getSquare().getZ() - spriteGrid2.getSpriteGridPosZ(sprite2);
                    return gridX1 == gridX2 && gridY1 == gridY2 && gridZ1 == gridZ2;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public boolean isOnScreen() {
        return this.getSquare() != null && this.getSquare().IsOnScreen();
    }

    public final void setOutlineHighlightCol(ColorInfo outlineHighlightCol) {
        if (!GameServer.server) {
            if (outlineHighlightCol != null) {
                for (int i = 0; i < this.outlineHighlightCol.length; i++) {
                    this.outlineHighlightCol[i] = Color.colorToABGR(outlineHighlightCol.r, outlineHighlightCol.g, outlineHighlightCol.b, outlineHighlightCol.a);
                }
            }
        }
    }

    public final int getOutlineHighlightCol(int playerIndex) {
        return GameServer.server ? -1 : this.outlineHighlightCol[playerIndex];
    }

    public final void setOutlineHighlightCol(int playerIndex, ColorInfo outlineHighlightCol) {
        if (outlineHighlightCol != null && !GameServer.server) {
            this.outlineHighlightCol[playerIndex] = Color.colorToABGR(
                outlineHighlightCol.r, outlineHighlightCol.g, outlineHighlightCol.b, outlineHighlightCol.a
            );
        }
    }

    public final void setOutlineHighlightCol(float r, float g, float b, float a) {
        if (!GameServer.server) {
            for (int i = 0; i < this.outlineHighlightCol.length; i++) {
                this.outlineHighlightCol[i] = Color.colorToABGR(r, g, b, a);
            }
        }
    }

    public final void setOutlineHighlightCol(int playerIndex, float r, float g, float b, float a) {
        if (!GameServer.server) {
            this.outlineHighlightCol[playerIndex] = Color.colorToABGR(r, g, b, a);
        }
    }

    public final boolean isOutlineHighlight() {
        return this.isOutlineHighlight != 0;
    }

    public final boolean isOutlineHighlight(int playerIndex) {
        return !DebugOptions.instance.terrain.renderTiles.renderContainerHighlight.getValue() ? false : (this.isOutlineHighlight & 1 << playerIndex) != 0;
    }

    public final void setOutlineHighlight(boolean isOutlineHighlight) {
        this.isOutlineHighlight = (byte)(isOutlineHighlight ? -1 : 0);
        if (isOutlineHighlight) {
            if (PerformanceSettings.fboRenderChunk) {
                long timeRender = UIManager.isRendering() ? UIManager.uiRenderTimeMS : 0L;
                long timeUpdate = UIManager.isUpdating() ? UIManager.uiUpdateTimeMS : 0L;

                for (int i = 0; i < 4; i++) {
                    FBORenderObjectOutline.getInstance().setDuringUIRenderTime(i, this, timeRender);
                    FBORenderObjectOutline.getInstance().setDuringUIUpdateTime(i, this, timeUpdate);
                }

                FBORenderObjectOutline.getInstance().registerObject(this);
            }
        } else if (PerformanceSettings.fboRenderChunk) {
            for (int i = 0; i < 4; i++) {
                FBORenderObjectOutline.getInstance().setDuringUIRenderTime(i, this, 0L);
                FBORenderObjectOutline.getInstance().setDuringUIUpdateTime(i, this, 0L);
            }

            FBORenderObjectOutline.getInstance().unregisterObject(this);
        }
    }

    public final void setOutlineHighlight(int playerIndex, boolean isOutlineHighlight) {
        byte old = this.isOutlineHighlight;
        if (isOutlineHighlight) {
            this.isOutlineHighlight |= (byte)(1 << playerIndex);
            if (PerformanceSettings.fboRenderChunk) {
                long timeRender = UIManager.isRendering() ? UIManager.uiRenderTimeMS : 0L;
                long timeUpdate = UIManager.isUpdating() ? UIManager.uiUpdateTimeMS : 0L;
                FBORenderObjectOutline.getInstance().setDuringUIRenderTime(playerIndex, this, timeRender);
                FBORenderObjectOutline.getInstance().setDuringUIUpdateTime(playerIndex, this, timeUpdate);
            }
        } else {
            this.isOutlineHighlight &= (byte)(~(1 << playerIndex));
            FBORenderObjectOutline.getInstance().setDuringUIRenderTime(playerIndex, this, 0L);
            FBORenderObjectOutline.getInstance().setDuringUIUpdateTime(playerIndex, this, 0L);
        }

        if (PerformanceSettings.fboRenderChunk && old != this.isOutlineHighlight) {
            if (this.isOutlineHighlight != 0) {
                FBORenderObjectOutline.getInstance().registerObject(this);
            } else {
                FBORenderObjectOutline.getInstance().unregisterObject(this);
            }
        }
    }

    public final boolean isOutlineHlAttached() {
        return this.isOutlineHlAttached != 0;
    }

    public final boolean isOutlineHlAttached(int playerIndex) {
        return (this.isOutlineHlAttached & 1 << playerIndex) != 0;
    }

    public void setOutlineHlAttached(boolean isOutlineHlAttached) {
        this.isOutlineHlAttached = (byte)(isOutlineHlAttached ? -1 : 0);
    }

    public final void setOutlineHlAttached(int playerIndex, boolean isOutlineHlAttached) {
        if (isOutlineHlAttached) {
            this.isOutlineHlAttached = (byte)(this.isOutlineHlAttached | 1 << playerIndex);
        } else {
            this.isOutlineHlAttached = (byte)(this.isOutlineHlAttached & ~(1 << playerIndex));
        }
    }

    public boolean isOutlineHlBlink() {
        return this.isOutlineHlBlink != 0;
    }

    public final boolean isOutlineHlBlink(int playerIndex) {
        return (this.isOutlineHlBlink & 1 << playerIndex) != 0;
    }

    public void setOutlineHlBlink(boolean isOutlineHlBlink) {
        this.isOutlineHlBlink = (byte)(isOutlineHlBlink ? -1 : 0);
    }

    public final void setOutlineHlBlink(int playerIndex, boolean isOutlineHlBlink) {
        if (isOutlineHlBlink) {
            this.isOutlineHlBlink |= (byte)(1 << playerIndex);
        } else {
            this.isOutlineHlBlink &= (byte)(~(1 << playerIndex));
        }
    }

    public void unsetOutlineHighlight() {
        this.isOutlineHighlight = 0;
        this.isOutlineHlBlink = 0;
        this.isOutlineHlAttached = 0;
    }

    public float getOutlineThickness() {
        return this.outlineThickness;
    }

    public void setOutlineThickness(float outlineThickness) {
        this.outlineThickness = outlineThickness;
    }

    protected void addItemsFromProperties() {
        PropertyContainer props = this.getProperties();
        if (props != null) {
            String Material = props.get("Material");
            String Material2 = props.get("Material2");
            String Material3 = props.get("Material3");
            if ("Wood".equals(Material) || "Wood".equals(Material2) || "Wood".equals(Material3)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                if (Rand.NextBool(5)) {
                    this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                }
            }

            if (("MetalBars".equals(Material) || "MetalBars".equals(Material2) || "MetalBars".equals(Material3)) && Rand.NextBool(2)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.MetalBar"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("MetalPlates".equals(Material) || "MetalPlates".equals(Material2) || "MetalPlates".equals(Material3)) && Rand.NextBool(2)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.SheetMetal"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("MetalPipe".equals(Material) || "MetalPipe".equals(Material2) || "MetalPipe".equals(Material3)) && Rand.NextBool(2)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.MetalPipe"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("MetalWire".equals(Material) || "MetalWire".equals(Material2) || "MetalWire".equals(Material3)) && Rand.NextBool(3)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Wire"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("Nails".equals(Material) || "Nails".equals(Material2) || "Nails".equals(Material3)) && Rand.NextBool(2)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Nails"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("Screws".equals(Material) || "Screws".equals(Material2) || "Screws".equals(Material3)) && Rand.NextBool(2)) {
                this.square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Screws"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.damage <= 0;
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        IsoGameCharacter chr = Type.tryCastTo(thumper, IsoGameCharacter.class);
        if (chr != null) {
            Thumpable thumpable = this.getThumpableFor(chr);
            if (thumpable == null) {
                return;
            }

            if (thumpable != this) {
                thumpable.Thump(thumper);
                return;
            }
        }

        boolean bBreakableFence = BrokenFences.getInstance().isBreakableObject(this);
        boolean bBendableFence = BentFences.getInstance().isUnbentObject(this);
        BentFences.ThumpData thumpData = bBendableFence ? BentFences.getInstance().getThumpData(this) : null;
        if (thumper instanceof IsoZombie) {
            if (bBendableFence && thumpData != null && thumpData.thumpersToDamage > 0) {
                WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 40, 40, true, 4.0F, 25.0F);
                if (thumpData.totalThumpers >= thumpData.thumpersToDamage) {
                    int amount = ThumpState.getFastForwardDamageMultiplier();
                    float totalDamage = amount * 0.1F * thumpData.damageMultiplier;
                    if (totalDamage >= 1.0F) {
                        this.damage -= (short)totalDamage;
                    } else {
                        this.partialThumpDmg = this.partialThumpDmg + totalDamage * ThumpState.getFastForwardDamageMultiplier();
                        if ((int)this.partialThumpDmg > 0) {
                            amount = (int)this.partialThumpDmg;
                            this.damage -= (short)amount;
                            this.partialThumpDmg -= amount;
                        }
                    }
                }
            } else {
                int thumpDmgThreshold = 8;
                int totalThumpers = thumper.getSurroundingThumpers();
                if (totalThumpers >= 8) {
                    int amount = (int)this.partialThumpDmg;
                    this.damage -= (short)amount;
                } else {
                    this.partialThumpDmg = this.partialThumpDmg + totalThumpers / 8.0F * ThumpState.getFastForwardDamageMultiplier();
                    if ((int)this.partialThumpDmg > 0) {
                        int amount = (int)this.partialThumpDmg;
                        this.damage -= (short)amount;
                        this.partialThumpDmg -= amount;
                    }
                }
            }

            WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
        }

        if (this.damage <= 0) {
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
            thumper.setThumpTarget(null);
            if (bBreakableFence || bBendableFence) {
                if (bBendableFence && thumpData != null && thumpData.directionToBend != IsoDirections.Max) {
                    BentFences.getInstance().bendFence(this, thumpData.directionToBend);
                    WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 40, 40, true, 4.0F, 25.0F);
                } else {
                    PropertyContainer props = this.getProperties();
                    IsoDirections dirBreak;
                    if (props.has(IsoFlagType.collideN) && props.has(IsoFlagType.collideW)) {
                        dirBreak = thumper.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                    } else if (props.has(IsoFlagType.collideN)) {
                        dirBreak = thumper.getY() >= this.getY() ? IsoDirections.N : IsoDirections.S;
                    } else {
                        dirBreak = thumper.getX() >= this.getX() ? IsoDirections.W : IsoDirections.E;
                    }

                    BrokenFences.getInstance().destroyFence(this, dirBreak);
                }

                return;
            }

            String breakSound = "BreakObject";
            if (chr != null) {
                chr.getEmitter().playSound("BreakObject", this);
            }

            if (GameServer.server) {
                GameServer.PlayWorldSoundServer("BreakObject", false, thumper.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
            }

            ArrayList<InventoryItem> items = new ArrayList<>();

            for (int i = 0; i < this.getContainerCount(); i++) {
                ItemContainer container = this.getContainerByIndex(i);
                items.clear();
                items.addAll(container.getItems());
                container.removeItemsFromProcessItems();
                container.removeAllItems();

                for (int j = 0; j < items.size(); j++) {
                    this.getSquare().AddWorldInventoryItem(items.get(j), 0.0F, 0.0F, 0.0F);
                }
            }

            this.square.transmitRemoveItemFromSquare(this);
        }
    }

    public void setMovedThumpable(boolean movedThumpable) {
        this.movedThumpable = movedThumpable;
    }

    public boolean isMovedThumpable() {
        return this.movedThumpable;
    }

    @Override
    public void WeaponHit(IsoGameCharacter chr, HandWeapon weapon) {
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        if (this.isDestroyed()) {
            return null;
        } else if (this.isMovedThumpable()) {
            return this;
        } else {
            if (!this.isHoppable() && BentFences.getInstance().isEnabled() && BentFences.getInstance().isUnbentObject(this)) {
                IsoZombie zombie = Type.tryCastTo(chr, IsoZombie.class);
                if (zombie != null && (zombie.getTarget() != null || zombie.isRespondingToPlayerSound())) {
                    return this;
                }
            }

            if (!BrokenFences.getInstance().isBreakableObject(this)) {
                return null;
            } else {
                IsoZombie zombie = Type.tryCastTo(chr, IsoZombie.class);
                return zombie != null && zombie.isCrawling() ? this : null;
            }
        }
    }

    public boolean isExistInTheWorld() {
        return this.square != null ? this.square.getObjects().contains(this) : false;
    }

    @Override
    public float getThumpCondition() {
        return PZMath.clamp(this.getDamage(), 0, 100) / 100.0F;
    }

    @Override
    public String toString() {
        return this.getName()
            + ":"
            + (this.getSpriteName() != null ? this.getSpriteName() : "null")
            + ":"
            + (this.getSprite() != null ? this.getSprite().getName() : "UNKNOWN")
            + ":"
            + super.toString();
    }

    @Override
    public GameEntityType getGameEntityType() {
        return GameEntityType.IsoObject;
    }

    @Override
    public long getEntityNetID() {
        if (this.getObjectIndex() == -1) {
            this.isoEntityNetId = -1L;
            return -1L;
        } else {
            if (this.isoEntityNetId == -1L || this.lastObjectIndex == -1 || this.lastObjectIndex != this.getObjectIndex()) {
                this.lastObjectIndex = this.getObjectIndex();
                long i = (long)this.lastObjectIndex << 40;
                long z = (long)this.square.getZ() << 32;
                long y = (long)this.square.getY() << 16;
                long x = this.square.getX();
                long newID = x + y + z + i;
                GameEntityManager.checkEntityIDChange(this, this.isoEntityNetId, newID);
                this.isoEntityNetId = newID;
            }

            return this.isoEntityNetId;
        }
    }

    @Override
    public boolean isEntityValid() {
        return true;
    }

    public IsoObject getMasterObject() {
        SpriteConfig spriteConfig = this.getSpriteConfig();
        return spriteConfig != null ? spriteConfig.getMultiSquareMaster() : this;
    }

    public boolean isTent() {
        if (this.getSprite().getProperties() == null
            || !this.getSprite().getProperties().has("CustomName")
            || !this.getSprite().getProperties().get("CustomName").contains("Tent") && !this.getSprite().getProperties().get("CustomName").contains("Shelter")) {
            if (this.getSprite().getProperties() == null
                || !this.getSprite().getProperties().has("CustomName")
                || !this.getSprite().getProperties().get("CustomName").contains("tent")
                    && !this.getSprite().getProperties().get("CustomName").contains("shelter")) {
                return this.getName() == null || !this.getName().contains("Tent") && !this.getName().contains("Shelter")
                    ? this.getName() != null && (this.getName().contains("tent") || this.getName().contains("shelter"))
                    : true;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public IsoDirections getFacing() {
        IsoSprite sprite = this.getSprite();
        return sprite != null ? sprite.getFacing() : null;
    }

    public String getTileName() {
        if (this.getProperties() != null) {
            PropertyContainer props = this.getProperties();
            if (props != null && props.has("CustomName")) {
                String customName = "Moveable Object";
                if (props.has("CustomName")) {
                    if (props.has("GroupName")) {
                        customName = props.get("GroupName") + " " + props.get("CustomName");
                    } else {
                        customName = props.get("CustomName");
                    }
                }

                return Translator.getMoveableDisplayName(customName);
            }
        }

        return this.getName();
    }

    public InventoryItem spawnItemToObjectSurface(String item) {
        return this.spawnItemToObjectSurface(item, false);
    }

    public InventoryItem spawnItemToObjectSurface(String item, boolean randomRotation) {
        return this.addItemToObjectSurface(item, randomRotation, true);
    }

    public InventoryItem spawnItemToObjectSurface(String item, boolean randomRotation, boolean checkForAdjacentCanStandSquare) {
        return checkForAdjacentCanStandSquare && !this.hasAdjacentCanStandSquare() ? null : this.addItemToObjectSurface(item, randomRotation, true);
    }

    public InventoryItem addItemToObjectSurface(String item) {
        return this.addItemToObjectSurface(item, false);
    }

    public InventoryItem addItemToObjectSurface(String item, boolean randomRotation) {
        return this.addItemToObjectSurface(item, randomRotation, false);
    }

    public InventoryItem addItemToObjectSurface(String item, boolean randomRotation, boolean spawnChecks) {
        if (item == null) {
            return null;
        } else {
            IsoDirections facing = this.getFacing();
            IsoGridSquare sq = this.getSquare();
            if (sq == null) {
                return null;
            } else {
                InventoryItem itemNew = null;
                if (facing != null) {
                    if (facing == IsoDirections.E) {
                        itemNew = ItemSpawner.spawnItem(item, sq, Rand.Next(0.4F, 0.42F), Rand.Next(0.34F, 0.74F), this.getSurfaceOffsetNoTable() / 96.0F);
                    }

                    if (facing == IsoDirections.W) {
                        itemNew = ItemSpawner.spawnItem(item, sq, Rand.Next(0.6F, 0.64F), Rand.Next(0.34F, 0.74F), this.getSurfaceOffsetNoTable() / 96.0F);
                    }

                    if (facing == IsoDirections.N) {
                        itemNew = ItemSpawner.spawnItem(item, sq, Rand.Next(0.44F, 0.64F), 0.67F, this.getSurfaceOffsetNoTable() / 96.0F);
                    }

                    if (facing == IsoDirections.S) {
                        itemNew = ItemSpawner.spawnItem(item, sq, Rand.Next(0.44F, 0.64F), 0.42F, this.getSurfaceOffsetNoTable() / 96.0F);
                    }
                } else {
                    itemNew = ItemSpawner.spawnItem(item, sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), this.getSurfaceOffsetNoTable() / 96.0F);
                }

                if (randomRotation) {
                    itemNew.randomizeWorldZRotation();
                }

                if (spawnChecks) {
                    itemNew.setAutoAge();
                    if (itemNew instanceof Food && itemNew.hasTag(ItemTag.SPAWN_COOKED)) {
                        itemNew.setCooked(true);
                        if (!StringUtils.isNullOrEmpty(((Food)itemNew).getOnCooked())) {
                            Object functionObj = LuaManager.getFunctionObject(((Food)itemNew).getOnCooked());
                            if (functionObj != null) {
                                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, itemNew);
                            }
                        }
                    }

                    if (itemNew instanceof HandWeapon handWeapon && Rand.Next(100) < 90) {
                        handWeapon.randomizeFirearmAsLoot();
                    }

                    if (itemNew.hasTag(ItemTag.SHOW_CONDITION) || itemNew instanceof HandWeapon || itemNew.hasSharpness()) {
                        itemNew.randomizeGeneralCondition();
                    }

                    itemNew.unsealIfNotFull();
                    if (itemNew instanceof InventoryContainer inventoryContainer) {
                        inventoryContainer.getItemContainer().setExplored(true);
                    }
                }

                return itemNew;
            }
        }
    }

    public ObjectRenderInfo getRenderInfo(int playerIndex) {
        if (GameServer.server) {
            ObjectRenderInfo info = new ObjectRenderInfo(this);
            info.layer = ObjectRenderLayer.None;
            info.targetAlpha = 0.0F;
            return info;
        } else {
            return this.renderInfo[playerIndex];
        }
    }

    public void invalidateRenderChunkLevel(long dirtyFlags) {
        if (this.getSquare() != null) {
            this.getSquare().invalidateRenderChunkLevel(dirtyFlags);
            IsoGridSquare renderSquare = this.getRenderSquare();
            if (renderSquare != null && renderSquare != this.getSquare()) {
                renderSquare.invalidateRenderChunkLevel(dirtyFlags);
            }
        }
    }

    public void invalidateVispolyChunkLevel() {
        if (this.getSquare() != null) {
            this.getSquare().invalidateVispolyChunkLevel();
        }
    }

    public boolean hasAnimatedAttachments() {
        if (this.hasAnimatedClockHands()) {
            return true;
        } else {
            boolean bShowOverlay = false;
            if (this.getSprite() != null && this.getProperties().has(IsoFlagType.HasLightOnSprite)) {
                bShowOverlay = this.shouldShowOnOverlay();
                if (bShowOverlay && this.getOnOverlay() == null && this.sprite.tilesetName != null) {
                    IsoSprite spr = IsoSpriteManager.instance.namedMap.get(this.sprite.tilesetName + "_on_" + this.sprite.tileSheetIndex);
                    if (spr != null) {
                        this.setOnOverlay(IsoSpriteInstance.get(spr));
                    }
                }
            }

            return bShowOverlay && this.getOnOverlay() != null;
        }
    }

    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo col) {
        this.renderClockHands(x, y, z, col);
        if (this.getOnOverlay() != null) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            float r = col.r;
            float g = col.g;
            float b = col.b;
            float a = col.a;
            col.set(1.0F, 1.0F, 1.0F, col.a);
            boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
            if (bHighlighted) {
                col.set(this.getHighlightColor(playerIndex));
                if (this.isBlink()) {
                    col.a = col.a * Core.blinkAlpha;
                }

                col.r = r * (1.0F - col.a) + this.getHighlightColor(playerIndex).r * col.a;
                col.g = g * (1.0F - col.a) + this.getHighlightColor(playerIndex).g * col.a;
                col.b = b * (1.0F - col.a) + this.getHighlightColor(playerIndex).b * col.a;
                col.a = a;
            }

            if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
                float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
                col.set(col.r * fade, col.g * fade, col.b * fade, col.getA());
            }

            this.getOnOverlay()
                .getParentSprite()
                .render(this.getOnOverlay(), this, x, y, z, this.dir, this.offsetX, this.offsetY + this.getRenderYOffset() * Core.tileScale, col, true);
            col.set(r, g, b, col.a);
        }
    }

    private ClockScript getClockScript() {
        if (this.clockScriptInit != this.sprite) {
            this.clockScriptInit = this.sprite;
            if (this.sprite != null && this.sprite.name != null) {
                this.clockScript = ScriptManager.instance.getClockScript(this.sprite.name);
            } else {
                this.clockScript = null;
            }
        }

        return this.clockScript;
    }

    private Texture checkClockTexture() {
        Texture oldTexture = this.sprite.texture;
        ClockScript clockScript = this.getClockScript();
        if (clockScript != null && clockScript.replacementSprite != null) {
            this.sprite.texture = Texture.getSharedTexture(clockScript.replacementSprite);
        }

        return oldTexture;
    }

    private boolean hasAnimatedClockHands() {
        ClockScript clockScript = this.getClockScript();
        return clockScript != null && !clockScript.replacementOnly;
    }

    private void renderClockHands(float x, float y, float z, ColorInfo col) {
        if (this.hasAnimatedClockHands()) {
            ClockScript clockScript = this.getClockScript();
            float tod = GameTime.getInstance().getTimeOfDay();
            KahluaTable luaObject = this.getModData();
            Float lastTime = null;
            if (luaObject != null && luaObject.rawget("LastTimeofDay") != null) {
                lastTime = (Float)luaObject.rawget("LastTimeofDay");
            }

            if ((!this.getSquare().isInARoom() && this.getSquare().associatedBuilding == null || !this.hasGridPower()) && !this.getSquare().haveElectricity()) {
                if (lastTime != null) {
                    tod = lastTime;
                } else {
                    tod = 7.0F;
                }
            } else if (luaObject == null || lastTime == null || tod > lastTime + 0.2F || tod < lastTime - 0.2F) {
                luaObject.rawset("LastTimeofDay", tod);
            }

            if (tod > 12.0F) {
                tod -= 12.0F;
            }

            float minute = tod - PZMath.floor(tod);
            float r = col.r;
            float g = col.g;
            float b = col.b;
            float alpha = col.a;
            col.a = this.getAlpha(IsoCamera.frameState.playerIndex);
            if (PerformanceSettings.fboRenderChunk) {
                col.a = this.getRenderInfo(IsoCamera.frameState.playerIndex).renderAlpha;
                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
                    float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
                    col.r *= fade;
                    col.g *= fade;
                    col.b *= fade;
                }
            }

            if (clockScript.north) {
                float ry = tod / 12.0F * 360.0F;
                this.renderClockHand(x, y, z, col, clockScript, clockScript.hourHand, 0.0F, 0.0F, ry, true);
                ry = minute * 360.0F;
                this.renderClockHand(x, y, z, col, clockScript, clockScript.minuteHand, 0.0F, 0.0F, ry, true);
            } else {
                float rx = 360.0F - tod / 12.0F * 360.0F;
                float ry = 180.0F;
                float rz = 0.0F;
                this.renderClockHand(x, y, z, col, clockScript, clockScript.hourHand, rx, 90.0F, 0.0F, false);
                rx = 360.0F - minute * 360.0F;
                this.renderClockHand(x, y, z, col, clockScript, clockScript.minuteHand, rx, 90.0F, 0.0F, false);
            }

            col.r = r;
            col.g = g;
            col.b = b;
            col.a = alpha;
        }
    }

    private void renderClockHand(
        float x, float y, float z, ColorInfo col, ClockScript clockScript, ClockScript.HandScript handScript, float rx, float ry, float rz, boolean bNorth
    ) {
        float SCALE = Core.debug ? 1.0F : 1.0F;
        float length1 = handScript.length;
        float length2 = 0.0F;
        float thickness = handScript.thickness;
        if (!Float.isNaN(handScript.textureAxisX) && !Float.isNaN(handScript.textureAxisY)) {
            Texture texture = Texture.getSharedTexture(handScript.texture);
            if (texture != null && texture.isReady()) {
                float length = handScript.length;
                float f = handScript.textureAxisY;
                length1 = length * f;
                length2 = length * (1.0F - f);
                thickness = texture.getWidth() * (length / texture.getHeight());
            }
        }

        ClockHands clockHands = ClockHands.s_pool.alloc();
        float dx = !bNorth && handScript == clockScript.hourHand ? 0.005F : 0.0F;
        float dy = bNorth && handScript == clockScript.hourHand ? 0.005F : 0.0F;
        clockHands.init(
            x + clockScript.handX + dx,
            y + clockScript.handY + dy,
            z + clockScript.handZ,
            rx,
            ry,
            rz,
            Texture.getSharedTexture(handScript.texture),
            length1 * SCALE,
            length2 * SCALE,
            thickness * SCALE,
            handScript.r * col.r,
            handScript.g * col.g,
            handScript.b * col.b,
            handScript.a * col.a
        );
        SpriteRenderer.instance.drawGeneric(clockHands);
    }

    public IsoGridSquare getRenderSquare() {
        return this.renderSquareOverride != null ? this.renderSquareOverride : this.getSquare();
    }

    public void setSpriteModelName(String spriteModelName) {
        this.spriteModelName = spriteModelName;
        this.spriteModel = null;
        this.spriteModelInit = null;
    }

    public SpriteModel getSpriteModel() {
        if (this.spriteModelName != null) {
            if (this.spriteModelInit != this.sprite) {
                this.spriteModelInit = this.sprite;
                this.spriteModel = ScriptManager.instance.getSpriteModel(this.spriteModelName);
            }

            return this.spriteModel;
        } else if (this.sprite != null && this.sprite.spriteModel != null) {
            if (this.spriteModelInit != this.sprite) {
                this.spriteModelInit = this.sprite;
                this.spriteModel = this.sprite.spriteModel;
            }

            return this.spriteModel;
        } else {
            return null;
        }
    }

    protected boolean renderModel(float x, float y, float z, ColorInfo col) {
        if (!PerformanceSettings.fboRenderChunk) {
            return false;
        } else {
            SpriteModel spriteModel = this.getSpriteModel();
            if (spriteModel == null) {
                return false;
            } else {
                float offset = this.sprite.getProperties().isSurfaceOffset() ? this.sprite.getProperties().getSurface() : 0.0F;
                int playerIndex = IsoCamera.frameState.playerIndex;
                boolean bDoor = this instanceof IsoDoor;
                if (this instanceof IsoThumpable thumpable) {
                    bDoor |= thumpable.isDoor();
                }

                if (bDoor && this.square != null && !DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                    col = this.square.getLightInfo(playerIndex);
                }

                ColorInfo colOrig = col;
                float a = col.a;
                float targetAlpha = this.getTargetAlpha(IsoCamera.frameState.playerIndex);
                float alpha = this.getAlpha(IsoCamera.frameState.playerIndex);
                if (alpha < targetAlpha) {
                    alpha += IsoSprite.alphaStep;
                    if (alpha > targetAlpha) {
                        alpha = targetAlpha;
                    }
                } else if (alpha > targetAlpha) {
                    alpha -= IsoSprite.alphaStep;
                    if (alpha < targetAlpha) {
                        alpha = targetAlpha;
                    }
                }

                if (alpha < 0.0F) {
                    alpha = 0.0F;
                }

                if (alpha > 1.0F) {
                    alpha = 1.0F;
                }

                this.setAlpha(IsoCamera.frameState.playerIndex, alpha);
                col.a = col.a * this.getAlpha(IsoCamera.frameState.playerIndex);
                if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue()) {
                    col.a = 1.0F;
                }

                boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
                if (bHighlighted) {
                    stCol.set(this.getHighlightColor(playerIndex));
                    if (this.isBlink()) {
                        stCol.a = stCol.a * Core.blinkAlpha;
                    }

                    stCol.r = col.r * (1.0F - stCol.a) + this.getHighlightColor(playerIndex).r * stCol.a;
                    stCol.g = col.g * (1.0F - stCol.a) + this.getHighlightColor(playerIndex).g * stCol.a;
                    stCol.b = col.b * (1.0F - stCol.a) + this.getHighlightColor(playerIndex).b * stCol.a;
                    stCol.a = col.a;
                    col = stCol;
                }

                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
                    alpha = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
                    col = stCol.set(col.r * alpha, col.g * alpha, col.b * alpha, col.a);
                }

                AnimationPlayer animationPlayer = IsoObjectAnimations.getInstance().getAnimationPlayer(this);
                IsoObjectModelDrawer.RenderStatus renderStatus;
                if (animationPlayer == null) {
                    ObjectRenderEffects renderEffects = this.getObjectRenderEffectsToApply();
                    if (renderEffects == null) {
                        renderStatus = IsoObjectModelDrawer.renderMain(spriteModel, x, y, z, col, this.getRenderYOffset() + offset);
                        if (this.isOutlineHighlight(playerIndex)) {
                            ColorInfo outlineColor = stCol.setABGR(this.getOutlineHighlightCol(playerIndex));
                            outlineColor.a = this.isOutlineHlBlink(playerIndex) ? Core.blinkAlpha : 1.0F;
                            IsoObjectModelDrawer.renderMainOutline(spriteModel, x, y, z, outlineColor, this.getRenderYOffset() + offset);
                        }
                    } else {
                        renderStatus = IsoObjectModelDrawer.renderMain(
                            spriteModel, x + (float)renderEffects.x1 * 1.5F, y + (float)renderEffects.y1 * 1.5F, z, col, this.getRenderYOffset() + offset
                        );
                    }
                } else {
                    renderStatus = IsoObjectModelDrawer.renderMain(spriteModel, x, y, z, col, this.getRenderYOffset() + offset, animationPlayer);
                }

                colOrig.a = a;
                if (renderStatus == IsoObjectModelDrawer.RenderStatus.Loading
                    && PerformanceSettings.fboRenderChunk
                    && FBORenderChunkManager.instance.isCaching()) {
                    FBORenderCell.instance.handleDelayedLoading(this);
                    return true;
                } else {
                    return renderStatus == IsoObjectModelDrawer.RenderStatus.Ready;
                }
            }
        }
    }

    public boolean isAnimating() {
        return this.animating;
    }

    public void setAnimating(boolean bAnimating) {
        this.animating = bAnimating;
    }

    public void onAnimationFinished() {
        this.setAnimating(false);
    }

    protected void updateRenderInfoForObjectPicker(float x, float y, float z, ColorInfo info) {
        float ox = x;
        float oy = y;
        Texture tex = this.sprite.getTextureForCurrentFrame(this.getDir());
        if (tex != null) {
            IsoSpriteInstance inst = this.sprite.def;
            if (Core.tileScale == 2 && tex.getWidthOrig() == 64 && tex.getHeightOrig() == 128) {
                inst.setScale(2.0F, 2.0F);
            }

            if (Core.tileScale == 2 && inst.scaleX == 2.0F && inst.scaleY == 2.0F && tex.getWidthOrig() == 128 && tex.getHeightOrig() == 256) {
                inst.setScale(1.0F, 1.0F);
            }

            float scaleX = inst.scaleX;
            float scaleY = inst.scaleY;
            if (FBORenderChunkManager.instance.isCaching()) {
                x = PZMath.coordmodulof(x, 8);
                y = PZMath.coordmodulof(y, 8);
                if (this.getSquare() != this.getRenderSquare()) {
                    x = ox - this.getRenderSquare().chunk.wx * 8;
                    y = oy - this.getRenderSquare().chunk.wy * 8;
                }

                this.sx = 0.0F;
            }

            if (this.sx == 0.0F) {
                this.sx = IsoUtils.XToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
                this.sy = IsoUtils.YToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
                this.sx = this.sx - this.offsetX;
                this.sy = this.sy - (this.offsetY + this.renderYOffset * Core.tileScale);
            }

            int playerIndex = IsoCamera.frameState.playerIndex;
            ObjectRenderInfo renderInfo = this.getRenderInfo(playerIndex);
            if (FBORenderObjectHighlight.getInstance().isRendering() || FBORenderObjectOutline.getInstance().isRendering()) {
                boolean var13 = true;
            } else if (FBORenderCell.instance.renderTranslucentOnly) {
                renderInfo.renderX = this.sx - IsoCamera.frameState.offX;
                renderInfo.renderY = this.sy - IsoCamera.frameState.offY;
            } else if (FBORenderChunkManager.instance.renderChunk.highRes) {
                renderInfo.renderX = this.sx + FBORenderChunkManager.instance.getXOffset() - FBORenderChunkManager.instance.renderChunk.w / 4.0F;
                renderInfo.renderY = this.sy + FBORenderChunkManager.instance.getYOffset();
            } else {
                renderInfo.renderX = this.sx + FBORenderChunkManager.instance.getXOffset();
                renderInfo.renderY = this.sy + FBORenderChunkManager.instance.getYOffset();
            }

            renderInfo.renderWidth = tex.getWidthOrig() * scaleX;
            renderInfo.renderHeight = tex.getHeightOrig() * scaleY;
            renderInfo.renderScaleX = scaleX;
            renderInfo.renderScaleY = scaleY;
            renderInfo.renderAlpha = info.a;
        }
    }

    public boolean isGrave() {
        return this.getSprite() == null
                || this.getSprite().getProperties() == null
                || !this.getSprite().getProperties().has("CustomName")
                || !Objects.requireNonNull(this.getSprite().getProperties().get("CustomName")).contains("Grave")
                    && !this.getSprite().getProperties().get("CustomName").contains("grave")
            ? this.getSprite() != null && this.getSprite().getName() != null && this.getSprite().getName().contains("cemetary")
            : true;
    }

    public IsoSpriteInstance getOnOverlay() {
        return this.onOverlay;
    }

    public void setOnOverlay(IsoSpriteInstance inst) {
        this.onOverlay = inst;
    }

    public void clearOnOverlay() {
        if (this.getOnOverlay() != null) {
            IsoSpriteInstance.add(this.getOnOverlay());
            this.setOnOverlay(null);
        }
    }

    public boolean shouldShowOnOverlay() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        return this.getSquare() != null && this.getSquare().isSeen(playerIndex) ? this.checkObjectPowered() : false;
    }

    public IsoLightSource getLightSource() {
        return this.lightSource;
    }

    public void setLightSource(IsoLightSource lightSource) {
        this.lightSource = lightSource;
    }

    protected boolean shouldLightSourceBeActive() {
        return this.checkObjectPowered();
    }

    protected void addLightSourceToWorld() {
        if (!GameServer.server) {
            IsoGridSquare square = this.getSquare();
            if (square != null) {
                if (!(this instanceof IsoFire)) {
                    if (!(this instanceof IsoFireplace)) {
                        if (!(this instanceof IsoLightSwitch)) {
                            if (!(this instanceof IsoMovingObject)) {
                                if (!(this instanceof IsoTelevision)) {
                                    PropertyContainer properties = this.getProperties();
                                    if (this.lightSource == null
                                        && properties != null
                                        && properties.has("lightR")
                                        && properties.has("lightG")
                                        && properties.has("lightB")) {
                                        float r = Float.parseFloat(properties.get("lightR")) / 255.0F;
                                        float g = Float.parseFloat(properties.get("lightG")) / 255.0F;
                                        float b = Float.parseFloat(properties.get("lightB")) / 255.0F;
                                        int radius = 10;
                                        if (properties.has("LightRadius") && Integer.parseInt(properties.get("LightRadius")) > 0) {
                                            radius = Integer.parseInt(this.sprite.getProperties().get("LightRadius"));
                                        }

                                        this.lightSource = new IsoLightSource(square.getX(), square.getY(), square.getZ(), r, g, b, radius);
                                        this.lightSource.active = this.shouldLightSourceBeActive();
                                        this.lightSource.hydroPowered = false;
                                    }

                                    if (this.lightSource != null) {
                                        IsoWorld.instance.currentCell.addLamppost(this.lightSource);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void removeLightSourceFromWorld() {
        if (this.lightSource != null) {
            IsoWorld.instance.currentCell.removeLamppost(this.lightSource);
            this.lightSource = null;
        }
    }

    public void checkLightSourceActive() {
        if (this.getLightSource() != null) {
            boolean bShouldBeActive = this.shouldLightSourceBeActive();
            if (this.getLightSource().isActive() != bShouldBeActive) {
                this.getLightSource().setActive(bShouldBeActive);
                IsoGridSquare.setRecalcLightTime(-1.0F);
                GameTime.instance.lightSourceUpdate = 100.0F;
            }
        }
    }

    public boolean isGenericCraftingSurface() {
        if (this.getSprite() != null && this.getSprite().getProperties() != null) {
            PropertyContainer props = this.getSprite().getProperties();
            if (Objects.equals(props.get("GenericCraftingSurface"), "true")) {
                return true;
            } else {
                return Objects.equals(props.get("GenericCraftingSurface"), "false")
                    ? false
                    : props.getSurface() >= 24 && props.getSurface() <= 47 && !props.isSurfaceOffset();
            }
        } else {
            return false;
        }
    }

    public boolean isBush() {
        return this.sprite != null && ("f_bushes_1".equals(this.sprite.tilesetName) || this.sprite.getProperties().get("Bush") != null);
    }

    public boolean isFascia() {
        PropertyContainer props = this.getProperties();
        if (props == null) {
            return false;
        } else {
            String FasciaEdge = props.get("FasciaEdge");
            if (FasciaEdge == null) {
                return false;
            } else {
                IsoGridSquare square = this.getSquare();
                switch (FasciaEdge) {
                    case "North":
                        if (square.getWallExcludingObject(true, this) != null) {
                            return false;
                        }
                        break;
                    case "South":
                        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                        if (squareS == null || squareS.getWallExcludingObject(true, this) != null) {
                            return false;
                        }
                        break;
                    case "East":
                        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
                        if (squareE == null || squareE.getWallExcludingObject(false, this) != null) {
                            return false;
                        }
                        break;
                    case "West":
                        if (square.getWallExcludingObject(false, this) != null) {
                            return false;
                        }
                    case "NorthEastCorner":
                    case "SouthEastCorner":
                    case "SouthWestCorner":
                    case "NorthWestCorner":
                }

                return true;
            }
        }
    }

    public IsoGridSquare getFasciaAttachedSquare() {
        IsoCell cell = IsoWorld.instance.currentCell;
        IsoGridSquare square = this.getSquare();
        PropertyContainer props = this.getProperties();
        String FasciaEdge = props.get("FasciaEdge");
        boolean FasciaEdgeReversible = StringUtils.equals(props.get("FasciaEdgeReversible"), "true");
        switch (FasciaEdge) {
            case "North":
                IsoGridSquare square1xxxxxxxx = cell.getGridSquare(square.getX(), square.getY() - 1, square.getZ() + 1);
                if (square1xxxxxxxx != null) {
                    return square1xxxxxxxx;
                }

                if (FasciaEdgeReversible) {
                    square1xxxxxxxx = cell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1);
                    if (square1xxxxxxxx != null) {
                        return square1xxxxxxxx;
                    }
                }
                break;
            case "South":
                IsoGridSquare square1xxxxxxx = cell.getGridSquare(square.getX(), square.getY() + 1, square.getZ() + 1);
                if (square1xxxxxxx != null) {
                    return square1xxxxxxx;
                }
                break;
            case "East":
                IsoGridSquare square1xxxxxx = cell.getGridSquare(square.getX() + 1, square.getY(), square.getZ() + 1);
                if (square1xxxxxx != null) {
                    return square1xxxxxx;
                }
                break;
            case "West":
                IsoGridSquare square1xxxxx = cell.getGridSquare(square.getX() - 1, square.getY(), square.getZ() + 1);
                if (square1xxxxx != null) {
                    return square1xxxxx;
                }

                if (FasciaEdgeReversible) {
                    square1xxxxx = cell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1);
                    if (square1xxxxx != null) {
                        return square1xxxxx;
                    }
                }
                break;
            case "NorthEastCorner":
                IsoGridSquare square1xxxx = cell.getGridSquare(square.getX() + 1, square.getY() - 1, square.getZ() + 1);
                if (square1xxxx != null) {
                    return square1xxxx;
                }
                break;
            case "SouthEastCorner":
                IsoGridSquare square1xxx = cell.getGridSquare(square.getX() + 1, square.getY() + 1, square.getZ() + 1);
                if (square1xxx != null) {
                    return square1xxx;
                }
                break;
            case "SouthWestCorner":
                IsoGridSquare square1xx = cell.getGridSquare(square.getX() - 1, square.getY() + 1, square.getZ() + 1);
                if (square1xx != null) {
                    return square1xx;
                }
                break;
            case "NorthWestCorner":
                IsoGridSquare square1x = cell.getGridSquare(square.getX() - 1, square.getY() - 1, square.getZ() + 1);
                if (square1x != null) {
                    return square1x;
                }
                break;
            case "NorthAndEast":
            case "SouthAndEast":
            case "SouthAndWest":
            case "NorthAndWest":
                IsoGridSquare square1 = cell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1);
                if (square1 != null) {
                    return square1;
                }
        }

        return null;
    }

    public void setExplored(boolean isExplored) {
        if (this.getContainer() != null) {
            this.getContainer().setExplored(isExplored);
        }

        if (this.secondaryContainers != null) {
            for (int i = 0; i < this.secondaryContainers.size(); i++) {
                ItemContainer container = this.secondaryContainers.get(i);
                container.setExplored(isExplored);
            }
        }
    }

    public void flagForHotSave() {
        if (this.square != null && this.square.getChunk() != null) {
            this.square.getChunk().flagForHotSave();
        }
    }

    public boolean hasGridPower() {
        return this.getSquare() != null && this.getSquare().hasGridPower();
    }

    public boolean isObjectNoContainerOrEmpty() {
        for (int i = 0; i < this.getContainerCount(); i++) {
            ItemContainer con = this.getContainerByIndex(i);
            if (con != null && (con.getItems() != null && !con.getItems().isEmpty() || !con.isExplored())) {
                return false;
            }
        }

        ArrayList<IsoObject> multiSquareObjects = new ArrayList<>();
        if (this.hasComponent(ComponentType.SpriteConfig)) {
            this.<SpriteConfig>getComponent(ComponentType.SpriteConfig).getAllMultiSquareObjects(multiSquareObjects);
        } else {
            multiSquareObjects.add(this);
        }

        for (IsoObject subObject : multiSquareObjects) {
            for (int ix = 0; ix < subObject.componentSize(); ix++) {
                Component component = subObject.getComponentForIndex(ix);
                if (component != null && !component.isNoContainerOrEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void dumpContentsInSquare() {
        if (this.getSquare() != null) {
            IsoObjectUtils.dumpContentsInSquare(this);
        }
    }

    public boolean isPropaneBBQ() {
        return this instanceof IsoBarbecue && this.getSprite() != null && this.getProperties().has("propaneTank");
    }

    public boolean hasPropaneTank() {
        return this.isPropaneBBQ() && this.hasPropaneTank();
    }

    public boolean isFireInteractionObject() {
        return this instanceof IsoFireplace || this instanceof IsoBarbecue;
    }

    public void setLit(boolean lit) {
    }

    public boolean isLit() {
        return false;
    }

    public void turnOn() {
        if (!this.isLit()) {
            this.setLit(true);
            if (this.getContainer() != null) {
                this.getContainer().addItemsToProcessItems();
            }
        }
    }

    public boolean checkObjectPowered() {
        if (IngameState.instance == null) {
            return ItemContainer.isObjectPowered(this);
        } else if (IngameState.instance.numberTicks == this.hasPowerTick) {
            return this.hasPower;
        } else {
            this.hasPowerTick = IngameState.instance.numberTicks;
            return this.hasPower = ItemContainer.isObjectPowered(this);
        }
    }

    public boolean isStump() {
        String customName = this.getProperty("CustomName");
        return customName == null ? false : customName.equals("Small Stump") || customName.equals("Tree Stump") || customName.equals("Stump");
    }

    public boolean isOre() {
        String customName = this.getProperty("CustomName");
        return customName != null
            && (
                customName.contains("ironOre")
                    || customName.contains("copperOre")
                    || customName.contains("FlintBoulder")
                    || customName.contains("LimestoneBoulder")
            );
    }

    public boolean hasAdjacentCanStandSquare() {
        return this.getSquare() != null && this.getSquare().hasAdjacentCanStandSquare();
    }

    public boolean isWindow() {
        return this.sprite != null && (this.sprite.getProperties().has(IsoFlagType.WindowN) || this.sprite.getProperties().has(IsoFlagType.WindowW));
    }

    public boolean isNorthBlocked() {
        if (this.sprite == null) {
            return false;
        } else {
            PropertyContainer properties = this.getProperties();
            if (properties == null) {
                return false;
            } else {
                for (IsoFlagType isoFlagType : NORTH_FLAGS) {
                    if (properties.has(isoFlagType)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    static {
        initFactory();
    }

    public static class IsoObjectFactory {
        private final byte classId;
        private final String objectName;
        private final int hashCode;

        public IsoObjectFactory(byte classId, String objectName) {
            this.classId = classId;
            this.objectName = objectName;
            this.hashCode = objectName.hashCode();
        }

        protected IsoObject InstantiateObject(IsoCell cell) {
            return new IsoObject(cell);
        }

        public byte getClassID() {
            return this.classId;
        }

        public String getObjectName() {
            return this.objectName;
        }
    }

    public static class OutlineShader {
        public static final IsoObject.OutlineShader instance = new IsoObject.OutlineShader();
        private ShaderProgram shaderProgram;
        private int stepSize;
        private int outlineColor;

        public void initShader() {
            this.shaderProgram = ShaderProgram.createShaderProgram("outline", false, false, true);
            if (this.shaderProgram.isCompiled()) {
                this.stepSize = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "stepSize");
                this.outlineColor = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "outlineColor");
                ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
                GL20.glUniform2f(this.stepSize, 0.001F, 0.001F);
                ShaderHelper.glUseProgramObjectARB(0);
            }
        }

        public void setOutlineColor(float r, float g, float b, float a) {
            SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.outlineColor, r, g, b, a);
        }

        public void setStepSize(float stepSize, int texWidth, int texHeight) {
            SpriteRenderer.instance.ShaderUpdate2f(this.shaderProgram.getShaderID(), this.stepSize, stepSize / texWidth, stepSize / texHeight);
        }

        public boolean StartShader() {
            if (this.shaderProgram == null) {
                RenderThread.invokeOnRenderContext(this::initShader);
            }

            if (this.shaderProgram.isCompiled()) {
                IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
                return true;
            } else {
                return false;
            }
        }
    }

    public static enum VisionResult {
        NoEffect,
        Blocked,
        Unblocked;
    }
}
