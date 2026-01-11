// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjglx.BufferUtils;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.Vector3;
import zombie.core.textures.Texture;
import zombie.creative.creativerects.OpenSimplexNoise;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;

public final class HeightTerrain {
    private final ByteBuffer buffer;
    public VertexBufferObject vb;
    public static float isoAngle = 62.65607F;
    public static float scale = 0.047085002F;
    OpenSimplexNoise noise = new OpenSimplexNoise(Rand.Next(10000000));
    static float[] lightAmbient = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] lightDiffuse = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] lightPosition = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] specular = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] shininess = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] emission = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] ambient = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static float[] diffuse = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    static ByteBuffer temp = ByteBuffer.allocateDirect(16);

    public HeightTerrain(int widthTiles, int heightTiles) {
        ArrayList<VertexPositionNormalTangentTextureSkin> verts = new ArrayList<>();
        int vertexCount = widthTiles * heightTiles;
        int xVerts = widthTiles;
        int yVerts = heightTiles;
        ArrayList<Integer> indices = new ArrayList<>();
        Vector2 size = new Vector2(2.0F, 0.0F);
        int n = 0;

        for (int x = 0; x < xVerts; x++) {
            for (int y = 0; y < yVerts; y++) {
                float noise = (float)this.calc(x, y);
                noise *= 1.0F;
                noise++;
                VertexPositionNormalTangentTextureSkin vert = null;
                vert = new VertexPositionNormalTangentTextureSkin();
                vert.position = new Vector3();
                vert.position.set(-x, noise * 30.0F, -y);
                vert.normal = new Vector3();
                vert.normal.set(0.0F, 1.0F, 0.0F);
                vert.normal.normalize();
                vert.textureCoordinates = new Vector2();
                vert.textureCoordinates = new Vector2((float)x / (xVerts - 1) * 16.0F, (float)y / (yVerts - 1) * 16.0F);
                verts.add(vert);
            }
        }

        n = 0;

        for (int x = 0; x < xVerts; x++) {
            for (int y = 0; y < yVerts; y++) {
                float noise = (float)this.calc(x, y);
                noise *= 1.0F;
                noise = Math.max(0.0F, ++noise);
                noise = Math.min(1.0F, noise);
                VertexPositionNormalTangentTextureSkin vert = null;
                vert = verts.get(n);
                Vector3 va = new Vector3();
                Vector3 vb = new Vector3();
                float noises21 = (float)this.calc(x + 1, y);
                noises21 *= 1.0F;
                noises21++;
                float noises01 = (float)this.calc(x - 1, y);
                noises01 *= 1.0F;
                noises01++;
                float noises12 = (float)this.calc(x, y + 1);
                noises12 *= 1.0F;
                noises12++;
                float noises10 = (float)this.calc(x, y - 1);
                noises10 *= 1.0F;
                noises10++;
                float s21 = noises21 * 700.0F;
                float s01 = noises01 * 700.0F;
                float s12 = noises12 * 700.0F;
                float s10 = noises10 * 700.0F;
                va.set(size.x, size.y, s21 - s01);
                vb.set(size.y, size.x, s12 - s10);
                va.normalize();
                vb.normalize();
                Vector3 cross = va.cross(vb);
                vert.normal.x(cross.x());
                vert.normal.y(cross.z());
                vert.normal.z(cross.y());
                vert.normal.normalize();
                System.out.println(vert.normal.x() + " , " + vert.normal.y() + ", " + vert.normal.z());
                vert.normal.normalize();
                n++;
            }
        }

        n = 0;

        for (int y = 0; y < yVerts - 1; y++) {
            if ((y & 1) == 0) {
                for (int x = 0; x < xVerts; x++) {
                    indices.add(x + (y + 1) * xVerts);
                    indices.add(x + y * xVerts);
                    n++;
                    n++;
                }
            } else {
                for (int x = xVerts - 1; x > 0; x--) {
                    indices.add(x - 1 + y * xVerts);
                    indices.add(x + (y + 1) * xVerts);
                    n++;
                    n++;
                }
            }
        }

        if ((xVerts & 1) > 0 && yVerts > 2) {
            indices.add((yVerts - 1) * xVerts);
            n++;
        }

        this.vb = new VertexBufferObject();
        ByteBuffer vertBuffer = BufferUtils.createByteBuffer(verts.size() * 36);

        for (int i = 0; i < verts.size(); i++) {
            VertexPositionNormalTangentTextureSkin vert = verts.get(i);
            vertBuffer.putFloat(vert.position.x());
            vertBuffer.putFloat(vert.position.y());
            vertBuffer.putFloat(vert.position.z());
            vertBuffer.putFloat(vert.normal.x());
            vertBuffer.putFloat(vert.normal.y());
            vertBuffer.putFloat(vert.normal.z());
            int col = -1;
            vertBuffer.putInt(-1);
            vertBuffer.putFloat(vert.textureCoordinates.x);
            vertBuffer.putFloat(vert.textureCoordinates.y);
        }

        vertBuffer.flip();
        int[] ind = new int[indices.size()];

        for (int i = 0; i < indices.size(); i++) {
            Integer indice = indices.get(indices.size() - 1 - i);
            ind[i] = indice;
        }

        this.vb.handle = this.vb.LoadSoftwareVBO(vertBuffer, this.vb.handle, ind);
        this.buffer = vertBuffer;
    }

    double calcTerrain(float x, float y) {
        x *= 10.0F;
        y *= 10.0F;
        double res = this.noise.eval(x / 900.0F, y / 600.0F, 0.0);
        res += this.noise.eval(x / 600.0F, y / 600.0F, 0.0) / 4.0;
        res += (this.noise.eval(x / 300.0F, y / 300.0F, 0.0) + 1.0) / 8.0;
        res += (this.noise.eval(x / 150.0F, y / 150.0F, 0.0) + 1.0) / 16.0;
        res += (this.noise.eval(x / 75.0F, y / 75.0F, 0.0) + 1.0) / 32.0;
        double del = (this.noise.eval(x, y, 0.0) + 1.0) / 2.0;
        del *= (this.noise.eval(x, y, 0.0) + 1.0) / 2.0;
        return res;
    }

    double calc(float x, float y) {
        return this.calcTerrain(x, y);
    }

    public void pushView(int ox, int oy, int oz) {
        GL11.glDepthMask(false);
        GL11.glMatrixMode(5889);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        float dist = 0.6F;
        int x1 = 0;
        int y1 = 0;
        int x2 = 0 + IsoCamera.getOffscreenWidth(IsoPlayer.getPlayerIndex());
        int y2 = 0 + IsoCamera.getOffscreenHeight(IsoPlayer.getPlayerIndex());
        double topLeftX = IsoUtils.XToIso(0.0F, 0.0F, 0.0F);
        double topLeftY = IsoUtils.YToIso(0.0F, 0.0F, 0.0F);
        double topRightX = IsoUtils.XToIso(Core.getInstance().getOffscreenWidth(IsoPlayer.getPlayerIndex()), 0.0F, 0.0F);
        double topRightY = IsoUtils.YToIso(x2, 0.0F, 0.0F);
        double bottomRightX = IsoUtils.XToIso(x2, y2, 0.0F);
        double bottomRightY = IsoUtils.YToIso(
            Core.getInstance().getOffscreenWidth(IsoPlayer.getPlayerIndex()), Core.getInstance().getOffscreenHeight(IsoPlayer.getPlayerIndex()), 6.0F
        );
        double bottomLeftX = IsoUtils.XToIso(-128.0F, Core.getInstance().getOffscreenHeight(IsoPlayer.getPlayerIndex()), 6.0F);
        double bottomLeftY = IsoUtils.YToIso(0.0F, y2, 0.0F);
        double screenWidth = bottomRightX - topLeftX;
        double screenHeight = bottomLeftY - topRightY;
        screenWidth = Math.abs(Core.getInstance().getOffscreenWidth(0)) / 1920.0F;
        screenHeight = Math.abs(Core.getInstance().getOffscreenHeight(0)) / 1080.0F;
        GL11.glLoadIdentity();
        GL11.glOrtho(-screenWidth / 2.0, screenWidth / 2.0, -screenHeight / 2.0, screenHeight / 2.0, -10.0, 10.0);
        GL11.glMatrixMode(5888);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glScaled(scale, scale, scale);
        GL11.glRotatef(isoAngle, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(IsoWorld.instance.currentCell.chunkMap[0].getWidthInTiles() / 2, 0.0, IsoWorld.instance.currentCell.chunkMap[0].getWidthInTiles() / 2);
        GL11.glDepthRange(-100.0, 100.0);
    }

    public void popView() {
        GL11.glEnable(3008);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(false);
        GL11.glMatrixMode(5889);
        GL11.glPopMatrix();
        GL11.glMatrixMode(5888);
        GL11.glPopMatrix();
    }

    public void render() {
        GL11.glPushClientAttrib(-1);
        GL11.glPushAttrib(1048575);
        GL11.glDisable(2884);
        GL11.glEnable(2929);
        GL11.glDepthFunc(519);
        GL11.glColorMask(true, true, true, true);
        GL11.glAlphaFunc(519, 0.0F);
        GL11.glDepthFunc(519);
        GL11.glDepthRange(-10.0, 10.0);
        GL11.glEnable(2903);
        GL11.glEnable(2896);
        GL11.glEnable(16384);
        GL11.glEnable(16385);
        GL11.glEnable(2929);
        GL11.glDisable(3008);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3008);
        GL11.glAlphaFunc(519, 0.0F);
        GL11.glDisable(3089);
        this.doLighting();
        GL11.glDisable(2929);
        GL11.glEnable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glCullFace(1029);
        this.pushView(
            IsoPlayer.getInstance().getCurrentSquare().getChunk().wx / 30 * 300, IsoPlayer.getInstance().getCurrentSquare().getChunk().wy / 30 * 300, 0
        );
        Texture.getSharedTexture("media/textures/grass.png").bind();
        this.vb.DrawStrip(null);
        this.popView();
        GL11.glEnable(3042);
        GL11.glDisable(3008);
        GL11.glDisable(2929);
        GL11.glEnable(6144);
        if (PerformanceSettings.modelLighting) {
            GL11.glDisable(2903);
            GL11.glDisable(2896);
            GL11.glDisable(16384);
            GL11.glDisable(16385);
        }

        GL11.glDepthRange(0.0, 100.0);
        SpriteRenderer.ringBuffer.restoreVbos = true;
        GL11.glEnable(2929);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3008);
        GL11.glAlphaFunc(516, 0.0F);
        GL11.glEnable(3553);
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
    }

    private void doLighting() {
        temp.order(ByteOrder.nativeOrder());
        temp.clear();
        GL11.glColorMaterial(1032, 5634);
        GL11.glDisable(2903);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2896);
        GL11.glEnable(16384);
        GL11.glDisable(16385);
        lightAmbient[0] = 0.7F;
        lightAmbient[1] = 0.7F;
        lightAmbient[2] = 0.7F;
        lightAmbient[3] = 0.5F;
        lightDiffuse[0] = 0.5F;
        lightDiffuse[1] = 0.5F;
        lightDiffuse[2] = 0.5F;
        lightDiffuse[3] = 1.0F;
        Vector3 v = new Vector3(1.0F, 1.0F, 1.0F);
        v.normalize();
        lightPosition[0] = -v.x();
        lightPosition[1] = v.y();
        lightPosition[2] = -v.z();
        lightPosition[3] = 0.0F;
        GL11.glLightfv(16384, 4608, temp.asFloatBuffer().put(lightAmbient).flip());
        GL11.glLightfv(16384, 4609, temp.asFloatBuffer().put(lightDiffuse).flip());
        GL11.glLightfv(16384, 4611, temp.asFloatBuffer().put(lightPosition).flip());
        GL11.glLightf(16384, 4615, 0.0F);
        GL11.glLightf(16384, 4616, 0.0F);
        GL11.glLightf(16384, 4617, 0.0F);
        specular[0] = 0.0F;
        specular[1] = 0.0F;
        specular[2] = 0.0F;
        specular[3] = 0.0F;
        GL11.glMaterialfv(1032, 4610, temp.asFloatBuffer().put(specular).flip());
        GL11.glMaterialfv(1032, 5633, temp.asFloatBuffer().put(specular).flip());
        GL11.glMaterialfv(1032, 5632, temp.asFloatBuffer().put(specular).flip());
        ambient[0] = 0.6F;
        ambient[1] = 0.6F;
        ambient[2] = 0.6F;
        ambient[3] = 1.0F;
        diffuse[0] = 0.6F;
        diffuse[1] = 0.6F;
        diffuse[2] = 0.6F;
        diffuse[3] = 0.6F;
        GL11.glMaterialfv(1032, 4608, temp.asFloatBuffer().put(ambient).flip());
        GL11.glMaterialfv(1032, 4609, temp.asFloatBuffer().put(diffuse).flip());
    }
}
