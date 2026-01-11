// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TFloatArrayList;

public final class WorldMapSymbolCollisions {
    final TFloatArrayList boxes = new TFloatArrayList();
    final TByteArrayList collide = new TByteArrayList();

    boolean addBox(float x, float y, float w, float h, boolean collide) {
        int b1 = this.boxes.size() / 4 - 1;
        int b2 = b1 + 1;
        this.boxes.add(x);
        this.boxes.add(y);
        this.boxes.add(x + w);
        this.boxes.add(y + h);
        this.collide.add((byte)(collide ? 1 : 0));
        if (!collide) {
            return false;
        } else {
            for (int b = 0; b <= b1; b++) {
                if (this.isCollision(b, b2)) {
                    x += w / 2.0F;
                    y += h / 2.0F;
                    this.boxes.set(b2 * 4, x - 3.0F - 1.0F);
                    this.boxes.set(b2 * 4 + 1, y - 3.0F - 1.0F);
                    this.boxes.set(b2 * 4 + 2, x + 3.0F + 1.0F);
                    this.boxes.set(b2 * 4 + 3, y - 3.0F + 1.0F);
                    return true;
                }
            }

            return false;
        }
    }

    boolean isCollision(int b1, int b2) {
        if (this.collide.getQuick(b1) != 0 && this.collide.getQuick(b2) != 0) {
            b1 *= 4;
            b2 *= 4;
            float x1 = this.boxes.get(b1);
            float y1 = this.boxes.get(b1 + 1);
            float x2 = this.boxes.get(b1 + 2);
            float y2 = this.boxes.get(b1 + 3);
            float x3 = this.boxes.get(b2);
            float y3 = this.boxes.get(b2 + 1);
            float x4 = this.boxes.get(b2 + 2);
            float y4 = this.boxes.get(b2 + 3);
            return x1 < x4 && x2 > x3 && y1 < y4 && y2 > y3;
        } else {
            return false;
        }
    }

    boolean isCollision(int b1) {
        for (int i = 0; i < this.boxes.size() / 4; i++) {
            if (i != b1 && this.isCollision(b1, i)) {
                return true;
            }
        }

        return false;
    }
}
