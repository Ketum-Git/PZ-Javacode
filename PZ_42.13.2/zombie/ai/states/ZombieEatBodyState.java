// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.iso.IsoMovingObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ZombieEatBodyState extends State {
    private static final ZombieEatBodyState _instance = new ZombieEatBodyState();

    public static ZombieEatBodyState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.setStateEventDelayTimer(Rand.Next(1800.0F, 3600.0F));
        zombie.setVariable("onknees", Rand.Next(3) != 0);
        if (zombie.getEatBodyTarget() instanceof IsoDeadBody) {
            IsoDeadBody bodyToEat = (IsoDeadBody)zombie.eatBodyTarget;
            if (!zombie.isEatingOther(bodyToEat)) {
                HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
                StateMachineParams.put(0, bodyToEat);
                bodyToEat.getEatingZombies().add(zombie);
            }

            if (GameClient.client && zombie.isLocal()) {
                GameClient.sendEatBody(zombie, zombie.getEatBodyTarget());
            }
        } else if (zombie.getEatBodyTarget() instanceof IsoPlayer && GameClient.client && zombie.isLocal()) {
            GameClient.sendEatBody(zombie, zombie.getEatBodyTarget());
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoMovingObject targetChar = zombie.getEatBodyTarget();
        if (zombie.getStateEventDelayTimer() <= 0.0F) {
            zombie.setEatBodyTarget(null, false);
        } else if (!GameServer.server && !Core.soundDisabled && Rand.Next(Rand.AdjustForFramerate(15)) == 0) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Eating);
        }

        zombie.timeSinceSeenFlesh = 0.0F;
        if (targetChar != null) {
            zombie.faceThisObject(targetChar);
        }

        if (Rand.Next(Rand.AdjustForFramerate(450)) == 0) {
            zombie.getCurrentSquare()
                .getChunk()
                .addBloodSplat(zombie.getX() + Rand.Next(-0.5F, 0.5F), zombie.getY() + Rand.Next(-0.5F, 0.5F), zombie.getZ(), Rand.Next(8));
            if (Rand.Next(6) == 0) {
                new IsoZombieGiblets(
                    IsoZombieGiblets.GibletType.B,
                    zombie.getCell(),
                    zombie.getX(),
                    zombie.getY(),
                    zombie.getZ() + 0.3F,
                    Rand.Next(-0.2F, 0.2F) * 1.5F,
                    Rand.Next(-0.2F, 0.2F) * 1.5F
                );
            } else {
                new IsoZombieGiblets(
                    IsoZombieGiblets.GibletType.A,
                    zombie.getCell(),
                    zombie.getX(),
                    zombie.getY(),
                    zombie.getZ() + 0.3F,
                    Rand.Next(-0.2F, 0.2F) * 1.5F,
                    Rand.Next(-0.2F, 0.2F) * 1.5F
                );
            }

            if (Rand.Next(4) == 0) {
                zombie.addBlood(null, true, false, false);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (StateMachineParams.get(0) instanceof IsoDeadBody) {
            ((IsoDeadBody)StateMachineParams.get(0)).getEatingZombies().remove(zombie);
        }

        if (zombie.parameterZombieState.isState(ParameterZombieState.State.Eating)) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
        }

        if (GameClient.client && zombie.isLocal()) {
            GameClient.sendEatBody(zombie, null);
        }
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
