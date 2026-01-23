// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class GLStateRenderThread {
    private static final GLState.C4BooleansValue temp4BooleansValue = new GLState.C4BooleansValue();
    private static final GLState.C3IntsValue temp3IntsValue = new GLState.C3IntsValue();
    private static final GLState.C4IntsValue temp4IntsValue = new GLState.C4IntsValue();
    private static final GLState.CIntFloatValue tempIntFloatValue = new GLState.CIntFloatValue();
    private static final GLState.CIntValue tempIntValue = new GLState.CIntValue();
    public static final GLStateRenderThread.CAlphaFunc AlphaFunc = new GLStateRenderThread.CAlphaFunc();
    public static final GLStateRenderThread.CAlphaTest AlphaTest = new GLStateRenderThread.CAlphaTest();
    public static final GLStateRenderThread.CBlend Blend = new GLStateRenderThread.CBlend();
    public static final GLStateRenderThread.CBlendFuncSeparate BlendFuncSeparate = new GLStateRenderThread.CBlendFuncSeparate();
    public static final GLStateRenderThread.CColorMask ColorMask = new GLStateRenderThread.CColorMask();
    public static final GLStateRenderThread.CDepthFunc DepthFunc = new GLStateRenderThread.CDepthFunc();
    public static final GLStateRenderThread.CDepthMask DepthMask = new GLStateRenderThread.CDepthMask();
    public static final GLStateRenderThread.CDepthTest DepthTest = new GLStateRenderThread.CDepthTest();
    public static final GLStateRenderThread.CScissorTest ScissorTest = new GLStateRenderThread.CScissorTest();
    public static final GLStateRenderThread.CStencilFunc StencilFunc = new GLStateRenderThread.CStencilFunc();
    public static final GLStateRenderThread.CStencilMask StencilMask = new GLStateRenderThread.CStencilMask();
    public static final GLStateRenderThread.CStencilOp StencilOp = new GLStateRenderThread.CStencilOp();
    public static final GLStateRenderThread.CStencilTest StencilTest = new GLStateRenderThread.CStencilTest();

    public static void startFrame() {
        AlphaFunc.setDirty();
        AlphaTest.setDirty();
        Blend.setDirty();
        BlendFuncSeparate.setDirty();
        ColorMask.setDirty();
        DepthFunc.setDirty();
        DepthMask.setDirty();
        DepthTest.setDirty();
        ScissorTest.setDirty();
        StencilFunc.setDirty();
        StencilMask.setDirty();
        StencilOp.setDirty();
        StencilTest.setDirty();
    }

    public static void restore() {
        AlphaFunc.restore();
        AlphaTest.restore();
        Blend.restore();
        BlendFuncSeparate.restore();
        ColorMask.restore();
        DepthFunc.restore();
        DepthMask.restore();
        DepthTest.restore();
        ScissorTest.restore();
        StencilFunc.restore();
        StencilMask.restore();
        StencilOp.restore();
        StencilTest.restore();
    }

    public static final class CAlphaFunc extends GLState.BaseIntFloat {
        public void set(int a, float b) {
            this.set(GLStateRenderThread.tempIntFloatValue.set(a, b));
        }

        void Set(GLState.CIntFloatValue value) {
            GL11.glAlphaFunc(value.a, value.b);
        }
    }

    public static final class CAlphaTest extends GLState.BaseBoolean {
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(3008);
            } else {
                GL11.glDisable(3008);
            }
        }
    }

    public static final class CBlend extends GLState.BaseBoolean {
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(3042);
            } else {
                GL11.glDisable(3042);
            }
        }
    }

    public static final class CBlendFuncSeparate extends GLState.Base4Ints {
        public void set(int a, int b, int c, int d) {
            this.set(GLStateRenderThread.temp4IntsValue.set(a, b, c, d));
        }

        @Override
        public void restore() {
            this.Set(this.getCurrentValue());
        }

        void Set(GLState.C4IntsValue value) {
            GL14.glBlendFuncSeparate(value.a, value.b, value.c, value.d);
        }
    }

    public static final class CColorMask extends GLState.Base4Booleans {
        public void set(boolean a, boolean b, boolean c, boolean d) {
            this.set(GLStateRenderThread.temp4BooleansValue.set(a, b, c, d));
        }

        void Set(GLState.C4BooleansValue value) {
            GL11.glColorMask(value.a, value.b, value.c, value.d);
        }
    }

    public static final class CDepthFunc extends GLState.BaseInt {
        public CDepthFunc() {
            this.currentValue.value = 513;
        }

        public void set(int a) {
            this.set(GLStateRenderThread.tempIntValue.set(a));
        }

        void Set(GLState.CIntValue value) {
            GL11.glDepthFunc(value.value);
        }
    }

    public static final class CDepthMask extends GLState.BaseBoolean {
        public void set(boolean b) {
            this.set(b ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        void Set(GLState.CBooleanValue value) {
            GL11.glDepthMask(value.value);
        }
    }

    public static final class CDepthTest extends GLState.BaseBoolean {
        public CDepthTest() {
            this.currentValue.value = false;
        }

        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(2929);
            } else {
                GL11.glDisable(2929);
            }
        }
    }

    public static final class CScissorTest extends GLState.BaseBoolean {
        public CScissorTest() {
            this.currentValue.value = false;
        }

        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(3089);
            } else {
                GL11.glDisable(3089);
            }
        }
    }

    public static final class CStencilFunc extends GLState.Base3Ints {
        public CStencilFunc() {
            this.currentValue.a = 519;
            this.currentValue.b = 0;
            this.currentValue.c = 0;
        }

        public void set(int a, int b, int c) {
            this.set(GLStateRenderThread.temp3IntsValue.set(a, b, c));
        }

        void Set(GLState.C3IntsValue value) {
            GL11.glStencilFunc(value.a, value.b, value.c);
        }
    }

    public static final class CStencilMask extends GLState.BaseInt {
        public CStencilMask() {
            this.currentValue.value = -1;
        }

        public void set(int a) {
            this.set(GLStateRenderThread.tempIntValue.set(a));
        }

        void Set(GLState.CIntValue value) {
            GL11.glStencilMask(value.value);
        }
    }

    public static final class CStencilOp extends GLState.Base3Ints {
        public void set(int a, int b, int c) {
            this.set(GLStateRenderThread.temp3IntsValue.set(a, b, c));
        }

        void Set(GLState.C3IntsValue value) {
            GL11.glStencilOp(value.a, value.b, value.c);
        }
    }

    public static final class CStencilTest extends GLState.BaseBoolean {
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(2960);
            } else {
                GL11.glDisable(2960);
            }
        }
    }
}
