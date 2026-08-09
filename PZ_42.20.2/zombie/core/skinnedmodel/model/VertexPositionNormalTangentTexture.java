// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import zombie.core.skinnedmodel.Vector3;
import zombie.iso.Vector2;

/**
 * Created by LEMMYATI on 09/03/14.
 */
public final class VertexPositionNormalTangentTexture {
    public Vector3 position;
    public Vector3 normal;
    public Vector3 tangent;
    public Vector2 textureCoordinates;

    public VertexPositionNormalTangentTexture(Vector3 position, Vector3 normal, Vector3 tangent, Vector2 uv) {
        this.position = position;
        this.normal = normal;
        this.tangent = tangent;
        this.textureCoordinates = uv;
    }

    public VertexPositionNormalTangentTexture() {
        this.position = new Vector3(0.0F, 0.0F, 0.0F);
        this.normal = new Vector3(0.0F, 0.0F, 1.0F);
        this.tangent = new Vector3(0.0F, 1.0F, 0.0F);
        this.textureCoordinates = new Vector2(0.0F, 0.0F);
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
    }
}
