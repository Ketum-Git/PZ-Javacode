// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import generation.ScriptFileGenerator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;

public class DevGameServer {
    public static void main(String[] args) throws Exception {
        Core.IS_DEV = true;
        Registry var1 = Registries.REGISTRY;
        ScriptFileGenerator.main(args);
        GameServer.main(args);
    }
}
