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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.jspecify.annotations.NonNull;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoChunk;
import zombie.network.CoopSlave;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.savefile.ServerPlayerDB;

public class ZipBackup {
    private static final int compressionMethod = 0;
    static ParallelScatterZipCreator scatterZipCreator;
    private static long lastBackupTime;
    private static Thread backupThread;

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
            if (System.currentTimeMillis() - lastBackupTime > period * 60000L && (backupThread == null || !backupThread.isAlive())) {
                lastBackupTime = System.currentTimeMillis();
                backupThread = new Thread(ThreadGroups.Workers, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            this.runInner();
                        } catch (Throwable var2) {
                            ExceptionLogger.logException(var2);
                        }
                    }

                    private void runInner() {
                        ServerPlayerDB.getInstance().saveFinishWait();
                        ZipBackup.makeBackupFile(GameServer.serverName, ZipBackup.BackupTypes.period);
                    }
                });
                backupThread.setDaemon(true);
                backupThread.start();
            }
        }
    }

    public static void waitFinish() {
        if (backupThread != null) {
            while (backupThread.isAlive()) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException var1) {
                    throw new RuntimeException(var1);
                }
            }
        }
    }

    public static boolean isRunning() {
        return backupThread != null && backupThread.isAlive();
    }

    private static void makeBackupFile(String serverName, ZipBackup.BackupTypes type) {
        String backupDir = ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + type.name();
        long startTime = System.currentTimeMillis();
        DebugType.DetailedInfo.trace("Start making backup to: " + backupDir);
        scatterZipCreator = new ParallelScatterZipCreator();
        CoopSlave.status("UI_ServerStatus_CreateBackup");
        OutputStream outputStream = null;

        try {
            File backupsDir = new File(backupDir);
            if (!backupsDir.exists()) {
                backupsDir.mkdirs();
            }

            rotateBackupFile(type);
            String zipname = backupDir + File.separator + "backup_1.zip";

            try {
                Files.deleteIfExists(Paths.get(zipname));
            } catch (IOException var17) {
                DebugType.General.printException(var17, LogSeverity.Error);
            }

            File zipFile = new File(zipname);
            zipFile.delete();
            outputStream = new FileOutputStream(zipFile);
            ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputStream);
            zipArchiveOutputStream.setUseZip64(Zip64Mode.AsNeeded);
            zipArchiveOutputStream.setMethod(0);
            zipArchiveOutputStream.setLevel(0);
            zipTextFile("readme.txt", getBackupReadme(serverName));
            zipArchiveOutputStream.setComment(getBackupReadme(serverName));
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
                String saveDir = "Saves" + File.separator + "Multiplayer" + File.separator + serverName;
                zipDir(saveDir, saveDir, path -> !path.endsWith("blam"));

                try {
                    scatterZipCreator.writeTo(zipArchiveOutputStream);
                    DebugLog.log(scatterZipCreator.getStatisticsMessage().toString());
                    zipArchiveOutputStream.close();
                    outputStream.close();
                } catch (IOException var15) {
                    DebugType.General.printException(var15, LogSeverity.Error);
                }
            }
        } catch (Exception var18) {
            DebugType.General.printException(var18, LogSeverity.Error);
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException var14) {
                    DebugType.General.printException(var14, LogSeverity.Error);
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
                DebugType.General.printException(var8, LogSeverity.Error);
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

    private static String getBackupReadme(String serverName) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        formatter.format(date);
        int saveWorldVersion = getWorldVersion(serverName);
        String saveWorldVersionDescription;
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
            + serverName
            + "\nCurrent server version:"
            + Core.getInstance().getGameVersion()
            + "\nCurrent world version:249\nWorld version in this backup is:"
            + saveWorldVersionDescription;
    }

    private static int getWorldVersion(String serverName) {
        File inFile = new File(
            ZomboidFileSystem.instance.getSaveDir() + File.separator + "Multiplayer" + File.separator + serverName + File.separator + "map_t.bin"
        );
        if (inFile.exists()) {
            try {
                byte var14;
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

                    var14 = -1;
                }

                return var14;
            } catch (Exception var13) {
                DebugType.General.printException(var13, LogSeverity.Error);
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
            DebugType.General.printException(var5, LogSeverity.Error);
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
            DebugType.General.printException(var4, LogSeverity.Error);
        }

        return content;
    }

    private static void zipTextFile(String filename, String text) {
        InputStreamSupplier streamSupplier = () -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
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
                    DebugType.General.printException(var3x, LogSeverity.Error);
                }

                return is;
            };
            ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(filename);
            zipArchiveEntry.setMethod(0);
            scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
        }
    }

    private static void zipDir(String filename, String indir) {
        zipDir(filename, indir, var0x -> false);
    }

    private static void zipDir(final String filename, String indir, final Predicate<Path> filter) {
        final Path pathDir = Paths.get(ZomboidFileSystem.instance.getCacheDirSub(indir));
        if (Files.exists(pathDir) && Files.isDirectory(pathDir)) {
            try {
                Files.walkFileTree(pathDir, new FileVisitor<Path>() {
                    public @NonNull FileVisitResult preVisitDirectory(Path dir, @NonNull BasicFileAttributes attrs) throws IOException {
                        return !dir.equals(pathDir) && filter != null && !filter.test(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                    }

                    public @NonNull FileVisitResult visitFile(Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                        Path relativePath = pathDir.relativize(file);
                        InputStreamSupplier streamSupplier = () -> {
                            InputStream is = null;

                            try {
                                is = Files.newInputStream(file);
                            } catch (IOException var3x) {
                                DebugType.General.printException(var3x, LogSeverity.Error);
                            }

                            return is;
                        };
                        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(filename + File.separator + relativePath.toString().replace("/", File.separator));
                        zipArchiveEntry.setMethod(0);
                        ZipBackup.scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
                        return FileVisitResult.CONTINUE;
                    }

                    public @NonNull FileVisitResult visitFileFailed(Path file, @NonNull IOException exc) throws IOException {
                        DebugType.General.printException(exc, LogSeverity.Error);
                        return FileVisitResult.CONTINUE;
                    }

                    public @NonNull FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc != null) {
                            DebugType.General.printException(exc, LogSeverity.Error);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception var5) {
                DebugType.General.printException(var5, LogSeverity.Error);
            }
        }
    }

    private static enum BackupTypes {
        period,
        startup,
        version;
    }
}
