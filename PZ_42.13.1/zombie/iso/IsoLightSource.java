// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoLightSwitch;

@UsedFromLua
public class IsoLightSource {
    public static int nextId = 1;
    public int id;
    public int x;
    public int y;
    public int z;
    public float r;
    public float g;
    public float b;
    public float rJni;
    public float gJni;
    public float bJni;
    public int radius;
    public boolean active;
    public boolean wasActive;
    public boolean activeJni;
    public int life = -1;
    public int startlife = -1;
    public IsoBuilding localToBuilding;
    public boolean hydroPowered;
    public ArrayList<IsoLightSwitch> switches = new ArrayList<>(0);
    public IsoChunk chunk;
    public Object lightMap;

    public IsoLightSource(int x, int y, int z, float r, float g, float b, int radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.active = true;
    }

    public IsoLightSource(int x, int y, int z, float r, float g, float b, int radius, IsoBuilding building) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.active = true;
        this.localToBuilding = building;
    }

    public IsoLightSource(int x, int y, int z, float r, float g, float b, int radius, int life) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.active = true;
        this.startlife = this.life = life;
    }

    @Deprecated
    public void update() {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (!this.hydroPowered || sq.hasGridPower() || sq != null && sq.haveElectricity()) {
            if (this.active) {
                if (this.localToBuilding != null) {
                    this.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                    this.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                    this.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                }

                if (this.life > 0) {
                    this.life--;
                }

                if (this.localToBuilding != null && sq != null) {
                    this.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8F * IsoGridSquare.rmod * 0.7F;
                    this.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8F * IsoGridSquare.gmod * 0.7F;
                    this.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8F * IsoGridSquare.bmod * 0.7F;
                }

                for (int xx = this.x - this.radius; xx < this.x + this.radius; xx++) {
                    for (int yy = this.y - this.radius; yy < this.y + this.radius; yy++) {
                        for (int zz = 0; zz < 8; zz++) {
                            sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, zz);
                            if (sq != null && (this.localToBuilding == null || this.localToBuilding == sq.getBuilding())) {
                                LosUtil.TestResults test = LosUtil.lineClear(
                                    sq.getCell(),
                                    PZMath.fastfloor((float)this.x),
                                    PZMath.fastfloor((float)this.y),
                                    this.z,
                                    sq.getX(),
                                    sq.getY(),
                                    sq.getZ(),
                                    false
                                );
                                if (sq.getX() == this.x && sq.getY() == this.y && sq.getZ() == this.z || test != LosUtil.TestResults.Blocked) {
                                    float del = 0.0F;
                                    float dist;
                                    if (Math.abs(sq.getZ() - this.z) <= 1) {
                                        dist = IsoUtils.DistanceTo(this.x, this.y, 0.0F, sq.getX(), sq.getY(), 0.0F);
                                    } else {
                                        dist = IsoUtils.DistanceTo(this.x, this.y, this.z, sq.getX(), sq.getY(), sq.getZ());
                                    }

                                    if (!(dist > this.radius)) {
                                        del = dist / this.radius;
                                        del = 1.0F - del;
                                        del *= del;
                                        if (this.life > -1) {
                                            del *= (float)this.life / this.startlife;
                                        }

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
                    }
                }
            }
        } else {
            this.active = false;
        }
    }

    /**
     * @return the x
     */
    public int getX() {
        return this.x;
    }

    /**
     * 
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return this.y;
    }

    /**
     * 
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * @return the z
     */
    public int getZ() {
        return this.z;
    }

    /**
     * 
     * @param z the z to set
     */
    public void setZ(int z) {
        z = Math.max(-32, z);
        z = Math.min(31, z);
        this.z = z;
    }

    /**
     * @return the r
     */
    public float getR() {
        return this.r;
    }

    /**
     * 
     * @param r the r to set
     */
    public void setR(float r) {
        this.r = r;
    }

    /**
     * @return the g
     */
    public float getG() {
        return this.g;
    }

    /**
     * 
     * @param g the g to set
     */
    public void setG(float g) {
        this.g = g;
    }

    /**
     * @return the b
     */
    public float getB() {
        return this.b;
    }

    /**
     * 
     * @param b the b to set
     */
    public void setB(float b) {
        this.b = b;
    }

    /**
     * @return the radius
     */
    public int getRadius() {
        return this.radius;
    }

    /**
     * 
     * @param radius the radius to set
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    /**
     * @return the bActive
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * 
     * @param bActive the bActive to set
     */
    public void setActive(boolean bActive) {
        this.active = bActive;
    }

    /**
     * @return the bWasActive
     */
    public boolean wasActive() {
        return this.wasActive;
    }

    /**
     * 
     * @param bWasActive the bWasActive to set
     */
    public void setWasActive(boolean bWasActive) {
        this.wasActive = bWasActive;
    }

    /**
     * @return the switches
     */
    public ArrayList<IsoLightSwitch> getSwitches() {
        return this.switches;
    }

    /**
     * 
     * @param switches the switches to set
     */
    public void setSwitches(ArrayList<IsoLightSwitch> switches) {
        this.switches = switches;
    }

    public void clearInfluence() {
        for (int xx = this.x - this.radius; xx < this.x + this.radius; xx++) {
            for (int yy = this.y - this.radius; yy < this.y + this.radius; yy++) {
                for (int zz = 0; zz < 8; zz++) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, zz);
                    if (sq != null) {
                        sq.setLampostTotalR(0.0F);
                        sq.setLampostTotalG(0.0F);
                        sq.setLampostTotalB(0.0F);
                    }
                }
            }
        }
    }

    public boolean isInBounds(int minX, int minY, int maxX, int maxY) {
        return this.x >= minX && this.x < maxX && this.y >= minY && this.y < maxY;
    }

    public boolean isInBounds() {
        IsoChunkMap[] ChunkMap = IsoWorld.instance.currentCell.chunkMap;

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            if (!ChunkMap[pn].ignore) {
                int minX = ChunkMap[pn].getWorldXMinTiles();
                int maxX = ChunkMap[pn].getWorldXMaxTiles();
                int minY = ChunkMap[pn].getWorldYMinTiles();
                int maxY = ChunkMap[pn].getWorldYMaxTiles();
                if (this.isInBounds(minX, minY, maxX, maxY)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isHydroPowered() {
        return this.hydroPowered;
    }

    public IsoBuilding getLocalToBuilding() {
        return this.localToBuilding;
    }
}
