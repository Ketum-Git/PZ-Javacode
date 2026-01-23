// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.UsedFromLua;
import zombie.util.StringUtils;

@XmlType(name = "BeardStyle")
@UsedFromLua
public class BeardStyle {
    @XmlElement(name = "name")
    public String name = "";
    @XmlElement(name = "model")
    public String model;
    @XmlElement(name = "texture")
    public String texture = "F_Hair_White";
    @XmlElement(name = "level")
    public int level;
    @XmlElement(name = "trimChoices")
    public ArrayList<String> trimChoices = new ArrayList<>();
    @XmlElement(name = "growReference")
    public boolean growReference;

    public boolean isValid() {
        return !StringUtils.isNullOrWhitespace(this.model) && !StringUtils.isNullOrWhitespace(this.texture);
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
}
