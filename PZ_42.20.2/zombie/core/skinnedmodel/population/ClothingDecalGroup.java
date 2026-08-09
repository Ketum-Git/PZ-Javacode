// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.util.list.PZArrayUtil;

@XmlType(name = "ClothingDecalGroup")
public class ClothingDecalGroup {
    @XmlElement(name = "name")
    public String name;
    @XmlElement(name = "decal")
    public final ArrayList<String> decals = new ArrayList<>();
    @XmlElement(name = "group")
    public final ArrayList<String> groups = new ArrayList<>();
    private final ArrayList<String> tempDecals = new ArrayList<>();

    public String getRandomDecal() {
        this.tempDecals.clear();
        this.getDecals(this.tempDecals);
        String decal = OutfitRNG.pickRandom(this.tempDecals);
        return decal == null ? null : decal;
    }

    public void getDecals(ArrayList<String> decals) {
        PZArrayUtil.addAll(decals, this.decals);

        for (int i = 0; i < this.groups.size(); i++) {
            ClothingDecalGroup group = ClothingDecals.instance.FindGroup(this.groups.get(i));
            if (group != null) {
                group.getDecals(decals);
            }
        }
    }
}
