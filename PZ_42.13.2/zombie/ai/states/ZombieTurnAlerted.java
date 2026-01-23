// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;

public final class ZombieTurnAlerted extends State {
    private static final ZombieTurnAlerted _instance = new ZombieTurnAlerted();
    public static final Integer PARAM_TARGET_ANGLE = 0;

    public static ZombieTurnAlerted instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        float targetAngle = (Float)StateMachineParams.get(PARAM_TARGET_ANGLE);
        owner.getAnimationPlayer().setTargetAngle(targetAngle);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.pathToSound(owner.getPathTargetX(), owner.getPathTargetY(), owner.getPathTargetZ());
        ((IsoZombie)owner).alerted = false;
    }

    public void setParams(IsoGameCharacter owner, float targetAngle) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.clear();
        StateMachineParams.put(PARAM_TARGET_ANGLE, targetAngle);
    }
}
