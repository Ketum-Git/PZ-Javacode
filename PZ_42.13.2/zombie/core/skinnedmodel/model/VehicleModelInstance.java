// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.Arrays;
import org.joml.Vector3f;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.textures.Texture;

public final class VehicleModelInstance extends ModelInstance {
    public Texture textureRust;
    public Texture textureMask;
    public Texture textureLights;
    public Texture textureDamage1Overlay;
    public Texture textureDamage1Shell;
    public Texture textureDamage2Overlay;
    public Texture textureDamage2Shell;
    public final float[] textureUninstall1 = new float[16];
    public final float[] textureUninstall2 = new float[16];
    public final float[] textureLightsEnables1 = new float[16];
    public final float[] textureLightsEnables2 = new float[16];
    public final float[] textureDamage1Enables1 = new float[16];
    public final float[] textureDamage1Enables2 = new float[16];
    public final float[] textureDamage2Enables1 = new float[16];
    public final float[] textureDamage2Enables2 = new float[16];
    public final float[] matrixBlood1Enables1 = new float[16];
    public final float[] matrixBlood1Enables2 = new float[16];
    public final float[] matrixBlood2Enables1 = new float[16];
    public final float[] matrixBlood2Enables2 = new float[16];
    public float textureRustA;
    public float refWindows = 0.5F;
    public float refBody = 0.4F;
    public final Vector3f painColor = new Vector3f(0.0F, 0.5F, 0.5F);
    private final ModelInstance.EffectLight[] lights = new ModelInstance.EffectLight[5];
    public final Object lightsLock = "Model Lights Lock";

    public VehicleModelInstance() {
        for (int i = 0; i < this.lights.length; i++) {
            this.lights[i] = new ModelInstance.EffectLight();
        }
    }

    @Override
    public void reset() {
        super.reset();
        Arrays.fill(this.textureUninstall1, 0.0F);
        Arrays.fill(this.textureUninstall2, 0.0F);
        Arrays.fill(this.textureLightsEnables1, 0.0F);
        Arrays.fill(this.textureLightsEnables2, 0.0F);
        Arrays.fill(this.textureDamage1Enables1, 0.0F);
        Arrays.fill(this.textureDamage1Enables2, 0.0F);
        Arrays.fill(this.textureDamage2Enables1, 0.0F);
        Arrays.fill(this.textureDamage2Enables2, 0.0F);
        Arrays.fill(this.matrixBlood1Enables1, 0.0F);
        Arrays.fill(this.matrixBlood1Enables2, 0.0F);
        Arrays.fill(this.matrixBlood2Enables1, 0.0F);
        Arrays.fill(this.matrixBlood2Enables2, 0.0F);
        this.textureRustA = 0.0F;
        this.refWindows = 0.5F;
        this.refBody = 0.4F;
        this.painColor.set(0.0F, 0.5F, 0.5F);

        for (int i = 0; i < this.lights.length; i++) {
            this.lights[i].clear();
        }
    }

    public ModelInstance.EffectLight[] getLights() {
        return this.lights;
    }

    public void UpdateLights(int playerIndex) {
        synchronized (this.lightsLock) {
            ModelManager.instance.getSquareLighting(playerIndex, this.object, this.lights);
        }
    }
}
