// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.AmbientStreamManager;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.ThumpState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.pathfind.PolygonalMap2;
import zombie.util.Type;

@UsedFromLua
public class IsoWindow extends IsoObject implements BarricadeAble, Thumpable {
    private static final int SinglePaneWindowMaxHealth = 50;
    private static final int DoublePaneWindowMaxHealth = 100;
    public static final float WeaponDoorDamageModifier = 5.0F;
    public static final float NoWeaponDoorDamage = 100.0F;
    private final IsoWindow.WindowType type = IsoWindow.WindowType.SinglePane;
    private int health = 50;
    private int maxHealth = 50;
    private boolean north;
    private boolean locked;
    private boolean permaLocked;
    private boolean open;
    private boolean destroyed;
    private boolean glassRemoved;
    private IsoSprite openSprite;
    private IsoSprite closedSprite;
    private IsoSprite smashedSprite;
    private IsoSprite glassRemovedSprite;

    public IsoWindow(IsoCell cell) {
        super(cell);
    }

    public IsoWindow(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north) {
        gid.getProperties().unset(IsoFlagType.cutN);
        gid.getProperties().unset(IsoFlagType.cutW);
        int openOffset = 0;
        if (gid.getProperties().has("OpenTileOffset")) {
            openOffset = Integer.parseInt(gid.getProperties().get("OpenTileOffset"));
        }

        this.permaLocked = gid.getProperties().has("WindowLocked");
        int smashedOffset = 0;
        if (gid.getProperties().has("SmashedTileOffset")) {
            smashedOffset = Integer.parseInt(gid.getProperties().get("SmashedTileOffset"));
        }

        this.closedSprite = gid;
        if (north) {
            this.closedSprite.getProperties().set(IsoFlagType.cutN);
            this.closedSprite.getProperties().set(IsoFlagType.windowN);
        } else {
            this.closedSprite.getProperties().set(IsoFlagType.cutW);
            this.closedSprite.getProperties().set(IsoFlagType.windowW);
        }

        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, openOffset);
        this.smashedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, smashedOffset);
        if (this.closedSprite.getProperties().has("GlassRemovedOffset")) {
            int glassRemovedOffset = Integer.parseInt(this.closedSprite.getProperties().get("GlassRemovedOffset"));
            this.glassRemovedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, this.closedSprite, glassRemovedOffset);
        } else {
            this.glassRemovedSprite = this.smashedSprite;
        }

        if (this.smashedSprite != this.closedSprite && this.smashedSprite != null) {
            this.smashedSprite.AddProperties(this.closedSprite);
            this.smashedSprite.setType(this.closedSprite.getType());
        }

        if (this.openSprite != this.closedSprite && this.openSprite != null) {
            this.openSprite.AddProperties(this.closedSprite);
            this.openSprite.setType(this.closedSprite.getType());
        }

        if (this.glassRemovedSprite != this.closedSprite && this.glassRemovedSprite != null) {
            this.glassRemovedSprite.AddProperties(this.closedSprite);
            this.glassRemovedSprite.setType(this.closedSprite.getType());
        }

        this.sprite = this.closedSprite;
        IsoObject wall = gridSquare.getWall(north);
        if (wall != null) {
            wall.rerouteCollide = this;
        }

        this.square = gridSquare;
        this.north = north;
        switch (this.type) {
            case SinglePane:
                this.maxHealth = this.health = 50;
                break;
            case DoublePane:
                this.maxHealth = this.health = 100;
        }

        IsoWindow.LockedHouseFrequency frequency = IsoWindow.LockedHouseFrequency.fromValue(SandboxOptions.instance.lockedHouses.getValue());
        int randLock = frequency.getLockChance();
        if (randLock > IsoWindow.LockedHouseFrequency.Never.lockChance) {
            this.locked = Rand.Next(100) < randLock;
        }
    }

    @Override
    public String getObjectName() {
        return "Window";
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

            return sq != null && sq.getRoom() != null ? sq : null;
        }
    }

    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        if (chr != null && chr.getCurrentSquare() != null) {
            IsoGridSquare sqChr = chr.getCurrentSquare();
            IsoGridSquare sqThis = this.getSquare();
            if (this.north) {
                if (sqChr.getY() < sqThis.getY()) {
                    return this.getCell().getGridSquare(sqThis.x, sqThis.y - 1, sqThis.z);
                }
            } else if (sqChr.getX() < sqThis.getX()) {
                return this.getCell().getGridSquare(sqThis.x - 1, sqThis.y, sqThis.z);
            }

            return sqThis;
        } else {
            return null;
        }
    }

    @Override
    public void AttackObject(IsoGameCharacter owner) {
        super.AttackObject(owner);
        IsoObject o = this.square.getWall(this.north);
        if (o != null) {
            o.AttackObject(owner);
        }
    }

    public IsoGridSquare getInsideSquare() {
        if (this.square == null) {
            return null;
        } else {
            return this.north
                ? this.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ())
                : this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
        }
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        return this.getInsideSquare();
    }

    public boolean isExterior() {
        IsoGridSquare sq = this.getSquare();
        IsoGridSquare sqOpposite = this.getOppositeSquare();
        return sqOpposite == null ? false : sq.isInARoom() != sqOpposite.isInARoom();
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        Thumpable thumpable = this.getThumpableFor(owner);
        if (GameClient.client) {
            if (player != null && weapon != ((IsoLivingCharacter)owner).bareHands && !this.isInvincible() && !(thumpable instanceof IsoBarricade)) {
                this.health = 0;
            }
        } else if (thumpable != null) {
            if (thumpable instanceof IsoBarricade) {
                thumpable.WeaponHit(owner, weapon);
            } else {
                LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
                if (weapon == ((IsoLivingCharacter)owner).bareHands) {
                    if (player != null) {
                        player.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Glass);
                        player.getEmitter().playSound(weapon.getDoorHitSound(), this);
                    }
                } else {
                    if (weapon != null) {
                        this.damage(weapon.getDoorDamage() * 5.0F, owner);
                    } else {
                        this.damage(100.0F, owner);
                    }

                    this.DirtySlice();
                    if (weapon != null && weapon.getDoorHitSound() != null) {
                        if (player != null) {
                            player.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Glass);
                        }

                        owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
                        if (GameServer.server) {
                            GameServer.PlayWorldSoundServer(owner, weapon.getDoorHitSound(), false, this.getSquare(), 1.0F, 20.0F, 2.0F, false);
                        }
                    }

                    WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                    if (player != null && weapon != ((IsoLivingCharacter)owner).bareHands && !this.isInvincible()) {
                        this.health = 0;
                    }

                    if (!this.isDestroyed() && this.health <= 0) {
                        this.smashWindow();
                        this.addBrokenGlass(owner);
                    }
                }
            }
        }
    }

    public void smashWindow(boolean bRemote, boolean doAlarm) {
        if (!this.destroyed) {
            if (GameClient.client && !bRemote) {
                GameClient.instance.smashWindow(this);
            }

            if (!bRemote) {
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer("SmashWindow", false, this.square, 0.2F, 20.0F, 1.1F, true);
                } else {
                    SoundManager.instance.PlayWorldSound("SmashWindow", this.square, 0.2F, 20.0F, 1.0F, true);
                }

                WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
            }

            this.destroyed = true;
            this.sprite = this.smashedSprite;
            if (this.getAttachedAnimSprite() != null) {
                this.getSquare().removeBlood(false, true);

                for (int i = 0; i < this.getAttachedAnimSprite().size(); i++) {
                    IsoSprite sprite = this.getAttachedAnimSprite().get(i).parentSprite;
                    if (sprite != null && sprite.getProperties().has("AttachedToGlass")) {
                        this.getAttachedAnimSprite().remove(i);
                        i--;
                    }
                }
            }

            this.getSquare().removeLightSwitch();
            if (doAlarm) {
                this.handleAlarm();
            }

            if (GameServer.server && !bRemote) {
                GameServer.smashWindow(this);
            }

            this.square.InvalidateSpecialObjectPaths();
            PolygonalMap2.instance.squareChanged(this.square);
        }
    }

    public void smashWindow(boolean bRemote) {
        this.smashWindow(bRemote, true);
    }

    public void smashWindow() {
        this.smashWindow(false, true);
    }

    public void addBrokenGlass(IsoMovingObject chr) {
        if (chr != null) {
            if (this.getSquare() != null) {
                if (this.getNorth()) {
                    this.addBrokenGlass(chr.getY() >= this.getSquare().getY());
                } else {
                    this.addBrokenGlass(chr.getX() >= this.getSquare().getX());
                }
            }
        }
    }

    public void addBrokenGlass(boolean onOppositeSquare) {
        IsoGridSquare square1 = onOppositeSquare ? this.getOppositeSquare() : this.getSquare();
        if (square1 != null) {
            square1.addBrokenGlass();
        }
    }

    private void handleAlarm() {
        if (!GameClient.client) {
            IsoGridSquare sq = this.getIndoorSquare();
            if (sq != null) {
                IsoRoom r = sq.getRoom();
                RoomDef def = r.def;
                if (def.building.alarmed && !GameClient.client) {
                    AmbientStreamManager.instance.doAlarm(def);
                }
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        if (from == this.square) {
            if (this.north && to.getY() < from.getY()) {
                if (obj != null) {
                    obj.collideWith(this);
                }

                return true;
            }

            if (!this.north && to.getX() < from.getX()) {
                if (obj != null) {
                    obj.collideWith(this);
                }

                return true;
            }
        } else {
            if (this.north && to.getY() > from.getY()) {
                if (obj != null) {
                    obj.collideWith(this);
                }

                return true;
            }

            if (!this.north && to.getX() > from.getX()) {
                if (obj != null) {
                    obj.collideWith(this);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (to.getZ() != from.getZ()) {
            return IsoObject.VisionResult.NoEffect;
        } else {
            if (from == this.square) {
                if (this.north && to.getY() < from.getY()) {
                    return IsoObject.VisionResult.Unblocked;
                }

                if (!this.north && to.getX() < from.getX()) {
                    return IsoObject.VisionResult.Unblocked;
                }
            } else {
                if (this.north && to.getY() > from.getY()) {
                    return IsoObject.VisionResult.Unblocked;
                }

                if (!this.north && to.getX() > from.getX()) {
                    return IsoObject.VisionResult.Unblocked;
                }
            }

            return IsoObject.VisionResult.NoEffect;
        }
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
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

        if (thumper instanceof IsoZombie isoZombie) {
            if (isoZombie.cognition == 1
                && !this.canClimbThrough(isoZombie)
                && !this.isInvincible()
                && (!this.locked || thumper.getCurrentSquare() != null && !thumper.getCurrentSquare().has(IsoFlagType.exterior))) {
                this.ToggleWindow((IsoGameCharacter)thumper);
                if (this.canClimbThrough(isoZombie)) {
                    return;
                }
            }

            int mult = ThumpState.getFastForwardDamageMultiplier();
            this.DirtySlice();
            this.damage(isoZombie.strength * mult, thumper);
            WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
        }

        if (!this.isDestroyed() && this.health <= 0) {
            if (this.getSquare().getBuilding() != null) {
                this.getSquare().getBuilding().forceAwake();
            }

            if (GameServer.server) {
                GameServer.smashWindow(this);
                GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, "SmashWindow", false, thumper.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
            }

            ((IsoGameCharacter)thumper).getEmitter().playSound("SmashWindow", this);
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
            thumper.setThumpTarget(null);
            this.destroyed = true;
            this.sprite = this.smashedSprite;
            this.square.InvalidateSpecialObjectPaths();
            this.addBrokenGlass(thumper);
            if (thumper instanceof IsoZombie isoZombie && this.getThumpableFor(isoZombie) != null) {
                thumper.setThumpTarget(this.getThumpableFor(isoZombie));
            }
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        IsoBarricade barricade = this.getBarricadeForCharacter(chr);
        if (barricade != null) {
            return barricade;
        } else {
            return (Thumpable)(!this.isDestroyed() && !this.IsOpen() ? this : this.getBarricadeOppositeCharacter(chr));
        }
    }

    @Override
    public float getThumpCondition() {
        return (float)PZMath.clamp(this.health, 0, this.maxHealth) / this.maxHealth;
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.open = input.get() == 1;
        this.north = input.get() == 1;
        this.health = input.getInt();
        this.locked = input.get() == 1;
        this.permaLocked = input.get() == 1;
        this.destroyed = input.get() == 1;
        this.glassRemoved = input.get() == 1;
        if (input.get() == 1) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }

        if (input.get() == 1) {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }

        if (input.get() == 1) {
            this.smashedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }

        if (input.get() == 1) {
            this.glassRemovedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }

        this.maxHealth = input.getInt();
        if (this.closedSprite != null) {
            if (this.north) {
                this.closedSprite.getProperties().set(IsoFlagType.cutN);
                this.closedSprite.getProperties().set(IsoFlagType.windowN);
            } else {
                this.closedSprite.getProperties().set(IsoFlagType.cutW);
                this.closedSprite.getProperties().set(IsoFlagType.windowW);
            }

            if (this.smashedSprite != this.closedSprite && this.smashedSprite != null) {
                this.smashedSprite.AddProperties(this.closedSprite);
                this.smashedSprite.setType(this.closedSprite.getType());
            }

            if (this.openSprite != this.closedSprite && this.openSprite != null) {
                this.openSprite.AddProperties(this.closedSprite);
                this.openSprite.setType(this.closedSprite.getType());
            }

            if (this.glassRemovedSprite != this.closedSprite && this.glassRemovedSprite != null) {
                this.glassRemovedSprite.AddProperties(this.closedSprite);
                this.glassRemovedSprite.setType(this.closedSprite.getType());
            }
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToWindowList(this);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.getCell().removeFromWindowList(this);
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.open ? 1 : 0));
        output.put((byte)(this.north ? 1 : 0));
        output.putInt(this.health);
        output.put((byte)(this.locked ? 1 : 0));
        output.put((byte)(this.permaLocked ? 1 : 0));
        output.put((byte)(this.destroyed ? 1 : 0));
        output.put((byte)(this.glassRemoved ? 1 : 0));
        if (this.openSprite != null) {
            output.put((byte)1);
            output.putInt(this.openSprite.id);
        } else {
            output.put((byte)0);
        }

        if (this.closedSprite != null) {
            output.put((byte)1);
            output.putInt(this.closedSprite.id);
        } else {
            output.put((byte)0);
        }

        if (this.smashedSprite != null) {
            output.put((byte)1);
            output.putInt(this.smashedSprite.id);
        } else {
            output.put((byte)0);
        }

        if (this.glassRemovedSprite != null) {
            output.put((byte)1);
            output.putInt(this.glassRemovedSprite.id);
        } else {
            output.put((byte)0);
        }

        output.putInt(this.maxHealth);
    }

    @Override
    public void saveState(ByteBuffer bb) throws IOException {
        bb.put((byte)(this.locked ? 1 : 0));
    }

    @Override
    public void loadState(ByteBuffer bb) throws IOException {
        boolean Locked = bb.get() == 1;
        if (Locked != this.locked) {
            this.locked = Locked;
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
    }

    public void openCloseCurtain(IsoGameCharacter chr) {
        if (chr == IsoPlayer.getInstance()) {
            IsoGridSquare sq = null;
            IsoGridSquare s = this.square;
            if (this.north) {
                if (s.getRoom() == null) {
                    s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
                }
            } else if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
            }

            sq = s;
            if (s != null) {
                for (int n = 0; n < sq.getSpecialObjects().size(); n++) {
                    if (sq.getSpecialObjects().get(n) instanceof IsoCurtain) {
                        ((IsoCurtain)sq.getSpecialObjects().get(n)).ToggleDoorSilent();
                        return;
                    }
                }
            }
        }
    }

    public void removeSheet(IsoGameCharacter chr) {
        IsoGridSquare sq = null;
        if (this.north) {
            IsoGridSquare s = this.square;
            if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
            }

            sq = s;
        } else {
            IsoGridSquare s = this.square;
            if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
            }

            sq = s;
        }

        for (int n = 0; n < sq.getSpecialObjects().size(); n++) {
            IsoObject o = sq.getSpecialObjects().get(n);
            if (o instanceof IsoCurtain) {
                sq.transmitRemoveItemFromSquare(o);
                if (chr != null) {
                    InventoryItem item = chr.getInventory().AddItem(o.getName());
                    if (GameServer.server) {
                        GameServer.sendAddItemToContainer(chr.getInventory(), item);
                    }
                }
                break;
            }
        }
    }

    public void addSheet(IsoGameCharacter chr) {
        IsoGridSquare sq = null;
        IsoGridSquare s = this.square;
        IsoObjectType curtainType;
        if (this.north) {
            curtainType = IsoObjectType.curtainN;
            if (chr != null) {
                if (chr.getY() < this.getY()) {
                    s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
                    curtainType = IsoObjectType.curtainS;
                }
            } else if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
                curtainType = IsoObjectType.curtainS;
            }
        } else {
            curtainType = IsoObjectType.curtainW;
            if (chr != null) {
                if (chr.getX() < this.getX()) {
                    s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
                    curtainType = IsoObjectType.curtainE;
                }
            } else if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
                curtainType = IsoObjectType.curtainE;
            }
        }

        if (s.getCurtain(curtainType) == null) {
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
            IsoCurtain c = new IsoCurtain(this.getCell(), s, "fixtures_windows_curtains_01_" + gid, this.north);
            s.AddSpecialTileObject(c);
            if (!c.open) {
                c.ToggleDoorSilent();
            }

            if (!GameClient.client && chr != null) {
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

    public void ToggleWindow(IsoGameCharacter chr) {
        IsoPlayer player = Type.tryCastTo(chr, IsoPlayer.class);
        this.DirtySlice();
        IsoGridSquare.setRecalcLightTime(-1.0F);
        if (!this.permaLocked) {
            if (!this.destroyed) {
                if (chr == null || this.getBarricadeForCharacter(chr) == null) {
                    this.locked = false;
                    this.open = !this.open;
                    this.sprite = this.closedSprite;
                    this.square.InvalidateSpecialObjectPaths();
                    if (this.open) {
                        if (!(chr instanceof IsoZombie) || SandboxOptions.getInstance().lore.triggerHouseAlarm.getValue()) {
                            this.handleAlarm();
                        }

                        this.sprite = this.openSprite;
                    }

                    this.square.RecalcProperties();
                    this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null, null);
                    PolygonalMap2.instance.squareChanged(this.square);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                    if (player != null && player.isLocalPlayer()) {
                        player.triggerMusicIntensityEvent(this.open ? "WindowOpen" : "WindowClose");
                    }
                }
            }
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        byte index = (byte)this.getObjectIndex();
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(index);
        b.putByte((byte)1);
        b.putByte((byte)0);
        b.putByte((byte)(this.open ? 1 : 0));
        b.putByte((byte)(this.destroyed ? 1 : 0));
        b.putByte((byte)(this.locked ? 1 : 0));
        b.putByte((byte)(this.permaLocked ? 1 : 0));
        b.putByte((byte)(this.glassRemoved ? 1 : 0));
        b.putInt(this.health);
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        this.open = bb.get() == 1;
        this.destroyed = bb.get() == 1;
        this.locked = bb.get() == 1;
        this.permaLocked = bb.get() == 1;
        this.glassRemoved = bb.get() == 1;
        this.health = bb.getInt();
        if (this.destroyed) {
            if (this.glassRemoved) {
                this.sprite = this.glassRemovedSprite;
            } else {
                this.sprite = this.smashedSprite;
            }
        } else if (this.open) {
            this.sprite = this.openSprite;
        } else {
            this.sprite = this.closedSprite;
        }

        this.square.RecalcProperties();
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    public static boolean isTopOfSheetRopeHere(IsoGridSquare sq) {
        return sq == null
            ? false
            : sq.has(IsoFlagType.climbSheetTopN)
                || sq.has(IsoFlagType.climbSheetTopS)
                || sq.has(IsoFlagType.climbSheetTopW)
                || sq.has(IsoFlagType.climbSheetTopE);
    }

    public static boolean isTopOfSheetRopeHere(IsoGridSquare sq, boolean north) {
        if (sq == null) {
            return false;
        } else if (north) {
            return sq.has(IsoFlagType.climbSheetTopN)
                ? true
                : sq.getAdjacentSquare(IsoDirections.N) != null && sq.getAdjacentSquare(IsoDirections.N).has(IsoFlagType.climbSheetTopS);
        } else {
            return sq.has(IsoFlagType.climbSheetTopW)
                ? true
                : sq.getAdjacentSquare(IsoDirections.W) != null && sq.getAdjacentSquare(IsoDirections.W).has(IsoFlagType.climbSheetTopE);
        }
    }

    @Override
    public boolean haveSheetRope() {
        return isTopOfSheetRopeHere(this.square, this.north);
    }

    public static boolean isSheetRopeHere(IsoGridSquare sq) {
        return sq == null
            ? false
            : sq.has(IsoFlagType.climbSheetTopW)
                || sq.has(IsoFlagType.climbSheetTopN)
                || sq.has(IsoFlagType.climbSheetTopE)
                || sq.has(IsoFlagType.climbSheetTopS)
                || sq.has(IsoFlagType.climbSheetW)
                || sq.has(IsoFlagType.climbSheetN)
                || sq.has(IsoFlagType.climbSheetE)
                || sq.has(IsoFlagType.climbSheetS);
    }

    public static boolean canClimbHere(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        } else if (sq.getProperties().has(IsoFlagType.solid)) {
            return false;
        } else {
            return !sq.has(IsoObjectType.stairsBN) && !sq.has(IsoObjectType.stairsMN) && !sq.has(IsoObjectType.stairsTN)
                ? !sq.has(IsoObjectType.stairsBW) && !sq.has(IsoObjectType.stairsMW) && !sq.has(IsoObjectType.stairsTW)
                : false;
        }
    }

    public static int countAddSheetRope(IsoGridSquare sq, boolean north) {
        if (isTopOfSheetRopeHere(sq, north)) {
            return 0;
        } else {
            IsoCell cell = IsoWorld.instance.currentCell;
            if (sq.TreatAsSolidFloor()) {
                if (north) {
                    IsoGridSquare sqn = cell.getOrCreateGridSquare(sq.getX(), sq.getY() - 1, sq.getZ());
                    if (sqn == null || sqn.TreatAsSolidFloor() || isSheetRopeHere(sqn) || !canClimbHere(sqn)) {
                        return 0;
                    }

                    sq = sqn;
                } else {
                    IsoGridSquare sqe = cell.getOrCreateGridSquare(sq.getX() - 1, sq.getY(), sq.getZ());
                    if (sqe == null || sqe.TreatAsSolidFloor() || isSheetRopeHere(sqe) || !canClimbHere(sqe)) {
                        return 0;
                    }

                    sq = sqe;
                }
            }

            for (int count = 1; sq != null; count++) {
                if (!canClimbHere(sq)) {
                    return 0;
                }

                if (sq.TreatAsSolidFloor()) {
                    return count;
                }

                if (sq.getZ() == sq.getChunk().getMinLevel()) {
                    return count;
                }

                sq = cell.getOrCreateGridSquare(sq.getX(), sq.getY(), sq.getZ() - 1);
            }

            return 0;
        }
    }

    @Override
    public int countAddSheetRope() {
        return countAddSheetRope(this.square, this.north);
    }

    public static boolean canAddSheetRope(IsoGridSquare sq, boolean north) {
        return countAddSheetRope(sq, north) != 0;
    }

    @Override
    public boolean canAddSheetRope() {
        return !this.canClimbThrough(null) ? false : canAddSheetRope(this.square, this.north);
    }

    @Override
    public boolean addSheetRope(IsoPlayer player, String itemType) {
        return !this.canAddSheetRope() ? false : addSheetRope(player, this.square, this.north, itemType);
    }

    public static boolean addSheetRope(IsoPlayer player, IsoGridSquare sq, boolean north, String itemType) {
        boolean bLast = false;
        int n = 0;
        int i = 0;
        if (north) {
            i = 1;
        }

        boolean south = false;
        boolean east = false;
        IsoGridSquare sqe = null;
        IsoGridSquare sqn = null;
        IsoCell cell = IsoWorld.instance.currentCell;
        if (sq.TreatAsSolidFloor()) {
            if (!north) {
                sqe = cell.getGridSquare(sq.getX() - 1, sq.getY(), sq.getZ());
                if (sqe != null) {
                    east = true;
                    i = 3;
                }
            } else {
                sqn = cell.getGridSquare(sq.getX(), sq.getY() - 1, sq.getZ());
                if (sqn != null) {
                    south = true;
                    i = 4;
                }
            }
        }

        for (; sq != null && (GameServer.server || player.getInventory().contains(itemType)); sq.invalidateRenderChunkLevel(64L)) {
            String d = "crafted_01_" + i;
            if (n > 0) {
                if (east) {
                    d = "crafted_01_10";
                } else if (south) {
                    d = "crafted_01_13";
                } else {
                    d = "crafted_01_" + (i + 8);
                }
            }

            IsoObject sheetTop = new IsoObject(cell, sq, d);
            sheetTop.setName(itemType);
            sheetTop.sheetRope = true;
            sq.getObjects().add(sheetTop);
            sheetTop.transmitCompleteItemToClients();
            sq.haveSheetRope = true;
            if (south && n == 0) {
                sq = sqn;
                sheetTop = new IsoObject(cell, sqn, "crafted_01_5");
                sheetTop.setName(itemType);
                sheetTop.sheetRope = true;
                sqn.getObjects().add(sheetTop);
                sheetTop.transmitCompleteItemToClients();
            }

            if (east && n == 0) {
                sq = sqe;
                sheetTop = new IsoObject(cell, sqe, "crafted_01_2");
                sheetTop.setName(itemType);
                sheetTop.sheetRope = true;
                sqe.getObjects().add(sheetTop);
                sheetTop.transmitCompleteItemToClients();
            }

            sq.RecalcProperties();
            sq.getProperties().unset(IsoFlagType.solidtrans);
            if (n == 0 && !sq.getProperties().has("TieSheetRope")) {
                ArrayList<InventoryItem> items = player.getInventory().RemoveAll("Nails", 1);
                if (GameServer.server) {
                    GameServer.sendRemoveItemsFromContainer(player.getInventory(), items);
                }
            }

            ArrayList<InventoryItem> items = player.getInventory().RemoveAll(itemType, 1);
            if (GameServer.server) {
                GameServer.sendRemoveItemsFromContainer(player.getInventory(), items);
            }

            n++;
            if (bLast) {
                break;
            }

            sq = cell.getOrCreateGridSquare(sq.getX(), sq.getY(), sq.getZ() - 1);
            if (sq != null && sq.TreatAsSolidFloor()) {
                bLast = true;
            }
        }

        return true;
    }

    @Override
    public boolean removeSheetRope(IsoPlayer player) {
        return !this.haveSheetRope() ? false : removeSheetRope(player, this.square, this.north);
    }

    public static boolean removeSheetRope(IsoPlayer player, IsoGridSquare square, boolean north) {
        if (square == null) {
            return false;
        } else {
            IsoGridSquare sq = square;
            square.haveSheetRope = false;
            IsoFlagType type1;
            IsoFlagType type2;
            if (north) {
                if (square.has(IsoFlagType.climbSheetTopN)) {
                    type1 = IsoFlagType.climbSheetTopN;
                    type2 = IsoFlagType.climbSheetN;
                } else {
                    if (square.getAdjacentSquare(IsoDirections.N) == null || !square.getAdjacentSquare(IsoDirections.N).has(IsoFlagType.climbSheetTopS)) {
                        return false;
                    }

                    type1 = IsoFlagType.climbSheetTopS;
                    type2 = IsoFlagType.climbSheetS;
                    String tile = "crafted_01_4";

                    for (int i = 0; i < sq.getObjects().size(); i++) {
                        IsoObject o = sq.getObjects().get(i);
                        if (o.sprite != null && o.sprite.getName() != null && o.sprite.getName().equals(tile)) {
                            sq.transmitRemoveItemFromSquare(o);
                            break;
                        }
                    }

                    sq = square.getAdjacentSquare(IsoDirections.N);
                }
            } else if (square.has(IsoFlagType.climbSheetTopW)) {
                type1 = IsoFlagType.climbSheetTopW;
                type2 = IsoFlagType.climbSheetW;
            } else {
                if (square.getAdjacentSquare(IsoDirections.W) == null || !square.getAdjacentSquare(IsoDirections.W).has(IsoFlagType.climbSheetTopE)) {
                    return false;
                }

                type1 = IsoFlagType.climbSheetTopE;
                type2 = IsoFlagType.climbSheetE;
                String tile = "crafted_01_3";

                for (int ix = 0; ix < sq.getObjects().size(); ix++) {
                    IsoObject o = sq.getObjects().get(ix);
                    if (o.sprite != null && o.sprite.getName() != null && o.sprite.getName().equals(tile)) {
                        sq.transmitRemoveItemFromSquare(o);
                        break;
                    }
                }

                sq = square.getAdjacentSquare(IsoDirections.W);
            }

            while (sq != null) {
                boolean removed = false;

                for (int ixx = 0; ixx < sq.getObjects().size(); ixx++) {
                    IsoObject o = sq.getObjects().get(ixx);
                    if (o.getProperties() != null && (o.getProperties().has(type1) || o.getProperties().has(type2))) {
                        sq.transmitRemoveItemFromSquare(o);
                        if (GameServer.server) {
                            if (player != null) {
                                player.sendObjectChange("addItemOfType", "type", o.getName());
                            }
                        } else if (player != null) {
                            player.getInventory().AddItem(o.getName());
                        }

                        removed = true;
                        break;
                    }
                }

                if (!removed || sq.getZ() == sq.getChunk().getMinLevel()) {
                    break;
                }

                sq = sq.getCell().getGridSquare(sq.getX(), sq.getY(), sq.getZ() - 1);
            }

            return true;
        }
    }

    @Override
    public void Damage(float amount) {
        this.damage(amount);
    }

    private void damage(float amount) {
        if (!this.isInvincible() && !"Tutorial".equals(Core.gameMode)) {
            this.DirtySlice();
            this.health -= (int)amount;
            if (this.health < 0) {
                this.health = 0;
            }

            if (!this.isDestroyed() && this.health == 0) {
                this.smashWindow(false, SandboxOptions.getInstance().lore.triggerHouseAlarm.getValue());
                if (this.getSquare().getBuilding() != null) {
                    this.getSquare().getBuilding().forceAwake();
                }
            }
        }
    }

    private void damage(float amount, IsoMovingObject chr) {
        if (!this.isInvincible() && !"Tutorial".equals(Core.gameMode)) {
            this.health -= (int)amount;
            if (this.health < 0) {
                this.health = 0;
            }

            if (!this.isDestroyed() && this.health == 0) {
                boolean doAlarm = !(chr instanceof IsoZombie) || SandboxOptions.getInstance().lore.triggerHouseAlarm.getValue();
                this.smashWindow(false, doAlarm);
                this.addBrokenGlass(chr);
            }
        }
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean isSmashed() {
        return this.destroyed;
    }

    public boolean isInvincible() {
        if (this.square != null && this.square.has(IsoFlagType.makeWindowInvincible)) {
            int index = this.getObjectIndex();
            if (index != -1) {
                IsoObject[] objects = this.square.getObjects().getElements();
                int size = this.square.getObjects().size();

                for (int i = 0; i < size; i++) {
                    if (i != index) {
                        IsoObject obj = objects[i];
                        PropertyContainer properties = obj.getProperties();
                        if (properties != null
                            && properties.has(this.getNorth() ? IsoFlagType.cutN : IsoFlagType.cutW)
                            && properties.has(IsoFlagType.makeWindowInvincible)) {
                            return true;
                        }
                    }
                }
            }

            return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.makeWindowInvincible);
        } else {
            return false;
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
        return true;
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else {
            return this.north ? pos.set(this.getX() + 0.5F, this.getY()) : pos.set(this.getX(), this.getY() + 0.5F);
        }
    }

    public void setIsLocked(boolean lock) {
        this.locked = lock;
    }

    public IsoSprite getOpenSprite() {
        return this.openSprite;
    }

    public void setOpenSprite(IsoSprite sprite) {
        this.openSprite = sprite;
    }

    public void setSmashed(boolean destroyed) {
        if (destroyed) {
            this.destroyed = true;
            this.sprite = this.smashedSprite;
        } else {
            this.destroyed = false;
            this.sprite = this.open ? this.openSprite : this.closedSprite;
            this.health = this.maxHealth;
        }

        this.glassRemoved = false;
    }

    public IsoSprite getSmashedSprite() {
        return this.smashedSprite;
    }

    public void setSmashedSprite(IsoSprite sprite) {
        this.smashedSprite = sprite;
    }

    public void setPermaLocked(Boolean permaLock) {
        this.permaLocked = permaLock;
    }

    public boolean isPermaLocked() {
        return this.permaLocked;
    }

    public static boolean canClimbThroughHelper(IsoGameCharacter chr, IsoGridSquare sq, IsoGridSquare oppositeSq, boolean north) {
        if (chr instanceof IsoAnimal) {
            return false;
        } else {
            IsoGridSquare testSquare = sq;
            float dx = 0.5F;
            float dy = 0.5F;
            if (north) {
                if (chr.getY() >= sq.getY()) {
                    testSquare = oppositeSq;
                    dy = 0.7F;
                } else {
                    dy = 0.3F;
                }
            } else if (chr.getX() >= sq.getX()) {
                testSquare = oppositeSq;
                dx = 0.7F;
            } else {
                dx = 0.3F;
            }

            if (testSquare == null) {
                return false;
            } else if (testSquare.isSolid()) {
                return false;
            } else if (testSquare.has(IsoFlagType.water)) {
                return false;
            } else {
                return !chr.canClimbDownSheetRope(testSquare)
                        && !testSquare.HasStairsBelow()
                        && !PolygonalMap2.instance.canStandAt(testSquare.x + dx, testSquare.y + dy, testSquare.z, null, 19)
                    ? !testSquare.TreatAsSolidFloor()
                    : !(GameClient.client && chr instanceof IsoPlayer isoPlayer)
                        || SafeHouse.isSafeHouse(testSquare, isoPlayer.getUsername(), true) == null
                        || ServerOptions.instance.safehouseAllowTrepass.getValue();
            }
        }
    }

    public boolean canClimbThrough(IsoGameCharacter chr) {
        if (this.square == null || this.isInvincible()) {
            return false;
        } else if (this.isBarricaded()) {
            return false;
        } else if (chr != null && !canClimbThroughHelper(chr, this.getSquare(), this.getOppositeSquare(), this.north)) {
            return false;
        } else {
            IsoGameCharacter chrClosing = this.getFirstCharacterClosing();
            if (chrClosing != null && chrClosing.isVariable("CloseWindowOutcome", "success")) {
                return false;
            } else {
                return this.health > 0 && !this.destroyed ? this.open : true;
            }
        }
    }

    public IsoGameCharacter getFirstCharacterClimbingThrough() {
        IsoGameCharacter chr = this.getFirstCharacterClimbingThrough(this.getSquare());
        return chr != null ? chr : this.getFirstCharacterClimbingThrough(this.getOppositeSquare());
    }

    public IsoGameCharacter getFirstCharacterClimbingThrough(IsoGridSquare square) {
        if (square == null) {
            return null;
        } else {
            for (int i = 0; i < square.getMovingObjects().size(); i++) {
                IsoGameCharacter chr = Type.tryCastTo(square.getMovingObjects().get(i), IsoGameCharacter.class);
                if (chr != null && chr.isClimbingThroughWindow(this)) {
                    return chr;
                }
            }

            return null;
        }
    }

    public IsoGameCharacter getFirstCharacterClosing() {
        IsoGameCharacter chr = this.getFirstCharacterClosing(this.getSquare());
        return chr != null ? chr : this.getFirstCharacterClosing(this.getOppositeSquare());
    }

    public IsoGameCharacter getFirstCharacterClosing(IsoGridSquare square) {
        if (square == null) {
            return null;
        } else {
            for (int i = 0; i < square.getMovingObjects().size(); i++) {
                IsoGameCharacter chr = Type.tryCastTo(square.getMovingObjects().get(i), IsoGameCharacter.class);
                if (chr != null && chr.isClosingWindow(this)) {
                    return chr;
                }
            }

            return null;
        }
    }

    public boolean isGlassRemoved() {
        return this.glassRemoved;
    }

    public void setGlassRemoved(boolean removed) {
        if (this.destroyed) {
            if (removed) {
                this.sprite = this.glassRemovedSprite;
                this.glassRemoved = true;
            } else {
                this.sprite = this.smashedSprite;
                this.glassRemoved = false;
            }

            if (this.getObjectIndex() != -1) {
                PolygonalMap2.instance.squareChanged(this.square);
            }
        }
    }

    public void removeBrokenGlass() {
        if (GameClient.client) {
            GameClient.instance.removeBrokenGlass(this);
        } else {
            this.setGlassRemoved(true);
        }
    }

    public IsoBarricade addBarricadesDebug(int numPlanks, boolean metal) {
        IsoGridSquare outside = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
        boolean addOpposite = outside != this.square;
        IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(this, addOpposite);
        if (barricade != null) {
            for (int b = 0; b < numPlanks; b++) {
                if (metal) {
                    barricade.addMetalBar(null, null);
                } else {
                    barricade.addPlank(null, null);
                }
            }
        }

        return barricade;
    }

    public void addRandomBarricades() {
        IsoGridSquare outside = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
        if (this.getZ() == 0.0F && outside != null && outside.getRoom() == null) {
            boolean addOpposite = outside != this.square;
            IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(this, addOpposite);
            if (barricade != null) {
                int numPlanks = Rand.Next(1, 4);

                for (int b = 0; b < numPlanks; b++) {
                    barricade.addPlank(null, null);
                }

                if (GameServer.server) {
                    barricade.transmitCompleteItemToClients();
                }
            }
        } else {
            this.addSheet(null);
            this.HasCurtains().ToggleDoor(null);
        }
    }

    public int getHealth() {
        return this.health;
    }

    public boolean IsOpen() {
        return this.open;
    }

    public boolean isNorth() {
        return this.north;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return true;
    }

    public boolean canAttackBypassIsoBarricade(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        IsoBarricade isoBarricade = this.getBarricadeForCharacter(isoGameCharacter);
        return isoBarricade == null ? true : isoBarricade.canAttackBypassIsoBarricade(isoGameCharacter, handWeapon);
    }

    @Override
    public void reset() {
        this.sprite = this.closedSprite;
        this.destroyed = false;
        this.glassRemoved = false;
        switch (this.type) {
            case SinglePane:
                this.maxHealth = this.health = 50;
                break;
            case DoublePane:
                this.maxHealth = this.health = 100;
        }
    }

    public static void resetCurrentCellWindows() {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();

        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.reset();
        }
    }

    private static enum LockedHouseFrequency {
        Never(0, 0),
        ExtremelyRare(1, 5),
        Rare(2, 10),
        Sometimes(3, 50),
        Often(4, 60),
        VeryOften(5, 70);

        private final int value;
        private final int lockChance;

        private LockedHouseFrequency(final int value, final int lockChance) {
            this.value = value;
            this.lockChance = lockChance;
        }

        public int getLockChance() {
            return this.lockChance;
        }

        public static IsoWindow.LockedHouseFrequency fromValue(int value) {
            for (IsoWindow.LockedHouseFrequency freq : values()) {
                if (freq.value == value) {
                    return freq;
                }
            }

            return VeryOften;
        }
    }

    public static enum WindowType {
        SinglePane,
        DoublePane;
    }
}
