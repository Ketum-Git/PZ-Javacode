// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Animation extends OptionGroup {
    public final Animation.AnimLayerOG animLayer = this.newOptionGroup(new Animation.AnimLayerOG());
    public final Animation.SharedSkelesOG sharedSkeles = this.newOptionGroup(new Animation.SharedSkelesOG());
    public final BooleanDebugOption dancingDoors = this.newDebugOnlyOption("DancingDoors", false);
    public final BooleanDebugOption debug = this.newDebugOnlyOption("Debug", false);
    public final BooleanDebugOption disableRagdolls = this.newDebugOnlyOption("DisableRagdolls", false);
    public final BooleanDebugOption allowEarlyTransitionOut = this.newDebugOnlyOption("AllowEarlyTransitionOut", true);
    public final BooleanDebugOption animRenderPicker = this.newDebugOnlyOption("Render.Picker", false);
    public final BooleanDebugOption blendUseFbx = this.newDebugOnlyOption("BlendUseFbx", false);
    public final BooleanDebugOption disableAnimationBlends = this.newDebugOnlyOption("DisableAnimationBlends", false);

    public static final class AnimLayerOG extends OptionGroup {
        public final BooleanDebugOption logStateChanges = this.newDebugOnlyOption("Debug.LogStateChanges", false);
        public final BooleanDebugOption allowAnimNodeOverride = this.newDebugOnlyOption("Debug.AllowAnimNodeOverride", false);
        public final BooleanDebugOption logNodeConditions = this.newDebugOnlyOption("Debug.LogNodeConditions", false);
    }

    public static final class SharedSkelesOG extends OptionGroup {
        public final BooleanDebugOption enabled = this.newDebugOnlyOption("Enabled", true);
        public final BooleanDebugOption allowLerping = this.newDebugOnlyOption("AllowLerping", true);
    }
}
