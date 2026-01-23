// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.iso.weather.WeatherShader;
import zombie.tileDepth.CutawayAttachedShader;
import zombie.tileDepth.TileDepthShader;
import zombie.tileDepth.TileSeamShader;
import zombie.viewCone.BlurShader;
import zombie.viewCone.ChunkRenderShader;

public class SceneShaderStore {
    public static ChunkRenderShader chunkRenderShader;
    public static DefaultShader defaultShader;
    public static Shader weatherShader;
    public static Shader blurShader;
    public static CutawayAttachedShader cutawayAttachedShader;
    public static TileDepthShader opaqueDepthShader;
    public static TileDepthShader tileDepthShader;
    public static TileSeamShader tileSeamShader;
    public static int defaultShaderId;

    public static void shaderOptionsChanged() {
        try {
            if (cutawayAttachedShader == null) {
                cutawayAttachedShader = new CutawayAttachedShader("CutawayAttached");
            }

            if (opaqueDepthShader == null) {
                opaqueDepthShader = new TileDepthShader("opaqueWithDepth");
            }

            if (tileDepthShader == null) {
                tileDepthShader = new TileDepthShader("tileWithDepth");
            }

            if (tileSeamShader == null) {
                tileSeamShader = new TileSeamShader("seamFix2");
            }

            if (defaultShader == null) {
                defaultShader = new DefaultShader("default");
                defaultShaderId = defaultShader.getID();
            }

            if (blurShader == null) {
                blurShader = new BlurShader("blur");
            }

            if (cutawayAttachedShader != null && !cutawayAttachedShader.isCompiled()) {
                cutawayAttachedShader = null;
            }

            if (opaqueDepthShader != null && !opaqueDepthShader.isCompiled()) {
                opaqueDepthShader = null;
            }

            if (tileDepthShader != null && !tileDepthShader.isCompiled()) {
                tileDepthShader = null;
            }

            if (tileSeamShader != null && !tileSeamShader.isCompiled()) {
                tileSeamShader = null;
            }

            if (blurShader != null && !blurShader.isCompiled()) {
                blurShader = null;
            }

            if (defaultShader != null && !defaultShader.isCompiled()) {
                defaultShader = null;
            }

            if (weatherShader == null) {
                weatherShader = new WeatherShader("screen");
            }

            if (weatherShader != null && !weatherShader.isCompiled()) {
                weatherShader = null;
            }
        } catch (Exception var1) {
            weatherShader = null;
            blurShader = null;
        }
    }

    public static void initShaders() {
        try {
            if (cutawayAttachedShader == null) {
                RenderThread.invokeOnRenderContext(() -> cutawayAttachedShader = new CutawayAttachedShader("CutawayAttached"));
            }

            if (opaqueDepthShader == null) {
                RenderThread.invokeOnRenderContext(() -> opaqueDepthShader = new TileDepthShader("opaqueWithDepth"));
            }

            if (tileDepthShader == null) {
                RenderThread.invokeOnRenderContext(() -> tileDepthShader = new TileDepthShader("tileWithDepth"));
            }

            if (tileSeamShader == null) {
                RenderThread.invokeOnRenderContext(() -> tileSeamShader = new TileSeamShader("seamFix2"));
            }

            if (weatherShader == null) {
                RenderThread.invokeOnRenderContext(() -> weatherShader = new WeatherShader("screen"));
            }

            if (blurShader == null) {
                RenderThread.invokeOnRenderContext(() -> blurShader = new BlurShader("blur"));
            }

            if (chunkRenderShader == null) {
                RenderThread.invokeOnRenderContext(() -> chunkRenderShader = new ChunkRenderShader("chunkShader"));
            }

            if (chunkRenderShader == null || !chunkRenderShader.isCompiled()) {
                chunkRenderShader = null;
            }

            if (cutawayAttachedShader != null && !cutawayAttachedShader.isCompiled()) {
                cutawayAttachedShader = null;
            }

            if (opaqueDepthShader != null && !opaqueDepthShader.isCompiled()) {
                opaqueDepthShader = null;
            }

            if (tileDepthShader != null && !tileDepthShader.isCompiled()) {
                tileDepthShader = null;
            }

            if (tileSeamShader != null && !tileSeamShader.isCompiled()) {
                tileSeamShader = null;
            }

            if (weatherShader == null || !weatherShader.isCompiled()) {
                weatherShader = null;
            }

            if (weatherShader == null || !weatherShader.isCompiled()) {
                weatherShader = null;
            }
        } catch (Exception var1) {
            weatherShader = null;
            blurShader = null;
            var1.printStackTrace();
        }
    }

    public static void initGlobalShader() {
        try {
            if (defaultShader == null) {
                RenderThread.invokeOnRenderContext(() -> defaultShader = new DefaultShader("default"));
                defaultShaderId = defaultShader.getID();
            }

            if (defaultShader == null || !defaultShader.isCompiled()) {
                defaultShader = null;
            }
        } catch (Exception var1) {
            defaultShader = null;
            var1.printStackTrace();
        }
    }
}
