// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ImportantArea {
    public int sx;
    public int sy;
    public long lastUpdate = System.currentTimeMillis();

    public ImportantArea(int sx, int sy) {
        this.sx = sx;
        this.sy = sy;
    }

    public final void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.sx = input.getInt();
        this.sy = input.getInt();
        this.lastUpdate = System.currentTimeMillis();
    }

    public final void save(ByteBuffer output) throws IOException {
        output.putInt(this.sx);
        output.putInt(this.sy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ImportantArea that = (ImportantArea)o;
            return this.sx == that.sx && this.sy == that.sy;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sx, this.sy);
    }
}
