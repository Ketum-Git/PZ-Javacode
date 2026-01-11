// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Anim2DBlendTriangle")
public final class Anim2DBlendTriangle {
    @XmlIDREF
    @XmlElement(name = "node1")
    public Anim2DBlend node1;
    @XmlIDREF
    @XmlElement(name = "node2")
    public Anim2DBlend node2;
    @XmlIDREF
    @XmlElement(name = "node3")
    public Anim2DBlend node3;

    public static double sign(float P1X, float P1Y, float P2X, float P2Y, float P3X, float P3Y) {
        return (P1X - P3X) * (P2Y - P3Y) - (P2X - P3X) * (P1Y - P3Y);
    }

    public static boolean PointInTriangle(float ptX, float ptY, float v1X, float v1Y, float v2X, float v2Y, float v3X, float v3Y) {
        boolean b1 = sign(ptX, ptY, v1X, v1Y, v2X, v2Y) < 0.0;
        boolean b2 = sign(ptX, ptY, v2X, v2Y, v3X, v3Y) < 0.0;
        boolean b3 = sign(ptX, ptY, v3X, v3Y, v1X, v1Y) < 0.0;
        return b1 == b2 && b2 == b3;
    }

    public boolean Contains(float x, float y) {
        return PointInTriangle(x, y, this.node1.posX, this.node1.posY, this.node2.posX, this.node2.posY, this.node3.posX, this.node3.posY);
    }
}
