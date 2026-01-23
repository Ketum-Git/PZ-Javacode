// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public class DepthShader extends Shader {
    public static boolean isActive;
    private static int diffuse;
    private static int depth;
    private int xOff;
    private int yOff;
    private int zOff;

    public DepthShader(String name) {
        super(name);
    }

    @Override
    public void startMainThread(TextureDraw texd, int playerIndex) {
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
        GL20.glUniform1i(depth, 0);
        int lastID = Texture.lastTextureID;
        Texture.lastTextureID = 0;
        this.getProgram().setValue("DIFFUSE", texd.tex1, 1);
        Texture.lastTextureID = lastID;
        GL13.glActiveTexture(33984);
        SpriteRenderer.ringBuffer.shaderChangedTexture1();
    }

    @Override
    public void onCompileSuccess(ShaderProgram sender) {
        int shaderID = this.getID();
        depth = GL20.glGetUniformLocation(shaderID, "DEPTH");
        this.xOff = GL20.glGetUniformLocation(shaderID, "XOFF");
        this.yOff = GL20.glGetUniformLocation(shaderID, "YOFF");
        this.zOff = GL20.glGetUniformLocation(shaderID, "ZOFF");
    }
}
