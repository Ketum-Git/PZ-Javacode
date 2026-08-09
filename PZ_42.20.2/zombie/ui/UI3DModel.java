// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.Objects;
import org.lwjgl.opengl.GL11;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Styles.UIFBOStyle;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.population.IClothingItemListener;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.util.StringUtils;

@UsedFromLua
public final class UI3DModel extends UIElement implements IClothingItemListener {
    private final AnimatedModel animatedModel = new AnimatedModel();
    private IsoDirections dir = IsoDirections.E;
    private boolean doExt;
    private long nextExt = -1L;
    private final UI3DModel.Drawer[] drawers = new UI3DModel.Drawer[3];
    private float zoom;
    private float yOffset;
    private float xOffset;

    public UI3DModel(KahluaTable table) {
        super(table);

        for (int i = 0; i < this.drawers.length; i++) {
            this.drawers[i] = new UI3DModel.Drawer();
        }

        if (OutfitManager.instance != null) {
            OutfitManager.instance.addClothingItemListener(this);
        }
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            super.render();
            if (this.parent == null || this.parent.maxDrawHeight == -1 || !(this.parent.maxDrawHeight <= this.y)) {
                if (this.doExt) {
                    long now = System.currentTimeMillis();
                    if (this.nextExt < 0L) {
                        this.nextExt = now + Rand.Next(5000, 10000);
                    }

                    if (this.nextExt < now) {
                        this.animatedModel.getActionContext().reportEvent("EventDoExt");
                        this.animatedModel.setVariable("Ext", Rand.Next(0, 6) + 1);
                        this.nextExt = -1L;
                    }
                }

                this.animatedModel.update();
                UI3DModel.Drawer drawer = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
                drawer.init(this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue());
                SpriteRenderer.instance.drawGeneric(drawer);
            }
        }
    }

    public void setDirection(IsoDirections dir) {
        this.dir = dir;
        if (dir != null) {
            this.animatedModel.setAngle(dir.ToVector());
        }
    }

    public IsoDirections getDirection() {
        return this.dir;
    }

    public void setAnimate(boolean animate) {
        this.animatedModel.setAnimate(animate);
    }

    public void setAnimSetName(String name) {
        this.animatedModel.setAnimSetName(name);
    }

    public void setDoRandomExtAnimations(boolean doExt) {
        this.doExt = doExt;
    }

    public void setIsometric(boolean iso) {
        this.animatedModel.setIsometric(iso);
    }

    public void setOutfitName(String outfitName, boolean female, boolean zombie) {
        this.animatedModel.setOutfitName(outfitName, female, zombie);
    }

    public void setCharacter(IsoGameCharacter character) {
        this.animatedModel.setCharacter(character);
    }

    public IsoGameCharacter getCharacter() {
        return this.animatedModel.getCharacter();
    }

    public void setSurvivorDesc(SurvivorDesc survivorDesc) {
        this.animatedModel.setSurvivorDesc(survivorDesc);
    }

    public void setState(String state) {
        this.animatedModel.setState(state);
    }

    public String getState() {
        return this.animatedModel.getState();
    }

    public void setVariable(String key, String value) {
        this.animatedModel.setVariable(key, value);
    }

    public void setVariable(String key, boolean value) {
        this.animatedModel.setVariable(key, value);
    }

    public Object getVariable(String key) {
        return this.animatedModel.getVariable(key);
    }

    public void setVariable(String key, float value) {
        this.animatedModel.setVariable(key, value);
    }

    public void clearVariable(String key) {
        this.animatedModel.clearVariable(key);
    }

    public void clearVariables() {
        this.animatedModel.clearVariables();
    }

    public void reportEvent(String event) {
        if (!StringUtils.isNullOrWhitespace(event)) {
            this.animatedModel.getActionContext().reportEvent(event);
        }
    }

    @Override
    public void clothingItemChanged(String itemGuid) {
        this.animatedModel.clothingItemChanged(itemGuid);
    }

    public void setZoom(float newZoom) {
        this.zoom = newZoom;
    }

    public void setYOffset(float newYOffset) {
        this.yOffset = newYOffset;
    }

    public void setXOffset(float newXOffset) {
        this.xOffset = newXOffset;
    }

    private final class Drawer extends TextureDraw.GenericDrawer {
        int absX;
        int absY;
        float animPlayerAngle;
        float zoom;
        boolean rendered;

        private Drawer() {
            Objects.requireNonNull(UI3DModel.this);
            super();
        }

        public void init(int x, int y) {
            this.absX = x;
            this.absY = y;
            this.animPlayerAngle = UI3DModel.this.animatedModel.getAnimationPlayer().getRenderedAngle();
            this.zoom = UI3DModel.this.zoom;
            this.rendered = false;
            float newyOffset = UI3DModel.this.animatedModel.isIsometric() ? -0.45F : -0.5F;
            if (UI3DModel.this.yOffset != 0.0F) {
                newyOffset = UI3DModel.this.yOffset;
            }

            UI3DModel.this.animatedModel.setOffset(UI3DModel.this.xOffset, newyOffset, 0.0F);
            UI3DModel.this.animatedModel.renderMain();
        }

        @Override
        public void render() {
            float size = UI3DModel.this.animatedModel.isIsometric() ? 22.0F : 25.0F;
            size -= this.zoom;
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glClearDepth(1.0);
            UI3DModel.this.animatedModel
                .DoRender(
                    this.absX,
                    Core.height - this.absY - (int)UI3DModel.this.height,
                    (int)UI3DModel.this.width,
                    (int)UI3DModel.this.height,
                    size,
                    this.animPlayerAngle
                );
            UIFBOStyle.instance.setupState();
            this.rendered = true;
        }

        @Override
        public void postRender() {
            UI3DModel.this.animatedModel.postRender(this.rendered);
        }
    }
}
