// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.maps;

import zombie.core.textures.ImageData;
import zombie.core.textures.MipMapLevel;

public final class BiomeRaster {
    private static final int NUM_BANDS = 2;
    private byte[] pixels;
    int width;
    int height;

    public void createFromImage(String fileName) throws Exception {
        ImageData imageData = new ImageData(fileName);
        MipMapLevel mipMapLevel = imageData.getData();
        this.width = imageData.getWidth();
        this.height = imageData.getHeight();
        int span = this.width * 2;
        this.pixels = new byte[span * this.height];
        int[] pixel = new int[4];

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                ImageData.getPixelClamped(mipMapLevel.getBuffer(), mipMapLevel.width, mipMapLevel.height, x, y, pixel);

                for (int i = 0; i < 2; i++) {
                    this.pixels[x * 2 + i + y * span] = (byte)pixel[i];
                }
            }
        }

        imageData.dispose();
    }

    public int[] getSamples(int x, int y, int w, int h, int b, int[] iArray) {
        int x1 = x + w;
        int y1 = y + h;
        if (x >= 0 && x1 >= x && x1 <= this.width && y >= 0 && y1 >= y && y1 <= this.height) {
            if (b >= 0 && b < 2) {
                if (iArray == null) {
                    iArray = new int[w * h];
                }

                int offset = 0;
                int span = this.width * 2;

                for (int i = y; i < y1; i++) {
                    for (int j = x; j < x1; j++) {
                        iArray[offset++] = this.pixels[j * 2 + b + i * span] & 255;
                    }
                }

                return iArray;
            } else {
                throw new IllegalArgumentException("invalid band");
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid coordinates.");
        }
    }
}
