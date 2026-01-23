// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.rendering;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.Util;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;

public abstract class RenderTarget {
    private static final ArrayList<RenderTarget> ActiveRenderTargets = new ArrayList<>();
    private static VertexBufferObject fullScreenTri;
    private static VertexBufferObject fullScreenQuad;
    public final String name;
    public int buffer = -1;
    private boolean created;

    protected RenderTarget(String _name) {
        this.name = _name;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", super.toString(), this.name);
    }

    private static VertexBufferObject GetFullScreenTri() {
        if (fullScreenTri == null) {
            VertexBufferObject.VertexFormat format = new VertexBufferObject.VertexFormat(2);
            format.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
            format.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
            format.calculate();
            VertexBufferObject.VertexArray array = new VertexBufferObject.VertexArray(format, 3);
            array.setElement(0, 0, -1.0F, -1.0F, 0.0F);
            array.setElement(0, 1, 0.0F, 0.0F);
            array.setElement(1, 0, -1.0F, 3.0F, 0.0F);
            array.setElement(1, 1, 0.0F, 2.0F);
            array.setElement(2, 0, 3.0F, -1.0F, 0.0F);
            array.setElement(2, 1, 2.0F, 0.0F);
            fullScreenTri = new VertexBufferObject(array, new int[]{0, 1, 2});
        }

        return fullScreenTri;
    }

    private static VertexBufferObject GetFullScreenQuad() {
        if (fullScreenQuad == null) {
            VertexBufferObject.VertexFormat format = new VertexBufferObject.VertexFormat(2);
            format.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
            format.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
            format.calculate();
            VertexBufferObject.VertexArray array = new VertexBufferObject.VertexArray(format, 4);
            array.setElement(0, 0, -1.0F, -1.0F, 0.0F);
            array.setElement(0, 1, 0.0F, 0.0F);
            array.setElement(1, 0, -1.0F, 1.0F, 0.0F);
            array.setElement(1, 1, 0.0F, 1.0F);
            array.setElement(2, 0, 1.0F, -1.0F, 0.0F);
            array.setElement(2, 1, 1.0F, 0.0F);
            array.setElement(3, 0, 1.0F, 1.0F, 0.0F);
            array.setElement(3, 1, 1.0F, 1.0F);
            fullScreenQuad = new VertexBufferObject(array, new int[]{0, 1, 2, 1, 3, 2});
        }

        return fullScreenQuad;
    }

    public static RenderTarget GetTarget(String name) {
        for (RenderTarget rt : ActiveRenderTargets) {
            if (rt.name.equals(name)) {
                return rt;
            }
        }

        return null;
    }

    public static void UnbindTarget() {
        GL30.glBindFramebuffer(36160, 0);
    }

    public final RenderTarget Create() {
        if (this.created) {
            return this;
        } else {
            this.OnCreate();
            int status = GL30.glCheckFramebufferStatus(36160);
            if (status != 36053) {
                this.Destroy();

                String message = switch (status) {
                    case 36054 -> "Incomplete attachment";
                    case 36055 -> "Incomplete missing attachment";
                    case 36059 -> "Incomplete draw buffer";
                    case 36060 -> "Incomplete read buffer";
                    case 36061 -> "Format combination is unsupported";
                    case 36182 -> "Number of samples do not match";
                    case 36264 -> "Texture not layered or from different target";
                    default -> "Unknown error";
                };
                throw new OpenGLException("Failed to create framebuffer - " + message);
            } else {
                Util.checkGLError();
                this.created = true;
                ActiveRenderTargets.add(this);
                return this;
            }
        }
    }

    public final void Destroy() {
        this.OnDestroy();
        ActiveRenderTargets.remove(this);
        this.created = false;
    }

    public void BindRead() {
        assert this.buffer != -1;

        GL30.glBindFramebuffer(36008, this.buffer);
    }

    public void BindDraw() {
        assert this.buffer != -1;

        GL30.glBindFramebuffer(36009, this.buffer);
        GL30.glViewport(0, 0, this.GetWidth(), this.GetHeight());
    }

    protected static int GetFormatType(int format) {
        return switch (format) {
            case 32852, 32859, 33189, 33322, 33324, 33332, 33338, 36214, 36215 -> 5123;
            case 33191, 33334, 33340, 36208, 36209 -> 5125;
            case 33325, 33327, 34842, 34843 -> 5131;
            case 33326, 33328, 34836, 34837, 36012 -> 5126;
            case 33329, 33335, 36238, 36239, 36756, 36757, 36758, 36759 -> 5120;
            case 33331, 33337, 36232, 36233, 36760, 36761, 36762, 36763 -> 5122;
            case 33333, 33339, 36226, 36227 -> 5124;
            case 35056 -> 34042;
            case 36013 -> 36269;
            default -> 5121;
        };
    }

    protected static int GetInternalFormat(int format) {
        return switch (format) {
            case 32852, 32859, 33322, 33324, 33332, 33338, 36214, 36215 -> 5123;
            case 33325, 33327, 34842, 34843 -> 5131;
            case 33326, 33328, 34836, 34837 -> 5126;
            case 33329, 33335, 36238, 36239, 36756, 36757, 36758, 36759 -> 5120;
            case 33331, 33337, 36232, 36233, 36760, 36761, 36762, 36763 -> 5122;
            case 33333, 33339, 36226, 36227 -> 5124;
            case 33334, 33340, 36208, 36209 -> 5125;
            default -> 6408;
        };
    }

    public abstract int GetWidth();

    public abstract int GetHeight();

    protected abstract void OnCreate();

    protected abstract void OnDestroy();

    public abstract void BindTexture();

    public void Blit(RenderTarget dest) {
        this.Blit(dest, null);
    }

    public void Blit(RenderTarget dest, Shader shader) {
        if (shader == null) {
            shader = ShaderManager.instance.getOrCreateShader("blit", false, false);
        }

        if (dest == null) {
            GL30.glBindFramebuffer(36009, 0);
        } else {
            dest.BindDraw();
        }

        GL20.glUseProgram(shader.getShaderProgram().getShaderID());
        GL20.glActiveTexture(33984);
        this.BindTexture();
        DrawFullScreenTri();
    }

    public static void DrawFullScreenTri() {
        DrawVBO(GetFullScreenTri());
    }

    public static void DrawFullScreenQuad() {
        DrawVBO(GetFullScreenQuad());
    }

    private static void DrawVBO(VertexBufferObject vbo) {
        GL11.glPushAttrib(8);
        GL11.glPushClientAttrib(2);
        GL11.glDisable(2884);
        vbo.Draw(null);
        GL11.glPopClientAttrib();
        GL11.glPopAttrib();
    }
}
