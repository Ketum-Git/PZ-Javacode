// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.CRC32;

public class MD5Checksum {
    public static long createChecksum(String filename) throws Exception {
        File ddd = new File(filename);
        if (!ddd.exists()) {
            return 0L;
        } else {
            InputStream fis = new FileInputStream(filename);
            CRC32 crcMaker = new CRC32();
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                crcMaker.update(buffer, 0, bytesRead);
            }

            long crc = crcMaker.getValue();
            fis.close();
            return crc;
        }
    }

    public static void main(String[] args) {
    }
}
