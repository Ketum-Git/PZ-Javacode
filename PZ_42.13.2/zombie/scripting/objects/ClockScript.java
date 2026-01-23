// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

public final class ClockScript extends BaseScriptObject {
    public String replacementSprite;
    public float handX;
    public float handY;
    public float handZ;
    public boolean north;
    public final ClockScript.HandScript hourHand = new ClockScript.HandScript();
    public final ClockScript.HandScript minuteHand = new ClockScript.HandScript();
    public boolean replacementOnly;

    public ClockScript() {
        super(ScriptType.Clock);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.replacementSprite = null;
        this.handX = this.handY = this.handZ = 0.0F;
        this.north = false;
        this.replacementOnly = false;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        boolean bReplacementOnly1 = block.children.isEmpty();

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("handOffset".equalsIgnoreCase(k)) {
                String[] ss = v.split("\\s+");
                if (ss.length == 3) {
                    this.handX = PZMath.tryParseFloat(ss[0], 0.0F);
                    this.handY = PZMath.tryParseFloat(ss[1], 0.0F);
                    this.handZ = PZMath.tryParseFloat(ss[2], 0.0F);
                }

                bReplacementOnly1 = false;
            } else if ("north".equalsIgnoreCase(k)) {
                this.north = StringUtils.tryParseBoolean(v);
                bReplacementOnly1 = false;
            } else if ("replacementSprite".equalsIgnoreCase(k)) {
                this.replacementSprite = StringUtils.discardNullOrWhitespace(v);
            }
        }

        if (bReplacementOnly1) {
            this.replacementOnly = true;
        } else {
            for (ScriptParser.Block child : block.children) {
                if ("hand".equalsIgnoreCase(child.type)) {
                    if ("hour".equalsIgnoreCase(child.id)) {
                        this.LoadHand(child, this.hourHand);
                    }

                    if ("minute".equalsIgnoreCase(child.id)) {
                        this.LoadHand(child, this.minuteHand);
                    }
                }
            }
        }
    }

    private void LoadHand(ScriptParser.Block block, ClockScript.HandScript handScript) {
        handScript.length = 1.0F;
        handScript.thickness = 0.1F;
        handScript.texture = "white.png";
        handScript.textureAxisX = handScript.textureAxisY = Float.NaN;
        handScript.r = handScript.g = handScript.b = handScript.a = 1.0F;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("length".equalsIgnoreCase(k)) {
                handScript.length = PZMath.tryParseFloat(v, 1.0F);
            } else if ("texture".equalsIgnoreCase(k)) {
                String texture = StringUtils.discardNullOrWhitespace(v);
                if (texture != null) {
                    handScript.texture = "media/textures/" + texture + ".png";
                }
            } else if ("textureInfo".equalsIgnoreCase(k)) {
                String[] ss = v.split("\\s+");
                if (ss.length == 4) {
                    float texW = PZMath.tryParseFloat(ss[0], 128.0F);
                    float texH = PZMath.tryParseFloat(ss[1], 128.0F);
                    float axisX = PZMath.tryParseFloat(ss[2], 0.5F);
                    float axisY = PZMath.tryParseFloat(ss[3], 0.5F);
                    handScript.textureAxisX = axisX / texW;
                    handScript.textureAxisY = axisY / texH;
                }
            } else if ("thickness".equalsIgnoreCase(k)) {
                handScript.thickness = PZMath.tryParseFloat(v, 0.1F);
            } else if ("rgba".equalsIgnoreCase(k)) {
                String[] ss = v.split("\\s+");
                if (ss.length == 4) {
                    handScript.r = PZMath.tryParseFloat(ss[0], 1.0F);
                    handScript.g = PZMath.tryParseFloat(ss[1], 1.0F);
                    handScript.b = PZMath.tryParseFloat(ss[2], 1.0F);
                    handScript.a = PZMath.tryParseFloat(ss[3], 1.0F);
                }
            }
        }
    }

    public static final class HandScript {
        public float length = 1.0F;
        public float thickness = 0.1F;
        public String texture;
        public float textureAxisX = Float.NaN;
        public float textureAxisY = Float.NaN;
        public float r;
        public float g;
        public float b;
        public float a;
    }
}
