// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.scripting;

import zombie.UsedFromLua;
import zombie.radio.ChannelCategory;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class DynamicRadioChannel extends RadioChannel {
    public DynamicRadioChannel(String n, int freq, ChannelCategory c) {
        super(n, freq, c);
    }

    public DynamicRadioChannel(String n, int freq, ChannelCategory c, String guid) {
        super(n, freq, c, guid);
    }

    @Override
    public void LoadAiringBroadcast(String guid, int line) {
    }
}
