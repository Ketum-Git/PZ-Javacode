// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import gnu.trove.set.hash.THashSet;
import imgui.ImGui;
import java.util.List;
import zombie.core.physics.BallisticsTarget;
import zombie.util.IPooledObject;
import zombie.util.Pool;

public class BallisticsTargetPanel extends PZDebugWindow {
    @Override
    public String getTitle() {
        return "Ballistics Targets";
    }

    @Override
    protected void doWindowContents() {
        ImGui.begin(this.getTitle(), 64);
        if (PZImGui.collapsingHeader("Ballistics Target Pool")) {
            Pool<IPooledObject> ballisticsTargetPool = BallisticsTarget.getBallisticsTargetPool();
            Pool.PoolStacks ballisticsTargetPoolStacks = ballisticsTargetPool.getPoolStacks().get();
            THashSet<IPooledObject> ballisticsTargetAllocated = ballisticsTargetPoolStacks.getInUse();
            List<IPooledObject> ballistictsTargetReleased = ballisticsTargetPoolStacks.getReleased();

            for (IPooledObject pooledObject : ballisticsTargetAllocated) {
                BallisticsTarget ballisticsTarget = (BallisticsTarget)pooledObject;
                if (ballisticsTarget != null) {
                    int id = ballisticsTarget.getID();
                    if (PZImGui.collapsingHeader("InUse: " + Integer.toString(id))) {
                    }
                }
            }

            for (IPooledObject pooledObjectx : ballistictsTargetReleased) {
                BallisticsTarget ballisticsTarget = (BallisticsTarget)pooledObjectx;
                if (ballisticsTarget != null) {
                    int id = ballisticsTarget.getID();
                    if (PZImGui.collapsingHeader("Released: " + Integer.toString(id))) {
                    }
                }
            }
        }

        ImGui.end();
    }
}
