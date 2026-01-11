// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import zombie.core.opengl.Shader;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ShaderStackEntry extends PooledObject {
    private Shader shader;
    private int playerIndex;
    private static final Pool<ShaderStackEntry> s_pool = new Pool<>(ShaderStackEntry::new);

    public Shader getShader() {
        return this.shader;
    }

    public int getPlayerIndex() {
        return this.playerIndex;
    }

    public static ShaderStackEntry alloc(Shader shader, int playerIndex) {
        ShaderStackEntry newEntry = s_pool.alloc();
        newEntry.shader = shader;
        newEntry.playerIndex = playerIndex;
        return newEntry;
    }
}
