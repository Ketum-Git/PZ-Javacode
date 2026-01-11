// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characterTextures;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.skinnedmodel.model.CharacterMask;

/**
 * Created by LEMMY on 7/1/2016.
 */
@UsedFromLua
public enum BloodBodyPartType {
    Hand_L("IGUI_health_Left_Hand", CharacterMask.Part.LeftHand),
    Hand_R("IGUI_health_Right_Hand", CharacterMask.Part.RightHand),
    ForeArm_L("IGUI_health_Left_Forearm", CharacterMask.Part.LeftArm),
    ForeArm_R("IGUI_health_Right_Forearm", CharacterMask.Part.RightArm),
    UpperArm_L("IGUI_health_Left_Upper_Arm", CharacterMask.Part.LeftArm),
    UpperArm_R("IGUI_health_Right_Upper_Arm", CharacterMask.Part.RightArm),
    Torso_Upper("IGUI_health_Upper_Torso", CharacterMask.Part.Chest),
    Torso_Lower("IGUI_health_Lower_Torso", CharacterMask.Part.Waist),
    Head("IGUI_health_Head", CharacterMask.Part.Head),
    Neck("IGUI_health_Neck", CharacterMask.Part.Head),
    Groin("IGUI_health_Groin", CharacterMask.Part.Crotch),
    UpperLeg_L("IGUI_health_Left_Thigh", CharacterMask.Part.LeftLeg, CharacterMask.Part.Pelvis),
    UpperLeg_R("IGUI_health_Right_Thigh", CharacterMask.Part.RightLeg, CharacterMask.Part.Pelvis),
    LowerLeg_L("IGUI_health_Left_Shin", CharacterMask.Part.LeftLeg),
    LowerLeg_R("IGUI_health_Right_Shin", CharacterMask.Part.RightLeg),
    Foot_L("IGUI_health_Left_Foot", CharacterMask.Part.LeftFoot),
    Foot_R("IGUI_health_Right_Foot", CharacterMask.Part.RightFoot),
    Back("IGUI_health_Back", CharacterMask.Part.Torso),
    MAX("IGUI_health_Unknown_Body_Part");

    private static final BloodBodyPartType[] VALUES = values();
    private static final Map<String, BloodBodyPartType> BY_NAME = new HashMap<>();
    private final String translationKey;
    private final CharacterMask.Part[] characterMaskParts;

    private BloodBodyPartType(final String translationKey, final CharacterMask.Part... parts) {
        this.translationKey = translationKey;
        this.characterMaskParts = parts;
    }

    public int index() {
        return this.ordinal();
    }

    public static BloodBodyPartType FromIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : MAX;
    }

    public static int ToIndex(BloodBodyPartType BPT) {
        return BPT == null ? 0 : BPT.index();
    }

    public static BloodBodyPartType FromString(String str) {
        return BY_NAME.getOrDefault(str, MAX);
    }

    public CharacterMask.Part[] getCharacterMaskParts() {
        return this.characterMaskParts;
    }

    public String getDisplayName() {
        return Translator.getText(this.translationKey);
    }

    static {
        for (BloodBodyPartType type : VALUES) {
            BY_NAME.put(type.name(), type);
        }
    }
}
