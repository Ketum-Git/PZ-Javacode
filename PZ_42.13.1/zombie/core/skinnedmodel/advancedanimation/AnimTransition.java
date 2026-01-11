// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import zombie.core.skinnedmodel.animation.BoneAxis;

@XmlType(name = "AnimTransition")
public final class AnimTransition {
    @XmlElement(name = "m_Source")
    public String source;
    @XmlElement(name = "m_Target")
    public String target;
    @XmlElement(name = "m_AnimName")
    public String animName;
    @XmlElement(name = "m_DeferredBoneName")
    public String deferredBoneName;
    @XmlElement(name = "m_deferredBoneAxis")
    public BoneAxis deferredBoneAxis = BoneAxis.Y;
    @XmlElement(name = "m_useDeferedRotation")
    public boolean useDeferedRotation;
    @XmlElement(name = "m_useDeferredMovement")
    public boolean useDeferredMovement = true;
    @XmlElement(name = "m_deferredRotationScale")
    public float deferredRotationScale = 1.0F;
    @XmlElement(name = "m_SyncAdjustTime")
    public float syncAdjustTime;
    @XmlElement(name = "m_blendInTime")
    public float blendInTime = Float.POSITIVE_INFINITY;
    @XmlElement(name = "m_blendOutTime")
    public float blendOutTime = Float.POSITIVE_INFINITY;
    @XmlElement(name = "m_speedScale")
    public float speedScale = Float.POSITIVE_INFINITY;
    @XmlTransient
    private boolean isParsed;
    @XmlElement(name = "m_Conditions")
    public AnimCondition[] conditions = new AnimCondition[0];

    public void parse(AnimNode in_fromNode, AnimNode in_toNode) {
        if (!this.isParsed) {
            for (AnimCondition condition : this.conditions) {
                condition.parse(in_fromNode, in_toNode);
            }

            this.isParsed = true;
        }
    }
}
