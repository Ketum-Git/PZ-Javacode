// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.GameTime;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.interfaces.ITexture;
import zombie.iso.weather.ClimateManager;

public final class IsoWater {
    public static final float DEPTH_ADJUST = 0.001F;
    public Shader effect;
    private float waterTime;
    private float waterWindAngle;
    private float waterWindIntensity;
    private float waterRainIntensity;
    private final Vector2f waterParamWindInt;
    private final Texture texBottom;
    private int apiId;
    private static IsoWater instance;
    private static boolean isShaderEnable;
    private final IsoWater.RenderData[][] renderData = new IsoWater.RenderData[3][4];
    private final IsoWater.RenderData[][] renderDataShore = new IsoWater.RenderData[3][4];
    static final int BYTES_PER_FLOAT = 4;
    static final int FLOATS_PER_VERTEX = 8;
    static final int BYTES_PER_VERTEX = 32;
    static final int VERTICES_PER_SQUARE = 4;
    private final Vector4f shaderOffset = new Vector4f();

    public static synchronized IsoWater getInstance() {
        if (instance == null) {
            instance = new IsoWater();
        }

        return instance;
    }

    public boolean getShaderEnable() {
        return isShaderEnable;
    }

    public IsoWater() {
        this.texBottom = Texture.getSharedTexture("media/textures/river_bottom.png");
        RenderThread.invokeOnRenderContext(() -> {
            if (GL.getCapabilities().OpenGL30) {
                this.apiId = 1;
            }

            if (GL.getCapabilities().GL_ARB_framebuffer_object) {
                this.apiId = 2;
            }

            if (GL.getCapabilities().GL_EXT_framebuffer_object) {
                this.apiId = 3;
            }
        });

        for (int i = 0; i < this.renderData.length; i++) {
            for (int pn = 0; pn < 4; pn++) {
                this.renderData[i][pn] = new IsoWater.RenderData();
                this.renderDataShore[i][pn] = new IsoWater.RenderData();
            }
        }

        this.applyWaterQuality();
        this.waterParamWindInt = new Vector2f(0.0F);
    }

    public void applyWaterQuality() {
        if (PerformanceSettings.waterQuality == 2) {
            isShaderEnable = false;
        }

        if (PerformanceSettings.waterQuality == 1) {
            isShaderEnable = true;
            RenderThread.invokeOnRenderContext(() -> {
                ShaderHelper.glUseProgramObjectARB(0);
                this.effect = new WaterShader("water");
                ShaderHelper.glUseProgramObjectARB(0);
            });
        }

        if (PerformanceSettings.waterQuality == 0) {
            isShaderEnable = true;
            RenderThread.invokeOnRenderContext(() -> {
                this.effect = new WaterShader("water_hq");
                this.effect.Start();
                this.effect.End();
            });
        }
    }

    public void render(ArrayList<IsoGridSquare> Grid) {
        if (this.getShaderEnable()) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            IsoWater.RenderData threadData = this.renderData[stateIndex][playerIndex];
            threadData.clear();

            for (int i = 0; i < Grid.size(); i++) {
                IsoGridSquare square = Grid.get(i);
                if (square.chunk == null || !square.chunk.lightingNeverDone[playerIndex]) {
                    IsoWaterGeometry waterGeometry = square.getWater();
                    if (waterGeometry != null && !waterGeometry.shore && waterGeometry.hasWater) {
                        threadData.addSquare(waterGeometry);
                    }
                }
            }

            if (threadData.numSquares != 0) {
                SpriteRenderer.instance.drawWater(this.effect, playerIndex, this.apiId, false);
            }
        }
    }

    public void renderShore(ArrayList<IsoGridSquare> Grid) {
        if (this.getShaderEnable()) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            IsoWater.RenderData threadDataShore = this.renderDataShore[stateIndex][playerIndex];
            threadDataShore.clear();

            for (int i = 0; i < Grid.size(); i++) {
                IsoGridSquare square = Grid.get(i);
                if (square.chunk == null || !square.chunk.lightingNeverDone[playerIndex]) {
                    IsoWaterGeometry waterGeometry = square.getWater();
                    if (waterGeometry != null && waterGeometry.shore) {
                        threadDataShore.addSquare(waterGeometry);
                    }
                }
            }

            if (threadDataShore.numSquares > 0) {
                SpriteRenderer.instance.drawWater(this.effect, playerIndex, this.apiId, true);
            }
        }
    }

    public void waterProjection(Matrix4f PROJECTION) {
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
        PROJECTION.setOrtho(
            camera.getOffX(), camera.getOffX() + camera.offscreenWidth, camera.getOffY() + camera.offscreenHeight, camera.getOffY(), -1.0F, 1.0F
        );
    }

    public void waterGeometry(boolean bShore) {
        long start = System.nanoTime();
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        IsoWater.RenderData threadData = bShore ? this.renderDataShore[stateIndex][playerIndex] : this.renderData[stateIndex][playerIndex];
        int firstSquare = 0;
        int numSquares = threadData.numSquares;

        while (numSquares > 0) {
            int some = this.renderSome(firstSquare, numSquares, bShore);
            firstSquare += some;
            numSquares -= some;
        }

        long end = System.nanoTime();
        SpriteRenderer.ringBuffer.restoreVbos = true;
    }

    private int renderSome(int firstSquare, int numSquares, boolean bShore) {
        IsoPuddles.VBOs.next();
        FloatBuffer vertices = IsoPuddles.VBOs.vertices;
        ShortBuffer indices = IsoPuddles.VBOs.indices;
        int aVertex = 0;
        int aColor = 1;
        int aDepth = 2;
        int aFlow = 3;
        int aSpeed = 4;
        int aExternal = 5;
        int aFragDepth = 6;
        GL20.glEnableVertexAttribArray(4);
        GL20.glEnableVertexAttribArray(5);
        GL20.glEnableVertexAttribArray(6);
        GL20.glVertexAttribPointer(2, 1, 5126, true, 32, 0L);
        GL20.glVertexAttribPointer(3, 1, 5126, true, 32, 4L);
        GL20.glVertexAttribPointer(4, 1, 5126, true, 32, 8L);
        GL20.glVertexAttribPointer(5, 1, 5126, true, 32, 12L);
        GL20.glVertexAttribPointer(0, 2, 5126, false, 32, 16L);
        GL20.glVertexAttribPointer(1, 4, 5121, true, 32, 24L);
        GL20.glVertexAttribPointer(6, 1, 5126, true, 32, 28L);
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        IsoWater.RenderData threadData = bShore ? this.renderDataShore[stateIndex][playerIndex] : this.renderData[stateIndex][playerIndex];
        int numVertices = Math.min(numSquares * 4, IsoPuddles.VBOs.bufferSizeVertices);
        vertices.put(threadData.data, firstSquare * 4 * 8, numVertices * 8);
        int vertexCursor = 0;
        int indexCursor = 0;

        for (int i = 0; i < numVertices / 4; i++) {
            if (threadData.data[i * 4 * 8] == threadData.data[i * 4 * 8 + 16]) {
                indices.put((short)vertexCursor);
                indices.put((short)(vertexCursor + 1));
                indices.put((short)(vertexCursor + 2));
                indices.put((short)vertexCursor);
                indices.put((short)(vertexCursor + 2));
                indices.put((short)(vertexCursor + 3));
            } else {
                indices.put((short)(vertexCursor + 1));
                indices.put((short)(vertexCursor + 2));
                indices.put((short)(vertexCursor + 3));
                indices.put((short)(vertexCursor + 1));
                indices.put((short)(vertexCursor + 3));
                indices.put((short)(vertexCursor + 0));
            }

            vertexCursor += 4;
            indexCursor += 6;
        }

        IsoPuddles.VBOs.unmap();
        GL11.glEnable(2929);
        GL11.glDepthFunc(513);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(770, 771);
        int start = 0;
        int startIndex = 0;
        GL12.glDrawRangeElements(4, 0, 0 + vertexCursor, indexCursor - 0, 5123, 0L);
        GL13.glActiveTexture(33984);
        GL20.glDisableVertexAttribArray(4);
        GL20.glDisableVertexAttribArray(5);
        GL20.glDisableVertexAttribArray(6);
        return numVertices / 4;
    }

    public ITexture getTextureBottom() {
        return this.texBottom;
    }

    public float getShaderTime() {
        return this.waterTime;
    }

    public float getRainIntensity() {
        return this.waterRainIntensity;
    }

    public void update(ClimateManager cm) {
        this.waterWindAngle = cm.getCorrectedWindAngleIntensity();
        this.waterWindIntensity = cm.getWindIntensity() * 5.0F;
        this.waterRainIntensity = cm.getRainIntensity();
        float mult = GameTime.getInstance().getMultiplier();
        this.waterTime += 0.0166F * mult;
        this.waterParamWindInt
            .add(
                (float)Math.sin(this.waterWindAngle * 6.0F) * this.waterWindIntensity * 0.05F * (mult / 1.6F),
                (float)Math.cos(this.waterWindAngle * 6.0F) * this.waterWindIntensity * 0.15F * (mult / 1.6F)
            );
    }

    public float getWaterWindX() {
        return this.waterParamWindInt.x;
    }

    public float getWaterWindY() {
        return this.waterParamWindInt.y;
    }

    public float getWaterWindSpeed() {
        return this.waterWindIntensity * 2.0F;
    }

    public Vector4f getShaderOffset() {
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
        float jx = -camera.fixJigglyModelsX * camera.zoom;
        float jy = -camera.fixJigglyModelsY * camera.zoom;
        return this.shaderOffset
            .set(
                camera.getOffX() + jx - IsoCamera.getOffscreenLeft(playerIndex) * camera.zoom,
                camera.getOffY() + jy + IsoCamera.getOffscreenTop(playerIndex) * camera.zoom,
                (float)camera.offscreenWidth,
                (float)camera.offscreenHeight
            );
    }

    public void FBOStart() {
        int nPlayer = IsoCamera.frameState.playerIndex;
    }

    public void FBOEnd() {
        int nPlayer = IsoCamera.frameState.playerIndex;
    }

    private static final class RenderData {
        int numSquares;
        int capacity = 512;
        float[] data;

        void clear() {
            this.numSquares = 0;
        }

        void addSquare(IsoWaterGeometry water) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            int VERTICES_PER_SQUARE = 4;
            if (this.data == null) {
                this.data = new float[this.capacity * 4 * 8];
            }

            if (this.numSquares + 1 > this.capacity) {
                this.capacity += 128;
                this.data = Arrays.copyOf(this.data, this.capacity * 4 * 8);
            }

            PlayerCamera camera = IsoCamera.cameras[playerIndex];
            float jx = camera.fixJigglyModelsX * camera.zoom;
            float jy = camera.fixJigglyModelsY * camera.zoom;
            int n = this.numSquares * 4 * 8;

            for (int i = 0; i < 4; i++) {
                this.data[n++] = water.depth[i];
                this.data[n++] = water.flow[i];
                this.data[n++] = water.speed[i];
                this.data[n++] = water.isExternal;
                this.data[n++] = water.x[i] + jx;
                this.data[n++] = water.y[i] + jy;
                if (water.square != null) {
                    int col = water.square.getVertLight((4 - i) % 4, playerIndex);
                    if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                        col = -1;
                    }

                    this.data[n++] = Float.intBitsToFloat(col);
                    float dx = 0.0F;
                    float dy = 0.0F;
                    if (i == 1 || i == 2) {
                        dx = 1.0F;
                    }

                    if (i == 2 || i == 3) {
                        dy = 1.0F;
                    }

                    dx += camera.fixJigglyModelsSquareX;
                    dy += camera.fixJigglyModelsSquareY;
                    float adjustDepth = 0.001F;
                    int stateIndex = SpriteRenderer.instance.getMainStateIndex();
                    if (this == IsoWater.instance.renderDataShore[stateIndex][playerIndex]) {
                    }

                    this.data[n++] = IsoDepthHelper.getSquareDepthData(
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                                water.square.x + dx - 0.0F,
                                water.square.y + dy - 0.0F,
                                water.square.z
                            )
                            .depthStart
                        + 0.001F;
                } else {
                    n++;
                    n++;
                }
            }

            this.numSquares++;
        }
    }
}
