// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.TextureDraw;

public class TileSeamShader extends Shader {
    private int diffuse;
    private int depth;
    private int mask;

    public TileSeamShader(String name) {
        super(name);
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
        GL20.glUniform1i(this.diffuse, 0);
        GL20.glUniform1i(this.depth, 1);
        GL20.glUniform1i(this.mask, 2);
        VertexBufferObject.setModelViewProjection(this.getProgram());
    }

    @Override
    protected void onCompileSuccess(ShaderProgram sender) {
        int shaderID = this.getID();
        this.diffuse = GL20.glGetUniformLocation(shaderID, "DIFFUSE");
        this.depth = GL20.glGetUniformLocation(shaderID, "DEPTH");
        this.mask = GL20.glGetUniformLocation(shaderID, "MASK");
    }
}
