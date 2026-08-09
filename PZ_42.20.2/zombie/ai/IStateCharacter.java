// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import zombie.characters.IsoGameCharacter;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public interface IStateCharacter {
    void changeState(State var1);

    State getCurrentState();

    boolean isCurrentState(State var1);

    default boolean hasCurrentState() {
        return this.getCurrentState() != null;
    }

    default boolean isCurrentStateAttacking() {
        return this.hasCurrentState() && this.getCurrentState().isAttacking(Type.tryCastTo(this, IsoGameCharacter.class));
    }

    default boolean isCurrentStateMoving() {
        return this.hasCurrentState() && this.getCurrentState().isMoving(Type.tryCastTo(this, IsoGameCharacter.class));
    }

    default boolean isDoingActionThatCanBeCancelled() {
        return this.hasCurrentState() && this.getCurrentState().isDoingActionThatCanBeCancelled();
    }

    default boolean canBeHitByVehicle(BaseVehicle impactingVehicle) {
        return this.hasCurrentState() && this.getCurrentState().canBeHitByVehicle(Type.tryCastTo(this, IsoGameCharacter.class), impactingVehicle);
    }

    default boolean causesDamageToVehicleWhenHit(BaseVehicle impactingVehicle) {
        return this.hasCurrentState() && this.getCurrentState().causesDamageToVehicleWhenHit(Type.tryCastTo(this, IsoGameCharacter.class), impactingVehicle);
    }

    default boolean canSlowDownVehicleWhenHit(BaseVehicle impactingVehicle) {
        return this.hasCurrentState() && this.getCurrentState().canSlowDownVehicleWhenHit(Type.tryCastTo(this, IsoGameCharacter.class), impactingVehicle);
    }

    default boolean canCurrentStateRagdoll() {
        return this.hasCurrentState() && this.getCurrentState().canRagdoll(Type.tryCastTo(this, IsoGameCharacter.class));
    }
}
