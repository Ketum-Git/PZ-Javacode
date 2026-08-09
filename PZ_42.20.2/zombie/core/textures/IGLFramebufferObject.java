// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

public interface IGLFramebufferObject {
    int GL_FRAMEBUFFER();

    int GL_RENDERBUFFER();

    int GL_COLOR_ATTACHMENT0();

    int GL_DEPTH_ATTACHMENT();

    int GL_STENCIL_ATTACHMENT();

    int GL_DEPTH_STENCIL();

    int GL_DEPTH24_STENCIL8();

    int GL_FRAMEBUFFER_COMPLETE();

    int GL_FRAMEBUFFER_UNDEFINED();

    int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT();

    int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT();

    int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS();

    int GL_FRAMEBUFFER_INCOMPLETE_FORMATS();

    int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER();

    int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER();

    int GL_FRAMEBUFFER_UNSUPPORTED();

    int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE();

    int glGenFramebuffers();

    void glBindFramebuffer(int target, int framebuffer);

    void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

    int glGenRenderbuffers();

    void glBindRenderbuffer(int target, int renderbuffer);

    void glRenderbufferStorage(int target, int internalformat, int width, int height);

    void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);

    int glCheckFramebufferStatus(int target);

    void glDeleteFramebuffers(int renderbuffer);

    void glDeleteRenderbuffers(int renderbuffer);
}
