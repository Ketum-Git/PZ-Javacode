// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.iso.Vector3;

public class RagdollBodyPartInfo {
    public int part;
    public boolean calculateLength;
    public float radius;
    public float height;
    public float gap;
    public int shape;
    public float mass;
    public Vector3 offset = new Vector3(0.0F, 0.0F, 0.0F);
    public boolean defaultCalculateLength;
    public float defaultRadius;
    public float defaultHeight;
    public float defaultGap;
    public int defaultShape;
    public float defaultMass;
    public Vector3 defaultOffset = new Vector3(0.0F, 0.0F, 0.0F);
}
