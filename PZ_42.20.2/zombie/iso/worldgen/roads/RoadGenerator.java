// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.roads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.utils.ChunkCoord;
import zombie.iso.worldgen.utils.triangulation.DelaunayTriangulator;
import zombie.iso.worldgen.utils.triangulation.Edge2D;
import zombie.iso.worldgen.utils.triangulation.NotEnoughPointsException;
import zombie.iso.worldgen.utils.triangulation.Vector2D;

public class RoadGenerator {
    private final long seed;
    private final long offset;
    private final RoadConfig config;
    private final List<Vector2D> roadDelaunayPoints;
    private final List<Edge2D> roadDelaunayEdges;
    private final List<Edge2D> roadDelaunayEdgesFiltered;
    private final List<RoadNexus> roadNexus;
    private final Map<ChunkCoord, Set<RoadEdge>> roadEdges;

    public RoadGenerator(long seed, RoadConfig config, long offset) {
        this.seed = seed;
        this.offset = offset;
        this.config = config;
        this.roadDelaunayPoints = this.genRoadDelaunayPoints();
        this.roadDelaunayEdges = this.genRoadDelaunayEdges(this.roadDelaunayPoints);
        this.roadDelaunayEdgesFiltered = this.filterEdges(this.roadDelaunayEdges);
        this.roadNexus = this.genRoadNexus(this.roadDelaunayPoints, this.roadDelaunayEdgesFiltered);
        this.roadEdges = this.getRoadEdgesMap(this.roadNexus);
    }

    private List<Vector2D> genRoadDelaunayPoints() {
        Set<Vector2D> nexus = new HashSet<>();

        for (int x = IsoWorld.instance.metaGrid.minX; x <= IsoWorld.instance.metaGrid.maxX; x++) {
            for (int y = IsoWorld.instance.metaGrid.minY; y <= IsoWorld.instance.metaGrid.maxY; y++) {
                Random rand = WorldGenParams.INSTANCE.getRandom(x, y, this.offset);
                if (rand.nextFloat() < this.config.probability()) {
                    nexus.add(new Vector2D(x * 256 + (int)(256.0F * rand.nextFloat()), y * 256 + (int)(256.0F * rand.nextFloat())));
                }
            }
        }

        nexus.add(new Vector2D(-1.0, 9895.0));
        nexus.add(new Vector2D(12598.0, 901.0));
        return nexus.stream().toList();
    }

    private List<Edge2D> genRoadDelaunayEdges(List<Vector2D> roadNexus) {
        DelaunayTriangulator triangulator = new DelaunayTriangulator(roadNexus);

        try {
            triangulator.triangulate();
        } catch (NotEnoughPointsException var4) {
            throw new RuntimeException("Not enough points in triangulation", var4);
        }

        return triangulator.getEdges();
    }

    private List<Edge2D> filterEdges(List<Edge2D> roadDelaunayEdges) {
        return roadDelaunayEdges.stream().filter(e -> e.magSqrt() < this.config.filter()).toList();
    }

    private List<RoadNexus> genRoadNexus(List<Vector2D> roadDelaunayPoints, List<Edge2D> roadDelaunayEdgesFiltered) {
        List<RoadNexus> nexus = new ArrayList<>();

        for (Vector2D p : roadDelaunayPoints) {
            Vector2i point = new Vector2i((int)p.x, (int)p.y);
            List<Vector2i> edges = new ArrayList<>();

            for (Edge2D e : roadDelaunayEdgesFiltered) {
                if ((int)e.a.x == point.x && (int)e.a.y == point.y) {
                    edges.add(new Vector2i((int)e.b.x, (int)e.b.y));
                } else if ((int)e.b.x == point.x && (int)e.b.y == point.y) {
                    edges.add(new Vector2i((int)e.a.x, (int)e.a.y));
                }
            }

            nexus.add(new RoadNexus(point, edges, this.config.tiles(), this.config.probaRoads()));
        }

        return nexus;
    }

    public List<RoadNexus> getRoadNexus() {
        return this.roadNexus;
    }

    private List<RoadEdge> getRoadEdges(List<RoadNexus> roadNexus) {
        List<RoadEdge> roadEdges = new ArrayList<>();

        for (RoadNexus nexus : roadNexus) {
            roadEdges.addAll(nexus.getRoadEdges());
        }

        return roadEdges;
    }

    private Map<ChunkCoord, Set<RoadEdge>> getRoadEdgesMap(List<RoadNexus> roadNexus) {
        Map<ChunkCoord, Set<RoadEdge>> roadEdgesMap = new HashMap<>();

        for (RoadEdge roadEdge : this.getRoadEdges(roadNexus)) {
            ChunkCoord coordA = new ChunkCoord(PZMath.fastfloor(roadEdge.a.x / 8.0F), PZMath.fastfloor(roadEdge.a.y / 8.0F));
            ChunkCoord coordB = new ChunkCoord(PZMath.fastfloor(roadEdge.b.x / 8.0F), PZMath.fastfloor(roadEdge.b.y / 8.0F));
            ChunkCoord coordSub = new ChunkCoord(PZMath.fastfloor(roadEdge.subnexus.x / 8.0F), PZMath.fastfloor(roadEdge.subnexus.y / 8.0F));
            Set<RoadEdge> roadEdgeA = roadEdgesMap.get(coordA);
            Set<RoadEdge> roadEdgeB = roadEdgesMap.get(coordB);
            Set<RoadEdge> roadEdgeSub = roadEdgesMap.get(coordSub);
            if (roadEdgeA == null) {
                roadEdgeA = new HashSet<>();
            }

            if (roadEdgeB == null) {
                roadEdgeB = new HashSet<>();
            }

            if (roadEdgeSub == null) {
                roadEdgeSub = new HashSet<>();
            }

            roadEdgeA.add(roadEdge);
            roadEdgeB.add(roadEdge);
            roadEdgeSub.add(roadEdge);
            roadEdgesMap.put(coordA, roadEdgeA);
            roadEdgesMap.put(coordB, roadEdgeB);
            roadEdgesMap.put(coordSub, roadEdgeSub);
        }

        return roadEdgesMap;
    }

    public Set<Road> getRoads(int cx, int cy) {
        Set<Road> roads = new HashSet<>();

        for (int x = IsoWorld.instance.metaGrid.minX * 32; x <= IsoWorld.instance.metaGrid.maxX * 32; x++) {
            Set<RoadEdge> edges = this.roadEdges.get(new ChunkCoord(x, cy));
            if (edges != null) {
                for (RoadEdge re : edges) {
                    for (Road road : re.roads) {
                        if (road.getDirection() == RoadDirection.WE
                            && Math.min(road.getCA().x(), road.getCB().x()) <= cx
                            && Math.max(road.getCA().x(), road.getCB().x()) >= cx) {
                            roads.add(road);
                        }
                    }
                }
            }
        }

        for (int y = IsoWorld.instance.metaGrid.minY * 32; y <= IsoWorld.instance.metaGrid.maxY * 32; y++) {
            Set<RoadEdge> edges = this.roadEdges.get(new ChunkCoord(cx, y));
            if (edges != null) {
                for (RoadEdge re : edges) {
                    for (Road roadx : re.roads) {
                        if (roadx.getDirection() == RoadDirection.NS
                            && Math.min(roadx.getCA().y(), roadx.getCB().y()) <= cy
                            && Math.max(roadx.getCA().y(), roadx.getCB().y()) >= cy) {
                            roads.add(roadx);
                        }
                    }
                }
            }
        }

        return roads;
    }
}
