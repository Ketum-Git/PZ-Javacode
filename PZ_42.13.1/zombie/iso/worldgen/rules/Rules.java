// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.rules;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import zombie.ZomboidFileSystem;

public class Rules {
    private int version;
    private Map<String, Alias> aliases;
    private Map<String, Rule> rules;
    private Map<String, int[]> colors;
    private Map<String, Integer> colorsInt;

    public static Rules load(String filename) {
        File file = ZomboidFileSystem.instance.getMediaFile(filename);
        if (!file.exists()) {
            return null;
        } else {
            Rules rules = new Rules();
            rules.aliases = new HashMap<>();
            rules.rules = new HashMap<>();
            rules.colors = new HashMap<>();
            rules.colorsInt = new HashMap<>();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));

                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        br.close();
                        break;
                    }

                    line = line.strip();
                    if (!line.isEmpty()) {
                        String[] split = line.split("\\h+");
                        String var6 = split[0];
                        switch (var6) {
                            case "version":
                                rules.version = Integer.parseInt(split[2]);
                                break;
                            case "alias":
                                Alias alias = Alias.load(br, split);
                                rules.aliases.put(alias.name(), alias);
                                break;
                            case "rule":
                                Rule rule = Rule.load(br, split);
                                rules.rules.put(rule.label(), rule);
                        }
                    }
                }
            } catch (IOException var12) {
                var12.printStackTrace();
                return null;
            }

            for (Rule rule : rules.rules.values().stream().filter(k -> k.condition()[0] == -1).toList()) {
                for (String tile : rule.tiles()) {
                    for (String subTile : rules.aliases.containsKey(tile) ? rules.aliases.get(tile).tiles() : Lists.newArrayList(tile)) {
                        int[] color = rule.color();
                        rules.colors.put(subTile, color);
                        rules.colorsInt.put(subTile, color[0] << 16 | color[1] << 8 | color[2]);
                    }
                }
            }

            return rules;
        }
    }

    public int getVersion() {
        return this.version;
    }

    public Map<String, Alias> getAliases() {
        return this.aliases;
    }

    public Map<String, Rule> getRules() {
        return this.rules;
    }

    public Map<String, int[]> getColors() {
        return this.colors;
    }

    public Map<String, Integer> getColorsInt() {
        return this.colorsInt;
    }
}
