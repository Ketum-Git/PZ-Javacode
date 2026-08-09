// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import org.joml.Vector2f;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;

@UsedFromLua
public final class IsoUtils {
    public static float clamp(float x, float minVal, float maxVal) {
        return Math.min(Math.max(x, minVal), maxVal);
    }

    public static float lerp(float val, float min, float max) {
        return max == min ? min : (clamp(val, min, max) - min) / (max - min);
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float DistanceTo(float fromX, float fromY, float toX, float toY) {
        return (float)Math.sqrt(Math.pow(toX - fromX, 2.0) + Math.pow(toY - fromY, 2.0));
    }

    public static float DistanceTo2D(float fromX, float fromY, float toX, float toY) {
        return (float)Math.sqrt(Math.pow(toX - fromX, 2.0) + Math.pow(toY - fromY, 2.0));
    }

    public static float DistanceTo(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        return (float)Math.sqrt(Math.pow(toX - fromX, 2.0) + Math.pow(toY - fromY, 2.0) + Math.pow(toZ - fromZ, 2.0));
    }

    public static float DistanceToSquared(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        return (float)(Math.pow(toX - fromX, 2.0) + Math.pow(toY - fromY, 2.0) + Math.pow(toZ - fromZ, 2.0));
    }

    public static float DistanceToSquared(float fromX, float fromY, float toX, float toY) {
        return (float)(Math.pow(toX - fromX, 2.0) + Math.pow(toY - fromY, 2.0));
    }

    public static float DistanceManhatten(float fromX, float fromY, float toX, float toY) {
        return Math.abs(toX - fromX) + Math.abs(toY - fromY);
    }

    public static float DistanceManhatten(float fromX, float fromY, float toX, float toY, float fromZ, float toZ) {
        return Math.abs(toX - fromX) + Math.abs(toY - fromY) + Math.abs(toZ - fromZ) * 2.0F;
    }

    public static float DistanceManhattenSquare(float fromX, float fromY, float toX, float toY) {
        return Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
    }

    public static float XToIso(float screenX, float screenY, float floor) {
        return XToIso(IsoPlayer.getPlayerIndex(), screenX, screenY, floor);
    }

    public static float XToIso(int playerIndex, float screenX, float screenY, float floor) {
        float px = screenX + IsoCamera.getOffX(playerIndex);
        float py = screenY + IsoCamera.getOffY(playerIndex);
        return (px + 2.0F * py) / (64.0F * Core.tileScale) + 3.0F * floor;
    }

    public static float XToIsoTrue(float screenX, float screenY, int floor) {
        float px = screenX + (int)IsoCamera.cameras[IsoPlayer.getPlayerIndex()].offX;
        float py = screenY + (int)IsoCamera.cameras[IsoPlayer.getPlayerIndex()].offY;
        return (px + 2.0F * py) / (64.0F * Core.tileScale) + 3.0F * floor;
    }

    public static float XToScreen(float objectX, float objectY, float objectZ, int screenZ) {
        float sx = 0.0F;
        sx += objectX * (32 * Core.tileScale);
        return sx - objectY * (32 * Core.tileScale);
    }

    public static float XToScreenInt(int objectX, int objectY, int objectZ, int screenZ) {
        return XToScreen(objectX, objectY, objectZ, screenZ);
    }

    public static float YToScreenExact(float objectX, float objectY, float objectZ, int screenZ) {
        return YToScreen(objectX, objectY, objectZ, screenZ) - IsoCamera.getOffY();
    }

    public static float XToScreenExact(float objectX, float objectY, float objectZ, int screenZ) {
        return XToScreen(objectX, objectY, objectZ, screenZ) - IsoCamera.getOffX();
    }

    public static float YToIso(float screenX, float screenY, float floor) {
        return YToIso(IsoPlayer.getPlayerIndex(), screenX, screenY, floor);
    }

    public static float YToIso(int playerIndex, float screenX, float screenY, float floor) {
        float px = screenX + IsoCamera.getOffX(playerIndex);
        float py = screenY + IsoCamera.getOffY(playerIndex);
        return (px - 2.0F * py) / (-64.0F * Core.tileScale) + 3.0F * floor;
    }

    public static float YToScreen(float objectX, float objectY, float objectZ, int screenZ) {
        float sy = 0.0F;
        sy += objectY * (16 * Core.tileScale);
        sy += objectX * (16 * Core.tileScale);
        return sy + (screenZ - objectZ) * (96 * Core.tileScale);
    }

    public static float YToScreenInt(int objectX, int objectY, int objectZ, int screenZ) {
        return YToScreen(objectX, objectY, objectZ, screenZ);
    }

    public static boolean isSimilarDirection(IsoGameCharacter chr, float xA, float yA, float xB, float yB, float similar) {
        Vector2f va = new Vector2f(xA - chr.getX(), yA - chr.getY());
        va.normalize();
        Vector2f vnb = new Vector2f(chr.getX() - xB, chr.getY() - yB);
        vnb.normalize();
        va.add(vnb);
        return va.length() < similar;
    }
}
