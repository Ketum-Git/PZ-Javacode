// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import zombie.ZomboidFileSystem;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public abstract class FileTask_AbstractLoadModel extends FileTask {
    protected String fileName;
    private final String mediaFilePath;
    private final String mediaFileXPath;

    protected FileTask_AbstractLoadModel(FileSystem fileSystem, IFileTaskCallback cb, String mediaFilePath, String mediaFileXPath) {
        super(fileSystem, cb);
        this.mediaFilePath = mediaFilePath;
        this.mediaFileXPath = mediaFileXPath;
    }

    @Override
    public Object call() throws Exception {
        this.checkSlowLoad();
        ModelFileExtensionType res = this.checkExtensionType();
        switch (res) {
            case X:
                return this.loadX();
            case Fbx:
                return this.loadFBX();
            case glTF:
                return this.loadGLTF();
            case Txt:
                return this.loadTxt();
            case None:
            default:
                return null;
        }
    }

    private void checkSlowLoad() {
        if (DebugOptions.instance.asset.slowLoad.getValue()) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException var2) {
            }
        }
    }

    private ModelFileExtensionType checkExtensionType() {
        String rawName = this.getRawFileName();
        String processedName = rawName.toLowerCase(Locale.ENGLISH);
        if (processedName.endsWith(".txt")) {
            return ModelFileExtensionType.Txt;
        } else {
            boolean forceLoadX = rawName.startsWith("x:");
            if (forceLoadX) {
                DebugLog.Animation.warn("Note: The 'x:' prefix is not required. name=\"" + rawName + "\"");
                processedName = rawName.substring(2);
            }

            if (rawName.contains("media/") || rawName.contains(".")) {
                this.fileName = rawName;
                if (!rawName.contains("media/")) {
                    this.fileName = this.mediaFileXPath + "/" + rawName;
                }

                this.fileName = ZomboidFileSystem.instance.getString(this.fileName);
                if (new File(this.fileName).exists()) {
                    if (this.fileName.endsWith(".fbx")) {
                        return ModelFileExtensionType.Fbx;
                    }

                    if (this.fileName.endsWith(".glb")) {
                        return ModelFileExtensionType.glTF;
                    }

                    if (this.fileName.endsWith(".x")) {
                        return ModelFileExtensionType.X;
                    }

                    return ModelFileExtensionType.X;
                }
            }

            this.fileName = this.mediaFileXPath + "/" + processedName + ".fbx";
            this.fileName = ZomboidFileSystem.instance.getString(this.fileName);
            if (new File(this.fileName).exists()) {
                return ModelFileExtensionType.Fbx;
            } else {
                this.fileName = this.mediaFileXPath + "/" + processedName + ".glb";
                this.fileName = ZomboidFileSystem.instance.getString(this.fileName);
                if (new File(this.fileName).exists()) {
                    return ModelFileExtensionType.glTF;
                } else {
                    this.fileName = this.mediaFileXPath + "/" + processedName + ".x";
                    this.fileName = ZomboidFileSystem.instance.getString(this.fileName);
                    if (new File(this.fileName).exists()) {
                        return ModelFileExtensionType.X;
                    } else if (forceLoadX) {
                        return ModelFileExtensionType.None;
                    } else {
                        if (!processedName.endsWith(".x")) {
                            this.fileName = this.mediaFilePath + "/" + processedName + ".txt";
                            if (rawName.contains("media/")) {
                                this.fileName = rawName;
                            }

                            this.fileName = ZomboidFileSystem.instance.getString(this.fileName);
                            if (new File(this.fileName).exists()) {
                                return ModelFileExtensionType.Txt;
                            }
                        }

                        return ModelFileExtensionType.None;
                    }
                }
            }
        }
    }

    public abstract String getRawFileName();

    public abstract ProcessedAiScene loadX() throws IOException;

    public abstract ProcessedAiScene loadFBX() throws IOException;

    public abstract ProcessedAiScene loadGLTF() throws IOException;

    public abstract ModelTxt loadTxt() throws IOException;
}
