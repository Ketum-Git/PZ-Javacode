// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.SoundManager;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class GrappledThrownOverFenceState extends State {
    private static final GrappledThrownOverFenceState INSTANCE = new GrappledThrownOverFenceState();
    public static final State.Param<Integer> START_X = State.Param.ofInt("start_x", 0);
    public static final State.Param<Integer> START_Y = State.Param.ofInt("start_y", 0);
    public static final State.Param<Integer> Z = State.Param.ofInt("z", 0);
    public static final State.Param<Integer> END_X = State.Param.ofInt("end_x", 0);
    public static final State.Param<Integer> END_Y = State.Param.ofInt("end_y", 0);
    public static final State.Param<IsoDirections> DIR = State.Param.of("dir", IsoDirections.class);
    public static final State.Param<Boolean> ZOMBIE_ON_FLOOR = State.Param.ofBool("zombie_on_floor", false);
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);
    public static final State.Param<Boolean> SCRATCH = State.Param.ofBool("scratch", false);
    public static final State.Param<Boolean> COUNTER = State.Param.ofBool("counter", false);
    public static final State.Param<Boolean> SOLID_FLOOR = State.Param.ofBool("solid_floor", false);
    public static final State.Param<Boolean> SHEET_ROPE = State.Param.ofBool("sheet_rope", false);
    public static final State.Param<Boolean> RUN = State.Param.ofBool("run", false);
    public static final State.Param<Boolean> SPRINT = State.Param.ofBool("sprint", false);
    public static final State.Param<Boolean> COLLIDABLE = State.Param.ofBool("collidable", false);
    static final int FENCE_TYPE_WOOD = 0;
    static final int FENCE_TYPE_METAL = 1;
    static final int FENCE_TYPE_SANDBAG = 2;
    static final int FENCE_TYPE_GRAVELBAG = 3;
    static final int FENCE_TYPE_BARBWIRE = 4;
    static final int FENCE_TYPE_ROADBLOCK = 5;
    static final int FENCE_TYPE_METAL_BARS = 6;
    static final int TRIP_WOOD = 0;
    static final int TRIP_METAL = 1;
    static final int TRIP_SANDBAG = 2;
    static final int TRIP_GRAVELBAG = 3;
    static final int TRIP_BARBWIRE = 4;
    public static final int TRIP_METAL_BARS = 8;

    public static GrappledThrownOverFenceState instance() {
        return INSTANCE;
    }

    private GrappledThrownOverFenceState() {
        super(true, false, true, false);
        this.addAnimEventListener("PlayFenceSound", this::OnAnimEvent_PlayFenceSound);
        this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setVariable("ClimbingFence", true);
        owner.setVariable("ClimbFenceStarted", false);
        owner.setVariable("ClimbFenceFinished", false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoDirections dir = owner.get(DIR);
        if (dir != null) {
            owner.setAnimated(true);
            if (owner.isFallOnFront()) {
                owner.setDir(dir.Rot180());
            } else {
                owner.setDir(dir);
            }

            if (owner.getVariableBoolean("ClimbFenceStarted")) {
                float x = owner.get(START_X).intValue();
                float y = owner.get(START_Y).intValue();
                switch (dir) {
                    case N:
                        y -= 0.1F;
                        break;
                    case S:
                        y++;
                        break;
                    case W:
                        x -= 0.1F;
                        break;
                    case E:
                        x++;
                }

                if (owner.getXi() == PZMath.fastfloor(x) || dir != IsoDirections.W && dir != IsoDirections.E) {
                    if (owner.getXi() == owner.get(END_X)) {
                        owner.setNextX(PZMath.clamp(owner.getNextX(), owner.get(END_X).intValue() + 0.01F, owner.get(END_X).intValue() + 0.99F));
                    }
                } else {
                    this.slideX(owner, x);
                }

                if (owner.getYi() == PZMath.fastfloor(y) || dir != IsoDirections.N && dir != IsoDirections.S) {
                    if (owner.getYi() == owner.get(END_Y)) {
                        owner.setNextY(PZMath.clamp(owner.getNextY(), owner.get(END_Y).intValue() + 0.01F, owner.get(END_Y).intValue() + 0.99F));
                    }
                } else {
                    this.slideY(owner, y);
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("ClimbingFence");
        owner.clearVariable("ClimbFenceFinished");
        owner.clearVariable("ClimbFenceOutcome");
        owner.clearVariable("ClimbFenceStarted");
        owner.clearVariable("ClimbFenceFlopped");
        owner.clearVariable("PlayerVoiceSound");
        owner.ClearVariable("VaultOverSprint");
        owner.ClearVariable("VaultOverRun");
        if (owner.hasAnimationPlayer() && owner.getAnimationPlayer().isReady()) {
            if (owner.get(DIR).dy() != 0) {
                owner.setX(owner.get(END_X).intValue() + 0.5F);
                owner.setY(PZMath.clamp(owner.getY(), owner.get(END_Y).intValue() + 0.01F, owner.get(END_Y).intValue() + 0.99F));
            } else {
                owner.setX(PZMath.clamp(owner.getX(), owner.get(END_X).intValue() + 0.01F, owner.get(END_X).intValue() + 0.99F));
                owner.setY(owner.get(END_Y).intValue() + 0.5F);
            }

            owner.getAnimationPlayer().setTargetAndCurrentDirection(owner.getForwardDirection());
        }

        owner.setIgnoreMovement(false);
        owner.setForwardDirectionFromAnimAngle();
    }

    private void OnAnimEvent_PlayerVoiceSound(IsoGameCharacter owner, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (!owner.getVariableBoolean("PlayerVoiceSound")) {
            if (player != null) {
                owner.setVariable("PlayerVoiceSound", true);
                player.playerVoiceSound(event.parameterValue);
            }
        }
    }

    private void OnAnimEvent_PlayFenceSound(IsoGameCharacter owner, AnimEvent event) {
        if (SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0F)) {
            IsoObject fence = this.getFence(owner);
            if (fence != null) {
                if (owner instanceof IsoZombie) {
                    long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                    int tripType = this.getTripType(fence);
                    owner.getEmitter().setParameterValueByName(instance, "TripObstacleType", tripType);
                } else {
                    int fenceType = this.getFenceType(fence);
                    long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                    if (owner instanceof IsoPlayer isoPlayer) {
                        ParameterCharacterMovementSpeed parameter = isoPlayer.getParameterCharacterMovementSpeed();
                        owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
                    }

                    owner.getEmitter().setParameterValueByName(instance, "FenceTypeLow", fenceType);
                }
            }
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return true;
    }

    private void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    private void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    private IsoObject getFence(IsoGameCharacter owner) {
        int startX = owner.get(START_X);
        int startY = owner.get(START_Y);
        int z = owner.get(Z);
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
        IsoGridSquare endSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        return startSq != null && endSq != null ? startSq.getHoppableTo(endSq) : null;
    }

    private int getFenceType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return 0;
        } else {
            PropertyContainer props = fence.getSprite().getProperties();
            String typeStr = props.get("FenceTypeLow");
            if (typeStr != null) {
                if ("Sandbag".equals(typeStr) && fence.getName() != null && StringUtils.containsIgnoreCase(fence.getName(), "Gravel")) {
                    typeStr = "Gravelbag";
                }
                return switch (typeStr) {
                    case "Wood" -> 0;
                    case "Metal" -> 1;
                    case "Sandbag" -> 2;
                    case "Gravelbag" -> 3;
                    case "Barbwire" -> 4;
                    case "RoadBlock" -> 5;
                    case "MetalGate" -> 6;
                    default -> 0;
                };
            } else {
                return 0;
            }
        }
    }

    private int getTripType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return 0;
        } else {
            PropertyContainer props = fence.getSprite().getProperties();
            String typeStr = props.get("FenceTypeLow");
            if (typeStr != null) {
                if ("Sandbag".equals(typeStr) && fence.getName() != null && StringUtils.containsIgnoreCase(fence.getName(), "Gravel")) {
                    typeStr = "Gravelbag";
                }
                return switch (typeStr) {
                    case "Wood" -> 0;
                    case "Metal" -> 1;
                    case "Sandbag" -> 2;
                    case "Gravelbag" -> 3;
                    case "Barbwire" -> 4;
                    case "MetalGate" -> 8;
                    default -> 0;
                };
            } else {
                return 0;
            }
        }
    }

    public void setParams(IsoGameCharacter owner, IsoGridSquare startSq, IsoDirections dir) {
        int x = startSq.getX();
        int y = startSq.getY();
        int z = startSq.getZ();
        int endX = x;
        int endY = y;
        switch (dir) {
            case N:
                endY = y - 1;
                break;
            case S:
                endY = y + 1;
                break;
            case W:
                endX = x - 1;
                break;
            case E:
                endX = x + 1;
                break;
            default:
                throw new IllegalArgumentException("invalid direction");
        }

        IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        boolean scratch = false;
        boolean isCounter = oppositeSq != null && oppositeSq.has(IsoFlagType.solidtrans);
        boolean isFloor = oppositeSq != null && oppositeSq.TreatAsSolidFloor();
        boolean isSheetRope = oppositeSq != null && owner.canClimbDownSheetRope(oppositeSq);
        owner.set(START_X, x);
        owner.set(START_Y, y);
        owner.set(Z, z);
        owner.set(END_X, endX);
        owner.set(END_Y, endY);
        owner.set(DIR, dir);
        owner.set(ZOMBIE_ON_FLOOR, false);
        owner.set(PREV_STATE, owner.getCurrentState());
        owner.set(SCRATCH, false);
        owner.set(COUNTER, isCounter);
        owner.set(SOLID_FLOOR, isFloor);
        owner.set(SHEET_ROPE, isSheetRope);
        owner.set(RUN, owner.isRunning());
        owner.set(SPRINT, owner.isSprinting());
        owner.set(COLLIDABLE, false);
    }
}
