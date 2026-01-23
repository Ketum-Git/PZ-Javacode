// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import zombie.core.skinnedmodel.Vector3;
import zombie.core.skinnedmodel.Vector4;
import zombie.iso.Vector2;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class VertexPositionNormalTangentTextureSkin {
    public Vector3 position;
    public Vector3 normal;
    public Vector3 tangent;
    public Vector2 textureCoordinates;
    public Vector4 blendWeights;
    public UInt4 blendIndices;

    public VertexPositionNormalTangentTextureSkin() {
    }

    public VertexPositionNormalTangentTextureSkin(Vector3 position, Vector3 normal, Vector3 tangent, Vector2 uv, Vector4 blendweights, UInt4 blendIndices) {
        this.position = position;
        this.normal = normal;
        this.tangent = tangent;
        this.textureCoordinates = uv;
        this.blendWeights = blendweights;
        this.blendIndices = blendIndices;
    }

    public void put(ByteBuffer buf) {
        buf.putFloat(this.position.x());
        buf.putFloat(this.position.y());
        buf.putFloat(this.position.z());
        buf.putFloat(this.normal.x());
        buf.putFloat(this.normal.y());
        buf.putFloat(this.normal.z());
        buf.putFloat(this.tangent.x());
        buf.putFloat(this.tangent.y());
        buf.putFloat(this.tangent.z());
        buf.putFloat(this.textureCoordinates.x);
        buf.putFloat(this.textureCoordinates.y);
        buf.putFloat(this.blendWeights.x);
        buf.putFloat(this.blendWeights.y);
        buf.putFloat(this.blendWeights.z);
        buf.putFloat(this.blendWeights.w);
        buf.putFloat(this.blendIndices.x);
        buf.putFloat(this.blendIndices.y);
        buf.putFloat(this.blendIndices.z);
        buf.putFloat(this.blendIndices.w);
    }
}
