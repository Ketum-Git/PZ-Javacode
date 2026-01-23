// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public final class TexturePackPage {
    public static HashMap<String, Stack<String>> foundTextures = new HashMap<>();
    public static final HashMap<String, Texture> subTextureMap = new HashMap<>();
    public static final HashMap<String, Texture> subTextureMap2 = new HashMap<>();
    public static final HashMap<String, TexturePackPage> texturePackPageMap = new HashMap<>();
    public static final HashMap<String, String> TexturePackPageNameMap = new HashMap<>();
    public final HashMap<String, Texture> subTextures = new HashMap<>();
    public Texture tex;
    static ByteBuffer sliceBuffer;
    static boolean hasCache;
    static int percent;
    public static int chl1;
    public static int chl2;
    public static int chl3;
    public static int chl4;
    static StringBuilder v = new StringBuilder(50);
    public static ArrayList<TexturePackPage.SubTextureInfo> tempSubTextureInfo = new ArrayList<>();
    public static ArrayList<String> tempFilenameCheck = new ArrayList<>();
    public static boolean ignoreWorldItemTextures = true;

    public static void LoadDir(String path) throws URISyntaxException {
    }

    public static void searchFolders(File fo) {
    }

    public static Texture getTexture(String tex) {
        if (tex.contains(".png")) {
            return Texture.getSharedTexture(tex);
        } else {
            return subTextureMap.containsKey(tex) ? subTextureMap.get(tex) : null;
        }
    }

    public static int readInt(InputStream in) throws EOFException, IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        chl1 = ch1;
        chl2 = ch2;
        chl3 = ch3;
        chl4 = ch4;
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        } else {
            return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
        }
    }

    public static int readInt(ByteBuffer in) throws EOFException, IOException {
        int ch1 = in.get();
        int ch2 = in.get();
        int ch3 = in.get();
        int ch4 = in.get();
        chl1 = ch1;
        chl2 = ch2;
        chl3 = ch3;
        chl4 = ch4;
        return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
    }

    public static int readIntByte(InputStream in) throws EOFException, IOException {
        int ch1 = chl2;
        int ch2 = chl3;
        int ch3 = chl4;
        int ch4 = in.read();
        chl1 = ch1;
        chl2 = ch2;
        chl3 = ch3;
        chl4 = ch4;
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        } else {
            return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
        }
    }

    public static String ReadString(InputStream input) throws IOException {
        v.setLength(0);
        int size = readInt(input);

        for (int n = 0; n < size; n++) {
            v.append((char)input.read());
        }

        return v.toString();
    }

    public void loadFromPackFile(BufferedInputStream input) throws Exception {
        String name = ReadString(input);
        tempFilenameCheck.add(name);
        int numEntries = readInt(input);
        boolean mask = readInt(input) != 0;
        if (mask) {
            boolean tex = false;
        }

        tempSubTextureInfo.clear();

        for (int n = 0; n < numEntries; n++) {
            String entryName = ReadString(input);
            int a = readInt(input);
            int b = readInt(input);
            int c = readInt(input);
            int d = readInt(input);
            int e = readInt(input);
            int f = readInt(input);
            int g = readInt(input);
            int h = readInt(input);
            if (!ignoreWorldItemTextures || !entryName.startsWith("WItem_")) {
                tempSubTextureInfo.add(new TexturePackPage.SubTextureInfo(a, b, c, d, e, f, g, h, entryName));
            }
        }

        Texture tex = new Texture(name, input, mask);

        for (int nx = 0; nx < tempSubTextureInfo.size(); nx++) {
            TexturePackPage.SubTextureInfo stex = tempSubTextureInfo.get(nx);
            Texture st = tex.split(stex.x, stex.y, stex.w, stex.h);
            st.copyMaskRegion(tex, stex.x, stex.y, stex.w, stex.h);
            st.setName(stex.name);
            this.subTextures.put(stex.name, st);
            subTextureMap.put(stex.name, st);
            st.offsetX = stex.ox;
            st.offsetY = stex.oy;
            st.widthOrig = stex.fx;
            st.heightOrig = stex.fy;
        }

        tex.mask = null;
        texturePackPageMap.put(name, this);
        int id = 0;

        do {
            id = readIntByte(input);
        } while (id != -559038737);
    }

    public static class SubTextureInfo {
        public int w;
        public int h;
        public int x;
        public int y;
        public int ox;
        public int oy;
        public int fx;
        public int fy;
        public String name;

        public SubTextureInfo(int x, int y, int w, int h, int ox, int oy, int fx, int fy, String name) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.ox = ox;
            this.oy = oy;
            this.fx = fx;
            this.fy = fy;
            this.name = name;
        }
    }
}
