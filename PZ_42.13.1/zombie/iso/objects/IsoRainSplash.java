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

public class IsoRainSplash extends IsoObject {
    public int age;

    @Override
    public boolean Serialize() {
        return false;
    }

    public IsoRainSplash(IsoCell cell, IsoGridSquare gridSquare) {
        if (gridSquare != null) {
            if (!gridSquare.getProperties().has(IsoFlagType.HasRainSplashes)) {
                this.age = 0;
                this.square = gridSquare;
                this.offsetX = 0.0F;
                this.offsetY = 0.0F;
                int NumRainSplashParticles = 1 + Rand.Next(2);
                int texWidth = 16;
                int texHeight = 8;

                for (int i = 0; i < NumRainSplashParticles; i++) {
                    float dx = Rand.Next(0.1F, 0.9F);
                    float dy = Rand.Next(0.1F, 0.9F);
                    int soffX = (short)(IsoUtils.XToScreen(dx, dy, 0.0F, 0) - 8.0F);
                    int soffY = (short)(IsoUtils.YToScreen(dx, dy, 0.0F, 0) - 4.0F);
                    this.AttachAnim("RainSplash", "00", 4, RainManager.rainSplashAnimDelay, -soffX, -soffY, true, 0, false, 0.7F, RainManager.rainSplashTintMod);
                    this.attachedAnimSprite.get(i).frame = (short)Rand.Next(4);
                    this.attachedAnimSprite.get(i).setScale(Core.tileScale, Core.tileScale);
                }

                gridSquare.getProperties().set(IsoFlagType.HasRainSplashes);
                RainManager.AddRainSplash(this);
            }
        }
    }

    @Override
    public String getObjectName() {
        return "RainSplashes";
    }

    @Override
    public boolean HasTooltip() {
        return false;
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare PassedObjectSquare) {
        return this.square == PassedObjectSquare;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.NoEffect;
    }

    public void ChangeTintMod(ColorInfo NewTintMod) {
        if (this.attachedAnimSprite != null) {
            int i = 0;

            while (i < this.attachedAnimSprite.size()) {
                i++;
            }
        }
    }

    @Override
    public void update() {
        this.sx = this.sy = 0.0F;
        this.age++;

        for (int n = 0; n < this.attachedAnimSprite.size(); n++) {
            IsoSpriteInstance s = this.attachedAnimSprite.get(n);
            IsoSprite sp = s.parentSprite;
            s.update();
            s.frame = s.frame + s.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F);
            if ((int)s.frame >= sp.currentAnim.frames.size() && sp.loop && s.looped) {
                s.frame = 0.0F;
            }
        }

        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                this.setAlphaAndTarget(playerIndex, 0.25F);
            } else {
                this.setAlphaAndTarget(playerIndex, 0.6F);
            }
        }
    }

    void Reset(IsoGridSquare gridSquare) {
        if (gridSquare != null) {
            if (!gridSquare.getProperties().has(IsoFlagType.HasRainSplashes)) {
                this.age = 0;
                this.square = gridSquare;
                int NumRainSplashParticles = 1 + Rand.Next(2);
                if (this.attachedAnimSprite != null) {
                    int i = 0;

                    while (i < this.attachedAnimSprite.size()) {
                        i++;
                    }
                }

                gridSquare.getProperties().set(IsoFlagType.HasRainSplashes);
                RainManager.AddRainSplash(this);
            }
        }
    }
}
