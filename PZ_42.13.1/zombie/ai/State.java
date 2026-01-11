// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.Map;
import zombie.characters.IsoGameCharacter;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;

public abstract class State implements IAnimEventListener, IAnimEventWrappedBroadcaster {
    private final AnimEventBroadcaster animEventBroadcaster = new AnimEventBroadcaster();

    public void enter(IsoGameCharacter owner) {
        DebugLog.Multiplayer.noise(this.getName());
    }

    public void execute(IsoGameCharacter owner) {
    }

    public void exit(IsoGameCharacter owner) {
        DebugLog.Multiplayer.noise(this.getName());
    }

    @Override
    public AnimEventBroadcaster getAnimEventBroadcaster() {
        return this.animEventBroadcaster;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.getAnimEventBroadcaster().animEvent(owner, layer, track, event);
    }

    /**
     * Return TRUE if the owner is currently attacking.
     *   Defaults to FALSE
     */
    public boolean isAttacking(IsoGameCharacter owner) {
        return false;
    }

    /**
     * Return TRUE if the owner is currently moving.
     *   Defaults to FALSE
     */
    public boolean isMoving(IsoGameCharacter owner) {
        return false;
    }

    /**
     * @return TRUE if this state handles the "Cancel Action" key or the B controller button.
     */
    public boolean isDoingActionThatCanBeCancelled() {
        return false;
    }

    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
    }

    /**
     * Return TRUE if the owner should ignore collisions when passing between two squares.
     *  Defaults to FALSE
     */
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return false;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        DebugLog.Multiplayer.trace("%s %s: %s", this.getName(), stage, owner.getStateMachineParams(this));
    }

    public boolean isSyncOnEnter() {
        return false;
    }

    public boolean isSyncOnExit() {
        return false;
    }

    public boolean isSyncOnSquare() {
        return false;
    }

    public boolean isSyncInIdle() {
        return false;
    }

    public boolean canRagdoll(IsoGameCharacter owner) {
        return true;
    }

    public boolean isProcessedOnEnter() {
        return false;
    }

    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
    }

    public boolean isProcessedOnExit() {
        return false;
    }

    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
    }

    public static enum Stage {
        Enter,
        Execute,
        Exit;
    }
}
