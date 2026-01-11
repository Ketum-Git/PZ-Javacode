// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.debug.DebugType;
import zombie.entity.util.Predicate;

public enum FallingWhileInjured {
    Head(1.01F, 1.01F, 1.05F, bodyPart -> bodyPart == BodyPartType.Head),
    Leg(
        1.01F,
        1.2F,
        2.5F,
        bodyPart -> bodyPart == BodyPartType.UpperLeg_L
            || bodyPart == BodyPartType.UpperLeg_R
            || bodyPart == BodyPartType.LowerLeg_L
            || bodyPart == BodyPartType.LowerLeg_R
    ),
    Foot(1.01F, 1.05F, 1.75F, bodyPart -> bodyPart == BodyPartType.Foot_L || bodyPart == BodyPartType.Foot_R),
    Arm(
        1.01F,
        1.15F,
        1.5F,
        bodyPart -> bodyPart == BodyPartType.UpperArm_L
            || bodyPart == BodyPartType.UpperArm_R
            || bodyPart == BodyPartType.ForeArm_L
            || bodyPart == BodyPartType.ForeArm_R
    ),
    Hand(1.01F, 1.05F, 1.25F, bodyPart -> bodyPart == BodyPartType.Hand_L || bodyPart == BodyPartType.Hand_R),
    Spine(
        1.01F,
        1.2F,
        2.2F,
        bodyPart -> bodyPart == BodyPartType.Neck
            || bodyPart == BodyPartType.Torso_Upper
            || bodyPart == BodyPartType.Torso_Lower
            || bodyPart == BodyPartType.Groin
    );

    private final float injured;
    private final float deepInjured;
    private final float fractured;
    private final Predicate<BodyPartType> isBodyPart;

    private FallingWhileInjured(final float in_injured, final float in_deepInjured, final float in_fractured, final Predicate<BodyPartType> in_isBodyPart) {
        this.injured = in_injured;
        this.deepInjured = in_deepInjured;
        this.fractured = in_fractured;
        this.isBodyPart = in_isBodyPart;
    }

    public boolean isBodyPart(BodyPartType in_bodyPartType) {
        return this.isBodyPart.evaluate(in_bodyPartType);
    }

    public static FallingWhileInjured fromBodyPartType(BodyPartType in_bodyPartType) {
        for (FallingWhileInjured elem : values()) {
            if (elem.isBodyPart(in_bodyPartType)) {
                return elem;
            }
        }

        DebugType.FallDamage.debugln("No FallingWhileInjured found for: %s", in_bodyPartType);
        return null;
    }

    public static float getDamageMultiplier(BodyPart in_bodyPart) {
        FallingWhileInjured bodyPartInjuryElem = fromBodyPartType(in_bodyPart.type);
        if (bodyPartInjuryElem == null) {
            return 1.0F;
        } else if (in_bodyPart.getFractureTime() > 0.0F) {
            DebugType.FallDamage.debugln("Impact with fractured %s. Damage multiplier: %f", in_bodyPart.type, bodyPartInjuryElem.fractured);
            return bodyPartInjuryElem.fractured;
        } else if (in_bodyPart.isDeepWounded()) {
            DebugType.FallDamage.debugln("Impact with deep injured %s. Damage multiplier: %f", in_bodyPart.type, bodyPartInjuryElem.deepInjured);
            return bodyPartInjuryElem.deepInjured;
        } else if (in_bodyPart.HasInjury()) {
            DebugType.FallDamage.debugln("Impact with injured %s. Damage multiplier: %f", in_bodyPart.type, bodyPartInjuryElem.injured);
            return bodyPartInjuryElem.injured;
        } else {
            return 1.0F;
        }
    }
}
