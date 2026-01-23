// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.audio.BaseSoundEmitter;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.UnderwearDefinition;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.stash.StashSystem;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityFactory;
import zombie.entity.GameEntityType;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.WeaponPart;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.MediaData;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.BookSubject;
import zombie.scripting.objects.CoverType;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemReplacement;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.MagazineSubject;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.SoundMapKey;
import zombie.scripting.objects.WeaponCategory;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.VehiclePart;
import zombie.world.ItemInfo;
import zombie.world.WorldDictionary;

@UsedFromLua
public class InventoryItem extends GameEntity {
    private static final ByteBuffer tempBuffer = ByteBuffer.allocate(20000);
    protected static final int DEFAULT_USES = 1;
    protected IsoGameCharacter previousOwner;
    protected Item scriptItem;
    protected ItemType itemType;
    protected ItemContainer container;
    protected int containerX;
    protected int containerY;
    protected String name;
    protected String replaceOnUse;
    protected String replaceOnUseFullType;
    protected int conditionMax = 10;
    protected ItemContainer rightClickContainer;
    protected Texture texture;
    protected Texture texturerotten;
    protected Texture textureCooked;
    protected Texture textureBurnt;
    protected String type;
    protected String fullType;
    protected int uses = 1;
    protected float age;
    protected float lastAged = -1.0F;
    protected boolean isCookable;
    protected float cookingTime;
    protected float minutesToCook = 60.0F;
    protected float minutesToBurn = 120.0F;
    public boolean cooked;
    protected boolean burnt;
    protected int offAge = 1000000000;
    protected int offAgeMax = 1000000000;
    protected float weight = 1.0F;
    protected float actualWeight = 1.0F;
    protected String worldTexture;
    protected String description;
    protected int condition = 10;
    protected String offString = Translator.getText("Tooltip_food_Rotten");
    protected String freshString = Translator.getText("Tooltip_food_Fresh");
    protected String staleString = Translator.getText("Tooltip_food_Stale");
    protected String cookedString = Translator.getText("Tooltip_food_Cooked");
    protected String toastedString = Translator.getText("Tooltip_food_Toasted");
    protected String grilledString = Translator.getText("Tooltip_food_Grilled");
    protected String unCookedString = Translator.getText("Tooltip_food_Uncooked");
    protected String frozenString = Translator.getText("Tooltip_food_Frozen");
    protected String burntString = Translator.getText("Tooltip_food_Burnt");
    protected String emptyString = Translator.getText("ContextMenu_Empty");
    private final String brokenString = Translator.getText("Tooltip_broken");
    private final String bluntString = Translator.getText("Tooltip_blunt");
    private final String dullString = Translator.getText("Tooltip_dull");
    private final String wornString = Translator.getText("IGUI_ClothingName_Worn");
    private final String bloodyString = Translator.getText("IGUI_ClothingName_Bloody");
    private final String activatedString = Translator.getText("Tooltip_activated");
    protected String module = "Base";
    protected float boredomChange;
    protected float unhappyChange;
    protected float stressChange;
    protected int foodSicknessChange;
    protected int inverseCoughProbability;
    protected int inverseCoughProbabilitySmoker;
    protected ArrayList<IsoObject> taken = new ArrayList<>();
    protected IsoDirections placeDir = IsoDirections.Max;
    protected IsoDirections newPlaceDir = IsoDirections.Max;
    private KahluaTable table;
    public String replaceOnUseOn;
    public Color col = Color.white;
    public boolean canStack;
    private boolean activated;
    private boolean isTorchCone;
    private int lightDistance;
    private int count = 1;
    public float fatigueChange;
    public IsoWorldInventoryObject worldItem;
    public IsoDeadBody deadBodyObject;
    private String customMenuOption;
    private String tooltip;
    private String displayCategory;
    private int haveBeenRepaired;
    private boolean broken;
    private String originalName;
    public int id;
    public boolean requiresEquippedBothHands;
    public ByteBuffer byteData;
    public ArrayList<String> extraItems = new ArrayList<>();
    private boolean customName;
    private String breakSound;
    protected boolean alcoholic;
    private float alcoholPower;
    private float bandagePower;
    private float reduceInfectionPower;
    private boolean customWeight;
    private boolean customColor;
    private int keyId = -1;
    private boolean remoteController;
    private boolean canBeRemote;
    private int remoteControlId = -1;
    private int remoteRange;
    private float colorRed = 1.0F;
    private float colorGreen = 1.0F;
    private float colorBlue = 1.0F;
    private String countDownSound;
    private String explosionSound;
    private IsoGameCharacter equipParent;
    private String evolvedRecipeName;
    private float metalValue;
    private float itemHeat = 1.0F;
    private float meltingTime;
    private String worker;
    private boolean isWet;
    private float wetCooldown = -1.0F;
    private String itemWhenDry;
    private boolean favorite;
    protected ArrayList<String> requireInHandOrInventory;
    private String stashMap;
    private boolean zombieInfected;
    private float itemCapacity = -1.0F;
    private int maxCapacity = -1;
    private float brakeForce;
    private float durability;
    private int chanceToSpawnDamaged;
    private float conditionLowerNormal;
    private float conditionLowerOffroad;
    private float wheelFriction;
    private float suspensionDamping;
    private float suspensionCompression;
    private float engineLoudness;
    protected ItemVisual visual;
    protected String staticModel;
    private ArrayList<String> iconsForTexture;
    private ArrayList<BloodClothingType> bloodClothingType = new ArrayList<>();
    private int stashChance = 80;
    private AmmoType ammoType;
    private int maxAmmo;
    private int currentAmmoCount;
    private String gunType;
    private String attachmentType;
    private ArrayList<String> attachmentsProvided;
    private int attachedSlot = -1;
    private String attachedSlotType;
    private String attachmentReplacement;
    private String attachedToModel;
    private final String alternateModelName = null;
    private short registryId = -1;
    public float worldScale = 1.0F;
    public float worldXRotation;
    public float worldYRotation;
    public float worldZRotation = -1.0F;
    public float worldAlpha = 1.0F;
    private short recordedMediaIndex = -1;
    private byte mediaType = -1;
    private boolean isInitialised;
    public WorldItemAtlas.ItemTexture atlasTexture;
    protected Texture textureColorMask;
    protected Texture textureFluidMask;
    private AnimalTracks animalTracks;
    private ArrayList<String> staticModelsByIndex;
    private ArrayList<String> worldStaticModelsByIndex;
    private boolean doingExtendedPlacement;
    private int modelIndex = -1;
    private final int maxTextLength = 256;
    private IsoPlayer equippedAndActivatedPlayer;
    private long equippedAndActivatedSound;
    private boolean isCraftingConsumed;
    public float jobDelta;
    public String jobType;
    public String mainCategory;
    private boolean canBeActivated;
    private float lightStrength;
    public String closeKillMove;
    private float useDelta = 0.03125F;
    private boolean beingFilled;

    public CoverType getCoverType() {
        boolean hasHardcover = this.hasTag(ItemTag.HARDCOVER, ItemTag.HOLLOW_BOOK, ItemTag.FANCY_BOOK);
        boolean hasSoftcover = this.hasTag(ItemTag.SOFTCOVER);
        if (hasHardcover != hasSoftcover) {
            return hasHardcover ? CoverType.HARDCOVER : CoverType.SOFTCOVER;
        } else {
            return CoverType.BOTH;
        }
    }

    public List<BookSubject> getBookSubjects() {
        return this.getScriptItem().bookSubjects;
    }

    public List<MagazineSubject> getMagazineSubjects() {
        return this.getScriptItem().magazineSubjects;
    }

    public IsoWorldInventoryObject getWorldItem() {
        return this.worldItem;
    }

    public void setEquipParent(IsoGameCharacter parent) {
        this.setEquipParent(parent, true);
    }

    public void setEquipParent(IsoGameCharacter parent, boolean register) {
        this.equipParent = parent;
        if (this.equipParent == null) {
            this.onUnEquip();
        } else {
            this.onEquip(register);
        }
    }

    public IsoGameCharacter getEquipParent() {
        return this.equipParent == null || this.equipParent.getPrimaryHandItem() != this && this.equipParent.getSecondaryHandItem() != this
            ? null
            : this.equipParent;
    }

    public String getBringToBearSound() {
        return this.scriptItem.getBringToBearSound();
    }

    public String getAimReleaseSound() {
        return this.scriptItem.getAimReleaseSound();
    }

    public String getEquipSound() {
        return this.scriptItem.getEquipSound();
    }

    public String getUnequipSound() {
        return this.scriptItem.getUnequipSound();
    }

    public String getDropSound() {
        if (StringUtils.equalsIgnoreCase(this.getType(), "CorpseAnimal")) {
            IsoDeadBody corpse = this.loadCorpseFromByteData(null);
            if (corpse != null && corpse.isAnimal()) {
                AnimalDefinitions def = AnimalDefinitions.getDef(corpse.getAnimalType());
                if (def == null) {
                    return this.scriptItem.getDropSound();
                } else {
                    AnimalBreed breed = def.getBreedByName(corpse.getBreed());
                    if (breed == null) {
                        return this.scriptItem.getDropSound();
                    } else {
                        AnimalBreed.Sound sound = breed.getSound("put_down_corpse");
                        return sound == null ? this.scriptItem.getDropSound() : sound.soundName;
                    }
                }
            } else {
                return this.scriptItem.getDropSound();
            }
        } else {
            return this.scriptItem.getDropSound();
        }
    }

    public void setWorldItem(IsoWorldInventoryObject w) {
        this.worldItem = w;
    }

    public void setJobDelta(float delta) {
        this.jobDelta = delta;
    }

    public float getJobDelta() {
        return this.jobDelta;
    }

    public void setJobType(String type) {
        this.jobType = type;
    }

    public String getJobType() {
        return this.jobType;
    }

    public boolean hasModData() {
        return this.table != null && !this.table.isEmpty();
    }

    public KahluaTable getModData() {
        if (this.table == null) {
            this.table = LuaManager.platform.newTable();
        }

        return this.table;
    }

    public void storeInByteData(IsoObject o) {
        tempBuffer.clear();

        try {
            o.save(tempBuffer, false);
        } catch (IOException var3) {
            var3.printStackTrace();
        }

        tempBuffer.flip();
        if (this.byteData == null || this.byteData.capacity() < tempBuffer.limit() - 2 + 8) {
            this.byteData = ByteBuffer.allocate(tempBuffer.limit() - 2 + 8);
        }

        tempBuffer.get();
        tempBuffer.get();
        this.byteData.clear();
        this.byteData.put((byte)87);
        this.byteData.put((byte)86);
        this.byteData.put((byte)69);
        this.byteData.put((byte)82);
        this.byteData.putInt(241);
        this.byteData.put(tempBuffer);
        this.byteData.flip();
    }

    public ByteBuffer getByteData() {
        return this.byteData;
    }

    public IsoDeadBody loadCorpseFromByteData(IsoGridSquare square) {
        if (this.getByteData() == null) {
            return this.createAndStoreDefaultDeadBody(square);
        } else {
            Object var4;
            try {
                try {
                    return this.tryLoadCorpseFromByteData(square);
                } catch (IOException var9) {
                    ExceptionLogger.logException(var9);
                }

                try {
                    return this.createDefaultDeadBody(square);
                } catch (Throwable var10) {
                    ExceptionLogger.logException(var10);
                    var4 = null;
                }
            } finally {
                this.getByteData().rewind();
            }

            return (IsoDeadBody)var4;
        }
    }

    private IsoDeadBody tryLoadCorpseFromByteData(IsoGridSquare square) throws IOException {
        this.getByteData().rewind();
        byte b1 = this.getByteData().get();
        byte b2 = this.getByteData().get();
        byte b3 = this.getByteData().get();
        byte b4 = this.getByteData().get();
        if (b1 == 87 && b2 == 86 && b3 == 69 && b4 == 82) {
            int worldVersion = this.getByteData().getInt();
            IsoDeadBody corpse = new IsoDeadBody(IsoWorld.instance.currentCell);
            corpse.load(this.getByteData(), worldVersion);
            if ("CorpseAnimal".equalsIgnoreCase(this.getType())) {
                Object skeleton = this.hasModData() ? this.getModData().rawget("skeleton") : null;
                if (skeleton != null && "true".equalsIgnoreCase(skeleton.toString())) {
                    corpse.getModData().rawset("skeleton", "true");
                }

                double ageHours = this.getAge() * 24.0;
                if (corpse.getModData().rawget("deathAge") instanceof Double deathAge) {
                    ageHours += deathAge;
                    ageHours -= PZMath.min((float)ageHours, corpse.getInitialItemAge(this) * 24.0F);
                }

                corpse.setDeathTime((float)(GameTime.getInstance().getWorldAgeHours() - ageHours));
                corpse.getModData().rawset("deathAge", ageHours);
            }

            if (square != null) {
                corpse.setSquare(square);
                corpse.setCurrent(square);
            }

            return corpse;
        } else {
            throw new IOException("expected 'WVER' signature in byteData");
        }
    }

    public boolean isForceDropHeavyItem() {
        return this.isHumanCorpse()
            || "Generator".equalsIgnoreCase(this.getType())
            || this.hasTag(ItemTag.HEAVY_ITEM)
            || "Animal".equalsIgnoreCase(this.getType());
    }

    public boolean isHumanCorpse() {
        String itemType = this.getType();
        return "CorpseFemale".equalsIgnoreCase(itemType) ? true : "CorpseMale".equalsIgnoreCase(itemType);
    }

    public boolean isAnimalCorpse() {
        String itemType = this.getType();
        return "CorpseAnimal".equalsIgnoreCase(itemType);
    }

    private IsoDeadBody createDefaultDeadBody(IsoGridSquare square) throws Throwable {
        if (this.isHumanCorpse()) {
            IsoZombie zombie = new IsoZombie(IsoWorld.instance.currentCell);
            zombie.setDir(IsoDirections.fromIndex(Rand.Next(8)));
            zombie.setForwardDirection(zombie.dir.ToVector());
            zombie.setFakeDead(false);
            zombie.setHealth(0.0F);
            if (square != null) {
                zombie.dressInRandomOutfit();
            } else if (!zombie.isSkeleton()) {
                String roomName = null;
                Outfit outfit = ZombiesZoneDefinition.getRandomDefaultOutfit(zombie.isFemale(), roomName);
                UnderwearDefinition.addRandomUnderwear(zombie);
                zombie.dressInPersistentOutfit(outfit.name);
            }

            zombie.DoZombieInventory();
            if (square != null) {
                zombie.setSquare(square);
                zombie.setCurrent(square);
            }

            return new IsoDeadBody(zombie, true, square != null);
        } else if (this.isAnimalCorpse()) {
            AnimalDefinitions def = PZArrayUtil.pickRandom(AnimalDefinitions.getAnimalDefsArray());
            if (def == null) {
                return null;
            } else {
                AnimalBreed breed = def.getRandomBreed();
                if (breed == null) {
                    return null;
                } else {
                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.currentCell, 0, 0, 0, def.getAnimalType(), breed.getName());
                    animal.setDir(IsoDirections.fromIndex(Rand.Next(8)));
                    animal.setForwardDirection(animal.getDir().ToVector());
                    animal.setHealth(0.0F);
                    if (square != null) {
                        animal.setSquare(square);
                        animal.setCurrent(square);
                    }

                    IsoDeadBody corpse = new IsoDeadBody(animal, true, square != null);
                    this.copyModData(corpse.getModData());
                    this.setIcon(Texture.getSharedTexture(corpse.invIcon));
                    if (corpse.isAnimalSkeleton()) {
                        this.setName(Translator.getText("IGUI_Item_AnimalSkeleton", corpse.customName));
                    } else {
                        this.setName(Translator.getText("IGUI_Item_AnimalCorpse", corpse.customName));
                    }

                    this.setCustomName(true);
                    this.setActualWeight(corpse.weight);
                    this.setWeight(corpse.weight);
                    this.setCustomWeight(true);
                    return corpse;
                }
            }
        } else {
            return null;
        }
    }

    public IsoDeadBody createAndStoreDefaultDeadBody(IsoGridSquare square) {
        try {
            IsoDeadBody corpse = this.createDefaultDeadBody(square);
            if (corpse != null) {
                this.storeInByteData(corpse);
            }

            return corpse;
        } catch (Throwable var3) {
            ExceptionLogger.logException(var3);
            return null;
        }
    }

    public boolean isRequiresEquippedBothHands() {
        return this.requiresEquippedBothHands;
    }

    public float getA() {
        return this.col.a;
    }

    public float getR() {
        return this.col.r;
    }

    public float getG() {
        return this.col.g;
    }

    public float getB() {
        return this.col.b;
    }

    public InventoryItem(String module, String name, String type, String tex) {
        this.col = Color.white;
        this.texture = Texture.trygetTexture(tex);
        if (this.texture == null) {
            this.texture = Texture.getSharedTexture("media/inventory/Question_On.png");
        }

        this.module = module;
        this.name = name;
        this.originalName = name;
        this.type = type;
        this.fullType = module + "." + type;
        this.worldTexture = tex.replace("Item_", "media/inventory/world/WItem_");
        this.worldTexture = this.worldTexture + ".png";
    }

    public InventoryItem(String module, String name, String type, Item item) {
        this.col = Color.white;
        this.texture = item.normalTexture;
        this.module = module;
        this.name = name;
        this.originalName = name;
        this.type = type;
        this.fullType = module + "." + type;
        this.worldTexture = item.worldTextureName;
    }

    public String getType() {
        return this.type;
    }

    public Texture getTex() {
        return this.texture;
    }

    public String getCategory() {
        return this.mainCategory != null ? this.mainCategory : "Item";
    }

    public boolean UseForCrafting(int uses) {
        return false;
    }

    public boolean IsRotten() {
        return this.age > this.offAge;
    }

    public float HowRotten() {
        if (this.offAgeMax - this.offAge == 0) {
            return this.age > this.offAge ? 1.0F : 0.0F;
        } else {
            return (this.age - this.offAge) / (this.offAgeMax - this.offAge);
        }
    }

    public boolean CanStack(InventoryItem item) {
        return false;
    }

    public boolean ModDataMatches(InventoryItem item) {
        KahluaTable t = item.getModData();
        KahluaTable t2 = item.getModData();
        if (t == null && t2 == null) {
            return true;
        } else if (t == null) {
            return false;
        } else if (t2 == null) {
            return false;
        } else if (t.len() != t2.len()) {
            return false;
        } else {
            KahluaTableIterator it = t.iterator();

            while (it.advance()) {
                Object a = t2.rawget(it.getKey());
                Object b = it.getValue();
                if (!a.equals(b)) {
                    return false;
                }
            }

            return true;
        }
    }

    public void DoTooltip(ObjectTooltip tooltipUI) {
        this.DoTooltipEmbedded(tooltipUI, null, 0);
    }

    public void DoTooltipEmbedded(ObjectTooltip tooltipUI, ObjectTooltip.Layout layoutOverride, int offsetY) {
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int lineSpacing = tooltipUI.getLineSpacing();
        int iconSize = lineSpacing;
        int y = tooltipUI.padTop + offsetY;
        IsoPlayer player = Type.tryCastTo(tooltipUI.getCharacter(), IsoPlayer.class);
        String s;
        if (player != null) {
            s = this.getName(player);
        } else {
            s = this.getName();
        }

        tooltipUI.DrawText(font, s, tooltipUI.padLeft, y, 1.0, 1.0, 0.8F, 1.0);
        tooltipUI.adjustWidth(tooltipUI.padLeft, s);
        ColorInfo highlightGood = Core.getInstance().getGoodHighlitedColor();
        ColorInfo highlightBad = Core.getInstance().getBadHighlitedColor();
        float goodR = highlightGood.getR();
        float goodG = highlightGood.getG();
        float goodB = highlightGood.getB();
        float badR = highlightBad.getR();
        float badG = highlightBad.getG();
        float badB = highlightBad.getB();
        y += lineSpacing + 5;
        if (this.extraItems != null && !this.extraItems.isEmpty()) {
            tooltipUI.DrawText(font, Translator.getText("Tooltip_item_Contains"), tooltipUI.padLeft, y, 1.0, 1.0, 0.8F, 1.0);
            int x = tooltipUI.padLeft + TextManager.instance.MeasureStringX(font, Translator.getText("Tooltip_item_Contains")) + 4;
            int dy = (lineSpacing - lineSpacing) / 2;

            for (int i = 0; i < this.extraItems.size(); i++) {
                InventoryItem item = InventoryItemFactory.CreateItem(this.extraItems.get(i));
                if (!this.isCookable && item.isCookable) {
                    item.setCooked(true);
                }

                if (this.isCooked() && item.isCookable) {
                    item.setCooked(true);
                }

                float dx = this.drawTooltipItemTexture(tooltipUI, item.getTex(), x, y + dy, iconSize, iconSize, 1.0F, 1.0F, 1.0F, 1.0F);
                x = x + (int)PZMath.ceil(dx) + 2;
            }

            y = y + lineSpacing + 5;
        }

        if (this instanceof Food && ((Food)this).spices != null) {
            tooltipUI.DrawText(font, Translator.getText("Tooltip_item_Spices"), tooltipUI.padLeft, y, 1.0, 1.0, 0.8F, 1.0);
            int x = tooltipUI.padLeft + TextManager.instance.MeasureStringX(font, Translator.getText("Tooltip_item_Spices")) + 4;
            int dy = (lineSpacing - iconSize) / 2;

            for (int i = 0; i < ((Food)this).spices.size(); i++) {
                InventoryItem itemx = InventoryItemFactory.CreateItem(((Food)this).spices.get(i));
                float dx = this.drawTooltipItemTexture(tooltipUI, itemx.getTex(), x, y + dy, iconSize, iconSize, 1.0F, 1.0F, 1.0F, 1.0F);
                x = x + (int)PZMath.ceil(dx) + 2;
            }

            y = y + lineSpacing + 5;
        }

        ObjectTooltip.Layout layout;
        if (layoutOverride != null) {
            layout = layoutOverride;
            layoutOverride.offsetY = y;
        } else {
            layout = tooltipUI.beginLayout();
            layout.setMinLabelWidth(80);
        }

        if (SandboxOptions.instance.isUnstableScriptNameSpam()) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Item Report") + ":", 1.0F, 0.4F, 0.7F, 1.0F);
            itemx.setValue(this.getFullType(), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (player != null && this.scriptItem != null && !this.scriptItem.getResearchableRecipes(player).isEmpty()) {
            ColorInfo color = Core.getInstance().getGoodHighlitedColor();
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            if (this.scriptItem.getResearchableRecipes(player).size() == 1) {
                itemx.setLabel(Translator.getText("Tooltip_item_CanResearch"), color.getR(), color.getG(), color.getB(), 1.0F);
            } else {
                itemx.setLabel(Translator.getText("Tooltip_item_CanResearchPlural"), color.getR(), color.getG(), color.getB(), 1.0F);
            }
        }

        if (player != null && this.scriptItem != null && this.scriptItem.isFavouriteRecipeInput(player)) {
            ColorInfo color = Core.getInstance().getGoodHighlitedColor();
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_item_IsFavouriteInput"), color.getR(), color.getG(), color.getB(), 1.0F);
        }

        if (player != null && this.isNoRecipes(player)) {
            ColorInfo color = Core.getInstance().getBadHighlitedColor();
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_NoRecipes_More"), color.getR(), color.getG(), color.getB(), 1.0F);
        }

        if (player != null && this.isUnwanted(player)) {
            ColorInfo color = Core.getInstance().getBadHighlitedColor();
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_Unwanted_More"), color.getR(), color.getG(), color.getB(), 1.0F);
        }

        ObjectTooltip.LayoutItem itemx = layout.addItem();
        itemx.setLabel(Translator.getText("Tooltip_item_Weight") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
        boolean isEquipped = this.isEquipped();
        if (!(this instanceof HandWeapon)
            && !(this instanceof Clothing)
            && !(this instanceof DrainableComboItem)
            && !this.getFullType().contains("Walkie")
            && !this.isKeyRing()) {
            if (this instanceof AnimalInventoryItem) {
                itemx.setValueRightNoPlus(this.getWeight());
            } else {
                float weight = this.getUnequippedWeight();
                if (weight > 0.0F && weight < 0.01F) {
                    weight = 0.01F;
                }

                if (this.getAttachedSlot() > -1) {
                    itemx.setValue(
                        this.getCleanString(this.getHotbarEquippedWeight())
                            + "    ("
                            + this.getCleanString(this.getUnequippedWeight())
                            + " "
                            + Translator.getText("Tooltip_item_Unattached")
                            + ")",
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                    );
                } else {
                    itemx.setValueRightNoPlus(weight);
                }
            }
        } else if (!isEquipped && !this.isFakeEquipped()) {
            if (this.getAttachedSlot() > -1) {
                itemx.setValue(
                    this.getCleanString(this.getHotbarEquippedWeight())
                        + "    ("
                        + this.getCleanString(this.getUnequippedWeight())
                        + " "
                        + Translator.getText("Tooltip_item_Unattached")
                        + ")",
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );
            } else {
                itemx.setValue(
                    this.getCleanString(this.getUnequippedWeight())
                        + "    ("
                        + this.getCleanString(this.getEquippedWeight())
                        + " "
                        + Translator.getText("Tooltip_item_Equipped")
                        + ")",
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );
            }
        } else {
            itemx.setValue(
                this.getCleanString(this.getEquippedWeight())
                    + "    ("
                    + this.getCleanString(this.getUnequippedWeight())
                    + " "
                    + Translator.getText("Tooltip_item_Unequipped")
                    + ")",
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
        }

        if (tooltipUI.getWeightOfStack() > 0.0F) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_item_StackWeight") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float weightx = tooltipUI.getWeightOfStack();
            if (weightx > 0.0F && weightx < 0.01F) {
                weightx = 0.01F;
            }

            itemx.setValueRightNoPlus(weightx);
        }

        if (this.getMaxAmmo() > 0 && !(this instanceof HandWeapon)) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_weapon_AmmoCount") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValue(this.getCurrentAmmoCount() + " / " + this.getMaxAmmo(), 1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (!(this instanceof HandWeapon) && this.getAmmoType() != null) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("ContextMenu_AmmoType") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            InventoryItem inventoryItem = InventoryItemFactory.CreateItem(this.getAmmoType());
            String displayName = inventoryItem.getDisplayName();
            itemx.setValue(Translator.getText(displayName), 1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (this.gunType != null) {
            Item GunType = ScriptManager.instance.FindItem(this.getGunType());
            if (GunType == null) {
                GunType = ScriptManager.instance.FindItem(this.getModule() + "." + this.ammoType);
            }

            if (GunType != null) {
                itemx = layout.addItem();
                itemx.setLabel(Translator.getText("ContextMenu_GunType") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValue(GunType.getDisplayName(), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue()) {
            itemx = layout.addItem();
            itemx.setLabel("getActualWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.getActualWeight());
            itemx = layout.addItem();
            itemx.setLabel("getWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.getWeight());
            itemx = layout.addItem();
            itemx.setLabel("getEquippedWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.getEquippedWeight());
            itemx = layout.addItem();
            itemx.setLabel("getUnequippedWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.getUnequippedWeight());
            itemx = layout.addItem();
            itemx.setLabel("getContentsWeight()", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.getContentsWeight());
            if (this instanceof Key || "Doorknob".equals(this.type)) {
                itemx = layout.addItem();
                itemx.setLabel("DBG: keyId", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus(this.getKeyId());
            }

            itemx = layout.addItem();
            itemx.setLabel("ID", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.id);
            itemx = layout.addItem();
            itemx.setLabel("DictionaryID", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(this.getRegistry_id());
            ClothingItem clothingItem = this.getClothingItem();
            if (clothingItem != null) {
                itemx = layout.addItem();
                itemx.setLabel("ClothingItem", 1.0F, 1.0F, 1.0F, 1.0F);
                itemx.setValue(this.getClothingItem().mame, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue() || LuaManager.GlobalObject.isAdmin()) {
            itemx = layout.addItem();
            String label = "Loot Category";
            String category = Translator.getText("Sandbox_" + this.getLootType() + "LootNew");
            itemx.setLabel("Loot Category:", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValue(category, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (this instanceof DrainableComboItem && !this.hasTag(ItemTag.HIDE_REMAINING)) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("IGUI_invpanel_Remaining") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            float f = this.getCurrentUsesFloat();
            ColorInfo G2Bgrad = new ColorInfo();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
            itemx.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
        }

        if (this instanceof Food && ((Food)this).isTainted() && SandboxOptions.instance.enableTaintedWaterText.getValue()) {
            itemx = layout.addItem();
            if (!this.hasMetal()) {
                itemx.setLabel(Translator.getText("Tooltip_item_TaintedWater"), 1.0F, 0.5F, 0.5F, 1.0F);
            } else {
                itemx.setLabel(Translator.getText("Tooltip_item_TaintedWater_Plastic"), 1.0F, 0.5F, 0.5F, 1.0F);
            }
        }

        if (!this.scriptItem.getForageFocusCategories().isEmpty()) {
            for (String categoryName : this.scriptItem.getForageFocusCategories()) {
                itemx = layout.addItem();
                itemx.setLabel(Translator.getText("UI_search_mode_focus") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValue(Translator.getText("IGUI_SearchMode_Categories_" + categoryName), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        if (this.getFatigueChange() != 0.0F) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_item_Fatigue") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.getFatigueChange() < 0.0F) {
                itemx.setProgress(this.getFatigueChange() * -1.0F, goodR, goodG, goodB, 1.0F);
            } else {
                itemx.setProgress(this.getFatigueChange(), badR, badG, badB, 1.0F);
            }
        }

        this.DoTooltip(tooltipUI, layout);
        if (this.getRemoteControlID() != -1) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_TrapControllerID"), 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValue(Integer.toString(this.getRemoteControlID()), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (this.getHaveBeenRepaired() > 0) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_weapon_Repaired") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.hasTimesHeadRepaired()) {
                itemx.setLabel(Translator.getText("Tooltip_handle_Repaired") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            }

            itemx.setValue(this.getHaveBeenRepaired() + "x", 1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (this.hasTimesHeadRepaired() && this.getTimesHeadRepaired() > 0) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_head_Repaired") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValue(this.getTimesHeadRepaired() + "x", 1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (this.isEquippedNoSprint()) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_CantSprintEquipped"), 1.0F, 0.1F, 0.1F, 1.0F);
        }

        if (this.isWet()) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_Wetness") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            float f = this.getWetCooldown() / 10000.0F;
            ColorInfo B2Ggrad = new ColorInfo();
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), f, B2Ggrad);
            itemx.setProgress(f, B2Ggrad.getR(), B2Ggrad.getG(), B2Ggrad.getB(), 1.0F);
        }

        if (this.getMaxCapacity() > 0) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_container_Capacity") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float capacity = this.getMaxCapacity();
            if (this.isConditionAffectsCapacity()) {
                capacity = VehiclePart.getNumberByCondition(this.getMaxCapacity(), this.getCondition(), 5.0F);
            }

            if (this.getItemCapacity() > -1.0F) {
                itemx.setValue(this.getItemCapacity() + " / " + capacity, 1.0F, 1.0F, 0.8F, 1.0F);
            } else {
                itemx.setValue("0 / " + capacity, 1.0F, 1.0F, 0.8F, 1.0F);
            }
        }

        if (!(this instanceof HandWeapon) && this.hasSharpness()) {
            float tr = 1.0F;
            float tg = 1.0F;
            float tb = 0.8F;
            float ta = 1.0F;
            ColorInfo G2Bgrad = new ColorInfo();
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_weapon_Sharpness") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float f = this.getSharpness();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
            itemx.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
        }

        if (!(this instanceof HandWeapon)
            && !(this instanceof Clothing)
            && this.getConditionMax() > 0
            && (this.getMechanicType() > 0 || this.hasTag(ItemTag.SHOW_CONDITION) || this.getConditionMax() > this.getCondition())) {
            ColorInfo G2Bgrad = new ColorInfo();
            float tr = 1.0F;
            float tg = 1.0F;
            float tb = 0.8F;
            float ta = 1.0F;
            itemx = layout.addItem();
            String text = "Tooltip_weapon_Condition";
            if (this.hasHeadCondition()) {
                text = "Tooltip_weapon_HandleCondition";
            }

            itemx.setLabel(Translator.getText(text) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float f = (float)this.getCondition() / this.getConditionMax();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
            itemx.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
        }

        if (player != null && player.hasReadMap(this)) {
            itemx = layout.addItem();
            String label = Translator.getText("Tooltip_literature_HasBeenRead");
            itemx.setLabel(label, 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (this.isRecordedMedia()) {
            MediaData data = this.getMediaData();
            if (data != null) {
                if (data.getTranslatedTitle() != null) {
                    itemx = layout.addItem();
                    itemx.setLabel(Translator.getText("Tooltip_media_title") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    itemx.setValue(data.getTranslatedTitle(), 1.0F, 1.0F, 1.0F, 1.0F);
                    if (data.getTranslatedSubTitle() != null) {
                        itemx = layout.addItem();
                        itemx.setLabel("", 1.0F, 1.0F, 0.8F, 1.0F);
                        itemx.setValue(data.getTranslatedSubTitle(), 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }

                if (data.getTranslatedAuthor() != null) {
                    itemx = layout.addItem();
                    itemx.setLabel(Translator.getText("Tooltip_media_author") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    itemx.setValue(data.getTranslatedAuthor(), 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }

            if (tooltipUI.getCharacter() instanceof IsoPlayer && this.hasBeenSeen((IsoPlayer)tooltipUI.getCharacter())) {
                itemx = layout.addItem();
                String label = Translator.getText("ContextMenu_Watched");
                itemx.setLabel(label, 1.0F, 1.0F, 0.8F, 1.0F);
            }

            if (tooltipUI.getCharacter() instanceof IsoPlayer && this.hasBeenHeard((IsoPlayer)tooltipUI.getCharacter())) {
                itemx = layout.addItem();
                String label = Translator.getText("ContextMenu_Heard");
                itemx.setLabel(label, 1.0F, 1.0F, 0.8F, 1.0F);
            }
        }

        if (this.hasTag(ItemTag.COMPASS) && this.isInPlayerInventory()) {
            IsoDirections dir = this.getOutermostContainer().getParent().getDir();
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_compass_" + dir.toCompassString()), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (this.isFishingLure()) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_IsFishingLure"), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (this.getAttributes() != null) {
            this.getAttributes().DoTooltip(tooltipUI, layout);
        }

        if (this.getFluidContainer() != null) {
            this.getFluidContainer().DoTooltip(tooltipUI, layout);
        }

        if (this.getWorldItem() != null && this.getWorldItem().getFluidContainer() != null) {
            this.getWorldItem().getFluidContainer().DoTooltip(tooltipUI, layout);
        }

        if (this.getDurabilityComponent() != null) {
            this.getDurabilityComponent().DoTooltip(tooltipUI, layout);
        }

        if (this.getVisionModifier() != 1.0F) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_item_VisionImpariment") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.getVisionModifier() < 1.0F) {
                itemx.setProgress(1.0F - this.getVisionModifier(), badR, badG, badB, 1.0F);
            } else {
                itemx.setProgress(this.getVisionModifier() - 1.0F, goodR, goodG, goodB, 1.0F);
            }
        }

        if (this.getHearingModifier() != 1.0F) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_item_HearingImpariment") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.getHearingModifier() < 1.0F) {
                itemx.setProgress(1.0F - this.getHearingModifier(), badR, badG, badB, 1.0F);
            } else {
                itemx.setProgress(this.getHearingModifier() - 1.0F, goodR, goodG, goodB, 1.0F);
            }
        }

        if (this.getDiscomfortModifier() != 0.0F) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_item_Discomfort") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.getDiscomfortModifier() > 0.0F) {
                itemx.setProgress(this.getDiscomfortModifier(), badR, badG, badB, 1.0F);
            } else {
                itemx.setProgress(this.getDiscomfortModifier() * -1.0F, goodR, goodG, goodB, 1.0F);
            }
        }

        if (Core.getInstance().getOptionShowItemModInfo() && !this.isVanilla()) {
            itemx = layout.addItem();
            Color c = Colors.CornFlowerBlue;
            itemx.setLabel("Mod: " + this.getModName(), c.r, c.g, c.b, 1.0F);
            ItemInfo info = WorldDictionary.getItemInfoFromID(this.getRegistry_id());
            if (info != null && info.getModOverrides() != null) {
                itemx = layout.addItem();
                float cl = 0.5F;
                if (info.getModOverrides().size() == 1) {
                    itemx.setLabel("This item overrides: " + WorldDictionary.getModNameFromID(info.getModOverrides().get(0)), 0.5F, 0.5F, 0.5F, 1.0F);
                } else {
                    itemx.setLabel("This item overrides:", 0.5F, 0.5F, 0.5F, 1.0F);

                    for (int i = 0; i < info.getModOverrides().size(); i++) {
                        itemx = layout.addItem();
                        itemx.setLabel(" - " + WorldDictionary.getModNameFromID(info.getModOverrides().get(i)), 0.5F, 0.5F, 0.5F, 1.0F);
                    }
                }
            }
        }

        if (this.getTooltip() != null) {
            itemx = layout.addItem();
            itemx.setLabel(Translator.getText(this.getTooltip()), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (layoutOverride == null) {
            y = layout.render(tooltipUI.padLeft, y, tooltipUI);
            tooltipUI.endLayout(layout);
            y += tooltipUI.padBottom;
            tooltipUI.setHeight(y);
            if (tooltipUI.getWidth() < 150.0) {
                tooltipUI.setWidth(150.0);
            }
        }
    }

    private float drawTooltipItemTexture(ObjectTooltip tooltipUI, Texture tex, float x, float y, float width, float height, float r, float g, float b, float a) {
        tooltipUI.DrawTextureScaledAspect(tex, x, y, width, height, r, g, b, a);
        if (tex != null && tex.getWidth() > 0 && tex.getHeight() > 0) {
            float ratio = Math.min(width / tex.getWidthOrig(), height / tex.getHeightOrig());
            return PZMath.ceil(tex.getWidth() * ratio);
        } else {
            return width;
        }
    }

    public String getCleanString(float weight) {
        float value = (int)((weight + 0.005) * 100.0) / 100.0F;
        return Float.toString(value);
    }

    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
    }

    public void SetContainerPosition(int x, int y) {
        this.containerX = x;
        this.containerY = y;
    }

    public void Use() {
        this.Use(false);
    }

    public void UseAndSync() {
        this.Use(false, false, GameServer.server);
    }

    public void UseItem() {
        this.Use(false);
    }

    public void Use(boolean bCrafting) {
        this.Use(bCrafting, false, false);
    }

    public void Use(boolean bCrafting, boolean bInContainer, boolean bNeedSync) {
        if (this.isDisappearOnUse() || bCrafting) {
            this.setCurrentUses(this.getCurrentUses() - 1);
            if (this.replaceOnUse != null && !bInContainer && !bCrafting && this.container != null) {
                String s = this.replaceOnUse;
                if (!this.replaceOnUse.contains(".")) {
                    s = this.module + "." + s;
                }

                InventoryItem item = this.container.AddItem(s);
                if (item != null) {
                    item.setColorRed(this.colorRed);
                    item.setColorGreen(this.colorGreen);
                    item.setColorBlue(this.colorBlue);
                    item.setColor(new Color(this.colorRed, this.colorGreen, this.colorBlue));
                    item.setCustomColor(true);
                    item.setModelIndex(this.modelIndex);
                    this.container.setDrawDirty(true);
                    this.container.setDirty(true);
                    item.copyConditionStatesFrom(this);
                    item.setFavorite(this.isFavorite());
                    if (GameServer.server && bNeedSync) {
                        GameServer.sendAddItemToContainer(this.container, item);
                    }
                }
            }

            if (this.getCurrentUses() <= 0) {
                if (this.isKeepOnDeplete()) {
                    return;
                }

                if (this.container != null) {
                    if (this.container.parent instanceof IsoGameCharacter chr && !(this instanceof HandWeapon)) {
                        chr.removeFromHands(this);
                    }

                    this.container.items.remove(this);
                    this.container.setDirty(true);
                    this.container.setDrawDirty(true);
                    if (GameServer.server && bNeedSync) {
                        GameServer.sendRemoveItemFromContainer(this.container, this);
                    }

                    this.container = null;
                }
            } else if (bNeedSync) {
                this.syncItemFields();
            }
        }
    }

    public boolean shouldUpdateInWorld() {
        if (!GameServer.server && this.itemHeat != 1.0F) {
            return true;
        } else if (!GameClient.client && (this.hasComponent(ComponentType.FluidContainer) || this instanceof Food)) {
            IsoGridSquare sq = this.getWorldItem().getSquare();
            return sq != null && sq.isOutside();
        } else {
            return false;
        }
    }

    public void update() {
        if (this.isWet()) {
            this.wetCooldown = this.wetCooldown - 1.0F * GameTime.instance.getMultiplier();
            if (this.wetCooldown <= 0.0F) {
                InventoryItem dryItem = InventoryItemFactory.CreateItem(this.itemWhenDry);
                if (this.isFavorite()) {
                    dryItem.setFavorite(true);
                }

                IsoWorldInventoryObject worldItem = this.getWorldItem();
                if (worldItem != null) {
                    IsoGridSquare square = worldItem.getSquare();
                    square.AddWorldInventoryItem(dryItem, worldItem.getX() % 1.0F, worldItem.getY() % 1.0F, worldItem.getZ() % 1.0F);
                    square.transmitRemoveItemFromSquare(worldItem);
                    if (this.getContainer() != null) {
                        this.getContainer().setDirty(true);
                        this.getContainer().setDrawDirty(true);
                    }

                    this.setWorldItem(null);
                } else if (this.getContainer() != null) {
                    this.getContainer().addItem(dryItem);
                    this.getContainer().Remove(this);
                }

                this.setWet(false);
                IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
                LuaEventManager.triggerEvent("OnContainerUpdate");
            }
        }

        if (this.hasComponent(ComponentType.FluidContainer)) {
            ItemContainer outermostContainer = this.getOutermostContainer();
            FluidContainer cont = this.getFluidContainer();
            if (outermostContainer != null) {
                float temp = outermostContainer.getTemprature();
                float tempChange = GameServer.server ? 0.06F : 0.001F;
                if (temp == 1.0F && this.itemHeat < 1.0F) {
                    this.itemHeat = this.itemHeat + tempChange * GameTime.instance.getMultiplier();
                    if (this.itemHeat > temp) {
                        this.itemHeat = temp;
                    }
                }

                if (this.itemHeat > temp) {
                    this.itemHeat = this.itemHeat - tempChange * GameTime.instance.getMultiplier();
                    if (this.itemHeat < Math.max(0.2F, temp)) {
                        this.itemHeat = Math.max(0.2F, temp);
                    }
                }

                if (this.itemHeat < temp
                    && (this.hasTag(ItemTag.COOKABLE) || this.hasTag(ItemTag.COOKABLE_MICROWAVE) && outermostContainer.getType().equals("microwave"))) {
                    this.itemHeat = this.itemHeat + temp / (GameServer.server ? 16 : 1000) * GameTime.instance.getMultiplier();
                    if (this.itemHeat > Math.min(3.0F, temp)) {
                        this.itemHeat = Math.min(3.0F, temp);
                    }
                }

                if (this.itemHeat > 1.6F && !cont.isEmpty()) {
                    if (cont.contains(Fluid.TaintedWater)) {
                        float oldAmount = cont.getSpecificFluidAmount(Fluid.TaintedWater);
                        float remove = PZMath.min(oldAmount, (GameServer.server ? 0.6F : 0.01F) * GameTime.instance.getMultiplier());
                        cont.adjustSpecificFluidAmount(Fluid.TaintedWater, oldAmount - remove);
                        cont.addFluid(Fluid.Water, remove);
                    }

                    if (cont.contains(Fluid.Petrol)) {
                        cont.removeFluid();
                        boolean isCampfire = this.container != null
                            && this.container.getParent() != null
                            && this.container.getParent().getName() != null
                            && this.container.getParent().getName().equals("Campfire");
                        if (!isCampfire && this.container != null && this.container.getParent() != null && this.container.getParent() instanceof IsoFireplace) {
                            isCampfire = true;
                        }

                        if (this.container != null && this.container.sourceGrid != null && !isCampfire) {
                            IsoFireManager.StartFire(this.container.sourceGrid.getCell(), this.container.sourceGrid, true, 500000);
                        }
                    }

                    if (GameServer.server) {
                        GameServer.sendItemStats(this);
                    }
                }
            }
        }

        if ((this.container == null || this.getWorldItem() != null) && this.itemHeat != 1.0F) {
            float temp2 = 1.0F;
            if (this.itemHeat > 1.0F) {
                this.itemHeat = this.itemHeat - 0.001F * GameTime.instance.getMultiplier();
                if (this.itemHeat < 1.0F) {
                    this.itemHeat = 1.0F;
                }
            }

            if (this.itemHeat < 1.0F) {
                this.itemHeat = this.itemHeat + 0.001F * GameTime.instance.getMultiplier();
                if (this.itemHeat > 1.0F) {
                    this.itemHeat = 1.0F;
                }
            }
        }

        if (!GameServer.server && this.getWorldItem() != null && RainManager.isRaining()) {
            IsoGridSquare sq = this.getWorldItem().getSquare();
            if (this instanceof Food && sq != null && sq.isOutside() && LuaManager.GlobalObject.ZombRandFloat(0.0F, 1.0F) < RainManager.getRainIntensity()) {
                ((Food)this).setTainted(true);
            }
        }
    }

    public boolean finishupdate() {
        if (!GameClient.client
            && this.getWorldItem() != null
            && this.getWorldItem().getObjectIndex() != -1
            && this instanceof Food
            && !((Food)this).isTainted()) {
            return false;
        } else if (this.getWorldItem() != null && this.itemHeat != 1.0F) {
            return false;
        } else {
            if (this.hasComponent(ComponentType.FluidContainer)) {
                FluidContainer cont = this.getFluidContainer();
                if (this.getWorldItem() != null && this.getWorldItem().getObjectIndex() != -1 && cont.canPlayerEmpty()) {
                    return false;
                }

                if (this.container != null
                    && (this.itemHeat != 1.0F || this.itemHeat != this.container.getTemprature() || this.container.isTemperatureChanging())) {
                    return false;
                }
            }

            return !this.isWet();
        }
    }

    public void updateSound(BaseSoundEmitter emitter) {
        this.updateEquippedAndActivatedSound(emitter);
    }

    public void updateEquippedAndActivatedSound(BaseSoundEmitter emitter) {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.EQUIPPED_AND_ACTIVATED);
        if (soundName != null) {
            IsoPlayer player = this.getOwnerPlayer(this.getContainer());
            if (player == null) {
                this.stopEquippedAndActivatedSound();
                ItemSoundManager.removeItem(this);
            } else if (this.isEquipped() && this.isActivated()) {
                BaseCharacterSoundEmitter currentEmitter = player.getEmitter();
                if (!currentEmitter.isPlaying(this.equippedAndActivatedSound)) {
                    this.stopEquippedAndActivatedSound();
                    this.equippedAndActivatedPlayer = player;
                    this.equippedAndActivatedSound = currentEmitter.playSoundImpl(soundName, player);
                }
            } else {
                this.stopEquippedAndActivatedSound();
                ItemSoundManager.removeItem(this);
            }
        }
    }

    public void updateEquippedAndActivatedSound() {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.EQUIPPED_AND_ACTIVATED);
        if (soundName != null) {
            if (this.isActivated() && this instanceof DrainableComboItem && this.getCurrentUses() <= 0) {
                this.setActivated(false);
            }

            if (this.isEquipped() && this.isActivated()) {
                ItemSoundManager.addItem(this);
            } else {
                this.stopEquippedAndActivatedSound();
                ItemSoundManager.removeItem(this);
            }
        }
    }

    protected void stopEquippedAndActivatedSound() {
        if (this.equippedAndActivatedPlayer != null && this.equippedAndActivatedSound != 0L) {
            this.equippedAndActivatedPlayer.getEmitter().stopOrTriggerSound(this.equippedAndActivatedSound);
            this.equippedAndActivatedPlayer = null;
            this.equippedAndActivatedSound = 0L;
        }
    }

    public void playActivateSound() {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.ACTIVATE);
        if (soundName != null) {
            this.playSoundOnPlayer(soundName);
        }
    }

    public void playDeactivateSound() {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.DEACTIVATE);
        if (soundName != null) {
            this.playSoundOnPlayer(soundName);
        }
    }

    public void playActivateDeactivateSound() {
        if (this.isActivated()) {
            this.playActivateSound();
        } else {
            this.playDeactivateSound();
        }
    }

    protected void playSoundOnPlayer(SoundKey soundKey) {
        if (soundKey != null) {
            this.playSoundOnPlayer(soundKey.id());
        }
    }

    protected void playSoundOnPlayer(String soundName) {
        IsoPlayer player = this.getOwnerPlayer(this.getContainer());
        if (player != null && player.isLocalPlayer()) {
            player.getEmitter().playSound(soundName);
        }
    }

    protected IsoPlayer getOwnerPlayer(ItemContainer container) {
        if (container == null) {
            return null;
        } else {
            return container.getParent() instanceof IsoPlayer isoPlayer ? isoPlayer : null;
        }
    }

    public boolean is(ItemKey... item) {
        String fullType = this.getFullType();

        for (int i = 0; i < item.length; i++) {
            if (item[i].toString().equals(fullType)) {
                return true;
            }
        }

        return false;
    }

    public String getFullType() {
        assert this.fullType != null && this.fullType.equals(this.module + "." + this.type);

        return this.fullType;
    }

    public void save(ByteBuffer output, boolean net) throws IOException {
        net = false;
        output.putShort(this.getRegistry_id());
        output.put((byte)-1);
        output.putInt(this.id);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (this.getCurrentUses() != 1) {
            header.addFlags(1);
            output.putInt(this.getCurrentUses());
        }

        if (this.condition != this.conditionMax) {
            header.addFlags(4);
            output.put((byte)this.getCondition());
        }

        if (this.visual != null) {
            header.addFlags(8);
            this.visual.save(output);
        }

        if (this.isCustomColor() && (this.col.r != 1.0F || this.col.g != 1.0F || this.col.b != 1.0F || this.col.a != 1.0F)) {
            header.addFlags(16);
            output.put(Bits.packFloatUnitToByte(this.getColor().r));
            output.put(Bits.packFloatUnitToByte(this.getColor().g));
            output.put(Bits.packFloatUnitToByte(this.getColor().b));
            output.put(Bits.packFloatUnitToByte(this.getColor().a));
        }

        if (this.itemCapacity != -1.0F) {
            header.addFlags(32);
            output.putFloat(this.itemCapacity);
        }

        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, output);
        if (this.table != null && !this.table.isEmpty()) {
            bits.addFlags(1);
            this.table.save(output);
        }

        if (this.isActivated()) {
            bits.addFlags(2);
        }

        if (this.haveBeenRepaired != 0) {
            bits.addFlags(4);
            output.putShort((short)this.getHaveBeenRepaired());
        }

        if (this.name != null && !this.name.equals(this.originalName)) {
            bits.addFlags(8);
            GameWindow.WriteString(output, this.name);
        }

        if (this.byteData != null) {
            bits.addFlags(16);
            this.byteData.rewind();
            output.putInt(this.byteData.limit());
            output.put(this.byteData);
            this.byteData.flip();
        }

        if (this.extraItems != null && !this.extraItems.isEmpty()) {
            bits.addFlags(32);
            output.putInt(this.extraItems.size());

            for (int i = 0; i < this.extraItems.size(); i++) {
                output.putShort(WorldDictionary.getItemRegistryID(this.extraItems.get(i)));
            }
        }

        if (this.isCustomName()) {
            bits.addFlags(64);
        }

        if (this.isCustomWeight()) {
            bits.addFlags(128);
            output.putFloat(this.isCustomWeight() ? this.getActualWeight() : -1.0F);
        }

        if (this.keyId != -1) {
            bits.addFlags(256);
            output.putInt(this.getKeyId());
        }

        if (this.remoteControlId != -1 || this.remoteRange != 0) {
            bits.addFlags(1024);
            output.putInt(this.getRemoteControlID());
            output.putInt(this.getRemoteRange());
        }

        if (this.colorRed != 1.0F || this.colorGreen != 1.0F || this.colorBlue != 1.0F) {
            bits.addFlags(2048);
            output.put(Bits.packFloatUnitToByte(this.colorRed));
            output.put(Bits.packFloatUnitToByte(this.colorGreen));
            output.put(Bits.packFloatUnitToByte(this.colorBlue));
        }

        if (this.worker != null) {
            bits.addFlags(4096);
            GameWindow.WriteString(output, this.getWorker());
        }

        if (this.wetCooldown != -1.0F) {
            bits.addFlags(8192);
            output.putFloat(this.wetCooldown);
        }

        if (this.isFavorite()) {
            bits.addFlags(16384);
        }

        if (this.stashMap != null) {
            bits.addFlags(32768);
            GameWindow.WriteString(output, this.stashMap);
        }

        if (this.isInfected()) {
            bits.addFlags(65536);
        }

        if (this.currentAmmoCount != 0) {
            bits.addFlags(131072);
            output.putInt(this.currentAmmoCount);
        }

        if (this.attachedSlot != -1) {
            bits.addFlags(262144);
            output.putInt(this.attachedSlot);
        }

        if (this.attachedSlotType != null) {
            bits.addFlags(524288);
            GameWindow.WriteString(output, this.attachedSlotType);
        }

        if (this.attachedToModel != null) {
            bits.addFlags(1048576);
            GameWindow.WriteString(output, this.attachedToModel);
        }

        if (this.maxCapacity != -1) {
            bits.addFlags(2097152);
            output.putInt(this.maxCapacity);
        }

        if (this.isRecordedMedia()) {
            bits.addFlags(4194304);
            output.putShort(this.recordedMediaIndex);
        }

        if (this.worldScale != 1.0F) {
            bits.addFlags(16777216);
            output.putFloat(this.worldScale);
        }

        if (this.isInitialised) {
            bits.addFlags(33554432);
        }

        if (this.requiresEntitySave()) {
            bits.addFlags(67108864);
            this.saveEntity(output);
        }

        if (this.animalTracks != null) {
            bits.addFlags(134217728);
            this.animalTracks.save(output);
        }

        if (this.texture != null
            && this.texture.getName() != null
            && this.texture != Texture.getSharedTexture("media/inventory/Question_On.png")
            && this.scriptItem.getIcon() != null
            && !Objects.equals(this.scriptItem.getIcon(), "None")
            && !Objects.equals(this.scriptItem.getIcon(), "default")) {
            String icon1 = this.texture.getName();
            int p = icon1.lastIndexOf(File.separator);
            if (p != -1) {
                icon1 = icon1.substring(p + 1).replace(".png", "");
            }

            String icon2 = "Item_" + this.scriptItem.getIcon();
            if (!Objects.equals(icon1, icon2)) {
                bits.addFlags(268435456);
                GameWindow.WriteString(output, this.texture.getName());
            }
        }

        if (this.modelIndex > -1) {
            bits.addFlags(536870912);
            output.putInt(this.modelIndex);
        }

        if (this.worldXRotation != 0.0F || this.worldYRotation != 0.0F || this.worldZRotation != -1.0F) {
            bits.addFlags(1073741824);
            output.putFloat(this.worldXRotation);
            output.putFloat(this.worldYRotation);
            output.putFloat(this.worldZRotation);
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

    public static InventoryItem loadItem(ByteBuffer input, int WorldVersion) throws IOException {
        return loadItem(input, WorldVersion, true);
    }

    /**
     * Attempts loading the item including creation, uppon failure bytes might be skipped or the buffer position may be set to end item position.
     *  Item needs to be saved with size.
     * @return InventoryItem, or null if the item failed loading or if Creating the item failed due to being obsolete etc.
     */
    public static InventoryItem loadItem(ByteBuffer input, int WorldVersion, boolean doSaveTypeCheck) throws IOException {
        return loadItem(input, WorldVersion, doSaveTypeCheck, null);
    }

    public static InventoryItem loadItem(ByteBuffer input, int WorldVersion, boolean doSaveTypeCheck, InventoryItem i) throws IOException {
        int dataLen = input.getInt();
        if (dataLen <= 0) {
            throw new IOException("InventoryItem.loadItem() invalid item data length: " + dataLen);
        } else {
            int position = input.position();
            short registryID = input.getShort();
            byte saveType = -1;
            saveType = input.get();
            InventoryItem item = i;
            if (i == null) {
                item = InventoryItemFactory.CreateItem(registryID);
            }

            if (item != null) {
                try {
                    item.load(input, WorldVersion);
                } catch (Exception var10) {
                    ExceptionLogger.logException(var10);
                    item = null;
                }
            }

            if (item != null) {
                if (dataLen != -1 && input.position() != position + dataLen) {
                    input.position(position + dataLen);
                    DebugLog.log(
                        "InventoryItem.loadItem() data length not matching, resetting buffer position to '"
                            + (position + dataLen)
                            + "'. itemtype: "
                            + WorldDictionary.getItemTypeDebugString(registryID)
                    );
                    if (Core.debug) {
                        throw new IOException(
                            "InventoryItem.loadItem() read more data than save() wrote (" + WorldDictionary.getItemTypeDebugString(registryID) + ")"
                        );
                    }
                }

                return item;
            } else {
                if (input.position() >= position + dataLen) {
                    if (input.position() >= position + dataLen) {
                        input.position(position + dataLen);
                        DebugLog.log(
                            "InventoryItem.loadItem() item == null, resetting buffer position to '"
                                + (position + dataLen)
                                + "'. itemtype: "
                                + WorldDictionary.getItemTypeDebugString(registryID)
                        );
                    }
                } else {
                    while (input.position() < position + dataLen) {
                        input.get();
                    }

                    DebugLog.log("InventoryItem.loadItem() item == null, skipped bytes. itemtype: " + WorldDictionary.getItemTypeDebugString(registryID));
                }

                return null;
            }
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.id = input.getInt();
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        this.setCurrentUses(1);
        this.name = this.originalName;
        this.condition = this.conditionMax;
        this.customColor = false;
        this.col = Color.white;
        this.itemCapacity = -1.0F;
        this.table = null;
        this.activated = false;
        this.haveBeenRepaired = 0;
        this.customName = false;
        this.customWeight = false;
        this.keyId = -1;
        this.remoteControlId = -1;
        this.remoteRange = 0;
        this.colorRed = this.colorGreen = this.colorBlue = 1.0F;
        this.worker = null;
        this.wetCooldown = -1.0F;
        this.favorite = false;
        this.stashMap = null;
        this.zombieInfected = false;
        this.currentAmmoCount = 0;
        this.attachedSlot = -1;
        this.attachedSlotType = null;
        this.attachedToModel = null;
        this.maxCapacity = -1;
        this.recordedMediaIndex = -1;
        this.worldZRotation = -1.0F;
        this.worldScale = 1.0F;
        this.isInitialised = false;
        if (!header.equals(0)) {
            if (WorldVersion >= 220) {
                if (header.hasFlags(1)) {
                    this.setCurrentUses(input.getInt());
                }

                if (header.hasFlags(2)) {
                }
            } else {
                if (header.hasFlags(1)) {
                    short usesvalue = input.getShort();
                    this.setCurrentUses(usesvalue);
                }

                if (header.hasFlags(2)) {
                    byte var9 = input.get();
                }
            }

            if (header.hasFlags(4)) {
                this.setConditionWhileLoading(input.get());
            }

            if (header.hasFlags(8)) {
                this.visual = new ItemVisual();
                this.visual.load(input, WorldVersion);
            }

            if (header.hasFlags(16)) {
                float r = Bits.unpackByteToFloatUnit(input.get());
                float g = Bits.unpackByteToFloatUnit(input.get());
                float b = Bits.unpackByteToFloatUnit(input.get());
                float a = Bits.unpackByteToFloatUnit(input.get());
                this.setColor(new Color(r, g, b, a));
                this.setCustomColor(true);
            }

            if (header.hasFlags(32)) {
                this.itemCapacity = input.getFloat();
            }

            if (header.hasFlags(64)) {
                BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Integer, input);
                if (bits.hasFlags(1)) {
                    if (this.table == null) {
                        this.table = LuaManager.platform.newTable();
                    }

                    this.table.load(input, WorldVersion);
                }

                this.activated = bits.hasFlags(2);
                if (bits.hasFlags(4)) {
                    this.setHaveBeenRepaired(input.getShort());
                }

                if (bits.hasFlags(8)) {
                    this.name = GameWindow.ReadString(input);
                }

                if (bits.hasFlags(16)) {
                    int size = input.getInt();
                    this.byteData = ByteBuffer.allocate(size);

                    for (int n = 0; n < size; n++) {
                        this.byteData.put(input.get());
                    }

                    this.byteData.flip();
                }

                if (bits.hasFlags(32)) {
                    int it = input.getInt();
                    if (it > 0) {
                        this.extraItems = new ArrayList<>();

                        for (int i = 0; i < it; i++) {
                            short registryItemId = input.getShort();
                            String itemfull = WorldDictionary.getItemTypeFromID(registryItemId);
                            this.extraItems.add(itemfull);
                        }
                    }
                }

                this.setCustomName(bits.hasFlags(64));
                if (bits.hasFlags(128)) {
                    float weight = input.getFloat();
                    if (weight >= 0.0F) {
                        this.setActualWeight(weight);
                        this.setWeight(weight);
                        this.setCustomWeight(true);
                    }
                }

                if (bits.hasFlags(256)) {
                    this.setKeyId(input.getInt());
                }

                if (bits.hasFlags(1024)) {
                    this.setRemoteControlID(input.getInt());
                    this.setRemoteRange(input.getInt());
                }

                if (bits.hasFlags(2048)) {
                    float r = Bits.unpackByteToFloatUnit(input.get());
                    float g = Bits.unpackByteToFloatUnit(input.get());
                    float b = Bits.unpackByteToFloatUnit(input.get());
                    this.setColorRed(r);
                    this.setColorGreen(g);
                    this.setColorBlue(b);
                    this.setColor(new Color(this.colorRed, this.colorGreen, this.colorBlue));
                }

                if (bits.hasFlags(4096)) {
                    this.setWorker(GameWindow.ReadString(input));
                }

                if (bits.hasFlags(8192)) {
                    this.setWetCooldown(input.getFloat());
                }

                this.setFavorite(bits.hasFlags(16384));
                if (bits.hasFlags(32768)) {
                    this.stashMap = GameWindow.ReadString(input);
                }

                this.setInfected(bits.hasFlags(65536));
                if (bits.hasFlags(131072)) {
                    this.setCurrentAmmoCount(input.getInt());
                }

                if (bits.hasFlags(262144)) {
                    this.attachedSlot = input.getInt();
                }

                if (bits.hasFlags(524288)) {
                    this.attachedSlotType = GameWindow.ReadString(input);
                }

                if (bits.hasFlags(1048576)) {
                    this.attachedToModel = GameWindow.ReadString(input);
                }

                if (bits.hasFlags(2097152)) {
                    this.maxCapacity = input.getInt();
                }

                if (bits.hasFlags(4194304)) {
                    this.setRecordedMediaIndex(input.getShort());
                }

                if (WorldVersion < 232 && bits.hasFlags(8388608)) {
                    this.setWorldZRotation(input.getInt());
                }

                if (bits.hasFlags(16777216)) {
                    this.worldScale = input.getFloat();
                }

                this.setInitialised(bits.hasFlags(33554432));
                if (bits.hasFlags(67108864)) {
                    List<Component> loadedComponents = new ArrayList<>();
                    this.loadEntity(input, WorldVersion, loadedComponents);

                    for (int i = this.componentSize() - 1; i >= 0; i--) {
                        Component component = this.getComponentForIndex(i);
                        if (!loadedComponents.contains(component)) {
                            GameEntityFactory.RemoveComponent(this, component);
                        }
                    }

                    loadedComponents.clear();
                }

                if (bits.hasFlags(134217728)) {
                    this.animalTracks = new AnimalTracks();
                    this.animalTracks.load(input, WorldVersion);
                }

                if (bits.hasFlags(268435456)) {
                    this.setTexture(Texture.getSharedTexture(GameWindow.ReadStringUTF(input)));
                }

                if (bits.hasFlags(536870912)) {
                    this.modelIndex = input.getInt();
                }

                if (bits.hasFlags(1073741824)) {
                    if (WorldVersion >= 232) {
                        this.setWorldXRotation(input.getFloat());
                        this.setWorldYRotation(input.getFloat());
                        this.setWorldZRotation(input.getFloat());
                    } else {
                        this.setWorldYRotation(input.getInt());
                        this.setWorldXRotation(input.getInt());
                    }
                }

                bits.release();
            }
        }

        this.synchWithVisual();
        header.release();
    }

    public InventoryItem createCloneItem() {
        if (Core.getInstance().getDebug()) {
            InventoryItem newItem = InventoryItemFactory.CreateItem(this.getFullType());

            try {
                tempBuffer.clear();
                int originalID = newItem.id;
                this.save(tempBuffer, false);
                tempBuffer.rewind();
                tempBuffer.getShort();
                tempBuffer.get();
                newItem.load(tempBuffer, 241);
                newItem.id = originalID;
            } catch (IOException var3) {
                var3.printStackTrace();
            }

            return newItem;
        } else {
            return null;
        }
    }

    public boolean IsFood() {
        return false;
    }

    public boolean IsWeapon() {
        return false;
    }

    public boolean IsDrainable() {
        return false;
    }

    public boolean IsLiterature() {
        return false;
    }

    public boolean IsClothing() {
        return false;
    }

    public boolean IsInventoryContainer() {
        return false;
    }

    public boolean IsMap() {
        return false;
    }

    static InventoryItem LoadFromFile(DataInputStream input) throws IOException {
        GameWindow.ReadString(input);
        return null;
    }

    public ItemContainer getOutermostContainer() {
        if (this.container != null && !"floor".equals(this.container.type)) {
            ItemContainer outer = this.container;

            while (
                outer.getContainingItem() != null
                    && outer.getContainingItem().getContainer() != null
                    && !"floor".equals(outer.getContainingItem().getContainer().type)
            ) {
                outer = outer.getContainingItem().getContainer();
            }

            return outer;
        } else {
            return null;
        }
    }

    public boolean isInLocalPlayerInventory() {
        if (!GameClient.client) {
            return false;
        } else {
            ItemContainer outer = this.getOutermostContainer();
            if (outer == null) {
                return false;
            } else {
                return outer.getParent() instanceof IsoPlayer ? ((IsoPlayer)outer.getParent()).isLocalPlayer() : false;
            }
        }
    }

    public boolean isInPlayerInventory() {
        ItemContainer outer = this.getOutermostContainer();
        return outer == null ? false : outer.getParent() instanceof IsoPlayer;
    }

    public ItemReplacement getItemReplacementPrimaryHand() {
        return this.scriptItem.replacePrimaryHand;
    }

    public ItemReplacement getItemReplacementSecondHand() {
        return this.scriptItem.replaceSecondHand;
    }

    public ClothingItem getClothingItem() {
        if ("RightHand".equalsIgnoreCase(this.getAlternateModelName())) {
            return this.getItemReplacementPrimaryHand().clothingItem;
        } else {
            return "LeftHand".equalsIgnoreCase(this.getAlternateModelName())
                ? this.getItemReplacementSecondHand().clothingItem
                : this.scriptItem.getClothingItemAsset();
        }
    }

    public String getAlternateModelName() {
        if (this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter chr) {
            if (chr.getPrimaryHandItem() == this && this.getItemReplacementPrimaryHand() != null) {
                return "RightHand";
            }

            if (chr.getSecondaryHandItem() == this && this.getItemReplacementSecondHand() != null) {
                return "LeftHand";
            }
        }

        return this.alternateModelName;
    }

    public ItemVisual getVisual() {
        ClothingItem clothingItem = this.getClothingItem();
        if (clothingItem != null && clothingItem.isReady()) {
            if (this.visual == null) {
                this.visual = new ItemVisual();
                this.visual.setItemType(this.getFullType());
                this.visual.pickUninitializedValues(clothingItem);
            }

            this.visual.setClothingItemName(clothingItem.mame);
            this.visual.setAlternateModelName(this.getAlternateModelName());
            return this.visual;
        } else {
            this.visual = null;
            return null;
        }
    }

    public boolean allowRandomTint() {
        ClothingItem clothingItem = this.getClothingItem();
        return clothingItem != null ? clothingItem.allowRandomTint : false;
    }

    public void synchWithVisual() {
        if (this instanceof HandWeapon && ((HandWeapon)this).getWeaponSpritesByIndex() != null && this.modelIndex == -1) {
            int maxIndex = ((HandWeapon)this).getWeaponSpritesByIndex().size();
            this.modelIndex = Rand.Next(maxIndex);
        } else if ((this.getStaticModelsByIndex() != null || this.getWorldStaticModelsByIndex() != null) && this.modelIndex == -1) {
            int maxIndex = -1;
            if (this.getStaticModelsByIndex() != null && this.getWorldStaticModelsByIndex() != null) {
                maxIndex = Math.max(this.getStaticModelsByIndex().size(), this.getWorldStaticModelsByIndex().size());
            } else if (this.getStaticModelsByIndex() != null && this.getWorldStaticModelsByIndex() == null) {
                maxIndex = this.getStaticModelsByIndex().size();
            } else {
                maxIndex = this.getWorldStaticModelsByIndex().size();
            }

            this.modelIndex = Rand.Next(maxIndex);
        }

        if (this.modelIndex != -1 && this.getIconsForTexture() != null && this.getIconsForTexture().get(this.modelIndex) != null) {
            String icon = this.getIconsForTexture().get(this.modelIndex);
            if (!StringUtils.isNullOrWhitespace(icon)) {
                this.texture = Texture.trygetTexture("Item_" + icon);
                if (this.texture == null) {
                    this.texture = Texture.getSharedTexture("media/inventory/Question_On.png");
                }
            }
        }

        if (this instanceof Clothing || this instanceof InventoryContainer) {
            ItemVisual visu = this.getVisual();
            if (visu != null) {
                if (this instanceof Clothing && this.getBloodClothingType() != null) {
                    BloodClothingType.calcTotalBloodLevel((Clothing)this);
                    BloodClothingType.calcTotalDirtLevel((Clothing)this);
                }

                ClothingItem clothingItem = this.getClothingItem();
                if (clothingItem.allowRandomTint && !this.customColor) {
                    this.setColor(new Color(visu.tint.r, visu.tint.g, visu.tint.b));
                } else {
                    this.setColor(new Color(this.getColorRed(), this.getColorGreen(), this.getColorBlue()));
                }

                if ((clothingItem.baseTextures.size() > 1 || visu.textureChoice > -1) && this.getIconsForTexture() != null) {
                    String icon = null;
                    if (visu.baseTexture > -1 && this.getIconsForTexture().size() > visu.baseTexture) {
                        icon = this.getIconsForTexture().get(visu.baseTexture);
                    } else if (visu.textureChoice > -1 && this.getIconsForTexture().size() > visu.textureChoice) {
                        icon = this.getIconsForTexture().get(visu.textureChoice);
                    }

                    if (!StringUtils.isNullOrWhitespace(icon)) {
                        this.texture = Texture.trygetTexture("Item_" + icon);
                        if (this.texture == null) {
                            this.texture = Texture.getSharedTexture("media/inventory/Question_On.png");
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the containerX
     */
    public int getContainerX() {
        return this.containerX;
    }

    /**
     * 
     * @param containerX the containerX to set
     */
    public void setContainerX(int containerX) {
        this.containerX = containerX;
    }

    /**
     * @return the containerY
     */
    public int getContainerY() {
        return this.containerY;
    }

    /**
     * 
     * @param containerY the containerY to set
     */
    public void setContainerY(int containerY) {
        this.containerY = containerY;
    }

    /**
     * @return the DisappearOnUse
     */
    public boolean isDisappearOnUse() {
        return this.scriptItem.isDisappearOnUse();
    }

    public boolean isKeepOnDeplete() {
        return !this.scriptItem.isDisappearOnUse();
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.getName(null);
    }

    public String getName(IsoPlayer player) {
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getUiName();
        } else if (this.getWorldItem() != null && this.getWorldItem().getFluidContainer() != null) {
            return this.getWorldItem().getFluidUiName();
        } else if (this.getRemoteControlID() != -1) {
            return Translator.getText("IGUI_ItemNameControllerLinked", this.name);
        } else {
            String fakeName = this.name;
            if (this.getMechanicType() > 0) {
                fakeName = Translator.getText("IGUI_ItemNameMechanicalType", fakeName, Translator.getText("IGUI_VehicleType_" + this.getMechanicType()));
            }

            String prefix = "";
            if (this.isBloody()) {
                prefix = prefix + this.bloodyString + ", ";
            }

            if (this.isBroken()) {
                prefix = prefix + this.brokenString + ", ";
            } else if (this.getCondition() < this.getConditionMax() / 3.0F) {
                prefix = prefix + this.wornString + ", ";
            }

            if (!this.isBroken() && this.hasSharpness() && this.getSharpness() < 0.33333334F) {
                if (this.getSharpness() <= 0.0F) {
                    prefix = prefix + this.bluntString + ", ";
                } else {
                    prefix = prefix + this.dullString + ", ";
                }
            }

            if (this instanceof DrainableComboItem && this.getCurrentUsesFloat() <= 0.0F || this instanceof WeaponPart && this.getCurrentUsesFloat() <= 0.0F) {
                prefix = prefix + this.emptyString + ", ";
            }

            if (this.canBeActivated() && this.isActivated()) {
                prefix = prefix + this.activatedString + ", ";
            }

            if (prefix.length() > 2) {
                prefix = prefix.substring(0, prefix.length() - 2);
            }

            prefix = prefix.trim();
            return prefix.isEmpty() ? fakeName : Translator.getText("IGUI_ClothingNaming", prefix, fakeName);
        }
    }

    /**
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        if (name.length() > 256) {
            name = name.substring(0, Math.min(name.length(), 256));
        }

        this.name = name;
    }

    /**
     * @return the replaceOnUse
     */
    public String getReplaceOnUse() {
        return this.replaceOnUse;
    }

    /**
     * 
     * @param replaceOnUse the replaceOnUse to set
     */
    public void setReplaceOnUse(String replaceOnUse) {
        this.replaceOnUse = replaceOnUse;
        this.replaceOnUseFullType = StringUtils.moduleDotType(this.getModule(), replaceOnUse);
    }

    public String getReplaceOnUseFullType() {
        return this.replaceOnUseFullType;
    }

    /**
     * @return the ConditionMax
     */
    public int getConditionMax() {
        return this.conditionMax;
    }

    /**
     * 
     * @param ConditionMax the ConditionMax to set
     */
    public void setConditionMax(int ConditionMax) {
        this.conditionMax = ConditionMax;
    }

    /**
     * @return the rightClickContainer
     */
    public ItemContainer getRightClickContainer() {
        return this.rightClickContainer;
    }

    /**
     * 
     * @param rightClickContainer the rightClickContainer to set
     */
    public void setRightClickContainer(ItemContainer rightClickContainer) {
        this.rightClickContainer = rightClickContainer;
    }

    /**
     * @return the swingAnim
     */
    public String getSwingAnim() {
        return this.scriptItem.swingAnim;
    }

    /**
     * @return the texture
     */
    public Texture getTexture() {
        return this.texture;
    }

    public Texture getIcon() {
        return this.getTexture();
    }

    /**
     * 
     * @param texture the texture to set
     */
    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public void setIcon(Texture texture) {
        this.setTexture(texture);
    }

    /**
     * @return the texturerotten
     */
    public Texture getTexturerotten() {
        return this.texturerotten;
    }

    /**
     * 
     * @param texturerotten the texturerotten to set
     */
    public void setTexturerotten(Texture texturerotten) {
        this.texturerotten = texturerotten;
    }

    /**
     * @return the textureCooked
     */
    public Texture getTextureCooked() {
        return this.textureCooked;
    }

    /**
     * 
     * @param textureCooked the textureCooked to set
     */
    public void setTextureCooked(Texture textureCooked) {
        this.textureCooked = textureCooked;
    }

    /**
     * @return the textureBurnt
     */
    public Texture getTextureBurnt() {
        return this.textureBurnt;
    }

    /**
     * 
     * @param textureBurnt the textureBurnt to set
     */
    public void setTextureBurnt(Texture textureBurnt) {
        this.textureBurnt = textureBurnt;
    }

    /**
     * 
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
        this.fullType = this.module + "." + type;
    }

    public void setCurrentUses(int newuses) {
        this.uses = newuses;
    }

    public int getCurrentUses() {
        return this.uses;
    }

    public void setCurrentUsesFrom(InventoryItem other) {
        float delta = (float)other.getCurrentUses() / other.getMaxUses();
        this.setCurrentUses((int)(delta * this.getMaxUses()));
    }

    public int getMaxUses() {
        return 1;
    }

    public float getCurrentUsesFloat() {
        return (float)this.uses / this.getMaxUses();
    }

    public void setCurrentUsesFloat(float newUses) {
        newUses = PZMath.clamp(newUses, 0.0F, 1.0F);
        this.uses = Math.round(newUses / this.useDelta);
    }

    public float getUseDelta() {
        return this.useDelta;
    }

    public void setUseDelta(float useDelta) {
        this.useDelta = useDelta;
    }

    /**
     * @return the uses
     */
    @Deprecated
    public int getUses() {
        return this.uses;
    }

    /**
     * 
     * @param newuses the uses to set
     */
    @Deprecated
    public void setUses(int newuses) {
        this.uses = newuses;
    }

    public void setUsesFrom(InventoryItem other) {
        this.setUses(other.getUses());
    }

    /**
     * @return the Age
     */
    public float getAge() {
        return this.age;
    }

    /**
     * 
     * @param Age the Age to set
     */
    public void setAge(float Age) {
        this.age = Age;
    }

    public float getLastAged() {
        return this.lastAged;
    }

    public void setLastAged(float time) {
        this.lastAged = time;
    }

    public void updateAge() {
    }

    public void setAutoAge() {
    }

    /**
     * @return the IsCookable
     */
    public boolean isIsCookable() {
        return this.isCookable;
    }

    /**
     * @return the IsCookable
     */
    public boolean isCookable() {
        return this.isCookable;
    }

    /**
     * 
     * @param IsCookable the IsCookable to set
     */
    public void setIsCookable(boolean IsCookable) {
        this.isCookable = IsCookable;
    }

    /**
     * @return the CookingTime
     */
    public float getCookingTime() {
        return this.cookingTime;
    }

    /**
     * 
     * @param CookingTime the CookingTime to set
     */
    public void setCookingTime(float CookingTime) {
        this.cookingTime = CookingTime;
    }

    /**
     * @return the MinutesToCook
     */
    public float getMinutesToCook() {
        return this.minutesToCook;
    }

    /**
     * 
     * @param MinutesToCook the MinutesToCook to set
     */
    public void setMinutesToCook(float MinutesToCook) {
        this.minutesToCook = MinutesToCook;
    }

    /**
     * @return the MinutesToBurn
     */
    public float getMinutesToBurn() {
        return this.minutesToBurn;
    }

    /**
     * 
     * @param MinutesToBurn the MinutesToBurn to set
     */
    public void setMinutesToBurn(float MinutesToBurn) {
        this.minutesToBurn = MinutesToBurn;
    }

    /**
     * @return the Cooked
     */
    public boolean isCooked() {
        return this.cooked;
    }

    /**
     * 
     * @param Cooked the Cooked to set
     */
    public void setCooked(boolean Cooked) {
        this.cooked = Cooked;
        if (Cooked && this.getCookingTime() < this.getMinutesToCook()) {
            this.setCookingTime(this.getMinutesToCook());
        }
    }

    /**
     * @return the Burnt
     */
    public boolean isBurnt() {
        return this.burnt;
    }

    /**
     * 
     * @param Burnt the Burnt to set
     */
    public void setBurnt(boolean Burnt) {
        this.burnt = Burnt;
        if (Burnt && this.getCookingTime() < this.getMinutesToBurn()) {
            this.setCookingTime(this.getMinutesToBurn());
        }
    }

    /**
     * @return the OffAge
     */
    public int getOffAge() {
        return this.offAge;
    }

    /**
     * 
     * @param OffAge the OffAge to set
     */
    public void setOffAge(int OffAge) {
        this.offAge = OffAge;
    }

    /**
     * @return the OffAgeMax
     */
    public int getOffAgeMax() {
        return this.offAgeMax;
    }

    /**
     * 
     * @param OffAgeMax the OffAgeMax to set
     */
    public void setOffAgeMax(int OffAgeMax) {
        this.offAgeMax = OffAgeMax;
    }

    /**
     * @return the Weight
     */
    public float getWeight() {
        return Math.max(this.weight, 0.0F);
    }

    /**
     * 
     * @param Weight the Weight to set
     */
    public void setWeight(float Weight) {
        if (Weight < 0.0F) {
            Weight = 0.0F;
        }

        this.weight = Weight;
    }

    /**
     * @return the ActualWeight
     */
    public float getActualWeight() {
        return this.getDisplayName().equals(this.getFullType()) ? 0.0F : Math.max(this.actualWeight, 0.0F);
    }

    /**
     * 
     * @param ActualWeight the ActualWeight to set
     */
    public void setActualWeight(float ActualWeight) {
        if (ActualWeight < 0.0F) {
            ActualWeight = 0.0F;
        }

        this.actualWeight = ActualWeight;
    }

    /**
     * @return the WorldTexture
     */
    public String getWorldTexture() {
        return this.worldTexture;
    }

    /**
     * 
     * @param WorldTexture the WorldTexture to set
     */
    public void setWorldTexture(String WorldTexture) {
        this.worldTexture = WorldTexture;
    }

    /**
     * @return the Description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 
     * @param Description the Description to set
     */
    public void setDescription(String Description) {
        this.description = Description;
    }

    public void incrementCondition(int increment) {
        this.condition += increment;
    }

    /**
     * @return the Condition
     */
    public int getCondition() {
        return this.condition;
    }

    public void setCondition(int Condition, boolean doSound) {
        if (!Core.debug || !DebugOptions.instance.cheat.player.unlimitedCondition.getValue() || this.condition <= Condition) {
            Condition = Math.max(0, Condition);
            Condition = Math.min(this.getConditionMax(), Condition);
            if (doSound && this.condition > 0 && Condition <= 0) {
                this.doBreakSound();
            } else if (doSound && this.condition > 0 && Condition < this.condition) {
                this.doDamagedSound();
            }

            this.condition = Condition;
            this.setBroken(Condition <= 0);
        }
    }

    public void doBreakSound() {
        IsoPlayer player = this.getPlayer();
        if (player != null) {
            if (!StringUtils.isNullOrWhitespace(this.getBreakSound())) {
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(
                        PacketTypes.PacketType.PlaySound, PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), this.getBreakSound(), false, player
                    );
                    return;
                }

                player.playSound(this.getBreakSound());
            } else if (!StringUtils.isNullOrWhitespace(this.getDamagedSound())) {
                this.doDamagedSound();
            }
        }
    }

    public void doDamagedSound() {
        IsoPlayer player = this.getPlayer();
        if (player != null) {
            if (!StringUtils.isNullOrWhitespace(this.getDamagedSound())) {
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(
                        PacketTypes.PacketType.PlaySound,
                        PZMath.fastfloor(player.getX()),
                        PZMath.fastfloor(player.getY()),
                        this.getDamagedSound(),
                        false,
                        player
                    );
                    return;
                }

                player.playSound(this.getDamagedSound());
            }
        }
    }

    /**
     * 
     * @param Condition the Condition to set
     */
    public void setCondition(int Condition) {
        this.setCondition(Condition, true);
    }

    public void setConditionNoSound(int Condition) {
        this.setCondition(Condition, false);
    }

    public void setConditionWhileLoading(int Condition) {
        this.condition = PZMath.clamp(Condition, 0, this.getConditionMax());
        this.broken = this.condition <= 0;
    }

    /**
     * @return the OffString
     */
    public String getOffString() {
        return this.offString;
    }

    /**
     * 
     * @param OffString the OffString to set
     */
    public void setOffString(String OffString) {
        this.offString = OffString;
    }

    /**
     * @return the CookedString
     */
    public String getCookedString() {
        return this.cookedString;
    }

    /**
     * 
     * @param CookedString the CookedString to set
     */
    public void setCookedString(String CookedString) {
        this.cookedString = CookedString;
    }

    /**
     * @return the UnCookedString
     */
    public String getUnCookedString() {
        return this.unCookedString;
    }

    /**
     * 
     * @param UnCookedString the UnCookedString to set
     */
    public void setUnCookedString(String UnCookedString) {
        this.unCookedString = UnCookedString;
    }

    /**
     * @return the BurntString
     */
    public String getBurntString() {
        return this.burntString;
    }

    /**
     * 
     * @param BurntString the BurntString to set
     */
    public void setBurntString(String BurntString) {
        this.burntString = BurntString;
    }

    /**
     * @return the module
     */
    public String getModule() {
        return this.module;
    }

    /**
     * 
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
        this.fullType = module + "." + this.type;
    }

    /**
     * @return the AlwaysWelcomeGift
     */
    public boolean isAlwaysWelcomeGift() {
        return this.scriptItem.isAlwaysWelcomeGift();
    }

    /**
     * @return the CanBandage
     */
    public boolean isCanBandage() {
        return this.scriptItem.isCanBandage();
    }

    /**
     * @return the boredomChange
     */
    public float getBoredomChange() {
        return this.boredomChange;
    }

    /**
     * 
     * @param boredomChange the boredomChange to set
     */
    public void setBoredomChange(float boredomChange) {
        this.boredomChange = boredomChange;
    }

    /**
     * @return the unhappyChange
     */
    public float getUnhappyChange() {
        return this.unhappyChange;
    }

    /**
     * 
     * @param unhappyChange the unhappyChange to set
     */
    public void setUnhappyChange(float unhappyChange) {
        this.unhappyChange = unhappyChange;
    }

    /**
     * @return the stressChange
     */
    public float getStressChange() {
        return this.stressChange;
    }

    /**
     * 
     * @param stressChange the stressChange to set
     */
    public void setStressChange(float stressChange) {
        this.stressChange = stressChange;
    }

    public int getFoodSicknessChange() {
        return this.foodSicknessChange;
    }

    public void setFoodSicknessChange(int foodSicknessChange) {
        this.foodSicknessChange = foodSicknessChange;
    }

    public int getInverseCoughProbability() {
        return this.inverseCoughProbability;
    }

    public void setInverseCoughProbability(int inverseCoughProbability) {
        this.inverseCoughProbability = inverseCoughProbability;
    }

    public int getInverseCoughProbabilitySmoker() {
        return this.inverseCoughProbabilitySmoker;
    }

    public void setInverseCoughProbabilitySmoker(int inverseCoughProbabilitySmoker) {
        this.inverseCoughProbabilitySmoker = inverseCoughProbabilitySmoker;
    }

    public Set<ItemTag> getTags() {
        return this.scriptItem.getTags();
    }

    public boolean hasTag(ItemTag... tags) {
        return this.scriptItem.hasTag(tags);
    }

    public boolean hasTag(ItemTag itemTag) {
        return this.scriptItem.hasTag(itemTag);
    }

    /**
     * @return the Taken
     */
    public ArrayList<IsoObject> getTaken() {
        return this.taken;
    }

    /**
     * 
     * @param Taken the Taken to set
     */
    public void setTaken(ArrayList<IsoObject> Taken) {
        this.taken = Taken;
    }

    /**
     * @return the placeDir
     */
    public IsoDirections getPlaceDir() {
        return this.placeDir;
    }

    /**
     * 
     * @param placeDir the placeDir to set
     */
    public void setPlaceDir(IsoDirections placeDir) {
        this.placeDir = placeDir;
    }

    /**
     * @return the newPlaceDir
     */
    public IsoDirections getNewPlaceDir() {
        return this.newPlaceDir;
    }

    /**
     * 
     * @param newPlaceDir the newPlaceDir to set
     */
    public void setNewPlaceDir(IsoDirections newPlaceDir) {
        this.newPlaceDir = newPlaceDir;
    }

    public void setReplaceOnUseOn(String ReplaceOnUseOn) {
        this.replaceOnUseOn = ReplaceOnUseOn;
    }

    public String getReplaceOnUseOn() {
        return this.replaceOnUseOn;
    }

    public String getReplaceOnUseOnString() {
        String replaceWith = this.getReplaceOnUseOn();
        if (replaceWith.split("-")[0].trim().contains("WaterSource")) {
            replaceWith = replaceWith.split("-")[1];
            if (!replaceWith.contains(".")) {
                replaceWith = this.getModule() + "." + replaceWith;
            }
        }

        return replaceWith;
    }

    public String getReplaceTypes() {
        return this.scriptItem.getReplaceTypes();
    }

    public HashMap<String, String> getReplaceTypesMap() {
        return this.scriptItem.getReplaceTypesMap();
    }

    public String getReplaceType(String key) {
        return this.scriptItem.getReplaceType(key);
    }

    public boolean hasReplaceType(String key) {
        return this.scriptItem.hasReplaceType(key);
    }

    /**
     * @return the IsWaterSource
     */
    public boolean isWaterSource() {
        return this.hasComponent(ComponentType.FluidContainer)
            && !this.getFluidContainer().isEmpty()
            && (
                this.getFluidContainer().getPrimaryFluid() == Fluid.Water
                    || this.getFluidContainer().getPrimaryFluid() == Fluid.TaintedWater
                    || this.getFluidContainer().getPrimaryFluid() == Fluid.CarbonatedWater
            );
    }

    boolean CanStackNoTemp(InventoryItem item) {
        return false;
    }

    public void CopyModData(KahluaTable DefaultModData) {
        this.copyModData(DefaultModData);
    }

    public void copyModData(KahluaTable modData) {
        if (this.table != null) {
            this.table.wipe();
        }

        if (modData != null) {
            LuaManager.copyTable(this.getModData(), modData);
        }
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        if (this.canEmitLight() && GameClient.client && this.getEquipParent() != null) {
            if (this.getEquipParent().getPrimaryHandItem() == this) {
                this.getEquipParent().reportEvent("EventSetActivatedPrimary");
            } else if (this.getEquipParent().getSecondaryHandItem() == this) {
                this.getEquipParent().reportEvent("EventSetActivatedSecondary");
            }
        }
    }

    public void setActivatedRemote(boolean activated) {
        this.activated = activated;
    }

    public void setCanBeActivated(boolean activatedItem) {
        this.canBeActivated = activatedItem;
    }

    public boolean canBeActivated() {
        return this.canBeActivated;
    }

    public void setLightStrength(float lightStrength) {
        this.lightStrength = lightStrength;
    }

    public float getLightStrength() {
        return this.lightStrength;
    }

    public boolean isTorchCone() {
        return this.isTorchCone;
    }

    public void setTorchCone(boolean isTorchCone) {
        this.isTorchCone = isTorchCone;
    }

    public float getTorchDot() {
        return this.scriptItem.torchDot;
    }

    public int getLightDistance() {
        return this.lightDistance;
    }

    public void setLightDistance(int lightDistance) {
        this.lightDistance = lightDistance;
    }

    public boolean canEmitLight() {
        if (this.getLightStrength() <= 0.0F) {
            return false;
        } else {
            Drainable drainable = Type.tryCastTo(this, Drainable.class);
            return drainable == null || this.getCurrentUses() > 0;
        }
    }

    public boolean isEmittingLight() {
        return !this.canEmitLight() ? false : !this.canBeActivated() || this.isActivated();
    }

    public boolean canStoreWater() {
        return this.hasComponent(ComponentType.FluidContainer);
    }

    public float getFatigueChange() {
        return this.fatigueChange;
    }

    public void setFatigueChange(float fatigueChange) {
        this.fatigueChange = fatigueChange;
    }

    /**
     * Return the real condition of the weapon, based on this calcul :
     *  Condition/ConditionMax * 100
     * @return float
     */
    public float getCurrentCondition() {
        return (float)this.condition / this.conditionMax * 100.0F;
    }

    public void setColor(Color color) {
        this.col = color;
    }

    public Color getColor() {
        return this.col;
    }

    public ColorInfo getColorInfo() {
        return new ColorInfo(this.col.getRedFloat(), this.col.getGreenFloat(), this.col.getBlueFloat(), this.col.getAlphaFloat());
    }

    public boolean isTwoHandWeapon() {
        return this.scriptItem.twoHandWeapon;
    }

    public String getCustomMenuOption() {
        return this.customMenuOption;
    }

    public void setCustomMenuOption(String customMenuOption) {
        this.customMenuOption = customMenuOption;
    }

    public void setTooltip(String tooltip) {
        this.getModData().rawset("Tooltip", tooltip);
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return this.getModData().rawget("Tooltip") instanceof String str ? str : this.tooltip;
    }

    public String getDisplayCategory() {
        return this.displayCategory;
    }

    public void setDisplayCategory(String displayCategory) {
        this.displayCategory = displayCategory;
    }

    public int getHaveBeenRepaired() {
        return this.haveBeenRepaired;
    }

    public void setHaveBeenRepaired(int haveBeenRepaired) {
        this.haveBeenRepaired = haveBeenRepaired;
    }

    public int getTimesRepaired() {
        return this.haveBeenRepaired;
    }

    public void setTimesRepaired(int haveBeenRepaired) {
        this.haveBeenRepaired = haveBeenRepaired;
    }

    public void copyTimesRepairedFrom(InventoryItem item) {
        this.setTimesRepaired(item.getTimesRepaired());
    }

    public void copyTimesRepairedTo(InventoryItem item) {
        item.setTimesRepaired(this.getTimesRepaired());
    }

    public int getTimesHeadRepaired() {
        return this.attrib() != null && this.attrib().contains(Attribute.TimesHeadRepaired)
            ? this.attrib().get(Attribute.TimesHeadRepaired)
            : this.haveBeenRepaired;
    }

    public void setTimesHeadRepaired(int haveBeenRepaired) {
        if (this.attrib() != null && this.attrib().contains(Attribute.TimesHeadRepaired)) {
            this.attrib().set(Attribute.TimesHeadRepaired, haveBeenRepaired);
        } else {
            this.haveBeenRepaired = haveBeenRepaired;
        }
    }

    public boolean hasTimesHeadRepaired() {
        return this.attrib() != null && this.attrib().contains(Attribute.TimesHeadRepaired);
    }

    public void copyTimesHeadRepairedFrom(InventoryItem item) {
        this.setTimesHeadRepaired(item.getTimesHeadRepaired());
    }

    public void copyTimesHeadRepairedTo(InventoryItem item) {
        item.setTimesHeadRepaired(this.getTimesHeadRepaired());
    }

    public boolean isBroken() {
        return this.broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
        if (!GameClient.client && broken) {
            this.onBreak();
        }
    }

    public String getDisplayName() {
        return this.name;
    }

    public boolean isTrap() {
        return this.scriptItem.trap;
    }

    public void addExtraItem(ItemKey key) {
        this.addExtraItem(key.toString());
    }

    public void addExtraItem(String type) {
        if (this.extraItems == null) {
            this.extraItems = new ArrayList<>();
        }

        this.extraItems.add(type);
    }

    public boolean haveExtraItems() {
        return this.extraItems != null && !this.extraItems.isEmpty();
    }

    public ArrayList<String> getExtraItems() {
        return this.extraItems;
    }

    public float getExtraItemsWeight() {
        if (!this.haveExtraItems()) {
            return 0.0F;
        } else {
            float extraWeight = 0.0F;

            for (int i = 0; i < this.extraItems.size(); i++) {
                InventoryItem item = InventoryItemFactory.CreateItem(this.extraItems.get(i));
                if (item != null && item.getActualWeight() > 0.0F) {
                    extraWeight += item.getActualWeight();
                }
            }

            return extraWeight * 0.6F;
        }
    }

    public boolean isCustomName() {
        return this.customName;
    }

    public void setCustomName(boolean customName) {
        this.customName = customName;
    }

    public boolean isFishingLure() {
        return this.scriptItem.fishingLure;
    }

    public void copyConditionModData(InventoryItem other) {
        if (other.hasModData()) {
            KahluaTableIterator it = other.getModData().iterator();

            while (it.advance()) {
                if (it.getKey() instanceof String && ((String)it.getKey()).startsWith("condition:")) {
                    this.getModData().rawset(it.getKey(), it.getValue());
                }
            }
        }
    }

    public void setConditionFromModData(InventoryItem other) {
        if (other.hasModData()) {
            if (other.getModData().rawget("condition:" + this.getType()) instanceof Double doubleValue) {
                this.setConditionNoSound((int)Math.round(doubleValue * this.getConditionMax()));
            }
        } else if (!this.hasTag(ItemTag.DONT_INHERIT_CONDITION)) {
            this.setConditionFrom(other);
        }
    }

    public String getBreakSound() {
        return this.breakSound;
    }

    public void setBreakSound(String breakSound) {
        this.breakSound = breakSound;
    }

    public String getPlaceOneSound() {
        return this.scriptItem.getPlaceOneSound();
    }

    public String getPlaceMultipleSound() {
        return this.scriptItem.getPlaceMultipleSound();
    }

    public String getSoundByID(String ID) {
        return this.scriptItem.getSoundByID(ID);
    }

    public void setBeingFilled(boolean v) {
        this.beingFilled = v;
    }

    public boolean isBeingFilled() {
        return this.beingFilled;
    }

    public String getFillFromDispenserSound() {
        return this.scriptItem.getFillFromDispenserSound();
    }

    public String getFillFromLakeSound() {
        return this.scriptItem.getFillFromLakeSound();
    }

    public String getFillFromTapSound() {
        return this.scriptItem.getFillFromTapSound();
    }

    public String getFillFromToiletSound() {
        return this.scriptItem.getFillFromToiletSound();
    }

    public String getPourLiquidOnGroundSound() {
        if (StringUtils.equalsIgnoreCase(this.getPourType(), "Bucket") && this.hasTag(ItemTag.HAS_METAL)) {
            return "PourLiquidOnGroundMetal";
        } else {
            return StringUtils.equalsIgnoreCase(this.getPourType(), "Pot") ? "PourLiquidOnGroundMetal" : "PourLiquidOnGround";
        }
    }

    public boolean isAlcoholic() {
        return this.alcoholic;
    }

    public void setAlcoholic(boolean alcoholic) {
        this.alcoholic = alcoholic;
    }

    public float getAlcoholPower() {
        return this.alcoholPower;
    }

    public void setAlcoholPower(float alcoholPower) {
        this.alcoholPower = alcoholPower;
    }

    public float getBandagePower() {
        return this.bandagePower;
    }

    public void setBandagePower(float bandagePower) {
        this.bandagePower = bandagePower;
    }

    public float getReduceInfectionPower() {
        if (this.burnt) {
            return (int)(this.reduceInfectionPower / 3.0F);
        } else if (this.age >= this.offAge && this.age < this.offAgeMax) {
            return (int)(this.reduceInfectionPower / 1.3F);
        } else if (this.age >= this.offAgeMax) {
            return (int)(this.reduceInfectionPower / 2.2F);
        } else {
            return this.isCooked() ? this.reduceInfectionPower * 1.3F : this.reduceInfectionPower;
        }
    }

    public void setReduceInfectionPower(float reduceInfectionPower) {
        this.reduceInfectionPower = reduceInfectionPower;
    }

    public final void saveWithSize(ByteBuffer output, boolean net) throws IOException {
        int position1 = output.position();
        output.putInt(0);
        int position2 = output.position();
        this.save(output, net);
        int position3 = output.position();
        output.position(position1);
        output.putInt(position3 - position2);
        output.position(position3);
    }

    public boolean isCustomWeight() {
        return this.customWeight;
    }

    public void setCustomWeight(boolean custom) {
        this.customWeight = custom;
    }

    public float getContentsWeight() {
        if (this.ammoType != null) {
            Item scriptAmmo = ScriptManager.instance.FindItem(this.ammoType.toString());
            if (scriptAmmo != null) {
                return scriptAmmo.getActualWeight() * this.getCurrentAmmoCount();
            }
        }

        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getAmount();
        } else {
            return this.getWorldItem() != null && this.getWorldItem().hasComponent(ComponentType.FluidContainer)
                ? this.getWorldItem().getFluidContainer().getAmount()
                : 0.0F;
        }
    }

    public float getHotbarEquippedWeight() {
        return this.hasTag(ItemTag.LIGHT_WHEN_ATTACHED)
            ? (this.getActualWeight() + this.getContentsWeight()) * 0.3F
            : (this.getActualWeight() + this.getContentsWeight()) * 0.7F;
    }

    public float getEquippedWeight() {
        return (this.getActualWeight() + this.getContentsWeight()) * 0.3F;
    }

    public float getUnequippedWeight() {
        return this.getActualWeight() + this.getContentsWeight();
    }

    public boolean isEquipped() {
        if (this.getContainer() == null) {
            return false;
        } else if (this.getContainer().getParent() instanceof IsoGameCharacter chr) {
            return chr.isEquipped(this);
        } else {
            return this.getContainer().getParent() instanceof IsoDeadBody deadBody ? deadBody.isEquipped(this) : false;
        }
    }

    public IsoGameCharacter getUser() {
        return this.getContainer() != null
                && this.getContainer().getParent() instanceof IsoGameCharacter
                && ((IsoGameCharacter)this.getContainer().getParent()).isEquipped(this)
            ? (IsoGameCharacter)this.getContainer().getParent()
            : null;
    }

    public IsoGameCharacter getOwner() {
        return this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter
            ? (IsoGameCharacter)this.getContainer().getParent()
            : null;
    }

    public int getKeyId() {
        return this.keyId;
    }

    public void setKeyId(int keyId) {
        this.keyId = keyId;
    }

    public boolean isRemoteController() {
        return this.remoteController;
    }

    public void setRemoteController(boolean remoteController) {
        this.remoteController = remoteController;
    }

    public boolean canBeRemote() {
        return this.canBeRemote;
    }

    public void setCanBeRemote(boolean canBeRemote) {
        this.canBeRemote = canBeRemote;
    }

    public int getRemoteControlID() {
        return this.remoteControlId;
    }

    public void setRemoteControlID(int remoteControlId) {
        this.remoteControlId = remoteControlId;
    }

    public int getRemoteRange() {
        return this.remoteRange;
    }

    public void setRemoteRange(int remoteRange) {
        this.remoteRange = remoteRange;
    }

    public String getExplosionSound() {
        return this.explosionSound;
    }

    public void setExplosionSound(String explosionSound) {
        this.explosionSound = explosionSound;
    }

    public String getCountDownSound() {
        return this.countDownSound;
    }

    public void setCountDownSound(String sound) {
        this.countDownSound = sound;
    }

    public float getColorRed() {
        return this.colorRed;
    }

    public void setColorRed(float colorRed) {
        this.colorRed = colorRed;
    }

    public float getColorGreen() {
        return this.colorGreen;
    }

    public void setColorGreen(float colorGreen) {
        this.colorGreen = colorGreen;
    }

    public float getColorBlue() {
        return this.colorBlue;
    }

    public void setColorBlue(float colorBlue) {
        this.colorBlue = colorBlue;
    }

    public String getEvolvedRecipeName() {
        return this.evolvedRecipeName;
    }

    public void setEvolvedRecipeName(String evolvedRecipeName) {
        this.evolvedRecipeName = evolvedRecipeName;
    }

    public float getMetalValue() {
        return this.metalValue;
    }

    public void setMetalValue(float metalValue) {
        this.metalValue = metalValue;
    }

    public float getItemHeat() {
        return this.itemHeat;
    }

    public void setItemHeat(float itemHeat) {
        if (itemHeat > 3.0F) {
            itemHeat = 3.0F;
        }

        if (itemHeat < 0.0F) {
            itemHeat = 0.0F;
        }

        this.itemHeat = itemHeat;
    }

    public float getInvHeat() {
        return 1.0F - this.itemHeat;
    }

    public float getMeltingTime() {
        return this.meltingTime;
    }

    public void setMeltingTime(float meltingTime) {
        if (meltingTime > 100.0F) {
            meltingTime = 100.0F;
        }

        if (meltingTime < 0.0F) {
            meltingTime = 0.0F;
        }

        this.meltingTime = meltingTime;
    }

    public String getWorker() {
        return this.worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public int getID() {
        return this.id;
    }

    public void setID(int itemId) {
        this.id = itemId;
    }

    public boolean isWet() {
        return this.isWet;
    }

    public void setWet(boolean isWet) {
        this.isWet = isWet;
    }

    public float getWetCooldown() {
        return this.wetCooldown;
    }

    public void setWetCooldown(float wetCooldown) {
        this.wetCooldown = wetCooldown;
    }

    public String getItemWhenDry() {
        return this.itemWhenDry;
    }

    public void setItemWhenDry(String itemWhenDry) {
        this.itemWhenDry = itemWhenDry;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public void setFavorite(boolean favorite) {
        this.setFavorite(favorite, false);
    }

    public void setFavorite(boolean favorite, boolean isSyncNeeded) {
        this.favorite = favorite;
        if (isSyncNeeded) {
            this.syncItemFields();
        }
    }

    public ArrayList<String> getRequireInHandOrInventory() {
        return this.requireInHandOrInventory;
    }

    public void setRequireInHandOrInventory(ArrayList<String> requireInHandOrInventory) {
        this.requireInHandOrInventory = requireInHandOrInventory;
    }

    public boolean isCustomColor() {
        return this.customColor;
    }

    public void setCustomColor(boolean customColor) {
        this.customColor = customColor;
    }

    public void doBuildingStash() {
        if (this.stashMap != null) {
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.ReadAnnotedMap, this.stashMap);
            } else {
                StashSystem.prepareBuildingStash(this.stashMap);
            }
        }
    }

    public void setStashMap(String stashMap) {
        this.stashMap = stashMap;
    }

    public String getStashMap() {
        return this.stashMap;
    }

    public int getMechanicType() {
        return this.scriptItem.vehicleType;
    }

    public float getItemCapacity() {
        return this.itemCapacity;
    }

    public void setItemCapacity(float capacity) {
        this.itemCapacity = capacity;
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public boolean isConditionAffectsCapacity() {
        return this.scriptItem != null && this.scriptItem.isConditionAffectsCapacity();
    }

    public float getBrakeForce() {
        return this.brakeForce;
    }

    public void setBrakeForce(float brakeForce) {
        this.brakeForce = brakeForce;
    }

    public float getDurability() {
        return this.durability;
    }

    public void setDurability(float durability) {
        this.durability = durability;
    }

    public int getChanceToSpawnDamaged() {
        return this.chanceToSpawnDamaged;
    }

    public void setChanceToSpawnDamaged(int chanceToSpawnDamaged) {
        this.chanceToSpawnDamaged = chanceToSpawnDamaged;
    }

    public float getConditionLowerNormal() {
        return this.conditionLowerNormal;
    }

    public void setConditionLowerNormal(float conditionLowerNormal) {
        this.conditionLowerNormal = conditionLowerNormal;
    }

    public float getConditionLowerOffroad() {
        return this.conditionLowerOffroad;
    }

    public void setConditionLowerOffroad(float conditionLowerOffroad) {
        this.conditionLowerOffroad = conditionLowerOffroad;
    }

    public float getWheelFriction() {
        return this.wheelFriction;
    }

    public void setWheelFriction(float wheelFriction) {
        this.wheelFriction = wheelFriction;
    }

    public float getSuspensionDamping() {
        return this.suspensionDamping;
    }

    public void setSuspensionDamping(float suspensionDamping) {
        this.suspensionDamping = suspensionDamping;
    }

    public float getSuspensionCompression() {
        return this.suspensionCompression;
    }

    public void setSuspensionCompression(float suspensionCompression) {
        this.suspensionCompression = suspensionCompression;
    }

    public void setInfected(boolean infected) {
        this.zombieInfected = infected;
    }

    public boolean isInfected() {
        return this.zombieInfected;
    }

    public float getEngineLoudness() {
        return this.engineLoudness;
    }

    public void setEngineLoudness(float engineLoudness) {
        this.engineLoudness = engineLoudness;
    }

    public String getStaticModel() {
        if (this.getModData().rawget("staticModel") != null) {
            return (String)this.getModData().rawget("staticModel");
        } else {
            return this.modelIndex != -1 && this.getStaticModelsByIndex() != null
                ? this.getStaticModelsByIndex().get(this.modelIndex)
                : this.scriptItem.getStaticModel();
        }
    }

    public void setStaticModel(String model) {
        this.getModData().rawset("staticModel", model);
    }

    public void setStaticModel(ModelKey model) {
        this.setStaticModel(model.toString());
    }

    public String getStaticModelException() {
        return this.hasTag(ItemTag.USE_WORLD_STATIC_MODEL) ? this.getWorldStaticModel() : this.getStaticModel();
    }

    public ArrayList<String> getIconsForTexture() {
        return this.iconsForTexture;
    }

    public void setIconsForTexture(ArrayList<String> iconsForTexture) {
        this.iconsForTexture = iconsForTexture;
    }

    public float getScore(SurvivorDesc desc) {
        return 0.0F;
    }

    /**
     * @return the previousOwner
     */
    public IsoGameCharacter getPreviousOwner() {
        return this.previousOwner;
    }

    /**
     * 
     * @param previousOwner the previousOwner to set
     */
    public void setPreviousOwner(IsoGameCharacter previousOwner) {
        this.previousOwner = previousOwner;
    }

    /**
     * @return the ScriptItem
     */
    public Item getScriptItem() {
        return this.scriptItem;
    }

    /**
     * 
     * @param ScriptItem the ScriptItem to set
     */
    public void setScriptItem(Item ScriptItem) {
        this.scriptItem = ScriptItem;
    }

    private ItemType getItemType() {
        return this.itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public boolean isItemType(ItemType itemType) {
        return this.itemType == itemType;
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
        this.container = container;
    }

    public ArrayList<BloodClothingType> getBloodClothingType() {
        return this.bloodClothingType;
    }

    public void setBloodClothingType(ArrayList<BloodClothingType> bloodClothingType) {
        this.bloodClothingType = bloodClothingType;
    }

    public void setBlood(BloodBodyPartType bodyPartType, float amount) {
        ItemVisual itemVisual = this.getVisual();
        if (itemVisual != null) {
            itemVisual.setBlood(bodyPartType, amount);
        }
    }

    public float getBlood(BloodBodyPartType bodyPartType) {
        ItemVisual itemVisual = this.getVisual();
        return itemVisual != null ? itemVisual.getBlood(bodyPartType) : 0.0F;
    }

    public void setDirt(BloodBodyPartType bodyPartType, float amount) {
        ItemVisual itemVisual = this.getVisual();
        if (itemVisual != null) {
            itemVisual.setDirt(bodyPartType, amount);
        }
    }

    public float getDirt(BloodBodyPartType bodyPartType) {
        ItemVisual itemVisual = this.getVisual();
        return itemVisual != null ? itemVisual.getDirt(bodyPartType) : 0.0F;
    }

    public String getClothingItemName() {
        return this.scriptItem.clothingItem;
    }

    public int getStashChance() {
        return this.stashChance;
    }

    public void setStashChance(int stashChance) {
        this.stashChance = stashChance;
    }

    public String getEatType() {
        return this.scriptItem.eatType;
    }

    public String getPourType() {
        return this.scriptItem.pourType;
    }

    public boolean isUseWorldItem() {
        return this.scriptItem.useWorldItem;
    }

    public AmmoType getAmmoType() {
        return this.ammoType;
    }

    public void setAmmoType(AmmoType ammoType) {
        this.ammoType = ammoType;
    }

    public int getMaxAmmo() {
        return this.maxAmmo;
    }

    public void setMaxAmmo(int maxAmmoCount) {
        this.maxAmmo = maxAmmoCount;
    }

    public int getCurrentAmmoCount() {
        return this.currentAmmoCount;
    }

    public void setCurrentAmmoCount(int ammo) {
        this.currentAmmoCount = ammo;
    }

    public String getGunType() {
        return this.gunType;
    }

    public void setGunType(String gunType) {
        this.gunType = gunType;
    }

    public boolean hasBlood() {
        if (this instanceof Clothing) {
            if (this.getBloodClothingType() == null || this.getBloodClothingType().isEmpty()) {
                return false;
            }

            ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
            if (coveredParts == null) {
                return false;
            }

            for (int i = 0; i < coveredParts.size(); i++) {
                if (this.getBlood(coveredParts.get(i)) > 0.0F) {
                    return true;
                }
            }
        } else {
            if (this instanceof HandWeapon) {
                return this.getBloodLevel() > 0.0F;
            }

            if (this instanceof InventoryContainer) {
                return this.getBloodLevel() > 0.0F;
            }
        }

        return false;
    }

    public boolean hasDirt() {
        if (this instanceof Clothing) {
            if (this.getBloodClothingType() == null || this.getBloodClothingType().isEmpty()) {
                return false;
            }

            ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
            if (coveredParts == null) {
                return false;
            }

            for (int i = 0; i < coveredParts.size(); i++) {
                if (this.getDirt(coveredParts.get(i)) > 0.0F) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getAttachmentType() {
        return this.attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public int getAttachedSlot() {
        return this.attachedSlot;
    }

    public void setAttachedSlot(int attachedSlot) {
        this.attachedSlot = attachedSlot;
    }

    public ArrayList<String> getAttachmentsProvided() {
        return this.attachmentsProvided;
    }

    public void setAttachmentsProvided(ArrayList<String> attachmentsProvided) {
        this.attachmentsProvided = attachmentsProvided;
    }

    public String getAttachedSlotType() {
        return this.attachedSlotType;
    }

    public void setAttachedSlotType(String attachedSlotType) {
        this.attachedSlotType = attachedSlotType;
    }

    public String getAttachmentReplacement() {
        return this.attachmentReplacement;
    }

    public void setAttachmentReplacement(String attachementReplacement) {
        this.attachmentReplacement = attachementReplacement;
    }

    public String getAttachedToModel() {
        return this.attachedToModel;
    }

    public void setAttachedToModel(String attachedToModel) {
        this.attachedToModel = attachedToModel;
    }

    public String getFabricType() {
        return this.scriptItem.fabricType;
    }

    public String getStringItemType() {
        Item item = ScriptManager.instance.FindItem(this.getFullType());
        if (item == null || item.isItemType(null)) {
            return "Other";
        } else if (item.isItemType(ItemType.FOOD)) {
            return item.cannedFood ? "CannedFood" : "Food";
        } else if ("Ammo".equals(item.getDisplayCategory())) {
            return "Ammo";
        } else if (item.isItemType(ItemType.WEAPON) && !item.isRanged()) {
            return "MeleeWeapon";
        } else if (!item.isItemType(ItemType.WEAPON_PART)
            && (!item.isItemType(ItemType.WEAPON) || !item.isRanged())
            && (!item.isItemType(ItemType.NORMAL) || item.getAmmoType() == null)) {
            if (item.isItemType(ItemType.LITERATURE)) {
                return "Literature";
            } else if (item.medical) {
                return "Medical";
            } else if (item.survivalGear) {
                return "SurvivalGear";
            } else {
                return item.mechanicsItem ? "Mechanic" : "Other";
            }
        } else {
            return "RangedWeapon";
        }
    }

    public boolean isProtectFromRainWhileEquipped() {
        return this.scriptItem.protectFromRainWhenEquipped;
    }

    public boolean isEquippedNoSprint() {
        return this.scriptItem.equippedNoSprint;
    }

    public ItemBodyLocation getBodyLocation() {
        return this.scriptItem.getBodyLocation();
    }

    public boolean isBodyLocation(ItemBodyLocation itemBodyLocation) {
        return this.scriptItem.getBodyLocation() == itemBodyLocation;
    }

    public String getMakeUpType() {
        return this.scriptItem.makeUpType;
    }

    public boolean isHidden() {
        return this.scriptItem.isHidden();
    }

    public String getConsolidateOption() {
        return this.scriptItem.consolidateOption;
    }

    public ArrayList<String> getClothingItemExtra() {
        return this.scriptItem.clothingItemExtra;
    }

    public ArrayList<String> getClothingItemExtraOption() {
        return this.scriptItem.clothingItemExtraOption;
    }

    public String getWorldStaticItem() {
        if (this.getModData().rawget("Flatpack") == "true") {
            return "Flatpack";
        } else if (this.getModData().rawget("worldStaticModel") != null) {
            return (String)this.getModData().rawget("worldStaticModel");
        } else {
            String model = this.tryGetWorldStaticModelByIndex(this.getModelIndex());
            return model != null ? model : this.scriptItem.worldStaticModel;
        }
    }

    public String getWorldStaticModel() {
        return this.getWorldStaticItem();
    }

    public void setWorldStaticItem(String model) {
        this.getModData().rawset("worldStaticModel", model);
    }

    public void setWorldStaticModel(String model) {
        this.setWorldStaticItem(model);
    }

    public void setWorldStaticModel(ModelKey model) {
        this.setWorldStaticItem(model.toString());
    }

    public void setRegistry_id(Item itemscript) {
        if (itemscript.getFullName().equals(this.getFullType())) {
            this.registryId = itemscript.getRegistry_id();
        } else if (Core.debug) {
            WorldDictionary.DebugPrintItem(itemscript);
            throw new RuntimeException("These types should always match");
        }
    }

    public short getRegistry_id() {
        return this.registryId;
    }

    public String getModID() {
        return this.scriptItem != null && this.scriptItem.getModID() != null ? this.scriptItem.getModID() : WorldDictionary.getItemModID(this.registryId);
    }

    public String getModName() {
        return WorldDictionary.getModNameFromID(this.getModID());
    }

    public boolean isVanilla() {
        if (this.getModID() != null) {
            return this.getModID().equals("pz-vanilla");
        } else if (Core.debug) {
            WorldDictionary.DebugPrintItem(this);
            throw new RuntimeException("Item has no modID?");
        } else {
            return true;
        }
    }

    public short getRecordedMediaIndex() {
        return this.recordedMediaIndex;
    }

    public void setRecordedMediaIndex(short id) {
        this.recordedMediaIndex = id;
        if (this.recordedMediaIndex >= 0) {
            MediaData mediaData = ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.recordedMediaIndex);
            this.mediaType = -1;
            if (mediaData != null) {
                this.name = mediaData.getTranslatedItemDisplayName();
                this.mediaType = mediaData.getMediaType();
            } else {
                this.recordedMediaIndex = -1;
            }
        } else {
            this.mediaType = -1;
            this.name = this.scriptItem.getDisplayName();
        }
    }

    public void setRecordedMediaIndexInteger(int id) {
        this.setRecordedMediaIndex((short)id);
    }

    public boolean isRecordedMedia() {
        return this.recordedMediaIndex >= 0;
    }

    public MediaData getMediaData() {
        return this.isRecordedMedia() ? ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.recordedMediaIndex) : null;
    }

    public byte getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(byte b) {
        this.mediaType = b;
    }

    public void setRecordedMediaData(MediaData data) {
        if (data != null && data.getIndex() >= 0) {
            this.setRecordedMediaIndex(data.getIndex());
        }
    }

    public void setWorldZRotation(float rot) {
        this.worldZRotation = rot;
    }

    public float getWorldZRotation() {
        return this.worldZRotation;
    }

    public void setWorldYRotation(float rot) {
        this.worldYRotation = rot;
    }

    public float getWorldYRotation() {
        return this.worldYRotation;
    }

    public void setWorldXRotation(float rot) {
        this.worldXRotation = rot;
    }

    public float getWorldXRotation() {
        return this.worldXRotation;
    }

    public void randomizeWorldZRotation() {
        this.worldZRotation = Rand.Next(360);
    }

    public void setWorldScale(float scale) {
        this.worldScale = scale;
    }

    public String getLuaCreate() {
        return this.scriptItem.getLuaCreate();
    }

    public boolean isInitialised() {
        return this.isInitialised;
    }

    public void setInitialised(boolean initialised) {
        this.isInitialised = initialised;
    }

    public void initialiseItem() {
        this.setInitialised(true);
        if (this.getLuaCreate() != null) {
            Object functionObject = LuaManager.getFunctionObject(this.getLuaCreate());
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this);
            }
        }
    }

    public String getMilkReplaceItem() {
        return this.scriptItem.milkReplaceItem;
    }

    public int getMaxMilk() {
        return this.scriptItem.maxMilk;
    }

    public boolean isAnimalFeed() {
        return !StringUtils.isNullOrEmpty(this.scriptItem.animalFeedType);
    }

    public String getAnimalFeedType() {
        return this.scriptItem.animalFeedType;
    }

    public String getDigType() {
        return this.scriptItem.digType;
    }

    public String getSoundParameter(String parameterName) {
        return this.scriptItem.getSoundParameter(parameterName);
    }

    public boolean isWorn() {
        return this.IsClothing() && this.isWorn();
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public String toString() {
        return this.getFullType() + ":" + super.toString();
    }

    public Texture getTextureColorMask() {
        return this.textureColorMask;
    }

    public Texture getTextureFluidMask() {
        return this.textureFluidMask;
    }

    public void setTextureColorMask(String tex) {
        this.textureColorMask = Texture.trygetTexture(tex);
        if (this.textureColorMask == null) {
            this.textureColorMask = Texture.getSharedTexture("media/inventory/Question_On.png");
        }
    }

    public void setTextureFluidMask(String tex) {
        this.textureFluidMask = Texture.trygetTexture(tex);
        if (this.textureFluidMask == null) {
            this.textureFluidMask = Texture.getSharedTexture("media/inventory/Question_On.png");
        }
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.equipParent != null ? this.equipParent.getSquare() : null;
    }

    @Override
    public GameEntityType getGameEntityType() {
        return GameEntityType.InventoryItem;
    }

    @Override
    public long getEntityNetID() {
        return this.id;
    }

    @Override
    public float getX() {
        return this.equipParent != null ? this.equipParent.getX() : Float.MAX_VALUE;
    }

    @Override
    public float getY() {
        return this.equipParent != null ? this.equipParent.getY() : Float.MAX_VALUE;
    }

    @Override
    public float getZ() {
        return this.equipParent != null ? this.equipParent.getZ() : Float.MAX_VALUE;
    }

    @Override
    public boolean isEntityValid() {
        return this.getEquipParent() != null;
    }

    public static boolean RemoveFromContainer(InventoryItem item) {
        ItemContainer container = item.getContainer();
        if (container != null) {
            if (container.getType().equals("floor") && item.getWorldItem() != null && item.getWorldItem().getSquare() != null) {
                item.getWorldItem().getSquare().transmitRemoveItemFromSquare(item.getWorldItem());
                item.getWorldItem().getSquare().getWorldObjects().remove(item.getWorldItem());
                item.getWorldItem().getSquare().getObjects().remove(item.getWorldItem());
                item.setWorldItem(null);
            }

            container.DoRemoveItem(item);
            return true;
        } else {
            return false;
        }
    }

    public AnimalTracks getAnimalTracks() {
        return this.animalTracks;
    }

    public void setAnimalTracks(AnimalTracks animalTracks) {
        this.animalTracks = animalTracks;
    }

    public void syncItemFields() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer != null && outer.getParent() instanceof IsoPlayer) {
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, outer.getParent(), this);
            } else if (GameServer.server) {
                INetworkPacket.send((IsoPlayer)outer.getParent(), PacketTypes.PacketType.SyncItemFields, outer.getParent(), this);
            }
        }
    }

    public void checkSyncItemFields(boolean b) {
        if (b) {
            this.syncItemFields();
        }
    }

    public String getWithDrainable() {
        return this.scriptItem.getWithDrainable();
    }

    public String getWithoutDrainable() {
        return this.scriptItem.getWithoutDrainable();
    }

    public ArrayList<String> getStaticModelsByIndex() {
        return this.staticModelsByIndex;
    }

    public void setStaticModelsByIndex(ArrayList<String> staticModelsByIndex) {
        this.staticModelsByIndex = staticModelsByIndex;
    }

    public ArrayList<String> getWorldStaticModelsByIndex() {
        return this.worldStaticModelsByIndex;
    }

    public void setWorldStaticModelsByIndex(ArrayList<String> staticModelsByIndex) {
        this.worldStaticModelsByIndex = staticModelsByIndex;
    }

    public String tryGetWorldStaticModelByIndex(int index) {
        ArrayList<String> models = this.getWorldStaticModelsByIndex();
        return models != null && index >= 0 && index < models.size() ? models.get(index) : null;
    }

    public int getModelIndex() {
        return this.modelIndex;
    }

    public void setModelIndex(int index) {
        this.modelIndex = index;
        this.synchWithVisual();
    }

    public float getVisionModifier() {
        return this.scriptItem.getVisionModifier();
    }

    public float getHearingModifier() {
        return this.scriptItem.getHearingModifier();
    }

    public String getWorldObjectSprite() {
        return this.scriptItem.getWorldObjectSprite();
    }

    public float getStrainModifier() {
        return this.scriptItem.getStrainModifier();
    }

    public int getConditionLowerChance() {
        return this.scriptItem.getConditionLowerChance();
    }

    public void setConditionFrom(InventoryItem item) {
        if (item != null) {
            if (this.hasSharpness() && item.hasSharpness()) {
                this.setSharpness(item.getSharpness());
            }

            if (this.getConditionMax() == item.getConditionMax()) {
                this.setConditionNoSound(item.getCondition());
            } else {
                float perc = (float)item.getCondition() / item.getConditionMax();
                this.setConditionNoSound((int)(this.getConditionMax() * perc));
                this.setTimesRepaired(item.getTimesRepaired());
            }
        }
    }

    public void setConditionTo(InventoryItem item) {
        if (item != null) {
            item.setConditionFrom(this);
        }
    }

    public void reduceCondition() {
        this.setCondition(this.getCondition() - 1);
        this.syncItemFields();
    }

    public boolean damageCheck() {
        return this.damageCheck(0, 1.0F);
    }

    public boolean damageCheck(int skill) {
        return this.damageCheck(skill, 1.0F);
    }

    public boolean damageCheck(int skill, float multiplier) {
        return this.damageCheck(skill, multiplier, true);
    }

    public boolean damageCheck(int skill, float multiplier, boolean maintenance) {
        return this.damageCheck(skill, multiplier, maintenance, true);
    }

    public boolean damageCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped) {
        return this.damageCheck(skill, multiplier, maintenance, isEquipped, null);
    }

    public boolean damageCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped, IsoGameCharacter character) {
        multiplier = Math.max(multiplier, 0.0F);
        if (maintenance) {
            skill += this.getMaintenanceMod(isEquipped, character);
        }

        boolean damage = this.sharpnessCheck(skill / 2, multiplier / 2.0F, false, isEquipped);
        if (this.headConditionCheck(skill, multiplier, false, isEquipped)) {
            damage = true;
        }

        if (Rand.NextBool((int)(this.getConditionLowerChance() * multiplier + skill))) {
            this.reduceCondition();
            return true;
        } else {
            return damage;
        }
    }

    public boolean sharpnessCheck() {
        return this.sharpnessCheck(0);
    }

    public boolean sharpnessCheck(int skill) {
        return this.sharpnessCheck(skill, 1.0F);
    }

    public boolean sharpnessCheck(int skill, float multiplier) {
        return this.sharpnessCheck(skill, multiplier, true);
    }

    public boolean sharpnessCheck(int skill, float multiplier, boolean maintenance) {
        return this.sharpnessCheck(skill, multiplier, maintenance, true);
    }

    public boolean sharpnessCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped) {
        return this.sharpnessCheck(skill, multiplier, maintenance, isEquipped, null);
    }

    private boolean sharpnessCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped, IsoGameCharacter character) {
        if (!this.hasSharpness()) {
            return false;
        } else {
            multiplier = Math.max(multiplier, 0.0F);
            int mod = 0;
            if (maintenance) {
                mod += this.getMaintenanceMod(isEquipped, character);
            }

            if (Rand.NextBool(2 * (int)(this.getConditionLowerChance() * multiplier + (skill + mod)))) {
                this.reduceSharpness();
                return true;
            } else {
                return false;
            }
        }
    }

    private void reduceSharpness() {
        if (this.hasSharpness()) {
            if (this.getSharpness() <= 0.0F) {
                if (this.hasHeadCondition()) {
                    this.reduceHeadCondition();
                } else {
                    this.reduceCondition();
                }
            } else {
                this.setSharpness(this.getSharpness() - this.getSharpnessIncrement());
            }
        }
    }

    public boolean hasSharpness() {
        return this.attrib() != null && this.attrib().getAttribute(Attribute.Sharpness) != null;
    }

    public float getSharpness() {
        if (!this.hasSharpness()) {
            return 0.0F;
        } else {
            if (this.attrib().getAttribute(Attribute.Sharpness).getFloatValue() > this.getMaxSharpness()) {
                this.applyMaxSharpness();
            }

            return this.attrib().getAttribute(Attribute.Sharpness).getFloatValue();
        }
    }

    public float getMaxSharpness() {
        if (!this.hasSharpness()) {
            return 1.0F;
        } else {
            return this.hasHeadCondition() ? (float)this.getHeadCondition() / this.getHeadConditionMax() : (float)this.getCondition() / this.getConditionMax();
        }
    }

    public void applyMaxSharpness() {
        if (this.hasSharpness()) {
            this.setSharpness(this.getMaxSharpness());
        }
    }

    public float getSharpnessMultiplier() {
        return !this.hasSharpness() ? 1.0F : (this.attrib().getAttribute(Attribute.Sharpness).getFloatValue() + 1.0F) / 2.0F;
    }

    public void setSharpness(float value) {
        if (this.hasSharpness()) {
            float max = this.getMaxSharpness();
            if (value > max) {
                value = max;
            }

            if (value < 0.0F) {
                value = 0.0F;
            }

            if (value > 1.0F) {
                value = 1.0F;
            }

            String val = String.valueOf(value);
            this.attrib().getAttribute(Attribute.Sharpness).setValueFromScriptString(val);
        }
    }

    public void setSharpnessFrom(InventoryItem item) {
        if (this.hasSharpness() && item.hasSharpness()) {
            this.setSharpness(item.getSharpness());
        }
    }

    public float getSharpnessIncrement() {
        return !this.hasSharpness() ? 0.0F : 1.0F / this.getConditionMax();
    }

    public boolean isDamaged() {
        return this.getCondition() < this.getConditionMax();
    }

    public boolean isDull() {
        return this.hasSharpness() && this.getSharpness() <= this.getMaxSharpness() / 3.0F;
    }

    public int getMaintenanceMod() {
        return this.getMaintenanceMod(true);
    }

    public int getMaintenanceMod(boolean isEquipped) {
        return this.getMaintenanceMod(isEquipped, null);
    }

    public int getMaintenanceMod(IsoGameCharacter character) {
        return this.getMaintenanceMod(false, character);
    }

    public int getMaintenanceMod(boolean isEquipped, IsoGameCharacter character) {
        if (isEquipped && !this.isEquipped()) {
            return 0;
        } else {
            if (isEquipped && character == null) {
                character = this.getUser();
            } else if (character == null) {
                character = this.getOwner();
            }

            if (character == null) {
                return 0;
            } else {
                int level = character.getPerkLevel(PerkFactory.Perks.Maintenance);
                if (this instanceof HandWeapon) {
                    level += character.getWeaponLevel((HandWeapon)this) / 2;
                }

                return level;
            }
        }
    }

    public int getWeaponLevel() {
        if (this.isEquipped() && this instanceof HandWeapon weapon) {
            WeaponType weaponType = WeaponType.getWeaponType((HandWeapon)this);
            int level = -1;
            if (weaponType != null && weaponType != WeaponType.UNARMED) {
                if (weapon.isOfWeaponCategory(WeaponCategory.AXE)) {
                    level = this.getUser().getPerkLevel(PerkFactory.Perks.Axe);
                }

                if (weapon.isOfWeaponCategory(WeaponCategory.SPEAR)) {
                    level += this.getUser().getPerkLevel(PerkFactory.Perks.Spear);
                }

                if (weapon.isOfWeaponCategory(WeaponCategory.SMALL_BLADE)) {
                    level += this.getUser().getPerkLevel(PerkFactory.Perks.SmallBlade);
                }

                if (weapon.isOfWeaponCategory(WeaponCategory.LONG_BLADE)) {
                    level += this.getUser().getPerkLevel(PerkFactory.Perks.LongBlade);
                }

                if (weapon.isOfWeaponCategory(WeaponCategory.BLUNT)) {
                    level += this.getUser().getPerkLevel(PerkFactory.Perks.Blunt);
                }

                if (weapon.isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
                    level += this.getUser().getPerkLevel(PerkFactory.Perks.SmallBlunt);
                }
            }

            if (level > 10) {
                level = 10;
            }

            return level == -1 ? 0 : level;
        } else {
            return 0;
        }
    }

    public boolean headConditionCheck() {
        return this.headConditionCheck(0, 1.0F);
    }

    public boolean headConditionCheck(int skill) {
        return this.headConditionCheck(skill, 1.0F);
    }

    public boolean headConditionCheck(int skill, float multiplier) {
        return this.headConditionCheck(skill, multiplier, true);
    }

    public boolean headConditionCheck(int skill, float multiplier, boolean maintenance) {
        return this.headConditionCheck(skill, multiplier, maintenance, true);
    }

    public boolean headConditionCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped) {
        return this.headConditionCheck(skill, multiplier, maintenance, isEquipped, null);
    }

    private boolean headConditionCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped, IsoGameCharacter character) {
        if (!this.hasHeadCondition()) {
            return false;
        } else {
            multiplier = Math.max(multiplier, 0.0F);
            int mod = 0;
            if (maintenance) {
                mod += this.getMaintenanceMod(isEquipped, character);
            }

            if (Rand.NextBool((int)(this.getHeadConditionLowerChance() * multiplier + (skill + mod)))) {
                this.reduceHeadCondition();
                return true;
            } else {
                return false;
            }
        }
    }

    public int getHeadConditionLowerChance() {
        return (int)(this.getConditionLowerChance() * this.getHeadConditionLowerChanceMultiplier());
    }

    public float getHeadConditionLowerChanceMultiplier() {
        return this.scriptItem.getHeadConditionLowerChanceMultiplier();
    }

    public void reduceHeadCondition() {
        if (this.hasHeadCondition()) {
            DebugLog.log("Reduce Head Condition from " + this.getHeadCondition());
            this.setHeadCondition(this.getHeadCondition() - 1);
        }
    }

    public boolean hasHeadCondition() {
        return this.attrib() != null && this.attrib().getAttribute(Attribute.HeadCondition) != null;
    }

    public int getHeadCondition() {
        return !this.hasHeadCondition() ? 0 : this.attrib().getAttribute(Attribute.HeadCondition).getIntValue();
    }

    public int getHeadConditionMax() {
        if (!this.hasHeadCondition()) {
            return 0;
        } else {
            return this.attrib() != null && this.attrib().getAttribute(Attribute.HeadConditionMax) != null
                ? this.attrib().get(Attribute.HeadConditionMax)
                : this.getConditionMax();
        }
    }

    public void setHeadCondition(int value) {
        if (this.hasHeadCondition()) {
            if (value < 0) {
                value = 0;
            }

            int max = this.getHeadConditionMax();
            if (value > max) {
                value = max;
            }

            this.attrib().set(Attribute.HeadCondition, value);
            if (this.getHeadCondition() <= 0) {
                this.setCondition(0);
            }
        }
    }

    public void setHeadConditionFromCondition(InventoryItem item) {
        if (item != null) {
            if (this.hasHeadCondition()) {
                if (this.getHeadConditionMax() == item.getConditionMax()) {
                    this.setHeadCondition(item.getCondition());
                    if (this.hasSharpness() && item.hasSharpness()) {
                        this.setSharpness(item.getSharpness());
                    }
                } else {
                    float perc = (float)item.getCondition() / item.getConditionMax();
                    this.setHeadCondition((int)(this.getHeadConditionMax() * perc));
                    if (this.hasSharpness() && item.hasSharpness()) {
                        this.setSharpness(item.getSharpness());
                    }
                }
            }
        }
    }

    public void setConditionFromHeadCondition(InventoryItem item) {
        if (item != null) {
            if (item.hasHeadCondition()) {
                if (this.getConditionMax() == item.getHeadConditionMax()) {
                    this.setConditionNoSound(item.getHeadCondition());
                    if (this.hasSharpness() && item.hasSharpness()) {
                        this.setSharpness(item.getSharpness());
                    }
                } else {
                    float perc = (float)item.getHeadCondition() / item.getHeadConditionMax();
                    this.setConditionNoSound((int)(this.getConditionMax() * perc));
                    if (this.hasSharpness() && item.hasSharpness()) {
                        this.setSharpness(item.getSharpness());
                    }
                }
            }
        }
    }

    public boolean hasQuality() {
        return this.attrib() != null && this.attrib().getAttribute(Attribute.Quality) != null;
    }

    public int getQuality() {
        return !this.hasQuality() ? 0 : this.attrib().getAttribute(Attribute.Quality).getIntValue();
    }

    public void setQuality(int value) {
        if (this.hasQuality()) {
            if (value < -50) {
                value = -50;
            }

            if (value > 50) {
                value = 50;
            }

            String val = String.valueOf(value);
            this.attrib().getAttribute(Attribute.Quality).setValueFromScriptString(val);
        }
    }

    public String getOnBreak() {
        return this.scriptItem.getOnBreak();
    }

    public void onBreak() {
        Object functionObj = LuaManager.getFunctionObject(this.getOnBreak());
        IsoGameCharacter user = null;
        if (this.container != null && this.container.parent instanceof IsoGameCharacter isoGameCharacter) {
            user = isoGameCharacter;
        }

        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this, user);
        }
    }

    public float getBloodLevelAdjustedLow() {
        return this instanceof Clothing ? this.getBloodLevel() / 100.0F : this.getBloodLevel();
    }

    public float getBloodLevelAdjustedHigh() {
        return this instanceof Clothing ? this.getBloodLevel() : this.getBloodLevel() * 100.0F;
    }

    public float getBloodLevel() {
        return 0.0F;
    }

    public void setBloodLevel(float level) {
    }

    public void copyBloodLevelFrom(InventoryItem item) {
        this.setBloodLevel(item.getBloodLevel());
    }

    public boolean isBloody() {
        return this.getBloodLevel() > 0.25F;
    }

    public String getDamagedSound() {
        return this.scriptItem == null ? null : this.scriptItem.getDamagedSound();
    }

    public String getBulletHitArmourSound() {
        return this.scriptItem == null ? null : this.scriptItem.getBulletHitArmourSound();
    }

    public String getWeaponHitArmourSound() {
        return this.scriptItem == null ? null : this.scriptItem.getWeaponHitArmourSound();
    }

    public String getShoutType() {
        return this.scriptItem == null ? null : this.scriptItem.getShoutType();
    }

    public float getShoutMultiplier() {
        return this.scriptItem == null ? 1.0F : this.scriptItem.getShoutMultiplier();
    }

    public int getEatTime() {
        return this.scriptItem == null ? 0 : this.scriptItem.getEatTime();
    }

    public boolean isVisualAid() {
        return this.scriptItem.isVisualAid();
    }

    public float getDiscomfortModifier() {
        return this.scriptItem.getDiscomfortModifier();
    }

    public boolean hasMetal() {
        return this.getMetalValue() > 0.0F || this.hasTag(ItemTag.HAS_METAL);
    }

    public float getFireFuelRatio() {
        return this.scriptItem.getFireFuelRatio();
    }

    public float getWetness() {
        return 0.0F;
    }

    public boolean isMemento() {
        return this.hasTag(ItemTag.IS_MEMENTO) || Objects.equals(this.getDisplayCategory(), "Memento");
    }

    public void nameAfterDescriptor(SurvivorDesc desc) {
        if (desc != null) {
            String name = this.scriptItem.getDisplayName();
            name = Translator.getText(name);
            this.setName(name + ": " + desc.getForename() + " " + desc.getSurname());
        }
    }

    public void monogramAfterDescriptor(SurvivorDesc desc) {
        if (desc != null) {
            String name = this.scriptItem.getDisplayName();
            name = Translator.getText(name);
            this.setName(name + ": " + desc.getForename().charAt(0) + desc.getSurname().charAt(0));
        }
    }

    public String getLootType() {
        return ItemPickerJava.getLootType(this.scriptItem);
    }

    public boolean getIsCraftingConsumed() {
        return this.isCraftingConsumed;
    }

    public void setIsCraftingConsumed(boolean craftingConsumed) {
        this.isCraftingConsumed = craftingConsumed;
    }

    public void OnAddedToContainer(ItemContainer container) {
    }

    public void OnBeforeRemoveFromContainer(ItemContainer container) {
    }

    public IsoDeadBody getDeadBodyObject() {
        return this.deadBodyObject;
    }

    public boolean isPureWater(boolean includeTainted) {
        FluidContainer fluidContainer = this.getFluidContainerFromSelfOrWorldItem();
        if (fluidContainer != null && !fluidContainer.isEmpty()) {
            if (fluidContainer.isPureFluid(Fluid.Water)) {
                return true;
            }

            if (includeTainted && fluidContainer.isPureFluid(Fluid.TaintedWater)) {
                return true;
            }
        }

        return false;
    }

    public void copyClothing(InventoryItem otherItem) {
        if (this.getClothingItem() != null && otherItem.getClothingItem() != null) {
            Object functionObj = LuaManager.getFunctionObject("copyClothingItem");
            if (functionObj != null) {
                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, otherItem, this);
            }
        }
    }

    public void inheritFoodAgeFrom(InventoryItem otherFood) {
    }

    public void inheritOlderFoodAge(InventoryItem otherFood) {
    }

    public boolean isFood() {
        return false;
    }

    public void unsealIfNotFull() {
        if (this.getFluidContainer() != null) {
            this.getFluidContainer().unsealIfNotFull();
        }
    }

    public void randomizeCondition() {
        if (!(this instanceof HandWeapon) || ((HandWeapon)this).getPhysicsObject() != null) {
            int con = Math.max(1, Rand.Next(this.getConditionMax() + 1));
            this.setCondition(con, false);
        }
    }

    public void randomizeGeneralCondition() {
        this.randomizeCondition();
        this.randomizeHeadCondition();
        this.randomizeSharpness();
    }

    public void randomizeHeadCondition() {
        if (this.hasHeadCondition()) {
            int con = Math.max(1, Rand.Next(this.getConditionMax() + 1));
            this.setCondition(con, false);
        }
    }

    public void randomizeSharpness() {
        if (this.hasSharpness()) {
            this.setSharpness(Rand.Next(0.0F, this.getMaxSharpness()));
        }
    }

    public FluidContainer getFluidContainerFromSelfOrWorldItem() {
        FluidContainer fluidContainer = this.getFluidContainer();
        if (fluidContainer == null && this.getWorldItem() != null) {
            fluidContainer = this.getWorldItem().getFluidContainer();
        }

        return fluidContainer;
    }

    public boolean isEmptyOfFluid() {
        return this.getFluidContainer() == null ? false : this.getFluidContainer().isEmpty();
    }

    public boolean isFullOfFluid() {
        return this.getFluidContainer() == null ? false : this.getFluidContainer().isFull();
    }

    public boolean isFluidContainer() {
        return this.getFluidContainer() != null;
    }

    public boolean isSpice() {
        return this.scriptItem.isSpice();
    }

    public boolean isKeyRing() {
        return this.scriptItem.isItemType(ItemType.KEY_RING) ? true : this.hasTag(ItemTag.KEY_RING) && this instanceof InventoryContainer;
    }

    public boolean isFakeEquipped(IsoGameCharacter character) {
        if (!this.isInPlayerInventory()) {
            return false;
        } else if (character == null || character.getInventory() == null || !character.getInventory().contains(this)) {
            return false;
        } else {
            return this.getOutermostContainer() != this.getContainer() ? false : this.isKeyRing();
        }
    }

    public boolean isFakeEquipped() {
        if (!this.isInPlayerInventory()) {
            return false;
        } else {
            return this.getOutermostContainer() != this.getContainer() ? false : this.isKeyRing();
        }
    }

    public String getItemAfterCleaning() {
        return this.scriptItem.getItemAfterCleaning();
    }

    public ArrayList<String> getResearchableRecipes() {
        return this.scriptItem.getResearchableRecipes();
    }

    public ArrayList<String> getResearchableRecipes(IsoGameCharacter chr) {
        return this.scriptItem.getResearchableRecipes(chr, true);
    }

    public boolean hasResearchableRecipes() {
        return this.scriptItem.hasResearchableRecipes();
    }

    public void researchRecipes(IsoGameCharacter character) {
        if (this.scriptItem != null) {
            this.scriptItem.researchRecipes(character);
        }
    }

    public boolean hasOrigin() {
        return this.attrib() != null
            && this.attrib().getAttribute(Attribute.OriginX) != null
            && this.attrib().getAttribute(Attribute.OriginY) != null
            && (this.attrib().getAttribute(Attribute.OriginX).getIntValue() != 0 || this.attrib().getAttribute(Attribute.OriginY).getIntValue() != 0);
    }

    public boolean canHaveOrigin() {
        return this.attrib() != null && this.attrib().getAttribute(Attribute.OriginX) != null && this.attrib().getAttribute(Attribute.OriginY) != null;
    }

    public boolean setOrigin(IsoGridSquare sq) {
        return sq != null && this.canHaveOrigin() ? this.setOrigin(sq.getX(), sq.getY(), sq.getZ()) : false;
    }

    public boolean setOrigin(int x, int y) {
        return this.setOrigin(x, y, 0);
    }

    public boolean setOrigin(int x, int y, int z) {
        if (!this.canHaveOrigin()) {
            return false;
        } else {
            this.setOriginX(x);
            this.setOriginY(y);
            this.setOriginZ(z);
            return true;
        }
    }

    public void setOriginX(int value) {
        if (this.canHaveOrigin()) {
            String val = String.valueOf(value);
            this.attrib().getAttribute(Attribute.OriginX).setValueFromScriptString(val);
        }
    }

    public void setOriginY(int value) {
        if (this.canHaveOrigin()) {
            String val = String.valueOf(value);
            this.attrib().getAttribute(Attribute.OriginY).setValueFromScriptString(val);
        }
    }

    public void setOriginZ(int value) {
        if (this.canHaveOrigin()) {
            String val = String.valueOf(value);
            this.attrib().getAttribute(Attribute.OriginZ).setValueFromScriptString(val);
        }
    }

    public int getOriginX() {
        return !this.hasOrigin() ? 0 : this.attrib().getAttribute(Attribute.OriginX).getIntValue();
    }

    public int getOriginY() {
        return !this.hasOrigin() ? 0 : this.attrib().getAttribute(Attribute.OriginY).getIntValue();
    }

    public int getOriginZ() {
        return !this.hasOrigin() ? 0 : this.attrib().getAttribute(Attribute.OriginZ).getIntValue();
    }

    public ItemBodyLocation canBeEquipped() {
        return null;
    }

    public IsoPlayer getPlayer() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer != null && outer.getParent() != null && outer.getParent() instanceof IsoPlayer) {
            return (IsoPlayer)outer.getParent();
        } else {
            return this.getOwner() != null && this.getOwner() instanceof IsoPlayer ? (IsoPlayer)this.getOwner() : null;
        }
    }

    public float getWorldAlpha() {
        return this.worldAlpha;
    }

    public void setWorldAlpha(float worldAlpha) {
        this.worldAlpha = worldAlpha;
    }

    public void Remove() {
        ItemUser.RemoveItem(this);
    }

    public void SynchSpawn() {
        if (this.getContainer() != null) {
            LuaManager.GlobalObject.sendAddItemToContainer(this.getContainer(), this);
        }

        if (this.getWorldItem() != null) {
            this.getWorldItem().transmitCompleteItemToClients();
        }
    }

    public boolean isFavouriteRecipeInput(IsoPlayer player) {
        return this.scriptItem == null ? false : this.scriptItem.isFavouriteRecipeInput(player);
    }

    public void copyConditionStatesFrom(InventoryItem otherItem) {
        if (otherItem != null) {
            if (this.getClothingItem() != null && otherItem.getClothingItem() != null) {
                this.copyClothing(otherItem);
            } else {
                this.copyConditionModData(otherItem);
                this.setConditionFrom(otherItem);
                this.setHaveBeenRepaired(otherItem.getHaveBeenRepaired());
                if (this.hasHeadCondition()) {
                    this.setTimesHeadRepaired(otherItem.getTimesHeadRepaired());
                    this.setHeadConditionFromCondition(otherItem);
                }

                if (this.hasSharpness()) {
                    this.setSharpnessFrom(otherItem);
                }

                this.setFavorite(otherItem.isFavorite());
                this.copyBloodLevelFrom(otherItem);
                if (this instanceof DrainableComboItem thisDrainable && otherItem instanceof DrainableComboItem thatDrainable) {
                    thisDrainable.setUsedDelta(thatDrainable.getCurrentUsesFloat());
                }
            }
        }
    }

    public String getFileName() {
        return this.scriptItem == null ? null : this.scriptItem.getFileName();
    }

    public void setDoingExtendedPlacement(boolean enable) {
        this.doingExtendedPlacement = enable;
    }

    public boolean isDoingExtendedPlacement() {
        return this.doingExtendedPlacement;
    }

    public boolean isNoRecipes(IsoPlayer player) {
        if (player == null) {
            return false;
        } else {
            String pinnedString = getNoRecipesModDataString();
            Object noRecipeString = this.getModData().rawget(pinnedString);
            return noRecipeString != null && noRecipeString.equals(player.getFullName());
        }
    }

    public void setNoRecipes(IsoPlayer player, Boolean noCrafting) {
        String noCraftingString = getNoRecipesModDataString();
        if (noCrafting) {
            this.getModData().rawset(noCraftingString, player.getFullName());
        } else {
            this.getModData().rawset(noCraftingString, false);
        }

        this.syncItemFields();
    }

    public static String getNoRecipesModDataString() {
        return "itemNoRecipes";
    }

    public boolean isUnwanted(IsoPlayer player) {
        if (this.isRecordedMedia()) {
            return player.isUnwanted(this.getMediaData().getId());
        } else if (this.getModData().rawget("literatureTitle") != null) {
            return player.isUnwanted(this.getModData().rawget("literatureTitle").toString());
        } else if (this.getModData().rawget("printMedia") != null) {
            return player.isUnwanted(this.getModData().rawget("printMedia").toString());
        } else {
            return this.getModData().rawget("collectibleKey") != null
                ? player.isUnwanted(this.getModData().rawget("collectibleKey").toString())
                : this.scriptItem.isUnwanted(player);
        }
    }

    public void setUnwanted(IsoPlayer player, boolean unwanted) {
        if (this.isRecordedMedia()) {
            player.setUnwanted(this.getMediaData().getId(), unwanted);
        } else if (this.getModData().rawget("literatureTitle") != null) {
            player.setUnwanted(this.getModData().rawget("literatureTitle").toString(), unwanted);
        } else if (this.getModData().rawget("printMedia") != null) {
            player.setUnwanted(this.getModData().rawget("printMedia").toString(), unwanted);
        } else if (this.getModData().rawget("collectibleKey") != null) {
            player.setUnwanted(this.getModData().rawget("collectibleKey").toString(), unwanted);
        } else if (this.scriptItem != null) {
            this.scriptItem.setUnwanted(player, unwanted);
        }
    }

    public InventoryItem emptyLiquid() {
        if (this.getFluidContainer() != null) {
            this.getFluidContainer().Empty();
        }

        return this;
    }

    public String getOpeningRecipe() {
        return this.scriptItem.getOpeningRecipe();
    }

    public String getDoubleClickRecipe() {
        return this.scriptItem.getDoubleClickRecipe();
    }

    public boolean isSealed() {
        return this.getName().contains(Translator.getFluidText("Fluid_Sealed"));
    }

    public boolean hasBeenSeen(IsoPlayer player) {
        if (!this.isRecordedMedia()) {
            return false;
        } else if (this.scriptItem.getRecordedMediaCat() == null) {
            return false;
        } else {
            return !Objects.equals(this.scriptItem.getRecordedMediaCat(), "Retail-VHS") && !Objects.equals(this.scriptItem.getRecordedMediaCat(), "Home-VHS")
                ? false
                : LuaManager.GlobalObject.getZomboidRadio().getRecordedMedia().hasListenedToAll(player, this.getMediaData());
        }
    }

    public boolean hasBeenHeard(IsoPlayer player) {
        if (!this.isRecordedMedia()) {
            return false;
        } else if (this.scriptItem.getRecordedMediaCat() == null) {
            return false;
        } else {
            return !Objects.equals(this.scriptItem.getRecordedMediaCat(), "Retail-VHS") && !Objects.equals(this.scriptItem.getRecordedMediaCat(), "Home-VHS")
                ? LuaManager.GlobalObject.getZomboidRadio().getRecordedMedia().hasListenedToAll(player, this.getMediaData())
                : false;
        }
    }

    public String getReplaceOnExtinguish() {
        return this.scriptItem == null ? null : this.scriptItem.getReplaceOnExtinguish();
    }

    public InventoryItem getExtinguishedItem() {
        if (this.getReplaceOnExtinguish() == null) {
            return null;
        } else {
            InventoryItem newItem = LuaManager.GlobalObject.instanceItem(this.getReplaceOnExtinguish());
            if (newItem == null) {
                return null;
            } else {
                newItem.copyConditionStatesFrom(this);
                return newItem;
            }
        }
    }

    public boolean isSharpenable() {
        return this.hasTag(ItemTag.SHARPENABLE) && this.hasSharpness() && this.getSharpness() < this.getMaxSharpness();
    }
}
