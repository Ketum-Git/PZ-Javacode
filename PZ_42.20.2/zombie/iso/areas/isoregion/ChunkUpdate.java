// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion;

import java.nio.ByteBuffer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;

public class ChunkUpdate {
    public static void writeIsoChunkIntoBuffer(IsoChunk c, ByteBuffer bb) {
        if (c != null) {
            int oldPos = bb.position();
            bb.putInt(0);
            bb.putInt(c.maxLevel);
            int maxBytes = (c.maxLevel + 1) * 8 * 8;
            bb.putInt(maxBytes);

            for (int z = 0; z <= c.maxLevel; z++) {
                for (int p = 0; p < c.squares[0].length; p++) {
                    int squaresIndexOfLevel = c.squaresIndexOfLevel(z);
                    IsoGridSquare sq = c.squares[squaresIndexOfLevel][p];
                    byte flags = IsoRegions.calculateSquareFlags(sq);
                    bb.put(flags);
                }
            }

            int newPos = bb.position();
            bb.position(oldPos);
            bb.putInt(newPos - oldPos);
            bb.position(newPos);
        } else {
            bb.putInt(-1);
        }
    }
}
