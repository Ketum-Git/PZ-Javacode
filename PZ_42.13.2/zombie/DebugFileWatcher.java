// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import zombie.core.logger.ExceptionLogger;
import zombie.util.list.PZArrayUtil;

public final class DebugFileWatcher {
    private boolean isInitialized;
    private final HashMap<Path, String> watchedFiles = new HashMap<>();
    private final HashMap<WatchKey, Path> watchkeyMapping = new HashMap<>();
    private final ArrayList<PredicatedFileWatcher> predicateWatchers = new ArrayList<>();
    private final ArrayList<PredicatedFileWatcher> predicateWatchersInvoking = new ArrayList<>();
    private final FileSystem fs = FileSystems.getDefault();
    private WatchService watcher;
    private boolean predicateWatchersInvokingDirty = true;
    private long modificationTime = -1L;
    private final ArrayList<String> modifiedFiles = new ArrayList<>();
    private final ArrayList<DebugFileWatcher.IOnInitListener> onInitListeners = new ArrayList<>();
    public static final DebugFileWatcher instance = new DebugFileWatcher();

    private DebugFileWatcher() {
    }

    public void init() {
        try {
            this.watcher = this.fs.newWatchService();
            this.registerDirRecursive(this.fs.getPath(ZomboidFileSystem.instance.getMediaRootPath()));
            this.registerDirRecursive(this.fs.getPath(ZomboidFileSystem.instance.getMessagingDir()));
            this.isInitialized = true;
            this.invokeOnInitListeners();
        } catch (IOException var2) {
            this.watcher = null;
        }
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    private void invokeOnInitListeners() {
        ArrayList<DebugFileWatcher.IOnInitListener> listeners = new ArrayList<>(this.onInitListeners);
        this.onInitListeners.clear();

        for (DebugFileWatcher.IOnInitListener listener : listeners) {
            listener.onInit(this);
        }
    }

    private void addOnInitListener(DebugFileWatcher.IOnInitListener listener) {
        if (!this.onInitListeners.contains(listener)) {
            this.onInitListeners.add(listener);
        }
    }

    private void registerDirRecursive(Path start) {
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                {
                    Objects.requireNonNull(DebugFileWatcher.this);
                }

                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    DebugFileWatcher.this.registerDir(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException var3) {
            ExceptionLogger.logException(var3);
            this.watcher = null;
        }
    }

    private void registerDir(Path dir) {
        if (!this.isInitialized()) {
            this.addOnInitListener(watcher -> this.registerDir(dir));
        } else {
            try {
                WatchKey key = dir.register(this.watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
                this.watchkeyMapping.put(key, dir);
            } catch (IOException var3) {
                ExceptionLogger.logException(var3);
                this.watcher = null;
            }
        }
    }

    private void addWatchedFile(String path) {
        if (path != null) {
            this.watchedFiles.put(this.fs.getPath(path), path);
        }
    }

    public void add(PredicatedFileWatcher watcher) {
        if (!this.predicateWatchers.contains(watcher)) {
            this.addWatchedFile(watcher.getPath());
            this.predicateWatchers.add(watcher);
            this.predicateWatchersInvokingDirty = true;
        }
    }

    public void addDirectory(String path) {
        if (path != null) {
            this.registerDir(this.fs.getPath(path));
        }
    }

    public void addDirectoryRecurse(String path) {
        if (path != null) {
            this.registerDirRecursive(this.fs.getPath(path));
        }
    }

    public void remove(PredicatedFileWatcher watcher) {
        this.predicateWatchers.remove(watcher);
    }

    public void update() {
        if (this.watcher != null) {
            for (WatchKey key = this.watcher.poll(); key != null; key = this.watcher.poll()) {
                try {
                    Path dir = this.watchkeyMapping.getOrDefault(key, null);

                    for (WatchEvent<?> watchEvent : key.pollEvents()) {
                        if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path filename = (Path)watchEvent.context();
                            Path fullPath = dir.resolve(filename);
                            String registeredPath = this.watchedFiles.getOrDefault(fullPath, fullPath.toString());
                            this.modificationTime = System.currentTimeMillis();
                            if (!this.modifiedFiles.contains(registeredPath)) {
                                this.modifiedFiles.add(registeredPath);
                            }
                        } else if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path filename = (Path)watchEvent.context();
                            Path fullPath = dir.resolve(filename);
                            if (Files.isDirectory(fullPath)) {
                                this.registerDirRecursive(fullPath);
                            } else {
                                String registeredPath = this.watchedFiles.getOrDefault(fullPath, fullPath.toString());
                                this.modificationTime = System.currentTimeMillis();
                                if (!this.modifiedFiles.contains(registeredPath)) {
                                    this.modifiedFiles.add(registeredPath);
                                }
                            }
                        }
                    }
                } finally {
                    if (!key.reset()) {
                        this.watchkeyMapping.remove(key);
                    }
                }
            }

            if (!this.modifiedFiles.isEmpty()) {
                if (this.modificationTime + 2000L <= System.currentTimeMillis()) {
                    for (int i = this.modifiedFiles.size() - 1; i >= 0; i--) {
                        String registeredPath = this.modifiedFiles.remove(i);
                        this.swapWatcherArrays();

                        for (PredicatedFileWatcher watcher : this.predicateWatchersInvoking) {
                            watcher.onModified(registeredPath);
                        }
                    }
                }
            }
        }
    }

    private void swapWatcherArrays() {
        if (this.predicateWatchersInvokingDirty) {
            this.predicateWatchersInvoking.clear();
            PZArrayUtil.addAll(this.predicateWatchersInvoking, this.predicateWatchers);
            this.predicateWatchersInvokingDirty = false;
        }
    }

    public interface IOnInitListener {
        void onInit(DebugFileWatcher var1);
    }
}
