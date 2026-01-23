// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.rendering;

import org.lwjgl.opengl.GL44;
import org.lwjgl.util.Rectangle;
import org.lwjglx.opengl.Util;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public class RenderTexture extends RenderTarget {
    private static VertexBufferObject fullScreenTri;
    private final RenderTexture.Descriptor descriptor;
    public int colour = -1;
    public int depth = -1;
    public int stencil = -1;
    public int width;
    public int height;
    public int length;
    public int colourFormat = 35907;
    public int depthFormat = 0;
    public boolean depthAsTexture;
    public int wrappingMode = 33071;

    public RenderTexture(String _name) {
        super(_name);
        this.descriptor = new RenderTexture.Descriptor(_name);
    }

    public RenderTexture(RenderTexture.Descriptor desc) {
        super(desc.name);
        this.width = desc.width;
        this.height = desc.height;
        this.length = desc.length;
        this.colourFormat = desc.colourFormat;
        this.depthFormat = desc.depthFormat;
        this.depthAsTexture = desc.depthAsTexture;
        this.wrappingMode = desc.wrappingMode;
        this.descriptor = new RenderTexture.Descriptor(desc);
    }

    @Override
    public int GetWidth() {
        return this.width;
    }

    @Override
    public int GetHeight() {
        return this.height;
    }

    @Override
    protected void OnCreate() {
        if (this.width == 0) {
            this.width = Core.width;
        }

        if (this.height == 0) {
            this.height = Core.height;
        }

        this.buffer = GL44.glGenFramebuffers();
        GL44.glBindFramebuffer(36160, this.buffer);
        if (this.colourFormat != 0) {
            this.CreateColourTexture();
        }

        if (this.depthFormat != 0) {
            this.CreateDepthTexture();
        }
    }

    @Override
    protected void OnDestroy() {
        if (this.colour != -1) {
            GL44.glDeleteTextures(this.colour);
        }

        if (this.depth != -1) {
            if (this.depthAsTexture) {
                GL44.glDeleteTextures(this.depth);
            } else {
                GL44.glDeleteRenderbuffers(this.depth);
            }
        }

        if (this.stencil != -1) {
            GL44.glDeleteTextures(this.stencil);
        }

        GL44.glDeleteFramebuffers(this.buffer);
        this.colour = -1;
        this.depth = -1;
        this.stencil = -1;
        this.buffer = -1;
    }

    @Override
    public void BindRead() {
        super.BindRead();
        GL44.glReadBuffer(36064);
    }

    @Override
    public void BindDraw() {
        super.BindDraw();
        GL44.glDrawBuffer(36064);
    }

    @Override
    public void BindTexture() {
        GL44.glBindTexture(3553, this.colour);
    }

    public void BindDepth() {
        assert this.depthAsTexture && this.depth > 0;

        GL44.glBindTexture(3553, this.depth);
    }

    public void BindStencil() {
        assert this.depthAsTexture && this.stencil > 0;

        GL44.glBindTexture(3553, this.stencil);
    }

    private void Copy(RenderTexture.Descriptor desc) {
        this.width = desc.width;
        this.height = desc.height;
        this.length = desc.length;
        this.colourFormat = desc.colourFormat;
        this.depthFormat = desc.depthFormat;
        this.depthAsTexture = desc.depthAsTexture;
        this.wrappingMode = desc.wrappingMode;
    }

    public void CopyTexture(RenderTarget dest) {
        int b = 0;
        int w = this.width;
        int h = this.height;
        if (dest != null) {
            b = dest.buffer;
            w = dest.GetWidth();
            h = dest.GetHeight();
        }

        GL44.glBindFramebuffer(36008, this.buffer);
        GL44.glBindFramebuffer(36009, b);
        GL44.glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, w, h, 16384, 9728);
    }

    public void CopyTexture(RenderTarget dest, Rectangle srcRect, Rectangle dstRect) {
        GL44.glBindFramebuffer(36008, this.buffer);
        GL44.glBindFramebuffer(36009, dest == null ? 0 : dest.buffer);
        GL44.glBlitFramebuffer(
            srcRect.getX(),
            srcRect.getY(),
            srcRect.getX() + srcRect.getWidth(),
            srcRect.getY() + srcRect.getHeight(),
            dstRect.getX(),
            dstRect.getY(),
            dstRect.getX() + dstRect.getWidth(),
            dstRect.getY() + dstRect.getHeight(),
            16384,
            9728
        );
    }

    public RenderTarget Recreate() {
        if (this.buffer == -1) {
            this.descriptor.Copy(this);
            return this.Create();
        } else {
            int newWidth = this.width == 0 ? Core.width : this.width;
            int newHeight = this.height == 0 ? Core.height : this.height;
            GL44.glBindFramebuffer(36160, this.buffer);
            if (this.colour >= 0) {
                if (this.colourFormat == 0) {
                    GL44.glFramebufferTexture(36160, 36064, 0, 0);
                    GL44.glDeleteTextures(this.colour);
                    this.colour = -1;
                } else if (this.colourFormat != this.descriptor.colourFormat || this.descriptor.width != newWidth || this.descriptor.height != newHeight) {
                    int target = this.length == 0 ? 3553 : '\u8c1a';
                    GL44.glBindTexture(target, this.colour);
                    int type = GetFormatType(this.colourFormat);
                    GL44.glTexStorage2D(target, this.length, this.colourFormat, newWidth, newHeight);
                }
            } else if (this.colourFormat != 0) {
                this.CreateColourTexture();
            }

            if (this.depth >= 0) {
                if (this.depthFormat == 0) {
                    if (this.depthAsTexture) {
                        GL44.glFramebufferTexture(36160, 36096, 0, 0);
                        GL44.glDeleteTextures(this.colour);
                    } else {
                        GL44.glFramebufferRenderbuffer(36160, 36096, 36161, 0);
                        GL44.glDeleteRenderbuffers(this.depth);
                    }

                    this.depth = -1;
                } else if (this.depthFormat != this.descriptor.depthFormat || this.width != newWidth || this.height != newHeight) {
                    GL44.glBindRenderbuffer(36161, this.depth);
                    GL44.glRenderbufferStorage(36161, this.depthFormat, this.width, this.height);
                }
            }

            this.width = newWidth;
            this.height = newHeight;
            this.descriptor.Copy(this);
            return this;
        }
    }

    private void AttachTexture(int attachment, int texture) {
        GL44.glFramebufferTexture2D(36160, attachment, 3553, texture, 0);
    }

    private int CreateTextureOrBuffer(int internalFormat, int attachment, boolean isTexture, int filtering) {
        int result;
        if (isTexture) {
            result = GL44.glGenTextures();
            GL44.glBindTexture(3553, result);
            GL44.glTexStorage2D(3553, 1, internalFormat, this.width, this.height);
            Util.checkGLError();
            GL44.glTexParameteri(3553, 10240, filtering);
            GL44.glTexParameteri(3553, 10241, filtering);
            GL44.glTexParameteri(3553, 10242, this.wrappingMode);
            GL44.glTexParameteri(3553, 10243, this.wrappingMode);
            Util.checkGLError();
            this.AttachTexture(attachment, result);
        } else {
            result = GL44.glGenRenderbuffers();
            GL44.glBindRenderbuffer(36161, result);
            GL44.glRenderbufferStorage(36161, internalFormat, this.width, this.height);
            GL44.glFramebufferRenderbuffer(36160, attachment, 36161, result);
        }

        Util.checkGLError();
        return result;
    }

    private void CreateColourTexture() {
        this.colour = this.CreateTextureOrBuffer(this.colourFormat, 36064, true, 9729);
        Util.checkGLError();
    }

    private void CreateDepthTexture() {
        if (this.depthFormat != 35056 && this.depthFormat != 36013) {
            this.depth = this.CreateTextureOrBuffer(this.depthFormat, 36096, this.depthAsTexture, 9728);
        } else {
            this.depth = this.CreateTextureOrBuffer(this.depthFormat, 33306, this.depthAsTexture, 9728);
            Util.checkGLError();
            if (this.depthAsTexture) {
                this.stencil = GL44.glGenTextures();
                Util.checkGLError();
                GL44.glTextureView(this.stencil, 3553, this.depth, this.depthFormat, 0, 1, 0, 1);
                Util.checkGLError();
                GL44.glBindTexture(3553, this.stencil);
                Util.checkGLError();
                GL44.glTexParameteri(3553, 37098, 6401);
                Util.checkGLError();
            }
        }

        Util.checkGLError();
    }

    public static RenderTexture GetTarget(String name, boolean createIfNull) {
        RenderTarget rt = GetTarget(name);

        assert rt == null || rt instanceof RenderTexture;

        if (rt == null && createIfNull) {
            rt = new RenderTexture(name);
        }

        return (RenderTexture)rt;
    }

    public static RenderTexture GetTexture(RenderTexture.Descriptor desc) {
        RenderTarget rt = GetTarget(desc.name);

        assert rt == null || rt instanceof RenderTexture;

        if (rt == null) {
            return new RenderTexture(desc);
        } else {
            RenderTexture tex = (RenderTexture)rt;
            if (tex.width != desc.width || tex.height != desc.height || tex.colourFormat != desc.colourFormat || tex.depthFormat != desc.depthFormat) {
                tex.Copy(desc);
                tex.Recreate();
            }

            return tex;
        }
    }

    public static class Descriptor {
        public final String name;
        public int width;
        public int height;
        public int length;
        public int colourFormat = 35907;
        public int depthFormat = 0;
        public boolean depthAsTexture;
        public int wrappingMode = 33071;

        public Descriptor(String _name) {
            this.name = _name;
        }

        public Descriptor(RenderTexture.Descriptor desc) {
            this.name = desc.name;
            this.width = desc.width;
            this.height = desc.height;
            this.length = desc.length;
            this.colourFormat = desc.colourFormat;
            this.depthFormat = desc.depthFormat;
            this.depthAsTexture = desc.depthAsTexture;
            this.wrappingMode = desc.wrappingMode;
        }

        private void Copy(RenderTexture rt) {
            this.width = rt.width;
            this.height = rt.height;
            this.length = rt.length;
            this.colourFormat = rt.colourFormat;
            this.depthFormat = rt.depthFormat;
            this.depthAsTexture = rt.depthAsTexture;
            this.wrappingMode = rt.wrappingMode;
        }
    }
}
