// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.PNGDecoder;
import zombie.util.StringUtils;

@UsedFromLua
public class SteamWorkshopItem {
    private final String workshopFolder;
    private String publishedFileId;
    private String title = "";
    private String description = "";
    private String visibility = "public";
    private final ArrayList<String> tags = new ArrayList<>();
    private String changeNote = "";
    private boolean hasMod;
    private boolean hasMap;
    private final ArrayList<String> modIds = new ArrayList<>();
    private final ArrayList<String> mapFolders = new ArrayList<>();
    private static final int VERSION1 = 1;
    private static final int LATEST_VERSION = 1;

    public SteamWorkshopItem(String workshopFolder) {
        this.workshopFolder = workshopFolder;
    }

    public String getContentFolder() {
        return this.workshopFolder + File.separator + "Contents";
    }

    public String getFolderName() {
        return new File(this.workshopFolder).getName();
    }

    public void setID(String ID) {
        if (ID != null && !SteamUtils.isValidSteamID(ID)) {
            ID = null;
        }

        this.publishedFileId = ID;
    }

    public String getID() {
        return this.publishedFileId;
    }

    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }

        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setDescription(String description) {
        if (description == null) {
            description = "";
        }

        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVisibility() {
        return this.visibility;
    }

    public void setVisibilityInteger(int v) {
        switch (v) {
            case 0:
                this.visibility = "public";
                break;
            case 1:
                this.visibility = "friendsOnly";
                break;
            case 2:
                this.visibility = "private";
                break;
            case 3:
                this.visibility = "unlisted";
                break;
            default:
                this.visibility = "public";
        }
    }

    public int getVisibilityInteger() {
        if ("public".equals(this.visibility)) {
            return 0;
        } else if ("friendsOnly".equals(this.visibility)) {
            return 1;
        } else if ("private".equals(this.visibility)) {
            return 2;
        } else {
            return "unlisted".equals(this.visibility) ? 3 : 0;
        }
    }

    public void setTags(ArrayList<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public static ArrayList<String> getAllowedTags() {
        ArrayList<String> tags = new ArrayList<>();
        File file = ZomboidFileSystem.instance.getMediaFile("WorkshopTags.txt");

        String line;
        try (
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
        ) {
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    tags.add(line);
                }
            }
        } catch (Exception var10) {
            ExceptionLogger.logException(var10);
        }

        return tags;
    }

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public String getSubmitDescription() {
        String s = this.getDescription();
        if (!s.isEmpty()) {
            s = s + "\n\n";
        }

        s = s + "Workshop ID: " + this.getID();

        for (int i = 0; i < this.modIds.size(); i++) {
            s = s + "\nMod ID: " + this.modIds.get(i);
        }

        for (int i = 0; i < this.mapFolders.size(); i++) {
            s = s + "\nMap Folder: " + this.mapFolders.get(i);
        }

        return s;
    }

    public String[] getSubmitTags() {
        ArrayList<String> tags = new ArrayList<>();
        tags.addAll(this.tags);
        return tags.toArray(new String[tags.size()]);
    }

    public String getPreviewImage() {
        return this.workshopFolder + File.separator + "preview.png";
    }

    public void setChangeNote(String changeNote) {
        if (changeNote == null) {
            changeNote = "";
        }

        this.changeNote = changeNote;
    }

    public String getChangeNote() {
        return this.changeNote;
    }

    public boolean create() {
        return SteamWorkshop.instance.CreateWorkshopItem(this);
    }

    public boolean submitUpdate() {
        return SteamWorkshop.instance.SubmitWorkshopItem(this);
    }

    public boolean getUpdateProgress(KahluaTable table) {
        if (table == null) {
            throw new NullPointerException("table is null");
        } else {
            long[] progress = new long[2];
            if (SteamWorkshop.instance.GetItemUpdateProgress(progress)) {
                System.out.println(progress[0] + "/" + progress[1]);
                table.rawset("processed", (double)progress[0]);
                table.rawset("total", (double)Math.max(progress[1], 1L));
                return true;
            } else {
                return false;
            }
        }
    }

    public int getUpdateProgressTotal() {
        return 1;
    }

    private String validateFileTypes(Path dir) {
        try {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
                for (Path path : dstrm) {
                    if (Files.isDirectory(path)) {
                        String err = this.validateFileTypes(path);
                        if (err != null) {
                            return err;
                        }
                    } else {
                        String fileName = path.getFileName().toString();
                        if (!StringUtils.endsWithIgnoreCase(fileName, "pyramid.zip")
                            && (
                                fileName.endsWith(".exe")
                                    || fileName.endsWith(".dll")
                                    || fileName.endsWith(".bat")
                                    || fileName.endsWith(".app")
                                    || fileName.endsWith(".dylib")
                                    || fileName.endsWith(".sh")
                                    || fileName.endsWith(".so")
                                    || fileName.endsWith(".zip")
                            )) {
                            return "FileTypeNotAllowed";
                        }
                    }
                }
            }

            return null;
        } catch (Exception var9) {
            ExceptionLogger.logException(var9);
            return "IOError";
        }
    }

    private String validateModDotInfo(Path path) {
        String modID = null;

        String line;
        try (
            FileReader fr = new FileReader(path.toFile());
            BufferedReader br = new BufferedReader(fr);
        ) {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id=")) {
                    modID = line.replace("id=", "").trim();
                    break;
                }
            }
        } catch (FileNotFoundException var11) {
            return "MissingModDotInfo";
        } catch (IOException var12) {
            ExceptionLogger.logException(var12);
            return "IOError";
        }

        if (modID != null && !modID.isEmpty()) {
            this.modIds.add(modID);
            return null;
        } else {
            return "InvalidModDotInfo";
        }
    }

    private String validateMapDotInfo(Path path) {
        return null;
    }

    private String validateMapFolder(Path dir) {
        boolean foundMapDotInfo = false;

        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
            for (Path path : dstrm) {
                if (!Files.isDirectory(path) && "map.info".equals(path.getFileName().toString())) {
                    String err = this.validateMapDotInfo(path);
                    if (err != null) {
                        return err;
                    }

                    foundMapDotInfo = true;
                }
            }
        } catch (Exception var10) {
            ExceptionLogger.logException(var10);
            return "IOError";
        }

        if (!foundMapDotInfo) {
            return "MissingMapDotInfo";
        } else {
            this.mapFolders.add(dir.getFileName().toString());
            return null;
        }
    }

    private String validateMapsFolder(Path dir) {
        boolean atLeastOneMap = false;

        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
            for (Path path : dstrm) {
                if (Files.isDirectory(path)) {
                    String err = this.validateMapFolder(path);
                    if (err != null) {
                        return err;
                    }

                    atLeastOneMap = true;
                }
            }
        } catch (Exception var10) {
            ExceptionLogger.logException(var10);
            return "IOError";
        }

        if (!atLeastOneMap) {
            return null;
        } else {
            this.hasMap = true;
            return null;
        }
    }

    private String validateMediaFolder(Path dir) {
        try {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
                for (Path path : dstrm) {
                    if (Files.isDirectory(path) && "maps".equals(path.getFileName().toString())) {
                        String err = this.validateMapsFolder(path);
                        if (err != null) {
                            return err;
                        }
                    }
                }
            }

            return null;
        } catch (Exception var9) {
            ExceptionLogger.logException(var9);
            return "IOError";
        }
    }

    private String validateModFolder(Path dir) {
        boolean foundModDotInfo = false;

        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
            for (Path path : dstrm) {
                if (Files.isDirectory(path)) {
                    if ("common".equals(path.getFileName().toString())) {
                        Path path2 = path.resolve("media");
                        if (Files.exists(path2)) {
                            String err = this.validateMediaFolder(path2);
                            if (err != null) {
                                return err;
                            }
                        }

                        Path path3 = path.resolve("mod.info");
                        if (Files.exists(path3)) {
                            String err = this.validateModDotInfo(path3);
                            if (err != null) {
                                return err;
                            }

                            foundModDotInfo = true;
                        }
                    } else {
                        Integer ver = ZomboidFileSystem.instance.getGameVersionIntFromName(path.getFileName().toString());
                        if (ver != null && ver >= 42000 && ver <= Core.getInstance().getGameVersion().getInt()) {
                            Path path2x = path.resolve("media");
                            if (Files.exists(path2x)) {
                                String err = this.validateMediaFolder(path2x);
                                if (err != null) {
                                    return err;
                                }
                            }

                            Path path3 = path.resolve("mod.info");
                            if (Files.exists(path3)) {
                                String err = this.validateModDotInfo(path3);
                                if (err != null) {
                                    return err;
                                }

                                foundModDotInfo = true;
                            }
                        }
                    }
                }
            }

            return !foundModDotInfo ? "MissingModDotInfo" : null;
        } catch (Exception var13) {
            ExceptionLogger.logException(var13);
            return "IOError";
        }
    }

    private String validateModsFolder(Path dir) {
        boolean atLeastOneMod = false;

        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
            for (Path path : dstrm) {
                if (!Files.isDirectory(path)) {
                    return "FileNotAllowedInMods";
                }

                String err = this.validateModFolder(path);
                if (err != null) {
                    return err;
                }

                atLeastOneMod = true;
            }
        } catch (Exception var10) {
            ExceptionLogger.logException(var10);
            return "IOError";
        }

        if (!atLeastOneMod) {
            return "EmptyModsFolder";
        } else {
            this.hasMod = true;
            return null;
        }
    }

    private String validateBuildingsFolder(Path dir) {
        return null;
    }

    private String validateCreativeFolder(Path dir) {
        return null;
    }

    public String validatePreviewImage(Path path) throws IOException {
        if (Files.exists(path) && Files.isReadable(path) && !Files.isDirectory(path)) {
            if (Files.size(path) > 1024000L) {
                return "PreviewFileSize";
            } else {
                try {
                    try (
                        FileInputStream fis = new FileInputStream(path.toFile());
                        BufferedInputStream bis = new BufferedInputStream(fis);
                    ) {
                        PNGDecoder png = new PNGDecoder(bis, false);
                        if (png.getWidth() != 256 || png.getHeight() != 256) {
                            return "PreviewDimensions";
                        }
                    }

                    return null;
                } catch (IOException var10) {
                    ExceptionLogger.logException(var10);
                    return "PreviewFormat";
                }
            }
        } else {
            return "PreviewNotFound";
        }
    }

    public String validateContents() {
        this.hasMod = false;
        this.hasMap = false;
        this.modIds.clear();
        this.mapFolders.clear();

        try {
            Path dir = FileSystems.getDefault().getPath(this.getContentFolder());
            if (!Files.isDirectory(dir)) {
                return "MissingContents";
            } else {
                Path path0 = FileSystems.getDefault().getPath(this.getPreviewImage());
                String err = this.validatePreviewImage(path0);
                if (err != null) {
                    return err;
                } else {
                    boolean atLeastOneFolder = false;

                    try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir)) {
                        for (Path path : dstrm) {
                            if (!Files.isDirectory(path)) {
                                return "FileNotAllowedInContents";
                            }

                            if ("buildings".equals(path.getFileName().toString())) {
                                err = this.validateBuildingsFolder(path);
                                if (err != null) {
                                    return err;
                                }
                            } else if ("creative".equals(path.getFileName().toString())) {
                                err = this.validateCreativeFolder(path);
                                if (err != null) {
                                    return err;
                                }
                            } else {
                                if (!"mods".equals(path.getFileName().toString())) {
                                    return "FolderNotAllowedInContents";
                                }

                                err = this.validateModsFolder(path);
                                if (err != null) {
                                    return err;
                                }
                            }

                            atLeastOneFolder = true;
                        }

                        return !atLeastOneFolder ? "EmptyContentsFolder" : this.validateFileTypes(dir);
                    } catch (Exception var11) {
                        ExceptionLogger.logException(var11);
                        return "IOError";
                    }
                }
            }
        } catch (IOException var12) {
            ExceptionLogger.logException(var12);
            return "IOError";
        }
    }

    public String getExtendedErrorInfo(String error) {
        if ("FolderNotAllowedInContents".equals(error)) {
            return "buildings/ creative/ mods/";
        } else {
            return "FileTypeNotAllowed".equals(error) ? "*.exe *.dll *.bat *.app *.dylib *.sh *.so *.zip" : "";
        }
    }

    public boolean readWorkshopTxt() {
        String fileName = this.workshopFolder + File.separator + "workshop.txt";
        if (!new File(fileName).exists()) {
            return true;
        } else {
            try {
                boolean var12;
                try (
                    FileReader fr = new FileReader(fileName);
                    BufferedReader br = new BufferedReader(fr);
                ) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("//")) {
                            if (line.startsWith("id=")) {
                                String ID = line.replace("id=", "");
                                this.setID(ID);
                            } else if (line.startsWith("description=")) {
                                if (!this.description.isEmpty()) {
                                    this.description = this.description + "\n";
                                }

                                this.description = this.description + line.replace("description=", "");
                            } else if (line.startsWith("tags=")) {
                                this.tags.addAll(Arrays.asList(line.replace("tags=", "").split(";")));
                            } else if (line.startsWith("title=")) {
                                this.title = line.replace("title=", "");
                            } else if (!line.startsWith("version=") && line.startsWith("visibility=")) {
                                this.visibility = line.replace("visibility=", "");
                            }
                        }
                    }

                    var12 = true;
                }

                return var12;
            } catch (IOException var10) {
                ExceptionLogger.logException(var10);
                return false;
            }
        }
    }

    public boolean writeWorkshopTxt() {
        String fileName = this.workshopFolder + File.separator + "workshop.txt";
        File file = new File(fileName);

        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("version=1");
            bw.newLine();
            bw.write("id=" + (this.publishedFileId == null ? "" : this.publishedFileId));
            bw.newLine();
            bw.write("title=" + this.title);
            bw.newLine();

            for (String s : this.description.split("\n")) {
                bw.write("description=" + s);
                bw.newLine();
            }

            String tagStr = "";

            for (int i = 0; i < this.tags.size(); i++) {
                tagStr = tagStr + this.tags.get(i);
                if (i < this.tags.size() - 1) {
                    tagStr = tagStr + ";";
                }
            }

            bw.write("tags=" + tagStr);
            bw.newLine();
            bw.write("visibility=" + this.visibility);
            bw.newLine();
            bw.close();
            return true;
        } catch (IOException var9) {
            ExceptionLogger.logException(var9);
            return false;
        }
    }

    public static enum ItemState {
        None(0),
        Subscribed(1),
        LegacyItem(2),
        Installed(4),
        NeedsUpdate(8),
        Downloading(16),
        DownloadPending(32);

        private final int value;

        private ItemState(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public boolean and(SteamWorkshopItem.ItemState other) {
            return (this.value & other.value) != 0;
        }

        public boolean and(long bits) {
            return (this.value & bits) != 0L;
        }

        public boolean not(long bits) {
            return (this.value & bits) == 0L;
        }

        public static String toString(long bits) {
            if (bits == None.getValue()) {
                return "None";
            } else {
                StringBuilder s = new StringBuilder();

                for (SteamWorkshopItem.ItemState e : values()) {
                    if (e != None && e.and(bits)) {
                        if (!s.isEmpty()) {
                            s.append("|");
                        }

                        s.append(e.name());
                    }
                }

                return s.toString();
            }
        }
    }
}
