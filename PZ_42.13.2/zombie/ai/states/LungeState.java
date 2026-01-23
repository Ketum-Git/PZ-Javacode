// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.util.Type;

@UsedFromLua
public final class LungeState extends State {
    private static final LungeState _instance = new LungeState();
    private final Vector2 temp = new Vector2();
    private static final Integer PARAM_TICK_COUNT = 0;

    public static LungeState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (System.currentTimeMillis() - zombie.lungeSoundTime > 5000L) {
            String t = zombie.getDescriptor().getVoicePrefix() + "Attack";
            if (GameServer.server) {
                GameServer.sendZombieSound(IsoZombie.ZombieSound.Lunge, zombie);
            }

            zombie.lungeSoundTime = System.currentTimeMillis();
        }

        zombie.lungeTimer = 180.0F;
        StateMachineParams.put(PARAM_TICK_COUNT, 0L);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zomb = (IsoZombie)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setOnFloor(false);
        owner.setShootable(true);
        if (zomb.lunger) {
            zomb.walkVariantUse = "ZombieWalk3";
        }

        zomb.lungeTimer = zomb.lungeTimer - GameTime.getInstance().getThirtyFPSMultiplier();
        IsoPlayer player = Type.tryCastTo(zomb.getTarget(), IsoPlayer.class);
        if (player != null && player.isGhostMode()) {
            zomb.lungeTimer = 0.0F;
        }

        if (zomb.lungeTimer < 0.0F) {
            zomb.lungeTimer = 0.0F;
        }

        if (zomb.lungeTimer <= 0.0F) {
            zomb.allowRepathDelay = 0.0F;
        }

        this.temp.x = zomb.vectorToTarget.x;
        this.temp.y = zomb.vectorToTarget.y;
        zomb.getZombieLungeSpeed();
        this.temp.normalize();
        zomb.setForwardDirection(this.temp);
        zomb.DirectionFromVector(this.temp);
        zomb.setForwardDirectionFromIsoDirection();
        zomb.setForwardDirection(this.temp);
        if (!zomb.isTargetLocationKnown()
            && zomb.lastTargetSeenX != -1
            && !owner.getPathFindBehavior2().isTargetLocation(zomb.lastTargetSeenX + 0.5F, zomb.lastTargetSeenY + 0.5F, zomb.lastTargetSeenZ)) {
            zomb.lungeTimer = 0.0F;
            owner.pathToLocation(zomb.lastTargetSeenX, zomb.lastTargetSeenY, zomb.lastTargetSeenZ);
        }

        long tickCount = (Long)StateMachineParams.get(PARAM_TICK_COUNT);
        if (tickCount == 2L) {
            ((IsoZombie)owner).parameterZombieState.setState(ParameterZombieState.State.LockTarget);
        }

        StateMachineParams.put(PARAM_TICK_COUNT, tickCount + 1L);
    }

    @Override
    public void exit(IsoGameCharacter chr) {
    }

    /**
     * Return TRUE if the owner is currently moving.
     *   Defaults to FALSE
     */
    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return true;
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
