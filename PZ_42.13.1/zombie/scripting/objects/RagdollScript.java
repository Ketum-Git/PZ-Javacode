// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.core.physics.Bullet;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.physics.RagdollJoint;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public final class RagdollScript extends BaseScriptObject {
    private static final String[] boneNameArray = new String[]{
        "Dummy01",
        "Bip01",
        "Bip01_Pelvis",
        "Bip01_Spine",
        "Bip01_Spine1",
        "Bip01_Neck",
        "Bip01_Head",
        "Bip01_L_Clavicle",
        "Bip01_L_UpperArm",
        "Bip01_L_Forearm",
        "Bip01_L_Hand",
        "Bip01_L_Finger0",
        "Bip01_L_Finger1",
        "Bip01_R_Clavicle",
        "Bip01_R_UpperArm",
        "Bip01_R_Forearm",
        "Bip01_R_Hand",
        "Bip01_R_Finger0",
        "Bip01_R_Finger1",
        "Bip01_BackPack",
        "Bip01_L_Thigh",
        "Bip01_L_Calf",
        "Bip01_L_Foot",
        "Bip01_L_Toe0",
        "Bip01_R_Thigh",
        "Bip01_R_Calf",
        "Bip01_R_Foot",
        "Bip01_R_Toe0",
        "Bip01_DressFront",
        "Bip01_DressFront02",
        "Bip01_DressBack",
        "Bip01_DressBack02",
        "Bip01_Prop1",
        "Bip01_Prop2",
        "Translation_Data"
    };
    private static final int NumberOfRagdollConstraintAttributes = 22;
    private static final int NumberOfRagdollAnchorAttributes = 5;
    private static final int NumberOfRagdollBodyPartInfo = 10;
    public static final int NumberOfRagdollBodyDynamics = 8;
    private static final ArrayList<RagdollConstraint> ragdollConstraintList = new ArrayList<>();
    private static final ArrayList<RagdollAnchor> ragdollAnchorList = new ArrayList<>();
    private static final ArrayList<RagdollBodyPartInfo> ragdollBodyPartInfoList = new ArrayList<>();
    private static final ArrayList<RagdollBodyDynamics> ragdollBodyDynamicsList = new ArrayList<>();
    private static String originalConstraintFilename;
    private static String originalAnchorFilename;
    private static String originalBodyPartInfoFilename;
    private static String originalBodyDynamicsFilename;

    public RagdollScript() {
        super(ScriptType.Ragdoll);
    }

    public static ArrayList<RagdollConstraint> getRagdollConstraintList() {
        return ragdollConstraintList;
    }

    public static ArrayList<RagdollAnchor> getRagdollAnchorList() {
        return ragdollAnchorList;
    }

    public static ArrayList<RagdollBodyPartInfo> getRagdollBodyPartInfoList() {
        return ragdollBodyPartInfoList;
    }

    public static ArrayList<RagdollBodyDynamics> getRagdollBodyDynamicsList() {
        return ragdollBodyDynamicsList;
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        if (scriptMgr.currentFileName.contains("_constraints")) {
            originalConstraintFilename = scriptMgr.currentFileName;
        } else if (scriptMgr.currentFileName.contains("_anchors")) {
            originalAnchorFilename = scriptMgr.currentFileName;
        } else if (scriptMgr.currentFileName.contains("_bodypartinfo")) {
            originalBodyPartInfoFilename = scriptMgr.currentFileName;
        } else if (scriptMgr.currentFileName.contains("_bodydynamics")) {
            originalBodyDynamicsFilename = scriptMgr.currentFileName;
        }
    }

    public static void toBullet(boolean liveUpdate) {
        if (!GameServer.server) {
            uploadBodyDynamics(liveUpdate);
            uploadConstraints(liveUpdate);
            uploadAnchors(liveUpdate);
            uploadBodyPartInfo(liveUpdate);
        }
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        if (block.id.contains("_constraint")) {
            this.loadConstraints(block);
        } else if (block.id.contains("_anchor")) {
            this.loadAnchors(block);
        } else if (block.id.contains("_bodypartinfo")) {
            this.loadBodyPartInfo(block);
        } else if (block.id.contains("_bodydynamics")) {
            this.loadBodyDynamics(block);
        }
    }

    public static void resetConstraintsToDefaultValues() {
        for (RagdollConstraint ragdollConstraint : ragdollConstraintList) {
            ragdollConstraint.constraintAxisA.set(ragdollConstraint.defaultConstraintAxisA);
            ragdollConstraint.constraintAxisB.set(ragdollConstraint.defaultConstraintAxisB);
            ragdollConstraint.constraintPositionOffsetA.set(ragdollConstraint.defaultConstraintPositionOffsetA);
            ragdollConstraint.constraintPositionOffsetB.set(ragdollConstraint.defaultConstraintPositionOffsetB);
            ragdollConstraint.constraintLimit.set(ragdollConstraint.defaultConstraintLimit);
            ragdollConstraint.constraintLimitExtended.set(ragdollConstraint.defaultConstraintLimitExtended);
        }

        uploadConstraints(true);
    }

    public static void resetAnchorsToDefaultValues() {
        for (RagdollAnchor ragdollAnchor : ragdollAnchorList) {
            ragdollAnchor.bodyPart = ragdollAnchor.defaultBodyPart;
            ragdollAnchor.reverse = ragdollAnchor.defaultReverse;
            ragdollAnchor.enabled = ragdollAnchor.defaultEnabled;
        }

        uploadAnchors(true);
    }

    public static void resetBodyPartInfoToDefaultValues() {
        for (RagdollBodyPartInfo ragdollBodyPartInfo : ragdollBodyPartInfoList) {
            ragdollBodyPartInfo.calculateLength = ragdollBodyPartInfo.defaultCalculateLength;
            ragdollBodyPartInfo.radius = ragdollBodyPartInfo.defaultRadius;
            ragdollBodyPartInfo.height = ragdollBodyPartInfo.defaultHeight;
            ragdollBodyPartInfo.gap = ragdollBodyPartInfo.defaultGap;
            ragdollBodyPartInfo.shape = ragdollBodyPartInfo.defaultShape;
            ragdollBodyPartInfo.mass = ragdollBodyPartInfo.defaultMass;
            ragdollBodyPartInfo.offset.set(ragdollBodyPartInfo.defaultOffset);
        }

        uploadBodyPartInfo(true);
    }

    public static void resetBodyDynamicsToDefaultValues() {
        for (RagdollBodyDynamics ragdollBodyDynamics : ragdollBodyDynamicsList) {
            ragdollBodyDynamics.linearDamping = ragdollBodyDynamics.defaultLinearDamping;
            ragdollBodyDynamics.angularDamping = ragdollBodyDynamics.defaultAngularDamping;
            ragdollBodyDynamics.deactivationTime = ragdollBodyDynamics.defaultDeactivationTime;
            ragdollBodyDynamics.linearSleepingThreshold = ragdollBodyDynamics.defaultLinearSleepingThreshold;
            ragdollBodyDynamics.angularSleepingThreshold = ragdollBodyDynamics.defaultAngularSleepingThreshold;
            ragdollBodyDynamics.friction = ragdollBodyDynamics.defaultFriction;
            ragdollBodyDynamics.rollingFriction = ragdollBodyDynamics.defaultRollingFriction;
        }

        uploadBodyDynamics(true);
    }

    private void loadConstraints(ScriptParser.Block block) {
        RagdollConstraint ragdollConstraint = new RagdollConstraint();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=");
                String k = ss[0].trim();
                String v = ss[1].trim();
                if ("joint".equals(k)) {
                    ragdollConstraint.joint = Integer.parseInt(v);
                } else if ("constraintType".equals(k)) {
                    ragdollConstraint.constraintType = Integer.parseInt(v);
                } else if ("constraintPartA".equals(k)) {
                    ragdollConstraint.constraintPartA = Integer.parseInt(v);
                } else if ("constraintPartB".equals(k)) {
                    ragdollConstraint.constraintPartB = Integer.parseInt(v);
                } else if ("constraintAxisA".equals(k)) {
                    this.LoadVector3(v, ragdollConstraint.constraintAxisA);
                    this.LoadVector3(v, ragdollConstraint.defaultConstraintAxisA);
                } else if ("constraintAxisB".equals(k)) {
                    this.LoadVector3(v, ragdollConstraint.constraintAxisB);
                    this.LoadVector3(v, ragdollConstraint.defaultConstraintAxisB);
                } else if ("constraintPositionOffsetA".equals(k)) {
                    this.LoadVector3(v, ragdollConstraint.constraintPositionOffsetA);
                    this.LoadVector3(v, ragdollConstraint.defaultConstraintPositionOffsetA);
                } else if ("constraintPositionOffsetB".equals(k)) {
                    this.LoadVector3(v, ragdollConstraint.constraintPositionOffsetB);
                    this.LoadVector3(v, ragdollConstraint.defaultConstraintPositionOffsetB);
                } else if ("constraintLimit".equals(k)) {
                    this.LoadVector3(v, ragdollConstraint.constraintLimit);
                    this.LoadVector3(v, ragdollConstraint.defaultConstraintLimit);
                } else if ("constraintLimitExtended".equals(k)) {
                    this.LoadVector3(v, ragdollConstraint.constraintLimitExtended);
                    this.LoadVector3(v, ragdollConstraint.defaultConstraintLimitExtended);
                }
            }
        }

        ragdollConstraintList.add(ragdollConstraint);
    }

    private void loadAnchors(ScriptParser.Block block) {
        RagdollAnchor ragdollAnchor = new RagdollAnchor();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=");
                String k = ss[0].trim();
                String v = ss[1].trim();
                if ("bone".equals(k)) {
                    ragdollAnchor.bone = Integer.parseInt(v);
                } else if ("bodyPart".equals(k)) {
                    ragdollAnchor.defaultBodyPart = ragdollAnchor.bodyPart = Integer.parseInt(v);
                } else if ("enabled".equals(k)) {
                    ragdollAnchor.defaultEnabled = ragdollAnchor.enabled = Boolean.parseBoolean(v);
                } else if ("reverse".equals(k)) {
                    ragdollAnchor.defaultReverse = ragdollAnchor.reverse = Boolean.parseBoolean(v);
                } else if ("original".equals(k)) {
                    ragdollAnchor.defaultOriginal = ragdollAnchor.original = Boolean.parseBoolean(v);
                }
            }
        }

        ragdollAnchorList.add(ragdollAnchor);
    }

    private void loadBodyPartInfo(ScriptParser.Block block) {
        RagdollBodyPartInfo ragdollBodyPartInfo = new RagdollBodyPartInfo();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=");
                String k = ss[0].trim();
                String v = ss[1].trim();
                if ("part".equals(k)) {
                    ragdollBodyPartInfo.part = Integer.parseInt(v);
                } else if ("calculateLength".equals(k)) {
                    ragdollBodyPartInfo.calculateLength = ragdollBodyPartInfo.defaultCalculateLength = Boolean.parseBoolean(v);
                } else if ("radius".equals(k)) {
                    ragdollBodyPartInfo.radius = ragdollBodyPartInfo.defaultRadius = Float.parseFloat(v);
                } else if ("height".equals(k)) {
                    ragdollBodyPartInfo.height = ragdollBodyPartInfo.defaultHeight = Float.parseFloat(v);
                } else if ("gap".equals(k)) {
                    ragdollBodyPartInfo.gap = ragdollBodyPartInfo.defaultGap = Float.parseFloat(v);
                } else if ("shape".equals(k)) {
                    ragdollBodyPartInfo.shape = ragdollBodyPartInfo.defaultShape = Integer.parseInt(v);
                } else if ("mass".equals(k)) {
                    ragdollBodyPartInfo.mass = ragdollBodyPartInfo.defaultMass = Float.parseFloat(v);
                } else if ("offset".equals(k)) {
                    this.LoadVector3(v, ragdollBodyPartInfo.offset);
                    this.LoadVector3(v, ragdollBodyPartInfo.defaultOffset);
                }
            }
        }

        ragdollBodyPartInfoList.add(ragdollBodyPartInfo);
    }

    public void loadBodyDynamics(ScriptParser.Block block) {
        RagdollBodyDynamics ragdollBodyDynamics = new RagdollBodyDynamics();

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=");
                String k = ss[0].trim();
                String v = ss[1].trim();
                if ("part".equals(k)) {
                    ragdollBodyDynamics.part = Integer.parseInt(v);
                } else if ("linearDamping".equals(k)) {
                    ragdollBodyDynamics.linearDamping = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultLinearDamping = ragdollBodyDynamics.linearDamping;
                } else if ("angularDamping".equals(k)) {
                    ragdollBodyDynamics.angularDamping = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultAngularDamping = ragdollBodyDynamics.angularDamping;
                } else if ("deactivationTime".equals(k)) {
                    ragdollBodyDynamics.deactivationTime = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultDeactivationTime = ragdollBodyDynamics.deactivationTime;
                } else if ("linearSleepingThreshold".equals(k)) {
                    ragdollBodyDynamics.linearSleepingThreshold = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultLinearSleepingThreshold = ragdollBodyDynamics.linearSleepingThreshold;
                } else if ("angularSleepingThreshold".equals(k)) {
                    ragdollBodyDynamics.angularSleepingThreshold = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultAngularSleepingThreshold = ragdollBodyDynamics.angularSleepingThreshold;
                } else if ("friction".equals(k)) {
                    ragdollBodyDynamics.friction = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultFriction = ragdollBodyDynamics.friction;
                } else if ("rollingFriction".equals(k)) {
                    ragdollBodyDynamics.rollingFriction = Float.parseFloat(v);
                    ragdollBodyDynamics.defaultRollingFriction = ragdollBodyDynamics.rollingFriction;
                }
            }
        }

        ragdollBodyDynamicsList.add(ragdollBodyDynamics);
    }

    public static void uploadConstraints(boolean liveUpdate) {
        int arrayIndex = 0;
        float[] params = new float[22 * ragdollConstraintList.size()];

        for (RagdollConstraint ragdollConstraint : ragdollConstraintList) {
            params[arrayIndex++] = ragdollConstraint.joint;
            params[arrayIndex++] = ragdollConstraint.constraintType;
            params[arrayIndex++] = ragdollConstraint.constraintPartA;
            params[arrayIndex++] = ragdollConstraint.constraintPartB;
            params[arrayIndex++] = ragdollConstraint.constraintAxisA.x;
            params[arrayIndex++] = ragdollConstraint.constraintAxisA.y;
            params[arrayIndex++] = ragdollConstraint.constraintAxisA.z;
            params[arrayIndex++] = ragdollConstraint.constraintAxisB.x;
            params[arrayIndex++] = ragdollConstraint.constraintAxisB.y;
            params[arrayIndex++] = ragdollConstraint.constraintAxisB.z;
            params[arrayIndex++] = ragdollConstraint.constraintPositionOffsetA.x;
            params[arrayIndex++] = ragdollConstraint.constraintPositionOffsetA.y;
            params[arrayIndex++] = ragdollConstraint.constraintPositionOffsetA.z;
            params[arrayIndex++] = ragdollConstraint.constraintPositionOffsetB.x;
            params[arrayIndex++] = ragdollConstraint.constraintPositionOffsetB.y;
            params[arrayIndex++] = ragdollConstraint.constraintPositionOffsetB.z;
            params[arrayIndex++] = ragdollConstraint.constraintLimit.x;
            params[arrayIndex++] = ragdollConstraint.constraintLimit.y;
            params[arrayIndex++] = ragdollConstraint.constraintLimit.z;
            params[arrayIndex++] = ragdollConstraint.constraintLimitExtended.x;
            params[arrayIndex++] = ragdollConstraint.constraintLimitExtended.y;
            params[arrayIndex++] = ragdollConstraint.constraintLimitExtended.z;
        }

        Bullet.defineRagdollConstraints(params, liveUpdate);
    }

    public static void uploadBodyPartInfo(boolean liveUpdate) {
        int arrayIndex = 0;
        float[] params = new float[10 * ragdollBodyPartInfoList.size()];

        for (RagdollBodyPartInfo ragdollBodyPartInfo : ragdollBodyPartInfoList) {
            params[arrayIndex++] = ragdollBodyPartInfo.part;
            params[arrayIndex++] = ragdollBodyPartInfo.calculateLength ? 1.0F : 0.0F;
            params[arrayIndex++] = ragdollBodyPartInfo.radius;
            params[arrayIndex++] = ragdollBodyPartInfo.height;
            params[arrayIndex++] = ragdollBodyPartInfo.gap;
            params[arrayIndex++] = ragdollBodyPartInfo.shape;
            params[arrayIndex++] = ragdollBodyPartInfo.mass;
            params[arrayIndex++] = ragdollBodyPartInfo.offset.x;
            params[arrayIndex++] = ragdollBodyPartInfo.offset.y;
            params[arrayIndex++] = ragdollBodyPartInfo.offset.z;
        }

        Bullet.defineRagdollBodyPartInfo(params, liveUpdate);
    }

    public static void uploadAnchors(boolean liveUpdate) {
        int arrayIndex = 0;
        float[] params = new float[5 * ragdollAnchorList.size()];

        for (RagdollAnchor ragdollAnchor : ragdollAnchorList) {
            params[arrayIndex++] = ragdollAnchor.bone;
            params[arrayIndex++] = ragdollAnchor.bodyPart;
            params[arrayIndex++] = ragdollAnchor.reverse ? 1.0F : 0.0F;
            params[arrayIndex++] = ragdollAnchor.original ? 1.0F : 0.0F;
            params[arrayIndex++] = ragdollAnchor.enabled ? 1.0F : 0.0F;
        }

        Bullet.defineRagdollAnchors(params, liveUpdate);
    }

    public static void uploadBodyDynamics(boolean liveUpdate) {
        int arrayIndex = 0;
        float[] params = new float[8 * ragdollBodyDynamicsList.size()];

        for (RagdollBodyDynamics ragdollBodyDynamics : ragdollBodyDynamicsList) {
            params[arrayIndex++] = ragdollBodyDynamics.part;
            params[arrayIndex++] = ragdollBodyDynamics.linearDamping;
            params[arrayIndex++] = ragdollBodyDynamics.angularDamping;
            params[arrayIndex++] = ragdollBodyDynamics.deactivationTime;
            params[arrayIndex++] = ragdollBodyDynamics.linearSleepingThreshold;
            params[arrayIndex++] = ragdollBodyDynamics.angularSleepingThreshold;
            params[arrayIndex++] = ragdollBodyDynamics.friction;
            params[arrayIndex++] = ragdollBodyDynamics.rollingFriction;
        }

        Bullet.defineRagdollBodyDynamics(params, liveUpdate);
    }

    public static void writeConstraintsToFile() {
        String filename = originalConstraintFilename;
        String userDirectory = System.getProperty("user.dir");
        if (userDirectory != null) {
            filename = userDirectory + "\\" + originalConstraintFilename;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("module Base \n{\n");

            for (RagdollConstraint ragdollConstraint : ragdollConstraintList) {
                writer.write("    ragdoll " + RagdollJoint.values()[ragdollConstraint.joint].toString() + "_constraint\n    {\n");
                writer.write("        joint = " + ragdollConstraint.joint + ",\n");
                writer.write("        constraintType = " + ragdollConstraint.constraintType + ",\n");
                writer.write("        constraintPartA = " + ragdollConstraint.constraintPartA + ",\n");
                writer.write("        constraintPartB = " + ragdollConstraint.constraintPartB + ",\n");
                writer.write(
                    "        constraintAxisA = "
                        + ragdollConstraint.constraintAxisA.x
                        + " "
                        + ragdollConstraint.constraintAxisA.y
                        + " "
                        + ragdollConstraint.constraintAxisA.z
                        + ",\n"
                );
                writer.write(
                    "        constraintAxisB = "
                        + ragdollConstraint.constraintAxisB.x
                        + " "
                        + ragdollConstraint.constraintAxisB.y
                        + " "
                        + ragdollConstraint.constraintAxisB.z
                        + ",\n"
                );
                writer.write(
                    "        constraintPositionOffsetA = "
                        + ragdollConstraint.constraintPositionOffsetA.x
                        + " "
                        + ragdollConstraint.constraintPositionOffsetA.y
                        + " "
                        + ragdollConstraint.constraintPositionOffsetA.z
                        + ",\n"
                );
                writer.write(
                    "        constraintPositionOffsetB = "
                        + ragdollConstraint.constraintPositionOffsetB.x
                        + " "
                        + ragdollConstraint.constraintPositionOffsetB.y
                        + " "
                        + ragdollConstraint.constraintPositionOffsetB.z
                        + ",\n"
                );
                writer.write(
                    "        constraintLimit = "
                        + ragdollConstraint.constraintLimit.x
                        + " "
                        + ragdollConstraint.constraintLimit.y
                        + " "
                        + ragdollConstraint.constraintLimit.z
                        + ",\n"
                );
                writer.write("    }\n\n");
            }

            writer.write("}\n");
        } catch (Exception var7) {
            DebugLog.Script.error(var7);
        }
    }

    public static void writeAnchorsToFile() {
        String filename = originalAnchorFilename;
        String userDirectory = System.getProperty("user.dir");
        if (userDirectory != null) {
            filename = userDirectory + "\\" + originalAnchorFilename;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("module Base \n{\n");

            for (RagdollAnchor ragdollAnchor : ragdollAnchorList) {
                writer.write("    ragdoll " + boneNameArray[ragdollAnchor.bone] + "_anchor\n    {\n");
                writer.write("        bone = " + ragdollAnchor.bone + ",\n");
                writer.write("        bodyPart = " + ragdollAnchor.bodyPart + ",\n");
                writer.write("        reverse = " + ragdollAnchor.reverse + ",\n");
                writer.write("        original = " + ragdollAnchor.original + ",\n");
                writer.write("        enabled = " + ragdollAnchor.enabled + ",\n");
                writer.write("    }\n\n");
            }

            writer.write("}\n");
        } catch (Exception var7) {
            DebugLog.Script.error(var7);
        }
    }

    public static void writeBodyPartInfoToFile() {
        String filename = originalBodyPartInfoFilename;
        String userDirectory = System.getProperty("user.dir");
        if (userDirectory != null) {
            filename = userDirectory + "\\" + originalBodyPartInfoFilename;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("module Base \n{\n");

            for (RagdollBodyPartInfo ragdollBodyPartInfo : ragdollBodyPartInfoList) {
                writer.write("    ragdoll " + RagdollBodyPart.values()[ragdollBodyPartInfo.part].toString() + "_bodypartinfo\n    {\n");
                writer.write("        part = " + ragdollBodyPartInfo.part + ",\n");
                writer.write("        calculateLength = " + ragdollBodyPartInfo.calculateLength + ",\n");
                writer.write("        radius = " + ragdollBodyPartInfo.radius + ",\n");
                writer.write("        height = " + ragdollBodyPartInfo.height + ",\n");
                writer.write("        gap = " + ragdollBodyPartInfo.gap + ",\n");
                writer.write("        shape = " + ragdollBodyPartInfo.shape + ",\n");
                writer.write("        mass = " + ragdollBodyPartInfo.mass + ",\n");
                writer.write(
                    "        offset = " + ragdollBodyPartInfo.offset.x + " " + ragdollBodyPartInfo.offset.y + " " + ragdollBodyPartInfo.offset.z + ",\n"
                );
                writer.write("    }\n\n");
            }

            writer.write("}\n");
        } catch (Exception var7) {
            DebugLog.Script.error(var7);
        }
    }

    public static void writeBodyDynamicsToFile() {
        String filename = originalBodyDynamicsFilename;
        String userDirectory = System.getProperty("user.dir");
        if (userDirectory != null) {
            filename = userDirectory + "\\" + originalBodyDynamicsFilename;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("module Base \n{\n");

            for (RagdollBodyDynamics ragdollBodyDynamics : ragdollBodyDynamicsList) {
                writer.write("    ragdoll " + RagdollBodyPart.values()[ragdollBodyDynamics.part].toString() + "_bodydynamics\n    {\n");
                writer.write("        part = " + ragdollBodyDynamics.part + ",\n");
                writer.write("        linearDamping = " + ragdollBodyDynamics.linearDamping + ",\n");
                writer.write("        angularDamping = " + ragdollBodyDynamics.angularDamping + ",\n");
                writer.write("        deactivationTime = " + ragdollBodyDynamics.deactivationTime + ",\n");
                writer.write("        linearSleepingThreshold = " + ragdollBodyDynamics.linearSleepingThreshold + ",\n");
                writer.write("        angularSleepingThreshold = " + ragdollBodyDynamics.angularSleepingThreshold + ",\n");
                writer.write("        friction = " + ragdollBodyDynamics.friction + ",\n");
                writer.write("        rollingFriction = " + ragdollBodyDynamics.rollingFriction + ",\n");
                writer.write("    }\n\n");
            }

            writer.write("}\n");
        } catch (Exception var7) {
            DebugLog.Script.error(var7);
        }
    }
}
