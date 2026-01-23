// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;

@UsedFromLua
public class FluidSample {
    private static final ConcurrentLinkedDeque<FluidSample> pool = new ConcurrentLinkedDeque<>();
    private final ArrayList<FluidInstance> fluids = new ArrayList<>();
    private boolean sealed;
    private float amount;

    public static FluidSample Alloc() {
        FluidSample obj = pool.poll();
        if (obj == null) {
            obj = new FluidSample();
        }

        return obj;
    }

    protected static void Release(FluidSample obj) {
        obj.reset();

        assert !Core.debug || !pool.contains(obj) : "Object already in pool.";

        pool.offer(obj);
    }

    private FluidSample() {
    }

    public void release() {
        Release(this);
    }

    private void reset() {
        this.sealed = false;
        this.amount = 0.0F;
        if (!this.fluids.isEmpty()) {
            for (int i = 0; i < this.fluids.size(); i++) {
                FluidInstance.Release(this.fluids.get(i));
            }

            this.fluids.clear();
        }
    }

    public void clear() {
        this.reset();
    }

    protected void addFluid(FluidInstance fluid) {
        if (!this.sealed) {
            this.fluids.add(fluid.copy());
            this.amount = this.amount + fluid.getAmount();
        } else {
            DebugLog.General.error("FluidSample is sealed");
        }
    }

    protected FluidSample seal() {
        this.sealed = true;
        return this;
    }

    public FluidSample copy() {
        FluidSample copy = Alloc();

        for (int i = 0; i < this.fluids.size(); i++) {
            copy.fluids.add(this.fluids.get(i).copy());
        }

        copy.sealed = true;
        copy.amount = this.amount;
        return copy;
    }

    public boolean isEmpty() {
        return this.fluids.isEmpty() || this.amount <= 0.0F;
    }

    public boolean isPureFluid() {
        return this.fluids.size() == 1;
    }

    public float getAmount() {
        return this.amount;
    }

    public int size() {
        return this.fluids.size();
    }

    public float getPercentage(int index) {
        if (index >= 0 && index < this.fluids.size()) {
            return this.fluids.get(index).getPercentage();
        } else {
            DebugLog.General.error("FluidSample index out of bounds");
            return 0.0F;
        }
    }

    public Fluid getFluid(int index) {
        if (index >= 0 && index < this.fluids.size()) {
            return this.fluids.get(index).getFluid();
        } else {
            DebugLog.General.error("FluidSample index out of bounds");
            return null;
        }
    }

    public FluidInstance getFluidInstance(int index) {
        if (index >= 0 && index < this.fluids.size()) {
            return this.fluids.get(index);
        } else {
            DebugLog.General.error("FluidSample index out of bounds");
            return null;
        }
    }

    public FluidInstance getFluidInstance(Fluid fluid) {
        for (int i = 0; i < this.fluids.size(); i++) {
            if (this.fluids.get(i).getFluid().equals(fluid)) {
                return this.fluids.get(i);
            }
        }

        return null;
    }

    public Fluid getPrimaryFluid() {
        if (this.isEmpty()) {
            return null;
        } else if (this.fluids.size() == 1) {
            return this.fluids.get(0).getFluid();
        } else {
            FluidInstance primary = null;

            for (int i = 0; i < this.fluids.size(); i++) {
                FluidInstance test = this.fluids.get(i);
                if (primary == null || test.getAmount() > primary.getAmount()) {
                    primary = test;
                }
            }

            return primary.getFluid();
        }
    }

    public Color getColor() {
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;
        Color color = new Color();

        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fluid = this.fluids.get(i);
            r += fluid.getColor().r * this.getPercentage(i);
            g += fluid.getColor().g * this.getPercentage(i);
            b += fluid.getColor().b * this.getPercentage(i);
        }

        color.set(r, g, b);
        return color;
    }

    public void scaleToAmount(float amount) {
        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fluidInstance = this.fluids.get(i);
            fluidInstance.setAmount(amount * fluidInstance.getPercentage());
        }

        this.amount = amount;
    }

    public static FluidSample combine(FluidSample a, FluidSample b) {
        FluidSample sample = Alloc();

        for (int i = 0; i < a.size(); i++) {
            sample.addFluid(a.getFluidInstance(i));
        }

        for (int i = 0; i < b.size(); i++) {
            FluidInstance existingFluid = sample.getFluidInstance(b.getFluid(i));
            if (existingFluid != null) {
                existingFluid.setAmount(existingFluid.getAmount() + b.getFluidInstance(i).getAmount());
                sample.amount = sample.amount + b.getFluidInstance(i).getAmount();
            } else {
                sample.addFluid(b.getFluidInstance(i));
            }
        }

        return sample.seal();
    }

    public FluidSample combineWith(FluidSample b) {
        FluidSample combined = combine(this, b);
        this.reset();

        for (int i = 0; i < combined.size(); i++) {
            this.addFluid(combined.getFluidInstance(i));
        }

        this.amount = combined.getAmount();
        combined.release();
        return this.seal();
    }

    public static void Save(FluidSample fluidSample, ByteBuffer output) throws IOException {
        output.put((byte)(fluidSample.sealed ? 1 : 0));
        output.putFloat(fluidSample.amount);
        output.putInt(fluidSample.size());

        for (int i = 0; i < fluidSample.size(); i++) {
            FluidInstance.save(fluidSample.fluids.get(i), output);
        }
    }

    public static FluidSample Load(ByteBuffer input, int WorldVersion) throws IOException {
        return Load(Alloc(), input, WorldVersion);
    }

    public static FluidSample Load(FluidSample fluidSample, ByteBuffer input, int WorldVersion) throws IOException {
        fluidSample.sealed = input.get() == 1;
        fluidSample.amount = input.getFloat();
        float amount = 0.0F;
        int size = input.getInt();

        for (int i = 0; i < size; i++) {
            FluidInstance fluidInstance = FluidInstance.load(input, WorldVersion);
            fluidSample.fluids.add(fluidInstance);
            amount += fluidInstance.getAmount();
        }

        if (amount != fluidSample.amount) {
            DebugLog.General.warn("Fluids amount mismatch with saved amount, correcting. save=" + fluidSample.amount + ", fluids=" + amount);
            fluidSample.amount = amount;
        }

        return fluidSample;
    }
}
