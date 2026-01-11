// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.modding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class ActiveModsFile {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;

    public boolean write(String path, ActiveMods activeMods) {
        if (Core.getInstance().isNoSave()) {
            return false;
        } else {
            File file = new File(path);

            try {
                try (
                    FileWriter fr = new FileWriter(file);
                    BufferedWriter br = new BufferedWriter(fr);
                ) {
                    String contents = this.toString(activeMods);
                    br.write(contents);
                }

                return true;
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
                return false;
            }
        }
    }

    private String toString(ActiveMods activeMods) {
        ScriptParser.Block block = new ScriptParser.Block();
        block.setValue("VERSION", String.valueOf(1));
        ScriptParser.Block blockMods = block.addBlock("mods", null);
        ArrayList<String> mods = activeMods.getMods();

        for (int i = 0; i < mods.size(); i++) {
            blockMods.addValue("mod", mods.get(i));
        }

        ScriptParser.Block blockMaps = block.addBlock("maps", null);
        ArrayList<String> maps = activeMods.getMapOrder();

        for (int i = 0; i < maps.size(); i++) {
            blockMaps.addValue("map", maps.get(i));
        }

        StringBuilder stringBuilder = new StringBuilder();
        String eol = System.lineSeparator();
        block.prettyPrintElements(0, stringBuilder, eol, "    ");
        return stringBuilder.toString();
    }

    public boolean read(String path, ActiveMods activeMods) {
        activeMods.clear();

        try {
            try (
                FileReader fr = new FileReader(path);
                BufferedReader br = new BufferedReader(fr);
            ) {
                StringBuilder stringBuilder = new StringBuilder();

                for (String str = br.readLine(); str != null; str = br.readLine()) {
                    stringBuilder.append(str);
                }

                this.fromString(stringBuilder.toString(), activeMods);
            }

            return true;
        } catch (FileNotFoundException var11) {
            return false;
        } catch (Exception var12) {
            ExceptionLogger.logException(var12);
            return false;
        }
    }

    private void fromString(String contents, ActiveMods activeMods) {
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }

        if (version >= 1 && version <= 1) {
            ScriptParser.Block blockMods = block.getBlock("mods", null);
            if (blockMods != null) {
                for (ScriptParser.Value value1 : blockMods.values) {
                    String key = value1.getKey().trim();
                    if (key.equalsIgnoreCase("mod")) {
                        String modID = value1.getValue().trim();
                        if (!StringUtils.isNullOrWhitespace(modID)) {
                            activeMods.getMods().add(modID);
                        }
                    }
                }
            }

            ScriptParser.Block blockMaps = block.getBlock("maps", null);
            if (blockMaps != null) {
                for (ScriptParser.Value value1x : blockMaps.values) {
                    String key = value1x.getKey().trim();
                    if (key.equalsIgnoreCase("map")) {
                        String map = value1x.getValue().trim();
                        if (!StringUtils.isNullOrWhitespace(map)) {
                            activeMods.getMapOrder().add(map);
                        }
                    }
                }
            }
        }
    }
}
