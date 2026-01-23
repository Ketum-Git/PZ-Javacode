// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
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
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class ClimbDownSheetRopeState extends State {
    private static final float ClimbDownFallChanceScale = 300.0F;
    private static final ClimbDownSheetRopeState _instance = new ClimbDownSheetRopeState();
    private static final Integer PARAM_SPEED = 0;
    private int numberOfFallingChecks;

    public static ClimbDownSheetRopeState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter isoGameCharacter) {
        isoGameCharacter.setIgnoreMovement(true);
        isoGameCharacter.setHideWeaponModel(true);
        isoGameCharacter.setbClimbing(true);
        isoGameCharacter.setVariable("ClimbRope", true);
        this.setParams(isoGameCharacter, State.Stage.Enter);
        ClimbSheetRopeState.createClimbData(isoGameCharacter);
        calculateClimbDown(isoGameCharacter);
        this.numberOfFallingChecks = 0;
    }

    @Override
    public void execute(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        HashMap<Object, Object> stateMachineParams = isoGameCharacter.getStateMachineParams(this);
        ClimbSheetRopeState.applyIdealDirection(isoGameCharacter);
        float climbSpeed = isoGameCharacter.getClimbRopeSpeed(true);
        if (!isoGameCharacter.isLocal()) {
            climbSpeed = (Float)stateMachineParams.getOrDefault(PARAM_SPEED, isoGameCharacter.getClimbRopeSpeed(true));
        }

        isoGameCharacter.getSpriteDef().animFrameIncrease = climbSpeed;
        int minLevel = isoGameCharacter.getCurrentSquare().getChunk().getMinLevel();
        float currentClimbHeight = isoGameCharacter.getZ() - climbSpeed / 10.0F * GameTime.instance.getMultiplier();
        currentClimbHeight = Math.max(currentClimbHeight, (float)minLevel);
        isoGameCharacter.setZ(currentClimbHeight);
        if (currentClimbHeight <= climbData.targetClimbHeight) {
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
                (float)(0.007 * GameTime.instance.getMultiplier() * (isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) + 1))
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
            stateMachineParams.put(PARAM_SPEED, isoGameCharacter.getClimbRopeSpeed(true));
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

    private static void calculateClimbDown(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.setIdealDirection(isoGameCharacter);
        ClimbSheetRopeState.applyIdealDirection(isoGameCharacter);
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        int minLevel = isoGameCharacter.getCurrentSquare().getChunk().getMinLevel();
        IsoCell cell = IsoWorld.instance.getCell();

        for (int z = PZMath.fastfloor(isoGameCharacter.getZ()); z >= minLevel; z--) {
            IsoGridSquare isoGridSquare = cell.getGridSquare((double)isoGameCharacter.getX(), (double)isoGameCharacter.getY(), (double)z);
            if (isoGridSquare.has(IsoFlagType.solidtrans) || isoGridSquare.TreatAsSolidFloor()) {
                climbData.targetGridSquare = isoGridSquare;
                climbData.targetClimbHeight = z;
                break;
            }
        }

        fallChanceCalculation(isoGameCharacter);
    }

    private static float fallChanceCalculation(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        climbData.fallChance = isoGameCharacter.getClimbingFailChanceFloat() + 1.0F;
        isoGameCharacter.setClimbRopeTime(isoGameCharacter.getClimbRopeTime() + GameTime.instance.getMultiplier());
        climbData.fallChance *= 300.0F;
        climbData.fallChance = climbData.fallChance / (GameTime.instance.getMultiplier() < 1.0F ? 1.0F : (int)GameTime.instance.getMultiplier());
        return climbData.fallChance;
    }

    private void finishClimbing(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        HashMap<Object, Object> stateMachineParams = isoGameCharacter.getStateMachineParams(this);
        isoGameCharacter.setZ(climbData.targetClimbHeight);
        stateMachineParams.clear();
        isoGameCharacter.clearVariable("ClimbRope");
        isoGameCharacter.setCollidable(true);
    }
}
