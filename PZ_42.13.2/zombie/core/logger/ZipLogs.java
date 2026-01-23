// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipError;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.network.MD5Checksum;

public final class ZipLogs {
    static ArrayList<String> filePaths = new ArrayList<>();

    public static void addZipFile(boolean saveTheSaveData) {
        FileSystem zipfs = null;

        try {
            String zipname = ZomboidFileSystem.instance.getCacheDir() + File.separator + "logs.zip";
            String zipnamePath = new File(zipname).toURI().toString();
            URI zip_disk = URI.create("jar:" + zipnamePath);
            Path zipLocation = FileSystems.getDefault().getPath(zipname).toAbsolutePath();
            Map<String, String> env = new HashMap<>();
            env.put("create", String.valueOf(Files.notExists(zipLocation)));

            try {
                zipfs = FileSystems.newFileSystem(zip_disk, env);
            } catch (IOException var19) {
                var19.printStackTrace();
                return;
            } catch (ZipError var20) {
                var20.printStackTrace();
                DebugLog.log("Deleting possibly-corrupt " + zipname);

                try {
                    Files.deleteIfExists(zipLocation);
                } catch (IOException var17) {
                    var17.printStackTrace();
                }

                return;
            }

            long consoleHash = getMD5FromZip(zipfs, "/meta/console.txt.md5");
            long consoleHash_coop = getMD5FromZip(zipfs, "/meta/coop-console.txt.md5");
            long consoleHash_server = getMD5FromZip(zipfs, "/meta/server-console.txt.md5");
            long consoleHash_debugLog = getMD5FromZip(zipfs, "/meta/DebugLog.txt.md5");
            addLogToZip(zipfs, "console", "console.txt", consoleHash);
            addLogToZip(zipfs, "coop-console", "coop-console.txt", consoleHash_coop);
            addLogToZip(zipfs, "server-console", "server-console.txt", consoleHash_server);
            addDebugLogToZip(zipfs, "debug-log", "DebugLog.txt", consoleHash_debugLog);
            addToZip(zipfs, "/configs/options.ini", "options.ini");
            addToZip(zipfs, "/configs/popman-options.ini", "popman-options.ini");
            addToZip(zipfs, "/configs/latestSave.ini", "latestSave.ini");
            addToZip(zipfs, "/configs/debug-options.ini", "debug-options.ini");
            addToZip(zipfs, "/configs/sounds.ini", "sounds.ini");
            addToZip(zipfs, "/addition/translationProblems.txt", "translationProblems.txt");
            addToZip(zipfs, "/addition/gamepadBinding.config", "gamepadBinding.config");
            addFilelistToZip(zipfs, "/addition/mods.txt", "mods");
            addDirToZip(zipfs, "/statistic", "Statistic");
            if (!saveTheSaveData) {
                addSaveOldToZip(zipfs, "/save_old/map_t.bin", "map_t.bin");
                addSaveOldToZip(zipfs, "/save_old/map_ver.bin", "map_ver.bin");
                addSaveOldToZip(zipfs, "/save_old/map.bin", "map.bin");
                addSaveOldToZip(zipfs, "/save_old/map_sand.bin", "map_sand.bin");
                addSaveOldToZip(zipfs, "/save_old/reanimated.bin", "reanimated.bin");
                addSaveOldToZip(zipfs, "/save_old/zombies.ini", "zombies.ini");
                addSaveOldToZip(zipfs, "/save_old/z_outfits.bin", "z_outfits.bin");
                addSaveOldToZip(zipfs, "/save_old/map_p.bin", "map_p.bin");
                addSaveOldToZip(zipfs, "/save_old/map_meta.bin", "map_meta.bin");
                addSaveOldToZip(zipfs, "/save_old/map_zone.bin", "map_zone.bin");
                addSaveOldToZip(zipfs, "/save_old/serverid.dat", "serverid.dat");
                addSaveOldToZip(zipfs, "/save_old/thumb.png", "thumb.png");
                addSaveOldToZip(zipfs, "/save_old/players.db", "players.db");
                addSaveOldToZip(zipfs, "/save_old/players.db-journal", "players.db-journal");
                addSaveOldToZip(zipfs, "/save_old/vehicles.db", "vehicles.db");
                addSaveOldToZip(zipfs, "/save_old/vehicles.db-journal", "vehicles.db-journal");
                putTextFile(zipfs, "/save_old/description.txt", getLastSaveDescription());
            } else {
                addSaveToZip(zipfs, "/save/map_t.bin", "map_t.bin");
                addSaveToZip(zipfs, "/save/map_ver.bin", "map_ver.bin");
                addSaveToZip(zipfs, "/save/map.bin", "map.bin");
                addSaveToZip(zipfs, "/save/map_sand.bin", "map_sand.bin");
                addSaveToZip(zipfs, "/save/reanimated.bin", "reanimated.bin");
                addSaveToZip(zipfs, "/save/zombies.ini", "zombies.ini");
                addSaveToZip(zipfs, "/save/z_outfits.bin", "z_outfits.bin");
                addSaveToZip(zipfs, "/save/map_p.bin", "map_p.bin");
                addSaveToZip(zipfs, "/save/map_meta.bin", "map_meta.bin");
                addSaveToZip(zipfs, "/save/map_zone.bin", "map_zone.bin");
                addSaveToZip(zipfs, "/save/serverid.dat", "serverid.dat");
                addSaveToZip(zipfs, "/save/thumb.png", "thumb.png");
                addSaveToZip(zipfs, "/save/players.db", "players.db");
                addSaveToZip(zipfs, "/save/players.db-journal", "players.db-journal");
                addSaveToZip(zipfs, "/save/vehicles.db", "vehicles.db");
                addSaveToZip(zipfs, "/save/vehicles.db-journal", "vehicles.db-journal");
                putTextFile(zipfs, "/save/description.txt", getCurrentSaveDescription());
            }

            try {
                zipfs.close();
            } catch (IOException var18) {
                var18.printStackTrace();
            }
        } catch (Exception var21) {
            if (zipfs != null) {
                try {
                    zipfs.close();
                } catch (IOException var16) {
                    var16.printStackTrace();
                }
            }

            var21.printStackTrace();
        }
    }

    private static void copyToZip(Path root, Path contents, Path file) throws IOException {
        Path to = root.resolve(contents.relativize(file).toString());
        if (Files.isDirectory(file)) {
            Files.createDirectories(to);
        } else {
            Files.copy(file, to);
        }
    }

    public static void addToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal);
            Files.createDirectories(internalTargetPath.getParent());
            Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename).toAbsolutePath();
            Files.deleteIfExists(internalTargetPath);
            if (Files.exists(filePath)) {
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException var5) {
            var5.printStackTrace();
        }
    }

    private static void addSaveToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal);
            Files.createDirectories(internalTargetPath.getParent());
            Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getFileNameInCurrentSave(filename)).toAbsolutePath();
            Files.deleteIfExists(internalTargetPath);
            if (Files.exists(filePath)) {
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException var5) {
            var5.printStackTrace();
        }
    }

    private static void addSaveOldToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini")));
            } catch (FileNotFoundException var8) {
                return;
            }

            String World = br.readLine();
            String GameMode = br.readLine();
            br.close();
            Path internalTargetPath = zipfs.getPath(filenameInternal);
            Files.createDirectories(internalTargetPath.getParent());
            Path filePath = FileSystems.getDefault()
                .getPath(ZomboidFileSystem.instance.getSaveDir() + File.separator + GameMode + File.separator + World + File.separator + filename)
                .toAbsolutePath();
            Files.deleteIfExists(internalTargetPath);
            if (Files.exists(filePath)) {
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException var9) {
            var9.printStackTrace();
        }
    }

    private static String getLastSaveDescription() {
        try {
            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini")));
            } catch (FileNotFoundException var3) {
                return "-";
            }

            String World = br.readLine();
            String GameMode = br.readLine();
            br.close();
            return "World: " + World + "\n\rGameMode:" + GameMode;
        } catch (IOException var4) {
            var4.printStackTrace();
            return "-";
        }
    }

    private static String getCurrentSaveDescription() {
        String GameMode = "Sandbox";
        if (Core.gameMode != null) {
            GameMode = Core.gameMode;
        }

        String World = "-";
        if (Core.gameSaveWorld != null) {
            World = Core.gameSaveWorld;
        }

        return "World: " + World + "\n\rGameMode:" + GameMode;
    }

    public static void addDirToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal);
            deleteDirectory(zipfs, internalTargetPath);
            Files.createDirectories(internalTargetPath);
            Path dirPath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename).toAbsolutePath();
            Stream<Path> files = Files.walk(dirPath);
            files.forEach(file -> {
                try {
                    copyToZip(internalTargetPath, dirPath, file);
                } catch (IOException var4x) {
                    throw new RuntimeException(var4x);
                }
            });
        } catch (IOException var6) {
        }
    }

    private static void addDirToZipLua(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal);
            deleteDirectory(zipfs, internalTargetPath);
            Files.createDirectories(internalTargetPath);
            Path dirPath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename).toAbsolutePath();
            Stream<Path> files = Files.walk(dirPath);
            files.forEach(file -> {
                try {
                    if (!file.endsWith("ServerList.txt") && !file.endsWith("ServerListSteam.txt")) {
                        copyToZip(internalTargetPath, dirPath, file);
                    }
                } catch (IOException var4x) {
                    throw new RuntimeException(var4x);
                }
            });
        } catch (IOException var6) {
        }
    }

    private static void addFilelistToZip(FileSystem zipfs, String filenameInternal, String dirname) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal);
            Path dirPath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + dirname).toAbsolutePath();
            Stream<Path> files = Files.list(dirPath);
            String filelist = files.map(Path::getFileName).map(Path::toString).collect(Collectors.joining("; "));
            Files.deleteIfExists(internalTargetPath);
            Files.write(internalTargetPath, filelist.getBytes());
        } catch (IOException var7) {
        }
    }

    static void deleteDirectory(FileSystem zipfs, Path dir) {
        filePaths.clear();
        getDirectoryFiles(dir);

        for (String s : filePaths) {
            try {
                Files.delete(zipfs.getPath(s));
            } catch (IOException var5) {
                var5.printStackTrace();
            }
        }
    }

    static void getDirectoryFiles(Path dir) {
        try {
            Stream<Path> files = Files.walk(dir);
            files.forEach(file -> {
                if (!file.toString().equals(dir.toString())) {
                    if (Files.isDirectory(file)) {
                        getDirectoryFiles(file);
                    } else if (!filePaths.contains(file.toString())) {
                        filePaths.add(file.toString());
                    }
                }
            });
            filePaths.add(dir.toString());
        } catch (IOException var2) {
        }
    }

    private static void addLogToZip(FileSystem zipfs, String dirnameInternal, String filename, long hashFromZip) {
        long md5;
        try {
            md5 = MD5Checksum.createChecksum(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename);
        } catch (Exception var16) {
            md5 = 0L;
        }

        File consoleFile_coop = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename);
        if (consoleFile_coop.exists() && !consoleFile_coop.isDirectory() && md5 != hashFromZip) {
            try {
                Path pathInZipfile = zipfs.getPath("/" + dirnameInternal + "/log_5.txt");
                Files.delete(pathInZipfile);
            } catch (Exception var15) {
            }

            for (int i = 5; i > 0; i--) {
                Path sourceURI = zipfs.getPath("/" + dirnameInternal + "/log_" + i + ".txt");
                Path destinationURI = zipfs.getPath("/" + dirnameInternal + "/log_" + (i + 1) + ".txt");

                try {
                    Files.move(sourceURI, destinationURI);
                } catch (Exception var14) {
                }
            }

            try {
                Path internalTargetPath = zipfs.getPath("/" + dirnameInternal + "/log_1.txt");
                Files.createDirectories(internalTargetPath.getParent());
                Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename).toAbsolutePath();
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
                Path pathInZipfile = zipfs.getPath("/meta/" + filename + ".md5");
                Files.createDirectories(pathInZipfile.getParent());

                try {
                    Files.delete(pathInZipfile);
                } catch (Exception var12) {
                }

                Files.write(pathInZipfile, String.valueOf(md5).getBytes());
            } catch (Exception var13) {
                var13.printStackTrace();
            }
        }
    }

    private static void addDebugLogToZip(FileSystem zipfs, String dirnameInternal, String filename, long hashFromZip) {
        String filepath = null;
        File logsDir = new File(LoggerManager.getLogsDir());
        String[] logFileNameList = logsDir.list();

        for (int i = 0; i < logFileNameList.length; i++) {
            String logFileName = logFileNameList[i];
            if (logFileName.contains("DebugLog.txt")) {
                filepath = LoggerManager.getLogsDir() + File.separator + logFileName;
                break;
            }
        }

        if (filepath != null) {
            long md5;
            try {
                md5 = MD5Checksum.createChecksum(filepath);
            } catch (Exception var19) {
                md5 = 0L;
            }

            File consoleFile_coop = new File(filepath);
            if (consoleFile_coop.exists() && !consoleFile_coop.isDirectory() && md5 != hashFromZip) {
                try {
                    Path pathInZipfile = zipfs.getPath("/" + dirnameInternal + "/log_5.txt");
                    Files.delete(pathInZipfile);
                } catch (Exception var18) {
                }

                for (int ix = 5; ix > 0; ix--) {
                    Path sourceURI = zipfs.getPath("/" + dirnameInternal + "/log_" + ix + ".txt");
                    Path destinationURI = zipfs.getPath("/" + dirnameInternal + "/log_" + (ix + 1) + ".txt");

                    try {
                        Files.move(sourceURI, destinationURI);
                    } catch (Exception var17) {
                    }
                }

                try {
                    Path internalTargetPath = zipfs.getPath("/" + dirnameInternal + "/log_1.txt");
                    Files.createDirectories(internalTargetPath.getParent());
                    Path filePath = FileSystems.getDefault().getPath(filepath).toAbsolutePath();
                    Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
                    Path pathInZipfile = zipfs.getPath("/meta/" + filename + ".md5");
                    Files.createDirectories(pathInZipfile.getParent());

                    try {
                        Files.delete(pathInZipfile);
                    } catch (Exception var15) {
                    }

                    Files.write(pathInZipfile, String.valueOf(md5).getBytes());
                } catch (Exception var16) {
                    var16.printStackTrace();
                }
            }
        }
    }

    private static long getMD5FromZip(FileSystem zipfs, String filenameInternal) {
        long md5 = 0L;

        try {
            Path pathInZipfile = zipfs.getPath(filenameInternal);
            if (Files.exists(pathInZipfile)) {
                List<String> lines = Files.readAllLines(pathInZipfile);
                md5 = Long.parseLong(lines.get(0));
            }
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        return md5;
    }

    public static void putTextFile(FileSystem zipfs, String filename, String text) {
        try {
            Path pathInZipfile = zipfs.getPath(filename);
            Files.createDirectories(pathInZipfile.getParent());

            try {
                Files.delete(pathInZipfile);
            } catch (Exception var5) {
            }

            Files.write(pathInZipfile, text.getBytes());
        } catch (Exception var6) {
            var6.printStackTrace();
        }
    }
}
