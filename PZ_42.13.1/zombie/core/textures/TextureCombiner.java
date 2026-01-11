// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.OpenGLException;
import zombie.SystemDisabler;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.utils.ImageUtils;

public final class TextureCombiner {
    public static final TextureCombiner instance = new TextureCombiner();
    public static int count;
    private TextureFBO fbo;
    private final float coordinateSpaceMax = 256.0F;
    private final ArrayList<TextureCombiner.CombinerFBO> fboPool = new ArrayList<>();

    public void init() throws Exception {
    }

    public void combineStart() {
        this.clear();
        count = 33984;
        GL11.glEnable(3042);
        GL11.glEnable(3553);
        GL11.glTexEnvi(8960, 8704, 7681);
    }

    public void combineEnd() {
        GL13.glActiveTexture(33984);
    }

    public void clear() {
        for (int x = 33985; x <= count; x++) {
            GL13.glActiveTexture(x);
            GL11.glDisable(3553);
        }

        GL13.glActiveTexture(33984);
    }

    public void overlay(Texture tex2) {
        GL13.glActiveTexture(count);
        GL11.glEnable(3553);
        GL11.glEnable(3042);
        tex2.bind();
        if (count > 33984) {
            GL11.glTexEnvi(8960, 8704, 34160);
            GL11.glTexEnvi(8960, 34161, 34165);
            GL11.glTexEnvi(8960, 34176, 34168);
            GL11.glTexEnvi(8960, 34177, 5890);
            GL11.glTexEnvi(8960, 34178, 34168);
            GL11.glTexEnvi(8960, 34192, 768);
            GL11.glTexEnvi(8960, 34193, 768);
            GL11.glTexEnvi(8960, 34194, 770);
            GL11.glTexEnvi(8960, 34162, 34165);
            GL11.glTexEnvi(8960, 34184, 34168);
            GL11.glTexEnvi(8960, 34185, 5890);
            GL11.glTexEnvi(8960, 34186, 34168);
            GL11.glTexEnvi(8960, 34200, 770);
            GL11.glTexEnvi(8960, 34201, 770);
            GL11.glTexEnvi(8960, 34202, 770);
        }

        count++;
    }

    public Texture combine(Texture tex1, Texture tex2) throws Exception {
        Core.getInstance().DoStartFrameStuff(tex1.width, tex2.width, 1.0F, 0);
        Texture tex3 = new Texture(tex1.width, tex2.height, 16);
        if (this.fbo == null) {
            this.fbo = new TextureFBO(tex3);
        } else {
            this.fbo.setTexture(tex3);
        }

        GL13.glActiveTexture(33984);
        GL11.glEnable(3553);
        GL11.glBindTexture(3553, tex1.getID());
        this.fbo.startDrawing(true, true);
        GL11.glBegin(7);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2d(0.0, tex1.height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2d(tex1.width, tex1.height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2d(tex1.width, 0.0);
        GL11.glEnd();
        GL11.glBindTexture(3553, tex2.getID());
        GL11.glBegin(7);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2d(0.0, tex1.height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2d(tex1.width, tex1.height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2d(tex1.width, 0.0);
        GL11.glEnd();
        this.fbo.endDrawing();
        Core.getInstance().DoEndFrameStuff(tex1.width, tex2.width);
        return tex3;
    }

    public static int[] flipPixels(int[] imgPixels, int imgw, int imgh) {
        int[] flippedPixels = null;
        if (imgPixels != null) {
            flippedPixels = new int[imgw * imgh];

            for (int y = 0; y < imgh; y++) {
                for (int x = 0; x < imgw; x++) {
                    flippedPixels[(imgh - y - 1) * imgw + x] = imgPixels[y * imgw + x];
                }
            }
        }

        return flippedPixels;
    }

    private TextureCombiner.CombinerFBO getFBO(int width, int height) {
        for (int i = 0; i < this.fboPool.size(); i++) {
            TextureCombiner.CombinerFBO combinerFBO = this.fboPool.get(i);
            if (combinerFBO.fbo.getWidth() == width && combinerFBO.fbo.getHeight() == height) {
                return combinerFBO;
            }
        }

        return null;
    }

    private Texture createTexture(int width, int height) {
        TextureCombiner.CombinerFBO combinerFBO = this.getFBO(width, height);
        Texture tex;
        if (combinerFBO == null) {
            combinerFBO = new TextureCombiner.CombinerFBO();
            tex = new Texture(width, height, 16);
            combinerFBO.fbo = new TextureFBO(tex);
            this.fboPool.add(combinerFBO);
        } else {
            tex = combinerFBO.textures.isEmpty() ? new Texture(width, height, 16) : combinerFBO.textures.pop();
            tex.bind();
            GL11.glTexImage2D(3553, 0, 6408, tex.getWidthHW(), tex.getHeightHW(), 0, 6408, 5121, (IntBuffer)null);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10241, 9729);
            tex.dataid.setMinFilter(9729);
            Texture.lastTextureID = 0;
            GL11.glBindTexture(3553, 0);
            combinerFBO.fbo.setTexture(tex);
        }

        this.fbo = combinerFBO.fbo;
        return tex;
    }

    public void releaseTexture(Texture tex) {
        TextureCombiner.CombinerFBO combinerFBO = this.getFBO(tex.getWidth(), tex.getHeight());
        if (combinerFBO != null && combinerFBO.textures.size() < 100) {
            combinerFBO.textures.push(tex);
        } else {
            tex.destroy();
        }
    }

    public Texture combine(ArrayList<TextureCombinerCommand> cmdList) throws Exception, OpenGLException {
        if (SystemDisabler.doEnableDetectOpenGLErrors) {
            PZGLUtil.checkGLErrorThrow("Enter");
        }

        int width = getResultingWidth(cmdList);
        int height = getResultingHeight(cmdList);
        Texture tex3 = this.createTexture(width, height);
        GL11.glPushAttrib(24576);
        GL11.glDisable(3089);
        GL11.glDisable(2960);
        GL11.glDisable(2884);
        this.fbo.startDrawing(true, true);
        if (SystemDisabler.doEnableDetectOpenGLErrors) {
            PZGLUtil.checkGLErrorThrow("FBO.startDrawing %s", this.fbo);
        }

        Core.getInstance().DoStartFrameStuffSmartTextureFx(width, height, -1);
        if (SystemDisabler.doEnableDetectOpenGLErrors) {
            PZGLUtil.checkGLErrorThrow("Core.DoStartFrameStuffFx w:%d, h:%d", width, height);
        }

        VBORenderer vbor = VBORenderer.getInstance();

        for (int x = 0; x < cmdList.size(); x++) {
            TextureCombinerCommand com = cmdList.get(x);
            if (com.shader != null) {
                com.shader.Start();
                VertexBufferObject.setModelViewProjection(com.shader.getShaderProgram());
                GL13.glActiveTexture(33984);
                GL11.glEnable(3553);
                Texture com_tex = com.tex == null ? Texture.getErrorTexture() : com.tex;
                com_tex.bind();
                if (com.mask != null) {
                    GL13.glActiveTexture(33985);
                    GL11.glEnable(3553);
                    int lastTextureID = Texture.lastTextureID;
                    if (com.mask.getTextureId() != null) {
                        com.mask.getTextureId().setMagFilter(9728);
                        com.mask.getTextureId().setMinFilter(9728);
                    }

                    com.mask.bind();
                    Texture.lastTextureID = lastTextureID;
                } else {
                    GL13.glActiveTexture(33985);
                    GL11.glDisable(3553);
                }

                if (com.shader != null) {
                    if (com.shaderParams != null) {
                        ArrayList<TextureCombinerShaderParam> shaderParams = com.shaderParams;

                        for (int i = 0; i < shaderParams.size(); i++) {
                            TextureCombinerShaderParam shaderParam = shaderParams.get(i);
                            float res = Rand.Next(shaderParam.min, shaderParam.max);
                            com.shader.setValue(shaderParam.name, res);
                        }
                    }

                    com.shader.setValue("DIFFUSE", com_tex, 0);
                    if (com.mask != null) {
                        com.shader.setValue("MASK", com.mask, 1);
                    }
                }

                GL14.glBlendFuncSeparate(com.blendSrc, com.blendDest, com.blendSrcA, com.blendDestA);
                if (com.x != -1) {
                    float xToFBO = width / 256.0F;
                    float yToFBO = height / 256.0F;
                    vbor.startRun(vbor.formatPositionColorUv);
                    vbor.setShaderProgram(com.shader.getShaderProgram());
                    vbor.setMode(7);
                    float z = 0.0F;
                    vbor.addQuad(
                        com.x * xToFBO,
                        com.y * yToFBO,
                        0.0F,
                        0.0F,
                        1.0F,
                        com.x * xToFBO,
                        (com.y + com.h) * yToFBO,
                        0.0F,
                        0.0F,
                        0.0F,
                        (com.x + com.w) * xToFBO,
                        (com.y + com.h) * yToFBO,
                        0.0F,
                        1.0F,
                        0.0F,
                        (com.x + com.w) * xToFBO,
                        com.y * yToFBO,
                        0.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                    );
                    vbor.endRun();
                    vbor.flush();
                } else {
                    vbor.startRun(vbor.formatPositionColorUv);
                    vbor.setShaderProgram(com.shader.getShaderProgram());
                    vbor.setMode(7);
                    float z = 0.0F;
                    vbor.addQuad(
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        0.0F,
                        height,
                        0.0F,
                        0.0F,
                        0.0F,
                        width,
                        height,
                        0.0F,
                        1.0F,
                        0.0F,
                        width,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                    );
                    vbor.endRun();
                    vbor.flush();
                }

                if (com.shader != null) {
                    com.shader.End();
                }

                if (SystemDisabler.doEnableDetectOpenGLErrors) {
                    PZGLUtil.checkGLErrorThrow("TextureCombinerCommand[%d}: %s", x, com);
                }
            } else {
                int dbg = 1;
            }
        }

        Core.getInstance().DoEndFrameStuffFx(width, height, -1);
        this.fbo.releaseTexture();
        this.fbo.endDrawing();
        if (SystemDisabler.doEnableDetectOpenGLErrors) {
            PZGLUtil.checkGLErrorThrow("FBO.endDrawing: %s", this.fbo);
        }

        GL11.glBlendFunc(770, 771);
        GL13.glActiveTexture(33985);
        GL11.glDisable(3553);
        if (Core.getInstance().getOptionModelTextureMipmaps()) {
        }

        GL13.glActiveTexture(33984);
        Texture.lastTextureID = 0;
        GL11.glBindTexture(3553, 0);
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        GL11.glPopAttrib();
        GLStateRenderThread.restore();
        if (SystemDisabler.doEnableDetectOpenGLErrors) {
            PZGLUtil.checkGLErrorThrow("Exit.");
        }

        return tex3;
    }

    public static int getResultingHeight(ArrayList<TextureCombinerCommand> cmdList) {
        if (cmdList.isEmpty()) {
            return 32;
        } else {
            TextureCombinerCommand dominantCmd = findDominantCommand(cmdList, Comparator.comparingInt(a -> a.tex.height));
            if (dominantCmd == null) {
                return 32;
            } else {
                Texture dominantTex = dominantCmd.tex;
                return ImageUtils.getNextPowerOfTwoHW(dominantTex.height);
            }
        }
    }

    public static int getResultingWidth(ArrayList<TextureCombinerCommand> cmdList) {
        if (cmdList.isEmpty()) {
            return 32;
        } else {
            TextureCombinerCommand dominantCmd = findDominantCommand(cmdList, Comparator.comparingInt(a -> a.tex.width));
            if (dominantCmd == null) {
                return 32;
            } else {
                Texture dominantTex = dominantCmd.tex;
                return ImageUtils.getNextPowerOfTwoHW(dominantTex.width);
            }
        }
    }

    private static TextureCombinerCommand findDominantCommand(ArrayList<TextureCombinerCommand> cmdList, Comparator<TextureCombinerCommand> comparator) {
        TextureCombinerCommand dominantCmd = null;
        int cmdCount = cmdList.size();

        for (int i = 0; i < cmdCount; i++) {
            TextureCombinerCommand cmd = cmdList.get(i);
            if (cmd.tex != null && (dominantCmd == null || comparator.compare(cmd, dominantCmd) > 0)) {
                dominantCmd = cmd;
            }
        }

        return dominantCmd;
    }

    private void createMipMaps(Texture texture) {
        if (GL.getCapabilities().OpenGL30) {
            GL13.glActiveTexture(33984);
            texture.bind();
            GL30.glGenerateMipmap(3553);
            int minFilter = 9987;
            GL11.glTexParameteri(3553, 10241, 9987);
            texture.dataid.setMinFilter(9987);
        }
    }

    private static final class CombinerFBO {
        TextureFBO fbo;
        final ArrayDeque<Texture> textures = new ArrayDeque<>();
    }
}
