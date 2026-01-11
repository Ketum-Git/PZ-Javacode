// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import astar.AStar;
import astar.ISearchNode;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.popman.ObjectPool;

public final class LowLevelAStar extends AStar {
    private MeshList meshList;
    LowLevelSearchNode initialNode;
    LowLevelGoalNode goalNode;
    TLongObjectHashMap<LowLevelSearchNode> nodeMap = new TLongObjectHashMap<>();
    IPathRenderer renderer;

    public LowLevelAStar(MeshList meshList) {
        this.meshList = meshList;
    }

    public void setMeshList(MeshList meshList) {
        this.meshList = meshList;
    }

    void findPath(float x1, float y1, int z1, float x2, float y2, int z2, ArrayList<Vector2f> pathOut) {
        Mesh mesh1 = this.meshList.getMeshAt(x1, y1, z1);
        Mesh mesh2 = this.meshList.getMeshAt(x2, y2, z2);
        if (mesh1 != null && mesh2 != null) {
            int startTri = mesh1.getTriangleAt(x1, y1);
            if (startTri != -1) {
                startTri |= mesh1.indexOf() << 16;
                int endTri = mesh2.getTriangleAt(x2, y2);
                if (endTri != -1) {
                    endTri |= mesh2.indexOf() << 16;
                    if (mesh1 == mesh2 && startTri == endTri) {
                        if (pathOut != null) {
                            pathOut.add(new Vector2f(x1, y1));
                            pathOut.add(new Vector2f(x2, y2));
                        }

                        this.renderer.drawLine(x1, y1, x2, y2, 0.0F, 1.0F, 0.0F, 1.0F);
                    } else {
                        this.initOffMeshConnections(mesh2);
                        LowLevelSearchNode initialNode = LowLevelSearchNode.pool.alloc();
                        initialNode.parent = null;
                        initialNode.astar = this;
                        initialNode.meshList = this.meshList;
                        initialNode.meshIdx = startTri >> 16 & 65535;
                        initialNode.triangleIdx = startTri & 65535;
                        initialNode.edgeIdx = -1;
                        initialNode.x = x1;
                        initialNode.y = y1;
                        LowLevelSearchNode goalNode1 = LowLevelSearchNode.pool.alloc();
                        goalNode1.parent = null;
                        goalNode1.astar = this;
                        goalNode1.meshList = this.meshList;
                        goalNode1.meshIdx = endTri >> 16 & 65535;
                        goalNode1.triangleIdx = endTri & 65535;
                        goalNode1.edgeIdx = -1;
                        goalNode1.x = x2;
                        goalNode1.y = y2;
                        LowLevelGoalNode goalNode = new LowLevelGoalNode();
                        goalNode.searchNode = goalNode1;
                        this.initialNode = initialNode;
                        this.goalNode = goalNode;

                        for (LowLevelSearchNode node : this.nodeMap.valueCollection()) {
                            LowLevelSearchNode.pool.release(node);
                        }

                        this.nodeMap.clear();
                        ArrayList<ISearchNode> path = this.shortestPath(initialNode, goalNode);
                        if (path != null) {
                            for (int i = 0; i < path.size() - 1; i++) {
                                LowLevelSearchNode node1 = (LowLevelSearchNode)path.get(i);
                                LowLevelSearchNode node2 = (LowLevelSearchNode)path.get(i + 1);
                                if (this.renderer != null) {
                                    this.renderer.drawLine(node1.getX(), node1.getY(), node2.getX(), node2.getY(), 1.0F, 1.0F, 0.0F, 1.0F);
                                    this.renderer.drawRect(node1.getX() - 0.5F, node1.getY() - 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
                                    Mesh mesh = this.meshList.get(node1.meshIdx);
                                    Vector2f t1 = mesh.triangles.get(node1.triangleIdx);
                                    Vector2f t2 = mesh.triangles.get(node1.triangleIdx + 1);
                                    Vector2f t3 = mesh.triangles.get(node1.triangleIdx + 2);
                                    float xt = (t1.x + t2.x + t3.x) / 3.0F;
                                    float yt = (t1.y + t2.y + t3.y) / 3.0F;
                                    this.renderer.drawRect(xt - 0.5F, yt - 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
                                }

                                if (i > 0 && i < path.size() - 2) {
                                }
                            }

                            ArrayList<Vector3f> betterPath = this.stringPull(path);
                            if (betterPath != null) {
                                if (pathOut != null) {
                                    for (int i = 0; i < betterPath.size(); i++) {
                                        Vector3f p = betterPath.get(i);
                                        pathOut.add(new Vector2f(p.x, p.y));
                                    }
                                }

                                if (this.renderer != null) {
                                    for (int i = 0; i < betterPath.size() - 1; i++) {
                                        Vector3f p1 = betterPath.get(i);
                                        Vector3f p2 = betterPath.get(i + 1);
                                        this.renderer.drawLine(p1.x, p1.y, p2.x, p2.y, 0.0F, 1.0F, 0.0F, 1.0F);
                                        this.renderer.drawRect(p1.x - 0.5F, p1.y - 0.5F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F);
                                    }
                                }

                                AnimalPathfind.getInstance().vector3fObjectPool.releaseAll(betterPath);
                            }
                        }

                        LowLevelSearchNode.pool.release(initialNode);
                        LowLevelSearchNode.pool.release(goalNode1);
                        LowLevelSearchNode.pool.releaseAll(new ArrayList<>(this.nodeMap.valueCollection()));
                        this.nodeMap.clear();
                    }
                }
            }
        }
    }

    public long makeKey(int meshIdx, int tri) {
        return meshIdx << 16 & -65536 | tri & 65535;
    }

    public int triFromKey(int key) {
        return key >> 8 & 65535;
    }

    public int edgeFromKey(int key) {
        return key & 0xFF;
    }

    public void getSuccessors(LowLevelSearchNode searchNode, ArrayList<ISearchNode> successors) {
        successors.clear();
        int tri1 = searchNode.triangleIdx;
        Mesh mesh1 = searchNode.meshList.get(searchNode.meshIdx);

        for (int edge1 = 0; edge1 < 3; edge1++) {
            int adjacent = mesh1.adjacentTriangles.get(tri1 + edge1);
            if (adjacent != -1) {
                int tri2 = adjacent >> 16 & 65535;
                int edge2 = adjacent & 65535;
                this.addSuccessor(searchNode.meshList, searchNode.meshIdx, tri2, edge2, Float.NaN, Float.NaN, successors);
            }
        }

        if (searchNode.meshList == this.goalNode.searchNode.meshList
            && searchNode.meshIdx == this.goalNode.searchNode.meshIdx
            && searchNode.triangleIdx == this.goalNode.searchNode.triangleIdx
            && searchNode != this.goalNode.searchNode) {
            successors.add(this.goalNode.searchNode);
        }

        this.initOffMeshConnections(searchNode.meshList.get(searchNode.meshIdx));

        for (OffMeshConnection omc : mesh1.offMeshConnections) {
            if (omc.triFrom == searchNode.triangleIdx) {
                this.addSuccessor(
                    omc.meshTo.meshList,
                    omc.meshTo.indexOf(),
                    omc.triTo,
                    omc.edgeTo,
                    (omc.edge1.x + omc.edge2.x) / 2.0F,
                    (omc.edge1.y + omc.edge2.y) / 2.0F,
                    successors
                );
            }
        }
    }

    void initOffMeshConnections(Mesh mesh) {
        if (!mesh.offMeshDone) {
            mesh.offMeshDone = true;

            assert mesh.offMeshConnections.isEmpty();

            if (!mesh.trianglesOnBoundaries.isEmpty()) {
                this.findOverlappingEdges(mesh, mesh.meshList);
            }
        }
    }

    LowLevelSearchNode getSearchNode(MeshList meshList, int meshIdx, int tri, int edge) {
        long key = this.makeKey(meshIdx, tri);
        LowLevelSearchNode searchNode1 = this.nodeMap.get(key);
        if (searchNode1 == null) {
            searchNode1 = LowLevelSearchNode.pool.alloc();
            searchNode1.parent = null;
            searchNode1.astar = this;
            searchNode1.meshList = meshList;
            searchNode1.meshIdx = meshIdx;
            searchNode1.triangleIdx = tri;
            searchNode1.edgeIdx = edge;
            searchNode1.x = Float.NaN;
            searchNode1.y = Float.NaN;
            if (edge == -1) {
                searchNode1.x = searchNode1.getCentroidX();
                searchNode1.y = searchNode1.getCentroidY();
            }

            this.nodeMap.put(key, searchNode1);
        }

        return searchNode1;
    }

    void addSuccessor(MeshList meshList, int meshIdx, int tri, int edge, float x, float y, ArrayList<ISearchNode> successors) {
        boolean exists = this.nodeMap.containsKey(this.makeKey(meshIdx, tri));
        LowLevelSearchNode searchNode1 = this.getSearchNode(meshList, meshIdx, tri, edge);
        if (!successors.contains(searchNode1)) {
            if (!exists) {
                searchNode1.x = x;
                searchNode1.y = y;
            }

            successors.add(searchNode1);
        }
    }

    void findOverlappingEdges(Mesh mesh1, MeshList meshList) {
        for (int j = 0; j < mesh1.trianglesOnBoundaries.size(); j++) {
            short tri1 = mesh1.trianglesOnBoundaries.get(j);
            short edges1 = mesh1.edgesOnBoundaries.get(j);

            for (int edge1 = 0; edge1 < 3; edge1++) {
                if ((edges1 & 1 << edge1) != 0) {
                    for (int meshIdx2 = 0; meshIdx2 < meshList.size(); meshIdx2++) {
                        Mesh mesh2 = meshList.get(meshIdx2);
                        if (mesh1 != mesh2) {
                            this.findOverlappingEdges(mesh1, tri1, edge1, mesh2);
                        }
                    }
                }
            }
        }
    }

    void findOverlappingEdges(Mesh mesh1, int tri1, int edge1, Mesh mesh2) {
        Vector2f p1 = mesh1.triangles.get(tri1 + edge1);
        Vector2f p2 = mesh1.triangles.get(tri1 + (edge1 + 1) % 3);
        Vector2f overlap1 = AnimalPathfind.getInstance().vector2fObjectPool.alloc();
        Vector2f overlap2 = AnimalPathfind.getInstance().vector2fObjectPool.alloc();

        for (int j2 = 0; j2 < mesh2.trianglesOnBoundaries.size(); j2++) {
            short tri2 = mesh2.trianglesOnBoundaries.get(j2);
            short edges2 = mesh2.edgesOnBoundaries.get(j2);

            for (int edge2 = 0; edge2 < 3; edge2++) {
                if ((edges2 & 1 << edge2) != 0) {
                    Vector2f p3 = mesh2.triangles.get(tri2 + edge2);
                    Vector2f p4 = mesh2.triangles.get(tri2 + (edge2 + 1) % 3);
                    if (RobustLineIntersector.computeIntersection(p1, p2, p3, p4, overlap1, overlap2) == 2) {
                        mesh1.addConnection(tri1, edge1, mesh2, tri2, edge2, overlap1, overlap2);
                        break;
                    }
                }
            }
        }

        AnimalPathfind.getInstance().vector2fObjectPool.release(overlap1);
        AnimalPathfind.getInstance().vector2fObjectPool.release(overlap2);
    }

    private boolean anyTwoPointsEqual(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4, Vector2f p5, Vector2f p6) {
        int count = 0;
        if (this.isPointEqualToAnyOf3(p1, p4, p5, p6)) {
            count++;
        }

        if (this.isPointEqualToAnyOf3(p2, p4, p5, p6)) {
            count++;
        }

        if (this.isPointEqualToAnyOf3(p3, p4, p5, p6)) {
            count++;
        }

        return count > 1;
    }

    private boolean isPointEqualToAnyOf2(Vector2f p1, Vector2f p4, Vector2f p5) {
        return this.isPointEqual(p1, p4) || this.isPointEqual(p1, p5);
    }

    private boolean isPointEqualToAnyOf3(Vector2f p1, Vector2f p4, Vector2f p5, Vector2f p6) {
        return this.isPointEqual(p1, p4) || this.isPointEqual(p1, p5) || this.isPointEqual(p1, p6);
    }

    private boolean isPointEqual(Vector2f p1, Vector2f p2) {
        return PZMath.equal(p1.x, p2.x, 0.001F) && PZMath.equal(p1.y, p2.y, 0.001F);
    }

    private boolean isPointEqual(Vector3f p1, Vector3f p2) {
        return PZMath.equal(p1.x, p2.x, 0.001F) && PZMath.equal(p1.y, p2.y, 0.001F);
    }

    public ArrayList<Vector3f> stringPull(ArrayList<ISearchNode> nodes) {
        ArrayList<Vector3f> portals = this.getPortalEdges(nodes);
        if (portals != null && portals.size() >= 6) {
            Vector3f portalApex = AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portals.get(0));
            Vector3f portalLeft = AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portals.get(0));
            Vector3f portalRight = AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portals.get(1));
            int apexIndex = 0;
            int leftIndex = 0;
            int rightIndex = 0;
            ArrayList<Vector3f> result = new ArrayList<>();
            result.add(AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portalApex));

            for (int i = 1; i < portals.size() / 2; i++) {
                Vector3f left = portals.get(i * 2);
                Vector3f right = portals.get(i * 2 + 1);
                if (this.triarea2(portalApex, portalRight, right) <= 0.0F) {
                    if (!this.isPointEqual(portalApex, portalRight) && !(this.triarea2(portalApex, portalLeft, right) > 0.0F)) {
                        result.add(AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portalLeft));
                        portalApex.set(portalLeft);
                        apexIndex = leftIndex;
                        portalLeft.set(portalApex);
                        portalRight.set(portalApex);
                        leftIndex = leftIndex;
                        rightIndex = apexIndex;
                        i = apexIndex;
                        continue;
                    }

                    portalRight.set(right);
                    rightIndex = i;
                }

                if (this.triarea2(portalApex, portalLeft, left) >= 0.0F) {
                    if (!this.isPointEqual(portalApex, portalLeft) && !(this.triarea2(portalApex, portalRight, left) < 0.0F)) {
                        result.add(AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portalRight));
                        portalApex.set(portalRight);
                        apexIndex = rightIndex;
                        portalLeft.set(portalApex);
                        portalRight.set(portalApex);
                        leftIndex = rightIndex;
                        rightIndex = rightIndex;
                        i = apexIndex;
                    } else {
                        portalLeft.set(left);
                        leftIndex = i;
                    }
                }
            }

            result.add(AnimalPathfind.getInstance().vector3fObjectPool.alloc().set(portals.get(portals.size() - 1)));
            AnimalPathfind.getInstance().vector3fObjectPool.release(portalApex);
            AnimalPathfind.getInstance().vector3fObjectPool.release(portalLeft);
            AnimalPathfind.getInstance().vector3fObjectPool.release(portalRight);
            return result;
        } else {
            return null;
        }
    }

    float triarea2(Vector3f a, Vector3f b, Vector3f c) {
        float ax = b.x - a.x;
        float ay = b.y - a.y;
        float bx = c.x - a.x;
        float by = c.y - a.y;
        return bx * ay - ax * by;
    }

    ArrayList<Vector3f> getPortalEdges(ArrayList<ISearchNode> nodes) {
        ArrayList<Vector3f> portals = new ArrayList<>();
        ObjectPool<Vector2f> vector2fPool = AnimalPathfind.getInstance().vector2fObjectPool;
        ObjectPool<Vector3f> vector3fPool = AnimalPathfind.getInstance().vector3fObjectPool;
        LowLevelSearchNode nodeStart = (LowLevelSearchNode)nodes.get(0);
        portals.add(vector3fPool.alloc().set(nodeStart.getX(), nodeStart.getY(), nodeStart.getZ()));
        portals.add(vector3fPool.alloc().set(nodeStart.getX(), nodeStart.getY(), nodeStart.getZ()));
        Vector2f edge1 = vector2fPool.alloc();
        Vector2f edge2 = vector2fPool.alloc();
        Vector2f overlap1 = vector2fPool.alloc();
        Vector2f overlap2 = vector2fPool.alloc();

        for (int i = 0; i < nodes.size() - 2; i++) {
            LowLevelSearchNode node1 = (LowLevelSearchNode)nodes.get(i);
            LowLevelSearchNode node2 = (LowLevelSearchNode)nodes.get(i + 1);
            if (node1.meshList.z != node2.meshList.z) {
                int x2 = (int)node2.getX();
                int y2 = (int)node2.getY();
                if (Math.abs(node2.getX() - node1.getX()) > Math.abs(node2.getY() - node1.getY())) {
                    portals.add(vector3fPool.alloc().set(x2 + 0.0F, y2 + 0.0F, node2.getZ()));
                    portals.add(vector3fPool.alloc().set(x2 + 0.0F, y2 + 1.0F, node2.getZ()));
                } else {
                    portals.add(vector3fPool.alloc().set(x2 + 0.0F, y2 + 0.0F, node2.getZ()));
                    portals.add(vector3fPool.alloc().set(x2 + 1.0F, y2 + 0.0F, node2.getZ()));
                }
            } else {
                Mesh mesh1 = node1.meshList.get(node1.meshIdx);
                Mesh mesh2 = node2.meshList.get(node2.meshIdx);
                int edges = this.getSharedEdge(mesh1, node1.triangleIdx, mesh2, node2.triangleIdx, edge1, edge2, overlap1, overlap2);
                if (edges == -1) {
                    return null;
                }

                int e1 = this.unpackEdge1(edges);
                Vector2f p1 = mesh1.triangles.get(node1.triangleIdx + e1);
                Vector2f p2 = mesh1.triangles.get(node1.triangleIdx + (e1 + 1) % 3);
                float len1 = Vector2f.distanceSquared(p1.x, p1.y, p2.x, p2.y);
                int e2 = this.unpackEdge2(edges);
                Vector2f p3 = mesh2.triangles.get(node2.triangleIdx + e2);
                Vector2f p4 = mesh2.triangles.get(node2.triangleIdx + (e2 + 1) % 3);
                float len2 = Vector2f.distanceSquared(p3.x, p3.y, p4.x, p4.y);
                if (!PZMath.equal(len1, len2, 0.01F)) {
                    portals.add(vector3fPool.alloc().set((overlap1.x + overlap2.x) / 2.0F, (overlap1.y + overlap2.y) / 2.0F, node1.getZ()));
                    portals.add(vector3fPool.alloc().set((overlap1.x + overlap2.x) / 2.0F, (overlap1.y + overlap2.y) / 2.0F, node1.getZ()));
                } else {
                    float x0 = node1.x;
                    float y0 = node1.y;
                    float x1 = node2.x;
                    float y1 = node2.y;
                    float x2 = edge1.x;
                    float y2 = edge1.y;
                    float isLeft = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
                    if (isLeft > 0.0F) {
                        portals.add(vector3fPool.alloc().set(edge1.x, edge1.y, node1.getZ()));
                        portals.add(vector3fPool.alloc().set(edge2.x, edge2.y, node1.getZ()));
                    } else {
                        portals.add(vector3fPool.alloc().set(edge2.x, edge2.y, node1.getZ()));
                        portals.add(vector3fPool.alloc().set(edge1.x, edge1.y, node1.getZ()));
                    }
                }
            }
        }

        LowLevelSearchNode nodeEnd = (LowLevelSearchNode)nodes.get(nodes.size() - 1);
        portals.add(vector3fPool.alloc().set(nodeEnd.getX(), nodeEnd.getY(), nodeEnd.getZ()));
        portals.add(vector3fPool.alloc().set(nodeEnd.getX(), nodeEnd.getY(), nodeEnd.getZ()));
        vector2fPool.release(edge1);
        vector2fPool.release(edge2);
        vector2fPool.release(overlap1);
        vector2fPool.release(overlap2);
        return portals;
    }

    int packEdges(int edge1, int edge2) {
        return edge2 << 8 | edge1;
    }

    int unpackEdge1(int packed) {
        return packed & 0xFF;
    }

    int unpackEdge2(int packed) {
        return packed >> 8 & 0xFF;
    }

    int getSharedEdge(Mesh mesh1, int tri1, Mesh mesh2, int tri2, Vector2f edge1, Vector2f edge2, Vector2f overlap1, Vector2f overlap2) {
        Vector2f p1 = mesh1.triangles.get(tri1);
        Vector2f p2 = mesh1.triangles.get(tri1 + 1);
        Vector2f p3 = mesh1.triangles.get(tri1 + 2);
        Vector2f p4 = mesh2.triangles.get(tri2);
        Vector2f p5 = mesh2.triangles.get(tri2 + 1);
        Vector2f p6 = mesh2.triangles.get(tri2 + 2);
        if (edge1 != null) {
            edge1.set(p1);
            edge2.set(p2);
        }

        if (RobustLineIntersector.computeIntersection(p1, p2, p4, p5, overlap1, overlap2) == 2) {
            return this.packEdges(0, 0);
        } else if (RobustLineIntersector.computeIntersection(p1, p2, p5, p6, overlap1, overlap2) == 2) {
            return this.packEdges(0, 1);
        } else if (RobustLineIntersector.computeIntersection(p1, p2, p6, p4, overlap1, overlap2) == 2) {
            return this.packEdges(0, 2);
        } else {
            if (edge1 != null) {
                edge1.set(p2);
                edge2.set(p3);
            }

            if (RobustLineIntersector.computeIntersection(p2, p3, p4, p5, overlap1, overlap2) == 2) {
                return this.packEdges(1, 0);
            } else if (RobustLineIntersector.computeIntersection(p2, p3, p5, p6, overlap1, overlap2) == 2) {
                return this.packEdges(1, 1);
            } else if (RobustLineIntersector.computeIntersection(p2, p3, p6, p4, overlap1, overlap2) == 2) {
                return this.packEdges(1, 2);
            } else {
                if (edge1 != null) {
                    edge1.set(p3.x, p3.y);
                    edge2.set(p1.x, p1.y);
                }

                if (RobustLineIntersector.computeIntersection(p3, p1, p4, p5, overlap1, overlap2) == 2) {
                    return this.packEdges(2, 0);
                } else if (RobustLineIntersector.computeIntersection(p3, p1, p5, p6, overlap1, overlap2) == 2) {
                    return this.packEdges(2, 1);
                } else {
                    return RobustLineIntersector.computeIntersection(p3, p1, p6, p4, overlap1, overlap2) == 2 ? this.packEdges(2, 2) : -1;
                }
            }
        }
    }
}
