// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import zombie.core.skinnedmodel.Vector3;
import zombie.iso.Vector2;

public final class SoftwareModelMesh {
    public int[] indicesUnskinned;
    public VertexPositionNormalTangentTextureSkin[] verticesUnskinned;
    public String texture;
    public VertexBufferObject vb;

    public SoftwareModelMesh(VertexPositionNormalTangentTextureSkin[] verticesUnskinned, int[] elements) {
        this.indicesUnskinned = elements;
        this.verticesUnskinned = verticesUnskinned;
    }

    public SoftwareModelMesh(VertexPositionNormalTangentTexture[] verticesUnskinned, int[] elements) {
        this.indicesUnskinned = elements;
        this.verticesUnskinned = new VertexPositionNormalTangentTextureSkin[verticesUnskinned.length];

        for (int i = 0; i < verticesUnskinned.length; i++) {
            VertexPositionNormalTangentTexture vertexPositionNormalTangentTexture = verticesUnskinned[i];
            this.verticesUnskinned[i] = new VertexPositionNormalTangentTextureSkin();
            this.verticesUnskinned[i].position = new Vector3(
                vertexPositionNormalTangentTexture.position.x(),
                vertexPositionNormalTangentTexture.position.y(),
                vertexPositionNormalTangentTexture.position.z()
            );
            this.verticesUnskinned[i].normal = new Vector3(
                vertexPositionNormalTangentTexture.normal.x(), vertexPositionNormalTangentTexture.normal.y(), vertexPositionNormalTangentTexture.normal.z()
            );
            this.verticesUnskinned[i].textureCoordinates = new Vector2(
                vertexPositionNormalTangentTexture.textureCoordinates.x, vertexPositionNormalTangentTexture.textureCoordinates.y
            );
        }
    }
}
