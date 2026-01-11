// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

@UsedFromLua
public final class ClimbSheetRopeState extends State {
    public static final float FallChanceBase = 1.0F;
    public static final float FallChanceMultiplier = 10.0F;
    private static final float FallChanceScale = 100.0F;
    public static final float ClimbSpeed = 0.16F;
    public static final float ClimbSlowdown = 0.5F;
    private static final ClimbSheetRopeState _instance = new ClimbSheetRopeState();
    private static final Integer PARAM_SPEED = 0;
    private int numberOfFallingChecks;

    public static ClimbSheetRopeState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter isoGameCharacter) {
        isoGameCharacter.setIgnoreMovement(true);
        isoGameCharacter.setHideWeaponModel(true);
        isoGameCharacter.setbClimbing(true);
        isoGameCharacter.setVariable("ClimbRope", true);
        this.setParams(isoGameCharacter, State.Stage.Enter);
        createClimbData(isoGameCharacter);
        calculateClimb(isoGameCharacter);
        this.numberOfFallingChecks = 0;
    }

    @Override
    public void execute(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        HashMap<Object, Object> stateMachineParams = isoGameCharacter.getStateMachineParams(this);
        applyIdealDirection(isoGameCharacter);
        float climbSpeed = isoGameCharacter.getClimbRopeSpeed(false);
        if (!isoGameCharacter.isLocal()) {
            climbSpeed = (Float)stateMachineParams.getOrDefault(PARAM_SPEED, isoGameCharacter.getClimbRopeSpeed(false));
        }

        isoGameCharacter.getSpriteDef().animFrameIncrease = climbSpeed;
        float currentClimbHeight = isoGameCharacter.getZ() + climbSpeed / 10.0F * GameTime.instance.getMultiplier();
        isoGameCharacter.setZ(currentClimbHeight);
        if (!(currentClimbHeight >= climbData.targetFallHeight) && currentClimbHeight > climbData.targetClimbHeight) {
            this.finishClimbing(isoGameCharacter);
        } else {
            fallChanceCalculation(isoGameCharacter);
            boolean canFallCheck = isoGameCharacter.getClimbRopeTime() > climbData.fallChance * 10.0F;
            boolean fall = Rand.NextBool((int)climbData.fallChance);
            if (canFallCheck) {
                DebugLog.Action.println("Checking For Fall #%d", this.numberOfFallingChecks++);
            }

            if (!IsoWindow.isSheetRopeHere(isoGameCharacter.getCurrentSquare())) {
                isoGameCharacter.setCollidable(true);
                isoGameCharacter.setbClimbing(false);
                isoGameCharacter.setbFalling(true);
                isoGameCharacter.clearVariable("ClimbRope");
            } else if (canFallCheck && !SandboxOptions.instance.easyClimbing.getValue() && fall && isoGameCharacter.isLocal()) {
                isoGameCharacter.fallFromRope();
            }

            float skillFactor = (
                    isoGameCharacter.getPerkLevel(PerkFactory.Perks.Nimble)
                        + Math.max(isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength), isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness)) * 2
                )
                / 3.0F;
            isoGameCharacter.addBothArmMuscleStrain(
                (float)(0.02 * GameTime.instance.getMultiplier() * (isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) + 1))
                    * ((15.0F - skillFactor) / 10.0F)
                    * (GameTime.instance.getMultiplier() / 0.8F)
            );
            if (isoGameCharacter instanceof IsoPlayer isoPlayer && isoPlayer.isLocalPlayer()) {
                isoPlayer.dirtyRecalcGridStackTime = 2.0F;
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter isoGameCharacter) {
        isoGameCharacter.setCollidable(true);
        isoGameCharacter.setIgnoreMovement(false);
        isoGameCharacter.setHideWeaponModel(false);
        isoGameCharacter.setbClimbing(false);
        isoGameCharacter.clearVariable("ClimbRope");
        this.setParams(isoGameCharacter, State.Stage.Exit);
    }

    @Override
    public void setParams(IsoGameCharacter isoGameCharacter, State.Stage stage) {
        HashMap<Object, Object> stateMachineParams = isoGameCharacter.getStateMachineParams(this);
        if (isoGameCharacter.isLocal()) {
            stateMachineParams.put(PARAM_SPEED, isoGameCharacter.getClimbRopeSpeed(false));
        }

        super.setParams(isoGameCharacter, stage);
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
        return true;
    }

    public static void createClimbData(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter.getClimbData() == null) {
            isoGameCharacter.setClimbData(new ClimbSheetRopeState.ClimbData());
        }
    }

    private static ClimbSheetRopeState.ClimbStatus calculateClimbOutcome(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        IsoGridSquare exitDirectionIsqGridSquare = climbData.targetGridSquare.getAdjacentSquare(isoGameCharacter.dir);
        if (!exitDirectionIsqGridSquare.TreatAsSolidFloor()) {
            return ClimbSheetRopeState.ClimbStatus.Blocked;
        } else {
            IsoWindow window = climbData.targetGridSquare.getWindowTo(exitDirectionIsqGridSquare);
            if (window != null) {
                if (!window.IsOpen()) {
                    window.ToggleWindow(isoGameCharacter);
                }

                if (!window.canClimbThrough(isoGameCharacter)) {
                    return ClimbSheetRopeState.ClimbStatus.Blocked;
                } else {
                    climbData.climbTargetIsoObject = window;
                    return ClimbSheetRopeState.ClimbStatus.OpenWindow;
                }
            } else {
                IsoThumpable isoThumpable = climbData.targetGridSquare.getWindowThumpableTo(exitDirectionIsqGridSquare);
                if (isoThumpable != null) {
                    if (!isoThumpable.canClimbThrough(isoGameCharacter)) {
                        return ClimbSheetRopeState.ClimbStatus.Blocked;
                    } else {
                        climbData.climbTargetIsoObject = isoThumpable;
                        return ClimbSheetRopeState.ClimbStatus.OpenWindow;
                    }
                } else {
                    isoThumpable = climbData.targetGridSquare.getHoppableThumpableTo(exitDirectionIsqGridSquare);
                    if (isoThumpable != null) {
                        return !IsoWindow.canClimbThroughHelper(
                                isoGameCharacter,
                                climbData.targetGridSquare,
                                exitDirectionIsqGridSquare,
                                isoGameCharacter.dir == IsoDirections.N || isoGameCharacter.dir == IsoDirections.S
                            )
                            ? ClimbSheetRopeState.ClimbStatus.Blocked
                            : ClimbSheetRopeState.ClimbStatus.Fence;
                    } else {
                        IsoWindowFrame isoWindowFrame = climbData.targetGridSquare.getWindowFrameTo(exitDirectionIsqGridSquare);
                        if (isoWindowFrame != null) {
                            if (!isoWindowFrame.canClimbThrough(isoGameCharacter)) {
                                return ClimbSheetRopeState.ClimbStatus.Blocked;
                            } else {
                                climbData.climbTargetIsoObject = isoWindowFrame;
                                return ClimbSheetRopeState.ClimbStatus.WindowFrame;
                            }
                        } else {
                            IsoObject hoppableWall = climbData.targetGridSquare.getWallHoppableTo(exitDirectionIsqGridSquare);
                            if (hoppableWall == null) {
                                return ClimbSheetRopeState.ClimbStatus.Undefined;
                            } else {
                                return !IsoWindow.canClimbThroughHelper(
                                        isoGameCharacter,
                                        climbData.targetGridSquare,
                                        exitDirectionIsqGridSquare,
                                        isoGameCharacter.dir == IsoDirections.N || isoGameCharacter.dir == IsoDirections.S
                                    )
                                    ? ClimbSheetRopeState.ClimbStatus.Blocked
                                    : ClimbSheetRopeState.ClimbStatus.Fence;
                            }
                        }
                    }
                }
            }
        }
    }

    private void finishClimbing(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        isoGameCharacter.setZ(climbData.targetClimbHeight);
        isoGameCharacter.setCurrent(climbData.targetGridSquare);
        isoGameCharacter.setCollidable(true);
        switch (climbData.exitBlocked) {
            case Undefined:
            default:
                break;
            case Blocked:
                isoGameCharacter.climbDownSheetRope();
                break;
            case OpenWindow:
                isoGameCharacter.climbThroughWindow(climbData.climbTargetIsoObject);
                break;
            case WindowFrame:
                isoGameCharacter.climbThroughWindowFrame((IsoWindowFrame)climbData.climbTargetIsoObject);
                break;
            case Fence:
                isoGameCharacter.climbOverFence(isoGameCharacter.dir);
        }
    }

    public static void setIdealDirection(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetN)
            || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopN)) {
            isoGameCharacter.setDir(IsoDirections.N);
            climbData.idealx = 0.54F;
            climbData.idealy = 0.39F;
        }

        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetS)
            || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopS)) {
            isoGameCharacter.setDir(IsoDirections.S);
            climbData.idealx = 0.118F;
            climbData.idealy = 0.5756F;
        }

        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetW)
            || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopW)) {
            isoGameCharacter.setDir(IsoDirections.W);
            climbData.idealx = 0.4F;
            climbData.idealy = 0.7F;
        }

        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetE)
            || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopE)) {
            isoGameCharacter.setDir(IsoDirections.E);
            climbData.idealx = 0.5417F;
            climbData.idealy = 0.3144F;
        }
    }

    public static void applyIdealDirection(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        float ox = isoGameCharacter.getX() - PZMath.fastfloor(isoGameCharacter.getX());
        float oy = isoGameCharacter.getY() - PZMath.fastfloor(isoGameCharacter.getY());
        if (ox != climbData.idealx) {
            float dif = (climbData.idealx - ox) / 4.0F;
            ox += dif;
            isoGameCharacter.setX(PZMath.fastfloor(isoGameCharacter.getX()) + ox);
        }

        if (oy != climbData.idealy) {
            float dif = (climbData.idealy - oy) / 4.0F;
            oy += dif;
            isoGameCharacter.setY(PZMath.fastfloor(isoGameCharacter.getY()) + oy);
        }

        isoGameCharacter.setNextX(isoGameCharacter.getX());
        isoGameCharacter.setNextY(isoGameCharacter.getY());
    }

    private static void calculateClimb(IsoGameCharacter isoGameCharacter) {
        setIdealDirection(isoGameCharacter);
        applyIdealDirection(isoGameCharacter);
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        int maxLevel = isoGameCharacter.getCurrentSquare().getChunk().getMaxLevel();
        IsoCell cell = IsoWorld.instance.getCell();

        for (int z = PZMath.fastfloor(isoGameCharacter.getZ()); z <= maxLevel; z++) {
            IsoGridSquare isoGridSquare = cell.getGridSquare((double)isoGameCharacter.getX(), (double)isoGameCharacter.getY(), (double)z);
            if (IsoWindow.isTopOfSheetRopeHere(isoGridSquare)) {
                climbData.targetGridSquare = isoGridSquare;
                climbData.targetClimbHeight = z;
                climbData.exitBlocked = calculateClimbOutcome(isoGameCharacter);
                break;
            }
        }

        fallChanceCalculation(isoGameCharacter);
    }

    private static float fallChanceCalculation(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        climbData.fallChance = isoGameCharacter.getClimbingFailChanceFloat() + 1.0F;
        isoGameCharacter.setClimbRopeTime(isoGameCharacter.getClimbRopeTime() + GameTime.instance.getMultiplier());
        climbData.fallChance *= 100.0F;
        climbData.fallChance = climbData.fallChance / (GameTime.instance.getMultiplier() < 1.0F ? 1.0F : (int)GameTime.instance.getMultiplier());
        return climbData.fallChance;
    }

    public void debug(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData debugClimbData = isoGameCharacter.getClimbData();
        IsoGridSquare currentIsoGridSquare = isoGameCharacter.getCurrentSquare();
        if (currentIsoGridSquare.haveSheetRope) {
            IndieGL.glBlendFunc(770, 771);
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            int sx = (int)IsoUtils.XToScreenExact(currentIsoGridSquare.getX(), currentIsoGridSquare.getY(), currentIsoGridSquare.getZ(), 0);
            int sy = (int)IsoUtils.YToScreenExact(currentIsoGridSquare.getX(), currentIsoGridSquare.getY(), currentIsoGridSquare.getZ() + 1.5F, 0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Climb Sheet Rope").append("\n");
            stringBuilder.append("Fail Chance: ").append(isoGameCharacter.getClimbingFailChanceFloat()).append("\n");
            stringBuilder.append("Fall Chance: ").append(debugClimbData.fallChance).append("\n");
            stringBuilder.append("Distance: ").append(debugClimbData.targetClimbHeight + 1).append("\n");
            stringBuilder.append("Climbing Skills:").append("\n");
            float chance = isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength) * 2.0F;
            stringBuilder.append(
                String.format("   %s: %d %.2f%%%n", PerkFactory.Perks.Strength.name, isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength), chance)
            );
            chance = isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness) * 2.0F;
            stringBuilder.append(
                String.format("   %s: %d %.2f%%%n", PerkFactory.Perks.Fitness.name, isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness), chance)
            );
            chance = isoGameCharacter.getPerkLevel(PerkFactory.Perks.Nimble) * 2.0F;
            stringBuilder.append(
                String.format("   %s: %d %.2f%%%n", PerkFactory.Perks.Nimble.name, isoGameCharacter.getPerkLevel(PerkFactory.Perks.Nimble), chance)
            );
            stringBuilder.append("Climbing Bonus:").append("\n");
            chance = !isoGameCharacter.isWearingAwkwardGloves() && isoGameCharacter.isWearingGloves() ? 4.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", "Wearing Gloves", chance));
            chance = isoGameCharacter.hasTrait(CharacterTrait.DEXTROUS) ? 4.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.DEXTROUS.toString(), chance));
            chance = isoGameCharacter.hasTrait(CharacterTrait.BURGLAR) ? 4.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.BURGLAR.toString(), chance));
            chance = isoGameCharacter.hasTrait(CharacterTrait.GYMNAST) ? 4.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.GYMNAST.toString(), chance));
            stringBuilder.append("Climbing Penalty:").append("\n");
            chance = isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * -5.0F;
            stringBuilder.append(
                String.format("   %s: %d %.2f%%%n", MoodleType.ENDURANCE.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.ENDURANCE), chance)
            );
            chance = isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.DRUNK) * -8.0F;
            stringBuilder.append(
                String.format("   %s: %d %.2f%%%n", MoodleType.DRUNK.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.DRUNK), chance)
            );
            chance = isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * -8.0F;
            stringBuilder.append(
                String.format(
                    "   %s: %d %.2f%%%n", MoodleType.HEAVY_LOAD.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD), chance
                )
            );
            chance = isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.PAIN) * -5.0F;
            stringBuilder.append(
                String.format("   %s: %d %.2f%%%n", MoodleType.PAIN.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.PAIN), chance)
            );
            chance = isoGameCharacter.hasTrait(CharacterTrait.OBESE) ? -25.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.OBESE.toString(), chance));
            chance = isoGameCharacter.hasTrait(CharacterTrait.OVERWEIGHT) ? -15.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.OVERWEIGHT.toString(), chance));
            chance = isoGameCharacter.hasTrait(CharacterTrait.CLUMSY) ? 2.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %s%n", CharacterTrait.CLUMSY.toString(), chance == 2.0F ? "Half" : ""));
            chance = isoGameCharacter.isWearingAwkwardGloves() ? 2.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %s%n", "Wearing Awkward Gloves", chance == 2.0F ? "Half" : ""));
            chance = isoGameCharacter.hasTrait(CharacterTrait.ALL_THUMBS) ? -4.0F : 0.0F;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.ALL_THUMBS.toString(), chance));
            chance = isoGameCharacter.nearbyZombieClimbPenalty();
            stringBuilder.append(String.format("   %s: %.2f%%%n", "Nearby Zombies", chance));
            TextManager.instance.DrawString(UIFont.NewMedium, sx, sy, 1.5, stringBuilder.toString(), 1.0, 1.0, 1.0, 1.0);
        }
    }

    public static class ClimbData {
        public int targetClimbHeight;
        public float fallChance;
        public float idealx;
        public float idealy;
        public float targetFallHeight = Float.MAX_VALUE;
        public IsoObject climbTargetIsoObject;
        public IsoGridSquare targetGridSquare;
        public ClimbSheetRopeState.ClimbStatus exitBlocked;
    }

    public static enum ClimbStatus {
        Undefined,
        Blocked,
        OpenWindow,
        WindowFrame,
        Fence;
    }
}
