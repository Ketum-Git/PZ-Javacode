// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.veins;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import zombie.iso.Vector2;
import zombie.iso.worldgen.biomes.TileGroup;

public class OreVein {
    private final int centerCellX;
    private final int centerCellY;
    private final int depth;
    private final int centerSquareX;
    private final int centerSquareY;
    private final int armsAmount;
    private final float[] armsOrientation;
    private final float[] armsDistance;
    private final Vector2 startPoint;
    private final Vector2[] endPoints;
    private final List<TileGroup> tileGroups;
    private final float armsProb;
    private final float centerProb;
    private final int centerRadius;

    public OreVein(int cellX, int cellY, OreVeinConfig config, Random rnd) {
        this.centerCellX = cellX;
        this.centerCellY = cellY;
        this.depth = 0;
        this.centerSquareX = rnd.nextInt(256);
        this.centerSquareY = rnd.nextInt(256);
        this.startPoint = new Vector2(this.centerCellX * 256 + this.centerSquareX, this.centerCellY * 256 + this.centerSquareY);
        this.armsAmount = rnd.nextInt(config.getArmsAmountMax() - config.getArmsAmountMin() + 1) + config.getArmsAmountMin();
        this.armsOrientation = new float[this.armsAmount];
        this.armsDistance = new float[this.armsAmount];
        this.endPoints = new Vector2[this.armsAmount];
        float offsetAngle = rnd.nextFloat() * 360.0F;
        float deltaAngle = 360.0F / this.armsAmount;

        for (int i = 0; i < this.armsAmount; i++) {
            float rao = (rnd.nextFloat() * 2.0F - 1.0F) * config.getArmsDeltaAngle();
            this.armsOrientation[i] = deltaAngle * i + offsetAngle + rao;
            this.armsDistance[i] = rnd.nextFloat() * config.getArmsDistMax() + config.getArmsDistMin();
            this.endPoints[i] = new Vector2(
                this.startPoint.x + (float)(Math.sin(Math.toRadians(this.armsOrientation[i])) * this.armsDistance[i]),
                this.startPoint.y + (float)(-Math.cos(Math.toRadians(this.armsOrientation[i])) * this.armsDistance[i])
            );
        }

        this.tileGroups = config.getTiles();
        this.armsProb = config.getArmsProb();
        this.centerProb = config.getCenterProb();
        this.centerRadius = config.getCenterRadius();
    }

    public boolean isValid(int x, int y, Random rnd) {
        Vector2 d2 = new Vector2(this.startPoint.x - x, this.startPoint.y - y);
        float lengthSquared = d2.getLengthSquared();
        if (lengthSquared < this.centerRadius * this.centerRadius && rnd.nextFloat() < this.centerProb) {
            return true;
        } else {
            for (int i = 0; i < this.armsAmount; i++) {
                if (!(lengthSquared > this.armsDistance[i] * this.armsDistance[i])) {
                    Vector2 d1p = new Vector2(this.endPoints[i].x - this.startPoint.x, this.endPoints[i].y - this.startPoint.y);
                    Vector2 d3p = new Vector2(this.endPoints[i].x - x, this.endPoints[i].y - y);
                    d1p.normalize();
                    d3p.normalize();
                    if (Math.abs(d1p.x - d3p.x) < 0.001F && Math.abs(d1p.y - d3p.y) < 0.001F && rnd.nextFloat() < this.armsProb) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public List<TileGroup> getSingleFeatures() {
        return this.tileGroups;
    }

    @Override
    public String toString() {
        return "OreVein{startPoint="
            + this.startPoint
            + ", amountArms="
            + this.armsAmount
            + ", orientationArms="
            + Arrays.toString(this.armsOrientation)
            + ", endPoints="
            + Arrays.toString((Object[])this.endPoints)
            + ", distanceArms="
            + Arrays.toString(this.armsDistance)
            + ", singleFeatures="
            + this.tileGroups
            + "}";
    }
}
