// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector2f;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.UIWorldMap;

public final class MeshWanderer {
    Zone zone;
    Mesh mesh;
    int triangleIndex;
    float x;
    float y;
    int targetTriangleIndex;
    int targetTriangleEdge;
    float targetX;
    float targetY;
    boolean movingToOtherTriangle;

    void update() {
        float len = Vector2f.length(this.targetX - this.x, this.targetY - this.y);
        if (len <= 1.0F) {
            if (!this.movingToOtherTriangle && !Rand.NextBool(2)) {
                Vector2f pt = this.mesh.pickRandomPointInTriangle(this.triangleIndex, new Vector2f());
                this.targetX = pt.x;
                this.targetY = pt.y;
            } else if (!this.movingToOtherTriangle) {
                this.targetX = this.mesh.getEdgeMidPointX(this.targetTriangleIndex, this.targetTriangleEdge);
                this.targetY = this.mesh.getEdgeMidPointY(this.targetTriangleIndex, this.targetTriangleEdge);
                this.movingToOtherTriangle = true;
            } else {
                int previousTriangleIndex = this.triangleIndex;
                this.triangleIndex = this.targetTriangleIndex;
                this.targetTriangleIndex = this.chooseNextTriangle(this.triangleIndex);
                Vector2f pt = this.mesh.pickRandomPointInTriangle(this.triangleIndex, new Vector2f());
                this.targetX = pt.x;
                this.targetY = pt.y;
                this.movingToOtherTriangle = false;
            }
        } else {
            float dist = 1.0F;
            float dx = (this.targetX - this.x) / len * 1.0F;
            float dy = (this.targetY - this.y) / len * 1.0F;
            this.x += dx;
            this.y += dy;
        }
    }

    int chooseNextTriangle(int ignoreTriangleIndex) {
        ArrayList<Integer> choices = new ArrayList<>();

        for (int edge = 0; edge < 3; edge++) {
            int adjacent = this.mesh.adjacentTriangles.get(ignoreTriangleIndex + edge);
            if (adjacent != -1) {
                choices.add(adjacent);
            }
        }

        if (choices.isEmpty()) {
            int edgex = Rand.Next(3);
            this.targetX = this.mesh.getEdgeMidPointX(ignoreTriangleIndex, edgex);
            this.targetY = this.mesh.getEdgeMidPointY(ignoreTriangleIndex, edgex);
            this.targetTriangleEdge = edgex;
            return ignoreTriangleIndex;
        } else {
            int adjacent = PZArrayUtil.pickRandom(choices);
            int tri2 = adjacent >> 16 & 65535;
            int edge2 = adjacent & 65535;
            this.targetX = this.mesh.getEdgeMidPointX(tri2, edge2);
            this.targetY = this.mesh.getEdgeMidPointY(tri2, edge2);
            this.targetTriangleEdge = edge2;
            return tri2;
        }
    }

    public void renderPath(
        UIWorldMap ui, Zone zone, float x1, float y1, float x2, float y2, IPathRenderer renderer, Mesh mesh1, HashMap<Mesh, Zone> meshZoneHashMap
    ) {
        if (this.zone != meshZoneHashMap.get(mesh1)) {
            this.zone = meshZoneHashMap.get(mesh1);
            this.mesh = mesh1;
            this.triangleIndex = 0;
            int adjacent = mesh1.adjacentTriangles.get(this.triangleIndex);
            if (adjacent == -1) {
                adjacent = mesh1.adjacentTriangles.get(this.triangleIndex + 1);
            }

            if (adjacent == -1) {
                adjacent = mesh1.adjacentTriangles.get(this.triangleIndex + 2);
            }

            this.targetTriangleIndex = adjacent >> 16 & 65535;
            this.targetTriangleEdge = adjacent & 65535;
            this.x = mesh1.triangles.get(this.triangleIndex).x();
            this.y = mesh1.triangles.get(this.triangleIndex).y();
            this.targetX = mesh1.getEdgeMidPointX(this.targetTriangleIndex, this.targetTriangleEdge);
            this.targetY = mesh1.getEdgeMidPointY(this.targetTriangleIndex, this.targetTriangleEdge);
        }

        this.update();
        renderer.drawLine(this.x, this.y, this.targetX, this.targetY, 1.0F, 1.0F, 0.0F, 1.0F);
        renderer.drawRect(this.x - 1.0F, this.y - 1.0F, 2.0F, 2.0F, 0.0F, 1.0F, 0.0F, 1.0F);
        renderer.drawTriangleCentroid(mesh1, this.targetTriangleIndex, 1.0F, 0.0F, 0.0F, 1.0F);

        for (int edge = 0; edge < 3; edge++) {
            int adjacentx = mesh1.adjacentTriangles.get(this.triangleIndex + edge);
            if (adjacentx != -1) {
                int tri1 = adjacentx >> 16 & 65535;
                renderer.drawTriangleCentroid(mesh1, tri1, 1.0F, 0.0F, 1.0F, 1.0F);
            }
        }
    }
}
