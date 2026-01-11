// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math.interpolators;

import javax.xml.bind.annotation.XmlRootElement;
import zombie.core.math.IInterpolator;

@XmlRootElement
public class LerpOne extends IInterpolator {
    public static final LerpOne instance = new LerpOne();

    @Override
    public float lerp(float in_alpha) {
        return 1.0F;
    }
}
