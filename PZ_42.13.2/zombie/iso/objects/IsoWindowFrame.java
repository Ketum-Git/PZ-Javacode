// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public class IsoWindowFrame extends IsoObject implements BarricadeAble {
    private boolean north;

    public IsoWindowFrame(IsoCell cell) {
        super(cell);
    }

    public IsoWindowFrame(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north) {
        super(cell, gridSquare, gid);
        this.north = north;
    }

    @Override
    public String getObjectName() {
        return "IsoWindowFrame";
    }

    @Override
    public boolean haveSheetRope() {
        return IsoWindow.isTopOfSheetRopeHere(this.getSquare(), this.getNorth());
    }

    @Override
    public int countAddSheetRope() {
        return countAddSheetRope(this);
    }

    @Override
    public boolean canAddSheetRope() {
        return !this.canClimbThrough(null) ? false : canAddSheetRope(this);
    }

    @Override
    public boolean addSheetRope(IsoPlayer player, String itemType) {
        return addSheetRope(this, player, itemType);
    }

    @Override
    public boolean removeSheetRope(IsoPlayer player) {
        return removeSheetRope(this, player);
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        IsoWindow window = this.getWindow();
        if (window != null) {
            return window.getThumpableFor(chr);
        } else {
            IsoBarricade barricade = this.getBarricadeForCharacter(chr);
            if (barricade != null) {
                return barricade;
            } else {
                barricade = this.getBarricadeOppositeCharacter(chr);
                return barricade != null ? barricade : null;
            }
        }
    }

    @Override
    public boolean isBarricaded() {
        IsoBarricade barricade = this.getBarricadeOnSameSquare();
        if (barricade == null) {
            barricade = this.getBarricadeOnOppositeSquare();
        }

        return barricade != null;
    }

    @Override
    public boolean isBarricadeAllowed() {
        return this.getWindow() == null;
    }

    @Override
    public IsoBarricade getBarricadeOnSameSquare() {
        return this.hasWindow() ? null : IsoBarricade.GetBarricadeOnSquare(this.square, this.getNorth() ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public IsoBarricade getBarricadeOnOppositeSquare() {
        return this.hasWindow() ? null : IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.getNorth() ? IsoDirections.S : IsoDirections.E);
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        return this.hasWindow() ? null : IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        return this.hasWindow() ? null : IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        return this.getSquare() == null ? null : this.getSquare().getAdjacentSquare(this.getNorth() ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.north ? 1 : 0));
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.north = input.get() == 1;
    }

    public IsoWindow getWindow() {
        return this.getSquare() == null ? null : this.getSquare().getWindow(this.getNorth());
    }

    public boolean hasWindow() {
        return this.getWindow() != null;
    }

    public boolean canClimbThrough(IsoGameCharacter chr) {
        return canClimbThrough(this, chr);
    }

    public IsoCurtain getCurtain() {
        return getCurtain(this);
    }

    public IsoCurtain HasCurtains() {
        return this.getCurtain();
    }

    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        return getAddSheetSquare(this, chr);
    }

    public void addSheet(IsoGameCharacter chr) {
        addSheet(this, chr);
    }

    private static IsoWindowFrame.Direction getDirection(IsoObject o) {
        if (o instanceof IsoWindowFrame isoWindowFrame) {
            return isoWindowFrame.getNorth() ? IsoWindowFrame.Direction.NORTH : IsoWindowFrame.Direction.WEST;
        } else if (!(o instanceof IsoWindow) && !(o instanceof IsoThumpable)) {
            if (o == null || o.getProperties() == null || o.getObjectIndex() == -1) {
                return IsoWindowFrame.Direction.INVALID;
            } else if (o.getProperties().has(IsoFlagType.WindowN)) {
                return IsoWindowFrame.Direction.NORTH;
            } else {
                return o.getProperties().has(IsoFlagType.WindowW) ? IsoWindowFrame.Direction.WEST : IsoWindowFrame.Direction.INVALID;
            }
        } else {
            return IsoWindowFrame.Direction.INVALID;
        }
    }

    public static boolean isWindowFrame(IsoObject o) {
        return getDirection(o).isValid();
    }

    public static boolean isWindowFrame(IsoObject o, boolean north) {
        IsoWindowFrame.Direction dir = getDirection(o);
        return north && dir == IsoWindowFrame.Direction.NORTH || !north && dir == IsoWindowFrame.Direction.WEST;
    }

    public static int countAddSheetRope(IsoObject o) {
        IsoWindowFrame.Direction dir = getDirection(o);
        return dir.isValid() ? IsoWindow.countAddSheetRope(o.getSquare(), dir == IsoWindowFrame.Direction.NORTH) : 0;
    }

    public static boolean canAddSheetRope(IsoObject o) {
        IsoWindowFrame.Direction dir = getDirection(o);
        return dir.isValid() && IsoWindow.canAddSheetRope(o.getSquare(), dir == IsoWindowFrame.Direction.NORTH);
    }

    public static boolean haveSheetRope(IsoObject o) {
        IsoWindowFrame.Direction dir = getDirection(o);
        return dir.isValid() && IsoWindow.isTopOfSheetRopeHere(o.getSquare(), dir == IsoWindowFrame.Direction.NORTH);
    }

    public static boolean addSheetRope(IsoObject o, IsoPlayer player, String itemType) {
        return !canAddSheetRope(o) ? false : IsoWindow.addSheetRope(player, o.getSquare(), getDirection(o) == IsoWindowFrame.Direction.NORTH, itemType);
    }

    public static boolean removeSheetRope(IsoObject o, IsoPlayer player) {
        return !haveSheetRope(o) ? false : IsoWindow.removeSheetRope(player, o.getSquare(), getDirection(o) == IsoWindowFrame.Direction.NORTH);
    }

    public static IsoGridSquare getOppositeSquare(IsoObject o) {
        IsoWindowFrame.Direction dir = getDirection(o);
        if (!dir.isValid()) {
            return null;
        } else {
            boolean north = dir == IsoWindowFrame.Direction.NORTH;
            return o.getSquare().getAdjacentSquare(north ? IsoDirections.N : IsoDirections.W);
        }
    }

    public static IsoGridSquare getIndoorSquare(IsoObject o) {
        IsoWindowFrame.Direction dir = getDirection(o);
        if (!dir.isValid()) {
            return null;
        } else {
            IsoGridSquare sqThis = o.getSquare();
            if (sqThis.getRoom() != null) {
                return sqThis;
            } else {
                IsoGridSquare sq = getOppositeSquare(o);
                return sq != null && sq.getRoom() != null ? sq : null;
            }
        }
    }

    public static IsoCurtain getCurtain(IsoObject o) {
        IsoWindowFrame.Direction dir = getDirection(o);
        if (!dir.isValid()) {
            return null;
        } else {
            boolean north = dir == IsoWindowFrame.Direction.NORTH;
            IsoCurtain curtain = o.getSquare().getCurtain(north ? IsoObjectType.curtainN : IsoObjectType.curtainW);
            if (curtain != null) {
                return curtain;
            } else {
                IsoGridSquare square = getOppositeSquare(o);
                return square == null ? null : square.getCurtain(north ? IsoObjectType.curtainS : IsoObjectType.curtainE);
            }
        }
    }

    public static IsoGridSquare getAddSheetSquare(IsoObject o, IsoGameCharacter chr) {
        IsoWindowFrame.Direction dir = getDirection(o);
        if (!dir.isValid()) {
            return null;
        } else {
            boolean north = dir == IsoWindowFrame.Direction.NORTH;
            if (chr != null && chr.getCurrentSquare() != null) {
                IsoGridSquare sqChr = chr.getCurrentSquare();
                IsoGridSquare sqThis = o.getSquare();
                if (north) {
                    if (sqChr.getY() < sqThis.getY()) {
                        return sqThis.getAdjacentSquare(IsoDirections.N);
                    }
                } else if (sqChr.getX() < sqThis.getX()) {
                    return sqThis.getAdjacentSquare(IsoDirections.W);
                }

                return sqThis;
            } else {
                return null;
            }
        }
    }

    public static void addSheet(IsoObject o, IsoGameCharacter chr) {
        IsoWindowFrame.Direction dir = getDirection(o);
        if (dir.isValid()) {
            boolean north = dir == IsoWindowFrame.Direction.NORTH;
            IsoGridSquare sq = getIndoorSquare(o);
            if (sq == null) {
                sq = o.getSquare();
            }

            if (chr != null) {
                sq = getAddSheetSquare(o, chr);
            }

            if (sq != null) {
                IsoObjectType curtainType;
                if (sq == o.getSquare()) {
                    curtainType = north ? IsoObjectType.curtainN : IsoObjectType.curtainW;
                } else {
                    curtainType = north ? IsoObjectType.curtainS : IsoObjectType.curtainE;
                }

                if (sq.getCurtain(curtainType) == null) {
                    int gid = 16;
                    if (curtainType == IsoObjectType.curtainE) {
                        gid++;
                    }

                    if (curtainType == IsoObjectType.curtainS) {
                        gid += 3;
                    }

                    if (curtainType == IsoObjectType.curtainN) {
                        gid += 2;
                    }

                    gid += 4;
                    IsoCurtain curtain = new IsoCurtain(o.getCell(), sq, "fixtures_windows_curtains_01_" + gid, north);
                    sq.AddSpecialTileObject(curtain);
                    if (!GameClient.client) {
                        InventoryItem item = chr.getInventory().FindAndReturn("Sheet");
                        chr.getInventory().Remove(item);
                        if (GameServer.server) {
                            GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
                        }
                    }

                    if (GameServer.server) {
                        curtain.transmitCompleteItemToClients();
                    }
                }
            }
        }
    }

    public static boolean canClimbThrough(IsoObject o, IsoGameCharacter chr) {
        IsoWindowFrame.Direction dir = getDirection(o);
        if (!dir.isValid()) {
            return false;
        } else if (o.getSquare() == null) {
            return false;
        } else {
            IsoWindow window = o.getSquare().getWindow(dir == IsoWindowFrame.Direction.NORTH);
            if (window != null && window.isBarricaded()) {
                return false;
            } else if (o instanceof IsoWindowFrame isoWindowFrame && isoWindowFrame.isBarricaded()) {
                return false;
            } else {
                if (chr != null) {
                    IsoGridSquare oppositeSq = dir == IsoWindowFrame.Direction.NORTH
                        ? o.getSquare().getAdjacentSquare(IsoDirections.N)
                        : o.getSquare().getAdjacentSquare(IsoDirections.W);
                    if (!IsoWindow.canClimbThroughHelper(chr, o.getSquare(), oppositeSq, dir == IsoWindowFrame.Direction.NORTH)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    private static enum Direction {
        INVALID,
        NORTH,
        WEST;

        public boolean isValid() {
            return this != INVALID;
        }
    }
}
