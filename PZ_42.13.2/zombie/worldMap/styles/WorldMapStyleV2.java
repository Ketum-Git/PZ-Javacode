// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.math.PZMath;
import zombie.ui.UIFont;
import zombie.worldMap.UIWorldMap;

@UsedFromLua
public class WorldMapStyleV2 extends WorldMapStyleV1 {
    public WorldMapStyleV2(UIWorldMap ui) {
        super(ui);
    }

    @Override
    protected void _initListener() {
        this.listener = new WorldMapStyleV2.Listener(this);
        this.style.addListener(this.listener);
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 newPyramidLayer(String id) throws IllegalArgumentException {
        WorldMapPyramidStyleLayer layer = new WorldMapPyramidStyleLayer(id);
        this.style.addLayer(layer);
        return this.layers.get(this.style.getLayerCount() - 1);
    }

    public WorldMapStyleV1.WorldMapStyleLayerV1 newTextLayer(String id) throws IllegalArgumentException {
        WorldMapTextStyleLayer layer = new WorldMapTextStyleLayer(id);
        this.style.addLayer(layer);
        return this.layers.get(this.style.getLayerCount() - 1);
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

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapStyleV2.class);
        exposer.setExposed(WorldMapStyleV2.WorldMapPyramidStyleLayerV1.class);
        exposer.setExposed(WorldMapStyleV2.WorldMapTextStyleLayerV1.class);
    }

    private static final class Listener extends IWorldMapStyleListener {
        final WorldMapStyleV2 api;

        Listener(WorldMapStyleV2 api) {
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
            } else if (layer instanceof WorldMapTextStyleLayer _layer) {
                WorldMapStyleV2.WorldMapTextStyleLayerV1 layerV1 = new WorldMapStyleV2.WorldMapTextStyleLayerV1(this.api, _layer);
                this.api.layers.add(this.api.style.indexOf(_layer), layerV1);
            } else if (layer instanceof WorldMapTextureStyleLayer _layer) {
                WorldMapStyleV1.WorldMapTextureStyleLayerV1 layerV1 = new WorldMapStyleV1.WorldMapTextureStyleLayerV1(this.api, _layer);
                this.api.layers.add(this.api.style.indexOf(_layer), layerV1);
            } else {
                if (layer instanceof WorldMapPyramidStyleLayer _layer) {
                    WorldMapStyleV2.WorldMapPyramidStyleLayerV1 layerV1 = new WorldMapStyleV2.WorldMapPyramidStyleLayerV1(this.api, _layer);
                    this.api.layers.add(this.api.style.indexOf(_layer), layerV1);
                }
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
    public static class WorldMapPyramidStyleLayerV1 extends WorldMapStyleV1.WorldMapStyleLayerV1 {
        WorldMapPyramidStyleLayer pyramidStyle = (WorldMapPyramidStyleLayer)this.layer;

        WorldMapPyramidStyleLayerV1(WorldMapStyleV2 owner, WorldMapPyramidStyleLayer layer) {
            super(owner, layer);
        }

        public String getPyramidFileName() {
            return this.pyramidStyle.fileName;
        }

        public void setPyramidFileName(String fileName) {
            this.pyramidStyle.fileName = fileName;
        }

        public void addFill(float zoom, int r, int g, int b, int a) {
            this.pyramidStyle.fill.add(new WorldMapStyleLayer.ColorStop(zoom, r, g, b, a));
        }

        public void removeFill(int index) {
            this.pyramidStyle.fill.remove(index);
        }

        public void removeAllFill() {
            this.pyramidStyle.fill.clear();
        }

        public void moveFill(int indexFrom, int indexTo) {
            WorldMapStyleLayer.ColorStop stop = this.pyramidStyle.fill.remove(indexFrom);
            this.pyramidStyle.fill.add(indexTo, stop);
        }

        public int getFillStops() {
            return this.pyramidStyle.fill.size();
        }

        public void setFillRGBA(int index, int r, int g, int b, int a) {
            this.pyramidStyle.fill.get(index).r = r;
            this.pyramidStyle.fill.get(index).g = g;
            this.pyramidStyle.fill.get(index).b = b;
            this.pyramidStyle.fill.get(index).a = a;
        }

        public void setFillZoom(int index, float zoom) {
            this.pyramidStyle.fill.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public float getFillZoom(int index) {
            return this.pyramidStyle.fill.get(index).zoom;
        }

        public int getFillRed(int index) {
            return this.pyramidStyle.fill.get(index).r;
        }

        public int getFillGreen(int index) {
            return this.pyramidStyle.fill.get(index).g;
        }

        public int getFillBlue(int index) {
            return this.pyramidStyle.fill.get(index).b;
        }

        public int getFillAlpha(int index) {
            return this.pyramidStyle.fill.get(index).a;
        }
    }

    @UsedFromLua
    public static class WorldMapTextStyleLayerV1 extends WorldMapStyleV1.WorldMapStyleLayerV1 {
        WorldMapTextStyleLayer textLayer = (WorldMapTextStyleLayer)this.layer;

        WorldMapTextStyleLayerV1(WorldMapStyleV2 owner, WorldMapTextStyleLayer layer) {
            super(owner, layer);
        }

        public UIFont getFont() {
            return this.textLayer.getFont();
        }

        public void setFont(UIFont font) {
            this.textLayer.font = font;
            if (this.owner.ui.getSymbolsDirect() != null) {
                this.owner.ui.getSymbolsDirect().invalidateLayout();
            }
        }

        public int getLineHeight() {
            return this.textLayer.lineHeight;
        }

        public void setLineHeight(int lineHeight) {
            this.textLayer.lineHeight = lineHeight;
        }

        public void addFill(float zoom, int r, int g, int b, int a) {
            this.textLayer.fill.add(new WorldMapStyleLayer.ColorStop(zoom, r, g, b, a));
        }

        public void removeFill(int index) {
            this.textLayer.fill.remove(index);
        }

        public void removeAllFill() {
            this.textLayer.fill.clear();
        }

        public void moveFill(int indexFrom, int indexTo) {
            WorldMapStyleLayer.ColorStop stop = this.textLayer.fill.remove(indexFrom);
            this.textLayer.fill.add(indexTo, stop);
        }

        public int getFillStops() {
            return this.textLayer.fill.size();
        }

        public void setFillRGBA(int index, int r, int g, int b, int a) {
            this.textLayer.fill.get(index).r = r;
            this.textLayer.fill.get(index).g = g;
            this.textLayer.fill.get(index).b = b;
            this.textLayer.fill.get(index).a = a;
        }

        public void setFillZoom(int index, float zoom) {
            this.textLayer.fill.get(index).zoom = PZMath.clamp(zoom, 0.0F, 24.0F);
        }

        public float getFillZoom(int index) {
            return this.textLayer.fill.get(index).zoom;
        }

        public int getFillRed(int index) {
            return this.textLayer.fill.get(index).r;
        }

        public int getFillGreen(int index) {
            return this.textLayer.fill.get(index).g;
        }

        public int getFillBlue(int index) {
            return this.textLayer.fill.get(index).b;
        }

        public int getFillAlpha(int index) {
            return this.textLayer.fill.get(index).a;
        }
    }
}
