// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

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
    private static final AnimalClimbOverFenceState INSTANCE = new AnimalClimbOverFenceState();
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
    public static final int TRIP_TREE = 5;
    public static final int TRIP_ZOMBIE = 6;
    public static final int COLLIDE_WITH_WALL = 7;
    public static final int TRIP_METAL_BARS = 8;
    public static final int TRIP_WINDOW = 9;

    public static AnimalClimbOverFenceState instance() {
        return INSTANCE;
    }

    private AnimalClimbOverFenceState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        boolean bCanClimb = animal.canClimbFences();
        if (!bCanClimb) {
            owner.clearVariable("climbDown");
            owner.setVariable("ClimbFence", false);
        } else {
            IsoDirections dir = owner.get(DIR);
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
        owner.setAnimated(true);
        IsoDirections dir = owner.get(DIR);
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
        owner.clearVariable("ClimbFence");
        owner.ClearVariable("climbDown");
        IsoDirections dir = owner.get(DIR);
        dir = dir.Rot180();
        owner.setDir(dir);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoAnimal animal = Type.tryCastTo(owner, IsoAnimal.class);
        if (event.eventName.equalsIgnoreCase("Climbed")) {
            animal.setVariable("climbDown", true);
            IsoDirections dir = owner.get(DIR);
            dir = dir.RotLeft();
            dir = dir.RotLeft();
            owner.set(DIR, dir);
        }

        if (event.eventName.equalsIgnoreCase("ClimbDone")) {
            animal.clearVariable("climbDown");
            animal.setVariable("ClimbFence", false);
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        int startX = owner.get(START_X);
        int startY = owner.get(START_Y);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
        int z = owner.get(Z);
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

    public void setParams(IsoGameCharacter owner, IsoDirections dir) {
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
        owner.set(START_X, x);
        owner.set(START_Y, y);
        owner.set(Z, z);
        owner.set(END_X, endX);
        owner.set(END_Y, endY);
        owner.set(DIR, dir);
        owner.set(ZOMBIE_ON_FLOOR, false);
        owner.set(PREV_STATE, owner.getCurrentState());
        owner.set(COUNTER, isCounter);
        owner.set(SOLID_FLOOR, isFloor);
        owner.set(SHEET_ROPE, isSheetRope);
        owner.set(RUN, owner.isRunning());
        owner.set(SPRINT, owner.isSprinting());
        owner.set(COLLIDABLE, false);
    }
}
