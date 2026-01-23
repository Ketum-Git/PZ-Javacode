// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiMesh;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public final class ImportedStaticMesh {
    VertexBufferObject.VertexArray verticesUnskinned;
    int[] elements;
    final Vector3f minXyz = new Vector3f(Float.MAX_VALUE);
    final Vector3f maxXyz = new Vector3f(-Float.MAX_VALUE);
    Matrix4f transform;

    public ImportedStaticMesh(AiMesh mesh) {
        this.processAiScene(mesh);
    }

    private void processAiScene(AiMesh mesh) {
        int numVertices = mesh.getNumVertices();
        int numUVs = 0;

        for (int i = 0; i < 8; i++) {
            if (mesh.hasTexCoords(i)) {
                numUVs++;
            }
        }

        VertexBufferObject.VertexFormat format = new VertexBufferObject.VertexFormat(2 + numUVs);
        format.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        format.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);

        for (int ix = 0; ix < numUVs; ix++) {
            format.setElement(2 + ix, VertexBufferObject.VertexType.TextureCoordArray, 8);
        }

        format.calculate();
        this.verticesUnskinned = new VertexBufferObject.VertexArray(format, numVertices);
        Vector3f pos = new Vector3f();

        for (int n = 0; n < numVertices; n++) {
            float vx = mesh.getPositionX(n);
            float vy = mesh.getPositionY(n);
            float vz = mesh.getPositionZ(n);
            this.minXyz.min(pos.set(vx, vy, vz));
            this.maxXyz.max(pos.set(vx, vy, vz));
            this.verticesUnskinned.setElement(n, 0, mesh.getPositionX(n), mesh.getPositionY(n), mesh.getPositionZ(n));
            if (mesh.hasNormals()) {
                this.verticesUnskinned.setElement(n, 1, mesh.getNormalX(n), mesh.getNormalY(n), mesh.getNormalZ(n));
            } else {
                this.verticesUnskinned.setElement(n, 1, 0.0F, 1.0F, 0.0F);
            }

            if (numUVs > 0) {
                int nUV = 0;

                for (int ix = 0; ix < 8; ix++) {
                    if (mesh.hasTexCoords(ix)) {
                        this.verticesUnskinned.setElement(n, 2 + nUV, mesh.getTexCoordU(n, ix), 1.0F - mesh.getTexCoordV(n, ix));
                        nUV++;
                    }
                }
            }
        }

        int numElements = mesh.getNumFaces();
        this.elements = new int[numElements * 3];

        for (int f = 0; f < numElements; f++) {
            this.elements[f * 3 + 2] = mesh.getFaceVertex(f, 0);
            this.elements[f * 3 + 1] = mesh.getFaceVertex(f, 1);
            this.elements[f * 3 + 0] = mesh.getFaceVertex(f, 2);
        }
    }
}
