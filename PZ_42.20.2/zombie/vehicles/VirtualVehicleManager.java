// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayList;
import java.util.List;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.input.Mouse;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.symbols.MapSymbolDefinitions;

public final class VirtualVehicleManager {
    private static VirtualVehicleManager instance;
    private final List<VirtualVehicle> vehicles = new ArrayList<>();

    public static VirtualVehicleManager getInstance() {
        if (instance == null) {
            instance = new VirtualVehicleManager();
        }

        return instance;
    }

    public boolean shouldAddToMeta(BaseVehicle vehicle) {
        return true;
    }

    public void addToMeta(BaseVehicle vehicle) {
        VirtualVehicle virtualVehicle = new VirtualVehicle();
        virtualVehicle.set(vehicle);
        this.vehicles.add(virtualVehicle);
    }

    public void removeFromMeta(BaseVehicle vehicle) {
        for (int i = 0; i < this.vehicles.size(); i++) {
            VirtualVehicle virtualVehicle = this.vehicles.get(i);
            if (virtualVehicle.getSqlId() == vehicle.getSqlId()) {
                virtualVehicle.removeFromMeta(vehicle);
                this.vehicles.remove(i);
                break;
            }
        }
    }

    public void update() {
        for (int i = 0; i < this.vehicles.size(); i++) {
            VirtualVehicle virtualVehicle = this.vehicles.get(i);
            virtualVehicle.update();
            if (!virtualVehicle.shouldUpdateInMeta()) {
                virtualVehicle.stopUpdatingInMeta();
                this.vehicles.remove(i--);
            }
        }
    }

    public void renderWorldMap(UIWorldMap ui) {
        MapSymbolDefinitions.MapSymbolDefinition symbol = MapSymbolDefinitions.getInstance().getSymbolById("SteeringWheel");
        if (symbol != null) {
            Texture tex = Texture.getSharedTexture(symbol.getTexturePath());
            if (tex != null && tex.isReady()) {
                int mouseX = Mouse.getXA() - ui.getAbsoluteX().intValue();
                int mouseY = Mouse.getYA() - ui.getAbsoluteY().intValue();
                VirtualVehicle vehicleUnderMouse = null;
                long ms = System.currentTimeMillis();
                float maxTexWidth = tex.getWidth() / 1.5F;
                float maxTexHeight = tex.getHeight() / 1.5F;
                double width = PZMath.lerp(tex.getWidth() / 2.0F, maxTexWidth, (float)Math.sin(ms / 300.0) + 1.0F);
                double height = PZMath.lerp(tex.getHeight() / 2.0F, maxTexHeight, (float)Math.sin(ms / 300.0) + 1.0F);
                double r = 0.0;
                double g = 0.0;
                double b = 0.0;
                double a = 1.0;

                for (int i = 0; i < this.vehicles.size(); i++) {
                    VirtualVehicle virtualVehicle = this.vehicles.get(i);
                    float uiX = PZMath.floor(ui.getAPI().worldToUIX(virtualVehicle.getX(), virtualVehicle.getY()));
                    float uiY = PZMath.floor(ui.getAPI().worldToUIY(virtualVehicle.getX(), virtualVehicle.getY()));
                    ui.DrawTextureScaledCol(tex, uiX - width / 2.0, uiY - height / 2.0, width, height, 0.0, 0.0, 0.0, 1.0);
                    if (mouseX >= uiX - maxTexWidth / 2.0F
                        && mouseX < uiX + maxTexWidth / 2.0F
                        && mouseY >= uiY - maxTexHeight / 2.0F
                        && mouseY < uiY + maxTexHeight / 2.0F) {
                        vehicleUnderMouse = virtualVehicle;
                    }
                }

                if (vehicleUnderMouse != null) {
                    float uiX = PZMath.floor(ui.getAPI().worldToUIX(vehicleUnderMouse.getX(), vehicleUnderMouse.getY()));
                    float uiY = PZMath.floor(ui.getAPI().worldToUIY(vehicleUnderMouse.getX(), vehicleUnderMouse.getY()));
                    int textY = PZMath.fastfloor(uiY + tex.getHeight() / 2);
                    UIFont font = UIFont.Medium;
                    int fontHgt = TextManager.instance.getFontHeight(font);
                    int lineNum = 0;
                    double padX = 4.0;
                    double padY = 0.0;
                    double textR = 0.0;
                    double textG = 0.0;
                    double textB = 0.0;
                    double textA = 1.0;
                    double bgR = 1.0;
                    double bgG = 1.0;
                    double bgB = 1.0;
                    double bgA = 0.9;
                    ui.drawTextWithBackground(font, vehicleUnderMouse.getScriptName(), uiX, textY + lineNum++, 0.0, 0.0, 0.0, 1.0, 4.0, 0.0, 1.0, 1.0, 1.0, 0.9);
                    ui.drawTextWithBackground(
                        font,
                        "Engine Running: %s".formatted(vehicleUnderMouse.isEngineRunning() ? "YES" : "NO"),
                        uiX,
                        textY + fontHgt * lineNum++,
                        0.0,
                        0.0,
                        0.0,
                        1.0,
                        4.0,
                        0.0,
                        1.0,
                        1.0,
                        1.0,
                        0.9
                    );
                    ui.drawTextWithBackground(
                        font,
                        "Alarm Active: %s".formatted(vehicleUnderMouse.isAlarmActive() ? "YES" : "NO"),
                        uiX,
                        textY + fontHgt * lineNum++,
                        0.0,
                        0.0,
                        0.0,
                        1.0,
                        4.0,
                        0.0,
                        1.0,
                        1.0,
                        1.0,
                        0.9
                    );
                    ui.drawTextWithBackground(
                        font,
                        "Siren Active: %s".formatted(vehicleUnderMouse.isSirenActive() ? "YES" : "NO"),
                        uiX,
                        textY + fontHgt * lineNum++,
                        0.0,
                        0.0,
                        0.0,
                        1.0,
                        4.0,
                        0.0,
                        1.0,
                        1.0,
                        1.0,
                        0.9
                    );
                    ui.drawTextWithBackground(
                        font,
                        "Battery Charge: %.4f".formatted(vehicleUnderMouse.getBatteryCharge()),
                        uiX,
                        textY + fontHgt * lineNum++,
                        0.0,
                        0.0,
                        0.0,
                        1.0,
                        4.0,
                        0.0,
                        1.0,
                        1.0,
                        1.0,
                        0.9
                    );
                    VehiclePart var50 = vehicleUnderMouse.getPartByPartId(zombie.scripting.objects.VehiclePart.GAS_TANK);
                    if (var50 instanceof VehiclePart) {
                        ui.drawTextWithBackground(
                            font,
                            "Fuel: %.4f".formatted(var50.getContainerContentAmount()),
                            uiX,
                            textY + fontHgt * lineNum,
                            0.0,
                            0.0,
                            0.0,
                            1.0,
                            4.0,
                            0.0,
                            1.0,
                            1.0,
                            1.0,
                            0.9
                        );
                    }
                }
            }
        }
    }

    public void save() {
        try {
            for (int i = 0; i < this.vehicles.size(); i++) {
                VirtualVehicle virtualVehicle = this.vehicles.get(i);
                virtualVehicle.save();
            }
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
        }
    }

    public void Reset() {
        for (int i = 0; i < this.vehicles.size(); i++) {
            VirtualVehicle virtualVehicle = this.vehicles.get(i);
            virtualVehicle.removeFromMeta(null);
        }

        this.vehicles.clear();
    }
}
