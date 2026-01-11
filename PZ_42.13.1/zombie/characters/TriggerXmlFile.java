// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TriggerXmlFile
 *   A serialized representation of a Trigger_SetClothing.xml file.
 *   Used by AnimZed to message a character to change its outfit.
 */
@XmlRootElement(name = "triggerXmlFile")
public final class TriggerXmlFile {
    @XmlElement(name = "outfitName")
    public String outfitName;
    @XmlElement(name = "clothingItemGUID")
    public String clothingItemGuid;
    @XmlElement(name = "isMale")
    public boolean isMale;
}
