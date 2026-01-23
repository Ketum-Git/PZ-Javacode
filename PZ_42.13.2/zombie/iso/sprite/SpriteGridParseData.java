// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.popman.ObjectPool;

public final class SpriteGridParseData {
    final ObjectPool<SpriteGridParseData.Level> levelPool = new ObjectPool<>(SpriteGridParseData.Level::new);
    public final ArrayList<SpriteGridParseData.Level> levels = new ArrayList<>();
    public int width;
    public int height;

    public SpriteGridParseData.Level getOrCreateLevel(int z) {
        for (int z1 = this.levels.size(); z1 <= z; z1++) {
            SpriteGridParseData.Level level = this.levelPool.alloc();
            level.width = 0;
            level.height = 0;
            level.z = z1;
            level.xyToSprite.clear();
            this.levels.add(level);
        }

        return this.levels.get(z);
    }

    public boolean isValid() {
        if (this.width > 0 && this.height > 0) {
            if (this.levels.isEmpty()) {
                return false;
            } else {
                for (SpriteGridParseData.Level level : this.levels) {
                    if (!level.isValid()) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public void clear() {
        this.levelPool.releaseAll(this.levels);
        this.levels.clear();
        this.width = 0;
        this.height = 0;
    }

    public static final class Level {
        public int width;
        public int height;
        public int z;
        public HashMap<String, IsoSprite> xyToSprite = new HashMap<>();

        boolean isValid() {
            return this.width <= 0 || this.height <= 0 ? false : !this.xyToSprite.isEmpty();
        }
    }
}
