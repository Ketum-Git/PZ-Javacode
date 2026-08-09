// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.profiling;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "triggerGameProfilerFile")
public class TriggerGameProfilerFile {
    @XmlElement(name = "discard")
    public boolean discard;
    @XmlElement(name = "isRecording")
    public boolean isRecording;
}
