// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.tools;

import java.io.FileWriter;
import java.io.IOException;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.network.PacketTypes;

public class WiresharkPluginUpdater {
    public static void main(String[] args) {
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        PacketTypes.PacketType[] values = PacketTypes.PacketType.values();
        StringBuilder content = new StringBuilder();
        content.append("pzPacketType = {\n");

        for (int i = 0; i < values.length; i++) {
            content.append(String.format("\t[%d] = \"%s\",\n", i, values[i].name()));
        }

        content.append("}\n");

        try (FileWriter writer = new FileWriter("packet_types.txt")) {
            writer.write(content.toString());
            System.out.println("Successfully wrote to file.");
        } catch (IOException var8) {
            System.out.println("An error occurred.");
            var8.printStackTrace();
        }
    }
}
