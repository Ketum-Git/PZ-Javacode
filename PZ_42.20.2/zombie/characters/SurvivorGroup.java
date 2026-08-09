// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import zombie.iso.BuildingDef;

public final class SurvivorGroup {
    public final ArrayList<SurvivorDesc> members = new ArrayList<>();
    public String order;
    public BuildingDef safehouse;

    public void addMember(SurvivorDesc member) {
    }

    public void removeMember(SurvivorDesc member) {
    }

    public SurvivorDesc getLeader() {
        return null;
    }

    public boolean isLeader(SurvivorDesc member) {
        return false;
    }
}
