// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import zombie.UsedFromLua;
import zombie.core.PerformanceSettings;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.popman.ObjectPool;

@UsedFromLua
public final class IsoSpriteInstance {
    public static final ObjectPool<IsoSpriteInstance> pool = new ObjectPool<>(IsoSpriteInstance::new);
    private static final AtomicBoolean lock = new AtomicBoolean(false);
    public IsoSprite parentSprite;
    public float tintb = 1.0F;
    public float tintg = 1.0F;
    public float tintr = 1.0F;
    public float frame;
    public float alpha = 1.0F;
    public float targetAlpha = 1.0F;
    public boolean copyTargetAlpha = true;
    public boolean multiplyObjectAlpha;
    public boolean flip;
    public float offZ;
    public float offX;
    public float offY;
    public float animFrameIncrease = 1.0F;
    static float multiplier = 1.0F;
    public boolean looped = true;
    public boolean finished;
    public boolean nextFrame;
    public float scaleX = 1.0F;
    public float scaleY = 1.0F;

    public static IsoSpriteInstance get(IsoSprite spr) {
        while (!lock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        IsoSpriteInstance spri = pool.alloc();
        lock.set(false);
        spri.parentSprite = spr;
        spri.reset();
        return spri;
    }

    private void reset() {
        this.tintb = 1.0F;
        this.tintg = 1.0F;
        this.tintr = 1.0F;
        this.frame = 0.0F;
        this.alpha = 1.0F;
        this.targetAlpha = 1.0F;
        this.copyTargetAlpha = true;
        this.multiplyObjectAlpha = false;
        this.flip = false;
        this.offZ = 0.0F;
        this.offX = 0.0F;
        this.offY = 0.0F;
        this.animFrameIncrease = 1.0F;
        multiplier = 1.0F;
        this.looped = true;
        this.finished = false;
        this.nextFrame = false;
        this.scaleX = 1.0F;
        this.scaleY = 1.0F;
    }

    public IsoSpriteInstance() {
    }

    public void setFrameSpeedPerFrame(float perSecond) {
        this.animFrameIncrease = perSecond * multiplier;
    }

    public int getID() {
        return this.parentSprite.id;
    }

    public String getName() {
        return this.parentSprite.getName();
    }

    public IsoSprite getParentSprite() {
        return this.parentSprite;
    }

    public IsoSpriteInstance(IsoSprite spr) {
        this.parentSprite = spr;
    }

    public float getTintR() {
        return this.tintr;
    }

    public float getTintG() {
        return this.tintg;
    }

    public float getTintB() {
        return this.tintb;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public float getTargetAlpha() {
        return this.targetAlpha;
    }

    public boolean isCopyTargetAlpha() {
        return this.copyTargetAlpha;
    }

    public boolean isMultiplyObjectAlpha() {
        return this.multiplyObjectAlpha;
    }

    public void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2) {
        this.parentSprite.render(this, obj, x, y, z, dir, offsetX, offsetY, info2, true);
    }

    public void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep) {
        this.parentSprite.render(this, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep);
    }

    public void render(
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        this.parentSprite.render(this, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
    }

    public void SetAlpha(float f) {
        this.alpha = f;
        this.copyTargetAlpha = false;
    }

    public void SetTargetAlpha(float targetAlpha) {
        this.targetAlpha = targetAlpha;
        this.copyTargetAlpha = false;
    }

    public void update() {
    }

    protected void renderprep(IsoObject obj) {
        if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue() && obj != null) {
            obj.setAlphaAndTarget(1.0F);
        }

        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.forceAlphaToTarget.getValue()) {
            if (obj != null && this.copyTargetAlpha) {
                this.targetAlpha = obj.getTargetAlpha(IsoCamera.frameState.playerIndex);
            }

            this.alpha = this.targetAlpha;
        } else if (obj != null && this.copyTargetAlpha) {
            this.targetAlpha = obj.getTargetAlpha(IsoCamera.frameState.playerIndex);
            this.alpha = obj.getAlpha(IsoCamera.frameState.playerIndex);
        } else if (!this.multiplyObjectAlpha) {
            if (this.alpha < this.targetAlpha) {
                this.alpha = this.alpha + IsoSprite.alphaStep;
                if (this.alpha > this.targetAlpha) {
                    this.alpha = this.targetAlpha;
                }
            } else if (this.alpha > this.targetAlpha) {
                this.alpha = this.alpha - IsoSprite.alphaStep;
                if (this.alpha < this.targetAlpha) {
                    this.alpha = this.targetAlpha;
                }
            }

            if (this.alpha < 0.0F) {
                this.alpha = 0.0F;
            }

            if (this.alpha > 1.0F) {
                this.alpha = 1.0F;
            }
        }
    }

    public float getFrame() {
        return this.frame;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void Dispose() {
    }

    public void RenderGhostTileColor(int x, int y, int z, float r, float g, float b, float a) {
        if (this.parentSprite != null) {
            IsoSpriteInstance spriteInstance = get(this.parentSprite);
            spriteInstance.frame = this.frame;
            spriteInstance.tintr = r;
            spriteInstance.tintg = g;
            spriteInstance.tintb = b;
            spriteInstance.alpha = spriteInstance.targetAlpha = a;
            IsoGridSquare.getDefColorInfo().r = IsoGridSquare.getDefColorInfo().g = IsoGridSquare.getDefColorInfo().b = IsoGridSquare.getDefColorInfo().a = 1.0F;
            this.parentSprite.render(spriteInstance, null, x, y, z, IsoDirections.N, 0.0F, -144.0F, IsoGridSquare.getDefColorInfo(), true);
        }
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public void scaleAspect(float texW, float texH, float width, float height) {
        if (texW > 0.0F && texH > 0.0F && width > 0.0F && height > 0.0F) {
            float rw = height * texW / texH;
            float rh = width * texH / texW;
            boolean useHeight = rw <= width;
            if (useHeight) {
                width = rw;
            } else {
                height = rh;
            }

            this.scaleX = width / texW;
            this.scaleY = height / texH;
        }
    }

    public static void add(IsoSpriteInstance isoSpriteInstance) {
        if (isoSpriteInstance != null) {
            isoSpriteInstance.reset();

            while (!lock.compareAndSet(false, true)) {
                Thread.onSpinWait();
            }

            pool.release(isoSpriteInstance);
            lock.set(false);
        }
    }
}
