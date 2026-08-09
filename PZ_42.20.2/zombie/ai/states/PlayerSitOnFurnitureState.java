// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import org.joml.Vector3f;
import zombie.AttackType;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.seating.SeatingManager;
import zombie.util.StringUtils;

@UsedFromLua
public final class PlayerSitOnFurnitureState extends State {
    private static final PlayerSitOnFurnitureState INSTANCE = new PlayerSitOnFurnitureState();
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();
    public static final State.Param<IsoDirections> DIR = State.Param.of("dir", IsoDirections.class, IsoDirections.N);
    public static final State.Param<IsoObject> SIT_OBJECT = State.Param.of("sit_object", IsoObject.class);
    public static final State.Param<String> BEFORE_SIT_DIR = State.Param.ofString("before_sit_dir", "Front");

    public static PlayerSitOnFurnitureState instance() {
        return INSTANCE;
    }

    private PlayerSitOnFurnitureState() {
        super(true, true, true, true);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreAimingInput(true);
        owner.setHeadLookAround(true);
        owner.setSittingOnFurniture(true);
        owner.clearVariable("forceGetUp");
        if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
            owner.setHideWeaponModel(true);
        }

        if (owner.getStateMachine().getPrevious() == IdleState.instance()) {
            owner.clearVariable("SitOnFurnitureAnim");
            owner.clearVariable("SitOnFurnitureStarted");
        }

        IsoObject object = owner.getSitOnFurnitureObject();
        Vector3f worldPos = new Vector3f();
        String sitDirection = owner.getSitOnFurnitureDirection().name();
        String sitOnFurnitureDirection = owner.getVariableString("SitOnFurnitureDirection");
        String animNodeName = "SitOnFurniture" + sitOnFurnitureDirection;
        boolean valid = SeatingManager.getInstance()
            .getAdjacentPosition(owner, object, sitDirection, sitOnFurnitureDirection, "sitonfurniture", animNodeName, worldPos);
        if (valid) {
            owner.setX(worldPos.x);
            owner.setY(worldPos.y);
            IsoDirections objectDirection = IsoDirections.fromString(sitDirection);

            IsoDirections beforeSitDirection = switch (sitOnFurnitureDirection) {
                case "Front" -> objectDirection;
                case "Left" -> objectDirection.RotRight(2);
                case "Right" -> objectDirection.RotLeft(2);
                default -> objectDirection;
            };
            owner.getAnimationPlayer().setTargetAndCurrentDirection(beforeSitDirection.ToVector());
        }

        if (owner.isLocal()) {
            this.setParams(owner, State.Stage.Enter);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false)) {
            owner.StopAllActionQueue();
            owner.setVariable("forceGetUp", true);
        } else {
            IsoObject furniture = owner.getSitOnFurnitureObject();
            if (furniture == null || furniture.getObjectIndex() == -1) {
                this.getUp(owner);
            } else if (!owner.isInvisible() && this.isVisibleZombieNearby(owner)) {
                this.getUp(owner);
            } else if (furniture.isFurnitureOccupied(owner)) {
                this.getUp(owner);
            } else {
                if (owner.getVariableBoolean("SitOnFurnitureStarted")) {
                    owner.setVariable("SitOnFurnitureAnim", "Idle");
                }

                IsoObject object = player.getSitOnFurnitureObject();
                IsoDirections sitDir = player.getSitOnFurnitureDirection();
                player.setInitiateAttack(false);
                player.setAttackStarted(false);
                player.setAttackType(AttackType.NONE);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        if (StringUtils.isNullOrEmpty(owner.getVariableString("HitReaction"))) {
            owner.clearVariable("forceGetUp");
            owner.clearVariable("SitOnFurnitureAnim");
            owner.clearVariable("SitOnFurnitureStarted");
            owner.setIgnoreMovement(false);
        } else if ("hitreaction".equalsIgnoreCase(owner.getCurrentActionContextStateName())) {
            this.abortSitting(owner);
        } else if ("hitreactionpvp".equalsIgnoreCase(owner.getCurrentActionContextStateName())) {
            this.abortSitting(owner);
        }

        owner.setIgnoreAimingInput(false);
        owner.setHeadLookAround(false);
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("SitOnFurnitureStarted")) {
            owner.setVariable("SitOnFurnitureStarted", true);
        } else {
            if (event.eventName.equalsIgnoreCase("PlaySitDownSound")) {
                IsoObject object = owner.getSitOnFurnitureObject();
                if (object != null && object.getProperties().has("SeatMaterial")) {
                    String soundSuffix = object.getProperties().get("SeatMaterial");
                    owner.playSoundLocal("SitDown" + soundSuffix);
                } else if (object != null) {
                    owner.playSoundLocal("SitDownFabric");
                }
            }
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(DIR, owner.getSitOnFurnitureDirection());
            owner.set(SIT_OBJECT, owner.getSitOnFurnitureObject());
            owner.set(BEFORE_SIT_DIR, owner.getVariableString("SitOnFurnitureDirection"));
        } else {
            owner.setSitOnFurnitureDirection(owner.get(DIR));
            owner.setSitOnFurnitureObject(owner.get(SIT_OBJECT));
            owner.setVariable("SitOnFurnitureDirection", owner.get(BEFORE_SIT_DIR));
            owner.faceDirection(owner.getSitOnFurnitureDirection());
            if (stage == State.Stage.Enter) {
                owner.setMoving(false);
            }
        }

        super.setParams(owner, stage);
    }

    public void abortSitting(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        owner.setIgnoreAimingInput(false);
        owner.setIgnoreMovement(false);
        owner.clearVariable("forceGetUp");
        owner.clearVariable("SitOnFurnitureAnim");
        owner.clearVariable("SitOnFurnitureStarted");
        IsoObject object = owner.getSitOnFurnitureObject();
        if (object != null) {
            object.setSatChair(false);
        }

        owner.setOnBed(false);
        owner.setSittingOnFurniture(false);
        owner.setSitOnFurnitureObject(null);
        owner.setSitOnFurnitureDirection(null);
    }

    private boolean isVisibleZombieNearby(IsoGameCharacter owner) {
        if (!IsoPlayer.isLocalPlayer(owner)) {
            return false;
        } else {
            int x = PZMath.fastfloor(owner.getX());
            int y = PZMath.fastfloor(owner.getY());
            int z = PZMath.fastfloor(owner.getZ());
            int playerIndex = ((IsoPlayer)owner).getIndex();

            for (int i = 0; i < DIRECTIONS.length; i++) {
                IsoDirections dir = DIRECTIONS[i];
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x + dir.dx(), y + dir.dy(), z);
                if (square != null) {
                    int j = 0;

                    for (int n = square.getMovingObjects().size(); j < n; j++) {
                        if (square.getMovingObjects().get(j) instanceof IsoZombie zombie
                            && !zombie.isReanimatedForGrappleOnly()
                            && square.isCanSee(playerIndex)
                            && zombie.getTargetAlpha(playerIndex) > 0.0F) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    private void getUp(IsoGameCharacter owner) {
        owner.StopAllActionQueue();
        owner.setVariable("forceGetUp", true);
        owner.setVariable("pressedRunButton", true);
        owner.setVariable("getUpQuick", true);
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setSittingOnFurniture(true);
        owner.setSitOnFurnitureDirection(DIR.fromDelegate(delegate));
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setSittingOnFurniture(false);
    }

    @Override
    public float awayCheckDistance() {
        return 0.8F;
    }
}
