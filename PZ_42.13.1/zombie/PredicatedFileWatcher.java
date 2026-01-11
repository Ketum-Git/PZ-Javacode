// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.File;
import java.util.function.Predicate;
import zombie.debug.DebugLog;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;

/**
 * PredicatedFileWatcher
 *  An advanced form of the regular DebugFileWatcher
 *  
 *  Includes the watched file path, a predicate callback, and a callback in case the predicate returns TRUE.
 */
public final class PredicatedFileWatcher {
    private final String path;
    private final Predicate<String> predicate;
    private final PredicatedFileWatcher.IPredicatedFileWatcherCallback callback;

    public PredicatedFileWatcher(Predicate<String> predicate, PredicatedFileWatcher.IPredicatedFileWatcherCallback callback) {
        this(null, predicate, callback);
    }

    public PredicatedFileWatcher(String path, PredicatedFileWatcher.IPredicatedFileWatcherCallback callback) {
        this(path, null, callback);
    }

    public <T> PredicatedFileWatcher(String path, Class<T> clazz, PredicatedFileWatcher.IPredicatedDataPacketFileWatcherCallback<T> callback) {
        this(path, null, new PredicatedFileWatcher.GenericPredicatedFileWatcherCallback<>(clazz, callback));
    }

    public PredicatedFileWatcher(String path, Predicate<String> predicate, PredicatedFileWatcher.IPredicatedFileWatcherCallback callback) {
        this.path = this.processPath(path);
        this.predicate = predicate != null ? predicate : this::pathsEqual;
        this.callback = callback;
    }

    public String getPath() {
        return this.path;
    }

    private String processPath(String path) {
        return path != null ? ZomboidFileSystem.processFilePath(path, File.separatorChar) : null;
    }

    private boolean pathsEqual(String entryKey) {
        return entryKey.equals(this.path);
    }

    public void onModified(String entryKey) {
        if (this.predicate.test(entryKey)) {
            this.callback.call(entryKey);
        }
    }

    public static class GenericPredicatedFileWatcherCallback<T> implements PredicatedFileWatcher.IPredicatedFileWatcherCallback {
        private final Class<T> clazz;
        private final PredicatedFileWatcher.IPredicatedDataPacketFileWatcherCallback<T> callback;

        public GenericPredicatedFileWatcherCallback(Class<T> clazz, PredicatedFileWatcher.IPredicatedDataPacketFileWatcherCallback<T> callback) {
            this.clazz = clazz;
            this.callback = callback;
        }

        @Override
        public void call(String xmlFile) {
            T data;
            try {
                data = PZXmlUtil.parse(this.clazz, xmlFile);
            } catch (PZXmlParserException var4) {
                DebugLog.General.error("Exception thrown. " + var4);
                return;
            }

            this.callback.call(data);
        }
    }

    public interface IPredicatedDataPacketFileWatcherCallback<T> {
        void call(T var1);
    }

    public interface IPredicatedFileWatcherCallback {
        void call(String entryKey);
    }
}
