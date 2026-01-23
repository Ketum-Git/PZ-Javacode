// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.Stack;
import org.lwjgl.opengl.GL11;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLState;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.Lambda;
import zombie.util.lambda.Invokers;

public final class IndieGL {
    public static int nCount;
    private static final GLState.CIntValue tempInt = new GLState.CIntValue();
    private static final GLState.C2IntsValue temp2Ints = new GLState.C2IntsValue();
    private static final GLState.C3IntsValue temp3Ints = new GLState.C3IntsValue();
    private static final GLState.C4IntsValue temp4Ints = new GLState.C4IntsValue();
    private static final GLState.C4BooleansValue temp4Booleans = new GLState.C4BooleansValue();
    private static final GLState.CIntFloatValue tempIntFloat = new GLState.CIntFloatValue();
    private static final Stack<ShaderStackEntry> m_shaderStack = new Stack<>();

    public static void glBlendFunc(int a, int b) {
        if (SpriteRenderer.glBlendfuncEnabled) {
            GLState.BlendFuncSeparate.set(temp4Ints.set(a, b, a, b));
        }
    }

    public static void glBlendFuncSeparate(int a, int b, int c, int d) {
        if (SpriteRenderer.glBlendfuncEnabled) {
            GLState.BlendFuncSeparate.set(temp4Ints.set(a, b, c, d));
        }
    }

    public static void restoreMainThreadValue_glBlendFuncSeparate() {
    }

    public static void glDefaultBlendFunc() {
        glBlendFunc(1, 771);
    }

    public static void glDefaultBlendFuncA() {
        GL11.glBlendFunc(1, 771);
    }

    public static void glDepthFunc(int a) {
        GLState.DepthFunc.set(tempInt.set(a));
    }

    public static void glDepthMask(boolean b) {
        GLState.DepthMask.set(b ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
    }

    public static void StartShader(Shader shader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        StartShader(shader, playerIndex);
    }

    public static void StartShader(Shader shader, int playerIndex) {
        if (shader != null) {
            StartShader(shader.getID(), playerIndex);
        } else {
            EndShader();
        }
    }

    public static void StartShader(int ID) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        StartShader(ID, playerIndex);
    }

    public static void StartShader(int ID, int playerIndex) {
        SpriteRenderer.instance.StartShader(ID, playerIndex);
    }

    public static void StartShader(Shader shader, ShaderUniformSetter uniforms) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        StartShader(shader, playerIndex, uniforms);
    }

    public static void StartShader(Shader shader, int playerIndex, ShaderUniformSetter uniforms) {
        if (shader != null) {
            StartShader(shader.getID(), playerIndex, uniforms);
        } else {
            EndShader();
        }
    }

    public static void StartShader(int ID, ShaderUniformSetter uniforms) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        StartShader(ID, playerIndex, uniforms);
    }

    public static void StartShader(int ID, int playerIndex, ShaderUniformSetter uniforms) {
        SpriteRenderer.instance.StartShader(ID, playerIndex, uniforms);
    }

    public static void EndShader() {
        SpriteRenderer.instance.EndShader();
    }

    public static void pushShader(Shader shader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        m_shaderStack.push(ShaderStackEntry.alloc(shader, playerIndex));
        StartShader(shader, playerIndex);
    }

    public static void pushShader(Shader shader, ShaderUniformSetter uniforms) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        m_shaderStack.push(ShaderStackEntry.alloc(shader, playerIndex));
        StartShader(shader, playerIndex, uniforms);
    }

    public static void popShader(Shader shader) {
        if (m_shaderStack.isEmpty()) {
            throw new RuntimeException("Push/PopShader mismatch. Cannot pop. Stack is empty.");
        } else if (m_shaderStack.peek().getShader() != shader) {
            throw new RuntimeException("Push/PopShader mismatch. The popped shader != the pushed shader.");
        } else {
            ShaderStackEntry topEntry = m_shaderStack.pop();
            topEntry.release();
            if (m_shaderStack.isEmpty()) {
                EndShader();
            } else {
                ShaderStackEntry nextTopEntry = m_shaderStack.peek();
                StartShader(nextTopEntry.getShader(), nextTopEntry.getPlayerIndex());
            }
        }
    }

    public static void bindShader(Shader shader, Runnable invoke) {
        pushShader(shader);

        try {
            invoke.run();
        } finally {
            popShader(shader);
        }
    }

    public static <T1> void bindShader(Shader shader, T1 val1, Invokers.Params1.ICallback<T1> invoker) {
        Lambda.capture(shader, val1, invoker, (stack, l_shader, l_val1, l_invoker) -> bindShader(l_shader, stack.invoker(l_val1, l_invoker)));
    }

    public static <T1, T2> void bindShader(Shader shader, T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
        Lambda.capture(
            shader, val1, val2, invoker, (stack, l_shader, l_val1, l_val2, l_invoker) -> bindShader(l_shader, stack.invoker(l_val1, l_val2, l_invoker))
        );
    }

    public static <T1, T2, T3> void bindShader(Shader shader, T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
        Lambda.capture(
            shader,
            val1,
            val2,
            val3,
            invoker,
            (stack, l_shader, l_val1, l_val2, l_val3, l_invoker) -> bindShader(l_shader, stack.invoker(l_val1, l_val2, l_val3, l_invoker))
        );
    }

    public static <T1, T2, T3, T4> void bindShader(Shader shader, T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
        Lambda.capture(
            shader,
            val1,
            val2,
            val3,
            val4,
            invoker,
            (stack, l_shader, l_val1, l_val2, l_val3, l_val4, l_invoker) -> bindShader(l_shader, stack.invoker(l_val1, l_val2, l_val3, l_val4, l_invoker))
        );
    }

    private static ShaderProgram.Uniform getShaderUniform(Shader shader, String uniformName, int uniformType) {
        if (shader == null) {
            return null;
        } else {
            ShaderProgram program = shader.getProgram();
            return program == null ? null : program.getUniform(uniformName, uniformType, false);
        }
    }

    public static void shaderSetSamplerUnit(Shader shader, String loc, int textureUnit) {
        ShaderProgram.Uniform u = getShaderUniform(shader, loc, 35678);
        if (u != null) {
            u.sampler = textureUnit;
            ShaderUpdate1i(shader.getID(), u.loc, textureUnit);
        }
    }

    public static void shaderSetValue(Shader shader, String loc, float val) {
        ShaderProgram.Uniform u = getShaderUniform(shader, loc, 5126);
        if (u != null) {
            ShaderUpdate1f(shader.getID(), u.loc, val);
        }
    }

    public static void shaderSetValue(Shader shader, String loc, int val) {
        ShaderProgram.Uniform u = getShaderUniform(shader, loc, 5124);
        if (u != null) {
            ShaderUpdate1i(shader.getID(), u.loc, val);
        }
    }

    public static void shaderSetValue(Shader shader, String loc, Vector2 val) {
        shaderSetVector2(shader, loc, val.x, val.y);
    }

    public static void shaderSetValue(Shader shader, String loc, Vector3 val) {
        shaderSetVector3(shader, loc, val.x, val.y, val.z);
    }

    public static void shaderSetVector2(Shader shader, String loc, float val_x, float val_y) {
        ShaderProgram.Uniform u = getShaderUniform(shader, loc, 35664);
        if (u != null) {
            ShaderUpdate2f(shader.getID(), u.loc, val_x, val_y);
        }
    }

    public static void shaderSetVector3(Shader shader, String loc, float val_x, float val_y, float val_z) {
        ShaderProgram.Uniform u = getShaderUniform(shader, loc, 35665);
        if (u != null) {
            ShaderUpdate3f(shader.getID(), u.loc, val_x, val_y, val_z);
        }
    }

    public static void shaderSetVector4(Shader shader, String loc, float val_x, float val_y, float val_z, float val_w) {
        ShaderProgram.Uniform u = getShaderUniform(shader, loc, 35666);
        if (u != null) {
            ShaderUpdate4f(shader.getID(), u.loc, val_x, val_y, val_z, val_w);
        }
    }

    public static void ShaderUpdate1i(int shaderID, int uniform, int uniformValue) {
        SpriteRenderer.instance.ShaderUpdate1i(shaderID, uniform, uniformValue);
    }

    public static void ShaderUpdate1f(int shaderID, int uniform, float uniformValue) {
        SpriteRenderer.instance.ShaderUpdate1f(shaderID, uniform, uniformValue);
    }

    public static void ShaderUpdate2f(int shaderID, int uniform, float value1, float value2) {
        SpriteRenderer.instance.ShaderUpdate2f(shaderID, uniform, value1, value2);
    }

    public static void ShaderUpdate3f(int shaderID, int uniform, float value1, float value2, float value3) {
        SpriteRenderer.instance.ShaderUpdate3f(shaderID, uniform, value1, value2, value3);
    }

    public static void ShaderUpdate4f(int shaderID, int uniform, float value1, float value2, float value3, float value4) {
        SpriteRenderer.instance.ShaderUpdate4f(shaderID, uniform, value1, value2, value3, value4);
    }

    public static void glBlendFuncA(int a, int b) {
        GL11.glBlendFunc(a, b);
    }

    public static void glEnable(int a) {
        if (a == 3008) {
            enableAlphaTest();
        } else if (a == 3042) {
            enableBlend();
        } else if (a == 2929) {
            enableDepthTest();
        } else if (a == 3089) {
            enableScissorTest();
        } else if (a == 2960) {
            enableStencilTest();
        } else {
            SpriteRenderer.instance.glEnable(a);
        }
    }

    public static void glDoStartFrame(int w, int h, float zoom, int player) {
        glDoStartFrame(w, h, zoom, player, false);
    }

    public static void glDoStartFrame(int w, int h, float zoom, int player, boolean isTextFrame) {
        SpriteRenderer.instance.glDoStartFrame(w, h, zoom, player, isTextFrame);
    }

    public static void glDoEndFrame() {
        SpriteRenderer.instance.glDoEndFrame();
    }

    public static void glColorMask(boolean bln, boolean bln1, boolean bln2, boolean bln3) {
        GLState.ColorMask.set(temp4Booleans.set(bln, bln1, bln2, bln3));
    }

    public static void glColorMaskA(boolean bln, boolean bln1, boolean bln2, boolean bln3) {
        GL11.glColorMask(bln, bln, bln3, bln3);
    }

    public static void glEnableA(int a) {
        GL11.glEnable(a);
    }

    public static void glAlphaFunc(int a, float b) {
        if (SpriteRenderer.glBlendfuncEnabled) {
            GLState.AlphaFunc.set(tempIntFloat.set(a, b));
        }
    }

    public static void glAlphaFuncA(int a, float b) {
        GL11.glAlphaFunc(a, b);
    }

    public static void glStencilFunc(int a, int b, int c) {
        GLState.StencilFunc.set(temp3Ints.set(a, b, c));
    }

    public static void glStencilFuncA(int a, int b, int c) {
        GL11.glStencilFunc(a, b, c);
    }

    public static void glStencilOp(int a, int b, int c) {
        GLState.StencilOp.set(temp3Ints.set(a, b, c));
    }

    public static void glStencilOpA(int a, int b, int c) {
        GL11.glStencilOp(a, b, c);
    }

    public static void glTexParameteri(int a, int b, int c) {
        SpriteRenderer.instance.glTexParameteri(a, b, c);
    }

    public static void glTexParameteriActual(int glTexture2d, int glTextureMagFilter, int glLinear) {
        GL11.glTexParameteri(glTexture2d, glTextureMagFilter, glLinear);
    }

    public static void glStencilMask(int a) {
        GLState.StencilMask.set(tempInt.set(a));
    }

    public static void glStencilMaskA(int a) {
        GL11.glStencilMask(a);
    }

    public static void glDisable(int a) {
        if (a == 3008) {
            disableAlphaTest();
        } else if (a == 3042) {
            disableBlend();
        } else if (a == 2929) {
            disableDepthTest();
        } else if (a == 3089) {
            disableScissorTest();
        } else if (a == 2960) {
            disableStencilTest();
        } else {
            SpriteRenderer.instance.glDisable(a);
        }
    }

    public static void glClear(int a) {
        SpriteRenderer.instance.glClear(a);
    }

    public static void glClearA(int a) {
        GL11.glClear(a);
    }

    public static void glDisableA(int a) {
        GL11.glDisable(a);
    }

    public static void glLoadIdentity() {
        SpriteRenderer.instance.glLoadIdentity();
    }

    public static void glBind(Texture offscreenTexture) {
        SpriteRenderer.instance.glBind(offscreenTexture.getID());
    }

    public static void enableAlphaTest() {
        GLState.AlphaTest.set(GLState.CBooleanValue.TRUE);
    }

    public static void disableAlphaTest() {
        GLState.AlphaTest.set(GLState.CBooleanValue.FALSE);
    }

    public static void enableBlend() {
        GLState.Blend.set(GLState.CBooleanValue.TRUE);
    }

    public static void disableBlend() {
        GLState.Blend.set(GLState.CBooleanValue.FALSE);
    }

    public static void enableDepthTest() {
        GLState.DepthTest.set(GLState.CBooleanValue.TRUE);
    }

    public static void disableDepthTest() {
        GLState.DepthTest.set(GLState.CBooleanValue.FALSE);
    }

    public static void enableScissorTest() {
        GLState.ScissorTest.set(GLState.CBooleanValue.TRUE);
    }

    public static void disableScissorTest() {
        GLState.ScissorTest.set(GLState.CBooleanValue.FALSE);
    }

    public static void enableStencilTest() {
        GLState.StencilTest.set(GLState.CBooleanValue.TRUE);
    }

    public static void disableStencilTest() {
        GLState.StencilTest.set(GLState.CBooleanValue.FALSE);
    }

    public static boolean isMaxZoomLevel() {
        return SpriteRenderer.instance.isMaxZoomLevel();
    }

    public static boolean isMinZoomLevel() {
        return SpriteRenderer.instance.isMinZoomLevel();
    }
}
