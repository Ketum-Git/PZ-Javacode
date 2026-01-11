// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import zombie.iso.IsoObject;

public final class ObjectRenderInfo {
    public final IsoObject object;
    public ObjectRenderLayer layer = ObjectRenderLayer.None;
    public float targetAlpha = 1.0F;
    public boolean cutaway;
    public float renderX;
    public float renderY;
    public float renderWidth;
    public float renderHeight;
    public float renderScaleX;
    public float renderScaleY;
    public float renderAlpha;

    public ObjectRenderInfo(IsoObject object) {
        this.object = object;
    }
}
