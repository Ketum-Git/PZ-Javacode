// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import zombie.AmbientStreamManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class MetaTracker {
    private MetaTracker() {
    }

    public static void save() {
        if (!GameClient.client && !GameServer.server && !Core.getInstance().isNoSave()) {
            try {
                ByteBuffer bb = ByteBuffer.allocate(10000);
                bb.putInt(241);
                IsoWorld.instance.helicopter.save(bb);
                AmbientStreamManager.instance.save(bb);
                bb.flip();
                File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("metadata.bin"));
                FileOutputStream output = new FileOutputStream(path);
                output.getChannel().truncate(0L);
                output.write(bb.array(), 0, bb.limit());
                output.flush();
                output.close();
            } catch (Exception var3) {
                ExceptionLogger.logException(var3);
            }
        }
    }

    public static void load() {
        if (!Core.getInstance().isNoSave()) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("metadata.bin");
            File path = new File(fileName);
            if (path.exists()) {
                try (FileInputStream inStream = new FileInputStream(path)) {
                    ByteBuffer bb = ByteBuffer.allocate((int)path.length());
                    bb.clear();
                    int len = inStream.read(bb.array());
                    bb.limit(len);
                    int worldVersion = bb.getInt();
                    IsoWorld.instance.helicopter.load(bb, worldVersion);
                    AmbientStreamManager.instance.load(bb, worldVersion);
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                }
            }
        }
    }
}
