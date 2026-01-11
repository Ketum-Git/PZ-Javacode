// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.energy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EnergyDefinitionScript;

@DebugClassFields
@UsedFromLua
public class Energy {
    private static boolean hasInitialized;
    private static final HashMap<EnergyType, Energy> energyEnumMap = new HashMap<>();
    private static final HashMap<String, Energy> energyStringMap = new HashMap<>();
    private static final HashMap<String, Energy> cacheStringMap = new HashMap<>();
    private static final ArrayList<Energy> allEnergies = new ArrayList<>();
    public static final Energy Electric = addEnergy(EnergyType.Electric);
    public static final Energy Mechanical = addEnergy(EnergyType.Mechanical);
    public static final Energy Thermal = addEnergy(EnergyType.Thermal);
    public static final Energy Steam = addEnergy(EnergyType.Steam);
    public static final Energy VoidEnergy = addEnergy(EnergyType.VoidEnergy);
    private EnergyDefinitionScript script;
    private final EnergyType energyType;
    private final String energyTypeString;
    private final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);

    private static Energy addEnergy(EnergyType type) {
        if (energyEnumMap.containsKey(type)) {
            throw new RuntimeException("Energy defined twice: " + type);
        } else {
            Energy energy = new Energy(type);
            energyEnumMap.put(type, energy);
            return energy;
        }
    }

    public static Energy Get(EnergyType type) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Energies have not yet been initialized!");
        } else {
            return energyEnumMap.get(type);
        }
    }

    public static Energy Get(String name) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Energies have not yet been initialized!");
        } else {
            return energyStringMap.get(name);
        }
    }

    public static ArrayList<Energy> getAllEnergies() {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Energies have not yet been initialized!");
        } else {
            return allEnergies;
        }
    }

    public static void Init(ScriptLoadMode loadMode) throws Exception {
        DebugLog.Energy.println("*************************************");
        DebugLog.Energy.println("* Energy: initialize Energies.      *");
        DebugLog.Energy.println("*************************************");
        ArrayList<EnergyDefinitionScript> scripts = ScriptManager.instance.getAllEnergyDefinitionScripts();
        cacheStringMap.clear();
        allEnergies.clear();
        if (loadMode == ScriptLoadMode.Reload) {
            for (Entry<String, Energy> entry : energyStringMap.entrySet()) {
                cacheStringMap.put(entry.getKey(), entry.getValue());
            }

            energyStringMap.clear();
        }

        for (EnergyDefinitionScript script : scripts) {
            if (script.getEnergyType() == EnergyType.Modded) {
                DebugLog.Energy.println(script.getModID() + " = " + script.getEnergyTypeString());
                Energy energy = cacheStringMap.get(script.getEnergyTypeString());
                if (energy == null) {
                    energy = new Energy(script.getEnergyTypeString());
                }

                energy.setScript(script);
                energyStringMap.put(script.getEnergyTypeString(), energy);
                allEnergies.add(energy);
            } else {
                DebugLog.Energy.println(script.getModID() + " = " + script.getEnergyType());
                Energy energy = energyEnumMap.get(script.getEnergyType());
                if (energy == null) {
                    if (Core.debug) {
                        throw new Exception("Energy not found: " + script.getEnergyType());
                    }
                } else {
                    energy.setScript(script);
                    allEnergies.add(energy);
                }
            }
        }

        for (Entry<EnergyType, Energy> entry : energyEnumMap.entrySet()) {
            if (Core.debug && entry.getValue().script == null) {
                throw new Exception("Energy has no script set: " + entry.getKey());
            }

            energyStringMap.put(entry.getKey().toString(), entry.getValue());
        }

        cacheStringMap.clear();
        hasInitialized = true;
        DebugLog.Energy.println("*************************************");
    }

    public static void PreReloadScripts() {
        hasInitialized = false;
    }

    public static void Reset() {
        energyStringMap.clear();
        hasInitialized = false;
    }

    public static void saveEnergy(Energy energy, ByteBuffer output) {
        output.put((byte)(energy != null ? 1 : 0));
        if (energy != null) {
            if (energy.energyType == EnergyType.Modded) {
                output.put((byte)1);
                GameWindow.WriteString(output, energy.energyTypeString);
            } else {
                output.put((byte)0);
                output.put(energy.energyType.getId());
            }
        }
    }

    public static Energy loadEnergy(ByteBuffer input, int WorldVersion) {
        if (input.get() == 0) {
            return null;
        } else {
            Energy energy;
            if (input.get() == 1) {
                String energyTypeString = GameWindow.ReadString(input);
                energy = Get(energyTypeString);
            } else {
                EnergyType energyType = EnergyType.FromId(input.get());
                energy = Get(energyType);
            }

            return energy;
        }
    }

    private Energy(EnergyType energyType) {
        this.energyType = energyType;
        this.energyTypeString = null;
    }

    private Energy(String energyTypeString) {
        this.energyType = EnergyType.Modded;
        this.energyTypeString = Objects.requireNonNull(energyTypeString);
    }

    private void setScript(EnergyDefinitionScript script) {
        this.script = Objects.requireNonNull(script);
        this.color.set(script.getColor());
    }

    public boolean isVanilla() {
        return this.script != null && this.script.isVanilla();
    }

    public String getDisplayName() {
        return this.script != null ? this.script.getDisplayName() : Translator.getEntityText("EC_Energy");
    }

    public Color getColor() {
        return this.color;
    }

    public Texture getIconTexture() {
        return this.script != null ? this.script.getIconTexture() : EnergyDefinitionScript.getDefaultIconTexture();
    }

    public Texture getHorizontalBarTexture() {
        return this.script != null ? this.script.getHorizontalBarTexture() : EnergyDefinitionScript.getDefaultHorizontalBarTexture();
    }

    public Texture getVerticalBarTexture() {
        return this.script != null ? this.script.getVerticalBarTexture() : EnergyDefinitionScript.getDefaultVerticalBarTexture();
    }

    public String getEnergyTypeString() {
        return this.energyType == EnergyType.Modded ? this.energyTypeString : this.energyType.toString();
    }
}
