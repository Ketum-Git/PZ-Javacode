// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import org.lwjgl.opengl.GL30;

public final class GLFramebufferObject30 implements IGLFramebufferObject {
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
        return GL30.glGenFramebuffers();
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GL30.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenRenderbuffers() {
        return GL30.glGenRenderbuffers();
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GL30.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GL30.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return GL30.glCheckFramebufferStatus(target);
    }

    @Override
    public void glDeleteFramebuffers(int renderbuffer) {
        GL30.glDeleteFramebuffers(renderbuffer);
    }

    @Override
    public void glDeleteRenderbuffers(int renderbuffer) {
        GL30.glDeleteRenderbuffers(renderbuffer);
    }
}
