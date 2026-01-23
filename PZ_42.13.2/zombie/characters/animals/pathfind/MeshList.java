// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import java.util.ArrayList;

public final class MeshList {
    ArrayList<Mesh> meshes = new ArrayList<>();
    int z;

    int size() {
        return this.meshes.size();
    }

    Mesh get(int index) {
        return this.meshes.get(index);
    }

    int indexOf(Mesh mesh) {
        return this.meshes.indexOf(mesh);
    }

    int getTriangleAt(float x, float y) {
        for (int meshIdx = 0; meshIdx < this.size(); meshIdx++) {
            int triIdx = this.get(meshIdx).getTriangleAt(x, y);
            if (triIdx != -1) {
                return meshIdx << 16 | triIdx;
            }
        }

        return -1;
    }

    Mesh getMeshAt(float x, float y, int z) {
        for (int meshIdx = 0; meshIdx < this.size(); meshIdx++) {
            int triIdx = this.get(meshIdx).getTriangleAt(x, y);
            if (triIdx != -1) {
                return this.get(meshIdx);
            }
        }

        return null;
    }
}
