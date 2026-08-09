// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math.interpolators.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import zombie.core.math.IInterpolator;
import zombie.core.math.interpolators.LerpEaseInQuad;
import zombie.core.math.interpolators.LerpEaseOutInQuad;
import zombie.core.math.interpolators.LerpEaseOutQuad;
import zombie.core.math.interpolators.LerpLinear;
import zombie.core.math.interpolators.LerpOne;
import zombie.core.math.interpolators.LerpPolyline;
import zombie.core.math.interpolators.LerpSequence;
import zombie.core.math.interpolators.LerpZero;

@XmlType(name = "InterpolatorSlot")
public class InterpolatorSlot {
    @XmlElements(
        {
                @XmlElement(name = "EaseInQuad", type = LerpEaseInQuad.class),
                @XmlElement(name = "EaseOutInQuad", type = LerpEaseOutInQuad.class),
                @XmlElement(name = "EaseOutQuad", type = LerpEaseOutQuad.class),
                @XmlElement(name = "Linear", type = LerpLinear.class),
                @XmlElement(name = "One", type = LerpOne.class),
                @XmlElement(name = "Polyline", type = LerpPolyline.class),
                @XmlElement(name = "Sequence", type = LerpSequence.class),
                @XmlElement(name = "Zero", type = LerpZero.class)
        }
    )
    public IInterpolator interpolator;
}
