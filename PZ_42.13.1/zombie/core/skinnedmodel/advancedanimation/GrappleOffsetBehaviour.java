// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType(name = "GrappleOffsetBehavior")
public enum GrappleOffsetBehaviour {
    @XmlEnumValue("None")
    NONE,
    @XmlEnumValue("Grappled")
    GRAPPLED,
    @XmlEnumValue("Grappled_TweenOutToNone")
    GRAPPLED_TWEEN_OUT_TO_NONE,
    @XmlEnumValue("Grappler")
    GRAPPLER,
    @XmlEnumValue("None_TweenInGrappler")
    NONE_TWEEN_IN_GRAPPLER;
}
