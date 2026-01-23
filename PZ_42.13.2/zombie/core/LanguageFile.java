// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class LanguageFile {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;

    public boolean read(String path, LanguageFileData data) {
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

                this.fromString(stringBuilder.toString(), data);
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

    private void fromString(String contents, LanguageFileData data) {
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }

        if (version >= 1 && version <= 1) {
            ScriptParser.Value text = block.getValue("text");
            if (text != null && !StringUtils.isNullOrWhitespace(text.getValue())) {
                ScriptParser.Value charset = block.getValue("charset");
                if (charset != null && !StringUtils.isNullOrWhitespace(charset.getValue())) {
                    data.text = text.getValue().trim();
                    data.charset = charset.getValue().trim();
                    ScriptParser.Value base = block.getValue("base");
                    if (base != null && !StringUtils.isNullOrWhitespace(base.getValue())) {
                        data.base = base.getValue().trim();
                    }

                    ScriptParser.Value azerty = block.getValue("azerty");
                    if (azerty != null) {
                        data.azerty = StringUtils.tryParseBoolean(azerty.getValue());
                    }
                } else {
                    throw new RuntimeException("missing or empty value \"charset\"");
                }
            } else {
                throw new RuntimeException("missing or empty value \"text\"");
            }
        } else {
            throw new RuntimeException("invalid or missing VERSION");
        }
    }
}
