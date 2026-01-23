// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TriggerXmlFile
 *   A serialized representation of a Trigger_SetClothing.xml file.
 *   Used by AnimZed to message a character to change its outfit.
 */
@XmlRootElement(name = "triggerSetAnimationRecorderFile")
public final class TriggerSetAnimationRecorderFile {
    @XmlElement(name = "isRecording")
    public boolean isRecording;
    @XmlElement(name = "discard")
    public boolean discard;
}
