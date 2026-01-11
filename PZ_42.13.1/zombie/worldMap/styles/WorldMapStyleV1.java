// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;

@UsedFromLua
public class WorldMapStyleV1 {
    public UIWorldMap ui;
    public UIWorldMapV1 api;
    public WorldMapStyle style;
    public final ArrayList<WorldMapStyleV1.WorldMapStyleLayerV1> layers = new ArrayList<>();
    protected IWorldMapStyleListener listener;

    public WorldMapStyleV1(UIWorldMap ui) {
        Objects.requireNonNull(ui);
        this.ui = ui;
        this.api = ui.getAPIv1();
        this.style = this.api.getStyle();
        this._initListener();

        for (int i = 0; i < this.style.getLayerCount(); i++) {
            WorldMapStyleLayer layer = this.style.getLayerByIndex(i);
            this.listener.onAdd(layer);
        }
    }

    protected void _initListener() {
        this.listener = new WorldMapStyleV1.Listener(this);
        this.style.addListener(this.listener);
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 newLineLayer(String id) throws IllegalArgumentException {
        WorldMapLineStyleLayer layer = new WorldMapLineStyleLayer(id);
        this.style.addLayer(layer);
        return this.layers.get(this.style.getLayerCount() - 1);
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 newPolygonLayer(String id) throws IllegalArgumentException {
        WorldMapPolygonStyleLayer layer = new WorldMapPolygonStyleLayer(id);
        this.style.addLayer(layer);
        return this.layers.get(this.style.getLayerCount() - 1);
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 newTextureLayer(String id) throws IllegalArgumentException {
        WorldMapTextureStyleLayer layer = new WorldMapTextureStyleLayer(id);
        this.style.addLayer(layer);
        return this.layers.get(this.style.getLayerCount() - 1);
    }

    public int getLayerCount() {
        return this.layers.size();
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 getLayerByIndex(int index) {
        return this.layers.get(index);
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 getLayerByName(String id) {
        int index = this.indexOfLayer(id);
        return index == -1 ? null : this.layers.get(index);
    }

    public int indexOfLayer(String id) {
        for (int i = 0; i < this.layers.size(); i++) {
            WorldMapStyleV1.WorldMapStyleLayerV1 layer = this.layers.get(i);
            if (layer.layer.id.equals(id)) {
                return i;
            }
        }

        return -1;
    }

    private int indexOfLayer(WorldMapStyleLayer layer) {
        for (int i = 0; i < this.layers.size(); i++) {
            WorldMapStyleV1.WorldMapStyleLayerV1 layerV1 = this.layers.get(i);
            if (layerV1.layer == layer) {
                return i;
            }
        }

        return -1;
    }

    public void moveLayer(int indexFrom, int indexTo) {
        this.style.moveLayer(indexFrom, indexTo);
    }

    public void removeLayerById(String id) {
        int index = this.indexOfLayer(id);
        if (index != -1) {
            this.removeLayerByIndex(index);
        }
    }

    public void removeLayerByIndex(int index) {
        this.style.removeAt(index);
    }

    public void clear() {
        this.style.clear();
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapStyleV1.class);
        exposer.setExposed(WorldMapStyleV1.WorldMapStyleLayerV1.class);
        exposer.setExposed(WorldMapStyleV1.WorldMapLineStyleLayerV1.class);
        exposer.setExposed(WorldMapStyleV1.WorldMapPolygonStyleLayerV1.class);
        exposer.setExposed(WorldMapStyleV1.WorldMapTextureStyleLayerV1.class);
    }

    protected static final class Listener extends IWorldMapStyleListener {
        final WorldMapStyleV1 api;

        Listener(WorldMapStyleV1 api) {
            this.api = api;
        }

        @Override
        void onAdd(WorldMapStyleLayer layer) {
            if (layer instanceof WorldMapLineStyleLayer _layer) {
                WorldMapStyleV1.WorldMapLineStyleLayerV1 layerV1 = new WorldMapStyleV1.WorldMapLineStyleLayerV1(this.api, _layer);
                this.api.layers.add(this.api.style.indexOf(_layer), layerV1);
            } else if (layer instanceof WorldMapPolygonStyleLayer _layer) {
                WorldMapStyleV1.WorldMapPolygonStyleLayerV1 layerV1 = new WorldMapStyleV1.WorldMapPolygonStyleLayerV1(this.api, _layer);
                this.api.layers.add(this.api.style.indexOf(_layer), layerV1);
            } else if (layer instanceof WorldMapTextureStyleLayer _layer) {
                WorldMapStyleV1.WorldMapTextureStyleLayerV1 layerV1 = new WorldMapStyleV1.WorldMapTextureStyleLayerV1(this.api, _layer);
                this.api.layers.add(this.api.style.indexOf(_layer), layerV1);
            } else {
                WorldMapStyleV1.WorldMapStyleLayerV1 layerV1 = new WorldMapStyleV1.WorldMapStyleLayerV1(this.api, layer);
                this.api.layers.add(this.api.style.indexOf(layer), layerV1);
            }
        }

        @Override
        void onBeforeRemove(WorldMapStyleLayer layer) {
            int index = this.api.indexOfLayer(layer);
            if (index != -1) {
                WorldMapStyleV1.WorldMapStyleLayerV1 layerV1 = this.api.layers.remove(index);
            }
        }

        @Override
        void onAfterRemove(WorldMapStyleLayer layer) {
        }

        @Override
        void onMoveLayer(int indexFrom, int indexTo) {
            WorldMapStyleV1.WorldMapStyleLayerV1 layerV1 = this.api.layers.remove(indexFrom);
            this.api.layers.add(indexTo, layerV1);
        }

        @Override
        void onBeforeClear() {
        }

        @Override
        void onAfterClear() {
            this.api.layers.clear();
        }
    }

    @UsedFromLua
    public static class WorldMapLineStyleLayerV1 extends WorldMapStyleV1.WorldMapStyleLayerV1 {
        WorldMapLineStyleLayer lineStyle = (WorldMapLineStyleLayer)this.layer;

        WorldMapLineStyleLayerV1(WorldMapStyleV1 owner, WorldMapLineStyleLayer layer) {
            super(owner, layer);
        }

        public void setFilter(String key, String value) {
            this.lineStyle.filterKey = key;
            this.lineStyle.filterValue = value;
            this.lineStyle.filter = (feature, args) -> feature.hasLineString() && value.equals(feature.properties.get(key));
        }

        public void addFill(float zoom, int r, int g, int b, int a) {
            this.lineStyle.fill.add(new WorldMapStyleLayer.ColorStop(zoom, r, g, b, a));
        }

        public void addLineWidth(float zoom, float width) {
            this.lineStyle.lineWidth.add(new WorldMapStyleLayer.FloatStop(zoom, width));
        }
    }

    @UsedFromLua
    public static class WorldMapPolygonStyleLayerV1 extends WorldMapStyleV1.WorldMapStyleLayerV1 {
        WorldMapPolygonStyleLayer polygonStyle = (WorldMapPolygonStyleLayer)this.layer;

        WorldMapPolygonStyleLayerV1(WorldMapStyleV1 owner, WorldMapPolygonStyleLayer layer) {
            super(owner, layer);
        }

        public void setFilter(String key, String value) {
            this.polygonStyle.filterKey = key;
            this.polygonStyle.filterValue = value;
            if ("*".equals(value)) {
                this.polygonStyle.filter = (feature, args) -> feature.hasPolygon() && feature.properties.containsKey(key);
            } else {
                this.polygonStyle.filter = (feature, args) -> feature.hasPolygon() && value.equals(feature.properties.get(key));
            }
        }

        public String getFilterKey() {
            return this.polygonStyle.filterKey;
        }

        public String getFilterValue() {
            return this.polygonStyle.filterValue;
        }

        public void addFill(float zoom, int r, int g, int b, int a) {
            this.polygonStyle.fill.add(new WorldMapStyleLayer.ColorStop(zoom, r, g, b, a));
        }

        public void addScale(float zoom, float scale) {
            this.polygonStyle.scale.add(new WorldMapStyleLayer.FloatStop(zoom, scale));
        }

        public void addTexture(float zoom, String texturePath) {
            this.polygonStyle.texture.add(new WorldMapStyleLayer.TextureStop(zoom, texturePath));
        }

        public void addTexture(float zoom, String texturePath, String scalingStr) {
            this.polygonStyle.texture.add(new WorldMapStyleLayer.TextureStop(zoom, texturePath, scalingStr));
        }

        public void removeFill(int index) {
            this.polygonStyle.fill.remove(index);
        }

        public void removeScale(int index) {
            this.polygonStyle.scale.remove(index);
        }

        public void removeTexture(int index) {
            this.polygonStyle.texture.remove(index);
        }

        public void moveFill(int indexFrom, int indexTo) {
            WorldMapStyleLayer.ColorStop stop = this.polygonStyle.fill.remove(indexFrom);
            this.polygonStyle.fill.add(indexTo, stop);
        }

        public void moveScale(int indexFrom, int indexTo) {
            WorldMapStyleLayer.FloatStop stop = this.polygonStyle.scale.remove(indexFrom);
            this.polygonStyle.scale.add(indexTo, stop);
        }

        public void moveTexture(int indexFrom, int indexTo) {
            WorldMapStyleLayer.TextureStop stop = this.polygonStyle.texture.remove(indexFrom);
            this.polygonStyle.texture.add(indexTo, stop);
        }

        public int getFillStops() {
            return this.polygonStyle.fill.size();
        }

        public void setFillRGBA(int index, int r, int g, int b, int a) {
            this.polygonStyle.fill.get(index).r = r;
            this.polygonStyle.fill.get(index).g = g;
            this.polygonStyle.fill.get(index).b = b;
            this.polygonStyle.fill.get(index).a = a;
        }

        public void setFillZoom(int index, float zoom) {
            this.polygonStyle.fill.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public float getFillZoom(int index) {
            return this.polygonStyle.fill.get(index).zoom;
        }

        public int getFillRed(int index) {
            return this.polygonStyle.fill.get(index).r;
        }

        public int getFillGreen(int index) {
            return this.polygonStyle.fill.get(index).g;
        }

        public int getFillBlue(int index) {
            return this.polygonStyle.fill.get(index).b;
        }

        public int getFillAlpha(int index) {
            return this.polygonStyle.fill.get(index).a;
        }

        public int getScaleStops() {
            return this.polygonStyle.scale.size();
        }

        public void setScaleZoom(int index, float zoom) {
            this.polygonStyle.scale.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public void setScaleValue(int index, int scale) {
            this.polygonStyle.scale.get(index).f = PZMath.clamp((float)scale, 0.0F, Float.MAX_VALUE);
        }

        public int getTextureStops() {
            return this.polygonStyle.texture.size();
        }

        public void setTextureZoom(int index, float zoom) {
            this.polygonStyle.texture.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public float getTextureZoom(int index) {
            return this.polygonStyle.texture.get(index).zoom;
        }

        public void setTexturePath(int index, String texturePath) {
            this.polygonStyle.texture.get(index).texturePath = texturePath;
            this.polygonStyle.texture.get(index).texture = Texture.getTexture(texturePath);
        }

        public String getTexturePath(int index) {
            return this.polygonStyle.texture.get(index).texturePath;
        }

        public Texture getTexture(int index) {
            return this.polygonStyle.texture.get(index).texture;
        }

        public void setTextureScaling(int index, String scalingStr) {
            this.polygonStyle.texture.get(index).scaling = WorldMapStyleLayer.TextureScaling.valueOf(scalingStr);
        }

        public String getTextureScaling(int index) {
            return this.polygonStyle.texture.get(index).scaling.name();
        }
    }

    @UsedFromLua
    public static class WorldMapStyleLayerV1 {
        WorldMapStyleV1 owner;
        WorldMapStyleLayer layer;

        WorldMapStyleLayerV1(WorldMapStyleV1 owner, WorldMapStyleLayer layer) {
            this.owner = owner;
            this.layer = layer;
        }

        public String getTypeString() {
            return this.layer.getTypeString();
        }

        public void setId(String id) {
            this.owner.style.setLayerID(this.layer, id);
        }

        public String getId() {
            return this.layer.id;
        }

        public String getID() {
            return this.layer.id;
        }

        public void setMinZoom(float minZoom) {
            this.layer.minZoom = minZoom;
        }

        public float getMinZoom() {
            return this.layer.minZoom;
        }
    }

    @UsedFromLua
    public static class WorldMapTextureStyleLayerV1 extends WorldMapStyleV1.WorldMapStyleLayerV1 {
        WorldMapTextureStyleLayer textureStyle = (WorldMapTextureStyleLayer)this.layer;

        WorldMapTextureStyleLayerV1(WorldMapStyleV1 owner, WorldMapTextureStyleLayer layer) {
            super(owner, layer);
        }

        public void addFill(float zoom, int r, int g, int b, int a) {
            this.textureStyle.fill.add(new WorldMapStyleLayer.ColorStop(zoom, r, g, b, a));
        }

        public void addTexture(float zoom, String texturePath) {
            this.textureStyle.texture.add(new WorldMapStyleLayer.TextureStop(zoom, texturePath));
        }

        public void removeFill(int index) {
            this.textureStyle.fill.remove(index);
        }

        public void removeAllFill() {
            this.textureStyle.fill.clear();
        }

        public void removeTexture(int index) {
            this.textureStyle.texture.remove(index);
        }

        public void removeAllTexture() {
            this.textureStyle.texture.clear();
        }

        public void moveFill(int indexFrom, int indexTo) {
            WorldMapStyleLayer.ColorStop stop = this.textureStyle.fill.remove(indexFrom);
            this.textureStyle.fill.add(indexTo, stop);
        }

        public void moveTexture(int indexFrom, int indexTo) {
            WorldMapStyleLayer.TextureStop stop = this.textureStyle.texture.remove(indexFrom);
            this.textureStyle.texture.add(indexTo, stop);
        }

        public void setBoundsInSquares(int minX, int minY, int maxX, int maxY) {
            this.textureStyle.worldX1 = minX;
            this.textureStyle.worldY1 = minY;
            this.textureStyle.worldX2 = maxX;
            this.textureStyle.worldY2 = maxY;
        }

        public int getMinXInSquares() {
            return this.textureStyle.worldX1;
        }

        public int getMinYInSquares() {
            return this.textureStyle.worldY1;
        }

        public int getMaxXInSquares() {
            return this.textureStyle.worldX2;
        }

        public int getMaxYInSquares() {
            return this.textureStyle.worldY2;
        }

        public int getWidthInSquares() {
            return this.textureStyle.worldX2 - this.textureStyle.worldX1;
        }

        public int getHeightInSquares() {
            return this.textureStyle.worldY2 - this.textureStyle.worldY1;
        }

        public void setTile(boolean tile) {
            this.textureStyle.tile = tile;
        }

        public boolean isTile() {
            return this.textureStyle.tile;
        }

        public void setUseWorldBounds(boolean useWorldBounds) {
            this.textureStyle.useWorldBounds = useWorldBounds;
        }

        public boolean isUseWorldBounds() {
            return this.textureStyle.useWorldBounds;
        }

        public int getFillStops() {
            return this.textureStyle.fill.size();
        }

        public void setFillRGBA(int index, int r, int g, int b, int a) {
            this.textureStyle.fill.get(index).r = r;
            this.textureStyle.fill.get(index).g = g;
            this.textureStyle.fill.get(index).b = b;
            this.textureStyle.fill.get(index).a = a;
        }

        public void setFillZoom(int index, float zoom) {
            this.textureStyle.fill.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public float getFillZoom(int index) {
            return this.textureStyle.fill.get(index).zoom;
        }

        public int getFillRed(int index) {
            return this.textureStyle.fill.get(index).r;
        }

        public int getFillGreen(int index) {
            return this.textureStyle.fill.get(index).g;
        }

        public int getFillBlue(int index) {
            return this.textureStyle.fill.get(index).b;
        }

        public int getFillAlpha(int index) {
            return this.textureStyle.fill.get(index).a;
        }

        public int getTextureStops() {
            return this.textureStyle.texture.size();
        }

        public void setTextureZoom(int index, float zoom) {
            this.textureStyle.texture.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public float getTextureZoom(int index) {
            return this.textureStyle.texture.get(index).zoom;
        }

        public void setTexturePath(int index, String texturePath) {
            this.textureStyle.texture.get(index).texturePath = texturePath;
            this.textureStyle.texture.get(index).texture = Texture.getTexture(texturePath);
        }

        public String getTexturePath(int index) {
            return this.textureStyle.texture.get(index).texturePath;
        }

        public Texture getTexture(int index) {
            return this.textureStyle.texture.get(index).texture;
        }
    }
}
