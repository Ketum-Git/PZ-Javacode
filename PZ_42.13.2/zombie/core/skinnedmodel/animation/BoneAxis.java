// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType(name = "BoneAxis")
public enum BoneAxis {
    @XmlEnumValue("Y")
    Y,
    @XmlEnumValue("Z")
    Z;
}
