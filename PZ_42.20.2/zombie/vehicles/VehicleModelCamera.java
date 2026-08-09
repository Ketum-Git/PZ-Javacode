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
            Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
            projection.setOrtho(-192.0F, 192.0F, -192.0F, 192.0F, -1000.0F, 1000.0F);
            float f = Math.sqrt(2048.0F);
            projection.scale(-f, f, f);
            Core.getInstance().projectionMatrixStack.push(projection);
            Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.identity();
            modelView.rotate((float) (java.lang.Math.PI / 6), 1.0F, 0.0F, 0.0F);
            modelView.rotate((float) (java.lang.Math.PI / 4), 0.0F, 1.0F, 0.0F);
            Core.getInstance().modelViewMatrixStack.push(modelView);
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
