// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import org.lwjgl.opengl.GL20;
import org.lwjglx.opengl.Util;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

public class DefaultShader extends Shader {
    public static boolean isActive;
    private static int diffuse;
    private boolean textureActive;
    private boolean textureActiveCached = true;
    private int useTexture;
    private int zDepth;
    private float cachedZ;

    public DefaultShader(String name) {
        super(name);
    }

    @Override
    public void startMainThread(TextureDraw texd, int playerIndex) {
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
        GL20.glUniform1i(diffuse, 0);
    }

    @Override
    public void onCompileSuccess(ShaderProgram sender) {
        super.onCompileSuccess(sender);
        int shaderID = this.getID();
        diffuse = GL20.glGetUniformLocation(shaderID, "DIFFUSE");
        this.zDepth = GL20.glGetUniformLocation(shaderID, "zDepth");
        this.useTexture = GL20.glGetUniformLocation(shaderID, "useTexture");
    }

    public void setTextureActive(boolean b) {
        this.textureActive = b;
        if (this.textureActive != this.textureActiveCached) {
            GL20.glUniform1i(this.useTexture, this.textureActive ? 1 : 0);
            Util.checkGLError();
        }

        this.textureActiveCached = this.textureActive;
    }

    public void setZ(float z) {
        if (this.cachedZ != z) {
            GL20.glUniform1f(this.zDepth, z);
            this.cachedZ = z;
        }
    }

    public void setChunkDepth(float depth) {
        this.getProgram().setValue("chunkDepth", depth);
    }
}
