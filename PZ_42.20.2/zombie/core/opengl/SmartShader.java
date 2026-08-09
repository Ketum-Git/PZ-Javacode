// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import org.lwjgl.util.vector.Matrix4f;
import zombie.core.textures.Texture;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

/**
 * Created by LEMMY on 6/30/2016.
 */
public final class SmartShader {
    private final ShaderProgram shaderProgram;

    public SmartShader(String name) {
        this.shaderProgram = ShaderProgram.createShaderProgram(name, false, false, true);
    }

    public SmartShader(String name, boolean bStatic) {
        this.shaderProgram = ShaderProgram.createShaderProgram(name, bStatic, false, true);
    }

    public ShaderProgram getShaderProgram() {
        return this.shaderProgram;
    }

    public void Start() {
        this.shaderProgram.Start();
    }

    public void End() {
        this.shaderProgram.End();
    }

    public void setValue(String loc, float val) {
        this.shaderProgram.setValue(loc, val);
    }

    public void setValue(String loc, int val) {
        this.shaderProgram.setValue(loc, val);
    }

    public void setValue(String loc, Vector3 val) {
        this.shaderProgram.setValue(loc, val);
    }

    public void setValue(String loc, Vector2 val) {
        this.shaderProgram.setValue(loc, val);
    }

    public void setVector2f(String loc, float f1, float f2) {
        this.shaderProgram.setVector2(loc, f1, f2);
    }

    public void setVector3f(String loc, float f1, float f2, float f3) {
        this.shaderProgram.setVector3(loc, f1, f2, f3);
    }

    public void setVector4f(String loc, float f1, float f2, float f3, float f4) {
        this.shaderProgram.setVector4(loc, f1, f2, f3, f4);
    }

    public void setValue(String loc, Matrix4f matrix4f) {
        this.shaderProgram.setValue(loc, matrix4f);
    }

    public void setValue(String loc, Texture tex, int samplerUnit) {
        this.shaderProgram.setValue(loc, tex, samplerUnit);
    }
}
