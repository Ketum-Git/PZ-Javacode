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

    public static double sign(float p1X, float p1Y, float p2X, float p2Y, float p3X, float p3Y) {
        return (p1X - p3X) * (p2Y - p3Y) - (p2X - p3X) * (p1Y - p3Y);
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
