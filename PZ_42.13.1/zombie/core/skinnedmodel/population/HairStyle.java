// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.UsedFromLua;
import zombie.util.StringUtils;

@XmlType(name = "HairStyle")
@UsedFromLua
public final class HairStyle {
    @XmlElement(name = "name")
    public String name = "";
    @XmlElement(name = "model")
    public String model;
    @XmlElement(name = "texture")
    public String texture = "F_Hair_White";
    @XmlElement(name = "alternate")
    public final ArrayList<HairStyle.Alternate> alternate = new ArrayList<>();
    @XmlElement(name = "level")
    public int level;
    @XmlElement(name = "trimChoices")
    public final ArrayList<String> trimChoices = new ArrayList<>();
    @XmlElement(name = "growReference")
    public boolean growReference;
    @XmlElement(name = "attachedHair")
    public boolean attachedHair;
    @XmlElement(name = "noChoose")
    public boolean noChoose;

    public boolean isValid() {
        return !StringUtils.isNullOrWhitespace(this.model) && !StringUtils.isNullOrWhitespace(this.texture);
    }

    public String getAlternate(String category) {
        for (int i = 0; i < this.alternate.size(); i++) {
            HairStyle.Alternate alternate = this.alternate.get(i);
            if (category.equalsIgnoreCase(alternate.category)) {
                return alternate.style;
            }
        }

        return this.name;
    }

    public int getLevel() {
        return this.level;
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<String> getTrimChoices() {
        return this.trimChoices;
    }

    public boolean isAttachedHair() {
        return this.attachedHair;
    }

    public boolean isGrowReference() {
        return this.growReference;
    }

    public boolean isNoChoose() {
        return this.noChoose;
    }

    @XmlType(name = "Alternate")
    public static final class Alternate {
        @XmlAttribute
        public String category;
        @XmlAttribute
        public String style;
    }
}
