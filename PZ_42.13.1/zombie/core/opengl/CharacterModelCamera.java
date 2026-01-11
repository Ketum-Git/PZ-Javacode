// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelCamera;

public final class CharacterModelCamera extends ModelCamera {
    public static final CharacterModelCamera instance = new CharacterModelCamera();

    @Override
    public void Begin() {
        if (this.useWorldIso) {
            Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, this.useAngle, this.inVehicle);
            GL11.glDepthMask(this.depthMask);
        } else {
            int w = 1024;
            int h = 1024;
            float sizeV = 42.75F;
            float offsetX = 0.0F;
            float offsetY = -0.45F;
            float offsetZ = 0.0F;
            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            float xScale = 1.0F;
            boolean flipY = false;
            PROJECTION.setOrtho(-42.75F, 42.75F, -42.75F, 42.75F, -100.0F, 100.0F);
            float f = Math.sqrt(2048.0F);
            PROJECTION.scale(-f, f, f);
            Core.getInstance().projectionMatrixStack.push(PROJECTION);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.translation(0.0F, -0.45F, 0.0F);
            MODELVIEW.rotate((float) (java.lang.Math.PI / 6), 1.0F, 0.0F, 0.0F);
            MODELVIEW.rotate(this.useAngle + (float) (java.lang.Math.PI / 4), 0.0F, 1.0F, 0.0F);
            Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
            GL11.glDepthRange(0.0, 1.0);
            GL11.glDepthMask(this.depthMask);
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
