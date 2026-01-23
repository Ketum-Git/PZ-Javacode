// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.textures.ImageData;
import zombie.core.textures.Texture;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.popman.ObjectPool;

public final class IsoPuddlesCompute {
    private static final float Pi = 3.1415F;
    private static float puddlesDirNE;
    private static float puddlesDirNW;
    private static float puddlesDirAll;
    private static float puddlesDirNone;
    private static float puddlesSize;
    private static boolean hdQuality = true;
    private static final Vector2f add = new Vector2f(1.0F, 0.0F);
    private static final Vector3f add_xyy = new Vector3f(1.0F, 0.0F, 0.0F);
    private static final Vector3f add_xxy = new Vector3f(1.0F, 1.0F, 0.0F);
    private static final Vector3f add_xxx = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final Vector3f add_xyx = new Vector3f(1.0F, 0.0F, 1.0F);
    private static final Vector3f add_yxy = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f add_yyx = new Vector3f(0.0F, 0.0F, 1.0F);
    private static final Vector3f add_yxx = new Vector3f(0.0F, 1.0F, 1.0F);
    private static final Vector3f HashVector31 = new Vector3f(17.1F, 31.7F, 32.6F);
    private static final Vector3f HashVector32 = new Vector3f(29.5F, 13.3F, 42.6F);
    private static final ObjectPool<Vector3f> pool_vector3f = new ObjectPool<>(Vector3f::new);
    private static final ArrayList<Vector3f> allocated_vector3f = new ArrayList<>();
    private static final Vector2f temp_vector2f = new Vector2f();
    private static final float puddleInteractionThreshold = 0.24F;

    private static Vector3f allocVector3f(float x, float y, float z) {
        Vector3f v = pool_vector3f.alloc().set(x, y, z);
        allocated_vector3f.add(v);
        return v;
    }

    private static Vector3f allocVector3f(Vector3f other) {
        return allocVector3f(other.x, other.y, other.z);
    }

    private static Vector3f floor(Vector3f a) {
        return allocVector3f(PZMath.fastfloor(a.x), PZMath.fastfloor(a.y), PZMath.fastfloor(a.z));
    }

    private static Vector3f fract(Vector3f a) {
        return allocVector3f(fract(a.x), fract(a.y), fract(a.z));
    }

    private static float fract(float a) {
        return a - PZMath.fastfloor(a);
    }

    private static float mix(float x, float y, float a) {
        return x * (1.0F - a) + y * a;
    }

    private static float FuncHash(Vector3f p) {
        Vector3f p2 = allocVector3f(p.dot(HashVector31), p.dot(HashVector32), 0.0F);
        return fract((float)(Math.sin(p2.x * 2.1 + 1.1) + Math.sin(p2.y * 2.5 + 1.5)));
    }

    private static float FuncNoise(Vector3f p) {
        Vector3f j = floor(p);
        Vector3f f0 = fract(p);
        Vector3f f = allocVector3f(f0.x * f0.x * (4.5F - 3.5F * f0.x), f0.y * f0.y * (4.5F - 3.5F * f0.y), f0.z * f0.z * (4.5F - 3.5F * f0.z));
        float r1 = mix(FuncHash(j), FuncHash(allocVector3f(j).add(add_xyy)), f.x);
        float r2 = mix(FuncHash(allocVector3f(j).add(add_yxy)), FuncHash(allocVector3f(j).add(add_xxy)), f.x);
        float r3 = mix(FuncHash(allocVector3f(j).add(add_yyx)), FuncHash(allocVector3f(j).add(add_xyx)), f.x);
        float r4 = mix(FuncHash(allocVector3f(j).add(add_yxx)), FuncHash(allocVector3f(j).add(add_xxx)), f.x);
        float r12 = mix(r1, r2, f.y);
        float r34 = mix(r3, r4, f.y);
        return mix(r12, r34, f.z);
    }

    private static float PerlinNoise(Vector3f p) {
        if (hdQuality) {
            p.mul(0.5F);
            float f = 0.5F * FuncNoise(p);
            p.mul(3.0F);
            f = (float)(f + 0.25 * FuncNoise(p));
            p.mul(3.0F);
            f = (float)(f + 0.125 * FuncNoise(p));
            return (float)(f * Math.min(1.0, 2.0 * FuncNoise(allocVector3f(p).mul(0.02F)) * Math.min(1.0, 1.0 * FuncNoise(allocVector3f(p).mul(0.1F)))));
        } else {
            return FuncNoise(p) * 0.4F;
        }
    }

    private static float getPuddles(Vector2f uv) {
        float dirNE = puddlesDirNE;
        float dirNW = puddlesDirNW;
        float dirA = puddlesDirAll;
        uv.mul(10.0F);
        float s = 1.02F * puddlesSize;
        s = (float)(s + dirNE * Math.sin((uv.x * 1.0 + uv.y * 2.0) * 3.1415F * 1.0) * Math.cos((uv.x * 1.0 + uv.y * 2.0) * 3.1415F * 1.0) * 2.0);
        s = (float)(s + dirNW * Math.sin((uv.x * 1.0 - uv.y * 2.0) * 3.1415F * 1.0) * Math.cos((uv.x * 1.0 - uv.y * 2.0) * 3.1415F * 1.0) * 2.0);
        s = (float)(s + dirA * 0.3);
        float b = PerlinNoise(allocVector3f(uv.x * 1.0F, 0.0F, uv.y * 2.0F));
        float a = Math.min(0.7F, s * b);
        b = Math.min(0.7F, PerlinNoise(allocVector3f(uv.x * 0.7F, 1.0F, uv.y * 0.7F)));
        return a + b;
    }

    public static float computePuddle(IsoGridSquare square) {
        pool_vector3f.releaseAll(allocated_vector3f);
        allocated_vector3f.clear();
        hdQuality = false;
        if (!Core.getInstance().getUseShaders()) {
            return -0.1F;
        } else if (Core.getInstance().getPerfPuddlesOnLoad() == 3 || Core.getInstance().getPerfPuddles() == 3) {
            return -0.1F;
        } else if (Core.getInstance().getPerfPuddles() > 0 && square.z > 0) {
            return -0.1F;
        } else {
            IsoPuddles isoPuddles = IsoPuddles.getInstance();
            puddlesSize = isoPuddles.getPuddlesSize();
            if (puddlesSize <= 0.0F) {
                return -0.1F;
            } else {
                return PerformanceSettings.puddlesQuality == 2
                    ? GetPuddlesFromLQTexture(CalculatePuddlesUvTexture(square, isoPuddles))
                    : GetPuddlesFromPerlinNoise(CalculatePuddlesUvMain(square, isoPuddles));
            }
        }
    }

    private static Vector2f CalculatePuddlesUvMain(IsoGridSquare square, IsoPuddles isoPuddles) {
        Vector4f WOffset = isoPuddles.getShaderOffsetMain();
        WOffset.x -= 90000.0F;
        WOffset.y -= 640000.0F;
        int camOffX = (int)IsoCamera.frameState.offX;
        int camOffY = (int)IsoCamera.frameState.offY;
        float x = IsoUtils.XToScreen(square.x + 0.5F - square.z * 3.0F, square.y + 0.5F - square.z * 3.0F, 0.0F, 0) - camOffX;
        float y = IsoUtils.YToScreen(square.x + 0.5F - square.z * 3.0F, square.y + 0.5F - square.z * 3.0F, 0.0F, 0) - camOffY;
        x /= IsoCamera.frameState.offscreenWidth;
        y /= IsoCamera.frameState.offscreenHeight;
        if (Core.getInstance().getPerfPuddles() <= 1) {
            square.getPuddles().recalcIfNeeded();
            puddlesDirNE = (square.getPuddles().pdne[0] + square.getPuddles().pdne[2]) * 0.5F;
            puddlesDirNW = (square.getPuddles().pdnw[0] + square.getPuddles().pdnw[2]) * 0.5F;
            puddlesDirAll = (square.getPuddles().pda[0] + square.getPuddles().pda[2]) * 0.5F;
            puddlesDirNone = (square.getPuddles().pnon[0] + square.getPuddles().pnon[2]) * 0.5F;
        } else {
            puddlesDirNE = 0.0F;
            puddlesDirNW = 0.0F;
            puddlesDirAll = 1.0F;
            puddlesDirNone = 0.0F;
        }

        return temp_vector2f.set((x * WOffset.z + WOffset.x) * 8.0E-4F + square.z * 7.0F, (y * WOffset.w + WOffset.y) * 8.0E-4F + square.z * 7.0F);
    }

    private static Vector2f CalculatePuddlesUvTexture(IsoGridSquare square, IsoPuddles isoPuddles) {
        Vector4f WOffset = isoPuddles.getShaderOffsetMain();
        WOffset.y *= -1.0F;
        WOffset.x -= 90000.0F;
        WOffset.y -= 640000.0F;
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        WOffset.x += chunkFloorYSpan;
        WOffset.y += chunkFloorYSpan;
        int camOffX = (int)IsoCamera.frameState.offX;
        int camOffY = (int)IsoCamera.frameState.offY;
        float x = IsoUtils.XToScreen(square.x + 0.5F - square.z * 3.0F, square.y + 0.5F - square.z * 3.0F, 0.0F, 0) - camOffX;
        float y = IsoUtils.YToScreen(square.x + 0.5F - square.z * 3.0F, square.y + 0.5F - square.z * 3.0F, 0.0F, 0) - camOffY;
        x /= IsoCamera.frameState.offscreenWidth;
        y /= IsoCamera.frameState.offscreenHeight;
        if (Core.getInstance().getPerfPuddles() <= 1) {
            square.getPuddles().recalcIfNeeded();
            puddlesDirNE = (square.getPuddles().pdne[0] + square.getPuddles().pdne[2]) * 0.5F;
            puddlesDirNW = (square.getPuddles().pdnw[0] + square.getPuddles().pdnw[2]) * 0.5F;
            puddlesDirAll = (square.getPuddles().pda[0] + square.getPuddles().pda[2]) * 0.5F;
            puddlesDirNone = (square.getPuddles().pnon[0] + square.getPuddles().pnon[2]) * 0.5F;
        } else {
            puddlesDirNE = 0.0F;
            puddlesDirNW = 0.0F;
            puddlesDirAll = 1.0F;
            puddlesDirNone = 0.0F;
        }

        float fragCoord_y = -y;
        return temp_vector2f.set((x * WOffset.z + WOffset.x) * 8.0E-4F + square.z * 7.0F, (fragCoord_y * WOffset.w + WOffset.y) * 8.0E-4F + square.z * 7.0F);
    }

    private static float GetPuddlesFromPerlinNoise(Vector2f uv) {
        float level = (float)Math.pow(getPuddles(uv), 2.0);
        float levelf = (float)Math.min(Math.pow(level, 0.3), 1.0) + level;
        return levelf * puddlesSize - 0.24F;
    }

    private static float GetPuddlesFromLQTexture(Vector2f uv) {
        Texture texture = (Texture)IsoPuddles.getInstance().getHMTexture();
        ByteBuffer textureBuffer = IsoPuddles.getInstance().getHMTextureBuffer();
        float puddleSampleResult = -1.0F;
        if (texture != null && textureBuffer != null) {
            float puddleTextureScale = 0.5F;
            float imageUvX = uv.x * 0.5F % 1.0F;
            float imageUvY = uv.y * 0.5F % 1.0F;
            if (imageUvX < 0.0F) {
                imageUvX++;
            }

            if (imageUvY < 0.0F) {
                imageUvY++;
            }

            int[] result = new int[4];
            ImageData.getPixelDiscard(
                textureBuffer, texture.getWidthHW(), texture.getHeightHW(), (int)(imageUvX * texture.getWidth()), (int)(imageUvY * texture.getHeight()), result
            );
            float r = result[0] / 255.0F;
            float g = result[1] / 255.0F;
            float b = result[2] / 255.0F;
            float level = mix(mix(b, r, puddlesDirNE), g, puddlesDirNW);
            float levelf = (float)Math.min(Math.pow(level, 0.3F), 1.0) + level;
            puddleSampleResult = levelf * puddlesSize - 0.24F;
        }

        return puddleSampleResult;
    }
}
