// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import zombie.core.Core;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class PlayerCheats {
    final EnumSet<CheatType> cheats = EnumSet.noneOf(CheatType.class);

    private void set(CheatType flag) {
        this.cheats.add(flag);
    }

    private void unset(CheatType flag) {
        this.cheats.remove(flag);
    }

    public boolean isSet(CheatType flag) {
        return this.isCheatAllowed() && this.cheats.contains(flag);
    }

    public void clear() {
        this.cheats.clear();
    }

    private boolean isCheatAllowed() {
        return Core.debug || GameClient.client || GameServer.server;
    }

    public void set(CheatType flag, boolean set) {
        if (this.isCheatAllowed()) {
            if (set) {
                this.set(flag);
            } else {
                this.unset(flag);
            }
        }
    }

    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        output.putInt(this.cheats.size());

        for (CheatType cheat : this.cheats) {
            output.put((byte)cheat.ordinal());
        }
    }

    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        int size = input.getInt();

        for (int i = 0; i < size; i++) {
            byte input_get = input.get();
            this.set(CheatType.fromId(input_get));
        }
    }
}
