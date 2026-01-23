// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.WornItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zombie.UsedFromLua;

@UsedFromLua
public final class BodyLocations {
    private static final List<BodyLocationGroup> groups = new ArrayList<>();

    public static BodyLocationGroup getGroup(String id) {
        for (BodyLocationGroup group : groups) {
            if (group.getId().equals(id)) {
                return group;
            }
        }

        BodyLocationGroup newGroup = new BodyLocationGroup(id);
        groups.add(newGroup);
        return newGroup;
    }

    public static void reset() {
        groups.clear();
    }

    public static List<BodyLocationGroup> getAllGroups() {
        return Collections.unmodifiableList(groups);
    }
}
