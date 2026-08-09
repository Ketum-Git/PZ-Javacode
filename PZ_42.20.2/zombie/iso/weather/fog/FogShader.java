// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fog;

import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.IShaderProgramListener;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;

public class FogShader implements IShaderProgramListener {
    public static final FogShader instance = new FogShader();
    private ShaderProgram shaderProgram;
    private int noiseTexture;
    private int screenInfo;
    private int textureInfo;
    private int rectangleInfo;
    private int worldOffset;
    private int scalingInfo;
    private int colorInfo;
    private int paramInfo;
    private int cameraInfo;
    private int uTargetDepth;

    public void initShader() {
        this.shaderProgram = ShaderProgram.createShaderProgram("fog", false, false, false);
        this.shaderProgram.addCompileListener(this);
        this.shaderProgram.compile();
        if (this.shaderProgram.isCompiled()) {
            ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
            ShaderHelper.glUseProgramObjectARB(0);
        }
    }

    public ShaderProgram getProgram() {
        return this.shaderProgram;
    }

    public void setScreenInfo(float width, float height, float zoom, float secondLayerAlpha) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.screenInfo, width, height, zoom, secondLayerAlpha);
    }

    public void setTextureInfo(float u1, float u2, float alpha, float u3) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.textureInfo, u1, u2, alpha, u3);
    }

    public void setRectangleInfo(float x, float y, float w, float h) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.rectangleInfo, x, y, w, h);
    }

    public void setScalingInfo(float a, float b, float c, float d) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.scalingInfo, a, b, c, d);
    }

    public void setColorInfo(float r, float g, float b, float a) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.colorInfo, r, g, b, a);
    }

    public void setWorldOffset(float x, float y, float z, float u) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.worldOffset, x, y, z, u);
    }

    public void setParamInfo(float x, float y, float z, float w) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.paramInfo, x, y, z, w);
    }

    public void setCameraInfo(float x, float y, float z, float w) {
        SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.cameraInfo, x, y, z, w);
    }

    public void setTargetDepth(float targetDepth) {
        SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.uTargetDepth, targetDepth);
    }

    public void setScreenInfo2(float width, float height, float zoom, float secondLayerAlpha) {
        VBORenderer.getInstance().cmdShader4f(this.screenInfo, width, height, zoom, secondLayerAlpha);
    }

    public void setTextureInfo2(float u1, float u2, float alpha, float u3) {
        VBORenderer.getInstance().cmdShader4f(this.textureInfo, u1, u2, alpha, u3);
    }

    public void setRectangleInfo2(float x, float y, float w, float h) {
        VBORenderer.getInstance().cmdShader4f(this.rectangleInfo, x, y, w, h);
    }

    public void setScalingInfo2(float a, float b, float c, float d) {
        VBORenderer.getInstance().cmdShader4f(this.scalingInfo, a, b, c, d);
    }

    public void setColorInfo2(float r, float g, float b, float a) {
        VBORenderer.getInstance().cmdShader4f(this.colorInfo, r, g, b, a);
    }

    public void setWorldOffset2(float x, float y, float z, float u) {
        VBORenderer.getInstance().cmdShader4f(this.worldOffset, x, y, z, u);
    }

    public void setParamInfo2(float x, float y, float z, float w) {
        VBORenderer.getInstance().cmdShader4f(this.paramInfo, x, y, z, w);
    }

    public void setCameraInfo2(float x, float y, float z, float w) {
        VBORenderer.getInstance().cmdShader4f(this.cameraInfo, x, y, z, w);
    }

    public void setTargetDepth2(float targetDepth) {
        VBORenderer.getInstance().cmdShader1f(this.uTargetDepth, targetDepth);
    }

    public void setTextureInfo3(float u1, float u2, float alpha, float u3) {
        GL20.glUniform4f(this.textureInfo, u1, u2, alpha, u3);
    }

    public void setWorldOffset3(float x, float y, float z, float u) {
        GL20.glUniform4f(this.worldOffset, x, y, z, u);
    }

    public void setParamInfo3(float x, float y, float z, float w) {
        GL20.glUniform4f(this.paramInfo, x, y, z, w);
    }

    public void setColorInfo3(float r, float g, float b, float a) {
        GL20.glUniform4f(this.colorInfo, r, g, b, a);
    }

    public boolean StartShader() {
        if (this.shaderProgram == null) {
            RenderThread.invokeOnRenderContext(this::initShader);
        }

        if (this.shaderProgram.isCompiled()) {
            IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
            return true;
        } else {
            return false;
        }
    }

    protected void reloadShader() {
        if (this.shaderProgram != null) {
            this.shaderProgram = null;
        }
    }

    @Override
    public void callback(ShaderProgram sender) {
        this.noiseTexture = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "NoiseTexture");
        this.screenInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "screenInfo");
        this.textureInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "textureInfo");
        this.rectangleInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "rectangleInfo");
        this.scalingInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "scalingInfo");
        this.colorInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "colorInfo");
        this.worldOffset = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "worldOffset");
        this.paramInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "paramInfo");
        this.cameraInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "cameraInfo");
        this.uTargetDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "targetDepth");
    }
}
