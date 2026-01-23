// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.popman.ObjectPool;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapRenderer;

public abstract class WorldMapStyleLayer {
    public String id;
    public float minZoom;
    public WorldMapStyleLayer.IWorldMapStyleFilter filter;
    public String filterKey;
    public String filterValue;

    public WorldMapStyleLayer(String id) {
        this.id = id;
    }

    public abstract String getTypeString();

    public boolean ignoreFeatures() {
        return false;
    }

    static <S extends WorldMapStyleLayer.Stop> int findStop(float zoom, ArrayList<S> stops) {
        if (stops.isEmpty()) {
            return -2;
        } else if (zoom <= stops.get(0).zoom) {
            return -1;
        } else {
            for (int i = 0; i < stops.size() - 1; i++) {
                if (zoom <= stops.get(i + 1).zoom) {
                    return i;
                }
            }

            return stops.size() - 1;
        }
    }

    protected WorldMapStyleLayer.RGBAf evalColor(WorldMapStyleLayer.RenderArgs args, ArrayList<WorldMapStyleLayer.ColorStop> stops) {
        return this.evalColor(args.drawer.zoomF, stops);
    }

    public WorldMapStyleLayer.RGBAf evalColor(float zoom, ArrayList<WorldMapStyleLayer.ColorStop> stops) {
        if (stops.isEmpty()) {
            return WorldMapStyleLayer.RGBAf.s_pool.alloc().init(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            int stopIndex = findStop(zoom, stops);
            int stopIndex1 = stopIndex == -1 ? 0 : stopIndex;
            int stopIndex2 = PZMath.min(stopIndex + 1, stops.size() - 1);
            WorldMapStyleLayer.ColorStop stop1 = stops.get(stopIndex1);
            WorldMapStyleLayer.ColorStop stop2 = stops.get(stopIndex2);
            float zoomAlpha = stopIndex1 == stopIndex2 ? 1.0F : (PZMath.clamp(zoom, stop1.zoom, stop2.zoom) - stop1.zoom) / (stop2.zoom - stop1.zoom);
            float r = PZMath.lerp(stop1.r, stop2.r, zoomAlpha) / 255.0F;
            float g = PZMath.lerp(stop1.g, stop2.g, zoomAlpha) / 255.0F;
            float b = PZMath.lerp(stop1.b, stop2.b, zoomAlpha) / 255.0F;
            float a = PZMath.lerp(stop1.a, stop2.a, zoomAlpha) / 255.0F;
            return WorldMapStyleLayer.RGBAf.s_pool.alloc().init(r, g, b, a);
        }
    }

    protected float evalFloat(WorldMapStyleLayer.RenderArgs args, ArrayList<WorldMapStyleLayer.FloatStop> stops) {
        return this.evalFloat(args, stops, 1.0F);
    }

    protected float evalFloat(WorldMapStyleLayer.RenderArgs args, ArrayList<WorldMapStyleLayer.FloatStop> stops, float emptyValue) {
        float zoomF = args.drawer.zoomF;
        return this.evalFloat(zoomF, stops, emptyValue);
    }

    protected float evalFloat(float zoomF, ArrayList<WorldMapStyleLayer.FloatStop> stops, float emptyValue) {
        if (stops.isEmpty()) {
            return emptyValue;
        } else {
            int stopIndex = findStop(zoomF, stops);
            int stopIndex1 = stopIndex == -1 ? 0 : stopIndex;
            int stopIndex2 = PZMath.min(stopIndex + 1, stops.size() - 1);
            WorldMapStyleLayer.FloatStop stop1 = stops.get(stopIndex1);
            WorldMapStyleLayer.FloatStop stop2 = stops.get(stopIndex2);
            float zoomAlpha = stopIndex1 == stopIndex2 ? 1.0F : (PZMath.clamp(zoomF, stop1.zoom, stop2.zoom) - stop1.zoom) / (stop2.zoom - stop1.zoom);
            return PZMath.lerp(stop1.f, stop2.f, zoomAlpha);
        }
    }

    protected Texture evalTexture(WorldMapStyleLayer.RenderArgs args, ArrayList<? extends WorldMapStyleLayer.TextureStop> stops) {
        WorldMapStyleLayer.TextureStop textureStop = this.evalTextureStop(args, stops);
        return textureStop == null ? null : textureStop.texture;
    }

    protected WorldMapStyleLayer.TextureScaling evalTextureScaling(
        WorldMapStyleLayer.RenderArgs args, ArrayList<? extends WorldMapStyleLayer.TextureStop> stops, WorldMapStyleLayer.TextureScaling defaultValue
    ) {
        WorldMapStyleLayer.TextureStop textureStop = this.evalTextureStop(args, stops);
        return textureStop == null ? defaultValue : textureStop.scaling;
    }

    protected WorldMapStyleLayer.TextureStop evalTextureStop(WorldMapStyleLayer.RenderArgs args, ArrayList<? extends WorldMapStyleLayer.TextureStop> stops) {
        if (stops.isEmpty()) {
            return null;
        } else {
            float zoomF = args.drawer.zoomF;
            int stopIndex = findStop(zoomF, stops);
            int stopIndex1 = stopIndex == -1 ? 0 : stopIndex;
            int stopIndex2 = PZMath.min(stopIndex + 1, stops.size() - 1);
            WorldMapStyleLayer.TextureStop stop1 = stops.get(stopIndex1);
            WorldMapStyleLayer.TextureStop stop2 = stops.get(stopIndex2);
            if (stop1 == stop2) {
                return zoomF < stop1.zoom ? null : stop1;
            } else if (!(zoomF < stop1.zoom) && !(zoomF > stop2.zoom)) {
                float zoomAlpha = stopIndex1 == stopIndex2 ? 1.0F : (PZMath.clamp(zoomF, stop1.zoom, stop2.zoom) - stop1.zoom) / (stop2.zoom - stop1.zoom);
                return zoomAlpha < 0.5F ? stop1 : stop2;
            } else {
                return null;
            }
        }
    }

    public boolean filter(WorldMapFeature feature, WorldMapStyleLayer.FilterArgs args) {
        return this.filter == null ? false : this.filter.filter(feature, args);
    }

    public abstract void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args);

    public void renderCell(WorldMapStyleLayer.RenderArgs args) {
    }

    public abstract void renderVisibleCells(WorldMapStyleLayer.RenderArgs arg0);

    public static class ColorStop extends WorldMapStyleLayer.Stop {
        public int r;
        public int g;
        public int b;
        public int a;

        public ColorStop(float zoom, int r, int g, int b, int a) {
            super(zoom);
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    public static final class FilterArgs {
        public WorldMapRenderer renderer;
    }

    public static class FloatStop extends WorldMapStyleLayer.Stop {
        public float f;

        public FloatStop(float zoom, float f) {
            super(zoom);
            this.f = f;
        }
    }

    public interface IWorldMapStyleFilter {
        boolean filter(WorldMapFeature feature, WorldMapStyleLayer.FilterArgs args);
    }

    public static final class RGBAf {
        public float r;
        public float g;
        public float b;
        public float a;
        public static final ObjectPool<WorldMapStyleLayer.RGBAf> s_pool = new ObjectPool<>(WorldMapStyleLayer.RGBAf::new);

        public RGBAf() {
            this.r = this.g = this.b = this.a = 1.0F;
        }

        public WorldMapStyleLayer.RGBAf init(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }
    }

    public static final class RenderArgs {
        public WorldMapRenderer renderer;
        public WorldMapRenderer.Drawer drawer;
        public int cellX;
        public int cellY;
    }

    public static class Stop {
        public float zoom;

        Stop(float zoom) {
            this.zoom = zoom;
        }
    }

    public static enum TextureScaling {
        IsoGridSquare,
        ScreenPixel;
    }

    public static class TextureStop extends WorldMapStyleLayer.Stop {
        public String texturePath;
        public Texture texture;
        public WorldMapStyleLayer.TextureScaling scaling = WorldMapStyleLayer.TextureScaling.IsoGridSquare;

        public TextureStop(float zoom, String texturePath) {
            super(zoom);
            this.texturePath = texturePath;
            this.texture = Texture.getTexture(texturePath);
        }

        public TextureStop(float zoom, String texturePath, WorldMapStyleLayer.TextureScaling scaling) {
            super(zoom);
            this.texturePath = texturePath;
            this.texture = Texture.getTexture(texturePath);
            this.scaling = scaling == null ? WorldMapStyleLayer.TextureScaling.IsoGridSquare : scaling;
        }

        public TextureStop(float zoom, String texturePath, String scalingStr) {
            this(zoom, texturePath, WorldMapStyleLayer.TextureScaling.valueOf(scalingStr));
        }
    }
}
