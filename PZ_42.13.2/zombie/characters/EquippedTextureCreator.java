// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import zombie.characterTextures.ItemSmartTexture;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.popman.ObjectPool;

public final class EquippedTextureCreator extends TextureDraw.GenericDrawer {
    private boolean rendered;
    private ModelInstance modelInstance;
    private float bloodLevel;
    private float fluidLevel;
    private String fluidTextureMask;
    private final Color fluidTint = new Color();
    private ImmutableColor tint = ImmutableColor.white;
    private String tintMask;
    private final ArrayList<Texture> texturesNotReady = new ArrayList<>();
    private static final ObjectPool<EquippedTextureCreator> pool = new ObjectPool<>(EquippedTextureCreator::new);

    public void init(ModelInstance modelInstance, InventoryItem item) {
        float bloodLevel = 0.0F;
        if (item instanceof HandWeapon weapon) {
            bloodLevel = weapon.getBloodLevel();
        }

        ImmutableColor tint = ImmutableColor.white;
        if (item.getColorRed() * item.getColorGreen() * item.getColorBlue() != 1.0F) {
            tint = new ImmutableColor(item.getColorRed(), item.getColorGreen(), item.getColorBlue());
        }

        float fluidLevel = 0.0F;
        Color fluidTint = null;
        FluidContainer fluidContainer = item.getFluidContainer();
        if (fluidContainer != null) {
            fluidLevel = fluidContainer.getFilledRatio();
            fluidTint = fluidContainer.getColor();
        }

        this.init(modelInstance, bloodLevel, tint, fluidLevel, fluidTint);
    }

    public void init(ModelInstance _modelInstance, float bloodLevel, ImmutableColor tint, float fluidLevel, Color fluidTint) {
        this.rendered = false;
        this.texturesNotReady.clear();
        this.modelInstance = _modelInstance;
        this.bloodLevel = bloodLevel;
        this.tint = tint;
        this.tintMask = null;
        this.fluidLevel = fluidLevel;
        this.fluidTextureMask = null;
        if (this.modelInstance != null) {
            this.modelInstance.renderRefCount++;
            Texture texture = this.modelInstance.tex;
            if (texture instanceof ItemSmartTexture smartTexture) {
                assert smartTexture.getTexName() != null;

                texture = this.getTextureWithFlags(smartTexture.getTexName());
            }

            if (texture != null && !texture.isReady()) {
                this.texturesNotReady.add(texture);
            }

            String texName = texture == null ? null : texture.getName();
            if (texName != null) {
                this.tintMask = this.initTextureName(texName, "TINT");
                texture = this.getTextureWithFlags(this.tintMask);
                if (texture == null) {
                    this.tintMask = null;
                } else if (!texture.isReady()) {
                    this.texturesNotReady.add(texture);
                }
            }

            texture = this.getTextureWithFlags("media/textures/BloodTextures/BloodOverlayWeapon.png");
            if (texture != null && !texture.isReady()) {
                this.texturesNotReady.add(texture);
            }

            texture = this.getTextureWithFlags("media/textures/BloodTextures/BloodOverlayWeaponMask.png");
            if (texture != null && !texture.isReady()) {
                this.texturesNotReady.add(texture);
            }

            if (fluidLevel > 0.0F && texName != null) {
                texture = Texture.getSharedTexture("media/textures/FullAlpha.png");
                if (texture != null && !texture.isReady()) {
                    this.texturesNotReady.add(texture);
                }

                String textureMask = this.initTextureName(texName, "FLUIDTINT");
                texture = Texture.getSharedTexture(textureMask);
                if (texture != null) {
                    if (!texture.isReady()) {
                        this.texturesNotReady.add(texture);
                    }

                    this.fluidTextureMask = textureMask;
                }

                this.fluidTint.set(fluidTint);
            }
        }
    }

    @Override
    public void render() {
        for (int i = 0; i < this.texturesNotReady.size(); i++) {
            Texture texture = this.texturesNotReady.get(i);
            if (!texture.isReady()) {
                return;
            }
        }

        GL11.glPushAttrib(2048);

        try {
            this.updateTexture(this.modelInstance, this.bloodLevel);
        } finally {
            GL11.glPopAttrib();
        }

        this.rendered = true;
    }

    private Texture getTextureWithFlags(String fileName) {
        return Texture.getSharedTexture(fileName, ModelManager.instance.getTextureFlags());
    }

    private void updateTexture(ModelInstance modelInstance, float bloodLevel) {
        if (modelInstance != null) {
            ItemSmartTexture itemSmartTexture = null;
            if (this.tint.equals(ImmutableColor.white) && !(bloodLevel > 0.0F)) {
                if (modelInstance.tex instanceof ItemSmartTexture smartTexture) {
                    itemSmartTexture = smartTexture;
                }
            } else if (modelInstance.tex instanceof ItemSmartTexture smartTexture) {
                itemSmartTexture = smartTexture;
            } else if (modelInstance.tex != null) {
                itemSmartTexture = new ItemSmartTexture(modelInstance.tex.getName());
            }

            if (itemSmartTexture != null) {
                String textureName = itemSmartTexture.getTexName();

                assert textureName != null;

                itemSmartTexture.clear();
                itemSmartTexture.add(textureName);
                if (!ImmutableColor.white.equals(this.tint)) {
                    if (this.tintMask != null) {
                        itemSmartTexture.setTintMask(this.tintMask, "media/textures/FullAlpha.png", 300, this.tint.toMutableColor());
                    } else {
                        itemSmartTexture.addTint(textureName, 300, this.tint.getRedFloat(), this.tint.getGreenFloat(), this.tint.getBlueFloat());
                    }
                }

                if (bloodLevel > 0.0F) {
                    itemSmartTexture.setBlood(
                        "media/textures/BloodTextures/BloodOverlayWeapon.png", "media/textures/BloodTextures/BloodOverlayWeaponMask.png", bloodLevel, 301
                    );
                }

                if (this.fluidTextureMask != null && Texture.getTexture(this.fluidTextureMask) != null) {
                    itemSmartTexture.setFluid(this.fluidTextureMask, "media/textures/FullAlpha.png", this.fluidLevel, 302, this.fluidTint);
                }

                itemSmartTexture.calculate();
                modelInstance.tex = itemSmartTexture;
            }
        }
    }

    @Override
    public void postRender() {
        ModelManager.instance.derefModelInstance(this.modelInstance);
        this.texturesNotReady.clear();
        if (!this.rendered) {
        }

        this.modelInstance = null;
        pool.release(this);
    }

    private String initTextureName(String textureName, String suffix) {
        if (textureName.endsWith(".png")) {
            textureName = textureName.substring(0, textureName.length() - 4);
        }

        return !textureName.contains("media/") && !textureName.contains("media\\")
            ? "media/textures/" + textureName + suffix + ".png"
            : textureName + suffix + ".png";
    }

    public boolean isRendered() {
        return this.rendered;
    }

    public static EquippedTextureCreator alloc() {
        return pool.alloc();
    }
}
