// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import zombie.debug.DebugLog;

public final class ParticlesArray<E> extends ArrayList<E> {
    private boolean needToUpdate;
    private int particleSystemsCount = 0;
    private int particleSystemsLast = 0;

    public ParticlesArray() {
        this.needToUpdate = true;
    }

    public synchronized int addParticle(E p) {
        if (p == null) {
            return -1;
        } else if (this.size() == this.particleSystemsCount) {
            this.add(p);
            this.particleSystemsCount++;
            this.needToUpdate = true;
            return this.size() - 1;
        } else {
            for (int i = this.particleSystemsLast; i < this.size(); i++) {
                if (this.get(i) == null) {
                    this.particleSystemsLast = i;
                    this.set(i, p);
                    this.particleSystemsCount++;
                    this.needToUpdate = true;
                    return i;
                }
            }

            for (int ix = 0; ix < this.particleSystemsLast; ix++) {
                if (this.get(ix) == null) {
                    this.particleSystemsLast = ix;
                    this.set(ix, p);
                    this.particleSystemsCount++;
                    this.needToUpdate = true;
                    return ix;
                }
            }

            DebugLog.log("ERROR: ParticlesArray.addParticle has unknown error");
            return -1;
        }
    }

    public synchronized boolean deleteParticle(int k) {
        if (k >= 0 && k < this.size() && this.get(k) != null) {
            this.set(k, null);
            this.particleSystemsCount--;
            this.needToUpdate = true;
            return true;
        } else {
            return false;
        }
    }

    public synchronized void defragmentParticle() {
        this.needToUpdate = false;
        if (this.particleSystemsCount != this.size() && this.size() != 0) {
            int pointernull = -1;

            for (int i = 0; i < this.size(); i++) {
                if (this.get(i) == null) {
                    pointernull = i;
                    break;
                }
            }

            for (int ix = this.size() - 1; ix >= 0; ix--) {
                if (this.get(ix) != null) {
                    this.set(pointernull, this.get(ix));
                    this.set(ix, null);

                    for (int k = pointernull; k < this.size(); k++) {
                        if (this.get(k) == null) {
                            pointernull = k;
                            break;
                        }
                    }

                    if (pointernull + 1 >= ix) {
                        this.particleSystemsLast = pointernull;
                        break;
                    }
                }
            }
        }
    }

    public synchronized int getCount() {
        return this.particleSystemsCount;
    }

    public synchronized boolean getNeedToUpdate() {
        return this.needToUpdate;
    }
}
