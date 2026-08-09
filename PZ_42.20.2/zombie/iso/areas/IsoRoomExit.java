// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;

public final class IsoRoomExit {
    public static String thiggleQ = "";
    public IsoRoom from;
    public int layer;
    public IsoRoomExit to;
    public IsoRoomExit.ExitType type = IsoRoomExit.ExitType.Door;
    public int x;
    public int y;

    public IsoRoomExit(IsoRoomExit to, int x, int y, int layer) {
        this.to = to;
        this.to.to = this;
        this.layer = layer;
        this.x = x;
        this.y = y;
    }

    public IsoRoomExit(IsoRoom from, IsoRoomExit to, int x, int y, int layer) {
        this.from = from;
        this.to = to;
        this.to.to = this;
        this.layer = layer;
        this.x = x;
        this.y = y;
    }

    public IsoRoomExit(IsoRoom from, int x, int y, int layer) {
        this.from = from;
        this.layer = layer;
        this.x = x;
        this.y = y;
    }

    public IsoObject getDoor(IsoCell cell) {
        IsoGridSquare sq = cell.getGridSquare(this.x, this.y, this.layer);
        if (sq != null) {
            if (!sq.getSpecialObjects().isEmpty() && sq.getSpecialObjects().get(0) instanceof IsoDoor) {
                return sq.getSpecialObjects().get(0);
            }

            if (!sq.getSpecialObjects().isEmpty()
                && sq.getSpecialObjects().get(0) instanceof IsoThumpable
                && ((IsoThumpable)sq.getSpecialObjects().get(0)).isDoor()) {
                return sq.getSpecialObjects().get(0);
            }
        }

        sq = cell.getGridSquare(this.x, this.y + 1, this.layer);
        if (sq != null) {
            if (!sq.getSpecialObjects().isEmpty() && sq.getSpecialObjects().get(0) instanceof IsoDoor) {
                return sq.getSpecialObjects().get(0);
            }

            if (!sq.getSpecialObjects().isEmpty()
                && sq.getSpecialObjects().get(0) instanceof IsoThumpable
                && ((IsoThumpable)sq.getSpecialObjects().get(0)).isDoor()) {
                return sq.getSpecialObjects().get(0);
            }
        }

        sq = cell.getGridSquare(this.x + 1, this.y, this.layer);
        if (sq != null) {
            if (!sq.getSpecialObjects().isEmpty() && sq.getSpecialObjects().get(0) instanceof IsoDoor) {
                return sq.getSpecialObjects().get(0);
            }

            if (!sq.getSpecialObjects().isEmpty()
                && sq.getSpecialObjects().get(0) instanceof IsoThumpable
                && ((IsoThumpable)sq.getSpecialObjects().get(0)).isDoor()) {
                return sq.getSpecialObjects().get(0);
            }
        }

        return null;
    }

    static {
        thiggleQ = thiggleQ + "D:/Dropbox/Zomboid/zombie/build/classes/";
    }

    public static enum ExitType {
        Door,
        Window;
    }
}
