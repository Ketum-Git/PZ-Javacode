// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.sounds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.sound.CraftBenchSoundsScript;
import zombie.util.StringUtils;

@UsedFromLua
public class CraftBenchSounds extends Component {
    private static final int MATCH_ANY = -1;
    private final ArrayList<CraftBenchSound> sounds = new ArrayList<>();

    protected CraftBenchSounds() {
        super(ComponentType.CraftBenchSounds);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        CraftBenchSoundsScript script = (CraftBenchSoundsScript)componentScript;
        this.sounds.clear();
        this.sounds.addAll(script.getSounds());
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
    }

    private int indexOf(String id, String param1) {
        if (this.sounds.isEmpty()) {
            GameEntityScript script = this.getOwner().getEntityScript();
            if (script != null) {
                CraftBenchSoundsScript craftBenchSounds = script.getComponentScriptFor(ComponentType.CraftBenchSounds);
                if (craftBenchSounds != null) {
                    this.readFromScript(craftBenchSounds);
                }
            }
        }

        for (int i = 0; i < this.sounds.size(); i++) {
            CraftBenchSound sound = this.sounds.get(i);
            if (sound.id.equalsIgnoreCase(id) && StringUtils.equalsIgnoreCase(sound.param1, param1)) {
                return i;
            }
        }

        return -1;
    }

    public String getSoundName(String id, String param1) {
        int index = this.indexOf(id, param1);
        return index == -1 ? null : this.sounds.get(index).gameSound;
    }
}
