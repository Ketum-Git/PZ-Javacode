// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.AttachedItems;

import java.util.ArrayList;
import java.util.List;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class AttachedModelNames {
    protected AttachedLocationGroup group;
    protected final ArrayList<AttachedModelName> models = new ArrayList<>();

    AttachedLocationGroup getGroup() {
        return this.group;
    }

    public void copyFrom(AttachedModelNames other) {
        this.models.clear();

        for (int i = 0; i < other.models.size(); i++) {
            AttachedModelName amn = other.models.get(i);
            this.models.add(new AttachedModelName(amn));
        }
    }

    public void initFrom(AttachedItems attachedItems) {
        if (attachedItems == null) {
            this.group = null;
            this.models.clear();
        } else {
            this.group = attachedItems.getGroup();
            this.models.clear();

            for (int i = 0; i < attachedItems.size(); i++) {
                AttachedItem attachedItem = attachedItems.get(i);
                String modelName = attachedItem.getItem().getStaticModelException();
                if (!StringUtils.isNullOrWhitespace(modelName)) {
                    String attachmentName = this.group.getLocation(attachedItem.getLocation()).getAttachmentName();
                    HandWeapon weapon = Type.tryCastTo(attachedItem.getItem(), HandWeapon.class);
                    float bloodLevel = weapon == null ? 0.0F : weapon.getBloodLevel();
                    AttachedModelName amn = new AttachedModelName(attachmentName, modelName, bloodLevel);
                    this.models.add(amn);
                    if (weapon != null) {
                        ArrayList<ModelWeaponPart> modelWeaponParts = weapon.getModelWeaponPart();
                        if (modelWeaponParts != null) {
                            List<WeaponPart> weaponParts = weapon.getAllWeaponParts();

                            for (int j = 0; j < weaponParts.size(); j++) {
                                WeaponPart part = weaponParts.get(j);

                                for (int k = 0; k < modelWeaponParts.size(); k++) {
                                    ModelWeaponPart mwp = modelWeaponParts.get(k);
                                    if (part.getFullType().equals(mwp.partType)) {
                                        AttachedModelName amn2 = new AttachedModelName(mwp.attachmentNameSelf, mwp.attachmentParent, mwp.modelName, 0.0F);
                                        amn.addChild(amn2);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int size() {
        return this.models.size();
    }

    public AttachedModelName get(int index) {
        return this.models.get(index);
    }

    public void clear() {
        this.models.clear();
    }
}
