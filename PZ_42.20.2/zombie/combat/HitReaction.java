// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.combat;

public enum HitReaction {
    NONE(""),
    SHOT("Shot"),
    SHOT_HEAD_FWD("ShotHeadFwd"),
    SHOT_HEAD_FWD02("ShotHeadFwd02"),
    SHOT_HEAD_BWD("ShotHeadBwd"),
    SHOT_BELLY("ShotBelly"),
    SHOT_BELLY_STEP("ShotBellyStep"),
    SHOT_BELLY_STEP_BEHIND("ShotBellyStepBehind"),
    SHOT_CHEST("ShotChest"),
    SHOT_CHEST_L("ShotChestL"),
    SHOT_CHEST_R("ShotChestR"),
    SHOT_CHEST_STEP_L("ShotChestStepL"),
    SHOT_CHEST_STEP_R("ShotChestStepR"),
    SHOT_SHOULDER_L("ShotShoulderL"),
    SHOT_SHOULDER_R("ShotShoulderR"),
    SHOT_SHOULDER_STEP_L("ShotShoulderStepL"),
    SHOT_SHOULDER_STEP_R("ShotShoulderStepR"),
    SHOT_LEG_L("ShotLegL"),
    SHOT_LEG_R("ShotLegR"),
    HEAD_LEFT("HeadLeft"),
    HEAD_TOP("HeadTop"),
    UPPERCUT("Uppercut"),
    KNIFE_DEATH("KnifeDeath"),
    HEAD_RIGHT("HeadRight"),
    HIT_SPEAR1("HitSpearDeath1"),
    HIT_SPEAR2("HitSpearDeath2"),
    ON_KNEES("OnKnees"),
    EATING("Eating"),
    GETTING_UP_FRONT("GettingUpFront"),
    FLOOR("Floor");

    private final String value;

    private HitReaction(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static HitReaction fromString(String value) {
        for (HitReaction hitReaction : values()) {
            if (hitReaction.value.equalsIgnoreCase(value)) {
                return hitReaction;
            }
        }

        return NONE;
    }
}
