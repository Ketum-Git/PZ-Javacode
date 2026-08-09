// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UpdateSchedulerSimulationLevel;
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
    private static final LungeState INSTANCE = new LungeState();
    public static final State.Param<Long> TICK_COUNT = State.Param.ofLong("tick_count", 0L);
    private final Vector2 temp = new Vector2();

    public static LungeState instance() {
        return INSTANCE;
    }

    private LungeState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (System.currentTimeMillis() - zombie.lungeSoundTime > 5000L) {
            String t = zombie.getDescriptor().getVoicePrefix() + "Attack";
            if (GameServer.server) {
                GameServer.sendZombieSound(IsoZombie.ZombieSound.Lunge, zombie);
            }

            zombie.lungeSoundTime = System.currentTimeMillis();
        }

        zombie.lungeTimer = 180.0F;
        owner.set(TICK_COUNT, 0L);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zomb = (IsoZombie)owner;
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
        if (!zomb.isTargetLocationKnown()
            && zomb.lastTargetSeenX != -1
            && !owner.getPathFindBehavior2().isTargetLocation(zomb.lastTargetSeenX + 0.5F, zomb.lastTargetSeenY + 0.5F, zomb.lastTargetSeenZ)) {
            zomb.lungeTimer = 0.0F;
            owner.pathToLocation(zomb.lastTargetSeenX, zomb.lastTargetSeenY, zomb.lastTargetSeenZ);
        }

        long tickCount = owner.get(TICK_COUNT);
        if (tickCount == 2L) {
            ((IsoZombie)owner).parameterZombieState.setState(ParameterZombieState.State.LockTarget);
        }

        owner.set(TICK_COUNT, tickCount + 1L);
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
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.HALF;
    }
}
