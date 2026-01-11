// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.File;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.profanity.ProfanityFilter;
import zombie.util.StringUtils;

@UsedFromLua
public class ServerSettings {
    protected String name;
    protected ServerOptions serverOptions;
    protected SandboxOptions sandboxOptions;
    protected ArrayList<SpawnRegions.Region> spawnRegions;
    protected ArrayList<SpawnRegions.Profession> spawnPoints;
    private boolean valid = true;
    private String errorMsg = null;

    public ServerSettings(String name) {
        this.valid = true;
        this.name = name;
        String error = ProfanityFilter.getInstance().validateString(name, true, true, true);
        if (!StringUtils.isNullOrEmpty(error)) {
            this.errorMsg = Translator.getText("UI_BadWordCheck", error);
            this.valid = false;
        }
    }

    public String getName() {
        return this.name;
    }

    public void resetToDefault() {
        this.serverOptions = new ServerOptions();
        this.sandboxOptions = new SandboxOptions();
        this.spawnRegions = new SpawnRegions().getDefaultServerRegions();
        this.spawnPoints = null;
    }

    public boolean loadFiles() {
        this.serverOptions = new ServerOptions();
        this.serverOptions.loadServerTextFile(this.name);
        this.sandboxOptions = new SandboxOptions();
        this.sandboxOptions.loadServerLuaFile(this.name);
        this.sandboxOptions.loadServerZombiesFile(this.name);
        SpawnRegions sr = new SpawnRegions();
        this.spawnRegions = sr.loadRegionsFile(ServerSettingsManager.instance.getNameInSettingsFolder(this.name + "_spawnregions.lua"));
        if (this.spawnRegions == null) {
            this.spawnRegions = sr.getDefaultServerRegions();
        }

        this.spawnPoints = sr.loadPointsFile(ServerSettingsManager.instance.getNameInSettingsFolder(this.name + "_spawnpoints.lua"));
        return true;
    }

    public boolean saveFiles() {
        if (this.serverOptions == null) {
            return false;
        } else {
            this.serverOptions.saveServerTextFile(this.name);
            this.sandboxOptions.saveServerLuaFile(this.name);
            if (this.spawnRegions != null) {
                new SpawnRegions().saveRegionsFile(ServerSettingsManager.instance.getNameInSettingsFolder(this.name + "_spawnregions.lua"), this.spawnRegions);
            }

            if (this.spawnPoints != null) {
                new SpawnRegions().savePointsFile(ServerSettingsManager.instance.getNameInSettingsFolder(this.name + "_spawnpoints.lua"), this.spawnPoints);
            }

            this.tryDeleteFile(this.name + "_zombies.ini");
            return true;
        }
    }

    private boolean tryDeleteFile(String fileName) {
        try {
            File file = new File(ServerSettingsManager.instance.getNameInSettingsFolder(fileName));
            if (file.exists()) {
                DebugLog.DetailedInfo.trace("deleting " + file.getAbsolutePath());
                file.delete();
            }

            return true;
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
            return false;
        }
    }

    public boolean deleteFiles() {
        this.tryDeleteFile(this.name + ".ini");
        this.tryDeleteFile(this.name + "_SandboxVars.lua");
        this.tryDeleteFile(this.name + "_spawnregions.lua");
        this.tryDeleteFile(this.name + "_spawnpoints.lua");
        this.tryDeleteFile(this.name + "_zombies.ini");
        return true;
    }

    public boolean duplicateFiles(String newName) {
        if (!ServerSettingsManager.instance.isValidNewName(newName)) {
            return false;
        } else {
            ServerSettings copy = new ServerSettings(this.name);
            copy.loadFiles();
            if (copy.spawnRegions != null) {
                for (SpawnRegions.Region region : copy.spawnRegions) {
                    if (region.serverfile != null && region.serverfile.equals(this.name + "_spawnpoints.lua")) {
                        region.serverfile = newName + "_spawnpoints.lua";
                    }
                }
            }

            copy.name = newName;
            copy.saveFiles();
            return true;
        }
    }

    public boolean rename(String newName) {
        if (!ServerSettingsManager.instance.isValidNewName(newName)) {
            return false;
        } else {
            this.loadFiles();
            this.deleteFiles();
            if (this.spawnRegions != null) {
                for (SpawnRegions.Region region : this.spawnRegions) {
                    if (region.serverfile != null && region.serverfile.equals(this.name + "_spawnpoints.lua")) {
                        region.serverfile = newName + "_spawnpoints.lua";
                    }
                }
            }

            this.name = newName;
            this.saveFiles();
            return true;
        }
    }

    public ServerOptions getServerOptions() {
        return this.serverOptions;
    }

    public SandboxOptions getSandboxOptions() {
        return this.sandboxOptions;
    }

    public int getNumSpawnRegions() {
        return this.spawnRegions.size();
    }

    public String getSpawnRegionName(int index) {
        return this.spawnRegions.get(index).name;
    }

    public String getSpawnRegionFile(int index) {
        SpawnRegions.Region region = this.spawnRegions.get(index);
        return region.file != null ? region.file : region.serverfile;
    }

    public void clearSpawnRegions() {
        this.spawnRegions.clear();
    }

    public void addSpawnRegion(String name, String file) {
        if (name != null && file != null) {
            SpawnRegions.Region region = new SpawnRegions.Region();
            region.name = name;
            if (file.startsWith("media")) {
                region.file = file;
            } else {
                region.serverfile = file;
            }

            this.spawnRegions.add(region);
        } else {
            throw new NullPointerException();
        }
    }

    public void removeSpawnRegion(int index) {
        this.spawnRegions.remove(index);
    }

    public KahluaTable loadSpawnPointsFile(String file) {
        SpawnRegions sr = new SpawnRegions();
        return sr.loadPointsTable(ServerSettingsManager.instance.getNameInSettingsFolder(file));
    }

    public boolean saveSpawnPointsFile(String file, KahluaTable professionsTable) {
        SpawnRegions sr = new SpawnRegions();
        return sr.savePointsTable(ServerSettingsManager.instance.getNameInSettingsFolder(file), professionsTable);
    }

    public boolean isValid() {
        return this.valid;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }
}
