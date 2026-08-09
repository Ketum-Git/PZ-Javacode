// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import zombie.core.SpriteRenderer;
import zombie.util.Type;

public final class GLState {
    public static final GLState.CAlphaFunc AlphaFunc = new GLState.CAlphaFunc();
    public static final GLState.CAlphaTest AlphaTest = new GLState.CAlphaTest();
    public static final GLState.CBlend Blend = new GLState.CBlend();
    public static final GLState.CBlendFunc BlendFunc = new GLState.CBlendFunc();
    public static final GLState.CBlendFuncSeparate BlendFuncSeparate = new GLState.CBlendFuncSeparate();
    public static final GLState.CColorMask ColorMask = new GLState.CColorMask();
    public static final GLState.CDepthFunc DepthFunc = new GLState.CDepthFunc();
    public static final GLState.CDepthMask DepthMask = new GLState.CDepthMask();
    public static final GLState.CDepthTest DepthTest = new GLState.CDepthTest();
    public static final GLState.CScissorTest ScissorTest = new GLState.CScissorTest();
    public static final GLState.CStencilFunc StencilFunc = new GLState.CStencilFunc();
    public static final GLState.CStencilMask StencilMask = new GLState.CStencilMask();
    public static final GLState.CStencilOp StencilOp = new GLState.CStencilOp();
    public static final GLState.CStencilTest StencilTest = new GLState.CStencilTest();

    public static void startFrame() {
        AlphaFunc.setDirty();
        AlphaTest.setDirty();
        Blend.setDirty();
        BlendFunc.setDirty();
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

    public abstract static class Base2Ints extends IOpenGLState<GLState.C2IntsValue> {
        GLState.C2IntsValue defaultValue() {
            return new GLState.C2IntsValue();
        }
    }

    public abstract static class Base3Ints extends IOpenGLState<GLState.C3IntsValue> {
        GLState.C3IntsValue defaultValue() {
            return new GLState.C3IntsValue();
        }
    }

    public abstract static class Base4Booleans extends IOpenGLState<GLState.C4BooleansValue> {
        GLState.C4BooleansValue defaultValue() {
            return new GLState.C4BooleansValue();
        }
    }

    public abstract static class Base4Ints extends IOpenGLState<GLState.C4IntsValue> {
        GLState.C4IntsValue defaultValue() {
            return new GLState.C4IntsValue();
        }
    }

    public abstract static class BaseBoolean extends IOpenGLState<GLState.CBooleanValue> {
        GLState.CBooleanValue defaultValue() {
            return new GLState.CBooleanValue(true);
        }
    }

    public abstract static class BaseInt extends IOpenGLState<GLState.CIntValue> {
        GLState.CIntValue defaultValue() {
            return new GLState.CIntValue();
        }
    }

    public abstract static class BaseIntFloat extends IOpenGLState<GLState.CIntFloatValue> {
        GLState.CIntFloatValue defaultValue() {
            return new GLState.CIntFloatValue();
        }
    }

    public static final class C2IntsValue implements IOpenGLState.Value {
        int a;
        int b;

        public GLState.C2IntsValue set(int a, int b) {
            this.a = a;
            this.b = b;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            GLState.C2IntsValue rhs = Type.tryCastTo(other, GLState.C2IntsValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            GLState.C2IntsValue rhs = (GLState.C2IntsValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            return this;
        }
    }

    public static final class C3IntsValue implements IOpenGLState.Value {
        int a;
        int b;
        int c;

        public GLState.C3IntsValue set(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            GLState.C3IntsValue rhs = Type.tryCastTo(other, GLState.C3IntsValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b && rhs.c == this.c;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            GLState.C3IntsValue rhs = (GLState.C3IntsValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            this.c = rhs.c;
            return this;
        }
    }

    public static final class C4BooleansValue implements IOpenGLState.Value {
        boolean a;
        boolean b;
        boolean c;
        boolean d;

        public GLState.C4BooleansValue set(boolean a, boolean b, boolean c, boolean d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            GLState.C4BooleansValue rhs = Type.tryCastTo(other, GLState.C4BooleansValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b && rhs.c == this.c && rhs.d == this.d;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            GLState.C4BooleansValue rhs = (GLState.C4BooleansValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            this.c = rhs.c;
            this.d = rhs.d;
            return this;
        }
    }

    public static final class C4IntsValue implements IOpenGLState.Value {
        int a;
        int b;
        int c;
        int d;

        public GLState.C4IntsValue set(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            GLState.C4IntsValue rhs = Type.tryCastTo(other, GLState.C4IntsValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b && rhs.c == this.c && rhs.d == this.d;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            GLState.C4IntsValue rhs = (GLState.C4IntsValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            this.c = rhs.c;
            this.d = rhs.d;
            return this;
        }
    }

    public static final class CAlphaFunc extends GLState.BaseIntFloat {
        void Set(GLState.CIntFloatValue value) {
            SpriteRenderer.instance.glAlphaFunc(value.a, value.b);
        }
    }

    public static final class CAlphaTest extends GLState.BaseBoolean {
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(3008);
            } else {
                SpriteRenderer.instance.glDisable(3008);
            }
        }
    }

    public static final class CBlend extends GLState.BaseBoolean {
        CBlend() {
            this.currentValue.value = true;
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(3042);
            } else {
                SpriteRenderer.instance.glDisable(3042);
            }
        }
    }

    public static final class CBlendFunc extends GLState.Base2Ints {
        void Set(GLState.C2IntsValue value) {
            SpriteRenderer.instance.glBlendFunc(value.a, value.b);
        }
    }

    public static final class CBlendFuncSeparate extends GLState.Base4Ints {
        void Set(GLState.C4IntsValue value) {
            SpriteRenderer.instance.glBlendFuncSeparate(value.a, value.b, value.c, value.d);
        }
    }

    public static class CBooleanValue implements IOpenGLState.Value {
        public static final GLState.CBooleanValue TRUE = new GLState.CBooleanValue(true);
        public static final GLState.CBooleanValue FALSE = new GLState.CBooleanValue(false);
        boolean value;

        CBooleanValue(boolean value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GLState.CBooleanValue cBooleanValue && cBooleanValue.value == this.value;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            this.value = ((GLState.CBooleanValue)other).value;
            return this;
        }
    }

    public static final class CColorMask extends GLState.Base4Booleans {
        void Set(GLState.C4BooleansValue value) {
            SpriteRenderer.instance.glColorMask(value.a ? 1 : 0, value.b ? 1 : 0, value.c ? 1 : 0, value.d ? 1 : 0);
        }
    }

    public static final class CDepthFunc extends GLState.BaseInt {
        CDepthFunc() {
            this.currentValue.value = 513;
        }

        void Set(GLState.CIntValue value) {
            SpriteRenderer.instance.glDepthFunc(value.value);
        }
    }

    public static final class CDepthMask extends GLState.BaseBoolean {
        void Set(GLState.CBooleanValue value) {
            SpriteRenderer.instance.glDepthMask(value.value);
        }
    }

    public static final class CDepthTest extends GLState.BaseBoolean {
        CDepthTest() {
            this.currentValue.value = false;
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(2929);
            } else {
                SpriteRenderer.instance.glDisable(2929);
            }
        }
    }

    public static final class CIntFloatValue implements IOpenGLState.Value {
        int a;
        float b;

        public GLState.CIntFloatValue set(int a, float b) {
            this.a = a;
            this.b = b;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            GLState.CIntFloatValue rhs = Type.tryCastTo(other, GLState.CIntFloatValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            GLState.CIntFloatValue rhs = (GLState.CIntFloatValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            return this;
        }
    }

    public static class CIntValue implements IOpenGLState.Value {
        int value;

        public GLState.CIntValue set(int a) {
            this.value = a;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GLState.CIntValue cIntValue && cIntValue.value == this.value;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            this.value = ((GLState.CIntValue)other).value;
            return this;
        }
    }

    public static final class CScissorTest extends GLState.BaseBoolean {
        CScissorTest() {
            this.currentValue.value = false;
        }

        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(3089);
            } else {
                SpriteRenderer.instance.glDisable(3089);
            }
        }
    }

    public static final class CStencilFunc extends GLState.Base3Ints {
        void Set(GLState.C3IntsValue value) {
            SpriteRenderer.instance.glStencilFunc(value.a, value.b, value.c);
        }
    }

    public static final class CStencilMask extends GLState.BaseInt {
        CStencilMask() {
            this.currentValue.value = 255;
        }

        void Set(GLState.CIntValue value) {
            SpriteRenderer.instance.glStencilMask(value.value);
        }
    }

    public static final class CStencilOp extends GLState.Base3Ints {
        void Set(GLState.C3IntsValue value) {
            SpriteRenderer.instance.glStencilOp(value.a, value.b, value.c);
        }
    }

    public static final class CStencilTest extends GLState.BaseBoolean {
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(2960);
            } else {
                SpriteRenderer.instance.glDisable(2960);
            }
        }
    }
}
