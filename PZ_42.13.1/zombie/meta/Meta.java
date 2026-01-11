// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.meta;

import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.iso.IsoGridSquare;
import zombie.iso.areas.SafeHouse;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.list.PZArrayUtil;

public final class Meta {
    public static final Meta instance = new Meta();
    final ArrayList<IsoGridSquare> squaresProcessing = new ArrayList<>();
    private final ArrayList<IsoGridSquare> squaresSeen = new ArrayList<>(2000);
    private final TIntHashSet squaresSeenSet = new TIntHashSet();

    public void dealWithSquareSeen(IsoGridSquare square) {
        if (!GameClient.client) {
            if (square.hourLastSeen != (int)GameTime.getInstance().getWorldAgeHours()) {
                synchronized (this.squaresSeen) {
                    if (!this.squaresSeenSet.contains(square.getID())) {
                        this.squaresSeen.add(square);
                        this.squaresSeenSet.add(square.getID());
                    }
                }
            }
        }
    }

    public void dealWithSquareSeenActual(IsoGridSquare square) {
        if (!GameClient.client) {
            Zone z = square.zone;
            if (z != null) {
                z.setHourSeenToCurrent();
            }

            if (GameServer.server) {
                SafeHouse safe = SafeHouse.getSafeHouse(square);
                if (safe != null) {
                    safe.updateSafehouse(null);
                }
            }

            square.setHourSeenToCurrent();
        }
    }

    public void update() {
        if (!GameClient.client) {
            this.squaresProcessing.clear();
            synchronized (this.squaresSeen) {
                PZArrayUtil.addAll(this.squaresProcessing, this.squaresSeen);
                this.squaresSeen.clear();
                this.squaresSeenSet.clear();
            }

            for (int n = 0; n < this.squaresProcessing.size(); n++) {
                this.dealWithSquareSeenActual(this.squaresProcessing.get(n));
            }

            this.squaresProcessing.clear();
        }
    }
}
