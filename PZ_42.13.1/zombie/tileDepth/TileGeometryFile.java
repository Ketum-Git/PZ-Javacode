// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import gnu.trove.list.array.TFloatArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.worldMap.Rasterize;

public final class TileGeometryFile {
    private static Clipper clipper;
    final ArrayList<TileGeometryFile.Tileset> tilesets = new ArrayList<>();
    private static final int COORD_MULT = 10000;
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION_LATEST = 2;
    int version = 2;

    void read(String fileName) {
        File file = new File(fileName);

        try (
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
        ) {
            CharBuffer totalFile = CharBuffer.allocate((int)file.length());
            br.read(totalFile);
            totalFile.flip();
            this.parseFile(totalFile.toString());
        } catch (FileNotFoundException var14) {
        } catch (Exception var15) {
            ExceptionLogger.logException(var15);
        }
    }

    void parseFile(String totalFile) throws IOException {
        totalFile = ScriptParser.stripComments(totalFile);
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        ScriptParser.Value value = block.getValue("VERSION");
        if (value == null) {
            throw new IOException("missing VERSION in tileGeometry.txt");
        } else {
            this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
            if (this.version >= 1 && this.version <= 2) {
                for (ScriptParser.Block child : block.children) {
                    if ("tileset".equals(child.type)) {
                        TileGeometryFile.Tileset tileset = this.parseTileset(child);
                        if (tileset != null) {
                            this.tilesets.add(tileset);
                        }
                    }
                }
            } else {
                throw new IOException(String.format("unknown tileGeometry.txt VERSION \"%s\"", value.getValue().trim()));
            }
        }
    }

    TileGeometryFile.Tileset parseTileset(ScriptParser.Block block) {
        TileGeometryFile.Tileset tileset = new TileGeometryFile.Tileset();
        ScriptParser.Value value = block.getValue("name");
        tileset.name = value.getValue().trim();

        for (ScriptParser.Block child : block.children) {
            if ("tile".equals(child.type)) {
                TileGeometryFile.Tile tile = this.parseTile(tileset, child);
                if (tile != null) {
                    int index = tileset.tiles.size();

                    for (int i = 0; i < tileset.tiles.size(); i++) {
                        TileGeometryFile.Tile tile2 = tileset.tiles.get(i);
                        if (tile2.col + tile2.row * 8 > tile.col + tile.row * 8) {
                            index = i;
                            break;
                        }
                    }

                    tileset.tiles.add(index, tile);
                }
            }
        }

        return tileset;
    }

    TileGeometryFile.Tile parseTile(TileGeometryFile.Tileset tileset, ScriptParser.Block block) {
        TileGeometryFile.Tile tile = new TileGeometryFile.Tile();
        ScriptParser.Value value = block.getValue("xy");
        String[] ss = value.getValue().trim().split("x");
        tile.col = Integer.parseInt(ss[0]);
        tile.row = Integer.parseInt(ss[1]);

        for (ScriptParser.Block child : block.children) {
            if ("box".equals(child.type)) {
                TileGeometryFile.Box box = this.parseBox(child);
                if (box != null) {
                    tile.geometry.add(box);
                }
            }

            if ("cylinder".equals(child.type)) {
                TileGeometryFile.Cylinder cylinder = this.parseCylinder(child);
                if (cylinder != null) {
                    tile.geometry.add(cylinder);
                }
            }

            if ("polygon".equals(child.type)) {
                TileGeometryFile.Polygon polygon = this.parsePolygon(child);
                if (polygon != null) {
                    tile.geometry.add(polygon);
                }
            }

            if ("properties".equals(child.type)) {
                tile.properties = this.parseTileProperties(child);
            }
        }

        return tile;
    }

    TileGeometryFile.Box parseBox(ScriptParser.Block block) {
        TileGeometryFile.Box box = new TileGeometryFile.Box();
        if (!this.parseVector3(block, "translate", box.translate)) {
            return null;
        } else if (!this.parseVector3(block, "rotate", box.rotate)) {
            return null;
        } else if (!this.parseVector3(block, "min", box.min)) {
            return null;
        } else {
            return !this.parseVector3(block, "max", box.max) ? null : box;
        }
    }

    TileGeometryFile.Cylinder parseCylinder(ScriptParser.Block block) {
        TileGeometryFile.Cylinder cylinder = new TileGeometryFile.Cylinder();
        if (!this.parseVector3(block, "translate", cylinder.translate)) {
            return null;
        } else if (!this.parseVector3(block, "rotate", cylinder.rotate)) {
            return null;
        } else {
            cylinder.radius1 = this.parseCoord(block, "radius1", -1.0F);
            if (cylinder.radius1 <= 0.0F) {
                return null;
            } else {
                cylinder.radius2 = this.parseCoord(block, "radius2", -1.0F);
                if (cylinder.radius2 <= 0.0F) {
                    return null;
                } else {
                    cylinder.height = this.parseCoord(block, "height", -1.0F);
                    return cylinder.height < 0.0F ? null : cylinder;
                }
            }
        }
    }

    TileGeometryFile.Polygon parsePolygon(ScriptParser.Block block) {
        TileGeometryFile.Polygon polygon = new TileGeometryFile.Polygon();
        if (!this.parseVector3(block, "translate", polygon.translate)) {
            return null;
        } else {
            ScriptParser.Value value = block.getValue("plane");
            polygon.plane = TileGeometryFile.Plane.valueOf(value.getValue().trim());
            if (block.getValue("rotate") == null) {
                switch (polygon.plane) {
                    case XY:
                        polygon.rotate.set(0.0F, 0.0F, 0.0F);
                        break;
                    case XZ:
                        polygon.rotate.set(90.0F, 0.0F, 0.0F);
                        break;
                    case YZ:
                        polygon.rotate.set(0.0F, 270.0F, 0.0F);
                }
            } else if (!this.parseVector3(block, "rotate", polygon.rotate)) {
            }

            value = block.getValue("points");
            String[] ss = value.getValue().trim().split(" ");

            for (String xy : ss) {
                String[] ss2 = xy.split("x");
                float x = this.parseCoord(ss2[0]);
                float y = this.parseCoord(ss2[1]);
                polygon.points.add(x);
                polygon.points.add(y);
            }

            return polygon;
        }
    }

    HashMap<String, String> parseTileProperties(ScriptParser.Block block) {
        if (block.values.isEmpty()) {
            return null;
        } else {
            HashMap<String, String> result = new HashMap<>();

            for (ScriptParser.Value value : block.values) {
                String key = value.getKey().trim().intern();
                String val = value.getValue().trim().intern();
                if (!key.isEmpty()) {
                    result.put(key, val);
                }
            }

            return result;
        }
    }

    float parseCoord(String str) {
        return this.version == 1 ? PZMath.tryParseFloat(str, 0.0F) : PZMath.tryParseInt(str, 0) / 10000.0F;
    }

    float parseCoord(ScriptParser.Block block, String valueName, float defaultValue) {
        ScriptParser.Value value = block.getValue(valueName);
        if (value == null) {
            return defaultValue;
        } else {
            String str = value.getValue().trim();
            return this.parseCoord(str);
        }
    }

    Vector2f parseVector2(String str, Vector2f v) {
        String[] ss = str.trim().split("x");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        return v;
    }

    Vector3f parseVector3(String str, Vector3f v) {
        String[] ss = str.trim().split("x");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        v.z = this.parseCoord(ss[2]);
        return v;
    }

    boolean parseVector3(ScriptParser.Block block, String valueName, Vector3f v) {
        ScriptParser.Value value = block.getValue(valueName);
        if (value == null) {
            return false;
        } else {
            String str = value.getValue().trim();
            this.parseVector3(str, v);
            return true;
        }
    }

    int coordInt(float f) {
        return Math.round(f * 10000.0F);
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "tileGeometry";
        blockOG.setValue("VERSION", String.valueOf(2));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys = new ArrayList<>();

        for (TileGeometryFile.Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);

            for (TileGeometryFile.Tile tile : tileset.tiles) {
                if (!tile.geometry.isEmpty() || tile.properties != null && !tile.properties.isEmpty()) {
                    ScriptParser.Block blockT = new ScriptParser.Block();
                    blockT.type = "tile";
                    blockT.setValue("xy", String.format("%dx%d", tile.col, tile.row));

                    for (TileGeometryFile.Geometry geometry : tile.geometry) {
                        ScriptParser.Block blockG = geometry.toBlock(sb);
                        if (blockG != null) {
                            blockT.elements.add(blockG);
                            blockT.children.add(blockG);
                        }
                    }

                    if (tile.properties != null && !tile.properties.isEmpty()) {
                        ScriptParser.Block blockP = new ScriptParser.Block();
                        blockP.type = "properties";
                        keys.clear();
                        keys.addAll(tile.properties.keySet());
                        keys.sort(Comparator.naturalOrder());

                        for (int i = 0; i < keys.size(); i++) {
                            String key = keys.get(i);
                            blockP.setValue(key, tile.properties.get(key));
                        }

                        blockT.elements.add(blockP);
                        blockT.children.add(blockP);
                    }

                    blockT.comment = String.format("/* %s_%d */", tileset.name, tile.col + tile.row * 8);
                    blockTS.elements.add(blockT);
                    blockTS.children.add(blockT);
                }
            }

            blockOG.elements.add(blockTS);
            blockOG.children.add(blockTS);
        }

        sb.setLength(0);
        String eol = System.lineSeparator();
        blockOG.prettyPrint(0, sb, eol);
        this.write(fileName, sb.toString());
    }

    void write(String fileName, String totalFile) {
        File file = new File(fileName);

        try (
            FileWriter fw = new FileWriter(file);
            BufferedWriter br = new BufferedWriter(fw);
        ) {
            br.write(totalFile);
        } catch (Throwable var12) {
            ExceptionLogger.logException(var12);
        }
    }

    void Reset() {
    }

    public static final class Box extends TileGeometryFile.Geometry {
        public final Vector3f translate = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public final Vector3f min = new Vector3f();
        public final Vector3f max = new Vector3f();

        @Override
        public Object clone() {
            TileGeometryFile.Box copy = new TileGeometryFile.Box();
            copy.translate.set(this.translate);
            copy.rotate.set(this.rotate);
            copy.min.set(this.min);
            copy.max.set(this.max);
            return copy;
        }

        @Override
        public boolean isBox() {
            return true;
        }

        @Override
        public ScriptParser.Block toBlock(StringBuilder sb) {
            ScriptParser.Block block = new ScriptParser.Block();
            block.type = "box";
            block.setValue("translate", this.formatVector3(this.translate));
            block.setValue("rotate", this.formatVector3(this.rotate));
            block.setValue("min", this.formatVector3(this.min));
            block.setValue("max", this.formatVector3(this.max));
            return block;
        }

        @Override
        public void offset(int dx, int dy) {
            this.translate.add(dx, 0.0F, dy);
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnBoxAt(tileX, tileY, this.translate, this.rotate, this.min, this.max);
        }
    }

    public static final class Cylinder extends TileGeometryFile.Geometry {
        public final Vector3f translate = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public float radius1;
        public float radius2;
        public float height;

        @Override
        public Object clone() {
            TileGeometryFile.Cylinder copy = new TileGeometryFile.Cylinder();
            copy.translate.set(this.translate);
            copy.rotate.set(this.rotate);
            copy.radius1 = this.radius1;
            copy.radius2 = this.radius2;
            copy.height = this.height;
            return copy;
        }

        @Override
        public boolean isCylinder() {
            return true;
        }

        @Override
        public ScriptParser.Block toBlock(StringBuilder sb) {
            ScriptParser.Block block = new ScriptParser.Block();
            block.type = "cylinder";
            block.setValue("translate", this.formatVector3(this.translate));
            block.setValue("rotate", this.formatVector3(this.rotate));
            block.setValue("radius1", this.formatFloat(this.radius1));
            block.setValue("radius2", this.formatFloat(this.radius2));
            block.setValue("height", this.formatFloat(this.height));
            return block;
        }

        @Override
        public void offset(int dx, int dy) {
            this.translate.add(dx, 0.0F, dy);
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnCylinderAt(tileX, tileY, this.translate, this.rotate, this.radius1, this.height);
        }
    }

    public static class Geometry {
        @Override
        public Object clone() {
            return null;
        }

        public boolean isBox() {
            return false;
        }

        public TileGeometryFile.Box asBox() {
            return Type.tryCastTo(this, TileGeometryFile.Box.class);
        }

        public boolean isCylinder() {
            return false;
        }

        public TileGeometryFile.Cylinder asCylinder() {
            return Type.tryCastTo(this, TileGeometryFile.Cylinder.class);
        }

        public boolean isPolygon() {
            return false;
        }

        public TileGeometryFile.Polygon asPolygon() {
            return Type.tryCastTo(this, TileGeometryFile.Polygon.class);
        }

        public ScriptParser.Block toBlock(StringBuilder sb) {
            return null;
        }

        public void offset(int dx, int dy) {
        }

        int coordInt(float f) {
            return Math.round(f * 10000.0F);
        }

        String formatFloat(float f) {
            return String.format(Locale.US, "%d", this.coordInt(f));
        }

        String formatVector3(Vector3f v) {
            return this.formatVector3(v.x, v.y, v.z);
        }

        String formatVector3(float x, float y, float z) {
            return String.format(Locale.US, "%dx%dx%d", this.coordInt(x), this.coordInt(y), this.coordInt(z));
        }

        float getNormalizedDepthAt(float tileX, float tileY) {
            return -1.0F;
        }
    }

    public static enum Plane {
        XY,
        XZ,
        YZ;
    }

    public static final class Polygon extends TileGeometryFile.Geometry {
        public TileGeometryFile.Plane plane;
        public final Vector3f translate = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public final TFloatArrayList points = new TFloatArrayList();
        public final TFloatArrayList triangles = new TFloatArrayList();

        @Override
        public Object clone() {
            TileGeometryFile.Polygon copy = new TileGeometryFile.Polygon();
            copy.plane = this.plane;
            copy.translate.set(this.translate);
            copy.rotate.set(this.rotate);
            copy.points.addAll(this.points);
            copy.triangles.addAll(this.triangles);
            return copy;
        }

        @Override
        public ScriptParser.Block toBlock(StringBuilder sb) {
            ScriptParser.Block blockP = new ScriptParser.Block();
            blockP.type = "polygon";
            blockP.setValue("translate", this.formatVector3(this.translate));
            blockP.setValue("rotate", this.formatVector3(this.rotate));
            blockP.setValue("plane", this.plane.name());
            sb.setLength(0);

            for (int i = 0; i < this.points.size(); i += 2) {
                float x = this.points.get(i);
                float y = this.points.get(i + 1);
                sb.append(String.format(Locale.US, "%dx%d ", this.coordInt(x), this.coordInt(y)));
            }

            blockP.setValue("points", sb.toString());
            return blockP;
        }

        @Override
        public void offset(int dx, int dy) {
            this.translate.add(dx, 0.0F, dy);
        }

        @Override
        public boolean isPolygon() {
            return true;
        }

        boolean isClockwise() {
            float sum = 0.0F;

            for (int i = 0; i < this.points.size(); i += 2) {
                float p1x = this.points.get(i);
                float p1y = this.points.get(i + 1);
                float p2x = this.points.get((i + 2) % this.points.size());
                float p2y = this.points.get((i + 3) % this.points.size());
                sum += (p2x - p1x) * (p2y + p1y);
            }

            return sum > 0.0;
        }

        void triangulate() {
            this.triangles.clear();
            if (TileGeometryFile.clipper == null) {
                TileGeometryFile.clipper = new Clipper();
            }

            TileGeometryFile.clipper.clear();
            ByteBuffer bb = ByteBuffer.allocateDirect(8 * (this.points.size() / 2) * 3);
            if (this.isClockwise()) {
                for (int i = this.points.size() - 2; i >= 0; i -= 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            } else {
                for (int i = 0; i < this.points.size(); i += 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            }

            TileGeometryFile.clipper.addPath(this.points.size() / 2, bb, false);
            int numPolygons = TileGeometryFile.clipper.generatePolygons();
            if (numPolygons >= 1) {
                bb.clear();
                int numPoints = TileGeometryFile.clipper.triangulate(0, bb);
                Matrix4f m = new Matrix4f();
                m.translation(this.translate);
                m.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
                Vector3f v = new Vector3f();

                for (int i = 0; i < numPoints; i++) {
                    float x = bb.getFloat();
                    float y = bb.getFloat();
                    float z = 0.0F;
                    v.set(x, y, 0.0F);
                    m.transformPosition(v);
                    this.triangles.add(v.x);
                    this.triangles.add(v.y);
                    this.triangles.add(v.z);
                }
            }
        }

        void triangulate2() {
            this.triangles.clear();
            if (TileGeometryFile.clipper == null) {
                TileGeometryFile.clipper = new Clipper();
            }

            TileGeometryFile.clipper.clear();
            ByteBuffer bb = ByteBuffer.allocateDirect(8 * (this.points.size() / 2) * 3);
            if (this.isClockwise()) {
                for (int i = this.points.size() - 2; i >= 0; i -= 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            } else {
                for (int i = 0; i < this.points.size(); i += 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            }

            TileGeometryFile.clipper.addPath(this.points.size() / 2, bb, false);
            int numPolygons = TileGeometryFile.clipper.generatePolygons();
            if (numPolygons >= 1) {
                bb.clear();
                int numPoints = TileGeometryFile.clipper.triangulate(0, bb);
                new Vector3f();

                for (int i = 0; i < numPoints; i++) {
                    float x = bb.getFloat();
                    float y = bb.getFloat();
                    this.triangles.add(x);
                    this.triangles.add(y);
                }
            }
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            Vector3f normal = BaseVehicle.allocVector3f().set(0.0F, 0.0F, 1.0F);
            Matrix4f m = BaseVehicle.allocMatrix4f()
                .rotationXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            m.transformDirection(normal);
            BaseVehicle.releaseMatrix4f(m);
            float depth = TileGeometryUtils.getNormalizedDepthOnPlaneAt(tileX, tileY, this.translate, normal);
            BaseVehicle.releaseVector3f(normal);
            return depth;
        }

        public void rasterize(Rasterize.ICallback consumer) {
            this.triangulate2();
            this.calcMatrices(TileGeometryFile.Polygon.L_rasterizeDepth.m_projection, TileGeometryFile.Polygon.L_rasterizeDepth.m_modelView);
            Vector2f uiPos1 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_1;
            Vector2f uiPos2 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_2;
            Vector2f uiPos3 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_3;
            Vector2f point = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_4;
            Vector2f tileXY = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_5;
            this.calculateTextureTopLeft(0.0F, 0.0F, 0.0F, tileXY);
            float pixelSize = this.calculatePixelSize();

            for (int i = 0; i < this.triangles.size(); i += 6) {
                float x0 = this.triangles.get(i);
                float y0 = this.triangles.get(i + 1);
                float x1 = this.triangles.get(i + 2);
                float y1 = this.triangles.get(i + 3);
                float x2 = this.triangles.get(i + 4);
                float y2 = this.triangles.get(i + 5);
                this.planeToUI(point.set(x0, y0), uiPos1);
                this.planeToUI(point.set(x1, y1), uiPos2);
                this.planeToUI(point.set(x2, y2), uiPos3);
                this.uiToTile(tileXY, pixelSize, uiPos1, uiPos1);
                this.uiToTile(tileXY, pixelSize, uiPos2, uiPos2);
                this.uiToTile(tileXY, pixelSize, uiPos3, uiPos3);
                TileGeometryFile.Polygon.L_rasterizeDepth.rasterize
                    .scanTriangle(uiPos1.x, uiPos1.y, uiPos2.x, uiPos2.y, uiPos3.x, uiPos3.y, -1000, 1000, consumer);
            }

            this.triangles.clear();
        }

        float zoomMult() {
            int m_zoom = 3;
            return (float)Math.exp(0.6F) * 160.0F / Math.max(1.82F, 1.0F);
        }

        private void calcMatrices(Matrix4f projection, Matrix4f modelView) {
            float w = this.screenWidth();
            float scale = 1366.0F / w;
            float h = this.screenHeight() * scale;
            w = 1366.0F;
            w /= this.zoomMult();
            h /= this.zoomMult();
            projection.setOrtho(-w / 2.0F, w / 2.0F, -h / 2.0F, h / 2.0F, -10.0F, 10.0F);
            float this_m_view_x = 0.0F;
            float this_m_view_y = 0.0F;
            float m_view_x = 0.0F / this.zoomMult() * scale;
            float m_view_y = 0.0F / this.zoomMult() * scale;
            projection.translate(-m_view_x, m_view_y, 0.0F);
            modelView.identity();
            float rotateX = 30.0F;
            float rotateY = 315.0F;
            float rotateZ = 0.0F;
            modelView.rotateXYZ((float) (Math.PI / 6), (float) (Math.PI * 7.0 / 4.0), 0.0F);
        }

        float calculatePixelSize() {
            float sx = this.sceneToUIX(0.0F, 0.0F, 0.0F);
            float sy = this.sceneToUIY(0.0F, 0.0F, 0.0F);
            float sx2 = this.sceneToUIX(1.0F, 0.0F, 0.0F);
            float sy2 = this.sceneToUIY(1.0F, 0.0F, 0.0F);
            return (float)(Math.sqrt((sx2 - sx) * (sx2 - sx) + (sy2 - sy) * (sy2 - sy)) / Math.sqrt(5120.0));
        }

        Vector2f calculateTextureTopLeft(float sceneX, float sceneY, float sceneZ, Vector2f topLeft) {
            float sx = this.sceneToUIX(sceneX, sceneY, sceneZ);
            float sy = this.sceneToUIY(sceneX, sceneY, sceneZ);
            float pixelSize = this.calculatePixelSize();
            float tileX = sx - 64.0F * pixelSize;
            float tileY = sy - 224.0F * pixelSize;
            return topLeft.set(tileX, tileY);
        }

        Vector3f planeTo3D(Vector2f pointOnPlane, Vector3f result) {
            Matrix4f m = BaseVehicle.allocMatrix4f();
            m.translation(this.translate);
            m.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            m.transformPosition(pointOnPlane.x, pointOnPlane.y, 0.0F, result);
            BaseVehicle.releaseMatrix4f(m);
            return result;
        }

        Vector2f planeToUI(Vector2f pointOnPlane, Vector2f result) {
            Vector3f scenePos = this.planeTo3D(pointOnPlane, BaseVehicle.allocVector3f());
            result.set(this.sceneToUIX(scenePos), this.sceneToUIY(scenePos));
            BaseVehicle.releaseVector3f(scenePos);
            return result;
        }

        public float sceneToUIX(Vector3f scenePos) {
            return this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
        }

        public float sceneToUIY(Vector3f scenePos) {
            return this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
        }

        public float sceneToUIX(float sceneX, float sceneY, float sceneZ) {
            Matrix4f matrix4f = TileGeometryFile.Polygon.L_rasterizeDepth.matrix4f_1;
            matrix4f.set(TileGeometryFile.Polygon.L_rasterizeDepth.m_projection);
            matrix4f.mul(TileGeometryFile.Polygon.L_rasterizeDepth.m_modelView);
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[0] = 0;
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[1] = 0;
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[2] = this.screenWidth();
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[3] = this.screenHeight();
            matrix4f.project(sceneX, sceneY, sceneZ, TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport, TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1);
            return TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1.x();
        }

        public float sceneToUIY(float sceneX, float sceneY, float sceneZ) {
            Matrix4f matrix4f = TileGeometryFile.Polygon.L_rasterizeDepth.matrix4f_1;
            matrix4f.set(TileGeometryFile.Polygon.L_rasterizeDepth.m_projection);
            matrix4f.mul(TileGeometryFile.Polygon.L_rasterizeDepth.m_modelView);
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[0] = 0;
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[1] = 0;
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[2] = this.screenWidth();
            TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[3] = this.screenHeight();
            matrix4f.project(sceneX, sceneY, sceneZ, TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport, TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1);
            return this.screenHeight() - TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1.y();
        }

        Vector2f uiToTile(Vector2f tileXY, float pixelSize, Vector2f uiPos, Vector2f tilePos) {
            float x = (uiPos.x - tileXY.x) / pixelSize;
            float y = (uiPos.y - tileXY.y) / pixelSize;
            return tilePos.set(x, y);
        }

        int screenWidth() {
            return 1366;
        }

        int screenHeight() {
            return 768;
        }

        static final class L_rasterizeDepth {
            static final Rasterize rasterize = new Rasterize();
            static final Vector2f vector2f_1 = new Vector2f();
            static final Vector2f vector2f_2 = new Vector2f();
            static final Vector2f vector2f_3 = new Vector2f();
            static final Vector2f vector2f_4 = new Vector2f();
            static final Vector2f vector2f_5 = new Vector2f();
            static final Matrix4f m_projection = new Matrix4f();
            static final Matrix4f m_modelView = new Matrix4f();
            static final Matrix4f matrix4f_1 = new Matrix4f();
            static final int[] m_viewport = new int[4];
            static final Vector3f vector3f_1 = new Vector3f();
        }
    }

    static final class Tile {
        int col;
        int row;
        final ArrayList<TileGeometryFile.Geometry> geometry = new ArrayList<>();
        HashMap<String, String> properties;

        void setGeometry(ArrayList<TileGeometryFile.Geometry> geometry) {
            this.geometry.clear();
            this.geometry.addAll(geometry);
        }
    }

    static final class Tileset {
        String name;
        final ArrayList<TileGeometryFile.Tile> tiles = new ArrayList<>();

        TileGeometryFile.Tile getTile(int col, int row) {
            for (TileGeometryFile.Tile tile : this.tiles) {
                if (tile.col == col && tile.row == row) {
                    return tile;
                }
            }

            return null;
        }

        TileGeometryFile.Tile getOrCreateTile(int col, int row) {
            TileGeometryFile.Tile tile = this.getTile(col, row);
            if (tile != null) {
                return tile;
            } else {
                tile = new TileGeometryFile.Tile();
                tile.col = col;
                tile.row = row;
                int index = this.tiles.size();

                for (int i = 0; i < this.tiles.size(); i++) {
                    TileGeometryFile.Tile tile2 = this.tiles.get(i);
                    if (tile2.col + tile2.row * 8 > col + row * 8) {
                        index = i;
                        break;
                    }
                }

                this.tiles.add(index, tile);
                return tile;
            }
        }

        void initSpriteProperties() {
            for (int i = 0; i < this.tiles.size(); i++) {
                TileGeometryFile.Tile tile = this.tiles.get(i);
                if (tile.properties != null && !tile.properties.isEmpty()) {
                    int index = tile.col + tile.row * 8;
                    IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(this.name + "_" + index);
                    if (sprite != null) {
                        String value = tile.properties.get("ItemHeight");
                        if (value != null) {
                            sprite.getProperties().set("ItemHeight", value, false);
                        }

                        value = tile.properties.get("Surface");
                        if (value != null) {
                            sprite.getProperties().set("Surface", value, false);
                        }

                        value = tile.properties.get("CurtainOffset");
                        if (!StringUtils.isNullOrWhitespace(value)) {
                            String[] ss = value.split("\\s+");
                            if (ss.length == 3) {
                                float x = PZMath.tryParseFloat(ss[0], 0.0F);
                                float y = PZMath.tryParseFloat(ss[1], 0.0F);
                                float z = PZMath.tryParseFloat(ss[2], 0.0F);
                                sprite.setCurtainOffset(x, y, z);
                            }
                        }

                        value = tile.properties.get("OpaquePixelsOnly");
                        if (StringUtils.tryParseBoolean(value)) {
                            sprite.depthFlags |= 4;
                        }

                        value = tile.properties.get("Translucent");
                        if (StringUtils.tryParseBoolean(value)) {
                            sprite.depthFlags |= 2;
                        }

                        value = tile.properties.get("UseObjectDepthTexture");
                        if (StringUtils.tryParseBoolean(value)) {
                            sprite.depthFlags |= 1;
                        }
                    }
                }
            }
        }
    }
}
