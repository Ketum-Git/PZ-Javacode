// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.UsedFromLua;
import zombie.util.StringUtils;

@XmlType(name = "VoiceStyle")
@UsedFromLua
public class VoiceStyle {
    @XmlElement(name = "name")
    public String name = "";
    @XmlElement(name = "prefix")
    public String prefix = "";
    @XmlElement(name = "voiceType")
    public int voiceType;
    @XmlElement(name = "bodyTypeDefault")
    public int bodyTypeDefault = 1;

    public boolean isValid() {
        return !StringUtils.isNullOrWhitespace(this.prefix);
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public int getBodyTypeDefault() {
        return this.bodyTypeDefault;
    }

    public int getVoiceType() {
        return this.voiceType;
    }
}
