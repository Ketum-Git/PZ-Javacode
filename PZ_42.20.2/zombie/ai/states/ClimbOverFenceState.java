// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterFenceTypeHigh;
import zombie.audio.parameters.ParameterFenceTypeLow;
import zombie.audio.parameters.ParameterTripObstacleType;
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
import zombie.core.properties.IsoPropertyType;
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
    private static final int SOUND_RADIUS = 70;
    private static final ClimbOverFenceState INSTANCE = new ClimbOverFenceState();
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
    public static final State.Param<String> OUTCOME = State.Param.ofString("outcome", "success");

    public static ClimbOverFenceState instance() {
        return INSTANCE;
    }

    private ClimbOverFenceState() {
        super(true, false, true, false);
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
        owner.setIgnoreMovement(true);
        if (owner.get(RUN)) {
            owner.setVariable("VaultOverRun", true);
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 300.0));
        } else if (owner.get(SPRINT)) {
            owner.setVariable("VaultOverSprint", true);
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 700.0));
        }

        boolean isCounter = owner.get(COUNTER);
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

        if (!owner.get(SOLID_FLOOR)) {
            owner.setVariable("ClimbFenceOutcome", "falling");
        }

        if (!(owner instanceof IsoZombie) && owner.get(SHEET_ROPE)) {
            owner.setVariable("ClimbFenceOutcome", "rope");
        }

        if (player != null && player.isLocalPlayer()) {
            player.dirtyRecalcGridStackTime = 20.0F;
            player.triggerMusicIntensityEvent("HopFence");
        }

        if (owner.isLocal()) {
            owner.set(OUTCOME, owner.getVariableString("ClimbFenceOutcome"));
        } else {
            owner.setVariable("ClimbFenceOutcome", owner.get(OUTCOME));
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
        IsoDirections dir = owner.get(DIR);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
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

        String climbFenceOutcome = owner.getVariableString("ClimbFenceOutcome");
        if (!"lunge".equals(climbFenceOutcome)) {
            float dxy = 0.05F;
            if (dir == IsoDirections.N || dir == IsoDirections.S) {
                owner.setX(owner.setNextX(PZMath.clamp(owner.getX(), endX + 0.05F, endX + 1 - 0.05F)));
            } else if (dir == IsoDirections.W || dir == IsoDirections.E) {
                owner.setY(owner.setNextY(PZMath.clamp(owner.getY(), endY + 0.05F, endY + 1 - 0.05F)));
            }
        }

        if (owner.getVariableBoolean("ClimbFenceStarted")
            && !"back".equals(climbFenceOutcome)
            && !"fallback".equals(climbFenceOutcome)
            && !"lunge".equalsIgnoreCase(climbFenceOutcome)
            && !"obstacle".equals(climbFenceOutcome)
            && !"obstacleEnd".equals(climbFenceOutcome)) {
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

            if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x) && (dir == IsoDirections.W || dir == IsoDirections.E)) {
                this.slideX(owner, x);
            }

            if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y) && (dir == IsoDirections.N || dir == IsoDirections.S)) {
                this.slideY(owner, y);
            }
        }

        if (owner instanceof IsoZombie) {
            boolean isDown = owner.get(ZOMBIE_ON_FLOOR);
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
            State prevState = owner.get(PREV_STATE);
            if (prevState == PathFindState.instance()) {
                if (owner.getPathFindBehavior2().getTargetChar() == null) {
                    owner.setVariable("bPathfind", true);
                    owner.setVariable("bMoving", false);
                } else if (zombie.isTargetLocationKnown()) {
                    owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
                } else if (zombie.lastTargetSeenX != -1) {
                    owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
                }
            } else if (prevState == WalkTowardState.instance() || prevState == WalkTowardNetworkState.instance()) {
                owner.setVariable("bPathFind", false);
                owner.setVariable("bMoving", true);
            }
        }

        if (zombie != null) {
            zombie.getNetworkCharacterAI().isClimbing = false;
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
        if (zombie != null) {
            try {
                ParameterZombieState.State state = ParameterZombieState.State.valueOf(event.parameterValue);
                zombie.parameterZombieState.setState(state);
            } catch (IllegalArgumentException var6) {
            }
        }
    }

    private void OnAnimEvent_SetCollidable(IsoGameCharacter owner, AnimEvent event) {
        owner.set(COLLIDABLE, Boolean.parseBoolean(event.parameterValue));
    }

    private void OnAnimEvent_PlayTripSound(IsoGameCharacter owner, AnimEvent event) {
        if (SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 70.0F)) {
            IsoObject fence = this.getFence(owner);
            if (fence != null) {
                ParameterTripObstacleType.ObstacleType tripType = this.getTripType(fence);
                long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                ParameterCharacterMovementSpeed parameter = ((IsoPlayer)owner).getParameterCharacterMovementSpeed();
                owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
                owner.getEmitter().setParameterValueByName(instance, "TripObstacleType", tripType.getValue());
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
        if (SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 70.0F)) {
            IsoObject fence = this.getFence(owner);
            if (fence != null) {
                if (owner instanceof IsoZombie) {
                    long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                    ParameterTripObstacleType.ObstacleType tripType = this.getTripType(fence);
                    owner.getEmitter().setParameterValueByName(instance, "TripObstacleType", tripType.getValue());
                } else {
                    ParameterFenceTypeLow.FenceType fenceType = this.getFenceType(fence);
                    long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
                    if (owner instanceof IsoPlayer isoPlayer) {
                        ParameterCharacterMovementSpeed parameter = isoPlayer.getParameterCharacterMovementSpeed();
                        owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
                    }

                    owner.getEmitter().setParameterValueByName(instance, "FenceTypeLow", fenceType.getValue());
                }
            }
        }
    }

    private void OnAnimEvent_OnFloor(IsoGameCharacter owner, AnimEvent event) {
        owner.set(ZOMBIE_ON_FLOOR, Boolean.parseBoolean(event.parameterValue));
        if (Boolean.parseBoolean(event.parameterValue)) {
            this.setLungeXVars((IsoZombie)owner);
            IsoObject fence = this.getFence(owner);
            if (this.countZombiesClimbingOver(fence) >= 2) {
                fence.damage = (short)(fence.damage - Rand.Next(7, 12) / (this.isMetalFence(fence) ? 2 : 1));
                if (fence.damage <= 0) {
                    fence.destroyFence(owner.get(DIR));
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

    private ParameterFenceTypeLow.FenceType getFenceType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return ParameterFenceTypeLow.FenceType.WOOD;
        } else {
            PropertyContainer props = fence.getSprite().getProperties();
            String typeStr = props.get(IsoPropertyType.FENCE_TYPE_LOW);
            if (typeStr != null) {
                return ParameterFenceTypeLow.FenceType.SANDBAG.getName().equals(typeStr)
                        && fence.getName() != null
                        && StringUtils.containsIgnoreCase(fence.getName(), "Gravel")
                    ? ParameterFenceTypeLow.FenceType.GRAVELBAG
                    : ParameterFenceTypeLow.FenceType.fromString(typeStr, ParameterFenceTypeLow.FenceType.WOOD);
            } else {
                typeStr = props.get(IsoPropertyType.FENCE_TYPE_HIGH);
                if (typeStr != null) {
                    ParameterFenceTypeHigh.FenceType fenceType = ParameterFenceTypeHigh.FenceType.fromString(typeStr, ParameterFenceTypeHigh.FenceType.WOOD);

                    return switch (fenceType) {
                        case WOOD -> ParameterFenceTypeLow.FenceType.WOOD;
                        case METAL -> ParameterFenceTypeLow.FenceType.METAL;
                        case METAL_GATE -> ParameterFenceTypeLow.FenceType.METAL_GATE;
                    };
                } else {
                    return ParameterFenceTypeLow.FenceType.WOOD;
                }
            }
        }
    }

    private ParameterTripObstacleType.ObstacleType getTripType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return ParameterTripObstacleType.ObstacleType.WOOD;
        } else {
            PropertyContainer props = fence.getSprite().getProperties();
            String typeStr = props.get(IsoPropertyType.FENCE_TYPE_LOW);
            if (typeStr != null) {
                if (ParameterFenceTypeLow.FenceType.SANDBAG.getName().equals(typeStr)
                    && fence.getName() != null
                    && StringUtils.containsIgnoreCase(fence.getName(), "Gravel")) {
                    return ParameterTripObstacleType.ObstacleType.GRAVELBAG;
                } else {
                    ParameterFenceTypeLow.FenceType fenceType = ParameterFenceTypeLow.FenceType.fromString(typeStr, ParameterFenceTypeLow.FenceType.WOOD);

                    return switch (fenceType) {
                        case WOOD -> ParameterTripObstacleType.ObstacleType.WOOD;
                        case METAL -> ParameterTripObstacleType.ObstacleType.METAL;
                        case SANDBAG -> ParameterTripObstacleType.ObstacleType.SANDBAG;
                        case GRAVELBAG -> ParameterTripObstacleType.ObstacleType.GRAVELBAG;
                        case BARBWIRE -> ParameterTripObstacleType.ObstacleType.BARBWIRE;
                        case ROADBLOCK -> ParameterTripObstacleType.ObstacleType.WOOD;
                        case METAL_GATE -> ParameterTripObstacleType.ObstacleType.METAL_BARS;
                    };
                }
            } else {
                typeStr = props.get(IsoPropertyType.FENCE_TYPE_HIGH);
                if (typeStr != null) {
                    ParameterFenceTypeHigh.FenceType fenceType = ParameterFenceTypeHigh.FenceType.fromString(typeStr, ParameterFenceTypeHigh.FenceType.WOOD);

                    return switch (fenceType) {
                        case WOOD -> ParameterTripObstacleType.ObstacleType.WOOD;
                        case METAL -> ParameterTripObstacleType.ObstacleType.METAL;
                        case METAL_GATE -> ParameterTripObstacleType.ObstacleType.METAL_BARS;
                    };
                } else {
                    return ParameterTripObstacleType.ObstacleType.WOOD;
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
            String material = props.get("Material");
            String material2 = props.get("Material2");
            String material3 = props.get("Material3");
            if ("MetalBars".equals(material) || "MetalBars".equals(material2) || "MetalBars".equals(material3)) {
                return true;
            } else if (!"MetalWire".equals(material) && !"MetalWire".equals(material2) && !"MetalWire".equals(material3)) {
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
        owner.set(SCRATCH, false);
        owner.set(COUNTER, isCounter);
        owner.set(SOLID_FLOOR, isFloor);
        owner.set(SHEET_ROPE, isSheetRope);
        owner.set(RUN, owner.isRunning());
        owner.set(SPRINT, owner.isSprinting());
        owner.set(COLLIDABLE, false);
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
        if (RUN.fromDelegate(delegate)) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 300.0));
        } else if (SPRINT.fromDelegate(delegate)) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 700.0));
        }
    }

    @Override
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.QUARTER;
    }
}
