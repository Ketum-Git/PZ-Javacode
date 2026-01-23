// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import java.io.InputStream;
import zombie.core.textures.ImageData;
import zombie.core.textures.TextureIDAssetManager;
import zombie.fileSystem.DeviceList;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public final class FileTask_LoadPackImage extends FileTask {
    String packName;
    String imageName;
    boolean mask;
    int flags;

    public FileTask_LoadPackImage(String packName, String imageName, FileSystem fs, IFileTaskCallback cb) {
        super(fs, cb);
        this.packName = packName;
        this.imageName = imageName;
        this.mask = fs.getTexturePackAlpha(packName, imageName);
        this.flags = fs.getTexturePackFlags(packName);
    }

    @Override
    public void done() {
    }

    @Override
    public Object call() throws Exception {
        TextureIDAssetManager.instance.waitFileTask();
        DeviceList deviceList = this.fileSystem.getTexturePackDevice(this.packName);

        ImageData var4;
        try (InputStream input = this.fileSystem.openStream(deviceList, this.imageName)) {
            ImageData imageData = new ImageData(input, this.mask);
            if ((this.flags & 64) != 0) {
                imageData.initMipMaps();
            }

            var4 = imageData;
        }

        return var4;
    }
}
