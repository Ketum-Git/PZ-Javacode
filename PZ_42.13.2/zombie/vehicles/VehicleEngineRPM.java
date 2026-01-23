// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public class VehicleEngineRPM extends BaseScriptObject {
    public static final int MAX_GEARS = 8;
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;
    private String name;
    public final EngineRPMData[] rpmData = new EngineRPMData[8];

    public VehicleEngineRPM() {
        super(ScriptType.VehicleEngineRPM);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void Load(String name, String totalFile) throws RuntimeException, Exception {
        this.name = name;
        int version = -1;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("VERSION".equals(k)) {
                version = PZMath.tryParseInt(v, -1);
                if (version < 0 || version > 1) {
                    throw new RuntimeException(String.format("unknown vehicleEngineRPM VERSION \"%s\"", v));
                }
            }
        }

        if (version == -1) {
            throw new RuntimeException(String.format("unknown vehicleEngineRPM VERSION \"%s\"", block.type));
        } else {
            int dataIndex = 0;

            for (ScriptParser.Block block1 : block.children) {
                if (!"data".equals(block1.type)) {
                    throw new RuntimeException(String.format("unknown block vehicleEngineRPM.%s", block1.type));
                }

                if (dataIndex == 8) {
                    throw new RuntimeException(String.format("too many vehicleEngineRPM.data blocks, max is %d", 8));
                }

                this.rpmData[dataIndex] = new EngineRPMData();
                this.LoadData(block1, this.rpmData[dataIndex]);
                dataIndex++;
            }
        }
    }

    private void LoadData(ScriptParser.Block block, EngineRPMData rpmData) {
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("afterGearChange".equals(k)) {
                rpmData.afterGearChange = PZMath.tryParseFloat(v, 0.0F);
            } else {
                if (!"gearChange".equals(k)) {
                    throw new RuntimeException(String.format("unknown value vehicleEngineRPM.data.%s", value.string));
                }

                rpmData.gearChange = PZMath.tryParseFloat(v, 0.0F);
            }
        }

        for (ScriptParser.Block block1 : block.children) {
            if (!"xxx".equals(block1.type)) {
                throw new RuntimeException(String.format("unknown block vehicleEngineRPM.data.%s", block1.type));
            }
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < this.rpmData.length; i++) {
            if (this.rpmData[i] != null) {
                this.rpmData[i].reset();
            }
        }
    }
}
