// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.objects.Item;

@UsedFromLua
public class DebugCSVExportFluidContainers {
    public static void doCSV() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("FluidContainersCSV.txt"))) {
            writer.println("CONTAINER ID,CAPACITY,TRANSFER RATE");

            for (Item item : ScriptManager.instance.getAllItems()) {
                if (item.containsComponent(ComponentType.FluidContainer)) {
                    writer.println(
                        "%s.%s,%s,%s"
                            .formatted(
                                item.getModuleName(),
                                item.getName(),
                                item.<FluidContainerScript>getComponentScriptFor(ComponentType.FluidContainer).getCapacity(),
                                item.<FluidContainerScript>getComponentScriptFor(ComponentType.FluidContainer).getTransferRate()
                            )
                    );
                }
            }
        }
    }
}
