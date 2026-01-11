// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class CustomSandboxOptions {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;
    public static final CustomSandboxOptions instance = new CustomSandboxOptions();
    private final ArrayList<CustomSandboxOption> options = new ArrayList<>();

    public void init() {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

        for (int i = 0; i < modIDs.size(); i++) {
            String modID = modIDs.get(i);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.getVersionDir() + File.separator + "media" + File.separator + "sandbox-options.txt");
                if (file.exists() && !file.isDirectory()) {
                    this.readFile(file.getAbsolutePath());
                } else {
                    file = new File(mod.getCommonDir() + File.separator + "media" + File.separator + "sandbox-options.txt");
                    if (file.exists() && !file.isDirectory()) {
                        this.readFile(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public static void Reset() {
        instance.options.clear();
    }

    public void initInstance(SandboxOptions options) {
        for (int i = 0; i < this.options.size(); i++) {
            CustomSandboxOption option = this.options.get(i);
            options.newCustomOption(option);
        }
    }

    private boolean readFile(String path) {
        try {
            boolean var6;
            try (
                FileReader fr = new FileReader(path);
                BufferedReader br = new BufferedReader(fr);
            ) {
                StringBuilder stringBuilder = new StringBuilder();

                for (String str = br.readLine(); str != null; str = br.readLine()) {
                    stringBuilder.append(str);
                }

                this.parse(stringBuilder.toString());
                var6 = true;
            }

            return var6;
        } catch (FileNotFoundException var11) {
            return false;
        } catch (Exception var12) {
            ExceptionLogger.logException(var12);
            return false;
        }
    }

    private void parse(String contents) {
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }

        if (version >= 1 && version <= 1) {
            for (ScriptParser.Block block1 : block.children) {
                if (!block1.type.equalsIgnoreCase("option")) {
                    throw new RuntimeException("unknown block type \"" + block1.type + "\"");
                }

                CustomSandboxOption option = this.parseOption(block1);
                if (option == null) {
                    DebugLog.General.warn("failed to parse custom sandbox option \"%s\"", block1.id);
                } else {
                    this.options.add(option);
                }
            }
        } else {
            throw new RuntimeException("invalid or missing VERSION");
        }
    }

    private CustomSandboxOption parseOption(ScriptParser.Block block) {
        if (StringUtils.isNullOrWhitespace(block.id)) {
            DebugLog.General.warn("missing or empty option id");
            return null;
        } else {
            ScriptParser.Value type = block.getValue("type");
            if (type != null && !StringUtils.isNullOrWhitespace(type.getValue())) {
                String var3 = type.getValue().trim();
                switch (var3) {
                    case "boolean":
                        return CustomBooleanSandboxOption.parse(block);
                    case "double":
                        return CustomDoubleSandboxOption.parse(block);
                    case "enum":
                        return CustomEnumSandboxOption.parse(block);
                    case "integer":
                        return CustomIntegerSandboxOption.parse(block);
                    case "string":
                        return CustomStringSandboxOption.parse(block);
                    default:
                        DebugLog.General.warn("unknown option type \"%s\"", type.getValue().trim());
                        return null;
                }
            } else {
                DebugLog.General.warn("missing or empty value \"type\"");
                return null;
            }
        }
    }
}
