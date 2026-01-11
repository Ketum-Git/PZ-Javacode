// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.gameStates.IngameState;
import zombie.iso.Vector2;
import zombie.util.Type;

public class LungeNetworkState extends State {
    private static final LungeNetworkState INSTANCE = new LungeNetworkState();
    private final Vector2 temp = new Vector2();
    private final Vector3f worldPos = new Vector3f();
    private static final Integer PARAM_TICK_COUNT = 0;

    public static LungeNetworkState instance() {
        return INSTANCE;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        WalkTowardNetworkState.instance().enter(owner);
        IsoZombie zombie = (IsoZombie)owner;
        zombie.lungeTimer = 180.0F;
        StateMachineParams.put(PARAM_TICK_COUNT, IngameState.instance.numberTicks);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        WalkTowardNetworkState.instance().execute(owner);
        IsoZombie zombie = (IsoZombie)owner;
        owner.setOnFloor(false);
        owner.setShootable(true);
        if (zombie.lunger) {
            zombie.walkVariantUse = "ZombieWalk3";
        }

        zombie.lungeTimer = zombie.lungeTimer - GameTime.getInstance().getThirtyFPSMultiplier();
        IsoPlayer player = Type.tryCastTo(zombie.getTarget(), IsoPlayer.class);
        if (player != null && player.isGhostMode()) {
            zombie.lungeTimer = 0.0F;
        }

        if (zombie.lungeTimer < 0.0F) {
            zombie.lungeTimer = 0.0F;
        }

        if (zombie.lungeTimer <= 0.0F) {
            zombie.allowRepathDelay = 0.0F;
        }

        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        long tickCount = (Long)StateMachineParams.get(PARAM_TICK_COUNT);
        if (IngameState.instance.numberTicks - tickCount == 2L) {
            ((IsoZombie)owner).parameterZombieState.setState(ParameterZombieState.State.LockTarget);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        WalkTowardNetworkState.instance().exit(owner);
    }

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
