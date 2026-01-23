// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import zombie.characters.IsoGameCharacter;
import zombie.characters.RagdollBuilder;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

public class RagdollSettingsManager {
    public static final float DefaultImpulse = 80.0F;
    public static final float DefaultImpulseMin = 0.0F;
    public static final float DefaultImpulseMax = 200.0F;
    public static final float DefaultUpwardImpulse = 40.0F;
    public static final float DefaultUpwardImpulseMin = 0.0F;
    public static final float DefaultUpwardImpulseMax = 200.0F;
    public static final int GlobalChanceID = 0;
    private static RagdollSettingsManager instance = new RagdollSettingsManager();
    private final int ragdollSettingsCount = 1;
    private final int hitReactionSettingsCount = 36;
    private final int locationsCount = 11;
    private final int globalImpulseId = 12;
    private final int globalUpImpulseId = 24;
    private final RagdollSettingsManager.RagdollSetting[] ragdollSettings = new RagdollSettingsManager.RagdollSetting[1];
    private final RagdollSettingsManager.HitReactionSetting[] hitReactionSettings = new RagdollSettingsManager.HitReactionSetting[36];
    private final RagdollSettingsManager.ForceHitReactionLocation[] forceHitReactionLocations = new RagdollSettingsManager.ForceHitReactionLocation[11];

    public RagdollSettingsManager() {
        this.setup();
    }

    public static RagdollSettingsManager getInstance() {
        return instance;
    }

    public static void setInstance(RagdollSettingsManager ragdollSettingsManager) {
        instance = ragdollSettingsManager;
    }

    public int getSettingsCount() {
        return 1;
    }

    public int getHitReactionSettingsCount() {
        return 36;
    }

    public int getHitReactionLocationsCount() {
        return 11;
    }

    public boolean usePhysicHitReaction(IsoGameCharacter isoGameCharacter) {
        if (this.isForcedHitReaction()) {
            RagdollSettingsManager.HitReactionSetting hitReactionSetting = this.getHitReactionSetting(0);
            float hitReactionChance = hitReactionSetting.getAdminValue();
            if (!hitReactionSetting.isEnableAdmin()) {
                String hitReaction = isoGameCharacter.getHitReaction();
                RagdollBodyPart bodyPart = RagdollBodyPart.BODYPART_PELVIS;
                switch (hitReaction) {
                    case "ShotBelly":
                    case "ShotBellyStep":
                        bodyPart = RagdollBodyPart.BODYPART_PELVIS;
                        break;
                    case "ShotChest":
                    case "ShotChestR":
                    case "ShotChestL":
                        bodyPart = RagdollBodyPart.BODYPART_SPINE;
                        break;
                    case "ShotLegR":
                        bodyPart = RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
                        break;
                    case "ShotLegL":
                        bodyPart = RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
                        break;
                    case "ShotShoulderStepR":
                        bodyPart = RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
                        break;
                    case "ShotShoulderStepL":
                        bodyPart = RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
                        break;
                    case "ShotHeadFwd":
                    case "ShotHeadFwd02":
                        bodyPart = RagdollBodyPart.BODYPART_HEAD;
                        break;
                    default:
                        DebugLog.Physics.debugln("RagdollState: HitReaction %s CASE NOT DEFINED", hitReaction);
                }

                boolean enabled = this.getEnabledSetting(bodyPart);
                if (!enabled) {
                    return false;
                } else {
                    hitReactionChance = this.getChanceSetting(bodyPart);
                    float chance = Rand.Next(0.0F, 100.0F);
                    return hitReactionChance > 0.0F && chance <= hitReactionChance;
                }
            } else {
                return true;
            }
        } else if (!Core.getInstance().getOptionUsePhysicsHitReaction()) {
            return false;
        } else {
            float hitReactionChance = this.getSandboxHitReactionFrequency();
            float chance = Rand.Next(0.0F, 100.0F);
            return hitReactionChance > 0.0F && chance <= hitReactionChance;
        }
    }

    private RagdollSettingsManager.RagdollSetting initRagdollSetting(int id, String name, float defaultValue, float min, float max) {
        if (id >= 0 && id < this.ragdollSettings.length) {
            return this.ragdollSettings[id].init(id, name, defaultValue, min, max);
        } else {
            DebugLog.Physics.error("RagdollSetting: id(%i) out of range ", id);
            return null;
        }
    }

    private RagdollSettingsManager.HitReactionSetting initHitReactionSetting(int id, String name, float defaultValue, float min, float max) {
        if (id >= 0 && id < this.hitReactionSettings.length) {
            return this.hitReactionSettings[id].init(id, name, defaultValue, min, max);
        } else {
            DebugLog.Physics.error("HitReactionSetting: id(%i) out of range ", id);
            return null;
        }
    }

    private RagdollSettingsManager.ForceHitReactionLocation initForceHitReactionLocation(int id, String name) {
        if (id >= 0 && id < this.forceHitReactionLocations.length) {
            return this.forceHitReactionLocations[id].init(id, name);
        } else {
            DebugLog.Physics.error("ForceHitReactionLocation: id(%i) out of range ", id);
            return null;
        }
    }

    public RagdollSettingsManager.RagdollSetting getSetting(int id) {
        if (id >= 0 && id < this.ragdollSettings.length) {
            return this.ragdollSettings[id];
        } else {
            DebugLog.Physics.error("RagdollSetting: id(%i) out of range ", id);
            return null;
        }
    }

    public RagdollSettingsManager.HitReactionSetting getHitReactionSetting(int id) {
        if (id >= 0 && id < this.hitReactionSettings.length) {
            return this.hitReactionSettings[id];
        } else {
            DebugLog.Physics.error("HitReactionSetting: id(%i) out of range ", id);
            return null;
        }
    }

    public boolean getEnabledSetting(RagdollBodyPart bodyPart) {
        int partID = bodyPart.ordinal() + 1;
        return this.getHitReactionSetting(partID).isAdminOverride;
    }

    public float getChanceSetting(RagdollBodyPart bodyPart) {
        int partID = bodyPart.ordinal() + 1;
        return this.getHitReactionSetting(partID).adminValue;
    }

    public float getImpulseSetting(RagdollBodyPart bodyPart) {
        int partID = bodyPart.ordinal() + 12 + 1;
        return this.getHitReactionSetting(partID).adminValue;
    }

    public float getUpImpulseSetting(RagdollBodyPart bodyPart) {
        int partID = bodyPart.ordinal() + 24 + 1;
        return this.getHitReactionSetting(partID).adminValue;
    }

    public float getGlobalImpulseSetting() {
        return this.getHitReactionSetting(12).adminValue;
    }

    public float getGlobalUpImpulseSetting() {
        return this.getHitReactionSetting(24).adminValue;
    }

    public RagdollSettingsManager.ForceHitReactionLocation getForceHitReactionLocation(int id) {
        if (id >= 0 && id < this.forceHitReactionLocations.length) {
            return this.forceHitReactionLocations[id];
        } else {
            DebugLog.Physics.error("ForceHitReactionLocation: id(%i) out of range ", id);
            return null;
        }
    }

    private void setup() {
        for (int i = 0; i < this.ragdollSettings.length; i++) {
            this.ragdollSettings[i] = new RagdollSettingsManager.RagdollSetting();
        }

        for (int i = 0; i < this.hitReactionSettings.length; i++) {
            this.hitReactionSettings[i] = new RagdollSettingsManager.HitReactionSetting();
        }

        for (int i = 0; i < this.forceHitReactionLocations.length; i++) {
            this.forceHitReactionLocations[i] = new RagdollSettingsManager.ForceHitReactionLocation();
        }

        float defaultMass = RagdollBuilder.instance.getMass();
        float defaultMassMin = 1.0F;
        float defaultMassMax = 100.0F;
        float defaultChance = 100.0F;
        float defaultChanceMin = 0.0F;
        float defaultChanceMax = 100.0F;
        int id = 0;
        this.initRagdollSetting(id++, "Mass", defaultMass, 1.0F, 100.0F);
        id = 0;
        this.initHitReactionSetting(id++, "Chance:Global", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:Pelvis", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:Spine", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:Head", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:L Thigh", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:L Calf", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:R Thigh", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:R Calf", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:L Upper Arm", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:L Lower Arm", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:R Upper Arm", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Chance:R Lower Arm", 100.0F, 0.0F, 100.0F);
        this.initHitReactionSetting(id++, "Impulse:Global", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:Pelvis", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:Spine", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:Head", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:L Thigh", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:L Calf", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:R Thigh", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:R Calf", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:L Upper Arm", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:L Lower Arm", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:R Upper Arm", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "Impulse:R Lower Arm", 80.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:Global", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:Pelvis", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:Spine", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:Head", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:L Thigh", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:L Calf", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:R Thigh", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:R Calf", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:L Upper Arm", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:L Lower Arm", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:R Upper Arm", 40.0F, 0.0F, 200.0F);
        this.initHitReactionSetting(id++, "UpImpulse:R Lower Arm", 40.0F, 0.0F, 200.0F);
        id = 0;
        this.initForceHitReactionLocation(id++, "Pelvis");
        this.initForceHitReactionLocation(id++, "Spine");
        this.initForceHitReactionLocation(id++, "Head");
        this.initForceHitReactionLocation(id++, "L Thigh");
        this.initForceHitReactionLocation(id++, "L Calf");
        this.initForceHitReactionLocation(id++, "R Thigh");
        this.initForceHitReactionLocation(id++, "R Calf");
        this.initForceHitReactionLocation(id++, "L Upper Arm");
        this.initForceHitReactionLocation(id++, "L Lower Arm");
        this.initForceHitReactionLocation(id++, "R Upper Arm");
        this.initForceHitReactionLocation(id++, "R Lower Arm");
    }

    public float getSandboxHitReactionFrequency() {
        float hitReactionChance = 100.0F;
        return 100.0F;
    }

    public float getSandboxHitReactionImpulseStrength() {
        return this.getGlobalImpulseSetting();
    }

    public float getSandboxHitReactionUpImpulseStrength() {
        return this.getGlobalUpImpulseSetting();
    }

    public void resetToDefaults() {
        for (int i = 0; i < this.ragdollSettings.length; i++) {
            this.ragdollSettings[i].reset();
        }

        for (int i = 0; i < this.hitReactionSettings.length; i++) {
            this.hitReactionSettings[i].reset();
        }

        for (int i = 0; i < this.forceHitReactionLocations.length; i++) {
            this.forceHitReactionLocations[i].setAdminValue(false);
        }
    }

    public boolean isForcedHitReaction() {
        for (int i = 0; i < this.forceHitReactionLocations.length; i++) {
            if (this.forceHitReactionLocations[i].getAdminValue()) {
                return true;
            }
        }

        return false;
    }

    public RagdollSettingsManager.ForceHitReactionLocation getForceHitReactionLocation() {
        for (int i = 0; i < this.forceHitReactionLocations.length; i++) {
            if (this.forceHitReactionLocations[i].getAdminValue()) {
                return this.forceHitReactionLocations[i];
            }
        }

        return null;
    }

    public String getForcedHitReactionLocationAsShotLocation() {
        int bodyPartID = -1;

        for (int i = 0; i < this.forceHitReactionLocations.length; i++) {
            if (this.forceHitReactionLocations[i].getAdminValue()) {
                bodyPartID = i;
                break;
            }
        }

        String shotLocation = "Default";
        switch (bodyPartID) {
            case 0:
                shotLocation = "ShotBelly";
                break;
            case 1:
                shotLocation = "ShotChest";
                break;
            case 2:
                shotLocation = "ShotHeadFwd";
                break;
            case 3:
                shotLocation = "ShotLegL";
                break;
            case 4:
                shotLocation = "ShotLegL";
                break;
            case 5:
                shotLocation = "ShotLegR";
                break;
            case 6:
                shotLocation = "ShotLegR";
                break;
            case 7:
                shotLocation = "ShotShoulderStepL";
                break;
            case 8:
                shotLocation = "ShotShoulderStepL";
                break;
            case 9:
                shotLocation = "ShotShoulderStepR";
                break;
            case 10:
                shotLocation = "ShotShoulderStepR";
                break;
            default:
                DebugLog.Physics.debugln("RagdollSettingManager: bodyPartID %s CASE NOT DEFINED", bodyPartID);
        }

        return shotLocation;
    }

    public void update() {
        float mass = this.ragdollSettings[0].adminValue;
        RagdollBuilder.instance.setMass(mass);
    }

    public static class ForceHitReactionLocation {
        int id;
        private String name;
        private boolean isAdminOverride;
        private boolean adminValue;

        public RagdollSettingsManager.ForceHitReactionLocation init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean getAdminValue() {
            return this.adminValue;
        }

        public void setAdminValue(boolean b) {
            this.adminValue = b;
        }
    }

    public static class HitReactionSetting {
        private int id;
        private String name;
        private float min;
        private float max = 1.0F;
        private boolean isAdminOverride;
        private float adminValue;
        private float finalValue;
        private float defaultValue;

        public RagdollSettingsManager.HitReactionSetting init(int id, String name, float defaultValue, float min, float max) {
            this.id = id;
            this.name = name;
            this.min = min;
            this.max = max;
            this.adminValue = defaultValue;
            this.defaultValue = defaultValue;
            return this;
        }

        public String getName() {
            return this.name;
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public float getAdminValue() {
            return this.adminValue;
        }

        public void setAdminValue(float f) {
            this.adminValue = f;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue = this.adminValue;
            }
        }

        public void reset() {
            this.adminValue = this.defaultValue;
        }
    }

    public static class RagdollSetting {
        private int id;
        private String name;
        private float min;
        private float max = 1.0F;
        private boolean isAdminOverride;
        private float adminValue;
        private float finalValue;
        private float defaultValue;

        public RagdollSettingsManager.RagdollSetting init(int id, String name, float defaultValue, float min, float max) {
            this.id = id;
            this.name = name;
            this.min = min;
            this.max = max;
            this.adminValue = defaultValue;
            this.defaultValue = defaultValue;
            return this;
        }

        public String getName() {
            return this.name;
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public float getAdminValue() {
            return this.adminValue;
        }

        public void setAdminValue(float f) {
            if (this.adminValue != f) {
                this.adminValue = f;
                RagdollSettingsManager.getInstance().update();
            }
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue = this.adminValue;
            }
        }

        public void reset() {
            if (this.adminValue != this.defaultValue) {
                this.adminValue = this.defaultValue;
                RagdollSettingsManager.getInstance().update();
            }
        }
    }
}
