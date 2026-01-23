// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import zombie.core.textures.ImageData;
import zombie.core.textures.TextureIDAssetManager;
import zombie.debug.DebugOptions;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public final class FileTask_LoadImageData extends FileTask {
    String imageName;
    boolean mask;

    public FileTask_LoadImageData(String imageName, FileSystem fs, IFileTaskCallback cb) {
        super(fs, cb);
        this.imageName = imageName;
    }

    @Override
    public String getErrorMessage() {
        return this.imageName;
    }

    @Override
    public void done() {
    }

    @Override
    public Object call() throws Exception {
        TextureIDAssetManager.instance.waitFileTask();
        if (DebugOptions.instance.asset.slowLoad.getValue()) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException var9) {
            }
        }

        ImageData var3;
        try (
            InputStream input = new FileInputStream(this.imageName);
            BufferedInputStream bis = new BufferedInputStream(input);
        ) {
            var3 = new ImageData(bis, this.mask);
        }

        return var3;
    }
}
