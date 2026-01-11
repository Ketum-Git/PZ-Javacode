// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.viewCone;

import org.lwjgl.opengl.GL13;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public class ChunkRenderShader extends Shader {
    public ChunkRenderShader(String name) {
        super(name);
    }

    @Override
    public void startMainThread(TextureDraw texd, int playerIndex) {
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
        this.getProgram().setValue("DEPTH", texd.tex1, 1);
        GL13.glActiveTexture(33984);
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        Texture.lastTextureID = 0;
        SpriteRenderer.ringBuffer.shaderChangedTexture1();
        this.getProgram().setValue("chunkDepth", texd.chunkDepth);
    }

    @Override
    public void onCompileSuccess(ShaderProgram sender) {
        this.Start();
        sender.setSamplerUnit("DIFFUSE", 0);
        sender.setSamplerUnit("DEPTH", 1);
        this.End();
    }
}
