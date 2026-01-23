// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelCamera;

public final class VehicleModelCamera extends ModelCamera {
    public static final VehicleModelCamera instance = new VehicleModelCamera();

    @Override
    public void Begin() {
        if (this.useWorldIso) {
            Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, this.useAngle, true);
            GL11.glDepthMask(this.depthMask);
        } else {
            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            PROJECTION.setOrtho(-192.0F, 192.0F, -192.0F, 192.0F, -1000.0F, 1000.0F);
            float f = Math.sqrt(2048.0F);
            PROJECTION.scale(-f, f, f);
            Core.getInstance().projectionMatrixStack.push(PROJECTION);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.identity();
            MODELVIEW.rotate((float) (java.lang.Math.PI / 6), 1.0F, 0.0F, 0.0F);
            MODELVIEW.rotate((float) (java.lang.Math.PI / 4), 0.0F, 1.0F, 0.0F);
            Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
        }
    }

    @Override
    public void End() {
        if (this.useWorldIso) {
            Core.getInstance().DoPopIsoStuff();
        } else {
            GL11.glDepthFunc(519);
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
        }
    }
}
