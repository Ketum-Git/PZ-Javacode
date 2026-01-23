// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImFloat;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.physics.RagdollBodyPart;
import zombie.inventory.types.HandWeapon;
import zombie.scripting.objects.PhysicsHitReaction;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.util.StringUtils;

public class PhysicsHitReactionsPanel extends PZDebugWindow {
    private HandWeapon previousHandWeapon;
    private final ImFloat maxForwardImpulse = new ImFloat(200.0F);
    private final ImFloat maxUpwardImpulse = new ImFloat(200.0F);

    @Override
    public String getTitle() {
        return "Physics Hit Reactions";
    }

    @Override
    protected void doWindowContents() {
        boolean weaponChanged = false;
        IsoPlayer player = IsoPlayer.players[0];
        if (player != null) {
            HandWeapon currentHandWeapon = player.getUseHandWeapon();
            if (this.previousHandWeapon != currentHandWeapon) {
                this.previousHandWeapon = currentHandWeapon;
                if (this.previousHandWeapon != null) {
                    weaponChanged = true;
                }
            }
        }

        ImGui.beginChild("Begin");
        if (PZImGui.button("Write All To File")) {
            PhysicsHitReactionScript.writeToFile();
        }

        if (ImGui.inputFloat("Max Forward Impulse", this.maxForwardImpulse, 0.1F, 1.0F, "%0.2f") && this.maxForwardImpulse.floatValue() <= 0.0F) {
            this.maxForwardImpulse.set(1.0F);
        }

        if (ImGui.inputFloat("Max Upward Impulse", this.maxUpwardImpulse, 0.1F, 1.0F, "%0.2f") && this.maxUpwardImpulse.floatValue() <= 0.0F) {
            this.maxUpwardImpulse.set(1.0F);
        }

        if (ImGui.beginTabBar("tabSelector")) {
            for (PhysicsHitReaction physicsHitReaction : PhysicsHitReactionScript.physicsHitReactionList) {
                boolean tabSelected = false;
                if (this.previousHandWeapon != null) {
                    this.previousHandWeapon.getAmmoType();
                } else {
                    Object var10000 = null;
                }

                if (weaponChanged && physicsHitReaction.ammoType == this.previousHandWeapon.getAmmoType()) {
                    tabSelected = true;
                }

                String translationName = physicsHitReaction.ammoType != null
                    ? physicsHitReaction.ammoType.getTranslationName()
                    : StringUtils.stripModule(physicsHitReaction.physicsObject);
                if (ImGui.beginTabItem(translationName, tabSelected ? 2 : 0)) {
                    this.physicsHitReactionTab(physicsHitReaction);
                    ImGui.endTabItem();
                }
            }

            ImGui.endTabBar();
        }

        ImGui.endChild();
    }

    private void physicsHitReactionTab(PhysicsHitReaction physicsHitReaction) {
        String translationName = physicsHitReaction.ammoType != null
            ? physicsHitReaction.ammoType.getTranslationName()
            : StringUtils.stripModule(physicsHitReaction.physicsObject);
        ImGui.beginChild(translationName);
        physicsHitReaction.useImpulseOverride = PZImGui.checkbox("Use Override", physicsHitReaction.useImpulseOverride);
        if (physicsHitReaction.useImpulseOverride) {
            physicsHitReaction.overrideForwardImpulse = PZImGui.sliderFloat(
                "Forward Impulse Override", physicsHitReaction.overrideForwardImpulse, 0.0F, this.maxForwardImpulse.floatValue()
            );
            physicsHitReaction.overrideUpwardImpulse = PZImGui.sliderFloat(
                "Upward Impulse Override", physicsHitReaction.overrideUpwardImpulse, 0.0F, this.maxUpwardImpulse.floatValue()
            );
        }

        for (int i = 0; i < physicsHitReaction.impulse.length; i++) {
            if (!physicsHitReaction.useImpulseOverride) {
                physicsHitReaction.impulse[i] = PZImGui.sliderFloat(
                    Translator.getText(RagdollBodyPart.values()[i].name()) + " Forward Impulse",
                    physicsHitReaction.impulse[i],
                    0.0F,
                    this.maxForwardImpulse.floatValue()
                );
                physicsHitReaction.upwardImpulse[i] = PZImGui.sliderFloat(
                    Translator.getText(RagdollBodyPart.values()[i].name()) + " Upward Impulse",
                    physicsHitReaction.upwardImpulse[i],
                    0.0F,
                    this.maxUpwardImpulse.floatValue()
                );
            } else {
                physicsHitReaction.impulse[i] = physicsHitReaction.overrideUpwardImpulse;
                physicsHitReaction.upwardImpulse[i] = physicsHitReaction.overrideForwardImpulse;
            }
        }

        ImGui.endChild();
    }
}
