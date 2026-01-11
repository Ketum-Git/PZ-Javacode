// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.Color;
import zombie.core.textures.Texture;
import zombie.network.GameClient;

public final class UI_BodyPart extends UIElement {
    public float alpha = 1.0F;
    public final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    public BodyPartType bodyPartType;
    public boolean isFlipped;
    public float maxOscilatorRate = 0.58F;
    public float minOscilatorRate = 0.025F;
    public float oscilator;
    public float oscilatorRate = 0.02F;
    public float oscilatorStep;
    IsoGameCharacter chr;
    boolean mouseOver;
    Texture scratchTex;
    Texture bandageTex;
    Texture dirtyBandageTex;
    Texture infectionTex;
    Texture deepWoundTex;
    Texture stitchTex;
    Texture biteTex;
    Texture glassTex;
    Texture boneTex;
    Texture splintTex;
    Texture burnTex;
    Texture bulletTex;

    public UI_BodyPart(BodyPartType type, int x, int y, String part, IsoGameCharacter character, boolean RenderFlipped) {
        String sex = "male";
        if (character.isFemale()) {
            sex = "female";
        }

        this.chr = character;
        this.bodyPartType = type;
        this.scratchTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_scratch_" + part);
        this.bandageTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bandage_" + part);
        this.dirtyBandageTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bandagedirty_" + part);
        this.infectionTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_infection_" + part);
        this.biteTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bite_" + part);
        this.deepWoundTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_deepwound_" + part);
        this.stitchTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_stitches_" + part);
        this.glassTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_glass_" + part);
        this.boneTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bones_" + part);
        this.splintTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_splint_" + part);
        this.burnTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_burn_" + part);
        this.bulletTex = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_bullet_" + part);
        this.x = x;
        this.y = y;
        this.width = this.scratchTex.getWidth();
        this.height = this.scratchTex.getHeight();
        this.isFlipped = RenderFlipped;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.mouseOver = false;
    }

    @Override
    public void render() {
        BodyDamage bodyDamage = this.chr.getBodyDamage();
        if (GameClient.client && this.chr instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
            bodyDamage = this.chr.getBodyDamageRemote();
        }

        if (this.infectionTex != null && !bodyDamage.IsBandaged(this.bodyPartType) && bodyDamage.getBodyPart(this.bodyPartType).getWoundInfectionLevel() > 0.0F
            )
         {
            this.DrawTexture(this.infectionTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getWoundInfectionLevel() / 10.0F);
        }

        if (this.bandageTex != null && bodyDamage.IsBandaged(this.bodyPartType) && bodyDamage.getBodyPart(this.bodyPartType).getBandageLife() > 0.0F) {
            this.DrawTexture(this.bandageTex, 0.0, 0.0, 1.0);
        } else if (this.dirtyBandageTex != null
            && bodyDamage.IsBandaged(this.bodyPartType)
            && bodyDamage.getBodyPart(this.bodyPartType).getBandageLife() <= 0.0F) {
            this.DrawTexture(this.dirtyBandageTex, 0.0, 0.0, 1.0);
        } else if (this.scratchTex != null && bodyDamage.IsScratched(this.bodyPartType)) {
            this.DrawTexture(this.scratchTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getScratchTime() / 20.0F);
        } else if (this.scratchTex != null && bodyDamage.IsCut(this.bodyPartType)) {
            this.DrawTexture(this.scratchTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getCutTime() / 20.0F);
        } else if (this.biteTex != null
            && !bodyDamage.IsBandaged(this.bodyPartType)
            && bodyDamage.IsBitten(this.bodyPartType)
            && bodyDamage.getBodyPart(this.bodyPartType).getBiteTime() >= 0.0F) {
            this.DrawTexture(this.biteTex, 0.0, 0.0, 1.0);
        } else if (this.deepWoundTex != null && bodyDamage.IsDeepWounded(this.bodyPartType)) {
            this.DrawTexture(this.deepWoundTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getDeepWoundTime() / 15.0F);
        } else if (this.stitchTex != null && bodyDamage.IsStitched(this.bodyPartType)) {
            this.DrawTexture(this.stitchTex, 0.0, 0.0, 1.0);
        }

        if (this.boneTex != null
            && bodyDamage.getBodyPart(this.bodyPartType).getFractureTime() > 0.0F
            && bodyDamage.getBodyPart(this.bodyPartType).getSplintFactor() == 0.0F) {
            this.DrawTexture(this.boneTex, 0.0, 0.0, 1.0);
        } else if (this.splintTex != null && bodyDamage.getBodyPart(this.bodyPartType).getSplintFactor() > 0.0F) {
            this.DrawTexture(this.splintTex, 0.0, 0.0, 1.0);
        }

        if (this.glassTex != null && bodyDamage.getBodyPart(this.bodyPartType).haveGlass() && !bodyDamage.getBodyPart(this.bodyPartType).bandaged()) {
            this.DrawTexture(this.glassTex, 0.0, 0.0, 1.0);
        }

        if (this.bulletTex != null && bodyDamage.getBodyPart(this.bodyPartType).haveBullet() && !bodyDamage.getBodyPart(this.bodyPartType).bandaged()) {
            this.DrawTexture(this.bulletTex, 0.0, 0.0, 1.0);
        }

        if (this.burnTex != null && bodyDamage.getBodyPart(this.bodyPartType).getBurnTime() > 0.0F && !bodyDamage.getBodyPart(this.bodyPartType).bandaged()) {
            this.DrawTexture(this.burnTex, 0.0, 0.0, bodyDamage.getBodyPart(this.bodyPartType).getBurnTime() / 100.0F);
        }

        super.render();
    }
}
