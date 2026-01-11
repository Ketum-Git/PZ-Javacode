// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.zones;

import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.LiangBarsky;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.util.SharedStrings;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperOffset;

@UsedFromLua
public class Zone {
    static final LiangBarsky LIANG_BARSKY = new LiangBarsky();
    static final Vector2 L_lineSegmentIntersects = new Vector2();
    private static final List<String> s_PreferredZoneTypes = List.of("DeepForest", "Farm", "FarmLand", "Forest", "Vegitation", "Nav", "TownZone", "TrailerPark");
    public static Clipper clipper;
    public HashMap<String, Integer> spawnedZombies;
    public final TIntArrayList points = new TIntArrayList();
    public UUID id;
    public int hourLastSeen;
    public int lastActionTimestamp;
    public boolean haveConstruction;
    public String zombiesTypeToSpawn;
    public Boolean spawnSpecialZombies;
    public String name;
    public String type;
    public int x;
    public int y;
    public int z;
    public int w;
    public int h;
    public ZoneGeometryType geometryType = ZoneGeometryType.INVALID;
    public int polylineWidth;
    public float[] polylineOutlinePoints;
    public float[] triangles;
    public float[] triangleAreas;
    public float totalArea;
    public int pickedXForZoneStory;
    public int pickedYForZoneStory;
    public RandomizedZoneStoryBase pickedRzStory;
    public boolean isPreferredZoneForSquare;
    private boolean triangulateFailed;
    private String originalName;

    public Zone() {
    }

    public Zone(String name, String type, int x, int y, int z, int w, int h) {
        this.id = UUID.randomUUID();
        this.originalName = name;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.h = h;
    }

    public Zone(String name, String type, int x, int y, int z, int w, int h, ZoneGeometryType geometryType, TIntArrayList points, int polylineWidth) {
        this(name, type, x, y, z, w, h);
        this.geometryType = geometryType;
        if (points != null) {
            this.points.addAll(points);
            this.polylineWidth = polylineWidth;
        }

        this.isPreferredZoneForSquare = isPreferredZoneForSquare(this.getType());
    }

    public Zone load(ByteBuffer input, int WorldVersion, Map<Integer, String> stringMap, SharedStrings sharedStrings) {
        this.name = sharedStrings.get(stringMap.get(Integer.valueOf(input.getShort())));
        this.type = sharedStrings.get(stringMap.get(Integer.valueOf(input.getShort())));
        this.loadData(input, WorldVersion);
        this.setOriginalName(stringMap.get(Integer.valueOf(input.getShort())));
        if (WorldVersion >= 215) {
            this.id = GameWindow.ReadUUID(input);
        } else {
            input.getDouble();
            this.id = UUID.randomUUID();
        }

        return this;
    }

    public Zone load(ByteBuffer input, int WorldVersion) {
        this.name = GameWindow.ReadStringUTF(input);
        this.type = GameWindow.ReadStringUTF(input);
        this.loadData(input, WorldVersion);
        this.setOriginalName(GameWindow.ReadStringUTF(input));
        if (WorldVersion >= 215) {
            this.id = GameWindow.ReadUUID(input);
        } else {
            input.getDouble();
            this.id = UUID.randomUUID();
        }

        return this;
    }

    private void loadData(ByteBuffer input, int WorldVersion) {
        this.x = input.getInt();
        this.y = input.getInt();
        this.z = input.get();
        this.w = input.getInt();
        this.h = input.getInt();
        TIntArrayList points = new TIntArrayList();
        ZoneGeometryType[] zoneGeometryTypes = ZoneGeometryType.values();
        int polylineWidth = 0;
        int zoneGeometryType = input.get();
        if (zoneGeometryType < 0 || zoneGeometryType >= zoneGeometryTypes.length) {
            zoneGeometryType = 0;
        }

        this.geometryType = zoneGeometryTypes[zoneGeometryType];
        if (this.geometryType != ZoneGeometryType.INVALID) {
            if (this.geometryType == ZoneGeometryType.Polyline) {
                polylineWidth = PZMath.clamp(input.get(), 0, 255);
            }

            short numPoints = input.getShort();

            for (int j = 0; j < numPoints; j++) {
                points.add(input.getInt());
            }

            this.points.addAll(points);
            this.polylineWidth = polylineWidth;
        }

        this.isPreferredZoneForSquare = isPreferredZoneForSquare(this.getType());
        this.hourLastSeen = input.getInt();
        this.haveConstruction = input.get() == 1;
        this.lastActionTimestamp = input.getInt();
    }

    public static boolean isPreferredZoneForSquare(String type) {
        return s_PreferredZoneTypes.contains(type);
    }

    public void save(ByteBuffer output, Map<String, Integer> stringMap) {
        output.putShort(stringMap.get(this.getName()).shortValue());
        output.putShort(stringMap.get(this.getType()).shortValue());
        this.saveData(output);
        output.putShort(stringMap.get(this.getOriginalName()).shortValue());
        GameWindow.WriteUUID(output, this.id);
    }

    public void save(ByteBuffer output) {
        GameWindow.WriteStringUTF(output, this.getName());
        GameWindow.WriteStringUTF(output, this.getType());
        this.saveData(output);
        GameWindow.WriteStringUTF(output, this.getOriginalName());
        GameWindow.WriteUUID(output, this.id);
    }

    private void saveData(ByteBuffer output) {
        output.putInt(this.x);
        output.putInt(this.y);
        output.put((byte)this.z);
        output.putInt(this.w);
        output.putInt(this.h);
        output.put((byte)this.geometryType.ordinal());
        if (!this.isRectangle()) {
            if (this.isPolyline()) {
                output.put((byte)this.polylineWidth);
            }

            output.putShort((short)this.points.size());

            for (int j = 0; j < this.points.size(); j++) {
                output.putInt(this.points.get(j));
            }
        }

        output.putInt(this.hourLastSeen);
        output.put((byte)(this.haveConstruction ? 1 : 0));
        output.putInt(this.lastActionTimestamp);
    }

    public boolean isFullyStreamed() {
        IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
        IsoGridSquare sq2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w - 1, this.y + this.h - 1, this.z);
        return sq != null && sq2 != null;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setH(int h) {
        this.h = h;
    }

    public boolean isPoint() {
        return this.geometryType == ZoneGeometryType.Point;
    }

    public boolean isPolygon() {
        return this.geometryType == ZoneGeometryType.Polygon;
    }

    public boolean isPolyline() {
        return this.geometryType == ZoneGeometryType.Polyline;
    }

    public boolean isRectangle() {
        return this.geometryType == ZoneGeometryType.INVALID;
    }

    public void setPickedXForZoneStory(int pickedXForZoneStory) {
        this.pickedXForZoneStory = pickedXForZoneStory;
    }

    public void setPickedYForZoneStory(int pickedYForZoneStory) {
        this.pickedYForZoneStory = pickedYForZoneStory;
    }

    public float getHoursSinceLastSeen() {
        return (float)GameTime.instance.getWorldAgeHours() - this.hourLastSeen;
    }

    public void setHourSeenToCurrent() {
        if (!"Ranch".equals(this.type)) {
            this.hourLastSeen = (int)GameTime.instance.getWorldAgeHours();
        }
    }

    public void setHaveConstruction(boolean have) {
        this.haveConstruction = have;
        if (GameClient.client) {
            ByteBufferWriter bb = GameClient.connection.startPacket();
            PacketTypes.PacketType.ConstructedZone.doPacket(bb);
            bb.putInt(this.x);
            bb.putInt(this.y);
            bb.putInt(this.z);
            PacketTypes.PacketType.ConstructedZone.send(GameClient.connection);
        }
    }

    public boolean haveCons() {
        return this.haveConstruction;
    }

    public int getZombieDensity() {
        IsoMetaChunk c = IsoWorld.instance.metaGrid.getChunkDataFromTile(this.x, this.y);
        return c != null ? c.getUnadjustedZombieIntensity() : 0;
    }

    public boolean contains(int x, int y, int z) {
        if (z != this.z) {
            return false;
        } else if (x < this.x || x >= this.x + this.w) {
            return false;
        } else if (y < this.y || y >= this.y + this.h) {
            return false;
        } else if (this.isPoint()) {
            return false;
        } else if (this.isPolyline()) {
            if (this.polylineWidth > 0) {
                this.checkPolylineOutline();
                return this.isPointInPolyline_WindingNumber(x + 0.5F, y + 0.5F, 0) == Zone.PolygonHit.Inside;
            } else {
                return false;
            }
        } else {
            return this.isPolygon() ? this.isPointInPolygon_WindingNumber(x + 0.5F, y + 0.5F, 0) == Zone.PolygonHit.Inside : true;
        }
    }

    public boolean intersects(int x, int y, int z, int w, int h) {
        if (this.z != z) {
            return false;
        } else if (x + w > this.x && x < this.x + this.w) {
            if (y + h <= this.y || y >= this.y + this.h) {
                return false;
            } else if (this.isPolygon()) {
                return this.polygonRectIntersect(x, y, w, h);
            } else if (this.isPolyline()) {
                if (this.polylineWidth > 0) {
                    this.checkPolylineOutline();
                    return this.polylineOutlineRectIntersect(x, y, w, h);
                } else {
                    for (int i = 0; i < this.points.size() - 2; i += 2) {
                        int x1 = this.points.getQuick(i);
                        int y1 = this.points.getQuick(i + 1);
                        int x2 = this.points.getQuick(i + 2);
                        int y2 = this.points.getQuick(i + 3);
                        if (LIANG_BARSKY.lineRectIntersect(x1, y1, x2 - x1, y2 - y1, x, y, x + w, y + h)) {
                            return true;
                        }
                    }

                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean difference(int x, int y, int z, int w, int h, ArrayList<Zone> result) {
        result.clear();
        if (!this.intersects(x, y, z, w, h)) {
            return false;
        } else if (this.isRectangle()) {
            if (this.x < x) {
                int top = Math.max(y, this.y);
                int bottom = Math.min(y + h, this.y + this.h);
                result.add(new Zone(this.name, this.type, this.x, top, z, x - this.x, bottom - top));
            }

            if (x + w < this.x + this.w) {
                int top = Math.max(y, this.y);
                int bottom = Math.min(y + h, this.y + this.h);
                result.add(new Zone(this.name, this.type, x + w, top, z, this.x + this.w - (x + w), bottom - top));
            }

            if (this.y < y) {
                result.add(new Zone(this.name, this.type, this.x, this.y, z, this.w, y - this.y));
            }

            if (y + h < this.y + this.h) {
                result.add(new Zone(this.name, this.type, this.x, y + h, z, this.w, this.y + this.h - (y + h)));
            }

            return true;
        } else {
            if (this.isPolygon()) {
                if (Zone.clipper == null) {
                    Zone.clipper = new Clipper();
                    IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
                }

                Clipper clipper = Zone.clipper;
                ByteBuffer clipperBuffer = IsoMetaGrid.clipperBuffer;
                clipperBuffer.clear();

                for (int i = 0; i < this.points.size(); i += 2) {
                    clipperBuffer.putFloat(this.points.getQuick(i));
                    clipperBuffer.putFloat(this.points.getQuick(i + 1));
                }

                clipper.clear();
                clipper.addPath(this.points.size() / 2, clipperBuffer, false);
                clipper.clipAABB(x, y, x + w, y + h);
                int numPolys = clipper.generatePolygons();

                for (int i = 0; i < numPolys; i++) {
                    clipperBuffer.clear();
                    clipper.getPolygon(i, clipperBuffer);
                    int numPoints = clipperBuffer.getShort();
                    if (numPoints < 3) {
                        clipperBuffer.position(clipperBuffer.position() + numPoints * 4 * 2);
                    } else {
                        Zone zone = new Zone(this.name, this.type, this.x, this.y, this.z, this.w, this.h);
                        zone.geometryType = ZoneGeometryType.Polygon;

                        for (int j = 0; j < numPoints; j++) {
                            zone.points.add((int)clipperBuffer.getFloat());
                            zone.points.add((int)clipperBuffer.getFloat());
                        }

                        result.add(zone);
                    }
                }
            }

            if (this.isPolyline()) {
            }

            return true;
        }
    }

    private int pickRandomTriangle() {
        float[] triangles = this.isPolygon() ? this.getPolygonTriangles() : (this.isPolyline() ? this.getPolylineOutlineTriangles() : null);
        if (triangles == null) {
            return -1;
        } else {
            int numTriangles = triangles.length / 6;
            float r = Rand.Next(0.0F, this.totalArea);
            float totalArea = 0.0F;

            for (int i = 0; i < this.triangleAreas.length; i++) {
                totalArea += this.triangleAreas[i];
                if (totalArea >= r) {
                    return i;
                }
            }

            return Rand.Next(numTriangles);
        }
    }

    private Vector2 pickRandomPointInTriangle(int triangleIndex, Vector2 out) {
        float ax = this.triangles[triangleIndex * 3 * 2];
        float ay = this.triangles[triangleIndex * 3 * 2 + 1];
        float bx = this.triangles[triangleIndex * 3 * 2 + 2];
        float by = this.triangles[triangleIndex * 3 * 2 + 3];
        float cx = this.triangles[triangleIndex * 3 * 2 + 4];
        float cy = this.triangles[triangleIndex * 3 * 2 + 5];
        float s = Rand.Next(0.0F, 1.0F);
        float t = Rand.Next(0.0F, 1.0F);
        boolean inTriangle = s + t <= 1.0F;
        float px;
        float py;
        if (inTriangle) {
            px = s * (bx - ax) + t * (cx - ax);
            py = s * (by - ay) + t * (cy - ay);
        } else {
            px = (1.0F - s) * (bx - ax) + (1.0F - t) * (cx - ax);
            py = (1.0F - s) * (by - ay) + (1.0F - t) * (cy - ay);
        }

        px += ax;
        py += ay;
        return out.set(px, py);
    }

    public IsoGameCharacter.Location pickRandomLocation(IsoGameCharacter.Location location) {
        if (this.isPolygon() || this.isPolyline() && this.polylineWidth > 0) {
            int triangleIndex = this.pickRandomTriangle();
            if (triangleIndex == -1) {
                return null;
            } else {
                for (int i = 0; i < 20; i++) {
                    Vector2 p = this.pickRandomPointInTriangle(triangleIndex, BaseVehicle.allocVector2());
                    if (this.contains((int)p.x, (int)p.y, this.z)) {
                        location.set((int)p.x, (int)p.y, this.z);
                        BaseVehicle.releaseVector2(p);
                        return location;
                    }

                    BaseVehicle.releaseVector2(p);
                }

                return null;
            }
        } else {
            return !this.isPoint() && !this.isPolyline() ? location.set(Rand.Next(this.x, this.x + this.w), Rand.Next(this.y, this.y + this.h), this.z) : null;
        }
    }

    public IsoGridSquare getRandomSquareInZone() {
        IsoGameCharacter.Location location = this.pickRandomLocation(IsoMetaGrid.TL_Location.get());
        return location == null ? null : IsoWorld.instance.currentCell.getGridSquare(location.x, location.y, location.z);
    }

    public IsoGridSquare getRandomFreeSquareInZone() {
        for (int it = 100; it > 0; it--) {
            IsoGameCharacter.Location location = this.pickRandomLocation(IsoMetaGrid.TL_Location.get());
            if (location == null) {
                return null;
            }

            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(location.x, location.y, location.z);
            if (sq != null && sq.isFree(true)) {
                return sq;
            }
        }

        return null;
    }

    public IsoGridSquare getRandomUnseenSquareInZone() {
        return null;
    }

    public void addSquare(IsoGridSquare sq) {
    }

    public ArrayList<IsoGridSquare> getSquares() {
        return null;
    }

    public void removeSquare(IsoGridSquare sq) {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLastActionTimestamp() {
        return this.lastActionTimestamp;
    }

    public void setLastActionTimestamp(int lastActionTimestamp) {
        this.lastActionTimestamp = lastActionTimestamp;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return this.z;
    }

    public int getHeight() {
        return this.h;
    }

    public int getWidth() {
        return this.w;
    }

    public float getTotalArea() {
        if (!this.isRectangle() && !this.isPoint() && (!this.isPolyline() || this.polylineWidth > 0)) {
            this.getPolygonTriangles();
            this.getPolylineOutlineTriangles();
            return this.totalArea;
        } else {
            return this.getWidth() * this.getHeight();
        }
    }

    public void sendToServer() {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.RegisterZone, this);
        }
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public int getClippedSegmentOfPolyline(int clipX1, int clipY1, int clipX2, int clipY2, double[] t1t2) {
        if (!this.isPolyline()) {
            return -1;
        } else {
            float dxy = this.polylineWidth % 2 == 0 ? 0.0F : 0.5F;

            for (int i = 0; i < this.points.size() - 2; i += 2) {
                int x1 = this.points.getQuick(i);
                int y1 = this.points.getQuick(i + 1);
                int x2 = this.points.getQuick(i + 2);
                int y2 = this.points.getQuick(i + 3);
                if (LIANG_BARSKY.lineRectIntersect(x1 + dxy, y1 + dxy, x2 - x1, y2 - y1, clipX1, clipY1, clipX2, clipY2, t1t2)) {
                    return i / 2;
                }
            }

            return -1;
        }
    }

    private void checkPolylineOutline() {
        if (this.polylineOutlinePoints == null) {
            if (this.isPolyline()) {
                if (this.polylineWidth > 0) {
                    if (IsoMetaGrid.clipperOffset == null) {
                        IsoMetaGrid.clipperOffset = new ClipperOffset();
                        IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
                    }

                    ClipperOffset clipperOffset = IsoMetaGrid.clipperOffset;
                    ByteBuffer clipperBuffer = IsoMetaGrid.clipperBuffer;
                    clipperOffset.clear();
                    clipperBuffer.clear();
                    float dxy = this.polylineWidth % 2 == 0 ? 0.0F : 0.5F;

                    for (int j = 0; j < this.points.size(); j += 2) {
                        int x1 = this.points.get(j);
                        int y1 = this.points.get(j + 1);
                        clipperBuffer.putFloat(x1 + dxy);
                        clipperBuffer.putFloat(y1 + dxy);
                    }

                    clipperBuffer.flip();
                    clipperOffset.addPath(this.points.size() / 2, clipperBuffer, ClipperOffset.JoinType.Miter.ordinal(), ClipperOffset.EndType.Butt.ordinal());
                    clipperOffset.execute(this.polylineWidth / 2.0F);
                    int numPolys = clipperOffset.getPolygonCount();
                    if (numPolys < 1) {
                        DebugLog.General.warn("Failed to generate polyline outline");
                    } else {
                        clipperBuffer.clear();
                        clipperOffset.getPolygon(0, clipperBuffer);
                        short pointCount = clipperBuffer.getShort();
                        this.polylineOutlinePoints = new float[pointCount * 2];

                        for (int k = 0; k < pointCount; k++) {
                            this.polylineOutlinePoints[k * 2] = clipperBuffer.getFloat();
                            this.polylineOutlinePoints[k * 2 + 1] = clipperBuffer.getFloat();
                        }
                    }
                }
            }
        }
    }

    float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    Zone.PolygonHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
        int wn = 0;

        for (int i = 0; i < this.points.size(); i += 2) {
            int x1 = this.points.getQuick(i);
            int y1 = this.points.getQuick(i + 1);
            int x2 = this.points.getQuick((i + 2) % this.points.size());
            int y2 = this.points.getQuick((i + 3) % this.points.size());
            if (y1 <= y) {
                if (y2 > y && this.isLeft(x1, y1, x2, y2, x, y) > 0.0F) {
                    wn++;
                }
            } else if (y2 <= y && this.isLeft(x1, y1, x2, y2, x, y) < 0.0F) {
                wn--;
            }
        }

        return wn == 0 ? Zone.PolygonHit.Outside : Zone.PolygonHit.Inside;
    }

    Zone.PolygonHit isPointInPolyline_WindingNumber(float x, float y, int flags) {
        int wn = 0;
        float[] points = this.polylineOutlinePoints;
        if (points == null) {
            return Zone.PolygonHit.Outside;
        } else {
            for (int i = 0; i < points.length; i += 2) {
                float x1 = points[i];
                float y1 = points[i + 1];
                float x2 = points[(i + 2) % points.length];
                float y2 = points[(i + 3) % points.length];
                if (y1 <= y) {
                    if (y2 > y && this.isLeft(x1, y1, x2, y2, x, y) > 0.0F) {
                        wn++;
                    }
                } else if (y2 <= y && this.isLeft(x1, y1, x2, y2, x, y) < 0.0F) {
                    wn--;
                }
            }

            return wn == 0 ? Zone.PolygonHit.Outside : Zone.PolygonHit.Inside;
        }
    }

    boolean polygonRectIntersect(int x, int y, int w, int h) {
        return this.x >= x && this.x + this.w <= x + w && this.y >= y && this.y + this.h <= y + h
            ? true
            : this.lineSegmentIntersects(x, y, x + w, y)
                || this.lineSegmentIntersects(x + w, y, x + w, y + h)
                || this.lineSegmentIntersects(x + w, y + h, x, y + h)
                || this.lineSegmentIntersects(x, y + h, x, y);
    }

    boolean lineSegmentIntersects(float sx, float sy, float ex, float ey) {
        L_lineSegmentIntersects.set(ex - sx, ey - sy);
        float lineSegmentLength = L_lineSegmentIntersects.getLength();
        L_lineSegmentIntersects.normalize();
        float dirX = L_lineSegmentIntersects.x;
        float dirY = L_lineSegmentIntersects.y;

        for (int j = 0; j < this.points.size(); j += 2) {
            float node1x = this.points.getQuick(j);
            float node1y = this.points.getQuick(j + 1);
            float node2x = this.points.getQuick((j + 2) % this.points.size());
            float node2y = this.points.getQuick((j + 3) % this.points.size());
            float doaX = sx - node1x;
            float doaY = sy - node1y;
            float dbaX = node2x - node1x;
            float dbaY = node2y - node1y;
            float invDbaDir = 1.0F / (dbaY * dirX - dbaX * dirY);
            float t = (dbaX * doaY - dbaY * doaX) * invDbaDir;
            if (t >= 0.0F && t <= lineSegmentLength) {
                float t2 = (doaY * dirX - doaX * dirY) * invDbaDir;
                if (t2 >= 0.0F && t2 <= 1.0F) {
                    return true;
                }
            }
        }

        return this.isPointInPolygon_WindingNumber((sx + ex) / 2.0F, (sy + ey) / 2.0F, 0) != Zone.PolygonHit.Outside;
    }

    boolean polylineOutlineRectIntersect(int x, int y, int w, int h) {
        if (this.polylineOutlinePoints == null) {
            return false;
        } else {
            return this.x >= x && this.x + this.w <= x + w && this.y >= y && this.y + this.h <= y + h
                ? true
                : this.polylineOutlineSegmentIntersects(x, y, x + w, y)
                    || this.polylineOutlineSegmentIntersects(x + w, y, x + w, y + h)
                    || this.polylineOutlineSegmentIntersects(x + w, y + h, x, y + h)
                    || this.polylineOutlineSegmentIntersects(x, y + h, x, y);
        }
    }

    boolean polylineOutlineSegmentIntersects(float sx, float sy, float ex, float ey) {
        L_lineSegmentIntersects.set(ex - sx, ey - sy);
        float lineSegmentLength = L_lineSegmentIntersects.getLength();
        L_lineSegmentIntersects.normalize();
        float dirX = L_lineSegmentIntersects.x;
        float dirY = L_lineSegmentIntersects.y;
        float[] points = this.polylineOutlinePoints;

        for (int j = 0; j < points.length; j += 2) {
            float node1x = points[j];
            float node1y = points[j + 1];
            float node2x = points[(j + 2) % points.length];
            float node2y = points[(j + 3) % points.length];
            float doaX = sx - node1x;
            float doaY = sy - node1y;
            float dbaX = node2x - node1x;
            float dbaY = node2y - node1y;
            float invDbaDir = 1.0F / (dbaY * dirX - dbaX * dirY);
            float t = (dbaX * doaY - dbaY * doaX) * invDbaDir;
            if (t >= 0.0F && t <= lineSegmentLength) {
                float t2 = (doaY * dirX - doaX * dirY) * invDbaDir;
                if (t2 >= 0.0F && t2 <= 1.0F) {
                    return true;
                }
            }
        }

        return this.isPointInPolyline_WindingNumber((sx + ex) / 2.0F, (sy + ey) / 2.0F, 0) != Zone.PolygonHit.Outside;
    }

    private boolean isClockwise() {
        if (!this.isPolygon()) {
            return false;
        } else {
            float sum = 0.0F;

            for (int i = 0; i < this.points.size(); i += 2) {
                int p1x = this.points.getQuick(i);
                int p1y = this.points.getQuick(i + 1);
                int p2x = this.points.getQuick((i + 2) % this.points.size());
                int p2y = this.points.getQuick((i + 3) % this.points.size());
                sum += (p2x - p1x) * (p2y + p1y);
            }

            return sum > 0.0;
        }
    }

    public float[] getPolygonTriangles() {
        if (this.triangles != null) {
            return this.triangles;
        } else if (this.triangulateFailed) {
            return null;
        } else if (!this.isPolygon()) {
            return null;
        } else {
            if (Zone.clipper == null) {
                Zone.clipper = new Clipper();
                IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
            }

            Clipper clipper = Zone.clipper;
            ByteBuffer clipperBuffer = IsoMetaGrid.clipperBuffer;
            clipperBuffer.clear();
            if (this.isClockwise()) {
                for (int i = this.points.size() - 1; i > 0; i -= 2) {
                    clipperBuffer.putFloat(this.points.getQuick(i - 1));
                    clipperBuffer.putFloat(this.points.getQuick(i));
                }
            } else {
                for (int i = 0; i < this.points.size(); i += 2) {
                    clipperBuffer.putFloat(this.points.getQuick(i));
                    clipperBuffer.putFloat(this.points.getQuick(i + 1));
                }
            }

            clipper.clear();
            clipper.addPath(this.points.size() / 2, clipperBuffer, false);
            int numPolys = clipper.generatePolygons();
            if (numPolys < 1) {
                this.triangulateFailed = true;
                return null;
            } else {
                clipperBuffer.clear();
                int numPoints = clipper.triangulate(0, clipperBuffer);
                this.triangles = new float[numPoints * 2];

                for (int i = 0; i < numPoints; i++) {
                    this.triangles[i * 2] = clipperBuffer.getFloat();
                    this.triangles[i * 2 + 1] = clipperBuffer.getFloat();
                }

                this.initTriangleAreas();
                return this.triangles;
            }
        }
    }

    private float triangleArea(float x0, float y0, float x1, float y1, float x2, float y2) {
        float a = Vector2f.length(x1 - x0, y1 - y0);
        float b = Vector2f.length(x2 - x1, y2 - y1);
        float c = Vector2f.length(x0 - x2, y0 - y2);
        float s = (a + b + c) / 2.0F;
        return (float)Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }

    private void initTriangleAreas() {
        int numTriangles = this.triangles.length / 6;
        this.triangleAreas = new float[numTriangles];
        this.totalArea = 0.0F;

        for (int i = 0; i < this.triangles.length; i += 6) {
            float x1 = this.triangles[i];
            float y1 = this.triangles[i + 1];
            float x2 = this.triangles[i + 2];
            float y2 = this.triangles[i + 3];
            float x3 = this.triangles[i + 4];
            float y3 = this.triangles[i + 5];
            float area = this.triangleArea(x1, y1, x2, y2, x3, y3);
            this.triangleAreas[i / 6] = area;
            this.totalArea += area;
        }
    }

    public float[] getPolylineOutlineTriangles() {
        if (this.triangles != null) {
            return this.triangles;
        } else if (!this.isPolyline() || this.polylineWidth <= 0) {
            return null;
        } else if (this.triangulateFailed) {
            return null;
        } else {
            this.checkPolylineOutline();
            float[] points = this.polylineOutlinePoints;
            if (points == null) {
                this.triangulateFailed = true;
                return null;
            } else {
                if (Zone.clipper == null) {
                    Zone.clipper = new Clipper();
                    IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
                }

                Clipper clipper = Zone.clipper;
                ByteBuffer clipperBuffer = IsoMetaGrid.clipperBuffer;
                clipperBuffer.clear();
                if (this.isClockwise()) {
                    for (int i = points.length - 1; i > 0; i -= 2) {
                        clipperBuffer.putFloat(points[i - 1]);
                        clipperBuffer.putFloat(points[i]);
                    }
                } else {
                    for (int i = 0; i < points.length; i += 2) {
                        clipperBuffer.putFloat(points[i]);
                        clipperBuffer.putFloat(points[i + 1]);
                    }
                }

                clipper.clear();
                clipper.addPath(points.length / 2, clipperBuffer, false);
                int numPolys = clipper.generatePolygons();
                if (numPolys < 1) {
                    this.triangulateFailed = true;
                    return null;
                } else {
                    clipperBuffer.clear();
                    int numPoints = clipper.triangulate(0, clipperBuffer);
                    this.triangles = new float[numPoints * 2];

                    for (int i = 0; i < numPoints; i++) {
                        this.triangles[i * 2] = clipperBuffer.getFloat();
                        this.triangles[i * 2 + 1] = clipperBuffer.getFloat();
                    }

                    this.initTriangleAreas();
                    return this.triangles;
                }
            }
        }
    }

    public float getPolylineLength() {
        if (this.isPolyline() && !this.points.isEmpty()) {
            float length = 0.0F;

            for (int i = 0; i < this.points.size() - 2; i += 2) {
                int x1 = this.points.get(i);
                int y1 = this.points.get(i + 1);
                int x2 = this.points.get(i + 2);
                int y2 = this.points.get(i + 3);
                length += Vector2f.length(x2 - x1, y2 - y1);
            }

            return length;
        } else {
            return 0.0F;
        }
    }

    public void Dispose() {
        this.pickedRzStory = null;
        this.points.clear();
        this.polylineOutlinePoints = null;
        if (this.spawnedZombies != null) {
            this.spawnedZombies.clear();
            this.spawnedZombies = null;
        }

        this.triangles = null;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Zone{");
        sb.append("name='").append(this.name).append('\'');
        sb.append(", type='").append(this.type).append('\'');
        sb.append(", x=").append(this.x);
        sb.append(", y=").append(this.y);
        sb.append(", w=").append(this.w);
        sb.append(", h=").append(this.h);
        sb.append(", id=").append(this.id.toString());
        sb.append('}');
        return sb.toString();
    }

    private static enum PolygonHit {
        OnEdge,
        Inside,
        Outside;
    }
}
