// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.regions;

import java.util.ArrayList;

/**
 * TurboTuTone.
 */
public interface IWorldRegion {
    ArrayList<IsoWorldRegion> getDebugConnectedNeighborCopy();

    ArrayList<IsoWorldRegion> getNeighbors();

    boolean isFogMask();

    boolean isPlayerRoom();

    boolean isFullyRoofed();

    int getRoofCnt();

    int getSquareSize();

    ArrayList<IsoChunkRegion> getDebugIsoChunkRegionCopy();
}
