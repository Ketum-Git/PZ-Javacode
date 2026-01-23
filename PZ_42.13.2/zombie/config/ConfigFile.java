// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;

public final class ConfigFile {
    protected ArrayList<ConfigOption> options;
    protected int version;
    protected String versionString = "Version";
    protected boolean writeTooltips = true;

    private void fileError(String fileName, int lineNumber, String message) {
        DebugLog.log(fileName + ":" + lineNumber + " " + message);
    }

    public boolean read(String fileName) {
        this.options = new ArrayList<>();
        this.version = 0;
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        } else {
            DebugLog.DetailedInfo.trace("reading " + fileName);

            try (
                FileReader fr = new FileReader(file);
                BufferedReader r = new BufferedReader(fr);
            ) {
                int lineNumber = 0;

                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        return true;
                    }

                    lineNumber++;
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        if (!line.contains("=")) {
                            this.fileError(fileName, lineNumber, line);
                        } else {
                            String[] ss = line.split("=");
                            if (this.versionString.equals(ss[0])) {
                                try {
                                    this.version = Integer.parseInt(ss[1]);
                                } catch (NumberFormatException var11) {
                                    this.fileError(fileName, lineNumber, "expected version number, got \"" + ss[1] + "\"");
                                }
                            } else {
                                StringConfigOption option = new StringConfigOption(ss[0], ss.length > 1 ? ss[1] : "", -1);
                                this.options.add(option);
                            }
                        }
                    }
                }
            } catch (Exception var14) {
                ExceptionLogger.logException(var14);
                return false;
            }
        }
    }

    public boolean write(String fileName, int version, ArrayList<? extends ConfigOption> options) {
        File file = new File(fileName);
        DebugLog.DetailedInfo.trace("writing " + fileName);

        try {
            try (FileWriter fw = new FileWriter(file, false)) {
                if (version != 0) {
                    fw.write(this.versionString + "=" + version + System.lineSeparator());
                }

                String lineSeparator = System.lineSeparator();
                if (this.writeTooltips) {
                    lineSeparator = lineSeparator + System.lineSeparator();
                }

                for (int i = 0; i < options.size(); i++) {
                    ConfigOption option = options.get(i);
                    if (this.writeTooltips) {
                        String tooltip = option.getTooltip();
                        if (tooltip != null) {
                            tooltip = tooltip.replace("\\n", " ").replace("\\\"", "\"");
                            tooltip = tooltip.replaceAll("\n", System.lineSeparator() + "# ");
                            fw.write("# " + tooltip + System.lineSeparator());
                        }

                        if (option instanceof SandboxOptions.EnumSandboxOption enumOption) {
                            for (int j = 1; j <= enumOption.getNumValues(); j++) {
                                try {
                                    String subOptionTranslated = enumOption.getValueTranslationByIndexOrNull(j);
                                    if (subOptionTranslated != null) {
                                        fw.write("    -- " + j + " = " + subOptionTranslated.replace("\\\"", "\"") + System.lineSeparator());
                                    }
                                } catch (Exception var14) {
                                    ExceptionLogger.logException(var14);
                                }
                            }
                        }
                    }

                    if (option instanceof ArrayConfigOption arrayConfigOption && arrayConfigOption.isMultiLine()) {
                        for (int j = 0; j < arrayConfigOption.size(); j++) {
                            ConfigOption element = arrayConfigOption.getElement(j);
                            fw.write(option.getName() + "=" + element.getValueAsString());
                            if (j < arrayConfigOption.size() - 1 || i < options.size() - 1) {
                                fw.write(lineSeparator);
                            }
                        }
                    } else {
                        fw.write(option.getName() + "=" + option.getValueAsString() + (i < options.size() - 1 ? lineSeparator : ""));
                    }
                }
            }

            return true;
        } catch (Exception var16) {
            ExceptionLogger.logException(var16);
            return false;
        }
    }

    public ArrayList<ConfigOption> getOptions() {
        return this.options;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersionString(String str) {
        this.versionString = str;
    }

    public void setWriteTooltips(boolean bWrite) {
        this.writeTooltips = bWrite;
    }
}
