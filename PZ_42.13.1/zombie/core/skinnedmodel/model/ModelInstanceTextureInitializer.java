// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import zombie.characters.EquippedTextureCreator;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.SpriteRenderer;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.popman.ObjectPool;
import zombie.util.Type;

public final class ModelInstanceTextureInitializer {
    private int stateIndex;
    private boolean rendered;
    private ModelInstance modelInstance;
    private InventoryItem item;
    private float bloodLevel;
    private float fluidLevel;
    private final Color fluidTint = new Color();
    private int changeNumberMain;
    private int changeNumberThread;
    private final ModelInstanceTextureInitializer.RenderData[] renderData = new ModelInstanceTextureInitializer.RenderData[3];
    private static final ObjectPool<ModelInstanceTextureInitializer> pool = new ObjectPool<>(ModelInstanceTextureInitializer::new);

    public void init(ModelInstance modelInstance, InventoryItem item) {
        this.stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.item = item;
        this.modelInstance = modelInstance;
        HandWeapon weapon = Type.tryCastTo(item, HandWeapon.class);
        this.bloodLevel = weapon == null ? 0.0F : weapon.getBloodLevel();
        this.fluidLevel = 0.0F;
        FluidContainer fluidContainer = item.getFluidContainer();
        if (fluidContainer != null) {
            this.fluidLevel = fluidContainer.getFilledRatio();
            this.fluidTint.set(fluidContainer.getColor());
        }

        this.setDirty();
    }

    public void init(ModelInstance modelInstance, float bloodLevel) {
        this.stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.item = null;
        this.modelInstance = modelInstance;
        this.bloodLevel = bloodLevel;
        this.fluidLevel = 0.0F;
        this.setDirty();
    }

    public void setDirty() {
        this.changeNumberMain++;
        this.rendered = false;
    }

    public boolean isDirty() {
        return !this.rendered;
    }

    public void renderMain() {
        if (!this.rendered) {
            int stateIndex = this.stateIndex;
            if (this.renderData[stateIndex] == null) {
                this.renderData[stateIndex] = new ModelInstanceTextureInitializer.RenderData();
            }

            ModelInstanceTextureInitializer.RenderData renderData = this.renderData[stateIndex];
            if (renderData.textureCreator == null) {
                renderData.changeNumber = this.changeNumberMain;
                renderData.textureCreator = EquippedTextureCreator.alloc();
                if (this.item == null) {
                    renderData.textureCreator.init(this.modelInstance, this.bloodLevel, ImmutableColor.white, this.fluidLevel, this.fluidTint);
                } else {
                    renderData.textureCreator.init(this.modelInstance, this.item);
                }

                renderData.rendered = false;
            }
        }
    }

    public void render() {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        ModelInstanceTextureInitializer.RenderData renderData = this.renderData[stateIndex];
        if (renderData != null) {
            if (renderData.textureCreator != null) {
                if (!renderData.rendered) {
                    if (renderData.changeNumber == this.changeNumberThread) {
                        renderData.rendered = true;
                    } else {
                        renderData.textureCreator.render();
                        if (renderData.textureCreator.isRendered()) {
                            this.changeNumberThread = renderData.changeNumber;
                            renderData.rendered = true;
                        }
                    }
                }
            }
        }
    }

    public void postRender() {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        ModelInstanceTextureInitializer.RenderData renderData = this.renderData[stateIndex];
        if (renderData != null) {
            if (renderData.textureCreator != null) {
                if (renderData.textureCreator.isRendered() && renderData.changeNumber == this.changeNumberMain) {
                    this.rendered = true;
                }

                if (renderData.rendered) {
                    renderData.textureCreator.postRender();
                    renderData.textureCreator = null;
                }
            }
        }
    }

    public boolean isRendered() {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        ModelInstanceTextureInitializer.RenderData renderData = this.renderData[stateIndex];
        if (renderData == null) {
            return true;
        } else {
            return renderData.textureCreator == null ? true : renderData.rendered;
        }
    }

    public static ModelInstanceTextureInitializer alloc() {
        return pool.alloc();
    }

    public void release() {
        this.item = null;
        this.modelInstance = null;
        pool.release(this);
    }

    private static final class RenderData {
        int changeNumber;
        boolean rendered;
        EquippedTextureCreator textureCreator;
    }
}
