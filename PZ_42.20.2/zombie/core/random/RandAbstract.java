// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.random;

import java.util.Random;

public abstract class RandAbstract implements RandInterface {
    private int id;
    protected Random rand;

    protected RandAbstract() {
    }

    @Override
    public abstract void init();

    protected int Next(int max, Random randomizer) {
        if (max <= 0) {
            return 0;
        } else {
            this.id++;
            if (this.id >= 10000) {
                this.id = 0;
            }

            return randomizer.nextInt(max);
        }
    }

    protected long Next(long max, Random randomizer) {
        return this.Next((int)max, randomizer);
    }

    protected int Next(int min, int max, Random randomizer) {
        if (max == min) {
            return min;
        } else {
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }

            this.id++;
            if (this.id >= 10000) {
                this.id = 0;
            }

            int n = randomizer.nextInt(max - min);
            return n + min;
        }
    }

    protected long Next(long min, long max, Random randomizer) {
        if (max == min) {
            return min;
        } else {
            if (min > max) {
                long temp = min;
                min = max;
                max = temp;
            }

            this.id++;
            if (this.id >= 10000) {
                this.id = 0;
            }

            int n = randomizer.nextInt((int)(max - min));
            return n + min;
        }
    }

    protected float Next(float min, float max, Random randomizer) {
        if (max == min) {
            return min;
        } else {
            if (min > max) {
                float temp = min;
                min = max;
                max = temp;
            }

            this.id++;
            if (this.id >= 10000) {
                this.id = 0;
            }

            return min + randomizer.nextFloat() * (max - min);
        }
    }

    @Override
    public int Next(int max) {
        return this.Next(max, this.rand);
    }

    @Override
    public long Next(long max) {
        return this.Next(max, this.rand);
    }

    @Override
    public int Next(int min, int max) {
        return this.Next(min, max, this.rand);
    }

    @Override
    public long Next(long min, long max) {
        return this.Next(min, max, this.rand);
    }

    @Override
    public float Next(float min, float max) {
        return this.Next(min, max, this.rand);
    }
}
