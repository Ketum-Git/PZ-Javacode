// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.List;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.roads.RoadEdge;
import zombie.iso.worldgen.roads.RoadGenerator;
import zombie.iso.worldgen.roads.RoadNexus;
import zombie.worldMap.UIWorldMap;

public class WorldGenDebug {
    private static final WorldGenDebug INSTANCE = new WorldGenDebug();

    private WorldGenDebug() {
    }

    public static WorldGenDebug getInstance() {
        return INSTANCE;
    }

    public void renderRoads(UIWorldMap ui) {
        for (int nGen = 0; nGen < IsoWorld.instance.getWgChunk().getRoadGenerators().size(); nGen++) {
            RoadGenerator generator = IsoWorld.instance.getWgChunk().getRoadGenerators().get(nGen);

            for (RoadNexus nexus : generator.getRoadNexus()) {
                Vector2i origin = nexus.getDelaunayPoint();
                List<Vector2i> remotes = nexus.getDelaunayRemotes();
                List<RoadEdge> edges = nexus.getRoadEdges();
                double r = 0.1 * nGen;
                double g = 1.0;
                double b = 0.0;
                double a = 1.0;
                float uiX = PZMath.floor(ui.getAPI().worldToUIX(origin.x, origin.y));
                float uiY = PZMath.floor(ui.getAPI().worldToUIY(origin.x, origin.y));
                ui.DrawTextureScaledColor(null, uiX - 3.0, uiY - 3.0, 6.0, 6.0, r, 1.0, 0.0, 1.0);

                for (Vector2i remote : remotes) {
                    float uiX_A = PZMath.floor(ui.getAPI().worldToUIX(remote.x, remote.y));
                    float uiY_A = PZMath.floor(ui.getAPI().worldToUIY(remote.x, remote.y));
                    float uiX_B = PZMath.floor(ui.getAPI().worldToUIX(origin.x, origin.y));
                    float uiY_B = PZMath.floor(ui.getAPI().worldToUIY(origin.x, origin.y));
                    ui.DrawLine(null, uiX_A, uiY_A, uiX_B, uiY_B, 0.5F, r, 1.0, 0.0, 1.0);
                }

                for (RoadEdge edge : edges) {
                    float uiXx = PZMath.floor(ui.getAPI().worldToUIX(edge.subnexus.x, edge.subnexus.y));
                    float uiYx = PZMath.floor(ui.getAPI().worldToUIY(edge.subnexus.x, edge.subnexus.y));
                    b = 0.1 * nGen;
                    a = 0.0;
                    double bx = 1.0;
                    double ax = 1.0;
                    ui.DrawTextureScaledColor(null, uiXx - 3.0, uiYx - 3.0, 6.0, 6.0, b, 0.0, 1.0, 1.0);
                    float uiX_A = PZMath.floor(ui.getAPI().worldToUIX(edge.a.x, edge.a.y));
                    float uiY_A = PZMath.floor(ui.getAPI().worldToUIY(edge.a.x, edge.a.y));
                    float uiX_B = PZMath.floor(ui.getAPI().worldToUIX(edge.subnexus.x, edge.subnexus.y));
                    float uiY_B = PZMath.floor(ui.getAPI().worldToUIY(edge.subnexus.x, edge.subnexus.y));
                    float uiX_C = PZMath.floor(ui.getAPI().worldToUIX(edge.b.x, edge.b.y));
                    float uiY_C = PZMath.floor(ui.getAPI().worldToUIY(edge.b.x, edge.b.y));
                    ui.DrawLine(null, uiX_A, uiY_A, uiX_B, uiY_B, 0.5F, b, 0.0, 1.0, 1.0);
                    ui.DrawLine(null, uiX_B, uiY_B, uiX_C, uiY_C, 0.5F, b, 0.0, 1.0, 1.0);
                }
            }
        }
    }
}
