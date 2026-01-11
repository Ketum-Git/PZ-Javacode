// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.devices;

import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatManager;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWaveSignal;
import zombie.radio.ZomboidRadio;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public interface WaveSignalDevice {
    DeviceData getDeviceData();

    void setDeviceData(DeviceData data);

    float getDelta();

    void setDelta(float d);

    IsoGridSquare getSquare();

    float getX();

    float getY();

    float getZ();

    void AddDeviceText(String line, float r, float g, float b, String guid, String codes, int distance);

    boolean HasPlayerInRange();

    default void AddDeviceText(IsoPlayer player, String line, float r, float g, float b, String guid, String codes, int distance) {
        if (this.getDeviceData() != null && this.getDeviceData().getDeviceVolume() > 0.0F) {
            if (!ZomboidRadio.isStaticSound(line)) {
                this.getDeviceData().doReceiveSignal(distance);
            }

            if (player != null && player.isLocalPlayer() && !player.hasTrait(CharacterTrait.DEAF)) {
                if (this.getDeviceData().getParent() instanceof InventoryItem && player.isEquipped((InventoryItem)this.getDeviceData().getParent())) {
                    player.getChatElement()
                        .addChatLine(line, r, g, b, UIFont.Medium, this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, false, false, true);
                } else if (this.getDeviceData().getParent() instanceof IsoWaveSignal) {
                    ((IsoWaveSignal)this.getDeviceData().getParent())
                        .getChatElement()
                        .addChatLine(line, r, g, b, UIFont.Medium, this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, true, true, true);
                } else if (this.getDeviceData().getParent() instanceof VehiclePart) {
                    ((VehiclePart)this.getDeviceData().getParent())
                        .getChatElement()
                        .addChatLine(line, r, g, b, UIFont.Medium, this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, true, true, true);
                }

                if (ZomboidRadio.isStaticSound(line)) {
                    ChatManager.getInstance().showStaticRadioSound(line);
                } else {
                    ChatManager.getInstance().showRadioMessage(line, this.getDeviceData().getChannel());
                }

                if (codes != null) {
                    LuaEventManager.triggerEvent("OnDeviceText", guid, codes, -1, -1, -1, line, this);
                }
            }
        }
    }
}
