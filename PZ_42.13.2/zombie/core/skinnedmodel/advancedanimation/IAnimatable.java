// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.characters.action.ActionContext;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.ModelInstance;

public interface IAnimatable extends IAnimationVariableSource {
    ActionContext getActionContext();

    default boolean canTransitionToState(String in_stateName) {
        ActionContext actionContext = this.getActionContext();
        return actionContext != null && actionContext.canTransitionToState(in_stateName);
    }

    AnimationPlayer getAnimationPlayer();

    AnimationPlayerRecorder getAnimationPlayerRecorder();

    boolean isAnimationRecorderActive();

    AdvancedAnimator getAdvancedAnimator();

    ModelInstance getModelInstance();

    String GetAnimSetName();

    String getUID();

    default short getOnlineID() {
        return -1;
    }

    boolean hasAnimationPlayer();

    IGrappleable getGrappleable();
}
