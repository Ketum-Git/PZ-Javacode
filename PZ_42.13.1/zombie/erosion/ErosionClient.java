// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion;

import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.sprite.IsoSpriteManager;

public class ErosionClient {
    public static ErosionClient instance;

    public ErosionClient(IsoSpriteManager _sprMngr, boolean _debug) {
        instance = this;
        new ErosionIceQueen(_sprMngr);
        ErosionRegions.init();
    }

    public static void Reset() {
        instance = null;
    }
}
