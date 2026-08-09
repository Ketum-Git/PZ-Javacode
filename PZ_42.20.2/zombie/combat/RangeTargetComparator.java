// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.combat;

import zombie.ai.states.ZombieGetUpState;
import zombie.characters.IsoZombie;
import zombie.network.fields.hit.HitInfo;
import zombie.util.Type;

public class RangeTargetComparator extends TargetComparator {
    @Override
    public int compare(HitInfo o1, HitInfo o2) {
        float dist1 = o1.distSq;
        float dist2 = o2.distSq;
        IsoZombie zombie1 = Type.tryCastTo(o1.getObject(), IsoZombie.class);
        IsoZombie zombie2 = Type.tryCastTo(o2.getObject(), IsoZombie.class);
        if (zombie1 != null && zombie2 != null) {
            boolean prone1 = zombie1.isProne();
            boolean prone2 = zombie2.isProne();
            boolean getUp1 = zombie1.isCurrentState(ZombieGetUpState.instance());
            boolean getUp2 = zombie2.isCurrentState(ZombieGetUpState.instance());
            if (getUp1 && !getUp2 && prone2) {
                return -1;
            }

            if (!getUp1 && prone1 && getUp2) {
                return 1;
            }

            if (prone1 && prone2) {
                if (zombie1.isCrawling() && !zombie2.isCrawling()) {
                    return -1;
                }

                if (!zombie1.isCrawling() && zombie2.isCrawling()) {
                    return 1;
                }
            }
        }

        if (dist1 > dist2) {
            return 1;
        } else {
            return dist2 > dist1 ? -1 : 0;
        }
    }
}
