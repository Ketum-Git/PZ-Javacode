// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class CustomizationManager {
    static CustomizationManager instance = new CustomizationManager();
    private ByteBuffer serverImageIcon;
    private ByteBuffer serverImageLoadingScreen;
    private ByteBuffer serverImageLoginScreen;

    public static CustomizationManager getInstance() {
        return instance;
    }

    public void load() {
        if (GameServer.server) {
            this.serverImageIcon = this.load(ServerOptions.instance.serverImageIcon.getValue(), 64, 64);
            this.serverImageLoginScreen = this.load(ServerOptions.instance.serverImageLoginScreen.getValue(), 1280, 720);
            this.serverImageLoadingScreen = this.load(ServerOptions.instance.serverImageLoadingScreen.getValue(), 1280, 720);
        }
    }

    public ByteBuffer getServerImageIcon() {
        return this.serverImageIcon;
    }

    public ByteBuffer getServerImageLoginScreen() {
        return this.serverImageLoginScreen;
    }

    public ByteBuffer getServerImageLoadingScreen() {
        return this.serverImageLoadingScreen;
    }

    public Texture getClientCustomBackground() {
        return null;
    }

    private ByteBuffer load(String filename, int dimensionW, int dimensionH) {
        ByteBuffer bb = null;
        if (!filename.isEmpty()) {
            DebugLog.General.println("Loading " + filename + " with size " + dimensionW + "x" + dimensionH);

            try {
                BufferedImage bufferedImage = ImageIO.read(new File(filename).getAbsoluteFile());
                bb = loadCompressAndResizeInstance(bufferedImage, dimensionW, dimensionH);
                DebugLog.General.println("Data size " + bb.limit());
            } catch (IOException var6) {
                DebugLog.General.printException(var6, "Error loading file " + filename, LogSeverity.Error);
            }
        }

        return bb;
    }

    public static ByteBuffer loadCompressAndResizeInstance(BufferedImage image, int dimensionW, int dimensionH) {
        BufferedImage scaledIcon = new BufferedImage(dimensionW, dimensionH, 1);
        Graphics2D g = scaledIcon.createGraphics();
        double ratio = getIconRatio(image, scaledIcon);
        double width = image.getWidth() * ratio;
        double height = image.getHeight() * ratio;
        g.drawImage(image, (int)((scaledIcon.getWidth() - width) / 2.0), (int)((scaledIcon.getHeight() - height) / 2.0), (int)width, (int)height, null);
        g.dispose();
        return compressToByteBuffer(scaledIcon, "jpg");
    }

    public static ByteBuffer loadAndResizeInstance(BufferedImage image, int dimensionW, int dimensionH) {
        BufferedImage scaledIcon = new BufferedImage(dimensionW, dimensionH, 3);
        Graphics2D g = scaledIcon.createGraphics();
        double ratio = getIconRatio(image, scaledIcon);
        double width = image.getWidth() * ratio;
        double height = image.getHeight() * ratio;
        g.drawImage(image, (int)((scaledIcon.getWidth() - width) / 2.0), (int)((scaledIcon.getHeight() - height) / 2.0), (int)width, (int)height, null);
        g.dispose();
        return convertToByteBuffer(scaledIcon);
    }

    public static double getIconRatio(BufferedImage src, BufferedImage icon) {
        double ratio = 1.0;
        if (src.getWidth() > icon.getWidth()) {
            ratio = (double)icon.getWidth() / src.getWidth();
        } else {
            ratio = icon.getWidth() / src.getWidth();
        }

        if (src.getHeight() > icon.getHeight()) {
            double r2 = (double)icon.getHeight() / src.getHeight();
            if (r2 < ratio) {
                ratio = r2;
            }
        } else {
            double r2 = icon.getHeight() / src.getHeight();
            if (r2 < ratio) {
                ratio = r2;
            }
        }

        return ratio;
    }

    public static ByteBuffer convertToByteBuffer(BufferedImage image) {
        byte[] buffer = new byte[image.getWidth() * image.getHeight() * 4];
        int counter = 0;

        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                int colorSpace = image.getRGB(j, i);
                buffer[counter] = (byte)(colorSpace << 8 >> 24);
                buffer[counter + 1] = (byte)(colorSpace << 16 >> 24);
                buffer[counter + 2] = (byte)(colorSpace << 24 >> 24);
                buffer[counter + 3] = (byte)(colorSpace >> 24);
                counter += 4;
            }
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length);
        bb.put(buffer);
        bb.flip();
        return bb;
    }

    public static ByteBuffer compressToByteBuffer(BufferedImage image, String formatName) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(10000000);
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
            ImageWriter writer = ImageIO.getImageWriters(type, formatName).next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(2);
                param.setCompressionQuality(0.8F);
            }

            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            if (os.size() > 900000) {
                throw new RuntimeException("Compressed image too big");
            } else {
                byte[] buffer = os.toByteArray();
                ByteBuffer bb = ByteBuffer.allocate(buffer.length);
                bb.put(buffer);
                bb.flip();
                return bb;
            }
        } catch (IOException var9) {
            var9.printStackTrace();
            return null;
        }
    }
}
