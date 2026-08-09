// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.textures.Texture;

public class ImageUtils {
    public static boolean useMipmap = true;

    private ImageUtils() {
    }

    public static void depureTexture(Texture texture, float limit) {
        WrappedBuffer wb = texture.getData();
        ByteBuffer data = wb.getBuffer();
        data.rewind();
        int ilimit = (int)(limit * 255.0F);
        long tot = texture.getWidthHW() * texture.getHeightHW();

        for (int i = 0; i < tot; i++) {
            data.mark();
            data.get();
            data.get();
            data.get();
            byte alpha = data.get();
            int ialpha;
            if (alpha < 0) {
                ialpha = 256 + alpha;
            } else {
                ialpha = alpha;
            }

            if (ialpha < ilimit) {
                data.reset();
                data.put((byte)0);
                data.put((byte)0);
                data.put((byte)0);
                data.put((byte)0);
            }
        }

        data.flip();
        texture.setData(data);
        wb.dispose();
    }

    public static int getNextPowerOfTwo(int fold) {
        int pow = 2;

        while (pow < fold) {
            pow += pow;
        }

        return pow;
    }

    public static int getNextPowerOfTwoHW(int fold) {
        int pow = 2;

        while (pow < fold) {
            pow += pow;
        }

        return pow;
    }

    public static Texture getScreenShot() {
        Texture texture = new Texture(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0);
        IntBuffer point = org.lwjglx.BufferUtils.createIntBuffer(4);
        texture.bind();
        point.rewind();
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glCopyTexImage2D(3553, 0, 6408, 0, 0, texture.getWidthHW(), texture.getHeightHW(), 0);
        return texture;
    }

    public static ByteBuffer makeTransp(ByteBuffer data, int red, int green, int blue, int widthHW, int heightHW) {
        return makeTransp(data, red, green, blue, 0, widthHW, heightHW);
    }

    public static ByteBuffer makeTransp(ByteBuffer data, int red, int green, int blue, int alpha, int widthHW, int heightHW) {
        data.rewind();

        for (int y = 0; y < heightHW; y++) {
            for (int x = 0; x < widthHW; x++) {
                byte r = data.get();
                byte g = data.get();
                byte b = data.get();
                if (r == (byte)red && g == (byte)green && b == (byte)blue) {
                    data.put((byte)alpha);
                } else {
                    data.get();
                }
            }
        }

        data.rewind();
        return data;
    }

    public static void saveBmpImage(Texture texture, String path) {
        saveImage(texture, path, "bmp");
    }

    public static void saveImage(Texture texture, String path, String format) {
        BufferedImage image = new BufferedImage(texture.getWidth(), texture.getHeight(), 1);
        WritableRaster raster = image.getRaster();
        WrappedBuffer wb = texture.getData();
        ByteBuffer bb = wb.getBuffer();
        bb.rewind();

        for (int y = 0; y < texture.getHeightHW() && y < texture.getHeight(); y++) {
            for (int x = 0; x < texture.getWidthHW(); x++) {
                if (x >= texture.getWidth()) {
                    bb.get();
                    bb.get();
                    bb.get();
                    bb.get();
                } else {
                    raster.setPixel(x, texture.getHeight() - 1 - y, new int[]{bb.get(), bb.get(), bb.get()});
                    bb.get();
                }
            }
        }

        wb.dispose();

        try {
            ImageIO.write(image, "png", new File(path));
        } catch (IOException var9) {
        }
    }

    public static void saveJpgImage(Texture texture, String path) {
        saveImage(texture, path, "jpg");
    }

    public static void savePngImage(Texture texture, String path) {
        saveImage(texture, path, "png");
    }
}
