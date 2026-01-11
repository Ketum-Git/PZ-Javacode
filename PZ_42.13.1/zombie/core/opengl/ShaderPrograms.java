// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import gnu.trove.map.hash.TIntObjectHashMap;

public final class ShaderPrograms {
    private static ShaderPrograms instance;
    private final TIntObjectHashMap<ShaderProgram> programById = new TIntObjectHashMap<>();

    public static ShaderPrograms getInstance() {
        if (instance == null) {
            instance = new ShaderPrograms();
        }

        return instance;
    }

    private ShaderPrograms() {
        this.programById.setAutoCompactionFactor(0.0F);
    }

    public void registerProgram(ShaderProgram shaderProgram) {
        if (shaderProgram.getShaderID() != 0) {
            this.programById.put(shaderProgram.getShaderID(), shaderProgram);
        }
    }

    public void unregisterProgram(ShaderProgram shaderProgram) {
        if (shaderProgram.getShaderID() != 0) {
            this.programById.remove(shaderProgram.getShaderID());
        }
    }

    public ShaderProgram getProgramByID(int shaderID) {
        return this.programById.get(shaderID);
    }
}
