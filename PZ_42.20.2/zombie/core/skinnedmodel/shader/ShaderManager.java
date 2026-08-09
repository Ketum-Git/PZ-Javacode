// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.shader;

import java.util.ArrayList;

public final class ShaderManager {
    public static final ShaderManager instance = new ShaderManager();
    private final ArrayList<Shader> shaders = new ArrayList<>();

    private Shader getShader(String name, boolean bStatic, boolean bInstanced) {
        for (int i = 0; i < this.shaders.size(); i++) {
            Shader shader = this.shaders.get(i);
            if (name.equals(shader.name) && bStatic == shader.isStatic && bInstanced == shader.instanced) {
                return shader;
            }
        }

        return null;
    }

    public Shader getOrCreateShader(String name, boolean bStatic, boolean bInstanced) {
        Shader shader = this.getShader(name, bStatic, bInstanced);
        if (shader != null) {
            return shader;
        } else {
            for (int i = 0; i < this.shaders.size(); i++) {
                Shader shader1 = this.shaders.get(i);
                if (shader1.name.equalsIgnoreCase(name) && !shader1.name.equals(name)) {
                    throw new IllegalArgumentException("shader filenames are case-sensitive");
                }
            }

            shader = new Shader(name, bStatic, bInstanced);
            this.shaders.add(shader);
            return shader;
        }
    }

    public Shader getShaderByID(int shaderID) {
        for (int i = 0; i < this.shaders.size(); i++) {
            Shader shader1 = this.shaders.get(i);
            if (shader1.getID() == shaderID) {
                return shader1;
            }
        }

        return null;
    }
}
