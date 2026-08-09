// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

public class NodeWeightEntry {
    public float weight;
    public int nodeId;
    public int layerIdx;

    public boolean isEmpty() {
        return this.weight < 1.0E-5F;
    }

    public void reset() {
        this.weight = 0.0F;
        this.layerIdx = 0;
    }
}
