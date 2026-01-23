// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.util.Lambda;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.SharedStrings;

public final class WorldMapXML {
    private final SharedStrings sharedStrings = new SharedStrings();
    private final WorldMapPoint point = new WorldMapPoint();
    private final WorldMapProperties properties = new WorldMapProperties();
    private final ArrayList<WorldMapProperties> sharedProperties = new ArrayList<>();

    public boolean read(String filePath, WorldMapData data) throws PZXmlParserException {
        Element root = PZXmlUtil.parseXml(filePath);
        if (root.getNodeName().equals("world")) {
            this.parseWorld(root, data);
            return true;
        } else {
            return false;
        }
    }

    private void parseWorld(Element node, WorldMapData data) {
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, data, (child, l_data) -> {
            if (!child.getNodeName().equals("cell")) {
                DebugLog.General.warn("Warning: Unrecognised element '" + child.getNodeName());
            } else {
                WorldMapCell cell = this.parseCell(child);
                l_data.cells.add(cell);
            }
        });
    }

    private WorldMapCell parseCell(Element node) {
        WorldMapCell cell = new WorldMapCell();
        cell.x = PZMath.tryParseInt(node.getAttribute("x"), 0);
        cell.y = PZMath.tryParseInt(node.getAttribute("y"), 0);
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, cell, (child, l_cell) -> {
            try {
                String s = child.getNodeName();
                if ("feature".equalsIgnoreCase(s)) {
                    WorldMapFeature feature = this.parseFeature(cell, child);
                    l_cell.features.add(feature);
                }
            } catch (Exception var6) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(var6);
            }
        });
        return cell;
    }

    private WorldMapFeature parseFeature(WorldMapCell cell, Element node) {
        WorldMapFeature feature = new WorldMapFeature(cell);
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, feature, (child, l_feature) -> {
            try {
                String s = child.getNodeName();
                if ("geometry".equalsIgnoreCase(s)) {
                    WorldMapGeometry geometry = this.parseGeometry(cell, child);
                    if (l_feature.geometry != null) {
                        throw new RuntimeException("only one feature geometry is supported");
                    }

                    l_feature.geometry = geometry;
                }

                if ("properties".equalsIgnoreCase(s)) {
                    this.parseFeatureProperties(child, l_feature);
                }
            } catch (Exception var6) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(var6);
            }
        });
        return feature;
    }

    private void parseFeatureProperties(Element node, WorldMapFeature feature) {
        this.properties.clear();
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, feature, (child, l_feature) -> {
            try {
                String s = child.getNodeName();
                if ("property".equalsIgnoreCase(s)) {
                    String name = this.sharedStrings.get(child.getAttribute("name"));
                    String value = this.sharedStrings.get(child.getAttribute("value"));
                    this.properties.put(name, value);
                }
            } catch (Exception var6) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(var6);
            }
        });
        feature.properties = this.getOrCreateProperties(this.properties);
    }

    private WorldMapProperties getOrCreateProperties(WorldMapProperties properties) {
        for (int i = 0; i < this.sharedProperties.size(); i++) {
            if (this.sharedProperties.get(i).equals(properties)) {
                return this.sharedProperties.get(i);
            }
        }

        WorldMapProperties result = new WorldMapProperties();
        result.putAll(properties);
        this.sharedProperties.add(result);
        return result;
    }

    private WorldMapGeometry parseGeometry(WorldMapCell cell, Element node) {
        WorldMapGeometry geometry = new WorldMapGeometry(cell);
        geometry.type = WorldMapGeometry.Type.valueOf(node.getAttribute("type"));
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, geometry, (child, l_geometry) -> {
            try {
                String s = child.getNodeName();
                if ("coordinates".equalsIgnoreCase(s)) {
                    WorldMapPoints points = new WorldMapPoints(geometry);
                    this.parseGeometryCoordinates(child, points);
                    l_geometry.points.add(points);
                }
            } catch (Exception var6) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(var6);
            }
        });
        geometry.calculateBounds();
        return geometry;
    }

    private int countPoints(Element node) {
        int pointCount = 0;

        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement && childElement.getNodeName().equalsIgnoreCase("point")) {
                pointCount++;
            }
        }

        return pointCount;
    }

    private void parseGeometryCoordinates(Element node, WorldMapPoints points) {
        int numPoints = this.countPoints(node);
        if (numPoints != 0) {
            WorldMapCell cell = points.owner.cell;
            ShortBuffer pointBuffer = cell.getPointBuffer(numPoints);
            int firstPoint = pointBuffer.position();
            Lambda.forEachFrom(PZXmlUtil::forEachElement, node, points, (child, l_points) -> {
                try {
                    String s = child.getNodeName();
                    if ("point".equalsIgnoreCase(s)) {
                        WorldMapPoint point = this.parsePoint(child, this.point);
                        pointBuffer.put((short)point.x);
                        pointBuffer.put((short)point.y);
                    }
                } catch (Exception var6x) {
                    DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                    DebugLog.General.error(var6x);
                }
            });
            int lastPoint = points.owner.cell.pointBuffer.position();
            points.setPoints((short)firstPoint, (short)(lastPoint - firstPoint));
        }
    }

    private WorldMapPoint parsePoint(Element node, WorldMapPoint point) {
        point.x = PZMath.tryParseInt(node.getAttribute("x"), 0);
        point.y = PZMath.tryParseInt(node.getAttribute("y"), 0);
        return point;
    }
}
