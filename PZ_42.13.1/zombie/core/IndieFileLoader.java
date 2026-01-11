// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import zombie.ZomboidFileSystem;

public class IndieFileLoader {
    public static InputStreamReader getStreamReader(String path) throws FileNotFoundException {
        return getStreamReader(path, false);
    }

    public static InputStreamReader getStreamReader(String path, boolean bIgnoreJar) throws FileNotFoundException {
        InputStreamReader isr = null;
        InputStream is = null;
        if (is != null && !bIgnoreJar) {
            isr = new InputStreamReader(is);
        } else {
            try {
                FileInputStream fis = new FileInputStream(ZomboidFileSystem.instance.getString(path));
                isr = new InputStreamReader(fis, "UTF-8");
            } catch (Exception var6) {
                FileInputStream fisx = new FileInputStream(Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + path);
                isr = new InputStreamReader(fisx);
            }
        }

        return isr;
    }
}
