// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.characters.IsoPlayer;
import zombie.core.opengl.RenderSettings;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoRoom;

public final class IsoRoomLight {
    public static int nextId = 1;
    private static final int SHINE_DIST = 5;
    public int id;
    public IsoRoom room;
    public int x;
    public int y;
    public int z;
    public int width;
    public int height;
    public float r;
    public float g;
    public float b;
    public boolean active;
    public boolean activeJni;
    public boolean hydroPowered = true;

    public IsoRoomLight(IsoRoom room, int x, int y, int z, int width, int height) {
        this.room = room;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.r = 0.9F;
        this.b = 0.8F;
        this.b = 0.7F;
        this.active = room.def.lightsActive;
    }

    public void addInfluence() {
        this.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8F * IsoGridSquare.rmod * 0.7F;
        this.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8F * IsoGridSquare.gmod * 0.7F;
        this.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8F * IsoGridSquare.bmod * 0.7F;
        this.r *= 2.0F;
        this.g *= 2.0F;
        this.b *= 2.0F;
        this.shineIn(this.x - 1, this.y, this.x, this.y + this.height, 5, 0);
        this.shineIn(this.x, this.y - 1, this.x + this.width, this.y, 0, 5);
        this.shineIn(this.x + this.width, this.y, this.x + this.width + 1, this.y + this.height, -5, 0);
        this.shineIn(this.x, this.y + this.height, this.x + this.width, this.y + this.height + 1, 0, -5);
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        this.active = this.room.def.lightsActive;
        if (sq != null || !this.hydroPowered || sq.hasGridPower() || sq != null && sq.haveElectricity()) {
            if (this.active) {
                this.r = 0.9F;
                this.g = 0.8F;
                this.b = 0.7F;

                for (int yy = this.y; yy < this.y + this.height; yy++) {
                    for (int xx = this.x; xx < this.x + this.width; xx++) {
                        sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, this.z);
                        if (sq != null) {
                            sq.setLampostTotalR(sq.getLampostTotalR() + this.r);
                            sq.setLampostTotalG(sq.getLampostTotalG() + this.g);
                            sq.setLampostTotalB(sq.getLampostTotalB() + this.b);
                        }
                    }
                }

                this.shineOut(this.x, this.y, this.x + 1, this.y + this.height, -5, 0);
                this.shineOut(this.x, this.y, this.x + this.width, this.y + 1, 0, -5);
                this.shineOut(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, 5, 0);
                this.shineOut(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0, 5);
            }
        } else {
            this.active = false;
        }
    }

    private void shineOut(int x1, int y1, int x2, int y2, int distX, int distY) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                this.shineOut(x, y, distX, distY);
            }
        }
    }

    private void shineOut(int x, int y, int distX, int distY) {
        if (distX > 0) {
            for (int i = 1; i <= distX; i++) {
                this.shineFromTo(x, y, x + i, y);
            }
        } else if (distX < 0) {
            for (int i = 1; i <= -distX; i++) {
                this.shineFromTo(x, y, x - i, y);
            }
        } else if (distY > 0) {
            for (int i = 1; i <= distY; i++) {
                this.shineFromTo(x, y, x, y + i);
            }
        } else if (distY < 0) {
            for (int i = 1; i <= -distY; i++) {
                this.shineFromTo(x, y, x, y - i);
            }
        }
    }

    private void shineFromTo(int x1, int y1, int x2, int y2) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.z);
        if (sq != null) {
            if (sq.getRoom() != this.room) {
                LosUtil.TestResults test = LosUtil.lineClear(IsoWorld.instance.currentCell, x1, y1, this.z, x2, y2, this.z, false);
                if (test != LosUtil.TestResults.Blocked) {
                    float dist = Math.abs(x1 - x2) + Math.abs(y1 - y2);
                    float del = dist / 5.0F;
                    del = 1.0F - del;
                    del *= del;
                    float totR = del * this.r * 2.0F;
                    float totG = del * this.g * 2.0F;
                    float totB = del * this.b * 2.0F;
                    sq.setLampostTotalR(sq.getLampostTotalR() + totR);
                    sq.setLampostTotalG(sq.getLampostTotalG() + totG);
                    sq.setLampostTotalB(sq.getLampostTotalB() + totB);
                }
            }
        }
    }

    private void shineIn(int x1, int y1, int x2, int y2, int distX, int distY) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                this.shineIn(x, y, distX, distY);
            }
        }
    }

    private void shineIn(int x, int y, int distX, int distY) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, this.z);
        if (sq != null && sq.has(IsoFlagType.exterior)) {
            if (distX > 0) {
                for (int i = 1; i <= distX; i++) {
                    this.shineFromToIn(x, y, x + i, y);
                }
            } else if (distX < 0) {
                for (int i = 1; i <= -distX; i++) {
                    this.shineFromToIn(x, y, x - i, y);
                }
            } else if (distY > 0) {
                for (int i = 1; i <= distY; i++) {
                    this.shineFromToIn(x, y, x, y + i);
                }
            } else if (distY < 0) {
                for (int i = 1; i <= -distY; i++) {
                    this.shineFromToIn(x, y, x, y - i);
                }
            }
        }
    }

    private void shineFromToIn(int x1, int y1, int x2, int y2) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.z);
        if (sq != null) {
            LosUtil.TestResults test = LosUtil.lineClear(IsoWorld.instance.currentCell, x1, y1, this.z, x2, y2, this.z, false);
            if (test != LosUtil.TestResults.Blocked) {
                float dist = Math.abs(x1 - x2) + Math.abs(y1 - y2);
                float del = dist / 5.0F;
                del = 1.0F - del;
                del *= del;
                float totR = del * this.r * 2.0F;
                float totG = del * this.g * 2.0F;
                float totB = del * this.b * 2.0F;
                sq.setLampostTotalR(sq.getLampostTotalR() + totR);
                sq.setLampostTotalG(sq.getLampostTotalG() + totG);
                sq.setLampostTotalB(sq.getLampostTotalB() + totB);
            }
        }
    }

    public void clearInfluence() {
        for (int yy = this.y - 5; yy < this.y + this.height + 5; yy++) {
            for (int xx = this.x - 5; xx < this.x + this.width + 5; xx++) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, this.z);
                if (sq != null) {
                    sq.setLampostTotalR(0.0F);
                    sq.setLampostTotalG(0.0F);
                    sq.setLampostTotalB(0.0F);
                }
            }
        }
    }

    public boolean isInBounds() {
        IsoChunkMap[] ChunkMap = IsoWorld.instance.currentCell.chunkMap;

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            if (!ChunkMap[pn].ignore) {
                int minX = ChunkMap[pn].getWorldXMinTiles();
                int maxX = ChunkMap[pn].getWorldXMaxTiles();
                int minY = ChunkMap[pn].getWorldYMinTiles();
                int maxY = ChunkMap[pn].getWorldYMaxTiles();
                if (this.x - 5 < maxX && this.x + this.width + 5 > minX && this.y - 5 < maxY && this.y + this.height + 5 > minY) {
                    return true;
                }
            }
        }

        return false;
    }
}
