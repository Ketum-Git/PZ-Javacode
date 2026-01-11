// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.VehicleScript;

@UsedFromLua
public final class VehicleWindow {
    protected VehiclePart part;
    protected boolean openable;
    protected boolean open;
    private float openDelta;

    public VehicleWindow(VehiclePart part) {
        this.part = part;
    }

    public void init(VehicleScript.Window scriptWindow) {
        this.openable = scriptWindow.openable;
        this.open = false;
    }

    public int getHealth() {
        return this.part.getCondition();
    }

    public boolean isDestroyed() {
        return this.getHealth() == 0;
    }

    public boolean isOpenable() {
        return this.openable;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        this.part.getVehicle().doDamageOverlay = true;
    }

    public void setOpenDelta(float delta) {
        this.openDelta = delta;
    }

    public float getOpenDelta() {
        return this.openDelta;
    }

    public VehiclePart getPart() {
        return this.part;
    }

    public boolean isHittable() {
        if (this.isDestroyed()) {
            return false;
        } else {
            return this.isOpen() ? false : this.part.getItemType() == null || this.part.getInventoryItem() != null;
        }
    }

    public void hit(IsoGameCharacter chr) {
        this.damage(this.getHealth());
        this.part.setCondition(0);
    }

    public void damage(int amount) {
        if (amount > 0) {
            if (this.isHittable()) {
                if (GameClient.client) {
                    GameClient.instance
                        .sendClientCommandV(null, "vehicle", "damageWindow", "vehicle", this.part.vehicle.getId(), "part", this.part.getId(), "amount", amount);
                } else {
                    if (this.part.getVehicle().isAlarmed()) {
                        this.part.getVehicle().triggerAlarm();
                    }

                    this.part.setCondition(this.part.getCondition() - amount);
                    if (this.isDestroyed()) {
                        if (this.part.getInventoryItem() != null) {
                            this.part.setInventoryItem(null);
                            this.part.getVehicle().transmitPartItem(this.part);
                        }

                        IsoGridSquare square = this.part.vehicle.square;
                        if (GameServer.server) {
                            GameServer.PlayWorldSoundServer("VehicleWindowSmash", false, square, 0.2F, 20.0F, 1.1F, true);
                        } else {
                            SoundManager.instance.PlayWorldSound("VehicleWindowSmash", square, 0.2F, 20.0F, 1.0F, true);
                        }

                        this.part.getVehicle().getSquare().addBrokenGlass();
                        WorldSoundManager.instance.addSound(null, square.getX(), square.getY(), square.getZ(), 10, 20, true, 4.0F, 15.0F);
                    }

                    this.part.getVehicle().transmitPartWindow(this.part);
                }
            }
        }
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)this.part.getCondition());
        output.put((byte)(this.open ? 1 : 0));
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.part.setCondition(input.get());
        this.open = input.get() == 1;
        this.openDelta = this.open ? 1.0F : 0.0F;
    }
}
