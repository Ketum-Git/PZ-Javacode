// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;

public interface IsoRenderable {
    void setDoRender(boolean arg0);

    boolean getDoRender();

    void setSceneCulled(boolean arg0);

    boolean isSceneCulled();

    void render(float arg0, float arg1, float arg2, ColorInfo arg3, boolean arg4, boolean arg5, Shader arg6);
}
