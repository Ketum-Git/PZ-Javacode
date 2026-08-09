// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;

public final class DeviceList {
    private final IFileDevice[] devices = new IFileDevice[8];

    public void add(IFileDevice device) {
        for (int i = 0; i < this.devices.length; i++) {
            if (this.devices[i] == null) {
                this.devices[i] = device;
                break;
            }
        }
    }

    public IFile createFile() {
        IFile prev = null;

        for (int i = 0; i < this.devices.length && this.devices[i] != null; i++) {
            prev = this.devices[i].createFile(prev);
        }

        return prev;
    }

    public InputStream createStream(String path) throws IOException {
        InputStream prev = null;

        for (int i = 0; i < this.devices.length && this.devices[i] != null; i++) {
            prev = this.devices[i].createStream(path, prev);
        }

        return prev;
    }
}
