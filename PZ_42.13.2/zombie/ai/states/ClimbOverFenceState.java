// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import java.util.Map;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class ClimbOverFenceState extends State {
    private static final ClimbOverFenceState _instance = new ClimbOverFenceState();
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
    private static final Integer PARAM_OUTCOME = 15;
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

    public static ClimbOverFenceState instance() {
        return _instance;
    }

    private ClimbOverFenceState() {
        this.addAnimEventListener("CheckAttack", this::OnAnimEvent_CheckAttack);
        this.addAnimEventListener("VaultSprintFallLanded", this::OnAnimEvent_VaultSprintFallLanded);
        this.addAnimEventListener("FallenOnKnees", this::OnAnimEvent_FallenOnKnees);
        this.addAnimEventListener("OnFloor", this::OnAnimEvent_OnFloor);
        this.addAnimEventListener("PlayFenceSound", this::OnAnimEvent_PlayFenceSound);
        this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
        this.addAnimEventListener("PlayTripSound", this::OnAnimEvent_PlayTripSound);
        this.addAnimEventListener("SetCollidable", this::OnAnimEvent_SetCollidable);
        this.addAnimEventListener("SetState", this::OnAnimEvent_SetState);
        this.addAnimEventListener("VaultOverStarted", this::OnAnimEvent_VaultOverStarted);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        owner.setVariable("FenceLungeX", 0.0F);
        owner.setVariable("FenceLungeY", 0.0F);
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setIgnoreMovement(true);
        if (StateMachineParams.get(PARAM_RUN) == Boolean.TRUE) {
            owner.setVariable("VaultOverRun", true);
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 300.0));
        } else if (StateMachineParams.get(PARAM_SPRINT) == Boolean.TRUE) {
            owner.setVariable("VaultOverSprint", true);
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 700.0));
        }

        boolean isCounter = StateMachineParams.get(PARAM_COUNTER) == Boolean.TRUE;
        owner.setVariable("ClimbingFence", true);
        owner.setVariable("ClimbFenceStarted", false);
        owner.setVariable("ClimbFenceFinished", false);
        owner.setVariable("ClimbFenceOutcome", isCounter ? "obstacle" : "success");
        owner.clearVariable("ClimbFenceFlopped");
        if ((owner.getVariableBoolean("VaultOverRun") || owner.getVariableBoolean("VaultOverSprint")) && this.shouldFallAfterVaultOver(owner)) {
            owner.setVariable("ClimbFenceOutcome", "fall");
        }

        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (!isCounter && zombie != null && zombie.shouldDoFenceLunge()) {
            owner.setVariable("ClimbFenceOutcome", "lunge");
            this.setLungeXVars(zombie);
        }

        if (StateMachineParams.get(PARAM_SOLID_FLOOR) == Boolean.FALSE) {
            owner.setVariable("ClimbFenceOutcome", "falling");
        }

        if (!(owner instanceof IsoZombie) && StateMachineParams.get(PARAM_SHEET_ROPE) == Boolean.TRUE) {
            owner.setVariable("ClimbFenceOutcome", "rope");
        }

        if (player != null && player.isLocalPlayer()) {
            player.dirtyRecalcGridStackTime = 20.0F;
            player.triggerMusicIntensityEvent("HopFence");
        }

        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_OUTCOME, owner.getVariableString("ClimbFenceOutcome"));
        } else {
            String outcome = (String)owner.getStateMachineParams(this).getOrDefault(PARAM_OUTCOME, "success");
            owner.setVariable("ClimbFenceOutcome", outcome);
        }
    }

    private void setLungeXVars(IsoZombie zombie) {
        IsoMovingObject target = zombie.getTarget();
        if (target != null) {
            zombie.setVariable("FenceLungeX", 0.0F);
            zombie.setVariable("FenceLungeY", 0.0F);
            float lungeX = 0.0F;
            float forwardX = zombie.getForwardDirectionX();
            float forwardY = zombie.getForwardDirectionY();
            PZMath.SideOfLine side = PZMath.testSideOfLine(
                zombie.getX(), zombie.getY(), zombie.getX() + forwardX, zombie.getY() + forwardY, target.getX(), target.getY()
            );
            float angleRad = (float)Math.acos(zombie.getDotWithForwardDirection(target.getX(), target.getY()));
            float angleDeg = PZMath.clamp(PZMath.radToDeg(angleRad), 0.0F, 90.0F);
            switch (side) {
                case Left:
                    lungeX = -angleDeg / 90.0F;
                    break;
                case OnLine:
                    lungeX = 0.0F;
                    break;
                case Right:
                    lungeX = angleDeg / 90.0F;
            }

            zombie.setVariable("FenceLungeX", lungeX);
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

        String ClimbFenceOutcome = owner.getVariableString("ClimbFenceOutcome");
        if (!"lunge".equals(ClimbFenceOutcome)) {
            float dxy = 0.05F;
            if (dir == IsoDirections.N || dir == IsoDirections.S) {
                owner.setX(owner.setNextX(PZMath.clamp(owner.getX(), endX + 0.05F, endX + 1 - 0.05F)));
            } else if (dir == IsoDirections.W || dir == IsoDirections.E) {
                owner.setY(owner.setNextY(PZMath.clamp(owner.getY(), endY + 0.05F, endY + 1 - 0.05F)));
            }
        }

        if (owner.getVariableBoolean("ClimbFenceStarted")
            && !"back".equals(ClimbFenceOutcome)
            && !"fallback".equals(ClimbFenceOutcome)
            && !"lunge".equalsIgnoreCase(ClimbFenceOutcome)
            && !"obstacle".equals(ClimbFenceOutcome)
            && !"obstacleEnd".equals(ClimbFenceOutcome)) {
            float x = ((Integer)StateMachineParams.get(PARAM_START_X)).intValue();
            float y = ((Integer)StateMachineParams.get(PARAM_START_Y)).intValue();
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

            if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x) && (dir == IsoDirections.W || dir == IsoDirections.E)) {
                this.slideX(owner, x);
            }

            if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y) && (dir == IsoDirections.N || dir == IsoDirections.S)) {
                this.slideY(owner, y);
            }
        }

        if (owner instanceof IsoZombie) {
            boolean isDown = StateMachineParams.get(PARAM_ZOMBIE_ON_FLOOR) == Boolean.TRUE;
            owner.setOnFloor(isDown);
            owner.setKnockedDown(isDown);
            owner.setFallOnFront(isDown);
        }

        if (owner.getVariableBoolean("ClimbFenceStarted") && owner.isVariable("ClimbFenceOutcome", "fall")) {
            owner.setbFalling(true);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (player != null && "fall".equals(owner.getVariableString("ClimbFenceOutcome"))) {
            owner.setSprinting(false);
        }

        owner.clearVariable("ClimbingFence");
        owner.clearVariable("ClimbFenceFinished");
        owner.clearVariable("ClimbFenceOutcome");
        owner.clearVariable("ClimbFenceStarted");
        owner.clearVariable("ClimbFenceFlopped");
        owner.clearVariable("PlayerVoiceSound");
        owner.ClearVariable("VaultOverSprint");
        owner.ClearVariable("VaultOverRun");
        owner.setIgnoreMovement(false);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (zombie != null) {
            zombie.allowRepathDelay = 0.0F;
            if (StateMachineParams.get(PARAM_PREV_STATE) == PathFindState.instance()) {
                if (owner.getPathFindBehavior2().getTargetChar() == null) {
                    owner.setVariable("bPathfind", true);
                    owner.setVariable("bMoving", false);
                } else if (zombie.isTargetLocationKnown()) {
                    owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
                } else if (zombie.lastTargetSeenX != -1) {
                    owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
                }
            } else if (StateMachineParams.get(PARAM_PREV_STATE) == WalkTowardState.instance()
                || StateMachineParams.get(PARAM_PREV_STATE) == WalkTowardNetworkState.instance()) {
                owner.setVariable("bPathFind", false);
                owner.setVariable("bMoving", true);
            }
        }

        if (zombie != null) {
            zombie.networkAi.isClimbing = false;
        }
    }

    private void OnAnimEvent_VaultOverStarted(IsoGameCharacter owner) {
        if (owner.isVariable("ClimbFenceOutcome", "fall")) {
            owner.reportEvent("EventFallClimb");
            owner.setVariable("BumpDone", true);
            owner.setFallOnFront(true);
        }
    }

    private void OnAnimEvent_SetState(IsoGameCharacter owner, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (zombie != null) {
            try {
                ParameterZombieState.State state = ParameterZombieState.State.valueOf(event.parameterValue);
                zombie.parameterZombieState.setState(state);
            } catch (IllegalArgumentException var7) {
            }
        }
    }

    private void OnAnimEvent_SetCollidable(IsoGameCharacter owner, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(PARAM_COLLIDABLE, Boolean.parseBoolean(event.parameterValue));
    }

    private void OnAnimEvent_PlayTripSound(IsoGameCharacter owner, AnimEvent event) {
        if (SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0F)) {
            IsoObject fence = this.getFence(owner);
            if (fence != null) {
                int tripType = this.getTripType(fence);
                long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                ParameterCharacterMovementSpeed parameter = ((IsoPlayer)owner).getParameterCharacterMovementSpeed();
                owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
                owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), tripType);
            }
        }
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

    private void OnAnimEvent_OnFloor(IsoGameCharacter owner, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.parseBoolean(event.parameterValue));
        if (Boolean.parseBoolean(event.parameterValue)) {
            this.setLungeXVars((IsoZombie)owner);
            IsoObject fence = this.getFence(owner);
            if (this.countZombiesClimbingOver(fence) >= 2) {
                fence.damage = (short)(fence.damage - Rand.Next(7, 12) / (this.isMetalFence(fence) ? 2 : 1));
                if (fence.damage <= 0) {
                    IsoDirections dir = Type.tryCastTo(StateMachineParams.get(PARAM_DIR), IsoDirections.class);
                    fence.destroyFence(dir);
                }
            }

            owner.setVariable("ClimbFenceFlopped", true);
        }
    }

    private void OnAnimEvent_FallenOnKnees(IsoGameCharacter owner) {
        owner.fallenOnKnees();
    }

    private void OnAnimEvent_VaultSprintFallLanded(IsoGameCharacter owner) {
        owner.dropHandItems();
        owner.fallenOnKnees();
    }

    private void OnAnimEvent_CheckAttack(IsoGameCharacter owner) {
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (zombie != null && zombie.target instanceof IsoGameCharacter isoGameCharacter) {
            isoGameCharacter.attackFromWindowsLunge(zombie);
        }
    }

    @Override
    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
        boolean hasPath = owner.getPath2() != null;
        boolean isPlayer = owner instanceof IsoPlayer;
        if (hasPath && isPlayer) {
            modifiers.setMaxTurnDelta(2.0F);
        }
    }

    /**
     * Description copied from class: State
     */
    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        int startX = (Integer)StateMachineParams.getOrDefault(PARAM_START_X, 0);
        int startY = (Integer)StateMachineParams.getOrDefault(PARAM_START_Y, 0);
        int endX = (Integer)StateMachineParams.getOrDefault(PARAM_END_X, 0);
        int endY = (Integer)StateMachineParams.getOrDefault(PARAM_END_Y, 0);
        int z = (Integer)StateMachineParams.getOrDefault(PARAM_Z, 0);
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
                typeStr = props.get("FenceTypeHigh");
                if (typeStr != null) {
                    return switch (typeStr) {
                        case "Wood" -> 0;
                        case "Metal" -> 1;
                        case "MetalBars" -> 8;
                        default -> 0;
                    };
                } else {
                    return 0;
                }
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
                typeStr = props.get("FenceTypeHigh");
                if (typeStr != null) {
                    return switch (typeStr) {
                        case "Wood" -> 0;
                        case "Metal" -> 1;
                        case "MetalBars" -> 8;
                        default -> 0;
                    };
                } else {
                    return 0;
                }
            }
        }
    }

    private boolean shouldFallAfterVaultOver(IsoGameCharacter owner) {
        if (DebugOptions.instance.character.debug.alwaysTripOverFence.getValue()) {
            return true;
        } else {
            float chance = 0.0F;
            if (owner.getVariableBoolean("VaultOverSprint")) {
                chance = 10.0F;
            }

            if (owner.getMoodles() != null) {
                chance += owner.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 10;
                chance += owner.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 10;
                chance += owner.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 13;
                chance += owner.getMoodles().getMoodleLevel(MoodleType.PAIN) * 5;
            }

            BodyPart part = owner.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
            if (part.getAdditionalPain(true) > 20.0F) {
                chance += (part.getAdditionalPain(true) - 20.0F) / 10.0F;
            }

            if (owner.hasTrait(CharacterTrait.CLUMSY)) {
                chance += 10.0F;
            }

            if (owner.hasTrait(CharacterTrait.GRACEFUL)) {
                chance -= 10.0F;
            }

            if (owner.hasTrait(CharacterTrait.VERY_UNDERWEIGHT)) {
                chance += 20.0F;
            }

            if (owner.hasTrait(CharacterTrait.VERY_UNDERWEIGHT)) {
                chance += 10.0F;
            }

            if (owner.hasTrait(CharacterTrait.OBESE)) {
                chance += 20.0F;
            }

            if (owner.hasTrait(CharacterTrait.OVERWEIGHT)) {
                chance += 10.0F;
            }

            chance -= owner.getPerkLevel(PerkFactory.Perks.Fitness);
            return Rand.Next(100) < chance;
        }
    }

    private int countZombiesClimbingOver(IsoObject fence) {
        if (fence != null && fence.getSquare() != null) {
            int count = 0;
            IsoGridSquare square = fence.getSquare();
            count += this.countZombiesClimbingOver(fence, square);
            if (fence.getProperties().has(IsoFlagType.HoppableN)) {
                square = square.getAdjacentSquare(IsoDirections.N);
            } else {
                square = square.getAdjacentSquare(IsoDirections.W);
            }

            return count + this.countZombiesClimbingOver(fence, square);
        } else {
            return 0;
        }
    }

    private int countZombiesClimbingOver(IsoObject fence, IsoGridSquare square) {
        if (square == null) {
            return 0;
        } else {
            int count = 0;

            for (int i = 0; i < square.getMovingObjects().size(); i++) {
                IsoZombie zombie = Type.tryCastTo(square.getMovingObjects().get(i), IsoZombie.class);
                if (zombie != null && zombie.target != null && zombie.isCurrentState(this) && this.getFence(zombie) == fence) {
                    count++;
                }
            }

            return count;
        }
    }

    private boolean isMetalFence(IsoObject fence) {
        if (fence != null && fence.getProperties() != null) {
            PropertyContainer props = fence.getProperties();
            String Material = props.get("Material");
            String Material2 = props.get("Material2");
            String Material3 = props.get("Material3");
            if ("MetalBars".equals(Material) || "MetalBars".equals(Material2) || "MetalBars".equals(Material3)) {
                return true;
            } else if (!"MetalWire".equals(Material) && !"MetalWire".equals(Material2) && !"MetalWire".equals(Material3)) {
                if (fence instanceof IsoThumpable && fence.hasModData()) {
                    KahluaTableIterator iter = fence.getModData().iterator();

                    while (iter.advance()) {
                        String key = Type.tryCastTo(iter.getKey(), String.class);
                        if (key != null && key.contains("MetalPipe")) {
                            return true;
                        }
                    }
                }

                return false;
            } else {
                return true;
            }
        } else {
            return false;
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

    @Override
    public boolean isSyncInIdle() {
        return false;
    }

    @Override
    public boolean canRagdoll(IsoGameCharacter owner) {
        return owner.getVariableBoolean("ClimbingFence", false) ? false : !(owner instanceof IsoZombie ownerZombie && ownerZombie.isOnFloor());
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        if (delegate.get(PARAM_RUN).equals(Boolean.TRUE)) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 300.0));
        } else if (delegate.get(PARAM_SPRINT).equals(Boolean.TRUE)) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 700.0));
        }
    }
}
