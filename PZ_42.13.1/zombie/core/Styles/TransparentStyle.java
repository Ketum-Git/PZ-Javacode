// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

import zombie.IndieGL;
import zombie.core.opengl.GLStateRenderThread;

public final class TransparentStyle extends AbstractStyle {
    private static final long serialVersionUID = 1L;
    public static final TransparentStyle instance = new TransparentStyle();

    @Override
    public void setupState() {
        IndieGL.glBlendFuncA(770, 771);
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
        return 2;
    }

    @Override
    public boolean getRenderSprite() {
        return true;
    }
}
