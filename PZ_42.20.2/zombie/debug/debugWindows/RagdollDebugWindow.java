// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import zombie.characters.RagdollBuilder;
import zombie.core.Translator;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.physics.RagdollController;
import zombie.core.physics.RagdollJoint;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.debug.DebugType;
import zombie.scripting.objects.RagdollAnchor;
import zombie.scripting.objects.RagdollBodyDynamics;
import zombie.scripting.objects.RagdollBodyPartInfo;
import zombie.scripting.objects.RagdollConstraint;
import zombie.scripting.objects.RagdollScript;

public class RagdollDebugWindow extends PZDebugWindow {
    private static final float ConstraintPositionOffsetMax = 0.25F;
    private static final int HingeConstraintType = 4;
    private static final int ConeTwistConstraintType = 5;
    private final String[] physicsShapeTypeArray = new String[]{"Capsule", "Box", "Sphere"};
    private final String[] constraintTypeArray = new String[]{"Hinge", "Cone Twist"};
    private final String[] bodyPartArray = new String[]{
        "BODYPART_PELVIS",
        "BODYPART_SPINE",
        "BODYPART_HEAD",
        "BODYPART_LEFT_UPPER_LEG",
        "BODYPART_LEFT_LOWER_LEG",
        "BODYPART_RIGHT_UPPER_LEG",
        "BODYPART_RIGHT_LOWER_LEG",
        "BODYPART_LEFT_UPPER_ARM",
        "BODYPART_LEFT_LOWER_ARM",
        "BODYPART_RIGHT_UPPER_ARM",
        "BODYPART_RIGHT_LOWER_ARM",
        "BODYPART_COUNT"
    };

    @Override
    public String getTitle() {
        return "Ragdoll Editor";
    }

    @Override
    protected void doWindowContents() {
        if (ImGui.beginTabBar("tabSelector")) {
            if (ImGui.beginTabItem("Physics Properties")) {
                this.physicsPropertiesTab();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Body Dynamics")) {
                this.bodyDynamicsTab();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Hit Reactions")) {
                this.hitReactionsTab();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Joint Constraints")) {
                this.jointConstraintsTab();
                ImGui.endTabItem();
            }

            if (RagdollBuilder.instance.isSkeletonBoneHierarchyInitialized() && ImGui.beginTabItem("Skeleton")) {
                this.skeletonTab();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Bone To Body Part Anchors")) {
                this.boneToBodyPartAnchorsTab();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Body Part Info")) {
                this.bodyPartInfoTab();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Vehicle Friction")) {
                this.vehicleFrictionTab();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
    }

    private void jointConstraintsTab() {
        if (PZImGui.button("Write All To File")) {
            RagdollScript.writeConstraintsToFile();
        } else {
            for (RagdollConstraint ragdollConstraint : RagdollScript.getRagdollConstraintList()) {
                if (PZImGui.collapsingHeader(RagdollJoint.values()[ragdollConstraint.joint].toString())) {
                    ImGui.beginChild(RagdollJoint.values()[ragdollConstraint.joint].toString());
                    int constraintType = ragdollConstraint.constraintType == 4 ? 0 : 1;
                    ImInt constraintTypeImInt = new ImInt(constraintType);
                    if (PZImGui.combo("Constraint Type", constraintTypeImInt, this.constraintTypeArray)) {
                        ragdollConstraint.constraintType = constraintTypeImInt.get() == 0 ? 4 : 5;
                    }

                    int constraintPartA = ragdollConstraint.constraintPartA;
                    ImInt constraintPartAImInt = new ImInt(constraintPartA);
                    if (PZImGui.combo("Constraint Part A", constraintPartAImInt, this.bodyPartArray)) {
                        ragdollConstraint.constraintPartA = constraintPartAImInt.get();
                    }

                    int constraintPartB = ragdollConstraint.constraintPartB;
                    ImInt constraintPartBImInt = new ImInt(constraintPartB);
                    if (PZImGui.combo("Constraint Part B", constraintPartBImInt, this.bodyPartArray)) {
                        ragdollConstraint.constraintPartB = constraintPartAImInt.get();
                    }

                    ragdollConstraint.constraintAxisA.x = PZImGui.sliderFloat(
                        "constraintAxisA.x", ragdollConstraint.constraintAxisA.x, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintAxisA.y = PZImGui.sliderFloat(
                        "constraintAxisA.y", ragdollConstraint.constraintAxisA.y, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintAxisA.z = PZImGui.sliderFloat(
                        "constraintAxisA.z", ragdollConstraint.constraintAxisA.z, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintAxisB.x = PZImGui.sliderFloat(
                        "constraintAxisB.x", ragdollConstraint.constraintAxisB.x, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintAxisB.y = PZImGui.sliderFloat(
                        "constraintAxisB.y", ragdollConstraint.constraintAxisB.y, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintAxisB.z = PZImGui.sliderFloat(
                        "constraintAxisB.z", ragdollConstraint.constraintAxisB.z, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintPositionOffsetA.x = PZImGui.sliderFloat(
                        "constraintPositionOffsetA.x", ragdollConstraint.constraintPositionOffsetA.x, -0.25F, 0.25F
                    );
                    ragdollConstraint.constraintPositionOffsetA.y = PZImGui.sliderFloat(
                        "constraintPositionOffsetA.y", ragdollConstraint.constraintPositionOffsetA.y, -0.25F, 0.25F
                    );
                    ragdollConstraint.constraintPositionOffsetA.z = PZImGui.sliderFloat(
                        "constraintPositionOffsetA.z", ragdollConstraint.constraintPositionOffsetA.z, -0.25F, 0.25F
                    );
                    ragdollConstraint.constraintPositionOffsetB.x = PZImGui.sliderFloat(
                        "constraintPositionOffsetB.x", ragdollConstraint.constraintPositionOffsetB.x, -0.25F, 0.25F
                    );
                    ragdollConstraint.constraintPositionOffsetB.y = PZImGui.sliderFloat(
                        "constraintPositionOffsetB.y", ragdollConstraint.constraintPositionOffsetB.y, -0.25F, 0.25F
                    );
                    ragdollConstraint.constraintPositionOffsetB.z = PZImGui.sliderFloat(
                        "constraintPositionOffsetB.z", ragdollConstraint.constraintPositionOffsetB.z, -0.25F, 0.25F
                    );
                    ragdollConstraint.constraintLimit.x = PZImGui.sliderFloat(
                        "constraintLimit.x", ragdollConstraint.constraintLimit.x, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintLimit.y = PZImGui.sliderFloat(
                        "constraintLimit.y", ragdollConstraint.constraintLimit.y, (float) -Math.PI, (float) Math.PI
                    );
                    ragdollConstraint.constraintLimit.z = PZImGui.sliderFloat(
                        "constraintLimit.z", ragdollConstraint.constraintLimit.z, (float) -Math.PI, (float) Math.PI
                    );
                    ImGui.endChild();
                }
            }

            if (RagdollBuilder.instance.isInitialized()) {
                if (PZImGui.button(Translator.getText("IGUI_PlayerStats_ResetToDefault"))) {
                    RagdollScript.resetConstraintsToDefaultValues();
                }

                if (PZImGui.button("Update toBullet()")) {
                    RagdollScript.uploadConstraints(true);
                }
            }
        }
    }

    private void skeletonTab() {
        if (ImGui.treeNode("Skeleton")) {
            SkeletonBone[] skeletonBones = SkeletonBone.all();
            SkinningBoneHierarchy boneHierarchy = RagdollBuilder.instance.getSkeletonBoneHierarchy();

            for (SkeletonBone skeletonBone : skeletonBones) {
                SkinningBone boneAt = boneHierarchy.getBone(skeletonBone);
                if (boneAt == null) {
                    DebugType.Ragdoll.debugln("Bone not found: %s", skeletonBone.toString());
                } else {
                    SkeletonBone parentBone = boneAt.getParentSkeletonBone();
                    if (ImGui.treeNode(parentBone.name())) {
                        StringBuilder skeletonBoneInfo = new StringBuilder();
                        skeletonBoneInfo.append("parent: ").append(boneAt.parent).append(", childBone: ").append(boneAt);
                        ImGui.text(skeletonBoneInfo.toString());
                        ImGui.treePop();
                    }
                }
            }

            ImGui.treePop();
        }
    }

    private void boneToBodyPartAnchorsTab() {
        if (PZImGui.button("Write All To File")) {
            RagdollScript.writeAnchorsToFile();
        } else {
            for (RagdollAnchor ragdollAnchor : RagdollScript.getRagdollAnchorList()) {
                if (PZImGui.collapsingHeader(SkeletonBone.values()[ragdollAnchor.bone].toString())) {
                    ImGui.beginChild(SkeletonBone.values()[ragdollAnchor.bone].toString());
                    boolean enabled = ragdollAnchor.enabled;
                    ImBoolean enabledTypeImInt = new ImBoolean(enabled);
                    ragdollAnchor.enabled = PZImGui.checkbox(Translator.getText("IGUI_DebugMenu_Enabled"), enabledTypeImInt::get, enabledTypeImInt::set);
                    if (enabled) {
                        int bodyPart = ragdollAnchor.bodyPart;
                        ImInt bodyPartTypeImInt = new ImInt(bodyPart);
                        if (PZImGui.combo("Body Part", bodyPartTypeImInt, this.bodyPartArray)) {
                            ragdollAnchor.bodyPart = bodyPartTypeImInt.get();
                        }

                        boolean reverse = ragdollAnchor.reverse;
                        ImBoolean reverseTypeImInt = new ImBoolean(reverse);
                        ragdollAnchor.reverse = PZImGui.checkbox("Reverse", reverseTypeImInt::get, reverseTypeImInt::set);
                        boolean original = ragdollAnchor.original;
                        ImBoolean originalTypeImInt = new ImBoolean(original);
                        ragdollAnchor.original = PZImGui.checkbox("Original", originalTypeImInt::get, originalTypeImInt::set);
                    }

                    ImGui.endChild();
                }
            }

            if (RagdollBuilder.instance.isInitialized()) {
                if (PZImGui.button(Translator.getText("IGUI_PlayerStats_ResetToDefault"))) {
                    RagdollScript.resetAnchorsToDefaultValues();
                }

                if (PZImGui.button("Update toBullet()")) {
                    RagdollScript.uploadAnchors(true);
                }
            }
        }
    }

    private void physicsPropertiesTab() {
        int count = RagdollSettingsManager.getInstance().getSettingsCount();
        ImGui.beginChild("Physics Properties");

        for (int i = 0; i < count; i++) {
            RagdollSettingsManager.RagdollSetting ragdollSetting = RagdollSettingsManager.getInstance().getSetting(i);
            float ragdollSettingValue = PZImGui.sliderFloat(
                ragdollSetting.getName(), ragdollSetting.getAdminValue(), ragdollSetting.getMin(), ragdollSetting.getMax()
            );
            if (RagdollBuilder.instance.isInitialized()) {
                ragdollSetting.setAdminValue(ragdollSettingValue);
            }
        }

        if (RagdollBuilder.instance.isInitialized() && PZImGui.button(Translator.getText("IGUI_PlayerStats_ResetToDefault"))) {
            for (int ix = 0; ix < count; ix++) {
                RagdollSettingsManager.RagdollSetting ragdollSetting = RagdollSettingsManager.getInstance().getSetting(ix);
                ragdollSetting.reset();
            }
        }

        ImGui.endChild();
    }

    private void hitReactionsTab() {
        int hitReactionSettingCount = RagdollSettingsManager.getInstance().getHitReactionSettingsCount() / 3;
        ImGui.beginChild("Hit Reactions");

        for (int i = 0; i < hitReactionSettingCount; i++) {
            RagdollSettingsManager.HitReactionSetting hitReactionSetting = RagdollSettingsManager.getInstance().getHitReactionSetting(i);
            if (PZImGui.collapsingHeader(hitReactionSetting.getName())) {
                ImGui.beginChild(hitReactionSetting.getName());
                if (i != 0) {
                    RagdollSettingsManager.ForceHitReactionLocation forceHitReactionLocation = RagdollSettingsManager.getInstance()
                        .getForceHitReactionLocation(i - 1);
                    boolean forceHitReactionLocationValue = PZImGui.checkbox(
                        "Force " + forceHitReactionLocation.getName() + " Hit Reaction", forceHitReactionLocation.getAdminValue()
                    );
                    if (RagdollBuilder.instance.isInitialized()) {
                        forceHitReactionLocation.setAdminValue(forceHitReactionLocationValue);
                    }
                }

                for (int j = 0; j < 3; j++) {
                    hitReactionSetting = RagdollSettingsManager.getInstance().getHitReactionSetting(i + j * 12);
                    float ragdollSettingValue = PZImGui.sliderFloat(
                        hitReactionSetting.getName(), hitReactionSetting.getAdminValue(), hitReactionSetting.getMin(), hitReactionSetting.getMax()
                    );
                    if (RagdollBuilder.instance.isInitialized()) {
                        hitReactionSetting.setAdminValue(ragdollSettingValue);
                    }
                }

                ImGui.endChild();
            }
        }

        if (RagdollBuilder.instance.isInitialized() && PZImGui.button(Translator.getText("IGUI_PlayerStats_ResetToDefault"))) {
            for (int ix = 0; ix < hitReactionSettingCount; ix++) {
                RagdollSettingsManager.HitReactionSetting hitReactionSetting = RagdollSettingsManager.getInstance().getHitReactionSetting(ix);
                hitReactionSetting.reset();
            }
        }

        ImGui.endChild();
    }

    private void bodyPartInfoTab() {
        if (PZImGui.button("Write All To File")) {
            RagdollScript.writeBodyPartInfoToFile();
        } else {
            for (RagdollBodyPartInfo ragdollBodyPartInfo : RagdollScript.getRagdollBodyPartInfoList()) {
                if (PZImGui.collapsingHeader(RagdollBodyPart.values()[ragdollBodyPartInfo.part].toString())) {
                    ImGui.beginChild(RagdollBodyPart.values()[ragdollBodyPartInfo.part].toString());
                    boolean calculateLength = ragdollBodyPartInfo.calculateLength;
                    ImBoolean calculateLengthTypeImInt = new ImBoolean(calculateLength);
                    ragdollBodyPartInfo.calculateLength = PZImGui.checkbox(
                        Translator.getText("Calculate Length"), calculateLengthTypeImInt::get, calculateLengthTypeImInt::set
                    );
                    ragdollBodyPartInfo.radius = PZImGui.sliderFloat("radius", ragdollBodyPartInfo.radius, 0.0F, 0.15F);
                    ragdollBodyPartInfo.height = PZImGui.sliderFloat("height", ragdollBodyPartInfo.height, 0.0F, 0.5F);
                    ragdollBodyPartInfo.gap = PZImGui.sliderFloat("gap", ragdollBodyPartInfo.gap, 0.0F, 0.1F);
                    int shapeType = ragdollBodyPartInfo.shape;
                    ImInt shapeTypeImInt = new ImInt(shapeType);
                    if (PZImGui.combo("shape", shapeTypeImInt, this.physicsShapeTypeArray)) {
                        ragdollBodyPartInfo.shape = shapeTypeImInt.get();
                    }

                    ragdollBodyPartInfo.mass = PZImGui.sliderFloat("mass", ragdollBodyPartInfo.mass, 0.0F, 1.0F);
                    ragdollBodyPartInfo.offset.x = PZImGui.sliderFloat("offset.x", ragdollBodyPartInfo.offset.x, -0.15F, 0.15F);
                    ragdollBodyPartInfo.offset.y = PZImGui.sliderFloat("offset.y", ragdollBodyPartInfo.offset.y, -0.15F, 0.15F);
                    ragdollBodyPartInfo.offset.z = PZImGui.sliderFloat("offset.z", ragdollBodyPartInfo.offset.z, -0.15F, 0.15F);
                    ImGui.endChild();
                }
            }

            if (RagdollBuilder.instance.isInitialized()) {
                if (PZImGui.button(Translator.getText("IGUI_PlayerStats_ResetToDefault"))) {
                    RagdollScript.resetBodyPartInfoToDefaultValues();
                }

                if (PZImGui.button("Update toBullet()")) {
                    RagdollScript.uploadBodyPartInfo(true);
                }
            }
        }
    }

    private void bodyDynamicsTab() {
        if (PZImGui.button("Write All To File")) {
            RagdollScript.writeBodyDynamicsToFile();
        } else {
            for (RagdollBodyDynamics ragdollBodyDynamics : RagdollScript.getRagdollBodyDynamicsList()) {
                if (PZImGui.collapsingHeader(RagdollBodyPart.values()[ragdollBodyDynamics.part].toString())) {
                    ImGui.beginChild(RagdollBodyPart.values()[ragdollBodyDynamics.part].toString());
                    ragdollBodyDynamics.linearDamping = PZImGui.sliderFloat("Linear Damping", ragdollBodyDynamics.linearDamping, 0.0F, 1.0F);
                    ragdollBodyDynamics.angularDamping = PZImGui.sliderFloat("Angular Damping", ragdollBodyDynamics.angularDamping, 0.0F, 1.0F);
                    ragdollBodyDynamics.deactivationTime = PZImGui.sliderFloat("Deactivation Time", ragdollBodyDynamics.deactivationTime, 0.0F, 2.0F);
                    ragdollBodyDynamics.linearSleepingThreshold = PZImGui.sliderFloat(
                        "Linear Sleep Threshold", ragdollBodyDynamics.linearSleepingThreshold, 0.0F, 2.0F
                    );
                    ragdollBodyDynamics.angularSleepingThreshold = PZImGui.sliderFloat(
                        "Angular Sleep Threshold", ragdollBodyDynamics.angularSleepingThreshold, 0.0F, 3.0F
                    );
                    ragdollBodyDynamics.friction = PZImGui.sliderFloat("Friction", ragdollBodyDynamics.friction, 0.0F, 2.0F);
                    ragdollBodyDynamics.rollingFriction = PZImGui.sliderFloat("Rolling Friction", ragdollBodyDynamics.rollingFriction, 0.0F, 2.0F);
                    ImGui.endChild();
                }
            }

            if (RagdollBuilder.instance.isInitialized()) {
                if (PZImGui.button(Translator.getText("IGUI_PlayerStats_ResetToDefault"))) {
                    RagdollScript.resetBodyDynamicsToDefaultValues();
                }

                if (PZImGui.button("Update toBullet()")) {
                    RagdollScript.uploadBodyDynamics(true);
                }
            }
        }
    }

    private void vehicleFrictionTab() {
        RagdollController.vehicleCollisionFriction = PZImGui.sliderFloat("VehicleCollisionFriction", RagdollController.vehicleCollisionFriction, 0.0F, 1.5F);
        if (RagdollBuilder.instance.isInitialized() && PZImGui.button("Update toBullet()")) {
            RagdollController.setVehicleRagdollBodyDynamics(RagdollScript.getRagdollBodyDynamicsList().get(0));
        }
    }
}
