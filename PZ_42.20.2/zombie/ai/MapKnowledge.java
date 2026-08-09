// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;

@UsedFromLua
public final class MapKnowledge {
    private final ArrayList<KnownBlockedEdges> knownBlockedEdges = new ArrayList<>();

    public ArrayList<KnownBlockedEdges> getKnownBlockedEdges() {
        return this.knownBlockedEdges;
    }

    public KnownBlockedEdges getKnownBlockedEdges(int x, int y, int z) {
        for (int i = 0; i < this.knownBlockedEdges.size(); i++) {
            KnownBlockedEdges kbe = this.knownBlockedEdges.get(i);
            if (kbe.x == x && kbe.y == y && kbe.z == z) {
                return kbe;
            }
        }

        return null;
    }

    private KnownBlockedEdges createKnownBlockedEdges(int x, int y, int z) {
        assert this.getKnownBlockedEdges(x, y, z) == null;

        KnownBlockedEdges kbe = KnownBlockedEdges.alloc();
        kbe.init(x, y, z);
        this.knownBlockedEdges.add(kbe);
        return kbe;
    }

    public KnownBlockedEdges getOrCreateKnownBlockedEdges(int x, int y, int z) {
        KnownBlockedEdges kbe = this.getKnownBlockedEdges(x, y, z);
        if (kbe == null) {
            kbe = this.createKnownBlockedEdges(x, y, z);
        }

        return kbe;
    }

    private void releaseIfEmpty(KnownBlockedEdges kbe) {
        if (!kbe.n && !kbe.w) {
            this.knownBlockedEdges.remove(kbe);
            kbe.release();
        }
    }

    public void setKnownBlockedEdgeW(int x, int y, int z, boolean blocked) {
        KnownBlockedEdges kbe = this.getOrCreateKnownBlockedEdges(x, y, z);
        kbe.w = blocked;
        this.releaseIfEmpty(kbe);
    }

    public void setKnownBlockedEdgeN(int x, int y, int z, boolean blocked) {
        KnownBlockedEdges kbe = this.getOrCreateKnownBlockedEdges(x, y, z);
        kbe.n = blocked;
        this.releaseIfEmpty(kbe);
    }

    public void setKnownBlockedDoor(IsoDoor object, boolean blocked) {
        IsoGridSquare square = object.getSquare();
        if (object.getNorth()) {
            this.setKnownBlockedEdgeN(square.x, square.y, square.z, blocked);
        } else {
            this.setKnownBlockedEdgeW(square.x, square.y, square.z, blocked);
        }
    }

    public void setKnownBlockedDoor(IsoThumpable object, boolean blocked) {
        if (object.isDoor()) {
            IsoGridSquare square = object.getSquare();
            if (object.getNorth()) {
                this.setKnownBlockedEdgeN(square.x, square.y, square.z, blocked);
            } else {
                this.setKnownBlockedEdgeW(square.x, square.y, square.z, blocked);
            }
        }
    }

    public void setKnownBlockedWindow(IsoWindow object, boolean blocked) {
        IsoGridSquare square = object.getSquare();
        if (object.getNorth()) {
            this.setKnownBlockedEdgeN(square.x, square.y, square.z, blocked);
        } else {
            this.setKnownBlockedEdgeW(square.x, square.y, square.z, blocked);
        }
    }

    public void setKnownBlockedWindowFrame(IsoWindowFrame object, boolean blocked) {
        IsoGridSquare square = object.getSquare();
        if (object.getNorth()) {
            this.setKnownBlockedEdgeN(square.x, square.y, square.z, blocked);
        } else {
            this.setKnownBlockedEdgeW(square.x, square.y, square.z, blocked);
        }
    }

    public void forget() {
        KnownBlockedEdges.releaseAll(this.knownBlockedEdges);
        this.knownBlockedEdges.clear();
    }
}
