// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TriggerXmlFile
 *   A serialized representation of a Trigger_SetAnimState.xml file.
 *   Used by AnimZed to message a character to change its outfit.
 */
@XmlRootElement(name = "animStateTriggerXmlFile")
public final class AnimStateTriggerXmlFile {
    @XmlElement(name = "forceAnim")
    public boolean forceAnim;
    @XmlElement(name = "animSet")
    public String animSet;
    @XmlElement(name = "stateName")
    public String stateName;
    @XmlElement(name = "nodeName")
    public String nodeName;
    @XmlElement(name = "setScalarValues")
    public boolean setScalarValues;
    @XmlElement(name = "scalarValue")
    public String scalarValue;
    @XmlElement(name = "scalarValue2")
    public String scalarValue2;
}
