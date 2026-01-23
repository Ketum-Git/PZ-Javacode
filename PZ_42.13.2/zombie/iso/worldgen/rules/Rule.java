// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Rule(String label, int bitmap, int[] color, List<String> tiles, String layer, int[] condition) {
    public static Rule load(BufferedReader br, String[] split_) throws IOException {
        String label = "";
        int bitmap = 0;
        int[] color = new int[]{-1, -1, -1};
        List<String> tiles = new ArrayList<>();
        String layer = "";
        int[] condition = new int[]{-1, -1, -1};

        label65:
        while (true) {
            String line = br.readLine();
            if (line == null || line.equals("}")) {
                return new Rule(label, bitmap, color, tiles, layer, condition);
            }

            line = line.strip();
            if (!line.isEmpty() && !line.equals("{")) {
                String[] split = line.split("\\h+");
                String var10 = split[0];
                switch (var10) {
                    case "label":
                        label = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                        break;
                    case "bitmap":
                        bitmap = Integer.parseInt(split[2]);
                        break;
                    case "color":
                        color = new int[]{Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4])};
                        break;
                    case "tiles":
                        if (split[2].equals("[")) {
                            while (true) {
                                String l = br.readLine();
                                if (l == null || l.strip().equals("]")) {
                                    continue label65;
                                }

                                tiles.add(l.strip());
                            }
                        }

                        tiles.add(split[2]);
                        break;
                    case "layer":
                        layer = split[2];
                        break;
                    case "condition":
                        condition = new int[]{Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4])};
                }
            }
        }
    }
}
