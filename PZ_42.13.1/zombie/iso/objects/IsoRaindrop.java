// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

public class IsoRaindrop extends IsoObject {
    public int animSpriteIndex;
    public float gravMod;
    public int life;
    public float splashY;
    public float offsetY;
    public float velY;

    @Override
    public boolean Serialize() {
        return false;
    }

    public IsoRaindrop(IsoCell cell, IsoGridSquare gridSquare, boolean CanSee) {
        if (CanSee) {
            if (gridSquare != null) {
                if (!gridSquare.getProperties().has(IsoFlagType.HasRaindrop)) {
                    this.life = 0;
                    this.square = gridSquare;
                    int texWidth = 1 * Core.tileScale;
                    int texHeight = 64 * Core.tileScale;
                    float dx = Rand.Next(0.1F, 0.9F);
                    float dy = Rand.Next(0.1F, 0.9F);
                    int soffX = (short)(IsoUtils.XToScreen(dx, dy, 0.0F, 0) - texWidth / 2);
                    int soffY = (short)(IsoUtils.YToScreen(dx, dy, 0.0F, 0) - texHeight);
                    this.offsetX = 0.0F;
                    this.offsetY = 0.0F;
                    this.offsetY = RainManager.raindropStartDistance;
                    this.splashY = soffY;
                    this.AttachAnim("Rain", "00", 1, 0.0F, -soffX, -soffY, true, 0, false, 0.7F, RainManager.raindropTintMod);
                    if (this.attachedAnimSprite != null) {
                        this.animSpriteIndex = this.attachedAnimSprite.size() - 1;
                    } else {
                        this.animSpriteIndex = 0;
                    }

                    this.attachedAnimSprite.get(this.animSpriteIndex).setScale(Core.tileScale, Core.tileScale);
                    gridSquare.getProperties().set(IsoFlagType.HasRaindrop);
                    this.velY = 0.0F;
                    float Modulus = 1000000.0F / Rand.Next(1000000) + 1.0E-5F;
                    this.gravMod = -(RainManager.gravModMin + (RainManager.gravModMax - RainManager.gravModMin) * Modulus);
                    RainManager.AddRaindrop(this);
                }
            }
        }
    }

    @Override
    public boolean HasTooltip() {
        return false;
    }

    @Override
    public String getObjectName() {
        return "RainDrops";
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare PassedObjectSquare) {
        return this.square == PassedObjectSquare;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.NoEffect;
    }

    public void ChangeTintMod(ColorInfo NewTintMod) {
    }

    @Override
    public void update() {
        this.sx = this.sy = 0.0F;
        this.life++;

        for (int n = 0; n < this.attachedAnimSprite.size(); n++) {
            IsoSpriteInstance s = this.attachedAnimSprite.get(n);
            s.update();
            s.frame = s.frame + s.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F);
            IsoSprite sp = s.parentSprite;
            if ((int)s.frame >= sp.currentAnim.frames.size() && sp.loop && s.looped) {
                s.frame = 0.0F;
            }
        }

        this.velY = this.velY + this.gravMod * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F);
        this.offsetY = this.offsetY + this.velY;
        if (this.attachedAnimSprite != null && this.attachedAnimSprite.size() > this.animSpriteIndex && this.animSpriteIndex >= 0) {
            this.attachedAnimSprite.get(this.animSpriteIndex).parentSprite.soffY = (short)(this.splashY + (int)this.offsetY);
        }

        if (this.offsetY < 0.0F) {
            this.offsetY = RainManager.raindropStartDistance;
            this.velY = 0.0F;
            float Modulus = 1000000.0F / Rand.Next(1000000) + 1.0E-5F;
            this.gravMod = -(RainManager.gravModMin + (RainManager.gravModMax - RainManager.gravModMin) * Modulus);
        }

        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                this.setAlphaAndTarget(playerIndex, 0.55F);
            } else {
                this.setAlphaAndTarget(playerIndex, 1.0F);
            }
        }
    }

    void Reset(IsoGridSquare gridSquare, boolean CanSee) {
        if (CanSee) {
            if (gridSquare != null) {
                if (!gridSquare.getProperties().has(IsoFlagType.HasRaindrop)) {
                    this.life = 0;
                    this.square = gridSquare;
                    this.offsetY = RainManager.raindropStartDistance;
                    if (this.attachedAnimSprite != null) {
                        this.animSpriteIndex = this.attachedAnimSprite.size() - 1;
                    } else {
                        this.animSpriteIndex = 0;
                    }

                    gridSquare.getProperties().set(IsoFlagType.HasRaindrop);
                    this.velY = 0.0F;
                    float Modulus = 1000000.0F / Rand.Next(1000000) + 1.0E-5F;
                    this.gravMod = -(RainManager.gravModMin + (RainManager.gravModMax - RainManager.gravModMin) * Modulus);
                    RainManager.AddRaindrop(this);
                }
            }
        }
    }
}
