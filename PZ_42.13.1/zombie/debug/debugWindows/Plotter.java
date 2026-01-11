// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.extension.implot.ImPlot;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.debug.BaseDebugWindow;

public class Plotter extends BaseDebugWindow {
    ArrayList<Plotter.PlottedVar> vars = new ArrayList<>();
    ArrayList<Double> valuesX = new ArrayList<>();
    ArrayList<Double> valuesY = new ArrayList<>();
    Float tick = 0.0F;
    float lastTime = -0.1F;

    @Override
    public String getTitle() {
        return "Plotter";
    }

    public Plotter(Object obj, Field field) {
        this.vars.add(new Plotter.PlottedVar(obj, field));
        this.lastTime = GameTime.getInstance().timeOfDay - 0.01F;
    }

    public void addVariable(Object obj, Field field) {
        this.vars.add(new Plotter.PlottedVar(obj, field));
    }

    @Override
    protected void doWindowContents() {
        float dif = GameTime.getInstance().timeOfDay - this.lastTime;
        if (dif > 0.01F || dif < 0.0F) {
            if (dif < 0.0F) {
                dif += 24.0F;
            }

            this.tick = this.tick + dif / 0.01F;

            for (int i = 0; i < this.vars.size(); i++) {
                Plotter.PlottedVar var = this.vars.get(i);
                var.field.setAccessible(true);

                try {
                    var.valuesY.add(((Float)var.field.get(var.obj)).doubleValue());
                } catch (IllegalAccessException var12) {
                }
            }

            this.valuesX.add(this.tick.doubleValue());
            this.lastTime = GameTime.getInstance().timeOfDay;
        }

        for (int i = 0; i < this.vars.size(); i++) {
            Plotter.PlottedVar var = this.vars.get(i);
            var.normalize();
        }

        double maxX = -1.0E8;
        double maxY = -1.0E8;
        double minX = 1.0E8;
        double minY = 1.0E8;

        for (int x = 0; x < this.valuesX.size(); x++) {
            minX = Math.min(minX, this.valuesX.get(x));
            maxX = Math.max(maxX, this.valuesX.get(x));
        }

        for (int x = 0; x < this.vars.size(); x++) {
            Plotter.PlottedVar v = this.vars.get(x);
            minY = Math.min(minY, v.minY);
            maxY = Math.max(maxY, v.maxY);
        }

        if (!this.valuesX.isEmpty()) {
            ImPlot.setNextPlotLimits(minX, maxX, minY, maxY, 1);
        }

        if (ImPlot.beginPlot("plot", "time", "")) {
            for (int i = 0; i < this.vars.size(); i++) {
                Plotter.PlottedVar var = this.vars.get(i);
                var.plot(this.valuesX);
            }
        }

        ImPlot.endPlot();
    }

    public class PlottedVar {
        public Object obj;
        public Field field;
        ArrayList<Double> valuesY;
        public double minY;
        public double maxY;

        public PlottedVar(final Object obj, final Field field) {
            Objects.requireNonNull(Plotter.this);
            super();
            this.valuesY = new ArrayList<>();
            this.field = field;
            this.obj = obj;
        }

        public void normalize() {
            double maxY = -1.0E8;
            double minY = 1.0E8;

            for (int x = 0; x < this.valuesY.size(); x++) {
                minY = Math.min(minY, this.valuesY.get(x));
                maxY = Math.max(maxY, this.valuesY.get(x));
            }

            if (minY >= 0.0 && maxY <= 1.0) {
                minY = 0.0;
                maxY = 1.0;
            }

            this.minY = minY;
            this.maxY = maxY;
        }

        public void plot(ArrayList<Double> valuesX) {
            ImPlot.plotLine(
                this.obj.getClass().getSimpleName() + "." + this.field.getName(), valuesX.toArray(new Double[0]), this.valuesY.toArray(new Double[0])
            );
        }
    }
}
