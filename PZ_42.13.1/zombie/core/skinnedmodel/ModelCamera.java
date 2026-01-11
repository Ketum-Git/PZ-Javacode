// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.opengl.IModelCamera;
import zombie.core.rendering.RenderTarget;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public abstract class ModelCamera implements IModelCamera {
    public static ModelCamera instance;
    public float useAngle;
    public boolean useWorldIso;
    public float x;
    public float y;
    public float z;
    public boolean inVehicle;
    public boolean depthMask = true;
    private static final Vector3f ImposterScale = new Vector3f();

    public void BeginImposter(RenderTarget imposter) {
        int w = imposter.GetWidth();
        int h = imposter.GetHeight();
        float sizeV = 42.75F;
        Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
        float xScale = (float)w / h;
        float hw = 0.5F;
        float hh = 0.5F;
        PROJECTION.setOrtho(-0.5F, 0.5F, -0.5F, 0.5F, -10.0F, 10.0F);
        Core.getInstance().projectionMatrixStack.push(PROJECTION);
        float scale = Core.scale * Core.tileScale / 2.0F * 1.5F;
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.scaling(Core.scale * Core.tileScale / 2.0F);
        MODELVIEW.rotateX((float) (Math.PI / 6));
        MODELVIEW.rotateY((float) (Math.PI * 3.0 / 4.0));
        MODELVIEW.scale(-1.5F, 1.5F, 1.5F);
        MODELVIEW.rotateY(this.useAngle + (float) Math.PI);
        MODELVIEW.translate(0.0F, -0.58F, 0.0F);
        MODELVIEW.getScale(ImposterScale);
        MODELVIEW.scaleLocal(1.0F / ImposterScale.x, 1.0F / ImposterScale.y, 1.0F);
        Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
        GL11.glDepthRange(-10.0, 10.0);
        GL11.glDepthMask(this.depthMask);
    }

    public void EndImposter() {
        GL11.glDepthFunc(519);
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
    }
}
