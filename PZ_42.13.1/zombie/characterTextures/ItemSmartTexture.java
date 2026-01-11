// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characterTextures;

import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.TextureCombinerCommand;
import zombie.core.textures.TextureCombinerShaderParam;
import zombie.util.StringUtils;

public final class ItemSmartTexture extends SmartTexture {
    public static final int DecalOverlayCategory = 300;
    public static final int FluidOverlayCategory = 302;
    private String texName;

    public ItemSmartTexture(String tex) {
        if (tex != null) {
            this.add(tex);
            this.texName = tex;
        }
    }

    public ItemSmartTexture(String tex, float hue) {
        this.addHue("media/textures/" + tex + ".png", 300, hue);
        this.texName = tex;
    }

    public void setDenimPatches(BloodBodyPartType bodyPart) {
        if (!StringUtils.isNullOrEmpty(CharacterSmartTexture.DenimPatchesMaskFiles[bodyPart.index()])) {
            String tex = "media/textures/patches/" + CharacterSmartTexture.DenimPatchesMaskFiles[bodyPart.index()] + ".png";
            int category = 300 + bodyPart.index();
            this.addOverlayPatches(tex, "media/textures/patches/patchesmask.png", category);
        }
    }

    public void setLeatherPatches(BloodBodyPartType bodyPart) {
        if (!StringUtils.isNullOrEmpty(CharacterSmartTexture.LeatherPatchesMaskFiles[bodyPart.index()])) {
            String tex = "media/textures/patches/" + CharacterSmartTexture.LeatherPatchesMaskFiles[bodyPart.index()] + ".png";
            int category = 300 + bodyPart.index();
            this.addOverlayPatches(tex, "media/textures/patches/patchesmask.png", category);
        }
    }

    public void setBasicPatches(BloodBodyPartType bodyPart) {
        if (!StringUtils.isNullOrEmpty(CharacterSmartTexture.BasicPatchesMaskFiles[bodyPart.index()])) {
            String tex = "media/textures/patches/" + CharacterSmartTexture.BasicPatchesMaskFiles[bodyPart.index()] + ".png";
            int category = 300 + bodyPart.index();
            this.addOverlayPatches(tex, "media/textures/patches/patchesmask.png", category);
        }
    }

    public void setFluid(String tex, String mask, float intensity, int category, Color tint) {
        intensity = PZMath.clamp_01(intensity);
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); i++) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (shaderParam.name.equals("intensity") && (shaderParam.min != intensity || shaderParam.max != intensity)) {
                    shaderParam.min = shaderParam.max = intensity;
                    this.setDirty();
                }
            }
        } else if (intensity > 0.0F) {
            float r = tint.getR();
            float g = tint.getG();
            float b = tint.getB();
            this.addTintedOverlay(tex, mask, intensity, category, r, g, b);
        }
    }

    public void setTintMask(String tex, String mask, int category, Color tint) {
        float r = tint.getR();
        float g = tint.getG();
        float b = tint.getB();
        this.addTintedOverlay(tex, mask, 1.0F, category, r, g, b);
    }

    public void setBlood(String tex, BloodBodyPartType bodyPart, float intensity) {
        String mask = "media/textures/BloodTextures/" + CharacterSmartTexture.MaskFiles[bodyPart.index()] + ".png";
        int category = 300 + bodyPart.index();
        this.setBlood(tex, mask, intensity, category);
    }

    public void setBlood(String tex, String mask, float intensity, int category) {
        intensity = Math.max(0.0F, Math.min(1.0F, intensity));
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); i++) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (shaderParam.name.equals("intensity") && (shaderParam.min != intensity || shaderParam.max != intensity)) {
                    shaderParam.min = shaderParam.max = intensity;
                    this.setDirty();
                }
            }
        } else if (intensity > 0.0F) {
            this.addOverlay(tex, mask, intensity, category);
        }
    }

    public float addBlood(String tex, BloodBodyPartType bodyPart, float intensity) {
        String mask = "media/textures/BloodTextures/" + CharacterSmartTexture.MaskFiles[bodyPart.index()] + ".png";
        int category = 300 + bodyPart.index();
        return this.addBlood(tex, mask, intensity, category);
    }

    public float addDirt(String tex, BloodBodyPartType bodyPart, float intensity) {
        String mask = "media/textures/BloodTextures/" + CharacterSmartTexture.MaskFiles[bodyPart.index()] + ".png";
        int category = 400 + bodyPart.index();
        return this.addDirt(tex, mask, intensity, category);
    }

    public float addBlood(String tex, String mask, float intensity, int category) {
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (com == null) {
            this.addOverlay(tex, mask, intensity, category);
            return intensity;
        } else {
            for (int i = 0; i < com.shaderParams.size(); i++) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (shaderParam.name.equals("intensity")) {
                    float in = shaderParam.min;
                    in += intensity;
                    in = Math.min(1.0F, in);
                    if (shaderParam.min != in || shaderParam.max != in) {
                        shaderParam.min = shaderParam.max = in;
                        this.setDirty();
                    }

                    return in;
                }
            }

            this.addOverlay(tex, mask, intensity, category);
            return intensity;
        }
    }

    public float addDirt(String tex, String mask, float intensity, int category) {
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (com == null) {
            this.addDirtOverlay(tex, mask, intensity, category);
            return intensity;
        } else {
            for (int i = 0; i < com.shaderParams.size(); i++) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (shaderParam.name.equals("intensity")) {
                    float in = shaderParam.min;
                    in += intensity;
                    in = Math.min(1.0F, in);
                    if (shaderParam.min != in || shaderParam.max != in) {
                        shaderParam.min = shaderParam.max = in;
                        this.setDirty();
                    }

                    return in;
                }
            }

            this.addOverlay(tex, mask, intensity, category);
            return intensity;
        }
    }

    public void removeBlood() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            this.removeBlood(BloodBodyPartType.FromIndex(i));
        }
    }

    public void removeDirt() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            this.removeDirt(BloodBodyPartType.FromIndex(i));
        }
    }

    public void removeBlood(BloodBodyPartType bodyPart) {
        TextureCombinerCommand com = this.getFirstFromCategory(300 + bodyPart.index());
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); i++) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (shaderParam.name.equals("intensity") && (shaderParam.min != 0.0F || shaderParam.max != 0.0F)) {
                    shaderParam.min = shaderParam.max = 0.0F;
                    this.setDirty();
                }
            }
        }
    }

    public void removeDirt(BloodBodyPartType bodyPart) {
        TextureCombinerCommand com = this.getFirstFromCategory(400 + bodyPart.index());
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); i++) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (shaderParam.name.equals("intensity") && (shaderParam.min != 0.0F || shaderParam.max != 0.0F)) {
                    shaderParam.min = shaderParam.max = 0.0F;
                    this.setDirty();
                }
            }
        }
    }

    public String getTexName() {
        return this.texName;
    }
}
