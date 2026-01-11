// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Model extends OptionGroup {
    public final Model.RenderOG render = this.newOptionGroup(new Model.RenderOG());
    public final BooleanDebugOption forceSkeleton = this.newOption("Force.Skeleton", false);

    public static final class RenderOG extends OptionGroup {
        public final BooleanDebugOption limitTextureSize = this.newOption("LimitTextureSize", true);
        public final BooleanDebugOption attachments = this.newOption("Attachments", false);
        public final BooleanDebugOption axis = this.newOption("Axis", false);
        public final BooleanDebugOption bones = this.newOption("Bones", false);
        public final BooleanDebugOption bounds = this.newOption("Bounds", false);
        public final BooleanDebugOption forceAlphaOne = this.newDebugOnlyOption("ForceAlphaOne", false);
        public final BooleanDebugOption lights = this.newOption("Lights", false);
        public final BooleanDebugOption muzzleFlash = this.newOption("MuzzleFlash", false);
        public final BooleanDebugOption skipVehicles = this.newOption("SkipVehicles", false);
        public final BooleanDebugOption weaponHitPoint = this.newOption("WeaponHitPoint", false);
        public final BooleanDebugOption wireframe = this.newOption("Wireframe", false);
    }
}
