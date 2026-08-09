// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import zombie.popman.ObjectPool;

public final class DepthStagger {
    private final TIntObjectHashMap<DepthStagger.Billboard> billboards = new TIntObjectHashMap<>();
    private final TIntArrayList overlap = new TIntArrayList();
    private final ObjectPool<DepthStagger.Billboard> pool = new ObjectPool<>(DepthStagger.Billboard::new, "DepthStagger.pool");

    public DepthStagger() {
        this.billboards.setAutoCompactionFactor(0.0F);
    }

    private int calculateOffset(DepthStagger.Billboard billboard, DepthStagger.Billboard head) {
        this.overlap.clear();

        for (DepthStagger.Billboard walk = head; walk != null; walk = walk.next) {
            if (billboard.screenX < walk.screenX + walk.width && billboard.screenX + billboard.width > walk.screenX) {
                this.overlap.add(walk.offset);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (!this.overlap.contains(i)) {
                return i;
            }

            if (!this.overlap.contains(-i)) {
                return -i;
            }
        }

        throw new RuntimeException("too much overlap");
    }

    public void startFrame() {
        this.billboards.forEachValue(billboard -> {
            billboard.release(this.pool);
            return true;
        });
        this.billboards.clear();
    }

    public int addBillboard(int depth, int screenX, int width) {
        DepthStagger.Billboard billboard = this.pool.alloc();
        billboard.set(screenX, width);
        DepthStagger.Billboard head = this.billboards.get(depth);
        billboard.offset = this.calculateOffset(billboard, head);
        billboard.next = head;
        this.billboards.put(depth, billboard);
        return billboard.offset;
    }

    private static final class Billboard {
        int screenX;
        int width;
        int offset;
        DepthStagger.Billboard next;

        DepthStagger.Billboard set(int screenX, int width) {
            this.screenX = screenX;
            this.width = width;
            this.offset = 0;
            this.next = null;
            return this;
        }

        void release(ObjectPool<DepthStagger.Billboard> pool) {
            if (this.next != null) {
                this.next.release(pool);
                this.next = null;
            }

            pool.release(this);
        }
    }
}
