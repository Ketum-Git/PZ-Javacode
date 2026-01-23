// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.fields.INetworkPacketField;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartModels extends VehicleField implements INetworkPacketField {
    private static final ArrayList<BaseVehicle.ModelInfo> oldModels = new ArrayList<>();
    private static final ArrayList<BaseVehicle.ModelInfo> curModels = new ArrayList<>();

    public VehiclePartModels(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            oldModels.clear();
            oldModels.addAll(this.getVehicle().models);
            curModels.clear();
            int numModels = bb.get();

            for (int i = 0; i < numModels; i++) {
                int partIndex = bb.get();
                int modelIndex = bb.get();
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                VehicleScript.Model model = part.getScriptPart().models.get(modelIndex);
                BaseVehicle.ModelInfo modelInfo = this.getVehicle().setModelVisible(part, model, true);
                curModels.add(modelInfo);
            }

            for (int i = 0; i < oldModels.size(); i++) {
                BaseVehicle.ModelInfo modelInfo = oldModels.get(i);
                if (!curModels.contains(modelInfo)) {
                    this.getVehicle().setModelVisible(modelInfo.part, modelInfo.scriptModel, false);
                }
            }

            this.getVehicle().doDamageOverlay();
        } catch (Exception var10) {
            DebugLog.Multiplayer.printException(var10, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.bb.put((byte)this.getVehicle().models.size());

            for (int j = 0; j < this.getVehicle().models.size(); j++) {
                BaseVehicle.ModelInfo modelInfo = this.getVehicle().models.get(j);
                b.bb.put((byte)modelInfo.part.getIndex());
                b.bb.put((byte)modelInfo.part.getScriptPart().models.indexOf(modelInfo.scriptModel));
            }
        } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
