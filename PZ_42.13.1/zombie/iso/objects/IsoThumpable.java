// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.ThumpState;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.iso.BrokenFences;
import zombie.iso.IHasHealth;
import zombie.iso.ILockableDoor;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.enums.MaterialType;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.world.WorldDictionary;

@UsedFromLua
public class IsoThumpable extends IsoObject implements BarricadeAble, Thumpable, IHasHealth, ILockableDoor {
    private KahluaTable table;
    private KahluaTable modData;
    private boolean isDoor;
    private boolean isDoorFrame;
    private String breakSound = "BreakObject";
    private boolean isCorner;
    private boolean isFloor;
    private boolean blockAllTheSquare;
    public boolean locked;
    private int maxHealth = 500;
    public int health = 500;
    public int pushedMaxStrength;
    public int pushedStrength;
    protected IsoSprite closedSprite;
    public boolean north;
    private int thumpDmg = 8;
    private float crossSpeed = 1.0F;
    public boolean open;
    public IsoSprite openSprite;
    private boolean destroyed;
    private boolean canBarricade;
    public boolean canPassThrough;
    private boolean isStairs;
    private boolean isContainer;
    private boolean dismantable;
    private boolean canBePlastered;
    private boolean paintable;
    private boolean isThumpable = true;
    private boolean isHoppable;
    private int lightSourceRadius = -1;
    private int lightSourceLife = -1;
    private int lightSourceXOffset;
    private int lightSourceYOffset;
    private boolean lightSourceOn;
    private IsoLightSource lightSource;
    private String lightSourceFuel;
    private float lifeLeft = -1.0F;
    private float lifeDelta;
    private boolean haveFuel;
    private float updateAccumulator;
    private float lastUpdateHours = -1.0F;
    public int keyId = -1;
    private boolean lockedByKey;
    public boolean lockedByPadlock;
    private boolean canBeLockByPadlock;
    public int lockedByCode;
    public int oldNumPlanks;
    public String thumpSound = "ZombieThumpGeneric";
    private boolean wasTryingToggleLockedDoor;
    private boolean wasTryingToggleBarricadedDoor;
    public static final Vector2 tempo = new Vector2();
    private short lastPlayerOnlineId = -1;

    @Override
    public KahluaTable getModData() {
        if (this.modData == null) {
            this.modData = LuaManager.platform.newTable();
        }

        return this.modData;
    }

    @Override
    public void setModData(KahluaTable modData) {
        this.modData = modData;
    }

    @Override
    public boolean hasModData() {
        return this.modData != null && !this.modData.isEmpty();
    }

    /**
     * Can you pass through the item, if false we gonna test the collide default to false (so it collide)
     */
    public boolean isCanPassThrough() {
        return this.canPassThrough;
    }

    public void setCanPassThrough(boolean pCanPassThrough) {
        this.canPassThrough = pCanPassThrough;
    }

    public boolean isBlockAllTheSquare() {
        return this.blockAllTheSquare;
    }

    public void setBlockAllTheSquare(boolean blockAllTheSquare) {
        this.blockAllTheSquare = blockAllTheSquare;
    }

    public void setIsDismantable(boolean dismantable) {
        this.dismantable = dismantable;
    }

    public boolean isDismantable() {
        return this.dismantable;
    }

    public float getCrossSpeed() {
        return this.crossSpeed;
    }

    public void setCrossSpeed(float pCrossSpeed) {
        this.crossSpeed = pCrossSpeed;
    }

    public void setIsFloor(boolean pIsFloor) {
        this.isFloor = pIsFloor;
    }

    public boolean isCorner() {
        return this.isCorner;
    }

    @Override
    public boolean isFloor() {
        return this.isFloor;
    }

    public void setIsContainer(boolean pIsContainer) {
        this.isContainer = pIsContainer;
        if (pIsContainer) {
            this.container = new ItemContainer("crate", this.square, this);
            if (this.sprite.getProperties().has("ContainerCapacity")) {
                this.container.capacity = Integer.parseInt(this.sprite.getProperties().get("ContainerCapacity"));
            }

            this.container.setExplored(true);
        }
    }

    public void setIsStairs(boolean pStairs) {
        this.isStairs = pStairs;
    }

    public boolean isStairs() {
        return this.isStairs;
    }

    @Override
    public boolean isWindow() {
        return this.sprite != null && (this.sprite.getProperties().has(IsoFlagType.WindowN) || this.sprite.getProperties().has(IsoFlagType.WindowW));
    }

    public boolean isWindowN() {
        return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.WindowN);
    }

    public boolean isWindowW() {
        return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.WindowW);
    }

    @Override
    public String getObjectName() {
        return "Thumpable";
    }

    public IsoThumpable(IsoCell cell) {
        super(cell);
    }

    public void setCorner(boolean pCorner) {
        this.isCorner = pCorner;
    }

    /**
     * Can you barricade/unbarricade the item default true
     */
    public void setCanBarricade(boolean pCanBarricade) {
        this.canBarricade = pCanBarricade;
    }

    /**
     * Can you barricade/unbarricade the item
     */
    public boolean getCanBarricade() {
        return this.canBarricade;
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
        if (GameServer.server) {
            this.sync();
        }
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

    /**
     * Numbers of zeds need to hurt the object default 8
     */
    public void setThumpDmg(Integer pThumpDmg) {
        this.thumpDmg = pThumpDmg;
    }

    public int getThumpDmg() {
        return this.thumpDmg;
    }

    /**
     * The sound that be played if this object is broken default "BreakDoor"
     */
    public void setBreakSound(String pBreakSound) {
        this.breakSound = pBreakSound;
    }

    public String getBreakSound() {
        return this.breakSound;
    }

    public boolean isDoor() {
        return this.isDoor;
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else if (this.isDoor
            || this.isDoorFrame
            || this.isWindow()
            || this.isHoppable
            || this.getProperties() != null && (this.getProperties().has(IsoFlagType.collideN) || this.getProperties().has(IsoFlagType.collideW))) {
            return this.north ? pos.set(this.getX() + 0.5F, this.getY()) : pos.set(this.getX(), this.getY() + 0.5F);
        } else {
            return pos.set(this.getX() + 0.5F, this.getY() + 0.5F);
        }
    }

    public boolean isDoorFrame() {
        return this.isDoorFrame;
    }

    public void setIsDoor(boolean pIsDoor) {
        this.isDoor = pIsDoor;
    }

    public void setIsDoorFrame(boolean pIsDoorFrame) {
        this.isDoorFrame = pIsDoorFrame;
    }

    @Override
    public void setSprite(String sprite) {
        this.setSpriteFromName(sprite);
    }

    @Override
    public void setSpriteFromName(String name) {
        super.setSpriteFromName(name);
        this.closedSprite = this.sprite;
    }

    public void setClosedSprite(IsoSprite sprite) {
        this.setSprite(sprite);
        this.closedSprite = sprite;
    }

    public void setOpenSprite(IsoSprite sprite) {
        this.openSprite = sprite;
    }

    /**
     * Create an object than can be interacted by you, survivor or zombie (destroy, barricade, etc.) This one have a closed/openSprite so it can be a
     *  door for example
     */
    public IsoThumpable(IsoCell cell, IsoGridSquare gridSquare, String closedSprite, String openSprite, boolean north, KahluaTable table) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        this.openSprite = IsoSpriteManager.instance.getSprite(openSprite);
        this.closedSprite = IsoSpriteManager.instance.getSprite(closedSprite);
        this.table = table;
        this.sprite = this.closedSprite;
        this.square = gridSquare;
        this.north = north;
    }

    /**
     * Create an object than can be interacted by you, survivor or zombie (destroy, barricade, etc.) This one can be a wall, a fence, etc.
     */
    public IsoThumpable(IsoCell cell, IsoGridSquare gridSquare, String sprite, boolean north, KahluaTable table) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        this.closedSprite = IsoSpriteManager.instance.getSprite(sprite);
        this.table = table;
        this.sprite = this.closedSprite;
        this.square = gridSquare;
        this.north = north;
    }

    public IsoThumpable(IsoCell cell, IsoGridSquare gridSquare, String sprite, boolean north) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        this.closedSprite = IsoSpriteManager.instance.getSprite(sprite);
        this.sprite = this.closedSprite;
        this.square = gridSquare;
        this.north = north;
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Long, input);
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        if (!bits.equals(0)) {
            this.open = bits.hasFlags(1);
            this.locked = bits.hasFlags(2);
            this.north = bits.hasFlags(4);
            if (bits.hasFlags(8)) {
                this.maxHealth = input.getInt();
            }

            if (bits.hasFlags(16)) {
                this.health = input.getInt();
            } else {
                this.health = this.maxHealth;
            }

            if (bits.hasFlags(32)) {
                this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
            }

            if (bits.hasFlags(64)) {
                this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
            }

            if (bits.hasFlags(128)) {
                this.thumpDmg = input.getInt();
            }

            this.isDoor = bits.hasFlags(512);
            this.isDoorFrame = bits.hasFlags(1024);
            this.isCorner = bits.hasFlags(2048);
            this.isStairs = bits.hasFlags(4096);
            this.isContainer = bits.hasFlags(8192);
            this.isFloor = bits.hasFlags(16384);
            this.canBarricade = bits.hasFlags(32768);
            this.canPassThrough = bits.hasFlags(65536);
            this.dismantable = bits.hasFlags(131072);
            this.canBePlastered = bits.hasFlags(262144);
            this.paintable = bits.hasFlags(524288);
            if (bits.hasFlags(1048576)) {
                this.crossSpeed = input.getFloat();
            }

            if (bits.hasFlags(2097152)) {
                if (this.table == null) {
                    this.table = LuaManager.platform.newTable();
                }

                this.table.load(input, WorldVersion);
            }

            if (bits.hasFlags(4194304)) {
                if (this.modData == null) {
                    this.modData = LuaManager.platform.newTable();
                }

                this.modData.load(input, WorldVersion);
            }

            this.blockAllTheSquare = bits.hasFlags(8388608);
            this.isThumpable = bits.hasFlags(16777216);
            this.isHoppable = bits.hasFlags(33554432);
            if (bits.hasFlags(67108864)) {
                this.setLightSourceLife(input.getInt());
            }

            if (bits.hasFlags(134217728)) {
                this.setLightSourceRadius(input.getInt());
            }

            if (bits.hasFlags(268435456)) {
                this.setLightSourceXOffset(input.getInt());
            }

            if (bits.hasFlags(536870912)) {
                this.setLightSourceYOffset(input.getInt());
            }

            if (bits.hasFlags(1073741824)) {
                this.setLightSourceFuel(WorldDictionary.getItemTypeFromID(input.getShort()));
            }

            if (bits.hasFlags(2147483648L)) {
                this.setLifeDelta(input.getFloat());
            }

            if (bits.hasFlags(4294967296L)) {
                this.setLifeLeft(input.getFloat());
            }

            if (bits.hasFlags(8589934592L)) {
                this.keyId = input.getInt();
            }

            this.lockedByKey = bits.hasFlags(17179869184L);
            this.lockedByPadlock = bits.hasFlags(34359738368L);
            this.canBeLockByPadlock = bits.hasFlags(68719476736L);
            if (bits.hasFlags(137438953472L)) {
                this.lockedByCode = input.getInt();
            }

            if (bits.hasFlags(274877906944L)) {
                this.thumpSound = GameWindow.ReadString(input);
                if ("thumpa2".equals(this.thumpSound)) {
                    this.thumpSound = "ZombieThumpGeneric";
                }

                if ("metalthump".equals(this.thumpSound)) {
                    this.thumpSound = "ZombieThumpMetal";
                }
            }

            if (bits.hasFlags(549755813888L)) {
                this.lastUpdateHours = input.getFloat();
            }

            if (bits.hasFlags(1099511627776L)) {
                this.haveFuel = true;
            }

            if (bits.hasFlags(2199023255552L)) {
                this.lightSourceOn = true;
            }
        }

        bits.release();
        if (this.getLightSourceFuel() != null) {
            boolean wasOn = this.isLightSourceOn();
            this.createLightSource(
                this.getLightSourceRadius(),
                this.getLightSourceXOffset(),
                this.getLightSourceYOffset(),
                0,
                this.getLightSourceLife(),
                this.getLightSourceFuel(),
                null,
                null
            );
            if (this.lightSource != null) {
                this.getLightSource().setActive(wasOn);
            }

            this.setLightSourceOn(wasOn);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Long, output);
        if (this.open) {
            bits.addFlags(1);
        }

        if (this.locked) {
            bits.addFlags(2);
        }

        if (this.north) {
            bits.addFlags(4);
        }

        if (this.maxHealth != 500) {
            bits.addFlags(8);
            output.putInt(this.maxHealth);
        }

        if (this.health != this.maxHealth) {
            bits.addFlags(16);
            output.putInt(this.health);
        }

        if (this.closedSprite != null) {
            bits.addFlags(32);
            output.putInt(this.closedSprite.id);
        }

        if (this.openSprite != null) {
            bits.addFlags(64);
            output.putInt(this.openSprite.id);
        }

        if (this.thumpDmg != 8) {
            bits.addFlags(128);
            output.putInt(this.thumpDmg);
        }

        if (this.isDoor) {
            bits.addFlags(512);
        }

        if (this.isDoorFrame) {
            bits.addFlags(1024);
        }

        if (this.isCorner) {
            bits.addFlags(2048);
        }

        if (this.isStairs) {
            bits.addFlags(4096);
        }

        if (this.isContainer) {
            bits.addFlags(8192);
        }

        if (this.isFloor) {
            bits.addFlags(16384);
        }

        if (this.canBarricade) {
            bits.addFlags(32768);
        }

        if (this.canPassThrough) {
            bits.addFlags(65536);
        }

        if (this.dismantable) {
            bits.addFlags(131072);
        }

        if (this.canBePlastered) {
            bits.addFlags(262144);
        }

        if (this.paintable) {
            bits.addFlags(524288);
        }

        if (this.crossSpeed != 1.0F) {
            bits.addFlags(1048576);
            output.putFloat(this.crossSpeed);
        }

        if (this.table != null && !this.table.isEmpty()) {
            bits.addFlags(2097152);
            this.table.save(output);
        }

        if (this.modData != null && !this.modData.isEmpty()) {
            bits.addFlags(4194304);
            this.modData.save(output);
        }

        if (this.blockAllTheSquare) {
            bits.addFlags(8388608);
        }

        if (this.isThumpable) {
            bits.addFlags(16777216);
        }

        if (this.isHoppable) {
            bits.addFlags(33554432);
        }

        if (this.getLightSourceLife() != -1) {
            bits.addFlags(67108864);
            output.putInt(this.getLightSourceLife());
        }

        if (this.getLightSourceRadius() != -1) {
            bits.addFlags(134217728);
            output.putInt(this.getLightSourceRadius());
        }

        if (this.getLightSourceXOffset() != 0) {
            bits.addFlags(268435456);
            output.putInt(this.getLightSourceXOffset());
        }

        if (this.getLightSourceYOffset() != 0) {
            bits.addFlags(536870912);
            output.putInt(this.getLightSourceYOffset());
        }

        if (this.getLightSourceFuel() != null) {
            bits.addFlags(1073741824);
            output.putShort(WorldDictionary.getItemRegistryID(this.getLightSourceFuel()));
        }

        if (this.getLifeDelta() != 0.0F) {
            bits.addFlags(2147483648L);
            output.putFloat(this.getLifeDelta());
        }

        if (this.getLifeLeft() != -1.0F) {
            bits.addFlags(4294967296L);
            output.putFloat(this.getLifeLeft());
        }

        if (this.keyId != -1) {
            bits.addFlags(8589934592L);
            output.putInt(this.keyId);
        }

        if (this.isLockedByKey()) {
            bits.addFlags(17179869184L);
        }

        if (this.isLockedByPadlock()) {
            bits.addFlags(34359738368L);
        }

        if (this.canBeLockByPadlock()) {
            bits.addFlags(68719476736L);
        }

        if (this.getLockedByCode() != 0) {
            bits.addFlags(137438953472L);
            output.putInt(this.getLockedByCode());
        }

        if (!this.thumpSound.equals("ZombieThumbGeneric")) {
            bits.addFlags(274877906944L);
            GameWindow.WriteString(output, this.thumpSound);
        }

        if (this.lastUpdateHours != -1.0F) {
            bits.addFlags(549755813888L);
            output.putFloat(this.lastUpdateHours);
        }

        if (this.haveFuel) {
            bits.addFlags(1099511627776L);
        }

        if (this.lightSourceOn) {
            bits.addFlags(2199023255552L);
        }

        bits.write();
        bits.release();
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public boolean IsOpen() {
        return this.open;
    }

    public boolean IsStrengthenedByPushedItems() {
        return false;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    @Override
    public boolean TestPathfindCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        boolean north = this.north;
        if (obj instanceof IsoSurvivor isoSurvivor && isoSurvivor.getInventory().contains("Hammer")) {
            return false;
        } else if (this.open) {
            return false;
        } else {
            if (from == this.square) {
                if (north && to.getY() < from.getY()) {
                    return true;
                }

                if (!north && to.getX() < from.getX()) {
                    return true;
                }
            } else {
                if (north && to.getY() > from.getY()) {
                    return true;
                }

                if (!north && to.getX() > from.getX()) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        if (obj instanceof IsoPlayer isoPlayer && isoPlayer.isNoClip()) {
            return false;
        } else {
            boolean north = this.north;
            if (this.open) {
                return false;
            } else if (this.blockAllTheSquare) {
                if (from != this.square) {
                    if (obj != null) {
                        obj.collideWith(this);
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                if (from == this.square) {
                    if (north && to.getY() < from.getY()) {
                        if (obj != null) {
                            obj.collideWith(this);
                        }

                        if (!this.canPassThrough && !this.isStairs && !this.isCorner) {
                            return true;
                        }
                    }

                    if (!north && to.getX() < from.getX()) {
                        if (obj != null) {
                            obj.collideWith(this);
                        }

                        if (!this.canPassThrough && !this.isStairs && !this.isCorner) {
                            return true;
                        }
                    }
                } else {
                    if (north && to.getY() > from.getY()) {
                        if (obj != null) {
                            obj.collideWith(this);
                        }

                        if (!this.canPassThrough && !this.isStairs && !this.isCorner) {
                            return true;
                        }
                    }

                    if (!north && to.getX() > from.getX()) {
                        if (obj != null) {
                            obj.collideWith(this);
                        }

                        if (!this.canPassThrough && !this.isStairs && !this.isCorner) {
                            return true;
                        }
                    }
                }

                if (this.isCorner) {
                    if (to.getY() < from.getY() && to.getX() < from.getX()) {
                        if (obj != null) {
                            obj.collideWith(this);
                        }

                        if (!this.canPassThrough) {
                            return true;
                        }
                    }

                    if (to.getY() > from.getY() && to.getX() > from.getX()) {
                        if (obj != null) {
                            obj.collideWith(this);
                        }

                        if (!this.canPassThrough) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (this.canPassThrough) {
            return IsoObject.VisionResult.NoEffect;
        } else {
            boolean north = this.north;
            if (this.open) {
                north = !north;
            }

            if (to.getZ() != from.getZ()) {
                return IsoObject.VisionResult.NoEffect;
            } else {
                boolean doorTrans = this.sprite != null && this.sprite.getProperties().has("doorTrans");
                if (from == this.square) {
                    if (north && to.getY() < from.getY()) {
                        if (doorTrans) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        if (this.isWindow()) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        return IsoObject.VisionResult.Blocked;
                    }

                    if (!north && to.getX() < from.getX()) {
                        if (doorTrans) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        if (this.isWindow()) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        return IsoObject.VisionResult.Blocked;
                    }
                } else {
                    if (north && to.getY() > from.getY()) {
                        if (doorTrans) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        if (this.isWindow()) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        return IsoObject.VisionResult.Blocked;
                    }

                    if (!north && to.getX() > from.getX()) {
                        if (doorTrans) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        if (this.isWindow()) {
                            return IsoObject.VisionResult.Unblocked;
                        }

                        return IsoObject.VisionResult.Blocked;
                    }
                }

                return IsoObject.VisionResult.NoEffect;
            }
        }
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        if (SandboxOptions.instance.lore.thumpOnConstruction.getValue()) {
            if (thumper instanceof IsoGameCharacter isoGameCharacter) {
                Thumpable thumpable = this.getThumpableFor(isoGameCharacter);
                if (thumpable == null) {
                    return;
                }

                if (thumpable != this) {
                    thumpable.Thump(thumper);
                    return;
                }
            }

            boolean bBreakableFence = BrokenFences.getInstance().isBreakableObject(this);
            if (thumper instanceof IsoZombie isoZombie) {
                if (isoZombie.cognition == 1 && this.isDoor() && !this.IsOpen() && !this.isLocked()) {
                    this.ToggleDoor((IsoGameCharacter)thumper);
                    return;
                }

                if (GameClient.client) {
                    if (isoZombie.isLocal()) {
                        GameClient.sendZombieHitThumpable((IsoGameCharacter)thumper, this);
                    }

                    WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
                    if (this.isDoor()) {
                        this.setRenderEffect(RenderEffectType.Hit_Door, true);
                    }

                    return;
                }

                int totalThumpers = thumper.getSurroundingThumpers();
                int max = this.thumpDmg;
                if (totalThumpers >= max) {
                    int amount = 1 * ThumpState.getFastForwardDamageMultiplier();
                    this.setHealth(this.getHealth() - amount);
                } else {
                    this.partialThumpDmg = this.partialThumpDmg + (float)totalThumpers / max * ThumpState.getFastForwardDamageMultiplier();
                    if ((int)this.partialThumpDmg > 0) {
                        int amount = (int)this.partialThumpDmg;
                        this.setHealth(this.getHealth() - amount);
                        this.partialThumpDmg -= amount;
                    }
                }

                WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
                if (this.isDoor()) {
                    this.setRenderEffect(RenderEffectType.Hit_Door, true);
                }
            }

            if (this.health <= 0) {
                if (!bBreakableFence) {
                    ((IsoGameCharacter)thumper).getEmitter().playSound(this.breakSound, this);
                    if (GameServer.server) {
                        GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, this.breakSound, false, thumper.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                    }
                }

                WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
                thumper.setThumpTarget(null);
                if (IsoDoor.destroyDoubleDoor(this)) {
                    return;
                }

                if (IsoDoor.destroyGarageDoor(this)) {
                    return;
                }

                if (bBreakableFence) {
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
                    return;
                }

                this.destroy();
            }
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        if (this.isDoor() || this.isWindow()) {
            IsoBarricade barricade = this.getBarricadeForCharacter(chr);
            if (barricade != null) {
                return barricade;
            }

            barricade = this.getBarricadeOppositeCharacter(chr);
            if (barricade != null) {
                return barricade;
            }
        }

        boolean bThumpable = this.isThumpable;
        boolean bCrawlingZombie = chr instanceof IsoZombie && chr.isCrawling();
        if (!bThumpable && bCrawlingZombie && BrokenFences.getInstance().isBreakableObject(this)) {
            bThumpable = true;
        }

        if (!bThumpable && bCrawlingZombie && this.isHoppable()) {
            bThumpable = true;
        }

        if (bThumpable && !this.isDestroyed()) {
            if ((!this.isDoor() || !this.IsOpen()) && !this.isWindow()) {
                return !bCrawlingZombie && this.isHoppable() && !this.isTallHoppable() ? null : this;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public float getThumpCondition() {
        return (float)PZMath.clamp(this.health, 0, this.maxHealth) / this.maxHealth;
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (GameClient.client) {
            if (this.isDoor()) {
                this.setRenderEffect(RenderEffectType.Hit_Door, true);
            }
        } else {
            Thumpable thumpable = this.getThumpableFor(owner);
            if (thumpable != null) {
                if (thumpable instanceof IsoBarricade) {
                    thumpable.WeaponHit(owner, weapon);
                } else {
                    LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
                    this.Damage(weapon.getDoorDamage());
                    if (weapon.getDoorHitSound() != null) {
                        if (player != null) {
                            player.setMeleeHitSurface(this.getMeleeHitSurface());
                        }

                        owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
                        if (GameServer.server) {
                            GameServer.PlayWorldSoundServer(owner, weapon.getDoorHitSound(), false, this.getSquare(), 0.2F, 20.0F, 1.0F, false);
                        }
                    }

                    WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                    if (this.isDoor()) {
                        this.setRenderEffect(RenderEffectType.Hit_Door, true);
                    }

                    if (!this.IsStrengthenedByPushedItems() && this.health <= 0 || this.IsStrengthenedByPushedItems() && this.health <= -this.pushedMaxStrength
                        )
                     {
                        owner.getEmitter().playSound(this.breakSound, this);
                        WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                        if (GameClient.client) {
                            GameClient.instance
                                .sendClientCommandV(
                                    null,
                                    "object",
                                    "OnDestroyIsoThumpable",
                                    "x",
                                    this.getXi(),
                                    "y",
                                    this.getYi(),
                                    "z",
                                    this.getZi(),
                                    "index",
                                    this.getObjectIndex()
                                );
                        }

                        LuaEventManager.triggerEvent("OnDestroyIsoThumpable", this, null);
                        if (IsoDoor.destroyDoubleDoor(this)) {
                            return;
                        }

                        if (IsoDoor.destroyGarageDoor(this)) {
                            return;
                        }

                        this.destroyed = true;
                        if (this.getObjectIndex() != -1) {
                            this.square.transmitRemoveItemFromSquare(this);
                        }
                    }
                }
            }
        }
    }

    public IsoGridSquare getOtherSideOfDoor(IsoGameCharacter chr) {
        if (this.north) {
            return chr.getCurrentSquare().getRoom() == this.square.getRoom()
                ? IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ())
                : IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
        } else {
            return chr.getCurrentSquare().getRoom() == this.square.getRoom()
                ? IsoWorld.instance.currentCell.getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ())
                : IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
        }
    }

    public void changeSprite(IsoThumpable thumpable) {
        thumpable.sprite = thumpable.open ? thumpable.openSprite : thumpable.closedSprite;
    }

    public boolean couldBeOpen(IsoGameCharacter chr) {
        if (chr instanceof IsoAnimal) {
            return false;
        } else if (this.isBarricaded()) {
            return false;
        } else if (this.isLockedByKey()
            && chr instanceof IsoPlayer
            && chr.getCurrentSquare().has(IsoFlagType.exterior)
            && chr.getInventory().haveThisKeyId(this.getKeyId()) == null) {
            return false;
        } else {
            return this.isLocked() && chr instanceof IsoPlayer && chr.getCurrentSquare().has(IsoFlagType.exterior) && !this.open ? false : !this.isObstructed();
        }
    }

    public void ToggleDoorActual(IsoGameCharacter chr) {
        if (!(chr instanceof IsoAnimal)) {
            this.lastPlayerOnlineId = chr != null ? chr.getOnlineID() : -1;
            this.wasTryingToggleBarricadedDoor = false;
            this.wasTryingToggleLockedDoor = false;
            if (this.isBarricaded()) {
                if (chr != null) {
                    this.TriggerBarricadedDoor(chr);
                }

                this.wasTryingToggleBarricadedDoor = true;
                this.syncIsoObject(false, (byte)0, null, null);
            } else if (this.isLockedByKey()
                && chr instanceof IsoPlayer
                && chr.getCurrentSquare().has(IsoFlagType.exterior)
                && chr.getInventory().haveThisKeyId(this.getKeyId()) == null) {
                this.TriggerLockedDoor(chr);
                this.wasTryingToggleLockedDoor = true;
                this.syncIsoObject(false, (byte)0, null, null);
            } else {
                if (this.isLockedByKey() && chr instanceof IsoPlayer && chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
                    this.playDoorSound(chr.getEmitter(), "Unlock");
                    this.setIsLocked(false);
                    this.setLockedByKey(false);
                }

                this.DirtySlice();
                this.square.InvalidateSpecialObjectPaths();
                if (this.locked && chr instanceof IsoPlayer && chr.getCurrentSquare().has(IsoFlagType.exterior) && !this.open) {
                    this.playDoorSound(chr.getEmitter(), "Locked");
                    this.setRenderEffect(RenderEffectType.Hit_Door, true);
                } else {
                    if (chr instanceof IsoPlayer) {
                    }

                    for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                        LosUtil.cachecleared[pn] = true;
                    }

                    IsoGridSquare.setRecalcLightTime(-1.0F);
                    GameTime.instance.lightSourceUpdate = 100.0F;
                    if (this.getSprite().getProperties().has("DoubleDoor")) {
                        if (IsoDoor.isDoubleDoorObstructed(this)) {
                            if (chr != null) {
                                this.playDoorSound(chr.getEmitter(), "Blocked");
                                chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                            }
                        } else {
                            boolean wasOpen = this.open;
                            IsoDoor.toggleDoubleDoor(this, true);
                            if (wasOpen != this.open) {
                                this.playDoorSound(chr.getEmitter(), this.open ? "Open" : "Close");
                            }
                        }
                    } else if (this.isObstructed()) {
                        if (chr != null) {
                            this.playDoorSound(chr.getEmitter(), "Blocked");
                            chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                        }
                    } else {
                        this.sprite = this.closedSprite;
                        this.open = !this.open;
                        this.setLockedByKey(false);
                        if (this.open) {
                            this.playDoorSound(chr.getEmitter(), "Open");
                            this.sprite = this.openSprite;
                        } else {
                            this.playDoorSound(chr.getEmitter(), "Close");
                        }

                        this.square.RecalcAllWithNeighbours(true);
                        this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null, null);
                        PolygonalMap2.instance.squareChanged(this.square);
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                        this.invalidateRenderChunkLevel(256L);
                        this.PlayAnimation();
                    }
                }
            }
        }
    }

    private void TriggerLockedDoor(IsoGameCharacter chr) {
        this.playDoorSound(chr.getEmitter(), "Locked");
        this.setRenderEffect(RenderEffectType.Hit_Door, true);
    }

    private void TriggerBarricadedDoor(IsoGameCharacter chr) {
        this.playDoorSound(chr.getEmitter(), "Blocked");
        if (chr instanceof IsoPlayer player && player.isLocalPlayer()) {
            chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBarricaded"), 255, 255, 255, 256.0F);
        }

        this.setRenderEffect(RenderEffectType.Hit_Door, true);
    }

    private void PlayAnimation() {
        SpriteModel spriteModel1 = this.getSpriteModel();
        if (spriteModel1 != null && spriteModel1.animationName != null) {
            IsoObjectAnimations.getInstance().addObject(this, spriteModel1, this.open ? "Open" : "Close");
        }

        this.setAnimating(true);
    }

    public void ToggleDoor(IsoGameCharacter chr) {
        this.ToggleDoorActual(chr);
    }

    public void ToggleDoorSilent() {
        if (!this.isBarricaded()) {
            this.square.InvalidateSpecialObjectPaths();

            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                LosUtil.cachecleared[pn] = true;
            }

            IsoGridSquare.setRecalcLightTime(-1.0F);
            this.open = !this.open;
            this.sprite = this.closedSprite;
            if (this.open) {
                this.sprite = this.openSprite;
            }
        }
    }

    public boolean isObstructed() {
        return IsoDoor.isDoorObstructed(this);
    }

    @Override
    public boolean haveSheetRope() {
        return !this.canAddSheetRope();
    }

    @Override
    public int countAddSheetRope() {
        return (this.isHoppable() || this.isWindow()) && this.canAddSheetRope() ? IsoWindow.countAddSheetRope(this.square, this.north) : 0;
    }

    @Override
    public boolean canAddSheetRope() {
        if (!this.isHoppable() && !this.isWindow()) {
            return false;
        } else {
            return this.north
                ? !this.square.has(IsoFlagType.climbSheetTopN) && !this.square.has(IsoFlagType.climbSheetTopS)
                : !this.square.has(IsoFlagType.climbSheetTopW) && !this.square.has(IsoFlagType.climbSheetTopE);
        }
    }

    @Override
    public boolean addSheetRope(IsoPlayer player, String itemType) {
        return !this.canAddSheetRope() ? false : IsoWindow.addSheetRope(player, this.square, this.north, itemType);
    }

    @Override
    public boolean removeSheetRope(IsoPlayer player) {
        return this.haveSheetRope() ? IsoWindow.removeSheetRope(player, this.square, this.north) : false;
    }

    public void createLightSource(
        int radius, int offsetX, int offsetY, int offsetZ, int life, String lightSourceFuel, InventoryItem baseItem, IsoGameCharacter chr
    ) {
        this.setLightSourceXOffset(offsetX);
        this.setLightSourceYOffset(offsetY);
        this.setLightSourceRadius(radius);
        this.setLightSourceFuel(lightSourceFuel);
        if (baseItem != null) {
            if (baseItem instanceof DrainableComboItem drainableComboItem) {
                this.setLifeLeft(baseItem.getCurrentUsesFloat());
                this.setLifeDelta(drainableComboItem.getUseDelta());
                this.setHaveFuel(!"Base.HandTorch".equals(baseItem.getFullType()) || baseItem.getCurrentUses() > 0);
            } else {
                this.setLifeLeft(1.0F);
                this.setHaveFuel(true);
            }
        }
    }

    public InventoryItem insertNewFuel(InventoryItem item, IsoGameCharacter chr) {
        if (item != null) {
            InventoryItem previousFuel = this.removeCurrentFuel(chr);
            if (chr != null) {
                chr.removeFromHands(item);
                chr.getInventory().Remove(item);
            }

            if (item instanceof DrainableComboItem drainableComboItem) {
                this.setLifeLeft(item.getCurrentUsesFloat());
                this.setLifeDelta(drainableComboItem.getUseDelta());
            } else {
                this.setLifeLeft(1.0F);
            }

            this.setHaveFuel(true);
            this.toggleLightSource(true);
            return previousFuel;
        } else {
            return null;
        }
    }

    public InventoryItem removeCurrentFuel(IsoGameCharacter chr) {
        if (this.haveFuel()) {
            InventoryItem previousFuel = InventoryItemFactory.CreateItem(this.getLightSourceFuel());
            if (previousFuel instanceof DrainableComboItem) {
                previousFuel.setCurrentUses((int)(previousFuel.getMaxUses() * this.getLifeLeft()));
            }

            if (chr != null) {
                chr.getInventory().AddItem(previousFuel);
            }

            this.setLifeLeft(0.0F);
            this.setLifeDelta(-1.0F);
            this.toggleLightSource(false);
            this.setHaveFuel(false);
            return previousFuel;
        } else {
            return null;
        }
    }

    private int calcLightSourceX() {
        int lx = (int)this.getX();
        int ly = (int)this.getY();
        if (this.lightSourceXOffset != 0) {
            for (int i = 1; i <= Math.abs(this.lightSourceXOffset); i++) {
                int dx = this.lightSourceXOffset > 0 ? 1 : -1;
                LosUtil.TestResults test = LosUtil.lineClear(this.getCell(), this.getXi(), this.getYi(), this.getZi(), lx + dx, ly, this.getZi(), false);
                if (test == LosUtil.TestResults.Blocked || test == LosUtil.TestResults.ClearThroughWindow) {
                    break;
                }

                lx += dx;
            }
        }

        return lx;
    }

    private int calcLightSourceY() {
        int lx = (int)this.getX();
        int ly = (int)this.getY();
        if (this.lightSourceYOffset != 0) {
            for (int i = 1; i <= Math.abs(this.lightSourceYOffset); i++) {
                int dy = this.lightSourceYOffset > 0 ? 1 : -1;
                LosUtil.TestResults test = LosUtil.lineClear(this.getCell(), this.getXi(), this.getYi(), this.getZi(), lx, ly + dy, this.getZi(), false);
                if (test == LosUtil.TestResults.Blocked || test == LosUtil.TestResults.ClearThroughWindow) {
                    break;
                }

                ly += dy;
            }
        }

        return ly;
    }

    @Override
    public void update() {
        if (this.getObjectIndex() != -1) {
            if (!GameServer.server) {
                if (this.lightSource != null && !this.lightSource.isInBounds()) {
                    this.lightSource = null;
                }

                if (this.lightSourceFuel != null && !this.lightSourceFuel.isEmpty() && this.lightSource == null && this.square != null) {
                    int offsetZ = 0;
                    int lx = this.calcLightSourceX();
                    int ly = this.calcLightSourceY();
                    if (IsoWorld.instance.currentCell.isInChunkMap(lx, ly)) {
                        int life = this.getLightSourceLife();
                        this.setLightSource(new IsoLightSource(lx, ly, this.getZi() + 0, 1.0F, 1.0F, 1.0F, this.lightSourceRadius, life > 0 ? life : -1));
                        this.lightSource.setActive(this.isLightSourceOn());
                        IsoWorld.instance.getCell().getLamppostPositions().add(this.getLightSource());
                    }
                }

                if (this.lightSource != null && this.lightSource.isActive()) {
                    int offsetZ = 0;
                    int lx = this.calcLightSourceX();
                    int ly = this.calcLightSourceY();
                    if (lx != this.lightSource.x || ly != this.lightSource.y) {
                        this.getCell().removeLamppost(this.lightSource);
                        int life = this.getLightSourceLife();
                        this.setLightSource(new IsoLightSource(lx, ly, this.getZi() + 0, 1.0F, 1.0F, 1.0F, this.lightSourceRadius, life > 0 ? life : -1));
                        this.lightSource.setActive(this.isLightSourceOn());
                        IsoWorld.instance.getCell().getLamppostPositions().add(this.getLightSource());
                    }
                }
            }

            if (this.getLifeLeft() > -1.0F) {
                if (this.isLightSourceOn()) {
                    int worldAgeMinutes = (int)(GameTime.getInstance().getWorldAgeHours() * 60.0);
                    if (Math.abs(worldAgeMinutes - this.lastUpdateHours) > 10.0F) {
                        if (this.lastUpdateHours > 1.0F) {
                            this.setLifeLeft(this.getLifeLeft() - this.getLifeDelta() * (Math.abs(worldAgeMinutes - this.lastUpdateHours) / 10.0F));
                        }

                        this.lastUpdateHours = worldAgeMinutes;
                        if (this.getLifeLeft() <= 0.0F) {
                            this.setLifeLeft(0.0F);
                            this.toggleLightSource(false);
                        }
                    }
                } else {
                    this.updateAccumulator = 0.0F;
                    this.lastUpdateHours = -1.0F;
                }
            }
        }
    }

    @Override
    public void Damage(float amount) {
        if (this.isThumpable()) {
            this.DirtySlice();
            this.health = (int)(this.health - amount);
            if (GameServer.server) {
                this.sync();
            }
        }
    }

    public void destroy() {
        if (!this.destroyed) {
            if (this.getObjectIndex() != -1) {
                this.dumpContentsInSquare();
                if (GameClient.client) {
                    GameClient.instance
                        .sendClientCommandV(
                            null,
                            "object",
                            "OnDestroyIsoThumpable",
                            "x",
                            this.square.getX(),
                            "y",
                            this.square.getY(),
                            "z",
                            this.square.getZ(),
                            "index",
                            this.getObjectIndex()
                        );
                }

                LuaEventManager.triggerEvent("OnDestroyIsoThumpable", this, null);
                this.health = 0;
                this.destroyed = true;
                if (this.getObjectIndex() != -1) {
                    this.square.transmitRemoveItemFromSquare(this);
                }
            }
        }
    }

    @Override
    public IsoBarricade getBarricadeOnSameSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.square, this.north ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public IsoBarricade getBarricadeOnOppositeSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.north ? IsoDirections.S : IsoDirections.E);
    }

    @Override
    public boolean isBarricaded() {
        IsoBarricade barricade = this.getBarricadeOnSameSquare();
        if (barricade == null) {
            barricade = this.getBarricadeOnOppositeSquare();
        }

        return barricade != null;
    }

    @Override
    public boolean isBarricadeAllowed() {
        return this.canBarricade;
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    public void setIsDoor(Boolean pIsDoor) {
        this.isDoor = pIsDoor;
    }

    /**
     * @return the table
     */
    @Override
    public KahluaTable getTable() {
        return this.table;
    }

    /**
     * 
     * @param table the table to set
     */
    @Override
    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public boolean canBePlastered() {
        return this.canBePlastered;
    }

    public void setCanBePlastered(boolean canBePlastered) {
        this.canBePlastered = canBePlastered;
    }

    public boolean isPaintable() {
        return this.paintable;
    }

    public void setPaintable(boolean paintable) {
        this.paintable = paintable;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setIsLocked(boolean lock) {
        this.locked = lock;
    }

    public boolean isThumpable() {
        return this.isBarricaded() ? true : this.isThumpable;
    }

    public void setIsThumpable(boolean thumpable) {
        this.isThumpable = thumpable;
    }

    public void setIsHoppable(boolean isHoppable) {
        this.setHoppable(isHoppable);
    }

    public IsoSprite getOpenSprite() {
        return this.openSprite;
    }

    @Override
    public boolean isHoppable() {
        if (this.isDoor() && !this.IsOpen() && this.closedSprite != null) {
            PropertyContainer props = this.closedSprite.getProperties();
            return props.has(IsoFlagType.HoppableN) || props.has(IsoFlagType.HoppableW);
        } else {
            return this.sprite == null || !this.sprite.getProperties().has(IsoFlagType.HoppableN) && !this.sprite.getProperties().has(IsoFlagType.HoppableW)
                ? this.isHoppable
                : true;
        }
    }

    @Override
    public boolean isTallHoppable() {
        if (this.isDoor() && !this.IsOpen() && this.closedSprite != null) {
            PropertyContainer props = this.closedSprite.getProperties();
            return props.has(IsoFlagType.TallHoppableN) || props.has(IsoFlagType.TallHoppableW);
        } else {
            return this.sprite != null
                && (this.sprite.getProperties().has(IsoFlagType.TallHoppableN) || this.sprite.getProperties().has(IsoFlagType.TallHoppableW));
        }
    }

    public void setHoppable(boolean isHoppable) {
        this.isHoppable = isHoppable;
    }

    public int getLightSourceRadius() {
        return this.lightSourceRadius;
    }

    public void setLightSourceRadius(int lightSourceRadius) {
        this.lightSourceRadius = lightSourceRadius;
    }

    public int getLightSourceXOffset() {
        return this.lightSourceXOffset;
    }

    public void setLightSourceXOffset(int lightSourceXOffset) {
        this.lightSourceXOffset = lightSourceXOffset;
    }

    public int getLightSourceYOffset() {
        return this.lightSourceYOffset;
    }

    public void setLightSourceYOffset(int lightSourceYOffset) {
        this.lightSourceYOffset = lightSourceYOffset;
    }

    public int getLightSourceLife() {
        return this.lightSourceLife;
    }

    public void setLightSourceLife(int lightSourceLife) {
        this.lightSourceLife = lightSourceLife;
    }

    public boolean isLightSourceOn() {
        return this.lightSourceOn;
    }

    public void setLightSourceOn(boolean lightSourceOn) {
        this.lightSourceOn = lightSourceOn;
    }

    @Override
    public IsoLightSource getLightSource() {
        return this.lightSource;
    }

    @Override
    public void setLightSource(IsoLightSource lightSource) {
        this.lightSource = lightSource;
    }

    public void toggleLightSource(boolean toggle) {
        this.setLightSourceOn(toggle);
        if (this.lightSource != null) {
            this.getLightSource().setActive(toggle);
            IsoGridSquare.setRecalcLightTime(-1.0F);
            GameTime.instance.lightSourceUpdate = 100.0F;
        }
    }

    public String getLightSourceFuel() {
        return this.lightSourceFuel;
    }

    public void setLightSourceFuel(String lightSourceFuel) {
        if (lightSourceFuel != null && lightSourceFuel.isEmpty()) {
            lightSourceFuel = null;
        }

        this.lightSourceFuel = lightSourceFuel;
    }

    public float getLifeLeft() {
        return this.lifeLeft;
    }

    public void setLifeLeft(float lifeLeft) {
        this.lifeLeft = lifeLeft;
    }

    public float getLifeDelta() {
        return this.lifeDelta;
    }

    public void setLifeDelta(float lifeDelta) {
        this.lifeDelta = lifeDelta;
    }

    public boolean haveFuel() {
        return this.haveFuel;
    }

    public void setHaveFuel(boolean haveFuel) {
        this.haveFuel = haveFuel;
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        super.syncIsoObjectSend(b);
        b.putBoolean(this.open);
        b.putBoolean(this.locked);
        b.putBoolean(this.lockedByKey);
        b.putBoolean(this.lockedByPadlock);
        b.putInt(this.keyId);
        b.putInt(this.health);
        if (GameClient.client) {
            b.putShort(IsoPlayer.getInstance().getOnlineID());
        } else {
            b.putShort(this.lastPlayerOnlineId);
        }

        b.putBoolean(this.wasTryingToggleBarricadedDoor);
        b.putBoolean(this.wasTryingToggleLockedDoor);
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        super.syncIsoObjectReceive(bb);
        this.square.InvalidateSpecialObjectPaths();

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            LosUtil.cachecleared[pn] = true;
        }

        IsoGridSquare.setRecalcLightTime(-1.0F);
        GameTime.instance.lightSourceUpdate = 100.0F;
        boolean bOpen = bb.get() == 1;
        boolean locked = bb.get() == 1;
        boolean wasLocked = this.locked && !locked;
        this.locked = locked;
        this.lockedByKey = bb.get() == 1;
        this.lockedByPadlock = bb.get() == 1;
        this.keyId = bb.getInt();
        this.health = bb.getInt();
        this.lastPlayerOnlineId = bb.getShort();
        this.wasTryingToggleBarricadedDoor = bb.get() == 1;
        this.wasTryingToggleLockedDoor = bb.get() == 1;
        IsoPlayer player = null;
        if (GameClient.client && this.lastPlayerOnlineId != -1) {
            player = GameClient.IDToPlayerMap.get(this.lastPlayerOnlineId);
            if (player != null) {
                player.networkAi.setNoCollision(1000L);
            }
        }

        if (wasLocked && player != null) {
            this.playDoorSound(player.getEmitter(), "Unlock");
        }

        if (IsoDoor.getDoubleDoorIndex(this) != -1) {
            if (bOpen != this.open) {
                IsoDoor.toggleDoubleDoor(this, false);
            }
        } else if (bOpen != this.open) {
            if (player != null) {
                this.playDoorSound(player.getEmitter(), this.open ? "Open" : "Close");
            }

            this.open = bOpen;
            this.sprite = this.open ? this.openSprite : this.closedSprite;
            this.PlayAnimation();
        } else {
            if (this.wasTryingToggleBarricadedDoor) {
                if (player != null) {
                    this.TriggerBarricadedDoor(player);
                }

                return;
            }

            if (this.wasTryingToggleLockedDoor) {
                if (player != null) {
                    this.TriggerLockedDoor(player);
                }

                return;
            }
        }

        this.square.RecalcProperties();
        PolygonalMap2.instance.squareChanged(this.square);
        LuaEventManager.triggerEvent("OnContainerUpdate");
        this.invalidateRenderChunkLevel(256L);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public void removeFromWorld() {
        if (this.lightSource != null) {
            IsoWorld.instance.currentCell.removeLamppost(this.lightSource);
        }

        super.removeFromWorld();
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        super.saveChange(change, tbl, bb);
        if ("lightSource".equals(change)) {
            bb.put((byte)(this.lightSourceOn ? 1 : 0));
            bb.put((byte)(this.haveFuel ? 1 : 0));
            bb.putFloat(this.lifeLeft);
            bb.putFloat(this.lifeDelta);
        } else if ("paintable".equals(change)) {
            bb.put((byte)(this.isPaintable() ? 1 : 0));
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        super.loadChange(change, bb);
        if ("lightSource".equals(change)) {
            boolean on = bb.get() == 1;
            this.haveFuel = bb.get() == 1;
            this.lifeLeft = bb.getFloat();
            this.lifeDelta = bb.getFloat();
            if (on != this.lightSourceOn) {
                this.toggleLightSource(on);
            }
        } else if ("paintable".equals(change)) {
            this.setPaintable(bb.get() == 1);
        }
    }

    public IsoCurtain HasCurtains() {
        IsoGridSquare toCheck = this.getOppositeSquare();
        if (toCheck != null) {
            IsoCurtain curtain = toCheck.getCurtain(this.getNorth() ? IsoObjectType.curtainS : IsoObjectType.curtainE);
            if (curtain != null) {
                return curtain;
            }
        }

        return this.getSquare().getCurtain(this.getNorth() ? IsoObjectType.curtainN : IsoObjectType.curtainW);
    }

    @Override
    public boolean canAddCurtain() {
        return false;
    }

    public IsoGridSquare getInsideSquare() {
        if (this.square == null) {
            return null;
        } else {
            return this.north
                ? this.square.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ())
                : this.square.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
        }
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        return this.getInsideSquare();
    }

    public boolean isAdjacentToSquare(IsoGridSquare square2) {
        IsoGridSquare square1 = this.getSquare();
        if (square1 != null && square2 != null) {
            boolean bClosed = !this.IsOpen();
            IsoGridSquare nw = square1.getAdjacentSquare(IsoDirections.NW);
            IsoGridSquare n = square1.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare ne = square1.getAdjacentSquare(IsoDirections.NE);
            IsoGridSquare w = square1.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare e = square1.getAdjacentSquare(IsoDirections.E);
            IsoGridSquare sw = square1.getAdjacentSquare(IsoDirections.SW);
            IsoGridSquare s = square1.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare se = square1.getAdjacentSquare(IsoDirections.SE);
            switch (this.getSpriteEdge(false)) {
                case N:
                    if (square2 == nw) {
                        if (!nw.isWallTo(n) && !nw.isWindowTo(n) && !nw.hasDoorOnEdge(IsoDirections.E, false) && !n.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == n) {
                        if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                            return false;
                        }

                        if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.N)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == ne) {
                        if (!ne.isWallTo(n) && !ne.isWindowTo(n) && !ne.hasDoorOnEdge(IsoDirections.W, false) && !n.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == w) {
                        if (!w.isWallTo(square1)
                            && !w.isWindowTo(square1)
                            && !w.hasDoorOnEdge(IsoDirections.E, false)
                            && !square1.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.N)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == e) {
                        if (!e.isWallTo(square1)
                            && !e.isWindowTo(square1)
                            && !e.hasDoorOnEdge(IsoDirections.W, false)
                            && !square1.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                case S:
                    if (square2 == w) {
                        if (!w.isWallTo(square1)
                            && !w.isWindowTo(square1)
                            && !w.hasDoorOnEdge(IsoDirections.E, false)
                            && !square1.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.S)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.S)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == e) {
                        if (!e.isWallTo(square1)
                            && !e.isWindowTo(square1)
                            && !e.hasDoorOnEdge(IsoDirections.W, false)
                            && !square1.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.S)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == sw) {
                        if (!sw.isWallTo(s) && !sw.isWindowTo(s) && !sw.hasDoorOnEdge(IsoDirections.E, false) && !s.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (s.hasDoorOnEdge(IsoDirections.N, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == s) {
                        if (s.hasDoorOnEdge(IsoDirections.N, false)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == se) {
                        if (!se.isWallTo(s) && !se.isWindowTo(s) && !se.hasDoorOnEdge(IsoDirections.W, false) && !s.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (s.hasDoorOnEdge(IsoDirections.N, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                case W:
                    if (square2 == nw) {
                        if (!nw.isWallTo(w) && !nw.isWindowTo(w) && !nw.hasDoorOnEdge(IsoDirections.S, false) && !w.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == w) {
                        if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                            return false;
                        }

                        if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.W)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == sw) {
                        if (!sw.isWallTo(w) && !sw.isWindowTo(w) && !sw.hasDoorOnEdge(IsoDirections.N, false) && !w.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == n) {
                        if (!n.isWallTo(square1)
                            && !n.isWindowTo(square1)
                            && !n.hasDoorOnEdge(IsoDirections.S, false)
                            && !square1.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.W)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == s) {
                        if (!s.isWallTo(square1)
                            && !s.isWindowTo(square1)
                            && !s.hasDoorOnEdge(IsoDirections.N, false)
                            && !square1.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                case E:
                    if (square2 == n) {
                        if (!n.isWallTo(square1)
                            && !n.isWindowTo(square1)
                            && !n.hasDoorOnEdge(IsoDirections.S, false)
                            && !square1.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.E)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.E)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == s) {
                        if (!s.isWallTo(square1)
                            && !s.isWindowTo(square1)
                            && !s.hasDoorOnEdge(IsoDirections.N, false)
                            && !square1.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.E)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == ne) {
                        if (!ne.isWallTo(e) && !ne.isWindowTo(e) && !ne.hasDoorOnEdge(IsoDirections.S, false) && !w.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (e.hasDoorOnEdge(IsoDirections.W, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == e) {
                        if (e.hasDoorOnEdge(IsoDirections.W, false)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == se) {
                        if (!se.isWallTo(e) && !se.isWindowTo(e) && !se.hasDoorOnEdge(IsoDirections.N, false) && !e.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (e.hasDoorOnEdge(IsoDirections.E, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                default:
                    return false;
            }

            return false;
        } else {
            return false;
        }
    }

    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        if (chr != null && chr.getCurrentSquare() != null) {
            IsoGridSquare sqChr = chr.getCurrentSquare();
            IsoGridSquare sqThis = this.getSquare();
            if (this.north) {
                return sqChr.getY() < sqThis.getY() ? this.getCell().getGridSquare(sqThis.x, sqThis.y - 1, sqThis.z) : sqThis;
            } else {
                return sqChr.getX() < sqThis.getX() ? this.getCell().getGridSquare(sqThis.x - 1, sqThis.y, sqThis.z) : sqThis;
            }
        } else {
            return null;
        }
    }

    public void addSheet(IsoGameCharacter chr) {
        IsoGridSquare sq = this.getIndoorSquare();
        IsoObjectType curtainType;
        if (this.north) {
            curtainType = IsoObjectType.curtainN;
            if (sq != this.square) {
                curtainType = IsoObjectType.curtainS;
            }
        } else {
            curtainType = IsoObjectType.curtainW;
            if (sq != this.square) {
                curtainType = IsoObjectType.curtainE;
            }
        }

        if (chr != null) {
            if (this.north) {
                if (chr.getY() < this.getY()) {
                    sq = this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ());
                    curtainType = IsoObjectType.curtainS;
                } else {
                    sq = this.getSquare();
                    curtainType = IsoObjectType.curtainN;
                }
            } else if (chr.getX() < this.getX()) {
                sq = this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
                curtainType = IsoObjectType.curtainE;
            } else {
                sq = this.getSquare();
                curtainType = IsoObjectType.curtainW;
            }
        }

        if (sq != null) {
            if (sq.getCurtain(curtainType) == null) {
                if (sq != null) {
                    int gid = 16;
                    if (curtainType == IsoObjectType.curtainE) {
                        gid++;
                    }

                    if (curtainType == IsoObjectType.curtainS) {
                        gid += 3;
                    }

                    if (curtainType == IsoObjectType.curtainN) {
                        gid += 2;
                    }

                    gid += 4;
                    IsoCurtain c = new IsoCurtain(this.getCell(), sq, "fixtures_windows_curtains_01_" + gid, this.north);
                    sq.AddSpecialTileObject(c);
                    if (!GameClient.client) {
                        InventoryItem item = chr.getInventory().FindAndReturn("Sheet");
                        chr.getInventory().Remove(item);
                        if (GameServer.server) {
                            GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
                        }
                    }

                    if (GameServer.server) {
                        c.transmitCompleteItemToClients();
                    }
                }
            }
        }
    }

    public IsoGridSquare getIndoorSquare() {
        if (this.square.getRoom() != null) {
            return this.square;
        } else {
            IsoGridSquare sq;
            if (this.north) {
                sq = IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ());
            } else {
                sq = IsoWorld.instance.currentCell.getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
            }

            if (sq == null || sq.getFloor() == null) {
                return this.square;
            } else if (sq.getRoom() != null) {
                return sq;
            } else if (this.square.getFloor() == null) {
                return sq;
            } else {
                String floorSprite = sq.getFloor().getSprite().getName();
                return floorSprite != null && floorSprite.startsWith("carpentry_02_") ? sq : this.square;
            }
        }
    }

    @Override
    public int getKeyId() {
        return this.keyId;
    }

    public void setKeyId(int keyId, boolean doNetwork) {
        if (doNetwork && this.keyId != keyId) {
            this.keyId = keyId;
            this.syncIsoThumpable();
        }
    }

    @Override
    public void setKeyId(int keyId) {
        this.setKeyId(keyId, true);
    }

    @Override
    public boolean isLockedByKey() {
        return this.lockedByKey;
    }

    @Override
    public void setLockedByKey(boolean lockedByKey) {
        boolean changed = lockedByKey != this.lockedByKey;
        this.lockedByKey = lockedByKey;
        this.setIsLocked(lockedByKey);
        if (!GameServer.server && changed) {
            if (lockedByKey) {
                this.syncIsoObject(false, (byte)3, null, null);
            } else {
                this.syncIsoObject(false, (byte)4, null, null);
            }
        }
    }

    public boolean isLockedByPadlock() {
        return this.lockedByPadlock;
    }

    public void syncIsoThumpable() {
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncThumpable, this.square.getX(), this.square.getY(), this);
        }

        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.SyncThumpable, this);
        }
    }

    public void setLockedByPadlock(boolean lockedByPadlock) {
        if (this.lockedByPadlock != lockedByPadlock) {
            this.lockedByPadlock = lockedByPadlock;
            this.syncIsoThumpable();
        }
    }

    public boolean canBeLockByPadlock() {
        return this.canBeLockByPadlock;
    }

    public void setCanBeLockByPadlock(boolean canBeLockByPadlock) {
        this.canBeLockByPadlock = canBeLockByPadlock;
    }

    public int getLockedByCode() {
        return this.lockedByCode;
    }

    public void setLockedByCode(int lockedByCode) {
        if (this.lockedByCode != lockedByCode) {
            this.lockedByCode = lockedByCode;
            this.syncIsoThumpable();
        }
    }

    public boolean isLockedToCharacter(IsoGameCharacter chr) {
        if (GameClient.client && chr instanceof IsoPlayer isoPlayer && isoPlayer.role.hasCapability(Capability.CanOpenLockedDoors)) {
            return false;
        } else {
            return this.getLockedByCode() > 0
                ? true
                : this.isLockedByPadlock() && (chr.getInventory() == null || chr.getInventory().haveThisKeyId(this.getKeyId()) == null);
        }
    }

    @Override
    public boolean canClimbOver(IsoGameCharacter chr) {
        if (this.square == null) {
            return false;
        } else {
            return !this.isHoppable() ? false : chr == null || IsoWindow.canClimbThroughHelper(chr, this.getSquare(), this.getOppositeSquare(), this.north);
        }
    }

    public boolean canClimbThrough(IsoGameCharacter chr) {
        if (this.square == null) {
            return false;
        } else if (!this.isWindow()) {
            return false;
        } else {
            return this.isBarricaded() ? false : chr == null || IsoWindow.canClimbThroughHelper(chr, this.getSquare(), this.getOppositeSquare(), this.north);
        }
    }

    public String getThumpSound() {
        if (this.sprite != null && this.sprite.getProperties().has("ThumpSound")) {
            String soundName = this.sprite.getProperties().get("ThumpSound");
            if (!StringUtils.isNullOrWhitespace(soundName)) {
                return soundName;
            }
        }

        if (this.isDoor) {
            String var3 = this.getSoundPrefix();
            switch (var3) {
                case "MetalGate":
                    return "ZombieThumpChainlinkFence";
                case "MetalPoleGate":
                case "MetalPoleGateDouble":
                    return "ZombieThumpMetalPoleGate";
                case "GarageDoor":
                    return "ZombieThumpGarageDoor";
                case "MetalDoor":
                case "PrisonMetalDoor":
                    return "ZombieThumpMetal";
                case "SlidingGlassDoor":
                    return "ZombieThumpWindow";
            }
        }

        return this.thumpSound;
    }

    public void setThumpSound(String thumpSound) {
        this.thumpSound = thumpSound;
    }

    @Override
    public IsoObject getRenderEffectMaster() {
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            IsoObject first = null;
            if (ddIndex == 2) {
                first = IsoDoor.getDoubleDoorObject(this, 1);
            } else if (ddIndex == 3) {
                first = IsoDoor.getDoubleDoorObject(this, 4);
            }

            if (first != null) {
                return first;
            }
        } else {
            IsoObject firstx = IsoDoor.getGarageDoorFirst(this);
            if (firstx != null) {
                return firstx;
            }
        }

        return this;
    }

    public IsoDirections getSpriteEdge(boolean ignoreOpen) {
        if (!this.isDoor() && !this.isWindow()) {
            return null;
        } else if (this.open && !ignoreOpen) {
            PropertyContainer properties = this.getProperties();
            if (properties != null && properties.has(IsoFlagType.attachedE)) {
                return IsoDirections.E;
            } else if (properties != null && properties.has(IsoFlagType.attachedS)) {
                return IsoDirections.S;
            } else {
                return this.north ? IsoDirections.W : IsoDirections.N;
            }
        } else {
            return this.north ? IsoDirections.N : IsoDirections.W;
        }
    }

    public String getSoundPrefix() {
        if (this.closedSprite == null) {
            return "WoodDoor";
        } else {
            PropertyContainer props = this.closedSprite.getProperties();
            return props.has("DoorSound") ? props.get("DoorSound") : "WoodDoor";
        }
    }

    private void playDoorSound(BaseCharacterSoundEmitter emitter, String suffix) {
        emitter.playSoundImpl(this.getSoundPrefix() + suffix, this);
    }

    private String getMeleeHitSurface() {
        if (!this.isDoor() && this.sprite != null && this.sprite.getProperties().has("ThumpSound")) {
            String soundName = this.sprite.getProperties().get("ThumpSound");
            if (!StringUtils.isNullOrWhitespace(soundName)) {
                return switch (soundName) {
                    case "ZombieThumpGarageDoor" -> "GarageDoor";
                    case "ZombieThumpGeneric" -> "Default";
                    case "ZombieThumpMetalPoleGate" -> "MetalGate";
                    case "ZombieThumpMetal" -> "Metal";
                    case "ZombieThumpChainlinkFence" -> "MetalGate";
                    case "ZombieThumpWindow", "ZombieThumpWindowExtra" -> "Glass";
                    case "ZombieThumpWood" -> "Wood";
                    default -> "Default";
                };
            }
        }

        return this.getSoundPrefix();
    }

    public static String GetBreakFurnitureSound(IsoSprite sprite) {
        if (sprite == null) {
            return "BreakFurniture";
        } else if (sprite.getProperties().propertyEquals("MoveType", "Vegitation")) {
            return "BreakPlant";
        } else {
            String sinkTypeStr = sprite.getProperties().get("SinkType");
            if (sinkTypeStr != null) {
                return switch (sinkTypeStr) {
                    case "Ceramic" -> "BreakFurnitureCeramic";
                    case "Metal" -> "BreakFurnitureMetal";
                    default -> "BreakFurniture";
                };
            } else {
                String materialTypeStr = sprite.getProperties().get("MaterialType");
                MaterialType materialType = StringUtils.tryParseEnum(MaterialType.class, materialTypeStr, MaterialType.Default);

                return switch (materialType) {
                    case Ceramic -> "BreakFurnitureCeramic";
                    case Metal, Metal_Large, Metal_Light, Metal_Solid -> "BreakFurnitureMetal";
                    default -> "BreakFurniture";
                };
            }
        }
    }

    public static String GetBreakFurnitureSound(String spriteName) {
        IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
        return GetBreakFurnitureSound(sprite);
    }

    public void checkKeyHighlight(int playerIndex) {
        Key key = Key.highlightDoor[playerIndex].key;
        if (key != null) {
            boolean seen = this.square.isSeen(playerIndex);
            if (!seen) {
                IsoGridSquare oppositeSq = this.getOppositeSquare();
                seen = oppositeSq != null && oppositeSq.isSeen(playerIndex);
            }

            if (seen && this.getKeyId() == key.getKeyId()) {
                this.setHighlighted(playerIndex, true, false);
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.isDoor) {
            int ddIndex = IsoDoor.getDoubleDoorIndex(this);
            if (ddIndex != -1) {
                IsoObject master = null;
                if (ddIndex == 2) {
                    master = IsoDoor.getDoubleDoorObject(this, 1);
                } else if (ddIndex == 3) {
                    master = IsoDoor.getDoubleDoorObject(this, 4);
                }

                if (master != null && master.getSpriteModel() != null) {
                    this.updateRenderInfoForObjectPicker(x, y, z, col);
                    this.sx = 0.0F;
                    return;
                }
            }
        }

        super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
    }

    @Override
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
        if (this.isDoor) {
            int ddIndex = IsoDoor.getDoubleDoorIndex(this);
            if (ddIndex != -1) {
                IsoObject master = null;
                if (ddIndex == 2) {
                    master = IsoDoor.getDoubleDoorObject(this, 1);
                } else if (ddIndex == 3) {
                    master = IsoDoor.getDoubleDoorObject(this, 4);
                }

                if (master != null && master.getSpriteModel() != null) {
                    this.updateRenderInfoForObjectPicker(x, y, z, col);
                    this.sx = 0.0F;
                    return;
                }
            }
        }

        super.renderWallTile(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
    }

    @Override
    public SpriteModel getSpriteModel() {
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        return (ddIndex == 1 || ddIndex == 4) && IsoDoor.getDoubleDoorObject(this, ddIndex == 1 ? 2 : 3) == null ? null : super.getSpriteModel();
    }

    public void animalHit(IsoAnimal animal) {
        float baseDmg = 1000.0F;
        baseDmg *= animal.calcDamage();
        boolean wasThumpable = this.isThumpable;
        this.setIsThumpable(true);
        this.Damage(baseDmg);
        this.setIsThumpable(wasThumpable);
        if (this.health <= 0) {
            animal.getEmitter().playSound(this.breakSound, this);
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer(animal, this.breakSound, false, animal.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
            }

            this.destroy();
        }
    }

    public String getClosedSpriteTextureName() {
        return this.closedSprite != null && this.closedSprite.texture != null ? this.closedSprite.texture.getName() : null;
    }

    @Override
    public void afterRotated() {
        super.afterRotated();
        String spriteName = this.getSpriteName();
        if (spriteName.equals("carpentry_02_62")) {
            this.setLightSourceXOffset(5);
            this.setLightSourceYOffset(0);
        }

        if (spriteName.equals("carpentry_02_61")) {
            this.setLightSourceXOffset(-5);
            this.setLightSourceYOffset(0);
        }

        if (spriteName.equals("carpentry_02_59")) {
            this.setLightSourceXOffset(0);
            this.setLightSourceYOffset(5);
        }

        if (spriteName.equals("carpentry_02_60")) {
            this.setLightSourceXOffset(0);
            this.setLightSourceYOffset(-5);
        }
    }
}
