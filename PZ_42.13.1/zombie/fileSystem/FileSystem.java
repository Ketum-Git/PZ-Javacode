// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import zombie.core.textures.TexturePackPage;

public abstract class FileSystem {
    public static final int INVALID_ASYNC = -1;

    public abstract boolean mount(IFileDevice device);

    public abstract boolean unMount(IFileDevice device);

    public abstract IFile open(DeviceList deviceList, String path, int mode);

    public abstract void close(IFile file);

    public abstract int openAsync(DeviceList deviceList, String path, int mode, IFileTask2Callback cb);

    public abstract void closeAsync(IFile file, IFileTask2Callback cb);

    public abstract void cancelAsync(int id);

    public abstract InputStream openStream(DeviceList deviceList, String path) throws IOException;

    public abstract void closeStream(InputStream stream);

    public abstract int runAsync(FileTask task);

    public abstract void updateAsyncTransactions();

    public abstract boolean hasWork();

    public abstract DeviceList getDefaultDevice();

    public abstract void mountTexturePack(String name, FileSystem.TexturePackTextures subTextures, int flags);

    public abstract DeviceList getTexturePackDevice(String name);

    public abstract int getTexturePackFlags(String name);

    public abstract boolean getTexturePackAlpha(String name, String page);

    public static final class SubTexture {
        public String packName;
        public String pageName;
        public TexturePackPage.SubTextureInfo info;

        public SubTexture(String packName, String pageName, TexturePackPage.SubTextureInfo info) {
            this.packName = packName;
            this.pageName = pageName;
            this.info = info;
        }
    }

    public static final class TexturePackTextures extends HashMap<String, FileSystem.SubTexture> {
    }
}
