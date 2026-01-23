// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.backup;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.network.CoopSlave;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

public class ZipBackup {
    private static final int compressionMethod = 0;
    static ParallelScatterZipCreator scatterZipCreator;
    private static long lastBackupTime;

    public static void onStartup() {
        lastBackupTime = System.currentTimeMillis();
        if (ServerOptions.getInstance().backupsOnStart.getValue()) {
            makeBackupFile(GameServer.serverName, ZipBackup.BackupTypes.startup);
        }
    }

    public static void onVersion() {
        if (ServerOptions.getInstance().backupsOnVersionChange.getValue()) {
            String serverVersionFile = ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + "last_server_version.txt";
            String serverVersion = getStringFromZip(serverVersionFile);
            String gameVersion = Core.getInstance().getGameVersion().toString();
            if (!gameVersion.equals(serverVersion)) {
                putTextFile(serverVersionFile, gameVersion);
                makeBackupFile(GameServer.serverName, ZipBackup.BackupTypes.version);
            }
        }
    }

    public static void onPeriod() {
        int period = ServerOptions.getInstance().backupsPeriod.getValue();
        if (period > 0) {
            if (System.currentTimeMillis() - lastBackupTime > period * 60000) {
                lastBackupTime = System.currentTimeMillis();
                makeBackupFile(GameServer.serverName, ZipBackup.BackupTypes.period);
            }
        }
    }

    public static void makeBackupFile(String ServerName, ZipBackup.BackupTypes type) {
        String backupDir = ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + type.name();
        long startTime = System.currentTimeMillis();
        DebugLog.DetailedInfo.trace("Start making backup to: " + backupDir);
        scatterZipCreator = new ParallelScatterZipCreator();
        CoopSlave.status("UI_ServerStatus_CreateBackup");
        OutputStream outputStream = null;
        ZipArchiveOutputStream zipArchiveOutputStream = null;

        try {
            File backupsDir = new File(backupDir);
            if (!backupsDir.exists()) {
                backupsDir.mkdirs();
            }

            rotateBackupFile(type);
            String zipname = backupDir + File.separator + "backup_1.zip";

            try {
                Files.deleteIfExists(Paths.get(zipname));
            } catch (IOException var16) {
                var16.printStackTrace();
            }

            File zipFile = new File(zipname);
            zipFile.delete();
            outputStream = new FileOutputStream(zipFile);
            zipArchiveOutputStream = new ZipArchiveOutputStream(outputStream);
            zipArchiveOutputStream.setUseZip64(Zip64Mode.AsNeeded);
            zipArchiveOutputStream.setMethod(0);
            zipArchiveOutputStream.setLevel(0);
            zipTextFile("readme.txt", getBackupReadme(ServerName));
            zipArchiveOutputStream.setComment(getBackupReadme(ServerName));
            zipFile("options.ini", "options.ini");
            zipFile("popman-options.ini", "popman-options.ini");
            zipFile("latestSave.ini", "latestSave.ini");
            zipFile("debug-options.ini", "debug-options.ini");
            zipFile("sounds.ini", "sounds.ini");
            zipFile("gamepadBinding.config", "gamepadBinding.config");
            zipDir("mods", "mods");
            zipDir("Lua", "Lua");
            zipDir("db", "db");
            zipDir("Server", "Server");
            synchronized (IsoChunk.WriteLock) {
                zipDir(
                    "Saves" + File.separator + "Multiplayer" + File.separator + ServerName,
                    "Saves" + File.separator + "Multiplayer" + File.separator + ServerName
                );

                try {
                    scatterZipCreator.writeTo(zipArchiveOutputStream);
                    DebugLog.log(scatterZipCreator.getStatisticsMessage().toString());
                    zipArchiveOutputStream.close();
                    outputStream.close();
                } catch (IOException var14) {
                    var14.printStackTrace();
                }
            }
        } catch (Exception var17) {
            var17.printStackTrace();
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException var13) {
                    var13.printStackTrace();
                }
            }
        }

        DebugLog.log("Backup made in " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private static void rotateBackupFile(ZipBackup.BackupTypes type) {
        int countBackupFiles = ServerOptions.getInstance().backupsCount.getValue() - 1;
        if (countBackupFiles > 0) {
            Path oldestURI = Paths.get(
                ZomboidFileSystem.instance.getCacheDir()
                    + File.separator
                    + "backups"
                    + File.separator
                    + type
                    + File.separator
                    + "backup_"
                    + (countBackupFiles + 1)
                    + ".zip"
            );

            try {
                Files.deleteIfExists(oldestURI);
            } catch (IOException var8) {
                var8.printStackTrace();
            }

            for (int i = countBackupFiles; i > 0; i--) {
                Path sourceURI = Paths.get(
                    ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + type + File.separator + "backup_" + i + ".zip"
                );
                Path destinationURI = Paths.get(
                    ZomboidFileSystem.instance.getCacheDir()
                        + File.separator
                        + "backups"
                        + File.separator
                        + type
                        + File.separator
                        + "backup_"
                        + (i + 1)
                        + ".zip"
                );

                try {
                    Files.move(sourceURI, destinationURI);
                } catch (Exception var7) {
                }
            }
        }
    }

    private static String getBackupReadme(String ServerName) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        formatter.format(date);
        int saveWorldVersion = getWorldVersion(ServerName);
        String saveWorldVersionDescription = "";
        if (saveWorldVersion == -2) {
            saveWorldVersionDescription = "World isn't exist";
        } else if (saveWorldVersion == -1) {
            saveWorldVersionDescription = "World version cannot be determined";
        } else {
            saveWorldVersionDescription = String.valueOf(saveWorldVersion);
        }

        return "Backup time: "
            + formatter.format(date)
            + "\nServerName: "
            + ServerName
            + "\nCurrent server version:"
            + Core.getInstance().getGameVersion()
            + "\nCurrent world version:241\nWorld version in this backup is:"
            + saveWorldVersionDescription;
    }

    private static int getWorldVersion(String ServerName) {
        File inFile = new File(
            ZomboidFileSystem.instance.getSaveDir() + File.separator + "Multiplayer" + File.separator + ServerName + File.separator + "map_t.bin"
        );
        if (inFile.exists()) {
            try {
                byte var15;
                try (
                    FileInputStream inStream = new FileInputStream(inFile);
                    DataInputStream input = new DataInputStream(inStream);
                ) {
                    byte b1 = input.readByte();
                    byte b2 = input.readByte();
                    byte b3 = input.readByte();
                    byte b4 = input.readByte();
                    if (b1 == 71 && b2 == 77 && b3 == 84 && b4 == 77) {
                        return input.readInt();
                    }

                    var15 = -1;
                }

                return var15;
            } catch (Exception var14) {
                var14.printStackTrace();
            }
        }

        return -2;
    }

    private static void putTextFile(String filename, String text) {
        try {
            Path pathFile = Paths.get(filename);
            Files.createDirectories(pathFile.getParent());

            try {
                Files.delete(pathFile);
            } catch (Exception var4) {
            }

            Files.write(pathFile, text.getBytes());
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    private static String getStringFromZip(String filename) {
        String content = null;

        try {
            Path pathFile = Paths.get(filename);
            if (Files.exists(pathFile)) {
                List<String> lines = Files.readAllLines(pathFile);
                content = lines.get(0);
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return content;
    }

    private static void zipTextFile(String filename, String text) {
        InputStreamSupplier streamSupplier = () -> {
            InputStream is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            return is;
        };
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(filename);
        zipArchiveEntry.setMethod(0);
        scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
    }

    private static void zipFile(String filename, String infile) {
        Path pathFile = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + infile);
        if (Files.exists(pathFile)) {
            InputStreamSupplier streamSupplier = () -> {
                InputStream is = null;

                try {
                    is = Files.newInputStream(pathFile);
                } catch (IOException var3x) {
                    var3x.printStackTrace();
                }

                return is;
            };
            ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(filename);
            zipArchiveEntry.setMethod(0);
            scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
        }
    }

    private static void zipDir(String filename, String indir) {
        Path pathDir = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + indir);
        if (Files.exists(pathDir)) {
            try {
                File srcFolder = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + indir);
                if (srcFolder.isDirectory()) {
                    Iterator<File> fileIterator = Arrays.asList(srcFolder.listFiles()).iterator();
                    int srcFolderLength = srcFolder.getAbsolutePath().length() + 1;

                    while (fileIterator.hasNext()) {
                        File file = fileIterator.next();
                        if (!file.isDirectory()) {
                            String relativePath = file.getAbsolutePath().substring(srcFolderLength);
                            InputStreamSupplier streamSupplier = () -> {
                                InputStream is = null;

                                try {
                                    is = Files.newInputStream(file.toPath());
                                } catch (IOException var3x) {
                                    var3x.printStackTrace();
                                }

                                return is;
                            };
                            ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(filename + File.separator + relativePath);
                            zipArchiveEntry.setMethod(0);
                            scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
                        }
                    }
                }
            } catch (Exception var10) {
                var10.printStackTrace();
            }
        }
    }

    private static enum BackupTypes {
        period,
        startup,
        version;
    }
}
