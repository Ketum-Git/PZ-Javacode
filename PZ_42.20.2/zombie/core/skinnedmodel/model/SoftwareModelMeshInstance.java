// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

public final class SoftwareModelMeshInstance {
    public SoftwareModelMesh softwareMesh;
    public VertexBufferObject vb;
    public String name;

    public SoftwareModelMeshInstance(String name, SoftwareModelMesh softwareMesh) {
        this.name = name;
        this.softwareMesh = softwareMesh;
        this.vb = new VertexBufferObject();
        this.vb.elements = softwareMesh.indicesUnskinned;
    }
}
