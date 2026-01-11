// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;

@UsedFromLua
public class FluidConsume extends SealedFluidProperties {
    private static final ConcurrentLinkedDeque<FluidConsume> pool = new ConcurrentLinkedDeque<>();
    private float amount;
    private PoisonEffect poisonEffect = PoisonEffect.None;

    public static FluidConsume Alloc() {
        FluidConsume fluidConsume = pool.poll();
        if (fluidConsume == null) {
            fluidConsume = new FluidConsume();
        }

        return fluidConsume;
    }

    protected static void Release(FluidConsume fluidConsume) {
        fluidConsume.reset();
        pool.offer(fluidConsume);
    }

    private FluidConsume() {
    }

    public void release() {
        Release(this);
    }

    private void reset() {
        this.clear();
    }

    @Override
    public void clear() {
        super.clear();
        this.amount = 0.0F;
        this.poisonEffect = PoisonEffect.None;
    }

    public static FluidConsume combine(FluidConsume a, FluidConsume b) {
        FluidConsume consume = Alloc();
        consume.setAmount(a.getAmount() + b.getAmount());
        consume.setPoisonEffect(a.getPoisonEffect());
        consume.setPoisonEffect(b.getPoisonEffect());
        return consume;
    }

    public FluidConsume combineWith(FluidConsume b) {
        FluidConsume combined = combine(this, b);
        this.setAmount(combined.getAmount());
        this.setPoisonEffect(combined.getPoisonEffect());
        combined.release();
        return this;
    }

    protected void setAmount(float amount) {
        this.amount = amount;
    }

    protected void setPoisonEffect(PoisonEffect poisonEffect) {
        if (poisonEffect.getLevel() > this.poisonEffect.getLevel()) {
            this.poisonEffect = poisonEffect;
        }
    }

    public float getAmount() {
        return this.amount;
    }

    public PoisonEffect getPoisonEffect() {
        return this.poisonEffect;
    }

    public static void Save(FluidConsume fluidConsume, ByteBuffer output) throws IOException {
        output.putFloat(fluidConsume.amount);
        output.putInt(fluidConsume.poisonEffect.getLevel());
        fluidConsume.save(output);
    }

    public static FluidConsume Load(ByteBuffer input, int WorldVersion) throws IOException {
        return Load(Alloc(), input, WorldVersion);
    }

    public static FluidConsume Load(FluidConsume fluidConsume, ByteBuffer input, int WorldVersion) throws IOException {
        fluidConsume.amount = input.getFloat();
        fluidConsume.poisonEffect = PoisonEffect.FromLevel(input.getInt());
        fluidConsume.load(input, WorldVersion);
        return fluidConsume;
    }
}
