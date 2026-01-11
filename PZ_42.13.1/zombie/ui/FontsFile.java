// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class FontsFile {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;

    public boolean read(String path, HashMap<String, FontsFileFont> fonts) {
        try {
            boolean var7;
            try (
                FileReader fr = new FileReader(path);
                BufferedReader br = new BufferedReader(fr);
            ) {
                StringBuilder stringBuilder = new StringBuilder();

                for (String str = br.readLine(); str != null; str = br.readLine()) {
                    stringBuilder.append(str);
                }

                this.fromString(stringBuilder.toString(), fonts);
                var7 = true;
            }

            return var7;
        } catch (FileNotFoundException var12) {
            return false;
        } catch (Exception var13) {
            ExceptionLogger.logException(var13);
            return false;
        }
    }

    private void fromString(String contents, HashMap<String, FontsFileFont> fonts) {
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }

        if (version >= 1 && version <= 1) {
            for (ScriptParser.Block block1 : block.children) {
                if (!block1.type.equalsIgnoreCase("font")) {
                    throw new RuntimeException("unknown block type \"" + block1.type + "\"");
                }

                if (StringUtils.isNullOrWhitespace(block1.id)) {
                    DebugLog.General.warn("missing or empty font id");
                } else {
                    ScriptParser.Value fnt = block1.getValue("fnt");
                    ScriptParser.Value img = block1.getValue("img");
                    ScriptParser.Value type = block1.getValue("type");
                    if (fnt != null && !StringUtils.isNullOrWhitespace(fnt.getValue())) {
                        FontsFileFont font = new FontsFileFont();
                        font.id = block1.id;
                        font.fnt = fnt.getValue().trim();
                        if (img != null && !StringUtils.isNullOrWhitespace(img.getValue())) {
                            font.img = img.getValue().trim();
                        }

                        if (type != null) {
                            font.type = StringUtils.discardNullOrWhitespace(type.getValue().trim());
                        }

                        fonts.put(font.id, font);
                    } else {
                        DebugLog.General.warn("missing or empty value \"fnt\"");
                    }
                }
            }
        } else {
            throw new RuntimeException("invalid or missing VERSION");
        }
    }
}
