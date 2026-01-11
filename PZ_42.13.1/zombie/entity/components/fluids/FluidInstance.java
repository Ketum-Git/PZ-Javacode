// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.ColorMixer;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
public class FluidInstance {
    private static final ConcurrentLinkedDeque<FluidInstance> pool = new ConcurrentLinkedDeque<>();
    @DebugNonRecursive
    private FluidContainer parent;
    private Fluid fluid;
    private float amount;
    private float percentage;
    private final Color color = new Color();

    private static FluidInstance Alloc() {
        FluidInstance fluidInstance = pool.poll();
        if (fluidInstance == null) {
            fluidInstance = new FluidInstance();
        }

        return fluidInstance;
    }

    protected static FluidInstance Alloc(Fluid fluid) {
        FluidInstance fluidInstance = Alloc();
        fluidInstance.fluid = fluid;
        fluidInstance.color.set(fluid.getColor());
        return fluidInstance;
    }

    protected static void Release(FluidInstance fluidInstance) {
        fluidInstance.reset();
        if (!DebugOptions.instance.checks.objectPoolContains.getValue() || !pool.contains(fluidInstance)) {
            pool.offer(fluidInstance);
        }
    }

    protected static void save(FluidInstance fluidInstance, ByteBuffer output) throws IOException {
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (fluidInstance.getFluid().getFluidType() != FluidType.Modded) {
            header.addFlags(1);
            output.put(fluidInstance.getFluid().getFluidType().getId());
        } else {
            header.addFlags(2);
            GameWindow.WriteString(output, fluidInstance.getFluid().getFluidTypeString());
        }

        if (!fluidInstance.getColor().equals(fluidInstance.getFluid().getColor())) {
            header.addFlags(4);
            fluidInstance.getColor().saveCompactNoAlpha(output);
        }

        output.putFloat(fluidInstance.amount);
        header.write();
        header.release();
    }

    protected static FluidInstance load(ByteBuffer input, int WorldVersion) throws IOException {
        FluidInstance fluidInstance = Alloc();
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        if (header.hasFlags(1)) {
            byte id = input.get();
            Fluid fluid = Fluid.Get(FluidType.FromId(id));
            fluidInstance.fluid = fluid;
            if (fluid != null) {
                fluidInstance.color.set(fluid.getColor());
            }
        } else if (header.hasFlags(2)) {
            String type = GameWindow.ReadString(input);
            Fluid fluid = Fluid.Get(type);
            fluidInstance.fluid = fluid;
            if (fluid != null) {
                fluidInstance.color.set(fluid.getColor());
            }
        }

        if (header.hasFlags(4)) {
            fluidInstance.getColor().loadCompactNoAlpha(input);
        }

        fluidInstance.amount = input.getFloat();
        header.release();
        return fluidInstance;
    }

    private FluidInstance() {
    }

    public FluidInstance copy() {
        FluidInstance copy = Alloc();
        copy.fluid = this.fluid;
        copy.amount = this.amount;
        copy.percentage = this.percentage;
        copy.color.set(this.color);
        return copy;
    }

    protected void setParent(FluidContainer parent) {
        this.parent = parent;
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public String getTranslatedName() {
        return this.fluid.getTranslatedName();
    }

    public float getAmount() {
        return this.amount;
    }

    protected void setAmount(float amount) {
        this.amount = amount;
    }

    protected float getPercentage() {
        return this.percentage;
    }

    protected void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    protected Color getColor() {
        return this.color;
    }

    public void mixColor(Color mix, float amountAdded) {
        if (this.amount == 0.0F) {
            this.setColor(mix);
        } else if (amountAdded != 0.0F) {
            float delta = amountAdded / (amountAdded + this.amount);
            ColorMixer.LerpLCH(this.color, mix, delta, this.color);
        }
    }

    public void setColor(Color color) {
        this.color.set(color);
        if (this.parent != null) {
            this.parent.invalidateColor();
        }
    }

    public void setColor(float r, float g, float b) {
        this.color.set(r, g, b);
        if (this.parent != null) {
            this.parent.invalidateColor();
        }
    }

    private void reset() {
        this.parent = null;
        this.fluid = null;
        this.amount = 0.0F;
        this.percentage = 0.0F;
        this.color.set(1.0F, 1.0F, 1.0F);
    }
}
