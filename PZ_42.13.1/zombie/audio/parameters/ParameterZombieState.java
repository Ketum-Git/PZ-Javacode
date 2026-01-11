// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoZombie;

public final class ParameterZombieState extends FMODLocalParameter {
    private final IsoZombie zombie;
    private ParameterZombieState.State state = ParameterZombieState.State.Idle;

    public ParameterZombieState(IsoZombie zombie) {
        super("ZombieState");
        this.zombie = zombie;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.zombie.target == null) {
            if (this.state == ParameterZombieState.State.SearchTarget) {
                this.setState(ParameterZombieState.State.Idle);
            }
        } else if (this.state == ParameterZombieState.State.Idle) {
            this.setState(ParameterZombieState.State.SearchTarget);
        }

        return this.state.index;
    }

    public void setState(ParameterZombieState.State state) {
        if (state != this.state) {
            this.state = state;
        }
    }

    public boolean isState(ParameterZombieState.State state) {
        return this.state == state;
    }

    public static enum State {
        Idle(0),
        Eating(1),
        SearchTarget(2),
        LockTarget(3),
        AttackScratch(4),
        AttackLacerate(5),
        AttackBite(6),
        Hit(7),
        Death(8),
        Reanimate(9),
        Pushed(10),
        GettingUp(11),
        Attack(12),
        RunOver(13);

        final int index;

        private State(final int index) {
            this.index = index;
        }
    }
}
