// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.HashMap;
import java.util.Map;
import zombie.debug.DebugType;

public class IsoTreeJumbo {
    public static final IsoTreeJumbo.TreeDescription NULL = new IsoTreeJumbo.TreeDescription(null, null, null, null, null, null);
    public static final Map<String, IsoTreeJumbo.TreeDescription> Jumbos = new HashMap<>();

    static {
        IsoTreeJumbo.GenericTree[] trees = new IsoTreeJumbo.GenericTree[]{
            new IsoTreeJumbo.GenericTree("e_redmaple", false),
            new IsoTreeJumbo.GenericTree("e_easternredbud", false),
            new IsoTreeJumbo.GenericTree("e_dogwood", false),
            new IsoTreeJumbo.GenericTree("e_cockspurhawthorn", false),
            new IsoTreeJumbo.GenericTree("e_carolinasilverbell", false),
            new IsoTreeJumbo.GenericTree("e_americanlinden", false),
            new IsoTreeJumbo.GenericTree("e_canadianhemlock", true),
            new IsoTreeJumbo.GenericTree("e_americanholly", true),
            new IsoTreeJumbo.GenericTree("e_yellowwood", false),
            new IsoTreeJumbo.GenericTree("e_virginiapine", true),
            new IsoTreeJumbo.GenericTree("e_riverbirch", false)
        };
        String[] types = new String[]{"JUMBOXL", "JUMBOXXL"};
        String trunksName = "e_stumps";

        for (int itree = 0; itree < trees.length; itree++) {
            String baseName = trees[itree].name();
            int ntrees = trees[itree].keepLeaves() ? 2 : 6;
            int section = itree / 8;
            int column = itree % 8;

            for (String type : types) {
                String advName = baseName + type + "_1_";
                String advTrunksName = "e_stumps_" + type + "_";

                for (int i = 0; i < ntrees; i++) {
                    int snowny = i == 1 ? 1 : 0;

                    int trunk = switch (i) {
                        case 0 -> 0;
                        case 1 -> 1;
                        default -> 2;
                    };
                    Jumbos.put(
                        advName + i,
                        new IsoTreeJumbo.TreeDescription(
                            advName + i,
                            advName + (i + 6),
                            advName + (trunk + 12),
                            advTrunksName + (column + 8 + snowny * 32 + section * 64),
                            advName + "15",
                            advName + "16"
                        )
                    );
                    DebugType.WorldGen.debugln(Jumbos.get(advName + i));
                }
            }
        }
    }

    private record GenericTree(String name, boolean keepLeaves) {
    }

    public record TreeDescription(String main, String treetop, String trunk, String stump, String burned, String burnedFrozen) {
    }
}
