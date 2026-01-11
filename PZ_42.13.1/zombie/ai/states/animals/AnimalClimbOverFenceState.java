// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import java.util.HashMap;
import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class AnimalClimbOverFenceState extends State {
    private static final AnimalClimbOverFenceState _instance = new AnimalClimbOverFenceState();
    static final Integer PARAM_START_X = 0;
    static final Integer PARAM_START_Y = 1;
    static final Integer PARAM_Z = 2;
    static final Integer PARAM_END_X = 3;
    static final Integer PARAM_END_Y = 4;
    static final Integer PARAM_DIR = 5;
    static final Integer PARAM_ZOMBIE_ON_FLOOR = 6;
    static final Integer PARAM_PREV_STATE = 7;
    static final Integer PARAM_SCRATCH = 8;
    static final Integer PARAM_COUNTER = 9;
    static final Integer PARAM_SOLID_FLOOR = 10;
    static final Integer PARAM_SHEET_ROPE = 11;
    static final Integer PARAM_RUN = 12;
    static final Integer PARAM_SPRINT = 13;
    static final Integer PARAM_COLLIDABLE = 14;
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
    public static final int TRIP_TREE = 5;
    public static final int TRIP_ZOMBIE = 6;
    public static final int COLLIDE_WITH_WALL = 7;
    public static final int TRIP_METAL_BARS = 8;
    public static final int TRIP_WINDOW = 9;

    public static AnimalClimbOverFenceState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoAnimal animal = (IsoAnimal)owner;
        boolean bCanClimb = animal.canClimbFences();
        if (!bCanClimb) {
            owner.clearVariable("climbDown");
            owner.setVariable("ClimbFence", false);
        } else {
            IsoDirections dir = Type.tryCastTo(StateMachineParams.get(PARAM_DIR), IsoDirections.class);
            IsoGridSquare oppositeSq = owner.getCurrentSquare().getAdjacentSquare(dir);
            if (oppositeSq == null) {
                owner.clearVariable("climbDown");
                owner.setVariable("ClimbFence", false);
            } else if (!owner.getCurrentSquare().isHoppableTo(oppositeSq)) {
                owner.clearVariable("climbDown");
                owner.setVariable("ClimbFence", false);
            } else {
                owner.setVariable("ClimbingFence", true);
            }
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoDirections dir = Type.tryCastTo(StateMachineParams.get(PARAM_DIR), IsoDirections.class);
        int endX = (Integer)StateMachineParams.get(PARAM_END_X);
        int endY = (Integer)StateMachineParams.get(PARAM_END_Y);
        owner.setAnimated(true);
        if (dir == IsoDirections.N) {
            owner.setDir(IsoDirections.N);
        } else if (dir == IsoDirections.S) {
            owner.setDir(IsoDirections.S);
        } else if (dir == IsoDirections.W) {
            owner.setDir(IsoDirections.W);
        } else if (dir == IsoDirections.E) {
            owner.setDir(IsoDirections.E);
        }

        if (owner.getVariableBoolean("ClimbFenceStarted") && owner.isVariable("ClimbFenceOutcome", "fall")) {
            owner.setbFalling(true);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.clearVariable("ClimbFence");
        owner.ClearVariable("climbDown");
        IsoDirections dir = Type.tryCastTo(StateMachineParams.get(PARAM_DIR), IsoDirections.class);
        dir = IsoDirections.reverse(dir);
        owner.setDir(dir);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoAnimal animal = Type.tryCastTo(owner, IsoAnimal.class);
        if (event.eventName.equalsIgnoreCase("Climbed")) {
            animal.setVariable("climbDown", true);
            IsoDirections dir = Type.tryCastTo(StateMachineParams.get(PARAM_DIR), IsoDirections.class);
            dir = dir.RotLeft();
            dir = dir.RotLeft();
            StateMachineParams.put(PARAM_DIR, dir);
        }

        if (event.eventName.equalsIgnoreCase("ClimbDone")) {
            animal.clearVariable("climbDown");
            animal.setVariable("ClimbFence", false);
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        int startX = (Integer)StateMachineParams.get(PARAM_START_X);
        int startY = (Integer)StateMachineParams.get(PARAM_START_Y);
        int endX = (Integer)StateMachineParams.get(PARAM_END_X);
        int endY = (Integer)StateMachineParams.get(PARAM_END_Y);
        int z = (Integer)StateMachineParams.get(PARAM_Z);
        if (z == fromZ && z == toZ) {
            int x1 = PZMath.min(startX, endX);
            int y1 = PZMath.min(startY, endY);
            int x2 = PZMath.max(startX, endX);
            int y2 = PZMath.max(startY, endY);
            int x3 = PZMath.min(fromX, toX);
            int y3 = PZMath.min(fromY, toY);
            int x4 = PZMath.max(fromX, toX);
            int y4 = PZMath.max(fromY, toY);
            return x1 <= x3 && y1 <= y3 && x2 >= x4 && y2 >= y4;
        } else {
            return false;
        }
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
        StateMachineParams.put(PARAM_START_X, x);
        StateMachineParams.put(PARAM_START_Y, y);
        StateMachineParams.put(PARAM_Z, z);
        StateMachineParams.put(PARAM_END_X, endX);
        StateMachineParams.put(PARAM_END_Y, endY);
        StateMachineParams.put(PARAM_DIR, dir);
        StateMachineParams.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.FALSE);
        StateMachineParams.put(PARAM_PREV_STATE, owner.getCurrentState());
        StateMachineParams.put(PARAM_COUNTER, isCounter ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_SOLID_FLOOR, isFloor ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_SHEET_ROPE, isSheetRope ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_RUN, owner.isRunning() ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_SPRINT, owner.isSprinting() ? Boolean.TRUE : Boolean.FALSE);
        StateMachineParams.put(PARAM_COLLIDABLE, Boolean.FALSE);
    }
}
