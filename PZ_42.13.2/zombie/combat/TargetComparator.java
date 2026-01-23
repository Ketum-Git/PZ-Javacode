// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.combat;

import java.util.Comparator;
import zombie.core.physics.BallisticsController;
import zombie.network.fields.hit.HitInfo;

public class TargetComparator implements Comparator<HitInfo> {
    protected BallisticsController ballisticsController;

    public void setBallisticsController(BallisticsController ballisticsController) {
        this.ballisticsController = ballisticsController;
    }

    public int compare(HitInfo o1, HitInfo o2) {
        return 0;
    }
}
