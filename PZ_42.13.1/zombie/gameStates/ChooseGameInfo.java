// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.GameVersion;
import zombie.core.IndieFileLoader;
import zombie.core.Language;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ChooseGameInfo {
    private static final HashMap<String, ChooseGameInfo.Map> Maps = new HashMap<>();
    private static final HashMap<String, ChooseGameInfo.Mod> Mods = new HashMap<>();
    private static final HashSet<String> MissingMods = new HashSet<>();
    private static final ArrayList<String> tempStrings = new ArrayList<>();

    private ChooseGameInfo() {
    }

    public static void Reset() {
        Maps.clear();
        Mods.clear();
        MissingMods.clear();
    }

    private static void readTitleDotTxt(ChooseGameInfo.Map map, String dir, Language language) throws IOException {
        String path = "media/lua/shared/Translate/" + language.toString() + "/" + dir + "/title.txt";
        File file = new File(ZomboidFileSystem.instance.getString(path));

        try (
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName(language.charset()));
            BufferedReader br = new BufferedReader(isr);
        ) {
            String line = br.readLine();
            line = StringUtils.stripBOM(line);
            if (!StringUtils.isNullOrWhitespace(line)) {
                map.title = line.trim();
            }
        } catch (FileNotFoundException var16) {
        }
    }

    private static void readDescriptionDotTxt(ChooseGameInfo.Map map, String dir, Language language) throws IOException {
        String path = "media/lua/shared/Translate/" + language.toString() + "/" + dir + "/description.txt";
        File file = new File(ZomboidFileSystem.instance.getString(path));

        try (
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName(language.charset()));
            BufferedReader br = new BufferedReader(isr);
        ) {
            map.desc = "";
            boolean bFirst = true;

            String line;
            while ((line = br.readLine()) != null) {
                if (bFirst) {
                    line = StringUtils.stripBOM(line);
                    bFirst = false;
                }

                map.desc = map.desc + line;
            }
        } catch (FileNotFoundException var16) {
        }
    }

    public static ChooseGameInfo.Map getMapDetails(String dir) {
        if (Maps.containsKey(dir)) {
            return Maps.get(dir);
        } else {
            File file = new File(ZomboidFileSystem.instance.getString("media/maps/" + dir + "/map.info"));
            if (!file.exists()) {
                return null;
            } else {
                ChooseGameInfo.Map map = new ChooseGameInfo.Map();
                map.dir = new File(file.getParent()).getAbsolutePath();
                map.title = dir;
                map.lotsDir = new ArrayList<>();

                try {
                    FileReader reader = new FileReader(file.getAbsolutePath());
                    BufferedReader br = new BufferedReader(reader);
                    String inputLine = null;

                    try {
                        while ((inputLine = br.readLine()) != null) {
                            inputLine = inputLine.trim();
                            if (inputLine.startsWith("title=")) {
                                map.title = inputLine.replace("title=", "");
                            } else if (inputLine.startsWith("lots=")) {
                                map.lotsDir.add(inputLine.replace("lots=", "").trim());
                            } else if (inputLine.startsWith("description=")) {
                                if (map.desc == null) {
                                    map.desc = "";
                                }

                                map.desc = map.desc + inputLine.replace("description=", "");
                            } else if (inputLine.startsWith("fixed2x=")) {
                                map.fixed2x = Boolean.parseBoolean(inputLine.replace("fixed2x=", "").trim());
                            } else if (inputLine.startsWith("zoomX=")) {
                                map.zoomX = Float.parseFloat(inputLine.replace("zoomX=", "").trim());
                            } else if (inputLine.startsWith("zoomY=")) {
                                map.zoomY = Float.parseFloat(inputLine.replace("zoomY=", "").trim());
                            } else if (inputLine.startsWith("zoomS=")) {
                                map.zoomS = Float.parseFloat(inputLine.replace("zoomS=", "").trim());
                            } else if (inputLine.startsWith("demoVideo=")) {
                                map.demoVideo = inputLine.replace("demoVideo=", "");
                            }
                        }
                    } catch (IOException var10) {
                        Logger.getLogger(ChooseGameInfo.class.getName()).log(Level.SEVERE, null, var10);
                    }

                    br.close();
                    map.thumb = Texture.getSharedTexture(map.dir + "/thumb.png");
                    String pyramidPath = ZomboidFileSystem.instance.getString(map.dir + "/spawnSelectImagePyramid.zip");
                    if (new File(pyramidPath).exists()) {
                        map.spawnSelectImagePyramid = pyramidPath;
                    } else {
                        map.worldmap = Texture.getSharedTexture(map.dir + "/worldmap.png");
                    }

                    ArrayList<Language> languages = new ArrayList<>();
                    Translator.addLanguageToList(Translator.getLanguage(), languages);
                    Translator.addLanguageToList(Translator.getDefaultLanguage(), languages);

                    for (int i = languages.size() - 1; i >= 0; i--) {
                        Language language = languages.get(i);
                        readTitleDotTxt(map, dir, language);
                        readDescriptionDotTxt(map, dir, language);
                    }
                } catch (Exception var11) {
                    ExceptionLogger.logException(var11);
                    return null;
                }

                Maps.put(dir, map);
                return map;
            }
        }
    }

    public static ChooseGameInfo.Mod getModDetails(String modId) {
        if (MissingMods.contains(modId)) {
            return null;
        } else if (Mods.containsKey(modId)) {
            return Mods.get(modId);
        } else {
            String modDir = ZomboidFileSystem.instance.getModDir(modId);
            if (modDir == null) {
                ArrayList<String> modsFolders = tempStrings;
                ZomboidFileSystem.instance.getAllModFolders(modsFolders);

                for (int i = 0; i < modsFolders.size(); i++) {
                    ChooseGameInfo.Mod mod = readModInfo(modsFolders.get(i));
                    Mods.putIfAbsent(mod.getId(), mod);
                    ZomboidFileSystem.instance.setModIdToDir(mod.getId(), modsFolders.get(i));
                    if (mod.getId().equals(modId)) {
                        return mod;
                    }
                }
            }

            ChooseGameInfo.Mod mod = readModInfo(modDir);
            if (mod == null) {
                MissingMods.add(modId);
            }

            return mod;
        }
    }

    public static ChooseGameInfo.Mod getAvailableModDetails(String modId) {
        ChooseGameInfo.Mod mod = getModDetails(modId);
        return mod != null && mod.isAvailable() ? mod : null;
    }

    public static ChooseGameInfo.Mod readModInfo(String modDir) {
        ChooseGameInfo.Mod mod = readModInfoAux(modDir);
        if (mod != null) {
            ChooseGameInfo.Mod exists = Mods.get(mod.getId());
            if (exists == null) {
                Mods.put(mod.getId(), mod);
            } else if (exists != mod) {
                ZomboidFileSystem.instance.getAllModFolders(tempStrings);
                int index1 = tempStrings.indexOf(mod.getDir());
                int index2 = tempStrings.indexOf(exists.getDir());
                if (index1 < index2) {
                    Mods.put(mod.getId(), mod);
                }
            }
        }

        return mod;
    }

    private static ChooseGameInfo.Mod readModInfoAux(String modDir) {
        if (modDir != null) {
            ChooseGameInfo.Mod mod = ZomboidFileSystem.instance.getModInfoForDir(modDir);
            if (mod.read) {
                return mod.valid ? mod : null;
            }

            mod.read = true;
            String versionSubdir = mod.getVersionDir();
            String commonSubdir = mod.getCommonDir();
            String fileName = versionSubdir + File.separator + "mod.info";
            File file = new File(fileName);
            if (!file.exists()) {
                fileName = commonSubdir + File.separator + "mod.info";
                file = new File(fileName);
                if (!file.exists()) {
                    DebugLog.Mod.warn("can't find \"" + fileName + "\"");
                    return null;
                }
            }

            try (
                InputStreamReader is = IndieFileLoader.getStreamReader(fileName);
                BufferedReader br = new BufferedReader(is);
            ) {
                while (true) {
                    String inputLine;
                    if ((inputLine = br.readLine()) == null) {
                        if (mod.getUrl() == null) {
                            mod.setUrl("");
                        }
                        break;
                    }

                    if (inputLine.contains("name=")) {
                        String nameStr = inputLine.replace("name=", "");
                        if (!StringUtils.isNullOrWhitespace(nameStr)) {
                            mod.name = nameStr;
                        }
                    } else if (inputLine.contains("poster=")) {
                        String texName = inputLine.replace("poster=", "");
                        if (!StringUtils.isNullOrWhitespace(texName)) {
                            String posterName = versionSubdir + File.separator + texName;
                            File posterFile = new File(posterName);
                            if (!posterFile.exists()) {
                                posterName = commonSubdir + File.separator + texName;
                            }

                            mod.posters.add(posterName);
                        }
                    } else if (inputLine.contains("description=")) {
                        mod.desc = mod.desc + inputLine.replace("description=", "");
                    } else if (inputLine.contains("require=")) {
                        mod.setRequire(new ArrayList<>(Arrays.asList(inputLine.replace("require=", "").split(","))));
                    } else if (inputLine.contains("incompatible=")) {
                        mod.setIncompatible(new ArrayList<>(Arrays.asList(inputLine.replace("incompatible=", "").split(","))));
                    } else if (inputLine.contains("loadModAfter=")) {
                        mod.setLoadAfter(new ArrayList<>(Arrays.asList(inputLine.replace("loadModAfter=", "").split(","))));
                    } else if (inputLine.contains("loadModBefore=")) {
                        mod.setLoadBefore(new ArrayList<>(Arrays.asList(inputLine.replace("loadModBefore=", "").split(","))));
                    } else if (inputLine.startsWith("id=")) {
                        String idStr = inputLine.replace("id=", "");
                        if (!StringUtils.isNullOrWhitespace(idStr)) {
                            mod.setId(idStr);
                        }
                    } else if (inputLine.contains("author=")) {
                        mod.setAuthor(inputLine.replace("author=", ""));
                    } else if (inputLine.contains("modversion=")) {
                        mod.setModVersion(inputLine.replace("modversion=", ""));
                    } else if (inputLine.contains("icon=")) {
                        String texName = inputLine.replace("icon=", "");
                        if (!StringUtils.isNullOrWhitespace(texName)) {
                            String iconName = versionSubdir + File.separator + texName;
                            File posterFile = new File(iconName);
                            if (!posterFile.exists()) {
                                iconName = commonSubdir + File.separator + texName;
                            }

                            mod.setIcon(iconName);
                        }
                    } else if (inputLine.contains("category=")) {
                        mod.setCategory(inputLine.replace("category=", ""));
                    } else if (inputLine.contains("url=")) {
                        mod.setUrl(inputLine.replace("url=", ""));
                    } else if (inputLine.contains("pack=")) {
                        String pack = inputLine.replace("pack=", "").trim();
                        if (pack.isEmpty()) {
                            DebugLog.Mod.error("pack= line requires a file name");
                            return null;
                        }

                        int flags = TextureID.useCompressionOption ? 4 : 0;
                        flags |= 64;
                        int p = pack.indexOf("type=");
                        if (p != -1) {
                            String type = pack.substring(p + "type=".length());
                            byte var50 = -1;
                            switch (type.hashCode()) {
                                case 3732:
                                    if (type.equals("ui")) {
                                        var50 = 0;
                                    }
                                default:
                                    switch (var50) {
                                        case 0:
                                            flags = 2;
                                            break;
                                        default:
                                            DebugLog.Mod.error("unknown pack type=" + type);
                                    }

                                    int p1 = pack.indexOf(32);
                                    pack = pack.substring(0, p1).trim();
                            }
                        }

                        String packName = pack;
                        String packSuffix = "";
                        if (pack.endsWith(".floor")) {
                            packName = pack.substring(0, pack.lastIndexOf(46));
                            packSuffix = ".floor";
                            flags &= -5;
                        }

                        int TileScale = 1;
                        if (Core.safeModeForced) {
                            TileScale = 1;
                        }

                        if (TileScale == 2) {
                            File texturePack = new File(
                                versionSubdir
                                    + File.separator
                                    + "media"
                                    + File.separator
                                    + "texturepacks"
                                    + File.separator
                                    + packName
                                    + "2x"
                                    + packSuffix
                                    + ".pack"
                            );
                            if (texturePack.isFile()) {
                                DebugLog.Mod.printf("2x version of %s.pack found.\n", pack);
                                pack = packName + "2x" + packSuffix;
                            } else {
                                texturePack = new File(
                                    versionSubdir + File.separator + "media" + File.separator + "texturepacks" + File.separator + pack + "2x.pack"
                                );
                                if (texturePack.isFile()) {
                                    DebugLog.Mod.printf("2x version of %s.pack found.\n", pack);
                                    pack = pack + "2x";
                                } else {
                                    texturePack = new File(
                                        commonSubdir
                                            + File.separator
                                            + "media"
                                            + File.separator
                                            + "texturepacks"
                                            + File.separator
                                            + packName
                                            + "2x"
                                            + packSuffix
                                            + ".pack"
                                    );
                                    if (texturePack.isFile()) {
                                        DebugLog.Mod.printf("2x version of %s.pack found.\n", pack);
                                        pack = packName + "2x" + packSuffix;
                                    } else {
                                        texturePack = new File(
                                            commonSubdir + File.separator + "media" + File.separator + "texturepacks" + File.separator + pack + "2x.pack"
                                        );
                                        if (texturePack.isFile()) {
                                            DebugLog.Mod.printf("2x version of %s.pack found.\n", pack);
                                            pack = pack + "2x";
                                        } else {
                                            DebugLog.Mod.printf("2x version of %s.pack not found.\n", pack);
                                        }
                                    }
                                }
                            }
                        }

                        mod.addPack(pack, flags);
                    } else if (inputLine.contains("tiledef=")) {
                        String[] ss = inputLine.replace("tiledef=", "").trim().split("\\s+");
                        if (ss.length != 2) {
                            DebugLog.Mod.error("tiledef= line requires file name and file number");
                            return null;
                        }

                        String tileDefName = ss[0];

                        int fileNumber;
                        try {
                            fileNumber = Integer.parseInt(ss[1]);
                        } catch (NumberFormatException var20) {
                            DebugLog.Mod.error("tiledef= line requires file name and file number");
                            return null;
                        }

                        int MIN_TILEDEF_FILE_NUMBER = 100;
                        int MAX_TILEDEF_FILE_NUMBER = 8190;
                        boolean vb = true;
                        int var47 = 16382;
                        if (fileNumber < 100 || fileNumber > var47) {
                            DebugLog.Mod.error("tiledef=%s %d file number must be from %d to %d", tileDefName, fileNumber, 100, Integer.valueOf(var47));
                            return null;
                        }

                        mod.addTileDef(tileDefName, fileNumber);
                    } else if (inputLine.startsWith("versionMax=")) {
                        String versionStr = inputLine.replace("versionMax=", "").trim();
                        if (!versionStr.isEmpty()) {
                            try {
                                mod.versionMax = GameVersion.parse(versionStr);
                            } catch (Exception var18) {
                                DebugLog.Mod.error("invalid versionMax: " + var18.getMessage());
                                return null;
                            }
                        }
                    } else if (inputLine.startsWith("versionMin=")) {
                        String versionStr = inputLine.replace("versionMin=", "").trim();
                        if (!versionStr.isEmpty()) {
                            try {
                                mod.versionMin = GameVersion.parse(versionStr);
                            } catch (Exception var19) {
                                DebugLog.Mod.error("invalid versionMin: " + var19.getMessage());
                                return null;
                            }
                        }
                    }
                }

                mod.valid = true;
                return mod;
            } catch (Exception var23) {
                ExceptionLogger.logException(var23);
            }
        }

        return null;
    }

    public static final class Map {
        private String dir;
        private Texture thumb;
        private Texture worldmap;
        private String spawnSelectImagePyramid;
        private String title;
        private ArrayList<String> lotsDir;
        private float zoomX;
        private float zoomY;
        private float zoomS;
        private String demoVideo;
        private String desc;
        private boolean fixed2x;

        public String getDirectory() {
            return this.dir;
        }

        public void setDirectory(String dir) {
            this.dir = dir;
        }

        public Texture getThumbnail() {
            return this.thumb;
        }

        public void setThumbnail(Texture thumb) {
            this.thumb = thumb;
        }

        public Texture getWorldmap() {
            return this.worldmap;
        }

        public void setWorldmap(Texture worldmap) {
            this.worldmap = worldmap;
        }

        public String getSpawnSelectImagePyramid() {
            return this.spawnSelectImagePyramid;
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public ArrayList<String> getLotDirectories() {
            return this.lotsDir;
        }

        public float getZoomX() {
            return this.zoomX;
        }

        public void setZoomX(float newX) {
            this.zoomX = newX;
        }

        public float getZoomY() {
            return this.zoomY;
        }

        public void setZoomY(float newY) {
            this.zoomY = newY;
        }

        public float getZoomS() {
            return this.zoomS;
        }

        public void setZoomS(float newS) {
            this.zoomS = newS;
        }

        public String getDemoVideo() {
            return this.demoVideo;
        }

        public void setDemoVideo(String video) {
            this.demoVideo = video;
        }

        public String getDescription() {
            return this.desc;
        }

        public void setDescription(String desc) {
            this.desc = desc;
        }

        public boolean isFixed2x() {
            return this.fixed2x;
        }

        public void setFixed2x(boolean fixed) {
            this.fixed2x = fixed;
        }
    }

    @UsedFromLua
    public static final class Mod {
        public String dir;
        public String commonDir;
        public String versionDir;
        public final ZomboidFileSystem.PZModFolder baseFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder mediaFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder actionGroupsFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder animSetsFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder animsXFile = new ZomboidFileSystem.PZModFolder();
        private final ArrayList<String> posters = new ArrayList<>();
        public Texture tex;
        private ArrayList<String> require;
        private ArrayList<String> incompatible;
        private ArrayList<String> loadAfter;
        private ArrayList<String> loadBefore;
        private String name = "Unnamed Mod";
        private String desc = "";
        private String id = "undefined_id";
        private String url;
        private String author;
        private String modVersion;
        private String icon;
        private String category;
        private String workshopId = "";
        private boolean availableDone;
        private boolean available = true;
        private GameVersion versionMin;
        private GameVersion versionMax;
        private final ArrayList<ChooseGameInfo.PackFile> packs = new ArrayList<>();
        private final ArrayList<ChooseGameInfo.TileDef> tileDefs = new ArrayList<>();
        private boolean read;
        private boolean valid;

        public Mod(String dir) {
            this.dir = dir;
            File commonFile = new File(dir, "common");
            File versionFile = ZomboidFileSystem.instance.getModVersionFile(dir);
            this.commonDir = commonFile.getAbsolutePath();
            this.versionDir = versionFile == null ? "" : versionFile.getAbsolutePath();
            this.baseFile.setWithCatch(commonFile, versionFile);
            this.mediaFile.setWithCatch(new File(this.baseFile.common.canonicalFile, "media"), new File(this.baseFile.version.canonicalFile, "media"));
            this.actionGroupsFile
                .setWithCatch(new File(this.mediaFile.common.canonicalFile, "actiongroups"), new File(this.mediaFile.version.canonicalFile, "actiongroups"));
            this.animSetsFile
                .setWithCatch(new File(this.mediaFile.common.canonicalFile, "AnimSets"), new File(this.mediaFile.version.canonicalFile, "AnimSets"));
            this.animsXFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "anims_X"), new File(this.mediaFile.version.canonicalFile, "anims_X"));
            File file = this.baseFile.common.canonicalFile.getParentFile();
            if (file != null) {
                file = file.getParentFile();
                if (file != null) {
                    this.workshopId = SteamWorkshop.instance.getIDFromItemInstallFolder(file.getAbsolutePath());
                    if (this.workshopId == null) {
                        this.workshopId = "";
                    }
                }
            }
        }

        public Texture getTexture() {
            if (this.tex == null) {
                String texName = this.posters.isEmpty() ? null : this.posters.get(0);
                if (!StringUtils.isNullOrWhitespace(texName)) {
                    this.tex = Texture.getSharedTexture(texName);
                }

                if (this.tex == null || this.tex.isFailure()) {
                    if (Core.debug && this.tex == null) {
                        DebugLog.Mod.println("failed to load poster " + (texName == null ? this.id : texName));
                    }

                    this.tex = Texture.getWhite();
                }
            }

            return this.tex;
        }

        public void setTexture(Texture tex) {
            this.tex = tex;
        }

        public int getPosterCount() {
            return this.posters.size();
        }

        public String getPoster(int index) {
            return index >= 0 && index < this.posters.size() ? this.posters.get(index) : null;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDir() {
            return this.dir;
        }

        public String getCommonDir() {
            return this.commonDir;
        }

        public String getVersionDir() {
            return this.versionDir;
        }

        public String getDescription() {
            return this.desc;
        }

        public ArrayList<String> getRequire() {
            return this.require;
        }

        public void setRequire(ArrayList<String> require) {
            this.require = require;
        }

        public ArrayList<String> getIncompatible() {
            return this.incompatible;
        }

        public void setIncompatible(ArrayList<String> incompatible) {
            this.incompatible = incompatible;
        }

        public ArrayList<String> getLoadAfter() {
            return this.loadAfter;
        }

        public void setLoadAfter(ArrayList<String> loadAfter) {
            this.loadAfter = loadAfter;
        }

        public ArrayList<String> getLoadBefore() {
            return this.loadBefore;
        }

        public void setLoadBefore(ArrayList<String> loadBefore) {
            this.loadBefore = loadBefore;
        }

        public String getId() {
            return this.workshopId + "\\" + this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isAvailable() {
            if (this.availableDone) {
                return this.available;
            } else {
                this.availableDone = true;
                if (!this.isAvailableSelf()) {
                    this.available = false;
                    return false;
                } else {
                    ChooseGameInfo.tempStrings.clear();
                    ChooseGameInfo.tempStrings.add(this.getId());
                    if (!this.isAvailableRequired(ChooseGameInfo.tempStrings)) {
                        this.available = false;
                        return false;
                    } else {
                        this.available = true;
                        return true;
                    }
                }
            }
        }

        public boolean isAvailableSelf() {
            GameVersion gameVersion = Core.getInstance().getGameVersion();
            return this.versionMin != null && this.versionMin.isGreaterThan(gameVersion)
                ? false
                : this.versionMax == null || !this.versionMax.isLessThan(gameVersion);
        }

        private boolean isAvailableRequired(ArrayList<String> seen) {
            if (this.require != null && !this.require.isEmpty()) {
                for (int i = 0; i < this.require.size(); i++) {
                    String modID = this.require.get(i).trim();
                    if (!seen.contains(modID)) {
                        seen.add(modID);
                        ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails(modID);
                        if (mod == null) {
                            return false;
                        }

                        if (!mod.isAvailableSelf()) {
                            return false;
                        }

                        if (!mod.isAvailableRequired(seen)) {
                            return false;
                        }
                    }
                }

                return true;
            } else {
                return true;
            }
        }

        @Deprecated
        public void setAvailable(boolean available) {
        }

        public String getUrl() {
            return this.url == null ? "" : this.url;
        }

        public void setUrl(String url) {
            if (url.startsWith("http://theindiestone.com") || url.startsWith("http://www.theindiestone.com") || url.startsWith("https://discord.gg")) {
                this.url = url;
            }
        }

        public String getAuthor() {
            return this.author == null ? "" : this.author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getModVersion() {
            return this.modVersion == null ? "" : this.modVersion;
        }

        public void setModVersion(String version) {
            this.modVersion = version;
        }

        public String getIcon() {
            return this.icon == null ? "" : this.icon;
        }

        public void setIcon(String name) {
            this.icon = name;
        }

        public String getCategory() {
            return this.category == null ? "" : this.category;
        }

        public void setCategory(String name) {
            this.category = name;
        }

        public GameVersion getVersionMin() {
            return this.versionMin;
        }

        public GameVersion getVersionMax() {
            return this.versionMax;
        }

        public void addPack(String name, int flags) {
            this.packs.add(new ChooseGameInfo.PackFile(name, flags));
        }

        public void addTileDef(String name, int fileNumber) {
            this.tileDefs.add(new ChooseGameInfo.TileDef(name, fileNumber));
        }

        public ArrayList<ChooseGameInfo.PackFile> getPacks() {
            return this.packs;
        }

        public ArrayList<ChooseGameInfo.TileDef> getTileDefs() {
            return this.tileDefs;
        }

        public String getWorkshopID() {
            return this.workshopId;
        }
    }

    public static final class PackFile {
        public final String name;
        public final int flags;

        public PackFile(String name, int flags) {
            this.name = name;
            this.flags = flags;
        }
    }

    public static final class SpawnOrigin {
        public int x;
        public int y;
        public int w;
        public int h;

        public SpawnOrigin(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public static final class TileDef {
        public String name;
        public int fileNumber;

        public TileDef(String name, int fileNumber) {
            this.name = name;
            this.fileNumber = fileNumber;
        }
    }
}
