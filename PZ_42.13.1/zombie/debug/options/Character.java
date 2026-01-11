// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Character extends OptionGroup {
    public final BooleanDebugOption createAllOutfits = this.newOption("Create.AllOutfits", false);
    public final Character.DebugOG debug = this.newOptionGroup(new Character.DebugOG());

    public static final class DebugOG extends OptionGroup {
        public final Character.DebugOG.RenderOG render = this.newOptionGroup(new Character.DebugOG.RenderOG());
        public final Character.DebugOG.AnimateOG animate = this.newOptionGroup(new Character.DebugOG.AnimateOG());
        public final Character.DebugOG.RagdollOG ragdoll = new Character.DebugOG.RagdollOG(this);
        public final BooleanDebugOption registerDebugVariables = this.newDebugOnlyOption("DebugVariables", false);
        public final BooleanDebugOption alwaysTripOverFence = this.newDebugOnlyOption("AlwaysTripOverFence", false);
        public final BooleanDebugOption playSoundWhenInvisible = this.newDebugOnlyOption("PlaySoundWhenInvisible", false);
        public final BooleanDebugOption updateAlpha = this.newDebugOnlyOption("UpdateAlpha", true);
        public final BooleanDebugOption updateAlphaEighthSpeed = this.newDebugOnlyOption("UpdateAlphaEighthSpeed", false);
        public final BooleanDebugOption alwaysHitTarget = this.newDebugOnlyOption("AlwaysHitTarget", false);
        public final BooleanDebugOption useNewClimbingCalculations = this.newDebugOnlyOption("UseNewClimbingCalculations", false);

        public static final class AnimateOG extends OptionGroup {
            public final BooleanDebugOption deferredRotationsOnly = this.newDebugOnlyOption("DeferredRotationsOnly", false);
            public final BooleanDebugOption noBoneMasks = this.newDebugOnlyOption("NoBoneMasks", false);
            public final BooleanDebugOption noBoneTwists = this.newDebugOnlyOption("NoBoneTwists", false);
            public final BooleanDebugOption alwaysAimTwist = this.newDebugOnlyOption("AlwaysAimTwist", false);
            public final BooleanDebugOption zeroCounterRotationBone = this.newDebugOnlyOption("ZeroCounterRotation", false);
            public final BooleanDebugOption keepAtOrigin = this.newDebugOnlyOption("KeepAtOrigin", true);
        }

        public static final class RagdollOG extends OptionGroup {
            public final Character.DebugOG.RagdollOG.RenderOG render = new Character.DebugOG.RagdollOG.RenderOG(this);
            public final Character.DebugOG.RagdollOG.PhysicsOG physics = this.newOptionGroup(new Character.DebugOG.RagdollOG.PhysicsOG());
            public final BooleanDebugOption debug = this.newDebugOnlyOption("Debug", false);
            public final BooleanDebugOption simulationActive = this.newDebugOnlyOption("SimulationActive", true);
            public final BooleanDebugOption enableInitialVelocities = this.newDebugOnlyOption("EnableInitialVelocities", true);

            public RagdollOG(IDebugOptionGroup parentGroup) {
                super(parentGroup, "Ragdoll");
            }

            public static final class PhysicsOG extends OptionGroup {
                public final BooleanDebugOption physicsHitReaction = this.newDebugOnlyOption("PhysicsHitReaction", true);
                public final BooleanDebugOption allowJointConstraintDetach = this.newDebugOnlyOption("AllowJointConstraintDetach", false);
            }

            public static final class RenderOG extends OptionGroup {
                public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", false);
                public final BooleanDebugOption body = this.newDebugOnlyOption("Body", false);
                public final BooleanDebugOption pelvisLocation = this.newDebugOnlyOption("PelvisLocation", false);
                public final BooleanDebugOption bodySinglePart = this.newDebugOnlyOption("BodySinglePart", false);
                public final BooleanDebugOption skeleton = this.newDebugOnlyOption("Skeleton", false);
                public final BooleanDebugOption skeletonSinglePart = this.newDebugOnlyOption("SkeletonSinglePart", false);

                public RenderOG(IDebugOptionGroup parentGroup) {
                    super(parentGroup, "Render");
                }
            }
        }

        public static final class RenderOG extends OptionGroup {
            public final BooleanDebugOption aimCone = this.newDebugOnlyOption("AimCone", false);
            public final BooleanDebugOption angle = this.newDebugOnlyOption("Angle", false);
            public final BooleanDebugOption testDotSide = this.newDebugOnlyOption("TestDotSide", false);
            public final BooleanDebugOption deferredMovement = this.newDebugOnlyOption("DeferredMovement", false);
            public final BooleanDebugOption deferredAngles = this.newDebugOnlyOption("DeferredRotation", false);
            public final BooleanDebugOption translationData = this.newDebugOnlyOption("Translation_Data", false);
            public final BooleanDebugOption bip01 = this.newDebugOnlyOption("Bip01", false);
            public final BooleanDebugOption primaryHandBone = this.newDebugOnlyOption("HandBones.Primary", false);
            public final BooleanDebugOption secondaryHandBone = this.newDebugOnlyOption("HandBones.Secondary", false);
            public final BooleanDebugOption skipCharacters = this.newDebugOnlyOption("SkipCharacters", false);
            public final BooleanDebugOption vision = this.newDebugOnlyOption("Vision", false);
            public final BooleanDebugOption displayRoomAndZombiesZone = this.newDebugOnlyOption("DisplayRoomAndZombiesZone", false);
            public final BooleanDebugOption fmodRoomType = this.newDebugOnlyOption("FMODRoomType", false);
            public final BooleanDebugOption carStopDebug = this.newDebugOnlyOption("CarStopDebug", false);
            public final BooleanDebugOption meleeOutline = this.newDebugOnlyOption("MeleeOutline", false);
            public final BooleanDebugOption aimVector = this.newDebugOnlyOption("AimVector", false);
            public final BooleanDebugOption climbRope = this.newDebugOnlyOption("ClimbRope", false);
            public final BooleanDebugOption explosionHitDirection = this.newDebugOnlyOption("ExplosionHitDirection", false);
        }
    }
}
