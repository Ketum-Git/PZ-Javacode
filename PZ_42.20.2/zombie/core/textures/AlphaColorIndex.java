// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.io.Serializable;

class AlphaColorIndex implements Serializable {
    byte alpha;
    byte blue;
    byte green;
    byte red;

    AlphaColorIndex(int red, int green, int blue, int alpha) {
        this.red = (byte)red;
        this.green = (byte)green;
        this.blue = (byte)blue;
        this.alpha = (byte)alpha;
    }
}
