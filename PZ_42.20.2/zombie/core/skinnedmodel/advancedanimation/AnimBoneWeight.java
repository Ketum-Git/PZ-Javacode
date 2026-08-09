// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "AnimBoneWeight")
public final class AnimBoneWeight {
    @XmlElement(name = "boneName")
    public String boneName;
    @XmlElement(name = "weight")
    public float weight = 1.0F;
    @XmlElement(name = "includeDescendants")
    public boolean includeDescendants = true;

    public AnimBoneWeight() {
    }

    public AnimBoneWeight(String boneName, float weight) {
        this.boneName = boneName;
        this.weight = weight;
        this.includeDescendants = true;
    }
}
