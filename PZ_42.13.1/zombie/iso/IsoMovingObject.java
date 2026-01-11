// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import zombie.AttackType;
import zombie.CollisionManager;
import zombie.GameTime;
import zombie.MovingObjectUpdateScheduler;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.State;
import zombie.ai.astar.Mover;
import zombie.ai.states.AttackState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.CrawlingZombieTurnState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.animals.AnimalAttackState;
import zombie.ai.states.animals.AnimalClimbOverFenceState;
import zombie.ai.states.animals.AnimalZoneState;
import zombie.audio.TreeSoundManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraits;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ZombiePopulationManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoMovingObject extends IsoObject implements Mover {
    public static TreeSoundManager treeSoundMgr = new TreeSoundManager();
    public static final int MAX_ZOMBIES_EATING = 3;
    private static int idCount;
    private static final Vector2 tempo = new Vector2();
    public boolean noDamage;
    public IsoGridSquare last;
    private float lastX;
    private float ly;
    private float lz;
    private float nx;
    private float ny;
    private float x;
    private float y;
    private float z;
    public Vector2 reqMovement = new Vector2();
    public IsoSpriteInstance def;
    protected IsoGridSquare current;
    protected Vector2 hitDir = new Vector2();
    protected int id;
    protected IsoGridSquare movingSq;
    protected boolean solid = true;
    protected float width = 0.24F;
    protected boolean shootable = true;
    protected boolean collidable = true;
    private float scriptnx;
    private float scriptny;
    protected String scriptModule = "none";
    protected Vector2 movementLastFrame = new Vector2();
    protected float weight = 1.0F;
    boolean onFloor;
    private boolean closeKilled;
    private String collideType;
    private float lastCollideTime;
    private int timeSinceZombieAttack = 1000000;
    private boolean collidedE;
    private boolean collidedN;
    private IsoObject collidedObject;
    private boolean collidedS;
    private boolean collidedThisFrame;
    private boolean collidedW;
    private boolean collidedWithDoor;
    private boolean collidedWithVehicle;
    private boolean destroyed;
    private boolean firstUpdate = true;
    private float impulsex;
    private float impulsey;
    private float limpulsex;
    private float limpulsey;
    private float hitForce;
    private float hitFromAngle;
    private int pathFindIndex = -1;
    private float stateEventDelayTimer;
    private Thumpable thumpTarget;
    private boolean altCollide;
    private IsoZombie lastTargettedBy;
    private float feelersize = 0.5F;
    private final ArrayList<IsoZombie> eatingZombies = new ArrayList<>();

    public IsoMovingObject(IsoCell cell) {
        this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        if (cell != null) {
            this.id = idCount++;
            if (this.getCell().isSafeToAdd()) {
                this.getCell().getObjectList().add(this);
            } else {
                this.getCell().getAddList().add(this);
            }
        }
    }

    public IsoMovingObject(IsoCell cell, boolean bObjectListAdd) {
        this.id = idCount++;
        this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        if (bObjectListAdd) {
            if (this.getCell().isSafeToAdd()) {
                this.getCell().getObjectList().add(this);
            } else {
                this.getCell().getAddList().add(this);
            }
        }
    }

    public IsoMovingObject(IsoCell cell, IsoGridSquare square, IsoSprite spr, boolean bObjectListAdd) {
        this.id = idCount++;
        this.sprite = spr;
        if (bObjectListAdd) {
            if (this.getCell().isSafeToAdd()) {
                this.getCell().getObjectList().add(this);
            } else {
                this.getCell().getAddList().add(this);
            }
        }
    }

    public IsoMovingObject() {
        this.id = idCount++;
        this.getCell().getAddList().add(this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{  Name:" + this.getName() + ",  ID:" + this.getID() + " }";
    }

    /**
     * @return the IDCount
     */
    public static int getIDCount() {
        return idCount;
    }

    /**
     * 
     * @param aIDCount the IDCount to set
     */
    public static void setIDCount(int aIDCount) {
        idCount = aIDCount;
    }

    public IsoBuilding getBuilding() {
        if (this.current == null) {
            return null;
        } else {
            IsoRoom r = this.current.getRoom();
            return r == null ? null : r.building;
        }
    }

    public IWorldRegion getMasterRegion() {
        return this.current != null ? this.current.getIsoWorldRegion() : null;
    }

    public float getWeight() {
        return this.weight;
    }

    /**
     * 
     * @param weight the weight to set
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getWeight(float x, float y) {
        return this.weight;
    }

    @Override
    public void onMouseRightClick(int lx, int ly) {
        if (this.square.getZ() == PZMath.fastfloor(IsoPlayer.getInstance().getZ()) && this.DistToProper(IsoPlayer.getInstance()) <= 2.0F) {
            IsoPlayer.getInstance().setDragObject(this);
        }
    }

    @Override
    public String getObjectName() {
        return "IsoMovingObject";
    }

    @Override
    public void onMouseRightReleased() {
    }

    public void collideWith(IsoObject obj) {
        if (this instanceof IsoGameCharacter && obj instanceof IsoGameCharacter) {
            LuaEventManager.triggerEvent("OnCharacterCollide", this, obj);
        } else {
            LuaEventManager.triggerEvent("OnObjectCollide", this, obj);
        }
    }

    public void doStairs() {
        if (this.current != null) {
            if (this.last != null) {
                if (this instanceof IsoGameCharacter && ((IsoGameCharacter)this).isAnimal() && !((IsoAnimal)this).canClimbStairs()) {
                }

                if (!(this instanceof IsoPhysicsObject)) {
                    IsoGridSquare current = this.current;
                    if ((current.has(IsoObjectType.stairsTN) || current.has(IsoObjectType.stairsTW)) && this.getZ() - PZMath.fastfloor(this.getZ()) < 0.1F) {
                        IsoGridSquare below = IsoWorld.instance.currentCell.getGridSquare(current.x, current.y, current.z - 1);
                        if (below != null && (below.has(IsoObjectType.stairsTN) || below.has(IsoObjectType.stairsTW))) {
                            current = below;
                        }
                    }

                    if (this instanceof IsoGameCharacter && (this.last.has(IsoObjectType.stairsTN) || this.last.has(IsoObjectType.stairsTW))) {
                        this.setZ(Math.round(this.getZ()));
                    }

                    float z = this.getZ();
                    if (current.HasStairs()) {
                        z = current.getApparentZ(this.getX() - current.getX(), this.getY() - current.getY());
                    }

                    if (this instanceof IsoGameCharacter) {
                        State state = ((IsoGameCharacter)this).getCurrentState();
                        if (state == ClimbOverFenceState.instance() || state == ClimbThroughWindowState.instance()) {
                            if (current.HasStairs() && this.getZ() > z) {
                                this.setZ(Math.max(z, this.getZ() - 0.075F * GameTime.getInstance().getMultiplier()));
                            }

                            return;
                        }
                    }

                    if (Math.abs(z - this.getZ()) < 0.95F) {
                        this.setZ(z);
                    }
                }
            }
        }
    }

    private void handleSlopedSurface() {
        if (!(this instanceof IsoPhysicsObject)) {
            if (this.current != null) {
                if (!(this instanceof IsoGameCharacter) || ((IsoGameCharacter)this).getVehicle() == null) {
                    if (this.last != null && this.last != this.current && this.last.hasSlopedSurface()) {
                        float slopeHeightMax = this.last.getSlopedSurfaceHeightMax();
                        if (slopeHeightMax == 1.0F) {
                            this.setZ(Math.round(this.getZ()));
                        }
                    }

                    float dz = this.current.getSlopedSurfaceHeight(this.getX() % 1.0F, this.getY() % 1.0F);
                    if (!(dz <= 0.0F)) {
                        this.setZ(this.current.z + dz);
                    }
                }
            }
        }
    }

    @Override
    public int getID() {
        return this.id;
    }

    /**
     * 
     * @param id the ID to set
     */
    public void setID(int id) {
        this.id = id;
    }

    @Override
    public int getPathFindIndex() {
        return this.pathFindIndex;
    }

    /**
     * 
     * @param PathFindIndex the PathFindIndex to set
     */
    public void setPathFindIndex(int PathFindIndex) {
        this.pathFindIndex = PathFindIndex;
    }

    public float getScreenX() {
        return IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
    }

    public float getScreenY() {
        return IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
    }

    public Thumpable getThumpTarget() {
        return this.thumpTarget;
    }

    /**
     * 
     * @param thumpTarget the thumpTarget to set
     */
    public void setThumpTarget(Thumpable thumpTarget) {
        this.thumpTarget = thumpTarget;
    }

    public Vector2 getVectorFromDirection(Vector2 moveForwardVec) {
        return getVectorFromDirection(moveForwardVec, this.dir);
    }

    public static Vector2 getVectorFromDirection(Vector2 moveForwardVec, IsoDirections dir) {
        if (moveForwardVec == null) {
            DebugLog.General.warn("Supplied vector2 is null. Cannot be processed. Using fail-safe fallback.");
            moveForwardVec = new Vector2();
        }

        moveForwardVec.x = 0.0F;
        moveForwardVec.y = 0.0F;
        switch (dir) {
            case S:
                moveForwardVec.x = 0.0F;
                moveForwardVec.y = 1.0F;
                break;
            case N:
                moveForwardVec.x = 0.0F;
                moveForwardVec.y = -1.0F;
                break;
            case E:
                moveForwardVec.x = 1.0F;
                moveForwardVec.y = 0.0F;
                break;
            case W:
                moveForwardVec.x = -1.0F;
                moveForwardVec.y = 0.0F;
                break;
            case NW:
                moveForwardVec.x = -1.0F;
                moveForwardVec.y = -1.0F;
                break;
            case NE:
                moveForwardVec.x = 1.0F;
                moveForwardVec.y = -1.0F;
                break;
            case SW:
                moveForwardVec.x = -1.0F;
                moveForwardVec.y = 1.0F;
                break;
            case SE:
                moveForwardVec.x = 1.0F;
                moveForwardVec.y = 1.0F;
        }

        moveForwardVec.normalize();
        return moveForwardVec;
    }

    /**
     * Get the object's position. Stored in the supplied parameter.
     * @return The out parameter.
     */
    @Override
    public Vector3 getPosition(Vector3 out) {
        out.set(this.getX(), this.getY(), this.getZ());
        return out;
    }

    @Override
    public Vector3f getPosition(Vector3f out) {
        out.set(this.getX(), this.getY(), this.getZ());
        return out;
    }

    public Vector2 getPosition(Vector2 out) {
        out.set(this.getX(), this.getY());
        return out;
    }

    public void setPosition(float x, float y) {
        this.setX(x);
        this.setY(y);
    }

    public void setPosition(Vector2 in_pos) {
        this.setPosition(in_pos.x, in_pos.y);
    }

    public void setPosition(float x, float y, float z) {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
    }

    @Override
    public float getX() {
        return this.x;
    }

    public float setX(float x) {
        this.x = x;
        this.setNextX(x);
        this.setScriptNextX(x);
        return this.x;
    }

    public void setForceX(float x) {
        this.setX(x);
        this.setNextX(x);
        this.setLastX(x);
        this.setScriptNextX(x);
    }

    @Override
    public float getY() {
        return this.y;
    }

    public float setY(float y) {
        this.y = y;
        this.setNextY(y);
        this.setScriptNextY(y);
        return this.y;
    }

    public void setForceY(float y) {
        if (this instanceof IsoPlayer && this != IsoPlayer.getInstance()) {
            boolean var2 = false;
        }

        this.setY(y);
        this.setNextY(y);
        this.setLastY(y);
        this.setScriptNextY(y);
    }

    @Override
    public float getZ() {
        return this.z;
    }

    public float setZ(float z) {
        z = Math.max(-32.0F, z);
        z = Math.min(31.0F, z);
        this.z = z;
        this.setLastZ(z);
        return this.z;
    }

    public IsoGridSquare getMovingSquare() {
        return this.movingSq;
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.current != null ? this.current : this.square;
    }

    public IsoBuilding getCurrentBuilding() {
        if (this.current == null) {
            return null;
        } else {
            return this.current.getRoom() == null ? null : this.current.getRoom().building;
        }
    }

    public float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta) {
        return 0.0F;
    }

    public void Move(Vector2 dir) {
        this.moveWithTimeDeltaInternal(dir.x, dir.y);
    }

    public void MoveUnmodded(Vector2 dir) {
        this.moveUnmoddedInternal(dir.x, dir.y);
    }

    protected final void moveWithTimeDeltaInternal(float dirx, float diry) {
        float deltaTMultiplier = GameTime.instance.getMultiplier();
        this.moveUnmoddedInternal(dirx * deltaTMultiplier, diry * deltaTMultiplier);
    }

    protected final void moveUnmoddedInternal(float dirx, float diry) {
        this.setNextX(this.getNextX() + dirx);
        this.setNextY(this.getNextY() + diry);
        this.reqMovement.x = dirx;
        this.reqMovement.y = diry;
    }

    @Override
    public boolean isCharacter() {
        return this instanceof IsoGameCharacter;
    }

    public float DistTo(int x, int y) {
        return IsoUtils.DistanceManhatten(x, y, this.getX(), this.getY());
    }

    public float DistTo(IsoMovingObject other) {
        return other == null ? 0.0F : IsoUtils.DistanceManhatten(this.getX(), this.getY(), other.getX(), other.getY());
    }

    public float DistToProper(IsoObject other) {
        return other instanceof IsoMovingObject movingObject
            ? PZMath.sqrt(this.DistToSquared(movingObject))
            : IsoUtils.DistanceTo(this.getX(), this.getY(), other.getX(), other.getY());
    }

    public float DistToSquared(IsoMovingObject other) {
        BaseVehicle vehicleThis = Type.tryCastTo(this, BaseVehicle.class);
        BaseVehicle vehicleOther = Type.tryCastTo(other, BaseVehicle.class);
        if (vehicleThis != null && vehicleOther != null) {
            Vector2f p1 = BaseVehicle.allocVector2f();
            Vector2f p2 = BaseVehicle.allocVector2f();
            float distSq = vehicleThis.getClosestPointOnPoly(vehicleOther, p1, p2);
            BaseVehicle.releaseVector2f(p1);
            BaseVehicle.releaseVector2f(p2);
            return distSq;
        } else if (vehicleThis != null) {
            Vector2f closest = BaseVehicle.allocVector2f();
            float distSq = vehicleThis.getClosestPointOnPoly(other.getX(), other.getY(), closest);
            BaseVehicle.releaseVector2f(closest);
            return distSq;
        } else if (vehicleOther != null) {
            Vector2f closest = BaseVehicle.allocVector2f();
            float distSq = vehicleOther.getClosestPointOnPoly(this.getX(), this.getY(), closest);
            BaseVehicle.releaseVector2f(closest);
            return distSq;
        } else {
            return IsoUtils.DistanceToSquared(this.getX(), this.getY(), other.getX(), other.getY());
        }
    }

    public float DistToSquared(float x, float y) {
        return IsoUtils.DistanceToSquared(x, y, this.getX(), this.getY());
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        float offsetX = input.getFloat();
        float offsetY = input.getFloat();
        this.setX(this.setLastX(this.setNextX(this.setScriptNextX(input.getFloat() + IsoWorld.saveoffsetx * 256))));
        this.setY(this.setLastY(this.setNextY(this.setScriptNextY(input.getFloat() + IsoWorld.saveoffsety * 256))));
        this.setZ(this.setLastZ(input.getFloat()));
        this.dir = IsoDirections.fromIndex(input.getInt());
        if (input.get() != 0) {
            if (this.table == null) {
                this.table = LuaManager.platform.newTable();
            }

            this.table.load(input, WorldVersion);
        }
    }

    public String getDescription(String in_separatorStr) {
        return this.getClass().getSimpleName()
            + " ["
            + in_separatorStr
            + "offset=("
            + this.offsetX
            + ", "
            + this.offsetY
            + ") | "
            + in_separatorStr
            + "pos=("
            + this.getX()
            + ", "
            + this.getY()
            + ", "
            + this.getZ()
            + ") | "
            + in_separatorStr
            + "dir="
            + this.dir.name()
            + " ] ";
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        DebugLog.Saving.trace("Saving: %s", this);
        output.put((byte)(this.Serialize() ? 1 : 0));
        output.put(IsoObject.factoryGetClassID(this.getObjectName()));
        output.putFloat(this.offsetX);
        output.putFloat(this.offsetY);
        output.putFloat(this.getX());
        output.putFloat(this.getY());
        output.putFloat(this.getZ());
        output.putInt(this.dir.index());
        if (this.table != null && !this.table.isEmpty()) {
            output.put((byte)1);
            this.table.save(output);
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void removeFromWorld() {
        IsoCell cell = this.getCell();
        if (cell.isSafeToAdd()) {
            cell.getObjectList().remove(this);
            cell.getRemoveList().remove(this);
        } else {
            cell.getRemoveList().add(this);
        }

        cell.getAddList().remove(this);
        MovingObjectUpdateScheduler.instance.removeObject(this);
        super.removeFromWorld();
    }

    @Override
    public void removeFromSquare() {
        if (this.current != null) {
            this.current.getMovingObjects().remove(this);
        }

        if (this.last != null) {
            this.last.getMovingObjects().remove(this);
        }

        if (this.movingSq != null) {
            this.movingSq.getMovingObjects().remove(this);
        }

        this.current = this.last = this.movingSq = null;
        if (this.square != null) {
            this.square.getStaticMovingObjects().remove(this);
        }

        super.removeFromSquare();
    }

    public IsoGridSquare getFuturWalkedSquare() {
        if (this.current != null) {
            IsoGridSquare feeler = this.getFeelerTile(this.feelersize);
            if (feeler != null && feeler != this.current) {
                return feeler;
            }
        }

        return null;
    }

    public float getGlobalMovementMod() {
        return this.getGlobalMovementMod(true);
    }

    public float getGlobalMovementMod(boolean bDoNoises) {
        if (this.current != null && this.getZ() - PZMath.fastfloor(this.getZ()) < 0.5F) {
            if (this.current.has(IsoObjectType.tree) || this.current.hasBush()) {
                if (bDoNoises) {
                    this.doTreeNoises();
                }

                for (int i = 1; i < this.current.getObjects().size(); i++) {
                    IsoObject obj = this.current.getObjects().get(i);
                    if (obj instanceof IsoTree) {
                        obj.setRenderEffect(RenderEffectType.Vegetation_Rustle);
                    } else if (obj.isBush()) {
                        obj.setRenderEffect(RenderEffectType.Vegetation_Rustle);
                    }
                }
            }

            IsoGridSquare feeler = this.getFeelerTile(this.feelersize);
            if (feeler != null && feeler != this.current && (feeler.has(IsoObjectType.tree) || feeler.hasBush())) {
                if (bDoNoises) {
                    this.doTreeNoises();
                }

                for (int ix = 1; ix < feeler.getObjects().size(); ix++) {
                    IsoObject obj = feeler.getObjects().get(ix);
                    if (obj instanceof IsoTree) {
                        obj.setRenderEffect(RenderEffectType.Vegetation_Rustle);
                    } else if (obj.isBush()) {
                        obj.setRenderEffect(RenderEffectType.Vegetation_Rustle);
                    }
                }
            }
        }

        return this.current != null && this.current.HasStairs() ? 0.75F : 1.0F;
    }

    protected void doTreeNoises() {
        if (!GameServer.server) {
            if (!(this instanceof IsoPhysicsObject)) {
                if (this.current != null) {
                    if (SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 20.0F)) {
                        treeSoundMgr.addSquare(this.current);
                    }
                }
            }
        }
    }

    public void postupdate() {
        IsoGameCharacter thisChr = Type.tryCastTo(this, IsoGameCharacter.class);
        IsoPlayer thisPlayer = Type.tryCastTo(this, IsoPlayer.class);
        IsoZombie thisZombie = Type.tryCastTo(this, IsoZombie.class);
        this.slideHeadAwayFromWalls();
        if (thisPlayer != null && thisPlayer.isLocalPlayer() && !(thisPlayer instanceof IsoAnimal)) {
            IsoPlayer.setInstance(thisPlayer);
            IsoCamera.setCameraCharacter(thisPlayer);
        }

        this.ensureOnTile();
        if (this.lastTargettedBy != null && this.lastTargettedBy.isDead()) {
            this.lastTargettedBy = null;
        }

        if (this.lastTargettedBy != null && this.timeSinceZombieAttack > 120) {
            this.lastTargettedBy = null;
        }

        this.timeSinceZombieAttack++;
        if (thisPlayer != null) {
            thisPlayer.setLastCollidedW(this.collidedW);
            thisPlayer.setLastCollidedN(this.collidedN);
        }

        if (!this.destroyed) {
            this.collidedThisFrame = false;
            this.collidedN = false;
            this.collidedS = false;
            this.collidedW = false;
            this.collidedE = false;
            this.collidedWithDoor = false;
            this.last = this.current;
            this.collidedObject = null;
            this.setNextX(this.getNextX() + this.impulsex);
            this.setNextY(this.getNextY() + this.impulsey);
            tempo.set(this.getNextX() - this.getX(), this.getNextY() - this.getY());
            if (tempo.getLength() > 1.0F) {
                tempo.normalize();
                this.setNextX(this.getX() + tempo.getX());
                this.setNextY(this.getY() + tempo.getY());
            }

            this.impulsex = 0.0F;
            this.impulsey = 0.0F;
            if (thisZombie == null
                || PZMath.fastfloor(this.getZ()) != 0
                || this.getCurrentBuilding() != null
                || this.isInLoadedArea(PZMath.fastfloor(this.getNextX()), PZMath.fastfloor(this.getNextY()))
                || !thisZombie.isCurrentState(PathFindState.instance()) && !thisZombie.isCurrentState(WalkTowardState.instance())) {
                IsoAnimal thisAnimal = Type.tryCastTo(this, IsoAnimal.class);
                if (thisAnimal != null
                    && this.getZi() == 0
                    && this.getCurrentBuilding() == null
                    && !this.isInLoadedArea(this.getNextXi(), this.getNextYi())
                    && thisAnimal.isCurrentState(AnimalZoneState.instance())) {
                    AnimalPopulationManager.getInstance().virtualizeAnimal(thisAnimal);
                } else {
                    float oldNx = this.getNextX();
                    float oldNy = this.getNextY();
                    this.collidedWithVehicle = false;
                    if (thisChr != null
                        && !this.isOnFloor()
                        && thisChr.getVehicle() == null
                        && this.isCollidable()
                        && (thisPlayer == null || !thisPlayer.isNoClip())) {
                        int fromX = PZMath.fastfloor(this.getX());
                        int fromY = PZMath.fastfloor(this.getY());
                        int toX = PZMath.fastfloor(this.getNextX());
                        int toY = PZMath.fastfloor(this.getNextY());
                        int fromZ = PZMath.fastfloor(this.getZ());
                        if (thisChr.getCurrentState() == null || !thisChr.getCurrentState().isIgnoreCollide(thisChr, fromX, fromY, fromZ, toX, toY, fromZ)) {
                            Vector2f v = PolygonalMap2.instance
                                .resolveCollision(thisChr, this.getNextX(), this.getNextY(), IsoMovingObject.L_postUpdate.vector2f);
                            if (v.x != this.getNextX() || v.y != this.getNextY()) {
                                this.setNextX(v.x);
                                this.setNextY(v.y);
                                this.collidedWithVehicle = true;
                            }
                        }
                    }

                    float onx = this.getNextX();
                    float ony = this.getNextY();
                    float len = 0.0F;
                    boolean bDidCollide = false;
                    if (this.collidable) {
                        if (this.altCollide) {
                            this.DoCollide(2);
                        } else {
                            this.DoCollide(1);
                        }

                        if (this.collidedN || this.collidedS) {
                            this.setNextY(this.getLastY());
                            this.DoCollideNorS();
                        }

                        if (this.collidedW || this.collidedE) {
                            this.setNextX(this.getLastX());
                            this.DoCollideWorE();
                        }

                        if (this.altCollide) {
                            this.DoCollide(1);
                        } else {
                            this.DoCollide(2);
                        }

                        this.altCollide = !this.altCollide;
                        if (this.collidedN || this.collidedS) {
                            this.setNextY(this.getLastY());
                            this.DoCollideNorS();
                            bDidCollide = true;
                        }

                        if (this.collidedW || this.collidedE) {
                            this.setNextX(this.getLastX());
                            this.DoCollideWorE();
                            bDidCollide = true;
                        }

                        len = Math.abs(this.getNextX() - this.getLastX()) + Math.abs(this.getNextY() - this.getLastY());
                        float lnx = this.getNextX();
                        float lny = this.getNextY();
                        this.setNextX(onx);
                        this.setNextY(ony);
                        if (this.collidable && bDidCollide) {
                            if (this.altCollide) {
                                this.DoCollide(2);
                            } else {
                                this.DoCollide(1);
                            }

                            if (this.collidedN || this.collidedS) {
                                this.setNextY(this.getLastY());
                                this.DoCollideNorS();
                            }

                            if (this.collidedW || this.collidedE) {
                                this.setNextX(this.getLastX());
                                this.DoCollideWorE();
                            }

                            if (this.altCollide) {
                                this.DoCollide(1);
                            } else {
                                this.DoCollide(2);
                            }

                            if (this.collidedN || this.collidedS) {
                                this.setNextY(this.getLastY());
                                this.DoCollideNorS();
                                bDidCollide = true;
                            }

                            if (this.collidedW || this.collidedE) {
                                this.setNextX(this.getLastX());
                                this.DoCollideWorE();
                                bDidCollide = true;
                            }

                            if (Math.abs(this.getNextX() - this.getLastX()) + Math.abs(this.getNextY() - this.getLastY()) < len) {
                                this.setNextX(lnx);
                                this.setNextY(lny);
                            }
                        }
                    }

                    if (this.collidedThisFrame) {
                        this.setCurrent(this.last);
                    }

                    this.checkHitWall();
                    if (thisPlayer != null
                        && !thisPlayer.isCurrentState(CollideWithWallState.instance())
                        && !this.collidedN
                        && !this.collidedS
                        && !this.collidedW
                        && !this.collidedE) {
                        this.setCollideType(null);
                    }

                    float dx = this.getNextX() - this.getX();
                    float dy = this.getNextY() - this.getY();
                    float movementMod = !(Math.abs(dx) > 0.0F) && !(Math.abs(dy) > 0.0F) ? 0.0F : this.getGlobalMovementMod();
                    if (Math.abs(dx) > 0.01F || Math.abs(dy) > 0.01F) {
                        dx *= movementMod;
                        dy *= movementMod;
                    }

                    this.setX(this.getX() + dx);
                    this.setY(this.getY() + dy);
                    this.doStairs();
                    this.handleSlopedSurface();
                    this.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ())));
                    if (this.current == null) {
                        for (int n = PZMath.fastfloor(this.getZ()); n >= 0; n--) {
                            this.current = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), n);
                            if (this.current != null) {
                                this.snapZToCurrentSquare();
                                break;
                            }
                        }

                        if (this.current == null && this.last != null) {
                            this.setCurrent(this.last);
                            this.setX(this.setNextX(this.setScriptNextX(this.current.getX() + 0.5F)));
                            this.setY(this.setNextY(this.setScriptNextY(this.current.getY() + 0.5F)));
                        }
                    }

                    if (this.movingSq != null) {
                        this.movingSq.getMovingObjects().remove(this);
                        this.movingSq = null;
                    }

                    if (this.current != null && !this.current.getMovingObjects().contains(this)) {
                        this.current.getMovingObjects().add(this);
                        this.movingSq = this.current;
                    }

                    this.ensureOnTile();
                    this.square = this.current;
                    this.setScriptNextX(this.getNextX());
                    this.setScriptNextY(this.getNextY());
                    this.firstUpdate = false;
                }
            } else {
                ZombiePopulationManager.instance.virtualizeZombie(thisZombie);
            }
        }
    }

    protected void snapZToCurrentSquare() {
        this.setZ(this.setLastZ(PZMath.min(this.getZ(), this.current.getZ() + 0.99999F)));
    }

    protected void snapZToCurrentSquareExact() {
        this.setZ(this.current.getZ());
    }

    public void updateAnimation() {
    }

    public void ensureOnTile() {
        if (this.current == null) {
            if (!(this instanceof IsoPlayer)) {
                if (this instanceof IsoSurvivor) {
                    IsoWorld.instance.currentCell.Remove(this);
                    IsoWorld.instance.currentCell.getSurvivorList().remove(this);
                }

                return;
            }

            boolean bDo = true;
            if (this.last != null && (this.last.has(IsoObjectType.stairsTN) || this.last.has(IsoObjectType.stairsTW))) {
                this.setCurrent(this.getCell().getGridSquare(this.getXi(), this.getYi(), this.getZi() + 1));
                bDo = false;
            }

            if (this.current == null) {
                this.setCurrent(this.getCell().getGridSquare(this.getXi(), this.getYi(), this.getZi()));
                if (this.current == null) {
                    this.setCurrent(this.getCell().getGridSquare(this.getXi(), this.getYi(), this.getZi() + 1));
                    if (this.current != null) {
                        this.snapZToCurrentSquareExact();
                    }
                }

                return;
            }

            if (bDo) {
                this.setX(this.setNextX(this.setScriptNextX(this.current.getX() + 0.5F)));
                this.setY(this.setNextY(this.setScriptNextY(this.current.getY() + 0.5F)));
            }

            this.setZ(this.current.getZ());
        }
    }

    public void preupdate() {
        this.setNextX(this.getX());
        this.setNextY(this.getY());
    }

    @Override
    public void renderlast() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.isOutlineHighlight(playerIndex)) {
            if (PerformanceSettings.fboRenderChunk) {
                long timeRender = FBORenderObjectOutline.getInstance().getDuringUIRenderTime(playerIndex, this);
                long timeUpdate = FBORenderObjectOutline.getInstance().getDuringUIUpdateTime(playerIndex, this);
                if (timeRender != 0L && timeRender == UIManager.uiRenderTimeMS) {
                    return;
                }

                if (timeUpdate != 0L && timeUpdate == UIManager.uiUpdateTimeMS) {
                    return;
                }
            }

            this.setOutlineHighlight(playerIndex, false);
        }
    }

    public void spotted(IsoMovingObject other, boolean bForced) {
    }

    @Override
    public void update() {
        if (this.def == null) {
            this.def = IsoSpriteInstance.get(this.sprite);
        }

        this.movementLastFrame.x = this.getX() - this.getLastX();
        this.movementLastFrame.y = this.getY() - this.getLastY();
        this.setLastX(this.getX());
        this.setLastY(this.getY());
        this.setLastZ(this.getZ());
        this.square = this.current;
        if (this.sprite != null) {
            this.sprite.update(this.def);
        }

        this.stateEventDelayTimer = this.stateEventDelayTimer - GameTime.instance.getMultiplier();
    }

    private void Collided() {
        this.collidedThisFrame = true;
    }

    public int compareToY(IsoMovingObject other) {
        if (this.sprite == null && other.sprite == null) {
            return 0;
        } else if (this.sprite != null && other.sprite == null) {
            return -1;
        } else if (this.sprite == null) {
            return 1;
        } else {
            float sy = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
            float osy = IsoUtils.YToScreen(other.getX(), other.getY(), other.getZ(), 0);
            if (sy > osy) {
                return 1;
            } else {
                return sy < osy ? -1 : 0;
            }
        }
    }

    public float distToNearestCamCharacter() {
        float dist = Float.MAX_VALUE;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null) {
                dist = Math.min(dist, this.DistTo(player));
            }
        }

        return dist;
    }

    public boolean isSolidForSeparate() {
        if (this instanceof IsoZombieGiblets) {
            return false;
        } else if (this.current == null) {
            return false;
        } else {
            return !this.solid ? false : !this.isOnFloor();
        }
    }

    public boolean isPushableForSeparate() {
        return true;
    }

    public boolean isPushedByForSeparate(IsoMovingObject other) {
        return !(this instanceof IsoAnimal) || ((IsoAnimal)this).adef.collidable && !((IsoAnimal)this).getBehavior().blockMovement
            ? !(other instanceof IsoAnimal isoAnimal && (!isoAnimal.adef.collidable || isoAnimal.getBehavior().blockMovement))
            : false;
    }

    /**
     * Collision detection
     */
    public void separate() {
        if (this.isSolidForSeparate()) {
            if (this.isPushableForSeparate()) {
                IsoGameCharacter thisChr = Type.tryCastTo(this, IsoGameCharacter.class);
                IsoPlayer thisPlyr = Type.tryCastTo(this, IsoPlayer.class);
                IsoZombie thisZombie = Type.tryCastTo(this, IsoZombie.class);

                for (int i = 0; i <= 8; i++) {
                    IsoGridSquare sq = i == 8 ? this.current : this.current.getSurroundingSquares()[i];
                    if (sq != null && !sq.getMovingObjects().isEmpty() && (sq == this.current || !this.current.isBlockedTo(sq))) {
                        float maxWeaponRange = thisPlyr != null && thisPlyr.getPrimaryHandItem() instanceof HandWeapon
                            ? ((HandWeapon)thisPlyr.getPrimaryHandItem()).getMaxRange()
                            : 0.3F;
                        int n = 0;

                        for (int size = sq.getMovingObjects().size(); n < size; n++) {
                            IsoMovingObject obj = sq.getMovingObjects().get(n);
                            if (obj != this && obj.isSolidForSeparate() && !(Math.abs(this.getZ() - obj.getZ()) > 0.3F)) {
                                IsoGameCharacter objChr = Type.tryCastTo(obj, IsoGameCharacter.class);
                                IsoPlayer objPlyr = Type.tryCastTo(obj, IsoPlayer.class);
                                IsoZombie objZombie = Type.tryCastTo(obj, IsoZombie.class);
                                float twidth = this.width + obj.width;
                                Vector2 diff = tempo;
                                diff.x = this.getNextX() - obj.getNextX();
                                diff.y = this.getNextY() - obj.getNextY();
                                float len = diff.getLength();
                                if (thisChr == null || objChr == null && !(obj instanceof BaseVehicle)) {
                                    if (len < twidth) {
                                        CollisionManager.instance.AddContact(this, obj);
                                    }

                                    return;
                                }

                                if (objChr != null) {
                                    if (thisPlyr != null
                                        && thisPlyr.getBumpedChr() != obj
                                        && len < twidth + maxWeaponRange
                                        && thisPlyr.getForwardDirection().angleBetween(diff) > 2.6179938155736564
                                        && thisPlyr.getBeenSprintingFor() >= 70.0F
                                        && WeaponType.getWeaponType(thisPlyr) == WeaponType.SPEAR) {
                                        thisPlyr.reportEvent("ChargeSpearConnect");
                                        thisPlyr.setAttackType(AttackType.CHARGE);
                                        thisPlyr.setAttackStarted(true);
                                        thisPlyr.setVariable("StartedAttackWhileSprinting", true);
                                        thisPlyr.setBeenSprintingFor(0.0F);
                                        return;
                                    }

                                    if (!(len >= twidth)) {
                                        boolean bump = false;
                                        if (thisPlyr != null
                                            && thisPlyr.getVariableFloat("WalkSpeed", 0.0F) > 0.2F
                                            && thisPlyr.runningTime > 0.5F
                                            && thisPlyr.getBumpedChr() != obj) {
                                            bump = true;
                                        }

                                        if (GameClient.client
                                            && thisPlyr != null
                                            && objChr instanceof IsoPlayer
                                            && !ServerOptions.getInstance().playerBumpPlayer.getValue()) {
                                            bump = false;
                                        }

                                        if (thisZombie != null && thisZombie.isReanimatedForGrappleOnly()) {
                                            bump = false;
                                        }

                                        if (objZombie != null && objZombie.isReanimatedForGrappleOnly()) {
                                            bump = false;
                                        }

                                        if (bump && !thisPlyr.isAttackType(AttackType.CHARGE)) {
                                            boolean wasBumped = !this.isOnFloor()
                                                && (
                                                    thisChr.getBumpedChr() != null
                                                        || (System.currentTimeMillis() - thisPlyr.getLastBump()) / 100L < 15L
                                                        || thisPlyr.isSprinting()
                                                )
                                                && (objPlyr == null || !objPlyr.isNPC());
                                            if (wasBumped) {
                                                thisChr.bumpNbr++;
                                                int baseChance = 10 - thisChr.bumpNbr * 3;
                                                baseChance += thisChr.getPerkLevel(PerkFactory.Perks.Fitness);
                                                baseChance += thisChr.getPerkLevel(PerkFactory.Perks.Strength);
                                                baseChance -= thisChr.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 2;
                                                CharacterTraits characterTraits = thisChr.getCharacterTraits();
                                                if (characterTraits.get(CharacterTrait.CLUMSY)) {
                                                    baseChance -= 5;
                                                }

                                                if (characterTraits.get(CharacterTrait.GRACEFUL)) {
                                                    baseChance += 5;
                                                }

                                                if (characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
                                                    baseChance -= 8;
                                                }

                                                if (characterTraits.get(CharacterTrait.UNDERWEIGHT)) {
                                                    baseChance -= 4;
                                                }

                                                if (characterTraits.get(CharacterTrait.OBESE)) {
                                                    baseChance -= 8;
                                                }

                                                if (characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                                                    baseChance -= 4;
                                                }

                                                BodyPart part = thisChr.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
                                                if (part.getAdditionalPain(true) > 20.0F) {
                                                    baseChance = (int)(baseChance - (part.getAdditionalPain(true) - 20.0F) / 20.0F);
                                                }

                                                baseChance = Math.min(80, baseChance);
                                                baseChance = Math.max(1, baseChance);
                                                if (Rand.Next(baseChance) == 0 || thisChr.isSprinting()) {
                                                    thisChr.setVariable("BumpDone", false);
                                                    thisChr.setBumpFall(true);
                                                    thisChr.setVariable("TripObstacleType", "zombie");
                                                }
                                            } else {
                                                thisChr.bumpNbr = 0;
                                            }

                                            thisChr.setLastBump(System.currentTimeMillis());
                                            thisChr.setBumpedChr(objChr);
                                            thisChr.setBumpType(this.getBumpedType(objChr));
                                            boolean fromBehind = thisChr.isBehind(objChr);
                                            String zombieBump = thisChr.getBumpType();
                                            if (fromBehind) {
                                                if (zombieBump.equals("left")) {
                                                    zombieBump = "right";
                                                } else {
                                                    zombieBump = "left";
                                                }
                                            }

                                            objChr.setBumpType(zombieBump);
                                            objChr.setHitFromBehind(fromBehind);
                                            if (wasBumped | GameClient.client) {
                                                thisChr.getActionContext().reportEvent("wasBumped");
                                            }
                                        }

                                        if (GameServer.server || this.distToNearestCamCharacter() < 60.0F) {
                                            if (this instanceof IsoZombie) {
                                                ((IsoZombie)this).networkAi.wasSeparated = true;
                                            }

                                            if (this.isPushedByForSeparate(obj)) {
                                                diff.setLength((len - twidth) / 8.0F);
                                                this.setNextX(this.getNextX() - diff.x);
                                                this.setNextY(this.getNextY() - diff.y);
                                            }

                                            this.collideWith(obj);
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

    public String getBumpedType(IsoGameCharacter bumped) {
        float comparedX = this.getX() - bumped.getX();
        float comparedY = this.getY() - bumped.getY();
        String bumpType = "left";
        if (this.dir == IsoDirections.S || this.dir == IsoDirections.SE || this.dir == IsoDirections.SW) {
            if (comparedX < 0.0F) {
                bumpType = "left";
            } else {
                bumpType = "right";
            }
        }

        if (this.dir == IsoDirections.N || this.dir == IsoDirections.NE || this.dir == IsoDirections.NW) {
            if (comparedX > 0.0F) {
                bumpType = "left";
            } else {
                bumpType = "right";
            }
        }

        if (this.dir == IsoDirections.E) {
            if (comparedY > 0.0F) {
                bumpType = "left";
            } else {
                bumpType = "right";
            }
        }

        if (this.dir == IsoDirections.W) {
            if (comparedY < 0.0F) {
                bumpType = "left";
            } else {
                bumpType = "right";
            }
        }

        return bumpType;
    }

    public float getLastX() {
        return this.lastX;
    }

    public float setLastX(float lx) {
        this.lastX = lx;
        return this.lastX;
    }

    public float getLastY() {
        return this.ly;
    }

    public float setLastY(float ly) {
        this.ly = ly;
        return this.ly;
    }

    public float getLastZ() {
        return this.lz;
    }

    public float setLastZ(float lz) {
        this.lz = lz;
        return this.lz;
    }

    public float getNextX() {
        return this.nx;
    }

    public final int getNextXi() {
        return PZMath.fastfloor(this.getNextX());
    }

    public float setNextX(float nx) {
        this.nx = nx;
        return this.nx;
    }

    public float getNextY() {
        return this.ny;
    }

    public final int getNextYi() {
        return PZMath.fastfloor(this.getNextY());
    }

    public float setNextY(float ny) {
        this.ny = ny;
        return this.ny;
    }

    public float getScriptNextX() {
        return this.scriptnx;
    }

    public final int getScriptNextXi() {
        return PZMath.fastfloor(this.getScriptNextX());
    }

    public float setScriptNextX(float scriptnx) {
        this.scriptnx = scriptnx;
        return this.scriptnx;
    }

    public float getScriptNextY() {
        return this.scriptny;
    }

    public final int getScriptNextYi() {
        return PZMath.fastfloor(this.getScriptNextY());
    }

    public float setScriptNextY(float scriptny) {
        this.scriptny = scriptny;
        return this.scriptny;
    }

    protected void slideHeadAwayFromWalls() {
    }

    protected void slideAwayFromWalls(float radius) {
    }

    private boolean DoCollide(int favour) {
        IsoGameCharacter chr = Type.tryCastTo(this, IsoGameCharacter.class);
        this.setCurrentSquareFromPosition(this.getNextX(), this.getNextY());
        if (chr != null && chr.isRagdollSimulationActive()) {
            return false;
        } else {
            if (this instanceof IsoMolotovCocktail) {
                for (int zz = PZMath.fastfloor(this.getZ()); zz > 0; zz--) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            IsoGridSquare sq = this.getCell()
                                .createNewGridSquare(PZMath.fastfloor(this.getNextX()) + dx, PZMath.fastfloor(this.getNextY()) + dy, zz, false);
                            if (sq != null) {
                                sq.RecalcAllWithNeighbours(true);
                            }
                        }
                    }
                }
            }

            if (this.current != null) {
                if (!this.current.TreatAsSolidFloor()) {
                    this.setCurrentSquareFromPosition(this.getNextX(), this.getNextY(), this.getZ());
                }

                if (this.current == null) {
                    return false;
                }

                this.setCurrentSquareFromPosition(this.getNextX(), this.getNextY(), this.getZ());
            }

            if (this.current != this.last && this.last != null && this.current != null) {
                if (chr != null
                    && chr.getCurrentState() != null
                    && chr.getCurrentState().isIgnoreCollide(chr, this.last.x, this.last.y, this.last.z, this.current.x, this.current.y, this.current.z)) {
                    return false;
                }

                if (this == IsoCamera.getCameraCharacter()) {
                    IsoWorld.instance.currentCell.lightUpdateCount = 10;
                }

                int dxx = this.current.getX() - this.last.getX();
                int dy = this.current.getY() - this.last.getY();
                int dz = this.current.getZ() - this.last.getZ();
                boolean bCollide = false;
                if (this.last.testCollideAdjacent(this, dxx, dy, dz) || this.current == null) {
                    bCollide = true;
                }

                if (bCollide) {
                    if (this.last.getX() < this.current.getX()) {
                        this.collidedE = true;
                    }

                    if (this.last.getX() > this.current.getX()) {
                        this.collidedW = true;
                    }

                    if (this.last.getY() < this.current.getY()) {
                        this.collidedS = true;
                    }

                    if (this.last.getY() > this.current.getY()) {
                        this.collidedN = true;
                    }

                    this.setCurrent(this.last);
                    this.checkBreakHoppable();
                    this.checkHitHoppable();
                    this.checkBreakBendableFence(this.current);
                    if (favour == 2) {
                        if ((this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                            this.collidedS = false;
                            this.collidedN = false;
                        }
                    } else if (favour == 1 && (this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                        this.collidedW = false;
                        this.collidedE = false;
                    }

                    this.Collided();
                    return true;
                }
            } else if (this.getNextX() != this.getLastX() || this.getNextY() != this.getLastY()) {
                if (this instanceof IsoZombie && Core.gameMode.equals("Tutorial")) {
                    return true;
                }

                if (this.current == null) {
                    if (this.getNextX() < this.getLastX()) {
                        this.collidedW = true;
                    }

                    if (this.getNextX() > this.getLastX()) {
                        this.collidedE = true;
                    }

                    if (this.getNextY() < this.getLastY()) {
                        this.collidedN = true;
                    }

                    if (this.getNextY() > this.getLastY()) {
                        this.collidedS = true;
                    }

                    this.setNextX(this.getLastX());
                    this.setNextY(this.getLastY());
                    this.setCurrent(this.last);
                    this.Collided();
                    return true;
                }

                if (chr != null && chr.getPath2() != null) {
                    PathFindBehavior2 pfb2 = chr.getPathFindBehavior2();
                    if (PZMath.fastfloor(pfb2.getTargetX()) == PZMath.fastfloor(this.getX())
                        && PZMath.fastfloor(pfb2.getTargetY()) == PZMath.fastfloor(this.getY())
                        && PZMath.fastfloor(pfb2.getTargetZ()) == PZMath.fastfloor(this.getZ())) {
                        return false;
                    }
                }

                if (chr != null && chr.isSittingOnFurniture()) {
                    return false;
                }

                IsoGridSquare feeler = this.getFeelerTile(this.feelersize);
                if (chr != null) {
                    if (chr.isClimbing()) {
                        feeler = this.current;
                    }

                    if (feeler != null && feeler != this.current && chr.getPath2() != null && !chr.getPath2().crossesSquare(feeler.x, feeler.y, feeler.z)) {
                        feeler = this.current;
                    }
                }

                if (feeler != null && feeler != this.current && this.current != null) {
                    if (chr != null
                        && chr.getCurrentState() != null
                        && chr.getCurrentState().isIgnoreCollide(chr, this.current.x, this.current.y, this.current.z, feeler.x, feeler.y, feeler.z)) {
                        return false;
                    }

                    if (this.current
                        .testCollideAdjacent(
                            this, feeler.getX() - this.current.getX(), feeler.getY() - this.current.getY(), feeler.getZ() - this.current.getZ()
                        )) {
                        if (this.last != null) {
                            if (this.current.getX() < feeler.getX()) {
                                this.collidedE = true;
                            }

                            if (this.current.getX() > feeler.getX()) {
                                this.collidedW = true;
                            }

                            if (this.current.getY() < feeler.getY()) {
                                this.collidedS = true;
                            }

                            if (this.current.getY() > feeler.getY()) {
                                this.collidedN = true;
                            }

                            this.checkBreakHoppable();
                            this.checkHitHoppable();
                            this.checkBreakBendableFence(this.current);
                            if (favour == 2 && (this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                                this.collidedS = false;
                                this.collidedN = false;
                            }

                            if (favour == 1 && (this.collidedS || this.collidedN) && (this.collidedE || this.collidedW)) {
                                this.collidedW = false;
                                this.collidedE = false;
                            }
                        }

                        this.Collided();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private void checkHitHoppableAnimal(IsoAnimal animal) {
        if (animal.adef.canClimbFences) {
            if (!animal.isCurrentState(AnimalAttackState.instance()) && !animal.isCurrentState(AnimalClimbOverFenceState.instance())) {
                if (this.collidedW && !this.collidedN && !this.collidedS && this.last.has(IsoFlagType.HoppableW)) {
                    animal.climbOverFence(IsoDirections.W);
                }

                if (this.collidedN && !this.collidedE && !this.collidedW && this.last.has(IsoFlagType.HoppableN)) {
                    animal.climbOverFence(IsoDirections.N);
                }

                if (this.collidedS && !this.collidedE && !this.collidedW) {
                    IsoGridSquare s = this.last.getAdjacentSquare(IsoDirections.S);
                    if (s != null && s.has(IsoFlagType.HoppableN)) {
                        animal.climbOverFence(IsoDirections.S);
                    }
                }

                if (this.collidedE && !this.collidedN && !this.collidedS) {
                    IsoGridSquare e = this.last.getAdjacentSquare(IsoDirections.E);
                    if (e != null && e.has(IsoFlagType.HoppableW)) {
                        animal.climbOverFence(IsoDirections.E);
                    }
                }
            }
        }
    }

    private void checkHitHoppable() {
        if (this instanceof IsoAnimal animal) {
            this.checkHitHoppableAnimal(animal);
        } else {
            IsoZombie zombie = Type.tryCastTo(this, IsoZombie.class);
            if (zombie != null && !zombie.crawling) {
                if (!zombie.isCurrentState(AttackState.instance())
                    && !zombie.isCurrentState(StaggerBackState.instance())
                    && !zombie.isCurrentState(ClimbOverFenceState.instance())
                    && !zombie.isCurrentState(ClimbThroughWindowState.instance())) {
                    if (this.collidedW && !this.collidedN && !this.collidedS) {
                        IsoGridSquare w = this.last.getAdjacentSquare(IsoDirections.W);
                        if (this.last.has(IsoFlagType.HoppableW) && !this.last.HasStairsNorth() && (w == null || !w.HasStairsNorth())) {
                            zombie.climbOverFence(IsoDirections.W);
                        }
                    }

                    if (this.collidedN && !this.collidedE && !this.collidedW) {
                        IsoGridSquare n = this.last.getAdjacentSquare(IsoDirections.N);
                        if (this.last.has(IsoFlagType.HoppableN) && !this.last.HasStairsWest() && (n == null || !n.HasStairsWest())) {
                            zombie.climbOverFence(IsoDirections.N);
                        }
                    }

                    if (this.collidedS && !this.collidedE && !this.collidedW) {
                        IsoGridSquare s = this.last.getAdjacentSquare(IsoDirections.S);
                        if (s != null && s.has(IsoFlagType.HoppableN) && !this.last.HasStairsWest() && !s.HasStairsWest()) {
                            zombie.climbOverFence(IsoDirections.S);
                        }
                    }

                    if (this.collidedE && !this.collidedN && !this.collidedS) {
                        IsoGridSquare e = this.last.getAdjacentSquare(IsoDirections.E);
                        if (e != null && e.has(IsoFlagType.HoppableW) && !this.last.HasStairsNorth() && !e.HasStairsNorth()) {
                            zombie.climbOverFence(IsoDirections.E);
                        }
                    }
                }
            }
        }
    }

    private void checkBreakBendableFence(IsoGridSquare square) {
        if (this instanceof IsoZombie zombie) {
            if (!zombie.isCurrentState(AttackState.instance())
                && !zombie.isCurrentState(StaggerBackState.instance())
                && !zombie.isCurrentState(CrawlingZombieTurnState.instance())) {
                IsoDirections dir = IsoDirections.Max;
                if (this.collidedW && !this.collidedN && !this.collidedS) {
                    dir = IsoDirections.W;
                }

                if (this.collidedN && !this.collidedE && !this.collidedW) {
                    dir = IsoDirections.N;
                }

                if (this.collidedS && !this.collidedE && !this.collidedW) {
                    dir = IsoDirections.S;
                }

                if (this.collidedE && !this.collidedN && !this.collidedS) {
                    dir = IsoDirections.E;
                }

                if (dir != IsoDirections.Max) {
                    IsoObject bend = this.last.getBendableTo(this.last.getAdjacentSquare(dir));
                    IsoThumpable thumpable = Type.tryCastTo(bend, IsoThumpable.class);
                    if (BentFences.getInstance().isEnabled()) {
                        if (thumpable != null && !thumpable.isThumpable()) {
                            zombie.setThumpTarget(thumpable);
                        } else if (bend != null && bend.getThumpableFor(zombie) != null) {
                            zombie.setThumpTarget(bend);
                        }
                    }
                }
            }
        }
    }

    private void checkBreakHoppable() {
        IsoZombie zombie = Type.tryCastTo(this, IsoZombie.class);
        if (zombie != null && zombie.crawling) {
            if (!zombie.isCurrentState(AttackState.instance())
                && !zombie.isCurrentState(StaggerBackState.instance())
                && !zombie.isCurrentState(CrawlingZombieTurnState.instance())) {
                IsoDirections dir = IsoDirections.Max;
                if (this.collidedW && !this.collidedN && !this.collidedS) {
                    dir = IsoDirections.W;
                }

                if (this.collidedN && !this.collidedE && !this.collidedW) {
                    dir = IsoDirections.N;
                }

                if (this.collidedS && !this.collidedE && !this.collidedW) {
                    dir = IsoDirections.S;
                }

                if (this.collidedE && !this.collidedN && !this.collidedS) {
                    dir = IsoDirections.E;
                }

                if (dir != IsoDirections.Max) {
                    IsoObject hop = this.last.getHoppableTo(this.last.getAdjacentSquare(dir));
                    IsoThumpable thumpable = Type.tryCastTo(hop, IsoThumpable.class);
                    if (thumpable != null && !thumpable.isThumpable()) {
                        zombie.setThumpTarget(thumpable);
                    } else if (hop != null && hop.getThumpableFor(zombie) != null) {
                        zombie.setThumpTarget(hop);
                    }
                }
            }
        }
    }

    private void checkHitWall() {
        if (this.collidedN || this.collidedS || this.collidedE || this.collidedW) {
            if (this.current != null) {
                if (this instanceof IsoPlayer player) {
                    if (StringUtils.isNullOrEmpty(this.getCollideType())) {
                        boolean valid = false;
                        int wallType = this.current.getWallType();
                        if (this.isCollidedWithDoor()
                            && this.getCollidedObject() instanceof IsoDoor door
                            && (door.north && (this.collidedN || this.collidedS) || !door.north && (this.collidedE || this.collidedW))) {
                            valid = true;
                        }

                        if ((wallType & 1) != 0 && this.collidedN && this.getDir() == IsoDirections.N) {
                            valid = true;
                        }

                        if ((wallType & 2) != 0 && this.collidedS && this.getDir() == IsoDirections.S) {
                            valid = true;
                        }

                        if ((wallType & 4) != 0 && this.collidedW && this.getDir() == IsoDirections.W) {
                            valid = true;
                        }

                        if ((wallType & 8) != 0 && this.collidedE && this.getDir() == IsoDirections.E) {
                            valid = true;
                        }

                        if (this.checkVaultOver()) {
                            valid = false;
                        }

                        if (valid && player.isSprinting() && player.isLocalPlayer()) {
                            this.setCollideType("wall");
                            player.getActionContext().reportEvent("collideWithWall");
                            this.lastCollideTime = 70.0F;
                        }
                    }
                }
            }
        }
    }

    private boolean checkVaultOver() {
        IsoPlayer player = (IsoPlayer)this;
        if (player.isCurrentState(ClimbOverFenceState.instance()) || player.isIgnoreAutoVault()) {
            return false;
        } else if (!player.IsRunning() && !player.isSprinting() && !player.isRemoteAndHasObstacleOnPath()) {
            return false;
        } else {
            IsoDirections dir = this.getDir();
            IsoGridSquare se = this.current.getAdjacentSquare(IsoDirections.SE);
            if (dir == IsoDirections.SE && se != null && se.has(IsoFlagType.HoppableN) && se.has(IsoFlagType.HoppableW)) {
                return false;
            } else {
                IsoGridSquare feeler = this.current;
                if (this.collidedS) {
                    feeler = this.current.getAdjacentSquare(IsoDirections.S);
                } else if (this.collidedE) {
                    feeler = this.current.getAdjacentSquare(IsoDirections.E);
                }

                if (feeler == null) {
                    return false;
                } else {
                    boolean vaultOver = false;
                    if (this.current.getProperties().has(IsoFlagType.HoppableN)
                        && this.collidedN
                        && !this.collidedW
                        && !this.collidedE
                        && (dir == IsoDirections.NW || dir == IsoDirections.N || dir == IsoDirections.NE)) {
                        dir = IsoDirections.N;
                        vaultOver = true;
                    }

                    if (feeler.getProperties().has(IsoFlagType.HoppableN)
                        && this.collidedS
                        && !this.collidedW
                        && !this.collidedE
                        && (dir == IsoDirections.SW || dir == IsoDirections.S || dir == IsoDirections.SE)) {
                        dir = IsoDirections.S;
                        vaultOver = true;
                    }

                    if (this.current.getProperties().has(IsoFlagType.HoppableW)
                        && this.collidedW
                        && !this.collidedN
                        && !this.collidedS
                        && (dir == IsoDirections.NW || dir == IsoDirections.W || dir == IsoDirections.SW)) {
                        dir = IsoDirections.W;
                        vaultOver = true;
                    }

                    if (feeler.getProperties().has(IsoFlagType.HoppableW)
                        && this.collidedE
                        && !this.collidedN
                        && !this.collidedS
                        && (dir == IsoDirections.NE || dir == IsoDirections.E || dir == IsoDirections.SE)) {
                        dir = IsoDirections.E;
                        vaultOver = true;
                    }

                    if (!this.current.isPlayerAbleToHopWallTo(dir, feeler)) {
                        return false;
                    } else if (vaultOver && player.isSafeToClimbOver(dir)) {
                        ClimbOverFenceState.instance().setParams(player, dir);
                        player.getActionContext().reportEvent("EventClimbFence");
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    public void setMovingSquareNow() {
        if (this.movingSq != null) {
            this.movingSq.getMovingObjects().remove(this);
            this.movingSq = null;
        }

        if (this.current != null && !this.current.getMovingObjects().contains(this)) {
            this.current.getMovingObjects().add(this);
            this.movingSq = this.current;
        }
    }

    public IsoGridSquare getFeelerTile(float dist) {
        Vector2 vec = tempo;
        vec.x = this.getNextX() - this.getLastX();
        vec.y = this.getNextY() - this.getLastY();
        vec.setLength(dist);
        return this.getCell().getGridSquare(PZMath.fastfloor(this.getX() + vec.x), PZMath.fastfloor(this.getY() + vec.y), PZMath.fastfloor(this.getZ()));
    }

    public void DoCollideNorS() {
        this.setNextY(this.getLastY());
    }

    public void DoCollideWorE() {
        this.setNextX(this.getLastX());
    }

    /**
     * @return the TimeSinceZombieAttack
     */
    public int getTimeSinceZombieAttack() {
        return this.timeSinceZombieAttack;
    }

    /**
     * 
     * @param TimeSinceZombieAttack the TimeSinceZombieAttack to set
     */
    public void setTimeSinceZombieAttack(int TimeSinceZombieAttack) {
        this.timeSinceZombieAttack = TimeSinceZombieAttack;
    }

    /**
     * @return the collidedE
     */
    public boolean isCollidedE() {
        return this.collidedE;
    }

    /**
     * 
     * @param collidedE the collidedE to set
     */
    public void setCollidedE(boolean collidedE) {
        this.collidedE = collidedE;
    }

    /**
     * @return the collidedN
     */
    public boolean isCollidedN() {
        return this.collidedN;
    }

    /**
     * 
     * @param collidedN the collidedN to set
     */
    public void setCollidedN(boolean collidedN) {
        this.collidedN = collidedN;
    }

    /**
     * @return the CollidedObject
     */
    public IsoObject getCollidedObject() {
        return this.collidedObject;
    }

    /**
     * 
     * @param CollidedObject the CollidedObject to set
     */
    public void setCollidedObject(IsoObject CollidedObject) {
        this.collidedObject = CollidedObject;
    }

    /**
     * @return the collidedS
     */
    public boolean isCollidedS() {
        return this.collidedS;
    }

    /**
     * 
     * @param collidedS the collidedS to set
     */
    public void setCollidedS(boolean collidedS) {
        this.collidedS = collidedS;
    }

    /**
     * @return the collidedThisFrame
     */
    public boolean isCollidedThisFrame() {
        return this.collidedThisFrame;
    }

    /**
     * 
     * @param collidedThisFrame the collidedThisFrame to set
     */
    public void setCollidedThisFrame(boolean collidedThisFrame) {
        this.collidedThisFrame = collidedThisFrame;
    }

    /**
     * @return the collidedW
     */
    public boolean isCollidedW() {
        return this.collidedW;
    }

    /**
     * 
     * @param collidedW the collidedW to set
     */
    public void setCollidedW(boolean collidedW) {
        this.collidedW = collidedW;
    }

    /**
     * @return the CollidedWithDoor
     */
    public boolean isCollidedWithDoor() {
        return this.collidedWithDoor;
    }

    /**
     * 
     * @param CollidedWithDoor the CollidedWithDoor to set
     */
    public void setCollidedWithDoor(boolean CollidedWithDoor) {
        this.collidedWithDoor = CollidedWithDoor;
    }

    public boolean isCollidedWithVehicle() {
        return this.collidedWithVehicle;
    }

    /**
     * @return the current
     */
    public IsoGridSquare getCurrentSquare() {
        return this.current;
    }

    public Zone getCurrentZone() {
        return this.current != null ? this.current.getZone() : null;
    }

    /**
     * 
     * @param current the current to set
     */
    public void setCurrent(IsoGridSquare current) {
        this.current = current;
    }

    public void setCurrentSquareFromPosition() {
        float x1 = this.getX();
        float y1 = this.getY();
        float z1 = this.getZ();
        this.setCurrentSquareFromPosition(x1, y1, z1);
    }

    public void setCurrentSquareFromPosition(float x1, float y1) {
        float z1 = this.getZ();
        this.setCurrentSquareFromPosition(x1, y1, z1);
    }

    public void setCurrentSquareFromPosition(float x1, float y1, float z1) {
        IsoGridSquare current = this.getCell().getGridSquare((double)x1, (double)y1, (double)z1);
        if (current == null) {
            for (int n = PZMath.fastfloor(z1); n >= 0; n--) {
                current = this.getCell().getGridSquare((double)x1, (double)y1, (double)n);
                if (current != null) {
                    break;
                }
            }
        }

        this.setCurrent(current);
    }

    /**
     * @return the destroyed
     */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /**
     * 
     * @param destroyed the destroyed to set
     */
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    /**
     * @return the firstUpdate
     */
    public boolean isFirstUpdate() {
        return this.firstUpdate;
    }

    /**
     * 
     * @param firstUpdate the firstUpdate to set
     */
    public void setFirstUpdate(boolean firstUpdate) {
        this.firstUpdate = firstUpdate;
    }

    /**
     * @return the hitDir
     */
    public Vector2 getHitDir() {
        return this.hitDir;
    }

    /**
     * 
     * @param hitDir the hitDir to set
     */
    public void setHitDir(Vector2 hitDir) {
        this.hitDir.set(hitDir);
    }

    /**
     * @return the impulsex
     */
    public float getImpulsex() {
        return this.impulsex;
    }

    /**
     * 
     * @param impulsex the impulsex to set
     */
    public void setImpulsex(float impulsex) {
        this.impulsex = impulsex;
    }

    /**
     * @return the impulsey
     */
    public float getImpulsey() {
        return this.impulsey;
    }

    /**
     * 
     * @param impulsey the impulsey to set
     */
    public void setImpulsey(float impulsey) {
        this.impulsey = impulsey;
    }

    /**
     * @return the limpulsex
     */
    public float getLimpulsex() {
        return this.limpulsex;
    }

    /**
     * 
     * @param limpulsex the limpulsex to set
     */
    public void setLimpulsex(float limpulsex) {
        this.limpulsex = limpulsex;
    }

    /**
     * @return the limpulsey
     */
    public float getLimpulsey() {
        return this.limpulsey;
    }

    /**
     * 
     * @param limpulsey the limpulsey to set
     */
    public void setLimpulsey(float limpulsey) {
        this.limpulsey = limpulsey;
    }

    /**
     * @return the hitForce
     */
    public float getHitForce() {
        return this.hitForce;
    }

    /**
     * 
     * @param hitForce the hitForce to set
     */
    public void setHitForce(float hitForce) {
        this.hitForce = hitForce;
    }

    /**
     * @return the hitFromAngle
     */
    public float getHitFromAngle() {
        return this.hitFromAngle;
    }

    /**
     * 
     * @param hitFromAngle the hitFromAngle to set
     */
    public void setHitFromAngle(float hitFromAngle) {
        this.hitFromAngle = hitFromAngle;
    }

    /**
     * @return the last
     */
    public IsoGridSquare getLastSquare() {
        return this.last;
    }

    /**
     * 
     * @param last the last to set
     */
    public void setLast(IsoGridSquare last) {
        this.last = last;
    }

    /**
     * @return whether the object should take damage or not.
     */
    public boolean getNoDamage() {
        return this.noDamage;
    }

    /**
     * 
     * @param dmg whether the object should take damage.
     */
    public void setNoDamage(boolean dmg) {
        this.noDamage = dmg;
    }

    /**
     * @return the solid
     */
    public boolean isSolid() {
        return this.solid;
    }

    /**
     * 
     * @param solid the solid to set
     */
    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    /**
     * @return the StateEventDelayTimer
     */
    public float getStateEventDelayTimer() {
        return this.stateEventDelayTimer;
    }

    /**
     * 
     * @param StateEventDelayTimer the StateEventDelayTimer to set
     */
    public void setStateEventDelayTimer(float StateEventDelayTimer) {
        this.stateEventDelayTimer = StateEventDelayTimer;
    }

    /**
     * @return the width
     */
    public float getWidth() {
        return this.width;
    }

    /**
     * 
     * @param width the width to set
     */
    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * @return the bAltCollide
     */
    public boolean isbAltCollide() {
        return this.altCollide;
    }

    /**
     * 
     * @param altCollide the bAltCollide to set
     */
    public void setbAltCollide(boolean altCollide) {
        this.altCollide = altCollide;
    }

    /**
     * @return the shootable
     */
    public boolean isShootable() {
        return this.shootable;
    }

    /**
     * 
     * @param shootable the shootable to set
     */
    public void setShootable(boolean shootable) {
        this.shootable = shootable;
    }

    /**
     * @return the lastTargettedBy
     */
    public IsoZombie getLastTargettedBy() {
        return this.lastTargettedBy;
    }

    /**
     * 
     * @param lastTargettedBy the lastTargettedBy to set
     */
    public void setLastTargettedBy(IsoZombie lastTargettedBy) {
        this.lastTargettedBy = lastTargettedBy;
    }

    /**
     * @return the Collidable
     */
    public boolean isCollidable() {
        return this.collidable;
    }

    /**
     * 
     * @param Collidable the Collidable to set
     */
    public void setCollidable(boolean Collidable) {
        this.collidable = Collidable;
    }

    /**
     * @return the scriptnx
     */
    public float getScriptnx() {
        return this.getScriptNextX();
    }

    /**
     * 
     * @param scriptnx the scriptnx to set
     */
    public void setScriptnx(float scriptnx) {
        this.setScriptNextX(scriptnx);
    }

    /**
     * @return the scriptny
     */
    public float getScriptny() {
        return this.getScriptNextY();
    }

    /**
     * 
     * @param scriptny the scriptny to set
     */
    public void setScriptny(float scriptny) {
        this.setScriptNextY(scriptny);
    }

    /**
     * @return the ScriptModule
     */
    public String getScriptModule() {
        return this.scriptModule;
    }

    /**
     * 
     * @param ScriptModule the ScriptModule to set
     */
    public void setScriptModule(String ScriptModule) {
        this.scriptModule = ScriptModule;
    }

    /**
     * @return the movementLastFrame
     */
    public Vector2 getMovementLastFrame() {
        return this.movementLastFrame;
    }

    /**
     * 
     * @param movementLastFrame the movementLastFrame to set
     */
    public void setMovementLastFrame(Vector2 movementLastFrame) {
        this.movementLastFrame = movementLastFrame;
    }

    /**
     * @return the feelersize
     */
    public float getFeelersize() {
        return this.feelersize;
    }

    /**
     * 
     * @param feelersize the feelersize to set
     */
    public void setFeelersize(float feelersize) {
        this.feelersize = feelersize;
    }

    /**
     * This function calculate count of attackers
     * @return 0 - no attackets, 1 - one player can attack this character, 2 - multiply players can attack this character
     */
    public byte canHaveMultipleHits() {
        byte numberOfPossibleAttackers = 0;
        ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();

        for (int i = 0; i < objects.size(); i++) {
            IsoMovingObject mov = objects.get(i);
            if (mov instanceof IsoPlayer chr) {
                HandWeapon weapon = Type.tryCastTo(chr.getPrimaryHandItem(), HandWeapon.class);
                if (weapon == null || chr.isDoShove()) {
                    weapon = chr.bareHands;
                }

                float distA = IsoUtils.DistanceTo(chr.getX(), chr.getY(), this.getX(), this.getY());
                float weaponRange = weapon.getMaxRange() * weapon.getRangeMod(chr) + 2.0F;
                if (!(distA > weaponRange)) {
                    float dot = chr.getDotWithForwardDirection(this.getX(), this.getY());
                    if (!(distA > 2.5F) || !(dot < 0.1F)) {
                        LosUtil.TestResults testResults = LosUtil.lineClear(
                            chr.getCell(),
                            PZMath.fastfloor(chr.getX()),
                            PZMath.fastfloor(chr.getY()),
                            PZMath.fastfloor(chr.getZ()),
                            PZMath.fastfloor(this.getX()),
                            PZMath.fastfloor(this.getY()),
                            PZMath.fastfloor(this.getZ()),
                            false
                        );
                        if (testResults != LosUtil.TestResults.Blocked && testResults != LosUtil.TestResults.ClearThroughClosedDoor) {
                            if (++numberOfPossibleAttackers >= 2) {
                                return numberOfPossibleAttackers;
                            }
                        }
                    }
                }
            }
        }

        return numberOfPossibleAttackers;
    }

    public boolean isOnFloor() {
        return this.onFloor;
    }

    public void setOnFloor(boolean bOnFloor) {
        this.onFloor = bOnFloor;
    }

    public final boolean isStanding() {
        return !this.isProne();
    }

    public boolean isProne() {
        return this.isOnFloor();
    }

    public boolean isGettingUp() {
        return false;
    }

    public boolean isCrawling() {
        return false;
    }

    public void Despawn() {
    }

    public boolean isCloseKilled() {
        return this.closeKilled;
    }

    public void setCloseKilled(boolean closeKilled) {
        this.closeKilled = closeKilled;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        pos.set(this.getX(), this.getY());
        return pos;
    }

    private boolean isInLoadedArea(int x, int y) {
        if (GameServer.server) {
            for (int i = 0; i < ServerMap.instance.loadedCells.size(); i++) {
                ServerMap.ServerCell serverCell = ServerMap.instance.loadedCells.get(i);
                if (x >= serverCell.wx * 64 && x < (serverCell.wx + 1) * 64 && y >= serverCell.wy * 64 && y < (serverCell.wy + 1) * 64) {
                    return true;
                }
            }
        } else {
            for (int ix = 0; ix < IsoPlayer.numPlayers; ix++) {
                IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[ix];
                if (!cm.ignore && x >= cm.getWorldXMinTiles() && x < cm.getWorldXMaxTiles() && y >= cm.getWorldYMinTiles() && y < cm.getWorldYMaxTiles()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isCollided() {
        return !StringUtils.isNullOrWhitespace(this.getCollideType());
    }

    public String getCollideType() {
        return this.collideType;
    }

    public void setCollideType(String collideType) {
        this.collideType = collideType;
    }

    public float getLastCollideTime() {
        return this.lastCollideTime;
    }

    public void setLastCollideTime(float lastCollideTime) {
        this.lastCollideTime = lastCollideTime;
    }

    public ArrayList<IsoZombie> getEatingZombies() {
        return this.eatingZombies;
    }

    public void setEatingZombies(ArrayList<IsoZombie> zeds) {
        this.eatingZombies.clear();
        this.eatingZombies.addAll(zeds);
    }

    public boolean isEatingOther(IsoMovingObject other) {
        return other == null ? false : other.eatingZombies.contains(this);
    }

    public float getDistanceSq(IsoMovingObject other) {
        float Xsq = this.getX() - other.getX();
        float Ysq = this.getY() - other.getY();
        Xsq *= Xsq;
        Ysq *= Ysq;
        return Xsq + Ysq;
    }

    @Override
    public boolean isExistInTheWorld() {
        return this.square != null ? this.square.getMovingObjects().contains(this) : false;
    }

    public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare square) {
        return false;
    }

    public int getSurroundingThumpers() {
        return this.getCurrentSquare() != null
            ? (int)this.getCurrentSquare()
                .getCell()
                .getZombieList()
                .stream()
                .filter(z -> IsoUtils.DistanceTo(this.getX(), this.getY(), z.getX(), z.getY()) < 1.5F)
                .count()
            : 1;
    }

    private static final class L_postUpdate {
        private static final Vector2f vector2f = new Vector2f();
    }
}
