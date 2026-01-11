// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import org.lwjgl.opengl.ARBFramebufferObject;

public final class GLFramebufferObjectARB implements IGLFramebufferObject {
    @Override
    public int GL_FRAMEBUFFER() {
        return 36160;
    }

    @Override
    public int GL_RENDERBUFFER() {
        return 36161;
    }

    @Override
    public int GL_COLOR_ATTACHMENT0() {
        return 36064;
    }

    @Override
    public int GL_DEPTH_ATTACHMENT() {
        return 36096;
    }

    @Override
    public int GL_STENCIL_ATTACHMENT() {
        return 36128;
    }

    @Override
    public int GL_DEPTH_STENCIL() {
        return 34041;
    }

    @Override
    public int GL_DEPTH24_STENCIL8() {
        return 35056;
    }

    @Override
    public int GL_FRAMEBUFFER_COMPLETE() {
        return 36053;
    }

    @Override
    public int GL_FRAMEBUFFER_UNDEFINED() {
        return 33305;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT() {
        return 36054;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT() {
        return 36055;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS() {
        return 0;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_FORMATS() {
        return 0;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER() {
        return 36059;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER() {
        return 36060;
    }

    @Override
    public int GL_FRAMEBUFFER_UNSUPPORTED() {
        return 36061;
    }

    @Override
    public int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE() {
        return 36182;
    }

    @Override
    public int glGenFramebuffers() {
        return ARBFramebufferObject.glGenFramebuffers();
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        ARBFramebufferObject.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        ARBFramebufferObject.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenRenderbuffers() {
        return ARBFramebufferObject.glGenRenderbuffers();
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        ARBFramebufferObject.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        ARBFramebufferObject.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        ARBFramebufferObject.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return ARBFramebufferObject.glCheckFramebufferStatus(target);
    }

    @Override
    public void glDeleteFramebuffers(int renderbuffer) {
        ARBFramebufferObject.glDeleteFramebuffers(renderbuffer);
    }

    @Override
    public void glDeleteRenderbuffers(int renderbuffer) {
        ARBFramebufferObject.glDeleteRenderbuffers(renderbuffer);
    }
}
