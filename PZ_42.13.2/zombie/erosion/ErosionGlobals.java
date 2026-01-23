// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion;

import zombie.erosion.season.ErosionIceQueen;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.sprite.IsoSpriteManager;

public final class ErosionGlobals {
    public static final boolean EROSION_DEBUG = true;

    public static void Boot(IsoSpriteManager _sprMngr) {
        new ErosionMain(_sprMngr, true);
    }

    public static void Reset() {
        ErosionMain.Reset();
        ErosionClient.Reset();
        ErosionIceQueen.Reset();
        ErosionSeason.Reset();
        ErosionRegions.Reset();
    }
}
