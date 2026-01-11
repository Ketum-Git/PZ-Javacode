// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import java.util.Map;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.ai.State;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.scripting.objects.MoodleType;
import zombie.util.Type;

@UsedFromLua
public final class ClimbOverWallState extends State {
    private static final ClimbOverWallState _instance = new ClimbOverWallState();
    static final Integer PARAM_START_X = 0;
    static final Integer PARAM_START_Y = 1;
    static final Integer PARAM_Z = 2;
    static final Integer PARAM_END_X = 3;
    static final Integer PARAM_END_Y = 4;
    static final Integer PARAM_DIR = 5;
    static final Integer PARAM_STRUGGLE = 6;
    static final Integer PARAM_SUCCESS = 7;
    static final int FENCE_TYPE_WOOD = 0;
    static final int FENCE_TYPE_METAL = 1;
    static final int FENCE_TYPE_METAL_BARS = 2;

    public static ClimbOverWallState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 1200.0));
        IsoPlayer player = (IsoPlayer)owner;
        boolean struggle = player.isClimbOverWallStruggle();
        if (struggle) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 500.0));
        }

        boolean success = player.isClimbOverWallSuccess();
        owner.setVariable("ClimbFenceFinished", false);
        owner.setVariable("ClimbFenceStarted", false);
        if (owner.isLocal()) {
            owner.setVariable("ClimbFenceOutcome", success ? "success" : "fail");
            owner.setVariable("ClimbFenceStruggle", struggle);
        } else {
            owner.setVariable("ClimbFenceOutcome", StateMachineParams.get(PARAM_SUCCESS) ? "success" : "fail");
            owner.setVariable("ClimbFenceStruggle", (Boolean)StateMachineParams.get(PARAM_STRUGGLE));
        }

        if (player.isLocalPlayer()) {
            player.triggerMusicIntensityEvent("ClimbWall");
        }

        if (!success && owner.getPathFindBehavior2() != null) {
            owner.getPathFindBehavior2().reset();
            owner.getPathFindBehavior2().cancel();
            owner.setPath2(null);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoDirections dir = (IsoDirections)StateMachineParams.get(PARAM_DIR);
        owner.setAnimated(true);
        owner.setDir(dir);
        float skillFactor = (owner.getPerkLevel(PerkFactory.Perks.Nimble) + owner.getPerkLevel(PerkFactory.Perks.Strength) * 2) / 3.0F;
        owner.addBothArmMuscleStrain(
            (float)(0.02 * GameTime.instance.getMultiplier() * (owner.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) + 1))
                * ((15.0F - skillFactor) / 10.0F)
                * (GameTime.instance.getMultiplier() / 0.8F)
        );
        boolean ClimbFenceStarted = owner.getVariableBoolean("ClimbFenceStarted");
        if (!ClimbFenceStarted) {
            int startX = (Integer)StateMachineParams.get(PARAM_START_X);
            int startY = (Integer)StateMachineParams.get(PARAM_START_Y);
            float dxy = 0.15F;
            float idealX = owner.getX();
            float idealY = owner.getY();
            switch (dir) {
                case N:
                    idealY = startY + 0.15F;
                    break;
                case S:
                    idealY = startY + 1 - 0.15F;
                    break;
                case W:
                    idealX = startX + 0.15F;
                    break;
                case E:
                    idealX = startX + 1 - 0.15F;
            }

            float mult = GameTime.getInstance().getThirtyFPSMultiplier() / 8.0F;
            owner.setX(owner.getX() + (idealX - owner.getX()) * mult);
            owner.setY(owner.getY() + (idealY - owner.getY()) * mult);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("ClimbingFence");
        owner.clearVariable("ClimbFenceFinished");
        owner.clearVariable("ClimbFenceOutcome");
        owner.clearVariable("ClimbFenceStarted");
        owner.clearVariable("ClimbFenceStruggle");
        owner.clearVariable("PlayerVoiceSound");
        owner.setIgnoreMovement(false);
        owner.setHideWeaponModel(false);
        if (owner instanceof IsoZombie isoZombie) {
            isoZombie.networkAi.isClimbing = false;
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("PlayFenceSound")) {
            IsoObject fence = this.getFence(owner);
            if (fence == null) {
                return;
            }

            int fenceType = this.getFenceType(fence);
            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("FenceTypeHigh"), fenceType);
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
    }

    /**
     * Description copied from class: State
     */
    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (!(Boolean)StateMachineParams.get(PARAM_SUCCESS)) {
            return false;
        } else {
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
    }

    private IsoObject getClimbableWallN(IsoGridSquare square) {
        IsoObject[] objects = square.getObjects().getElements();
        int i = 0;

        for (int n = square.getObjects().size(); i < n; i++) {
            IsoObject object = objects[i];
            PropertyContainer props = object.getProperties();
            if (props != null
                && !props.has(IsoFlagType.CantClimb)
                && object.getType() == IsoObjectType.wall
                && props.has(IsoFlagType.collideN)
                && !props.has(IsoFlagType.HoppableN)) {
                return object;
            }
        }

        return null;
    }

    private IsoObject getClimbableWallW(IsoGridSquare square) {
        IsoObject[] objects = square.getObjects().getElements();
        int i = 0;

        for (int n = square.getObjects().size(); i < n; i++) {
            IsoObject object = objects[i];
            PropertyContainer props = object.getProperties();
            if (props != null
                && !props.has(IsoFlagType.CantClimb)
                && object.getType() == IsoObjectType.wall
                && props.has(IsoFlagType.collideW)
                && !props.has(IsoFlagType.HoppableW)) {
                return object;
            }
        }

        return null;
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
        if (startSq != null && endSq != null) {
            IsoDirections dir = (IsoDirections)StateMachineParams.get(PARAM_DIR);

            return switch (dir) {
                case N -> this.getClimbableWallN(startSq);
                case S -> this.getClimbableWallN(endSq);
                case W -> this.getClimbableWallW(startSq);
                case E -> this.getClimbableWallW(endSq);
                default -> null;
            };
        } else {
            return null;
        }
    }

    private int getFenceType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return 0;
        } else {
            PropertyContainer props = fence.getSprite().getProperties();
            String typeStr = props.get("FenceTypeHigh");
            if (typeStr != null) {
                return switch (typeStr) {
                    case "Wood" -> 0;
                    case "Metal" -> 1;
                    case "MetalGate" -> 2;
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

        StateMachineParams.put(PARAM_START_X, x);
        StateMachineParams.put(PARAM_START_Y, y);
        StateMachineParams.put(PARAM_Z, z);
        StateMachineParams.put(PARAM_END_X, endX);
        StateMachineParams.put(PARAM_END_Y, endY);
        StateMachineParams.put(PARAM_DIR, dir);
        IsoPlayer player = (IsoPlayer)owner;
        if (player.isLocalPlayer()) {
            if (SandboxOptions.instance.easyClimbing.getValue()) {
                player.setClimbOverWallStruggle(false);
                player.setClimbOverWallSuccess(true);
            } else {
                int struggleChance = owner.getClimbingFailChanceInt();
                DebugLog.log("ClimbWall actual struggleChance 1 in " + struggleChance / 2);
                boolean struggle = Rand.NextBool(struggleChance / 2);
                if ("Tutorial".equals(Core.gameMode)) {
                    struggle = false;
                }

                DebugLog.log("ClimbWall struggle? " + struggle);
                DebugLog.log("ClimbWall failure chance 1 in " + struggleChance);
                boolean success = false;
                if (struggleChance > 0) {
                    success = !Rand.NextBool(struggleChance);
                } else if (owner.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) == 0) {
                    int chance = Math.max(1, owner.getPerkLevel(PerkFactory.Perks.Strength));
                    DebugLog.log("ClimbWall bonus " + (chance + 1) + " of success when base chance is 0 when encumbered");
                    success = Rand.Next(100) <= chance;
                }

                DebugLog.log("ClimbWall success? " + success);
                player.setClimbOverWallStruggle(struggle);
                player.setClimbOverWallSuccess(success);
            }
        }

        StateMachineParams.put(PARAM_STRUGGLE, player.isClimbOverWallStruggle());
        StateMachineParams.put(PARAM_SUCCESS, player.isClimbOverWallSuccess());
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
        owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 1200.0));
        if (delegate.get(PARAM_STRUGGLE).equals(Boolean.TRUE)) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 500.0));
        }
    }
}
