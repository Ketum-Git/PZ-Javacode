// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

import zombie.IndieGL;
import zombie.core.opengl.GLStateRenderThread;

public final class AdditiveStyle extends AbstractStyle {
    private static final long serialVersionUID = 1L;
    public static final AdditiveStyle instance = new AdditiveStyle();

    @Override
    public void setupState() {
        IndieGL.glBlendFuncA(1, 771);
    }

    @Override
    public void resetState() {
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
