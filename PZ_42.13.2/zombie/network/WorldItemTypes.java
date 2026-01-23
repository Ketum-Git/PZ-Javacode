// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import zombie.iso.IsoObject;

public class WorldItemTypes {
    public static IsoObject createFromBuffer(ByteBuffer bb) {
        IsoObject o = null;
        return IsoObject.factoryFromFileInput(null, bb);
    }
}
