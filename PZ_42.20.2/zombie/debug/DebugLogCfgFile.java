// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.ZomboidFileSystem;
import zombie.util.StringUtils;

public final class DebugLogCfgFile {
    private final List<DebugLogProfile> profiles = new ArrayList<>();
    private final Map<String, DebugLogProfile> profileMap = new HashMap<>();
    private final List<String> aliases = new ArrayList<>();
    private final Map<String, String> aliasMap = new HashMap<>();
    private String selectedProfile;

    public void clear() {
        this.profiles.clear();
        this.profileMap.clear();
        this.aliases.clear();
        this.aliasMap.clear();
        this.selectedProfile = null;
    }

    public void setDefault() {
        this.clear();
        DebugLogProfile profile = this.createProfile("default");
        profile.addCommand("-all");

        for (DebugType type : DebugType.values()) {
            if (type.getLogSeverity() != LogSeverity.Off) {
                profile.addCommand("+%s %s".formatted(type.name(), type.getLogSeverity().name()));
            }
        }

        this.selectedProfile = "default";
    }

    public void read(String filePath) throws IOException {
        this.clear();

        try (
            FileReader fr = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fr);
        ) {
            this.readInternal(br);
        }
    }

    private void readInternal(BufferedReader br) throws IOException {
        boolean opened = false;
        String line = null;
        DebugLogProfile profile = null;

        String l;
        while ((l = br.readLine()) != null) {
            String lastLine = line;
            line = l.trim();
            if (!line.startsWith("//") && !line.startsWith("#") && !StringUtils.isNullOrWhitespace(line)) {
                if (line.startsWith("=")) {
                    this.selectedProfile = line.substring(1).trim();
                } else if (line.startsWith("$")) {
                    try {
                        String s = line.substring(1).trim();
                        int i = s.indexOf(61);
                        String alias = s.substring(0, i).trim();
                        String command = s.substring(i + 1).trim();
                        this.aliases.add(alias);
                        this.aliasMap.put(alias, command);
                    } catch (Exception var11) {
                        DebugType.General.printException(var11, LogSeverity.Error);
                    }
                } else if (!opened && line.startsWith("{") && lastLine != null) {
                    opened = true;
                    profile = this.createProfile(lastLine);
                } else if (opened) {
                    if (line.startsWith("}")) {
                        opened = false;
                    } else {
                        profile.addCommand(line);
                    }
                }
            }
        }
    }

    public void write(String filePath) throws IOException {
        if (filePath.endsWith(".cfg") && !StringUtils.containsDoubleDot(filePath) && filePath.startsWith(ZomboidFileSystem.instance.getCacheDir())) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                for (DebugLogProfile profile : this.profiles) {
                    profile.write(bw);
                    bw.newLine();
                }

                for (String alias : this.aliases) {
                    bw.write("$%s = %s".formatted(alias, this.aliasMap.get(alias)));
                    bw.newLine();
                }

                if (this.getSelectedProfile() != null) {
                    if (!this.aliases.isEmpty()) {
                        bw.newLine();
                    }

                    bw.write("=%s".formatted(this.getSelectedProfile()));
                    bw.newLine();
                }
            }
        }
    }

    public DebugLogProfile createProfile(String profileName) {
        DebugLogProfile profile = new DebugLogProfile(profileName);
        this.profiles.add(profile);
        this.profileMap.put(profileName, profile);
        return profile;
    }

    public List<DebugLogProfile> getProfiles() {
        return this.profiles;
    }

    public Map<String, DebugLogProfile> getProfileMap() {
        return this.profileMap;
    }

    public DebugLogProfile getProfile(String name) {
        return this.profileMap.get(name);
    }

    public String getSelectedProfile() {
        return this.selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public Map<String, String> getAliasMap() {
        return this.aliasMap;
    }

    public String getAlias(String name) {
        return this.aliasMap.get(name);
    }

    public List<DebugLogProfile> resolveAliasProfiles(String alias) {
        List<DebugLogProfile> profiles = new ArrayList<>();
        String aliasCommand = this.getAlias(alias.substring(1));
        String[] ss = aliasCommand.split("\\+");

        for (String elem : ss) {
            String profileName = elem.trim();
            DebugLogProfile var11 = this.getProfile(profileName);
            if (var11 instanceof DebugLogProfile) {
                profiles.add(var11);
            }
        }

        return profiles;
    }
}
