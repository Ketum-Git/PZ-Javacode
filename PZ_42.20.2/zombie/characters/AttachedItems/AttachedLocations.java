// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.AttachedItems;

import java.util.ArrayList;
import zombie.UsedFromLua;

@UsedFromLua
public final class AttachedLocations {
    protected static final ArrayList<AttachedLocationGroup> groups = new ArrayList<>();

    public static AttachedLocationGroup getGroup(String id) {
        for (int i = 0; i < groups.size(); i++) {
            AttachedLocationGroup group = groups.get(i);
            if (group.id.equals(id)) {
                return group;
            }
        }

        AttachedLocationGroup group = new AttachedLocationGroup(id);
        groups.add(group);
        return group;
    }

    public static void Reset() {
        groups.clear();
    }
}
