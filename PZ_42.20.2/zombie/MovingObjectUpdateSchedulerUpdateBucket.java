// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoZombie;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class MovingObjectUpdateSchedulerUpdateBucket {
    private final UpdateSchedulerSimulationLevel simulationLevel;
    private final int frameMod;
    private final List<IsoMovingObject>[] buckets;

    public MovingObjectUpdateSchedulerUpdateBucket(UpdateSchedulerSimulationLevel simulationLevel) {
        this.simulationLevel = simulationLevel;
        this.frameMod = simulationLevel.getFrameMod();
        this.buckets = PZArrayUtil.newInstance(List.class, this.frameMod, ArrayList::new);
    }

    public void clear() {
        for (List<IsoMovingObject> bucket : this.buckets) {
            bucket.clear();
        }
    }

    public void add(IsoMovingObject o) {
        int index = o.getID() % this.frameMod;
        this.buckets[index].add(o);
    }

    public void update(int frameCounter) {
        GameTime.getInstance().perObjectMultiplier = this.frameMod;
        List<IsoMovingObject> fullSimulation = this.buckets[frameCounter % this.frameMod];

        for (int i = 0; i < fullSimulation.size(); i++) {
            IsoMovingObject isoMovingObject = fullSimulation.get(i);
            if (isoMovingObject instanceof IsoDeadBody) {
                IsoWorld.instance.getCell().getRemoveList().add(isoMovingObject);
            } else {
                IsoZombie zombie = Type.tryCastTo(isoMovingObject, IsoZombie.class);
                if (zombie != null && VirtualZombieManager.instance.isReused(zombie)) {
                    DebugLog.log(DebugType.Zombie, "REUSABLE ZOMBIE IN MovingObjectUpdateSchedulerUpdateBucket IGNORED " + isoMovingObject);
                } else {
                    isoMovingObject.setCurrentSimulationLevel(this.simulationLevel);
                    isoMovingObject.preupdate();
                    isoMovingObject.frameStep();
                    isoMovingObject.update();
                }
            }
        }

        GameTime.getInstance().perObjectMultiplier = 1.0F;
    }

    public void postupdate(int frameCounter) {
        GameTime.getInstance().perObjectMultiplier = this.frameMod;
        List<IsoMovingObject> fullSimulation = this.buckets[frameCounter % this.frameMod];

        for (int i = 0; i < fullSimulation.size(); i++) {
            IsoMovingObject isoMovingObject = fullSimulation.get(i);
            IsoZombie zombie = Type.tryCastTo(isoMovingObject, IsoZombie.class);
            if (zombie != null && VirtualZombieManager.instance.isReused(zombie)) {
                DebugLog.log(DebugType.Zombie, "REUSABLE ZOMBIE IN MovingObjectUpdateSchedulerUpdateBucket IGNORED " + isoMovingObject);
            } else {
                isoMovingObject.postupdate();
            }
        }

        GameTime.getInstance().perObjectMultiplier = 1.0F;
    }

    public void removeObject(IsoMovingObject object) {
        for (int i = 0; i < this.buckets.length; i++) {
            List<IsoMovingObject> bucket = this.buckets[i];
            bucket.remove(object);
        }
    }

    public List<IsoMovingObject> getBucket(int frameCounter) {
        return this.buckets[frameCounter % this.frameMod];
    }
}
