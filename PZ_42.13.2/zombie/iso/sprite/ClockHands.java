// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.popman.ObjectPool;

public final class ClockHands extends TextureDraw.GenericDrawer {
    public static final ObjectPool<ClockHands> s_pool = new ObjectPool<>(ClockHands::new);
    float originX;
    float originY;
    float originZ;
    float rx;
    float ry;
    float rz;
    float lengthForward;
    float lengthBackward;
    float thickness;
    Texture texture;
    float r;
    float g;
    float b;
    float a;
    float depth;

    public ClockHands init(
        float x,
        float y,
        float z,
        float rx,
        float ry,
        float rz,
        Texture texture,
        float lengthForward,
        float lengthBackward,
        float thickness,
        float r,
        float g,
        float b,
        float a
    ) {
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.texture = texture;
        this.lengthForward = lengthForward;
        this.lengthBackward = lengthBackward;
        this.thickness = thickness;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        float playerX = IsoCamera.frameState.camCharacterX;
        float playerY = IsoCamera.frameState.camCharacterY;
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
            PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.originX, this.originY, this.originZ
        );
        this.depth = results.depthStart;
        return this;
    }

    @Override
    public void render() {
        Core.getInstance().DoPushIsoStuff(this.originX, this.originY, this.originZ, 0.0F, false);
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.setDepthTestForAllRuns(Boolean.TRUE);
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.identity();
        MODELVIEW.rotateXYZ(this.rx * (float) (Math.PI / 180.0), this.ry * (float) (Math.PI / 180.0), this.rz * (float) (Math.PI / 180.0));
        MODELVIEW.scale(0.6666667F);
        Core.getInstance().modelViewMatrixStack.peek().mul(MODELVIEW, MODELVIEW);
        Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
        float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
        float targetDepth = this.depth - (depthBufferValue + 1.0F) / 2.0F;
        vbor.setUserDepthForAllRuns(targetDepth);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDepthFunc(515);
        GL11.glDisable(2884);
        GL11.glDepthMask(true);
        float z = 0.0F;
        vbor.startRun(vbor.formatPositionColorUv);
        vbor.setMode(7);
        if (this.texture != null) {
            vbor.setTextureID(this.texture.getTextureId());
            vbor.addQuad(
                -this.thickness / 2.0F,
                this.lengthForward,
                this.texture.xStart,
                this.texture.yStart,
                this.thickness / 2.0F,
                -this.lengthBackward,
                this.texture.xEnd,
                this.texture.yEnd,
                0.0F,
                this.r,
                this.g,
                this.b,
                this.a
            );
        } else {
            vbor.addQuad(
                -this.thickness / 2.0F,
                this.lengthForward,
                0.0F,
                0.0F,
                this.thickness / 2.0F,
                -this.lengthBackward,
                1.0F,
                1.0F,
                0.0F,
                this.r,
                this.g,
                this.b,
                this.a
            );
        }

        vbor.endRun();
        vbor.flush();
        vbor.setDepthTestForAllRuns(null);
        vbor.setUserDepthForAllRuns(null);
        if (DebugOptions.instance.model.render.axis.getValue()) {
            Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 2.0F, 1.0F);
        }

        Core.getInstance().modelViewMatrixStack.pop();
        Core.getInstance().DoPopIsoStuff();
        GLStateRenderThread.restore();
    }

    @Override
    public void postRender() {
        s_pool.release(this);
    }
}
