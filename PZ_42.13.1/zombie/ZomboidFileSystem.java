// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream.Filter;
import java.security.AccessControlException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoWorld;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.modding.ActiveMods;
import zombie.modding.ActiveModsFile;
import zombie.network.CoopMaster;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;

public final class ZomboidFileSystem {
    public static final ZomboidFileSystem instance = new ZomboidFileSystem();
    private final ArrayList<String> loadList = new ArrayList<>();
    private final Map<String, String> modIdToDir = new HashMap<>();
    private final Map<String, ChooseGameInfo.Mod> modDirToMod = new HashMap<>();
    private ArrayList<String> modFolders;
    private ArrayList<String> modFoldersOrder;
    public final HashMap<String, String> activeFileMap = new HashMap<>();
    private final HashSet<String> allAbsolutePaths = new HashSet<>();
    public final ZomboidFileSystem.PZFolder base = new ZomboidFileSystem.PZFolder();
    private final ZomboidFileSystem.PZFolder workdir = new ZomboidFileSystem.PZFolder();
    private File localWorkdir;
    private final ZomboidFileSystem.PZFolder anims = new ZomboidFileSystem.PZFolder();
    private final ZomboidFileSystem.PZFolder animsX = new ZomboidFileSystem.PZFolder();
    private final ZomboidFileSystem.PZFolder animSets = new ZomboidFileSystem.PZFolder();
    private final ZomboidFileSystem.PZFolder actiongroups = new ZomboidFileSystem.PZFolder();
    private File cacheDir;
    private final ConcurrentHashMap<String, String> relativeMap = new ConcurrentHashMap<>();
    public final ThreadLocal<Boolean> ignoreActiveFileMap = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final ConcurrentHashMap<String, URI> canonicalUriMap = new ConcurrentHashMap<>();
    private final ArrayList<String> mods = new ArrayList<>();
    private final HashSet<String> loadedPacks = new HashSet<>();
    private FileGuidTable fileGuidTable;
    private boolean fileGuidTableWatcherActive;
    private final PredicatedFileWatcher modFileWatcher = new PredicatedFileWatcher(this::isModFile, this::onModFileChanged);
    private final HashSet<String> watchedModFolders = new HashSet<>();
    private long modsChangedTime;
    private static String startupTimeStamp;
    private static final SimpleDateFormat s_dateTimeSdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    private static final SimpleDateFormat s_dateOnlySdf = new SimpleDateFormat("yyyy-MM-dd");

    private ZomboidFileSystem() {
    }

    public void init() throws IOException {
        this.base.set(new File("./"));
        this.workdir.set(new File(this.base.canonicalFile, "media"));
        this.localWorkdir = this.base.canonicalFile.toPath().relativize(this.workdir.canonicalFile.toPath()).toFile();
        this.anims.set(new File(this.workdir.canonicalFile, "anims"));
        this.animsX.set(new File(this.workdir.canonicalFile, "anims_X"));
        this.animSets.set(new File(this.workdir.canonicalFile, "AnimSets"));
        this.actiongroups.set(new File(this.workdir.canonicalFile, "actiongroups"));
        this.searchFolders(this.workdir.canonicalFile);

        for (int n = 0; n < this.loadList.size(); n++) {
            String rel = this.getRelativeFile(this.loadList.get(n));
            File file = new File(this.loadList.get(n)).getAbsoluteFile();
            String abs = file.getAbsolutePath();
            if (file.isDirectory()) {
                abs = abs + File.separator;
            }

            this.activeFileMap.put(rel.toLowerCase(Locale.ENGLISH), abs);
            this.allAbsolutePaths.add(abs);
        }

        this.loadList.clear();
    }

    public File getCanonicalFile(File parent, String child) {
        if (!parent.isDirectory()) {
            return new File(parent, child);
        } else {
            File[] files = parent.listFiles((dir, name) -> name.equalsIgnoreCase(child));
            return files != null && files.length != 0 ? files[0] : new File(parent, child);
        }
    }

    public String getGameModeCacheDir() {
        if (Core.gameMode == null) {
            Core.getInstance().setGameMode("Sandbox");
        }

        String cacheDirRoot = this.getSaveDir();
        return cacheDirRoot + File.separator + Core.gameMode;
    }

    public String getCurrentSaveDir() {
        return this.getGameModeCacheDir() + File.separator + Core.gameSaveWorld;
    }

    public String getFileNameInCurrentSave(String fileName) {
        return this.getCurrentSaveDir() + File.separator + fileName;
    }

    public String getFileNameInCurrentSave(String subDir, String fileName) {
        return this.getFileNameInCurrentSave(subDir + File.separator + fileName);
    }

    public String getFileNameInCurrentSave(String subDir1, String subDir2, String fileName) {
        return this.getFileNameInCurrentSave(subDir1 + File.separator + subDir2 + File.separator + fileName);
    }

    public File getFileInCurrentSave(String fileName) {
        return new File(this.getFileNameInCurrentSave(fileName));
    }

    public File getFileInCurrentSave(String subDir, String fileName) {
        return new File(this.getFileNameInCurrentSave(subDir, fileName));
    }

    public File getFileInCurrentSave(String subDir1, String subDir2, String fileName) {
        return new File(this.getFileNameInCurrentSave(subDir1, subDir2, fileName));
    }

    public String getSaveDir() {
        String savesDir = this.getCacheDirSub("Saves");
        ensureFolderExists(savesDir);
        return savesDir;
    }

    public String getSaveDirSub(String subPath) {
        return this.getSaveDir() + File.separator + subPath;
    }

    public String getScreenshotDir() {
        String dir = this.getCacheDirSub("Screenshots");
        ensureFolderExists(dir);
        return dir;
    }

    public String getScreenshotDirSub(String subPath) {
        return this.getScreenshotDir() + File.separator + subPath;
    }

    public void setCacheDir(String dir) {
        dir = dir.replace("/", File.separator);
        this.cacheDir = new File(dir).getAbsoluteFile();
        ensureFolderExists(this.cacheDir);
    }

    public String getCacheDir() {
        if (this.cacheDir == null) {
            String cacheDirRoot = System.getProperty("deployment.user.cachedir");
            if (cacheDirRoot == null || System.getProperty("os.name").startsWith("Win")) {
                cacheDirRoot = System.getProperty("user.home");
            }

            String cacheDirPath = cacheDirRoot + File.separator + "Zomboid";
            this.setCacheDir(cacheDirPath);
        }

        return this.cacheDir.getPath();
    }

    public String getCacheDirSub(String subPath) {
        return this.getCacheDir() + File.separator + subPath;
    }

    public String getMessagingDir() {
        String messagingDir = this.getCacheDirSub("messaging");
        ensureFolderExists(messagingDir);
        return messagingDir;
    }

    public String getMessagingDirSub(String subPath) {
        return this.getMessagingDir() + File.separator + subPath;
    }

    public URI getMediaLowercaseURI() {
        return this.workdir.lowercaseUri;
    }

    public File getMediaRootFile() {
        assert this.workdir.canonicalFile != null;

        return this.workdir.canonicalFile;
    }

    public String getMediaRootPath() {
        return this.workdir.canonicalFile.getPath();
    }

    public File getMediaFile(String subPath) {
        assert this.workdir.canonicalFile != null;

        return new File(this.workdir.canonicalFile, subPath);
    }

    public String getMediaPath(String subPath) {
        return this.getMediaFile(subPath).getPath();
    }

    public String getAbsoluteWorkDir() {
        return this.workdir.canonicalFile.getPath();
    }

    public String getLocalWorkDir() {
        return this.localWorkdir.getPath();
    }

    public String getLocalWorkDirSub(String subPath) {
        return this.getLocalWorkDir() + File.separator + subPath;
    }

    public File getAnimsXFile() {
        return this.animsX.canonicalFile;
    }

    public String getAnimSetsPath() {
        return this.animSets.canonicalFile.getPath();
    }

    public String getActionGroupsPath() {
        return this.actiongroups.canonicalFile.getPath();
    }

    public static boolean ensureFolderExists(String path) {
        return ensureFolderExists(new File(path).getAbsoluteFile());
    }

    public static boolean ensureFolderExists(File directory) {
        return directory.exists() || directory.mkdirs();
    }

    public void searchFolders(File fo) {
        if (!GameServer.server) {
            Thread.yield();
            Core.getInstance().DoFrameReady();
        }

        if (fo.isDirectory()) {
            String path = fo.getAbsolutePath().replace("\\", "/").replace("./", "");
            if (path.contains("media/maps/")) {
                this.loadList.add(path);
            }

            String[] internalNames = fo.list();

            for (int i = 0; i < internalNames.length; i++) {
                this.searchFolders(new File(fo.getAbsolutePath() + File.separator + internalNames[i]));
            }
        } else {
            this.loadList.add(fo.getAbsolutePath().replace("\\", "/").replace("./", ""));
        }
    }

    public Object[] getAllPathsContaining(String str) {
        ArrayList<String> loadList = new ArrayList<>();

        for (Entry<String, String> entry : this.activeFileMap.entrySet()) {
            if (entry.getKey().contains(str)) {
                loadList.add(entry.getValue());
            }
        }

        return loadList.toArray();
    }

    public Object[] getAllPathsContaining(String str, String str2) {
        ArrayList<String> loadList = new ArrayList<>();

        for (Entry<String, String> entry : this.activeFileMap.entrySet()) {
            if (entry.getKey().contains(str) && entry.getKey().contains(str2)) {
                loadList.add(entry.getValue());
            }
        }

        return loadList.toArray();
    }

    public synchronized String getString(String str) {
        if (this.ignoreActiveFileMap.get()) {
            return str;
        } else {
            String lower = str.toLowerCase(Locale.ENGLISH);
            String relative = this.relativeMap.get(lower);
            if (relative != null) {
                lower = relative;
            } else {
                String var6 = this.getRelativeFile(str);
                lower = var6.toLowerCase(Locale.ENGLISH);
                this.relativeMap.put(lower, lower);
            }

            String absolute = this.activeFileMap.get(lower);
            return absolute != null ? absolute : str;
        }
    }

    public synchronized String getDirectoryString(String str) {
        String absPath = this.getString(str);
        if (absPath != str) {
            return absPath;
        } else {
            return str.endsWith("/") ? this.getString(str.substring(0, str.length() - 1)) : this.getString(str + "/");
        }
    }

    public synchronized boolean isKnownFile(String str) {
        if (this.allAbsolutePaths.contains(str)) {
            return true;
        } else {
            String lower = str.toLowerCase(Locale.ENGLISH);
            String relative = this.relativeMap.get(lower);
            if (relative != null) {
                lower = relative;
            } else {
                String var6 = this.getRelativeFile(str);
                lower = var6.toLowerCase(Locale.ENGLISH);
                this.relativeMap.put(lower, lower);
            }

            String absolute = this.activeFileMap.get(lower);
            return absolute != null;
        }
    }

    public String getAbsolutePath(String rel) {
        String lower = rel.toLowerCase(Locale.ENGLISH);
        return this.activeFileMap.get(lower);
    }

    public void Reset() {
        this.loadList.clear();
        this.activeFileMap.clear();
        this.allAbsolutePaths.clear();
        this.canonicalUriMap.clear();
        this.modIdToDir.clear();
        this.modDirToMod.clear();
        this.mods.clear();
        this.modFolders = null;
        ActiveMods.Reset();
        if (this.fileGuidTable != null) {
            this.fileGuidTable.clear();
            this.fileGuidTable = null;
        }
    }

    public File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (Exception var3) {
            return file.getAbsoluteFile();
        }
    }

    public File getCanonicalFile(String path) {
        return this.getCanonicalFile(new File(path));
    }

    public String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception var3) {
            return file.getAbsolutePath();
        }
    }

    public String getCanonicalPath(String path) {
        return this.getCanonicalPath(new File(path));
    }

    public URI getCanonicalURI(String path) {
        URI uri = this.canonicalUriMap.get(path);
        if (uri == null) {
            uri = this.getCanonicalFile(path).toURI();
            this.canonicalUriMap.put(path, uri);
        }

        return uri;
    }

    public void resetModFolders() {
        this.modFolders = null;
    }

    public void getInstalledItemModsFolders(ArrayList<String> out) {
        if (SteamUtils.isSteamModeEnabled()) {
            String[] folders = SteamWorkshop.instance.GetInstalledItemFolders();
            if (folders != null) {
                for (String folder : folders) {
                    File file = new File(folder + File.separator + "mods");
                    if (file.exists()) {
                        out.add(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public void getStagedItemModsFolders(ArrayList<String> out) {
        if (SteamUtils.isSteamModeEnabled()) {
            ArrayList<String> folders = SteamWorkshop.instance.getStageFolders();

            for (int i = 0; i < folders.size(); i++) {
                File file = new File(folders.get(i) + File.separator + "Contents" + File.separator + "mods");
                if (file.exists()) {
                    out.add(file.getAbsolutePath());
                }
            }
        }
    }

    private void getAllModFoldersAux(String folder, List<String> out) {
        Filter<Path> filter = new Filter<Path>() {
            {
                Objects.requireNonNull(ZomboidFileSystem.this);
            }

            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry)
                    && Files.exists(entry.resolve("common"))
                    && ZomboidFileSystem.instance.getModVersionFile(entry.toString()) != null;
            }
        };
        Path dir = FileSystems.getDefault().getPath(folder);
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, filter)) {
                for (Path path : dstrm) {
                    if (path.getFileName().toString().equalsIgnoreCase("examplemod")) {
                        DebugLog.Mod.println("refusing to list " + path.getFileName());
                    } else {
                        String absolutePath = path.toAbsolutePath().toString();
                        Path commonPath = path.resolve("common");
                        File versionFile = this.getModVersionFile(absolutePath);
                        if (versionFile != null || Files.exists(commonPath)) {
                            if (!this.watchedModFolders.contains(absolutePath)) {
                                this.watchedModFolders.add(absolutePath);
                                DebugFileWatcher.instance.addDirectory(absolutePath);
                                if (Files.exists(commonPath)) {
                                    Path path1 = commonPath.resolve("media");
                                    if (Files.exists(path1)) {
                                        DebugFileWatcher.instance.addDirectoryRecurse(path1.toAbsolutePath().toString());
                                    }
                                }

                                if (versionFile != null) {
                                    Path path2 = versionFile.toPath().resolve("media");
                                    if (Files.exists(path2)) {
                                        DebugFileWatcher.instance.addDirectoryRecurse(path2.toAbsolutePath().toString());
                                    }
                                }
                            }

                            out.add(absolutePath);
                        }
                    }
                }
            } catch (Exception var14) {
                var14.printStackTrace();
            }
        }
    }

    public void setModFoldersOrder(String s) {
        this.modFoldersOrder = new ArrayList<>(Arrays.asList(s.split(",")));
    }

    public void getAllModFolders(List<String> out) {
        if (this.modFolders == null) {
            this.modFolders = new ArrayList<>();
            if (this.modFoldersOrder == null) {
                this.setModFoldersOrder("workshop,steam,mods");
            }

            ArrayList<String> modsFolders = new ArrayList<>();

            for (int i = 0; i < this.modFoldersOrder.size(); i++) {
                String s = this.modFoldersOrder.get(i);
                if ("workshop".equals(s)) {
                    this.getStagedItemModsFolders(modsFolders);
                }

                if ("steam".equals(s)) {
                    this.getInstalledItemModsFolders(modsFolders);
                }

                if ("mods".equals(s)) {
                    modsFolders.add(Core.getMyDocumentFolder() + File.separator + "mods");
                }
            }

            for (int j = 0; j < modsFolders.size(); j++) {
                String folder = modsFolders.get(j);
                if (!this.watchedModFolders.contains(folder)) {
                    this.watchedModFolders.add(folder);
                    DebugFileWatcher.instance.addDirectory(folder);
                }

                this.getAllModFoldersAux(folder, this.modFolders);
            }

            DebugFileWatcher.instance.add(this.modFileWatcher);
        }

        out.clear();
        out.addAll(this.modFolders);
    }

    public File getModVersionFile(String modDir) {
        int modVersionInt = 42000;
        Path versionFolder = null;
        int currentGameVersion = Core.getInstance().getGameVersion().getInt();
        Path dir = FileSystems.getDefault().getPath(modDir);
        if (!Files.exists(dir)) {
            return null;
        } else {
            try (DirectoryStream<Path> modDstrm = Files.newDirectoryStream(dir, x$0 -> Files.isDirectory(x$0))) {
                for (Path modSubDirPath : modDstrm) {
                    Integer folderVersion = this.getGameVersionIntFromName(modSubDirPath.getFileName().toString());
                    if (folderVersion != null && folderVersion >= modVersionInt && folderVersion <= currentGameVersion) {
                        modVersionInt = folderVersion;
                        versionFolder = modSubDirPath;
                    }
                }
            } catch (Exception var12) {
                var12.printStackTrace();
            }

            return versionFolder != null ? versionFolder.toFile() : null;
        }
    }

    public Integer getGameVersionIntFromName(String modSubdir) {
        if (!modSubdir.contains(".")) {
            int major = PZMath.tryParseInt(modSubdir, 0);
            if (major >= 42) {
                return major * 1000;
            }
        } else {
            Matcher m1 = Pattern.compile("([0-9]+)\\.([0-9]+)(.*)").matcher(modSubdir);
            if (m1.matches()) {
                int major = PZMath.tryParseInt(m1.group(1), 0);
                int minor = PZMath.tryParseInt(m1.group(2), 0);
                if (major >= 42 && minor >= 0 && minor <= 999) {
                    return major * 1000 + minor;
                }
            }
        }

        return null;
    }

    public ArrayList<ChooseGameInfo.Mod> getWorkshopItemMods(long itemID) {
        ArrayList<ChooseGameInfo.Mod> mods = new ArrayList<>();
        if (!SteamUtils.isSteamModeEnabled()) {
            return mods;
        } else {
            String folder = SteamWorkshop.instance.GetItemInstallFolder(itemID);
            if (folder == null) {
                return mods;
            } else {
                File file = new File(folder + File.separator + "mods");
                if (file.exists() && file.isDirectory()) {
                    File[] files = file.listFiles();

                    for (File file2 : files) {
                        if (file2.isDirectory()) {
                            ChooseGameInfo.Mod modInfo = ChooseGameInfo.readModInfo(file2.getAbsolutePath());
                            if (modInfo != null) {
                                mods.add(modInfo);
                            }
                        }
                    }

                    return mods;
                } else {
                    return mods;
                }
            }
        }
    }

    public void setModIdToDir(String id, String dir) {
        this.modIdToDir.putIfAbsent(id, dir);
    }

    public ChooseGameInfo.Mod searchForModInfo(File path, String modSearched, ArrayList<ChooseGameInfo.Mod> mods) {
        if (path.isDirectory()) {
            String[] internalNames = path.list();
            if (internalNames == null) {
                return null;
            }

            for (int i = 0; i < internalNames.length; i++) {
                File file = new File(path.getAbsolutePath() + File.separator + internalNames[i]);
                ChooseGameInfo.Mod result = this.searchForModInfo(file, modSearched, mods);
                if (result != null) {
                    return result;
                }
            }
        } else if (path.getAbsolutePath().endsWith("mod.info")) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.readModInfo(path.getAbsoluteFile().getParent());
            if (mod == null) {
                return null;
            }

            if (!mod.getId().equals("\\")) {
                this.modIdToDir.put(mod.getId(), mod.getDir());
                mods.add(mod);
            }

            if (mod.getId().equals(modSearched)) {
                return mod;
            }
        }

        return null;
    }

    public void loadMod(String modId) {
        if (this.getModDir(modId) != null) {
            if (CoopMaster.instance != null) {
                CoopMaster.instance.update();
            }

            DebugLog.Mod.println("loading " + modId);
            ChooseGameInfo.Mod mod = this.getModInfoForDir(this.getModDir(modId));
            this.loadList.clear();
            File modPathbase = new File(mod.getCommonDir().toLowerCase(Locale.ENGLISH));
            URI modPathbaseURI = modPathbase.toURI();
            this.searchFolders(new File(mod.getCommonDir()));

            for (int n = 0; n < this.loadList.size(); n++) {
                String rel = this.getRelativeFile(modPathbaseURI, this.loadList.get(n));
                rel = rel.toLowerCase(Locale.ENGLISH);
                if (this.activeFileMap.containsKey(rel) && !rel.endsWith("mod.info") && !rel.endsWith("poster.png")) {
                    DebugLog.Mod.println("mod \"" + modId + "\" overrides " + rel);
                }

                String absPath = new File(this.loadList.get(n)).getAbsolutePath();
                this.activeFileMap.put(rel, absPath);
                this.allAbsolutePaths.add(absPath);
            }

            this.loadList.clear();
            modPathbase = new File(mod.getVersionDir().toLowerCase(Locale.ENGLISH));
            modPathbaseURI = modPathbase.toURI();
            this.searchFolders(new File(mod.getVersionDir()));

            for (int n = 0; n < this.loadList.size(); n++) {
                String rel = this.getRelativeFile(modPathbaseURI, this.loadList.get(n));
                rel = rel.toLowerCase(Locale.ENGLISH);
                if (this.activeFileMap.containsKey(rel) && !rel.endsWith("mod.info") && !rel.endsWith("poster.png")) {
                    DebugLog.Mod.println("mod \"" + modId + "\" overrides " + rel);
                }

                String absPath = new File(this.loadList.get(n)).getAbsolutePath();
                this.activeFileMap.put(rel, absPath);
                this.allAbsolutePaths.add(absPath);
            }

            this.loadList.clear();
        }
    }

    private ArrayList<String> readLoadedDotTxt() {
        String path = Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + "loaded.txt";
        File file = new File(path);
        if (!file.exists()) {
            return null;
        } else {
            ArrayList<String> modIDs = new ArrayList<>();

            try (
                FileReader fr = new FileReader(path);
                BufferedReader br = new BufferedReader(fr);
            ) {
                for (String str = br.readLine(); str != null; str = br.readLine()) {
                    str = str.trim();
                    if (!str.isEmpty()) {
                        modIDs.add(str);
                    }
                }
            } catch (Exception var13) {
                ExceptionLogger.logException(var13);
                modIDs = null;
            }

            try {
                file.delete();
            } catch (Exception var8) {
                ExceptionLogger.logException(var8);
            }

            return modIDs;
        }
    }

    private ActiveMods readDefaultModsTxt() {
        ActiveMods activeMods = ActiveMods.getById("default");
        ArrayList<String> modIDs = this.readLoadedDotTxt();
        if (modIDs != null) {
            activeMods.getMods().addAll(modIDs);
            this.saveModsFile();
        }

        activeMods.clear();
        String path = Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + "default.txt";

        try {
            ActiveModsFile activeModsFile = new ActiveModsFile();
            if (activeModsFile.read(path, activeMods)) {
            }
        } catch (Exception var5) {
            ExceptionLogger.logException(var5);
        }

        return activeMods;
    }

    public void loadMods(String activeMods) {
        if (Core.optionModsEnabled) {
            if (GameClient.client) {
                ArrayList<String> toLoad = new ArrayList<>();
                this.loadTranslationMods(toLoad);
                toLoad.addAll(GameClient.instance.serverMods);
                this.loadMods(toLoad);
            } else {
                ActiveMods activeMods1 = ActiveMods.getById(activeMods);
                if (!"default".equalsIgnoreCase(activeMods)) {
                    ActiveMods.setLoadedMods(activeMods1);
                    this.loadMods(activeMods1.getMods());
                } else {
                    try {
                        activeMods1 = this.readDefaultModsTxt();
                        activeMods1.checkMissingMods();
                        activeMods1.checkMissingMaps();
                        ActiveMods.setLoadedMods(activeMods1);
                        this.loadMods(activeMods1.getMods());
                    } catch (Exception var4) {
                        ExceptionLogger.logException(var4);
                    }
                }
            }
        }
    }

    private boolean isTranslationMod(String modId) {
        ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
        if (info == null) {
            return false;
        } else {
            boolean hasTranslations = false;
            File modPathBase = new File(info.getCommonDir());
            URI modPathBaseURI = modPathBase.toURI();
            this.loadList.clear();
            this.searchFolders(modPathBase);

            for (int n = 0; n < this.loadList.size(); n++) {
                String rel = this.getRelativeFile(modPathBaseURI, this.loadList.get(n));
                if (rel.endsWith(".lua")) {
                    return false;
                }

                if (rel.startsWith("media/maps/")) {
                    return false;
                }

                if (rel.startsWith("media/scripts/")) {
                    return false;
                }

                if (rel.startsWith("media/lua/")) {
                    if (!rel.startsWith("media/lua/shared/Translate/")) {
                        return false;
                    }

                    hasTranslations = true;
                }
            }

            modPathBase = new File(info.getVersionDir());
            modPathBaseURI = modPathBase.toURI();
            this.loadList.clear();
            this.searchFolders(modPathBase);

            for (int n = 0; n < this.loadList.size(); n++) {
                String relx = this.getRelativeFile(modPathBaseURI, this.loadList.get(n));
                if (relx.endsWith(".lua")) {
                    return false;
                }

                if (relx.startsWith("media/maps/")) {
                    return false;
                }

                if (relx.startsWith("media/scripts/")) {
                    return false;
                }

                if (relx.startsWith("media/lua/")) {
                    if (!relx.startsWith("media/lua/shared/Translate/")) {
                        return false;
                    }

                    hasTranslations = true;
                }
            }

            this.loadList.clear();
            return hasTranslations;
        }
    }

    private void loadTranslationMods(ArrayList<String> toLoad) {
        if (GameClient.client) {
            ActiveMods activeMods = this.readDefaultModsTxt();
            ArrayList<String> mods = new ArrayList<>();
            if (this.loadModsAux(activeMods.getMods(), mods) == null) {
                for (String modId : mods) {
                    if (this.isTranslationMod(modId)) {
                        DebugLog.Mod.println("loading translation mod \"" + modId + "\"");
                        if (!toLoad.contains(modId)) {
                            toLoad.add(modId);
                        }
                    }
                }
            }
        }
    }

    private String loadModAndRequired(String modId, ArrayList<String> ordered) {
        if (modId.isEmpty()) {
            return null;
        } else if (modId.equalsIgnoreCase("examplemod")) {
            DebugLog.Mod.warn("refusing to load " + modId);
            return null;
        } else if (ordered.contains(modId)) {
            return null;
        } else {
            ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
            if (info == null) {
                if (GameServer.server) {
                    GameServer.ServerMods.remove(modId);
                }

                DebugLog.Mod.warn("required mod \"" + modId + "\" not found");
                return modId;
            } else {
                if (info.getRequire() != null) {
                    String reqId = this.loadModsAux(info.getRequire(), ordered);
                    if (reqId != null) {
                        return reqId;
                    }
                }

                ordered.add(modId);
                return null;
            }
        }
    }

    public String loadModsAux(ArrayList<String> toLoad, ArrayList<String> ordered) {
        for (String modId : toLoad) {
            String failId = this.loadModAndRequired(modId, ordered);
            if (failId != null) {
                return failId;
            }
        }

        return null;
    }

    public void loadMods(ArrayList<String> toLoad) {
        this.mods.clear();

        for (String modId : toLoad) {
            this.loadModAndRequired(modId, this.mods);
        }

        for (String modId : this.mods) {
            this.loadMod(modId);
        }
    }

    public ArrayList<String> getModIDs() {
        return this.mods;
    }

    public String getModDir(String modId) {
        return this.modIdToDir.get(modId);
    }

    public ChooseGameInfo.Mod getModInfoForDir(String modDir) {
        ChooseGameInfo.Mod modInfo = this.modDirToMod.get(modDir);
        if (modInfo == null) {
            modInfo = new ChooseGameInfo.Mod(modDir);
            this.modDirToMod.put(modDir, modInfo);
        }

        return modInfo;
    }

    public ChunkGenerationStatus isModded(String path) {
        for (String lookup : this.modDirToMod.keySet()) {
            if (path.startsWith(lookup)) {
                return ChunkGenerationStatus.MODDED;
            }
        }

        return ChunkGenerationStatus.CORE;
    }

    public String getRelativeFile(File file) {
        return this.getRelativeFile(this.base.lowercaseUri, file.getAbsolutePath());
    }

    public String getRelativeFile(String string) {
        return this.getRelativeFile(this.base.lowercaseUri, string);
    }

    public String getRelativeFile(URI root, File file) {
        return this.getRelativeFile(root, file.getAbsolutePath());
    }

    public String getRelativeFile(URI root, String string) {
        String absolutePath = new File(string).getAbsolutePath();
        URI uri = new File(absolutePath.toLowerCase(Locale.ENGLISH)).toURI();
        URI rel = root.relativize(uri);
        if (rel.equals(uri)) {
            return string;
        } else {
            String relPath = rel.getPath();
            if (string.endsWith("/") && !relPath.endsWith("/")) {
                relPath = relPath + "/";
            }

            return relPath;
        }
    }

    public String getAnimName(URI mediaURI, File file) {
        String relativePath = this.getRelativeFile(mediaURI, file);
        String animName = relativePath.toLowerCase(Locale.ENGLISH);
        int dotIndex = animName.lastIndexOf(46);
        if (dotIndex > -1) {
            animName = animName.substring(0, dotIndex);
        }

        if (animName.startsWith("anims/")) {
            animName = animName.substring("anims/".length());
        } else if (animName.startsWith("anims_x/")) {
            animName = animName.substring("anims_x/".length());
        }

        return animName;
    }

    public String resolveRelativePath(String srcFilePath, String relativePath) {
        Path srcPath = Paths.get(srcFilePath);
        Path srcFolder = srcPath.getParent();
        Path destPath = srcFolder.resolve(relativePath);
        String result = destPath.toString();
        return this.getRelativeFile(result);
    }

    public void saveModsFile() {
        try {
            ensureFolderExists(Core.getMyDocumentFolder() + File.separator + "mods");
            String path = Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + "default.txt";
            ActiveModsFile activeModsFile = new ActiveModsFile();
            activeModsFile.write(path, ActiveMods.getById("default"));
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
        }
    }

    public void loadModPackFiles() {
        for (String modId : this.mods) {
            try {
                ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
                if (info != null) {
                    for (ChooseGameInfo.PackFile pack : info.getPacks()) {
                        String rel = this.getRelativeFile("media/texturepacks/" + pack.name + ".pack");
                        rel = rel.toLowerCase(Locale.ENGLISH);
                        if (!this.activeFileMap.containsKey(rel)) {
                            DebugLog.Mod.warn("pack file \"" + pack.name + "\" needed by " + modId + " not found");
                        } else {
                            String fileName = instance.getString("media/texturepacks/" + pack.name + ".pack");
                            if (!this.loadedPacks.contains(fileName)) {
                                GameWindow.LoadTexturePack(pack.name, pack.flags, modId);
                                this.loadedPacks.add(fileName);
                            }
                        }
                    }
                }
            } catch (Exception var8) {
                ExceptionLogger.logException(var8);
            }
        }

        GameWindow.setTexturePackLookup();
    }

    public void loadModTileDefs() {
        HashSet<Integer> usedFileNumbers = new HashSet<>();

        for (String modId : this.mods) {
            try {
                ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
                if (info != null) {
                    for (ChooseGameInfo.TileDef tileDef : info.getTileDefs()) {
                        if (usedFileNumbers.contains(tileDef.fileNumber)) {
                            DebugLog.Mod.error("tiledef fileNumber " + tileDef.fileNumber + " used by more than one mod");
                        } else {
                            String name = tileDef.name;
                            String rel = this.getRelativeFile("media/" + name + ".tiles");
                            rel = rel.toLowerCase(Locale.ENGLISH);
                            if (!this.activeFileMap.containsKey(rel)) {
                                DebugLog.Mod.error("tiledef file \"" + tileDef.name + "\" needed by " + modId + " not found");
                            } else {
                                name = this.activeFileMap.get(rel);
                                IsoWorld.instance.LoadTileDefinitions(IsoSpriteManager.instance, name, tileDef.fileNumber);
                                usedFileNumbers.add(tileDef.fileNumber);
                            }
                        }
                    }
                }
            } catch (Exception var9) {
                var9.printStackTrace();
            }
        }
    }

    public void loadModTileDefPropertyStrings() {
        HashSet<Integer> usedFileNumbers = new HashSet<>();

        for (String modId : this.mods) {
            try {
                ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
                if (info != null) {
                    for (ChooseGameInfo.TileDef tileDef : info.getTileDefs()) {
                        if (usedFileNumbers.contains(tileDef.fileNumber)) {
                            DebugLog.Mod.error("tiledef fileNumber " + tileDef.fileNumber + " used by more than one mod");
                        } else {
                            String name = tileDef.name;
                            String rel = this.getRelativeFile("media/" + name + ".tiles");
                            rel = rel.toLowerCase(Locale.ENGLISH);
                            if (!this.activeFileMap.containsKey(rel)) {
                                DebugLog.Mod.error("tiledef file \"" + tileDef.name + "\" needed by " + modId + " not found");
                            } else {
                                name = this.activeFileMap.get(rel);
                                IsoWorld.instance.LoadTileDefinitionsPropertyStrings(IsoSpriteManager.instance, name, tileDef.fileNumber);
                                usedFileNumbers.add(tileDef.fileNumber);
                            }
                        }
                    }
                }
            } catch (Exception var9) {
                var9.printStackTrace();
            }
        }
    }

    public void loadFileGuidTable() {
        File file = instance.getMediaFile("fileGuidTable.xml");

        try (FileInputStream adrFile = new FileInputStream(file)) {
            JAXBContext ctx = JAXBContext.newInstance(FileGuidTable.class);
            Unmarshaller um = ctx.createUnmarshaller();
            this.fileGuidTable = (FileGuidTable)um.unmarshal(adrFile);
            this.fileGuidTable.setModID("game");
        } catch (IOException | JAXBException var20) {
            System.err.println("Failed to load file Guid table.");
            ExceptionLogger.logException(var20);
            return;
        }

        try {
            JAXBContext ctx = JAXBContext.newInstance(FileGuidTable.class);
            Unmarshaller um = ctx.createUnmarshaller();

            for (String modID : this.getModIDs()) {
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
                if (mod != null) {
                    try (FileInputStream fis = new FileInputStream(mod.getCommonDir() + "/media/fileGuidTable.xml")) {
                        FileGuidTable table = (FileGuidTable)um.unmarshal(fis);
                        table.setModID(modID);
                        this.fileGuidTable.mergeFrom(table);
                    } catch (FileNotFoundException var17) {
                    } catch (Exception var18) {
                        ExceptionLogger.logException(var18);
                    }

                    try (FileInputStream fis = new FileInputStream(mod.getVersionDir() + "/media/fileGuidTable.xml")) {
                        FileGuidTable table = (FileGuidTable)um.unmarshal(fis);
                        table.setModID(modID);
                        this.fileGuidTable.mergeFrom(table);
                    } catch (FileNotFoundException var13) {
                    } catch (Exception var14) {
                        ExceptionLogger.logException(var14);
                    }
                }
            }
        } catch (Exception var21) {
            ExceptionLogger.logException(var21);
        }

        this.fileGuidTable.loaded();
        if (!this.fileGuidTableWatcherActive) {
            DebugFileWatcher.instance.add(new PredicatedFileWatcher("media/fileGuidTable.xml", entryKey -> this.loadFileGuidTable()));
            this.fileGuidTableWatcherActive = true;
        }
    }

    public FileGuidTable getFileGuidTable() {
        if (this.fileGuidTable == null) {
            this.loadFileGuidTable();
        }

        return this.fileGuidTable;
    }

    public String getFilePathFromGuid(String guid) {
        FileGuidTable table = this.getFileGuidTable();
        return table != null ? table.getFilePathFromGuid(guid) : null;
    }

    public String getGuidFromFilePath(String path) {
        FileGuidTable table = this.getFileGuidTable();
        return table != null ? table.getGuidFromFilePath(path) : null;
    }

    public String resolveFileOrGUID(String source) {
        String fileName = source;
        String guidFileName = this.getFilePathFromGuid(source);
        if (guidFileName != null) {
            fileName = guidFileName;
        }

        String lower = fileName.toLowerCase(Locale.ENGLISH);
        return this.activeFileMap.containsKey(lower) ? this.activeFileMap.get(lower) : fileName;
    }

    public boolean isValidFilePathGuid(String source) {
        return this.getFilePathFromGuid(source) != null;
    }

    public static File[] listAllDirectories(String rootPath, FileFilter filter, boolean recursive) {
        File root = new File(rootPath).getAbsoluteFile();
        return listAllDirectories(root, filter, recursive);
    }

    public static File[] listAllDirectories(File root, FileFilter filter, boolean recursive) {
        if (!root.isDirectory()) {
            return new File[0];
        } else {
            ArrayList<File> result = new ArrayList<>();
            listAllDirectoriesInternal(root, filter, recursive, result);
            return result.toArray(new File[0]);
        }
    }

    private static void listAllDirectoriesInternal(File folder, FileFilter filter, boolean recursive, ArrayList<File> out_result) {
        File[] list = folder.listFiles();
        if (list != null) {
            for (File listedFile : list) {
                if (!listedFile.isFile() && listedFile.isDirectory()) {
                    if (filter.accept(listedFile)) {
                        out_result.add(listedFile);
                    }

                    if (recursive) {
                        listAllFilesInternal(listedFile, filter, true, out_result);
                    }
                }
            }
        }
    }

    public static File[] listAllFiles(String folderPath, FileFilter filter, boolean recursive) {
        File folder = new File(folderPath).getAbsoluteFile();
        return listAllFiles(folder, filter, recursive);
    }

    public static File[] listAllFiles(File folder, FileFilter filter, boolean recursive) {
        if (folder != null && folder.isDirectory()) {
            ArrayList<File> result = new ArrayList<>();
            listAllFilesInternal(folder, filter, recursive, result);
            return result.toArray(new File[0]);
        } else {
            return new File[0];
        }
    }

    public static File[] listAllFiles(File folder) {
        return listAllFiles(folder, file -> true, false);
    }

    private static void listAllFilesInternal(File folder, FileFilter filter, boolean recursive, ArrayList<File> out_result) {
        File[] list = folder.listFiles();
        if (list != null) {
            for (File listedFile : list) {
                if (listedFile.isFile()) {
                    if (filter.accept(listedFile)) {
                        out_result.add(listedFile);
                    }
                } else if (listedFile.isDirectory() && recursive) {
                    listAllFilesInternal(listedFile, filter, true, out_result);
                }
            }
        }
    }

    public void walkGameAndModFiles(String relPath, boolean recursive, ZomboidFileSystem.IWalkFilesVisitor consumer) {
        this.walkGameAndModFilesInternal(this.base.canonicalFile, relPath, recursive, consumer);
        ArrayList<String> modIDs = this.getModIDs();

        for (int n = 0; n < modIDs.size(); n++) {
            String modID = modIDs.get(n);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                this.walkGameAndModFilesInternal(new File(mod.getCommonDir()), relPath, recursive, consumer);
                this.walkGameAndModFilesInternal(new File(mod.getVersionDir()), relPath, recursive, consumer);
            }
        }
    }

    private void walkGameAndModFilesInternal(File rootFile, String relPath, boolean recursive, ZomboidFileSystem.IWalkFilesVisitor consumer) {
        File fo = new File(rootFile, relPath);
        if (fo.isDirectory()) {
            File[] list = fo.listFiles();
            if (list != null) {
                for (File listedFile : list) {
                    consumer.visit(listedFile, relPath);
                    if (recursive && listedFile.isDirectory()) {
                        this.walkGameAndModFilesInternal(rootFile, relPath + "/" + listedFile.getName(), true, consumer);
                    }
                }
            }
        }
    }

    public String[] resolveAllDirectories(String relPath, FileFilter filter, boolean recursive) {
        ArrayList<String> result = new ArrayList<>();
        this.walkGameAndModFiles(relPath, recursive, (file, relPath2) -> {
            if (file.isDirectory() && filter.accept(file)) {
                String relPath3 = relPath2 + "/" + file.getName();
                if (!result.contains(relPath3)) {
                    result.add(relPath3);
                }
            }
        });
        return result.toArray(new String[0]);
    }

    public String[] resolveAllFiles(String relPath, FileFilter filter, boolean recursive) {
        ArrayList<String> result = new ArrayList<>();
        this.walkGameAndModFiles(relPath, recursive, (file, relPath2) -> {
            if (file.isFile() && filter.accept(file)) {
                String relPath3 = relPath2 + "/" + file.getName();
                if (!result.contains(relPath3)) {
                    result.add(relPath3);
                }
            }
        });
        return result.toArray(new String[0]);
    }

    public String normalizeFolderPath(String path) {
        path = path.toLowerCase(Locale.ENGLISH).replace('\\', '/');
        path = path + "/";
        return path.replace("///", "/").replace("//", "/");
    }

    public static String processFilePath(String filePath, char separatorChar) {
        if (separatorChar != '\\') {
            filePath = filePath.replace('\\', separatorChar);
        }

        if (separatorChar != '/') {
            filePath = filePath.replace('/', separatorChar);
        }

        return filePath;
    }

    public boolean tryDeleteFile(String filePath) {
        if (StringUtils.isNullOrWhitespace(filePath)) {
            return false;
        } else {
            try {
                return this.deleteFile(filePath);
            } catch (AccessControlException | IOException var3) {
                ExceptionLogger.logException(var3, String.format("Failed to delete file: \"%s\"", filePath), DebugLog.FileIO, LogSeverity.General);
                return false;
            }
        }
    }

    public static boolean deleteDirectory(String dirPath) {
        File directoryToBeDeleted = new File(dirPath);
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file.getAbsolutePath());
            }
        }

        return directoryToBeDeleted.delete();
    }

    public boolean deleteFile(String filePath) throws IOException {
        File file = new File(filePath).getAbsoluteFile();
        if (!file.isFile()) {
            throw new FileNotFoundException(String.format("File path not found: \"%s\"", filePath));
        } else if (file.delete()) {
            DebugLog.FileIO.debugln("File deleted successfully: \"%s\"", filePath);
            return true;
        } else {
            DebugLog.FileIO.debugln("Failed to delete file: \"%s\"", filePath);
            return false;
        }
    }

    public void update() {
        if (this.modsChangedTime != 0L) {
            long now = System.currentTimeMillis();
            if (this.modsChangedTime <= now) {
                this.modsChangedTime = 0L;
                this.modFolders = null;
                this.modIdToDir.clear();
                this.modDirToMod.clear();
                ChooseGameInfo.Reset();

                for (String modID : this.getModIDs()) {
                    ChooseGameInfo.getModDetails(modID);
                }

                LuaEventManager.triggerEvent("OnModsModified");
            }
        }
    }

    private boolean isModFile(String path) {
        if (this.modsChangedTime > 0L) {
            return false;
        } else if (this.modFolders == null) {
            return false;
        } else {
            path = path.toLowerCase().replace('\\', '/');
            if (path.endsWith("/mods/default.txt")) {
                return false;
            } else {
                for (int i = 0; i < this.modFolders.size(); i++) {
                    String path1 = this.modFolders.get(i).toLowerCase().replace('\\', '/');
                    if (path.startsWith(path1)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    private void onModFileChanged(String path) {
        this.modsChangedTime = System.currentTimeMillis() + 2000L;
    }

    public void cleanMultiplayerSaves() {
        DebugLog.FileIO.println("Start cleaning save fs");
        String cacheDirRoot = this.getSaveDir();
        String saveDirRoot = cacheDirRoot + File.separator + "Multiplayer" + File.separator;
        File mpDir = new File(saveDirRoot);
        if (!mpDir.exists()) {
            mpDir.mkdir();
        }

        try {
            File[] mpDirFiles = mpDir.listFiles();

            for (File saveDir : mpDirFiles) {
                DebugLog.FileIO.println("Checking " + saveDir.getAbsoluteFile() + " dir");
                if (saveDir.isDirectory()) {
                    File map_bin = new File(saveDir.toString() + File.separator + "map.bin");
                    if (map_bin.exists()) {
                        DebugLog.FileIO.println("Processing " + saveDir.getAbsoluteFile() + " dir");

                        try {
                            Stream<Path> files = Files.walk(saveDir.toPath());
                            files.forEach(file -> {
                                if (file.getFileName().toString().matches("map_\\d+_\\d+.bin")) {
                                    DebugLog.FileIO.println("Delete " + file.getFileName().toString());
                                    file.toFile().delete();
                                }
                            });
                        } catch (IOException var11) {
                            throw new RuntimeException(var11);
                        }
                    }
                }
            }
        } catch (RuntimeException var12) {
            var12.printStackTrace();
        }
    }

    public void resetDefaultModsForNewRelease(String versionStr) {
        ensureFolderExists(this.getCacheDirSub("mods"));
        String path = this.getCacheDirSub("mods") + File.separator + "reset-mods-" + versionStr + ".txt";
        File file = new File(path);
        if (!file.exists()) {
            try (
                FileWriter fr = new FileWriter(file);
                BufferedWriter br = new BufferedWriter(fr);
            ) {
                String contents = "If this file does not exist, default.txt will be reset to empty (no mods active).";
                br.write("If this file does not exist, default.txt will be reset to empty (no mods active).");
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
                return;
            }

            ActiveMods activeMods = ActiveMods.getById("default");
            activeMods.clear();
            this.saveModsFile();
        }
    }

    public static synchronized String getStartupTimeStamp() {
        if (startupTimeStamp == null) {
            startupTimeStamp = getDateTimeStampStringNow();
        }

        return startupTimeStamp;
    }

    public static String getDateTimeStampStringNow() {
        return getTimeStampStringNow(s_dateTimeSdf);
    }

    public static String getDateTimeStampString(Date time) {
        return getTimeStampString(time, s_dateTimeSdf);
    }

    public static String getDateStampString(Date time) {
        return getTimeStampString(time, s_dateOnlySdf);
    }

    public static String getTimeStampString(Date in_time, SimpleDateFormat in_format) {
        return in_format.format(in_time);
    }

    public static String getTimeStampStringNow(SimpleDateFormat in_format) {
        Date time = Calendar.getInstance().getTime();
        return getTimeStampString(time, in_format);
    }

    public interface IWalkFilesVisitor {
        void visit(File var1, String var2);
    }

    public static final class PZFolder {
        public File absoluteFile;
        public File canonicalFile;
        public URI canonicalUri;
        public URI lowercaseUri;

        public void set(File file) throws IOException {
            this.absoluteFile = file.getAbsoluteFile();
            this.canonicalFile = this.absoluteFile.getCanonicalFile();
            this.canonicalUri = this.canonicalFile.toURI();
            this.lowercaseUri = new File(this.canonicalFile.getPath().toLowerCase(Locale.ENGLISH)).toURI();
        }

        public void setWithCatch(File file) {
            try {
                this.set(file);
            } catch (IOException var3) {
                this.canonicalFile = null;
                this.canonicalUri = null;
                this.lowercaseUri = null;
                ExceptionLogger.logException(var3);
            }
        }
    }

    public static final class PZModFolder {
        public ZomboidFileSystem.PZFolder common = new ZomboidFileSystem.PZFolder();
        public ZomboidFileSystem.PZFolder version = new ZomboidFileSystem.PZFolder();

        public void setWithCatch(File commonFile, File versionFile) {
            this.common.setWithCatch(commonFile);
            this.version.setWithCatch(versionFile);
        }
    }
}
