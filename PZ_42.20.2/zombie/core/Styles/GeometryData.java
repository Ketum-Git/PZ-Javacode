// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

/**
 * Really basic geometry data which is used by Style
 */
public class GeometryData {
    private final FloatList vertexData;
    private final ShortList indexData;

    /**
     * C'tor
     */
    public GeometryData(FloatList vertexData, ShortList indexData) {
        this.vertexData = vertexData;
        this.indexData = indexData;
    }

    public void clear() {
        this.vertexData.clear();
        this.indexData.clear();
    }

    public FloatList getVertexData() {
        return this.vertexData;
    }

    public ShortList getIndexData() {
        return this.indexData;
    }
}
