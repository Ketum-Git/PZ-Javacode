// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlTransient;

public class AnimEventFlagWhileAlive extends AnimEvent {
    @XmlTransient
    public AnimationVariableReference variableReference;
    @XmlTransient
    public boolean flagValue = true;

    public AnimEventFlagWhileAlive(AnimEvent event) {
        super(event);
        String[] ss = event.parameterValue.split("=");
        if (ss.length == 2) {
            this.variableReference = AnimationVariableReference.fromRawVariableName(ss[0]);
            this.flagValue = Boolean.parseBoolean(ss[1]);
        } else {
            this.variableReference = AnimationVariableReference.fromRawVariableName(event.parameterValue);
            this.flagValue = true;
        }
    }
}
