// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.AttachedItems;

import java.util.ArrayList;

public final class AttachedModelName {
    public String attachmentNameSelf;
    public String attachmentNameParent;
    public String modelName;
    public float bloodLevel;
    public ArrayList<AttachedModelName> children;

    public AttachedModelName(AttachedModelName other) {
        this.attachmentNameSelf = other.attachmentNameSelf;
        this.attachmentNameParent = other.attachmentNameParent;
        this.modelName = other.modelName;
        this.bloodLevel = other.bloodLevel;

        for (int i = 0; i < other.getChildCount(); i++) {
            AttachedModelName child = other.getChildByIndex(i);
            this.addChild(new AttachedModelName(child));
        }
    }

    public AttachedModelName(String attachmentName, String modelName, float bloodLevel) {
        this.attachmentNameSelf = attachmentName;
        this.attachmentNameParent = attachmentName;
        this.modelName = modelName;
        this.bloodLevel = bloodLevel;
    }

    public AttachedModelName(String attachmentNameSelf, String attachmentNameParent, String modelName, float bloodLevel) {
        this.attachmentNameSelf = attachmentNameSelf;
        this.attachmentNameParent = attachmentNameParent;
        this.modelName = modelName;
        this.bloodLevel = bloodLevel;
    }

    public void addChild(AttachedModelName child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }

        this.children.add(child);
    }

    public int getChildCount() {
        return this.children == null ? 0 : this.children.size();
    }

    public AttachedModelName getChildByIndex(int index) {
        return this.children.get(index);
    }
}
