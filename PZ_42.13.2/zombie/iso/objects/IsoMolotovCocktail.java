// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoPhysicsObject;
import zombie.network.GameClient;

@UsedFromLua
public class IsoMolotovCocktail extends IsoPhysicsObject {
    private HandWeapon weapon;
    private IsoGameCharacter character;
    private int timer;
    private int explodeTimer;

    @Override
    public String getObjectName() {
        return "MolotovCocktail";
    }

    public IsoMolotovCocktail(IsoCell cell) {
        super(cell);
    }

    public IsoMolotovCocktail(IsoCell cell, float x, float y, float z, float xVelocity, float yVelocity, HandWeapon weapon, IsoGameCharacter character) {
        super(cell);
        this.weapon = weapon;
        this.character = character;
        this.explodeTimer = weapon.getTriggerExplosionTimer();
        this.velX = xVelocity;
        this.velY = yVelocity;
        float randX = Rand.Next(4000) / 10000.0F;
        float randY = Rand.Next(4000) / 10000.0F;
        randX -= 0.2F;
        randY -= 0.2F;
        this.velX += randX;
        this.velY += randY;
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.offsetX = 0.0F;
        this.offsetY = 0.0F;
        this.terminalVelocity = -0.02F;
        Texture tex = this.sprite.LoadSingleTexture(weapon.getTex().getName());
        if (tex != null) {
            this.sprite.animate = false;
            int SCL = Core.tileScale;
            this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * SCL, 16 * SCL);
        }

        this.speedMod = 0.6F;
    }

    public void collideCharacter() {
        if (this.explodeTimer == 0) {
            this.Explode();
        }
    }

    @Override
    public void collideGround() {
        if (this.explodeTimer == 0) {
            this.Explode();
        }
    }

    @Override
    public void collideWall() {
        if (this.explodeTimer == 0) {
            this.Explode();
        }
    }

    @Override
    public void update() {
        super.update();
        if (!this.isDestroyed()) {
            if (this.isCollidedThisFrame() && this.explodeTimer == 0) {
                this.Explode();
            }

            if (this.explodeTimer > 0) {
                this.timer++;
                if (this.timer >= this.explodeTimer) {
                    this.Explode();
                }
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(this.weapon)) {
            ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(this.weapon, this.getSquare(), this.getRenderSquare(), x, y, z, 0.0F);
            if (status == ItemModelRenderer.RenderStatus.Loading || status == ItemModelRenderer.RenderStatus.Ready) {
                return;
            }
        }

        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        if (Core.debug) {
        }
    }

    private void Explode() {
        if (!this.isDestroyed() && this.getCurrentSquare() != null) {
            this.setDestroyed(true);
            this.getCurrentSquare().getMovingObjects().remove(this);
            this.getCell().Remove(this);
            if (GameClient.client) {
                if (!(this.character instanceof IsoPlayer isoPlayer) || !isoPlayer.isLocalPlayer()) {
                    return;
                }

                this.square.syncIsoTrap(this.weapon);
            }

            IsoTrap trap = new IsoTrap(this.character, this.weapon, this.getCurrentSquare().getCell(), this.getCurrentSquare());
            if (this.weapon.isInstantExplosion()) {
                trap.triggerExplosion(false);
            } else {
                trap.getSquare().AddTileObject(trap);
            }
        }
    }
}
