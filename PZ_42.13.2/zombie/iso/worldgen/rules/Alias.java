// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Alias(String name, List<String> tiles) {
    public static Alias load(BufferedReader br, String[] split_) throws IOException {
        String name = "";
        List<String> tiles = new ArrayList<>();

        while (true) {
            String line = br.readLine();
            if (line == null || line.equals("}")) {
                return new Alias(name, tiles);
            }

            line = line.strip();
            if (!line.isEmpty() && !line.equals("{")) {
                String[] split = line.split("\\h+");
                String var6 = split[0];
                switch (var6) {
                    case "name":
                        name = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                        break;
                    case "tiles":
                        if (split[2].equals("[")) {
                            while (true) {
                                String l = br.readLine();
                                if (l == null || l.strip().equals("]")) {
                                    break;
                                }

                                tiles.add(l.strip());
                            }
                        } else {
                            tiles.add(split[2]);
                        }
                }
            }
        }
    }
}
