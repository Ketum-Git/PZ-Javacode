// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.Type;

public final class MovingObjectUpdateSchedulerUpdateBucket {
    public int frameMod;
    ArrayList<IsoMovingObject>[] buckets;

    public MovingObjectUpdateSchedulerUpdateBucket(int mod) {
        this.init(mod);
    }

    public void init(int frameMod) {
        this.frameMod = frameMod;
        this.buckets = new ArrayList[frameMod];

        for (int i = 0; i < this.buckets.length; i++) {
            this.buckets[i] = new ArrayList<>();
        }
    }

    public void clear() {
        for (int i = 0; i < this.buckets.length; i++) {
            ArrayList<IsoMovingObject> bucket = this.buckets[i];
            bucket.clear();
        }
    }

    public void add(IsoMovingObject o) {
        int index = o.getID() % this.frameMod;
        this.buckets[index].add(o);
    }

    public void update(int frameCounter) {
        GameTime.getInstance().perObjectMultiplier = this.frameMod;
        ArrayList<IsoMovingObject> fullSimulation = this.buckets[frameCounter % this.frameMod];

        for (int i = 0; i < fullSimulation.size(); i++) {
            IsoMovingObject isoMovingObject = fullSimulation.get(i);
            if (isoMovingObject instanceof IsoDeadBody) {
                IsoWorld.instance.getCell().getRemoveList().add(isoMovingObject);
            } else {
                IsoZombie zombie = Type.tryCastTo(isoMovingObject, IsoZombie.class);
                if (zombie != null && VirtualZombieManager.instance.isReused(zombie)) {
                    DebugLog.log(DebugType.Zombie, "REUSABLE ZOMBIE IN MovingObjectUpdateSchedulerUpdateBucket IGNORED " + isoMovingObject);
                } else {
                    isoMovingObject.preupdate();
                    isoMovingObject.update();
                }
            }
        }

        GameTime.getInstance().perObjectMultiplier = 1.0F;
    }

    public void postupdate(int frameCounter) {
        GameTime.getInstance().perObjectMultiplier = this.frameMod;
        ArrayList<IsoMovingObject> fullSimulation = this.buckets[frameCounter % this.frameMod];

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

    public void updateAnimation(int frameCounter) {
        GameTime.getInstance().perObjectMultiplier = this.frameMod;
        ArrayList<IsoMovingObject> fullSimulation = this.buckets[frameCounter % this.frameMod];

        for (int i = 0; i < fullSimulation.size(); i++) {
            IsoMovingObject isoMovingObject = fullSimulation.get(i);
            IsoZombie zombie = Type.tryCastTo(isoMovingObject, IsoZombie.class);
            if (zombie != null && VirtualZombieManager.instance.isReused(zombie)) {
                DebugLog.log(DebugType.Zombie, "REUSABLE ZOMBIE IN MovingObjectUpdateSchedulerUpdateBucket IGNORED " + isoMovingObject);
            } else {
                try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("Update Anim")) {
                    isoMovingObject.updateAnimation();
                }
            }
        }

        GameTime.getInstance().perObjectMultiplier = 1.0F;
    }

    public void removeObject(IsoMovingObject object) {
        for (int i = 0; i < this.buckets.length; i++) {
            ArrayList<IsoMovingObject> bucket = this.buckets[i];
            bucket.remove(object);
        }
    }

    public ArrayList<IsoMovingObject> getBucket(int frameCounter) {
        return this.buckets[frameCounter % this.frameMod];
    }
}
