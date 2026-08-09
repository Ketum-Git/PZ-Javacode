// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import zombie.UsedFromLua;
import zombie.characters.CharacterGender;
import zombie.core.Translator;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.debug.DebugType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum BodyPartType {
    Hand_L(
        "Hand_L",
        "IGUI_health_Left_Hand",
        "Base.Bandage_LeftHand",
        "Base.Wound_LHand_Bite_",
        "Base.Wound_LHand_Scratch_",
        "Base.Wound_LHand_Laceration_",
        0.5F,
        0.1F,
        0.2F,
        0.01F,
        0.8F,
        0.35F,
        1.0F,
        0.05F
    ),
    Hand_R(
        "Hand_R",
        "IGUI_health_Right_Hand",
        "Base.Bandage_RightHand",
        "Base.Wound_RHand_Bite_",
        "Base.Wound_RHand_Scratch_",
        "Base.Wound_RHand_Laceration_",
        0.5F,
        0.1F,
        0.2F,
        0.01F,
        0.8F,
        0.35F,
        1.0F,
        0.05F
    ),
    ForeArm_L(
        "ForeArm_L",
        "IGUI_health_Left_Forearm",
        "Base.Bandage_LeftLowerArm",
        "Base.Wound_LForearm_Bite_",
        "Base.Wound_LForearm_Scratch_",
        "Base.Wound_LForearm_Laceration_",
        0.6F,
        0.2F,
        0.3F,
        0.035F,
        0.6F,
        0.3F,
        0.6F,
        0.1F
    ),
    ForeArm_R(
        "ForeArm_R",
        "IGUI_health_Right_Forearm",
        "Base.Bandage_RightLowerArm",
        "Base.Wound_RForearm_Bite_",
        "Base.Wound_RForearm_Scratch_",
        "Base.Wound_RForearm_Laceration_",
        0.6F,
        0.2F,
        0.3F,
        0.035F,
        0.6F,
        0.3F,
        0.6F,
        0.1F
    ),
    UpperArm_L(
        "UpperArm_L",
        "IGUI_health_Left_Upper_Arm",
        "Base.Bandage_LeftUpperArm",
        "Base.Wound_LUArm_Bite_",
        "Base.Wound_LUArm_Scratch_",
        "Base.Wound_LUArm_Laceration_",
        0.6F,
        0.3F,
        0.4F,
        0.045F,
        0.3F,
        0.25F,
        0.4F,
        0.1F
    ),
    UpperArm_R(
        "UpperArm_R",
        "IGUI_health_Right_Upper_Arm",
        "Base.Bandage_RightUpperArm",
        "Base.Wound_RUArm_Bite_",
        "Base.Wound_RUArm_Scratch_",
        "Base.Wound_RUArm_Laceration_",
        0.6F,
        0.3F,
        0.4F,
        0.045F,
        0.3F,
        0.25F,
        0.4F,
        0.1F
    ),
    Torso_Upper(
        "Torso_Upper",
        "IGUI_health_Upper_Torso",
        "Base.Bandage_Chest",
        "Base.Wound_Chest_Bite_",
        "Base.Wound_Chest_Scratch_",
        "Base.Wound_Chest_Laceration_",
        0.7F,
        0.35F,
        0.5F,
        0.18F,
        0.0F,
        0.2F,
        0.2F,
        0.05F
    ),
    Torso_Lower(
        "Torso_Lower",
        "IGUI_health_Lower_Torso",
        "Base.Bandage_Abdomen",
        "Base.Wound_Abdomen_Bite_",
        "Base.Wound_Abdomen_Scratch_",
        "Base.Wound_Abdomen_Laceration_",
        0.78F,
        0.4F,
        0.9F,
        0.12F,
        0.05F,
        0.25F,
        0.1F,
        0.05F
    ),
    Head(
        "Head",
        "IGUI_health_Head",
        "Base.Bandage_Head",
        "Base.Wound_Neck_Bite_",
        "Base.Wound_Neck_Scratch_",
        "Base.Wound_Neck_Laceration_",
        0.8F,
        0.6F,
        1.0F,
        0.08F,
        0.05F,
        0.05F,
        0.4F,
        0.25F
    ),
    Neck(
        "Neck",
        "IGUI_health_Neck",
        "Base.Bandage_Neck",
        "Base.Wound_Neck_Bite_",
        "Base.Wound_Neck_Scratch_",
        "Base.Wound_Neck_Laceration_",
        0.8F,
        0.7F,
        1.5F,
        0.02F,
        0.02F,
        0.1F,
        0.05F,
        0.05F
    ),
    Groin(
        "Groin",
        "IGUI_health_Groin",
        "Base.Bandage_Groin",
        "Base.Wound_Groin_Bite_",
        "Base.Wound_Groin_Scratch_",
        "Base.Wound_Groin_Laceration_",
        0.7F,
        0.4F,
        0.5F,
        0.06F,
        0.3F,
        0.3F,
        0.05F,
        0.15F
    ),
    UpperLeg_L("UpperLeg_L", "IGUI_health_Left_Thigh", "Base.Bandage_LeftUpperLeg", null, null, null, 0.7F, 0.3F, 0.4F, 0.09F, 0.45F, 0.55F, 0.1F, 0.4F),
    UpperLeg_R("UpperLeg_R", "IGUI_health_Right_Thigh", "Base.Bandage_RightUpperLeg", null, null, null, 0.7F, 0.3F, 0.4F, 0.09F, 0.45F, 0.55F, 0.1F, 0.4F),
    LowerLeg_L("LowerLeg_L", "IGUI_health_Left_Shin", "Base.Bandage_LeftLowerLeg", null, null, null, 0.6F, 0.2F, 0.3F, 0.07F, 0.75F, 0.75F, 0.1F, 0.6F),
    LowerLeg_R("LowerLeg_R", "IGUI_health_Right_Shin", "Base.Bandage_RightLowerLeg", null, null, null, 0.6F, 0.2F, 0.3F, 0.07F, 0.75F, 0.75F, 0.1F, 0.6F),
    Foot_L("Foot_L", "IGUI_health_Left_Foot", "Base.Bandage_LeftFoot", null, null, null, 0.5F, 0.2F, 0.2F, 0.02F, 1.0F, 1.0F, 0.1F, 1.0F),
    Foot_R("Foot_R", "IGUI_health_Right_Foot", "Base.Bandage_RightFoot", null, null, null, 0.5F, 0.2F, 0.2F, 0.02F, 1.0F, 1.0F, 0.1F, 1.0F),
    MAX("Unknown Body Part", "IGUI_health_Unknown_Body_Part", null, null, null, null, 1.0F, 1.0F, 1.0F, 0.001F, 0.0F, 1.0F, 0.0F, 0.0F);

    private static final BodyPartType[] values = values();
    private final String name;
    private final float painModifier;
    private final String displayNameKey;
    private final float damageModifier;
    private final float bleedingTimeModifier;
    private final float skinSurface;
    private final float distToCore;
    private final float umbrellaMod;
    private final float maxActionPenalty;
    private final float maxMovementPenalty;
    private final String bandageModel;
    private final String biteWoundModel;
    private final String scratchWoundModel;
    private final String cutWoundModel;

    private BodyPartType(
        final String name,
        final String displayNameKey,
        final String bandageModel,
        final String biteWoundModel,
        final String scratchWoundModel,
        final String cutWoundModel,
        final float painModifier,
        final float damageModifier,
        final float bleedingTimeModifier,
        final float skinSurface,
        final float distToCore,
        final float umbrellaMod,
        final float maxActionPenalty,
        final float maxMovementPenalty
    ) {
        this.name = name;
        this.painModifier = painModifier;
        this.displayNameKey = displayNameKey;
        this.damageModifier = damageModifier;
        this.bleedingTimeModifier = bleedingTimeModifier;
        this.skinSurface = skinSurface;
        this.distToCore = distToCore;
        this.umbrellaMod = umbrellaMod;
        this.maxActionPenalty = maxActionPenalty;
        this.maxMovementPenalty = maxMovementPenalty;
        this.bandageModel = bandageModel;
        this.biteWoundModel = biteWoundModel;
        this.scratchWoundModel = scratchWoundModel;
        this.cutWoundModel = cutWoundModel;
    }

    public static BodyPartType FromIndex(int index) {
        return index >= 0 && index < values.length ? values[index] : MAX;
    }

    public int index() {
        return this.ordinal();
    }

    public static BodyPartType FromString(String str) {
        return PZArrayUtil.findOrDefault(values, str, (bodyPart, _str) -> StringUtils.equalsIgnoreCase(bodyPart.name, _str), MAX);
    }

    public static float getPainModifyer(int index) {
        return FromIndex(index).painModifier;
    }

    public static String getDisplayName(BodyPartType bpt) {
        return Translator.getText(bpt != null ? bpt.displayNameKey : MAX.displayNameKey);
    }

    public static int ToIndex(BodyPartType bpt) {
        return bpt != null ? bpt.ordinal() : 0;
    }

    public static String ToString(BodyPartType bpt) {
        return bpt != null ? bpt.name : MAX.name;
    }

    public static float getDamageModifyer(int index) {
        return FromIndex(index).damageModifier;
    }

    public static float getBleedingTimeModifyer(int index) {
        return FromIndex(index).bleedingTimeModifier;
    }

    public static float GetSkinSurface(BodyPartType bodyPartType) {
        if (bodyPartType != null && bodyPartType != MAX) {
            return bodyPartType.skinSurface;
        } else {
            DebugType.General.warn("Warning: couldn't get skinSurface for body part '%s'.", bodyPartType);
            return MAX.skinSurface;
        }
    }

    public static float GetDistToCore(BodyPartType bodyPartType) {
        if (bodyPartType != null && bodyPartType != MAX) {
            return bodyPartType.distToCore;
        } else {
            DebugType.General.warn("Warning: couldn't get distToCore for body part '%s'.", bodyPartType);
            return MAX.distToCore;
        }
    }

    public static float GetUmbrellaMod(BodyPartType bodyPartType) {
        if (bodyPartType != null && bodyPartType != MAX) {
            return bodyPartType.umbrellaMod;
        } else {
            DebugType.General.warn("Warning: couldn't get umbrellaMod for body part '%s'.", bodyPartType);
            return MAX.umbrellaMod;
        }
    }

    public static float GetMaxActionPenalty(BodyPartType bodyPartType) {
        if (bodyPartType != null && bodyPartType != MAX) {
            return bodyPartType.maxActionPenalty;
        } else {
            DebugType.General.warn("Warning: couldn't get maxActionPenalty for body part '%s'.", bodyPartType);
            return MAX.maxActionPenalty;
        }
    }

    public static float GetMaxMovementPenalty(BodyPartType bodyPartType) {
        if (bodyPartType != null && bodyPartType != MAX) {
            return bodyPartType.maxMovementPenalty;
        } else {
            DebugType.General.warn("Warning: couldn't get maxMovementPenalty for body part '%s'.", bodyPartType);
            return MAX.maxMovementPenalty;
        }
    }

    public String getBandageModel() {
        return this.bandageModel;
    }

    public String getBiteWoundModel(CharacterGender gender) {
        return this.biteWoundModel != null ? this.biteWoundModel + gender : null;
    }

    public String getScratchWoundModel(CharacterGender gender) {
        return this.scratchWoundModel != null ? this.scratchWoundModel + gender : null;
    }

    public String getCutWoundModel(CharacterGender gender) {
        return this.cutWoundModel != null ? this.cutWoundModel + gender : null;
    }

    public static BodyPartType getRandom() {
        return FromIndex(OutfitRNG.Next(0, MAX.index()));
    }
}
