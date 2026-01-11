// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public class ParameterBroadcastVoiceType extends FMODLocalParameter {
    private ParameterBroadcastVoiceType.BroadcastVoiceType voiceType = ParameterBroadcastVoiceType.BroadcastVoiceType.Male;

    public ParameterBroadcastVoiceType() {
        super("BroadcastVoiceType");
    }

    @Override
    public float calculateCurrentValue() {
        return this.voiceType.label;
    }

    public void setValue(ParameterBroadcastVoiceType.BroadcastVoiceType voiceType) {
        this.voiceType = voiceType;
    }

    public static enum BroadcastVoiceType {
        Male(0),
        Female(1);

        final int label;

        private BroadcastVoiceType(final int label) {
            this.label = label;
        }

        public int getValue() {
            return this.label;
        }
    }
}
