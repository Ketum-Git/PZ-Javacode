// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureID;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.MapFiles;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;
import zombie.worldMap.streets.StreetRenderData;
import zombie.worldMap.styles.WorldMapPyramidStyleLayer;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapTextureStyleLayer;
import zombie.worldMap.symbols.SymbolsLayoutData;
import zombie.worldMap.symbols.SymbolsRenderData;

public final class WorldMapRenderer {
    private WorldMap worldMap;
    private int x;
    private int y;
    private int width;
    private int height;
    private int zoom;
    private float zoomF;
    private float displayZoomF;
    private float centerWorldX;
    private float centerWorldY;
    private float zoomUiX;
    private float zoomUiY;
    private float zoomWorldX;
    private float zoomWorldY;
    private boolean transitionTo;
    private float transitionFromWorldX;
    private float transitionFromWorldY;
    private float transitionFromZoomF;
    private float transitionToWorldX;
    private float transitionToWorldY;
    private float transitionToZoomF;
    private long transitionToStartMs;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f modelViewProjection = new Matrix4f();
    private final Quaternionf modelViewChange = new Quaternionf();
    private long viewChangeTime;
    private static final long VIEW_CHANGE_TIME = 175L;
    private boolean isIsometric;
    private boolean firstUpdate;
    private WorldMapVisited visited;
    private final WorldMapRenderer.Drawer[] drawer = new WorldMapRenderer.Drawer[3];
    private final WorldMapRenderer.CharacterModelCamera characterModelCamera = new WorldMapRenderer.CharacterModelCamera();
    private int dropShadowWidth = 12;
    public WorldMapStyle style;
    protected static VBORenderer vboLines;
    private float maxZoom = 18.0F;
    private final int[] viewport = new int[]{0, 0, 0, 0};
    private static final ThreadLocal<ObjectPool<UI3DScene.Plane>> TL_Plane_pool = ThreadLocal.withInitial(UI3DScene.PlaneObjectPool::new);
    private static final ThreadLocal<ObjectPool<UI3DScene.Ray>> TL_Ray_pool = ThreadLocal.withInitial(UI3DScene.RayObjectPool::new);
    static final float SMALL_NUM = 1.0E-8F;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final WorldMapRenderer.WorldMapBooleanOption allPrintMedia = new WorldMapRenderer.WorldMapBooleanOption("AllPrintMedia", false);
    private final WorldMapRenderer.WorldMapBooleanOption allStashMaps = new WorldMapRenderer.WorldMapBooleanOption("AllStashMaps", false);
    private final WorldMapRenderer.WorldMapBooleanOption animals = new WorldMapRenderer.WorldMapBooleanOption("Animals", false);
    private final WorldMapRenderer.WorldMapBooleanOption animalTracks = new WorldMapRenderer.WorldMapBooleanOption("AnimalTracks", false);
    private final WorldMapRenderer.WorldMapBooleanOption basements = new WorldMapRenderer.WorldMapBooleanOption("Basements", false);
    private final WorldMapRenderer.WorldMapBooleanOption blurUnvisited = new WorldMapRenderer.WorldMapBooleanOption("BlurUnvisited", true);
    private final WorldMapRenderer.WorldMapBooleanOption buildingsWithoutFeatures = new WorldMapRenderer.WorldMapBooleanOption(
        "BuildingsWithoutFeatures", false
    );
    private final WorldMapRenderer.WorldMapBooleanOption cellGrid = new WorldMapRenderer.WorldMapBooleanOption("CellGrid", false);
    private final WorldMapRenderer.WorldMapBooleanOption clampBaseZoomToPoint5 = new WorldMapRenderer.WorldMapBooleanOption("ClampBaseZoomToPoint5", true);
    private final WorldMapRenderer.WorldMapBooleanOption colorblindPatterns = new WorldMapRenderer.WorldMapBooleanOption("ColorblindPatterns", false);
    private final WorldMapRenderer.WorldMapBooleanOption debugInfo = new WorldMapRenderer.WorldMapBooleanOption("DebugInfo", false);
    private final WorldMapRenderer.WorldMapBooleanOption placeNames = new WorldMapRenderer.WorldMapBooleanOption("PlaceNames", true);
    private final WorldMapRenderer.WorldMapBooleanOption tileGrid = new WorldMapRenderer.WorldMapBooleanOption("TileGrid", false);
    private final WorldMapRenderer.WorldMapBooleanOption unvisitedGrid = new WorldMapRenderer.WorldMapBooleanOption("UnvisitedGrid", true);
    private final WorldMapRenderer.WorldMapBooleanOption features = new WorldMapRenderer.WorldMapBooleanOption("Features", true);
    private final WorldMapRenderer.WorldMapBooleanOption otherZones = new WorldMapRenderer.WorldMapBooleanOption("OtherZones", false);
    private final WorldMapRenderer.WorldMapBooleanOption forestZones = new WorldMapRenderer.WorldMapBooleanOption("ForagingZones", false);
    private final WorldMapRenderer.WorldMapBooleanOption zombieIntensity = new WorldMapRenderer.WorldMapBooleanOption("ZombieIntensity", false);
    private final WorldMapRenderer.WorldMapBooleanOption zombieVoronoi = new WorldMapRenderer.WorldMapBooleanOption("ZombieVoronoi", false);
    private final WorldMapRenderer.WorldMapBooleanOption zombieCutoff = new WorldMapRenderer.WorldMapBooleanOption("ZombieCutoff", false);
    private final WorldMapRenderer.WorldMapBooleanOption hideUnvisited = new WorldMapRenderer.WorldMapBooleanOption("HideUnvisited", false);
    private final WorldMapRenderer.WorldMapBooleanOption hitTest = new WorldMapRenderer.WorldMapBooleanOption("HitTest", false);
    private final WorldMapRenderer.WorldMapBooleanOption highlightStreet = new WorldMapRenderer.WorldMapBooleanOption("HighlightStreet", true);
    private final WorldMapRenderer.WorldMapBooleanOption largeStreetLabel = new WorldMapRenderer.WorldMapBooleanOption("LargeStreetLabel", true);
    private final WorldMapRenderer.WorldMapBooleanOption outlineStreet = new WorldMapRenderer.WorldMapBooleanOption("OutlineStreets", false);
    private final WorldMapRenderer.WorldMapBooleanOption showStreetNames = new WorldMapRenderer.WorldMapBooleanOption("ShowStreetNames", true);
    private final WorldMapRenderer.WorldMapBooleanOption imagePyramid = new WorldMapRenderer.WorldMapBooleanOption("ImagePyramid", true);
    private final WorldMapRenderer.WorldMapBooleanOption terrainImage = new WorldMapRenderer.WorldMapBooleanOption("TerrainImage", false);
    private final WorldMapRenderer.WorldMapBooleanOption isometric = new WorldMapRenderer.WorldMapBooleanOption("Isometric", true);
    private final WorldMapRenderer.WorldMapBooleanOption lineString = new WorldMapRenderer.WorldMapBooleanOption("LineString", true);
    private final WorldMapRenderer.WorldMapBooleanOption players = new WorldMapRenderer.WorldMapBooleanOption("Players", false);
    private final WorldMapRenderer.WorldMapBooleanOption playerModel = new WorldMapRenderer.WorldMapBooleanOption("PlayerModel", false);
    private final WorldMapRenderer.WorldMapBooleanOption remotePlayers = new WorldMapRenderer.WorldMapBooleanOption("RemotePlayers", false);
    private final WorldMapRenderer.WorldMapBooleanOption playerNames = new WorldMapRenderer.WorldMapBooleanOption("PlayerNames", false);
    private final WorldMapRenderer.WorldMapBooleanOption symbols = new WorldMapRenderer.WorldMapBooleanOption("Symbols", true);
    private final WorldMapRenderer.WorldMapBooleanOption wireframe = new WorldMapRenderer.WorldMapBooleanOption("Wireframe", false);
    private final WorldMapRenderer.WorldMapBooleanOption worldBounds = new WorldMapRenderer.WorldMapBooleanOption("WorldBounds", true);
    private final WorldMapRenderer.WorldMapBooleanOption miniMapSymbols = new WorldMapRenderer.WorldMapBooleanOption("MiniMapSymbols", false);
    private final WorldMapRenderer.WorldMapBooleanOption visibleCells = new WorldMapRenderer.WorldMapBooleanOption("VisibleCells", false);
    private final WorldMapRenderer.WorldMapBooleanOption visibleTiles = new WorldMapRenderer.WorldMapBooleanOption("VisibleTiles", false);
    private final WorldMapRenderer.WorldMapBooleanOption dimUnsharedSymbols = new WorldMapRenderer.WorldMapBooleanOption("DimUnsharedSymbols", false);
    private final WorldMapRenderer.WorldMapBooleanOption storyZones = new WorldMapRenderer.WorldMapBooleanOption("StoryZones", false);
    private final WorldMapRenderer.WorldMapBooleanOption wgRoads = new WorldMapRenderer.WorldMapBooleanOption("WGRoads", false);
    private final WorldMapRenderer.WorldMapBooleanOption infiniteZoom = new WorldMapRenderer.WorldMapBooleanOption("InfiniteZoom", false);

    public WorldMapRenderer() {
        PZArrayUtil.arrayPopulate(this.drawer, WorldMapRenderer.Drawer::new);
    }

    public int getAbsoluteX() {
        return this.x;
    }

    public int getAbsoluteY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    private void calcMatrices(float centerWorldX, float centerWorldY, float zoomF, Matrix4f projection, Matrix4f modelView) {
        int w = this.getWidth();
        int h = this.getHeight();
        projection.setOrtho(-w / 2.0F, w / 2.0F, h / 2.0F, -h / 2.0F, -2000.0F, 2000.0F);
        modelView.identity();
        if (this.isometric.getValue()) {
            modelView.rotateXYZ((float) (Math.PI / 3), 0.0F, (float) (Math.PI / 4));
        }
    }

    public Vector3f uiToScene(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, Vector3f out) {
        UI3DScene.Plane plane = allocPlane();
        plane.point.set(0.0F);
        plane.normal.set(0.0F, 0.0F, 1.0F);
        UI3DScene.Ray cameraRay = this.getCameraRay(uiX, this.getHeight() - uiY, projection, modelView, allocRay());
        if (this.intersect_ray_plane(plane, cameraRay, out) != 1) {
            out.set(0.0F);
        }

        releasePlane(plane);
        releaseRay(cameraRay);
        return out;
    }

    public Vector3f uiToScene(float uiX, float uiY, Matrix4f modelViewProjection, Vector3f out) {
        UI3DScene.Plane plane = allocPlane();
        plane.point.set(0.0F);
        plane.normal.set(0.0F, 0.0F, 1.0F);
        UI3DScene.Ray cameraRay = this.getCameraRay(uiX, this.getHeight() - uiY, modelViewProjection, allocRay());
        if (this.intersect_ray_plane(plane, cameraRay, out) != 1) {
            out.set(0.0F);
        }

        releasePlane(plane);
        releaseRay(cameraRay);
        return out;
    }

    public Vector3f sceneToUI(float sceneX, float sceneY, float sceneZ, Matrix4f modelViewProjection, Vector3f out) {
        this.viewport[0] = 0;
        this.viewport[1] = 0;
        this.viewport[2] = this.getWidth();
        this.viewport[3] = this.getHeight();
        modelViewProjection.project(sceneX, sceneY, sceneZ, this.viewport, out);
        return out;
    }

    public Vector3f sceneToUI(float sceneX, float sceneY, float sceneZ, Matrix4f projection, Matrix4f modelView, Vector3f out) {
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        this.sceneToUI(sceneX, sceneY, sceneZ, matrix4f, out);
        releaseMatrix4f(matrix4f);
        return out;
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY) {
        Matrix4f projection = allocMatrix4f();
        Matrix4f modelView = allocMatrix4f();
        this.calcMatrices(centerWorldX, centerWorldY, zoomF, projection, modelView);
        float x = this.uiToWorldX(uiX, uiY, zoomF, centerWorldX, centerWorldY, projection, modelView);
        releaseMatrix4f(projection);
        releaseMatrix4f(modelView);
        return x;
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY) {
        Matrix4f projection = allocMatrix4f();
        Matrix4f modelView = allocMatrix4f();
        this.calcMatrices(centerWorldX, centerWorldY, zoomF, projection, modelView);
        float y = this.uiToWorldY(uiX, uiY, zoomF, centerWorldX, centerWorldY, projection, modelView);
        releaseMatrix4f(projection);
        releaseMatrix4f(modelView);
        return y;
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, projection, modelView, allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0F / worldScale);
        float worldX = worldPos.x() + centerWorldX;
        releaseVector3f(worldPos);
        return worldX;
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, projection, modelView, allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0F / worldScale);
        float worldY = worldPos.y() + centerWorldY;
        releaseVector3f(worldPos);
        return worldY;
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, modelViewProjection, allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0F / worldScale);
        float worldX = worldPos.x() + centerWorldX;
        releaseVector3f(worldPos);
        return worldX;
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        Vector3f worldPos = this.uiToScene(uiX, uiY, modelViewProjection, allocVector3f());
        float worldScale = this.getWorldScale(zoomF);
        worldPos.mul(1.0F / worldScale);
        float worldY = worldPos.y() + centerWorldY;
        releaseVector3f(worldPos);
        return worldY;
    }

    public float worldToUIX(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI(
            (worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0F, projection, modelView, allocVector3f()
        );
        float uiX = uiPos.x();
        releaseVector3f(uiPos);
        return uiX;
    }

    public float worldToUIY(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI(
            (worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0F, projection, modelView, allocVector3f()
        );
        float uiY = this.getHeight() - uiPos.y();
        releaseVector3f(uiPos);
        return uiY;
    }

    public float worldToUIX(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI((worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0F, modelViewProjection, allocVector3f());
        float uiX = uiPos.x();
        releaseVector3f(uiPos);
        return uiX;
    }

    public float worldToUIY(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        float worldScale = this.getWorldScale(zoomF);
        Vector3f uiPos = this.sceneToUI((worldX - centerWorldX) * worldScale, (worldY - centerWorldY) * worldScale, 0.0F, modelViewProjection, allocVector3f());
        float uiY = this.getHeight() - uiPos.y();
        releaseVector3f(uiPos);
        return uiY;
    }

    public float worldOriginUIX(float zoomF, float centerWorldX) {
        return this.worldToUIX(0.0F, 0.0F, zoomF, centerWorldX, this.centerWorldY, this.modelViewProjection);
    }

    public float worldOriginUIY(float zoomF, float centerWorldY) {
        return this.worldToUIY(0.0F, 0.0F, zoomF, this.centerWorldX, centerWorldY, this.modelViewProjection);
    }

    public int getZoom() {
        return this.zoom;
    }

    public float getZoomF() {
        return this.zoomF;
    }

    public float getDisplayZoomF() {
        return this.displayZoomF;
    }

    public float zoomMult() {
        return this.zoomMult(this.zoomF);
    }

    public float zoomMult(float zoomF) {
        return (float)Math.pow(2.0, zoomF);
    }

    public float getWorldScale(float zoomF) {
        int tileSize = this.getHeight();
        double metersPerPixel = MapProjection.metersPerPixelAtZoom(zoomF, tileSize);
        return (float)(1.0 / metersPerPixel);
    }

    public void zoomAt(int mouseX, int mouseY, int delta) {
        float worldX = this.uiToWorldX(mouseX, mouseY, this.displayZoomF, this.centerWorldX, this.centerWorldY);
        float worldY = this.uiToWorldY(mouseX, mouseY, this.displayZoomF, this.centerWorldX, this.centerWorldY);
        this.zoomF = PZMath.clamp(this.zoomF + delta / 2.0F, this.getBaseZoom(), this.getMaxZoom());
        this.zoom = (int)this.zoomF;
        this.zoomWorldX = worldX;
        this.zoomWorldY = worldY;
        this.zoomUiX = mouseX;
        this.zoomUiY = mouseY;
    }

    public void transitionTo(float worldX, float worldY, float zoomF) {
        this.transitionFromWorldX = this.centerWorldX;
        this.transitionFromWorldY = this.centerWorldY;
        this.transitionFromZoomF = this.displayZoomF;
        this.transitionToWorldX = worldX;
        this.transitionToWorldY = worldY;
        this.transitionToZoomF = PZMath.clamp(zoomF, this.getBaseZoom(), this.getMaxZoom());
        this.transitionTo = true;
        this.transitionToStartMs = System.currentTimeMillis();
    }

    public float getCenterWorldX() {
        return this.centerWorldX;
    }

    public float getCenterWorldY() {
        return this.centerWorldY;
    }

    public void centerOn(float worldX, float worldY) {
        this.centerWorldX = worldX;
        this.centerWorldY = worldY;
        if (this.displayZoomF != this.zoomF) {
            this.zoomWorldX = worldX;
            this.zoomWorldY = worldY;
            this.zoomUiX = this.width / 2.0F;
            this.zoomUiY = this.height / 2.0F;
        }
    }

    public void moveView(int dx, int dy) {
        this.centerOn(this.centerWorldX + dx, this.centerWorldY + dy);
    }

    public double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    public float getBaseZoom() {
        double zoom = MapProjection.zoomAtMetersPerPixel((double)this.worldMap.getHeightInSquares() / this.getHeight(), this.getHeight());
        if (this.worldMap.getWidthInSquares() * this.getWorldScale((float)zoom) > this.getWidth()) {
            zoom = MapProjection.zoomAtMetersPerPixel((double)this.worldMap.getWidthInSquares() / this.getWidth(), this.getHeight());
        }

        if (this.clampBaseZoomToPoint5.getValue()) {
            zoom = (int)(zoom * 2.0) / 2.0;
        }

        return PZMath.max((float)zoom, this.infiniteZoom.getValue() ? (float)zoom : 12.0F);
    }

    public void setZoom(float zoom) {
        this.zoomF = PZMath.clamp(zoom, this.getBaseZoom(), this.getMaxZoom());
        this.zoom = (int)this.zoomF;
        this.displayZoomF = this.zoomF;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = PZMath.clamp(maxZoom, 0.0F, 24.0F);
    }

    public float getMaxZoom() {
        return this.infiniteZoom.getValue() ? 24.0F : this.maxZoom;
    }

    public void resetView() {
        this.zoomF = this.getWidth() * this.getHeight() < 1 ? 12.0F : this.getBaseZoom();
        this.zoom = (int)this.zoomF;
        this.centerWorldX = this.worldMap.getMinXInSquares() + this.worldMap.getWidthInSquares() / 2.0F;
        this.centerWorldY = this.worldMap.getMinYInSquares() + this.worldMap.getHeightInSquares() / 2.0F;
        this.zoomWorldX = this.centerWorldX;
        this.zoomWorldY = this.centerWorldY;
        this.zoomUiX = this.getWidth() / 2.0F;
        this.zoomUiY = this.getHeight() / 2.0F;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projection;
    }

    public Matrix4f getModelViewMatrix() {
        return this.modelView;
    }

    public Matrix4f getModelViewProjectionMatrix() {
        return this.modelViewProjection;
    }

    public void setMap(WorldMap worldMap, int x, int y, int width, int height) {
        this.worldMap = worldMap;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public WorldMap getWorldMap() {
        return this.worldMap;
    }

    public void setVisited(WorldMapVisited visited) {
        this.visited = visited;
    }

    public WorldMapVisited getVisited() {
        return this.visited;
    }

    public void updateView() {
        if (this.worldMap != null
            && this.worldMap.getWidthInSquares() > 1
            && this.worldMap.getHeightInSquares() > 1
            && this.getWidth() >= 1
            && this.getHeight() >= 1) {
            if (this.displayZoomF == 0.0F) {
                this.displayZoomF = this.zoomF;
            }

            if (this.displayZoomF != this.zoomF) {
                float dt = (float)(UIManager.getMillisSinceLastRender() / 750.0);
                float diff = Math.abs(this.zoomF - this.displayZoomF);
                float mult = diff > 0.25F ? diff / 0.25F : 1.0F;
                if (this.displayZoomF < this.zoomF) {
                    this.displayZoomF = PZMath.min(this.displayZoomF + dt * mult, this.zoomF);
                } else if (this.displayZoomF > this.zoomF) {
                    this.displayZoomF = PZMath.max(this.displayZoomF - dt * mult, this.zoomF);
                }

                float worldX2 = this.uiToWorldX(this.zoomUiX, this.zoomUiY, this.displayZoomF, 0.0F, 0.0F);
                float worldY2 = this.uiToWorldY(this.zoomUiX, this.zoomUiY, this.displayZoomF, 0.0F, 0.0F);
                this.centerWorldX = this.zoomWorldX - worldX2;
                this.centerWorldY = this.zoomWorldY - worldY2;
            }

            if (!this.firstUpdate) {
                this.firstUpdate = true;
                this.isIsometric = this.isometric.getValue();
            }

            if (this.isIsometric != this.isometric.getValue()) {
                this.isIsometric = this.isometric.getValue();
                long ms = System.currentTimeMillis();
                if (this.viewChangeTime + 175L < ms) {
                    this.modelViewChange.setFromUnnormalized(this.modelView);
                }

                this.viewChangeTime = ms;
            }

            if (this.transitionTo) {
                long elapsed = System.currentTimeMillis() - this.transitionToStartMs;
                if (elapsed >= 600L) {
                    this.transitionTo = false;
                    this.centerWorldX = this.transitionToWorldX;
                    this.centerWorldY = this.transitionToWorldY;
                    this.zoomF = this.transitionToZoomF;
                    this.zoom = (int)PZMath.floor(this.zoomF);
                    this.displayZoomF = this.zoomF;
                } else {
                    float F = (float)elapsed / 600.0F;
                    F = PZMath.lerpFunc_EaseOutInQuad(F);
                    this.centerWorldX = PZMath.lerp(this.transitionFromWorldX, this.transitionToWorldX, F);
                    this.centerWorldY = PZMath.lerp(this.transitionFromWorldY, this.transitionToWorldY, F);
                    this.zoomF = PZMath.lerp(this.transitionFromZoomF, this.transitionToZoomF, F);
                    this.zoom = (int)PZMath.floor(this.zoomF);
                    this.displayZoomF = this.zoomF;
                }
            }

            this.calcMatrices(this.centerWorldX, this.centerWorldY, this.displayZoomF, this.projection, this.modelView);
            long ms = System.currentTimeMillis();
            if (this.viewChangeTime + 175L > ms) {
                float f = (float)(this.viewChangeTime + 175L - ms) / 175.0F;
                Quaternionf q1 = allocQuaternionf().set(this.modelViewChange);
                Quaternionf q2 = allocQuaternionf().setFromUnnormalized(this.modelView);
                this.modelView.set(q1.slerp(q2, 1.0F - f));
                releaseQuaternionf(q1);
                releaseQuaternionf(q2);
            }

            this.modelViewProjection.set(this.projection).mul(this.modelView);
        }
    }

    public void render(UIWorldMap ui) {
        vboLines = VBORenderer.getInstance();
        this.style = ui.getAPI().getStyle();
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.drawer[stateIndex].init(this, ui);
        SpriteRenderer.instance.drawGeneric(this.drawer[stateIndex]);
    }

    public void setDropShadowWidth(int width) {
        this.dropShadowWidth = width;
    }

    private static Matrix4f allocMatrix4f() {
        return BaseVehicle.TL_matrix4f_pool.get().alloc();
    }

    private static void releaseMatrix4f(Matrix4f matrix) {
        BaseVehicle.TL_matrix4f_pool.get().release(matrix);
    }

    private static Quaternionf allocQuaternionf() {
        return BaseVehicle.TL_quaternionf_pool.get().alloc();
    }

    private static void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.TL_quaternionf_pool.get().release(q);
    }

    private static UI3DScene.Ray allocRay() {
        return TL_Ray_pool.get().alloc();
    }

    private static void releaseRay(UI3DScene.Ray ray) {
        TL_Ray_pool.get().release(ray);
    }

    private static UI3DScene.Plane allocPlane() {
        return TL_Plane_pool.get().alloc();
    }

    private static void releasePlane(UI3DScene.Plane Plane) {
        TL_Plane_pool.get().release(Plane);
    }

    private static Vector2 allocVector2() {
        return Vector2ObjectPool.get().alloc();
    }

    private static void releaseVector2(Vector2 vector2) {
        Vector2ObjectPool.get().release(vector2);
    }

    private static Vector3f allocVector3f() {
        return BaseVehicle.TL_vector3f_pool.get().alloc();
    }

    private static void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.TL_vector3f_pool.get().release(vector3f);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, UI3DScene.Ray camera_ray) {
        return this.getCameraRay(uiX, uiY, this.modelViewProjection, camera_ray);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, UI3DScene.Ray camera_ray) {
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        this.getCameraRay(uiX, uiY, matrix4f, camera_ray);
        releaseMatrix4f(matrix4f);
        return camera_ray;
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f modelViewProjection, UI3DScene.Ray camera_ray) {
        Matrix4f matrix4f = allocMatrix4f().set(modelViewProjection);
        matrix4f.invert();
        this.viewport[0] = 0;
        this.viewport[1] = 0;
        this.viewport[2] = this.getWidth();
        this.viewport[3] = this.getHeight();
        Vector3f ray_start = matrix4f.unprojectInv(uiX, uiY, 0.0F, this.viewport, allocVector3f());
        Vector3f ray_end = matrix4f.unprojectInv(uiX, uiY, 1.0F, this.viewport, allocVector3f());
        camera_ray.origin.set(ray_start);
        camera_ray.direction.set(ray_end.sub(ray_start).normalize());
        releaseVector3f(ray_end);
        releaseVector3f(ray_start);
        releaseMatrix4f(matrix4f);
        return camera_ray;
    }

    int intersect_ray_plane(UI3DScene.Plane Pn, UI3DScene.Ray S, Vector3f out) {
        Vector3f u = allocVector3f().set(S.direction).mul(10000.0F);
        Vector3f w = allocVector3f().set(S.origin).sub(Pn.point);

        byte sI;
        try {
            float D = Pn.normal.dot(u);
            float N = -Pn.normal.dot(w);
            if (!(Math.abs(D) < 1.0E-8F)) {
                float sIx = N / D;
                if (!(sIx < 0.0F) && !(sIx > 1.0F)) {
                    out.set(S.origin).add(u.mul(sIx));
                    return 1;
                }

                return 0;
            }

            if (N != 0.0F) {
                return 0;
            }

            sI = 2;
        } finally {
            releaseVector3f(u);
            releaseVector3f(w);
        }

        return sI;
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        if (this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption) {
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        return this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption ? booleanConfigOption.getValue() : false;
    }

    public void setDouble(String name, double value) {
        if (this.getOptionByName(name) instanceof DoubleConfigOption doubleConfigOption) {
            doubleConfigOption.setValue(value);
        }
    }

    public double getDouble(String name, double defaultValue) {
        return this.getOptionByName(name) instanceof DoubleConfigOption doubleConfigOption ? doubleConfigOption.getValue() : defaultValue;
    }

    public boolean isDimUnsharedSymbols() {
        return this.dimUnsharedSymbols.getValue();
    }

    private static final class CharacterModelCamera extends ModelCamera {
        float worldScale;
        float angle;
        float playerX;
        float playerY;
        boolean vehicle;

        @Override
        public void Begin() {
            Matrix4f matrix4f = WorldMapRenderer.allocMatrix4f();
            matrix4f.identity();
            matrix4f.translate(this.playerX * this.worldScale, this.playerY * this.worldScale, 0.0F);
            matrix4f.rotateX((float) (Math.PI / 2));
            matrix4f.rotateY(this.angle + (float) (Math.PI * 3.0 / 2.0));
            if (this.vehicle) {
                matrix4f.scale(this.worldScale);
            } else {
                matrix4f.scale(1.5F * this.worldScale);
            }

            PZGLUtil.pushAndMultMatrix(5888, matrix4f);
            WorldMapRenderer.releaseMatrix4f(matrix4f);
        }

        @Override
        public void End() {
            PZGLUtil.popMatrix(5888);
        }
    }

    public static final class Drawer extends TextureDraw.GenericDrawer {
        public WorldMapRenderer renderer;
        public final WorldMapStyle style = new WorldMapStyle();
        public WorldMap worldMap;
        public int x;
        public int y;
        public int width;
        public int height;
        float centerWorldX;
        float centerWorldY;
        int zoom;
        public float zoomF;
        public float worldScale;
        float renderOriginX;
        float renderOriginY;
        public float worldOriginUiX;
        public float worldOriginUiY;
        float renderCellX;
        float renderCellY;
        private final Matrix4f projection = new Matrix4f();
        private final Matrix4f modelView = new Matrix4f();
        private final Matrix4f modelViewProjection = new Matrix4f();
        private final WorldMapRenderer.PlayerRenderData[] playerRenderData = new WorldMapRenderer.PlayerRenderData[4];
        final WorldMapStyleLayer.FilterArgs filterArgs = new WorldMapStyleLayer.FilterArgs();
        final WorldMapStyleLayer.RenderArgs renderArgs = new WorldMapStyleLayer.RenderArgs();
        final TLongObjectHashMap<WorldMapRenderCell> renderCellLookup = new TLongObjectHashMap<>();
        WorldMapRenderer.ListOfRenderLayers[] renderLayersForStyleLayer;
        final ArrayList<WorldMapRenderCell> renderCells = new ArrayList<>();
        final ArrayList<WorldMapFeature> features = new ArrayList<>();
        final ArrayList<Zone> zones = new ArrayList<>();
        final HashSet<Zone> zoneSet = new HashSet<>();
        WorldMapStyleLayer.RGBAf fill;
        int triangulationsThisFrame;
        public final SymbolsRenderData symbolsRenderData = new SymbolsRenderData();
        public final SymbolsLayoutData symbolsLayoutData = new SymbolsLayoutData();
        final StreetRenderData streetRenderData = new StreetRenderData();
        float[] floatArray;
        final Vector2f vector2f = new Vector2f();
        final TIntArrayList rasterizeXy = new TIntArrayList();
        final TIntArrayList rasterizePyramidXy = new TIntArrayList();
        final TIntSet rasterizeSet = new TIntHashSet();
        float rasterizeMinTileX;
        float rasterizeMinTileY;
        float rasterizeMaxTileX;
        float rasterizeMaxTileY;
        final Rasterize rasterize = new Rasterize();
        int[] rasterizeXyInts;
        int rasterizeMult = 1;
        final int[] rasterizeTileBounds = new int[4];
        static final int PRT_REQUIRED = 1;
        static final int PRT_RENDERED = 2;
        byte[] renderedTiles;
        static final int[] s_tilesCoveringCell = new int[4];

        Drawer() {
            PZArrayUtil.arrayPopulate(this.playerRenderData, WorldMapRenderer.PlayerRenderData::new);
        }

        void init(WorldMapRenderer renderer, UIWorldMap ui) {
            this.renderer = renderer;
            this.style.copyFrom(this.renderer.style);
            this.worldMap = renderer.worldMap;
            this.x = renderer.x;
            this.y = renderer.y;
            this.width = renderer.width;
            this.height = renderer.height;
            this.centerWorldX = renderer.centerWorldX;
            this.centerWorldY = renderer.centerWorldY;
            this.zoomF = renderer.displayZoomF;
            this.zoom = (int)this.zoomF;
            this.worldScale = this.getWorldScale();
            this.renderOriginX = (this.renderer.worldMap.getMinXInSquares() - this.centerWorldX) * this.worldScale;
            this.renderOriginY = (this.renderer.worldMap.getMinYInSquares() - this.centerWorldY) * this.worldScale;
            this.projection.set(renderer.projection);
            this.modelView.set(renderer.modelView);
            this.modelViewProjection.set(renderer.modelViewProjection);
            this.fill = ui.color;
            this.triangulationsThisFrame = 0;
            this.streetRenderData.init(ui, renderer);
            this.worldOriginUiX = this.worldOriginUIX(this.centerWorldX);
            this.worldOriginUiY = this.worldOriginUIY(this.centerWorldY);
            this.symbolsRenderData.renderMain(ui, ui.symbols, this.symbolsLayoutData, this.style);
            if (this.renderer.visited != null) {
                this.renderer.visited.renderMain();
            }

            for (int i = 0; i < 4; i++) {
                this.playerRenderData[i].modelSlotRenderData = null;
            }

            if (this.renderer.players.getValue() && this.zoomF >= 20.0F && this.renderer.playerModel.getValue()) {
                for (int pn = 0; pn < 4; pn++) {
                    IsoPlayer player = IsoPlayer.players[pn];
                    if (player != null && !player.isDead() && player.legsSprite.hasActiveModel()) {
                        float playerX = player.getX();
                        float playerY = player.getY();
                        if (player.getVehicle() != null) {
                            playerX = player.getVehicle().getX();
                            playerY = player.getVehicle().getY();
                        }

                        float uiX = this.renderer.worldToUIX(playerX, playerY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
                        float uiY = this.renderer.worldToUIY(playerX, playerY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
                        if (!(uiX < -100.0F) && !(uiX > this.width + 100) && !(uiY < -100.0F) && !(uiY > this.height + 100)) {
                            this.playerRenderData[pn].angle = player.getVehicle() == null
                                ? player.getAnimationPlayer().getAngle()
                                : (float) (Math.PI * 3.0 / 2.0);
                            this.playerRenderData[pn].x = playerX - this.centerWorldX;
                            this.playerRenderData[pn].y = playerY - this.centerWorldY;
                            player.legsSprite.modelSlot.model.updateLights();
                            int cameraIndex = IsoCamera.frameState.playerIndex;
                            IsoCamera.frameState.playerIndex = pn;
                            player.checkUpdateModelTextures();
                            this.playerRenderData[pn].modelSlotRenderData = ModelSlotRenderData.alloc();
                            this.playerRenderData[pn].modelSlotRenderData.initModel(player.legsSprite.modelSlot);
                            this.playerRenderData[pn].modelSlotRenderData.init(player.legsSprite.modelSlot);
                            this.playerRenderData[pn].modelSlotRenderData.centerOfMassY = 0.0F;
                            IsoCamera.frameState.playerIndex = cameraIndex;
                            player.legsSprite.modelSlot.renderRefCount++;
                        }
                    }
                }
            }
        }

        public int getAbsoluteX() {
            return this.x;
        }

        public int getAbsoluteY() {
            return this.y;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public float getWorldScale() {
            return this.renderer.getWorldScale(this.zoomF);
        }

        public float uiToWorldX(float uiX, float uiY) {
            return this.renderer.uiToWorldX(uiX, uiY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float uiToWorldY(float uiX, float uiY) {
            return this.renderer.uiToWorldY(uiX, uiY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float worldToUIX(float worldX, float worldY) {
            return this.renderer.worldToUIX(worldX, worldY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float worldToUIY(float worldX, float worldY) {
            return this.renderer.worldToUIY(worldX, worldY, this.zoomF, this.centerWorldX, this.centerWorldY, this.modelViewProjection);
        }

        public float worldOriginUIX(float centerWorldX) {
            return this.renderer.worldOriginUIX(this.zoomF, centerWorldX);
        }

        public float worldOriginUIY(float centerWorldY) {
            return this.renderer.worldOriginUIY(this.zoomF, centerWorldY);
        }

        public void renderVisibleCells(WorldMapStyleLayer styleLayer) {
            int layerIndex = this.style.indexOf(styleLayer);
            WorldMapRenderer.ListOfRenderLayers renderLayers = this.renderLayersForStyleLayer[layerIndex];

            for (int i = 0; i < renderLayers.size(); i++) {
                WorldMapRenderLayer renderLayer = renderLayers.get(i);
                WorldMapRenderCell renderCell = renderLayer.renderCell;
                int cellX = renderCell.cellX;
                int cellY = renderCell.cellY;
                this.renderCell(styleLayer, cellX, cellY, renderLayer.features);
            }
        }

        private void renderCellFeatures() {
            this.renderArgs.renderer = this.renderer;
            this.renderArgs.drawer = this;

            for (int i = 0; i < this.style.getLayerCount(); i++) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (!(styleLayer.minZoom > this.zoomF)) {
                    styleLayer.renderVisibleCells(this.renderArgs);
                }
            }
        }

        private void renderNonCellFeatures() {
            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    this.renderArgs.renderer = this.renderer;
                    this.renderArgs.drawer = this;
                    this.renderArgs.cellX = cellX;
                    this.renderArgs.cellY = cellY;
                    this.renderCellX = this.renderOriginX + (cellX * 256 - this.worldMap.getMinXInSquares()) * this.worldScale;
                    this.renderCellY = this.renderOriginY + (cellY * 256 - this.worldMap.getMinYInSquares()) * this.worldScale;

                    for (int j = 0; j < this.style.getLayerCount(); j++) {
                        WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(j);
                        if (!(styleLayer.minZoom > this.zoomF)
                            && (styleLayer instanceof WorldMapPyramidStyleLayer || styleLayer instanceof WorldMapTextureStyleLayer)) {
                            styleLayer.renderCell(this.renderArgs);
                        }
                    }
                }
            }
        }

        private void renderCell(WorldMapStyleLayer styleLayer, int cellX, int cellY, ArrayList<WorldMapFeature> features) {
            this.renderCellX = this.renderOriginX + (cellX * 256 - this.worldMap.getMinXInSquares()) * this.worldScale;
            this.renderCellY = this.renderOriginY + (cellY * 256 - this.worldMap.getMinYInSquares()) * this.worldScale;
            this.renderArgs.renderer = this.renderer;
            this.renderArgs.drawer = this;
            this.renderArgs.cellX = cellX;
            this.renderArgs.cellY = cellY;
            styleLayer.renderCell(this.renderArgs);

            for (int j = 0; j < features.size(); j++) {
                WorldMapFeature feature = features.get(j);
                styleLayer.render(feature, this.renderArgs);
            }
        }

        void filterFeatures() {
            for (int i = 0; i < this.renderCells.size(); i++) {
                WorldMapRenderCell renderCell = this.renderCells.get(i);
                WorldMapRenderLayer.pool.releaseAll(renderCell.renderLayers);
                renderCell.renderLayers.clear();
                WorldMapRenderCell.pool.release(renderCell);
            }

            this.renderCells.clear();
            this.renderCellLookup.clear();
            if (this.renderLayersForStyleLayer == null || this.renderLayersForStyleLayer.length < this.style.getLayerCount()) {
                this.renderLayersForStyleLayer = new WorldMapRenderer.ListOfRenderLayers[this.style.getLayerCount()];
            }

            for (int i = 0; i < this.style.getLayerCount(); i++) {
                if (this.renderLayersForStyleLayer[i] == null) {
                    this.renderLayersForStyleLayer[i] = new WorldMapRenderer.ListOfRenderLayers();
                }

                this.renderLayersForStyleLayer[i].clear();
            }

            this.filterArgs.renderer = this.renderer;

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    this.features.clear();

                    for (int k = 0; k < this.worldMap.data.size(); k++) {
                        WorldMapData data = this.worldMap.data.get(k);
                        if (data.isReady()) {
                            WorldMapCell cell = data.getCell(cellX, cellY);
                            if (cell != null && !cell.features.isEmpty()) {
                                if (cell.priority == -1) {
                                    ArrayList<MapFiles> mapFiles = MapFiles.getCurrentMapFiles();

                                    for (int m = 0; m < mapFiles.size(); m++) {
                                        MapFiles mapFiles1 = mapFiles.get(m);
                                        if (data.relativeFileName.startsWith("media/maps/" + mapFiles1.mapDirectoryName)) {
                                            cell.priority = m;
                                            break;
                                        }
                                    }
                                }

                                PZArrayUtil.addAll(this.features, cell.features);
                            }
                        }
                    }

                    if (!this.features.isEmpty()) {
                        WorldMapRenderCell renderCell = WorldMapRenderCell.alloc();
                        renderCell.cellX = cellX;
                        renderCell.cellY = cellY;
                        this.filterFeatures(this.features, this.filterArgs, renderCell);
                        if (renderCell.renderLayers.isEmpty()) {
                            WorldMapRenderCell.pool.release(renderCell);
                        } else {
                            this.renderCells.add(renderCell);
                            this.renderCellLookup.put(cellX | (long)cellY << 32, renderCell);
                        }
                    }
                }
            }
        }

        void filterFeatures(ArrayList<WorldMapFeature> features, WorldMapStyleLayer.FilterArgs args, WorldMapRenderCell renderCell) {
            for (int i = 0; i < this.style.getLayerCount(); i++) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (!styleLayer.ignoreFeatures() && !(styleLayer.minZoom > this.zoomF)) {
                    WorldMapRenderLayer renderLayer = null;

                    for (int j = 0; j < features.size(); j++) {
                        WorldMapFeature feature = features.get(j);
                        if (styleLayer.filter(feature, args)) {
                            if (renderLayer == null) {
                                renderLayer = WorldMapRenderLayer.pool.alloc();
                                renderLayer.renderCell = renderCell;
                                renderLayer.styleLayer = styleLayer;
                                renderLayer.features.clear();
                                renderCell.renderLayers.add(renderLayer);
                            }

                            renderLayer.features.add(feature);
                        }
                    }

                    if (renderLayer != null) {
                        this.renderLayersForStyleLayer[i].add(renderLayer);
                    }
                }
            }
        }

        void renderCellGrid(int minX, int minY, int maxX, int maxY) {
            float minXui = this.renderOriginX + (minX * 256 - this.worldMap.getMinXInSquares()) * this.worldScale;
            float minYui = this.renderOriginY + (minY * 256 - this.worldMap.getMinYInSquares()) * this.worldScale;
            float maxXui = minXui + (maxX - minX + 1) * 256 * this.worldScale;
            float maxYui = minYui + (maxY - minY + 1) * 256 * this.worldScale;
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(1);
            WorldMapRenderer.vboLines.setLineWidth(1.0F);

            for (int x = minX; x <= maxX + 1; x++) {
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (x * 256 - this.worldMap.getMinXInSquares()) * this.worldScale,
                        minYui,
                        0.0F,
                        this.renderOriginX + (x * 256 - this.worldMap.getMinXInSquares()) * this.worldScale,
                        maxYui,
                        0.0F,
                        0.25F,
                        0.25F,
                        0.25F,
                        1.0F
                    );
            }

            for (int y = minY; y <= maxY + 1; y++) {
                WorldMapRenderer.vboLines
                    .addLine(
                        minXui,
                        this.renderOriginY + (y * 256 - this.worldMap.getMinYInSquares()) * this.worldScale,
                        0.0F,
                        maxXui,
                        this.renderOriginY + (y * 256 - this.worldMap.getMinYInSquares()) * this.worldScale,
                        0.0F,
                        0.25F,
                        0.25F,
                        0.25F,
                        1.0F
                    );
            }

            WorldMapRenderer.vboLines.endRun();
            WorldMapRenderer.vboLines.flush();
        }

        void renderPlayers() {
            boolean bClearDepth = true;

            for (int i = 0; i < this.playerRenderData.length; i++) {
                WorldMapRenderer.PlayerRenderData playerRenderData = this.playerRenderData[i];
                if (playerRenderData.modelSlotRenderData != null) {
                    if (bClearDepth) {
                        GL11.glClear(256);
                        bClearDepth = false;
                    }

                    this.renderer.characterModelCamera.worldScale = this.worldScale;
                    this.renderer.characterModelCamera.useWorldIso = true;
                    this.renderer.characterModelCamera.angle = playerRenderData.angle;
                    this.renderer.characterModelCamera.playerX = playerRenderData.x;
                    this.renderer.characterModelCamera.playerY = playerRenderData.y;
                    this.renderer.characterModelCamera.vehicle = playerRenderData.modelSlotRenderData.inVehicle;
                    ModelCamera.instance = this.renderer.characterModelCamera;
                    playerRenderData.modelSlotRenderData.render();
                }
            }

            if (UIManager.useUiFbo) {
                GL14.glBlendFuncSeparate(770, 771, 1, 771);
            }
        }

        public void drawLineStringXXX(WorldMapStyleLayer.RenderArgs args, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.LineString) {
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(1);
                WorldMapRenderer.vboLines.setLineWidth(lineWidth);

                for (int k = 0; k < geometry.points.size(); k++) {
                    WorldMapPoints points = geometry.points.get(k);

                    for (int j = 0; j < points.numPoints() - 1; j++) {
                        float x1 = points.getX(j);
                        float y1 = points.getY(j);
                        float x2 = points.getX(j + 1);
                        float y2 = points.getY(j + 1);
                        WorldMapRenderer.vboLines
                            .addLine(
                                renderOriginX + x1 * scale,
                                renderOriginY + y1 * scale,
                                0.0F,
                                renderOriginX + x2 * scale,
                                renderOriginY + y2 * scale,
                                0.0F,
                                r,
                                g,
                                b,
                                a
                            );
                    }
                }

                WorldMapRenderer.vboLines.endRun();
            }
        }

        public void drawLineStringYYY(WorldMapStyleLayer.RenderArgs args, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.LineString) {
                StrokeGeometry.Point[] points = new StrokeGeometry.Point[geometry.points.size()];
                WorldMapPoints gpoints = geometry.points.get(0);

                for (int j = 0; j < gpoints.numPoints(); j++) {
                    float x1 = gpoints.getX(j);
                    float y1 = gpoints.getY(j);
                    points[j] = StrokeGeometry.newPoint(renderOriginX + x1 * scale, renderOriginY + y1 * scale);
                }

                StrokeGeometry.Attrs attrs = new StrokeGeometry.Attrs();
                attrs.join = "miter";
                attrs.width = lineWidth;
                ArrayList<StrokeGeometry.Point> vertices = StrokeGeometry.getStrokeGeometry(points, attrs);
                if (vertices == null) {
                    return;
                }

                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(4);

                for (int j = 0; j < vertices.size(); j++) {
                    float x1 = (float)vertices.get(j).x;
                    float y1 = (float)vertices.get(j).y;
                    WorldMapRenderer.vboLines.addElement(x1, y1, 0.0F, r, g, b, a);
                }

                WorldMapRenderer.vboLines.endRun();
                StrokeGeometry.release(vertices);
            }
        }

        public void drawLineString(WorldMapStyleLayer.RenderArgs args, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth) {
            if (this.renderer.lineString.getValue()) {
                float renderOriginX = this.renderCellX;
                float renderOriginY = this.renderCellY;
                float scale = this.worldScale;
                float r = color.r;
                float g = color.g;
                float b = color.b;
                float a = color.a;
                WorldMapRenderer.vboLines.flush();
                WorldMapGeometry geometry = feature.geometry;
                if (geometry.type == WorldMapGeometry.Type.LineString) {
                    WorldMapPoints points = geometry.points.get(0);
                    if (this.floatArray == null || this.floatArray.length < points.numPoints() * 2) {
                        this.floatArray = new float[points.numPoints() * 2];
                    }

                    for (int j = 0; j < points.numPoints(); j++) {
                        float x1 = points.getX(j);
                        float y1 = points.getY(j);
                        this.floatArray[j * 2] = renderOriginX + x1 * scale;
                        this.floatArray[j * 2 + 1] = renderOriginY + y1 * scale;
                    }

                    GL13.glActiveTexture(33984);
                    GL11.glDisable(3553);
                    GL11.glEnable(3042);
                }
            }
        }

        public void drawLineStringTexture(
            WorldMapStyleLayer.RenderArgs args, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color, float lineWidth, Texture texture
        ) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            if (texture != null && texture.isReady()) {
                if (texture.getID() == -1) {
                    texture.bind();
                }

                WorldMapGeometry geometry = feature.geometry;
                if (geometry.type == WorldMapGeometry.Type.LineString) {
                    WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                    WorldMapRenderer.vboLines.setMode(7);
                    WorldMapRenderer.vboLines.setTextureID(texture.getTextureId());
                    float thick = lineWidth;
                    WorldMapPoints points = geometry.points.get(0);

                    for (int j = 0; j < points.numPoints() - 1; j++) {
                        float x1 = renderOriginX + points.getX(j) * scale;
                        float y1 = renderOriginY + points.getY(j) * scale;
                        float x2 = renderOriginX + points.getX(j + 1) * scale;
                        float y2 = renderOriginY + points.getY(j + 1) * scale;
                        float perpX = y2 - y1;
                        float perpY = -(x2 - x1);
                        Vector2f perp = this.vector2f.set(perpX, perpY);
                        perp.normalize();
                        float px1 = x1 + perp.x * thick / 2.0F;
                        float py1 = y1 + perp.y * thick / 2.0F;
                        float px2 = x2 + perp.x * thick / 2.0F;
                        float py2 = y2 + perp.y * thick / 2.0F;
                        float px3 = x2 - perp.x * thick / 2.0F;
                        float py3 = y2 - perp.y * thick / 2.0F;
                        float px4 = x1 - perp.x * thick / 2.0F;
                        float py4 = y1 - perp.y * thick / 2.0F;
                        float length = Vector2f.length(x2 - x1, y2 - y1);
                        float u1 = 0.0F;
                        float v1 = length / (thick * ((float)texture.getHeight() / texture.getWidth()));
                        float u2 = 0.0F;
                        float v2 = 0.0F;
                        float u3 = 1.0F;
                        float v3 = 0.0F;
                        float u4 = 1.0F;
                        float v4 = length / (thick * ((float)texture.getHeight() / texture.getWidth()));
                        WorldMapRenderer.vboLines
                            .addQuad(
                                px1, py1, 0.0F, v1, px2, py2, 0.0F, 0.0F, px3, py3, 1.0F, 0.0F, px4, py4, 1.0F, v4, 0.0F, color.r, color.g, color.b, color.a
                            );
                    }

                    WorldMapRenderer.vboLines.endRun();
                }
            }
        }

        public void fillPolygon(WorldMapStyleLayer.RenderArgs args, WorldMapFeature feature, WorldMapStyleLayer.RGBAf color) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float scale = this.worldScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.Polygon) {
                boolean USE_WORLD_MAP_VBO = false;
                if (geometry.failedToTriangulate) {
                    if (Core.debug) {
                    }

                    return;
                }

                if (geometry.firstIndex == -1) {
                    if (this.triangulationsThisFrame > 500) {
                        return;
                    }

                    this.triangulationsThisFrame++;
                    double[] delta = feature.properties.containsKey("highway") ? new double[]{1.0, 2.0, 4.0, 8.0, 12.0, 18.0} : null;
                    geometry.triangulate(feature.cell, delta);
                    if (geometry.indexCount <= 0) {
                        geometry.failedToTriangulate = true;
                        return;
                    }
                }

                ShortBuffer indices = geometry.cell.indexBuffer;
                FloatBuffer tris = geometry.cell.triangleBuffer;
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(4);
                double delta = 0.0;
                if (this.zoomF <= 11.5) {
                    delta = 18.0;
                } else if (this.zoomF <= 12.0) {
                    delta = 12.0;
                } else if (this.zoomF <= 12.5) {
                    delta = 8.0;
                } else if (this.zoomF <= 13.0) {
                    delta = 4.0;
                } else if (this.zoomF <= 13.5) {
                    delta = 2.0;
                } else if (this.zoomF <= 14.0) {
                    delta = 1.0;
                }

                WorldMapGeometry.TrianglesPerZoom tpz = delta == 0.0 ? null : geometry.findTriangles(delta);
                if (tpz != null) {
                    for (int j = 0; j < tpz.indexCount; j += 3) {
                        int i1 = indices.get(tpz.firstIndex + j) & '\uffff';
                        int i2 = indices.get(tpz.firstIndex + j + 1) & '\uffff';
                        int i3 = indices.get(tpz.firstIndex + j + 2) & '\uffff';
                        float x1 = tris.get(i1 * 2);
                        float y1 = tris.get(i1 * 2 + 1);
                        float x2 = tris.get(i2 * 2);
                        float y2 = tris.get(i2 * 2 + 1);
                        float x3 = tris.get(i3 * 2);
                        float y3 = tris.get(i3 * 2 + 1);
                        WorldMapRenderer.vboLines.reserve(3);
                        float m = 1.0F;
                        WorldMapRenderer.vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0F, r * 1.0F, g * 1.0F, b * 1.0F, a);
                        WorldMapRenderer.vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0F, r * 1.0F, g * 1.0F, b * 1.0F, a);
                        WorldMapRenderer.vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0F, r * 1.0F, g * 1.0F, b * 1.0F, a);
                    }

                    WorldMapRenderer.vboLines.endRun();
                    return;
                }

                for (int j = 0; j < geometry.indexCount; j += 3) {
                    int i1 = indices.get(geometry.firstIndex + j) & '\uffff';
                    int i2 = indices.get(geometry.firstIndex + j + 1) & '\uffff';
                    int i3 = indices.get(geometry.firstIndex + j + 2) & '\uffff';
                    float x1 = tris.get(i1 * 2);
                    float y1 = tris.get(i1 * 2 + 1);
                    float x2 = tris.get(i2 * 2);
                    float y2 = tris.get(i2 * 2 + 1);
                    float x3 = tris.get(i3 * 2);
                    float y3 = tris.get(i3 * 2 + 1);
                    WorldMapRenderer.vboLines.reserve(3);
                    WorldMapRenderer.vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0F, r, g, b, a);
                    WorldMapRenderer.vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0F, r, g, b, a);
                    WorldMapRenderer.vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0F, r, g, b, a);
                }

                WorldMapRenderer.vboLines.endRun();
            }
        }

        public void fillPolygon(
            WorldMapStyleLayer.RenderArgs args,
            WorldMapFeature feature,
            WorldMapStyleLayer.RGBAf color,
            Texture texture,
            float textureScale,
            WorldMapStyleLayer.TextureScaling scaling
        ) {
            float renderOriginX = this.renderCellX;
            float renderOriginY = this.renderCellY;
            float worldScale = this.worldScale;
            boolean bOnePixelPerSquare = scaling == WorldMapStyleLayer.TextureScaling.IsoGridSquare;
            float textureCoordScale = bOnePixelPerSquare ? 1.0F / textureScale : worldScale / textureScale;
            float r = color.r;
            float g = color.g;
            float b = color.b;
            float a = color.a;
            WorldMapGeometry geometry = feature.geometry;
            if (geometry.type == WorldMapGeometry.Type.Polygon) {
                if (geometry.failedToTriangulate) {
                    return;
                }

                if (geometry.firstIndex == -1) {
                    if (this.triangulationsThisFrame > 500) {
                        return;
                    }

                    this.triangulationsThisFrame++;
                    geometry.triangulate(feature.cell, null);
                    if (geometry.indexCount <= 0) {
                        geometry.failedToTriangulate = true;
                        return;
                    }
                }

                GL11.glEnable(3553);
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                WorldMapRenderer.vboLines.setMode(4);
                WorldMapRenderer.vboLines.setTextureID(texture.getTextureId());
                WorldMapRenderer.vboLines.setMinMagFilters(9728, 9728);
                float minX = args.cellX * 256 + geometry.minX;
                float minY = args.cellY * 256 + geometry.minY;
                float texW = texture.getWidth() * textureScale;
                float texH = texture.getHeight() * textureScale;
                float texWhw = texture.getWidthHW();
                float texHhw = texture.getHeightHW();
                float textureOriginX = PZMath.floor(minX / texW) * texW;
                float textureOriginY = PZMath.floor(minY / texH) * texH;
                ShortBuffer indices = geometry.cell.indexBuffer;
                FloatBuffer tris = geometry.cell.triangleBuffer;

                for (int j = 0; j < geometry.indexCount; j += 3) {
                    int i1 = indices.get(geometry.firstIndex + j) & '\uffff';
                    int i2 = indices.get(geometry.firstIndex + j + 1) & '\uffff';
                    int i3 = indices.get(geometry.firstIndex + j + 2) & '\uffff';
                    float x1 = tris.get(i1 * 2);
                    float y1 = tris.get(i1 * 2 + 1);
                    float x2 = tris.get(i2 * 2);
                    float y2 = tris.get(i2 * 2 + 1);
                    float x3 = tris.get(i3 * 2);
                    float y3 = tris.get(i3 * 2 + 1);
                    float tx1 = (x1 + args.cellX * 256 - textureOriginX) * textureCoordScale;
                    float ty1 = (y1 + args.cellY * 256 - textureOriginY) * textureCoordScale;
                    float tx2 = (x2 + args.cellX * 256 - textureOriginX) * textureCoordScale;
                    float ty2 = (y2 + args.cellY * 256 - textureOriginY) * textureCoordScale;
                    float tx3 = (x3 + args.cellX * 256 - textureOriginX) * textureCoordScale;
                    float ty3 = (y3 + args.cellY * 256 - textureOriginY) * textureCoordScale;
                    x1 = renderOriginX + x1 * worldScale;
                    y1 = renderOriginY + y1 * worldScale;
                    x2 = renderOriginX + x2 * worldScale;
                    y2 = renderOriginY + y2 * worldScale;
                    x3 = renderOriginX + x3 * worldScale;
                    y3 = renderOriginY + y3 * worldScale;
                    float u1 = tx1 / texWhw;
                    float v1 = ty1 / texHhw;
                    float u2 = tx2 / texWhw;
                    float v2 = ty2 / texHhw;
                    float u3 = tx3 / texWhw;
                    float v3 = ty3 / texHhw;
                    WorldMapRenderer.vboLines.reserve(3);
                    WorldMapRenderer.vboLines.addElement(x1, y1, 0.0F, u1, v1, r, g, b, a);
                    WorldMapRenderer.vboLines.addElement(x2, y2, 0.0F, u2, v2, r, g, b, a);
                    WorldMapRenderer.vboLines.addElement(x3, y3, 0.0F, u3, v3, r, g, b, a);
                }

                WorldMapRenderer.vboLines.endRun();
                GL11.glDisable(3553);
            }
        }

        void uploadTrianglesToVBO(WorldMapGeometry geometry) {
            int[] indices1 = new int[2];
            ShortBuffer indices = geometry.cell.indexBuffer;
            FloatBuffer tris = geometry.cell.triangleBuffer;
            int numElements = geometry.indexCount / 3;
            if (numElements > 2340) {
                int doneTris = 0;

                while (numElements > 0) {
                    int numTris = PZMath.min(numElements / 3, 780);
                    WorldMapVBOs.getInstance().reserveVertices(numTris * 3, indices1);
                    if (geometry.vboIndex1 == -1) {
                        geometry.vboIndex1 = indices1[0];
                        geometry.vboIndex2 = indices1[1];
                    } else {
                        geometry.vboIndex3 = indices1[0];
                        geometry.vboIndex4 = indices1[1];
                    }

                    int j = doneTris;

                    for (int nj = doneTris + numTris; j < nj; j++) {
                        int i1 = indices.get(geometry.firstIndex + j) & '\uffff';
                        int i2 = indices.get(geometry.firstIndex + j + 1) & '\uffff';
                        int i3 = indices.get(geometry.firstIndex + j + 2) & '\uffff';
                        float x1 = tris.get(i1 * 2);
                        float y1 = tris.get(i1 * 2 + 1);
                        float x2 = tris.get(i2 * 2);
                        float y2 = tris.get(i2 * 2 + 1);
                        float x3 = tris.get(i3 * 2);
                        float y3 = tris.get(i3 * 2 + 1);
                        WorldMapVBOs.getInstance().addElement(x1, y1, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                        WorldMapVBOs.getInstance().addElement(x2, y2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                        WorldMapVBOs.getInstance().addElement(x3, y3, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                    }

                    doneTris += numTris;
                    numElements -= numTris * 3;
                }
            } else {
                WorldMapVBOs.getInstance().reserveVertices(numElements, indices1);
                geometry.vboIndex1 = indices1[0];
                geometry.vboIndex2 = indices1[1];

                for (int j = 0; j < geometry.indexCount; j += 3) {
                    short i1 = indices.get(geometry.firstIndex + j);
                    short i2 = indices.get(geometry.firstIndex + j + 1);
                    short i3 = indices.get(geometry.firstIndex + j + 2);
                    float x1 = tris.get(i1 * 2);
                    float y1 = tris.get(i1 * 2 + 1);
                    float x2 = tris.get(i2 * 2);
                    float y2 = tris.get(i2 * 2 + 1);
                    float x3 = tris.get(i3 * 2);
                    float y3 = tris.get(i3 * 2 + 1);
                    WorldMapVBOs.getInstance().addElement(x1, y1, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                    WorldMapVBOs.getInstance().addElement(x2, y2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                    WorldMapVBOs.getInstance().addElement(x3, y3, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }

        void outlineTriangles(WorldMapGeometry geometry, float renderOriginX, float renderOriginY, float scale) {
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(1);
            float a = 1.0F;
            float r = 1.0F;
            float b = 0.0F;
            float g = 0.0F;
            ShortBuffer indices = geometry.cell.indexBuffer;
            FloatBuffer tris = geometry.cell.triangleBuffer;

            for (int j = 0; j < geometry.indexCount; j += 3) {
                int i1 = indices.get(geometry.firstIndex + j) & '\uffff';
                int i2 = indices.get(geometry.firstIndex + j + 1) & '\uffff';
                int i3 = indices.get(geometry.firstIndex + j + 2) & '\uffff';
                float x1 = tris.get(i1 * 2);
                float y1 = tris.get(i1 * 2 + 1);
                float x2 = tris.get(i2 * 2);
                float y2 = tris.get(i2 * 2 + 1);
                float x3 = tris.get(i3 * 2);
                float y3 = tris.get(i3 * 2 + 1);
                WorldMapRenderer.vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0F, r, g, b, 1.0F);
                WorldMapRenderer.vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0F, r, g, b, 1.0F);
                WorldMapRenderer.vboLines.addElement(renderOriginX + x2 * scale, renderOriginY + y2 * scale, 0.0F, r, g, b, 1.0F);
                WorldMapRenderer.vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0F, r, g, b, 1.0F);
                WorldMapRenderer.vboLines.addElement(renderOriginX + x3 * scale, renderOriginY + y3 * scale, 0.0F, r, g, b, 1.0F);
                WorldMapRenderer.vboLines.addElement(renderOriginX + x1 * scale, renderOriginY + y1 * scale, 0.0F, r, g, b, 1.0F);
            }

            WorldMapRenderer.vboLines.endRun();
        }

        void outlinePolygon(WorldMapGeometry geometry, float renderOriginX, float renderOriginY, float scale) {
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(1);
            float a = 1.0F;
            float b = 0.8F;
            float g = 0.8F;
            float r = 0.8F;
            WorldMapRenderer.vboLines.setLineWidth(4.0F);

            for (int k = 0; k < geometry.points.size(); k++) {
                WorldMapPoints points = geometry.points.get(k);

                for (int j = 0; j < points.numPoints(); j++) {
                    int p1x = points.getX(j);
                    int p1y = points.getY(j);
                    int p2x = points.getX((j + 1) % points.numPoints());
                    int p2y = points.getY((j + 1) % points.numPoints());
                    WorldMapRenderer.vboLines.addElement(renderOriginX + p1x * scale, renderOriginY + p1y * scale, 0.0F, r, g, b, 1.0F);
                    WorldMapRenderer.vboLines.addElement(renderOriginX + p2x * scale, renderOriginY + p2y * scale, 0.0F, r, g, b, 1.0F);
                }
            }

            WorldMapRenderer.vboLines.endRun();
        }

        public void drawTexture(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2) {
            if (texture != null && texture.isReady()) {
                float worldScale = this.worldScale;
                float x1 = (worldX1 - this.centerWorldX) * worldScale;
                float y1 = (worldY1 - this.centerWorldY) * worldScale;
                float x2 = x1 + (worldX2 - worldX1) * worldScale;
                float y2 = y1 + (worldY2 - worldY1) * worldScale;
                GL11.glEnable(3553);
                GL11.glEnable(3042);
                GL11.glDisable(2929);
                if (texture.getID() == -1) {
                    texture.bind();
                }

                float u0 = texture.getXStart();
                float v0 = texture.getYStart();
                float u1 = texture.getXEnd();
                float v1 = texture.getYEnd();
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                WorldMapRenderer.vboLines.setMode(7);
                WorldMapRenderer.vboLines.setTextureID(texture.getTextureId());
                WorldMapRenderer.vboLines.setMinMagFilters(9728, 9728);
                WorldMapRenderer.vboLines.addQuad(x1, y1, u0, v0, x2, y2, u1, v1, 0.0F, fill.r, fill.g, fill.b, fill.a);
                WorldMapRenderer.vboLines.endRun();
            }
        }

        public void drawTexture(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2, int cellX, int cellY) {
            if (texture != null && texture.isReady()) {
                float worldScale = this.worldScale;
                float x1 = (worldX1 - this.centerWorldX) * worldScale;
                float y1 = (worldY1 - this.centerWorldY) * worldScale;
                float x2 = x1 + (worldX2 - worldX1) * worldScale;
                float y2 = y1 + (worldY2 - worldY1) * worldScale;
                float x1c = PZMath.clamp(x1, (float)cellX, cellX + 256.0F * worldScale);
                float y1c = PZMath.clamp(y1, (float)cellY, cellY + 256.0F * worldScale);
                float x2c = PZMath.clamp(x2, (float)cellX, cellX + 256.0F * worldScale);
                float y2c = PZMath.clamp(y2, (float)cellY, cellY + 256.0F * worldScale);
                if (!(x1c >= x2c) && !(y1c >= y2c)) {
                    float xScale = (float)texture.getWidth() / (worldX2 - worldX1);
                    float yScale = (float)texture.getHeight() / (worldY2 - worldY1);
                    GL11.glEnable(3553);
                    GL11.glEnable(3042);
                    GL11.glDisable(2929);
                    if (texture.getID() == -1) {
                        texture.bind();
                    }

                    float u0 = (x1c - x1) / (texture.getWidthHW() * worldScale) * xScale;
                    float v0 = (y1c - y1) / (texture.getHeightHW() * worldScale) * yScale;
                    float u1 = (x2c - x1) / (texture.getWidthHW() * worldScale) * xScale;
                    float v1 = (y2c - y1) / (texture.getHeightHW() * worldScale) * yScale;
                    WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                    WorldMapRenderer.vboLines.setMode(7);
                    WorldMapRenderer.vboLines.setTextureID(texture.getTextureId());
                    WorldMapRenderer.vboLines.setMinMagFilters(9728, 9728);
                    WorldMapRenderer.vboLines.addQuad(x1c, y1c, u0, v0, x2c, y2c, u1, v1, 0.0F, fill.r, fill.g, fill.b, fill.a);
                    WorldMapRenderer.vboLines.endRun();
                }
            }
        }

        public void drawTextureTiled(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2, int cellX, int cellY) {
            if (texture != null && texture.isReady()) {
                if (cellX * 256 < worldX2 && (cellX + 1) * 256 > worldX1) {
                    if (cellY * 256 < worldY2 && (cellY + 1) * 256 > worldY1) {
                        float worldScale = this.worldScale;
                        int tileW = texture.getWidth();
                        int tileH = texture.getHeight();
                        int x1 = (int)(PZMath.floor(cellX * 256.0F / tileW) * tileW);
                        int y1 = (int)(PZMath.floor(cellY * 256.0F / tileH) * tileH);
                        int x2 = x1 + (int)Math.ceil(((cellX + 1) * 256.0F - x1) / tileW) * tileW;
                        int y2 = y1 + (int)Math.ceil(((cellY + 1) * 256.0F - y1) / tileH) * tileH;
                        float x1c = PZMath.clamp(x1, cellX * 256, (cellX + 1) * 256);
                        float y1c = PZMath.clamp(y1, cellY * 256, (cellY + 1) * 256);
                        float x2c = PZMath.clamp(x2, cellX * 256, (cellX + 1) * 256);
                        float y2c = PZMath.clamp(y2, cellY * 256, (cellY + 1) * 256);
                        x1c = PZMath.clamp(x1c, (float)worldX1, (float)worldX2);
                        y1c = PZMath.clamp(y1c, (float)worldY1, (float)worldY2);
                        x2c = PZMath.clamp(x2c, (float)worldX1, (float)worldX2);
                        y2c = PZMath.clamp(y2c, (float)worldY1, (float)worldY2);
                        float tileX1 = (x1c - worldX1) / tileW;
                        float tileY1 = (y1c - worldY1) / tileH;
                        float tileX2 = (x2c - worldX1) / tileW;
                        float tileY2 = (y2c - worldY1) / tileH;
                        x1c = (x1c - this.centerWorldX) * worldScale;
                        y1c = (y1c - this.centerWorldY) * worldScale;
                        x2c = (x2c - this.centerWorldX) * worldScale;
                        y2c = (y2c - this.centerWorldY) * worldScale;
                        float u0 = tileX1 * texture.xEnd;
                        float v0 = tileY1 * texture.yEnd;
                        float u1 = (int)tileX2 + (tileX2 - (int)tileX2) * texture.xEnd;
                        float v1 = (int)tileY2 + (tileY2 - (int)tileY2) * texture.yEnd;
                        GL11.glEnable(3553);
                        if (texture.getID() == -1) {
                            texture.bind();
                        } else {
                            GL11.glBindTexture(3553, Texture.lastTextureID = texture.getID());
                            GL11.glTexParameteri(3553, 10242, 10497);
                            GL11.glTexParameteri(3553, 10243, 10497);
                        }

                        WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                        WorldMapRenderer.vboLines.setMode(7);
                        WorldMapRenderer.vboLines.setTextureID(texture.getTextureId());
                        WorldMapRenderer.vboLines.setMinMagFilters(9728, 9728);
                        WorldMapRenderer.vboLines.addQuad(x1c, y1c, u0, v0, x2c, y2c, u1, v1, 0.0F, fill.r, fill.g, fill.b, fill.a);
                        WorldMapRenderer.vboLines.endRun();
                        GL11.glDisable(3553);
                    }
                }
            }
        }

        public void drawTextureTiled(Texture texture, WorldMapStyleLayer.RGBAf fill, int worldX1, int worldY1, int worldX2, int worldY2) {
            if (texture != null && texture.isReady()) {
                float worldScale = this.worldScale;
                int tileW = texture.getWidth();
                int tileH = texture.getHeight();
                float cellX = PZMath.floor(worldX1 / 256.0F);
                float cellY = PZMath.floor(worldY1 / 256.0F);
                float cellX2 = PZMath.floor(worldX2 / 256.0F);
                float cellY2 = PZMath.floor(worldY2 / 256.0F);
                int x1 = (int)(PZMath.floor(cellX * 256.0F / tileW) * tileW);
                int y1 = (int)(PZMath.floor(cellY * 256.0F / tileH) * tileH);
                int x2 = x1 + (int)Math.ceil(((cellX2 + 1.0F) * 256.0F - x1) / tileW) * tileW;
                int y2 = y1 + (int)Math.ceil(((cellY2 + 1.0F) * 256.0F - y1) / tileH) * tileH;
                float x1c = PZMath.clamp((float)x1, cellX * 256.0F, (cellX2 + 1.0F) * 256.0F);
                float y1c = PZMath.clamp((float)y1, cellY * 256.0F, (cellY2 + 1.0F) * 256.0F);
                float x2c = PZMath.clamp((float)x2, cellX * 256.0F, (cellX2 + 1.0F) * 256.0F);
                float y2c = PZMath.clamp((float)y2, cellY * 256.0F, (cellY2 + 1.0F) * 256.0F);
                x1c = PZMath.clamp(x1c, (float)worldX1, (float)worldX2);
                y1c = PZMath.clamp(y1c, (float)worldY1, (float)worldY2);
                x2c = PZMath.clamp(x2c, (float)worldX1, (float)worldX2);
                y2c = PZMath.clamp(y2c, (float)worldY1, (float)worldY2);
                float tileX1 = (x1c - worldX1) / tileW;
                float tileY1 = (y1c - worldY1) / tileH;
                float tileX2 = (x2c - worldX1) / tileW;
                float tileY2 = (y2c - worldY1) / tileH;
                x1c = (x1c - this.centerWorldX) * worldScale;
                y1c = (y1c - this.centerWorldY) * worldScale;
                x2c = (x2c - this.centerWorldX) * worldScale;
                y2c = (y2c - this.centerWorldY) * worldScale;
                float u0 = tileX1 * texture.xEnd;
                float v0 = tileY1 * texture.yEnd;
                float u1 = (int)tileX2 + (tileX2 - (int)tileX2) * texture.xEnd;
                float v1 = (int)tileY2 + (tileY2 - (int)tileY2) * texture.yEnd;
                GL11.glEnable(3553);
                if (texture.getID() == -1) {
                    texture.bind();
                } else {
                    GL11.glBindTexture(3553, Texture.lastTextureID = texture.getID());
                    GL11.glTexParameteri(3553, 10242, 10497);
                    GL11.glTexParameteri(3553, 10243, 10497);
                }

                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                WorldMapRenderer.vboLines.setMode(7);
                WorldMapRenderer.vboLines.setTextureID(texture.getTextureId());
                WorldMapRenderer.vboLines.setMinMagFilters(9728, 9728);
                WorldMapRenderer.vboLines.addQuad(x1c, y1c, u0, v0, x2c, y2c, u1, v1, 0.0F, fill.r, fill.g, fill.b, fill.a);
                WorldMapRenderer.vboLines.endRun();
                GL11.glDisable(3553);
            }
        }

        void renderZones() {
            this.zoneSet.clear();

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
                    if (metaCell != null) {
                        metaCell.getZonesUnique(this.zoneSet);
                    }
                }
            }

            this.zones.clear();
            this.zones.addAll(this.zoneSet);
            this.renderZones(this.zones, "Water", 0.0F, 0.0F, 1.0F, 0.25F);
            this.renderZones(this.zones, "WaterNoFish", 0.0F, 0.2F, 1.0F, 0.25F);
            this.renderZones(this.zones, "Forest", 0.0F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "DeepForest", 0.0F, 0.5F, 0.0F, 0.25F);
            this.renderZones(this.zones, "ForagingNav", 0.5F, 0.0F, 1.0F, 0.25F);
            this.renderZones(this.zones, "Vegitation", 1.0F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "TrailerPark", 0.0F, 0.5F, 1.0F, 0.25F);
            this.renderZones(this.zones, "TownZone", 0.0F, 0.5F, 1.0F, 0.25F);
            this.renderZones(this.zones, "Farm", 0.5F, 0.5F, 1.0F, 0.25F);
            this.renderZones(this.zones, "FarmLand", 0.5F, 0.5F, 1.0F, 0.25F);
            this.renderZones(this.zones, "PHForest", 0.1F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "PHMixForest", 0.2F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "PRForest", 0.3F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "FarmMixForest", 0.4F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "FarmForest", 0.5F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "BirchForest", 0.6F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "BirchMixForest", 0.7F, 1.0F, 0.0F, 0.25F);
            this.renderZones(this.zones, "OrganicForest", 0.8F, 1.0F, 0.0F, 0.25F);
        }

        void renderOtherZones() {
            this.zoneSet.clear();

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
                    if (metaCell != null) {
                        metaCell.getZonesUnique(this.zoneSet);
                    }
                }
            }

            HashMap<String, ColorInfo> zoneTypeToColor = new HashMap<>();
            HashMap<String, ArrayList<Zone>> zoneTypeToZoneList = new HashMap<>();

            for (Zone zone : this.zoneSet) {
                if (zone.type != null
                    && !Objects.equals(zone.type, "DeepForest")
                    && !Objects.equals(zone.type, "Farm")
                    && !Objects.equals(zone.type, "FarmLand")
                    && !Objects.equals(zone.type, "Forest")
                    && !Objects.equals(zone.type, "ForagingNav")
                    && !Objects.equals(zone.type, "TownZone")
                    && !Objects.equals(zone.type, "TrailerPark")
                    && !Objects.equals(zone.type, "Vegitation")
                    && !Objects.equals(zone.type, "Foraging_None")
                    && !Objects.equals(zone.type, "PHForest")
                    && !Objects.equals(zone.type, "PHMixForest")
                    && !Objects.equals(zone.type, "PRForest")
                    && !Objects.equals(zone.type, "FarmMixForest")
                    && !Objects.equals(zone.type, "FarmForest")
                    && !Objects.equals(zone.type, "BirchForest")
                    && !Objects.equals(zone.type, "BirchMixForest")
                    && !Objects.equals(zone.type, "OrganicForest")) {
                    if (!zoneTypeToColor.containsKey(zone.type)) {
                        StringBuilder hash = new StringBuilder(Integer.toHexString(zone.type.hashCode()));
                        hash.append("0".repeat(Math.max(0, 6 - hash.length())));
                        float r = Integer.parseInt(hash.substring(0, 2), 16) / 255.0F;
                        float g = Integer.parseInt(hash.substring(2, 4), 16) / 255.0F;
                        float b = Integer.parseInt(hash.substring(4, 6), 16) / 255.0F;
                        ColorInfo colorInfo = new ColorInfo(r, g, b, 1.0F);
                        zoneTypeToColor.put(zone.type, colorInfo);
                    }

                    if (!zoneTypeToZoneList.containsKey(zone.type)) {
                        zoneTypeToZoneList.put(zone.type, new ArrayList<>());
                    }

                    zoneTypeToZoneList.get(zone.type).add(zone);
                }
            }

            for (Entry<String, ArrayList<Zone>> entry : zoneTypeToZoneList.entrySet()) {
                ColorInfo colorInfo = zoneTypeToColor.get(entry.getKey());
                this.renderZones(entry.getValue(), null, colorInfo.r, colorInfo.g, colorInfo.b, 0.05F);
            }
        }

        void renderZombieIntensity() {
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
            float scale = this.worldScale;

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
                    if (metaCell != null) {
                        WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                        WorldMapRenderer.vboLines.setMode(4);

                        for (int x = 0; x < 32; x++) {
                            for (int y = 0; y < 32; y++) {
                                int value = metaCell.info.getZombieIntensity(x + y * 32);
                                WorldMapRenderer.vboLines
                                    .addQuad(
                                        (metaCell.getX() * 256 + x * 8 - this.centerWorldX) * scale,
                                        (metaCell.getY() * 256 + y * 8 - this.centerWorldY) * scale,
                                        (metaCell.getX() * 256 + (x + 1) * 8 - this.centerWorldX) * scale,
                                        (metaCell.getY() * 256 + (y + 1) * 8 - this.centerWorldY) * scale,
                                        0.0F,
                                        value / 255.0F * 40.0F,
                                        value / 255.0F * 40.0F,
                                        value / 255.0F * 40.0F,
                                        0.75F
                                    );
                            }
                        }

                        WorldMapRenderer.vboLines.endRun();
                    }
                }
            }
        }

        void renderZombieVoronoi(boolean cutoff) {
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
            float scale = this.worldScale;

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
                    if (metaCell != null) {
                        List<double[]> values = cutoff
                            ? IsoWorld.instance.getZombieVoronois().stream().map(vx -> vx.evaluateCellCutoff(metaCell.getX(), metaCell.getY())).toList()
                            : IsoWorld.instance.getZombieVoronois().stream().map(vx -> vx.evaluateCellNoise(metaCell.getX(), metaCell.getY())).toList();
                        if (!values.isEmpty()) {
                            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                            WorldMapRenderer.vboLines.setMode(4);

                            for (int x = 0; x < 32; x++) {
                                for (int y = 0; y < 32; y++) {
                                    float squareX = metaCell.getX() * 256 + x * 8;
                                    float squareY = metaCell.getY() * 256 + y * 8;
                                    float value = 1.0F;

                                    for (double[] v : values) {
                                        value *= (float)v[x + y * 32];
                                    }

                                    WorldMapRenderer.vboLines
                                        .addQuad(
                                            (squareX - this.centerWorldX) * scale,
                                            (squareY - this.centerWorldY) * scale,
                                            (squareX + 8.0F - this.centerWorldX) * scale,
                                            (squareY + 8.0F - this.centerWorldY) * scale,
                                            0.0F,
                                            value,
                                            value,
                                            value,
                                            cutoff ? 0.25F : 0.75F
                                        );
                                }
                            }

                            WorldMapRenderer.vboLines.endRun();
                        }
                    }
                }
            }
        }

        void renderAnimalZones() {
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
            this.zoneSet.clear();

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
                    if (metaCell != null) {
                        for (int index = 0; index < metaCell.getAnimalZonesSize(); index++) {
                            this.zoneSet.add(metaCell.getAnimalZone(index));
                        }
                    }
                }
            }

            this.zones.clear();
            this.zones.addAll(this.zoneSet);
            this.renderZones(this.zones, "Animal", 0.0F, 0.5F, 1.0F, 1.0F);
        }

        void renderStoryZones() {
            this.zoneSet.clear();

            for (int i = 0; i < this.rasterizeXy.size() - 1; i += 2) {
                int cellX = this.rasterizeXyInts[i];
                int cellY = this.rasterizeXyInts[i + 1];
                if (this.renderer.visited == null || this.renderer.visited.isCellVisible(cellX, cellY)) {
                    IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
                    if (metaCell != null) {
                        metaCell.getZonesUnique(this.zoneSet);
                    }
                }
            }

            this.zones.clear();
            this.zones.addAll(this.zoneSet);
            this.renderZones(this.zones, "ZoneStory", 1.0F, 0.5F, 0.5F, 1.0F);
        }

        void renderZones(ArrayList<Zone> zones, String zoneType, float r, float g, float b, float a) {
            float scale = this.worldScale;
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(4);
            Iterator var8 = zones.iterator();

            while (true) {
                Zone zone;
                label103:
                while (true) {
                    if (!var8.hasNext()) {
                        WorldMapRenderer.vboLines.endRun();
                        WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                        WorldMapRenderer.vboLines.setMode(1);
                        WorldMapRenderer.vboLines.setLineWidth(2.0F);

                        for (Zone zonex : zones) {
                            if (zoneType == null || zoneType.equals(zonex.type)) {
                                if (zonex.isRectangle()) {
                                    float x1 = (zonex.x - this.centerWorldX) * scale;
                                    float y1 = (zonex.y - this.centerWorldY) * scale;
                                    float x2 = (zonex.x + zonex.w - this.centerWorldX) * scale;
                                    float y2 = (zonex.y + zonex.h - this.centerWorldY) * scale;
                                    WorldMapRenderer.vboLines.addLine(x1, y1, 0.0F, x2, y1, 0.0F, r, g, b, 1.0F);
                                    WorldMapRenderer.vboLines.addLine(x2, y1, 0.0F, x2, y2, 0.0F, r, g, b, 1.0F);
                                    WorldMapRenderer.vboLines.addLine(x2, y2, 0.0F, x1, y2, 0.0F, r, g, b, 1.0F);
                                    WorldMapRenderer.vboLines.addLine(x1, y2, 0.0F, x1, y1, 0.0F, r, g, b, 1.0F);
                                }

                                if (zonex.isPolygon()) {
                                    for (int i = 0; i < zonex.points.size(); i += 2) {
                                        float x1 = (zonex.points.getQuick(i) - this.centerWorldX) * scale;
                                        float y1 = (zonex.points.getQuick(i + 1) - this.centerWorldY) * scale;
                                        float x2 = (zonex.points.getQuick((i + 2) % zonex.points.size()) - this.centerWorldX) * scale;
                                        float y2 = (zonex.points.getQuick((i + 3) % zonex.points.size()) - this.centerWorldY) * scale;
                                        WorldMapRenderer.vboLines.addLine(x1, y1, 0.0F, x2, y2, 0.0F, r, g, b, 1.0F);
                                    }
                                }

                                if (zonex.isPolyline()) {
                                    float[] points = zonex.polylineOutlinePoints;
                                    if (points == null) {
                                        for (int i = 0; i < zonex.points.size() - 2; i += 2) {
                                            float x1 = (zonex.points.getQuick(i) + 0.5F - this.centerWorldX) * scale;
                                            float y1 = (zonex.points.getQuick(i + 1) + 0.5F - this.centerWorldY) * scale;
                                            float x2 = (zonex.points.getQuick((i + 2) % zonex.points.size()) + 0.5F - this.centerWorldX) * scale;
                                            float y2 = (zonex.points.getQuick((i + 3) % zonex.points.size()) + 0.5F - this.centerWorldY) * scale;
                                            WorldMapRenderer.vboLines.addLine(x1, y1, 0.0F, x2, y2, 0.0F, r, g, b, 1.0F);
                                        }
                                    } else {
                                        for (int i = 0; i < points.length; i += 2) {
                                            float x1 = (points[i] - this.centerWorldX) * scale;
                                            float y1 = (points[i + 1] - this.centerWorldY) * scale;
                                            float x2 = (points[(i + 2) % points.length] - this.centerWorldX) * scale;
                                            float y2 = (points[(i + 3) % points.length] - this.centerWorldY) * scale;
                                            WorldMapRenderer.vboLines.addLine(x1, y1, 0.0F, x2, y2, 0.0F, r, g, b, 1.0F);
                                        }
                                    }
                                }
                            }
                        }

                        WorldMapRenderer.vboLines.endRun();
                        return;
                    }

                    zone = (Zone)var8.next();
                    if (zoneType == null || zoneType.equals(zone.type)) {
                        if (zone.isRectangle()) {
                            WorldMapRenderer.vboLines
                                .addQuad(
                                    (zone.x - this.centerWorldX) * scale,
                                    (zone.y - this.centerWorldY) * scale,
                                    (zone.x + zone.w - this.centerWorldX) * scale,
                                    (zone.y + zone.h - this.centerWorldY) * scale,
                                    0.0F,
                                    r,
                                    g,
                                    b,
                                    a
                                );
                        }

                        if (!zone.isPolygon()) {
                            break;
                        }

                        float[] triangles = zone.getPolygonTriangles();
                        if (triangles != null) {
                            int i = 0;

                            while (true) {
                                if (i >= triangles.length) {
                                    break label103;
                                }

                                float x1 = (triangles[i] - this.centerWorldX) * scale;
                                float y1 = (triangles[i + 1] - this.centerWorldY) * scale;
                                float x2 = (triangles[i + 2] - this.centerWorldX) * scale;
                                float y2 = (triangles[i + 3] - this.centerWorldY) * scale;
                                float x3 = (triangles[i + 4] - this.centerWorldX) * scale;
                                float y3 = (triangles[i + 5] - this.centerWorldY) * scale;
                                WorldMapRenderer.vboLines.addTriangle(x1, y1, 0.0F, x2, y2, 0.0F, x3, y3, 0.0F, r, g, b, a);
                                i += 6;
                            }
                        }
                    }
                }

                if (zone.isPolyline()) {
                    float[] triangles = zone.getPolylineOutlineTriangles();
                    if (triangles != null) {
                        for (int i = 0; i < triangles.length; i += 6) {
                            float x1 = (triangles[i] - this.centerWorldX) * scale;
                            float y1 = (triangles[i + 1] - this.centerWorldY) * scale;
                            float x2 = (triangles[i + 2] - this.centerWorldX) * scale;
                            float y2 = (triangles[i + 3] - this.centerWorldY) * scale;
                            float x3 = (triangles[i + 4] - this.centerWorldX) * scale;
                            float y3 = (triangles[i + 5] - this.centerWorldY) * scale;
                            WorldMapRenderer.vboLines.addTriangle(x1, y1, 0.0F, x2, y2, 0.0F, x3, y3, 0.0F, r, g, b, a);
                        }
                    }
                }
            }
        }

        @Override
        public void render() {
            try {
                PZGLUtil.pushAndLoadMatrix(5889, this.projection);
                PZGLUtil.pushAndLoadMatrix(5888, this.modelView);
                DefaultShader.isActive = false;
                ShaderHelper.forgetCurrentlyBound();
                GL20.glUseProgram(0);
                this.renderInternal();
                ShaderHelper.glUseProgramObjectARB(0);
            } catch (Exception var5) {
                ExceptionLogger.logException(var5);
            } finally {
                PZGLUtil.popMatrix(5889);
                PZGLUtil.popMatrix(5888);
            }
        }

        private void renderInternal() {
            float worldScale = this.worldScale;
            int minX = this.worldMap.getMinXInSquares();
            int minY = this.worldMap.getMinYInSquares();
            int maxX = this.worldMap.getMaxXInSquares();
            int maxY = this.worldMap.getMaxYInSquares();
            GL11.glViewport(this.x, Core.height - this.height - this.y, this.width, this.height);
            GLVertexBufferObject.funcs.glBindBuffer(GLVertexBufferObject.funcs.GL_ARRAY_BUFFER(), 0);
            GLVertexBufferObject.funcs.glBindBuffer(GLVertexBufferObject.funcs.GL_ELEMENT_ARRAY_BUFFER(), 0);
            GL11.glPolygonMode(1032, this.renderer.wireframe.getValue() ? 6913 : 6914);
            if (this.renderer.imagePyramid.getValue()) {
                this.renderImagePyramids();
            }

            this.calculateVisibleCells();
            if (this.renderer.features.getValue()) {
                this.filterFeatures();
                this.renderCellFeatures();
            } else {
                this.renderNonCellFeatures();
            }

            this.streetRenderData.render(this.renderer, WorldMapRenderer.vboLines);
            if (this.renderer.getBoolean("Symbols")) {
                this.symbolsRenderData.render(this, false);
            }

            if (this.renderer.otherZones.getValue()) {
                this.renderOtherZones();
            }

            if (this.renderer.forestZones.getValue()) {
                this.renderZones();
            }

            if (this.renderer.zombieIntensity.getValue()) {
                this.renderZombieIntensity();
            }

            if (this.renderer.zombieVoronoi.getValue()) {
                this.renderZombieVoronoi(false);
            }

            if (this.renderer.zombieCutoff.getValue()) {
                this.renderZombieVoronoi(true);
            }

            if (this.renderer.animals.getValue() || this.renderer.animalTracks.getValue()) {
                this.renderAnimalZones();
            }

            if (this.renderer.storyZones.getValue()) {
                this.renderStoryZones();
            }

            if (this.renderer.visibleCells.getValue()) {
                this.renderVisibleCells();
            }

            WorldMapRenderer.vboLines.flush();
            GL13.glActiveTexture(33984);
            GL11.glTexEnvi(8960, 8704, 8448);
            GL11.glPolygonMode(1032, 6914);
            GL11.glEnable(3042);
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            SpriteRenderer.ringBuffer.restoreVbos = true;
            if (this.renderer.visited != null) {
                this.renderer
                    .visited
                    .render(
                        this.renderOriginX - (this.worldMap.getMinXInSquares() - this.renderer.visited.getMinX() * 256) * worldScale,
                        this.renderOriginY - (this.worldMap.getMinYInSquares() - this.renderer.visited.getMinY() * 256) * worldScale,
                        minX / 256,
                        minY / 256,
                        maxX / 256,
                        maxY / 256,
                        worldScale,
                        this.renderer.blurUnvisited.getValue()
                    );
                if (this.renderer.unvisitedGrid.getValue()) {
                    this.renderer
                        .visited
                        .renderGrid(
                            this.renderOriginX - (this.worldMap.getMinXInSquares() - this.renderer.visited.getMinX() * 256) * worldScale,
                            this.renderOriginY - (this.worldMap.getMinYInSquares() - this.renderer.visited.getMinY() * 256) * worldScale,
                            minX / 256,
                            minY / 256,
                            maxX / 256,
                            maxY / 256,
                            worldScale,
                            this.zoomF
                        );
                }
            }

            this.renderPlayers();
            if (this.renderer.cellGrid.getValue()) {
                this.renderCellGrid(minX / 256, minY / 256, maxX / 256, maxY / 256);
            }

            if (Core.debug) {
            }

            this.paintAreasOutsideBounds(minX, minY, maxX, maxY, worldScale);
            if (this.renderer.worldBounds.getValue()) {
                this.renderWorldBounds();
            }

            if (this.renderer.getBoolean("Symbols")) {
                this.symbolsRenderData.render(this, true);
            }

            WorldMapRenderer.vboLines.flush();
            GL11.glViewport(0, 0, Core.width, Core.height);
        }

        private void rasterizeCellsCallback(int _x, int _y) {
            int setId = _x + _y * this.worldMap.getWidthInCells();
            if (!this.rasterizeSet.contains(setId)) {
                for (int y = _y * this.rasterizeMult; y < _y * this.rasterizeMult + this.rasterizeMult; y++) {
                    for (int x = _x * this.rasterizeMult; x < _x * this.rasterizeMult + this.rasterizeMult; x++) {
                        if (x >= this.worldMap.getMinXInCells()
                            && x <= this.worldMap.getMaxXInCells()
                            && y >= this.worldMap.getMinYInCells()
                            && y <= this.worldMap.getMaxYInCells()) {
                            this.rasterizeSet.add(setId);
                            this.rasterizeXy.add(x);
                            this.rasterizeXy.add(y);
                        }
                    }
                }
            }
        }

        private void rasterizeTilesCallback(int _x, int _y) {
            int setId = _x + _y * 1000;
            if (!this.rasterizeSet.contains(setId)) {
                if (!(_x < this.rasterizeMinTileX) && !(_x > this.rasterizeMaxTileX) && !(_y < this.rasterizeMinTileY) && !(_y > this.rasterizeMaxTileY)) {
                    this.rasterizeSet.add(setId);
                    this.rasterizePyramidXy.add(_x);
                    this.rasterizePyramidXy.add(_y);
                }
            }
        }

        private void calculateVisibleCells() {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleCells.getValue();
            int PAD = bDebugRasterize ? 200 : 0;
            float worldScale = this.worldScale;
            if (1.0F / worldScale > 100.0F) {
                this.rasterizeXy.resetQuick();

                for (int y = this.worldMap.getMinYInCells(); y <= this.worldMap.getMaxYInCells(); y++) {
                    for (int x = this.worldMap.getMinXInCells(); x <= this.worldMap.getMaxXInCells(); x++) {
                        this.rasterizeXy.add(x);
                        this.rasterizeXy.add(y);
                    }
                }

                if (this.rasterizeXyInts == null || this.rasterizeXyInts.length < this.rasterizeXy.size()) {
                    this.rasterizeXyInts = new int[this.rasterizeXy.size()];
                }

                this.rasterizeXyInts = this.rasterizeXy.toArray(this.rasterizeXyInts);
            } else {
                float xTL = this.uiToWorldX(PAD + 0.0F, PAD + 0.0F) / 256.0F;
                float yTL = this.uiToWorldY(PAD + 0.0F, PAD + 0.0F) / 256.0F;
                float xTR = this.uiToWorldX(this.getWidth() - PAD, 0.0F + PAD) / 256.0F;
                float yTR = this.uiToWorldY(this.getWidth() - PAD, 0.0F + PAD) / 256.0F;
                float xBR = this.uiToWorldX(this.getWidth() - PAD, this.getHeight() - PAD) / 256.0F;
                float yBR = this.uiToWorldY(this.getWidth() - PAD, this.getHeight() - PAD) / 256.0F;
                float xBL = this.uiToWorldX(0.0F + PAD, this.getHeight() - PAD) / 256.0F;
                float yBL = this.uiToWorldY(0.0F + PAD, this.getHeight() - PAD) / 256.0F;
                int mult = 1;

                while (
                    this.triangleArea(xBL / mult, yBL / mult, xBR / mult, yBR / mult, xTR / mult, yTR / mult)
                            + this.triangleArea(xTR / mult, yTR / mult, xTL / mult, yTL / mult, xBL / mult, yBL / mult)
                        > 80.0F
                ) {
                    mult++;
                }

                this.rasterizeMult = mult;
                this.rasterizeXy.resetQuick();
                this.rasterizeSet.clear();
                this.rasterize.scanTriangle(xBL / mult, yBL / mult, xBR / mult, yBR / mult, xTR / mult, yTR / mult, -1000, 1000, this::rasterizeCellsCallback);
                this.rasterize.scanTriangle(xTR / mult, yTR / mult, xTL / mult, yTL / mult, xBL / mult, yBL / mult, -1000, 1000, this::rasterizeCellsCallback);
                if (this.rasterizeXyInts == null || this.rasterizeXyInts.length < this.rasterizeXy.size()) {
                    this.rasterizeXyInts = new int[this.rasterizeXy.size()];
                }

                this.rasterizeXyInts = this.rasterizeXy.toArray(this.rasterizeXyInts);
            }
        }

        void renderVisibleCells() {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleCells.getValue();
            int PAD = bDebugRasterize ? 200 : 0;
            float worldScale = this.worldScale;
            if (!(1.0F / worldScale > 100.0F)) {
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(4);

                for (int i = 0; i < this.rasterizeXy.size(); i += 2) {
                    int x = this.rasterizeXy.get(i);
                    int y = this.rasterizeXy.get(i + 1);
                    float x0 = this.renderOriginX + (x * 256 - this.worldMap.getMinXInSquares()) * worldScale;
                    float y0 = this.renderOriginY + (y * 256 - this.worldMap.getMinYInSquares()) * worldScale;
                    float x1 = this.renderOriginX + ((x + 1) * 256 - this.worldMap.getMinXInSquares()) * worldScale;
                    float y1 = this.renderOriginY + ((y + 1) * 256 - this.worldMap.getMinYInSquares()) * worldScale;
                    WorldMapRenderer.vboLines.reserve(3);
                    WorldMapRenderer.vboLines.addElement(x0, y0, 0.0F, 0.0F, 1.0F, 0.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x1, y0, 0.0F, 0.0F, 1.0F, 0.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x0, y1, 0.0F, 0.0F, 1.0F, 0.0F, 0.2F);
                    WorldMapRenderer.vboLines.reserve(3);
                    WorldMapRenderer.vboLines.addElement(x1, y0, 0.0F, 0.0F, 0.0F, 1.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x1, y1, 0.0F, 0.0F, 0.0F, 1.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x0, y1, 0.0F, 0.0F, 0.0F, 1.0F, 0.2F);
                }

                WorldMapRenderer.vboLines.endRun();
                float xTL = this.uiToWorldX(PAD + 0.0F, PAD + 0.0F) / 256.0F;
                float yTL = this.uiToWorldY(PAD + 0.0F, PAD + 0.0F) / 256.0F;
                float xTR = this.uiToWorldX(this.getWidth() - PAD, 0.0F + PAD) / 256.0F;
                float yTR = this.uiToWorldY(this.getWidth() - PAD, 0.0F + PAD) / 256.0F;
                float xBR = this.uiToWorldX(this.getWidth() - PAD, this.getHeight() - PAD) / 256.0F;
                float yBR = this.uiToWorldY(this.getWidth() - PAD, this.getHeight() - PAD) / 256.0F;
                float xBL = this.uiToWorldX(0.0F + PAD, this.getHeight() - PAD) / 256.0F;
                float yBL = this.uiToWorldY(0.0F + PAD, this.getHeight() - PAD) / 256.0F;
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(1);
                WorldMapRenderer.vboLines.setLineWidth(4.0F);
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xBL * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBL * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xBR * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBR * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xBR * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBR * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xTR * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTR * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xTR * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTR * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xBL * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBL * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        0.5F,
                        0.5F,
                        0.5F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xTR * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTR * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xTL * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTL * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xTL * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTL * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xBL * 256.0F - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBL * 256.0F - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines.endRun();
            }
        }

        double getZoomAdjustedForPyramidScale(float zoomF, WorldMapImages images) {
            double mppaz = MapProjection.metersPerPixelAtZoom(zoomF, this.getHeight() * images.getResolution() * 2.0F);
            return MapProjection.zoomAtMetersPerPixel(mppaz, this.getHeight());
        }

        void calcVisiblePyramidTiles(WorldMapImages images, int ptz) {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleTiles.getValue();
            int PAD = bDebugRasterize ? PZMath.min(this.width / 20, 200) : 0;
            float worldScale = this.worldScale;
            int tileSize = 256;
            float metersPerTile = (float)images.getPyramid().calculateMetersPerTile(ptz);
            int tileOriginWorldX = images.getMinX();
            int tileOriginWorldY = images.getMinY();
            float xTL = (this.uiToWorldX(PAD + 0.0F, PAD + 0.0F) - tileOriginWorldX) / metersPerTile;
            float yTL = (this.uiToWorldY(PAD + 0.0F, PAD + 0.0F) - tileOriginWorldY) / metersPerTile;
            float xTR = (this.uiToWorldX(this.getWidth() - PAD, 0.0F + PAD) - tileOriginWorldX) / metersPerTile;
            float yTR = (this.uiToWorldY(this.getWidth() - PAD, 0.0F + PAD) - tileOriginWorldY) / metersPerTile;
            float xBR = (this.uiToWorldX(this.getWidth() - PAD, this.getHeight() - PAD) - tileOriginWorldX) / metersPerTile;
            float yBR = (this.uiToWorldY(this.getWidth() - PAD, this.getHeight() - PAD) - tileOriginWorldY) / metersPerTile;
            float xBL = (this.uiToWorldX(0.0F + PAD, this.getHeight() - PAD) - tileOriginWorldX) / metersPerTile;
            float yBL = (this.uiToWorldY(0.0F + PAD, this.getHeight() - PAD) - tileOriginWorldY) / metersPerTile;
            this.rasterizePyramidXy.resetQuick();
            this.rasterizeSet.clear();
            this.rasterizeMinTileX = (int)((this.worldMap.getMinXInSquares() - images.getMinX()) / metersPerTile);
            this.rasterizeMinTileY = (int)((this.worldMap.getMinYInSquares() - images.getMinY()) / metersPerTile);
            this.rasterizeMaxTileX = (this.worldMap.getMaxXInSquares() - images.getMinX()) / metersPerTile;
            this.rasterizeMaxTileY = (this.worldMap.getMaxYInSquares() - images.getMinY()) / metersPerTile;
            this.rasterize.scanTriangle(xBL, yBL, xBR, yBR, xTR, yTR, -1000, 1000, this::rasterizeTilesCallback);
            this.rasterize.scanTriangle(xTR, yTR, xTL, yTL, xBL, yBL, -1000, 1000, this::rasterizeTilesCallback);
            this.rasterizeTileBounds[0] = Integer.MAX_VALUE;
            this.rasterizeTileBounds[1] = Integer.MAX_VALUE;
            this.rasterizeTileBounds[2] = Integer.MIN_VALUE;
            this.rasterizeTileBounds[3] = Integer.MIN_VALUE;

            for (int i = 0; i < this.rasterizePyramidXy.size() - 1; i += 2) {
                int ptx = this.rasterizePyramidXy.getQuick(i);
                int pty = this.rasterizePyramidXy.getQuick(i + 1);
                this.rasterizeTileBounds[0] = PZMath.min(this.rasterizeTileBounds[0], ptx);
                this.rasterizeTileBounds[1] = PZMath.min(this.rasterizeTileBounds[1], pty);
                this.rasterizeTileBounds[2] = PZMath.max(this.rasterizeTileBounds[2], ptx);
                this.rasterizeTileBounds[3] = PZMath.max(this.rasterizeTileBounds[3], pty);
            }
        }

        void renderVisiblePyramidTiles(WorldMapImages images, int ptz) {
            boolean bDebugRasterize = Core.debug && this.renderer.visibleTiles.getValue();
            int PAD = bDebugRasterize ? PZMath.min(this.width / 20, 200) : 0;
            float worldScale = this.worldScale;
            float metersPerTile = (float)images.getPyramid().calculateMetersPerTile(ptz);
            int tileOriginWorldX = images.getMinX();
            int tileOriginWorldY = images.getMinY();
            float xTL = (this.uiToWorldX(PAD + 0.0F, PAD + 0.0F) - tileOriginWorldX) / metersPerTile;
            float yTL = (this.uiToWorldY(PAD + 0.0F, PAD + 0.0F) - tileOriginWorldY) / metersPerTile;
            float xTR = (this.uiToWorldX(this.getWidth() - PAD, 0.0F + PAD) - tileOriginWorldX) / metersPerTile;
            float yTR = (this.uiToWorldY(this.getWidth() - PAD, 0.0F + PAD) - tileOriginWorldY) / metersPerTile;
            float xBR = (this.uiToWorldX(this.getWidth() - PAD, this.getHeight() - PAD) - tileOriginWorldX) / metersPerTile;
            float yBR = (this.uiToWorldY(this.getWidth() - PAD, this.getHeight() - PAD) - tileOriginWorldY) / metersPerTile;
            float xBL = (this.uiToWorldX(0.0F + PAD, this.getHeight() - PAD) - tileOriginWorldX) / metersPerTile;
            float yBL = (this.uiToWorldY(0.0F + PAD, this.getHeight() - PAD) - tileOriginWorldY) / metersPerTile;
            if (bDebugRasterize) {
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(1);
                WorldMapRenderer.vboLines.setLineWidth(4.0F);
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xBL * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBL * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xBR * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBR * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xBR * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBR * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xTR * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTR * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xTR * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTR * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xBL * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBL * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        0.5F,
                        0.5F,
                        0.5F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xTR * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTR * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xTL * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTL * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines
                    .addLine(
                        this.renderOriginX + (xTL * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yTL * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        this.renderOriginX + (xBL * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale,
                        this.renderOriginY + (yBL * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F
                    );
                WorldMapRenderer.vboLines.endRun();
            }

            if (bDebugRasterize) {
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(4);

                for (int i = 0; i < this.rasterizePyramidXy.size(); i += 2) {
                    int x = this.rasterizePyramidXy.get(i);
                    int y = this.rasterizePyramidXy.get(i + 1);
                    float x0 = this.renderOriginX + (x * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale;
                    float y0 = this.renderOriginY + (y * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale;
                    float x1 = this.renderOriginX + ((x + 1) * metersPerTile - this.worldMap.getMinXInSquares()) * worldScale;
                    float y1 = this.renderOriginY + ((y + 1) * metersPerTile - this.worldMap.getMinYInSquares()) * worldScale;
                    WorldMapRenderer.vboLines.addElement(x0, y0, 0.0F, 0.0F, 1.0F, 0.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x1, y0, 0.0F, 0.0F, 1.0F, 0.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x0, y1, 0.0F, 0.0F, 1.0F, 0.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x1, y0, 0.0F, 0.0F, 0.0F, 1.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x1, y1, 0.0F, 0.0F, 0.0F, 1.0F, 0.2F);
                    WorldMapRenderer.vboLines.addElement(x0, y1, 0.0F, 0.0F, 0.0F, 1.0F, 0.2F);
                }

                WorldMapRenderer.vboLines.endRun();
            }
        }

        boolean hasAnyImagePyramidStyleLayers() {
            for (int i = 0; i < this.style.getLayerCount(); i++) {
                WorldMapStyleLayer styleLayer = this.style.getLayerByIndex(i);
                if (styleLayer instanceof WorldMapPyramidStyleLayer) {
                    return true;
                }
            }

            return false;
        }

        void renderImagePyramids() {
            if (this.hasAnyImagePyramidStyleLayers()) {
                for (int i = 0; i < this.worldMap.getImagesCount(); i++) {
                    WorldMapImages images = this.worldMap.getImagesByIndex(i);
                    if (this.findPyramidStyleLayer(images) != null) {
                        this.calculateVisibleCells();
                        int ptz = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
                        images.getPyramid().calculateRequiredTilesForCells(this.rasterizeXy, ptz);
                    }
                }
            } else {
                for (int ix = this.worldMap.getImagesCount() - 1; ix >= 0; ix--) {
                    WorldMapImages images = this.worldMap.getImagesByIndex(ix);
                    this.renderImagePyramid(images);
                    GL11.glDisable(3553);
                }
            }
        }

        WorldMapPyramidStyleLayer findPyramidStyleLayer(WorldMapImages images) {
            this.renderArgs.renderer = this.renderer;
            this.renderArgs.drawer = this;
            this.renderArgs.cellX = Integer.MIN_VALUE;
            this.renderArgs.cellY = Integer.MIN_VALUE;
            WorldMapPyramidStyleLayer pyramidStyleLayer = null;

            for (int i = 0; i < this.style.getLayerCount(); i++) {
                if (this.style.getLayerByIndex(i) instanceof WorldMapPyramidStyleLayer layer
                    && images.getAbsolutePath().endsWith(File.separator + layer.fileName)) {
                    pyramidStyleLayer = layer;
                    if (layer.isVisible(this.renderArgs)) {
                        return layer;
                    }
                }
            }

            return pyramidStyleLayer;
        }

        public void renderImagePyramid(WorldMapImages images) {
            if (this.renderer.imagePyramid.getValue()) {
                ImagePyramid pyramid = images.getPyramid();
                float worldScale = this.worldScale;
                int ptz1 = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
                double metersPerTile = pyramid.calculateMetersPerTile(ptz1);
                this.calcVisiblePyramidTiles(images, ptz1);
                pyramid.calculateRequiredTiles(this.rasterizePyramidXy, ptz1);
                int tileSpanX = this.rasterizeTileBounds[2] - this.rasterizeTileBounds[0] + 1;
                int tileSpanY = this.rasterizeTileBounds[3] - this.rasterizeTileBounds[1] + 1;
                if (this.renderedTiles == null || this.renderedTiles.length < tileSpanX * tileSpanY) {
                    this.renderedTiles = new byte[tileSpanX * tileSpanY];
                }

                Arrays.fill(this.renderedTiles, (byte)0);
                GL11.glEnable(3553);
                GL11.glEnable(3042);
                int clipX1 = PZMath.clamp(images.getMinX(), this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares());
                int clipY1 = PZMath.clamp(images.getMinY(), this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares());
                int clipX2 = PZMath.clamp(images.getMaxX(), this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares() + 1);
                int clipY2 = PZMath.clamp(images.getMaxY(), this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares() + 1);
                WorldMapPyramidStyleLayer pyramidStyleLayer = this.findPyramidStyleLayer(images);
                float r = 1.0F;
                float g = 1.0F;
                float b = 1.0F;
                float a = 1.0F;
                if (pyramidStyleLayer != null) {
                    if (!pyramidStyleLayer.isVisible(this.renderArgs)) {
                        return;
                    }

                    WorldMapStyleLayer.RGBAf fill = pyramidStyleLayer.getFill(this.renderArgs);
                    r = fill.r;
                    g = fill.g;
                    b = fill.b;
                    a = fill.a;
                    WorldMapStyleLayer.RGBAf.s_pool.release(fill);
                }

                for (int i = 0; i < this.rasterizePyramidXy.size() - 1; i += 2) {
                    int ptx = this.rasterizePyramidXy.getQuick(i);
                    int pty = this.rasterizePyramidXy.getQuick(i + 1);
                    int lx = ptx - this.rasterizeTileBounds[0];
                    int ly = pty - this.rasterizeTileBounds[1];
                    if (pyramid.isValidTile(ptx, pty, ptz1)) {
                        this.renderedTiles[lx + ly * tileSpanX] = (byte)(this.renderedTiles[lx + ly * tileSpanX] | 1);
                        ImagePyramid.PyramidTexture pyramidTexture = pyramid.getTexture(ptx, pty, ptz1);
                        if (pyramidTexture != null && pyramidTexture.isReady()) {
                            TextureID textureID = pyramidTexture.getTextureID();
                            if (textureID != null && textureID.isReady()) {
                                double worldX1 = images.getMinX() + ptx * metersPerTile;
                                double worldY1 = images.getMinY() + pty * metersPerTile;
                                double worldX2 = worldX1 + metersPerTile;
                                double worldY2 = worldY1 + metersPerTile;
                                double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                                double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                                double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                                double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                                double x1 = (worldX1c - this.centerWorldX) * worldScale;
                                double y1 = (worldY1c - this.centerWorldY) * worldScale;
                                double x2 = (worldX2c - this.centerWorldX) * worldScale;
                                double y2 = (worldY1c - this.centerWorldY) * worldScale;
                                double x3 = (worldX2c - this.centerWorldX) * worldScale;
                                double y3 = (worldY2c - this.centerWorldY) * worldScale;
                                double x4 = (worldX1c - this.centerWorldX) * worldScale;
                                double y4 = (worldY2c - this.centerWorldY) * worldScale;
                                double u1 = (worldX1c - worldX1) / metersPerTile;
                                double v1 = (worldY1c - worldY1) / metersPerTile;
                                double u2 = (worldX2c - worldX1) / metersPerTile;
                                double v2 = (worldY1c - worldY1) / metersPerTile;
                                double u3 = (worldX2c - worldX1) / metersPerTile;
                                double v3 = (worldY2c - worldY1) / metersPerTile;
                                double u4 = (worldX1c - worldX1) / metersPerTile;
                                double v4 = (worldY2c - worldY1) / metersPerTile;
                                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                                WorldMapRenderer.vboLines.setMode(7);
                                WorldMapRenderer.vboLines.setTextureID(textureID);
                                WorldMapRenderer.vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                                WorldMapRenderer.vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                                WorldMapRenderer.vboLines
                                    .addQuad(
                                        (float)x1,
                                        (float)y1,
                                        0.0F,
                                        (float)u1,
                                        (float)v1,
                                        (float)x2,
                                        (float)y2,
                                        0.0F,
                                        (float)u2,
                                        (float)v2,
                                        (float)x3,
                                        (float)y3,
                                        0.0F,
                                        (float)u3,
                                        (float)v3,
                                        (float)x4,
                                        (float)y4,
                                        0.0F,
                                        (float)u4,
                                        (float)v4,
                                        r,
                                        g,
                                        b,
                                        a
                                    );
                                WorldMapRenderer.vboLines.endRun();
                                if (this.renderer.tileGrid.getValue()) {
                                    WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                                    WorldMapRenderer.vboLines.setMode(1);
                                    WorldMapRenderer.vboLines.setLineWidth(2.0F);
                                    WorldMapRenderer.vboLines
                                        .addRectOutline(
                                            (float)(worldX1 - this.centerWorldX) * worldScale,
                                            (float)(worldY1 - this.centerWorldY) * worldScale,
                                            (float)(worldX2 - this.centerWorldX) * worldScale,
                                            (float)(worldY2 - this.centerWorldY) * worldScale,
                                            0.0F,
                                            1.0F,
                                            0.0F,
                                            0.0F,
                                            1.0F
                                        );
                                    WorldMapRenderer.vboLines.endRun();
                                }

                                this.renderedTiles[lx + ly * tileSpanX] = (byte)(this.renderedTiles[lx + ly * tileSpanX] | 2);
                            }
                        } else if (pyramidTexture != null) {
                            boolean textureID = true;
                        }
                    }
                }

                int ptz = ptz1;

                for (int ly = 0; ly < tileSpanY; ly++) {
                    for (int lx = 0; lx < tileSpanX; lx++) {
                        if ((this.renderedTiles[lx + ly * tileSpanX] & 1) != 0 && (this.renderedTiles[lx + ly * tileSpanX] & 2) == 0) {
                            int ptx = this.rasterizeTileBounds[0] + lx;
                            int pty = this.rasterizeTileBounds[1] + ly;
                            ImagePyramid.PyramidTexture pyramidTexture = pyramid.getLowerResTexture(ptx, pty, ptz);
                            if (pyramidTexture != null && pyramidTexture.isReady()) {
                                TextureID textureID = pyramidTexture.getTextureID();
                                if (textureID != null && textureID.isReady()) {
                                    int ptx1 = pyramidTexture.x;
                                    int pty1 = pyramidTexture.y;
                                    ptz1 = pyramidTexture.z;
                                    double metersPerTile1 = pyramid.calculateMetersPerTile(ptz1);
                                    double worldX1 = images.getMinX() + ptx1 * metersPerTile1;
                                    double worldY1 = images.getMinY() + pty1 * metersPerTile1;
                                    double worldX2 = worldX1 + metersPerTile1;
                                    double worldY2 = worldY1 + metersPerTile1;
                                    double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                                    double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                                    double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                                    double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                                    double hx1 = images.getMinX() + ptx * metersPerTile;
                                    double hy1 = images.getMinY() + pty * metersPerTile;
                                    double hx2 = hx1 + metersPerTile;
                                    double hy2 = hy1 + metersPerTile;
                                    worldX1c = PZMath.clamp(worldX1c, hx1, hx2);
                                    worldY1c = PZMath.clamp(worldY1c, hy1, hy2);
                                    worldX2c = PZMath.clamp(worldX2c, hx1, hx2);
                                    worldY2c = PZMath.clamp(worldY2c, hy1, hy2);
                                    double x1 = (worldX1c - this.centerWorldX) * worldScale;
                                    double y1 = (worldY1c - this.centerWorldY) * worldScale;
                                    double x2 = (worldX2c - this.centerWorldX) * worldScale;
                                    double y2 = (worldY2c - this.centerWorldY) * worldScale;
                                    double u1 = (worldX1c - worldX1) / metersPerTile1;
                                    double v1 = (worldY1c - worldY1) / metersPerTile1;
                                    double u2 = (worldX2c - worldX1) / metersPerTile1;
                                    double v2 = (worldY2c - worldY1) / metersPerTile1;
                                    WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                                    WorldMapRenderer.vboLines.setMode(7);
                                    WorldMapRenderer.vboLines.setTextureID(textureID);
                                    WorldMapRenderer.vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                                    WorldMapRenderer.vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                                    float alpha = (float)(Math.sin(System.currentTimeMillis() / 100L) + 1.0);
                                    alpha = 1.0F;
                                    WorldMapRenderer.vboLines
                                        .addQuad(
                                            (float)x1, (float)y1, (float)u1, (float)v1, (float)x2, (float)y2, (float)u2, (float)v2, 0.0F, r, g, b, a * alpha
                                        );
                                    WorldMapRenderer.vboLines.endRun();
                                    if (this.renderer.tileGrid.getValue()) {
                                        WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                                        WorldMapRenderer.vboLines.setMode(1);
                                        WorldMapRenderer.vboLines.setLineWidth(2.0F);
                                        WorldMapRenderer.vboLines
                                            .addRectOutline(
                                                (float)(worldX1 - this.centerWorldX) * worldScale,
                                                (float)(worldY1 - this.centerWorldY) * worldScale,
                                                (float)(worldX2 - this.centerWorldX) * worldScale,
                                                (float)(worldY2 - this.centerWorldY) * worldScale,
                                                0.0F,
                                                1.0F,
                                                0.0F,
                                                0.0F,
                                                1.0F * alpha
                                            );
                                        WorldMapRenderer.vboLines.endRun();
                                    }

                                    if (Core.debug) {
                                    }
                                } else {
                                    int dbg = 1;
                                }
                            } else if (pyramidTexture != null) {
                                boolean var82 = true;
                            }
                        }
                    }
                }

                if (Core.debug && this.renderer.visibleTiles.getValue()) {
                    this.renderVisiblePyramidTiles(images, ptz);
                }
            }
        }

        public void drawImagePyramid(int cellX, int cellY, String fileName, WorldMapStyleLayer.RGBAf fill) {
            for (int i = 0; i < this.worldMap.getImagesCount(); i++) {
                WorldMapImages images = this.worldMap.getImagesByIndex(i);
                if (images.getAbsolutePath().endsWith(File.separator + fileName)) {
                    this.drawImagePyramid(cellX, cellY, images, fill);
                }
            }
        }

        public void drawImagePyramid(int cellX, int cellY, WorldMapImages images, WorldMapStyleLayer.RGBAf fill) {
            if (this.renderer.imagePyramid.getValue()) {
                ImagePyramid pyramid = images.getPyramid();
                int ptz1 = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
                if (pyramid.calculateTilesCoveringCell(cellX, cellY, ptz1, s_tilesCoveringCell)) {
                    int clipX1 = PZMath.clamp(images.getMinX(), cellX * 256, (cellX + 1) * 256);
                    int clipY1 = PZMath.clamp(images.getMinY(), cellY * 256, (cellY + 1) * 256);
                    int clipX2 = PZMath.clamp(images.getMaxX(), cellX * 256, (cellX + 1) * 256);
                    int clipY2 = PZMath.clamp(images.getMaxY(), cellY * 256, (cellY + 1) * 256);
                    if (clipX1 != clipX2 && clipY1 != clipY2) {
                        float worldScale = this.worldScale;
                        double metersPerTile = pyramid.calculateMetersPerTile(ptz1);
                        clipX1 = PZMath.clamp(clipX1, this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares());
                        clipY1 = PZMath.clamp(clipY1, this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares());
                        clipX2 = PZMath.clamp(clipX2, this.worldMap.getMinXInSquares(), this.worldMap.getMaxXInSquares() + 1);
                        clipY2 = PZMath.clamp(clipY2, this.worldMap.getMinYInSquares(), this.worldMap.getMaxYInSquares() + 1);
                        int tileMinX = s_tilesCoveringCell[0];
                        int tileMinY = s_tilesCoveringCell[1];
                        int tileMaxX = s_tilesCoveringCell[2];
                        int tileMaxY = s_tilesCoveringCell[3];
                        int tileSpanX = tileMaxX - tileMinX + 1;
                        int tileSpanY = tileMaxY - tileMinY + 1;
                        if (this.renderedTiles == null || this.renderedTiles.length < tileSpanX * tileSpanY) {
                            this.renderedTiles = new byte[tileSpanX * tileSpanY];
                        }

                        Arrays.fill(this.renderedTiles, (byte)0);
                        boolean bGLinit = false;

                        for (int pty = tileMinY; pty <= tileMaxY; pty++) {
                            for (int ptx = tileMinX; ptx <= tileMaxX; ptx++) {
                                if (pyramid.isValidTile(ptx, pty, ptz1)) {
                                    this.renderedTiles[ptx - tileMinX + (pty - tileMinY) * tileSpanX] = (byte)(
                                        this.renderedTiles[ptx - tileMinX + (pty - tileMinY) * tileSpanX] | 1
                                    );
                                    ImagePyramid.PyramidTexture pyramidTexture = pyramid.getTexture(ptx, pty, ptz1);
                                    if (pyramidTexture != null && pyramidTexture.isReady()) {
                                        TextureID textureID = pyramidTexture.getTextureID();
                                        if (textureID != null && textureID.isReady()) {
                                            double worldX1 = images.getMinX() + ptx * metersPerTile;
                                            double worldY1 = images.getMinY() + pty * metersPerTile;
                                            double worldX2 = worldX1 + metersPerTile;
                                            double worldY2 = worldY1 + metersPerTile;
                                            double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                                            double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                                            double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                                            double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                                            if (!(worldX2c <= worldX1c) && !(worldY2c <= worldY1c)) {
                                                double x1 = (worldX1c - this.centerWorldX) * worldScale;
                                                double y1 = (worldY1c - this.centerWorldY) * worldScale;
                                                double x2 = (worldX2c - this.centerWorldX) * worldScale;
                                                double y2 = (worldY1c - this.centerWorldY) * worldScale;
                                                double x3 = (worldX2c - this.centerWorldX) * worldScale;
                                                double y3 = (worldY2c - this.centerWorldY) * worldScale;
                                                double x4 = (worldX1c - this.centerWorldX) * worldScale;
                                                double y4 = (worldY2c - this.centerWorldY) * worldScale;
                                                double u1 = (worldX1c - worldX1) / metersPerTile;
                                                double v1 = (worldY1c - worldY1) / metersPerTile;
                                                double u2 = (worldX2c - worldX1) / metersPerTile;
                                                double v2 = (worldY1c - worldY1) / metersPerTile;
                                                double u3 = (worldX2c - worldX1) / metersPerTile;
                                                double v3 = (worldY2c - worldY1) / metersPerTile;
                                                double u4 = (worldX1c - worldX1) / metersPerTile;
                                                double v4 = (worldY2c - worldY1) / metersPerTile;
                                                if (!bGLinit) {
                                                    bGLinit = true;
                                                    GL11.glEnable(3553);
                                                    GL11.glEnable(3042);
                                                }

                                                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                                                WorldMapRenderer.vboLines.setMode(7);
                                                WorldMapRenderer.vboLines.setTextureID(textureID);
                                                WorldMapRenderer.vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                                                WorldMapRenderer.vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                                                WorldMapRenderer.vboLines
                                                    .addQuad(
                                                        (float)x1,
                                                        (float)y1,
                                                        0.0F,
                                                        (float)u1,
                                                        (float)v1,
                                                        (float)x2,
                                                        (float)y2,
                                                        0.0F,
                                                        (float)u2,
                                                        (float)v2,
                                                        (float)x3,
                                                        (float)y3,
                                                        0.0F,
                                                        (float)u3,
                                                        (float)v3,
                                                        (float)x4,
                                                        (float)y4,
                                                        0.0F,
                                                        (float)u4,
                                                        (float)v4,
                                                        fill.r,
                                                        fill.g,
                                                        fill.b,
                                                        fill.a
                                                    );
                                                WorldMapRenderer.vboLines.endRun();
                                                if (this.renderer.tileGrid.getValue()) {
                                                    WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                                                    WorldMapRenderer.vboLines.setMode(1);
                                                    WorldMapRenderer.vboLines.setLineWidth(2.0F);
                                                    WorldMapRenderer.vboLines
                                                        .addRectOutline(
                                                            (float)(worldX1 - this.centerWorldX) * worldScale,
                                                            (float)(worldY1 - this.centerWorldY) * worldScale,
                                                            (float)(worldX2 - this.centerWorldX) * worldScale,
                                                            (float)(worldY2 - this.centerWorldY) * worldScale,
                                                            0.0F,
                                                            1.0F,
                                                            0.0F,
                                                            0.0F,
                                                            1.0F
                                                        );
                                                    WorldMapRenderer.vboLines.endRun();
                                                }

                                                this.renderedTiles[ptx - tileMinX + (pty - tileMinY) * tileSpanX] = (byte)(
                                                    this.renderedTiles[ptx - tileMinX + (pty - tileMinY) * tileSpanX] | 2
                                                );
                                            } else {
                                                this.renderedTiles[ptx - tileMinX + (pty - tileMinY) * tileSpanX] = (byte)(
                                                    this.renderedTiles[ptx - tileMinX + (pty - tileMinY) * tileSpanX] | 2
                                                );
                                            }
                                        }
                                    } else if (pyramidTexture != null) {
                                        boolean ptxx = true;
                                    }
                                }
                            }
                        }

                        int ptz = ptz1;

                        for (int ly = 0; ly < tileSpanY; ly++) {
                            for (int lx = 0; lx < tileSpanX; lx++) {
                                if ((this.renderedTiles[lx + ly * tileSpanX] & 1) != 0 && (this.renderedTiles[lx + ly * tileSpanX] & 2) == 0) {
                                    int ptxx = tileMinX + lx;
                                    int pty = tileMinY + ly;
                                    ImagePyramid.PyramidTexture pyramidTexture = pyramid.getLowerResTexture(ptxx, pty, ptz);
                                    if (pyramidTexture != null && pyramidTexture.isReady()) {
                                        TextureID textureID = pyramidTexture.getTextureID();
                                        if (textureID != null && textureID.isReady()) {
                                            int ptx1 = pyramidTexture.x;
                                            int pty1 = pyramidTexture.y;
                                            ptz1 = pyramidTexture.z;
                                            double metersPerTile1 = pyramid.calculateMetersPerTile(ptz1);
                                            double worldX1 = images.getMinX() + ptx1 * metersPerTile1;
                                            double worldY1 = images.getMinY() + pty1 * metersPerTile1;
                                            double worldX2 = worldX1 + metersPerTile1;
                                            double worldY2 = worldY1 + metersPerTile1;
                                            double worldX1c = PZMath.clamp(worldX1, (double)clipX1, (double)clipX2);
                                            double worldY1c = PZMath.clamp(worldY1, (double)clipY1, (double)clipY2);
                                            double worldX2c = PZMath.clamp(worldX2, (double)clipX1, (double)clipX2);
                                            double worldY2c = PZMath.clamp(worldY2, (double)clipY1, (double)clipY2);
                                            double hx1 = images.getMinX() + ptxx * metersPerTile;
                                            double hy1 = images.getMinY() + pty * metersPerTile;
                                            double hx2 = hx1 + metersPerTile;
                                            double hy2 = hy1 + metersPerTile;
                                            worldX1c = PZMath.clamp(worldX1c, hx1, hx2);
                                            worldY1c = PZMath.clamp(worldY1c, hy1, hy2);
                                            worldX2c = PZMath.clamp(worldX2c, hx1, hx2);
                                            worldY2c = PZMath.clamp(worldY2c, hy1, hy2);
                                            double x1x = (worldX1c - this.centerWorldX) * worldScale;
                                            double y1x = (worldY1c - this.centerWorldY) * worldScale;
                                            double x2x = (worldX2c - this.centerWorldX) * worldScale;
                                            double y2x = (worldY2c - this.centerWorldY) * worldScale;
                                            double u1x = (worldX1c - worldX1) / metersPerTile1;
                                            double v1x = (worldY1c - worldY1) / metersPerTile1;
                                            double u2x = (worldX2c - worldX1) / metersPerTile1;
                                            double v2x = (worldY2c - worldY1) / metersPerTile1;
                                            if (!bGLinit) {
                                                bGLinit = true;
                                                GL11.glEnable(3553);
                                                GL11.glEnable(3042);
                                            }

                                            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColorUv);
                                            WorldMapRenderer.vboLines.setMode(7);
                                            WorldMapRenderer.vboLines.setTextureID(textureID);
                                            WorldMapRenderer.vboLines.setClampST(pyramid.getClampS(), pyramid.getClampT());
                                            WorldMapRenderer.vboLines.setMinMagFilters(pyramid.getMinFilter(), pyramid.getMagFilter());
                                            float alpha = (float)(Math.sin(System.currentTimeMillis() / 100L) + 1.0);
                                            alpha = 1.0F;
                                            WorldMapRenderer.vboLines
                                                .addQuad(
                                                    (float)x1x,
                                                    (float)y1x,
                                                    (float)u1x,
                                                    (float)v1x,
                                                    (float)x2x,
                                                    (float)y2x,
                                                    (float)u2x,
                                                    (float)v2x,
                                                    0.0F,
                                                    fill.r,
                                                    fill.g,
                                                    fill.b,
                                                    fill.a * alpha
                                                );
                                            WorldMapRenderer.vboLines.endRun();
                                            if (this.renderer.tileGrid.getValue()) {
                                                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                                                WorldMapRenderer.vboLines.setMode(1);
                                                WorldMapRenderer.vboLines.setLineWidth(2.0F);
                                                WorldMapRenderer.vboLines
                                                    .addRectOutline(
                                                        (float)(worldX1 - this.centerWorldX) * worldScale,
                                                        (float)(worldY1 - this.centerWorldY) * worldScale,
                                                        (float)(worldX2 - this.centerWorldX) * worldScale,
                                                        (float)(worldY2 - this.centerWorldY) * worldScale,
                                                        0.0F,
                                                        1.0F,
                                                        0.0F,
                                                        0.0F,
                                                        1.0F * alpha
                                                    );
                                                WorldMapRenderer.vboLines.endRun();
                                            }

                                            if (Core.debug) {
                                            }
                                        } else {
                                            int dbg = 1;
                                        }
                                    } else if (pyramidTexture != null) {
                                        boolean var84 = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        void renderImagePyramidGrid(WorldMapImages images) {
            float worldScale = this.worldScale;
            int tileSize = 256;
            int ptz1 = images.getZoom((float)this.getZoomAdjustedForPyramidScale(this.zoomF, images));
            float metersPerTile = 256 * (1 << ptz1);
            metersPerTile *= images.getResolution();
            float m_renderOriginX = (images.getMinX() - this.centerWorldX) * worldScale;
            float m_renderOriginY = (images.getMinY() - this.centerWorldY) * worldScale;
            int numTilesX = (int)Math.ceil((images.getMaxX() - images.getMinX()) / metersPerTile);
            int numTilesY = (int)Math.ceil((images.getMaxY() - images.getMinY()) / metersPerTile);
            float minXui = m_renderOriginX;
            float minYui = m_renderOriginY;
            float maxXui = m_renderOriginX + numTilesX * metersPerTile * worldScale;
            float maxYui = m_renderOriginY + numTilesY * metersPerTile * worldScale;
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(1);
            WorldMapRenderer.vboLines.setLineWidth(2.0F);

            for (int x = 0; x < numTilesX + 1; x++) {
                WorldMapRenderer.vboLines
                    .addLine(
                        m_renderOriginX + x * metersPerTile * worldScale,
                        minYui,
                        0.0F,
                        m_renderOriginX + x * metersPerTile * worldScale,
                        maxYui,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        0.5F
                    );
            }

            for (int y = 0; y < numTilesY + 1; y++) {
                WorldMapRenderer.vboLines
                    .addLine(
                        minXui,
                        m_renderOriginY + y * metersPerTile * worldScale,
                        0.0F,
                        maxXui,
                        m_renderOriginY + y * metersPerTile * worldScale,
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        0.5F
                    );
            }

            WorldMapRenderer.vboLines.endRun();
        }

        float triangleArea(float x0, float y0, float x1, float y1, float x2, float y2) {
            float a = Vector2f.length(x1 - x0, y1 - y0);
            float b = Vector2f.length(x2 - x1, y2 - y1);
            float c = Vector2f.length(x0 - x2, y0 - y2);
            float s = (a + b + c) / 2.0F;
            return (float)Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }

        void paintAreasOutsideBounds(int minX, int minY, int maxX, int maxY, float worldScale) {
            float x1cell = this.renderOriginX - minX % 256 * worldScale;
            float y1cell = this.renderOriginY - minY % 256 * worldScale;
            float x2cell = this.renderOriginX + ((this.worldMap.getMaxXInCells() + 1) * 256 - minX) * worldScale;
            float y2cell = this.renderOriginY + ((this.worldMap.getMaxYInCells() + 1) * 256 - minY) * worldScale;
            float z = 0.0F;
            WorldMapStyleLayer.RGBAf fill = this.fill;
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(7);
            if (minX % 256 != 0) {
                float x2 = this.renderOriginX;
                WorldMapRenderer.vboLines.addQuad(x1cell, y1cell, x2, y2cell, 0.0F, fill.r, fill.g, fill.b, fill.a);
            }

            if (minY % 256 != 0) {
                float x1 = this.renderOriginX;
                float x2 = x1 + this.worldMap.getWidthInSquares() * this.worldScale;
                float y2 = this.renderOriginY;
                WorldMapRenderer.vboLines.addQuad(x1, y1cell, x2, y2, 0.0F, fill.r, fill.g, fill.b, fill.a);
            }

            if (maxX + 1 != 0) {
                float x1 = this.renderOriginX + (maxX - minX + 1) * worldScale;
                WorldMapRenderer.vboLines.addQuad(x1, y1cell, x2cell, y2cell, 0.0F, fill.r, fill.g, fill.b, fill.a);
            }

            if (maxY + 1 != 0) {
                float x1 = this.renderOriginX;
                float y1 = this.renderOriginY + this.worldMap.getHeightInSquares() * worldScale;
                float x2 = this.renderOriginX + this.worldMap.getWidthInSquares() * worldScale;
                WorldMapRenderer.vboLines.addQuad(x1, y1, x2, y2cell, 0.0F, fill.r, fill.g, fill.b, fill.a);
            }

            WorldMapRenderer.vboLines.endRun();
        }

        void renderWorldBounds() {
            float x1 = this.renderOriginX;
            float y1 = this.renderOriginY;
            float x2 = x1 + this.worldMap.getWidthInSquares() * this.worldScale;
            float y2 = y1 + this.worldMap.getHeightInSquares() * this.worldScale;
            this.renderDropShadow();
            WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
            WorldMapRenderer.vboLines.setMode(1);
            WorldMapRenderer.vboLines.setLineWidth(2.0F);
            float rgb = 0.5F;
            WorldMapRenderer.vboLines.addLine(x1, y1, 0.0F, x2, y1, 0.0F, 0.5F, 0.5F, 0.5F, 1.0F);
            WorldMapRenderer.vboLines.addLine(x2, y1, 0.0F, x2, y2, 0.0F, 0.5F, 0.5F, 0.5F, 1.0F);
            WorldMapRenderer.vboLines.addLine(x2, y2, 0.0F, x1, y2, 0.0F, 0.5F, 0.5F, 0.5F, 1.0F);
            WorldMapRenderer.vboLines.addLine(x1, y2, 0.0F, x1, y1, 0.0F, 0.5F, 0.5F, 0.5F, 1.0F);
            WorldMapRenderer.vboLines.endRun();
        }

        private void renderDropShadow() {
            float TH = this.renderer.dropShadowWidth
                * (this.renderer.getHeight() / 1080.0F)
                * this.worldScale
                / this.renderer.getWorldScale(this.renderer.getBaseZoom());
            if (!(TH < 2.0F)) {
                float x1 = this.renderOriginX;
                float y1 = this.renderOriginY;
                float x2 = x1 + this.worldMap.getWidthInSquares() * this.worldScale;
                float y2 = y1 + this.worldMap.getHeightInSquares() * this.worldScale;
                WorldMapRenderer.vboLines.startRun(WorldMapRenderer.vboLines.formatPositionColor);
                WorldMapRenderer.vboLines.setMode(4);
                WorldMapRenderer.vboLines.reserve(3);
                WorldMapRenderer.vboLines.addElement(x1 + TH, y2, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F);
                WorldMapRenderer.vboLines.addElement(x2, y2, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F);
                WorldMapRenderer.vboLines.addElement(x1 + TH, y2 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.0F);
                WorldMapRenderer.vboLines.reserve(3);
                WorldMapRenderer.vboLines.addElement(x2, y2, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F);
                WorldMapRenderer.vboLines.addElement(x2 + TH, y2 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.0F);
                WorldMapRenderer.vboLines.addElement(x1 + TH, y2 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.0F);
                WorldMapRenderer.vboLines.reserve(3);
                WorldMapRenderer.vboLines.addElement(x2, y1 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F);
                WorldMapRenderer.vboLines.addElement(x2 + TH, y1 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.0F);
                WorldMapRenderer.vboLines.addElement(x2, y2, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F);
                WorldMapRenderer.vboLines.reserve(3);
                WorldMapRenderer.vboLines.addElement(x2 + TH, y1 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.0F);
                WorldMapRenderer.vboLines.addElement(x2 + TH, y2 + TH, 0.0F, 0.5F, 0.5F, 0.5F, 0.0F);
                WorldMapRenderer.vboLines.addElement(x2, y2, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F);
                WorldMapRenderer.vboLines.endRun();
            }
        }

        @Override
        public void postRender() {
            for (int i = 0; i < this.playerRenderData.length; i++) {
                WorldMapRenderer.PlayerRenderData playerRenderData = this.playerRenderData[i];
                if (playerRenderData.modelSlotRenderData != null) {
                    playerRenderData.modelSlotRenderData.postRender();
                }
            }

            this.symbolsRenderData.postRender();
        }
    }

    private static final class ListOfRenderLayers extends ArrayList<WorldMapRenderLayer> {
    }

    private static final class PlayerRenderData {
        ModelSlotRenderData modelSlotRenderData;
        float angle;
        float x;
        float y;
    }

    @UsedFromLua
    public final class WorldMapBooleanOption extends BooleanConfigOption {
        public WorldMapBooleanOption(final String name, final boolean defaultValue) {
            Objects.requireNonNull(WorldMapRenderer.this);
            super(name, defaultValue);
            WorldMapRenderer.this.options.add(this);
        }
    }

    @UsedFromLua
    public final class WorldMapDoubleOption extends DoubleConfigOption {
        public WorldMapDoubleOption(final String name, final double min, final double max, final double defaultValue) {
            Objects.requireNonNull(WorldMapRenderer.this);
            super(name, min, max, defaultValue);
            WorldMapRenderer.this.options.add(this);
        }
    }
}
