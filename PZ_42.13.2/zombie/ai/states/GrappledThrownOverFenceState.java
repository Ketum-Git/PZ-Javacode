// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
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
    private static final GrappledThrownOverFenceState _instance = new GrappledThrownOverFenceState();
    private static final Integer PARAM_START_X = 0;
    private static final Integer PARAM_START_Y = 1;
    private static final Integer PARAM_Z = 2;
    private static final Integer PARAM_END_X = 3;
    private static final Integer PARAM_END_Y = 4;
    private static final Integer PARAM_DIR = 5;
    private static final Integer PARAM_ZOMBIE_ON_FLOOR = 6;
    private static final Integer PARAM_PREV_STATE = 7;
    private static final Integer PARAM_SCRATCH = 8;
    private static final Integer PARAM_COUNTER = 9;
    private static final Integer PARAM_SOLID_FLOOR = 10;
    private static final Integer PARAM_SHEET_ROPE = 11;
    private static final Integer PARAM_RUN = 12;
    private static final Integer PARAM_SPRINT = 13;
    private static final Integer PARAM_COLLIDABLE = 14;
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
        return _instance;
    }

    private GrappledThrownOverFenceState() {
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoDirections dir = Type.tryCastTo(StateMachineParams.get(PARAM_DIR), IsoDirections.class);
        owner.setAnimated(true);
        if (owner.isFallOnFront()) {
            owner.setDir(dir);
        } else {
            owner.setDir(dir.Rot180());
        }

        if (owner.getVariableBoolean("ClimbFenceStarted")) {
            float x = ((Integer)StateMachineParams.get(PARAM_START_X)).intValue();
            float y = ((Integer)StateMachineParams.get(PARAM_START_Y)).intValue();
            switch (dir) {
                case S:
                    y -= 0.1F;
                    break;
                case N:
                    y++;
                    break;
                case E:
                    x -= 0.1F;
                    break;
                case W:
                    x++;
            }

            if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x) && (dir == IsoDirections.W || dir == IsoDirections.E)) {
                this.slideX(owner, x);
            }

            if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y) && (dir == IsoDirections.N || dir == IsoDirections.S)) {
                this.slideY(owner, y);
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
                    owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), tripType);
                } else {
                    int fenceType = this.getFenceType(fence);
                    long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                    if (owner instanceof IsoPlayer isoPlayer) {
                        ParameterCharacterMovementSpeed parameter = isoPlayer.getParameterCharacterMovementSpeed();
                        owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
                    }

                    owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("FenceTypeLow"), fenceType);
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        int startX = (Integer)StateMachineParams.get(PARAM_START_X);
        int startY = (Integer)StateMachineParams.get(PARAM_START_Y);
        int z = (Integer)StateMachineParams.get(PARAM_Z);
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        int endX = (Integer)StateMachineParams.get(PARAM_END_X);
        int endY = (Integer)StateMachineParams.get(PARAM_END_Y);
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

    public void setParams(IsoGameCharacter owner, IsoDirections dir) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        int x = owner.getSquare().getX();
        int y = owner.getSquare().getY();
        int z = owner.getSquare().getZ();
        int endX = x;
        int endY = y;
        switch (dir) {
            case S:
                endY = y + 1;
                break;
            case N:
                endY = y - 1;
                break;
            case E:
                endX = x + 1;
                break;
            case W:
                endX = x - 1;
                break;
            default:
                throw new IllegalArgumentException("invalid direction");
        }

        IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        boolean scratch = false;
        boolean isCounter = oppositeSq != null && oppositeSq.has(IsoFlagType.solidtrans);
        boolean isFloor = oppositeSq != null && oppositeSq.TreatAsSolidFloor();
        boolean isSheetRope = oppositeSq != null && owner.canClimbDownSheetRope(oppositeSq);
        StateMachineParams.put(PARAM_START_X, x);
        StateMachineParams.put(PARAM_START_Y, y);
        StateMachineParams.put(PARAM_Z, z);
        StateMachineParams.put(PARAM_END_X, endX);
        StateMachineParams.put(PARAM_END_Y, endY);
        StateMachineParams.put(PARAM_DIR, dir);
        StateMachineParams.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.FALSE);
        StateMachineParams.put(PARAM_PREV_STATE, owner.getCurrentState());
        StateMachineParams.put(PARAM_SCRATCH, Boolean.FALSE);
        StateMachineParams.put(PARAM_COUNTER, isCounter ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_SOLID_FLOOR, isFloor ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_SHEET_ROPE, isSheetRope ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_RUN, owner.isRunning() ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_SPRINT, owner.isSprinting() ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_COLLIDABLE, Boolean.FALSE);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return true;
    }
}
