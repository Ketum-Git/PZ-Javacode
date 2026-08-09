// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.ThumpDamageRender;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoPropertyType;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IHasHealth;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemTag;
import zombie.util.Type;

@UsedFromLua
public class IsoCompost extends IsoObject implements Thumpable, IHasHealth {
    private static final int MaximumThumpDamage = 8;
    private static final float MaximumCompost = 100.0F;
    private static final float NoWeaponCompostDamage = 50.0F;
    private static final int DefaultCapacity = 30;
    private float compost;
    private float lastUpdated = -1.0F;
    private int health = 100;
    private int maxHealth = 100;
    private float partialThumpDmg;

    public IsoCompost(IsoCell cell) {
        super(cell);
    }

    public IsoCompost(IsoCell cell, IsoGridSquare sq, String sprite) {
        this(cell, sq, IsoSpriteManager.instance.getSprite(sprite));
    }

    public IsoCompost(IsoCell cell, IsoGridSquare sq, IsoSprite sprite) {
        this.sprite = sprite;
        this.square = sq;
        this.container = new ItemContainer();
        this.container.setType("composter");
        this.container.setParent(this);
        this.container.explored = true;
        int capacity = PZMath.tryParseInt(this.sprite.getProperties().get(IsoPropertyType.CONTAINER_CAPACITY), 30);
        this.container.setCapacity(capacity);
    }

    @Override
    public void update() {
        if (!GameClient.client && this.container != null) {
            float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
            this.lastUpdated = GameTime.checkHours(this.lastUpdated, worldAgeHours);
            float elapsedHours = worldAgeHours - this.lastUpdated;
            if (!(elapsedHours <= 0.0F)) {
                this.lastUpdated = worldAgeHours;
                int compostHours = SandboxOptions.instance.getCompostHours();
                int wormCount = 0;

                for (int i = 0; i < this.container.getItems().size(); i++) {
                    InventoryItem item = this.container.getItems().get(i);
                    if (item instanceof Food food && Objects.equals(food.getFullType(), "Base.Worm") && food.isFresh()) {
                        food.setAge(0.0F);
                        wormCount++;
                    }
                }

                for (int ix = 0; ix < this.container.getItems().size(); ix++) {
                    InventoryItem item = this.container.getItems().get(ix);
                    boolean isCompostable = item.hasTag(ItemTag.IS_COMPOSTABLE);
                    if (item instanceof Food food && !item.hasTag(ItemTag.CANT_COMPOST)) {
                        if (GameServer.server && (!Objects.equals(food.getFullType(), "Base.Worm") || !food.isFresh())) {
                            food.updateAge();
                        }

                        if (food.isRotten() || isCompostable) {
                            if (this.getCompost() < 100.0F) {
                                food.setRottenTime(0.0F);
                                food.setCompostTime(food.getCompostTime() + elapsedHours);
                            }

                            if (food.getCompostTime() >= compostHours) {
                                float compostValue = Math.abs(food.getHungChange()) * 2.0F;
                                if (compostValue == 0.0F) {
                                    compostValue = Math.abs(food.getWeight()) * 10.0F;
                                }

                                this.setCompost(this.getCompost() + compostValue);
                                if (this.getCompost() > 100.0F) {
                                    this.setCompost(100.0F);
                                }

                                if (GameServer.server) {
                                    GameServer.sendCompost(this, null);
                                    GameServer.sendRemoveItemFromContainer(this.container, item);
                                }

                                boolean tooCold = "Winter".equals(ClimateManager.getInstance().getSeasonName()) && this.isOutside();
                                if (wormCount >= 2 && !tooCold) {
                                    InventoryItem worm = InventoryItemFactory.CreateItem("Base.Worm");
                                    this.container.AddItem(worm);
                                    if (GameServer.server && worm != null) {
                                        GameServer.sendAddItemToContainer(this.container, worm);
                                    }
                                }

                                item.setCurrentUses(1);
                                item.Use();
                                IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
                            }
                        }
                    }
                }

                this.updateSprite();
            }
        }
    }

    public void updateSprite() {
        if (this.getCompost() >= 10.0F && this.sprite.getName().equals("camping_01_19")) {
            this.sprite = IsoSpriteManager.instance.getSprite("camping_01_20");
            this.transmitUpdatedSpriteToClients();
        } else if (this.getCompost() < 10.0F && this.sprite.getName().equals("camping_01_20")) {
            this.sprite = IsoSpriteManager.instance.getSprite("camping_01_19");
            this.transmitUpdatedSpriteToClients();
        } else if (this.getCompost() >= 10.0F && this.sprite.getName().equals("carpentry_02_116")) {
            this.sprite = IsoSpriteManager.instance.getSprite("carpentry_02_117");
            this.transmitUpdatedSpriteToClients();
        } else if (this.getCompost() < 10.0F && this.sprite.getName().equals("carpentry_02_117")) {
            this.sprite = IsoSpriteManager.instance.getSprite("carpentry_02_116");
            this.transmitUpdatedSpriteToClients();
        }
    }

    public void syncCompost() {
        if (GameClient.client) {
            GameClient.sendCompost(this);
        } else if (GameServer.server) {
            GameServer.sendCompost(this, null);
        }
    }

    @Override
    public void sync() {
        this.syncCompost();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        if (this.container != null) {
            this.container.setType("composter");
        }

        this.compost = input.getFloat();
        this.lastUpdated = input.getFloat();
        if (worldVersion >= 213) {
            this.health = input.getInt();
            this.maxHealth = input.getInt();
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.putFloat(this.compost);
        output.putFloat(this.lastUpdated);
        output.putInt(this.health);
        output.putInt(this.maxHealth);
    }

    @Override
    public String getObjectName() {
        return "IsoCompost";
    }

    public float getCompost() {
        return this.compost;
    }

    public void setCompost(float compost) {
        this.compost = PZMath.clamp(compost, 0.0F, 100.0F);
    }

    public void remove() {
        if (this.getSquare() != null) {
            this.getSquare().transmitRemoveItemFromSquare(this);
        }
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        return this.isDestroyed() ? null : this;
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public int getHealth() {
        return this.health;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    @Override
    public int getMaxHealth() {
        return this.maxHealth;
    }

    private void dropContainedItems() {
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
    }

    @Override
    public void Thump(IsoMovingObject thumper, int thumpEventCount) {
        if (SandboxOptions.instance.lore.thumpOnConstruction.getValue()) {
            if (thumper instanceof IsoGameCharacter isoGameCharacter) {
                Thumpable thumpable = this.getThumpableFor(isoGameCharacter);
                if (thumpable == null) {
                    return;
                }
            }

            if (thumper instanceof IsoZombie) {
                int oldHealth = this.getHealth();
                int totalThumpers = thumper.getSurroundingThumpers();
                int maxThumpers = 8;
                int damagePerMaxThumpers = 1;
                if (totalThumpers >= 8) {
                    int amount = 1 * thumpEventCount;
                    this.health -= amount;
                } else {
                    this.partialThumpDmg += 1 * totalThumpers / 8.0F * thumpEventCount;
                    if ((int)this.partialThumpDmg > 0.0F) {
                        int amount = (int)this.partialThumpDmg;
                        this.health -= amount;
                        this.partialThumpDmg -= amount;
                    }
                }

                WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
                if (DebugOptions.instance.zombieRenderThump.getValue()) {
                    ThumpDamageRender.instance.thump(thumper, oldHealth - this.getHealth(), this.getHealth());
                }
            }

            if (this.isDestroyed()) {
                String breakSound = "BreakObject";
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, "BreakObject", false, thumper.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                } else {
                    ((IsoGameCharacter)thumper).getEmitter().playSound("BreakObject", this);
                }

                WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
                thumper.setThumpTarget(null);
                if (this.getObjectIndex() != -1) {
                    this.addItemsFromProperties();
                    this.dropContainedItems();
                    this.square.transmitRemoveItemFromSquare(this);
                }
            }
        }
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        if (!this.isDestroyed()) {
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (!GameClient.client) {
                LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
                ParameterMeleeHitSurface.Material material = ParameterMeleeHitSurface.Material.Wood;
                if (GameServer.server) {
                    GameServer.sendCharacterSound(owner, weapon.getDoorHitSound(), (byte)0, material);
                } else {
                    if (player != null) {
                        player.setMeleeHitSurface(material);
                    }

                    owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
                }

                if (weapon != null) {
                    this.Damage(weapon.getDoorDamage());
                } else {
                    this.Damage(50.0F);
                }

                WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                if (this.isDestroyed()) {
                    if (owner != null) {
                        String sound2 = "BreakObject";
                        owner.getEmitter().playSound("BreakObject");
                        if (GameServer.server) {
                            GameServer.PlayWorldSoundServer("BreakObject", false, owner.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                        }
                    }

                    this.addItemsFromProperties();
                    this.dropContainedItems();
                    this.square.transmitRemoveItemFromSquare(this);
                    if (!GameServer.server) {
                        this.square.RemoveTileObject(this);
                    }
                }
            }
        }
    }

    @Override
    public void Damage(float amount) {
        this.DirtySlice();
        this.health = (int)(this.health - amount);
    }

    @Override
    public boolean isDestroyed() {
        return this.health <= 0;
    }

    @Override
    public float getThumpCondition() {
        return this.getMaxHealth() <= 0 ? 0.0F : (float)PZMath.clamp(this.getHealth(), 0, this.getMaxHealth()) / this.getMaxHealth();
    }
}
