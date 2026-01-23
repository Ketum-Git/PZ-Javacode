// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;

public class FBORenderChunkCamera implements IModelCamera {
    FBORenderChunk renderChunk;
    float originX;
    float originY;
    float originZ;
    float x;
    float y;
    float z;
    float useAngle;
    int width;
    int height;
    int bottom;

    public void set(FBORenderChunk renderChunk, float x, float y, float z, float angle) {
        this.renderChunk = renderChunk;
        this.originX = renderChunk.chunk.wx * 8 + 4.0F;
        this.originY = renderChunk.chunk.wy * 8 + 4.0F;
        this.originZ = renderChunk.getMinLevel();
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        int numLevels = renderChunk.getTopLevel() - renderChunk.getMinLevel() + 1;
        this.bottom = chunkFloorYSpan + numLevels * FBORenderChunk.PIXELS_PER_LEVEL;
        this.bottom = this.bottom + FBORenderLevels.extraHeightForJumboTrees(renderChunk.getMinLevel(), renderChunk.getTopLevel());
        this.x = x;
        this.y = y;
        this.z = z;
        this.useAngle = angle;
        this.width = renderChunk.w;
        this.height = renderChunk.h;
    }

    public void pushProjectionMatrix() {
        double screenWidth = this.width / 1920.0F;
        Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        float bottom = this.bottom - chunkFloorYSpan / 2.0F;
        if (this.renderChunk.highRes) {
            bottom *= 2.0F;
        }

        float top = this.height - bottom;
        PROJECTION.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, bottom / 1920.0F, -top / 1920.0F, -10.0F, 10.0F);
        Core.getInstance().projectionMatrixStack.push(PROJECTION);
    }

    public void pushModelViewMatrix(float ox, float oy, float oz, float useangle, boolean vehicle) {
        float cx = this.originX;
        float cy = this.originY;
        float cz = this.originZ;
        double x = cx;
        double y = cy;
        double z = cz;
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.scaling(Core.scale);
        MODELVIEW.scale(Core.tileScale / 2.0F);
        if (this.renderChunk.highRes) {
            MODELVIEW.scale(2.0F);
        }

        MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = ox - x;
        double difY = oy - y;
        MODELVIEW.translate(-((float)difX), (float)(oz - z) * 2.44949F, -((float)difY));
        if (vehicle) {
            MODELVIEW.scale(-1.0F, 1.0F, 1.0F);
        } else {
            MODELVIEW.scale(-1.5F, 1.5F, 1.5F);
        }

        MODELVIEW.rotate(useangle + (float) Math.PI, 0.0F, 1.0F, 0.0F);
        if (!vehicle) {
        }

        Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
    }

    public void DoPushIsoStuff(float ox, float oy, float oz, float useangle, boolean vehicle) {
        this.pushProjectionMatrix();
        this.pushModelViewMatrix(ox, oy, oz, useangle, vehicle);
        GL11.glDepthRange(0.0, 1.0);
    }

    public void DoPopIsoStuff() {
        GL11.glDepthRange(0.0, 1.0);
        GL11.glEnable(3008);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(false);
        GLStateRenderThread.AlphaTest.restore();
        GLStateRenderThread.DepthFunc.restore();
        GLStateRenderThread.DepthMask.restore();
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
    }

    @Override
    public void Begin() {
        this.DoPushIsoStuff(this.x, this.y, this.z, this.useAngle, false);
    }

    @Override
    public void End() {
        this.DoPopIsoStuff();
    }
}
