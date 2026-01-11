// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import jassimp.AiPostProcessSteps;
import jassimp.Jassimp;
import java.util.EnumSet;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public final class FileTask_LoadAiScene extends FileTask {
    String filename;
    EnumSet<AiPostProcessSteps> postProcessStepSet;

    public FileTask_LoadAiScene(String filename, EnumSet<AiPostProcessSteps> pps, IFileTaskCallback cb, FileSystem fileSystem) {
        super(fileSystem, cb);
        this.filename = filename;
        this.postProcessStepSet = pps;
    }

    @Override
    public String getErrorMessage() {
        return this.filename;
    }

    @Override
    public void done() {
        this.filename = null;
        this.postProcessStepSet = null;
    }

    @Override
    public Object call() throws Exception {
        return Jassimp.importFile(this.filename, this.postProcessStepSet);
    }
}
