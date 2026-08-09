// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.core.network.ByteBufferReader;
import zombie.iso.IsoObject;

public class WorldItemTypes {
    public static IsoObject createFromBuffer(ByteBufferReader bb) {
        return IsoObject.factoryFromFileInput(null, bb.bb);
    }
}
