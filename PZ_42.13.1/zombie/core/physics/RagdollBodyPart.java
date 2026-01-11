// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import org.joml.Random;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.random.Rand;

public enum RagdollBodyPart {
    BODYPART_PELVIS,
    BODYPART_SPINE,
    BODYPART_HEAD,
    BODYPART_LEFT_UPPER_LEG,
    BODYPART_LEFT_LOWER_LEG,
    BODYPART_RIGHT_UPPER_LEG,
    BODYPART_RIGHT_LOWER_LEG,
    BODYPART_LEFT_UPPER_ARM,
    BODYPART_LEFT_LOWER_ARM,
    BODYPART_RIGHT_UPPER_ARM,
    BODYPART_RIGHT_LOWER_ARM,
    BODYPART_COUNT;

    private static final RagdollBodyPart[] VALUES = values();
    private static final int RANDOM_BOUND = VALUES.length - 1;
    private static final Random RANDOM = new Random();

    public static RagdollBodyPart getRandomPart() {
        return VALUES[RANDOM.nextInt(RANDOM_BOUND)];
    }

    public static boolean isHead(int value) {
        return value == BODYPART_HEAD.ordinal();
    }

    public static boolean isLeg(int value) {
        return value >= BODYPART_LEFT_UPPER_LEG.ordinal() && value <= BODYPART_RIGHT_LOWER_LEG.ordinal();
    }

    public static boolean isArm(int value) {
        return value >= BODYPART_LEFT_UPPER_ARM.ordinal() && value <= BODYPART_RIGHT_LOWER_ARM.ordinal();
    }

    public static int getBodyPartType(int value) {
        boolean secondaryBodyPart = Rand.NextBool(2);

        return switch (VALUES[value]) {
            case BODYPART_PELVIS -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.Torso_Lower) : BodyPartType.ToIndex(BodyPartType.Groin);
            case BODYPART_SPINE -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.Torso_Upper) : BodyPartType.ToIndex(BodyPartType.Torso_Lower);
            case BODYPART_HEAD -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.Head) : BodyPartType.ToIndex(BodyPartType.Neck);
            case BODYPART_LEFT_UPPER_LEG -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.Groin) : BodyPartType.ToIndex(BodyPartType.UpperLeg_L);
            case BODYPART_LEFT_LOWER_LEG -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.LowerLeg_L) : BodyPartType.ToIndex(BodyPartType.Foot_L);
            case BODYPART_RIGHT_UPPER_LEG -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.Groin) : BodyPartType.ToIndex(BodyPartType.UpperLeg_R);
            case BODYPART_RIGHT_LOWER_LEG -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.LowerLeg_R) : BodyPartType.ToIndex(BodyPartType.Foot_R);
            case BODYPART_LEFT_UPPER_ARM -> BodyPartType.ToIndex(BodyPartType.UpperArm_L);
            case BODYPART_LEFT_LOWER_ARM -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.ForeArm_L) : BodyPartType.ToIndex(BodyPartType.Hand_L);
            case BODYPART_RIGHT_UPPER_ARM -> BodyPartType.ToIndex(BodyPartType.UpperArm_R);
            case BODYPART_RIGHT_LOWER_ARM -> secondaryBodyPart ? BodyPartType.ToIndex(BodyPartType.ForeArm_R) : BodyPartType.ToIndex(BodyPartType.Hand_R);
            default -> BodyPartType.ToIndex(BodyPartType.MAX);
        };
    }
}
