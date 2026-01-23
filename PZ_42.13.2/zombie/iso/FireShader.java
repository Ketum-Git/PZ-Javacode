// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

public class FireShader extends Shader {
    private int mvpMatrix;
    private int fireTime;
    private int fireParam;
    private int fireTexture;

    public FireShader(String name) {
        super(name);
    }

    @Override
    protected void onCompileSuccess(ShaderProgram shaderProgram) {
        int shaderID = shaderProgram.getShaderID();
        this.fireTexture = GL20.glGetUniformLocation(shaderID, "FireTexture");
        this.mvpMatrix = GL20.glGetUniformLocation(shaderID, "mvpMatrix");
        this.fireTime = GL20.glGetUniformLocation(shaderID, "FireTime");
        this.fireParam = GL20.glGetUniformLocation(shaderID, "FireParam");
        this.Start();
        if (this.fireTexture != -1) {
            GL20.glUniform1i(this.fireTexture, 0);
        }

        this.End();
    }

    public void updateFireParams(TextureDraw texd, int userId, float in_firetime) {
        ParticlesFire f = ParticlesFire.getInstance();
        GL13.glActiveTexture(33984);
        f.getFireFlameTexture().bind();
        GL11.glTexEnvi(8960, 8704, 7681);
        GL20.glUniformMatrix4fv(this.mvpMatrix, true, f.getMVPMatrix());
        GL20.glUniform1f(this.fireTime, in_firetime);
        GL20.glUniformMatrix3fv(this.fireParam, true, f.getParametersFire());
        if (this.fireTexture != -1) {
            GL20.glUniform1i(this.fireTexture, 0);
        }
    }
}
