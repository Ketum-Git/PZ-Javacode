// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import zombie.characters.Capability;
import zombie.network.anticheats.AntiCheat;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketSetting {
    int priority();

    int reliability();

    byte ordering();

    Capability requiredCapability();

    int handlingType();

    AntiCheat[] anticheats() default {AntiCheat.None};

    public static class HandlingType {
        public static final int None = 0;
        public static final int Server = 1;
        public static final int Client = 2;
        public static final int ClientLoading = 4;
        public static final int All = 7;

        static int getType(boolean server, boolean client, boolean clientLoading) {
            int type = 0;
            if (server) {
                type |= 1;
            }

            if (client) {
                type |= 2;
            }

            if (clientLoading) {
                type |= 4;
            }

            return type;
        }
    }
}
