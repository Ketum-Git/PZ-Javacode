// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import java.util.Map;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.scripting.objects.MoodleType;
import zombie.util.Pool;
import zombie.util.Type;

@UsedFromLua
public final class ClimbThroughWindowState extends State {
    private static final ClimbThroughWindowState _instance = new ClimbThroughWindowState();
    private static final Integer PARAM_PARAMS = 0;
    private static final Integer PARAM_PREV_STATE = 1;
    private static final Integer PARAM_ZOMBIE_ON_FLOOR = 2;
    private static final Integer PARAM_OUTCOME = 3;
    private static final Integer PARAM_SCRATCHED = 4;

    public static ClimbThroughWindowState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        ClimbThroughWindowPositioningParams positioningParams = (ClimbThroughWindowPositioningParams)StateMachineParams.computeIfAbsent(
            PARAM_PARAMS, key -> ClimbThroughWindowPositioningParams.alloc()
        );
        boolean isCounter = positioningParams.isCounter;
        owner.setVariable("ClimbWindowStarted", false);
        owner.setVariable("ClimbWindowEnd", false);
        owner.setVariable("ClimbWindowFinished", false);
        owner.clearVariable("ClimbWindowGetUpBack");
        owner.clearVariable("ClimbWindowGetUpFront");
        owner.setVariable("ClimbWindowOutcome", isCounter ? "obstacle" : "success");
        owner.clearVariable("ClimbWindowFlopped");
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (!isCounter && zombie != null && zombie.shouldDoFenceLunge()) {
            this.setLungeXVars(zombie);
            owner.setVariable("ClimbWindowOutcome", "lunge");
        }

        if (!positioningParams.isFloor) {
            owner.setVariable("ClimbWindowOutcome", "fall");
        }

        if (!(owner instanceof IsoZombie) && positioningParams.isSheetRope) {
            owner.setVariable("ClimbWindowOutcome", "rope");
        }

        if (player != null && player.isLocalPlayer()) {
            player.dirtyRecalcGridStackTime = 20.0F;
            player.triggerMusicIntensityEvent("ClimbThroughWindow");
        }

        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        ClimbThroughWindowPositioningParams positioningParams = (ClimbThroughWindowPositioningParams)StateMachineParams.get(PARAM_PARAMS);
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (!this.isWindowClosing(owner)) {
            owner.setDir(positioningParams.climbDir);
            String ClimbWindowOutcome = owner.getVariableString("ClimbWindowOutcome");
            if (owner instanceof IsoZombie) {
                boolean isDown = StateMachineParams.get(PARAM_ZOMBIE_ON_FLOOR) == Boolean.TRUE;
                if (!owner.isFallOnFront() && isDown) {
                    int oppositeX = positioningParams.oppositeX;
                    int oppositeY = positioningParams.oppositeY;
                    int oppositeZ = positioningParams.z;
                    IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY, oppositeZ);
                    if (oppositeSq != null && oppositeSq.getBrokenGlass() != null) {
                        owner.addBlood(BloodBodyPartType.Head, true, true, true);
                        owner.addBlood(BloodBodyPartType.Head, true, true, true);
                        owner.addBlood(BloodBodyPartType.Head, true, true, true);
                        owner.addBlood(BloodBodyPartType.Head, true, true, true);
                        owner.addBlood(BloodBodyPartType.Head, true, true, true);
                        owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                        owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                        owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                        owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                        owner.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                        owner.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                        owner.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                    }
                }

                owner.setOnFloor(isDown);
                owner.setKnockedDown(isDown);
                owner.setFallOnFront(isDown);
            }

            if (!owner.getVariableBoolean("ClimbWindowStarted")) {
                slideCharacterToWindowOpening(owner, positioningParams);
            }

            if (owner instanceof IsoPlayer && ClimbWindowOutcome.equalsIgnoreCase("obstacle")) {
                float endX = positioningParams.endX + 0.5F;
                float endY = positioningParams.endY + 0.5F;
                if (owner.DistToSquared(endX, endY) < 0.5625F) {
                    owner.setVariable("ClimbWindowOutcome", "obstacleEnd");
                }
            }

            if (owner instanceof IsoPlayer
                && !owner.getVariableBoolean("ClimbWindowEnd")
                && !"fallfront".equals(ClimbWindowOutcome)
                && !"back".equals(ClimbWindowOutcome)
                && !"fallback".equals(ClimbWindowOutcome)) {
                int oppositeX = positioningParams.oppositeX;
                int oppositeY = positioningParams.oppositeY;
                int oppositeZ = positioningParams.z;
                IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY, oppositeZ);
                if (oppositeSq != null) {
                    this.checkForFallingBack(oppositeSq, owner);
                    if (oppositeSq != owner.getSquare() && oppositeSq.TreatAsSolidFloor()) {
                        this.checkForFallingFront(owner.getSquare(), owner);
                    }
                }

                if (owner.getMoodles().getMoodleLevel(MoodleType.DRUNK) > 1
                    && owner.getVariableString("ClimbWindowOutcome").equals(ClimbWindowOutcome)
                    && Rand.Next(2000) < owner.getStats().get(CharacterStat.INTOXICATION)) {
                    if (Rand.NextBool(2)) {
                        owner.setVariable("ClimbWindowOutcome", "fallback");
                    } else {
                        owner.setVariable("ClimbWindowOutcome", "fallfront");
                    }
                }
            }

            if (owner.getVariableBoolean("ClimbWindowStarted")
                && !"back".equals(ClimbWindowOutcome)
                && !"fallback".equals(ClimbWindowOutcome)
                && !"lunge".equals(ClimbWindowOutcome)
                && !"obstacle".equals(ClimbWindowOutcome)
                && !"obstacleEnd".equals(ClimbWindowOutcome)) {
                float x = positioningParams.startX;
                float y = positioningParams.startY;
                switch (positioningParams.climbDir) {
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

                if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x)
                    && (positioningParams.climbDir == IsoDirections.W || positioningParams.climbDir == IsoDirections.E)) {
                    slideX(owner, x);
                }

                if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y)
                    && (positioningParams.climbDir == IsoDirections.N || positioningParams.climbDir == IsoDirections.S)) {
                    slideY(owner, y);
                }
            }

            if (owner.getVariableBoolean("ClimbWindowStarted") && positioningParams.scratch) {
                positioningParams.scratch = false;
                owner.getBodyDamage().setScratchedWindow();
                if (player != null) {
                    player.playerVoiceSound("PainFromGlassCut");
                }
            }

            if (owner.getVariableBoolean("ClimbWindowStarted") && owner.isVariable("ClimbWindowOutcome", "fall")) {
                owner.setbFalling(true);
            }
        }
    }

    public static void slideCharacterToWindowOpening(IsoGameCharacter in_character, ClimbThroughWindowPositioningParams in_positioningParams) {
        IsoDirections climbDir = in_positioningParams.climbDir;
        if (climbDir == IsoDirections.N || climbDir == IsoDirections.S) {
            float startX = in_positioningParams.startX + 0.5F;
            if (in_character.getX() != startX) {
                slideX(in_character, startX);
            }
        }

        if (climbDir == IsoDirections.W || climbDir == IsoDirections.E) {
            float startY = in_positioningParams.startY + 0.5F;
            if (in_character.getY() != startY) {
                slideY(in_character, startY);
            }
        }
    }

    private void checkForFallingBack(IsoGridSquare sq, IsoGameCharacter owner) {
        for (int i = 0; i < sq.getMovingObjects().size(); i++) {
            IsoMovingObject movingObj = sq.getMovingObjects().get(i);
            IsoZombie zombie = Type.tryCastTo(movingObj, IsoZombie.class);
            if (zombie != null && !zombie.isOnFloor() && !zombie.isSitAgainstWall()) {
                if (!zombie.isVariable("AttackOutcome", "success") && Rand.Next(5 + owner.getPerkLevel(PerkFactory.Perks.Fitness)) != 0) {
                    owner.setVariable("ClimbWindowOutcome", "back");
                } else {
                    owner.setVariable("ClimbWindowOutcome", "fallback");
                }
            }
        }
    }

    private void checkForFallingFront(IsoGridSquare sq, IsoGameCharacter owner) {
        for (int i = 0; i < sq.getMovingObjects().size(); i++) {
            IsoMovingObject movingObj = sq.getMovingObjects().get(i);
            IsoZombie zombie = Type.tryCastTo(movingObj, IsoZombie.class);
            if (zombie != null && !zombie.isOnFloor() && !zombie.isSitAgainstWall() && zombie.isVariable("AttackOutcome", "success")) {
                owner.setVariable("ClimbWindowOutcome", "fallfront");
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setHideWeaponModel(false);
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isVariable("ClimbWindowOutcome", "fall")
            || owner.isVariable("ClimbWindowOutcome", "fallback")
            || owner.isVariable("ClimbWindowOutcome", "fallfront")) {
            owner.setHitReaction("");
        }

        owner.clearVariable("ClimbWindowFinished");
        owner.clearVariable("ClimbWindowOutcome");
        owner.clearVariable("ClimbWindowStarted");
        owner.clearVariable("ClimbWindowFlopped");
        owner.clearVariable("PlayerVoiceSound");
        if (owner instanceof IsoZombie) {
            owner.setOnFloor(false);
            owner.setKnockedDown(false);
        }

        if (owner instanceof IsoZombie zombie) {
            zombie.allowRepathDelay = 0.0F;
            if (StateMachineParams.get(PARAM_PREV_STATE) == PathFindState.instance()) {
                if (owner.getPathFindBehavior2().getTargetChar() == null) {
                    owner.setVariable("bPathFind", true);
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

        if (owner instanceof IsoZombie isoZombie) {
            isoZombie.networkAi.isClimbing = false;
        }

        Pool.tryRelease(StateMachineParams.get(PARAM_PARAMS));
        StateMachineParams.clear();
    }

    public static void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    public static void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (event.eventName.equalsIgnoreCase("CheckAttack") && zombie != null && zombie.target instanceof IsoGameCharacter isoGameCharacter) {
            isoGameCharacter.attackFromWindowsLunge(zombie);
        }

        if (event.eventName.equalsIgnoreCase("OnFloor") && zombie != null) {
            boolean bOnFloor = Boolean.parseBoolean(event.parameterValue);
            StateMachineParams.put(PARAM_ZOMBIE_ON_FLOOR, bOnFloor);
            if (bOnFloor) {
                this.setLungeXVars(zombie);
                IsoThumpable windows = Type.tryCastTo(this.getWindow(owner), IsoThumpable.class);
                if (windows != null && windows.getSquare() != null && zombie.target != null) {
                    windows.health = windows.health - Rand.Next(10, 20);
                    if (windows.health <= 0) {
                        windows.destroy();
                    }
                }

                owner.setVariable("ClimbWindowFlopped", true);
            }
        }

        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound")) {
            if (owner.getVariableBoolean("PlayerVoiceSound")) {
                return;
            }

            if (player == null) {
                return;
            }

            owner.setVariable("PlayerVoiceSound", true);
            player.playerVoiceSound(event.parameterValue);
        }

        if (event.eventName.equalsIgnoreCase("PlayWindowSound")) {
            if (!SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0F)) {
                return;
            }

            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), 9.0F);
        }

        if (event.eventName.equalsIgnoreCase("SetState")) {
            if (zombie == null) {
                return;
            }

            try {
                ParameterZombieState.State state = ParameterZombieState.State.valueOf(event.parameterValue);
                zombie.parameterZombieState.setState(state);
            } catch (IllegalArgumentException var10) {
            }
        }
    }

    /**
     * Description copied from class: State
     */
    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        ClimbThroughWindowPositioningParams positioningParams = (ClimbThroughWindowPositioningParams)StateMachineParams.get(PARAM_PARAMS);
        int startX = positioningParams.startX;
        int startY = positioningParams.startY;
        int endX = positioningParams.endX;
        int endY = positioningParams.endY;
        int z = positioningParams.z;
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

    public IsoObject getWindow(IsoGameCharacter owner) {
        if (!owner.isCurrentState(this)) {
            return null;
        } else {
            HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
            ClimbThroughWindowPositioningParams positioningParams = (ClimbThroughWindowPositioningParams)StateMachineParams.get(PARAM_PARAMS);
            int startX = positioningParams.startX;
            int startY = positioningParams.startY;
            int z = positioningParams.z;
            IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
            int endX = positioningParams.endX;
            int endY = positioningParams.endY;
            IsoGridSquare endSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
            if (startSq != null && endSq != null) {
                IsoObject obj = startSq.getWindowTo(endSq);
                if (obj == null) {
                    obj = startSq.getWindowThumpableTo(endSq);
                }

                if (obj == null) {
                    obj = startSq.getHoppableTo(endSq);
                }

                return obj;
            } else {
                return null;
            }
        }
    }

    public boolean isWindowClosing(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        ClimbThroughWindowPositioningParams positioningParams = (ClimbThroughWindowPositioningParams)StateMachineParams.get(PARAM_PARAMS);
        if (owner.getVariableBoolean("ClimbWindowStarted")) {
            return false;
        } else {
            int startX = positioningParams.startX;
            int startY = positioningParams.startY;
            int z = positioningParams.z;
            IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
            if (owner.getCurrentSquare() != startSq) {
                return false;
            } else if (this.getWindow(owner) instanceof IsoWindow window) {
                IsoGameCharacter chrClosing = window.getFirstCharacterClosing();
                if (chrClosing != null && chrClosing.isVariable("CloseWindowOutcome", "success")) {
                    if (owner.isZombie()) {
                        owner.setHitReaction("HeadLeft");
                    } else {
                        owner.setVariable("ClimbWindowFinished", true);
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
        boolean hasPath = owner.getPath2() != null;
        boolean isPlayer = owner instanceof IsoPlayer;
        if (hasPath && isPlayer) {
            modifiers.setMaxTurnDelta(2.0F);
        }

        if (isPlayer && owner.getVariableBoolean("isTurning")) {
            modifiers.setMaxTurnDelta(2.0F);
        }
    }

    public static boolean isFreeSquare(IsoGridSquare square) {
        return square != null && square.TreatAsSolidFloor() && !square.has(IsoFlagType.solid) && !square.has(IsoFlagType.solidtrans);
    }

    public static boolean isObstacleSquare(IsoGridSquare square) {
        return square != null
            && square.TreatAsSolidFloor()
            && !square.has(IsoFlagType.solid)
            && square.has(IsoFlagType.solidtrans)
            && !square.has(IsoFlagType.water);
    }

    public static IsoGridSquare getFreeSquareAfterObstacles(IsoGridSquare square, IsoDirections dir) {
        while (true) {
            IsoGridSquare square1 = square.getAdjacentSquare(dir);
            if (square1 == null || square.isSomethingTo(square1) || square.getWindowFrameTo(square1) != null || square.getWindowThumpableTo(square1) != null) {
                return null;
            }

            if (isFreeSquare(square1)) {
                return square1;
            }

            if (!isObstacleSquare(square1)) {
                return null;
            }

            square = square1;
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

    public boolean isPastInnerEdgeOfSquare(IsoGameCharacter owner, int x, int y, IsoDirections moveDir) {
        if (moveDir == IsoDirections.N) {
            return owner.getY() < y + 1 - 0.3F;
        } else if (moveDir == IsoDirections.S) {
            return owner.getY() > y + 0.3F;
        } else if (moveDir == IsoDirections.W) {
            return owner.getX() < x + 1 - 0.3F;
        } else if (moveDir == IsoDirections.E) {
            return owner.getX() > x + 0.3F;
        } else {
            throw new IllegalArgumentException("unhandled direction");
        }
    }

    public boolean isPastOuterEdgeOfSquare(IsoGameCharacter owner, int x, int y, IsoDirections moveDir) {
        if (moveDir == IsoDirections.N) {
            return owner.getY() < y - 0.3F;
        } else if (moveDir == IsoDirections.S) {
            return owner.getY() > y + 1 + 0.3F;
        } else if (moveDir == IsoDirections.W) {
            return owner.getX() < x - 0.3F;
        } else if (moveDir == IsoDirections.E) {
            return owner.getX() > x + 1 + 0.3F;
        } else {
            throw new IllegalArgumentException("unhandled direction");
        }
    }

    public void setParams(IsoGameCharacter owner, IsoObject obj) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        ClimbThroughWindowPositioningParams climbParams = (ClimbThroughWindowPositioningParams)StateMachineParams.computeIfAbsent(
            PARAM_PARAMS, key -> ClimbThroughWindowPositioningParams.alloc()
        );
        getClimbThroughWindowPositioningParams(owner, obj, climbParams);
        if (climbParams.windowObject == null) {
            throw new IllegalArgumentException("No valid climb-throuwh portal found. Expected thumpable, window, or window-frame");
        } else {
            StateMachineParams.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.FALSE);
            StateMachineParams.put(PARAM_PREV_STATE, climbParams.climbingCharacter.getCurrentState());
            StateMachineParams.put(PARAM_SCRATCHED, climbParams.scratch ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    public static void getClimbThroughWindowPositioningParams(
        IsoGameCharacter in_climbingCharacter, IsoObject in_windowObject, ClimbThroughWindowPositioningParams out_climbParams
    ) {
        boolean scratch = false;
        boolean north;
        if (in_windowObject instanceof IsoWindow window) {
            out_climbParams.canClimb = window.canClimbThrough(in_climbingCharacter);
            north = window.isNorth();
            if (in_climbingCharacter instanceof IsoPlayer && window.isDestroyed() && !window.isGlassRemoved() && Rand.Next(2) == 0) {
                scratch = true;
            }
        } else if (in_windowObject instanceof IsoThumpable thumpable) {
            out_climbParams.canClimb = thumpable.canClimbThrough(in_climbingCharacter);
            north = thumpable.north;
            if (in_climbingCharacter instanceof IsoPlayer && thumpable.getName().equals("Barbed Fence") && Rand.Next(101) > 75) {
                scratch = true;
            }
        } else {
            if (!(in_windowObject instanceof IsoWindowFrame windowFrame)) {
                out_climbParams.canClimb = false;
                out_climbParams.climbingCharacter = in_climbingCharacter;
                out_climbParams.windowObject = null;
                return;
            }

            out_climbParams.canClimb = true;
            north = windowFrame.getNorth();
        }

        int x = in_windowObject.getSquare().getX();
        int y = in_windowObject.getSquare().getY();
        int z = in_windowObject.getSquare().getZ();
        int startX = x;
        int startY = y;
        int oppositeX = x;
        int oppositeY = y;
        IsoDirections dir;
        if (north) {
            if (y < in_climbingCharacter.getY()) {
                oppositeY = y - 1;
                dir = IsoDirections.N;
            } else {
                startY = y - 1;
                dir = IsoDirections.S;
            }
        } else if (x < in_climbingCharacter.getX()) {
            oppositeX = x - 1;
            dir = IsoDirections.W;
        } else {
            startX = x - 1;
            dir = IsoDirections.E;
        }

        IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY, z);
        boolean isCounter = oppositeSq != null && oppositeSq.has(IsoFlagType.solidtrans);
        boolean isFloor = oppositeSq != null && oppositeSq.TreatAsSolidFloor();
        boolean isSheetRope = oppositeSq != null && in_climbingCharacter.canClimbDownSheetRope(oppositeSq);
        int endX = oppositeX;
        int endY = oppositeY;
        if (isCounter && in_climbingCharacter.isZombie()) {
            IsoGridSquare square = oppositeSq.getAdjacentSquare(dir);
            if (isFreeSquare(square)
                && !oppositeSq.isSomethingTo(square)
                && oppositeSq.getWindowFrameTo(square) == null
                && oppositeSq.getWindowThumpableTo(square) == null) {
                endX = square.x;
                endY = square.y;
            } else {
                isCounter = false;
            }
        }

        if (isCounter && !in_climbingCharacter.isZombie()) {
            IsoGridSquare freeSq = getFreeSquareAfterObstacles(oppositeSq, dir);
            if (freeSq == null) {
                isCounter = false;
            } else {
                endX = freeSq.x;
                endY = freeSq.y;
            }
        }

        out_climbParams.climbDir = dir;
        out_climbParams.climbingCharacter = in_climbingCharacter;
        out_climbParams.windowObject = in_windowObject;
        out_climbParams.startX = startX;
        out_climbParams.startY = startY;
        out_climbParams.z = z;
        out_climbParams.oppositeX = oppositeX;
        out_climbParams.oppositeY = oppositeY;
        out_climbParams.endX = endX;
        out_climbParams.endY = endY;
        out_climbParams.scratch = scratch;
        out_climbParams.isCounter = isCounter;
        out_climbParams.isFloor = isFloor;
        out_climbParams.isSheetRope = isSheetRope;
    }

    public ClimbThroughWindowPositioningParams getPositioningParams(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        return (ClimbThroughWindowPositioningParams)StateMachineParams.get(PARAM_PARAMS);
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_OUTCOME, owner.getVariableString("ClimbWindowOutcome"));
        } else {
            owner.setVariable("ClimbWindowOutcome", (String)StateMachineParams.getOrDefault(PARAM_OUTCOME, "success"));
        }

        super.setParams(owner, stage);
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
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        boolean scratched = (Boolean)delegate.getOrDefault(PARAM_SCRATCHED, false);
        if (scratched) {
            owner.getBodyDamage().setScratchedWindow();
        }
    }

    @Override
    public boolean canRagdoll(IsoGameCharacter owner) {
        return owner.getVariableBoolean("ClimbWindowStarted", false) ? false : !(owner instanceof IsoZombie ownerZombie && ownerZombie.isOnFloor());
    }
}
