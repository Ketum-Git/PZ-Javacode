// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.SpriteRenderer;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.WorldMapRenderer;

public class AnimalPathfind implements IPathRenderer {
    private static AnimalPathfind instance;
    final ObjectPool<Vector2f> vector2fObjectPool = new ObjectPool<>(Vector2f::new);
    final ObjectPool<Vector3f> vector3fObjectPool = new ObjectPool<>(Vector3f::new);
    final MeshList meshList = new MeshList();
    final LowLevelAStar cdAStar = new LowLevelAStar(this.meshList);
    UIWorldMap uiWorldMap;
    UIWorldMapV1 uiWorldMapV1;
    final HashMap<Mesh, Zone> meshZoneHashMap = new HashMap<>();
    final HashMap<Zone, Mesh> zoneMeshHashMap = new HashMap<>();
    MeshWanderer meshWanderer = new MeshWanderer();

    public static AnimalPathfind getInstance() {
        if (instance == null) {
            instance = new AnimalPathfind();
        }

        return instance;
    }

    public void renderPath(UIWorldMap ui, Zone zone, float x1, float y1, float x2, float y2) {
        this.uiWorldMap = ui;
        this.uiWorldMapV1 = ui.getAPIv1();
        this.cdAStar.renderer = this;
        this.meshList.meshes.clear();
        this.createMeshesFromZonesInArea((int)x1 - 300, (int)y1 - 300, 600, 600);

        for (Mesh mesh : this.meshList.meshes) {
            this.cdAStar.initOffMeshConnections(mesh);
        }

        int z1 = 0;
        Mesh mesh1 = this.meshList.getMeshAt(x1, y1, 0);
        if (mesh1 != null) {
            this.meshList.meshes.clear();
            mesh1.gatherConnectedMeshes(this.meshList.meshes);
        }
    }

    private void createMeshesFromZonesInArea(int x, int y, int w, int h) {
        int cellX = (x + 300) / 300;
        int cellY = (y + 300) / 300;
        IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
        if (metaCell != null) {
            ArrayList<Zone> zones = new ArrayList<>();

            for (int i = 0; i < metaCell.getAnimalZonesSize(); i++) {
                zones.add(metaCell.getAnimalZone(i));
            }

            for (int i = 0; i < zones.size(); i++) {
                Zone zone1 = zones.get(i);
                Mesh mesh = this.zoneMeshHashMap.get(zone1);
                if (mesh != null) {
                    this.meshList.meshes.add(mesh);
                } else {
                    if (zone1.isRectangle()) {
                    }

                    if (zone1.getPolygonTriangles() != null) {
                        mesh = new Mesh();
                        mesh.meshList = this.meshList;
                        mesh.initFromZone(zone1);
                        this.meshList.meshes.add(mesh);
                        this.meshZoneHashMap.put(mesh, zone1);
                        this.zoneMeshHashMap.put(zone1, mesh);
                    }
                }
            }
        }
    }

    @Override
    public void drawTriangleCentroid(Mesh mesh, int tri, float r, float g, float b, float a) {
        Vector2f t1 = this.meshWanderer.mesh.triangles.get(tri);
        Vector2f t2 = this.meshWanderer.mesh.triangles.get(tri + 1);
        Vector2f t3 = this.meshWanderer.mesh.triangles.get(tri + 2);
        float cx = (t1.x + t2.x + t3.x) / 3.0F;
        float cy = (t1.y + t2.y + t3.y) / 3.0F;
        this.drawRect(cx - 1.0F, cy - 1.0F, 2.0F, 2.0F, r, g, b, a);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        WorldMapRenderer rr = this.uiWorldMapV1.getRenderer();
        float _x1 = rr.worldToUIX(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _y1 = rr.worldToUIY(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _x2 = rr.worldToUIX(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _y2 = rr.worldToUIY(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        SpriteRenderer.instance.renderline(null, (int)_x1, (int)_y1, (int)_x2, (int)_y2, r, g, b, a, 1.0F);
    }

    @Override
    public void drawRect(float x1, float y1, float w, float h, float r, float g, float b, float a) {
        this.drawLine(x1, y1, x1 + w, y1, r, g, b, a);
        this.drawLine(x1 + w, y1, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(x1, y1 + h, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(x1, y1, x1, y1 + h, r, g, b, a);
    }
}
