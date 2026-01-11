// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import java.util.ArrayList;
import java.util.List;
import zombie.debug.DebugLog;

public final class BodyPartContacts {
    private static final BodyPartContacts.ContactNode root;
    private static final BodyPartContacts.ContactNode[] nodes;

    public static BodyPartType[] getAllContacts(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; i++) {
            BodyPartContacts.ContactNode node = nodes[i];
            if (node.bodyPart == bodyPartType) {
                return node.bodyPartAllContacts;
            }
        }

        return null;
    }

    public static BodyPartType[] getChildren(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; i++) {
            BodyPartContacts.ContactNode node = nodes[i];
            if (node.bodyPart == bodyPartType) {
                return node.bodyPartChildren;
            }
        }

        return null;
    }

    public static BodyPartType getParent(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; i++) {
            BodyPartContacts.ContactNode node = nodes[i];
            if (node.bodyPart == bodyPartType) {
                return node.bodyPartParent;
            }
        }

        return null;
    }

    public static int getNodeDepth(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; i++) {
            BodyPartContacts.ContactNode node = nodes[i];
            if (node.bodyPart == bodyPartType) {
                if (!node.initialised) {
                    DebugLog.log("Warning: attempting to get depth for non initialised node '" + node.bodyPart.toString() + "'.");
                }

                return node.depth;
            }
        }

        return -1;
    }

    private static BodyPartContacts.ContactNode getNodeForBodyPart(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].bodyPart == bodyPartType) {
                return nodes[i];
            }
        }

        return null;
    }

    private static void initNodes(BodyPartContacts.ContactNode current, int depth, BodyPartContacts.ContactNode up) {
        current.parent = up;
        current.depth = depth;
        List<BodyPartContacts.ContactNode> allContactsList = new ArrayList<>();
        if (current.parent != null) {
            allContactsList.add(current.parent);
        }

        if (current.children != null) {
            for (BodyPartContacts.ContactNode node : current.children) {
                allContactsList.add(node);
                initNodes(node, depth + 1, current);
            }
        }

        current.allContacts = new BodyPartContacts.ContactNode[allContactsList.size()];
        allContactsList.toArray(current.allContacts);
        current.initialised = true;
    }

    private static void postInit() {
        for (BodyPartContacts.ContactNode node : nodes) {
            if (node.parent != null) {
                node.bodyPartParent = node.parent.bodyPart;
            }

            if (node.children != null && node.children.length > 0) {
                node.bodyPartChildren = new BodyPartType[node.children.length];

                for (int i = 0; i < node.children.length; i++) {
                    node.bodyPartChildren[i] = node.children[i].bodyPart;
                }
            } else {
                node.bodyPartChildren = new BodyPartType[0];
            }

            if (node.allContacts != null && node.allContacts.length > 0) {
                node.bodyPartAllContacts = new BodyPartType[node.allContacts.length];

                for (int i = 0; i < node.allContacts.length; i++) {
                    node.bodyPartAllContacts[i] = node.allContacts[i].bodyPart;
                }
            } else {
                node.bodyPartAllContacts = new BodyPartType[0];
            }

            if (!node.initialised) {
                DebugLog.log("Warning: node for '" + node.bodyPart.toString() + "' is not initialised!");
            }
        }
    }

    static {
        int max = BodyPartType.ToIndex(BodyPartType.MAX);
        nodes = new BodyPartContacts.ContactNode[max];

        for (int i = 0; i < max; i++) {
            nodes[i] = new BodyPartContacts.ContactNode(BodyPartType.FromIndex(i));
        }

        root = getNodeForBodyPart(BodyPartType.Torso_Upper);
        root.children = new BodyPartContacts.ContactNode[]{
            getNodeForBodyPart(BodyPartType.Neck),
            getNodeForBodyPart(BodyPartType.Torso_Lower),
            getNodeForBodyPart(BodyPartType.UpperArm_L),
            getNodeForBodyPart(BodyPartType.UpperArm_R)
        };
        BodyPartContacts.ContactNode node = getNodeForBodyPart(BodyPartType.Neck);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.Head)};
        node = getNodeForBodyPart(BodyPartType.UpperArm_L);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.ForeArm_L)};
        node = getNodeForBodyPart(BodyPartType.ForeArm_L);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.Hand_L)};
        node = getNodeForBodyPart(BodyPartType.UpperArm_R);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.ForeArm_R)};
        node = getNodeForBodyPart(BodyPartType.ForeArm_R);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.Hand_R)};
        node = getNodeForBodyPart(BodyPartType.Torso_Lower);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.Groin)};
        node = getNodeForBodyPart(BodyPartType.Groin);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.UpperLeg_L), getNodeForBodyPart(BodyPartType.UpperLeg_R)};
        node = getNodeForBodyPart(BodyPartType.UpperLeg_L);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.LowerLeg_L)};
        node = getNodeForBodyPart(BodyPartType.LowerLeg_L);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.Foot_L)};
        node = getNodeForBodyPart(BodyPartType.UpperLeg_R);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.LowerLeg_R)};
        node = getNodeForBodyPart(BodyPartType.LowerLeg_R);
        node.children = new BodyPartContacts.ContactNode[]{getNodeForBodyPart(BodyPartType.Foot_R)};
        initNodes(root, 0, null);
        postInit();
    }

    private static class ContactNode {
        BodyPartType bodyPart;
        int depth = -1;
        BodyPartContacts.ContactNode parent;
        BodyPartContacts.ContactNode[] children;
        BodyPartContacts.ContactNode[] allContacts;
        BodyPartType bodyPartParent;
        BodyPartType[] bodyPartChildren;
        BodyPartType[] bodyPartAllContacts;
        boolean initialised;

        public ContactNode(BodyPartType bodyPart) {
            this.bodyPart = bodyPart;
        }
    }
}
