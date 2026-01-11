// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.util.ArrayList;
import java.util.HashMap;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.util.StringUtils;

public final class TileGeometry {
    private TileGeometryFile file;
    private final String mediaAbsPath;

    public TileGeometry(String mediaAbsPath) {
        this.mediaAbsPath = mediaAbsPath;
    }

    public void init() {
        this.file = new TileGeometryFile();
        this.file.read(this.mediaAbsPath + "/tileGeometry.txt");
    }

    public void write() {
        this.file.write(this.mediaAbsPath + "/tileGeometry.txt");
    }

    public void setGeometry(String tilesetName, int col, int row, ArrayList<TileGeometryFile.Geometry> geometry) {
        TileGeometryFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            tileset = new TileGeometryFile.Tileset();
            tileset.name = tilesetName;
            this.file.tilesets.add(tileset);
        }

        TileGeometryFile.Tile tile = tileset.getOrCreateTile(col, row);
        tile.setGeometry(geometry);
    }

    public void copyGeometry(String tilesetName, int col, int row, ArrayList<TileGeometryFile.Geometry> geometries) {
        ArrayList<TileGeometryFile.Geometry> geometriesCopy = new ArrayList<>();

        for (int i = 0; i < geometries.size(); i++) {
            TileGeometryFile.Geometry geometryCopy = (TileGeometryFile.Geometry)geometries.get(i).clone();
            geometriesCopy.add(geometryCopy);
        }

        this.setGeometry(tilesetName, col, row, geometriesCopy);
    }

    public ArrayList<TileGeometryFile.Geometry> getGeometry(String tilesetName, int col, int row) {
        TileGeometryFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            return null;
        } else {
            TileGeometryFile.Tile tile = tileset.getTile(col, row);
            return tile == null ? null : tile.geometry;
        }
    }

    public void setProperty(String tilesetName, int col, int row, String key, String value) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            TileGeometryFile.Tileset tileset = this.findTileset(tilesetName);
            if (tileset == null) {
                if (value == null) {
                    return;
                }

                tileset = new TileGeometryFile.Tileset();
                tileset.name = tilesetName;
                this.file.tilesets.add(tileset);
            }

            TileGeometryFile.Tile tile = tileset.getOrCreateTile(col, row);
            if (tile != null) {
                if (tile.properties == null) {
                    if (value == null) {
                        return;
                    }

                    tile.properties = new HashMap<>();
                }

                if (value == null) {
                    tile.properties.remove(key.trim());
                } else {
                    tile.properties.put(key.trim(), value.trim());
                }
            }
        }
    }

    public String getProperty(String tilesetName, int col, int row, String key) {
        if (StringUtils.isNullOrWhitespace(key)) {
            return null;
        } else {
            TileGeometryFile.Tileset tileset = this.findTileset(tilesetName);
            if (tileset == null) {
                return null;
            } else {
                TileGeometryFile.Tile tile = tileset.getTile(col, row);
                if (tile == null) {
                    return null;
                } else {
                    return tile.properties == null ? null : tile.properties.get(key.trim());
                }
            }
        }
    }

    TileGeometryFile.Tileset findTileset(String tilesetName) {
        for (TileGeometryFile.Tileset tileset : this.file.tilesets) {
            if (tilesetName.equals(tileset.name)) {
                return tileset;
            }
        }

        return null;
    }

    TileGeometryFile.Tile getTile(String tilesetName, int col, int row) {
        TileGeometryFile.Tileset tileset = this.findTileset(tilesetName);
        return tileset == null ? null : tileset.getTile(col, row);
    }

    TileGeometryFile.Tile getOrCreateTile(String tilesetName, int col, int row) {
        TileGeometryFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            tileset = new TileGeometryFile.Tileset();
            tileset.name = tilesetName;
            this.file.tilesets.add(tileset);
        }

        return tileset.getOrCreateTile(col, row);
    }

    void initSpriteProperties() {
        for (TileGeometryFile.Tileset tileset : this.file.tilesets) {
            tileset.initSpriteProperties();
        }
    }

    void renderGeometry(IsoObject object) {
        IsoSprite sprite = object.getSprite();
        if (sprite != null) {
            Texture texture = sprite.getTextureForCurrentFrame(object.getDir());
            if (texture != null && texture.getName() != null) {
                TileGeometryFile.Tileset tileset = this.findTileset(texture.getName().substring(0, texture.getName().lastIndexOf(95)));
                if (tileset != null) {
                    TileGeometryFile.Tile tile = tileset.getTile(sprite.tileSheetIndex % 8, sprite.tileSheetIndex / 8);
                    if (tile != null) {
                        SpriteRenderer.instance.drawGeneric(TileGeometry.Drawer.s_pool.alloc().init(tile, object.square.x, object.square.y, object.square.z));
                    }
                }
            }
        }
    }

    public void Reset() {
        this.file.Reset();
        this.file = null;
    }

    static final class Drawer extends TextureDraw.GenericDrawer {
        static final ObjectPool<TileGeometry.Drawer> s_pool = new ObjectPool<>(TileGeometry.Drawer::new);
        TileGeometryFile.Tile tile;
        float x;
        float y;
        float z;

        TileGeometry.Drawer init(TileGeometryFile.Tile tile, float x, float y, float z) {
            this.tile = tile;

            for (TileGeometryFile.Geometry geometry : this.tile.geometry) {
                TileGeometryFile.Polygon polygon = geometry.asPolygon();
                if (polygon != null && polygon.triangles.isEmpty()) {
                    polygon.triangulate();
                }
            }

            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public void render() {
            Core.getInstance().DoPushIsoStuff(this.x + 0.5F, this.y + 0.5F, this.z, 0.0F, false);
            GL11.glDisable(3553);
            GL11.glPolygonMode(1032, 6914);
            GL11.glMatrixMode(5888);
            GL11.glDepthMask(true);
            GL11.glEnable(2929);
            boolean drawGeometry = false;
            GL11.glColorMask(false, false, false, false);
            GL11.glScalef(-0.6666667F, 0.6666667F, 0.6666667F);

            for (TileGeometryFile.Geometry geometry : this.tile.geometry) {
                GL11.glBegin(4);
                TileGeometryFile.Polygon polygon = geometry.asPolygon();
                if (polygon != null) {
                    for (int i = 0; i < polygon.triangles.size(); i += 9) {
                        GL11.glVertex3f(polygon.triangles.getQuick(i), polygon.triangles.getQuick(i + 1), polygon.triangles.getQuick(i + 2));
                        GL11.glVertex3f(polygon.triangles.getQuick(i + 3), polygon.triangles.getQuick(i + 4), polygon.triangles.getQuick(i + 5));
                        GL11.glVertex3f(polygon.triangles.getQuick(i + 6), polygon.triangles.getQuick(i + 7), polygon.triangles.getQuick(i + 8));
                    }
                }

                GL11.glEnd();
            }

            GL11.glEnable(3553);
            GL11.glPolygonMode(1032, 6914);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(false);
            GL11.glDisable(2929);
            Core.getInstance().DoPopIsoStuff();
        }

        @Override
        public void postRender() {
            s_pool.release(this);
        }
    }
}
