// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.util.StringUtils;

public final class ParameterSinkType extends FMODLocalParameter {
    public ParameterSinkType() {
        super("SinkType");
    }

    public static enum SinkType {
        GENERIC("Generic", 0),
        CERAMIC("Ceramic", 1),
        METAL("Metal", 2);

        private final String name;
        private final int value;

        private SinkType(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return this.name;
        }

        public int getValue() {
            return this.value;
        }

        public static ParameterSinkType.SinkType fromString(String name, ParameterSinkType.SinkType defaultValue) {
            return StringUtils.tryParseEnum(ParameterSinkType.SinkType.class, name, (e, s) -> e.getName().equals(s), defaultValue);
        }
    }
}
