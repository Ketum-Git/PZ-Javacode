// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.core.physics.RagdollBodyPart;

public class PhysicsHitReaction {
    public boolean useImpulseOverride;
    public AmmoType ammoType;
    public String physicsObject;
    public float overrideForwardImpulse = 80.0F;
    public float overrideUpwardImpulse = 40.0F;
    public float[] impulse = new float[RagdollBodyPart.BODYPART_COUNT.ordinal()];
    public float[] upwardImpulse = new float[RagdollBodyPart.BODYPART_COUNT.ordinal()];
}
