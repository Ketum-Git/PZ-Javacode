// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Anim2DBlend")
public final class Anim2DBlend {
    @XmlElement(name = "m_AnimName")
    public String animName = "";
    @XmlElement(name = "m_XPos")
    public float posX;
    @XmlElement(name = "m_YPos")
    public float posY;
    @XmlElement(name = "m_SpeedScale")
    public float speedScale = 1.0F;
    @XmlAttribute(name = "referenceID")
    @XmlID
    public String referenceId;
}
