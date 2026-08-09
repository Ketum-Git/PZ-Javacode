// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * class AnimEvent
 *   Used to set a game variable from an animation node.
 * 
 *    eg. Set a sword's collision box to Active during a swing animation,
 *     then Inactive once swing is done.
 * 
 *    Holds a time, name, and value
 *     The time is measured as a fraction of the animation's time.
 *     This means that scaling an animation's speed scales the Events as well.
 */
@XmlType(name = "AnimEvent")
public class AnimEvent {
    @XmlElement(name = "m_EventName")
    public String eventName;
    @XmlElement(name = "m_Time")
    public AnimEvent.AnimEventTime time = AnimEvent.AnimEventTime.PERCENTAGE;
    @XmlElement(name = "m_TimePc")
    public float timePc;
    @XmlElement(name = "m_ParameterValue")
    public String parameterValue;
    @XmlTransient
    public AnimNode parentAnimNode;

    public AnimEvent() {
    }

    public AnimEvent(AnimEvent src) {
        this.eventName = src.eventName;
        this.time = src.time;
        this.timePc = src.timePc;
        this.parameterValue = src.parameterValue;
    }

    @Override
    public String toString() {
        return String.format("%s { %s }", this.getClass().getName(), this.toDetailsString());
    }

    public String toDetailsString() {
        return String.format(
            "Details: %s %s, time: %s",
            this.eventName,
            this.parameterValue,
            this.time == AnimEvent.AnimEventTime.PERCENTAGE ? Float.toString(this.timePc) : this.time.name()
        );
    }

    @XmlEnum
    @XmlType(name = "AnimEventTime")
    public static enum AnimEventTime {
        @XmlEnumValue("Percentage")
        PERCENTAGE,
        @XmlEnumValue("Start")
        START,
        @XmlEnumValue("End")
        END;
    }
}
