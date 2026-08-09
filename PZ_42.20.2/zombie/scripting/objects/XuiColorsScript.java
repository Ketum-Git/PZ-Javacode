// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@UsedFromLua
public class XuiColorsScript extends BaseScriptObject {
    private String name;
    private final Map<String, Color> colorMap = new HashMap<>();

    public XuiColorsScript() {
        super(ScriptType.XuiColor);
    }

    public String getName() {
        return this.name;
    }

    public Map<String, Color> getColorMap() {
        return this.colorMap;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.name = name;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        this.LoadColorsBlock(block);
    }

    protected void LoadColorsBlock(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                Color color = new Color();
                String[] split = val.split(":");
                if (split.length > 1 && split[0].trim().equalsIgnoreCase("rgb")) {
                    for (int i = 1; i < split.length; i++) {
                        switch (i) {
                            case 1:
                                color.r = Float.parseFloat(split[i].trim()) / 255.0F;
                                break;
                            case 2:
                                color.g = Float.parseFloat(split[i].trim()) / 255.0F;
                                break;
                            case 3:
                                color.b = Float.parseFloat(split[i].trim()) / 255.0F;
                                break;
                            case 4:
                                color.a = Float.parseFloat(split[i].trim()) / 255.0F;
                        }
                    }
                } else {
                    for (int i = 0; i < split.length; i++) {
                        switch (i) {
                            case 0:
                                color.r = Float.parseFloat(split[i].trim());
                                break;
                            case 1:
                                color.g = Float.parseFloat(split[i].trim());
                                break;
                            case 2:
                                color.b = Float.parseFloat(split[i].trim());
                                break;
                            case 3:
                                color.a = Float.parseFloat(split[i].trim());
                        }
                    }
                }

                this.colorMap.put(key, color);
            }
        }
    }
}
