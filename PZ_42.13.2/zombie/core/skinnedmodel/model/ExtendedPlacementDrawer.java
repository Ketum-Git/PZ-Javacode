// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;

public class ExtendedPlacementDrawer extends TextureDraw.GenericDrawer {
    IsoWorldInventoryObject worldItem;
    float x;
    float y;
    float z;
    float xoff;
    float yoff;
    float zoff;
    float minModelZ;
    float surfaceOffset1;
    float surfaceOffset2;
    float depth;
    static final ObjectPool<ExtendedPlacementDrawer> s_pool = new ObjectPool<>(ExtendedPlacementDrawer::new);

    public void init(IsoWorldInventoryObject worldItem, float minModelZ) {
        this.worldItem = worldItem;
        this.x = worldItem.getSquare().getX() + 0.5F;
        this.y = worldItem.getSquare().getY() + 0.5F;
        this.z = worldItem.getSquare().getZ();
        this.xoff = worldItem.xoff;
        this.yoff = worldItem.yoff;
        this.zoff = worldItem.zoff;
        this.minModelZ = minModelZ;
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
            PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), this.x, this.y, this.z
        );
        this.depth = results.depthStart;
        this.surfaceOffset1 = 0.0F;
        this.surfaceOffset2 = 0.0F;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
        if (square != null) {
            for (int i = square.getObjects().size() - 1; i >= 0; i--) {
                IsoObject object = square.getObjects().get(i);
                float surface = object.getSurfaceOffsetNoTable();
                if (surface > 0.0F) {
                    if (this.surfaceOffset2 == 0.0F) {
                        this.surfaceOffset2 = surface;
                    } else {
                        this.surfaceOffset1 = surface;
                    }
                }
            }
        }
    }

    @Override
    public void render() {
        Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, 0.0F, false);
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.setDepthTestForAllRuns(Boolean.TRUE);
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.identity();
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
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = 0.5F;
        float SurfaceOffset = this.surfaceOffset2;
        float deltaZ = this.zoff + this.minModelZ - SurfaceOffset / 96.0F;
        float radius = deltaZ / 2.0F;
        if (deltaZ < 0.0F) {
            radius = 0.5F;
        }

        float aboveSurface = 0.015F;
        MODELVIEW.translate(-this.xoff + 0.5F, SurfaceOffset / 96.0F * 2.44949F + 0.015F, this.yoff - 0.5F);
        MODELVIEW.rotateX((float) (Math.PI / 2));
        vbor.addDisk_Fill(radius - 0.02F, radius, 32, 1, Texture.getErrorTexture().getTextureId(), r, g, 1.0F, a);
        vbor.flush();
        vbor.setOffset(0.0F, 0.0F, 0.0F);
        vbor.setDepthTestForAllRuns(null);
        vbor.setUserDepthForAllRuns(null);
        Core.getInstance().modelViewMatrixStack.pop();
        Core.getInstance().DoPopIsoStuff();
        GLStateRenderThread.restore();
    }

    @Override
    public void postRender() {
        s_pool.release(this);
    }

    private void onRotateGizmo(float x, float y, float z) {
        this.worldItem.getItem().setWorldXRotation(x);
        this.worldItem.getItem().setWorldYRotation(z);
        this.worldItem.getItem().setWorldZRotation(y);
    }

    private void onTranslateGizmo(float x, float y, float z) {
        this.worldItem.setOffX(PZMath.clamp(x - this.worldItem.getSquare().getX(), 0.0F, 1.0F));
        this.worldItem.setOffY(PZMath.clamp(z - this.worldItem.getSquare().getY(), 0.0F, 1.0F));
        this.worldItem.setOffZ(PZMath.clamp(y - this.worldItem.getSquare().getZ(), 0.0F, 1.0F));
    }
}
