// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.combat;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.raknet.UdpConnection;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.iso.enums.MaterialType;
import zombie.scripting.entity.ComponentScript;
import zombie.ui.ObjectTooltip;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public class Durability extends Component {
    private float maxHitPoints = 1.0F;
    private float currentHitPoints = 1.0F;
    private MaterialType material = MaterialType.Default;

    private Durability() {
        super(ComponentType.Durability);
    }

    private Durability(ComponentType componentType) {
        super(componentType);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        DurabilityScript script = (DurabilityScript)componentScript;
        this.currentHitPoints = script.getCurrentHitPoints();
        this.maxHitPoints = script.getMaxHitPoints();
        this.material = script.getMaterial();
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 240);
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Short, output);
        if (this.currentHitPoints != 1.0F) {
            header.addFlags(1);
            output.putFloat(this.currentHitPoints);
        }

        if (this.maxHitPoints != 1.0F) {
            header.addFlags(2);
            output.putFloat(this.maxHitPoints);
        }

        if (this.material != MaterialType.Default) {
            header.addFlags(4);
            GameWindow.WriteString(output, this.material.toString());
        }

        header.write();
        header.release();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.currentHitPoints = 1.0F;
        this.maxHitPoints = 1.0F;
        this.material = MaterialType.Default;
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Short, input);
        if (header.hasFlags(1)) {
            this.currentHitPoints = input.getFloat();
        }

        if (header.hasFlags(2)) {
            this.maxHitPoints = input.getFloat();
        }

        if (header.hasFlags(4)) {
            this.material = MaterialType.valueOf(GameWindow.ReadString(input));
        }
    }

    public MaterialType getMaterial() {
        return this.material;
    }

    public void setMaterial(MaterialType material) {
        this.material = material;
    }

    public float getCurrentHitPoints() {
        return this.currentHitPoints;
    }

    public void setCurrentHitPoints(float hitPoints) {
        this.currentHitPoints = hitPoints;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel("Material:", 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue(this.material.name(), 1.0F, 1.0F, 1.0F, 1.0F);
            item = layout.addItem();
            item.setLabel("Hit Points:", 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue(String.valueOf(this.currentHitPoints), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
