// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fog;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.TextureDraw;

public final class ImprovedFogDrawer extends TextureDraw.GenericDrawer {
    float screenInfo1;
    float screenInfo2;
    float screenInfo3;
    float screenInfo4;
    float textureInfo1;
    float textureInfo2;
    float textureInfo3;
    float textureInfo4;
    float worldOffset1;
    float worldOffset2;
    float worldOffset3;
    float worldOffset4;
    float scalingInfo1;
    float scalingInfo2;
    float scalingInfo3;
    float scalingInfo4;
    float colorInfo1;
    float colorInfo2;
    float colorInfo3;
    float colorInfo4;
    float paramInfo1;
    float paramInfo2;
    float paramInfo3;
    float paramInfo4;
    float cameraInfo1;
    float cameraInfo2;
    float cameraInfo3;
    float cameraInfo4;
    float alpha;
    static final int RECTANGLE_BYTES = 60;
    ByteBuffer rectangleBuffer = ByteBuffer.allocate(7680);

    @Override
    public void render() {
        FogShader shader = FogShader.instance;
        if (shader.getProgram() == null) {
            shader.initShader();
            if (!shader.getProgram().isCompiled()) {
                return;
            }
        }

        if (this.rectangleBuffer.position() == 0) {
            int shaderID = shader.getProgram().getShaderID();
            ShaderHelper.glUseProgramObjectARB(shaderID);
            shader.setTextureInfo3(this.textureInfo1, this.textureInfo2, this.textureInfo3, this.textureInfo4);
            shader.setWorldOffset3(this.worldOffset1, this.worldOffset2, this.worldOffset3, this.worldOffset4);
            shader.setColorInfo3(this.colorInfo1, this.colorInfo2, this.colorInfo3, this.colorInfo4);
            shader.setParamInfo3(this.paramInfo1, this.paramInfo2, this.paramInfo3, this.paramInfo4);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            if (PerformanceSettings.fboRenderChunk) {
                GL11.glDepthMask(false);
                GL11.glDepthFunc(513);
            }

            int zLayer1 = -1;
            VBORenderer vbor = VBORenderer.getInstance();
            int numRectangles = this.rectangleBuffer.limit() / 60;

            for (int i = 0; i < numRectangles; i++) {
                float sx = this.rectangleBuffer.getFloat();
                float sy = this.rectangleBuffer.getFloat();
                float ex = this.rectangleBuffer.getFloat();
                float ey = this.rectangleBuffer.getFloat();
                float u0 = this.rectangleBuffer.getFloat();
                float v0 = this.rectangleBuffer.getFloat();
                float u1 = this.rectangleBuffer.getFloat();
                float v1 = this.rectangleBuffer.getFloat();
                float depth0 = this.rectangleBuffer.getFloat();
                float depth1 = this.rectangleBuffer.getFloat();
                float depth2 = this.rectangleBuffer.getFloat();
                float depth3 = this.rectangleBuffer.getFloat();
                float offset = this.rectangleBuffer.getFloat();
                float layerAlpha = this.rectangleBuffer.getFloat();
                int zLayer = this.rectangleBuffer.getInt();
                vbor.startRun(VBORenderer.getInstance().formatPositionColorUvDepth);
                vbor.setMode(7);
                vbor.setTextureID(ImprovedFog.getNoiseTexture().getTextureId());
                vbor.setDepthTest(PerformanceSettings.fboRenderChunk);
                vbor.setShaderProgram(shader.getProgram());
                if (zLayer != zLayer1) {
                    shader.setScalingInfo2(this.scalingInfo1, this.scalingInfo2, zLayer, this.scalingInfo4);
                    zLayer1 = zLayer;
                }

                shader.setScreenInfo2(this.screenInfo1, this.screenInfo2, this.screenInfo3, layerAlpha);
                shader.setRectangleInfo2((int)sx, (int)sy, (int)(ex - sx), (int)(ey - sy));
                shader.setCameraInfo2(this.cameraInfo1, this.cameraInfo2, this.cameraInfo3, offset);
                float glZ = 0.0F;
                vbor.addQuadDepth(
                    sx,
                    sy,
                    0.0F,
                    u0,
                    v0,
                    depth0,
                    ex,
                    sy,
                    0.0F,
                    u1,
                    v0,
                    depth1,
                    ex,
                    ey,
                    0.0F,
                    u1,
                    v1,
                    depth2,
                    sx,
                    ey,
                    0.0F,
                    u0,
                    v1,
                    depth3,
                    1.0F,
                    1.0F,
                    1.0F,
                    this.alpha
                );
                vbor.endRun();
            }

            vbor.flush();
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }
    }

    public void startFrame() {
        this.rectangleBuffer.clear();
    }

    public void endFrame() {
        if (this.rectangleBuffer.position() != 0) {
            this.rectangleBuffer.flip();
            SpriteRenderer.instance.drawGeneric(this);
        }
    }

    void addRectangle(
        float sx,
        float sy,
        float ex,
        float ey,
        float u0,
        float v0,
        float u1,
        float v1,
        float offset,
        float depthBottom,
        float depthTop,
        float layerAlpha,
        int zLayer
    ) {
        if (this.rectangleBuffer.capacity() < this.rectangleBuffer.position() + 60) {
            ByteBuffer bb = ByteBuffer.allocate(this.rectangleBuffer.capacity() + 7680);
            this.rectangleBuffer.flip();
            bb.put(this.rectangleBuffer);
            this.rectangleBuffer = bb;
        }

        this.rectangleBuffer.putFloat(sx);
        this.rectangleBuffer.putFloat(sy);
        this.rectangleBuffer.putFloat(ex);
        this.rectangleBuffer.putFloat(ey);
        this.rectangleBuffer.putFloat(u0);
        this.rectangleBuffer.putFloat(v0);
        this.rectangleBuffer.putFloat(u1);
        this.rectangleBuffer.putFloat(v1);
        this.rectangleBuffer.putFloat(depthTop);
        this.rectangleBuffer.putFloat(depthTop);
        this.rectangleBuffer.putFloat(depthBottom);
        this.rectangleBuffer.putFloat(depthBottom);
        this.rectangleBuffer.putFloat(offset);
        this.rectangleBuffer.putFloat(layerAlpha);
        this.rectangleBuffer.putInt(zLayer);
    }
}
