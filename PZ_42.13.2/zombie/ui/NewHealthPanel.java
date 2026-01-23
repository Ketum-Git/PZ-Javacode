// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.network.GameClient;

@UsedFromLua
public final class NewHealthPanel extends NewWindow {
    public static NewHealthPanel instance;
    public Texture bodyOutline;
    public UI_BodyPart footL;
    public UI_BodyPart footR;
    public UI_BodyPart foreArmL;
    public UI_BodyPart foreArmR;
    public UI_BodyPart groin;
    public UI_BodyPart handL;
    public UI_BodyPart handR;
    public UI_BodyPart head;
    public UI_BodyPart lowerLegL;
    public UI_BodyPart lowerLegR;
    public UI_BodyPart neck;
    public UI_BodyPart torsoLower;
    public UI_BodyPart torsoUpper;
    public UI_BodyPart upperArmL;
    public UI_BodyPart upperArmR;
    public UI_BodyPart upperLegL;
    public UI_BodyPart upperLegR;
    public Texture healthBar;
    public Texture healthBarBack;
    public Texture healthIcon;
    private static final int UI_BORDER_SPACING = 10;
    IsoGameCharacter parentChar;

    public void SetCharacter(IsoGameCharacter chr) {
        this.parentChar = chr;
    }

    public NewHealthPanel(int x, int y, IsoGameCharacter ParentCharacter) {
        super(x, y, 10, 10, true);
        this.parentChar = ParentCharacter;
        this.resizeToFitY = false;
        this.visible = false;
        instance = this;
        int flags = 2;
        this.healthIcon = Texture.getSharedTexture("media/ui/Heart_On.png", 2);
        this.healthBarBack = Texture.getSharedTexture("media/ui/BodyDamage/DamageBar_Vert.png", 2);
        this.healthBar = Texture.getSharedTexture("media/ui/BodyDamage/DamageBar_Vert_Fill.png", 2);
        String sex = "male";
        if (ParentCharacter.isFemale()) {
            sex = "female";
        }

        this.bodyOutline = Texture.getSharedTexture("media/ui/BodyDamage/" + sex + "_base.png");
        int bodyOutlineX = this.bodyOutline.getWidth() - 123 + 2;
        int bodyOutlineY = this.bodyOutline.getHeight() - 302 + 2;
        this.width = this.bodyOutline.getWidth() + this.healthIcon.getWidth() + 10;
        this.height = 270 + this.titleRight.getHeight() + 5;
        this.handL = new UI_BodyPart(BodyPartType.Hand_L, bodyOutlineX, bodyOutlineY, "hand_left.png", this.parentChar, false);
        this.handR = new UI_BodyPart(BodyPartType.Hand_R, bodyOutlineX, bodyOutlineY, "hand_right.png", this.parentChar, false);
        this.foreArmL = new UI_BodyPart(BodyPartType.ForeArm_L, bodyOutlineX, bodyOutlineY, "lowerarm_left.png", this.parentChar, false);
        this.foreArmR = new UI_BodyPart(BodyPartType.ForeArm_R, bodyOutlineX, bodyOutlineY, "lowerarm_right.png", this.parentChar, false);
        this.upperArmL = new UI_BodyPart(BodyPartType.UpperArm_L, bodyOutlineX, bodyOutlineY, "upperarm_left.png", this.parentChar, false);
        this.upperArmR = new UI_BodyPart(BodyPartType.UpperArm_R, bodyOutlineX, bodyOutlineY, "upperarm_right.png", this.parentChar, false);
        this.torsoUpper = new UI_BodyPart(BodyPartType.Torso_Upper, bodyOutlineX, bodyOutlineY, "chest.png", this.parentChar, false);
        this.torsoLower = new UI_BodyPart(BodyPartType.Torso_Lower, bodyOutlineX, bodyOutlineY, "abdomen.png", this.parentChar, false);
        this.head = new UI_BodyPart(BodyPartType.Head, bodyOutlineX, bodyOutlineY, "head.png", this.parentChar, false);
        this.neck = new UI_BodyPart(BodyPartType.Neck, bodyOutlineX, bodyOutlineY, "neck.png", this.parentChar, false);
        this.groin = new UI_BodyPart(BodyPartType.Groin, bodyOutlineX, bodyOutlineY, "groin.png", this.parentChar, false);
        this.upperLegL = new UI_BodyPart(BodyPartType.UpperLeg_L, bodyOutlineX, bodyOutlineY, "upperleg_left.png", this.parentChar, false);
        this.upperLegR = new UI_BodyPart(BodyPartType.UpperLeg_R, bodyOutlineX, bodyOutlineY, "upperleg_right.png", this.parentChar, false);
        this.lowerLegL = new UI_BodyPart(BodyPartType.LowerLeg_L, bodyOutlineX, bodyOutlineY, "lowerleg_left.png", this.parentChar, false);
        this.lowerLegR = new UI_BodyPart(BodyPartType.LowerLeg_R, bodyOutlineX, bodyOutlineY, "lowerleg_right.png", this.parentChar, false);
        this.footL = new UI_BodyPart(BodyPartType.Foot_L, bodyOutlineX, bodyOutlineY, "foot_left.png", this.parentChar, false);
        this.footR = new UI_BodyPart(BodyPartType.Foot_R, bodyOutlineX, bodyOutlineY, "foot_right.png", this.parentChar, false);
        this.AddChild(this.handL);
        this.AddChild(this.handR);
        this.AddChild(this.foreArmL);
        this.AddChild(this.foreArmR);
        this.AddChild(this.upperArmL);
        this.AddChild(this.upperArmR);
        this.AddChild(this.torsoUpper);
        this.AddChild(this.torsoLower);
        this.AddChild(this.head);
        this.AddChild(this.neck);
        this.AddChild(this.groin);
        this.AddChild(this.upperLegL);
        this.AddChild(this.upperLegR);
        this.AddChild(this.lowerLegL);
        this.AddChild(this.lowerLegR);
        this.AddChild(this.footL);
        this.AddChild(this.footR);
    }

    @Override
    public void render() {
        int bodyOutlineWidth = this.bodyOutline.getWidth();
        int bodyOutlineHeight = this.bodyOutline.getHeight();
        if (this.isVisible()) {
            this.DrawTexture(this.bodyOutline, bodyOutlineWidth - 123 + 2, bodyOutlineHeight - 302 + 2, this.alpha);
            this.handL.render();
            this.handR.render();
            this.foreArmL.render();
            this.foreArmR.render();
            this.upperArmL.render();
            this.upperArmR.render();
            this.torsoUpper.render();
            this.torsoLower.render();
            this.head.render();
            this.neck.render();
            this.groin.render();
            this.upperLegL.render();
            this.upperLegR.render();
            this.lowerLegL.render();
            this.lowerLegR.render();
            this.footL.render();
            this.footR.render();
            BodyDamage bodyDamage = this.parentChar.getBodyDamage();
            if (GameClient.client && this.parentChar instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
                bodyDamage = this.parentChar.getBodyDamageRemote();
            }

            double borderRGB = 0.15;
            double borderA = 1.0;
            this.DrawTextureScaledColor(null, bodyOutlineWidth + 10 + 0.0, 0.0, 25.0, 258.0, 0.15, 0.15, 0.15, 1.0);
            float HealthBarLength = (100.0F - bodyDamage.getHealth()) * 2.56F;
            this.DrawTexture(this.healthIcon, bodyOutlineWidth + 10 - 1, bodyOutlineHeight - this.healthIcon.getHeight() - 2, this.alpha);
            this.DrawTextureScaled(this.healthBarBack, bodyOutlineWidth + 10 + 1, 1.0, 23.0, 256.0, this.alpha);
            this.DrawTextureScaled(this.healthBar, bodyOutlineWidth + 10 + 1, 1 + (int)HealthBarLength, 23.0, 256 - (int)HealthBarLength, this.alpha);
            if (Core.debug && DebugOptions.instance.uiRenderOutline.getValue()) {
                Double x = -this.getXScroll();
                Double y = -this.getYScroll();
                this.DrawTextureScaledColor(null, x, y, 1.0, (double)this.height, 1.0, 1.0, 1.0, 0.5);
                this.DrawTextureScaledColor(null, x + 1.0, y, this.width - 2.0, 1.0, 1.0, 1.0, 1.0, 0.5);
                this.DrawTextureScaledColor(null, x + this.width - 1.0, y, 1.0, (double)this.height, 1.0, 1.0, 1.0, 0.5);
                this.DrawTextureScaledColor(null, x + 1.0, y + this.height - 1.0, this.width - 2.0, 1.0, 1.0, 1.0, 1.0, 0.5);
            }
        }
    }

    @Override
    public void update() {
        if (this.isVisible()) {
            super.update();
        }
    }

    public String getDamageStatusString() {
        BodyDamage bodyDamage = this.parentChar.getBodyDamage();
        if (GameClient.client && this.parentChar instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
            bodyDamage = this.parentChar.getBodyDamageRemote();
        }

        if (bodyDamage.getHealth() == 100.0F) {
            return Translator.getText("IGUI_health_ok");
        } else if (bodyDamage.getHealth() > 90.0F) {
            return Translator.getText("IGUI_health_Slight_damage");
        } else if (bodyDamage.getHealth() > 80.0F) {
            return Translator.getText("IGUI_health_Very_Minor_damage");
        } else if (bodyDamage.getHealth() > 70.0F) {
            return Translator.getText("IGUI_health_Minor_damage");
        } else if (bodyDamage.getHealth() > 60.0F) {
            return Translator.getText("IGUI_health_Moderate_damage");
        } else if (bodyDamage.getHealth() > 50.0F) {
            return Translator.getText("IGUI_health_Severe_damage");
        } else if (bodyDamage.getHealth() > 40.0F) {
            return Translator.getText("IGUI_health_Very_Severe_damage");
        } else if (bodyDamage.getHealth() > 20.0F) {
            return Translator.getText("IGUI_health_Crital_damage");
        } else if (bodyDamage.getHealth() > 10.0F) {
            return Translator.getText("IGUI_health_Highly_Crital_damage");
        } else {
            return bodyDamage.getHealth() > 0.0F ? Translator.getText("IGUI_health_Terminal_damage") : Translator.getText("IGUI_health_Deceased");
        }
    }
}
