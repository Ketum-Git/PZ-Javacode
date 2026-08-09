// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

import zombie.IndieGL;
import zombie.core.opengl.GLStateRenderThread;

public final class LightingStyle extends AbstractStyle {
    private static final long serialVersionUID = 1L;
    public static final LightingStyle instance = new LightingStyle();

    @Override
    public void setupState() {
        IndieGL.glBlendFuncA(0, 768);
    }

    @Override
    public void resetState() {
        IndieGL.glBlendFuncA(770, 771);
        GLStateRenderThread.BlendFuncSeparate.restore();
    }

    @Override
    public AlphaOp getAlphaOp() {
        return AlphaOp.KEEP;
    }

    @Override
    public int getStyleID() {
        return 3;
    }

    @Override
    public boolean getRenderSprite() {
        return true;
    }
}
