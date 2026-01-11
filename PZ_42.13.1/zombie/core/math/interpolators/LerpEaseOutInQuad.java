// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math.interpolators;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;

@XmlRootElement
public class LerpEaseOutInQuad extends IInterpolator {
    public static final LerpEaseOutInQuad instance = new LerpEaseOutInQuad();
    @XmlAttribute(name = "y0")
    public float startValue;
    @XmlAttribute(name = "y1")
    public float endValue = 1.0F;

    @Override
    public float lerp(float in_alpha) {
        return PZMath.lerp(this.startValue, this.endValue, PZMath.lerpFunc_EaseOutInQuad(in_alpha));
    }
}
