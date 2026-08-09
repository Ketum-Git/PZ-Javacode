// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import zombie.characters.IsoGameCharacter;
import zombie.vehicles.BaseVehicle;

public interface IStateFlagsSource {
    default boolean isAttacking(IsoGameCharacter owner) {
        return false;
    }

    default boolean isMoving(IsoGameCharacter owner) {
        return false;
    }

    default boolean isDoingActionThatCanBeCancelled() {
        return false;
    }

    default boolean canBeHitByVehicle(IsoGameCharacter owner, BaseVehicle impactingVehicle) {
        return true;
    }

    default boolean causesDamageToVehicleWhenHit(IsoGameCharacter owner, BaseVehicle impactingVehicle) {
        return true;
    }

    default boolean canSlowDownVehicleWhenHit(IsoGameCharacter owner, BaseVehicle impactingVehicle) {
        return true;
    }

    default boolean canRagdoll(IsoGameCharacter owner) {
        return true;
    }

    default boolean isSyncOnEnter() {
        return false;
    }

    default boolean isSyncOnExit() {
        return false;
    }

    default boolean isSyncOnSquare() {
        return false;
    }

    default boolean isSyncInIdle() {
        return false;
    }
}
