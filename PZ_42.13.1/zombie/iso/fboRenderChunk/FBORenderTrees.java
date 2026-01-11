// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.advancedanimation.Anim2DBlendTriangle;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.popman.ObjectPool;

public final class FBORenderTrees extends TextureDraw.GenericDrawer {
    static final ObjectPool<FBORenderTrees> s_pool = new ObjectPool<>(FBORenderTrees::new);
    static final ObjectPool<FBORenderTrees.Tree> s_treePool = new ObjectPool<>(FBORenderTrees.Tree::new);
    public static FBORenderTrees current;
    static final FBORenderChunkCamera s_chunkCamera = new FBORenderChunkCamera();
    static Shader shader;
    static final float SQRT2 = (float)Math.sqrt(2.0);
    static final FBORenderTrees.BarycentricCompute s_barycentricCompute = new FBORenderTrees.BarycentricCompute();
    static final FBORenderTrees.Barycentric[] s_barycentric = new FBORenderTrees.Barycentric[4];
    final ArrayList<FBORenderTrees.Tree> trees = new ArrayList<>();
    float playerX;
    float playerY;
    float playerZ;
    boolean playerInside;

    public static FBORenderTrees alloc() {
        return s_pool.alloc();
    }

    @Override
    public void render() {
        boolean bRenderToChunkTexture = FBORenderChunkManager.instance.renderThreadCurrent != null;
        if (bRenderToChunkTexture) {
            s_chunkCamera.set(FBORenderChunkManager.instance.renderThreadCurrent, 0.0F, 0.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0));
            s_chunkCamera.pushProjectionMatrix();
        } else {
            this.pushProjectionMatrix();
        }

        for (int i = 0; i < this.trees.size(); i++) {
            FBORenderTrees.Tree tree = this.trees.get(i);
            this.renderTree(tree);
        }

        Core.getInstance().projectionMatrixStack.pop();
        GLStateRenderThread.restore();
    }

    private void pushProjectionMatrix() {
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        double screenWidth = camera.offscreenWidth / 1920.0F;
        double screenHeight = camera.offscreenHeight / 1920.0F;
        Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
        PROJECTION.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -10.0F, 10.0F);
        Core.getInstance().projectionMatrixStack.push(PROJECTION);
    }

    private void pushModelViewMatrix(float ox, float oy, float oz, float useangle) {
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        float rcx = camera.rightClickX;
        float rcy = camera.rightClickY;
        float tox = camera.getTOffX();
        float toy = camera.getTOffY();
        float defx = camera.deferedX;
        float defy = camera.deferedY;
        float cx = this.playerX - camera.XToIso(-tox - rcx, -toy - rcy, 0.0F);
        float cy = this.playerY - camera.YToIso(-tox - rcx, -toy - rcy, 0.0F);
        cx += defx;
        cy += defy;
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.scaling(Core.scale);
        MODELVIEW.scale(Core.tileScale / 2.0F);
        MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = ox - cx;
        double difY = oy - cy;
        double difZ = (oz - this.playerZ) * 2.44949F;
        MODELVIEW.translate(-((float)difX), (float)difZ, -((float)difY));
        MODELVIEW.scale(-1.0F, 1.0F, 1.0F);
        MODELVIEW.rotate(useangle + (float) Math.PI, 0.0F, 1.0F, 0.0F);
        MODELVIEW.translate(0.0F, -0.71999997F, 0.0F);
        Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
    }

    private void renderTree(FBORenderTrees.Tree tree) {
        if (Core.debug) {
        }

        boolean bLines = false;
        boolean bRenderToChunkTexture = FBORenderChunkManager.instance.renderThreadCurrent != null;
        VBORenderer vbor = VBORenderer.getInstance();
        if (bRenderToChunkTexture) {
            s_chunkCamera.set(FBORenderChunkManager.instance.renderThreadCurrent, tree.x, tree.y, tree.z, (float) (Math.PI * 3.0 / 4.0));
            s_chunkCamera.pushModelViewMatrix(tree.x, tree.y, tree.z, (float) (Math.PI * 3.0 / 4.0), false);
            Core.getInstance().modelViewMatrixStack.peek().scale(0.6666667F);
        } else {
            this.pushModelViewMatrix(tree.x, tree.y, tree.z, (float) (Math.PI * 3.0 / 4.0));
        }

        GL11.glDepthMask(true);
        GL11.glDepthFunc(515);
        if (bLines) {
            GL11.glPolygonMode(1032, 6913);
        }

        if (tree.useStencil) {
            GL11.glEnable(2960);
            GL11.glStencilFunc(517, 128, 128);
            GL11.glBlendFunc(770, 771);
        } else {
            GL11.glDisable(2960);
        }

        this.renderTreeTextures(tree, false);
        vbor.flush();
        if (tree.useStencil) {
            GL11.glStencilFunc(514, 128, 128);
            float a = tree.a;
            tree.a = tree.fadeAlpha;
            this.renderTreeTextures(tree, false);
            vbor.flush();
            tree.a = a;
            this.renderTreeTextures(tree, true);
            vbor.flush();
            GL11.glStencilFunc(519, 255, 255);
        }

        if (bLines) {
            GL11.glPolygonMode(1032, 6914);
        }

        Core.getInstance().modelViewMatrixStack.pop();
    }

    private void renderTreeTextures(FBORenderTrees.Tree tree, boolean bUseShader) {
        int floorHeight = 32 * Core.tileScale;
        float dyFloor = floorHeight / 2.0F;
        if (tree.texture != null) {
            float y1 = tree.texture.getOffsetY();
            float y2 = tree.texture.getHeightOrig() - dyFloor;
            float y3 = tree.texture.getOffsetY() + tree.texture.getHeight();
            this.renderTexture(tree, tree.texture, y1, y2, bUseShader);
            this.renderTexture(tree, tree.texture, y2, y3, bUseShader);
        }

        if (tree.texture2 != null) {
            float y1 = tree.texture2.getOffsetY();
            float y2 = tree.texture2.getOffsetY() + tree.texture2.getHeight();
            this.renderTexture(tree, tree.texture2, y1, y2, bUseShader);
        }
    }

    private float calculateDepth(FBORenderTrees.Tree tree, boolean bTexture2, boolean bUseShader, boolean bFloorHack) {
        float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
            PZMath.fastfloor(this.playerX), PZMath.fastfloor(this.playerY), tree.x - 0.5F, tree.y - 0.5F, tree.z
        );
        float targetDepth = results.depthStart - (depthBufferValue + 1.0F) / 2.0F;
        float chunkDepth = 0.0F;
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.renderThreadCurrent;
        if (renderChunk != null) {
            chunkDepth = IsoDepthHelper.getChunkDepthData(
                    PZMath.fastfloor(this.playerX / 8.0F),
                    PZMath.fastfloor(this.playerY / 8.0F),
                    renderChunk.chunk.wx,
                    renderChunk.chunk.wy,
                    PZMath.fastfloor(tree.z)
                )
                .depthStart;
            targetDepth -= chunkDepth;
        }

        if (bTexture2) {
            targetDepth -= 1.0E-4F;
        }

        if (bUseShader) {
            targetDepth -= 2.0E-4F;
        }

        if (PZMath.fastfloor(tree.x) % 2 == 0) {
            targetDepth += 2.0E-4F;
        }

        if (bFloorHack) {
            targetDepth -= 0.0015F;
        }

        return targetDepth;
    }

    private void renderTexture(FBORenderTrees.Tree tree, Texture tex, float top, float bottom, boolean bUseShader) {
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.startRun(VBORenderer.getInstance().formatPositionColorUv);
        vbor.setMode(7);
        vbor.setTextureID(tex.getTextureId());
        vbor.setMinMagFilters(9728, 9728);
        vbor.setDepthTest(true);
        if (shader == null) {
            shader = ShaderManager.instance.getOrCreateShader("vboRenderer_Tree", true, false);
        }

        if (bUseShader && shader.getShaderProgram().isCompiled()) {
            shader.Start();
            shader.getShaderProgram().setSamplerUnit("DIFFUSE", 0);
            shader.End();
            vbor.setShaderProgram(shader.getShaderProgram());
            vbor.cmdShader4f("outlineColor", 0.1F, 0.1F, 0.1F, this.playerInside && tree.fadeAlpha < 0.5F ? tree.fadeAlpha : 1.0F - tree.fadeAlpha);
            vbor.cmdShader2f("stepSize", 0.25F / tex.getWidth(), 0.25F / tex.getHeight());
        }

        float targetDepth = this.calculateDepth(tree, tex == tree.texture2, bUseShader, top > tex.getOffsetY());
        vbor.setUserDepth(targetDepth);
        boolean bJumbo = tex.getWidthOrig() == Core.tileScale * 64 * 3;
        int unitsX = 2;
        int unitsY = 4;
        if (bJumbo) {
            unitsX = 6;
            unitsY = 8;
        }

        float x1 = tex.getOffsetX();
        float x2 = tex.getOffsetX() + tex.getWidth();
        float x3 = tex.getOffsetX() + tex.getWidth();
        float x4 = tex.getOffsetX();
        float y1 = top;
        float y2 = top;
        float y3 = bottom;
        float y4 = bottom;
        if (tree.objectRenderEffects) {
            this.calculateBarycentricCoordinatesForVertex(tex, x1, top, 0);
            this.calculateBarycentricCoordinatesForVertex(tex, x2, top, 1);
            this.calculateBarycentricCoordinatesForVertex(tex, x3, bottom, 2);
            this.calculateBarycentricCoordinatesForVertex(tex, x4, bottom, 3);
            double ax = 0.0;
            double ay = 0.0;
            double bx = tex.getWidthOrig();
            double by = 0.0;
            double cx = tex.getWidthOrig();
            double cy = tex.getHeightOrig();
            double dx = 0.0;
            double dy = tex.getHeightOrig();
            int texW = 128;
            int texH = 256;
            ax += tree.oreX1 * 128.0F;
            ay += tree.oreY1 * 256.0F;
            bx += tree.oreX2 * 128.0F;
            by += tree.oreY2 * 256.0F;
            cx += tree.oreX3 * 128.0F;
            cy += tree.oreY3 * 256.0F;
            dx += tree.oreX4 * 128.0F;
            dy += tree.oreY4 * 256.0F;
            double[] xy = s_barycentric[0].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x1 = (float)xy[0];
            y1 = (float)xy[1];
            xy = s_barycentric[1].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x2 = (float)xy[0];
            y2 = (float)xy[1];
            xy = s_barycentric[2].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x3 = (float)xy[0];
            y3 = (float)xy[1];
            xy = s_barycentric[3].compute(ax, ay, bx, by, cx, cy, dx, dy);
            x4 = (float)xy[0];
            y4 = (float)xy[1];
        }

        float dx = unitsX / SQRT2 / 2.0F;
        float xTL = this.pixelToGLX(tex, x1, unitsX) - dx;
        float xTR = this.pixelToGLX(tex, x2, unitsX) - dx;
        float xBR = this.pixelToGLX(tex, x3, unitsX) - dx;
        float xBL = this.pixelToGLX(tex, x4, unitsX) - dx;
        float yTL = unitsY - this.pixelToGLY(tex, y1, unitsY);
        float yTR = unitsY - this.pixelToGLY(tex, y2, unitsY);
        float yBR = this.pixelToGLY(tex, tex.getHeightOrig() - y3, unitsY);
        float yBL = this.pixelToGLY(tex, tex.getHeightOrig() - y4, unitsY);
        float yStart = tex.getYStart() + (top - tex.getOffsetY()) / tex.getHeight() * (tex.getYEnd() - tex.getYStart());
        float yEnd = tex.getYStart() + (bottom - tex.getOffsetY()) / tex.getHeight() * (tex.getYEnd() - tex.getYStart());
        float z = 0.0F;
        vbor.addQuad(
            xTL,
            yTL * 0.8164967F,
            0.0F,
            tex.getXStart(),
            yStart,
            xTR,
            yTR * 0.8164967F,
            0.0F,
            tex.getXEnd(),
            yStart,
            xBR,
            yBR * 0.8164967F,
            0.0F,
            tex.getXEnd(),
            yEnd,
            xBL,
            yBL * 0.8164967F,
            0.0F,
            tex.getXStart(),
            yEnd,
            tree.r,
            tree.g,
            tree.b,
            tree.a
        );
        vbor.endRun();
    }

    private void calculateBarycentricCoordinatesForVertex(Texture tex, float x, float y, int index) {
        FBORenderTrees.Barycentric barycentric = s_barycentric[index];
        float x1 = 0.0F;
        float y1 = 0.0F;
        float x2 = tex.getWidthOrig();
        float y2 = 0.0F;
        float x3 = tex.getWidthOrig();
        float y3 = tex.getHeightOrig();
        float x4 = 0.0F;
        float y4 = tex.getHeightOrig();
        if (Anim2DBlendTriangle.PointInTriangle(x, y, 0.0F, 0.0F, x2, 0.0F, x3, y3)) {
            barycentric.triangle = 0;
            double[] bcc = s_barycentricCompute.compute(x, y, 0.0, 0.0, x2, 0.0, x3, y3);
            barycentric.u = bcc[0];
            barycentric.v = bcc[1];
            barycentric.w = bcc[2];
        } else {
            barycentric.triangle = 1;
            double[] bcc = s_barycentricCompute.compute(x, y, 0.0, 0.0, x3, y3, 0.0, y4);
            barycentric.u = bcc[0];
            barycentric.v = bcc[1];
            barycentric.w = bcc[2];
        }
    }

    private float pixelToGLX(Texture tex, float pixels, int unitsX) {
        float u = pixels / tex.getWidthOrig();
        return u * (unitsX / SQRT2);
    }

    private float pixelToGLY(Texture tex, float pixels, int unitsY) {
        float v = pixels / tex.getHeightOrig();
        return v * unitsY;
    }

    @Override
    public void postRender() {
        s_treePool.releaseAll(this.trees);
        this.trees.clear();
        s_pool.release(this);
    }

    public void init() {
        s_treePool.releaseAll(this.trees);
        this.trees.clear();
        this.playerX = IsoCamera.frameState.camCharacterX;
        this.playerY = IsoCamera.frameState.camCharacterY;
        this.playerZ = IsoCamera.frameState.camCharacterZ;
        this.playerInside = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
    }

    public void addTree(
        Texture texture,
        Texture texture2,
        float x,
        float y,
        float z,
        float r,
        float g,
        float b,
        float a,
        ObjectRenderEffects ore,
        boolean bUseStencil,
        float fadeAlpha
    ) {
        FBORenderTrees.Tree tree = s_treePool.alloc();
        tree.texture = texture;
        tree.texture2 = texture2;
        tree.x = x + 1.0F;
        tree.y = y + 1.0F;
        tree.z = z;
        tree.r = r;
        tree.g = g;
        tree.b = b;
        tree.a = a;
        tree.objectRenderEffects = ore != null
            && (ore.x1 != 0.0 || ore.y1 != 0.0 || ore.x2 != 0.0 || ore.y2 != 0.0 || ore.x3 != 0.0 || ore.y3 != 0.0 || ore.x4 != 0.0 || ore.y4 != 0.0);
        if (tree.objectRenderEffects) {
            tree.oreX1 = (float)ore.x1;
            tree.oreY1 = (float)ore.y1;
            tree.oreX2 = (float)ore.x2;
            tree.oreY2 = (float)ore.y2;
            tree.oreX3 = (float)ore.x3;
            tree.oreY3 = (float)ore.y3;
            tree.oreX4 = (float)ore.x4;
            tree.oreY4 = (float)ore.y4;
        }

        tree.useStencil = bUseStencil;
        tree.fadeAlpha = fadeAlpha;
        this.trees.add(tree);
    }

    static {
        for (int i = 0; i < s_barycentric.length; i++) {
            s_barycentric[i] = new FBORenderTrees.Barycentric();
        }
    }

    static final class Barycentric {
        int triangle;
        double u;
        double v;
        double w;
        double[] pt = new double[2];

        double[] compute(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
            if (this.triangle == 0) {
                this.pt[0] = this.u * ax + this.v * bx + this.w * cx;
                this.pt[1] = this.u * ay + this.v * by + this.w * cy;
            } else {
                this.pt[0] = this.u * ax + this.v * cx + this.w * dx;
                this.pt[1] = this.u * ay + this.v * cy + this.w * dy;
            }

            return this.pt;
        }
    }

    static final class BarycentricCompute {
        double[] v0 = new double[2];
        double[] v1 = new double[2];
        double[] v2 = new double[2];
        double[] barycentric = new double[3];

        double[] compute(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
            this.v0[0] = bx - ax;
            this.v0[1] = by - ay;
            this.v1[0] = cx - ax;
            this.v1[1] = cy - ay;
            this.v2[0] = px - ax;
            this.v2[1] = py - ay;
            double dot00 = this.v0[0] * this.v0[0] + this.v0[1] * this.v0[1];
            double dot01 = this.v0[0] * this.v1[0] + this.v0[1] * this.v1[1];
            double dot02 = this.v0[0] * this.v2[0] + this.v0[1] * this.v2[1];
            double dot11 = this.v1[0] * this.v1[0] + this.v1[1] * this.v1[1];
            double dot12 = this.v1[0] * this.v2[0] + this.v1[1] * this.v2[1];
            double invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01);
            this.barycentric[1] = (dot11 * dot02 - dot01 * dot12) * invDenom;
            this.barycentric[2] = (dot00 * dot12 - dot01 * dot02) * invDenom;
            this.barycentric[0] = 1.0 - this.barycentric[1] - this.barycentric[2];
            return this.barycentric;
        }
    }

    static final class Tree {
        Texture texture;
        Texture texture2;
        float x;
        float y;
        float z;
        float r;
        float g;
        float b;
        float a;
        boolean objectRenderEffects;
        boolean useStencil;
        float fadeAlpha;
        float oreX1;
        float oreY1;
        float oreX2;
        float oreY2;
        float oreX3;
        float oreY3;
        float oreX4;
        float oreY4;
    }
}
